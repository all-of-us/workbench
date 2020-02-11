import {ResearchPurposeItems, SpecificPopulationItems} from 'app/pages/workspace/workspace-edit';
import {SpecificPopulationEnum, Workspace} from 'generated/fetch';

export function getSelectedResearchPurposeItems(workspace: Workspace) {
  return ResearchPurposeItems.filter((item) =>
      workspace.researchPurpose[item.shortName]).map((item) => {
        let content = item.shortDescription;
        if (item.shortName === 'otherPurpose') {
          content += ': ' + workspace.researchPurpose.otherPurposeDetails;
        }
        if (item.shortName === 'diseaseFocusedResearch') {
          content += ': ' + workspace.researchPurpose.diseaseOfFocus;
        }
        return content;
      });
}

export function getSelectedPopulations(workspace: Workspace) {
  const populations = SpecificPopulationItems.filter(sp =>
      workspace.researchPurpose.populationDetails.includes(sp.shortName))
  .map(sp => sp.ubrLabel);
  if (workspace.researchPurpose.populationDetails.includes(SpecificPopulationEnum.OTHER)) {
    populations.push('Other: ' + workspace.researchPurpose.otherPopulationDetails);
  }
  return populations;
}
