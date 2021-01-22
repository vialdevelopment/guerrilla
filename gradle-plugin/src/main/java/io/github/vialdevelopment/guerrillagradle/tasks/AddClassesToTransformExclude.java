package io.github.vialdevelopment.guerrillagradle.tasks;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.TreeSet;

public class AddClassesToTransformExclude extends DefaultTask {
    /** project build directory */
    public File buildClassesDirectory;
    /** project build resources folder */
    public File resourcesDir;
    /** transformers package */
    public String transformers;

    @TaskAction
    public void process() {
        TreeSet<String> toExclude = new TreeSet<>();
        File transformersFolder = new File(buildClassesDirectory.getPath() + "/" + transformers);
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

        try {
            // now we write the classes to receive the public and non-final abuse to a file
            // to be done at runtime
            // FIXME this shouldn't be always in the main submodule
            File excludeTransformFile = new File(resourcesDir + "/main/guerrilla-transform-exclude.txt");
            excludeTransformFile.getParentFile().mkdirs();
            FileWriter fileWriter = new FileWriter(excludeTransformFile);
            for (String s : toExclude) {
                fileWriter.write(s);
                fileWriter.write("\n");
            }
            fileWriter.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
