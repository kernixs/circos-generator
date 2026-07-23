package org.mpg.circos.assembly;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ClasspathAssemblyRepository implements AssemblyRepository {
    private final ObjectMapper mapper;

    public ClasspathAssemblyRepository() {
        this(new ObjectMapper());
    }

    public ClasspathAssemblyRepository(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public GenomeAssembly load(String assemblyId) {
        AssemblyId id = AssemblyId.from(assemblyId);
        String resource = "/genomes/" + id.canonical().toLowerCase() + ".chromosomes.json";
        try (InputStream input = ClasspathAssemblyRepository.class.getResourceAsStream(resource)) {
            if (input == null) throw new IllegalStateException("Missing assembly resource: " + resource);
            JsonNode root = mapper.readTree(input);
            validateIdentity(id, root, resource);
            List<Chromosome> chromosomes = new ArrayList<>();
            Set<String> names = new HashSet<>();
            for (JsonNode node : root.withArray("chromosomes")) {
                String name = node.path("name").asText();
                long length = node.path("length").asLong();
                if (name.isBlank() || length <= 0 || !names.add(name)) {
                    throw new IllegalStateException("Invalid chromosome entry in assembly resource: " + resource);
                }
                List<String> aliases = new ArrayList<>();
                node.withArray("aliases").forEach(alias -> aliases.add(alias.asText()));
                chromosomes.add(new Chromosome(name, length, aliases));
            }
            if (chromosomes.size() != 24) {
                throw new IllegalStateException("Expected 24 primary chromosomes in assembly resource: " + resource);
            }
            return new GenomeAssembly(id, chromosomes);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load assembly resource: " + resource, e);
        }
    }

    private void validateIdentity(AssemblyId id, JsonNode root, String resource) {
        if (!id.canonical().equals(root.path("assemblyId").asText())) {
            throw new IllegalStateException("Assembly identity does not match resource: " + resource);
        }
        if (id == AssemblyId.T2T_CHM13
                && (!"T2T-CHM13v2.0".equals(root.path("assemblyName").asText())
                || !"GCF_009914755.1".equals(root.path("refSeqAccession").asText())
                || !"hs1".equals(root.path("ucscName").asText()))) {
            throw new IllegalStateException("T2T assembly resource is not pinned to T2T-CHM13v2.0/hs1");
        }
    }
}
