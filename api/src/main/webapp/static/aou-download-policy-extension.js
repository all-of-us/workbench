// Workbench "Download policy" extension, to show the policy before letting user download the notebook

define([
    'jquery',
    'base/js/dialog',
    'base/js/namespace'
], ($, dialogLib, Jupyter) => {
  const policyUrl = 'https://www.researchallofus.org/data-tools/data-access/';

  const load = () => {
    $('#download_menu li a').click((e) => {
      e.preventDefault();

      // Mirrors logic from here; the element id encodes the target format:
      // https://github.com/jupyter/notebook/blob/8a4cbd0ad91371392b90672afea58560d37c847e/notebook/static/notebook/js/menubar.js#L197
      const targetFmt = e.target.parentElement.getAttribute('id').substring(9);

      // We reference the dialog element in the body callbacks, so declare here
      // for the below closures.
      let dialog;

      // The following dialog code is modeled off of the Jupyter UI codebase. This is used over a
      // standard alert dialog in order to support the required checkbox and links. In particular,
      // the notebook rename modal was used for reference:
      // https://github.com/jupyter/notebook/blob/8a4cbd0ad91371392b90672afea58560d37c847e/notebook/static/notebook/js/savewidget.js#L74-L132
      const dialogBody = $('<div/>')
          .append(
              $(`<p>Our <a href="${policyUrl}" target="_blank">Data Use Policies</a> `+
                'prohibit you from removing participant-level data from the workbench. You ' +
                'are also prohibited from publishing or otherwise distributing any data or ' +
                'aggregate statistics corresponding to fewer than 20 participants unless ' +
                'expressly permitted by our data use policies.</p>'))
          .append($("<br/>"))
          .append(
              $('<input/>')
                .attr('type','checkbox')
                .attr('id','confirm-policy')
                .css('display', 'inline')
                .change((e) => {
                  dialog.find('#aou-download').prop('disabled', !e.target.checked);
                }))
          .append(
            $('<label>I understand the data use policies and certify that this download will be ' +
              `used in accordance with <a href="${policyUrl}" target="_blank"><i>All of Us</i> Data Use ` +
              'Policies</a>.</label>')
              .attr('for','confirm-policy')
              .css('display', 'inline')
              .css('margin-left', '5px'));
      dialog = dialogLib.modal({
        title: 'Data Download',
        body: dialogBody,
        default_button: "Cancel",
        buttons : {
          "Cancel": {
            class: "btn-primary",
          },
          "Download": {
            id: 'aou-download',
            click: () => {
              // See https://github.com/jupyter/notebook/blob/8a4cbd0ad91371392b90672afea58560d37c847e/notebook/static/notebook/js/menubar.js#L197
              Jupyter.menubar._nbconvert(targetFmt, true);
              dialog.modal('hide');
            }
          }
        },
        open : () => {
          dialog.find('#aou-download').prop('disabled', true);
        }
      });
      return false;
    });
  };

  return {
    'load_ipython_extension': load
  };
});
