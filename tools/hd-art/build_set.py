"""Write the full HD bubble set into res/drawable-nodpi as <name>_hd.png."""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from PIL import Image
from hdbubble import hd_bubble, hd_colourblind_shapes, hd_frozen, hd_fixed

res = sys.argv[1]
src = os.path.join(res, "drawable")
dst = os.path.join(res, "drawable-nodpi")
os.makedirs(dst, exist_ok=True)

def load(name):
    return Image.open(os.path.join(src, name + ".png")).convert("RGBA")

def save(img, name):
    img.save(os.path.join(dst, name + "_hd.png"), optimize=True)

for i in range(1, 9):
    base = load("bubble_%d" % i)
    save(hd_bubble(base), "bubble_%d" % i)
    save(hd_colourblind_shapes(base, i), "bubble_colourblind_%d" % i)
    save(hd_frozen(load("frozen_%d" % i)), "frozen_%d" % i)
for i in range(1, 7):
    save(hd_fixed(i), "fixed_%d" % i)
save(hd_bubble(load("bubble_blink")), "bubble_blink")
files = sorted(f for f in os.listdir(dst) if f.endswith("_hd.png"))
total = sum(os.path.getsize(os.path.join(dst, f)) for f in files)
print(len(files), "files,", total // 1024, "KB")
