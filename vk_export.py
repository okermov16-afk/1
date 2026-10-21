#!/usr/bin/env python3
"""Выгрузка сообщества VK: посты, фото (из постов и альбомов), документы, ссылки на видео.

Только стандартная библиотека Python 3.8+.

Использование:
    python3 vk_export.py --token ВАШ_ТОКЕН --group manipulator_verse --out vk_backup

Токен: пользовательский (лучше от админа группы) со scope: wall,photos,video,docs,groups,offline
Получить: https://vkhost.github.io/ (Kate Mobile / VK Admin) или своё приложение на https://dev.vk.com
Скрипт можно перезапускать: уже скачанные файлы пропускаются.
"""
import argparse
import html
import json
import os
import time
import urllib.error
import urllib.parse
import urllib.request
from datetime import datetime

API = "https://api.vk.com/method/"
V = "5.199"


class VK:
    def __init__(self, token):
        self.token = token
        self.last = 0.0

    def call(self, method, **params):
        params.update(access_token=self.token, v=V)
        url = API + method + "?" + urllib.parse.urlencode(params)
        for attempt in range(6):
            wait = 0.4 - (time.time() - self.last)  # лимит ~3 запроса/сек
            if wait > 0:
                time.sleep(wait)
            self.last = time.time()
            try:
                with urllib.request.urlopen(url, timeout=60) as r:
                    data = json.load(r)
            except (urllib.error.URLError, TimeoutError):
                time.sleep(2 ** attempt)
                continue
            if "error" in data:
                code = data["error"].get("error_code")
                if code in (6, 9, 10):  # слишком часто / внутренняя ошибка
                    time.sleep(2 ** attempt)
                    continue
                raise RuntimeError(f"{method}: {data['error']}")
            return data["response"]
        raise RuntimeError(f"{method}: не удалось выполнить после повторов")

    def paginate(self, method, **params):
        offset, items = 0, []
        while True:
            r = self.call(method, count=100, offset=offset, **params)
            batch = r["items"]
            items += batch
            offset += len(batch)
            print(f"  {method}: {len(items)}/{r['count']}", end="\r")
            if not batch or offset >= r["count"]:
                print()
                return items


def download(url, path):
    if os.path.exists(path) and os.path.getsize(path) > 0:
        return True
    os.makedirs(os.path.dirname(path), exist_ok=True)
    for attempt in range(4):
        try:
            urllib.request.urlretrieve(url, path)
            return True
        except Exception:
            time.sleep(2 ** attempt)
    print(f"  не скачалось: {url}")
    return False


def best_photo(photo):
    sizes = photo.get("sizes") or []
    if not sizes:
        return None
    return max(sizes, key=lambda s: s.get("width", 0) * s.get("height", 0))["url"]


def photo_path(out, photo, folder="photos"):
    return os.path.join(out, folder, f"{photo['owner_id']}_{photo['id']}.jpg")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--token", required=True)
    ap.add_argument("--group", default="manipulator_verse")
    ap.add_argument("--out", default="vk_backup")
    a = ap.parse_args()

    vk = VK(a.token)
    g = vk.call("groups.getById", group_id=a.group)
    g = (g.get("groups") or g)[0] if isinstance(g, dict) else g[0]
    gid, owner = g["id"], -g["id"]
    print(f"Группа: {g['name']} (id {gid})")
    os.makedirs(a.out, exist_ok=True)
    json.dump(g, open(os.path.join(a.out, "group.json"), "w", encoding="utf-8"), ensure_ascii=False, indent=1)

    # --- стена ---
    print("Посты...")
    posts = vk.paginate("wall.get", owner_id=owner, extended=0)
    json.dump(posts, open(os.path.join(a.out, "posts.json"), "w", encoding="utf-8"), ensure_ascii=False, indent=1)

    # --- все фото сообщества (альбомы, стена, обсуждения) ---
    print("Фотографии...")
    try:
        all_photos = vk.paginate("photos.getAll", owner_id=owner, photo_sizes=1)
    except RuntimeError as e:
        print("  photos.getAll недоступен:", e)
        all_photos = []
    json.dump(all_photos, open(os.path.join(a.out, "photos.json"), "w", encoding="utf-8"), ensure_ascii=False, indent=1)

    photo_files = {}  # "owner_id" -> относительный путь
    n = 0
    for ph in all_photos:
        url = best_photo(ph)
        if url:
            p = photo_path(a.out, ph)
            if download(url, p):
                photo_files[(ph["owner_id"], ph["id"])] = os.path.relpath(p, a.out)
        n += 1
        print(f"  фото {n}/{len(all_photos)}", end="\r")
    print()

    # --- вложения постов: фото, которых нет в альбомах, документы, видео ---
    videos, docs_links = [], []
    html_posts = []
    for post in sorted(posts, key=lambda x: x["date"], reverse=True):
        parts = []
        for att in post.get("attachments", []):
            t = att["type"]
            if t == "photo":
                ph = att["photo"]
                key = (ph["owner_id"], ph["id"])
                if key not in photo_files:
                    url = best_photo(ph)
                    p = photo_path(a.out, ph, "photos_from_posts")
                    if url and download(url, p):
                        photo_files[key] = os.path.relpath(p, a.out)
                if key in photo_files:
                    parts.append(f'<img src="{photo_files[key]}" style="max-width:100%">')
            elif t == "doc":
                d = att["doc"]
                p = os.path.join(a.out, "docs", f"{d['id']}_{d['title']}".replace("/", "_"))
                if download(d["url"], p):
                    parts.append(f'<a href="{os.path.relpath(p, a.out)}">📎 {html.escape(d["title"])}</a>')
            elif t == "video":
                v = att["video"]
                link = f"https://vk.com/video{v['owner_id']}_{v['id']}"
                videos.append({"title": v.get("title"), "url": link, "post": post["id"]})
                parts.append(f'<a href="{link}">🎬 {html.escape(v.get("title", "видео"))}</a>')
            elif t == "link":
                l = att["link"]
                parts.append(f'<a href="{l["url"]}">🔗 {html.escape(l.get("title") or l["url"])}</a>')
        date = datetime.fromtimestamp(post["date"]).strftime("%Y-%m-%d %H:%M")
        text = html.escape(post.get("text", "")).replace("\n", "<br>")
        html_posts.append(
            f'<article><h4>{date} — <a href="https://vk.com/wall{owner}_{post["id"]}">пост {post["id"]}</a></h4>'
            f"<p>{text}</p>{'<br>'.join(parts)}</article><hr>"
        )

    # --- отдельно все видео сообщества (ссылки) ---
    print("Видео (ссылки)...")
    try:
        for v in vk.paginate("video.get", owner_id=owner):
            videos.append({"title": v.get("title"), "url": f"https://vk.com/video{v['owner_id']}_{v['id']}"})
    except RuntimeError as e:
        print("  video.get недоступен:", e)
    json.dump(videos, open(os.path.join(a.out, "videos.json"), "w", encoding="utf-8"), ensure_ascii=False, indent=1)

    with open(os.path.join(a.out, "index.html"), "w", encoding="utf-8") as f:
        f.write(
            '<meta charset="utf-8"><body style="max-width:720px;margin:auto;font-family:sans-serif">'
            f"<h1>{html.escape(g['name'])}</h1>" + "".join(html_posts)
        )

    print(f"\nГотово: {len(posts)} постов, {len(photo_files)} фото, {len(videos)} видео-ссылок -> {a.out}/")
    print("Откройте index.html для просмотра. Видео скачивайте: yt-dlp -a urls.txt (URL из videos.json).")


if __name__ == "__main__":
    main()
