import * as React from 'react';
import { useState } from 'react';

import { NewUserSatisfactionSurveySatisfaction } from 'generated/fetch';

import { Button } from 'app/components/buttons';
import { FlexColumn, FlexRow } from 'app/components/flex';
import {
  ErrorMessage,
  TextAreaWithLengthValidationMessage,
} from 'app/components/inputs';
import { Modal } from 'app/components/modals';
import { MultipleChoiceQuestion } from 'app/components/multiple-choice-question';
import { surveysApi } from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import {
  createNewUserSatisfactionSurveyStore,
  useStore,
} from 'app/utils/stores';

const orderedSatisfactionOptions = [
  {
    value: NewUserSatisfactionSurveySatisfaction.VERYUNSATISFIED,
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
}

export const NewUserSatisfactionSurveyModal = ({
  onCancel,
  onSubmitSuccess,
}: NewUserSatisfactionSurveyModalProps) => {
  const { newUserSatisfactionSurveyData } = useStore(
    createNewUserSatisfactionSurveyStore
  );
  const [submittingRequest, setSubmittingRequest] = useState(false);
  const [error, setError] = useState(false);

  return (
    <Modal width={850} onRequestClose={onCancel}>
      <FlexColumn style={{ gap: '0.5rem' }}>
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
          questionStyle={{ marginBottom: '0.5rem' }}
        />
        <label
          htmlFor='new-user-satisfaction-survey-additional-info'
          style={{
            fontWeight: 'bold',
            color: colors.primary,
            fontSize: '18px',
            lineHeight: '1.25rem',
          }}
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
          heightOverride={{ height: '7rem' }}
          maxCharacters={ADDITIONAL_INFO_MAX_CHARACTERS}
        />
        <FlexRow style={{ justifyContent: 'flex-end' }}>
          {error && (
            <ErrorMessage>
              There was an error processing your request.
            </ErrorMessage>
          )}
        </FlexRow>
        <FlexRow style={{ justifyContent: 'flex-end', gap: '0.5rem' }}>
          <Button type='secondary' onClick={onCancel}>
            Cancel
          </Button>
          <Button
            type='primary'
            disabled={
              newUserSatisfactionSurveyData.satisfaction === undefined ||
              newUserSatisfactionSurveyData.additionalInfo.length > 500 ||
              submittingRequest
            }
            onClick={async () => {
              setSubmittingRequest(true);
              try {
                await surveysApi().createNewUserSatisfactionSurvey(
                  newUserSatisfactionSurveyData
                );
                setError(false);
                onSubmitSuccess();
              } catch {
                setError(true);
              } finally {
                setSubmittingRequest(false);
              }
            }}
          >
            Submit
          </Button>
        </FlexRow>
      </FlexColumn>
    </Modal>
  );
};
