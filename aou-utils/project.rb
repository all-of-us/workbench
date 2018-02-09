#!/usr/bin/env ruby

require_relative "workbench"

# There are no commands for this workspace yet; we just
# use this to initialize git hooks and submodules.
Workbench.handle_argv_or_die(__FILE__)

