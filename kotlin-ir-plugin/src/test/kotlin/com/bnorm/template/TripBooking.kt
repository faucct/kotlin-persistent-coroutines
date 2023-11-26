package com.bnorm.template

import com.bnorm.template.AccountTransfer.persist
import javaslang.Tuple3
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.util.*
import kotlin.coroutines.Continuation

object TripBooking {
  @JvmStatic
  fun main(args: Array<String>) {
    println(Arrays.toString(TripBooking::class.java.getMethod("bookTrip", String::class.java, Int::class.java, Continuation::class.java).annotations))
    val bookTrip = ClassLoader.getSystemClassLoader().loadClass("com.bnorm.template.TripBooking\$bookTrip\$1")
    for (annotation in bookTrip.annotations) {
      println(annotation)
    }
    for (field in bookTrip.declaredFields) {
      println(field)
      for (annotation in field.annotations) {
        println(annotation)
      }
    }
    runBlocking {
      bookTrip("name")
      println("done")
    }
  }

  lateinit var newId: () -> Int
  lateinit var bar: suspend (Int, Int) -> Unit

  suspend fun bookTrip(@PersistedField name: String, counter: Int = 0): Unit = coroutineScope {
    if (counter > 0) {
      async { bookTrip(name, counter - 1) }.await()
      println("bookedTrip $counter")
      return@coroutineScope
    }
    delay(1000)
    @PersistedField val carReservationID = Random().nextInt()
    @PersistencePoint val _10 = persist()
    rollbackIfThrows({ println("undoing $carReservationID") }) {
      @PersistedField val hotelReservationID = Random().nextInt()
      @PersistencePoint val _20 = persist()
      rollbackIfThrows({ println("undoing $hotelReservationID") }) {
        @PersistedField val flightReservationID = Random().nextInt()
        @PersistencePoint val _30 = persist()
        @PersistencePoint val _15 = foo(newId(), newId())
        println(name)
        Tuple3(carReservationID, hotelReservationID, flightReservationID)
      }
    }
  }

  private suspend fun foo(@PersistedField foo1: Int, @PersistedField foo2: Int) {
    bar(foo1, foo2)
    println("foo")
  }

  private inline fun <T> rollbackIfThrows(rollback: () -> Unit, throwing: () -> T): T {
    try {
      return throwing()
    } catch (suppressing: Throwable) {
      try {
        rollback()
      } catch (suppressed: Throwable) {
        suppressing.addSuppressed(suppressed)
      }
      throw suppressing
    }
  }
}