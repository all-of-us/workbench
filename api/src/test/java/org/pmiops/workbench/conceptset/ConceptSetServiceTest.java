package org.pmiops.workbench.conceptset;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.time.Clock;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.cdr.ConceptBigQueryService;
import org.pmiops.workbench.cohortbuilder.CohortBuilderService;
import org.pmiops.workbench.cohortbuilder.mapper.CohortBuilderMapper;
import org.pmiops.workbench.concept.ConceptService;
import org.pmiops.workbench.conceptset.mapper.ConceptSetMapper;
import org.pmiops.workbench.conceptset.mapper.ConceptSetMapperImpl;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbConceptSet;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.model.ConceptSetConceptId;
import org.pmiops.workbench.model.Domain;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class ConceptSetServiceTest {

  @Autowired WorkspaceDao workspaceDao;
  @Autowired ConceptSetService conceptSetService;
  @Autowired ConceptService conceptService;
  @Autowired ConceptSetMapper conceptSetMapper;

  @TestConfiguration
  @Import({ConceptSetService.class, ConceptService.class, ConceptSetMapperImpl.class})
  @MockBean({
    CommonMappers.class,
    CohortBuilderMapper.class,
    ConceptBigQueryService.class,
    Clock.class,
    CohortBuilderService.class
  })
  static class Configuration {}

  @Test
  public void testCloneConceptSetWithNoCdrVersionChange() {
    DbWorkspace mockDbWorkspace = mockWorkspace();
    DbConceptSet fromConceptSet = mockConceptSet();
    DbConceptSet copiedConceptSet =
        conceptSetService.cloneConceptSetAndConceptIds(fromConceptSet, mockDbWorkspace, false);
    assertNotNull(copiedConceptSet);
    assertEquals(copiedConceptSet.getConceptSetConceptIds().size(), 5);
    assertEquals(copiedConceptSet.getWorkspaceId(), mockDbWorkspace.getWorkspaceId());
  }

  private DbConceptSet mockConceptSet() {
    ConceptSetConceptId conceptSetConceptId1 =
        new ConceptSetConceptId().conceptId(1L).standard(true);
    ConceptSetConceptId conceptSetConceptId2 =
        new ConceptSetConceptId().conceptId(2L).standard(true);
    ConceptSetConceptId conceptSetConceptId3 =
        new ConceptSetConceptId().conceptId(3L).standard(true);
    ConceptSetConceptId conceptSetConceptId4 =
        new ConceptSetConceptId().conceptId(4L).standard(true);
    ConceptSetConceptId conceptSetConceptId5 =
        new ConceptSetConceptId().conceptId(5L).standard(true);
    Set conceptSetConceptIds =
        Stream.of(
                conceptSetConceptId1,
                conceptSetConceptId2,
                conceptSetConceptId3,
                conceptSetConceptId4,
                conceptSetConceptId5)
            .collect(Collectors.toCollection(HashSet::new));

    DbConceptSet conceptSet = new DbConceptSet();
    conceptSet.setConceptSetConceptIds(conceptSetConceptIds);
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
