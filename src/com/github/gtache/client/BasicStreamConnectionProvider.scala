package com.github.gtache.client

import java.io.{InputStream, OutputStream}

case class BasicStreamConnectionProvider(in: InputStream, out: OutputStream) extends StreamConnectionProvider {
  override def start(): Unit = {}

  override def getInputStream: InputStream = in

  override def getOutputStream: OutputStream = out

  override def stop(): Unit = {
    in.close()
    out.close()
  }
}
