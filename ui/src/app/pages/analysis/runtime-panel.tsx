import {
  Button,
  Clickable,
  LinkButton,
  StyledExternalLink,
} from 'app/components/buttons';
import { FlexColumn, FlexRow } from 'app/components/flex';
import { ClrIcon } from 'app/components/icons';
import { ErrorMessage, WarningMessage } from 'app/components/messages';
import { TooltipTrigger } from 'app/components/popups';
import { Spinner } from 'app/components/spinners';
import { TextColumn } from 'app/components/text-column';

import { diskApi, workspacesApi } from 'app/services/swagger-fetch-clients';
import colors, { addOpacity, colorWithWhiteness } from 'app/styles/colors';
import {
  cond,
  DEFAULT,
  reactStyles,
  summarizeErrors,
  switchCase,
  withCdrVersions,
  withCurrentWorkspace,
  withUserProfile,
} from 'app/utils';
import {
  AutopauseMinuteThresholds,
  ComputeType,
  DEFAULT_AUTOPAUSE_THRESHOLD_MINUTES,
  DEFAULT_MACHINE_NAME,
  DEFAULT_MACHINE_TYPE,
  detachableDiskPricePerMonth,
  findMachineByName,
  getValidGpuTypes,
  gpuTypeToDisplayName,
  Machine,
  machineRunningCost,
  validLeoDataprocMasterMachineTypes,
  validLeoDataprocWorkerMachineTypes,
  validLeoGceMachineTypes,
} from 'app/utils/machines';
import { formatUsd } from 'app/utils/numbers';
import { applyPresetOverride, runtimePresets } from 'app/utils/runtime-presets';
import { isUsingFreeTierBillingAccount } from 'app/utils/workspace-utils';
import { RuntimeCostEstimator } from 'app/components/runtime-cost-estimator';
import { RuntimeSummary } from 'app/components/runtime-summary';
import {
  diffsToUpdateMessaging,
  DiskConfig,
  diskTypeLabels,
  fromAnalysisConfig,
  getAnalysisConfigDiffs,
  maybeWithExistingDiskName,
  AnalysisDiff,
  RuntimeStatusRequest,
  toAnalysisConfig,
  UpdateMessaging,
  useCustomRuntime,
  useRuntimeStatus,
  withAnalysisConfigDefaults,
  AnalysisConfig,
} from 'app/utils/runtime-utils';
import {
  diskStore,
  runtimeStore,
  serverConfigStore,
  useStore,
  withStore,
} from 'app/utils/stores';

import { CheckBox, RadioButton } from 'app/components/inputs';
import { AoU } from 'app/components/text-wrappers';
import { findCdrVersion } from 'app/utils/cdr-versions';
import { supportUrls } from 'app/utils/zendesk';
import {
  BillingStatus,
  DataprocConfig,
  Disk,
  DiskType,
  GpuConfig,
  Runtime,
  RuntimeConfigurationType,
  RuntimeStatus,
} from 'generated/fetch';
import * as fp from 'lodash/fp';
import { Dropdown } from 'primereact/dropdown';
import { InputNumber } from 'primereact/inputnumber';
import * as React from 'react';
import { validate } from 'validate.js';

const { useState, useEffect, Fragment } = React;

import computeStarting from 'assets/icons/compute-starting.svg';
import computeRunning from 'assets/icons/compute-running.svg';
import computeStopping from 'assets/icons/compute-stopping.svg';
import computeError from 'assets/icons/compute-error.svg';
import computeStopped from 'assets/icons/compute-stopped.svg';
import computeNone from 'assets/icons/compute-none.svg';
import { SparkConsolePath } from './leonardo-app-launcher';
import { RouteLink } from 'app/components/app-router';
import { WorkspaceData } from 'app/utils/workspace-data';

const styles = reactStyles({
  baseHeader: {
    color: colors.primary,
    fontSize: '16px',
    lineHeight: '1rem',
    margin: 0,
  },
  sectionHeader: {
    marginBottom: '12px',
    marginTop: '12px',
  },
  bold: {
    fontWeight: 700,
  },
  label: {
    fontWeight: 600,
    marginRight: '.5rem',
  },
  labelAndInput: {
    alignItems: 'center',
  },
  controlSection: {
    backgroundColor: String(addOpacity(colors.white, 0.75)),
    borderRadius: '3px',
    padding: '.75rem',
  },
  presetMenuItem: {
    color: colors.primary,
    fontSize: '14px',
  },
  formGrid2: {
    display: 'grid',
    gridTemplateColumns: 'repeat(2, 1fr)',
    gridGap: '1rem',
    alignItems: 'center',
  },
  formGrid3: {
    display: 'grid',
    gridTemplateColumns: 'repeat(3, 1fr)',
    gridGap: '1rem',
    alignItems: 'center',
  },
  sectionTitle: {
    fontWeight: 600,
    marginBottom: '0.5rem',
  },
  inputNumber: {
    backgroundColor: colors.white,
    padding: '.75rem .5rem',
    width: '2rem',
  },
  errorMessage: {
    backgroundColor: colorWithWhiteness(colors.highlight, 0.5),
    marginTop: '0.5rem',
    color: colors.primary,
    fontSize: '14px',
    padding: '0.5rem',
    borderRadius: '0.5em',
  },
  costPredictorWrapper: {
    backgroundColor: colorWithWhiteness(colors.accent, 0.85),
    // Not using shorthand here because react doesn't like it when you mix shorthand and non-shorthand,
    // and the border color changes when the runtime does
    borderWidth: '1px',
    borderStyle: 'solid',
    borderColor: colorWithWhiteness(colors.dark, 0.5),
    borderRadius: '5px',
    color: colors.dark,
  },
  costComparison: {
    padding: '.25rem .5rem',
    width: '400px',
  },
  costsDrawnFrom: {
    borderLeft: `1px solid ${colorWithWhiteness(colors.dark, 0.5)}`,
    fontSize: '12px',
    padding: '.33rem .5rem',
    width: '200px',
  },
  deleteLink: {
    alignSelf: 'center',
    fontSize: '16px',
    textTransform: 'uppercase',
  },
  confirmWarning: {
    backgroundColor: colorWithWhiteness(colors.warning, 0.9),
    border: `1px solid ${colors.warning}`,
    borderRadius: '5px',
    display: 'grid',
    gridColumnGap: '.4rem',
    gridRowGap: '.7rem',
    fontSize: '14px',
    fontWeight: 500,
    padding: '.5rem',
    marginTop: '1rem',
    marginBottom: '1rem',
  },
  confirmWarningText: {
    color: colors.primary,
    margin: 0,
  },
  gpuCheckBox: {
    marginRight: '10px',
  },
  gpuCheckBoxRow: {
    alignItems: 'center',
    gap: '10px',
  },
  gpuSection: {
    gap: '10px',
    marginTop: '1rem',
  },
  sparkConsoleHeader: {
    color: '#333F52',
    fontSize: '14px',
    fontWeight: 600,
    margin: 0,
  },
  sparkConsoleSection: {
    backgroundColor: colors.light,
    border: '1px solid #4D72AA',
    borderRadius: '5px',
    fontSize: '14px',
    padding: '21px 17px',
  },
  sparkConsoleLaunchButton: {
    border: '1px solid #4D72AA',
    borderRadius: '2px',
    display: 'inline-block',
    marginTop: '17px',
    padding: '10px 21px',
  },
  diskRow: {
    gap: '8px',
  },
  diskRadio: {
    height: '24px',
  },
  diskLabel: {
    fontWeight: 500,
  },
});

// exported for testing
export const MIN_DISK_SIZE_GB = 100;
export const DATAPROC_WORKER_MIN_DISK_SIZE_GB = 150;

enum PanelContent {
  Create = 'Create',
  Customize = 'Customize',
  DeleteRuntime = 'DeleteRuntime',
  DeleteUnattachedPd = 'DeleteUnattachedPd',
  DeleteUnattachedPdAndCreate = 'DeleteUnattachedPdAndCreate',
  DeleteUnattachedPdAndUpdate = 'DeleteUnattachedPdAndUpdate',
  Disabled = 'Disabled',
  ConfirmUpdate = 'ConfirmUpdate',
  ConfirmUpdateWithDiskDelete = 'ConfirmUpdateWithDiskDelete',
  SparkConsole = 'SparkConsole',
}

const sparkLinkConfigs: {
  name: string;
  description: string;
  path: SparkConsolePath;
}[] = [
  {
    name: 'YARN',
    description:
      'YARN Resource Manager provides information about cluster status ' +
      'and metrics as well as information about the scheduler, nodes, and ' +
      'applications on the cluster.',
    path: SparkConsolePath.Yarn,
  },
  {
    name: 'YARN Application Timeline',
    description:
      'YARN Application Timeline provides information about current and ' +
      'historic applications executed on the cluster.',
    path: SparkConsolePath.YarnTimeline,
  },
  {
    name: 'Spark History Server',
    description:
      'Spark History Server provides information about completed Spark applications on the cluster.',
    path: SparkConsolePath.SparkHistory,
  },
  {
    name: 'MapReduce History Server',
    description:
      'MapReduce History Server displays information about completed MapReduce applications on a cluster.',
    path: SparkConsolePath.JobHistory,
  },
];

// this is only used in the test.
export interface Props {
  onClose: () => void;
}

