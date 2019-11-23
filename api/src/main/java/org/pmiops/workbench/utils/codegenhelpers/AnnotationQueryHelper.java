package org.pmiops.workbench.utils.codegenhelpers;

import java.util.ArrayList;
import org.pmiops.workbench.model.AnnotationQuery;

public class AnnotationQueryHelper implements GeneratedClassHelper<AnnotationQuery> {

  private AnnotationQueryHelper() {
  }

  private  static AnnotationQueryHelper instance;

  public static AnnotationQueryHelper getInstance() {
    if (instance == null) {
      instance = new AnnotationQueryHelper();
    }
    return instance;
  }

  @Override
  public AnnotationQuery create() {
    return sanitize(new AnnotationQuery());
  }

  @Override
  public AnnotationQuery sanitize(AnnotationQuery defaultConstructedInstance) {
    if (defaultConstructedInstance.getColumns() == null) {
      defaultConstructedInstance.setColumns(new ArrayList<>());
    }
    if (defaultConstructedInstance.getOrderBy() == null) {
      defaultConstructedInstance.setOrderBy(new ArrayList<>());
    }
    return defaultConstructedInstance;
  }
}
