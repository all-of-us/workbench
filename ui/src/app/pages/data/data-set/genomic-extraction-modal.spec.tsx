import * as React from "react";

import {GenomicExtractionModal} from "./genomic-extraction-modal";
import {DataSetApiStub} from "testing/stubs/data-set-api-stub";
import {registerApiClient} from "app/services/swagger-fetch-clients";
import {DataSetApi, TerraJobStatus} from "generated/fetch";
import {mount} from "enzyme";
import {workspaceDataStub} from "testing/stubs/workspaces";
import {genomicExtractionStore} from "app/utils/stores";
import moment = require("moment");

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
          completionTime: moment().subtract(1, 'hour').unix(),
          datasetName: dataset.name,
        },
        {
          status: TerraJobStatus.FAILED,
          completionTime: moment().subtract(2, 'hour').unix(),
          datasetName: dataset.name,
        }
      ]
    });

    const wrapper = mount(component());
    const warning = wrapper.find('[data-test-id="extract-warning"]');
    expect(warning).toBeTruthy();
    expect(warning.text()).toContain("An extraction is currently running");
  });

  it('should show a warning message when the most recent extract has succeeded', () => {
    genomicExtractionStore.set({
      [workspaceDataStub.namespace]: [
        {
          status: TerraJobStatus.SUCCEEDED,
          completionTime: moment().unix(),
          datasetName: dataset.name,
        },
        {
          status: TerraJobStatus.FAILED,
          completionTime: moment().subtract(1, 'hour').unix(),
          datasetName: dataset.name,
        },
        {
          status: TerraJobStatus.RUNNING,
          datasetName: 'some other data set with a different name',
        }
      ]
    });

    const wrapper = mount(component());
    const warning = wrapper.find('[data-test-id="extract-warning"]');
    expect(warning).toBeTruthy();
    expect(warning.text()).toContain("VCF file(s) already exist for this dataset.");
  });

  it('should show a warning message the most recent extract has failed', () => {
    genomicExtractionStore.set({
      [workspaceDataStub.namespace]: [
        {
          status: TerraJobStatus.FAILED,
          completionTime: moment().unix(),
          datasetName: dataset.name,
        },
        {
          status: TerraJobStatus.SUCCEEDED,
          completionTime: moment().subtract(1, 'hour').unix(),
          datasetName: dataset.name,
        },
        {
          status: TerraJobStatus.RUNNING,
          datasetName: 'some other data set with a different name',
        }
      ]
    });

    const wrapper = mount(component());
    const warning = wrapper.find('[data-test-id="extract-warning"]');
    expect(warning).toBeTruthy();
    expect(warning.text()).toContain("Last time a VCF extract was attempted for this workflow, it failed.");
  });

  it('should not show a warning message with no succeded, failed, or running extracts for this dataste', () => {
    genomicExtractionStore.set({
      [workspaceDataStub.namespace]: [
        {
          status: TerraJobStatus.ABORTED,
          completionTime: moment().unix(),
          datasetName: dataset.name,
        },
        {
          status: TerraJobStatus.RUNNING,
          datasetName: 'some other data set with a different name',
        }
      ]
    });

    const wrapper = mount(component());
    expect(wrapper.find('[data-test-id="extract-warning"]').exists()).toBeFalsy();
  });
});
