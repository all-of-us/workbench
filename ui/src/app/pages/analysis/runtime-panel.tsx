import {Button, Clickable, Link, MenuItem} from 'app/components/buttons';
import {FlexColumn, FlexRow} from 'app/components/flex';
import {ClrIcon} from 'app/components/icons';
import {PopupTrigger, TooltipTrigger} from 'app/components/popups';
import {Spinner} from 'app/components/spinners';
import {TextColumn} from 'app/components/text-column';
import {WarningMessage} from 'app/components/warning-message';

import {workspacesApi} from 'app/services/swagger-fetch-clients';
import colors, {addOpacity, colorWithWhiteness} from 'app/styles/colors';
import {
  reactStyles,
  switchCase,
  withCdrVersions,
  withCurrentWorkspace,
  withUserProfile
} from 'app/utils';
import {
  allMachineTypes,
  ComputeType,
  findMachineByName,
  Machine,
  machineRunningCost,
  machineRunningCostBreakdown,
  machineStorageCost,
  machineStorageCostBreakdown,
  validLeonardoMachineTypes
} from 'app/utils/machines';
import {formatUsd} from 'app/utils/numbers';
import {runtimePresets} from 'app/utils/runtime-presets';
import {
  getRuntimeConfigDiffs,
  RuntimeConfig,
  RuntimeDiffState,
  RuntimeStatusRequest,
  useCustomRuntime,
  useRuntimeStatus
} from 'app/utils/runtime-utils';
import {WorkspaceData} from 'app/utils/workspace-data';
import {
  BillingAccountType,
  CdrVersionListResponse,
  DataprocConfig,
  Runtime,
  RuntimeConfigurationType,
  RuntimeStatus
} from 'generated/fetch';
import * as fp from 'lodash/fp';

import {Dropdown} from 'primereact/dropdown';
import {InputNumber} from 'primereact/inputnumber';
import * as React from 'react';

const {useState, useEffect, Fragment} = React;

const styles = reactStyles({
  baseHeader: {
    color: colors.primary,
    fontSize: '16px',
    lineHeight: '1rem',
    margin: 0
  },
  sectionHeader: {
    marginBottom: '12px',
    marginTop: '12px'
  },
  bold: {
    fontWeight: 700,
  },
  controlSection: {
    backgroundColor: String(addOpacity(colors.white, .75)),
    borderRadius: '3px',
    padding: '.75rem',
    marginTop: '.75rem'
  },
  presetMenuItem: {
    color: colors.primary,
    fontSize: '14px'
  },
  formGrid: {
    display: 'grid',
    gridTemplateColumns: 'repeat(6, 1fr)',
    gridGap: '1rem',
    alignItems: 'center'
  },
  workerConfigLabel: {
    fontWeight: 600,
    marginBottom: '0.5rem'
  },
  inputNumber: {
    backgroundColor: colors.white,
    padding: '.75rem .5rem',
    width: '2rem'
  },
  errorMessage: {
    backgroundColor: colorWithWhiteness(colors.highlight, .5),
    marginTop: '0.5rem',
    color: colors.primary,
    fontSize: '14px',
    padding: '0.5rem',
    borderRadius: '0.5em'
  },
  costPredictor: {
    backgroundColor: colorWithWhiteness(colors.accent, 0.85),
    // Not using shorthand here because react doesn't like it when you mix shorthand and non-shorthand,
    // and the border color changes when the runtime does
    borderWidth: '1px',
    borderStyle: 'solid',
    borderColor: colorWithWhiteness(colors.dark, .5),
    borderRadius: '5px',
    color: colors.dark,
    marginBottom: '.5rem',
  },
  costsDrawnFrom: {
    borderLeft: `1px solid ${colorWithWhiteness(colors.dark, .5)}`,
    padding: '.33rem .5rem'
  },
  deleteLink: {
    alignSelf: 'center',
    fontSize: '16px',
    textTransform: 'uppercase'
  },
  confirmWarning: {
    backgroundColor: colorWithWhiteness(colors.warning, .9),
    border: `1px solid ${colors.warning}`,
    borderRadius: '5px',
    display: 'grid',
    gridColumnGap: '.4rem',
    gridRowGap: '.7rem',
    fontSize: '14px',
    fontWeight: 500,
    padding: '.5rem',
    marginTop: '1rem',
    marginBottom: '1rem'
  },
  confirmWarningText: {
    color: colors.primary,
    margin: 0
  }
});

