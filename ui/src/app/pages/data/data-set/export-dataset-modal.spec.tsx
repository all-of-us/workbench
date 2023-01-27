import * as React from 'react';
import { act } from 'react-dom/test-utils';
import { mount } from 'enzyme';

import {
  DataSetApi,
  DataSetExportRequest,
  DataSetRequest,
  KernelTypeEnum,
  NotebooksApi,
  PrePackagedConceptSetEnum,
} from 'generated/fetch';

import { Select } from 'app/components/inputs';
import { Tooltip } from 'app/components/popups';
import {
  appendNotebookFileSuffix,
  dropNotebookFileSuffix,
} from 'app/pages/analysis/util';
import { ExportDatasetModal } from 'app/pages/data/data-set/export-dataset-modal';
import {
  dataSetApi,
  registerApiClient,
} from 'app/services/swagger-fetch-clients';

import { waitOneTickAndUpdate } from 'testing/react-test-helpers';
import { DataSetApiStub } from 'testing/stubs/data-set-api-stub';
import { NotebooksApiStub } from 'testing/stubs/notebooks-api-stub';
import { workspaceDataStub } from 'testing/stubs/workspaces';

describe('ExportDatasetModal', () => {
  let dataset;
  let workspace;
  let testProps;
  let notebooksApiStub;
  let datasetApiStub;

  const component = (props) => {
    return <ExportDatasetModal {...props} />;
  };

  function findExportButton(wrapper) {
    return wrapper.find('[data-test-id="export-data-set"]').first();
  }

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
    jest.clearAllMocks();
  });

  it('should render', () => {
    const wrapper = mount(component(testProps));
    expect(wrapper.exists()).toBeTruthy();
  });

  it('should export to a new notebook', async () => {
    const wrapper = mount(component(testProps));
    const exportSpy = jest.spyOn(dataSetApi(), 'exportToNotebook');
    const notebookName = 'Notebook Name';
    const expectedNotebookName = appendNotebookFileSuffix(notebookName);
    const expectedDatasetRequest: DataSetRequest = {
      dataSetId: dataset.id,
      name: dataset.name,
      domainValuePairs: dataset.domainValuePairs,
    };

    wrapper
      .find('[data-test-id="notebook-name-input"]')
      .first()
      .simulate('change', { target: { value: notebookName } });
    findExportButton(wrapper).simulate('click');
    expect(exportSpy).toHaveBeenCalledWith(
      workspace.namespace,
      workspace.id,
      expect.objectContaining({
        dataSetRequest: expectedDatasetRequest,
        newNotebook: true,
        notebookName: expectedNotebookName,
        kernelType: KernelTypeEnum.Python,
        generateGenomicsAnalysisCode: false,
      })
    );
  });

  it('should export to a new notebook if the user types in the file suffix', async () => {
    const wrapper = mount(component(testProps));
    const exportSpy = jest.spyOn(dataSetApi(), 'exportToNotebook');
    const notebookName = 'MyNotebook.ipynb';
    const expectedNotebookName = notebookName;
    const expectedDatasetRequest: DataSetRequest = {
      dataSetId: dataset.id,
      name: dataset.name,
      domainValuePairs: dataset.domainValuePairs,
    };

    wrapper
      .find('[data-test-id="notebook-name-input"]')
      .first()
      .simulate('change', { target: { value: notebookName } });
    findExportButton(wrapper).simulate('click');
    expect(exportSpy).toHaveBeenCalledWith(
      workspace.namespace,
      workspace.id,
      expect.objectContaining({
        dataSetRequest: expectedDatasetRequest,
        newNotebook: true,
        notebookName: expectedNotebookName,
        kernelType: KernelTypeEnum.Python,
        generateGenomicsAnalysisCode: false,
      })
    );
  });

  it('should disable export if no name is provided', async () => {
    const wrapper = mount(component(testProps));
    const exportSpy = jest.spyOn(dataSetApi(), 'exportToNotebook');

    wrapper
      .find('[data-test-id="notebook-name-input"]')
      .first()
      .simulate('change', { target: { value: '' } });
    findExportButton(wrapper).simulate('click');
    expect(findExportButton(wrapper).props().disabled).toBeTruthy();
    expect(exportSpy).not.toHaveBeenCalled();

    findExportButton(wrapper).simulate('mouseenter');
    expect(wrapper.find(Tooltip).text()).toBe("Notebook name can't be blank");
  });

  it('should disable export if a conflicting name is provided, without the suffix', async () => {
    const existingNotebookName = 'existing notebook.ipynb';
    notebooksApiStub.notebookList = [
      {
        name: existingNotebookName,
      },
    ];
    const wrapper = mount(component(testProps));
    const exportSpy = jest.spyOn(dataSetApi(), 'exportToNotebook');

    wrapper
      .find('[data-test-id="notebook-name-input"]')
      .first()
      .simulate('change', {
        target: { value: dropNotebookFileSuffix(existingNotebookName) },
      });
    await waitOneTickAndUpdate(wrapper);
    findExportButton(wrapper).simulate('click');
    expect(findExportButton(wrapper).props().disabled).toBeTruthy();

    findExportButton(wrapper).simulate('mouseenter');
    expect(wrapper.find(Tooltip).text()).toBe('Notebook name already exists');
    expect(exportSpy).not.toHaveBeenCalled();
  });

  it('should disable export if a conflicting name is provided, including the suffix', async () => {
    const existingNotebookName = 'existing notebook.ipynb';
    notebooksApiStub.notebookList = [
      {
        name: existingNotebookName,
      },
    ];
    const wrapper = mount(component(testProps));
    const exportSpy = jest.spyOn(dataSetApi(), 'exportToNotebook');

    wrapper
      .find('[data-test-id="notebook-name-input"]')
      .first()
      .simulate('change', {
        target: { value: appendNotebookFileSuffix(existingNotebookName) },
      });
    await waitOneTickAndUpdate(wrapper);
    findExportButton(wrapper).simulate('click');
    expect(findExportButton(wrapper).props().disabled).toBeTruthy();

    findExportButton(wrapper).simulate('mouseenter');
    expect(wrapper.find(Tooltip).text()).toBe('Notebook name already exists');
    expect(exportSpy).not.toHaveBeenCalled();
  });

  it('should export to an existing notebook with the correct kernel type', async () => {
    const notebookName = 'existing notebook';
    const datasetName = 'dataset';
    const expectedNotebookName = appendNotebookFileSuffix(notebookName);
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

    const wrapper = mount(component(testProps));
    act(() => {
      wrapper.find(Select).props().onChange(notebookName);
    });
    await waitOneTickAndUpdate(wrapper);

    findExportButton(wrapper).simulate('click');

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
    const wrapper = mount(component(testProps));
    const previewSpy = jest.spyOn(dataSetApi(), 'previewExportToNotebook');

    wrapper.find('[data-test-id="code-preview-button"]').simulate('click');
    await waitOneTickAndUpdate(wrapper);

    expect(previewSpy).toHaveBeenCalledWith(
      workspace.namespace,
      workspace.id,
      expect.objectContaining({
        dataSetRequest: expectedDatasetRequest,
        kernelType: KernelTypeEnum.Python,
      })
    );
    expect(wrapper.find('iframe').html()).toContain('hello world!');

    wrapper.find('[data-test-id="kernel-type-r"]').first().simulate('click');
    await waitOneTickAndUpdate(wrapper);
    expect(previewSpy).toHaveBeenCalledWith(
      workspace.namespace,
      workspace.id,
      expect.objectContaining({
        dataSetRequest: expectedDatasetRequest,
        kernelType: KernelTypeEnum.R,
      })
    );

    wrapper.find('[data-test-id="code-preview-button"]').simulate('click');
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.find('iframe').exists()).toBeFalsy();
  });

  it('Show genomics analysis tools if WGS is in the dataset', async () => {
    testProps.dataset.prePackagedConceptSet = [
      PrePackagedConceptSetEnum.WHOLEGENOME,
    ];
    const wrapper = mount(component(testProps));

    Object.keys(DataSetExportRequest.GenomicsAnalysisToolEnum).forEach(
      (tool) => {
        expect(
          wrapper.find(`[data-test-id="genomics-tool-${tool}"]`).exists()
        ).toBeTruthy();
      }
    );
  });

  it('Remove genomics analysis tools if R is selected', async () => {
    testProps.dataset.prePackagedConceptSet = [
      PrePackagedConceptSetEnum.WHOLEGENOME,
    ];
    const wrapper = mount(component(testProps));

    wrapper.find('[data-test-id="kernel-type-r"]').first().simulate('click');
    await waitOneTickAndUpdate(wrapper);

    Object.keys(DataSetExportRequest.GenomicsAnalysisToolEnum).forEach(
      (tool) => {
        expect(
          wrapper.find(`[data-test-id="genomics-tool-${tool}"]`).exists()
        ).toBeFalsy();
      }
    );
  });

  it('Remove genomics analysis tools if no WGS', async () => {
    testProps.dataset.prePackagedConceptSet = [];
    const wrapper = mount(component(testProps));

    Object.keys(DataSetExportRequest.GenomicsAnalysisToolEnum).forEach(
      (tool) => {
        expect(
          wrapper.find(`[data-test-id="genomics-tool-${tool}"]`).exists()
        ).toBeFalsy();
      }
    );
  });

  it('Should export code with genomics analysis tool', async () => {
    testProps.dataset.prePackagedConceptSet = [
      PrePackagedConceptSetEnum.WHOLEGENOME,
    ];
    const wrapper = mount(component(testProps));
    const exportSpy = jest.spyOn(dataSetApi(), 'exportToNotebook');
    const notebookName = 'Notebook Name';
    const expectedNotebookName = appendNotebookFileSuffix(notebookName);
    const expectedDatasetRequest: DataSetRequest = {
      dataSetId: dataset.id,
      name: dataset.name,
      domainValuePairs: dataset.domainValuePairs,
    };

    wrapper
      .find('[data-test-id="notebook-name-input"]')
      .first()
      .simulate('change', { target: { value: notebookName } });
    findExportButton(wrapper).simulate('click');
    expect(exportSpy).toHaveBeenCalledWith(workspace.namespace, workspace.id, {
      dataSetRequest: expectedDatasetRequest,
      newNotebook: true,
      notebookName: expectedNotebookName,
      kernelType: KernelTypeEnum.Python,
      generateGenomicsAnalysisCode: true,
      genomicsAnalysisTool: DataSetExportRequest.GenomicsAnalysisToolEnum.HAIL,
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
      PrePackagedConceptSetEnum.WHOLEGENOME,
    ];
    const wrapper = mount(component(testProps));
    const previewSpy = jest.spyOn(dataSetApi(), 'previewExportToNotebook');

    wrapper.find('[data-test-id="code-preview-button"]').simulate('click');
    await waitOneTickAndUpdate(wrapper);
    expect(previewSpy).toHaveBeenCalledWith(
      workspace.namespace,
      workspace.id,
      expect.objectContaining({
        dataSetRequest: expectedDatasetRequest,
        kernelType: KernelTypeEnum.Python,
        genomicsAnalysisTool:
          DataSetExportRequest.GenomicsAnalysisToolEnum.HAIL,
      })
    );

    wrapper
      .find('[data-test-id="genomics-tool-PLINK"]')
      .first()
      .simulate('click');
    await waitOneTickAndUpdate(wrapper);
    expect(previewSpy).toHaveBeenCalledWith(
      workspace.namespace,
      workspace.id,
      expect.objectContaining({
        dataSetRequest: expectedDatasetRequest,
        kernelType: KernelTypeEnum.Python,
        genomicsAnalysisTool:
          DataSetExportRequest.GenomicsAnalysisToolEnum.PLINK,
      })
    );

    wrapper
      .find('[data-test-id="genomics-tool-NONE"]')
      .first()
      .simulate('click');
    await waitOneTickAndUpdate(wrapper);
    expect(previewSpy).toHaveBeenCalledWith(
      workspace.namespace,
      workspace.id,
      expect.objectContaining({
        dataSetRequest: expectedDatasetRequest,
        kernelType: KernelTypeEnum.Python,
        genomicsAnalysisTool:
          DataSetExportRequest.GenomicsAnalysisToolEnum.NONE,
      })
    );
  });
});
