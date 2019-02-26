#!/usr/bin/env ruby

# A script to kick off a Workbench CircleCI run without pushing a commit to the master branch.
#
# Use this to test local CircleCI config changes by actually running a job on CircleCI.
#
# Source: https://gist.github.com/masonmark/9b68f0b291c7daae74b770538ab699f8
#
# A couple gotchas:
#
# - You must generate a personal API token by visiting https://circleci.com/account/api. Store the token in your
#     environment as CIRCLE_API_TOKEN.
# - This doesn't work for workflows, only for jobs.
# - It will ONLY run a job named 'build'!
# - This seems to use an undocumented call to the CircleCI API where the form variable 'config' is sent to the CircleCI
#     server. This worked as of Feb 2019, but may go away (or be replaced with a better alternative) at any point.
#
# So, to test a given job, edit the config.yml to rename that job to 'build', and run this script pointing at your
# current GitHub branch (or at master, if your changes are only to config.yml).
#
# Example usage (from project root dir):
#
# $ export CIRCLE_API_TOKEN=[API token goes here]
# $ .circleci/run_circle_ci.rb gj/circle-mem

require 'pathname'

PATH_TO_ME      = Pathname.new File.expand_path(__FILE__)
PATH_TO_ROOT    = PATH_TO_ME.parent.parent

Dir.chdir PATH_TO_ROOT

github_organization = 'all-of-us'
github_project      = 'workbench'
circle_ci_api_token = ENV['CIRCLE_API_TOKEN']
existing_git_branch = ARGV[0]

cmd = [
  "curl ",
  "-X POST",
  "--form config=@.circleci/config.yml",
  "--form notify=false",
  "https://circleci.com/api/v1.1/project/github/#{github_organization}/#{github_project}/tree/#{existing_git_branch}?circle-token=#{circle_ci_api_token}"
].join(' ')

puts ''
puts '➡️  ' + cmd
puts ''

Kernel.exec cmd

# FIXME: might be nice to not use Kernel.exec above, instead capture the output (which contains a JSON struct including the build number), and then open the build's specific URL:
# https://circleci.com/gh/#{github_organization}/#{github_project}/#{build_number}