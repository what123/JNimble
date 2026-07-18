package com.jnimble.platform.persistence.crud;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Utility class for common MyBatis-Plus CRUD operations.
 *
 * <p>Provides convenience methods for insert, update, select, delete, pagination,
 * and dynamic query building with column validation and SQL injection prevention.
 * All entity columns are validated against the entity class's declared fields
 * before being used in query conditions.</p>
 *
 * <p>MyBatis-Plus 通用 CRUD 操作工具类。提供插入、更新、查询、删除、分页和
 * 动态查询构建的便捷方法，包含列校验和 SQL 注入防护。所有实体列在使用前
 * 都会根据实体类声明的字段进行校验。</p>
 */
public final class MapperUtils {

    private MapperUtils() {
    }

    /**
     * Inserts an entity into the database and returns it.
     *
     * @param mapper the MyBatis mapper
     * @param entity the entity to insert
     * @param <T>    the entity type
     * @return the inserted entity
     */
    public static <T> T insert(BaseMapper<T> mapper, T entity) {
        mapper.insert(entity);
        return entity;
    }

    /**
     * Updates an entity by its primary key.
     *
     * @param mapper the MyBatis mapper
     * @param entity the entity with updated fields and primary key set
     * @param <T>    the entity type
     * @return the updated entity
     * @throws IllegalArgumentException if the record is not found
     */
    public static <T> T updateById(BaseMapper<T> mapper, T entity) {
        requireUpdated(mapper.updateById(entity), "Record not found");
        return entity;
    }

    /**
     * Updates entities matching the given condition.
     *
     * @param mapper       the MyBatis mapper
     * @param entity       the entity with fields to update
     * @param entityClass  the entity class for column validation
     * @param customizer   the query condition customizer
     * @param <T>          the entity type
     * @return the number of updated rows
     * @throws IllegalArgumentException if the record is not found
     */
    public static <T> int updateOne(BaseMapper<T> mapper, T entity, Class<T> entityClass, Consumer<QueryWrapper<T>> customizer) {
        QueryWrapper<T> queryWrapper = buildWrapper(entityClass, customizer);
        int updated = mapper.update(entity, queryWrapper);
        requireUpdated(updated, "Record not found");
        return updated;
    }

    /**
     * Updates entities matching the given condition without requiring at least one match.
     *
     * @param mapper       the MyBatis mapper
     * @param entity       the entity with fields to update
     * @param entityClass  the entity class for column validation
     * @param customizer   the query condition customizer
     * @param <T>          the entity type
     * @return the number of updated rows
     */
    public static <T> int updateByCondition(
            BaseMapper<T> mapper,
            T entity,
            Class<T> entityClass,
            Consumer<QueryWrapper<T>> customizer
    ) {
        QueryWrapper<T> queryWrapper = buildWrapper(entityClass, customizer);
        return mapper.update(entity, queryWrapper);
    }

    /**
     * Selects an entity by its primary key.
     *
     * @param mapper         the MyBatis mapper
     * @param id             the primary key value
     * @param missingMessage the error message if not found (null to skip exception)
     * @param <T>            the entity type
     * @return the entity, or null if not found and missingMessage is null
     * @throws IllegalArgumentException if not found and missingMessage is provided
     */
    public static <T> T getById(BaseMapper<T> mapper, Object id, String missingMessage) {
        T entity = mapper.selectById(serializableId(id));
        if (entity == null && missingMessage != null) {
            throw new IllegalArgumentException(missingMessage);
        }
        return entity;
    }

    /**
     * Selects a single entity matching the given condition.
     *
     * @param mapper      the MyBatis mapper
     * @param entityClass the entity class for column validation
     * @param customizer  the query condition customizer
     * @param <T>         the entity type
     * @return the matching entity, or null if not found
     */
    public static <T> T selectOne(BaseMapper<T> mapper, Class<T> entityClass, Consumer<QueryWrapper<T>> customizer) {
        return mapper.selectOne(buildWrapper(entityClass, customizer));
    }

    /**
     * Deletes an entity by its primary key.
     *
     * @param mapper the MyBatis mapper
     * @param id     the primary key value
     * @param <T>    the entity type
     * @return the number of deleted rows
     */
    public static <T> int deleteById(BaseMapper<T> mapper, Object id) {
        return mapper.deleteById(serializableId(id));
    }

    /**
     * Deletes entities matching the given condition.
     *
     * @param mapper      the MyBatis mapper
     * @param entityClass the entity class for column validation
     * @param customizer  the query condition customizer
     * @param <T>         the entity type
     * @return the number of deleted rows
     */
    public static <T> int deleteByCondition(
            BaseMapper<T> mapper,
            Class<T> entityClass,
            Consumer<QueryWrapper<T>> customizer
    ) {
        return mapper.delete(buildWrapper(entityClass, customizer));
    }

