package cn.edu.xmu.javaee.core.clonefactory;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *  标识向哪些类拷贝属性，编译前自动生成CloneFactory的copy方法
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.SOURCE)
public @interface CopyTo {
    Class<?>[] value();
    /**
     * 写在field前，排除向Exclude中的类的属性拷贝
     */
    @Target({ ElementType.METHOD,ElementType.FIELD  })
    @Retention(RetentionPolicy.SOURCE)
    public @interface Exclude {
        Class<?>[] value();
    }
    /**
     * 写在field前，只向Of中类对应属性拷贝
     */
    @Target({ ElementType.METHOD,ElementType.FIELD })
    @Retention(RetentionPolicy.SOURCE)
    public @interface Of {
        Class<?>[] value();
    }
}