const defaultMachineName = 'n1-standard-4';
const defaultMachineType: Machine = findMachineByName(defaultMachineName);
const defaultDiskSize = 50;

// Returns true if two runtimes are equivalent in terms of the fields which are
// affected by runtime presets.
const presetEquals = (a: Runtime, b: Runtime): boolean => {
  const strip = fp.flow(
    // In the future, things like toolDockerImage and autopause may be considerations.
    fp.pick(['gceConfig', 'dataprocConfig']),
    // numberOfWorkerLocalSSDs is currently part of the API spec, but is not used by the panel.
    fp.omit(['dataprocConfig.numberOfWorkerLocalSSDs']));
  return fp.isEqual(strip(a), strip(b));
};

enum PanelContent {
  Create = 'Create',
  Customize = 'Customize',
  Delete = 'Delete',
  Confirm = 'Confirm'
}

export interface Props {
  workspace: WorkspaceData;
  cdrVersionListResponse?: CdrVersionListResponse;
  onUpdate: () => void;
}

// Exported for testing only.
export const ConfirmDelete = ({onCancel, onConfirm}) => {
  const [deleting, setDeleting] = useState(false);
  return <Fragment>
    <div style={styles.confirmWarning}>
      <div style={{display: 'flex', justifyContent: 'center'}}>
        <ClrIcon style={{color: colors.warning, gridColumn: 1, gridRow: 1}} className='is-solid'
                 shape='exclamation-triangle' size='20'/>
      </div>
      <h3 style={{...styles.baseHeader, ...styles.bold, gridColumn: 2, gridRow: 1}}>Delete your environment</h3>
      <p style={{...styles.confirmWarningText, gridColumn: 2, gridRow: 2}}>
        You’re about to delete your cloud analysis environment.
      </p>
      <p style={{...styles.confirmWarningText, gridColumn: 2, gridRow: 3}}>
        Any in-memory state and local file modifications will be erased.&nbsp;
        Data stored in workspace buckets is never affected by changes to your cloud&nbsp;
        environment. You’ll still be able to view notebooks in this workspace, but&nbsp;
        editing and running notebooks will require you to create a new cloud environment.
      </p>
    </div>
    <FlexRow style={{justifyContent: 'flex-end'}}>
      <Button
        type='secondaryLight'
        aria-label={'Cancel'}
        disabled={deleting}
        style={{marginRight: '.6rem'}}
        onClick={() => onCancel()}>
        Cancel
      </Button>
      <Button
        aria-label={'Delete'}
        disabled={deleting}
        onClick={async() => {
          setDeleting(true);
          try {
            await onConfirm();
          } catch (err) {
            setDeleting(false);
            throw err;
          }
        }}>
        Delete
      </Button>
    </FlexRow>
  </Fragment>;
};

const MachineSelector = ({onChange, selectedMachine, machineType, disabled, idPrefix}) => {
  const initialMachineType = fp.find(({name}) => name === machineType, allMachineTypes) || defaultMachineType;
  const {cpu, memory} = selectedMachine || initialMachineType;

  return <Fragment>
      <label htmlFor={`${idPrefix}-cpu`}>CPUs</label>
      <Dropdown id={`${idPrefix}-cpu`}
        options={fp.flow(
          // Show all CPU options.
          fp.map('cpu'),
          // In the event that was remove a machine type from our set of valid
          // configs, we want to continue to allow rendering of the value here.
          // Union also makes the CPU values unique.
          fp.union([cpu]),
          fp.sortBy(fp.identity)
        )(validLeonardoMachineTypes)}
        onChange={
          ({value}) => fp.flow(
            fp.sortBy('memory'),
            fp.find({cpu: value}),
            onChange)(validLeonardoMachineTypes)
        }
        disabled={disabled}
        value={cpu}/>
      <label htmlFor={`${idPrefix}-ram`}>RAM (GB)</label>
      <Dropdown id={`${idPrefix}-ram`}
        options={fp.flow(
          // Show valid memory options as constrained by the currently selected CPU.
          fp.filter(({cpu: availableCpu}) => availableCpu === cpu),
          fp.map('memory'),
          // See above comment on CPU union.
          fp.union([memory]),
          fp.sortBy(fp.identity)
        )(validLeonardoMachineTypes)}
        onChange={
          ({value}) => fp.flow(
            fp.find({cpu, memory: value}),
            // If the selected machine is not different from the current machine return null
            // maybeGetMachine,
            onChange
            )(validLeonardoMachineTypes) }
        disabled={disabled}
        value={memory}
        />
  </Fragment>;
};

