# intellij-lsp
Plugin adding support for Language Server Protocol for IntelliJ     

What's working :      
Requests to the server:     
-Hover     
-DocumentHighlight     
-Completion     
-workspaceSymbol     
-willSave, didSave, didClose, didOpen, didChange       
Client :      
-showMessage     
-logMessage    
-applyEdit

Concretely, what you can do with IntelliJ at the moment :     
-Hover to get documentation (you can also use (by default) CTRL+Q)    
-Use Goto file/class/symbol (CTRL(+SHIFT+ALT)+N by default)    
-See which symbols are the same as the one selected


There is a skeleton of an LSP plugin for Dotty in the intellij-lsp-dotty folder (mainly for testing purposes).
