#!/usr/bin/env python3
"""Полная выгрузка сообщества VK одним запуском.

Забирает: описание, аватарку и обложку (с историей), все посты с комментариями,
все альбомы и фото, обсуждения (темы + все сообщения), документы, видео, подписчиков.
Результат: папка с JSON-данными, скачанными файлами и HTML-страницами для просмотра.

Только стандартная библиотека Python 3.8+ (для скачивания видео нужен yt-dlp, необязательно).

    python3 vk_export.py     (спросит токен сам)

Токен: пользовательский (админа группы), права: wall,photos,video,docs,groups,market,offline
Можно передать через переменную окружения VK_TOKEN. Повторный запуск докачивает недостающее.
"""
import argparse
import html
import json
import os
import shutil
import subprocess
import sys
import time
import traceback
import urllib.error
import urllib.parse
import urllib.request
from datetime import datetime

API = "https://api.vk.com/method/"
V = "5.199"


def ts(t):
    return datetime.fromtimestamp(t).strftime("%Y-%m-%d %H:%M")


class VK:
    def __init__(self, token):
        self.token, self.last = token, 0.0

    def call(self, method, **params):
        params.update(access_token=self.token, v=V)
        data = urllib.parse.urlencode(params).encode()
        for attempt in range(6):
            wait = 0.36 - (time.time() - self.last)  # лимит ~3 запроса/сек
            if wait > 0:
                time.sleep(wait)
            self.last = time.time()
            try:
                with urllib.request.urlopen(API + method, data=data, timeout=60) as r:
                    resp = json.load(r)
            except (urllib.error.URLError, TimeoutError, ValueError):
                time.sleep(2 ** attempt)
                continue
            if "error" in resp:
                if resp["error"].get("error_code") in (6, 9, 10):
                    time.sleep(2 ** attempt)
                    continue
                raise RuntimeError(f"{method}: {resp['error'].get('error_msg')}")
            return resp["response"]
        raise RuntimeError(f"{method}: не удалось выполнить после повторов")

    def all(self, method, step=100, extra=None, **params):
        """Постраничная загрузка. Возвращает (items, profiles, groups)."""
        offset, items, profiles, groups = 0, [], [], []
        while True:
            r = self.call(method, count=step, offset=offset, **params)
            batch = r.get("items", [])
            items += batch
            profiles += r.get("profiles", [])
            groups += r.get("groups", [])
            offset += len(batch)
            print(f"    {method}: {len(items)}/{r.get('count', '?')}   ", end="\r")
            if not batch or offset >= r.get("count", 0):
                print()
                return items, profiles, groups


def largest(sizes):
    return max(sizes, key=lambda s: s.get("width", 0) * s.get("height", 0))["url"] if sizes else None


