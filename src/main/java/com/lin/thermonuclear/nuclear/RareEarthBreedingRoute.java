package com.lin.thermonuclear.nuclear;

import gregtech.api.enums.Materials;

/** Element-only endpoints; isotope bridges and intermediate decay products are intentionally omitted. */
public enum RareEarthBreedingRoute {

    CALCIUM_TO_SCANDIUM("calcium_to_scandium", Materials.Calcium, Materials.Scandium),
    STRONTIUM_TO_YTTRIUM("strontium_to_yttrium", Materials.Strontium, Materials.Yttrium),
    BARIUM_TO_LANTHANUM("barium_to_lanthanum", Materials.Barium, Materials.Lanthanum),
    LANTHANUM_TO_CERIUM("lanthanum_to_cerium", Materials.Lanthanum, Materials.Cerium),
    CERIUM_TO_PRASEODYMIUM("cerium_to_praseodymium", Materials.Cerium, Materials.Praseodymium),
    PRASEODYMIUM_TO_NEODYMIUM("praseodymium_to_neodymium", Materials.Praseodymium, Materials.Neodymium),
    NEODYMIUM_TO_PROMETHIUM("neodymium_to_promethium", Materials.Neodymium, Materials.Promethium),
    PROMETHIUM_TO_SAMARIUM("promethium_to_samarium", Materials.Promethium, Materials.Samarium),
    SAMARIUM_TO_EUROPIUM("samarium_to_europium", Materials.Samarium, Materials.Europium),
    EUROPIUM_TO_GADOLINIUM("europium_to_gadolinium", Materials.Europium, Materials.Gadolinium),
    GADOLINIUM_TO_TERBIUM("gadolinium_to_terbium", Materials.Gadolinium, Materials.Terbium),
    TERBIUM_TO_DYSPROSIUM("terbium_to_dysprosium", Materials.Terbium, Materials.Dysprosium),
    DYSPROSIUM_TO_HOLMIUM("dysprosium_to_holmium", Materials.Dysprosium, Materials.Holmium),
    HOLMIUM_TO_ERBIUM("holmium_to_erbium", Materials.Holmium, Materials.Erbium),
    ERBIUM_TO_THULIUM("erbium_to_thulium", Materials.Erbium, Materials.Thulium),
    THULIUM_TO_YTTERBIUM("thulium_to_ytterbium", Materials.Thulium, Materials.Ytterbium),
    YTTERBIUM_TO_LUTETIUM("ytterbium_to_lutetium", Materials.Ytterbium, Materials.Lutetium);

    private final String name;
    private final Materials inputMaterial;
    private final Materials outputMaterial;

    RareEarthBreedingRoute(String name, Materials inputMaterial, Materials outputMaterial) {
        this.name = name;
        this.inputMaterial = inputMaterial;
        this.outputMaterial = outputMaterial;
    }

    public String itemName(boolean bred) {
        return (bred ? "bred_" : "") + name + "_breeder_rod";
    }

    public Materials inputMaterial() {
        return inputMaterial;
    }

    public Materials outputMaterial() {
        return outputMaterial;
    }
}
