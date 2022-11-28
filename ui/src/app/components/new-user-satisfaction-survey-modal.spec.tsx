import * as React from 'react';
import { shallow, ShallowWrapper } from 'enzyme';

import {
  CreateNewUserSatisfactionSurvey,
  NewUserSatisfactionSurveySatisfaction,
  SurveysApi,
} from 'generated/fetch';

import { ErrorMessage } from 'app/components/inputs';
import {
  registerApiClient,
  surveysApi,
} from 'app/services/swagger-fetch-clients';
import { createNewUserSatisfactionSurveyStore } from 'app/utils/stores';

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
      NewUserSatisfactionSurveySatisfaction.VERYSATISFIED
    );
    setNewUserSatisfactionSurveyData('additionalInfo', '');
  };

  beforeEach(() => {
    registerApiClient(SurveysApi, new SurveysApiStub());
    setValidNewUserSatisfactionSurveyData();
  });

  const createShallowWrapper = ({
    onCancel = () => {},
    onSubmitSuccess = () => {},
  } = {}) => {
    return shallow(
      <NewUserSatisfactionSurveyModal
        onCancel={onCancel}
        onSubmitSuccess={onSubmitSuccess}
      />
    );
  };

  const findSubmitButton = (wrapper: ShallowWrapper) => {
    return wrapper.find({ children: 'Submit' });
  };

  it('should disable the submit button until a satisfaction value is selected', () => {
    setNewUserSatisfactionSurveyData('satisfaction', undefined);
    expect(
      findSubmitButton(createShallowWrapper()).prop('disabled')
    ).toBeTruthy();

    setNewUserSatisfactionSurveyData(
      'satisfaction',
      NewUserSatisfactionSurveySatisfaction.VERYSATISFIED
    );
    expect(
      findSubmitButton(createShallowWrapper()).prop('disabled')
    ).toBeFalsy();
  });

  it('should disable the submit button if additional info exceeds the max length', () => {
    setNewUserSatisfactionSurveyData(
      'additionalInfo',
      'A'.repeat(ADDITIONAL_INFO_MAX_CHARACTERS + 1)
    );
    expect(
      findSubmitButton(createShallowWrapper()).prop('disabled')
    ).toBeTruthy();

    setNewUserSatisfactionSurveyData(
      'additionalInfo',
      'A'.repeat(ADDITIONAL_INFO_MAX_CHARACTERS)
    );
    expect(
      findSubmitButton(createShallowWrapper()).prop('disabled')
    ).toBeFalsy();
  });

  it('should disable the submit button while awaiting submission response', async () => {
    const wrapper = createShallowWrapper();
    expect(findSubmitButton(wrapper).prop('disabled')).toBeFalsy();

    const submitPromise = findSubmitButton(wrapper).prop('onClick')();
    expect(findSubmitButton(wrapper).prop('disabled')).toBeTruthy();

    await submitPromise;
    expect(findSubmitButton(wrapper).prop('disabled')).toBeFalsy();
  });

  it('should display an error on API failure and remove it on API success', async () => {
    const wrapper = createShallowWrapper();
    expect(wrapper.find(ErrorMessage).exists()).toBeFalsy();

    jest
      .spyOn(surveysApi(), 'createNewUserSatisfactionSurvey')
      .mockImplementationOnce(() => Promise.reject());
    await findSubmitButton(wrapper).prop('onClick')();
    expect(wrapper.find(ErrorMessage).exists()).toBeTruthy();

    jest
      .spyOn(surveysApi(), 'createNewUserSatisfactionSurvey')
      .mockImplementationOnce(() => Promise.resolve(new Response()));
    await findSubmitButton(wrapper).prop('onClick')();
    expect(wrapper.find(ErrorMessage).exists()).toBeFalsy();
  });
});
