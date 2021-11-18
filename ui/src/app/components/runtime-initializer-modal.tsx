import {Modal, ModalBody, ModalFooter, ModalTitle} from './modals';
import * as React from 'react';
import { Button, Clickable } from './buttons';
import { Runtime, RuntimeConfigurationType } from 'generated/fetch';
import colors, { colorWithWhiteness } from 'app/styles/colors';
import { RuntimeCostEstimator } from './runtime-cost-estimator';
import { RuntimeSummary } from './runtime-summary';
import { toRuntimeConfig } from 'app/utils/runtime-utils';

import {useState} from 'react';
import { ClrIcon } from './icons';
import { reactStyles } from 'app/utils';
import { setSidebarActiveIconStore } from 'app/utils/navigation';
import { WarningMessage } from './messages';
import { serverConfigStore } from 'app/utils/stores';


const styles = reactStyles({
  bodyElement: {
    marginTop: '15px'
  },
  runtimeDetails: {
    border: `1px solid ${colorWithWhiteness(colors.dark, 0.8)}`,
    borderRadius: '5px',
    marginTop: '10px',
    padding: '10px'
  }
});

interface Props {
  cancel: () => void;
  createAndContinue: () => void;
  defaultRuntime: Runtime;
}

export const RuntimeInitializerModal = ({cancel, createAndContinue, defaultRuntime}: Props) => {
  const [showDetails, setShowDetails] = useState(false);

  const defaultRuntimeConfig = toRuntimeConfig(defaultRuntime);
  return <Modal width={600}>
    <ModalTitle>Create an Analysis Environment</ModalTitle>
    <ModalBody>
      <WarningMessage iconPosition="top">
        Continuing with this action requires a cloud analysis environment, which will be charged
        to this workspace.&nbsp;
        {defaultRuntime.configurationType === RuntimeConfigurationType.GeneralAnalysis ?
           'Would you like to continue with this default environment?' :
           'Would you like to continue with your most recently used environment settings in this workspace?'}
      </WarningMessage>
      <RuntimeCostEstimator
        runtimeParameters={defaultRuntimeConfig}
        usePersistentDisk={serverConfigStore.get().config.enablePersistentDisk}
        style={{...styles.bodyElement, justifyContent: 'space-evenly'}} />
      <Clickable onClick={() => setShowDetails(!showDetails)} style={styles.bodyElement} >
        Environment details<ClrIcon shape='angle' style={{transform: showDetails ? 'rotate(180deg)' : 'rotate(90deg)'}} />
      </Clickable>
     {showDetails &&
       <div style={styles.runtimeDetails}>
         <RuntimeSummary runtimeConfig={defaultRuntimeConfig} />
         <div style={{marginTop: '10px'}}>To change this configuration, click 'Configure' below.</div>
       </div>}
    </ModalBody>
    <ModalFooter style={{justifyContent: 'space-between'}}>
      <Button
          data-test-id='runtime-intializer-cancel'
          type='secondary'
          onClick={() => cancel()}
      >
        Cancel
      </Button>
      <Button
          data-test-id='runtime-intializer-configure'
          type='secondary'
          onClick={() => {
            setSidebarActiveIconStore.next('runtime');
            cancel();
          }}
      >
        Configure
      </Button>
      <Button
        data-test-id='runtime-intializer-create'
        onClick={() => {
          createAndContinue();
        }}>
        Create Environment
      </Button>
    </ModalFooter>
  </Modal>
}
