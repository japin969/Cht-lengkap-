package com.jayra.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object JayraIcons {
    private fun vector(name: String, block: ImageVector.Builder.() -> Unit): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply(block).build()

    val Search: ImageVector by lazy { vector("Search") {
        path(fill = SolidColor(Color.Black)) {
            moveTo(15.5f, 14f); lineTo(14.71f, 14f); lineTo(14.43f, 13.73f)
            curveTo(15.41f, 12.59f, 16f, 11.11f, 16f, 9.5f)
            curveTo(16f, 5.91f, 13.09f, 3f, 9.5f, 3f); curveTo(5.91f, 3f, 3f, 5.91f, 3f, 9.5f)
            curveTo(3f, 13.09f, 5.91f, 16f, 9.5f, 16f)
            curveTo(11.11f, 16f, 12.59f, 15.41f, 13.73f, 14.43f); lineTo(14f, 14.71f); lineTo(14f, 15.5f)
            lineTo(19f, 20.49f); lineTo(20.49f, 19f); lineTo(15.5f, 14f); close()
            moveTo(9.5f, 14f); curveTo(7.01f, 14f, 5f, 11.99f, 5f, 9.5f)
            curveTo(5f, 7.01f, 7.01f, 5f, 9.5f, 5f); curveTo(11.99f, 5f, 14f, 7.01f, 14f, 9.5f)
            curveTo(14f, 11.99f, 11.99f, 14f, 9.5f, 14f); close()
        }
    } }

    val Phone: ImageVector by lazy { vector("Phone") {
        path(fill = SolidColor(Color.Black)) {
            moveTo(6.62f, 10.79f); curveTo(8.06f, 13.62f, 10.38f, 15.94f, 13.21f, 17.38f)
            lineTo(15.41f, 15.18f); curveTo(15.68f, 14.91f, 16.08f, 14.83f, 16.43f, 14.95f)
            curveTo(17.55f, 15.32f, 18.75f, 15.52f, 20f, 15.52f)
            curveTo(20.55f, 15.52f, 21f, 15.97f, 21f, 16.52f); lineTo(21f, 20f)
            curveTo(21f, 20.55f, 20.55f, 21f, 20f, 21f)
            curveTo(10.61f, 21f, 3f, 13.39f, 3f, 4f); curveTo(3f, 3.45f, 3.45f, 3f, 4f, 3f)
            lineTo(7.5f, 3f); curveTo(8.05f, 3f, 8.5f, 3.45f, 8.5f, 4f)
            curveTo(8.5f, 5.25f, 8.7f, 6.45f, 9.07f, 7.57f); curveTo(9.18f, 7.92f, 9.1f, 8.31f, 8.83f, 8.58f)
            lineTo(6.62f, 10.79f); close()
        }
    } }

    val Warning: ImageVector by lazy { vector("Warning") {
        path(fill = SolidColor(Color.Black)) {
            moveTo(1f, 21f); lineTo(23f, 21f); lineTo(12f, 2f); lineTo(1f, 21f); close()
            moveTo(13f, 18f); lineTo(11f, 18f); lineTo(11f, 16f); lineTo(13f, 16f); lineTo(13f, 18f); close()
            moveTo(13f, 14f); lineTo(11f, 14f); lineTo(11f, 10f); lineTo(13f, 10f); lineTo(13f, 14f); close()
        }
    } }

    val Close: ImageVector by lazy { vector("Close") {
        path(fill = SolidColor(Color.Black)) {
            moveTo(19f, 6.41f); lineTo(17.59f, 5f); lineTo(12f, 10.59f); lineTo(6.41f, 5f)
            lineTo(5f, 6.41f); lineTo(10.59f, 12f); lineTo(5f, 17.59f); lineTo(6.41f, 19f)
            lineTo(12f, 13.41f); lineTo(17.59f, 19f); lineTo(19f, 17.59f); lineTo(13.41f, 12f); lineTo(19f, 6.41f); close()
        }
    } }

    val Video: ImageVector by lazy { vector("Video") {
        path(fill = SolidColor(Color.Black)) {
            moveTo(17f, 10.5f); lineTo(21f, 6.5f); lineTo(21f, 17.5f); lineTo(17f, 13.5f); lineTo(17f, 16f)
            curveTo(17f, 16.55f, 16.55f, 17f, 16f, 17f); lineTo(4f, 17f)
            curveTo(3.45f, 17f, 3f, 16.55f, 3f, 16f); lineTo(3f, 8f)
            curveTo(3f, 7.45f, 3.45f, 7f, 4f, 7f); lineTo(16f, 7f)
            curveTo(16.55f, 7f, 17f, 7.45f, 17f, 8f); lineTo(17f, 10.5f); close()
        }
    } }

    val Person: ImageVector by lazy { vector("Person") {
        path(fill = SolidColor(Color.Black)) {
            moveTo(12f, 12f); curveTo(14.21f, 12f, 16f, 10.21f, 16f, 8f)
            curveTo(16f, 5.79f, 14.21f, 4f, 12f, 4f); curveTo(9.79f, 4f, 8f, 5.79f, 8f, 8f)
            curveTo(8f, 10.21f, 9.79f, 12f, 12f, 12f); close()
            moveTo(12f, 14f); curveTo(9.33f, 14f, 4f, 15.34f, 4f, 18f); lineTo(4f, 20f); lineTo(20f, 20f)
            lineTo(20f, 18f); curveTo(20f, 15.34f, 14.67f, 14f, 12f, 14f); close()
        }
    } }

    val Delete: ImageVector by lazy { vector("Delete") {
        path(fill = SolidColor(Color.Black)) {
            moveTo(6f, 19f); curveTo(6f, 20.1f, 6.9f, 21f, 8f, 21f); lineTo(16f, 21f)
            curveTo(17.1f, 21f, 18f, 20.1f, 18f, 19f); lineTo(18f, 7f); lineTo(6f, 7f); lineTo(6f, 19f); close()
            moveTo(8f, 9f); lineTo(16f, 9f); lineTo(16f, 19f); lineTo(8f, 19f); lineTo(8f, 9f); close()
            moveTo(15.5f, 4f); lineTo(14.5f, 3f); lineTo(9.5f, 3f); lineTo(8.5f, 4f); lineTo(5f, 4f)
            lineTo(5f, 6f); lineTo(19f, 6f); lineTo(19f, 4f); close()
        }
    } }

    val PersonAdd: ImageVector by lazy { vector("PersonAdd") {
        path(fill = SolidColor(Color.Black)) {
            moveTo(15f, 12f); curveTo(16.66f, 12f, 18f, 10.66f, 18f, 9f)
            curveTo(18f, 7.34f, 16.66f, 6f, 15f, 6f); curveTo(13.34f, 6f, 12f, 7.34f, 12f, 9f)
            curveTo(12f, 10.66f, 13.34f, 12f, 15f, 12f); close()
            moveTo(6f, 10f); lineTo(6f, 7f); lineTo(4f, 7f); lineTo(4f, 10f); lineTo(1f, 10f); lineTo(1f, 12f)
            lineTo(4f, 12f); lineTo(4f, 15f); lineTo(6f, 15f); lineTo(6f, 12f); lineTo(9f, 12f); lineTo(9f, 10f); lineTo(6f, 10f); close()
            moveTo(15f, 14f); curveTo(12.99f, 14f, 9f, 15.01f, 9f, 17f); lineTo(9f, 20f); lineTo(21f, 20f)
            lineTo(21f, 17f); curveTo(21f, 15.01f, 17.01f, 14f, 15f, 14f); close()
        }
    } }

    val Check: ImageVector by lazy { vector("Check") {
        path(fill = SolidColor(Color.Black)) {
            moveTo(9f, 16.17f); lineTo(4.83f, 12f); lineTo(3.41f, 13.41f); lineTo(9f, 19f)
            lineTo(21f, 7f); lineTo(19.59f, 5.59f); lineTo(9f, 16.17f); close()
        }
    } }

    val MusicNote: ImageVector by lazy { vector("MusicNote") {
        path(fill = SolidColor(Color.Black)) {
            moveTo(12f, 3f); lineTo(12f, 15.55f); curveTo(11.41f, 15.21f, 10.73f, 15f, 10f, 15f)
            curveTo(7.79f, 15f, 6f, 16.79f, 6f, 19f); curveTo(6f, 21.21f, 7.79f, 23f, 10f, 23f)
            curveTo(12.21f, 23f, 14f, 21.21f, 14f, 19f); lineTo(14f, 7f); lineTo(18f, 7f); lineTo(18f, 3f); lineTo(12f, 3f); close()
        }
    } }

    val Security: ImageVector by lazy { vector("Security") {
        path(fill = SolidColor(Color.Black)) {
            moveTo(12f, 1f); lineTo(3f, 5f); lineTo(3f, 11f)
            curveTo(3f, 16.55f, 6.84f, 21.74f, 12f, 23f); curveTo(17.16f, 21.74f, 21f, 16.55f, 21f, 11f)
            lineTo(21f, 5f); lineTo(12f, 1f); close()
            moveTo(12f, 21f); curveTo(8.11f, 19.84f, 5f, 15.66f, 5f, 11.32f); lineTo(5f, 6.3f); lineTo(12f, 3.18f)
            lineTo(19f, 6.3f); lineTo(19f, 11.32f); curveTo(19f, 15.66f, 15.89f, 19.84f, 12f, 21f); close()
        }
    } }

    val MoreVert: ImageVector by lazy { vector("MoreVert") {
        path(fill = SolidColor(Color.Black)) {
            moveTo(12f, 8f); curveTo(13.1f, 8f, 14f, 7.1f, 14f, 6f); curveTo(14f, 4.9f, 13.1f, 4f, 12f, 4f)
            curveTo(10.9f, 4f, 10f, 4.9f, 10f, 6f); curveTo(10f, 7.1f, 10.9f, 8f, 12f, 8f); close()
            moveTo(12f, 10f); curveTo(10.9f, 10f, 10f, 10.9f, 10f, 12f); curveTo(10f, 13.1f, 10.9f, 14f, 12f, 14f)
            curveTo(13.1f, 14f, 14f, 13.1f, 14f, 12f); curveTo(14f, 10.9f, 13.1f, 10f, 12f, 10f); close()
            moveTo(12f, 16f); curveTo(10.9f, 16f, 10f, 16.9f, 10f, 18f); curveTo(10f, 19.1f, 10.9f, 20f, 12f, 20f)
            curveTo(13.1f, 20f, 14f, 19.1f, 14f, 18f); curveTo(14f, 16.9f, 13.1f, 16f, 12f, 16f); close()
        }
    } }

    val PlayArrow: ImageVector by lazy { vector("PlayArrow") {
        path(fill = SolidColor(Color.Black)) {
            moveTo(8f, 5f); lineTo(19f, 12f); lineTo(8f, 19f); lineTo(8f, 5f); close()
        }
    } }

    val Chat: ImageVector by lazy { vector("Chat") {
        path(fill = SolidColor(Color.Black)) {
            moveTo(20f, 2f); lineTo(4f, 2f); curveTo(2.9f, 2f, 2f, 2.9f, 2f, 4f)
            lineTo(2f, 22f); lineTo(6f, 18f); lineTo(20f, 18f); curveTo(21.1f, 18f, 22f, 17.1f, 22f, 16f)
            lineTo(22f, 4f); curveTo(22f, 2.9f, 21.1f, 2f, 20f, 2f); close()
            moveTo(20f, 16f); lineTo(6f, 16f); lineTo(4f, 18f); lineTo(4f, 4f); lineTo(20f, 4f); lineTo(20f, 16f); close()
        }
    } }

    val Visibility: ImageVector by lazy { vector("Visibility") {
        path(fill = SolidColor(Color.Black)) {
            moveTo(12f, 4.5f); curveTo(7f, 4.5f, 2.73f, 7.61f, 1f, 12f)
            curveTo(2.73f, 16.39f, 7f, 19.5f, 12f, 19.5f); curveTo(17f, 19.5f, 21.27f, 16.39f, 23f, 12f)
            curveTo(21.27f, 7.61f, 17f, 4.5f, 12f, 4.5f); close()
            moveTo(12f, 17f); curveTo(9.24f, 17f, 7f, 14.76f, 7f, 12f); curveTo(7f, 9.24f, 9.24f, 7f, 12f, 7f)
            curveTo(14.76f, 7f, 17f, 9.24f, 17f, 12f); curveTo(17f, 14.76f, 14.76f, 17f, 12f, 17f); close()
            moveTo(12f, 9f); curveTo(10.34f, 9f, 9f, 10.34f, 9f, 12f); curveTo(9f, 13.66f, 10.34f, 15f, 12f, 15f)
            curveTo(13.66f, 15f, 15f, 13.66f, 15f, 12f); curveTo(15f, 10.34f, 13.66f, 9f, 12f, 9f); close()
        }
    } }

    val VisibilityOff: ImageVector by lazy { vector("VisibilityOff") {
        path(fill = SolidColor(Color.Black)) {
            moveTo(12f, 7f); curveTo(14.76f, 7f, 17f, 9.24f, 17f, 12f); curveTo(17f, 12.65f, 16.87f, 13.26f, 16.64f, 13.83f)
            lineTo(19.56f, 16.75f); curveTo(21.07f, 15.49f, 22.26f, 13.86f, 23f, 12f)
            curveTo(21.27f, 7.61f, 17f, 4.5f, 12f, 4.5f); curveTo(10.6f, 4.5f, 9.26f, 4.75f, 8.01f, 5.2f)
            lineTo(10.17f, 7.36f); curveTo(10.74f, 7.13f, 11.35f, 7f, 12f, 7f); close()
            moveTo(1f, 4.27f); lineTo(3.28f, 6.55f); lineTo(3.74f, 7.01f)
            curveTo(2.08f, 8.3f, 0.78f, 10.02f, 0f, 12f); curveTo(1.73f, 16.39f, 6f, 19.5f, 11f, 19.5f)
            curveTo(12.73f, 19.5f, 14.39f, 19.13f, 15.89f, 18.47f); lineTo(16.42f, 19f); lineTo(19.73f, 22.27f)
            lineTo(21f, 21f); lineTo(2.27f, 3f); lineTo(1f, 4.27f); close()
            moveTo(12f, 17f); curveTo(9.24f, 17f, 7f, 14.76f, 7f, 12f); curveTo(7f, 11.27f, 7.17f, 10.58f, 7.47f, 9.96f)
            lineTo(9.19f, 11.68f); curveTo(9.14f, 11.89f, 9.11f, 12.11f, 9.11f, 12.32f); curveTo(9.11f, 14.18f, 10.67f, 15.74f, 12.53f, 15.74f)
            curveTo(12.74f, 15.74f, 12.96f, 15.71f, 13.17f, 15.66f); lineTo(14.89f, 17.38f)
            curveTo(14.27f, 17.68f, 13.58f, 17.85f, 12.85f, 17.85f); close()
        }
    } }

    val Add: ImageVector by lazy { vector("Add") {
        path(fill = SolidColor(Color.Black)) {
            moveTo(19f, 13f); lineTo(13f, 13f); lineTo(13f, 19f); lineTo(11f, 19f); lineTo(11f, 13f); lineTo(5f, 13f); lineTo(5f, 11f); lineTo(11f, 11f); lineTo(11f, 5f); lineTo(13f, 5f); lineTo(13f, 11f); lineTo(19f, 11f); close()
        }
    } }

    val Settings: ImageVector by lazy { vector("Settings") {
        path(fill = SolidColor(Color.Black)) {
            moveTo(19.14f, 12.94f); curveTo(19.18f, 12.64f, 19.2f, 12.33f, 19.2f, 12f)
            curveTo(19.2f, 11.68f, 19.18f, 11.36f, 19.13f, 11.06f)
            lineTo(21.16f, 9.48f); curveTo(21.34f, 9.34f, 21.39f, 9.08f, 21.27f, 8.87f)
            lineTo(19.36f, 5.56f); curveTo(19.24f, 5.35f, 18.99f, 5.28f, 18.77f, 5.35f)
            lineTo(16.38f, 6.31f); curveTo(15.88f, 5.93f, 15.34f, 5.61f, 14.75f, 5.38f)
            lineTo(14.38f, 2.84f); curveTo(14.35f, 2.61f, 14.15f, 2.44f, 13.92f, 2.44f)
            lineTo(10.08f, 2.44f); curveTo(9.85f, 2.44f, 9.66f, 2.61f, 9.63f, 2.84f)
            lineTo(9.26f, 5.38f); curveTo(8.66f, 5.61f, 8.12f, 5.94f, 7.62f, 6.31f)
            lineTo(5.23f, 5.35f); curveTo(5.02f, 5.28f, 4.76f, 5.35f, 4.65f, 5.56f)
            lineTo(2.74f, 8.87f); curveTo(2.62f, 9.08f, 2.67f, 9.34f, 2.85f, 9.48f)
            lineTo(4.88f, 11.06f); curveTo(4.83f, 11.36f, 4.8f, 11.69f, 4.8f, 12f)
            curveTo(4.8f, 12.31f, 4.82f, 12.64f, 4.87f, 12.94f)
            lineTo(2.84f, 14.52f); curveTo(2.66f, 14.66f, 2.61f, 14.92f, 2.73f, 15.13f)
            lineTo(4.64f, 18.44f); curveTo(4.76f, 18.65f, 5.01f, 18.72f, 5.23f, 18.65f)
            lineTo(7.62f, 17.69f); curveTo(8.12f, 18.07f, 8.66f, 18.39f, 9.25f, 18.62f)
            lineTo(9.62f, 21.16f); curveTo(9.66f, 21.39f, 9.85f, 21.56f, 10.08f, 21.56f)
            lineTo(13.92f, 21.56f); curveTo(14.15f, 21.56f, 14.35f, 21.39f, 14.38f, 21.16f)
            lineTo(14.75f, 18.62f); curveTo(15.34f, 18.39f, 15.88f, 18.06f, 16.38f, 17.69f)
            lineTo(18.77f, 18.65f); curveTo(18.99f, 18.72f, 19.24f, 18.65f, 19.36f, 18.44f)
            lineTo(21.27f, 15.13f); curveTo(21.39f, 14.92f, 21.34f, 14.66f, 21.16f, 14.52f)
            lineTo(19.14f, 12.94f); close()
            moveTo(12f, 15.5f); curveTo(10.07f, 15.5f, 8.5f, 13.93f, 8.5f, 12f)
            curveTo(8.5f, 10.07f, 10.07f, 8.5f, 12f, 8.5f); curveTo(13.93f, 8.5f, 15.5f, 10.07f, 15.5f, 12f)
            curveTo(15.5f, 13.93f, 13.93f, 15.5f, 12f, 15.5f); close()
        }
    } }

    // Tambahan: Sparkles untuk tombol AI
    val Sparkles: ImageVector by lazy { vector("Sparkles") {
        path(fill = SolidColor(Color.Black)) {
            moveTo(12f, 2f); lineTo(13.5f, 8.5f); lineTo(20f, 10f); lineTo(13.5f, 11.5f)
            lineTo(12f, 18f); lineTo(10.5f, 11.5f); lineTo(4f, 10f); lineTo(10.5f, 8.5f); close()
            moveTo(5f, 3f); lineTo(5.75f, 6.25f); lineTo(9f, 7f); lineTo(5.75f, 7.75f)
            lineTo(5f, 11f); lineTo(4.25f, 7.75f); lineTo(1f, 7f); lineTo(4.25f, 6.25f); close()
            moveTo(19f, 14f); lineTo(19.6f, 17.4f); lineTo(23f, 18f); lineTo(19.6f, 18.6f)
            lineTo(19f, 22f); lineTo(18.4f, 18.6f); lineTo(15f, 18f); lineTo(18.4f, 17.4f); close()
        }
    } }
}
