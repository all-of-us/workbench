import * as React from 'react';

import { Profile } from 'generated/fetch';

import { Divider } from 'app/components/divider';
import { PageHeader } from 'app/components/headers';
import { TextColumn } from 'app/components/text-column';
import { AoU } from 'app/components/text-wrappers';
import {
  AccountCreationResendModal,
  AccountCreationUpdateModal,
} from 'app/pages/login/account-creation/account-creation-modals';
import colors from 'app/styles/colors';

const styles = {
  buttonLinkStyling: {
    border: 'none',
    cursor: 'pointer',
    outlineColor: 'transparent',
    color: colors.accent,
    backgroundColor: 'transparent',
  },
  borderStyle: {
    border: `1px solid ${colors.primary}`,
    color: colors.primary,
    borderRadius: '8px',
    marginLeft: '3.45rem',
    padding: '0.75rem',
    width: '28.5rem',
  },
};

interface AccountCreationSuccessProps {
  profile: Profile;
}

interface AccountCreationSuccessState {
  resendModal: boolean;
  updateModal: boolean;
  contactEmail: string;
}

export class AccountCreationSuccess extends React.Component<
  AccountCreationSuccessProps,
  AccountCreationSuccessState
> {
  constructor(props: AccountCreationSuccessProps) {
    super(props);
    this.state = {
      contactEmail: this.props.profile.contactEmail,
      resendModal: false,
      updateModal: false,
    };
  }

  render() {
    return (
      <React.Fragment>
        <div
          id='account-creation-success'
          style={{
            padding: '4.5rem 4.5rem 0 4.5rem',
            marginLeft: '-0.75rem',
            marginRight: '-0.5',
            width: '37.5rem',
          }}
        >
          <PageHeader>Congratulations!</PageHeader>

          <TextColumn>
            <div style={{ fontSize: '16px', marginTop: '.375rem' }}>
              Your <AoU /> research account has been created!
            </div>
          </TextColumn>

          <Divider verticalMargin='1.5rem' style={{ width: '100%' }} />

          <TextColumn style={{ fontSize: '20px', lineHeight: '30px' }}>
            <div>Your new research workbench account</div>
            <div>{this.props.profile.username}</div>
            <div>is hosted by Google.</div>
            <div style={{ marginTop: '1.5rem' }}>
              Check your contact email for
            </div>
            <div>instructions on getting started.</div>
          </TextColumn>

          <Divider verticalMargin='1.5rem' style={{ width: '100%' }} />

          <TextColumn>
            <div style={{ fontSize: '16px' }}>
              Your contact email is: {this.props.profile.contactEmail}
            </div>
            <a
              style={{ marginTop: '.375rem' }}
              onClick={() => this.setState({ resendModal: true })}
            >
              Resend Instructions
            </a>
          </TextColumn>
        </div>
        {this.state.resendModal && (
          <AccountCreationResendModal
            username={this.props.profile.username}
            creationNonce={this.props.profile.creationNonce}
            onClose={() => this.setState({ resendModal: false })}
          />
        )}
        {this.state.updateModal && (
          <AccountCreationUpdateModal
            username={this.props.profile.username}
            creationNonce={this.props.profile.creationNonce}
            onDone={(newEmail: string) => {
              this.setState({ contactEmail: newEmail, updateModal: false });
            }}
            onClose={() => this.setState({ updateModal: false })}
          />
        )}
        <div style={{ marginBottom: '.75rem', paddingTop: '1.5rem' }}>
          <div style={styles.borderStyle}>
            Please note: For full access to the Research Workbench data and
            tools, you'll be required to complete the necessary registration
            steps.
          </div>
        </div>
      </React.Fragment>
    );
  }
}
