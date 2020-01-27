import * as React from 'react';

import {Clickable} from 'app/components/buttons';
import {FadeBox} from 'app/components/containers';
import {TwoColPaddedTable} from 'app/components/tables';
import {EditComponentReact} from 'app/icons/edit';
import {
  ResearchPurposeDescription,
  ResearchPurposeItems,
  SpecificPopulationItems} from 'app/pages/workspace/workspace-edit';
import colors from 'app/styles/colors';
import {reactStyles, withCurrentWorkspace} from 'app/utils';
import {sliceByHalfLength} from 'app/utils/index';
import {navigate} from 'app/utils/navigation';
import {WorkspaceData} from 'app/utils/workspace-data';
import {WorkspacePermissions} from 'app/utils/workspace-permissions';
import {SpecificPopulationEnum} from 'generated/fetch';

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


export const ResearchPurpose = withCurrentWorkspace()(
  class extends React.Component<
    {workspace: WorkspaceData},
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
      const populations = SpecificPopulationItems.filter(sp =>
        this.props.workspace.researchPurpose.populationDetails.includes(sp.shortName))
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

    render() {
      const {workspace} = this.props;
      const {workspacePermissions} = this.state;
      const selectedResearchPurposeItems = this.getSelectedResearchPurposeItems();
      const rpItemsHalfLen = sliceByHalfLength(selectedResearchPurposeItems);
      return <FadeBox>
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
      </FadeBox>;
    }
  }
);
