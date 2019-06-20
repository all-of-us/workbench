import {Component} from '@angular/core';
import * as React from 'react';

import {ReactWrapperBase, withCurrentWorkspace} from 'app/utils';
import {WorkspaceData} from 'app/utils/workspace-data';
import {workspacesApi} from "../services/swagger-fetch-clients";
import {NotebookActionsPopup} from "./notebook-actions-popup";

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
          <div style={{float: 'left', height: '100%', width: 125, color: '#262262', borderLeft: '1px solid rgb(205, 205, 205)', borderRight: '1px solid rgb(205, 205, 205)', backgroundColor: 'rgba(38,34,98,0.05)', textAlign: 'center', lineHeight: '35px'}}>
            <clr-icon style={{paddingRight: '2px', paddingBottom: '3px'}} shape="pencil" className="icon"></clr-icon>
            Edit (In Use)
          </div>
          <div style={{float: 'left', height: '100%', width: 240, color: '#262262', backgroundColor: 'rgba(38,34,98,0.05)', textAlign: 'center', lineHeight: '35px'}}> Run (Playground Mode) </div>
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
