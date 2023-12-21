import * as React from 'react';
import { Dropdown } from 'primereact/dropdown';

import { Disk, DiskType } from 'generated/fetch';

import { StyledExternalLink } from 'app/components/buttons';
import { styles } from 'app/components/common-env-conf-panels/styles';
import { FlexColumn, FlexRow } from 'app/components/flex';
import { RadioButton } from 'app/components/inputs';
import { WarningMessage } from 'app/components/messages';
import { TooltipTrigger } from 'app/components/popups';
import { AoU } from 'app/components/text-wrappers';
import { maybeWithExistingDiskName } from 'app/utils/analysis-config';
import { ComputeType } from 'app/utils/machines';
import { DiskConfig, diskTypeLabels } from 'app/utils/runtime-utils';
import { supportUrls } from 'app/utils/zendesk';

import { DiskSizeSelector } from './disk-size-selector';

interface Props {
  diskConfig: DiskConfig;
  onChange: (c: DiskConfig) => void;
  disabled: boolean;
  existingDisk: Disk | null;
  computeType: string | null;
}
export const DiskSelector = ({
  diskConfig,
  onChange,
  disabled,
  existingDisk,
  computeType,
}: Props) => {
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
      {computeType === ComputeType.Standard ? (
        <>
          <WarningMessage>
            <AoU /> will now only support reattachable persistent disks as the
            storage disk option for Standard VMs and will discontinue standard
            disks. You will continue to use standard disks with Dataproc
            clusters. Refer to the
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
            <RadioButton
              aria-label='Detachable Disk'
              data-test-id='detachable-disk-radio'
              name='detachableDisk'
              style={styles.diskRadio}
              disabled={true}
              onChange={() => {}}
              checked={diskConfig.detachable}
            />
            <FlexColumn>
              <label style={styles.diskLabel}>
                Reattachable persistent disk
              </label>
              <span>
                A reattachable disk is saved even when your compute environment
                is deleted.
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
                      options={[DiskType.STANDARD, DiskType.SSD].map(
                        (value) => ({
                          label: diskTypeLabels[value],
                          value,
                        })
                      )}
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
      ) : (
        <TooltipTrigger content='Reattachable disks are unsupported for this compute type'>
          <FlexRow style={styles.diskRow}>
            <RadioButton
              aria-label='Standard Disk'
              name='standardDisk'
              data-test-id='standard-disk-radio'
              style={styles.diskRadio}
              disabled={true}
              onChange={() => {}}
              checked={!diskConfig.detachable}
            />
            <FlexColumn>
              <label style={styles.diskLabel}>Standard disk</label>
              <span>
                A standard disk is created and deleted with your cloud
                environment.
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
      )}
    </FlexColumn>
  );
};
