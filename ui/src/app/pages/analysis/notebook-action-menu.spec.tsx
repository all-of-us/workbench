import '@testing-library/jest-dom';

import { WorkspaceAccessLevel } from 'generated/fetch';

import { screen } from '@testing-library/react';
import { NotebookActionMenu } from 'app/pages/analysis/notebook-action-menu';

import { renderWithRouter } from 'testing/react-test-helpers';

describe('NotebookActionMenu', () => {
  const props = {
    notebook: {
      name: 'name',
    },
    permission: WorkspaceAccessLevel.WRITER,
  };
  const component = () => {
    return renderWithRouter(
      <NotebookActionMenu
        resource={props}
        existingNameList={[]}
        onUpdate={() => {}}
      />
    );
  };

  it('should render', () => {
    component();
    // screen.logTestingPlaygroundURL();
    expect(
      screen.getByRole('link', { name: props.notebook.name })
    ).toBeInTheDocument();
    expect(screen.getByText(/last modified:/i)).toBeInTheDocument();
  });
});
