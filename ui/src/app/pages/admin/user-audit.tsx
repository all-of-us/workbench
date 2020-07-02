import {Navigate} from 'app/components/app-router';
import {Button} from 'app/components/buttons';
import {ActionAuditCardBase} from 'app/components/card';
import {TextInput} from 'app/components/inputs';
import {profileApi} from 'app/services/swagger-fetch-clients';
import {
  actionToString,
  agentToString,
  headerToString,
  targetToString
} from 'app/utils/audit-utils';
import * as fp from 'lodash/fp';
import { cpuUsage } from 'process';
import * as React from 'react';
import {useParams} from 'react-router-dom';
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


const PropChanges = props => {
  const {targetProperty, previousValue, newValue} = props;
  return <React.Fragment>
    <div>{targetProperty || '--'}</div>
    <div>{previousValue || '--'}</div>
    <div>{newValue || '--'}</div>
  </React.Fragment>;
};

const PropertyChangeListView = (props: {propertyChanges: AuditTargetPropertyChange[]}) => {
  const {propertyChanges} = props;

  return <div style={{margin: '0.25rem 0 0rem 1rem', display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)'}}>
    <div style={{fontWeight: 600}}>Property</div>
    <div style={{fontWeight: 600}}>Previous Value</div>
    <div style={{fontWeight: 600}}>New Value</div>
    {propertyChanges.map((changes, index) => <PropChanges {...changes} key={index}/>)}
  </div>;
};

const AuditEventBundleHeaderView = (props: {header: AuditEventBundleHeader}) => {
  const {header} = props;
  return <div>
    <div style={{textAlign: 'left', fontWeight: 600}}>{header.actionType} Action</div>
    <div style={{marginLeft: '1rem', display: 'grid', justifyItems: 'start', columnGap: '0.5rem', gridTemplateColumns: 'auto 1fr' }}>
      <div style={{fontWeight: 600}}>{header.agent.agentType} Agent</div>
      <div>{agentToString(header.agent)}</div>
      <div style={{fontWeight: 600}}>{header.target.targetType} Target</div>
      <div>{targetToString(header.target)}</div>
    </div>
  </div>;
};

const EventBundleView = (props: {eventBundle: AuditEventBundle}) => {
  const {eventBundle} = props;
  return <div style={{marginBottom: '1rem'}}>
    <AuditEventBundleHeaderView header={eventBundle.header} />
    <PropertyChangeListView propertyChanges={eventBundle.propertyChanges}/>
  </div>;
};

const AuditActionCard = (props: {action: AuditAction}) => {
  const {action} = props;
  // Something in the codegen is wonky here. the actionTime field is typed as a Date,
  // but turns out to be a number for some reason here. In other contexts it appears
  // to format itself happily though.
  const time = new Date(action.actionTime).toISOString();
  return (
    <ActionAuditCardBase>
      <div style={{fontWeight:  600, textAlign: 'center', fontSize: '0.825rem'}}>{time}: {action.eventBundles.length} events</div>
      {action.eventBundles.map((eventBundle, index) => <EventBundleView key={index} eventBundle={eventBundle}/>)}
    </ActionAuditCardBase>
  );
};

const AuditActionCardListView = (props: {actions: AuditAction[]}) => {
  const {actions} = props;
  const timesMillis = actions.map(action => new Date(action.actionTime).getTime());
  const minTime = new Date(Math.min(...timesMillis)).toDateString();
  const maxTime = new Date(Math.max(...timesMillis)).toDateString();

  return (
    <div style={{margin: '1rem', width: '30rem'}}>
        {actions.map((action, index) => (<AuditActionCard key={index} action={action} />))}
    </div>
  );
};

export const UserAudit = () => {
  const {username = ''} = useParams();
  const [userActions, setUserActions] = useState([]);
  const [nextUsername, setNextUsername] = useState('');
  const [navigateTo, setNavigateTo] = useState(false);
  const [loading, setLoading] = useState(true);
  const usernameWithoutGsuiteDomain = 'jaycarlton';

  useEffect(() => {
    setNextUsername(username);
  }, []);

  useEffect(() => {
    const getLogEntries = async() => {
      setLoading(true);
      const limit = 50;
      try {
        const {actions, query} = await profileApi().getAuditLogEntries(username, limit);
        console.log(query); // dont' think limit is working
        const renderedString = actions.map(action => actionToString(action)).join('\n');
        console.log(renderedString);
        console.log(actions);
        setUserActions(actions);
      } catch (e) {
        setUserActions([]);
      }
      setLoading(false);
    };

    username.length && getLogEntries();
  }, [username]);

  useEffect(() => {
    navigateTo && setNavigateTo(false);
  }, [navigateTo]);

  return !loading
    ? <React.Fragment>
        {navigateTo && <Navigate to={`/user-audit/${nextUsername}`}/>}
        <TextInput
          style={{
            width: '15rem',
            margin: '1rem'
          }}
          value = {nextUsername}
          onChange = {setNextUsername}
        />
        <Button style={{height: '1.5rem', margin: '0.25rem 0.5rem'}}
          onClick={() => setNavigateTo(true)}>
          Submit
        </Button>
        <AuditActionCardListView actions={fp.slice(0, 10, userActions)}/>
      </React.Fragment>
    : <div>Loading Audit Actions for user {username}...</div>;
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
