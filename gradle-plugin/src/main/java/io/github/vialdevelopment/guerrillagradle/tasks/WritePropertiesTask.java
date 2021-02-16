package io.github.vialdevelopment.guerrillagradle.tasks;

import io.github.vialdevelopment.guerrillagradle.GuerrillaGradlePluginExtension;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

public class WritePropertiesTask extends DefaultTask {
    /** properties */
    public Properties properties;
    /** resources directory */
    public String resourcesDir;
    /** extension */
    public GuerrillaGradlePluginExtension extension;

    @TaskAction
    public void process() {
        properties.setProperty("compute-frames", extension.computeFrames ? "true" : "false");
        try {
            properties.store(new FileWriter(resourcesDir + "/main/guerrilla.properties"), null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
