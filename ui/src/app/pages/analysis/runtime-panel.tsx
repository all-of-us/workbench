import { Button, Clickable, LinkButton } from 'app/components/buttons';
import { FlexColumn, FlexRow } from 'app/components/flex';
import { ClrIcon } from 'app/components/icons';
import { ErrorMessage, WarningMessage } from 'app/components/messages';
import { TooltipTrigger } from 'app/components/popups';
import { Spinner } from 'app/components/spinners';
import { TextColumn } from 'app/components/text-column';

import { disksApi, workspacesApi } from 'app/services/swagger-fetch-clients';
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
  diskPricePerMonth,
  findGpu,
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
  getRuntimeConfigDiffs,
  RuntimeConfig,
  RuntimeDiffState,
  RuntimeStatusRequest,
  useCustomRuntime,
  useRuntimeStatus,
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
  workerConfigLabel: {
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
  costsDrawnFrom: {
    borderLeft: `1px solid ${colorWithWhiteness(colors.dark, 0.5)}`,
    padding: '.33rem .5rem',
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
    justifyContent: 'space-between',
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
});

// exported for testing
export const MIN_DISK_SIZE_GB = 100;
export const DATAPROC_WORKER_MIN_DISK_SIZE_GB = 150;

const defaultMachineName = 'n1-standard-4';
const defaultMachineType: Machine = findMachineByName(defaultMachineName);
const defaultDiskSize = 100;

// Returns true if two runtimes are equivalent in terms of the fields which are
// affected by runtime presets.
const presetEquals = (a: Runtime, b: Runtime): boolean => {
  const strip = fp.flow(
    // In the future, things like toolDockerImage and autopause may be considerations.
    fp.pick(['gceConfig', 'dataprocConfig']),
    // numberOfWorkerLocalSSDs is currently part of the API spec, but is not used by the panel.
    fp.omit(['dataprocConfig.numberOfWorkerLocalSSDs'])
  );
  return fp.isEqual(strip(a), strip(b));
};

enum PanelContent {
  Create = 'Create',
  Customize = 'Customize',
  DeleteRuntime = 'DeleteRuntime',
  DeleteUnattachedPd = 'DeleteUnattachedPd',
  Disabled = 'Disabled',
  Confirm = 'Confirm',
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

export const ConfirmDeleteUnattachedPD = ({ onConfirm, onCancel }) => {
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
          Delete environment options
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
          Delete
        </Button>
      </FlexRow>
    </Fragment>
  );
};

