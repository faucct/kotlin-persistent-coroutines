package com.bnorm.template;

import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlin.jvm.functions.Function1;
import kotlinx.coroutines.CancellableContinuation;
import kotlinx.coroutines.CancellableContinuationKt;
import kotlinx.serialization.KSerializer;
import kotlinx.serialization.StringFormat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class JavaCoroutineSerializer implements CoroutineSerializer {
    private final StringFormat stringFormat;
    private final KSerializer<Class<?>> classSerializer;

    public JavaCoroutineSerializer(StringFormat stringFormat, KSerializer<Class<?>> classSerializer) {
        this.stringFormat = stringFormat;
        this.classSerializer = classSerializer;
    }

    @Nullable
    @Override
    public Function1<? super Continuation<? super String>, ?> serializer(
            @NotNull Continuation<? super Function1<? super Continuation<? super String>, ?>> $completion
    ) {
        return (Function1<Continuation<? super String>, Object>) continuation -> stringFormat.encodeToString(
                new ContinuationSerializer($completion, classSerializer), continuation
        );
    }

    @Nullable
    @Override
    public Object deserialize(@NotNull String string, @NotNull Continuation<? super Unit> $completion) {
        new Function1<CancellableContinuation<? super Unit>, Unit>() {
            CancellableContinuation<? super Unit> invoked;

            {
                 CancellableContinuationKt.suspendCancellableCoroutine(
                        this,
                        (Continuation<Unit>) stringFormat.decodeFromString(
                                new ContinuationSerializer($completion, classSerializer), string
                        )
                );
                invoked.resume(Unit.INSTANCE, null);
            }

            @Override
            public Unit invoke(CancellableContinuation<? super Unit> cancellableContinuation) {
                invoked = cancellableContinuation;
                return Unit.INSTANCE;
            }
        };
        return kotlin.coroutines.intrinsics.IntrinsicsKt.getCOROUTINE_SUSPENDED();
    }
}
