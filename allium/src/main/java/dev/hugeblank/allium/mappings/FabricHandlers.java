package dev.hugeblank.allium.mappings;

import dev.hugeblank.allium.api.PlatformHandler;
import net.fabricmc.mappingio.adapter.MappingSourceNsSwitch;
import net.fabricmc.mappingio.tree.MemoryMappingTree;
import net.fabricmc.mappingio.tree.VisitableMappingTree;

import java.io.IOException;

// named of fabric and named of mojang overlap. we have the opportunity to change the name of proguard mappings,
// so we change them to mojnamed.

// If Neoforge

// reversed yarn (named -> intermediary), reversed intermediary (intermediary -> official), mojang (official -> mojnamed)
// mojang (dummy)
// reversed quilt (named -> intermediary), reversed intermediary (intermediary -> official), mojang (official -> mojnamed)

// If fabric
// reversed yarn (named -> intermediary)
// reversed mojang (mojnamed -> official), intermediary (official -> intermediary)
// reversed quilt (named -> intermediary)

public class FabricHandlers {

    static class SimpleReverseHandler extends PlatformHandler {
        private final String mapping;

        SimpleReverseHandler(String mapping) {
            this.mapping = mapping;
        }

        @Override
        protected VisitableMappingTree createMappingTree() throws IOException {
            VisitableMappingTree yarn = new MemoryMappingTree(true);
            PlatformMappings.getLoader(mapping).load(yarn);
            VisitableMappingTree reverseYarn = new MemoryMappingTree(true);
            yarn.accept(new MappingSourceNsSwitch(reverseYarn, "named"));
            return reverseYarn;
        }

        @Override
        public String getSourceNS() {
            return "named";
        }

        @Override
        public String getDestNS() {
            return "intermediary";
        }

        @Override
        public String getID() {
            return mapping;
        }
    }

    static class MojangHandler extends PlatformHandler {

        @Override
        protected VisitableMappingTree createMappingTree() throws IOException {
            VisitableMappingTree mojang = new MemoryMappingTree(true);
            PlatformMappings.getLoader("mojang").load(mojang);
            VisitableMappingTree intermediary = new MemoryMappingTree();
            PlatformMappings.getLoader("intermediary").load(intermediary);
            mojang.accept(intermediary);
            return mojang;
        }

        @Override
        public String getSourceNS() {
            return "mojnamed";
        }

        @Override
        public String getDestNS() {
            return "intermediary";
        }

        @Override
        public String getID() {
            return "mojang";
        }
    }

    public static void register() {
        PlatformMappings.registerHandler(new SimpleReverseHandler( "yarn"));
        PlatformMappings.registerHandler(new SimpleReverseHandler("quilt"));
        PlatformMappings.registerHandler(new MojangHandler());
    }
}
