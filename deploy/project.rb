#!/usr/bin/env ruby

require_relative "../aou-utils/workbench"
require_relative "libproject/deploy.rb"
require_relative "libproject/some.js"

Workbench.handle_argv_or_die(__FILE__)
