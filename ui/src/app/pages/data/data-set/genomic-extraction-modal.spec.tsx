import '@testing-library/jest-dom';

import * as React from 'react';
import { mount } from 'enzyme';

import { DataSetApi, TerraJobStatus } from 'generated/fetch';

import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { registerApiClient } from 'app/services/swagger-fetch-clients';
import moment from 'moment';
import { SWRConfig } from 'swr';

import { waitOneTickAndUpdate } from 'testing/react-test-helpers';
import { DataSetApiStub } from 'testing/stubs/data-set-api-stub';
import { workspaceDataStub } from 'testing/stubs/workspaces';

import { GenomicExtractionModal } from './genomic-extraction-modal';

describe('GenomicExtractionModal', () => {
  let dataset;
  let datasetApiStub: DataSetApiStub;
  let testProps;
  let workspace;

  const component = async () => {
    const w = mount(
      <SWRConfig value={{ provider: () => new Map() }}>
        <GenomicExtractionModal {...testProps} />
      </SWRConfig>
    );
    await waitOneTickAndUpdate(w);
    return w;
  };

  const componentAlt = async () => {
    const component = render(
      <SWRConfig value={{ provider: () => new Map() }}>
        <GenomicExtractionModal {...testProps} />
      </SWRConfig>
    );
    await waitFor(() =>
      expect(screen.queryByLabelText('Please Wait')).not.toBeInTheDocument()
    );
    return component;
  };

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
      title: "Top 10 Egregious Hacks Your Tech Lead Doesn't Want You To Know",
    };
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  it('should render', async () => {
    await componentAlt();
    screen.getByText(
      /top 10 egregious hacks your tech lead doesn't want you to know/i
    );
  });

  it('should show a warning when there is a currently running extract for this dataset', async () => {
    const oneHourAgo = new Date();
    oneHourAgo.setHours(oneHourAgo.getHours() - 1);
    datasetApiStub.extractionJobs = [
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
      },
    ];

    await componentAlt();

    expect(
      screen.getByText(/an extraction is currently running for this dataset/i)
    ).toBeInTheDocument();
  });

  it('should show a warning message when the most recent extract has succeeded', async () => {
    datasetApiStub.extractionJobs = [
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
      },
    ];

    await componentAlt();

    expect(
      screen.getByText(/vcf file\(s\) already exist for this dataset\. /i)
    ).toBeInTheDocument();
  });

  it('should show a warning message the most recent extract has failed', async () => {
    datasetApiStub.extractionJobs = [
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
      },
    ];

    const wrapper = await component();
    const warning = wrapper.find('[data-test-id="extract-warning"]');
    expect(warning.exists()).toBeTruthy();
    expect(warning.text()).toContain(
      'Last time a VCF extract was attempted for this workflow, it failed.'
    );
  });

  it('should not show a warning message with no succeeded, failed, or running extracts for this dataset', async () => {
    datasetApiStub.extractionJobs = [
      {
        status: TerraJobStatus.ABORTED,
        completionTime: moment().unix(),
        datasetName: dataset.name,
      },
      {
        status: TerraJobStatus.RUNNING,
        datasetName: 'some other data set with a different name',
      },
    ];

    const wrapper = await component();
    expect(
      wrapper.find('[data-test-id="extract-warning"]').exists()
    ).toBeFalsy();
  });

  it('should show error text on known failed extract', async () => {
    const message = 'invalid dataset';
    jest
      .spyOn(datasetApiStub, 'extractGenomicData')
      .mockRejectedValueOnce(
        new Response(JSON.stringify({ message }), { status: 412 })
      );

    const wrapper = await component();
    await waitOneTickAndUpdate(wrapper);

    const extractButton = () =>
      wrapper.find('[data-test-id="extract-button"]').first();
    extractButton().simulate('click');
    await waitOneTickAndUpdate(wrapper);

    const error = wrapper.find('[data-test-id="extract-error"]');
    expect(error.exists()).toBeTruthy();
    expect(error.text()).toContain(message);

    // Client errors will not work on retry, disable the extract button.
    expect(extractButton().prop('disabled')).toBe(true);
  });

  it('should show error text on unknown error', async () => {
    jest
      .spyOn(datasetApiStub, 'extractGenomicData')
      .mockRejectedValueOnce(new Response(null, { status: 500 }));

    const wrapper = await component();
    await waitOneTickAndUpdate(wrapper);

    const extractButton = () =>
      wrapper.find('[data-test-id="extract-button"]').first();
    extractButton().simulate('click');
    await waitOneTickAndUpdate(wrapper);

    const error = wrapper.find('[data-test-id="extract-error"]');
    expect(error.exists()).toBeTruthy();

    // Unknown errors may be transient, allow the user to try again.
    expect(extractButton().prop('disabled')).toBe(false);
  });
});
