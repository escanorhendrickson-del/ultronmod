#!/usr/bin/env python3
"""Generates Ultron's custom model (UltronModel.java) + skin + glow texture.
Run from anywhere:  python3 tools/generate_assets.py   (needs Pillow)
Model is built at 2x detail and scaled 0.5 in the renderer (2 texels per pixel)."""
import random, os
from PIL import Image

S = 2
rng = random.Random(20260)
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TEX_DIR = ROOT + "/src/main/resources/assets/ultronmod/textures/entity/"
JAVA_OUT = ROOT + "/src/main/java/com/example/ultronmod/client/UltronModel.java"

PIVOTS = {"head": (0, -8, 0), "body": (0, 0, 0),
          "left_arm": (7, -5, 0), "right_arm": (-7, -5, 0),
          "left_leg": (2.5, 10, 0), "right_leg": (-2.5, 10, 0)}

cubes = []
def add(name, part, mat, pos, size):
    cubes.append(dict(name=name, part=part, mat=mat, pos=pos, size=size))
def pair(name, part_p, part_n, mat, pos, size):
    add(name + "_p", part_p, mat, pos, size)
    add(name + "_n", part_n, mat, (-pos[0] - size[0], pos[1], pos[2]), size)

# ---- head ----
add("head_main", "head", "light", (-3, -16, -3), (6, 8, 6))
add("face_plate", "head", "steel", (-2.5, -14.5, -4), (5, 5, 1))
add("jaw_grill", "head", "dark", (-2, -9.5, -4), (4, 2, 1))
add("brow", "head", "dark", (-3.5, -15.5, -4.5), (7, 1, 2))
pair("ear", "head", "head", "dark", (3, -15, -1.5), (1, 4, 3))
# ---- body ----
add("neck", "body", "core", (-1.5, -8, -1.5), (3, 2, 3))
add("chest_main", "body", "steel", (-5, -7, -3), (10, 7, 6))
pair("pec", "body", "body", "light", (0.5, -6.5, -5), (4, 4, 2))
add("abdomen", "body", "core", (-3, 0, -2), (6, 6, 4))
add("spine", "body", "dark", (-1, -6, 3), (2, 10, 1))
add("pelvis", "body", "steel", (-4, 6, -2.5), (8, 4, 5))
add("fauld", "body", "light", (-3, 7, -3.5), (6, 3, 1))
# ---- arms ----
pair("shoulder", "left_arm", "right_arm", "light", (5, -7.5, -2.5), (4, 3, 5))
pair("upper_arm", "left_arm", "right_arm", "dark", (5.5, -4.5, -1.5), (3, 6, 3))
pair("elbow", "left_arm", "right_arm", "core", (5, 1.5, -2), (4, 2, 4))
pair("forearm", "left_arm", "right_arm", "steel", (5, 3.5, -2), (4, 7, 4))
pair("hand", "left_arm", "right_arm", "dark", (5.5, 10.5, -1.5), (3, 3, 3))
pair("finger0", "left_arm", "right_arm", "steel", (5.5, 13.5, -1.5), (1, 3, 1))
pair("finger1", "left_arm", "right_arm", "steel", (6.5, 13.5, -1.5), (1, 3, 1))
pair("finger2", "left_arm", "right_arm", "steel", (7.5, 13.5, -1.5), (1, 3, 1))
# ---- legs ----
pair("thigh", "left_leg", "right_leg", "steel", (0.5, 10, -2), (4, 7, 4))
pair("knee", "left_leg", "right_leg", "dark", (0.5, 15.5, -3), (4, 3, 2))
pair("shin", "left_leg", "right_leg", "steel", (0.5, 17, -2), (4, 5, 4))
pair("calf", "left_leg", "right_leg", "dark", (1, 17.5, 2), (3, 4, 2))
pair("foot", "left_leg", "right_leg", "dark", (0, 22, -4.5), (5, 2, 7))

for c in cubes:
    c["S"] = tuple(int(round(v * S)) for v in c["size"])
    c["P"] = tuple(v * S for v in c["pos"])

