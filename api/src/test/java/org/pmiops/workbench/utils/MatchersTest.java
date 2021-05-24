package org.pmiops.workbench.utils;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;

import com.google.common.collect.ImmutableMap;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
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

  @Test
  public void testReplaceAll() {
    final String input = "food is good for you!   2food3.";
    final Map<Pattern, String> patternToReplacement =
        ImmutableMap.of(
            Pattern.compile("\\bfood\\b"), "exercise",
            Pattern.compile("\\s+"), "_",
            Pattern.compile("\\dfood\\d\\."), "<3");
    final String updated = Matchers.replaceAllInMap(patternToReplacement, input);
    assertThat(updated).contains("exercise_is_good_for_you!_<3");
  }

  @Test
  public void testReplaceAll_parameters() {
    final String input = "AND @after <= TIMESTAMP_MILLIS(CAST(jsonPayload.timestamp AS INT64))";
    final Map<Pattern, String> patternToReplacement =
        ImmutableMap.of(
            Pattern.compile("(?<=\\W)@after\\b"), "TIMESTAMP 'WHENEVER'",
            Pattern.compile("\\bAS\\b"), "AS IF");
    final String updated = Matchers.replaceAllInMap(patternToReplacement, input);
    assertThat(updated)
        .isEqualTo(
            "AND TIMESTAMP 'WHENEVER' <= TIMESTAMP_MILLIS(CAST(jsonPayload.timestamp AS IF INT64))");
  }

  @Test
  public void testReplaceAll_multipleOccurrences() {
    final String input = "And she'll have fun fun fun til her daddy takes the T-bird away.";
    final Map<Pattern, String> patternToReplacement =
        ImmutableMap.of(Pattern.compile("\\bfun\\b"), "cat");
    final String updated = Matchers.replaceAllInMap(patternToReplacement, input);
    assertThat(updated).contains("have cat cat cat");
  }

  @Test
  public void testReplaceAll_emptyMap() {
    final String input = "food is good for you!   2food3.";
    final String updated = Matchers.replaceAllInMap(Collections.emptyMap(), input);
    assertThat(updated).isEqualTo(input);
  }
}
