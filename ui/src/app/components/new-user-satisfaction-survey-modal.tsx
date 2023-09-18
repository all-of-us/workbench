import * as React from 'react';
import { useState } from 'react';
import validate from 'validate.js';

import {
  CreateNewUserSatisfactionSurvey,
  NewUserSatisfactionSurveySatisfaction,
} from 'generated/fetch';

import { Button } from 'app/components/buttons';
import { FlexColumn, FlexRow } from 'app/components/flex';
import {
  ErrorMessage,
  TextAreaWithLengthValidationMessage,
} from 'app/components/inputs';
import { Modal } from 'app/components/modals';
import { MultipleChoiceQuestion } from 'app/components/multiple-choice-question';
import { TooltipTrigger } from 'app/components/popups';
import { surveyStyles } from 'app/components/surveys';
import {
  createNewUserSatisfactionSurveyStore,
  useStore,
} from 'app/utils/stores';

const orderedSatisfactionOptions = [
  {
    value: NewUserSatisfactionSurveySatisfaction.VERY_UNSATISFIED,
    label: 'Very unsatisfied',
  },
  {
    value: NewUserSatisfactionSurveySatisfaction.UNSATISFIED,
    label: 'Unsatisfied',
  },
  {
    value: NewUserSatisfactionSurveySatisfaction.NEUTRAL,
    label: 'Neutral',
  },
  {
    value: NewUserSatisfactionSurveySatisfaction.SATISFIED,
    label: 'Satisfied',
  },
  {
    value: NewUserSatisfactionSurveySatisfaction.VERYSATISFIED,
    label: 'Very satisfied',
  },
];

export const ADDITIONAL_INFO_MAX_CHARACTERS = 500;

export interface NewUserSatisfactionSurveyModalProps {
  onCancel: () => void;
  onSubmitSuccess: () => void;
  createSurveyApiCall: (
    newUserSatisfactionSurveyData: CreateNewUserSatisfactionSurvey
  ) => Promise<any>;
}

const validationCheck = {
  satisfaction: {
    presence: {
      allowEmpty: false,
      message: "^Satisfaction rating can't be blank",
    },
  },
  additionalInfo: {
    length: {
      maximum: ADDITIONAL_INFO_MAX_CHARACTERS,
    },
  },
};

export const NewUserSatisfactionSurveyModal = ({
  onCancel,
  onSubmitSuccess,
  createSurveyApiCall,
}: NewUserSatisfactionSurveyModalProps) => {
  const { newUserSatisfactionSurveyData } = useStore(
    createNewUserSatisfactionSurveyStore
  );
  const [submittingRequest, setSubmittingRequest] = useState(false);
  const [error, setError] = useState(false);

  const validationErrors = validate(
    newUserSatisfactionSurveyData,
    validationCheck
  );

  return (
    <Modal
      width={850}
      onRequestClose={onCancel}
      shouldCloseOnOverlayClick={false}
    >
      <FlexColumn style={{ gap: '0.75rem' }}>
        <MultipleChoiceQuestion
          question='How would you rate your overall satisfaction with the Researcher Workbench?'
          options={orderedSatisfactionOptions}
          selected={newUserSatisfactionSurveyData.satisfaction}
          onChange={(satisfaction: NewUserSatisfactionSurveySatisfaction) => {
            createNewUserSatisfactionSurveyStore.set({
              newUserSatisfactionSurveyData: {
                ...newUserSatisfactionSurveyData,
                satisfaction: satisfaction,
              },
            });
          }}
          questionStyle={{ marginBottom: '0.75rem' }}
        />
        <label
          htmlFor='new-user-satisfaction-survey-additional-info'
          style={surveyStyles.question}
        >
          Please explain your selection. What were you expecting? What went
          well? What didn't go well?
        </label>

        <TextAreaWithLengthValidationMessage
          id='new-user-satisfaction-survey-additional-info'
          onChange={(additionalInfo) => {
            createNewUserSatisfactionSurveyStore.set({
              newUserSatisfactionSurveyData: {
                ...newUserSatisfactionSurveyData,
                additionalInfo,
              },
            });
          }}
          initialText={newUserSatisfactionSurveyData.additionalInfo}
          textBoxStyleOverrides={{ width: '100%' }}
          heightOverride={{ height: '10.5rem' }}
          maxCharacters={ADDITIONAL_INFO_MAX_CHARACTERS}
        />
        <FlexRow style={{ justifyContent: 'flex-end' }}>
          {error && (
            <ErrorMessage>
              There was an error processing your request.
            </ErrorMessage>
          )}
        </FlexRow>
        <FlexRow style={{ justifyContent: 'flex-end', gap: '0.75rem' }}>
          <Button type='secondary' onClick={onCancel} aria-label='cancel'>
            Cancel
          </Button>
          <TooltipTrigger
            content={
              validationErrors && (
                <ul>
                  {
                    <>
                      {Object.entries(validationErrors).map(
                        ([attribute, errorMessages]) => (
                          <li key={attribute}>{errorMessages[0]}</li>
                        )
                      )}
                    </>
                  }
                </ul>
              )
            }
          >
            <Button
              type='primary'
              aria-label='submit'
              disabled={!!validationErrors || submittingRequest}
              onClick={async () => {
                setSubmittingRequest(true);
                try {
                  await createSurveyApiCall(newUserSatisfactionSurveyData);
                  setSubmittingRequest(false);
                  setError(false);
                  window.dispatchEvent(
                    new Event('new-user-satisfaction-survey-submitted')
                  );
                  onSubmitSuccess();
                } catch {
                  setSubmittingRequest(false);
                  setError(true);
                }
              }}
            >
              Submit
            </Button>
          </TooltipTrigger>
        </FlexRow>
      </FlexColumn>
    </Modal>
  );
};
