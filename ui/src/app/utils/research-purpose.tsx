import * as React from 'react';

import {ResearchPurposeItems, SpecificPopulationItems} from 'app/pages/workspace/workspace-edit-text';
import colors from 'app/styles/colors';
import {ResearchPurpose, SpecificPopulationEnum} from 'generated/fetch';

const styles = {
  researchPurposeItemHeader: {
    fontSize: '14px',
    fontWeight: 600,
    color: colors.primary
  },
  researchPurposeItemDescription: {
    fontSize: '14px',
    fontWeight: 400,
    color: colors.primary,
    lineHeight: '22px'
  }
};

export function getSelectedResearchPurposeItems(researchPurpose: ResearchPurpose) {
  return ResearchPurposeItems.filter((item) =>
      researchPurpose[item.shortName]).map((item) => {
        const headerContent = <div style={styles.researchPurposeItemHeader}>{item.shortDescription}</div>;
        let descriptiveContent = <div style={styles.researchPurposeItemDescription}>{item.longDescription}</div>;
        if (item.shortName === 'otherPurpose') {
          descriptiveContent = <div style={styles.researchPurposeItemDescription}>{researchPurpose.otherPurposeDetails}</div>;
        } else if (item.shortName === 'diseaseFocusedResearch') {
          descriptiveContent = <div style={{...styles.researchPurposeItemDescription,
            backgroundColor: colors.white, borderRadius: '3px', padding: '10px'}}>
            {researchPurpose.diseaseOfFocus}
          </div>;
        }
        return <div>{headerContent}{descriptiveContent}</div>;
      });
}

export function getSelectedPopulations(researchPurpose: ResearchPurpose) {
  const populations = SpecificPopulationItems.filter(sp =>
      researchPurpose.populationDetails.includes(sp.shortName))
  .map(sp => sp.ubrLabel);
  if (researchPurpose.populationDetails.includes(SpecificPopulationEnum.OTHER)) {
    populations.push('Other: ' + researchPurpose.otherPopulationDetails);
  }
  return populations;
}
