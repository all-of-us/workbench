package org.pmiops.workbench.db.dao;

import java.util.Optional;
import org.pmiops.workbench.db.model.DbUserTermsOfService;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.springframework.data.repository.CrudRepository;

public interface UserTermsOfServiceDao extends CrudRepository<DbUserTermsOfService, Long> {
  Optional<DbUserTermsOfService> findFirstByUserIdOrderByTosVersionDesc(long userId);

  default DbUserTermsOfService findByUserIdOrThrow(long userId) {
    return findFirstByUserIdOrderByTosVersionDesc(userId)
        .orElseThrow(
            () ->
                new BadRequestException(
                    String.format(
                        "No Terms of Service acceptance recorded for user ID %d", userId)));
  }
}
