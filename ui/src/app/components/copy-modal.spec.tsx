import '@testing-library/jest-dom';

import * as React from 'react';

import {
  ConceptSetsApi,
  NotebooksApi,
  ResourceType,
  WorkspaceAccessLevel,
  WorkspacesApi,
} from 'generated/fetch';

import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { dropJupyterNotebookFileSuffix } from 'app/pages/analysis/util';
import {
  conceptSetsApi,
  notebooksApi,
  registerApiClient,
} from 'app/services/swagger-fetch-clients';
import { AccessTierShortNames } from 'app/utils/access-tiers';
import { cdrVersionStore } from 'app/utils/stores';

import { expectButtonElementDisabled } from 'testing/react-test-helpers';
import {
  CdrVersionsStubVariables,
  cdrVersionTiersResponse,
} from 'testing/stubs/cdr-versions-api-stub';
import { ConceptSetsApiStub } from 'testing/stubs/concept-sets-api-stub';
import { NotebooksApiStub } from 'testing/stubs/notebooks-api-stub';
import { WorkspacesApiStub } from 'testing/stubs/workspaces-api-stub';

import { CopyModal, CopyModalProps } from './copy-modal';

interface TestWorkspace {
  namespace: string;
  name: string;
  id: string;
  cdrVersionId: string;
  accessTierShortName: string;
}

const selectWorkspace = async (workspace: TestWorkspace) => {
  const copyButton = screen.getByText('Copy to Workspace');
  await userEvent.click(copyButton);

  const selectButton = screen.getByText(/select\.\.\./i);
  await userEvent.click(selectButton);

  const selection = screen.queryByText(workspace.name);
  expect(selection).toBeInTheDocument();
  return userEvent.click(selection);
};

const renameNotebook = async (newName: string) => {
  const notebookTextBox = screen.getByDisplayValue(/notebook/i);
  expect(notebookTextBox).toBeInTheDocument();

  await userEvent.click(notebookTextBox);
  await userEvent.clear(notebookTextBox);
  await userEvent.paste(newName);
};

const renameConceptSet = async (newName: string) => {
  const csTextBox = screen.getByDisplayValue(/concept set/i);
  expect(csTextBox).toBeInTheDocument();

  await userEvent.click(csTextBox);
  await userEvent.clear(csTextBox);
  await userEvent.paste(newName);
};

