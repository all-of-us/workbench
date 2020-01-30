import * as React from 'react';
import {BoldHeader} from 'app/components/headers';
import {Button} from 'app/components/buttons';

export interface AccountCreationTosProps {
  onComplete: (tosVersion: number) => void;
}

interface AccountCreationTosState {
  hasReadEntireTos: boolean;
  hasAckedPrivacyStatement: boolean;
  hasAckedTermsOfService: boolean;
}

export class AccountCreationTos extends React.Component<AccountCreationTosProps, AccountCreationTosState> {

  constructor(props: AccountCreationTosProps) {
    super(props);
    this.state = {
      hasReadEntireTos: false,
      hasAckedPrivacyStatement: false,
      hasAckedTermsOfService: false
    };
  }

  render() {
    return <div data-test-id='account-creation-tos' style={{padding: '3rem 3rem 0 3rem'}}>
      <div style={{marginTop: '0', paddingTop: '.5rem'}}>
        <BoldHeader>
          Agree to the Terms of Service:
        </BoldHeader>
        <div>
          <Button style={{width: '10rem', height: '2rem', margin: '.25rem .5rem .25rem 0'}}
                  onClick={() => this.props.onComplete(1)}>
            Next
          </Button>
        </div>
      </div>
    </div>;
  }
}

export default AccountCreationTos;
