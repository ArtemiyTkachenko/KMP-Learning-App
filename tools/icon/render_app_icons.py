"""Writes every host icon asset from the single mark defined in `app_icon.py`.

Run from the repository root:

    python3 tools/icon/render_app_icons.py

The repository has no image dependencies and this is authoring tooling rather than part of the
application build, so the rasteriser is here: an analytic scanline fill with exact horizontal
coverage and eight vertical samples per row, which is ample for flat polygons and a rounded plate.
PNG, ICO and SVG are written directly; the macOS `.icns` is assembled with `iconutil`, which ships
with macOS.

Every asset is a projection of the same design, so none of them can drift into a different mark.
"""

from __future__ import annotations

import math
import os
import shutil
import struct
import subprocess
import sys
import tempfile
import zlib

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import app_icon as mark

SUBSAMPLES = 8


# --------------------------------------------------------------------------------------- shapes


def _rounded_rect_spans(y: float, x0: float, y0: float, x1: float, y1: float, radius: float):
    if y < y0 or y > y1:
        return []
    inset = 0.0
    if y < y0 + radius:
        dy = (y0 + radius) - y
        inset = radius - math.sqrt(max(radius * radius - dy * dy, 0.0))
    elif y > y1 - radius:
        dy = y - (y1 - radius)
        inset = radius - math.sqrt(max(radius * radius - dy * dy, 0.0))
    return [(x0 + inset, x1 - inset)]


def _circle_spans(y: float, cx: float, cy: float, radius: float):
    dy = y - cy
    if abs(dy) >= radius:
        return []
    dx = math.sqrt(radius * radius - dy * dy)
    return [(cx - dx, cx + dx)]


def _polygon_spans(y: float, points):
    crossings = []
    count = len(points)
    for index in range(count):
        x_start, y_start = points[index]
        x_end, y_end = points[(index + 1) % count]
        if y_start == y_end:
            continue
        low, high = min(y_start, y_end), max(y_start, y_end)
        if not (low <= y < high):
            continue
        t = (y - y_start) / (y_end - y_start)
        crossings.append(x_start + t * (x_end - x_start))
    crossings.sort()
    return list(zip(crossings[0::2], crossings[1::2]))


class Shape:
    """A filled region, able to report the x intervals it covers on a horizontal line."""

    def __init__(self, kind, color, **geometry):
        self.kind = kind
        self.color = color
        self.geometry = geometry

    def spans(self, y: float):
        if self.kind == "rounded_rect":
            return _rounded_rect_spans(y, **self.geometry)
        if self.kind == "circle":
            return _circle_spans(y, **self.geometry)
        return _polygon_spans(y, **self.geometry)


# ----------------------------------------------------------------------------------- rendering


def _parse_color(value: str):
    value = value.lstrip("#")
    return tuple(int(value[index:index + 2], 16) for index in (0, 2, 4))


def _coverage_row(shape, size: int, row: int):
    """Per-pixel coverage of `shape` across one pixel row, in 0.0..1.0."""
    coverage = [0.0] * size
    for sample in range(SUBSAMPLES):
        y = row + (sample + 0.5) / SUBSAMPLES
        for span_start, span_end in shape.spans(y):
            if span_end <= span_start:
                continue
            first = max(int(math.floor(span_start)), 0)
            last = min(int(math.ceil(span_end)), size)
            for column in range(first, last):
                overlap = min(span_end, column + 1.0) - max(span_start, float(column))
                if overlap > 0.0:
                    coverage[column] += overlap
    return [value / SUBSAMPLES for value in coverage]


