import * as React from 'react';

import { ResearchPurpose } from 'generated/fetch';

import colors, { colorWithWhiteness } from 'app/styles/colors';
import { reactStyles } from 'app/utils';
import {
  getSelectedPopulations,
  getSelectedResearchPurposeItems,
} from 'app/utils/research-purpose';
import {
  aianResearchTypeMap,
  disseminateFindings,
  researchOutcomes,
  researchPurposeQuestions,
} from 'app/utils/research-purpose-text';

const styles = reactStyles({
  editIcon: {
    marginTop: '0.15rem',
    height: 22,
    width: 22,
    fill: colors.light,
    backgroundColor: colors.accent,
    padding: '5px',
    borderRadius: '23px',
  },
  mainHeader: {
    fontSize: '18px',
    fontWeight: 600,
    color: colors.primary,
    marginBottom: '0.75rem',
    display: 'flex',
    flexDirection: 'row',
    alignItems: 'center',
  },
  sectionContentContainer: {
    marginLeft: '1.5rem',
  },
  sectionHeader: {
    fontSize: '16px',
    fontWeight: 600,
    color: colors.primary,
    marginTop: '1.5rem',
  },
  sectionItemWithBackground: {
    padding: '10px',
    backgroundColor: 'transparent',
    color: colors.primary,
    marginLeft: '0.75rem',
    borderRadius: '3px',
  },
  sectionSubHeader: {
    fontSize: '14px',
    fontWeight: 600,
    color: colors.primary,
    marginTop: '0.75rem',
  },
  sectionText: {
    fontSize: '14px',
    lineHeight: '24px',
    color: colors.primary,
    marginTop: '0.45rem',
  },
  reviewPurposeReminder: {
    marginTop: '0.45rem',
    borderStyle: 'solid',
    height: '3.75rem',
    color: colors.primary,
    alignItems: 'center',
    justifyContent: 'center',
    borderColor: colors.warning,
    borderRadius: '0.6rem',
    borderWidth: '0.15rem',
    backgroundColor: colorWithWhiteness(colors.highlight, 0.7),
  },
});

interface Props {
  researchPurpose: ResearchPurpose;
  showAIAN: boolean;
}

export const ResearchPurposeSection = ({
  researchPurpose,
  showAIAN,
}: Props) => {
  const {
    intendedStudy,
    anticipatedFindings,
    populationDetails,
    scientificApproach,
    disseminateResearchFindingList,
    researchOutcomeList,
    aianResearchType,
    aianResearchDetails,
  } = researchPurpose;

  const selectedResearchPurposeItems = getSelectedResearchPurposeItems(
    researchPurpose,
    true
  );
  const selectedPrimaryPurposeItems = getSelectedResearchPurposeItems(
    researchPurpose,
    false
  );

  return (
    <div>
      <div style={styles.sectionHeader}>Primary purpose of project</div>
      <div style={styles.sectionContentContainer}>
        {selectedResearchPurposeItems &&
          selectedResearchPurposeItems.length > 0 && (
            <div style={styles.sectionSubHeader}>Research Purpose</div>
          )}
        {selectedResearchPurposeItems.map((selectedResearchPurposeItem, i) => (
          <div key={i}>
            <div
              data-test-id='primaryResearchPurpose'
              style={{
                marginTop: i > 0 ? '1.5rem' : '0.45rem',
                marginLeft: '1.5rem',
              }}
            >
              {selectedResearchPurposeItem}
            </div>
          </div>
        ))}
      </div>
      <div style={styles.sectionContentContainer}>
        {selectedPrimaryPurposeItems.map((selectedPrimaryPurposeItem, i) => (
          <div key={i}>
            <div data-test-id='primaryPurpose' style={{ marginTop: '1.5rem' }}>
              {selectedPrimaryPurposeItem}
            </div>
          </div>
        ))}
      </div>
      <div style={styles.sectionHeader}>Summary of research purpose</div>
      <div style={styles.sectionContentContainer}>
        {/* Intended study section*/}
        <div style={styles.sectionSubHeader}>
          {researchPurposeQuestions[2].header}
        </div>
        <div style={{ ...styles.sectionItemWithBackground, padding: '15px' }}>
          {intendedStudy}
        </div>

        {/* Scientific approach section*/}
        <div style={styles.sectionSubHeader}>
          {researchPurposeQuestions[3].header}
        </div>
        <div style={{ ...styles.sectionItemWithBackground, padding: '15px' }}>
          {scientificApproach}
        </div>

        {/* Anticipated findings section*/}
        <div style={styles.sectionSubHeader}>
          {researchPurposeQuestions[4].header}
        </div>
        <div style={{ ...styles.sectionItemWithBackground, padding: '15px' }}>
          {anticipatedFindings}
        </div>
      </div>

      {/* Findings section*/}
      <div style={styles.sectionHeader}>Findings will be disseminated via:</div>
      <div style={styles.sectionContentContainer}>
        {disseminateResearchFindingList.map((disseminateFinding, i) => (
          <div
            key={i}
            style={{
              ...styles.sectionItemWithBackground,
              marginTop: '0.75rem',
            }}
          >
            {
              disseminateFindings.find(
                (finding) => finding.shortName === disseminateFinding
              ).label
            }
          </div>
        ))}
      </div>

      {/* Outcomes section*/}
      <div style={styles.sectionHeader}>
        Outcomes anticipated from the research:
      </div>
      <div style={styles.sectionContentContainer}>
        {researchOutcomeList.map((workspaceOutcome, i) => (
          <div
            key={i}
            style={{
              ...styles.sectionItemWithBackground,
              marginTop: '0.75rem',
            }}
          >
            {
              researchOutcomes.find(
                (outcome) => outcome.shortName === workspaceOutcome
              ).label
            }
          </div>
        ))}
      </div>

      {/* Underserved populations section*/}
      {populationDetails.length > 0 && (
        <React.Fragment>
          <div style={styles.sectionHeader}>Population of interest</div>
          <div style={styles.sectionContentContainer}>
            <div style={{ marginTop: '0.75rem' }}>
              {getSelectedPopulations(researchPurpose)}
            </div>
          </div>
        </React.Fragment>
      )}

      {/* AI/AN Research Approach section*/}
      {showAIAN && (
        <>
          <div style={styles.sectionHeader}>AI/AN Research Approach</div>
          <div style={styles.sectionContentContainer}>
            <div style={styles.sectionSubHeader}>
              {researchPurposeQuestions[10].header}
            </div>
            <div
              style={{ ...styles.sectionItemWithBackground, padding: '15px' }}
            >
              {aianResearchTypeMap.get(aianResearchType)}
            </div>
            <div style={styles.sectionSubHeader}>
              {researchPurposeQuestions[11].header}
            </div>
            <div
              style={{ ...styles.sectionItemWithBackground, padding: '15px' }}
            >
              {aianResearchDetails}
            </div>
          </div>
        </>
      )}
    </div>
  );
};