// Exported for testing only.
export const ConfirmDelete = ({ onCancel, onConfirm }) => {
  const [deleting, setDeleting] = useState(false);
  return (
    <Fragment>
      <div style={styles.confirmWarning}>
        <div style={{ display: 'flex', justifyContent: 'center' }}>
          <ClrIcon
            style={{ color: colors.warning, gridColumn: 1, gridRow: 1 }}
            className='is-solid'
            shape='exclamation-triangle'
            size='20'
          />
        </div>
        <h3
          style={{
            ...styles.baseHeader,
            ...styles.bold,
            gridColumn: 2,
            gridRow: 1,
          }}
        >
          Delete your environment
        </h3>
        <p style={{ ...styles.confirmWarningText, gridColumn: 2, gridRow: 2 }}>
          You’re about to delete your cloud analysis environment.
        </p>
        <p style={{ ...styles.confirmWarningText, gridColumn: 2, gridRow: 3 }}>
          Any in-memory state and local file modifications will be erased. Data
          stored in workspace buckets is never affected by changes to your cloud
          environment. You’ll still be able to view notebooks in this workspace,
          but editing and running notebooks will require you to create a new
          cloud environment.
        </p>
      </div>
      <FlexRow style={{ justifyContent: 'flex-end' }}>
        <Button
          type='secondaryLight'
          aria-label={'Cancel'}
          disabled={deleting}
          style={{ marginRight: '.6rem' }}
          onClick={() => onCancel()}
        >
          Cancel
        </Button>
        <Button
          aria-label={'Delete'}
          disabled={deleting}
          onClick={async () => {
            setDeleting(true);
            try {
              await onConfirm();
            } catch (err) {
              setDeleting(false);
              throw err;
            }
          }}
        >
          Delete
        </Button>
      </FlexRow>
    </Fragment>
  );
};

const BackupFilesHelpSection = () => (
  <div>
    <div>
      To backup and share files, such as input data, analysis outputs, or
      installed packages,{' '}
      <a href={supportUrls.workspaceBucket}>
        move them to the workspace bucket.
      </a>
    </div>
    <div>
      Note: Jupyter notebooks are autosaved to the workspace bucket, and
      deleting your disk will not delete your notebooks.
    </div>
  </div>
);

export const ConfirmDeleteUnattachedPD = ({
  onConfirm,
  onCancel,
  showCreateMessaging = false,
}) => {
  const [deleting, setDeleting] = useState(false);

  return (
    <Fragment>
      <div style={{ display: 'flex', marginRight: '0.5rem' }}>
        <ClrIcon
          style={{ color: colors.warning, marginRight: '0.25rem' }}
          className='is-solid'
          shape='exclamation-triangle'
          size='20'
        />
        <h3 style={{ ...styles.baseHeader, ...styles.bold }}>
          {showCreateMessaging
            ? 'Environment creation requires deleting your unattached disk'
            : 'Delete environment options'}
        </h3>
      </div>

      <div style={styles.confirmWarning}>
        <h3
          style={{
            ...styles.baseHeader,
            ...styles.bold,
            gridColumn: 1,
            gridRow: 1,
          }}
        >
          <div
            data-test-id='delete-unattached-pd'
            style={{ display: 'inline-block', marginRight: '0.5rem' }}
          >
            <RadioButton
              data-test-id='delete-unattached-pd-radio'
              style={{ marginRight: '0.25rem' }}
              onChange={() => setDeleting(true)}
              checked={deleting === true}
            />
            <label>Delete persistent disk</label>
          </div>
        </h3>
        <p style={{ ...styles.confirmWarningText, gridColumn: 1, gridRow: 2 }}>
          Deletes your persistent disk, which will also delete all files on the
          disk.
        </p>
        <p style={{ ...styles.confirmWarningText, gridColumn: 1, gridRow: 3 }}>
          If you want to permanently save some files from the disk before
          deleting it, you will need to create a new cloud environment to access
          it.{' '}
        </p>
      </div>
      <BackupFilesHelpSection />
      <FlexRow style={{ justifyContent: 'flex-end' }}>
        <Button
          type='secondaryLight'
          aria-label={'Cancel'}
          style={{ marginRight: '.6rem' }}
          onClick={() => onCancel()}
        >
          Cancel
        </Button>
        <Button
          aria-label={'Delete'}
          disabled={!deleting}
          onClick={async () => {
            setDeleting(true);
            try {
              await onConfirm();
            } catch (err) {
              setDeleting(false);
              throw err;
            }
          }}
        >
          {showCreateMessaging ? 'Delete and recreate' : 'Delete'}
        </Button>
      </FlexRow>
    </Fragment>
  );
};

export const ConfirmDeleteRuntimeWithPD = ({
  onCancel,
  onConfirm,
  computeType,
  disk,
}) => {
  const [deleting, setDeleting] = useState(false);
  const [runtimeStatusReq, setRuntimeStatusReq] = useState(
    RuntimeStatusRequest.DeleteRuntime
  );
  const standardvmDeleteOption = (
    <div>
      <div style={styles.confirmWarning}>
        <h3
          style={{
            ...styles.baseHeader,
            ...styles.bold,
            gridColumn: 1,
            gridRow: 1,
          }}
        >
          <div
            data-test-id='delete-runtime'
            style={{ display: 'inline-block', marginRight: '0.5rem' }}
          >
            <RadioButton
              style={{ marginRight: '0.25rem' }}
              onChange={() =>
                setRuntimeStatusReq(RuntimeStatusRequest.DeleteRuntime)
              }
              checked={runtimeStatusReq === RuntimeStatusRequest.DeleteRuntime}
            />
            <label>Keep persistent disk, delete environment</label>
          </div>
        </h3>
        <p style={{ ...styles.confirmWarningText, gridColumn: 1, gridRow: 2 }}>
          Please save your analysis data in the directory
          /home/jupyter/notebooks to ensure it’s stored on your disk.
        </p>
        <p style={{ ...styles.confirmWarningText, gridColumn: 1, gridRow: 3 }}>
          Deletes your analysis environment, but detaches your persistent disk
          and saves it for later. The disk will be automatically reattached the
          next time you create a cloud environment using the standard VM compute
          type within this workspace.
        </p>
        <p style={{ ...styles.confirmWarningText, gridColumn: 1, gridRow: 4 }}>
          You will continue to incur persistent disk cost at{' '}
          <b>{formatUsd(detachableDiskPricePerMonth(disk))}</b> per month. You
          can delete your disk at any time via the runtime panel.
        </p>
      </div>
      <div style={styles.confirmWarning}>
        <h3
          style={{
            ...styles.baseHeader,
            ...styles.bold,
            gridColumn: 1,
            gridRow: 1,
          }}
        >
          <div
            data-test-id='delete-runtime-and-pd'
            style={{ display: 'inline-block', marginRight: '0.5rem' }}
          >
            <RadioButton
              style={{ marginRight: '0.25rem' }}
              onChange={() =>
                setRuntimeStatusReq(RuntimeStatusRequest.DeleteRuntimeAndPD)
              }
              checked={
                runtimeStatusReq === RuntimeStatusRequest.DeleteRuntimeAndPD
              }
            />
            <label>Delete persistent disk and environment</label>
          </div>
        </h3>
        <p style={{ ...styles.confirmWarningText, gridColumn: 1, gridRow: 2 }}>
          Deletes your persistent disk, which will also delete all files on the
          disk. Also deletes your analysis environment.
        </p>
      </div>
    </div>
  );
  const dataprocDeleteOption = (
    <div>
      <div style={styles.confirmWarning}>
        <h3
          style={{
            ...styles.baseHeader,
            ...styles.bold,
            gridColumn: 1,
            gridRow: 1,
          }}
        >
          <div
            data-test-id='delete-runtime'
            style={{ display: 'inline-block', marginRight: '0.5rem' }}
          >
            <RadioButton
              style={{ marginRight: '0.25rem' }}
              onChange={() =>
                setRuntimeStatusReq(RuntimeStatusRequest.DeleteRuntime)
              }
              checked={runtimeStatusReq === RuntimeStatusRequest.DeleteRuntime}
            />
            <label>
              Delete application configuration and cloud compute profile
            </label>
          </div>
        </h3>
        <p style={{ ...styles.confirmWarningText, gridColumn: 1, gridRow: 2 }}>
          You’re about to delete your cloud analysis environment.
        </p>
        <p style={{ ...styles.confirmWarningText, gridColumn: 1, gridRow: 3 }}>
          Deletes your application configuration and cloud compute profile. This
          will also delete all files on the built-in hard disk.
        </p>
      </div>
      <div style={styles.confirmWarning}>
        <h3
          style={{
            ...styles.baseHeader,
            ...styles.bold,
            gridColumn: 1,
            gridRow: 1,
          }}
        >
          <div
            data-test-id='delete-unattached-pd'
            style={{ display: 'inline-block', marginRight: '0.5rem' }}
          >
            <RadioButton
              style={{ marginRight: '0.25rem' }}
              onChange={() =>
                setRuntimeStatusReq(RuntimeStatusRequest.DeletePD)
              }
              checked={runtimeStatusReq === RuntimeStatusRequest.DeletePD}
            />
            <label>Delete unattached persistent disk</label>
          </div>
        </h3>
        <p style={{ ...styles.confirmWarningText, gridColumn: 1, gridRow: 2 }}>
          Deletes your unattached persistent disk, which will also delete all
          files on the disk.
        </p>
        <p style={{ ...styles.confirmWarningText, gridColumn: 1, gridRow: 3 }}>
          Since the persistent disk is not attached, the application
          configuration and cloud compute profile will remain.
        </p>
        <p style={{ ...styles.confirmWarningText, gridColumn: 1, gridRow: 4 }}>
          You will continue to incur persistent disk cost at{' '}
          <b>{formatUsd(detachableDiskPricePerMonth(disk))}</b> per month.
        </p>
      </div>
    </div>
  );
  return (
    <Fragment>
      <div style={{ display: 'flex', marginRight: '0.5rem' }}>
        <ClrIcon
          style={{ color: colors.warning, marginRight: '0.25rem' }}
          className='is-solid'
          shape='exclamation-triangle'
          size='20'
        />
        <h3 style={{ ...styles.baseHeader, ...styles.bold }}>
          Delete environment options
        </h3>
      </div>
      {computeType === ComputeType.Standard
        ? standardvmDeleteOption
        : dataprocDeleteOption}
      <BackupFilesHelpSection />
      <FlexRow style={{ justifyContent: 'flex-end' }}>
        <Button
          type='secondaryLight'
          aria-label={'Cancel'}
          disabled={deleting}
          style={{ marginRight: '.6rem' }}
          onClick={() => onCancel()}
        >
          Cancel
        </Button>
        <Button
          aria-label={'Delete'}
          disabled={deleting}
          onClick={async () => {
            setDeleting(true);
            try {
              await onConfirm(runtimeStatusReq);
            } catch (err) {
              setDeleting(false);
              throw err;
            }
          }}
        >
          Delete
        </Button>
      </FlexRow>
    </Fragment>
  );
};

