import * as React from 'react';
import {Header, SmallHeader} from '../../common/common';
import {Button, Google} from './css';

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
          <Header>Already have an account?</Header>
          <div>
            <Button onClick={() => this.signIn()}>
              <Google src={this.googleIcon}/>
              <div>Sign In with Google</div>
            </Button>
          </div>
        </div>
        <div style={{paddingTop: '1rem'}}>
          <SmallHeader>Don't have an account?</SmallHeader>
          <button onClick={() => this.showCreateAccountState()} className='btn btn-secondary'>
            Create Account
          </button>
        </div>
      </React.Fragment>
    </div>;
  }
}
export default LoginReactComponent;




