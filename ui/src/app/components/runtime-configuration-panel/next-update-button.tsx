import * as React from 'react';

import { Button } from 'app/components/buttons';
import { PanelContent } from 'app/utils/runtime-utils';

interface Props {
  runtimeCanBeUpdated: boolean;
  updateYieldsUnusedDisk: boolean;
  setPanelContent: (pc: PanelContent) => void;
}
export const NextUpdateButton = ({
  runtimeCanBeUpdated,
  updateYieldsUnusedDisk,
  setPanelContent,
}: Props) => (
  <Button
    aria-label='Next'
    disabled={!runtimeCanBeUpdated}
    onClick={() => {
      if (updateYieldsUnusedDisk) {
        setPanelContent(PanelContent.ConfirmUpdateWithDiskDelete);
      } else {
        setPanelContent(PanelContent.ConfirmUpdate);
      }
    }}
  >
    Next
  </Button>
);
