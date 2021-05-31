import * as React from 'react';

import colors from 'app/styles/colors';
import {reactStyles} from 'app/utils';
import {maybeDaysRemaining} from 'app/utils/access-utils';
import {navigate} from 'app/utils/navigation';
import {profileStore, useStore} from 'app/utils/stores';
import {Button} from './buttons';
import {FlexRow} from './flex';
import {AlarmExclamation} from './icons';

const styles = reactStyles({
  box: {
    boxSizing: 'border-box',
    height: '56.5px',
    width: '380.5px',
    border: '0.5px solid rgba(38,34,98,0.5)',
    borderRadius: '5px',
    backgroundColor: 'rgba(38,34,98,0.08)',
    alignItems: 'center',
    justifyContent: 'center',
    marginLeft: 'auto',
  },
  icon: {
    height: '25px',
    width: '25px',
    color:  colors.warning,
    fontFamily: 'Font Awesome 5 Pro',
    fontSize: '25px',
    letterSpacing: 0,
    lineHeight: '25px',
    marginLeft: 'auto',
  },
  text: {
    height: '40px',
    width: '177px',
    color: colors.primary,
    fontFamily: 'Montserrat',
    fontSize: '14px',
    fontWeight: 600,
    letterSpacing: 0,
    lineHeight: '20px',
    marginLeft: 'auto',
  },
  button: {
    boxSizing: 'border-box',
    height: '30px',
    width: '102px',
    border: '0.8px solid #216FB4',
    borderRadius: '1.6px',
    marginLeft: 'auto',
    marginRight: 'auto',
  },
  buttonText: {
    height: '20px',
    width: '79.6px',
    fontFamily: 'Montserrat',
    fontSize: '11.2px',
    fontWeight: 500,
    letterSpacing: '-0.24px',
    lineHeight: '19.2px',
    textAlign: 'center',
  },
});


const AccessRenewalNotification = ({daysRemaining}: {daysRemaining: number}) => {
  return <FlexRow style={styles.box}>
    <AlarmExclamation style={styles.icon}/>
    <div style={styles.text}>Time for access renewal. {
      `${daysRemaining >= 0 ? daysRemaining + ' days remaining.' : 'Your access has expired.'}`
    }
    </div>
    {/* TODO RW-6618 navigate to Access Renewal pages */}
    <Button type='primary' style={styles.button} onClick={() => navigate(['access-renewal'])}>
      <div style={styles.buttonText}>Get Started</div>
    </Button>
  </FlexRow>;
};

export const AccessRenewalNotificationMaybe = () => {
  const {profile} = useStore(profileStore);
  const daysRemaining = maybeDaysRemaining(profile);
  // returning null is a way to tell React not to render this component.  `undefined` won't work here.
  return daysRemaining !== undefined ? <AccessRenewalNotification daysRemaining={daysRemaining}/> : null;
};
