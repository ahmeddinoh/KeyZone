package com.ciphervault.app.totp

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/**
 * Generates QR code bitmaps for TOTP enrollment URIs.
 * Uses the ZXing library — fully offline, no network calls.
 */
object QrCodeGenerator {

    /**
     * Generates a QR code bitmap from an otpauth:// URI string.
     *
     * @param content The string to encode (typically an otpauth://totp/... URI)
     * @param size Width and height of the generated bitmap in pixels
     * @return A [Bitmap] containing the QR code (black modules on white background)
     */
    fun generate(content: String, size: Int = 512): Bitmap {
        val hints = mapOf(
            EncodeHintType.MARGIN to 2,
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M
        )

        val bitMatrix = QRCodeWriter().encode(
            content,
            BarcodeFormat.QR_CODE,
            size,
            size,
            hints
        )

        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        return bitmap
    }
}
