package com.lin.thermonuclear;

import java.io.File;
import java.lang.reflect.Field;

import net.minecraft.launchwrapper.Launch;
import net.minecraftforge.common.config.Configuration;

import com.gtnewhorizon.gtnhlib.config.Config.Comment;
import com.gtnewhorizon.gtnhlib.config.Config.DefaultDouble;
import com.gtnewhorizon.gtnhlib.config.Config.DefaultInt;
import com.gtnewhorizon.gtnhlib.config.Config.DefaultString;
import com.gtnewhorizon.gtnhlib.config.Config.Ignore;
import com.gtnewhorizon.gtnhlib.config.Config.LangKey;
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
@com.gtnewhorizon.gtnhlib.config.Config(modid = Thermonuclear.MODID, category = "")
@Comment("原型测试配置，不代表最终 GTNH 平衡。修改后需要重启游戏或服务器。")
@RequiresMcRestart
public class Config {

    // Source-only switch: never persist it in the configuration being reset, and never reset in an obfuscated game.
    @Ignore
    public static boolean useDevelopmentDefaults = true;

    @Comment("通用配置。")
    @LangKey("thermonuclear.config.general")
    public static final General general = new General();

    @Name("nuclear_power_plant")
    @Comment("核电站原型配置。")
    @LangKey("thermonuclear.config.nuclear_power_plant")
    public static final NuclearPowerPlant nuclearPowerPlant = new NuclearPowerPlant();

    @Name("heat_exchange_station")
    @Comment("热交换站旧参数兼容分组；当前固定换热公式不读取以下参数。")
    @LangKey("thermonuclear.config.heat_exchange_station")
    public static final HeatExchangeStation heatExchangeStation = new HeatExchangeStation();

    @Name("large_steam_turbine")
    @Comment("大型蒸汽涡轮机原型配置。")
    @LangKey("thermonuclear.config.large_steam_turbine")
    public static final LargeSteamTurbine largeSteamTurbine = new LargeSteamTurbine();

    public static class General {

        @Comment("启动时写入日志的问候语，不影响机器机制。")
        @DefaultString("核电 轻而易举呀")
        public String greeting;
    }

    public static class NuclearPowerPlant {

        @Comment("核电站直接发电效率，无量纲；最终输出还受直发倍率影响。")
        @DefaultDouble(1)
        @RangeDouble(min = 0.001, max = 1)
        public double nuclearDirectEfficiency;

        @Comment("核电站供热效率，无量纲；最终产热还受供热倍率影响。")
        @DefaultDouble(1)
        @RangeDouble(min = 0.001, max = 1)
        public double nuclearHeatEfficiency;

        @Comment("旧热缓存容量兼容参数，当前不读取；热量上限由燃料棒方块等级决定。")
        @DefaultInt(50000)
        @RangeInt(min = 1, max = Integer.MAX_VALUE)
        public int nuclearHeatCapacity;

        @Comment("停机且未完成流体冷却时的自然散热，单位：热量/秒。设为 0 关闭自然散热。")
        @DefaultDouble(100)
        @RangeDouble(min = 0, max = Integer.MAX_VALUE)
        public double nuclearPassiveCoolingPerSecond;

        @Comment("核燃料每秒消耗的寿命周期数；供热启动阶段还受启动进度影响。")
        @DefaultDouble(1)
        @RangeDouble(min = 0.001, max = 100)
        public double fuelCyclesPerSecond;

        @Comment("旧管道等级批量兼容参数，当前不读取；装载上限由燃料棒方块等级与电源面板并行设置决定。")
        @DefaultInt(256)
        @RangeInt(min = 1, max = 256)
        public int nuclearFuelRodsPerPipeTier;

        @Name("ic2CoolantLitresPerFuelHeatUnit")
        @Comment("核电站每单位缓存热量加热的 IC2 冷却液，单位：L/热量。")
        @DefaultDouble(1)
        @RangeDouble(min = 0.001, max = 1000)
        public double ic2CoolantPerHeat;

        @Name("superCoolantLitresPerFuelHeatUnit")
        @Comment("核电站每单位缓存热量加热的超级冷却液，单位：L/热量。")
        @DefaultDouble(0.25)
        @RangeDouble(min = 0.001, max = 1000)
        public double superCoolantPerHeat;

        @Comment("燃料代表堆栈数量大于 4 时追加的反射脉冲数，不是结构中反射板的数量。")
        @DefaultInt(4)
        @RangeInt(min = 0, max = 6)
        public int nuclearMaxReflectorCount;

        @Comment("核电站直接发电基准倍率，无量纲；另乘 IC2 核能配置与直发效率。")
        @DefaultDouble(2)
        @RangeDouble(min = 0.001, max = 100)
        public double nuclearDirectOutputMultiplier;

        @Comment("核电站理论燃料热值的供热倍率，无量纲；另乘供热效率。")
        @DefaultDouble(50)
        @RangeDouble(min = 0.001, max = 100)
        public double nuclearHeatOutputMultiplier;

