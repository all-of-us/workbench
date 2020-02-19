import {ResearchPurposeItems, SpecificPopulationItems} from 'app/pages/workspace/workspace-edit';
import {ResearchPurpose, SpecificPopulationEnum} from 'generated/fetch';

export function getSelectedResearchPurposeItems(researchPurpose: ResearchPurpose) {
  return ResearchPurposeItems.filter((item) =>
      researchPurpose[item.shortName]).map((item) => {
        let content = item.shortDescription;
        if (item.shortName === 'otherPurpose') {
          content += ': ' + researchPurpose.otherPurposeDetails;
        }
        if (item.shortName === 'diseaseFocusedResearch') {
          content += ': ' + researchPurpose.diseaseOfFocus;
        }
        return content;
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
