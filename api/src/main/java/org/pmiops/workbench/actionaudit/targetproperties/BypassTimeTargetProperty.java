package org.pmiops.workbench.actionaudit.targetproperties;

import org.jetbrains.annotations.NotNull;

public enum BypassTimeTargetProperty implements SimpleTargetProperty {
  DATA_USER_CODE_OF_CONDUCT("data_use_agreement_bypass_time"),
  RT_COMPLIANCE_TRAINING("compliance_training_time"),
  CT_COMPLIANCE_TRAINING("ct_compliance_training_time"),
  ERA_COMMONS("era_commons_bypass_time"),
  TWO_FACTOR_AUTH("two_factor_auth_bypass_time"),
  PROFILE_CONFIRMATION("profile_confirmation_bypass_time"),
  PUBLICATION_CONFIRMATION("publication_confirmation_bypass_time"),
  IDENTITY("identity_bypass_time"),
  RAS_ID_ME("ras_link_id_me_bypass_time"),
  RAS_LOGIN_GOV("ras_link_login_gov_bypass_time");

  private final String propertyName;

  BypassTimeTargetProperty(String propertyName) {
    this.propertyName = propertyName;
  }

  @NotNull
  @Override
  public String getPropertyName() {
    return propertyName;
  }
}
