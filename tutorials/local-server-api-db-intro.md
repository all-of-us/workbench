# Local Server

## Overview 

In this module we will bring up the API server and do some basic exploration of the database and make an API call against the server. The goal is to help you gain an understanding of how the back end and data is structured and to start gaining an understanding the tools, scripts and languages we use to aid us in development.

## Tasks

### (LOCAL-SERVER-1) Bring up the local server

./project.rb dev-up

This will bring up a server with API endpoints and a local database.


#### Dependencies
You should have the following installed on your machine for this to work

- Ruby installed
- docker installed and running
- docker-compose
- docker-sync (`gem install docker-sync` or `gem install --user-install docker-sync` or non-root installs)

Having ruby gems installed on your path is a good idea as well - adding this to your profile or rc file will help to find the ruby gems:

```sh
if which ruby >/dev/null && which gem >/dev/null; then
  PATH="$(ruby -r rubygems -e 'puts Gem.user_dir')/bin:$PATH"
fi
```

### (LOCAL-SERVER-2) Using a REST client (e.g. insomnia) make a request to the local server
  1. Start with the /v1/profile endpoint (http://localhost:8081/v1/profile)
  2. What was the response?

### (LOCAL-SERVER-3) Repeat LOCAL-SERVER-2 With a Bearer Token

Generate a bearer token and pass this along to the endpoint
Ge
  1. What happened, what was the error code and response now?
  2. What do you think you need to do in order to log in?

### (LOCAL-SERVER-4) Initial Database Exploration

  Run `./project.rb connect-to-db`
  This will connect to your local database

  1. Run a query to display all of the tables in the database
  2. Which tables do you think are involved with the error you encountered in task LOCAL-SERVER-3?

### (LOCAL-SERVER-5) Further DB and Endpoint Exploration

  1. Using SQL, look at the schema of the `user` table
  2. Insert a row with your test account data to the `user` table
  3. Run the `/v1/profile` endpoint in your REST client
  4. What is the response?

### Comprehension Questions

  These questions are not meant to be a quiz, but are intended to help you think about the code base and learn. If you cannot answer any of them feel free to ask a team member for help.

  If you are familiar with any of the technologies we are questioning you on, feel free to skip or refresh your knowledge depending on your level of familiarity.

  For any open ended questions we do not want you to feel like you are taking a quiz, but do encourage you to actually write down your answer(s) - even if no one will look at them! There are studies showing that the act of re-representing information in your own written words helps to reinforce learning.

  1. In this exercise you ran two Ruby commands: `dev-up` and `connect-to-db`. 
     1. Where in the codebase are those registered and defined?
     2. What is the name of the database that `connect-to-db` connects to?
  2. How are those commands invoked in the ruby script / what would the stack trace look like?
  3. We had to install [docker-sync](https://docker-sync.readthedocs.io/en/latest/getting-started/configuration.html) in order to run the local API server. Write in your own words: what purpose does docker-sync serve? [Hint](https://dev.to/kovah/cut-your-docker-for-mac-response-times-in-half-with-docker-sync-1e8j)?
  4. What does [docker-compose](https://docs.docker.com/compose/) do? Why do you think we use `docker-compose`?