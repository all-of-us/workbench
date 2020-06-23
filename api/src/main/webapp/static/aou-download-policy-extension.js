// Workbench "Download policy" extension, to show the policy before letting user download the notebook

define([
    'base/js/namespace'
], (Jupyter) => {
  const load = () => {
    // This will open a dialog box when the user clicks any one of the Download As option.
    // File will be downloaded only if the user clicks OK else it will just close the dialog box and do nothing.
    $('#download_menu li a').click(function(e) {
      e.preventDefault();
      return confirm("Data Download Reminder" + "\n\n" +
        "You are prohibited from taking screenshots or attempting in any way to remove participant-level data from the workbench.\n\n" +
        "You are also prohibited from publishing or otherwise distributing any data or aggregate statistics corresponding to fewer than 20 participants unless expressly permitted by our data use policies.\n\n" +
        "For more information, please see our Data Use Policies by visiting https://www.researchallofus.org/data-use-policies/.");
    });
  };

  return {
    'load_ipython_extension': load
  };
});
