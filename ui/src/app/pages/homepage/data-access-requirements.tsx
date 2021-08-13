import * as fp from 'lodash/fp';
import * as React from 'react';

import {FadeBox} from 'app/components/containers';
import {FlexColumn, FlexRow} from 'app/components/flex';
import {Header} from 'app/components/headers';
import {AoU} from 'app/components/text-wrappers';
import {withProfileErrorModal} from 'app/components/with-error-modal';
import {WithSpinnerOverlayProps} from 'app/components/with-spinner-overlay';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {reactStyles} from 'app/utils';
import {getAccessModuleStatusByName} from 'app/utils/access-utils';
import {profileStore, useStore} from 'app/utils/stores';
import {AccessModule} from 'generated/fetch';
import {useEffect} from 'react';

const styles = reactStyles({
  headerFlexColumn: {
    marginLeft: '3%',
    width: '50%',
  },
  headerRW: {
    textTransform: 'uppercase',
    margin: '1em 0 0 0',
  },
  headerDAR: {
    height: '30px',
    width: '302px',
    fontFamily: 'Montserrat',
    fontSize: '22px',
    fontWeight: 500,
    letterSpacing: 0,
    margin: '0.5em 0 0 0',
  },
  pageWrapper: {
    marginLeft: '-1rem',
    marginRight: '-0.6rem',
    justifyContent: 'space-between',
    fontSize: '1.2em',
  },
  fadeBox: {
    margin: '0.5rem 0 0 3%',
    width: '95%',
    padding: '0 0.1rem',
  },
  pleaseComplete: {
    fontSize: '14px',
    color: colors.primary,
  },
  stepCard: {
    height: '375px',
    width: '1195px',
    borderRadius: '0.4rem',
    marginTop: '0.7rem',
    marginBottom: '1.7rem',
    color: colors.primary,
    backgroundColor: colorWithWhiteness(colors.accent, 0.9),
    padding: '1em',
    fontWeight: 500,
  },
  stepCardStep: {
    height: '19px',
    marginBottom: '0.5em',
  },
  stepCardHeader: {
    fontSize: '24px',
    fontWeight: 600,
    letterSpacing: 0,
    lineHeight: '22px',
    marginBottom: '0.5em',
  },
  rtData: {
    fontSize: '16px',
    fontWeight: 600,
    marginBottom: '0.5em',
  },
  rtDataDetails: {
    fontSize: '14px',
    fontWeight: 100,
    marginBottom: '0.5em',
  }
});

const DARHeader = () => <FlexColumn style={styles.headerFlexColumn}>
    <Header style={styles.headerRW}>Researcher Workbench</Header>
    <Header style={styles.headerDAR}>Data Access Requirements</Header>
  </FlexColumn>;

const RegisteredTierCard = () => {
  // in display order
  const rtModules = [
    AccessModule.TWOFACTORAUTH,
    AccessModule.RASLINKLOGINGOV,
    AccessModule.ERACOMMONS,
    AccessModule.COMPLIANCETRAINING,
  ];

  const {profile} = useStore(profileStore);

  return <FlexColumn style={styles.stepCard}>
    <div style={styles.stepCardStep}>Step 1</div>
    <FlexRow>
      <FlexColumn>
        <div style={styles.stepCardHeader}>Complete Registration</div>
        <div style={styles.rtData}>Registered Tier data</div>
        <div style={styles.rtDataDetails}>Once registered, you’ll have access to:</div>
        <FlexRow style={styles.rtDataDetails}>Individual (not aggregated) data</FlexRow>
        <FlexRow style={styles.rtDataDetails}>Identifying information removed</FlexRow>
        <FlexRow style={styles.rtDataDetails}>Electronic health records</FlexRow>
        <FlexRow style={styles.rtDataDetails}>Survey responses</FlexRow>
        <FlexRow style={styles.rtDataDetails}>Physical measurements</FlexRow>
        <FlexRow style={styles.rtDataDetails}>Wearable devices</FlexRow>
      </FlexColumn>
      <FlexColumn style={{padding: '1em'}}>
        <div>Not the real RT tasks UI (TODO) but some output for debugging</div>
        {rtModules.map(module => {
          const {completionEpochMillis, bypassEpochMillis} = getAccessModuleStatusByName(profile, module);
          return <React.Fragment>
            <hr/>
            <FlexRow>
              <div>{module}</div>
              <FlexColumn style={{paddingLeft: '1em'}}>
                <div>Completion Time {completionEpochMillis || '(none)'}</div>
                <div>Bypass Time {bypassEpochMillis || '(none)'}</div>
              </FlexColumn>
            </FlexRow>
          </React.Fragment>; })}
      </FlexColumn>
    </FlexRow>
  </FlexColumn>;
};

const DuccCard = () => <FlexColumn style={styles.stepCard}>
  {/* This will be Step 3 when CT becomes the new Step 2 */}
  <div style={styles.stepCardStep}>Step 2</div>
  <FlexRow>
    <div style={styles.stepCardHeader}>Sign the Code of Conduct</div>
  </FlexRow>
</FlexColumn>;

export const DataAccessRequirements = fp.flow(withProfileErrorModal)((spinnerProps: WithSpinnerOverlayProps) => {
  useEffect(() => spinnerProps.hideSpinner(), []);

  return <FlexColumn style={styles.pageWrapper}>
    <DARHeader/>
    <FadeBox style={styles.fadeBox}>
      <div style={styles.pleaseComplete}>
        Please complete the necessary steps to gain access to the <AoU/> datasets.
      </div>
      <RegisteredTierCard/>
      {/* TODO - Step 2 ControlledTierCard */}
      <DuccCard/>
    </FadeBox>
    </FlexColumn>;
});
