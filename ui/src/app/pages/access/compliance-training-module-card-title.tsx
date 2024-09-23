import * as React from 'react';

import { AccessModule, Profile } from 'generated/fetch';

import { AoU } from 'app/components/text-wrappers';
import colors from 'app/styles/colors';
import { AccessTierShortNames } from 'app/utils/access-tiers';
import {
  getAccessModuleStatusByName,
  isCompliant,
  isExpiringOrExpired,
} from 'app/utils/access-utils';
import { COMPLIANCE_TRAINIING_OUTAGE_MESSAGE } from 'app/utils/constants';
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
            <p style={{ color: colors.primary }}>
              {COMPLIANCE_TRAINIING_OUTAGE_MESSAGE}
            </p>
          ) : (
            <p>Navigate to "My Courses" and select "{courseTitle}"</p>
          )}
        </>
      )}
    </>
  );
};
