import * as React from 'react';

import {Button} from 'app/components/buttons';
import {FlexColumn, FlexRow} from 'app/components/flex';
import {FormSection} from 'app/components/forms';
import {CheckBox, RadioButton} from 'app/components/inputs';
import {Modal, ModalBody, ModalFooter, ModalTitle} from 'app/components/modals';
import {TooltipTrigger} from 'app/components/popups';
import {SpinnerOverlay} from 'app/components/spinners';
import {TextColumn} from 'app/components/text-column';
import {AouTitle} from 'app/components/text-wrappers';
import {AccountCreationOptions} from 'app/pages/login/account-creation/account-creation-options';
import {DropDownSection, Section, TextInputWithLabel} from 'app/pages/login/account-creation/common';
import {profileApi} from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import {toggleIncludes} from 'app/utils';
import {convertAPIError, reportError} from 'app/utils/errors';
import {serverConfigStore} from 'app/utils/navigation';
import {environment} from 'environments/environment';
import {ErrorResponse, Profile} from 'generated/fetch';
import ReCAPTCHA from 'react-google-recaptcha';


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
      />
    </React.Fragment>
  }
}
