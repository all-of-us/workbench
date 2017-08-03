source setup_vars.sh
export WORKBENCH_PASSWORD=${WORKBENCH_PASSWORD:=workbench!pwd}
export DB_SERVER=${DB_SERVER:=aouwb-api-db:3306}
export DB_CONNECTION_STRING=${DB_CONNECTION_STRING:=jdbc:mysql://${DB_SERVER}/${DB_NAME}}
