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

    // notebook-list.js in the Jupyter UI serves the download and view button by using it's own
    // jQuery click handler. We have no way of ordering our click handler ahead
    // of it without digging into jQuery internals. Instead, we insert a span
    // on top of the button to gain click priority.
    $('.dynamic-buttons .download-button').empty().append(
        '<span id="aou-download-button-overlay">Download</span>');
    $('.dynamic-buttons .view-button').empty().append(
        '<span id="aou-view-button-overlay">View</span>');


    function downloadPolicyPopUp() {
      var affirm =  prompt(
          'The All of Us Data Use Policies prohibit you from removing participant-level data from ' +
          'the workbench. You are also prohibited from publishing or otherwise distributing any data ' +
          'or aggregate statistics corresponding to fewer than 20 participants unless ' +
          'expressly permitted by our data use policies.\n\n' +
          'To continue, affirm that this download will be used in accordance with the All of Us ' +
          'data use policy by typing "affirm" below.');
      return !!affirm && 'affirm' === affirm.replace(/\W/g, '').toLowerCase()
    }


    $('#aou-download-button-overlay').click(() => {
      return downloadPolicyPopUp();
    });

    const viewableFileExtenstions = ['ipynb', 'txt', 'sh'];
    // Clicking the view button will
    // 1) Open All Jupyter, text or shell files in a new tab.
    // 2) Display a download policy prompt for all other files as they will be downloaded
    $('#aou-view-button-overlay').click(() => {
      var openInANewTab = true;
      $('#notebook_list input:checked').each(function() {
        // Find the selected file name
        const elem = $(this).siblings().find($('.item_link .item_name'));
        const fileName = elem.text();
        var fileExt = fileName.substring( (fileName.lastIndexOf('.') +1) );
        if (viewableFileExtenstions.indexOf(fileExt) === -1) {
          openInANewTab = false;
          return false;
        }
       });
      // If all selected Files are files that open in a new tab (ipynb, txt or sh), return now
      if (!!openInANewTab) {
        return true;
      }

      return downloadPolicyPopUp();
    });
  };

  return {
    'load_ipython_extension': load_upload_extension
  };
});
