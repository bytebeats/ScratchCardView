package me.bytebeats.views.scratch

import android.graphics.Bitmap
import java.nio.ByteBuffer

/**
 * Created by bytebeats on 2021/8/24 : 17:52
 * E-mail: happychinapc@gmail.com
 * Quote: Peasant. Educated. Worker
 */
object BitmapHelper {
    /**
     * Compares two bitmaps and gives the percentage of similarity
     *
     * @param bm1 input bitmap 1
     * @param bm2 input bitmap 2
     * @return a value between 0.0 to 1.0 . Note the method will return 0.0 if either of bitmaps are null nor of same size.
     *
     */
    fun compareEquivalance(bm1: Bitmap?, bm2: Bitmap?): Float {
        if (bm1 == null || bm2 == null || bm1.width != bm2.width || bm1.height != bm2.height) {
            return 0F
        }
        val buffer1 = ByteBuffer.allocate(bm1.height * bm1.rowBytes)
        bm1.copyPixelsToBuffer(buffer1)
        val buffer2 = ByteBuffer.allocate(bm2.height * bm2.rowBytes)
        bm2.copyPixelsToBuffer(buffer2)
        val array1 = buffer1.array()
        val array2 = buffer2.array()
        val size = array1.size
        var count = 0
        for (i in 0 until size) {
            if (array1[i] == array2[i]) {
                count++
            }
        }
        return count.toFloat() / size
    }

    /**
     * Finds the percentage of pixels that do are empty.
     *
     * @param bm input bitmap
     * @return a value between 0.0 to 1.0 . Note the method will return 0.0 if either of bitmaps are null nor of same size.
     *
     */
    fun transparentPixelPercent(bm: Bitmap?): Float {
        if (bm == null) {
            return 0F
        }
        val buffer = ByteBuffer.allocate(bm.height * bm.rowBytes)
        bm.copyPixelsToBuffer(buffer)
        val array = buffer.array()
        val size = array.size
        var count = 0
        for (i in 0 until size) {
            if (array[i] == 0.toByte()) {
                count++
            }
        }
        return count.toFloat() / size
    }
}