class Exporter:
    def __init__(self, vk, out, group):
        self.vk, self.out, self.group = vk, out, group
        self.names = {}      # id (users >0, groups <0) -> имя
        self.photo_files = {}  # (owner, id) -> относительный путь
        self.videos = {}     # "owner_id" -> {title,url}
        os.makedirs(out, exist_ok=True)

    # ---------- утилиты ----------
    def save_json(self, name, data):
        with open(os.path.join(self.out, name), "w", encoding="utf-8") as f:
            json.dump(data, f, ensure_ascii=False, indent=1)

    def remember(self, profiles, groups):
        for p in profiles:
            self.names[p["id"]] = f"{p.get('first_name', '')} {p.get('last_name', '')}".strip()
        for g in groups:
            self.names[-g["id"]] = g.get("name", "")

    def download(self, url, rel):
        path = os.path.join(self.out, rel)
        if os.path.exists(path) and os.path.getsize(path) > 0:
            return rel
        os.makedirs(os.path.dirname(path), exist_ok=True)
        for attempt in range(4):
            try:
                urllib.request.urlretrieve(url, path)
                return rel
            except Exception:
                time.sleep(2 ** attempt)
        print(f"    не скачалось: {url}")
        return None

    def save_photo(self, ph, folder="photos"):
        key = (ph["owner_id"], ph["id"])
        if key in self.photo_files:
            return self.photo_files[key]
        url = largest(ph.get("sizes"))
        rel = self.download(url, f"{folder}/{key[0]}_{key[1]}.jpg") if url else None
        self.photo_files[key] = rel
        return rel

    def att_html(self, att):
        t = att.get("type")
        a = att.get(t, {})
        if t == "photo":
            rel = self.save_photo(a)
            return f'<img src="{rel}" style="max-width:100%">' if rel else ""
        if t == "doc":
            safe = f"{a['id']}_{a.get('title', 'doc')}".replace("/", "_")
            rel = self.download(a["url"], f"docs/{safe}")
            return f'<a href="{rel}">📎 {html.escape(a.get("title", ""))}</a>' if rel else ""
        if t == "video":
            link = f"https://vk.com/video{a['owner_id']}_{a['id']}"
            self.videos[f"{a['owner_id']}_{a['id']}"] = {"title": a.get("title"), "url": link}
            return f'<a href="{link}">🎬 {html.escape(a.get("title", "видео"))}</a>'
        if t == "link":
            return f'<a href="{a["url"]}">🔗 {html.escape(a.get("title") or a["url"])}</a>'
        if t == "audio":
            return f'🎵 {html.escape(a.get("artist", ""))} — {html.escape(a.get("title", ""))}'
        if t == "poll":
            ans = ", ".join(x["text"] for x in a.get("answers", []))
            return f'📊 {html.escape(a.get("question", ""))}: {html.escape(ans)}'
        if t == "sticker":
            url = largest(a.get("images"))
            rel = self.download(url, f"photos/sticker_{a['sticker_id']}.png") if url else None
            return f'<img src="{rel}" width="128">' if rel else ""
        if t == "album":
            return f'🖼 альбом: {html.escape(a.get("title", ""))}'
        return f"<i>[вложение: {t}]</i>"

    def atts_html(self, item):
        return "<br>".join(filter(None, (self.att_html(x) for x in item.get("attachments", []))))

    def author(self, uid):
        n = html.escape(self.names.get(uid, str(uid)))
        return f'<a href="https://vk.com/{"club" if uid < 0 else "id"}{abs(uid)}">{n}</a>'

    def comment_html(self, c, depth=0):
        who = c.get("from_id") or c.get("owner_id", 0)
        text = html.escape(c.get("text", "")).replace("\n", "<br>")
        s = (f'<div style="margin-left:{depth * 28}px;border-left:2px solid #ddd;padding-left:8px;margin-top:6px">'
             f'<small>{self.author(who)} · {ts(c["date"])}</small><br>{text}<br>{self.atts_html(c)}</div>')
        for r in (c.get("thread") or {}).get("items", []):
            s += self.comment_html(r, depth + 1)
        return s

    def page(self, title, body, name):
        nav = ('<nav><a href="index.html">Посты</a> · <a href="board.html">Обсуждения</a> · '
               '<a href="albums.html">Альбомы</a> · <a href="videos.html">Видео</a> · <a href="about.html">О группе</a></nav><hr>')
        with open(os.path.join(self.out, name), "w", encoding="utf-8") as f:
            f.write('<meta charset="utf-8"><body style="max-width:760px;margin:auto;font-family:sans-serif;line-height:1.4">'
                    f"{nav}<h1>{html.escape(title)}</h1>{body}")

    # ---------- этапы ----------
    def about(self):
        fields = "description,cover,members_count,contacts,links,site,status,activity,city,country,photo_max_orig,wiki_page,menu"
        r = self.vk.call("groups.getById", group_id=self.group, fields=fields)
        g = (r.get("groups") if isinstance(r, dict) else r)[0]
        self.gid, self.owner, self.title = g["id"], -g["id"], g["name"]
        self.names[self.owner] = g["name"]
        self.save_json("group.json", g)
        body = ""
        if g.get("photo_max_orig"):
            rel = self.download(g["photo_max_orig"], "avatar/current.jpg")
            body += f'<img src="{rel}" width="200"><br>'
        imgs = (g.get("cover") or {}).get("images") or []
        if imgs:
            rel = self.download(largest(imgs), "avatar/cover.jpg")
            body += f'<img src="{rel}" style="max-width:100%"><br>'
        body += f'<p><b>{html.escape(g.get("status", ""))}</b></p><p>{html.escape(g.get("description", "")).replace(chr(10), "<br>")}</p>'
        body += f'<p>Подписчиков: {g.get("members_count", "?")}</p>'
        for l in g.get("links", []):
            body += f'<p><a href="{l["url"]}">{html.escape(l.get("name", l["url"]))}</a></p>'
        self.page(g["name"], body, "about.html")
        print(f"Группа: {g['name']} (id {self.gid})")

    def albums(self):
        r =self.vk.call("photos.getAlbums", owner_id=self.owner, need_system=1)
        albums = r["items"]
        # аватарка с историей лежит в системном альбоме profile
        albums.append({"id": "profile", "title": "Фото профиля (аватарки)"})
        body, data = "", []
        for al in albums:
            try:
                photos, _, _ = self.vk.all("photos.get", step=1000, owner_id=self.owner,
                                           album_id=al["id"], photo_sizes=1, rev=1)
            except RuntimeError as e:
                print(f"    альбом {al.get('title')}: {e}")
                continue
            if not photos:
                continue
            folder = "avatar" if al["id"] == "profile" else "photos"
            body += f'<h2>{html.escape(al.get("title", ""))} ({len(photos)})</h2>'
            for i, ph in enumerate(photos, 1):
                rel = self.save_photo(ph, folder)
                print(f"    {al.get('title')}: {i}/{len(photos)}   ", end="\r")
                if rel:
                    cap = html.escape(ph.get("text", ""))
                    body += f'<figure><img src="{rel}" style="max-width:100%"><figcaption>{cap}</figcaption></figure>'
            print()
            data.append({"album": al, "photos": photos})
        # страховка: фото, не попавшие в альбомы (со стены, из обсуждений)
        try:
            rest, _, _ = self.vk.all("photos.getAll", step=200, owner_id=self.owner, photo_sizes=1)
            extra = [p for p in rest if (p["owner_id"], p["id"]) not in self.photo_files]
            if extra:
                body += f"<h2>Остальные фото ({len(extra)})</h2>"
                for ph in extra:
                    rel = self.save_photo(ph)
                    if rel:
                        body += f'<img src="{rel}" style="max-width:100%">'
                data.append({"album": {"title": "Остальные"}, "photos": extra})
        except RuntimeError as e:
            print("    photos.getAll:", e)
        self.save_json("albums.json", data)
        self.page("Альбомы", body, "albums.html")

    def wall(self):
        posts, profiles, groups = self.vk.all("wall.get", step=100, owner_id=self.owner, extended=1)
        self.remember(profiles, groups)
        print("  комментарии к постам...")
        for i, p in enumerate(posts, 1):
            if p.get("comments", {}).get("count", 0) > 0:
                try:
                    p["_comments"] = self.post_comments(p["id"])
                except RuntimeError as e:
                    print(f"    пост {p['id']}: {e}")
            print(f"    {i}/{len(posts)}   ", end="\r")
        print()
        self.save_json("posts.json", posts)
        body = ""
        for p in posts:  # wall.get уже отдаёт от новых к старым (закреп первым)
            text = html.escape(p.get("text", "")).replace("\n", "<br>")
            body += (f'<article><h4>{ts(p["date"])} — <a href="https://vk.com/wall{self.owner}_{p["id"]}">пост {p["id"]}</a>'
                     f'{" 📌" if p.get("is_pinned") else ""}</h4><p>{text}</p>{self.atts_html(p)}')
            for rp in p.get("copy_history", []):  # репосты
                body += (f'<blockquote><small>репост от {self.author(rp.get("owner_id", 0))}</small><br>'
                         f'{html.escape(rp.get("text", "")).replace(chr(10), "<br>")}<br>{self.atts_html(rp)}</blockquote>')
            if p.get("_comments"):
                body += "<details><summary>Комментарии ({})</summary>{}</details>".format(
                    len(p["_comments"]), "".join(self.comment_html(c) for c in p["_comments"]))
            body += "</article><hr>"
        self.page(self.title, body, "index.html")

    def post_comments(self, post_id):
        cs, pr, gr = self.vk.all("wall.getComments", step=100, owner_id=self.owner, post_id=post_id,
                                 extended=1, thread_items_count=10, sort="asc")
        self.remember(pr, gr)
        for c in cs:
            th = c.get("thread") or {}
            if th.get("count", 0) > len(th.get("items", [])):
                items, pr, gr = self.vk.all("wall.getComments", step=100, owner_id=self.owner, post_id=post_id,
                                            comment_id=c["id"], extended=1, sort="asc")
                self.remember(pr, gr)
                c["thread"]["items"] = items
        return cs

    def board(self):
        topics, pr, gr = self.vk.all("board.getTopics", step=100, group_id=self.gid, extended=1, preview=0)
        self.remember(pr, gr)
        body = ""
        for t in topics:
            comments, pr, gr = self.vk.all("board.getComments", step=100, group_id=self.gid,
                                           topic_id=t["id"], extended=1, sort="asc")
            self.remember(pr, gr)
            t["_comments"] = comments
            body += f'<h2>{html.escape(t["title"])}</h2><small>{len(comments)} сообщ.</small>'
            for c in comments:
                body += self.comment_html({**c, "from_id": c.get("from_id")})
            body += "<hr>"
        self.save_json("board.json", topics)
        self.page("Обсуждения", body or "<p>Обсуждений нет.</p>", "board.html")

    def docs(self):
        docs, _, _ = self.vk.all("docs.get", step=200, owner_id=self.owner)
        for d in docs:
            self.att_html({"type": "doc", "doc": d})
        self.save_json("docs.json", docs)

    def video_list(self):
        try:
            vids, _, _ = self.vk.all("video.get", step=200, owner_id=self.owner)
            for v in vids:
                self.videos[f"{v['owner_id']}_{v['id']}"] = {"title": v.get("title"),
                                                              "url": f"https://vk.com/video{v['owner_id']}_{v['id']}"}
        except RuntimeError as e:
            print("    video.get:", e)

    def video_download(self):
        self.save_json("videos.json", self.videos)
        exe = shutil.which("yt-dlp")
        cmd = [exe] if exe else ([sys.executable, "-m", "yt_dlp"] if subprocess.run([sys.executable, "-m", "yt_dlp", "--version"], capture_output=True).returncode == 0 else None)
        exe = cmd
        body = ""
        for key, v in self.videos.items():
            local = None
            if exe:
                os.makedirs(os.path.join(self.out, "videos"), exist_ok=True)
                print(f"    видео {v['title']}")
                subprocess.run([*exe, "-q", "--no-warnings", "-o", os.path.join(self.out, "videos", f"{key}.%(ext)s"), v["url"]])
                found = [f for f in os.listdir(os.path.join(self.out, "videos")) if f.startswith(key + ".")]
                local = f"videos/{found[0]}" if found else None
            link = f'<a href="{local}">▶ файл</a> · ' if local else ""
            body += f'<p>{html.escape(v["title"] or key)} — {link}<a href="{v["url"]}">на VK</a></p>'
        if not exe:
            body = "<p><i>yt-dlp не найден — видео не скачаны (pip install yt-dlp и перезапустите скрипт).</i></p>" + body
        self.page("Видео", body or "<p>Видео нет.</p>", "videos.html")

    def members(self):
        m, _, _ = self.vk.all("groups.getMembers", step=1000, group_id=self.gid, fields="domain")
        self.save_json("members.json", m)

    def market(self):
        items, _, _ = self.vk.all("market.get", step=200, owner_id=self.owner, extended=1)
        for it in items:
            for ph in it.get("photos", []):
                self.save_photo(ph, "market")
        self.save_json("market.json", items)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--token", default=os.environ.get("VK_TOKEN"))
    ap.add_argument("--group", default="manipulator_verse")
    ap.add_argument("--out", default=os.path.join(os.path.dirname(os.path.abspath(__file__)), "vk_backup"))
    a = ap.parse_args()
    token = a.token or input("Вставьте токен (или всю ссылку целиком) и нажмите Enter: ").strip()
    if "access_token=" in token:  # можно вставить ссылку целиком
        token = urllib.parse.parse_qs(token.split("#", 1)[-1])["access_token"][0]
    a.token = token
    if not shutil.which("yt-dlp"):  # для скачивания видео
        print("Ставлю yt-dlp для видео...")
        subprocess.run([sys.executable, "-m", "pip", "install", "-q", "yt-dlp"])

    ex = Exporter(VK(a.token), a.out, a.group)
    ex.about()
    steps = [("Альбомы и аватарки", ex.albums), ("Посты и комментарии", ex.wall),
             ("Обсуждения", ex.board), ("Документы", ex.docs), ("Список видео", ex.video_list),
             ("Подписчики", ex.members), ("Товары", ex.market), ("Видео (файлы)", ex.video_download)]
    failed = []
    for name, fn in steps:
        print(f"== {name}")
        try:
            fn()
        except Exception as e:
            failed.append(f"{name}: {e}")
            traceback.print_exc()
    print(f"\nГотово. Фото: {len([v for v in ex.photo_files.values() if v])}, видео: {len(ex.videos)}. Папка: {a.out}/")
    print(f"Откройте {os.path.join(a.out, 'index.html')}")
    if failed:
        print("Не удалось выгрузить (скорее всего, нет прав у токена или раздел отключён):")
        for f in failed:
            print("  -", f)
    input("\nНажмите Enter, чтобы закрыть окно...")


if __name__ == "__main__":
    main()
