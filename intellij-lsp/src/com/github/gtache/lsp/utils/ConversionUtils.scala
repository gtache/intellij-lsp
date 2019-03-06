package com.github.gtache.lsp.utils

import org.eclipse.lsp4j.{Location, LocationLink}

object ConversionUtils {

  implicit def locationToLocationLink(l: Location): LocationLink = {
    val lLink = new LocationLink()
    lLink.setTargetUri(l.getUri)
    lLink.setTargetRange(l.getRange)
    lLink
  }

  implicit def locationLinkToLocation(l: LocationLink): Location = {
    val loc = new Location()
    loc.setUri(l.getTargetUri)
    loc.setRange(l.getTargetRange)
    loc
  }

}
