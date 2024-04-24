import '@testing-library/jest-dom';

import * as React from 'react';

import {
  CreateNewUserSatisfactionSurvey,
  NewUserSatisfactionSurveySatisfaction,
  SurveysApi,
} from 'generated/fetch';

import { fireEvent, render, waitFor } from '@testing-library/react';
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

  const createRenderResult = ({
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

  const findSubmitButton = (renderResult) => {
    return renderResult.getByRole('button', { name: 'submit' });
  };

  it('should disable the submit button until a satisfaction value is selected', async () => {
    setNewUserSatisfactionSurveyData('satisfaction', undefined);

    let component = createRenderResult();
    expectButtonElementDisabled(findSubmitButton(component));
    component.unmount();

    setNewUserSatisfactionSurveyData(
      'satisfaction',
      NewUserSatisfactionSurveySatisfaction.VERY_SATISFIED
    );
    component = createRenderResult();
    expectButtonElementEnabled(findSubmitButton(component));
  });

  it('should disable the submit button if additional info exceeds the max length', () => {
    setNewUserSatisfactionSurveyData(
      'additionalInfo',
      'A'.repeat(ADDITIONAL_INFO_MAX_CHARACTERS + 1)
    );
    let component = createRenderResult();
    expectButtonElementDisabled(findSubmitButton(component));
    component.unmount();

    setNewUserSatisfactionSurveyData(
      'additionalInfo',
      'A'.repeat(ADDITIONAL_INFO_MAX_CHARACTERS)
    );
    component = createRenderResult();
    expectButtonElementEnabled(findSubmitButton(component));
  });

  it('should disable the submit button while awaiting submission response', async () => {
    setNewUserSatisfactionSurveyData(
      'satisfaction',
      NewUserSatisfactionSurveySatisfaction.VERY_SATISFIED
    );

    // Allows the createSurveyApiCall to remain unresolved until resolveSubmit is called.
    let resolveSubmit: (value: void | PromiseLike<void>) => void;
    const createSurveyApiCall: () => Promise<void> = () =>
      new Promise((resolve) => {
        resolveSubmit = resolve;
      });

    const component = createRenderResult({
      createSurveyApiCall,
    });

    expectButtonElementEnabled(findSubmitButton(component));

    fireEvent.click(findSubmitButton(component));
    // the promise is not yet resolved, so the button should be disabled
    expectButtonElementDisabled(findSubmitButton(component));

    resolveSubmit();
    await waitFor(() =>
      expectButtonElementEnabled(findSubmitButton(component))
    );
  });

  it('should display an error on API failure and remove it on API success', async () => {
    const createSurveyApiCall = jest.fn();
    const component = createRenderResult({
      createSurveyApiCall,
    });

    const queryForError = () =>
      component.queryByText(/There was an error processing your request/);

    expect(queryForError()).not.toBeInTheDocument();

    createSurveyApiCall.mockImplementationOnce(() => Promise.reject());
    fireEvent.click(findSubmitButton(component));
    await waitFor(() => expect(queryForError()).toBeInTheDocument());

    createSurveyApiCall.mockImplementationOnce(() => Promise.resolve());
    fireEvent.click(findSubmitButton(component));
    await waitFor(() => expect(queryForError()).not.toBeInTheDocument());
  });
});
