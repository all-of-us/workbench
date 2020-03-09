package org.pmiops.workbench.db.model;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import org.pmiops.workbench.model.RdrEntity;

public class RdrEntityEnums {
  private static final BiMap<RdrEntity, Short> CLIENT_TO_STORAGE_ENTITY =
      ImmutableBiMap.<RdrEntity, Short>builder()
          .put(RdrEntity.USER, (short) 1)
          .put(RdrEntity.WORKSPACE, (short) 2)
          .build();

  public static RdrEntity entityFromStorage(Short entity) {
    return CLIENT_TO_STORAGE_ENTITY.inverse().get(entity);
  }

  public static Short entityToStorage(RdrEntity entity) {
    return CLIENT_TO_STORAGE_ENTITY.get(entity);
  }
}