const DiskSizeSelector = ({onChange, disabled, selectedDiskSize, diskSize, idPrefix}) => {
  return <Fragment>
    <label htmlFor={`${idPrefix}-disk`}>Disk (GB)</label>
    <InputNumber id={`${idPrefix}-disk`}
      showButtons
      disabled={disabled}
      decrementButtonClassName='p-button-secondary'
      incrementButtonClassName='p-button-secondary'
      value={selectedDiskSize || diskSize}
      inputStyle={styles.inputNumber}
      onChange={({value}) => onChange(value)}
      min={50 /* Runtime API has a minimum 50GB requirement. */}/>
  </Fragment>;
};

const DataProcConfigSelector = ({onChange, disabled, dataprocConfig})  => {
  const {
    workerMachineType = defaultMachineName,
    workerDiskSize = 50,
    numberOfWorkers = 2,
    numberOfPreemptibleWorkers = 0
  } = dataprocConfig || {};
  const initialMachine = findMachineByName(workerMachineType);
  const [selectedNumWorkers, setSelectedNumWorkers] = useState<number>(numberOfWorkers);
  const [selectedPreemtible, setSelectedPreemptible] = useState<number>(numberOfPreemptibleWorkers);
  const [selectedWorkerMachine, setSelectedWorkerMachine] = useState<Machine>(initialMachine);
  const [selectedDiskSize, setSelectedDiskSize] = useState<number>(workerDiskSize);

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
      workerMachineType: selectedWorkerMachine && selectedWorkerMachine.name,
      workerDiskSize: selectedDiskSize,
      numberOfWorkers: selectedNumWorkers,
      numberOfPreemptibleWorkers: selectedPreemtible
    });
  }, [selectedNumWorkers, selectedPreemtible, selectedWorkerMachine, selectedDiskSize]);

  return <fieldset style={{marginTop: '0.75rem'}}>
    <legend style={styles.workerConfigLabel}>Worker Config</legend>
    <div style={styles.formGrid}>
      <label htmlFor='num-workers'>Workers</label>
      <InputNumber id='num-workers'
        showButtons
        disabled={disabled}
        decrementButtonClassName='p-button-secondary'
        incrementButtonClassName='p-button-secondary'
        value={selectedNumWorkers}
        inputStyle={styles.inputNumber}
        onChange={({value}) => setSelectedNumWorkers(value)}
        min={2}/>
      <label htmlFor='num-preemptible'>Preemptible</label>
      <InputNumber id='num-preemptible'
        showButtons
        disabled={disabled}
        decrementButtonClassName='p-button-secondary'
        incrementButtonClassName='p-button-secondary'
        value={selectedPreemtible}
        inputStyle={styles.inputNumber}
        onChange={({value}) => setSelectedPreemptible(value)}
        min={0}/>
      <div style={{gridColumnEnd: 'span 2'}}/>
      {/* TODO: Do the worker nodes have the same minimum requirements as the master node?
       to https://precisionmedicineinitiative.atlassian.net/browse/RW-5763 */}
      <MachineSelector
        machineType={workerMachineType}
        onChange={setSelectedWorkerMachine}
        selectedMachine={selectedWorkerMachine}
        disabled={disabled}
        idPrefix='worker'/>
      <DiskSizeSelector
        diskSize={workerDiskSize}
        onChange={setSelectedDiskSize}
        selectedDiskSize={selectedDiskSize}
        disabled={disabled}
        idPrefix='worker'/>
    </div>
  </fieldset>;
};

