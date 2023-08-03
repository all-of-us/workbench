import * as React from 'react';

import { AccessModule, Profile } from 'generated/fetch';

import { FlexColumn, FlexRow } from 'app/components/flex';
import { WithSpinnerOverlayProps } from 'app/components/with-spinner-overlay';
import { DARPageMode } from 'app/utils/access-utils';

import { styles } from './data-access-requirements';
import { ModulesForAnnualRenewal } from './modules-for-annual-renewal';
import { ModulesForInitialRegistration } from './modules-for-initial-registration';

const duccModule = AccessModule.DATAUSERCODEOFCONDUCT;
export const DuccCard = (props: {
  profile: Profile;
  focusedModule: AccessModule;
  activeModules: AccessModule[];
  spinnerProps: WithSpinnerOverlayProps;
  pageMode: DARPageMode;
  stepNumber: number;
}) => {
  const {
    profile,
    focusedModule,
    activeModules,
    spinnerProps,
    pageMode,
    stepNumber,
  } = props;
  return (
    <FlexRow style={styles.card}>
      <FlexColumn>
        <div style={styles.cardStep}>Step {stepNumber}</div>
        <div style={styles.cardHeader}>Sign the Code of Conduct</div>
      </FlexColumn>
      {pageMode === DARPageMode.INITIAL_REGISTRATION ? (
        <ModulesForInitialRegistration
          {...{ profile, focusedModule, activeModules, spinnerProps }}
          modules={[duccModule]}
        />
      ) : (
        <ModulesForAnnualRenewal {...{ profile }} modules={[duccModule]} />
      )}
    </FlexRow>
  );
};
