package com.jnimble.platform.persistence.crud;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for specifying MyBatis-Plus query wrapper behavior on entity fields.
 *
 * <p>Allows customizing the query condition type (e.g., "eq", "like") and
 * specifying alternative field names for query construction.</p>
 *
 * <p>用于指定实体字段上 MyBatis-Plus 查询 Wrapper 行为的注解。
 * 允许自定义查询条件类型（如 "eq"、"like"）并指定用于查询构造的替代字段名。</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface WrapperType {

    /**
     * The query condition type.
     *
     * @return the type string, defaults to "eq" (equals)
     */
    String type() default "eq";

    /**
     * Alternative field names for query construction.
     *
     * @return an array of field names
     */
    String[] fields() default {};
}
