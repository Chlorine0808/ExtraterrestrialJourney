"""Generate placeholder 16x16 textures for the End zones, biomes, ores, plants, large plants, crystals and drops."""
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


# plant -> (shape, stem or blade colour, head colour)
PLANTS = {
    'grove_grass': ('blades', (110, 170, 80), None),
    'grove_fern': ('fern', (70, 140, 70), None),
    'grove_bloom': ('flower', (80, 130, 60), (245, 160, 190)),
    'highland_tuft': ('blades', (190, 180, 120), None),
    'highland_thistle': ('flower', (140, 150, 110), (170, 110, 200)),
    'mycelia_sprout': ('blades', (170, 120, 150), None),
    'glowcap': ('mushroom', (200, 190, 170), (120, 230, 210)),
    'amber_reed': ('blades', (220, 150, 60), None),
    'amber_bulb': ('bulb', (160, 100, 40), (255, 190, 60)),
}


def put(img, x, y, color):
    # plants are drawn crossed, so pixels off the sheet or in its top corners are dropped
    if 0 <= x < SIZE and 0 <= y < SIZE and (x, y) not in ((0, 0), (SIZE - 1, 0)):
        img.putpixel((x, y), color)


def stem(img, rng, x, height, color):
    for i in range(height):
        put(img, x, SIZE - 1 - i, noise(rng, color, 0.1))