// Select a recommended preset configuration.
const PresetSelector = ({
  hasMicroarrayData, setSelectedDiskSize, setSelectedMachine,
  setSelectedCompute, setSelectedDataprocConfig, disabled}) => {
  return <PopupTrigger side='bottom'
                closeOnClick
                disabled={disabled}
                content={
                  <React.Fragment>
                    {
                      fp.flow(
                        fp.filter(({runtimeTemplate}) => hasMicroarrayData || !runtimeTemplate.dataprocConfig),
                        fp.toPairs,
                        fp.map(([i, preset]) => {
                          return <MenuItem
                                style={styles.presetMenuItem}
                                key={i}
                                aria-label={preset.displayName}
                                onClick={() => {
                                  // renaming to avoid shadowing
                                  const {runtimeTemplate} = preset;
                                  const {presetDiskSize, presetMachineName, presetCompute} = fp.cond([
                                    // Can't destructure due to shadowing.
                                    [() => !!runtimeTemplate.gceConfig, (tmpl: Runtime) => ({
                                      presetDiskSize: tmpl.gceConfig.diskSize,
                                      presetMachineName: tmpl.gceConfig.machineType,
                                      presetCompute: ComputeType.Standard
                                    })],
                                    [() => !!runtimeTemplate.dataprocConfig, ({dataprocConfig: {masterDiskSize, masterMachineType}}) => ({
                                      presetDiskSize: masterDiskSize,
                                      presetMachineName: masterMachineType,
                                      presetCompute: ComputeType.Dataproc
                                    })]
                                  ])(runtimeTemplate);
                                  const presetMachineType = fp.find(({name}) => name === presetMachineName, validLeonardoMachineTypes);

                                  setSelectedDiskSize(presetDiskSize);
                                  setSelectedMachine(presetMachineType);
                                  setSelectedCompute(presetCompute);
                                  setSelectedDataprocConfig(runtimeTemplate.dataprocConfig);
                                }}>
                              {preset.displayName}
                            </MenuItem>;
                        })
                      )(runtimePresets)
                    }
                  </React.Fragment>
                }>
    {/* inline-block aligns the popup menu beneath the clickable content, rather than the middle of the panel */}
    <Clickable
      disabled={disabled}
      data-test-id='runtime-presets-menu'
      style={{display: 'inline-block', ...(disabled ?
        {color: colorWithWhiteness(colors.dark, .4)} : {})}}>
      Recommended environments <ClrIcon shape='caret down'/>
    </Clickable>
  </PopupTrigger>;
};

const CostEstimator = ({
  runtimeParameters,
  costTextColor = colors.accent
}) => {
  const {
    computeType,
    diskSize,
    machine,
    dataprocConfig
  } = runtimeParameters;
  const {
    numberOfWorkers = 0,
    masterMachineType = machine.name,
    masterDiskSize = diskSize,
    workerMachineType = null,
    workerDiskSize = null,
    numberOfPreemptibleWorkers = 0
  } = dataprocConfig || {};
  const masterMachine = findMachineByName(masterMachineType);
  const workerMachine = findMachineByName(workerMachineType);

  const costConfig = {
    computeType, masterDiskSize, masterMachine,
    numberOfWorkers, numberOfPreemptibleWorkers, workerDiskSize, workerMachine
  };
  const runningCost = machineRunningCost(costConfig);
  const runningCostBreakdown = machineRunningCostBreakdown(costConfig);
  const storageCost = machineStorageCost(costConfig);
  const storageCostBreakdown = machineStorageCostBreakdown(costConfig);

  return <FlexRow>
      <FlexColumn style={{marginRight: '1rem'}}>
        <div style={{fontSize: '10px', fontWeight: 600}}>Cost when running</div>
        <TooltipTrigger content={
          <div>
            <div>Cost Breakdown</div>
            {runningCostBreakdown.map((lineItem, i) => <div key={i}>{lineItem}</div>)}
          </div>
        }>
          <div
              style={{fontSize: '20px', color: costTextColor}}
              data-test-id='running-cost'
          >
            {formatUsd(runningCost)}/hr
          </div>
        </TooltipTrigger>
      </FlexColumn>
      <FlexColumn>
        <div style={{fontSize: '10px', fontWeight: 600}}>Cost when paused</div>
        <TooltipTrigger content={
          <div>
            <div>Cost Breakdown</div>
            {storageCostBreakdown.map((lineItem, i) => <div key={i}>{lineItem}</div>)}
          </div>
        }>
          <div
              style={{fontSize: '20px', color: costTextColor}}
              data-test-id='storage-cost'
          >
            {formatUsd(storageCost)}/hr
          </div>
        </TooltipTrigger>
      </FlexColumn>
  </FlexRow>;
};

