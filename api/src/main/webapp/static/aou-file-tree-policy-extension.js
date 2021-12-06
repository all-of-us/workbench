// Workbench policy extension, to show the data use policy before letting user
// upload or download files.

define([
  'base/js/namespace'
], (Jupyter) => {

  const load_upload_extension = () => {
  // This will open a dialog box when the user clicks Upload Button on the Jupyter File List page.
  // File will be uploaded only if the user clicks OK else it will just close the dialog box and do nothing.

    $('#notebook_list_info input').click(() => confirm(
        'It is All of Us data use policy to not upload data or files containing ' +
        'personally identifiable information (PII). Any external data, files, or software that ' +
        'is uploaded into the Workspace should be exclusively for the research purpose that was ' +
        'provided for this Workspace.'));

    // notebook-list.js in the Jupyter UI serves the download by using it's own
    // jQuery click handler. We have no way of ordering our click handler ahead
    // of it without digging into jQuery internals. Instead, we insert a span
    // on top of the button to gain click priority.
    $('.dynamic-buttons .download-button').empty().append(
        '<span id="aou-download-button-overlay">Download</span>');
    $('#aou-download-button-overlay').click(() => confirm(
        'The All of Us Data Use Policies prohibit you from removing participant-level data from ' +
        'the workbench. You are also prohibited from publishing or otherwise distributing any data ' +
        'or aggregate statistics corresponding to fewer than 20 participants unless ' +
        'expressly permitted by our data use policies. By continuing, you affirm that this ' +
        'download will be used in accordance with the All of Us data use policy.'));
  };

  return {
    'load_ipython_extension': load_upload_extension
  };
});
