import * as React from 'react';
import { useState } from 'react';
import { Dropdown } from 'primereact/dropdown';
import { faPlusCircle } from '@fortawesome/pro-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { BillingStatus } from 'generated/fetch';

import { Button } from 'app/components/buttons';
import {
  Modal,
  ModalBody,
  ModalFooter,
  ModalTitle,
} from 'app/components/modals';
// TODO joel move to components
import { NewNotebookModal } from 'app/pages/analysis/new-notebook-modal';
import colors from 'app/styles/colors';
import { reactStyles } from 'app/utils';
import { AnalyticsTracker } from 'app/utils/analytics';
import { APP_LIST, JUPYTER_APP } from 'app/utils/constants';
import { WorkspaceData } from 'app/utils/workspace-data';
import { WorkspacePermissionsUtil } from 'app/utils/workspace-permissions';

const styles = reactStyles({
  startButton: {
    paddingLeft: '0.75rem',
    height: '3rem',
    backgroundColor: colors.secondary,
  },
  appsLabel: {
    color: colors.primary,
    fontWeight: 600,
    fontSize: '14px',
    lineHeight: '24px',
    paddingBottom: '0.75rem',
  },
});

interface AppSelectorProps {
  workspaceData: WorkspaceData;
}

export const AppSelector = (props: AppSelectorProps) => {
  const { workspaceData } = props;
  const { billingStatus, accessLevel } = workspaceData || {};
  const [selectedApp, setSelectedApp] = useState('');
  const [showSelectAppModal, setShowSelectAppModal] = useState(false);
  const [showJupyterModal, setShowJupyterModal] = useState(false);

  const onClose = () => {
    setSelectedApp('');
    setShowSelectAppModal(false);
    setShowJupyterModal(false);
  };

  const onNext = () => {
    setShowSelectAppModal(false);
    switch (selectedApp) {
      case JUPYTER_APP:
        AnalyticsTracker.Notebooks.OpenCreateModal();
        setShowJupyterModal(true);
        break;
    }
  };

  return (
    <>
      <Button
        aria-label='start'
        data-test-id='start-button'
        style={styles.startButton}
        onClick={() => {
          setShowSelectAppModal(true);
        }}
        disabled={
          billingStatus === BillingStatus.INACTIVE ||
          !WorkspacePermissionsUtil.canWrite(accessLevel)
        }
      >
        <div style={{ paddingRight: '0.75rem' }}>Start</div>
        <FontAwesomeIcon icon={faPlusCircle} />
      </Button>
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
              id='application-list-dropdown'
              data-test-id='application-list-dropdown'
              ariaLabel='Application List Dropdown'
              value={selectedApp}
              appendTo='self'
              options={APP_LIST}
              placeholder='Choose One'
              onChange={(e) => setSelectedApp(e.value)}
              style={{ width: '13.5rem' }}
            />
          </ModalBody>
          <ModalFooter style={{ paddingTop: '3rem' }}>
            <Button
              style={{ marginRight: '3rem' }}
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
          workspace={workspaceData}
          existingNameList={null}
          onBack={() => setShowSelectAppModal(true)}
        />
      )}
    </>
  );
};
