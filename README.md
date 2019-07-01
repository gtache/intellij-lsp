## Development resumes today! Sorry for the wait
# intellij-lsp
Plugin adding support for Language Server Protocol for IntelliJ since version 2017.3     
This plugin should be compatible with any JetBrains IDE (tested successfully on IntelliJ, PyCharm and CLion)    

## Features
What is working (or should be but isn't tested) :      
Requests to the server:     
- hover     
- documentHighlight     
- completion     
- workspaceSymbol     
- rename     
- definition     
- references     
- codeAction     
- executeCommand    
- formatting, rangeFormatting, onTypeFormatting    
- willSave, willSaveWaitUntil, didSave, didClose, didOpen, didChange, didChangeWatchedFiles   

Client :      
- showMessage     
- showMessageRequest    
- logMessage    
- applyEdit    
- publishDiagnostics

The plugin should integrate seamlessly with another plugin supporting a same language (e.g. Scala language server and Scala plugin). The plugin will simply delegate to the Scala plugin any features it already supports (for example, if the Scala plugin supports formatting, the LSP plugin won't ask the language server to format.) The reason is that the current specific plugins are much more powerful than the current language servers at the moment.    
You have the possibility to force sending the requests by checking the "Always send requests" checkbox in the settings.

Concretely, what you can do with IntelliJ at the moment :     
- Hover to get documentation (you can also use (by default) Ctrl+Q)    
![Hover](https://github.com/gtache/intellij-lsp/blob/master/doc/images/hover.gif "HoverGif")
- Use Goto file/class/symbol (Ctrl(+Shift+Alt)+N by default)    
![Goto](https://github.com/gtache/intellij-lsp/blob/master/doc/images/goto.gif "GotoGif")
- See which symbols are the same as the one selected   
![Selection](https://github.com/gtache/intellij-lsp/blob/master/doc/images/selection.gif "SelectionGif")
- Rename a symbol with the basic refactor action (Shift+F6 by default) (you can also rename using Shift+Alt+F6 as a backup if needed)         
![Rename](https://github.com/gtache/intellij-lsp/blob/master/doc/images/rename.gif "RenameGif")
- Go to definition of a symbol, using Ctrl+Click (may need several tries for the first time)    
- See usages / references of a symbol, using Shift+Alt+F7 on it or Ctrl+Click on its definition, and go to these locations by clicking on them in the generated window (may need several tries to make Ctrl-click work as for definition)    
Ctrl-click :     
![CtrlClick](https://github.com/gtache/intellij-lsp/blob/master/doc/images/ctrlClick.gif "CtrlClickGif")    
Shift+Alt+F7 :     
![References](https://github.com/gtache/intellij-lsp/blob/master/doc/images/references.gif "ReferencesGif")     
- See diagnostics (error, warnings) and hover over them to see the message    
![Diagnostic](https://github.com/gtache/intellij-lsp/blob/master/doc/images/diagnostic.gif "DiagnosticGif")    
- You can see the server(s) status in the status bar, and click on the icon to see the connected files and the timeouts    
![ServerStatus](https://github.com/gtache/intellij-lsp/blob/master/doc/images/server_status.gif "StatusGif")    
- Format a document / a selection (Ctrl+Alt(+Shift)+L by default) - tested with TestServer, should work     
- Format while typing - tested with TestServer    
- Get the signature help when typing - tested with TestServer     
- Apply quickfixes (lightbulb icon) - tested with TestServer     

## Add a Language Server
This plugin supports communicating with multiple and different language servers at the same time.    
To add a supported language server, you can either create a plugin (see intellij-lsp-dotty) which extends this plugin and extend LanguageServerDefinition or instantiate a concrete subclass of it and register it, or simply go to IntelliJ/file/settings/Languages & Frameworks/Language Server Protocol and fill the required informations. You can specify multiple extensions for one server by separating them with a semicolon. (example : ts;js)        
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

The current concrete classes are :      
- ArtifactLanguageServerDefinition : This definition uses an artifact location (like a Maven one), retrieves the jar using Coursier and launches the given main class with the given arguments.
- RawCommandServerDefinition : This definition simply runs the command given.
- ExeLanguageServerDefinition : Basically a more convenient way to write a RawCommand, it splits the file to execute and the arguments given to it.    
**Note that all those classes will use the server stdin/stdout to communicate**

If you need/want to write an other implementation, you will need to at least implement the createConnectionProvider method, which instantiates and returns a StreamConnectionProvider (which can be basically anything as long as it gets you the input and output stream to the server). You can also implement custom logic in the start and stop methods of the server definition if needed.

## Further extensions
You can add a custom LSPIconProvider to provide custom icons for the completion/symbols items or for the server status. You need to register an LSPIconProvider extension in your plugin.xml and implement the required methods (or delegate to the default provider).

## FAQ
- Where are the IDE logs?
  - On your IDE, go to 'Help->Show Log in explorer', open the last log file and you should be able to see all the logs of the IDE. The logs related to the LSP can be filtered using com.github.gtache or simply gtache
- Where are the servers logs?
  - In each project folder, you'll have an "lsp" folder. You'll find inside an error log file for each server for each day. If you check the "Log server communications" box in the settings, you'll also find a log file for the communications between server and client. Note that this communication log can grow very fast.
- How to specify multiple extensions for a single server?
  - Simply separate them with a semicolon, i.e. "c;cpp;h"
- How to force-link a file to a specific language server?
  - Right-click either in the editor, the editor tab, or the file list on the left and choose 'Link to LSP server'
- How to force the plugin to make the requests even if the actions are supported by another installed plugin or natively ?
  - Simply check the "Always send requests" box in the Settings->Language->Language Server Protocol settings
- How can I make modifications to the plugin and test them?
  - Clone either master (for the latest release) or the current development branch, open the project in IntelliJ, set the project SDK to the IntelliJ SDK, java 8, and you should be able to simply run the plugin, which will start an IntelliJ sandbox with the plugin installed.
  - Otherwise, use 'Build->Prepare plugin for development' and install the zipped plugin (Settings->Plugins->Install from disk)
