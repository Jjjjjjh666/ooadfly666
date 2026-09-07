package cn.edu.xmu.javaee.core.clonefactory;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *  标识从那些类拷贝属性（仅拷贝非空属性），编译前自动生成CloneFactory的copyNotNull方法
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.SOURCE)
public @interface CopyNotNullFrom {
    Class<?>[] value();
    @Target({ ElementType.METHOD,ElementType.FIELD  })
    @Retention(RetentionPolicy.SOURCE)
    /**
     * 写在field前，排除从Exclude中的类的属性拷贝
     */
    public @interface Exclude {
        Class<?>[] value();
    }
    /**
     * 写在field前，只从Of中类对应属性拷贝
     */
    @Target({ ElementType.METHOD,ElementType.FIELD })
    @Retention(RetentionPolicy.SOURCE)
    public @interface Of {
        Class<?>[] value();
    }
}