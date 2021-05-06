import {Button, Clickable, Link} from 'app/components/buttons';
import {FlexColumn, FlexRow} from 'app/components/flex';
import {ClrIcon} from 'app/components/icons';
import {ErrorMessage, WarningMessage} from 'app/components/messages';
import {TooltipTrigger} from 'app/components/popups';
import {Spinner} from 'app/components/spinners';
import {TextColumn} from 'app/components/text-column';

import {workspacesApi} from 'app/services/swagger-fetch-clients';
import colors, {addOpacity, colorWithWhiteness} from 'app/styles/colors';
import {
  DEFAULT,
  reactStyles,
  summarizeErrors,
  switchCase,
  withCdrVersions,
  withCurrentWorkspace,
  withUserProfile
} from 'app/utils';
import {
  ComputeType,
  findMachineByName,
  Machine,
  machineRunningCost,
  machineRunningCostBreakdown,
  machineStorageCost,
  machineStorageCostBreakdown,
  validLeoDataprocMasterMachineTypes,
  validLeoDataprocWorkerMachineTypes,
  validLeoGceMachineTypes
} from 'app/utils/machines';
import {formatUsd} from 'app/utils/numbers';
import {applyPresetOverride, runtimePresets} from 'app/utils/runtime-presets';
import {
  getRuntimeConfigDiffs,
  RuntimeConfig,
  RuntimeDiffState,
  RuntimeStatusRequest,
  useCustomRuntime,
  useRuntimeStatus
} from 'app/utils/runtime-utils';
import {WorkspaceData} from 'app/utils/workspace-data';

