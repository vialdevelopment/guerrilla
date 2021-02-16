package io.github.vialdevelopment.guerrillagradle;

import io.github.vialdevelopment.guerrillagradle.mapping.mapper.api.Mapper;
import io.github.vialdevelopment.guerrillagradle.tasks.*;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.compile.JavaCompile;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.TreeSet;

public class GuerrillaGradlePlugin implements Plugin<Project> {

    public static Mapper mapper = null;

    @Override
    public void apply(Project project) {
        GuerrillaGradlePluginExtension extension = project.getExtensions().create("guerrilla", GuerrillaGradlePluginExtension.class);
        JavaCompile javaCompile = (JavaCompile) project.getTasks().getByName("compileJava");

        TreeSet<String> alreadyDone = new TreeSet<>();
        Map<String, String> transformersTransforming = new HashMap<>();
        Properties properties = new Properties();

        TaskProvider<CreatePUBLICAndTree> createPublicJarTaskProvider = project.getTasks().register("createPublicTree", CreatePUBLICAndTree.class);
        createPublicJarTaskProvider.configure(task -> {
            task.extension = extension;
            task.javaCompile = javaCompile;
        });

        TaskProvider<InitMapperTask> initMapperTaskTaskProvider = project.getTasks().register("initMapper", InitMapperTask.class);
        initMapperTaskTaskProvider.configure(task -> {
            task.extension = extension;
        });

        TaskProvider<FixTransformerClassesTask> fixTransformerClassesTaskProvider = project.getTasks().register("fixTransformers", FixTransformerClassesTask.class);
        fixTransformerClassesTaskProvider.configure(task -> {
            task.extension = extension;
            task.buildClassesDirectory = javaCompile.getDestinationDir();
            task.alreadyUsedTransformers = alreadyDone;
            task.transformersTransforming = transformersTransforming;
        });

        TaskProvider<FixAllClassesTask> fixAllClassesTaskProvider = project.getTasks().register("fixAllClasses", FixAllClassesTask.class);
        fixAllClassesTaskProvider.configure(task -> {
            task.extension = extension;
            task.buildClassesDirectory = javaCompile.getDestinationDir();
            task.resourcesDir = new File(project.getBuildDir() + "/resources");
            task.alreadyUsedTransformers = alreadyDone;
            task.transformersTransforming = transformersTransforming;
            task.properties = properties;
        });

        TaskProvider<AddClassesToTransformExcludeTask> addClassesToTransformExcludeTaskProvider = project.getTasks().register("addRuntimeTransformExclude", AddClassesToTransformExcludeTask.class);
        addClassesToTransformExcludeTaskProvider.configure(task -> {
            task.extension = extension;
            task.buildClassesDirectory = javaCompile.getDestinationDir();
            task.properties = properties;
        });

        TaskProvider<WritePropertiesTask> writePropertiesTaskTaskProvider = project.getTasks().register("WriteProperties", WritePropertiesTask.class);
        writePropertiesTaskTaskProvider.configure(task -> {
            task.properties = properties;
            task.resourcesDir = project.getBuildDir() + "/resources";
        });

        javaCompile.dependsOn(createPublicJarTaskProvider);
        javaCompile.dependsOn(initMapperTaskTaskProvider);
        javaCompile.finalizedBy(fixTransformerClassesTaskProvider);
        javaCompile.finalizedBy(fixAllClassesTaskProvider);
        javaCompile.finalizedBy(addClassesToTransformExcludeTaskProvider);
        javaCompile.finalizedBy(writePropertiesTaskTaskProvider);

        project.getTasks().findByName("createPublicTree").mustRunAfter(project.getTasks().findByName("initMapper"));

        project.afterEvaluate(action -> {
            project.getRepositories().flatDir(repo -> {
                repo.setName("guerrilla");
                repo.dirs(project.getBuildDir() + "/tmp/guerrilla");
            });
            project.getDependencies().add("compile", ":public");
        });
    }
}
