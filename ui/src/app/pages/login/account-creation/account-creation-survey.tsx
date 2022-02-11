import * as React from 'react';
import * as fp from 'lodash/fp';

import { Profile } from 'generated/fetch';

import { environment } from 'environments/environment';
import { DemographicSurvey } from 'app/pages/profile/demographic-survey';
import { profileApi } from 'app/services/swagger-fetch-clients';
import { AnalyticsTracker } from 'app/utils/analytics';

export interface AccountCreationSurveyProps {
  termsOfServiceVersion?: number;
  profile: Profile;
  onComplete: (profile: Profile) => void;
  onPreviousClick: (profile: Profile) => void;
}

export interface AccountCreationState {
  captcha: boolean;
}

export class AccountCreationSurvey extends React.Component<
  AccountCreationSurveyProps,
  AccountCreationState
> {
  constructor(props: any) {
    super(props);
    this.state = {
      captcha: !environment.enableCaptcha,
    };
    this.createAccount = this.createAccount.bind(this);
  }

  async createAccount(profile, captchaToken): Promise<Profile> {
    const { termsOfServiceVersion, onComplete } = this.props;

    const newProfile = await profileApi().createAccount({
      profile: profile,
      captchaVerificationToken: captchaToken,
      termsOfServiceVersion: termsOfServiceVersion,
    });
    onComplete(newProfile);
    return newProfile;
  }

  render() {
    return (
      <DemographicSurvey
        profile={fp.set(
          'demographicSurvey',
          fp.mapValues(() => undefined, this.props.profile.demographicSurvey),
          this.props.profile
        )}
        saveProfile={(profile, captchaToken) => {
          AnalyticsTracker.Registration.DemographicSurvey();
          return this.createAccount(profile, captchaToken);
        }}
        onPreviousClick={(profile) => this.props.onPreviousClick(profile)}
        enableCaptcha={true}
        enablePrevious={true}
        showStepCount={true}
      />
    );
  }
}
