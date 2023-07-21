import * as React from 'react';
import { MemoryRouter } from 'react-router';
import { mount } from 'enzyme';

import { NotebooksApi, ProfileApi, WorkspacesApi } from 'generated/fetch';

import {
  MODIFIED_BY_COLUMN_NUMBER,
  MODIFIED_DATE_COLUMN_NUMBER,
  NAME_COLUMN_NUMBER,
  RESOURCE_TYPE_COLUMN_NUMBER,
  resourceTableColumns,
} from 'app/components/resource-list.spec';
import { registerApiClient } from 'app/services/swagger-fetch-clients';
import { displayDateWithoutHours } from 'app/utils/dates';
import { currentWorkspaceStore } from 'app/utils/navigation';
import { appFilesTabName } from 'app/utils/user-apps-utils';

import { waitOneTickAndUpdate } from 'testing/react-test-helpers';
import { NotebooksApiStub } from 'testing/stubs/notebooks-api-stub';
import { ProfileApiStub } from 'testing/stubs/profile-api-stub';
import { workspaceDataStub } from 'testing/stubs/workspaces';
import { WorkspacesApiStub } from 'testing/stubs/workspaces-api-stub';

import { NotebookList } from './notebook-list';

describe('NotebookList', () => {
  beforeEach(() => {
    registerApiClient(WorkspacesApi, new WorkspacesApiStub());
    registerApiClient(NotebooksApi, new NotebooksApiStub());
    registerApiClient(ProfileApi, new ProfileApiStub());
  });

  it('should render notebooks', async () => {
    currentWorkspaceStore.next(workspaceDataStub);
    const wrapper = mount(
      <MemoryRouter>
        <NotebookList hideSpinner={() => {}} />
      </MemoryRouter>
    );
    await waitOneTickAndUpdate(wrapper);

    // Second Column of notebook table displays the type of resource: Notebook
    expect(
      resourceTableColumns(wrapper).at(RESOURCE_TYPE_COLUMN_NUMBER).text()
    ).toMatch('Notebook');

    // Third column of notebook table displays the notebook file name
    expect(resourceTableColumns(wrapper).at(NAME_COLUMN_NUMBER).text()).toMatch(
      NotebooksApiStub.stubNotebookList()[0].name.split('.ipynb')[0]
    );

    // Forth column of notebook table displays last modified time
    expect(
      resourceTableColumns(wrapper).at(MODIFIED_DATE_COLUMN_NUMBER).text()
    ).toMatch(
      displayDateWithoutHours(
        NotebooksApiStub.stubNotebookList()[0].lastModifiedTime
      )
    );

    // Fifth column of notebook table displays last modified by
    expect(
      resourceTableColumns(wrapper).at(MODIFIED_BY_COLUMN_NUMBER).text()
    ).toMatch(NotebooksApiStub.stubNotebookList()[0].lastModifiedBy);
  });

  it('should redirect to notebook playground mode when either resource type or name is clicked', async () => {
    currentWorkspaceStore.next(workspaceDataStub);
    const wrapper = mount(
      <MemoryRouter>
        <NotebookList hideSpinner={() => {}} />
      </MemoryRouter>
    );
    await waitOneTickAndUpdate(wrapper);

    const expected = `/workspaces/${workspaceDataStub.namespace}/${workspaceDataStub.id}/${NOTEBOOKS_TAB_NAME}/preview/mockFile.ipynb`;
    expect(
      resourceTableColumns(wrapper)
        .at(RESOURCE_TYPE_COLUMN_NUMBER)
        .find('a')
        .prop('href')
    ).toBe(expected);

    expect(
      resourceTableColumns(wrapper)
        .at(NAME_COLUMN_NUMBER)
        .find('a')
        .prop('href')
    ).toBe(expected);
  });
});
