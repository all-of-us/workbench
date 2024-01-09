import '@testing-library/jest-dom';

import * as React from 'react';

import { DiskType, RuntimeStatus } from 'generated/fetch';

import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { toAnalysisConfig } from 'app/utils/analysis-config';
import { serverConfigStore } from 'app/utils/stores';

import defaultServerConfig from 'testing/default-server-config';
import { stubDisk } from 'testing/stubs/disks-api-stub';
import { defaultGceRuntimeWithPd } from 'testing/stubs/runtime-api-stub';
import { ALL_RUNTIME_STATUSES, minus } from 'testing/utils';

import {
  CustomizePanelFooter,
  CustomizePanelFooterProps,
} from './customize-panel-footer';
import { PanelContent } from './utils';

const onClose = jest.fn();
const requestAnalysisConfig = jest.fn();
const setPanelContent = jest.fn();
const currentRuntime = defaultGceRuntimeWithPd();
const gcePersistentDisk = stubDisk();
const analysisConfig = toAnalysisConfig(currentRuntime, gcePersistentDisk);
const existingAnalysisConfig = analysisConfig;
const defaultProps: CustomizePanelFooterProps = {
  disableControls: false,
  runtimeCanBeCreated: false,
  runtimeCanBeUpdated: false,
  runtimeExists: false,
  unattachedPdExists: false,
  onClose,
  requestAnalysisConfig,
  setPanelContent,
  currentRuntime,
  gcePersistentDisk,
  analysisConfig,
  existingAnalysisConfig,
};

