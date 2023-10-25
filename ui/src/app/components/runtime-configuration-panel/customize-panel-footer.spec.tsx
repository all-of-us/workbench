import '@testing-library/jest-dom';

import * as React from 'react';

import { render, screen, waitFor } from '@testing-library/react';
import { serverConfigStore } from 'app/utils/stores';

import defaultServerConfig from 'testing/default-server-config';

import {
  CustomizePanelFooter,
  CustomizePanelFooterProps,
} from './customize-panel-footer';

const defaultProps: CustomizePanelFooterProps = {
  analysisConfig: undefined,
  currentRuntime: undefined,
  disableControls: undefined,
  existingAnalysisConfig: undefined,
  onClose: undefined,
  requestAnalysisConfig: undefined,
  runtimeCanBeCreated: undefined,
  runtimeCanBeUpdated: undefined,
  runtimeExists: undefined,
  setPanelContent: undefined,
  unattachedDiskNeedsRecreate: undefined,
  unattachedPdExists: undefined,
};

describe(CustomizePanelFooter.name, () => {
  const component = async (
    propOverrides?: Partial<CustomizePanelFooterProps>
  ) =>
    render(<CustomizePanelFooter {...{ ...defaultProps, ...propOverrides }} />);

  beforeEach(async () => {
    serverConfigStore.set({
      config: defaultServerConfig,
    });
  });

  it('renders', async () => {
    await component({});
    expect(screen.queryByText(/Delete Environment/)).toBeInTheDocument();
  });
});
