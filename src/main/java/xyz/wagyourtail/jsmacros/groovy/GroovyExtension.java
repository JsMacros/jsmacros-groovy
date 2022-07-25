package xyz.wagyourtail.jsmacros.groovy;

import com.google.common.collect.Sets;
import groovy.lang.GroovyShell;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.ErrorCollector;
import org.codehaus.groovy.control.MultipleCompilationErrorsException;
import org.codehaus.groovy.control.messages.Message;
import org.codehaus.groovy.control.messages.SyntaxErrorMessage;
import xyz.wagyourtail.jsmacros.core.Core;
import xyz.wagyourtail.jsmacros.core.extensions.Extension;
import xyz.wagyourtail.jsmacros.core.language.BaseLanguage;
import xyz.wagyourtail.jsmacros.core.language.BaseWrappedException;
import xyz.wagyourtail.jsmacros.core.library.BaseLibrary;
import xyz.wagyourtail.jsmacros.groovy.language.impl.GroovyLanguageDescription;
import xyz.wagyourtail.jsmacros.groovy.library.impl.FWrapper;

import java.io.File;
import java.util.*;

public class GroovyExtension implements Extension {
    public static GroovyLanguageDescription languageDescription;

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
        if (throwable instanceof MultipleCompilationErrorsException) {
            MultipleCompilationErrorsException ex = (MultipleCompilationErrorsException) throwable;
            ErrorCollector ec = ex.getErrorCollector();
            List<Message> messages = new ArrayList<>();
            for (int i = 0; i < ec.getErrorCount(); i++) {
                messages.add(ec.getError(i));
            }
            Iterator<Message> it = messages.iterator();
            return new BaseWrappedException<>(ex, "MultipleCompilationErrorsException", null, it.hasNext() ? wrapMessage(it.next(), it) : null);
        }
        if (throwable instanceof CompilationFailedException) {
            return new BaseWrappedException<>(null, null, null, null);
        }
        if (throwable instanceof GroovyLanguageDescription.GroovyRuntimeException) {
            GroovyLanguageDescription.GroovyRuntimeException ex = (GroovyLanguageDescription.GroovyRuntimeException) throwable;
            Throwable cause = ex.getCause();
            if (cause == null) return null;
            Iterator<StackTraceElement> nextGetter = Arrays.stream(cause.getStackTrace()).iterator();
            String message = cause.getClass().getSimpleName();
            String intMessage = cause.getMessage();
            if (intMessage != null) {
                message += ": " + intMessage;
            }
            return new BaseWrappedException<>(cause, message, null, nextGetter.hasNext() ? wrapStackTrace(ex.file, nextGetter.next(), nextGetter) : null);
        }
        return null;
    }

    public BaseWrappedException<?> wrapMessage(Message message, Iterator<Message> next) {
        if (message instanceof SyntaxErrorMessage) {
            SyntaxErrorMessage ex = (SyntaxErrorMessage) message;
            File f = new File(ex.getCause().getSourceLocator());
            if (!f.exists()) {
                f = null;
            }
            BaseWrappedException.GuestLocation loc = new BaseWrappedException.GuestLocation(f, -1, -1, ex.getCause().getStartLine(), ex.getCause().getStartColumn());
            return new BaseWrappedException<>(ex, ex.getCause().getMessage().split("@")[0].trim(), loc, next.hasNext() ? wrapMessage(next.next(), next) : null);
        } else {
            System.out.println("Unknown message type: " + message.getClass().getName());
            return next.hasNext() ? wrapMessage(next.next(), next) : null;
        }
    }

    public BaseWrappedException<?> wrapStackTrace(File f, StackTraceElement el, Iterator<StackTraceElement> next) {
        String fname = el.getFileName();
        if (fname != null && fname.endsWith(".groovy")) {
            if (!f.exists()) {
                f = null;
                System.out.println("File not found: " + f);
            }
            BaseWrappedException.GuestLocation loc = new BaseWrappedException.GuestLocation(f, -1, -1, el.getLineNumber(), -1);
            return new BaseWrappedException<>(null, el.getClassName() + "." + el.getMethodName(), loc, next.hasNext() ? wrapStackTrace(f, next.next(), next) : null);
        } else {
            if (el.getClassName().contains("internal.reflect") || el.getClassName().contains("groovy.runtime.callsite") || el.getClassName().contains("groovy.reflection")) {
                return next.hasNext() ? wrapStackTrace(f, next.next(), next) : null;
            } else {
                if (el.getClassName().equals("groovy.lang.GroovyShell")) {
                    return null;
                }
                return BaseWrappedException.wrapHostElement(el, next.hasNext() ? wrapStackTrace(f, next.next(), next) : null);
            }
        }
    }

    @Override
    public boolean isGuestObject(Object o) {
        return false;
    }

}
