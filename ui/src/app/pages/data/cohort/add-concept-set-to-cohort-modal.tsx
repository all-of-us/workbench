import * as React from 'react';
import { useParams } from 'react-router';
import { Dropdown } from 'primereact/dropdown';

import { ConceptSet, Criteria } from 'generated/fetch';

import { AlertWarning } from 'app/components/alert';
import { Button } from 'app/components/buttons';
import {
  Modal,
  ModalBody,
  ModalFooter,
  ModalTitle,
} from 'app/components/modals';
import { TooltipTrigger } from 'app/components/popups';
import { Spinner } from 'app/components/spinners';
import { saveCriteria } from 'app/pages/data/cohort/cohort-search';
import { conceptSetsApi } from 'app/services/swagger-fetch-clients';
import { MatchParams } from 'app/utils/stores';

const { useEffect, useState } = React;

export const AddConceptSetToCohortModal = ({ onClose }) => {
  const { ns, wsid } = useParams<MatchParams>();
  const [conceptSets, setConceptSets] = useState<ConceptSet[]>();
  const [selectedConceptSetIndex, setSelectedConceptSetIndex] =
    useState<number>(-1);
  const [loading, setLoading] = useState<boolean>(true);
  const [saveError, setSaveError] = useState<boolean>(false);
  const [saving, setSaving] = useState<boolean>(false);

  const getConceptSets = async () => {
    try {
      const conceptSetsResponse =
        await conceptSetsApi().getConceptSetsInWorkspace(ns, wsid);
      setConceptSets(conceptSetsResponse.items);
      setLoading(false);
    } catch (error) {
      console.error(error);
    }
  };

  useEffect(() => {
    getConceptSets();
  }, []);

  const getParamId = ({ code, conceptId, id, standard }: Criteria) => {
    return `param${conceptId ? conceptId + code : id}${standard}`;
  };

  const addConceptSetAsItem = async () => {
    setSaving(true);
    const selectedConceptSet = await conceptSetsApi().getConceptSet(
      ns,
      wsid,
      conceptSets[selectedConceptSetIndex].id
    );
    if (selectedConceptSet) {
      saveCriteria(
        selectedConceptSet.criteriums.map((crit) => ({
          parameterId: getParamId(crit),
          ...crit,
        }))
      );
    } else {
      setSaveError(true);
    }
    setSaving(false);
  };

  return (
    <Modal>
      <ModalTitle data-test-id='add-concept-title'>
        Concept sets created from this workspace
      </ModalTitle>
      {loading ? (
        <div style={{ display: 'flex', justifyContent: 'center' }}>
          <Spinner style={{ alignContent: 'center' }} />
        </div>
      ) : (
        <>
          <ModalBody>
            {saveError && (
              <AlertWarning>
                Sorry, the request cannot be completed. Please try again or
                contact Support in the left hand navigation.
              </AlertWarning>
            )}
            <Dropdown
              style={{
                height: '2.25rem',
                width: '100%',
              }}
              value={selectedConceptSetIndex}
              options={conceptSets.map((set: ConceptSet, i) => ({
                label: set.name,
                value: i,
              }))}
              onChange={(e) => setSelectedConceptSetIndex(e.value)}
              placeholder='Select Concept Set'
            />
          </ModalBody>
          <ModalFooter>
            <Button type='secondary' disabled={saving} onClick={onClose}>
              Cancel
            </Button>
            <TooltipTrigger
              content={<div>No concept set selected</div>}
              disabled={selectedConceptSetIndex > -1}
            >
              <Button
                style={{ marginLeft: '0.75rem' }}
                disabled={selectedConceptSetIndex === -1 || saving}
                onClick={() => addConceptSetAsItem()}
              >
                {saving && (
                  <Spinner size={16} style={{ marginRight: '0.25rem' }} />
                )}
                Add
              </Button>
            </TooltipTrigger>
          </ModalFooter>
        </>
      )}
    </Modal>
  );
};
