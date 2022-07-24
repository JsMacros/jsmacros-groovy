package xyz.wagyourtail.jsmacros.groovy.library.impl;

import groovy.lang.Closure;
import groovy.lang.GroovyShell;
import xyz.wagyourtail.jsmacros.core.Core;
import xyz.wagyourtail.jsmacros.core.MethodWrapper;
import xyz.wagyourtail.jsmacros.core.language.BaseLanguage;
import xyz.wagyourtail.jsmacros.core.library.IFWrapper;
import xyz.wagyourtail.jsmacros.core.library.Library;
import xyz.wagyourtail.jsmacros.core.library.PerExecLanguageLibrary;
import xyz.wagyourtail.jsmacros.groovy.language.impl.GroovyLanguageDescription;
import xyz.wagyourtail.jsmacros.groovy.language.impl.GroovyScriptContext;

@SuppressWarnings({"rawtypes", "unchecked"})
@Library(value = "JavaWrapper", languages = GroovyLanguageDescription.class)
public class FWrapper extends PerExecLanguageLibrary<GroovyShell, GroovyScriptContext> implements IFWrapper<Closure> {


    public FWrapper(GroovyScriptContext context, Class<? extends BaseLanguage<GroovyShell, GroovyScriptContext>> language) {
        super(context, language);
    }

    @Override
    public <A, B, R> MethodWrapper<A, B, R, ?> methodToJava(Closure closure) {
        return new GroovyMethodWrapper<>(closure, true, ctx);
    }

    @Override
    public <A, B, R> MethodWrapper<A, B, R, ?> methodToJavaAsync(Closure closure) {
        return new GroovyMethodWrapper<>(closure, false, ctx);
    }

    @Override
    public void stop() {

    }


    private static class GroovyMethodWrapper<A, B, R> extends MethodWrapper<A, B, R, GroovyScriptContext> {
        private final Closure<R> fn;
        private final boolean await;

        public GroovyMethodWrapper(Closure<R> fn, boolean await, GroovyScriptContext ctx) {
            super(ctx);
            this.fn = fn;
            this.await = await;
        }

        public void internal_accept(Object... args) {
            if (await) {
                internal_apply(args);
                return;
            }
            Thread t = new Thread(() -> {
                ctx.bindThread(Thread.currentThread());
                try {
                    fn.call(args);
                } catch (Throwable e) {
                    Core.getInstance().profile.logError(e);
                } finally {
                    ctx.releaseBoundEventIfPresent(Thread.currentThread());
                    ctx.unbindThread(Thread.currentThread());

                    Core.getInstance().profile.joinedThreadStack.remove(Thread.currentThread());
                }
            });
            t.start();
        }

        public R internal_apply(Object... args) {
            if (ctx.getBoundThreads().contains(Thread.currentThread())) {
                return fn.call(args);
            }

            try {
                ctx.bindThread(Thread.currentThread());
                if (Core.getInstance().profile.checkJoinedThreadStack()) {
                    Core.getInstance().profile.joinedThreadStack.add(Thread.currentThread());
                }
                return fn.call(args);
            } catch (Throwable ex) {
                throw new RuntimeException(ex);
            } finally {
                ctx.releaseBoundEventIfPresent(Thread.currentThread());
                ctx.unbindThread(Thread.currentThread());
                Core.getInstance().profile.joinedThreadStack.remove(Thread.currentThread());
            }
        }

        @Override
        public void accept(A a) {
            internal_accept(a);
        }

        @Override
        public void accept(A a, B b) {
            internal_accept(a, b);
        }

        @Override
        public R apply(A a) {
            return internal_apply(a);
        }

        @Override
        public R apply(A a, B b) {
            return internal_apply(a, b);
        }

        @Override
        public boolean test(A a) {
            return (Boolean) internal_apply(a);
        }

        @Override
        public boolean test(A a, B b) {
            return (Boolean) internal_apply(a, b);
        }

        @Override
        public void run() {
            internal_accept();
        }

        @Override
        public int compare(A o1, A o2) {
            return (Integer) internal_apply(o1, o2);
        }

        @Override
        public R get() {
            return internal_apply();
        }

    }
}
