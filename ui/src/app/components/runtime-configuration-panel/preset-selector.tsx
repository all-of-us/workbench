import * as React from 'react';
import { Dropdown } from 'primereact/dropdown';

import { Disk } from 'generated/fetch';

import colors from 'app/styles/colors';
import { runtimePresets } from 'app/utils/runtime-presets';
import { AnalysisConfig, toAnalysisConfig } from 'app/utils/runtime-utils';

export interface PresetSelectorProps {
  allowDataproc: boolean;
  setAnalysisConfig: (analysisConfig: AnalysisConfig) => void;
  disabled: boolean;
  gcePersistentDisk: Disk | null | undefined;
}
// Select a recommended preset configuration.
export const PresetSelector = ({
  allowDataproc,
  setAnalysisConfig,
  disabled,
  gcePersistentDisk,
}: PresetSelectorProps) => {
  const options = Object.values(runtimePresets)
    .filter(
      ({ runtimeTemplate }) => allowDataproc || !runtimeTemplate.dataprocConfig
    )
    .map(({ displayName, runtimeTemplate }) => ({
      label: displayName,
      value: runtimeTemplate,
    }));
  return (
    <Dropdown
      {...{ options, disabled }}
      id='runtime-presets-menu'
      appendTo='self'
      style={{
        marginTop: '21px',
        color: colors.primary,
      }}
      placeholder='Recommended environments'
      onChange={({ value }) => {
        setAnalysisConfig(toAnalysisConfig(value, gcePersistentDisk));

        // Return false to skip the normal handling of the value selection. We're
        // abusing the dropdown here to act as if it were a menu instead.
        // Therefore, we never want the empty "placeholder" text to change to a
        // selected value (it should always read "recommended environments"). The presets
        // are not persistent state, they just snap the rest of the form to a particular configuration.
        // See RW-5996 for more details.
        return false;
      }}
    />
  );
};
