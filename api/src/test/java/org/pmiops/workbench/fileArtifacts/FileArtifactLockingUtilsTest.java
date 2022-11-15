package org.pmiops.workbench.fileArtifacts;

import static com.google.common.truth.Truth.assertThat;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class FileArtifactLockingUtilsTest {
  private static Stream<Arguments> notebookLockingCases() {
    return Stream.of(
        Arguments.of(
            "fc-bucket-id-1",
            "user@aou",
            "dc5acd54f734a2e2350f2adcb0a25a4d1978b45013b76d6bc0a2d37d035292fe"),
        Arguments.of(
            "fc-bucket-id-1",
            "another-user@aou",
            "bc90f9f740702e5e0408f2ea13fed9457a7ee9c01117820f5c541067064468c3"),
        Arguments.of(
            "fc-bucket-id-2",
            "user@aou",
            "a759e5aef091fd22bbf40bf8ee7cfde4988c668541c18633bd79ab84b274d622"),
        // catches an edge case where the hash has a leading 0
        Arguments.of(
            "fc-5ac6bde3-f225-44ca-ad4d-92eed68df7db",
            "brubenst2@fake-research-aou.org",
            "060c0b2ef2385804b7b69a4b4477dd9661be35db270c940525c2282d081aef56"));
  }

  @ParameterizedTest
  @MethodSource("notebookLockingCases")
  public void testNotebookLockingEmailHash(String bucket, String email, String hash) {
    assertThat(FileArtifactLockingUtils.notebookLockingEmailHash(bucket, email)).isEqualTo(hash);
  }
}
