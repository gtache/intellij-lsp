# intellij-lsp
Plugin adding support for Language Server Protocol for IntelliJ     
This plugin should be compatible with any JetBrains IDE (tested successfully on IntelliJ, PyCharm and CLion)

## Features
What is working (or should be but isn't tested) :      
Requests to the server:     
-hover     
-documentHighlight     
-completion     
-workspaceSymbol     
-rename     
-definition     
-references     
-codeAction     
-formatting, rangeFormatting, onTypeFormatting    
-willSave, willSaveWaitUntil, didSave, didClose, didOpen, didChange, didChangeWatchedFiles       
Client :      
-showMessage     
-logMessage    
-applyEdit    
-publishDiagnostics

The plugin should integrate seamlessly with another plugin supporting a same language (e.g. Scala language server and Scala plugin). The plugin will simply delegate to the Scala plugin any features it already supports (for example, if the Scala plugin supports formatting, the LSP plugin won't ask the language server to format.) The reason is that the current specific plugins are much more powerful than the current language servers at the moment.    
I will probably add settings later to choose which plugin will be prioritized.

Concretely, what you can do with IntelliJ at the moment :     
-Hover to get documentation (you can also use (by default) Ctrl+Q)    
![Hover](https://github.com/gtache/intellij-lsp/blob/master/doc/images/hover.gif "HoverGif")
-Use Goto file/class/symbol (Ctrl(+Shift+Alt)+N by default)    
![Goto](https://github.com/gtache/intellij-lsp/blob/master/doc/images/goto.gif "GotoGif")
-See which symbols are the same as the one selected   
![Selection](https://github.com/gtache/intellij-lsp/blob/master/doc/images/selection.gif "SelectionGif")
-Rename a symbol with Shift+Alt+F6 (seems to make IntelliJ consider Ctrl pressed until pressing it, will look into it)     
![Rename](https://github.com/gtache/intellij-lsp/blob/master/doc/images/rename.gif "RenameGif")
-Go to definition of a symbol, using Ctrl+Click (may need several tries)    
-See usages / references of a symbol, using Shift+Alt+F7 on it or Ctrl+Click on its definition, and go to these locations by clicking on them in the generated window (may need several tries to make Ctrl-click work, will look into it)     
![CtrlClick](https://github.com/gtache/intellij-lsp/blob/master/doc/images/ctrlClick.gif "CtrlClickGif")
-See diagnostics (error, warnings) and hover over them to see the message    
![Diagnostic](https://github.com/gtache/intellij-lsp/blob/master/doc/images/diagnostic.gif "DiagnosticGif")    
-Format a document / a selection (Ctrl+Alt(+Shift)+L by default) - untested     
-Get the signature help when typing - untested     

## Add a Language Server
This plugin supports communicating with multiple and different language servers at the same time.    
To add a supported language server, you can either create a plugin (see intellij-lsp-dotty) which extends this plugin and extend LanguageServerDefinition or instantiate a concrete subclass of it and register it, or simply go to IntelliJ/file/settings/Languages & Frameworks/Language Server Protocol and fill the required informations.    
Note that the settings will always override a possible LSP plugin for the same file extension.    
You can also configure the timeouts for the requests (if you see timeouts warning in the log for example), depending on your computer.    
Settings:    
![Settings](https://github.com/gtache/intellij-lsp/blob/master/doc/images/settings.gif "SettingsGif")

Via a plugin (example with concrete subclass instantation):
```
class RustPreloadingActivity extends PreloadingActivity {

  override def preload(indicator: ProgressIndicator): Unit = {
    //Assume rls is on Path
    LanguageServerDefinition.register(new ExeLanguageServerDefinition("rs", "rls", Array()))
  }
}
```    

With plugin.xml containing
```
<extensions defaultExtensionNs="com.intellij">
      <preloadingActivity implementation="com.github.gtache.lsp.rust.RustPreloadingActivity" id="com.github.gtache.lsp.rust.RustPreloadingActivity" />
  </extensions>
<depends>com.github.gtache.lsp</depends>
```

There is a skeleton of a more concrete LSP plugin for Dotty with Syntax Highlighting in the intellij-lsp-dotty folder. Most of the code is taken and adapted from https://github.com/JetBrains/intellij-scala
