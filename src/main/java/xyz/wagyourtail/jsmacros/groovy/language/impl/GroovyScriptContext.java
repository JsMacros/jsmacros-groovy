package xyz.wagyourtail.jsmacros.groovy.language.impl;

import groovy.lang.GroovyShell;
import xyz.wagyourtail.jsmacros.core.event.BaseEvent;
import xyz.wagyourtail.jsmacros.core.language.BaseScriptContext;

import java.io.File;

public class GroovyScriptContext extends BaseScriptContext<GroovyShell> {

    public GroovyScriptContext(BaseEvent event, File file) {
        super(event, file);
    }

    @Override
    public synchronized void closeContext() {
        super.closeContext();
    }

    @Override
    public boolean isMultiThreaded() {
        return true;
    }

}
