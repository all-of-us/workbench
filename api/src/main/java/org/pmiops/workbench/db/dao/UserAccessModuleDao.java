package org.pmiops.workbench.db.dao;

import java.util.List;
import java.util.Optional;
import org.pmiops.workbench.db.model.DbAccessModule;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbUserAccessModule;
import org.pmiops.workbench.exceptions.ConflictException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.repository.CrudRepository;

public interface UserAccessModuleDao extends CrudRepository<DbUserAccessModule, Long> {
  // TODO config?
  int MAX_RETRIES_COUNT = 3;

  Optional<DbUserAccessModule> getByUserAndAccessModule(DbUser user, DbAccessModule accessModule);

  List<DbUserAccessModule> getAllByUser(DbUser user);

  default DbUserAccessModule saveWithRetries(DbUserAccessModule row) {
    int lockingFailureCount = 0;
    while (true) {
      try {
        return save(row);
      } catch (OptimisticLockingFailureException e) {
        if (lockingFailureCount < MAX_RETRIES_COUNT) {
          lockingFailureCount++;
        } else {
          throw new ConflictException(
              String.format(
                  "Could not update DbUserAccessModule row %s after %d locking failures",
                  row.toString(), lockingFailureCount));
        }
      }
    }
  }
}
