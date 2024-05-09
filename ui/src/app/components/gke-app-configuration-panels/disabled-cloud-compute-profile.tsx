import * as React from 'react';

import { AppType, Disk, PersistentDiskRequest } from 'generated/fetch';

import { styles } from 'app/components/common-env-conf-panels/styles';
import { FlexColumn, FlexRow } from 'app/components/flex';
import { TooltipTrigger } from 'app/components/popups';
import { Machine } from 'app/utils/machines';
import { appTypeToString } from 'app/utils/user-apps-utils';

interface Props {
  machine: Machine;
  persistentDiskRequest: Disk | PersistentDiskRequest;
  appType: AppType;
  otherAppsString: string;
  disabledText?: string;
}
export const DisabledCloudComputeProfile = ({
  machine: { cpu, memory },
  persistentDiskRequest: { size },
  appType,
  otherAppsString,
  disabledText = `The cloud compute profile for ${appTypeToString[appType]} beta is non-configurable.`,
}: Props) => (
  <FlexColumn style={{ rowGap: '1em' }}>
    <FlexRow style={{ alignItems: 'center' }}>
      <div style={{ fontWeight: 'bold', marginRight: '0.5rem' }}>
        Cloud compute profile
      </div>
      <TooltipTrigger content={disabledText} side={'right'}>
        <div style={styles.disabledCloudProfile}>
          {`${cpu} CPUS, ${memory}GB RAM, ${size}GB disk`}
        </div>
      </TooltipTrigger>
    </FlexRow>
    <div>
      Your {appTypeToString[appType]} environment will share CPU and RAM
      resources with any {otherAppsString} environments you run in this
      workspace.
    </div>
  </FlexColumn>
);
