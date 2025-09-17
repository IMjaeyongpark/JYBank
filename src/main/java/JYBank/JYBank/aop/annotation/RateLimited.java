package JYBank.JYBank.aop.annotation;

import java.lang.annotation.*;
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimited {
    String key(); // SpEL e.g. "'transfer:' + #userId"
    long permitsPerMinute() default 30;
}
