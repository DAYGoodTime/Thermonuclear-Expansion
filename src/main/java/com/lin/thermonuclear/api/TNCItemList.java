package com.lin.thermonuclear.api;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.lin.thermonuclear.Thermonuclear;

import gregtech.api.util.GTRecipeBuilder;
import gregtech.api.util.GTUtility;

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
    NaquadahFuelRodMkI,
    NaquadahFuelRodMkII,
    NaquadahFuelRodMkIII,
    NaquadahFuelRodMkIV,
    NaquadahFuelRodMkV,
    NaquadahFuelRodMkVI,

    DepletedNaquadahFuelRodMkI,
    DepletedNaquadahFuelRodMkII,
    DepletedNaquadahFuelRodMkIII,
    DepletedNaquadahFuelRodMkIV,
    DepletedNaquadahFuelRodMkV,
    DepletedNaquadahFuelRodMkVI,

    CalciumToScandiumBreederRod,
    StrontiumToYttriumBreederRod,
    BariumToLanthanumBreederRod,
    LanthanumToCeriumBreederRod,
    CeriumToPraseodymiumBreederRod,
    PraseodymiumToNeodymiumBreederRod,
    NeodymiumToPromethiumBreederRod,
    PromethiumToSamariumBreederRod,
    SamariumToEuropiumBreederRod,
    EuropiumToGadoliniumBreederRod,
    GadoliniumToTerbiumBreederRod,
    TerbiumToDysprosiumBreederRod,
    DysprosiumToHolmiumBreederRod,
    HolmiumToErbiumBreederRod,
    ErbiumToThuliumBreederRod,
    ThuliumToYtterbiumBreederRod,
    YtterbiumToLutetiumBreederRod,

    BredCalciumToScandiumBreederRod,
    BredStrontiumToYttriumBreederRod,
    BredBariumToLanthanumBreederRod,
    BredLanthanumToCeriumBreederRod,
    BredCeriumToPraseodymiumBreederRod,
    BredPraseodymiumToNeodymiumBreederRod,
    BredNeodymiumToPromethiumBreederRod,
    BredPromethiumToSamariumBreederRod,
    BredSamariumToEuropiumBreederRod,
    BredEuropiumToGadoliniumBreederRod,
    BredGadoliniumToTerbiumBreederRod,
    BredTerbiumToDysprosiumBreederRod,
    BredDysprosiumToHolmiumBreederRod,
    BredHolmiumToErbiumBreederRod,
    BredErbiumToThuliumBreederRod,
    BredThuliumToYtterbiumBreederRod,
    BredYtterbiumToLutetiumBreederRod;

    private ItemStack template;

    public TNCItemList set(ItemStack stack) {
        if (GTUtility.isStackInvalid(stack)) {
            Thermonuclear.LOG.warn("Cannot bind an invalid item stack to TNCItemList.{}", name());
            return this;
        }
        template = GTUtility.copyAmountUnsafe(1, stack);
        return this;
    }

    public TNCItemList set(Item item) {
        return set(item == null ? null : new ItemStack(item));
    }

    public TNCItemList set(Block block) {
        return set(block == null ? null : new ItemStack(block));
    }

    public boolean hasBeenSet() {
        return template != null;
    }

    public ItemStack get(int amount) {
        return GTUtility.copyAmountUnsafe(amount, template);
    }

    public Item getItem() {
        return template.getItem();
    }

    public boolean isStackEqual(ItemStack stack) {
        return isStackEqual(stack, false, false);
    }

    public boolean isStackEqual(ItemStack stack, boolean wildcard, boolean ignoreNBT) {
        if (GTUtility.isStackInvalid(stack)) return false;
        ItemStack reference = get(1);
        if (wildcard) reference.setItemDamage(GTRecipeBuilder.WILDCARD);
        return GTUtility.areUnificationsEqual(stack, reference, ignoreNBT);
    }

}
