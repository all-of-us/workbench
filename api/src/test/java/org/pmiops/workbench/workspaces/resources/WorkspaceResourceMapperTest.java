package org.pmiops.workbench.workspaces.resources;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.cohorts.CohortMapperImpl;
import org.pmiops.workbench.cohorts.CohortService;
import org.pmiops.workbench.conceptset.ConceptSetService;
import org.pmiops.workbench.conceptset.mapper.ConceptSetMapperImpl;
import org.pmiops.workbench.dataset.mapper.DataSetMapperImpl;
import org.pmiops.workbench.db.model.DbDataset;
import org.pmiops.workbench.db.model.DbDatasetValue;
import org.pmiops.workbench.db.model.DbStorageEnums;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.model.DataSet;
import org.pmiops.workbench.model.Domain;
import org.pmiops.workbench.model.PrePackagedConceptSetEnum;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.pmiops.workbench.utils.mappers.FirecloudMapperImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

@DataJpaTest
public class WorkspaceResourceMapperTest {
  private DbDataset dbDataset;

  @Autowired private WorkspaceResourceMapper workspaceResourceMapper;

  @TestConfiguration
  @Import({
    FakeClockConfiguration.class,
    WorkspaceResourceMapperImpl.class,
    CommonMappers.class,
    DataSetMapperImpl.class,
    CohortMapperImpl.class,
    ConceptSetMapperImpl.class,
    FirecloudMapperImpl.class,
  })
  @MockBean({FireCloudService.class, CohortService.class, ConceptSetService.class})
  static class Configuration {}

  @BeforeEach
  public void setUp() {
    dbDataset =
        new DbDataset()
            .setVersion(1)
            .setDataSetId(101L)
            .setName("name")
            .setLastModifiedTime(FakeClockConfiguration.NOW)
            .setIncludesAllParticipants(false)
            .setDescription("asdf")
            .setInvalid(false)
            .setWorkspaceId(1L)
            .setPrePackagedConceptSet(
                Collections.singletonList(
                    DbStorageEnums.prePackagedConceptSetsToStorage(PrePackagedConceptSetEnum.NONE)))
            .setValues(
                ImmutableList.of(
                    new DbDatasetValue(
                        DbStorageEnums.domainToStorage(Domain.CONDITION).toString(), "value")));
  }

  @Test
  public void fromDbDataset() {
    ResourceFields got = workspaceResourceMapper.fromDbDataset(dbDataset);
    assertThat(got.getDataSet())
        .isEqualTo(
            new DataSet()
                .id(101L)
                .name("name")
                .etag("\"1\"")
                .includesAllParticipants(false)
                .description("asdf")
                .workspaceId(1L)
                .lastModifiedTime(FakeClockConfiguration.NOW_TIME)
                .prePackagedConceptSet(ImmutableList.of(PrePackagedConceptSetEnum.NONE)));
  }

  @Test
  public void fromDbDatasetNullLastModified() {
    ResourceFields got = workspaceResourceMapper.fromDbDataset(dbDataset.setLastModifiedTime(null));
    assertThat(got.getDataSet().getLastModifiedTime()).isNull();
  }
}
