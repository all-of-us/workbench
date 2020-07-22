import {ActionAuditCardBase} from 'app/components/card';
import {FlexRow} from 'app/components/flex';
import colors from 'app/styles/colors';
import {reactStyles} from 'app/utils';
import {usernameWithoutDomain} from 'app/utils/audit-utils';
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

const HideableLinkCell = (props: {url: string, content: string}) => {
  const {content, url} = props;
  return <a
      style={{backgroundColor: content ? 'white' : '#f0f3f5',
        border: '1px solid',
        boxSizing: 'border-box'}}
        href={url}>{content}</a>;
};

const styles = reactStyles({
  propertyCell: {
    fontWeight: 600,
    border: '1px solid'
  }
});

const isWorkspaceNamespace = (targetType?: string, targetProperty?: string) => {
  return targetType === 'WORKSPACE' && targetProperty === 'namespace';
};

const PossibleLinkCell = (props: {targetType?: string, targetProperty?: string, value?: string}) => {
  const {targetType, targetProperty, value} = props;
  if (isWorkspaceNamespace(targetType, targetProperty)) {
    return <HideableLinkCell url={`/admin/workspace-audit/${value}`} content={value}/>;
  } else {
    return <HideableCell content={value}/>;
  }
};

function isRealPropertyChange(newValue: string, previousValue: string) {
  return fp.isEmpty(newValue) && fp.isEmpty(previousValue);
}

const PropertyChangeListEntry = (props: {targetProperty?: string, previousValue?: string,
  newValue?: string, targetType?: string}) => {
  const {targetProperty, previousValue, newValue, targetType} = props;
  // On the backend, fields are initialized to null but sometimes re-initialized to
  // empty strings. Since it's not a user-driven change, I'm dropping those rows from the output.
  const isFalseChange = isRealPropertyChange(newValue, previousValue);
  return isFalseChange
      ? null
      : <React.Fragment>
    <HideableCell content={targetProperty}/>
    <HideableCell content={previousValue}/>
    <PossibleLinkCell targetType={targetType}
                      targetProperty={targetProperty}
                      value={newValue}/>
  </React.Fragment>;
};

const PropertyChangeListView = (props: { eventBundle: AuditEventBundle }) => {
  const {header, propertyChanges} = props.eventBundle;

  return (propertyChanges.length > 0)
    && fp.any((p: AuditTargetPropertyChange) => isRealPropertyChange(p.newValue, p.previousValue))(propertyChanges)
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
        <PropertyChangeListEntry targetProperty={propertyChange.targetProperty}
                                 previousValue={propertyChange.previousValue}
                                 newValue={propertyChange.newValue}
                                 targetType={header.target.targetType}
                                 key={index}/>)}
  </div>
      : <div style={{margin: '0.25rem 0 0rem 1rem', fontStyle: 'italic'}}>No Property Changes</div>;
};

const AgentUsernameCell = (props: {agent: AuditAgent}) => {
  const {agentType, agentUsername} = props.agent;
  return agentType === 'USER'
      ? <div><a href={`/admin/user-audit/${usernameWithoutDomain(agentUsername)}`}>{agentUsername}</a></div>
      : <div>agentUsername</div>;
};

const AgentHeader = (props: {agent: AuditAgent}) => {
  const {agent} = props;
  return <React.Fragment>
    <div style={{fontWeight: 600}}>Agent</div>
    <div>{`${agent.agentType} ${agent.agentId}`}</div>
    <AgentUsernameCell agent={agent}/>
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

const EventBundleView = (props: {eventBundle: AuditEventBundle}) => {
  const {eventBundle} = props;
  return <div style={{marginBottom: '1rem'}}>
    <AuditEventBundleHeaderView header={eventBundle.header}/>
    <PropertyChangeListView eventBundle={eventBundle}/>
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
                             eventBundle={eventBundle}/>)}
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
