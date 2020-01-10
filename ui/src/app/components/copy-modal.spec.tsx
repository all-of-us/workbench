import {mount} from 'enzyme';
import * as React from 'react';
import Select from 'react-select';

import {TextInput} from 'app/components/inputs';
import {registerApiClient, workspacesApi} from 'app/services/swagger-fetch-clients';
import {WorkspaceAccessLevel, WorkspacesApi} from 'generated/fetch';
import {waitOneTickAndUpdate} from 'testing/react-test-helpers';
import {WorkspacesApiStub} from 'testing/stubs/workspaces-api-stub';

import {dropNotebookFileSuffix} from 'app/pages/analysis/util';
import {CopyModal, CopyModalProps, CopyModalState} from './copy-modal';
import {ResourceType} from 'generated/fetch';

describe('CopyModal', () => {
  let props: CopyModalProps;

  const component = () => {
    return mount<CopyModal, CopyModalProps, CopyModalState>
    (<CopyModal {...props}/>);
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
  const fromWorkspaceNamespace = 'namespace';
  const fromWorkspaceName = 'name';
  const fromResourceName = 'notebook';

  beforeEach(() => {
    const apiStub = new WorkspacesApiStub(workspaces);
    registerApiClient(WorkspacesApi, apiStub);
    props = {
      fromWorkspaceNamespace: fromWorkspaceNamespace,
      fromWorkspaceName: fromWorkspaceName,
      fromResourceName: fromResourceName,
      resourceType: ResourceType.NOTEBOOK,
      onClose: () => {},
      onCopy: () => {},
      saveFunction: (copyRequest) => {
        return workspacesApi().copyNotebook(
          fromWorkspaceNamespace,
          fromWorkspaceName,
          dropNotebookFileSuffix(fromResourceName),
          copyRequest
        );
      }
    };
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

    const select = wrapper.find(Select);
    select.instance().setState({menuIsOpen: true});
    wrapper.update();

    const options = wrapper.find(Select).find({type: 'option'}).map(e => e.text());

    expect(options).toEqual([workspaces[0].name, workspaces[2].name]);
  });

  it('should call correct copyNotebook() call after selecting an option and entering a name', async() => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);

    // Open Select options. Simulating a click doesn't work for some reason
    const select = wrapper.find(Select);
    select.instance().setState({menuIsOpen: true});
    wrapper.update();

    // Select an option
    wrapper.find(Select).find({type: 'option'})
      .findWhere(e => e.text() === workspaces[2].name)
      .first()
      .simulate('click');

    // Type out new name
    wrapper.find(TextInput).simulate('change', {target: {value: 'Freeblast'}});

    const spy = jest.spyOn(workspacesApi(), 'copyNotebook');
    // Click copy button
    wrapper.find('[data-test-id="copy-button"]').first().simulate('click');

    expect(spy).toHaveBeenCalledWith(
      props.fromWorkspaceNamespace,
      props.fromWorkspaceName,
      props.fromResourceName,
      {
        toWorkspaceName: workspaces[2].id,
        toWorkspaceNamespace: workspaces[2].namespace,
        newName: 'Freeblast'
      }
    );
  });

  it('should disable copy button if option is not selected', async() => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);

    const spy = jest.spyOn(workspacesApi(), 'copyNotebook');

    // Click copy button
    const copyButton = wrapper.find('[data-test-id="copy-button"]').first();
    copyButton.simulate('click');

    expect(copyButton.prop('disabled')).toBe(true);
    expect(spy).toHaveBeenCalledTimes(0);
  });
});