describe(CopyModal.name, () => {
  let props: CopyModalProps;

  const component = () => {
    return render(<CopyModal {...props} />);
  };

  const defaultNamespace = 'El Capitan';
  const altNamespace = 'Something Different';
  const controlledNamespace = 'Workspaces under control';

  const workspaces: TestWorkspace[] = [
    {
      namespace: defaultNamespace,
      name: 'Freerider',
      id: 'freerider',
      cdrVersionId: CdrVersionsStubVariables.DEFAULT_WORKSPACE_CDR_VERSION_ID,
      accessTierShortName: AccessTierShortNames.Registered,
    },
    {
      namespace: defaultNamespace,
      name: 'Dawn Wall',
      id: 'dawn wall',
      cdrVersionId: CdrVersionsStubVariables.DEFAULT_WORKSPACE_CDR_VERSION_ID,
      accessTierShortName: AccessTierShortNames.Registered,
    },
    {
      namespace: defaultNamespace,
      name: 'Zodiac',
      id: 'zodiac',
      cdrVersionId: CdrVersionsStubVariables.DEFAULT_WORKSPACE_CDR_VERSION_ID,
      accessTierShortName: AccessTierShortNames.Registered,
    },
    {
      namespace: defaultNamespace,
      name: 'The Nose',
      id: 'the nose',
      cdrVersionId: CdrVersionsStubVariables.DEFAULT_WORKSPACE_CDR_VERSION_ID,
      accessTierShortName: AccessTierShortNames.Registered,
    },
    {
      namespace: altNamespace,
      name: 'Sesame Street',
      id: 'sesame-street',
      cdrVersionId: CdrVersionsStubVariables.ALT_WORKSPACE_CDR_VERSION_ID,
      accessTierShortName: AccessTierShortNames.Registered,
    },
    {
      namespace: controlledNamespace,
      name: 'A tightly controlled workspace',
      id: 'controlled-ws-1',
      cdrVersionId: CdrVersionsStubVariables.CONTROLLED_TIER_CDR_VERSION_ID,
      accessTierShortName: AccessTierShortNames.Controlled,
    },
  ];

  const ownerWorkspace = workspaces[0];
  const readerWorkspace = workspaces[1];
  const writerWorkspace = workspaces[2];
  const noAccessWorkspace = workspaces[3];
  const altCdrWorkspace = workspaces[4];
  const controlledCdrWorkspace = workspaces[5];

  const fromWorkspaceNamespace = ownerWorkspace.namespace;
  const fromWorkspaceFirecloudName = ownerWorkspace.id;
  const fromCdrVersionId = ownerWorkspace.cdrVersionId;
  const fromAccessTierShortName = ownerWorkspace.accessTierShortName;
  const fromResourceName = 'notebook';
  const notebookSaveFunction = (copyRequest) => {
    return notebooksApi().copyNotebook(
      fromWorkspaceNamespace,
      fromWorkspaceFirecloudName,
      dropJupyterNotebookFileSuffix(fromResourceName),
      copyRequest
    );
  };

  const setupConceptSetTest = () => {
    props.resourceType = ResourceType.CONCEPT_SET;
    props.fromResourceName = new ConceptSetsApiStub().conceptSets[0].name;
    props.saveFunction = (copyRequest) => {
      return conceptSetsApi().copyConceptSet(
        props.fromWorkspaceNamespace,
        props.fromWorkspaceFirecloudName,
        props.fromResourceName,
        copyRequest
      );
    };
  };

  beforeEach(() => {
    const wsApiStub = new WorkspacesApiStub(workspaces);
    registerApiClient(WorkspacesApi, wsApiStub);
    registerApiClient(NotebooksApi, new NotebooksApiStub());
    registerApiClient(ConceptSetsApi, new ConceptSetsApiStub());

    props = {
      fromWorkspaceNamespace: fromWorkspaceNamespace,
      fromWorkspaceFirecloudName: fromWorkspaceFirecloudName,
      fromResourceName: fromResourceName,
      fromCdrVersionId: fromCdrVersionId,
      fromAccessTierShortName: fromAccessTierShortName,
      resourceType: ResourceType.NOTEBOOK,
      onClose: () => {},
      onCopy: () => {},
      saveFunction: notebookSaveFunction,
    };
    wsApiStub.workspaceAccess.set(
      ownerWorkspace.id,
      WorkspaceAccessLevel.OWNER
    );
    wsApiStub.workspaceAccess.set(
      readerWorkspace.id,
      WorkspaceAccessLevel.READER
    );
    wsApiStub.workspaceAccess.set(
      writerWorkspace.id,
      WorkspaceAccessLevel.WRITER
    );
    wsApiStub.workspaceAccess.set(
      noAccessWorkspace.id,
      WorkspaceAccessLevel.NO_ACCESS
    );
    wsApiStub.workspaceAccess.set(
      altCdrWorkspace.id,
      WorkspaceAccessLevel.WRITER
    );
    wsApiStub.workspaceAccess.set(
      controlledCdrWorkspace.id,
      WorkspaceAccessLevel.OWNER
    );

    cdrVersionStore.set(cdrVersionTiersResponse);
  });

  it('should populate select options with writeable Workspaces from getWorkspaces()', async () => {
    const currentWsOption = ownerWorkspace.name + ' (current workspace)';
    const writerWsOption = writerWorkspace.name;
    const writerAltCdrWsOption = altCdrWorkspace.name;
    const writerControlledCdrWsOption = controlledCdrWorkspace.name;

    const expectedOptions = [
      currentWsOption,
      writerWsOption,
      writerAltCdrWsOption,
      writerControlledCdrWsOption,
    ];
    component();

    // verify that copy-to options are not displayed by default
    expectedOptions.forEach((option) =>
      expect(screen.queryByText(option)).not.toBeInTheDocument()
    );

    const copyButton = screen.getByText('Copy to Workspace');
    await userEvent.click(copyButton);

    const selectButton = screen.getByText(/select\.\.\./i);
    await userEvent.click(selectButton);

    expectedOptions.forEach((option) =>
      expect(screen.queryByText(option)).toBeInTheDocument()
    );
  });

  it('should list workspaces with the same CDR version first', async () => {
    // choose a workspace with an alternative CDR version instead of the default
    props.fromWorkspaceNamespace = altCdrWorkspace.namespace;
    props.fromWorkspaceFirecloudName = altCdrWorkspace.id;
    props.fromCdrVersionId = altCdrWorkspace.cdrVersionId;

    component();

    const copyButton = screen.getByText('Copy to Workspace');
    await userEvent.click(copyButton);

    const selectButton = screen.getByText(/select\.\.\./i);
    await userEvent.click(selectButton);

    // this is ugly and fragile, but I haven't been able to do better yet

    const currentWsOption = altCdrWorkspace.name + ' (current workspace)';
    const controlledCdrWriterWsOption = controlledCdrWorkspace.name;
    const otherCdrOwnerWsOption = ownerWorkspace.name;
    const otherCdrWriterWsOption = writerWorkspace.name;

    const optionAncestor =
      screen.queryByText(currentWsOption).parentNode.parentNode.parentNode;
    const optionElems: HTMLElement[] = Array.from(optionAncestor.children).map(
      (c) => c as HTMLElement
    );

    // alt CDR, with respect to the other tests
    const sameCdrElem = optionElems[0];
    const controlledCdrElem = optionElems[1];
    const otherCdrElem = optionElems[2];

    expect(within(sameCdrElem).getByText(currentWsOption)).toBeInTheDocument();
    expect(
      within(controlledCdrElem).getByText(controlledCdrWriterWsOption)
    ).toBeInTheDocument();
    expect(
      within(otherCdrElem).getByText(otherCdrOwnerWsOption)
    ).toBeInTheDocument();
    expect(
      within(otherCdrElem).getByText(otherCdrWriterWsOption)
    ).toBeInTheDocument();
  });

  it('should call correct copyNotebook() call after selecting an option and entering a name', async () => {
    component();

    await selectWorkspace(writerWorkspace);

    const newName = 'Freeblast';
    await renameNotebook(newName);

    const spy = jest.spyOn(notebooksApi(), 'copyNotebook');

    const copyButton = screen.getByText('Copy Notebook');
    await userEvent.click(copyButton);

    expect(spy).toHaveBeenCalledWith(
      props.fromWorkspaceNamespace,
      props.fromWorkspaceFirecloudName,
      props.fromResourceName,
      {
        toWorkspaceName: writerWorkspace.id,
        toWorkspaceNamespace: writerWorkspace.namespace,
        newName,
      }
    );
  });

  it('should call correct copyNotebook() call when a mismatched CDR is selected', async () => {
    component();

    await selectWorkspace(altCdrWorkspace);

    expect(
      screen.getByText(
        'The selected destination workspace uses a different dataset version ' +
          `(${CdrVersionsStubVariables.ALT_WORKSPACE_CDR_VERSION}) from the current workspace ` +
          `(${CdrVersionsStubVariables.DEFAULT_WORKSPACE_CDR_VERSION}). ` +
          'Edits may be required to ensure your analysis is functional and accurate.',
        { exact: false }
      )
    ).toBeInTheDocument();

    screen.debug();

    const newName = 'Freeblast';
    await renameNotebook(newName);

    const spy = jest.spyOn(notebooksApi(), 'copyNotebook');

    const copyButton = screen.getByText('Copy Notebook');
    await userEvent.click(copyButton);

    expect(spy).toHaveBeenCalledWith(
      props.fromWorkspaceNamespace,
      props.fromWorkspaceFirecloudName,
      props.fromResourceName,
      {
        toWorkspaceName: altCdrWorkspace.id,
        toWorkspaceNamespace: altCdrWorkspace.namespace,
        newName,
      }
    );
  });

  it('should disable notebook copy button when a mismatched access tier is selected', async () => {
    component();

    await selectWorkspace(controlledCdrWorkspace);

    expect(
      screen.getByText(
        'Can’t copy to that workspace. It has a different access tier ' +
          `(${AccessTierShortNames.Controlled}) from the current workspace (${AccessTierShortNames.Registered}).`,
        { exact: false }
      )
    ).toBeInTheDocument();

    expect(screen.getByTestId('access-tier-mismatch-error')).toBeTruthy();

    await renameNotebook('Some new notebook name');

    const spy = jest.spyOn(notebooksApi(), 'copyNotebook');

    const copyButton = screen.getByRole('button', { name: /copy/i });
    await userEvent.click(copyButton);
    expectButtonElementDisabled(copyButton);
    expect(spy).toHaveBeenCalledTimes(0);
  });

  it('should disable copy notebook button if option is not selected', async () => {
    component();

    const spy = jest.spyOn(notebooksApi(), 'copyNotebook');

    // Click copy button
    const copyButton = screen.getByRole('button', { name: /copy/i });
    await userEvent.click(copyButton);
    expectButtonElementDisabled(copyButton);
    expect(spy).toHaveBeenCalledTimes(0);
  });

  it('should call correct copyConceptSet() call after selecting an option with a matching CDR and entering a name', async () => {
    setupConceptSetTest();

    component();

    await selectWorkspace(writerWorkspace);

    const newName = 'Some Concepts';
    await renameConceptSet(newName);

    const spy = jest.spyOn(conceptSetsApi(), 'copyConceptSet');

    await userEvent.click(screen.getByRole('button', { name: /copy/i }));

    expect(spy).toHaveBeenCalledWith(
      props.fromWorkspaceNamespace,
      props.fromWorkspaceFirecloudName,
      props.fromResourceName,
      {
        toWorkspaceName: writerWorkspace.id,
        toWorkspaceNamespace: writerWorkspace.namespace,
        newName,
      }
    );
  });

  it('should disable concept set copy button when a mismatched CDR is selected', async () => {
    setupConceptSetTest();

    component();

    await selectWorkspace(altCdrWorkspace);

    expect(
      screen.getByText(
        'Can’t copy to that workspace. It uses a different dataset version ' +
          `(${CdrVersionsStubVariables.ALT_WORKSPACE_CDR_VERSION}) from the current workspace ` +
          `(${CdrVersionsStubVariables.DEFAULT_WORKSPACE_CDR_VERSION}).`,
        { exact: false }
      )
    ).toBeInTheDocument();

    expect(screen.getByTestId('concept-set-cdr-mismatch-error')).toBeTruthy();

    await renameConceptSet('whatever');

    const spy = jest.spyOn(conceptSetsApi(), 'copyConceptSet');

    const copyButton = screen.getByRole('button', { name: /copy/i });
    await userEvent.click(copyButton);
    expectButtonElementDisabled(copyButton);
    expect(spy).toHaveBeenCalledTimes(0);
  });

  it('should disable concept set copy button when a mismatched access tier is selected', async () => {
    setupConceptSetTest();

    component();

    await selectWorkspace(controlledCdrWorkspace);

    expect(
      screen.getByText(
        'Can’t copy to that workspace. It has a different access tier ' +
          `(${AccessTierShortNames.Controlled}) from the current workspace (${AccessTierShortNames.Registered}).`,
        { exact: false }
      )
    ).toBeInTheDocument();

    expect(screen.getByTestId('access-tier-mismatch-error')).toBeTruthy();

    await renameConceptSet('whatever');

    const spy = jest.spyOn(conceptSetsApi(), 'copyConceptSet');

    const copyButton = screen.getByRole('button', { name: /copy/i });
    await userEvent.click(copyButton);
    expectButtonElementDisabled(copyButton);
    expect(spy).toHaveBeenCalledTimes(0);
  });
});
