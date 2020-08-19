package org.pmiops.workbench.reporting.insertion;

public interface ColumnDrivenBuilder<T> {
  // Extending classes need only provide an  array of QueryParameterColumns, such
  // as an enum class's values() array.
  QueryParameterColumn<T>[] getQueryParameterColumns();
}
