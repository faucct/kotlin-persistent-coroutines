package com.bnorm.template;

import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.jvm.functions.Function1;
import kotlinx.coroutines.CancellableContinuation;
import kotlinx.coroutines.CancellableContinuationKt;
import kotlinx.serialization.StringFormat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.atomic.AtomicReference;

public class PersistingWrapper {
    private static final Path STATE_PATH = Path.of("coroutine");
    private static final Path OUTPUT_PATH = Path.of("coroutine.tmp");

    private static class PersistedContinuation<T> implements Continuation<T> {
        private final Continuation<? super T> continuation;
        private final CoroutineContext coroutineContext;

        private PersistedContinuation(ClassLoader classLoader, StringFormat stringFormat, Continuation<? super T> continuation) {
            this.continuation = continuation;
            coroutineContext = new PersistingCoroutineContext<>(classLoader, stringFormat, this).plus(continuation.getContext());
        }

        @NotNull
        @Override
        public CoroutineContext getContext() {
            return coroutineContext;
        }

        @Override
        public void resumeWith(@NotNull Object o) {
            continuation.resumeWith(o);
        }
    }

    public static class PersistingCoroutineContext<T> extends Persistor {
        private final ClassLoader classLoader;
        private final StringFormat stringFormat;
        private final PersistedContinuation<T> persistedContinuation;

        public PersistingCoroutineContext(ClassLoader classLoader, StringFormat stringFormat, PersistedContinuation<T> persistedContinuation) {
            this.classLoader = classLoader;
            this.stringFormat = stringFormat;
            this.persistedContinuation = persistedContinuation;
        }

        @Nullable
        @Override
        public Object persist(@NotNull Continuation<? super Unit> $completion) {
            try {
                try (var output = Files.newOutputStream(OUTPUT_PATH)) {
                    output.write(stringFormat.encodeToString(
                            new ContinuationSerializer(classLoader, persistedContinuation),
                            (Continuation<? super Object>) $completion
                    ).getBytes());
                }
                Files.move(OUTPUT_PATH, STATE_PATH, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return Unit.INSTANCE;
        }
    }

    public static Wrapper wrapper(ClassLoader classLoader, StringFormat stringFormat) {
        return new Wrapper() {
            public <T> Object invoke(
                    @NotNull Function1<? super Continuation<? super T>, ?> function1,
                    @NotNull Continuation<? super T> continuation
            ) {
                PersistedContinuation<T> persistedContinuation = new PersistedContinuation<>(classLoader, stringFormat, continuation);
                if (Files.exists(STATE_PATH)) {
                    try (var input = Files.newInputStream(STATE_PATH)) {
                        var cancellableContinuationReference = new AtomicReference<CancellableContinuation<? super Unit>>();
                        Object coroutineSuspended = CancellableContinuationKt.suspendCancellableCoroutine(
                                cancellableContinuation -> {
                                    cancellableContinuationReference.set(cancellableContinuation);
                                    return Unit.INSTANCE;
                                },
                                (Continuation<Unit>) (Continuation<?>) stringFormat.decodeFromString(
                                        new ContinuationSerializer(classLoader, persistedContinuation),
                                        new String(input.readAllBytes())
                                )
                        );
                        cancellableContinuationReference.get().resume(Unit.INSTANCE, null);
                        return coroutineSuspended;
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                return function1.invoke(persistedContinuation);
            }
        };
    }
}
