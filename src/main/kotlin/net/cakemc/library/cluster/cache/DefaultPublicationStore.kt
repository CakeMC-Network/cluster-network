package net.cakemc.library.cluster.cache

class DefaultPublicationStore : PublicationStore {
    private val cache: MutableMap<String?, SelectedAwareNodes> =
        HashMap()

    override fun updateAwareNodes(key: String?, version: Long, awareNodes: ShortArray) {
        var message = cache[key]

        if (message == null) {
            message = SelectedAwareNodes(version, awareNodes)
            cache.put(key, message)
            return
        }

        if (message.version < version) {
            message.version = version
            message.awareNodes = awareNodes
        } else if (message.version == version) {
            val combined = ShortArray(awareNodes.size + message.awareNodes.size)
            var index = 0
            for (i in message.awareNodes.indices) {
                combined[i] = message.awareNodes[index++]
            }
            for (awareNode in awareNodes) {
                combined[index] = awareNode
                index++
            }
            message.awareNodes = combined
        }
    }

    override fun getAwareNodes(key: String?, version: Long): ShortArray? {
        val message = cache[key] ?: return null

        if (message.version == version) {
            return message.awareNodes
        }

        return null
    }

    override fun shutdown() {
        cache.clear()
    }
}
