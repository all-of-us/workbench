import * as React from 'react';
import { useEffect, useState } from 'react';
import { useHistory } from 'react-router-dom';

import { useQuery } from 'app/components/app-router';
import { NewUserSatisfactionSurveyModal } from 'app/components/new-user-satisfaction-survey-modal';
import { surveysApi } from 'app/services/swagger-fetch-clients';
import { authStore, profileStore, useStore } from 'app/utils/stores';

const ONE_TIME_CODE_QUERY_PARAMETER = 'surveyCode';

export const withNewUserSatisfactionSurveyModal = (WrappedComponent) => {
  return ({ ...props }) => {
    const [showModal, setShowModal] = useState(false);
    const { reload } = useStore(profileStore);
    const [surveyCode, setSurveyCode] = useState(null);
    const query = useQuery();
    const history = useHistory();

    useEffect(() => {
      const codeParam = query.get(ONE_TIME_CODE_QUERY_PARAMETER);
      if (codeParam == null) {
        return;
      }
      setSurveyCode(codeParam);

      // Deleting the query parameter prevents certain flows (such as logging out) from re-triggering a cancelled survey
      query.delete(ONE_TIME_CODE_QUERY_PARAMETER);
      history.replace({
        search: query.toString(),
      });

      surveysApi()
        .validateOneTimeCodeForNewUserSatisfactionSurvey(codeParam)
        .then(setShowModal);
    }, []);

    return (
      <>
        <WrappedComponent {...props} />
        {showModal && (
          <NewUserSatisfactionSurveyModal
            onCancel={() => {
              setShowModal(false);
            }}
            onSubmitSuccess={() => {
              setShowModal(false);
              if (authStore.get().isSignedIn) {
                reload();
              }
            }}
            createSurveyApiCall={(data) =>
              surveysApi().createNewUserSatisfactionSurveyWithOneTimeCode({
                createNewUserSatisfactionSurvey: data,
                oneTimeCode: surveyCode,
              })
            }
          />
        )}
      </>
    );
  };
};
