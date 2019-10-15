import {domainToTitle} from 'app/cohort-search/utils';
import {ClrIcon} from 'app/components/icons';
import {TextInput} from 'app/components/inputs';
import {SpinnerOverlay} from 'app/components/spinners';
import {ReviewDomainChartsComponent} from 'app/pages/data/cohort-review/review-domain-charts';
import {cohortReviewStore, vocabOptions} from 'app/services/review-state.service';
import {cohortReviewApi} from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import {datatableStyles} from 'app/styles/datatable';
import {reactStyles, withCurrentWorkspace} from 'app/utils';
import {triggerEvent} from 'app/utils/analytics';
import {WorkspaceData} from 'app/utils/workspace-data';
import {DomainType, PageFilterRequest, SortOrder} from 'generated/fetch';
import * as fp from 'lodash/fp';
import * as moment from 'moment';
import {Column} from 'primereact/column';
import {DataTable} from 'primereact/datatable';
import {OverlayPanel} from 'primereact/overlaypanel';
import {TabPanel, TabView} from 'primereact/tabview';
import * as React from 'react';

const css = `
  .name-container {
    overflow: hidden;
    height: 1.2rem;
  }
  .name-container:before {
    content:"";
    float: left;
    width: 1px;
    height: 100%;
  }
`;

const styles = reactStyles({
  container: {
    position: 'relative',
    minHeight: '15rem'
  },
  table: {
    fontSize: '12px',
  },
  tableBody: {
    textAlign: 'left',
    lineHeight: '0.75rem'
  },
  columnHeader: {
    background: '#f4f4f4',
    color: colors.primary,
    fontWeight: 600,
  },
  columnBody: {
    background: colors.white,
    padding: '0.5rem 0.5rem 0.3rem',
    verticalAlign: 'top',
    textAlign: 'left',
    borderLeft: 0,
    borderRight: 0,
    borderBottom: 'none',
    lineHeight: '0.6rem',
  },
  graphColumnBody: {
    background: colors.white,
    padding: '5px',
    verticalAlign: 'top',
    textAlign: 'left',
    borderLeft: 0,
    borderRight: 0,
    lineHeight: '0.6rem',
    width: '2rem',
  },
  filterIcon: {
    marginLeft: '0.3rem',
    padding: '2px 2px 1px 1px',
    borderRadius: '50%',
    fontWeight: 600,
    float: 'right'
  },
  sortIcon: {
    marginTop: '4px',
    color: colors.accent,
    fontSize: '0.5rem',
    float: 'right'
  },
  overlayHeader: {
    padding: '0.3rem',
  },
  caretIcon: {
    fontSize: '0.6rem',
    paddingLeft: '0.4rem',
    color: colors.accent,
    cursor: 'pointer',
  },
  filterBorder: {
    paddingTop: '0.5rem',
    borderTop: '1px solid #ccc',
  },
  graphStyle: {
    borderLeft: 'none',
    width: '2rem',
  },
  headerStyle: {
    color: colors.primary,
    fontSize: '14px',
    fontWeight: 'bold',
    width: '20rem',
    textOverflow: 'ellipsis',
    overflow: 'hidden',
    whiteSpace: 'nowrap',
    margin: 'auto',
    paddingTop: '0.5rem',
    textAlign: 'center',
  },
  unitsLabel: {
    width: '22rem',
    margin: '0 0 -1.65rem 12.5rem',
    color: colors.accent,
  },
  textSearch: {
    width: '85%',
    borderRadius: '4px',
    backgroundColor: colors.light,
    marginLeft: '5px'
  },
  textInput: {
    width: '85%',
    padding: '0 0 0 5px',
    border: 0,
    backgroundColor: 'transparent',
    outline: 'none',
  },
  noResults: {
    paddingLeft: '0.5rem',
    lineHeight: '1.25rem',
    opacity: 0.5,
  },
  nameWrapper: {
    float: 'right',
    width: '100%',
    marginLeft: '-1px',
  },
  nameContent: {
    margin: 0,
    fontSize: '12px',
    lineHeight: '0.6rem',
  },
  showMore: {
    width: '70px',
    bottom: 0,
    right: 0,
    background: colors.white,
    color: colors.accent,
    boxSizing: 'content-box',
    position: 'absolute',
    paddingLeft: '10px',
    textAlign: 'left',
    cursor: 'pointer',
  },
  error: {
    width: '100%',
    background: colors.warning,
    color: colors.white,
    fontSize: '12px',
    fontWeight: 500,
    textAlign: 'left',
    borderRadius: '5px',
    marginTop: '0.25rem',
    padding: '8px',
  },
});
const filterIcons = {
  active: {
    ...styles.filterIcon,
    background: colors.success,
    color: colors.white,
  },
  default: {
    ...styles.filterIcon,
    color: colors.accent,
  }
};

