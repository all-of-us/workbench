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
- [ ] I have added explanatory comments where the logic is not obvious
- [ ] I have run and tested this change locally, and my testing process is described here
- [ ] If this includes a new feature flag, I have created and linked new JIRA tickets to (a) turn on the feature flag and (b) remove it later
- [ ] If this includes an API change, I have run the E2E tests on this change against my local server with [yarn test-local](https://github.com/all-of-us/workbench/blob/master/e2e/README.md#examples) because this PR won't be covered by the CircleCI tests 
- [ ] If this includes a UI change, I have taken screen recordings or screenshots of the new behavior and notified the PO and UX designer in [Slack](https://pmi-engteam.slack.com/archives/C02MWP2RN5P)
- [ ] If this change impacts deployment safety (e.g. removing/altering APIs which are in use) I have documented these in the description
- [ ] If this includes an API change, I have updated the appropriate Swagger definitions and updated the appropriate API consumers
  * AoU UI
  * [Perf tests](https://github.com/broadinstitute/mcnulty/blob/develop/src/test/scala/services/AoU.scala)
  * [Researcher Directory export](https://github.com/all-of-us/workbench/wiki/Researcher-Directory-(RDR-export))
  * Cron tasks - for Offline*Controllers
  * SumoLogic - for EgressAlert 
