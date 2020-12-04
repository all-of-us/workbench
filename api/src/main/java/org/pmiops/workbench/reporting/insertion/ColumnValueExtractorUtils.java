package org.pmiops.workbench.reporting.insertion;

public class ColumnValueExtractorUtils {

  /**
   * Poor man's static enum method or extension method to allow grabbing a table property without
   * referring directly to a column value or index. This is necessary because enum classes don't
   * support static methods, and neither do interfaces.
   *
   * @param columnValueExtractor
   * @param <E>
   * @return
   */
  public static <E extends Enum<E> & ColumnValueExtractor<?>> String getBigQueryTableName(
      Class<E> columnValueExtractor) {
    return columnValueExtractor.getEnumConstants()[0].getBigQueryTableName();
  }
}
