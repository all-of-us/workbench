import * as React from 'react';
import {
  AuditAction,
  AuditEventBundle,
  AuditEventBundleHeader,
  AuditTargetPropertyChange
} from '../../../generated';
import {agentToString, targetToString} from '../../utils/audit-utils';
import {ActionAuditCardBase} from '../card';

const hideableStyle = (content) => {
  return {backgroundColor: content ? 'white' : 'lightGray'};
};

const HidableCell = (props: {content: string}) => {
  const {content} = props;
  return <div style={{backgroundColor: content ? 'white' : '#f0f3f5'}}>{content}</div>;
};

const PropertyChangeListEntry = (props: {targetProperty?: string, previousValue?: string, newValue?: string}) => {
  const {targetProperty, previousValue, newValue} = props;
  return <React.Fragment>
    {/*<div style={hideableStyle(targetProperty)}>{targetProperty}</div>*/}
    {/*<div style={hideableStyle(previousValue)}>{previousValue}</div>*/}
    {/*<div style={hideableStyle(newValue)}>{newValue}</div>*/}

    <HidableCell content={targetProperty}/>
    <HidableCell content={previousValue}/>
    <HidableCell content={newValue}/>
  </React.Fragment>;
};
const PropertyChangeListView = (props: { propertyChanges: AuditTargetPropertyChange[] }) => {
  const {propertyChanges} = props;

  return propertyChanges.length > 0 ?
  <div style={{
    margin: '0.25rem 0 0rem 1rem',
    display: 'grid',
    gridTemplateColumns: 'repeat(3, 1fr)'
  }}>
    <div style={{fontWeight: 600}}>Property</div>
    <div style={{fontWeight: 600}}>Previous Value</div>
    <div style={{fontWeight: 600}}>New Value</div>
    {propertyChanges.map((changes, index) => <PropertyChangeListEntry {...changes} key={index}/>)}
  </div> :
      <div>No Property Changes</div>;
};
const AuditEventBundleHeaderView = (props: { header: AuditEventBundleHeader }) => {
  const {header} = props;
  return <div>
    <div style={{textAlign: 'left', fontWeight: 600}}>{header.actionType} Action</div>
    <div style={{
      marginLeft: '1rem',
      display: 'grid',
      justifyItems: 'start',
      columnGap: '0.5rem',
      gridTemplateColumns: 'auto 1fr'
    }}>
      <div style={{fontWeight: 600}}>{header.agent.agentType} Agent</div>
      <div>{agentToString(header.agent)}</div>
      <div style={{fontWeight: 600}}>{header.target.targetType} Target</div>
      <div>{targetToString(header.target)}</div>
    </div>
  </div>;
};

const EventBundleView = (props: { eventBundle: AuditEventBundle }) => {
  const {eventBundle} = props;
  return <div style={{marginBottom: '1rem'}}>
    <AuditEventBundleHeaderView header={eventBundle.header}/>
    <PropertyChangeListView propertyChanges={eventBundle.propertyChanges}/>
  </div>;
};

const AuditActionCard = (props: { action: AuditAction }) => {
  const {action} = props;
  // Something in the codegen is wonky here. the actionTime field is typed as a Date,
  // but turns out to be a number for some reason here. In other contexts it appears
  // to format itself happily though.
  const timeString = new Date(action.actionTime).toTimeString();
  const actionTypes = action.eventBundles.map((eventBundle) => {
    return eventBundle.header.actionType;
  }).join(' & ');
  return (
      <ActionAuditCardBase>
        <div style={{
          fontWeight: 600,
          textAlign: 'left',
          fontSize: '0.825rem',
          backgroundColor: 'lightGrey'
        }}>{timeString}
        </div>
        <div>{action.eventBundles.length} events: {actionTypes}</div>
        {action.eventBundles.map((eventBundle, index) =>
            <EventBundleView key={index} eventBundle={eventBundle}/>)}
      </ActionAuditCardBase>
  );
};
export const AuditActionCardListView = (props: { actions: AuditAction[]}) => {
  const {actions} = props;

  return (
      <div style={{margin: '1rem', width: '30rem'}}>
        {actions.map((action, index) => (<AuditActionCard key={index} action={action}/>))}
      </div>
  );
};
