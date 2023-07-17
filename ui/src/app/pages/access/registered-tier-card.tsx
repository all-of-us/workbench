import * as React from 'react';

import { AccessModule, Profile } from 'generated/fetch';

import { FlexColumn, FlexRow } from 'app/components/flex';
import { RegisteredTierBadge } from 'app/components/icons';
import { WithSpinnerOverlayProps } from 'app/components/with-spinner-overlay';
import { switchCase } from 'app/utils';
import { AccessTierDisplayNames } from 'app/utils/access-tiers';
import { DARPageMode, getAccessModuleConfig } from 'app/utils/access-utils';
import { serverConfigStore } from 'app/utils/stores';

import {
  DataDetail,
  getEligibleModules,
  initialRtModules,
  renewalRtModules,
  styles,
} from './data-access-requirements';
import { ModuleIcon } from './module-icon';
import { ModulesForAnnualRenewal } from './modules-for-annual-renewal';
import { ModulesForInitialRegistration } from './modules-for-initial-registration';

// Sep 16 hack while we work out some RAS bugs
const TemporaryRASModule = (props: { profile: Profile }) => {
  const moduleName = AccessModule.RASLINKLOGINGOV;
  const { DARTitleComponent } = getAccessModuleConfig(moduleName);
  return (
    <FlexRow data-test-id={`module-${moduleName}`}>
      <FlexRow style={styles.moduleCTA} />
      <FlexRow style={styles.backgroundModuleBox}>
        <ModuleIcon
          {...{ moduleName }}
          completedOrBypassed={false}
          eligible={false}
        />
        <FlexColumn style={styles.backgroundModuleText}>
          <DARTitleComponent profile={props.profile} />
          <div style={{ fontSize: '14px', marginTop: '0.5em' }}>
            <b>Temporarily disabled.</b> Due to technical difficulties, this
            step is disabled. In the future, you'll be prompted to complete
            identity verification to continue using the workbench.
          </div>
        </FlexColumn>
      </FlexRow>
    </FlexRow>
  );
};

const RtDataDetailHeader = (props: { pageMode: DARPageMode }) => {
  return switchCase(
    props.pageMode,
    [
      DARPageMode.INITIAL_REGISTRATION,
      () => (
        <div style={styles.dataDetails}>
          Once registered, you’ll have access to:
        </div>
      ),
    ],
    [
      DARPageMode.ANNUAL_RENEWAL,
      () => (
        <div style={styles.dataDetails}>
          Once renewed, you’ll have access to:
        </div>
      ),
    ]
  );
};

export const RegisteredTierCard = (props: {
  profile: Profile;
  activeModule: AccessModule;
  clickableModules: AccessModule[];
  spinnerProps: WithSpinnerOverlayProps;
  pageMode: DARPageMode;
}) => {
  const { profile, activeModule, clickableModules, spinnerProps, pageMode } =
    props;
  const rtDisplayName = AccessTierDisplayNames.Registered;
  const { enableRasLoginGovLinking } = serverConfigStore.get().config;

  return (
    <FlexRow style={styles.card}>
      <FlexColumn>
        <div style={styles.cardStep}>Step 1</div>
        <div style={styles.cardHeader}>
          {pageMode === DARPageMode.ANNUAL_RENEWAL
            ? 'Basic Data Access'
            : 'Complete Registration'}
        </div>
        <FlexRow>
          <RegisteredTierBadge />
          <div style={styles.dataHeader}>{rtDisplayName} data</div>
        </FlexRow>
        <RtDataDetailHeader pageMode={pageMode} />
        <DataDetail icon='individual' text='Individual (not aggregated) data' />
        <DataDetail icon='identifying' text='Identifying information removed' />
        <DataDetail icon='electronic' text='Electronic health records' />
        <DataDetail icon='survey' text='Survey responses' />
        <DataDetail icon='physical' text='Physical measurements' />
        <DataDetail icon='wearable' text='Wearable devices' />
      </FlexColumn>
      {pageMode === DARPageMode.INITIAL_REGISTRATION ? (
        <ModulesForInitialRegistration
          {...{ profile, activeModule, clickableModules, spinnerProps }}
          modules={getEligibleModules(initialRtModules, profile)}
        >
          {!enableRasLoginGovLinking && <TemporaryRASModule {...{ profile }} />}
        </ModulesForInitialRegistration>
      ) : (
        <ModulesForAnnualRenewal
          {...{ profile }}
          modules={getEligibleModules(renewalRtModules, profile)}
        />
      )}
    </FlexRow>
  );
};
