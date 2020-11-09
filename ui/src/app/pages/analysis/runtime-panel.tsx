import {NO_CHANGE} from '@angular/core/src/render3/instructions';
import {Button, Clickable, Link, MenuItem} from 'app/components/buttons';
import {FlexColumn, FlexRow} from 'app/components/flex';
import {ClrIcon} from 'app/components/icons';
import {PopupTrigger} from 'app/components/popups';
import {Spinner} from 'app/components/spinners';
import colors, {addOpacity, colorWithWhiteness} from 'app/styles/colors';
import {reactStyles, withCurrentWorkspace} from 'app/utils';
import {withCdrVersions} from 'app/utils';
import {allMachineTypes, Machine, validLeonardoMachineTypes} from 'app/utils/machines';
import {runtimePresets} from 'app/utils/runtime-presets';
import {useCustomRuntime} from 'app/utils/runtime-utils';
import {WorkspaceData} from 'app/utils/workspace-data';


import {Dropdown} from 'primereact/dropdown';
import {InputNumber} from 'primereact/inputnumber';

import { Runtime, RuntimeConfigurationType, RuntimeStatus } from 'generated/fetch';
import { CdrVersionListResponse, DataprocConfig } from 'generated/fetch';
import * as fp from 'lodash/fp';
import * as React from 'react';
import {TextColumn} from '../../components/text-column';

const {useState, useEffect, Fragment} = React;

const styles = reactStyles({
  sectionHeader: {
    color: colors.primary,
    fontSize: '16px',
    fontWeight: 700,
    lineHeight: '1rem',
    marginBottom: '12px',
    marginTop: '12px'
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
  }
});

const defaultMachineName = 'n1-standard-4';
const defaultMachineType = allMachineTypes.find(({name}) => name === defaultMachineName);
const findMachineByName = machineToFind => fp.find(({name}) => name === machineToFind, allMachineTypes) || defaultMachineType;
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

export enum ComputeType {
  Standard = 'Standard VM',
  Dataproc = 'Dataproc Cluster'
}

export interface Props {
  workspace: WorkspaceData;
  cdrVersionListResponse?: CdrVersionListResponse;
}

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

interface RuntimeDiff {
  desc: string;
  previous: string;
  new: string;
  differenceType: RuntimeDiffState;
}

enum RuntimeDiffState {
  NO_CHANGE,
  CAN_UPDATE,
  NEEDS_DELETE
}

interface RuntimeConfig {
  computeType: ComputeType;
  machine: Machine;
  diskSize: number;
  dataprocConfig: DataprocConfig;
}

function compareComputeTypes(oldRuntime: RuntimeConfig, newRuntime: RuntimeConfig): RuntimeDiff {
  return {
    desc: 'Change Compute Type',
    previous: oldRuntime.computeType,
    new: newRuntime.computeType,
    differenceType: oldRuntime.computeType === newRuntime.computeType ?
      RuntimeDiffState.NO_CHANGE : RuntimeDiffState.NEEDS_DELETE
  };
}

function compareMachineCpu(oldRuntime: RuntimeConfig, newRuntime: RuntimeConfig): RuntimeDiff {
  const oldCpu = oldRuntime.machine.cpu;
  const newCpu = newRuntime.machine.cpu;

  return {
    desc: (newCpu < oldCpu ?  'Decrease' : 'Increase') + ' Number of CPUs',
    previous: oldCpu.toString(),
    new: newCpu.toString(),
    differenceType: oldCpu === newCpu ? RuntimeDiffState.NO_CHANGE : RuntimeDiffState.NEEDS_DELETE
  };
}

function compareMachineMemory(oldRuntime: RuntimeConfig, newRuntime: RuntimeConfig): RuntimeDiff {
  const oldMemory = oldRuntime.machine.memory;
  const newMemory = newRuntime.machine.memory;

  return {
    desc: (newMemory < oldMemory ?  'Decrease' : 'Increase') + ' Memory',
    previous: oldMemory.toString(),
    new: newMemory.toString(),
    differenceType: oldMemory === newMemory ? RuntimeDiffState.NO_CHANGE : RuntimeDiffState.NEEDS_DELETE
  };
}

