import * as React from 'react';
import { CSSProperties } from 'react';
import * as fp from 'lodash/fp';

import { RuntimeStatus } from 'generated/fetch';

import { ComputeSecuritySuspendedError } from 'app/utils/runtime-utils';
import {
  CompoundRuntimeOpStore,
  compoundRuntimeOpStore,
  runtimeStore,
  useStore,
  withStore,
} from 'app/utils/stores';

import { FlexRow } from './flex';
import {
  ErrorIcon,
  RunningIcon,
  StoppedIcon,
  StoppingIcon,
  SuspendedIcon,
  UpdatingIcon,
} from './status-icon';

export const RuntimeStatusIndicator = fp.flow(
  withStore(compoundRuntimeOpStore, 'compoundRuntimeOps')
)(
  (props: {
    style?: CSSProperties;
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
              return <SuspendedIcon />;
            }
            return <ErrorIcon />;
          }
          switch (status) {
            case RuntimeStatus.Creating:
            case RuntimeStatus.Starting:
            case RuntimeStatus.Updating:
              return <UpdatingIcon />;
            case RuntimeStatus.Stopped:
              return <StoppedIcon />;
            case RuntimeStatus.Running:
              return <RunningIcon />;
            case RuntimeStatus.Stopping:
            case RuntimeStatus.Deleting:
              return <StoppingIcon />;
            case RuntimeStatus.Error:
              return <ErrorIcon />;
          }
        })()}
      </FlexRow>
    );
  }
);