import {AoU} from 'app/components/text-wrappers';
import {findCdrVersion} from 'app/utils/cdr-versions';
import {
  BillingAccountType,
  BillingStatus,
  CdrVersionTiersResponse,
  DataprocConfig,
  Runtime,
  RuntimeConfigurationType,
  RuntimeStatus
} from 'generated/fetch';
import * as fp from 'lodash/fp';
import {Dropdown} from 'primereact/dropdown';
import {InputNumber} from 'primereact/inputnumber';
import * as React from 'react';
import {validate} from 'validate.js';

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
  label: {
    fontWeight: 600,
    marginRight: '.5rem'
  },
  labelAndInput: {
    alignItems: 'center',
  },
  controlSection: {
    backgroundColor: String(addOpacity(colors.white, .75)),
    borderRadius: '3px',
    padding: '.75rem'
  },
  presetMenuItem: {
    color: colors.primary,
    fontSize: '14px'
  },
  formGrid: {
    display: 'grid',
    gridTemplateColumns: 'repeat(3, 1fr)',
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
  costPredictorWrapper: {
    backgroundColor: colorWithWhiteness(colors.accent, 0.85),
    // Not using shorthand here because react doesn't like it when you mix shorthand and non-shorthand,
    // and the border color changes when the runtime does
    borderWidth: '1px',
    borderStyle: 'solid',
    borderColor: colorWithWhiteness(colors.dark, .5),
    borderRadius: '5px',
    color: colors.dark
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
  Disabled = 'Disabled',
  Confirm = 'Confirm'
}

// this is only used in the test.
export interface Props {
  workspace: WorkspaceData;
  cdrVersionTiersResponse?: CdrVersionTiersResponse;
  onClose: () => void;
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

const MachineSelector = ({
  onChange,
  selectedMachine,
  machineType,
  disabled,
  idPrefix,
  validMachineTypes,
  cpuLabelStyles = {},
  ramLabelStyles = {}}
) => {
  const initialMachineType = findMachineByName(machineType) || defaultMachineType;
  const {cpu, memory} = selectedMachine || initialMachineType;

  return <Fragment>
    <FlexRow style={styles.labelAndInput}>
      <label style={{...styles.label, ...cpuLabelStyles}} htmlFor={`${idPrefix}-cpu`}>CPUs</label>
      <Dropdown id={`${idPrefix}-cpu`}
                options={fp.flow(
                    // Show all CPU options.
                  fp.map('cpu'),
                    // In the event that was remove a machine type from our set of valid
                    // configs, we want to continue to allow rendering of the value here.
                    // Union also makes the CPU values unique.
                  fp.union([cpu]),
                  fp.sortBy(fp.identity)
                )(validMachineTypes)}
                onChange={
                  ({value}) => fp.flow(
                    fp.sortBy('memory'),
                    fp.find({cpu: value}),
                    onChange)(validMachineTypes)
                }
                disabled={disabled}
                value={cpu}/>
    </FlexRow>
    <FlexRow style={styles.labelAndInput}>
      <label style={{...styles.label, ...ramLabelStyles}} htmlFor={`${idPrefix}-ram`}>RAM (GB)</label>
      <Dropdown id={`${idPrefix}-ram`}
        options={fp.flow(
          // Show valid memory options as constrained by the currently selected CPU.
          fp.filter(({cpu: availableCpu}) => availableCpu === cpu),
          fp.map('memory'),
          // See above comment on CPU union.
          fp.union([memory]),
          fp.sortBy(fp.identity)
        )(validMachineTypes)}
        onChange={
          ({value}) => fp.flow(
            fp.find({cpu, memory: value}),
            // If the selected machine is not different from the current machine return null
            // maybeGetMachine,
            onChange
            )(validMachineTypes)}
        disabled={disabled}
        value={memory}
        />
    </FlexRow>
  </Fragment>;
};

const DisabledPanel = () => {
  return <WarningMessage
        data-test-id='runtime-disabled-panel'
        iconSize={16}
        iconPosition={'top'}
    >
      {
        <TextColumn>
          <div style={{fontWeight: 600}}>Cloud services are disabled for this workspace.</div>
          <div style={{marginTop: '0.5rem'}}>
            You cannot run or edit notebooks in this workspace because billed services are disabled
            for the workspace creator's <AoU/> Researcher account.
          </div>
        </TextColumn>
      }
    </WarningMessage>;
};

const DiskSizeSelector = ({onChange, disabled, selectedDiskSize, diskSize, idPrefix}) => {
  return <FlexRow  style={styles.labelAndInput}>
      <label style={styles.label} htmlFor={`${idPrefix}-disk`}>Disk (GB)</label>
      <InputNumber id={`${idPrefix}-disk`}
                   showButtons
                   disabled={disabled}
                   decrementButtonClassName='p-button-secondary'
                   incrementButtonClassName='p-button-secondary'
                   value={selectedDiskSize || diskSize}
                   inputStyle={styles.inputNumber}
                   onChange={({value}) => onChange(value)}
      />
    </FlexRow>;
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
    <legend style={styles.workerConfigLabel}>Worker Configuration</legend>
    <div style={styles.formGrid}>
      <FlexRow style={styles.labelAndInput}>
        <label style={styles.label} htmlFor='num-workers'>Workers</label>
        <InputNumber id='num-workers'
          showButtons
          disabled={disabled}
          decrementButtonClassName='p-button-secondary'
          incrementButtonClassName='p-button-secondary'
          value={selectedNumWorkers}
          inputStyle={styles.inputNumber}
          onChange={({value}) => setSelectedNumWorkers(value)}
          min={2}/>
      </FlexRow>
      <FlexRow style={styles.labelAndInput}>
        <label style={styles.label} htmlFor='num-preemptible'>Preemptible</label>
        <InputNumber id='num-preemptible'
          showButtons
          disabled={disabled}
          decrementButtonClassName='p-button-secondary'
          incrementButtonClassName='p-button-secondary'
          value={selectedPreemtible}
          inputStyle={styles.inputNumber}
          onChange={({value}) => setSelectedPreemptible(value)}
          min={0}/>
      </FlexRow>
      <div style={{gridColumnEnd: 'span 1'}}/>
      <MachineSelector
        machineType={workerMachineType}
        onChange={setSelectedWorkerMachine}
        selectedMachine={selectedWorkerMachine}
        disabled={disabled}
        validMachineTypes={validLeoDataprocWorkerMachineTypes}
        idPrefix='worker'
        cpuLabelStyles={{minWidth: '2.5rem'}} // width of 'Workers' label above
        ramLabelStyles={{minWidth: '3.75rem'}} // width of 'Preemptible' label above
      />
      <DiskSizeSelector
        diskSize={workerDiskSize}
        onChange={setSelectedDiskSize}
        selectedDiskSize={selectedDiskSize}
        disabled={disabled}
        idPrefix='worker'
      />
    </div>
  </fieldset>;
};

// Select a recommended preset configuration.
const PresetSelector = ({
  hasMicroarrayData, setSelectedDiskSize, setSelectedMachine,
  setSelectedCompute, setSelectedDataprocConfig, disabled}) => {
  return <Dropdown
    id='runtime-presets-menu'
    disabled={disabled}
    style={{marginTop: '21px', display: 'inline-block', color: colors.primary}}
    placeholder='Recommended environments'
    options={fp.flow(
      fp.values,
      fp.filter(({runtimeTemplate}) => hasMicroarrayData || !runtimeTemplate.dataprocConfig),
      fp.map(({displayName, runtimeTemplate}) => ({label: displayName, value: runtimeTemplate}))
      )(runtimePresets)
    }
    onChange={({value}) => {
      const {presetDiskSize, presetMachineName, presetCompute} = fp.cond([
        // Can't destructure due to shadowing.
        [() => !!value.gceConfig, (tmpl: Runtime) => ({
          presetDiskSize: tmpl.gceConfig.diskSize,
          presetMachineName: tmpl.gceConfig.machineType,
          presetCompute: ComputeType.Standard
        })],
        [() => !!value.dataprocConfig, ({dataprocConfig: {masterDiskSize, masterMachineType}}) => ({
          presetDiskSize: masterDiskSize,
          presetMachineName: masterMachineType,
          presetCompute: ComputeType.Dataproc
        })]
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
    }} />;
};

const StartStopRuntimeButton = ({workspaceNamespace, googleProject}) => {
  const [status, setRuntimeStatus] = useRuntimeStatus(workspaceNamespace, googleProject);

  const rotateStyle = {animation: 'rotation 2s infinite linear'};
  const {altText, iconShape = null, styleOverrides = {}, onClick = null } = switchCase(status,
    [
      RuntimeStatus.Creating,
      () => ({
        altText: 'Runtime creation in progress',
        iconShape: 'compute-starting',
        styleOverrides: rotateStyle
      })
    ],
    [
      RuntimeStatus.Running,
      () => ({
        altText: 'Runtime running, click to pause',
        iconShape: 'compute-running',
        onClick: () => { setRuntimeStatus(RuntimeStatusRequest.Stop); }
      })
    ],
    [
      RuntimeStatus.Updating,
      () => ({
        altText: 'Runtime update in progress',
        iconShape: 'compute-starting',
        styleOverrides: rotateStyle
      })
    ],
    [
      RuntimeStatus.Error,
      () => ({
        altText: 'Runtime in error state',
        iconShape: 'compute-error'
      })
    ],
    [
      RuntimeStatus.Stopping,
      () => ({
        altText: 'Runtime pause in progress',
        iconShape: 'compute-stopping',
        styleOverrides: rotateStyle
      })
    ],
    [
      RuntimeStatus.Stopped,
      () => ({
        altText: 'Runtime paused, click to resume',
        iconShape: 'compute-stopped',
        onClick: () => { setRuntimeStatus(RuntimeStatusRequest.Start); }
      })
    ],
    [
      RuntimeStatus.Starting,
      () => ({
        altText: 'Runtime resume in progress',
        iconShape: 'compute-starting',
        styleOverrides: rotateStyle
      })
    ],
    [
      RuntimeStatus.Deleting,
      () => ({
        altText: 'Runtime deletion in progress',
        iconShape: 'compute-stopping',
        styleOverrides: rotateStyle,
      })
    ],
    [
      RuntimeStatus.Deleted,
      () => ({
        altText: 'Runtime has been deleted',
        iconShape: 'compute-none'
      })
    ],
    [
      RuntimeStatus.Unknown,
      () => ({
        altText: 'Runtime status unknown',
        iconShape: 'compute-none'
      })
    ],
    [
      DEFAULT,
      () => ({
        altText: 'No runtime found',
        iconShape: 'compute-none'
      })
    ]
  );

  const iconSrc = `/assets/icons/${iconShape}.svg`;

  {/* height/width of the icon wrapper are set so that the img element can rotate inside it */}
  {/* without making it larger. the svg is 36 x 36 px, per pythagorean theorem the diagonal */}
  {/* is 50.9px, so we round up */}
  const iconWrapperStyle = {
    height: '51px',
    width: '51px',
    justifyContent: 'space-around',
    alignItems: 'center',
  };

  return <FlexRow style={{
    backgroundColor: addOpacity(colors.primary, 0.1),
    justifyContent: 'space-around',
    alignItems: 'center',
    padding: '0 1rem',
    borderRadius: '5px 0 0 5px'
  }}>
    {/* TooltipTrigger inside the conditionals because it doesn't handle fragments well. */}
    {
      onClick && <TooltipTrigger content={<div>{altText}</div>} side='left'>
        <FlexRow style={iconWrapperStyle}>
          <Clickable onClick={() => onClick()}>
            <img alt={altText} src={iconSrc} style={styleOverrides} data-test-id='runtime-status-icon'/>
          </Clickable>
        </FlexRow>
      </TooltipTrigger>
    }
    {!onClick && <TooltipTrigger content={<div>{altText}</div>} side='left'>
        <FlexRow style={iconWrapperStyle}>
          <img alt={altText} src={iconSrc} style={styleOverrides} data-test-id='runtime-status-icon'/>
        </FlexRow>
      </TooltipTrigger>
    }
  </FlexRow>;
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
    workerMachineType = null,
    workerDiskSize = null,
    numberOfPreemptibleWorkers = 0
  } = dataprocConfig || {};
  const workerMachine = findMachineByName(workerMachineType);

  const costConfig = {
    computeType, masterMachine: machine, masterDiskSize: diskSize,
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
  const remainingCredits = creatorFreeCreditsRemaining === null ? <Spinner size={10}/> : formatUsd(creatorFreeCreditsRemaining);

  return <FlexRow
    style={
      runtimeChanged
        ? {backgroundColor: colorWithWhiteness(colors.warning, .9), borderColor: colors.warning}
        : {}
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
        Costs will draw from your remaining {remainingCredits} of free credits.
      </div>
    }
    {
      workspace.billingAccountType === BillingAccountType.FREETIER
      && currentUser !== workspace.creator
      && <div style={styles.costsDrawnFrom}>
        Costs will draw from workspace creator's remaining {remainingCredits} of free credits.
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

const CreatePanel = ({creatorFreeCreditsRemaining, profile, setPanelContent, workspace, runtimeConfig}) => {
  const displayName = runtimeConfig.computeType === ComputeType.Dataproc ?
    runtimePresets.hailAnalysis.displayName : runtimePresets.generalAnalysis.displayName;

  return <div data-test-id='runtime-create-panel' style={styles.controlSection}>
    <FlexRow style={styles.costPredictorWrapper}>
      <StartStopRuntimeButton workspaceNamespace={workspace.namespace} googleProject={workspace.googleProject}/>
      <CostInfo runtimeChanged={false}
                runtimeConfig={runtimeConfig}
                currentUser={profile.username}
                workspace={workspace}
                creatorFreeCreditsRemaining={creatorFreeCreditsRemaining}
      />
    </FlexRow>
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
      <b> {runtimeConfig.machine.cpu} CPUs</b>,
      <b> {runtimeConfig.machine.memory} GB memory</b>, and a
      <b> {runtimeConfig.diskSize} GB disk</b>
    </div>
    {runtimeConfig.computeType === ComputeType.Dataproc && <Fragment>
      <label htmlFor='worker-configuration' style={{...styles.bold, marginTop: '1rem'}}>Worker Configuration</label>
      <div id='worker-configuration'>- Default:
        <b> {runtimeConfig.dataprocConfig.numberOfWorkers} worker(s) </b>
        {
          runtimeConfig.dataprocConfig.numberOfPreemptibleWorkers > 0 &&
          <b>and {runtimeConfig.dataprocConfig.numberOfPreemptibleWorkers} preemptible worker(s) </b>
        }
        each with compute size of <b>{findMachineByName(runtimeConfig.dataprocConfig.workerMachineType).cpu} CPUs</b>,
        <b> {findMachineByName(runtimeConfig.dataprocConfig.workerMachineType).memory} GB memory</b>, and a
        <b> {runtimeConfig.dataprocConfig.workerDiskSize} GB disk</b>
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
          <div style={{...styles.costPredictorWrapper, padding: '.25rem .5rem'}}>
            <CostEstimator runtimeParameters={newRuntimeConfig}/>
          </div>
        </div>
        <div>
          <b style={{fontSize: 10}}>Previous estimated cost</b>
          <div style={{...styles.costPredictorWrapper,
            padding: '.25rem .5rem',
            color: 'grey',
            backgroundColor: ''}}>
            <CostEstimator runtimeParameters={initialRuntimeConfig} costTextColor='grey'/>
          </div>
        </div>
      </FlexRow>
    </div>

    <WarningMessage iconSize={30} iconPosition={'center'}>
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
)(({ cdrVersionTiersResponse, workspace, profileState, onClose = () => {}}) => {
  const {namespace, id, cdrVersionId, googleProject} = workspace;

  const {profile} = profileState;

  const {hasMicroarrayData} = findCdrVersion(cdrVersionId, cdrVersionTiersResponse) || {hasMicroarrayData: false};
  let [{currentRuntime, pendingRuntime}, setRequestedRuntime] = useCustomRuntime(namespace);

  // If the runtime has been deleted, it's possible that the default preset values have changed since its creation
  if (currentRuntime && currentRuntime.status === RuntimeStatus.Deleted) {
    currentRuntime = applyPresetOverride(currentRuntime);
  }

  // Prioritize the "pendingRuntime", if any. When an update is pending, we want
  // to render the target runtime details, which  may not match the current runtime.
  const {dataprocConfig = null, gceConfig = {diskSize: defaultDiskSize}} = pendingRuntime || currentRuntime || {} as Partial<Runtime>;
  const [status, setRuntimeStatus] = useRuntimeStatus(namespace, googleProject);
  const diskSize = dataprocConfig ? dataprocConfig.masterDiskSize : gceConfig.diskSize;
  const machineName = dataprocConfig ? dataprocConfig.masterMachineType : gceConfig.machineType;
  const initialMasterMachine = findMachineByName(machineName) || defaultMachineType;
  const initialCompute = dataprocConfig ? ComputeType.Dataproc : ComputeType.Standard;

  // We may encounter a race condition where an existing current runtime has not loaded by the time this panel renders.
  // It's unclear how often that would actually happen.
  const initialPanelContent = fp.cond([
    [([b, , ]) => b === BillingStatus.INACTIVE, () => PanelContent.Disabled],
    // currentRuntime being undefined means the first `getRuntime` has still not completed.
    // If there's a pendingRuntime, this means there's already a create/update
    // in progress, even if the runtime store doesn't actively reflect this yet.
    // Show the customize panel in this event.
    [([, r, ]) => r === undefined || !!pendingRuntime, () => PanelContent.Customize],
    [([, r, s]) => r === null || s === RuntimeStatus.Unknown, () => PanelContent.Create],
    [([, r, ]) => r.status === RuntimeStatus.Deleted &&
      ([RuntimeConfigurationType.GeneralAnalysis, RuntimeConfigurationType.HailGenomicAnalysis].includes(r.configurationType)),
      () => PanelContent.Create],
    [() => true, () => PanelContent.Customize]
  ])([workspace.billingStatus, currentRuntime, status]);
  const [panelContent, setPanelContent] = useState<PanelContent>(initialPanelContent);

  const [selectedMachine, setSelectedMachine] = useState(initialMasterMachine);
  const [selectedDiskSize, setSelectedDiskSize] = useState(diskSize);
  const [selectedCompute, setSelectedCompute] = useState<ComputeType>(initialCompute);
  // Note: while the Dataproc config does contain masterMachineType and masterDiskSize,
  // the source of truth for these values are selectedMachine, and selectedDiskSize, as
  // these UI components are used for both Dataproc and standard VMs.
  const [selectedDataprocConfig, setSelectedDataprocConfig] = useState<DataprocConfig | null>(dataprocConfig);

  const validMainMachineTypes = selectedCompute === ComputeType.Standard ?
      validLeoGceMachineTypes : validLeoDataprocMasterMachineTypes;
  // The compute type affects the set of valid machine types, so revert to the
  // default machine type if switching compute types would invalidate the main
  // machine type choice.
  useEffect(() => {
    if (!validMainMachineTypes.find(({name}) => name === selectedMachine.name)) {
      setSelectedMachine(initialMasterMachine);
    }
  }, [selectedCompute]);

  const runtimeExists = (status && ![RuntimeStatus.Deleted, RuntimeStatus.Error].includes(status)) || !!pendingRuntime;
  const disableControls = runtimeExists && ![RuntimeStatus.Running, RuntimeStatus.Stopped].includes(status as RuntimeStatus);

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

  const [creatorFreeCreditsRemaining, setCreatorFreeCreditsRemaining] = useState(null);
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

  // 50 GB is the minimum GCP limit for disk size, 4000 GB is our arbitrary limit for not making a
  // disk that is way too big and expensive on free tier ($.22 an hour). 64 TB is the GCE limit on
  // persistent disk.
  const diskSizeValidatorWithMessage = (diskType = 'standard' || 'master' || 'worker') => {
    const maxDiskSize = workspace.billingAccountType === BillingAccountType.FREETIER
        ? 4000
        : 64000;
    const message = {
      standard: `^Disk size must be between 50 and ${maxDiskSize} GB`,
      master: `^Master disk size must be between 50 and ${maxDiskSize} GB`,
      worker: `^Worker disk size must be between 50 and ${maxDiskSize} GB`
    };

    return {
      numericality: {
        greaterThanOrEqualTo: 50,
        lessThanOrEqualTo: maxDiskSize,
        message: message[diskType]
      }
    };
  };

  const runningCostValidatorWithMessage = () => {
    const maxRunningCost = workspace.billingAccountType === BillingAccountType.FREETIER
      ? 25
      : 150;
    const message = workspace.billingAccountType === BillingAccountType.FREETIER
      ? '^Your runtime is too expensive. To proceed using free credits, reduce your running costs below $25/hr.'
      : '^Your runtime is very expensive. Are you sure you wish to proceed?';
    return {
      numericality: {
        lessThan: maxRunningCost,
        message: message
      }
    };
  };

  const currentRunningCost = machineRunningCost({
    computeType: selectedCompute,
    masterMachine: selectedMachine,
    masterDiskSize: diskSize,
    numberOfWorkers: selectedDataprocConfig && selectedDataprocConfig.numberOfWorkers,
    numberOfPreemptibleWorkers: selectedDataprocConfig && selectedDataprocConfig.numberOfPreemptibleWorkers,
    workerDiskSize: selectedDataprocConfig && selectedDataprocConfig.workerDiskSize,
    workerMachine: selectedDataprocConfig && findMachineByName(selectedDataprocConfig.workerMachineType)
  });

  const standardDiskValidator = {
    selectedDiskSize: diskSizeValidatorWithMessage('standard')
  };
  const runningCostValidator = {
    currentRunningCost: runningCostValidatorWithMessage()
  };
  // We don't clear dataproc config when we change compute type so we can't combine this with the
  // above or else we can end up with phantom validation fails
  const dataprocValidators = {
    masterDiskSize: diskSizeValidatorWithMessage('master'),
    workerDiskSize: diskSizeValidatorWithMessage('worker')
  };

  const {masterDiskSize = null, workerDiskSize = null} = selectedDataprocConfig || {};
  const standardDiskErrors = validate({selectedDiskSize}, standardDiskValidator);
  const runningCostErrors = validate({currentRunningCost}, runningCostValidator);
  const dataprocErrors = selectedCompute === ComputeType.Dataproc
      ? validate({masterDiskSize, workerDiskSize}, dataprocValidators)
      : undefined;

  const getErrorMessageContent = () => {
    const errorDivs = [];
    if (standardDiskErrors) {
      errorDivs.push(summarizeErrors(standardDiskErrors));
    }
    if (dataprocErrors) {
      errorDivs.push(summarizeErrors(dataprocErrors));
    }
    if (workspace.billingAccountType === BillingAccountType.FREETIER && runningCostErrors) {
      errorDivs.push(summarizeErrors(runningCostErrors));
    }
    return errorDivs;
  };

  const getWarningMessageContent = () => {
    const warningDivs = [];
    if (workspace.billingAccountType === BillingAccountType.USERPROVIDED && runningCostErrors) {
      warningDivs.push(summarizeErrors(runningCostErrors));
    }
    return warningDivs;
  };

  const runtimeCanBeCreated = !(getErrorMessageContent().length > 0);
  // Casting to RuntimeStatus here because it can't easily be done at the destructuring level
  // where we get 'status' from
  const runtimeCanBeUpdated = runtimeChanged
      && [RuntimeStatus.Running, RuntimeStatus.Stopped].includes(status as RuntimeStatus)
      && runtimeCanBeCreated;

  const renderUpdateButton = () => {
    return <Button
      aria-label='Update'
      disabled={!runtimeCanBeUpdated}
      onClick={() => {
        setRequestedRuntime(createRuntimeRequest(newRuntimeConfig));
        onClose();
      }}>
      {needsDelete ? 'APPLY & RECREATE' : 'APPLY & REBOOT'}
    </Button>;
  };

  const renderCreateButton = () => {
    return <Button
      aria-label='Create'
      disabled={!runtimeCanBeCreated}
      onClick={() => {
        setRequestedRuntime(createRuntimeRequest(newRuntimeConfig));
        onClose();
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

  return <div id='runtime-panel'>
    {switchCase(panelContent,
      [PanelContent.Create, () =>
            <Fragment>
              <CreatePanel
                  creatorFreeCreditsRemaining={creatorFreeCreditsRemaining}
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
              onClose();
            }}
            onCancel={() => setPanelContent(PanelContent.Customize)}
          />],
      [PanelContent.Customize, () => <Fragment>
            <div style={styles.controlSection}>
              <FlexRow style={styles.costPredictorWrapper}>
                <StartStopRuntimeButton workspaceNamespace={workspace.namespace} googleProject={workspace.googleProject}/>
                <CostInfo runtimeChanged={runtimeChanged}
                  runtimeConfig={newRuntimeConfig}
                  currentUser={profile.username}
                  workspace={workspace}
                  creatorFreeCreditsRemaining={creatorFreeCreditsRemaining}
                  />
              </FlexRow>
              <PresetSelector
                hasMicroarrayData={hasMicroarrayData}
                disabled={disableControls}
                setSelectedDiskSize={(disk) => setSelectedDiskSize(disk)}
                setSelectedMachine={(machine) => setSelectedMachine(machine)}
                setSelectedCompute={(compute) => setSelectedCompute(compute)}
                setSelectedDataprocConfig={(dataproc) => setSelectedDataprocConfig(dataproc)}
              />
              {/* Runtime customization: change detailed machine configuration options. */}
              <h3 style={{...styles.sectionHeader, ...styles.bold}}>Cloud compute profile</h3>
              <div style={styles.formGrid}>
                <MachineSelector
                  idPrefix='runtime'
                  disabled={disableControls}
                  selectedMachine={selectedMachine}
                  onChange={(value) => setSelectedMachine(value)}
                  validMachineTypes={validMainMachineTypes}
                  machineType={machineName}/>
                <DiskSizeSelector
                    idPrefix='runtime'
                    selectedDiskSize={selectedDiskSize}
                    onChange={(value) => {
                      setSelectedDiskSize(value);
                    }}
                    disabled={disableControls}
                    diskSize={diskSize}/>
             </div>
             <FlexColumn style={{marginTop: '1rem'}}>
               <label style={styles.label} htmlFor='runtime-compute'>Compute type</label>
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
             <WarningMessage iconSize={30} iconPosition={'center'}>
                <div>You've made changes that require recreating your environment to take effect.</div>
             </WarningMessage>
           }
           {getErrorMessageContent().length > 0 &&
             <ErrorMessage iconSize={16} iconPosition={'top'} data-test-id={'runtime-error-messages'}>
               {getErrorMessageContent()}
             </ErrorMessage>
           }
           {getWarningMessageContent().length > 0 &&
            <WarningMessage iconSize={16} iconPosition={'top'} data-test-id={'runtime-warning-messages'}>
              {getWarningMessageContent()}
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
          />],
      [PanelContent.Disabled, () => <DisabledPanel/>])}
  </div>;
});
