package com.jnimble.platform.persistence.crud;

import java.math.BigDecimal;

/**
 * Value holder for search filter range queries (min/max).
 *
 * <p>Used in {@link SearchFilter} to represent boundary values for
 * "between" and comparison queries, with an equality flag.</p>
 *
 * <p>搜索过滤器范围查询的值持有者。用于 SearchFilter 中表示
 * "between" 和比较查询的边界值，附带等值标志。</p>
 */
public class SearchFilterValue {

    private boolean equal = true;
    private BigDecimal value;

    /**
     * Checks if the comparison includes equality.
     *
     * @return {@code true} if the comparison is inclusive
     */
    public boolean isEqual() {
        return equal;
    }

    /**
     * Sets whether the comparison includes equality.
     *
     * @param equal {@code true} for inclusive comparison
     */
    public void setEqual(boolean equal) {
        this.equal = equal;
    }

    /**
     * Gets the boundary value.
     *
     * @return the BigDecimal value
     */
    public BigDecimal getValue() {
        return value;
    }

    /**
     * Sets the boundary value.
     *
     * @param value the BigDecimal value to set
     */
    public void setValue(BigDecimal value) {
        this.value = value;
    }
}
