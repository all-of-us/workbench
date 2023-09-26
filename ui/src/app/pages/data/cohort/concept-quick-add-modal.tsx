import * as React from 'react';
import { useParams } from 'react-router';
import * as fp from 'lodash/fp';
import { InputTextarea } from 'primereact/inputtextarea';

import { Criteria } from 'generated/fetch';

import { AlertWarning } from 'app/components/alert';
import { Button, Clickable } from 'app/components/buttons';
import { ClrIcon } from 'app/components/icons';
import { CheckBox } from 'app/components/inputs';
import {
  Modal,
  ModalBody,
  ModalFooter,
  ModalTitle,
} from 'app/components/modals';
import { TooltipTrigger } from 'app/components/popups';
import { Spinner } from 'app/components/spinners';
import { initGroup } from 'app/pages/data/cohort/cohort-search';
import { initItem } from 'app/pages/data/cohort/search-group-list';
import { searchRequestStore } from 'app/pages/data/cohort/search-state.service';
import { domainToTitle, generateId } from 'app/pages/data/cohort/utils';
import { cohortBuilderApi } from 'app/services/swagger-fetch-clients';
import colors, { colorWithWhiteness } from 'app/styles/colors';
import { withCurrentCohortSearchContext } from 'app/utils';
import { currentCohortSearchContextStore } from 'app/utils/navigation';
import { MatchParams } from 'app/utils/stores';

const { useState } = React;

const infoTooltip = (
  <div style={{ marginLeft: '0.75rem' }}>
    <ul style={{ listStylePosition: 'outside' }}>
      <li>
        Concepts can be found by entering a list of concept ids or concepts
        codes
      </li>
      <li>
        Ids or codes can be entered one-per-line or as a comma-separated list
      </li>
      <li>
        If concepts for multiple domains are returned, a separate line item will
        be added to the cohort for each domain
      </li>
    </ul>
  </div>
);

