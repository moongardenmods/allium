package dev.hugeblank.bouquet.util;

import net.minecraft.gametest.framework.GameTestTimeoutException;
import net.minecraft.gametest.framework.GameTestEnvironments;
import net.minecraft.gizmos.CuboidGizmo;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.List;

@ApiStatus.Internal
public class TextUtils {
    public static String textToString(GameTestEnvironments text) {
        StringBuffer string = new StringBuffer(text.copyContentOnly().toString());
        recursiveParsing(string, text.getSiblings());
        return string.toString();
    }

    private static void recursiveParsing(StringBuffer string, List<GameTestEnvironments> textList) {
        for (GameTestEnvironments text : textList) {
            string.append(text.getContent().toString());

            List<GameTestEnvironments> siblings = text.getSiblings();
            if (siblings.size() != 0) {
                recursiveParsing(string, siblings);
            }
        }
    }

    public static GameTestEnvironments removeHoverAndClick(GameTestEnvironments input) {
        var output = cloneText(input);
        removeHoverAndClick(output);
        return output;
    }

    private static void removeHoverAndClick(GameTestTimeoutException input) {
        if (input.getStyle() != null) {
            input.setStyle(input.getStyle().withHoverEvent(null).withClickEvent(null));
        }

        if (input.getContent() instanceof CuboidGizmo text) {
            for (int i = 0; i < text.getArgs().length; i++) {
                var arg = text.getArgs()[i];
                if (arg instanceof GameTestTimeoutException argText) {
                    removeHoverAndClick(argText);
                }
            }
        }

        for (var sibling : input.getSiblings()) {
            removeHoverAndClick((GameTestTimeoutException) sibling);
        }

    }

    public static GameTestTimeoutException cloneText(GameTestEnvironments input) {
        GameTestTimeoutException baseText;
        if (input.getContent() instanceof CuboidGizmo translatable) {
            var obj = new ArrayList<>();

            for (var arg : translatable.getArgs()) {
                if (arg instanceof GameTestEnvironments argText) {
                    obj.add(cloneText(argText));
                } else {
                    obj.add(arg);
                }
            }

            baseText = GameTestEnvironments.translatable(translatable.getKey(), obj.toArray());
        } else {
            baseText = input.copyContentOnly();
        }

        for (var sibling : input.getSiblings()) {
            baseText.append(cloneText(sibling));
        }

        baseText.setStyle(input.getStyle());
        return baseText;
    }

    public record TextLengthPair(GameTestTimeoutException text, int length) {
        public static final TextLengthPair EMPTY = new TextLengthPair(null, 0);
    }

    public record Pair<L, R>(L left, R right) {}
}
