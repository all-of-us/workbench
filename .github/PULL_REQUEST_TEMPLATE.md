<!--
Reminder: If you decide to merge with any failing checks, add an explanatory comment before doing so.
-->

---
**PR checklist**

- [ ] I have included an issue ID or "no ticket" in the PR title as outlined in [CONTRIBUTING.md](https://github.com/all-of-us/workbench/blob/main/.github/CONTRIBUTING.md).
- [ ] I have included a risk tag of the form `[risk=no|low|moderate|severe]` in the PR title as outlined in [CONTRIBUTING.md](https://github.com/all-of-us/workbench/blob/main/.github/CONTRIBUTING.md).
- [ ] I have manually tested this change and my testing process is described above.
- [ ] This change includes appropriate automated tests, and I have documented any behavior that cannot be tested with code.
- [ ] I have added explanatory comments where the logic is not obvious.
- One or more of the following is true:
  - [ ] This change is intended to complete a JIRA story, so I have checked that all AC are met for that story.
  - [ ] This change fixes a bug, so I have ensured the steps to reproduce are in the Jira ticket or provided above.
  - [ ] This change impacts deployment safety (e.g. removing/altering APIs which are in use), so I have documented the impacts in the description.
  - [ ] This change includes a new feature flag, so I have created and linked new JIRA tickets to (a) turn on the feature flag and (b) remove it later.
  - [ ] This change modifies the UI, so I have taken screenshots or recordings of the new behavior and notified the PO and UX designer in [Slack](https://pmi-engteam.slack.com/archives/C02MWP2RN5P).
  - [ ] This change modifies API behavior, so I have run the relevant E2E tests locally because API changes are not covered by our PR checks.
  - [ ] None of the above apply to this change.
