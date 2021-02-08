# Writing Transformers

## Target class

```java
@TransformClass(className = "bar.foo.Boxes")
public class MyTransformer extends Boxes {

}
```
This is a transformer class, which targets `EntityPlayerSP`.
Extending the class we're transforming may seem strange. It is.<br>
This extending is actually stripped out by the gradle plugin.
We don't have to extend the class being transformed, but it makes certain actions a lot easier.

## Implements
```java
public class MyTransformer implements MyInterface {}
```
Any interfaces are automatically copied over.

## ASM Method Transformation
```java
@TransformMethod(methodName = "walk", methodDesc = "(Ljava/lang/String;D)V")
public static void transformWalk(MethodNode methodNode, boolean obf) {
    // ASM transformation done here
}
```
You can perform any cursed ASM transformations to the method you want here, taking advantage of ASM 9.0
features and utilities like `Pattern`.

## Inserting at HEAD
```java
@TransformMethod(methodName = "walk", methodDesc = "(Ljava/lang/String;D)V")
@Insert(value = @At(at = At.loc.HEAD))
public void hookHeadWalk(String person, double ticks) {
    // add code to be inserted at start of method here
}
```
This inserts your code at the beginning of the method being transformed.<br>
Your hook method should have the same parameters and return type.

## Inserting at RETURN
```java
@TransformMethod(methodName = "walk", methodDesc = "(Ljava/lang/String;D)V")
@Insert(value = @At(at = At.loc.RETURN))
public void hookReturnWalk(String person, double ticks) {
    // add code to be inserted before every return of the method here 
}
```
This inserts your code before every return in the method being transformed.<br>
Your hook method should have the same parameters and return type.

## Redirecting a method call
```java
@TransformMethod(methodName = "walk", methodDesc = "(Ljava/lang/String;D)V")
@Insert(value = @At(at = At.loc.INVOKE, ref = Opcodes.INVOKEVIRTUAL + " the/foo/owner redirectedMethod (Ljava/lang/Boolean;D)V"))
public void hooWalkRedirectedMethod(Boolean dupe, double ticks) {
    // add code for this method call 
}
```
This redirects a method call, with a method with the same return value and the args of:

    - if static: method's object
    - redirected method's args
    - caller method's args
    
## Redirecting a field call
```java
@TransformMethod(methodName = "walk", methodDesc = "(Ljava/lang/String;D)V")
@Insert(value = @At(at = At.loc.FIELD, ref = Opcodes.GETFIELD + " the/foo/Owner theField Z"))
public boolean hooWalkRedirectedField(Owner owner, String name, double ticks) {
    // add code for this field call 
}
```
This redirects a field call, with a method with the same return value as the field type and the args of:

    - if static: field's object
    - caller method args

## Overwriting
```java
@TransformMethod(methodName = "walk", methodDesc = "(Ljava/lang/String;D)V")
@Overwrite
public void walkOverwrite(String person, double ticks) {
    // new code
}
```

This overwrites the method transformed, replacing it with your method.<br>
This is a crime to use.

## Class variables
```java
@TransformClass(className = "bar.foo.Boxes")
public class MyTransformer extends Boxes {
    public int a = 5;
}
```

These are automatically inlined. 
