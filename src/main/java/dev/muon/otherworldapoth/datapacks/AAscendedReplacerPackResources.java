package dev.muon.otherworldapoth.datapacks;

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

public class AAscendedReplacerPackResources implements PackResources {
    private static final String PACK_META = """
        {
            "pack": {
                "description": "Otherworld Apoth Ascended Data Overrides",
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
        String specificPath = "/data/" + namespace + "/" + path;
        URL url = OtherworldApoth.class.getResource(specificPath);

        if (url == null && path.isEmpty()) {
            url = OtherworldApoth.class.getResource("/data/" + namespace);
        }

        if (url == null) {
            return;
        }

        try {
            Path rootPath = Paths.get(url.toURI());
            if (!Files.exists(rootPath)) return;

            Files.walk(rootPath)
                    .filter(Files::isRegularFile)
                    .forEach(filePath -> {
                        try {
                            String relativePath = rootPath.relativize(filePath).toString().replace('\\', '/');
                            String fullPath = path.isEmpty() ? relativePath : path + "/" + relativePath;

                            ResourceLocation location = new ResourceLocation(
                                    namespace,
                                    fullPath.endsWith(".json")
                                            ? fullPath.substring(0, fullPath.length() - 5)
                                            : fullPath
                            );

                            resourceOutput.accept(location, () -> Files.newInputStream(filePath));
                        } catch (Exception e) {
                            OtherworldApoth.LOGGER.error("Error processing file: " + filePath, e);
                        }
                    });
        } catch (Exception e) {
            OtherworldApoth.LOGGER.error("Error listing resources for " + url, e);
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
        return Set.of("apotheosis_ascended");
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
        return "ow_irons_apth";
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