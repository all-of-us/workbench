import * as React from 'react';
import { Dropdown } from 'primereact/dropdown';

import { Disk, DiskType } from 'generated/fetch';

import { StyledExternalLink } from 'app/components/buttons';
import { FlexColumn, FlexRow } from 'app/components/flex';
import { WarningIcon } from 'app/components/icons';
import { RadioButton } from 'app/components/inputs';
import { TooltipTrigger } from 'app/components/popups';
import { styles } from 'app/components/runtime-configuration-panel/styles';
import { AoU } from 'app/components/text-wrappers';
import colors, { colorWithWhiteness } from 'app/styles/colors';
import { reactStyles } from 'app/utils';
import { ComputeType } from 'app/utils/machines';
import {
  DiskConfig,
  diskTypeLabels,
  maybeWithExistingDiskName,
} from 'app/utils/runtime-utils';
import { supportUrls } from 'app/utils/zendesk';

import { DiskSizeSelector } from './disk-size-selector';

const warningBoxStyle = reactStyles({
  box: {
    boxSizing: 'border-box',
    height: '75px',
    border: '0.5px solid rgba(38,34,98,0.5)',
    borderRadius: '5px',
    backgroundColor: colorWithWhiteness(colors.warning, 0.9),
    alignItems: 'center',
    justifyContent: 'center',
    marginLeft: 'auto',
    gap: '24.5px',
    paddingLeft: '24.5px',
    paddingRight: '24.5px',
  },
  icon: {
    height: '3rem',
    width: '3rem',
    color: colors.warning,
    fontFamily: 'Font Awesome 5 Pro',
    fontSize: '25px',
    letterSpacing: 0,
    lineHeight: '25px',
    alignItems: 'center',
  },
});

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
      {!disableDetachableReason && (
        <FlexRow style={warningBoxStyle.box}>
          <WarningIcon style={warningBoxStyle.icon} />
          <div>
            <AoU /> will now only support re-attachable persistent disks as the
            storage disk option and will discontinue standard disks. Refer to
            the
            <a
              target='_blank'
              href={
                'https://support.researchallofus.org/hc/en-us/articles/360041683891'
              }
            >
              {' '}
              article{' '}
            </a>{' '}
            to learn more.
          </div>
        </FlexRow>
      )}
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
      <TooltipTrigger
        content={'We are removing the ability of using standard disk type '}
        disabled={disableDetachableReason}
      >
        <FlexRow style={styles.diskRow}>
          <RadioButton
            name='standardDisk'
            data-test-id='standard-disk-radio'
            style={styles.diskRadio}
            disabled={disabled || !disableDetachableReason}
            onChange={() =>
              onChange({
                ...diskConfig,
                detachable: false,
                detachableType: null,
                existingDiskName: null,
              })
            }
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
                idPrefix='standard'
                diskSize={diskConfig.size}
                disabled={!disableDetachableReason}
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
            checked={diskConfig.detachable}
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
