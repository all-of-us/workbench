import * as React from 'react';
import { MemoryRouter } from 'react-router';
import { mount } from 'enzyme';

import { WorkspaceAccessLevel } from 'generated/fetch';

import { NotebookResourceCard } from 'app/pages/analysis/notebook-resource-card';

describe('NotebookResourceCard', () => {
  const component = () => {
    const props = {
      notebook: {
        name: 'name',
      },
      permission: WorkspaceAccessLevel.WRITER,
    };

    return mount(
      <MemoryRouter>
        <NotebookResourceCard
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
