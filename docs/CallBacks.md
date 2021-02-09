# CallBacks
CallBacks are used to return values from your patch methods.

This is because the compiler automatically inserts `return` instructions
into your code, which need to be stripped out, otherwise your code would just
always return the method being transformed. To get around this,
we use `CallBack`.<br>The `CallBack` calls are actually at runtime
transformed into return statements, and the original return statements
that the compiler inserted are stripped out.

## Returning void
```java
@TransformMethod(name = "foo", desc = "(Ljava/lang/String;I)V")
@Insert(@At(at = At.loc.HEAD))
public void foo(String dod, int bar) {
    PooEvent event = new PooEvent(dod, bar);
    Bus.INSTANCE.dispatch(event);
    if (event.isCanceled()) CallBack.cancel(null);
}
```
This method is inserted into the `foo` method, and if our `PooEvent` is cancelled, then we return.

## Returning a value
```java
@TransformMethod(name = "canBeSteered", desc = "()Z")
@Insert(returnType = ASMFactory.EReturnTypes.BOOLEAN, value = @At(at = At.loc.HEAD))
public void canBeSteered() {
    if(MyThingy.INSTANCE.getState()) {
        CallBack.cancel(true);
    }
}
```
This method is inserted into the 'canBeSteered' method, and if `MyThings` is enabled, we return with `true`.<br>
The `returnType` must be specified in the `@Insert` annotation.
