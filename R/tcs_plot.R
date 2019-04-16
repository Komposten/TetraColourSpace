#' Call this to start building a TCS plot.
#' Will clear all previously added TCS data.
tcs.begin <- function(colour_background = NULL, colour_wl_long = NULL, colour_wl_medium = NULL, colour_wl_short = NULL,
                      colour_wl_uv = NULL, colour_achro = NULL, colour_selection = NULL,
                      colour_highlight = NULL, colour_metric_line = NULL, colour_metric_fill = NULL)
{
  tcsEnv <<- new.env()
  tcsEnv$data <- c("<?xml version=\"1.0\"?>", "<data>")
  
  params <<- unlist(as.list(match.call()))[-1]
  
  if (length(params) > 0) 
  {
    tcsEnv$data <- c(tcsEnv$data, "<style>")
    for (i in seq_along(params))
    {
      name <- substring(names(params)[i], 8)
      value <- params[i]
      tcsEnv$data <- c(tcsEnv$data, sprintf("<colour id=\"%s\">%s</colour>", name, value))
    }
    tcsEnv$data <- c(tcsEnv$data, "</style>")
  }
}

#' Adds a number of points to the current plot.
#' @param data TCS data containing the points to add.
#' Must either be a colspace object from the package pavo,
#' or a data frame/matrix with the first three columns being
#' theta, phi and r (magnitude).
#' @param name The name of the group of points.
#' @param colours A vector specifying the colours of the points.
#' @param shape The shape to use for the points.
tcs.points <- function(data, labels = NULL, name = NULL, colours = "#000", shape = c("sphere", "box", "pyramid"))
{
  if (!exists("tcsEnv"))
    stop("Must call tcs.begin before tcs.points!")
  
  if (is(data, "colspace"))
  {
    data <- data[,c("h.theta", "h.phi", "r.vec")]
  }
  
  colours <- rep(colours, length.out = nrow(data))
  
  if (is.null(labels))
  {
    labels <- row.names(data)
  }
  
  if (!is.null(name))
    tcsEnv$data <- c(tcsEnv$data, paste0("<group name=\"", name, "\" shape=\"", shape[1], "\">"))
  else
    tcsEnv$data <- c(tcsEnv$data, paste0("<group shape=\"", shape[1], "\">"))
  tcsEnv$data <- c(tcsEnv$data, unlist(lapply(1:nrow(data), function(i)
  {
    position <- paste(data[i,], collapse = ",")
    return(paste0("<point ", "name=\"", labels[i], "\" colour=\"", colours[i], "\" position=\"", position, "\"/>"))
  })))
  tcsEnv$data <- c(tcsEnv$data, "</group>")
}

#' Adds one or more volumes to the current plot.
#' @param data A list of TCS data containing the points to form the volumes from.
#' Each element in the list must either be a colspace object from the package pavo,
#' or a data frame/matrix with the first three columns being
#' theta, phi and r (magnitude).
#' @param colours A vector specifying the colours of the volumes.
tcs.volumes <- function(data, colours = "#000")
{
  if (!exists("tcsEnv"))
    stop("Must call tcs.begin before tcs.volumes!")
  if (!is.list(data))
    stop("data must be a list!")
  
  colours <- rep(colours, length.out = length(data))
  
  invisible(lapply(1:length(data), function(i)
  {
    tcs.volume(data[[i]], colours[i])
  }))
}

#' Adds a volume mesh to the plot based on a number of points.
#' @param data TCS data containing the points to base the volume on.
#' Must either be a colspace object from the package pavo,
#' or a data frame/matrix with the first three columns being
#' theta, phi and r (magnitude).
#' @param colours A vector specifying the colours of the volumes.
tcs.volume <- function(data, colour = "#000")
{
  if (!exists("tcsEnv"))
    stop("Must call tcs.begin before tcs.volume!")
  
  if (is(data, "colspace"))
  {
    data <- data[,c("h.theta", "h.phi", "r.vec")]
  }
  
  tcsEnv$data <- c(tcsEnv$data, paste0("<volume colour=\"", colour, "\">"))
  tcsEnv$data <- c(tcsEnv$data, unlist(apply(data, 1, paste, collapse = ",")))
  tcsEnv$data <- c(tcsEnv$data, "</volume>")
}

#' Saves the current TCS data to a file, launches the plot viewer,
#' and removes the TCS data storage.
#' @param file The output file to save the TCS data to.
#' @param jarPath Path to the plot viewer .jar file.
tcs.end <- function(file, jarPath, outputPath = NULL, async = F)
{
  if (!exists("tcsEnv"))
    stop("Must call tcs.begin before tcs.end!")
  
  tcsEnv$data <- c(tcsEnv$data, "</data>")
  
  file.create(file)
  connection <- file(file)
  writeLines(tcsEnv$data, connection)
  close(connection)
  
  rm(tcsEnv, envir = .GlobalEnv)
  
  tcs.launch(file, jarPath, outputPath, async)
}

#' Launches the specified jar-file with the provided graph file and output path.
tcs.launch <- function(file, jarPath, outputPath = NULL, async = F)
{
  if (is.null(outputPath))
    command <- sprintf("java -jar \"%s\" \"%s\"", jarPath, file)
  else
    command <- sprintf("java -jar \"%s\" \"%s\" \"%s\"", jarPath, file, outputPath)
  
  system(command, wait = !async)
}