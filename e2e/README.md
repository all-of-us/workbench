# All-of-Us Test Automation with Puppeteer in Chrome or Chromium

Puppeteer `e2e` end-to-end integration tests. These tests are run in CircleCI for every PR and merge to
master ([example](https://app.circleci.com/pipelines/github/all-of-us/workbench/4074/workflows/ca636d7c-8c11-463e-bfdc-39ea63b6df52/jobs/100294))
.

* See [Puppeteer API](https://github.com/puppeteer/puppeteer/blob/v5.0.0/docs/api.md). Puppeteer supports the
  latest [maintenance LTS](https://github.com/nodejs/Release#release-schedule) version of Node.
* Use [jest-puppeteer](https://github.com/smooth-code/jest-puppeteer) preset to configure `jest` to run Pupppeteer
  tests. Puppeteer `browser`, `context` and `page` variables are global variables exposed by jest-puppeteer preset.
* AoU tests run in Chromium by default.

## Set up project

* Clone github project: `git clone git@github.com:all-of-us/workbench.git`

* Install required node libraries
    - change to e2e dir `cd e2e`
    - run cmd `yarn install`
* Compile `yarn compile`

## Test Users

e2e tests are run using pre-existing users (rather than by generating new users)
so we must supply their credentials using an environment property file `.env`. See [.env.sample](.env.sample) for an
example.

### CircleCI Test Users

**DO NOT**
use these users for local development.

All users have bypassed access modules. Free credits: $800.
See [doc](https://docs.google.com/document/d/1rbAlU6CVgfh6R_o2BdD476AXrZR6qB7167yrhMf4KMA/edit?usp=sharing).

* Master branch (after merge)
    - `puppeteer-tester-1@fake-research-aou.org`
    - `puppeteer-collaborator-1@fake-research-aou.org`
    - `puppeteer-reader-1@fake-research-aou.org`
    - `puppeteer-writer-1@fake-research-aou.org`
    - `puppet-access-tester-1@fake-research-aou.org`
* Presubmit (PR testing on local ui server connected to test api server)
    - `puppeteer-pr-tester-1@fake-research-aou.org`
    - `puppeteer-pr-collaborator-1@fake-research-aou.org`
    - `puppeteer-pr-reader-1@fake-research-aou.org`
    - `puppeteer-pr-writer-1@fake-research-aou.org`
    - `puppet-access-tester-1@fake-research-aou.org`
* Staging env (releases)
    - `puppeteer-tester-1@staging.fake-research-aou.org`
    - `puppeteer-collaborator-1@staging.fake-research-aou.org`
    - `puppeteer-reader-1@staging.fake-research-aou.org`
    - `puppeteer-writer-1@staging.fake-research-aou.org`
    - `puppet-access-tester-1@staging.fake-research-aou.org`
* Perf env (releases)
    - `[TODO]@perf.fake-research-aou.org`
    - `puppetciperfreader@perf.fake-research-aou.org`
    - `puppetciperfreader@perf.fake-research-aou.org`
    - `puppetciperfwriter1@perf.fake-research-aou.org`
    - TBD (access test user)

## Running Tests on Localhost

### Local Test Users

Developers' Local environments share a Google Suite domain with the Test environment, so users with matching emails
share the same Terra and Google cloud resources between these environments. To avoid contention, it's necessary to
create some test users unique to the local environment. This is the same process as creating other local test users.
Ensure that local test users are 2FA-Bypassed. Fill out the environment property file `.env` with local test users
credentials.

e2e tests require 5 test users:

- USER_NAME: Default test user
- WRITER_USER: Share workspace to this test user with WRITER role
- READER_USER: Share workspace to this test user with READER role
- COLLABORATOR_USER: Share workspace to this test user with OWNER role
- ACCESS_TEST_USER: User to be modified outside of e2e tests to prepare for access testing

Fill out local test user credentials

- Copy `.env.sample`, save as `.env`.
- Update `.env` with your local user emails.

## Command line tests are run using [Yarn](https://classic.yarnpkg.com/en/)

**To see the list of available Yarn commands** <div class="text-blue">`yarn run`</div>

### Examples

* Run one test in **headless mode** (with invisible browser
  execution) <div class="text-blue">`yarn test [TEST_FILE]` </div>
* Run one test in **headful mode** (with the browser interactions visible to
  you) <div class="text-blue">`yarn test:debug [TEST_FILE]` </div>
* Run all tests in parallel in **headless mode** on "test" environment <div class="text-blue">`yarn test`</div>
* Run one test on your local server <div class="text-blue">`yarn test-local [TEST_FILE]` </div>
* Run tests against a local UI and API server (RW-6132 will eliminate this distinction):
    * Stop your local API server
    * `e2e$ yarn impersonate-test-users`
    * Restart your local API server: `api$ ./project.rb run-api`
    * `e2e$ yarn test-local-devup`
* Run one test in headless Chrome with node `--inspect-brk` argument. It pauses test playback at breakpoints which is
  useful for debugging or/and writing new tests <div class="text-blue">`yarn test:debugTest [TEST_FILE]` </div>
    * Navigate to `chrome://inspect` after launching the tests to debug test code
* If you don't want to use the `.env` file, you can also specify `USER_NAME` and `PASSWORD` as environment
  variables. <div class="text-blue">`USER_NAME=<YOUR_USERID> PASSWORD=<YOUR_USER_PASSWORD> yarn test-local tests/user/login.spec.ts`</div>

### Debugging

- To suspend test execution during test playback, insert one line of
  code <div class="text-blue">`await jestPuppeteer.debug();`</div>

(Refers to https://github.com/smooth-code/jest-puppeteer#put-in-debug-mode for details)

## Features

* One retry running failed test
  by [jest-circus](https://github.com/facebook/jest/blob/f45d1c939cbf55a71dbfdfc316d2be62b590197f/docs/JestObjectAPI.md#jestretrytimes)
  .
* Take screenshot and save the html page to `/log` directory if test fails.

## Test Development

The `e2e` code has the following directory structure:

* `app/component` - interactions with UI React Components such as `Modal` and `Table`
* `app/element` - interactions with UI Form Elements such as `Button` and `TextBox`
* `app/page` - high-level access to pages in the UI through awareness of page layouts and constants
* `resources` - test data and configurations
* `tests` - **the e2e tests**, sorted by theme
* `utils` - test utility functions

### Development Tips

#### Initial state

Page tests typically begin by navigating to the desired page (which will differ depending on the page) and then ensuring
that the desired page has loaded.

Example:

```   
await signInWithAccessToken(page);
await navigation.navMenu(page, NavLink.PROFILE);
profilePage = new ProfilePage(page);
await profilePage.waitForLoad();
``` 

`page` is the global `jest` context. When constructing a RW page (such as `ProfilePage`), it is saved as `this.page`.

The `ProfilePage` is only accessible to signed-in users, so we start by signing in.

We navigate to the `ProfilePage` by accessing the correct link from the `navMenu` (the Hamburger menu).

A `ProfilePage` object will give us helpful accessors for our test, so we create one using the global `page` context and
use `ProfilePage`'s knowledge of what a successful page load looks like to determine that it has done so.

#### Page accessors

A common testing need is to find an element or component on a page and interact with it in some way:
click it, read its value, or wait for a state update. These should have well-defined accessors, so please create them as
needed.

Example:

```
async getFirstNameInput(): Promise<Textbox> {
    return Textbox.findByName(this.page, {dataTestId: DataTestIdAlias.FirstName});
}
```

Many element types have a `findByName()` method to locate them using `XPathOptions` to match by name,
text, `data-test-id`, or other means. These are all ultimately converted
to [XPath](https://www.w3schools.com/xml/xpath_syntax.asp) notation, which you can use directly if you need additional
control, but using the higher-level finders (or adding one!) is preferred.

```
async getResearchBackgroundTextarea(): Promise<Textarea> {
    return Textarea.findByName(this.page, {normalizeSpace: LabelAlias.ResearchBackground});
}
```

`normalizeSpace` is one of the `XPathOptions` worth calling out explicitly here: use this to match text which may be
broken up by formatting. This skips over the formatting and only matches the text.

#### Spinners/Loading

Many UI actions take some time to execute and display a spinner. Use `waitWhileLoading()` to pause test execution while
there is a spinner present.

#### type() and paste()

`paste()` is an action available to all elements, which directly sets the value of the element to the string in the
parameter. This is suitable for large amounts of text and executes quickly.

Some elements also have `type()` which simulates individual keypresses, with a small delay between them.

#### Test strings

`str-utils` has useful functions for generating random text that fits expected patterns: `makeString()`,
`makeUrl()`, `makeWorkspaceName()`, etc

#### Slow Motion

If the screens go by too quickly when debugging by running headful, set the `PUPPETEER_SLOWMO` env variable. Units are
in ms between keypresses.

#### Debugging test failures

Failed tests capture the screen state at time of failure, in screenshot and HTML form. In addition, the command line
jest runner will often give an XPath associated with the failure. These can be combined to learn more about the failure:

1. Load the HTML into Chrome
2. Open the Chrome Javascript console
3. Execute an XPath search: `$x.(<XPath from jest>)`

