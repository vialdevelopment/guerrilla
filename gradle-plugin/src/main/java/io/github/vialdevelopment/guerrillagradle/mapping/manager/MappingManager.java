package io.github.vialdevelopment.guerrillagradle.mapping.manager;

import io.github.vialdevelopment.guerrillagradle.mapping.mapper.api.Mapper;
import io.github.vialdevelopment.guerrillagradle.mapping.mapper.impl.forge.ForgeGradle2;
import io.github.vialdevelopment.guerrillagradle.mapping.mapper.impl.forge.ForgeGradle3;
import io.github.vialdevelopment.guerrillagradle.mapping.mapper.impl.formats.Searge;
import io.github.vialdevelopment.guerrillagradle.mapping.mapper.impl.formats.TSearge;

import java.util.HashMap;
import java.util.Map;

/**
 * Mappings Types available are:
 *  - SEARGE
 *  - TSEARGE
 *  - FORGE_GRADLE_2
 *  - FORGE_GRADLE_3
 */
public class MappingManager {

    private final Map<String, Mapper> mappers = new HashMap<>();

    public MappingManager() {
        mappers.put("SEARGE", new Searge());
        mappers.put("TSEARGE", new TSearge());
        mappers.put("FORGE_GRADLE_2", new ForgeGradle2());
        mappers.put("FORGE_GRADLE_3", new ForgeGradle3());
    }

    public Mapper getMapper(String mapper) {
        return mappers.get(mapper);
    }

}
