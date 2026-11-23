package ru.practicum.mainservice.comments.utils;

import org.junit.jupiter.api.Test;
import ru.practicum.common.pagination.OffsetPageRequest;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OffsetPageRequestTest {
    @Test
    void supportsNonAlignedOffsetsAndNavigation() {
        OffsetPageRequest request = new OffsetPageRequest(15, 10, Sort.by("createdAt"));
        assertThat(request.getOffset()).isEqualTo(15L);
        assertThat(request.next().getOffset()).isEqualTo(25L);
        assertThat(request.previousOrFirst().getOffset()).isEqualTo(5L);
        assertThat(request.first().getOffset()).isEqualTo(0L);
        assertThat(request.withPage(3).getOffset()).isEqualTo(30L);
        assertThat(request.getSort().getOrderFor("createdAt")).isNotNull();
    }

    @Test
    void rejectsInvalidPaging() {
        assertThatThrownBy(() -> new OffsetPageRequest(-1, 10, Sort.unsorted()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new OffsetPageRequest(0, 0, Sort.unsorted()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
