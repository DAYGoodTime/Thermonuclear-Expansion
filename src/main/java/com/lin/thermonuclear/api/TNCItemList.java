package com.lin.thermonuclear.api;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.lin.thermonuclear.common.nuclear.breeding.RareEarthBreedingRoute;
import com.lin.thermonuclear.common.registry.NaquadahFuelTier;

public enum TNCItemList {

    NuclearPowerPlant,
    HeatExchangeStation,
    LargeSteamTurbine,
    BreederInputBus,
    BreederOutputBus,
    DebugStructureBuilder,
    ContainmentCasing,
    TurbineBlade,
    LowPressureTurbineShaft,
    HighPressureTurbineShaft,
    FuelRodTier1,
    FuelRodTier2,
    FuelRodTier3,
    FuelRodTier4,
    WirelessUpgradeChip,
    NaquadahFuelRodMkI(NaquadahFuelTier.MK_I, false),
    DepletedNaquadahFuelRodMkI(NaquadahFuelTier.MK_I, true),
    NaquadahFuelRodMkII(NaquadahFuelTier.MK_II, false),
    DepletedNaquadahFuelRodMkII(NaquadahFuelTier.MK_II, true),
    NaquadahFuelRodMkIII(NaquadahFuelTier.MK_III, false),
    DepletedNaquadahFuelRodMkIII(NaquadahFuelTier.MK_III, true),
    NaquadahFuelRodMkIV(NaquadahFuelTier.MK_IV, false),
    DepletedNaquadahFuelRodMkIV(NaquadahFuelTier.MK_IV, true),
    NaquadahFuelRodMkV(NaquadahFuelTier.MK_V, false),
    DepletedNaquadahFuelRodMkV(NaquadahFuelTier.MK_V, true),
    NaquadahFuelRodMkVI(NaquadahFuelTier.MK_VI, false),
    DepletedNaquadahFuelRodMkVI(NaquadahFuelTier.MK_VI, true),
    CalciumToScandiumBreederRod(RareEarthBreedingRoute.CALCIUM_TO_SCANDIUM, false),
    BredCalciumToScandiumBreederRod(RareEarthBreedingRoute.CALCIUM_TO_SCANDIUM, true),
    StrontiumToYttriumBreederRod(RareEarthBreedingRoute.STRONTIUM_TO_YTTRIUM, false),
    BredStrontiumToYttriumBreederRod(RareEarthBreedingRoute.STRONTIUM_TO_YTTRIUM, true),
    BariumToLanthanumBreederRod(RareEarthBreedingRoute.BARIUM_TO_LANTHANUM, false),
    BredBariumToLanthanumBreederRod(RareEarthBreedingRoute.BARIUM_TO_LANTHANUM, true),
    LanthanumToCeriumBreederRod(RareEarthBreedingRoute.LANTHANUM_TO_CERIUM, false),
    BredLanthanumToCeriumBreederRod(RareEarthBreedingRoute.LANTHANUM_TO_CERIUM, true),
    CeriumToPraseodymiumBreederRod(RareEarthBreedingRoute.CERIUM_TO_PRASEODYMIUM, false),
    BredCeriumToPraseodymiumBreederRod(RareEarthBreedingRoute.CERIUM_TO_PRASEODYMIUM, true),
    PraseodymiumToNeodymiumBreederRod(RareEarthBreedingRoute.PRASEODYMIUM_TO_NEODYMIUM, false),
    BredPraseodymiumToNeodymiumBreederRod(RareEarthBreedingRoute.PRASEODYMIUM_TO_NEODYMIUM, true),
    NeodymiumToPromethiumBreederRod(RareEarthBreedingRoute.NEODYMIUM_TO_PROMETHIUM, false),
    BredNeodymiumToPromethiumBreederRod(RareEarthBreedingRoute.NEODYMIUM_TO_PROMETHIUM, true),
    PromethiumToSamariumBreederRod(RareEarthBreedingRoute.PROMETHIUM_TO_SAMARIUM, false),
    BredPromethiumToSamariumBreederRod(RareEarthBreedingRoute.PROMETHIUM_TO_SAMARIUM, true),
    SamariumToEuropiumBreederRod(RareEarthBreedingRoute.SAMARIUM_TO_EUROPIUM, false),
    BredSamariumToEuropiumBreederRod(RareEarthBreedingRoute.SAMARIUM_TO_EUROPIUM, true),
    EuropiumToGadoliniumBreederRod(RareEarthBreedingRoute.EUROPIUM_TO_GADOLINIUM, false),
    BredEuropiumToGadoliniumBreederRod(RareEarthBreedingRoute.EUROPIUM_TO_GADOLINIUM, true),
    GadoliniumToTerbiumBreederRod(RareEarthBreedingRoute.GADOLINIUM_TO_TERBIUM, false),
    BredGadoliniumToTerbiumBreederRod(RareEarthBreedingRoute.GADOLINIUM_TO_TERBIUM, true),
    TerbiumToDysprosiumBreederRod(RareEarthBreedingRoute.TERBIUM_TO_DYSPROSIUM, false),
    BredTerbiumToDysprosiumBreederRod(RareEarthBreedingRoute.TERBIUM_TO_DYSPROSIUM, true),
    DysprosiumToHolmiumBreederRod(RareEarthBreedingRoute.DYSPROSIUM_TO_HOLMIUM, false),
    BredDysprosiumToHolmiumBreederRod(RareEarthBreedingRoute.DYSPROSIUM_TO_HOLMIUM, true),
    HolmiumToErbiumBreederRod(RareEarthBreedingRoute.HOLMIUM_TO_ERBIUM, false),
    BredHolmiumToErbiumBreederRod(RareEarthBreedingRoute.HOLMIUM_TO_ERBIUM, true),
    ErbiumToThuliumBreederRod(RareEarthBreedingRoute.ERBIUM_TO_THULIUM, false),
    BredErbiumToThuliumBreederRod(RareEarthBreedingRoute.ERBIUM_TO_THULIUM, true),
    ThuliumToYtterbiumBreederRod(RareEarthBreedingRoute.THULIUM_TO_YTTERBIUM, false),
    BredThuliumToYtterbiumBreederRod(RareEarthBreedingRoute.THULIUM_TO_YTTERBIUM, true),
    YtterbiumToLutetiumBreederRod(RareEarthBreedingRoute.YTTERBIUM_TO_LUTETIUM, false),
    BredYtterbiumToLutetiumBreederRod(RareEarthBreedingRoute.YTTERBIUM_TO_LUTETIUM, true);

