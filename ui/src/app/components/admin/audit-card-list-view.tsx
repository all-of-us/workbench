import {ActionAuditCardBase} from 'app/components/card';
import {FlexRow} from 'app/components/flex';
import colors from 'app/styles/colors';
import {reactStyles} from 'app/utils';
import {
  AuditAction, AuditAgent,
  AuditEventBundle,
  AuditEventBundleHeader, AuditTarget,
  AuditTargetPropertyChange
} from 'generated';
import * as fp from 'lodash/fp';
import * as moment from 'moment';
import * as React from 'react';

const HideableCell = (props: {content: string}) => {
  const {content} = props;
  return <div
      style={{backgroundColor: content ? 'white' : '#f0f3f5',
        border: '1px solid',
        boxSizing: 'border-box'
      }}>{content}</div>;
};

const styles = reactStyles({
  propertyCell: {
    fontWeight: 600,
    border: '1px solid'
  }
});

const PropertyChangeListEntry = (props: {targetProperty?: string, previousValue?: string, newValue?: string}) => {
  const {targetProperty, previousValue, newValue} = props;
  return <React.Fragment>
    <HideableCell content={targetProperty}/>
    <HideableCell content={previousValue}/>
    <HideableCell content={newValue}/>
  </React.Fragment>;
};

const PropertyChangeListView = (props: { propertyChanges: AuditTargetPropertyChange[] }) => {
  const {propertyChanges} = props;

  return propertyChanges.length > 0
      ? <div style={{
        marginTop: '0.25rem',
        marginLeft: '1rem',
        display: 'grid',
        gridTemplateColumns: 'repeat(3, 1fr)'
      }}>
    <div style={styles.propertyCell}>Changed Property</div>
    <div style={styles.propertyCell}>Previous Value</div>
    <div style={styles.propertyCell}>New Value</div>
    {propertyChanges.map((propertyChange, index) =>
        <PropertyChangeListEntry {...propertyChange} key={index}/>)}
  </div>
      : <div style={{margin: '0.25rem 0 0rem 1rem', fontStyle: 'italic'}}>No Property Changes</div>;
};

const AgentHeader = (props: {agent: AuditAgent}) => {
  const {agent} = props;
  return <React.Fragment>
    <div style={{fontWeight: 600}}>Agent</div>
    <div>{`${agent.agentType} ${agent.agentId}`}</div>
    <div>{agent.agentUsername}</div>
  </React.Fragment>;
};

const TargetHeader = (props: {target: AuditTarget}) => {
  const {target} = props;
  return <React.Fragment>
  <div style={{fontWeight: 600, color: colors.accent}}>Target</div>
  <div
      style={{color: colors.accent}}>{`${target.targetType} ${target.targetId || ''}`}</div>
  <div/>
  </React.Fragment>;
};

const AuditEventBundleHeaderView = (props: { header: AuditEventBundleHeader }) => {
  const {header} = props;
  return <div>
    <div style={{
      margin: '0',
      display: 'flex',
      flexDirection: 'row',
      textAlign: 'center',
      fontWeight: 600
    }}>
      <div style={{color: colors.primary, margin: '5px'}}>{header.agent.agentType}</div>
      <div style={{color: colors.success, margin: '5px'}}>{header.actionType}</div>
      <div style={{color: colors.accent, margin: '5px'}}>{header.target.targetType}</div>
    </div>
    <div style={{
      marginLeft: '1rem',
      display: 'grid',
      justifyItems: 'start',
      columnGap: '0.5rem',
      gridTemplateColumns: 'auto 1fr',
      gridTemplateRows: '1fr 1fr 1fr',
      gridAutoFlow: 'column',
      color: colors.primary
    }}>
      <AgentHeader agent={header.agent}/>
      <TargetHeader target={header.target}/>
    </div>
  </div>;
};

const EventBundleView = (eventBundle: AuditEventBundle) => {
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
  const timeString = moment(new Date(action.actionTime)).format('YYYY-MM-DD h:mm:ss');
  const actionTypes = fp.flow(
    fp.map(fp.get('header.actionType')),
    s => s.join(' & '))
  (action.eventBundles);

  return (
      <ActionAuditCardBase>
        <FlexRow style={{
          fontWeight: 200,
          textAlign: 'left',
          fontSize: '0.825rem',
          padding: '5px'
        }}>
          <div>{timeString}</div>
          <div style={{marginLeft: 'auto'}}>{actionTypes}</div>
        </FlexRow>
        {action.eventBundles.map((eventBundle, index) =>
            <EventBundleView key={index}
                             header={eventBundle.header}
                             propertyChanges={eventBundle.propertyChanges}/>)}
      </ActionAuditCardBase>
  );
};

export const AuditActionCardListView = (props: { actions: AuditAction[]}) => {
  const {actions} = props;

  // Temporary workaround for sort order in the APIs, fixed in RW-4999.
  const actionsSorted = actions.sort((a, b) => {
    return new Date(b.actionTime).getTime() - new Date(a.actionTime).getTime();
  });

  return (
      <div style={{margin: '1rem', width: '30rem'}}>
        {actionsSorted.map((action, index) => (<AuditActionCard key={index} action={action}/>))}
      </div>
  );
};
