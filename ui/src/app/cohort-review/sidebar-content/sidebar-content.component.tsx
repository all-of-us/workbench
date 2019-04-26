import {Component, Input} from '@angular/core';
import * as fp from 'lodash/fp';
import * as moment from 'moment';
import * as React from 'react';

import {Participant} from 'app/cohort-review/participant.model';
import {cohortReviewStore} from 'app/cohort-review/review-state.service';
import {Button} from 'app/components/buttons';
import {styles as headerStyles} from 'app/components/headers';
import {ClrIcon} from 'app/components/icons';
import {CheckBox, DatePicker, NumberInput, Select, TextArea} from 'app/components/inputs';
import {Spinner} from 'app/components/spinners';
import {cohortReviewApi} from 'app/services/swagger-fetch-clients';
import {WorkspaceData} from 'app/services/workspace-storage.service';
import colors from 'app/styles/colors';
import {ReactWrapperBase, withCurrentWorkspace, withUrlParams} from 'app/utils/index';
import {AnnotationType, CohortAnnotationDefinition, CohortStatus, ParticipantCohortAnnotation, WorkspaceAccessLevel} from 'generated/fetch';

const styles = {
  header: {
    fontSize: 18,
    color: colors.purple[0],
  }
};

const writeValue = (type, value) => {
  switch (type) {
    case AnnotationType.INTEGER: return {annotationValueInteger: value};
    case AnnotationType.STRING: return {annotationValueString: value};
    case AnnotationType.ENUM: return {annotationValueEnum: value};
    case AnnotationType.BOOLEAN: return {annotationValueBoolean: value};
    case AnnotationType.DATE: return {annotationValueDate: moment(value).format('YYYY-MM-DD')};
  }
};

const readValue = (type, annotation) => {
  switch (type) {
    case AnnotationType.INTEGER: return annotation ? annotation.annotationValueInteger : null;
    case AnnotationType.STRING: return annotation ? annotation.annotationValueString : '';
    case AnnotationType.ENUM: return annotation ? annotation.annotationValueEnum : '';
    case AnnotationType.BOOLEAN: return annotation ? annotation.annotationValueBoolean : false;
    case AnnotationType.DATE:
      return annotation ? moment(annotation.annotationValueDate).toDate() : null;
  }
};

