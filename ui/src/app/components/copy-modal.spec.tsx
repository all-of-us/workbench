import {mount, ReactWrapper, ShallowWrapper} from 'enzyme';
import * as React from 'react';
import Select from 'react-select';

import {TextInput} from 'app/components/inputs';
import {conceptSetsApi, registerApiClient, workspacesApi} from 'app/services/swagger-fetch-clients';
import {ConceptSetsApi, ResourceType, WorkspaceAccessLevel, WorkspacesApi} from 'generated/fetch';
import {dropNotebookFileSuffix} from 'app/pages/analysis/util';

import {waitOneTickAndUpdate} from 'testing/react-test-helpers';
import {ConceptSetsApiStub} from 'testing/stubs/concept-sets-api-stub';
import {WorkspacesApiStub} from 'testing/stubs/workspaces-api-stub';
import {cdrVersionTiersResponse, CdrVersionsStubVariables} from 'testing/stubs/cdr-versions-api-stub';

import {CopyModalComponent, CopyModalProps, CopyModalState} from './copy-modal';

describe('CopyModal', () => {
  let props: CopyModalProps;

  const component = () => {
    return mount<CopyModalComponent, CopyModalProps, CopyModalState>
    (<CopyModalComponent {...props}/>);
  };

  const workspaces = [
    {
      namespace: 'El Capitan',
      name: 'Freerider',
      id: 'freerider',
      cdrVersionId: CdrVersionsStubVariables.DEFAULT_WORKSPACE_CDR_VERSION_ID,
    },
    {
      namespace: 'El Capitan',
      name: 'Dawn Wall',
      id: 'dawn wall',
      cdrVersionId: CdrVersionsStubVariables.DEFAULT_WORKSPACE_CDR_VERSION_ID,
    },
    {
      namespace: 'El Capitan',
      name: 'Zodiac',
      id: 'zodiac',
      cdrVersionId: CdrVersionsStubVariables.DEFAULT_WORKSPACE_CDR_VERSION_ID,
    },
    {
      namespace: 'El Capitan',
      name: 'The Nose',
      id: 'the nose',
      cdrVersionId: CdrVersionsStubVariables.DEFAULT_WORKSPACE_CDR_VERSION_ID,
    },
    {
      namespace: 'Something Different',
      name: 'Sesame Street',
      id: 'sesame-street',
      cdrVersionId: CdrVersionsStubVariables.ALT_WORKSPACE_CDR_VERSION_ID,
    },
  ];

  const altCdrWorkspace = workspaces[4];

  const fromWorkspaceNamespace = workspaces[0].namespace;
  const fromWorkspaceFirecloudName = workspaces[0].id;
  const fromCdrVersionId = workspaces[0].cdrVersionId;
  const fromResourceName = 'notebook';
  const notebookSaveFunction = (copyRequest) => {
    return workspacesApi().copyNotebook(
        fromWorkspaceNamespace,
        fromWorkspaceFirecloudName,
        dropNotebookFileSuffix(fromResourceName),
        copyRequest
    );
  }

  type AnyWrapper = (ShallowWrapper|ReactWrapper);

  function getConceptSetCdrMismatchError(wrapper: AnyWrapper): AnyWrapper {
    return wrapper.find('[data-test-id="concept-set-cdr-mismatch-error"]');
  }

  function getNotebookCdrMismatchWarning(wrapper: AnyWrapper): AnyWrapper {
    return wrapper.find('[data-test-id="notebook-cdr-mismatch-warning"]');
  }

  beforeEach(() => {
    const wsApiStub = new WorkspacesApiStub(workspaces);
    registerApiClient(WorkspacesApi, wsApiStub);

    props = {
      cdrVersionTiersResponse: cdrVersionTiersResponse,
      fromWorkspaceNamespace: fromWorkspaceNamespace,
      fromWorkspaceFirecloudName: fromWorkspaceFirecloudName,
      fromResourceName: fromResourceName,
      fromCdrVersionId: fromCdrVersionId,
      resourceType: ResourceType.NOTEBOOK,
      onClose: () => {},
      onCopy: () => {},
      saveFunction: notebookSaveFunction,
    };
    wsApiStub.workspaceAccess.set(workspaces[0].id, WorkspaceAccessLevel.OWNER);
    wsApiStub.workspaceAccess.set(workspaces[1].id, WorkspaceAccessLevel.READER);
    wsApiStub.workspaceAccess.set(workspaces[2].id, WorkspaceAccessLevel.WRITER);
    wsApiStub.workspaceAccess.set(workspaces[3].id, WorkspaceAccessLevel.NOACCESS);
    wsApiStub.workspaceAccess.set(workspaces[4].id, WorkspaceAccessLevel.WRITER);
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

    const currentWsOption = workspaces[0].name + ' (current workspace)';
    const writerWsOption = workspaces[2].name;
    const writerAltCdrWsOption = workspaces[4].name;

    expect(options).toEqual([currentWsOption, writerWsOption, writerAltCdrWsOption]);
  });

  it('should list workspaces with the same CDR version first', async() => {
    // choose a workspace with an alternative CDR version instead of the default
    props.fromWorkspaceNamespace = altCdrWorkspace.namespace;
    props.fromWorkspaceFirecloudName = altCdrWorkspace.id;
    props.fromCdrVersionId = altCdrWorkspace.cdrVersionId;

    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);

    const select = wrapper.find(Select);
    select.instance().setState({menuIsOpen: true});
    wrapper.update();

    const options = wrapper.find(Select).find({type: 'option'}).map(e => e.text());

    const currentWsOption = altCdrWorkspace.name + ' (current workspace)';
    const otherCdrOwnerWsOption = workspaces[0].name;
    const otherCdrWriterWsOption = workspaces[2].name;

    expect(options).toEqual([currentWsOption, otherCdrOwnerWsOption, otherCdrWriterWsOption]);
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
      props.fromWorkspaceFirecloudName,
      props.fromResourceName,
      {
        toWorkspaceName: workspaces[2].id,
        toWorkspaceNamespace: workspaces[2].namespace,
        newName: 'Freeblast'
      }
    );
  });

  it('should call correct copyNotebook() call when a mismatched CDR is selected', async() => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);

    // Open Select options. Simulating a click doesn't work for some reason
    const select = wrapper.find(Select);
    select.instance().setState({menuIsOpen: true});
    wrapper.update();

    // Select an option
    wrapper.find(Select).find({type: 'option'})
        .findWhere(e => e.text() === altCdrWorkspace.name)
        .first()
        .simulate('click');

    expect(getNotebookCdrMismatchWarning(wrapper).getDOMNode().textContent).toBe(
        'The selected destination workspace uses a different dataset version ' +
        `(${CdrVersionsStubVariables.ALT_WORKSPACE_CDR_VERSION}) than the current workspace ` +
        `(${CdrVersionsStubVariables.DEFAULT_WORKSPACE_CDR_VERSION}). ` +
        'Edits may be required to ensure your analysis is functional and accurate.');

    // Type out new name
    wrapper.find(TextInput).simulate('change', {target: {value: 'Freeblast'}});

    const spy = jest.spyOn(workspacesApi(), 'copyNotebook');
    // Click copy button
    wrapper.find('[data-test-id="copy-button"]').first().simulate('click');

    expect(spy).toHaveBeenCalledWith(
        props.fromWorkspaceNamespace,
        props.fromWorkspaceFirecloudName,
        props.fromResourceName,
        {
          toWorkspaceName: altCdrWorkspace.id,
          toWorkspaceNamespace: altCdrWorkspace.namespace,
          newName: 'Freeblast'
        }
    );
  });

  it('should disable copy notebook button if option is not selected', async() => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);

    const spy = jest.spyOn(workspacesApi(), 'copyNotebook');

    // Click copy button
    const copyButton = wrapper.find('[data-test-id="copy-button"]').first();
    copyButton.simulate('click');

    expect(copyButton.prop('disabled')).toBe(true);
    expect(spy).toHaveBeenCalledTimes(0);
  });

  it('should call correct copyConceptSet() call after selecting an option with a matching CDR and entering a name', async() => {
    const csApiStub = new ConceptSetsApiStub();
    registerApiClient(ConceptSetsApi, csApiStub);

    props.resourceType = ResourceType.CONCEPTSET;
    props.fromResourceName = csApiStub.conceptSets[0].name;
    props.saveFunction = (copyRequest) => {
      return conceptSetsApi().copyConceptSet(
          fromWorkspaceNamespace,
          fromWorkspaceFirecloudName,
          props.fromResourceName,
          copyRequest
      );
    }

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
    wrapper.find(TextInput).simulate('change', {target: {value: 'Some Concepts'}});

    const spy = jest.spyOn(conceptSetsApi(), 'copyConceptSet');
    // Click copy button
    wrapper.find('[data-test-id="copy-button"]').first().simulate('click');

    expect(spy).toHaveBeenCalledWith(
        props.fromWorkspaceNamespace,
        props.fromWorkspaceFirecloudName,
        props.fromResourceName,
        {
          toWorkspaceName: workspaces[2].id,
          toWorkspaceNamespace: workspaces[2].namespace,
          newName: 'Some Concepts'
        }
    );
  });

  it('should disable concept set copy button when a mismatched CDR is selected', async() => {
    const csApiStub = new ConceptSetsApiStub();
    registerApiClient(ConceptSetsApi, csApiStub);

    props.resourceType = ResourceType.CONCEPTSET;
    props.fromResourceName = csApiStub.conceptSets[0].name;
    props.saveFunction = (copyRequest) => {
      return conceptSetsApi().copyConceptSet(
          fromWorkspaceNamespace,
          fromWorkspaceFirecloudName,
          props.fromResourceName,
          copyRequest
      );
    }

    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);

    // Open Select options. Simulating a click doesn't work for some reason
    const select = wrapper.find(Select);
    select.instance().setState({menuIsOpen: true});
    wrapper.update();

    // Select an option
    wrapper.find(Select).find({type: 'option'})
        .findWhere(e => e.text() === altCdrWorkspace.name)
        .first()
        .simulate('click');

    expect(getConceptSetCdrMismatchError(wrapper).getDOMNode().textContent).toBe(
        'Can’t copy to that workspace. It uses a different dataset version ' +
        `(${CdrVersionsStubVariables.ALT_WORKSPACE_CDR_VERSION}) than the current workspace ` +
        `(${CdrVersionsStubVariables.DEFAULT_WORKSPACE_CDR_VERSION}).`);

    // Type out new name
    wrapper.find(TextInput).simulate('change', {target: {value: 'Some Concepts'}});

    const spy = jest.spyOn(conceptSetsApi(), 'copyConceptSet');
    // Click copy button
    const copyButton = wrapper.find('[data-test-id="copy-button"]').first();
    copyButton.simulate('click');

    expect(copyButton.prop('disabled')).toBe(true);
    expect(spy).toHaveBeenCalledTimes(0);
  });
});
