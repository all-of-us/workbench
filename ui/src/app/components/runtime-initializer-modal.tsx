import * as React from 'react';
import { useState } from 'react';

import { Runtime } from 'generated/fetch';

import colors, { colorWithWhiteness } from 'app/styles/colors';
import { reactStyles } from 'app/utils';
import { toAnalysisConfig } from 'app/utils/analysis-config';
import { sidebarActiveIconStore } from 'app/utils/navigation';
import { runtimeDiskStore, useStore } from 'app/utils/stores';

import { Button, Clickable } from './buttons';
import { EnvironmentCostEstimator } from './common-env-conf-panels/environment-cost-estimator';
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
  const { gcePersistentDisk } = useStore(runtimeDiskStore);

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
          which will be charged to this workspace. Would you like to continue
          with this default environment?'
        </WarningMessage>
        <EnvironmentCostEstimator
          analysisConfig={defaultAnalysisConfig}
          isGKEApp={false}
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
        <Button type='secondary' onClick={() => cancel()}>
          Cancel
        </Button>
        <Button
          type='secondary'
          onClick={() => {
            sidebarActiveIconStore.next('runtimeConfig');
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
