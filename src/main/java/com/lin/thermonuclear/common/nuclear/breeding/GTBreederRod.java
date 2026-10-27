package com.lin.thermonuclear.common.nuclear.breeding;

import net.minecraft.item.ItemStack;

import com.lin.thermonuclear.common.item.ItemRareEarthBreederRod;

import gregtech.api.enums.ItemList;
import gregtech.api.items.ItemBreederCell;

public final class GTBreederRod {

    private GTBreederRod() {}

    public static boolean accepts(ItemStack stack) {
        return stack != null && stack.getMaxDamage() > 0
            && (stack.getItem() instanceof ItemRareEarthBreederRod rod && !rod.isBred()
                || stack.getItem() instanceof ItemBreederCell
                    && (isRod(stack, ItemList.RodLithium) || isRod(stack, ItemList.RodGlowstone)));
    }

    private static boolean isRod(ItemStack stack, ItemList rod) {
        return rod.hasBeenSet() && stack.getItem() == rod.getItem();
    }

    public static ItemStack product(ItemStack stack) {
        if (!accepts(stack)) throw new IllegalArgumentException("Unsupported breeder rod");
        ItemStack product;
        if (stack.getItem() instanceof ItemRareEarthBreederRod rod) {
            product = rod.route()
                .bredRod()
                .get(1);
        } else {
            product = (isRod(stack, ItemList.RodLithium) ? ItemList.DepletedRodLithium : ItemList.DepletedRodGlowstone)
                .get(1);
        }
        // ItemList.get delegates to copyAmount, which silently caps requested counts at 64 in .133.
        product.stackSize = stack.stackSize;
        return product;
    }
}
