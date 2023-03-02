import * as React from 'react';
import { CSSProperties } from 'react';
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
  runtimeStore,
  useStore,
  withStore,
} from 'app/utils/stores';

import { FlexRow } from './flex';

const styles = reactStyles({
  asyncOperationStatusIcon: {
    width: '.75rem',
    height: '.75rem',
    zIndex: 2,
  },
  runtimeStatusIconOutline: {
    border: `1px solid ${colors.white}`,
    borderRadius: '.375rem',
  },
  rotate: {
    animation: 'rotation 2s infinite linear',
  },
});

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

export const RuntimeStatusIcon = fp.flow(
  withStore(compoundRuntimeOpStore, 'compoundRuntimeOps')
)(
  (props: {
    style: CSSProperties;
    compoundRuntimeOps: CompoundRuntimeOpStore;
    workspaceNamespace: string;
    userSuspended: boolean;
  }) => {
    const { style, compoundRuntimeOps, workspaceNamespace, userSuspended } =
      props;

    const { runtime, loadingError } = useStore(runtimeStore);
    let status = runtime?.status;
    if (
      (!status || status === RuntimeStatus.Deleted) &&
      compoundRuntimeOps &&
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
      <FlexRow {...{ style }} data-test-id='runtime-status-icon-container'>
        {(() => {
          if (loadingError || userSuspended) {
            if (
              loadingError instanceof ComputeSecuritySuspendedError ||
              userSuspended
            ) {
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
