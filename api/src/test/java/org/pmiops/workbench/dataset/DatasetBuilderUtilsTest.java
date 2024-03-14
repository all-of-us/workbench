package org.pmiops.workbench.dataset;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.Test;

public class DatasetBuilderUtilsTest {
  @Test
  public void testSplitWithLineBreaks_MultipleChunks() {
    String input = "1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20";
    String expected = "1,2,3,4,5,\n6,7,8,9,10,\n,11,12,13,14,15,\n16,17,18,19,20";
    assertThat(DatasetBuilderUtils.splitWithLineBreaks(input, 5)).isEqualTo(expected);
  }

  @Test
  public void testSplitWithLineBreaks_SingleChunk() {
    String input = "1,2,3,4,5";
    String expected = "1,2,3,4,5";
    assertThat(DatasetBuilderUtils.splitWithLineBreaks(input, 5)).isEqualTo(expected);
  }

  @Test
  public void testSplitWithLineBreaks_EmptyString() {
    String input = "";
    String expected = "";
    assertThat(DatasetBuilderUtils.splitWithLineBreaks(input, 5)).isEqualTo(expected);
  }

  @Test
  public void testSplitWithLineBreaks_ChunkSizeLargerThanInput() {
    String input = "1,2,3,4,5";
    String expected = "1,2,3,4,5";
    assertThat(DatasetBuilderUtils.splitWithLineBreaks(input, 10)).isEqualTo(expected);
  }
}
