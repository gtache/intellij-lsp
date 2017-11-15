# intellij-lsp
Plugin adding support for Language Server Protocol for IntelliJ     

## Features
What's working :      
Requests to the server:     
-hover     
-documentHighlight     
-completion     
-workspaceSymbol     
-rename     
-definition     
-references     
-formatting, rangeFormatting    
-willSave, didSave, didClose, didOpen, didChange       
Client :      
-showMessage     
-logMessage    
-applyEdit    
-publishDiagnostics

Concretely, what you can do with IntelliJ at the moment :     
-Hover to get documentation (you can also use (by default) Ctrl+Q)    
-Use Goto file/class/symbol (Ctrl(+Shift+Alt)+N by default)    
-See which symbols are the same as the one selected    
-Rename a symbol with Shift+Alt+F6    
-Format a document / a selection (Ctrl+Alt(+Shift)+L by default) - untested     
-Go to definition of a symbol, using Ctrl+Click    
-See usages / references of a symbol, using Shift+Alt+F7 on it or Ctrl+Click on its definition, and go to these locations by clicking on them in the generated window     
-Get the signature help when typing - untested     
-See diagnostics (error, warnings) and hover over them to see the message

## Add a Language Server
To add a supported language server, you can either create a plugin (see intellij-lsp-dotty) which extends this plugin and extend LanguageServerDefinition or instantiate a concrete subclass of it and register it, or simply go to IntelliJ/file/settings/Languages & Frameworks/Language Server Protocol and fill the required informations.    
Note that the settings will always override a possible LSP plugin for the same file extension.


There is a skeleton of an LSP plugin for Dotty in the intellij-lsp-dotty folder (mainly for testing purposes). Most of the code is taken and adapted from https://github.com/JetBrains/intellij-scala
