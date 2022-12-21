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
import { NewNotebookModal } from 'app/pages/analysis/new-notebook-modal';
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
  const [showSelectAppModal, setShowSelectAppModal] = useState(false);
  const [showJupyterModal, setShowJupyterModal] = useState(false);

  const canWrite = (): boolean => {
    return WorkspacePermissionsUtil.canWrite(props.workspace.accessLevel);
  };

  const closeAllApplicationModal = () => {
    setShowJupyterModal(false);
  };

  const onClose = () => {
    setSelectedApp('');
    closeAllApplicationModal();
    setShowSelectAppModal(false);
  };

  const backToSelectAppModal = () => {
    setShowSelectAppModal(true);
  };

  const onJupyterAppSelect = () => {
    AnalyticsTracker.Notebooks.OpenCreateModal();
    setShowSelectAppModal(false);
    setShowJupyterModal(true);
  };

  const onNext = () => {
    switch (selectedApp) {
      case JUPYTER_APP:
        onJupyterAppSelect();
        break;
    }
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
              aria-label='start'
              data-test-id='start-button'
              style={styles.startButton}
              onClick={() => {
                setShowSelectAppModal(true);
              }}
              disabled={
                workspace.billingStatus === BillingStatus.INACTIVE ||
                !canWrite()
              }
            >
              <div style={{ paddingRight: '0.5rem' }}>Start</div>
              <FontAwesomeIcon icon={faPlusCircle} />
            </Button>
          </FlexRow>
        </FlexColumn>
      </FadeBox>
      {showSelectAppModal && (
        <Modal
          data-test-id='select-application-modal'
          aria={{
            label: 'Select Applications Modal',
          }}
        >
          <ModalTitle>Analyze Data</ModalTitle>
          <ModalBody>
            <div style={styles.appsLabel}>Select an application</div>
            <Dropdown
              data-test-id='application-list-dropdown'
              ariaLabel='Application List Dropdown'
              value={selectedApp}
              options={APP_LIST}
              placeholder='Choose One'
              onChange={(e) => setSelectedApp(e.value)}
              style={{ width: '9rem' }}
            />
          </ModalBody>
          <ModalFooter style={{ paddingTop: '2rem' }}>
            <Button
              style={{ marginRight: '2rem' }}
              type='secondary'
              aria-label='close'
              onClick={() => onClose()}
            >
              Close
            </Button>
            <Button
              data-test-id='next-btn'
              type='primary'
              aria-label='next'
              onClick={() => onNext()}
              disabled={selectedApp === ''}
            >
              Next
            </Button>
          </ModalFooter>
        </Modal>
      )}
      {showJupyterModal && !showSelectAppModal && (
        <NewNotebookModal
          data-test-id='jupyter-modal'
          onClose={() => onClose()}
          workspace={props.workspace}
          existingNameList={null}
          onBack={() => backToSelectAppModal()}
        />
      )}
    </>
  );
});
