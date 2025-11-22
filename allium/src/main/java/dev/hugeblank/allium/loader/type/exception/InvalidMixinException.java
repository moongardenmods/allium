package dev.hugeblank.allium.loader.type.exception;

public class InvalidMixinException extends Exception {
    public InvalidMixinException(Type type, String message) {
        super(switch (type) {
            case NO_INJECTOR_ANNOTATION -> "Missing injector annotation for method with event id '" + message + "'";
            case TOO_MANY_INJECTOR_ANNOTATIONS -> "More than one injector annotation found for method with event id '" + message + "'";
            case INVALID_DESCRIPTOR -> "Could not find method matching descriptor: "+message;
            case INVALID_CLASSTYPE -> "Attempt to use "+message+" method on non-"+message+" mixin.";
        });

    }

    public enum Type {
        NO_INJECTOR_ANNOTATION,
        TOO_MANY_INJECTOR_ANNOTATIONS,
        INVALID_DESCRIPTOR,
        INVALID_CLASSTYPE,
    }
}
