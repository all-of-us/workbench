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

import { fireEvent, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { appendJupyterNotebookFileSuffix } from 'app/pages/analysis/util';
import { ExportDatasetModal } from 'app/pages/data/data-set/export-dataset-modal';
import {
  dataSetApi,
  registerApiClient,
} from 'app/services/swagger-fetch-clients';

import { renderModal } from 'testing/react-test-helpers';
import { DataSetApiStub } from 'testing/stubs/data-set-api-stub';
import { NotebooksApiStub } from 'testing/stubs/notebooks-api-stub';
import { workspaceDataStub } from 'testing/stubs/workspaces';

describe('ExportDatasetModal', () => {
  let dataset;
  let workspace;
  let testProps;
  let notebooksApiStub;
  let datasetApiStub;
  let user;

  const component = (props) => {
    return renderModal(<ExportDatasetModal {...props} />);
  };

  function findExportButton() {
    return screen.getByRole('button', {
      name: /export/i,
    });
  }

  function findCopyCodeButton() {
    return screen.getByRole('button', {
      name: /copy code/i,
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

  const changeNotebookName = async (newNotebookName) => {
    await user.type(findNotebookNameInput(), newNotebookName);
  };

  const clickExportButton = () => {
    fireEvent.click(findExportButton());
  };

  const clickShowCodePreviewButton = () => {
    fireEvent.click(findSeeCodePreviewButton());
  };

  const clickHideCodePreviewButton = () => {
    fireEvent.click(findHideCodePreviewButton());
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

    user = userEvent.setup();
  });

  afterEach(() => {
    jest.resetAllMocks();
  });

  it('should render', async () => {
    component(testProps);
    await screen.findByText('Export Dataset');
  });

  it('should export to a new notebook', async () => {
    component(testProps);
    const exportSpy = jest.spyOn(dataSetApi(), 'exportToNotebook');
    const notebookName = 'Notebook Name';
    const expectedNotebookName = appendJupyterNotebookFileSuffix(notebookName);
    const expectedDatasetRequest: DataSetRequest = {
      dataSetId: dataset.id,
      name: dataset.name,
      domainValuePairs: dataset.domainValuePairs,
    };

    await waitUntilDoneLoading();

    await changeNotebookName(notebookName);
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
    component(testProps);
    const exportSpy = jest.spyOn(dataSetApi(), 'exportToNotebook');
    const notebookName = 'MyNotebook.ipynb';
    const expectedNotebookName = notebookName;
    const expectedDatasetRequest: DataSetRequest = {
      dataSetId: dataset.id,
      name: dataset.name,
      domainValuePairs: dataset.domainValuePairs,
    };

    await waitUntilDoneLoading();

    await changeNotebookName(notebookName);
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
    component(testProps);
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
    component(testProps);
    const exportSpy = jest.spyOn(dataSetApi(), 'exportToNotebook');

    await waitUntilDoneLoading();

    await changeNotebookName(existingNotebookName);
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
    component(testProps);
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

    component(testProps);

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
    component(testProps);
    const previewSpy = jest.spyOn(dataSetApi(), 'previewExportToNotebook');

    await waitUntilDoneLoading();

    clickShowCodePreviewButton();

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

    const rRadioButton = screen.getByRole('radio', {
      name: 'R',
    });

    fireEvent.click(rRadioButton);

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
    component(testProps);

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
    component(testProps);

    const rRadioButton = screen.getByRole('radio', {
      name: 'R',
    });

    fireEvent.click(rRadioButton);

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
    component(testProps);

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
    component(testProps);
    const exportSpy = jest.spyOn(dataSetApi(), 'exportToNotebook');
    const notebookName = 'Notebook Name';
    const expectedNotebookName = appendJupyterNotebookFileSuffix(notebookName);
    const expectedDatasetRequest: DataSetRequest = {
      dataSetId: dataset.id,
      name: dataset.name,
      domainValuePairs: dataset.domainValuePairs,
    };

    await waitUntilDoneLoading();

    await changeNotebookName(notebookName);
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
    const previewSpy = jest.spyOn(dataSetApi(), 'previewExportToNotebook');

    component(testProps);

    await waitUntilDoneLoading();
    clickShowCodePreviewButton();
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

    await waitUntilDoneLoading();

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

  it('Dataset copied to clipboard when "copy code" button is clicked', async () => {
    // Arrange
    datasetApiStub.codePreview = {
      text: '<div id="notebook">print("hello world!")</div>',
    };

    // Act
    component(testProps);
    await user.click(findCopyCodeButton());

    // Assert
    const clipboardText = await navigator.clipboard.readText();
    expect(clipboardText).toBe(
      '<div id="notebook">print("hello world!")</div>'
    );
    // Assert tooltip is shown
    await screen.findByText('Dataset query copied to clipboard');
  });

  it('Dataset copied to clipboard when "copy code" icon button is clicked in expanded view', async () => {
    // Arrange
    datasetApiStub.codePreview = {
      text: '<div id="notebook">print("hello world!")</div>',
    };

    // Act
    component(testProps);
    await waitUntilDoneLoading();

    clickShowCodePreviewButton();

    await waitUntilDoneLoading();
    await user.click(screen.getByLabelText('Dataset query copy icon button'));

    // Assert
    const clipboardText = await navigator.clipboard.readText();
    expect(clipboardText).toBe(
      '<div id="notebook">print("hello world!")</div>'
    );
    // Assert tooltip is shown
    await screen.findByText('Dataset query copied to clipboard');
  });

  it('Nothing should be clickable when notebooks are loading', async () => {
    testProps.dataset.prePackagedConceptSet = [
      PrePackagedConceptSetEnum.WHOLE_GENOME,
    ];
    const previewSpy = jest.spyOn(dataSetApi(), 'previewExportToNotebook');
    const exportSpy = jest.spyOn(dataSetApi(), 'exportToNotebook');
    // UI never finishes loading notebooks
    notebooksApiStub.getNoteBookList = () => new Promise(() => {});

    component(testProps);

    await changeNotebookName('New Notebook Name');
    expect(screen.queryByDisplayValue('New Notebook Name')).toBeNull();

    const rRadioButton = screen.getByRole('radio', {
      name: 'R',
    });
    await user.click(rRadioButton);
    expect(rRadioButton).not.toBeChecked();

    const plinkRadioButton = screen.getByRole('radio', {
      name: 'PLINK',
    });

    await user.click(plinkRadioButton);
    expect(plinkRadioButton).not.toBeChecked();

    clickShowCodePreviewButton();

    expect(previewSpy).not.toHaveBeenCalled();

    clickExportButton();
    expect(exportSpy).not.toHaveBeenCalled();

    await user.click(findCopyCodeButton());
    expect(screen.queryByText('Dataset query copied to clipboard')).toBeNull();
  });

  it('Nothing should be clickable when loading code preview', async () => {
    testProps.dataset.prePackagedConceptSet = [
      PrePackagedConceptSetEnum.WHOLE_GENOME,
    ];
    // Preview never resolves
    datasetApiStub.previewExportToNotebook = () => new Promise(() => {});
    const exportSpy = jest.spyOn(dataSetApi(), 'exportToNotebook');

    component(testProps);
    await waitUntilDoneLoading();
    clickShowCodePreviewButton();

    await changeNotebookName('New Notebook Name');
    expect(screen.queryByDisplayValue('New Notebook Name')).toBeNull();

    const rRadioButton = screen.getByRole('radio', {
      name: 'R',
    });
    await user.click(rRadioButton);
    expect(rRadioButton).not.toBeChecked();

    const plinkRadioButton = screen.getByRole('radio', {
      name: 'PLINK',
    });

    await user.click(plinkRadioButton);
    expect(plinkRadioButton).not.toBeChecked();

    clickHideCodePreviewButton();
    // When clicking hide, it should immediately disappear,
    // so if it is still here, then it was disabled.
    expect(findHideCodePreviewButton()).toBeInTheDocument();

    clickExportButton();
    expect(exportSpy).not.toHaveBeenCalled();

    await user.click(findCopyCodeButton());
    expect(screen.queryByText('Dataset query copied to clipboard')).toBeNull();
  });

  it('Nothing should be clickable when exporting', async () => {
    testProps.dataset.prePackagedConceptSet = [
      PrePackagedConceptSetEnum.WHOLE_GENOME,
    ];
    const previewSpy = jest.spyOn(dataSetApi(), 'previewExportToNotebook');
    // Export never resolves
    const exportSpy = jest
      .spyOn(dataSetApi(), 'exportToNotebook')
      .mockImplementation(() => new Promise(() => {}));

    component(testProps);
    await waitUntilDoneLoading();
    await changeNotebookName('Notebook Name');
    clickExportButton();

    await changeNotebookName('New Notebook Name');
    expect(screen.queryByDisplayValue('New Notebook Name')).toBeNull();

    const rRadioButton = screen.getByRole('radio', {
      name: 'R',
    });
    await user.click(rRadioButton);
    expect(rRadioButton).not.toBeChecked();

    const plinkRadioButton = screen.getByRole('radio', {
      name: 'PLINK',
    });

    await user.click(plinkRadioButton);
    expect(plinkRadioButton).not.toBeChecked();

    clickShowCodePreviewButton();
    expect(previewSpy).toHaveBeenCalledTimes(0);

    clickExportButton();
    // Should only be called the initial time and not allow a
    // subsequent click.
    expect(exportSpy).toHaveBeenCalledTimes(1);

    await user.click(findCopyCodeButton());
    expect(screen.queryByText('Dataset query copied to clipboard')).toBeNull();
  });

  it('Nothing should be clickable when copying code to clipboard', async () => {
    testProps.dataset.prePackagedConceptSet = [
      PrePackagedConceptSetEnum.WHOLE_GENOME,
    ];
    // Preview never resolves
    const previewSpy = jest
      .spyOn(dataSetApi(), 'previewExportToNotebook')
      .mockImplementation(() => new Promise(() => {}));
    // Export never resolves
    const exportSpy = jest.spyOn(dataSetApi(), 'exportToNotebook');

    component(testProps);
    await user.click(findCopyCodeButton());

    await changeNotebookName('New Notebook Name');
    expect(screen.queryByDisplayValue('New Notebook Name')).toBeNull();

    const rRadioButton = screen.getByRole('radio', {
      name: 'R',
    });
    await user.click(rRadioButton);
    expect(rRadioButton).not.toBeChecked();

    const plinkRadioButton = screen.getByRole('radio', {
      name: 'PLINK',
    });
    await user.click(plinkRadioButton);
    expect(plinkRadioButton).not.toBeChecked();

    clickShowCodePreviewButton();
    // Should only be called on the initial click
    expect(previewSpy).toHaveBeenCalledTimes(1);

    clickExportButton();
    expect(exportSpy).not.toHaveBeenCalled();

    // The button's text becomes a spinner while loading, so you
    // should not be able to find the button by its original text.
    expect(
      screen.queryByRole('button', {
        name: /copy code/i,
      })
    ).not.toBeInTheDocument();
  });
});
