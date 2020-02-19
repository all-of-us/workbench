import {Button} from 'app/components/buttons';
import {CookieBanner} from 'app/components/cookie-banner';
import {Header, SmallHeader} from 'app/components/headers';
import colors from 'app/styles/colors';
import {reactStyles} from 'app/utils';

import * as React from 'react';

const googleIcon = '/assets/icons/google-icon.png';

export const styles = reactStyles({
  sign: {
    backgroundSize: 'contain',
    backgroundRepeat: 'noRepeat',
    backgroundPosition: 'center',
    display: 'flex',
    justifyContent: 'spaceAround',
    alignItems: 'flexStart',
    width: 'auto'
  },
  button: {
    marginTop: '0.5rem',
    display: 'flex',
    alignItems: 'center',
    height: 'auto',
    paddingLeft: '0',
    fontSize: '18px',
    fontStyle: 'normal',
    textTransform: 'none',
    borderRadius: '2px',
    justifyContent: 'baseline',
    maxWidth: '11.45rem'
  },
  fismaCommon: {
    fontSize: '10px',
    color: colors.primary,
    lineHeight: '18px'
  },
  fismaHeader: {
    fontWeight: 600,
    textTransform: 'uppercase',
  },
  fismaSection: {
    fontWeight: 400,
    marginBottom: '0.5rem'
  }
});


export const LoginReactComponent: React.FunctionComponent<{
  signIn: Function, onCreateAccount: Function }> = ({ signIn, onCreateAccount}) => {
    return <React.Fragment>
      <div data-test-id='login' style={{marginTop: '5.5rem',  paddingLeft: '3rem'}}>
        <div>
          <Header>
            Already have an account?
          </Header>
          <div>
            <Button type='primary' style={styles.button} onClick={signIn}>
              <img src={googleIcon}
                     style={{ height: '54px', width: '54px', margin: '-3px 19px -3px -3px'}}/>
              <div>
                Sign In with Google
              </div>
            </Button>
          </div>
        </div>
        <div style={{paddingTop: '1.25rem'}}>
          <SmallHeader>
            Don't have an account?
          </SmallHeader>
          <Button type='secondary' style={{fontSize: '10px', margin: '.25rem .5rem .25rem 0',
            border: `1px solid ${colors.primary}`, height: '48px'}}
                  onClick={onCreateAccount}>
            Create Account
          </Button>
        </div>
        <div style={{width: '400px'}}>
          <h4 style={{...styles.fismaCommon, ...styles.fismaHeader}}>Warning Notice</h4>
          <div style={{...styles.fismaCommon, ...styles.fismaSection}}>
            You are accessing a US Government web site which may contain information that must be
            protected under the US Privacy Act or other sensitive information and is intended for
            Government authorized use only.
          </div>
          <div style={{...styles.fismaCommon, ...styles.fismaSection}}>
            Unauthorized attempts to upload information, change information, or use of this web site
            may result in disciplinary action, civil, and/or criminal penalties. Unauthorized users
            of this website should have no expectation of privacy regarding any communications or
            data processed by this website.
          </div>
          <div style={{...styles.fismaCommon, ...styles.fismaSection}}>
            By continuing to log in, anyone accessing this website expressly consents to monitoring
            of their actions and all communications or data transiting or stored on related to this
            website and is advised that if such monitoring reveals possible evidence of criminal
            activity, NIH may provide that evidence to law enforcement officials.
          </div>
        </div>
      </div>
      <CookieBanner/>
    </React.Fragment>;
  };

export default LoginReactComponent;
