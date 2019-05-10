package com.github.winter4666.datasourcerouter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 数据源注解
 * @author wutian
 */
@Target({ElementType.METHOD,ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface DataSource {
	
	/**
	 * lookupKey，该值可直接确定要使用的数据源
	 * @return
	 */
	String value() default "";
	
	/**
	 * 传递给LookupKeyBuilder类getLookupKey的参数,支持spel表达式
	 * @return
	 */
	String param() default "";
	
	/**
	 * 该类封装生成lookupKey的算法
	 * @return
	 */
	Class<? extends LookupKeyBuilder> builderClass() default LookupKeyBuilder.class;
	
}
