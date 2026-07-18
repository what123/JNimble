package com.jnimble.platform.persistence.crud;

/**
 * Generic paginated search request with condition and keywords.
 *
 * <p>Encapsulates pagination parameters (offset, size, sort), a typed search
 * condition, and optional keyword search for flexible querying.</p>
 *
 * <p>通用分页搜索请求，包含条件查询和关键字搜索。封装分页参数（偏移量、每页大小、排序）、
 * 类型化的搜索条件和可选的关键字搜索，支持灵活的查询方式。</p>
 *
 * @param <T> the type of the search condition object
 */
public class PageSearchRequest<T> {

    private PageInfo page;
    private T condition;
    private String keywords;

    /**
     * Gets the pagination information.
     *
     * @return the PageInfo containing offset, size, and sort
     */
    public PageInfo getPage() {
        return page;
    }

    /**
     * Sets the pagination information.
     *
     * @param page the PageInfo to set
     */
    public void setPage(PageInfo page) {
        this.page = page;
    }

    /**
     * Gets the typed search condition.
     *
     * @return the search condition
     */
    public T getCondition() {
        return condition;
    }

    /**
     * Sets the typed search condition.
     *
     * @param condition the search condition to set
     */
    public void setCondition(T condition) {
        this.condition = condition;
    }

    /**
     * Gets the keyword search string.
     *
     * @return the keywords
     */
    public String getKeywords() {
        return keywords;
    }

    /**
     * Sets the keyword search string.
     *
     * @param keywords the keywords to set
     */
    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }

    /**
     * Pagination information including offset, page size, and sort expression.
     *
     * <p>分页信息，包含偏移量、每页大小和排序表达式。</p>
     */
    public static class PageInfo {

        private Integer offset;
        private Integer size;
        private String sort;

        /**
         * Gets the offset (starting position).
         *
         * @return the offset
         */
        public Integer getOffset() {
            return offset;
        }

        /**
         * Sets the offset (starting position).
         *
         * @param offset the offset to set
         */
        public void setOffset(Integer offset) {
            this.offset = offset;
        }

        /**
         * Gets the page size (number of items per page).
         *
         * @return the page size
         */
        public Integer getSize() {
            return size;
        }

        /**
         * Sets the page size (number of items per page).
         *
         * @param size the page size to set
         */
        public void setSize(Integer size) {
            this.size = size;
        }

        /**
         * Gets the sort expression (e.g., "created_at desc").
         *
         * @return the sort expression
         */
        public String getSort() {
            return sort;
        }

        /**
         * Sets the sort expression (e.g., "created_at desc").
         *
         * @param sort the sort expression to set
         */
        public void setSort(String sort) {
            this.sort = sort;
        }
    }
}
