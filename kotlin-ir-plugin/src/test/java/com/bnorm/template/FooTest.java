package com.bnorm.template;

import kotlin.coroutines.EmptyCoroutineContext;
import kotlinx.coroutines.BuildersKt;
import org.junit.Test;

public class FooTest {
    @Test
    public void testSomething() throws InterruptedException {
        BuildersKt.runBlocking(EmptyCoroutineContext.INSTANCE, (a, b) ->
                TripBooking.INSTANCE.bookTrip("", 5, b)
        );
    }
}
