"""Генерирует app/src/main/java/com/clawd/phone/WardrobeData.kt из clawd_wardrobe.py (Clawd для ПК),
чтобы одежда на телефоне и компе была одна и та же.

    py tools/gen_wardrobe.py путь/к/clawd_wardrobe.py
"""

import importlib.util
import sys
from pathlib import Path

# ачивки ПК, которых нет на телефоне → столько ачивок всего (или телефонная с тем же смыслом)
PHONE_NEED = {"pet10": 1, "ask10": 2, "file10": 3, "ride10": 3, "ask50": 5, "ghost5": 6, "throw20": "throw50"}
PHONE_ACH = {"pet100", "days7", "days30", "eat50", "clean10", "ball30", "pomo5", "feed20", "bath10", "tag5", "seek10",
             "remind5", "throw50", "style10", "style40"}


def kstr(s):
    return '"' + s.replace("\\", "\\\\").replace('"', '\\"').replace("$", "\\$") + '"'


def kcolor(c):
    return f"0xFF{c[1:].upper()}.toInt()"


def klist(rows):
    return "listOf(" + ", ".join(kstr(r) for r in rows) + ")" if rows else "emptyList()"


def need(n):
    n = PHONE_NEED.get(n, n)
    if n is None:
        return "null"
    if isinstance(n, int):
        return str(n)
    assert n in PHONE_ACH, n
    return kstr(n)


def item(it):
    args = [kstr(it["key"]), kstr(it["slot"]), kstr(it["name"]), need(it["need"])]
    if it["rows"]:
        args.append("rows = " + klist(it["rows"]))
    if it["pal"]:
        args.append("pal = mapOf(" + ", ".join(f"'{k}' to {kcolor(v)}" for k, v in it["pal"].items()) + ")")
    for f in ("dx", "dy"):
        if it[f]:
            args.append(f"{f} = {it[f]}")
    for f in ("left", "right"):
        if it[f]:
            args.append(f"{f} = " + klist(it[f]))
    for f in ("bridge", "edge"):
        if it[f]:
            args.append(f"{f} = listOf(" + ", ".join(f"{r} to '{c}'" for r, c in it[f]) + ")")
    if it["aura"]:
        a = it["aura"]
        cols = "intArrayOf(" + ", ".join(kcolor(c) for c in a["colors"]) + ")"
        args.append(f"aura = WAura({kstr(a['shape'])}, {cols}, {kstr(a.get('text', ''))}, {a['every']}f, "
                    f"{'true' if a.get('fall') else 'false'})")
    if it["auto"]:
        args.append("auto = true")
    if it.get("season"):
        args.append("season = " + kstr(it["season"]))
    return "        WItem(" + ", ".join(args) + "),"


def main():
    src = Path(sys.argv[1])
    spec = importlib.util.spec_from_file_location("clawd_wardrobe", src)
    w = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(w)
    out = ["package com.clawd.phone", "",
           "// СГЕНЕРИРОВАНО tools/gen_wardrobe.py из clawd_wardrobe.py (Clawd для ПК) — правь там, потом перегенерируй.", "",
           "object WardrobeData {",
           "    val SLOTS = listOf(" + ", ".join(f"Triple({kstr(k)}, {kstr(n)}, {kstr(i)})" for k, n, i in w.SLOTS) + ")",
           "    val SEASONS = listOf(" + ", ".join(f"Triple({kstr(k)}, {kstr(n)}, {kstr(i)})" for k, n, i in w.SEASONS) + ")", ""]
    # по функции на слот: один огромный список не влезет в метод JVM
    for slot in w.SLOT_KEYS:
        out.append(f"    private fun {slot}() = listOf(")
        out += [item(it) for it in w.ITEMS.values() if it["slot"] == slot]
        out.append("    )")
        out.append("")
    out.append("    val ITEMS: Map<String, WItem> = (" + " + ".join(f"{s}()" for s in w.SLOT_KEYS) + ").associateBy { it.key }")
    out.append("")
    # праздничные украшения в уголке: «*» и буквы со значением «*» — мигающие огоньки
    out.append("    val DECORS = listOf(")
    for key, (rows, colors) in w.DECOR.items():
        pal = ", ".join(f"'{k}' to {kcolor(v)}" for k, v in colors.items() if v != "*")
        lights = "*" + "".join(k for k, v in colors.items() if v == "*")
        out.append(f"        WDecor({kstr(key)}, {kstr(w.DECOR_NAMES[key])}, {klist(rows)}, mapOf({pal}), {kstr(lights)}, "
                   f"intArrayOf({', '.join(kcolor(c) for c in w.DECOR_LIGHTS[key])})),")
    out.append("    ).associateBy { it.key }")
    out.append("}")
    dst = Path(__file__).resolve().parent.parent / "app/src/main/java/com/clawd/phone/WardrobeData.kt"
    dst.write_text("\n".join(out) + "\n", encoding="utf-8")
    print(dst, len(w.ITEMS), "items")


if __name__ == "__main__":
    main()
