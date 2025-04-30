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

    // Function to check if user affirmed the download policy and stop propagation if not
    function handlePolicyAffirmation(event) {
      const affirmed = downloadPolicyPopUp();
      if (!affirmed) {
        event.stopPropagation();
      }
    }

    // notebook-list.js in the Jupyter UI serves the download and view button by using it's own
    // jQuery click handler. We have no way of ensuring that our click handler is ahead
    // of it without digging into jQuery internals, however using an event listener with event capturing
    // seems to work.
    document.querySelector('.download-button').addEventListener('click', function(e) {
      handlePolicyAffirmation(e);
    }, true);

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

    const viewableFileExtension = 'ipynb';
    // Clicking the view button will
    // 1) Open All Jupyter, text or shell files in a new tab.
    // 2) Display a download policy prompt for all other files as they will be downloaded
    document.querySelector('.dynamic-buttons .view-button').addEventListener('click', function(e) {
      var openInANewTab = true;
      $('#notebook_list input:checked').each(function() {
        // Find the selected file name
        const elem = $(this).siblings().find($('.item_link .item_name'));
        const fileName = elem.text();
        var fileExt = fileName.substring( (fileName.lastIndexOf('.') +1) );
        if (viewableFileExtension !== fileExt) {
          openInANewTab = false;
        }
      });
      // Only show download dialog if some of the selected Files
      // are not in the viewable file extensions list.
      if(!openInANewTab) {
        handlePolicyAffirmation(e);
      }
    }, true);
  };

  return {
    'load_ipython_extension': load_upload_extension
  };
});