const rows = 25;
const domains = [
  DomainType.CONDITION,
  DomainType.PROCEDURE,
  DomainType.DRUG,
  DomainType.OBSERVATION,
  DomainType.PHYSICALMEASUREMENT,
  DomainType.LAB,
  DomainType.VITAL,
  DomainType.SURVEY,
];

class NameContainer extends React.Component<{data: any, vocab: string}, {showMore: boolean}> {
  container: HTMLDivElement;
  constructor(props: any) {
    super(props);
    this.state = {showMore: false};
  }

  handleResize = fp.debounce(100, () => {
    this.checkContainerHeight();
  });

  componentDidMount(): void {
    this.checkContainerHeight();
    window.addEventListener('resize', this.handleResize);
  }

  componentWillUnmount(): void {
    window.removeEventListener('resize', this.handleResize);
  }

  checkContainerHeight() {
    const {offsetHeight, scrollHeight} = this.container;
    this.setState({showMore: scrollHeight > offsetHeight});
  }

  render() {
    const {data, vocab} = this.props;
    const {showMore} = this.state;
    let nl: any;
    return <div ref={(e) => this.container = e} style={{overflow: 'hidden', maxHeight: '1.2rem'}}>
      <div style={styles.nameWrapper}>
        <p style={styles.nameContent}>{data[`${vocab}Name`]}</p>
      </div>
      {showMore && <React.Fragment>
        <span style={styles.showMore} onClick={(e) => nl.toggle(e)}>Show more</span>
        <OverlayPanel className='labOverlay' ref={(el) => nl = el} showCloseIcon={true} dismissable={true}>
          <div style={{paddingBottom: '0.2rem'}}>{data[`${vocab}Name`]}</div>
        </OverlayPanel>
      </React.Fragment>}
    </div>;
  }
}

interface Props {
  tabName: string;
  columns: Array<any>;
  domain: DomainType;
  participantId: number;
  workspace: WorkspaceData;
  filterState: any;
  getFilteredData: Function;
  updateState: number;
}

interface State {
  data: Array<any>;
  filteredData: Array<any>;
  loading: boolean;
  start: number;
  sortField: string;
  sortOrder: number;
  expandedRows: Array<any>;
  codeResults: any;
  error: boolean;
}

