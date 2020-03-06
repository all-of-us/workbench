import {AlertDanger} from 'app/components/alert';
import {Button} from 'app/components/buttons';
import {BoldHeader} from 'app/components/headers';
import {TextInput} from 'app/components/inputs';

import {profileApi} from 'app/services/swagger-fetch-clients';

import {SpinnerOverlay} from 'app/components/spinners';
import * as React from 'react';

function isBlank(s: string) {
  return (!s || /^\s*$/.test(s));
}

function isValidKeyFormat(k: string): boolean {
  return /^[\sa-zA-Z0-9]*$/.test(k);
}

export interface InvitationKeyProps {
  onInvitationKeyVerified: (invitationKey: any) => void;
}

interface InvitationKeyState {
  invitationKey: string;
  loading: boolean;
  invitationKeyRequired: boolean;
  invitationKeyInvalid: boolean;
}

export class InvitationKey extends React.Component<InvitationKeyProps, InvitationKeyState> {

  private inputElement = React.createRef<HTMLInputElement>();


  constructor(props: InvitationKeyProps) {
    super(props);
    this.state = {
      invitationKey: '',
      loading: false,
      invitationKeyRequired: false,
      invitationKeyInvalid: false
    };
  }

  checkInvitationKey() {
    this.setState({
      loading: true,
      invitationKeyRequired: false,
      invitationKeyInvalid: false
    });
    const input = this.inputElement.current;
    if (input) {
      input.focus();
    }
    if (isBlank(this.state.invitationKey)) {
      this.setState({
        invitationKeyRequired: true
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
        this.setState({loading: false});
        this.props.onInvitationKeyVerified(this.state.invitationKey);
      })
      .catch(error => {
        this.setState({
          loading: false,
          invitationKeyInvalid: true
        });
      });
  }

  keyPressed(key) {
    if (key === 'Enter') {
      this.checkInvitationKey();
    }
  }

  render() {
    const {loading, invitationKey, invitationKeyInvalid, invitationKeyRequired} = this.state;
    return <div data-test-id='invitationKey' style={{padding: '3rem 3rem 0 3rem'}}>
      <div style={{marginTop: '0', paddingTop: '.5rem'}}>
        <BoldHeader>
          Enter your Invitation Key:
        </BoldHeader>
        <TextInput id='invitationKey' value={invitationKey}
                   onKeyPress={(event) => this.keyPressed(event.key)}
                   style={{width: '16rem'}}
                   placeholder='Invitation Key'
          onChange={v => this.setState({invitationKey: v})}
                   ref={this.inputElement} autoFocus/>
        {invitationKeyRequired &&
         <AlertDanger>
           <div style={{fontWeight: 'bolder'}}>Invitation Key is required.</div>
         </AlertDanger>
        }
        {invitationKeyInvalid &&
        <AlertDanger>
            <div style={{fontWeight: 'bolder'}}>
                Invitation Key is not Valid.
            </div>
        </AlertDanger>
        }
        <div>
          <Button style={{width: '10rem', height: '2rem', margin: '.25rem .5rem .25rem 0'}}
                  onClick={() => this.checkInvitationKey()}>
            Next
          </Button>
        </div>
      </div>
      {loading && <SpinnerOverlay />}
    </div>;
  }
}

export default InvitationKey;
