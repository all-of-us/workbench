import '@testing-library/jest-dom';

import * as React from 'react';

import {
  CreateNewUserSatisfactionSurvey,
  NewUserSatisfactionSurveySatisfaction,
  SurveysApi,
} from 'generated/fetch';

import {
  fireEvent,
  render,
  RenderResult,
  waitFor,
} from '@testing-library/react';
import { registerApiClient } from 'app/services/swagger-fetch-clients';
import { createNewUserSatisfactionSurveyStore } from 'app/utils/stores';

import {
  expectButtonElementDisabled,
  expectButtonElementEnabled,
} from 'testing/react-test-helpers';
import { SurveysApiStub } from 'testing/stubs/surveys-api-stub';

import {
  ADDITIONAL_INFO_MAX_CHARACTERS,
  NewUserSatisfactionSurveyModal,
} from './new-user-satisfaction-survey-modal';

describe(NewUserSatisfactionSurveyModal.name, () => {
  const setNewUserSatisfactionSurveyData = <
    T extends keyof CreateNewUserSatisfactionSurvey
  >(
    attribute: T,
    value: CreateNewUserSatisfactionSurvey[T]
  ) => {
    createNewUserSatisfactionSurveyStore.set({
      newUserSatisfactionSurveyData: {
        ...createNewUserSatisfactionSurveyStore.get()
          .newUserSatisfactionSurveyData,
        [attribute]: value,
      },
    });
  };

  const setValidNewUserSatisfactionSurveyData = () => {
    setNewUserSatisfactionSurveyData(
      'satisfaction',
      NewUserSatisfactionSurveySatisfaction.VERY_SATISFIED
    );
    setNewUserSatisfactionSurveyData('additionalInfo', '');
  };

  beforeEach(() => {
    registerApiClient(SurveysApi, new SurveysApiStub());
    setValidNewUserSatisfactionSurveyData();
  });

  const renderTestComponent = ({
    onCancel = () => {},
    onSubmitSuccess = () => {},
    createSurveyApiCall = () => Promise.resolve(),
  } = {}) => {
    return render(
      <NewUserSatisfactionSurveyModal
        {...{ onCancel, onSubmitSuccess, createSurveyApiCall }}
      />
    );
  };

  const getSubmitButton = (component: RenderResult) => {
    return component.getByRole('button', { name: 'submit' });
  };

  it('should disable the submit button until a satisfaction value is selected', async () => {
    setNewUserSatisfactionSurveyData('satisfaction', undefined);

    const component = renderTestComponent();
    expectButtonElementDisabled(getSubmitButton(component));

    setNewUserSatisfactionSurveyData(
      'satisfaction',
      NewUserSatisfactionSurveySatisfaction.VERY_SATISFIED
    );
    await waitFor(() => expectButtonElementEnabled(getSubmitButton(component)));
  });

  it('should disable the submit button if additional info exceeds the max length', () => {
    setNewUserSatisfactionSurveyData(
      'additionalInfo',
      'A'.repeat(ADDITIONAL_INFO_MAX_CHARACTERS + 1)
    );
    let component = renderTestComponent();
    expectButtonElementDisabled(getSubmitButton(component));
    component.unmount();

    setNewUserSatisfactionSurveyData(
      'additionalInfo',
      'A'.repeat(ADDITIONAL_INFO_MAX_CHARACTERS)
    );
    component = renderTestComponent();
    expectButtonElementEnabled(getSubmitButton(component));
  });

  it('should disable the submit button while awaiting submission response', async () => {
    setNewUserSatisfactionSurveyData(
      'satisfaction',
      NewUserSatisfactionSurveySatisfaction.VERY_SATISFIED
    );

    // Allows the createSurveyApiCall to remain unresolved until resolveSubmit is called.
    let resolveSubmit;
    const createSurveyApiCall: () => Promise<void> = () =>
      new Promise((resolve) => {
        resolveSubmit = resolve;
      });

    const component = renderTestComponent({
      createSurveyApiCall,
    });

    expectButtonElementEnabled(getSubmitButton(component));

    fireEvent.click(getSubmitButton(component));
    // the promise is not yet resolved, so the button should be disabled
    expectButtonElementDisabled(getSubmitButton(component));

    resolveSubmit();
    await waitFor(() => expectButtonElementEnabled(getSubmitButton(component)));
  });

  it('should display an error on API failure and remove it on API success', async () => {
    const createSurveyApiCall = jest.fn();
    const component = renderTestComponent({
      createSurveyApiCall,
    });

    const queryForError = () =>
      component.queryByText(/There was an error processing your request/);

    expect(queryForError()).not.toBeInTheDocument();

    createSurveyApiCall.mockImplementationOnce(() => Promise.reject());
    fireEvent.click(getSubmitButton(component));
    await waitFor(() => expect(queryForError()).toBeInTheDocument());

    createSurveyApiCall.mockImplementationOnce(() => Promise.resolve());
    fireEvent.click(getSubmitButton(component));
    await waitFor(() => expect(queryForError()).not.toBeInTheDocument());
  });
});
