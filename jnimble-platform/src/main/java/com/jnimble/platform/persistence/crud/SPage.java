package com.jnimble.platform.persistence.crud;

import java.util.List;

/**
 * Generic paginated result wrapper (Java record).
 *
 * <p>Holds a list of data items for a single page along with the total
 * number of items across all pages, enabling pagination UI rendering.</p>
 *
 * <p>通用分页结果包装器（Java record）。持有单页数据项的列表以及所有页面的总条目数，
 * 支持分页界面渲染。</p>
 *
 * @param <T>   the type of data items in the page
 * @param list  the data items for the current page
 * @param total the total number of items across all pages
 */
public record SPage<T>(List<T> list, long total) {
}
