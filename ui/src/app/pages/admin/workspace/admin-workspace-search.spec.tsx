import '@testing-library/jest-dom';

import * as React from 'react';
import { mockNavigate } from 'setupTests';

import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { AdminWorkspaceSearch } from './admin-workspace-search';

let showSpinner, hideSpinner;

beforeEach(() => {
  showSpinner = jest.fn();
  hideSpinner = jest.fn();
});

afterEach(() => {
  jest.clearAllMocks();
});

const wsNs = 'can be anything - not testing the navigation here';
const expectedNavigation = ['admin/workspaces/' + wsNs];

describe('AdminWorkspaceSearch', () => {
  it('navigates to an existing workspace by typing enter', async () => {
    const user = userEvent.setup();
    const { container } = render(
      <AdminWorkspaceSearch {...{ showSpinner, hideSpinner }} />
    );
    expect(container).toBeInTheDocument();

    const searchBox = screen.getByLabelText('Workspace namespace');
    expect(searchBox).toBeInTheDocument();

    await user.type(searchBox, wsNs);
    await user.type(searchBox, '{enter}');

    expect(mockNavigate).toHaveBeenCalledWith(expectedNavigation);
  });

  it('navigates to an existing workspace by clicking the button', async () => {
    const user = userEvent.setup();
    const { container } = render(
      <AdminWorkspaceSearch {...{ showSpinner, hideSpinner }} />
    );
    expect(container).toBeInTheDocument();

    const searchBox = screen.getByLabelText('Workspace namespace');
    expect(searchBox).toBeInTheDocument();

    const loadButton = screen.getByText('Load Workspace');
    expect(loadButton).toBeInTheDocument();

    await user.type(searchBox, wsNs);
    await user.click(loadButton);

    expect(mockNavigate).toHaveBeenCalledWith(expectedNavigation);
  });
});
