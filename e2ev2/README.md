Install dependencies:
```
yarn install
```

Bearer token for calling impersonation APIs:
```
export BEARER_TOKEN=$(gcloud auth print-access-token --account=you@pmi-ops.org)
```

Make sure impersonation works:
```
node src/impersonate.js all-of-us-workbench-test puppeteer-tester-6@fake-research-aou.org
```

Sanity checks:
```
yarn test tests/sanity.browser.test.js

HEADLESS=false yarn test tests/sanity.browser.test.js
```