def render(size: int, shapes) -> bytes:
    """Composites `shapes` in order onto transparency and returns RGBA bytes."""
    pixels = bytearray(size * size * 4)
    for shape in shapes:
        red, green, blue = _parse_color(shape.color)
        for row in range(size):
            coverage = _coverage_row(shape, size, row)
            base = row * size * 4
            for column in range(size):
                alpha = coverage[column]
                if alpha <= 0.0:
                    continue
                offset = base + column * 4
                existing_alpha = pixels[offset + 3] / 255.0
                out_alpha = alpha + existing_alpha * (1.0 - alpha)
                for channel, value in enumerate((red, green, blue)):
                    existing = pixels[offset + channel] / 255.0
                    blended = (
                        value / 255.0 * alpha + existing * existing_alpha * (1.0 - alpha)
                    ) / out_alpha
                    pixels[offset + channel] = int(round(blended * 255.0))
                pixels[offset + 3] = int(round(out_alpha * 255.0))
    return bytes(pixels)


def icon_shapes(size: int, plate: str, radius_fraction: float, inset_fraction: float = 0.0):
    """The mark at `size` pixels: plate, then the cube scaled as a launcher mask would show it."""
    inset = size * inset_fraction
    extent = size - 2 * inset
    if plate == "circle":
        plate_shape = Shape(
            "circle", mark.PLATE, cx=size / 2.0, cy=size / 2.0, radius=extent / 2.0
        )
    else:
        plate_shape = Shape(
            "rounded_rect",
            mark.PLATE,
            x0=inset,
            y0=inset,
            x1=size - inset,
            y1=size - inset,
            radius=extent * radius_fraction,
        )

    scale = extent / mark.GRID * mark.MASKED_ZOOM
    center = size / 2.0

    def place(points):
        return [
            (
                center + (x - mark.GRID / 2.0) * scale,
                center + (y - mark.GRID / 2.0) * scale,
            )
            for x, y in points
        ]

    shapes = [plate_shape]
    for points, color in mark.FACES:
        shapes.append(Shape("polygon", color, points=place(points)))
    return shapes


# ------------------------------------------------------------------------------------- writing


def write_png(path: str, size: int, rgba: bytes) -> None:
    raw = bytearray()
    stride = size * 4
    for row in range(size):
        raw.append(0)
        raw.extend(rgba[row * stride:(row + 1) * stride])

    def chunk(tag: bytes, data: bytes) -> bytes:
        return (
            struct.pack(">I", len(data))
            + tag
            + data
            + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)
        )

    png = b"\x89PNG\r\n\x1a\n"
    png += chunk(b"IHDR", struct.pack(">IIBBBBB", size, size, 8, 6, 0, 0, 0))
    png += chunk(b"IDAT", zlib.compress(bytes(raw), 9))
    png += chunk(b"IEND", b"")
    with open(path, "wb") as handle:
        handle.write(png)
    print("wrote", os.path.relpath(path), f"({size}x{size})")


