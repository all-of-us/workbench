import {mount} from 'enzyme';
import * as React from 'react';

import {ConceptSetsApiStub} from 'testing/stubs/concept-sets-api-stub';
import {ConceptSetsApi} from 'generated/fetch/api';
import {currentWorkspaceStore} from 'app/utils/navigation';
import {CreateConceptSetModal} from './component';
import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {DomainStubVariables} from 'testing/stubs/concepts-api-stub';
import {workspaceDataStub} from 'testing/stubs/workspaces-api-stub';
import {waitOneTickAndUpdate} from 'testing/react-test-helpers';


describe('CreateConceptSetModal', () => {
  let conceptSetsApi: ConceptSetsApiStub;
  const stubDomains = DomainStubVariables.STUB_DOMAINS.map(d => d.domain.toString());
  const component = () => {
    return mount(<CreateConceptSetModal
        onCreate={() => {}} onClose={() => {}}
        conceptDomainList={DomainStubVariables.STUB_DOMAINS}
        existingConceptSets={conceptSetsApi.conceptSets}/>);
  };

  beforeEach(() => {
    conceptSetsApi = new ConceptSetsApiStub();
    registerApiClient(ConceptSetsApi, conceptSetsApi);
    currentWorkspaceStore.next(workspaceDataStub);
  });

  it('gets domain list', () => {
    const wrapper = component();
    const domains = wrapper.find('[data-test-id="domain-options"]')
        .find('option').map(v => v.text());
    expect(domains).toEqual(stubDomains);
  });

  it('saves concept set information', async () => {
    const wrapper = component();
    const csName = 'new-name';
    wrapper.find('[data-test-id="concept-set-name"]').find('input')
        .simulate('change', {target: {value: csName}});

    wrapper.find('[data-test-id="save-concept-set"]').first().simulate('click');
    await waitOneTickAndUpdate(wrapper);
    expect(conceptSetsApi.conceptSets.map(cs => cs.name)).toContain(csName);
  });

  it('does not allow save for a concept set that already exists', async () => {
    const wrapper = component();
    const csName = conceptSetsApi.conceptSets[0].name;
    wrapper.find('[data-test-id="concept-set-name"]').find('input')
        .simulate('change', {target: {value: csName}});
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.find('[data-test-id="save-concept-set"]')
        .first().prop('disabled')).toBe(true);

  });

});
