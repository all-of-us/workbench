import {mount} from 'enzyme';
import * as React from 'react';
import Select from 'react-select';

import {registerApiClient, workspacesApi} from 'app/services/swagger-fetch-clients';
import {WorkspaceAccessLevel, WorkspacesApi} from 'generated/fetch';
import {WorkspacesApiStub} from 'testing/stubs/workspaces-api-stub';
import {waitOneTickAndUpdate} from 'testing/react-test-helpers';

import { CopyNotebookModal, CopyNotebookModalProps, CopyNotebookModalState } from './component';
import { TextInput } from 'app/components/inputs';

describe('CopyNotebookModal', () => {
  let props: CopyNotebookModalProps;

  const component = () => {
    return mount<CopyNotebookModal, CopyNotebookModalProps, CopyNotebookModalState>
    (<CopyNotebookModal {...props}/>);
  };

  const workspaces = [
    {
      namespace: 'El Capitan',
      name: 'Freerider',
      id: 'freerider'
    },
    {
      namespace: 'El Capitan',
      name: 'Dawn Wall',
      id: 'dawn wall'
    },
    {
      namespace: 'El Capitan',
      name: 'Zodiac',
      id: 'zodiac'
    },
    {
      namespace: 'El Capitan',
      name: 'The Nose',
      id: 'the nose'
    }
  ];

  beforeEach(() => {
    props = {
      fromWorkspaceNamespace: "namespace",
      fromWorkspaceName: "name",
      fromNotebook: {
        name: "notebook",
        path: "path",
        lastModifiedTime: null
      },
      onClose: () => {},
      onCopy: () => {}
    };

    let apiStub = new WorkspacesApiStub(workspaces);
    registerApiClient(WorkspacesApi, apiStub);
    apiStub.workspaceAccess.set(workspaces[0].id, WorkspaceAccessLevel.OWNER);
    apiStub.workspaceAccess.set(workspaces[1].id, WorkspaceAccessLevel.READER);
    apiStub.workspaceAccess.set(workspaces[2].id, WorkspaceAccessLevel.WRITER);
    apiStub.workspaceAccess.set(workspaces[3].id, WorkspaceAccessLevel.NOACCESS);
  });

  it('should render', async() => {
    const wrapper = component();
    expect(wrapper).toBeTruthy();
  });

  it('should populate select options with writeable Workspaces from getWorkspaces()', async() => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);

    let select = wrapper.find(Select);
    select.instance().setState({menuIsOpen: true});
    wrapper.update();

    const options = wrapper.find(Select).find({role: 'option'}).map(e => e.text());

    expect(options).toEqual([workspaces[0].name, workspaces[2].name]);
  });

  it('should call correct copyNotebook() call after selecting an option and entering a name', async() => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);

    // Open Select options. Simulating a click doesn't work for some reason
    let select = wrapper.find(Select);
    select.instance().setState({menuIsOpen: true});
    wrapper.update();

    // Select an option
    wrapper.find(Select).find({role: 'option'})
      .findWhere(e => e.text() === workspaces[2].name)
      .first()
      .simulate('click');

    // Type out new name
    wrapper.find(TextInput).simulate('change', {target: {value: 'Freeblast'}});

    const spy = jest.spyOn(workspacesApi(), 'copyNotebook');
    // Click copy button
    wrapper.find('[data-test-id="copy-notebook-button"]').first().simulate('click');

    expect(spy).toHaveBeenCalledWith(
      props.fromWorkspaceNamespace,
      props.fromWorkspaceName,
      props.fromNotebook.name,
      {
        toWorkspaceName: workspaces[2].id,
        toWorkspaceNamespace: workspaces[2].namespace,
        newName: "Freeblast"
      }
    );
  });

  it('should disable copy button if option is not selected', async() => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);

    const spy = jest.spyOn(workspacesApi(), 'copyNotebook');

    // Click copy button
    const copyButton = wrapper.find('[data-test-id="copy-notebook-button"]').first();
    copyButton.simulate('click');

    expect(copyButton.prop('disabled')).toBe(true);
    expect(spy).toHaveBeenCalledTimes(0);
  });
});
