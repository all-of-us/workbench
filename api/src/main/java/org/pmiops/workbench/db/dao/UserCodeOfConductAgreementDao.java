package org.pmiops.workbench.db.dao;

import org.pmiops.workbench.db.model.DbUserCodeOfConductAgreement;
import org.springframework.data.repository.CrudRepository;

public interface UserCodeOfConductAgreementDao
    extends CrudRepository<DbUserCodeOfConductAgreement, Long> {}
