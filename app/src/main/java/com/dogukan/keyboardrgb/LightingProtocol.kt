package com.dogukan.keyboardrgb

/** No vendor protocol has been verified. A writable UUID is not a lighting protocol. */
interface LightingProtocol {
    val evidence: String
    val supportedEffects: Set<String>
    fun apply(effect: String): Result<Unit>
}
object UnverifiedProtocol : LightingProtocol {
    override val evidence = "156 Wireless Magic Keyboard için doğrulanmış komut veya bağımsız LED bölgesi kanıtı yok."
    override val supportedEffects = emptySet<String>()
    override fun apply(effect: String): Result<Unit> = Result.failure(UnsupportedOperationException(evidence))
}
class LightingController(private val protocol: LightingProtocol = UnverifiedProtocol) {
    fun apply(effect: String): Result<Unit> = if (effect in protocol.supportedEffects) protocol.apply(effect)
        else Result.failure(UnsupportedOperationException("Donanım desteği doğrulanmadı."))
}