const AnnotationItem = fp.flow(
  withUrlParams(),
  withCurrentWorkspace(),
)(class extends React.Component<{
  annotation: ParticipantCohortAnnotation,
  setAnnotation: Function,
  definition: CohortAnnotationDefinition,
  urlParams: any,
  workspace: WorkspaceData,
}, {
  editValue: number | string | boolean | Date,
  savingValue: number | string | boolean | Date,
}> {
  constructor(props) {
    super(props);
    this.state = {
      editValue: undefined,
      savingValue: undefined,
    };
  }

  async save(newValue) {
    try {
      const {
        annotation, setAnnotation,
        definition: {annotationType, cohortAnnotationDefinitionId},
        urlParams: {ns, wsid, cid, pid},
        workspace: {cdrVersionId},
      } = this.props;
      const aid = annotation ? annotation.annotationId : undefined;
      const value = readValue(annotationType, annotation);
      this.setState({savingValue: newValue});
      if (aid && fp.includes(newValue, [null, ''])) {
        setAnnotation(await cohortReviewApi()
          .deleteParticipantCohortAnnotation(ns, wsid, cid, +cdrVersionId, pid, aid));
      } else if (aid && newValue !== value) {
        setAnnotation(await cohortReviewApi()
          .updateParticipantCohortAnnotation(ns, wsid, cid, +cdrVersionId, pid, aid, {
            ...writeValue(annotationType, newValue),
          }));
      } else if (!aid && newValue) {
        setAnnotation(await cohortReviewApi()
          .createParticipantCohortAnnotation(ns, wsid, cid, +cdrVersionId, pid, {
            cohortAnnotationDefinitionId,
            cohortReviewId: cohortReviewStore.getValue().cohortReviewId,
            participantId: pid,
            ...writeValue(annotationType, newValue),
          }));
      }
    } catch (error) {
      console.error(error);
    } finally {
      this.setState({savingValue: undefined});
    }
  }

  renderInput() {
    const {
      definition: {annotationType, enumValues}, annotation, workspace: {accessLevel}
    } = this.props;
    const {editValue, savingValue} = this.state;
    const value = fp.pull(undefined,
      [savingValue, editValue, readValue(annotationType, annotation)]
    )[0];
    const disabled = accessLevel === WorkspaceAccessLevel[WorkspaceAccessLevel.NOACCESS] ||
      accessLevel === WorkspaceAccessLevel[WorkspaceAccessLevel.READER];
    switch (annotationType) {
      case AnnotationType.INTEGER:
        return <NumberInput
          value={value}
          onChange={v => this.setState({editValue: v})}
          onBlur={() => {
            this.setState({editValue: undefined});
            this.save(value);
          }}
          disabled={disabled}
        />;
      case AnnotationType.STRING:
        return <TextArea
          value={value}
          onChange={v => this.setState({editValue: v})}
          onBlur={() => {
            this.setState({editValue: undefined});
            this.save(value);
          }}
          disabled={disabled}
        />;
      case AnnotationType.ENUM:
        return <Select
          options={[
            {label: '--', value: ''},
            ...enumValues.map(s => ({label: s, value: s})),
          ]}
          value={value}
          onChange={v => this.save(v)}
          disabled={disabled}
        />;
      case AnnotationType.BOOLEAN:
        return <CheckBox
          checked={value}
          onChange={v => this.save(v)}
          disabled={disabled}
        />;
      case AnnotationType.DATE:
        return <DatePicker
          value={value}
          onChange={v => {
            if (moment(v, 'YYYY-MM-DD', true).isValid()) {
              this.setState({editValue: undefined});
              this.save(v);
            } else {
              this.setState({editValue: v});
            }
          }}
          onBlur={() => {
            this.setState({editValue: undefined});
            if (moment(value, 'YYYY-MM-DD', true).isValid()) {
              this.save(value);
            }
          }}
          disabled={disabled}
        />;
    }
  }

  render() {
    const {definition: {columnName}} = this.props;
    const {savingValue} = this.state;
    return <React.Fragment>
      <div style={{display: 'flex', alignItems: 'center', ...headerStyles.formLabel}}>
        <div>{columnName}</div>
        {savingValue !== undefined &&
          <Spinner style={{marginLeft: 'auto'}} width={16} height={16}/>
        }
      </div>
      {this.renderInput()}
    </React.Fragment>;
  }
});

