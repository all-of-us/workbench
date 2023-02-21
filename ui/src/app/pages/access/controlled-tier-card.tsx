import * as React from 'react';

import { AccessModule, Profile } from 'generated/fetch';

import { Button } from 'app/components/buttons';
import { FlexColumn, FlexRow } from 'app/components/flex';
import {
  CheckCircle,
  ControlledTierBadge,
  MinusCircle,
} from 'app/components/icons';
import { SUPPORT_EMAIL, SupportMailto } from 'app/components/support';
import { WithSpinnerOverlayProps } from 'app/components/with-spinner-overlay';
import colors from 'app/styles/colors';
import {
  AccessTierDisplayNames,
  AccessTierShortNames,
} from 'app/utils/access-tiers';
import {
  DARPageMode,
  getAccessModuleConfig,
  getAccessModuleStatusByName,
  isCompliant,
  redirectToNiH,
} from 'app/utils/access-utils';
import { serverConfigStore } from 'app/utils/stores';
import { getCustomOrDefaultUrl } from 'app/utils/urls';

import { DataDetail, styles } from './data-access-requirements';
import { Module } from './module';
import { ModuleIcon } from './module-icon';
import { ModulesForAnnualRenewal } from './modules-for-annual-renewal';
import { ModulesForInitialRegistration } from './modules-for-initial-registration';

const handleClickSupportButton = (url) => () => {
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

  const moduleName = AccessModule.ERACOMMONS;

  const status = getAccessModuleStatusByName(profile, moduleName);

  // module is not clickable if (user is ineligible for CT) or (user has completed/bypassed module already)
  const clickable = eligible && !isCompliant(status);

  return (
    <Module
      {...{ clickable, eligible, moduleName, profile, spinnerProps, status }}
      active={false}
      moduleAction={redirectToNiH}
    />
  );
};

// Placeholder until CT Training has been updated; see TemporaryRASModule for inspiration
const TemporaryTrainingModule = (props: { profile: Profile }) => {
  const moduleName = AccessModule.CTCOMPLIANCETRAINING;
  const { DARTitleComponent } = getAccessModuleConfig(moduleName);
  return (
    <FlexRow
      data-test-id={`module-${moduleName}`}
      style={{ paddingTop: '1.4em' }}
    >
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
            <b>Temporarily unavailable</b>: Renewal of Controlled Tier training
            will be available in early March 2023. Please email{' '}
            <SupportMailto /> if you have questions.
          </div>
        </FlexColumn>
      </FlexRow>
    </FlexRow>
  );
};

const ControlledTierStep = (props: {
  enabled: boolean;
  text: String;
  style?;
}) => {
  return (
    <FlexRow style={props.style}>
      <FlexRow style={styles.moduleCTA} />
      {/* Since Institution access steps does not require user interaction, will display them as inactive*/}
      <FlexRow style={styles.backgroundModuleBox}>
        <div style={styles.moduleIcon}>
          {props.enabled ? (
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
          <div>{props.text}</div>
        </FlexColumn>
      </FlexRow>
    </FlexRow>
  );
};

export const ControlledTierCard = (props: {
  profile: Profile;
  activeModule: AccessModule;
  clickableModules: AccessModule[];
  reload: Function;
  spinnerProps: WithSpinnerOverlayProps;
  pageMode: DARPageMode;
}) => {
  const { profile, activeModule, clickableModules, spinnerProps, pageMode } =
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

  const { enableComplianceTraining, enableControlledTierTrainingRenewal } =
    serverConfigStore.get().config;

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
                onClick={handleClickSupportButton(institutionRequestAccessUrl)}
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
          data-test-id='controlled-signed'
          enabled={isSigned}
          text={`${institutionDisplayName} must sign an institutional agreement`}
        />
        <ControlledTierStep
          data-test-id='controlled-user-email'
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
              {...{ profile, activeModule, clickableModules, spinnerProps }}
              modules={[ctModule]}
            />
          )}
        {enableComplianceTraining &&
          pageMode === DARPageMode.ANNUAL_RENEWAL &&
          isEligible &&
          (enableControlledTierTrainingRenewal ? (
            <ModulesForAnnualRenewal profile={profile} modules={[ctModule]} />
          ) : (
            <TemporaryTrainingModule {...{ profile }} />
          ))}
      </FlexColumn>
    </FlexRow>
  );
};
