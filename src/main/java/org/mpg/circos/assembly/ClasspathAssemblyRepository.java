package org.mpg.circos.assembly;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

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
            List<Chromosome> chromosomes = new ArrayList<>();
            for (JsonNode node : root.withArray("chromosomes")) {
                List<String> aliases = new ArrayList<>();
                node.withArray("aliases").forEach(alias -> aliases.add(alias.asText()));
                chromosomes.add(new Chromosome(node.path("name").asText(), node.path("length").asLong(), aliases));
            }
            return new GenomeAssembly(id, chromosomes);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load assembly resource: " + resource, e);
        }
    }
}
