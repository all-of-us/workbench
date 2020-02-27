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
  },
  sectionItemWithBackground: {
    padding: '10px',
    backgroundColor: colors.white,
    color: colors.primary,
    marginLeft: '0.5rem',
    borderRadius: '3px'
  },
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
  const categories = SpecificPopulationItems.filter(specificPopulationItem => specificPopulationItem
    .subCategory.filter(item => researchPurpose.populationDetails.includes(item.shortName)).length > 0);
  categories.forEach(category => category.subCategory = category.subCategory
    .filter(subCategoryItem => researchPurpose.populationDetails.includes(subCategoryItem.shortName)));
  return categories.map((selectedPopulationOfInterest, index) => {
    return <React.Fragment>
      <div key={index} style={{...styles.researchPurposeItemHeader, marginTop: index === 0 ? 0 : '0.5rem'}}>
        {selectedPopulationOfInterest.label}</div>
      {selectedPopulationOfInterest.subCategory.map((subCategory, subCategoryIndex) => <div style={{
        ...styles.sectionItemWithBackground, marginTop: '0.5rem'}} key={subCategoryIndex}>
        {subCategory.label}
      </div>)}
    </React.Fragment>;
  });
}
