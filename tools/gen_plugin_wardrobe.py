"""Вставляет гардероб из clawd_wardrobe.py (Clawd для ПК) в плагин Clawd+ для exteraGram — между метками
«# ==== ГАРДЕРОБ» и «# ==== КОНЕЦ ГАРДЕРОБА». Замки — как на телефоне (ачивки плагин берёт у приложения).

    py tools/gen_plugin_wardrobe.py путь/к/clawd_wardrobe.py путь/к/clawd_pet_plus.plugin
"""

import importlib.util
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from gen_wardrobe import PHONE_ACH, PHONE_NEED  # noqa: E402

START, END = "# ==== ГАРДЕРОБ", "# ==== КОНЕЦ ГАРДЕРОБА"

CODE = '''

def w_shade(c, t):
    """t < 0 — темнее, t > 0 — светлее (цвет 0xAARRGGBB)."""
    out = 0xFF000000
    for shift in (16, 8, 0):
        v = (c >> shift) & 0xFF
        v = round(v * (1 + t)) if t < 0 else round(v + (255 - v) * t)
        out |= max(0, min(255, v)) << shift
    return out


def _w_color(ch, pal, body, eye):
    if ch == "B":
        return body
    if ch == "D":
        return w_shade(body, -0.28)
    if ch == "L":
        return w_shade(body, 0.32)
    if ch == "E":
        return eye
    return pal.get(ch)


def _w_stamp(px, rows, pal, x0, y0, body, eye):
    for r, row in enumerate(rows):
        c = 0
        while c < len(row):
            ch = row[c]
            if ch == ".":
                c += 1
                continue
            end = c + 1
            while end < len(row) and row[end] == ch:
                end += 1
            color = _w_color(ch, pal, body, eye)
            if color is not None:
                px(x0 + c, y0 + r, x0 + end, y0 + r + 1, color)
            c = end


def _w_fill(px, a, b, y, ch, pal, body, eye):
    color = _w_color(ch, pal, body, eye) if b > a else None
    if color is not None:
        px(a, y, b, y + 1, color)


def w_draw(px, keys, phase, left, right, top, bottom, ey, look, body, eye, hands_free=True):
    """phase: back — до тела, pattern — по телу, front — поверх глаз и рта (как clawd_wardrobe.draw)."""
    mx = (left + right) // 2
    worn = [W_ITEMS[k] for k in keys if k in W_ITEMS]
    if phase == "back":
        for it in worn:
            if it["slot"] == "back":
                _w_stamp(px, it["rows"], it["pal"], mx - len(it["rows"][0]) // 2 + it["dx"], top + it["dy"], body, eye)
        return
    if phase == "pattern":
        for it in worn:
            if it["slot"] != "pattern":
                continue
            rows = it["rows"]
            h, w = len(rows), len(rows[0])
            for y in range(top, bottom):
                _w_stamp(px, ["".join(rows[(y - top) % h][i % w] for i in range(right - left))], it["pal"], left, y, body, eye)
        return
    order = {"face": 0, "neck": 1, "head": 2, "hand": 3}
    for it in sorted((i for i in worn if i["slot"] in order), key=lambda i: order[i["slot"]]):
        slot, pal = it["slot"], it["pal"]
        if slot == "face":
            lcx, rcx, y0 = left + 3 + look, right - 4 + look, ey - 1 + it["dy"]
            _w_stamp(px, it["left"], pal, lcx - 2, y0, body, eye)
            _w_stamp(px, it["right"], pal, rcx - 2, y0, body, eye)
            for row, ch in it["bridge"]:
                _w_fill(px, lcx + 3, rcx - 2, y0 + row, ch, pal, body, eye)
            for row, ch in it["edge"]:
                _w_fill(px, left, lcx - 2, y0 + row, ch, pal, body, eye)
                _w_fill(px, rcx + 3, right, y0 + row, ch, pal, body, eye)
        elif slot == "neck":
            _w_stamp(px, it["rows"], pal, mx - len(it["rows"][0]) // 2 + it["dx"], top + 5 + it["dy"], body, eye)
        elif slot == "head":
            _w_stamp(px, it["rows"], pal, mx - len(it["rows"][0]) // 2 + it["dx"], top - len(it["rows"]) + it["dy"], body, eye)
        elif slot == "hand" and hands_free:
            _w_stamp(px, it["rows"], pal, right + 1 + it["dx"], top + 5 - len(it["rows"]) + it["dy"], body, eye)


_W_PAD = {}


def w_pad(acc):
    """Сколько клеток запаса нужно вокруг кадра 18×13 под надетое (одинаково со всех сторон)."""
    if not acc:
        return 0
    if acc not in _W_PAD:
        keys = acc.split(",")
        b = [0, 0, GRID_W, GRID_H]

        def px(x0, y0, x1, y1, _c):
            b[0], b[1], b[2], b[3] = min(b[0], x0), min(b[1], y0), max(b[2], x1), max(b[3], y1)

        for l, r, t, bt in ((3, 15, 3, 10), (2, 16, 5, 10), (4, 14, 2, 10), (3, 15, 4, 11)):
            for look in (-1, 0, 1):
                for phase in ("back", "pattern", "front"):
                    w_draw(px, keys, phase, l, r, t, bt, t + 2, look, 0xFF000000, 0xFF000000)
        # свечение ауры выходит за всё надетое ещё на W_GLOW клеток
        g = W_GLOW if w_aura_of(keys) else 0
        _W_PAD[acc] = max(0, g - b[0], g - b[1], b[2] + g - GRID_W, b[3] + g - GRID_H)
    return _W_PAD[acc]


W_GLOW = 3  # на сколько клеток свечение ауры выходит за силуэт


def w_aura_of(keys):
    return next((W_ITEMS[k]["aura"] for k in keys if k in W_ITEMS and W_ITEMS[k]["aura"]), None)


def w_glow(px, cells, keys, phase):
    """Свечение ауры вокруг силуэта: три кольца — сплошное, «рябью», искрами (как clawd_wardrobe.glow)."""
    aura = w_aura_of(keys)
    if aura is None or not cells:
        return
    cols = aura["colors"] or [0xFFFF6FA8, 0xFFFF9FC0]
    n = len(cols)
    xs = [x for x, _ in cells]
    ys = [y for _, y in cells]
    taken = set(cells)
    for x in range(min(xs), max(xs) + 1):
        for y in range(min(ys), max(ys) + 1):
            if (x, y) not in cells and (((x - 1, y) in cells and (x + 1, y) in cells) or
                                        ((x, y - 1) in cells and (x, y + 1) in cells)):
                taken.add((x, y))
    ring = set(taken)
    for k in range(W_GLOW):
        ring = {(x + dx, y + dy) for x, y in ring for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))} - taken
        taken |= ring
        color = cols[(k + phase) % n] if n >= 4 else cols[max(0, n - 1 - k)]
        for x, y in ring:
            if k == 1 and (x + y + phase) % 2:
                continue
            if k == 2 and (x + 2 * y + phase) % 4:
                continue
            px(x, y, x + 1, y + 1, color)


# значки частиц ауры (на ПК это текст)
W_GLYPHS = {
    "dot": ["XX", "XX"],
    "ring": [".X.", "X.X", ".X."],
    "oval": [".X.", "XXX", ".X."],
    "star": [".X.", "XXX", ".X."],
    "spark": ["..X..", "..X..", "XX.XX", "..X..", "..X.."],
    "note": ["..XXX", "..X.X", "..X.X", "XXX.X", "XXX.."],
    "money": [".X.", "XXX", "X..", "XXX", "..X", "XXX", ".X."],
    "zap": ["..X", ".X.", "XXX", ".X.", "X.."],
}


def w_glyph(shape, ch):
    if shape in ("ring", "oval"):
        return shape
    if shape == "text":
        return {"♪": "note", "♫": "note", "$": "money", "ϟ": "zap", "✦": "spark"}.get(ch, "star")
    return "dot"


def w_need_text(need):
    if need is None:
        return ""
    if isinstance(need, int):
        return f"{need} " + ru_plural(need, ("ачивка", "ачивки", "ачивок"))
    return "ачивка «" + W_ACH_TITLES.get(need, need) + "»"
'''

