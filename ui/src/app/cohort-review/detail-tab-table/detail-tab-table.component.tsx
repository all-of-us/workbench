import {Component, Input} from '@angular/core';
import {ReviewDomainChartsComponent} from 'app/cohort-review/review-domain-charts/review-domain-charts';
import {SpinnerOverlay} from 'app/components/spinners';
import {cohortReviewApi} from 'app/services/swagger-fetch-clients';
import {WorkspaceData} from 'app/services/workspace-storage.service';
import {reactStyles, ReactWrapperBase, withCurrentWorkspace} from 'app/utils';
import {urlParamsStore} from 'app/utils/navigation';
import {DomainType, PageFilterRequest, PageFilterType, SortOrder} from 'generated/fetch';
import * as moment from 'moment';
import {Column} from 'primereact/column';
import {DataTable} from 'primereact/datatable';
import {OverlayPanel} from 'primereact/overlaypanel';
import {TabPanel, TabView} from 'primereact/tabview';
import * as React from 'react';


const css = `
  body .p-datatable .p-sortable-column:not(.p-highlight):hover,
  body .p-datatable .p-sortable-column.p-highlight {
    color: #333333;
    background-color: #f4f4f4;
  }
  body .p-datatable .p-datatable-thead > tr > th {
    padding: 10px 5px 10px 10px;
    vertical-align: middle;
    background: #f4f4f4;
    border: 0;
    border-bottom: 1px solid #c8c8c8;
    border-left: 1px solid #c8c8c8;
  }
  body .p-datatable .p-datatable-thead > tr > th:first-of-type {
    border-left: 0;
  }
  body .p-datatable .p-datatable-tbody > tr:not(last-of-type) {
    border-bottom: 1px solid #c8c8c8;
  }
  body .p-datatable .p-column-title {
    display: flex;
  }
  .pi.pi-sort,
  .pi.pi-sort-up,
  .pi.pi-sort-down {
    display: none;
  }
  .p-datatable .p-datatable-scrollable-wrapper {
    border: 1px solid #c8c8c8;
  }
  .p-datatable .p-paginator.p-paginator-bottom {
    border: 0;
    margin-top: 20px;
    background: none;
    font-size: 12px;
    text-align: right;
  }
  body .p-paginator .p-paginator-pages {
    display: inline;
  }
  body .p-paginator .p-paginator-prev,
  body .p-paginator .p-paginator-next,
  body .p-paginator .p-paginator-pages .p-paginator-page {
    border: 1px solid #cccccc;
    border-radius: 3px;
    background: #fafafa;
    color: #2691D0;
    height: auto;
    width: auto;
    min-width: 0;
    padding: 7px;
    margin: 0 2px;
    line-height: 0.5rem;
  }
  body .p-paginator .p-paginator-prev,
  body .p-paginator .p-paginator-next {
    height: 28px;
    width: 24px;
  }
  body .p-paginator .p-paginator-pages .p-paginator-page:focus {
    box-shadow: 0;
  }
  body .p-paginator .p-paginator-pages .p-paginator-page.p-highlight {
    background: #fafafa;
    color: rgba(0, 0, 0, .5);
  }
  body .p-overlaypanel .p-overlaypanel-close {
    top: 0.231em;
    right: 0.231em;
    background-color: white;
    color: #0086C1;
  }
  body .p-overlaypanel {
    top: 19px!important;
    left: 0px!important;
    width:9.5rem;
  }
  body .p-overlaypanel .p-overlaypanel-close:hover {
    top: 0.231em;
    right: 0.231em;
    background-color: white;
    color: #0086C1;
  }
  body .p-overlaypanel .p-overlaypanel-content {
    padding: 0.6rem 0.6rem;
    font-size: 13px;
  }
  .pi-chevron-right:before {
    content: "\\E96D";
    color: rgb(0, 134, 193);
    font-size: 0.8rem;
    line-height: 0rem;
  }
 .pi-chevron-down:before {
    content: "\\E96D";
    color: rgb(0, 134, 193);
    font-size: 0.8rem;
    line-height: 0rem;
  }
  .graphExpander .pi-chevron-right {
    display: none;
  }
   .graphExpander body .p-datatable .p-datatable-tbody > tr > td  {
    border: none!important;
  }
  body .p-tabview.p-tabview-top,
  body .p-tabview.p-tabview-bottom,
  body .p-tabview.p-tabview-left, body
  .p-tabview.p-tabview-right {
    margin: 1rem 11rem;
    background-color: transparent;
  }

  body .p-tabview.p-tabview-top .p-tabview-nav li a,
  body .p-tabview.p-tabview-bottom .p-tabview-nav li a,
  body .p-tabview.p-tabview-left .p-tabview-nav li a,
  body .p-tabview.p-tabview-right .p-tabview-nav li a {
    border: none;
    background-color: transparent;
  }
	body .p-tabview.p-tabview-top .p-tabview-nav li.p-highlight a,
	body .p-tabview.p-tabview-bottom .p-tabview-nav li.p-highlight a,
	body .p-tabview.p-tabview-left .p-tabview-nav li.p-highlight a,
	body .p-tabview.p-tabview-right .p-tabview-nav li.p-highlight a {
    background-color: none!important;
    border-bottom: 3px solid #007ad9!important;
    color: black;
  }
  body .p-tabview.p-tabview-top .p-tabview-nav li.p-highlight a,
  body .p-tabview.p-tabview-bottom .p-tabview-nav li.p-highlight a,
  body .p-tabview.p-tabview-left .p-tabview-nav li.p-highlight a,
  body .p-tabview.p-tabview-right .p-tabview-nav li.p-highlight a {
    background-color: transparent!important;
    border: none;
    color: black;
  }

  body .p-tabview.p-tabview-top .p-tabview-nav
  li.p-highlight:hover a, body .p-tabview.p-tabview-bottom
  .p-tabview-nav li.p-highlight:hover a, body .p-tabview.p-tabview-left
  .p-tabview-nav li.p-highlight:hover a, body .p-tabview.p-tabview-right
  .p-tabview-nav li.p-highlight:hover a {
    border: none;
    background-color: transparent;
    color: black;
  }
  body .p-tabview.p-tabview-top .p-tabview-nav
  li.p-highlight:hover a, body .p-tabview.p-tabview-bottom
  .p-tabview-nav li.p-highlight:focus a, body .p-tabview.p-tabview-left
  .p-tabview-nav li.p-highlight:focus a, body .p-tabview.p-tabview-right
  .p-tabview-nav li.p-highlight:focus a {
    border: none;
    background-color: transparent;
    color: black;
  }
  body .p-tabview.p-tabview-top
  .p-tabview-nav li:not(.p-highlight):not(.p-disabled):hover a,
  body .p-tabview.p-tabview-bottom .p-tabview-nav li:not(.p-highlight):not(.p-disabled):hover a,
  body .p-tabview.p-tabview-left .p-tabview-nav li:not(.p-highlight):not(.p-disabled):hover a,
  body .p-tabview.p-tabview-right .p-tabview-nav li:not(.p-highlight):not(.p-disabled):hover a {
    background-color: transparent;
    border-bottom: 3px solid #007ad9!important;
    border-top: none;
    border-right: none;
    border-left: none;
    color: black;
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
    lineHeight: '0.6rem'
  },
  columnHeader: {
    background: '#f4f4f4',
    color: '#262262',
    fontWeight: 600,
  },
  columnBody: {
    background: '#ffffff',
    padding: '5px',
    verticalAlign: 'top',
    textAlign: 'left',
    borderLeft: 0,
    borderRight: 0,
    borderBottom: 'none',
    lineHeight: '0.6rem',
  },
  graphColumnBody: {
    // background: '#ffffff',
    // padding: '5px',
    // verticalAlign: 'top',
    // textAlign: 'left',
    // borderLeft: 0,
    // borderRight: 0,
    // lineHeight: '0.6rem',
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
  graphStyle: {
    borderLeft: 'none',
    width: '2rem',
  },
  testHeader: {

  }
});
const rows = 25;

export interface DetailTabTableProps {
  tabname: string;
  columns: Array<any>;
  domain: string;
  filterType: PageFilterType;
  participantId: number;
  workspace: WorkspaceData;
}

export interface DetailTabTableState {
  data: Array<any>;
  loading: boolean;
  start: number;
  sortField: string;
  sortOrder: number;
  expandedRows: Array<any>;
  openClickedTab: any;
}

export const DetailTabTable = withCurrentWorkspace()(
  class extends React.Component<DetailTabTableProps, DetailTabTableState> {
    valueArray: any;
    constructor(props: DetailTabTableProps) {
      super(props);
      this.state = {
        data: null,
        loading: true,
        start: 0,
        sortField: null,
        sortOrder: 1,
        expandedRows: [],
        openClickedTab: {}
      };
    }

    componentDidMount() {
      this.getParticipantData();
    }

    componentDidUpdate(prevProps: any) {
      if (prevProps.participantId !== this.props.participantId) {
        this.setState({
          data: null,
          loading: true,
        });
        this.getParticipantData();
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
            <OverlayPanel ref={(el) => {vl = el; }} showCloseIcon={true} dismissable={true}>
              {(rowData.refRange &&  column.field === 'value') &&
                <div style={{paddingBottom: '0.2rem'}}>Reference Range: {rowData.refRange}</div>}
              {(rowData.unit && column.field === 'value') &&
                <div>Units: {rowData.unit}</div>}
              {nameField &&
                <div>Route: {rowData.route}</div>}
            </OverlayPanel>
      </div>;
    }

    groupByData = (objectArray: any, property: any) => {
      return objectArray.reduce((acc, obj) => {
        const key = obj[property];
        if (!acc[key]) {
          acc[key] = [];
        }
        acc[key].push(obj);
        return acc;
      }, {});
    }

    rowExpansionTemplate = (rowData: any) => {
      const {data} = this.state;
      const conceptIdBasedData = this.groupByData(data, 'standardConceptId');
      const unitsObj = this.groupByData(conceptIdBasedData[rowData.standardConceptId], 'unit');
      const unitKey = Object.keys(unitsObj);
      return rowData.standardName === 'No matching concept' ? <div>NO Data to show</div>  : <TabView>
        {unitKey.map((k, i) => {
          { this.valueArray = unitsObj[k].map(v => {
            return {
              values: parseInt(v.value, 10),
              date: v.itemDate,
              standardConceptId: v.standardConceptId
            };
          }); }
          return <TabPanel header={k} key={i}>
            <ReviewDomainChartsComponent orgData={this.valueArray} unitName={k}/>
          </TabPanel>;
        })}
      </TabView>;
    }
    hideGraphIcon = (rowData: any) => {
      const noConcept = rowData.standardName && rowData.standardName === 'No matching concept'
          return {'graphExpander' : noConcept};
    }
    render() {
      const {data, loading, start, sortField, sortOrder} = this.state;
      let pageReportTemplate;
       let noMatch;
      if (data !== null) {
        const lastRowOfPage = (start + rows) > data.length
          ? start + rows - (start + rows - data.length) : start + rows;
        pageReportTemplate = `${start + 1} - ${lastRowOfPage} of ${data.length} records `;
      }
      let paginatorTemplate = 'CurrentPageReport';
      if (data && data.length > rows) {
        paginatorTemplate += ' PrevPageLink PageLinks NextPageLink';
      }
        const columns = this.props.columns.map((col) => {
        const asc = sortField === col.name && sortOrder === 1;
        const desc = sortField === col.name && sortOrder === -1;
        const colName = col.name === 'value' || col.name === 'standardName';
        const isExpanderNeeded = col.name === 'graph'  &&
          (this.props.tabname === 'Vitals' || this.props.tabname === 'Labs');
        const overlayTemplate = colName && this.overlayTemplate;
        const header = <React.Fragment>
          <span
            onClick={() => this.columnSort(col.name)}
            style={styles.columnHeader}>
            {col.displayName}
          </span>
          {(asc && !isExpanderNeeded) && <i className='pi pi-arrow-up' style={styles.sortIcon} />}
          {(desc && !isExpanderNeeded)  && <i className='pi pi-arrow-down' style={styles.sortIcon} />}
        </React.Fragment>;

        return <Column
          expander={isExpanderNeeded}
          style={styles.tableBody}
          bodyStyle={isExpanderNeeded ? styles.graphColumnBody && styles.columnBody : styles.columnBody}
          key={col.name}
          field={col.name}
          header={header}
          headerStyle={isExpanderNeeded && styles.graphStyle}
          sortable
          body={overlayTemplate}/>;
      });
      return <div style={styles.container} >
        <style>{css}</style>
        {data && <DataTable  expandedRows={this.state.expandedRows}
          onRowToggle={(e) => this.setState({expandedRows: e.data})}
          rowExpansionTemplate={this.rowExpansionTemplate}
          rowClassName = {this.hideGraphIcon}
          style={styles.table}
          value={data}
          sortField={sortField}
          sortOrder={sortOrder}
          onSort={this.onSort}
          paginator
          paginatorTemplate={data.length ? paginatorTemplate : ''}
          currentPageReportTemplate={data.length ? pageReportTemplate : ''}
          onPage={this.onPage}
          first={start}
          rows={rows}
          totalRecords={data.length}
          scrollable
          scrollHeight='calc(100vh - 350px)'
          autoLayout
          emptyMessage={data !== null ? 'No ' + this.props.tabname + ' Data' : ''}>
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
  @Input('tabname') tabname: DetailTabTableProps['tabname'];
  @Input('columns') columns: DetailTabTableProps['columns'];
  @Input('domain') domain: DetailTabTableProps['domain'];
  @Input('filterType') filterType: DetailTabTableProps['filterType'];
  @Input('participantId') participantId: DetailTabTableProps['participantId'];

  constructor() {
    super(DetailTabTable, [
      'tabname',
      'columns',
      'domain',
      'filterType',
      'participantId',
    ]);
  }
}
