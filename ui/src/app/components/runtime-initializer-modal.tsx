import * as React from 'react';
import { useState } from 'react';

import { Runtime, RuntimeConfigurationType } from 'generated/fetch';

import colors, { colorWithWhiteness } from 'app/styles/colors';
import { reactStyles } from 'app/utils';
import { setSidebarActiveIconStore } from 'app/utils/navigation';
import { toAnalysisConfig } from 'app/utils/runtime-utils';
import { diskStore, useStore } from 'app/utils/stores';

import { Button, Clickable } from './buttons';
import { EnvironmentCostEstimator } from './environment-cost-estimator';
import { ClrIcon } from './icons';
import { WarningMessage } from './messages';
import { Modal, ModalBody, ModalFooter, ModalTitle } from './modals';
import { RuntimeSummary } from './runtime-summary';

const styles = reactStyles({
  bodyElement: {
    marginTop: '15px',
  },
  runtimeDetails: {
    border: `1px solid ${colorWithWhiteness(colors.dark, 0.8)}`,
    borderRadius: '5px',
    marginTop: '10px',
    padding: '10px',
  },
});

interface Props {
  cancel: () => void;
  createAndContinue: () => void;
  defaultRuntime: Runtime;
}

export const RuntimeInitializerModal = ({
  cancel,
  createAndContinue,
  defaultRuntime,
}: Props) => {
  const [showDetails, setShowDetails] = useState(false);
  const { gcePersistentDisk } = useStore(diskStore);

  const defaultAnalysisConfig = toAnalysisConfig(
    defaultRuntime,
    gcePersistentDisk
  );
  return (
    <Modal width={600}>
      <ModalTitle>Create an Analysis Environment</ModalTitle>
      <ModalBody>
        <WarningMessage iconPosition='top'>
          Continuing with this action requires a cloud analysis environment,
          which will be charged to this workspace.&nbsp;
          {defaultRuntime.configurationType ===
          RuntimeConfigurationType.GeneralAnalysis
            ? 'Would you like to continue with this default environment?'
            : 'Would you like to continue with your most recently used environment settings in this workspace?'}
        </WarningMessage>
        <EnvironmentCostEstimator
          analysisConfig={defaultAnalysisConfig}
          style={{ ...styles.bodyElement, justifyContent: 'space-evenly' }}
        />
        <Clickable
          onClick={() => setShowDetails(!showDetails)}
          style={styles.bodyElement}
        >
          Environment details
          <ClrIcon
            shape='angle'
            style={{
              transform: showDetails ? 'rotate(180deg)' : 'rotate(90deg)',
            }}
          />
        </Clickable>
        {showDetails && (
          <div style={styles.runtimeDetails}>
            <RuntimeSummary analysisConfig={defaultAnalysisConfig} />
            <div style={{ marginTop: '10px' }}>
              To change this configuration, click 'Configure' below.
            </div>
          </div>
        )}
      </ModalBody>
      <ModalFooter style={{ justifyContent: 'space-between' }}>
        <Button
          data-test-id='runtime-initializer-cancel'
          type='secondary'
          onClick={() => cancel()}
        >
          Cancel
        </Button>
        <Button
          data-test-id='runtime-initializer-configure'
          type='secondary'
          onClick={() => {
            setSidebarActiveIconStore.next('runtimeConfig');
            cancel();
          }}
        >
          Configure
        </Button>
        <Button
          data-test-id='runtime-initializer-create'
          onClick={() => {
            createAndContinue();
          }}
        >
          Create Environment
        </Button>
      </ModalFooter>
    </Modal>
  );
};
