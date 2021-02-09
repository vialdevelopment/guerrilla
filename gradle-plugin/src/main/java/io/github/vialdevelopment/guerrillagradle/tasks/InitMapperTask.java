package io.github.vialdevelopment.guerrillagradle.tasks;

import io.github.vialdevelopment.guerrillagradle.GuerrillaGradlePluginExtension;
import io.github.vialdevelopment.guerrillagradle.Mapper;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

/**
 * Inits the mapper
 */
public class InitMapperTask extends DefaultTask {
    /** config extension */
    public GuerrillaGradlePluginExtension extension;
    /** Mapper instance */
    public Mapper mapper;

    @TaskAction
    public void process() {
        mapper.init(getProject(), extension.mcpVersion, extension.mappingsSrgFile);
    }

}
