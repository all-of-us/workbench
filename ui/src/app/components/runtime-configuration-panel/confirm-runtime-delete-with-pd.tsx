import * as React from 'react';

import { Disk } from 'generated/fetch';

import { UIAppType } from 'app/components/apps-panel/utils';
import { Button } from 'app/components/buttons';
import { FlexRow } from 'app/components/flex';
import { ClrIcon } from 'app/components/icons';
import { RadioButton } from 'app/components/inputs';
import colors from 'app/styles/colors';
import { DEFAULT, switchCase } from 'app/utils';
import { detachableDiskPricePerMonth } from 'app/utils/machines';
import { formatUsd } from 'app/utils/numbers';

import { BackupFilesHelpSection } from './backup-files-help-section';
import { styles } from './styles';

const { useState, Fragment } = React;

interface ConfirmDeleteRuntimeWithPDProps {
  onCancel: () => void;
  onConfirm: (deletePDSelected: boolean) => Promise<void>;
  appType: UIAppType;
  // assumption: usingDataproc is always false for GKE apps
  usingDataproc: boolean;
  disk: Disk;
}

export const ConfirmDeleteRuntimeWithPD = ({
  onCancel,
  onConfirm,
  appType,
  usingDataproc,
  disk,
}: ConfirmDeleteRuntimeWithPDProps) => {
  const [deleting, setDeleting] = useState(false);
  const [deletePDSelected, setDeletePDSelected] = useState(false);

  const volumeHome = switchCase(
    appType,
    [UIAppType.JUPYTER, () => '/home/jupyter'],
    [UIAppType.RSTUDIO, () => '/home/rstudio'],
    [DEFAULT, () => null]
  );

  const standardvmDeleteOption = (
    <div>
      <div style={styles.confirmWarning}>
        <h3
          style={{
            ...styles.baseHeader,
            ...styles.bold,
            gridColumn: 1,
            gridRow: 1,
          }}
        >
          <div style={{ display: 'inline-block', marginRight: '0.75rem' }}>
            <RadioButton
              style={{ marginRight: '0.375rem' }}
              onChange={() => setDeletePDSelected(false)}
              checked={!deletePDSelected}
            />
            <label>Keep persistent disk, delete environment</label>
          </div>
        </h3>
        {appType !== UIAppType.CROMWELL && (
          <p
            style={{ ...styles.confirmWarningText, gridColumn: 1, gridRow: 2 }}
          >
            Please save your analysis data in the directory {volumeHome} to
            ensure it’s stored on your disk.
          </p>
        )}
        <p style={{ ...styles.confirmWarningText, gridColumn: 1, gridRow: 3 }}>
          Deletes your analysis environment, but detaches your persistent disk
          and saves it for later. The disk will be automatically reattached the
          next time you create a cloud environment using the standard VM compute
          type within this workspace.
        </p>
        <p style={{ ...styles.confirmWarningText, gridColumn: 1, gridRow: 4 }}>
          You will continue to incur persistent disk cost at{' '}
          <b>{formatUsd(detachableDiskPricePerMonth(disk))}</b> per month. You
          can delete your disk at any time via the runtime panel.
        </p>
      </div>
      <div style={styles.confirmWarning}>
        <h3
          style={{
            ...styles.baseHeader,
            ...styles.bold,
            gridColumn: 1,
            gridRow: 1,
          }}
        >
          <div style={{ display: 'inline-block', marginRight: '0.75rem' }}>
            <RadioButton
              data-test-id='delete-environment-and-pd'
              style={{ marginRight: '0.375rem' }}
              onChange={() => setDeletePDSelected(true)}
              checked={deletePDSelected}
            />
            <label>Delete persistent disk and environment</label>
          </div>
        </h3>
        <p style={{ ...styles.confirmWarningText, gridColumn: 1, gridRow: 2 }}>
          Deletes your persistent disk, which will also delete all files on the
          disk. Also deletes your analysis environment.
        </p>
      </div>
    </div>
  );
  const dataprocDeleteOption = (
    <div>
      <div style={styles.confirmWarning}>
        <h3
          style={{
            ...styles.baseHeader,
            ...styles.bold,
            gridColumn: 1,
            gridRow: 1,
          }}
        >
          <div
            data-test-id='delete-runtime'
            style={{ display: 'inline-block', marginRight: '0.75rem' }}
          >
            <RadioButton
              style={{ marginRight: '0.375rem' }}
              onChange={() => setDeletePDSelected(false)}
              checked={!deletePDSelected}
            />
            <label>
              Delete application configuration and cloud compute profile
            </label>
          </div>
        </h3>
        <p style={{ ...styles.confirmWarningText, gridColumn: 1, gridRow: 2 }}>
          You’re about to delete your cloud analysis environment.
        </p>
        <p style={{ ...styles.confirmWarningText, gridColumn: 1, gridRow: 3 }}>
          Deletes your application configuration and cloud compute profile. This
          will also delete all files on the built-in hard disk.
        </p>
      </div>
      <div style={styles.confirmWarning}>
        <h3
          style={{
            ...styles.baseHeader,
            ...styles.bold,
            gridColumn: 1,
            gridRow: 1,
          }}
        >
          <div
            data-test-id='delete-unattached-pd'
            style={{ display: 'inline-block', marginRight: '0.75rem' }}
          >
            <RadioButton
              style={{ marginRight: '0.375rem' }}
              onChange={() => setDeletePDSelected(true)}
              checked={deletePDSelected}
            />
            <label>Delete unattached persistent disk</label>
          </div>
        </h3>
        <p style={{ ...styles.confirmWarningText, gridColumn: 1, gridRow: 2 }}>
          Deletes your unattached persistent disk, which will also delete all
          files on the disk.
        </p>
        <p style={{ ...styles.confirmWarningText, gridColumn: 1, gridRow: 3 }}>
          Since the persistent disk is not attached, the application
          configuration and cloud compute profile will remain.
        </p>
        <p style={{ ...styles.confirmWarningText, gridColumn: 1, gridRow: 4 }}>
          You will continue to incur persistent disk cost at{' '}
          <b>{formatUsd(detachableDiskPricePerMonth(disk))}</b> per month.
        </p>
      </div>
    </div>
  );
  return (
    <div id='confirm-delete-environment-with-pd-panel'>
      <div style={{ display: 'flex', marginRight: '0.75rem' }}>
        <ClrIcon
          style={{ color: colors.warning, marginRight: '0.375rem' }}
          className='is-solid'
          shape='exclamation-triangle'
          size='20'
        />
        <h3 style={{ ...styles.baseHeader, ...styles.bold }}>
          Delete environment options
        </h3>
      </div>
      {usingDataproc ? dataprocDeleteOption : standardvmDeleteOption}
      <BackupFilesHelpSection appType={appType} />
      <FlexRow style={{ justifyContent: 'flex-end' }}>
        <Button
          type='secondaryLight'
          aria-label={'Cancel'}
          disabled={deleting}
          style={{ marginRight: '.9rem' }}
          onClick={() => onCancel()}
        >
          Cancel
        </Button>
        <Button
          aria-label={'Delete'}
          disabled={deleting}
          onClick={async () => {
            setDeleting(true);
            try {
              await onConfirm(deletePDSelected);
            } catch (err) {
              setDeleting(false);
              throw err;
            }
          }}
        >
          Delete
        </Button>
      </FlexRow>
    </div>
  );
};
