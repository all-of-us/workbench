import {AlertDanger} from 'app/components/alert';
import {Button} from 'app/components/buttons';
import {BoldHeader} from 'app/components/headers';
import {TextInput} from 'app/components/inputs';

import {profileApi} from 'app/services/swagger-fetch-clients';

import * as React from 'react';

function isBlank(s: string) {
  return (!s || /^\s*$/.test(s));
}

function isValidKeyFormat(k: string): boolean {
  return /^[\sa-zA-Z0-9]*$/.test(k);
}

export interface InvitationKeyProps {
  onInvitationKeyVerify: (invitationKey: any) => void;
}

interface InvitationKeyState {
  invitationKey: string;
  invitationKeyReq: boolean;
  invitationKeyInvalid: boolean;
}

export class InvitationKey extends React.Component<InvitationKeyProps, InvitationKeyState> {

  private inputElement = React.createRef<HTMLInputElement>();


  constructor(props: InvitationKeyProps) {
    super(props);
    this.state = {
      invitationKey: '',
      invitationKeyReq: false,
      invitationKeyInvalid: false
    };
  }

  next() {
    this.setState({
      invitationKeyReq: false,
      invitationKeyInvalid: false
    });
    const input = this.inputElement.current;
    if (input) {
      input.focus();
    }
    if (isBlank(this.state.invitationKey)) {
      this.setState({
        invitationKeyReq: true
      });
      return;
    }

    if (!isValidKeyFormat(this.state.invitationKey)) {
      this.setState({
        invitationKeyInvalid: true
      });
      if (input) {
        input.focus();
      }
      return;
    }

    profileApi()
      .invitationKeyVerification({invitationKey: this.state.invitationKey})
      .then(response => {
        this.props.onInvitationKeyVerify(this.state.invitationKey);
      })
      .catch(error => {
        this.setState({
          invitationKeyInvalid: true
        });
      });
  }

  keyPressed(key) {
    if (key === 'Enter') {
      this.next();
    }
  }

  render() {
    return <div data-test-id='invitationKey' style={{padding: '3rem 3rem 0 3rem'}}>
      <div style={{marginTop: '0', paddingTop: '.5rem'}}>
        <BoldHeader>
          Enter your Invitation Key:
        </BoldHeader>
        <TextInput id='invitationKey' value={this.state.invitationKey}
                   onKeyPress={(event) => this.keyPressed(event.key)}
                   style={{width: '16rem'}}
                   placeholder='Invitation Key'
          onChange={v => this.setState({invitationKey: v})}
                   ref={this.inputElement} autoFocus/>
        {this.state.invitationKeyReq &&
         <AlertDanger>
           <div style={{fontWeight: 'bolder'}}>Invitation Key is required.</div>
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

export default InvitationKey;
