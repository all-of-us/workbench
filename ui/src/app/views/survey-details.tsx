import {CheckBox} from 'app/components/inputs';
import {SpinnerOverlay} from 'app/components/spinners';
import {conceptsApi} from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import {reactStyles, withCurrentWorkspace} from 'app/utils';
import {WorkspaceData} from 'app/utils/workspace-data';
import {SurveyDetailsResponse} from 'generated/fetch';
import {Column} from 'primereact/column';
import {DataTable} from 'primereact/datatable';
import * as React from 'react';


const style = reactStyles({
  questionHeader: {
    height: 24,
    width: 351,
    color: colors.purple[0],
    fontFamily: 'Montserrat',
    fontSize: 20,
    fontWeight: 600,
    lineHeight: '24px',
    paddingTop: '1rem',
    textAlign: 'left',
    borderBottomColor: colors.white,
    backgroundColor: colors.white
  },
  questionLabel: {
    height: 18,
    width: 76,
    color: colors.blue[8],
    fontFamily: 'Montserrat',
    fontSize: 14,
    fontWeight: 600,
    lineHeight: '18px'
  },
  question: {
    color: colors.blue[8],
    fontFamily: 'Montserrat',
    fontSize: 16,
    lineHeight: 19,
    paddingTop: '0.3rem',
    paddingBottom: '0.3rem'
  }
});

interface Props {
  surveyName: string;
  workspace: WorkspaceData;
  surveySelected: Function;
}

interface State {
  surveyList: Array<SurveyDetailsResponse>;
  loading: boolean;
  expandedRows: Array<any>;
  selectedQuestions: Array<SurveyDetailsResponse>;
}

export const SurveyDetails = withCurrentWorkspace()(
  class extends React.Component<Props, State> {
    constructor(props) {
      super(props);
      this.state = {
        surveyMap: undefined,
        surveyList: [],
        loading: true,
        expandedRows: [],
        selectedQuestions: []
      };
    }

    componentDidMount() {
      this.loadSurveyDetails();
    }

    async loadSurveyDetails() {
      try {
        const {workspace, surveyName} = this.props;
        const surveys = await conceptsApi().getSurveyDetails(
          workspace.namespace, workspace.id, surveyName);
        this.setState({surveyList: surveys, loading: false});
      } catch (ex) {
        console.log(ex);
      }
    }

    setSelectedSurveyQuestion(question, select) {
      const selectedQuestions = this.state.selectedQuestions;
      select ? selectedQuestions.push(question) :
        selectedQuestions.splice(selectedQuestions.findIndex(question), 1);
      this.setState({selectedQuestions: selectedQuestions});
      this.props.surveySelected(selectedQuestions);
    }

    getQuestion(row, col) {
      return <div style={{display: 'flex', flexDirection: 'column'}}>
      <div style={{display: 'flex'}}>
        <CheckBox style={{marginTop: '0.3rem'}}
                  onChange={(value) => this.setSelectedSurveyQuestion(row, value)}/>
       <div style={{paddingLeft: '0.5rem'}}>
         <label style={style.questionLabel}>Question {col.rowIndex + 1}</label>
         <div style={style.question}>{row.question}</div>
       </div>
      </div>
        <a>See my Answers</a>
      </div>;
    }

    getTableHeader() {
      return this.props.surveyName + ' - Survey Question';
    }

    render() {
      const {loading, surveyList} = this.state;
      return <div>
        {loading ? <SpinnerOverlay/> :
        <DataTable value={surveyList} expandedRows={this.state.expandedRows}>
          <Column headerStyle={...style.questionHeader} header ={this.getTableHeader()}
                  body={(row, col) => this.getQuestion(row, col)}/>
        </DataTable>}
      </div>;
    }
  });
