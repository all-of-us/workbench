import {fullUrl} from 'app/utils/fetch';

import {AlertDanger} from 'app/components/alert';
import {BoldHeader} from 'app/components/headers';
import {FormInput} from 'app/components/inputs';

import {NextButton} from './style';

import {FetchArgs,
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

  state: InvitationKeyState;

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
    this.setState({
      invitationKeyReq: false,
      invitationKeyInvalid: false
    });

    if (isBlank(this.state.invitationKey)) {
      this.setState({
        invitationKeyReq: true
      });
      return;
    }

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
        }})
      .catch(error => console.log(error));
  }

  render() {
    return <div style={{padding: '3rem 3rem 0 3rem'}}>
      <div style={{marginTop: '0', paddingTop: '.5rem'}}>
        <BoldHeader>
          Enter your Invitation Key:
        </BoldHeader>
        <FormInput type='text' id='invitationKey' value={this.state.invitationKey}
                   placeholder='Invitation Key' onChange={this.updateInvitationKey} autoFocus/>
        {
          this.state.invitationKeyReq &&
          <AlertDanger>
            <div style={{fontWeight: 'bolder'}}> Invitation Key is required.</div>
          </AlertDanger>
        }
        {
          this.state.invitationKeyInvalid &&
          <AlertDanger>
            <div style={{fontWeight: 'bolder'}}>
              Invitation Key is not Valid.
            </div>
          </AlertDanger>
        }
        <div>
          <NextButton onClick={() => this.next()}>
            Next
          </NextButton>
        </div>
      </div>
    </div>;
  }
}

export default InvitationKeyReact;