export const ConceptQuickAddModal = withCurrentCohortSearchContext()(
  ({ cohortContext, onClose }) => {
    const { ns, wsid } = useParams<MatchParams>();
    const [conceptIdInput, setConceptIdInput] = useState<string>();
    const [matchedConcepts, setMatchedConcepts] = useState<Criteria[]>();
    const [selectedConcepts, setSelectedConcepts] = useState<number[]>();
    const [error, setError] = useState<boolean>(false);
    const [loading, setLoading] = useState<boolean>(false);

    const lookupConcepts = async () => {
      setError(false);
      setLoading(true);
      const conceptsRequest = {
        conceptKeys: conceptIdInput
          .split(/[\n,]/)
          .map((conceptId) => conceptId.trim())
          .filter((conceptId) => !!conceptId),
      };
      try {
        const matchedConceptsResp =
          await cohortBuilderApi().findCriteriaByConceptIdsOrConceptCodes(
            ns,
            wsid,
            conceptsRequest
          );
        setSelectedConcepts(
          matchedConceptsResp.items.map((concept, index) => index)
        );
        setMatchedConcepts(matchedConceptsResp.items);
      } catch (err) {
        console.error(err);
        setError(true);
      } finally {
        setLoading(false);
      }
    };

    const getParamId = ({ code, conceptId, id, standard }: Criteria) => {
      return `param${conceptId ? conceptId + code : id}${standard}`;
    };

    const addConceptsAsItems = () => {
      const { groupId, role } = cohortContext;
      // Save only selected concepts
      const filteredConcepts = matchedConcepts.filter((concept, index) =>
        selectedConcepts.includes(index)
      );
      // Group concepts by domain
      const conceptsByDomain = fp.groupBy('domainId', filteredConcepts);
      const searchRequest = searchRequestStore.getValue();
      // Add a separate line item for each domain
      const items = Object.values(conceptsByDomain).map((domainConcepts) => ({
        ...initItem(generateId('items'), domainConcepts[0].domainId),
        searchParameters: domainConcepts.map((crit) => ({
          parameterId: getParamId(crit),
          ...crit,
        })),
      }));
      if (groupId) {
        const groupIndex = searchRequest[role].findIndex(
          (grp) => grp.id === groupId
        );
        if (groupIndex > -1) {
          searchRequest[role][groupIndex].items = [
            ...searchRequest[role][groupIndex].items,
            ...items,
          ];
        }
      } else {
        searchRequest[role].push(initGroup(role, items));
      }
      searchRequestStore.next(searchRequest);
      currentCohortSearchContextStore.next(undefined);
    };

    const onSelectAllChange = (checked: boolean) => {
      setSelectedConcepts(
        checked ? matchedConcepts.map((concept, index) => index) : []
      );
    };

    const onCheckboxChange = (index: number, checked: boolean) => {
      setSelectedConcepts((prevState) =>
        checked
          ? [...prevState, index]
          : prevState.filter((selection) => selection !== index)
      );
    };

    return (
      <Modal width={650}>
        <ModalTitle
          style={{ marginBottom: '0.5rem' }}
          data-test-id='add-concept-title'
        >
          Concept Lookup
          <Clickable style={{ float: 'right' }} onClick={() => onClose()}>
            <ClrIcon shape='times' size='24' style={{ color: colors.accent }} />
          </Clickable>
        </ModalTitle>
        <ModalBody style={{ color: colors.primary }}>
          {error && (
            <AlertWarning>
              Sorry, the request cannot be completed. Please try again or
              contact Support in the left hand navigation.
            </AlertWarning>
          )}
          <div
            style={{
              fontSize: '14px',
              fontWeight: 600,
              marginBottom: '0.5rem',
            }}
          >
            Concept IDs or Codes
            <TooltipTrigger side='top' content={infoTooltip}>
              <ClrIcon
                style={{
                  color: colorWithWhiteness(colors.accent, 0.1),
                  marginLeft: '0.375rem',
                }}
                className='is-solid'
                shape='info-standard'
              />
            </TooltipTrigger>
          </div>
          <InputTextarea
            rows={3}
            style={{ height: '4rem', width: '50%' }}
            value={conceptIdInput}
            disabled={loading}
            onChange={(e) => setConceptIdInput(e.target.value)}
          />
          <div>
            <Button
              type='secondary'
              style={{
                border: `1px solid ${
                  !conceptIdInput || loading ? colors.disabled : colors.accent
                }`,
                color:
                  !conceptIdInput || loading ? colors.disabled : colors.accent,
                height: '2.5rem',
                marginTop: '0.75rem',
                padding: '0 1rem',
              }}
              disabled={!conceptIdInput || loading}
              onClick={() => lookupConcepts()}
            >
              {loading && (
                <Spinner size={16} style={{ marginRight: '0.25rem' }} />
              )}
              Lookup
            </Button>
          </div>
          {!loading && !!matchedConcepts && (
            <div style={{ fontSize: '12px', marginTop: '0.5rem' }}>
              {matchedConcepts.length === 0 ? (
                <div style={{ color: colors.warning, fontWeight: 500 }}>
                  <ClrIcon
                    shape='exclamation-triangle'
                    size='20'
                    className='is-solid'
                  />{' '}
                  No matching concepts found
                </div>
              ) : (
                <>
                  <div style={{ color: colors.select, fontWeight: 500 }}>
                    <ClrIcon
                      shape='check-circle'
                      size='20'
                      className='is-solid'
                    />{' '}
                    {matchedConcepts.length.toLocaleString()} Concept
                    {matchedConcepts.length > 1 && 's'} found
                  </div>
                  <table style={{ textAlign: 'left', width: '100%' }}>
                    <thead>
                      <tr>
                        <th>
                          <CheckBox
                            checked={
                              selectedConcepts.length === matchedConcepts.length
                            }
                            onChange={(v) => onSelectAllChange(v)}
                            manageOwnState={false}
                          />
                        </th>
                        <th style={{ width: '40%' }}>Name</th>
                        <th style={{ width: '20%' }}>Concept Id</th>
                        <th style={{ width: '20%' }}>Code</th>
                        <th style={{ width: '20%' }}>Domain</th>
                      </tr>
                    </thead>
                    <tbody>
                      {matchedConcepts.map((concept, index) => (
                        <tr key={index}>
                          <td>
                            <CheckBox
                              checked={selectedConcepts.includes(index)}
                              onChange={(v) => onCheckboxChange(index, v)}
                              manageOwnState={false}
                            />
                          </td>
                          <td>{concept.name}</td>
                          <td>{concept.conceptId}</td>
                          <td>{concept.code}</td>
                          <td>{domainToTitle(concept.domainId)}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </>
              )}
            </div>
          )}
        </ModalBody>
        <ModalFooter>
          {!!matchedConcepts && (
            <Button
              style={{ marginLeft: '0.75rem' }}
              onClick={() => addConceptsAsItems()}
              disabled={selectedConcepts.length === 0}
            >
              Add
            </Button>
          )}
        </ModalFooter>
      </Modal>
    );
  }
);
