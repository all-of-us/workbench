import * as React from 'react';

import { Button } from 'app/components/buttons';
import { PanelContent } from 'app/utils/runtime-utils';

interface Props {
  runtimeCanBeCreated: boolean;
  setPanelContent: (pc: PanelContent) => void;
}
export const NextWithDiskDeleteButton = ({
  runtimeCanBeCreated,
  setPanelContent,
}: Props) => (
  <Button
    aria-label='Next'
    disabled={!runtimeCanBeCreated}
    onClick={() => {
      setPanelContent(PanelContent.DeleteUnattachedPdAndCreate);
    }}
  >
    Next
  </Button>
);
