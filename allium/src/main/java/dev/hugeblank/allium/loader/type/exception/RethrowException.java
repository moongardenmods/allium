package dev.hugeblank.allium.loader.type.exception;

/// @see dev.hugeblank.allium.api.Rethrowable
public final class RethrowException extends RuntimeException {
    private final RuntimeException exception;
    public RethrowException(RuntimeException exception) {
        super(null, null, false, false); // Disable stack trace + suppression
        this.exception = exception;
    }

    public void rethrow() {
        throw this.exception;
    }
}