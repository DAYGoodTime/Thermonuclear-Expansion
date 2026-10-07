package com.lin.thermonuclear;

import java.lang.reflect.Field;

import net.minecraft.launchwrapper.Launch;
import net.minecraftforge.common.config.Configuration;

import com.gtnewhorizon.gtnhlib.config.Config.Comment;
import com.gtnewhorizon.gtnhlib.config.Config.DefaultDouble;
import com.gtnewhorizon.gtnhlib.config.Config.DefaultInt;
import com.gtnewhorizon.gtnhlib.config.Config.DefaultString;
import com.gtnewhorizon.gtnhlib.config.Config.Ignore;
import com.gtnewhorizon.gtnhlib.config.Config.Name;
import com.gtnewhorizon.gtnhlib.config.Config.RangeDouble;
import com.gtnewhorizon.gtnhlib.config.Config.RangeInt;
import com.gtnewhorizon.gtnhlib.config.Config.RequiresMcRestart;
import com.gtnewhorizon.gtnhlib.config.ConfigException;
import com.gtnewhorizon.gtnhlib.config.ConfigFieldParser;
import com.gtnewhorizon.gtnhlib.config.ConfigurationManager;

/**
 * Loads annotated defaults before machine registration. Keep defaults in annotations only so development resets and
 * normal configuration loading cannot drift apart.
 */
@com.gtnewhorizon.gtnhlib.config.Config(modid = Thermonuclear.MODID, category = "prototype")
@Comment("原型测试配置，不代表最终 GTNH 平衡。修改后需要重启游戏或服务器。")
@RequiresMcRestart
public class Config {

    // Source-only switch: never persist it in the configuration being reset, and never reset in an obfuscated game.
    @Ignore
    public static boolean useDevelopmentDefaults = true;

    @Comment("核电站直接发电效率，无量纲；最终输出还受直发倍率影响。")
    @DefaultDouble(1)
    @RangeDouble(min = 0.001, max = 1)
    public static double nuclearDirectEfficiency;

    @Comment("核电站供热效率，无量纲；最终产热还受供热倍率影响。")
    @DefaultDouble(1)
    @RangeDouble(min = 0.001, max = 1)
    public static double nuclearHeatEfficiency;

    @Comment("核电站热缓存容量，单位：热量。计划产热超过容量时关机。")
    @DefaultInt(50000)
    @RangeInt(min = 1, max = Integer.MAX_VALUE)
    public static int nuclearHeatCapacity;

    @Comment("停机且未完成流体冷却时的自然散热，单位：热量/秒。设为 0 关闭自然散热。")
    @DefaultDouble(100)
    @RangeDouble(min = 0, max = Integer.MAX_VALUE)
    public static double nuclearPassiveCoolingPerSecond;

    @Comment("核燃料每秒消耗的寿命周期数；供热启动阶段还受启动进度影响。")
    @DefaultDouble(1)
    @RangeDouble(min = 0.001, max = 100)
    public static double fuelCyclesPerSecond;

    @Comment("每级流体管道外壳允许装载的燃料棒数量，单位：根/等级。")
    @DefaultInt(256)
    @RangeInt(min = 1, max = 256)
    public static int nuclearFuelRodsPerPipeTier;

    @Name("ic2CoolantLitresPerFuelHeatUnit")
    @Comment("核电站每单位缓存热量加热的 IC2 冷却液，单位：L/热量。")
    @DefaultDouble(1)
    @RangeDouble(min = 0.001, max = 1000)
    public static double ic2CoolantPerHeat;

    @Name("superCoolantLitresPerFuelHeatUnit")
    @Comment("核电站每单位缓存热量加热的超级冷却液，单位：L/热量。")
    @DefaultDouble(0.25)
    @RangeDouble(min = 0.001, max = 1000)
    public static double superCoolantPerHeat;

    @Comment("燃料代表堆栈数量大于 4 时追加的反射脉冲数，不是结构中反射板的数量。")
    @DefaultInt(4)
    @RangeInt(min = 0, max = 6)
    public static int nuclearMaxReflectorCount;

    @Comment("核电站直接发电基准倍率，无量纲；另乘 IC2 核能配置与直发效率。")
    @DefaultDouble(2)
    @RangeDouble(min = 0.001, max = 100)
    public static double nuclearDirectOutputMultiplier;

    @Comment("核电站理论燃料热值的供热倍率，无量纲；另乘供热效率。")
    @DefaultDouble(50)
    @RangeDouble(min = 0.001, max = 100)
    public static double nuclearHeatOutputMultiplier;

    @Comment("核电站每单位缓存热量加热的蒸馏水，单位：L/热量。")
    @DefaultDouble(1)
    @RangeDouble(min = 0.001, max = 1000)
    public static double nuclearDistilledWaterPerHeat;

    @Comment("核电站每升蒸馏水产生的普通蒸汽，单位：L/L；不用于热交换站。")
    @DefaultDouble(10)
    @RangeDouble(min = 0.001, max = 1000000)
    public static double nuclearSteamPerDistilledWater;

    @Name("ic2HotCoolantLitresPerWater")
    @Comment("热交换站每消耗 1 L 蒸馏水所需的热 IC2 冷却液，单位：L。")
    @DefaultInt(1)
    @RangeInt(min = 1, max = 1000000)
    public static int ic2HotCoolantPerWater;

