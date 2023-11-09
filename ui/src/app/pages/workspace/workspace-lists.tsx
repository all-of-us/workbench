import * as React from 'react';
import { useEffect, useState } from 'react';
import { Column } from 'primereact/column';
import { DataTable } from 'primereact/datatable';

import { AppStatus, FileDetail, Profile } from 'generated/fetch';

import { ExpandedApp } from 'app/components/apps-panel/expanded-app';
import {
  findApp,
  getAppsByDisplayGroup,
  UIAppType,
} from 'app/components/apps-panel/utils';
import { CloseButton, SnowmanButton } from 'app/components/buttons';
import { FlexColumn, FlexRow } from 'app/components/flex';
import { PopupTrigger } from 'app/components/popups';
import { formatMB } from 'app/pages/admin/workspace/file-table';
import { WorkspaceActionsMenu } from 'app/pages/workspace/workspace-actions-menu';
import { WorkspaceMenu } from 'app/pages/workspace/workspace-menu';
import { WorkspaceName } from 'app/pages/workspace/workspace-name';
import { appsApi } from 'app/services/swagger-fetch-clients';
import colors, { colorWithWhiteness } from 'app/styles/colors';
import { reactStyles } from 'app/utils';
import { hasTierAccess } from 'app/utils/access-tiers';
import { AnalyticsTracker, triggerEvent } from 'app/utils/analytics';
import { displayDate } from 'app/utils/dates';
import { useNavigation } from 'app/utils/navigation';
import {
  runtimeStore,
  serverConfigStore,
  userAppsStore,
  useStore,
} from 'app/utils/stores';
import { WorkspacePermissions } from 'app/utils/workspace-permissions';
const styles = reactStyles({
  workspaceCard: {
    justifyContent: 'flex-end',
    height: '100%',
    // Set relative positioning so the spinner overlay is centered in the card.
    position: 'relative',
  },
  workspaceMenuWrapper: {
    paddingTop: '.75rem',
    borderRight: '1px solid',
    borderColor: colorWithWhiteness(colors.dark, 0.6),
    flex: '0 0 1.5rem',
    justifyContent: 'flex-start',
    alignItems: 'center',
  },
  workspaceName: {
    color: colors.accent,
    marginBottom: '0.75rem',
    fontSize: 18,
    wordBreak: 'break-all',
  },
  workspaceNameDisabled: {
    color: colors.disabled,
    marginBottom: '0.75rem',
    fontSize: 18,
    wordBreak: 'break-all',
    pointerEvents: 'none',
    cursor: 'not-allowed',
  },
  permissionBox: {
    color: colors.white,
    height: '1.5rem',
    width: '4.5rem',
    fontSize: 10,
    textAlign: 'center',
    borderRadius: '0.3rem',
    padding: 0,
  },
  lockWorkspace: {
    color: colors.warning,
    marginBottom: '0.15rem',
    width: '21px',
    height: '21px',
    viewBox: '0 0 25 27',
  },
});
interface workspaceProps {
  list: WorkspacePermissions[];
  profile: Profile;
}

interface TableEntry {
  menu: JSX.Element;
  rawName: string;
  name: JSX.Element;
  accessLevels: JSX.Element;
  lastChanged: JSX.Element;
}

export const WorkspaceLists = (props: workspaceProps) => {
  const [tableData, setTableDate] = useState<Array<TableEntry>>();
  const [navigate] = useNavigation();

  const tierAccessDisabled = (workspace) => {
    return !hasTierAccess(props.profile, workspace.accessTierShortName);
  };
  const menu = (workspace, accessLevel) => {
    return (
      <WorkspaceMenu
        tierAccessDisabled={tierAccessDisabled(workspace)}
        workspace={workspace}
        accessLevel={accessLevel}
      />
    );
  };
  const { runtime } = useStore(runtimeStore);
  const { config } = useStore(serverConfigStore);
  // all GKE apps (not Jupyter)
  const { userApps } = useStore(userAppsStore);

  // in display order
  const appsToDisplay = [
    UIAppType.JUPYTER,
    ...(config.enableRStudioGKEApp ? [UIAppType.RSTUDIO] : []),
    // ...(config.enableCromwellGKEApp ? [UIAppType.CROMWELL] : []),
  ];

  const [activeApps, availableApps] = getAppsByDisplayGroup(
    runtime,
    userApps,
    appsToDisplay
  );
  const [appsList, setAppsList] = useState<Array<string>>([]);
  const fetchAppsList = (namespace) => {
    const appsRunning = [];
    appsApi()
      .listAppsInWorkspace(namespace)
      .then((listResponse) => {
        listResponse.map((lists) => {
          if (lists.status === AppStatus.RUNNING) {
            appsRunning.push(lists.appName);
          }
        });
        setAppsList(appsRunning);
      });
    return <div>Apps</div>;
  };
  const initTable = (
    fileDetails: WorkspacePermissions[]
  ): Array<TableEntry> => {
    return fileDetails.map((file) => {
      return {
        menu: menu(file.workspace, file.accessLevel),
        rawName: file.workspace.name,
        name: (
          <WorkspaceName
            workspace={file.workspace}
            tierAccessDisabled={tierAccessDisabled(file.workspace)}
          />
        ),
        accessLevels: (
          <div
            style={{
              ...styles.permissionBox,
              backgroundColor:
                colors.workspacePermissionsHighlights[file.accessLevel],
            }}
            data-test-id='workspace-access-level'
          >
            {file.accessLevel}
          </div>
        ),
        lastChanged: (
          <div style={{ fontSize: 12 }}>
            {displayDate(file.workspace.lastModifiedTime)}
          </div>
        ),
      };
    });
  };

  useEffect(() => {
    setTableDate(initTable(props.list));
  }, [props.list]);

  return (
    <div>
      <DataTable value={tableData} filterDisplay='row' paginator rows={10}>
        <Column field={'menu'} />
        <Column
          header='Name'
          field={'name'}
          sortField={'rawName'}
          sortable
          filter
          filterField={'rawName'}
        />
        <Column header='Access Level' field={'accessLevels'} />
        <Column header='Last Changed' field={'lastChanged'} />
      </DataTable>
    </div>
  );
};
