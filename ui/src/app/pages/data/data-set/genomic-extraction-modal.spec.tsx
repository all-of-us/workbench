import * as React from "react";

import {GenomicExtractionModal} from "./genomic-extraction-modal";
import {DataSetApiStub} from "testing/stubs/data-set-api-stub";
import {registerApiClient} from "app/services/swagger-fetch-clients";
import {DataSetApi, TerraJobStatus} from "generated/fetch";
import {mount} from "enzyme";
import {workspaceDataStub} from "testing/stubs/workspaces";
import {genomicExtractionStore} from "app/utils/stores";

describe('GenomicExtractionModal', () => {
  let dataset;
  let datasetApiStub;
  let testProps;
  let workspace;

  const component = () => {
    return <GenomicExtractionModal {...testProps}/>;
  }

  beforeEach(() => {
    datasetApiStub = new DataSetApiStub();
    registerApiClient(DataSetApi, datasetApiStub);

    dataset = datasetApiStub.getDatasetMock;
    workspace = workspaceDataStub;

    testProps = {
      dataSet: dataset,
      workspaceNamespace: workspace.namespace,
      workspaceFirecloudName: workspace.id,
      closeFunction: () => {},
      title: 'Top 10 Egregious Hacks Your Tech Lead Doesn\'t Want You To Know'
    }
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  it('should render', () => {
    const wrapper = mount(component());
    expect(wrapper.exists()).toBeTruthy();
  });

  it('should show a warning when there is a currently running extract for this dataset', () => {
    const oneHourAgo = new Date();
    oneHourAgo.setHours(oneHourAgo.getHours() - 1);
    genomicExtractionStore.set({
      [workspaceDataStub.namespace]: [
        {
          status: TerraJobStatus.RUNNING,
          datasetName: dataset.name,
        },
        {
          status: TerraJobStatus.SUCCEEDED,
          completionTime: oneHourAgo.getTime(),
          datasetName: dataset.name,
        },
        {
          status: TerraJobStatus.FAILED,
          completionTime: oneHourAgo.getTime(),
          datasetName: dataset.name,
        }
      ]
    });

    const wrapper = mount(component());
    expect(wrapper.find('[data-test-id="running-extract-warning"]')).toBeTruthy();
  });

  it('should show a warning message when there is a preexisting completed extract for the dataset', () => {
    const oneHourAgo = new Date();
    oneHourAgo.setHours(oneHourAgo.getHours() - 1);
    genomicExtractionStore.set({
      [workspaceDataStub.namespace]: [
        {
          status: TerraJobStatus.SUCCEEDED,
          completionTime: oneHourAgo.getTime(),
          datasetName: dataset.name,
        },
        {
          status: TerraJobStatus.FAILED,
          completionTime: new Date().getTime(),
          datasetName: dataset.name,
        },
        {
          status: TerraJobStatus.RUNNING,
          datasetName: 'some other data set with a different name',
        }
      ]
    });

    const wrapper = mount(component());
    expect(wrapper.find('[data-test-id="preexisting-extract-warning"]')).toBeTruthy();
  });

  it('should not show a warning message when there are only failed extracts for this dataset', () => {
    genomicExtractionStore.set({
      [workspaceDataStub.namespace]: [
        {
          status: TerraJobStatus.FAILED,
          completionTime: new Date().getTime(),
          datasetName: dataset.name,
        },
        {
          status: TerraJobStatus.RUNNING,
          datasetName: 'some other data set with a different name',
        }
      ]
    });

    const wrapper = mount(component());
    expect(wrapper.find('[data-test-id="preexisting-extract-warning"]').exists()).toBeFalsy();
    expect(wrapper.find('[data-test-id="running-extract-warning"]').exists()).toBeFalsy();
  });
});
