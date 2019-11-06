package org.pmiops.workbench.conceptset;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.db.dao.ConceptSetDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbConceptSet;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.model.Domain;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DataJpaTest
@Import(LiquibaseAutoConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class ConceptSetServiceTest {

  @Autowired private ConceptSetDao conceptSetDao;

  @Autowired private WorkspaceDao workspaceDao;

  private ConceptSetService conceptSetService;
  private DbWorkspace workspace;

  @Before
  public void setUp() {
    conceptSetService = new ConceptSetService();
    conceptSetService.setConceptSetDao(conceptSetDao);
    workspace = mockWorkspace();
  }

  @Test
  public void testCloneConceptSetWithNoCdrVersionChange() {
    DbConceptSet fromConceptSet = mockConceptSet();
    DbConceptSet copiedConceptSet =
        conceptSetService.cloneConceptSetAndConceptIds(fromConceptSet, workspace, false);
    assertNotNull(copiedConceptSet);
    assertEquals(copiedConceptSet.getConceptIds().size(), 5);
    assertEquals(copiedConceptSet.getWorkspaceId(), workspace.getWorkspaceId());
  }

  private DbConceptSet mockConceptSet() {
    Set conceptIdsSet = Stream.of(1, 2, 3, 4, 5).collect(Collectors.toCollection(HashSet::new));

    DbConceptSet conceptSet = new DbConceptSet();
    conceptSet.setConceptIds(conceptIdsSet);
    conceptSet.setConceptSetId(1);
    conceptSet.setName("Mock Concept Set");
    conceptSet.setDomainEnum(Domain.CONDITION);
    return conceptSet;
  }

  private DbWorkspace mockWorkspace() {
    DbWorkspace workspace = new DbWorkspace();
    workspace.setName("Target DbWorkspace");
    workspace.setWorkspaceId(2);
    workspace = workspaceDao.save(workspace);
    return workspace;
  }
}
