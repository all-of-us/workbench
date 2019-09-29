// Sends a message to the parent frame every second that user activity is detected

define([
    'base/js/namespace'
], (Jupyter) => {

  // Should have the same implementation as debouncer in app/utils/index.tsx
  function debouncer(action, sensitivityMs) {
    let t = Date.now();

    const timer = setInterval(() => {
      if (Date.now() - t < sensitivityMs) {
        action();
      }
    }, sensitivityMs);

    return {
      invoke: () => {
        t = Date.now();
      },
      getTimer: () => timer
    }
  }

  const load = () => {
    const signalUserActivity = debouncer(() => {
      // USER_ACTIVITY_DETECTED should be in sync with INACTIVITY_CONFIG.MESSAGE_KEY from signed-in/component.ts
      // Setting targetOrigin to * means that it may be possible for other domains to snoop on this payload
      // We're going to have to have separate versions of this file uploaded to GCS if want to specify the
      // domain. I decided it wasn't worth the effort at this time since the payload doesn't have to be secure
      window.parent.postMessage('USER_ACTIVITY_DETECTED', '*');
    }, 1000);

    // Events array should be in sync with INACTIVITY_CONFIG.TRACKED_EVENTS from signed-in/component.ts
    ['mousedown', 'keypress', 'scroll', 'click'].forEach(eventName => {
      window.addEventListener(eventName, () => signalUserActivity.invoke(), false);
    });

    console.log("Loaded Activity Tracker");
  };

  return {
    'load_ipython_extension': load
  };
});
