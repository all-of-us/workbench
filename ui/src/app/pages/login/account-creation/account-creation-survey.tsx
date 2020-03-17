import * as React from 'react';

import {DemographicSurvey} from 'app/pages/profile/demographics-survey';
import {profileApi} from 'app/services/swagger-fetch-clients';

import {environment} from 'environments/environment';
import {Profile} from 'generated/fetch';


export interface AccountCreationSurveyProps {
  invitationKey: string;
  termsOfServiceVersion?: number;
  profile: Profile;
  onComplete: (profile: Profile) => void;
  onPreviousClick: (profile: Profile) => void;
}

export interface AccountCreationState {
  captcha: boolean;
}

export class AccountCreationSurvey extends React.Component<AccountCreationSurveyProps, AccountCreationState> {
  constructor(props: any) {
    super(props);
    this.state = {
      captcha: !environment.enableCaptcha
    };
    this.createAccount = this.createAccount.bind(this);
  }

  // TODO: we should probably bump this logic out of the survey component and either into its own
  // component or into the top-level SignIn component. The fact that we're awkwardly passing the
  // invitation key and tos version into this component (for the sole purpose of relaying this data
  // to the backend) is a telltale sign that this should be refactored.
  async createAccount(profile, captchaToken): Promise<Profile> {
    const {invitationKey, termsOfServiceVersion, onComplete} = this.props;

    const newProfile = await profileApi().createAccount({
      profile: profile,
      captchaVerificationToken: captchaToken,
      invitationKey: invitationKey,
      termsOfServiceVersion: termsOfServiceVersion
    });
    onComplete(newProfile);
    return newProfile;
  }

  render() {
    return <React.Fragment>
      <DemographicSurvey
          profile={this.props.profile}
          onSubmit={(profile, captchaToken) => this.createAccount(profile, captchaToken)}
          onPreviousClick={(profile) => this.props.onPreviousClick(profile)}
          enableCaptcha={true}
          enablePrevious={true}
      />
    </React.Fragment>;
  }
}
