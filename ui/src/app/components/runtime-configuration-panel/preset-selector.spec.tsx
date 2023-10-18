import '@testing-library/jest-dom';

import * as React from 'react';

import { render, screen, waitFor } from '@testing-library/react';

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
  //
  // beforeEach(async () => {
  //   // serverConfigStore.set({
  //   //   config: defaultServerConfig,
  //   // });
  // });

  it('should render', async () => {
    await component();
    expect(
      screen.queryByLabelText(/Recommended environments/)
    ).toBeInTheDocument();
  });
});
