package ir.raha.hotspot
enum class Mode { ALL, ALLOW, BLOCK }
data class Policy(val mode: Mode, val packages: Set<String>) {
    init { require(mode != Mode.BLOCK || packages.isNotEmpty()) }
    fun blocks(pkg: String): Boolean = when(mode) {
        Mode.ALL -> true
        Mode.ALLOW -> pkg !in packages
        Mode.BLOCK -> pkg in packages
    }
}