import * as React from 'react';

import { UIAppType } from 'app/components/apps-panel/utils';
import { Button } from 'app/components/buttons';
import { FlexRow } from 'app/components/flex';
import { ClrIcon } from 'app/components/icons';
import { RadioButton } from 'app/components/inputs';
import colors from 'app/styles/colors';

import { BackupFilesHelpSection } from './backup-files-help-section';
import { styles } from './styles';

const { useState, Fragment } = React;

export const ConfirmDeleteUnattachedPD = ({
  onConfirm,
  onCancel,
  showCreateMessaging = false,
}) => {
  const [deleting, setDeleting] = useState(false);

  return (
    <Fragment>
      <div style={{ display: 'flex', marginRight: '0.75rem' }}>
        <ClrIcon
          style={{ color: colors.warning, marginRight: '0.375rem' }}
          className='is-solid'
          shape='exclamation-triangle'
          size='20'
        />
        <h3 style={{ ...styles.baseHeader, ...styles.bold }}>
          {showCreateMessaging
            ? 'Environment creation requires deleting your unattached disk'
            : 'Delete environment options'}
        </h3>
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
              data-test-id='delete-unattached-pd-radio'
              style={{ marginRight: '0.375rem' }}
              onChange={() => setDeleting(true)}
              checked={deleting === true}
            />
            <label>Delete persistent disk</label>
          </div>
        </h3>
        <p style={{ ...styles.confirmWarningText, gridColumn: 1, gridRow: 2 }}>
          Deletes your persistent disk, which will also delete all files on the
          disk.
        </p>
        <p style={{ ...styles.confirmWarningText, gridColumn: 1, gridRow: 3 }}>
          If you want to permanently save some files from the disk before
          deleting it, you will need to create a new cloud environment to access
          it.{' '}
        </p>
      </div>
      <BackupFilesHelpSection appType={UIAppType.JUPYTER} />
      <FlexRow style={{ justifyContent: 'flex-end' }}>
        <Button
          type='secondaryLight'
          aria-label={'Cancel'}
          style={{ marginRight: '.9rem' }}
          onClick={() => onCancel()}
        >
          Cancel
        </Button>
        <Button
          aria-label={'Delete'}
          disabled={!deleting}
          onClick={async () => {
            setDeleting(true);
            try {
              await onConfirm();
            } catch (err) {
              setDeleting(false);
              throw err;
            }
          }}
        >
          {showCreateMessaging ? 'Delete and recreate' : 'Delete'}
        </Button>
      </FlexRow>
    </Fragment>
  );
};
