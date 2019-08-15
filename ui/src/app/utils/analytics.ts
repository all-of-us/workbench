declare let gtag: Function;

/**
 * Triggers a Google Analytics event.
 *
 * For guidance on how to use action names, categories, and labels effectively,
 * see https://support.google.com/analytics/answer/1033068#Anatomy.
 */
export function triggerEvent(
  category: string, action: string, label?: string, value?: number,
  nonInteraction: Boolean = false) {
  if (window['gtag']) {
    gtag('event', action, {
      'event_category': category,
      'event_label': label,
      'event_value': value,
      'non_interaction': nonInteraction
    });
  } else {
    console.error('Google Analytics gtag.js has not been loaded');
  }
}
