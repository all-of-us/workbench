import * as fp from 'lodash/fp';
import * as React from 'react';
import {validate} from 'validate.js';

import {Button, Clickable} from 'app/components/buttons';
import {styles as headerStyles} from 'app/components/headers';
import {ClrIcon} from 'app/components/icons';
import {Select, TextInput} from 'app/components/inputs';
import {Modal, ModalBody, ModalFooter, ModalTitle} from 'app/components/modals';
import {TooltipTrigger} from 'app/components/popups';
import {cohortAnnotationDefinitionApi} from 'app/services/swagger-fetch-clients';
import {reactStyles, summarizeErrors, withUrlParams} from 'app/utils/index';
import {AnnotationType, CohortAnnotationDefinition} from 'generated/fetch';

const styles = reactStyles({
  editRow: {
    display: 'flex', alignItems: 'center',
    height: '2rem', borderBottom: '1px solid #c3c3c3'
  },
  defName: {
    flex: 1, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis'
  }
});

export const AddAnnotationDefinitionModal = withUrlParams()(class extends React.Component<
  {
    annotationDefinitions: CohortAnnotationDefinition[],
    onCancel: Function,
    onCreate: Function,
    urlParams: any
  },
  {name: string, annotationType: AnnotationType, enumValues: string[], saving: boolean}
> {
  constructor(props) {
    super(props);
    this.state = {
      name: '',
      annotationType: AnnotationType.STRING,
      enumValues: [],
      saving: false
    };
  }

  async create() {
    try {
      const {onCreate, urlParams: {ns, wsid, cid}} = this.props;
      const {name, annotationType, enumValues} = this.state;
      this.setState({saving: true});
      const newDef = await cohortAnnotationDefinitionApi().createCohortAnnotationDefinition(
        ns, wsid, cid, {
          cohortId: cid,
          columnName: name,
          annotationType,
          enumValues: annotationType === AnnotationType.ENUM ? enumValues : undefined
        }
      );
      onCreate(newDef);
    } catch (error) {
      console.error(error);
    } finally {
      this.setState({saving: false});
    }
  }

  render() {
    const {annotationDefinitions, onCancel} = this.props;
    const {name, annotationType, enumValues, saving} = this.state;
    const errors = validate({name, annotationType, enumValues}, {
      name: {
        presence: {allowEmpty: false},
        exclusion: {
          within: annotationDefinitions.map(({columnName}) => columnName),
          message: 'already exists'
        }
      },
      enumValues: {
        custom: {fn: (value, key, obj) => {
          if (obj.annotationType === AnnotationType.ENUM && !value.length) {
            return 'must be provided';
          }
        }}
      }
    });
    return <Modal loading={saving}>
      <ModalTitle>Create a Cohort-wide Annotation</ModalTitle>
      <ModalBody>
        <div style={headerStyles.formLabel}>Name:</div>
        <TextInput
          maxLength={255}
          value={name}
          onChange={v => this.setState({name: v})}
        />
        <div style={{...headerStyles.formLabel, marginTop: '1rem'}}>Type:</div>
        <Select
          value={annotationType}
          options={[
            {value: AnnotationType.STRING, label: 'Text'},
            {value: AnnotationType.ENUM, label: 'Enumeration'},
            {value: AnnotationType.DATE, label: 'Date'},
            {value: AnnotationType.BOOLEAN, label: 'Boolean'},
            {value: AnnotationType.INTEGER, label: 'Integer'},
          ]}
          onChange={v => this.setState({annotationType: v})}
        />
        {annotationType === AnnotationType.ENUM && <React.Fragment>
          <div style={{...headerStyles.formLabel, marginTop: '1rem'}}>Values:</div>
          {enumValues.map((enumValue, i) => {
            return <div key={i} style={{display: 'flex', alignItems: 'center', height: '2rem'}}>
              <TextInput
                value={enumValue}
                onChange={v => this.setState(fp.set(['enumValues', i], v))}
              />
              <Clickable
                onClick={() => this.setState(fp.update('enumValues', fp.pullAt(i)))}
                style={{marginLeft: '0.5rem'}}
              ><ClrIcon shape='minus-circle' size={18} /></Clickable>
            </div>;
          })}
          <Button
            type='link'
            style={{padding: '0.25rem 0'}}
            onClick={() => this.setState(fp.update('enumValues', ev => ev.concat([''])))}
          >
            <ClrIcon shape='plus-circle' /> Add a value
          </Button>
        </React.Fragment>}
      </ModalBody>
      <ModalFooter>
        <Button type='secondary' onClick={onCancel}>Cancel</Button>
        <TooltipTrigger content={summarizeErrors(errors)}>
          <Button
            style={{marginLeft: '0.5rem'}}
            disabled={!!errors || saving}
            onClick={() => this.create()}
          >Create</Button>
        </TooltipTrigger>
      </ModalFooter>
    </Modal>;
  }
});

