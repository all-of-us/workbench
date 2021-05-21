import * as fp from 'lodash/fp';
import * as React from 'react';

import {withRouteData, Navigate} from 'app/components/app-router';
import {Button, Clickable} from 'app/components/buttons';
import {FadeBox} from 'app/components/containers';
import {FlexColumn } from 'app/components/flex';
import {Arrow, ClrIcon, ExclamationTriangle, withCircleBackground} from 'app/components/icons';
import {RadioButton} from 'app/components/inputs';
import {AoU} from 'app/components/text-wrappers';
import {withProfileErrorModal} from 'app/components/with-error-modal';
import {styles} from 'app/pages/profile/profile-styles';
import {navigateByUrl, navigate} from 'app/utils/navigation';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {maybeDaysRemaining} from 'app/components/access-renewal-notification'
import {profileApi} from 'app/services/swagger-fetch-clients';
import {redirectToTraining} from 'app/utils/access-utils'
import {
  daysFromNow,
  displayDateWithoutHours,
  useId,
  withStyle
} from 'app/utils';
import {profileStore, useStore, withProfileStoreReload} from 'app/utils/stores';

const {useState} = React;
// Lookback period - at what point do we give users the option to update their compliance items?
// In an effort to allow users to sync all of their training, we are setting at 330 to start.
const LOOKBACK_PERIOD = 330;

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
  card: {
    backgroundColor: colors.white,
    border: `1px solid ${colorWithWhiteness(colors.dark, 0.8)}`,
    borderRadius: '0.2rem',
    display: 'flex',
    fontSize: '0.58rem',
    fontWeight: 500,
    height: 345,
    lineHeight: '22px',
    margin: 0,
    padding: '0.5rem',
    width: 560
  }
};

const withInvalidDateHandling = date => {
  if (!date) {
    return 'Unavailable';
  } else {
    return displayDateWithoutHours(date);
  }
};

const confirmPublications = withProfileStoreReload(async () => {
  try {
    await profileApi().confirmPublications();
  } catch {
    console.log('Error')
  }
})

const BackArrow = withCircleBackground(() => <Arrow style={{height: 21, width: 18}}/>);

const RenewalCard = withStyle(renewalStyle.card)(
  ({step, TitleComponent, lastCompletion, nextReview, children, style}) => {
    const daysRemaining = daysFromNow(nextReview);
    return <FlexColumn style={style}>
      <div style={renewalStyle.h3}>STEP {step}</div>
      <div style={renewalStyle.h3}><TitleComponent/></div>
      <div style={{ color: colors.primary, margin: '0.5rem 0', display: 'grid', gridTemplateColumns: '6rem 1fr'}}>
        <div>Last Updated On:</div>
        <div>Next Review:</div>
        <div>{`${withInvalidDateHandling(lastCompletion)}`}</div>
        <div>{`${withInvalidDateHandling(nextReview)} (${daysRemaining >= 0 ? daysRemaining + ' days' : 'expired'})`}</div>
      </div>
      {children}
    </FlexColumn>;
  }
);

const CompletedButton = ({buttonText, wasBypassed, style}:
  {buttonText: string, wasBypassed: boolean, style?: React.CSSProperties}) => <Button disabled={true}
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

const isComplete = (nextReview: number): boolean => daysFromNow(nextReview) > LOOKBACK_PERIOD;

