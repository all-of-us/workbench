package org.pmiops.workbench.notebooks;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig
public class NotebookUtilsTest {
  @Test
  public void testAppendFileExtension_newFileAlreadyHasExtension() {
    assertThat(NotebookUtils.appendFileExtensionIfNotExist("from.ipynb", "new.ipynb"))
        .isEqualTo("new.ipynb");
    assertThat(NotebookUtils.appendFileExtensionIfNotExist("from.Rmd", "new.Rmd"))
        .isEqualTo("new.Rmd");
    assertThat(NotebookUtils.appendFileExtensionIfNotExist("from.sas", "new.sas"))
        .isEqualTo("new.sas");
  }

  @Test
  public void testAppendFileExtension_newFileNotHavingExtension() {
    assertThat(NotebookUtils.appendFileExtensionIfNotExist("from.ipynb", "new"))
        .isEqualTo("new.ipynb");
    assertThat(NotebookUtils.appendFileExtensionIfNotExist("from.Rmd", "new")).isEqualTo("new.Rmd");
    assertThat(NotebookUtils.appendFileExtensionIfNotExist("from.sas", "new")).isEqualTo("new.sas");
  }
}
