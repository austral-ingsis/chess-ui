package edu.austral.dissis.chess.gui

import javafx.scene.image.Image

interface ImageResolver {
    fun resolve(imageId: String, requestedWidth: Double, requestedHeight: Double): Image
}

class DefaultImageResolver : ImageResolver {
    override fun resolve(imageId: String, requestedWidth: Double, requestedHeight: Double): Image {
        val url = resolvePath(imageId)
        val resource = DefaultImageResolver::class.java.getResourceAsStream(url)
            ?: throw IllegalArgumentException("Cannot resolve image $imageId")

        return Image(resource, requestedWidth, requestedHeight, true, true)
    }

    private fun resolvePath(imageId: String): String = "/$imageId.png"
}

class CachedImageResolver(private val baseResolver: ImageResolver) : ImageResolver {
    private data class Key(val imageId: String, val requestedWidth: Double, val requestedHeight: Double)

    private val map = mutableMapOf<Key, Image>()

    override fun resolve(imageId: String, requestedWidth: Double, requestedHeight: Double): Image =
        map.getOrPut(Key(imageId, requestedWidth, requestedHeight)) {
            baseResolver.resolve(imageId, requestedWidth, requestedHeight)
        }
}