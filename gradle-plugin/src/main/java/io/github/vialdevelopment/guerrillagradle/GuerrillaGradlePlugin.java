package io.github.vialdevelopment.guerrillagradle;

import io.github.vialdevelopment.guerrillagradle.tasks.*;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.compile.JavaCompile;

import java.io.File;
import java.util.TreeSet;

public class GuerrillaGradlePlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        GuerrillaGradlePluginExtension extension = project.getExtensions().create("guerrilla", GuerrillaGradlePluginExtension.class);

        Mapper mapper = new Mapper();

        AlreadyUsedTransformersHolder alreadyUsedTransformersHolder = new AlreadyUsedTransformersHolder();

        JavaCompile javaCompile = (JavaCompile) project.getTasks().getByName("compileJava");

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
            task.alreadyUsedTransformersHolder = alreadyUsedTransformersHolder;
        });

        TaskProvider<FixAllClasses> fixAllClassesTaskProvider = project.getTasks().register("fixAllClasses", FixAllClasses.class);
        fixAllClassesTaskProvider.configure(task -> {
            task.buildClassesDirectory = javaCompile.getDestinationDir();
            task.resourcesDir = new File(project.getBuildDir() + "/resources");
            task.alreadyUsedTransformersHolder = alreadyUsedTransformersHolder;
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

        project.getTasks().getByName("clean").doLast(action -> {
            try {
                if (extension.mcpVersion != null && extension.forgeVersion != null) {
                    new File(project.getGradle().getGradleUserHomeDir().getPath() + "/caches/minecraft/guerrilla/io/github/vialdevelopment/minecraft-public/" + extension.mcpVersion + extension.forgeVersion + "/minecraft-public.tree").delete();
                    new File(project.getGradle().getGradleUserHomeDir().getPath() + "/caches/minecraft/guerrilla/io/github/vialdevelopment/minecraft-public/" + extension.mcpVersion + extension.forgeVersion + "/minecraft-public.jar").delete();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public static class AlreadyUsedTransformersHolder {
        public TreeSet<String> alreadyDone = new TreeSet<>();
    }
}
