import {Button} from 'app/components/buttons';
import {StyledAnchorTag} from 'app/components/buttons';
import {CookieBanner} from 'app/components/cookie-banner';
import {GoogleSignInButton} from 'app/components/google-sign-in';
import {Header, SmallHeader} from 'app/components/headers';
import {AouTitle} from 'app/components/text-wrappers';
import colors from 'app/styles/colors';
import {reactStyles} from 'app/utils';
import * as React from 'react';

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
  createAccountButton: {
    fontSize: '12px',
    margin: '.5rem .5rem .25rem 0',
    border: `1px solid ${colors.primary}`,
    height: '48px',
    width: '6.5rem'
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
      <div data-test-id='login' style={{marginBottom: '.5rem', marginTop: '5.5rem',  paddingLeft: '3rem'}}>
        <div>
          <Header style={{width: '14rem', lineHeight: '30px'}}>
            Already have a
            Researcher Workbench account?
          </Header>
          <div>
            <GoogleSignInButton signIn={signIn} />
          </div>
          <StyledAnchorTag
            target='_blank'
            href='https://www.researchallofus.org/faq/what-if-i-have-trouble-signing-in-to-the-workbench'
            style={{marginTop: '0.625rem', lineHeight: '0.75rem'}}>
              Trouble Signing In?
          </StyledAnchorTag>
        </div>
        <div style={{paddingTop: '1.25rem'}}>
          <SmallHeader>
            Don't have an account?
          </SmallHeader>
          <Button type='secondary' style={styles.createAccountButton}
                  onClick={onCreateAccount}>
            Create Account
          </Button>
        </div>
        <div style={{width: '400px'}}>
          <h4 style={{...styles.fismaCommon, ...styles.fismaHeader}}>Warning Notice</h4>
          <div style={{...styles.fismaCommon, ...styles.fismaSection}}>
            You are accessing a web site created by the <AouTitle/>, funded by the National Institutes
            of Health.
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