    /**
     * Checks if any entity exists matching the given condition.
     *
     * @param mapper      the MyBatis mapper
     * @param entityClass the entity class for column validation
     * @param customizer  the query condition customizer
     * @param <T>         the entity type
     * @return {@code true} if at least one matching entity exists
     */
    public static <T> boolean existsByCondition(
            BaseMapper<T> mapper,
            Class<T> entityClass,
            Consumer<QueryWrapper<T>> customizer
    ) {
        return mapper.exists(buildWrapper(entityClass, customizer));
    }

    /**
     * Selects a list of entities matching the given condition.
     *
     * @param mapper      the MyBatis mapper
     * @param entityClass the entity class for column validation
     * @param customizer  the query condition customizer
     * @param <T>         the entity type
     * @return a list of matching entities
     */
    public static <T> List<T> selectList(BaseMapper<T> mapper, Class<T> entityClass, Consumer<QueryWrapper<T>> customizer) {
        return mapper.selectList(buildWrapper(entityClass, customizer));
    }

    /**
     * Performs a paginated query with offset-based pagination.
     *
     * @param mapper      the MyBatis mapper
     * @param entityClass the entity class for column validation
     * @param request     the pagination request containing offset, size, and sort info
     * @param customizer  the query condition customizer
     * @param <T>         the entity type
     * @return a page result containing the data list and total count
     */
    public static <T> SPage<T> selectByOffset(
            BaseMapper<T> mapper,
            Class<T> entityClass,
            PageSearchRequest<?> request,
            Consumer<QueryWrapper<T>> customizer
    ) {
        PageSearchRequest.PageInfo pageInfo = request == null ? new PageSearchRequest.PageInfo() : request.getPage();
        int offset = Math.max(0, pageInfo.getOffset() == null ? 0 : pageInfo.getOffset());
        int size = Math.max(1, Math.min(pageInfo.getSize() == null ? 20 : pageInfo.getSize(), 500));
        QueryWrapper<T> countWrapper = buildWrapper(entityClass, customizer);
        long total = mapper.selectCount(countWrapper);
        QueryWrapper<T> dataWrapper = buildWrapper(entityClass, customizer);
        applySort(dataWrapper, entityClass, pageInfo.getSort(), null);
        dataWrapper.last("limit " + size + " offset " + offset);
        return new SPage<>(mapper.selectList(dataWrapper), total);
    }

    /**
     * Builds a QueryWrapper with the given condition customizer.
     *
     * @param entityClass the entity class for column validation
     * @param customizer  the query condition customizer
     * @param <T>         the entity type
     * @return a configured QueryWrapper
     */
    public static <T> QueryWrapper<T> buildWrapper(
            Class<T> entityClass,
            Consumer<QueryWrapper<T>> customizer
    ) {
        QueryWrapper<T> wrapper = new QueryWrapper<>();
        if (customizer != null) {
            customizer.accept(wrapper);
        }
        validateColumns(entityClass, wrapper);
        return wrapper;
    }

    /**
     * Builds a QueryWrapper from a BaseSearchCondition with filters and sort.
     *
     * @param entityClass the entity class for column validation
     * @param condition   the search condition with filters
     * @param defaultSort the default sort expression
     * @param <T>         the entity type
     * @return a configured QueryWrapper
     */
    public static <T> QueryWrapper<T> buildWrapper(
            Class<T> entityClass,
            BaseSearchCondition condition,
            String defaultSort
    ) {
        QueryWrapper<T> wrapper = new QueryWrapper<>();
        applyFilters(wrapper, entityClass, condition == null ? null : condition.getFilters());
        applySort(wrapper, entityClass, defaultSort, defaultSort);
        return wrapper;
    }

    /**
     * Builds an UpdateWrapper with the given condition customizer.
     *
     * @param entityClass the entity class for column validation
     * @param customizer  the update condition customizer
     * @param <T>         the entity type
     * @return a configured UpdateWrapper
     */
    public static <T> UpdateWrapper<T> updateWrapper(Class<T> entityClass, Consumer<UpdateWrapper<T>> customizer) {
        UpdateWrapper<T> wrapper = new UpdateWrapper<>();
        if (customizer != null) {
            customizer.accept(wrapper);
        }
        validateColumns(entityClass, wrapper);
        return wrapper;
    }

