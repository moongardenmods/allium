package dev.hugeblank.allium.api;

/**
 * Entrypoint interface for allium extensions.
 * Use this interface to provide additional libraries to scripts
 *
 * @see dev.hugeblank.allium.loader.EnvironmentManager#registerLibrary(WrappedLuaLibrary)
 * @see dev.hugeblank.allium.loader.EnvironmentManager#registerLibrary(LibraryInitializer)
  */
public interface AlliumExtension {
    void onInitialize();
}
