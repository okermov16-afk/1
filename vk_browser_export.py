#!/usr/bin/env python3
"""Выгрузка группы VK БЕЗ API и без токена: через настоящий браузер.

Запуск: python vk_browser_export.py
Сам поставит нужное (playwright + браузер), откроет окно. Если VK попросит войти — войдите
в этом окне, потом вернитесь в консоль и нажмите Enter. Дальше всё автоматически:
листает стену до конца, сохраняет каждую страницу целиком (с картинками внутри файла .mhtml,
открывается в Chrome/Edge) и текстом (.txt), обходит альбомы, обсуждения, видео.
Результат: папка vk_browser_backup рядом со скриптом.
"""
import argparse
import os
import re
import subprocess
import sys
import time


def ensure_playwright():
    try:
        import playwright  # noqa: F401
    except ImportError:
        print("Ставлю playwright...")
        subprocess.run([sys.executable, "-m", "pip", "install", "-q", "playwright"], check=True)
    print("Проверяю браузер (первый раз скачивается ~150 МБ)...")
    subprocess.run([sys.executable, "-m", "playwright", "install", "chromium"])


def scroll_all(page, label):
    """Листает страницу вниз, пока подгружается новое."""
    same, last = 0, -1
    while same < 6:
        for txt in ("Показать ещё", "Показать больше", "Показать все", "Показать предыдущие"):
            try:
                btn = page.get_by_text(txt, exact=False).first
                if btn.is_visible(timeout=200):
                    btn.click(timeout=1000)
            except Exception:
                pass
        page.mouse.wheel(0, 20000)
        page.evaluate("window.scrollTo(0, document.body.scrollHeight)")
        time.sleep(1.5)
        h = page.evaluate("document.body.scrollHeight")
        same = same + 1 if h == last else 0
        last = h
        print(f"    {label}: высота страницы {h}   ", end="\r", flush=True)
    print()


def snapshot(context, page, out, name):
    os.makedirs(out, exist_ok=True)
    cdp = context.new_cdp_session(page)
    data = cdp.send("Page.captureSnapshot", {"format": "mhtml"})["data"]
    with open(os.path.join(out, name + ".mhtml"), "w", encoding="utf-8", newline="") as f:
        f.write(data)
    with open(os.path.join(out, name + ".txt"), "w", encoding="utf-8") as f:
        f.write(page.inner_text("body"))


def links(page, pattern):
    hrefs = page.eval_on_selector_all("a[href]", "els => els.map(e => e.href)")
    seen, res = set(), []
    for h in hrefs:
        m = re.search(pattern, h)
        if m and m.group(0) not in seen:
            seen.add(m.group(0))
            res.append(h.split("?")[0] if "?" in h and "z=" not in h else h)
    return res


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--group", default="manipulator_verse")
    ap.add_argument("--base", default="https://vk.ru")
    ap.add_argument("--out", default=os.path.join(os.path.dirname(os.path.abspath(__file__)), "vk_browser_backup"))
    ap.add_argument("--headless", action="store_true")
    ap.add_argument("--no-wait", action="store_true")
    ap.add_argument("--exe", help=argparse.SUPPRESS)
    a = ap.parse_args()
    ensure_playwright()
    from playwright.sync_api import sync_playwright

    with sync_playwright() as p:
        ctx = p.chromium.launch_persistent_context(os.path.join(a.out, "_profile"), headless=a.headless,
                                                   viewport={"width": 1280, "height": 900}, locale="ru-RU",
                                                   executable_path=a.exe or None)
        page = ctx.new_page()
        page.goto(f"{a.base}/{a.group}", wait_until="domcontentloaded")
        if not a.no_wait:
            input("\nЕсли в окне браузера VK просит войти — войдите. Когда видите страницу группы, нажмите Enter здесь: ")
        gid = None
        out = a.out

        print("== Стена")
        scroll_all(page, "стена")
        snapshot(ctx, page, out, "wall")
        html = page.content()
        m = re.search(r"-(\d{5,})_\d+", html)
        gid = m.group(1) if m else None
        album_links = links(page, r"album-\d+_\d+")
        topic_links = links(page, r"topic-\d+_\d+")
        video_links = links(page, r"video-\d+_\d+")

        sections = [("albums", f"{a.base}/albums-{gid}" if gid else None),
                    ("board", f"{a.base}/board{gid}" if gid else None),
                    ("videos", f"{a.base}/videos-{gid}" if gid else None),
                    ("docs", f"{a.base}/docs-{gid}" if gid else None)]
        for name, url in sections:
            if not url:
                continue
            print(f"== {name}")
            try:
                page.goto(url, wait_until="domcontentloaded")
                time.sleep(2)
                scroll_all(page, name)
                snapshot(ctx, page, out, name)
                album_links += links(page, r"album-\d+_\d+") if name == "albums" else []
                topic_links += links(page, r"topic-\d+_\d+") if name == "board" else []
                video_links += links(page, r"video-\d+_\d+") if name == "videos" else []
            except Exception as e:
                print("   пропущено:", e)

        for kind, items in (("album", album_links), ("topic", topic_links)):
            items = list(dict.fromkeys(items))
            for i, url in enumerate(items, 1):
                print(f"== {kind} {i}/{len(items)}")
                try:
                    page.goto(url, wait_until="domcontentloaded")
                    time.sleep(2)
                    scroll_all(page, kind)
                    snapshot(ctx, page, os.path.join(out, kind + "s"), f"{i:03d}_" + re.sub(r"\W+", "_", url.split("/")[-1])[:60])
                except Exception as e:
                    print("   пропущено:", e)

        video_links = list(dict.fromkeys(video_links))
        with open(os.path.join(out, "video_links.txt"), "w", encoding="utf-8") as f:
            f.write("\n".join(video_links))
        ctx.close()

    if video_links:
        print(f"== Видео: {len(video_links)} шт.")
        subprocess.run([sys.executable, "-m", "pip", "install", "-q", "yt-dlp"])
        subprocess.run([sys.executable, "-m", "yt_dlp", "-q", "--no-warnings", "-a", os.path.join(out, "video_links.txt"),
                        "-o", os.path.join(out, "videos", "%(id)s.%(ext)s")])
    print(f"\nГотово. Откройте файлы .mhtml из папки {out} в Chrome/Edge (wall.mhtml — стена).")
    if not a.no_wait:
        input("Нажмите Enter, чтобы закрыть окно...")


if __name__ == "__main__":
    main()
