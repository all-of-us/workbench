import * as React from 'react';

import { Disk } from 'generated/fetch';

import { UIAppType } from 'app/components/apps-panel/utils';
import { Button } from 'app/components/buttons';
import { BackupFilesHelpSection } from 'app/components/common-env-conf-panels/backup-files-help-section';
import { styles } from 'app/components/common-env-conf-panels/styles';
import { FlexRow } from 'app/components/flex';
import { ClrIcon } from 'app/components/icons';
import { RadioButton } from 'app/components/inputs';
import colors from 'app/styles/colors';
import { detachableDiskPricePerMonth } from 'app/utils/machines';
import { formatUsd } from 'app/utils/numbers';

const { useState, Fragment } = React;

interface Props {
  onNext: (deleteDetachedDisk: boolean) => void;
  onCancel: () => void;
  disk: Disk;
}
export const OfferDeleteDiskWithUpdate = ({
  onNext,
  onCancel,
  disk,
}: Props) => {
  const [deleteDetachedDisk, setDeleteDetachedDisk] = useState(false);
  return (
    <Fragment>
      <div style={{ display: 'flex', marginRight: '0.75rem' }}>
        <ClrIcon
          style={{ color: colors.warning, marginRight: '0.375rem' }}
          className='is-solid'
          shape='exclamation-triangle'
          size='20'
        />
        <h3 style={{ ...styles.baseHeader, ...styles.bold }}>Disk options</h3>
      </div>
      <div>
        <div>
          Your environment currently has a reattachable disk, which will be
          unused after you apply this update. What would you like to do with it?
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
              data-test-id='keep-pd'
              style={{ display: 'inline-block', marginRight: '0.75rem' }}
            >
              <RadioButton
                style={{ marginRight: '0.375rem' }}
                onChange={() => setDeleteDetachedDisk(false)}
                checked={!deleteDetachedDisk}
              />
              <label>Keep unattached persistent disk</label>
            </div>
          </h3>
          <p
            style={{ ...styles.confirmWarningText, gridColumn: 1, gridRow: 2 }}
          >
            Your disk will be saved for later and can be reattached when you
            next configure a standard VM analysis environment. You will continue
            to incur persistent disk cost at{' '}
            <b>{formatUsd(detachableDiskPricePerMonth(disk))}</b> per month.
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
              data-test-id='delete-pd'
              style={{ display: 'inline-block', marginRight: '0.75rem' }}
            >
              <RadioButton
                //                aria-label='Delete persistent disk'
                style={{ marginRight: '0.375rem' }}
                onChange={() => setDeleteDetachedDisk(true)}
                checked={deleteDetachedDisk}
              />
              <label>Delete persistent disk</label>
            </div>
          </h3>
          <p
            style={{ ...styles.confirmWarningText, gridColumn: 1, gridRow: 2 }}
          >
            Delete your persistent disk, which will also delete all files on the
            disk.
          </p>
        </div>
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
        <Button aria-label={'Next'} onClick={() => onNext(deleteDetachedDisk)}>
          Next
        </Button>
      </FlexRow>
    </Fragment>
  );
};
