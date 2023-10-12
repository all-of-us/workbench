import * as React from 'react';

import { Button } from 'app/components/buttons';
import { AnalysisConfig } from 'app/utils/runtime-utils';

interface Props {
  runtimeCanBeCreated: boolean;
  analysisConfig: AnalysisConfig;
  requestAnalysisConfig: (ac: AnalysisConfig) => void;
  onClose: () => void;
}
export const TryAgainButton = ({
  runtimeCanBeCreated,
  analysisConfig,
  requestAnalysisConfig,
  onClose,
}: Props) => (
  <Button
    aria-label='Try Again'
    disabled={!runtimeCanBeCreated}
    onClick={() => {
      requestAnalysisConfig(analysisConfig);
      onClose();
    }}
  >
    Try Again
  </Button>
);
