import * as React from 'react';

import {Button, Secondarybutton} from '../../components/buttons';
import {Header, SmallHeader} from '../../components/headers';

import {styles} from './style';

export class LoginReactComponent extends React.Component<any, any> {
  googleIcon = '/assets/icons/google-icon.png';

  constructor(props: Object) {
    super(props);
  }

  showCreateAccount() {
    this.props.onCreateAccount();
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
            <Button style={styles.button} onClick={() => this.signIn()}>
              <img src={this.googleIcon}
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
          <Secondarybutton style={styles.secondaryButton} onClick={() => this.showCreateAccount()}>
            Create Account
          </Secondarybutton>
        </div>
      </React.Fragment>
    </div>;
  }
}
export default LoginReactComponent;
