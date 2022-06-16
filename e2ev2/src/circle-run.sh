# https://github.com/nodejs/node/discussions/43184?sort=new
export OPENSSL_CONF=/dev/null # WTF?

yarn install
SA_KEY_JSON="$(echo "$GCLOUD_CREDENTIALS" \
  | openssl enc -d -md sha256 -aes-256-cbc -base64 -A -k "$GCLOUD_CREDENTIALS_KEY")"

mkdir screenshots
chmod 777 screenshots

docker run --name tc --rm -it -v "$PWD":/w -w /w -u node \
  -e PUPPETEER_EXECUTABLE_PATH=/usr/bin/chromium-browser \
  -e SA_KEY_JSON="$SA_KEY_JSON" \
  -e JEST_SILENT_REPORTER_DOTS=true \
  -e JEST_SILENT_REPORTER_SHOW_PATHS=true \
  --privileged \
  shivjm/node-chromium-alpine:16 \
  yarn test --reporters=jest-silent-reporter;

