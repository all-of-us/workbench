import '@testing-library/jest-dom';

import * as React from 'react';

import { render, screen } from '@testing-library/react';
import { currentWorkspaceStore } from 'app/utils/navigation';

import { workspaceDataStub } from 'testing/stubs/workspaces';

import { ComboChart } from './combo-chart.component';

describe('ComboChart', () => {
  beforeEach(() => {
    currentWorkspaceStore.next(workspaceDataStub);
  });

  it('should render', () => {
    render(<ComboChart data={[]} legendTitle='Age' mode='percent' />);
    expect(screen.getByText('Age')).toBeInTheDocument();
  });
});
