const Sequencer = require('@jest/test-sequencer').default;

// Reproduced exact order observed for RW-7364.
const weights = {
  "src/app/pages/analysis/runtime-panel.spec.tsx": 1,
  "src/app/pages/workspace/workspace-edit.spec.tsx": 2,
  "src/app/pages/admin/admin-institution-edit.spec.tsx": 3,
  "src/app/pages/data/data-set/dataset-page.spec.tsx": 4,
  "src/app/pages/homepage/data-access-requirements.spec.tsx": 5,
  "src/app/components/copy-modal.spec.tsx": 6,
  "src/app/pages/access/access-renewal.spec.tsx": 7,
  "src/app/components/help-sidebar.spec.tsx": 8,
  "src/app/pages/login/account-creation/account-creation-institution.spec.tsx": 9,
  "src/app/utils/leo-runtime-initializer.spec.tsx": 10,
  "src/app/pages/data/data-set/export-dataset-modal.spec.tsx": 11,
  "src/app/pages/analysis/notebook-redirect.spec.tsx": 12,
  "src/app/cohort-search/attributes-page/attributes-page.component.spec.tsx": 13,
  "src/app/utils/resources.spec.tsx": 14,
  "src/app/pages/workspace/workspace-share.spec.tsx": 15,
  "src/app/utils/access-utils.spec.tsx": 16,
  "src/app/pages/data/data-set/genomic-extraction-modal.spec.tsx": 17,
  "src/app/pages/workspace/workspace-about.spec.tsx": 18,
  "src/app/pages/workspace/create-billing-account-modal.spec.tsx": 19,
  "src/app/pages/data/concept/concept-search.spec.tsx": 20,
  "src/app/pages/login/account-creation/account-creation.spec.tsx": 21,
  "src/app/pages/profile/data-user-code-of-conduct.spec.tsx": 22,
  "src/app/utils/subscribable.spec.tsx": 23,
  "src/app/pages/login/sign-in.spec.tsx": 24,
  "src/app/pages/data/concept/concept-add-modal.spec.tsx": 25,
  "src/app/pages/data/data-page.spec.tsx": 26,
  "src/app/utils/runtime-utils.spec.tsx": 27,
  "src/app/components/app-router.spec.tsx": 28,
  "src/app/pages/profile/profile-component.spec.tsx": 29,
  "src/app/pages/homepage/quick-tour-and-videos.spec.tsx": 30,
  "src/app/pages/homepage/homepage.spec.tsx": 31,
  "src/app/pages/workspace/workspace-nav-bar.spec.tsx": 32,
  "src/app/pages/data/concept/concept-homepage.spec.tsx": 33,
  "src/app/pages/profile/demographic-survey.spec.tsx": 34,
  "src/app/utils/cdr-versions.spec.tsx": 35,
  "src/app/components/render-resource-card.spec.tsx": 36,
  "src/app/pages/profile/data-access-panel.spec.tsx": 37,
  "src/app/pages/workspace/workspace-library.spec.tsx": 38,
  "src/app/pages/workspace/workspace-list.spec.tsx": 39,
  "src/app/pages/login/account-creation/account-creation-tos.spec.tsx": 40,
  "src/app/pages/homepage/quick-tour-modal.spec.tsx": 41,
  "src/app/components/search-input.spec.tsx": 42,
  "src/app/cohort-search/cohort-search/cohort-search.component.spec.tsx": 43,
  "src/app/cohort-search/tree-node/tree-node.component.spec.tsx": 44,
  "src/app/cohort-search/cohort-page/cohort-page.component.spec.tsx": 45,
  "src/app/components/error-handler.spec.tsx": 46,
  "src/app/components/login-gov-ial2-notification.spec.tsx": 47,
  "src/app/components/inputs.spec.tsx": 48,
  "src/app/pages/admin/admin-review-workspace.spec.tsx": 49,
  "src/app/cohort-search/search-group/search-group.component.spec.tsx": 50,
  "src/app/cohort-search/modifier-page/modifier-page.component.spec.tsx": 51,
  "src/app/components/side-nav.spec.tsx": 52,
  "src/app/components/data-set-reference-modal.spec.tsx": 53,
  "src/app/utils/authorities.spec.tsx": 54,
  "src/app/pages/workspace/workspace-card.spec.tsx": 55,
  "src/app/icons/edit.spec.tsx": 56,
  "src/app/pages/data/cohort-review/sidebar-content.component.spec.tsx": 57,
  "src/app/components/modals.spec.tsx": 58,
  "src/app/components/bug-report.spec.tsx": 59,
  "src/app/components/breadcrumb.spec.tsx": 60,
  "src/app/pages/login/account-creation/account-creation-survey.spec.tsx": 61,
  "src/app/pages/admin/admin-users.spec.tsx": 62,
  "src/app/pages/admin/admin-institution.spec.tsx": 63,
  "src/app/pages/data/cohort-review/cohort-review.spec.tsx": 64,
  "src/app/pages/homepage/recent-resources.spec.tsx": 65,
  "src/app/cohort-search/search-bar/search-bar.component.spec.tsx": 66,
  "src/app/cohort-search/search-group-list/search-group-list.component.spec.tsx": 67,
  "src/app/pages/analysis/notebook-list.spec.tsx": 68,
  "src/app/components/rename-modal.spec.tsx": 69,
  "src/app/pages/data/cohort-review/annotation-definition-modals.component.spec.tsx": 70,
  "src/app/pages/admin/admin-user-bypass.spec.tsx": 71,
  "src/app/pages/data/concept/concept-navigation-bar.spec.tsx": 72,
  "src/app/utils/retry.spec.tsx": 73,
  "src/app/utils/index.spec.tsx": 74,
  "src/app/pages/login/account-creation/account-creation-modals.spec.tsx": 75,
  "src/app/cohort-search/demographics/demographics.component.spec.tsx": 76,
  "src/app/cohort-search/selection-list/selection-list.component.spec.tsx": 77,
  "src/app/cohort-search/tree/tree.component.spec.tsx": 78,
  "src/app/components/confirm-delete-modal.spec.tsx": 79,
  "src/app/components/html-viewer.spec.tsx": 80,
  "src/app/pages/analysis/notebook-resource-card.spec.tsx": 81,
  "src/app/cohort-search/list-search/list-search.component.spec.tsx": 82,
  "src/app/pages/data/cohort-review/individual-participants-charts.spec.tsx": 83,
  "src/app/pages/login/login.spec.tsx": 84,
  "src/app/pages/data/cohort/cohort-resource-card.spec.tsx": 85,
  "src/app/cohort-search/search-group-item/search-group-item.component.spec.tsx": 86,
  "src/app/components/combo-chart.component.spec.tsx": 87,
  "src/app/cohort-search/gender-chart/gender-chart.component.spec.tsx": 88,
  "src/app/pages/analysis/new-notebook-modal.spec.tsx": 89,
  "src/app/pages/homepage/two-factor-auth-modal.spec.tsx": 90,
  "src/app/cohort-search/overview/overview.component.spec.tsx": 91,
  "src/app/icons/scroll.spec.tsx": 92
};

const weigh = (path) => {
  const p = path.substring(path.indexOf("src/app/"));
  if (!weights[p]) {
    console.log('miss: ', p);
    return 1000;
  }
  return weights[p];
};

// Alphabetical sort, per https://jestjs.io/docs/configuration#testsequencer-string
class CustomSequencer extends Sequencer {
  sort(tests) {
    // Test structure information
    // https://github.com/facebook/jest/blob/6b8b1404a1d9254e7d5d90a8934087a9c9899dab/packages/jest-runner/src/types.ts#L17-L21
    const copyTests = Array.from(tests);
    return copyTests.sort((testA, testB) => (weigh(testA.path) - weigh(testB.path)));
  }
}

module.exports = CustomSequencer;
