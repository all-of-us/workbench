import * as React from 'react';
import { useEffect, useState } from 'react';
import * as fp from 'lodash/fp';
import { Dropdown } from 'primereact/dropdown';

import { GpuConfig } from 'generated/fetch';

import { styles } from 'app/components/common-env-conf-panels/styles';
import { FlexColumn, FlexRow } from 'app/components/flex';
import { CheckBox } from 'app/components/inputs';
import {
  getValidGpuTypes,
  gpuTypeToDisplayName,
  Machine,
} from 'app/utils/machines';
import { supportUrls } from 'app/utils/zendesk';

interface Props {
  disabled: boolean;
  onChange: (gpuConfig: GpuConfig) => void;
  selectedMachine: Machine;
  gpuConfig: GpuConfig;
}
export const GpuConfigSelector = ({
  disabled,
  onChange,
  selectedMachine,
  gpuConfig,
}: Props) => {
  const { gpuType = 'nvidia-tesla-t4', numOfGpus = 1 } = gpuConfig || {};
  const [selectedGpuType, setSelectedGpuType] = useState<string>(gpuType);
  const [selectedNumOfGpus, setSelectedNumOfGpus] = useState<number>(numOfGpus);
  const [hasGpu, setHasGpu] = useState<boolean>(!!gpuConfig);
  const validGpuOptions = getValidGpuTypes(
    selectedMachine.cpu,
    selectedMachine.memory
  );
  const validGpuNames = fp.flow(
    fp.map('name'),
    fp.uniq,
    fp.sortBy('price')
  )(validGpuOptions);
  const validNumGpusOptions = fp.flow(
    fp.filter({ type: selectedGpuType }),
    fp.map('numGpus')
  )(validGpuOptions);

  useEffect(() => {
    onChange(
      hasGpu && validGpuOptions.length > 0
        ? {
            gpuType: selectedGpuType,
            numOfGpus: selectedNumOfGpus,
          }
        : null
    );
  }, [hasGpu, selectedGpuType, selectedNumOfGpus]);

  return (
    <FlexColumn style={styles.gpuSection}>
      <FlexRow style={styles.gpuCheckBoxRow}>
        <CheckBox
          id={'enable-gpu'}
          label='Enable GPUs'
          checked={hasGpu}
          style={styles.gpuCheckBox}
          disabled={disabled}
          onChange={() => {
            setHasGpu(!hasGpu);
          }}
        />
        <a target='_blank' href={supportUrls.gpus}>
          Learn more about GPU cost and restrictions.
        </a>
      </FlexRow>
      {hasGpu && (
        <FlexRow style={styles.formGrid2}>
          <FlexRow style={styles.labelAndInput}>
            <label
              style={{ ...styles.label, minWidth: '4.5rem' }}
              htmlFor='gpu-type'
            >
              Gpu Type
            </label>
            <Dropdown
              id={'gpu-type'}
              style={{ width: '10.5rem' }}
              options={validGpuNames}
              onChange={({ value }) => {
                setSelectedGpuType(
                  fp.find({ name: value }, validGpuOptions).type
                );
              }}
              disabled={disabled}
              value={gpuTypeToDisplayName(selectedGpuType)}
            />
          </FlexRow>
          <FlexRow style={styles.labelAndInput}>
            <label
              style={{ ...styles.label, minWidth: '3.0rem' }}
              htmlFor='gpu-num'
            >
              GPUs
            </label>
            <Dropdown
              id={'gpu-num'}
              options={validNumGpusOptions}
              onChange={({ value }) => setSelectedNumOfGpus(value)}
              disabled={disabled}
              value={selectedNumOfGpus}
            />
          </FlexRow>
        </FlexRow>
      )}
    </FlexColumn>
  );
};
