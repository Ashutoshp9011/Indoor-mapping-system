from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter, ImageFont


ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / "docs" / "stop-node-ui-reference.png"


def font(size: int, bold: bool = False) -> ImageFont.ImageFont:
    candidates = []
    if bold:
        candidates.extend(
            [
                "arialbd.ttf",
                "Arial Bold.ttf",
                "DejaVuSans-Bold.ttf",
            ]
        )
    candidates.extend(
        [
            "arial.ttf",
            "Arial.ttf",
            "DejaVuSans.ttf",
        ]
    )

    for candidate in candidates:
        try:
            return ImageFont.truetype(candidate, size=size)
        except OSError:
            continue
    return ImageFont.load_default()


def rounded(draw: ImageDraw.ImageDraw, box, radius, fill, outline=None, width=1):
    draw.rounded_rectangle(box, radius=radius, fill=fill, outline=outline, width=width)


def centered_text(draw: ImageDraw.ImageDraw, box, text, fill, typeface):
    left, top, right, bottom = box
    bbox = draw.textbbox((0, 0), text, font=typeface)
    tw = bbox[2] - bbox[0]
    th = bbox[3] - bbox[1]
    x = left + (right - left - tw) / 2
    y = top + (bottom - top - th) / 2 - 1
    draw.text((x, y), text, font=typeface, fill=fill)


def chip(draw, box, title, value, accent):
    rounded(draw, box, 26, fill=(25, 30, 42), outline=(36, 45, 62), width=2)
    left, top, right, _ = box
    draw.text((left + 22, top + 18), title, font=font(18, bold=True), fill=(140, 152, 178))
    draw.text((left + 22, top + 46), value, font=font(26, bold=True), fill=accent)


def pill_button(draw, box, label, fill, text_fill=(255, 255, 255), outline=None):
    rounded(draw, box, 18, fill=fill, outline=outline, width=2 if outline else 1)
    centered_text(draw, box, label, text_fill, font(22, bold=True))


def action_button(draw, box, label):
    rounded(draw, box, 16, fill=(24, 28, 39), outline=(49, 59, 78), width=2)
    centered_text(draw, box, label, (236, 241, 248), font(18, bold=True))


def filter_chip(draw, box, label):
    rounded(draw, box, 16, fill=(28, 34, 53), outline=(56, 68, 92), width=2)
    centered_text(draw, box, label, (233, 238, 246), font(18, bold=True))


