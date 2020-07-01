import {profileApi} from 'app/services/swagger-fetch-clients';
import * as fp from 'lodash/fp';
import * as React from 'react';
import {UserAuditLogQueryResponse} from '../../../generated';
import {AuditActionCard} from '../audit/audit-action-card';
import {actionToString} from 'app/utils/audit-utils';

const {useEffect, useState} = React;

// const firstTen = fp.flow(fp.slice(0, 10), fp.map(JSON.stringify));
const firstTen = (res: UserAuditLogQueryResponse) => {
  return JSON.stringify(res.actions);
};

// fp.flow(fp.slice(0, 10), fp.map(JSON.stringify));
export const UserAudit = () => {
  const [userActions, setUserActions] = useState();

  useEffect(() => {
    const getLogEntries = async() => {
      const usernameWithoutGsuiteDomain = 'jaycarlton';
      const limit = 50;
      const {actions} = await profileApi().getAuditLogEntries(usernameWithoutGsuiteDomain, limit);
      const renderedString = actions.map(action => actionToString(action)).join('<br/>');
      console.log(renderedString);
      console.log(actions);
      // setUserActions(actions);
      setUserActions(renderedString);
    };

    getLogEntries();
  }, []);

  // return userActions ? <div>{firstTen(userActions)}</div> : <div>Loading Log Entries...</div>;
  return userActions ? <div>{userActions}</div> : <div>Loading Log Entries...</div>;

  // return <AuditActionCard action={actions[0]}/>;
};

// Status:
// Done:
// - hard-coded route to user-audit
// component for user audit page
// connection to profile API & imports
// downloading N at a time
// stringifiers for AuditAgent, AuditTarget, ...AuditAction
// verification in console
// very rorugh rendering of strirngs

// TODO for MVP
// route parameterized on username in admin section (with approprriate guards)
// CardListView component to hold cards in vertically scrollable container
// AuditActionCard: card for an individual action
//   - (common action ID & Date, i.e. a single AuditAction)
//   - Shows date & time of action
//   - expose prop for actionId for debugging (not rendered)
//   - holds one or more EventBundleCards
// EventBundleCard: card showing a single event bundle, which is a tuple of
//   {ActionType, Agent, Target} at a constant time and ActionId
//   - header info: can use headerToString() to start
//   - zero or more AuditPropertyChangeCards
// AuditPropertyChangeCard: shows property name and old and new values
//   - can use propertyChangeToString()  to start
//   - will likely want to specialize for cases of new property & deleted property (or no values at all)
//   - we can   punt on this if  we have to and use a multi-line string, but I haven't found
//     that to bev easeir so far.
// basic styling: just enough to tell the cards apart. BG color is one approach

// TODO (extra credit)
// - make all cards in the CardListView  collapsible to a summary, e.g. time + a list of action types
// - make the list of AuditPropertyChangeCards collapsible locally (expand/collapse all would  be nice too)
// - add a filter on  ActionType to all the cards in the view. All selected actions must be present
//   in a card for it to render. I.e. we don't want to hide individual EventBundleCards I dont' think.
// - pagination based on the `before` property in the API.
//     * first fetch the 20 (say) most recent actions (limit = 20, after = null,  before = null)
//     * Take timeestamp of the eoldest card reeturned, set `before` to that value and call again
//     - repeat until no results

