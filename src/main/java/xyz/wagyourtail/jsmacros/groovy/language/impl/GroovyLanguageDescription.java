package xyz.wagyourtail.jsmacros.groovy.language.impl;

import groovy.lang.GroovyShell;
import org.codehaus.groovy.control.CompilationFailedException;
import xyz.wagyourtail.jsmacros.core.Core;
import xyz.wagyourtail.jsmacros.core.config.ScriptTrigger;
import xyz.wagyourtail.jsmacros.core.event.BaseEvent;
import xyz.wagyourtail.jsmacros.core.extensions.Extension;
import xyz.wagyourtail.jsmacros.core.language.BaseLanguage;
import xyz.wagyourtail.jsmacros.core.language.EventContainer;

import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;

public class GroovyLanguageDescription extends BaseLanguage<GroovyShell, GroovyScriptContext> {

    public GroovyLanguageDescription(Extension extension, Core<?, ?> runner) {
        super(extension, runner);
    }

    private void runGroovyShell(EventContainer<GroovyScriptContext> ctx, Consumer<GroovyShell> r) {
        GroovyShell shell = new GroovyShell(GroovyLanguageDescription.class.getClassLoader());
        ctx.getCtx().setContext(shell);
        retrieveLibs(ctx.getCtx()).forEach(shell::setProperty);

        r.accept(shell);
    }

    @Override
    protected void exec(EventContainer<GroovyScriptContext> eventContainer, ScriptTrigger scriptTrigger, BaseEvent baseEvent) throws Exception {
        try {
            runGroovyShell(eventContainer, (shell) -> {
                shell.setProperty("event", baseEvent);
                shell.setProperty("file", eventContainer.getCtx().getFile());
                shell.setProperty("ctx", eventContainer);

                try {
                    shell.evaluate(eventContainer.getCtx().getFile());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (CompilationFailedException e) {
            throw e;
        } catch (Throwable e) {
            throw new GroovyRuntimeException(e, eventContainer.getCtx().getFile());
        }
    }

    @Override
    protected void exec(EventContainer<GroovyScriptContext> eventContainer, String lang, String script, BaseEvent baseEvent) throws Exception {
        try {
            runGroovyShell(eventContainer, (shell) -> {
                shell.setProperty("event", baseEvent);
                shell.setProperty("file", eventContainer.getCtx().getFile());
                shell.setProperty("ctx", eventContainer);

                shell.evaluate(script);
            });
        } catch (CompilationFailedException e) {
            throw e;
        } catch (Throwable e) {
            throw new GroovyRuntimeException(e, eventContainer.getCtx().getFile());
        }
    }

    @Override
    public GroovyScriptContext createContext(BaseEvent baseEvent, File file) {
        return new GroovyScriptContext(baseEvent, file);
    }

    public static class GroovyRuntimeException extends Exception {
        public final File file;
        public GroovyRuntimeException(Throwable cause, File file) {
            super(cause);
            this.file = file;
        }
    }
}
