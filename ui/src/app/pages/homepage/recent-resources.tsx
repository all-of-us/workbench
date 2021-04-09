import * as fp from 'lodash/fp';
import * as React from 'react';

import {Column} from 'primereact/column';
import {DataTable} from 'primereact/datatable';
import {CSSProperties, useEffect, useState} from 'react';

import {Clickable} from 'app/components/buttons';
import {SmallHeader} from 'app/components/headers';
import {renderResourceCard} from 'app/components/render-resource-card';
import {ResourceNavigation, StyledResourceType} from 'app/components/resource-card';
import {SpinnerOverlay} from 'app/components/spinners';
import {userMetricsApi} from 'app/services/swagger-fetch-clients';
import {formatWorkspaceResourceDisplayDate, reactStyles, withCdrVersions} from 'app/utils';
import {getCdrVersion} from 'app/utils/cdr-versions';
import {navigateAndPreventDefaultIfNoKeysPressed} from 'app/utils/navigation';
import {getDisplayName, isNotebook} from 'app/utils/resources';
import {
  CdrVersionListResponse,
  Workspace,
  WorkspaceResource,
  WorkspaceResourceResponse,
  WorkspaceResponse
} from 'generated/fetch';
import {datatableStyles} from '../../styles/datatable';

const styles = reactStyles({
  column: {
    textAlign: 'left',
  },
  typeColumn: {
    textAlign: 'left',
    width: '130px',
  },
  menu: {
    width: '30px',
  },
  navigation: {
    fontFamily: 'Montserrat',
    fontSize: '14px',
    letterSpacing: 0,
    lineHeight: '22px',
  }
});

interface NavProps {
  workspace: Workspace;
  resource: WorkspaceResource;
  style?: CSSProperties;
}

const WorkspaceNavigation = (props: NavProps) => {
  const {workspace: {name, namespace, id}, resource, style} = props;
  const tab = isNotebook(resource) ? 'notebooks' : 'data';
  const url = `/workspaces/${namespace}/${id}/${tab}`;

  return <Clickable>
    <a data-test-id='workspace-navigation'
       style={style}
       href={url}
       onClick={e => navigateAndPreventDefaultIfNoKeysPressed(e, url)}>
      {name}
    </a>
  </Clickable>;
};

interface TableData {
  menu: JSX.Element;
  resourceType: JSX.Element;
  resourceName: JSX.Element;
  workspaceName: JSX.Element;
  formattedLastModified: string;
  cdrVersionName: string;
}

interface Props {
  cdrVersionListResponse: CdrVersionListResponse;
  workspaces: WorkspaceResponse[];
}

export const RecentResources = fp.flow(withCdrVersions())((props: Props) => {
  const [loading, setLoading] = useState(true);
  const [resources, setResources] = useState<WorkspaceResourceResponse>();
  const [wsMap, setWorkspaceMap] = useState<Map<string, Workspace>>();
  const [tableData, setTableData] = useState<TableData[]>();

  const loadResources = () => {
    setLoading(true);
    return userMetricsApi().getUserRecentResources()
      .then(setResources)
      .then(() => setLoading(false));
  };

  useEffect(() => {
    const {workspaces} = props;
    if (workspaces) {
      const workspaceTuples = workspaces.map(r => [r.workspace.id, r.workspace] as [string, Workspace]);
      setWorkspaceMap(new Map(workspaceTuples));
      loadResources();
    }
  }, [props.workspaces]);

  const renderResourceMenu = (resource: WorkspaceResource) => {
    return renderResourceCard({
      resource,
      menuOnly: true,
      existingNameList: [],   // TODO existing bug RW-5847: does not populate names for rename modal
      onUpdate: loadResources});
  };

  useEffect(() => {
    const getWorkspace = (r: WorkspaceResource) => {
      return wsMap.get(r.workspaceFirecloudName);
    };

    const getCdrVersionName = (r: WorkspaceResource) => {
      const {cdrVersionListResponse} = props;
      return getCdrVersion(getWorkspace(r), cdrVersionListResponse).name;
    };

    if (resources && wsMap) {
      setTableData(resources.map(r => {
        return {
          menu: renderResourceMenu(r),
          resourceType: <ResourceNavigation resource={r}><StyledResourceType resource={r}/></ResourceNavigation>,
          resourceName: <ResourceNavigation resource={r} style={styles.navigation}>{getDisplayName(r)}</ResourceNavigation>,
          resourceName: getDisplayName(r),
          workspaceName: <WorkspaceNavigation workspace={getWorkspace(r)} resource={r} style={styles.navigation}/>,
          formattedLastModified: formatWorkspaceResourceDisplayDate(r.modifiedTime),
          cdrVersionName: getCdrVersionName(r),
        };
      }));
    }
  }, [resources, wsMap]);

  return (resources && wsMap && !loading) ? <React.Fragment>
    <style>{`
      .pi-sort-amount-up-alt:before {
        color:#000;
    content: '\\f35a'; /* You should use \\ and not /*/
    font-family: "Font Awesome 5 Free"; /* This is the correct font-family*/
    font-style: normal;
    font-weight: normal;
    font-size:40px;
      }

      .pi-sort-amount-down:before {
   color:#000;
    content: 'hello';
    font-family: "Font Awesome 5 Free"; /* This is the correct font-family*/
    font-style: normal;
    font-weight: normal;
    font-size:40px;
    font-weight:900;
      }

      `}
    </style>
    <SmallHeader>Recently Accessed Items</SmallHeader>
      <div data-test-id='recent-resources-table'><DataTable
          style={{border: 0}}
          tableStyle={{border: 0}}
          value={tableData}
          scrollable={true}
          paginator={true}
          paginatorTemplate='CurrentPageReport'
          currentPageReportTemplate='Showing {totalRecords} most recent items'>
        <Column field='menu' style={styles.menu}/>
        <Column field='resourceType' header='Item type' style={styles.typeColumn}/>
        <Column field='resourceName' header='Name' style={styles.column} sortable/>
        <Column field='workspaceName' header='Workspace name' style={styles.column}/>
        <Column field='formattedLastModified' header='Last changed' style={styles.column}/>
        <Column field='cdrVersionName' header='Dataset' style={styles.column}/>
      </DataTable></div>
  </React.Fragment> : <SpinnerOverlay/>;
});
