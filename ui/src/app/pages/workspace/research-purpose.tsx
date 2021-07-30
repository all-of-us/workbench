import * as fp from 'lodash/fp';
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
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {reactStyles, withCurrentWorkspace} from 'app/utils';
import {AnalyticsTracker} from 'app/utils/analytics';
import {useNavigation} from 'app/utils/navigation';
import {
  getSelectedPopulations,
  getSelectedResearchPurposeItems
} from 'app/utils/research-purpose';
import {serverConfigStore} from 'app/utils/stores';
import {WorkspaceData} from 'app/utils/workspace-data';
import {WorkspacePermissionsUtil} from 'app/utils/workspace-permissions';
import {withNavigation} from '../../utils/navigation-wrapper';

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
    marginTop: '0.3rem',
    borderStyle: 'solid',
    height: '2.5rem',
    color: colors.primary,
    alignItems: 'center',
    justifyContent: 'center',
    borderColor: colors.warning,
    borderRadius: '0.4rem',
    borderWidth: '0.1rem',
    backgroundColor: colorWithWhiteness(colors.highlight, 0.7)
  }
});

export const ResearchPurpose = fp.flow(withCurrentWorkspace(), withNavigation)(
  ({workspace}: {workspace: WorkspaceData}) => {
    const [navigate, ] = useNavigation();
    const isOwner = WorkspacePermissionsUtil.isOwner(workspace.accessLevel);
    const selectedResearchPurposeItems = getSelectedResearchPurposeItems(workspace.researchPurpose, true);
    const selectedPrimaryPurposeItems = getSelectedResearchPurposeItems(workspace.researchPurpose, false);

    const updateWorkspaceEvent = () => {
      AnalyticsTracker.WorkspaceUpdatePrompt.UpdateWorkspace();
      navigate(['workspaces', workspace.namespace, workspace.id, 'edit']);
    };

    const updateWorkspaceRPReviewPrompt = () => {
      workspacesApi().markResearchPurposeReviewed(workspace.namespace, workspace.id)
        .then((markedWorkspace) => {
          workspace.researchPurpose.needsReviewPrompt = false;
          navigate(
            ['workspaces',  markedWorkspace.namespace, markedWorkspace.id, 'about']);
        });
    };

    const looksGoodEvent = () => {
      AnalyticsTracker.WorkspaceUpdatePrompt.LooksGood();
      updateWorkspaceRPReviewPrompt();
    };

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
      {serverConfigStore.get().config.enableResearchReviewPrompt && isOwner
        && workspace.researchPurpose.needsReviewPrompt && <FlexRow style={styles.reviewPurposeReminder}>
        <ClrIcon style={{color: colors.warning, marginLeft: '0.3rem'}} className='is-solid'
        shape='exclamation-triangle' size='25'/>
        <FlexColumn style={{paddingRight: '0.5rem', paddingLeft: '0.5rem', color: colors.primary}}>
        <label style={{fontWeight: 600, fontSize: '14px', flex: 1}}>
          Please review your workspace description to make sure it is accurate.</label>
          <label>Project descriptions are publicly cataloged in the <a
              href='https://www.researchallofus.org/research-projects-directory/' target='_blank'>
            Research Project Directory</a> for participants and public to review.</label>
        </FlexColumn>
        <div style={{marginLeft: 'auto', marginRight: '0.5rem'}}>
        <a style={{marginRight: '0.5rem'}} onClick={() => looksGoodEvent()}>Looks
        Good</a>
        |
        <a style={{marginLeft: '0.5rem'}} onClick={() => updateWorkspaceEvent()}>Update</a>
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
      <div style={styles.sectionHeader}>Findings will be disseminated via:</div>
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