function compareDiskSize(oldRuntime: RuntimeConfig, newRuntime: RuntimeConfig): RuntimeDiff {
  let desc = 'Memory';
  let diffType;

  if (newRuntime.diskSize < oldRuntime.diskSize) {
    desc = 'Decease ' + desc;
    diffType = RuntimeDiffState.NEEDS_DELETE;
  } else if (newRuntime.diskSize > oldRuntime.diskSize) {
    desc = 'Increase ' + desc;
    diffType = RuntimeDiffState.CAN_UPDATE;
  } else {
    diffType = RuntimeDiffState.NO_CHANGE;
  }

  return {
    desc: desc,
    previous: oldRuntime.diskSize.toString(),
    new: newRuntime.diskSize.toString(),
    differenceType: diffType
  };
}

function compareDataprocMasterDiskSize(oldRuntime: RuntimeConfig, newRuntime: RuntimeConfig): RuntimeDiff {
  if (oldRuntime.dataprocConfig === null || newRuntime.dataprocConfig === null) {
    return null;
  }

  let desc = 'Dataproc Master Machine Disk Size';
  let diffType;

  if (newRuntime.dataprocConfig.masterDiskSize < oldRuntime.dataprocConfig.masterDiskSize) {
    desc = 'Decease ' + desc;
    diffType = RuntimeDiffState.NEEDS_DELETE;
  } else if (newRuntime.dataprocConfig.masterDiskSize > oldRuntime.dataprocConfig.masterDiskSize) {
    desc = 'Increase ' + desc;
    diffType = RuntimeDiffState.CAN_UPDATE;
  } else {
    diffType = RuntimeDiffState.NO_CHANGE;
  }

  return {
    desc: desc,
    previous: oldRuntime.dataprocConfig.masterDiskSize.toString(),
    new: newRuntime.dataprocConfig.masterDiskSize.toString(),
    differenceType: diffType
  };
}

function compareDataprocMasterMachineType(oldRuntime: RuntimeConfig, newRuntime: RuntimeConfig): RuntimeDiff {
  if (oldRuntime.dataprocConfig === null || newRuntime.dataprocConfig === null) {
    return null;
  }

  return {
    desc: 'Change Master Machine Type',
    previous: oldRuntime.dataprocConfig.masterMachineType,
    new: newRuntime.dataprocConfig.masterMachineType,
    differenceType: oldRuntime.dataprocConfig.masterMachineType === newRuntime.dataprocConfig.masterMachineType ?
      RuntimeDiffState.NO_CHANGE : RuntimeDiffState.NEEDS_DELETE
  };
}

function compareDataprocWorkerMachineType(oldRuntime: RuntimeConfig, newRuntime: RuntimeConfig): RuntimeDiff {
  if (oldRuntime.dataprocConfig === null || newRuntime.dataprocConfig === null) {
    return null;
  }

  return {
    desc: 'Change Worker Machine Type',
    previous: oldRuntime.dataprocConfig.workerMachineType,
    new: newRuntime.dataprocConfig.workerMachineType,
    differenceType: oldRuntime.dataprocConfig.workerMachineType === newRuntime.dataprocConfig.workerMachineType ?
      RuntimeDiffState.NO_CHANGE : RuntimeDiffState.NEEDS_DELETE
  };
}

function compareDataprocWorkerDiskSize(oldRuntime: RuntimeConfig, newRuntime: RuntimeConfig): RuntimeDiff {
  if (oldRuntime.dataprocConfig === null || newRuntime.dataprocConfig === null) {
    return null;
  }

  const oldDiskSize = oldRuntime.dataprocConfig.workerDiskSize;
  const newDiskSize = newRuntime.dataprocConfig.workerDiskSize;

  return {
    desc: (newDiskSize < oldDiskSize ?  'Decrease' : 'Increase') + ' Change Worker Machine Type',
    previous: oldDiskSize.toString(),
    new: newDiskSize.toString(),
    differenceType: oldDiskSize === newDiskSize ?
      RuntimeDiffState.NO_CHANGE : RuntimeDiffState.NEEDS_DELETE
  };
}

