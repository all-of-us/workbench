import '@testing-library/jest-dom';

import * as React from 'react';

import {
  DataSetApi,
  TerraJobStatus,
  WorkspaceAccessLevel,
} from 'generated/fetch';

import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {
  dataSetApi,
  registerApiClient,
} from 'app/services/swagger-fetch-clients';

import {
  expectMenuItemElementDisabled,
  expectMenuItemElementEnabled,
  renderModal,
} from 'testing/react-test-helpers';
import { DataSetApiStub } from 'testing/stubs/data-set-api-stub';
import {
  buildWorkspaceStub,
  WorkspaceStubVariables,
} from 'testing/stubs/workspaces';
import { ALL_TERRA_JOB_STATUSES, minus } from 'testing/utils';

import {
  GenomicsExtractionMenu,
  GenomicsExtractionMenuProps,
} from './genomics-extraction-menu';

describe(GenomicsExtractionMenu.name, () => {
  const defaultProps: GenomicsExtractionMenuProps = {
    job: { status: TerraJobStatus.RUNNING },
    workspace: {
      ...buildWorkspaceStub(),
      accessLevel: WorkspaceAccessLevel.OWNER,
    },
    onMutate: jest.fn(),
  };

  const component = async (
    propOverrides?: Partial<GenomicsExtractionMenuProps>
  ) =>
    renderModal(
      <GenomicsExtractionMenu {...{ ...defaultProps, ...propOverrides }} />
    );

  beforeEach(async () => {
    registerApiClient(DataSetApi, new DataSetApiStub());
    jest.useFakeTimers();
  });

  afterEach(() => {
    jest.clearAllTimers();
    jest.useRealTimers();
  });

  it('Renders all menu components', async () => {
    const user = userEvent.setup();
    await component();

    const menuTitle = screen.getByTitle('Genomic Extractions Action Menu');
    const menuIcon = menuTitle.parentElement;

    user.click(menuIcon);
    await waitFor(() => {
      expect(screen.getByText('View Path')).toBeInTheDocument();
      expect(screen.getByText('Abort Extraction')).toBeInTheDocument();
      expect(screen.getByText('Delete Extract')).toBeInTheDocument();
    });
  });

  it('Enables clicking View Path when the extraction succeeds and has an output location', async () => {
    const user = userEvent.setup();

    const datasetName = 'my dataset';
    await component({
      job: {
        datasetName,
        status: TerraJobStatus.SUCCEEDED,
        outputDir: 'some dir',
      },
    });

    const menuTitle = screen.getByTitle('Genomic Extractions Action Menu');
    const menuIcon = menuTitle.parentElement;

    user.click(menuIcon);
    const menuItem = await waitFor(async () => {
      const viewPathMenuItem = screen.getByText('View Path');
      expect(viewPathMenuItem).toBeInTheDocument();
      expectMenuItemElementEnabled(viewPathMenuItem);
      return viewPathMenuItem;
    });

    menuItem.click();
    await waitFor(() => {
      expect(menuItem).not.toBeInTheDocument();
      expect(
        screen.getByText(`GCS Path for ${datasetName} VCFs`)
      ).toBeInTheDocument();
    });
  });

  it('Disables clicking View Path when the extraction succeeds but has no output location', async () => {
    const user = userEvent.setup();

    const datasetName = 'my dataset';
    await component({
      job: {
        datasetName,
        status: TerraJobStatus.SUCCEEDED,
      },
    });

    const menuTitle = screen.getByTitle('Genomic Extractions Action Menu');
    const menuIcon = menuTitle.parentElement;

    user.click(menuIcon);
    await waitFor(async () => {
      const viewPathMenuItem = screen
        .getByText('View Path')
        .closest('[role~=button]');
      expect(viewPathMenuItem).toBeInTheDocument();
      expectMenuItemElementDisabled(viewPathMenuItem);
    });
  });

  test.each(minus(ALL_TERRA_JOB_STATUSES, [TerraJobStatus.SUCCEEDED]))(
    'Clicking View Path is disabled when the extraction has job status %s',
    async (status) => {
      const user = userEvent.setup();
      await component({
        job: {
          status,
          outputDir: 'any',
        },
      });

      const menuTitle = screen.getByTitle('Genomic Extractions Action Menu');
      const menuIcon = menuTitle.parentElement;

      user.click(menuIcon);
      await waitFor(async () => {
        const viewPathMenuItem = screen
          .getByText('View Path')
          .closest('[role~=button]');
        expect(viewPathMenuItem).toBeInTheDocument();
        expectMenuItemElementDisabled(viewPathMenuItem);
      });
    }
  );

  it('Enables aborting a running extraction', async () => {
    const abortSpy = jest
      .spyOn(dataSetApi(), 'abortGenomicExtractionJob')
      .mockImplementation((): Promise<any> => Promise.resolve());

    const user = userEvent.setup();

    const genomicExtractionJobId = 12345;
    await component({
      job: {
        status: TerraJobStatus.RUNNING,
        genomicExtractionJobId,
      },
    });

    const menuTitle = screen.getByTitle('Genomic Extractions Action Menu');
    const menuIcon = menuTitle.parentElement;

    user.click(menuIcon);
    const menuItem = await waitFor(async () => {
      const abortExtractionMenuItem = screen.getByText('Abort Extraction');
      expect(abortExtractionMenuItem).toBeInTheDocument();
      expectMenuItemElementEnabled(abortExtractionMenuItem);
      return abortExtractionMenuItem;
    });

    menuItem.click();
    await waitFor(() => {
      expect(abortSpy).toHaveBeenCalledTimes(1);
      expect(abortSpy).toHaveBeenCalledWith(
        WorkspaceStubVariables.DEFAULT_WORKSPACE_NS,
        WorkspaceStubVariables.DEFAULT_WORKSPACE_ID,
        genomicExtractionJobId.toString()
      );
    });
  });

  it('Disables aborting when the user has READER access', async () => {
    const user = userEvent.setup();

    const genomicExtractionJobId = 12345;
    await component({
      job: {
        status: TerraJobStatus.RUNNING,
        genomicExtractionJobId,
      },
      workspace: {
        ...defaultProps.workspace,
        accessLevel: WorkspaceAccessLevel.READER,
      },
    });

    const menuTitle = screen.getByTitle('Genomic Extractions Action Menu');
    const menuIcon = menuTitle.parentElement;

    user.click(menuIcon);
    await waitFor(async () => {
      const abortExtractionMenuItem = screen.getByText('Abort Extraction');
      expect(abortExtractionMenuItem).toBeInTheDocument();
      expectMenuItemElementDisabled(abortExtractionMenuItem);
    });
  });

  test.each(minus(ALL_TERRA_JOB_STATUSES, [TerraJobStatus.RUNNING]))(
    'Clicking Abort Extraction is disabled when the extraction has job status %s',
    async (status) => {
      const user = userEvent.setup();
      await component({
        job: {
          status,
          outputDir: 'any',
        },
      });

      const menuTitle = screen.getByTitle('Genomic Extractions Action Menu');
      const menuIcon = menuTitle.parentElement;

      user.click(menuIcon);
      await waitFor(async () => {
        const abortExtractionMenuItem = screen.getByText('Abort Extraction');
        expect(abortExtractionMenuItem).toBeInTheDocument();
        expectMenuItemElementDisabled(abortExtractionMenuItem);
      });
    }
  );

  // always disabled - not implemented yet?
  it('Disables clicking Delete Extract', async () => {
    const user = userEvent.setup();
    await component();

    const menuTitle = screen.getByTitle('Genomic Extractions Action Menu');
    const menuIcon = menuTitle.parentElement;

    user.click(menuIcon);
    await waitFor(() => {
      const deleteExtractionMenuItem = screen.getByText('Delete Extract');
      expect(deleteExtractionMenuItem).toBeInTheDocument();
      expectMenuItemElementDisabled(deleteExtractionMenuItem);
    });
  });
});
