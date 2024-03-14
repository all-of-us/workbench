package org.pmiops.workbench.utils;

import static com.google.common.truth.Truth.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

public class WorkbenchStringUtilsTest {

  @Test
  public void testSplitByLength_ShorterThanLimit() {
    String line = "a,b";
    int limit = 5;
    String tokenSeparator = ",";
    assertThat(WorkbenchStringUtils.splitByLength(line, limit, tokenSeparator))
        .isEqualTo(List.of(line));
  }

  @Test
  public void testSplitByLength_EqualToLimit() {
    String line = "a, b, c";
    int limit = 7;
    String tokenSeparator = ", ";
    assertThat(WorkbenchStringUtils.splitByLength(line, limit, tokenSeparator))
        .isEqualTo(List.of(line));
  }

  @Test
  public void testSplitByLength_LongerThanLimit() {
    String line = "a, b, c";
    int limit = 6;
    String tokenSeparator = ", ";
    assertThat(WorkbenchStringUtils.splitByLength(line, limit, tokenSeparator))
        .isEqualTo(List.of("a, b, ", "c"));
  }

  @Test
  public void testSplitByLength_LongerThanSmallerLimit() {
    String line = "a, b, c";
    int limit = 3;
    String tokenSeparator = ", ";
    assertThat(WorkbenchStringUtils.splitByLength(line, limit, tokenSeparator))
        .isEqualTo(List.of("a, ", "b, ", "c"));
  }

  @Test
  public void testSplitByLength_LongerThanLimit_cantSplit() {
    String line = "abcdefg";
    int limit = 6;
    assertThat(line.length()).isGreaterThan(limit); // sanity check

    String tokenSeparator = ",";
    assertThat(WorkbenchStringUtils.splitByLength(line, limit, tokenSeparator))
        .isEqualTo(List.of(line));
  }

  @Test
  public void testSplitByLength_NoSeparator() {
    String line = "abcdefghij";
    int limit = 5;
    String tokenSeparator = ",";
    assertThat(WorkbenchStringUtils.splitByLength(line, limit, tokenSeparator))
        .isEqualTo(List.of(line));
  }

  @Test
  public void testSplitByLength_TrailingSeparator_ShorterThanLimit() {
    String line = "abcdefghij,";
    int limit = 50;
    String tokenSeparator = ",";
    assertThat(WorkbenchStringUtils.splitByLength(line, limit, tokenSeparator))
        .isEqualTo(List.of(line));
  }

  @Test
  public void testSplitByLength_TrailingSeparator_LongerThanLimit() {
    String line = "abcdefghij,";
    int limit = 5;
    String tokenSeparator = ",";
    assertThat(WorkbenchStringUtils.splitByLength(line, limit, tokenSeparator))
        .isEqualTo(List.of(line));
  }

  @Test
  public void testSplitTooLongLines_ShorterThanLimit() {
    String input =
        """
          SELECT * FROM table
          WHERE column = 'value';
          """;
    int limit = 100;
    String tokenSeparator = ",";
    String lineSeparator = System.lineSeparator();
    String result =
        WorkbenchStringUtils.splitTooLongLines(input, limit, tokenSeparator, lineSeparator);
    assertThat(result.trim()).isEqualTo(input.trim());
  }

  @Test
  public void testSplitTooLongLines_EqualsLimit() {
    String input =
        """
          SELECT * FROM table
          WHERE column = 'value';
          """;
    int limit = 23; // length of longest line in input text
    String tokenSeparator = ",";
    String lineSeparator = System.lineSeparator();
    String result =
        WorkbenchStringUtils.splitTooLongLines(input, limit, tokenSeparator, lineSeparator);
    assertThat(result.trim()).isEqualTo(input.trim());
  }

  @Test
  public void testSplitTooLongLines_LongerThanLimit_cantSplit() {
    String input =
        """
          SELECT * FROM table
          WHERE column = 'value';
          """;
    int limit = 5;
    String tokenSeparator = ","; // no commas in input
    String lineSeparator = System.lineSeparator();
    String result =
        WorkbenchStringUtils.splitTooLongLines(input, limit, tokenSeparator, lineSeparator);
    assertThat(result.trim()).isEqualTo(input.trim());
  }

  @Test
  public void testSplitTooLongLines_LongerThanLimit() {
    String input =
        """
          SELECT * FROM table
          WHERE id IN (1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
          """;
    int limit = 30;
    String tokenSeparator = ", ";
    String lineSeparator = System.lineSeparator();

    // a trailing space is expected on the split line here because the tokenSeparator is ", "
    String expected =
        """
          SELECT * FROM table
          WHERE id IN (1, 2, 3, 4, 5, 6,\s
          7, 8, 9, 10);
          """;

    String result =
        WorkbenchStringUtils.splitTooLongLines(input, limit, tokenSeparator, lineSeparator);
    assertThat(result.trim()).isEqualTo(expected.trim());
  }
}
