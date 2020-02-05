package org.pmiops.workbench.db.dao;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.db.model.DbInstitution;
import org.pmiops.workbench.db.model.DbInstitutionEmailAddress;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DataJpaTest
public class InstitutionEmailAddressDaoTest {
  @Autowired InstitutionDao institutionDao;
  @Autowired InstitutionEmailAddressDao institutionEmailAddressDao;

  @Test
  public void testDao() {
    final DbInstitution testInst =
        institutionDao.save(new DbInstitution("Broad", "The Broad Institute"));
    assertThat(institutionDao.findAll()).hasSize(1);

    final DbInstitution otherInst =
        institutionDao.save(new DbInstitution("NIH", "The National Institutes of Health"));
    assertThat(institutionDao.findAll()).hasSize(2);

    assertThat(institutionEmailAddressDao.findAll()).isEmpty();
    assertThat(institutionEmailAddressDao.findAllByInstitution(testInst)).isEmpty();

    institutionEmailAddressDao.save(new DbInstitutionEmailAddress(otherInst, "N/A"));

    assertThat(institutionEmailAddressDao.findAll()).hasSize(1);
    assertThat(institutionEmailAddressDao.findAllByInstitution(testInst)).isEmpty();

    // we have no domain uniqueness constraint between institutions
    // or even within an institution - so these will be distinct entities
    institutionEmailAddressDao.save(new DbInstitutionEmailAddress(testInst, "N/A"));
    institutionEmailAddressDao.save(new DbInstitutionEmailAddress(testInst, "N/A"));

    assertThat(institutionEmailAddressDao.findAll()).hasSize(3);
    assertThat(institutionEmailAddressDao.findAllByInstitution(testInst)).hasSize(2);

    institutionEmailAddressDao.deleteAllByInstitution(testInst);

    assertThat(institutionEmailAddressDao.findAll()).hasSize(1);
    assertThat(institutionEmailAddressDao.findAllByInstitution(testInst)).isEmpty();
  }
}