function compareDataprocNumberOfPreemptibleWorkers(oldRuntime: RuntimeConfig, newRuntime: RuntimeConfig): RuntimeDiff {
  if (oldRuntime.dataprocConfig === null || newRuntime.dataprocConfig === null) {
    return null;
  }

  const oldNumWorkers = oldRuntime.dataprocConfig.numberOfPreemptibleWorkers;
  const newNumWorkers = newRuntime.dataprocConfig.numberOfPreemptibleWorkers;

  return {
    desc: (newNumWorkers < oldNumWorkers ?  'Decrease' : 'Increase') + ' Number of Preemptible Workers',
    previous: oldNumWorkers.toString(),
    new: newNumWorkers.toString(),
    differenceType: oldNumWorkers === newNumWorkers ?
      RuntimeDiffState.NO_CHANGE : RuntimeDiffState.NEEDS_DELETE
  };
}

function compareDataprocNumberOfWorkers(oldRuntime: RuntimeConfig, newRuntime: RuntimeConfig): RuntimeDiff {
  if (oldRuntime.dataprocConfig === null || newRuntime.dataprocConfig === null) {
    return null;
  }

  const oldNumWorkers = oldRuntime.dataprocConfig.numberOfWorkers;
  const newNumWorkers = newRuntime.dataprocConfig.numberOfWorkers;

  return {
    desc: (newNumWorkers < oldNumWorkers ?  'Decrease' : 'Increase') + ' Number of Workers',
    previous: oldNumWorkers.toString(),
    new: newNumWorkers.toString(),
    differenceType: oldNumWorkers === newNumWorkers ?
      RuntimeDiffState.NO_CHANGE : RuntimeDiffState.NEEDS_DELETE
  };
}

function getRuntimeDiff(oldRuntime: RuntimeConfig, newRuntime: RuntimeConfig): RuntimeDiff[] {
  const compareFns = [compareComputeTypes, compareDiskSize, compareMachineCpu,
    compareMachineMemory, compareDataprocMasterDiskSize, compareDataprocMasterMachineType,
    compareDataprocNumberOfPreemptibleWorkers, compareDataprocNumberOfWorkers,
    compareDataprocWorkerDiskSize, compareDataprocWorkerMachineType];

  return compareFns.map(compareFn => compareFn(oldRuntime, newRuntime)).filter(diff => diff !== null);
}

enum RuntimePanelMode {
  Edit,
  Confirm
}

