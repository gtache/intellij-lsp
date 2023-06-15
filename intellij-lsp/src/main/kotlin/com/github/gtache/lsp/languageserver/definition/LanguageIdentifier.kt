package com.github.gtache.lsp.languageserver.definition

/**
 * List of default extensions for the common files
 */
enum class LanguageIdentifier constructor(
    private val id: String,
    private val exts: Array<String> = arrayOf(id)
) {
    ABAP("abap"),
    BAT("bat"),
    BIBTEX("bibtex", arrayOf("bib")),
    CLOJURE("clojure", arrayOf("clj")),
    COFFEESCRIPT("coffeescript", arrayOf("coffee")),
    C("c"),
    CPP("cpp"),
    CSHARP("csharp", arrayOf("cs", "c#")),
    CSS("css"),
    DIFF("diff"),
    DART("dart"),
    DOCKERFILE("dockerfile", arrayOf<String>()),
    FSHARP("fsharp", arrayOf("fs", "f#")),
    GITC("git-commit", arrayOf<String>()),
    GITR("git-rebase", arrayOf<String>()),
    GO("go"),
    GROOVY("groovy"),
    HANDLEBARS("handlebars"),
    HTML("html"),
    INI("ini"),
    JAVA("java"),
    JAVASCRIPT("javascript", arrayOf("js")),
    JAVASCRIPTREACT("javascriptreact", arrayOf("jsx")),
    JSON("json"),
    LATEX("latex", arrayOf("tex")),
    LESS("less"),
    LUA("lua"),
    MAKEFILE("makefile", emptyArray()),
    MARKDOWN("markdown", arrayOf("md")),
    OBJECTIVEC("objective-c", arrayOf("m")),
    OBJECTIVECPP("objective-cpp", arrayOf("mm")),
    PERL("perl", arrayOf("pl", "pm")),
    PERL6("perl6", arrayOf("p6", "pm6")),
    PHP("php"),
    POWERSHELL("powershell", arrayOf("ps1")),
    PUG("jade", arrayOf("pug")),
    PYTHON("python", arrayOf(".py")),
    R("r"),
    RAZOR("razor", arrayOf("cshtml")),
    RUBY("ruby"),
    RUST("rust", arrayOf("rs", "rlib")),
    SCSS("scss"),
    SASS("sass"),
    SCALA("scala"),
    SHADERLAB("shaderlab", arrayOf("vert", "frag")),
    SHELLSCRIPT("shellscript", arrayOf("sh", "bash", "zsh", "csh")),
    SQL("sql"),
    SWIFT("swift"),
    TYPESCRIPT("typescript", arrayOf("ts")),
    TYPESCRIPTREACT("typescriptreact", arrayOf("tsx")),
    TEX("tex"),
    VISUALBASIC("vb"),
    XML("xml"),
    XSL("xsl"),
    YAML("yaml");

    companion object {
        private val extensionToId: MutableMap<String, String?> = HashMap(60)

        /**
         * Returns the Language ID for the given [extension]
         */
        fun extToLanguageId(extension: String): String? {
            return extensionToId.getOrDefault(extension, null)
        }

        init {
            for (li in values()) {
                for (ext in li.exts) {
                    extensionToId[ext] = li.id
                }
            }
        }
    }
}