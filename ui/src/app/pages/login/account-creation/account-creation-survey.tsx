import * as React from 'react';

import {withProfileErrorModal, WithProfileErrorModalProps} from 'app/components/with-error-modal';
import {DemographicSurvey} from 'app/pages/profile/demographic-survey';
import {profileApi} from 'app/services/swagger-fetch-clients';

import {AnalyticsTracker} from 'app/utils/analytics';
import {convertAPIError, reportError} from 'app/utils/errors';
import {environment} from 'environments/environment';
import {Profile} from 'generated/fetch';
import * as fp from 'lodash/fp';


export interface AccountCreationSurveyProps extends WithProfileErrorModalProps {
  invitationKey: string;
  termsOfServiceVersion?: number;
  profile: Profile;
  onComplete: (profile: Profile) => void;
  onPreviousClick: (profile: Profile) => void;
}

export interface AccountCreationState {
  captcha: boolean;
}

export const AccountCreationSurvey = withProfileErrorModal({title: 'Error creating account'})
(class extends React.Component<AccountCreationSurveyProps, AccountCreationState> {
  constructor(props: any) {
    super(props);
    this.state = {
      captcha: !environment.enableCaptcha
    };
    this.createAccount = this.createAccount.bind(this);
  }

  async createAccount(profile, captchaToken): Promise<Profile> {
    const {invitationKey, termsOfServiceVersion, onComplete} = this.props;

    try {
      const newProfile = await profileApi().createAccount({
        profile: profile,
        captchaVerificationToken: captchaToken,
        invitationKey: invitationKey,
        termsOfServiceVersion: termsOfServiceVersion
      });
      onComplete(newProfile);
      return newProfile;
    } catch (error) {
      reportError(error);
      const errorResponse = await convertAPIError(error);
      this.props.showProfileErrorModal(errorResponse.message);
    }
  }

  render() {
    return <React.Fragment>
      <DemographicSurvey
          profile={fp.set('demographicSurvey', fp.mapValues(() => undefined, this.props.profile.demographicSurvey), this.props.profile)}
          onSubmit={(profile, captchaToken) => {
            AnalyticsTracker.Registration.DemographicSurvey();
            return this.createAccount(profile, captchaToken);
          }}
          onPreviousClick={(profile) => this.props.onPreviousClick(profile)}
          enableCaptcha={true}
          enablePrevious={true}
          showStepCount={true}
      />
    </React.Fragment>;
  }
});
