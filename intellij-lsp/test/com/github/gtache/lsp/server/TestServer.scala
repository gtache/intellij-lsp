package com.github.gtache.lsp.server

import java.util
import java.util.concurrent.CompletableFuture

import org.eclipse.lsp4j._
import org.eclipse.lsp4j.jsonrpc.messages
import org.eclipse.lsp4j.jsonrpc.messages.Either
import org.eclipse.lsp4j.services.{LanguageClient, LanguageServer, TextDocumentService, WorkspaceService}

import scala.collection.mutable.ListBuffer

class TestServer extends LanguageServer {

  import scala.collection.JavaConverters._

  private val commands = Array("do, did, done")
  private val completionItems = Array(
    {
      val a = new CompletionItem("A")
      a.setInsertText("CompletionA")
      a.setInsertTextFormat(InsertTextFormat.PlainText)
      a.setKind(CompletionItemKind.Enum)
      a.setDetail("DetailA")
      a.setDocumentation("DocumentationA")
      a
    }, {
      val b = new CompletionItem("B")
      b.setTextEdit(new TextEdit(new Range(new Position(0, 0), new Position(0, 0)), "CompletedB"))
      b.setAdditionalTextEdits(createList(new TextEdit(new Range(new Position(0, 20), new Position(0, 30)), "CompletedB")))
      b.setKind(CompletionItemKind.Class)
      b.setDetail("DetailB")
      b
    }, {
      val c = new CompletionItem("C")
      c.setCommand(new Command(commands(0), commands(0)))
      c.setDocumentation("DocumentationC")
      c
    }, {
      val d = new CompletionItem("D")
      d.setKind(CompletionItemKind.Method)
      d
    }
  )
  private val uris: ListBuffer[String] = ListBuffer()
  private val capabilities: ServerCapabilities = new ServerCapabilities()
  private val syncKindOptions = new TextDocumentSyncOptions
  capabilities.setCodeActionProvider(true)
  capabilities.setCodeLensProvider(new CodeLensOptions(true))
  capabilities.setCompletionProvider(new CompletionOptions(true, createList("^")))
  capabilities.setDefinitionProvider(true)
  capabilities.setDocumentFormattingProvider(true)
  capabilities.setDocumentHighlightProvider(true)
  capabilities.setDocumentLinkProvider(new DocumentLinkOptions(true))
  capabilities.setDocumentOnTypeFormattingProvider(new DocumentOnTypeFormattingOptions("°", createList("§")))
  capabilities.setDocumentRangeFormattingProvider(true)
  capabilities.setDocumentSymbolProvider(true)
  capabilities.setExecuteCommandProvider(new ExecuteCommandOptions(commands.toIndexedSeq.asJava))
  capabilities.setHoverProvider(true)
  capabilities.setReferencesProvider(true)
  capabilities.setRenameProvider(true)
  capabilities.setSignatureHelpProvider(new SignatureHelpOptions(createList("(")))
  private val positions: IndexedSeq[Position] = Stream.range(0, 5).flatMap(line => Stream.range(0, 30).map(col => new Position(line, col)).toIndexedSeq).toIndexedSeq
  syncKindOptions.setChange(TextDocumentSyncKind.Incremental)
  syncKindOptions.setOpenClose(true)
  syncKindOptions.setSave(new SaveOptions(true))
  syncKindOptions.setWillSave(true)
  syncKindOptions.setWillSaveWaitUntil(true)
  capabilities.setTextDocumentSync(syncKindOptions)
  capabilities.setWorkspaceSymbolProvider(true)
  private val ranges: IndexedSeq[Range] = positions.zip(positions.reverse).map(p => new Range(p._1, p._2))
  private val ws: WorkspaceService = new WorkspaceService {
    override def symbol(params: WorkspaceSymbolParams): CompletableFuture[util.List[_ <: SymbolInformation]] = {
      val symbols = Array(
        new SymbolInformation("SymbA", SymbolKind.Class, new Location(uris.head, ranges(0))),
        new SymbolInformation("SymbB", SymbolKind.Enum, new Location(uris.tail.head, ranges(1)))
      )
      if (params.getQuery.isEmpty) CompletableFuture.completedFuture(symbols.toIndexedSeq.asJava) else CompletableFuture.completedFuture(createList(symbols.head))
    }

    override def didChangeWatchedFiles(params: DidChangeWatchedFilesParams): Unit = {
      client.logMessage(new MessageParams(MessageType.Info, "DidChangeWatchedFiles " + params))
    }

    override def didChangeConfiguration(params: DidChangeConfigurationParams): Unit = {
      client.logMessage(new MessageParams(MessageType.Info, "didChangeConfiguration " + params))
    }
  }
  private val tds: TextDocumentService = new TextDocumentService {
    override def references(params: ReferenceParams): CompletableFuture[util.List[_ <: Location]] = {
      val uri = params.getTextDocument.getUri
      if (uri.contains("test1")) {
        CompletableFuture.completedFuture(createList(new Location(uri, ranges(0)), new Location(uri, ranges(1))))
      } else {
        CompletableFuture.completedFuture(createList(new Location(uris.filter(s => s != uri).head, ranges(3))))
      }
    }

    override def resolveCompletionItem(unresolved: CompletionItem): CompletableFuture[CompletionItem] = {
      CompletableFuture.completedFuture(unresolved)
    }

    override def codeLens(params: CodeLensParams): CompletableFuture[util.List[_ <: CodeLens]] = {
      CompletableFuture.completedFuture(createList(new CodeLens(ranges(0), new Command(commands(0), commands(0)), null)))
    }

    override def documentHighlight(position: TextDocumentPositionParams): CompletableFuture[util.List[_ <: DocumentHighlight]] = {
      CompletableFuture.completedFuture(createList(new DocumentHighlight(ranges(0), DocumentHighlightKind.Read),
        new DocumentHighlight(ranges(1), DocumentHighlightKind.Write),
        new DocumentHighlight(ranges(2), DocumentHighlightKind.Text)))
    }

    override def didChange(params: DidChangeTextDocumentParams): Unit = {
      client.logMessage(new MessageParams(MessageType.Warning, "DidChange"))
    }

    override def hover(position: TextDocumentPositionParams): CompletableFuture[Hover] = {
      val uri = position.getTextDocument.getUri
      if (uri.contains("test1")) {
        CompletableFuture.completedFuture(new Hover(createList(Either.forRight(new MarkedString("java", "***Bold*** *Italic*"))), ranges(0)))
      } else {
        CompletableFuture.completedFuture(new Hover(createList(Either.forLeft("This is hover"))))
      }
    }

    override def documentSymbol(params: DocumentSymbolParams): CompletableFuture[util.List[_ <: SymbolInformation]] = {
      val uri = params.getTextDocument.getUri
      if (uri.contains("test1")) {
        val symbols = Array(
          new SymbolInformation("SymbA", SymbolKind.Class, new Location(uri, ranges(0))),
          new SymbolInformation("SymbB", SymbolKind.Enum, new Location(uri, ranges(1)))
        )
        CompletableFuture.completedFuture(symbols.toIndexedSeq.asJava)
      } else {
        val nUri = uris.find(u => u != uri).get
        val symbols = Array(
          new SymbolInformation("SymbA", SymbolKind.Class, new Location(nUri, ranges(0))),
          new SymbolInformation("SymbB", SymbolKind.Enum, new Location(nUri, ranges(1)))
        )
        CompletableFuture.completedFuture(symbols.toIndexedSeq.asJava)
      }
    }

    override def didClose(params: DidCloseTextDocumentParams): Unit = {
      client.logMessage(new MessageParams(MessageType.Info, "didClose"))
    }

    override def didSave(params: DidSaveTextDocumentParams): Unit = {
      client.logMessage(new MessageParams(MessageType.Info, "DidSave"))
    }

    override def definition(position: TextDocumentPositionParams): CompletableFuture[util.List[_ <: Location]] = {
      val uri = position.getTextDocument.getUri
      if (uri.contains("test1")) {
        CompletableFuture.completedFuture(createList(new Location(uri, ranges(0))))
      } else {
        CompletableFuture.completedFuture(createList(new Location(uris.find(u => u != uri).get, ranges(1))))
      }
    }

    override def resolveCodeLens(unresolved: CodeLens): CompletableFuture[CodeLens] = {
      CompletableFuture.completedFuture(unresolved)
    }

    override def completion(position: TextDocumentPositionParams): CompletableFuture[messages.Either[util.List[CompletionItem], CompletionList]] = {
      client.showMessage(new MessageParams(MessageType.Info, "Hello completion!"))
      CompletableFuture.completedFuture(Either.forRight(new CompletionList(completionItems.toIndexedSeq.asJava)))
    }

    override def onTypeFormatting(params: DocumentOnTypeFormattingParams): CompletableFuture[util.List[_ <: TextEdit]] = {
      CompletableFuture.completedFuture(createList(new TextEdit(new Range(new Position(0, 0), new Position(0, 0)), "Formatted")))
    }

    override def didOpen(params: DidOpenTextDocumentParams): Unit = {
      if (!uris.contains(params.getTextDocument.getUri)) uris += params.getTextDocument.getUri
    }

    override def signatureHelp(position: TextDocumentPositionParams): CompletableFuture[SignatureHelp] = {
      val uri = position.getTextDocument.getUri
      if (uri.contains("test1")) {
        CompletableFuture.completedFuture(new SignatureHelp(
          createList(
            new SignatureInformation("A", "Do A", createList(
              new ParameterInformation("paramA", "paramA000"),
              new ParameterInformation("paramB", "paramB001")
            )),
            new SignatureInformation("B", "Do B", createList(
              new ParameterInformation("paramA", "paramA010")
            ))
          ), 0, 1))
      } else {
        CompletableFuture.completedFuture(new SignatureHelp(
          createList(
            new SignatureInformation("AA", "Does AA", createList(
              new ParameterInformation("paramAA", "paramAA100"),
              new ParameterInformation("paramBB", "paramBB101")
            )),
            new SignatureInformation("BB", "Does BB", createList(
              new ParameterInformation("paramBB", "paramBB110")
            ))
          )
          , 1, 0))
      }
    }

    override def rangeFormatting(params: DocumentRangeFormattingParams): CompletableFuture[util.List[_ <: TextEdit]] = {
      val range = params.getRange
      val length = range.getEnd.getCharacter - range.getStart.getCharacter
      CompletableFuture.completedFuture(createList(new TextEdit(range, "f" * length)))
    }

    override def codeAction(params: CodeActionParams): CompletableFuture[util.List[_ <: Command]] = {
      val uri = params.getTextDocument.getUri
      if (uri.contains("test1")) {
        CompletableFuture.completedFuture(createList(new Command(commands(0), commands(0))))
      } else {
        CompletableFuture.completedFuture(createList(new Command(commands(1), commands(1)), new Command(commands(2), commands(2))))
      }
    }

    override def rename(params: RenameParams): CompletableFuture[WorkspaceEdit] = {
      client.showMessageRequest(new ShowMessageRequestParams(createList(
        new MessageActionItem("One"),
        new MessageActionItem("Two"),
        new MessageActionItem("Three")
      )))
      CompletableFuture.completedFuture(new WorkspaceEdit(scala.collection.mutable.Map(params.getTextDocument.getUri -> createList(new TextEdit(ranges(0), "Renamed"))).asJava))
    }

    override def formatting(params: DocumentFormattingParams): CompletableFuture[util.List[_ <: TextEdit]] = {
      val uri = params.getTextDocument.getUri
      if (uri.contains("test1")) {
        CompletableFuture.completedFuture(createList(new TextEdit(new Range(new Position(0, 0), new Position(0, 0)), "Formatted")))
      } else {
        CompletableFuture.completedFuture(createList(new TextEdit(ranges(0), "")))
      }
    }
  }
  private var client: LanguageClient = _

  override def getTextDocumentService: TextDocumentService = tds

  override def exit(): Unit = {}

  override def initialize(params: InitializeParams): CompletableFuture[InitializeResult] = {
    CompletableFuture.completedFuture(new InitializeResult(capabilities))
  }

  override def getWorkspaceService: WorkspaceService = ws

  override def shutdown(): CompletableFuture[AnyRef] = CompletableFuture.completedFuture(null)

  private def createList[T](t: T*): util.List[T] = {
    t.toIndexedSeq.asJava
  }
}