const CostInfo = ({runtimeChanged, runtimeConfig, currentUser, workspace, creatorFreeCreditsRemaining}) => {
  return <FlexRow
    style={
      runtimeChanged
        ? {...styles.costPredictor, backgroundColor: colorWithWhiteness(colors.warning, .9), borderColor: colors.warning}
        : styles.costPredictor
    }
    data-test-id='cost-estimator'
  >
    <div style={{minWidth: '250px', margin: '.33rem .5rem'}}>
      <CostEstimator runtimeParameters={runtimeConfig}/>
    </div>
    {
      workspace.billingAccountType === BillingAccountType.FREETIER
      && currentUser === workspace.creator
      && <div style={styles.costsDrawnFrom}>
        Costs will draw from your remaining {formatUsd(creatorFreeCreditsRemaining)} of free credits.
      </div>
    }
    {
      workspace.billingAccountType === BillingAccountType.FREETIER
      && currentUser !== workspace.creator
      && <div style={styles.costsDrawnFrom}>
        Costs will draw from workspace creator's remaining {formatUsd(creatorFreeCreditsRemaining)} of free credits.
      </div>
    }
    {
      workspace.billingAccountType === BillingAccountType.USERPROVIDED
      && <div style={styles.costsDrawnFrom}>
        Costs will be charged to billing account {workspace.billingAccountName}.
      </div>
    }
  </FlexRow>;
};

const CreatePanel = ({creatorFreeCreditsRemaining, preset, profile, setPanelContent, workspace, runtimeConfig}) => {
  const {
    displayName,
    runtimeTemplate: {gceConfig = null, dataprocConfig = null}
  } = preset;
  const computeType = dataprocConfig ? ComputeType.Dataproc : ComputeType.Standard;
  const masterMachineType = computeType === ComputeType.Dataproc ? dataprocConfig.masterMachineType : gceConfig.machineType;
  const masterMachine = findMachineByName(masterMachineType);
  const masterDiskSize = computeType === ComputeType.Dataproc ? dataprocConfig.masterDiskSize : gceConfig.diskSize;
  const workerMachineType = computeType === ComputeType.Dataproc ? dataprocConfig.workerMachineType : null;
  const workerMachine = workerMachineType ? findMachineByName(workerMachineType) : null;
  const workerDiskSize = computeType === ComputeType.Dataproc ? dataprocConfig.workerDiskSize : null;

  return <div style={styles.controlSection}>
    <CostInfo runtimeChanged={false}
              runtimeConfig={runtimeConfig}
              currentUser={profile.username}
              workspace={workspace}
              creatorFreeCreditsRemaining={creatorFreeCreditsRemaining}
    />
    <FlexRow style={{justifyContent: 'space-between', alignItems: 'center'}}>
      <h3 style={{...styles.sectionHeader, ...styles.bold}}>Recommended Environment for {displayName}</h3>
      <Button
          type='secondarySmall'
          onClick={() => setPanelContent(PanelContent.Customize)}
          aria-label='Customize'
      >
        Customize
      </Button>
    </FlexRow>
    <label htmlFor='compute-resources' style={{...styles.bold, marginTop: '1rem'}}>Compute Resources</label>
    <div id='compute-resources'>- Default: compute size of
      <b> {masterMachine.cpu} CPUs</b>,
      <b> {masterMachine.memory} GB memory</b>, and a
      <b> {masterDiskSize} GB disk</b>
    </div>
    {computeType === ComputeType.Dataproc && <Fragment>
      <label htmlFor='worker-configuration' style={{...styles.bold, marginTop: '1rem'}}>Worker Configuration</label>
      <div id='worker-configuration'>- Default:
        <b> {dataprocConfig.numberOfWorkers} worker(s) </b>
        {
          dataprocConfig.numberOfPreemptibleWorkers > 0 &&
          <b>and {dataprocConfig.numberOfPreemptibleWorkers}  preemptible worker(s) </b>
        }
        each with compute size of <b>{workerMachine.cpu} CPUs</b>,
        <b> {workerMachine.memory} GB memory</b>, and a
        <b> {workerDiskSize} GB disk</b>
      </div>
    </Fragment>}
  </div>;
};

