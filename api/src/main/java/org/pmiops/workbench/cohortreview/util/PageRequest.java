package org.pmiops.workbench.cohortreview.util;

public class PageRequest {
    private int page;
    private int size;
    private SortOrder sortOrder;
    private SortColumn sortColumn;

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
        this.sortOrder = SortOrder.asc;
        this.sortColumn = SortColumn.PARTICIPANT_ID;
    }

    /**
     * Creates a new {@link PageRequest} with sort/column parameters applied.
     *
     * @param page zero-based page index.
     * @param size the size of the page to be returned.
     * @param sortOrder order of the sort.
     * @param sortColumn column to sort.
     */
    public PageRequest(int page, int size, SortOrder sortOrder, SortColumn sortColumn) {
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

    public SortColumn getSortColumn() { return this.sortColumn; }
}
