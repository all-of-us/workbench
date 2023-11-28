import * as React from 'react';
import { useState } from 'react';

import { BillingStatus } from 'generated/fetch';

import { switchCase } from '@terra-ui-packages/core-utils';
import { UIAppType } from 'app/components/apps-panel/utils';
import { Button } from 'app/components/buttons';
import { NewJupyterNotebookModal } from 'app/pages/analysis/new-jupyter-notebook-modal';
import colors from 'app/styles/colors';
import { reactStyles } from 'app/utils';
import { AnalyticsTracker } from 'app/utils/analytics';
import {
  currentWorkspaceStore,
  NavigationProps,
  useNavigation,
} from 'app/utils/navigation';
import { userAppsStore, useStore } from 'app/utils/stores';
import {
  openRStudioOrConfigPanel,
  openSASOrConfigPanel,
} from 'app/utils/user-apps-utils';
import { withNavigation } from 'app/utils/with-navigation-hoc';
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
}

interface AppSelectorProps {
  workspace: WorkspaceData;
}

export const AppSelector = ({}) => {
  const workspace = currentWorkspaceStore.getValue();
  const [navigate] = useNavigation();
  const { userApps } = useStore(userAppsStore);
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
        openRStudioOrConfigPanel(
          workspace.namespace,
          workspace.name,
          userApps,
          'default',
          navigate
        );
        break;
      case UIAppType.SAS:
        setVisibleModal(VisibleModal.None);
        openSASOrConfigPanel(
          workspace.namespace,
          workspace.name,
          userApps,
          'default',
          navigate
        );
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
