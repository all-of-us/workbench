// Workbench "Playground Mode" extension. In "Playground Mode", changes are not
// saved back to GCS. This extension makes minor UI tweaks to differentiate this
// mode from normal Workbench Jupyter usage. For now, this also removes/hides
// save controls. Technically one can still save changes locally (and changes
// are automatically saved periodically), but as a first pass we just want to
// avoid having to explain this nuance.

define([
    'base/js/namespace'
], (Jupyter) => {
  const load = () => {
    // This prefix must be kept in sync with the Workbench localization API,
    // see https://github.com/all-of-us/workbench/blob/master/api/src/main/java/org/pmiops/workbench/api/ClusterController.java
    const nbPath = Jupyter.notebook.notebook_path;
    if (!nbPath.startsWith('workspaces_playground/')) {
      return;
    }

    // Disable UI controls/notifications relating to saving.

    // "notbook" is an intentional typo to match Jupyter UI HTML.
    $('#save-notbook').remove();
    $('#save_notebook_as').remove();
    $('#save_checkpoint').remove();
    $('#menubar-container')
        .append(
            '<style>' +
              '.autosave_status { display: none; }' +
              '#playground-mode { background-color: #FFFFB2; }' +
              '</style>');

    // Add our own persistent "Playground mode" notification next to the other
    // notifications, e.g. kernel status.
    $('#notification_area').prepend(
        '<div id="playground-mode" class="notification_widget btn btn-xs navbar-btn">' +
          '<span>Playground mode - changes will not be saved</span>' +
          '</div>');
  };

  return {
    'load_ipython_extension': load
  };
});
