#' Call this to start building a TCS plot.
#' Will clear all previously added TCS data.
#' 
#' @param colour_background The graph's background colour.
#' @param colour_text The graph's text colour.
#' @param colour_wl_long The colour to use for the long wavelength corner.
#' @param colour_wl_medium The colour to use for the medium wavelength corner.
#' @param colour_wl_short The colour to use for the short wavelength corner.
#' @param colour_wl_uv The colour to use for the UV wavelength corner.
#' @param colour_achro The colour to use for the achromatic centre.
#' @param colour_selection The colour to use for the indicator around the selected point.
#' @param colour_highlight The colour to use for the indicator around the highlighted point.
#' @param colour_metric_line The colour to use for the lines when displaying metrics.
#' @param colour_metric_fill The colour to use for the filled arcs when displaying metrics.
#' @param point_size The default size to use for data points.
#' @param corner_size The size to use for the spheres in the corners of the tetrahedron.
#' @param sphere_quality The quality of the spheres, as an integer >= 5.
#' @param render_mode The render mode to use, either 'fast' or 'slow'.
#' 
tcs.begin <- function(colour_background = NULL, colour_text = NULL,
                      colour_wl_long = NULL, colour_wl_medium = NULL,
                      colour_wl_short = NULL, colour_wl_uv = NULL,
                      colour_achro = NULL, colour_selection = NULL,
                      colour_highlight = NULL, colour_metric_line = NULL,
                      colour_metric_fill = NULL, point_size = NULL,
                      corner_size = NULL, sphere_quality = NULL,
                      render_mode = c("fast", "slow"))
{
  tcsEnv <<- new.env()
  tcsEnv$data <- c("<?xml version=\"1.0\"?>", "<data>")
  
  params <<- unlist(as.list(match.call()))[-1]
  
  if (length(params) > 0) 
  {
    tcsEnv$data <- c(tcsEnv$data, "<style>")
    for (i in seq_along(params))
    {
      name <- names(params)[i]
      value <- params[i][1]
      
      if (substring(name, 1, 6) == "colour")
      {
        name <- substring(name, 8)
        format <- '<colour id="%s">%s</colour>'
      }
      else
      {
        format <- '<setting id="%s">%s</setting>'
      }
      
      tcsEnv$data <- c(tcsEnv$data, sprintf(format, name, value))
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
#' @param size The size of the points. Overrides the size specified
#' in tcs.begin() for this data group.
tcs.points <- function(data, labels = NULL, name = NULL, colours = "#000",
                       shape = c("sphere", "box", "pyramid"), size = NULL)
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
  
  groupAttributes <- character(0)
  if (!is.null(name))
    groupAttributes <- c(groupAttributes, sprintf('name="%s"', name))
  if (!is.null(size))
    groupAttributes <- c(groupAttributes, sprintf('size="%s"', size))
  groupAttributes <- c(groupAttributes, sprintf('shape="%s"', shape[1]))
  
  openTag <- sprintf('<group %s>', paste(groupAttributes, collapse = " "))
  
  tcsEnv$data <- c(tcsEnv$data, openTag)
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
#' @param colour A character specifying the colour of the volume.
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
#' @param jarPath Path to the TetraColourSpace .jar file. May be NULL
#' (in which case the plot is not displayed).
tcs.end <- function(file, jarPath = NULL, outputPath = NULL, async = F)
{
  if (!exists("tcsEnv"))
    stop("Must call tcs.begin before tcs.end!")
  
  tcsEnv$data <- c(tcsEnv$data, "</data>")
  
  file.create(file)
  connection <- file(file)
  writeLines(tcsEnv$data, connection)
  close(connection)
  
  rm(tcsEnv, envir = .GlobalEnv)
  
  if (!is.null(jarPath))
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