import * as React from 'react';
import { CSSProperties, Fragment } from 'react';
import {
  InputNumber,
  InputNumberValueChangeEvent,
} from 'primereact/inputnumber';

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
  const { cpuInMillicores, memoryInGb } = selectedMachine;

  const cpuOnchange = (x: InputNumberValueChangeEvent) => {
    const memoryToCpuRatio = memoryInGb / x.value; // Memory to CPU ratio needs to be between 1:1 and 6.5:1
    const adjustedMemory = (() => {
      if (memoryToCpuRatio >= 1 && memoryToCpuRatio <= 6.5) {
        return memoryInGb;
      } else {
        // auto adjust
        if (memoryToCpuRatio < 1) {
          return x.value;
        } else if (memoryToCpuRatio > 6.5) {
          const newMemory = x.value * 6.5;
          return newMemory;
        }
      }
    })();
    onChange({
      ...selectedMachine,
      cpuInMillicores: x.value * 1000, // number is presented as number of CPUs, but backend we're using milicores as unit.
      memoryInGb: adjustedMemory,
    });
  };
  const memoryOnChange = (x: InputNumberValueChangeEvent) => {
    const memoryToCpuRatio = x.value / (cpuInMillicores / 1000); // Memory to CPU ratio needs to be between 1:1 and 6.5:1
    const adjustedCpu = (() => {
      if (memoryToCpuRatio >= 1 && memoryToCpuRatio <= 6.5) {
        return cpuInMillicores / 1000;
      } else {
        // auto adjust
        if (memoryToCpuRatio < 1) {
          return x.value;
        } else if (memoryToCpuRatio > 6.5) {
          const newCpu = x.value / 6.5;
          return newCpu;
        }
      }
    })();
    onChange({
      ...selectedMachine,
      cpuInMillicores: adjustedCpu * 1000, // number is presented as number of CPUs, but backend we're using milicores as unit.
      memoryInGb: x.value,
    });
  };

  return (
    <Fragment>
      <FlexRow style={styles.labelAndInput}>
        <label
          id={`${idPrefix}-cpu-label`}
          style={{ ...styles.label, ...cpuLabelStyles }}
          htmlFor={`${idPrefix}-cpu`}
        >
          CPUs
        </label>
        <InputNumber
          id={`${idPrefix}-cpu`}
          name={`${idPrefix}-cpu`}
          aria-labelledby={`${idPrefix}-cpu-label`}
          onValueChange={cpuOnchange}
          allowEmpty={false}
          disabled={disabled}
          inputStyle={styles.largeInputNumber}
          maxFractionDigits={2}
          value={cpuInMillicores / 1000}
          max={30}
          min={0.5}
          tooltip={
            '0.5 to 30. CPU to Memory ratio needs to be between 1:1 and 1:6.5, or it will be auto adjusted.'
          }
        />
      </FlexRow>
      <FlexRow style={styles.labelAndInput}>
        <label
          id={`${idPrefix}-mem-label`}
          style={{ ...styles.label, ...ramLabelStyles }}
          htmlFor={`${idPrefix}-mem`}
        >
          RAM (GB)
        </label>
        <InputNumber
          id={`${idPrefix}-mem`}
          name={`${idPrefix}-mem`}
          aria-labelledby={`${idPrefix}-mem-label`}
          onValueChange={memoryOnChange}
          allowEmpty={false}
          disabled={disabled}
          inputStyle={styles.largeInputNumber}
          value={memoryInGb}
          max={110}
          min={2}
          tooltip={
            '5GB to 110GB. CPU to Memory ratio needs to be between 1:1 and 1:6.5, or it will be auto adjusted.'
          }
        />
      </FlexRow>
    </Fragment>
  );
};
