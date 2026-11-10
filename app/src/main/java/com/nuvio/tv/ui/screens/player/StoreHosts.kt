package com.nuvio.tv.ui.screens.player

/** TorBox store hosts (store-N.region.tb-cdn.*): their films run on bigger pieces, so fewer requests carry the same film. */
internal object StoreHosts {
    const val STORE_PIECE_BYTES = 16L * 1024 * 1024
    private val STORE_HOST = Regex("""^store(-\d+)?\.[^.]+\.tb-cdn\.[^.]+$""")

    fun isStoreHost(host: String): Boolean = STORE_HOST.matches(host.lowercase())

    fun isStoreUrl(url: String?): Boolean {
        val host = url?.let { runCatching { java.net.URI(it).host }.getOrNull() } ?: return false
        return isStoreHost(host)
    }
}
