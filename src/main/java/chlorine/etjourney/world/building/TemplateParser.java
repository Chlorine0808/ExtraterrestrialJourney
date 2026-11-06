package chlorine.etjourney.world.building;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import chlorine.etjourney.core.util.ModLog;

/**
 * Reads the structure JSON that OKModular's wand scan writes: one entry, layers top to bottom, rows along z,
 * characters along x. Only the layers and mappings are read.
 */
public final class TemplateParser {

    static final char BLANK = ' ', AIR = '_', KEEP = '.', ANCHOR = 'Q';
    /** Reserved for the joints of rooms joined into one building; not yet given a meaning. */
    static final char JOINT = '@';

    private TemplateParser() {}

    public static Template parse(String name, Reader json, boolean blankIsAir) throws TemplateException {
        try {
            return read(name, new JsonParser().parse(json), blankIsAir);
        } catch (RuntimeException e) {
            // Gson throws unchecked exceptions for a wrong shape (a missing key, a string for a number).
            throw new TemplateException("malformed JSON: " + e);
        }
    }

    private static Template read(String name, JsonElement root, boolean blankIsAir) throws TemplateException {
        if (!root.isJsonArray() || root.getAsJsonArray()
            .size() != 1) throw new TemplateException("the file must hold an array of exactly one building");
        JsonObject entry = root.getAsJsonArray()
            .get(0)
            .getAsJsonObject();
        Map<Character, Cell> mappings = mappings(entry.getAsJsonObject("mappings"));
        Cell blank = blankIsAir ? Cell.AIR : Cell.KEEP;

        JsonArray layers = entry.getAsJsonArray("layers");
        if (layers == null || layers.size() == 0) throw new TemplateException("no layers");
        // Split every row into its cells first; a braced id is one cell.
        List<List<List<Cell>>> rows = new ArrayList<>();
        int sizeX = 0, sizeZ = 0;
        int[] anchor = null;
        boolean warned = false;
        int sizeY = layers.size();
        for (int i = 0; i < sizeY; i++) {
            int y = sizeY - 1 - i;
            JsonArray layerRows = layers.get(i)
                .getAsJsonObject()
                .getAsJsonArray("rows");
            List<List<Cell>> layer = new ArrayList<>();
            for (int z = 0; z < layerRows.size(); z++) {
                String row = layerRows.get(z)
                    .getAsString();
                List<Cell> cells = new ArrayList<>();
                for (int c = 0; c < row.length(); c++) {
                    char ch = row.charAt(c);
                    if (ch == '{') {
                        int end = row.indexOf('}', c);
                        if (end < 0) throw new TemplateException("unclosed '{' in row '" + row + "'");
                        cells.add(Cell.of(BlockRef.parse(row.substring(c + 1, end), null)));
                        c = end;
                        continue;
                    }
                    if (ch == ANCHOR) {
                        if (anchor != null) throw new TemplateException("more than one 'Q'");
                        anchor = new int[] { cells.size(), y, z };
                    }
                    if (ch == JOINT && !warned) {
                        ModLog.LOG.warn("Building {}: '@' is reserved for room joints and is read as blank", name);
                        warned = true;
                    }
                    cells.add(cell(ch, mappings, blank));
                }
                sizeX = Math.max(sizeX, cells.size());
                layer.add(cells);
            }
            sizeZ = Math.max(sizeZ, layer.size());
            rows.add(layer);
        }
        if (sizeX == 0 || sizeZ == 0) throw new TemplateException("empty building");

        Cell[] cells = new Cell[sizeX * sizeY * sizeZ];
        for (int i = 0; i < sizeY; i++) {
            int y = sizeY - 1 - i;
            List<List<Cell>> layer = rows.get(i);
            for (int z = 0; z < sizeZ; z++) {
                List<Cell> row = z < layer.size() ? layer.get(z) : null;
                for (int x = 0; x < sizeX; x++)
                    cells[Template.index(sizeX, sizeZ, x, y, z)] = row != null && x < row.size() ? row.get(x) : blank;
            }
        }
        if (anchor == null) anchor = new int[] { sizeX / 2, 0, sizeZ / 2 };
        return new Template(name, sizeX, sizeY, sizeZ, cells, anchor[0], anchor[1], anchor[2]);
    }

    private static Cell cell(char ch, Map<Character, Cell> mappings, Cell blank) throws TemplateException {
        Cell mapped = mappings.get(ch);
        if (mapped != null) return mapped;
        switch (ch) {
            case BLANK:
            case ANCHOR:
            case JOINT:
                return blank;
            case AIR:
                return Cell.AIR;
            case KEEP:
                return Cell.KEEP;
            default:
                throw new TemplateException("'" + ch + "' has no mapping");
        }
    }

    private static Map<Character, Cell> mappings(JsonObject json) throws TemplateException {
        Map<Character, Cell> mappings = new HashMap<>();
        if (json == null) return mappings;
        for (Map.Entry<String, JsonElement> e : json.entrySet()) {
            if (e.getKey()
                .length() != 1) throw new TemplateException("mapping key '" + e.getKey() + "' is not one character");
            mappings.put(
                e.getKey()
                    .charAt(0),
                mapping(e.getKey(), e.getValue()));
        }
        return mappings;
    }

    private static Cell mapping(String key, JsonElement value) throws TemplateException {
        if (value.isJsonPrimitive()) {
            String id = value.getAsString();
            return id.equals("_") ? Cell.AIR : Cell.of(BlockRef.parse(id, null));
        }
        JsonObject obj = value.getAsJsonObject();
        if (obj.has("tiers") || obj.has("component")) throw new TemplateException(
            "mapping '" + key + "' uses OKModular machine keys, which buildings do not read");
        if (obj.has("block")) {
            String loot = obj.has("loot") ? obj.get("loot")
                .getAsString() : null;
            return Cell.of(
                BlockRef.parse(
                    obj.get("block")
                        .getAsString(),
                    loot));
        }
        JsonArray blocks = obj.getAsJsonArray("blocks");
        if (blocks == null || blocks.size() == 0) throw new TemplateException("mapping '" + key + "' has no blocks");
        BlockRef[] choices = new BlockRef[blocks.size()];
        for (int i = 0; i < choices.length; i++) choices[i] = BlockRef.parse(
            blocks.get(i)
                .getAsString(),
            null);
        double[] weights = new double[choices.length];
        JsonArray w = obj.getAsJsonArray("weights");
        if (w == null) Arrays.fill(weights, 1);
        else {
            if (w.size() != choices.length) throw new TemplateException(
                "mapping '" + key + "' has " + w.size() + " weights for " + choices.length + " blocks");
            double total = 0;
            for (int i = 0; i < weights.length; i++) {
                weights[i] = w.get(i)
                    .getAsDouble();
                if (weights[i] < 0) throw new TemplateException("mapping '" + key + "' has a negative weight");
                total += weights[i];
            }
            if (total <= 0) throw new TemplateException("mapping '" + key + "' has weights that add up to zero");
        }
        return Cell.of(choices, weights);
    }
}