    private final RareEarthBreedingRoute route;
    private final boolean bred;
    private final NaquadahFuelTier fuelTier;
    private final boolean depleted;
    private ItemStack template;

    TNCItemList() {
        this((RareEarthBreedingRoute) null, false);
    }

    TNCItemList(RareEarthBreedingRoute route, boolean bred) {
        this.route = route;
        this.bred = bred;
        this.fuelTier = null;
        this.depleted = false;
    }

    TNCItemList(NaquadahFuelTier fuelTier, boolean depleted) {
        this.route = null;
        this.bred = false;
        this.fuelTier = fuelTier;
        this.depleted = depleted;
    }

    public void set(ItemStack stack) {
        if (hasBeenSet()) throw new IllegalStateException("Thermonuclear item already bound: " + name());
        if (stack == null || stack.getItem() == null || stack.stackSize <= 0) {
            throw new IllegalArgumentException("Cannot bind an empty Thermonuclear item: " + name());
        }
        template = stack.copy();
        template.stackSize = 1;
    }

    public void set(Item item) {
        if (item == null) throw new IllegalArgumentException("Cannot bind a null item: " + name());
        set(new ItemStack(item));
    }

    public void set(Block block) {
        if (block == null) throw new IllegalArgumentException("Cannot bind a null block: " + name());
        set(new ItemStack(block));
    }

    public boolean hasBeenSet() {
        return template != null;
    }

    public ItemStack get(int amount) {
        if (!hasBeenSet()) throw new IllegalStateException("Thermonuclear item not yet bound: " + name());
        if (amount <= 0) throw new IllegalArgumentException("Item amount must be positive: " + amount);
        // Copy the entire template, including metadata and NBT; never expose the shared stack.
        ItemStack stack = template.copy();
        stack.stackSize = amount;
        return stack;
    }

    public Item getItem() {
        if (!hasBeenSet()) throw new IllegalStateException("Thermonuclear item not yet bound: " + name());
        return template.getItem();
    }

    public static TNCItemList rareEarthRod(RareEarthBreedingRoute route, boolean bred) {
        if (route == null) throw new IllegalArgumentException("Breeding route must not be null");
        for (TNCItemList entry : values()) {
            if (entry.route == route && entry.bred == bred) return entry;
        }
        throw new IllegalArgumentException("Unknown rare earth breeding route: " + route);
    }

    public static void validateAllBound() {
        for (TNCItemList entry : values()) {
            if (!entry.hasBeenSet()) {
                throw new IllegalStateException("Thermonuclear item not yet bound: " + entry.name());
            }
        }
    }

    public static TNCItemList naquadahFuelRod(NaquadahFuelTier tier, boolean depleted) {
        if (tier == null) throw new IllegalArgumentException("Fuel tier must not be null");
        for (TNCItemList entry : values()) {
            if (entry.fuelTier == tier && entry.depleted == depleted) return entry;
        }
        throw new IllegalArgumentException("Unknown naquadah fuel tier: " + tier);
    }
}
