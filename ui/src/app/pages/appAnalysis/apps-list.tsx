import * as React from 'react';
import { useEffect, useState } from 'react';
import { Dropdown } from 'primereact/dropdown';
import { faPlusCircle } from '@fortawesome/pro-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { BillingStatus } from 'generated/fetch';

import { Button } from 'app/components/buttons';
import { FadeBox } from 'app/components/containers';
import { FlexColumn, FlexRow } from 'app/components/flex';
import { ListPageHeader } from 'app/components/headers';
import {
  Modal,
  ModalBody,
  ModalFooter,
  ModalTitle,
} from 'app/components/modals';
import colors from 'app/styles/colors';
import { reactStyles, withCurrentWorkspace } from 'app/utils';
import { AnalyticsTracker } from 'app/utils/analytics';
import { APP_LIST, JUPYTER_APP } from 'app/utils/constants';
import { WorkspacePermissionsUtil } from 'app/utils/workspace-permissions';

const styles = reactStyles({
  fadeBox: {
    margin: 'auto',
    marginTop: '1rem',
    width: '95.7%',
  },
  startButton: {
    paddingLeft: '0.5rem',
    height: '2rem',
    backgroundColor: colors.secondary,
  },
  appsLabel: {
    color: colors.primary,
    fontWeight: 600,
    fontSize: '14px',
    lineHeight: '24px',
    paddingBottom: '0.5rem',
  },
});

export const AppsList = withCurrentWorkspace()((props) => {
  const { workspace } = props;
  const [selectedApp, setSelectedApp] = useState('');
  const [showAppModal, setShowAppModal] = useState(false);
  const [showJupyterModal, setShowJupyterModal] = useState(false);

  const displayJupyterModal = () => {
    setShowAppModal(false);
    setShowJupyterModal(true);
  };

  const canWrite = (): boolean => {
    return WorkspacePermissionsUtil.canWrite(props.workspace.accessLevel);
  };

  const showApplicationModal = () => {
    switch (selectedApp) {
      case JUPYTER_APP: {
        displayJupyterModal();
        break;
      }
    }
  };

  const onClose = () => {
    setShowJupyterModal(false);
    setShowAppModal(false);
  };

  useEffect(() => {
    props.hideSpinner();
  }, []);

  return (
    <>
      <FadeBox style={styles.fadeBox}>
        <FlexColumn>
          <FlexRow>
            <ListPageHeader style={{ paddingRight: '1.5rem' }}>
              Your Analysis
            </ListPageHeader>
            <Button
              data-test-id='start-button'
              style={styles.startButton}
              onClick={() => {
                AnalyticsTracker.Notebooks.OpenCreateModal();
                setShowAppModal(true);
              }}
              disabled={
                workspace.billingStatus === BillingStatus.INACTIVE ||
                !canWrite()
              }
            >
              <div style={{ paddingRight: '0.5rem' }}>Start</div>
              <FontAwesomeIcon icon={faPlusCircle}></FontAwesomeIcon>
            </Button>
          </FlexRow>
        </FlexColumn>
      </FadeBox>
      {showAppModal && (
        <Modal data-test-id='select-application-modal'>
          <ModalTitle>Analysis Data</ModalTitle>
          <ModalBody>
            <div style={styles.appsLabel}>Select an Application</div>
            <Dropdown
              data-test-id={'application-list-dropdown'}
              value={selectedApp}
              options={APP_LIST}
              placeholder={'Choose One'}
              onChange={(e) => setSelectedApp(e.value)}
              style={{ width: '9rem' }}
            />
          </ModalBody>
          <ModalFooter style={{ paddingTop: '2rem' }}>
            <Button
              style={{ marginRight: '2rem' }}
              type={'secondary'}
              label={'Close'}
              onClick={() => onClose()}
            >
              Close
            </Button>
            <Button
              type={'primary'}
              label={'Next'}
              onClick={() => showApplicationModal()}
            >
              Next
            </Button>
          </ModalFooter>
        </Modal>
      )}
      {showJupyterModal && (
        <div>Modal will come here </div>
        // <JupyterModal
        //   onBack={() => closeApplication()}
        //   onCancel={() => cancel()}
        // />
      )}
    </>
  );
});
