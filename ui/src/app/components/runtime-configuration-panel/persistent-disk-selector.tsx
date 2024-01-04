import * as React from 'react';
import { Dropdown } from 'primereact/dropdown';

import { Disk, DiskType } from 'generated/fetch';

import { styles } from 'app/components/common-env-conf-panels/styles';
import { FlexColumn, FlexRow } from 'app/components/flex';
import { WarningMessage } from 'app/components/messages';
import { AoU } from 'app/components/text-wrappers';
import { maybeWithExistingDiskName } from 'app/utils/analysis-config';
import { DiskConfig, diskTypeLabels } from 'app/utils/runtime-utils';

import { DiskSizeSelector } from './disk-size-selector';

interface Props {
  diskConfig: DiskConfig;
  onChange: (c: DiskConfig) => void;
  disabled: boolean;
  existingDisk: Disk | null;
}

export const PersistentDiskSelector = ({
  diskConfig,
  disabled,
  onChange,
  existingDisk,
}: Props) => (
  <>
    <WarningMessage>
      <AoU /> will now only support reattachable persistent disks as the storage
      disk option for Standard VMs and will discontinue standard disks. You will
      continue to use standard disks with Dataproc clusters. Refer to the
      <a
        href={
          'https://support.researchallofus.org/hc/en-us/articles/5140493753620-Persistent-Disk'
        }
        target='_blank'
      >
        {' '}
        article{' '}
      </a>{' '}
      to learn more.
    </WarningMessage>
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
                id={'disk-type'}
                options={[DiskType.STANDARD, DiskType.SSD].map((value) => ({
                  label: diskTypeLabels[value],
                  value,
                }))}
                style={{ width: '150px' }}
                disabled={disabled}
                onChange={({ value }) =>
                  onChange(
                    maybeWithExistingDiskName(
                      {
                        ...diskConfig,
                        detachableType: value,
                      },
                      existingDisk
                    )
                  )
                }
                value={diskConfig.detachableType}
              />
            </FlexRow>
            <DiskSizeSelector
              idPrefix='detachable'
              diskSize={diskConfig.size}
              disabled={disabled}
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
          </FlexRow>
        )}
      </FlexColumn>
    </FlexRow>
  </>
);
