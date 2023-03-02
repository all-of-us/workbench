# Generating Emails

## Background

Emails, unless they are plain text, are notoriously tricky to format consistently across the
plethora of email clients available today.

## Solution

[MJML](https://mjml.io/) is a node package that allows you to write emails in
a [custom markup language](https://documentation.mjml.io/) that can be converted into HTML that will
be rendered [consistently across many email clients](https://mjml.io/faq#email-clients)

## Installation

We do not currently have the infrastructure to support node packages in our backend, so you will
need to install MJML locally:

```npm install -g mjml@4.13.0```

*Note: We do not currently have a package.json for our api, but for consistency, please use version
4.13.0 of MJML.*

## Helpful Commands

There are two commands that you should be aware of.

First, in order to convert an mjml file to an html file, you can run:

```mjml index.mjml index.html```

If you want to update an html file everytime that an mjml file is updated, you can run:

```mjml --watch index.mjml index.html```

If you are interested in more of MJML's commands, please refer to the
[CLI documentation.](https://documentation.mjml.io/#command-line-interface)

