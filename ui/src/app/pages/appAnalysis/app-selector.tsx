import * as React from 'react';
import { useState } from 'react';
import { Dropdown } from 'primereact/dropdown';

import { BillingStatus } from 'generated/fetch';

import { UIAppType } from 'app/components/apps-panel/utils';
import { Button } from 'app/components/buttons';
import { rstudioConfigIconId } from 'app/components/help-sidebar-icons';
import {
  Modal,
  ModalBody,
  ModalFooter,
  ModalTitle,
} from 'app/components/modals';
import { NewJupyterNotebookModal } from 'app/pages/analysis/new-jupyter-notebook-modal';
import colors from 'app/styles/colors';
import { reactStyles } from 'app/utils';
import { AnalyticsTracker } from 'app/utils/analytics';
import { setSidebarActiveIconStore } from 'app/utils/navigation';
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

export const APP_LIST = [UIAppType.JUPYTER, UIAppType.RSTUDIO];

const enum VisiblePanel {
  None = 'None',
  Selector = 'Selector',
  Jupyter = 'Jupyter',
  RStudio = 'RStudio',
}

interface AppSelectorProps {
  workspace: WorkspaceData;
}

export const AppSelector = (props: AppSelectorProps) => {
  const { workspace } = props;
  const [visiblePanel, setVisiblePanel] = useState(VisiblePanel.None);
  const [selectedApp, setSelectedApp] = useState<UIAppType>(undefined);

  const canCreateApps =
    workspace.billingStatus === BillingStatus.ACTIVE &&
    WorkspacePermissionsUtil.canWrite(workspace.accessLevel);

  const onClose = () => {
    setSelectedApp(undefined);
    setVisiblePanel(VisiblePanel.None);
  };

  const onNext = () => {
    switch (selectedApp) {
      case UIAppType.JUPYTER:
        AnalyticsTracker.Notebooks.OpenCreateModal();
        setVisiblePanel(VisiblePanel.Jupyter);
        break;
      case UIAppType.RSTUDIO:
        // TODO do something more interesting?
        setVisiblePanel(VisiblePanel.None);
        setSidebarActiveIconStore.next(rstudioConfigIconId);
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
          setVisiblePanel(VisiblePanel.Selector);
        }}
        disabled={!canCreateApps}
      >
        <div style={{ width: '9rem', paddingLeft: '1rem' }}>Choose an App</div>
      </Button>
      {visiblePanel === VisiblePanel.Selector && (
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
              onClick={onClose}
            >
              Close
            </Button>
            <Button
              data-test-id='next-btn'
              type='primary'
              aria-label='next'
              onClick={onNext}
              disabled={!selectedApp}
            >
              Next
            </Button>
          </ModalFooter>
        </Modal>
      )}
      {visiblePanel === VisiblePanel.Jupyter && (
        <NewJupyterNotebookModal
          {...{ workspace, onClose }}
          data-test-id='jupyter-modal'
          existingNameList={null}
          onBack={() => {
            setVisiblePanel(VisiblePanel.Selector);
          }}
        />
      )}
    </>
  );
};
