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



        TaskProvider<CreatePublicJar> createPublicJarTaskProvider = project.getTasks().register("createPublicJar", CreatePublicJar.class);
        createPublicJarTaskProvider.configure(task -> {
            task.javaCompile = javaCompile;
            task.mapper = mapper;
            task.makePublics = extension.makePublic;
        });

        TaskProvider<InitMapper> initMapperTaskProvider = project.getTasks().register("initMapper", InitMapper.class);
        initMapperTaskProvider.configure(task -> {
            task.mapper = mapper;
            task.mcpVersion = extension.mcpVersion;
        });

        TaskProvider<FixTransformerClasses> fixTransformerClassesTaskProvider = project.getTasks().register("fixTransformers", FixTransformerClasses.class);
        fixTransformerClassesTaskProvider.configure(task -> {
            task.buildClassesDirectory = javaCompile.getDestinationDir();
            task.transformers = extension.transformers;
            task.mapper = mapper;
            task.alreadyUsedTransformers = alreadyDone;
            task.transformersTransforming = transformersTransforming;
        });

        TaskProvider<FixAllClasses> fixAllClassesTaskProvider = project.getTasks().register("fixAllClasses", FixAllClasses.class);
        fixAllClassesTaskProvider.configure(task -> {
            task.buildClassesDirectory = javaCompile.getDestinationDir();
            task.resourcesDir = new File(project.getBuildDir() + "/resources");
            task.alreadyUsedTransformers = alreadyDone;
            task.makePublics = extension.makePublic;
            task.transformers = extension.transformers;
            task.transformer = extension.transformer;
            task.transformersTransforming = transformersTransforming;
        });

        TaskProvider<AddClassesToTransformExclude> addClassesToTransformExcludeTaskProvider = project.getTasks().register("AddRuntimeTransformExclude", AddClassesToTransformExclude.class);
        addClassesToTransformExcludeTaskProvider.configure(task -> {
            task.buildClassesDirectory = javaCompile.getDestinationDir();
            task.resourcesDir = new File(project.getBuildDir() + "/resources");
            task.transformers = extension.transformers;
        });

        javaCompile.dependsOn(createPublicJarTaskProvider);
        javaCompile.finalizedBy(initMapperTaskProvider);
        javaCompile.finalizedBy(fixTransformerClassesTaskProvider);
        javaCompile.finalizedBy(fixAllClassesTaskProvider);
        javaCompile.finalizedBy(addClassesToTransformExcludeTaskProvider);

        project.afterEvaluate(action -> {
            // setupMinecraftJarTaskProvider.get().process();
            project.getRepositories().flatDir(repo -> {
                repo.setName("guerrilla");
                repo.dirs(project.getBuildDir() + "/tmp/guerrilla");
            });
            project.getDependencies().add("compile", ":public");
        });
    }
}
