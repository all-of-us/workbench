import {CheckBox} from 'app/components/inputs';
import {SpinnerOverlay} from 'app/components/spinners';
import {conceptsApi} from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import {reactStyles, withCurrentWorkspace} from 'app/utils';
import {WorkspaceData} from 'app/utils/workspace-data';
import {SurveyAnswerResponse, SurveyQuestionsResponse} from 'generated/fetch';
import {Column} from 'primereact/column';
import {DataTable} from 'primereact/datatable';
import * as React from 'react';

interface SurveyDetails {
  question: SurveyQuestionsResponse;
  answer: Array<SurveyAnswerResponse>;
}

const style = reactStyles({
  questionHeader: {
    height: 24,
    color: colors.purple[0],
    fontFamily: 'Montserrat',
    fontSize: '20px',
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
    fontSize: '14px',
    fontWeight: 600,
    lineHeight: '18px'
  },
  columnLabel: {
    color: colors.blue[8],
    fontFamily: 'Montserrat',
    fontSize: '16px',
    lineHeight: '19px',
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
  surveyList: Array<SurveyDetails>;
  answerLoading: Array<boolean>;
  loading: boolean;
  expandedRows: Array<any>;
  selectedQuestions: Array<SurveyQuestionsResponse>;
  seeMyAnswers: Array<boolean>;
}

export const SurveyDetails = withCurrentWorkspace()(
  class extends React.Component<Props, State> {
    constructor(props) {
      super(props);
      this.state = {
        surveyList: [],
        answerLoading: [],
        loading: true,
        expandedRows: [],
        selectedQuestions: [],
        seeMyAnswers: [],
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
        const seeMyAnsList = [];
        surveys.forEach(survey => seeMyAnsList.push(false));
        const surveyDetails = [];
        surveys.map((survey) => {
          const surveyQuestionAnswer = {
            question: survey,
            answer: []
          };
          surveyDetails.push(surveyQuestionAnswer);
        });
        this.setState({surveyList: surveyDetails, loading: false, seeMyAnswers: seeMyAnsList});
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
      return  <div style={{display: 'flex', flexDirection: 'column'}}>
      <div style={{display: 'flex'}}>
        <CheckBox style={{marginTop: '0.3rem'}}
                  onChange={(value) => this.setSelectedSurveyQuestion(row, value)}/>
       <div style={{paddingLeft: '0.5rem'}}>
         <label style={style.questionLabel}>Question {col.rowIndex + 1}</label>
         <div style={style.columnLabel}>{row.question}</div>
       </div>
      </div>
        <a href='#' onClick={(e) => {e.preventDefault(); this.updateSeeMyAnswer(col.rowIndex); }}>
          See my Answers
        </a>
        {this.state.seeMyAnswers[col.rowIndex] && this.renderAnswerTable(col.rowIndex)}
      </div>;
    }

    renderAnswerTable(index) {
      const {answerLoading, surveyList} = this.state;
      return <DataTable emptyMessage={answerLoading[index] ? '' : 'No records found'}
                        loading={answerLoading[index]} paginator={true} rows={10}
                        value={surveyList[index].answer}>
        <Column headerStyle={style.questionHeader} style={{...style.columnLabel, fontSize: '13px'}}
                field='answer'/>
        <Column headerStyle={{...style.questionHeader, fontSize: '14px'}}
                style={{...style.columnLabel, fontSize: '13px'}}
                field='conceptId' header='Concept Code'/>
        <Column field='participationCount' header='Participation Count'
                headerStyle={{...style.questionHeader, fontSize: '14px'}}/>
        <Column field='percentAnswered' header='% answered'
                headerStyle={{...style.questionHeader, fontSize: '14px'}}/>
          </DataTable>;
    }

    async updateSeeMyAnswer(index) {
      const answerLoadinglist = this.state.answerLoading;
      answerLoadinglist[index] = true;
      this.setState({answerLoading: answerLoadinglist});
      const {workspace} = this.props;
      const seeMyAnsList = this.state.seeMyAnswers;
      seeMyAnsList[index] = !this.state.seeMyAnswers[index];
      this.setState({seeMyAnswers: seeMyAnsList});
      // Call api to retrieve answer if it has never been called for the question before
      if (this.state.surveyList[index].answer && this.state.surveyList[index].answer.length === 0) {
        const answers =
            await conceptsApi().getSurveyAnswers(workspace.namespace, workspace.id,
              this.state.surveyList[index].question.conceptId);
        const answersList = this.state.surveyList;
        answersList[index].answer =  answers;
        this.setState({surveyList: answersList});
      }
      answerLoadinglist[index] = false;
      this.setState({answerLoading: answerLoadinglist});

    }

    getTableHeader() {
      return this.props.surveyName + ' - Survey Question';
    }

    render() {
      const {loading, surveyList} = this.state;
      return <div>
        {loading ? <SpinnerOverlay/> :
        <DataTable  value={surveyList} expandedRows={this.state.expandedRows}>
          <Column headerStyle={style.questionHeader} header ={this.getTableHeader()}
                  body={(row, col) => this.getQuestion(row.question, col)}/>
        </DataTable>}
      </div>;
    }
  });