export const DetailTabTable = withCurrentWorkspace()(
  class extends React.Component<Props, State> {
    codeInputChange: Function;
    constructor(props: Props) {
      super(props);
      this.state = {
        data: null,
        filteredData: null,
        loading: true,
        start: 0,
        sortField: null,
        sortOrder: 1,
        expandedRows: [],
        codeResults: null,
        error: false,
      };
      this.codeInputChange = fp.debounce(300, (e) => this.filterCodes(e));
    }

    componentDidMount() {
      this.getParticipantData();
    }

    componentDidUpdate(prevProps: any) {
      const {participantId, updateState} = this.props;
      if (prevProps.participantId !== participantId) {
        this.setState({
          data: null,
          filteredData: null,
          loading: true,
          error: false,
        });
        this.getParticipantData();
      } else if (prevProps.updateState !== updateState) {
        this.filterData();
      }
    }

    getParticipantData() {
      try {
        const {columns, domain, participantId,
          workspace: {id, namespace}} = this.props;
        const pageFilterRequest = {
          page: 0,
          pageSize: 10000,
          sortOrder: SortOrder.Asc,
          sortColumn: columns[0].name,
          domain: domain,
        } as PageFilterRequest;

        cohortReviewApi().getParticipantData(
          namespace,
          id,
          cohortReviewStore.getValue().cohortReviewId,
          participantId,
          pageFilterRequest
        ).then(response => {
          response.items.forEach(item => {
            if (domain === DomainType.VITAL || domain === DomainType.LAB) {
              item['itemTime'] = moment(item.itemDate, 'YYYY-MM-DD HH:mm Z').format('hh:mm a z');
            }
            item.itemDate = moment(item.itemDate).format('YYYY-MM-DD');
          });
          this.setState({
            data: response.items,
            loading: false,
          });
          this.filterData();
        });
      } catch (error) {
        console.log(error);
        this.setState({
          loading: false,
          error: true
        });
      }
    }

    onSort = (event: any) => {
      this.setState({sortField: event.sortField, sortOrder: event.sortOrder});
    }

    columnSort = (sortField: string) => {
      if (this.state.sortField === sortField) {
        const sortOrder = this.state.sortOrder === 1 ? -1 : 1;
        this.setState({sortOrder});
      } else {
        this.setState({sortField, sortOrder: 1});
      }
    }

    onPage = (event: any) => {
      this.setState({start: event.first});
    }

    overlayTemplate = (rowData: any, column: any) => {
      let vl: any;
      const {filterState: {vocab}} = this.props;
      const valueField = (rowData.refRange || rowData.unit) && column.field === 'value';
      const nameField = rowData.route && column.field === `${vocab}Name`;
      return <React.Fragment>
        <div style={{position: 'relative'}}>
          {column.field === 'value' && <span>{rowData.value}</span>}
          {column.field === `${vocab}Name` && <NameContainer data={rowData} vocab={vocab} />}
          {(valueField || nameField)
          && <i className='pi pi-caret-down' style={styles.caretIcon}
              onClick={(e) => vl.toggle(e)}/>}
          <OverlayPanel className='labOverlay' ref={(el) => vl = el}
                        showCloseIcon={true} dismissable={true}>
            {(rowData.refRange &&  column.field === 'value') &&
            <div style={{paddingBottom: '0.2rem'}}>Reference Range: {rowData.refRange}</div>}
            {(rowData.unit && column.field === 'value') &&
            <div>Units: {rowData.unit}</div>}
            {nameField &&
            <div>Route: {rowData.route}</div>}
          </OverlayPanel>
        </div>
      </React.Fragment>;
    }

    updateData = (event, colName, namesArray) => {
      const {checked, name} = event.target;
      const {domain, filterState, getFilteredData} = this.props;
      let checkedItems = filterState.tabs[domain][colName];
      if (checked) {
        if (name === 'Select All') {
          checkedItems = namesArray.map(opt => opt.name);
        } else {
          checkedItems.push(name);
          if (namesArray.length - 1 === checkedItems.length) {
            // we have to add selectall when everything is selected
            checkedItems.push('Select All');
          }
        }
      } else {
        if (name === 'Select All') {
          checkedItems = [];
        } else {
          if (checkedItems.find(s => s === 'Select All')) {
            checkedItems.splice(checkedItems.indexOf('Select All'), 1);
          }
          checkedItems.splice(checkedItems.indexOf(name), 1);
        }
      }
      filterState.tabs[domain][colName] = checkedItems;
      getFilteredData(filterState);
    }

    filterData() {
      let {data, start} = this.state;
      const {
        domain,
        filterState,
        filterState: {global: {ageMin, ageMax, dateMin, dateMax, visits}, vocab}
      } = this.props;
      /* Global filters */
      if (dateMin || dateMax) {
        const min = dateMin ? Date.parse(dateMin) : 0;
        const max = dateMax ? Date.parse(dateMax) : 9999999999999;
        data = data.filter(item => {
          const itemDate = Date.parse(item.itemDate);
          return itemDate >= min && itemDate <= max;
        });
      }
      if (domain !== DomainType.SURVEY && (ageMin || ageMax)) {
        const min = ageMin || 0;
        const max = ageMax || 120;
        data = data.filter(item => item.ageAtEvent >= min && item.ageAtEvent <= max);
      }
      if (domain !== DomainType.SURVEY
        && domain !== DomainType.PHYSICALMEASUREMENT
        && visits) {
        data = data.filter(item => visits === item.visitType);
      }
      /* Column filters */
      const columnCheck = [
        'domain',
        `${vocab}Vocabulary`,
        `${vocab}Code`,
        `${vocab}Name`,
        'value',
        'numMentions',
        'firstMention',
        'lastMention',
        'itemTime',
        'survey'
      ];
      const columnFilters = filterState.tabs[domain];
      if (!columnFilters) {
        if (data.length < start + rows) {
          start = Math.floor(data.length / rows) * rows;
        }
        this.setState({filteredData: data, start: start});
      } else {
        for (const col in columnFilters) {
          // Makes sure we only filter by correct concept type (standard/source)
          if (columnCheck.includes(col)) {
            if (Array.isArray(columnFilters[col])) {
              // checkbox filters
              if (!columnFilters[col].length) {
                data = [];
                break;
              } else if (!columnFilters[col].includes('Select All')
                && !(vocab === 'source' && domain === DomainType.OBSERVATION)) {
                data = data.filter(row => columnFilters[col].includes(row[col]));
              }
            } else {
              // text filters
              if (columnFilters[col]) {
                data = data.filter(row =>
                  row[col] && row[col].toLowerCase().includes(columnFilters[col].toLowerCase()));
              }
            }
          }
        }
        if (data && data.length < start + rows) {
          start = Math.floor(data.length / rows) * rows;
        }
        this.setState({filteredData: data, start: start});
      }
    }

    errorMessage = () => {
      const {tabName} = this.props;
      const {data, filteredData, error} = this.state;
      if ((filteredData && filteredData.length) || (!data && !error)) {
        return false;
      }
      let message: string;
      if (data && data.length === 0) {
        message = 'No ' + tabName + ' data found';
      } else if (data && data.length > 0 && filteredData && filteredData.length === 0) {
        message = 'Data cannot be found. Please review your filters and try again.';
      } else if (error) {
        message = `Sorry, the request cannot be completed. Please try refreshing the page or
           contact Support in the left hand navigation.`;
      }
      return <div style={styles.error}>
        <ClrIcon style={{margin: '0 0.5rem 0 0.25rem'}} className='is-solid'
        shape='exclamation-triangle' size='22'/>
        {message}
      </div>;
    }

    filterCodes = (input: string) => {
      if (!input) {
        this.setState({codeResults: null});
      } else {
        const {data} = this.state;
        const {filterState: {vocab}} = this.props;
        const codeType = `${vocab}Code`;
        const codeResults = data.reduce((acc, item) => {
          if (item[codeType].toLowerCase().includes(input.toLowerCase())) {
            acc.add(item[codeType]);
          }
          return acc;
        }, new Set());
        this.setState({codeResults});
      }
    }

    filterText = (input: string, column: string) => {
      const {domain, filterState, getFilteredData} = this.props;
      filterState.tabs[domain][column] = input;
      getFilteredData(filterState);
    }

    filterEvent(column: string) {
      const {columns, domain} = this.props;
      const {displayName} = columns.find(col => col.name === column);
      triggerEvent('Review Individual', 'Click', `${domainToTitle(domain)} - Filter - ${displayName} - Review Individual`);
    }

    checkboxFilter(column: string) {
      const {codeResults, data} = this.state;
      const {domain, filterState, filterState: {vocab}} = this.props;
      const columnFilters = filterState.tabs[domain];
      if (!data) {
        return '';
      }
      const counts = {total: 0};
      let options: Array<any>;
      data.forEach(item => {
        counts[item[column]] = !!counts[item[column]] ? counts[item[column]] + 1 : 1;
        counts.total++;
      });
      switch (column) {
        case 'domain':
          options = domains.map(name => {
            return {name, count: counts[name] || 0};
          });
          options.sort((a, b) => b.count - a.count);
          break;
        case `${vocab}Code`:
          options = Object.keys(counts).reduce((acc, name) => {
            if (name !== 'total') {
              acc.push({name, count: counts[name]});
            }
            return acc;
          }, []);
          options.sort((a, b) => b.count - a.count);
          if (!columnFilters[column].includes('Select All')) {
            columnFilters[column].forEach(name => {
              if (!options.find(opt => opt.name === name)) {
                options = [{name, count: 0}, ...options];
              }
            });
          }
          break;
        case `${vocab}Vocabulary`:
          const vocabs = vocabOptions.getValue()[vocab];
          options = vocabs[domain] ? vocabs[domain].map(name => {
            return {name, count: counts[name] || 0};
          }) : [];
          options.sort((a, b) => b.count - a.count);
          break;
      }
      options.push({name: 'Select All', count: counts.total});
      if (columnFilters[column].includes('Select All')) {
        columnFilters[column] = options.map(opt => opt.name);
      }
      const checkboxes = codeResults && codeResults.size === 0
        ? <em style={styles.noResults}>No matching codes</em>
        : options.reduce((acc, opt, i) => {
          if (!codeResults || codeResults.has(opt.name)) {
            acc.push(<React.Fragment key={i}>
              {opt.name !== 'Select All' && <div style={{padding: '0.3rem 0.4rem'}}>
                <input style={{width: '0.7rem', height: '0.7rem'}} type='checkbox' name={opt.name}
                       checked={columnFilters[column].includes(opt.name)}
                       onChange={($event) => this.updateData($event, column, options)}/>
                <label style={{paddingLeft: '0.4rem'}}> {opt.name} ({opt.count}) </label>
              </div>}
            </React.Fragment>);
          }
          return acc;
        }, []);
      const filtered = !columnFilters[column].includes('Select All');
      let fl: any;
      return <span>
        <i className='pi pi-filter'
           style={filtered ? filterIcons.active : filterIcons.default}
           onClick={(e) => {
             this.filterEvent(column);
             fl.toggle(e);
           }}/>
        <OverlayPanel style={{left: '359.531px!important'}} className='filterOverlay'
                      ref={(el) => fl = el} showCloseIcon={true} dismissable={true}>
          {column === `${vocab}Code` && <div style={styles.textSearch}>
            <i className='pi pi-search' style={{margin: '0 5px'}} />
            <TextInput
              style={styles.textInput}
              onChange={this.codeInputChange}
              placeholder={'Search'}/>
          </div>}
          <div style={{maxHeight: 'calc(100vh - 450px)', overflow: 'auto'}}>
            {checkboxes}
          </div>
          <div style={{borderTop: '1px solid #ccc', padding: '0.5rem 0.5rem'}}>
            <input style={{width: '0.7rem',  height: '0.7rem'}} type='checkbox' name='Select All'
                   checked={columnFilters[column].includes('Select All')}
                   onChange={($event) => this.updateData($event, column, options)}/>
            <label style={{paddingLeft: '0.4rem'}}> Select All ({counts.total}) </label>
          </div>
        </OverlayPanel>
      </span>;
    }

    textFilter(column: string) {
      const {domain, filterState} = this.props;
      const columnFilters = filterState.tabs[domain];
      const filtered = !!columnFilters[column];
      let fl: any, ip: any;
      return <span>
        <i className='pi pi-filter'
          style={filtered ? filterIcons.active : filterIcons.default}
          onClick={(e) => {
            this.filterEvent(column);
            fl.toggle(e);
            ip.focus();
          }}/>
        <OverlayPanel style={{left: '359.531px!important'}} className='filterOverlay'
                      ref={(el) => fl = el} showCloseIcon={true} dismissable={true}>
          <div style={styles.textSearch}>
            <i className='pi pi-search' style={{margin: '0 5px'}} />
            <TextInput
              ref={(i) => ip = i}
              style={styles.textInput}
              value={columnFilters[column]}
              onChange={(e) => this.filterText(e, column)}
              placeholder={'Search'} />
          </div>
        </OverlayPanel>
      </span>;
    }

    rowExpansionTemplate = (rowData: any) => {
      const {data} = this.state;
      const {filterState: {vocab}} = this.props;
      const conceptIdBasedData = fp.groupBy('standardConceptId', data);
      const unitsObj = fp.groupBy( 'unit', conceptIdBasedData[rowData.standardConceptId]);
      const unitKey = Object.keys(unitsObj);
      let valueArray;
      return <React.Fragment>
        <div style={styles.headerStyle}>{rowData[`${vocab}Name`]}</div>
        <div style={styles.unitsLabel}>Units:</div>
        <TabView className='unitTab'>
          {unitKey.map((k, i) => {
            const name = (k === 'null' ? 'No Unit' : k);
            { valueArray = unitsObj[k].map(v => {
              return {
                values: parseInt(v.value, 10),
                date: v.itemDate,
              };
            }); }
            return <TabPanel header={name} key={i}>
              <ReviewDomainChartsComponent unitData={valueArray} />
            </TabPanel>;
          })}
        </TabView>
      </React.Fragment>;
    }

    hideGraphIcon = (rowData: any) => {
      const {filterState: {vocab}} = this.props;
      const noConcept = rowData[`${vocab}Name`]
        && rowData[`${vocab}Name`] === 'No matching concept';
      return {'graphExpander' : noConcept};
    }

    render() {
      const {loading, start, sortField, sortOrder} = this.state;
      const {columns, filterState: {vocab}, tabName} = this.props;
      const filteredData = loading ? null : this.state.filteredData;
      let pageReportTemplate;
      if (filteredData !== null) {
        const lastRowOfPage = (start + rows) > filteredData.length
          ? start + rows - (start + rows - filteredData.length) : start + rows;
        pageReportTemplate = `${start + 1} - ${lastRowOfPage} of ${filteredData.length} records `;
      }
      let paginatorTemplate = 'CurrentPageReport';
      if (filteredData && filteredData.length > rows) {
        paginatorTemplate += ' PrevPageLink PageLinks NextPageLink';
      }
      const cols = columns.map((col) => {
        const asc = sortField === col.name && sortOrder === 1;
        const desc = sortField === col.name && sortOrder === -1;
        const colName = col.name === 'value' || col.name === `${vocab}Name`;
        const hasCheckboxFilter = [
          'domain',
          'sourceVocabulary',
          'standardVocabulary',
          'sourceCode',
          'standardCode'
        ].includes(col.name);
        const hasTextFilter = [
          'sourceName',
          'standardName',
          'value',
          'numMentions',
          'firstMention',
          'lastMention',
          'itemTime',
          'survey'
        ].includes(col.name);
        const isExpanderNeeded = col.name === 'graph' &&
          (tabName === 'Vitals' || tabName === 'Labs');
        const overlayTemplate = colName && this.overlayTemplate;
        const header = <React.Fragment>
          <span
            onClick={() => this.columnSort(col.name)}
            style={styles.columnHeader}>
            {col.displayName}
          </span>
          {hasCheckboxFilter && this.checkboxFilter(col.name)}
          {hasTextFilter && this.textFilter(col.name)}
          {(asc && !isExpanderNeeded) && <i className='pi pi-arrow-up' style={styles.sortIcon}
            onClick={() => this.columnSort(col.name)} />}
          {(desc && !isExpanderNeeded) && <i className='pi pi-arrow-down' style={styles.sortIcon}
            onClick={() => this.columnSort(col.name)} />}
        </React.Fragment>;
        return <Column
          expander={isExpanderNeeded}
          style={styles.tableBody}
          bodyStyle={isExpanderNeeded ? styles.graphColumnBody : styles.columnBody}
          key={col.name}
          field={col.name}
          header={header}
          headerStyle={isExpanderNeeded ? styles.graphStyle : {}}
          sortable
          body={overlayTemplate}/>;
      });
      return <div style={styles.container}>
        <style>{datatableStyles + css}</style>
        <DataTable
          expandedRows={this.state.expandedRows}
          onRowToggle={(e) => this.setState({expandedRows: e.data})}
          rowExpansionTemplate={this.rowExpansionTemplate}
          rowClassName = {this.hideGraphIcon}
          style={styles.table}
          value={filteredData}
          sortField={sortField}
          sortOrder={sortOrder}
          onSort={this.onSort}
          paginator
          alwaysShowPaginator={false}
          paginatorTemplate={filteredData && filteredData.length ? paginatorTemplate : ''}
          currentPageReportTemplate={filteredData && filteredData.length ? pageReportTemplate : ''}
          onPage={this.onPage}
          first={start}
          rows={rows}
          totalRecords={filteredData && filteredData.length}
          scrollable
          scrollHeight='calc(100vh - 350px)'
          autoLayout
          footer={this.errorMessage()}>
          {cols}
        </DataTable>
        {loading && <SpinnerOverlay />}
      </div>;
    }
  }
);
