import * as React from 'react';
import { Dropdown } from 'primereact/dropdown';

import { Disk, DiskType } from 'generated/fetch';

import { StyledExternalLink } from 'app/components/buttons';
import { FlexColumn, FlexRow } from 'app/components/flex';
import { RadioButton } from 'app/components/inputs';
import { WarningMessage } from 'app/components/messages';
import { TooltipTrigger } from 'app/components/popups';
import { styles } from 'app/components/runtime-configuration-panel/styles';
import { AoU } from 'app/components/text-wrappers';
import { ComputeType } from 'app/utils/machines';
import {
  DiskConfig,
  diskTypeLabels,
  maybeWithExistingDiskName,
} from 'app/utils/runtime-utils';
import { supportUrls } from 'app/utils/zendesk';

import { DiskSizeSelector } from './disk-size-selector';

export const DiskSelector = ({
  diskConfig,
  onChange,
  disabled,
  disableDetachableReason,
  existingDisk,
  computeType,
}: {
  diskConfig: DiskConfig;
  onChange: (c: DiskConfig) => void;
  disabled: boolean;
  disableDetachableReason: string | null;
  existingDisk: Disk | null;
  computeType: string | null;
}) => {
  return (
    <FlexColumn
      style={{ ...styles.controlSection, gap: '11px', marginTop: '11px' }}
    >
      <FlexColumn>
        {computeType === ComputeType.Dataproc && (
          <span style={{ ...styles.sectionTitle, marginBottom: 0 }}>
            Master Node Configuration
          </span>
        )}
        <FlexRow style={{ gap: '8px' }}>
          <span style={{ ...styles.sectionTitle, marginBottom: 0 }}>
            Storage disk options
          </span>
          <StyledExternalLink href={supportUrls.persistentDisk}>
            View documentation
          </StyledExternalLink>
        </FlexRow>
      </FlexColumn>
      {computeType === ComputeType.Standard && (
        <WarningMessage>
          <AoU /> will now only support reattachable persistent disks as the
          storage disk option for Standard VMs and will discontinue standard
          disks. You will continue to use standard disks with Dataproc clusters.
          Refer to the
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
      )}
      <TooltipTrigger
        content={
          'To creating a new  Standard VM environment, you will have to use a reattachable persistent disk'
        }
        disabled={computeType !== ComputeType.Standard}
      >
        <FlexRow style={styles.diskRow}>
          <RadioButton
            name='standardDisk'
            data-test-id='standard-disk-radio'
            style={styles.diskRadio}
            disabled={disabled || computeType === ComputeType.Standard}
            onChange={() =>
              onChange({
                ...diskConfig,
                detachable: false,
                detachableType: null,
                existingDiskName: null,
              })
            }
            checked={
              computeType !== ComputeType.Standard && !diskConfig.detachable
            }
          />
          <FlexColumn>
            <label style={styles.diskLabel}>Standard disk</label>
            <span>
              A standard disk is created and deleted with your cloud
              environment.
            </span>
            {diskConfig.detachable || (
              <DiskSizeSelector
                idPrefix='standard'
                diskSize={diskConfig.size}
                disabled={disabled}
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
      <TooltipTrigger
        content={disableDetachableReason}
        disabled={!disableDetachableReason}
      >
        <FlexRow style={styles.diskRow}>
          <RadioButton
            data-test-id='detachable-disk-radio'
            name='detachableDisk'
            style={styles.diskRadio}
            onChange={() =>
              onChange(
                maybeWithExistingDiskName(
                  {
                    ...diskConfig,
                    size: existingDisk?.size || diskConfig.size,
                    detachable: true,
                    detachableType: existingDisk?.diskType || DiskType.Standard,
                  },
                  existingDisk
                )
              )
            }
            checked={
              computeType === ComputeType.Standard || diskConfig.detachable
            }
            disabled={disabled || !!disableDetachableReason}
          />
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
                    options={[DiskType.Standard, DiskType.Ssd].map((value) => ({
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
      </TooltipTrigger>
    </FlexColumn>
  );
};
