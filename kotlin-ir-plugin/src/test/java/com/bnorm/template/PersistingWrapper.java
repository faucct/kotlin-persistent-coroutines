package com.bnorm.template;

import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.jvm.functions.Function1;
import kotlinx.coroutines.CancellableContinuation;
import kotlinx.coroutines.CancellableContinuationKt;
import kotlinx.serialization.KSerializer;
import kotlinx.serialization.StringFormat;
import org.jetbrains.annotations.NotNull;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class PersistingWrapper {
    private static final Path STATE_PATH = Path.of("coroutine");
    private static final Path OUTPUT_PATH = Path.of("coroutine.tmp");

    public static Wrapper wrapper(
            ClassLoader classLoader, StringFormat stringFormat, KSerializer<Class<?>> classSerializer
    ) {
        return new Wrapper() {
            public <T> Object invoke(
                    @NotNull Function1<? super Continuation<? super T>, ?> function1,
                    @NotNull Continuation<? super T> continuation
            ) {
                var persistedContinuation = new Continuation<T>() {
                    final ContinuationSerializer continuationSerializer = new ContinuationSerializer(
                            classLoader, this, classSerializer
                    );
                    final CoroutineContext coroutineContext = new Persistor() {
                        @Override
                        public Object persist(@NotNull Continuation<? super Unit> $completion) {
                            try {
                                try (var output = Files.newOutputStream(OUTPUT_PATH)) {
                                    output.write(stringFormat.encodeToString(continuationSerializer, $completion).getBytes());
                                }
                                Files.move(OUTPUT_PATH, STATE_PATH, StandardCopyOption.ATOMIC_MOVE);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                            return Unit.INSTANCE;
                        }
                    }.plus(continuation.getContext());

                    @NotNull
                    @Override
                    public CoroutineContext getContext() {
                        return coroutineContext;
                    }

                    @Override
                    public void resumeWith(@NotNull Object o) {
                        continuation.resumeWith(o);
                    }
                };
                try (var input = Files.newInputStream(STATE_PATH)) {
                    return new Function1<CancellableContinuation<? super Unit>, Unit>() {
                        CancellableContinuation<? super Unit> invoked;
                        final Object coroutineSuspended = CancellableContinuationKt.suspendCancellableCoroutine(
                                this,
                                (Continuation<Unit>) stringFormat.decodeFromString(
                                        persistedContinuation.continuationSerializer,
                                        new String(input.readAllBytes())
                                )
                        );

                        {
                            invoked.resume(Unit.INSTANCE, null);
                        }

                        @Override
                        public Unit invoke(CancellableContinuation<? super Unit> cancellableContinuation) {
                            invoked = cancellableContinuation;
                            return Unit.INSTANCE;
                        }
                    }.coroutineSuspended;
                } catch (NoSuchFileException ignored) {
                    return function1.invoke(persistedContinuation);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }
}
