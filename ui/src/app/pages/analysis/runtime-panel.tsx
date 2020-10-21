import {Button, Clickable, MenuItem} from 'app/components/buttons';
import {FlexColumn, FlexRow} from 'app/components/flex';
import {ClrIcon} from 'app/components/icons';
import {PopupTrigger} from 'app/components/popups';
import {Spinner} from 'app/components/spinners';
import colors, {addOpacity} from 'app/styles/colors';
import {reactStyles, withCurrentWorkspace} from 'app/utils';
import {allMachineTypes, Machine, validLeonardoMachineTypes} from 'app/utils/machines';
import {runtimePresets} from 'app/utils/runtime-presets';
import {useCustomRuntime} from 'app/utils/runtime-utils';
import {WorkspaceData} from 'app/utils/workspace-data';

import {Dropdown} from 'primereact/dropdown';
import {InputNumber} from 'primereact/inputnumber';

import { RuntimeConfigurationType, RuntimeStatus } from 'generated';
import { DataprocConfig } from 'generated/fetch';
import * as fp from 'lodash/fp';
import * as React from 'react';

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
    gridTemplateColumns: '1fr 1fr 1fr 1fr 3rem 1fr',
    gridGap: '1rem',
    alignItems: 'center'
  },
  workerConfigLabel: {
    fontWeight: 600,
    marginBottom: '0.5rem'
  }
});

const defaultMachineName = 'n1-standard-4';
const defaultMachineType = allMachineTypes.find(({name}) => name === defaultMachineName);
const findMachineByName = machineToFind => fp.find(({name}) => name === machineToFind, allMachineTypes) || defaultMachineType;

enum ComputeType {
  Standard = 'Standard VM',
  Dataproc = 'Dataproc Cluster'
}

export interface Props {
  workspace: WorkspaceData;
}

