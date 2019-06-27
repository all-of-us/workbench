import {Component} from '@angular/core';
import * as React from 'react';

import {Button, Clickable} from 'app/components/buttons';
import {FadeBox} from 'app/components/containers';
import {TwoColPaddedTable} from 'app/components/tables';
import {EditComponentReact} from 'app/icons/edit';
import colors from 'app/styles/colors';
import {reactStyles, ReactWrapperBase, withCurrentWorkspace} from 'app/utils';
import {sliceByHalfLength} from 'app/utils/index';
import {navigate} from 'app/utils/navigation';
import {WorkspaceData} from 'app/utils/workspace-data';
import {WorkspacePermissions} from 'app/utils/workspace-permissions';
import {SpecificPopulationEnum} from 'generated/fetch';
import {ResearchPurposeDescription, ResearchPurposeItems, specificPopulations} from './workspace-edit';
import {workspacesApi} from "../services/swagger-fetch-clients";

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

    getSelectedResearchPurposeItems() {
      return ResearchPurposeItems.filter((item) =>
        this.props.workspace.researchPurpose[item.shortName]).map((item) => {
          let content = item.shortDescription;
          if (item.shortName === 'otherPurpose') {
            content += ': ' + this.props.workspace.researchPurpose.otherPurposeDetails;
          }
          if (item.shortName === 'diseaseFocusedResearch') {
            content += ': ' + this.props.workspace.researchPurpose.diseaseOfFocus;
          }
          return content;
        });
    }

    getSelectedPopulations() {
      const populations = specificPopulations.filter(sp =>
        this.props.workspace.researchPurpose.populationDetails.includes(sp.object))
        .map(sp => sp.ubrLabel);
      if (this.props.workspace.researchPurpose.populationDetails
        .includes(SpecificPopulationEnum.OTHER)) {
        populations.push('Other: ' + this.props.workspace.researchPurpose.otherPopulationDetails);
      }
      return populations;
    }

    getSelectedPopulationsSlice(left: boolean) {
      const populations = this.getSelectedPopulations();
      const populationsHalfLen = sliceByHalfLength(populations);
      if (left) {
        return populations.slice(0, populationsHalfLen);
      } else {
        return populations.slice(populationsHalfLen);
      }
    }

    async publishWorkspace() {
      try {
        await workspacesApi()
          .publishWorkspace(this.props.workspace.namespace, this.props.workspace.id);
      } catch (error) {
        console.log(error);
      }
    }

    render() {
      const workspace = this.props.workspace;
      const {workspacePermissions} = this.state;
      const selectedResearchPurposeItems = this.getSelectedResearchPurposeItems();
      const rpItemsHalfLen = sliceByHalfLength(selectedResearchPurposeItems);
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
        <TwoColPaddedTable header={false} style={{marginTop: '0.3rem'}}
                           contentLeft={selectedResearchPurposeItems.slice(0, rpItemsHalfLen)}
                           contentRight={selectedResearchPurposeItems.slice(rpItemsHalfLen)}/>
        <div style={styles.sectionHeader}>
          Reason for choosing All of Us data for your investigation</div>
        <div style={styles.sectionText}>{workspace.researchPurpose.reasonForAllOfUs}</div>
        <div style={styles.sectionHeader}>Area of intended study</div>
        <div style={styles.sectionText}>{workspace.researchPurpose.intendedStudy}</div>
        <div style={styles.sectionHeader}>Anticipated findings from this study</div>
        <div style={styles.sectionText}>{workspace.researchPurpose.anticipatedFindings}</div>
        {workspace.researchPurpose.population && <div style={{marginBottom: '1rem'}}>
          <div style={styles.sectionHeader}>Population area(s) of focus</div>
          <TwoColPaddedTable header={false} style={{marginTop: '0.3rem'}}
                             contentLeft={this.getSelectedPopulationsSlice(true)}
                             contentRight={this.getSelectedPopulationsSlice(false)}/>
        </div>
        }
        <Button onClick={() => this.publishWorkspace()}>Publish</Button>
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
