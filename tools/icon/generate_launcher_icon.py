"""Generates every file that makes up the Reevz Drip launcher icon.

    python tools/icon/generate_launcher_icon.py            # write the resources
    python tools/icon/generate_launcher_icon.py --preview  # just render a look-at-it sheet

Needs Pillow, and nothing else. It is deliberately not wired into the Gradle build: the icon
changes roughly never, and a build that shells out to Python would not be configuration-cache
safe.

Why this exists at all. An adaptive icon is three vector layers, but `minSdk 24` predates
adaptive icons, so API 24-25 also need ten flat bitmaps. Drawing the tee twice -- once as path
data, once in an image editor -- is two things that silently drift apart. Here the bitmaps are
rasterised from the same outline the vector is emitted from, so editing the shape below and
re-running this is the only supported way to change the icon. Hand-editing
`ic_launcher_foreground.xml` will leave the ten `mipmap-*dpi` files showing the previous tee.
"""

from __future__ import annotations

import math
import pathlib
import sys

from PIL import Image, ImageDraw

RES = pathlib.Path(__file__).resolve().parents[2] / "app/src/main/res"

# Both colours are lifted from ui/theme/Color.kt so the icon and the app read as one thing.
BACKGROUND = "#FF2E2E31"   # SlateInk    - the app's accent
FOREGROUND = "#FFFAFAFB"   # StoneGround - the app's off-white
BG_RGBA = (0x2E, 0x2E, 0x31, 255)
FG_RGBA = (0xFA, 0xFA, 0xFB, 255)

VIEWPORT = 108.0   # adaptive icons are authored on a 108dp canvas...
VISIBLE = 72.0     # ...of which only the central 72dp is ever guaranteed to be on screen.
SAFE_RADIUS = 36.0
SCALE = 0.84       # how much of that visible 72dp the tee fills, leaving a margin
SUPERSAMPLE = 8    # rasterise this many times over, then downsample, for clean edges

# The outline, clockwise from the left neck point, in unscaled 108dp coordinates. Each entry is
# (point, corner radius, edge leading to the next point). "L" is a straight edge; the one tuple
# is the cubic that scoops out the neckline.
_NECK = ("C", (60.0, 35.5), (48.0, 35.5))
_OUTLINE = [
    ((45.0, 27.0), 2.5, "L"),    # left neck
    ((33.0, 31.5), 5.0, "L"),    # left shoulder / sleeve top
    ((22.0, 49.0), 4.0, "L"),    # left sleeve outer hem
    ((35.5, 55.0), 5.0, "L"),    # left armpit
    ((35.5, 82.0), 4.0, "L"),    # left hem
    ((72.5, 82.0), 4.0, "L"),    # right hem
    ((72.5, 55.0), 5.0, "L"),    # right armpit
    ((86.0, 49.0), 4.0, "L"),    # right sleeve outer hem
    ((75.0, 31.5), 5.0, "L"),    # right shoulder / sleeve top
    ((63.0, 27.0), 2.5, _NECK),  # right neck, then the neckline back to the start
]


def _scaled(p):
    c = VIEWPORT / 2
    return (c + (p[0] - c) * SCALE, c + (p[1] - c) * SCALE)


def _toward(origin, target, distance):
    dx, dy = target[0] - origin[0], target[1] - origin[1]
    d = math.hypot(dx, dy)
    return (origin[0] + dx / d * distance, origin[1] + dy / d * distance)


def _nodes():
    """The outline, scaled, with each corner trimmed back along both of its edges."""
    nodes = [
        {"p": _scaled(p), "r": r * SCALE,
         "edge": e if e == "L" else ("C", _scaled(e[1]), _scaled(e[2]))}
        for p, r, e in _OUTLINE
    ]
    n = len(nodes)
    for i, node in enumerate(nodes):
        prev, nxt = nodes[(i - 1) % n], nodes[(i + 1) % n]
        # Aim at the neighbouring point on a straight edge, at the adjacent control point on the
        # curved one, so the rounding follows the curve's tangent rather than its chord.
        back = prev["p"] if prev["edge"] == "L" else prev["edge"][2]
        fwd = nxt["p"] if node["edge"] == "L" else node["edge"][1]
        node["in"] = _toward(node["p"], back, node["r"])
        node["out"] = _toward(node["p"], fwd, node["r"])
    return nodes


