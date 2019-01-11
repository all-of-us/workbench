import {fullUrl} from '../../utils/fetch';

import {AlertDanger} from '../../components/alert';
import {Button} from '../../components/buttons';
import {BoldHeader} from '../../components/headers';
import {FormInput} from '../../components/inputs';

import {FetchArgs,
  InvitationVerificationRequest,
  ProfileApiFetchParamCreator
} from '../../../generated/fetch/api';

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

  inputElement: any;

  constructor(props: Object) {
    super(props);
    this.state = {
      invitationKey: '',
      invitationKeyReq: false,
      invitationKeyInvalid: false
    };
    this.inputElement = React.createRef();
    this.updateInvitationKey = this.updateInvitationKey.bind(this);
  }

  componentDidUpdate() {
    this.inputElement.current.focus();
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
          this.props.setInvitationKey(this.state.invitationKey);
          this.props.onInvitationkeyVerify();
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
                   placeholder='Invitation Key' onChange={this.updateInvitationKey}
                   inputref={this.inputElement}
                   autoFocus/>
        {this.state.invitationKeyReq &&
         <AlertDanger>
           <div style={{fontWeight: 'bolder'}}> Invitation Key is required.</div>
         </AlertDanger>
        }
        {this.state.invitationKeyInvalid &&
         <AlertDanger>
           <div style={{fontWeight: 'bolder'}}>
             Invitation Key is not Valid.
           </div>
         </AlertDanger>
        }
        <div>
          <Button style={{width: '10rem', height: '2rem', margin: '.25rem .5rem .25rem 0'}}
                  onClick={() => this.next()}>
            Next
          </Button>
        </div>
      </div>
    </div>;
  }
}

export default InvitationKeyReact;
