package com.bnorm.template

import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.nio.file.StandardCopyOption

class StringPersistedToFile(val path: Path, val tmpPath: Path = Path.of("$path.tmp")) : PersistedString {
  override suspend fun getPersisted(): String? = try {
    Files.newInputStream(path).use { String(it.readAllBytes()) }
  } catch (ignored: NoSuchFileException) {
    null
  }

  override suspend fun setPersisted(value: String?) {
    if (value != null) {
      Files.newOutputStream(tmpPath).use { it.write(value.encodeToByteArray()) }
      Files.move(tmpPath, path, StandardCopyOption.ATOMIC_MOVE)
    } else {
      Files.deleteIfExists(path)
    }
  }
}
