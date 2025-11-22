package dev.hugeblank.allium.util;

import dev.hugeblank.allium.Allium;
import org.apache.logging.log4j.message.FormattedMessageFactory;
import org.apache.logging.log4j.message.Message;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicLong;

public class SimpleProfiler {
    private static final FormattedMessageFactory FACTORY = new FormattedMessageFactory();
    private final List<Entry> roots;
    private final Stack<Entry> stack;
    private final boolean enable;
    public SimpleProfiler(boolean enable) {
        this.stack = new Stack<>();
        this.roots = new ArrayList<>();
        this.enable = enable;
    }

    public void push(String... id) {
        if (Allium.DEVELOPMENT && enable) {
            Entry e = new Entry(
                    Arrays.stream(id).reduce("", (a, b) -> (!a.isEmpty()) ? a + ':' + b : a + b),
                    new AtomicLong(System.currentTimeMillis()),
                    new ArrayList<>()
            );
            stack.push(e);
        }
    }

    public void pop() {
        if (Allium.DEVELOPMENT && enable) {
            // Note: Sub-entries are reversed, that should probably be fixed.
            long stop = System.currentTimeMillis();
            Entry e = stack.pop();
            long duration = e.time().accumulateAndGet(stop, (a, b) -> b-a);
            if (duration > 0) {
                if (stack.isEmpty()) {
                    roots.add(e);
                } else {
                    stack.peek().children().add(e);
                }
            }
        };
    }

    public void print() {
        if (Allium.DEVELOPMENT && enable) {
            StringBuilder log = new StringBuilder("\n");
            printHelper(log, roots, 0);
            log.append(" = [total] ")
                    .append(roots.stream().map(e -> e.time().get()).reduce(0L, Long::sum))
                    .append("ms\n");

            Allium.LOGGER.info(log.toString());
            roots.clear();
            stack.clear();
        }
    }

    public void printHelper(StringBuilder log, List<Entry> entries, int depth) {
        for (Entry entry : entries) {
            Message msg = FACTORY.newMessage("{} - [{}] {}ms\n", (" ").repeat(depth*2), entry.id(), entry.time());
            log.append(msg.getFormattedMessage());
            printHelper(log, entry.children(), depth+1);
        }
    }

    public record Entry(String id, AtomicLong time, List<Entry> children) {
    }
}