export const AccessRenewalPage = fp.flow(
  withRouteData,
  withProfileErrorModal
)(() => {
  const {profile: {
    complianceTrainingCompletionTime,
    dataUseAgreementCompletionTime,
    publicationsLastConfirmedTime,
    profileLastConfirmedTime,
    dataUseAgreementBypassTime,
    complianceTrainingBypassTime,
    renewableAccessModules: {modules}},
    profile
  } = useStore(profileStore);
  const [publications, setPublications] = useState<boolean>(null);
  const noReportId = useId();
  const reportId = useId();
  const [navigatex, setNavigatex] = useState(null)
  const getExpirationTimeFor = moduleName => fp.flow(fp.find({moduleName: moduleName}), fp.get('expirationEpochMillis'))(modules);

  if (navigatex) {
    return <Navigate to={navigatex}/>;
  }

  console.log(maybeDaysRemaining(profile) )
  return <FadeBox style={{margin: '1rem auto 0', color: colors.primary}}>
    <div style={{display: 'grid', gridTemplateColumns: '1.5rem 1fr', alignItems: 'center', columnGap: '.675rem'}}>
      
      {maybeDaysRemaining(profile) < 0
        ? <React.Fragment>
            <ExclamationTriangle style={{height: '1.5rem', width: '1.5rem'}}/>
            <div style={styles.h1}>Access to the Researcher Workbench revoked.</div>
          </React.Fragment>  
        : <React.Fragment>
            <Clickable onClick={() => history.back()}><BackArrow style={{height: '1.5rem', width: '1.5rem'}}/></Clickable>
            <div style={styles.h1}>Yearly Researcher Workbench access renewal</div>
          </React.Fragment> 
      }
      <div style={{gridColumnStart: 2}}>Researchers are required to complete a number of steps as part of the annual renewal
        to maintain access to <AoU/> data. Renewal of access will occur on a rolling basis annually (i.e. for each user, access
        renewal will be due 365 days after the date of authorization to access <AoU/> data.
      </div>
    </div>
    <div style={{...renewalStyle.h2, margin: '1rem 0'}}>Please complete the following steps</div>
    <div style={{display: 'grid', gridTemplateColumns: 'auto 1fr', marginBottom: '1rem', alignItems: 'center', gap: '1rem'}}>
      {/* Profile */}
      <RenewalCard step={1}
                TitleComponent={() => 'Update your profile'}
                lastCompletion={profileLastConfirmedTime}
                nextReview={getExpirationTimeFor('profileConfirmation')}>
        <div style={{marginBottom: '0.5rem'}}>Please update your profile information if any of it has changed recently.</div>
        <div>Note that you are obliged by the Terms of Use of the Workbench to provide keep your profile
          information up-to-date at all times.
        </div>
        {
          isComplete(getExpirationTimeFor('profileConfirmation'))
            ? <CompletedButton buttonText='Confirmed' wasBypassed={false}/>
            : <Button onClick={() => {
              // history.pushState('access-renewal', '');
              navigateByUrl('profile?renewal=1')
            }} style={{marginTop: 'auto', height: '1.6rem', width: '4.5rem'}}>Review</Button>
        }
      </RenewalCard>
      {/* Publications */}
      <RenewalCard step={2}
                   TitleComponent={() => 'Report any publications or presentations based on your research using the Researcher Workbench'}
                   lastCompletion={publicationsLastConfirmedTime}
                   nextReview={getExpirationTimeFor('publicationConfirmation')}>
        <div>The <AoU/> Publication and Presentation Policy requires that you report any upcoming publication or
             presentation resulting from the use of <AoU/> Research Program Data at least two weeks before the date of publication.
             If you are lead on or part of a publication or presentation that hasn’t been reported to the
             program, <a target='_blank' style={{textDecoration: 'underline'}} 
              href={'https://redcap.pmi-ops.org/surveys/?s=MKYL8MRD4N'}>please report it now.</a>
        </div>
        <div style={{marginTop: 'auto', display: 'grid', columnGap: '0.25rem', gridTemplateColumns: 'auto 1rem 1fr', alignItems: 'center'}}>
          {
            isComplete(getExpirationTimeFor('publicationConfirmation'))
              ? <CompletedButton style={{gridRow: '1 / span 2'}} buttonText='Confirmed' wasBypassed={false}/>
              : <Button disabled={publications === null}
                    onClick={() => confirmPublications()}
                    style={{gridRow: '1 / span 2', height: '1.6rem', width: '4.5rem', marginRight: '0.25rem'}}>Confirm</Button>
          }
          <RadioButton id={noReportId} 
            disabled={isComplete(getExpirationTimeFor('publicationConfirmation'))} 
            style={{justifySelf: 'end'}} checked={publications === true} 
            onChange={() => setPublications(true)}/>
          <label htmlFor={noReportId}> At this time, I have nothing to report </label>
          <RadioButton id={reportId} 
            disabled={isComplete(getExpirationTimeFor('publicationConfirmation'))}
            style={{justifySelf: 'end'}} 
            checked={publications === false} 
            onChange={() => setPublications(false)}/>
          <label htmlFor={reportId}>Report submitted</label>
        </div>
      </RenewalCard>
      {/* Compliance Training */}
      <RenewalCard step={3}
                   TitleComponent={() => <div><AoU/> Responsible Conduct of Research Training</div>}
                   lastCompletion={complianceTrainingCompletionTime}
                   nextReview={getExpirationTimeFor('complianceTraining')}>
        <div> You are required to complete the refreshed ethics training courses to understand the privacy safeguards and
          the compliance requirements for using the <AoU/> Dataset.
        </div>
        {
          complianceTrainingBypassTime || isComplete(getExpirationTimeFor('complianceTraining'))
            ? <CompletedButton buttonText='Confirmed' wasBypassed={!!complianceTrainingBypassTime}/>
            : <Button onClick={redirectToTraining} 
                style={{marginTop: 'auto', height: '1.6rem', width: 'max-content'}}>Complete Training</Button>
        }
      </RenewalCard>
      {/* DUCC */}
      <RenewalCard step={4}
                   TitleComponent={() => 'Sign Data User Code of Conduct'}
                   lastCompletion={dataUseAgreementCompletionTime}
                   nextReview={getExpirationTimeFor('dataUseAgreement')}>
        <div>Please review and sign the data user code of conduct consenting to the <AoU/> data use policy.</div>
        {
          dataUseAgreementBypassTime || isComplete(getExpirationTimeFor('dataUseAgreement'))
            ? <CompletedButton buttonText='Confirmed' wasBypassed={!!dataUseAgreementBypassTime} />
            : <Button onClick={() => navigate(['data-code-of-conduct'])} 
                      style={{marginTop: 'auto', height: '1.6rem', width: 'max-content'}}>View & Sign</Button>
        }
      </RenewalCard>
    </div>
  </FadeBox>;
});
