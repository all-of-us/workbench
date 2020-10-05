import {Button, Clickable} from 'app/components/buttons';
import {FlexRow, FlexRowWrap} from 'app/components/flex';
import {ClrIcon} from 'app/components/icons';
import colors from 'app/styles/colors';
import {
  reactStyles,
  withCurrentConcept, withCurrentWorkspace
} from 'app/utils';
import {currentConceptStore, NavStore, setSidebarActiveIconStore} from 'app/utils/navigation';
import {WorkspaceData} from 'app/utils/workspace-data';
import {Domain, DomainCount} from 'generated';
import {ConceptSet} from 'generated/fetch';
import * as fp from 'lodash/fp';
import * as React from 'react';
import {ConceptAddModal} from './concept-add-modal';
import {ConceptSurveyAddModal} from './concept-survey-add-modal';
const styles = reactStyles({
  sectionTitle: {
    marginTop: '0',
    fontWeight: 600,
    color: colors.primary,
    marginBottom: '1rem'
  },
  selectionContainer: {
    background: colors.white,
    border: `2px solid ${colors.primary}`,
    borderRadius: '5px',
    height: 'calc(100% - 17rem)',
    lineHeight: '0.75rem',
    minHeight: 'calc(100vh - 15rem)',
    padding: '0.5rem',
    overflowX: 'hidden',
    overflowY: 'auto',
  },
  removeSelection: {
    background: 'none',
    border: 0,
    color: colors.danger,
    cursor: 'pointer',
    marginRight: '0.25rem',
    padding: 0
  },
  saveButton: {
    height: '2rem',
    borderRadius: '5px',
    fontWeight: 600,
    marginRight: '0.5rem'
  }
});

interface Props {
  workspace: WorkspaceData;
  concept: Array<any>;
}

interface State {
  conceptAddModalOpen: boolean;
  surveyAddModalOpen: boolean;
}
export const  ConceptListPage = fp.flow(withCurrentWorkspace(), withCurrentConcept())(
  class extends React.Component<Props, State> {
    constructor(props) {
      super(props);
      this.state = {
        conceptAddModalOpen: false,
        surveyAddModalOpen: false
      };
    }

    removeSelection(conceptToDel) {
      const updatedConceptList = this.props.concept.filter((concept) => concept !== conceptToDel);
      currentConceptStore.next(updatedConceptList);
    }

    afterConceptsSaved(conceptSet: ConceptSet) {
      const {namespace, id} = this.props.workspace;
      NavStore.navigate(['workspaces', namespace, id, 'data',
        'concepts', 'sets', conceptSet.id, 'actions']);
    }

    getDomainCount() {
      const domain: Domain = this.props.concept[0].domainId.toUpperCase() as Domain;
      const domainCount: DomainCount = {
        domain: domain,
        name: this.props.concept[0].domainId,
        conceptCount: this.props.concept.length
      };
      return domainCount;
    }

    openSaveDialog() {
      this.props.concept[0].name ?
         this.setState({conceptAddModalOpen: true}) :
         this.setState({surveyAddModalOpen: true}) ;
    }

    closeConceptAddModal() {
      this.setState({conceptAddModalOpen: false});
      setSidebarActiveIconStore.next(undefined);
    }

    closeSurveyAddModal() {
      this.setState({surveyAddModalOpen: false});
      setSidebarActiveIconStore.next(undefined);
    }

    render() {
      const {conceptAddModalOpen, surveyAddModalOpen} = this.state;
      return <div>
        <FlexRow><h3 style={styles.sectionTitle}>Selected Concepts</h3>
          <Clickable style={{marginRight: '1rem', position: 'absolute', right: '0px'}}
                     onClick={() => setSidebarActiveIconStore.next(undefined)}>
            <img src={'/assets/icons/times-light.svg'}
                 style={{height: '27px', width: '17px'}}
                 alt='Close'/>
          </Clickable></FlexRow>

        <div style={styles.selectionContainer}>
          {this.props.concept.map((con, index) => <FlexRow key={index} style={{lineHeight: '1.25rem'}}>
            <button style={styles.removeSelection} onClick={() => this.removeSelection(con)}>
              <ClrIcon shape='times-circle'/>
            </button>
            <b style={{paddingRight: '0.25rem'}}>{con.conceptCode}</b>
            {con.name ? con.name : con.question}
          </FlexRow>)}
        </div>
        <FlexRowWrap style={{flexDirection: 'row-reverse', marginTop: '1rem'}}>
          <Button type='primary'
                  style={styles.saveButton}
                  disabled={!this.props.concept || this.props.concept.length === 0}
                  onClick={() => this.openSaveDialog()}>Save Concept Set</Button>
          <Button type='link'
                  style={{color: colors.primary, left: 0}}
                  onClick={() => setSidebarActiveIconStore.next(undefined)}>
            Close
          </Button>
        </FlexRowWrap>
        {conceptAddModalOpen &&
        <ConceptAddModal activeDomainTab={this.getDomainCount()}
                         selectedConcepts={this.props.concept}
                         onSave={(conceptSet) => this.afterConceptsSaved(conceptSet)}
                         onClose={() => this.closeConceptAddModal()}/>}
        {surveyAddModalOpen &&
        <ConceptSurveyAddModal selectedSurvey={this.props.concept}
                               onClose={() => this.setState({surveyAddModalOpen: false})}
                               onSave={(conceptSet) => this.afterConceptsSaved(conceptSet)}
                               surveyName={this.props.concept[0].surveyName}/>}
        </div>;
    }
  });
