import * as React from 'react';

import { WarningMessage } from 'app/components/messages';
import { TextColumn } from 'app/components/text-column';
import { AoU } from 'app/components/text-wrappers';

export const DisabledPanel = () => {
  return (
    <WarningMessage
      data-test-id='environment-disabled-panel'
      iconSize={16}
      iconPosition={'top'}
    >
      {
        <TextColumn>
          <div style={{ fontWeight: 600 }}>
            Cloud services are disabled for this workspace.
          </div>
          <div style={{ marginTop: '0.75rem' }}>
            You cannot run or edit notebooks in this workspace because billed
            services are disabled for the workspace creator's <AoU /> Researcher
            account.
          </div>
        </TextColumn>
      }
    </WarningMessage>
  );
};
