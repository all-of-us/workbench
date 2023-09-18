import * as React from 'react';
import * as fp from 'lodash/fp';

import {
  AnnotationType,
  CohortAnnotationDefinition,
  CohortReview,
  CohortStatus,
  ParticipantCohortAnnotation,
  ParticipantCohortStatus,
  WorkspaceAccessLevel,
} from 'generated/fetch';

import { Button } from 'app/components/buttons';
import { styles as headerStyles } from 'app/components/headers';
import { ClrIcon } from 'app/components/icons';
import {
  CheckBox,
  DatePicker,
  inputBorderColor,
  NumberInput,
  Select,
  TextArea,
} from 'app/components/inputs';
import { Spinner } from 'app/components/spinners';
import {
  AddAnnotationDefinitionModal,
  EditAnnotationDefinitionsModal,
} from 'app/pages/data/cohort-review/annotation-definition-modals.component';
import {
  participantStore,
  updateParticipant,
} from 'app/services/review-state.service';
import {
  cohortAnnotationDefinitionApi,
  cohortReviewApi,
} from 'app/services/swagger-fetch-clients';
import colors, { colorWithWhiteness } from 'app/styles/colors';
import { withCurrentCohortReview, withCurrentWorkspace } from 'app/utils';
import { WorkspaceData } from 'app/utils/workspace-data';
import moment from 'moment';

const styles = {
  header: {
    fontSize: 18,
    color: colors.primary,
  },
  error: {
    color: colors.warning,
    marginTop: '-3px',
  },
  success: {
    color: colors.success,
    marginTop: '-3px',
  },
  message: {
    color: colorWithWhiteness(colors.dark, -1),
    fontSize: '13px',
    fontWeight: 400,
  },
  button: {
    marginLeft: '1.5rem',
    height: 'auto',
  },
  inlineBlock: {
    display: 'inline-block',
  },
  numberInput: {
    background: colors.white,
    border: `1px solid ${inputBorderColor}`,
    borderRadius: 3,
    height: '2.25rem',
    padding: '0 0.75rem',
    width: '100%',
  },
};

const writeValue = (type, value) => {
  switch (type) {
    case AnnotationType.INTEGER:
      return { annotationValueInteger: value };
    case AnnotationType.STRING:
      return { annotationValueString: value };
    case AnnotationType.ENUM:
      return { annotationValueEnum: value };
    case AnnotationType.BOOLEAN:
      return { annotationValueBoolean: value };
    case AnnotationType.DATE:
      return { annotationValueDate: moment(value).format('YYYY-MM-DD') };
  }
};

const readValue = (type, annotation) => {
  switch (type) {
    case AnnotationType.INTEGER:
      return annotation ? annotation.annotationValueInteger : null;
    case AnnotationType.STRING:
      return annotation ? annotation.annotationValueString : '';
    case AnnotationType.ENUM:
      return annotation ? annotation.annotationValueEnum : '';
    case AnnotationType.BOOLEAN:
      return annotation ? annotation.annotationValueBoolean : false;
    case AnnotationType.DATE:
      return annotation
        ? moment(annotation.annotationValueDate).toDate()
        : null;
  }
};

interface AnnotationProps {
  annotation: ParticipantCohortAnnotation;
  setAnnotation: Function;
  cohortReview: CohortReview;
  definition: CohortAnnotationDefinition;
  participantId: number;
  workspace: WorkspaceData;
}

interface AnnotationState {
  editValue: number | string | boolean | Date;
  savingValue: number | string | boolean | Date;
  saving: boolean;
  error: boolean;
  success: boolean;
  timeout: NodeJS.Timeout;
}

