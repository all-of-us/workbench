import * as React from 'react';

import { Button } from 'app/components/buttons';
import { AnalysisConfig, UpdateMessaging } from 'app/utils/runtime-utils';

interface Props {
  analysisConfig: AnalysisConfig;
  requestAnalysisConfig: (ac: AnalysisConfig) => void;
  runtimeCanBeUpdated: boolean;
  onClose: () => void;
  updateMessaging: UpdateMessaging;
}

export const UpdateButton = ({
  analysisConfig,
  requestAnalysisConfig,
  runtimeCanBeUpdated,
  onClose,
  updateMessaging,
}: Props) => (
  <Button
    aria-label='Update'
    disabled={!runtimeCanBeUpdated}
    onClick={() => {
      requestAnalysisConfig(analysisConfig);
      onClose();
    }}
  >
    {updateMessaging.applyAction}
  </Button>
);
