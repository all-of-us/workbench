// Workbench "Download policy" extension, to show the policy before letting user download the notebook

define([
    'base/js/namespace'
], (Jupyter) => {
  const load = () => {

    console.log("HELLO FROM OUTER SPACE");
  };

  return {
    'load_ipython_extension': load
  };
});
