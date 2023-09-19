# Using Packages from @terra-ui-packages

Terra UI publishes a [set of packages](https://github.com/DataBiosphere/terra-ui/tree/dev/packages) on every commit. These are stored in a public registry for use by anyone. We can and do use packages from this registry in our codebase.

## Using Packages

The [packages](https://github.com/DataBiosphere/terra-ui/tree/dev/packages) directory in the [Terra UI GitHub repo](https://github.com/DataBiosphere/terra-ui) contains a set of directories each with its own package.json file. Here is a current (2023-08-31) listing:
```
/git-repos/terra-ui $ grep -R \"name\": packages
packages/test-utils/package.json:  "name": "@terra-ui-packages/test-utils",
packages/components/package.json:  "name": "@terra-ui-packages/components",
packages/core-utils/package.json:  "name": "@terra-ui-packages/core-utils",
packages/build-utils/package.json:  "name": "@terra-ui-packages/build-utils",
```

These packages are published at a Broad-owned registry. Because this registry is public, the JSON package descriptions are available:
```
$ curl https://us-central1-npm.pkg.dev/dsp-artifact-registry/terra-ui-packages/@terra-ui-packages/core-utils
{"name":"@terra-ui-packages/core-utils","dist-tags":{"latest":"0.0.5"},...}
```

The `gitHead` field contains the commit corresponding to the published version. I find it useful to browse the Terra UI source tree at the version's commit to see exactly which functions/components are available in the published version. For example, @terra-ui-packages/core-utils at version 0.0.5 has a gitHead (hash of a git commit) c79428c00ec8c32994294257ffb628e3d1d9b10f. The source tree at that commit is available here:

https://github.com/DataBiosphere/terra-ui/tree/c79428c00ec8c32994294257ffb628e3d1d9b10f

Once you have selected a function or component to use, there are only two steps:
1. Add the package to our dependencies (e.g., `"@terra-ui-packages/core-utils": "0.0.5"`) if it hasn't already been done, then;
2. Import and use it (e.g., `import { cond } from '@terra-ui-packages/core-utils';`)

## Working on Packages

You can use checked-out source code as a package to test unpublished versions. To do this, first clone the Terra UI GitHub repository:

`$ git clone https://github.com/DataBiosphere/terra-ui.git`

### Development Cycle

After each change, build the package:

`terra-ui-repo-dir $ yarn workspace @terra-ui-packages/core-utils build`

Re-add the package from source as a dependency:

`aou-repo-ui-dir $ yarn add /path/to/terra-ui-repo-dir/packages/core-utils`

This should allow you to use the updated package code:

`$ yarn run tsc`
