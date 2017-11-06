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
-formatting, rangeFormatting    
-willSave, didSave, didClose, didOpen, didChange       
Client :      
-showMessage     
-logMessage    
-applyEdit    
-publishDiagnostics

Concretely, what you can do with IntelliJ at the moment :     
-Hover to get documentation (you can also use (by default) CTRL+Q)    
-Use Goto file/class/symbol (CTRL(+SHIFT+ALT)+N by default)    
-See which symbols are the same as the one selected    
-Rename a symbol with SHIFT+ALT+F6    
-Format a document / a selection (CTRL+ALT(+SHIFT)+L by default)    
-See diagnostics (error, warnings) and hover over them to see the message

## Add a Language Server
To add a supported language server, you can either create a plugin (see intellij-lsp-dotty) which extends this plugin and extend LanguageServerDefinition or instantiate a concrete subclass of it and register it, or simply go to IntelliJ/file/settings/Languages & Frameworks/Language Server Protocol and fill the required informations.    
Note that the settings will always override a possible LSP plugin for the same file extension.


There is a skeleton of an LSP plugin for Dotty in the intellij-lsp-dotty folder (mainly for testing purposes). Most of the code is taken and adapted from https://github.com/JetBrains/intellij-scala
