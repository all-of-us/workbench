import * as React from 'react';
import { MemoryRouter } from 'react-router';
import { mount } from 'enzyme';

import { NotebooksApi, ProfileApi, WorkspacesApi } from 'generated/fetch';

import { registerApiClient } from 'app/services/swagger-fetch-clients';
import { displayDateWithoutHours } from 'app/utils/dates';
import { currentWorkspaceStore } from 'app/utils/navigation';

import { waitOneTickAndUpdate } from 'testing/react-test-helpers';
import { NotebooksApiStub } from 'testing/stubs/notebooks-api-stub';
import { ProfileApiStub } from 'testing/stubs/profile-api-stub';
import { workspaceDataStub } from 'testing/stubs/workspaces';
import { WorkspacesApiStub } from 'testing/stubs/workspaces-api-stub';

import { NotebookList } from './notebook-list';

const RESOURCE_TYPE_COLUMN_NUMBER = 1;
const NOTEBOOK_NAME_COLUMN_NUMBER = 2;
const MODIFIED_DATE_COLUMN_NUMBER = 3;

const NOTEBOOK_HREF_LOCATION =
  '/workspaces/defaultNamespace/1/notebooks/preview/mockFile.ipynb';

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
    const notebookTableColumns = wrapper
      .find('[data-test-id="resource-list"]')
      .find('tbody')
      .find('td');

    // Second Column of notebook table displays the type of resource type: Notebook
    expect(notebookTableColumns.at(RESOURCE_TYPE_COLUMN_NUMBER).text()).toMatch(
      'Notebook'
    );

    // Third column of notebook table displays the notebook file name
    expect(notebookTableColumns.at(NOTEBOOK_NAME_COLUMN_NUMBER).text()).toMatch(
      NotebooksApiStub.stubNotebookList()[0].name.split('.ipynb')[0]
    );

    // Forth column of notebook table displays last modified time
    expect(notebookTableColumns.at(MODIFIED_DATE_COLUMN_NUMBER).text()).toMatch(
      displayDateWithoutHours(
        NotebooksApiStub.stubNotebookList()[0].lastModifiedTime
      )
    );
  });

  it('should redirect to notebook playground mode when some resource type or name is clicked', async () => {
    currentWorkspaceStore.next(workspaceDataStub);
    const wrapper = mount(
      <MemoryRouter>
        <NotebookList hideSpinner={() => {}} />
      </MemoryRouter>
    );
    await waitOneTickAndUpdate(wrapper);
    const notebookTableColumns = wrapper
      .find('[data-test-id="resource-list"]')
      .find('tbody')
      .find('td');

    // verify columns displaying resource type and notebook name are clickable
    expect(
      notebookTableColumns
        .at(RESOURCE_TYPE_COLUMN_NUMBER)
        .find('a')
        .prop('href')
    ).toBe(NOTEBOOK_HREF_LOCATION);

    expect(
      notebookTableColumns
        .at(NOTEBOOK_NAME_COLUMN_NUMBER)
        .find('a')
        .prop('href')
    ).toBe(NOTEBOOK_HREF_LOCATION);
  });
});
