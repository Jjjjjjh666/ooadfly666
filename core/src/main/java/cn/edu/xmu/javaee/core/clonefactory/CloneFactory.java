package cn.edu.xmu.javaee.core.clonefactory;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记CloneFactory的包名
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.SOURCE)
public @interface CloneFactory {
    String value() default "cn.edu.xmu.javaee.core.util";
}
