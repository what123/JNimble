package com.jnimble.platform.persistence.crud;

import com.jnimble.platform.persistence.entity.UserEntity;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MapperUtilsTest {

    @Test
    void buildWrapperSupportsEqFilterAndSortWhitelist() {
        SearchFilter filter = new SearchFilter();
        filter.setKey("username");
        filter.setMatch("eq");
        filter.setKeyword("admin");
        BaseSearchCondition condition = new BaseSearchCondition();
        condition.setFilters(List.of(filter));

        String sql = MapperUtils.buildWrapper(UserEntity.class, condition, "created_at desc").getSqlSegment();

        assertThat(sql)
                .contains("username")
                .contains("created_at")
                .contains("DESC");
    }

    @Test
    void buildWrapperRejectsUnknownFilterField() {
        SearchFilter filter = new SearchFilter();
        filter.setKey("unknown_column");
        filter.setMatch("eq");
        filter.setKeyword("admin");
        BaseSearchCondition condition = new BaseSearchCondition();
        condition.setFilters(List.of(filter));

        assertThatThrownBy(() -> MapperUtils.buildWrapper(UserEntity.class, condition, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown field");
    }

    @Test
    void buildWrapperRejectsUnknownSortField() {
        assertThatThrownBy(() -> MapperUtils.buildWrapper(UserEntity.class, null, "unknown_column desc"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown field");
    }

    @Test
    void buildWrapperSupportsRangeFilters() {
        SearchFilter filter = new SearchFilter();
        filter.setKey("created_at");
        filter.setMatch("ge");
        filter.setValue(BigDecimal.ONE);
        BaseSearchCondition condition = new BaseSearchCondition();
        condition.setFilters(List.of(filter));

        assertThat(MapperUtils.buildWrapper(UserEntity.class, condition, null).getSqlSegment())
                .contains("created_at");
    }
}