export const SidebarContent = fp.flow(
  withUrlParams(),
  withCurrentWorkspace(),
)(class extends React.Component<
  {
    participant: Participant,
    setParticipant: Function,
    annotations: ParticipantCohortAnnotation[],
    annotationDefinitions: CohortAnnotationDefinition[],
    setAnnotations: Function,
    openCreateDefinitionModal: Function,
    openEditDefinitionsModal: Function,
    urlParams: any,
    workspace: WorkspaceData,
  },
  {savingStatus: CohortStatus}
> {
  constructor(props) {
    super(props);
    this.state = {
      savingStatus: undefined,
    };
  }

  async saveStatus(v) {
    try {
      const {
        setParticipant,
        urlParams: {ns, wsid, cid, pid},
        workspace: {cdrVersionId},
      } = this.props;
      this.setState({savingStatus: v});
      const data = await cohortReviewApi().updateParticipantCohortStatus(
        ns, wsid, cid, +cdrVersionId, pid, {status: v}
      );
      // make sure we're still on the same page before updating
      if (data.participantId === +this.props.urlParams.pid) {
        setParticipant(new Participant(data as any));
      }
    } catch (error) {
      console.error(error);
    } finally {
      this.setState({savingStatus: undefined});
    }
  }

  render() {
    const {
      participant: {participantId, birthDate, gender, race, ethnicity, status},
      annotations, setAnnotations, annotationDefinitions,
      openCreateDefinitionModal, openEditDefinitionsModal, workspace: {accessLevel}
    } = this.props;
    const {savingStatus} = this.state;
    const disabled = accessLevel === WorkspaceAccessLevel[WorkspaceAccessLevel.NOACCESS] ||
      accessLevel === WorkspaceAccessLevel[WorkspaceAccessLevel.READER];
    return <div>
      <div style={styles.header}>Participant {participantId}</div>
      <div><span style={{fontWeight: 'bold'}}>DOB:</span> {birthDate}</div>
      <div><span style={{fontWeight: 'bold'}}>Gender:</span> {gender}</div>
      <div><span style={{fontWeight: 'bold'}}>Race:</span> {race}</div>
      <div><span style={{fontWeight: 'bold'}}>Ethnicity:</span> {ethnicity}</div>

      <div style={{display: 'flex', marginTop: '1rem'}}>
        <div style={styles.header}>Participant Status</div>
        {savingStatus && <Spinner width={16} height={16} style={{marginLeft: 'auto'}} />}
      </div>
      <div>Choose a Review Status for Participant {participantId}</div>
      <div style={{...(disabled ? {cursor: 'not-allowed'} : {})}}>
        <Select
          options={[
            {label: '--', value: CohortStatus.NOTREVIEWED},
            {label: 'Excluded', value: CohortStatus.EXCLUDED},
            {label: 'Included', value: CohortStatus.INCLUDED},
            {label: 'Needs Further Review', value: CohortStatus.NEEDSFURTHERREVIEW},
          ]}
          value={savingStatus || status}
          onChange={v => this.saveStatus(v)}
          isDisabled={disabled}
        />
      </div>

      <div style={{display: 'flex', marginTop: '1rem'}}>
        <div style={styles.header}>Annotations</div>
        <Button
          type='link' style={{marginLeft: '1rem', ...(disabled ? {cursor: 'not-allowed'} : {})}}
          onClick={openCreateDefinitionModal} disabled={disabled}>
          <ClrIcon shape='plus-circle' size={21} />
        </Button>
        {!!annotationDefinitions.length && <Button
          style={{marginLeft: '1rem', ...(disabled ? {cursor: 'not-allowed'} : {})}} type='link'
          onClick={openEditDefinitionsModal} disabled={disabled}
        >Edit</Button>}
      </div>
      {annotationDefinitions.map(def => {
        const {cohortAnnotationDefinitionId} = def;
        const annotation = fp.find({cohortAnnotationDefinitionId}, annotations);
        return <AnnotationItem
          key={cohortAnnotationDefinitionId} annotation={annotation} definition={def}
          setAnnotation={update => {
            // make sure we're still on the same page before updating
            if (participantId === +this.props.urlParams.pid) {
              const filtered = fp.remove({cohortAnnotationDefinitionId}, annotations);
              setAnnotations(filtered.concat(update.annotationId ? [update] : []));
            }
          }}
        />;
      })}
    </div>;
  }
});

@Component({
  selector: 'app-sidebar-content',
  template: '<div #root></div>',
})
export class SidebarContentComponent extends ReactWrapperBase {
  @Input() participant: Participant;
  @Input() setParticipant: Function;
  @Input() annotations: ParticipantCohortAnnotation[];
  @Input() annotationDefinitions: CohortAnnotationDefinition[];
  @Input() setAnnotations: Function;
  @Input() openCreateDefinitionModal: Function;
  @Input() openEditDefinitionsModal: Function;

  constructor() {
    super(SidebarContent, [
      'participant', 'setParticipant', 'annotations', 'annotationDefinitions', 'setAnnotations',
      'openCreateDefinitionModal', 'openEditDefinitionsModal',
    ]);
  }
}
