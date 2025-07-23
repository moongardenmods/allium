package dev.hugeblank.allium.loader.type;

public final class RethrowException extends RuntimeException {
    public RethrowException(Throwable throwable) {
        super(null, null, false, false); // Disable stack trace + suppression
        rethrow(throwable);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void rethrow(Throwable throwable) throws T {
        throw (T) throwable;
    }
}