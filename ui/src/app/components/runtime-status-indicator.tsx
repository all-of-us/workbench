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

import {
  DeletingIcon,
  ErrorIcon,
  RunningIcon,
  StoppedIcon,
  StoppingIcon,
  SuspendedIcon,
  UpdatingIcon,
} from './environment-status-icon';
import { FlexRow } from './flex';

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
      !status &&
      compoundRuntimeOps &&
      workspaceNamespace in compoundRuntimeOps
    ) {
      // If a compound operation is still pending, and we're transitioning
      // through the "Deleted" phase of the runtime, we want to keep showing
      // an activity spinner. Avoids an awkward UX during a delete/create cycle.
      // There also be some lag during the runtime creation flow between when
      // the compound operation starts, and the runtime is set in the store; for
      // this reason use Creating rather than Deleting here.
      status = RuntimeStatus.CREATING;
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
            case RuntimeStatus.CREATING:
            case RuntimeStatus.STARTING:
            case RuntimeStatus.UPDATING:
              return <UpdatingIcon />;
            case RuntimeStatus.STOPPED:
              return <StoppedIcon />;
            case RuntimeStatus.RUNNING:
              return <RunningIcon />;
            case RuntimeStatus.DELETING:
              return <DeletingIcon />;
            case RuntimeStatus.STOPPING:
              return <StoppingIcon />;
            case RuntimeStatus.ERROR:
              return <ErrorIcon />;
          }
        })()}
      </FlexRow>
    );
  }
);
