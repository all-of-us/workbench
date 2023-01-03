import * as React from 'react';

import { Button } from 'app/components/buttons';
import { FlexRow } from 'app/components/flex';
import { ClrIcon } from 'app/components/icons';
import { RadioButton } from 'app/components/inputs';
import colors from 'app/styles/colors';
import { ComputeType, detachableDiskPricePerMonth } from 'app/utils/machines';
import { formatUsd } from 'app/utils/numbers';
import { RuntimeStatusRequest } from 'app/utils/runtime-utils';

import { BackupFilesHelpSection } from './backup-files-help-section';
import { styles } from './styles';

const { useState, Fragment } = React;

export const ConfirmDeleteRuntimeWithPD = ({
  onCancel,
  onConfirm,
  computeType,
  disk,
}) => {
  const [deleting, setDeleting] = useState(false);
  const [runtimeStatusReq, setRuntimeStatusReq] = useState(
    RuntimeStatusRequest.DeleteRuntime
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
          <div
            data-test-id='delete-runtime'
            style={{ display: 'inline-block', marginRight: '0.75rem' }}
          >
            <RadioButton
              style={{ marginRight: '0.375rem' }}
              onChange={() =>
                setRuntimeStatusReq(RuntimeStatusRequest.DeleteRuntime)
              }
              checked={runtimeStatusReq === RuntimeStatusRequest.DeleteRuntime}
            />
            <label>Keep persistent disk, delete environment</label>
          </div>
        </h3>
        <p style={{ ...styles.confirmWarningText, gridColumn: 1, gridRow: 2 }}>
          Please save your analysis data in the directory
          /home/jupyter/notebooks to ensure it’s stored on your disk.
        </p>
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
          <div
            data-test-id='delete-runtime-and-pd'
            style={{ display: 'inline-block', marginRight: '0.75rem' }}
          >
            <RadioButton
              style={{ marginRight: '0.375rem' }}
              onChange={() =>
                setRuntimeStatusReq(RuntimeStatusRequest.DeleteRuntimeAndPD)
              }
              checked={
                runtimeStatusReq === RuntimeStatusRequest.DeleteRuntimeAndPD
              }
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
              onChange={() =>
                setRuntimeStatusReq(RuntimeStatusRequest.DeleteRuntime)
              }
              checked={runtimeStatusReq === RuntimeStatusRequest.DeleteRuntime}
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
              onChange={() =>
                setRuntimeStatusReq(RuntimeStatusRequest.DeletePD)
              }
              checked={runtimeStatusReq === RuntimeStatusRequest.DeletePD}
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
    <Fragment>
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
      {computeType === ComputeType.Standard
        ? standardvmDeleteOption
        : dataprocDeleteOption}
      <BackupFilesHelpSection />
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
              await onConfirm(runtimeStatusReq);
            } catch (err) {
              setDeleting(false);
              throw err;
            }
          }}
        >
          Delete
        </Button>
      </FlexRow>
    </Fragment>
  );
};
