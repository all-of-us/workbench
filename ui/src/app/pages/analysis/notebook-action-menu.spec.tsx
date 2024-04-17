import '@testing-library/jest-dom';

import { MemoryRouter } from 'react-router';

import { WorkspaceAccessLevel } from 'generated/fetch';

import { render, screen } from '@testing-library/react';
import { NotebookActionMenu } from 'app/pages/analysis/notebook-action-menu';

describe('NotebookActionMenu', () => {
  const component = () => {
    const props = {
      notebook: {
        name: 'name',
      },
      permission: WorkspaceAccessLevel.WRITER,
    };
    return render(
      <MemoryRouter>
        <NotebookActionMenu
          resource={props}
          existingNameList={[]}
          onUpdate={() => {}}
        />
      </MemoryRouter>
    );
  };

  it('should render', () => {
    const { container } = component();
    expect(container).toBeInTheDocument();
  });
});
