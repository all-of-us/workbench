package org.pmiops.workbench.db.dao;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.db.model.DbInstitution;
import org.pmiops.workbench.db.model.DbInstitutionEmailDomain;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class InstitutionEmailDomainDaoTest {
  @Autowired InstitutionDao institutionDao;
  @Autowired InstitutionEmailDomainDao institutionEmailDomainDao;

  @Test
  public void testDao() {
    final DbInstitution testInst =
        institutionDao.save(new DbInstitution("Broad", "The Broad Institute"));
    assertThat(institutionDao.findAll()).hasSize(1);

    final DbInstitution otherInst =
        institutionDao.save(new DbInstitution("NIH", "The National Institutes of Health"));
    assertThat(institutionDao.findAll()).hasSize(2);

    assertThat(institutionEmailDomainDao.findAll()).isEmpty();
    assertThat(institutionEmailDomainDao.findAllByInstitution(testInst)).isEmpty();

    institutionEmailDomainDao.save(new DbInstitutionEmailDomain(otherInst, "N/A"));

    assertThat(institutionEmailDomainDao.findAll()).hasSize(1);
    assertThat(institutionEmailDomainDao.findAllByInstitution(testInst)).isEmpty();

    // we have no domain uniqueness constraint between institutions
    // or even within an institution - so these will be distinct entities
    institutionEmailDomainDao.save(new DbInstitutionEmailDomain(testInst, "N/A"));
    institutionEmailDomainDao.save(new DbInstitutionEmailDomain(testInst, "N/A"));

    assertThat(institutionEmailDomainDao.findAll()).hasSize(3);
    assertThat(institutionEmailDomainDao.findAllByInstitution(testInst)).hasSize(2);

    institutionEmailDomainDao.deleteAllByInstitution(testInst);

    assertThat(institutionEmailDomainDao.findAll()).hasSize(1);
    assertThat(institutionEmailDomainDao.findAllByInstitution(testInst)).isEmpty();
  }
}