const AnnotationItem = fp.flow(
  withCurrentCohortReview(),
  withCurrentWorkspace()
)(
  class extends React.Component<AnnotationProps, AnnotationState> {
    constructor(props) {
      super(props);
      this.state = {
        editValue: undefined,
        savingValue: undefined,
        saving: false,
        error: false,
        success: false,
        timeout: undefined,
      };
    }

    componentDidUpdate(prevProps: AnnotationProps): void {
      const { timeout } = this.state;
      if (this.props.participantId !== prevProps.participantId) {
        // get rid of spinners and save messages when switching participants
        clearTimeout(timeout);
        this.setState({ saving: false, error: false, success: false });
      }
    }

    async save(newValue) {
      try {
        const {
          annotation,
          setAnnotation,
          cohortReview: { cohortReviewId },
          definition: { annotationType, cohortAnnotationDefinitionId },
          workspace: { namespace, id },
          participantId,
        } = this.props;
        const { timeout } = this.state;
        const aid = annotation ? annotation.annotationId : undefined;
        const value = readValue(annotationType, annotation);
        this.setState({ savingValue: newValue });
        if (aid && fp.includes(newValue, [null, ''])) {
          setAnnotation(
            await cohortReviewApi().deleteParticipantCohortAnnotation(
              namespace,
              id,
              cohortReviewId,
              participantId,
              aid
            )
          );
        } else if (aid && newValue !== value) {
          clearTimeout(timeout);
          this.setState({ error: false, success: false, saving: true });
          await cohortReviewApi()
            .updateParticipantCohortAnnotation(
              namespace,
              id,
              cohortReviewId,
              participantId,
              aid,
              {
                ...writeValue(annotationType, newValue),
              }
            )
            .then((res) => {
              setAnnotation(res);
              this.setState({ saving: false, success: true });
            });
        } else if (!aid && newValue) {
          clearTimeout(timeout);
          this.setState({ error: false, success: false, saving: true });
          await cohortReviewApi()
            .createParticipantCohortAnnotation(
              namespace,
              id,
              cohortReviewId,
              participantId,
              {
                cohortAnnotationDefinitionId,
                cohortReviewId,
                participantId: participantId,
                ...writeValue(annotationType, newValue),
              }
            )
            .then((res) => {
              setAnnotation(res);
              this.setState({ saving: false, success: true });
            });
        }
      } catch (error) {
        console.error(error);
        this.setState({ saving: false, error: true });
      } finally {
        const timeout: NodeJS.Timeout = global.setTimeout(
          () => this.setState({ error: false, success: false }),
          5000
        );
        this.setState({ savingValue: undefined, timeout });
      }
    }

    renderInput() {
      const {
        definition: { annotationType, enumValues },
        annotation,
        workspace: { accessLevel },
      } = this.props;
      const { editValue, savingValue } = this.state;
      const value = fp.pull(undefined, [
        savingValue,
        editValue,
        readValue(annotationType, annotation),
      ])[0];
      const disabled =
        accessLevel === WorkspaceAccessLevel.NO_ACCESS ||
        accessLevel === WorkspaceAccessLevel.READER;
      switch (annotationType) {
        case AnnotationType.INTEGER:
          return (
            <NumberInput
              value={value}
              onChange={(v) => this.setState({ editValue: v })}
              onBlur={() => {
                this.setState({ editValue: undefined });
                this.save(value);
              }}
              disabled={disabled}
            />
          );
        case AnnotationType.STRING:
          return (
            <TextArea
              value={value}
              onChange={(v) => this.setState({ editValue: v })}
              onBlur={() => {
                this.setState({ editValue: undefined });
                this.save(value);
              }}
              disabled={disabled}
            />
          );
        case AnnotationType.ENUM:
          return (
            <Select
              options={[
                { label: '--', value: '' },
                ...enumValues.map((s) => ({ label: s, value: s })),
              ]}
              value={value}
              onChange={(v) => this.save(v)}
              disabled={disabled}
            />
          );
        case AnnotationType.BOOLEAN:
          return (
            <CheckBox
              manageOwnState={false}
              checked={value}
              onChange={(v) => this.save(v)}
              disabled={disabled}
            />
          );
        case AnnotationType.DATE:
          return (
            <DatePicker
              value={value}
              onChange={(v) => {
                if (moment(v, 'YYYY-MM-DD', true).isValid()) {
                  this.setState({ editValue: undefined });
                  this.save(v);
                } else {
                  this.setState({ editValue: v });
                }
              }}
              onBlur={() => {
                this.setState({ editValue: undefined });
                if (moment(value, 'YYYY-MM-DD', true).isValid()) {
                  this.save(value);
                }
              }}
              placeholder='YYYY-MM-DD'
              disabled={disabled}
            />
          );
      }
    }

    render() {
      const {
        definition: { columnName },
      } = this.props;
      const { error, saving, success } = this.state;
      return (
        <React.Fragment>
          <div style={{ alignItems: 'center', ...headerStyles.formLabel }}>
            <div style={styles.inlineBlock}>{columnName}</div>
            {error && (
              <div style={styles.inlineBlock}>
                <ClrIcon
                  style={styles.error}
                  shape='exclamation-triangle'
                  size='20'
                  className='is-solid'
                />
                <span style={styles.message}> Save Failed</span>
              </div>
            )}
            {success && (
              <div style={styles.inlineBlock}>
                <ClrIcon
                  style={styles.success}
                  shape='check-circle'
                  size='20'
                  className='is-solid'
                />
                <span style={styles.message}> Annotation Saved</span>
              </div>
            )}
            {saving && <Spinner style={{ marginLeft: '0.375rem' }} size={16} />}
          </div>
          {this.renderInput()}
        </React.Fragment>
      );
    }
  }
);

