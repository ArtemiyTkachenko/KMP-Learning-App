"""The Android Engineering Lab application mark, and the geometry every host asset derives from.

The mark is an isometric cube built from three layers: one compact hexagonal silhouette, three
shaded faces, and two thin seams cutting the front faces into stacked layers. It is the subject of
the product drawn as plainly as it can be — the app teaches how an Android application is put
together in layers, and the mark is a thing put together in layers. It carries no text, uses no
platform's product imagery, and is made only of flat polygons on a plate.

Why the layers are seams inside one silhouette rather than three separate slabs: three slabs are
legible on a launcher and turn to mush in a 16px browser tab. Seams degrade gracefully instead. At
launcher size the layering reads; at favicon size the seams fall below a pixel and what is left is
still a clean, recognisable cube.

This file is the canonical source for the artwork: the geometry and the palette are stated once
here, `render_app_icons.py` writes every host asset from them, and `tools/icon/app-icon.svg` is the
master rendering it emits for inspection. Editing the mark means editing the constants below and
re-running the renderer; no host asset is drawn by hand.

Coordinate space
----------------
Everything is defined on the 108x108 grid Android adaptive icons use, so the design is authored
once in the space with the strictest constraint. A launcher may mask anything outside the centred
66x66 region, so the mark is checked against that region's radius of 33 — see `max_radius`. Hosts
that need a pre-masked square — iOS, Desktop, the Web favicon, the legacy Android mipmaps — render
the same design zoomed by 108/72, which is exactly what a launcher shows of an adaptive icon.

No dependencies: the repository has no image libraries, so the renderer is a small analytic
scanline rasteriser and a PNG/ICO writer. Both live in `render_app_icons.py`.
"""

from __future__ import annotations

import math

# The grid the design is authored on, and the region a launcher mask always shows.
GRID = 108.0
ADAPTIVE_VISIBLE = 72.0

#: Zoom applied by hosts that render a pre-masked square, mirroring a launcher mask.
MASKED_ZOOM = GRID / ADAPTIVE_VISIBLE

#: The radius a launcher mask is guaranteed not to cut into.
SAFE_RADIUS = 33.0

# The product palette, taken from AppLightColorScheme rather than invented for the icon.
PLATE = "#3F5BA9"  # primary
FACE_TOP = "#FFFFFF"  # onPrimary
FACE_LEFT = "#DBE1FF"  # primaryContainer
FACE_RIGHT = "#B4C5FF"  # primaryFixedDim

# Cube construction. HALF_WIDTH sets the silhouette's width, RHOMBUS_RISE is the isometric
# 30-degree rise of a face corner, and the body is LAYER_COUNT layers of LAYER_HEIGHT each.
HALF_WIDTH = 24.0
RHOMBUS_RISE = HALF_WIDTH / math.sqrt(3.0)
LAYER_HEIGHT = 11.0
LAYER_COUNT = 3
BODY_HEIGHT = LAYER_HEIGHT * LAYER_COUNT

#: Width of the seams that divide the front faces into layers.
SEAM_WIDTH = 2.2

_CX = GRID / 2.0
_TOP = (GRID - (BODY_HEIGHT + 2 * RHOMBUS_RISE)) / 2.0 + RHOMBUS_RISE
_BOTTOM = _TOP + BODY_HEIGHT

#: Cube vertices: apex, the four side corners, the base, and the centre junction.
APEX = (_CX, _TOP - RHOMBUS_RISE)
RIGHT_UPPER = (_CX + HALF_WIDTH, _TOP)
RIGHT_LOWER = (_CX + HALF_WIDTH, _BOTTOM)
BASE = (_CX, _BOTTOM + RHOMBUS_RISE)
LEFT_LOWER = (_CX - HALF_WIDTH, _BOTTOM)
LEFT_UPPER = (_CX - HALF_WIDTH, _TOP)
JUNCTION = (_CX, _TOP + RHOMBUS_RISE)

#: The outer silhouette, painted first so the seams between faces cannot show as hairlines.
SILHOUETTE = (APEX, RIGHT_UPPER, RIGHT_LOWER, BASE, LEFT_LOWER, LEFT_UPPER)

TOP_FACE = (APEX, RIGHT_UPPER, JUNCTION, LEFT_UPPER)
RIGHT_FACE = (RIGHT_UPPER, RIGHT_LOWER, BASE, JUNCTION)
LEFT_FACE = (LEFT_UPPER, JUNCTION, BASE, LEFT_LOWER)


def _widen(start, end, width):
    """The quadrilateral covering one line segment, used for the layer seams."""
    dx, dy = end[0] - start[0], end[1] - start[1]
    length = math.hypot(dx, dy)
    nx, ny = -dy / length, dx / length
    half = width / 2.0
    return [
        (start[0] + nx * half, start[1] + ny * half),
        (end[0] + nx * half, end[1] + ny * half),
        (end[0] - nx * half, end[1] - ny * half),
        (start[0] - nx * half, start[1] - ny * half),
    ]


