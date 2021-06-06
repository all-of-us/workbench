package org.pmiops.workbench.conceptset;

import static com.google.common.truth.Truth.assertThat;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.cdr.ConceptBigQueryService;
import org.pmiops.workbench.cohortbuilder.CohortBuilderService;
import org.pmiops.workbench.cohortbuilder.mapper.CohortBuilderMapper;
import org.pmiops.workbench.conceptset.mapper.ConceptSetMapper;
import org.pmiops.workbench.conceptset.mapper.ConceptSetMapperImpl;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbConceptSet;
import org.pmiops.workbench.db.model.DbConceptSetConceptId;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.model.Domain;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;

@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class ConceptSetServiceTest {

  @Autowired WorkspaceDao workspaceDao;
  @Autowired ConceptSetService conceptSetService;
  @Autowired ConceptSetMapper conceptSetMapper;

  @TestConfiguration
  @Import({FakeClockConfiguration.class, ConceptSetService.class, ConceptSetMapperImpl.class})
  @MockBean({
    CommonMappers.class,
    CohortBuilderMapper.class,
    ConceptBigQueryService.class,
    CohortBuilderService.class,
    DbUser.class
  })
  static class Configuration {}

  @Test
  public void testCloneConceptSetWithNoCdrVersionChange() {
    DbWorkspace mockDbWorkspace = mockWorkspace();
    DbConceptSet fromConceptSet = mockConceptSet();
    DbConceptSet copiedConceptSet =
        conceptSetService.cloneConceptSetAndConceptIds(fromConceptSet, mockDbWorkspace);
    assertThat(copiedConceptSet).isNotNull();
    assertThat(copiedConceptSet.getConceptSetConceptIds().size()).isEqualTo(5);
    assertThat(copiedConceptSet.getWorkspaceId()).isEqualTo(mockDbWorkspace.getWorkspaceId());
    assertThat(copiedConceptSet).isNotNull();
  }

  private DbConceptSet mockConceptSet() {
    Set<DbConceptSetConceptId> dbConceptSetConceptIds =
        Stream.of(1L, 2L, 3L, 4L, 5L)
            .map(id -> DbConceptSetConceptId.builder().addConceptId(id).addStandard(true).build())
            .collect(Collectors.toSet());

    DbConceptSet conceptSet = new DbConceptSet();
    conceptSet.setConceptSetConceptIds(dbConceptSetConceptIds);
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
