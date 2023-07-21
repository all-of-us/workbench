import * as React from 'react';
import { useState } from 'react';

import { BillingStatus } from 'generated/fetch';

import { UIAppType } from 'app/components/apps-panel/utils';
import { Button } from 'app/components/buttons';
import { rstudioConfigIconId } from 'app/components/help-sidebar-icons';
import { NewJupyterNotebookModal } from 'app/pages/analysis/new-jupyter-notebook-modal';
import colors from 'app/styles/colors';
import { reactStyles, switchCase } from 'app/utils';
import { AnalyticsTracker } from 'app/utils/analytics';
import { setSidebarActiveIconStore } from 'app/utils/navigation';
import { WorkspaceData } from 'app/utils/workspace-data';
import { WorkspacePermissionsUtil } from 'app/utils/workspace-permissions';

import { AppSelectorModal } from './app-selector-modal';

const styles = reactStyles({
  startButton: {
    paddingLeft: '0.75rem',
    height: '3rem',
    backgroundColor: colors.secondary,
  },
});

const enum VisibleModal {
  None = 'None',
  SelectAnApp = 'SelectAnApp',
  Jupyter = 'Jupyter',
  // TODO will we need this?
  // RStudio = 'RStudio'
}

interface AppSelectorProps {
  workspace: WorkspaceData;
}

export const AppSelector = (props: AppSelectorProps) => {
  const { workspace } = props;
  const [selectedApp, setSelectedApp] = useState<UIAppType>(undefined);
  const [visibleModal, setVisibleModal] = useState(VisibleModal.None);

  const canCreateApps =
    workspace.billingStatus === BillingStatus.ACTIVE &&
    WorkspacePermissionsUtil.canWrite(workspace.accessLevel);

  const onClose = () => {
    setSelectedApp(undefined);
    setVisibleModal(VisibleModal.None);
  };

  const onNext = () => {
    switch (selectedApp) {
      case UIAppType.JUPYTER:
        AnalyticsTracker.Notebooks.OpenCreateModal();
        setVisibleModal(VisibleModal.Jupyter);
        break;
      case UIAppType.RSTUDIO:
        setVisibleModal(VisibleModal.None);
        // TODO iterate on what is the best action here
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
          setVisibleModal(VisibleModal.SelectAnApp);
        }}
        disabled={!canCreateApps}
      >
        <div style={{ width: '9rem', paddingLeft: '1rem' }}>Choose an App</div>
      </Button>
      {switchCase(
        visibleModal,
        [
          VisibleModal.SelectAnApp,
          () => (
            <AppSelectorModal
              {...{ selectedApp, setSelectedApp, onNext, onClose }}
            />
          ),
        ],
        [
          VisibleModal.Jupyter,
          () => (
            <NewJupyterNotebookModal
              {...{ workspace, onClose }}
              data-test-id='jupyter-modal'
              existingNameList={null}
              onBack={() => {
                setVisibleModal(VisibleModal.SelectAnApp);
              }}
            />
          ),
        ]
      )}
    </>
  );
};
