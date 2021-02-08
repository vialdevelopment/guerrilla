# Registering Transformers
Obviously, your transformers have to be registered somehow.

The example shown is for forge's `IClassTransformer`,
but it can be used in every other use case where you are given
a class name and bytes.

```java
public class MyClassTransformer implements IClassTransformer {

    private boolean init = false;

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (name.startsWith("io.github.vialdevelopment.guerrilla.")) return basicClass;

        if (!init) {
            init = true;
            TransformManager.init();
            TransformManager.addTransform(MyOneTransformer.class);
            TransformManager.addTransform(TwoThreeTransformer.class);
            TransformManager.addTransform(BlockMoreTransformer.class);
        }
        
        return TransformManager.transformClass(name, basicClass);
    }
}
```

The `init` variable must be used, and the check for if a class is in the guerrilla package,<br> 
to prevent class circularity issues, and because you need to be able to add your own transformers, even if another mod has already initialized the `TransformManager`.

