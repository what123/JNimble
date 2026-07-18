package com.jnimble.platform.persistence.crud;

import java.util.List;

/**
 * Base search condition with a list of search filters.
 *
 * <p>Provides a filter-based approach to dynamic query construction, where each
 * {@link SearchFilter} specifies a field, operator, and value. Designed to be
 * extended by specific search condition classes.</p>
 *
 * <p>包含搜索过滤器列表的基础搜索条件。提供基于过滤器的动态查询构建方式，
 * 每个 SearchFilter 指定字段、操作符和值。设计为可被特定搜索条件类继承。</p>
 */
public class BaseSearchCondition {

    private List<SearchFilter> filters;

    /**
     * Gets the list of search filters.
     *
     * @return the filter list
     */
    public List<SearchFilter> getFilters() {
        return filters;
    }

    /**
     * Sets the list of search filters.
     *
     * @param filters the filter list to set
     */
    public void setFilters(List<SearchFilter> filters) {
        this.filters = filters;
    }
}
