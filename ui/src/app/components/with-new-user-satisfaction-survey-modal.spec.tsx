import * as React from 'react';
import { MemoryRouter } from 'react-router';
import { mount } from 'enzyme';

import { SurveysApi } from 'generated/fetch';

import { withNewUserSatisfactionSurveyModal } from 'app/components/with-new-user-satisfaction-survey-modal-wrapper';
import {
  registerApiClient,
  surveysApi,
} from 'app/services/swagger-fetch-clients';
import { notificationStore } from 'app/utils/stores';

import { waitOneTickAndUpdate } from 'testing/react-test-helpers';
import { SurveysApiStub } from 'testing/stubs/surveys-api-stub';

import { NewUserSatisfactionSurveyModal } from './new-user-satisfaction-survey-modal';

describe('withNewUserSatisfactionSurveyModal', () => {
  beforeEach(() => {
    registerApiClient(SurveysApi, new SurveysApiStub());
  });

  const createSurveyData = () => {
    return {
      satisfaction: undefined,
      additionalInfo: '',
    };
  };

  const createWrapperAtPath = async (path: string) => {
    const Component = withNewUserSatisfactionSurveyModal(() => <div />);
    const wrapper = mount(
      <MemoryRouter initialEntries={[path]}>
        <Component />
      </MemoryRouter>
    );
    await waitOneTickAndUpdate(wrapper);
    return wrapper;
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

    const wrapper = await createWrapperAtPath(`?surveyCode=${code}`);

    expect(validationMock.mock.calls[0][0]).toEqual(code);
    expect(wrapper.find(NewUserSatisfactionSurveyModal).exists()).toBeTruthy();
  });

  it('should not show the modal if the code query parameter is not present', async () => {
    const wrapper = await createWrapperAtPath('');

    expect(wrapper.find(NewUserSatisfactionSurveyModal).exists()).toBeFalsy();
  });

  it('should not show the modal if the code query parameter is invalid', async () => {
    const code = 'abc';
    jest
      .spyOn(surveysApi(), 'validateOneTimeCodeForNewUserSatisfactionSurvey')
      .mockImplementationOnce(() => Promise.resolve(false));

    const wrapper = await createWrapperAtPath(`?surveyCode=${code}`);

    expect(wrapper.find(NewUserSatisfactionSurveyModal).exists()).toBeFalsy();
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
    const surveyData = createSurveyData();
    const validationMock = jest
      .spyOn(surveysApi(), 'createNewUserSatisfactionSurveyWithOneTimeCode')
      .mockImplementationOnce(() => Promise.resolve(undefined));

    const wrapper = await createWrapperWithValidCode(code);
    await wrapper
      .find(NewUserSatisfactionSurveyModal)
      .prop('createSurveyApiCall')(surveyData);

    expect(validationMock.mock.calls[0][0]).toEqual({
      createNewUserSatisfactionSurvey: surveyData,
      oneTimeCode: code,
    });
  });
});
