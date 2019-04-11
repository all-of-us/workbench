import {Component, Input} from '@angular/core';
import {ReviewDomainChartsComponent} from 'app/cohort-review/review-domain-charts/review-domain-charts';
import {vocabOptions} from 'app/cohort-review/review-state.service';
import {css} from 'app/cohort-review/review-utils/primeReactCss.utils';
import {TextInput} from 'app/components/inputs';
import {SpinnerOverlay} from 'app/components/spinners';
import {cohortReviewApi} from 'app/services/swagger-fetch-clients';
import {WorkspaceData} from 'app/services/workspace-storage.service';
import {reactStyles, ReactWrapperBase, withCurrentWorkspace} from 'app/utils';
import {urlParamsStore} from 'app/utils/navigation';
import {DomainType, PageFilterRequest, PageFilterType, SortOrder} from 'generated/fetch';
import * as fp from 'lodash/fp';
import * as moment from 'moment';
import {Column} from 'primereact/column';
import {DataTable} from 'primereact/datatable';
import {OverlayPanel} from 'primereact/overlaypanel';
import {TabPanel, TabView} from 'primereact/tabview';
import * as React from 'react';


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
    lineHeight: '0.6rem'
  },
  columnHeader: {
    background: '#f4f4f4',
    color: '#262262',
    fontWeight: 600,
  },
  columnBody: {
    background: '#ffffff',
    padding: '0.5rem 0.5rem 0.3rem',
    verticalAlign: 'top',
    textAlign: 'left',
    borderLeft: 0,
    borderRight: 0,
    borderBottom: 'none',
    lineHeight: '0.6rem',
  },
  graphColumnBody: {
    background: '#ffffff',
    padding: '5px',
    verticalAlign: 'top',
    textAlign: 'left',
    borderLeft: 0,
    borderRight: 0,
    lineHeight: '0.6rem',
    width: '2rem',
  },

  filterIcon: {
    color: '#0086C1',
    fontSize: '0.5rem',
    float: 'right'
  },
  sortIcon: {
    color: '#2691D0',
    fontSize: '0.4rem'
  },
  overlayHeader: {
    padding: '0.3rem',
  },
  caretIcon: {
    fontSize: '0.6rem',
    paddingLeft: '0.4rem',
    color: '#0086C1',
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
    color: '#2691D0',
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
  textSearch: {
    width: '85%',
    borderRadius: '4px',
    backgroundColor: '#dae6ed',
    marginLeft: '5px'
  },
  textInput: {
    width: 'auto',
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
});
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

export interface DetailTabTableProps {
  tabName: string;
  columns: Array<any>;
  domain: string;
  filterType: PageFilterType;
  participantId: number;
  workspace: WorkspaceData;
  filterState: any;
  getFilteredData: Function;
  updateState: number;
}

export interface DetailTabTableState {
  data: Array<any>;
  filteredData: Array<any>;
  loading: boolean;
  start: number;
  sortField: string;
  sortOrder: number;
  expandedRows: Array<any>;
  codeResults: Array<any>;
}

