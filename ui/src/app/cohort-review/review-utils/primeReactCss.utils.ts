export const datatableStyles = `
  body .p-datatable .p-sortable-column:not(.p-highlight):hover,
  body .p-datatable .p-sortable-column.p-highlight {
    color: #333333;
    background-color: #f4f4f4;
  }
  body .p-datatable .p-datatable-thead > tr > th {
    padding: 10px;
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
  .pi.pi-sort,
  .pi.pi-sort-up,
  .pi.pi-sort-down {
    display: none;
  }
  .p-datatable .p-datatable-scrollable-wrapper {
    border: 1px solid #c8c8c8;
    border-radius: 3px;
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
    cursor: default;
  }
  body .labOverlay.p-overlaypanel .p-overlaypanel-close {
    top: 0.231em;
    right: 0.231em;
    background-color: white;
    color: #0086C1;
  }
  body .labOverlay.p-overlaypanel {
    top: 1.25rem!important;
    left: -2.75rem!important;
    width:9.5rem;
  }
  body .labOverlay.p-overlaypanel .p-overlaypanel-close:hover {
    top: 0.231em;
    right: 0.231em;
    background-color: white;
    color: #0086C1;
  }
  body .labOverlay.p-overlaypanel .p-overlaypanel-content {
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
    border-bottom: 2px solid white;
  }

  body .unitTab.p-tabview.p-tabview-top,
  body .unitTab.p-tabview.p-tabview-bottom,
  body .unitTab.p-tabview.p-tabview-left, body
  .unitTab.p-tabview.p-tabview-right {
    margin: 0rem 11rem;
    background-color: transparent;
  }

  body .unitTab.p-tabview.p-tabview-top .p-tabview-nav li a,
  body .unitTab.p-tabview.p-tabview-bottom .p-tabview-nav li a,
  body .unitTab.p-tabview.p-tabview-left .p-tabview-nav li a,
  body .unitTab.p-tabview.p-tabview-right .p-tabview-nav li a {
    border: none;
    background-color: transparent;
  }
	body .unitTab.p-tabview.p-tabview-top .p-tabview-nav li.p-highlight a,
	body .unitTab.p-tabview.p-tabview-bottom .p-tabview-nav li.p-highlight a,
	body .unitTab.p-tabview.p-tabview-left .p-tabview-nav li.p-highlight a,
	body .unitTab.p-tabview.p-tabview-right .p-tabview-nav li.p-highlight a {
    background-color: none!important;
    border-bottom: 4px solid #007ad9!important;
    color: #2691D0;
    font-size: 14px;
  }
  body .unitTab.p-tabview.p-tabview-top .p-tabview-nav li.p-highlight a,
  body .unitTab.p-tabview.p-tabview-bottom .p-tabview-nav li.p-highlight a,
  body .unitTab.p-tabview.p-tabview-left .p-tabview-nav li.p-highlight a,
  body .unitTab.p-tabview.p-tabview-right .p-tabview-nav li.p-highlight a {
    background-color: transparent!important;
    border: none;
    color: #2691D0;
    font-size: 14px;
  }

  body .unitTab.p-tabview.p-tabview-top .p-tabview-nav
  li.p-highlight:hover a, body .unitTab.p-tabview.p-tabview-bottom
  .p-tabview-nav li.p-highlight:hover a, body .unitTab.p-tabview.p-tabview-left
  .p-tabview-nav li.p-highlight:hover a, body .unitTab.p-tabview.p-tabview-right
  .p-tabview-nav li.p-highlight:hover a {
    border: none;
    background-color: transparent;
    color: #2691D0;
    font-size: 14px;
  }
  body .unitTab.p-tabview.p-tabview-top .p-tabview-nav
  li.p-highlight:hover a, body .unitTab.p-tabview.p-tabview-bottom
  .p-tabview-nav li.p-highlight:focus a, body .unitTab.p-tabview.p-tabview-left
  .p-tabview-nav li.p-highlight:focus a, body .unitTab.p-tabview.p-tabview-right
  .p-tabview-nav li.p-highlight:focus a {
    border: none;
    background-color: transparent;
    color: #2691D0;
    font-size: 14px;
  }
  body .unitTab.p-tabview.p-tabview-top
  .p-tabview-nav li:not(.p-highlight):not(.p-disabled):hover a,
  body .unitTab.p-tabview.p-tabview-bottom
  .p-tabview-nav li:not(.p-highlight):not(.p-disabled):hover a,
  body .unitTab.p-tabview.p-tabview-left
  .p-tabview-nav li:not(.p-highlight):not(.p-disabled):hover a,
  body .unitTab.p-tabview.p-tabview-right
  .p-tabview-nav li:not(.p-highlight):not(.p-disabled):hover a {
    background-color: transparent;
    border-bottom: 4px solid #007ad9!important;
    border-top: none;
    border-right: none;
    border-left: none;
    color: #2691D0;
    font-size: 14px;
  }
 {
    outline: 0 none;
    outline-offset: 0;
    box-shadow: 0 0 0 0.2em #8dcdff;
  }
  body .unitTab.p-tabview .p-tabview-panels {
    border: none;
    background-color: transparent;
  }
  body .unitTab.p-tabview.p-tabview-top .p-tabview-nav {
    margin-left: 3rem;
    border-bottom: 1px solid #007ad9;
  }

  body .p-datatable .p-datatable-tbody > tr > td {
    background: #ECF1F4;
    border: none;
  }

  body .p-datatable .p-sortable-column:focus,
  body .p-link:focus, body .p-tabview.p-tabview-top
  .p-tabview-nav li a:not(.p-disabled):focus, body
  .p-tabview.p-tabview-bottom .p-tabview-nav li
  a:not(.p-disabled):focus, body .p-tabview.p-tabview-left
  .p-tabview-nav li a:not(.p-disabled):focus,
  body .p-tabview.p-tabview-right
  .p-tabview-nav li a:not(.p-disabled):focus{
    outline: 0;
    outline-offset: 0;
    box-shadow: none;
  }
     body .filterOverlay.p-overlaypanel .p-overlaypanel-close {
    top: 0.231em;
    right: 0.231em;
    background-color: white;
    color: #0086C1;
  }
  body .filterOverlay.p-overlaypanel {
    top: 1.6rem!important;
    left: auto!important;
    width:9.5rem;
  }
  body .filterOverlay.p-overlaypanel .p-overlaypanel-close:hover {
    top: 0.231em;
    right: 0.231em;
    background-color: white;
    color: #0086C1;
  }
  body .filterOverlay.p-overlaypanel .p-overlaypanel-content {
    padding: 0.7em 0em;
    font-size: 13px;
  }
  `;
