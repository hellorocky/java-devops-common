package me.rockywu.common.monitor;

import java.lang.annotation.*;

/**
 * 这里的参数可以作为开发者在使用这个注解的时候传递参数来自定义一些label.目前没有使用到
 * @author RockyWu
 * @date 2018/11/8
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PrometheusMetrics {
    String service() default "";
}

