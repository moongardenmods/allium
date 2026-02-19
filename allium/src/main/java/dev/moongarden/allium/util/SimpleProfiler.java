package dev.moongarden.allium.util;

import dev.moongarden.allium.Allium;
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
        }
    }

    public void print() {
        if (Allium.DEVELOPMENT && enable) {
            long total = roots.stream().map(e -> e.time().get()).reduce(0L, Long::sum);
            StringBuilder log = new StringBuilder("\n");
            printHelper(log, roots, total, 0);
            log.append(" = [total] ")
                    .append(total)
                    .append("ms\n");

            Allium.LOGGER.info(log.toString());
            roots.clear();
            stack.clear();
        }
    }

    public void printHelper(StringBuilder log, List<Entry> entries, long total, int depth) {
        for (Entry entry : entries) {
            long time = entry.time().get();
            Message msg = FACTORY.newMessage("{} - [{}] {}ms {}%\n", (" ").repeat(depth*2), entry.id(), time, Math.round((time/(float)total)*100));
            log.append(msg.getFormattedMessage());
            printHelper(log, entry.children(), total, depth+1);
        }
    }

    public record Entry(String id, AtomicLong time, List<Entry> children) {
    }
}
