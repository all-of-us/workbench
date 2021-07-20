import {TabButton} from 'app/components/buttons';
import {FadeBox} from 'app/components/containers';
import {renderResourceCard} from 'app/components/render-resource-card';
import {SpinnerOverlay} from 'app/components/spinners';
import {userApi, workspacesApi} from 'app/services/swagger-fetch-clients';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import * as React from 'react';
import {WithSpinnerOverlayProps} from 'app/components/with-spinner-overlay';
import {TabPanel, TabView} from 'primereact/tabview';
import {
  BillingAccount,
  Workspace,
  WorkspaceAccessLevel,
  WorkspaceResource,
  WorkspaceResponse
} from 'generated/fetch';
import {WorkspacePermissions} from "../../utils/workspace-permissions";
import {convertAPIError} from 'app/utils/errors';
import {Column} from "primereact/column";
import {DataTable} from 'primereact/datatable';
import {ensureBillingScope} from "../../utils/workbench-gapi-client";

const styles = {
  column: {
    textAlign: 'left',
  },
  typeColumn: {
    textAlign: 'left',
    width: '130px',
  },
  cardButtonArea: {
    display: 'flex',
    alignItems: 'center',
    width: '100%'
  },
  cardHeader: {
    display: 'flex',
    alignItems: 'baseline'
  },
  resourceTypeButton: {
    width: '33%',
    justifyContent: 'flex-start',
    maxWidth: 'none',
    margin: '1.9rem 1rem 0 0',
    minHeight: '325px',
    maxHeight: '325px'
  },
  resourceTypeButtonLast: {
    marginRight: '0rem'
  },
  cardHeaderText: (disabled) => {
    return {
      color: disabled ? colorWithWhiteness(colors.dark, 0.4) : colors.accent,
      fontSize: '20px',
      marginRight: '0.5rem',
      marginTop: '0.5rem'
    };
  },
  cardText: {
    color: colors.primary,
    fontSize: '14px',
    lineHeight: '22px'
  },
  tabContainer: {
    display: 'flex',
    justifyContent: 'flex-start',
    alignItems: 'center',
    width: '100%',
    marginBottom: '0.5rem'
  }
};

interface TableData {
  workspaceName: string;
  createdBy: string;
  billingAccountName: string;
}

interface State {
  activeTab: number;
  dataLoading: boolean;
  errorText: string;
  ownedWorkspaceTableData: TableData[];
  sharedWorkspaceTableData: TableData[];
  billingAccounts: Array<BillingAccount>;
  tableData: TableData[];
}

export class BillingDashboardComponent extends React.Component<WithSpinnerOverlayProps, State>{
  constructor(props) {
    super(props);
    this.state = {
      activeTab: 0,
      dataLoading: true,
      errorText: '',
      ownedWorkspaceTableData: [],
      sharedWorkspaceTableData: [],
      billingAccounts:[],
      tableData:[],
    };
  };

  componentDidMount(): void {
    this.props.hideSpinner();
    this.loadData();
  }

  componentDidUpdate(prevProps: any): void {
    this.props.hideSpinner();
    this.loadData();
  }

  async loadData() {
    this.setState({dataLoading: true});
    try {
      const workspacesReceived = (await workspacesApi().getWorkspaces()).items
      this.setState({billingAccounts: (await userApi().listBillingAccounts()).billingAccounts})
      this.setState({ownedWorkspaceTableData: workspacesReceived
        .filter(w => w.accessLevel === WorkspaceAccessLevel.WRITER).map(
            workspaceResponse =>
              this.getTableData(workspaceResponse)
            )})

      this.setState({sharedWorkspaceTableData: workspacesReceived
        .filter(w => (w.accessLevel == WorkspaceAccessLevel.WRITER || w.accessLevel == WorkspaceAccessLevel.READER)).map(
            workspaceResponse =>
                this.getTableData(workspaceResponse)
        )})
      this.setState({dataLoading: false});
    } catch (e) {
      const response = await convertAPIError(e);
      this.setState({errorText: response.message});
    }
  }

  tabChange = (e: any) => {
    const tab = e.index === 0 ? 'My Workspace' : "Shared Workspace";
    this.setState({activeTab: e.index});
  }

  getTableData(workspaceResponse: WorkspaceResponse) : TableData {
    return {
      workspaceName: workspaceResponse.workspace.name,
      createdBy: workspaceResponse.workspace.creator,
      billingAccountName: workspaceResponse.workspace.billingAccountName,
    };
  }

  render() {
    const {
      activeTab,
      dataLoading,
      billingAccounts,
      ownedWorkspaceTableData,
      sharedWorkspaceTableData,
    } = this.state;

    return <React.Fragment>
      <FadeBox style={{marginTop: '1rem'}}>
        <div style={styles.tabContainer}>
          <h2 style={{
            margin: 0,
            color: colors.primary,
            fontSize: '16px',
            fontWeight: 600
          }}>Show:</h2>
          <TabButton active={activeTab === 0} onClick={() => this.tabChange(0)}>
            My Workspaces
          </TabButton>
          <TabButton active={activeTab === 1} onClick={() => this.tabChange(1)}>
            Shared Workspaces
          </TabButton>
          <TabView style={{padding: 0}} activeIndex={activeTab} onTabChange={this.tabChange}>
            <TabPanel header='Workspaces you own'>
              <div data-test-id='owned-workspace-table'><DataTable
                  value={ownedWorkspaceTableData}
                  scrollable={true}>
                <Column field='workspaceName' header='Workspace name' style={styles.column}/>
                <Column field='workspaceCreator' header='Created by' style={styles.column}/>
                <Column field='billingAccount' header='Billing account name' style={styles.column}/>
              </DataTable></div>
            </TabPanel>
            <TabPanel header='Workspaces shared with you'>
              <div data-test-id='shared-workspace-table'><DataTable
                  value={sharedWorkspaceTableData}
                  scrollable={true}>
                <Column field='workspaceName' header='Workspace name' style={styles.column}/>
                <Column field='workspaceCreator' header='Created by' style={styles.column}/>
                <Column field='billingAccount' header='Billing account name' style={styles.column}/>
              </DataTable></div>
            </TabPanel>
          </TabView>
        </div>
      </FadeBox>
    </React.Fragment>;
  }
};