# ачивки приложения на телефоне — для подписей замков
ACH_TITLES = {"pet100": "Гладильщик", "days7": "Неделя вместе", "days30": "Месяц дружбы", "eat50": "Обжора",
              "clean10": "Чистюля", "ball30": "Футболист", "pomo5": "Отличник", "feed20": "Кормилец",
              "bath10": "Банщик", "tag5": "Ловкач", "seek10": "Сыщик", "remind5": "Ничего не забуду",
              "throw50": "Метатель", "style10": "Модник", "style40": "Икона стиля"}
# окрасы как на телефоне: (название, тело, глаза) и замки
COLORS = [
    ("Claude", "#d97757", "#1a1a1a"), ("Мятный", "#4fbf98", "#12241d"), ("Лавандовый", "#9b8aee", "#1b1631"),
    ("Небесный", "#4fa8e8", "#0e1c29"), ("Розовый", "#ee85ae", "#2a1220"), ("Графит", "#5e6670", "#f2f2f2"),
    ("Золотой", "#e8b84a", "#2a1e05"), ("Лаймовый", "#9bd45a", "#16240a"), ("Призрак", "#e9ecf2", "#3a3a44"),
    ("Ночной", "#2e3a6e", "#f2f2f2"), ("Вишнёвый", "#c2405a", "#1a0a10"), ("Апельсин", "#f28a2e", "#2a1405"),
    ("Банан", "#f2d04a", "#2a2205"), ("Изумруд", "#2fa36b", "#08201a"), ("Бирюза", "#2bc4c4", "#062424"),
    ("Персик", "#f5b08a", "#2a160c"), ("Шоколад", "#7a4a2e", "#f2e6dc"), ("Снежный", "#ffffff", "#1a1a2a"),
    ("Сапфир", "#3156c9", "#f2f2f2"), ("Сирень", "#c08ae8", "#22142e"), ("Фуксия", "#e04fb0", "#2a0a20"),
    ("Кофе с молоком", "#b08a6a", "#24180f"), ("Песок", "#d9c08a", "#2a2010"), ("Морской", "#1f7a8c", "#e8fbff"),
    ("Клубника", "#ff5a7a", "#ffffff"), ("Оливка", "#8a9a3a", "#16180a"), ("Ледяной", "#bfe8ff", "#1a3a5a"),
    ("Розовое золото", "#e6a89a", "#3a1a14"), ("Мята-шоколад", "#a6e6c8", "#4a2a1a"), ("Тыква", "#ff8c1a", "#1a0f05"),
    ("Уголь", "#26262c", "#ff5a5a"), ("Неон", "#39ff8a", "#06200f"), ("Кибер", "#00e5ff", "#1a0033"),
    ("Лава", "#ff4a1a", "#ffe14a"),
]
COLOR_NEED = {6: "pet100", 7: "eat50", 8: "clean10", 9: "pomo5", 16: 1, 17: 2, 18: 3, 19: 4, 20: 5, 21: 6, 22: 7,
              23: 8, 24: 9, 25: 10, 26: 11, 27: 12, 28: 13, 29: 14, 30: "throw50", 31: "seek10", 32: "tag5",
              33: "style10"}


