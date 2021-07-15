import {environment} from 'environments/environment';
import {ZendeskEnv} from 'environments/environment-type';

interface ZendeskUrls {
  billing: string;
  communityForum: string;
  createBillingAccount: string;
  dataDictionary: string;
  faq: string;
  gettingStarted: string;
  helpCenter: string;
  tableOfContents: string;
  researchPurpose: string;
}

const zendeskConfigs = {
  [ZendeskEnv.Prod]: {
    baseUrl: 'https://aousupporthelp.zendesk.com/hc',
    widgetKey: '5a7d70b9-37f9-443b-8d0e-c3bd3c2a55e3'
  },
  [ZendeskEnv.Preprod]: {
    baseUrl: 'https://aoupreprodsupporthelp.zendesk.com/hc',
    widgetKey: '41815bdd-7e8f-4450-aadf-dd5957093233'
  },
  [ZendeskEnv.Sandbox]: {
    baseUrl: 'https://aousupporthelp1580753096.zendesk.com/hc',
    widgetKey: 'df0a2e39-f8a8-482b-baf5-af82e14d38f9'
  }
};

export const zendeskWidgetKey = () => {
  return zendeskConfigs[environment.zendeskEnv].widgetKey;
};

/**
 * A set of support URLs for the current environment. These need to be
 * parameterized per environment since the Zendesk support content IDs are not
 * stable across the prod, preprod  and sandbox Zendesk instances.
 */
export const supportUrls: ZendeskUrls = ((env) => {
  const baseUrl = zendeskConfigs[env].baseUrl;

  const article = (id) => `${baseUrl}/articles/${id}`;
  const category = (id) => `${baseUrl}/categories/${id}`;
  const section = (id) => `${baseUrl}/sections/${id}`;
  const commonUrls = {
    // This link in particular needs the "en-us" infix. The other urls will
    // redirect according to detected locale, which is preferred.
    communityForum: `${baseUrl}/en-us/community/topics`,
    helpCenter: baseUrl
  };
  const urls: {[key: string]: ZendeskUrls} = {
    [ZendeskEnv.Prod]: {
      ...commonUrls,
      billing: section('360008099991'),
      createBillingAccount: article('360039539411'),
      dataDictionary: article('360033200232'),
      faq: category('360002157532'),
      gettingStarted: category('360002157352'),
      tableOfContents: category('360002625291'),
      researchPurpose: article('360042673211'),
    },
    [ZendeskEnv.Preprod]: {
      ...commonUrls,
      billing: section('360012893532'),
      createBillingAccount: article('360060301171'),
      dataDictionary: article('360058949792'),
      faq: category('360005877792'),
      gettingStarted: category('360005884571'),
      tableOfContents: category('360005884591'),
      researchPurpose: article('360058861612'),
    },
    [ZendeskEnv.Sandbox]: {
      ...commonUrls,
      billing: section('360009142932'),
      createBillingAccount: article('360044792211'),
      dataDictionary: article('360044793611'),
      faq: category('360003453651'),
      gettingStarted: category('360003430652'),
      tableOfContents: category('360003430672'),
      researchPurpose: article('360044334652'),
    }
  };
  return urls[env];
})(environment.zendeskEnv);

// ideally this initialization would be done by including <script> tags in
// the component template html file.  However, Angular does not allow this.
// TODO: investigate whether this will be possible after we migrate to React

export function openZendeskWidget(
  givenName: string, familyName: string, aouEmailAddress: string,
  contactEmailAddress: string): void {
  // Note: we're string-protecting our access of the 'zE' property, since
  // this property is dynamically loaded by the Zendesk web widget snippet,
  // and can't properly be typed. If for some reason the support widget is
  // unavailable, we'll show a notice to the user.
  if (window['zE'] == null) {
    // Show an error message to the user, asking them to reload or contact
    // support via email.
    console.error('Error loading Zendesk widget');
    return;
  }

  // In theory, this webWidget call should identify the user who is filing
  // this support request.
  //
  // In practice, the values provided via 'identify' don't seem to do much.
  // Zendesk uses values from the 'prefill' action to assign the user email
  // in the created ticket, which means that for AoU these tickets won't be
  // correctly associated with the researcher's AoU Google Account. See
  // the Zendesk integration doc for more discussion.
  window['zE']('webWidget', 'identify', {
    name: `${givenName} ${familyName}`,
    email: aouEmailAddress,
  });

  window['zE']('webWidget', 'prefill', {
    name: {
      value: `${givenName} ${familyName}`,
      readOnly: true,
    },
    // For the contact email we use the user's *contact* email address,
    // since the AoU email address is not a valid email inbox.
    email: {
      value: contactEmailAddress,
      readOnly: true,
    },
  });

  // Trigger the widget to open the full contact form (instead of the
  // help icon, which is the starting state).
  window['zE']('webWidget', 'open');
}
