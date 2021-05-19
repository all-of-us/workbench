import * as fp from 'lodash/fp';
import * as React from 'react';

import {Button} from 'app/components/buttons';
import {FadeBox} from 'app/components/containers';
import {RadioButton} from 'app/components/inputs';
import {FlexColumn, } from 'app/components/flex';
import {withProfileErrorModal} from 'app/components/with-error-modal';
import {styles} from 'app/pages/profile/profile-styles';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {
  displayDateWithoutHours,
  daysFromNow,
  withStyle,
} from 'app/utils';
import {withRouteData} from 'app/components/app-router';
import {Arrow, withCircleBackground} from 'app/components/icons'
import {baseStyles} from 'app/components/card';
import {profileStore, useStore} from 'app/utils/stores';

const {useState} = React;

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
  }
}


const BackArrow = withCircleBackground(() => <Arrow style={{height: 21, width: 18}}/>);
const borderColor = colorWithWhiteness(colors.dark, 0.8)

const RenewalCard = withStyle({...baseStyles.card, fontSize: '0.58rem', lineHeight: '22px', fontWeight: 500, border: `1px solid ${borderColor}`, padding: '0.5rem', margin:0, boxShadow: 'none', height: 345, width: 560})(
  ({step, title, lastCompletion, nextReview, children, style}) => {
    const daysRemaining = daysFromNow(nextReview)
    return <FlexColumn style={style}>
      <div style={renewalStyle.h3}>STEP {step}</div>
      <div style={renewalStyle.h3}>{title}</div>
      <div style={{ color: colors.primary, margin: '0.5rem 0', display: 'grid', gridTemplateColumns: '6rem 1fr'}}>
        <div>Last Updated On:</div>
        <div>Next Review:</div>
        <div>{`${displayDateWithoutHours(lastCompletion)}`}</div>
        <div>{`${displayDateWithoutHours(nextReview)} (${daysRemaining > 0 ? daysRemaining + ' days' : 'expired'})`}</div>
      </div>  
      {children}
    </FlexColumn>
  }
)

const getExpirationTimeForModule = (moduleName, modules) => {
  return fp.flow(fp.find({moduleName: moduleName}), fp.get('expirationEpochMillis'))(modules)
}

export const AccessRenewalPage = fp.flow(
  withRouteData,
  withProfileErrorModal
)(() => {
  const {profile: {
    complianceTrainingCompletionTime,
    dataUseAgreementCompletionTime,
    renewableAccessModules: {modules}}, 
    profile
  } = useStore(profileStore);
  const [publications, setPublications] = useState<boolean>()
  console.log(dataUseAgreementCompletionTime, complianceTrainingCompletionTime, profile);

  return <FadeBox style={{margin: '1rem auto 0', color: colors.primary}}>
    <div style={{display: 'grid', gridTemplateColumns: '1.5rem 1fr', alignItems: 'center', columnGap: '.675rem'}}>
      <BackArrow style={{height: '1.5rem', width: '1.5rem'}}/>
      <div style={styles.h1}>Yearly Researcher Workbench access renewal</div>
      <div style={{gridColumnStart: 2}}>Researchers are required to complete a number of steps as part of the annual renewal 
        to maintain access to All of Us data. Renewal of access will occur on a rolling basis annually (i.e. for each user, access 
        renewal will be due 365 days after the date of authorization to access All of Us data.
      </div>
    </div>
    <div style={{...renewalStyle.h2, margin: '1rem 0'}}>Please complete the following steps</div>
    <div style={{display: 'grid', gridTemplateColumns: 'auto 1fr', marginBottom: '1rem', alignItems: 'center', gap: '1rem'}}>
      <RenewalCard step={1} 
                title={'Update your profile'} 
                nextReview={getExpirationTimeForModule('profileConfirmation', modules)}>
        <div style={{marginBottom: '0.5rem'}}>Please update your profile information if any of it has changed recently.</div>
        <div>Note that you are obliged by the Terms of Use of the Workbench to provide keep your profile information up-to-date at all times.</div>
        <Button style={{marginTop: 'auto', height: '1.6rem', width: '4.5rem'}}>Review</Button>
      </RenewalCard>
      <RenewalCard step={2} 
                   title={'Report any publications or presentations based on your research using the Researcher Workbench'} 
                   nextReview={getExpirationTimeForModule('publicationConfirmation', modules)}>
        <div>The All of Us Publication and Presentation Policy requires that you report any upcoming publication or 
             presentation resulting from the use of All of Us Research Program Data at least two weeks before the date of publication. 
             If you are lead on or part of a publication or presentation that hasn’t been reported to the program, please report it now. 
        </div>
        <div style={{marginTop: 'auto', display: 'grid', columnGap: '0.25rem', gridTemplateColumns: 'auto 1rem 1fr', alignItems: 'center'}}>
          <Button style={{gridRow: '1 / span 2', height: '1.6rem', width: '4.5rem', marginRight: '0.25rem'}}>Confirm</Button>
          <RadioButton style={{justifySelf: 'end'}} checked={publications === true} onChange={() => setPublications(true)}/>
          <label> At this time, I have nothing to report </label>
          <RadioButton style={{justifySelf: 'end'}} checked={publications === false} onChange={() => setPublications(false)}/>
          <label>Report submitted</label>
        </div>
      </RenewalCard>
      <RenewalCard step={3} 
                   title={'All of Us Responsible Conduct of Research Training'} 
                   lastCompletion={complianceTrainingCompletionTime}
                   nextReview={getExpirationTimeForModule('complianceTraining', modules)}>
        <div> You are required to complete the refreshed ethics training courses to understand the privacy safeguards and 
          the compliance requirements for using the All of Us Dataset.
        </div>
        <Button style={{marginTop: 'auto', height: '1.6rem', width: '8rem'}}>Complete Training</Button>
      </RenewalCard>
      <RenewalCard step={4} 
                   title={'Sign Data User Code of Conduct'} 
                   lastCompletion={dataUseAgreementCompletionTime}
                   nextReview={getExpirationTimeForModule('publicationConfirmation', modules)}>
        <div>Please review and sign the data user code of conduct consenting to the 
          All of Us data use policy. 
        </div>
        <Button style={{marginTop: 'auto', height: '1.6rem', width: '5rem' }}>{'View & Sign'}</Button>
      </RenewalCard>
    </div>
  </FadeBox>;
}) 