const OfferDeleteDiskWithUpdate = ({
  onNext,
  onCancel,
  disk,
}: {
  onNext: (deleteDetachedDisk: boolean) => void;
  onCancel: () => void;
  disk: Disk;
}) => {
  const [deleteDetachedDisk, setDeleteDetachedDisk] = useState(false);
  return (
    <Fragment>
      <div style={{ display: 'flex', marginRight: '0.5rem' }}>
        <ClrIcon
          style={{ color: colors.warning, marginRight: '0.25rem' }}
          className='is-solid'
          shape='exclamation-triangle'
          size='20'
        />
        <h3 style={{ ...styles.baseHeader, ...styles.bold }}>Disk options</h3>
      </div>
      <div>
        <div>
          Your environment currently has a reattachable disk, which will be
          unused after you apply this update. What would you like to do with it?
        </div>
        <div style={styles.confirmWarning}>
          <h3
            style={{
              ...styles.baseHeader,
              ...styles.bold,
              gridColumn: 1,
              gridRow: 1,
            }}
          >
            <div
              data-test-id='keep-pd'
              style={{ display: 'inline-block', marginRight: '0.5rem' }}
            >
              <RadioButton
                style={{ marginRight: '0.25rem' }}
                onChange={() => setDeleteDetachedDisk(false)}
                checked={!deleteDetachedDisk}
              />
              <label>Keep unattached persistent disk</label>
            </div>
          </h3>
          <p
            style={{ ...styles.confirmWarningText, gridColumn: 1, gridRow: 2 }}
          >
            Your disk will be saved for later and can be reattached when you
            next configure a standard VM analysis environment. You will continue
            to incur persistent disk cost at{' '}
            <b>{formatUsd(detachableDiskPricePerMonth(disk))}</b> per month.
          </p>
        </div>
        <div style={styles.confirmWarning}>
          <h3
            style={{
              ...styles.baseHeader,
              ...styles.bold,
              gridColumn: 1,
              gridRow: 1,
            }}
          >
            <div
              data-test-id='delete-pd'
              style={{ display: 'inline-block', marginRight: '0.5rem' }}
            >
              <RadioButton
                style={{ marginRight: '0.25rem' }}
                onChange={() => setDeleteDetachedDisk(true)}
                checked={deleteDetachedDisk}
              />
              <label>Delete persistent disk</label>
            </div>
          </h3>
          <p
            style={{ ...styles.confirmWarningText, gridColumn: 1, gridRow: 2 }}
          >
            Delete your persistent disk, which will also delete all files on the
            disk.
          </p>
        </div>
      </div>
      <BackupFilesHelpSection />
      <FlexRow style={{ justifyContent: 'flex-end' }}>
        <Button
          type='secondaryLight'
          aria-label={'Cancel'}
          style={{ marginRight: '.6rem' }}
          onClick={() => onCancel()}
        >
          Cancel
        </Button>
        <Button aria-label={'Next'} onClick={() => onNext(deleteDetachedDisk)}>
          Next
        </Button>
      </FlexRow>
    </Fragment>
  );
};

const SparkConsolePanel = ({ namespace, id }: WorkspaceData) => {
  return (
    <FlexColumn style={{ gap: '24px', paddingBottom: '10px' }}>
      <h3 style={{ ...styles.baseHeader, ...styles.bold }}>Spark Console</h3>
      The spark console is used to manage and monitor cluster resources and
      facilities, such as the YARN resource manager, the Hadoop Distributed File
      System (HDFS), MapReduce, and Spark.
      {sparkLinkConfigs.map(({ name, description, path }) => (
        <FlexColumn key={name} style={styles.sparkConsoleSection}>
          <h4 style={styles.sparkConsoleHeader}>{name}</h4>
          <div>{description}</div>
          <div>
            <RouteLink
              path={`/workspaces/${namespace}/${id}/spark/${path}`}
              style={styles.sparkConsoleLaunchButton}
            >
              Launch
            </RouteLink>
          </div>
        </FlexColumn>
      ))}
    </FlexColumn>
  );
};

const MachineSelector = ({
  onChange,
  selectedMachine,
  machineType,
  disabled,
  idPrefix,
  validMachineTypes,
  cpuLabelStyles = {},
  ramLabelStyles = {},
}) => {
  const initialMachineType =
    findMachineByName(machineType) || DEFAULT_MACHINE_TYPE;
  const { cpu, memory } = selectedMachine || initialMachineType;

  return (
    <Fragment>
      <FlexRow style={styles.labelAndInput}>
        <label
          style={{ ...styles.label, ...cpuLabelStyles }}
          htmlFor={`${idPrefix}-cpu`}
        >
          CPUs
        </label>
        <Dropdown
          id={`${idPrefix}-cpu`}
          options={fp.flow(
            // Show all CPU options.
            fp.map('cpu'),
            // In the event that was remove a machine type from our set of valid
            // configs, we want to continue to allow rendering of the value here.
            // Union also makes the CPU values unique.
            fp.union([cpu]),
            fp.sortBy(fp.identity)
          )(validMachineTypes)}
          onChange={({ value }) =>
            fp.flow(
              fp.sortBy('memory'),
              fp.find({ cpu: value }),
              onChange
            )(validMachineTypes)
          }
          disabled={disabled}
          value={cpu}
        />
      </FlexRow>
      <FlexRow style={styles.labelAndInput}>
        <label
          style={{ ...styles.label, ...ramLabelStyles }}
          htmlFor={`${idPrefix}-ram`}
        >
          RAM (GB)
        </label>
        <Dropdown
          id={`${idPrefix}-ram`}
          options={fp.flow(
            // Show valid memory options as constrained by the currently selected CPU.
            fp.filter(({ cpu: availableCpu }) => availableCpu === cpu),
            fp.map('memory'),
            // See above comment on CPU union.
            fp.union([memory]),
            fp.sortBy(fp.identity)
          )(validMachineTypes)}
          onChange={({ value }) =>
            fp.flow(
              fp.find({ cpu, memory: value }),
              // If the selected machine is not different from the current machine return null
              // maybeGetMachine,
              onChange
            )(validMachineTypes)
          }
          disabled={disabled}
          value={memory}
        />
      </FlexRow>
    </Fragment>
  );
};

const DisabledPanel = () => {
  return (
    <WarningMessage
      data-test-id='runtime-disabled-panel'
      iconSize={16}
      iconPosition={'top'}
    >
      {
        <TextColumn>
          <div style={{ fontWeight: 600 }}>
            Cloud services are disabled for this workspace.
          </div>
          <div style={{ marginTop: '0.5rem' }}>
            You cannot run or edit notebooks in this workspace because billed
            services are disabled for the workspace creator's <AoU /> Researcher
            account.
          </div>
        </TextColumn>
      }
    </WarningMessage>
  );
};

const DiskSizeSelector = ({
  onChange,
  disabled,
  diskSize,
  idPrefix,
  style = {},
}) => {
  return (
    <FlexRow style={{ ...styles.labelAndInput, ...style }}>
      <label style={styles.label} htmlFor={`${idPrefix}-disk-size`}>
        Disk (GB)
      </label>
      <InputNumber
        id={`${idPrefix}-disk`}
        showButtons
        disabled={disabled}
        decrementButtonClassName='p-button-secondary'
        incrementButtonClassName='p-button-secondary'
        value={diskSize}
        inputStyle={styles.inputNumber}
        onChange={({ value }) => onChange(value)}
      />
    </FlexRow>
  );
};

