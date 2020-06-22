package org.pmiops.workbench.utils;

import static com.google.common.truth.Truth8.assertThat;

import java.util.Optional;
import java.util.regex.Pattern;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class MatchersTest {

  private static final Pattern SINGLE_GROUP_PATTERN = Pattern.compile("abcd(?<next>[efg])");
  private static final String GROUP_NAME = "next";

  @Test
  public void itMatchesGroup() {
    final Optional<String> nextLetter =
        Matchers.getGroup(SINGLE_GROUP_PATTERN, "abcdf", GROUP_NAME);
    assertThat(nextLetter).hasValue("f");
  }

  @Test
  public void getGroup_noMatch() {
    final Optional<String> nextLetter =
        Matchers.getGroup(SINGLE_GROUP_PATTERN, "abcdk", GROUP_NAME);
    assertThat(nextLetter).isEmpty();
  }

  @Test
  public void testGetGroup_noSuchGroup() {
    final Optional<String> nextLetter = Matchers.getGroup(SINGLE_GROUP_PATTERN, "abcdk", "oops");
    assertThat(nextLetter).isEmpty();
  }

  @Test
  public void testGetGroup_hyphens() {
    final Pattern VM_NAME_PATTERN = Pattern.compile("all-of-us-(?<userid>\\d+)-m");
    final Optional<String> suffix =
        Matchers.getGroup(VM_NAME_PATTERN, "all-of-us-2222-m", "userid");
    assertThat(suffix).hasValue("2222");
  }
}
