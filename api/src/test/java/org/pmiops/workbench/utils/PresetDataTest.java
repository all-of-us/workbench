package org.pmiops.workbench.utils;

import org.junit.jupiter.api.Test;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.config.CommonConfig;
import org.pmiops.workbench.db.dao.InstitutionDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.VerifiedInstitutionalAffiliationDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@Import({FakeClockConfiguration.class, CommonConfig.class})
@DataJpaTest
public class PresetDataTest {
  @Autowired InstitutionDao institutionDao;
  @Autowired private VerifiedInstitutionalAffiliationDao verifiedInstitutionalAffiliationDao;
  @Autowired private UserDao userDao;

  @Test
  public void testAllPresetsCanBeSavedWithMinimalArguments() {
    var user = userDao.save(PresetData.createDbUser());
    var institution = institutionDao.save(PresetData.createDbInstitution());
    verifiedInstitutionalAffiliationDao.save(
        PresetData.createDbVerifiedInstitutionalAffiliation(institution, user));
  }
}
