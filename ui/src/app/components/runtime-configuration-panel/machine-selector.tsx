import * as React from 'react';
import { CSSProperties, Fragment } from 'react';
import * as fp from 'lodash/fp';
import { Dropdown } from 'primereact/dropdown';

import { styles } from 'app/components/common-env-conf-panels/styles';
import { FlexRow } from 'app/components/flex';
import {
  DEFAULT_MACHINE_TYPE,
  findMachineByName,
  Machine,
} from 'app/utils/machines';

interface Props {
  selectedMachine: Machine;
  machineType: string;
  validMachineTypes: Machine[];
  onChange: (machine: Machine) => void;
  disabled: boolean;
  idPrefix: string;
  cpuLabelStyles?: CSSProperties;
  ramLabelStyles?: CSSProperties;
}
export const MachineSelector = ({
  onChange,
  selectedMachine,
  machineType,
  disabled,
  idPrefix,
  validMachineTypes,
  cpuLabelStyles = {},
  ramLabelStyles = {},
}: Props) => {
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
          appendTo='self'
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
          appendTo='self'
        />
      </FlexRow>
    </Fragment>
  );
};
