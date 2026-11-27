package ru.practicum.common.pagination;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Offset-based pagination for API requests where {@code from} is a row offset,
 * not a page number. Unlike PageRequest.of(from / size, size), it supports any
 * non-negative offset, including offsets that are not multiples of the size.
 */
public final class OffsetPageRequest implements Pageable {
    private final long offset;
    private final int size;
    private final Sort sort;

    public OffsetPageRequest(long offset, int size, Sort sort) {
        if (offset < 0 || size < 1) {
            throw new IllegalArgumentException("Invalid offset or page size");
        }
        this.offset = offset;
        this.size = size;
        this.sort = sort == null ? Sort.unsorted() : sort;
    }

    @Override
    public int getPageNumber() {
        return Math.toIntExact(offset / size);
    }

    @Override
    public int getPageSize() {
        return size;
    }

    @Override
    public long getOffset() {
        return offset;
    }

    @Override
    public Sort getSort() {
        return sort;
    }

    @Override
    public Pageable next() {
        return new OffsetPageRequest(Math.addExact(offset, size), size, sort);
    }

    @Override
    public Pageable previousOrFirst() {
        return new OffsetPageRequest(Math.max(0, offset - size), size, sort);
    }

    @Override
    public Pageable first() {
        return new OffsetPageRequest(0, size, sort);
    }

    @Override
    public Pageable withPage(int pageNumber) {
        if (pageNumber < 0) {
            throw new IllegalArgumentException("Page number must not be negative");
        }
        return new OffsetPageRequest((long) pageNumber * size, size, sort);
    }

    @Override
    public boolean hasPrevious() {
        return offset > 0;
    }
}
