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
  getByText,
  render,
  screen,
  waitFor,
  within,
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

  function findSeeCodePreviewButton() {
    return screen.getByRole('button', {
      name: /see code preview/i,
    });
  }

  function findHideCodePreviewButton() {
    return screen.getByRole('button', {
      name: /hide code preview/i,
    });
  }

  function findNotebookNameInput() {
    return screen.getByLabelText('Notebook Name');
  }

  const waitUntilDoneLoading = async () =>
    await waitFor(() => {
      expect(screen.queryByLabelText('Please Wait')).toBeNull();
    });

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
    jest.resetAllMocks();
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

    await waitUntilDoneLoading();

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

    await waitUntilDoneLoading();

    const notebookNameInput = findNotebookNameInput();
    fireEvent.change(notebookNameInput, {
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

  it('should disable export if no name is provided', async () => {
    const { container, unmount } = render(component(testProps));
    const exportSpy = jest.spyOn(dataSetApi(), 'exportToNotebook');

    const notebookNameInput = findNotebookNameInput();
    fireEvent.change(notebookNameInput, {
      target: { value: '' },
    });

    const exportButton = findExportButton();
    fireEvent.click(exportButton);
    expect(exportSpy).not.toHaveBeenCalled();

    fireEvent.mouseEnter(exportButton);

    await screen.findByText("Notebook name can't be blank");
    unmount();
  });

  it('should disable export if a conflicting name is provided, without the suffix', async () => {
    const existingNotebookName = 'existing notebook1.ipynb';
    notebooksApiStub.notebookList = [
      {
        name: existingNotebookName,
      },
    ];
    const { container, unmount } = render(component(testProps));
    const exportSpy = jest.spyOn(dataSetApi(), 'exportToNotebook');

    await waitUntilDoneLoading();

    const notebookNameInput = findNotebookNameInput();
    fireEvent.change(notebookNameInput, {
      target: { value: dropJupyterNotebookFileSuffix(existingNotebookName) },
    });

    const exportButton = findExportButton();
    fireEvent.click(exportButton);
    expect(exportSpy).not.toHaveBeenCalled();

    fireEvent.mouseEnter(exportButton);

    await screen.findByText('Notebook name already exists');
    unmount();
  });

  it('should disable export if a conflicting name is provided, including the suffix', async () => {
    const existingNotebookName = 'existing notebook2.ipynb';
    notebooksApiStub.notebookList = [
      {
        name: existingNotebookName,
      },
    ];
    const { container, unmount } = render(component(testProps));
    const exportSpy = jest.spyOn(dataSetApi(), 'exportToNotebook');

    await waitUntilDoneLoading();

    const notebookNameInput = findNotebookNameInput();
    fireEvent.change(notebookNameInput, {
      target: { value: appendJupyterNotebookFileSuffix(existingNotebookName) },
    });

    const exportButton = findExportButton();
    fireEvent.click(exportButton);
    expect(exportSpy).not.toHaveBeenCalled();

    fireEvent.mouseEnter(exportButton);
    await screen.findByText('Notebook name already exists');

    unmount();
  });

  it('should export to an existing notebook with the correct kernel type', async () => {
    const notebookName = 'existing notebook';
    const datasetName = 'dataset';
    const expectedNotebookName = appendJupyterNotebookFileSuffix(notebookName);
    dataset.name = datasetName;
    notebooksApiStub.notebookList = [
      {
        name: expectedNotebookName,
      },
    ];
    notebooksApiStub.notebookKernel = KernelTypeEnum.R;

    const expectedDatasetRequest = {
      dataSetId: dataset.id,
      name: datasetName,
      domainValuePairs: dataset.domainValuePairs,
    };
    const exportSpy = jest.spyOn(dataSetApi(), 'exportToNotebook');

    const { container, unmount } = render(component(testProps));

    await waitUntilDoneLoading();

    const notebookDropdown = screen.getByText(/\(create a new notebook\)/i);
    fireEvent.mouseDown(notebookDropdown);

    const existingNotebookOption = screen.getByText(notebookName);
    fireEvent.click(existingNotebookOption);

    await waitFor(() => {
      expect(screen.queryByLabelText('Notebook Name')).toBeNull();
    });

    const exportButton = findExportButton();
    fireEvent.click(exportButton);

    expect(exportSpy).toHaveBeenCalledWith(
      workspace.namespace,
      workspace.id,
      expect.objectContaining({
        dataSetRequest: expectedDatasetRequest,
        newNotebook: false,
        notebookName: expectedNotebookName,
        kernelType: KernelTypeEnum.R,
      })
    );

    unmount();
  });

  it('should show code preview, auto reload on kernel switch, and hide code preview', async () => {
    const expectedDatasetRequest = {
      dataSetId: dataset.id,
      name: dataset.name,
      domainValuePairs: dataset.domainValuePairs,
    };

    datasetApiStub.codePreview = {
      html: '<div id="notebook">print("hello world!")</div>',
    };
    const { container, unmount } = render(component(testProps));
    const previewSpy = jest.spyOn(dataSetApi(), 'previewExportToNotebook');

    await waitUntilDoneLoading();

    const seeCodePreviewButton = findSeeCodePreviewButton();
    fireEvent.click(seeCodePreviewButton);

    await waitFor(() => {
      expect(screen.queryByLabelText('Please Wait')).toBeNull();
    });

    let iframe;
    await waitFor(() => {
      iframe = screen.getByTestId('export-preview-frame');
      expect(iframe).toBeInTheDocument();
      expect(iframe.contentDocument.readyState).toEqual('complete');
      expect(iframe.outerHTML).toContain('hello world!');
    });

    expect(previewSpy).toHaveBeenCalledWith(
      workspace.namespace,
      workspace.id,
      expect.objectContaining({
        dataSetRequest: expectedDatasetRequest,
        kernelType: KernelTypeEnum.PYTHON,
      })
    );

    const rRadioButtonLabel = screen.getByRole('radio', {
      name: 'R',
    });

    fireEvent.click(rRadioButtonLabel);

    expect(previewSpy).toHaveBeenCalledWith(
      workspace.namespace,
      workspace.id,
      expect.objectContaining({
        dataSetRequest: expectedDatasetRequest,
        kernelType: KernelTypeEnum.R,
      })
    );

    const hideCodePreviewButton = findHideCodePreviewButton();

    await waitFor(() => {
      expect(screen.queryByLabelText('Please Wait')).toBeNull();
    });
    screen.logTestingPlaygroundURL();
    fireEvent.click(hideCodePreviewButton);

    await waitFor(() => {
      iframe = screen.queryByTestId('export-preview-frame');
      expect(iframe).not.toBeInTheDocument();
    });

    unmount();
  });

  it('Show genomics analysis tools if WGS is in the dataset', async () => {
    testProps.dataset.prePackagedConceptSet = [
      PrePackagedConceptSetEnum.WHOLE_GENOME,
    ];
    const { container, unmount } = render(component(testProps));

    await screen.findByRole('radio', {
      name: 'Hail',
    });

    await screen.findByRole('radio', {
      name: 'PLINK',
    });

    await screen.findByRole('radio', {
      name: 'Other VCF-compatible tool',
    });

    unmount();
  });

  it('Remove genomics analysis tools if R is selected', async () => {
    testProps.dataset.prePackagedConceptSet = [
      PrePackagedConceptSetEnum.WHOLE_GENOME,
    ];
    const { container, unmount } = render(component(testProps));

    const rRadioButtonLabel = screen.getByRole('radio', {
      name: 'R',
    });

    fireEvent.click(rRadioButtonLabel);

    const hailRadio = screen.queryByRole('radio', {
      name: 'Hail',
    });
    expect(hailRadio).toBeNull();

    const plinkRadio = screen.queryByRole('radio', {
      name: 'PLINK',
    });
    expect(plinkRadio).toBeNull();

    const otherRadio = screen.queryByRole('radio', {
      name: 'Other VCF-compatible tool',
    });
    expect(otherRadio).toBeNull();

    unmount();
  });

  it('Remove genomics analysis tools if no WGS', async () => {
    testProps.dataset.prePackagedConceptSet = [];
    const { container, unmount } = render(component(testProps));

    const hailRadio = screen.queryByRole('radio', {
      name: 'Hail',
    });
    expect(hailRadio).toBeNull();

    const plinkRadio = screen.queryByRole('radio', {
      name: 'PLINK',
    });
    expect(plinkRadio).toBeNull();

    const otherRadio = screen.queryByRole('radio', {
      name: 'Other VCF-compatible tool',
    });
    expect(otherRadio).toBeNull();

    unmount();
  });

  it('Should export code with genomics analysis tool', async () => {
    testProps.dataset.prePackagedConceptSet = [
      PrePackagedConceptSetEnum.WHOLE_GENOME,
    ];
    const { container, unmount } = render(component(testProps));
    const exportSpy = jest.spyOn(dataSetApi(), 'exportToNotebook');
    const notebookName = 'Notebook Name';
    const expectedNotebookName = appendJupyterNotebookFileSuffix(notebookName);
    const expectedDatasetRequest: DataSetRequest = {
      dataSetId: dataset.id,
      name: dataset.name,
      domainValuePairs: dataset.domainValuePairs,
    };

    await waitUntilDoneLoading();

    const notebookNameInput = findNotebookNameInput();
    fireEvent.change(notebookNameInput, {
      target: { value: appendJupyterNotebookFileSuffix(notebookName) },
    });
    const exportButton = findExportButton();
    fireEvent.click(exportButton);

    expect(exportSpy).toHaveBeenCalledWith(workspace.namespace, workspace.id, {
      dataSetRequest: expectedDatasetRequest,
      newNotebook: true,
      notebookName: expectedNotebookName,
      kernelType: KernelTypeEnum.PYTHON,
      generateGenomicsAnalysisCode: true,
      genomicsAnalysisTool: DataSetExportRequestGenomicsAnalysisToolEnum.HAIL,
    });
    unmount();
  });

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
