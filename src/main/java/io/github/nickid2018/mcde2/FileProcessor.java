package io.github.nickid2018.mcde2;

import net.fabricmc.loom.api.mappings.layered.MappingsNamespace;
import net.fabricmc.loom.configuration.providers.mappings.LayeredMappingsProcessor;
import net.fabricmc.loom.configuration.providers.mappings.mojmap.MojangMappingLayer;
import net.fabricmc.loom.configuration.providers.mappings.utils.AddConstructorMappingVisitor;
import net.fabricmc.mappingio.MappingWriter;
import net.fabricmc.mappingio.adapter.MappingDstNsReorder;
import net.fabricmc.mappingio.adapter.MappingSourceNsSwitch;
import net.fabricmc.mappingio.format.tiny.Tiny2FileWriter;
import net.fabricmc.mappingio.tree.MemoryMappingTree;
import net.fabricmc.tinyremapper.NonClassCopyMode;
import net.fabricmc.tinyremapper.OutputConsumerPath;
import net.fabricmc.tinyremapper.TinyRemapper;
import net.fabricmc.tinyremapper.TinyUtils;
import org.gradle.api.logging.TemplateLogger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Pattern;

public class FileProcessor {
    private static final Pattern MC_LV_PATTERN = Pattern.compile("\\$\\$\\d+");

    public static void process(Path input, Path output, Path clientMappings, Path serverMappings) throws Exception {
        Path mappings = Paths.get("mappings.tiny");
        generateMappings(mappings, clientMappings, serverMappings);
        remap(input, output, mappings, "official", "named");
    }

    public static void generateMappings(Path output, Path clientMappings, Path serverMappings) throws IOException {
        MojangMappingLayer mojangMappingLayer = new MojangMappingLayer(
            clientMappings,
            serverMappings,
            false,
            false,
            null,
            new TemplateLogger()
        );

        LayeredMappingsProcessor processor = new LayeredMappingsProcessor(null, true);
        MemoryMappingTree mappings = processor.getMappings(List.of(mojangMappingLayer));

        MappingWriter mapWriter = new Tiny2FileWriter(Files.newBufferedWriter(output), false);
        MappingDstNsReorder nsReorder = new MappingDstNsReorder(mapWriter, List.of(MappingsNamespace.NAMED.toString()));
        MappingSourceNsSwitch nsSwitch = new MappingSourceNsSwitch(nsReorder, MappingsNamespace.OFFICIAL.toString(), true);
        AddConstructorMappingVisitor addConstructor = new AddConstructorMappingVisitor(nsSwitch);
        mappings.accept(addConstructor);
    }

    public static void remap(Path input, Path output, Path tinyMapping, String from, String to) {
        TinyRemapper remapper = TinyRemapper.newRemapper()
            .withMappings(TinyUtils.createTinyMappingProvider(tinyMapping, from, to))
            .renameInvalidLocals(true)
            .rebuildSourceFilenames(true)
            .invalidLvNamePattern(MC_LV_PATTERN)
            .inferNameFromSameLvIndex(true)
            .build();

        try (OutputConsumerPath outputConsumer = new OutputConsumerPath.Builder(output).build()) {
            outputConsumer.addNonClassFiles(input, NonClassCopyMode.FIX_META_INF, remapper);

            remapper.readInputs(input);
            remapper.readClassPath(input);

            remapper.apply(outputConsumer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            remapper.finish();
        }
    }
}
