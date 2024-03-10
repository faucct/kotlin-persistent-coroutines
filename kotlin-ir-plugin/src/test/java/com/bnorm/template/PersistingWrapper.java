package com.bnorm.template;

import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.jvm.internal.CoroutineStackFrame;
import kotlin.jvm.functions.Function1;
import kotlinx.serialization.KSerializer;
import kotlinx.serialization.StringFormat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PersistingWrapper {
    public static Wrapper wrapper(
            StringFormat stringFormat, KSerializer<Class<?>> classSerializer, PersistedString persistedString
    ) {
        return new Wrapper() {
            public <T> Object invoke(
                    @NotNull Function1<? super Continuation<? super T>, ?> function1,
                    @NotNull Continuation<? super T> continuation
            ) {
                var javaCoroutineSerializer = new JavaCoroutineSerializer(stringFormat, classSerializer);
                class PersistedContinuation implements Continuation<T>, CoroutineStackFrame {
                    final CoroutineContext coroutineContext = new Persistor() {
                        @Override
                        public Object persist(@NotNull Continuation<? super Unit> $completion) {
                            persistedString.setPersisted(((String) serializer.invoke(
                                    (Continuation<? super String>) $completion
                            )), $completion);
                            return Unit.INSTANCE;
                        }
                    }.plus(continuation.getContext());
                    Function1<? super Continuation<? super String>, ?> serializer = javaCoroutineSerializer.serializer(
                            (Continuation<? super Function1<? super Continuation<? super String>, ?>>) (Continuation<?>) this
                    );

                    @NotNull
                    @Override
                    public CoroutineContext getContext() {
                        return coroutineContext;
                    }

                    @Override
                    public void resumeWith(@NotNull Object o) {
                        continuation.resumeWith(o);
                    }

                    @Nullable
                    @Override
                    public CoroutineStackFrame getCallerFrame() {
                        return continuation instanceof CoroutineStackFrame ? ((CoroutineStackFrame) continuation) : null;
                    }

                    @Nullable
                    @Override
                    public StackTraceElement getStackTraceElement() {
                        return null;
                    }
                }
                var persistedContinuation = new PersistedContinuation();
                var persisted = (String) persistedString.getPersisted((Continuation<? super String>) continuation);
                return persisted != null ? javaCoroutineSerializer.deserialize(
                        persisted,
                        (Continuation<? super Unit>) (Continuation<?>) persistedContinuation
                ) : function1.invoke(persistedContinuation);
            }
        };
    }
}
