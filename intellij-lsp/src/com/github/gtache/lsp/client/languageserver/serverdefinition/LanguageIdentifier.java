package com.github.gtache.lsp.client.languageserver.serverdefinition;

import java.util.HashMap;
import java.util.Map;

public enum LanguageIdentifier {
    ABAP("abap"),
    BAT("bat"),
    BIBTEX("bibtex", new String[]{"bib"}),
    CLOJURE("clojure", new String[]{"clj"}),
    COFFEESCRIPT("coffeescript", new String[]{"coffee"}),
    C("c"),
    CPP("cpp"),
    CSHARP("csharp", new String[]{"cs", "c#"}),
    CSS("css"),
    DIFF("diff"),
    DART("dart"),
    DOCKERFILE("dockerfile", new String[]{}),
    FSHARP("fsharp", new String[]{"fs", "f#"}),
    GITC("git-commit", new String[]{}),
    GITR("git-rebase", new String[]{}),
    GO("go"),
    GROOVY("groovy"),
    HANDLEBARS("handlebars"),
    HTML("html"),
    INI("ini"),
    JAVA("java"),
    JAVASCRIPT("javascript", new String[]{"js"}),
    JAVASCRIPTREACT("javascriptreact", new String[]{"jsx"}),
    JSON("json"),
    LATEX("latex", new String[]{"tex"}),
    LESS("less"),
    LUA("lua"),
    MAKEFILE("makefile", new String[]{}),
    MARKDOWN("markdown", new String[]{"md"}),
    OBJECTIVEC("objective-c", new String[]{"m"}),
    OBJECTIVECPP("objective-cpp", new String[]{"mm"}),
    PERL("perl", new String[]{"pl", "pm"}),
    PERL6("perl6", new String[]{"p6", "pm6"}),
    PHP("php"),
    POWERSHELL("powershell", new String[]{"ps1"}),
    PUG("jade", new String[]{"pug"}),
    PYTHON("python", new String[]{".py"}), R("r"),
    RAZOR("razor", new String[]{"cshtml"}),
    RUBY("ruby"),
    RUST("rust", new String[]{"rs", "rlib"}),
    SCSS("scss"),
    SASS("sass"),
    SCALA("scala"),
    SHADERLAB("shaderlab", new String[]{"vert", "frag"}),
    SHELLSCRIPT("shellscript", new String[]{"sh", "bash", "zsh", "csh"}),
    SQL("sql"),
    SWIFT("swift"),
    TYPESCRIPT("typescript", new String[]{"ts"}),
    TYPESCRIPTREACT("typescriptreact", new String[]{"tsx"}), TEX("tex"),
    VISUALBASIC("vb"),
    XML("xml"),
    XSL("xsl"),
    YAML("yaml");

    private final String id;
    private final String[] exts;
    private static final Map<String, String> extToId = new HashMap<>(60);

    static {
        for (final LanguageIdentifier li : LanguageIdentifier.values()) {
            for (final String ext : li.exts) {
                extToId.put(ext, li.id);
            }
        }
    }

    LanguageIdentifier(final String id) {
        this(id, new String[]{id});
    }

    LanguageIdentifier(final String id, final String[] exts) {
        this.id = id;
        this.exts = exts;
    }

    public static String extToLanguageId(final String ext) {
        return extToId.getOrDefault(ext, null);
    }


}
