package com.jnimble.platform.persistence.crud;

import java.math.BigDecimal;
import java.util.Collection;

/**
 * Search filter specification for dynamic query construction.
 *
 * <p>Defines a single filter condition with a field name, match mode
 * (e.g., "eq", "like", "in", "between"), and value. Supports text matching,
 * range queries via min/max, and multi-value (IN) queries.</p>
 *
 * <p>用于动态查询构建的搜索过滤器规格。定义单个过滤条件，包含字段名、
 * 匹配模式（如 "eq"、"like"、"in"、"between"）和值。支持文本匹配、
 * 通过 min/max 的范围查询和多值（IN）查询。</p>
 */
public class SearchFilter {

    private String key;
    private String keyword;
    private String match;
    private BigDecimal value;
    private Collection<?> values;
    private SearchFilterValue min;
    private SearchFilterValue max;

    /**
     * Gets the field key to filter on.
     *
     * @return the field key
     */
    public String getKey() {
        return key;
    }

    /**
     * Sets the field key to filter on.
     *
     * @param key the field key to set
     */
    public void setKey(String key) {
        this.key = key;
    }

    /**
     * Gets the keyword value for text-based matching.
     *
     * @return the keyword
     */
    public String getKeyword() {
        return keyword;
    }

    /**
     * Sets the keyword value for text-based matching.
     *
     * @param keyword the keyword to set
     */
    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    /**
     * Gets the match mode (e.g., "eq", "like", "in", "between").
     *
     * @return the match mode
     */
    public String getMatch() {
        return match;
    }

    /**
     * Sets the match mode (e.g., "eq", "like", "in", "between").
     *
     * @param match the match mode to set
     */
    public void setMatch(String match) {
        this.match = match;
    }

    /**
     * Gets the value for exact/comparison filters.
     *
     * @return the BigDecimal value
     */
    public BigDecimal getValue() {
        return value;
    }

    /**
     * Sets the value for exact/comparison filters.
     *
     * @param value the BigDecimal value to set
     */
    public void setValue(BigDecimal value) {
        this.value = value;
    }

    /**
     * Gets the collection of values for multi-value (IN) filters.
     *
     * @return the values collection
     */
    public Collection<?> getValues() {
        return values;
    }

    /**
     * Sets the collection of values for multi-value (IN) filters.
     *
     * @param values the values collection to set
     */
    public void setValues(Collection<?> values) {
        this.values = values;
    }

    /**
     * Gets the minimum value for range filters.
     *
     * @return the minimum value
     */
    public SearchFilterValue getMin() {
        return min;
    }

    /**
     * Sets the minimum value for range filters.
     *
     * @param min the minimum value to set
     */
    public void setMin(SearchFilterValue min) {
        this.min = min;
    }

    /**
     * Gets the maximum value for range filters.
     *
     * @return the maximum value
     */
    public SearchFilterValue getMax() {
        return max;
    }

    /**
     * Sets the maximum value for range filters.
     *
     * @param max the maximum value to set
     */
    public void setMax(SearchFilterValue max) {
        this.max = max;
    }
}