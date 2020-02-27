import * as React from 'react';

import {Clickable} from 'app/components/buttons';
import {FadeBox} from 'app/components/containers';
import {EditComponentReact} from 'app/icons/edit';
import {
  disseminateFindings,
  researchOutcomes,
  researchPurposeQuestions,
  SpecificPopulationItems
} from 'app/pages/workspace/workspace-edit-text';
import colors from 'app/styles/colors';
import {reactStyles, withCurrentWorkspace} from 'app/utils';
import {navigate} from 'app/utils/navigation';
import {
  getSelectedPopulations,
  getSelectedResearchPurposeItems
} from 'app/utils/research-purpose';
import {WorkspaceData} from 'app/utils/workspace-data';
import {WorkspacePermissions} from 'app/utils/workspace-permissions';

const styles = reactStyles({
  editIcon: {
    marginTop: '0.1rem',
    height: 22,
    width: 22,
    fill: colors.light,
    backgroundColor: colors.accent,
    cursor: 'pointer',
    padding: '5px',
    borderRadius: '23px'
  },
  mainHeader: {
    fontSize: '16px', fontWeight: 600, color: colors.primary, marginBottom: '0.5rem',
    display: 'flex', flexDirection: 'row', alignItems: 'center'
  },
  sectionContentContainer: {
    marginLeft: '1rem'
  },
  sectionHeader: {
    fontSize: '16px', fontWeight: 600, color: colors.primary, marginTop: '1rem'
  },
  sectionItemWithBackground: {
    padding: '10px',
    backgroundColor: colors.white,
    color: colors.primary,
    marginLeft: '0.5rem',
    borderRadius: '3px'
  },
  sectionSubHeader: {
    fontSize: '14px', fontWeight: 600, color: colors.primary, marginTop: '0.5rem'
  },
  sectionText: {
    fontSize: '14px', lineHeight: '24px', color: colors.primary, marginTop: '0.3rem'
  },
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

    render() {
      const {workspace} = this.props;
      const {workspacePermissions} = this.state;
      const selectedResearchPurposeItems = getSelectedResearchPurposeItems(this.props.workspace.researchPurpose);
      return <FadeBox>
        <div style={styles.mainHeader}>Primary purpose of project
          <Clickable disabled={!workspacePermissions.canWrite}
                     style={{display: 'flex', alignItems: 'center'}}
                     data-test-id='edit-workspace'
                     onClick={() => navigate(
                       ['workspaces',  workspace.namespace, workspace.id, 'edit'])}>
            <EditComponentReact enableHoverEffect={true}
                                disabled={!workspacePermissions.canWrite}
                                style={styles.editIcon}/>
          </Clickable>
        </div>
        <div style={styles.sectionContentContainer}>
          {selectedResearchPurposeItems.map((selectedResearchPurposeItem, i) => <div key={i}>
            <div style={{marginTop: '1rem'}}>{selectedResearchPurposeItem}</div>
          </div>)}
        </div>
        <div style={styles.sectionHeader}>Summary of research purpose</div>
        <div style={styles.sectionContentContainer}>
          {/*Intended study section*/}
          <div style={styles.sectionSubHeader}>{researchPurposeQuestions[2].header}</div>
          <div style={{...styles.sectionItemWithBackground, padding: '15px'}}>
            {workspace.researchPurpose.intendedStudy}</div>

          {/*Scientific approach section*/}
          <div style={styles.sectionSubHeader}>{researchPurposeQuestions[3].header}</div>
          <div style={{...styles.sectionItemWithBackground, padding: '15px'}}>
            {workspace.researchPurpose.scientificApproach}</div>

          {/*Anticipated findings section*/}
          <div style={styles.sectionSubHeader}>{researchPurposeQuestions[4].header}</div>
          <div style={{...styles.sectionItemWithBackground, padding: '15px'}}>
            {workspace.researchPurpose.anticipatedFindings}
          </div>
        </div>

        {/*Findings section*/}
        <div style={styles.sectionHeader}>Findings will be disseminate by the following:</div>
        <div style={styles.sectionContentContainer}>
          {workspace.researchPurpose.disseminateResearchFindingList.map((disseminateFinding, i) =>
            <div key={i} style={{...styles.sectionItemWithBackground, marginTop: '0.5rem'}}>{disseminateFindings
              .find(finding => finding.shortName === disseminateFinding).label}</div>
          )}
        </div>

        {/*Outcomes section*/}
        <div style={styles.sectionHeader}>Outcomes anticipated from the research:</div>
        <div style={styles.sectionContentContainer}>
          {workspace.researchPurpose.researchOutcomeList.map((workspaceOutcome, i) =>
            <div key={i} style={{...styles.sectionItemWithBackground, marginTop: '0.5rem'}}>{researchOutcomes
              .find(outcome => outcome.shortName === workspaceOutcome).label}</div>
          )}
        </div>

        {/*Underserved populations section*/}
        {workspace.researchPurpose.population && <React.Fragment>
          <div style={styles.sectionHeader}>Population of interest</div>
          <div style={styles.sectionContentContainer}>
            <div style={{marginTop: '0.5rem'}}>{getSelectedPopulations(workspace.researchPurpose)}</div>
          </div>
        </React.Fragment>}
      </FadeBox>;
    }
  }
);
