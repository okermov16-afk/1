package net.minecraftforge.fml.common;
import java.lang.annotation.*;
@Retention(RetentionPolicy.RUNTIME)
public @interface Mod {
    String modid(); String name() default ""; String version() default ""; String acceptedMinecraftVersions() default ""; boolean clientSideOnly() default false;
    @Retention(RetentionPolicy.RUNTIME) @interface EventHandler {}
}
