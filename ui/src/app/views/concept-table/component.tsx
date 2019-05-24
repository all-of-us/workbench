import {Component, Input} from '@angular/core';
import {Clickable, Link} from 'app/components/buttons';
import {ClrIcon} from 'app/components/icons';
import {CheckBox} from 'app/components/inputs';
import {PopupTrigger, TooltipTrigger} from 'app/components/popups';
import * as Color from 'color';
import colors from 'app/styles/colors';
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
  },
  akaText: {
    minWidth: '170px',
    maxWidth: '170px',
    fontStyle: 'italic',
    color: colors.gray[2]
  },
  akaIcon: {
    marginLeft: 10,
    verticalAlign: 'middle',
    color: colors.blue[3]
  },
  highlighted: {
    color: colors.green[3],
    backgroundColor: Color(colors.green[3]).alpha(0.2).toString(),
    display: 'inline-block'
  }
});

interface SynonymsObjectState {
  seeMore: boolean;
  willOverflow: boolean;
}
const ROWS_TO_DISPLAY = 10;
export class SynonymsObject extends React.Component<{}, SynonymsObjectState> {
  domElement: any;
  constructor(props) {
    super(props);
    this.state = {seeMore: false, willOverflow: false};
  }

  componentDidMount() {
    const element = this.domElement;
    const hasOverflowingChildren = element.offsetHeight < element.scrollHeight ||
      element.offsetWidth < element.scrollWidth;
    this.setState({willOverflow: hasOverflowingChildren});
  }

  render() {
    const {seeMore, willOverflow} = this.state;
    return <div style={{display: 'flex'}}>
      <div style={styles.akaText}>
        Also Known As:
        <TooltipTrigger
          side='top'
          content='Medical concepts often have alternative names and descriptions,
            known as synonyms. Alternate names and descriptions, if available, are
            listed for each medical concept'>
          <ClrIcon
            shape='info-standard'
            className='is-solid'
            style={styles.akaIcon}
          />
        </TooltipTrigger>
      </div>
      <div style={{
        textOverflow: seeMore ? 'auto' : 'hidden',
        minWidth: '810px',
        maxWidth: '810px',
        fontSize: '12px',
        height: seeMore ? 'auto' : '1rem',
        overflow: seeMore ? 'auto' : 'hidden'
      }} ref={el => this.domElement = el}>
        {this.props.children}
      </div>
      {willOverflow ?
        <Link onClick={() => this.setState({seeMore: !seeMore})}>
          {seeMore ? 'See Less' : 'See More...'}
        </Link> : null}
    </div>;
  }
}

interface Props {
  concepts: Concept[];
  loading: boolean;
  onSelectConcepts: Function;
  nextPage?: Function;
  placeholderValue: string;
  reactKey: string;
  searchTerm?: string;
  selectedConcepts: Concept[];
}

interface State {
  first: number;
  pageLoading: boolean;
  selectedConcepts: Concept[];
  selectedVocabularies: string[];
  totalRecords: number;
  pageConcepts: Concept[];
  pageNumber: number;
}

export class ConceptTable extends React.Component<Props, State> {

  private dt: DataTable;
  private filterImageSrc: string;

  constructor(props) {
    super(props);
    this.state = {
      selectedConcepts: props.selectedConcepts,
      selectedVocabularies: [],
      pageLoading: false,
      first: 0,
      totalRecords: props.concepts.length,
      pageNumber: 0,
      pageConcepts: props.concepts.slice(0, 10)
    };
    this.filterImageSrc = 'filter';
  }

