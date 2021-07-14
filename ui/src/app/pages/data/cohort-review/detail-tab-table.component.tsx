import {domainToTitle} from 'app/cohort-search/utils';
import {ClrIcon} from 'app/components/icons';
import {TextInput} from 'app/components/inputs';
import {SpinnerOverlay} from 'app/components/spinners';
import {ReviewDomainChartsComponent} from 'app/pages/data/cohort-review/review-domain-charts';
import {vocabOptions} from 'app/services/review-state.service';
import {cohortReviewApi} from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import {datatableStyles} from 'app/styles/datatable';
import {reactStyles, withCurrentCohortReview, withCurrentWorkspace} from 'app/utils';
import {triggerEvent} from 'app/utils/analytics';
import {WorkspaceData} from 'app/utils/workspace-data';
import {CohortReview, Domain, Operator, PageFilterRequest, SortOrder} from 'generated/fetch';
import * as fp from 'lodash/fp';
import * as moment from 'moment';
import {Column} from 'primereact/column';
import {DataTable} from 'primereact/datatable';
import {OverlayPanel} from 'primereact/overlaypanel';
import {TabPanel, TabView} from 'primereact/tabview';
import * as React from 'react';
import {Key} from 'ts-key-enum';

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
    display: 'inline-block',
    background: '#f4f4f4',
    color: colors.primary,
    fontWeight: 600,
    maxWidth: '80%'
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
    marginTop: '2px',
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
    width: '95%',
    borderRadius: '4px',
    backgroundColor: colors.light,
    marginLeft: '5px'
  },
  textInput: {
    width: '75%',
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

const rowsPerPage = 25;
const lazyLoadSize = 125;
const domains = [
  Domain.CONDITION,
  Domain.PROCEDURE,
  Domain.DRUG,
  Domain.OBSERVATION,
  Domain.PHYSICALMEASUREMENT,
  Domain.LAB,
  Domain.VITAL,
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
  cohortReview: CohortReview;
  columns: Array<any>;
  domain: Domain;
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
  updating: boolean;
  loadingPrevious: boolean;
  page: number;
  start: number;
  sortField: string;
  sortOrder: number;
  expandedRows: Array<any>;
  codeResults: any;
  error: boolean;
  lazyLoad: boolean;
  totalCount: number;
  requestPage: number;
  range: Array<number>;
  tabFilterState: any;
}

export const DetailTabTable = fp.flow(withCurrentCohortReview(), withCurrentWorkspace())(
  class extends React.Component<Props, State> {
    codeInputChange: Function;
    private countAborter = new AbortController();
    private dataAborter = new AbortController();
    constructor(props: Props) {
      super(props);
      this.state = {
        data: null,
        filteredData: null,
        loading: true,
        updating: false,
        loadingPrevious: false,
        page: 0,
        start: 0,
        sortField: props.columns[0].name,
        sortOrder: 1,
        expandedRows: [],
        codeResults: null,
        error: false,
        lazyLoad: false,
        totalCount: null,
        requestPage: 0,
        range: [0, 124],
        tabFilterState: JSON.parse(JSON.stringify(props.filterState.tabs[props.domain]))
      };
      this.codeInputChange = fp.debounce(300, (e) => this.filterCodes(e));
    }

    componentDidMount() {
      this.getParticipantData(true);
    }

    componentDidUpdate(prevProps: any) {
      const {domain, filterState, participantId, updateState} = this.props;
      const {lazyLoad, loading} = this.state;
      if (prevProps.participantId !== participantId) {
        if (loading) {
          // cancel any pending count or data calls
          this.abortPendingApiCalls(true);
        }
        this.setState({
          data: null,
          filteredData: null,
          lazyLoad: false,
          loading: true,
          error: false,
          page: 0,
          start: 0,
        }, () => this.getParticipantData(true));
      } else if (prevProps.updateState !== updateState) {
        const tabFilterState = JSON.parse(JSON.stringify(filterState.tabs[domain]));
        if (lazyLoad) {
          if (loading) {
            // cancel any pending count or data calls
            this.abortPendingApiCalls(true);
          }
          this.setState({
            data: null,
            filteredData: null,
            loading: true,
            error: false,
            tabFilterState
          }, () => this.getParticipantData(true));
        } else {
          this.setState({tabFilterState}, () => this.filterData());
        }
      }
    }

    componentWillUnmount(): void {
      this.abortPendingApiCalls();
    }

    async getParticipantData(getCount: boolean) {
      try {
        const {columns, domain} = this.props;
        const {range, sortField, sortOrder} = this.state;
        let {lazyLoad, page, start, totalCount} = this.state;
        const filters = this.getFilters();
        if (filters !== null) {
          const pageFilterRequest = {
            page: Math.floor(page / (lazyLoadSize / rowsPerPage)),
            pageSize: lazyLoadSize,
            sortOrder: sortOrder === 1 ? SortOrder.Asc : SortOrder.Desc,
            sortColumn: columns.find(col => col.name === sortField).filter,
            domain,
            filters: lazyLoad ? filters : {items: []}
          } as PageFilterRequest;
          if (getCount) {
            // call api for count with no filters to get total count
            await this.callCountApi(pageFilterRequest).then(async(count) => {
              totalCount = count;
              if (lazyLoad) {
                if (start > totalCount) {
                  // If the data was filtered to a smaller count than the previous start, reset to the last page of the new data
                  start = Math.floor((totalCount - 1) / rowsPerPage) * rowsPerPage;
                  page = start / rowsPerPage;
                  pageFilterRequest.page = Math.floor(totalCount / lazyLoadSize);
                }
              } else {
                lazyLoad = totalCount > 1000;
                if (lazyLoad) {
                  pageFilterRequest.filters = filters;
                  pageFilterRequest.pageSize = lazyLoadSize;
                  if (filters.items.length) {
                    // if filters exist, call api for count a second time to get the filtered count
                    await this.callCountApi(pageFilterRequest).then(filteredCount => {
                      totalCount = filteredCount;
                      if (start > totalCount) {
                        // If the data was filtered to a smaller count than the previous start, reset to the last page of the new data
                        start = Math.floor(totalCount / rowsPerPage) * rowsPerPage;
                        pageFilterRequest.page = Math.floor(totalCount / lazyLoadSize);
                      }
                    });
                  }
                } else {
                  pageFilterRequest.pageSize = totalCount;
                }
              }
            });
          }
          this.callDataApi(pageFilterRequest).then(data => {
            if (lazyLoad) {
              const end = Math.min(range[0] + lazyLoadSize, totalCount);
              this.setState({data, filteredData: data, loading: false, lazyLoad, page, range: [range[0], end], start, totalCount});
            } else {
              this.setState({data, loading: false, lazyLoad, range: [0, totalCount - 1]});
              this.filterData();
            }
          });
        }
      } catch (error) {
        // Ignore abort errors since those were intentional
        if (error.name !== 'AbortError') {
          console.error(error);
          this.setState({loading: false, error: true});
        }
      }
    }

    updatePageData(previous: boolean) {
      try {
        this.setState({loadingPrevious: previous, updating: true, error: false});
        const {columns, domain} = this.props;
        const {range, sortField, sortOrder} = this.state;
        let {filteredData} = this.state;
        const requestPage = previous ? range[0] / lazyLoadSize : (filteredData.length + range[0]) / lazyLoadSize;
        const filters = this.getFilters();
        if (filters !== null) {
          const pageFilterRequest = {
            page: requestPage,
            pageSize: lazyLoadSize,
            sortOrder: sortOrder === 1 ? SortOrder.Asc : SortOrder.Desc,
            sortColumn: columns.find(col => col.name === sortField).filter,
            domain: domain,
            filters
          } as PageFilterRequest;
          this.callDataApi(pageFilterRequest).then(data => {
            if (previous) {
              filteredData = [...data, ...filteredData];
              if (filteredData.length > (lazyLoadSize * 3)) {
                filteredData = filteredData.slice(0, filteredData.length - lazyLoadSize);
                range[1] -= lazyLoadSize;
              }
            } else {
              filteredData = [...filteredData, ...data];
              if (filteredData.length > (lazyLoadSize * 3)) {
                filteredData = filteredData.slice(lazyLoadSize);
                range[0] += lazyLoadSize;
              }
            }
            this.setState({data, filteredData, range, loadingPrevious: false, updating: false});
          });
        }
      } catch (error) {
        console.error(error);
        this.setState({loadingPrevious: false, updating: false, error: true});
      }
    }

    async callDataApi(request: PageFilterRequest) {
      const {cohortReview: {cohortReviewId}, domain, participantId, workspace: {id, namespace}} = this.props;
      let data = [];
      await cohortReviewApi()
        .getParticipantData(namespace, id, cohortReviewId, participantId, request, {signal: this.dataAborter.signal})
        .then(response => {
          data = response.items.map(item => {
            if (domain === Domain.VITAL || domain === Domain.LAB) {
              item['itemTime'] = moment(item.itemDate, 'YYYY-MM-DD HH:mm Z').format('hh:mm a z');
            }
            item.itemDate = moment(item.itemDate, 'YYYY-MM-DD HH:mm Z').format('YYYY-MM-DD');
            return item;
          });
        });
      return data;
    }

    async callCountApi(request: PageFilterRequest) {
      const {cohortReview: {cohortReviewId}, participantId, workspace: {id, namespace}} = this.props;
      let count = null;
      await cohortReviewApi()
        .getParticipantCount(namespace, id, cohortReviewId, participantId, request, {signal: this.countAborter.signal})
        .then(response => {
          count = response.count;
        });
      return count;
    }

    abortPendingApiCalls(reset?: boolean) {
      this.countAborter.abort();
      this.dataAborter.abort();
      if (reset) {
        this.countAborter = new AbortController();
        this.dataAborter = new AbortController();
      }
    }

    getFilters() {
      const {columns, domain, filterState} = this.props;
      const filters = {items: []};
      const columnFilters = filterState.tabs[domain];
      if (!!columnFilters) {
        for (const col in columnFilters) {
          if (columnFilters.hasOwnProperty(col)) {
            const filter = columnFilters[col];
            if (Array.isArray(filter)) {
              // checkbox filters
              if (!filter.length) {
                // No filters checked so clear the data, don't call api
                this.setState({data: [], filteredData: null, loading: false});
                return null;
              } else if (!filter.includes('Select All')) {
                filters.items.push({
                  property: columns.find(c => c.name === col).filter,
                  operator: Operator.IN,
                  values: filter
                });
              }
            } else {
              // text filters
              if (!!filter) {
                filters.items.push({
                  property: columns.find(c => c.name === col).filter,
                  operator: Operator.LIKE,
                  values: [filter]
                });
              }
            }
          }
        }
      }
      return filters;
    }

    onSort = (event: any) => {
      this.setState({sortField: event.sortField, sortOrder: event.sortOrder});
      const {lazyLoad, page} = this.state;
      if (lazyLoad) {
        const start = Math.floor(page / 5) * lazyLoadSize;
        const range = [start, start + lazyLoadSize - 1];
        this.setState({loading: true, range}, () => this.getParticipantData(false));
      }
    }

    columnSort = (sortField: string) => {
      if (this.state.sortField === sortField) {
        const sortOrder = this.state.sortOrder === 1 ? -1 : 1;
        this.setState({sortOrder});
      } else {
        this.setState({sortField, sortOrder: 1});
      }
      const {lazyLoad, start} = this.state;
      if (lazyLoad) {
        const rangeStart = Math.floor(start / lazyLoadSize) * lazyLoadSize;
        const range = [rangeStart, rangeStart + lazyLoadSize - 1];
        console.log(range);
        this.setState({loading: true, range}, () => this.getParticipantData(false));
      }
    }

    onPage = (event: any) => {
      const {lazyLoad, page, range, totalCount} = this.state;
      if (lazyLoad) {
        if (event.page < page && event.page > 1 && range[0] >= (event.first - rowsPerPage)) {
          range[0] -= lazyLoadSize;
          this.setState({page: event.page, range, start: event.first}, () => this.updatePageData(true));
        } else if (event.page > page && range[1] <= (event.first + (rowsPerPage * 2)) && range[1] < totalCount) {
          range[1] = Math.min(totalCount, range[1] + lazyLoadSize);
          this.setState({page: event.page, range, start: event.first}, () => this.updatePageData(false));
        } else {
          this.setState({page: event.page, range, start: event.first});
        }
      } else {
        this.setState({page: event.page, range, start: event.first});
      }
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
      const {domain, filterState: {global: {ageMin, ageMax, dateMin, dateMax, visits}, tabs, vocab}} = this.props;
      /* Global filters */
      if (dateMin || dateMax) {
        const min = dateMin ? Date.parse(dateMin) : 0;
        const max = dateMax ? Date.parse(dateMax) : 9999999999999;
        data = data.filter(item => {
          const itemDate = Date.parse(item.itemDate);
          return itemDate >= min && itemDate <= max;
        });
      }
      if (domain !== Domain.SURVEY && (ageMin || ageMax)) {
        const min = ageMin || 0;
        const max = ageMax || 120;
        data = data.filter(item => item.ageAtEvent >= min && item.ageAtEvent <= max);
      }
      if (domain !== Domain.SURVEY
        && domain !== Domain.PHYSICALMEASUREMENT
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
      const columnFilters = tabs[domain];
      if (!columnFilters) {
        if (data.length < start + rowsPerPage) {
          start = Math.floor(data.length / rowsPerPage) * rowsPerPage;
        }
        this.setState({filteredData: data, start: start});
      } else {
        for (const col in columnFilters) {
          if (columnFilters.hasOwnProperty(col)) {
            // Makes sure we only filter by correct concept type (standard/source)
            if (columnCheck.includes(col)) {
              if (Array.isArray(columnFilters[col])) {
                // checkbox filters
                if (!columnFilters[col].length) {
                  data = [];
                  break;
                } else if (!columnFilters[col].includes('Select All')
                  && !(vocab === 'source' && domain === Domain.OBSERVATION)) {
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
        }
        if (data && data.length < start + rowsPerPage) {
          start = Math.floor(data.length / rowsPerPage) * rowsPerPage;
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

    filterText = () => {
      const {domain, filterState, getFilteredData} = this.props;
      const {tabFilterState} = this.state;
      filterState.tabs[domain] = tabFilterState;
      getFilteredData(filterState);
    }

    filterEvent(column: string) {
      const {columns, domain} = this.props;
      const {displayName} = columns.find(col => col.name === column);
      triggerEvent('Review Individual', 'Click', `${domainToTitle(domain)} - Filter - ${displayName} - Review Individual`);
    }

    checkboxFilter(column: string) {
      const {codeResults} = this.state;
      const {domain, filterState: {tabs, vocab}} = this.props;
      const columnFilters = tabs[domain];
      const filterStyle = !columnFilters[column].includes('Select All') ? filterIcons.active : filterIcons.default;
      let options: Array<any>;
      const counts = {total: 0};
      switch (column) {
        case 'domain':
          options = domains.map(name => ({name}));
          break;
        case `${vocab}Vocabulary`:
          const vocabs = vocabOptions.getValue() ? vocabOptions.getValue()[vocab] : {};
          options = vocabs[domain] ? vocabs[domain].map(name => ({name})) : [];
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
              {opt.name !== 'Select All' && <div style={{padding: '0.3rem 0 0.3rem 0.4rem'}}>
                <input style={{width: '0.7rem', height: '0.7rem'}} type='checkbox' name={opt.name}
                       checked={columnFilters[column].includes(opt.name)}
                       onChange={($event) => this.updateData($event, column, options)}/>
                <label> {opt.name} </label>
              </div>}
            </React.Fragment>);
          }
          return acc;
        }, []);
      let fl: any;
      return <React.Fragment>
        <i className='pi pi-filter'
           style={filterStyle}
           onClick={(e) => {
             this.filterEvent(column);
             fl.toggle(e);
           }}/>
        <OverlayPanel style={{left: '359.531px!important', textAlign: 'left'}} className='filterOverlay'
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
            <label> Select All </label>
          </div>
        </OverlayPanel>
      </React.Fragment>;
    }

    textFilter(column: string) {
      const {domain, filterState} = this.props;
      const {tabFilterState} = this.state;
      const columnFilters = filterState.tabs[domain];
      const filtered = !!columnFilters[column];
      let fl: any, ip: any;
      return <React.Fragment>
        <i className='pi pi-filter'
          style={filtered ? filterIcons.active : filterIcons.default}
          onClick={(e) => {
            this.filterEvent(column);
            fl.toggle(e);
            ip.focus();
          }}/>
        <OverlayPanel style={{left: '359.531px!important'}}
          className='filterOverlay'
          ref={(el) => fl = el} dismissable={true}
          onHide={() => {
            if (columnFilters[column] !== tabFilterState[column]) {
              tabFilterState[column] = columnFilters[column];
              this.setState({tabFilterState});
            }
          }}>
          <div style={styles.textSearch}>
            <i className='pi pi-search' style={{cursor: 'default', margin: '0 5px'}} />
            <TextInput
              ref={(i) => ip = i}
              style={styles.textInput}
              value={tabFilterState[column]}
              onChange={(v) => {
                tabFilterState[column] = v;
                this.setState({tabFilterState});
              }}
              onKeyUp={e => e.key === Key.Enter && this.filterText()}
              placeholder={'Search'} />
            <i className='pi pi-times-circle' style={{margin: '0 5px'}} onClick={() => {
              tabFilterState[column] = '';
              this.setState({tabFilterState}, () => this.filterText());
            }}
            title='Clear filter'/>
          </div>
        </OverlayPanel>
      </React.Fragment>;
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
      const {expandedRows, loading, lazyLoad, loadingPrevious, range, start, sortField, sortOrder, totalCount, updating} = this.state;
      const {columns, filterState: {vocab}, tabName} = this.props;
      const filteredData = loading ? null : this.state.filteredData;
      let pageReportTemplate;
      let value = null;
      let max;
      if (filteredData !== null) {
        max = lazyLoad ? totalCount : filteredData.length;
        const lastRowOfPage = Math.min(start + rowsPerPage, max);
        pageReportTemplate = `${(start + 1).toLocaleString()} -
          ${lastRowOfPage.toLocaleString()} of
          ${max.toLocaleString()} records `;
        const pageStart = loadingPrevious ? start - range[0] - lazyLoadSize : start - range[0];
        value = lazyLoad
          ? (loadingPrevious && pageStart < 0 ? [] : filteredData.slice(pageStart, pageStart + rowsPerPage))
          : filteredData;
      }
      let paginatorTemplate = 'CurrentPageReport';
      if (max > rowsPerPage) {
        paginatorTemplate += ' PrevPageLink PageLinks NextPageLink';
      }
      const spinner = loading || (updating && value && value.length === 0);
      const cols = columns.map((col) => {
        const asc = sortField === col.name && sortOrder === 1;
        const desc = sortField === col.name && sortOrder === -1;
        const colName = col.name === 'value' || col.name === `${vocab}Name`;
        const hasCheckboxFilter = [
          'domain',
          'sourceVocabulary',
          'standardVocabulary',
        ].includes(col.name);
        const hasTextFilter = [
          'sourceCode',
          'standardCode',
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
          <span style={{display: 'inline-block', marginTop: '-3px', float: 'right'}}>
            {hasCheckboxFilter && this.checkboxFilter(col.name)}
            {hasTextFilter && this.textFilter(col.name)}
            {(asc && !isExpanderNeeded) && <i className='pi pi-arrow-up' style={styles.sortIcon}
              onClick={() => this.columnSort(col.name)} />}
            {(desc && !isExpanderNeeded) && <i className='pi pi-arrow-down' style={styles.sortIcon}
              onClick={() => this.columnSort(col.name)} />}
          </span>
        </React.Fragment>;
        return <Column
          expander={isExpanderNeeded}
          style={styles.tableBody}
          bodyStyle={isExpanderNeeded ? styles.graphColumnBody : styles.columnBody}
          key={col.name}
          field={col.name}
          header={header}
          headerStyle={isExpanderNeeded ? styles.graphStyle : {}}
          sortable={!!col.filter}
          body={overlayTemplate}/>;
      });
      return <div style={styles.container}>
        <style>{datatableStyles}</style>
        <DataTable
          expandedRows={expandedRows}
          onRowToggle={(e) => this.setState({expandedRows: e.data})}
          rowExpansionTemplate={this.rowExpansionTemplate}
          rowClassName = {this.hideGraphIcon}
          style={styles.table}
          value={value}
          sortField={sortField}
          sortOrder={sortOrder}
          onSort={this.onSort}
          lazy={lazyLoad}
          paginator
          paginatorTemplate={!spinner && !!value ? paginatorTemplate : ''}
          currentPageReportTemplate={!spinner && !!value ? pageReportTemplate : ''}
          onPage={this.onPage}
          alwaysShowPaginator={false}
          first={start}
          rows={rowsPerPage}
          totalRecords={max}
          scrollable
          scrollHeight='calc(100vh - 350px)'
          autoLayout
          footer={this.errorMessage()}>
          {cols}
        </DataTable>
        {spinner && <SpinnerOverlay />}
      </div>;
    }
  }
);
