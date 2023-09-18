import * as React from 'react';

import { AccessModule, Profile } from 'generated/fetch';

import { Button } from 'app/components/buttons';
import { FlexColumn, FlexRow } from 'app/components/flex';
import {
  CheckCircle,
  ControlledTierBadge,
  MinusCircle,
} from 'app/components/icons';
import { SUPPORT_EMAIL } from 'app/components/support';
import { WithSpinnerOverlayProps } from 'app/components/with-spinner-overlay';
import colors from 'app/styles/colors';
import {
  AccessTierDisplayNames,
  AccessTierShortNames,
} from 'app/utils/access-tiers';
import {
  DARPageMode,
  getAccessModuleStatusByName,
  isCompliant,
  redirectToNiH,
} from 'app/utils/access-utils';
import { serverConfigStore } from 'app/utils/stores';
import { getCustomOrDefaultUrl } from 'app/utils/urls';

import { DataDetail, styles } from './data-access-requirements';
import { Module } from './module';
import { ModulesForAnnualRenewal } from './modules-for-annual-renewal';
import { ModulesForInitialRegistration } from './modules-for-initial-registration';

const handleRequestAccessButton = (url) => () => {
  const adjustedUrl = getCustomOrDefaultUrl(url, `mailto:${SUPPORT_EMAIL}`);
  window.open(adjustedUrl);
};

const ctModule = AccessModule.CTCOMPLIANCETRAINING;

const ControlledTierEraModule = (props: {
  profile: Profile;
  eligible: boolean;
  spinnerProps: WithSpinnerOverlayProps;
}): JSX.Element => {
  const { profile, eligible, spinnerProps } = props;

  const moduleName = AccessModule.ERA_COMMONS;

  const status = getAccessModuleStatusByName(profile, moduleName);

  // module is not clickable if (user is ineligible for CT) or (user has completed/bypassed module already)
  const active = eligible && !isCompliant(status);

  return (
    <Module
      {...{ active, eligible, moduleName, profile, spinnerProps, status }}
      focused={false}
      moduleAction={redirectToNiH}
    />
  );
};

const ControlledTierStep = (props: {
  enabled: boolean;
  text: String;
  description: String;
  style?;
}) => {
  const { enabled, text, description, style } = props;
  return (
    <FlexRow title={description} style={style}>
      <FlexRow style={styles.moduleCTA} />
      {/* Since Institution access steps does not require user interaction, will display them as inactive*/}
      <FlexRow style={styles.backgroundModuleBox}>
        <div style={styles.moduleIcon}>
          {enabled ? (
            <CheckCircle
              data-test-id='eligible'
              style={{ color: colors.success }}
            />
          ) : (
            <MinusCircle
              data-test-id='ineligible'
              style={{ color: colors.disabled }}
            />
          )}
        </div>
        <FlexColumn style={styles.backgroundModuleText}>
          <div>{text}</div>
        </FlexColumn>
      </FlexRow>
    </FlexRow>
  );
};

export const ControlledTierCard = (props: {
  profile: Profile;
  focusedModule: AccessModule;
  activeModules: AccessModule[];
  reload: Function;
  spinnerProps: WithSpinnerOverlayProps;
  pageMode: DARPageMode;
}) => {
  const { profile, focusedModule, activeModules, spinnerProps, pageMode } =
    props;
  const controlledTierEligibility = profile.tierEligibilities.find(
    (tier) => tier.accessTierShortName === AccessTierShortNames.Controlled
  );
  const registeredTierEligibility = profile.tierEligibilities.find(
    (tier) => tier.accessTierShortName === AccessTierShortNames.Registered
  );
  const isSigned = !!controlledTierEligibility;
  const isEligible = isSigned && controlledTierEligibility.eligible;
  const {
    verifiedInstitutionalAffiliation: {
      institutionDisplayName,
      institutionRequestAccessUrl,
    },
  } = profile;
  // Display era in CT if:
  // 1) Institution has signed the CT institution agreement,
  // 2) Registered Tier DOES NOT require era
  // 3) CT Requirement DOES require era
  const displayEraCommons =
    isSigned &&
    !registeredTierEligibility?.eraRequired &&
    controlledTierEligibility.eraRequired;
  const rtDisplayName = AccessTierDisplayNames.Registered;
  const ctDisplayName = AccessTierDisplayNames.Controlled;

  const { enableComplianceTraining } = serverConfigStore.get().config;

  return (
    <FlexRow
      id='controlled-card'
      data-test-id='controlled-card'
      style={styles.card}
    >
      <FlexColumn>
        <div style={styles.cardStep}>Step 2</div>
        <div style={styles.cardHeader}>Additional Data Access</div>
        <FlexRow>
          <ControlledTierBadge />
          <div style={styles.dataHeader}>{ctDisplayName} data -</div>
          <div style={styles.ctDataOptional}>&nbsp;Optional</div>
        </FlexRow>
        {isEligible ? (
          <div data-test-id='eligible-text' style={styles.dataDetails}>
            You are eligible to access {ctDisplayName} data.
          </div>
        ) : (
          <div>
            <div data-test-id='ineligible-text' style={styles.dataDetails}>
              You are not currently eligible; action by {institutionDisplayName}{' '}
              is required.
            </div>
            <div style={styles.requestAccess}>
              <Button
                onClick={handleRequestAccessButton(institutionRequestAccessUrl)}
              >
                Request Access
              </Button>
            </div>
          </div>
        )}
        <div style={styles.dataDetails}>
          In addition to {rtDisplayName} data, the {ctDisplayName} curated
          dataset contains:
        </div>
        <DataDetail icon='genomic' text='Genomic data' />
        <DataDetail icon='additional' text='Additional demographic details' />
      </FlexColumn>
      <FlexColumn style={styles.modulesContainer}>
        <ControlledTierStep
          description='Section describing whether an institutional agreement has been signed for controlled tier access'
          enabled={isSigned}
          text={`${institutionDisplayName} must sign an institutional agreement`}
        />
        <ControlledTierStep
          description='Section describing whether an institution has granted controlled tier access to the current user'
          enabled={isEligible}
          text={`${institutionDisplayName} must allow you to access ${ctDisplayName} data`}
          style={{ marginTop: '1.9em' }}
        />
        {displayEraCommons && (
          <ControlledTierEraModule
            {...{ profile, spinnerProps }}
            eligible={isEligible}
          />
        )}

        {enableComplianceTraining &&
          pageMode === DARPageMode.INITIAL_REGISTRATION && (
            <ModulesForInitialRegistration
              {...{ profile, focusedModule, activeModules, spinnerProps }}
              modules={[ctModule]}
            />
          )}
        {enableComplianceTraining &&
          pageMode === DARPageMode.ANNUAL_RENEWAL &&
          isEligible && (
            <ModulesForAnnualRenewal profile={profile} modules={[ctModule]} />
          )}
      </FlexColumn>
    </FlexRow>
  );
};