    @Name("hotSuperCoolantLitresPerWater")
    @Comment("热交换站每消耗 1 L 蒸馏水所需的热超级冷却液，单位：L。")
    @DefaultInt(1)
    @RangeInt(min = 1, max = 1000000)
    public static int hotSuperCoolantPerWater;

    @Name("ordinarySteamLitresPerHotCoolant")
    @Comment("热交换站每升实际消耗的热冷却液产生的普通蒸汽，单位：L/L。")
    @DefaultDouble(160)
    @RangeDouble(min = 0.001, max = 1000000)
    public static double ordinarySteamPerHotCoolant;

    @Name("superheatedSteamLitresPerHotCoolant")
    @Comment("热交换站每升实际消耗的热冷却液产生的过热蒸汽，单位：L/L。")
    @DefaultDouble(160)
    @RangeDouble(min = 0.001, max = 1000000)
    public static double superheatedSteamPerHotCoolant;

    @Name("supercriticalSteamLitresPerHotCoolant")
    @Comment("热交换站每升实际消耗的热冷却液产生的超临界蒸汽，单位：L/L。")
    @DefaultDouble(160)
    @RangeDouble(min = 0.001, max = 1000000)
    public static double supercriticalSteamPerHotCoolant;

    @Name("ultraSupercriticalSteamLitresPerHotCoolant")
    @Comment("热交换站每升实际消耗的热冷却液产生的超超临界蒸汽，单位：L/L。")
    @DefaultDouble(160)
    @RangeDouble(min = 0.001, max = 1000000)
    public static double ultraSupercriticalSteamPerHotCoolant;

    @Comment("GT 转子蒸汽效率倍率，无量纲；倍率计算后的实际效率封顶为 1。")
    @DefaultDouble(1.5)
    @RangeDouble(min = 1, max = 100)
    public static double rotorEfficiencyMultiplier;

    @Comment("GT 转子最佳蒸汽流量倍率，无量纲。")
    @DefaultDouble(2)
    @RangeDouble(min = 1, max = 100)
    public static double rotorCapacityMultiplier;

    @Comment("核电站供热启动进度从 0 升至 1 所需的 tick 数；20 tick = 1 秒，直发不使用启动倍率。")
    @DefaultInt(1200)
    @RangeInt(min = 1, max = 720000)
    public static int nuclearStartupTicks;

    @Comment("核电站停机进度从 1 降至 0 所需的 tick 数；与热缓存散热独立。")
    @DefaultInt(600)
    @RangeInt(min = 1, max = 720000)
    public static int nuclearDecayTicks;

    @Comment("蒸汽涡轮启动进度从 0 升至 1 所需的 tick 数；20 tick = 1 秒。")
    @DefaultInt(600)
    @RangeInt(min = 1, max = 720000)
    public static int turbineStartupTicks;

    @Comment("蒸汽涡轮停机进度从 1 降至 0 所需的 tick 数；20 tick = 1 秒。")
    @DefaultInt(300)
    @RangeInt(min = 1, max = 720000)
    public static int turbineDecayTicks;

    @com.gtnewhorizon.gtnhlib.config.Config(modid = Thermonuclear.MODID, category = "general")
    @Comment("通用配置。")
    @RequiresMcRestart
    public static class General {

        @Comment("启动时写入日志的问候语，不影响机器机制。")
        @DefaultString("Hello World")
        public static String greeting;
    }

    /** Loads both legacy categories before any machine may read their otherwise uninitialized fields. */
    public static void synchronizeConfiguration() {
        boolean resetDefaults = useDevelopmentDefaults
            && Boolean.TRUE.equals(Launch.blackboard.get("fml.deobfuscatedEnvironment"));
        ConfigurationManager.registerConfig(General.class);
        ConfigurationManager.registerConfig(Config.class);
        applyDefaultsAndValidate(General.class, resetDefaults);
        applyDefaultsAndValidate(Config.class, resetDefaults);
        ConfigurationManager.getConfig(Config.class)
            .save();
        if (resetDefaults) {
            Thermonuclear.LOG.info("Development configuration: saved values replaced with annotated defaults.");
        }
    }

    private static void applyDefaultsAndValidate(Class<?> configClass, boolean resetDefaults) {
        Configuration configuration = ConfigurationManager.getConfig(configClass);
        String category = configClass.getAnnotation(com.gtnewhorizon.gtnhlib.config.Config.class)
            .category();
        for (Field field : configClass.getDeclaredFields()) {
            if (field.isSynthetic() || field.isAnnotationPresent(Ignore.class) || !ConfigFieldParser.canParse(field)) {
                continue;
            }
            String key = ConfigFieldParser.getFieldName(field);
            if (resetDefaults) {
                configuration.getCategory(category)
                    .get(key)
                    .setToDefault();
                ConfigFieldParser.loadField(null, field, configuration, category, key);
            }
            try {
                // GTNHLib 0.11.51 exposes double ranges as metadata but does not clamp values or reject NaN/infinity.
                if (field.getType() == double.class) {
                    double value = field.getDouble(null);
                    if (!Double.isFinite(value)) value = field.getAnnotation(DefaultDouble.class)
                        .value();
                    RangeDouble range = field.getAnnotation(RangeDouble.class);
                    field.setDouble(null, Math.max(range.min(), Math.min(range.max(), value)));
                }
                ConfigFieldParser.saveField(null, field, configuration, category);
            } catch (IllegalAccessException e) {
                throw new ConfigException(e);
            }
        }
    }
}
