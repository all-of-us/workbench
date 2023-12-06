import '@testing-library/jest-dom';

import * as React from 'react';

import { render, screen, waitFor } from '@testing-library/react';
import { toAnalysisConfig } from 'app/utils/analysis-config';
import { runtimePresets } from 'app/utils/runtime-presets';

import { stubDisk } from 'testing/stubs/disks-api-stub';

import { PresetSelector, PresetSelectorProps } from './preset-selector';

const setAnalysisConfig = jest.fn();

const defaultProps: PresetSelectorProps = {
  allowDataproc: true,
  disabled: false,
  setAnalysisConfig,
  gcePersistentDisk: stubDisk(),
};

describe(PresetSelector.name, () => {
  const component = async (propOverrides?: Partial<PresetSelectorProps>) =>
    render(<PresetSelector {...{ ...defaultProps, ...propOverrides }} />);

  it('should display the preset options', async () => {
    await component();
    const dropdown = screen.queryByLabelText('Recommended environments');
    expect(dropdown).toBeInTheDocument();
    dropdown.click();
    await waitFor(() => {
      expect(
        screen.queryByLabelText(runtimePresets.hailAnalysis.displayName)
      ).toBeInTheDocument();
      expect(
        screen.queryByLabelText(runtimePresets.generalAnalysis.displayName)
      ).toBeInTheDocument();
    });
  });

  test.each([
    [
      runtimePresets.hailAnalysis.displayName,
      toAnalysisConfig(runtimePresets.hailAnalysis.runtimeTemplate, stubDisk()),
    ],
    [
      runtimePresets.generalAnalysis.displayName,
      toAnalysisConfig(
        runtimePresets.generalAnalysis.runtimeTemplate,
        stubDisk()
      ),
    ],
  ])(
    'should allow selection of the %s preset',
    async (presetName, expectedAnalysisConfig) => {
      await component();
      const dropdown = screen.queryByLabelText('Recommended environments');
      expect(dropdown).toBeInTheDocument();
      dropdown.click();

      const preset = await waitFor(() => {
        const _preset = screen.queryByLabelText(presetName);

        expect(_preset).toBeInTheDocument();
        return _preset;
      });

      preset.click();
      await waitFor(() =>
        expect(setAnalysisConfig).toHaveBeenCalledWith(expectedAnalysisConfig)
      );
    }
  );
});