export const DetailTabTable = withCurrentWorkspace()(
  class extends React.Component<DetailTabTableProps, DetailTabTableState> {
    codeInputChange: Function;
    constructor(props: DetailTabTableProps) {
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
      };
      this.codeInputChange = fp.debounce(300, (e) => this.filterCodes(e));
    }

    componentDidMount() {
      this.getParticipantData();
    }

    componentDidUpdate(prevProps: any) {
      if (prevProps.participantId !== this.props.participantId) {
        this.setState({
          data: null,
          filteredData: null,
          loading: true,
        });
        this.getParticipantData();
      } else if (prevProps.updateState !== this.props.updateState) {
        this.filterData();
      }
    }

    getParticipantData() {
      try {
        const {cdrVersionId, id, namespace} = this.props.workspace;
        const {cid} = urlParamsStore.getValue();
        const pageFilterRequest = {
          page: 0,
          pageSize: 10000,
          sortOrder: SortOrder.Asc,
          sortColumn: this.props.columns[0].name,
          pageFilterType: this.props.filterType,
          domain: this.props.domain,
        } as PageFilterRequest;

        cohortReviewApi().getParticipantData(
          namespace,
          id,
          +cid,
          +cdrVersionId,
          this.props.participantId,
          pageFilterRequest
        ).then(response => {
          response.items.forEach(item => {
            if (this.props.domain === DomainType[DomainType.VITAL]
              || this.props.domain === DomainType[DomainType.LAB]) {
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

    overlayTemplate(rowData: any, column: any) {
      let vl: any;
      const valueField = (rowData.refRange || rowData.unit ) && column.field === 'value';
      const nameField = rowData.route && column.field === 'standardName';
      return <div style={{position: 'relative'}}>
        { column.field === 'value' && <span>{rowData.value}</span>}
        { column.field === 'standardName' && <span>{rowData.standardName}</span>}
        {(valueField || nameField)
        && <i className='pi pi-caret-down' style={styles.caretIcon} onClick={(e) => vl.toggle(e)}/>}
        <OverlayPanel className='labOverlay' ref={(el) => vl = el}
                      showCloseIcon={true} dismissable={true}>
          {(rowData.refRange &&  column.field === 'value') &&
          <div style={{paddingBottom: '0.2rem'}}>Reference Range: {rowData.refRange}</div>}
          {(rowData.unit && column.field === 'value') &&
          <div>Units: {rowData.unit}</div>}
          {nameField &&
          <div>Route: {rowData.route}</div>}
        </OverlayPanel>
      </div>;
    }

    updateData = (event, colName, namesArray) => {
      const {checked, name} = event.target;
      const {domain, filterState} = this.props;
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
      this.props.getFilteredData(filterState);
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
      if (this.props.domain !== DomainType[DomainType.SURVEY] && (ageMin || ageMax)) {
        const min = ageMin || 0;
        const max = ageMax || 120;
        data = data.filter(item => item.ageAtEvent >= min && item.ageAtEvent <= max);
      }
      if (this.props.domain !== DomainType[DomainType.SURVEY]
        && this.props.domain !== DomainType[DomainType.PHYSICALMEASUREMENT]
        && visits) {
        data = data.filter(item => visits === item.visitType);
      }
      /* Column filters */
      const columnFilters = filterState.tabs[domain];
      if (!columnFilters) {
        if (data.length < start + rows) {
          start = Math.floor(data.length / rows) * rows;
        }
        this.setState({filteredData: data, start: start});
      } else {
        for (const col in columnFilters) {
          if (Array.isArray(columnFilters[col])) {
            if (!columnFilters[col].length) {
              data = [];
              break;
            } else if (!columnFilters[col].includes('Select All')
              && !(vocab === 'source' && domain === DomainType[DomainType.OBSERVATION])) {
              data = data.filter(row => columnFilters[col].includes(row[col]));
            }
          } else {
            if (columnFilters[col]) {
              data = data.filter(
                row => row[col] && row[col].toLowerCase().includes(columnFilters[col].toLowerCase())
              );
            }
          }
        }
        if (data.length < start + rows) {
          start = Math.floor(data.length / rows) * rows;
        }
        this.setState({filteredData: data, start: start});
      }
    }

    emptyMessage = () => {
      const {data, filteredData} = this.state;
      if (data && data.length === 0) {
        return  'No ' + this.props.tabName + ' Data';
      } else if (data && data.length > 0 && filteredData && filteredData.length === 0) {
        return 'There is data, but it is currently hidden. Please check your filters';
      }
    }

    filterCodes = (input: string) => {
      if (!input) {
        this.setState({codeResults: null});
      } else {
        const {data} = this.state;
        const {filterState: {vocab}} = this.props;
        const codeType = `${vocab}Code`;
        const codeResults = data.reduce((acc, item) => {
          if (item[codeType].includes(input) && !acc.includes(input)) {
            acc.push(item[codeType]);
          }
          return acc;
        }, []);
        this.setState({codeResults});
      }
    }

    filterText = (input: string, column: string) => {
      const {domain, filterState} = this.props;
      filterState.tabs[domain][column] = input;
      this.props.getFilteredData(filterState);
    }

    checkboxFilter(column: string) {
      const {codeResults, data} = this.state;
      const {domain, filterState, filterState: {vocab}} = this.props;
      const columnFilters = filterState.tabs[domain];
      if (!data) {
        return {};
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
      const checkboxes = codeResults && codeResults.length === 0
        ? <em style={styles.noResults}>No matching codes</em>
        : options.reduce((acc, opt, i) => {
          if (!codeResults || codeResults.includes(opt.name)) {
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
      let fl: any;
      return <span>
        <i className='pi pi-filter' onClick={(e) => fl.toggle(e)}/>
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
      let fl: any, ip: any;
      return <span>
        <i className='pi pi-filter' onClick={(e) => {
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
      const conceptIdBasedData = fp.groupBy('standardConceptId', data);
      const unitsObj = fp.groupBy( 'unit', conceptIdBasedData[rowData.standardConceptId]);
      const unitKey = Object.keys(unitsObj);
      let valueArray;
      return <React.Fragment>
        <div style={styles.headerStyle}>{rowData.standardName}</div>
      <TabView className='unitTab'>
        {unitKey.map((k, i) => {
          const name = k === 'null' ? 'No Unit' : k;
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
      const noConcept = rowData.standardName && rowData.standardName === 'No matching concept';
      return {'graphExpander' : noConcept};
    }

    render() {
      const {filteredData, loading, start, sortField, sortOrder} = this.state;
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
      const columns = this.props.columns.map((col) => {
        const asc = sortField === col.name && sortOrder === 1;
        const desc = sortField === col.name && sortOrder === -1;
        const colName = col.name === 'value' || col.name === 'standardName';
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
          (this.props.tabName === 'Vitals' || this.props.tabName === 'Labs');
        const overlayTemplate = colName && this.overlayTemplate;
        const header = <React.Fragment>
          <span
            onClick={() => this.columnSort(col.name)}
            style={styles.columnHeader}>
            {col.displayName}
          </span>
          {hasCheckboxFilter && this.checkboxFilter(col.name)}
          {hasTextFilter && this.textFilter(col.name)}
          {(asc && !isExpanderNeeded) && <i className='pi pi-arrow-up' style={styles.sortIcon} />}
          {(desc && !isExpanderNeeded) &&
          <i className='pi pi-arrow-down' style={styles.sortIcon} />}
        </React.Fragment>;

        return <Column
          expander={isExpanderNeeded}
          style={styles.tableBody}
          bodyStyle={isExpanderNeeded ? styles.graphColumnBody : styles.columnBody}
          key={col.name}
          field={col.name}
          header={header}
          headerStyle={isExpanderNeeded && styles.graphStyle}
          sortable
          body={overlayTemplate}/>;
      });

      return <div style={styles.container}>
        <style>{css}</style>
        {filteredData && <DataTable
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
          paginatorTemplate={filteredData.length ? paginatorTemplate : ''}
          currentPageReportTemplate={filteredData.length ? pageReportTemplate : ''}
          onPage={this.onPage}
          first={start}
          rows={rows}
          totalRecords={filteredData.length}
          scrollable
          scrollHeight='calc(100vh - 350px)'
          autoLayout
          emptyMessage={this.emptyMessage()}>
          {columns}
        </DataTable>}
        {loading && <SpinnerOverlay />}
      </div>;
    }
  }
);

@Component ({
  selector : 'app-detail-tab-table',
  template: '<div #root></div>'
})
export class DetailTabTableComponent extends ReactWrapperBase {
  @Input('tabName') tabName: DetailTabTableProps['tabName'];
  @Input('columns') columns: DetailTabTableProps['columns'];
  @Input('domain') domain: DetailTabTableProps['domain'];
  @Input('filterType') filterType: DetailTabTableProps['filterType'];
  @Input('participantId') participantId: DetailTabTableProps['participantId'];
  @Input('filterState') filterState: DetailTabTableProps['filterState'];
  @Input('getFilteredData') getFilteredData: DetailTabTableProps['getFilteredData'];
  @Input('updateState') updateState: DetailTabTableProps['updateState'];

  constructor() {
    super(DetailTabTable, [
      'tabName',
      'columns',
      'domain',
      'filterType',
      'participantId',
      'filterState',
      'getFilteredData',
      'updateState',
    ]);
  }
}
