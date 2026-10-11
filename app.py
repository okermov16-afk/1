"""Slide Grabber: выделяешь рамкой презентацию на видео — получаешь папку уникальных слайдов."""
import multiprocessing
import threading
import tkinter as tk
from tkinter import filedialog, messagebox, ttk

import cv2
from PIL import Image, ImageTk

import slides

CANVAS_W, CANVAS_H = 960, 540


class App(tk.Tk):
    def __init__(self):
        super().__init__()
        self.title("Slide Grabber")
        self.resizable(False, False)
        self.video = None
        self.frame_size = None   # (w, h) исходного кадра
        self.scale = 1.0
        self.offset = (0, 0)
        self.rect = None         # (x0, y0, x1, y1) в координатах canvas
        self.pcap = None         # постоянный захват для предпросмотра
        self.fps = 25.0
        self.cap_total = 1
        self._seek_job = None
        self.cancel_flag = False
        self.worker = None
        self._photo = None

        top = ttk.Frame(self, padding=8)
        top.pack(fill="x")
        ttk.Button(top, text="Открыть видео…", command=self.open_video).pack(side="left")
        self.path_lbl = ttk.Label(top, text="Файл не выбран")
        self.path_lbl.pack(side="left", padx=8)

        self.canvas = tk.Canvas(self, width=CANVAS_W, height=CANVAS_H, bg="#222", highlightthickness=0, cursor="cross")
        self.canvas.pack(padx=8)
        self.canvas.bind("<ButtonPress-1>", self.on_press)
        self.canvas.bind("<B1-Motion>", self.on_drag)

        self.start_var = tk.DoubleVar(value=0.0)
        self.end_var = tk.DoubleVar(value=1.0)
        self.start_lbl = self._slider_row("Начало:", self.start_var, lambda v: self.on_slider("start"))
        self.end_lbl = self._slider_row("Конец:   ", self.end_var, lambda v: self.on_slider("end"))

        opts = ttk.Frame(self, padding=(8, 4))
        opts.pack(fill="x")
        ttk.Label(opts, text="Строгость отсева похожих (выше — меньше слайдов):").pack(side="left")
        self.sim = tk.DoubleVar(value=0.85)
        ttk.Scale(opts, from_=0.6, to=0.97, variable=self.sim, length=200).pack(side="left", padx=8)
        self.sim_lbl = ttk.Label(opts, text="85%", width=5)
        self.sim_lbl.pack(side="left")
        self.sim.trace_add("write", lambda *a: self.sim_lbl.config(text=f"{int(self.sim.get() * 100)}%"))

        bot = ttk.Frame(self, padding=8)
        bot.pack(fill="x")
        self.go_btn = ttk.Button(bot, text="Сделать снимки слайдов", command=self.start)
        self.go_btn.pack(side="left")
        self.stop_btn = ttk.Button(bot, text="Стоп", command=self.stop, state="disabled")
        self.stop_btn.pack(side="left", padx=6)
        self.bar = ttk.Progressbar(bot, length=400)
        self.bar.pack(side="left", padx=8)
        self.status = ttk.Label(bot, text="Выберите видео и обведите рамкой презентацию")
        self.status.pack(side="left")

    def _slider_row(self, text, var, cmd):
        row = ttk.Frame(self, padding=(8, 2))
        row.pack(fill="x")
        ttk.Label(row, text=text, width=8).pack(side="left")
        ttk.Scale(row, from_=0, to=1, variable=var, command=cmd).pack(side="left", fill="x", expand=True, padx=8)
        lbl = ttk.Label(row, text="0:00:00", width=8)
        lbl.pack(side="left")
        return lbl

    # --- видео и рамка ---
    def open_video(self):
        p = filedialog.askopenfilename(title="Видео", filetypes=[
            ("Видео", "*.mp4 *.mkv *.avi *.mov *.webm *.wmv *.flv *.m4v"), ("Все файлы", "*.*")])
        if not p:
            return
        cap = cv2.VideoCapture(slides.short_path(p))
        if not cap.isOpened():
            messagebox.showerror("Ошибка", "Не удалось открыть видео")
            return
        if self.pcap:
            self.pcap.release()
        self.pcap = cap
        self.cap_total = int(cap.get(cv2.CAP_PROP_FRAME_COUNT)) or 1
        self.frame_size = (int(cap.get(cv2.CAP_PROP_FRAME_WIDTH)), int(cap.get(cv2.CAP_PROP_FRAME_HEIGHT)))
        self.fps = cap.get(cv2.CAP_PROP_FPS) or 25.0
        self.video = p
        self.path_lbl.config(text=p)
        self.rect = None
        self.start_var.set(0.0)
        self.end_var.set(1.0)
        self.start_lbl.config(text=self.fmt(0))
        self.end_lbl.config(text=self.fmt(self.cap_total - 1))
        self.show_frame(0.1)

    def frame_of(self, v):
        return int(v * (self.cap_total - 1))

    def fmt(self, f):
        t = int(f / self.fps)
        return f"{t // 3600}:{t % 3600 // 60:02d}:{t % 60:02d}"

    def on_slider(self, which):
        if not self.video:
            return
        s, e = self.start_var.get(), self.end_var.get()
        if which == "start" and s >= e:      # начало не может быть правее конца
            s = max(0.0, e - 0.001)
            self.start_var.set(s)
        elif which == "end" and e <= s:
            e = min(1.0, s + 0.001)
            self.end_var.set(e)
        self.start_lbl.config(text=self.fmt(self.frame_of(s)))
        self.end_lbl.config(text=self.fmt(self.frame_of(e)))
        pos = s if which == "start" else e
        if self._seek_job:
            self.after_cancel(self._seek_job)
        self._seek_job = self.after(60, lambda: self.show_frame(pos))

    def show_frame(self, pos):
        if not self.pcap:
            return
        target = self.frame_of(pos)
        frame = None
        for back in (0, 15, 60, 150):  # если кадр не читается — берём чуть раньше
            self.pcap.set(cv2.CAP_PROP_POS_FRAMES, max(0, target - back))
            ok, f = self.pcap.read()
            if ok:
                frame = f
                break
        if frame is None:
            return  # оставляем прежний кадр на экране
        fw, fh = self.frame_size
        self.scale = min(CANVAS_W / fw, CANVAS_H / fh)
        dw, dh = int(fw * self.scale), int(fh * self.scale)
        self.offset = ((CANVAS_W - dw) // 2, (CANVAS_H - dh) // 2)
        img = Image.fromarray(cv2.cvtColor(cv2.resize(frame, (dw, dh)), cv2.COLOR_BGR2RGB))
        self._photo = ImageTk.PhotoImage(img)
        self.canvas.delete("all")
        self.canvas.create_image(*self.offset, anchor="nw", image=self._photo)
        self.draw_rect()

    def clamp(self, x, y):
        ox, oy = self.offset
        fw, fh = self.frame_size
        return (min(max(x, ox), ox + fw * self.scale), min(max(y, oy), oy + fh * self.scale))

    def on_press(self, e):
        if self.video:
            x, y = self.clamp(e.x, e.y)
            self.rect = (x, y, x, y)

    def on_drag(self, e):
        if self.video and self.rect:
            x, y = self.clamp(e.x, e.y)
            self.rect = (self.rect[0], self.rect[1], x, y)
            self.draw_rect()

    def draw_rect(self):
        self.canvas.delete("rect")
        if self.rect:
            self.canvas.create_rectangle(*self.rect, outline="#00ff66", width=2, tags="rect")

    def roi(self):
        if not self.rect:
            return None
        ox, oy = self.offset
        x0, x1 = sorted((self.rect[0], self.rect[2]))
        y0, y1 = sorted((self.rect[1], self.rect[3]))
        fw, fh = self.frame_size
        x = int((x0 - ox) / self.scale)
        y = int((y0 - oy) / self.scale)
        w = int((x1 - x0) / self.scale)
        h = int((y1 - y0) / self.scale)
        x, y = max(0, x), max(0, y)
        w, h = min(w, fw - x), min(h, fh - y)
        return (x, y, w, h) if w >= 50 and h >= 50 else None

    # --- обработка ---
    def start(self):
        if not self.video:
            return messagebox.showinfo("Slide Grabber", "Сначала откройте видео")
        roi = self.roi()
        if not roi:
            return messagebox.showinfo("Slide Grabber", "Обведите мышкой рамку вокруг презентации")
        self.cancel_flag = False
        self.go_btn.config(state="disabled")
        self.stop_btn.config(state="normal")
        self.bar["value"] = 0
        self.worker = threading.Thread(target=self.run, args=(roi, self.sim.get(), self.frame_of(self.start_var.get()), self.frame_of(self.end_var.get())), daemon=True)
        self.worker.start()

    def stop(self):
        self.cancel_flag = True

    def run(self, roi, sim, start_f, end_f):
        try:
            folder, n = slides.extract_slides(
                self.video, roi, similarity=sim, start_frame=start_f, end_frame=end_f,
                progress=lambda p, k: self.after(0, self.update_progress, p, k),
                cancel=lambda: self.cancel_flag)
            self.after(0, self.done, folder, n, None)
        except Exception as ex:
            self.after(0, self.done, None, 0, ex)

    def update_progress(self, p, k):
        self.bar["value"] = p * 100
        self.status.config(text=f"Найдено слайдов: {k}")

    def done(self, folder, n, err):
        self.go_btn.config(state="normal")
        self.stop_btn.config(state="disabled")
        if err:
            self.status.config(text="Ошибка")
            return messagebox.showerror("Ошибка", str(err))
        self.status.config(text=f"Готово: {n} слайдов")
        messagebox.showinfo("Готово", f"Сохранено слайдов: {n}\nПапка на рабочем столе:\n{folder}")


if __name__ == "__main__":
    multiprocessing.freeze_support()
    App().mainloop()
