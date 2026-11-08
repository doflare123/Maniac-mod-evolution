"""Install the supplied Blockbench syringe exports without changing their source projects."""
import argparse
import json
import itertools
import math
import shutil
from pathlib import Path


def fit_display(geometry, rotation, max_size=14, ground=False):
    """Match Minecraft ItemTransform's XYZ rotation around the recentered Gecko mesh."""
    x, y, z = map(math.radians, rotation)
    cx, cy, cz = math.cos(x), math.cos(y), math.cos(z)
    sx, sy, sz = math.sin(x), math.sin(y), math.sin(z)
    points = []
    for bone in geometry["bones"]:
        assert not any(bone.get("rotation", [0, 0, 0]))
        for cube in bone.get("cubes", []):
            assert not any(cube.get("rotation", [0, 0, 0]))
            inflate = cube.get("inflate", 0)
            for bits in itertools.product((0, 1), repeat=3):
                a, b, c = [cube["origin"][i] + bits[i] * cube["size"][i] + (inflate if bits[i] else -inflate) for i in range(3)]
                a = -a  # GeckoLib's baked geometry reflects model X.
                a, b = cz * a - sz * b, sz * a + cz * b
                a, c = cy * a + sy * c, -sy * a + cy * c
                b, c = cx * b - sx * c, sx * b + cx * c
                points.append((a, b, c))
    low = [min(p[i] for p in points) for i in range(3)]
    high = [max(p[i] for p in points) for i in range(3)]
    scale = .5 if ground else min(1, max_size / max(high[i] - low[i] for i in (0, 1)))
    translation = [-(low[i] + high[i]) * scale / 2 for i in range(3)]
    if ground:
        translation[1] = 1 - low[1] * scale
    return {"rotation": rotation, "translation": [round(v, 6) for v in translation], "scale": [round(scale, 6)] * 3}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("source", type=Path)
    args = parser.parse_args()
    source = args.source.resolve()
    assets = Path(__file__).resolve().parents[1] / "src/main/resources/assets/maniacrev"
    stem = "syringe_V1_Skay_animated"
    copies = {
        stem + ".geo.json": "geo/syringe.geo.json",
        stem + ".animation.json": "animations/syringe.animation.json",
        "paired/syringe_held.animation.json": "animations/syringe_held.animation.json",
        "paired/syringe_player.animation.json": "animations/syringe_player.animation.json",
        "syringeText_animated.png": "textures/item/syringe.png",
    }
    for src, dst in copies.items():
        target = assets / dst
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(source / src, target)
    project = json.loads((source / (stem + ".bbmodel")).read_text(encoding="utf-8"))
    display = {name: {k: value for k, value in transform.items() if k in ("rotation", "translation", "scale")}
               for name, transform in project["display"].items()}
    # Player rendering uses the paired shoulder/hand mount, without display transforms.
    for name in ("thirdperson_righthand", "thirdperson_lefthand", "firstperson_righthand", "firstperson_lefthand"):
        display[name] = {}
    geometry = json.loads((source / (stem + ".geo.json")).read_text())["minecraft:geometry"][0]
    for name in ("gui", "fixed", "ground"):
        display[name] = fit_display(geometry, display[name]["rotation"], ground=name == "ground")
    model = {"parent": "builtin/entity", "textures": {"particle": "maniacrev:item/syringe"}, "display": display}
    (assets / "models/item/syringe.json").write_text(json.dumps(model, indent=2) + "\n", encoding="utf-8")
    print("Installed syringe geometry, texture, three animation exports and item display settings.")


if __name__ == "__main__":
    main()
