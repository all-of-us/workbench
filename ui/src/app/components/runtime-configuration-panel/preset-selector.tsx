import * as React from 'react';
import * as fp from 'lodash/fp';
import { Dropdown } from 'primereact/dropdown';

import colors from 'app/styles/colors';
import { runtimePresets } from 'app/utils/runtime-presets';
import { toAnalysisConfig } from 'app/utils/runtime-utils';

// Select a recommended preset configuration.
export const PresetSelector = ({
  allowDataproc,
  setAnalysisConfig,
  disabled,
  persistentDisk,
}) => {
  return (
    <Dropdown
      id='runtime-presets-menu'
      disabled={disabled}
      style={{
        marginTop: '21px',
        display: 'inline-block',
        color: colors.primary,
      }}
      placeholder='Recommended environments'
      options={fp.flow(
        fp.values,
        fp.filter(
          ({ runtimeTemplate }) =>
            allowDataproc || !runtimeTemplate.dataprocConfig
        ),
        fp.map(({ displayName, runtimeTemplate }) => ({
          label: displayName,
          value: runtimeTemplate,
        }))
      )(runtimePresets)}
      onChange={({ value }) => {
        setAnalysisConfig(toAnalysisConfig(value, persistentDisk));

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
