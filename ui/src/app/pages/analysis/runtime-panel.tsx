import {Button, Clickable, Link, MenuItem} from 'app/components/buttons';
import {FlexColumn, FlexRow} from 'app/components/flex';
import {ClrIcon} from 'app/components/icons';
import {PopupTrigger, TooltipTrigger} from 'app/components/popups';
import {Spinner} from 'app/components/spinners';
import colors, {addOpacity, colorWithWhiteness} from 'app/styles/colors';
import {reactStyles, switchCase, withCurrentWorkspace, withUserProfile} from 'app/utils';
import {withCdrVersions} from 'app/utils';
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
import {runtimePresets} from 'app/utils/runtime-presets';
import {RuntimeStatusRequest, useCustomRuntime, useRuntimeStatus} from 'app/utils/runtime-utils';
import {WorkspaceData} from 'app/utils/workspace-data';

import {Dropdown} from 'primereact/dropdown';
import {InputNumber} from 'primereact/inputnumber';

import {workspacesApi} from 'app/services/swagger-fetch-clients';
import {formatUsd} from 'app/utils/numbers';
import {Runtime, RuntimeConfigurationType, RuntimeStatus} from 'generated/fetch';
import {BillingAccountType, CdrVersionListResponse, DataprocConfig} from 'generated/fetch';
import * as fp from 'lodash/fp';
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
  costPredictorWrapper: {
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
  Delete = 'Delete'
}

