import '@testing-library/jest-dom';

import * as React from 'react';

import { CohortBuilderApi } from 'generated/fetch';

import { render, screen, waitFor } from '@testing-library/react';
import {
  ppiQuestions,
  ppiSurveys,
} from 'app/pages/data/cohort/search-state.service';
import {
  cohortBuilderApi,
  registerApiClient,
} from 'app/services/swagger-fetch-clients';
import { currentWorkspaceStore } from 'app/utils/navigation';

import {
  expectButtonElementDisabled,
  expectButtonElementEnabled,
} from 'testing/react-test-helpers';
import {
  CohortBuilderServiceStub,
  CriteriaWithAttributesStubVariables,
  RootSurveyStubVariables,
  SurveyQuestionStubVariables,
} from 'testing/stubs/cohort-builder-service-stub';
import { workspaceDataStub } from 'testing/stubs/workspaces';

import { AttributesPage, AttributesPageProps } from './attributes-page';
import SpyInstance = jest.SpyInstance;
import userEvent from '@testing-library/user-event';

let props: AttributesPageProps;
let mockCountParticipants: SpyInstance;
let mockFindCriteriaAttributeByConceptId: SpyInstance;
let mockFindSurveyVersionByQuestionConceptId: SpyInstance;
let mockFindSurveyVersionByQuestionConceptIdAndAnswerConceptId: SpyInstance;

async function component() {
  render(<AttributesPage {...props} />);
  await waitFor(() =>
    expect(screen.queryByLabelText('Please Wait')).not.toBeInTheDocument()
  );
}

