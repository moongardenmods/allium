package dev.hugeblank.allium.api;

import dev.hugeblank.allium.loader.Script;

/**
 * Functional Interface for libraries that depend on the script to function.
 * Passing the constructor of a class that extends <code>WrappedScriptLibrary</code> is the ideal use case.
 *
 * @see dev.hugeblank.allium.loader.EnvironmentManager#registerLibrary(LibraryInitializer)
 * @see WrappedScriptLibrary
  */
@FunctionalInterface
public interface LibraryInitializer {
    WrappedScriptLibrary init(Script script);
}
