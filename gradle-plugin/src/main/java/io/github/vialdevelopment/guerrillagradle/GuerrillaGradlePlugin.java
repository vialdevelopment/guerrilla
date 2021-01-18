package io.github.vialdevelopment.guerrillagradle;

import io.github.vialdevelopment.guerrillagradle.tasks.FixAllClasses;
import io.github.vialdevelopment.guerrillagradle.tasks.FixTransformerClasses;
import io.github.vialdevelopment.guerrillagradle.tasks.InitMapper;
import io.github.vialdevelopment.guerrillagradle.tasks.SetupMinecraftJar;
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

        TaskProvider<SetupMinecraftJar> setupMinecraftJarTaskProvider = project.getTasks().register("setupMinecraftJar", SetupMinecraftJar.class);
        setupMinecraftJarTaskProvider.configure(task -> {
            task.mapper = mapper;
            task.mcpVersion = extension.mcpVersion;
            task.forgeVersion = extension.forgeVersion;
        });

        TaskProvider<InitMapper> initMapperTaskProvider = project.getTasks().register("initMapper", InitMapper.class);
        initMapperTaskProvider.configure(task -> {
            task.mapper = mapper;
            task.mcpVersion = extension.mcpVersion;
        });

        javaCompile.dependsOn(setupMinecraftJarTaskProvider);
        javaCompile.finalizedBy(initMapperTaskProvider);
        javaCompile.finalizedBy(fixTransformerClassesTaskProvider);
        javaCompile.finalizedBy(fixAllClassesTaskProvider);

        project.afterEvaluate(action -> {
            setupMinecraftJarTaskProvider.get().process();
            project.getRepositories().flatDir(repo -> {
                repo.setName("guerrilla");
                repo.dirs(project.getGradle().getGradleUserHomeDir().getPath() + "/caches/minecraft/guerrilla/");
            });
            project.getDependencies().add("compile", ":io/github/vialdevelopment/minecraft-public/" + extension.mcpVersion + extension.forgeVersion + "/minecraft-public");
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
