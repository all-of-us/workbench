import * as React from 'react';

import {Header, SmallHeader} from '../../components/headers';

import {GoogleIcon, LoginButton, SecondaryLoginbutton} from './style';

export class LoginReactComponent extends React.Component<any, any> {
  googleIcon = '/assets/icons/google-icon.png';

  constructor(props: Object) {
    super(props);
  }

  showCreateAccountState() {
    this.props.updateNext(1);
  }

  signIn() {
    this.props.signIn();
  }

  render() {
    return <div style={{marginTop: '6.5rem',  paddingLeft: '3rem'}}>
      <React.Fragment>
        <div>
          <Header>
            Already have an account?
          </Header>
          <div>
            <LoginButton onClick={() => this.signIn()}>
              <GoogleIcon src={this.googleIcon}/>
              <div>
                Sign In with Google
              </div>
            </LoginButton>
          </div>
        </div>
        <div style={{paddingTop: '1.25rem'}}>
          <SmallHeader>
            Don't have an account?
          </SmallHeader>
          <SecondaryLoginbutton onClick={() => this.showCreateAccountState()}>
            Create Account
          </SecondaryLoginbutton>
        </div>
      </React.Fragment>
    </div>;
  }
}
export default LoginReactComponent;
