import * as React from 'react';

import { AccessModule, Profile } from 'generated/fetch';

import { AoU } from 'app/components/text-wrappers';
import { AccessTierShortNames } from 'app/utils/access-tiers';
import {
  getAccessModuleStatusByName,
  isCompliant,
  isExpiringOrExpired,
} from 'app/utils/access-utils';

export interface ComplianceTrainingModuleCardProps {
  tier: AccessTierShortNames;
  profile: Profile;
}

export const ComplianceTrainingModuleCardTitle = ({
  tier,
  profile,
}: ComplianceTrainingModuleCardProps) => {
  const { accessModule, trainingTitle, courseTitle } =
    tier === AccessTierShortNames.Registered
      ? {
          accessModule: AccessModule.COMPLIANCE_TRAINING,
          trainingTitle: 'Responsible Conduct of Research Training',
          courseTitle: 'Responsible Conduct of Research',
        }
      : {
          accessModule: AccessModule.CT_COMPLIANCE_TRAINING,
          trainingTitle: 'Controlled Tier training',
          courseTitle: 'Researcher Workbench: Controlled Tier Data',
        };

  const userAccessModule = getAccessModuleStatusByName(profile, accessModule);
  const showHelpText =
    !isCompliant(userAccessModule, profile.duccSignedVersion) ||
    isExpiringOrExpired(userAccessModule.expirationEpochMillis, accessModule);

  return (
    <>
      <div>
        Complete <AoU /> {trainingTitle}
      </div>
      {showHelpText && (
        <p>Navigate to "My Courses" and select "{courseTitle}"</p>
      )}
    </>
  );
};
