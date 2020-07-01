import {profileApi} from 'app/services/swagger-fetch-clients';
import * as fp from 'lodash/fp';
import * as React from 'react';
import {
  AuditAction,
  AuditEventBundle,
  AuditEventBundleHeader,
  UserAuditLogQueryResponse
} from '../../../generated';
import {actionToString, agentToString, headerToString, targetToString} from 'app/utils/audit-utils';

const {useEffect, useState} = React;

const AuditEventBundleHeaderView = (props: {header: AuditEventBundleHeader}) => {
  const {header} = props;
  return (
      // <dl>
      //   <dt key="actionType">Action Type</dt>
      //   <dd>{header.actionType}</dd>
      //   <dt key="agent">Agent</dt>
      //   <dd>{agentToString(header.agent)}</dd>
      //   <dt key="target">Target</dt>
      //   <dd>{targetToString(header.target)}</dd>
      // </dl>
      <table>
        <thead>
        <tr><th>{header.actionType}</th></tr>
        </thead>
        <tbody>
          <tr>
            <td>Agent</td><td>{agentToString(header.agent)}</td>
          </tr>
          <tr>
            <td>Target</td><td>{targetToString(header.target)}</td>
          </tr>
        </tbody>
      </table>
  );
};

const EventBundleView = (props: {eventBundle: AuditEventBundle}) => {
  const {eventBundle} = props;
  return <AuditEventBundleHeaderView header={eventBundle.header} />;
};

const AuditActionCard = (props: {action: AuditAction}) => {
  const {action} = props;
  // Something in the codegen is wonky here. the actionTime field is typed as a Date,
  // but turns out to be a number for some reason here. In other contexts it appears
  // to format itself happily though.
  const time = new Date(action.actionTime).toISOString();
  return (
      <table>
        <thead>
        <tr><th>{time}: {action.eventBundles.length} events</th></tr>
        </thead>
        <tbody>
        {action.eventBundles.map(eventBundle =>
            <tr key={headerToString(eventBundle.header)}><td><EventBundleView eventBundle={eventBundle}/></td></tr>
        )}
        </tbody>
      </table>
  );
};

const AuditActionCardListView = (props: {actions: AuditAction[]}) => {
  return (
      <dl>
        {props.actions.map(action => (
            <React.Fragment key={action.actionId}>
              <dt>Action ID: {action.actionId}</dt>
              <dd><AuditActionCard action={action}/></dd>
            </React.Fragment>
        ))}
      </dl>
  );

};

// fp.flow(fp.slice(0, 10), fp.map(JSON.stringify));
export const UserAudit = () => {
  const [userActions, setUserActions] = useState();
  const usernameWithoutGsuiteDomain = 'jaycarlton';

  useEffect(() => {
    const getLogEntries = async() => {
      const limit = 50;
      const {actions} = await profileApi().getAuditLogEntries(usernameWithoutGsuiteDomain, limit);
      const renderedString = actions.map(action => actionToString(action)).join('<br/>');
      console.log(renderedString);
      console.log(actions);
      setUserActions(actions);
    };

    getLogEntries();
  }, []);

  return userActions ?
      <AuditActionCardListView actions={userActions}/> :
      <div>Loading Audit Actions for user {usernameWithoutGsuiteDomain}...</div>;
};

// Status:
// Done:
// - hard-coded route to user-audit
// component for user audit page
// connection to profile API & imports
// downloading N at a time
// stringifiers for AuditAgent, AuditTarget, ...AuditAction
// verification in console
// very rough rendering of strings

// TODO for MVP
// route parameterized on username in admin section (with approprriate guards)
// fix page title (currently undefined)
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
// - make a generic CardListView
// - use common card style stuff like React.FunctionComponent<WorkspaceCardMenuProps>
