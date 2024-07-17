import * as React from 'react';
import { CSSProperties, Fragment } from 'react';
import { Dropdown } from 'primereact/dropdown';

import { styles } from 'app/components/common-env-conf-panels/styles';
import { FlexRow } from 'app/components/flex';
import {
  AutopilotMachine,
  DEFAULT_AUTOPILOT_MACHINE,
} from 'app/utils/machines';

interface Props {
  selectedMachine: AutopilotMachine;
  initialMachine?: AutopilotMachine;
  onChange: (machine: AutopilotMachine) => void;
  disabled: boolean;
  idPrefix: string;
  cpuLabelStyles?: CSSProperties;
  ramLabelStyles?: CSSProperties;
}
export const AutopilotMachineSelector = ({
  onChange,
  selectedMachine,
  initialMachine,
  disabled,
  idPrefix,
  cpuLabelStyles = {},
  ramLabelStyles = {},
}: Props) => {
  const initialMachineType = initialMachine || DEFAULT_AUTOPILOT_MACHINE;
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
          name={`${idPrefix}-cpu`}
          options={[1, 2, 3, 4, 5, 6, 7, 8]}
          onChange={({ value }) => onChange}
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
          name={`${idPrefix}-ram`}
          options={[10, 20, 30, 40, 50, 60, 70, 80, 90, 100]}
          onChange={({ value }) => onChange}
          disabled={disabled}
          value={memory}
          appendTo='self'
        />
      </FlexRow>
    </Fragment>
  );
};
