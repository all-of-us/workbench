import {TextAreaWithLengthValidationMessage} from 'app/components/inputs';
import {NOT_ENOUGH_CHARACTERS_RESEARCH_DESCRIPTION} from 'app/utils/strings';
import * as React from 'react';

import {WorkspaceEditSection} from './workspace-edit-section';
import {ResearchPurposeQuestion} from './workspace-edit-text';


interface Props {
  id: string;
  index: string;
  onChange: Function;
  researchPurpose: ResearchPurposeQuestion;
  researchValue: string;
}

export const WorkspaceResearchSummary = (props: Props) => {
  return <WorkspaceEditSection
        data-test-id={props.id}
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
          tooLongWarningCharacters={950}
          tooShortWarningCharacters={100}
          tooShortWarning={NOT_ENOUGH_CHARACTERS_RESEARCH_DESCRIPTION}
       />
    </WorkspaceEditSection>;
};
