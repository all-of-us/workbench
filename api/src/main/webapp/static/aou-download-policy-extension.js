// Workbench "Download policy" extension, to show the policy before letting user download the notebook

define([
    'base/js/namespace'
], (Jupyter) => {
  const load = () => {
    // This will open a dialog box when the user clicks any one of the Download As option.
    // File will be downloaded only if the user clicks OK else it will just close the dialog box and do nothing.
    $('#download_menu li a').click(function(e) {
      e.preventDefault();
      return confirm("Policy Reminder" + "\n\n" +
        "It is All of Us data use policy that researchers should not make copies of " +
        "or download individual-level data (including taking screenshots or other means of viewing " +
        "individual-level data) outside of the All of Us research environment without approval from " +
        "All of Us Resource Access Board. So, please make sure that the output cells in the notebook " +
        "you are downloading do not contain individual-level data.");
    });
  };

  return {
    'load_ipython_extension': load
  };
});
