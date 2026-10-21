#!/usr/bin/env python3
"""Скачивает на компьютер все картинки из wall_clean.json и собирает локальную стену.

Запуск: python vk_images.py            (ищет wall_clean.json рядом и в папке Загрузки)
        python vk_images.py путь\\к\\wall_clean.json
Если рядом есть wall_links.json (даты, ссылки на посты, документы, видео) — добавит их в страницу.
Результат: папка vk_wall_local/ (открывайте index.html), можно перезапускать — докачивает.
"""
import html
import json
import os
import re
import sys
import time
import urllib.request

here = os.path.dirname(os.path.abspath(__file__))
home = os.path.expanduser("~")


def find(name, arg=None):
    cands = [arg] if arg else []
    for d in (os.getcwd(), here, os.path.join(home, "Downloads"), os.path.join(home, "Загрузки")):
        cands.append(os.path.join(d, name))
    for c in cands:
        if c and os.path.exists(c):
            return c
    return None


def main():
    src = find("wall_clean.json", sys.argv[1] if len(sys.argv) > 1 else None)
    if not src:
        sys.exit("Не нашёл wall_clean.json. Запустите так: python vk_images.py путь_к_файлу")
    posts = json.load(open(src, encoding="utf-8"))
    links_path = find("wall_links.json", os.path.join(os.path.dirname(src), "wall_links.json"))
    extra = {x["id"]: x for x in json.load(open(links_path, encoding="utf-8"))} if links_path else {}
    out = os.path.join(os.path.dirname(src), "vk_wall_local")
    os.makedirs(os.path.join(out, "images"), exist_ok=True)
    print(f"Постов: {len(posts)}. Дополнительные данные: {'есть' if extra else 'нет'}")

    seen, n, total = {}, 0, sum(len(p["imgs"]) for p in posts)
    body = []
    for p in posts:
        e = extra.get(p["id"], {})
        imgs = []
        for u in p["imgs"]:
            if u not in seen:
                n += 1
                ext = os.path.splitext(u.split("?")[0])[1][:5] or ".jpg"
                name = f"images/{len(seen):05d}{ext}"
                path = os.path.join(out, name)
                if not (os.path.exists(path) and os.path.getsize(path) > 0):
                    for attempt in range(3):
                        try:
                            req = urllib.request.Request(u, headers={"User-Agent": "Mozilla/5.0"})
                            with urllib.request.urlopen(req, timeout=60) as r, open(path, "wb") as f:
                                f.write(r.read())
                            break
                        except Exception:
                            time.sleep(1 + attempt)
                    else:
                        name = None
                seen[u] = name
                print(f"  картинка {n}/{total}   ", end="\r", flush=True)
            if seen[u]:
                imgs.append(seen[u])
        head = html.escape(e.get("date") or p["id"])
        if e.get("permalink"):
            head = f'<a href="{e["permalink"]}">{head}</a>'
        extras = "".join(f'<div><a href="{html.escape(l["href"])}">🔗 {html.escape(l.get("text") or l["href"])}</a></div>'
                         for l in e.get("links", []))
        body.append(f"<article><h4>{head}</h4><p>{html.escape(p['text']).replace(chr(10), '<br>')}</p>"
                    + "".join(f'<img src="{i}" style="max-width:100%">' for i in imgs) + extras + "</article><hr>")
    with open(os.path.join(out, "index.html"), "w", encoding="utf-8") as f:
        f.write('<!doctype html><meta charset="utf-8"><body style="max-width:720px;margin:auto;font-family:sans-serif">'
                + "".join(body))
    ok = sum(1 for v in seen.values() if v)
    print(f"\nГотово. Скачано картинок: {ok} из {len(seen)}. Откройте: {os.path.join(out, 'index.html')}")
    input("Нажмите Enter, чтобы закрыть...")


if __name__ == "__main__":
    main()