def seam_strips():
    """The two V-shaped seams across the front faces, as quadrilaterals.

    Each seam runs from one side edge, down to the vertical centre edge, and back up to the other
    side edge, following the isometric direction of the faces it crosses. The strips are painted in
    the plate colour and deliberately allowed to overhang the silhouette by half their width: the
    overhang lands on the plate, in the plate's own colour, so it cannot be seen.
    """
    strips = []
    for layer in range(1, LAYER_COUNT):
        y = _TOP + layer * LAYER_HEIGHT
        left = (_CX - HALF_WIDTH, y)
        middle = (_CX, y + RHOMBUS_RISE)
        right = (_CX + HALF_WIDTH, y)
        strips.append(_widen(left, middle, SEAM_WIDTH))
        strips.append(_widen(middle, right, SEAM_WIDTH))
    return strips


#: Every polygon to paint, in order: silhouette, the two shaded faces, then the layer seams.
FACES = (
    (SILHOUETTE, FACE_TOP),
    (RIGHT_FACE, FACE_RIGHT),
    (LEFT_FACE, FACE_LEFT),
) + tuple((tuple(strip), PLATE) for strip in seam_strips())


def _signed_area(polygon) -> float:
    total = 0.0
    for index in range(len(polygon)):
        x0, y0 = polygon[index]
        x1, y1 = polygon[(index + 1) % len(polygon)]
        total += x0 * y1 - x1 * y0
    return total / 2.0


def clip_to_convex(subject, clip):
    """Sutherland-Hodgman: the part of `subject` inside the convex polygon `clip`.

    The vector drawables need this. A seam is drawn as a widened line, so it overhangs whatever it
    crosses; in a raster render the overhang lands on the plate in the plate's own colour and is
    invisible, but a vector drawable punches the seams out of each face with the even-odd rule,
    where an overhang would fill instead of clear. Clipping each seam to the shape it cuts is what
    keeps the two renderings identical.
    """
    clip = list(clip) if _signed_area(clip) > 0 else list(reversed(clip))
    output = list(subject)
    for index in range(len(clip)):
        if not output:
            return []
        edge_start = clip[index]
        edge_end = clip[(index + 1) % len(clip)]
        edge = (edge_end[0] - edge_start[0], edge_end[1] - edge_start[1])

        def inside(point):
            return (
                edge[0] * (point[1] - edge_start[1]) - edge[1] * (point[0] - edge_start[0])
            ) >= -1e-9

        clipped = []
        for position in range(len(output)):
            current = output[position]
            previous = output[position - 1]
            if inside(current):
                if not inside(previous):
                    clipped.append(_intersect(previous, current, edge_start, edge_end))
                clipped.append(current)
            elif inside(previous):
                clipped.append(_intersect(previous, current, edge_start, edge_end))
        output = clipped
    return output


def _intersect(start, end, edge_start, edge_end):
    dx1, dy1 = end[0] - start[0], end[1] - start[1]
    dx2, dy2 = edge_end[0] - edge_start[0], edge_end[1] - edge_start[1]
    denominator = dx1 * dy2 - dy1 * dx2
    t = (
        (edge_start[0] - start[0]) * dy2 - (edge_start[1] - start[1]) * dx2
    ) / denominator
    return (start[0] + t * dx1, start[1] + t * dy1)


def vector_layers():
    """The mark as (outline, colour, cut-outs) layers, for the Android vector drawables.

    The silhouette comes first for the same reason it does in the raster renderer: adjacent faces
    that merely abut can show a hairline of whatever is behind them.
    """
    seams = seam_strips()
    layers = []
    for outline, color in (
        (SILHOUETTE, FACE_TOP),
        (RIGHT_FACE, FACE_RIGHT),
        (LEFT_FACE, FACE_LEFT),
    ):
        cutouts = [
            clipped
            for clipped in (clip_to_convex(seam, outline) for seam in seams)
            # Clipping a seam against a face it only touches leaves a zero-area sliver along the
            # shared edge. Dropping those keeps each face's cut-outs to the seams that cross it.
            if len(clipped) >= 3 and abs(_signed_area(clipped)) > 0.01
        ]
        layers.append((list(outline), color, cutouts))
    return layers


def monochrome_layer():
    """The silhouette with the layer seams cleared, for Android's themed icons.

    A themed icon is tinted a single colour, which would fuse the three layers into one blob, so
    the seams have to be holes rather than a lighter shade.
    """
    seams = [clip_to_convex(seam, SILHOUETTE) for seam in seam_strips()]
    return list(SILHOUETTE), [
        seam for seam in seams if len(seam) >= 3 and abs(_signed_area(seam)) > 0.01
    ]


def max_radius() -> float:
    """How far the mark reaches from the centre, for checking it against a launcher mask."""
    return max(math.hypot(x - _CX, y - GRID / 2.0) for x, y in SILHOUETTE)
