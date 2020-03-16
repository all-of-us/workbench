import {FlexRow} from 'app/components/flex';
import {TextArea} from 'app/components/inputs';
import colors from 'app/styles/colors';
import * as React from 'react';
import {WorkspaceEditSection} from './workspace-edit-section';
import {ResearchPurposeQuestion} from './workspace-edit-text';
import {styles} from './workspace-styles';

interface Props {
  index: string;
  onChange: Function;
  researchPurpose: ResearchPurposeQuestion;
  researchValue: string;
  rowId: string;
}

interface State {
  showWarningMessage: boolean;
  researchValue: string;
  textColor: string;
}

export class WorkspaceResearchSummary extends React.Component<Props, State > {

  constructor(props: Props) {
    super(props);
    this.state = {
      researchValue: props.researchValue,
      showWarningMessage: false,
      textColor: colors.disabled
    };
  }

  onTextUpdate(value) {
    const {showWarningMessage, textColor} = this.state;
    if (showWarningMessage && value.length >= 50) {
      this.setState({showWarningMessage: false});
    }
    if (value.length > 1000) {
      value = value.substring(0, 1000);
    }
    if (value.length >= 950 && textColor !== colors.danger) {
      this.setState({textColor: colors.danger});
    } else if (value.length < 950 && textColor !== colors.disabled) {
      this.setState({textColor: colors.disabled});
    }
    this.setState({researchValue: value});
    this.props.onChange(value);
  }

  onBlur() {
    if (this.state.researchValue.length < 50) {
      this.setState({showWarningMessage: true});
    } else {
      this.setState({showWarningMessage: false});
    }
  }
  render() {
    const {index, researchPurpose, rowId} = this.props;
    const {researchValue, textColor} = this.state;
    return <WorkspaceEditSection data-test-id={rowId}
                                 header={researchPurpose.header}
                                 description={researchPurpose.description} index={index}
                                 indent>
      <TextArea style={styles.textArea}
                id={rowId} name={rowId} value={this.state.researchValue}
                onBlur={() => this.onBlur()}
                onChange={v => this.onTextUpdate(v)}/>

      <FlexRow id={rowId} style={styles.textBoxCharRemaining}>
        {this.state.showWarningMessage && <label data-test-id='warningMsg'
                                                 style={{color: colors.danger, justifyContent: 'flex-start'}}>
          The description you entered seems too short. Please consider adding more descriptive
          details to help the Program and your fellow Researchers understand your work.
        </label>}
        {researchValue.length === 1000 &&
        <label data-test-id='characterLimit' style={{color: colors.danger}}>
          You have reached the character limit for this question</label>}
        {researchValue &&
        <div data-test-id='characterMessage'
             style={{color: textColor, marginLeft: 'auto'}}>
          {1000 - researchValue.length} characters remaining</div>}
        {!researchValue &&
        <div data-test-id='characterMessage'
             style={{color: textColor, marginLeft: 'auto'}}>1000
          characters remaining</div>}
      </FlexRow>
    </WorkspaceEditSection>;
  }
}