def _ico_bmp_entry(size: int, rgba: bytes) -> bytes:
    """A 32-bit BMP icon image, which every Windows version reads."""
    header = struct.pack(
        "<IiiHHIIiiII", 40, size, size * 2, 1, 32, 0, size * size * 4, 0, 0, 0, 0
    )
    body = bytearray()
    for row in range(size - 1, -1, -1):
        for column in range(size):
            offset = (row * size + column) * 4
            red, green, blue, alpha = rgba[offset:offset + 4]
            body += bytes((blue, green, red, alpha))
    mask_stride = ((size + 31) // 32) * 4
    body += bytes(mask_stride * size)
    return header + bytes(body)


def write_ico(path: str, images) -> None:
    """`images` is a list of (size, png_bytes_or_None, rgba). PNG payloads cover the large sizes."""
    entries = []
    for size, png, rgba in images:
        payload = png if png is not None else _ico_bmp_entry(size, rgba)
        entries.append((size, payload))

    offset = 6 + 16 * len(entries)
    directory = struct.pack("<HHH", 0, 1, len(entries))
    payloads = b""
    for size, payload in entries:
        dimension = 0 if size >= 256 else size
        directory += struct.pack(
            "<BBBBHHII", dimension, dimension, 0, 0, 1, 32, len(payload), offset
        )
        offset += len(payload)
        payloads += payload
    with open(path, "wb") as handle:
        handle.write(directory + payloads)
    print("wrote", os.path.relpath(path), f"({len(entries)} sizes)")


def _svg_polygon(points, color, scale, translate):
    coordinates = " ".join(
        f"{translate[0] + x * scale:.4f},{translate[1] + y * scale:.4f}" for x, y in points
    )
    return f'  <polygon points="{coordinates}" fill="{color}"/>'


def write_svg(path: str, size: float, radius: float, zoomed: bool) -> None:
    scale = (size / mark.GRID) * (mark.MASKED_ZOOM if zoomed else 1.0)
    offset = (size - mark.GRID * scale) / 2.0
    lines = [
        f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {size:g} {size:g}" '
        f'width="{size:g}" height="{size:g}" role="img" '
        'aria-label="Android Engineering Lab">',
        f'  <rect width="{size:g}" height="{size:g}" rx="{radius:g}" ry="{radius:g}" '
        f'fill="{mark.PLATE}"/>',
    ]
    for points, color in mark.FACES:
        lines.append(_svg_polygon(points, color, scale, (offset, offset)))
    lines.append("</svg>")
    with open(path, "w") as handle:
        handle.write("\n".join(lines) + "\n")
    print("wrote", os.path.relpath(path))


def _path_data(*polygons) -> str:
    """Vector-drawable path data: one closed subpath per polygon."""
    parts = []
    for polygon in polygons:
        points = " ".join(f"L{x:.4f},{y:.4f}" for x, y in polygon[1:])
        parts.append(f"M{polygon[0][0]:.4f},{polygon[0][1]:.4f} {points} Z")
    return " ".join(parts)


def _vector_drawable(paths) -> str:
    """A 108dp vector drawable, the size an adaptive icon layer is authored at."""
    lines = [
        '<?xml version="1.0" encoding="utf-8"?>',
        "<!-- Generated by tools/icon/render_app_icons.py from tools/icon/app_icon.py.",
        "     Do not edit: change the mark's geometry there and re-run the renderer. -->",
        '<vector xmlns:android="http://schemas.android.com/apk/res/android"',
        '    android:width="108dp"',
        '    android:height="108dp"',
        '    android:viewportWidth="108"',
        '    android:viewportHeight="108">',
    ]
    for color, data in paths:
        lines.append("    <path")
        lines.append(f'        android:fillColor="{color}"')
        lines.append('        android:fillType="evenOdd"')
        lines.append(f'        android:pathData="{data}" />')
    lines.append("</vector>")
    return "\n".join(lines) + "\n"


def write_android_vectors(android_res: str) -> None:
    """The adaptive icon's three layers, generated from the same geometry the rasters use."""
    drawable = os.path.join(android_res, "drawable")
    os.makedirs(drawable, exist_ok=True)

    plate = [(mark.PLATE, _path_data([(0, 0), (108, 0), (108, 108), (0, 108)]))]
    with open(os.path.join(drawable, "ic_launcher_background.xml"), "w") as handle:
        handle.write(_vector_drawable(plate))
    print("wrote", os.path.relpath(os.path.join(drawable, "ic_launcher_background.xml")))

    foreground = [
        (color, _path_data(outline, *cutouts))
        for outline, color, cutouts in mark.vector_layers()
    ]
    with open(os.path.join(drawable, "ic_launcher_foreground.xml"), "w") as handle:
        handle.write(_vector_drawable(foreground))
    print("wrote", os.path.relpath(os.path.join(drawable, "ic_launcher_foreground.xml")))

    outline, seams = mark.monochrome_layer()
    monochrome = [("#FFFFFF", _path_data(outline, *seams))]
    with open(os.path.join(drawable, "ic_launcher_monochrome.xml"), "w") as handle:
        handle.write(_vector_drawable(monochrome))
    print("wrote", os.path.relpath(os.path.join(drawable, "ic_launcher_monochrome.xml")))


# ---------------------------------------------------------------------------------------- main


def main() -> int:
    root = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
    icon_dir = os.path.join(root, "tools", "icon")

    def rgba(size, plate="rounded", radius_fraction=0.22, inset_fraction=0.0):
        return render(size, icon_shapes(size, plate, radius_fraction, inset_fraction))

    def png_bytes(size, **kwargs):
        path = os.path.join(tempfile.mkdtemp(), "icon.png")
        write_png(path, size, rgba(size, **kwargs))
        with open(path, "rb") as handle:
            return handle.read()

    # The master rendering, for inspection alongside the geometry that produced it.
    write_svg(os.path.join(icon_dir, "app-icon.svg"), 108, 0, zoomed=False)

    # Android: the adaptive icon's vector layers, then the legacy mipmaps that cover launchers
    # below API 26 and the round variant.
    android_res = os.path.join(root, "androidApp", "src", "main", "res")
    write_android_vectors(android_res)
    for density, size in (
        ("mdpi", 48),
        ("hdpi", 72),
        ("xhdpi", 96),
        ("xxhdpi", 144),
        ("xxxhdpi", 192),
    ):
        directory = os.path.join(android_res, f"mipmap-{density}")
        os.makedirs(directory, exist_ok=True)
        write_png(os.path.join(directory, "ic_launcher.png"), size, rgba(size))
        write_png(
            os.path.join(directory, "ic_launcher_round.png"),
            size,
            rgba(size, plate="circle"),
        )

    # iOS: one 1024 master in the existing AppIcon set. Square and fully opaque, because iOS
    # applies its own mask and rejects transparency in an app icon.
    write_png(
        os.path.join(
            root, "iosApp", "iosApp", "Assets.xcassets", "AppIcon.appiconset", "app-icon-1024.png"
        ),
        1024,
        rgba(1024, radius_fraction=0.0),
    )

    # Desktop: one PNG on the classpath, which the window loads and the Linux package also uses,
    # plus the two host-specific containers macOS and Windows packaging require.
    desktop_resources = os.path.join(root, "desktopApp", "src", "main", "resources")
    os.makedirs(desktop_resources, exist_ok=True)
    write_png(os.path.join(desktop_resources, "app-icon.png"), 512, rgba(512, radius_fraction=0.2))

    desktop_icons = os.path.join(root, "desktopApp", "icons")
    os.makedirs(desktop_icons, exist_ok=True)
    write_ico(
        os.path.join(desktop_icons, "app-icon.ico"),
        [
            (16, None, rgba(16, radius_fraction=0.2)),
            (32, None, rgba(32, radius_fraction=0.2)),
            (48, None, rgba(48, radius_fraction=0.2)),
            (256, png_bytes(256, radius_fraction=0.2), None),
        ],
    )

    # `.icns` is assembled by iconutil from an iconset. macOS icons sit inside their canvas
    # rather than filling it, so this one keeps a margin the other hosts do not need.
    iconset = os.path.join(tempfile.mkdtemp(), "AppIcon.iconset")
    os.makedirs(iconset)
    for base in (16, 32, 128, 256, 512):
        for suffix, factor in (("", 1), ("@2x", 2)):
            size = base * factor
            write_png(
                os.path.join(iconset, f"icon_{base}x{base}{suffix}.png"),
                size,
                rgba(size, radius_fraction=0.22, inset_fraction=0.08),
            )
    icns = os.path.join(desktop_icons, "app-icon.icns")
    subprocess.run(["iconutil", "-c", "icns", iconset, "-o", icns], check=True)
    print("wrote", os.path.relpath(icns, root))
    shutil.rmtree(os.path.dirname(iconset))

    # Web: a vector favicon for current browsers and a 32px raster fallback.
    web_resources = os.path.join(root, "webApp", "src", "webMain", "resources")
    write_svg(os.path.join(web_resources, "favicon.svg"), 64, 13, zoomed=True)
    write_png(os.path.join(web_resources, "favicon-32.png"), 32, rgba(32, radius_fraction=0.2))

    print(f"\nmark reaches {mark.max_radius():.2f} of the 33.0 adaptive-mask safe radius")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
