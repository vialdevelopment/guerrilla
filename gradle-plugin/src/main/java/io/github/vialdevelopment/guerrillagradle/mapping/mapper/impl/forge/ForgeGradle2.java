package io.github.vialdevelopment.guerrillagradle.mapping.mapper.impl.forge;

import io.github.vialdevelopment.guerrillagradle.mapping.mapper.api.Mapper;
import io.github.vialdevelopment.guerrillagradle.mapping.mapper.api.name.ClassName;
import io.github.vialdevelopment.guerrillagradle.mapping.mapper.api.name.FieldName;
import io.github.vialdevelopment.guerrillagradle.mapping.mapper.api.name.MethodName;
import io.github.vialdevelopment.guerrillagradle.mapping.mapper.impl.formats.Searge;
import org.gradle.api.Project;

import java.io.File;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Objects;

/**
 * A mapper for forge gradle 2.x
 */
public class ForgeGradle2 extends Mapper {

    private final Mapper seargeMapper = new Searge();

    /**
     * Init
     * @param project gradle project
     * @param mappings mappings version e.g. "stable_39"
     */
    @Override
    public void init(Project project, String mappings) {
        String mcpType = "mcp_" + mappings.split("_")[0];
        String mcpVersionNumber = mappings.split("_")[1];
        for (File file : Objects.requireNonNull(Paths.get(project.getGradle().getGradleUserHomeDir().getPath(), "caches/minecraft/de/oceanlabs/mcp/", mcpType, mcpVersionNumber).toFile().listFiles())) {
            if (file.isDirectory()) {
                seargeMapper.init(project, file.getAbsolutePath() + "/srgs/mcp-notch.srg");
                break;
            }
        }
    }

    @Override
    public ClassName remapClassName(ClassName className) {
        return seargeMapper.remapClassName(className);
    }

    @Override
    public MethodName remapMethodName(MethodName methodName) {
        return seargeMapper.remapMethodName(methodName);
    }

    @Override
    public FieldName remapFieldName(FieldName fieldName) {
        return seargeMapper.remapFieldName(fieldName);
    }

    @Override
    public void loadFromCache(Project project) {
        seargeMapper.loadFromCache(project);
    }

    @Override
    public void writeToCache(Project project) {
        seargeMapper.writeToCache(project);
    }

    @Override
    public void setInheritanceMap(Map<String, String> inheritanceMap) {
        seargeMapper.setInheritanceMap(inheritanceMap);
    }

}
