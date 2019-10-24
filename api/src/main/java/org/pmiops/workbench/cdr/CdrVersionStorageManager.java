package org.pmiops.workbench.cdr;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.model.CdrVersionEntity;
import org.pmiops.workbench.db.model.CommonStorageEnums;
import org.pmiops.workbench.model.DataAccessLevel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CdrVersionStorageManager {

  private CdrVersionDao cdrVersionDao;

  // todo: move this to new enum
  private static final ImmutableSet<Short> REGISTERED_ONLY =
      ImmutableSet.of(CommonStorageEnums.dataAccessLevelToStorage(DataAccessLevel.REGISTERED));

  private static final ImmutableSet<Short> REGISTERED_AND_PROTECTED =
      ImmutableSet.of(
          CommonStorageEnums.dataAccessLevelToStorage(DataAccessLevel.REGISTERED),
          CommonStorageEnums.dataAccessLevelToStorage(DataAccessLevel.PROTECTED));

  private static final ImmutableMap<DataAccessLevel, ImmutableSet<Short>> DATA_ACCESS_LEVEL_TO_VISIBLE_VALUES =
      ImmutableMap.<DataAccessLevel, ImmutableSet<Short>>builder()
          .put(DataAccessLevel.REGISTERED, REGISTERED_ONLY)
          .put(DataAccessLevel.PROTECTED, REGISTERED_AND_PROTECTED)
          .build();

  @Autowired
  public CdrVersionStorageManager(CdrVersionDao cdrVersionDao) {
    this.cdrVersionDao = cdrVersionDao;
  }

  public List<ImmutableCdrVersion> getByVisibleValues(DataAccessLevel dataAccessLevel) {
    return getByVisibleValues(getVisibleValues(dataAccessLevel));
  }

  public static Set<Short> getVisibleValues(DataAccessLevel dataAccessLevel) {
    return Optional.ofNullable(DATA_ACCESS_LEVEL_TO_VISIBLE_VALUES.get(dataAccessLevel))
    .orElse(ImmutableSet.of());
  }

  public List<ImmutableCdrVersion> getByVisibleValues(Set<Short> visibleValues) {
    final List<CdrVersionEntity> entities = cdrVersionDao
        .findByDataAccessLevelInOrderByCreationTimeDescDataAccessLevelDesc(
        visibleValues);
    return entities.stream()
        .map(ImmutableCdrVersion::fromEntity)
        .collect(Collectors.toList());
  }
}
