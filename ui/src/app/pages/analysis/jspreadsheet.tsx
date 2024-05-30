import React, { useEffect, useRef, useState } from 'react';
import { RouteComponentProps, withRouter } from 'react-router-dom';
import * as fp from 'lodash/fp';

import parser from '@jspreadsheet/parser';
import { jspreadsheet, Spreadsheet, Worksheet } from '@jspreadsheet/react';
import { withCurrentWorkspace } from 'app/utils';
import { withRuntimeStore } from 'app/utils/runtime-hooks';
import { withUserAppsStore } from 'app/utils/runtime-utils';
import { withNavigation } from 'app/utils/with-navigation-hoc';
import { parse } from 'url';

import '/node_modules/jspreadsheet/dist/jspreadsheet.css';
import '/node_modules/jsuites/dist/jsuites.css';

// Set license
jspreadsheet.setLicense(
  'NTYwMmQ2M2QzNGIyZmVmNmMxY2U0YzI2OTRkMjgyMGM2YTYzNTNjOGI5NTdkYzkxMmM5OTdjODAyYzVjYzg5ZmIyZjk4NWQ3NzYzMGU3MDExNmI2ODhjZGM2OWVlZGZkNGM0YzZlMzI3ZjlhODZiMzFlOTA3OWYzNmYzMjAxNGEsZXlKamJHbGxiblJKWkNJNklqUXdaalExT1RFd1pqRTNOV1ZsT0RVek1URXlOR0kzWlRVNE9Ea3pOVGhsTldKallXRXpPRFFpTENKdVlXMWxJam9pUlhKcFl5SXNJbVJoZEdVaU9qRTNNVGt3TVRBNE1EQXNJbVJ2YldGcGJpSTZXeUozWldJaUxDSnNiMk5oYkdodmMzUWlYU3dpY0d4aGJpSTZNekVzSW5OamIzQmxJanBiSW5ZM0lpd2lkamdpTENKMk9TSXNJbll4TUNJc0luWXhNU0lzSW1admNtMTFiR0VpTENKbWIzSnRjeUlzSW5KbGJtUmxjaUlzSW5CaGNuTmxjaUlzSW1sdGNHOXlkR1Z5SWl3aWMyVmhjbU5vSWl3aVkyOXRiV1Z1ZEhNaUxDSjJZV3hwWkdGMGFXOXVjeUlzSW1Ob1lYSjBjeUlzSW5CeWFXNTBJaXdpWW1GeUlpd2ljMmhsWlhSeklpd2ljMmhoY0dWeklpd2ljMlZ5ZG1WeUlsMTk='
);

// Set extensions
jspreadsheet.setExtensions({ parser });

// Create the spreadsheet from a local file
const load = function (e, spreadsheet) {
  // Parse XLSX file and create a new spreadsheet
  jspreadsheet.parser({
    file: e.target.files[0],
    // It would be used to updated the formats only
    locale: 'en-GB',
    onload: function (config) {
      jspreadsheet(spreadsheet.current, {
        ...config,
        filters: true,
        toolbar: true,
      });
    },
    onerror: function (error) {
      alert(error);
    },
  });
};
const JSpreadsheetComponent = () => {
  // Spreadsheet array of worksheets
  const spreadsheet = useRef();
  // Render component
  return (
    <div>
      <div ref={spreadsheet}></div>
      <input
        type={'file'}
        name={'file'}
        id={'file'}
        onChange={(e) => load(e, spreadsheet)}
        style={{ display: 'none' }}
      />
      <input
        type={'button'}
        value={'Load a XLSX file from my local computer'}
        onClick={() => document.getElementById('file').click()}
      />
    </div>
  );
};

export default JSpreadsheetComponent;