const MachineSelector = ({onChange, selectedMachine, machineType, idPrefix}) => {
  const initialMachineType = fp.find(({name}) => name === machineType, allMachineTypes) || defaultMachineType;
  const {cpu, memory} = selectedMachine || initialMachineType;

  return <Fragment>
      <label htmlFor={`${idPrefix}-cpu`} style={{marginRight: '.25rem'}}>CPUs</label>
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
      <label htmlFor={`${idPrefix}-ram`} style={{marginRight: '.25rem'}}>RAM (GB)</label>
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
    <label htmlFor={`${idPrefix}-disk`} style={{marginRight: '.25rem'}}>Disk (GB)</label>
    <InputNumber id={`${idPrefix}-disk`}
      showButtons
      decrementButtonClassName='p-button-secondary'
      incrementButtonClassName='p-button-secondary'
      value={selectedDiskSize || diskSize}
      inputStyle={{padding: '.75rem .5rem', width: '2rem'}}
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
  const [selectedPreemtible, setUpdatedPreemptible] = useState<number>(numberOfPreemptibleWorkers);
  const [selectedWorkerMachine, setSelectedWorkerMachine] = useState<Machine>(initialMachine);
  const [selectedDiskSize, setSelectedDiskSize] = useState<number>(workerDiskSize);

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
      <label htmlFor='num-workers' style={{marginRight: '.25rem'}}>Workers</label>
      <InputNumber id='num-workers'
        showButtons
        decrementButtonClassName='p-button-secondary'
        incrementButtonClassName='p-button-secondary'
        value={selectedNumWorkers}
        inputStyle={{padding: '.75rem .5rem', width: '2rem'}}
        onChange={({value}) => setSelectedNumWorkers(value)}
        min={2}/>
      <label htmlFor='num-preemptible' style={{marginRight: '.25rem'}}>Preemptible</label>
      <InputNumber id='num-preemptible'
        showButtons
        decrementButtonClassName='p-button-secondary'
        incrementButtonClassName='p-button-secondary'
        value={selectedPreemtible}
        inputStyle={{padding: '.75rem .5rem', width: '2rem'}}
        onChange={({value}) => setUpdatedPreemptible(value)}
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

export const RuntimePanel = withCurrentWorkspace()(({workspace}) => {
  const [currentRuntime, setRequestedRuntime] = useCustomRuntime(workspace.namespace);

  const {status = RuntimeStatus.Unknown, toolDockerImage = '', dataprocConfig = null, gceConfig = {}} = currentRuntime || {};
  const machineName  = !!dataprocConfig ? dataprocConfig.masterMachineType : gceConfig.machineType;
  const diskSize = !!dataprocConfig ? dataprocConfig.masterDiskSize : gceConfig.bootDiskSize;
  const initialMasterMachine = findMachineByName(machineName);

  const [selectedDiskSize, setSelectedDiskSize] = useState(diskSize);
  const [selectedMachine, setSelectedMachine] = useState(initialMasterMachine);
  const [runtimeConfigurationType, setRuntimeConfigurationType] = useState(null);
  const [selectedCompute, setSelectedCompute] = useState<ComputeType>(dataprocConfig ? ComputeType.Dataproc : ComputeType.Standard);
  const [selectedDataprocConfig, setSelectedDataprocConfig] = useState<DataprocConfig | null>(dataprocConfig);

  const selectedMachineType = selectedMachine && selectedMachine.name;
  const runtimeChanged = !fp.equals(selectedMachine, initialMasterMachine) ||
    selectedDiskSize !== diskSize ||
    !fp.equals(selectedDataprocConfig, dataprocConfig);

  if (currentRuntime === undefined) {
    return <Spinner style={{width: '100%', marginTop: '5rem'}}/>;
  } else if (currentRuntime === null) {
    // TODO(RW-5591): Create runtime page goes here.
    return <React.Fragment>
      <div>No runtime exists yet</div>
    </React.Fragment>;
  }

  return <div data-test-id='runtime-panel'>
    <h3 style={styles.sectionHeader}>Cloud analysis environment</h3>
    <div>
      Your analysis environment consists of an application and compute resources.
      Your cloud environment is unique to this workspace and not shared with other users.
    </div>
    {/* TODO(RW-5419): Cost estimates go here. */}
    <div style={styles.controlSection}>
      {/* Recommended runtime: pick from default templates or change the image. */}
      <PopupTrigger side='bottom'
                    closeOnClick
                    content={
                      <React.Fragment>
                        {
                          fp.flow(
                            fp.filter(['displayName', 'General Analysis']),
                            fp.toPairs,
                            fp.map(([i, preset]) => {
                              return <MenuItem
                              style={styles.presetMenuItem}
                              key={i}
                              onClick={() => {
                                // renaming to avoid shadowing
                                const {runtimeTemplate} = preset;
                                const {presetDiskSize, presetMachineName} = fp.cond([
                                  [() => !!runtimeTemplate.gceConfig, ({gceConfig: {bootDiskSize, machineType}}) => ({
                                    presetDiskSize: bootDiskSize,
                                    presetMachineName: machineType
                                  })],
                                  [() => !!runtimeTemplate.dataprocConfig, ({dataprocConfig: {masterDiskSize, masterMachineType}}) => ({
                                    presetDiskSize: masterDiskSize,
                                    presetMachineName: masterMachineType
                                  })]
                                ])(runtimeTemplate);
                                const presetMachineType = fp.find(({name}) => name === presetMachineName, validLeonardoMachineTypes);

                                setSelectedDiskSize(presetDiskSize);
                                setSelectedMachine(presetMachineType);
                                setRuntimeConfigurationType(RuntimeConfigurationType.GeneralAnalysis);
                              }}>
                                {preset.displayName}
                              </MenuItem>;
                            })
                          )(runtimePresets)
                        }
                      </React.Fragment>
                    }>
        <Clickable data-test-id='runtime-presets-menu'>
          Recommended environments <ClrIcon shape='caret down'/>
        </Clickable>
      </PopupTrigger>
      <h3 style={styles.sectionHeader}>Application configuration</h3>
      {/* TODO(RW-5413): Populate the image list with server driven options. */}
      {toolDockerImage && <Dropdown style={{width: '100%'}}
                data-test-id='runtime-image-dropdown'
                disabled={true}
                options={[toolDockerImage]}
                value={toolDockerImage}/>
      }
      {/* Runtime customization: change detailed machine configuration options. */}
      <h3 style={styles.sectionHeader}>Cloud compute profile</h3>
      <FlexRow style={{justifyContent: 'space-between'}}>
        <MachineSelector
            idPrefix='runtime'
            selectedMachine={selectedMachine}
            onChange={(value) => {
              setSelectedMachine(value);
              if (value !== selectedMachine && value !== diskSize) {
                setRuntimeConfigurationType(RuntimeConfigurationType.UserOverride);
              }
            }}
            machineType={machineName}
        />
        <DiskSizeSelector
            idPrefix='runtime'
            selectedDiskSize={selectedDiskSize}
            onChange={(value) => {
              setSelectedDiskSize(value);
              if (value !== selectedDiskSize && value !== diskSize) {
                setRuntimeConfigurationType(RuntimeConfigurationType.UserOverride);
              }
            }}
            diskSize={diskSize}
        />
      </FlexRow>
      <FlexColumn style={{marginTop: '1rem'}}>
        <label htmlFor='runtime-compute'>Compute type</label>
        <Dropdown id='runtime-compute'
                  style={{width: '10rem'}}
                  options={[ComputeType.Dataproc, ComputeType.Standard]}
                  value={selectedCompute || ComputeType.Standard}
                  onChange={({value}) => setSelectedCompute(value)}
                  />
        {
          selectedCompute === ComputeType.Dataproc &&
          <DataProcConfigSelector onChange={setSelectedDataprocConfig} dataprocConfig={dataprocConfig} />
        }
      </FlexColumn>
    </div>
    <FlexRow style={{justifyContent: 'flex-end', marginTop: '.75rem'}}>
      <Button
        aria-label={currentRuntime ? 'Update' : 'Create'}
        disabled={status !== RuntimeStatus.Running || !runtimeChanged}
        onClick={() => {
          const runtimeToRequest = selectedDataprocConfig ? {dataprocConfig: selectedDataprocConfig} : {gceConfig: {
            machineType: selectedMachineType || machineName,
            diskSize: selectedDiskSize || diskSize
          }};
          setRequestedRuntime({configurationType: runtimeConfigurationType, ...runtimeToRequest});
        }
      }>{currentRuntime ? 'Update' : 'Create'}</Button>
    </FlexRow>
  </div>;

});
