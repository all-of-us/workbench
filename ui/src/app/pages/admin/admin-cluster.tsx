import * as React from "react";
import {Component} from "@angular/core";
import {ReactWrapperBase} from "app/utils";
import {TextInput} from "app/components/inputs";
import {FlexRow} from "app/components/flex";
import {Button} from "app/components/buttons";
import {notebooksClusterApi} from "app/services/notebooks-swagger-fetch-clients";

const styles = {
  marginRightSmall: {
    marginRight: '1rem'
  }
};

interface Props{}
interface State{
  googleProject: string
}

export class AdminCluster extends React.Component<Props, State> {
  constructor(props) {
    super(props);

    this.state = {
      googleProject: ""
    }
  }

  updateProject(newProject: string) {
    this.setState({googleProject: newProject})
  }

  findClusters() {
    return notebooksClusterApi().listClustersByProject(this.state.googleProject);
  }

  render() {
    return <div>
      <h2>Manage Clusters</h2>
      <FlexRow style={{justifyContent: 'flex-start', alignItems: 'center'}}>
        <label style={styles.marginRightSmall}>GCP Project ID</label>
        <TextInput style={{width: '400px', ...styles.marginRightSmall}} onChange={value => this.updateProject(value)}/>
        <Button style={{height: '1.5rem'}} onClick={() => this.findClusters()}>Find Clusters</Button>
      </FlexRow>
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
