import '@testing-library/jest-dom';

import * as React from 'react';
import { MemoryRouter } from 'react-router';

import { SurveysApi } from 'generated/fetch';

import { fireEvent, render, waitFor } from '@testing-library/react';
import { withNewUserSatisfactionSurveyModal } from 'app/components/with-new-user-satisfaction-survey-modal-wrapper';
import {
  registerApiClient,
  surveysApi,
} from 'app/services/swagger-fetch-clients';
import { notificationStore } from 'app/utils/stores';

import { SurveysApiStub } from 'testing/stubs/surveys-api-stub';

describe(withNewUserSatisfactionSurveyModal.name, () => {
  beforeEach(() => {
    registerApiClient(SurveysApi, new SurveysApiStub());
  });

  const createWrapperAtPath = async (path: string) => {
    const Component = withNewUserSatisfactionSurveyModal(() => <div />);
    return render(
      <MemoryRouter initialEntries={[path]}>
        <Component />
      </MemoryRouter>
    );
  };

  const createWrapperWithValidCode = (code: string) => {
    jest
      .spyOn(surveysApi(), 'validateOneTimeCodeForNewUserSatisfactionSurvey')
      .mockImplementationOnce(() => Promise.resolve(true));
    return createWrapperAtPath(`?surveyCode=${code}`);
  };

  const overallQuestionText =
    'How would you rate your overall satisfaction with the Researcher Workbench?';

  it('should show the modal if the code query parameter is valid', async () => {
    const code = 'abc';
    const validationMock = jest
      .spyOn(surveysApi(), 'validateOneTimeCodeForNewUserSatisfactionSurvey')
      .mockImplementationOnce(() => Promise.resolve(true));
    const { queryByText } = await createWrapperAtPath(`?surveyCode=${code}`);
    expect(validationMock).toHaveBeenCalledWith(code);
    expect(queryByText(overallQuestionText)).toBeInTheDocument();
  });

  it('should not show the modal if the code query parameter is not present', async () => {
    const { queryByText } = await createWrapperAtPath('');
    expect(queryByText(overallQuestionText)).not.toBeInTheDocument();
  });

  it('should not show the modal if the code query parameter is invalid', async () => {
    const code = 'abc';
    jest
      .spyOn(surveysApi(), 'validateOneTimeCodeForNewUserSatisfactionSurvey')
      .mockImplementationOnce(() => Promise.resolve(false));
    const { queryByText } = await createWrapperAtPath(`?surveyCode=${code}`);
    expect(queryByText(overallQuestionText)).not.toBeInTheDocument();
  });

  it('should not show an error if the code query parameter validation request succeeds', async () => {
    const code = 'abc';
    jest
      .spyOn(surveysApi(), 'validateOneTimeCodeForNewUserSatisfactionSurvey')
      .mockImplementationOnce(() => Promise.resolve(true));
    await createWrapperAtPath(`?surveyCode=${code}`);
    expect(notificationStore.get()).toBeNull();
  });

  it('should show an error if the code query parameter validation request fails', async () => {
    const code = 'abc';
    jest
      .spyOn(surveysApi(), 'validateOneTimeCodeForNewUserSatisfactionSurvey')
      .mockImplementationOnce(() => Promise.reject());
    expect(notificationStore.get()).toBeNull();
    await createWrapperAtPath(`?surveyCode=${code}`);
    await waitFor(() => expect(notificationStore.get().title).toBeTruthy());
    await waitFor(() => expect(notificationStore.get().message).toBeTruthy());
  });

  it('should call create API with the code', async () => {
    const code = 'abc';
    const surveyData = {
      satisfaction: undefined,
      additionalInfo: '',
    };
    const validationMock = jest
      .spyOn(surveysApi(), 'createNewUserSatisfactionSurveyWithOneTimeCode')
      .mockImplementationOnce(() => Promise.resolve(undefined));
    const { queryByRole } = await createWrapperWithValidCode(code);
    fireEvent.click(queryByRole('button', { name: 'submit' }));
    await waitFor(() =>
      expect(validationMock).toHaveBeenCalledWith({
        createNewUserSatisfactionSurvey: surveyData,
        oneTimeCode: code,
      })
    );
  });
});
