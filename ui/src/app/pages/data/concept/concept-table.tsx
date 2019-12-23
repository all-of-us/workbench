import {Clickable, Link} from 'app/components/buttons';
import {FlexRow} from 'app/components/flex';
import {ClrIcon} from 'app/components/icons';
import {TooltipTrigger} from 'app/components/popups';
import {SpinnerOverlay} from 'app/components/spinners';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {reactStyles} from 'app/utils';
import {Concept} from 'generated/fetch/api';
import * as fp from 'lodash/fp';
import {Column} from 'primereact/column';
import {DataTable} from 'primereact/datatable';
import * as React from 'react';

function formatCounts(concept: any) {
  concept.countValue = concept.countValue.toLocaleString();
  return concept;
}

const styles = reactStyles({
  colStyle: {
    lineHeight: '0.5rem',
    textAlign: 'center'
  },
  akaText: {
    minWidth: '170px',
    maxWidth: '170px',
    fontStyle: 'italic',
    color: colors.primary
  },
  akaIcon: {
    marginLeft: 10,
    verticalAlign: 'middle',
    color: colorWithWhiteness(colors.accent, 0.5)
  },
  highlighted: {
    color: colorWithWhiteness(colors.success, -0.4),
    backgroundColor: colorWithWhiteness(colors.success, 0.7),
    padding: '0 0.25rem',
    display: 'inline-block'
  }
});

interface SynonymsObjectState {
  seeMore: boolean;
  willOverflow: boolean;
}
const ROWS_TO_DISPLAY = 20;
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
  placeholderValue: string;
  reactKey: string;
  searchTerm?: string;
  selectedConcepts: Concept[];
}

interface State {
  first: number;
  pageLoading: boolean;
  selectedConcepts: Concept[];
  showBanner: boolean;
  selectAll: boolean;
  totalRecords: number;
  pageConcepts: Concept[];
  pageNumber: number;
  tableRef: any;
  pageNum: number;
}

export class ConceptTable extends React.Component<Props, State> {

  constructor(props) {
    super(props);
    this.state = {
      selectedConcepts: props.selectedConcepts,
      showBanner: false,
      selectAll: false,
      pageLoading: false,
      first: 0,
      totalRecords: props.concepts.length,
      pageNumber: 1,
      pageConcepts: props.concepts.slice(0, 10).map(formatCounts),
      tableRef: React.createRef(),
      pageNum: 1
    };
  }

  componentDidUpdate(prevProps) {
    if (this.state.selectedConcepts !== this.props.selectedConcepts) {
      // when parent has updated a set with selected concepts, unselect them from table
      this.setState({selectedConcepts: this.props.selectedConcepts});
    }
  }

  updateSelectedConceptList(selectedConcepts, origin) {
    // By default Data table will select all the concepts in the table but since we have first give
    // an option to user to select all concepts in a page
    // we will just add the the concepts in the page to selected concept list
    if (selectedConcepts.length === this.props.concepts.length && origin === 'table') {
      const startIndex = this.state.tableRef.current.state.first;
      const endIndex = startIndex + ROWS_TO_DISPLAY;
      selectedConcepts = fp.uniqBy( 'conceptId', this.state.selectedConcepts
          .concat(this.props.concepts.slice(startIndex, endIndex)));
      this.setState({showBanner: true});
    } else if (selectedConcepts.length === 0 ) {
      // if no concepts are selected remove the banner
      this.setState({showBanner: false});
    }
    this.setState({selectedConcepts: selectedConcepts});
    this.props.onSelectConcepts(selectedConcepts);
  }

  distinctVocabulary() {
    const vocabularyIds = this.props.concepts.map(concept => concept.vocabularyId);
    return fp.uniq(vocabularyIds);
  }

  componentWillReceiveProps(nextProps) {
    if ((nextProps.concepts !==  this.props.concepts)) {
      if (nextProps.concepts !== this.props.concepts && nextProps.concepts.length > 0 ) {
        this.setState({totalRecords: nextProps.concepts.length});
      }
    }
    if (nextProps.reactKey !== this.props.reactKey) {
      this.setState({showBanner: false});
    }
  }

  rowExpansionTemplate(data) {
    return (<SynonymsObject>
      {this.highlightWithSearchTerm(fp.uniq(data.conceptSynonyms).join(', '))}
    </SynonymsObject>);
  }

  highlightWithSearchTerm(stringToHighlight: string) {
    const {searchTerm} = this.props;
    if (!searchTerm || searchTerm.trim() === '') {
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
    return words.map((word, w) => <span key={w}
      style={matchString.test(word.toLowerCase()) ? styles.highlighted : {}}>
        {word}
      </span>);
  }

  selectAll() {
    this.setState({selectAll: !this.state.selectAll});
    const selectedConcept = this.state.selectAll ? [] : this.props.concepts;
    this.updateSelectedConceptList(selectedConcept, 'link');
  }

  selectAllHeader() {
    if (this.state.showBanner) {
      const {concepts} = this.props;
      const bannerText = this.state.selectAll ? 'Clear Selection' : 'Select ' + concepts.length;
      return <FlexRow  data-test-id='selection' style={{fontWeight: '200'}}>
        All concepts on this page are selected.&nbsp;
        <Clickable data-test-id='banner-link' style={{color: 'blue'}} onClick={() => this.selectAll()}>
          {bannerText}
        </Clickable>
      </FlexRow>;
    }
    return;
  }

  onPageChange() {
    this.setState({showBanner: false});
  }

  render() {
    const {selectedConcepts, tableRef} = this.state;
    const {placeholderValue, loading, reactKey} = this.props;
    return <div data-test-id='conceptTable' key={reactKey} style={{position: 'relative', minHeight: '10rem'}}>
      {loading ? <SpinnerOverlay /> : <DataTable ref={tableRef} emptyMessage={loading ? '' : placeholderValue}
                 header={this.selectAllHeader()} value={this.props.concepts} scrollable={true}
                 selection={selectedConcepts} style={{minWidth: 1100}}
                 totalRecords={this.state.totalRecords}
                 expandedRows={this.props.concepts
                   .filter(concept => concept.conceptSynonyms.length > 0)}
                 rowExpansionTemplate={(data) => this.rowExpansionTemplate(data)}
                 paginator={true} rows={ROWS_TO_DISPLAY}
                 data-test-id='conceptRow'
                 onValueChange={(value) => this.onPageChange()}
                 onSelectionChange={e => this.updateSelectedConceptList(e.value, 'table')} >
      <Column bodyStyle={{...styles.colStyle, width: '3rem'}} headerStyle = {{width: '3rem'}}
              data-test-id='conceptCheckBox' selectionMode='multiple'/>
      <Column bodyStyle={styles.colStyle} field='conceptName' header='Name'
              data-test-id='conceptName'/>
      <Column bodyStyle={styles.colStyle} field='conceptCode' header='Code'/>
      <Column field='vocabularyId' header='Vocabulary' bodyStyle={styles.colStyle} />
      <Column style={styles.colStyle} field='countValue' header='Count'/>
    </DataTable>}
    </div>;
  }
}