# ---- pack UVs (box-UV layout) ----
TEXW = 256
x = y = rowh = 0
for c in sorted(cubes, key=lambda c: -(c["S"][2] + c["S"][1])):
    w, h, d = c["S"]
    rw, rh = 2 * d + 2 * w, d + h
    if x + rw > TEXW:
        x, y, rowh = 0, y + rowh, 0
    c["uv"] = (x, y)
    x += rw
    rowh = max(rowh, rh)
TEXH = ((y + rowh) + 15) // 16 * 16
print("texture size", TEXW, TEXH)

tex = Image.new("RGBA", (TEXW, TEXH), (0, 0, 0, 0))
glow = Image.new("RGBA", (TEXW, TEXH), (0, 0, 0, 0))
tp, gp = tex.load(), glow.load()

def cl(c): return tuple(max(0, min(255, int(v))) for v in c)
def put(x, y, c): tp[x, y] = cl(c) + (255,)
def glowpix(x, y, col, dim=0.4):
    tp[x, y] = cl([v * dim for v in col]) + (255,)
    gp[x, y] = cl(col) + (255,)
def shade_px(x, y, f):
    r, g, b, a = tp[x, y]
    tp[x, y] = cl((r * f, g * f, b * f)) + (255,)

def faces(c):
    u, v = c["uv"]; w, h, d = c["S"]
    return {"top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d),
            "right": (u, v + d, d, h), "front": (u + d, v + d, w, h),
            "left": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h)}

MAT = {"steel": (150, 156, 168), "light": (176, 182, 194),
       "dark": (74, 78, 88), "core": (40, 32, 34)}
RED = (255, 70, 40); ORANGE = (255, 120, 40); HOT = (255, 215, 150)

