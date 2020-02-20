package org.pmiops.workbench.db.dao;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import java.sql.Timestamp;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.db.model.CommonStorageEnums;
import org.pmiops.workbench.db.model.DbConceptSet;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.model.Domain;
import org.pmiops.workbench.model.Surveys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class ConceptSetDaoTest {

  @Autowired private ConceptSetDao conceptSetDao;
  @Autowired private UserDao userDao;
  @Autowired private WorkspaceDao workspaceDao;
  private DbConceptSet dbConceptSet;

  @Before
  public void setUp() {
    DbWorkspace ws = new DbWorkspace();
    ws.setVersion(1);
    ws = workspaceDao.save(ws);

    DbUser creator = new DbUser();
    creator.setUsername("brian");
    creator = userDao.save(creator);

    Timestamp now = new Timestamp(System.currentTimeMillis());
    dbConceptSet =
        conceptSetDao.save(
            DbConceptSet.builder()
                .addCreationTime(now)
                .addCreator(creator)
                .addDescription("descr")
                .addDomain(CommonStorageEnums.domainToStorage(Domain.CONDITION))
                .addLastModifiedTime(now)
                .addName("conceptSet")
                .addParticipantCount(200)
                .addSurvey(CommonStorageEnums.surveysToStorage(Surveys.THE_BASICS))
                .addVersion(1)
                .addWorkspaceId(ws.getWorkspaceId())
                .build());
  }

  @Test
  public void findByConceptSetIdAndWorkspaceId() {
    assertThat(
            conceptSetDao
                .findByConceptSetIdAndWorkspaceId(
                    dbConceptSet.getConceptSetId(), dbConceptSet.getWorkspaceId())
                .get())
        .isEqualTo(dbConceptSet);
  }

  @Test
  public void findByWorkspaceId() {
    assertThat(conceptSetDao.findByWorkspaceId(dbConceptSet.getWorkspaceId()).get(0))
        .isEqualTo(dbConceptSet);
  }

  @Test
  public void findByWorkspaceIdAndSurvey() {
    assertThat(
            conceptSetDao
                .findByWorkspaceIdAndSurvey(dbConceptSet.getWorkspaceId(), dbConceptSet.getSurvey())
                .get(0))
        .isEqualTo(dbConceptSet);
  }

  @Test
  public void findConceptSetByNameAndWorkspaceId() {
    assertThat(
            conceptSetDao.findConceptSetByNameAndWorkspaceId(
                dbConceptSet.getName(), dbConceptSet.getWorkspaceId()))
        .isEqualTo(dbConceptSet);
  }

  @Test
  public void findAllByConceptSetIdIn() {
    assertThat(
            conceptSetDao
                .findAllByConceptSetIdIn(ImmutableList.of(dbConceptSet.getConceptSetId()))
                .get(0))
        .isEqualTo(dbConceptSet);
  }
}