const ConfirmUpdatePanel = ({initialRuntimeConfig, newRuntimeConfig, onCancel, updateButton}) => {
  const runtimeDiffs = getRuntimeConfigDiffs(initialRuntimeConfig, newRuntimeConfig);
  const needsDelete = runtimeDiffs.map(diff => diff.differenceType).includes(RuntimeDiffState.NEEDS_DELETE);

  return <React.Fragment>
    <div style={styles.controlSection}>
      <h3 style={{...styles.baseHeader, ...styles.sectionHeader, marginTop: '.1rem', marginBottom: '.2rem'}}>Editing your environment</h3>
      <div>
        You're about to apply the following changes to your environment:
      </div>
      <ul>
        {runtimeDiffs.map((diff, i) =>
          <li key={i}>
            {diff.desc} from <b>{diff.previous}</b> to <b>{diff.new}</b>
          </li>
        )}
      </ul>
      <FlexRow style={{marginTop: '.5rem'}}>
        <div style={{marginRight: '1rem'}}>
          <b style={{fontSize: 10}}>New estimated cost</b>
          <div style={{...styles.costPredictor, padding: '.25rem .5rem'}}>
            <CostEstimator runtimeParameters={newRuntimeConfig}/>
          </div>
        </div>
        <div>
          <b style={{fontSize: 10}}>Previous estimated cost</b>
          <div style={{...styles.costPredictor,
            padding: '.25rem .5rem',
            color: 'grey',
            backgroundColor: ''}}>
            <CostEstimator runtimeParameters={initialRuntimeConfig} costTextColor='grey'/>
          </div>
        </div>
      </FlexRow>
    </div>

    <WarningMessage>
      <TextColumn>
        {needsDelete ? <React.Fragment>
          <div>
            You've made changes that can only take effect upon deletion and re-creation of
            your cloud environment.
          </div>
          <div style={{marginTop: '0.5rem'}}>
            Any in-memory state and local file modifications will be erased. Data stored in
            workspace buckets is never affected by changes to your cloud environment.
          </div>
        </React.Fragment> : <React.Fragment>
          <div>
            These changes require a reboot of your environment to take effect.
          </div>
          <div style={{marginTop: '0.5rem'}}>
            Any in-memory state will be erased, but local file modifications will be preserved.
            Data stored in workspace buckets is never affected by changes to your cloud environment.
          </div>
        </React.Fragment>}
      </TextColumn>
    </WarningMessage>

    <FlexRow style={{justifyContent: 'flex-end', marginTop: '.75rem'}}>
      <Button
        type='secondary'
        aria-label='Cancel'
        style={{marginRight: '.25rem'}}
        onClick={onCancel}>
        Cancel
      </Button>
      {updateButton}
    </FlexRow>
  </React.Fragment>;
};

