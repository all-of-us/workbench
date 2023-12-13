import '@testing-library/jest-dom';

import * as React from 'react';
import { act } from 'react-dom/test-utils';

import {
  DataSetApi,
  DataSetExportRequestGenomicsAnalysisToolEnum,
  DataSetRequest,
  KernelTypeEnum,
  NotebooksApi,
  PrePackagedConceptSetEnum,
} from 'generated/fetch';

import {
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Select } from 'app/components/inputs';
import { Tooltip } from 'app/components/popups';
import {
  appendJupyterNotebookFileSuffix,
  dropJupyterNotebookFileSuffix,
} from 'app/pages/analysis/util';
import { ExportDatasetModal } from 'app/pages/data/data-set/export-dataset-modal';
import {
  dataSetApi,
  registerApiClient,
} from 'app/services/swagger-fetch-clients';

import { DataSetApiStub } from 'testing/stubs/data-set-api-stub';
import { NotebooksApiStub } from 'testing/stubs/notebooks-api-stub';
import { workspaceDataStub } from 'testing/stubs/workspaces';

describe('ExportDatasetModal', () => {
  let dataset;
  let workspace;
  let testProps;
  let notebooksApiStub;
  let datasetApiStub;
  let modalRoot;

  const component = (props) => {
    return <ExportDatasetModal {...props} />;
  };

  function findExportButton() {
    return screen.getByRole('button', {
      name: /export/i,
    });
  }

  function findNotebookNameInput() {
    return screen.getByLabelText('Notebook Name');
  }

  beforeEach(() => {
    // // Setup a DOM element as a render target
    // modalRoot = document.createElement('div');
    // modalRoot.setAttribute('id', 'popup-root');
    // document.body.appendChild(modalRoot);

    window.open = jest.fn();
    dataset = {
      id: 1,
      name: 'Test Dataset Name',
      domainValuePairs: [],
    };

    notebooksApiStub = new NotebooksApiStub();
    registerApiClient(NotebooksApi, notebooksApiStub);
    datasetApiStub = new DataSetApiStub();
    registerApiClient(DataSetApi, datasetApiStub);

    workspace = workspaceDataStub;

    testProps = {
      workspace,
      closeFunction: () => {},
      dataset: dataset,
    };
  });

  afterEach(() => {
    // console.log('A');
    jest.clearAllMocks();
  });

  it('should render', async () => {
    const popupRoot = document.createElement('div');
    popupRoot.setAttribute('id', 'popup-root');
    const user = userEvent.setup();
    const { container, unmount } = render(component(testProps));
    await screen.findByText('Export Dataset');
    unmount();
  });

  it('should export to a new notebook', async () => {
    const popupRoot = document.createElement('div');
    popupRoot.setAttribute('id', 'popup-root');
    document.body.appendChild(popupRoot);
    const { container, unmount } = render(component(testProps));
    const notebookNameInput = findNotebookNameInput();
    const exportSpy = jest.spyOn(dataSetApi(), 'exportToNotebook');
    const notebookName = 'Notebook Name';
    const expectedNotebookName = appendJupyterNotebookFileSuffix(notebookName);
    const expectedDatasetRequest: DataSetRequest = {
      dataSetId: dataset.id,
      name: dataset.name,
      domainValuePairs: dataset.domainValuePairs,
    };

    fireEvent.change(notebookNameInput, { target: { value: notebookName } });
    fireEvent.click(findExportButton());
    expect(exportSpy).toHaveBeenCalledWith(
      workspace.namespace,
      workspace.id,
      expect.objectContaining({
        dataSetRequest: expectedDatasetRequest,
        newNotebook: true,
        notebookName: expectedNotebookName,
        kernelType: KernelTypeEnum.PYTHON,
        generateGenomicsAnalysisCode: false,
      })
    );
    unmount();
  });

  it('should export to a new notebook if the user types in the file suffix', async () => {
    const popupRoot = document.createElement('div');
    popupRoot.setAttribute('id', 'popup-root');
    const { container, unmount } = render(component(testProps));
    const exportSpy = jest.spyOn(dataSetApi(), 'exportToNotebook');
    const notebookName = 'MyNotebook.ipynb';
    const expectedNotebookName = notebookName;
    const expectedDatasetRequest: DataSetRequest = {
      dataSetId: dataset.id,
      name: dataset.name,
      domainValuePairs: dataset.domainValuePairs,
    };

    const x = findNotebookNameInput();
    fireEvent.change(x, {
      target: { value: notebookName },
    });
    fireEvent.click(findExportButton());
    expect(exportSpy).toHaveBeenCalledWith(
      workspace.namespace,
      workspace.id,
      expect.objectContaining({
        dataSetRequest: expectedDatasetRequest,
        newNotebook: true,
        notebookName: expectedNotebookName,
        kernelType: KernelTypeEnum.PYTHON,
        generateGenomicsAnalysisCode: false,
      })
    );
    unmount();
  });

  // it('should disable export if no name is provided', async () => {
  //   const { container } = render(component(testProps));
  //   const exportSpy = jest.spyOn(dataSetApi(), 'exportToNotebook');
  //
  //   container
  //     .querySelector('[data-test-id="notebook-name-input"]')
  //     .first()
  //     .simulate('change', { target: { value: '' } });
  //   findExportButton(container).simulate('click');
  //   expect(findExportButton(container).props().disabled).toBeTruthy();
  //   expect(exportSpy).not.toHaveBeenCalled();
  //
  //   findExportButton(container).simulate('mouseenter');
  //   expect(container.querySelector(Tooltip).text()).toBe("Notebook name can't be blank");
  // });
  //
  // it('should disable export if a conflicting name is provided, without the suffix', async () => {
  //   const existingNotebookName = 'existing notebook.ipynb';
  //   notebooksApiStub.notebookList = [
  //     {
  //       name: existingNotebookName,
  //     },
  //   ];
  //   const { container } = render(component(testProps));
  //   const exportSpy = jest.spyOn(dataSetApi(), 'exportToNotebook');
  //
  //   container
  //     .querySelector('[data-test-id="notebook-name-input"]')
  //     .first()
  //     .simulate('change', {
  //       target: { value: dropJupyterNotebookFileSuffix(existingNotebookName) },
  //     });
  //   findExportButton(container).simulate('click');
  //   expect(findExportButton(container).props().disabled).toBeTruthy();
  //
  //   findExportButton(container).simulate('mouseenter');
  //   expect(container.querySelector(Tooltip).text()).toBe('Notebook name already exists');
  //   expect(exportSpy).not.toHaveBeenCalled();
  // });
  //
  // it('should disable export if a conflicting name is provided, including the suffix', async () => {
  //   const existingNotebookName = 'existing notebook.ipynb';
  //   notebooksApiStub.notebookList = [
  //     {
  //       name: existingNotebookName,
  //     },
  //   ];
  //   const { container } = render(component(testProps));
  //   const exportSpy = jest.spyOn(dataSetApi(), 'exportToNotebook');
  //
  //   container
  //     .querySelector('[data-test-id="notebook-name-input"]')
  //     .first()
  //     .simulate('change', {
  //       target: {
  //         value: appendJupyterNotebookFileSuffix(existingNotebookName),
  //       },
  //     });
  //   findExportButton(container).simulate('click');
  //   expect(findExportButton(container).props().disabled).toBeTruthy();
  //
  //   findExportButton(container).simulate('mouseenter');
  //   expect(container.querySelector(Tooltip).text()).toBe('Notebook name already exists');
  //   expect(exportSpy).not.toHaveBeenCalled();
  // });
  //
  // it('should export to an existing notebook with the correct kernel type', async () => {
  //   const notebookName = 'existing notebook';
  //   const datasetName = 'dataset';
  //   const expectedNotebookName = appendJupyterNotebookFileSuffix(notebookName);
  //   dataset.name = datasetName;
  //   notebooksApiStub.notebookList = [
  //     {
  //       name: expectedNotebookName,
  //     },
  //   ];
  //   notebooksApiStub.notebookKernel = KernelTypeEnum.R;
  //
  //   const expectedDatasetRequest = {
  //     dataSetId: dataset.id,
  //     name: datasetName,
  //     domainValuePairs: dataset.domainValuePairs,
  //   };
  //   const exportSpy = jest.spyOn(dataSetApi(), 'exportToNotebook');
  //
  //   const { container } = render(component(testProps));
  //   act(() => {
  //     container.querySelector(Select).props().onChange(notebookName);
  //   });
  //
  //   findExportButton(container).simulate('click');
  //
  //   expect(exportSpy).toHaveBeenCalledWith(
  //     workspace.namespace,
  //     workspace.id,
  //     expect.objectContaining({
  //       dataSetRequest: expectedDatasetRequest,
  //       newNotebook: false,
  //       notebookName: expectedNotebookName,
  //       kernelType: KernelTypeEnum.R,
  //     })
  //   );
  // });
  //
  // it('should show code preview, auto reload on kernel switch, and hide code preview', async () => {
  //   const expectedDatasetRequest = {
  //     dataSetId: dataset.id,
  //     name: dataset.name,
  //     domainValuePairs: dataset.domainValuePairs,
  //   };
  //   datasetApiStub.codePreview = {
  //     html: '<div id="notebook">print("hello world!")</div>',
  //   };
  //   const { container } = render(component(testProps));
  //   const previewSpy = jest.spyOn(dataSetApi(), 'previewExportToNotebook');
  //
  //   container.querySelector('[data-test-id="code-preview-button"]').simulate('click');
  //
  //   expect(previewSpy).toHaveBeenCalledWith(
  //     workspace.namespace,
  //     workspace.id,
  //     expect.objectContaining({
  //       dataSetRequest: expectedDatasetRequest,
  //       kernelType: KernelTypeEnum.PYTHON,
  //     })
  //   );
  //   expect(container.querySelector('iframe').html()).toContain('hello world!');
  //
  //   container.querySelector('[data-test-id="kernel-type-r"]').first().simulate('click');
  //   expect(previewSpy).toHaveBeenCalledWith(
  //     workspace.namespace,
  //     workspace.id,
  //     expect.objectContaining({
  //       dataSetRequest: expectedDatasetRequest,
  //       kernelType: KernelTypeEnum.R,
  //     })
  //   );
  //
  //   container.querySelector('[data-test-id="code-preview-button"]').simulate('click');
  //   expect(container.querySelector('iframe').exists()).toBeFalsy();
  // });
  //
  // it('Show genomics analysis tools if WGS is in the dataset', async () => {
  //   testProps.dataset.prePackagedConceptSet = [
  //     PrePackagedConceptSetEnum.WHOLE_GENOME,
  //   ];
  //   const { container } = render(component(testProps));
  //
  //   Object.keys(DataSetExportRequestGenomicsAnalysisToolEnum).forEach(
  //     (tool) => {
  //       expect(
  //         container.querySelector(`[data-test-id="genomics-tool-${tool}"]`).exists()
  //       ).toBeTruthy();
  //     }
  //   );
  // });
  //
  // it('Remove genomics analysis tools if R is selected', async () => {
  //   testProps.dataset.prePackagedConceptSet = [
  //     PrePackagedConceptSetEnum.WHOLE_GENOME,
  //   ];
  //   const { container } = render(component(testProps));
  //
  //   container.querySelector('[data-test-id="kernel-type-r"]').first().simulate('click');
  //
  //   Object.keys(DataSetExportRequestGenomicsAnalysisToolEnum).forEach(
  //     (tool) => {
  //       expect(
  //         container.querySelector(`[data-test-id="genomics-tool-${tool}"]`).exists()
  //       ).toBeFalsy();
  //     }
  //   );
  // });
  //
  // it('Remove genomics analysis tools if no WGS', async () => {
  //   testProps.dataset.prePackagedConceptSet = [];
  //   const { container } = render(component(testProps));
  //
  //   Object.keys(DataSetExportRequestGenomicsAnalysisToolEnum).forEach(
  //     (tool) => {
  //       expect(
  //         container.querySelector(`[data-test-id="genomics-tool-${tool}"]`).exists()
  //       ).toBeFalsy();
  //     }
  //   );
  // });
  //
  // it('Should export code with genomics analysis tool', async () => {
  //   testProps.dataset.prePackagedConceptSet = [
  //     PrePackagedConceptSetEnum.WHOLE_GENOME,
  //   ];
  //   const { container } = render(component(testProps));
  //   const exportSpy = jest.spyOn(dataSetApi(), 'exportToNotebook');
  //   const notebookName = 'Notebook Name';
  //   const expectedNotebookName = appendJupyterNotebookFileSuffix(notebookName);
  //   const expectedDatasetRequest: DataSetRequest = {
  //     dataSetId: dataset.id,
  //     name: dataset.name,
  //     domainValuePairs: dataset.domainValuePairs,
  //   };
  //
  //   container
  //     .querySelector('[data-test-id="notebook-name-input"]')
  //     .first()
  //     .simulate('change', { target: { value: notebookName } });
  //   findExportButton(container).simulate('click');
  //   expect(exportSpy).toHaveBeenCalledWith(workspace.namespace, workspace.id, {
  //     dataSetRequest: expectedDatasetRequest,
  //     newNotebook: true,
  //     notebookName: expectedNotebookName,
  //     kernelType: KernelTypeEnum.PYTHON,
  //     generateGenomicsAnalysisCode: true,
  //     genomicsAnalysisTool: DataSetExportRequestGenomicsAnalysisToolEnum.HAIL,
  //   });
  // });
  //
  // it('Auto reload code preview if genomics analysis tool is changed', async () => {
  //   const expectedDatasetRequest = {
  //     dataSetId: dataset.id,
  //     name: dataset.name,
  //     domainValuePairs: dataset.domainValuePairs,
  //   };
  //   datasetApiStub.codePreview = {
  //     html: '<div id="notebook">print("hello world!")</div>',
  //   };
  //   testProps.dataset.prePackagedConceptSet = [
  //     PrePackagedConceptSetEnum.WHOLE_GENOME,
  //   ];
  //   const { container } = render(component(testProps));
  //   const previewSpy = jest.spyOn(dataSetApi(), 'previewExportToNotebook');
  //
  //   container.querySelector('[data-test-id="code-preview-button"]').simulate('click');
  //   expect(previewSpy).toHaveBeenCalledWith(
  //     workspace.namespace,
  //     workspace.id,
  //     expect.objectContaining({
  //       dataSetRequest: expectedDatasetRequest,
  //       kernelType: KernelTypeEnum.PYTHON,
  //       genomicsAnalysisTool: DataSetExportRequestGenomicsAnalysisToolEnum.HAIL,
  //     })
  //   );
  //
  //   container
  //     .querySelector('[data-test-id="genomics-tool-PLINK"]')
  //     .first()
  //     .simulate('click');
  //   expect(previewSpy).toHaveBeenCalledWith(
  //     workspace.namespace,
  //     workspace.id,
  //     expect.objectContaining({
  //       dataSetRequest: expectedDatasetRequest,
  //       kernelType: KernelTypeEnum.PYTHON,
  //       genomicsAnalysisTool:
  //         DataSetExportRequestGenomicsAnalysisToolEnum.PLINK,
  //     })
  //   );
  //
  //   container
  //     .querySelector('[data-test-id="genomics-tool-NONE"]')
  //     .first()
  //     .simulate('click');
  //   expect(previewSpy).toHaveBeenCalledWith(
  //     workspace.namespace,
  //     workspace.id,
  //     expect.objectContaining({
  //       dataSetRequest: expectedDatasetRequest,
  //       kernelType: KernelTypeEnum.PYTHON,
  //       genomicsAnalysisTool: DataSetExportRequestGenomicsAnalysisToolEnum.NONE,
  //     })
  //   );
  // });
});
