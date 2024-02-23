import * as React from 'react';
import { Dropdown } from 'primereact/dropdown';

import { DiskType } from 'generated/fetch';

import { styles } from 'app/components/common-env-conf-panels/styles';
import { FlexColumn, FlexRow } from 'app/components/flex';
import { DiskConfig, diskTypeLabels } from 'app/utils/runtime-utils';

import { DiskSizeSelector } from './disk-size-selector';

interface Props {
  diskConfig: DiskConfig;
  onChange: (change: Partial<DiskConfig>) => void;
  disabled: boolean;
}
export const PersistentDiskSelector = ({
  diskConfig,
  disabled,
  onChange,
}: Props) => (
  <>
    <FlexRow style={styles.diskRow}>
      <FlexColumn>
        <label style={styles.diskLabel}>Reattachable persistent disk</label>
        <span>
          A reattachable disk is saved even when your compute environment is
          deleted.
        </span>
        {diskConfig.detachable && (
          <FlexRow style={{ ...styles.formGrid2, marginTop: '11px' }}>
            <FlexRow style={styles.labelAndInput}>
              <label
                style={{ ...styles.label, minWidth: '4.5rem' }}
                htmlFor='disk-type'
              >
                Disk type
              </label>
              <Dropdown
                {...{ disabled }}
                id={'disk-type'}
                options={[DiskType.STANDARD, DiskType.SSD].map((value) => ({
                  label: diskTypeLabels[value],
                  value,
                }))}
                style={{ width: '150px' }}
                onChange={({ value }) => onChange({ detachableType: value })}
                value={diskConfig.detachableType}
              />
            </FlexRow>
            <DiskSizeSelector
              {...{ disabled }}
              idPrefix='detachable'
              diskSize={diskConfig.size}
              onChange={(size: number) => onChange({ size })}
            />
          </FlexRow>
        )}
      </FlexColumn>
    </FlexRow>
  </>
);
