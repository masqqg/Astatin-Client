"""Astatin brand-mark generator.

The supplied reference (white "Ast" wordmark inside a tilted orbit ring with a
four-point star on a black backdrop) is recreated here as clean vector-style
art with a real alpha channel. 1024px master + 256px mod icon.

Run:  python3 tools/make_logo.py
"""
from PIL import Image, ImageDraw, ImageFont, ImageFilter, ImageOps
import os

W = 1024
HERE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
FONT = os.path.join(HERE, "src/main/resources/assets/astatin/fonts/Inter-Italic.ttf")
OUT_LOGO = os.path.join(HERE, "assets-src/logo.png")
OUT_ICON = os.path.join(HERE, "src/main/resources/assets/astatin/icon.png")

# ---------- ring ----------
ring_layer = Image.new("RGBA", (W, W), (0, 0, 0, 0))
rd = ImageDraw.Draw(ring_layer)
cx, cy = 512, 512
rw, rh = 392, 156
rd.ellipse([cx - rw, cy - rh, cx + rw, cy + rh], outline=(255, 255, 255, 255), width=38)
# subtle cool shading on the lower half, like the reference render
shade = Image.new("RGBA", (W, W), (0, 0, 0, 0))
sd = ImageDraw.Draw(shade)
sd.ellipse([cx - rw, cy - rh, cx + rw, cy + rh], outline=(185, 188, 196, 90), width=38)
shade = shade.filter(ImageFilter.GaussianBlur(1.2))
ring_layer = Image.alpha_composite(ring_layer, shade)
ring_layer = ring_layer.rotate(-18, resample=Image.BICUBIC, center=(cx, cy))

# ---------- wordmark ----------
font = ImageFont.truetype(FONT, 330)
try:
    font.set_variation_by_axes([32, 800])       # opsz, wght
except Exception:
    pass

def wordmark_mask(pad=0):
    layer = Image.new("L", (W, W), 0)
    d = ImageDraw.Draw(layer)
    d.text((492, 545), "Ast", font=font, fill=255, anchor="mm", stroke_width=pad)
    return layer.rotate(-6, resample=Image.BICUBIC, center=(cx, cy))

text_mask = wordmark_mask()
# carve a soft gap out of the ring behind the letters
gap = text_mask.filter(ImageFilter.MaxFilter(19))
ring_rgba = ring_layer.split()[3]
ring_rgba = Image.composite(Image.new("L", (W, W), 0), ring_rgba, gap)
ring_layer.putalpha(ring_rgba)

text_layer = Image.new("RGBA", (W, W), (0, 0, 0, 0))
text_layer.paste((250, 250, 252, 255), (0, 0), text_mask)
# faint inner shading at the bottom of the letters
grad = Image.new("L", (W, W), 0)
gd = ImageDraw.Draw(grad)
for y in range(430, 760):
    a = int(max(0.0, min(1.0, (y - 520) / 220.0)) * 52)
    gd.line([(0, y), (W, y)], fill=a)
text_shade = Image.new("RGBA", (W, W), (176, 180, 192, 255))
text_shade.putalpha(Image.composite(grad, Image.new("L", (W, W), 0), text_mask))
text_layer = Image.alpha_composite(text_layer, text_shade)

# ---------- star ----------
star = Image.new("RGBA", (W, W), (0, 0, 0, 0))
sd = ImageDraw.Draw(star)
sx, sy, s = 700, 366, 54
sd.polygon([(sx, sy - s), (sx + s * 0.20, sy - s * 0.20), (sx + s, sy),
              (sx + s * 0.20, sy + s * 0.20), (sx, sy + s),
              (sx - s * 0.20, sy + s * 0.20), (sx - s, sy),
              (sx - s * 0.20, sy - s * 0.20)], fill=(255, 255, 255, 255))
sd.ellipse([sx - 7, sy - 7, sx + 7, sy + 7], fill=(255, 255, 255, 255))
star = star.rotate(-18, resample=Image.BICUBIC, center=(sx, sy))

mark = Image.alpha_composite(Image.alpha_composite(ring_layer, text_layer), star)

# ---------- glow ----------
glow = mark.filter(ImageFilter.GaussianBlur(26))
glow = ImageOps.colorize(glow.convert("L"), (0, 0, 0), (255, 150, 60)).convert("RGBA")
glow.putalpha(glow.split()[3].point(lambda a: int(a * 0.30)))
narrow = mark.filter(ImageFilter.GaussianBlur(9))
narrow.putalpha(narrow.split()[3].point(lambda a: int(a * 0.35)))

out = Image.new("RGBA", (W, W), (0, 0, 0, 0))
out = Image.alpha_composite(out, glow)
out = Image.alpha_composite(out, narrow)
out = Image.alpha_composite(out, mark)
out.save(OUT_LOGO)

icon = out.resize((256, 256), Image.LANCZOS)
icon.save(OUT_ICON)
print("logo:", OUT_LOGO, out.size)
print("icon:", OUT_ICON, icon.size)

# ---------- background ----------
bg = Image.open(os.path.join(HERE, "assets-src/background_raw.jpg")).convert("RGB")
bg = ImageOps.fit(bg, (1920, 1080), Image.LANCZOS)
bg.save(os.path.join(HERE, "src/main/resources/assets/astatin/textures/gui/background.jpg"), quality=91, optimize=True)
print("background saved")
