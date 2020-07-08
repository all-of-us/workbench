import colors, {colorWithWhiteness} from 'app/styles/colors';
import * as moment from 'moment';
import * as React from 'react';
import {
  AuditAction,
  AuditEventBundle,
  AuditEventBundleHeader,
  AuditTargetPropertyChange
} from '../../../generated';
import {ActionAuditCardBase} from '../card';
import {FlexColumn, FlexRow} from '../flex';


const HidableCell = (props: {content: string}) => {
  const {content} = props;
  return <div style={{backgroundColor: content ? 'white' : '#f0f3f5',
    border: '1px solid',
    boxSizing: 'border-box'}}>{content}</div>;
};

const PropertyChangeListEntry = (props: {targetProperty?: string, previousValue?: string, newValue?: string}) => {
  const {targetProperty, previousValue, newValue} = props;
  return <React.Fragment>
    <HidableCell content={targetProperty}/>
    <HidableCell content={previousValue}/>
    <HidableCell content={newValue}/>
  </React.Fragment>;
};

const PropertyChangeListView = (props: { propertyChanges: AuditTargetPropertyChange[] }) => {
  const {propertyChanges} = props;
  const propertyCellStyle = {fontWeight: 600, border: `1px solid`};

  return propertyChanges.length > 0 ?
  <div style={{
    margin: '0.25rem 0 0rem 1rem',
    display: 'grid',
    gridTemplateColumns: 'repeat(3, 1fr)'
  }}>
    <div style={propertyCellStyle}>Changed Property</div>
    <div style={propertyCellStyle}>Previous Value</div>
    <div style={propertyCellStyle}>New Value</div>
    {propertyChanges.map((propertyChange, index) => <PropertyChangeListEntry {...propertyChange} key={index}/>)}
  </div> : <div style={{margin: '0.25rem 0 0rem 1rem', fontStyle: 'italic'}}>No Property Changes</div>;
};

const actionTextColor = 'black';
const targetTextColor = 'green';

const AuditEventBundleHeaderView = (props: { header: AuditEventBundleHeader }) => {
  const {header} = props;
  return <div>
    <div style={{  margin: '0',
      display: 'flex',
      flexDirection: 'row',
      textAlign: 'center',
      fontWeight: 600}}>
      <div style={{color: colors.accent, margin: '5px'}}>{header.agent.agentType}</div>
      <div style={{color: actionTextColor, margin: '5px'}}>{header.actionType}</div>
      <div style={{color: targetTextColor, margin: '5px'}}>{header.target.targetType}</div>
    </div>
    <div style={{
      marginLeft: '1rem',
      display: 'grid',
      justifyItems: 'start',
      columnGap: '0.5rem',
      gridTemplateColumns: 'auto 1fr',
      gridTemplateRows: '1fr 1fr 1fr',
      gridAutoFlow: 'column',
      color: colors.accent
    }}>
          <div style={{fontWeight:  600}}>Agent</div>
          <div>{`${header.agent.agentType} ${header.agent.agentId}`}</div>
          <div>{`${header.agent.agentUsername}`}</div>
          <div style={{fontWeight:  600, color: targetTextColor}}>Target</div>
          <div style={{color: targetTextColor}}>{`${header.target.targetType} ${header.target.targetId || ''}`}</div>
          <div></div>
    </div>
  </div >;
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
  // yyyy-MM-dd HH:mm:ss.SSS
  // SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");

  const timeString = moment(new Date(action.actionTime)).format('YYYY-MM-DD h:mm:ss');
  const actionTypes = action.eventBundles.map((eventBundle) => {
    return eventBundle.header.actionType;
  }).join(' & ');
  return (
      <ActionAuditCardBase>
        <FlexRow style={{
          fontWeight: 200,
          textAlign: 'left',
          fontSize: '0.825rem',
          padding: '5px'
        }}><div>{timeString}</div><div style={{marginLeft: 'auto'}}>{actionTypes}</div>
        </FlexRow>
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
