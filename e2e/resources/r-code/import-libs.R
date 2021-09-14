install_if_missing_then_load <- function(package, ...) {
  if(!require(package, ...)) {
    install.packages(package, verbose=F)
  }
  require(package, quietly=TRUE, ...)
}

pkgs <- c(
  "data.table",
  "dplyr",
  "ggplot2",
  "ggthemes",
  "glue",
  "grid",
  "gridExtra",
  "jsonlite",
  "reticulate",
  "scales")

sapply(pkgs, install_if_missing_then_load, character.only=T)

library(data.table)
library(dplyr)
library(ggplot2)
library(ggthemes)
library(glue)
library(grid)
library(gridExtra)
library(jsonlite)
library(reticulate)
library(scales)

pd <- reticulate::import("pandas")

print('success',quote=FALSE)