def plant(name):
    rng = rng_for(name)
    shape, body, head = PLANTS[name]
    img = Image.new('RGBA', (SIZE, SIZE), (0, 0, 0, 0))
    if shape == 'blades':
        for _ in range(7):
            x, lean = rng.randrange(2, 14), rng.choice((-1, 0, 1))
            height = rng.randint(5, 13)
            for i in range(height):
                # darker at the root, lighter at the tip
                put(img, x + lean * (i // 4), SIZE - 1 - i, shade(body, 0.7 + 0.5 * i / height))
    elif shape == 'fern':
        stem(img, rng, 8, 13, shade(body, 0.8)[:3])
        for i in range(2, 13, 2):
            reach = max(1, (13 - i) // 2)
            for d in range(1, reach + 1):
                put(img, 8 - d, SIZE - 1 - i + d // 2, noise(rng, body, 0.1))
                put(img, 8 + d, SIZE - 2 - i + d // 2, noise(rng, body, 0.1))
    elif shape == 'flower':
        for x, height in ((5, 8), (10, 10)):
            stem(img, rng, x, height, body)
            top = SIZE - 1 - height
            for dx, dy in ((0, 0), (-1, 0), (1, 0), (0, -1), (0, 1), (-1, -1), (1, -1)):
                put(img, x + dx, top + dy, noise(rng, head, 0.08))
            put(img, x, top, shade(head, 0.6))
    elif shape == 'mushroom':
        for x, height, width in ((5, 5, 2), (10, 8, 3)):
            stem(img, rng, x, height, body)
            top = SIZE - 1 - height
            for dx in range(-width, width + 1):
                put(img, x + dx, top, noise(rng, head, 0.1))
                if abs(dx) < width:
                    put(img, x + dx, top - 1, shade(head, 1.15))
    else:  # bulb on a short stem
        stem(img, rng, 8, 7, body)
        for dx in range(-2, 3):
            for dy in range(-2, 3):
                if dx * dx + dy * dy <= 5:
                    put(img, 8 + dx, 6 + dy, noise(rng, head, 0.06))
        put(img, 7, 5, (255, 240, 200, 255))
    return img


# sapling -> (trunk colour, leaf colour)
SAPLINGS = {
    'grove_sapling': ((92, 66, 42), (64, 126, 56)),
    'highland_sapling': ((148, 140, 126), (118, 142, 104)),
}


def sapling(name):
    rng = rng_for(name)
    trunk, leaf = SAPLINGS[name]
    img = Image.new('RGBA', (SIZE, SIZE), (0, 0, 0, 0))
    stem(img, rng, 8, 7, trunk)
    # a round tuft of leaves on the stem, with a lighter fleck here and there
    for dx in range(-4, 5):
        for dy in range(-4, 5):
            if dx * dx + dy * dy <= 14:
                color = shade(leaf, 1.25) if rng.random() < 0.2 else noise(rng, leaf, 0.1)
                put(img, 8 + dx, 5 + dy, color)
    return img


# log -> (bark colour, ring colour)
LOGS = {
    'grove_log': ((92, 66, 42), (156, 124, 84)),
    'highland_log': ((148, 140, 126), (204, 194, 172)),
    'mycelia_stem': ((196, 180, 166), (232, 218, 192)),
    'amber_root': ((146, 88, 40), (224, 154, 62)),
}

# planks -> colour
PLANKS = {
    'grove_planks': (168, 134, 88),
    'highland_planks': (190, 182, 160),
    'mycelia_planks': (206, 176, 186),
    'amber_planks': (214, 150, 70),
}

# opaque blocks of the large plants -> (base colour, fleck colour, fleck chance)
FOLIAGE = {
    'grove_leaves': ((64, 126, 56), (40, 92, 40), 0.3),
    'grove_blossom': ((88, 140, 70), (248, 170, 198), 0.35),
    'highland_leaves': ((118, 142, 104), (158, 170, 130), 0.25),
    'mycelia_cap': ((148, 68, 108), (200, 130, 160), 0.15),
    'mycelia_gill': ((120, 224, 204), (210, 255, 240), 0.2),
    'amber_seed': ((250, 184, 58), (255, 232, 150), 0.2),
    'amber_stem': ((176, 108, 48), (130, 76, 34), 0.25),
}

# crystal block -> (colour, facet colour)
CRYSTALS = {
    'crystal': ((150, 200, 255), (220, 240, 255)),
    'budding_crystal': ((104, 140, 220), (170, 200, 250)),
}

# crystal bud -> (spike heights from the left, colour); the cluster is the largest
BUDS = {
    'crystal_bud_small': ((3, 4), (140, 190, 250)),
    'crystal_bud_medium': ((6, 4, 7), (150, 200, 255)),
    'crystal_bud_large': ((9, 6, 10, 7), (160, 208, 255)),
    'crystal_cluster': ((13, 8, 11, 9, 12), (170, 215, 255)),
}


def log_side(name):
    rng = rng_for(name + '_side')
    bark = LOGS[name][0]
    img = Image.new('RGBA', (SIZE, SIZE))
    for x in range(SIZE):
        column = 1 + rng.uniform(-0.18, 0.18)
        for y in range(SIZE):
            img.putpixel((x, y), noise(rng, shade(bark, column)[:3], 0.06))
    return img


def log_top(name):
    rng = rng_for(name + '_top')
    bark, ring = LOGS[name]
    img = Image.new('RGBA', (SIZE, SIZE))
    for y in range(SIZE):
        for x in range(SIZE):
            r = math.hypot(x - 7.5, y - 7.5)
            if r > 6.8:
                img.putpixel((x, y), noise(rng, bark, 0.06))
            else:
                img.putpixel((x, y), noise(rng, shade(ring, 0.85 if int(r) % 2 else 1.05)[:3], 0.04))
    return img


def planks(name):
    rng = rng_for(name)
    base = PLANKS[name]
    img = Image.new('RGBA', (SIZE, SIZE))
    for y in range(SIZE):
        board = 1 + (0.08 if (y // 4) % 2 else -0.04)
        for x in range(SIZE):
            seam = y % 4 == 3
            img.putpixel((x, y), shade(base, 0.72) if seam else noise(rng, shade(base, board)[:3], 0.05))
    return img


def foliage(name):
    rng = rng_for(name)
    base, fleck, chance = FOLIAGE[name]
    img = Image.new('RGBA', (SIZE, SIZE))
    for y in range(SIZE):
        for x in range(SIZE):
            img.putpixel((x, y), noise(rng, fleck if rng.random() < chance else base, 0.1))
    return img


def crystal(name):
    rng = rng_for(name)
    base, facet = CRYSTALS[name]
    img = Image.new('RGBA', (SIZE, SIZE))
    for y in range(SIZE):
        for x in range(SIZE):
            # diagonal facets
            band = (x + y) % 8
            img.putpixel((x, y), noise(rng, facet if band < 2 else base, 0.06))
    if name == 'budding_crystal':
        for _ in range(8):
            img.putpixel((rng.randrange(SIZE), rng.randrange(SIZE)), (230, 250, 255, 255))
    return img


def bud(name):
    rng = rng_for(name)
    heights, color = BUDS[name]
    img = Image.new('RGBA', (SIZE, SIZE), (0, 0, 0, 0))
    step = SIZE // len(heights)
    for i, height in enumerate(heights):
        left = i * step + (step - 3) // 2
        for dy in range(height):
            # a spike narrowing towards its tip, lit from the left
            wide = 3 if dy < height - 3 else 2 if dy < height - 1 else 1
            for dx in range(wide):
                x = left + dx + (3 - wide) // 2
                light = 1.2 if dx == 0 else 0.9
                put(img, x, SIZE - 1 - dy, noise(rng, shade(color, light)[:3], 0.05))
    return img


ITEM_OUT = OUT.parent / 'items'

# ore drop item -> (shape, colour)
ITEMS = {
    'raw_ender_gold': ('lump', ORES['ender_gold_ore']),
    'raw_ender_platinum': ('lump', ORES['ender_platinum_ore']),
    'raw_sakura_gold': ('lump', ORES['sakura_gold_ore']),
    'raw_golden_palladium': ('lump', ORES['golden_palladium_ore']),
    'drift_shard': ('shard', ORES['drift_ore']),
    'ascended_crystal': ('gem', ORES['ascended_crystal_ore']),
    # lifted from the ore grain so the gem is not lost against dark slots
    'black_quartz': ('gem', (70, 62, 84)),
    'crystal_shard': ('shard', (150, 200, 255)),
}


def item(name):
    rng = rng_for(name)
    shape, color = ITEMS[name]
    if shape == 'lump':
        cells = {(x, y) for x in range(3, 13) for y in range(4, 13)
                 if (x - 7.5) ** 2 / 25 + (y - 8.5) ** 2 / 18 + rng.uniform(-0.25, 0.1) <= 1}
    elif shape == 'gem':
        cells = {(x, y) for x in range(SIZE) for y in range(2, 15) if abs(x - 7.5) + abs(y - 8) * 0.8 <= 6}
    else:  # shard: a thin diagonal splinter
        cells = {(x, y) for x in range(SIZE) for y in range(2, 14) if abs(x - (15 - y)) <= 1.5}
    img = Image.new('RGBA', (SIZE, SIZE), (0, 0, 0, 0))
    for x, y in sorted(cells):
        # dark rim where a neighbour is missing, lit from the top left inside
        edge = any((x + dx, y + dy) not in cells for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
        light = 1.25 - 0.025 * (x + y)
        img.putpixel((x, y), shade(color, 0.5) if edge else noise(rng, shade(color, light)[:3], 0.06))
    return img


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    ITEM_OUT.mkdir(parents=True, exist_ok=True)
    for name in PLANTS:
        plant(name).save(OUT / f'{name}.png')
    for name in SAPLINGS:
        sapling(name).save(OUT / f'{name}.png')
    for name in ITEMS:
        item(name).save(ITEM_OUT / f'{name}.png')
    for zone, (surface, _, _) in ZONES.items():
        top(zone).save(OUT / f'{zone}_{surface}_top.png')
        side(zone).save(OUT / f'{zone}_{surface}_side.png')
        rock(zone).save(OUT / f'{zone}_rock.png')
    for name in ORES:
        ore(name).save(OUT / f'{name}.png')
    for name in LOGS:
        log_side(name).save(OUT / f'{name}_side.png')
        log_top(name).save(OUT / f'{name}_top.png')
    for name in PLANKS:
        planks(name).save(OUT / f'{name}.png')
    for name in FOLIAGE:
        foliage(name).save(OUT / f'{name}.png')
    for name in CRYSTALS:
        crystal(name).save(OUT / f'{name}.png')
    for name in BUDS:
        bud(name).save(OUT / f'{name}.png')


if __name__ == '__main__':
    main()
