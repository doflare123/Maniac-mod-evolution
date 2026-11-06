package org.example.maniacrevolution.atmosphere.compat;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.extensibility.IRemapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * ForgeGradle does not rename Veil's shadow$SRG declarations when mapping dependencies.
 * Supply the dev SRG -> Mojang remapper to Mixin's shadow lookup, without modifying Veil.
 * No mappings file/property is supplied in a normal installed client/server.
 */
public final class VeilDevelopmentPlugin implements IMixinConfigPlugin {
    @Override
    public void onLoad(String mixinPackage) {
        String filename = System.getProperty("mixin.env.refMapRemappingFile");
        if (filename == null || !Files.isRegularFile(Path.of(filename))) return;
        Map<String, String> methods = new HashMap<>();
        Map<String, String> fields = new HashMap<>();
        try (var lines = Files.lines(Path.of(filename))) {
            lines.forEach(line -> {
                String[] parts = line.split(" ");
                if (parts.length >= 5 && parts[0].equals("MD:")) methods.put(name(parts[1]), name(parts[3]));
                if (parts.length >= 3 && parts[0].equals("FD:")) fields.put(name(parts[1]), name(parts[2]));
            });
        } catch (IOException error) {
            throw new IllegalStateException("Cannot read Veil development mappings", error);
        }
        MixinEnvironment.getDefaultEnvironment().getRemappers().add(new IRemapper() {
            @Override public String mapMethodName(String owner, String name, String desc) { return methods.getOrDefault(name, name); }
            @Override public String mapFieldName(String owner, String name, String desc) { return fields.getOrDefault(name, name); }
            @Override public String map(String name) { return name; }
            @Override public String unmap(String name) { return name; }
            @Override public String mapDesc(String desc) { return desc; }
            @Override public String unmapDesc(String desc) { return desc; }
        });
    }

    private static String name(String reference) {
        return reference.substring(reference.lastIndexOf('/') + 1);
    }

    @Override public String getRefMapperConfig() { return null; }
    @Override public boolean shouldApplyMixin(String target, String mixin) { return true; }
    @Override public void acceptTargets(Set<String> mine, Set<String> others) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
    @Override public void postApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
}
