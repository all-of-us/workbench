import '@testing-library/jest-dom';

import * as React from 'react';

import {
  DataSetApi,
  DataSetExportRequestGenomicsAnalysisToolEnum,
  DataSetRequest,
  KernelTypeEnum,
  NotebooksApi,
  PrePackagedConceptSetEnum,
} from 'generated/fetch';

import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { appendJupyterNotebookFileSuffix } from 'app/pages/analysis/util';
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
  let unmount;

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

  const changeNotebookName = (newNotebookName) => {
    fireEvent.change(findNotebookNameInput(), {
      target: { value: newNotebookName },
    });
  };

  const clickExportButton = () => {
    fireEvent.click(findExportButton());
  };

  const hoverOverExportButton = () => {
    fireEvent.mouseEnter(findExportButton());
  };

  beforeEach(() => {
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
    // React-modal makes changes to the ownerDocument where
    // a modal is found when the modal is closed. Since we are using
    // React Testing Library(RTL), components are generally tested in
    // isolation, so we have little concept of an owning document.
    // By using RTL's umount method, we are able to cleanup our
    // modal before its cleanup functionality is called.
    unmount();
    jest.resetAllMocks();
  });

  it('should render', async () => {
    ({ unmount } = render(component(testProps)));
    await screen.findByText('Export Dataset');
  });

  it('should export to a new notebook', async () => {
    ({ unmount } = render(component(testProps)));
    const exportSpy = jest.spyOn(dataSetApi(), 'exportToNotebook');
    const notebookName = 'Notebook Name';
    const expectedNotebookName = appendJupyterNotebookFileSuffix(notebookName);
    const expectedDatasetRequest: DataSetRequest = {
      dataSetId: dataset.id,
      name: dataset.name,
      domainValuePairs: dataset.domainValuePairs,
    };

    await waitUntilDoneLoading();

    changeNotebookName(notebookName);
    clickExportButton();
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
  });

  it('should export to a new notebook if the user types in the file suffix', async () => {
    ({ unmount } = render(component(testProps)));
    const exportSpy = jest.spyOn(dataSetApi(), 'exportToNotebook');
    const notebookName = 'MyNotebook.ipynb';
    const expectedNotebookName = notebookName;
    const expectedDatasetRequest: DataSetRequest = {
      dataSetId: dataset.id,
      name: dataset.name,
      domainValuePairs: dataset.domainValuePairs,
    };

    await waitUntilDoneLoading();

    changeNotebookName(notebookName);
    clickExportButton();

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
  });

  it('should disable export if no name is provided', async () => {
    ({ unmount } = render(component(testProps)));
    const exportSpy = jest.spyOn(dataSetApi(), 'exportToNotebook');

    changeNotebookName('');
    clickExportButton();

    expect(exportSpy).not.toHaveBeenCalled();

    hoverOverExportButton();

    await screen.findByText("Notebook name can't be blank");
  });

  it('should disable export if a conflicting name is provided, without the suffix', async () => {
    const existingNotebookName = 'existing notebook1.ipynb';
    notebooksApiStub.notebookList = [
      {
        name: existingNotebookName,
      },
    ];
    ({ unmount } = render(component(testProps)));
    const exportSpy = jest.spyOn(dataSetApi(), 'exportToNotebook');

    await waitUntilDoneLoading();

    changeNotebookName(existingNotebookName);
    clickExportButton();

    expect(exportSpy).not.toHaveBeenCalled();

    hoverOverExportButton();

    await screen.findByText('Notebook name already exists');
  });

  it('should disable export if a conflicting name is provided, including the suffix', async () => {
    const existingNotebookName = 'existing notebook2.ipynb';
    notebooksApiStub.notebookList = [
      {
        name: existingNotebookName,
      },
    ];
    ({ unmount } = render(component(testProps)));
    const exportSpy = jest.spyOn(dataSetApi(), 'exportToNotebook');

    await waitUntilDoneLoading();
    changeNotebookName(existingNotebookName);

    clickExportButton();
    expect(exportSpy).not.toHaveBeenCalled();

    hoverOverExportButton();
    await screen.findByText('Notebook name already exists');
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

    ({ unmount } = render(component(testProps)));

    await waitUntilDoneLoading();

    const notebookDropdown = screen.getByText(/\(create a new notebook\)/i);
    fireEvent.mouseDown(notebookDropdown);

    const existingNotebookOption = screen.getByText(notebookName);
    fireEvent.click(existingNotebookOption);

    await waitFor(() => {
      expect(screen.queryByLabelText('Notebook Name')).toBeNull();
    });

    clickExportButton();

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
    ({ unmount } = render(component(testProps)));
    const previewSpy = jest.spyOn(dataSetApi(), 'previewExportToNotebook');

    await waitUntilDoneLoading();

    const seeCodePreviewButton = findSeeCodePreviewButton();
    fireEvent.click(seeCodePreviewButton);

    await waitUntilDoneLoading();

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

    await waitUntilDoneLoading();

    const hideCodePreviewButton = findHideCodePreviewButton();
    fireEvent.click(hideCodePreviewButton);

    await waitFor(() => {
      iframe = screen.queryByTestId('export-preview-frame');
      expect(iframe).not.toBeInTheDocument();
    });
  });

  it('Show genomics analysis tools if WGS is in the dataset', async () => {
    testProps.dataset.prePackagedConceptSet = [
      PrePackagedConceptSetEnum.WHOLE_GENOME,
    ];
    ({ unmount } = render(component(testProps)));

    await screen.findByRole('radio', {
      name: 'Hail',
    });

    await screen.findByRole('radio', {
      name: 'PLINK',
    });

    await screen.findByRole('radio', {
      name: 'Other VCF-compatible tool',
    });
  });

  it('Remove genomics analysis tools if R is selected', async () => {
    testProps.dataset.prePackagedConceptSet = [
      PrePackagedConceptSetEnum.WHOLE_GENOME,
    ];
    ({ unmount } = render(component(testProps)));

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
  });

  it('Remove genomics analysis tools if no WGS', async () => {
    testProps.dataset.prePackagedConceptSet = [];
    ({ unmount } = render(component(testProps)));

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
  });

  it('Should export code with genomics analysis tool', async () => {
    testProps.dataset.prePackagedConceptSet = [
      PrePackagedConceptSetEnum.WHOLE_GENOME,
    ];
    ({ unmount } = render(component(testProps)));
    const exportSpy = jest.spyOn(dataSetApi(), 'exportToNotebook');
    const notebookName = 'Notebook Name';
    const expectedNotebookName = appendJupyterNotebookFileSuffix(notebookName);
    const expectedDatasetRequest: DataSetRequest = {
      dataSetId: dataset.id,
      name: dataset.name,
      domainValuePairs: dataset.domainValuePairs,
    };

    await waitUntilDoneLoading();

    changeNotebookName(notebookName);
    clickExportButton();

    expect(exportSpy).toHaveBeenCalledWith(workspace.namespace, workspace.id, {
      dataSetRequest: expectedDatasetRequest,
      newNotebook: true,
      notebookName: expectedNotebookName,
      kernelType: KernelTypeEnum.PYTHON,
      generateGenomicsAnalysisCode: true,
      genomicsAnalysisTool: DataSetExportRequestGenomicsAnalysisToolEnum.HAIL,
    });
  });

  it('Auto reload code preview if genomics analysis tool is changed', async () => {
    const expectedDatasetRequest = {
      dataSetId: dataset.id,
      name: dataset.name,
      domainValuePairs: dataset.domainValuePairs,
    };
    datasetApiStub.codePreview = {
      html: '<div id="notebook">print("hello world!")</div>',
    };
    testProps.dataset.prePackagedConceptSet = [
      PrePackagedConceptSetEnum.WHOLE_GENOME,
    ];
    ({ unmount } = render(component(testProps)));
    const previewSpy = jest.spyOn(dataSetApi(), 'previewExportToNotebook');

    const seeCodePreviewButton = findSeeCodePreviewButton();
    fireEvent.click(seeCodePreviewButton);
    await waitUntilDoneLoading();

    expect(previewSpy).toHaveBeenCalledWith(
      workspace.namespace,
      workspace.id,
      expect.objectContaining({
        dataSetRequest: expectedDatasetRequest,
        kernelType: KernelTypeEnum.PYTHON,
        genomicsAnalysisTool: DataSetExportRequestGenomicsAnalysisToolEnum.HAIL,
      })
    );

    const plinkjRadioButtonLabel = screen.getByRole('radio', {
      name: 'PLINK',
    });
    fireEvent.click(plinkjRadioButtonLabel);

    expect(previewSpy).toHaveBeenCalledWith(
      workspace.namespace,
      workspace.id,
      expect.objectContaining({
        dataSetRequest: expectedDatasetRequest,
        kernelType: KernelTypeEnum.PYTHON,
        genomicsAnalysisTool:
          DataSetExportRequestGenomicsAnalysisToolEnum.PLINK,
      })
    );

    const otherjRadioButtonLabel = screen.getByRole('radio', {
      name: 'Other VCF-compatible tool',
    });
    fireEvent.click(otherjRadioButtonLabel);
    expect(previewSpy).toHaveBeenCalledWith(
      workspace.namespace,
      workspace.id,
      expect.objectContaining({
        dataSetRequest: expectedDatasetRequest,
        kernelType: KernelTypeEnum.PYTHON,
        genomicsAnalysisTool: DataSetExportRequestGenomicsAnalysisToolEnum.NONE,
      })
    );
  });
});
