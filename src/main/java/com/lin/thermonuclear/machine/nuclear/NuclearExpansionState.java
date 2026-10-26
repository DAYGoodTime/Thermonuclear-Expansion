package com.lin.thermonuclear.machine.nuclear;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import net.minecraft.nbt.NBTTagCompound;

import com.lin.thermonuclear.machine.nuclear.NuclearModuleStructure.Position;

/** Persistent comparison history is deliberately distinct from this session's validated structure. */
final class NuclearExpansionState {

    private enum Validation {
        CHECKING,
        STRUCTURE,
        TIMED_OUT,
        VALID,
        CAPPED
    }

    private int lastKnownLevel;
    private Validation validation = Validation.CHECKING;
    private Set<Position> modules = Collections.emptySet();

    boolean validated() {
        return validation == Validation.VALID || validation == Validation.CAPPED;
    }

    int batchLimit() {
        return validated() ? lastKnownLevel : 0;
    }

    Set<Position> modules() {
        return modules;
    }

    void invalidate() {
        validation = Validation.STRUCTURE;
        modules = Collections.emptySet();
    }

    void timeOut() {
        invalidate();
        validation = Validation.TIMED_OUT;
    }

    boolean accept(NuclearModuleSearch.Result scan) {
        if (scan.timedOut) throw new IllegalArgumentException("Cannot accept an incomplete expansion scan");
        int next = 1 + scan.modules;
        boolean changed = lastKnownLevel > 0 && lastKnownLevel != next;
        modules = Collections.unmodifiableSet(new HashSet<>(scan.valid));
        lastKnownLevel = next;
        validation = scan.capped ? Validation.CAPPED : Validation.VALID;
        return changed;
    }

    void save(NBTTagCompound nbt) {
        nbt.setInteger("tnExpansionLevel", lastKnownLevel > 0 ? lastKnownLevel : 1);
        nbt.setBoolean("tnExpansionKnown", lastKnownLevel > 0);
    }

    void load(NBTTagCompound nbt) {
        int level = nbt.getInteger("tnExpansionLevel");
        lastKnownLevel = nbt.getBoolean("tnExpansionKnown") && level >= 1
            && level <= 1 + NuclearModuleStructure.MAX_MODULES ? level : 0;
        validation = Validation.CHECKING;
        modules = Collections.emptySet();
    }
}
