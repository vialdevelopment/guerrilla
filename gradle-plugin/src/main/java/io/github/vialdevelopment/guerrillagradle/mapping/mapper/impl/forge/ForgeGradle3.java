package io.github.vialdevelopment.guerrillagradle.mapping.mapper.impl.forge;

import io.github.vialdevelopment.guerrillagradle.mapping.mapper.api.Mapper;
import io.github.vialdevelopment.guerrillagradle.mapping.mapper.api.name.ClassName;
import io.github.vialdevelopment.guerrillagradle.mapping.mapper.api.name.FieldName;
import io.github.vialdevelopment.guerrillagradle.mapping.mapper.api.name.MethodName;
import io.github.vialdevelopment.guerrillagradle.mapping.mapper.impl.formats.TSearge;
import org.gradle.api.Project;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Paths;
import java.util.Map;

/**
 * A mapper for forge gradle 3.x
 */
public class ForgeGradle3 extends Mapper {

    private final TSearge notchToSrg = new TSearge();
    private final TSearge srgToMCP = new TSearge();

    /**
     * Init
     * @param project gradle project
     * @param mappings mappings version e.g. "1.12.2-20200226.224830 stable_39-1.12"
     */
    @Override
    public void init(Project project, String mappings) {
        String[] split = mappings.split(" ");
        notchToSrg.init(project, Paths.get(project.getGradle().getGradleUserHomeDir().getPath(), "caches/forge_gradle/minecraft_user_repo/de/oceanlabs/mcp/mcp_config", split[0], "obf_to_srg.tsrg").toAbsolutePath().toString());
        srgToMCP.init(project, Paths.get(project.getGradle().getGradleUserHomeDir().getPath(), "caches/forge_gradle/minecraft_user_repo/de/oceanlabs/mcp/mcp_config", split[0], "srg_to_" + split[1] + ".tsrg").toAbsolutePath().toString());
    }

    @Override
    public ClassName remapClassName(ClassName className) {
        return srgToMCP.remapClassName(notchToSrg.remapClassName(className));
    }

    @Override
    public MethodName remapMethodName(MethodName methodName) {
        return srgToMCP.remapMethodName(notchToSrg.remapMethodName(methodName));
    }

    @Override
    public FieldName remapFieldName(FieldName fieldName) {
        return srgToMCP.remapFieldName(notchToSrg.remapFieldName(fieldName));
    }

    @Override
    public void loadFromCache(ObjectInputStream objectInputStream) throws IOException, ClassNotFoundException {
        notchToSrg.loadFromCache(objectInputStream);
        srgToMCP.loadFromCache(objectInputStream);
    }

    @Override
    public void writeToCache(ObjectOutputStream objectOutputStream) throws IOException {
        notchToSrg.writeToCache(objectOutputStream);
        srgToMCP.writeToCache(objectOutputStream);
    }

    @Override
    public void setInheritanceMap(Map<String, String> inheritanceMap) {
        notchToSrg.setInheritanceMap(inheritanceMap);
        srgToMCP.setInheritanceMap(inheritanceMap);
    }
}
