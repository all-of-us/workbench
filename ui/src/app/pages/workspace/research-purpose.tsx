import * as React from 'react';

import {Clickable} from 'app/components/buttons';
import {FadeBox} from 'app/components/containers';
import {FlexColumn, FlexRow} from 'app/components/flex';
import {ClrIcon} from 'app/components/icons';
import {EditComponentReact} from 'app/icons/edit';
import {
  disseminateFindings,
  researchOutcomes,
  researchPurposeQuestions
} from 'app/pages/workspace/workspace-edit-text';
import {workspacesApi} from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import {reactStyles, withCurrentWorkspace} from 'app/utils';
import {navigate} from 'app/utils/navigation';
import {
  getSelectedPopulations,
  getSelectedResearchPurposeItems
} from 'app/utils/research-purpose';
import {WorkspaceData} from 'app/utils/workspace-data';
import {WorkspacePermissionsUtil} from 'app/utils/workspace-permissions';

const styles = reactStyles({
  editIcon: {
    marginTop: '0.1rem',
    height: 22,
    width: 22,
    fill: colors.light,
    backgroundColor: colors.accent,
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
  reviewPurposeReminder: {
    paddingTop: '0.3rem', borderStyle: 'solid', height: '3rem', color: colors.primary,
    borderColor: colors.warning, borderRadius: '0.5rem', backgroundColor: colors.resourceCardHighlights.cohort
  }
});

function updateWorkspace(workspace) {
  workspace.researchPurpose.researchPurposeReviewed = true;
  workspacesApi().updateWorkspace(workspace.namespace, workspace.id, {workspace: workspace})
    .then(workspaceUpdate => navigate(['workspaces',  workspaceUpdate.namespace, workspaceUpdate.id, 'data']));

}

export const ResearchPurpose = withCurrentWorkspace()(
  ({workspace}: {workspace: WorkspaceData}) => {
    const isOwner = WorkspacePermissionsUtil.isOwner(workspace.accessLevel);
    const selectedResearchPurposeItems = getSelectedResearchPurposeItems(workspace.researchPurpose, true);
    const selectedPrimaryPurposeItems = getSelectedResearchPurposeItems(workspace.researchPurpose, false);
    return <FadeBox>
      <div style={styles.mainHeader}>Primary purpose of project
        <Clickable disabled={!isOwner}
                   style={{display: 'flex', alignItems: 'center', marginLeft: '.5rem'}}
                   data-test-id='edit-workspace'
                   onClick={() => navigate(
                     ['workspaces',  workspace.namespace, workspace.id, 'edit'])}>
          <EditComponentReact enableHoverEffect={true}
                              disabled={!isOwner}
                              style={styles.editIcon}/>
        </Clickable>
      </div>
      {isOwner && !workspace.researchPurpose.researchPurposeReviewed && <FlexRow style={styles.reviewPurposeReminder}>
        <ClrIcon style={{color: colors.warning, paddingTop: '0.2rem'}} className='is-solid' shape='exclamation-triangle' size='22'/>
        <FlexColumn style={{paddingRight: '0.5rem', paddingLeft: '0.5rem', color: colors.primary}}>
          <label style={{fontWeight: 600, fontSize: '14px'}}>Please update or verify the following Research Purpose.</label>
          Every 365 days you will need to update or verify the Research Purpose
        </FlexColumn>
        <div style={{paddingTop: '0.2rem', marginLeft: '0.8rem'}}>
          <a style={{marginRight: '0.5rem'}} onClick={() => updateWorkspace(workspace)}>Looks Good</a>
          |
          <a style={{marginLeft: '0.5rem'}} onClick={() => navigate(
                     ['workspaces',  workspace.namespace, workspace.id, 'edit'])}>Update</a>
        </div>
    </FlexRow>}
      <div style={styles.sectionContentContainer}>
        {selectedResearchPurposeItems && selectedResearchPurposeItems.length > 0 && <div
             style={styles.sectionSubHeader}>Research Purpose</div>
        }
        {selectedResearchPurposeItems.map((selectedResearchPurposeItem, i) => <div key={i}>
          <div data-test-id='primaryResearchPurpose'
               style={{marginTop: i > 0 ? '1rem' : '0.3rem', marginLeft: '1rem'}}>{selectedResearchPurposeItem}</div>
        </div>)}
      </div>
      <div style={styles.sectionContentContainer}>
        {selectedPrimaryPurposeItems.map((selectedPrimaryPurposeItem, i) => <div key={i}>
          <div data-test-id='primaryPurpose' style={{marginTop: '1rem'}}>{selectedPrimaryPurposeItem}</div>
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
      {workspace.researchPurpose.populationDetails.length > 0 && <React.Fragment>
        <div style={styles.sectionHeader}>Population of interest</div>
        <div style={styles.sectionContentContainer}>
          <div style={{marginTop: '0.5rem'}}>{getSelectedPopulations(workspace.researchPurpose)}</div>
        </div>
      </React.Fragment>}
    </FadeBox>;
  }
);