        @Comment("核电站每单位缓存热量加热的蒸馏水，单位：L/热量；默认每升带走 5 点热量。")
        @DefaultDouble(0.2)
        @RangeDouble(min = 0.001, max = 1000)
        public double nuclearDistilledWaterPerHeat;

        @Comment("核电站每升蒸馏水产生的普通蒸汽，单位：L/L；不用于热交换站。")
        @DefaultDouble(160)
        @RangeDouble(min = 0.001, max = 1000000)
        public double nuclearSteamPerDistilledWater;

        @Comment("核电站供热启动进度从 0 升至 1 所需的 tick 数；20 tick = 1 秒，直发不使用启动倍率。")
        @DefaultInt(1200)
        @RangeInt(min = 1, max = 720000)
        public int nuclearStartupTicks;

        @Comment("核电站停机进度从 1 降至 0 所需的 tick 数；与热缓存散热独立。")
        @DefaultInt(600)
        @RangeInt(min = 1, max = 720000)
        public int nuclearDecayTicks;
    }

    public static class HeatExchangeStation {

    }

    public static class LargeSteamTurbine {

        @Comment("蒸汽涡轮启动进度从 0 升至 1 所需的 tick 数；20 tick = 1 秒。")
        @DefaultInt(600)
        @RangeInt(min = 1, max = 720000)
        public int turbineStartupTicks;

        @Comment("蒸汽涡轮停机进度从 1 降至 0 所需的 tick 数；20 tick = 1 秒。")
        @DefaultInt(300)
        @RangeInt(min = 1, max = 720000)
        public int turbineDecayTicks;
    }

    /** Migrates saved categories before GTNHLib loads fields or removes undeclared categories. */
    public static void synchronizeConfiguration(File configFile) {
        boolean resetDefaults = useDevelopmentDefaults
            && Boolean.TRUE.equals(Launch.blackboard.get("fml.deobfuscatedEnvironment"));
        migrateLegacyConfiguration(configFile);
        ConfigurationManager.registerConfig(Config.class);
        Configuration configuration = ConfigurationManager.getConfig(Config.class);
        applyDefaultsAndValidate(Config.class, null, configuration, "", resetDefaults);
        configuration.save();
        if (resetDefaults) {
            Thermonuclear.LOG.info("Development configuration: saved values replaced with annotated defaults.");
        }
    }

    private static void migrateLegacyConfiguration(File configFile) {
        if (!configFile.isFile()) return;
        Configuration configuration = new Configuration(configFile);
        configuration.load();
        if (!configuration.hasCategory("prototype")) return;
        for (Field group : Config.class.getDeclaredFields()) {
            if (group.isSynthetic() || group.isAnnotationPresent(Ignore.class)) continue;
            String category = ConfigFieldParser.getFieldName(group);
            for (Field field : group.getType()
                .getDeclaredFields()) {
                if (!ConfigFieldParser.canParse(field)) continue;
                String key = ConfigFieldParser.getFieldName(field);
                // A value explicitly saved in the new group always wins over a legacy copy.
                if (!configuration.hasKey(category, key)) {
                    configuration.moveProperty("prototype", key, category);
                }
            }
        }
        configuration.removeCategory(configuration.getCategory("prototype"));
        configuration.save();
    }

    private static void applyDefaultsAndValidate(Class<?> configClass, Object instance, Configuration configuration,
        String category, boolean resetDefaults) {
        for (Field field : configClass.getDeclaredFields()) {
            if (field.isSynthetic() || field.isAnnotationPresent(Ignore.class)) {
                continue;
            }
            String key = ConfigFieldParser.getFieldName(field);
            try {
                if (!ConfigFieldParser.canParse(field)) {
                    String childCategory = category.isEmpty() ? key : category + Configuration.CATEGORY_SPLITTER + key;
                    applyDefaultsAndValidate(
                        field.getType(),
                        field.get(instance),
                        configuration,
                        childCategory,
                        resetDefaults);
                    continue;
                }
                if (resetDefaults) {
                    configuration.getCategory(category)
                        .get(key)
                        .setToDefault();
                    ConfigFieldParser.loadField(instance, field, configuration, category, key);
                }
                // GTNHLib 0.11.51 exposes double ranges as metadata but does not clamp values or reject NaN/infinity.
                if (field.getType() == double.class) {
                    double value = field.getDouble(instance);
                    if (!Double.isFinite(value)) value = field.getAnnotation(DefaultDouble.class)
                        .value();
                    RangeDouble range = field.getAnnotation(RangeDouble.class);
                    field.setDouble(instance, Math.max(range.min(), Math.min(range.max(), value)));
                }
                ConfigFieldParser.saveField(instance, field, configuration, category);
            } catch (IllegalAccessException e) {
                throw new ConfigException(e);
            }
        }
    }
}
