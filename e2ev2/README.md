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

Set the hostname of the UI you are testing against:
```
export UI_HOSTNAME=all-of-us-workbench-test.appspot.com
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

## Useful Resources

Jest's `expect` global provides assertions:

https://jestjs.io/docs/expect

Options for `yarn test ...`:

https://jestjs.io/docs/cli

## Contributing Notes

Jest's default test timeout is five seconds. If a test times out, it is a bug in the test. When a test fails because of a timeout, it should fail on a particular action, such as `page.waitForSelector`. In the common case, we expect these calls to return quickly. When we are expecting an operation to take some time, we set that timeout explicitly and give that particular test more time to finish. This convention provides higher granularity about why a test fails so that we can make the correct adjustments.

These tests exercise the live system over TCP connections that can disconnect against dependent services (e.g. GCP) that occasionally fail. We should not expect them to always pass. Instead, we should expect them to transparently report failures and allow rapid re-runs.
