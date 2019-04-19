import {Component, Input} from '@angular/core';
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


export class ConceptTable extends React.Component<{concepts: Concept[];
  loading: boolean; placeholderValue: string, onSelectConcepts: Function,
  selectedConcepts: Concept[]; },
  {selectedConcepts: Concept[]; selectedVocabularies: string[]; }> {

  private dt: DataTable;
  private filterImageSrc: string;

  constructor(props) {
    super(props);
    this.state = {
      selectedConcepts: props.selectedConcepts,
      selectedVocabularies: []
    };
    this.filterImageSrc = 'filter';
  }

  componentDidUpdate(prevProps) {
    if (this.state.selectedConcepts !== this.props.selectedConcepts) {
      // when parent has updated a set with selected concepts, unselect them from table
      this.setState({selectedConcepts: this.props.selectedConcepts});
    }
  }

  conceptSynonymColTemplate(rowData) {
    return <div>{fp.uniq(rowData.conceptSynonyms).join(', ')}</div>;
  }

  updateSelectedConceptList(selectedConcepts) {
    this.setState({selectedConcepts : selectedConcepts});
    this.props.onSelectConcepts(selectedConcepts);
  }

  distinctVocabulary() {
    const vocabularyIds = this.props.concepts.map(concept => concept.vocabularyId);
    return fp.uniq(vocabularyIds);
  }


  filterByVocabulary(vocabulary) {
    const selectedVocabularies =
        toggleIncludes(vocabulary, this.state.selectedVocabularies) as unknown as string[];
    this.filterImageSrc = selectedVocabularies.length > 0 ? 'filtered' : 'filter';
    this.dt.filter(selectedVocabularies, 'vocabularyId', 'in');
    this.setState({selectedVocabularies: selectedVocabularies});
  }

  componentWillReceiveProps(nextProps) {
    // The purpose of this is to reset the filter on vocabulary on change of domain/concepts
    if ((nextProps.concepts !==  this.props.concepts)) {
      if (this.state.selectedVocabularies) {
        this.dt.filter([], 'vocabularyId', 'in');
        this.setState({selectedVocabularies : []});
      }
    }
  }

  render() {
    const {selectedConcepts, selectedVocabularies} = this.state;
    const {concepts, placeholderValue, loading} = this.props;
    const vocabularyFilter = <PopupTrigger
        side='bottom'
        content={
          this.distinctVocabulary().map((vocabulary, i) => {
            return <div key={i}>
              <CheckBox style={{marginLeft: '0.2rem', marginRight: '0.3rem'}}
                        checked={fp.includes(vocabulary, selectedVocabularies)}
                        onChange={(checked) => this.filterByVocabulary(vocabulary)}>
              </CheckBox>
              <label style={{marginRight: '0.2rem'}}>{vocabulary}</label></div>;
          })}>
      <Clickable
          data-test-id='workspace-menu-button'
          hover={{opacity: 1}}>
        <img style={{width: '15%', marginLeft: '-2.5rem'}}
             src={'/assets/icons/' + this.filterImageSrc + '.svg'}/>
      </Clickable>
    </PopupTrigger>;
    return <div data-test-id='conceptTable'>
      <DataTable emptyMessage={loading ? '' : placeholderValue} ref={(el) => this.dt = el}
                 value={concepts} paginator={true} rows={50} scrollable={true} loading={loading}
                 selection={selectedConcepts}
                 onSelectionChange={e => this.updateSelectedConceptList(e.value)} >
      <Column bodyStyle={{...styles.colStyle, width: '3rem'}} headerStyle = {{width: '3rem'}}
              selectionMode='multiple' />
      <Column bodyStyle={styles.colStyle} field='conceptName' header='Name'/>
      <Column bodyStyle={styles.colStyle} field='conceptSynonyms' header='Synonyms'
              body={this.conceptSynonymColTemplate}/>
      <Column bodyStyle={styles.colStyle} field='conceptCode' header='Code'/>
      <Column field='vocabularyId' header='Vocabulary' bodyStyle={styles.colStyle}
              filter={true} headerStyle={{display: 'flex', textAlign: 'center', paddingTop: '0.6rem'
        , paddingLeft: '5rem'}}
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
  @Input() onSelectConcepts;
  @Input() loading = false;
  @Input() searchTerm = '';
  @Input() placeholderValue = '';
  @Input() selectedConcepts: Array<any> = [];

  constructor() {
    super(ConceptTable, ['concepts', 'loading', 'placeholderValue', 'onSelectConcepts',
      'selectedConcepts']);
  }
}
