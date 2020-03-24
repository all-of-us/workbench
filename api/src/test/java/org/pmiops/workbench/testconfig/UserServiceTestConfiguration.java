package org.pmiops.workbench.testconfig;

import org.pmiops.workbench.db.dao.UserServiceImpl;
import org.pmiops.workbench.institution.InstitutionMapperImpl;
import org.pmiops.workbench.institution.InstitutionServiceImpl;
import org.pmiops.workbench.institution.PublicInstitutionDetailsMapperImpl;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;

// Collect the imports necessary for tests to access UserServiceImpl in one place

@TestConfiguration
@Import({
  UserServiceImpl.class,
  // UserServiceImpl depends on InstitutionServiceImpl
  InstitutionServiceImpl.class,
  // InstitutionServiceImpl depends on these 2
  InstitutionMapperImpl.class,
  PublicInstitutionDetailsMapperImpl.class
})
public class UserServiceTestConfiguration {}
