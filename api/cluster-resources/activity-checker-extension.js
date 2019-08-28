// Workbench "Download policy" extension, to show the policy before letting user download the notebook

define([
    'base/js/namespace'
], (Jupyter) => {

  function activityReporter(report) {
    var t = Date.now();

    setInterval(() => {
      if (Date.now() - t < 1000) {
      report();
    }
  }, 1000);

    return () => {
      t = Date.now();
    }
  }

  const load = () => {
    const signalUserActivity = activityReporter(() => {
      window.parent.postMessage("Frame is active", '*');
    });

    window.addEventListener('mousemove', () => signalUserActivity(), false);
    window.addEventListener('mousedown', () => signalUserActivity(), false);
    window.addEventListener('keypress', () => signalUserActivity(), false);
    window.addEventListener('scroll', () => signalUserActivity(), false);
    window.addEventListener('click', () => signalUserActivity(), false);

    console.log("Loaded Activity Tracker");
  };

  return {
    'load_ipython_extension': load
  };
});
