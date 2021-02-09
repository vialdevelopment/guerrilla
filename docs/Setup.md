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
        classpath "io.github.vialdevelopment.guerrilla:guerrilla-gradle:VERSION"
    }   
}

apply plugin: "io.github.vialdevelopment.guerrilla-gradle"

guerrilla {
    mcpVersion = "stable_39"
    transformers = "my/project/transformers/package"
    transformer = "my/project/loading/transformer"
    makePublic = ['make/everything/here/public/.*']
}
```

Consult the javadoc for what each of the guerrilla config options means.

### Runtime
```groovy
repositories {
        maven { url 'https://jitpack.io' }
}

dependencies {
    compile('io.github.vialdevelopment.guerrilla:guerrilla-runtime:VERSION')
}
```
