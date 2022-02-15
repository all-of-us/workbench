package org.pmiops.workbench.access;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@Import(AccessModuleNameMapperImpl.class)
@SpringJUnitConfig
public class AccessModuleNameMapperTest {
  @Autowired private AccessModuleNameMapper mapper;

  @Test
  public void test() {
    assertThat(true).isTrue();
  }
}
