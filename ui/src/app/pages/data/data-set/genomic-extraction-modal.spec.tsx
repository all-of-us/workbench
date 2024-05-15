import '@testing-library/jest-dom';

import * as React from 'react';

import { DataSetApi, TerraJobStatus } from 'generated/fetch';

import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { registerApiClient } from 'app/services/swagger-fetch-clients';
import moment from 'moment';
import { SWRConfig } from 'swr';

import {
  expectButtonElementDisabled,
  expectButtonElementEnabled,
} from 'testing/react-test-helpers';
import { DataSetApiStub } from 'testing/stubs/data-set-api-stub';
import { workspaceDataStub } from 'testing/stubs/workspaces';

import { GenomicExtractionModal } from './genomic-extraction-modal';

describe('GenomicExtractionModal', () => {
  let dataset;
  let datasetApiStub: DataSetApiStub;
  let testProps;
  let workspace;
  let user;

  const component = async () => {
    const renderResult = render(
      <SWRConfig value={{ provider: () => new Map() }}>
        <GenomicExtractionModal {...testProps} />
      </SWRConfig>
    );
    await waitFor(() =>
      expect(screen.queryByLabelText('Please Wait')).not.toBeInTheDocument()
    );
    return renderResult;
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
    user = userEvent.setup();
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  it('should render', async () => {
    await component();
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

    await component();

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

    await component();

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

    await component();

    expect(
      screen.getByText(
        /last time a vcf extract was attempted for this workflow, it failed\. /i
      )
    ).toBeInTheDocument();
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

    await component();
    expect(
      screen.queryByText(/an extraction is currently running for this dataset/i)
    ).not.toBeInTheDocument();
    expect(
      screen.queryByText(/vcf file\(s\) already exist for this dataset\. /i)
    ).not.toBeInTheDocument();
    expect(
      screen.queryByText(
        /last time a vcf extract was attempted for this workflow, it failed\. /i
      )
    ).not.toBeInTheDocument();
  });

  it('should show error text on known failed extract', async () => {
    const message = 'invalid dataset';
    jest
      .spyOn(datasetApiStub, 'extractGenomicData')
      .mockRejectedValueOnce(
        new Response(JSON.stringify({ message }), { status: 412 })
      );

    await component();

    const extractButton = await screen.findByRole('button', {
      name: /extract/i,
    });
    await user.click(extractButton);
    await screen.findByText(`Failed to launch extraction: ${message}.`);

    // Client errors will not work on retry, disable the extract button.
    expectButtonElementDisabled(extractButton);
  });

  it('should show error text on unknown error', async () => {
    jest
      .spyOn(datasetApiStub, 'extractGenomicData')
      .mockRejectedValueOnce(new Response(null, { status: 500 }));

    await component();

    const extractButton = await screen.findByRole('button', {
      name: /extract/i,
    });
    await user.click(extractButton);
    await screen.findByText(`Failed to launch extraction: unknown error.`);

    // Unknown errors may be transient, allow the user to try again.
    expectButtonElementEnabled(extractButton);
  });
});
