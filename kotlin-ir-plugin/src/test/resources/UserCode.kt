import com.bnorm.template.*
import com.bnorm.template.PersistingWrapper.wrapper
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import java.util.*
import kotlin.coroutines.coroutineContext

@Suppress("unused")
class UserCode {
  @PersistableContinuation("foo")
  suspend fun foo() {
    @PersistedField val a = "a"
    println("hi")
    @PersistencePoint("barring") val barring = persist()
    @PersistencePoint("delaying") val delaying = bar()
    println("then $a")
    delay(1000)
    yield()
    println("later")
  }

  @PersistableContinuation("bar")
  suspend fun bar() {
    println("bar")
    @PersistencePoint("delaying") val delaying = persist()
    println("then")
    delay(1000)
    yield()
    println("later")
  }

  suspend fun supplier(): Int {
    persist()
    return 0
  }

  suspend fun consumer(a: Int, b: Int) {
    println("bar")
    persist()
    println("then")
    delay(1000)
    yield()
    println("later")
  }

  suspend fun supplierConsumer() {
    consumer(
      Random().nextInt(),
      supplier(),
    )
  }

  companion object {
    var persisting = false

    @JvmStatic
    @PersistableContinuation("persist")
    suspend fun persist() {
      persisting = true
      @PersistencePoint("persist") val persist = coroutineContext[Persistor]!!.persist()
      if (persisting) {
        persisting = false
        try {
          coroutineContext[Dispose]!!.dispose()
        } finally {
          println("NOPE")
        }
      }
    }

    @JvmStatic
    fun main(persistedString: PersistedString) {
      val main = UserCode()
      val json = Json {
        serializersModule = SerializersModule {
          contextual(UserCode::class, SingletonKSerializer(main))
          contextual(Companion::class, SingletonKSerializer(Companion))
        }
      }
      val disposableCoroutine = DisposableCoroutine()
      runBlocking(disposableCoroutine) {
        disposableCoroutine {
          (wrapper(json, MapClassSerializerFactory(UserCode::class), persistedString)) @PersistableContinuation("main") {
            @PersistencePoint("fooing") val fooing = UserCode().foo()
            @PersistencePoint("done") val done = persist()
            println("done")
          }
        }
      }
    }
  }
}
