package JYBank.JYBank.aop.annotation;

import java.lang.annotation.*;
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {
    String key(); // SpEL e.g. "#idemKey"
    long ttlSeconds() default 120;
}
