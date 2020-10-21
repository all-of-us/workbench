package org.pmiops.workbench.db.dao;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.db.dao.projection.ProjectedReportingInstitution;
import org.pmiops.workbench.db.model.DbInstitution;
import org.pmiops.workbench.model.DuaType;
import org.pmiops.workbench.model.OrganizationType;
import org.pmiops.workbench.testconfig.ReportingTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DataJpaTest
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
public class InstitutionDaoTest {

  @Autowired InstitutionDao institutionDao;

  private DbInstitution testInst;

  @Before
  public void setUp() {
    testInst =
        institutionDao.save(
            new DbInstitution().setShortName("Broad").setDisplayName("The Broad Institute"));
  }

  @Test
  public void test_save() {
    final DbInstitution toSave =
        new DbInstitution()
            .setShortName("VUMC")
            .setDisplayName("Vanderbilt University Medical Center");
    final DbInstitution saved = institutionDao.save(toSave);
    assertThat(saved).isEqualTo(toSave);
  }

  @Test
  public void test_delete() {
    institutionDao.delete(testInst.getInstitutionId());
    DbInstitution dbInstitution = institutionDao.findOne(testInst.getInstitutionId());
    assertThat(dbInstitution).isNull();
    assertThat(institutionDao.findAll()).isEmpty();
  }

  @Test
  public void test_findAll() {
    assertThat(institutionDao.findAll()).containsExactly(testInst);
  }

  @Test
  public void test_findOne() {
    DbInstitution dbInstitution = institutionDao.findOne(testInst.getInstitutionId());
    assertThat(dbInstitution).isEqualTo(testInst);
  }

  @Test
  public void test_findOneByShortName() {
    assertThat(institutionDao.findOneByShortName("Broad")).hasValue(testInst);
    assertThat(institutionDao.findOneByShortName("Verily")).isEmpty();
  }

  @Test
  public void test_findOneByDisplayName() {
    assertThat(institutionDao.findOneByDisplayName("The Broad Institute")).hasValue(testInst);
    assertThat(institutionDao.findOneByDisplayName("The National Institutes of Health")).isEmpty();
  }

  @Test(expected = DataIntegrityViolationException.class)
  public void test_shortNameRequired() {
    final DbInstitution testInst = new DbInstitution();
    testInst.setDisplayName("so long");
    institutionDao.save(testInst);
  }

  @Test(expected = DataIntegrityViolationException.class)
  public void test_displayNameRequired() {
    final DbInstitution testInst = new DbInstitution();
    testInst.setShortName("VUMC");
    institutionDao.save(testInst);
  }

  @Test(expected = DataIntegrityViolationException.class)
  public void test_uniqueShortNameRequired() {
    final DbInstitution snowflake1 =
        new DbInstitution().setShortName("unique?").setDisplayName("We are all individuals");
    institutionDao.save(snowflake1);

    final DbInstitution snowflake2 =
        new DbInstitution().setShortName("unique?").setDisplayName("I'm not");
    institutionDao.save(snowflake2);
  }

  @Test(expected = DataIntegrityViolationException.class)
  public void test_uniqueDisplayNameRequired() {
    final DbInstitution snowflake1 =
        new DbInstitution().setShortName("Inst1").setDisplayName("Not Unique");
    institutionDao.save(snowflake1);

    final DbInstitution snowflake2 =
        new DbInstitution().setShortName("Inst2").setDisplayName("Not Unique");
    institutionDao.save(snowflake2);
  }

  @Test
  public void testGetReportingInstitutions() {
    institutionDao.deleteAll();
    institutionDao.save(ReportingTestUtils.createDbInstitution());

    final List<ProjectedReportingInstitution> institutions =
        institutionDao.getReportingInstitutions();
    assertThat(institutions).hasSize(1);
    ReportingTestUtils.assertInstitutionFields(institutions.get(0));
  }

  @Test
  public void testGetReportingInstitutions_empty() {
    institutionDao.deleteAll();
    assertThat(institutionDao.getReportingInstitutions()).isEmpty();
  }

  @Test
  public void testGetReportingInstitutions_multiple() {
    institutionDao.deleteAll();

    DbInstitution institution1 = ReportingTestUtils.createDbInstitution();
    institution1.setDisplayName("Cairo Consulting");
    institution1.setDuaTypeEnum(DuaType.RESTRICTED);
    institution1.setOrganizationTypeEnum(OrganizationType.OTHER);
    institution1.setOrganizationTypeOtherText("Pyramid Scheme");

    DbInstitution institution2 = new DbInstitution();
    institution2.setShortName("mash");
    institution2.setDisplayName("MASH");
    institution2.setDuaTypeEnum(DuaType.MASTER);
    institution2.setOrganizationTypeEnum(OrganizationType.HEALTH_CENTER_NON_PROFIT);

    institutionDao.save(ImmutableList.of(institution1, institution2));

    final List<ProjectedReportingInstitution> institutions =
        institutionDao.getReportingInstitutions();
    assertThat(institutions).hasSize(2);
    assertThat(institutions.get(0).getDisplayName()).isEqualTo("Cairo Consulting");
    assertThat(institutions.get(0).getDuaTypeEnum()).isEqualTo(DuaType.RESTRICTED);
    assertThat(institutions.get(0).getOrganizationTypeEnum()).isEqualTo(OrganizationType.OTHER);
    assertThat(institutions.get(0).getOrganizationTypeOtherText()).isEqualTo("Pyramid Scheme");

    assertThat(institutions.get(1).getDisplayName()).isEqualTo("MASH");
    assertThat(institutions.get(1).getDuaTypeEnum()).isEqualTo(DuaType.MASTER);
    assertThat(institutions.get(1).getOrganizationTypeEnum())
        .isEqualTo(OrganizationType.HEALTH_CENTER_NON_PROFIT);
    assertThat(institutions.get(1).getDisplayName()).isEqualTo("MASH");
    assertThat(institutions.get(1).getOrganizationTypeOtherText()).isNull();
  }
}
