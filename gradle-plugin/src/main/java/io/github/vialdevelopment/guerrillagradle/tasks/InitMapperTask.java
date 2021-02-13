package io.github.vialdevelopment.guerrillagradle.tasks;

import io.github.vialdevelopment.guerrillagradle.GuerrillaGradlePlugin;
import io.github.vialdevelopment.guerrillagradle.GuerrillaGradlePluginExtension;
import io.github.vialdevelopment.guerrillagradle.mapping.manager.MappingManager;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

/**
 * Inits the mapper
 */
public class InitMapperTask extends DefaultTask {
    /** config extension */
    public GuerrillaGradlePluginExtension extension;

    @TaskAction
    public void process() {
        GuerrillaGradlePlugin.mapper = new MappingManager().getMapper(extension.mappingsType);
        GuerrillaGradlePlugin.mapper.init(getProject(), extension.mappings);
    }

}
