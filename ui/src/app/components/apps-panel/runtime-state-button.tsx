import * as React from 'react';

import { Workspace } from 'generated/fetch';

import {
  RuntimeStatusRequest,
  useRuntimeStatus,
} from 'app/utils/runtime-utils';

import { PauseResumeButton } from './pause-resume-button';

export const RuntimeStateButton = (props: { workspace: Workspace }) => {
  const {
    workspace: { namespace, googleProject },
  } = props;

  const [status, setRuntimeStatus] = useRuntimeStatus(namespace, googleProject);

  return (
    <PauseResumeButton
      externalStatus={status}
      onPause={() => setRuntimeStatus(RuntimeStatusRequest.Stop)}
      onResume={() => setRuntimeStatus(RuntimeStatusRequest.Start)}
    />
  );
};
