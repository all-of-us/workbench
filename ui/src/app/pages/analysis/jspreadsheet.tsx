import React, { useEffect, useRef, useState } from 'react';

import parser from '@jspreadsheet/parser';
import { jspreadsheet, Spreadsheet, Worksheet } from '@jspreadsheet/react';

import '/node_modules/jspreadsheet/dist/jspreadsheet.css';
import '/node_modules/jsuites/dist/jsuites.css';

// Set license
jspreadsheet.setLicense(
  'NTYwMmQ2M2QzNGIyZmVmNmMxY2U0YzI2OTRkMjgyMGM2YTYzNTNjOGI5NTdkYzkxMmM5OTdjODAyYzVjYzg5ZmIyZjk4NWQ3NzYzMGU3MDExNmI2ODhjZGM2OWVlZGZkNGM0YzZlMzI3ZjlhODZiMzFlOTA3OWYzNmYzMjAxNGEsZXlKamJHbGxiblJKWkNJNklqUXdaalExT1RFd1pqRTNOV1ZsT0RVek1URXlOR0kzWlRVNE9Ea3pOVGhsTldKallXRXpPRFFpTENKdVlXMWxJam9pUlhKcFl5SXNJbVJoZEdVaU9qRTNNVGt3TVRBNE1EQXNJbVJ2YldGcGJpSTZXeUozWldJaUxDSnNiMk5oYkdodmMzUWlYU3dpY0d4aGJpSTZNekVzSW5OamIzQmxJanBiSW5ZM0lpd2lkamdpTENKMk9TSXNJbll4TUNJc0luWXhNU0lzSW1admNtMTFiR0VpTENKbWIzSnRjeUlzSW5KbGJtUmxjaUlzSW5CaGNuTmxjaUlzSW1sdGNHOXlkR1Z5SWl3aWMyVmhjbU5vSWl3aVkyOXRiV1Z1ZEhNaUxDSjJZV3hwWkdGMGFXOXVjeUlzSW1Ob1lYSjBjeUlzSW5CeWFXNTBJaXdpWW1GeUlpd2ljMmhsWlhSeklpd2ljMmhoY0dWeklpd2ljMlZ5ZG1WeUlsMTk='
);

// Set extensions
jspreadsheet.setExtensions({ parser });

// Create the spreadsheet from a local file
const JSpreadsheetComponent = () => {
  const [message, setMessage] = useState('');
  const [blob, setBlob] = useState(null);
  // setMessage('JSpreadsheetComponent');
  // Spreadsheet array of worksheets
  const spreadsheet = useRef();

  const handleFileChange = (e) => {
    const file = e.target.files[0];
    const reader = new FileReader();

    reader.onload = function (event) {
      setMessage('FileReader onload called'); // Added console.log
      setBlob(event.target.result);
    };

    reader.onerror = function (error) {
      setMessage(`FileReader onerror called, ${error}`); // Added console.log
    };

    reader.readAsArrayBuffer(file);
  };

  useEffect(() => {
    if (blob) {
      setMessage('blob is set'); // Added console.log
      jspreadsheet.parser({
        file: blob,
        onload: function (config) {
          setMessage('jspreadsheet.parser onload called'); // Added console.log
          jspreadsheet(spreadsheet.current, {
            ...config,
            filters: true,
            toolbar: true,
          });
        },
        onerror: function (error) {
          setMessage(`jspreadsheet.parser onerror called, ${error}`); // Added console.log
          console.error(error);
        },
      });
    }
  }, [blob]);

  // Render component
  return (
    <div>
      <div ref={spreadsheet}></div>
      <div>{message}</div>
      <input type='file' onChange={handleFileChange} />
    </div>
  );
};

export default JSpreadsheetComponent;
