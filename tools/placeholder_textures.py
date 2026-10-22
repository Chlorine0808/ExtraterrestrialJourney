"""Generate placeholder 16x16 block textures for the End zones, biomes and ores."""
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
    # biomes of the continent
    'grove': ('moss', (92, 140, 70), (78, 66, 52)),
    'highland': ('crust', (200, 198, 188), (176, 150, 104)),
    'mycelia': ('turf', (120, 82, 96), (60, 52, 62)),
    'amber': ('crust', (226, 140, 40), (170, 108, 46)),
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
    if zone == 'grove':
        return noise(rng, (60, 98, 46), 0.1) if rng.random() < 0.25 else noise(rng, color, 0.15)
    if zone == 'highland':
        return noise(rng, (150, 148, 140), 0.1) if (x, y) in crack_cells else noise(rng, color, 0.06)
    if zone == 'mycelia':
        return noise(rng, (190, 170, 180), 0.08) if rng.random() < 0.08 else noise(rng, color, 0.15)
    if zone == 'amber':
        return noise(rng, (255, 200, 90), 0.06) if rng.random() < 0.1 else noise(rng, color, 0.1)
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


END_STONE = (219, 222, 158)

# ore block -> grain colour, set in end stone
ORES = {
    'ender_gold_ore': (240, 190, 40),
    'ender_platinum_ore': (210, 230, 235),
    'sakura_gold_ore': (240, 140, 170),
    'golden_palladium_ore': (200, 160, 70),
    'drift_ore': (120, 220, 200),
    'ascended_crystal_ore': (150, 200, 255),
    'black_quartz_ore': (30, 26, 34),
}


def ore(name):
    rng = rng_for(name)
    img = Image.new('RGBA', (SIZE, SIZE))
    for y in range(SIZE):
        for x in range(SIZE):
            img.putpixel((x, y), noise(rng, END_STONE, 0.06))
    # clumps of two to four grains, dark-edged so they read on any rock
    for _ in range(5):
        cx, cy = rng.randrange(SIZE), rng.randrange(SIZE)
        for _ in range(rng.randint(2, 4)):
            img.putpixel((cx % SIZE, cy % SIZE), noise(rng, ORES[name], 0.1))
            img.putpixel(((cx + 1) % SIZE, (cy + 1) % SIZE), shade(ORES[name], 0.55))
            cx += rng.choice((-1, 0, 1))
            cy += rng.choice((-1, 0, 1))
    return img


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    for zone, (surface, _, _) in ZONES.items():
        top(zone).save(OUT / f'{zone}_{surface}_top.png')
        side(zone).save(OUT / f'{zone}_{surface}_side.png')
        rock(zone).save(OUT / f'{zone}_rock.png')
    for name in ORES:
        ore(name).save(OUT / f'{name}.png')


if __name__ == '__main__':
    main()
