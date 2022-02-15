package org.pmiops.workbench.access;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.Test;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@Import({FakeClockConfiguration.class, UserAccessModuleMapperImpl.class, CommonMappers.class})
@SpringJUnitConfig
public class AccessModuleMapperTest {
  @Autowired private AccessModuleMapper mapper;

    @Test
    public void test() {
      assertThat(true).isTrue();
    }
}
