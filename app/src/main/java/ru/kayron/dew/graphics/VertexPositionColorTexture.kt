package ru.kayron.dew.graphics

import android.opengl.GLES30
import ru.kayron.dew.math.Color
import ru.kayron.dew.math.Vector2
import ru.kayron.dew.math.Vector3
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

data class VertexPositionColorTexture(
    val position: Vector3 = Vector3.Zero,
    val color: Color = Color.White,
    val textureCoordinate: Vector2 = Vector2.Zero
) {
    companion object {
        val Declaration = VertexDeclaration(
            VertexElement("aPosition", 3, GLES30.GL_FLOAT),
            VertexElement("aColor", 4, GLES30.GL_FLOAT),
            VertexElement("aTexCoord", 2, GLES30.GL_FLOAT)
        )

        const val SIZE = (3 + 4 + 2) * 4 // 36 bytes

        fun toFloatBuffer(vertices: List<VertexPositionColorTexture>): FloatBuffer {
            val buffer = ByteBuffer.allocateDirect(vertices.size * SIZE)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
            for (v in vertices) {
                buffer.put(v.position.x); buffer.put(v.position.y); buffer.put(v.position.z)
                buffer.put(v.color.rf); buffer.put(v.color.gf); buffer.put(v.color.bf); buffer.put(v.color.af)
                buffer.put(v.textureCoordinate.x); buffer.put(v.textureCoordinate.y)
            }
            buffer.position(0)
            return buffer
        }
    }
}
