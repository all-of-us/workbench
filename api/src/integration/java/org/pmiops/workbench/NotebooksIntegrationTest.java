package org.pmiops.workbench;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.pmiops.workbench.notebooks.LeonardoNotebooksClient;
import org.pmiops.workbench.notebooks.LeonardoNotebooksClientImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

public class NotebooksIntegrationTest extends BaseIntegrationTest {
  @Autowired private LeonardoNotebooksClient leonardoNotebooksClient;

  @TestConfiguration
  @ComponentScan(basePackageClasses = LeonardoNotebooksClientImpl.class)
  @Import({LeonardoNotebooksClientImpl.class})
  static class Configuration {}

  @Test
  public void testStatus() {
    assertThat(leonardoNotebooksClient.getNotebooksStatus()).isTrue();
  }
}
