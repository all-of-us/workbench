import * as React from 'react';
import { MemoryRouter } from 'react-router';
import { mount } from 'enzyme';

import { NotebooksApi, WorkspacesApi } from 'generated/fetch';

import { AppFilesList } from 'app/pages/appAnalysis/app-files-list';
import { registerApiClient } from 'app/services/swagger-fetch-clients';
import { displayDateWithoutHours } from 'app/utils/dates';
import { currentWorkspaceStore } from 'app/utils/navigation';
import { appFilesTabName } from 'app/utils/user-apps-utils';

import { waitOneTickAndUpdate } from 'testing/react-test-helpers';
import { NotebooksApiStub } from 'testing/stubs/notebooks-api-stub';
import { workspaceDataStub } from 'testing/stubs/workspaces';
import { WorkspacesApiStub } from 'testing/stubs/workspaces-api-stub';

const appsFilesTable = (wrapper) =>
  wrapper.find('[data-test-id="apps-file-list"]').find('tbody');
const appsFilesTableColumns = (wrapper) => appsFilesTable(wrapper).find('td');

const MENU_COLUMN_NUMBER = 0;
const APPLICATION_COLUMN_NUMBER = 1;
const NAME_COLUMN_NUMBER = 2;
const MODIFIED_DATE_COLUMN_NUMBER = 3;
const MODIFIED_BY_COLUMN_NUMBER = 4;

describe('AppsList', () => {
  beforeEach(() => {
    registerApiClient(WorkspacesApi, new WorkspacesApiStub());
    registerApiClient(NotebooksApi, new NotebooksApiStub());
  });

  it('should render new Analysis tab', async () => {
    currentWorkspaceStore.next(workspaceDataStub);
    const wrapper = mount(
      <MemoryRouter>
        <AppFilesList showSpinner={() => {}} hideSpinner={() => {}} />
      </MemoryRouter>
    );
    await waitOneTickAndUpdate(wrapper);

    // First Column : Menu icon should be a kebab icon with circle
    expect(
      appsFilesTableColumns(wrapper)
        .at(MENU_COLUMN_NUMBER)
        .first()
        .find('[data-icon="circle-ellipsis-vertical"]')
    ).toBeTruthy();

    // Second Column displays the type of Application: In this case Jupyter
    expect(
      appsFilesTableColumns(wrapper)
        .at(APPLICATION_COLUMN_NUMBER)
        .find('img')
        .prop('alt')
    ).toBe('Jupyter');

    // Fourth column of table displays file name with extension
    expect(
      appsFilesTableColumns(wrapper).at(NAME_COLUMN_NUMBER).text()
    ).toMatch(NotebooksApiStub.stubNotebookList()[0].name);

    // Fifth column of notebook table displays last modified time
    expect(
      appsFilesTableColumns(wrapper).at(MODIFIED_DATE_COLUMN_NUMBER).text()
    ).toMatch(
      displayDateWithoutHours(
        NotebooksApiStub.stubNotebookList()[0].lastModifiedTime
      )
    );

    // Sixth column of notebook table displays last modified by
    expect(
      appsFilesTableColumns(wrapper).at(MODIFIED_BY_COLUMN_NUMBER).text()
    ).toMatch(NotebooksApiStub.stubNotebookList()[0].lastModifiedBy);
  });

  it('should redirect to notebook playground mode when file name of Jupyter App is clicked', async () => {
    currentWorkspaceStore.next(workspaceDataStub);
    const wrapper = mount(
      <MemoryRouter>
        <AppFilesList showSpinner={() => {}} hideSpinner={() => {}} />
      </MemoryRouter>
    );
    await waitOneTickAndUpdate(wrapper);

    const expected = `/workspaces/${workspaceDataStub.namespace}/${workspaceDataStub.id}/${NOTEBOOKS_TAB_NAME}/preview/mockFile.ipynb`;
    expect(
      appsFilesTableColumns(wrapper)
        .at(NAME_COLUMN_NUMBER)
        .find('a')
        .prop('href')
    ).toBe(expected);
  });
});
