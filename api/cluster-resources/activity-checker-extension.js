// Workbench "Download policy" extension, to show the policy before letting user download the notebook

define([
    'base/js/namespace'
], (Jupyter) => {

  function debouncer(action, sensitivityMs) {
    var t = Date.now();

    setInterval(() => {
      if (Date.now() - t < sensitivityMs) {
        action();
      }
    }, sensitivityMs);

    return () => {
      t = Date.now();
    }
  }

  const load = () => {
    const signalUserActivity = debouncer(() => {
      window.parent.postMessage("Frame is active", '*');
    }, 1000);

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
