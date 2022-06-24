Install dependencies:
```
yarn install
```

Service Account key for calling impersonation APIs:
```
export SA_KEY_JSON="$(<sa-key.json)"
```

Make sure impersonation works:
```
node src/impersonate.js "$SA_KEY_JSON" all-of-us-workbench-test puppeteer-tester-6@fake-research-aou.org
```

Tell Puppeteer where your Chrome lives:
```
export PUPPETEER_EXECUTABLE_PATH=/path/to/some/chromium/binary
```

Sanity checks:
```
yarn test tests/sanity.browser.test.js

HEADLESS=false yarn test tests/sanity.browser.test.js
```

Full suite:
```
yarn test
```
