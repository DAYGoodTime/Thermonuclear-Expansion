package com.lin.thermonuclear.common.machine.base;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTSizeTracker;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.fluids.FluidStack;

import gregtech.api.util.GTUtility;

/** Display-only, bounded records. Native pending stacks remain the authoritative output inventory. */
public final class OutputDisplaySnapshot {

    public static final int MAX_ENTRY_BYTES = 4096;
    public static final int MAX_PAGE_BYTES = 16 * 1024;
    public static final int MAX_PAGE_ENTRIES = 16;
    public static final int PAGE_HEADER_BYTES = 28;
    public static final OutputDisplaySnapshot EMPTY = new OutputDisplaySnapshot(Collections.emptyList());

    private final List<byte[]> entries;
    private final List<List<byte[]>> pages = new ArrayList<>();

    private OutputDisplaySnapshot(List<byte[]> entries) {
        this.entries = entries;
        List<byte[]> page = new ArrayList<>();
        int bytes = PAGE_HEADER_BYTES;
        for (byte[] entry : entries) {
            if (page.size() == MAX_PAGE_ENTRIES || bytes + 4 + entry.length > MAX_PAGE_BYTES) {
                pages.add(Collections.unmodifiableList(page));
                page = new ArrayList<>();
                bytes = PAGE_HEADER_BYTES;
            }
            page.add(entry);
            bytes += 4 + entry.length;
        }
        pages.add(Collections.unmodifiableList(page));
    }

    public static OutputDisplaySnapshot capture(ItemStack[] items, FluidStack[] fluids) {
        Map<GTUtility.ItemId, Long> itemTotals = new LinkedHashMap<>();
        Map<GTUtility.FluidId, Long> fluidTotals = new LinkedHashMap<>();
        // Keys borrow NBT only during aggregation; the encoded records below own their data.
        if (items != null) for (ItemStack item : items) {
            if (item != null && item.stackSize > 0)
                itemTotals.merge(GTUtility.ItemId.createNoCopy(item), (long) item.stackSize, Math::addExact);
        }
        if (fluids != null) for (FluidStack fluid : fluids) {
            if (fluid != null && fluid.amount > 0) fluidTotals.merge(
                GTUtility.FluidId.createNoCopy(fluid.getFluid(), null, fluid.tag),
                (long) fluid.amount,
                Math::addExact);
        }
        List<byte[]> records = new ArrayList<>(itemTotals.size() + fluidTotals.size());
        itemTotals.forEach((id, amount) -> {
            NBTTagCompound record = new NBTTagCompound();
            record.setLong("amount", amount);
            record.setTag(
                "item",
                id.getItemStack()
                    .writeToNBT(new NBTTagCompound()));
            records.add(encode(record));
        });
        fluidTotals.forEach((id, amount) -> {
            NBTTagCompound record = new NBTTagCompound();
            record.setLong("amount", amount);
            record.setBoolean("fluidKind", true);
            record.setTag(
                "fluid",
                id.getFluidStack()
                    .writeToNBT(new NBTTagCompound()));
            records.add(encode(record));
        });
        return records.isEmpty() ? EMPTY : new OutputDisplaySnapshot(Collections.unmodifiableList(records));
    }

    private static byte[] encode(NBTTagCompound record) {
        try {
            return encodeBounded(record);
        } catch (IOException tooLarge) {
            // Preserve grouping and long totals, but never let one display tag defeat the packet budget.
            record.setBoolean("detailsOmitted", true);
            record.getCompoundTag("item")
                .removeTag("tag");
            record.getCompoundTag("fluid")
                .removeTag("Tag");
            try {
                return encodeBounded(record);
            } catch (IOException invalidTemplate) {
                record.removeTag("item");
                record.removeTag("fluid");
                try {
                    return encodeBounded(record);
                } catch (IOException impossible) {
                    throw new IllegalStateException("Cannot encode output display count", impossible);
                }
            }
        }
    }

    private static byte[] encodeBounded(NBTTagCompound record) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        OutputStream bounded = new OutputStream() {

            @Override
            public void write(int value) throws IOException {
                if (bytes.size() >= MAX_ENTRY_BYTES) throw new IOException("Output display tag exceeds byte budget");
                bytes.write(value);
            }

            @Override
            public void write(byte[] data, int offset, int length) throws IOException {
                if (length > MAX_ENTRY_BYTES - bytes.size())
                    throw new IOException("Output display tag exceeds byte budget");
                bytes.write(data, offset, length);
            }
        };
        CompressedStreamTools.write(record, new DataOutputStream(bounded));
        return bytes.toByteArray();
    }

    public static Entry decode(byte[] bytes) throws IOException {
        if (bytes.length == 0 || bytes.length > MAX_ENTRY_BYTES) throw new IOException("Invalid output display length");
        DataInputStream input = new DataInputStream(new ByteArrayInputStream(bytes));
        NBTTagCompound record = CompressedStreamTools.func_152456_a(input, new NBTSizeTracker(1024 * 1024));
        if (input.available() != 0 || record.getLong("amount") <= 0)
            throw new IOException("Invalid output display record");
        return new Entry(
            record.hasKey("item") ? ItemStack.loadItemStackFromNBT(record.getCompoundTag("item")) : null,
            record.hasKey("fluid") ? FluidStack.loadFluidStackFromNBT(record.getCompoundTag("fluid")) : null,
            record.getLong("amount"),
            record.getBoolean("fluidKind"),
            record.getBoolean("detailsOmitted"));
    }

    public int size() {
        return entries.size();
    }

    public int pageCount() {
        return pages.size();
    }

    public List<byte[]> page(int index) {
        return pages.get(index);
    }

    public void writeWailaPreview(NBTTagCompound tag) {
        NBTTagList preview = new NBTTagList();
        for (int i = 0; i < Math.min(3, entries.size()); i++) {
            NBTTagCompound entry = new NBTTagCompound();
            entry.setByteArray("data", entries.get(i));
            preview.appendTag(entry);
        }
        tag.setTag("tnOutputPreview", preview);
        tag.setInteger("tnOutputKinds", entries.size());
    }

    public static final class Entry {

        public final ItemStack item;
        public final FluidStack fluid;
        public final long amount;
        public final boolean fluidKind;
        public final boolean detailsOmitted;

        private Entry(ItemStack item, FluidStack fluid, long amount, boolean fluidKind, boolean detailsOmitted) {
            this.item = item;
            this.fluid = fluid;
            this.amount = amount;
            this.fluidKind = fluidKind;
            this.detailsOmitted = detailsOmitted;
        }
    }
}
