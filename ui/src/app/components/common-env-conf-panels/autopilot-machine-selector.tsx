import * as React from 'react';
import { CSSProperties, Fragment } from 'react';
import { InputNumber, InputNumberChangeEvent } from 'primereact/inputnumber';

import { Autopilot } from 'generated/fetch';

import { styles } from 'app/components/common-env-conf-panels/styles';
import { FlexRow } from 'app/components/flex';

interface Props {
  selectedMachine: Autopilot;
  initialMachine?: Autopilot;
  onChange: (autopilot: Autopilot) => void;
  disabled: boolean;
  idPrefix: string;
  cpuLabelStyles?: CSSProperties;
  ramLabelStyles?: CSSProperties;
}
// Resource requirements https://cloud.google.com/kubernetes-engine/docs/concepts/autopilot-resource-requests#compute-class-min-max
export const AutopilotMachineSelector = ({
  selectedMachine,
  idPrefix,
  onChange,
  disabled,
  cpuLabelStyles = {},
  ramLabelStyles = {},
}: Props) => {
  const cpuOnchange = (x: InputNumberChangeEvent) => {
    onChange({
      ...selectedMachine,
      cpuInMillicores: x.value * 1000, // number is presented as number of CPUs, but backend we're using milicores as unit.
    });
  };
  const memoryOnChange = (x: InputNumberChangeEvent) => {
    onChange({
      ...selectedMachine,
      memoryInGb: x.value,
    });
  };
  return (
    <Fragment>
      <FlexRow style={styles.labelAndInput}>
        <label
          style={{ ...styles.label, ...cpuLabelStyles }}
          htmlFor={`${idPrefix}-cpu`}
        >
          CPUs
        </label>
        <InputNumber
          id={`${idPrefix}-cpu`}
          name={`${idPrefix}-cpu`}
          onChange={cpuOnchange}
          allowEmpty={false}
          disabled={disabled}
          inputStyle={styles.largeInputNumber}
          maxFractionDigits={2}
          value={0.5}
          max={30}
          min={0.5}
          tooltip={
            '0.5 to 30. CPU to Memory ratio needs to be between 1:1 and 1:6.5, or it may be auto adjusted ???.'
          }
        />
      </FlexRow>
      <FlexRow style={styles.labelAndInput}>
        <label
          style={{ ...styles.label, ...ramLabelStyles }}
          htmlFor={`${idPrefix}-ram`}
        >
          RAM (GB)
        </label>
        <InputNumber
          id={`${idPrefix}-mem`}
          name={`${idPrefix}-mem`}
          onChange={memoryOnChange}
          allowEmpty={false}
          disabled={disabled}
          inputStyle={styles.largeInputNumber}
          value={5}
          max={110}
          min={2}
          tooltip={
            '5GB to 110GB. CPU to Memory ratio needs to be between 1:1 and 1:6.5, or it may be auto adjusted ???'
          }
        />
      </FlexRow>
    </Fragment>
  );
};
