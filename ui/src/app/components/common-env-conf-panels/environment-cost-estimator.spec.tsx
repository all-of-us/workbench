import '@testing-library/jest-dom';

import * as React from 'react';

import { render, screen } from '@testing-library/react';
import { toAnalysisConfig } from 'app/utils/analysis-config';

import { stubDisk } from 'testing/stubs/disks-api-stub';
import { defaultGceRuntimeWithPd } from 'testing/stubs/runtime-api-stub';

import { EnvironmentCostEstimator } from './environment-cost-estimator';

describe('Estimate cost estimator', () => {
  const defaultAnalysisConfig = toAnalysisConfig(
    defaultGceRuntimeWithPd(),
    stubDisk()
  );

  const renderComponent = (isGkeApp: boolean) =>
    render(
      <EnvironmentCostEstimator
        isGKEApp={isGkeApp}
        analysisConfig={defaultAnalysisConfig}
      />
    );

  it('Should show pause cost if not GKE APPS', () => {
    // Act
    renderComponent(false);

    // Assert
    expect(screen.queryByText('Cost when paused'));
  });

  it('Should not show cost when paused if its GKE APPS', () => {
    // Act
    renderComponent(true);

    // Assert
    expect(screen.queryByText('Cost when paused')).not.toBeInTheDocument();
  });
});
