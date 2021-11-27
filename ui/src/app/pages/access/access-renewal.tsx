import * as fp from 'lodash/fp';
import * as React from 'react';

import {Button, Clickable} from 'app/components/buttons';
import {FadeBox} from 'app/components/containers';
import {FlexColumn, FlexRow} from 'app/components/flex';
import {Arrow, ClrIcon, ExclamationTriangle, withCircleBackground} from 'app/components/icons';
import {RadioButton} from 'app/components/inputs';
import {withErrorModal, withSuccessModal} from 'app/components/modals';
import {SpinnerOverlay} from 'app/components/spinners';
import {AoU} from 'app/components/text-wrappers';
import {withProfileErrorModal} from 'app/components/with-error-modal';
import {WithSpinnerOverlayProps} from 'app/components/with-spinner-overlay';
import {styles} from 'app/pages/profile/profile-styles';
import {profileApi} from 'app/services/swagger-fetch-clients';
import colors, {addOpacity, colorWithWhiteness} from 'app/styles/colors';
import {cond, useId, withStyle} from 'app/utils';
import {
  computeRenewalDisplayDates,
  accessRenewalModules,
  syncModulesExternal,
  getAccessModuleConfig,
  isExpiring,
  maybeDaysRemaining,
  redirectToRegisteredTraining,
} from 'app/utils/access-utils';
import {useNavigation} from 'app/utils/navigation';
import {profileStore, serverConfigStore, useStore} from 'app/utils/stores';
import {AccessModule, AccessModuleStatus} from 'generated/fetch';
import {SupportMailto} from 'app/components/support';


const {useState, useEffect} = React;

const renewalStyle = {
  h1: {
    fontSize: '0.83rem',
    fontWeight: 600,
    color: colors.primary
  },
  h2: {
    fontSize: '0.75rem',
    fontWeight: 600
  },
  h3: {
    fontSize: '0.675rem',
    fontWeight: 600
  },
  completionBox: {
    height: '3.5rem',
    background: `${addOpacity(colors.accent, 0.15)}`,
    borderRadius: 5 ,
    marginTop: '0.5rem',
    padding: '0.75rem'
  },
  card: {
    backgroundColor: colors.white,
    border: `1px solid ${colorWithWhiteness(colors.dark, 0.8)}`,
    borderRadius: '0.2rem',
    display: 'flex',
    fontSize: '0.58rem',
    fontWeight: 500,
    height: '15.375rem',
    lineHeight: '22px',
    margin: 0,
    padding: '0.5rem',
    width: 560
  }
};


// Async Calls with error handling
const reloadProfile = withErrorModal({
  title: 'Could Not Load Profile',
  message: 'Profile could not be reloaded. Please refresh the page to get your updated profile'
},
  profileStore.get().reload
);

const confirmPublications = fp.flow(
  withSuccessModal({
    title: 'Confirmed Publications',
    message: 'You have successfully reported your publications',
    onDismiss: reloadProfile
  }),
  withErrorModal({
    title: 'Failed To Confirm Publications',
    message: 'An error occurred trying to confirm your publications. Please try again.',
  })
)(async() => await profileApi().confirmPublications());


const syncAndReloadTraining = fp.flow(
  withSuccessModal({
    title: 'Compliance Status Refreshed',
    message: 'Your compliance training has been refreshed. If you are not seeing the correct status, try again in a few minutes.',
    onDismiss: reloadProfile
  }),
  withErrorModal({
    title: 'Failed To Refresh',
    message: 'An error occurred trying to refresh your compliance training status. Please try again.',
  })
)(async() => {
  await profileApi().syncComplianceTrainingStatus();
});

// Helper Functions

const isModuleExpiring = (status: AccessModuleStatus): boolean => isExpiring(status.expirationEpochMillis);

const isExpiringAndNotBypassed = (moduleName: AccessModule, modules: AccessModuleStatus[]) => {
  const status = modules.find(m => m.moduleName === moduleName);
  return isModuleExpiring(status) && !status.bypassEpochMillis;
}


// Helper / Stateless Components
interface CompletedButtonInterface {
  buttonText: string;
  wasBypassed: boolean;
  style?: React.CSSProperties;
}
const CompletedButton = ({buttonText, wasBypassed, style}: CompletedButtonInterface) => <Button disabled={true}
    data-test-id='completed-button'
    style={{
      height: '1.6rem',
      marginTop: 'auto',
      backgroundColor: colors.success,
      width: 'max-content',
      cursor: 'default',
      ...style
    }}>
    <ClrIcon shape='check' style={{marginRight: '0.3rem'}}/>{wasBypassed ? 'Bypassed' : buttonText}
  </Button>;

interface ActionButtonInterface {
  moduleStatus: AccessModuleStatus;
  actionButtonText: string;
  completedButtonText: string;
  onClick: Function;
  disabled?: boolean;
  style?: React.CSSProperties;
}
const ActionButton = (
  {moduleStatus, actionButtonText, completedButtonText, onClick, disabled, style}: ActionButtonInterface) => {
  const wasBypassed = !!moduleStatus.bypassEpochMillis;
  return wasBypassed || !isModuleExpiring(moduleStatus)
    ? <CompletedButton buttonText={completedButtonText} wasBypassed={wasBypassed} style={style}/>
    : <Button
        onClick={onClick}
        disabled={disabled}
        style={{marginTop: 'auto', height: '1.6rem', width: 'max-content', ...style}}>{actionButtonText}</Button>;
};

