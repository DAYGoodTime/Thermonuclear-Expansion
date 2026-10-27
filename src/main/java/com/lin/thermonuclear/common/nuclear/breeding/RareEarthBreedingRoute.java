package com.lin.thermonuclear.common.nuclear.breeding;

import com.lin.thermonuclear.api.TNCItemList;

import gregtech.api.enums.Materials;

/** Element-only endpoints; isotope bridges and intermediate decay products are intentionally omitted. */
public enum RareEarthBreedingRoute {

    CALCIUM_TO_SCANDIUM("calcium_to_scandium", Materials.Calcium, Materials.Scandium,
        TNCItemList.CalciumToScandiumBreederRod, TNCItemList.BredCalciumToScandiumBreederRod),
    STRONTIUM_TO_YTTRIUM("strontium_to_yttrium", Materials.Strontium, Materials.Yttrium,
        TNCItemList.StrontiumToYttriumBreederRod, TNCItemList.BredStrontiumToYttriumBreederRod),
    BARIUM_TO_LANTHANUM("barium_to_lanthanum", Materials.Barium, Materials.Lanthanum,
        TNCItemList.BariumToLanthanumBreederRod, TNCItemList.BredBariumToLanthanumBreederRod),
    LANTHANUM_TO_CERIUM("lanthanum_to_cerium", Materials.Lanthanum, Materials.Cerium,
        TNCItemList.LanthanumToCeriumBreederRod, TNCItemList.BredLanthanumToCeriumBreederRod),
    CERIUM_TO_PRASEODYMIUM("cerium_to_praseodymium", Materials.Cerium, Materials.Praseodymium,
        TNCItemList.CeriumToPraseodymiumBreederRod, TNCItemList.BredCeriumToPraseodymiumBreederRod),
    PRASEODYMIUM_TO_NEODYMIUM("praseodymium_to_neodymium", Materials.Praseodymium, Materials.Neodymium,
        TNCItemList.PraseodymiumToNeodymiumBreederRod, TNCItemList.BredPraseodymiumToNeodymiumBreederRod),
    NEODYMIUM_TO_PROMETHIUM("neodymium_to_promethium", Materials.Neodymium, Materials.Promethium,
        TNCItemList.NeodymiumToPromethiumBreederRod, TNCItemList.BredNeodymiumToPromethiumBreederRod),
    PROMETHIUM_TO_SAMARIUM("promethium_to_samarium", Materials.Promethium, Materials.Samarium,
        TNCItemList.PromethiumToSamariumBreederRod, TNCItemList.BredPromethiumToSamariumBreederRod),
    SAMARIUM_TO_EUROPIUM("samarium_to_europium", Materials.Samarium, Materials.Europium,
        TNCItemList.SamariumToEuropiumBreederRod, TNCItemList.BredSamariumToEuropiumBreederRod),
    EUROPIUM_TO_GADOLINIUM("europium_to_gadolinium", Materials.Europium, Materials.Gadolinium,
        TNCItemList.EuropiumToGadoliniumBreederRod, TNCItemList.BredEuropiumToGadoliniumBreederRod),
    GADOLINIUM_TO_TERBIUM("gadolinium_to_terbium", Materials.Gadolinium, Materials.Terbium,
        TNCItemList.GadoliniumToTerbiumBreederRod, TNCItemList.BredGadoliniumToTerbiumBreederRod),
    TERBIUM_TO_DYSPROSIUM("terbium_to_dysprosium", Materials.Terbium, Materials.Dysprosium,
        TNCItemList.TerbiumToDysprosiumBreederRod, TNCItemList.BredTerbiumToDysprosiumBreederRod),
    DYSPROSIUM_TO_HOLMIUM("dysprosium_to_holmium", Materials.Dysprosium, Materials.Holmium,
        TNCItemList.DysprosiumToHolmiumBreederRod, TNCItemList.BredDysprosiumToHolmiumBreederRod),
    HOLMIUM_TO_ERBIUM("holmium_to_erbium", Materials.Holmium, Materials.Erbium, TNCItemList.HolmiumToErbiumBreederRod,
        TNCItemList.BredHolmiumToErbiumBreederRod),
    ERBIUM_TO_THULIUM("erbium_to_thulium", Materials.Erbium, Materials.Thulium, TNCItemList.ErbiumToThuliumBreederRod,
        TNCItemList.BredErbiumToThuliumBreederRod),
    THULIUM_TO_YTTERBIUM("thulium_to_ytterbium", Materials.Thulium, Materials.Ytterbium,
        TNCItemList.ThuliumToYtterbiumBreederRod, TNCItemList.BredThuliumToYtterbiumBreederRod),
    YTTERBIUM_TO_LUTETIUM("ytterbium_to_lutetium", Materials.Ytterbium, Materials.Lutetium,
        TNCItemList.YtterbiumToLutetiumBreederRod, TNCItemList.BredYtterbiumToLutetiumBreederRod);

    private final String name;
    private final Materials inputMaterial;
    private final Materials outputMaterial;
    private final TNCItemList workingRod;
    private final TNCItemList bredRod;

    RareEarthBreedingRoute(String name, Materials inputMaterial, Materials outputMaterial, TNCItemList workingRod,
        TNCItemList bredRod) {
        this.name = name;
        this.inputMaterial = inputMaterial;
        this.outputMaterial = outputMaterial;
        this.workingRod = workingRod;
        this.bredRod = bredRod;
    }

    public TNCItemList workingRod() {
        return workingRod;
    }

    public TNCItemList bredRod() {
        return bredRod;
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
