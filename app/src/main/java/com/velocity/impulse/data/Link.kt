package com.velocity.impulse.data

/** How this phone reaches the computer, judged from the address alone. */
enum class Link(val label: String) {
    Local("Local network"),
    Tailscale("Tailscale"),
    Cloudflare("Cloudflare Tunnel"),
    Secure("Secure link"),
    Remote("Internet");

    companion object {
        fun of(url: String): Link {
            val host = url.substringAfter("://").substringBefore('/').substringBefore(':').lowercase()
            val ip = host.split('.').mapNotNull { it.toIntOrNull() }.takeIf { it.size == 4 }
            return when {
                host.endsWith(".ts.net") -> Tailscale
                ip != null && ip[0] == 100 && ip[1] in 64..127 -> Tailscale
                host.endsWith(".trycloudflare.com") || host.endsWith(".cfargotunnel.com") -> Cloudflare
                ip != null && (ip[0] == 10 || ip[0] == 192 && ip[1] == 168 || ip[0] == 172 && ip[1] in 16..31) -> Local
                host == "localhost" || ip != null && ip[0] == 127 -> Local
                host.endsWith(".local") || host.endsWith(".lan") -> Local
                url.startsWith("https://") -> Secure
                else -> Remote
            }
        }
    }
}
