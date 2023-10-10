import * as React from 'react';

import { AppType, Disk, PersistentDiskRequest } from 'generated/fetch';

import { styles } from 'app/components/common-env-conf-panels/styles';
import { FlexRow } from 'app/components/flex';
import { TooltipTrigger } from 'app/components/popups';
import { Machine } from 'app/utils/machines';
import { appTypeToString } from 'app/utils/user-apps-utils';

interface Props {
  machine: Machine;
  persistentDiskRequest: Disk | PersistentDiskRequest;
  appType: AppType;
}
export const DisabledCloudComputeProfile = ({
  machine: { cpu, memory },
  persistentDiskRequest: { size },
  appType,
}: Props) => (
  <FlexRow style={{ alignItems: 'center' }}>
    <div style={{ fontWeight: 'bold', marginRight: '0.5rem' }}>
      Cloud compute profile
    </div>
    <TooltipTrigger
      content={`The cloud compute profile for ${appTypeToString[appType]} beta is non-configurable.`}
      side={'right'}
    >
      <div style={styles.disabledCloudProfile}>
        {`${cpu} CPUS, ${memory}GB RAM, ${size}GB disk`}
      </div>
    </TooltipTrigger>
  </FlexRow>
);
