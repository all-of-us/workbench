import * as React from 'react';

import { render, screen } from '@testing-library/react';
import { currentWorkspaceStore } from 'app/utils/navigation';

import { workspaceDataStub } from 'testing/stubs/workspaces';

import { GenderChart } from './gender-chart';

describe('GenderChart', () => {
  beforeEach(() => {
    currentWorkspaceStore.next(workspaceDataStub);
  });

  it('should render', () => {
    render(<GenderChart data={[]} />);
    expect(screen.getByText('# Participants')).toBeTruthy();
  });
});
