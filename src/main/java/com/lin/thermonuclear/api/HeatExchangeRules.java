package com.lin.thermonuclear.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.lin.thermonuclear.Thermonuclear;

public final class HeatExchangeRules {

    private static final List<HeatExchangeRule> rules = new ArrayList<>();
    private static boolean frozen;

    private HeatExchangeRules() {}

    public static void register(HeatExchangeRule rule) {
        if (frozen) throw new IllegalStateException("Heat exchange registration is closed");
        Objects.requireNonNull(rule, "rule");
        Objects.requireNonNull(rule.coolant(), "coolant");
        Objects.requireNonNull(rule.steam(), "steam");
        CoolantType coolant = rule.coolant();
        if (rule.id() == null || rule.id()
            .isEmpty()
            || coolant.id() == null
            || coolant.id()
                .isEmpty()
            || !Double.isFinite(rule.steamPerHotCoolant())
            || rule.steamPerHotCoolant() <= 0) {
            throw new IllegalArgumentException("Invalid heat exchange rule: " + rule.id());
        }
        for (HeatExchangeRule existing : rules) {
            CoolantType existingCoolant = existing.coolant();
            if (existing.id()
                .equals(rule.id())
                || existingCoolant.id()
                    .equals(coolant.id())) {
                throw new IllegalArgumentException("Duplicate heat exchange rule or coolant: " + rule.id());
            }
        }
        rules.add(rule);
    }

    public static void freeze() {
        if (frozen) return;
        List<HeatExchangeRule> available = new ArrayList<>();
        for (HeatExchangeRule rule : rules) {
            CoolantType coolant = rule.coolant();
            if (coolant.hot() == null || coolant.cold() == null
                || rule.steam()
                    .fluid() == null) {
                Thermonuclear.LOG.error("Missing working fluid for heat exchange rule {}. Path disabled.", rule.id());
                continue;
            }
            for (HeatExchangeRule existing : available) {
                CoolantType existingCoolant = existing.coolant();
                if (existingCoolant.hot() == coolant.hot() || existingCoolant.cold() == coolant.cold()) {
                    throw new IllegalArgumentException("Coolant has multiple heat exchange rules: " + rule.id());
                }
            }
            available.add(rule);
        }
        rules.clear();
        rules.addAll(available);
        frozen = true;
    }

    public static List<HeatExchangeRule> getAllRules() {
        if (!frozen) throw new IllegalStateException("Heat exchange working fluids have not been resolved");
        return Collections.unmodifiableList(rules);
    }

    public static HeatExchangeRule find(String id) {
        for (HeatExchangeRule rule : getAllRules()) {
            if (rule.id()
                .equals(id)) return rule;
        }
        return null;
    }
}
