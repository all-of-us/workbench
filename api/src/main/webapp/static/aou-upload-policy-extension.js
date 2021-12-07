// TODO(calbach): Remove this file 2w after aou-file-tree-policy-extension.js is live.
// This file is being kept so that old runtimes can continue to reference it.

// Workbench "Upload policy" extension, to show the policy before letting user upload any file

define([
  'base/js/namespace'
], (Jupyter) => {

  const load_upload_extension = () => {
  // This will open a dialog box when the user clicks Upload Button on the Jupyter File List page.
  // File will be uploaded only if the user clicks OK else it will just close the dialog box and do nothing.

  $("#notebook_list_info input").click(function() {
    return confirm("It is All of Us data use policy to not upload data or files containing " +
        "personally identifiable information (PII). Any external data, files, or software that " +
        "is uploaded into the Workspace should be exclusively for the research purpose that was " +
        "provided for this Workspace.");
  });
 };

  return {
    'load_ipython_extension': load_upload_extension
  };
});
