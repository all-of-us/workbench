import * as React from 'react';

import { AppType } from 'generated/fetch';

import { FlexRow } from 'app/components/flex';
import { TooltipTrigger } from 'app/components/popups';
import { styles } from 'app/components/runtime-configuration-panel/styles';
import { appTypeToString } from 'app/utils/user-apps-utils';

interface DisabledCloudComputeProfileProps {
  cpu: number;
  memory: number;
  persistentDiskRequestSize: number;
  appType: AppType;
}

export function DisabledCloudComputeProfile({
  cpu,
  memory,
  persistentDiskRequestSize,
  appType,
}: DisabledCloudComputeProfileProps) {
  return (
    <FlexRow style={{ alignItems: 'center' }}>
      <div style={{ fontWeight: 'bold', marginRight: '0.5rem' }}>
        Cloud compute profile
      </div>
      <TooltipTrigger
        content={`The cloud compute profile for ${appTypeToString[appType]} beta is non-configurable.`}
        side={'right'}
      >
        <div style={styles.disabledCloudProfile}>
          {`${cpu} CPUS, ${memory}GB RAM, ${persistentDiskRequestSize}GB disk`}
        </div>
      </TooltipTrigger>
    </FlexRow>
  );
}