export const RuntimePanel = fp.flow(withCurrentWorkspace(), withCdrVersions())(({workspace, cdrVersionListResponse}) => {
  const {namespace, cdrVersionId} = workspace;
  const {hasMicroarrayData} = fp.find({cdrVersionId}, cdrVersionListResponse.items) || {hasMicroarrayData: false};
  const [currentRuntime, setRequestedRuntime] = useCustomRuntime(namespace);
  const {status = null, dataprocConfig = null, gceConfig = {diskSize: defaultDiskSize}} = currentRuntime || {} as Partial<Runtime>;
  const machineName = !!dataprocConfig ? dataprocConfig.masterMachineType : gceConfig.machineType;
  const diskSize = !!dataprocConfig ? dataprocConfig.masterDiskSize : gceConfig.diskSize;
  const initialMasterMachine = findMachineByName(machineName);
  const [selectedDiskSize, setSelectedDiskSize] = useState(diskSize);
  const [selectedMachine, setSelectedMachine] = useState(initialMasterMachine);
  const initialComputeType = dataprocConfig ? ComputeType.Dataproc : ComputeType.Standard;
  const [selectedCompute, setSelectedCompute] = useState<ComputeType>(dataprocConfig ? ComputeType.Dataproc : ComputeType.Standard);
  const [selectedDataprocConfig, setSelectedDataprocConfig] = useState<DataprocConfig | null>(dataprocConfig);
  const [runtimePanelMode, setRuntimePanelMode] = useState(RuntimePanelMode.Edit);

  const selectedMachineType = selectedMachine && selectedMachine.name;
  const runtimeExists = status && status !== RuntimeStatus.Deleted;

  const initialRuntimeConfig = {
    computeType: initialComputeType,
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

  const runtimeDiffs = getRuntimeDiff(initialRuntimeConfig, newRuntimeConfig);
  console.log(runtimeDiffs);

  // TODO(RW-5591): Conditionally render create runtime page if runtime null or Deleted.
  if (currentRuntime === undefined) {
    return <Spinner style={{width: '100%', marginTop: '5rem'}}/>;
  }

  const renderUpdateButton = () => {
    return <Button
      aria-label='Update'
      disabled={
          !runtimeDiffs.map(diff => diff.differenceType).includes(RuntimeDiffState.CAN_UPDATE)
          // Casting to RuntimeStatus here because it can't easily be done at the destructuring level
          // where we get 'status' from
          || ![RuntimeStatus.Running, RuntimeStatus.Stopped].includes(status as RuntimeStatus)
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
      }}>
        Update
    </Button>;
  };

  const renderNextButton = () => {
    return <Button
      aria-label='Next'
      onClick={() => {
        setRuntimePanelMode(RuntimePanelMode.Confirm);
      }}>
      Next
    </Button>;
  };

  const renderCreateButton = () => {
    return <Button
      aria-label='Create'
      onClick={() => {
        // TODO: refactor this portion
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
      }}>
      Create
    </Button>;
  };

  const renderControlSection = () => {
    return <React.Fragment>
      <div style={styles.controlSection}>
        {/* Recommended runtime: pick from default templates or change the image. */}
        <PopupTrigger side='bottom'
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
        </PopupTrigger>
        {/* Runtime customization: change detailed machine configuration options. */}
        <h3 style={styles.sectionHeader}>Cloud compute profile</h3>
        <div style={styles.formGrid}>
          <MachineSelector
            idPrefix='runtime'
            selectedMachine={selectedMachine}
            onChange={(value) => setSelectedMachine(value)}
            machineType={machineName}
          />
          <DiskSizeSelector
            idPrefix='runtime'
            selectedDiskSize={selectedDiskSize}
            onChange={(value) => setSelectedDiskSize(value)}
            diskSize={diskSize}
          />
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

      <FlexRow style={styles.errorMessage}>
        <ClrIcon
          shape={'warning-standard'}
          class={'is-solid'}
          size={26}
          style={{
            color: colors.warning,
            flex: '0 0 auto'
          }}
        />
        <div style={{paddingLeft: '0.5rem'}}>
          You've made changes that require recreating your environment to take effect.
        </div>
      </FlexRow>

      <FlexRow style={{justifyContent: 'flex-end', marginTop: '.75rem'}}>
        {!runtimeExists ? renderCreateButton() :
          runtimeDiffs.map(diff => diff.differenceType).includes(RuntimeDiffState.NEEDS_DELETE) ?
            renderNextButton() : renderUpdateButton()}
      </FlexRow>
    </React.Fragment>;
  };

  const renderConfirmSection = () => {
    return <React.Fragment>
      <div style={styles.controlSection}>
        <h3 style={{...styles.sectionHeader, marginTop: '.1rem', marginBottom: '.1rem'}}>Editing your environment</h3>
        <div>
          You're about to apply the following changes to your environment:
        </div>
        <ul>
          {runtimeDiffs.filter(diff => diff.differenceType !== RuntimeDiffState.NO_CHANGE)
            .map(diff =>
              <li>
                {diff.desc} from {diff.previous} to {diff.new}
              </li>
            )
          }
        </ul>
      </div>

      <FlexRow style={styles.errorMessage}>
        <ClrIcon
          shape={'warning-standard'}
          class={'is-solid'}
          size={20}
          style={{
            marginTop: '0.2rem',
            color: colors.warning,
            flex: '0 0 auto'
          }}
        />
        <div style={{paddingLeft: '0.25rem'}}>
          <TextColumn>
            <div>
              You've made changes that can only take effect upon deletion and re-creation of your cloud environment.
            </div>
            <div style={{marginTop: '0.5rem'}}>
              Any in-memory state and local file modifications will be erased. Data stored in workspace buckets
              is never affected by changes to your cloud environment.
            </div>
          </TextColumn>
        </div>
      </FlexRow>

      <FlexRow style={{justifyContent: 'flex-end', marginTop: '.75rem'}}>
        {renderUpdateButton()}
      </FlexRow>
    </React.Fragment>;
  };

  return <div data-test-id='runtime-panel'>
    <h3 style={styles.sectionHeader}>Cloud analysis environment</h3>
    <div>
      Your analysis environment consists of an application and compute resources.
      Your cloud environment is unique to this workspace and not shared with other users.
    </div>
    {/* TODO(RW-5419): Cost estimates go here. */}
    <React.Fragment>
      {
        runtimePanelMode === RuntimePanelMode.Edit ? renderControlSection() :
          runtimePanelMode === RuntimePanelMode.Confirm ? renderConfirmSection() :
            null
      }
    </React.Fragment>

  </div>;

});