const BackArrow = withCircleBackground(() => <Arrow style={{height: 21, width: 18}}/>);

interface CardProps {
  step: number,
  moduleStatus: AccessModuleStatus,
  style: React.CSSProperties,
  children: string | React.ReactNode,
}
const RenewalCard = withStyle(renewalStyle.card)(
  ({step, moduleStatus, style, children}: CardProps) => {
    const {AARTitleComponent} = getAccessModuleConfig(moduleStatus.moduleName);
    const {lastConfirmedDate, nextReviewDate} = computeRenewalDisplayDates(moduleStatus);
    return <FlexColumn style={style}>
      <div style={renewalStyle.h3}>STEP {step}</div>
      <div style={renewalStyle.h3}><AARTitleComponent/></div>
      <div style={{ color: colors.primary, margin: '0.5rem 0', display: 'grid', columnGap: '1rem', gridTemplateColumns: 'auto 1fr'}}>
        <div>Last Updated On:</div>
        <div>Next Review:</div>
        <div>{lastConfirmedDate}</div>
        <div>{nextReviewDate}</div>
      </div>
      {children}
    </FlexColumn>;
  }
);

// Page to render
export const AccessRenewal = fp.flow(withProfileErrorModal)((spinnerProps: WithSpinnerOverlayProps) => {
  // State
  const {
    profile,
    profile: {accessModules: {modules}},
  } = useStore(profileStore);
  const {config: {enableComplianceTraining}} = useStore(serverConfigStore);
  const [publications, setPublications] = useState<boolean>(null);
  const noReportId = useId();
  const reportId = useId();
  const [refreshButtonDisabled, setRefreshButtonDisabled] = useState(true);
  const [loading, setLoading] = useState(false);
  const [, navigateByUrl] = useNavigation();

  const expirableModules = modules.filter(moduleStatus => accessRenewalModules.includes(moduleStatus.moduleName));

  const completeOrBypassed = moduleName => {
    const status = modules.find(m => m.moduleName === moduleName);
    const wasBypassed = !!status.bypassEpochMillis;
    return wasBypassed || !isExpiring(status.expirationEpochMillis);
  }

  const allModulesCompleteOrBypassed = fp.flow(fp.map('moduleName'), fp.all(completeOrBypassed))(expirableModules);

  // onMount - as we move between pages, let's make sure we have the latest profile and external module information
  useEffect(() => {
    const expiringModules = expirableModules
      .filter(status => status.expirationEpochMillis && isExpiring(status.expirationEpochMillis))
      .map(status => status.moduleName);

    const onMount = async() => {
      setLoading(true);
      await syncModulesExternal(expiringModules);
      await reloadProfile();
      setLoading(false);
      spinnerProps.hideSpinner();
    };

    onMount();
  }, []);

  // Render
  return <FadeBox style={{margin: '1rem auto 0', color: colors.primary}}>
    <div style={{display: 'grid', gridTemplateColumns: '1.5rem 1fr', alignItems: 'center', columnGap: '.675rem'}}>
      {cond(
        // Completed - no icon or button
        [allModulesCompleteOrBypassed, () => null],
        // Access expired icon
        [maybeDaysRemaining(profile) < 0, () => <React.Fragment>
          <ExclamationTriangle color={colors.warning} style={{height: '1.5rem', width: '1.5rem'}}/>
          <div style={styles.h1}>Researcher workbench access has expired.</div>
        </React.Fragment>],
        // Default - back button
        () => <React.Fragment>
          <Clickable onClick={() => history.back()}><BackArrow style={{height: '1.5rem', width: '1.5rem'}}/></Clickable>
          <div style={styles.h1}>Yearly Researcher Workbench access renewal</div>
        </React.Fragment>
        )
      }
      <div style={allModulesCompleteOrBypassed ? {gridColumn: '1 / span 2'} : {gridColumnStart: 2}}>
        Researchers are required to complete a number of steps as part of the annual renewal
        to maintain access to <AoU/> data. Renewal of access will occur on a rolling basis annually (i.e. for each user, access
        renewal will be due 365 days after the date of authorization to access <AoU/> data.
      </div>
      {allModulesCompleteOrBypassed && <div style={{...renewalStyle.completionBox, gridColumn: '1 / span 2'}}>
        <div style={renewalStyle.h2}>Thank you for completing all the necessary steps</div>
        <div>
          Your yearly Researcher Workbench access renewal is complete. You can use the menu icon in the top left to continue your research.
        </div>
      </div>}
    </div>
    <div style={{...renewalStyle.h2, margin: '1rem 0'}}>Please complete the following steps</div>
    <div style={{display: 'grid', gridTemplateColumns: 'auto 1fr', marginBottom: '1rem', alignItems: 'center', gap: '1rem'}}>
      {/* Profile */}
      <RenewalCard
          step={1}
          moduleStatus={modules.find(m => m.moduleName === AccessModule.PROFILECONFIRMATION)}>
        <div style={{marginBottom: '0.5rem'}}>Please update your profile information if any of it has changed recently.</div>
        <div>Note that you are obliged by the Terms of Use of the Workbench to provide keep your profile
          information up-to-date at all times.
        </div>
        <ActionButton
            actionButtonText='Review'
            completedButtonText='Confirmed'
            moduleStatus={modules.find(m => m.moduleName === AccessModule.PROFILECONFIRMATION)}
            onClick={() => navigateByUrl('profile', {queryParams: {renewal: 1}})}/>
      </RenewalCard>
      {/* Publications */}
      <RenewalCard
          step={2}
          moduleStatus={modules.find(m => m.moduleName === AccessModule.PUBLICATIONCONFIRMATION)}>
        <div>The <AoU/> Publication and Presentation Policy requires that you report any upcoming publication or
             presentation resulting from the use of <AoU/> Research Program Data at least two weeks before the date of publication.
             If you are lead on or part of a publication or presentation that hasn’t been reported to the
             program, <a target='_blank' style={{textDecoration: 'underline'}}
              href={'https://redcap.pmi-ops.org/surveys/?s=MKYL8MRD4N'}>please report it now.</a> For any questions,
             please contact <SupportMailto/>
        </div>
        <div style={{marginTop: 'auto', display: 'grid', columnGap: '0.25rem', gridTemplateColumns: 'auto 1rem 1fr', alignItems: 'center'}}>
          <ActionButton
              actionButtonText='Confirm'
              completedButtonText='Confirmed'
              moduleStatus={modules.find(m => m.moduleName === AccessModule.PUBLICATIONCONFIRMATION)}
              onClick={async() => {
                setLoading(true);
                await confirmPublications();
                setLoading(false);
              }}
              disabled={publications === null}
              style={{gridRow: '1 / span 2', marginRight: '0.25rem'}}/>
          <RadioButton
              id={noReportId}
              disabled={!isModuleExpiring(modules.find(m => m.moduleName === AccessModule.PUBLICATIONCONFIRMATION))}
              style={{justifySelf: 'end'}}
              checked={publications === true}
              onChange={() => setPublications(true)}/>
          <label htmlFor={noReportId}> At this time, I have nothing to report </label>
          <RadioButton
              id={reportId}
              disabled={!isModuleExpiring(modules.find(m => m.moduleName === AccessModule.PUBLICATIONCONFIRMATION))}
              style={{justifySelf: 'end'}}
              checked={publications === false}
              onChange={() => setPublications(false)}/>
          <label htmlFor={reportId}>Report submitted</label>
        </div>
      </RenewalCard>
      {/* Compliance Training */}
      {enableComplianceTraining && <RenewalCard
          step={3}
          moduleStatus={modules.find(m => m.moduleName === AccessModule.COMPLIANCETRAINING)}>
      <div> You are required to complete the refreshed ethics training courses to understand the privacy safeguards and
          the compliance requirements for using the <AoU/> Dataset.
        </div>
        {isExpiringAndNotBypassed(AccessModule.COMPLIANCETRAINING, modules) &&
          <div style={{borderTop: `1px solid ${colorWithWhiteness(colors.dark, 0.8)}`, marginTop: '0.5rem', paddingTop: '0.5rem'}}>
            When you have completed the training click the refresh button or reload the page.
          </div>}
        <FlexRow style={{marginTop: 'auto'}}>
          <ActionButton
              actionButtonText='Complete Training'
              completedButtonText='Completed'
              moduleStatus={modules.find(m => m.moduleName === AccessModule.COMPLIANCETRAINING)}
              onClick={() => {
                setRefreshButtonDisabled(false);
                redirectToRegisteredTraining();
              }}/>
          {isExpiringAndNotBypassed(AccessModule.COMPLIANCETRAINING, modules) && <Button
            disabled={refreshButtonDisabled}
            onClick={async() => {
              setLoading(true);
              await syncAndReloadTraining();
              setLoading(false);
            }}
            style={{height: '1.6rem', marginLeft: '0.75rem', width: 'max-content'}}>Refresh</Button>}
        </FlexRow>
      </RenewalCard>}
      {/* DUCC */}
      <RenewalCard
          step={enableComplianceTraining ? 4 : 3}
          moduleStatus={modules.find(m => m.moduleName === AccessModule.DATAUSERCODEOFCONDUCT)}>
        <div>Please review and sign the data user code of conduct consenting to the <AoU/> data use policy.</div>
        <ActionButton
            actionButtonText='View & Sign'
            completedButtonText='Completed'
            moduleStatus={modules.find(m => m.moduleName === AccessModule.DATAUSERCODEOFCONDUCT)}
            onClick={() => navigateByUrl('data-code-of-conduct', {queryParams: {renewal: 1}})}/>
      </RenewalCard>
    </div>
    {loading && <SpinnerOverlay dark={true} opacity={0.6}/>}
  </FadeBox>;
});
