/** *****************************************************************************
  * Copyright (c) 2016 Red Hat Inc. and others.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
  * Mickael Istria (Red Hat Inc.) - initial implementation
  * ******************************************************************************/
package com.github.gtache.client

import java.io.{IOException, InputStream, OutputStream}
import java.net.URI

import org.eclipse.lsp4j.jsonrpc.messages.Message
import org.eclipse.lsp4j.services.LanguageServer

/**
  * A class representing a stream connection
  */
trait StreamConnectionProvider {
  @throws[IOException]
  def start(): Unit

  def getInputStream: InputStream

  def getOutputStream: OutputStream

  /**
    * User provided initialization options.
    */
  def getInitializationOptions(rootUri: URI): Any = null

  def stop(): Unit

  /**
    * Allows to hook custom behavior on messages.
    *
    * @param message        a message
    * @param languageServer the language server receiving/sending the message.
    * @param rootURI
    */
  def handleMessage(message: Message, languageServer: LanguageServer, rootURI: URI): Unit = {
  }
}
