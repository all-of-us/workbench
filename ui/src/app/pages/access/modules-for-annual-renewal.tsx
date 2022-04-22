import * as React from 'react';

import { AccessModule, Profile } from 'generated/fetch';

import { FlexColumn } from 'app/components/flex';
import { getAccessModuleStatusByNameOrEmpty } from 'app/utils/access-utils';

import { RenewalCardBody } from './access-renewal';
import { styles } from './data-access-requirements';

interface RenewalCardProps {
  profile: Profile;
  modules: AccessModule[];
}

export const ModulesForAnnualRenewal = (props: RenewalCardProps) => {
  const { profile, modules } = props;
  const showingStyle = {
    ...styles.clickableModuleBox,
    ...styles.clickableModuleText,
  };
  const hiddenStyle = {
    ...styles.backgroundModuleBox,
    ...styles.backgroundModuleText,
  };
  return (
    <FlexColumn style={styles.modulesContainer}>
      {modules.map((moduleName) => {
        // TODO RW-7797.  Until then, hardcode
        const showModule = true;
        return (
          <FlexColumn
            data-test-id={`module-${moduleName}`}
            style={showModule ? showingStyle : hiddenStyle}
          >
            <RenewalCardBody
              moduleStatus={getAccessModuleStatusByNameOrEmpty(
                profile.accessModules.modules,
                moduleName
              )}
              setLoading={() => {}}
              textStyle={{ fontSize: '0.6rem' }}
              hide={!showModule}
              showTimeEstimate={true}
            />
          </FlexColumn>
        );
      })}
    </FlexColumn>
  );
};
