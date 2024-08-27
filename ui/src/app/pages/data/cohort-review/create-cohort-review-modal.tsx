import * as React from 'react';
import { useParams } from 'react-router';
import { validate } from 'validate.js';

import { AlertWarning } from 'app/components/alert';
import { Button } from 'app/components/buttons';
import { NumberInput, TextInput, ValidationError } from 'app/components/inputs';
import {
  Modal,
  ModalBody,
  ModalFooter,
  ModalTitle,
} from 'app/components/modals';
import { Spinner } from 'app/components/spinners';
import { cohortReviewApi } from 'app/services/swagger-fetch-clients';
import { reactStyles, summarizeErrors } from 'app/utils';
import { currentCohortReviewStore } from 'app/utils/navigation';
import { MatchParams } from 'app/utils/stores';

const { useState } = React;

const styles = reactStyles({
  title: {
    color: '#302973',
    fontSize: '1.35rem',
    fontWeight: 200,
  },
  body: {
    lineHeight: '1.5rem',
  },
  input: {
    height: '1.875rem',
    width: '15rem',
  },
  cancel: {
    color: '#000000',
    fontWeight: 'bold',
    marginRight: '1.5rem',
    padding: '0.375rem 0.75rem',
    letterSpacing: '1.25px',
  },
  create: {
    background: '#302C71',
    marginLeft: '0.75rem',
  },
  disabled: {
    background: '#9b9b9b',
    color: '#c9c8c8',
    border: '1px soilid #9b9b9b',
    marginLeft: '0.75rem',
  },
});

export const CreateCohortReviewModal = ({
  canceled,
  cohortName,
  created,
  existingNames,
  participantCount,
}) => {
  const { ns, terraName, cid } = useParams<MatchParams>();
  const [createError, setCreateError] = useState(false);
  const [creating, setCreating] = useState(false);
  const [numberOfParticipants, setNumberOfParticipants] = useState(undefined);
  const [reviewName, setReviewName] = useState(undefined);
  const [reviewNameTouched, setReviewNameTouched] = useState(false);

  const validators = {
    reviewName: {
      presence: { allowEmpty: false },
      format: {
        pattern: /^[^\/]*$/,
        message: "can't contain a slash",
      },
      exclusion: {
        within: existingNames,
        message: 'already exists',
      },
    },
    numberOfParticipants: {
      numericality: {
        onlyInteger: true,
        greaterThan: 0,
        lessThanOrEqualTo: Math.min(participantCount, 10000),
        message:
          'must be a whole number 1 - ' +
          Math.min(participantCount, 10000).toLocaleString(),
      },
    },
  };

  const errors = validate(
    { reviewName: reviewName?.trim(), numberOfParticipants },
    validators
  );

  const createDisabled = !reviewName || !numberOfParticipants || errors;

  const createReview = () => {
    setCreating(true);
    setCreateError(false);
    const request = {
      name: reviewName.trim(),
      size: parseInt(numberOfParticipants, 10),
    };

    cohortReviewApi()
      .createCohortReview(ns, terraName, +cid, request)
      .then((response) => {
        currentCohortReviewStore.next(response);
        setCreating(false);
        created(response);
      })
      .catch((error) => {
        console.error(error);
        setCreateError(true);
        setCreating(false);
      });
  };

  return (
    <Modal onRequestClose={() => canceled()}>
      <ModalTitle style={styles.title}>Create Review Set</ModalTitle>
      {createError && (
        <AlertWarning>
          Sorry, the request cannot be completed. Please try again or contact
          Support in the left hand navigation.
        </AlertWarning>
      )}
      <ModalBody style={styles.body}>
        <TextInput
          autoFocus
          id='new-review-name'
          placeholder='COHORT REVIEW NAME'
          onChange={(v) => {
            setReviewName(v);
            setReviewNameTouched(true);
          }}
        />
        <ValidationError>
          {summarizeErrors(reviewNameTouched && errors?.reviewName)}
        </ValidationError>
        <div style={{ margin: '0.75rem 0' }}>
          Cohort {cohortName} has {participantCount.toLocaleString() + ' '}
          participants for possible review. How many would you like to review?
          {participantCount > 10000 && <span> (max 10,000)</span>}
        </div>
        <NumberInput
          value={numberOfParticipants}
          style={styles.input}
          placeholder='NUMBER OF PARTICIPANTS'
          onChange={(v) => setNumberOfParticipants(v)}
        />
        <ValidationError>
          {summarizeErrors(
            numberOfParticipants !== undefined && errors?.numberOfParticipants
          )}
        </ValidationError>
      </ModalBody>
      <ModalFooter>
        <Button
          style={styles.cancel}
          type='link'
          disabled={creating}
          onClick={() => canceled()}
        >
          CANCEL
        </Button>
        <Button
          style={createDisabled ? styles.disabled : styles.create}
          type='primary'
          disabled={createDisabled || creating}
          onClick={() => createReview()}
        >
          {creating && (
            <Spinner
              size={16}
              style={{ marginRight: '0.375rem', marginLeft: '-0.375rem' }}
            />
          )}
          CREATE SET
        </Button>
      </ModalFooter>
    </Modal>
  );
};
