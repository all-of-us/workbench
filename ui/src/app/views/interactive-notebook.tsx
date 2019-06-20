import {Component} from '@angular/core';
import * as React from 'react';

import {ReactWrapperBase, withCurrentWorkspace} from 'app/utils';
import {WorkspaceData} from 'app/utils/workspace-data';
import {workspacesApi} from "../services/swagger-fetch-clients";
import {NotebookActionsPopup} from "./notebook-actions-popup";
import {IconComponent} from "../icons/icon";
import {PlaygroundModeIcon} from "../icons/playground-mode-icon";
import {EditComponentReact} from "../icons/edit";

export const InteractiveNotebook = withCurrentWorkspace()(class extends React.Component<{
  workspace: WorkspaceData
}, {html: string}> {
  constructor(props) {
    super(props);
    this.state = {
      html: ''
    };
  }

  componentDidMount(): void {
    workspacesApi().readOnlyNotebook("aou-rw-local1-93b1df18", "newbuffer", "test.ipynb")
      .then(html => {this.setState({html: html.html})})
      .catch(resp => console.error(resp));
  }

  render() {
    return (
      <div>
        <div style={{height: 35, backgroundColor: 'white', borderStyle: 'solid', borderWidth: 'thin', borderColor: '#cdcdcd'}}>
          <div style={{float: 'left', height: '100%', width: 227, color: '#262262', backgroundColor: 'rgba(38,34,98,0.2)', textAlign: 'center', lineHeight: '35px'}}> Preview (Read-Only) </div>
          <div style={{float: 'left', height: '100%', width: 135, color: '#262262', borderLeft: '1px solid rgb(205, 205, 205)', borderRight: '1px solid rgb(205, 205, 205)', backgroundColor: 'rgba(38,34,98,0.05)', textAlign: 'center', lineHeight: '35px'}}>
            <EditComponentReact enableHoverEffect={false} disabled={false} style={{height: '14px', width: '14px', verticalAlign: 'middle', marginLeft: 0, marginRight: '5px', marginBottom: '3px'}} />
            Edit (In Use)
          </div>
          <div style={{float: 'left', height: '100%', width: 210, color: '#262262', backgroundColor: 'rgba(38,34,98,0.05)', textAlign: 'center', lineHeight: '35px'}}>
            <div style={{fill: '#216FB4', width: 20, height: '100%', lineHeight: '52px', marginLeft: '12px', float: 'left'}}>
              <PlaygroundModeIcon />
            </div>
            <div style={{float: 'left'}}>
              Run (Playground Mode)
            </div>
          </div>
          <div style={{float: 'left', height: '100%', width: 40, color: '#262262', borderLeft: '1px solid rgb(205, 205, 205)', backgroundColor: 'rgba(38,34,98,0.05)', textAlign: 'center', lineHeight: '35px'}}>
            <NotebookActionsPopup />
          </div>
        </div>
        <iframe style={{width: '100%', height: 800, border: 0}} srcDoc={this.state.html}>
        </iframe>
      </div>
    );
  }
});

@Component({
  template: '<div #root></div>'
})
export class InteractiveNotebookComponent extends ReactWrapperBase {
  constructor() {
    super(InteractiveNotebook, []);
  }
}
