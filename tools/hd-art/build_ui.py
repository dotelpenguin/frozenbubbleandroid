"""Write the HD UI art (round buttons, launcher, tomato, banana, Bluetooth
icon) into the project's resources."""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import hdui

res = sys.argv[1]
nodpi = os.path.join(res, "drawable-nodpi")
xxxhdpi = os.path.join(res, "drawable-xxxhdpi")
os.makedirs(nodpi, exist_ok=True)
os.makedirs(xxxhdpi, exist_ok=True)
out = {
    "back_button_hd": hdui.back_button(),
    "pause_button_hd": hdui.pause_button(),
    "play_button_hd": hdui.play_button(),
    "next_song_button_hd": hdui.next_song_button(),
    "launcher_hd": hdui.launcher(),
    "tomato_hd": hdui.tomato(),
    "banana_hd": hdui.banana(),
}
for name, img in out.items():
    img.save(os.path.join(nodpi, name + ".png"), optimize=True)
# Dialog icon: Android picks this on high-density screens (32 dp at 4x).
hdui.bluetooth_icon().save(os.path.join(xxxhdpi, "bluetooth.png"), optimize=True)
print(len(out) + 1, "files written")
