import * as React from 'react';
import { CSSProperties, Fragment } from 'react';
import { Dropdown } from 'primereact/dropdown';

import { Autopilot } from 'generated/fetch';

import { styles } from 'app/components/common-env-conf-panels/styles';
import { FlexRow } from 'app/components/flex';
import { DEFAULT_AUTOPILOT_MACHINE } from 'app/utils/machines';

interface Props {
  selectedMachine: Autopilot;
  initialMachine?: Autopilot;
  onChange: (machine: Autopilot) => void;
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
  const { cpuInMillicores, memoryInGb } = selectedMachine || initialMachineType;

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
          onChange={({ value }) => onChange(value)}
          disabled={disabled}
          placeholder={'select'}
          value={cpuInMillicores}
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
          onChange={({ value }) => onChange(value)}
          disabled={disabled}
          value={memoryInGb}
          appendTo='self'
        />
      </FlexRow>
    </Fragment>
  );
};
