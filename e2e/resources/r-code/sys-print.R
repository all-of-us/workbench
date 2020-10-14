print_env_var <- function(var) {
    value <- Sys.getenv(var, unset=NA)
    print(value)
    return(value)
}

vars <- c('OWNER_EMAIL',
          'WORKSPACE_CDR',
          'WORKSPACE_CDR_VERSION_ID',
          'WORKSPACE_NAMESPACE',
          'GOOGLE_PROJECT', # same as WORKSPACE_NAMESPACE
          'CLUSTER_NAME',
          'WORKSPACE_BUCKET')

vals <- sapply(vars, print_env_var)

# only print success if all env vars are present
if (!anyNA(vals)) {
    print('success',quote=FALSE)
}