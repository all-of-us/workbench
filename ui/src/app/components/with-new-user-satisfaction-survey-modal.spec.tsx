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
    const { getByTestId } = render(
      <MemoryRouter initialEntries={[path]}>
        <Component />
      </MemoryRouter>
    );
    await waitFor(() => getByTestId('component'));
    return getByTestId;
  };

  const createWrapperWithValidCode = (code: string) => {
    jest
      .spyOn(surveysApi(), 'validateOneTimeCodeForNewUserSatisfactionSurvey')
      .mockImplementationOnce(() => Promise.resolve(true));
    return createWrapperAtPath(`?surveyCode=${code}`);
  };

  it('should show the modal if the code query parameter is valid', async () => {
    const code = 'abc';
    const validationMock = jest
      .spyOn(surveysApi(), 'validateOneTimeCodeForNewUserSatisfactionSurvey')
      .mockImplementationOnce(() => Promise.resolve(true));
    const getByTestId = await createWrapperAtPath(`?surveyCode=${code}`);
    expect(validationMock).toHaveBeenCalledWith(code);
    expect(
      getByTestId('new-user-satisfaction-survey-modal')
    ).toBeInTheDocument();
  });

  it('should not show the modal if the code query parameter is not present', async () => {
    const getByTestId = await createWrapperAtPath('');
    expect(
      getByTestId('new-user-satisfaction-survey-modal')
    ).not.toBeInTheDocument();
  });

  it('should not show the modal if the code query parameter is invalid', async () => {
    const code = 'abc';
    jest
      .spyOn(surveysApi(), 'validateOneTimeCodeForNewUserSatisfactionSurvey')
      .mockImplementationOnce(() => Promise.resolve(false));
    const getByTestId = await createWrapperAtPath(`?surveyCode=${code}`);
    expect(
      getByTestId('new-user-satisfaction-survey-modal')
    ).not.toBeInTheDocument();
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
    expect(notificationStore.get().title).toBeTruthy();
    expect(notificationStore.get().message).toBeTruthy();
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
    const getByTestId = await createWrapperWithValidCode(code);
    fireEvent.click(getByTestId('new-user-satisfaction-survey-modal-submit'));
    expect(validationMock).toHaveBeenCalledWith({
      createNewUserSatisfactionSurvey: surveyData,
      oneTimeCode: code,
    });
  });
});
