import {Clickable, Link} from 'app/components/buttons';
import {FlexRow} from 'app/components/flex';
import {ClrIcon} from 'app/components/icons';
import {TooltipTrigger} from 'app/components/popups';
import {SpinnerOverlay} from 'app/components/spinners';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {reactStyles} from 'app/utils';
import {Domain} from 'generated/fetch';
import * as fp from 'lodash/fp';
import {Column} from 'primereact/column';
import {DataTable} from 'primereact/datatable';
import * as React from 'react';

function formatCounts(concept: any) {
  if (concept.countValue) {
    concept.countValue = concept.countValue.toLocaleString();
  }
  return concept;
}

const styles = reactStyles({
  datatable: {
    border: `1px solid ${colorWithWhiteness(colors.black, 0.8)}`,
    borderBottomLeftRadius: '3px',
    borderBottomRightRadius: '3px',
    marginBottom: '1rem'
  },
  headerStyle: {
    color: colors.primary,
    textAlign: 'left',
    border: 0,
  },
  colStyle: {
    color: colors.primary,
    lineHeight: '0.5rem',
    border: 0,
    borderTop: `1px solid ${colorWithWhiteness(colors.black, 0.8)}`
  },
  akaText: {
    minWidth: '150px',
    maxWidth: '150px',
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
  },
  error: {
    background: colors.warning,
    color: colors.white,
    fontSize: '12px',
    fontWeight: 500,
    textAlign: 'left',
    border: '1px solid #ebafa6',
    borderRadius: '5px',
    marginTop: '0.25rem',
    padding: '8px',
  },
});
const domainColumns = [
  {
    bodyStyle: styles.colStyle,
    className: null,
    field: 'conceptName',
    header: 'Name',
    headerStyle: styles.headerStyle,
    selectionMode: null,
    testId: 'conceptName'
  },
  {
    bodyStyle: styles.colStyle,
    className: 'divider',
    field: 'conceptCode',
    header: 'Code',
    headerStyle: styles.headerStyle,
    selectionMode: null,
    testId: 'conceptCode'
  },
  {
    bodyStyle: styles.colStyle,
    className: 'divider',
    field: 'vocabularyId',
    header: 'Vocabulary',
    headerStyle: styles.headerStyle,
    selectionMode: null,
    testId: null
  },
  {
    bodyStyle: styles.colStyle,
    className: 'divider',
    field: 'countValue',
    header: 'Participant Count',
    headerStyle: styles.headerStyle,
    selectionMode: null,
    testId: null
  }
];

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
    return <div style={{display: 'flex', paddingLeft: '2rem'}}>
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
        width: `calc(100% - ${willOverflow ? '250' : '180'}px)`,
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
  concepts: any[];
  domain: Domain;
  loading: boolean;
  onSelectConcepts: Function;
  placeholderValue: string;
  reactKey: string;
  searchTerm?: string;
  selectedConcepts: any[];
  error: boolean;
}

