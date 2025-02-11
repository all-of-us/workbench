import * as React from 'react';

import { TextAreaWithLengthValidationMessage } from 'app/components/inputs';
import { ResearchPurposeQuestion } from 'app/utils/research-purpose-text';
import { NOT_ENOUGH_CHARACTERS_RESEARCH_DESCRIPTION } from 'app/utils/strings';

import { WorkspaceEditSection } from './workspace-edit-section';

interface Props {
  id: string;
  index: string;
  onChange: Function;
  researchPurpose: ResearchPurposeQuestion;
  researchValue: string;
  ariaLabel?: string;
}

export const WorkspaceResearchSummary = (props: Props) => {
  return (
    <WorkspaceEditSection
      dataTestId={props.id}
      header={props.researchPurpose.header}
      description={props.researchPurpose.description}
      index={props.index}
      indent
    >
      <TextAreaWithLengthValidationMessage
        id={props.id}
        initialText={props.researchValue}
        maxCharacters={1000}
        onChange={(s: string) => props.onChange(s)}
        tooShortWarningCharacters={100}
        tooShortWarning={NOT_ENOUGH_CHARACTERS_RESEARCH_DESCRIPTION}
        ariaLabel={
          props.ariaLabel
            ? `Text area describing the ${props.ariaLabel.toLowerCase()}`
            : undefined
        }
      />
    </WorkspaceEditSection>
  );
};