export const ConfirmDeleteRuntimeWithPD = ({
  onCancel,
  onConfirm,
  computeType,
  pdSize,
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
              name='ageType'
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
          <b>{formatUsd(diskPricePerMonth * pdSize)}</b> per month. You can
          delete your disk at any time via the runtime panel.
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
              name='ageType'
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
          <b>{formatUsd(diskPricePerMonth * pdSize)}</b> per month.
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
      <div>
        <div>
          To backup and share files, such as input data, analysis outputs, or
          installed packages,
          <a href={supportUrls.workspaceBucket}>
            move them to the workspace bucket.
          </a>
        </div>
        <div>
          Note: Jupyter notebooks are autosaved to the workspace bucket, and
          deleting your disk will not delete your notebooks.
        </div>
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
    findMachineByName(machineType) || defaultMachineType;
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
  selectedDiskSize,
  diskSize,
  idPrefix,
}) => {
  return (
    <FlexRow style={styles.labelAndInput}>
      <label style={styles.label} htmlFor={`${idPrefix}-disk`}>
        Disk (GB)
      </label>
      <InputNumber
        id={`${idPrefix}-disk`}
        showButtons
        disabled={disabled}
        decrementButtonClassName='p-button-secondary'
        incrementButtonClassName='p-button-secondary'
        value={selectedDiskSize || diskSize}
        inputStyle={styles.inputNumber}
        onChange={({ value }) => onChange(value)}
      />
    </FlexRow>
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

const PersistentDiskSizeSelector = ({
  onChange,
  disabled,
  selectedDiskSize,
  diskSize,
}) => {
  return (
    <div>
      <h3 style={{ ...styles.sectionHeader, ...styles.bold }}>
        Persistent Disk (GB)
      </h3>
      <div>
        {' '}
        Persistent disks store analysis data.
        <a href='https://support.terra.bio/hc/en-us/articles/360047318551'>
          Learn more about persistent disks and where your disk is mounted.
        </a>
      </div>
      <InputNumber
        id={'persistent-disk'}
        showButtons
        disabled={disabled}
        decrementButtonClassName='p-button-secondary'
        incrementButtonClassName='p-button-secondary'
        value={selectedDiskSize || diskSize}
        inputStyle={styles.inputNumber}
        onChange={({ value }) => onChange(value)}
      />
    </div>
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
    workerMachineType = defaultMachineName,
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
      <legend style={styles.workerConfigLabel}>Worker Configuration</legend>
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
          selectedDiskSize={selectedDiskSize}
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
  setSelectedDiskSize,
  setSelectedMachine,
  setSelectedCompute,
  setSelectedDataprocConfig,
  disabled,
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
        const { presetDiskSize, presetMachineName, presetCompute } = fp.cond([
          // Can't destructure due to shadowing.
          [
            () => !!value.gceConfig,
            (tmpl: Runtime) => ({
              presetDiskSize: tmpl.gceConfig.diskSize,
              presetMachineName: tmpl.gceConfig.machineType,
              presetCompute: ComputeType.Standard,
            }),
          ],
          [
            () => !!value.dataprocConfig,
            ({ dataprocConfig: { masterDiskSize, masterMachineType } }) => ({
              presetDiskSize: masterDiskSize,
              presetMachineName: masterMachineType,
              presetCompute: ComputeType.Dataproc,
            }),
          ],
        ])(value);

        const presetMachineType = findMachineByName(presetMachineName);
        setSelectedDiskSize(presetDiskSize);
        setSelectedMachine(presetMachineType);
        setSelectedCompute(presetCompute);
        setSelectedDataprocConfig(value.dataprocConfig);

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
  runtimeConfig,
  currentUser,
  workspace,
  creatorFreeCreditsRemaining,
  runtimeCtx,
}) => {
  const remainingCredits =
    creatorFreeCreditsRemaining === null ? (
      <Spinner size={10} />
    ) : (
      formatUsd(creatorFreeCreditsRemaining)
    );

  return (
    <FlexRow
      style={
        runtimeChanged
          ? {
              backgroundColor: colorWithWhiteness(colors.warning, 0.9),
              borderColor: colors.warning,
            }
          : {}
      }
      data-test-id='cost-estimator'
    >
      <div style={{ minWidth: '250px', margin: '.33rem .5rem' }}>
        <RuntimeCostEstimator
          runtimeParameters={runtimeConfig}
          usePersistentDisk={runtimeCtx.enablePD && !runtimeCtx.dataprocExists}
        />
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
  runtimeConfig,
  runtimeCtx,
}) => {
  const displayName =
    runtimeConfig.computeType === ComputeType.Dataproc
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
          runtimeConfig={runtimeConfig}
          currentUser={profile.username}
          workspace={workspace}
          creatorFreeCreditsRemaining={creatorFreeCreditsRemaining}
          runtimeCtx={runtimeCtx}
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
      <RuntimeSummary runtimeConfig={runtimeConfig} />
    </div>
  );
};

const ConfirmUpdatePanel = ({
  initialRuntimeConfig,
  newRuntimeConfig,
  onCancel,
  updateButton,
  runtimeCtx,
}) => {
  const runtimeDiffs = getRuntimeConfigDiffs(
    initialRuntimeConfig,
    newRuntimeConfig,
    runtimeCtx
  );
  const updateMessaging = diffsToUpdateMessaging(runtimeDiffs);
  const usePersistentDisk = runtimeCtx.enablePD && !runtimeCtx.dataprocExists;
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
          {runtimeDiffs.map((diff, i) => (
            <li key={i}>
              {diff.desc} from <b>{diff.previous}</b> to <b>{diff.new}</b>
            </li>
          ))}
        </ul>
        <FlexRow style={{ marginTop: '.5rem' }}>
          <div style={{ marginRight: '1rem' }}>
            <b style={{ fontSize: 10 }}>New estimated cost</b>
            <div
              style={{
                ...styles.costPredictorWrapper,
                padding: '.25rem .5rem',
              }}
            >
              <RuntimeCostEstimator
                runtimeParameters={newRuntimeConfig}
                usePersistentDisk={usePersistentDisk}
              />
            </div>
          </div>
          <div>
            <b style={{ fontSize: 10 }}>Previous estimated cost</b>
            <div
              style={{
                ...styles.costPredictorWrapper,
                padding: '.25rem .5rem',
                color: 'grey',
                backgroundColor: '',
              }}
            >
              <RuntimeCostEstimator
                runtimeParameters={initialRuntimeConfig}
                usePersistentDisk={usePersistentDisk}
                costTextColor='grey'
              />
            </div>
          </div>
        </FlexRow>
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
    let [{ currentRuntime, pendingRuntime }, setRequestedRuntime] =
      useCustomRuntime(namespace, persistentDisk);
    // If the runtime has been deleted, it's possible that the default preset values have changed since its creation
    if (currentRuntime && currentRuntime.status === RuntimeStatus.Deleted) {
      currentRuntime = applyPresetOverride(currentRuntime);
    }

    // Prioritize the "pendingRuntime", if any. When an update is pending, we want
    // to render the target runtime details, which  may not match the current runtime.
    const existingRuntime =
      pendingRuntime || currentRuntime || ({} as Partial<Runtime>);
    const { dataprocConfig = null, gceConfig = { diskSize: defaultDiskSize } } =
      existingRuntime;
    const [status, setRuntimeStatus] = useRuntimeStatus(
      namespace,
      googleProject
    );
    const diskSize = dataprocConfig
      ? dataprocConfig.masterDiskSize
      : gceConfig.diskSize
      ? gceConfig.diskSize
      : defaultDiskSize;
    const machineName = dataprocConfig
      ? dataprocConfig.masterMachineType
      : gceConfig.machineType;
    const initialMasterMachine =
      findMachineByName(machineName) || defaultMachineType;
    const initialCompute = dataprocConfig
      ? ComputeType.Dataproc
      : ComputeType.Standard;
    const pdExists = !!persistentDisk;
    const pdSize = pdExists ? persistentDisk.size : defaultDiskSize;
    const initialAutopauseThreshold =
      existingRuntime.autopauseThreshold || DEFAULT_AUTOPAUSE_THRESHOLD_MINUTES;
    const gpuConfig = gceConfig?.gpuConfig ?? null;
    const enableGpu = serverConfigStore.get().config.enableGpu;

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

    const [selectedMachine, setSelectedMachine] =
      useState(initialMasterMachine);
    const [selectedDiskSize, setSelectedDiskSize] = useState(diskSize);
    const [selectedCompute, setSelectedCompute] =
      useState<ComputeType>(initialCompute);
    const [selectedAutopauseThreshold, setSelectedAutopauseThreshold] =
      useState(initialAutopauseThreshold);

    // Note: while the Dataproc config does contain masterMachineType and masterDiskSize,
    // the source of truth for these values are selectedMachine, and selectedDiskSize, as
    // these UI components are used for both Dataproc and standard VMs.
    const [selectedDataprocConfig, setSelectedDataprocConfig] =
      useState<DataprocConfig | null>(dataprocConfig);
    const [selectedPdSize, setSelectedPdSize] = useState(pdSize);

    const [selectedGpuConfig, setSelectedGpuConfig] =
      useState<GpuConfig | null>(gpuConfig);

    const validMainMachineTypes =
      selectedCompute === ComputeType.Standard
        ? validLeoGceMachineTypes
        : validLeoDataprocMasterMachineTypes;
    // The compute type affects the set of valid machine types, so revert to the
    // default machine type if switching compute types would invalidate the main
    // machine type choice.
    useEffect(() => {
      if (
        !validMainMachineTypes.find(({ name }) => name === selectedMachine.name)
      ) {
        setSelectedMachine(initialMasterMachine);
      }
    }, [selectedCompute]);

    const runtimeExists =
      (status &&
        ![RuntimeStatus.Deleted, RuntimeStatus.Error].includes(status)) ||
      !!pendingRuntime;
    const disableControls =
      runtimeExists &&
      ![RuntimeStatus.Running, RuntimeStatus.Stopped].includes(
        status as RuntimeStatus
      );

    const initialRuntimeConfig = {
      computeType: initialCompute,
      machine: initialMasterMachine,
      diskSize,
      dataprocConfig,
      gpuConfig,
      pdSize,
      autopauseThreshold: initialAutopauseThreshold,
    };

    const newRuntimeConfig = {
      computeType: selectedCompute,
      machine: selectedMachine,
      diskSize: selectedDiskSize,
      dataprocConfig: selectedDataprocConfig,
      gpuConfig:
        selectedCompute === ComputeType.Standard ? selectedGpuConfig : null,
      pdSize: selectedPdSize,
      autopauseThreshold: selectedAutopauseThreshold,
    };

    const gceExists = runtimeExists && initialCompute === ComputeType.Standard;
    const dataprocExists = dataprocConfig !== null;
    const enablePD =
      serverConfigStore.get().config.enablePersistentDisk &&
      (pdExists || !gceExists);
    const unattachedPdExists = enablePD && !gceExists && pdExists;
    const pdSizeReduced = selectedPdSize < pdSize;
    // A runtime context can wrap/pass complex runtime context and also make the code cleaner
    const runtimeCtx = {
      runtimeExists: runtimeExists,
      gceExists: gceExists,
      dataprocExists: dataprocExists,
      pdExists: pdExists,
      // Here the enablePD is not simply the pd feature flag.
      // It stands for the point when user has no old version gce instances exists and the pd feature flag is also True at the same time.
      // By using this predicate, we can differentiate the old version and pd version panel much more easily. The code is also cleaner.
      // In addition, after all the old users change to the new pd version, we can simply replace all occurrences of this variable to True.
      // The code refactor cost is low.
      enablePD: enablePD,
      unattachedPdExists: unattachedPdExists,
    };
    const runtimeDiffs = getRuntimeConfigDiffs(
      initialRuntimeConfig,
      newRuntimeConfig,
      runtimeCtx
    );
    const updateMessaging = diffsToUpdateMessaging(runtimeDiffs);
    const runtimeChanged = runtimeExists && runtimeDiffs.length > 0;

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
    const createStandardComputeRuntimeRequest = (runtime: RuntimeConfig) => {
      // The logic here is tricky to be compatible
      // post launch PD when all existing running Runtime shutdown.
      const runtimeDiffTypes = runtimeDiffs.map((diff) => diff.differenceType);
      const runtimeNeedsDelete =
        runtimeDiffTypes.includes(RuntimeDiffState.NEEDS_DELETE_RUNTIME) ||
        runtimeDiffTypes.includes(RuntimeDiffState.NEEDS_DELETE_PD);
      if (
        runtimeCtx.enablePD &&
        (!runtimeCtx.gceExists || runtimeNeedsDelete)
      ) {
        return {
          gceWithPdConfig: {
            machineType: runtime.machine.name,
            persistentDisk: {
              // When reducing PD size, passing empty name to backend then API will create a new PD
              name: !pdExists || pdSizeReduced ? '' : persistentDisk.name,
              size: runtime.pdSize,
              diskType: DiskType.Standard,
              labels: {},
            },
            gpuConfig: runtime.gpuConfig,
          },
        };
      } else {
        return {
          gceConfig: {
            machineType: runtime.machine.name,
            diskSize: !runtimeCtx.enablePD ? runtime.diskSize : runtime.pdSize,
            gpuConfig: runtime.gpuConfig,
          },
        };
      }
    };
    const createRuntimeRequest = (runtime: RuntimeConfig) => {
      const runtimeRequest: Runtime =
        runtime.computeType === ComputeType.Dataproc
          ? {
              dataprocConfig: {
                ...runtime.dataprocConfig,
                masterMachineType: runtime.machine.name,
                masterDiskSize: runtime.diskSize,
              },
            }
          : runtime.computeType === ComputeType.Standard
          ? createStandardComputeRuntimeRequest(runtime)
          : null;

      // If the selected runtime matches a preset, plumb through the appropriate configuration type.
      runtimeRequest.configurationType =
        fp.get(
          'runtimeTemplate.configurationType',
          fp.find(
            ({ runtimeTemplate }) =>
              presetEquals(runtimeRequest, runtimeTemplate),
            runtimePresets
          )
        ) || RuntimeConfigurationType.UserOverride;

      runtimeRequest.autopauseThreshold = runtime.autopauseThreshold;

      return runtimeRequest;
    };

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

    const currentRunningCost = machineRunningCost({
      computeType: selectedCompute,
      masterMachine: selectedMachine,
      masterDiskSize: diskSize,
      gpu: gpuConfig ? findGpu(gpuConfig.gpuType, gpuConfig.numOfGpus) : null,
      numberOfWorkers: selectedDataprocConfig?.numberOfWorkers,
      numberOfPreemptibleWorkers:
        selectedDataprocConfig?.numberOfPreemptibleWorkers,
      workerDiskSize: selectedDataprocConfig?.workerDiskSize,
      workerMachine:
        selectedDataprocConfig &&
        findMachineByName(selectedDataprocConfig.workerMachineType),
    });

    const standardDiskValidator = {
      selectedDiskSize: diskSizeValidatorWithMessage('standard'),
    };

    const standardPdValidator = {
      selectedPdSize: diskSizeValidatorWithMessage('standard'),
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
      selectedDataprocConfig || {};
    const standardDiskErrors = validate(
      { selectedDiskSize },
      standardDiskValidator
    );
    const standardPdErrors = validate({ selectedPdSize }, standardPdValidator);
    const runningCostErrors = validate(
      { currentRunningCost },
      runningCostValidator
    );
    const dataprocErrors =
      selectedCompute === ComputeType.Dataproc
        ? validate(
            { masterDiskSize, workerDiskSize, numberOfWorkers },
            dataprocValidators
          )
        : undefined;

    const getErrorMessageContent = () => {
      const errorDivs = [];
      if (standardPdErrors && selectedCompute === ComputeType.Standard) {
        errorDivs.push(summarizeErrors(standardPdErrors));
      } else if (
        standardDiskErrors &&
        (!runtimeCtx.enablePD || selectedCompute === ComputeType.Dataproc)
      ) {
        errorDivs.push(summarizeErrors(standardDiskErrors));
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
      ((runtimeChanged &&
        [RuntimeStatus.Running, RuntimeStatus.Stopped].includes(
          status as RuntimeStatus
        )) ||
        (pdExists && pdSizeReduced)) &&
      runtimeCanBeCreated;

    const renderUpdateButton = () => {
      return (
        <Button
          aria-label='Update'
          disabled={!runtimeCanBeUpdated}
          onClick={() => {
            setRequestedRuntime(createRuntimeRequest(newRuntimeConfig));
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
            setRequestedRuntime(createRuntimeRequest(newRuntimeConfig));
            onClose();
          }}
        >
          Create
        </Button>
      );
    };

    const renderTryAgainButton = () => {
      return (
        <Button
          aria-label='Try Again'
          disabled={!runtimeCanBeCreated}
          onClick={() => {
            setRequestedRuntime(createRuntimeRequest(newRuntimeConfig));
            onClose();
          }}
        >
          Try Again
        </Button>
      );
    };

    const renderNextButton = () => {
      return (
        <Button
          aria-label='Next'
          disabled={!runtimeCanBeUpdated}
          onClick={() => {
            setPanelContent(PanelContent.Confirm);
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
                  runtimeConfig={newRuntimeConfig}
                  runtimeCtx={runtimeCtx}
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
              if (runtimeCtx.enablePD && runtimeCtx.pdExists) {
                return (
                  <ConfirmDeleteRuntimeWithPD
                    onConfirm={async (runtimeStatusReq) => {
                      await setRuntimeStatus(runtimeStatusReq);
                      onClose();
                    }}
                    onCancel={() => setPanelContent(PanelContent.Customize)}
                    computeType={initialCompute}
                    pdSize={selectedPdSize}
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
                  await disksApi().deleteDisk(namespace, persistentDisk.name);
                  onClose();
                }}
                onCancel={() => setPanelContent(PanelContent.Customize)}
              />
            ),
          ],
          [
            PanelContent.Customize,
            () => (
              <Fragment>
                <div style={styles.controlSection}>
                  <FlexRow style={styles.costPredictorWrapper}>
                    <StartStopRuntimeButton
                      workspaceNamespace={workspace.namespace}
                      googleProject={workspace.googleProject}
                    />
                    <CostInfo
                      runtimeChanged={runtimeChanged}
                      runtimeConfig={newRuntimeConfig}
                      currentUser={profile.username}
                      workspace={workspace}
                      creatorFreeCreditsRemaining={creatorFreeCreditsRemaining}
                      runtimeCtx={runtimeCtx}
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
                    allowDataproc={allowDataproc}
                    disabled={disableControls}
                    setSelectedDiskSize={(disk) => setSelectedDiskSize(disk)}
                    setSelectedMachine={(machine) =>
                      setSelectedMachine(machine)
                    }
                    setSelectedCompute={(compute) =>
                      setSelectedCompute(compute)
                    }
                    setSelectedDataprocConfig={(dataproc) =>
                      setSelectedDataprocConfig(dataproc)
                    }
                  />
                  {/* Runtime customization: change detailed machine configuration options. */}
                  <h3 style={{ ...styles.sectionHeader, ...styles.bold }}>
                    Cloud compute profile
                  </h3>
                  <div style={styles.formGrid3}>
                    <MachineSelector
                      idPrefix='runtime'
                      disabled={disableControls}
                      selectedMachine={selectedMachine}
                      onChange={(value) => setSelectedMachine(value)}
                      validMachineTypes={validMainMachineTypes}
                      machineType={machineName}
                    />
                    {(!runtimeCtx.enablePD ||
                      selectedCompute !== ComputeType.Standard) && (
                      <DiskSizeSelector
                        idPrefix='runtime'
                        selectedDiskSize={selectedDiskSize}
                        onChange={(value) => {
                          setSelectedDiskSize(value);
                        }}
                        disabled={disableControls}
                        diskSize={diskSize}
                      />
                    )}
                  </div>
                  {enableGpu && selectedCompute === ComputeType.Standard && (
                    <GpuConfigSelector
                      disabled={disableControls}
                      onChange={(config) => {
                        setSelectedGpuConfig(config);
                      }}
                      selectedMachine={selectedMachine}
                      gpuConfig={selectedGpuConfig}
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
                          value={selectedCompute || ComputeType.Standard}
                          onChange={({ value }) => {
                            setSelectedCompute(value);
                          }}
                        />
                        {selectedCompute === ComputeType.Dataproc && (
                          <TooltipTrigger
                            content={
                              status !== RuntimeStatus.Running
                                ? 'Start your Dataproc cluster to access the Spark console'
                                : null
                            }
                          >
                            <LinkButton
                              data-test-id='manage-spark-console'
                              disabled={status !== RuntimeStatus.Running}
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
                  {selectedCompute === ComputeType.Dataproc && (
                    <DataProcConfigSelector
                      disabled={disableControls}
                      runtimeStatus={status}
                      dataprocExists={runtimeCtx.dataprocExists}
                      onChange={(config) => setSelectedDataprocConfig(config)}
                      dataprocConfig={selectedDataprocConfig}
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
                          selectedAutopauseThreshold ||
                          DEFAULT_AUTOPAUSE_THRESHOLD_MINUTES
                        }
                        onChange={({ value }) =>
                          setSelectedAutopauseThreshold(value)
                        }
                      />
                    </FlexColumn>
                  </FlexRow>
                  <FlexRow
                    style={{
                      justifyContent: 'space-between',
                      marginTop: '.75rem',
                    }}
                  >
                    {runtimeCtx.enablePD &&
                      selectedCompute === ComputeType.Standard && (
                        <div>
                          <PersistentDiskSizeSelector
                            selectedDiskSize={selectedPdSize}
                            onChange={(value) => {
                              setSelectedPdSize(value);
                            }}
                            disabled={disableControls}
                            diskSize={pdSize}
                          />{' '}
                        </div>
                      )}
                  </FlexRow>
                </div>
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
                {runtimeCtx.unattachedPdExists && !runtimeExists ? (
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
                    {!pdSizeReduced ? renderCreateButton() : renderNextButton()}
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
                      [
                        runtimeExists || (pdExists && pdSizeReduced),
                        () => renderNextButton(),
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
              </Fragment>
            ),
          ],
          [
            PanelContent.Confirm,
            () => (
              <ConfirmUpdatePanel
                initialRuntimeConfig={initialRuntimeConfig}
                newRuntimeConfig={newRuntimeConfig}
                onCancel={() => {
                  setPanelContent(PanelContent.Customize);
                }}
                updateButton={renderUpdateButton()}
                runtimeCtx={runtimeCtx}
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
