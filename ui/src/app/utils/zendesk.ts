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