describe(CustomizePanelFooter.name, () => {
  const component = async (
    propOverrides?: Partial<CustomizePanelFooterProps>
  ) =>
    render(<CustomizePanelFooter {...{ ...defaultProps, ...propOverrides }} />);

  beforeEach(async () => {
    serverConfigStore.set({
      config: defaultServerConfig,
    });
  });

  it('allows deleting a PD when one exists', async () => {
    await component({ unattachedPdExists: true });
    const deletePDButton = screen.queryByRole('button', {
      name: 'Delete Persistent Disk',
    });
    expect(deletePDButton).toBeInTheDocument();
    deletePDButton.click();
    await waitFor(() =>
      expect(setPanelContent).toHaveBeenCalledWith(
        PanelContent.DeleteUnattachedPd
      )
    );
  });

  it('does not allow deleting a PD when none exists', async () => {
    await component({ unattachedPdExists: false });
    const deletePDButton = screen.queryByRole('button', {
      name: 'Delete Persistent Disk',
    });
    expect(deletePDButton).not.toBeInTheDocument();
  });

  const recreatePdTooBigConfig = {
    unattachedPdExists: true,
    runtimeCanBeCreated: true,
    analysisConfig: {
      ...analysisConfig,
      diskConfig: { ...analysisConfig.diskConfig, detachable: true }, // pretty sure this is already true but be explicit
    },
    gcePersistentDisk: {
      ...gcePersistentDisk,
      size: gcePersistentDisk.size + 1, // the existing PD is bigger than requested
    },
  };
  it('should recreate the attached PD when too big', async () => {
    await component(recreatePdTooBigConfig);
    const nextButton = screen.queryByRole('button', { name: 'Next' });
    expect(nextButton).toBeInTheDocument();
    nextButton.click();
    await waitFor(() =>
      expect(setPanelContent).toHaveBeenCalledWith(
        PanelContent.DeleteUnattachedPdAndCreate
      )
    );
  });

  it('should disable PD recreation when the attached PD is too big but !runtimeCanBeCreated', async () => {
    await component({ ...recreatePdTooBigConfig, runtimeCanBeCreated: false });
    const nextButton = screen.queryByRole('button', { name: 'Next' });
    expect(nextButton).toBeInTheDocument();
    nextButton.click();
    await waitFor(() => expect(setPanelContent).not.toHaveBeenCalled());
  });

  it('should create the analysisConfig without PD recreation when the PD is too big but !unattachedPdExists', async () => {
    await component({ ...recreatePdTooBigConfig, unattachedPdExists: false });
    const createButton = screen.queryByRole('button', { name: 'Create' });
    expect(createButton).toBeInTheDocument();
    createButton.click();
    await waitFor(() => {
      expect(requestAnalysisConfig).toHaveBeenCalledWith(analysisConfig);
      expect(onClose).toHaveBeenCalled();
    });
  });

  it('should create the analysisConfig without PD recreation when the PD is too big but !detachable', async () => {
    const newAnalysisConfig = {
      ...analysisConfig,
      diskConfig: { ...analysisConfig.diskConfig, detachable: false },
    };
    await component({
      ...recreatePdTooBigConfig,
      analysisConfig: newAnalysisConfig,
    });
    const createButton = screen.queryByRole('button', { name: 'Create' });
    expect(createButton).toBeInTheDocument();
    createButton.click();
    await waitFor(() => {
      expect(requestAnalysisConfig).toHaveBeenCalledWith(newAnalysisConfig);
      expect(onClose).toHaveBeenCalled();
    });
  });

  const recreatePdWrongTypeConfig = {
    unattachedPdExists: true,
    runtimeCanBeCreated: true,
    analysisConfig: {
      ...analysisConfig,
      diskConfig: { ...analysisConfig.diskConfig, detachable: true }, // pretty sure this is already true but be explicit
    },
    gcePersistentDisk: {
      ...gcePersistentDisk,
      diskType: DiskType.SSD,
    },
  };
  it('should recreate the attached PD when the disk is of the wrong type', async () => {
    // sanity check test validity
    expect(gcePersistentDisk.diskType).not.toEqual(
      recreatePdWrongTypeConfig.gcePersistentDisk.diskType
    );

    await component(recreatePdWrongTypeConfig);
    const nextButton = screen.queryByRole('button', { name: 'Next' });
    expect(nextButton).toBeInTheDocument();
    nextButton.click();
    await waitFor(() =>
      expect(setPanelContent).toHaveBeenCalledWith(
        PanelContent.DeleteUnattachedPdAndCreate
      )
    );
  });

  it('should disable PD recreation when the attached PD is the wrong type but !runtimeCanBeCreated', async () => {
    await component({
      ...recreatePdWrongTypeConfig,
      runtimeCanBeCreated: false,
    });
    const nextButton = screen.queryByRole('button', { name: 'Next' });
    expect(nextButton).toBeInTheDocument();
    nextButton.click();
    await waitFor(() => expect(setPanelContent).not.toHaveBeenCalled());
  });

  it('should create the analysisConfig without PD recreation when the PD is the wrong type but !unattachedPdExists', async () => {
    await component({
      ...recreatePdWrongTypeConfig,
      unattachedPdExists: false,
    });
    const createButton = screen.queryByRole('button', { name: 'Create' });
    expect(createButton).toBeInTheDocument();
    createButton.click();
    await waitFor(() => {
      expect(requestAnalysisConfig).toHaveBeenCalledWith(analysisConfig);
      expect(onClose).toHaveBeenCalled();
    });
  });

  it('should create the analysisConfig without PD recreation when the PD is the wrong type but !detachable', async () => {
    const newAnalysisConfig = {
      ...analysisConfig,
      diskConfig: { ...analysisConfig.diskConfig, detachable: false },
    };
    await component({
      ...recreatePdWrongTypeConfig,
      analysisConfig: newAnalysisConfig,
    });
    const createButton = screen.queryByRole('button', { name: 'Create' });
    expect(createButton).toBeInTheDocument();
    createButton.click();
    await waitFor(() => {
      expect(requestAnalysisConfig).toHaveBeenCalledWith(newAnalysisConfig);
      expect(onClose).toHaveBeenCalled();
    });
  });

  const canDeleteStatuses = [
    RuntimeStatus.RUNNING,
    RuntimeStatus.STOPPED,
    RuntimeStatus.ERROR,
  ];
  test.each(canDeleteStatuses)(
    'it allows deleting the environment when a runtime is %s and no detached PD exists',
    async (status) => {
      await component({
        currentRuntime: { ...currentRuntime, status },
        unattachedPdExists: false,
        runtimeExists: false, // the previous logic relied on this value - show that it no longer applies
      });
      const deleteButton = screen.queryByRole('button', {
        name: 'Delete Environment',
      });
      expect(deleteButton).toBeInTheDocument();
      deleteButton.click();
      await waitFor(() =>
        expect(setPanelContent).toHaveBeenCalledWith(PanelContent.DeleteRuntime)
      );
    }
  );

  test.each(minus(ALL_RUNTIME_STATUSES, canDeleteStatuses))(
    'it does not allow deleting the environment when a runtime is %s and no detached PD exists',
    async (status) => {
      await component({
        currentRuntime: { ...currentRuntime, status },
        unattachedPdExists: false,
        runtimeExists: true, // the previous logic relied on this value - show that it no longer applies
      });
      const deleteButton = screen.queryByRole('button', {
        name: 'Delete Environment',
      });
      expect(deleteButton).toBeInTheDocument();
      deleteButton.click();
      await waitFor(() => expect(setPanelContent).not.toHaveBeenCalled());
    }
  );

  it('does not allow deleting the environment when controls are disabled', async () => {
    await component({
      runtimeExists: true,
      unattachedPdExists: false,
      disableControls: true,
    });
    const deleteButton = screen.queryByRole('button', {
      name: 'Delete Environment',
    });
    expect(deleteButton).toBeInTheDocument();
    deleteButton.click();
    await waitFor(() => expect(setPanelContent).not.toHaveBeenCalled());
  });

  it.each([
    [
      'Next',
      'allows',
      'the runtime exists and we are not removing the disk',
      {
        unattachedPdExists: false,
        runtimeExists: true,
        runtimeCanBeUpdated: true,
        analysisConfig: {
          ...analysisConfig,
          diskConfig: { ...analysisConfig.diskConfig, detachable: true }, // pretty sure this is already true but be explicit
        },
      },
      () =>
        expect(setPanelContent).toHaveBeenCalledWith(
          PanelContent.ConfirmUpdate
        ),
    ],
    [
      'Next',
      'disallows',
      'the runtime exists and we are not removing the disk, but the runtime cannot be updated',
      {
        unattachedPdExists: false,
        runtimeExists: true,
        runtimeCanBeUpdated: false,
        analysisConfig: {
          ...analysisConfig,
          diskConfig: { ...analysisConfig.diskConfig, detachable: true }, // pretty sure this is already true but be explicit
        },
      },
      () => expect(setPanelContent).not.toHaveBeenCalled(),
    ],
    [
      'Next',
      'allows',
      'the runtime exists and we are removing the disk',
      {
        unattachedPdExists: false,
        runtimeExists: true,
        runtimeCanBeUpdated: true,
        analysisConfig: {
          ...analysisConfig,
          diskConfig: {
            ...analysisConfig.diskConfig,
            // existing config has detachable: true so this is a removal
            detachable: false,
          },
        },
      },
      () =>
        expect(setPanelContent).toHaveBeenCalledWith(
          PanelContent.ConfirmUpdateWithDiskDelete
        ),
    ],
    [
      'Next',
      'disallows',
      'the runtime exists and we are removing the disk, but the runtime cannot be updated',
      {
        unattachedPdExists: false,
        runtimeExists: true,
        runtimeCanBeUpdated: false,
        analysisConfig: {
          ...analysisConfig,
          diskConfig: {
            ...analysisConfig.diskConfig,
            // existing config has detachable: true so this is a removal
            detachable: false,
          },
        },
      },
      () => expect(setPanelContent).not.toHaveBeenCalled(),
    ],
    [
      'Try Again',
      'allows',
      'the runtime does not exist and the currentRuntime has errors', // TODO better names for these!
      {
        unattachedPdExists: false,
        unattachedDiskNeedsRecreate: false,
        runtimeExists: false,
        runtimeCanBeCreated: true,
        currentRuntime: {
          ...currentRuntime,
          errors: [{ errorMessage: 'oops!' }],
        },
      },
      () => {
        expect(requestAnalysisConfig).toHaveBeenCalledWith(analysisConfig);
        expect(onClose).toHaveBeenCalled();
      },
    ],
    [
      'Try Again',
      'disallows',
      'the runtime does not exist and the currentRuntime has errors, but the runtime cannot be created',
      {
        unattachedPdExists: false,
        unattachedDiskNeedsRecreate: false,
        runtimeExists: false,
        runtimeCanBeCreated: false,
        currentRuntime: {
          ...currentRuntime,
          errors: [{ errorMessage: 'oops!' }],
        },
      },
      () => {
        expect(requestAnalysisConfig).not.toHaveBeenCalled();
        expect(onClose).not.toHaveBeenCalled();
      },
    ],
    [
      'Create',
      'allows',
      'the runtime does not exist and the currentRuntime has no errors', // TODO better names for these!
      {
        unattachedPdExists: false,
        unattachedDiskNeedsRecreate: false,
        runtimeExists: false,
        runtimeCanBeCreated: true,
        currentRuntime: {
          ...currentRuntime,
          errors: [], // be explicit about the lack of errors
        },
      },
      () => {
        expect(requestAnalysisConfig).toHaveBeenCalledWith(analysisConfig);
        expect(onClose).toHaveBeenCalled();
      },
    ],
    [
      'Create',
      'disallows',
      'the runtime does not exist and the currentRuntime has no errors, but the runtime cannot be created', // TODO better names for these!
      {
        unattachedPdExists: false,
        unattachedDiskNeedsRecreate: false,
        runtimeExists: false,
        runtimeCanBeCreated: false,
        currentRuntime: {
          ...currentRuntime,
          errors: [], // be explicit about the lack of errors
        },
      },
      () => {
        expect(requestAnalysisConfig).not.toHaveBeenCalled();
        expect(onClose).not.toHaveBeenCalled();
      },
    ],
  ])(
    'renders the %s button and %s clicking when %s ',
    async (
      name: string,
      allowsDisallows: string,
      description: string,
      config: Partial<CustomizePanelFooterProps>,
      expectation: () => void
    ) => {
      await component(config);
      const button = screen.queryByRole('button', { name });
      expect(button).toBeInTheDocument();
      button.click();
      await waitFor(expectation);
    }
  );

  it('shows disabled tooltip when "Next" button is disabled', async () => {
    const user = userEvent.setup();
    await component({
      runtimeExists: true,
      runtimeCanBeUpdated: false,
      runtimeCannotBeUpdatedExplanation: 'Testing tooltip',
    });
    const nextButton = screen.queryByRole('button', {
      name: 'Next',
    });
    expect(nextButton).toBeInTheDocument();
    await user.pointer([{ pointerName: 'mouse', target: nextButton }]);
    // Show tooltip when hovering over disabled button.
    screen.getByText('Testing tooltip');
  });

  it('does not show disabled tooltip when "Next" button is enabled', async () => {
    const user = userEvent.setup();
    await component({
      runtimeExists: true,
      runtimeCanBeUpdated: true,
      runtimeCannotBeUpdatedExplanation: 'Testing tooltip',
    });
    const nextButton = screen.queryByRole('button', {
      name: 'Next',
    });
    expect(nextButton).toBeInTheDocument();
    await user.pointer([{ pointerName: 'mouse', target: nextButton }]);
    // Show tooltip when hovering over disabled button.
    expect(screen.queryByText('Testing tooltip')).not.toBeInTheDocument();
  });

  it('shows disabled tooltip when "Create" button is disabled', async () => {
    const user = userEvent.setup();
    await component({
      runtimeExists: false,
      runtimeCanBeCreated: false,
      runtimeCannotBeCreatedExplanation: 'Testing tooltip',
    });
    const createButton = screen.queryByRole('button', {
      name: 'Create',
    });
    expect(createButton).toBeInTheDocument();
    await user.pointer([{ pointerName: 'mouse', target: createButton }]);
    // Show tooltip when hovering over disabled button.
    screen.getByText('Testing tooltip');
  });

  it('does not show disabled tooltip when "Create" button is enabled', async () => {
    const user = userEvent.setup();
    await component({
      runtimeExists: false,
      runtimeCanBeCreated: true,
      runtimeCannotBeCreatedExplanation: 'Testing tooltip',
    });
    const createButton = screen.queryByRole('button', {
      name: 'Create',
    });
    expect(createButton).toBeInTheDocument();
    await user.pointer([{ pointerName: 'mouse', target: createButton }]);
    // Show tooltip when hovering over disabled button.
    expect(screen.queryByText('Testing tooltip')).not.toBeInTheDocument();
  });
});