const DiskSelector = ({
  diskConfig,
  onChange,
  disabled,
  disableDetachableReason,
  existingDisk,
}: {
  diskConfig: DiskConfig;
  onChange: (c: DiskConfig) => void;
  disabled: boolean;
  disableDetachableReason: string | null;
  existingDisk: Disk | null;
}) => {
  return (
    <FlexColumn
      style={{ ...styles.controlSection, gap: '11px', marginTop: '11px' }}
    >
      <FlexRow style={{ gap: '8px' }}>
        <span style={{ ...styles.sectionTitle, marginBottom: 0 }}>
          Storage disk options
        </span>
        <StyledExternalLink href='https://support.terra.bio/hc/en-us/articles/360047318551'>
          View documentation
        </StyledExternalLink>
      </FlexRow>
      <FlexRow style={styles.diskRow}>
        <RadioButton
          name='standardDisk'
          style={styles.diskRadio}
          disabled={disabled}
          onChange={() =>
            onChange({
              ...diskConfig,
              detachable: false,
              detachableType: null,
              existingDiskName: null,
            })
          }
          checked={!diskConfig.detachable}
        />
        <FlexColumn>
          <label style={styles.diskLabel}>Standard disk</label>
          <span>
            A standard disk is created and deleted with your cloud environment.
          </span>
          {diskConfig.detachable || (
            <DiskSizeSelector
              idPrefix='standard'
              diskSize={diskConfig.size}
              disabled={disabled}
              style={{ marginTop: '11px' }}
              onChange={(size: number) =>
                onChange(
                  maybeWithExistingDiskName(
                    {
                      ...diskConfig,
                      size,
                    },
                    existingDisk
                  )
                )
              }
            />
          )}
        </FlexColumn>
      </FlexRow>
      <TooltipTrigger
        content={disableDetachableReason}
        disabled={!disableDetachableReason}
      >
        <FlexRow style={styles.diskRow}>
          <RadioButton
            name='detachableDisk'
            style={styles.diskRadio}
            onChange={() =>
              onChange(
                maybeWithExistingDiskName(
                  {
                    ...diskConfig,
                    size: existingDisk?.size || diskConfig.size,
                    detachable: true,
                    detachableType: existingDisk?.diskType || DiskType.Standard,
                  },
                  existingDisk
                )
              )
            }
            checked={diskConfig.detachable}
            disabled={disabled || !!disableDetachableReason}
          />
          <FlexColumn>
            <label style={styles.diskLabel}>Reattachable persistent disk</label>
            <span>
              A reattachable disk is saved even when your compute environment is
              deleted.
            </span>
            {diskConfig.detachable && (
              <FlexRow style={{ ...styles.formGrid2, marginTop: '11px' }}>
                <FlexRow style={styles.labelAndInput}>
                  <label
                    style={{ ...styles.label, minWidth: '3.0rem' }}
                    htmlFor='disk-type'
                  >
                    Disk type
                  </label>
                  <Dropdown
                    id={'disk-type'}
                    options={[DiskType.Standard, DiskType.Ssd].map((value) => ({
                      label: diskTypeLabels[value],
                      value,
                    }))}
                    style={{ width: '150px' }}
                    disabled={disabled}
                    onChange={({ value }) =>
                      onChange(
                        maybeWithExistingDiskName(
                          {
                            ...diskConfig,
                            detachableType: value,
                          },
                          existingDisk
                        )
                      )
                    }
                    value={diskConfig.detachableType}
                  />
                </FlexRow>
                <DiskSizeSelector
                  idPrefix='detachable'
                  diskSize={diskConfig.size}
                  disabled={disabled}
                  onChange={(size: number) =>
                    onChange(
                      maybeWithExistingDiskName(
                        {
                          ...diskConfig,
                          size,
                        },
                        existingDisk
                      )
                    )
                  }
                />
              </FlexRow>
            )}
          </FlexColumn>
        </FlexRow>
      </TooltipTrigger>
    </FlexColumn>
  );
};

const GpuConfigSelector = ({
  disabled,
  onChange,
  selectedMachine,
  gpuConfig,
}) => {
  const { gpuType = 'nvidia-tesla-t4', numOfGpus = 1 } = gpuConfig || {};
  const [selectedGpuType, setSelectedGpuType] = useState<string>(gpuType);
  const [selectedNumOfGpus, setSelectedNumOfGpus] = useState<number>(numOfGpus);
  const [hasGpu, setHasGpu] = useState<boolean>(!!gpuConfig);
  const validGpuOptions = getValidGpuTypes(
    selectedMachine.cpu,
    selectedMachine.memory
  );
  const validGpuNames = fp.flow(
    fp.map('name'),
    fp.uniq,
    fp.sortBy('price')
  )(validGpuOptions);
  const validNumGpusOptions = fp.flow(
    fp.filter({ type: selectedGpuType }),
    fp.map('numGpus')
  )(validGpuOptions);

  useEffect(() => {
    onChange(
      hasGpu && validGpuOptions.length > 0
        ? {
            gpuType: selectedGpuType,
            numOfGpus: selectedNumOfGpus,
          }
        : null
    );
  }, [hasGpu, selectedGpuType, selectedNumOfGpus]);

  return (
    <FlexColumn style={styles.gpuSection}>
      <FlexRow style={styles.gpuCheckBoxRow}>
        <CheckBox
          id={'enable-gpu'}
          label='Enable GPUs'
          checked={hasGpu}
          style={styles.gpuCheckBox}
          disabled={disabled}
          onChange={() => {
            setHasGpu(!hasGpu);
          }}
        />
        <a
          target='_blank'
          href='https://support.terra.bio/hc/en-us/articles/4403006001947'
        >
          Learn more about GPU cost and restrictions.
        </a>
      </FlexRow>
      {hasGpu && (
        <FlexRow style={styles.formGrid2}>
          <FlexRow style={styles.labelAndInput}>
            <label
              style={{ ...styles.label, minWidth: '3.0rem' }}
              htmlFor='gpu-type'
            >
              Gpu Type
            </label>
            <Dropdown
              id={'gpu-type'}
              style={{ width: '7rem' }}
              options={validGpuNames}
              onChange={({ value }) => {
                setSelectedGpuType(
                  fp.find({ name: value }, validGpuOptions).type
                );
              }}
              disabled={disabled}
              value={gpuTypeToDisplayName(selectedGpuType)}
            />
          </FlexRow>
          <FlexRow style={styles.labelAndInput}>
            <label
              style={{ ...styles.label, minWidth: '2.0rem' }}
              htmlFor='gpu-num'
            >
              GPUs
            </label>
            <Dropdown
              id={'gpu-num'}
              options={validNumGpusOptions}
              onChange={({ value }) => setSelectedNumOfGpus(value)}
              disabled={disabled}
              value={selectedNumOfGpus}
            />
          </FlexRow>
        </FlexRow>
      )}
    </FlexColumn>
  );
};

const DataProcConfigSelector = ({
  onChange,
  disabled,
  runtimeStatus,
  dataprocExists,
  dataprocConfig,
}) => {
  const {
    workerMachineType = DEFAULT_MACHINE_NAME,
    workerDiskSize = DATAPROC_WORKER_MIN_DISK_SIZE_GB,
    numberOfWorkers = 2,
    numberOfPreemptibleWorkers = 0,
  } = dataprocConfig || {};
  const initialMachine = findMachineByName(workerMachineType);
  const [selectedNumWorkers, setSelectedNumWorkers] =
    useState<number>(numberOfWorkers);
  const [selectedPreemtible, setSelectedPreemptible] = useState<number>(
    numberOfPreemptibleWorkers
  );
  const [selectedWorkerMachine, setSelectedWorkerMachine] =
    useState<Machine>(initialMachine);
  const [selectedDiskSize, setSelectedDiskSize] =
    useState<number>(workerDiskSize);

  // If the dataprocConfig prop changes externally, reset the selectors accordingly.
  useEffect(() => {
    setSelectedNumWorkers(numberOfWorkers);
    setSelectedPreemptible(numberOfPreemptibleWorkers);
    setSelectedWorkerMachine(initialMachine);
    setSelectedDiskSize(workerDiskSize);
  }, [dataprocConfig]);

  useEffect(() => {
    onChange({
      ...dataprocConfig,
      workerMachineType: selectedWorkerMachine?.name,
      workerDiskSize: selectedDiskSize,
      numberOfWorkers: selectedNumWorkers,
      numberOfPreemptibleWorkers: selectedPreemtible,
    });
  }, [
    selectedNumWorkers,
    selectedPreemtible,
    selectedWorkerMachine,
    selectedDiskSize,
  ]);

  // As a special case in Dataproc, worker counts can be dynamically changed on
  // a running cluster but not on a stopped cluster. Rather than building a
  // one-off resume->update workflow into Workbench, just disable the control
  // and let the user resume themselves.
  const workerCountDisabledByStopped =
    dataprocExists && runtimeStatus === RuntimeStatus.Stopped;
  const workerCountTooltip = workerCountDisabledByStopped
    ? 'Cannot update worker counts on a stopped Dataproc environment, please start your environment first.'
    : undefined;

  return (
    <fieldset style={{ marginTop: '0.75rem' }}>
      <legend style={styles.sectionTitle}>Worker Configuration</legend>
      <div style={styles.formGrid3}>
        <FlexRow style={styles.labelAndInput}>
          <label style={styles.label} htmlFor='num-workers'>
            Workers
          </label>
          <InputNumber
            id='num-workers'
            showButtons
            disabled={disabled || workerCountDisabledByStopped}
            decrementButtonClassName='p-button-secondary'
            incrementButtonClassName='p-button-secondary'
            value={selectedNumWorkers}
            inputStyle={styles.inputNumber}
            tooltip={workerCountTooltip}
            onChange={({ value }) => setSelectedNumWorkers(value)}
          />
        </FlexRow>
        <FlexRow style={styles.labelAndInput}>
          <label style={styles.label} htmlFor='num-preemptible'>
            Preemptible
          </label>
          <InputNumber
            id='num-preemptible'
            showButtons
            disabled={disabled || workerCountDisabledByStopped}
            decrementButtonClassName='p-button-secondary'
            incrementButtonClassName='p-button-secondary'
            value={selectedPreemtible}
            inputStyle={styles.inputNumber}
            tooltip={workerCountTooltip}
            onChange={({ value }) => setSelectedPreemptible(value)}
          />
        </FlexRow>
        <div style={{ gridColumnEnd: 'span 1' }} />
        <MachineSelector
          machineType={workerMachineType}
          onChange={setSelectedWorkerMachine}
          selectedMachine={selectedWorkerMachine}
          disabled={disabled}
          validMachineTypes={validLeoDataprocWorkerMachineTypes}
          idPrefix='worker'
          cpuLabelStyles={{ minWidth: '2.5rem' }} // width of 'Workers' label above
          ramLabelStyles={{ minWidth: '3.75rem' }} // width of 'Preemptible' label above
        />
        <DiskSizeSelector
          diskSize={workerDiskSize}
          onChange={setSelectedDiskSize}
          disabled={disabled}
          idPrefix='worker'
        />
      </div>
    </fieldset>
  );
};

