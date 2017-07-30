package uk.co.itstherules.snapshot;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Tag("testcast")
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(SnapShotExtension.class)
@Target({ ElementType.METHOD })
public @interface SnapShot {
    String fileName() default "";
}