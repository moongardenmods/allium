package dev.hugeblank.allium.loader.mixin;

public class AnnotationPair<T> {
    protected final String key;
    protected final T value;
    public AnnotationPair(String key, T value) {
        this.key = key;
        this.value = value;
    }
}