def ci(c):
    return "0xFF" + c[1:].upper()


def need(n):
    n = PHONE_NEED.get(n, n)
    assert n is None or isinstance(n, int) or n in PHONE_ACH, n
    return repr(n)


def item(it):
    pal = "{" + ", ".join(f"{k!r}: {ci(v)}" for k, v in it["pal"].items()) + "}"
    aura = "None"
    if it["aura"]:
        a = it["aura"]
        aura = (f"{{'shape': {a['shape']!r}, 'colors': [{', '.join(ci(c) for c in a['colors'])}], "
                f"'text': {a.get('text', '')!r}, 'every': {a['every']}, 'fall': {bool(a.get('fall'))}}}")
    return (f"    {it['key']!r}: {{'key': {it['key']!r}, 'slot': {it['slot']!r}, 'name': {it['name']!r}, "
            f"'need': {need(it['need'])}, 'rows': {it['rows']!r}, 'pal': {pal}, 'dx': {it['dx']}, 'dy': {it['dy']}, "
            f"'left': {it['left']!r}, 'right': {it['right']!r}, 'bridge': {[tuple(b) for b in it['bridge']]!r}, "
            f"'edge': {[tuple(e) for e in it['edge']]!r}, 'aura': {aura}, 'auto': {it['auto']}}},")


def main():
    spec = importlib.util.spec_from_file_location("clawd_wardrobe", sys.argv[1])
    w = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(w)
    out = [START + ": СГЕНЕРИРОВАНО gen_plugin_wardrobe.py из clawd_wardrobe.py (Clawd для ПК) — правь там ====",
           f"W_SLOTS = {w.SLOTS!r}", "W_SLOT_KEYS = [s for s, _, _ in W_SLOTS]", "W_ITEMS = {"]
    out += [item(it) for it in w.ITEMS.values()]
    out.append("}")
    out.append("# что можно выбрать в каждом слоте (порядок = номер в списке настроек минус 1)")
    out.append("W_SLOT_LIST = {s: [k for k, it in W_ITEMS.items() if it['slot'] == s and not it['auto']] for s in W_SLOT_KEYS}")
    out.append(f"W_ACH_TITLES = {ACH_TITLES!r}")
    out.append("W_COLORS = [" + ", ".join(f"({n!r}, {ci(b)}, {ci(e)})" for n, b, e in COLORS) + "]")
    out.append(f"W_COLOR_NEED = {COLOR_NEED!r}")
    out.append(CODE.rstrip())
    out.append(END + " ====")
    block = "\n".join(out) + "\n"

    path = Path(sys.argv[2])
    src = path.read_text(encoding="utf-8")
    a, b = src.find(START), src.find(END)
    if a >= 0 and b > a:
        src = src[:a] + block + src[src.index("\n", b) + 1:]
    else:
        # первый раз — сразу после старых аксессуаров
        anchor = src.index("\nACC_COLORS = {")
        end = src.index("\n}\n", anchor) + 3
        src = src[:end] + "\n" + block + src[end:]
    path.write_text(src, encoding="utf-8")
    print(path, len(w.ITEMS), "items")


main()
