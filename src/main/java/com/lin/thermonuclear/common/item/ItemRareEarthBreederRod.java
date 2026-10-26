package com.lin.thermonuclear.common.item;

import java.util.List;
import java.util.Map;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import com.lin.thermonuclear.common.nuclear.breeding.RareEarthBreedingRoute;

/** Each route has distinct working/completed items; vanilla damage stores breeding progress only. */
public final class ItemRareEarthBreederRod extends ItemBreederRod {

    // User-approved prototype target; not final balance.
    public static final int REQUIRED_PROGRESS = 10000;
    private final RareEarthBreedingRoute route;

    private ItemRareEarthBreederRod(RareEarthBreedingRoute route, boolean bred) {
        super(route.itemName(bred), bred, REQUIRED_PROGRESS);
        this.route = route;
    }

    public static Map<RareEarthBreedingRoute, ItemRareEarthBreederRod> registerVariants(boolean bred) {
        return registerVariants(RareEarthBreedingRoute.values(), route -> new ItemRareEarthBreederRod(route, bred));
    }

    public RareEarthBreedingRoute route() {
        return route;
    }

    @Override
    public void addInformation(ItemStack stack, EntityPlayer player, List<String> tooltip, boolean advanced) {
        if (isBred()) {
            tooltip.add(StatCollector.translateToLocal("thermonuclear.tooltip.bred_rare_earth_rod"));
        } else {
            tooltip.add(StatCollector.translateToLocal("thermonuclear.tooltip.rare_earth_breeder_rod.0"));
            tooltip.add(StatCollector.translateToLocal("thermonuclear.tooltip.rare_earth_breeder_rod.1"));
        }
        super.addInformation(stack, player, tooltip, advanced);
    }
}
