"""Ядро: извлечение уникальных слайдов с текстом из видео."""
import os
import sys
import ctypes
from pathlib import Path

import cv2
import numpy as np

SAMPLE_SEC = 1.0      # шаг выборки кадров
STABLE_DIFF = 0.01    # кадр «устоялся», если почти не отличается от предыдущей выборки
CMP_SIZE = (160, 90)  # размер для проверки стабильности
INK_SIZE = (480, 270)  # размер для сравнения слайдов


def short_path(path: str) -> str:
    """OpenCV на Windows плохо читает кириллицу в путях — берём короткий путь 8.3."""
    if sys.platform != "win32":
        return path
    buf = ctypes.create_unicode_buffer(32768)
    n = ctypes.windll.kernel32.GetShortPathNameW(path, buf, 32768)
    return buf.value if n else path


def desktop_dir() -> Path:
    if sys.platform == "win32":
        try:
            buf = ctypes.create_unicode_buffer(1024)
            # CSIDL_DESKTOPDIRECTORY = 0x10 (учитывает OneDrive и переносы)
            ctypes.windll.shell32.SHGetFolderPathW(None, 0x10, None, 0, buf)
            if buf.value:
                return Path(buf.value)
        except Exception:
            pass
    d = Path.home() / "Desktop"
    return d if d.exists() else Path.home()


def small(gray: np.ndarray) -> np.ndarray:
    return cv2.GaussianBlur(cv2.resize(gray, CMP_SIZE, interpolation=cv2.INTER_AREA), (3, 3), 0)


def changed_fraction(a: np.ndarray, b: np.ndarray) -> float:
    return float(np.mean(cv2.absdiff(a, b) > 25))


def ink(gray: np.ndarray) -> np.ndarray:
    """Бинарная маска «чернил» (текст/линии/фигуры) независимо от цвета фона."""
    g = cv2.resize(gray, INK_SIZE, interpolation=cv2.INTER_AREA)
    bg = cv2.medianBlur(g, 21)
    m = (cv2.absdiff(g, bg) > 30).astype(np.uint8)
    return cv2.dilate(m, np.ones((3, 3), np.uint8)).astype(bool)


def same_slide(a: np.ndarray, b: np.ndarray, similarity: float) -> bool:
    """Слайды совпадают, если их «чернила» почти одинаковы либо один — дополнение другого."""
    na, nb = int(a.sum()), int(b.sum())
    if na == 0 or nb == 0:
        return na == nb
    inter = int((a & b).sum())
    jaccard = inter / (na + nb - inter)
    contain = inter / min(na, nb)
    return jaccard >= similarity or contain >= 0.92


def text_score(gray: np.ndarray) -> int:
    """Число «символоподобных» компонент (буквы, цифры, знаки). 0 — текста нет."""
    h, w = gray.shape
    if w > 1000:
        gray = cv2.resize(gray, (1000, int(h * 1000 / w)), interpolation=cv2.INTER_AREA)
    edges = cv2.Canny(gray, 80, 200)
    edges = cv2.dilate(edges, np.ones((2, 2), np.uint8))
    n, _, stats, _ = cv2.connectedComponentsWithStats(edges, connectivity=8)
    cnt = 0
    for i in range(1, n):
        x, y, cw, ch, area = stats[i]
        if 5 <= ch <= 60 and 2 <= cw <= 80 and 0.1 <= cw / ch <= 4 and area >= 8:
            cnt += 1
    return cnt


def extract_slides(video, roi, out_dir=None, similarity=0.80, min_chars=20,
                   progress=None, cancel=None):
    """
    video: путь; roi: (x, y, w, h) в пикселях видео; similarity: степень совпадения
    содержимого (0..1), с которой слайды считаются «одинаковыми».
    Возвращает (папка, число сохранённых слайдов).
    """
    cap = cv2.VideoCapture(short_path(str(video)))
    if not cap.isOpened():
        raise RuntimeError("Не удалось открыть видео")
    fps = cap.get(cv2.CAP_PROP_FPS) or 25.0
    total = int(cap.get(cv2.CAP_PROP_FRAME_COUNT)) or 0
    step = max(1, int(round(fps * SAMPLE_SEC)))
    x, y, w, h = roi

    name = Path(video).stem
    base = Path(out_dir) if out_dir else desktop_dir()
    folder = base / f"Слайды - {name}"
    k = 2
    while folder.exists():
        folder = base / f"Слайды - {name} ({k})"
        k += 1
    folder.mkdir(parents=True)

    kept = []  # {"small": ..., "score": int, "frame": ndarray}
    prev = None
    idx = 0
    try:
        while True:
            if cancel and cancel():
                break
            if not cap.grab():
                break
            if idx % step == 0:
                ok, frame = cap.retrieve()
                if ok:
                    crop = frame[y:y + h, x:x + w]
                    if crop.size:
                        gray = cv2.cvtColor(crop, cv2.COLOR_BGR2GRAY)
                        sm = small(gray)
                        stable = prev is not None and changed_fraction(sm, prev) < STABLE_DIFF
                        prev = sm
                        if stable:
                            _consider(kept, crop, gray, similarity, min_chars)
            if progress and total and idx % (step * 5) == 0:
                progress(min(idx / total, 1.0), len(kept))
            idx += 1
    finally:
        cap.release()

    for n, s in enumerate(kept, 1):
        ok, buf = cv2.imencode(".png", s["frame"])
        if ok:
            buf.tofile(str(folder / f"slide_{n:03d}.png"))
    if progress:
        progress(1.0, len(kept))
    return folder, len(kept)


def _consider(kept, crop, gray, similarity, min_chars):
    score = text_score(gray)
    if score < min_chars:
        return
    mask = ink(gray)
    for s in kept:
        if same_slide(mask, s["mask"], similarity):
            # тот же слайд: оставляем версию с большим количеством текста
            if score > s["score"]:
                s.update(mask=mask, score=score, frame=crop.copy())
            return
    kept.append({"mask": mask, "score": score, "frame": crop.copy()})
