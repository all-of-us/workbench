package org.pmiops.workbench.google;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

public class DirectoryServiceImplTest {
  private DirectoryServiceImpl service;

  @Before
  public void setup() {
    service = new DirectoryServiceImpl();
  }

  @Test
  public void testTrueIsTrue() {
    assertThat(service.getTrue()).isTrue();
  }
}
