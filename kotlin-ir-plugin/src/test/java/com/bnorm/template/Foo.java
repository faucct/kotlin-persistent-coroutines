package com.bnorm.template;

import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.EmptyCoroutineContext;
import kotlinx.coroutines.BuildersKt;
import kotlinx.coroutines.CoroutineDispatcher;
import org.jetbrains.annotations.NotNull;

public class Foo {
    public static void main(String[] args) throws InterruptedException {
        BuildersKt.runBlocking(EmptyCoroutineContext.INSTANCE, (coroutineScope, continuation) ->
                TripBooking.INSTANCE.bookTrip("", 5, continuation)
        );

        CoroutineDispatcher coroutineDispatcher = new CoroutineDispatcher() {
            @Override
            public void dispatch(@NotNull CoroutineContext coroutineContext, @NotNull Runnable runnable) {
                runnable.run();
            }
        };
        TripBooking.INSTANCE.bookTrip("", 5, new Continuation<>() {
            @Override
            public void resumeWith(@NotNull Object o) {
                System.out.println("resumed " + o);
            }

            @NotNull
            @Override
            public CoroutineContext getContext() {
                return (CoroutineContext) coroutineDispatcher;
            }
        });
    }
}
