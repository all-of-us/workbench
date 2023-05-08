package org.pmiops.workbench;

import static com.google.common.truth.Truth.assertThat;
import static org.pmiops.workbench.leonardo.LeonardoLabelHelper.LEONARDO_LABEL_APP_TYPE;

import java.util.HashMap;
import java.util.Map;
import org.broadinstitute.dsde.workbench.client.leonardo.model.AuditInfo;
import org.broadinstitute.dsde.workbench.client.leonardo.model.DiskStatus;
import org.broadinstitute.dsde.workbench.client.leonardo.model.DiskType;
import org.broadinstitute.dsde.workbench.client.leonardo.model.GetPersistentDiskResponse;
import org.broadinstitute.dsde.workbench.client.leonardo.model.ListPersistentDiskResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.apiclients.leonardo.NewLeonardoMapper;
import org.pmiops.workbench.apiclients.leonardo.NewLeonardoMapperImpl;
import org.pmiops.workbench.model.AppType;
import org.pmiops.workbench.model.Disk;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@Import(NewLeonardoMapperImpl.class)
@SpringJUnitConfig
public class NewLeonardoMapperTest {
  @Autowired private NewLeonardoMapper mapper;
  private AuditInfo auditInfo;

  @BeforeEach
  public void setUp() {
    auditInfo =
        new AuditInfo()
            .createdDate("2022-10-10")
            .creator("bob@gmail.com")
            .dateAccessed("2022-10-10");
  }

  @Test
  public void testToApiDiskFromListDiskResponse() {
    ListPersistentDiskResponse listPersistentDiskResponse =
        new ListPersistentDiskResponse()
            .diskType(DiskType.SSD)
            .auditInfo(auditInfo)
            .status(DiskStatus.READY);

    Disk disk =
        new Disk()
            .diskType(org.pmiops.workbench.model.DiskType.SSD)
            .isGceRuntime(true)
            .creator(auditInfo.getCreator())
            .dateAccessed(auditInfo.getDateAccessed())
            .createdDate(auditInfo.getCreatedDate())
            .status(org.pmiops.workbench.model.DiskStatus.READY);
    assertThat(mapper.toApiListDisksResponse(listPersistentDiskResponse)).isEqualTo(disk);

    // RSTUDIO
    Map<String, String> rstudioLabel = new HashMap<>();
    rstudioLabel.put(LEONARDO_LABEL_APP_TYPE, "rstudio");
    assertThat(mapper.toApiListDisksResponse(listPersistentDiskResponse.labels(rstudioLabel)))
        .isEqualTo(disk.appType(AppType.RSTUDIO).isGceRuntime(false));
  }

  @Test
  public void testToApiDiskFromGetDiskResponse() {
    GetPersistentDiskResponse getPersistentDiskResponse =
        new GetPersistentDiskResponse()
            .diskType(DiskType.SSD)
            .auditInfo(auditInfo)
            .status(DiskStatus.READY);

    Disk disk =
        new Disk()
            .diskType(org.pmiops.workbench.model.DiskType.SSD)
            .isGceRuntime(true)
            .creator(auditInfo.getCreator())
            .dateAccessed(auditInfo.getDateAccessed())
            .createdDate(auditInfo.getCreatedDate())
            .status(org.pmiops.workbench.model.DiskStatus.READY);
    assertThat(mapper.toApiGetDiskResponse(getPersistentDiskResponse)).isEqualTo(disk);

    // RSTUDIO
    Map<String, String> rstudioLabel = new HashMap<>();
    rstudioLabel.put(LEONARDO_LABEL_APP_TYPE, "rstudio");
    assertThat(mapper.toApiGetDiskResponse(getPersistentDiskResponse.labels(rstudioLabel)))
        .isEqualTo(disk.appType(AppType.RSTUDIO).isGceRuntime(false));
  }
}
