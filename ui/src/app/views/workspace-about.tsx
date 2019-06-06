import {Component} from '@angular/core';
import {FadeBox} from 'app/components/containers';
import colors from 'app/styles/colors';
import {reactStyles, ReactWrapperBase, withCurrentWorkspace} from 'app/utils';
import {WorkspaceData} from 'app/utils/workspace-data';
import * as React from 'react';
import {ResearchPurposeDescription, ResearchPurposeItems, specificPopulations, UbrTableCell} from './workspace-edit';
import {EditComponentReact} from "../icons/edit";
import {Clickable} from "../components/buttons";
import {WorkspacePermissions} from "../utils/workspace-permissions";
import {navigate} from 'app/utils/navigation';

const styles = reactStyles({
  mainHeader: {
    fontSize: '20px', fontWeight: 600, color: colors.purple[0], marginBottom: '0.5rem',
    display: 'flex', flexDirection: 'row'
  },
  sectionHeader: {
    fontSize: '16px', fontWeight: 600, color: colors.purple[0], marginTop: '1rem'
  },
  sectionText: {
    fontSize: '14px', lineHeight: '24px', color: colors.purple[0], marginTop: '0.3rem'
  }
});


export const WorkspaceAbout = withCurrentWorkspace()(
  class extends React.Component<{workspace: WorkspaceData},
    {workspacePermissions: WorkspacePermissions}> {
    constructor(props) {
      super(props);
      this.state = {
        workspacePermissions: new WorkspacePermissions(props.workspace)
      };
    }

    render() {
      const workspace = this.props.workspace;
      const {workspacePermissions} = this.state;
      return <FadeBox style={{margin: 'auto', marginTop: '1rem', width: '98%'}}>
        <div style={styles.mainHeader}>Research Purpose
          <Clickable disabled={!workspacePermissions.canWrite}
                     data-test-id='edit-workspace'
                     onClick={() => navigate(
                       ['workspaces',  workspace.namespace, workspace.id, 'edit'])}>
            <EditComponentReact disabled={!workspacePermissions.canWrite}
                                style={{marginTop: '0.1rem'}}/>
          </Clickable>
        </div>
        <div style={styles.sectionText}>{ResearchPurposeDescription}</div>
        <div style={styles.sectionHeader}>Primary purpose of the project</div>
        {ResearchPurposeItems.map((item, key) => {
          if (workspace.researchPurpose[item.shortName]) {
            let content = item.shortDescription;
            if (item.shortName === 'otherPurpose') {
              content += ': ' + workspace.researchPurpose.otherPurposeDetails;
            }
            if (item.shortName === 'diseaseFocusedResearch') {
              content += ': ' + workspace.researchPurpose.diseaseOfFocus;
            }
            return <UbrTableCell left={true} key={key} content={content}/>;
          }
        })}
        <div style={styles.sectionHeader}>
          Reason for choosing All of Us data for your investigation</div>
        <div style={styles.sectionText}>{workspace.researchPurpose.reasonForAllOfUs}</div>
        <div style={styles.sectionHeader}>Area of intended study</div>
        <div style={styles.sectionText}>{workspace.researchPurpose.intendedStudy}</div>
        <div style={styles.sectionHeader}>Anticipated findings from this study</div>
        <div style={styles.sectionText}>{workspace.researchPurpose.anticipatedFindings}</div>
        {workspace.researchPurpose.population && <div style={{marginBottom: '1rem'}}>
          <div style={styles.sectionHeader}>Population area(s) of focus</div>
          {specificPopulations.map((population, key) => {
            if (workspace.researchPurpose.populationDetails.includes(population.object)) {
              return <UbrTableCell left={true} key={key}
                                   content={population.ubrLabel}/>;
            }
          })}</div>
        }
      </FadeBox>;
    }
  }
);

@Component({
  selector: 'app-workspace-about',
  template: '<div #root></div>'
})
export class WorkspaceAboutComponent extends ReactWrapperBase {

  constructor() {
    super(WorkspaceAbout, []);
  }
}
