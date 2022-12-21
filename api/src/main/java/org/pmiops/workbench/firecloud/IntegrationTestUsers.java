package org.pmiops.workbench.firecloud;

// test users for FireCloudIntegrationTest, which need to be accessible outside of a testing context
public class IntegrationTestUsers {
  // RW-8212: This test requires that these two users keep their Terra-ToS compliance statuses
  // integration-test-user (created by gjordan) is non-compliant
  // integration-test-user-with-tos (created by thibault) is compliant
  public static final String NON_COMPLIANT_USER = "integration-test-user@fake-research-aou.org";
  public static final String COMPLIANT_USER =
      "integration-test-user-with-tos@fake-research-aou.org";
  public static final String COMPLIANT_USER_SUBJECT_ID = "265045784107300a16ccd";
}