describe('AttributesPageV2', () => {
  beforeEach(() => {
    currentWorkspaceStore.next(workspaceDataStub);
    registerApiClient(CohortBuilderApi, new CohortBuilderServiceStub());
    mockCountParticipants = jest.spyOn(cohortBuilderApi(), 'countParticipants');
    mockFindCriteriaAttributeByConceptId = jest.spyOn(
      cohortBuilderApi(),
      'findCriteriaAttributeByConceptId'
    );
    mockFindSurveyVersionByQuestionConceptId = jest.spyOn(
      cohortBuilderApi(),
      'findSurveyVersionByQuestionConceptId'
    );
    mockFindSurveyVersionByQuestionConceptIdAndAnswerConceptId = jest.spyOn(
      cohortBuilderApi(),
      'findSurveyVersionByQuestionConceptIdAndAnswerConceptId'
    );
    props = {
      back: () => {},
      close: () => {},
      criteria: [],
      node: CriteriaWithAttributesStubVariables[0],
      workspace: workspaceDataStub,
    };
  });

  it('should render', async () => {
    component();

    expect(screen.getByText(/number of participants:/i)).toBeTruthy();
  });

  it('should not call api and render a single dropdown for Height in Physical Measurements', async () => {
    await component();
    expect(mockCountParticipants).toHaveBeenCalledTimes(0);
    expect(mockFindCriteriaAttributeByConceptId).toHaveBeenCalledTimes(0);
    expect(mockFindSurveyVersionByQuestionConceptId).toHaveBeenCalledTimes(0);
    expect(
      mockFindSurveyVersionByQuestionConceptIdAndAnswerConceptId
    ).toHaveBeenCalledTimes(0);
    screen.getByRole('button', {
      name: /select operator/i,
    });
    expect(screen.queryByRole('spinbutton')).not.toBeInTheDocument();
    expectButtonElementEnabled(
      screen.getByRole('button', {
        name: /add this/i,
      })
    );
  });

  it('should not call api and render two dropdowns for BP in Physical Measurements', async () => {
    props.node = CriteriaWithAttributesStubVariables[1];
    await component();
    expect(mockCountParticipants).toHaveBeenCalledTimes(0);
    expect(mockFindCriteriaAttributeByConceptId).toHaveBeenCalledTimes(0);
    expect(mockFindSurveyVersionByQuestionConceptId).toHaveBeenCalledTimes(0);
    expect(
      mockFindSurveyVersionByQuestionConceptIdAndAnswerConceptId
    ).toHaveBeenCalledTimes(0);
    const dropDowns = screen.getAllByRole('button', {
      name: /select operator/i,
    });
    expect(dropDowns.length).toBe(2);
    expect(screen.queryByRole('spinbutton')).not.toBeInTheDocument();
    expectButtonElementEnabled(
      screen.getByRole('button', {
        name: /add this/i,
      })
    );
  });

  it('should call api for attributes for Labs and Measurements nodes', async () => {
    props.node = CriteriaWithAttributesStubVariables[2];
    await component();
    expect(mockCountParticipants).toHaveBeenCalledTimes(0);
    expect(mockFindCriteriaAttributeByConceptId).toHaveBeenCalledTimes(1);
    expect(mockFindSurveyVersionByQuestionConceptId).toHaveBeenCalledTimes(0);
    expect(
      mockFindSurveyVersionByQuestionConceptIdAndAnswerConceptId
    ).toHaveBeenCalledTimes(0);
  });

  it('should call api for attributes for non COPE Survey nodes', async () => {
    ppiSurveys.next({
      [workspaceDataStub.cdrVersionId]: RootSurveyStubVariables,
    });
    ppiQuestions.next(SurveyQuestionStubVariables);
    props.node = CriteriaWithAttributesStubVariables[3];
    await component();
    expect(mockCountParticipants).toHaveBeenCalledTimes(0);
    expect(mockFindCriteriaAttributeByConceptId).toHaveBeenCalledTimes(1);
    expect(mockFindSurveyVersionByQuestionConceptId).toHaveBeenCalledTimes(0);
    expect(
      mockFindSurveyVersionByQuestionConceptIdAndAnswerConceptId
    ).toHaveBeenCalledTimes(0);
  });

  it('should call api for survey versions for COPE questions', async () => {
    ppiSurveys.next({
      [workspaceDataStub.cdrVersionId]: RootSurveyStubVariables,
    });
    ppiQuestions.next(SurveyQuestionStubVariables);
    props.node = CriteriaWithAttributesStubVariables[4];
    await component();
    expect(mockCountParticipants).toHaveBeenCalledTimes(0);
    expect(mockFindCriteriaAttributeByConceptId).toHaveBeenCalledTimes(0);
    expect(mockFindSurveyVersionByQuestionConceptId).toHaveBeenCalledTimes(1);
    expect(
      mockFindSurveyVersionByQuestionConceptIdAndAnswerConceptId
    ).toHaveBeenCalledTimes(0);
  });

  it('should call api for attributes and survey versions (Question and Answer conceptId call) for COPE answers', async () => {
    ppiSurveys.next({
      [workspaceDataStub.cdrVersionId]: RootSurveyStubVariables,
    });
    ppiQuestions.next(SurveyQuestionStubVariables);
    props.node = CriteriaWithAttributesStubVariables[5];
    await component();
    expect(mockCountParticipants).toHaveBeenCalledTimes(0);
    expect(mockFindCriteriaAttributeByConceptId).toHaveBeenCalledTimes(1);
    expect(mockFindSurveyVersionByQuestionConceptId).toHaveBeenCalledTimes(0);
    expect(
      mockFindSurveyVersionByQuestionConceptIdAndAnswerConceptId
    ).toHaveBeenCalledTimes(1);
  });

  it('should call api for attributes and survey versions (Question conceptId call) for COPE Select a Value answers', async () => {
    ppiSurveys.next({
      [workspaceDataStub.cdrVersionId]: RootSurveyStubVariables,
    });
    ppiQuestions.next(SurveyQuestionStubVariables);
    props.node = CriteriaWithAttributesStubVariables[6];
    await component();
    expect(mockCountParticipants).toHaveBeenCalledTimes(0);
    expect(mockFindCriteriaAttributeByConceptId).toHaveBeenCalledTimes(1);
    expect(mockFindSurveyVersionByQuestionConceptId).toHaveBeenCalledTimes(1);
    expect(
      mockFindSurveyVersionByQuestionConceptIdAndAnswerConceptId
    ).toHaveBeenCalledTimes(0);
  });

  it('should render a single input for EQUAL operator and disable calculate button when empty', async () => {
    const user = userEvent.setup();
    await component();

    screen.getByDisplayValue(/any value/i);
    // Simulate the dropdown change
    const dropdown = screen.getByRole('button', { name: /select operator/i });
    user.click(dropdown);
    const equalOption = await screen.findByText('Equals');
    user.click(equalOption);
    await screen.findByDisplayValue('Equals');
    expect(screen.queryByDisplayValue(/any value/i)).not.toBeInTheDocument();

    // Check that the input field is rendered
    const numericalInput = screen.getByRole('spinbutton');
    expect(numericalInput).toBeInTheDocument();

    // Check that the calculate button is disabled
    const calculateButton = screen.getByRole('button', { name: /calculate/i });
    expectButtonElementDisabled(calculateButton);

    // Simulate the input change
    user.type(numericalInput, '100');

    await waitFor(() => expectButtonElementEnabled(calculateButton));

    user.click(calculateButton);

    await waitFor(() => expect(mockCountParticipants).toHaveBeenCalledTimes(1));
  });
});
