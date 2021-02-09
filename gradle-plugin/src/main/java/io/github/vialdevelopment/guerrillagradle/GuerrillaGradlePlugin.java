package io.github.vialdevelopment.guerrillagradle;

import io.github.vialdevelopment.guerrillagradle.tasks.*;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.compile.JavaCompile;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

public class GuerrillaGradlePlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        GuerrillaGradlePluginExtension extension = project.getExtensions().create("guerrilla", GuerrillaGradlePluginExtension.class);
        JavaCompile javaCompile = (JavaCompile) project.getTasks().getByName("compileJava");

        Mapper mapper = new Mapper();
        TreeSet<String> alreadyDone = new TreeSet<>();
        Map<String, String> transformersTransforming = new HashMap<>();



        TaskProvider<CreatePublicJarTask> createPublicJarTaskProvider = project.getTasks().register("createPublicJar", CreatePublicJarTask.class);
        createPublicJarTaskProvider.configure(task -> {
            task.extension = extension;
            task.javaCompile = javaCompile;
            task.mapper = mapper;
        });

        TaskProvider<InitMapperTask> initMapperTaskProvider = project.getTasks().register("initMapper", InitMapperTask.class);
        initMapperTaskProvider.configure(task -> {
            task.extension = extension;
            task.mapper = mapper;
        });

        TaskProvider<FixTransformerClassesTask> fixTransformerClassesTaskProvider = project.getTasks().register("fixTransformers", FixTransformerClassesTask.class);
        fixTransformerClassesTaskProvider.configure(task -> {
            task.extension = extension;
            task.buildClassesDirectory = javaCompile.getDestinationDir();
            task.mapper = mapper;
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
            task.mapper = mapper;
        });

        TaskProvider<AddClassesToTransformExcludeTask> addClassesToTransformExcludeTaskProvider = project.getTasks().register("AddRuntimeTransformExclude", AddClassesToTransformExcludeTask.class);
        addClassesToTransformExcludeTaskProvider.configure(task -> {
            task.extension = extension;
            task.buildClassesDirectory = javaCompile.getDestinationDir();
        });

        javaCompile.dependsOn(createPublicJarTaskProvider);
        javaCompile.finalizedBy(initMapperTaskProvider);
        javaCompile.finalizedBy(fixTransformerClassesTaskProvider);
        javaCompile.finalizedBy(fixAllClassesTaskProvider);
        javaCompile.finalizedBy(addClassesToTransformExcludeTaskProvider);

        project.afterEvaluate(action -> {
            project.getRepositories().flatDir(repo -> {
                repo.setName("guerrilla");
                repo.dirs(project.getBuildDir() + "/tmp/guerrilla");
            });
            project.getDependencies().add("compile", ":public");
        });
    }
}
