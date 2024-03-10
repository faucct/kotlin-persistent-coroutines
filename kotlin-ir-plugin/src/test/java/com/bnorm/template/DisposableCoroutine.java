package com.bnorm.template;

import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DisposableCoroutine extends Dispose implements UnitWrapper {
    private Continuation<?> $completion;

    @Override
    public Object dispose(@NotNull Continuation<? super Unit> $completion) {
        this.$completion.resumeWith(Unit.INSTANCE);
        return kotlin.coroutines.intrinsics.IntrinsicsKt.getCOROUTINE_SUSPENDED();
    }

    @Nullable
    @Override
    public Object invoke(
            @NotNull Function1<? super Continuation<? super Unit>, ?> block,
            @NotNull Continuation<? super Unit> $completion
    ) {
        this.$completion = $completion;
        return block.invoke($completion);
    }
}
