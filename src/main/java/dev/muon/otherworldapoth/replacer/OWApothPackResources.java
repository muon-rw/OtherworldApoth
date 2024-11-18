package dev.muon.otherworldapoth.replacer;

import dev.muon.otherworldapoth.OtherworldApoth;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.AbstractPackResources;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.resources.IoSupplier;
import org.apache.commons.io.IOUtils;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

public class OWApothPackResources implements PackResources {
    private static final String PACK_META = """
        {
            "pack": {
                "description": "Otherworld Apotheosis Data Overrides",
                "pack_format": 15
            }
        }""";

    @Override
    @Nullable
    public IoSupplier<InputStream> getRootResource(String... elements) {
        if (elements.length == 1 && elements[0].equals("pack.mcmeta")) {
            return () -> IOUtils.toInputStream(PACK_META, StandardCharsets.UTF_8);
        }
        return null;
    }

    @Override
    public void listResources(PackType type, String namespace, String path, ResourceOutput resourceOutput) {
        try {
            URL url = OtherworldApoth.class.getResource("/data/" + namespace);
            if (url == null) return;

            Path rootPath = Paths.get(url.toURI());
            try (var stream = Files.walk(rootPath)) {
                stream.filter(Files::isRegularFile)
                        .forEach(filePath -> {
                            String relativePath = rootPath.relativize(filePath).toString()
                                    .replace('\\', '/');
                            ResourceLocation location = new ResourceLocation(
                                    namespace,
                                    relativePath.endsWith(".json")
                                            ? relativePath.substring(0, relativePath.length() - 5)
                                            : relativePath
                            );

                            resourceOutput.accept(location, () -> {
                                try {
                                    return Files.newInputStream(filePath);
                                } catch (IOException e) {
                                    OtherworldApoth.LOGGER.error("Failed to read resource: " + location, e);
                                    return null;
                                }
                            });
                        });
            }
        } catch (Exception e) {
            OtherworldApoth.LOGGER.error("Failed to list resources for namespace: " + namespace, e);
        }
    }

    @Override
    @Nullable
    public IoSupplier<InputStream> getResource(PackType type, ResourceLocation location) {
        if (location.getPath().equals("pack.mcmeta")) {
            return () -> IOUtils.toInputStream(PACK_META, StandardCharsets.UTF_8);
        }

        String path = String.format("/data/%s/%s", location.getNamespace(), location.getPath());
        if (OtherworldApoth.class.getResource(path) == null) {
            return null;
        }

        return () -> {
            try {
                return OtherworldApoth.class.getResourceAsStream(path);
            } catch (Exception e) {
                OtherworldApoth.LOGGER.error("Failed to get resource: " + location, e);
                return null;
            }
        };
    }

    @Override
    public Set<String> getNamespaces(PackType type) {
        return Set.of("apotheosis");
    }

    @Override
    @Nullable
    public <T> T getMetadataSection(MetadataSectionSerializer<T> deserializer) throws IOException {
        if (deserializer.getMetadataSectionName().equals("pack")) {
            try (InputStream stream = IOUtils.toInputStream(PACK_META, StandardCharsets.UTF_8)) {
                return AbstractPackResources.getMetadataFromStream(deserializer, stream);
            }
        }
        return null;
    }

    @Override
    public String packId() {
        return "otherworldapoth_overrides";
    }

    @Override
    public boolean isBuiltin() {
        return true;
    }

    @Override
    public void close() {
        // No resources to close
    }
}