interface SidebarProps {
  cohortReview: CohortReview;
  participant: ParticipantCohortStatus;
  workspace: WorkspaceData;
}

interface SidebarState {
  savingStatus: CohortStatus;
  creatingDefinition: boolean;
  editingDefinitions: boolean;
  annotations: ParticipantCohortAnnotation[];
  annotationDefinitions: CohortAnnotationDefinition[];
  annotationDeleted: boolean;
}

export const SidebarContent = fp.flow(
  withCurrentCohortReview(),
  withCurrentWorkspace()
)(
  class extends React.Component<SidebarProps, SidebarState> {
    constructor(props) {
      super(props);
      this.state = {
        savingStatus: undefined,
        creatingDefinition: false,
        editingDefinitions: false,
        annotations: null,
        annotationDefinitions: null,
        annotationDeleted: false,
      };
    }

    componentDidMount(): void {
      const {
        cohortReview: { cohortId, cohortReviewId },
        participant,
      } = this.props;
      const { namespace, id } = this.props.workspace;
      this.getAnnotations(
        namespace,
        id,
        cohortReviewId,
        participant.participantId
      );
      cohortAnnotationDefinitionApi()
        .getCohortAnnotationDefinitions(namespace, id, cohortId)
        .then(({ items }) => {
          this.setState({ annotationDefinitions: items });
        });
    }

    componentDidUpdate(prevProps: SidebarProps): void {
      const {
        cohortReview: { cohortReviewId },
        participant,
      } = this.props;
      const { namespace, id } = this.props.workspace;
      if (
        participant.participantId !== prevProps.participant.participantId &&
        !isNaN(participant.participantId)
      ) {
        // get values for annotations when switching participants
        this.getAnnotations(
          namespace,
          id,
          cohortReviewId,
          participant.participantId
        );
      }
    }

    async getAnnotations(ns, wsid, cohortReviewId, pid) {
      const { items } = await cohortReviewApi().getParticipantCohortAnnotations(
        ns,
        wsid,
        cohortReviewId,
        pid
      );
      this.setState({ annotations: items });
    }

    async saveStatus(v) {
      try {
        const {
          cohortReview: { cohortReviewId },
          workspace: { namespace, id },
          participant: { participantId },
        } = this.props;
        this.setState({ savingStatus: v });
        const participant =
          await cohortReviewApi().updateParticipantCohortStatus(
            namespace,
            id,
            cohortReviewId,
            participantId,
            { status: v }
          );
        // make sure we're still on the same page before updating
        if (participant.participantId === participantId) {
          participantStore.next(participant);
          updateParticipant(participant);
        }
      } catch (error) {
        console.error(error);
      } finally {
        this.setState({ savingStatus: undefined });
      }
    }

    closeEditDefinitionsModal(deleted: boolean = false) {
      this.setState({ editingDefinitions: false, annotationDeleted: deleted });
      if (deleted) {
        setTimeout(() => this.setState({ annotationDeleted: false }), 5000);
      }
    }

    definitionCreated(ad) {
      const annotationDefinitions = this.state.annotationDefinitions.concat([
        ad,
      ]);
      this.setState({ annotationDefinitions, creatingDefinition: false });
    }

    render() {
      const {
        workspace: { accessLevel },
        cohortReview,
        participant,
      } = this.props;
      const {
        participantId,
        birthDate,
        gender,
        race,
        ethnicity,
        deceased,
        status,
      } = participant;
      const {
        annotations,
        annotationDefinitions,
        annotationDeleted,
        savingStatus,
        creatingDefinition,
        editingDefinitions,
      } = this.state;
      const disabled =
        accessLevel === WorkspaceAccessLevel.NO_ACCESS ||
        accessLevel === WorkspaceAccessLevel.READER;
      const annotationsExist =
        annotationDefinitions && annotationDefinitions.length > 0;
      return (
        <React.Fragment>
          <div>
            <span style={{ fontWeight: 'bold' }}>DOB:</span> {birthDate}
          </div>
          <div>
            <span style={{ fontWeight: 'bold' }}>Gender:</span> {gender}
          </div>
          <div>
            <span style={{ fontWeight: 'bold' }}>Race:</span> {race}
          </div>
          <div>
            <span style={{ fontWeight: 'bold' }}>Ethnicity:</span> {ethnicity}
          </div>
          <div>
            <span style={{ fontWeight: 'bold' }}>Deceased:</span>{' '}
            {deceased ? 'Yes' : 'No'}
          </div>

          <div style={{ display: 'flex', marginTop: '1.5rem' }}>
            <div style={styles.header}>Participant Status</div>
            {savingStatus && (
              <Spinner width={16} height={16} style={{ marginLeft: 'auto' }} />
            )}
          </div>
          <div>Choose a Review Status for Participant {participantId}</div>
          <div
            style={{
              ...(disabled ? { cursor: 'not-allowed' } : {}),
              marginBottom: '1.5rem',
            }}
          >
            <Select
              options={[
                { label: '--', value: CohortStatus.NOTREVIEWED },
                { label: 'Excluded', value: CohortStatus.EXCLUDED },
                { label: 'Included', value: CohortStatus.INCLUDED },
                {
                  label: 'Needs Further Review',
                  value: CohortStatus.NEEDS_FURTHER_REVIEW,
                },
              ]}
              value={savingStatus || status}
              onChange={(v) => this.saveStatus(v)}
              isDisabled={disabled}
            />
          </div>
          {annotationDeleted && (
            <div>
              <ClrIcon
                style={styles.success}
                shape='check-circle'
                size='20'
                className='is-solid'
              />
              <span style={styles.message}> Annotation Field Deleted</span>
            </div>
          )}
          <div style={{ display: 'flex' }}>
            <div style={styles.header}>Annotations</div>
            <Button
              type='link'
              style={{
                ...styles.button,
                ...(disabled ? { cursor: 'not-allowed' } : {}),
              }}
              onClick={() => this.setState({ creatingDefinition: true })}
              disabled={disabled}
            >
              <ClrIcon shape='plus-circle' size={21} />
            </Button>
            {annotationsExist && (
              <Button
                type='link'
                style={{
                  ...styles.button,
                  ...(disabled ? { cursor: 'not-allowed' } : {}),
                }}
                onClick={() => this.setState({ editingDefinitions: true })}
                disabled={disabled}
              >
                Edit
              </Button>
            )}
          </div>
          {annotationsExist &&
            annotationDefinitions.map((def) => {
              const { cohortAnnotationDefinitionId } = def;
              const annotation = fp.find(
                { cohortAnnotationDefinitionId },
                annotations
              );
              return (
                <AnnotationItem
                  key={cohortAnnotationDefinitionId}
                  annotation={annotation}
                  definition={def}
                  setAnnotation={(update) => {
                    const filtered = fp.remove(
                      { cohortAnnotationDefinitionId },
                      annotations
                    );
                    this.setState({
                      annotations: filtered.concat(
                        update.annotationId ? [update] : []
                      ),
                    });
                  }}
                  participantId={participant.participantId}
                />
              );
            })}
          {editingDefinitions && (
            <EditAnnotationDefinitionsModal
              onClose={() => this.closeEditDefinitionsModal()}
              annotationDefinitions={annotationDefinitions}
              setAnnotationDefinitions={(v) =>
                this.setState({ annotationDefinitions: v })
              }
            ></EditAnnotationDefinitionsModal>
          )}
          {creatingDefinition && (
            <AddAnnotationDefinitionModal
              annotationDefinitions={annotationDefinitions}
              cohortId={cohortReview.cohortId}
              onCancel={() => this.setState({ creatingDefinition: false })}
              onCreate={(ad) => this.definitionCreated(ad)}
              workspace={this.props.workspace}
            ></AddAnnotationDefinitionModal>
          )}
        </React.Fragment>
      );
    }
  }
);