export const RuntimePanel = fp.flow(
  withCdrVersions(),
  withCurrentWorkspace(),
  withUserProfile()
)(({cdrVersionListResponse, workspace, profileState, onUpdate = () => {}}) => {
  const {namespace, id, cdrVersionId} = workspace;

  const {profile} = profileState;

  const {hasMicroarrayData} = fp.find({cdrVersionId}, cdrVersionListResponse.items) || {hasMicroarrayData: false};
  const [{currentRuntime, pendingRuntime}, setRequestedRuntime] = useCustomRuntime(namespace);

  // Prioritize the "pendingRuntime", if any. When an update is pending, we want
  // to render the target runtime details, which  may not match the current runtime.
  const {dataprocConfig = null, gceConfig = {diskSize: defaultDiskSize}} = pendingRuntime || currentRuntime || {} as Partial<Runtime>;
  const [status, setRuntimeStatus] = useRuntimeStatus(namespace);
  const diskSize = dataprocConfig ? dataprocConfig.masterDiskSize : gceConfig.diskSize;
  const machineName = dataprocConfig ? dataprocConfig.masterMachineType : gceConfig.machineType;
  const initialMasterMachine = findMachineByName(machineName) || defaultMachineType;
  const initialCompute = dataprocConfig ? ComputeType.Dataproc : ComputeType.Standard;

  // We may encounter a race condition where an existing current runtime has not loaded by the time this panel renders.
  // It's unclear how often that would actually happen.
  const initialPanelContent = fp.cond([
    // currentRuntime being undefined means the first `getRuntime` has still not completed.
    [([r, ]) => r === undefined, () => PanelContent.Customize],
    [([r, s]) => r === null || s === RuntimeStatus.Unknown, () => PanelContent.Create],
    [() => true, () => PanelContent.Customize]
  ])([currentRuntime, status]);
  const [panelContent, setPanelContent] = useState<PanelContent>(initialPanelContent);

  const initialPreset = runtimePresets.generalAnalysis;

  const [selectedDiskSize, setSelectedDiskSize] = useState(diskSize);
  const [selectedMachine, setSelectedMachine] = useState(initialMasterMachine);
  const [selectedCompute, setSelectedCompute] = useState<ComputeType>(initialCompute);
  const [selectedDataprocConfig, setSelectedDataprocConfig] = useState<DataprocConfig | null>(dataprocConfig);

  const runtimeExists = (status && ![RuntimeStatus.Deleted, RuntimeStatus.Error].includes(status)) || !!pendingRuntime;
  const disableControls = !!status && ![RuntimeStatus.Deleted, RuntimeStatus.Running, RuntimeStatus.Stopped].includes(status as RuntimeStatus) || !!pendingRuntime;

  const initialRuntimeConfig = {
    computeType: initialCompute,
    machine: initialMasterMachine,
    diskSize: diskSize,
    dataprocConfig: dataprocConfig
  };

  const newRuntimeConfig = {
    computeType: selectedCompute,
    machine: selectedMachine,
    diskSize: selectedDiskSize,
    dataprocConfig: selectedDataprocConfig
  };

  const runtimeDiffs = getRuntimeConfigDiffs(initialRuntimeConfig, newRuntimeConfig);
  const runtimeChanged = runtimeExists && runtimeDiffs.length > 0;
  const needsDelete = runtimeDiffs.map(diff => diff.differenceType).includes(RuntimeDiffState.NEEDS_DELETE);

  const [creatorFreeCreditsRemaining, setCreatorFreeCreditsRemaining] = useState(0);
  useEffect(() => {
    const aborter = new AbortController();
    const fetchFreeCredits = async() => {
      const {freeCreditsRemaining} = await workspacesApi().getWorkspaceCreatorFreeCreditsRemaining(namespace, id, {signal: aborter.signal});
      setCreatorFreeCreditsRemaining(freeCreditsRemaining);
    };

    fetchFreeCredits();

    return function cleanup() {
      aborter.abort();
    };
  }, []);

  if (currentRuntime === undefined) {
    return <Spinner style={{width: '100%', marginTop: '5rem'}}/>;
  }

  const createRuntimeRequest = (runtime: RuntimeConfig) => {
    const runtimeRequest: Runtime = runtime.computeType === ComputeType.Dataproc ? {
      dataprocConfig: {
        ...runtime.dataprocConfig,
        masterMachineType: runtime.machine.name,
        masterDiskSize: runtime.diskSize
      }
    } : runtime.computeType === ComputeType.Standard ? {
      gceConfig: {
        machineType: runtime.machine.name,
        diskSize: runtime.diskSize
      }
    } : null;

    // If the selected runtime matches a preset, plumb through the appropriate configuration type.
    runtimeRequest.configurationType = fp.get(
      'runtimeTemplate.configurationType',
      fp.find(
        ({runtimeTemplate}) => presetEquals(runtimeRequest, runtimeTemplate),
        runtimePresets)
    ) || RuntimeConfigurationType.UserOverride;

    return runtimeRequest;
  };
  // Casting to RuntimeStatus here because it can't easily be done at the destructuring level
  // where we get 'status' from
  const runtimeCanBeUpdated = runtimeChanged && [RuntimeStatus.Running, RuntimeStatus.Stopped].includes(status as RuntimeStatus);

  const renderUpdateButton = () => {
    return <Button
      aria-label='Update'
      disabled={!runtimeCanBeUpdated}
      onClick={() => {
        setRequestedRuntime(createRuntimeRequest(newRuntimeConfig));
        onUpdate();
      }}>
      {needsDelete ? 'APPLY & RECREATE' : 'APPLY & REBOOT'}
    </Button>;
  };

  const renderCreateButton = () => {
    return <Button
      aria-label='Create'
      onClick={() => {
        setRequestedRuntime(createRuntimeRequest(newRuntimeConfig));
        onUpdate();
      }}>
      Create
    </Button>;
  };

  const renderNextButton = () => {
    return <Button
      aria-label='Next'
      disabled={!runtimeCanBeUpdated}
      onClick={() => {
        setPanelContent(PanelContent.Confirm);
      }}>
      Next
    </Button>;
  };

  return <div data-test-id='runtime-panel'>
    <h3 style={{...styles.baseHeader, ...styles.bold, ...styles.sectionHeader}}>Cloud analysis environment</h3>
    <div>
      Your analysis environment consists of an application and compute resources.
      Your cloud environment is unique to this workspace and not shared with other users.
    </div>

    {switchCase(panelContent,
      [PanelContent.Create, () =>
        <Fragment>
          <CreatePanel
              creatorFreeCreditsRemaining={creatorFreeCreditsRemaining}
              preset={initialPreset}
              profile={profile}
              setPanelContent={(value) => setPanelContent(value)}
              workspace={workspace}
              runtimeConfig={newRuntimeConfig}
          />
          <FlexRow style={{justifyContent: 'flex-end', marginTop: '1rem'}}>
            {renderCreateButton()}
          </FlexRow>
        </Fragment>
      ],
      [PanelContent.Delete, () => <ConfirmDelete
        onConfirm={async() => {
          await setRuntimeStatus(RuntimeStatusRequest.Delete);
          setPanelContent(PanelContent.Customize);
        }}
        onCancel={() => setPanelContent(PanelContent.Customize)}
      />],
      [PanelContent.Customize, () => <Fragment>
        <div style={styles.controlSection}>
          <CostInfo runtimeChanged={runtimeChanged}
                                runtimeConfig={newRuntimeConfig}
                                currentUser={profile.username}
                                workspace={workspace}
                                creatorFreeCreditsRemaining={creatorFreeCreditsRemaining}
          />

          <PresetSelector
            hasMicroarrayData={hasMicroarrayData}
            disabled={disableControls}
            setSelectedDiskSize={(disk) => setSelectedDiskSize(disk)}
            setSelectedMachine={(machine) => setSelectedMachine(machine)}
            setSelectedCompute={(compute) => setSelectedCompute(compute)}
            setSelectedDataprocConfig={(dataproc) => setSelectedDataprocConfig(dataproc)}
          />
          {/* Runtime customization: change detailed machine configuration options. */}
          <h3 style={styles.sectionHeader}>Cloud compute profile</h3>
          <div style={styles.formGrid}>
            <MachineSelector
              idPrefix='runtime'
              disabled={disableControls}
              selectedMachine={selectedMachine}
              onChange={(value) => setSelectedMachine(value)}
              machineType={machineName}/>
            <DiskSizeSelector
                idPrefix='runtime'
                selectedDiskSize={selectedDiskSize}
                onChange={(value) => setSelectedDiskSize(value)}
                disabled={disableControls}
                diskSize={diskSize}/>
         </div>
         <FlexColumn style={{marginTop: '1rem'}}>
           <label htmlFor='runtime-compute'>Compute type</label>
           <Dropdown id='runtime-compute'
                     disabled={!hasMicroarrayData || disableControls}
                     style={{width: '10rem'}}
                     options={[ComputeType.Standard, ComputeType.Dataproc]}
                     value={selectedCompute || ComputeType.Standard}
                     onChange={({value}) => {setSelectedCompute(value); }}
                     />
           {
             selectedCompute === ComputeType.Dataproc &&
             <DataProcConfigSelector
               disabled={disableControls}
               onChange={config => setSelectedDataprocConfig(config)}
               dataprocConfig={selectedDataprocConfig} />
           }
         </FlexColumn>
       </div>
       {runtimeExists && runtimeChanged &&
         <WarningMessage>
            <div>You've made changes that require recreating your environment to take effect.</div>
         </WarningMessage>
       }
       <FlexRow style={{justifyContent: 'space-between', marginTop: '.75rem'}}>
         <Link
           style={{...styles.deleteLink, ...(
             (disableControls || !runtimeExists) ?
             {color: colorWithWhiteness(colors.dark, .4)} : {}
           )}}
           aria-label='Delete Environment'
           disabled={disableControls || !runtimeExists}
           onClick={() => setPanelContent(PanelContent.Delete)}>Delete Environment</Link>
         {!runtimeExists ? renderCreateButton() : renderNextButton()}
       </FlexRow>
     </Fragment>],
      [PanelContent.Confirm, () => <ConfirmUpdatePanel initialRuntimeConfig={initialRuntimeConfig}
                                                       newRuntimeConfig={newRuntimeConfig}
                                                       onCancel={() => {
                                                         setPanelContent(PanelContent.Customize);
                                                       }}
                                                       updateButton={renderUpdateButton()}
      />])}
  </div>;
});