def main():
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)

    canvas = Image.new("RGB", (1080, 1920), (10, 14, 22))
    draw = ImageDraw.Draw(canvas)

    draw.rectangle((0, 0, 1080, 1920), fill=(9, 14, 25))

    # Phone shell
    rounded(draw, (120, 40, 960, 1880), 56, fill=(20, 25, 36), outline=(40, 49, 68), width=4)
    rounded(draw, (155, 90, 925, 1835), 40, fill=(13, 17, 26))
    rounded(draw, (400, 66, 680, 92), 13, fill=(35, 41, 56))

    # Header chips
    chip(draw, (185, 130, 325, 235), "STEPS", "248", (112, 211, 255))
    chip(draw, (345, 130, 515, 235), "DIST", "187 m", (112, 211, 255))
    chip(draw, (535, 130, 675, 235), "NODES", "14", (220, 125, 255))
    chip(draw, (695, 130, 805, 235), "FLOOR", "G", (255, 191, 61))
    chip(draw, (825, 130, 895, 235), "STATUS", "Walk", (53, 236, 131))

    # Search
    rounded(draw, (185, 270, 895, 355), 28, fill=(23, 28, 40), outline=(42, 52, 74), width=2)
    draw.text((220, 300), "Search rooms, halls, toilets...", font=font(22), fill=(122, 135, 160))

    # Map card
    rounded(draw, (185, 390, 895, 1360), 32, fill=(16, 23, 35), outline=(35, 47, 68), width=2)

    # Grid
    for x in range(210, 885, 55):
        draw.line((x, 415, x, 1335), fill=(27, 35, 52), width=1)
    for y in range(415, 1335, 55):
        draw.line((210, y, 870, y), fill=(27, 35, 52), width=1)

    # Path
    path = [(280, 1180), (280, 980), (475, 980), (475, 780), (670, 780), (670, 620)]
    draw.line(path, fill=(60, 142, 255), width=14)
    draw.ellipse((252, 1152, 308, 1208), fill=(30, 232, 132))
    draw.ellipse((642, 592, 698, 648), fill=(255, 84, 84))
    draw.ellipse((646, 756, 694, 804), fill=(255, 255, 255))
    draw.line((670, 780, 730, 735), fill=(131, 243, 255), width=8)
    draw.polygon([(730, 735), (706, 736), (720, 758)], fill=(131, 243, 255))

    # Compass and side controls
    rounded(draw, (770, 430, 860, 520), 44, fill=(22, 30, 48))
    draw.ellipse((790, 450, 840, 500), outline=(92, 114, 152), width=3)
    draw.line((815, 475, 815, 445), fill=(118, 214, 255), width=5)
    centered_text(draw, (792, 482, 840, 515), "N", (255, 255, 255), font(16, bold=True))

    y = 665
    for label in ["G", "1", "2", "3"]:
        rounded(
            draw,
            (795, y, 855, y + 48),
            14,
            fill=(14, 106, 255) if label == "G" else (27, 34, 51),
        )
        centered_text(draw, (795, y, 855, y + 48), label, (255, 255, 255), font(18, bold=True))
        y += 58

    for label, top in [("+", 948), ("-", 1018)]:
        rounded(draw, (792, top, 858, top + 56), 18, fill=(24, 33, 52))
        centered_text(draw, (792, top, 858, top + 56), label, (102, 200, 255), font(30, bold=True))

    # Bottom bar
    pill_button(draw, (185, 1400, 340, 1490), "Stop", (245, 158, 11), text_fill=(10, 10, 10))
    action_button(draw, (360, 1400, 470, 1490), "Clear")
    action_button(draw, (490, 1400, 600, 1490), "Node")
    action_button(draw, (620, 1400, 730, 1490), "Entry")
    action_button(draw, (750, 1400, 860, 1490), "Exit")

    # Stop dialog
    shadow = Image.new("RGBA", canvas.size, (0, 0, 0, 0))
    shadow_draw = ImageDraw.Draw(shadow)
    shadow_draw.rounded_rectangle((175, 1510, 905, 1810), radius=28, fill=(0, 0, 0, 150))
    shadow = shadow.filter(ImageFilter.GaussianBlur(16))
    canvas = Image.alpha_composite(canvas.convert("RGBA"), shadow)
    draw = ImageDraw.Draw(canvas)

    rounded(draw, (180, 1515, 900, 1805), 28, fill=(22, 28, 40), outline=(42, 52, 74), width=2)
    draw.text((215, 1555), "You stopped walking", font=font(28, bold=True), fill=(236, 242, 248))
    draw.text(
        (215, 1600),
        "Add a quick left/right node, or open more place options.",
        font=font(18),
        fill=(142, 154, 176),
    )

    pill_button(draw, (215, 1650, 415, 1722), "Left", (25, 111, 255))
    rounded(draw, (440, 1650, 640, 1722), 18, fill=(33, 39, 54), outline=(54, 65, 88), width=2)
    centered_text(draw, (440, 1650, 640, 1722), "More", (240, 244, 248), font(22, bold=True))
    pill_button(draw, (665, 1650, 865, 1722), "Right", (139, 92, 246))

    filter_chip(draw, (215, 1745, 365, 1795), "Room")
    filter_chip(draw, (385, 1745, 565, 1795), "Toilet")
    filter_chip(draw, (585, 1745, 735, 1795), "Hall")
    filter_chip(draw, (215, 1810, 345, 1860), "Lab")
    filter_chip(draw, (365, 1810, 545, 1860), "Modify")

    canvas.convert("RGB").save(OUTPUT, quality=95)
    print(OUTPUT)


if __name__ == "__main__":
    main()