export const EditAnnotationDefinitionsModal = withUrlParams()(class extends React.Component<
  {
    onClose: Function,
    annotationDefinitions: CohortAnnotationDefinition[],
    setAnnotationDefinitions: Function,
    urlParams: any
  },
  {editId: number, editValue: string, busy: boolean}
> {
  constructor(props) {
    super(props);
    this.state = {editId: undefined, editValue: '', busy: false};
  }

  async delete(id) {
    try {
      const {
        annotationDefinitions, setAnnotationDefinitions,
        urlParams: {ns, wsid, cid}
      } = this.props;
      this.setState({busy: true});
      await cohortAnnotationDefinitionApi().deleteCohortAnnotationDefinition(ns, wsid, cid, id);
      setAnnotationDefinitions(
        fp.remove({cohortAnnotationDefinitionId: id}, annotationDefinitions)
      );
    } catch (error) {
      console.error(error);
    } finally {
      this.setState({busy: false});
    }
  }

  async rename() {
    try {
      const {
        annotationDefinitions,
        setAnnotationDefinitions,
        urlParams: {ns, wsid, cid}
      } = this.props;
      const {editId, editValue} = this.state;
      if (editValue && !fp.some({columnName: editValue}, annotationDefinitions)) {
        this.setState({busy: true});
        const {annotationType, etag} = annotationDefinitions
          .find(annotationDef => annotationDef.cohortAnnotationDefinitionId === editId);
        const newDef = await cohortAnnotationDefinitionApi().updateCohortAnnotationDefinition(
          ns, wsid, cid, editId,
          {cohortId: cid, columnName: editValue, annotationType, etag}
        );
        setAnnotationDefinitions(
          annotationDefinitions.map(oldDef => {
            return oldDef.cohortAnnotationDefinitionId === editId ? newDef : oldDef;
          })
        );
      }
    } catch (error) {
      console.error(error);
    } finally {
      this.setState({busy: false, editId: undefined});
    }
  }

  render() {
    const {onClose, annotationDefinitions} = this.props;
    const {editId, editValue, busy} = this.state;
    return <Modal loading={busy}>
      <ModalTitle>Edit Cohort-wide Annotations</ModalTitle>
      <ModalBody>
        {annotationDefinitions.map(({cohortAnnotationDefinitionId: id, columnName}) => {
          return <div key={id} style={styles.editRow}>
            {editId === id ?
              <TextInput
                maxLength={255}
                autoFocus
                value={editValue}
                onChange={v => this.setState({editValue: v})}
                onBlur={() => this.rename()}
                onKeyPress={e => {
                  if (e.key === 'Enter') {
                    this.rename();
                  }
                }}
              />
            :
              <div style={styles.defName}>
                {columnName}
              </div>
            }
            <Button
              type='link'
              disabled={busy}
              style={{flex: 'none', margin: '0 0.5rem'}}
              onClick={() => this.setState({editId: id, editValue: columnName})}
            >Rename</Button>
            <div>|</div>
            <Button
              type='link'
              disabled={busy}
              style={{flex: 'none', marginLeft: '0.5rem'}}
              onClick={() => this.delete(id)}
            >Delete</Button>
          </div>;
        })}
        {!annotationDefinitions.length &&
          <div style={{fontStyle: 'italic'}}>
            No review annotations defined.
          </div>
        }
      </ModalBody>
      <ModalFooter>
        <Button disabled={busy} onClick={onClose}>Close</Button>
      </ModalFooter>
    </Modal>;
  }
});
