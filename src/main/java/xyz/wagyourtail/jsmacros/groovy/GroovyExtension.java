package xyz.wagyourtail.jsmacros.groovy;

import com.google.common.collect.Sets;
import groovy.lang.GroovyShell;
import xyz.wagyourtail.jsmacros.core.Core;
import xyz.wagyourtail.jsmacros.core.extensions.Extension;
import xyz.wagyourtail.jsmacros.core.language.BaseLanguage;
import xyz.wagyourtail.jsmacros.core.language.BaseWrappedException;
import xyz.wagyourtail.jsmacros.core.library.BaseLibrary;
import xyz.wagyourtail.jsmacros.groovy.language.impl.GroovyLanguageDescription;
import xyz.wagyourtail.jsmacros.groovy.library.impl.FWrapper;

import java.io.File;
import java.util.Set;

public class GroovyExtension implements Extension {
    private static GroovyLanguageDescription languageDescription;

    @Override
    public void init() {
        Thread t = new Thread(() -> {
            GroovyShell shell = new GroovyShell();
            shell.evaluate("println(\"Groovy Loaded.\")");
        });
        t.start();
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public String getLanguageImplName() {
        return "groovy";
    }

    @Override
    public ExtMatch extensionMatch(File file) {
        if (file.getName().endsWith(".groovy")) {
            return ExtMatch.MATCH_WITH_NAME;
        }
        return ExtMatch.NOT_MATCH;
    }

    @Override
    public String defaultFileExtension() {
        return "groovy";
    }

    @Override
    public BaseLanguage<?, ?> getLanguage(Core<?, ?> core) {
        if (languageDescription == null) {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(GroovyExtension.class.getClassLoader());
            languageDescription = new GroovyLanguageDescription(this, core);
            Thread.currentThread().setContextClassLoader(classLoader);
        }
        return languageDescription;
    }

    @Override
    public Set<Class<? extends BaseLibrary>> getLibraries() {
        return Sets.newHashSet(FWrapper.class);
    }

    @Override
    public BaseWrappedException<?> wrapException(Throwable throwable) {
        // figure out what to do here later
        return null;
    }

    @Override
    public boolean isGuestObject(Object o) {
        return false;
    }

}