  componentDidUpdate(prevProps) {
    if (this.state.selectedConcepts !== this.props.selectedConcepts) {
      // when parent has updated a set with selected concepts, unselect them from table
      this.setState({selectedConcepts: this.props.selectedConcepts});
    }
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
      if (nextProps.concepts !== this.props.concepts && nextProps.concepts.length > 0 ) {
        this.setState({totalRecords: nextProps.concepts.length});

        // Update pageConcepts only for the first time/page.
        // onPage() will update for the rest of the pages
        if (this.state.pageNumber === 0 ) {
          this.setState({pageConcepts: nextProps.concepts.slice(0, 10)});
        }
      }
    }
  }

  rowExpansionTemplate(data) {
    return (<SynonymsObject>
      {this.highlightWithSearchTerm(fp.uniq(data.conceptSynonyms).join(', '))}
    </SynonymsObject>);
  }

  highlightWithSearchTerm(stringToHighlight: string) {
    const {searchTerm} = this.props;
    if (!searchTerm) {
      return stringToHighlight;
    }
    const words: string[] = [];
    let searchWords = searchTerm.split(new RegExp(',| '));
    searchWords = searchWords
      .filter(w => w.length > 0 )
      .map(word => word.replace(/[&!^\/\\#,+()$~%.'":*?<>{}]/g, ''));
    const matchString = new RegExp(searchWords.join('|'), 'i');
    const matches = stringToHighlight.match(new RegExp(matchString, 'gi'));
    const splits = stringToHighlight.split(new RegExp(matchString, 'gi'));
    if (matches) {
      for (let i = 0; i < matches.length; i++) {
        words.push(splits[i], matches[i]);
      }
      words.push(splits[splits.length - 1]);
    }
    return words.map(word => <span
      style={matchString.test(word.toLowerCase()) ? styles.highlighted : {}}>
        {word}
      </span>);
  }

  async onPage(event) {
    this.setState({pageLoading: true});

    // Call next set of concepts only if user is on the last page
    if ((event.page + 1) === event.pageCount) {
      const pageCount = Math.ceil(this.state.totalRecords / 100);
      await this.props.nextPage(pageCount);
      this.setState({pageNumber: pageCount, pageLoading: true});
    }

    const startIndex = event.first;
    const endIndex = event.first + event.rows;

    this.setState({
      first: event.first,
      pageConcepts: this.props.concepts.slice(startIndex, endIndex),
      pageLoading: false
    });
  }

  render() {
    const {pageConcepts, pageLoading, selectedConcepts, selectedVocabularies} = this.state;
    const {placeholderValue, loading, reactKey} = this.props;
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
    return <div data-test-id='conceptTable' key={reactKey}>
      <DataTable emptyMessage={loading ? '' : placeholderValue} ref={(el) => this.dt = el}
                 value={pageConcepts} scrollable={true}
                 selection={selectedConcepts} style={{minWidth: 1100}}
                 totalRecords={this.state.totalRecords}
                 expandedRows={this.props.concepts
                   .filter(concept => concept.conceptSynonyms.length > 0)}
                 rowExpansionTemplate={(data) => this.rowExpansionTemplate(data)}
                 paginator={true} rows={ROWS_TO_DISPLAY}
                 onPage={(event) => this.onPage(event)}
                 loading={pageLoading}
                 lazy={true} first={this.state.first}
                 data-test-id='conceptRow'
                 onSelectionChange={e => this.updateSelectedConceptList(e.value)} >
      <Column bodyStyle={{...styles.colStyle, width: '3rem'}} headerStyle = {{width: '3rem'}}
              data-test-id='conceptCheckBox' selectionMode='multiple' />
      <Column bodyStyle={styles.colStyle} field='conceptName' header='Name'
              data-test-id='conceptName'/>
      <Column bodyStyle={styles.colStyle} field='conceptCode' header='Code'/>
      <Column field='vocabularyId' header='Vocabulary' bodyStyle={styles.colStyle}
              filter={true} headerStyle={{display: 'flex', textAlign: 'center',
                paddingTop: '0.6rem', paddingLeft: '5rem'}}
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
  @Input() placeholderValue = '';
  @Input() searchTerm;
  @Input() selectedConcepts: Array<any> = [];

  constructor() {
    super(ConceptTable, ['concepts', 'loading', 'placeholderValue', 'onSelectConcepts',
      'searchTerm', 'selectedConcepts']);
  }
}
