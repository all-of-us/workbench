import {Component, EventEmitter, Input, Output} from '@angular/core';
import {Clickable} from 'app/components/buttons';
import {CheckBox} from 'app/components/inputs';
import {PopupTrigger} from 'app/components/popups';
import {ReactWrapperBase, toggleIncludes} from 'app/utils';
import {reactStyles} from 'app/utils';
import {Concept} from 'generated/fetch/api';
import * as fp from 'lodash/fp';
import {Column} from 'primereact/column';
import {DataTable} from 'primereact/datatable';
import * as React from 'react';

const styles = reactStyles({
  colStyle: {
    lineHeight: '0.5rem',
    textAlign: 'center'
  }
});

interface ConceptTableProps {
  concepts: Concept[];
  loading: boolean;
  onSelectedChanged: Function;
}

interface ConceptTableState {
  selectedConcepts: Concept[];
  concepts: Concept[];
  selectedVocabularies: string[];
}

export class ConceptTable extends React.Component<ConceptTableProps, ConceptTableState> {

  private dt: DataTable;
  constructor(props: ConceptTableProps) {
    super(props);
    this.state = {
      selectedConcepts: [],
      selectedVocabularies: []
    };
  }

  conceptSynonymColTemplate(rowData) {
    return <div>{fp.uniq(rowData.conceptSynonyms).join(', ')}</div>;
  }

  updateSelectedConceptList(selectedConcepts) {
    this.setState({selectedConcepts : selectedConcepts});
    this.props.onSelectedChanged(selectedConcepts);
  }

  distinctVocabulary() {
    const vocabularyIds = this.props.concepts.map(concept => concept.vocabularyId);
    return fp.uniq(vocabularyIds);
  }


  filterByVocabulary(vocabulary) {
    const selectedVocabularies =
        toggleIncludes(vocabulary, this.state.selectedVocabularies) as unknown as string[];
    this.dt.filter(selectedVocabularies, 'vocabularyId', 'in');
    this.setState({selectedVocabularies: selectedVocabularies});
  }

  componentWillReceiveProps(nextProps) {
    // The purpose of this is to reset the filter on vocabulary on change of domain/concepts
    if (nextProps.concepts !==  this.props.concepts) {
      if (this.state.selectedVocabularies) {
        this.dt.filter([], 'vocabularyId', 'in');
        this.setState({selectedVocabularies : []});
      }
    }
  }

  render() {
    const vocabularyFilter = <PopupTrigger
        side='bottom'
        content={
          this.distinctVocabulary().map((vocabulary, i) => {
            return <div key={i}>
              <CheckBox style={{marginLeft: '0.2rem', marginRight: '0.3rem'}}
                        checked={fp.includes(vocabulary, this.state.selectedVocabularies)}
                        onChange={(checked) => this.filterByVocabulary(vocabulary)}>
              </CheckBox>
              <label style={{marginRight: '0.2rem'}}>{vocabulary}</label></div>;
          })}>
      <Clickable
          data-test-id='workspace-menu-button'
          hover={{opacity: 1}}>
        <img style={{width: '15%'}} src='/assets/icons/filter.svg'/>
      </Clickable>
    </PopupTrigger>;
    return <div data-test-id='conceptTable'>
      <DataTable ref={(el) => this.dt = el} value={this.props.concepts}
                 paginator={true} rows={100} scrollable={true} loading={this.props.loading}
                 selection={this.state.selectedConcepts}
                 onSelectionChange={e => this.updateSelectedConceptList(e.value)} >
      <Column bodyStyle={{...styles.colStyle}} selectionMode='multiple' />
      <Column bodyStyle={{...styles.colStyle}} field='conceptName' header='Name'/>
      <Column bodyStyle={{...styles.colStyle}} field='conceptSynonyms' header='Synonyms'
              body={this.conceptSynonymColTemplate}/>
      <Column bodyStyle={{...styles.colStyle}} field='conceptCode' header='Code'/>
      <Column field='vocabularyId' header='Vocabulary'
              filter={true} headerStyle={{display: 'flex', textAlign: 'center'}}
              filterElement={vocabularyFilter} />
      <Column style={styles.colStyle} field='countValue' header='Count'/>
    </DataTable>
    </div>;
  }
}

@Component({
  selector: 'app-concept-table',
  template: '<div #root></div>'
})
export class ConceptTableComponent extends ReactWrapperBase {
  @Input() concepts: Object[];
  @Output() getSelectedConcepts = new EventEmitter<any>(true);
  @Input() loading = false;
  @Input() searchTerm = '';
  @Input() placeholderValue = '';

  selectedConcepts: Array<any> = [];

  constructor() {
    super(ConceptTable, ['concepts', 'loading', 'onSelectedChanged']);
    this.onSelectedChanged = this.onSelectedChanged.bind(this);
  }

  onSelectedChanged(selectedConcepts: Concept[]) {
    this.selectedConcepts = selectedConcepts;
    this.getSelectedConcepts.emit(selectedConcepts);
  }
}
