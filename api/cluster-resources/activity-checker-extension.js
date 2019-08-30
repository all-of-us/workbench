// Sends a message to the parent frame every second that user activity is detected

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

  ['mousemove', 'mousedown', 'keypress', 'scroll', 'click'].forEach(eventName => {
    window.addEventListener(eventName, () => signalUserActivity(), false);
  });

    console.log("Loaded Activity Tracker");
  };

  return {
    'load_ipython_extension': load
  };
});
