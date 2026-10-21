#!/usr/bin/env python3
"""Сохраняет СТЕНУ группы VK (все посты) через браузер. Без токена и API.

Запуск: python vk_wall.py
1) откроется браузер; войдите в VK, если просит, и откройте страницу группы
2) вернитесь в консоль, нажмите Enter
3) скрипт сам листает стену до конца и сохраняет результат в папку vk_wall_backup
"""
import argparse
import os
import re
import subprocess
import sys
import time


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--group", default="manipulator_verse")
    ap.add_argument("--base", default="https://vk.ru")
    ap.add_argument("--out", default=os.path.join(os.path.dirname(os.path.abspath(__file__)), "vk_wall_backup"))
    ap.add_argument("--headless", action="store_true", help=argparse.SUPPRESS)
    ap.add_argument("--no-wait", action="store_true", help=argparse.SUPPRESS)
    ap.add_argument("--exe", help=argparse.SUPPRESS)
    a = ap.parse_args()
    if not a.exe:
        try:
            import playwright  # noqa: F401
        except ImportError:
            print("Ставлю playwright...")
            subprocess.run([sys.executable, "-m", "pip", "install", "-q", "playwright"], check=True)
        print("Проверяю браузер (первый раз ~150 МБ, подождите)...")
        subprocess.run([sys.executable, "-m", "playwright", "install", "chromium"])
    from playwright.sync_api import sync_playwright

    os.makedirs(a.out, exist_ok=True)
    url = f"{a.base}/{a.group}"
    with sync_playwright() as p:
        ctx = p.chromium.launch_persistent_context(os.path.join(a.out, "_profile"), headless=a.headless,
                                                   viewport={"width": 1280, "height": 900}, locale="ru-RU",
                                                   executable_path=a.exe or None)
        page = ctx.new_page()
        page.goto(url, wait_until="domcontentloaded")
        if not a.no_wait:
            input("\nВойдите в VK в окне браузера (если просит) и дождитесь, пока откроется стена группы\n"
                  "с постами. Потом нажмите Enter здесь: ")
        if a.group not in page.url:
            page.goto(url, wait_until="domcontentloaded")
            time.sleep(3)

        def count():
            return len(set(re.findall(r"wall-?\d+_\d+", page.content())))

        def save():
            cdp = ctx.new_cdp_session(page)
            data = cdp.send("Page.captureSnapshot", {"format": "mhtml"})["data"]
            with open(os.path.join(a.out, "wall.mhtml"), "w", encoding="utf-8", newline="") as f:
                f.write(data)
            with open(os.path.join(a.out, "wall.txt"), "w", encoding="utf-8") as f:
                f.write(page.inner_text("body"))

        print("Листаю стену. Не закрывайте окно и ничего в нём не нажимайте.")
        same, last, rounds = 0, -1, 0
        while same < 8:
            for txt in ("Показать ещё", "Показать больше", "Показать предыдущие"):
                try:
                    b = page.get_by_text(txt).first
                    if b.is_visible(timeout=200):
                        b.click(timeout=1000)
                except Exception:
                    pass
            page.mouse.wheel(0, 20000)
            page.evaluate("window.scrollTo(0, document.body.scrollHeight)")
            time.sleep(1.5)
            c = count()
            same = same + 1 if c == last else 0
            last, rounds = c, rounds + 1
            print(f"  найдено постов: {c}   ", end="\r", flush=True)
            if rounds % 25 == 0:
                save()  # промежуточное сохранение
        print()
        save()
        ctx.close()
    print(f"\nГотово. Постов найдено: {last}. Откройте в Chrome/Edge: {os.path.join(a.out, 'wall.mhtml')}")
    print(f"Текст всех постов: {os.path.join(a.out, 'wall.txt')}")
    if not a.no_wait:
        input("Нажмите Enter, чтобы закрыть...")


if __name__ == "__main__":
    main()
