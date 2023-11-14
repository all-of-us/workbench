import * as React from 'react';

import { AccessModule, Profile } from 'generated/fetch';

import { DEFAULT, switchCase } from '@terra-ui-packages/core-utils';
import { ComplianceTrainingModuleCardTitle } from 'app/pages/access/compliance-training-module-card-title';
import { AccessTierShortNames } from 'app/utils/access-tiers';

export interface AARTitleProps {
  moduleName: AccessModule;
  profile: Profile;
}

export const AARTitle = (props: AARTitleProps) => {
  return switchCase<AccessModule, React.ReactElement>(
    props.moduleName,
    [
      AccessModule.COMPLIANCE_TRAINING,
      () => (
        <ComplianceTrainingModuleCardTitle
          tier={AccessTierShortNames.Registered}
          profile={props.profile}
        />
      ),
    ],
    [
      AccessModule.CT_COMPLIANCE_TRAINING,
      () => (
        <ComplianceTrainingModuleCardTitle
          tier={AccessTierShortNames.Controlled}
          profile={props.profile}
        />
      ),
    ],
    [
      AccessModule.DATA_USER_CODE_OF_CONDUCT,
      () => <span>Sign Data User Code of Conduct</span>,
    ],
    [AccessModule.PROFILE_CONFIRMATION, () => <span>Update your profile</span>],
    [
      AccessModule.PUBLICATION_CONFIRMATION,
      () => (
        <span>
          Report any publications or presentations based on your research using
          the Researcher Workbench
        </span>
      ),
    ],
    [DEFAULT, () => null]
  );
};
