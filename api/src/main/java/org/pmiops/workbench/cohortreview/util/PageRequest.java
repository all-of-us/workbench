package org.pmiops.workbench.cohortreview.util;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.pmiops.workbench.model.ParticipantCohortStatusColumns;
import org.pmiops.workbench.model.SortOrder;

import java.util.Objects;

public class PageRequest {
    private int page;
    private int size;
    private SortOrder sortOrder;
    private ParticipantCohortStatusColumns sortColumn;

    /**
     * Creates a new {@link PageRequest}. Pages are zero indexed.
     *
     * @param page zero-based page index.
     * @param size the size of the page to be returned.
     */
    public PageRequest(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("Page index must not be less than zero!");
        }

        if (size < 1) {
            throw new IllegalArgumentException("Page size must not be less than one!");
        }

        this.page = page;
        this.size = size;
        this.sortOrder = SortOrder.ASC;
        this.sortColumn = ParticipantCohortStatusColumns.PARTICIPANTID;
    }

    /**
     * Creates a new {@link PageRequest} with sort/column parameters applied.
     *
     * @param page zero-based page index.
     * @param size the size of the page to be returned.
     * @param sortOrder order of the sort.
     * @param sortColumn column to sort.
     */
    public PageRequest(int page, int size, SortOrder sortOrder, ParticipantCohortStatusColumns sortColumn) {
        this(page, size);
        this.sortOrder = sortOrder;
        this.sortColumn = sortColumn;
    }

    public int getPageSize() {
        return this.size;
    }

    public int getPageNumber() {
        return this.page;
    }

    public SortOrder getSortOrder() { return this.sortOrder; }

    public ParticipantCohortStatusColumns getSortColumn() { return this.sortColumn; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PageRequest that = (PageRequest) o;
        return page == that.page &&
                size == that.size &&
                sortOrder == that.sortOrder &&
                sortColumn == that.sortColumn;
    }

    @Override
    public int hashCode() {
        return Objects.hash(page, size, sortOrder, sortColumn);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("page", page)
                .append("size", size)
                .append("sortOrder", sortOrder)
                .append("sortColumn", sortColumn)
                .toString();
    }
}