def _n(x):
    return f"{x:.2f}".rstrip("0").rstrip(".")


def path_data() -> str:
    """The outline as VectorDrawable pathData."""
    nodes = _nodes()
    n = len(nodes)
    out = [f"M{_n(nodes[0]['out'][0])},{_n(nodes[0]['out'][1])}"]
    for i in range(n):
        node, nxt = nodes[i], nodes[(i + 1) % n]
        if node["edge"] == "L":
            out.append(f"L{_n(nxt['in'][0])},{_n(nxt['in'][1])}")
        else:
            _, c1, c2 = node["edge"]
            out.append(f"C{_n(c1[0])},{_n(c1[1])} {_n(c2[0])},{_n(c2[1])} "
                       f"{_n(nxt['in'][0])},{_n(nxt['in'][1])}")
        out.append(f"Q{_n(nxt['p'][0])},{_n(nxt['p'][1])} {_n(nxt['out'][0])},{_n(nxt['out'][1])}")
    return "".join(out) + "Z"


def _bezier(points, steps):
    """Sample a quadratic or cubic Bezier, excluding its first point."""
    n = len(points) - 1
    for s in range(1, steps + 1):
        t = s / steps
        x = y = 0.0
        for i, p in enumerate(points):
            b = math.comb(n, i) * (1 - t) ** (n - i) * t**i
            x += b * p[0]
            y += b * p[1]
        yield (x, y)


def polygon():
    """The same outline sampled as points, for rasterising."""
    nodes = _nodes()
    n = len(nodes)
    pts = [nodes[0]["out"]]
    for i in range(n):
        node, nxt = nodes[i], nodes[(i + 1) % n]
        if node["edge"] == "L":
            pts.append(nxt["in"])
        else:
            _, c1, c2 = node["edge"]
            pts.extend(_bezier([node["out"], c1, c2, nxt["in"]], 24))
        pts.extend(_bezier([nxt["in"], nxt["p"], nxt["out"]], 10))
    return pts


def furthest_from_centre():
    """How close the tee comes to the edge of the circle a launcher mask is guaranteed to keep."""
    c = VIEWPORT / 2
    p = max(polygon(), key=lambda q: math.hypot(q[0] - c, q[1] - c))
    return math.hypot(p[0] - c, p[1] - c), p


def render(size, background, tee, mask=None):
    """Rasterise the visible 72dp of the canvas at `size` px, optionally masked to a shape."""
    big = size * SUPERSAMPLE
    k = big / VISIBLE
    off = (VIEWPORT - VISIBLE) / 2.0

    im = Image.new("RGBA", (big, big), background or (0, 0, 0, 0))
    ImageDraw.Draw(im).polygon([((x - off) * k, (y - off) * k) for x, y in polygon()], fill=tee)

    if mask:
        m = Image.new("L", (big, big), 0)
        d = ImageDraw.Draw(m)
        if mask == "circle":
            d.ellipse([0, 0, big - 1, big - 1], fill=255)
        else:
            d.rounded_rectangle([0, 0, big - 1, big - 1], radius=int(big * 0.22), fill=255)
        im.putalpha(Image.composite(im.getchannel("A"), Image.new("L", (big, big), 0), m))

    return im.resize((size, size), Image.LANCZOS)


_HEADER = """<?xml version="1.0" encoding="utf-8"?>
<!--
  {what}

  GENERATED by tools/icon/generate_launcher_icon.py. Change the shape there and re-run it;
  editing this path by hand leaves the ten mipmap-*dpi bitmaps showing the previous tee.

  Every point sits inside the 72dp circle an adaptive icon actually guarantees (the furthest
  is {r:.1f} of {safe:.0f}), so no launcher mask can clip a sleeve.
-->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path
        android:fillColor="{fill}"
        android:pathData="{path}" />
</vector>
"""

