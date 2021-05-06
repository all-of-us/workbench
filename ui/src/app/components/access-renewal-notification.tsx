import * as fp from 'lodash/fp';
import * as React from 'react';

import colors from 'app/styles/colors';
import {reactStyles} from 'app/utils';
import {Profile, RenewableAccessModuleStatus} from 'generated/fetch';
import {Button} from './buttons';
import {FlexRow} from './flex';
import {AlarmExclamation} from './icons';
import {navigate, navigateByUrl} from "../utils/navigation";

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
    backgroundColor: 'rgba(38,34,98,0.08)',
    marginLeft: 'auto',
    marginRight: 'auto',
  },
  buttonText: {
    height: '20px',
    width: '79.6px',
    color: colors.accent,
    fontFamily: 'Montserrat',
    fontSize: '11.2px',
    fontWeight: 500,
    letterSpacing: '-0.24px',
    lineHeight: '19.2px',
    textAlign: 'center',
  },
});

export const MILLIS_PER_DAY = 24 * 60 * 60 * 1000;
export const NOTIFICATION_THRESHOLD_DAYS = 30;

// return the number of full days remaining to expiration in the soonest-to-expire module,
// but only if it is within the threshold.
// if it is not, or no expiration dates are present in the profile: return undefined.
export const maybeDaysRemaining = (profile: Profile): number | undefined => {
  const earliestExpiration: number = fp.flow(
    fp.get(['renewableAccessModules', 'modules']),
    fp.map<RenewableAccessModuleStatus, number>(m => m.expirationEpochMillis),
      // remove the undefined expirationEpochMillis
    fp.compact,
    fp.min)(profile);

  if (earliestExpiration) {
    const daysRemaining = (earliestExpiration - Date.now()) / MILLIS_PER_DAY;
    if (daysRemaining < NOTIFICATION_THRESHOLD_DAYS) {
      // note that we will show [0] days remaining if the expiration is later today
      return Math.trunc(daysRemaining);
    }
  }
};

const AccessRenewalNotification = (props: {daysRemaining: number}) => {
  return <FlexRow style={styles.box}>
    <AlarmExclamation style={styles.icon}/>
    <div style={styles.text}>Time for access renewal. [{props.daysRemaining}] days remaining.</div>
    {/* TODO navigate to Access Renewal pages */}
    <Button style={styles.button} onClick={() => navigateByUrl('profile')}>
      <div style={styles.buttonText}>Get Started</div>
    </Button>
  </FlexRow>;
};

export const AccessRenewalNotificationMaybe = (props: {profile: Profile}) => {
  const daysRemaining = maybeDaysRemaining(props.profile);
  if (!!daysRemaining) {
    return <AccessRenewalNotification daysRemaining={daysRemaining}/>;
  } else {
    // do not render
    return null;
  }
};