    /**
     * Applies sort ordering to an existing QueryWrapper.
     *
     * @param wrapper     the query wrapper to apply sort to
     * @param entityClass the entity class for column validation
     * @param sort        the sort expression (e.g., "created_at desc")
     * @param defaultSort the default sort if sort is null/blank
     * @param <T>         the entity type
     * @return the same QueryWrapper with sort applied
     */
    public static <T> QueryWrapper<T> orderBy(
            QueryWrapper<T> wrapper,
            Class<T> entityClass,
            String sort,
            String defaultSort
    ) {
        applySort(wrapper, entityClass, sort, defaultSort);
        return wrapper;
    }

    private static <T> void applyFilters(QueryWrapper<T> wrapper, Class<T> entityClass, List<SearchFilter> filters) {
        if (filters == null || filters.isEmpty()) {
            return;
        }
        Map<String, String> columns = allowedColumns(entityClass);
        for (SearchFilter filter : filters) {
            if (filter == null || blank(filter.getKey())) {
                continue;
            }
            String column = requireColumn(columns, filter.getKey());
            String match = blank(filter.getMatch()) ? "eq" : filter.getMatch().trim().toLowerCase(Locale.ROOT);
            switch (match) {
                case "eq" -> wrapper.eq(column, filterValue(filter));
                case "like" -> wrapper.like(column, filter.getKeyword());
                case "in" -> wrapper.in(column, filter.getValues());
                case "gt" -> wrapper.gt(column, filterValue(filter));
                case "ge" -> wrapper.ge(column, filterValue(filter));
                case "lt" -> wrapper.lt(column, filterValue(filter));
                case "le" -> wrapper.le(column, filterValue(filter));
                case "between" -> wrapper.between(column,
                        filter.getMin() == null ? null : filter.getMin().getValue(),
                        filter.getMax() == null ? null : filter.getMax().getValue());
                case "zero_to_null_eq" -> {
                    Object value = filterValue(filter);
                    if (Objects.equals(value, 0) || Objects.equals(value, 0L)) {
                        wrapper.isNull(column);
                    } else {
                        wrapper.eq(column, value);
                    }
                }
                default -> throw new IllegalArgumentException("Unsupported filter match: " + match);
            }
        }
    }

    private static <T> void applySort(
            QueryWrapper<T> wrapper,
            Class<T> entityClass,
            String sort,
            String defaultSort
    ) {
        String effectiveSort = blank(sort) ? defaultSort : sort;
        if (blank(effectiveSort)) {
            return;
        }
        Map<String, String> columns = allowedColumns(entityClass);
        for (String part : effectiveSort.split(",")) {
            if (blank(part)) {
                continue;
            }
            String[] tokens = part.trim().split("\\s+");
            String column = requireColumn(columns, tokens[0]);
            String direction = tokens.length > 1 ? tokens[1].toLowerCase(Locale.ROOT) : "asc";
            if ("asc".equals(direction)) {
                wrapper.orderByAsc(column);
            } else if ("desc".equals(direction)) {
                wrapper.orderByDesc(column);
            } else {
                throw new IllegalArgumentException("Unsupported sort direction: " + direction);
            }
        }
    }

    private static <T> void validateColumns(Class<T> entityClass, Wrapper<T> wrapper) {
        if (wrapper == null || wrapper.getSqlSegment() == null) {
            return;
        }
        allowedColumns(entityClass);
    }

    private static Object filterValue(SearchFilter filter) {
        if (filter.getValue() != null) {
            return filter.getValue();
        }
        return filter.getKeyword();
    }

    private static String requireColumn(Map<String, String> columns, String requestedColumn) {
        String normalized = requestedColumn.trim();
        String column = columns.get(normalized);
        if (column == null) {
            throw new IllegalArgumentException("Unknown field: " + requestedColumn);
        }
        return column;
    }

    private static Map<String, String> allowedColumns(Class<?> entityClass) {
        Map<String, String> columns = new LinkedHashMap<>();
        for (Field field : allFields(entityClass)) {
            String fieldName = field.getName();
            if (fieldName.startsWith("$") || java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            String column = camelToSnake(fieldName);
            columns.put(fieldName, column);
            columns.put(column, column);
        }
        return columns;
    }

    private static List<Field> allFields(Class<?> entityClass) {
        return Arrays.stream(entityClass.getDeclaredFields()).toList();
    }

    private static String camelToSnake(String value) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (Character.isUpperCase(ch)) {
                builder.append('_').append(Character.toLowerCase(ch));
            } else {
                builder.append(ch);
            }
        }
        return builder.toString();
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static void requireUpdated(int updated, String message) {
        if (updated == 0) {
            throw new IllegalArgumentException(message);
        }
    }

    private static Serializable serializableId(Object id) {
        if (id instanceof Serializable serializable) {
            return serializable;
        }
        throw new IllegalArgumentException("id must be Serializable");
    }
}