_ADAPTIVE = """<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
    <monochrome android:drawable="@drawable/ic_launcher_monochrome" />
</adaptive-icon>
"""

_LAYERS = {
    "ic_launcher_background": (
        "Launcher icon, background layer: SlateInk, the app's accent colour. Flat on purpose,\n"
        "  because a gradient is noise at 48px and sibling app Reevz Mealz is flat too.",
        BACKGROUND, "M0,0h108v108h-108z",
    ),
    "ic_launcher_foreground": (
        "Launcher icon, foreground layer: the t-shirt, in StoneGround.",
        FOREGROUND, None,
    ),
    "ic_launcher_monochrome": (
        "Launcher icon, monochrome layer, used by themed icons (Android 13+), which is what the\n"
        "  target Nothing Phone (2a) shows by default. Same outline as the foreground, filled\n"
        "  solid: the system reads only the alpha and tints it to the wallpaper, so the colour\n"
        "  here is arbitrary.",
        "#FF000000", None,
    ),
}

DENSITIES = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}


def write_resources():
    path = path_data()
    radius, _ = furthest_from_centre()

    for name, (what, fill, data) in _LAYERS.items():
        xml = _HEADER.format(what=what, fill=fill, path=data or path, r=radius, safe=SAFE_RADIUS)
        # XML forbids "--" inside a comment, and aapt rejects the file outright rather than
        # warning. Easy to reintroduce while rewording a comment, so check rather than remember.
        comment = xml[xml.index("<!--") + 4:xml.index("-->")]
        if "--" in comment:
            raise SystemExit(f'{name}.xml: "--" is not permitted inside an XML comment')
        (RES / "drawable" / f"{name}.xml").write_text(xml, encoding="utf-8")
        print(f"  drawable/{name}.xml")

    for name in ("ic_launcher.xml", "ic_launcher_round.xml"):
        (RES / "mipmap-anydpi-v26" / name).write_text(_ADAPTIVE, encoding="utf-8")
        print(f"  mipmap-anydpi-v26/{name}")

    # API 24-25 predate adaptive icons and fall back to these.
    for density, px in DENSITIES.items():
        for name, mask in (("ic_launcher", "squircle"), ("ic_launcher_round", "circle")):
            out = RES / f"mipmap-{density}" / f"{name}.webp"
            render(px, BG_RGBA, FG_RGBA, mask).save(out, "WEBP", lossless=True, method=6)
            print(f"  mipmap-{density}/{name}.webp  ({px}x{px}, {out.stat().st_size:,} bytes)")

    print(f"\nfurthest point from centre: {radius:.2f} of {SAFE_RADIUS:.0f} safe radius")


def write_preview(path="icon-preview.png"):
    """A look-at-it sheet: big, then at real launcher sizes on light and dark, plus themed."""
    sheet = Image.new("RGBA", (780, 470), (128, 128, 132, 255))
    big = render(384, BG_RGBA, FG_RGBA, "squircle")
    sheet.paste(big, (24, 43), big)
    for shade, y in ((245, 25), (26, 250)):
        sheet.paste(Image.new("RGBA", (316, 195), (shade, shade, shade, 255)), (440, y))
        x = 452
        for px in (48, 72, 96):
            ic = render(px, BG_RGBA, FG_RGBA, "squircle")
            sheet.paste(ic, (x, y + 18), ic)
            x += px + 12
        tint = (0, 0, 0, 255) if shade > 128 else (255, 255, 255, 255)
        pair = (render(88, BG_RGBA, FG_RGBA, "circle"),
                render(88, (shade, shade, shade, 255), tint, "circle"))
        for i, ic in enumerate(pair):
            sheet.paste(ic, (452 + i * 104, y + 90), ic)
    sheet.convert("RGB").save(path)
    print("wrote", path)


if __name__ == "__main__":
    if "--preview" in sys.argv:
        args = [a for a in sys.argv[1:] if not a.startswith("-")]
        write_preview(*args[:1])
    else:
        write_resources()
