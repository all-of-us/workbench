// import 'jspreadsheet/dist/jspreadsheet.css';
// import 'jsuites/dist/jsuites.css';
import '@syncfusion/ej2-base/styles/material.css';
import '@syncfusion/ej2-buttons/styles/material.css';
import '@syncfusion/ej2-calendars/styles/material.css';
import '@syncfusion/ej2-dropdowns/styles/material.css';
import '@syncfusion/ej2-inputs/styles/material.css';
import '@syncfusion/ej2-navigations/styles/material.css';
import '@syncfusion/ej2-popups/styles/material.css';
import '@syncfusion/ej2-splitbuttons/styles/material.css';
import '@syncfusion/ej2-react-spreadsheet/styles/material.css';

import React, { useEffect, useRef, useState } from 'react';
import { RouteComponentProps, withRouter } from 'react-router-dom';
import * as fp from 'lodash/fp';

import parser from '@jspreadsheet/parser';
import { jspreadsheet, Spreadsheet, Worksheet } from '@jspreadsheet/react';
import { SpreadsheetComponent as SpreadsheetSync } from '@syncfusion/ej2-react-spreadsheet';
import { withCurrentWorkspace } from 'app/utils';
import { withRuntimeStore } from 'app/utils/runtime-hooks';
import { withUserAppsStore } from 'app/utils/runtime-utils';
import { withNavigation } from 'app/utils/with-navigation-hoc';
import { parse } from 'url';

// Set license
jspreadsheet.setLicense(
  'YzZlY2VhZGU0OTVmNTNlNDA4MDI0NmI4MWI4MzhjNDc1OGVjMGViMjUyZGUzZmE4M2Q3MGQ2ZTA4YzUzMWE5NWNmN2ZmY2YyY2QyMzE1MDIyZWUwNWQzZGUyMTQ0ZTYyNmMzOTNhNjljYWE0MmE1ZWY1YTNiYjgzOThjNzY1ZGYsZXlKamJHbGxiblJKWkNJNklpSXNJbTVoYldVaU9pSktjM0J5WldGa2MyaGxaWFFpTENKa1lYUmxJam94TnpFMk5UTTBNVGd3TENKa2IyMWhhVzRpT2xzaWFuTndjbVZoWkhOb1pXVjBMbU52YlNJc0ltTnZaR1Z6WVc1a1ltOTRMbWx2SWl3aWFuTm9aV3hzTG01bGRDSXNJbU56WWk1aGNIQWlMQ0ozWldJaUxDSnNiMk5oYkdodmMzUWlYU3dpY0d4aGJpSTZJak0wSWl3aWMyTnZjR1VpT2xzaWRqY2lMQ0oyT0NJc0luWTVJaXdpZGpFd0lpd2lkakV4SWl3aVkyaGhjblJ6SWl3aVptOXliWE1pTENKbWIzSnRkV3hoSWl3aWNHRnljMlZ5SWl3aWNtVnVaR1Z5SWl3aVkyOXRiV1Z1ZEhNaUxDSnBiWEJ2Y25SbGNpSXNJbUpoY2lJc0luWmhiR2xrWVhScGIyNXpJaXdpYzJWaGNtTm9JaXdpY0hKcGJuUWlMQ0p6YUdWbGRITWlMQ0pqYkdsbGJuUWlMQ0p6WlhKMlpYSWlMQ0p6YUdGd1pYTWlYU3dpWkdWdGJ5STZkSEoxWlgwPQ=='
);

// Set extensions
jspreadsheet.setExtensions({ parser });

// Create the spreadsheet from a local file
const load = function (e, other) {
  // Parse XLSX file and create a new spreadsheet
  const ss = other.current;
  if (ss) {
    ss.open({ file: e.target.files[0] });
  }
};
const SSpreadsheetComponent = () => {
  // Spreadsheet array of worksheets
  const spreadsheetRef = useRef(null);
  const [count, setCount] = useState(0);
  // Render component
  return (
    <div>
      <div>
        <h1>
          Do not load sensitive data. This is processeed with external service
        </h1>
        <input
          type={'file'}
          name={'file'}
          id={'file'}
          onChange={(e) => {
            setCount(count + 1);
            load(e, spreadsheetRef);
          }}
          style={{ display: 'none' }}
        />
        <input
          type={'button'}
          value={`Load a XLSX file from my local computer ${count}`}
          onClick={() => document.getElementById('file').click()}
        />
      </div>
      <div>
        <SpreadsheetSync
          ref={spreadsheetRef}
          showRibbon={true}
          showFormulaBar={true}
          openUrl='https://services.syncfusion.com/react/production/api/spreadsheet/open'
        />
      </div>
    </div>
  );
};

export default SSpreadsheetComponent;
