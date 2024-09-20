import * as React from 'react';

import { AccessModule, Profile } from 'generated/fetch';

import { AoU } from 'app/components/text-wrappers';
import { AccessTierShortNames } from 'app/utils/access-tiers';
import {
  getAccessModuleStatusByName,
  isCompliant,
  isExpiringOrExpired,
} from 'app/utils/access-utils';
import { serverConfigStore } from 'app/utils/stores';

export interface ComplianceTrainingModuleCardProps {
  tier: AccessTierShortNames;
  profile: Profile;
}

export const ComplianceTrainingModuleCardTitle = ({
  tier,
  profile,
}: ComplianceTrainingModuleCardProps) => {
  const { blockComplianceTraining } = serverConfigStore.get().config;
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
        <>
          {blockComplianceTraining ? (
            <p>
              Our training system is conducting scheduled maintenance from
              October 9th through October 11th. Please return after our
              maintenance window in order to complete your training.
            </p>
          ) : (
            <p>Navigate to "My Courses" and select "{courseTitle}"</p>
          )}
        </>
      )}
    </>
  );
};
