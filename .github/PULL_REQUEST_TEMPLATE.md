Description:
<!--
Replace this template with your PR description.
Please remember to keep in mind the security levels outlined in
[CONTRIBUTING.md](https://github.com/all-of-us/workbench/blob/main/.github/CONTRIBUTING.md) and to
include a risk tag of the form `[risk=no|low|moderate|severe]` in the PR title

* **no**: None
* **low**: Low chance of potential impact to, or exposure of patient data
* **moderate**: Moderate chance of potential impact to, or exposure of patient data
* **severe**: Severe chance of potential impact to, or exposure of patient data

Please also:

* Get thumbs from reviewer(s)
* Verify all tests go green, including CI tests
-->


---
**PR checklist**

- [ ] This PR meets the Acceptance Criteria in the JIRA story
- [ ] The JIRA story has been moved to Dev Review
- [ ] This PR includes appropriate unit tests
- [ ] I have run and tested this change locally
- [ ] I have run the E2E tests on ths change against my local UI and/or API server with `yarn test-local` or [yarn test-local-devup](https://github.com/all-of-us/workbench/blob/main/e2e/README.md#examples)
- [ ] If this includes a UI change, I have taken screen recordings or screenshots of the new behavior and notified the PO and UX designer
- [ ] If this includes an API change, I have updated the appropriate Swagger definitions and notified API consumers
- [ ] If this includes a new feature flag, I have created and linked new JIRA tickets to (a) turn on the feature flag and (b) remove it later
