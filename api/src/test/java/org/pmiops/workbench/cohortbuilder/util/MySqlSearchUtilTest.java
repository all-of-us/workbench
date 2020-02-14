package org.pmiops.workbench.cohortbuilder.util;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class MySqlSearchUtilTest {

  @Test
  public void modifySearchTerm() {
    assertThat(MySqlSearchUtil.modifySearchTerm("brian")).isEqualTo("+brian*");
    assertThat(MySqlSearchUtil.modifySearchTerm("brian free")).isEqualTo("+\"brian\"+free*");
    assertThat(MySqlSearchUtil.modifySearchTerm("001")).isEqualTo("+\"001");
    assertThat(MySqlSearchUtil.modifySearchTerm("001.1")).isEqualTo("+\"001\"");
    assertThat(MySqlSearchUtil.modifySearchTerm("001*")).isEqualTo("+\"001");
    assertThat(MySqlSearchUtil.modifySearchTerm("re -write")).isEqualTo("+write*");
    assertThat(MySqlSearchUtil.modifySearchTerm("at base ball hill"))
        .isEqualTo("+\"base\"+\"ball\"+hill*");
  }
}
