import * as React from "react";
import {Component} from "@angular/core";
import {ReactWrapperBase} from "app/utils";
import {TextInput} from "app/components/inputs";
import {FlexColumn, FlexRow} from "app/components/flex";
import {Button} from "app/components/buttons";
import {notebooksClusterApi} from "app/services/notebooks-swagger-fetch-clients";
import {DataTable} from "primereact/datatable";
import {workspaceAdminApi} from "app/services/swagger-fetch-clients";

const styles = {
  marginRightSmall: {
    marginRight: '1rem'
  }
};

interface Props{}
interface State{
  googleProject: string
  clusters: Array<object>
}

export class AdminCluster extends React.Component<Props, State> {
  constructor(props) {
    super(props);

    this.state = {
      googleProject: "",
      clusters: []
    }
  }

  updateProject(newProject: string) {
    this.setState({googleProject: newProject})
  }

  federatedGetWorkspaceInformation() {
    // notebooksClusterApi().listClustersByProject(this.state.googleProject).then(
    //   response => {
    //     this.setState({clusters: response})
    //   }
    // ).catch(error => {
    //   console.error(error)
    // });
    workspaceAdminApi().getFederatedWorkspaceDetails(this.state.googleProject).then(
      response => {
        console.log(response)
      }
    ).catch(error => {
      console.error(error)
    });
  }

  maybeFindClusters(event) {
    if (event.key === "Enter")
    return this.federatedGetWorkspaceInformation();
  }

  render() {
    return <div>
      <h2>Manage Workspaces</h2>
      <FlexRow style={{justifyContent: 'flex-start', alignItems: 'center'}}>
        <label style={styles.marginRightSmall}>GCP Project ID</label>
        <TextInput style={{width: '400px', ...styles.marginRightSmall}} onChange={value => this.updateProject(value)} onKeyDown={event => this.maybeFindClusters(event)}/>
        <Button style={{height: '1.5rem'}} onClick={() => this.federatedGetWorkspaceInformation()}>Find Clusters</Button>
      </FlexRow>
      {/*{*/}
      {/*  this.state.clusters.length > 0 && <FlexColumn>*/}
      {/*    <h3>Clusters</h3>*/}
      {/*    <DataTable>*/}

      {/*    </DataTable>*/}
      {/*  </FlexColumn>*/}
      {/*}*/}
    </div>;
  }
}

@Component({
  template: '<div #root></div>'
})
export class AdminClusterComponent extends ReactWrapperBase {
  constructor() {
    super(AdminCluster, []);
  }
}
