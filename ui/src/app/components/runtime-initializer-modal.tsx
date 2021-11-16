import {Modal, ModalBody, ModalFooter, ModalTitle} from './modals';
import * as React from 'react';
import { Button } from './buttons';
import { Runtime, RuntimeConfigurationType } from 'generated/fetch';
import { RuntimeCostEstimator } from './runtime-cost-estimator';
import { RuntimeSummary } from './runtime-summary';
import { toRuntimeConfig } from 'app/utils/runtime-utils';

interface Props {
  cancel: () => void;
  openRuntimePanel: () => void;
  createDefault: () => void;
  defaultRuntime: Runtime;
}

export const RuntimeInitializerModal = ({cancel, openRuntimePanel, createDefault, defaultRuntime}: Props) => {
  const defaultRuntimeConfig = toRuntimeConfig(defaultRuntime);
  return <Modal>
    <ModalTitle>Create an Analysis Environment</ModalTitle>
    <ModalBody>
      <div>
        Continuing with this action requires a cloud analysis environment, which will be charged
        to this workspace.&nbsp;
        {defaultRuntime.configurationType === RuntimeConfigurationType.GeneralAnalysis ?
           'Would you like to continue with this default environment?' :
           'Would you like to continue with your most recently used environment settings in this workspace?'}
      </div>
      <RuntimeCostEstimator runtimeParameters={defaultRuntimeConfig} />
      <RuntimeSummary runtimeConfig={defaultRuntimeConfig} />
    </ModalBody>
    <ModalFooter style={{justifyContent: 'space-between'}}>
      <Button
          type='secondary'
          onClick={() => cancel()}
      >
        Cancel
      </Button>
      <Button
          type='secondary'
          onClick={() => {
            openRuntimePanel();
          }}
      >
        Edit
      </Button>
      <Button onClick={() => {
        createDefault();
      }}>
        Create Environment
      </Button>
    </ModalFooter>
  </Modal>
}
