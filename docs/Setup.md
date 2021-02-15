# Setup

## Gradle config
Guerrilla is made up of 2 components, a gradle plugin and runtime

### Gradle plugin
```groovy
buildscript {
    repositories {
        maven { url 'https://jitpack.io' }
    }
    dependencies {
        classpath 'io.github.vialdevelopment.guerrilla:guerrilla-gradle:0.0.2'
    }   
}

apply plugin: 'io.github.vialdevelopment.guerrilla-gradle'

guerrilla {
    mappingsType = 'FORGE_GRADLE_2'
    mappings = 'stable_39'
    transformersPackage = 'my/project/transformers/package'
    transformerRegistrationClass = 'my/project/loading/transformer'
    makePublic = ['make/everything/here/public/.*']
}
```

Consult the javadoc of `io.github.vialdevelopment.guerrillagradle.GuerrillaGradleExtension` for more info about these options.

### Runtime
```groovy
repositories {
        maven { url 'https://jitpack.io' }
}

dependencies {
    compile('io.github.vialdevelopment.guerrilla:guerrilla-runtime:0.0.3')
}
```

## Finally
Before you can compile your project, you must run the gradle `createPublicTree` command.
