import * as React from 'react';
import { MemoryRouter } from 'react-router';
import { mount } from 'enzyme';

import { WorkspaceAccessLevel } from 'generated/fetch';

import { NotebookActionMenu } from 'app/pages/analysis/notebook-action-menu';

describe('NotebookActionMenu', () => {
  const component = () => {
    const props = {
      notebook: {
        name: 'name',
      },
      permission: WorkspaceAccessLevel.WRITER,
    };

    return mount(
      <MemoryRouter>
        <NotebookActionMenu
          resource={props}
          existingNameList={[]}
          onUpdate={() => {}}
        />
        );
      </MemoryRouter>
    );
  };

  it('should render', () => {
    const wrapper = component();
    expect(wrapper).toBeTruthy();
  });
});
