package com.lin.thermonuclear.machine.nuclear;

import java.util.ArrayList;
import java.util.List;

import com.lin.thermonuclear.machine.nuclear.NuclearModuleGeometry.Position;

/** Formal module fragment: P = main-tier pipe, F = containment, D = main-tier axial fuel rod. */
final class NuclearModuleStructure {

    static final int MAX_MODULES = 5;
    static final String MODULE = "nuclear_module";
    private static final String[][] TEMPLATE = NuclearModuleShape.create();
    private static final List<RequiredBlock> BLOCKS = createBlocks();

    private NuclearModuleStructure() {}

    static int moduleLimit(int pipeTier) {
        // Retain the established pipe-tier module budget, not a budget for removed connection segments.
        return switch (pipeTier) {
            case 1 -> 1;
            case 2 -> 3;
            case 3 -> MAX_MODULES;
            default -> 0;
        };
    }

    static String[][] shape() {
        String[][] copy = new String[TEMPLATE.length][];
        for (int i = 0; i < copy.length; i++) copy[i] = TEMPLATE[i].clone();
        return copy;
    }

    private static List<RequiredBlock> createBlocks() {
        if (TEMPLATE.length != NuclearModuleGeometry.LENGTH) throw new IllegalStateException("Module slice count");
        List<RequiredBlock> blocks = new ArrayList<>();
        for (int z = 0; z < TEMPLATE.length; z++) {
            if (TEMPLATE[z].length != NuclearModuleGeometry.HEIGHT) throw new IllegalStateException("Module height");
            for (int y = 0; y < TEMPLATE[z].length; y++) {
                if (TEMPLATE[z][y].length() != NuclearModuleGeometry.WIDTH)
                    throw new IllegalStateException("Module width");
                for (int x = 0; x < TEMPLATE[z][y].length(); x++) {
                    char material = TEMPLATE[z][y].charAt(x);
                    if (material == ' ') continue;
                    if (material != 'P' && material != 'F' && material != 'D')
                        throw new IllegalStateException("Module material");
                    blocks.add(new RequiredBlock(new Position(x, y, z), material));
                }
            }
        }
        return java.util.Collections.unmodifiableList(blocks);
    }

    static List<RequiredBlock> blocks() {
        return BLOCKS;
    }

    static final class RequiredBlock {

        final Position position;
        final char material;

        RequiredBlock(Position position, char material) {
            this.position = position;
            this.material = material;
        }
    }
}
