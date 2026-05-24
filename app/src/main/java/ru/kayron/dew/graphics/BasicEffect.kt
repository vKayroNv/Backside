package ru.kayron.dew.graphics

import android.opengl.GLES30
import ru.kayron.dew.math.Matrix
import ru.kayron.dew.math.Vector3
import ru.kayron.dew.math.Color

class BasicEffect : Effect() {

    var world: Matrix = Matrix.Identity
    var view: Matrix = Matrix.Identity
    var projection: Matrix = Matrix.Identity

    var diffuseColor: Vector3 = Vector3(1f, 1f, 1f)
    var emissiveColor: Vector3 = Vector3.Zero
    var specularColor: Vector3 = Vector3.Zero
    var specularPower: Float = 16f
    var alpha: Float = 1f

    var vertexColorEnabled: Boolean = false
    var textureEnabled: Boolean = false
    var lightingEnabled: Boolean = false

    var texture: Texture2D? = null

    private val vertexShaderSource = """
        #version 300 es
        in vec3 aPosition;
        in vec4 aColor;
        in vec2 aTexCoord;
        out vec4 vColor;
        out vec2 vTexCoord;
        uniform vec3 uDiffuseColor;
        uniform float uAlpha;
        uniform mat4 uWorld;
        uniform mat4 uView;
        uniform mat4 uProjection;
        uniform bool uVertexColorEnabled;
        void main() {
            gl_Position = uProjection * uView * uWorld * vec4(aPosition, 1.0);
            if (uVertexColorEnabled) { vColor = aColor; }
            else { vColor = vec4(uDiffuseColor, uAlpha); }
            vTexCoord = aTexCoord;
        }
    """.trimIndent()

    private val fragmentShaderSource = """
        #version 300 es
        precision mediump float;
        in vec4 vColor;
        in vec2 vTexCoord;
        out vec4 fragColor;
        uniform bool uTextureEnabled;
        uniform sampler2D uTexture;
        void main() {
            vec4 color = vColor;
            if (uTextureEnabled) { color *= texture(uTexture, vTexCoord); }
            fragColor = color;
        }
    """.trimIndent()

    init {
        linkProgram(vertexShaderSource, fragmentShaderSource)
    }

    override fun apply() {
        super.apply()
        setUniformMatrix("uWorld", world)
        setUniformMatrix("uView", view)
        setUniformMatrix("uProjection", projection)
        setUniform("uDiffuseColor", diffuseColor.x, diffuseColor.y, diffuseColor.z)
        setUniform("uAlpha", alpha)
        setUniform("uVertexColorEnabled", vertexColorEnabled)
        setUniform("uTextureEnabled", textureEnabled)
        val currentTexture = texture
        if (textureEnabled && currentTexture != null) {
            GLES30.glActiveTexture(android.opengl.GLES30.GL_TEXTURE0)
            currentTexture.bind()
            setUniform("uTexture", 0)
        }
    }

    fun enableDefaultLighting() {
        lightingEnabled = true
    }
}
