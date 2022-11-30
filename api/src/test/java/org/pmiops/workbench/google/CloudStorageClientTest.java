package org.pmiops.workbench.google;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.cloud.storage.Blob;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.model.FileDetail;
import org.pmiops.workbench.notebooks.NotebookUtils;
import org.pmiops.workbench.test.FakeClock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@DataJpaTest
public class CloudStorageClientTest {

  private static final FakeClock CLOCK = new FakeClock(Instant.now(), ZoneId.systemDefault());

  @Autowired private CloudStorageClient cloudStorageClient;

  @TestConfiguration
  @Import({CloudStorageClientImpl.class})
  static class Configuration {

    @Bean
    Clock clock() {
      return CLOCK;
    }
  }

  @Test
  public void testBlobToFileDetail() {
    String notebookPath = NotebookUtils.withNotebookPath("nb1.ipynb");
    Long notebookSize = 500l;
    Long updateTime = Instant.now().getEpochSecond();
    String bucketName = "bucket";

    Blob notebookBlob = mock(Blob.class);
    when(notebookBlob.getName()).thenReturn(notebookPath);
    when(notebookBlob.getSize()).thenReturn(notebookSize);
    when(notebookBlob.getUpdateTime()).thenReturn(updateTime);
    FileDetail actualFileDetail =
        cloudStorageClient.blobToFileDetail(notebookBlob, bucketName, mock(Set.class));

    assertThat(actualFileDetail.getName()).isEqualTo("nb1.ipynb");
    assertThat(actualFileDetail.getPath())
        .isEqualTo("gs://" + bucketName + "/" + notebookBlob.getName());
    assertThat(actualFileDetail.getLastModifiedTime()).isEqualTo(updateTime);
    assertThat(actualFileDetail.getSizeInBytes()).isEqualTo(notebookSize);
  }
}
