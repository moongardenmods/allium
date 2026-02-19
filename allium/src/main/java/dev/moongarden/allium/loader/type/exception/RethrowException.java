package dev.moongarden.allium.loader.type.exception;

import dev.moongarden.allium.api.Rethrowable;

/// @see Rethrowable
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