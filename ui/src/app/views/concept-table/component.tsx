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
  selectedConcept: Concept[];
  concepts: Concept[];
  selectedVocabulary: string[];
}

export class ConceptTable extends React.Component<ConceptTableProps, ConceptTableState> {

  private dt: DataTable;
  constructor(props: ConceptTableProps) {
    super(props);
    this.state = {
      selectedConcept: [],
      concepts: this.props.concepts,
      selectedVocabulary: []
    };
  }

  synonymTemplate(rowData) {
    return <div>{fp.uniq(rowData.conceptSynonyms).join(', ')}</div>;
  }

  updateSelectedConceptList(selectedConcepts) {
    this.setState({selectedConcept : selectedConcepts});
    this.props.onSelectedChanged(selectedConcepts);
  }

  distinctVocabulary() {
    const vocabularyIds = this.props.concepts.map(concept => concept.vocabularyId);
    return fp.uniq(vocabularyIds);
  }


  filterVocabulary(vocabulary) {
    const selectedVocab =
        toggleIncludes(vocabulary, this.state.selectedVocabulary) as unknown as string[];
    this.dt.filter(selectedVocab, 'vocabularyId', 'in');
    this.setState({selectedVocabulary: selectedVocab});
  }

  componentWillReceiveProps(nextProps) {
    if (nextProps.concepts !==  this.state.concepts) {
      if (this.state.selectedVocabulary) {
        this.dt.filter([], 'vocabularyId', 'in');
        this.setState({selectedVocabulary : []});
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
                        checked={fp.includes(vocabulary, this.state.selectedVocabulary)}
                        onChange={(checked) => this.filterVocabulary(vocabulary)}>
              </CheckBox>
              <label style={{marginRight: '0.2rem'}}>{vocabulary}</label></div>;
          })}>
      <Clickable
          data-test-id='workspace-menu-button'
          hover={{opacity: 1}}>
        <img style={{width: '15%'}} src='/assets/icons/filter.svg'/>
      </Clickable>
    </PopupTrigger>;
    return <div>
      <DataTable ref={(el) => this.dt = el} value={this.props.concepts}
                 paginator={true} rows={100} scrollable={true} loading={this.props.loading}
                 selection={this.state.selectedConcept}
                 onSelectionChange={e => this.updateSelectedConceptList(e.value)} >
      <Column bodyStyle={{...styles.colStyle}} selectionMode='multiple' />
      <Column bodyStyle={{...styles.colStyle}} field='conceptName' header='Name'/>
      <Column bodyStyle={{...styles.colStyle}} field='conceptSynonyms' header='Synonyms'
              body={this.synonymTemplate}/>
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


  constructor() {
    super(ConceptTable, ['concepts', 'loading', 'onSelectedChanged']);
    this.onSelectedChanged = this.onSelectedChanged.bind(this);
  }

  selectedConcepts: Array<any> = [];

  onSelectedChanged(selectedConcepts: Concept[]) {
    console.log(this.selectedConcepts);
    this.getSelectedConcepts.emit(selectedConcepts);
  }
}
