package org.pmiops.workbench.cohortreview.util

import java.util.ArrayList
import java.util.Objects
import org.pmiops.workbench.model.Filter
import org.pmiops.workbench.model.SortOrder

/** PageRequest  */
class PageRequest {
    /**
     * the page
     *
     * @return page
     */
    var page: Int? = null
    /**
     * the page size.
     *
     * @return pageSize
     */
    var pageSize: Int? = null
    /**
     * Get sortOrder
     *
     * @return sortOrder
     */
    var sortOrder: SortOrder? = null
    /**
     * sort column
     *
     * @return sortColumn
     */
    var sortColumn: String? = null
    /**
     * Get filters
     *
     * @return filters
     */
    var filters: List<Filter> = ArrayList<Filter>()

    fun page(page: Int?): PageRequest {
        this.page = page
        return this
    }

    fun pageSize(pageSize: Int?): PageRequest {
        this.pageSize = pageSize
        return this
    }

    fun sortOrder(sortOrder: SortOrder): PageRequest {
        this.sortOrder = sortOrder
        return this
    }

    fun sortColumn(sortColumn: String): PageRequest {
        this.sortColumn = sortColumn
        return this
    }

    fun filters(filters: List<Filter>): PageRequest {
        this.filters = filters
        return this
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) {
            return true
        }
        if (o == null || javaClass != o.javaClass) {
            return false
        }
        val pageRequest = o as PageRequest?
        return (this.page == pageRequest!!.page
                && this.pageSize == pageRequest.pageSize
                && this.sortOrder == pageRequest.sortOrder
                && this.sortColumn == pageRequest.sortColumn
                && this.filters == pageRequest.filters)
    }

    override fun hashCode(): Int {
        return Objects.hash(page, pageSize, sortOrder, sortColumn, filters)
    }

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append("class PageRequest {\n")

        sb.append("    page: ").append(toIndentedString(page)).append("\n")
        sb.append("    pageSize: ").append(toIndentedString(pageSize)).append("\n")
        sb.append("    sortOrder: ").append(toIndentedString(sortOrder)).append("\n")
        sb.append("    sortColumn: ").append(toIndentedString(sortColumn)).append("\n")
        sb.append("    filters: ").append(toIndentedString(filters)).append("\n")
        sb.append("}")
        return sb.toString()
    }

    /**
     * Convert the given object to string with each line indented by 4 spaces (except the first line).
     */
    private fun toIndentedString(o: Any?): String {
        return o?.toString()?.replace("\n", "\n    ") ?: "null"
    }
}
