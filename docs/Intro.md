# Guerrilla
## What is this?
This is a lightweight java bytecode patching utility, allowing the insertion and redirection of code at runtime, 
though it doesn't have to be.

## Why should I use this?
    - It's lightweight
        - Just 500kb compressed compared to mixin's 1mb compressed
    - It's simple
        - Need to access a private field? Just use theClassPUBLIC!
        - Want to perform something simple that couldn't possibly be written normally? Just use ASM transformation!
        - Very little unneeded bloat
        - It performs straight instrumentation, bytes in, bytes out, as decoupled from Minecraft as possible
        - It's very easy to use outside of minecraft mods, such as in java agents
    - It's fast
        - Benchmarking is needed (but general rule of bloat supports my theory)
     
## Warnings
If you want a safety net, use mixins. This performs no checking that what you are doing is safe or correct.<br>
Try not to screw up the code you're transforming for some compatibility.
