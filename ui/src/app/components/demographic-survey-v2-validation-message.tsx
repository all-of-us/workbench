import * as React from 'react';
import * as fp from 'lodash/fp';

import { withProfileErrorModal } from 'app/components/with-error-modal-wrapper';

import {
  possiblePreferNotToAnswerErrors,
  questionsIndex,
} from './demographic-survey-v2';
import { WithSpinnerOverlayProps } from './with-spinner-overlay';

interface DemographicSurveyValidationMessageProps
  extends WithSpinnerOverlayProps {
  errors: any;
  isAccountCreation: boolean;
  captcha?: boolean;
  changed?: boolean;
}

const showPreferNotToAnswerMessage = (errors) =>
  Object.keys(errors).some((error) =>
    possiblePreferNotToAnswerErrors.includes(error)
  );
export const DemographicSurveyValidationMessage = fp.flow(
  withProfileErrorModal
)((props: DemographicSurveyValidationMessageProps) => {
  const { captcha, changed, errors, isAccountCreation } = props;
  if (errors) {
    console.error('DemographicSurveyValidationMessage errors:', errors);
  }
  return (
    <>
      <div>Please review the following:</div>
      <ul>
        {errors && (
          <>
            {Object.keys(errors).map((key) => (
              <li key={errors[key][0]}>
                {key}: {errors[key]} (Question {questionsIndex[key]})
              </li>
            ))}
            {showPreferNotToAnswerMessage(errors) && (
              <li>
                You may select "Prefer not to answer" for many items in this
                survey
              </li>
            )}
          </>
        )}
        {isAccountCreation && !captcha && (
          <li key='captcha'>Please fill out reCAPTCHA.</li>
        )}
        {!isAccountCreation && !changed && (
          <li>Your survey has not changed since your last submission.</li>
        )}
      </ul>
    </>
  );
});
