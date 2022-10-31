A properly configured system should be able to run:
```
yarn test tests/sanity.browser.test.js
```

Let's give it a try:
```
% yarn test tests/sanity.browser.test.js
yarn run v1.22.18
$ jest tests/sanity.browser.test.js
/bin/sh: jest: command not found
error Command failed with exit code 127.
info Visit https://yarnpkg.com/en/docs/cli/run for documentation about this command.
```

Dependencies are not installed. Run:
```
yarn install
```

Let's try again:
```
% yarn test tests/sanity.browser.test.js
yarn run v1.22.18
$ jest tests/sanity.browser.test.js
 FAIL  tests/sanity.browser.test.js
  ✕ page loads (7 ms)
  ✕ view cookie policy page (1 ms)
  ✕ navigation to /workspaces redirects to sign-in page
  ✕ navigation to /profile redirects to sign-in page

  ● page loads

    Could not find expected browser (chrome) locally. Run `npm install` to download the correct Chromium revision (1002410).
```

We need to tell Puppeteer where your Chrome lives:
```
export PUPPETEER_EXECUTABLE_PATH=/path/to/some/chromium/binary
```

Again:
```
% yarn test tests/sanity.browser.test.js
<snip>
  ● view cookie policy page

    assert(received)

    Expected value to be equal to:
      true
    Received:
      undefined

    Message:
      API_PROXY_URL not defined
```

Point the tests at a running API Proxy. For example:
```
cd ../api-proxy
PROXY_PORT=8080 PROXY_MODE=replay-only node startproxy.mjs
```

`replay-only` means that requests missing handlers will produce a 500 error rather than forwarding the request to the API servier. `record-only` does the opposite. The default is to use handlers when they are available and forward requests that do not have a handler defined.

then:
```
export API_PROXY_URL=http://localhost:8080
```

Again:
```
% yarn test tests/sanity.browser.test.js
<snip>
    Message:
      UI_URL_ROOT not defined. Try: export UI_URL_ROOT=https://all-of-us-workbench-test.appspot.com
```

`UI_URL_ROOT` holds the code under test.
```
export UI_URL_ROOT=https://all-of-us-workbench-test.appspot.com
```

Should be good to go now:
```
% yarn test tests/sanity.browser.test.js
yarn run v1.22.18
$ jest tests/sanity.browser.test.js
 PASS  tests/sanity.browser.test.js (14.386 s)
  ✓ page loads (1370 ms)
  ✓ view cookie policy page (5326 ms)
  ✓ navigation to /workspaces redirects to sign-in page (3876 ms)
  ✓ navigation to /profile redirects to sign-in page (3530 ms)

Test Suites: 1 passed, 1 total
Tests:       4 passed, 4 total
Snapshots:   0 total
Time:        14.42 s
Ran all test suites matching /tests\/sanity.browser.test.js/i.
✨  Done in 14.99s.
```

Also try:
```
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
