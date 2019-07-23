import * as fp from 'lodash/fp';
import * as React from 'react';

import {Button, Clickable} from 'app/components/buttons';
import {FadeBox} from 'app/components/containers';
import {TwoColPaddedTable} from 'app/components/tables';
import {EditComponentReact} from 'app/icons/edit';
import {workspacesApi} from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import {reactStyles, withCurrentWorkspace, withUserProfile} from 'app/utils';
import {sliceByHalfLength} from 'app/utils/index';
import {navigate} from 'app/utils/navigation';
import {WorkspaceData} from 'app/utils/workspace-data';
import {WorkspacePermissions} from 'app/utils/workspace-permissions';
import {Authority, Profile, SpecificPopulationEnum} from 'generated/fetch';
import {ResearchPurposeDescription, ResearchPurposeItems, specificPopulations} from './workspace-edit';

const styles = reactStyles({
  mainHeader: {
    fontSize: '20px', fontWeight: 600, color: colors.primary, marginBottom: '0.5rem',
    display: 'flex', flexDirection: 'row'
  },
  sectionHeader: {
    fontSize: '16px', fontWeight: 600, color: colors.primary, marginTop: '1rem'
  },
  sectionText: {
    fontSize: '14px', lineHeight: '24px', color: colors.primary, marginTop: '0.3rem'
  }
});


export const ResearchPurpose = fp.flow(withCurrentWorkspace(), withUserProfile())(
  class extends React.Component<
    {profileState: { profile: Profile, reload: Function }, workspace: WorkspaceData},
    {publishing: boolean, workspacePermissions: WorkspacePermissions}> {
    constructor(props) {
      super(props);
      this.state = {
        publishing: false,
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

    async publishUnpublishWorkspace(publish: boolean) {
      this.setState({publishing: true});
      try {
        if (publish) {
          await workspacesApi()
            .publishWorkspace(this.props.workspace.namespace, this.props.workspace.id);
        } else {
          await workspacesApi()
            .unpublishWorkspace(this.props.workspace.namespace, this.props.workspace.id);
        }
      } catch (error) {
        console.error(error);
      } finally {
        this.setState({publishing: false});
      }
    }

    render() {
      const {workspace, profileState: {profile}} = this.props;
      const {publishing, workspacePermissions} = this.state;
      const selectedResearchPurposeItems = this.getSelectedResearchPurposeItems();
      const rpItemsHalfLen = sliceByHalfLength(selectedResearchPurposeItems);
      return <FadeBox style={{margin: '1rem', width: '98%'}}>
        <div style={styles.mainHeader}>Research Purpose
          <Clickable disabled={!workspacePermissions.canWrite}
                     data-test-id='edit-workspace'
                     onClick={() => navigate(
                       ['workspaces',  workspace.namespace, workspace.id, 'edit'])}>
            <EditComponentReact enableHoverEffect={true}
                                disabled={!workspacePermissions.canWrite}
                                style={{marginTop: '0.1rem'}}/>
          </Clickable>
        </div>
        <div style={styles.sectionText}>{ResearchPurposeDescription}</div>
        <div style={styles.sectionHeader}>Primary purpose of the project</div>
        <TwoColPaddedTable header={false} style={{marginTop: '0.3rem'}}
                           contentLeft={selectedResearchPurposeItems.slice(0, rpItemsHalfLen)}
                           contentRight={selectedResearchPurposeItems.slice(rpItemsHalfLen)}/>
        <div style={styles.sectionHeader}>
          Reason for choosing <i>All of Us</i> data for your investigation</div>
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
        {profile.authorities.includes(Authority.FEATUREDWORKSPACEADMIN) &&
          <div style={{display: 'flex', justifyContent: 'flex-end'}}>
              <Button disabled={publishing} type='secondary'
                onClick={() => this.publishUnpublishWorkspace(false)}>Unpublish</Button>
              <Button onClick={() => this.publishUnpublishWorkspace(true)}
                disabled={publishing} style={{marginLeft: '0.5rem'}}>Publish</Button>
          </div>}
      </FadeBox>;
    }
  }
);

