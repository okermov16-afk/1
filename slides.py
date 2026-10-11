"""Ядро: извлечение уникальных слайдов с текстом из видео."""
import os
import sys
import ctypes
from concurrent.futures import ProcessPoolExecutor
from pathlib import Path

import cv2
import numpy as np

SAMPLE_SEC = 0.5      # шаг выборки кадров
STABLE_DIFF = 0.01    # кадр «устоялся», если почти не отличается от предыдущей выборки
CMP_SIZE = (160, 90)  # размер для проверки стабильности
INK_SIZE = (640, 360)  # размер для сравнения слайдов


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


BLOCK = 20


def ink(gray: np.ndarray):
    """Слайд как набор блоков: нормированные патчи + маска блоков, где есть содержимое."""
    g = cv2.GaussianBlur(cv2.resize(gray, INK_SIZE, interpolation=cv2.INTER_AREA), (3, 3), 0)
    g = g.astype(np.float32)
    h, w = g.shape
    rows, cols = h // BLOCK, w // BLOCK
    p = g[:rows * BLOCK, :cols * BLOCK].reshape(rows, BLOCK, cols, BLOCK).swapaxes(1, 2)
    p = p.reshape(rows * cols, BLOCK * BLOCK)
    p = p - p.mean(axis=1, keepdims=True)
    norm = np.linalg.norm(p, axis=1)
    has = norm > 8 * BLOCK  # контраст блока (std > ~8)
    p = np.where(has[:, None], p / np.maximum(norm, 1e-6)[:, None], 0).astype(np.float32)
    return p, has


def _cover(a, b) -> float:
    """Какая доля содержательных блоков a совпадает с тем же блоком в b."""
    (pa, ha), (pb, hb) = a, b
    n = int(ha.sum())
    if n == 0:
        return 1.0
    corr = (pa[ha] * pb[ha]).sum(axis=1)
    return float(np.count_nonzero((corr > 0.8) & hb[ha])) / n


def same_slide(a, b, similarity: float) -> bool:
    """Слайды совпадают, если содержимое почти всех блоков одинаково,
    либо один — «достроенная» версия другого."""
    na, nb = int(a[1].sum()), int(b[1].sum())
    if na == 0 or nb == 0:
        return na == nb
    ab, ba = _cover(a, b), _cover(b, a)
    if min(ab, ba) >= similarity:
        return True
    small_cov = ab if na <= nb else ba
    return small_cov >= 0.96 and max(na, nb) >= 1.15 * min(na, nb)


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
        if 5 <= ch <= 150 and 2 <= cw <= 200 and 0.1 <= cw / ch <= 4 and area >= 8:
            cnt += 1
    return cnt


def _scan_segment(args):
    """Обрабатывает кадры [start, end) (кратные step). Возвращает кандидатов по порядку времени."""
    video, roi, start, end, step, similarity, min_chars = args
    fps = cv2.VideoCapture(short_path(str(video))).get(cv2.CAP_PROP_FPS) or 25.0
    cap = cv2.VideoCapture(short_path(str(video)))
    x, y, w, h = roi
    kept, prev, log = [], None, []
    # один кадр до начала — чтобы на границе работала проверка «кадр устоялся»
    first = max(0, start - step)
    if first:
        cap.set(cv2.CAP_PROP_POS_FRAMES, first)
    idx = first
    try:
        while idx < end:
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
                        if stable and idx >= start:
                            _consider(kept, crop, gray, similarity, min_chars, idx / fps, log)
            idx += 1
    finally:
        cap.release()
    return kept, log


def extract_slides(video, roi, out_dir=None, similarity=0.85, min_chars=10,
                   progress=None, cancel=None, workers=None):
    """
    video: путь; roi: (x, y, w, h) в пикселях видео; similarity: степень совпадения
    содержимого (0..1), с которой слайды считаются «одинаковыми».
    Видео режется на куски, они обрабатываются параллельно, результат собирается
    строго в порядке времени. Возвращает (папка, число сохранённых слайдов).
    """
    cap = cv2.VideoCapture(short_path(str(video)))
    if not cap.isOpened():
        raise RuntimeError("Не удалось открыть видео")
    fps = cap.get(cv2.CAP_PROP_FPS) or 25.0
    total = int(cap.get(cv2.CAP_PROP_FRAME_COUNT)) or 0
    cap.release()
    if total <= 0:
        raise RuntimeError("Не удалось определить длину видео")
    step = max(1, int(round(fps * SAMPLE_SEC)))
    workers = workers or max(1, os.cpu_count() or 1)

    name = Path(video).stem
    base = Path(out_dir) if out_dir else desktop_dir()
    folder = base / f"Слайды - {name}"
    k = 2
    while folder.exists():
        folder = base / f"Слайды - {name} ({k})"
        k += 1

    # куски по границам, кратным step; кусков больше, чем процессов — для баланса и прогресса
    n_seg = min(max(workers * 4, 1), max(1, total // (step * 10)))
    seg_len = -(-total // n_seg)
    seg_len = -(-seg_len // step) * step
    tasks = [(video, roi, s, min(s + seg_len, total), step, similarity, min_chars)
             for s in range(0, total, seg_len)]

    kept, log = [], []
    done = 0
    if workers == 1 or len(tasks) == 1:
        results = (_scan_segment(t) for t in tasks)
        pool = None
    else:
        pool = ProcessPoolExecutor(max_workers=workers)
        results = (f.result() for f in [pool.submit(_scan_segment, t) for t in tasks])
    try:
        for seg, seg_log in results:  # строго по порядку кусков
            log.extend(seg_log)
            if cancel and cancel():
                break
            for c in seg:  # слияние и удаление дублей на стыках
                _merge(kept, c, similarity)
            done += 1
            if progress:
                progress(done / len(tasks), len(kept))
    finally:
        if pool:
            pool.shutdown(wait=False, cancel_futures=True)

    folder.mkdir(parents=True)
    for n, s in enumerate(kept, 1):
        ok, buf = cv2.imencode(".png", s["frame"])
        if ok:
            buf.tofile(str(folder / f"slide_{n:03d}.png"))
    lines = [f"slide_{n:03d}: {_ts(s['t'])}" for n, s in enumerate(kept, 1)]
    lines += ["", "Пропущенные кадры (время — причина):"]
    lines += [f"{_ts(t)} — {why}" for t, why in sorted(log)]
    (folder / "report.txt").write_text("\n".join(lines), encoding="utf-8")
    if progress:
        progress(1.0, len(kept))
    return folder, len(kept)


def _ts(t):
    t = int(t)
    return f"{t // 3600}:{t % 3600 // 60:02d}:{t % 60:02d}"


def _merge(kept, c, similarity):
    for s in kept:
        if same_slide(c["mask"], s["mask"], similarity):
            if c["score"] > s["score"]:
                s.update(c)
            return
    kept.append(c)


def _consider(kept, crop, gray, similarity, min_chars, t, log):
    score = text_score(gray)
    if score < min_chars:
        if score >= 3:  # почти текст — пригодится для отладки
            log.append((t, f"мало символов ({score} < {min_chars})"))
        return
    mask = ink(gray)
    for n, s in enumerate(kept, 1):
        if same_slide(mask, s["mask"], similarity):
            # тот же слайд: оставляем версию с большим количеством текста
            if score > s["score"]:
                s.update(mask=mask, score=score, frame=crop.copy())
            log.append((t, "похож на уже найденный слайд"))
            return
    kept.append({"mask": mask, "score": score, "frame": crop.copy(), "t": t})