// Select a recommended preset configuration.
const PresetSelector = ({
  allowDataproc,
  setAnalysisConfig,
  disabled,
  persistentDisk,
}) => {
  return (
    <Dropdown
      id='runtime-presets-menu'
      disabled={disabled}
      style={{
        marginTop: '21px',
        display: 'inline-block',
        color: colors.primary,
      }}
      placeholder='Recommended environments'
      options={fp.flow(
        fp.values,
        fp.filter(
          ({ runtimeTemplate }) =>
            allowDataproc || !runtimeTemplate.dataprocConfig
        ),
        fp.map(({ displayName, runtimeTemplate }) => ({
          label: displayName,
          value: runtimeTemplate,
        }))
      )(runtimePresets)}
      onChange={({ value }) => {
        setAnalysisConfig(toAnalysisConfig(value, persistentDisk));

        // Return false to skip the normal handling of the value selection. We're
        // abusing the dropdown here to act as if it were a menu instead.
        // Therefore, we never want the empty "placeholder" text to change to a
        // selected value (it should always read "recommended environments"). The presets
        // are not persistent state, they just snap the rest of the form to a particular configuration.
        // See RW-5996 for more details.
        return false;
      }}
    />
  );
};

const StartStopRuntimeButton = ({ workspaceNamespace, googleProject }) => {
  const [status, setRuntimeStatus] = useRuntimeStatus(
    workspaceNamespace,
    googleProject
  );

  const rotateStyle = { animation: 'rotation 2s infinite linear' };
  const {
    altText,
    iconSrc,
    dataTestId,
    styleOverrides = {},
    onClick = null,
  } = switchCase(
    status,
    [
      RuntimeStatus.Creating,
      () => ({
        altText: 'Runtime creation in progress',
        iconSrc: computeStarting,
        dataTestId: 'runtime-status-icon-starting',
        styleOverrides: rotateStyle,
      }),
    ],
    [
      RuntimeStatus.Running,
      () => ({
        altText: 'Runtime running, click to pause',
        iconSrc: computeRunning,
        dataTestId: 'runtime-status-icon-running',
        onClick: () => {
          setRuntimeStatus(RuntimeStatusRequest.Stop);
        },
      }),
    ],
    [
      RuntimeStatus.Updating,
      () => ({
        altText: 'Runtime update in progress',
        iconSrc: computeStarting,
        dataTestId: 'runtime-status-icon-starting',
        styleOverrides: rotateStyle,
      }),
    ],
    [
      RuntimeStatus.Error,
      () => ({
        altText: 'Runtime in error state',
        iconSrc: computeError,
        dataTestId: 'runtime-status-icon-error',
      }),
    ],
    [
      RuntimeStatus.Stopping,
      () => ({
        altText: 'Runtime pause in progress',
        iconSrc: computeStopping,
        dataTestId: 'runtime-status-icon-stopping',
        styleOverrides: rotateStyle,
      }),
    ],
    [
      RuntimeStatus.Stopped,
      () => ({
        altText: 'Runtime paused, click to resume',
        iconSrc: computeStopped,
        dataTestId: 'runtime-status-icon-stopped',
        onClick: () => {
          setRuntimeStatus(RuntimeStatusRequest.Start);
        },
      }),
    ],
    [
      RuntimeStatus.Starting,
      () => ({
        altText: 'Runtime resume in progress',
        iconSrc: computeStarting,
        dataTestId: 'runtime-status-icon-starting',
        styleOverrides: rotateStyle,
      }),
    ],
    [
      RuntimeStatus.Deleting,
      () => ({
        altText: 'Runtime deletion in progress',
        iconSrc: computeStopping,
        dataTestId: 'runtime-status-icon-stopping',
        styleOverrides: rotateStyle,
      }),
    ],
    [
      RuntimeStatus.Deleted,
      () => ({
        altText: 'Runtime has been deleted',
        iconSrc: computeNone,
        dataTestId: 'runtime-status-icon-none',
      }),
    ],
    [
      RuntimeStatus.Unknown,
      () => ({
        altText: 'Runtime status unknown',
        iconSrc: computeNone,
        dataTestId: 'runtime-status-icon-none',
      }),
    ],
    [
      DEFAULT,
      () => ({
        altText: 'No runtime found',
        iconSrc: computeNone,
        dataTestId: 'runtime-status-icon-none',
      }),
    ]
  );

  {
    /* height/width of the icon wrapper are set so that the img element can rotate inside it */
  }
  {
    /* without making it larger. the svg is 36 x 36 px, per pythagorean theorem the diagonal */
  }
  {
    /* is 50.9px, so we round up */
  }
  const iconWrapperStyle = {
    height: '51px',
    width: '51px',
    justifyContent: 'space-around',
    alignItems: 'center',
  };

  return (
    <FlexRow
      style={{
        backgroundColor: addOpacity(colors.primary, 0.1),
        justifyContent: 'space-around',
        alignItems: 'center',
        padding: '0 1rem',
        borderRadius: '5px 0 0 5px',
      }}
    >
      {/* TooltipTrigger inside the conditionals because it doesn't handle fragments well. */}
      {onClick && (
        <TooltipTrigger content={<div>{altText}</div>} side='left'>
          <FlexRow style={iconWrapperStyle}>
            <Clickable onClick={() => onClick()}>
              <img
                alt={altText}
                src={iconSrc}
                style={styleOverrides}
                data-test-id={dataTestId}
              />
            </Clickable>
          </FlexRow>
        </TooltipTrigger>
      )}
      {!onClick && (
        <TooltipTrigger content={<div>{altText}</div>} side='left'>
          <FlexRow style={iconWrapperStyle}>
            <img
              alt={altText}
              src={iconSrc}
              style={styleOverrides}
              data-test-id={dataTestId}
            />
          </FlexRow>
        </TooltipTrigger>
      )}
    </FlexRow>
  );
};

const CostInfo = ({
  runtimeChanged,
  analysisConfig,
  currentUser,
  workspace,
  creatorFreeCreditsRemaining,
}) => {
  const remainingCredits =
    creatorFreeCreditsRemaining === null ? (
      <Spinner size={10} />
    ) : (
      formatUsd(creatorFreeCreditsRemaining)
    );

  return (
    <FlexRow data-test-id='cost-estimator'>
      <div
        style={{
          padding: '.33rem .5rem',
          ...(runtimeChanged
            ? {
                backgroundColor: colorWithWhiteness(colors.warning, 0.9),
              }
            : {}),
        }}
      >
        <RuntimeCostEstimator {...{ analysisConfig }} />
      </div>
      {isUsingFreeTierBillingAccount(workspace) &&
        currentUser === workspace.creator && (
          <div style={styles.costsDrawnFrom}>
            Costs will draw from your remaining {remainingCredits} of free
            credits.
          </div>
        )}
      {isUsingFreeTierBillingAccount(workspace) &&
        currentUser !== workspace.creator && (
          <div style={styles.costsDrawnFrom}>
            Costs will draw from workspace creator's remaining{' '}
            {remainingCredits} of free credits.
          </div>
        )}
      {!isUsingFreeTierBillingAccount(workspace) && (
        <div style={styles.costsDrawnFrom}>
          Costs will be charged to billing account{' '}
          {workspace.billingAccountName}.
        </div>
      )}
    </FlexRow>
  );
};

