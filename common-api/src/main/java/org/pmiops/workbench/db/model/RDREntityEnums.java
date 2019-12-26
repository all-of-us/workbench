package main.java.org.pmiops.workbench.db.model;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import org.pmiops.workbench.model.RDREntity;

public class RDREntityEnums {
  private static final BiMap<RDREntity, Short> CLIENT_TO_STORAGE_ENTITY =
      ImmutableBiMap.<RDREntity, Short>builder()
          .put(RDREntity.USER, (short) 1)
          .put(RDREntity.WORKSPACE, (short) 2)
          .build();

  public static RDREntity entityFromStorage(Short entity) {
    return CLIENT_TO_STORAGE_ENTITY.inverse().get(entity);
  }

  public static Short entityToStorage(RDREntity entity) {
    return CLIENT_TO_STORAGE_ENTITY.get(entity);
  }
}
