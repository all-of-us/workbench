import * as React from 'react';

import colors from 'app/styles/colors';
import {reactStyles} from 'app/utils';
import {getAccessModuleStatusByName} from 'app/utils/access-utils';
import {useNavigation} from 'app/utils/navigation';
import {profileStore, serverConfigStore, useStore} from 'app/utils/stores';
import {environment} from 'environments/environment';
import {AccessModule} from 'generated/fetch';
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
    width: '199px',
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
    height: '38px',
    width: '120px',
    border: '0.8px solid #216FB4',
    borderRadius: '1.6px',
    marginLeft: 'auto',
    marginRight: 'auto',
  },
  buttonText: {
    height: '21px',
    width: '98px',
    fontFamily: 'Montserrat',
    fontSize: '14px',
    fontWeight: 500,
    letterSpacing: '0',
    lineHeight: '21px',
    textAlign: 'center',
  },
});


const LoginGovIAL2Notification = () => {
  const [navigate, ] = useNavigation();
  return <FlexRow data-test-id='ial2-notification' style={styles.box}>
    <AlarmExclamation style={styles.icon}/>
    <div style={styles.text}>Please verify your identity by 10/06/2021.
    </div>
    <Button
        type='primary'
        style={styles.button}
        onClick={() => environment.enableDataAccessRequirements ?
            navigate(['data-access-requirements']) :
            location.replace('/?workbenchAccessTasks=true')}>
      <div style={styles.buttonText}>LEARN MORE</div>
    </Button>
  </FlexRow>;
};

export const LoginGovIAL2NotificationMaybe = () => {
  const {profile} = useStore(profileStore);
  const {config: {enableRasLoginGovLinking}} = useStore(serverConfigStore);
  const loginGovModule = getAccessModuleStatusByName(profile, AccessModule.RASLINKLOGINGOV);
  // Show the Login.gov IAL2 notification when
  // 1: enableRasLoginGovLinking enabled AND
  // 2: user is not bypassed AND hasn't completed.
  // 3: loginGovModule undefined means the same thing as 2.
  const shouldShowIal2Notification = enableRasLoginGovLinking &&
      (!loginGovModule || (!loginGovModule.bypassEpochMillis && !loginGovModule.completionEpochMillis));
  return shouldShowIal2Notification ? <LoginGovIAL2Notification/> : null;
};
