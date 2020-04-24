import {FlexRow} from 'app/components/flex';
import {TextArea, TextAreaWithLengthValidationMessage} from 'app/components/inputs';
import colors from 'app/styles/colors';
import {colorWithWhiteness} from 'app/styles/colors';
import {reactStyles} from 'app/utils';
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

export class WorkspaceResearchSummary extends React.Component<Props> {

  constructor(props: Props) {
    super(props);
  }

  render() {
    const {id, index, onChange, researchPurpose, researchValue} = this.props;
    return <WorkspaceEditSection data-test-id={id}
                                 header={researchPurpose.header}
                                 description={researchPurpose.description} index={index}
                                 indexStyle={{marginRight: '0.2rem'}}
                                 indent>
      <TextAreaWithLengthValidationMessage
          id={id}
          initialText={researchValue}
          maxCharacters={1000}
          onChange={(s: string) => onChange(s)}
          tooLongWarningCharacters={950}
          tooShortWarningCharacters={50}
          tooShortWarning={"The description you entered seems too short. Please consider " +
            "adding more descriptive details to help the Program and your fellow Researchers " +
            "understand your work."}
       />
    </WorkspaceEditSection>;
  }
}
