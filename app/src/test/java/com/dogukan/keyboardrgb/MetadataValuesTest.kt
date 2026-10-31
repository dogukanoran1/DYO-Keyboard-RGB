package com.dogukan.keyboardrgb

import org.junit.Assert.*
import org.junit.Test

class MetadataValuesTest {
    @Test fun pnpKeepsNamespaceAndLittleEndianUnsignedValues() {
        val result = MetadataValues.pnp(byteArrayOf(2, 0x15, 0x19, 0x40, 0, 0xff.toByte(), 0x80.toByte()))!!
        assertEquals(2, result.source)
        assertEquals(0x1915, result.vendorId)
        assertEquals(0x0040, result.productId)
        assertEquals(0x80ff, result.version)
    }
    @Test fun malformedPnpIsNotInvented() {
        for(size in listOf(0, 1, 6, 8)) assertNull(MetadataValues.pnp(ByteArray(size)))
    }
    @Test fun binaryEvidencePreservesZeroAndHighBits() {
        assertEquals("000fff80", MetadataValues.hex(byteArrayOf(0, 15, -1, -128)))
    }
}
