package uk.co.itstherules.screencast.extensions;


import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Tag("screencast")
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(ScreenCastExtension.class)
@Target({ElementType.METHOD})
public @interface ScreenCast {
    String fileName() default "";
}