const CreatePanel = ({
  creatorFreeCreditsRemaining,
  profile,
  setPanelContent,
  workspace,
  analysisConfig,
}) => {
  const displayName =
    analysisConfig.computeType === ComputeType.Dataproc
      ? runtimePresets.hailAnalysis.displayName
      : runtimePresets.generalAnalysis.displayName;

  return (
    <div data-test-id='runtime-create-panel' style={styles.controlSection}>
      <FlexRow style={styles.costPredictorWrapper}>
        <StartStopRuntimeButton
          workspaceNamespace={workspace.namespace}
          googleProject={workspace.googleProject}
        />
        <CostInfo
          runtimeChanged={false}
          analysisConfig={analysisConfig}
          currentUser={profile.username}
          workspace={workspace}
          creatorFreeCreditsRemaining={creatorFreeCreditsRemaining}
        />
      </FlexRow>
      <FlexRow
        style={{ justifyContent: 'space-between', alignItems: 'center' }}
      >
        <h3 style={{ ...styles.sectionHeader, ...styles.bold }}>
          Recommended Environment for {displayName}
        </h3>
        <Button
          type='secondarySmall'
          onClick={() => setPanelContent(PanelContent.Customize)}
          aria-label='Customize'
        >
          Customize
        </Button>
      </FlexRow>
      <RuntimeSummary analysisConfig={analysisConfig} />
    </div>
  );
};

const ConfirmUpdatePanel = ({
  existingAnalysisConfig,
  newAnalysisConfig,
  onCancel,
  updateButton,
}) => {
  const configDiffs = getAnalysisConfigDiffs(
    existingAnalysisConfig,
    newAnalysisConfig
  );
  const updateMessaging = diffsToUpdateMessaging(configDiffs);
  return (
    <React.Fragment>
      <div style={styles.controlSection}>
        <h3
          style={{
            ...styles.baseHeader,
            ...styles.sectionHeader,
            marginTop: '.1rem',
            marginBottom: '.2rem',
          }}
        >
          Editing your environment
        </h3>
        <div>
          You're about to apply the following changes to your environment:
        </div>
        <ul>
          {configDiffs.map((diff, i) => (
            <li key={i}>
              {diff.desc} from <b>{diff.previous}</b> to <b>{diff.new}</b>
            </li>
          ))}
        </ul>
        <FlexColumn style={{ gap: '8px', marginTop: '.5rem' }}>
          <div>
            <b style={{ fontSize: 10 }}>New estimated cost</b>
            <div
              style={{
                ...styles.costPredictorWrapper,
                ...styles.costComparison,
              }}
            >
              <RuntimeCostEstimator analysisConfig={newAnalysisConfig} />
            </div>
          </div>
          <div>
            <b style={{ fontSize: 10 }}>Previous estimated cost</b>
            <div
              style={{
                ...styles.costPredictorWrapper,
                ...styles.costComparison,
                color: 'grey',
                backgroundColor: '',
              }}
            >
              <RuntimeCostEstimator
                analysisConfig={existingAnalysisConfig}
                costTextColor='grey'
              />
            </div>
          </div>
        </FlexColumn>
      </div>
      {updateMessaging.warn && (
        <WarningMessage iconSize={30} iconPosition={'center'}>
          <TextColumn>
            <React.Fragment>
              <div>{updateMessaging.warn}</div>
              <div style={{ marginTop: '0.5rem' }}>
                {updateMessaging.warnMore}
              </div>
            </React.Fragment>
          </TextColumn>
        </WarningMessage>
      )}
      <FlexRow style={{ justifyContent: 'flex-end', marginTop: '.75rem' }}>
        <Button
          type='secondary'
          aria-label='Cancel'
          style={{ marginRight: '.25rem' }}
          onClick={onCancel}
        >
          Cancel
        </Button>
        {updateButton}
      </FlexRow>
    </React.Fragment>
  );
};

