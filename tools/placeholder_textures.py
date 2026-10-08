"""Generate placeholder 16x16 block textures for the four End zones."""
import math
import random
import zlib
from pathlib import Path

from PIL import Image

OUT = Path(__file__).resolve().parent.parent / 'src/main/resources/assets/etjourney/textures/blocks'
SIZE = 16

# zone -> (surface block name, surface colour, rock colour)
ZONES = {
    'solar': ('crust', (214, 96, 28), (122, 58, 40)),
    'vortex': ('turf', (40, 170, 160), (62, 78, 70)),
    'nebula': ('moss', (140, 80, 180), (96, 62, 118)),
    'stardust': ('crust', (196, 218, 240), (150, 166, 196)),
}


def rng_for(name):
    # crc32 instead of hash() so the seed does not depend on PYTHONHASHSEED
    return random.Random(zlib.crc32(name.encode()))


def shade(color, factor):
    return tuple(max(0, min(255, round(c * factor))) for c in color) + (255,)


def noise(rng, color, amp):
    return shade(color, 1 + rng.uniform(-amp, amp))


def rock(zone):
    rng = rng_for(zone + '_rock')
    base = ZONES[zone][2]
    img = Image.new('RGBA', (SIZE, SIZE))
    for y in range(SIZE):
        for x in range(SIZE):
            img.putpixel((x, y), noise(rng, base, 0.12))
    # a few darker pits so it reads as stone rather than flat noise
    for _ in range(6):
        x, y = rng.randrange(SIZE), rng.randrange(SIZE)
        img.putpixel((x, y), shade(base, 0.7))
    return img


def cracks(rng):
    # connected random walks that wrap at the edges so the texture tiles
    cells = set()
    for i in range(4):
        a, b = rng.randrange(SIZE), rng.randrange(SIZE)
        for _ in range(rng.randint(6, 10)):
            # alternate walks run along x or along y, drifting sideways
            cells.add((a % SIZE, b % SIZE) if i % 2 else (b % SIZE, a % SIZE))
            a += 1
            b += rng.choice((-1, 0, 0, 1))
    return cells


def surface_pixel(zone, rng, x, y, crack_cells=frozenset()):
    color = ZONES[zone][1]
    if zone == 'solar':
        return noise(rng, (30, 22, 20), 0.2) if (x, y) in crack_cells else noise(rng, color, 0.15)
    if zone == 'vortex':
        wave = math.sin((x + y * 0.6) * 0.9 + math.sin(y * 0.7) * 2)
        return noise(rng, shade(color, 1.35)[:3], 0.08) if wave > 0.75 else noise(rng, color, 0.12)
    if zone == 'nebula':
        return noise(rng, (232, 130, 196), 0.1) if rng.random() < 0.14 else noise(rng, color, 0.18)
    # stardust: rare bright star grains
    return (255, 255, 255, 255) if rng.random() < 0.05 else noise(rng, color, 0.08)


def top(zone):
    rng = rng_for(zone + '_top')
    crack_cells = cracks(rng)
    img = Image.new('RGBA', (SIZE, SIZE))
    for y in range(SIZE):
        for x in range(SIZE):
            img.putpixel((x, y), surface_pixel(zone, rng, x, y, crack_cells))
    return img


def side(zone):
    rng = rng_for(zone + '_side')
    crack_cells = cracks(rng)
    img = rock(zone)
    for x in range(SIZE):
        for y in range(rng.randint(3, 5)):
            img.putpixel((x, y), surface_pixel(zone, rng, x, y, crack_cells))
    return img


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    for zone, (surface, _, _) in ZONES.items():
        top(zone).save(OUT / f'{zone}_{surface}_top.png')
        side(zone).save(OUT / f'{zone}_{surface}_side.png')
        rock(zone).save(OUT / f'{zone}_rock.png')


if __name__ == '__main__':
    main()
