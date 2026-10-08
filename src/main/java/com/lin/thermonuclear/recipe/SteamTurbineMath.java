package com.lin.thermonuclear.recipe;

public final class SteamTurbineMath {

    private SteamTurbineMath() {}

    public static long parseLimit(String text, long fallback) {
        if (text == null || text.isEmpty() || text.length() > 19) return fallback;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) < '0' || text.charAt(i) > '9') return fallback;
        }
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    public static long steamLimitLitresPerCycle(long requestedLitresPerCycle, int steamLitresPerWaterLitre,
        int condensationRemainderLitres) {
        if (requestedLitresPerCycle <= 0 || steamLitresPerWaterLitre <= 0) return 0;
        // Forge represents each output FluidStack in int litres; inputs may span multiple stacks.
        long waterLimitLitresPerCycle = (long) Integer.MAX_VALUE * steamLitresPerWaterLitre
            - condensationRemainderLitres;
        return Math.min(requestedLitresPerCycle, waterLimitLitresPerCycle);
    }

    /** Accepts L/t, not a prepaid batch in litres; startup is applied later by the running tick. */
    public static double generationEUt(double steamLitresPerTick, double euPerLitre) {
        if (!Double.isFinite(steamLitresPerTick) || steamLitresPerTick <= 0
            || !Double.isFinite(euPerLitre)
            || euPerLitre <= 0) return 0;
        // Dynamo rating limits injection, not fuel consumption or generated power.
        return steamLitresPerTick * euPerLitre;
    }
}
