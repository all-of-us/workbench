package org.pmiops.workbench.db.dao;

import org.pmiops.workbench.db.model.DbUserTermsOfService;
import org.springframework.data.repository.CrudRepository;

public interface UserTermsOfServiceDao extends CrudRepository<DbUserTermsOfService, Long> {}
