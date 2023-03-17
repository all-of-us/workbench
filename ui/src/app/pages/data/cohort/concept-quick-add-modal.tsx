import * as React from 'react';
import { useParams } from 'react-router';
import { InputTextarea } from 'primereact/inputtextarea';

import { Criteria } from 'generated/fetch';

import { AlertWarning } from 'app/components/alert';
import { Button, Clickable } from 'app/components/buttons';
import {
  Modal,
  ModalBody,
  ModalFooter,
  ModalTitle,
} from 'app/components/modals';
import { TooltipTrigger } from 'app/components/popups';
import { Spinner } from 'app/components/spinners';
import { saveCriteria } from 'app/pages/data/cohort/cohort-search';
import { cohortBuilderApi } from 'app/services/swagger-fetch-clients';
import { MatchParams } from 'app/utils/stores';
import { ClrIcon } from 'app/components/icons';
import colors from 'app/styles/colors';

const { useState } = React;

export const ConceptQuickAddModal = ({ onClose }) => {
  const { ns, wsid } = useParams<MatchParams>();
  const [conceptIdInput, setConceptIdInput] = useState<string>();
  const [matchedConcepts, setMatchedConcepts] = useState<Criteria[]>();
  const [error, setError] = useState<boolean>(false);
  const [loading, setLoading] = useState<boolean>(false);

  const lookupConcepts = async () => {
    setError(false);
    setLoading(true);
    const conceptsRequest = {
      conceptKeys: conceptIdInput.split(/[\n,]/)
    };
    try {
      const matchedConceptsResp = await cohortBuilderApi().findCriteriaByConceptIdsOrConceptCodes(
        ns,
        wsid,
        conceptsRequest
      );
      setMatchedConcepts(matchedConceptsResp.items);
    } catch (error) {
      console.error(error);
      setError(true);
    } finally {
      setLoading(false);
    }
  };

  const addConceptsAsItem = () => {};

  return (
    <Modal>
      <ModalTitle data-test-id='add-concept-title'>
        Concept ID Lookup
        <Clickable style={{ float: 'right' }} onClick={() => onClose()}>
          <ClrIcon shape='times' size='24' style={{ color: colors.accent }} />
        </Clickable>
      </ModalTitle>
      <ModalBody>
        {loading && (
          <div style={{ display: 'flex', justifyContent: 'center' }}>
            <Spinner style={{ alignContent: 'center' }} />
          </div>
        )}
        {error && (
          <AlertWarning>
            Sorry, the request cannot be completed. Please try again or contact
            Support in the left hand navigation.
          </AlertWarning>
        )}
        <InputTextarea
          rows={3}
          value={conceptIdInput}
          disabled={loading}
          onChange={(e) => setConceptIdInput(e.target.value)}
        />
        <Button
          type='secondary'
          disabled={!conceptIdInput || loading}
          onClick={() => lookupConcepts()}
        >
          {loading && (
            <Spinner size={16} style={{ marginRight: '0.25rem' }} />
          )}
          Lookup
        </Button>
        {!!matchedConcepts && (
          <table>
            <thead>
              <tr>
                <th>Concept Id</th>
                <th>Code</th>
                <th>Name</th>
              </tr>
            </thead>
            <tbody>
              {matchedConcepts.map((concept, index) => (
                <tr>
                  <td>{concept.conceptId}</td>
                  <td>{concept.code}</td>
                  <td>{concept.name}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </ModalBody>
      <ModalFooter>
        {!!matchedConcepts && (
          <Button
            style={{ marginLeft: '0.75rem' }}
            onClick={() => addConceptsAsItem()}
          >
            Add
          </Button>
        )}
      </ModalFooter>
    </Modal>
  );
};