export interface Props {
  workspace: WorkspaceData;
  cdrVersionListResponse?: CdrVersionListResponse;
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

const MachineSelector = ({onChange, selectedMachine, machineType, idPrefix}) => {
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
        value={memory}
        />
  </Fragment>;
};

const DiskSizeSelector = ({onChange, selectedDiskSize, diskSize, idPrefix}) => {
  return <Fragment>
    <label htmlFor={`${idPrefix}-disk`}>Disk (GB)</label>
    <InputNumber id={`${idPrefix}-disk`}
      showButtons
      decrementButtonClassName='p-button-secondary'
      incrementButtonClassName='p-button-secondary'
      value={selectedDiskSize || diskSize}
      inputStyle={styles.inputNumber}
      onChange={({value}) => onChange(value)}
      min={50 /* Runtime API has a minimum 50GB requirement. */}/>
  </Fragment>;
};

const DataProcConfigSelector = ({onChange, dataprocConfig})  => {
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

  // On unmount clear the config - the user is no longer configuring a dataproc cluster
  useEffect(() => () => onChange(null), []);

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
        decrementButtonClassName='p-button-secondary'
        incrementButtonClassName='p-button-secondary'
        value={selectedNumWorkers}
        inputStyle={styles.inputNumber}
        onChange={({value}) => setSelectedNumWorkers(value)}
        min={2}/>
      <label htmlFor='num-preemptible'>Preemptible</label>
      <InputNumber id='num-preemptible'
        showButtons
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
        idPrefix='worker'/>
      <DiskSizeSelector diskSize={workerDiskSize} onChange={setSelectedDiskSize} selectedDiskSize={selectedDiskSize} idPrefix='worker'/>
    </div>
  </fieldset>;
};

const PresetSelector = ({hasMicroarrayData, setSelectedDiskSize, setSelectedMachine, setSelectedCompute, setSelectedDataprocConfig}) => {
  {/* Recommended runtime: pick from default templates or change the image. */}
  return <PopupTrigger side='bottom'
                closeOnClick
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
    <Clickable style={{display: 'inline-block'}} data-test-id='runtime-presets-menu'>
      Recommended environments <ClrIcon shape='caret down'/>
    </Clickable>
  </PopupTrigger>;
};

const CostEstimator = ({
  freeCreditsRemaining,
  profile,
  runtimeParameters,
  runtimeChanged,
  workspace
}) => {
  const wrapperStyle = runtimeChanged
    ? {...styles.costPredictorWrapper, backgroundColor: colorWithWhiteness(colors.warning, .9), borderColor: colors.warning}
    : styles.costPredictorWrapper;
  const {
    computeType,
    diskSize,
    machineType,
    dataprocConfig
  } = runtimeParameters;
  const {
    numberOfWorkers = 0,
    masterMachineType = machineType,
    masterDiskSize = diskSize,
    workerMachineType = null,
    workerDiskSize = null,
    numberOfPreemptibleWorkers = 0
  } = dataprocConfig || {};

  const runningCost = machineRunningCost({
    computeType: computeType,
    masterDiskSize: masterDiskSize || diskSize,
    masterMachineName: masterMachineType || machineType,
    numberOfWorkers: numberOfWorkers,
    numberOfPreemptibleWorkers: numberOfPreemptibleWorkers,
    workerDiskSize: workerDiskSize,
    workerMachineName: workerMachineType
  });

  const runningCostBreakdown = machineRunningCostBreakdown({
    computeType: computeType,
    masterDiskSize: masterDiskSize || diskSize,
    masterMachineName: masterMachineType || machineType,
    numberOfWorkers: numberOfWorkers,
    numberOfPreemptibleWorkers: numberOfPreemptibleWorkers,
    workerDiskSize: workerDiskSize,
    workerMachineName: workerMachineType
  });

  const storageCost = machineStorageCost({
    masterDiskSize: masterDiskSize || diskSize,
    numberOfPreemptibleWorkers: numberOfPreemptibleWorkers,
    numberOfWorkers: numberOfWorkers,
    workerDiskSize: workerDiskSize
  });

  const storageCostBreakdown = machineStorageCostBreakdown({
    masterDiskSize: masterDiskSize || diskSize,
    numberOfPreemptibleWorkers: numberOfPreemptibleWorkers,
    numberOfWorkers: numberOfWorkers,
    workerDiskSize: workerDiskSize
  });

  return <FlexRow
    style={wrapperStyle}
    data-test-id='cost-estimator'
  >
    <FlexRow style={{minWidth: '250px', margin: '.33rem .5rem'}}>
      <FlexColumn style={{marginRight: '1rem'}}>
        <div style={{fontSize: '10px', fontWeight: 600}}>Cost when running</div>
        <TooltipTrigger content={
          <div>
            <div>Cost Breakdown</div>
            {runningCostBreakdown.map((lineItem, i) => <div key={i}>{lineItem}</div>)}
          </div>
        }>
          <div
              style={{fontSize: '20px', color: colors.accent}}
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
              style={{fontSize: '20px', color: colors.accent}}
              data-test-id='storage-cost'
          >
            {formatUsd(storageCost)}/hr
          </div>
        </TooltipTrigger>
      </FlexColumn>
    </FlexRow>
    {
      workspace.billingAccountType === BillingAccountType.FREETIER
      && profile.username === workspace.creator
      && <div style={styles.costsDrawnFrom}>
        Costs will draw from your remaining {formatUsd(freeCreditsRemaining)} of free credits.
      </div>
    }
    {
      workspace.billingAccountType === BillingAccountType.FREETIER
      && profile.username !== workspace.creator
      && <div style={styles.costsDrawnFrom}>
        Costs will draw from workspace creator's remaining {formatUsd(freeCreditsRemaining)} of free credits.
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

const CreatePanel = ({creatorFreeCreditsRemaining, preset, profile, setPanelContent, workspace}) => {
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
    <CostEstimator
        freeCreditsRemaining={creatorFreeCreditsRemaining}
        profile={profile}
        runtimeParameters={{
          computeType: computeType,
          diskSize: gceConfig ? gceConfig.diskSize : null,
          machineType: gceConfig ? gceConfig.machineType : null,
          dataprocConfig: dataprocConfig ? dataprocConfig : null
        }}
        runtimeChanged={false}
        workspace={workspace}
    />
    <FlexRow style={{justifyContent: 'space-between'}}>
      <h3 style={{...styles.sectionHeader, ...styles.bold}}>Recommended Environment for {displayName}</h3>
      <Button
          type='secondaryLight'
          onClick={() => setPanelContent(PanelContent.Customize)}
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
        <b> {dataprocConfig.numberOfWorkers} worker(s)</b>
        {!dataprocConfig.numberOfPreemptibleWorkers && <Fragment>, </Fragment>}
        <b>
          {
            dataprocConfig.numberOfPreemptibleWorkers > 0 &&
            <Fragment> and {dataprocConfig.numberOfPreemptibleWorkers}  preemptible worker(s), </Fragment>
          }
          each with compute size of {workerMachine.cpu} CPUs
        </b>,
        <b> {workerMachine.memory} GB memory</b>, and a
        <b> {workerDiskSize} GB disk</b>
      </div>
    </Fragment>}
  </div>
};

const UpdateRuntimeButton = ({
   customOnClick = () => {},
   runtimeExists,
   runtimeChanged,
   runtimeStatus,
   selectedDiskSize,
   selectedMachineType,
   selectedDataprocConfig,
   setRequestedRuntime
}) => {
  return <Button
      aria-label={runtimeExists ? 'Update' : 'Create'}
      disabled={
        runtimeExists && (
            !runtimeChanged
            // Casting to RuntimeStatus here because it can't easily be done at the destructuring level
            // where we get 'status' from
            || ![RuntimeStatus.Running, RuntimeStatus.Stopped].includes(runtimeStatus as RuntimeStatus))
      }
      onClick={() => {
        const runtimeToRequest: Runtime = selectedDataprocConfig ? {
          dataprocConfig: {
            ...selectedDataprocConfig,
            masterMachineType: selectedMachineType,
            masterDiskSize: selectedDiskSize
          }
        } : {
          gceConfig: {
            machineType: selectedMachineType,
            diskSize: selectedDiskSize
          }
        };

        // If the selected runtime matches a preset, plumb through the appropriate configuration type.
        runtimeToRequest.configurationType = fp.get(
            'runtimeTemplate.configurationType',
            fp.find(
                ({runtimeTemplate}) => presetEquals(runtimeToRequest, runtimeTemplate),
                runtimePresets)
        ) || RuntimeConfigurationType.UserOverride;
        setRequestedRuntime(runtimeToRequest);
        customOnClick();
      }}>{runtimeExists ? 'Update' : 'Create'}</Button>
}

export const RuntimePanel = fp.flow(
  withCdrVersions(),
  withCurrentWorkspace(),
  withUserProfile()
)(({cdrVersionListResponse, workspace, profileState}) => {
  const {namespace, id, cdrVersionId} = workspace;

  const {profile} = profileState;

  const {hasMicroarrayData} = fp.find({cdrVersionId}, cdrVersionListResponse.items) || {hasMicroarrayData: false};
  const [currentRuntime, setRequestedRuntime] = useCustomRuntime(namespace);

  const {status = null, dataprocConfig = null, gceConfig = {diskSize: defaultDiskSize}} = currentRuntime || {} as Partial<Runtime>;
  const [, setRuntimeStatus] = useRuntimeStatus(namespace);
  const diskSize = dataprocConfig ? dataprocConfig.masterDiskSize : gceConfig.diskSize;
  const machineName = dataprocConfig ? dataprocConfig.masterMachineType : gceConfig.machineType;
  const initialMasterMachine = findMachineByName(machineName) || defaultMachineType;
  const initialCompute = dataprocConfig ? ComputeType.Dataproc : ComputeType.Standard;
  // TODO(RW-5591): Initialize PanelContent according to the runtime status.
  const initialPanelContent = fp.cond([
    [(s) => s === RuntimeStatus.Running || s === RuntimeStatus.Stopped, () => PanelContent.Customize],
    [(s) => s === null || s === RuntimeStatus.Unknown, () => PanelContent.Create],
    [() => true, () => PanelContent.Delete]
  ])(status);
  const [panelContent, setPanelContent] = useState<PanelContent>(initialPanelContent);

  const [selectedDiskSize, setSelectedDiskSize] = useState(diskSize);
  const [selectedMachine, setSelectedMachine] = useState(initialMasterMachine);
  const [selectedCompute, setSelectedCompute] = useState<ComputeType>(initialCompute);
  const [selectedDataprocConfig, setSelectedDataprocConfig] = useState<DataprocConfig | null>(dataprocConfig);

  const selectedMachineType = selectedMachine && selectedMachine.name;
  const runtimeExists = status && status !== RuntimeStatus.Deleted;
  const runtimeChanged = !fp.equals(selectedMachine, initialMasterMachine) ||
    selectedDiskSize !== diskSize ||
    !fp.equals(selectedDataprocConfig, dataprocConfig) ||
    !fp.equals(selectedCompute, initialCompute);

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

  // TODO(RW-5591): Conditionally render create runtime page if runtime null or Deleted.
  if (currentRuntime === undefined) {
    return <Spinner style={{width: '100%', marginTop: '5rem'}}/>;
  }

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
              preset={runtimePresets.generalAnalysis}
              profile={profile}
              setPanelContent={(value) => setPanelContent(value)}
              workspace={workspace}
          />
          <FlexRow style={{justifyContent: 'flex-end', marginTop: '1rem'}}>
            <UpdateRuntimeButton
                customOnClick={() => {
                  setRuntimeStatus(RuntimeStatusRequest.Create);
                  setPanelContent(PanelContent.Customize);
                }}
                runtimeExists={runtimeExists}
                runtimeChanged={runtimeChanged}
                runtimeStatus={status}
                selectedDiskSize={selectedDiskSize}
                selectedMachineType={selectedMachineType}
                selectedDataprocConfig={selectedDataprocConfig}
                setRequestedRuntime={(value) => setRequestedRuntime(value)}
            />
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
          <CostEstimator
              freeCreditsRemaining={creatorFreeCreditsRemaining}
              profile={profile}
              runtimeParameters={{
                computeType: selectedCompute,
                diskSize: selectedDiskSize,
                machineType: selectedMachineType,
                dataprocConfig: selectedDataprocConfig
              }}
              runtimeChanged={runtimeChanged}
              workspace={workspace}
          />
          <PresetSelector
              hasMicroarrayData={hasMicroarrayData}
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
                selectedMachine={selectedMachine}
             onChange={(value) => setSelectedMachine(value)}
             machineType={machineName}/>
            <DiskSizeSelector
                idPrefix='runtime'
                selectedDiskSize={selectedDiskSize}
                onChange={(value) => setSelectedDiskSize(value)}
                diskSize={diskSize}/>
         </div>
         <FlexColumn style={{marginTop: '1rem'}}>
           <label htmlFor='runtime-compute'>Compute type</label>
           <Dropdown id='runtime-compute'
                     disabled={!hasMicroarrayData}
                     style={{width: '10rem'}}
                     options={[ComputeType.Standard, ComputeType.Dataproc]}
                     value={selectedCompute || ComputeType.Standard}
                     onChange={({value}) => setSelectedCompute(value)}
                     />
           {
             selectedCompute === ComputeType.Dataproc &&
             <DataProcConfigSelector onChange={setSelectedDataprocConfig} dataprocConfig={selectedDataprocConfig} />
           }
         </FlexColumn>
       </div>
       {runtimeExists && runtimeChanged && <FlexRow
           style={{
             alignItems: 'center',
             backgroundColor: colorWithWhiteness(colors.warning, .9),
             border: `1px solid ${colors.warning}`,
             borderRadius: '5px',
             color: colors.dark,
             marginTop: '.5rem',
             padding: '.5rem 0px'
           }}
       >
         <ClrIcon
             style={{color: colors.warning, marginLeft: '.5rem'}}
             shape={'warning-standard'}
             size={16}
             class={'is-solid'}
         />
         <div style={{marginLeft: '.5rem'}}>You've made changes that require recreating your environment to take effect.</div>
       </FlexRow>}
       <FlexRow style={{justifyContent: 'space-between', marginTop: '.75rem'}}>
         <Link
           style={{...styles.deleteLink, ...(
             [RuntimeStatus.Running, RuntimeStatus.Stopped].includes(status as RuntimeStatus) ?
             {} : {color: colorWithWhiteness(colors.dark, .4)}
           )}}
           aria-label='Delete Environment'
           disabled={![RuntimeStatus.Running, RuntimeStatus.Stopped].includes(status as RuntimeStatus)}
           onClick={() => setPanelContent(PanelContent.Delete)}>Delete Environment</Link>
         <UpdateRuntimeButton
           runtimeExists={runtimeExists}
           runtimeChanged={runtimeChanged}
           runtimeStatus={status}
           selectedDiskSize={selectedDiskSize}
           selectedMachineType={selectedMachineType}
           selectedDataprocConfig={selectedDataprocConfig}
           setRequestedRuntime={(value) => setRequestedRuntime(value)}
         />
       </FlexRow>
     </Fragment>])}
  </div>;
});
