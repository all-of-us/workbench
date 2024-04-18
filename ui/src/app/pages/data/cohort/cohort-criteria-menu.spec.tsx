import '@testing-library/jest-dom';

import * as React from 'react';

import { CohortBuilderApi } from 'generated/fetch';

import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {
  cohortBuilderApi,
  registerApiClient,
} from 'app/services/swagger-fetch-clients';
import { currentWorkspaceStore } from 'app/utils/navigation';

import { waitOneTickAndUpdate } from 'testing/react-test-helpers';
import { CohortBuilderServiceStub } from 'testing/stubs/cohort-builder-service-stub';
import { workspaceDataStub } from 'testing/stubs/workspaces';

import { CohortCriteriaMenu } from './cohort-criteria-menu';

describe('CohortCriteriaMenu', () => {
  const component = async () => {
    render(<CohortCriteriaMenu launchSearch={() => {}} menuOptions={[]} />);
  };

  beforeEach(() => {
    registerApiClient(CohortBuilderApi, new CohortBuilderServiceStub());
    currentWorkspaceStore.next({
      ...workspaceDataStub,
      cdrVersionId: '1',
    });
  });

  it('should render', async () => {
    await component();
    const addCriteriaButton = await screen.findByRole('button', {
      name: 'Add Criteria',
    });
    expect(addCriteriaButton).toBeInTheDocument();
  });

  it('should open the menu on button click', async () => {
    component();
    expect(
      screen.queryByTestId('criteria-menu-dropdown')
    ).not.toBeInTheDocument();
    const addCriteriaButton = await screen.findByRole('button', {
      name: 'Add Criteria',
    });
    userEvent.click(addCriteriaButton);
    expect(
      await screen.findByTestId('criteria-menu-dropdown')
    ).toBeInTheDocument();
  });

  it('should call the api when a valid search term is entered', async () => {
    component();
    const shortInput = 'i';
    const validInput = 'valid input';
    const addCriteriaButton = await screen.findByRole('button', {
      name: 'Add Criteria',
    });
    userEvent.click(addCriteriaButton);
    const domainCountsSpy = jest.spyOn(
      cohortBuilderApi(),
      'findUniversalDomainCounts'
    );
    expect(domainCountsSpy).toHaveBeenCalledTimes(0);

    const criteriaInput = await screen.findByRole('textbox');

    // Show the alert message when only a single char is entered
    await userEvent.type(criteriaInput, shortInput);
    fireEvent.keyDown(criteriaInput, {
      key: 'Enter',
    });
    await waitFor(() =>
      expect(
        screen.getByTestId('criteria-menu-input-alert')
      ).toBeInTheDocument()
    );

    expect(domainCountsSpy).toHaveBeenCalledTimes(0);

    // No alert and call api for valid search term
    await userEvent.type(criteriaInput, validInput);
    fireEvent.keyDown(criteriaInput, {
      key: 'Enter',
    });

    await waitFor(() => {
      expect(
        screen.queryByTestId('criteria-menu-input-alert')
      ).not.toBeInTheDocument();
      expect(domainCountsSpy).toHaveBeenCalledTimes(1);
    });
  });
});