const RuntimePanel = fp.flow(
  withCdrVersions(),
  withCurrentWorkspace(),
  withUserProfile()
)(
  ({
    cdrVersionTiersResponse,
    workspace,
    profileState,
    onClose = () => {},
  }) => {
    const { namespace, id, cdrVersionId, googleProject } = workspace;

    const { profile } = profileState;

    const { hasWgsData: allowDataproc } = findCdrVersion(
      cdrVersionId,
      cdrVersionTiersResponse
    ) || { hasWgsData: false };
    const { persistentDisk } = useStore(diskStore);
    let [{ currentRuntime, pendingRuntime }, setRuntimeRequest] =
      useCustomRuntime(namespace, persistentDisk);
    // If the runtime has been deleted, it's possible that the default preset values have changed since its creation
    if (currentRuntime && currentRuntime.status === RuntimeStatus.Deleted) {
      currentRuntime = applyPresetOverride(currentRuntime);
    }

    const [status, setRuntimeStatus] = useRuntimeStatus(
      namespace,
      googleProject
    );

    // Prioritize the "pendingRuntime", if any. When an update is pending, we want
    // to render the target runtime details, which  may not match the current runtime.
    const existingRuntime =
      pendingRuntime || currentRuntime || ({} as Partial<Runtime>);
    const existingAnalysisConfig = toAnalysisConfig(
      existingRuntime,
      persistentDisk
    );

    const [analysisConfig, setAnalysisConfig] = useState(
      withAnalysisConfigDefaults(existingAnalysisConfig, persistentDisk)
    );
    const requestAnalysisConfig = (config: AnalysisConfig) =>
      setRuntimeRequest({
        runtime: fromAnalysisConfig(config),
        detachedDisk: config.detachedDisk,
      });

    const { enableGpu, enablePersistentDisk } = serverConfigStore.get().config;

    const initialPanelContent = fp.cond([
      [([b, ,]) => b === BillingStatus.INACTIVE, () => PanelContent.Disabled],
      // If there's a pendingRuntime, this means there's already a create/update
      // in progress, even if the runtime store doesn't actively reflect this yet.
      // Show the customize panel in this event.
      [() => !!pendingRuntime, () => PanelContent.Customize],
      [
        ([, r, s]) =>
          r === null || r === undefined || s === RuntimeStatus.Unknown,
        () => PanelContent.Create,
      ],
      [
        ([, r]) =>
          r.status === RuntimeStatus.Deleted &&
          [
            RuntimeConfigurationType.GeneralAnalysis,
            RuntimeConfigurationType.HailGenomicAnalysis,
          ].includes(r.configurationType),
        () => PanelContent.Create,
      ],
      [() => true, () => PanelContent.Customize],
    ])([workspace.billingStatus, currentRuntime, status]);
    const [panelContent, setPanelContent] =
      useState<PanelContent>(initialPanelContent);

    const validMainMachineTypes =
      analysisConfig.computeType === ComputeType.Standard
        ? validLeoGceMachineTypes
        : validLeoDataprocMasterMachineTypes;
    // The compute type affects the set of valid machine types, so revert to the
    // default machine type if switching compute types would invalidate the main
    // machine type choice.
    useEffect(() => {
      if (
        !validMainMachineTypes.find(
          ({ name }) => name === analysisConfig.machine.name
        )
      ) {
        setAnalysisConfig({
          ...analysisConfig,
          machine: existingAnalysisConfig.machine,
        });
      }
    }, [analysisConfig.computeType]);

    const runtimeExists =
      (status &&
        ![RuntimeStatus.Deleted, RuntimeStatus.Error].includes(status)) ||
      !!pendingRuntime;
    const disableControls =
      runtimeExists &&
      ![RuntimeStatus.Running, RuntimeStatus.Stopped].includes(
        status as RuntimeStatus
      );

    const dataprocExists =
      runtimeExists && existingAnalysisConfig.dataprocConfig !== null;

    const attachedPdExists =
      !!persistentDisk &&
      runtimeExists &&
      existingAnalysisConfig.diskConfig.detachable;
    const unattachedPdExists = !!persistentDisk && !attachedPdExists;
    const unattachedDiskNeedsRecreate =
      unattachedPdExists &&
      analysisConfig.diskConfig.detachable &&
      (persistentDisk.size > analysisConfig.diskConfig.size ||
        persistentDisk.diskType !== analysisConfig.diskConfig.detachableType);

    const disableDetachableReason = cond(
      [
        analysisConfig.computeType === ComputeType.Dataproc,
        () => 'Reattachable disks are unsupported for this compute type',
      ],
      [
        runtimeExists &&
          existingAnalysisConfig?.diskConfig?.detachable === false,
        () =>
          'To use a detachable disk, first delete your analysis environment',
      ],
      () => null
    );

    let configDiffs: AnalysisDiff[] = [];
    let updateMessaging: UpdateMessaging;
    if (runtimeExists) {
      configDiffs = getAnalysisConfigDiffs(
        existingAnalysisConfig,
        analysisConfig
      );
      updateMessaging = diffsToUpdateMessaging(configDiffs);
    }
    const runtimeChanged = configDiffs.length > 0;

    const [creatorFreeCreditsRemaining, setCreatorFreeCreditsRemaining] =
      useState(null);
    useEffect(() => {
      const aborter = new AbortController();
      const fetchFreeCredits = async () => {
        const { freeCreditsRemaining } =
          await workspacesApi().getWorkspaceCreatorFreeCreditsRemaining(
            namespace,
            id,
            { signal: aborter.signal }
          );
        setCreatorFreeCreditsRemaining(freeCreditsRemaining);
      };

      fetchFreeCredits();

      return function cleanup() {
        aborter.abort();
      };
    }, []);

    // Leonardo enforces a minimum limit for disk size, 4000 GB is our arbitrary limit for not making a
    // disk that is way too big and expensive on free tier ($.22 an hour). 64 TB is the GCE limit on
    // persistent disk.
    const diskSizeValidatorWithMessage = (
      diskType = 'standard' || 'master' || 'worker'
    ) => {
      const maxDiskSize = isUsingFreeTierBillingAccount(workspace)
        ? 4000
        : 64000;
      const message = {
        standard: `^Disk size must be between ${MIN_DISK_SIZE_GB} and ${maxDiskSize} GB`,
        master: `^Master disk size must be between ${MIN_DISK_SIZE_GB} and ${maxDiskSize} GB`,
        worker: `^Worker disk size must be between ${DATAPROC_WORKER_MIN_DISK_SIZE_GB} and ${maxDiskSize} GB`,
      };

      return {
        numericality: {
          greaterThanOrEqualTo:
            diskType === 'worker'
              ? DATAPROC_WORKER_MIN_DISK_SIZE_GB
              : MIN_DISK_SIZE_GB,
          lessThanOrEqualTo: maxDiskSize,
          message: message[diskType],
        },
      };
    };

    const costErrorsAsWarnings =
      !isUsingFreeTierBillingAccount(workspace) ||
      // We've increased the workspace creator's free credits. This means they may be expecting to run
      // a more expensive analysis, and the program has extended some further trust for free credit
      // use. Allow them to provision a larger runtime (still warn them). Block them if they get below
      // the default amount of free credits because (1) this can result in overspend and (2) we have
      // easy access to remaining credits, and not the creator's quota.
      creatorFreeCreditsRemaining >
        serverConfigStore.get().config.defaultFreeCreditsDollarLimit;

    const runningCostValidatorWithMessage = () => {
      const maxRunningCost = isUsingFreeTierBillingAccount(workspace)
        ? 25
        : 150;
      const message = costErrorsAsWarnings
        ? '^Your runtime is expensive. Are you sure you wish to proceed?'
        : `^Your runtime is too expensive. To proceed using free credits, reduce your running costs below $${maxRunningCost}/hr.`;
      return {
        numericality: {
          lessThan: maxRunningCost,
          message: message,
        },
      };
    };

    const currentRunningCost = machineRunningCost(analysisConfig);

    const diskValidator = {
      diskSize: diskSizeValidatorWithMessage('standard'),
    };

    const runningCostValidator = {
      currentRunningCost: runningCostValidatorWithMessage(),
    };
    // We don't clear dataproc config when we change compute type so we can't combine this with the
    // above or else we can end up with phantom validation fails
    const dataprocValidators = {
      masterDiskSize: diskSizeValidatorWithMessage('master'),
      workerDiskSize: diskSizeValidatorWithMessage('worker'),
      numberOfWorkers: {
        numericality: {
          greaterThanOrEqualTo: 2,
          message: 'Dataproc requires at least 2 worker nodes',
        },
      },
    };

    const { masterDiskSize, workerDiskSize, numberOfWorkers } =
      analysisConfig.dataprocConfig || {};
    const diskErrors = validate(
      { diskSize: analysisConfig.diskConfig.size },
      diskValidator
    );
    const runningCostErrors = validate(
      { currentRunningCost },
      runningCostValidator
    );
    const dataprocErrors =
      analysisConfig.computeType === ComputeType.Dataproc
        ? validate(
            { masterDiskSize, workerDiskSize, numberOfWorkers },
            dataprocValidators
          )
        : undefined;

    const getErrorMessageContent = () => {
      const errorDivs = [];
      if (diskErrors) {
        errorDivs.push(summarizeErrors(diskErrors));
      }
      if (dataprocErrors) {
        errorDivs.push(summarizeErrors(dataprocErrors));
      }
      if (!costErrorsAsWarnings && runningCostErrors) {
        errorDivs.push(summarizeErrors(runningCostErrors));
      }
      return errorDivs;
    };

    const getWarningMessageContent = () => {
      const warningDivs = [];
      if (costErrorsAsWarnings && runningCostErrors) {
        warningDivs.push(summarizeErrors(runningCostErrors));
      }
      return warningDivs;
    };

    const runtimeCanBeCreated = !(getErrorMessageContent().length > 0);
    // Casting to RuntimeStatus here because it can't easily be done at the destructuring level
    // where we get 'status' from
    const runtimeCanBeUpdated =
      runtimeChanged &&
      [RuntimeStatus.Running, RuntimeStatus.Stopped].includes(
        status as RuntimeStatus
      ) &&
      runtimeCanBeCreated;

    const renderUpdateButton = () => {
      return (
        <Button
          aria-label='Update'
          disabled={!runtimeCanBeUpdated}
          onClick={() => {
            requestAnalysisConfig(analysisConfig);
            onClose();
          }}
        >
          {updateMessaging.applyAction}
        </Button>
      );
    };

    const renderCreateButton = () => {
      return (
        <Button
          aria-label='Create'
          disabled={!runtimeCanBeCreated}
          onClick={() => {
            requestAnalysisConfig(analysisConfig);
            onClose();
          }}
        >
          Create
        </Button>
      );
    };

    const renderNextWithDiskDeleteButton = () => {
      return (
        <Button
          aria-label='Next'
          disabled={!runtimeCanBeCreated}
          onClick={() => {
            setPanelContent(PanelContent.DeleteUnattachedPdAndCreate);
          }}
        >
          Next
        </Button>
      );
    };

    const renderTryAgainButton = () => {
      return (
        <Button
          aria-label='Try Again'
          disabled={!runtimeCanBeCreated}
          onClick={() => {
            requestAnalysisConfig(analysisConfig);
            onClose();
          }}
        >
          Try Again
        </Button>
      );
    };

    const updateYieldsUnusedDisk =
      existingAnalysisConfig.diskConfig.detachable &&
      !analysisConfig.diskConfig.detachable;
    const renderNextUpdateButton = () => {
      return (
        <Button
          aria-label='Next'
          disabled={!runtimeCanBeUpdated}
          onClick={() => {
            if (updateYieldsUnusedDisk) {
              setPanelContent(PanelContent.ConfirmUpdateWithDiskDelete);
            } else {
              setPanelContent(PanelContent.ConfirmUpdate);
            }
          }}
        >
          Next
        </Button>
      );
    };

    return (
      <div id='runtime-panel'>
        {cond(
          [
            [PanelContent.Create, PanelContent.Customize].includes(
              panelContent
            ),
            () => (
              <div style={{ marginBottom: '1rem' }}>
                Your analysis environment consists of an application and compute
                resources. Your cloud environment is unique to this workspace
                and not shared with other users.
              </div>
            ),
          ],
          () => null
        )}
        {switchCase(
          panelContent,
          [
            PanelContent.Create,
            () => (
              <Fragment>
                <CreatePanel
                  creatorFreeCreditsRemaining={creatorFreeCreditsRemaining}
                  profile={profile}
                  setPanelContent={(value) => setPanelContent(value)}
                  workspace={workspace}
                  analysisConfig={analysisConfig}
                />
                <FlexRow
                  style={{ justifyContent: 'flex-end', marginTop: '1rem' }}
                >
                  {renderCreateButton()}
                </FlexRow>
              </Fragment>
            ),
          ],
          [
            PanelContent.DeleteRuntime,
            () => {
              if (attachedPdExists) {
                return (
                  <ConfirmDeleteRuntimeWithPD
                    onConfirm={async (runtimeStatusReq) => {
                      await setRuntimeStatus(runtimeStatusReq);
                      onClose();
                    }}
                    onCancel={() => setPanelContent(PanelContent.Customize)}
                    computeType={existingAnalysisConfig.computeType}
                    disk={persistentDisk}
                  />
                );
              } else {
                return (
                  <ConfirmDelete
                    onConfirm={async () => {
                      await setRuntimeStatus(
                        RuntimeStatusRequest.DeleteRuntime
                      );
                      onClose();
                    }}
                    onCancel={() => setPanelContent(PanelContent.Customize)}
                  />
                );
              }
            },
          ],
          [
            PanelContent.DeleteUnattachedPd,
            () => (
              <ConfirmDeleteUnattachedPD
                onConfirm={async () => {
                  await diskApi().deleteDisk(namespace, persistentDisk.name);
                  onClose();
                }}
                onCancel={() => setPanelContent(PanelContent.Customize)}
              />
            ),
          ],
          [
            PanelContent.DeleteUnattachedPdAndCreate,
            () => (
              <ConfirmDeleteUnattachedPD
                showCreateMessaging
                onConfirm={async () => {
                  await diskApi().deleteDisk(namespace, persistentDisk.name);
                  requestAnalysisConfig(analysisConfig);
                  onClose();
                }}
                onCancel={() => setPanelContent(PanelContent.Customize)}
              />
            ),
          ],
          [
            PanelContent.Customize,
            () => (
              <div style={{ marginBottom: '10px' }}>
                <div style={styles.controlSection}>
                  <FlexRow style={styles.costPredictorWrapper}>
                    <StartStopRuntimeButton
                      workspaceNamespace={workspace.namespace}
                      googleProject={workspace.googleProject}
                    />
                    <CostInfo
                      {...{
                        runtimeChanged,
                        analysisConfig,
                        workspace,
                        creatorFreeCreditsRemaining,
                        currentUser: profile.username,
                      }}
                    />
                  </FlexRow>
                  {currentRuntime?.errors && currentRuntime.errors.length > 0 && (
                    <ErrorMessage iconPosition={'top'} iconSize={16}>
                      <div>
                        An error was encountered with your cloud environment.
                        Please re-attempt creation of the environment and
                        contact support if the error persists.
                      </div>
                      <div>Error details:</div>
                      {currentRuntime.errors.map((err, idx) => {
                        return (
                          <div style={{ fontFamily: 'monospace' }} key={idx}>
                            {err.errorMessage}
                          </div>
                        );
                      })}
                    </ErrorMessage>
                  )}
                  <PresetSelector
                    {...{
                      allowDataproc,
                      setAnalysisConfig,
                      persistentDisk,
                      disabled: disableControls,
                    }}
                  />
                  {/* Runtime customization: change detailed machine configuration options. */}
                  <h3 style={{ ...styles.sectionHeader, ...styles.bold }}>
                    Cloud compute profile
                  </h3>
                  <div style={styles.formGrid3}>
                    <MachineSelector
                      idPrefix='runtime'
                      disabled={disableControls}
                      selectedMachine={analysisConfig.machine}
                      onChange={(machine: Machine) =>
                        setAnalysisConfig({ ...analysisConfig, machine })
                      }
                      validMachineTypes={validMainMachineTypes}
                      machineType={analysisConfig.machine.name}
                    />
                    {enablePersistentDisk || (
                      <DiskSizeSelector
                        idPrefix='runtime'
                        onChange={(size: number) =>
                          setAnalysisConfig({
                            ...analysisConfig,
                            diskConfig: {
                              size,
                              detachable: false,
                              detachableType: null,
                              existingDiskName: null,
                            },
                          })
                        }
                        diskSize={analysisConfig.diskConfig.size}
                        disabled={disableControls}
                      />
                    )}
                  </div>
                  {enableGpu &&
                    analysisConfig.computeType === ComputeType.Standard && (
                      <GpuConfigSelector
                        disabled={disableControls}
                        onChange={(gpuConfig: GpuConfig) =>
                          setAnalysisConfig({ ...analysisConfig, gpuConfig })
                        }
                        selectedMachine={analysisConfig.machine}
                        gpuConfig={analysisConfig.gpuConfig}
                      />
                    )}
                  <FlexRow
                    style={{
                      marginTop: '1rem',
                      justifyContent: 'space-between',
                    }}
                  >
                    <FlexColumn>
                      <label style={styles.label} htmlFor='runtime-compute'>
                        Compute type
                      </label>
                      <FlexRow style={{ gap: '10px', alignItems: 'center' }}>
                        <Dropdown
                          id='runtime-compute'
                          disabled={!allowDataproc || disableControls}
                          style={{ width: '10rem' }}
                          options={[ComputeType.Standard, ComputeType.Dataproc]}
                          value={
                            analysisConfig.computeType || ComputeType.Standard
                          }
                          onChange={({ value: computeType }) =>
                            // When the compute type changes, we need to normalize the config and potentially restore defualts.
                            setAnalysisConfig(
                              withAnalysisConfigDefaults(
                                { ...analysisConfig, computeType },
                                persistentDisk
                              )
                            )
                          }
                        />
                        {analysisConfig.computeType ===
                          ComputeType.Dataproc && (
                          <TooltipTrigger
                            content={
                              status !== RuntimeStatus.Running
                                ? 'Start your Dataproc cluster to access the Spark console'
                                : null
                            }
                          >
                            <LinkButton
                              data-test-id='manage-spark-console'
                              disabled={
                                status !== RuntimeStatus.Running ||
                                existingAnalysisConfig.computeType !==
                                  ComputeType.Dataproc
                              }
                              onClick={() =>
                                setPanelContent(PanelContent.SparkConsole)
                              }
                            >
                              Manage and monitor Spark console
                            </LinkButton>
                          </TooltipTrigger>
                        )}
                      </FlexRow>
                    </FlexColumn>
                  </FlexRow>
                  {analysisConfig.computeType === ComputeType.Dataproc && (
                    <DataProcConfigSelector
                      disabled={disableControls}
                      runtimeStatus={status}
                      dataprocExists={dataprocExists}
                      onChange={(dataprocConfig: DataprocConfig) =>
                        setAnalysisConfig({ ...analysisConfig, dataprocConfig })
                      }
                      dataprocConfig={analysisConfig.dataprocConfig}
                    />
                  )}
                  <FlexRow
                    style={{
                      marginTop: '1rem',
                      justifyContent: 'space-between',
                    }}
                  >
                    <FlexColumn>
                      <label style={styles.label} htmlFor='runtime-autopause'>
                        Automatically pause after idle for
                      </label>
                      <Dropdown
                        id='runtime-autopause'
                        disabled={disableControls}
                        style={{ width: '10rem' }}
                        options={Array.from(
                          AutopauseMinuteThresholds.entries()
                        ).map((entry) => ({
                          label: entry[1],
                          value: entry[0],
                        }))}
                        value={
                          analysisConfig.autopauseThreshold ||
                          DEFAULT_AUTOPAUSE_THRESHOLD_MINUTES
                        }
                        onChange={({ value: autopauseThreshold }) =>
                          setAnalysisConfig({
                            ...analysisConfig,
                            autopauseThreshold,
                          })
                        }
                      />
                    </FlexColumn>
                  </FlexRow>
                </div>
                {enablePersistentDisk && (
                  <DiskSelector
                    diskConfig={analysisConfig.diskConfig}
                    onChange={(diskConfig) =>
                      setAnalysisConfig({
                        ...analysisConfig,
                        diskConfig,
                        detachedDisk: diskConfig.detachable
                          ? null
                          : persistentDisk,
                      })
                    }
                    disabled={disableControls}
                    disableDetachableReason={disableDetachableReason}
                    existingDisk={persistentDisk}
                  />
                )}
                {runtimeExists && updateMessaging.warn && (
                  <WarningMessage iconSize={30} iconPosition={'center'}>
                    <div>{updateMessaging.warn}</div>
                  </WarningMessage>
                )}
                {getErrorMessageContent().length > 0 && (
                  <ErrorMessage
                    iconSize={16}
                    iconPosition={'top'}
                    data-test-id={'runtime-error-messages'}
                  >
                    {getErrorMessageContent()}
                  </ErrorMessage>
                )}
                {getWarningMessageContent().length > 0 && (
                  <WarningMessage
                    iconSize={16}
                    iconPosition={'top'}
                    data-test-id={'runtime-warning-messages'}
                  >
                    {getWarningMessageContent()}
                  </WarningMessage>
                )}
                {unattachedPdExists && !runtimeExists ? (
                  <FlexRow
                    style={{
                      justifyContent: 'space-between',
                      marginTop: '.75rem',
                    }}
                  >
                    <LinkButton
                      style={{
                        ...styles.deleteLink,
                        ...(disableControls
                          ? { color: colorWithWhiteness(colors.dark, 0.4) }
                          : {}),
                      }}
                      aria-label='Delete Persistent Disk'
                      disabled={disableControls}
                      onClick={() =>
                        setPanelContent(PanelContent.DeleteUnattachedPd)
                      }
                    >
                      Delete Persistent Disk
                    </LinkButton>
                    {unattachedDiskNeedsRecreate
                      ? renderNextWithDiskDeleteButton()
                      : renderCreateButton()}
                  </FlexRow>
                ) : (
                  <FlexRow
                    style={{
                      justifyContent: 'space-between',
                      marginTop: '.75rem',
                    }}
                  >
                    <LinkButton
                      style={{
                        ...styles.deleteLink,
                        ...(disableControls || !runtimeExists
                          ? { color: colorWithWhiteness(colors.dark, 0.4) }
                          : {}),
                      }}
                      aria-label='Delete Environment'
                      disabled={disableControls || !runtimeExists}
                      onClick={() =>
                        setPanelContent(PanelContent.DeleteRuntime)
                      }
                    >
                      Delete Environment
                    </LinkButton>
                    {cond(
                      [runtimeExists, () => renderNextUpdateButton()],
                      [
                        unattachedDiskNeedsRecreate,
                        () => renderNextWithDiskDeleteButton(),
                      ],
                      [
                        currentRuntime?.errors &&
                          currentRuntime.errors.length > 0,
                        () => renderTryAgainButton(),
                      ],
                      () => renderCreateButton()
                    )}
                  </FlexRow>
                )}
              </div>
            ),
          ],
          [
            PanelContent.ConfirmUpdate,
            () => (
              <ConfirmUpdatePanel
                existingAnalysisConfig={existingAnalysisConfig}
                newAnalysisConfig={analysisConfig}
                onCancel={() => {
                  setPanelContent(PanelContent.Customize);
                }}
                updateButton={renderUpdateButton()}
              />
            ),
          ],
          [
            PanelContent.ConfirmUpdateWithDiskDelete,
            () => (
              <OfferDeleteDiskWithUpdate
                onNext={(deleteDetachedDisk: boolean) => {
                  if (deleteDetachedDisk) {
                    setAnalysisConfig({
                      ...analysisConfig,
                      detachedDisk: null,
                    });
                  }
                  setPanelContent(PanelContent.ConfirmUpdate);
                }}
                onCancel={() => setPanelContent(PanelContent.Customize)}
                disk={persistentDisk}
              />
            ),
          ],
          [PanelContent.Disabled, () => <DisabledPanel />],
          [
            PanelContent.SparkConsole,
            () => <SparkConsolePanel {...workspace} />,
          ]
        )}
      </div>
    );
  }
);

export const RuntimePanelWrapper = withStore(
  runtimeStore,
  'runtime'
)(({ runtime, onClose = () => {} }) => {
  if (!runtime.runtimeLoaded) {
    return <Spinner style={{ width: '100%', marginTop: '5rem' }} />;
  }

  return <RuntimePanel onClose={onClose} />;
});
