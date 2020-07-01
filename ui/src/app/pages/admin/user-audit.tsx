import {profileApi} from 'app/services/swagger-fetch-clients';
import {
  actionToString,
  agentToString,
  headerToString,
  targetToString
} from 'app/utils/audit-utils';
import * as fp from 'lodash/fp';
import * as React from 'react';
import {
  AuditAction,
  AuditEventBundle,
  AuditEventBundleHeader, AuditTargetPropertyChange
} from '../../../generated';

const {useEffect, useState} = React;
const AUDIT_TABLE_STYLE = {
  border: 'border:1px red solid',
  backgroundColor: 'light-grey',
  align: 'left'
} as React.CSSProperties;

const PropertyChangeRow = (props: {propertyChange: AuditTargetPropertyChange}) => {
  const {propertyChange} = props;
  return (<React.Fragment>
    <tr key = {propertyChange.targetProperty}>
      <td>{propertyChange.targetProperty || '--'}</td>
      <td>{propertyChange.previousValue || '--'}</td>
      <td>{propertyChange.newValue || '--'}</td>
    </tr>
  </React.Fragment>);
};

const PropertyChangeListView = (props: {propertyChanges: AuditTargetPropertyChange[]}) => {
  const {propertyChanges} = props;
  return (
      propertyChanges.length > 0 ?
      <React.Fragment>
        <table style={AUDIT_TABLE_STYLE}>
            <thead>
              <tr>
                <th>Property</th>
                <th>Previous Value</th>
                <th>New Value</th>
              </tr>
            </thead>
          <tbody>
            {propertyChanges.map((propertyChange) =>
               <PropertyChangeRow propertyChange={propertyChange}/>
            )}
        </tbody>
      </table>
      </React.Fragment> :
      null);
};

const AuditEventBundleHeaderView = (props: {header: AuditEventBundleHeader}) => {
  const {header} = props;
  return (
      <table>
        <thead>
        <tr><th>{header.actionType} Action</th></tr>
        </thead>
        <tbody>
          <tr>
            <td>{header.agent.agentType} Agent</td><td>{agentToString(header.agent)}</td>
          </tr>
          <tr>
            <td>{header.target.targetType} Target</td><td>{targetToString(header.target)}</td>
          </tr>
        </tbody>
      </table>
  );
};

const EventBundleView = (props: {eventBundle: AuditEventBundle}) => {
  const {eventBundle} = props;
  return (
      <table>
        <thead>
        <tr>
          <th>
            <AuditEventBundleHeaderView header={eventBundle.header} />
          </th>
        </tr>
        </thead>
        <tbody>
          <tr>
            <PropertyChangeListView propertyChanges={eventBundle.propertyChanges}/>
          </tr>
        </tbody>
      </table>
  );
};

const AuditActionCard = (props: {action: AuditAction}) => {
  const {action} = props;
  // Something in the codegen is wonky here. the actionTime field is typed as a Date,
  // but turns out to be a number for some reason here. In other contexts it appears
  // to format itself happily though.
  const time = new Date(action.actionTime).toISOString();
  return (
      <table style={AUDIT_TABLE_STYLE}>
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
  const {actions} = props;
  const timesMillis = actions.map(action => new Date(action.actionTime).getTime());
  const minTime = new Date(Math.min(...timesMillis)).toDateString();
  const maxTime = new Date(Math.max(...timesMillis)).toDateString();
  return (
      <table style={AUDIT_TABLE_STYLE}>
        <thead>
          <tr><th>{actions.length} Audit Actions from {minTime} to {maxTime}</th></tr>
        </thead>
        <tbody>
          {actions.map(action => (
              <tr key={action.actionId}>
                <td><AuditActionCard action={action}/></td>
              </tr>
          ))}
        </tbody>
      </table>
  );
};

export const UserAudit = () => {
  const [userActions, setUserActions] = useState();
  const usernameWithoutGsuiteDomain = 'jaycarlton';

  useEffect(() => {
    const getLogEntries = async() => {
      const limit = 50;
      const {actions, query} = await profileApi().getAuditLogEntries(usernameWithoutGsuiteDomain, limit);
      console.log(query); // dont' think limit is working
      const renderedString = actions.map(action => actionToString(action)).join('\n');
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

// TODO for MVP
// route parameterized on username in admin section (with approprriate guards)
// fix page title (currently undefined)
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
