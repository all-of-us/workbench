import * as React from 'react';
import { useEffect } from 'react';

import { AccessModule, Profile } from 'generated/fetch';

import { AoU } from 'app/components/text-wrappers';
import { profileApi } from 'app/services/swagger-fetch-clients';
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
  const [useAbsorb, setUseAbsorb] = React.useState(false);
  useEffect(() => {
    profileApi().useAbsorb().then(setUseAbsorb);
  }, []);

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
    useAbsorb &&
    (!isCompliant(userAccessModule, profile.duccSignedVersion) ||
      isExpiringOrExpired(
        userAccessModule.expirationEpochMillis,
        accessModule
      ));

  const { trainingMigrationPauseActive } = serverConfigStore.get().config;

  return (
    <>
      <div>
        Complete <AoU /> {trainingTitle}
      </div>
      {showHelpText && (
        <p>Navigate to "My Courses" and select "{courseTitle}"</p>
      )}
      {trainingMigrationPauseActive && (
        <p style={{ marginTop: '1rem' }}>
          Please note: We are currently migrating all trainings to a new
          platform. You will not be able to access the training until the
          migration is complete. We expect the migration to complete on&nbsp;
          <b>February 5th</b>. We apologize for the inconvenience.
        </p>
      )}
    </>
  );
};
