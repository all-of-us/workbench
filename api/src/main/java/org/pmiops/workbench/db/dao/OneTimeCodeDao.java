package org.pmiops.workbench.db.dao;

import java.util.Optional;
import java.util.UUID;
import org.pmiops.workbench.db.model.DbOneTimeCode;
import org.springframework.data.repository.CrudRepository;

public interface OneTimeCodeDao extends CrudRepository<DbOneTimeCode, UUID> {
  default Optional<DbOneTimeCode> findByStringId(String id) {
    UUID uuid;
    try {
      uuid = UUID.fromString(id);
    } catch (IllegalArgumentException e) {
      return Optional.empty();
    }
    return this.findById(uuid);
  }
}
