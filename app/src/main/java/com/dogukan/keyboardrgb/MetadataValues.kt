package com.dogukan.keyboardrgb

object MetadataValues {
    data class Pnp(val source: Int, val vendorId: Int, val productId: Int, val version: Int)
    fun pnp(bytes: ByteArray): Pnp? {
        if(bytes.size != 7) return null
        fun u16(offset: Int) = (bytes[offset].toInt() and 255) or ((bytes[offset + 1].toInt() and 255) shl 8)
        return Pnp(bytes[0].toInt() and 255, u16(1), u16(3), u16(5))
    }
    fun hex(bytes: ByteArray): String = bytes.joinToString("") { "%02x".format(it.toInt() and 255) }
}
