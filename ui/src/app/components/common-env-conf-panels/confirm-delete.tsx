import * as React from 'react';

import { Button } from 'app/components/buttons';
import { FlexRow } from 'app/components/flex';
import { ClrIcon } from 'app/components/icons';
import colors from 'app/styles/colors';

import { styles } from './styles';

const { useState, Fragment } = React;

interface Props {
  onCancel: () => void;
  onConfirm: () => void;
}
export const ConfirmDelete = ({ onCancel, onConfirm }: Props) => {
  const [deleting, setDeleting] = useState(false);
  return (
    <Fragment>
      <div style={styles.confirmWarning}>
        <div style={{ display: 'flex', justifyContent: 'center' }}>
          <ClrIcon
            style={{ color: colors.warning, gridColumn: 1, gridRow: 1 }}
            className='is-solid'
            shape='exclamation-triangle'
            size='20'
          />
        </div>
        <h3
          style={{
            ...styles.baseHeader,
            ...styles.bold,
            gridColumn: 2,
            gridRow: 1,
          }}
        >
          Delete your environment
        </h3>
        <p style={{ ...styles.confirmWarningText, gridColumn: 2, gridRow: 2 }}>
          You’re about to delete your cloud analysis environment.
        </p>
        <p style={{ ...styles.confirmWarningText, gridColumn: 2, gridRow: 3 }}>
          Any in-memory state and local file modifications will be erased. Data
          stored in workspace buckets is never affected by changes to your cloud
          environment. You’ll still be able to view notebooks in this workspace,
          but editing and running notebooks will require you to create a new
          cloud environment.
        </p>
      </div>
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
          aria-label='Delete'
          disabled={deleting}
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
          Delete
        </Button>
      </FlexRow>
    </Fragment>
  );
};
