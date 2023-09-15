package org.pmiops.workbench.notebooks;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.pmiops.workbench.exceptions.NotImplementedException;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig
public class NotebookUtilsTest {
  @Test
  public void testAppendFileExtension_newFileAlreadyHasExtension() {
    assertThat(NotebookUtils.appendFileExtensionIfMissing("from.ipynb", "new.ipynb"))
        .isEqualTo("new.ipynb");
    assertThat(NotebookUtils.appendFileExtensionIfMissing("from.Rmd", "new.Rmd"))
        .isEqualTo("new.Rmd");
    assertThat(NotebookUtils.appendFileExtensionIfMissing("from.sas", "new.sas"))
        .isEqualTo("new.sas");
    assertThat(NotebookUtils.appendFileExtensionIfMissing("from.R", "new.R")).isEqualTo("new.R");
  }

  @Test
  public void testAppendFileExtension_newFileNotHavingExtension() {
    assertThat(NotebookUtils.appendFileExtensionIfMissing("from.ipynb", "new"))
        .isEqualTo("new.ipynb");
    assertThat(NotebookUtils.appendFileExtensionIfMissing("from.Rmd", "new")).isEqualTo("new.Rmd");
    assertThat(NotebookUtils.appendFileExtensionIfMissing("from.sas", "new")).isEqualTo("new.sas");
    assertThat(NotebookUtils.appendFileExtensionIfMissing("from.R", "new")).isEqualTo("new.R");
  }

  @Test
  public void testAppendFileExtension_unsupported() {
    // lower case of Rmd which is not a supported file format
    assertThrows(
        NotImplementedException.class,
        () -> NotebookUtils.appendFileExtensionIfMissing("from.rmd", "new"));
  }
}
