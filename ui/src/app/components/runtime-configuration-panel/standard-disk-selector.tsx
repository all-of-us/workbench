import * as React from 'react';

import { Disk } from 'generated/fetch';

import { styles } from 'app/components/common-env-conf-panels/styles';
import { FlexColumn, FlexRow } from 'app/components/flex';
import { TooltipTrigger } from 'app/components/popups';
import { maybeWithExistingDiskName } from 'app/utils/analysis-config';
import { DiskConfig } from 'app/utils/runtime-utils';

import { DiskSizeSelector } from './disk-size-selector';

interface Props {
  diskConfig: DiskConfig;
  onChange: (c: DiskConfig) => void;
  disabled: boolean;
  existingDisk: Disk | null;
}

export const StandardDiskSelector = ({
  diskConfig,
  disabled,
  onChange,
  existingDisk,
}: Props) => (
  <TooltipTrigger content='Reattachable disks are unsupported for this compute type'>
    <FlexRow style={styles.diskRow}>
      <FlexColumn>
        <label style={styles.diskLabel}>Standard disk</label>
        <span>
          A standard disk is created and deleted with your cloud environment.
        </span>
        {diskConfig.detachable || (
          <DiskSizeSelector
            {...{ disabled }}
            idPrefix='standard'
            diskSize={diskConfig.size}
            style={{ marginTop: '11px' }}
            onChange={(size: number) =>
              onChange(
                maybeWithExistingDiskName(
                  {
                    ...diskConfig,
                    size,
                  },
                  existingDisk
                )
              )
            }
          />
        )}
      </FlexColumn>
    </FlexRow>
  </TooltipTrigger>
);
