package com.ciphervault.app.crypto

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.datasource.DataSpec
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Random

@androidx.media3.common.util.UnstableApi
class CipherVaultDataSourceTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val cryptoManager = CryptoManager()

    private fun createTestDataSpec(position: Long = 0, length: Long = C.LENGTH_UNSET.toLong()): DataSpec {
        val constructor = DataSpec::class.java.getDeclaredConstructor(
            Uri::class.java,
            Long::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
            ByteArray::class.java,
            Map::class.java,
            Long::class.javaPrimitiveType,
            Long::class.javaPrimitiveType,
            String::class.java,
            Int::class.javaPrimitiveType,
            Any::class.java
        ).apply { isAccessible = true }

        return constructor.newInstance(
            null,
            0L,
            1,
            null,
            emptyMap<String, String>(),
            position,
            length,
            null,
            0,
            null
        ) as DataSpec
    }

    @Test
    fun testStreamPlaybackRead() {
        val masterKey = cryptoManager.generateMasterKey()
        val originalData = ByteArray(100_000).apply { Random(42).nextBytes(this) }

        val encryptedFile = tempFolder.newFile("test_media.enc")
        encryptedFile.outputStream().use { os ->
            cryptoManager.encrypt(ByteArrayInputStream(originalData), os, masterKey)
        }

        val dataSource = CipherVaultDataSource(encryptedFile, masterKey)
        val dataSpec = createTestDataSpec()

        val openedLength = dataSource.open(dataSpec)
        assertEquals(originalData.size.toLong(), openedLength)

        val readOutput = ByteArrayOutputStream()
        val buffer = ByteArray(4096)
        while (true) {
            val bytesRead = dataSource.read(buffer, 0, buffer.size)
            if (bytesRead == C.RESULT_END_OF_INPUT) break
            assertTrue("bytesRead should be > 0", bytesRead > 0)
            readOutput.write(buffer, 0, bytesRead)
        }
        dataSource.close()

        assertArrayEquals(originalData, readOutput.toByteArray())
    }

    @Test
    fun testSmallChunkReadsDoesNotCauseStackOverflow() {
        val masterKey = cryptoManager.generateMasterKey()
        val originalData = ByteArray(10_000).apply { Random(123).nextBytes(this) }

        val encryptedFile = tempFolder.newFile("test_small_chunks.enc")
        encryptedFile.outputStream().use { os ->
            cryptoManager.encrypt(ByteArrayInputStream(originalData), os, masterKey)
        }

        val dataSource = CipherVaultDataSource(encryptedFile, masterKey)
        val dataSpec = createTestDataSpec()

        dataSource.open(dataSpec)

        val readOutput = ByteArrayOutputStream()
        // Read 1 to 3 bytes at a time (previously caused deep recursion and StackOverflowError)
        val buffer = ByteArray(3)
        while (true) {
            val bytesRead = dataSource.read(buffer, 0, buffer.size)
            if (bytesRead == C.RESULT_END_OF_INPUT) break
            assertTrue("bytesRead should be > 0", bytesRead > 0)
            readOutput.write(buffer, 0, bytesRead)
        }
        dataSource.close()

        assertArrayEquals(originalData, readOutput.toByteArray())
    }

    @Test
    fun testSeekingInEncryptedStream() {
        val masterKey = cryptoManager.generateMasterKey()
        val originalData = ByteArray(50_000).apply { Random(999).nextBytes(this) }

        val encryptedFile = tempFolder.newFile("test_seek.enc")
        encryptedFile.outputStream().use { os ->
            cryptoManager.encrypt(ByteArrayInputStream(originalData), os, masterKey)
        }

        val seekPosition = 12_345L
        val dataSource = CipherVaultDataSource(encryptedFile, masterKey)
        val dataSpec = createTestDataSpec(position = seekPosition)

        val remainingLength = dataSource.open(dataSpec)
        assertEquals(originalData.size - seekPosition, remainingLength)

        val readOutput = ByteArrayOutputStream()
        val buffer = ByteArray(2048)
        while (true) {
            val bytesRead = dataSource.read(buffer, 0, buffer.size)
            if (bytesRead == C.RESULT_END_OF_INPUT) break
            assertTrue("bytesRead should be > 0", bytesRead > 0)
            readOutput.write(buffer, 0, bytesRead)
        }
        dataSource.close()

        val expectedData = originalData.copyOfRange(seekPosition.toInt(), originalData.size)
        assertArrayEquals(expectedData, readOutput.toByteArray())
    }
}