interface State {
  first: number;
  pageLoading: boolean;
  selectedConcepts: any[];
  showBanner: boolean;
  selectAll: boolean;
  totalRecords: number;
  pageConcepts: any[];
  tableRef: any;
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
      pageConcepts: props.concepts.slice(0, 10).map(formatCounts),
      tableRef: React.createRef(),
    };
  }

  componentDidUpdate(prevProps) {
    console.log(this.props.concepts);
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
    } else if (selectedConcepts.length < this.props.concepts.length) {
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
    const {concepts} = this.props;
    const {selectAll, showBanner} = this.state;
    if (showBanner && concepts.length > ROWS_TO_DISPLAY) {
      const selectedConceptSize = selectAll ? concepts.length : ROWS_TO_DISPLAY;
      const text = selectAll ? '.' : 'on this page.';
      const clickableText = selectAll ? 'Clear Selection' :
          'Select all ' + concepts.length + ' concepts';
      return <div data-test-id='selection'><FlexRow style={{fontWeight: '200'}}>
        You’ve selected all {selectedConceptSize} concepts {text} &nbsp;
        <Clickable data-test-id='banner-link' style={{color: 'blue'}} onClick={() => this.selectAll()}>
          {clickableText}
        </Clickable>
        {!selectAll && <div> &nbsp;in this domain </div>}
      </FlexRow></div>;
    }
    return;
  }

  onPageChange() {
    this.setState({showBanner: false});
  }

  errorMessage() {
    return !this.props.error ? false : <div style={styles.error}>
      <ClrIcon style={{margin: '0 0.5rem 0 0.25rem'}} className='is-solid' shape='exclamation-triangle' size='22'/>
      Sorry, the request cannot be completed. Please try refreshing the page or contact Support in the left hand navigation.
    </div>;
  }

  renderColumns() {
    const {concepts, domain} = this.props;
    const surveyColumns = [
      {
        bodyStyle: styles.colStyle,
        className: null,
        field: concepts.length && !!concepts[0].question ? 'question' : 'conceptName',
        header: 'Question',
        headerStyle: styles.headerStyle,
        selectionMode: null,
        testId: 'question'
      },
      {
        bodyStyle: styles.colStyle,
        className: 'divider',
        field: 'countValue',
        header: 'Participant Count',
        headerStyle: {...styles.headerStyle, width: '20%'},
        selectionMode: null,
        testId: null
      }
    ];
    const columns = [
      {
        bodyStyle: {...styles.colStyle, textAlign: 'center'},
        className: null,
        field: null,
        header: null,
        headerStyle: {...styles.headerStyle, textAlign: 'center', width: '2rem'},
        selectionMode: 'multiple',
        testId: 'conceptCheckBox'
      },
      ...(domain === Domain.SURVEY ? surveyColumns : domainColumns)
    ];
    return columns.map((col, c) => <Column
      bodyStyle={col.bodyStyle}
      className={col.className}
      field={col.field}
      header={col.header}
      headerStyle={col.headerStyle}
      key={c}
      selectionMode={col.selectionMode}
      data-test-id={col.testId}
    />);
  }

  render() {
    const {selectedConcepts, tableRef} = this.state;
    const {concepts, error, placeholderValue, loading, reactKey} = this.props;
    return <div data-test-id='conceptTable' key={reactKey} style={{position: 'relative', minHeight: '10rem'}}>
      <style>
        {`
          body .p-datatable .p-datatable-tbody > tr:nth-child(even),
          body .p-datatable .p-datatable-tbody > tr:nth-child(even).p-highlight,
          body .p-datatable .p-datatable-tbody > .p-datatable-row.p-highlight {
            background: ${colors.white};
          }
          body .p-datatable .p-datatable-tbody > tr:not(.p-datatable-row) > td {
            border: 0;
          }
          body .p-datatable > .p-paginator {
            background: ${colors.white};
            border: 0;
            color: ${colors.primary};
          }
          body .p-datatable .p-datatable-footer {
            background: ${colors.white};
            border: 0;
          }
        `}
      </style>
      {loading ? <SpinnerOverlay /> : <DataTable ref={tableRef} emptyMessage={loading || error ? '' : placeholderValue}
                                                 style={styles.datatable}
                                                 header={this.selectAllHeader()}
                                                 value={error ? null : concepts.map(formatCounts)}
                                                 scrollable={true}
                                                 selection={selectedConcepts}
                                                 totalRecords={this.state.totalRecords}
                                                 expandedRows={
                                                   concepts.filter(concept => concept.conceptSynonyms && concept.conceptSynonyms.length > 0)
                                                 }
                                                 rowExpansionTemplate={(data) => this.rowExpansionTemplate(data)}
                                                 alwaysShowPaginator={false}
                                                 paginator={true} rows={ROWS_TO_DISPLAY}
                                                 data-test-id='conceptRow'
                                                 onValueChange={(value) => this.onPageChange()}
                                                 onSelectionChange={e => this.updateSelectedConceptList(e.value, 'table')}
                                                 footer={this.errorMessage()}>
        {this.renderColumns()}
      </DataTable>}
    </div>;
  }
}

