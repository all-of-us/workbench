import * as React from 'react';
import {
  AuditAction,
  AuditAgent,
  AuditEventBundle,
  AuditEventBundleHeader,
  AuditTarget
} from 'generated';
import {WorkspaceCardBase} from '../../components/card';

interface AuditActionCardProps {
  action: AuditAction
}

interface AuditActionCardState {
  isLoaded: boolean
}

export class AuditActionCard extends React.Component<AuditActionCardProps, AuditActionCardState> {

  // agentToString = (agent?: AuditAgent) => {
  //   return `${agent.agentType} DB ID: ${agent.agentId} Username: ${agent.agentUsername}`;
  // };
  //
  // targetToString = (target?: AuditTarget) => {
  //   return `${target.targetType} DB ID: ${target.targetId}`;
  // };
  //
  // headerToString = (header?: AuditEventBundleHeader) => {
  //   return `${this.agentToString(header.agent)} \-> ${this.targetToString(header.target)}`;
  // };
  // const eventToString = (event: AuditEventBundle) => {
  //   return `${this.headerToString(event.header)}\n\t# Prop Changes: ${event.propertyChanges.length}`;
  // };
  //
  // actionToString = (action: AuditAction) => {
  //   events = action.eventBundles.map((event) => {
  //     return this.eventToString(event);
  //   });
  //   return events.join('\n');
  // };

  render() {
    const {action} = this.props;

    return <React.Fragment>
      <WorkspaceCardBase>
        <div>Time: {action.actionTime}</div>
        <div>ID: {action.actionId}</div>
      </WorkspaceCardBase>
    </React.Fragment>
  }
}
