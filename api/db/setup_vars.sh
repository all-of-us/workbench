export WORKBENCH_DB_USER=${WORKBENCH_DB_USER:=workbench}
export LIQUIBASE_DB_USER=${LIQUIBASE_DB_USER:=liquibase}
export ROOT_DB_USER=${ROOT_DB_USER:=root}
export DB_USER=${DB_USER:=${WORKBENCH_DB_USER}}
export DB_NAME=${DB_NAME:=workbench}
export DB_DRIVER=${DB_DRIVER:=com.mysql.jdbc.Driver}
