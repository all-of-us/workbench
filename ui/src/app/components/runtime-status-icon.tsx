import * as React from 'react';
import * as fp from 'lodash/fp';
import { faCircle } from '@fortawesome/free-solid-svg-icons/faCircle';
import { faLock } from '@fortawesome/free-solid-svg-icons/faLock';
import { faSyncAlt } from '@fortawesome/free-solid-svg-icons/faSyncAlt';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { RuntimeStatus } from 'generated/fetch';

import colors from 'app/styles/colors';
import { reactStyles } from 'app/utils';
import { ComputeSecuritySuspendedError } from 'app/utils/runtime-utils';
import {
  CompoundRuntimeOpStore,
  compoundRuntimeOpStore,
  RuntimeStore,
  withStore,
} from 'app/utils/stores';

import { FlexRow } from './flex';

const styles = reactStyles({
  asyncOperationStatusIcon: {
    width: '.5rem',
    height: '.5rem',
    zIndex: 2,
  },
  runtimeStatusIconOutline: {
    border: `1px solid ${colors.white}`,
    borderRadius: '.25rem',
  },
  statusIconContainer: {
    alignSelf: 'flex-end',
    margin: '0 .1rem .1rem auto',
  },
  rotate: {
    animation: 'rotation 2s infinite linear',
  },
});

export const RuntimeStatusIcon = fp.flow(
  withStore(compoundRuntimeOpStore, 'compoundRuntimeOps')
)(
  (props: {
    store: RuntimeStore;
    compoundRuntimeOps: CompoundRuntimeOpStore;
    workspaceNamespace: string;
  }) => {
    const { store, compoundRuntimeOps, workspaceNamespace } = props;

    let status = store?.runtime?.status;
    if (
      (!status || status === RuntimeStatus.Deleted) &&
      workspaceNamespace in compoundRuntimeOps
    ) {
      // If a compound operation is still pending, and we're transitioning
      // through the "Deleted" phase of the runtime, we want to keep showing
      // an activity spinner. Avoids an awkward UX during a delete/create cycle.
      // There also be some lag during the runtime creation flow between when
      // the compound operation starts, and the runtime is set in the store; for
      // this reason use Creating rather than Deleting here.
      status = RuntimeStatus.Creating;
    }

    return (
      <FlexRow
        data-test-id='runtime-status-icon-container'
        style={styles.statusIconContainer}
      >
        {(() => {
          const errIcon = (
            <FontAwesomeIcon
              icon={faCircle}
              style={{
                ...styles.asyncOperationStatusIcon,
                ...styles.runtimeStatusIconOutline,
                color: colors.asyncOperationStatus.error,
              }}
            />
          );

          if (store.loadingError) {
            if (store.loadingError instanceof ComputeSecuritySuspendedError) {
              return (
                <FontAwesomeIcon
                  icon={faLock}
                  style={{
                    ...styles.asyncOperationStatusIcon,
                    color: colors.asyncOperationStatus.stopped,
                  }}
                />
              );
            }
            return errIcon;
          }
          switch (status) {
            case RuntimeStatus.Creating:
            case RuntimeStatus.Starting:
            case RuntimeStatus.Updating:
              return (
                <FontAwesomeIcon
                  icon={faSyncAlt}
                  style={{
                    ...styles.asyncOperationStatusIcon,
                    ...styles.rotate,
                    color: colors.asyncOperationStatus.starting,
                  }}
                />
              );
            case RuntimeStatus.Stopped:
              return (
                <FontAwesomeIcon
                  icon={faCircle}
                  style={{
                    ...styles.asyncOperationStatusIcon,
                    ...styles.runtimeStatusIconOutline,
                    color: colors.asyncOperationStatus.stopped,
                  }}
                />
              );
            case RuntimeStatus.Running:
              return (
                <FontAwesomeIcon
                  icon={faCircle}
                  style={{
                    ...styles.asyncOperationStatusIcon,
                    ...styles.runtimeStatusIconOutline,
                    color: colors.asyncOperationStatus.running,
                  }}
                />
              );
            case RuntimeStatus.Stopping:
            case RuntimeStatus.Deleting:
              return (
                <FontAwesomeIcon
                  icon={faSyncAlt}
                  style={{
                    ...styles.asyncOperationStatusIcon,
                    ...styles.rotate,
                    color: colors.asyncOperationStatus.stopping,
                  }}
                />
              );
            case RuntimeStatus.Error:
              return errIcon;
          }
        })()}
      </FlexRow>
    );
  }
);
