package com.lin.thermonuclear.client.gui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

import net.minecraft.network.PacketBuffer;

import com.cleanroommc.modularui.value.sync.SyncHandler;
import com.lin.thermonuclear.common.machine.base.OutputDisplaySnapshot;

/** Sends one atomic, byte-bounded page; C2S carries only a revision and a page number. */
final class OutputDisplaySyncHandler extends SyncHandler<OutputDisplaySyncHandler> {

    private static final int PAGE = 0;
    private static final int REQUEST_PAGE = 1;
    private final Supplier<OutputDisplaySnapshot> source;
    private final IntSupplier durationSource;
    private OutputDisplaySnapshot snapshot;
    private long revision;
    private int page;
    private int pageCount = 1;
    private int duration;
    private int total;
    private List<OutputDisplaySnapshot.Entry> entries = Collections.emptyList();

    OutputDisplaySyncHandler(Supplier<OutputDisplaySnapshot> source, IntSupplier durationSource) {
        this.source = source;
        this.durationSource = durationSource;
        allowC2S();
    }

    @Override
    public void detectAndSendChanges(boolean init) {
        OutputDisplaySnapshot current = source.get();
        int currentDuration = durationSource.getAsInt();
        if (init || snapshot != current || duration != currentDuration) {
            snapshot = current;
            duration = currentDuration;
            revision++;
            pageCount = snapshot.pageCount();
            total = snapshot.size();
            page = Math.min(page, pageCount - 1);
            sendPage();
        }
    }

    private void sendPage() {
        syncToClient(PAGE, buffer -> {
            buffer.writeLong(revision);
            buffer.writeInt(page);
            buffer.writeInt(pageCount);
            buffer.writeInt(total);
            buffer.writeInt(duration);
            List<byte[]> records = snapshot.page(page);
            buffer.writeInt(records.size());
            for (byte[] record : records) {
                buffer.writeInt(record.length);
                buffer.writeBytes(record);
            }
        });
    }

    @Override
    public void readOnClient(int id, PacketBuffer buffer) throws IOException {
        if (id != PAGE || buffer.readableBytes() < OutputDisplaySnapshot.PAGE_HEADER_BYTES
            || buffer.readableBytes() > OutputDisplaySnapshot.MAX_PAGE_BYTES)
            throw new IOException("Invalid output display page");
        long nextRevision = buffer.readLong();
        int nextPage = buffer.readInt();
        int nextPageCount = buffer.readInt();
        int nextTotal = buffer.readInt();
        int nextDuration = buffer.readInt();
        int count = buffer.readInt();
        if (nextPage < 0 || nextPageCount <= nextPage
            || nextTotal < 0
            || nextDuration < 0
            || count < 0
            || count > OutputDisplaySnapshot.MAX_PAGE_ENTRIES
            || count > nextTotal) throw new IOException("Invalid output display page metadata");
        List<OutputDisplaySnapshot.Entry> next = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            int length = buffer.readInt();
            if (length <= 0 || length > OutputDisplaySnapshot.MAX_ENTRY_BYTES || length > buffer.readableBytes())
                throw new IOException("Invalid output display record length");
            byte[] record = new byte[length];
            buffer.readBytes(record);
            next.add(OutputDisplaySnapshot.decode(record));
        }
        if (buffer.isReadable()) throw new IOException("Trailing output display bytes");
        if (nextRevision < revision) return;
        revision = nextRevision;
        page = nextPage;
        pageCount = nextPageCount;
        duration = nextDuration;
        total = nextTotal;
        entries = Collections.unmodifiableList(next);
    }

    @Override
    public void readOnServer(int id, PacketBuffer buffer) {
        // Never deserialize client-supplied products, even though pagination needs C2S enabled.
        if (id != REQUEST_PAGE || buffer.readableBytes() != 12) return;
        long requestedRevision = buffer.readLong();
        int requestedPage = buffer.readInt();
        if (snapshot == null || requestedRevision != revision || requestedPage < 0 || requestedPage >= pageCount)
            return;
        page = requestedPage;
        sendPage();
    }

    void turnPage(int delta) {
        int requested = page + delta;
        if (!getSyncManager().isClient() || requested < 0 || requested >= pageCount) return;
        syncToServer(REQUEST_PAGE, buffer -> {
            buffer.writeLong(revision);
            buffer.writeInt(requested);
        });
    }

    List<OutputDisplaySnapshot.Entry> entries() {
        return entries;
    }

    int page() {
        return page;
    }

    int pageCount() {
        return pageCount;
    }

    int duration() {
        return duration;
    }

    int total() {
        return total;
    }

}