def paint_face(face, rect, mat):
    x0, y0, fw, fh = rect
    base = MAT[mat]
    rows = [rng.randint(-4, 4) for _ in range(fh)]
    for yy in range(fh):
        for xx in range(fw):
            shade = 1.0 - 0.20 * (yy / max(1, fh - 1))
            if face == "top": shade += 0.12
            if face == "bottom": shade -= 0.25
            if face in ("left", "right"): shade -= 0.06
            col = [b * shade + rng.randint(-5, 5) + rows[yy] for b in base]
            if fw >= 4 and fh >= 4:
                if xx == 0 or yy == 0: col = [v + 14 for v in col]
                if xx == fw - 1 or yy == fh - 1: col = [v - 20 for v in col]
            put(x0 + xx, y0 + yy, col)
    if mat in ("steel", "light") and fw >= 10 and fh >= 8 and face in ("front", "back"):
        for xx in range(fw): shade_px(x0 + xx, y0 + fh // 2, 0.72)
    if mat in ("steel", "light", "dark") and fw >= 8 and fh >= 8:
        for (rx, ry) in ((2, 2), (fw - 3, 2), (2, fh - 3), (fw - 3, fh - 3)):
            put(x0 + rx, y0 + ry, [v + 40 for v in base])
            put(x0 + rx + 1, y0 + ry + 1, [v - 35 for v in base])

# ---------------- per-part details ----------------
def f_head_main(c, F, s):
    x0, y0, fw, fh = F["back"]
    for cx in (fw // 3, 2 * fw // 3):
        for yy in range(3, fh - 3): glowpix(x0 + cx, y0 + yy, RED, 0.4)
    for face in ("left", "right"):
        x0, y0, fw, fh = F[face]
        for yy in (fh // 2, fh // 2 + 2):
            for xx in range(2, fw - 2): shade_px(x0 + xx, y0 + yy, 0.6)

def f_face_plate(c, F, s):
    x0, y0, fw, fh = F["front"]
    for yy in range(fh):
        for xx in (4, 5): shade_px(x0 + xx, y0 + yy, 0.7)
    for xx in range(fw):
        for yy in (0, 1): shade_px(x0 + xx, y0 + yy, 0.72)
    eye = [(0, 3), (1, 3), (2, 3), (1, 4), (2, 4), (3, 4), (3, 5), (4, 5)]
    for (ex, ey) in eye:
        for X in (ex, fw - 1 - ex): glowpix(x0 + X, y0 + ey, (255, 105, 35), 0.45)
    for (ex, ey) in [(2, 3), (2, 4), (3, 4)]:
        for X in (ex, fw - 1 - ex): glowpix(x0 + X, y0 + ey, HOT, 0.5)
    for xx in list(range(1, 4)) + list(range(6, 9)): shade_px(x0 + xx, y0 + 7, 0.6)

def f_jaw_grill(c, F, s):
    x0, y0, fw, fh = F["front"]
    for yy in range(fh):
        for xx in range(fw):
            if xx % 2 == 0: put(x0 + xx, y0 + yy, (26, 20, 22))
            else: glowpix(x0 + xx, y0 + yy, (210, 40, 28), 0.3)

def f_brow(c, F, s):
    x0, y0, fw, fh = F["front"]
    for xx in range(1, fw - 1): glowpix(x0 + xx, y0 + fh - 1, RED, 0.5)

def f_ear(c, F, s):
    for face in ("left", "right"):
        x0, y0, fw, fh = F[face]
        for yy in range(2, fh - 2): glowpix(x0 + fw // 2, y0 + yy, RED, 0.45)

def f_neck(c, F, s):
    for face in ("front", "back", "left", "right"):
        x0, y0, fw, fh = F[face]
        for yy in range(fh):
            for xx in range(fw):
                if yy % 2 == 0: glowpix(x0 + xx, y0 + yy, (230, 50, 30), 0.45)
                else: put(x0 + xx, y0 + yy, (28, 22, 24))

def f_chest(c, F, s):
    x0, y0, fw, fh = F["front"]
    for yy in range(fh):
        for xx in (fw // 2 - 1, fw // 2): glowpix(x0 + xx, y0 + yy, ORANGE, 0.55)
    for yy in (10, 12):
        for xx in range(3, fw - 3): glowpix(x0 + xx, y0 + yy, RED, 0.5)
    x0, y0, fw, fh = F["back"]
    for yy in (4, 6, 8):
        for xx in range(4, fw - 4): glowpix(x0 + xx, y0 + yy, RED, 0.4)

def f_pec(c, F, s):
    x0, y0, fw, fh = F["front"]
    for xx in range(fw):
        yy = int(xx * (fh - 1) / (fw - 1))
        if s == "n": yy = fh - 1 - yy
        put(x0 + xx, y0 + yy, (46, 50, 58))
    ex = fw - 1 if s == "n" else 0
    for yy in range(fh): glowpix(x0 + ex, y0 + yy, RED, 0.5)

def f_abdomen(c, F, s):
    for face in ("front", "back", "left", "right"):
        x0, y0, fw, fh = F[face]
        for yy in range(fh):
            for xx in range(fw):
                m = yy % 4
                if m == 0: glowpix(x0 + xx, y0 + yy, (255, 60, 35), 0.5)
                elif m == 1: put(x0 + xx, y0 + yy, (30, 24, 26))
                else: shade_px(x0 + xx, y0 + yy, 1.5)

def f_spine(c, F, s):
    x0, y0, fw, fh = F["back"]
    for yy in range(fh):
        if yy % 4 in (1, 2):
            for xx in (1, 2): glowpix(x0 + xx, y0 + yy, RED, 0.5)

def f_pelvis(c, F, s):
    for face in ("front", "left", "right", "back"):
        x0, y0, fw, fh = F[face]
        for xx in range(1, fw - 1): glowpix(x0 + xx, y0 + 2, RED, 0.5)

def f_fauld(c, F, s):
    x0, y0, fw, fh = F["front"]
    for yy in range(1, fh - 1): glowpix(x0 + fw // 2, y0 + yy, RED, 0.5)

def f_shoulder(c, F, s):
    x0, y0, fw, fh = F["top"]
    for yy in range(1, fh - 1):
        if yy % 3 != 2:
            for xx in (fw // 2 - 1, fw // 2): glowpix(x0 + xx, y0 + yy, RED, 0.5)

def f_upper_arm(c, F, s):
    for face in ("front", "back", "left", "right"):
        x0, y0, fw, fh = F[face]
        for xx in range(fw): glowpix(x0 + xx, y0 + 3, RED, 0.45)

def f_elbow(c, F, s):
    for face in ("front", "back", "left", "right"):
        x0, y0, fw, fh = F[face]
        for yy in (fh // 2 - 1, fh // 2):
            for xx in range(fw): glowpix(x0 + xx, y0 + yy, ORANGE, 0.6)

def f_forearm(c, F, s):
    for face in ("front", "back"):
        x0, y0, fw, fh = F[face]
        for yy in range(2, fh - 2):
            for xx in (fw // 2 - 1, fw // 2): glowpix(x0 + xx, y0 + yy, RED, 0.45)

def f_finger(c, F, s):
    x0, y0, fw, fh = F["front"]
    for xx in range(fw): glowpix(x0 + xx, y0 + fh - 1, ORANGE, 0.6)

def f_knee(c, F, s):
    x0, y0, fw, fh = F["front"]
    for yy in (2, 3):
        for xx in (3, 4): glowpix(x0 + xx, y0 + yy, ORANGE, 0.6)

def f_calf(c, F, s):
    x0, y0, fw, fh = F["back"]
    for yy in range(1, fh - 1):
        for xx in (fw // 2 - 1, fw // 2): glowpix(x0 + xx, y0 + yy, RED, 0.5)

FEATURES = {"head_main": f_head_main, "face_plate": f_face_plate, "jaw_grill": f_jaw_grill,
            "brow": f_brow, "ear": f_ear, "neck": f_neck, "chest_main": f_chest,
            "pec": f_pec, "abdomen": f_abdomen, "spine": f_spine, "pelvis": f_pelvis,
            "fauld": f_fauld, "shoulder": f_shoulder, "upper_arm": f_upper_arm,
            "elbow": f_elbow, "forearm": f_forearm, "finger0": f_finger,
            "finger1": f_finger, "finger2": f_finger, "knee": f_knee, "calf": f_calf}

for c in cubes:
    F = faces(c)
    for face, rect in F.items():
        paint_face(face, rect, c["mat"])
    base, side = c["name"][:-2], c["name"][-1]
    fn = FEATURES.get(base) or FEATURES.get(c["name"])
    if fn: fn(c, F, side)

os.makedirs(TEX_DIR, exist_ok=True)
tex.save(TEX_DIR + "ultron.png")
glow.save(TEX_DIR + "ultron_glow.png")

# ---- preview (front view) ----
ox, oy, sc = 24, 36, 8
prev = Image.new("RGBA", (48, 90), (28, 30, 38, 255)); pp = prev.load()
for c in sorted(cubes, key=lambda c: -(c["P"][2] + c["S"][2] / 2)):
    x0, y0, fw, fh = faces(c)["front"]
    px, py = c["P"][0], c["P"][1]
    for yy in range(fh):
        for xx in range(fw):
            X, Y = int(ox + px + xx), int(oy + py + yy)
            if 0 <= X < 48 and 0 <= Y < 90:
                r, g, b, a = tp[x0 + xx, y0 + yy]
                gr, gg, gb, ga = gp[x0 + xx, y0 + yy]
                if ga: r, g, b = min(255, r + gr), min(255, g + gg), min(255, b + gb)
                pp[X, Y] = (r, g, b, 255)
prev.resize((48 * sc, 90 * sc), Image.NEAREST).save(ROOT + "/tools/preview_front.png")

# ---- emit UltronModel.java ----
def fm(v): return "%.1fF" % float(v)
parts_java = []
for part, piv in PIVOTS.items():
    pv = tuple(v * S for v in piv)
    lines = ['        root.addOrReplaceChild("%s", CubeListBuilder.create()' % part]
    for c in cubes:
        if c["part"] != part: continue
        u, v = c["uv"]; w, h, d = c["S"]
        px, py, pz = c["P"]
        lines.append("                .texOffs(%d, %d).addBox(%s, %s, %s, %s, %s, %s)" % (
            u, v, fm(px - pv[0]), fm(py - pv[1]), fm(pz - pv[2]), fm(w), fm(h), fm(d)))
    lines.append("                , PartPose.offset(%s, %s, %s));" % (fm(pv[0]), fm(pv[1]), fm(pv[2])))
    parts_java.append("\n".join(lines))

TEMPLATE = open(os.path.join(os.path.dirname(os.path.abspath(__file__)), "UltronModel.java.template")).read()
out = TEMPLATE.replace("@@PARTS@@", "\n".join(parts_java)).replace("@@TEXW@@", str(TEXW)).replace("@@TEXH@@", str(TEXH))
open(JAVA_OUT, "w").write(out)
print("cubes:", len(cubes))
