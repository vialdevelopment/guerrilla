package io.github.vialdevelopment.guerrillagradle.tasks;

import io.github.vialdevelopment.guerrillagradle.Mapper;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

/**
 * Inits the mapper
 */
public class InitMapper extends DefaultTask {
    /** Mapper instance */
    public Mapper mapper;
    /** MCP version */
    public String mcpVersion;
    /** Mappings SRG File */
    public String mappingsSrgFile;

    @TaskAction
    public void process() {
        mapper.init(getProject(), mcpVersion, mappingsSrgFile);
    }

}
