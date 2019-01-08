
import {fullUrl} from 'app/utils/fetch';

import {BoldHeader} from 'app/components/headers';
import {FormInput} from 'app/components/inputs';
import {
  FetchArgs,
  InvitationVerificationRequest,
  ProfileApiFetchParamCreator
} from 'generated/fetch/api';
import * as React from 'react';

function isBlank(s: string) {
  return (!s || /^\s*$/.test(s));
}

interface InvitationKeyState {
  invitationKey: string;
  invitationKeyReq: boolean;
  invitationKeyInvalid: boolean;
}
export class InvitationKeyReact extends React.Component<any, InvitationKeyState> {

  state: InvitationKeyState

  constructor(props: Object) {
    super(props);
    this.state = {
      invitationKey: '',
      invitationKeyReq: false,
      invitationKeyInvalid: false
    };
    this.updateInvitationKey = this.updateInvitationKey.bind(this);
  }


  updateInvitationKey(input) {
    this.setState({
      invitationKey: input.target.value
    });
  }

  next() {
    if (isBlank(this.state.invitationKey)) {
      this.setState({
        invitationKeyReq: true
      });
      return;
    }
    this.setState({
      invitationKeyReq: false,
      invitationKeyInvalid: false
    });
    const request: InvitationVerificationRequest = {
      invitationKey: this.state.invitationKey
    };

    const args: FetchArgs = ProfileApiFetchParamCreator().invitationKeyVerification(request);
    fetch(fullUrl(args.url), args.options)
        .then(response => {
          if (response.status === 400 ) {
            this.setState({
              invitationKeyInvalid: true
            });
          } else {
            this.props.updateNext(2);
            return;
          }
        })
        .catch(error => {});
  }

  render() {
    return <div style={{padding: '3rem 3rem 0 3rem'}}>
      <div className='form-area'>
        <div className='form-section'>
          <BoldHeader>Enter your Invitation Key:</BoldHeader>
          <FormInput type='text' id='invitationKey' value={this.state.invitationKey}
                 placeholder='Invitation Key' onChange={this.updateInvitationKey} autoFocus/>
          {this.state.invitationKeyReq &&
          <div className='alert alert-danger'>
            <div style={{fontWeight: 'bolder'}}> Invitation Key is required.</div>
          </div>}
          {this.state.invitationKeyInvalid &&
           <div className='alert alert-danger'>
            <div style={{fontWeight: 'bolder'}}> Invitation Key is not Valid.</div>
            </div>}
          <div>
            <button className='btn btn-primary' onClick={() => this.next()}>
              Next
            </button>
          </div>
        </div>
      </div>
    </div>;
  }
}

export default InvitationKeyReact;

