import {Component} from '@angular/core';
import * as fp from 'lodash/fp';
import * as React from 'react';

import {CardButton} from 'app/components/buttons';
import {FadeBox} from 'app/components/containers';
import {ClrIcon, InfoIcon} from 'app/components/icons';
import {TooltipTrigger} from 'app/components/popups';
import {SpinnerOverlay} from 'app/components/spinners';
import {convertToResource, ResourceType} from 'app/utils/resourceActionsReact';

import {WorkspaceData} from 'app/resolvers/workspace';
import {profileApi, workspacesApi} from 'app/services/swagger-fetch-clients';
import {ReactWrapperBase, withCurrentWorkspace} from 'app/utils';
import {NewNotebookModal} from 'app/views/new-notebook-modal/component';
import {ResourceCard} from 'app/views/resource-card/component';

import {FileDetail, WorkspaceAccessLevel} from 'generated/fetch';

const styles = {
  heading: {
    color: '#2F2E7E',
    fontSize: 20, fontWeight: 600, lineHeight: '24px'
  },
  addCard: {
    width: 200, height: 105,
    marginTop: '1rem', marginRight: '1rem',
    fontSize: 18, fontWeight: 500, lineHeight: '22px',
  }
};

export const NotebookList = withCurrentWorkspace()(class extends React.Component<{
  workspace: WorkspaceData
}, {
  notebooks: FileDetail[],
  creating: boolean,
  loading: boolean
}> {
  constructor(props) {
    super(props);
    this.state = {notebooks: [], creating: false, loading: false};
  }

  componentDidMount() {
    profileApi().updatePageVisits({page: 'notebook'});
    this.loadNotebooks();
  }

  private async loadNotebooks() {
    try {
      const {workspace: {namespace, id}} = this.props;
      this.setState({loading: true});
      const notebooks = await workspacesApi().getNoteBookList(namespace, id);
      this.setState({notebooks});
    } catch (error) {
      console.error(error);
    } finally {
      this.setState({loading: false});
    }
  }

  render() {
    const {workspace, workspace: {namespace, id, accessLevel}} = this.props;
    const {notebooks, creating, loading} = this.state;
    // TODO Remove this cast when we switch to fetch types
    const al = accessLevel as unknown as WorkspaceAccessLevel;
    const canWrite = fp.includes(al, [WorkspaceAccessLevel.OWNER, WorkspaceAccessLevel.WRITER]);
    return <FadeBox style={{margin: 'auto', marginTop: '1rem', width: '95.7%'}}>
      <div style={styles.heading}>
        Notebooks&nbsp;
        <TooltipTrigger
          content={`A Notebook is a computational environment where you
            can analyze data with basic programming knowledge in R or Python.`}
        ><InfoIcon size={16} /></TooltipTrigger>
      </div>
      <div style={{display: 'flex', alignItems: 'flex-start'}}>
        <TooltipTrigger content={!canWrite && 'Write permission required to create notebooks'}>
          <CardButton
            disabled={!canWrite}
            style={styles.addCard}
            onClick={() => this.setState({creating: true})}
          >
            Create a<br/>New Notebook
            <ClrIcon shape='plus-circle' size={21} style={{marginTop: 5}} />
          </CardButton>
        </TooltipTrigger>
        <div style={{display: 'flex', flexWrap: 'wrap', justifyContent: 'flex-start' }}>
          {notebooks.map(notebook => {
            return <ResourceCard key={notebook.path}
              resourceCard={convertToResource(notebook, namespace, id, al, ResourceType.NOTEBOOK)}
              onUpdate={() => this.loadNotebooks()}
            />;
          })}
        </div>
      </div>
      {loading && <SpinnerOverlay />}
      {creating && <NewNotebookModal
        workspace={workspace}
        existingNotebooks={notebooks}
        onClose={() => this.setState({creating: false})}
      />}
    </FadeBox>;
  }
});

@Component({
  template: '<div #root></div>'
})
export class NotebookListComponent extends ReactWrapperBase {
  constructor() {
    super(NotebookList, []);
  }
}
