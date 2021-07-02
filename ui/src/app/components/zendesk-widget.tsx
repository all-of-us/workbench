import {zendeskWidgetKey} from 'app/utils/zendesk';
import * as React from 'react';

const {useEffect} = React;

const zendeskSettingsGlobal = 'zESettings';

export const ZendeskWidget = () => {
  useEffect(() => {
    // This external script loads the Zendesk web widget, connected to our
    // production Zendesk account.
    const s = document.createElement('script');
    s.type = 'text/javascript';
    s.id = 'ze-snippet';
    s.src = 'https://static.zdassets.com/ekr/snippet.js?key=' + zendeskWidgetKey();
    document.body.appendChild(s);

    // This data configures the Zendesk web widget with settings to show only
    // the "contact us" form. See https://developer.zendesk.com/embeddables/docs/widget/
    // for API docs.
    window[zendeskSettingsGlobal] = {
      webWidget: {
        chat: {
          suppress: true,
        },
        color: {
          // This is an AoU dark purple color.
          theme: '#262262'
        },
        contactForm: {
          attachments: true,
          subject: false,
          // We include a tag indicating that this support request was filed via the
          // Researcher Workbench. This helps distinguish from tickets filed via other
          // AoU sub-products, e.g. Research Hub or Data Browser.
          tags: ['research_workbench'],
          title: {
            '*': 'Help Desk',
          },
        },
        helpCenter: {
          suppress: true,
        },
        talk: {
          suppress: true,
        },
      }
    };
    () => {
      delete window[zendeskSettingsGlobal];
      s.remove();
    };
  }, []);
  return <React.Fragment />;
}
