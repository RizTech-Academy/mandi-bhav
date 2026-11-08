"""Draws the commodity pictures in api/v1/images/.

Every picture in the API is made by this script: a heap of produce on a
hessian sack, drawn with Pillow. Nothing is downloaded or copied from
anywhere, so the images carry no third-party licence.

    python3 tools/draw_commodity_images.py            # writes api/v1/images/
    python3 tools/draw_commodity_images.py --camera onion /tmp/onion-4000.jpg

The second form draws one commodity at 4000 x 3000, the size of a
12-megapixel phone photo, for the image-loading experiments in the course.
It is not part of the API. Output is the same on every run (fixed seeds).
"""

import math
import random
import sys
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter

ROOT = Path(__file__).resolve().parent.parent / "api" / "v1" / "images"

# name: (item colour, highlight, shadow, item size as a fraction of width, shape)
COMMODITIES = {
    "onion": ((150, 52, 92), (214, 140, 170), (80, 22, 48), 0.105, "onion"),
    "tomato": ((205, 38, 30), (255, 140, 120), (110, 15, 10), 0.10, "tomato"),
    "potato": ((176, 132, 78), (222, 190, 140), (100, 70, 38), 0.11, "potato"),
    "wheat": ((205, 160, 70), (240, 210, 140), (130, 90, 30), 0.022, "grain"),
    "soybean": ((222, 196, 120), (248, 232, 180), (150, 120, 60), 0.03, "bean"),
    "tur-dal": ((236, 178, 40), (255, 222, 120), (170, 110, 10), 0.026, "dal"),
}


def sack(width, height, rng):
    """A hessian sack: woven brown with a little noise."""
    img = Image.new("RGB", (width, height), (150, 118, 80))
    px = img.load()
    step = max(2, width // 240)
    for y in range(height):
        for x in range(width):
            weave = ((x // step) + (y // step)) % 2
            n = rng.randint(-18, 18)
            base = 138 if weave else 156
            px[x, y] = (base + n, int((base + n) * 0.78), int((base + n) * 0.52))
    return img.filter(ImageFilter.GaussianBlur(radius=max(1, width // 900)))


def shaded_item(draw, cx, cy, rx, ry, colour, light, dark, shape, rng, angle):
    """One onion, tomato, grain or bean, lit from the top left."""
    layers = 7
    for i in range(layers):
        t = i / (layers - 1)
        r = [int(dark[c] + (colour[c] - dark[c]) * min(1, t * 1.6)) for c in range(3)]
        if t > 0.65:
            k = (t - 0.65) / 0.35
            r = [int(r[c] + (light[c] - r[c]) * k * 0.8) for c in range(3)]
        shrink = 1 - t * 0.55
        ox = -rx * 0.25 * t
        oy = -ry * 0.25 * t
        box = [cx + ox - rx * shrink, cy + oy - ry * shrink, cx + ox + rx * shrink, cy + oy + ry * shrink]
        draw.ellipse(box, fill=tuple(r))
    if shape == "onion":
        # The papery tip, and a few dry lines down the bulb.
        tip = [(cx - rx * 0.12, cy - ry * 0.85), (cx + rx * 0.12, cy - ry * 0.85), (cx, cy - ry * 1.25)]
        draw.polygon(tip, fill=(190, 150, 110))
        for k in (-0.4, 0.0, 0.4):
            draw.arc([cx - rx * (0.7 - abs(k) * 0.3) + k * rx, cy - ry * 0.9, cx + rx * (0.7 - abs(k) * 0.3) + k * rx, cy + ry * 0.9],
                     200, 340, fill=dark, width=max(1, int(rx * 0.04)))
    elif shape == "tomato":
        for a in range(5):
            th = angle + a * 2 * math.pi / 5
            draw.line([cx, cy - ry * 0.75, cx + math.cos(th) * rx * 0.35, cy - ry * 0.75 + math.sin(th) * ry * 0.2],
                      fill=(40, 110, 30), width=max(1, int(rx * 0.12)))
    elif shape == "potato":
        for _ in range(4):
            ex = cx + rng.uniform(-0.5, 0.5) * rx
            ey = cy + rng.uniform(-0.5, 0.5) * ry
            e = rx * 0.06
            draw.ellipse([ex - e, ey - e, ex + e, ey + e], fill=dark)
    elif shape == "dal":
        # Split lentils show a flat face with a faint line across it.
        draw.line([cx - rx * 0.6, cy, cx + rx * 0.6, cy], fill=dark, width=max(1, int(rx * 0.1)))


def heap(img, spec, rng):
    width, height = img.size
    colour, light, dark, size, shape = spec
    draw = ImageDraw.Draw(img)
    base_r = width * size
    items = []
    count = min(int(1.1 / size ** 2), 4000)
    for _ in range(count):
        x = rng.uniform(0.05, 0.95) * width
        # A mound: higher in the middle, lower at the sides.
        top = height * (0.30 + 0.55 * ((x / width - 0.5) * 2) ** 2)
        y = rng.uniform(top, height * 1.05)
        items.append((y, x))
    items.sort()
    for y, x in items:
        r = base_r * rng.uniform(0.8, 1.2)
        ry = r * (0.55 if shape in ("grain", "bean", "dal") else 0.9) * rng.uniform(0.9, 1.1)
        if shape == "potato":
            ry = r * rng.uniform(0.65, 0.8)
        jitter = rng.randint(-12, 12)
        c = tuple(max(0, min(255, v + jitter)) for v in colour)
        shaded_item(draw, x, y, r, ry, c, light, dark, shape, rng, rng.uniform(0, math.pi))
    return img


def draw_commodity(name, width, height):
    rng = random.Random(f"{name}-{width}")
    # Draw at twice the size and shrink: smooth edges without anti-aliasing.
    big = sack(width * 2, height * 2, rng) if width <= 1000 else sack(width, height, rng)
    heap(big, COMMODITIES[name], rng)
    if big.size != (width, height):
        big = big.resize((width, height), Image.LANCZOS)
    return big


def main():
    if len(sys.argv) == 4 and sys.argv[1] == "--camera":
        draw_commodity(sys.argv[2], 4000, 3000).save(sys.argv[3], "JPEG", quality=90)
        return
    (ROOT / "thumb").mkdir(parents=True, exist_ok=True)
    (ROOT / "photo").mkdir(parents=True, exist_ok=True)
    for name in COMMODITIES:
        photo = draw_commodity(name, 960, 640)
        photo.save(ROOT / "photo" / f"{name}.jpg", "JPEG", quality=82, optimize=True, progressive=True)
        # The thumbnail is the middle of the photo, square, at 160 px:
        # 56 dp on a 2.625x phone is 147 px, so 160 covers it.
        square = photo.crop((160, 0, 800, 640)).resize((160, 160), Image.LANCZOS)
        square.save(ROOT / "thumb" / f"{name}.jpg", "JPEG", quality=82, optimize=True)
    for f in sorted(ROOT.rglob("*.jpg")):
        print(f"{f.relative_to(ROOT.parent.parent.parent)}  {f.stat().st_size:,} bytes")


if __name__ == "__main__":
    main()
