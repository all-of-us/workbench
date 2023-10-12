import * as React from 'react';

import { Button } from 'app/components/buttons';
import { AnalysisConfig } from 'app/utils/runtime-utils';

interface Props {
  analysisConfig: AnalysisConfig;
  requestAnalysisConfig: (ac: AnalysisConfig) => void;
  runtimeCanBeCreated: boolean;
  onClose: () => void;
}
export const CreateButton = ({
  analysisConfig,
  requestAnalysisConfig,
  runtimeCanBeCreated,
  onClose,
}: Props) => (
  <Button
    aria-label='Create'
    disabled={!runtimeCanBeCreated}
    onClick={() => {
      requestAnalysisConfig(analysisConfig);
      onClose();
    }}
  >
    Create
  </Button>
);
