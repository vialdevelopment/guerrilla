package io.github.vialdevelopment.guerrillagradle.tasks;

import io.github.vialdevelopment.guerrillagradle.GuerrillaGradlePluginExtension;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.TreeSet;

/**
 * Reads in all the transformer classes and adds them to a list to be excluded from runtime transformation
 */
public class AddClassesToTransformExcludeTask extends DefaultTask {
    /** config extension */
    public GuerrillaGradlePluginExtension extension;
    /** project build directory */
    public File buildClassesDirectory;
    /** guerrilla properties */
    public Properties properties;

    @TaskAction
    public void process() {
        TreeSet<String> toExclude = new TreeSet<>();
        File transformersFolder = new File(buildClassesDirectory.getPath() + "/" + extension.transformersPackage);
        try {
            // loop over all transformer class files
            Files.walk(Paths.get(transformersFolder.toURI())).forEach(path -> {
                try {
                    // make sure is file and isn't one we generated
                    if (path.toFile().isFile() && path.toString().endsWith(".class")) {
                        // read in the class file
                        byte[] transformerBytes = Files.readAllBytes(path);

                        ClassNode classNode = new ClassNode();
                        ClassReader classReader = new ClassReader(transformerBytes);
                        classReader.accept(classNode, 0);

                        toExclude.add(classNode.name);
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        StringBuilder transformExcludeBuilder = new StringBuilder();
        toExclude.forEach(s -> { transformExcludeBuilder.append(s); transformExcludeBuilder.append(';'); });
        properties.setProperty("transform-exclude", transformExcludeBuilder.toString());
    }

}
