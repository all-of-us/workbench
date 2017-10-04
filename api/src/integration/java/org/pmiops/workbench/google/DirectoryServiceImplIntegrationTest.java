package org.pmiops.workbench.google;

import java.io.IOException;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

public class DirectoryServiceImplIntegrationTest {
  private DirectoryServiceImpl service;

  @Before
  public void setup() {
    service = new DirectoryServiceImpl();
  }

  @Test
  public void testDummyUsernameIsNotTaken() {
    assertThat(service.isUsernameTaken("username-that-should-not-exist")).isFalse();
  }

  @Test
  public void testDirectoryServiceUsernameIsTaken() {
    assertThat(service.isUsernameTaken("directory-service")).isTrue();
  }
}
