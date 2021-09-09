import * as fp from 'lodash/fp';
import * as React from 'react';
const {useState, useEffect} = React;

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
import {cond, daysFromNow, displayDateWithoutHours, switchCase, useId, withStyle} from 'app/utils';
import {
  getAccessModuleBypassTime,
  getAccessModuleCompletionTime,
  maybeDaysRemaining,
  redirectToTraining
} from 'app/utils/access-utils';
import {useNavigation} from 'app/utils/navigation';
import {profileStore, serverConfigStore, useStore} from 'app/utils/stores';
import {AccessModule, AccessModuleStatus} from 'generated/fetch';

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


const syncAndReload = fp.flow(
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

const isExpiring = (nextReview: number): boolean => daysFromNow(nextReview) <= serverConfigStore.get().config.accessRenewalLookback;

const getExpirationTimeFor = (modules: Array<AccessModuleStatus>, moduleName: AccessModule): number => fp.flow(
    fp.find({moduleName: moduleName}),
    fp.get('expirationEpochMillis'))
(modules);

const isModuleExpiring = (modules: Array<AccessModuleStatus>, moduleName: AccessModule): boolean =>
    isExpiring(getExpirationTimeFor(modules, moduleName));

const withInvalidDateHandling = date => {
  if (!date) {
    return 'Unavailable';
  } else {
    return displayDateWithoutHours(date);
  }
};

const computeDisplayDates = (lastConfirmedTime, bypassTime, nextReviewTime) => {
  const userCompletedModule = !!lastConfirmedTime;
  const userBypassedModule = !!bypassTime;
  const lastConfirmedDate = withInvalidDateHandling(lastConfirmedTime);
  const nextReviewDate = withInvalidDateHandling(nextReviewTime);
  const bypassDate = withInvalidDateHandling(bypassTime);

  return cond(
    // User has bypassed module
    [userBypassedModule, () => ({lastConfirmedDate: `${bypassDate}`, nextReviewDate: 'Unavailable (bypassed)'})],
    // User never completed training
    [!userCompletedModule && !userBypassedModule, () =>
      ({lastConfirmedDate: 'Unavailable (not completed)', nextReviewDate: 'Unavailable (not completed)'})],
    // User completed training, but is in the lookback window
    [userCompletedModule && isExpiring(nextReviewTime), () => {
      const daysRemaining = daysFromNow(nextReviewTime);
      const daysRemainingDisplay = daysRemaining >= 0 ? `(${daysRemaining} day${daysRemaining !== 1 ? 's' : ''})` : '(expired)';
      return {
        lastConfirmedDate,
        nextReviewDate: `${nextReviewDate} ${daysRemainingDisplay}`
      };
    }],
    // User completed training and is up to date
    [userCompletedModule && !isExpiring(nextReviewTime), () => {
      const daysRemaining = daysFromNow(nextReviewTime);
      return {lastConfirmedDate, nextReviewDate: `${nextReviewDate} (${daysRemaining} day${daysRemaining !== 1 ? 's' : ''})`};
    }]
  );
};


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

const wasBypassed = (modules, moduleName) => switchCase(moduleName,
    [AccessModule.DATAUSERCODEOFCONDUCT, () => !!getAccessModuleBypassTime(modules, AccessModule.DATAUSERCODEOFCONDUCT)],
    [AccessModule.COMPLIANCETRAINING, () => !!getAccessModuleBypassTime(modules, AccessModule.COMPLIANCETRAINING)],
    // these cannot be bypassed
    [AccessModule.PROFILECONFIRMATION, () => false],
    [AccessModule.PUBLICATIONCONFIRMATION, () => false]);


interface ActionButtonInterface {
  modules: AccessModuleStatus[];
  moduleName: AccessModule;
  actionButtonText: string;
  completedButtonText: string;
  onClick: Function;
  disabled?: boolean;
  style?: React.CSSProperties;
}
const ActionButton = (
  {modules, moduleName, actionButtonText, completedButtonText, onClick, disabled, style}: ActionButtonInterface) => {

  return wasBypassed(modules, moduleName) || !isModuleExpiring(modules, moduleName)
    ? <CompletedButton buttonText={completedButtonText} wasBypassed={wasBypassed(modules, moduleName)} style={style}/>
    : <Button
        onClick={onClick}
        disabled={disabled}
        style={{marginTop: 'auto', height: '1.6rem', width: 'max-content', ...style}}>{actionButtonText}</Button>;
};

const BackArrow = withCircleBackground(() => <Arrow style={{height: 21, width: 18}}/>);

const RenewalCard = withStyle(renewalStyle.card)(
  ({step, TitleComponent, modules, moduleName, children, style}) => {
    const lastCompletionTime = getAccessModuleCompletionTime(modules, moduleName);
    const nextReviewTime = getExpirationTimeFor(modules, moduleName)
    const bypassTime = getAccessModuleBypassTime(modules, moduleName)

    const {lastConfirmedDate, nextReviewDate} = computeDisplayDates(lastCompletionTime, bypassTime, nextReviewTime);

    return <FlexColumn style={style}>
      <div style={renewalStyle.h3}>STEP {step}</div>
      <div style={renewalStyle.h3}><TitleComponent/></div>
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
export const AccessRenewal = fp.flow(
  withProfileErrorModal
)((spinnerProps: WithSpinnerOverlayProps) => {
  useEffect(() => spinnerProps.hideSpinner(), []);
  // State
  const {profile: {
    accessModules: {modules}},
    profile
  } = useStore(profileStore);
  const {config: {enableComplianceTraining}} = useStore(serverConfigStore);
  const [publications, setPublications] = useState<boolean>(null);
  const noReportId = useId();
  const reportId = useId();
  const [refreshButtonDisabled, setRefreshButtonDisabled] = useState(true);
  const [loading, setLoading] = useState(false);
  const [, navigateByUrl] = useNavigation();


  // onMount - as we move between pages, let's make sure we have the latest profile
  useEffect(() => {
    const getProfile = async() => {
      setLoading(true);
      await reloadProfile();
      setLoading(false);
    };

    getProfile();
  }, []);

  const completeOrBypassed = moduleName => wasBypassed(modules, moduleName) || !isExpiring(getExpirationTimeFor(modules, moduleName));
  const allModulesCompleteOrBypassed = fp.flow(fp.map('moduleName'), fp.all(completeOrBypassed))(modules);

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
          TitleComponent={() => 'Update your profile'}
          modules={modules}
          moduleName={AccessModule.PROFILECONFIRMATION}>
        <div style={{marginBottom: '0.5rem'}}>Please update your profile information if any of it has changed recently.</div>
        <div>Note that you are obliged by the Terms of Use of the Workbench to provide keep your profile
          information up-to-date at all times.
        </div>
        <ActionButton
            modules={modules}
            moduleName={AccessModule.PROFILECONFIRMATION}
            actionButtonText='Review'
            completedButtonText='Confirmed'
            onClick={() => navigateByUrl('profile', {queryParams: {renewal: 1}})}/>
      </RenewalCard>
      {/* Publications */}
      <RenewalCard
          step={2}
          TitleComponent={() => 'Report any publications or presentations based on your research using the Researcher Workbench'}
          modules={modules}
          moduleName={AccessModule.PUBLICATIONCONFIRMATION}>
        <div>The <AoU/> Publication and Presentation Policy requires that you report any upcoming publication or
             presentation resulting from the use of <AoU/> Research Program Data at least two weeks before the date of publication.
             If you are lead on or part of a publication or presentation that hasn’t been reported to the
             program, <a target='_blank' style={{textDecoration: 'underline'}}
              href={'https://redcap.pmi-ops.org/surveys/?s=MKYL8MRD4N'}>please report it now.</a> For any questions,
             please contact <a href='mailto:support@researchallofus.org'>support@researchallofus.org</a>
        </div>
        <div style={{marginTop: 'auto', display: 'grid', columnGap: '0.25rem', gridTemplateColumns: 'auto 1rem 1fr', alignItems: 'center'}}>
          <ActionButton
              modules={modules}
              moduleName={AccessModule.PUBLICATIONCONFIRMATION}
              actionButtonText='Confirm'
              completedButtonText='Confirmed'
              onClick={async() => {
                setLoading(true);
                await confirmPublications();
                setLoading(false);
              }}
              disabled={publications === null}
              style={{gridRow: '1 / span 2', marginRight: '0.25rem'}}/>
          <RadioButton id={noReportId}
            disabled={!isModuleExpiring(modules, AccessModule.PUBLICATIONCONFIRMATION)}
            style={{justifySelf: 'end'}} checked={publications === true}
            onChange={() => setPublications(true)}/>
          <label htmlFor={noReportId}> At this time, I have nothing to report </label>
          <RadioButton id={reportId}
            disabled={!isModuleExpiring(modules, AccessModule.PUBLICATIONCONFIRMATION)}
            style={{justifySelf: 'end'}}
            checked={publications === false}
            onChange={() => setPublications(false)}/>
          <label htmlFor={reportId}>Report submitted</label>
        </div>
      </RenewalCard>
      {/* Compliance Training */}
      {enableComplianceTraining && <RenewalCard
          step={3}
          TitleComponent={() => <div><AoU/> Responsible Conduct of Research Training</div>}
          modules={modules}
          moduleName={AccessModule.COMPLIANCETRAINING}>
      <div> You are required to complete the refreshed ethics training courses to understand the privacy safeguards and
          the compliance requirements for using the <AoU/> Dataset.
        </div>
        {isModuleExpiring(modules, AccessModule.COMPLIANCETRAINING)
        && !getAccessModuleBypassTime(modules, AccessModule.COMPLIANCETRAINING) &&
          <div style={{borderTop: `1px solid ${colorWithWhiteness(colors.dark, 0.8)}`, marginTop: '0.5rem', paddingTop: '0.5rem'}}>
            When you have completed the training click the refresh button or reload the page.
          </div>}
        <FlexRow style={{marginTop: 'auto'}}>
          <ActionButton
              modules={modules}
              moduleName={AccessModule.COMPLIANCETRAINING}
              actionButtonText='Complete Training'
              completedButtonText='Completed'
              onClick={() => {
                setRefreshButtonDisabled(false);
                redirectToTraining();
              }}/>
          {isModuleExpiring(modules, AccessModule.COMPLIANCETRAINING)
          && !getAccessModuleBypassTime(modules, AccessModule.COMPLIANCETRAINING)
          && <Button
            disabled={refreshButtonDisabled}
            onClick={async() => {
              setLoading(true);
              await syncAndReload();
              setLoading(false);
            }}
            style={{height: '1.6rem', marginLeft: '0.75rem', width: 'max-content'}}>Refresh</Button>}
        </FlexRow>
      </RenewalCard>}
      {/* DUCC */}
      <RenewalCard
          step={enableComplianceTraining ? 4 : 3}
          TitleComponent={() => 'Sign Data User Code of Conduct'}
          modules={modules}
          moduleName={AccessModule.DATAUSERCODEOFCONDUCT}>
        <div>Please review and sign the data user code of conduct consenting to the <AoU/> data use policy.</div>
        <ActionButton
            modules={modules}
            moduleName={AccessModule.DATAUSERCODEOFCONDUCT}
            actionButtonText='View & Sign'
            completedButtonText='Completed'
            onClick={() => navigateByUrl('data-code-of-conduct', {queryParams: {renewal: 1}})}/>
      </RenewalCard>
    </div>
    {loading && <SpinnerOverlay dark={true} opacity={0.6}/>}
  </FadeBox>;
});
