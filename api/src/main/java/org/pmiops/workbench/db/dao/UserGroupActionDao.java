package org.pmiops.workbench.db.dao;

import java.util.List;
import org.pmiops.workbench.db.model.DbUserGroupAction;
import org.springframework.data.repository.CrudRepository;

public interface UserGroupActionDao extends CrudRepository<DbUserGroupAction, Long> {
  DbUserGroupAction findFirstByInstitutionIdAndUserGroupActionStatusOrderByAddedTime(
      long institutionId, String status);

  List<DbUserGroupAction> findByUserEmailAndGroupNameAndUserGroupActionStatus(
      String userEmail, String groupName, String status);

  List<DbUserGroupAction> findByUserEmailAndGroupNameAndInstitutionIdAndUserGroupActionStatus(
      String userEmail, String groupName, long institutionId, String status);
}
