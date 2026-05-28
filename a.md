=== ./README.md ===
# Backside / Dew

Backside - Android game app `ru.kayron.backside`. Внутри репозитория лежит маленький XNA/MonoGame-подобный движок Dew (`ru.kayron.dew`) на Kotlin и OpenGL ES 3.0.

Код приложения и движка находится в `app/src/main/java/`. Каталог `src/main/kotlin/` в корне не используется.

## Быстрый старт

Минимальная игра состоит из `DewActivity`, которая создаёт наследника `Game`.

```kotlin
package ru.kayron.backside

import ru.kayron.dew.DewActivity
import ru.kayron.dew.Game

class MainActivity : DewActivity() {
    override fun createGame(): Game = MyGame()
}
```

```kotlin
package ru.kayron.backside

import ru.kayron.dew.Game
import ru.kayron.dew.GameTime
import ru.kayron.dew.graphics.*
import ru.kayron.dew.input.Keyboard
import ru.kayron.dew.input.Keys
import ru.kayron.dew.math.*

class MyGame : Game() {
    private lateinit var batch: SpriteBatch
    private lateinit var texture: Texture2D
    private lateinit var font: SpriteFont
    private var position = Vector2(100f, 100f)

    override fun loadContent() {
        batch = SpriteBatch(graphicsDevice)
        texture = content.load("player.png")
        font = SpriteFont.fromSystemFont("monospace", 32f)
    }

    override fun update(gameTime: GameTime) {
        val dt = gameTime.elapsedGameTimeSeconds
        if (Keyboard.getState().isKeyDown(Keys.DpadRight)) {
            position.x += 300f * dt
        }
        if (Keyboard.getState().isKeyDown(Keys.Back)) {
            exit()
        }
    }

    override fun draw(gameTime: GameTime) {
        graphicsDevice.clear(Color.CornflowerBlue)
        batch.begin(
            depthStencilState = DepthStencilState.None,
            rasterizerState = RasterizerState.CullNone
        )
        batch.draw(texture, position, Color.White)
        batch.drawString(font, "Hello Dew", Vector2(32f, 32f), Color.White)
        batch.end()
    }

    override fun unloadContent() {
        content.unload()
    }

    override fun dispose() {
        batch.dispose()
        super.dispose()
    }
}
```

Сборка:

```sh
./gradlew :app:assembleDebug
./gradlew :app:assembleRelease
```

Если `gradlew` не исполняемый, запускайте через shell:

```sh
bash ./gradlew :app:assembleDebug
bash ./gradlew :app:assembleRelease
```

Release build включает R8 minify и resource shrink. Для публикации понадобится настроить release signing отдельно.

Текущая проверенная конфигурация: compileSdk 34, targetSdk 34, AGP 8.9.2, Kotlin 2.2.21. В локальном SDK есть android-35, но используемый `aapt2` override стабильно собирает проект на build-tools 34.0.0.

## Жизненный цикл

### DewActivity

`DewActivity` - базовая Android activity. Она скрывает системный UI, создаёт `DewGameView`, передаёт Android `AssetManager` в `game.content`, ставит игру на паузу/возобновляет её и вызывает `game.dispose()` в `onDestroy()`.

Главная функция:

```kotlin
protected abstract fun createGame(): Game
```

Back button обрабатывается через `OnBackPressedDispatcher` и вызывает `game.exit()`.

### DewGameView

`DewGameView` - `GLSurfaceView` с GLES 3.0 и `RENDERMODE_CONTINUOUSLY`. Manifest требует `android:glEsVersion="0x00030000"`.

Что делает:

- `onSurfaceChanged()` обновляет размер окна, viewport и `presentationParameters`.
- При первом изменении surface вызывает `game.run()`.
- Каждый кадр вызывает `game.tick()`.
- Touch и key events прокидывает в `Game`.
- Joystick motion events прокидывает в `GamePad`.

Обычно создавать `DewGameView` вручную не нужно: это делает `DewActivity`.

### Game

`Game` - базовый класс игры.

Основные поля:

- `graphicsDevice: GraphicsDevice` - низкоуровневый GLES-рендер.
- `graphicsDeviceManager: GraphicsDeviceManager` - параметры backbuffer.
- `content: ContentManager` - загрузка ассетов из `assets/Content`.
- `gameWindow: GameWindow` - размер клиентской области.
- `services: GameServiceContainer` - контейнер сервисов.
- `isActive`, `isMouseVisible`, `exitRequested` - состояние игры.

Переопределяемые методы:

```kotlin
override fun loadContent() {}
override fun unloadContent() {}
override fun update(gameTime: GameTime) {}
override fun draw(gameTime: GameTime) {}
override fun dispose() {}
```

Методы управления:

- `initialize()` - создаёт graphics device, инициализирует компоненты, вызывает `loadContent()`.
- `run()` - запускает игру и инициализирует её.
- `tick(): Boolean` - один кадр: input update, `update()`, `draw()`, `present()`.
- `exit()` - просит закрыть игру.
- `resetElapsedTime()` - сбрасывает точку отсчёта времени.
- `components(): List<GameComponent>` - возвращает копию списка компонентов.

Важно: `tick()` ограничивает `elapsedGameTime` максимумом 50 мс, чтобы после паузы не было большого шага симуляции.

```kotlin
override fun update(gameTime: GameTime) {
    val dt = gameTime.elapsedGameTimeSeconds
    playerPosition += velocity * dt
}
```

### GameTime

`GameTime(totalGameTime, elapsedGameTime, isRunningSlowly)` хранит время в наносекундах.

Удобные свойства:

- `totalGameTimeSeconds: Float`
- `elapsedGameTimeSeconds: Float`

## Компоненты и сервисы

### GameComponent

`GameComponent` реализует `IGameComponent`, `IUpdateable`, `IDrawable`.

Поля:

- `enabled` - если `false`, `Game.update()` не вызовет компонент.
- `visible` - если `false`, `Game.draw()` не вызовет компонент.
- `updateOrder`, `drawOrder` - используются для сортировки компонентов в `Game.update()` и `Game.draw()`.

```kotlin
class Spinner(game: Game) : GameComponent(game) {
    var angle = 0f

    override fun update(gameTime: GameTime) {
        angle += gameTime.elapsedGameTimeSeconds
    }
}
```

Компоненты добавляются и удаляются через `Game`:

```kotlin
val spinner = Spinner(this)
addComponent(spinner)
removeComponent(spinner)
```

### DrawableGameComponent

То же, что `GameComponent`, но семантически предназначен для отрисовываемых компонентов.

```kotlin
class Hud(game: Game, private val batch: SpriteBatch) : DrawableGameComponent(game) {
    override fun draw(gameTime: GameTime) {
        // draw HUD
    }
}
```

### GameServiceContainer

Простой type-keyed контейнер:

- `addService(type, provider)`
- `getService(type)`
- `removeService(type)`

```kotlin
interface ScoreService {
    var score: Int
}

services.addService(ScoreService::class.java, object : ScoreService {
    override var score = 0
})

val score = services.getService(ScoreService::class.java)?.score
```

## ContentManager и ассеты

Ассеты кладутся в:

```text
app/src/main/assets/Content/
```

Загрузка:

```kotlin
val texture: Texture2D = content.load("player.png")
val sound: SoundEffect = content.load("shot.wav")
val json: String = content.load("levels/level1.json")
```

Поддерживаемые типы:

- изображения: `.png`, `.jpg`, `.jpeg`, `.bmp` -> `Texture2D`
- звук: `.wav` -> `SoundEffect`
- звук: `.ogg` -> `SoundEffect` с декодированием через Android `MediaExtractor`/`MediaCodec`
- текст: `.txt`, `.json`, `.xml`, `.glsl` -> `String`

Функции:

- `load<T>(path)` - загружает и кэширует asset.
- `getLoaded<T>(key)` - возвращает уже загруженный asset.
- `unload()` / `dispose()` - dispose для `Texture2D` и `SoundEffect`, затем очистка кэша.

По умолчанию `rootDirectory` равен `Content`; его можно изменить через `setRootDirectory(path)`.

```kotlin
content.setRootDirectory("Content")
```

## Графика

### GraphicsDevice

`GraphicsDevice` - тонкая обёртка над GLES30.

Основные поля:

- `presentationParameters`
- `viewport`
- `blendState`, `rasterizerState`, `depthStencilState`
- `samplerStates`, `textures`
- `vertexBuffer`, `indexBuffer`
- `displayMode`

Функции:

- `clear(color)`
- `clear(options, color, depth, stencil)`
- `setViewport(x, y, width, height)` и `setViewport(viewport)`
- `setRenderTarget(renderTarget)` / `getRenderTarget()`
- `setVertexBuffer(buffer)` / `setIndexBuffer(buffer)`
- `drawPrimitives(type, startVertex, vertexCount)`
- `drawIndexedPrimitives(type, baseVertex, startIndex, primitiveCount)`
- `setScissorRect(rect)`
- `present()`
- `dispose()`

```kotlin
graphicsDevice.clear(Color.Black)
graphicsDevice.setViewport(0, 0, gameWindow.clientBounds.width, gameWindow.clientBounds.height)
```

### SpriteBatch

`SpriteBatch` рисует 2D-текстуры и текст. Координаты экранные: `(0, 0)` в левом верхнем углу.

Частый шаблон для UI и 2D:

```kotlin
batch.begin(
    sortMode = SpriteSortMode.Deferred,
    blendState = BlendState.AlphaBlend,
    samplerState = SamplerState.LinearClamp,
    depthStencilState = DepthStencilState.None,
    rasterizerState = RasterizerState.CullNone
)
batch.draw(texture, Vector2(100f, 100f), Color.White)
batch.end()
```

Функции:

- `begin(sortMode, blendState, samplerState, depthStencilState, rasterizerState, effect)`
- `draw(texture, position, sourceRectangle, color, rotation, origin, scale, effects, layerDepth)`
- `draw(texture, destinationRectangle, sourceRectangle, color, rotation, origin, effects, layerDepth)`
- `draw(texture, position, color)`
- `draw(texture, rectangle, color)`
- `drawString(spriteFont, text, position, color)`
- `end()`
- `dispose()`

Примеры:

```kotlin
batch.draw(texture, Vector2(32f, 64f), Color.White)

batch.draw(
    texture = atlas,
    position = Vector2(200f, 120f),
    sourceRectangle = Rectangle(0, 0, 32, 32),
    color = Color.White,
    rotation = MathHelper.toRadians(45f),
    origin = Vector2(16f, 16f),
    scale = Vector2(3f, 3f),
    effects = SpriteEffects.FlipHorizontally,
    layerDepth = 0.5f
)

batch.draw(
    texture = panel,
    destinationRectangle = Rectangle(10, 10, 300, 80),
    color = Color(255, 255, 255, 180)
)
```

Особенности текущей реализации:

- `SpriteSortMode.Texture` сортирует спрайты по GL texture id и снижает число переключений текстур.
- В остальных режимах `SpriteBatch` сохраняет порядок/глубину и автоматически разбивает flush на последовательные группы с одинаковой текстурой.
- Внутренний VBO переиспользуется между flush, чтобы не создавать GL buffer каждый кадр.
- Для 2D обычно нужно передавать `DepthStencilState.None` и `RasterizerState.CullNone`, иначе depth/culling из `GraphicsDevice.initialize()` может скрыть спрайты.

### SpriteFont

`SpriteFont` генерирует bitmap atlas из Android system font.

Функции:

- `SpriteFont.fromSystemFont(fontName, fontSize, characters)`
- `measureString(text): Vector2`
- `draw(batch, text, position, color)` - обычно вызывается через `SpriteBatch.drawString()`.

```kotlin
val font = SpriteFont.fromSystemFont("monospace", 48f)
val size = font.measureString("FPS: 60")
batch.drawString(font, "FPS: 60", Vector2(20f, 20f), Color.Yellow)
```

Если нужны символы не из стандартной строки `characters`, передайте свой набор:

```kotlin
val font = SpriteFont.fromSystemFont(
    fontName = "sans-serif",
    fontSize = 36f,
    characters = "0123456789HP: /"
)
```

### Texture2D и Texture

`Texture` - базовый класс с `glTexture`, `levelCount`, `format`, `bind()`, `dispose()`.

`Texture2D(width, height, mipMap, format)` создаёт GLES texture.

Функции:

- `setData(color: IntArray)`
- `setData(color: IntArray, startX, startY, width, height)`
- `setData(bitmap: Bitmap)`
- `getData(): IntArray`
- `bind()`
- `dispose()`
- `Texture2D.fromBitmap(bitmap, mipMap, linear)`
- `Texture2D.fromAsset(path, mipMap)` - загружает изображение через Android `AssetManager`, который устанавливает `DewActivity`.

```kotlin
val bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
val texture = Texture2D.fromBitmap(bitmap, linear = false)
```

### RenderTarget2D

`RenderTarget2D` наследуется от `Texture2D` и создаёт framebuffer.

```kotlin
val target = RenderTarget2D(512, 512, depthStencilFormat = PresentationParameters.DepthFormat.None)

graphicsDevice.setRenderTarget(target)
graphicsDevice.clear(Color.Transparent)
// draw to target
graphicsDevice.setRenderTarget(null)

batch.begin(depthStencilState = DepthStencilState.None, rasterizerState = RasterizerState.CullNone)
batch.draw(target, Vector2.Zero, Color.White)
batch.end()
```

Поля:

- `glFramebuffer`
- `glDepthStencil`
- `depthStencilFormat`
- `multiSampleCount`
- `renderTargetUsage`

### Effect и BasicEffect

`Effect` компилирует и линкует GLES shaders, кэширует uniforms/attributes.

Функции:

- `linkProgram(vertexSource, fragmentSource)`
- `apply()`
- `setUniform(name, Float)`
- `setUniform(name, Float, Float)`
- `setUniform(name, Float, Float, Float)`
- `setUniform(name, Float, Float, Float, Float)`
- `setUniform(name, Int)`
- `setUniform(name, Boolean)`
- `setUniformMatrix(name, Matrix)`
- `setUniformMatrixArray(name, FloatArray)`
- `dispose()`

```kotlin
val effect = Effect()
effect.linkProgram(vertexShader, fragmentShader)
effect.apply()
effect.setUniform("uTime", gameTime.totalGameTimeSeconds)
```

`BasicEffect` уже содержит простой shader для `aPosition`, `aColor`, `aTexCoord`.

Поля:

- `world`, `view`, `projection`
- `diffuseColor`, `emissiveColor`, `specularColor`, `specularPower`, `alpha`
- `vertexColorEnabled`
- `textureEnabled`
- `lightingEnabled`
- `texture`

```kotlin
val effect = BasicEffect().apply {
    world = Matrix.Identity
    view = Matrix.createLookAt(Vector3(0f, 0f, 5f), Vector3.Zero, Vector3.Up)
    projection = Matrix.createPerspectiveFieldOfView(
        MathHelper.toRadians(60f),
        graphicsDevice.viewport.aspectRatio,
        0.1f,
        100f
    )
    vertexColorEnabled = true
}
effect.apply()
```

Важно: `enableDefaultLighting()` только выставляет `lightingEnabled = true`; shader сейчас освещение не считает.

### Buffers, vertices, primitives

`VertexBuffer(declaration, vertexCount, usage)`:

- `setData(FloatArray)`
- `setData(FloatBuffer)`
- `bind()`
- `dispose()`

`IndexBuffer(indexCount, usage)`:

- `setData(ShortArray)`
- `bind()`
- `dispose()`

`VertexDeclaration(vararg elements)`:

- `elements`
- `vertexStride`
- `setup(program)`
- `dispose()`

`VertexElement(name, elementCount, type, size)` описывает один attribute.

`VertexPositionColorTexture`:

- поля `position`, `color`, `textureCoordinate`
- `Declaration` для attributes `aPosition: vec3`, `aColor: vec4`, `aTexCoord: vec2`
- `SIZE`
- `toFloatBuffer(vertices)`

`PrimitiveType`: `TriangleList`, `TriangleStrip`, `LineList`, `LineStrip`, `PointList`.

```kotlin
val vertices = listOf(
    VertexPositionColorTexture(Vector3(-0.5f, -0.5f, 0f), Color.Red, Vector2(0f, 1f)),
    VertexPositionColorTexture(Vector3( 0.5f, -0.5f, 0f), Color.Green, Vector2(1f, 1f)),
    VertexPositionColorTexture(Vector3( 0.0f,  0.5f, 0f), Color.Blue, Vector2(0.5f, 0f)),
)

val vb = VertexBuffer(VertexPositionColorTexture.Declaration, vertices.size)
vb.setData(VertexPositionColorTexture.toFloatBuffer(vertices))

graphicsDevice.setVertexBuffer(vb)
effect.apply()
graphicsDevice.drawPrimitives(PrimitiveType.TriangleList, 0, 3)
```

Для indexed draw:

```kotlin
val ib = IndexBuffer(3)
ib.setData(shortArrayOf(0, 1, 2))
graphicsDevice.setIndexBuffer(ib)
graphicsDevice.drawIndexedPrimitives(PrimitiveType.TriangleList, 0, 0, 1)
```

### Render states

`BlendState` готовые:

- `Opaque`
- `AlphaBlend`
- `Additive`
- `NonPremultiplied`
- `Multiply`
- `Subtract`

```kotlin
BlendState.AlphaBlend.apply()
```

`DepthStencilState` готовые:

- `Default`
- `DepthRead`
- `None`

Также доступны `CompareFunction` и `StencilOperation`.

```kotlin
DepthStencilState.None.apply()
```

`RasterizerState` готовые:

- `CullNone`
- `CullClockwise`
- `CullCounterClockwise`

```kotlin
RasterizerState.CullNone.apply()
```

`SamplerState` готовые:

- `LinearClamp`
- `PointClamp`
- `LinearWrap`
- `PointWrap`
- `AnisotropicClamp`

Функция:

- `apply(unit)`

```kotlin
texture.bind()
SamplerState.PointClamp.apply(0)
```

`TextureFilter`: `Linear`, `Point`, `Anisotropic`, `LinearMipPoint`, `PointMipLinear`, `MinLinearMagPointMipLinear`, `MinLinearMagPointMipPoint`, `MinPointMagLinearMipLinear`, `MinPointMagLinearMipPoint`.

`TextureAddressMode`: `Wrap`, `Clamp`, `Mirror`.

### Viewport и PresentationParameters

`Viewport`:

- поля `x`, `y`, `width`, `height`, `minDepth`, `maxDepth`
- `aspectRatio`
- `bounds`
- `titleSafeArea`
- `project(source, projection, view, world)`
- `unproject(source, projection, view, world)`

```kotlin
val screen = graphicsDevice.viewport.project(worldPoint, projection, view, Matrix.Identity)
val world = graphicsDevice.viewport.unproject(Vector3(screen.x, screen.y, 0f), projection, view, Matrix.Identity)
```

`PresentationParameters` хранит параметры backbuffer:

- `backBufferWidth`, `backBufferHeight`, `backBufferFormat`
- `depthStencilFormat`
- `multiSampleCount`
- `presentationInterval`
- `displayOrientation`
- `autoDepthStencil`
- `isFullScreen`
- `bounds`
- `renderTargetUsage`
- `glDepthFormat`, `glStencilFormat`
- `clone()`

`DisplayMode(width, height, format, refreshRate)` используется через `graphicsDevice.displayMode`.

`SurfaceFormat` содержит набор форматов, но реально в `Texture2D` явно обработаны `Color`, `Bgr565`, `Bgra5551`, `Bgra4444`, `Alpha8`, `Single`; остальные падают в `GL_RGBA`.

## Input

Input обновляется внутри `Game.tick()` один раз за кадр. Вручную `Keyboard.update()`, `Touch.update()`, `Mouse.update()`, `GamePad.update()` вызывать не нужно.

### Keyboard

Функции:

- `getState(): KeyboardState`
- `getPreviousState(): KeyboardState`
- `isKeyPressed(key)`
- `isKeyReleased(key)`

`KeyboardState`:

- `isKeyDown(key)`
- `isKeyUp(key)`
- `getPressedKeys()`

```kotlin
override fun update(gameTime: GameTime) {
    if (Keyboard.getState().isKeyDown(Keys.Space)) {
        jumpHeld = true
    }
    if (Keyboard.isKeyPressed(Keys.Enter)) {
        startGame()
    }
}
```

`Keys` содержит Android key codes: буквы `A..Z`, цифры `D0..D9`, `Back`, `Enter`, `Space`, `Escape`, `DpadUp/Down/Left/Right`, `F1..F12`, numpad, volume, navigation и OEM-клавиши.

### Touch

Функции:

- `getState(): TouchCollection`
- `getPreviousState(): TouchCollection`
- `isAnyTouch()`

`TouchCollection`:

- наследуется от `ArrayList<TouchLocation>`
- `isAnyTouch()`
- `findById(id)`
- `clearAll()`

`TouchLocation`:

- `id`
- `position`
- `previousPosition`
- `state`: `Invalid`, `Pressed`, `Moved`, `Released`
- `pressure`
- `tryGetPreviousLocation()`

```kotlin
for (touch in Touch.getState()) {
    when (touch.state) {
        TouchLocation.TouchLocationState.Pressed -> spawnAt(touch.position)
        TouchLocation.TouchLocationState.Moved -> dragTo(touch.position)
        TouchLocation.TouchLocationState.Released -> stopDrag(touch.id)
        TouchLocation.TouchLocationState.Invalid -> Unit
    }
}
```

### Mouse

На Android mouse state синхронизирован с touch down/up для левой кнопки.

Функции:

- `getState(): MouseState`
- `setPosition(x, y)`
- `isButtonPressed(button)`

`MouseState`:

- `x`, `y`
- `position: Point`
- `leftButton`, `middleButton`, `rightButton`
- `scrollWheelValue`, `horizontalScrollWheelValue`
- `ButtonState.Pressed` / `Released`

```kotlin
val mouse = Mouse.getState()
if (mouse.leftButton == MouseState.ButtonState.Pressed) {
    aimAt(mouse.position.toVector2())
}
```

### GamePad

Функции:

- `getState(playerIndex)`
- `setVibration(playerIndex, leftMotor, rightMotor)` - включает короткую Android-вибрацию с амплитудой по максимальному мотору; `0f, 0f` отменяет вибрацию.

Для вибрации в manifest добавлен `android.permission.VIBRATE`; если на устройстве нет vibrator, метод вернёт `false`.

`GamePadState`:

- `isConnected`
- `packetNumber`
- `buttons`
- `dPad`
- `thumbSticks`
- `triggers`

```kotlin
val pad = GamePad.getState(0)
if (pad.buttons.a) {
    fire()
}
playerVelocity = pad.thumbSticks.left * 250f
```

`GamePadButtons`: `a`, `b`, `x`, `y`, `start`, `back`, `leftShoulder`, `rightShoulder`, `leftStick`, `rightStick`, `bigButton`.

`GamePadDPad`: `up`, `down`, `left`, `right`.

`GamePadThumbSticks`: `left`, `right`.

`GamePadTriggers`: `left`, `right`.

`Buttons` enum содержит XNA-like битовые значения, но текущий `GamePadState` использует data-классы, а не bitmask.

## Audio

### SoundEffect

`SoundEffect` хранит PCM buffer и параметры аудио.

Функции:

- `createInstance(): SoundEffectInstance`
- `play(volume, pitch, pan)`
- `dispose()`
- `SoundEffect.fromWav(inputStream)`
- `SoundEffect.fromOgg(inputStream)`

```kotlin
private lateinit var shot: SoundEffect

override fun loadContent() {
    shot = content.load("shot.wav")
}

fun fire() {
    shot.play(volume = 0.8f, pitch = 0f, pan = 0f)
}
```

### SoundEffectInstance

Функции:

- `play()`
- `pause()`
- `resume()`
- `stop()`
- `dispose()`

Поля:

- `volume`
- `pitch` - применяется через `AudioTrack.playbackParams`; диапазон полезных значений `-1f..1f`.
- `pan`
- `isLooped` - включает loop points для static `AudioTrack`.

```kotlin
val instance = shot.createInstance().apply {
    volume = 0.5f
    pan = -0.25f
}
instance.play()
```

### AudioEngine, AudioEmitter, AudioListener

`AudioEngine`:

- `isDisposed`
- `update()`
- `dispose()`

`AudioEmitter` и `AudioListener` хранят `position`, `forward`, `up`, `velocity`; `AudioEmitter` также `dopplerScale`. Пространственный звук сейчас не рассчитывается.

```kotlin
val listener = AudioListener(position = cameraPosition)
val emitter = AudioEmitter(position = enemyPosition)
```

## Math

### Vector2

Поля и константы:

- `x`, `y`
- `Zero`, `One`, `UnitX`, `UnitY`

Функции экземпляра:

- `length()`, `lengthSquared()`, `normalize()`, `toArray()`
- операторы `+`, `-`, `* Vector2`, `* Float`, `/ Vector2`, `/ Float`, unary `-`

Функции в классе:

- `add`, `subtract`, `multiply`, `divide`, `negate`
- `dot`, `cross`
- `distance`, `distanceSquared`
- `lerp`, `normalize`
- `reflect`
- `min`, `max`, `clamp`
- `transform`, `transformNormal`
- `angle`

Примечание: эти helper-функции объявлены как методы класса, не как `companion object`; вызывайте их через любой экземпляр или используйте операторы.

```kotlin
val v = Vector2(10f, 5f)
val moved = v + Vector2.UnitX * 100f
val dir = target - position
dir.normalize()
```

### Vector3

Константы: `Zero`, `One`, `UnitX`, `UnitY`, `UnitZ`, `Up`, `Down`, `Right`, `Left`, `Forward`, `Backward`.

Функции: `add`, `subtract`, `multiply`, `divide`, `negate`, `dot`, `cross`, `distance`, `distanceSquared`, `lerp`, `normalize`, `reflect`, `min`, `max`, `clamp`, `transform`, `transformNormal`, а также instance `length`, `lengthSquared`, `normalize` и операторы.

```kotlin
val forward = Vector3.normalize(target - camera)
val right = Vector3.cross(Vector3.Up, forward)
```

### Vector4

Константы: `Zero`, `One`.

Функции: `dot`, `lerp`, `transform`, `length`, `lengthSquared`, `normalize` и операторы `+`, `-`, `* Vector4`, `* Float`, `/ Float`, unary `-`.

```kotlin
val clip = Vector4.transform(Vector4(0f, 0f, 0f, 1f), projection)
```

### Point

`Point(x, y)` - integer координаты.

Константы: `Zero`, `One`.

Функции и операторы: `+`, `-`, `* Int`, `/ Int`, unary `-`, `toVector2()`.

```kotlin
val cell = Point(3, 4)
val pixels = cell * 32
```

### Rectangle

Поля: `x`, `y`, `width`, `height`.

Свойства: `left`, `right`, `top`, `bottom`, `center`, `location`, `isZero`, `isEmpty`.

Функции:

- `contains(x, y)`, `contains(point)`, `contains(rect)`
- `inflate(horizontalAmount, verticalAmount)`
- `intersects(other)`
- `intersect(other)`
- `union(other)`
- `offset(offsetX, offsetY)`, `offset(point)`
- `toFloatArray()`

```kotlin
val hitBox = Rectangle(100, 100, 64, 64)
if (hitBox.contains(touch.position.x.toInt(), touch.position.y.toInt())) {
    selected = true
}
```

### Color

`Color` хранит packed RGBA-like `UInt` и предоставляет компоненты:

- `r`, `g`, `b`, `a` как `Int`
- `rf`, `gf`, `bf`, `af` как `Float`
- `toVector3()`, `toVector4()`, `toArray()`
- `multiply(other)`, `*`, `+`

Конструкторы:

```kotlin
Color(255, 128, 0, 255)
Color(1f, 0.5f, 0f, 1f)
Color(Vector3(1f, 0f, 0f))
Color(Vector4(1f, 1f, 1f, 0.5f))
```

Готовые цвета включают `Transparent`, `Black`, `White`, `Red`, `Green`, `Blue`, `Yellow`, `Cyan`, `Magenta`, `CornflowerBlue` и большой набор XNA-like named colors.

### Matrix

`Matrix` - 4x4 matrix с полями `m11..m44`.

Статические функции:

- `Identity`
- `createTranslation(x, y, z)` и `createTranslation(Vector3)`
- `createScale(x, y, z)`, `createScale(Vector3)`, `createScale(Float)`
- `createRotationX/Y/Z(radians)`
- `createOrthographic(width, height, near, far)`
- `createOrthographicOffCenter(left, right, bottom, top, near, far)`
- `createPerspectiveFieldOfView(fov, aspect, near, far)`
- `createLookAt(cameraPosition, cameraTarget, cameraUpVector)`
- `createFromQuaternion(quaternion)`
- `createWorld(position, forward, up)`
- `transpose(matrix)`
- `multiply(matrix1, matrix2)`
- `invert(matrix)`

Функции экземпляра:

- `determinant()`
- `toArray()`
- `translation()`
- `scale()`
- `forward()`, `backward()`, `up()`, `down()`, `right()`, `left()`
- `decomposeScale()`
- операторы `* Matrix`, `* Vector3`

```kotlin
val world =
    Matrix.createScale(2f) *
    Matrix.createRotationZ(MathHelper.toRadians(30f)) *
    Matrix.createTranslation(100f, 50f, 0f)

val view = Matrix.createLookAt(Vector3(0f, 0f, 10f), Vector3.Zero, Vector3.Up)
val projection = Matrix.createPerspectiveFieldOfView(
    MathHelper.toRadians(60f),
    graphicsDevice.viewport.aspectRatio,
    0.1f,
    100f
)
```

### Quaternion

`Quaternion(x, y, z, w)` хранит вращение.

Статические функции:

- `Identity`
- `createFromAxisAngle(axis, angle)`
- `createFromYawPitchRoll(yaw, pitch, roll)`
- `createFromRotationMatrix(matrix)`
- `conjugate(q)`
- `inverse(q)`
- `dot(q1, q2)`
- `lerp(q1, q2, amount)`
- `slerp(q1, q2, amount)`
- `normalize(q)`

Функции экземпляра:

- `length()`, `lengthSquared()`
- `normalize()`
- `conjugate()`
- `toMatrix()`
- операторы `+`, `-`, `* Quaternion`, `* Float`, unary `-`

```kotlin
val q = Quaternion.createFromAxisAngle(Vector3.UnitY, MathHelper.toRadians(90f))
val world = q.toMatrix()
```

### MathHelper

Константы: `E`, `Log10E`, `Log2E`, `Pi`, `PiOver2`, `PiOver4`, `TwoPi`.

Функции:

- `clamp(Float/Int)`
- `distance`
- `lerp`
- `smoothStep`
- `hermite`
- `catmullRom`
- `toRadians`, `toDegrees`
- `wrapAngle`
- `max`, `min`, `sqrt`

```kotlin
val t = MathHelper.clamp(elapsed / duration, 0f, 1f)
val value = MathHelper.smoothStep(0f, 1f, t)
val angle = MathHelper.wrapAngle(currentAngle + turnSpeed * dt)
```

## BacksideGame

`BacksideGame` - текущий базовый game class приложения. Он создаёт `SpriteBatch`, системный `SpriteFont`, рисует FPS и собирает touch state.

Хуки для наследников:

```kotlin
protected open fun onTouch(position: Vector2, state: TouchLocation.TouchLocationState) {}
protected open fun onDraw(batch: SpriteBatch) {}
```

Пример наследника:

```kotlin
class DemoGame : BacksideGame() {
    private lateinit var player: Texture2D

    override fun loadContent() {
        super.loadContent()
        player = content.load("player.png")
    }

    override fun onDraw(batch: SpriteBatch) {
        batch.draw(player, Vector2(300f, 200f), Color.White)
    }

    override fun onTouch(position: Vector2, state: TouchLocation.TouchLocationState) {
        // handle touch
    }
}
```

## Практические замечания

- Все реальные исходники лежат в `app/src/main/java/`.
- Для 2D-отрисовки через `SpriteBatch` используйте `DepthStencilState.None` и `RasterizerState.CullNone`.
- Не вызывайте input `update()` вручную: `Game.tick()` уже делает это.
- `ContentManager` кэширует ассеты только в памяти.
- `SpriteBatch` поддерживает разные текстуры в одном `begin/end`, но `SpriteSortMode.Texture` обычно быстрее для большого числа спрайтов.
- Освещение в `BasicEffect` пока ограничено diffuse/alpha и texture/vertex color; полноценной light-модели нет.
- `AudioEmitter`/`AudioListener` пока только хранят данные для будущего пространственного звука.
=== ./a.md ===
=== ./app/src/main/AndroidManifest.xml ===
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <uses-feature
        android:glEsVersion="0x00030000"
        android:required="true" />

    <uses-permission android:name="android.permission.VIBRATE" />

    <application
        android:allowBackup="true"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="@xml/backup_rules"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.Backside">
        <activity
            android:name="ru.kayron.backside.MainActivity"
            android:configChanges="keyboardHidden|orientation|screenSize"
            android:exported="true"
            android:screenOrientation="userLandscape"
            tools:ignore="DiscouragedApi">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />

                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>

</manifest>
=== ./app/src/main/res/values-night/themes.xml ===
<resources>
    <style name="Theme.Backside" parent="Theme.AppCompat.Light.NoActionBar">
        <item name="android:windowFullscreen">true</item>
        <item name="android:statusBarColor">@android:color/transparent</item>
        <item name="android:navigationBarColor">@android:color/transparent</item>
        <item name="android:windowDrawsSystemBarBackgrounds">true</item>
    </style>
</resources>
=== ./app/src/main/res/values/strings.xml ===
<resources>
    <string name="app_name">Backside</string>
</resources>=== ./app/src/main/res/values/themes.xml ===
<resources>
    <style name="Theme.Backside" parent="Theme.AppCompat.Light.NoActionBar">
        <item name="android:windowFullscreen">true</item>
        <item name="android:statusBarColor">@android:color/transparent</item>
        <item name="android:navigationBarColor">@android:color/transparent</item>
        <item name="android:windowDrawsSystemBarBackgrounds">true</item>
    </style>
</resources>
=== ./app/src/main/res/drawable/ic_launcher_background.xml ===
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path
        android:fillColor="#3DDC84"
        android:pathData="M0,0h108v108h-108z" />
    <path
        android:fillColor="#00000000"
        android:pathData="M9,0L9,108"
        android:strokeWidth="0.8"
        android:strokeColor="#33FFFFFF" />
    <path
        android:fillColor="#00000000"
        android:pathData="M19,0L19,108"
        android:strokeWidth="0.8"
        android:strokeColor="#33FFFFFF" />
    <path
        android:fillColor="#00000000"
        android:pathData="M29,0L29,108"
        android:strokeWidth="0.8"
        android:strokeColor="#33FFFFFF" />
    <path
        android:fillColor="#00000000"
        android:pathData="M39,0L39,108"
        android:strokeWidth="0.8"
        android:strokeColor="#33FFFFFF" />
    <path
        android:fillColor="#00000000"
        android:pathData="M49,0L49,108"
        android:strokeWidth="0.8"
        android:strokeColor="#33FFFFFF" />
    <path
        android:fillColor="#00000000"
        android:pathData="M59,0L59,108"
        android:strokeWidth="0.8"
        android:strokeColor="#33FFFFFF" />
    <path
        android:fillColor="#00000000"
        android:pathData="M69,0L69,108"
        android:strokeWidth="0.8"
        android:strokeColor="#33FFFFFF" />
    <path
        android:fillColor="#00000000"
        android:pathData="M79,0L79,108"
        android:strokeWidth="0.8"
        android:strokeColor="#33FFFFFF" />
    <path
        android:fillColor="#00000000"
        android:pathData="M89,0L89,108"
        android:strokeWidth="0.8"
        android:strokeColor="#33FFFFFF" />
    <path
        android:fillColor="#00000000"
        android:pathData="M99,0L99,108"
        android:strokeWidth="0.8"
        android:strokeColor="#33FFFFFF" />
    <path
        android:fillColor="#00000000"
        android:pathData="M0,9L108,9"
        android:strokeWidth="0.8"
        android:strokeColor="#33FFFFFF" />
    <path
        android:fillColor="#00000000"
        android:pathData="M0,19L108,19"
        android:strokeWidth="0.8"
        android:strokeColor="#33FFFFFF" />
    <path
        android:fillColor="#00000000"
        android:pathData="M0,29L108,29"
        android:strokeWidth="0.8"
        android:strokeColor="#33FFFFFF" />
    <path
        android:fillColor="#00000000"
        android:pathData="M0,39L108,39"
        android:strokeWidth="0.8"
        android:strokeColor="#33FFFFFF" />
    <path
        android:fillColor="#00000000"
        android:pathData="M0,49L108,49"
        android:strokeWidth="0.8"
        android:strokeColor="#33FFFFFF" />
    <path
        android:fillColor="#00000000"
        android:pathData="M0,59L108,59"
        android:strokeWidth="0.8"
        android:strokeColor="#33FFFFFF" />
    <path
        android:fillColor="#00000000"
        android:pathData="M0,69L108,69"
        android:strokeWidth="0.8"
        android:strokeColor="#33FFFFFF" />
    <path
        android:fillColor="#00000000"
        android:pathData="M0,79L108,79"
        android:strokeWidth="0.8"
        android:strokeColor="#33FFFFFF" />
    <path
        android:fillColor="#00000000"
        android:pathData="M0,89L108,89"
        android:strokeWidth="0.8"
        android:strokeColor="#33FFFFFF" />
    <path
        android:fillColor="#00000000"
        android:pathData="M0,99L108,99"
        android:strokeWidth="0.8"
        android:strokeColor="#33FFFFFF" />
    <path
        android:fillColor="#00000000"
        android:pathData="M19,29L89,29"
        android:strokeWidth="0.8"
        android:strokeColor="#33FFFFFF" />
    <path
        android:fillColor="#00000000"
        android:pathData="M19,39L89,39"
        android:strokeWidth="0.8"
        android:strokeColor="#33FFFFFF" />
    <path
        android:fillColor="#00000000"
        android:pathData="M19,49L89,49"
        android:strokeWidth="0.8"
        android:strokeColor="#33FFFFFF" />
    <path
        android:fillColor="#00000000"
        android:pathData="M19,59L89,59"
        android:strokeWidth="0.8"
        android:strokeColor="#33FFFFFF" />
    <path
        android:fillColor="#00000000"
        android:pathData="M19,69L89,69"
        android:strokeWidth="0.8"
        android:strokeColor="#33FFFFFF" />
    <path
        android:fillColor="#00000000"
        android:pathData="M19,79L89,79"
        android:strokeWidth="0.8"
        android:strokeColor="#33FFFFFF" />
    <path
        android:fillColor="#00000000"
        android:pathData="M29,19L29,89"
        android:strokeWidth="0.8"
        android:strokeColor="#33FFFFFF" />
    <path
        android:fillColor="#00000000"
        android:pathData="M39,19L39,89"
        android:strokeWidth="0.8"
        android:strokeColor="#33FFFFFF" />
    <path
        android:fillColor="#00000000"
        android:pathData="M49,19L49,89"
        android:strokeWidth="0.8"
        android:strokeColor="#33FFFFFF" />
    <path
        android:fillColor="#00000000"
        android:pathData="M59,19L59,89"
        android:strokeWidth="0.8"
        android:strokeColor="#33FFFFFF" />
    <path
        android:fillColor="#00000000"
        android:pathData="M69,19L69,89"
        android:strokeWidth="0.8"
        android:strokeColor="#33FFFFFF" />
    <path
        android:fillColor="#00000000"
        android:pathData="M79,19L79,89"
        android:strokeWidth="0.8"
        android:strokeColor="#33FFFFFF" />
</vector>
=== ./app/src/main/res/drawable/ic_launcher_foreground.xml ===
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:aapt="http://schemas.android.com/aapt"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path android:pathData="M31,63.928c0,0 6.4,-11 12.1,-13.1c7.2,-2.6 26,-1.4 26,-1.4l38.1,38.1L107,108.928l-32,-1L31,63.928z">
        <aapt:attr name="android:fillColor">
            <gradient
                android:endX="85.84757"
                android:endY="92.4963"
                android:startX="42.9492"
                android:startY="49.59793"
                android:type="linear">
                <item
                    android:color="#44000000"
                    android:offset="0.0" />
                <item
                    android:color="#00000000"
                    android:offset="1.0" />
            </gradient>
        </aapt:attr>
    </path>
    <path
        android:fillColor="#FFFFFF"
        android:fillType="nonZero"
        android:pathData="M65.3,45.828l3.8,-6.6c0.2,-0.4 0.1,-0.9 -0.3,-1.1c-0.4,-0.2 -0.9,-0.1 -1.1,0.3l-3.9,6.7c-6.3,-2.8 -13.4,-2.8 -19.7,0l-3.9,-6.7c-0.2,-0.4 -0.7,-0.5 -1.1,-0.3C38.8,38.328 38.7,38.828 38.9,39.228l3.8,6.6C36.2,49.428 31.7,56.028 31,63.928h46C76.3,56.028 71.8,49.428 65.3,45.828zM43.4,57.328c-0.8,0 -1.5,-0.5 -1.8,-1.2c-0.3,-0.7 -0.1,-1.5 0.4,-2.1c0.5,-0.5 1.4,-0.7 2.1,-0.4c0.7,0.3 1.2,1 1.2,1.8C45.3,56.528 44.5,57.328 43.4,57.328L43.4,57.328zM64.6,57.328c-0.8,0 -1.5,-0.5 -1.8,-1.2s-0.1,-1.5 0.4,-2.1c0.5,-0.5 1.4,-0.7 2.1,-0.4c0.7,0.3 1.2,1 1.2,1.8C66.5,56.528 65.6,57.328 64.6,57.328L64.6,57.328z"
        android:strokeWidth="1"
        android:strokeColor="#00000000" />
</vector>=== ./app/src/main/res/xml/data_extraction_rules.xml ===
<?xml version="1.0" encoding="utf-8"?><!--
   Sample data extraction rules file; uncomment and customize as necessary.
   See https://developer.android.com/about/versions/12/backup-restore#xml-changes
   for details.
-->
<data-extraction-rules>
    <cloud-backup>
        <!-- TODO: Use <include> and <exclude> to control what is backed up.
        <include .../>
        <exclude .../>
        -->
    </cloud-backup>
    <!--
    <device-transfer>
        <include .../>
        <exclude .../>
    </device-transfer>
    -->
</data-extraction-rules>=== ./app/src/main/res/xml/backup_rules.xml ===
<?xml version="1.0" encoding="utf-8"?><!--
   Sample backup rules file; uncomment and customize as necessary.
   See https://developer.android.com/guide/topics/data/autobackup
   for details.
   Note: This file is ignored for devices older than API 31
   See https://developer.android.com/about/versions/12/backup-restore
-->
<full-backup-content>
    <!--
   <include domain="sharedpref" path="."/>
   <exclude domain="sharedpref" path="device.xml"/>
-->
</full-backup-content>=== ./app/src/main/res/mipmap-anydpi/ic_launcher.xml ===
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
    <monochrome android:drawable="@drawable/ic_launcher_foreground" />
</adaptive-icon>
=== ./app/src/main/res/mipmap-anydpi/ic_launcher_round.xml ===
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
    <monochrome android:drawable="@drawable/ic_launcher_foreground" />
</adaptive-icon>
=== ./app/src/main/java/ru/kayron/dew/math/Vector2.kt ===
package ru.kayron.dew.math

import kotlin.math.sqrt
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.abs

data class Vector2(var x: Float = 0f, var y: Float = 0f) {

    companion object {
        val Zero = Vector2(0f, 0f)
        val One = Vector2(1f, 1f)
        val UnitX = Vector2(1f, 0f)
        val UnitY = Vector2(0f, 1f)
    }

    fun length(): Float = sqrt(x * x + y * y)

    fun lengthSquared(): Float = x * x + y * y

    fun normalize() {
        val len = length()
        if (len > 0f) {
            x /= len
            y /= len
        }
    }

    fun toArray(): FloatArray = floatArrayOf(x, y)

    operator fun plus(other: Vector2) = Vector2(x + other.x, y + other.y)
    operator fun minus(other: Vector2) = Vector2(x - other.x, y - other.y)
    operator fun times(other: Vector2) = Vector2(x * other.x, y * other.y)
    operator fun times(scalar: Float) = Vector2(x * scalar, y * scalar)
    operator fun div(other: Vector2) = Vector2(x / other.x, y / other.y)
    operator fun div(scalar: Float) = Vector2(x / scalar, y / scalar)
    operator fun unaryMinus() = Vector2(-x, -y)

    // ---------- Static methods ----------

    fun add(v1: Vector2, v2: Vector2): Vector2 = Vector2(v1.x + v2.x, v1.y + v2.y)

    fun subtract(v1: Vector2, v2: Vector2): Vector2 = Vector2(v1.x - v2.x, v1.y - v2.y)

    fun multiply(v1: Vector2, v2: Vector2): Vector2 = Vector2(v1.x * v2.x, v1.y * v2.y)

    fun multiply(v: Vector2, scalar: Float): Vector2 = Vector2(v.x * scalar, v.y * scalar)

    fun divide(v1: Vector2, v2: Vector2): Vector2 = Vector2(v1.x / v2.x, v1.y / v2.y)

    fun divide(v: Vector2, scalar: Float): Vector2 = Vector2(v.x / scalar, v.y / scalar)

    fun negate(v: Vector2): Vector2 = Vector2(-v.x, -v.y)

    fun dot(v1: Vector2, v2: Vector2): Float = v1.x * v2.x + v1.y * v2.y

    fun cross(v1: Vector2, v2: Vector2): Float = v1.x * v2.y - v1.y * v2.x

    fun distance(v1: Vector2, v2: Vector2): Float = (v1 - v2).length()

    fun distanceSquared(v1: Vector2, v2: Vector2): Float = (v1 - v2).lengthSquared()

    fun lerp(v1: Vector2, v2: Vector2, amount: Float): Vector2 =
        Vector2(
            MathHelper.lerp(v1.x, v2.x, amount),
            MathHelper.lerp(v1.y, v2.y, amount)
        )

    fun normalize(v: Vector2): Vector2 {
        val len = v.length()
        return if (len > 0f) Vector2(v.x / len, v.y / len) else Vector2.Zero
    }

    fun reflect(vector: Vector2, normal: Vector2): Vector2 {
        val d = dot(vector, normal)
        return vector - (normal * (2f * d))
    }

    fun min(v1: Vector2, v2: Vector2): Vector2 =
        Vector2(minOf(v1.x, v2.x), minOf(v1.y, v2.y))

    fun max(v1: Vector2, v2: Vector2): Vector2 =
        Vector2(maxOf(v1.x, v2.x), maxOf(v1.y, v2.y))

    fun clamp(v: Vector2, min: Vector2, max: Vector2): Vector2 =
        Vector2(
            MathHelper.clamp(v.x, min.x, max.x),
            MathHelper.clamp(v.y, min.y, max.y)
        )

    fun transform(v: Vector2, matrix: Matrix): Vector2 {
        val x = v.x * matrix.m11 + v.y * matrix.m21 + matrix.m41
        val y = v.x * matrix.m12 + v.y * matrix.m22 + matrix.m42
        return Vector2(x, y)
    }

    fun transformNormal(v: Vector2, matrix: Matrix): Vector2 {
        val x = v.x * matrix.m11 + v.y * matrix.m21
        val y = v.x * matrix.m12 + v.y * matrix.m22
        return Vector2(x, y)
    }

    fun angle(v1: Vector2, v2: Vector2): Float =
        atan2(v2.y - v1.y, v2.x - v1.x)

    override fun toString(): String = "($x, $y)"
}
=== ./app/src/main/java/ru/kayron/dew/math/Vector3.kt ===
package ru.kayron.dew.math

import kotlin.math.sqrt

data class Vector3(var x: Float = 0f, var y: Float = 0f, var z: Float = 0f) {

    companion object {
        val Zero = Vector3(0f, 0f, 0f)
        val One = Vector3(1f, 1f, 1f)
        val UnitX = Vector3(1f, 0f, 0f)
        val UnitY = Vector3(0f, 1f, 0f)
        val UnitZ = Vector3(0f, 0f, 1f)
        val Up = Vector3(0f, 1f, 0f)
        val Down = Vector3(0f, -1f, 0f)
        val Right = Vector3(1f, 0f, 0f)
        val Left = Vector3(-1f, 0f, 0f)
        val Forward = Vector3(0f, 0f, -1f)
        val Backward = Vector3(0f, 0f, 1f)

        fun add(v1: Vector3, v2: Vector3): Vector3 = Vector3(v1.x + v2.x, v1.y + v2.y, v1.z + v2.z)

        fun subtract(v1: Vector3, v2: Vector3): Vector3 = Vector3(v1.x - v2.x, v1.y - v2.y, v1.z - v2.z)

        fun multiply(v1: Vector3, v2: Vector3): Vector3 = Vector3(v1.x * v2.x, v1.y * v2.y, v1.z * v2.z)

        fun multiply(v: Vector3, scalar: Float): Vector3 = Vector3(v.x * scalar, v.y * scalar, v.z * scalar)

        fun divide(v1: Vector3, v2: Vector3): Vector3 = Vector3(v1.x / v2.x, v1.y / v2.y, v1.z / v2.z)

        fun divide(v: Vector3, scalar: Float): Vector3 = Vector3(v.x / scalar, v.y / scalar, v.z / scalar)

        fun negate(v: Vector3): Vector3 = Vector3(-v.x, -v.y, -v.z)

        fun dot(v1: Vector3, v2: Vector3): Float = v1.x * v2.x + v1.y * v2.y + v1.z * v2.z

        fun cross(v1: Vector3, v2: Vector3): Vector3 = Vector3(
            v1.y * v2.z - v1.z * v2.y,
            v1.z * v2.x - v1.x * v2.z,
            v1.x * v2.y - v1.y * v2.x
        )

        fun distance(v1: Vector3, v2: Vector3): Float = (v1 - v2).length()

        fun distanceSquared(v1: Vector3, v2: Vector3): Float = (v1 - v2).lengthSquared()

        fun lerp(v1: Vector3, v2: Vector3, amount: Float): Vector3 = Vector3(
            MathHelper.lerp(v1.x, v2.x, amount),
            MathHelper.lerp(v1.y, v2.y, amount),
            MathHelper.lerp(v1.z, v2.z, amount)
        )

        fun normalize(v: Vector3): Vector3 {
            val len = v.length()
            return if (len > 0f) Vector3(v.x / len, v.y / len, v.z / len) else Vector3.Zero
        }

        fun reflect(vector: Vector3, normal: Vector3): Vector3 {
            val d = dot(vector, normal)
            return vector - (normal * (2f * d))
        }

        fun min(v1: Vector3, v2: Vector3): Vector3 = Vector3(
            minOf(v1.x, v2.x), minOf(v1.y, v2.y), minOf(v1.z, v2.z)
        )

        fun max(v1: Vector3, v2: Vector3): Vector3 = Vector3(
            maxOf(v1.x, v2.x), maxOf(v1.y, v2.y), maxOf(v1.z, v2.z)
        )

        fun clamp(v: Vector3, min: Vector3, max: Vector3): Vector3 = Vector3(
            MathHelper.clamp(v.x, min.x, max.x),
            MathHelper.clamp(v.y, min.y, max.y),
            MathHelper.clamp(v.z, min.z, max.z)
        )

        fun transform(v: Vector3, matrix: Matrix): Vector3 {
            val x = v.x * matrix.m11 + v.y * matrix.m21 + v.z * matrix.m31 + matrix.m41
            val y = v.x * matrix.m12 + v.y * matrix.m22 + v.z * matrix.m32 + matrix.m42
            val z = v.x * matrix.m13 + v.y * matrix.m23 + v.z * matrix.m33 + matrix.m43
            return Vector3(x, y, z)
        }

        fun transformNormal(v: Vector3, matrix: Matrix): Vector3 {
            val x = v.x * matrix.m11 + v.y * matrix.m21 + v.z * matrix.m31
            val y = v.x * matrix.m12 + v.y * matrix.m22 + v.z * matrix.m32
            val z = v.x * matrix.m13 + v.y * matrix.m23 + v.z * matrix.m33
            return Vector3(x, y, z)
        }
    }

    fun length(): Float = sqrt(x * x + y * y + z * z)

    fun lengthSquared(): Float = x * x + y * y + z * z

    fun normalize() {
        val len = length()
        if (len > 0f) {
            x /= len
            y /= len
            z /= len
        }
    }

    operator fun plus(other: Vector3) = Vector3(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: Vector3) = Vector3(x - other.x, y - other.y, z - other.z)
    operator fun times(other: Vector3) = Vector3(x * other.x, y * other.y, z * other.z)
    operator fun times(scalar: Float) = Vector3(x * scalar, y * scalar, z * scalar)
    operator fun div(other: Vector3) = Vector3(x / other.x, y / other.y, z / other.z)
    operator fun div(scalar: Float) = Vector3(x / scalar, y / scalar, z / scalar)
    operator fun unaryMinus() = Vector3(-x, -y, -z)

    override fun toString(): String = "($x, $y, $z)"
}
=== ./app/src/main/java/ru/kayron/dew/math/Vector4.kt ===
package ru.kayron.dew.math

import kotlin.math.sqrt

data class Vector4(var x: Float = 0f, var y: Float = 0f, var z: Float = 0f, var w: Float = 0f) {

    companion object {
        val Zero = Vector4(0f, 0f, 0f, 0f)
        val One = Vector4(1f, 1f, 1f, 1f)

        fun dot(v1: Vector4, v2: Vector4): Float =
            v1.x * v2.x + v1.y * v2.y + v1.z * v2.z + v1.w * v2.w

        fun lerp(v1: Vector4, v2: Vector4, amount: Float): Vector4 = Vector4(
            MathHelper.lerp(v1.x, v2.x, amount),
            MathHelper.lerp(v1.y, v2.y, amount),
            MathHelper.lerp(v1.z, v2.z, amount),
            MathHelper.lerp(v1.w, v2.w, amount)
        )

        fun transform(v: Vector4, matrix: Matrix): Vector4 {
            val x = v.x * matrix.m11 + v.y * matrix.m21 + v.z * matrix.m31 + v.w * matrix.m41
            val y = v.x * matrix.m12 + v.y * matrix.m22 + v.z * matrix.m32 + v.w * matrix.m42
            val z = v.x * matrix.m13 + v.y * matrix.m23 + v.z * matrix.m33 + v.w * matrix.m43
            val w = v.x * matrix.m14 + v.y * matrix.m24 + v.z * matrix.m34 + v.w * matrix.m44
            return Vector4(x, y, z, w)
        }
    }

    fun length(): Float = sqrt(x * x + y * y + z * z + w * w)

    fun lengthSquared(): Float = x * x + y * y + z * z + w * w

    fun normalize() {
        val len = length()
        if (len > 0f) {
            x /= len; y /= len; z /= len; w /= len
        }
    }

    operator fun plus(other: Vector4) = Vector4(x + other.x, y + other.y, z + other.z, w + other.w)
    operator fun minus(other: Vector4) = Vector4(x - other.x, y - other.y, z - other.z, w - other.w)
    operator fun times(other: Vector4) = Vector4(x * other.x, y * other.y, z * other.z, w * other.w)
    operator fun times(scalar: Float) = Vector4(x * scalar, y * scalar, z * scalar, w * scalar)
    operator fun div(scalar: Float) = Vector4(x / scalar, y / scalar, z / scalar, w / scalar)
    operator fun unaryMinus() = Vector4(-x, -y, -z, -w)

    override fun toString(): String = "($x, $y, $z, $w)"
}
=== ./app/src/main/java/ru/kayron/dew/math/Matrix.kt ===
package ru.kayron.dew.math

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan
import kotlin.math.sqrt

class Matrix {
    var m11: Float = 1f; var m12: Float = 0f; var m13: Float = 0f; var m14: Float = 0f
    var m21: Float = 0f; var m22: Float = 1f; var m23: Float = 0f; var m24: Float = 0f
    var m31: Float = 0f; var m32: Float = 0f; var m33: Float = 1f; var m34: Float = 0f
    var m41: Float = 0f; var m42: Float = 0f; var m43: Float = 0f; var m44: Float = 1f

    constructor()
    constructor(
        m11: Float, m12: Float, m13: Float, m14: Float,
        m21: Float, m22: Float, m23: Float, m24: Float,
        m31: Float, m32: Float, m33: Float, m34: Float,
        m41: Float, m42: Float, m43: Float, m44: Float
    ) {
        this.m11 = m11; this.m12 = m12; this.m13 = m13; this.m14 = m14
        this.m21 = m21; this.m22 = m22; this.m23 = m23; this.m24 = m24
        this.m31 = m31; this.m32 = m32; this.m33 = m33; this.m34 = m34
        this.m41 = m41; this.m42 = m42; this.m43 = m43; this.m44 = m44
    }

    companion object {
        val Identity = Matrix()

        fun createTranslation(x: Float, y: Float, z: Float): Matrix {
            val m = Matrix()
            m.m41 = x; m.m42 = y; m.m43 = z
            return m
        }

        fun createTranslation(position: Vector3): Matrix = createTranslation(position.x, position.y, position.z)

        fun createScale(x: Float, y: Float, z: Float): Matrix {
            val m = Matrix()
            m.m11 = x; m.m22 = y; m.m33 = z
            return m
        }

        fun createScale(scale: Vector3): Matrix = createScale(scale.x, scale.y, scale.z)

        fun createScale(scale: Float): Matrix = createScale(scale, scale, scale)

        fun createRotationX(radians: Float): Matrix {
            val c = cos(radians); val s = sin(radians)
            val m = Matrix()
            m.m22 = c; m.m23 = s
            m.m32 = -s; m.m33 = c
            return m
        }

        fun createRotationY(radians: Float): Matrix {
            val c = cos(radians); val s = sin(radians)
            val m = Matrix()
            m.m11 = c; m.m13 = -s
            m.m31 = s; m.m33 = c
            return m
        }

        fun createRotationZ(radians: Float): Matrix {
            val c = cos(radians); val s = sin(radians)
            val m = Matrix()
            m.m11 = c; m.m12 = s
            m.m21 = -s; m.m22 = c
            return m
        }

        fun createOrthographic(width: Float, height: Float, zNearPlane: Float, zFarPlane: Float): Matrix {
            val m = Matrix()
            m.m11 = 2f / width
            m.m22 = 2f / height
            m.m33 = 1f / (zNearPlane - zFarPlane)
            m.m41 = -1f
            m.m42 = -1f
            m.m43 = zNearPlane / (zNearPlane - zFarPlane)
            return m
        }

        fun createOrthographicOffCenter(
            left: Float, right: Float,
            bottom: Float, top: Float,
            zNearPlane: Float, zFarPlane: Float
        ): Matrix {
            val m = Matrix()
            m.m11 = 2f / (right - left)
            m.m22 = 2f / (top - bottom)
            m.m33 = 1f / (zNearPlane - zFarPlane)
            m.m41 = (left + right) / (left - right)
            m.m42 = (top + bottom) / (bottom - top)
            m.m43 = zNearPlane / (zNearPlane - zFarPlane)
            return m
        }

        fun createPerspectiveFieldOfView(fieldOfView: Float, aspectRatio: Float, nearPlane: Float, farPlane: Float): Matrix {
            val yScale = 1f / tan(fieldOfView / 2f)
            val xScale = yScale / aspectRatio
            val m = Matrix()
            m.m11 = xScale
            m.m22 = yScale
            m.m33 = farPlane / (nearPlane - farPlane)
            m.m34 = -1f
            m.m43 = nearPlane * farPlane / (nearPlane - farPlane)
            m.m44 = 0f
            return m
        }

        fun createLookAt(cameraPosition: Vector3, cameraTarget: Vector3, cameraUpVector: Vector3): Matrix {
            val zAxis = Vector3.normalize(cameraPosition - cameraTarget)
            val xAxis = Vector3.normalize(Vector3.cross(cameraUpVector, zAxis))
            val yAxis = Vector3.cross(zAxis, xAxis)
            val m = Matrix()
            m.m11 = xAxis.x; m.m12 = yAxis.x; m.m13 = zAxis.x; m.m14 = 0f
            m.m21 = xAxis.y; m.m22 = yAxis.y; m.m23 = zAxis.y; m.m24 = 0f
            m.m31 = xAxis.z; m.m32 = yAxis.z; m.m33 = zAxis.z; m.m34 = 0f
            m.m41 = -Vector3.dot(xAxis, cameraPosition)
            m.m42 = -Vector3.dot(yAxis, cameraPosition)
            m.m43 = -Vector3.dot(zAxis, cameraPosition)
            m.m44 = 1f
            return m
        }

        fun createFromQuaternion(quaternion: Quaternion): Matrix {
            val xx = quaternion.x * quaternion.x * 2f
            val yy = quaternion.y * quaternion.y * 2f
            val zz = quaternion.z * quaternion.z * 2f
            val xy = quaternion.x * quaternion.y * 2f
            val xz = quaternion.x * quaternion.z * 2f
            val yz = quaternion.y * quaternion.z * 2f
            val wx = quaternion.w * quaternion.x * 2f
            val wy = quaternion.w * quaternion.y * 2f
            val wz = quaternion.w * quaternion.z * 2f
            val m = Matrix()
            m.m11 = 1f - (yy + zz); m.m12 = xy + wz;          m.m13 = xz - wy;          m.m14 = 0f
            m.m21 = xy - wz;          m.m22 = 1f - (xx + zz); m.m23 = yz + wx;          m.m24 = 0f
            m.m31 = xz + wy;          m.m32 = yz - wx;          m.m33 = 1f - (xx + yy); m.m34 = 0f
            m.m41 = 0f;               m.m42 = 0f;               m.m43 = 0f;               m.m44 = 1f
            return m
        }

        fun createWorld(position: Vector3, forward: Vector3, up: Vector3): Matrix {
            val zAxis = Vector3.normalize(-forward)
            val xAxis = Vector3.normalize(Vector3.cross(up, zAxis))
            val yAxis = Vector3.cross(zAxis, xAxis)
            val m = Matrix()
            m.m11 = xAxis.x; m.m12 = xAxis.y; m.m13 = xAxis.z; m.m14 = 0f
            m.m21 = yAxis.x; m.m22 = yAxis.y; m.m23 = yAxis.z; m.m24 = 0f
            m.m31 = zAxis.x; m.m32 = zAxis.y; m.m33 = zAxis.z; m.m34 = 0f
            m.m41 = position.x; m.m42 = position.y; m.m43 = position.z; m.m44 = 1f
            return m
        }

        fun transpose(matrix: Matrix): Matrix {
            val m = Matrix()
            m.m11 = matrix.m11; m.m12 = matrix.m21; m.m13 = matrix.m31; m.m14 = matrix.m41
            m.m21 = matrix.m12; m.m22 = matrix.m22; m.m23 = matrix.m32; m.m24 = matrix.m42
            m.m31 = matrix.m13; m.m32 = matrix.m23; m.m33 = matrix.m33; m.m34 = matrix.m43
            m.m41 = matrix.m14; m.m42 = matrix.m24; m.m43 = matrix.m34; m.m44 = matrix.m44
            return m
        }

        fun multiply(matrix1: Matrix, matrix2: Matrix): Matrix {
            val m = Matrix()
            m.m11 = matrix1.m11 * matrix2.m11 + matrix1.m12 * matrix2.m21 + matrix1.m13 * matrix2.m31 + matrix1.m14 * matrix2.m41
            m.m12 = matrix1.m11 * matrix2.m12 + matrix1.m12 * matrix2.m22 + matrix1.m13 * matrix2.m32 + matrix1.m14 * matrix2.m42
            m.m13 = matrix1.m11 * matrix2.m13 + matrix1.m12 * matrix2.m23 + matrix1.m13 * matrix2.m33 + matrix1.m14 * matrix2.m43
            m.m14 = matrix1.m11 * matrix2.m14 + matrix1.m12 * matrix2.m24 + matrix1.m13 * matrix2.m34 + matrix1.m14 * matrix2.m44
            m.m21 = matrix1.m21 * matrix2.m11 + matrix1.m22 * matrix2.m21 + matrix1.m23 * matrix2.m31 + matrix1.m24 * matrix2.m41
            m.m22 = matrix1.m21 * matrix2.m12 + matrix1.m22 * matrix2.m22 + matrix1.m23 * matrix2.m32 + matrix1.m24 * matrix2.m42
            m.m23 = matrix1.m21 * matrix2.m13 + matrix1.m22 * matrix2.m23 + matrix1.m23 * matrix2.m33 + matrix1.m24 * matrix2.m43
            m.m24 = matrix1.m21 * matrix2.m14 + matrix1.m22 * matrix2.m24 + matrix1.m23 * matrix2.m34 + matrix1.m24 * matrix2.m44
            m.m31 = matrix1.m31 * matrix2.m11 + matrix1.m32 * matrix2.m21 + matrix1.m33 * matrix2.m31 + matrix1.m34 * matrix2.m41
            m.m32 = matrix1.m31 * matrix2.m12 + matrix1.m32 * matrix2.m22 + matrix1.m33 * matrix2.m32 + matrix1.m34 * matrix2.m42
            m.m33 = matrix1.m31 * matrix2.m13 + matrix1.m32 * matrix2.m23 + matrix1.m33 * matrix2.m33 + matrix1.m34 * matrix2.m43
            m.m34 = matrix1.m31 * matrix2.m14 + matrix1.m32 * matrix2.m24 + matrix1.m33 * matrix2.m34 + matrix1.m34 * matrix2.m44
            m.m41 = matrix1.m41 * matrix2.m11 + matrix1.m42 * matrix2.m21 + matrix1.m43 * matrix2.m31 + matrix1.m44 * matrix2.m41
            m.m42 = matrix1.m41 * matrix2.m12 + matrix1.m42 * matrix2.m22 + matrix1.m43 * matrix2.m32 + matrix1.m44 * matrix2.m42
            m.m43 = matrix1.m41 * matrix2.m13 + matrix1.m42 * matrix2.m23 + matrix1.m43 * matrix2.m33 + matrix1.m44 * matrix2.m43
            m.m44 = matrix1.m41 * matrix2.m14 + matrix1.m42 * matrix2.m24 + matrix1.m43 * matrix2.m34 + matrix1.m44 * matrix2.m44
            return m
        }

        fun invert(matrix: Matrix): Matrix {
            val det = matrix.determinant()
            if (det == 0f) return Matrix()
            val invDet = 1f / det
            val m = Matrix()
            m.m11 = (matrix.m22 * (matrix.m33 * matrix.m44 - matrix.m34 * matrix.m43) -
                     matrix.m23 * (matrix.m32 * matrix.m44 - matrix.m34 * matrix.m42) +
                     matrix.m24 * (matrix.m32 * matrix.m43 - matrix.m33 * matrix.m42)) * invDet
            m.m12 = -(matrix.m12 * (matrix.m33 * matrix.m44 - matrix.m34 * matrix.m43) -
                      matrix.m13 * (matrix.m32 * matrix.m44 - matrix.m34 * matrix.m42) +
                      matrix.m14 * (matrix.m32 * matrix.m43 - matrix.m33 * matrix.m42)) * invDet
            m.m13 = (matrix.m12 * (matrix.m23 * matrix.m44 - matrix.m24 * matrix.m43) -
                     matrix.m13 * (matrix.m22 * matrix.m44 - matrix.m24 * matrix.m42) +
                     matrix.m14 * (matrix.m22 * matrix.m43 - matrix.m23 * matrix.m42)) * invDet
            m.m14 = -(matrix.m12 * (matrix.m23 * matrix.m34 - matrix.m24 * matrix.m33) -
                      matrix.m13 * (matrix.m22 * matrix.m34 - matrix.m24 * matrix.m32) +
                      matrix.m14 * (matrix.m22 * matrix.m33 - matrix.m23 * matrix.m32)) * invDet
            m.m21 = -(matrix.m21 * (matrix.m33 * matrix.m44 - matrix.m34 * matrix.m43) -
                      matrix.m23 * (matrix.m31 * matrix.m44 - matrix.m34 * matrix.m41) +
                      matrix.m24 * (matrix.m31 * matrix.m43 - matrix.m33 * matrix.m41)) * invDet
            m.m22 = (matrix.m11 * (matrix.m33 * matrix.m44 - matrix.m34 * matrix.m43) -
                     matrix.m13 * (matrix.m31 * matrix.m44 - matrix.m34 * matrix.m41) +
                     matrix.m14 * (matrix.m31 * matrix.m43 - matrix.m33 * matrix.m41)) * invDet
            m.m23 = -(matrix.m11 * (matrix.m23 * matrix.m44 - matrix.m24 * matrix.m43) -
                      matrix.m13 * (matrix.m21 * matrix.m44 - matrix.m24 * matrix.m41) +
                      matrix.m14 * (matrix.m21 * matrix.m43 - matrix.m23 * matrix.m41)) * invDet
            m.m24 = (matrix.m11 * (matrix.m23 * matrix.m34 - matrix.m24 * matrix.m33) -
                     matrix.m13 * (matrix.m21 * matrix.m34 - matrix.m24 * matrix.m31) +
                     matrix.m14 * (matrix.m21 * matrix.m33 - matrix.m23 * matrix.m31)) * invDet
            m.m31 = (matrix.m21 * (matrix.m32 * matrix.m44 - matrix.m34 * matrix.m42) -
                     matrix.m22 * (matrix.m31 * matrix.m44 - matrix.m34 * matrix.m41) +
                     matrix.m24 * (matrix.m31 * matrix.m42 - matrix.m32 * matrix.m41)) * invDet
            m.m32 = -(matrix.m11 * (matrix.m32 * matrix.m44 - matrix.m34 * matrix.m42) -
                      matrix.m12 * (matrix.m31 * matrix.m44 - matrix.m34 * matrix.m41) +
                      matrix.m14 * (matrix.m31 * matrix.m42 - matrix.m32 * matrix.m41)) * invDet
            m.m33 = (matrix.m11 * (matrix.m22 * matrix.m44 - matrix.m24 * matrix.m42) -
                     matrix.m12 * (matrix.m21 * matrix.m44 - matrix.m24 * matrix.m41) +
                     matrix.m14 * (matrix.m21 * matrix.m42 - matrix.m22 * matrix.m41)) * invDet
            m.m34 = -(matrix.m11 * (matrix.m22 * matrix.m34 - matrix.m24 * matrix.m32) -
                      matrix.m12 * (matrix.m21 * matrix.m34 - matrix.m24 * matrix.m31) +
                      matrix.m14 * (matrix.m21 * matrix.m32 - matrix.m22 * matrix.m31)) * invDet
            m.m41 = -(matrix.m21 * (matrix.m32 * matrix.m43 - matrix.m33 * matrix.m42) -
                      matrix.m22 * (matrix.m31 * matrix.m43 - matrix.m33 * matrix.m41) +
                      matrix.m23 * (matrix.m31 * matrix.m42 - matrix.m32 * matrix.m41)) * invDet
            m.m42 = (matrix.m11 * (matrix.m32 * matrix.m43 - matrix.m33 * matrix.m42) -
                     matrix.m12 * (matrix.m31 * matrix.m43 - matrix.m33 * matrix.m41) +
                     matrix.m13 * (matrix.m31 * matrix.m42 - matrix.m32 * matrix.m41)) * invDet
            m.m43 = -(matrix.m11 * (matrix.m22 * matrix.m43 - matrix.m23 * matrix.m42) -
                      matrix.m12 * (matrix.m21 * matrix.m43 - matrix.m23 * matrix.m41) +
                      matrix.m13 * (matrix.m21 * matrix.m42 - matrix.m22 * matrix.m41)) * invDet
            m.m44 = (matrix.m11 * (matrix.m22 * matrix.m33 - matrix.m23 * matrix.m32) -
                     matrix.m12 * (matrix.m21 * matrix.m33 - matrix.m23 * matrix.m31) +
                     matrix.m13 * (matrix.m21 * matrix.m32 - matrix.m22 * matrix.m31)) * invDet
            return m
        }
    }

    fun determinant(): Float {
        return m11 * (m22 * (m33 * m44 - m34 * m43) - m23 * (m32 * m44 - m34 * m42) + m24 * (m32 * m43 - m33 * m42))
             - m12 * (m21 * (m33 * m44 - m34 * m43) - m23 * (m31 * m44 - m34 * m41) + m24 * (m31 * m43 - m33 * m41))
             + m13 * (m21 * (m32 * m44 - m34 * m42) - m22 * (m31 * m44 - m34 * m41) + m24 * (m31 * m42 - m32 * m41))
             - m14 * (m21 * (m32 * m43 - m33 * m42) - m22 * (m31 * m43 - m33 * m41) + m23 * (m31 * m42 - m32 * m41))
    }

    fun toArray(): FloatArray = floatArrayOf(
        m11, m12, m13, m14,
        m21, m22, m23, m24,
        m31, m32, m33, m34,
        m41, m42, m43, m44
    )

    fun translation(): Vector3 = Vector3(m41, m42, m43)

    fun scale(): Vector3 {
        val sx = Vector3(m11, m12, m13).length()
        val sy = Vector3(m21, m22, m23).length()
        val sz = Vector3(m31, m32, m33).length()
        return Vector3(sx, sy, sz)
    }

    fun forward(): Vector3 = Vector3(-m31, -m32, -m33)
    fun backward(): Vector3 = Vector3(m31, m32, m33)
    fun up(): Vector3 = Vector3(m21, m22, m23)
    fun down(): Vector3 = Vector3(-m21, -m22, -m23)
    fun right(): Vector3 = Vector3(m11, m12, m13)
    fun left(): Vector3 = Vector3(-m11, -m12, -m13)

    fun decomposeScale(): Vector3 {
        val sx = Vector3(m11, m12, m13).length()
        val sy = Vector3(m21, m22, m23).length()
        val sz = Vector3(m31, m32, m33).length()
        return Vector3(sx, sy, sz)
    }

    operator fun times(other: Matrix): Matrix = multiply(this, other)

    operator fun times(vector: Vector3): Vector3 = Vector3.transform(vector, this)

    override fun toString(): String {
        return "[$m11,$m12,$m13,$m14]\n[$m21,$m22,$m23,$m24]\n[$m31,$m32,$m33,$m34]\n[$m41,$m42,$m43,$m44]"
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Matrix) return false
        return m11 == other.m11 && m12 == other.m12 && m13 == other.m13 && m14 == other.m14 &&
               m21 == other.m21 && m22 == other.m22 && m23 == other.m23 && m24 == other.m24 &&
               m31 == other.m31 && m32 == other.m32 && m33 == other.m33 && m34 == other.m34 &&
               m41 == other.m41 && m42 == other.m42 && m43 == other.m43 && m44 == other.m44
    }

    override fun hashCode(): Int {
        var hash = m11.hashCode()
        hash = 31 * hash + m22.hashCode()
        hash = 31 * hash + m33.hashCode()
        hash = 31 * hash + m44.hashCode()
        return hash
    }
}
=== ./app/src/main/java/ru/kayron/dew/math/Quaternion.kt ===
package ru.kayron.dew.math

import kotlin.math.sqrt
import kotlin.math.acos

data class Quaternion(var x: Float = 0f, var y: Float = 0f, var z: Float = 0f, var w: Float = 1f) {

    companion object {
        val Identity = Quaternion(0f, 0f, 0f, 1f)

        fun createFromAxisAngle(axis: Vector3, angle: Float): Quaternion {
            val half = angle * 0.5f
            val s = kotlin.math.sin(half.toDouble()).toFloat()
            return Quaternion(axis.x * s, axis.y * s, axis.z * s, kotlin.math.cos(half.toDouble()).toFloat())
        }

        fun createFromYawPitchRoll(yaw: Float, pitch: Float, roll: Float): Quaternion {
            val halfYaw = yaw * 0.5f
            val halfPitch = pitch * 0.5f
            val halfRoll = roll * 0.5f
            val cosYaw = kotlin.math.cos(halfYaw.toDouble()).toFloat()
            val sinYaw = kotlin.math.sin(halfYaw.toDouble()).toFloat()
            val cosPitch = kotlin.math.cos(halfPitch.toDouble()).toFloat()
            val sinPitch = kotlin.math.sin(halfPitch.toDouble()).toFloat()
            val cosRoll = kotlin.math.cos(halfRoll.toDouble()).toFloat()
            val sinRoll = kotlin.math.sin(halfRoll.toDouble()).toFloat()
            return Quaternion(
                cosRoll * sinPitch * cosYaw + sinRoll * cosPitch * sinYaw,
                cosRoll * cosPitch * sinYaw - sinRoll * sinPitch * cosYaw,
                sinRoll * cosPitch * cosYaw - cosRoll * sinPitch * sinYaw,
                cosRoll * cosPitch * cosYaw + sinRoll * sinPitch * sinYaw
            )
        }

        fun createFromRotationMatrix(matrix: Matrix): Quaternion {
            val trace = matrix.m11 + matrix.m22 + matrix.m33
            if (trace > 0f) {
                var s = sqrt((trace + 1f).toDouble()).toFloat() * 2f
                val w = 0.25f * s
                val x = (matrix.m32 - matrix.m23) / s
                val y = (matrix.m13 - matrix.m31) / s
                val z = (matrix.m21 - matrix.m12) / s
                return Quaternion(x, y, z, w)
            } else if (matrix.m11 > matrix.m22 && matrix.m11 > matrix.m33) {
                var s = sqrt((1f + matrix.m11 - matrix.m22 - matrix.m33).toDouble()).toFloat() * 2f
                val w = (matrix.m32 - matrix.m23) / s
                val x = 0.25f * s
                val y = (matrix.m12 + matrix.m21) / s
                val z = (matrix.m13 + matrix.m31) / s
                return Quaternion(x, y, z, w)
            } else if (matrix.m22 > matrix.m33) {
                var s = sqrt((1f + matrix.m22 - matrix.m11 - matrix.m33).toDouble()).toFloat() * 2f
                val w = (matrix.m13 - matrix.m31) / s
                val x = (matrix.m12 + matrix.m21) / s
                val y = 0.25f * s
                val z = (matrix.m23 + matrix.m32) / s
                return Quaternion(x, y, z, w)
            } else {
                var s = sqrt((1f + matrix.m33 - matrix.m11 - matrix.m22).toDouble()).toFloat() * 2f
                val w = (matrix.m21 - matrix.m12) / s
                val x = (matrix.m13 + matrix.m31) / s
                val y = (matrix.m23 + matrix.m32) / s
                val z = 0.25f * s
                return Quaternion(x, y, z, w)
            }
        }

        fun conjugate(q: Quaternion): Quaternion = Quaternion(-q.x, -q.y, -q.z, q.w)

        fun inverse(q: Quaternion): Quaternion {
            val lenSq = q.lengthSquared()
            if (lenSq == 0f) return Quaternion.Identity
            val inv = 1f / lenSq
            return Quaternion(-q.x * inv, -q.y * inv, -q.z * inv, q.w * inv)
        }

        fun dot(q1: Quaternion, q2: Quaternion): Float =
            q1.x * q2.x + q1.y * q2.y + q1.z * q2.z + q1.w * q2.w

        fun lerp(q1: Quaternion, q2: Quaternion, amount: Float): Quaternion {
            val t = MathHelper.clamp(amount, 0f, 1f)
            return Quaternion(
                MathHelper.lerp(q1.x, q2.x, t),
                MathHelper.lerp(q1.y, q2.y, t),
                MathHelper.lerp(q1.z, q2.z, t),
                MathHelper.lerp(q1.w, q2.w, t)
            ).let { it.normalize(); it }
        }

        fun slerp(q1: Quaternion, q2: Quaternion, amount: Float): Quaternion {
            var cosOmega = Quaternion.dot(q1, q2)
            var q2Copy = q2
            if (cosOmega < 0f) {
                cosOmega = -cosOmega
                q2Copy = -q2
            }
            val k0: Float
            val k1: Float
            if (cosOmega > 0.9999f) {
                k0 = 1f - amount
                k1 = amount
            } else {
                val sinOmega = sqrt((1f - cosOmega * cosOmega).toDouble()).toFloat()
                val omega = acos(cosOmega.toDouble()).toFloat()
                val invSinOmega = 1f / sinOmega
                k0 = kotlin.math.sin(((1f - amount) * omega).toDouble()).toFloat() * invSinOmega
                k1 = kotlin.math.sin((amount * omega).toDouble()).toFloat() * invSinOmega
            }
            return Quaternion(
                q1.x * k0 + q2Copy.x * k1,
                q1.y * k0 + q2Copy.y * k1,
                q1.z * k0 + q2Copy.z * k1,
                q1.w * k0 + q2Copy.w * k1
            )
        }

        fun normalize(q: Quaternion): Quaternion {
            val len = q.length()
            return if (len > 0f) Quaternion(q.x / len, q.y / len, q.z / len, q.w / len) else Quaternion.Identity
        }
    }

    fun length(): Float = sqrt((x * x + y * y + z * z + w * w).toDouble()).toFloat()

    fun lengthSquared(): Float = x * x + y * y + z * z + w * w

    fun normalize() {
        val len = length()
        if (len > 0f) {
            x /= len; y /= len; z /= len; w /= len
        }
    }

    fun conjugate() {
        x = -x; y = -y; z = -z
    }

    fun toMatrix(): Matrix = Matrix.createFromQuaternion(this)

    operator fun plus(other: Quaternion) = Quaternion(x + other.x, y + other.y, z + other.z, w + other.w)
    operator fun minus(other: Quaternion) = Quaternion(x - other.x, y - other.y, z - other.z, w - other.w)
    operator fun times(other: Quaternion): Quaternion {
        return Quaternion(
            w * other.x + x * other.w + y * other.z - z * other.y,
            w * other.y - x * other.z + y * other.w + z * other.x,
            w * other.z + x * other.y - y * other.x + z * other.w,
            w * other.w - x * other.x - y * other.y - z * other.z
        )
    }
    operator fun times(scalar: Float) = Quaternion(x * scalar, y * scalar, z * scalar, w * scalar)
    operator fun unaryMinus() = Quaternion(-x, -y, -z, -w)

    override fun toString(): String = "($x, $y, $z, $w)"
}
=== ./app/src/main/java/ru/kayron/dew/math/MathHelper.kt ===
package ru.kayron.dew.math

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.math.PI

object MathHelper {
    const val E = 2.7182817f
    const val Log10E = 0.4342945f
    const val Log2E = 1.442695f
    const val Pi = 3.1415927f
    const val PiOver2 = 1.5707964f
    const val PiOver4 = 0.7853982f
    const val TwoPi = 6.2831855f

    fun clamp(value: Float, min: Float, max: Float): Float =
        when {
            value < min -> min
            value > max -> max
            else -> value
        }

    fun clamp(value: Int, min: Int, max: Int): Int =
        when {
            value < min -> min
            value > max -> max
            else -> value
        }

    fun distance(value1: Float, value2: Float): Float = abs(value1 - value2)

    fun lerp(value1: Float, value2: Float, amount: Float): Float =
        value1 + (value2 - value1) * amount

    fun smoothStep(value1: Float, value2: Float, amount: Float): Float {
        val t = clamp(amount, 0f, 1f)
        val t2 = t * t * (3f - 2f * t)
        return value1 + (value2 - value1) * t2
    }

    fun hermite(value1: Float, tangent1: Float, value2: Float, tangent2: Float, amount: Float): Float {
        val t = amount
        val t2 = t * t
        val t3 = t2 * t
        val a = 2f * t3 - 3f * t2 + 1f
        val b = t3 - 2f * t2 + t
        val c = -2f * t3 + 3f * t2
        val d = t3 - t2
        return value1 * a + tangent1 * b + value2 * c + tangent2 * d
    }

    fun catmullRom(value1: Float, value2: Float, value3: Float, value4: Float, amount: Float): Float {
        val t = amount
        val t2 = t * t
        val t3 = t2 * t
        return 0.5f * (
            (2f * value2) +
            (-value1 + value3) * t +
            (2f * value1 - 5f * value2 + 4f * value3 - value4) * t2 +
            (-value1 + 3f * value2 - 3f * value3 + value4) * t3
        )
    }

    fun toRadians(degrees: Float): Float = degrees * (Pi / 180f)

    fun toDegrees(radians: Float): Float = radians * (180f / Pi)

    fun wrapAngle(angle: Float): Float {
        var a = angle % TwoPi
        if (a > Pi) a -= TwoPi
        if (a < -Pi) a += TwoPi
        return a
    }

    fun max(a: Float, b: Float): Float = max(a, b)
    fun min(a: Float, b: Float): Float = min(a, b)
    fun sqrt(value: Float): Float = sqrt(value)
}
=== ./app/src/main/java/ru/kayron/dew/math/Color.kt ===
package ru.kayron.dew.math

import kotlin.math.roundToInt

data class Color(val packedValue: UInt = 0xFFFFFFFFu) {

    /**
     * Формат:
     * RRGGBBAA
     */
    constructor(r: Int, g: Int, b: Int, a: Int = 255) : this(
        ((r.toUInt() and 0xFFu) shl 24) or
        ((g.toUInt() and 0xFFu) shl 16) or
        ((b.toUInt() and 0xFFu) shl 8) or
        (a.toUInt() and 0xFFu)
    )

    constructor(r: Float, g: Float, b: Float, a: Float = 1f) : this(
        (r * 255f).roundToInt().coerceIn(0, 255),
        (g * 255f).roundToInt().coerceIn(0, 255),
        (b * 255f).roundToInt().coerceIn(0, 255),
        (a * 255f).roundToInt().coerceIn(0, 255)
    )

    constructor(vector3: Vector3) : this(vector3.x, vector3.y, vector3.z)
    constructor(vector4: Vector4) : this(vector4.x, vector4.y, vector4.z, vector4.w)

    val r: Int get() = ((packedValue shr 24) and 0xFFu).toInt()
    val g: Int get() = ((packedValue shr 16) and 0xFFu).toInt()
    val b: Int get() = ((packedValue shr 8) and 0xFFu).toInt()
    val a: Int get() = (packedValue and 0xFFu).toInt()

    val rf: Float get() = r / 255f
    val gf: Float get() = g / 255f
    val bf: Float get() = b / 255f
    val af: Float get() = a / 255f

    fun toVector3(): Vector3 = Vector3(rf, gf, bf)
    fun toVector4(): Vector4 = Vector4(rf, gf, bf, af)

    fun toArray(): FloatArray =
        floatArrayOf(rf, gf, bf, af)

    fun multiply(other: Color): Color = Color(
        (r * other.r / 255f).roundToInt(),
        (g * other.g / 255f).roundToInt(),
        (b * other.b / 255f).roundToInt(),
        (a * other.a / 255f).roundToInt()
    )

    operator fun times(other: Color) = multiply(other)

    operator fun plus(other: Color): Color = Color(
        (r + other.r).coerceAtMost(255),
        (g + other.g).coerceAtMost(255),
        (b + other.b).coerceAtMost(255),
        (a + other.a).coerceAtMost(255)
    )

    override fun toString(): String =
        "Color(r=$r, g=$g, b=$b, a=$a)"

    companion object {

        val Transparent = Color(0x00000000u)

        val Black = Color(0x000000FFu)
        val White = Color(0xFFFFFFFFu)

        val Red = Color(0xFF0000FFu)
        val Green = Color(0x00FF00FFu)
        val Blue = Color(0x0000FFFFu)

        val Yellow = Color(0xFFFF00FFu)
        val Cyan = Color(0x00FFFFFFu)
        val Magenta = Color(0xFF00FFFFu)

        val Orange = Color(0xFFA500FFu)
        val Purple = Color(0x800080FFu)
        val Gray = Color(0x808080FFu)

        val DarkGray = Color(0x444444FFu)
        val LightGray = Color(0xC0C0C0FFu)

        val Pink = Color(0xFFC0CBFFu)
        val CornflowerBlue = Color(0x6495EDFFu)

        val DarkBlue = Color(0x00008BFFu)
        val DarkGreen = Color(0x006400FFu)
        val DarkRed = Color(0x8B0000FFu)

        val DarkCyan = Color(0x008B8BFFu)
        val DarkMagenta = Color(0x8B008BFFu)
        val DarkOrange = Color(0xFF8C00FFu)

        val DarkSalmon = Color(0xE9967AFFu)
        val DarkSeaGreen = Color(0x8FBC8FFFu)
        val DarkSlateBlue = Color(0x483D8BFFu)

        val DarkSlateGray = Color(0x2F4F4FFFu)
        val DarkTurquoise = Color(0x00CED1FFu)
        val DarkViolet = Color(0x9400D3FFu)

        val DeepPink = Color(0xFF1493FFu)
        val DeepSkyBlue = Color(0x00BFFFFFu)
        val DimGray = Color(0x696969FFu)

        val DodgerBlue = Color(0x1E90FFFFu)
        val Firebrick = Color(0xB22222FFu)
        val FloralWhite = Color(0xFFFAF0FFu)

        val ForestGreen = Color(0x228B22FFu)
        val Fuchsia = Color(0xFF00FFFFu)
        val Gainsboro = Color(0xDCDCDCFFu)

        val GhostWhite = Color(0xF8F8FFFFu)
        val Gold = Color(0xFFD700FFu)
        val Goldenrod = Color(0xDAA520FFu)

        val GreenYellow = Color(0xADFF2FFFu)
        val Honeydew = Color(0xF0FFF0FFu)
        val HotPink = Color(0xFF69B4FFu)

        val IndianRed = Color(0xCD5C5CFFu)
        val Indigo = Color(0x4B0082FFu)
        val Ivory = Color(0xFFFFF0FFu)

        val Khaki = Color(0xF0E68CFFu)
        val Lavender = Color(0xE6E6FAFFu)
        val LavenderBlush = Color(0xFFF0F5FFu)

        val LawnGreen = Color(0x7CFC00FFu)
        val LemonChiffon = Color(0xFFFACDFFu)
        val LightBlue = Color(0xADD8E6FFu)

        val LightCoral = Color(0xF08080FFu)
        val LightCyan = Color(0xE0FFFFFFu)
        val LightGoldenrodYellow = Color(0xFAFAD2FFu)

        val LightGreen = Color(0x90EE90FFu)
        val LightPink = Color(0xFFB6C1FFu)
        val LightSalmon = Color(0xFFA07AFFu)

        val LightSeaGreen = Color(0x20B2AAFFu)
        val LightSkyBlue = Color(0x87CEFAFFu)
        val LightSlateGray = Color(0x778899FFu)

        val LightSteelBlue = Color(0xB0C4DEFFu)
        val LightYellow = Color(0xFFFFE0FFu)

        val Lime = Color(0x00FF00FFu)
        val LimeGreen = Color(0x32CD32FFu)

        val Linen = Color(0xFAF0E6FFu)
        val Maroon = Color(0x800000FFu)

        val MediumAquamarine = Color(0x66CDAAFFu)
        val MediumBlue = Color(0x0000CDFFu)
        val MediumOrchid = Color(0xBA55D3FFu)

        val MediumPurple = Color(0x9370DBFFu)
        val MediumSeaGreen = Color(0x3CB371FFu)
        val MediumSlateBlue = Color(0x7B68EEFFu)

        val MediumSpringGreen = Color(0x00FA9AFFu)
        val MediumTurquoise = Color(0x48D1CCFFu)
        val MediumVioletRed = Color(0xC71585FFu)

        val MidnightBlue = Color(0x191970FFu)
        val MintCream = Color(0xF5FFFAFFu)
        val MistyRose = Color(0xFFE4E1FFu)

        val Moccasin = Color(0xFFE4B5FFu)
        val NavajoWhite = Color(0xFFDEADFFu)
        val Navy = Color(0x000080FFu)

        val OldLace = Color(0xFDF5E6FFu)
        val Olive = Color(0x808000FFu)
        val OliveDrab = Color(0x6B8E23FFu)

        val OrangeRed = Color(0xFF4500FFu)
        val Orchid = Color(0xDA70D6FFu)
        val PaleGoldenrod = Color(0xEEE8AAFFu)

        val PaleGreen = Color(0x98FB98FFu)
        val PaleTurquoise = Color(0xAFEEEEFFu)
        val PaleVioletRed = Color(0xDB7093FFu)

        val PapayaWhip = Color(0xFFEFD5FFu)
        val PeachPuff = Color(0xFFDAB9FFu)
        val Peru = Color(0xCD853FFFu)

        val Plum = Color(0xDDA0DDFFu)
        val PowderBlue = Color(0xB0E0E6FFu)
        val RosyBrown = Color(0xBC8F8FFFu)

        val RoyalBlue = Color(0x4169E1FFu)
        val SaddleBrown = Color(0x8B4513FFu)
        val Salmon = Color(0xFA8072FFu)

        val SandyBrown = Color(0xF4A460FFu)
        val SeaGreen = Color(0x2E8B57FFu)
        val SeaShell = Color(0xFFF5EEFFu)

        val Sienna = Color(0xA0522DFFu)
        val Silver = Color(0xC0C0C0FFu)
        val SkyBlue = Color(0x87CEEBFFu)

        val SlateBlue = Color(0x6A5ACDFFu)
        val SlateGray = Color(0x708090FFu)
        val Snow = Color(0xFFFAFAFFu)

        val SpringGreen = Color(0x00FF7FFFu)
        val SteelBlue = Color(0x4682B4FFu)
        val Tan = Color(0xD2B48CFFu)

        val Teal = Color(0x008080FFu)
        val Thistle = Color(0xD8BFD8FFu)
        val Tomato = Color(0xFF6347FFu)

        val Turquoise = Color(0x40E0D0FFu)
        val Violet = Color(0xEE82EEFFu)
        val Wheat = Color(0xF5DEB3FFu)

        val WhiteSmoke = Color(0xF5F5F5FFu)
        val YellowGreen = Color(0x9ACD32FFu)

        fun fromHex(hex: UInt): Color =
            Color(hex)

        fun fromRGB(hex: UInt): Color =
            Color((hex shl 8) or 0xFFu)
    }
}=== ./app/src/main/java/ru/kayron/dew/math/Rectangle.kt ===
package ru.kayron.dew.math

data class Rectangle(
    var x: Int = 0,
    var y: Int = 0,
    var width: Int = 0,
    var height: Int = 0
) {
    val left: Int get() = x
    val right: Int get() = x + width
    val top: Int get() = y
    val bottom: Int get() = y + height

    val center: Point get() = Point(x + width / 2, y + height / 2)

    val location: Point get() = Point(x, y)

    val isZero: Boolean get() = x == 0 && y == 0 && width == 0 && height == 0

    val isEmpty: Boolean get() = width == 0 && height == 0

    fun contains(x: Int, y: Int): Boolean =
        x >= left && x < right && y >= top && y < bottom

    fun contains(point: Point): Boolean = contains(point.x, point.y)

    fun contains(rect: Rectangle): Boolean =
        rect.left >= left && rect.right <= right && rect.top >= top && rect.bottom <= bottom

    fun inflate(horizontalAmount: Int, verticalAmount: Int) {
        x -= horizontalAmount
        y -= verticalAmount
        width += horizontalAmount * 2
        height += verticalAmount * 2
    }

    fun intersects(other: Rectangle): Boolean =
        left < other.right && right > other.left && top < other.bottom && bottom > other.top

    fun intersect(other: Rectangle): Rectangle {
        if (!intersects(other)) return Rectangle()
        return Rectangle(
            maxOf(left, other.left),
            maxOf(top, other.top),
            minOf(right, other.right) - maxOf(left, other.left),
            minOf(bottom, other.bottom) - maxOf(top, other.top)
        )
    }

    fun union(other: Rectangle): Rectangle {
        val minX = minOf(left, other.left)
        val minY = minOf(top, other.top)
        val maxX = maxOf(right, other.right)
        val maxY = maxOf(bottom, other.bottom)
        return Rectangle(minX, minY, maxX - minX, maxY - minY)
    }

    fun offset(offsetX: Int, offsetY: Int) {
        x += offsetX
        y += offsetY
    }

    fun offset(amount: Point) {
        x += amount.x
        y += amount.y
    }

    fun toFloatArray(): FloatArray = floatArrayOf(
        x.toFloat(), y.toFloat(),
        width.toFloat(), height.toFloat()
    )

    override fun toString(): String = "Rectangle($x, $y, $width, $height)"
}
=== ./app/src/main/java/ru/kayron/dew/math/Point.kt ===
package ru.kayron.dew.math

data class Point(var x: Int = 0, var y: Int = 0) {

    companion object {
        val Zero = Point(0, 0)
        val One = Point(1, 1)
    }

    operator fun plus(other: Point) = Point(x + other.x, y + other.y)
    operator fun minus(other: Point) = Point(x - other.x, y - other.y)
    operator fun times(scalar: Int) = Point(x * scalar, y * scalar)
    operator fun div(scalar: Int) = Point(x / scalar, y / scalar)
    operator fun unaryMinus() = Point(-x, -y)

    fun toVector2(): Vector2 = Vector2(x.toFloat(), y.toFloat())

    override fun toString(): String = "($x, $y)"
}
=== ./app/src/main/java/ru/kayron/dew/graphics/SurfaceFormat.kt ===
package ru.kayron.dew.graphics

enum class SurfaceFormat {
    Color,
    Bgr565,
    Bgra5551,
    Bgra4444,
    Dxt1,
    Dxt3,
    Dxt5,
    NormalizedByte2,
    NormalizedByte4,
    Rgba1010102,
    Rg32,
    Rgba64,
    Alpha8,
    Single,
    Vector2,
    Vector4,
    HalfSingle,
    HalfVector2,
    HalfVector4,
    HdrBlendable,
    Etc1,
    Etc2,
    Pvrtc2,
    Pvrtc4,
    S3tcDxt1,
    S3tcDxt3,
    S3tcDxt5,
}
=== ./app/src/main/java/ru/kayron/dew/graphics/PresentationParameters.kt ===
package ru.kayron.dew.graphics

import android.opengl.GLES30
import ru.kayron.dew.math.Rectangle

class PresentationParameters {
    var backBufferWidth: Int = 0
    var backBufferHeight: Int = 0
    var backBufferFormat: SurfaceFormat = SurfaceFormat.Color
    var depthStencilFormat: DepthFormat = DepthFormat.Depth24Stencil8
    var multiSampleCount: Int = 0
    var presentationInterval: PresentInterval = PresentInterval.Default
    var displayOrientation: DisplayOrientation = DisplayOrientation.Default
    var autoDepthStencil: Boolean = true
    var isFullScreen: Boolean = true
    var bounds: Rectangle = Rectangle()
    var renderTargetUsage: RenderTargetUsage = RenderTargetUsage.DiscardContents

    enum class DepthFormat {
        None,
        Depth16,
        Depth24,
        Depth24Stencil8,
    }

    enum class PresentInterval {
        Default,
        One,
        Two,
        Immediate,
    }

    enum class DisplayOrientation(val value: Int) {
        Default(0),
        LandscapeLeft(1),
        LandscapeRight(2),
        Portrait(3),
        PortraitDown(4),
    }

    enum class RenderTargetUsage {
        DiscardContents,
        PreserveContents,
        PlatformContents,
    }

    val glDepthFormat: Int
        get() = when (depthStencilFormat) {
            DepthFormat.Depth16 -> GLES30.GL_DEPTH_COMPONENT16
            DepthFormat.Depth24 -> GLES30.GL_DEPTH_COMPONENT24
            DepthFormat.Depth24Stencil8 -> GLES30.GL_DEPTH24_STENCIL8
            DepthFormat.None -> GLES30.GL_NONE
        }

    val glStencilFormat: Int
        get() = when (depthStencilFormat) {
            DepthFormat.Depth24Stencil8 -> GLES30.GL_STENCIL_INDEX8
            else -> GLES30.GL_NONE
        }

    fun clone(): PresentationParameters {
        val pp = PresentationParameters()
        pp.backBufferWidth = backBufferWidth
        pp.backBufferHeight = backBufferHeight
        pp.backBufferFormat = backBufferFormat
        pp.depthStencilFormat = depthStencilFormat
        pp.multiSampleCount = multiSampleCount
        pp.presentationInterval = presentationInterval
        pp.displayOrientation = displayOrientation
        pp.autoDepthStencil = autoDepthStencil
        pp.isFullScreen = isFullScreen
        pp.bounds = bounds
        pp.renderTargetUsage = renderTargetUsage
        return pp
    }
}
=== ./app/src/main/java/ru/kayron/dew/graphics/Viewport.kt ===
package ru.kayron.dew.graphics

import ru.kayron.dew.math.Matrix
import ru.kayron.dew.math.Rectangle
import ru.kayron.dew.math.Vector3

data class Viewport(
    var x: Int = 0,
    var y: Int = 0,
    var width: Int = 0,
    var height: Int = 0,
    var minDepth: Float = 0f,
    var maxDepth: Float = 1f
) {
    val aspectRatio: Float get() = if (height != 0) width.toFloat() / height.toFloat() else 0f

    val bounds: Rectangle get() = Rectangle(x, y, width, height)

    val titleSafeArea: Rectangle get() = Rectangle(x, y, width, height)

    fun project(source: Vector3, projection: Matrix, view: Matrix, world: Matrix): Vector3 {
        val matrix = world * view * projection
        var vector = Vector3.transform(source, matrix)
        val w = source.x * matrix.m14 + source.y * matrix.m24 + source.z * matrix.m34 + matrix.m44
        if (w != 0f) {
            vector /= w
        }
        vector.x = (vector.x + 1f) * 0.5f * width.toFloat() + x.toFloat()
        vector.y = (-vector.y + 1f) * 0.5f * height.toFloat() + y.toFloat()
        vector.z = vector.z * (maxDepth - minDepth) + minDepth
        return vector
    }

    fun unproject(source: Vector3, projection: Matrix, view: Matrix, world: Matrix): Vector3 {
        val matrix = Matrix.invert(world * view * projection)
        var vector = source
        vector.x = ((vector.x - x.toFloat()) / width.toFloat()) * 2f - 1f
        vector.y = -(((vector.y - y.toFloat()) / height.toFloat()) * 2f - 1f)
        vector.z = (vector.z - minDepth) / (maxDepth - minDepth)
        vector = Vector3.transform(vector, matrix)
        val w = source.x * matrix.m14 + source.y * matrix.m24 + source.z * matrix.m34 + matrix.m44
        if (w != 0f) {
            vector /= w
        }
        return vector
    }

    override fun toString(): String = "Viewport($x, $y, $width, $height)"
}
=== ./app/src/main/java/ru/kayron/dew/graphics/SpriteEffects.kt ===
package ru.kayron.dew.graphics

enum class SpriteEffects(val value: Int) {
    None(0),
    FlipHorizontally(1),
    FlipVertically(2),
}
=== ./app/src/main/java/ru/kayron/dew/graphics/ClearOptions.kt ===
package ru.kayron.dew.graphics

enum class ClearOptions(val value: Int) {
    Target(1),
    DepthBuffer(2),
    Stencil(4),
}
=== ./app/src/main/java/ru/kayron/dew/graphics/SpriteSortMode.kt ===
package ru.kayron.dew.graphics

enum class SpriteSortMode {
    Deferred,
    Immediate,
    Texture,
    BackToFront,
    FrontToBack,
}
=== ./app/src/main/java/ru/kayron/dew/graphics/DisplayMode.kt ===
package ru.kayron.dew.graphics

data class DisplayMode(
    val width: Int,
    val height: Int,
    val format: SurfaceFormat = SurfaceFormat.Color,
    val refreshRate: Int = 60
)
=== ./app/src/main/java/ru/kayron/dew/graphics/PrimitiveType.kt ===
package ru.kayron.dew.graphics

enum class PrimitiveType(val glType: Int) {
    TriangleList(android.opengl.GLES30.GL_TRIANGLES),
    TriangleStrip(android.opengl.GLES30.GL_TRIANGLE_STRIP),
    LineList(android.opengl.GLES30.GL_LINES),
    LineStrip(android.opengl.GLES30.GL_LINE_STRIP),
    PointList(android.opengl.GLES30.GL_POINTS),
}
=== ./app/src/main/java/ru/kayron/dew/graphics/BlendState.kt ===
package ru.kayron.dew.graphics

import android.opengl.GLES30

class BlendState private constructor(
    val name: String,
    val colorSourceBlend: Blend,
    val colorDestinationBlend: Blend,
    val colorBlendFunction: BlendFunction,
    val alphaSourceBlend: Blend,
    val alphaDestinationBlend: Blend,
    val alphaBlendFunction: BlendFunction,
    val colorWriteChannels: ColorWriteChannels = ColorWriteChannels.All
) {
    companion object {
        val Opaque = BlendState("Opaque",
            Blend.One, Blend.Zero, BlendFunction.Add,
            Blend.One, Blend.Zero, BlendFunction.Add
        )
        val AlphaBlend = BlendState("AlphaBlend",
            Blend.SourceAlpha, Blend.InverseSourceAlpha, BlendFunction.Add,
            Blend.One, Blend.InverseSourceAlpha, BlendFunction.Add
        )
        val Additive = BlendState("Additive",
            Blend.SourceAlpha, Blend.One, BlendFunction.Add,
            Blend.One, Blend.One, BlendFunction.Add
        )
        val NonPremultiplied = BlendState("NonPremultiplied",
            Blend.SourceAlpha, Blend.InverseSourceAlpha, BlendFunction.Add,
            Blend.SourceAlpha, Blend.InverseSourceAlpha, BlendFunction.Add
        )
        val Multiply = BlendState("Multiply",
            Blend.DestinationColor, Blend.Zero, BlendFunction.Add,
            Blend.DestinationAlpha, Blend.Zero, BlendFunction.Add
        )
        val Subtract = BlendState("Subtract",
            Blend.SourceAlpha, Blend.One, BlendFunction.ReverseSubtract,
            Blend.One, Blend.One, BlendFunction.ReverseSubtract
        )
    }

    enum class Blend(val glValue: Int) {
        Zero(GLES30.GL_ZERO),
        One(GLES30.GL_ONE),
        SourceColor(GLES30.GL_SRC_COLOR),
        InverseSourceColor(GLES30.GL_ONE_MINUS_SRC_COLOR),
        SourceAlpha(GLES30.GL_SRC_ALPHA),
        InverseSourceAlpha(GLES30.GL_ONE_MINUS_SRC_ALPHA),
        DestinationColor(GLES30.GL_DST_COLOR),
        InverseDestinationColor(GLES30.GL_ONE_MINUS_DST_COLOR),
        DestinationAlpha(GLES30.GL_DST_ALPHA),
        InverseDestinationAlpha(GLES30.GL_ONE_MINUS_DST_ALPHA),
        BlendFactor(GLES30.GL_CONSTANT_COLOR),
        InverseBlendFactor(GLES30.GL_ONE_MINUS_CONSTANT_COLOR),
        SourceAlphaSaturation(GLES30.GL_SRC_ALPHA_SATURATE),
    }

    enum class BlendFunction(val glValue: Int) {
        Add(GLES30.GL_FUNC_ADD),
        Subtract(GLES30.GL_FUNC_SUBTRACT),
        ReverseSubtract(GLES30.GL_FUNC_REVERSE_SUBTRACT),
        Min(GLES30.GL_MIN),
        Max(GLES30.GL_MAX),
    }

    enum class ColorWriteChannels(val value: Int) {
        None(0),
        Red(1),
        Green(2),
        Blue(4),
        Alpha(8),
        All(15),
    }

    fun apply() {
        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFuncSeparate(
            colorSourceBlend.glValue, colorDestinationBlend.glValue,
            alphaSourceBlend.glValue, alphaDestinationBlend.glValue
        )
        GLES30.glBlendEquationSeparate(colorBlendFunction.glValue, alphaBlendFunction.glValue)
        GLES30.glColorMask(
            colorWriteChannels.value and 1 != 0,
            colorWriteChannels.value and 2 != 0,
            colorWriteChannels.value and 4 != 0,
            colorWriteChannels.value and 8 != 0
        )
    }
}
=== ./app/src/main/java/ru/kayron/dew/graphics/RasterizerState.kt ===
package ru.kayron.dew.graphics

import android.opengl.GLES30

data class RasterizerState(
    var cullMode: CullMode = CullMode.CullCounterClockwiseFace,
    var fillMode: FillMode = FillMode.Solid,
    var depthBias: Float = 0f,
    var slopeScaleDepthBias: Float = 0f,
    var scissorTestEnable: Boolean = false,
    var multiSampleAntiAlias: Boolean = true,
) {
    companion object {
        val CullNone = RasterizerState(cullMode = CullMode.None)
        val CullClockwise = RasterizerState(cullMode = CullMode.CullClockwiseFace)
        val CullCounterClockwise = RasterizerState(cullMode = CullMode.CullCounterClockwiseFace)
    }

    enum class CullMode(val glValue: Int) {
        None(GLES30.GL_NONE),
        CullClockwiseFace(GLES30.GL_FRONT),
        CullCounterClockwiseFace(GLES30.GL_BACK),
    }

    enum class FillMode(val glValue: Int) {
        Solid(0),
    }

    fun apply() {
        if (cullMode == CullMode.None) {
            GLES30.glDisable(GLES30.GL_CULL_FACE)
        } else {
            GLES30.glEnable(GLES30.GL_CULL_FACE)
            GLES30.glCullFace(cullMode.glValue)
        }
        if (scissorTestEnable) {
            GLES30.glEnable(GLES30.GL_SCISSOR_TEST)
        } else {
            GLES30.glDisable(GLES30.GL_SCISSOR_TEST)
        }
        GLES30.glPolygonOffset(depthBias, slopeScaleDepthBias)
    }
}
=== ./app/src/main/java/ru/kayron/dew/graphics/DepthStencilState.kt ===
package ru.kayron.dew.graphics

import android.opengl.GLES30

data class DepthStencilState(
    var depthBufferEnable: Boolean = true,
    var depthBufferWriteEnable: Boolean = true,
    var depthBufferFunction: CompareFunction = CompareFunction.LessEqual,
    var stencilEnable: Boolean = false,
    var stencilFunction: CompareFunction = CompareFunction.Always,
    var stencilPass: StencilOperation = StencilOperation.Keep,
    var stencilFail: StencilOperation = StencilOperation.Keep,
    var stencilDepthBufferFail: StencilOperation = StencilOperation.Keep,
    var twoSidedStencilMode: Boolean = false,
    var referenceStencil: Int = 0,
    var stencilMask: Int = 0xFF,
    var stencilWriteMask: Int = 0xFF,
) {
    companion object {
        val Default = DepthStencilState()
        val DepthRead = DepthStencilState(
            depthBufferWriteEnable = false
        )
        val None = DepthStencilState(
            depthBufferEnable = false,
            depthBufferWriteEnable = false
        )
    }

    enum class CompareFunction(val glValue: Int) {
        Always(GLES30.GL_ALWAYS),
        Never(GLES30.GL_NEVER),
        Less(GLES30.GL_LESS),
        LessEqual(GLES30.GL_LEQUAL),
        Equal(GLES30.GL_EQUAL),
        GreaterEqual(GLES30.GL_GEQUAL),
        Greater(GLES30.GL_GREATER),
        NotEqual(GLES30.GL_NOTEQUAL),
    }

    enum class StencilOperation(val glValue: Int) {
        Keep(GLES30.GL_KEEP),
        Zero(GLES30.GL_ZERO),
        Replace(GLES30.GL_REPLACE),
        Increment(GLES30.GL_INCR),
        Decrement(GLES30.GL_DECR),
        IncrementSaturation(GLES30.GL_INCR_WRAP),
        DecrementSaturation(GLES30.GL_DECR_WRAP),
        Invert(GLES30.GL_INVERT),
    }

    fun apply() {
        if (depthBufferEnable) {
            GLES30.glEnable(GLES30.GL_DEPTH_TEST)
            GLES30.glDepthMask(depthBufferWriteEnable)
            GLES30.glDepthFunc(depthBufferFunction.glValue)
        } else {
            GLES30.glDisable(GLES30.GL_DEPTH_TEST)
        }
        if (stencilEnable) {
            GLES30.glEnable(GLES30.GL_STENCIL_TEST)
            GLES30.glStencilFunc(stencilFunction.glValue, referenceStencil, stencilMask)
            GLES30.glStencilOp(
                stencilFail.glValue,
                stencilDepthBufferFail.glValue,
                stencilPass.glValue
            )
            GLES30.glStencilMask(stencilWriteMask)
        } else {
            GLES30.glDisable(GLES30.GL_STENCIL_TEST)
        }
    }
}
=== ./app/src/main/java/ru/kayron/dew/graphics/SamplerState.kt ===
package ru.kayron.dew.graphics

import android.opengl.GLES30

class SamplerState(
    var filter: TextureFilter = TextureFilter.Linear,
    var addressU: TextureAddressMode = TextureAddressMode.Clamp,
    var addressV: TextureAddressMode = TextureAddressMode.Clamp,
    var addressW: TextureAddressMode = TextureAddressMode.Clamp,
    var maxAnisotropy: Int = 4,
    var maxMipLevel: Int = 0,
    var mipMapLevelOfDetailBias: Float = 0f,
) {
    companion object {
        val LinearClamp = SamplerState()
        val PointClamp = SamplerState(filter = TextureFilter.Point)
        val LinearWrap = SamplerState(addressU = TextureAddressMode.Wrap, addressV = TextureAddressMode.Wrap)
        val PointWrap = SamplerState(filter = TextureFilter.Point, addressU = TextureAddressMode.Wrap, addressV = TextureAddressMode.Wrap)
        val AnisotropicClamp = SamplerState(filter = TextureFilter.Anisotropic)
    }

    enum class TextureFilter {
        Linear,
        Point,
        Anisotropic,
        LinearMipPoint,
        PointMipLinear,
        MinLinearMagPointMipLinear,
        MinLinearMagPointMipPoint,
        MinPointMagLinearMipLinear,
        MinPointMagLinearMipPoint,
    }

    enum class TextureAddressMode(val glValue: Int) {
        Wrap(GLES30.GL_REPEAT),
        Clamp(GLES30.GL_CLAMP_TO_EDGE),
        Mirror(GLES30.GL_MIRRORED_REPEAT),
    }

    fun apply(unit: Int) {
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0 + unit)
        val glFilter = when (filter) {
            TextureFilter.Linear -> GLES30.GL_LINEAR
            TextureFilter.Point -> GLES30.GL_NEAREST
            TextureFilter.Anisotropic -> GLES30.GL_LINEAR_MIPMAP_LINEAR
            else -> GLES30.GL_LINEAR
        }
        val glMinFilter = when (filter) {
            TextureFilter.Anisotropic -> GLES30.GL_LINEAR_MIPMAP_LINEAR
            TextureFilter.LinearMipPoint -> GLES30.GL_LINEAR_MIPMAP_NEAREST
            TextureFilter.PointMipLinear -> GLES30.GL_NEAREST_MIPMAP_LINEAR
            else -> glFilter
        }
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, glMinFilter)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, glFilter)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, addressU.glValue)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, addressV.glValue)
    }
}
=== ./app/src/main/java/ru/kayron/dew/graphics/Texture.kt ===
package ru.kayron.dew.graphics

abstract class Texture {
    var glTexture: Int = 0
        protected set
    var levelCount: Int = 1
        protected set
    var format: SurfaceFormat = SurfaceFormat.Color
        protected set

    abstract fun bind()
    abstract fun dispose()

    protected fun generateTexture(): Int {
        val textures = IntArray(1)
        android.opengl.GLES30.glGenTextures(1, textures, 0)
        return textures[0]
    }

    protected fun deleteTexture() {
        if (glTexture != 0) {
            android.opengl.GLES30.glDeleteTextures(1, intArrayOf(glTexture), 0)
            glTexture = 0
        }
    }
}
=== ./app/src/main/java/ru/kayron/dew/graphics/Texture2D.kt ===
package ru.kayron.dew.graphics

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.opengl.GLES30
import android.opengl.GLUtils
import java.nio.ByteBuffer
import java.nio.ByteOrder

open class Texture2D(
    val width: Int,
    val height: Int,
    mipMap: Boolean = false,
    format: SurfaceFormat = SurfaceFormat.Color
) : Texture() {
    var mipMap: Boolean = mipMap
        private set

    init {
        this.format = format
        this.glTexture = generateTexture()
        bind()
        val internalFormat = when (format) {
            SurfaceFormat.Color -> GLES30.GL_RGBA
            SurfaceFormat.Bgr565 -> GLES30.GL_RGB
            SurfaceFormat.Bgra5551 -> GLES30.GL_RGB5_A1
            SurfaceFormat.Bgra4444 -> GLES30.GL_RGBA4
            SurfaceFormat.Alpha8 -> GLES30.GL_ALPHA
            SurfaceFormat.Single -> GLES30.GL_LUMINANCE
            else -> GLES30.GL_RGBA
        }
        val border = 0
        GLES30.glTexImage2D(
            GLES30.GL_TEXTURE_2D, 0, internalFormat,
            width, height, border, internalFormat,
            GLES30.GL_UNSIGNED_BYTE, null
        )
        if (mipMap) {
            GLES30.glGenerateMipmap(GLES30.GL_TEXTURE_2D)
            levelCount = (32 - Integer.numberOfLeadingZeros(maxOf(width, height))).coerceAtLeast(1)
        }
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
    }

    fun setData(color: IntArray) {
        require(color.size >= width * height) {
            "Expected at least ${width * height} pixels, got ${color.size}"
        }
        bind()
        val buffer = ByteBuffer.allocateDirect(color.size * 4)
            .order(ByteOrder.nativeOrder())
        buffer.asIntBuffer().put(color)
        buffer.position(0)
        GLES30.glTexSubImage2D(
            GLES30.GL_TEXTURE_2D, 0, 0, 0, width, height,
            GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, buffer
        )
        if (mipMap) {
            GLES30.glGenerateMipmap(GLES30.GL_TEXTURE_2D)
        }
    }

    fun setData(color: IntArray, startX: Int, startY: Int, width: Int, height: Int) {
        require(color.size >= width * height) {
            "Expected at least ${width * height} pixels, got ${color.size}"
        }
        bind()
        val buffer = ByteBuffer.allocateDirect(color.size * 4)
            .order(ByteOrder.nativeOrder())
        buffer.asIntBuffer().put(color)
        buffer.position(0)
        GLES30.glTexSubImage2D(
            GLES30.GL_TEXTURE_2D, 0, startX, startY, width, height,
            GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, buffer
        )
    }

    fun setData(bitmap: Bitmap) {
        bind()
        GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmap, 0)
        if (mipMap) {
            GLES30.glGenerateMipmap(GLES30.GL_TEXTURE_2D)
        }
    }

    fun getData(): IntArray {
        val pixels = IntArray(width * height)
        val buffer = ByteBuffer.allocateDirect(width * height * 4)
            .order(ByteOrder.nativeOrder())
        GLES30.glReadPixels(0, 0, width, height, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, buffer)
        buffer.asIntBuffer().get(pixels)
        return pixels
    }

    override fun bind() {
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, glTexture)
    }

    override fun dispose() {
        deleteTexture()
    }

    companion object {
        private var assetManager: AssetManager? = null

        internal fun setAssetManager(am: AssetManager) {
            assetManager = am
        }

        fun fromBitmap(bitmap: Bitmap, mipMap: Boolean = false, linear: Boolean = true): Texture2D {
            val tex = Texture2D(bitmap.width, bitmap.height, mipMap)
            tex.setData(bitmap)
            tex.bind()
            val filter = if (linear) GLES30.GL_LINEAR else GLES30.GL_NEAREST
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, filter)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, filter)
            return tex
        }

        fun fromAsset(path: String, mipMap: Boolean = false): Texture2D? {
            val am = assetManager ?: return null
            val candidates = if (path.startsWith("Content/")) {
                listOf(path)
            } else {
                listOf(path, "Content/$path")
            }
            for (candidate in candidates) {
                try {
                    am.open(candidate).use { stream ->
                        val bitmap = BitmapFactory.decodeStream(stream) ?: return null
                        return fromBitmap(bitmap, mipMap)
                    }
                } catch (_: Exception) {
                }
            }
            return null
        }
    }
}
=== ./app/src/main/java/ru/kayron/dew/graphics/RenderTarget2D.kt ===
package ru.kayron.dew.graphics

import android.opengl.GLES30

class RenderTarget2D(
    width: Int,
    height: Int,
    mipMap: Boolean = false,
    format: SurfaceFormat = SurfaceFormat.Color,
    val depthStencilFormat: PresentationParameters.DepthFormat = PresentationParameters.DepthFormat.Depth24Stencil8,
    val multiSampleCount: Int = 0,
    val renderTargetUsage: PresentationParameters.RenderTargetUsage = PresentationParameters.RenderTargetUsage.DiscardContents
) : Texture2D(width, height, mipMap, format) {

    var glFramebuffer: Int = 0
        private set
    var glDepthStencil: Int = 0
        private set

    init {
        createFramebuffer()
    }

    private fun createFramebuffer() {
        val fbos = IntArray(1)
        GLES30.glGenFramebuffers(1, fbos, 0)
        glFramebuffer = fbos[0]
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, glFramebuffer)
        GLES30.glFramebufferTexture2D(
            GLES30.GL_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0,
            GLES30.GL_TEXTURE_2D, glTexture, 0
        )
        if (depthStencilFormat != PresentationParameters.DepthFormat.None) {
            val rbos = IntArray(1)
            GLES30.glGenRenderbuffers(1, rbos, 0)
            glDepthStencil = rbos[0]
            GLES30.glBindRenderbuffer(GLES30.GL_RENDERBUFFER, glDepthStencil)
            val depthStencilGlFormat = when (depthStencilFormat) {
                PresentationParameters.DepthFormat.Depth16 -> GLES30.GL_DEPTH_COMPONENT16
                PresentationParameters.DepthFormat.Depth24 -> GLES30.GL_DEPTH_COMPONENT24
                PresentationParameters.DepthFormat.Depth24Stencil8 -> GLES30.GL_DEPTH24_STENCIL8
                PresentationParameters.DepthFormat.None -> GLES30.GL_NONE
            }
            GLES30.glRenderbufferStorage(
                GLES30.GL_RENDERBUFFER,
                depthStencilGlFormat,
                width, height
            )
            val attachment = when (depthStencilFormat) {
                PresentationParameters.DepthFormat.Depth24Stencil8 -> GLES30.GL_DEPTH_STENCIL_ATTACHMENT
                else -> GLES30.GL_DEPTH_ATTACHMENT
            }
            GLES30.glFramebufferRenderbuffer(
                GLES30.GL_FRAMEBUFFER, attachment,
                GLES30.GL_RENDERBUFFER, glDepthStencil
            )
        }
        val status = GLES30.glCheckFramebufferStatus(GLES30.GL_FRAMEBUFFER)
        if (status != GLES30.GL_FRAMEBUFFER_COMPLETE) {
            android.util.Log.e("RenderTarget2D", "Framebuffer not complete: $status")
        }
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
    }

    override fun dispose() {
        if (glFramebuffer != 0) {
            GLES30.glDeleteFramebuffers(1, intArrayOf(glFramebuffer), 0)
            glFramebuffer = 0
        }
        if (glDepthStencil != 0) {
            GLES30.glDeleteRenderbuffers(1, intArrayOf(glDepthStencil), 0)
            glDepthStencil = 0
        }
        super.dispose()
    }
}
=== ./app/src/main/java/ru/kayron/dew/graphics/VertexDeclaration.kt ===
package ru.kayron.dew.graphics

import android.opengl.GLES30

class VertexDeclaration(vararg elements: VertexElement) {
    val elements: List<VertexElement> = elements.toList()
    val vertexStride: Int = elements.sumOf { it.size }
    var glVao: Int = 0

    fun setup(program: Int) {
        if (glVao == 0) {
            val vaos = IntArray(1)
            GLES30.glGenVertexArrays(1, vaos, 0)
            glVao = vaos[0]
        }
        GLES30.glBindVertexArray(glVao)
        var offset = 0
        for (element in elements) {
            val location = GLES30.glGetAttribLocation(program, element.name)
            if (location >= 0) {
                GLES30.glEnableVertexAttribArray(location)
                GLES30.glVertexAttribPointer(
                    location, element.elementCount,
                    element.type, false, vertexStride, offset
                )
            }
            offset += element.size
        }
    }

    fun dispose() {
        if (glVao != 0) {
            GLES30.glDeleteVertexArrays(1, intArrayOf(glVao), 0)
            glVao = 0
        }
    }
}

data class VertexElement(
    val name: String,
    val elementCount: Int,
    val type: Int = GLES30.GL_FLOAT,
    val size: Int = elementCount * when (type) {
        GLES30.GL_FLOAT -> 4
        GLES30.GL_UNSIGNED_BYTE -> 1
        GLES30.GL_UNSIGNED_SHORT -> 2
        else -> 4
    }
)
=== ./app/src/main/java/ru/kayron/dew/graphics/VertexPositionColorTexture.kt ===
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
=== ./app/src/main/java/ru/kayron/dew/graphics/VertexBuffer.kt ===
package ru.kayron.dew.graphics

import android.opengl.GLES30
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class VertexBuffer(
    val declaration: VertexDeclaration,
    val vertexCount: Int,
    val usage: BufferUsage = BufferUsage.None
) {
    var glBuffer: Int = 0
        private set
    var isDynamic: Boolean = usage == BufferUsage.WriteOnly

    enum class BufferUsage {
        None,
        WriteOnly,
    }

    init {
        val bufs = IntArray(1)
        GLES30.glGenBuffers(1, bufs, 0)
        glBuffer = bufs[0]
    }

    fun setData(vertices: FloatArray) {
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, glBuffer)
        val buffer = ByteBuffer.allocateDirect(vertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        buffer.put(vertices)
        buffer.position(0)
        val usageGl = if (isDynamic) GLES30.GL_DYNAMIC_DRAW else GLES30.GL_STATIC_DRAW
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, vertices.size * 4, buffer, usageGl)
    }

    fun setData(buffer: FloatBuffer) {
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, glBuffer)
        buffer.position(0)
        val usageGl = if (isDynamic) GLES30.GL_DYNAMIC_DRAW else GLES30.GL_STATIC_DRAW
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, buffer.capacity() * 4, buffer, usageGl)
    }

    fun bind() {
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, glBuffer)
    }

    fun dispose() {
        if (glBuffer != 0) {
            GLES30.glDeleteBuffers(1, intArrayOf(glBuffer), 0)
            glBuffer = 0
        }
    }
}
=== ./app/src/main/java/ru/kayron/dew/graphics/IndexBuffer.kt ===
package ru.kayron.dew.graphics

import android.opengl.GLES30
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.ShortBuffer

class IndexBuffer(
    val indexCount: Int,
    val usage: VertexBuffer.BufferUsage = VertexBuffer.BufferUsage.None
) {
    var glBuffer: Int = 0
        private set
    var isDynamic: Boolean = usage == VertexBuffer.BufferUsage.WriteOnly

    init {
        val bufs = IntArray(1)
        GLES30.glGenBuffers(1, bufs, 0)
        glBuffer = bufs[0]
    }

    fun setData(indices: ShortArray) {
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, glBuffer)
        val buffer = ByteBuffer.allocateDirect(indices.size * 2)
            .order(ByteOrder.nativeOrder())
            .asShortBuffer()
        buffer.put(indices)
        buffer.position(0)
        val usageGl = if (isDynamic) GLES30.GL_DYNAMIC_DRAW else GLES30.GL_STATIC_DRAW
        GLES30.glBufferData(GLES30.GL_ELEMENT_ARRAY_BUFFER, indices.size * 2, buffer, usageGl)
    }

    fun bind() {
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, glBuffer)
    }

    fun dispose() {
        if (glBuffer != 0) {
            GLES30.glDeleteBuffers(1, intArrayOf(glBuffer), 0)
            glBuffer = 0
        }
    }
}
=== ./app/src/main/java/ru/kayron/dew/graphics/Effect.kt ===
package ru.kayron.dew.graphics

import android.opengl.GLES30
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

open class Effect {
    var program: Int = 0
        protected set
    val uniforms = mutableMapOf<String, Int>()
    protected val attributes = mutableMapOf<String, Int>()

    protected fun compileShader(type: Int, source: String): Int {
        val shader = GLES30.glCreateShader(type)
        if (shader == 0) {
            android.util.Log.e("Effect", "Failed to create shader")
            return 0
        }
        GLES30.glShaderSource(shader, source)
        GLES30.glCompileShader(shader)
        val status = IntArray(1)
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, status, 0)
        if (status[0] == 0) {
            val log = GLES30.glGetShaderInfoLog(shader)
            android.util.Log.e("Effect", "Shader compile error: $log")
            GLES30.glDeleteShader(shader)
            return 0
        }
        return shader
    }

    fun linkProgram(vertexSource: String, fragmentSource: String) {
        val vertexShader = compileShader(GLES30.GL_VERTEX_SHADER, vertexSource)
        val fragmentShader = compileShader(GLES30.GL_FRAGMENT_SHADER, fragmentSource)
        if (vertexShader == 0 || fragmentShader == 0) {
            program = 0
            return
        }
        program = GLES30.glCreateProgram()
        GLES30.glAttachShader(program, vertexShader)
        GLES30.glAttachShader(program, fragmentShader)
        GLES30.glLinkProgram(program)
        val status = IntArray(1)
        GLES30.glGetProgramiv(program, GLES30.GL_LINK_STATUS, status, 0)
        if (status[0] == 0) {
            val log = GLES30.glGetProgramInfoLog(program)
            android.util.Log.e("Effect", "Program link error: $log")
            GLES30.glDeleteProgram(program)
            program = 0
        }
        GLES30.glDeleteShader(vertexShader)
        GLES30.glDeleteShader(fragmentShader)
        if (program != 0) {
            cacheUniforms()
            cacheAttributes()
        }
    }

    private fun cacheUniforms() {
        val count = IntArray(1)
        GLES30.glGetProgramiv(program, GLES30.GL_ACTIVE_UNIFORMS, count, 0)
        for (i in 0 until count[0]) {
            val length = IntArray(1)
            val size = IntArray(1)
            val type = IntArray(1)
            val nameBytes = ByteArray(128)
            GLES30.glGetActiveUniform(program, i, 128, length, 0, size, 0, type, 0, nameBytes, 0)
            val name = if (length[0] > 0) String(nameBytes, 0, length[0]) else null
            if (name != null) {
                uniforms[name] = GLES30.glGetUniformLocation(program, name)
            }
        }
    }

    private fun cacheAttributes() {
        val count = IntArray(1)
        GLES30.glGetProgramiv(program, GLES30.GL_ACTIVE_ATTRIBUTES, count, 0)
        for (i in 0 until count[0]) {
            val length = IntArray(1)
            val size = IntArray(1)
            val type = IntArray(1)
            val nameBytes = ByteArray(128)
            GLES30.glGetActiveAttrib(program, i, 128, length, 0, size, 0, type, 0, nameBytes, 0)
            val name = if (length[0] > 0) String(nameBytes, 0, length[0]) else null
            if (name != null) {
                attributes[name] = GLES30.glGetAttribLocation(program, name)
            }
        }
    }

    open fun apply() {
        if (program == 0) {
            android.util.Log.w("Effect", "apply() called with program=0")
            return
        }
        GLES30.glUseProgram(program)
    }

    fun setUniform(name: String, value: Float) {
        uniforms[name]?.let { GLES30.glUniform1f(it, value) }
    }

    fun setUniform(name: String, v0: Float, v1: Float) {
        uniforms[name]?.let { GLES30.glUniform2f(it, v0, v1) }
    }

    fun setUniform(name: String, v0: Float, v1: Float, v2: Float) {
        uniforms[name]?.let { GLES30.glUniform3f(it, v0, v1, v2) }
    }

    fun setUniform(name: String, v0: Float, v1: Float, v2: Float, v3: Float) {
        uniforms[name]?.let { GLES30.glUniform4f(it, v0, v1, v2, v3) }
    }

    fun setUniform(name: String, value: Int) {
        uniforms[name]?.let { GLES30.glUniform1i(it, value) }
    }

    fun setUniform(name: String, value: Boolean) {
        uniforms[name]?.let { GLES30.glUniform1i(it, if (value) 1 else 0) }
    }

    fun setUniformMatrix(name: String, matrix: ru.kayron.dew.math.Matrix) {
        uniforms[name]?.let { loc ->
            val buffer = ByteBuffer.allocateDirect(16 * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
            buffer.put(matrix.toArray())
            buffer.position(0)
            GLES30.glUniformMatrix4fv(loc, 1, false, buffer)
        }
    }

    fun setUniformMatrixArray(name: String, matrices: FloatArray) {
        uniforms[name]?.let { loc ->
            val buffer = ByteBuffer.allocateDirect(matrices.size * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
            buffer.put(matrices)
            buffer.position(0)
            GLES30.glUniformMatrix4fv(loc, matrices.size / 16, false, buffer)
        }
    }

    fun dispose() {
        if (program != 0) {
            GLES30.glDeleteProgram(program)
            program = 0
        }
        uniforms.clear()
        attributes.clear()
    }
}
=== ./app/src/main/java/ru/kayron/dew/graphics/BasicEffect.kt ===
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
=== ./app/src/main/java/ru/kayron/dew/graphics/SpriteBatch.kt ===
package ru.kayron.dew.graphics

import android.opengl.GLES30
import ru.kayron.dew.math.*
import java.nio.ByteBuffer
import java.nio.ByteOrder

class SpriteBatch(private val graphicsDevice: GraphicsDevice) {

    private inner class SpriteInfo(
        val texture: Texture2D,
        val sourceRect: Rectangle?,
        val destinationRect: Rectangle,
        val color: Color,
        val rotation: Float,
        val origin: Vector2,
        val effects: SpriteEffects,
        val depth: Float
    )

    private val sprites = mutableListOf<SpriteInfo>()

    private var sortMode: SpriteSortMode = SpriteSortMode.Deferred
    private var effect: Effect? = null
    private var transformMatrix: Matrix? = null

    private var isStarted = false

    private val maxBatchSize = 2048
    private var currentBatch = 0

    private val vertexData = FloatArray(maxBatchSize * 6 * 9)

    private val vertexBuffer = ByteBuffer.allocateDirect(vertexData.size * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()

    private val vbo = IntArray(1)

    private val spriteEffect: Effect

    init {

        val vertexSrc = """
            #version 300 es

            in vec3 aPosition;
            in vec2 aTexCoord;
            in vec4 aColor;

            out vec2 vTexCoord;
            out vec4 vColor;

            uniform mat4 uProjection;

            void main() {
                gl_Position = uProjection * vec4(aPosition, 1.0);
                vTexCoord = aTexCoord;
                vColor = aColor;
            }
        """.trimIndent()

        val fragmentSrc = """
            #version 300 es

            precision mediump float;

            in vec2 vTexCoord;
            in vec4 vColor;

            out vec4 fragColor;

            uniform sampler2D uTexture;

            void main() {
                fragColor = texture(uTexture, vTexCoord) * vColor;
            }
        """.trimIndent()

        spriteEffect = Effect()
        spriteEffect.linkProgram(vertexSrc, fragmentSrc)

        GLES30.glGenBuffers(1, vbo, 0)
    }

    fun begin(
        sortMode: SpriteSortMode = SpriteSortMode.Deferred,
        blendState: BlendState? = BlendState.AlphaBlend,
        samplerState: SamplerState? = SamplerState.LinearClamp,
        depthStencilState: DepthStencilState? = null,
        rasterizerState: RasterizerState? = null,
        effect: Effect? = null,
        transformMatrix: Matrix? = null
    ) {

        this.sortMode = sortMode
        this.effect = effect
        this.transformMatrix = transformMatrix

        sprites.clear()

        currentBatch = 0
        isStarted = true

        blendState?.apply()
        samplerState?.apply(0)
        depthStencilState?.apply()
        rasterizerState?.apply()
    }

    fun draw(
        texture: Texture2D,
        position: Vector2,
        sourceRectangle: Rectangle? = null,
        color: Color = Color.White,
        rotation: Float = 0f,
        origin: Vector2 = Vector2.Zero,
        scale: Vector2 = Vector2.One,
        effects: SpriteEffects = SpriteEffects.None,
        layerDepth: Float = 0f
    ) {

        val width = sourceRectangle?.width ?: texture.width
        val height = sourceRectangle?.height ?: texture.height

        val dest = Rectangle(
            (position.x - origin.x * scale.x).toInt(),
            (position.y - origin.y * scale.y).toInt(),
            (width * scale.x).toInt(),
            (height * scale.y).toInt()
        )

        val scaledOrigin = Vector2(
            origin.x * scale.x,
            origin.y * scale.y
        )

        sprites.add(
            SpriteInfo(
                texture,
                sourceRectangle,
                dest,
                color,
                rotation,
                scaledOrigin,
                effects,
                layerDepth
            )
        )

        afterDraw()
    }

    fun draw(
        texture: Texture2D,
        destinationRectangle: Rectangle,
        sourceRectangle: Rectangle? = null,
        color: Color = Color.White,
        rotation: Float = 0f,
        origin: Vector2 = Vector2.Zero,
        effects: SpriteEffects = SpriteEffects.None,
        layerDepth: Float = 0f
    ) {

        sprites.add(
            SpriteInfo(
                texture,
                sourceRectangle,
                destinationRectangle,
                color,
                rotation,
                origin,
                effects,
                layerDepth
            )
        )

        afterDraw()
    }

    fun draw(
        texture: Texture2D,
        position: Vector2,
        color: Color = Color.White
    ) {
        draw(texture, position, null, color)
    }

    fun draw(
        texture: Texture2D,
        rectangle: Rectangle,
        color: Color = Color.White
    ) {
        draw(texture, rectangle, null, color)
    }

    fun drawString(
        spriteFont: SpriteFont,
        text: String,
        position: Vector2,
        color: Color
    ) {
        spriteFont.draw(this, text, position, color)
    }

    fun end() {

        if (!isStarted)
            return

        flushAll()

        isStarted = false
    }

    private fun flushBatch() {

        if (currentBatch >= sprites.size)
            return

        val batchSize = minOf(
            maxBatchSize,
            sprites.size - currentBatch
        )

        if (batchSize == 0)
            return

        val rawSlice = sprites.subList(
            currentBatch,
            currentBatch + batchSize
        )

        val sortedSlice = when (sortMode) {

            SpriteSortMode.BackToFront ->
                rawSlice.sortedByDescending { it.depth }

            SpriteSortMode.FrontToBack ->
                rawSlice.sortedBy { it.depth }

            SpriteSortMode.Texture ->
                rawSlice.sortedBy { it.texture.glTexture }

            else ->
                rawSlice
        }

        var start = 0

        while (start < sortedSlice.size) {

            val texture = sortedSlice[start].texture

            var end = start + 1

            while (
                end < sortedSlice.size &&
                sortedSlice[end].texture === texture
            ) {
                end++
            }

            flushRun(texture, sortedSlice, start, end)

            start = end
        }

        currentBatch += batchSize
    }

    private fun afterDraw() {

        if (sortMode == SpriteSortMode.Immediate) {

            flushBatch()

            sprites.clear()

            currentBatch = 0
        }
        else if (sprites.size - currentBatch >= maxBatchSize) {

            flushBatch()
        }
    }

    private fun flushRun(
        texture: Texture2D,
        sortedSlice: List<SpriteInfo>,
        start: Int,
        end: Int
    ) {

        var vi = 0

        for (i in start until end) {

            val sprite = sortedSlice[i]

            val x = sprite.destinationRect.x.toFloat()
            val y = sprite.destinationRect.y.toFloat()

            val w = sprite.destinationRect.width.toFloat()
            val h = sprite.destinationRect.height.toFloat()

            val originX = sprite.origin.x
            val originY = sprite.origin.y

            val anchorX = x + originX
            val anchorY = y + originY

            val cosR = kotlin.math.cos(sprite.rotation)
            val sinR = kotlin.math.sin(sprite.rotation)

            val fx =
                if (sprite.effects.value and SpriteEffects.FlipHorizontally.value != 0)
                    -1f
                else
                    1f

            val fy =
                if (sprite.effects.value and SpriteEffects.FlipVertically.value != 0)
                    -1f
                else
                    1f

            val u0 = sprite.sourceRect?.let {
                it.x.toFloat() / sprite.texture.width
            } ?: 0f

            val v0 = sprite.sourceRect?.let {
                it.y.toFloat() / sprite.texture.height
            } ?: 0f

            val u1 = sprite.sourceRect?.let {
                (it.x + it.width).toFloat() / sprite.texture.width
            } ?: 1f

            val v1 = sprite.sourceRect?.let {
                (it.y + it.height).toFloat() / sprite.texture.height
            } ?: 1f

            val r = sprite.color.rf
            val g = sprite.color.gf
            val b = sprite.color.bf
            val a = sprite.color.af

            fun tx(lx: Float, ly: Float): Float {

                val dx = lx * fx
                val dy = ly * fy

                return dx * cosR - dy * sinR + anchorX
            }

            fun ty(lx: Float, ly: Float): Float {

                val dx = lx * fx
                val dy = ly * fy

                return dx * sinR + dy * cosR + anchorY
            }

            fun emit(
                rx: Float,
                ry: Float,
                u: Float,
                v: Float
            ) {

                vertexData[vi++] = rx
                vertexData[vi++] = ry
                vertexData[vi++] = 0f

                vertexData[vi++] = u
                vertexData[vi++] = v

                vertexData[vi++] = r
                vertexData[vi++] = g
                vertexData[vi++] = b
                vertexData[vi++] = a
            }

            val x0 = -originX
            val y0 = -originY

            val x1 = w - originX
            val y1 = h - originY

            emit(
                tx(x0, y0),
                ty(x0, y0),
                u0,
                v0
            )

            emit(
                tx(x1, y0),
                ty(x1, y0),
                u1,
                v0
            )

            emit(
                tx(x1, y1),
                ty(x1, y1),
                u1,
                v1
            )

            emit(
                tx(x0, y0),
                ty(x0, y0),
                u0,
                v0
            )

            emit(
                tx(x1, y1),
                ty(x1, y1),
                u1,
                v1
            )

            emit(
                tx(x0, y1),
                ty(x0, y1),
                u0,
                v1
            )
        }

        applySpriteEffect(texture, vi, end - start)
    }

    private fun applySpriteEffect(
        texture: Texture2D,
        vertexFloatCount: Int,
        spriteCount: Int
    ) {

        val eff = effect ?: spriteEffect

        eff.apply()

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)

        texture.bind()

        eff.setUniform("uTexture", 0)

        val w = graphicsDevice.viewport.width.toFloat()
        val h = graphicsDevice.viewport.height.toFloat()

        val projection = Matrix.createOrthographicOffCenter(
            0f,
            w,
            h,
            0f,
            0f,
            1f
        )

        val finalMatrix =
            transformMatrix?.let {
                projection * it
            } ?: projection

        eff.setUniformMatrix(
            "uProjection",
            finalMatrix
        )

        vertexBuffer.clear()

        vertexBuffer.put(
            vertexData,
            0,
            vertexFloatCount
        )

        vertexBuffer.position(0)

        GLES30.glBindBuffer(
            GLES30.GL_ARRAY_BUFFER,
            vbo[0]
        )

        GLES30.glBufferData(
            GLES30.GL_ARRAY_BUFFER,
            vertexFloatCount * 4,
            vertexBuffer,
            GLES30.GL_DYNAMIC_DRAW
        )

        val posLoc =
            eff.uniforms["aPosition"]
                ?: GLES30.glGetAttribLocation(
                    eff.program,
                    "aPosition"
                )

        val texLoc =
            eff.uniforms["aTexCoord"]
                ?: GLES30.glGetAttribLocation(
                    eff.program,
                    "aTexCoord"
                )

        val colLoc =
            eff.uniforms["aColor"]
                ?: GLES30.glGetAttribLocation(
                    eff.program,
                    "aColor"
                )

        val stride = 9 * 4

        if (posLoc >= 0) {

            GLES30.glEnableVertexAttribArray(posLoc)

            GLES30.glVertexAttribPointer(
                posLoc,
                3,
                GLES30.GL_FLOAT,
                false,
                stride,
                0
            )
        }

        if (texLoc >= 0) {

            GLES30.glEnableVertexAttribArray(texLoc)

            GLES30.glVertexAttribPointer(
                texLoc,
                2,
                GLES30.GL_FLOAT,
                false,
                stride,
                3 * 4
            )
        }

        if (colLoc >= 0) {

            GLES30.glEnableVertexAttribArray(colLoc)

            GLES30.glVertexAttribPointer(
                colLoc,
                4,
                GLES30.GL_FLOAT,
                false,
                stride,
                5 * 4
            )
        }

        GLES30.glEnable(GLES30.GL_BLEND)

        GLES30.glBlendFunc(
            GLES30.GL_SRC_ALPHA,
            GLES30.GL_ONE_MINUS_SRC_ALPHA
        )

        GLES30.glDrawArrays(
            GLES30.GL_TRIANGLES,
            0,
            spriteCount * 6
        )

        if (posLoc >= 0)
            GLES30.glDisableVertexAttribArray(posLoc)

        if (texLoc >= 0)
            GLES30.glDisableVertexAttribArray(texLoc)

        if (colLoc >= 0)
            GLES30.glDisableVertexAttribArray(colLoc)

        val error = GLES30.glGetError()

        if (error != GLES30.GL_NO_ERROR) {

            android.util.Log.e(
                "SpriteBatch",
                "GL error: $error"
            )
        }
    }

    private fun flushAll() {

        while (currentBatch < sprites.size) {

            flushBatch()
        }
    }

    fun dispose() {

        if (vbo[0] != 0) {

            GLES30.glDeleteBuffers(
                1,
                vbo,
                0
            )

            vbo[0] = 0
        }

        spriteEffect.dispose()
    }
}=== ./app/src/main/java/ru/kayron/dew/graphics/SpriteFont.kt ===
package ru.kayron.dew.graphics

import android.graphics.Paint
import android.graphics.Typeface
import androidx.core.graphics.createBitmap
import ru.kayron.dew.math.*

class SpriteFont(
    val texture: Texture2D,
    val glyphMap: Map<Char, GlyphInfo>,
    val lineSpacing: Float = 0f,
    val spacing: Float = 0f
) {
    data class GlyphInfo(
        val bounds: Rectangle,
        val width: Float,
        val character: Char
    )

    fun measureString(text: String): Vector2 {
        var width = 0f
        var height = lineSpacing
        for (ch in text) {
            val glyph = glyphMap[ch] ?: continue
            width += glyph.width + spacing
            if (glyph.bounds.height.toFloat() > height) {
                height = glyph.bounds.height.toFloat()
            }
        }
        return Vector2(width, height)
    }

    fun draw(batch: SpriteBatch, text: String, position: Vector2, color: Color) {
        var x = position.x
        for (ch in text) {
            val glyph = glyphMap[ch] ?: continue
            batch.draw(
                texture,
                Vector2(x, position.y),
                glyph.bounds,
                color
            )
            x += glyph.width + spacing
        }
    }

    companion object {
        fun fromSystemFont(
            fontName: String = "monospace",
            fontSize: Float = 24f,
            characters: String = "абвгдеёжзийклмнопрстуфхцчшщъыьэюяАБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯabcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789 .,!?:;()-+=_/\\\"'@#\$%^&*<>[]{}|~`"
        ): SpriteFont {
            val paint = Paint().apply {
                typeface = Typeface.create(fontName, Typeface.NORMAL)
                textSize = fontSize
                isAntiAlias = true
                isFakeBoldText = false
            }
            val metrics = paint.fontMetrics
            val lineHeight = (-metrics.ascent + metrics.descent).toInt()
            val glyphs = mutableMapOf<Char, GlyphInfo>()
            var totalWidth = 0
            for (ch in characters) {
                val width = paint.measureText(ch.toString()).toInt() + 2
                glyphs[ch] = GlyphInfo(Rectangle(totalWidth, 0, width, lineHeight), width.toFloat(), ch)
                totalWidth += width
            }
            val bitmap = createBitmap(totalWidth.coerceAtLeast(1), lineHeight.coerceAtLeast(1))
            val canvas = android.graphics.Canvas(bitmap)
            canvas.drawColor(android.graphics.Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR)
            paint.color = android.graphics.Color.WHITE
            var x = 0f
            for (ch in characters) {
                canvas.drawText(ch.toString(), x, -metrics.ascent, paint)
                x += glyphs[ch]?.width ?: 0f
            }
            val texture = Texture2D.fromBitmap(bitmap)
            return SpriteFont(texture, glyphs, lineHeight.toFloat())
        }
    }
}
=== ./app/src/main/java/ru/kayron/dew/graphics/GraphicsDevice.kt ===
package ru.kayron.dew.graphics

import android.opengl.GLES30
import android.opengl.GLES32
import ru.kayron.dew.math.Color
import ru.kayron.dew.math.Rectangle
import ru.kayron.dew.math.Vector4

class GraphicsDevice {
    val presentationParameters = PresentationParameters()
    var viewport: Viewport = Viewport()
        private set

    var blendState: BlendState = BlendState.Opaque
    var rasterizerState: RasterizerState = RasterizerState.CullCounterClockwise
    var depthStencilState: DepthStencilState = DepthStencilState.Default
    var samplerStates: Array<SamplerState?> = arrayOfNulls(16)
    var textures: Array<Texture?> = arrayOfNulls(16)

    var vertexBuffer: VertexBuffer? = null
    var indexBuffer: IndexBuffer? = null

    private var defaultFramebuffer: Int = 0
    private var currentRenderTarget: RenderTarget2D? = null

    val displayMode: DisplayMode
        get() = DisplayMode(viewport.width, viewport.height)

    fun initialize() {
        val fb = IntArray(1)
        GLES30.glGetIntegerv(GLES30.GL_FRAMEBUFFER_BINDING, fb, 0)
        defaultFramebuffer = fb[0]

        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glDepthFunc(GLES30.GL_LEQUAL)
        GLES30.glEnable(GLES30.GL_CULL_FACE)

        val extVersion = GLES30.glGetString(GLES30.GL_EXTENSIONS) ?: ""
        if (extVersion.contains("GL_OES_EGL_image_external")) {
            android.util.Log.i("GraphicsDevice", "EGL image external supported")
        }
        android.util.Log.i("GraphicsDevice", "OpenGL ES ${GLES30.glGetString(GLES30.GL_VERSION)}")
        android.util.Log.i("GraphicsDevice", "GL Renderer: ${GLES30.glGetString(GLES30.GL_RENDERER)}")
    }

    fun clear(color: Color) {
        GLES30.glClearColor(color.rf, color.gf, color.bf, color.af)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT or GLES30.GL_STENCIL_BUFFER_BIT)
    }

    fun clear(options: ClearOptions, color: Color, depth: Float = 1f, stencil: Int = 0) {
        var mask = 0
        if (options == ClearOptions.Target || options.value and 1 != 0) {
            GLES30.glClearColor(color.rf, color.gf, color.bf, color.af)
            mask = mask or GLES30.GL_COLOR_BUFFER_BIT
        }
        if (options == ClearOptions.DepthBuffer || options.value and 2 != 0) {
            GLES30.glClearDepthf(depth)
            mask = mask or GLES30.GL_DEPTH_BUFFER_BIT
        }
        if (options == ClearOptions.Stencil || options.value and 4 != 0) {
            GLES30.glClearStencil(stencil)
            mask = mask or GLES30.GL_STENCIL_BUFFER_BIT
        }
        GLES30.glClear(mask)
    }

    fun setViewport(x: Int, y: Int, width: Int, height: Int) {
        viewport = Viewport(x, y, width, height)
        GLES30.glViewport(x, y, width, height)
        GLES30.glScissor(x, y, width, height)
    }

    @JvmName("applyViewport")
    fun setViewport(vp: Viewport) {
        setViewport(vp.x, vp.y, vp.width, vp.height)
    }

    fun setRenderTarget(renderTarget: RenderTarget2D?) {
        if (renderTarget == null) {
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, defaultFramebuffer)
            currentRenderTarget = null
            setViewport(0, 0, presentationParameters.backBufferWidth, presentationParameters.backBufferHeight)
        } else {
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, renderTarget.glFramebuffer)
            currentRenderTarget = renderTarget
            setViewport(0, 0, renderTarget.width, renderTarget.height)
        }
    }

    fun getRenderTarget(): RenderTarget2D? = currentRenderTarget

    @JvmName("bindVertexBuffer")
    fun setVertexBuffer(buffer: VertexBuffer) {
        vertexBuffer = buffer
        buffer.bind()
    }

    @JvmName("bindIndexBuffer")
    fun setIndexBuffer(buffer: IndexBuffer) {
        indexBuffer = buffer
        buffer.bind()
    }

    fun drawPrimitives(primitiveType: PrimitiveType, startVertex: Int, vertexCount: Int) {
        vertexBuffer?.bind()
        vertexBuffer?.declaration?.setup(0)
        GLES30.glDrawArrays(primitiveType.glType, startVertex, vertexCount)
    }

    fun drawIndexedPrimitives(primitiveType: PrimitiveType, baseVertex: Int, startIndex: Int, primitiveCount: Int) {
        vertexBuffer?.bind()
        indexBuffer?.bind()
        vertexBuffer?.declaration?.setup(0)
        val indexCount = when (primitiveType) {
            PrimitiveType.TriangleList -> primitiveCount * 3
            PrimitiveType.TriangleStrip -> primitiveCount + 2
            PrimitiveType.LineList -> primitiveCount * 2
            PrimitiveType.LineStrip -> primitiveCount + 1
            PrimitiveType.PointList -> primitiveCount
        }
        GLES30.glDrawElements(primitiveType.glType, indexCount, GLES30.GL_UNSIGNED_SHORT, startIndex * 2)
    }

    fun setScissorRect(rect: Rectangle) {
        GLES30.glScissor(rect.x, rect.y, rect.width, rect.height)
    }

    fun present() {
        val error = GLES30.glGetError()
        if (error != GLES30.GL_NO_ERROR) {
            android.util.Log.w("GraphicsDevice", "GL error after present: $error")
        }
        GLES30.glFinish()
    }

    fun dispose() {
        vertexBuffer?.dispose()
        indexBuffer?.dispose()
    }
}
=== ./app/src/main/java/ru/kayron/dew/input/Keys.kt ===
package ru.kayron.dew.input

enum class Keys(val value: Int) {
    None(-1),
    A(29),
    B(30),
    C(31),
    D(32),
    E(33),
    F(34),
    G(35),
    H(36),
    I(37),
    J(38),
    K(39),
    L(40),
    M(41),
    N(42),
    O(43),
    P(44),
    Q(45),
    R(46),
    S(47),
    T(48),
    U(49),
    V(50),
    W(51),
    X(52),
    Y(53),
    Z(54),
    D0(7),
    D1(8),
    D2(9),
    D3(10),
    D4(11),
    D5(12),
    D6(13),
    D7(14),
    D8(15),
    D9(16),
    Back(4),
    Tab(61),
    Enter(66),
    ShiftLeft(59),
    ShiftRight(60),
    CtrlLeft(113),
    CtrlRight(114),
    AltLeft(57),
    AltRight(58),
    Space(62),
    Escape(111),
    Delete(112),
    VolumeUp(25),
    VolumeDown(24),
    Home(3),
    Backspace(67),
    Menu(1),
    DpadUp(19),
    DpadDown(20),
    DpadLeft(21),
    DpadRight(22),
    NumPad0(144),
    NumPad1(145),
    NumPad2(146),
    NumPad3(147),
    NumPad4(148),
    NumPad5(149),
    NumPad6(150),
    NumPad7(151),
    NumPad8(152),
    NumPad9(153),
    F1(131),
    F2(132),
    F3(133),
    F4(134),
    F5(135),
    F6(136),
    F7(137),
    F8(138),
    F9(139),
    F10(140),
    F11(141),
    F12(142),
    OemPeriod(158),
    OemComma(55),
    OemMinus(170),
    OemPlus(135),
    OemSemicolon(181),
    OemQuestion(154),
    OemOpenBrackets(29),
    OemCloseBrackets(36),
    OemPipe(74),
    OemQuotes(76),
    OemTilde(68),
    OemBackslash(73),
    Insert(124),
    PageUp(92),
    PageDown(93),
    End(123),
    CapsLock(115),
    NumLock(143),
    ScrollLock(116),
    Break(131),
    PrintScreen(120),
    Pause(121),
    Sleep(125),
    Zoom(168);
}
=== ./app/src/main/java/ru/kayron/dew/input/KeyboardState.kt ===
package ru.kayron.dew.input

import android.view.KeyEvent

class KeyboardState {
    internal val keys = mutableSetOf<Keys>()

    fun isKeyDown(key: Keys): Boolean = keys.contains(key)

    fun isKeyUp(key: Keys): Boolean = !keys.contains(key)

    fun getPressedKeys(): Set<Keys> = keys.toSet()

    internal fun onKeyDown(keyCode: Int) {
        val key = keyCodeToKeys(keyCode) ?: return
        keys.add(key)
    }

    internal fun onKeyUp(keyCode: Int) {
        val key = keyCodeToKeys(keyCode) ?: return
        keys.remove(key)
    }

    internal fun clear() {
        keys.clear()
    }

    companion object {
        fun keyCodeToKeys(keyCode: Int): Keys? {
            return Keys.entries.find { it.value == keyCode }
        }
    }
}
=== ./app/src/main/java/ru/kayron/dew/input/Keyboard.kt ===
package ru.kayron.dew.input

object Keyboard {
    private val currentState = KeyboardState()
    private val previousState = KeyboardState()

    fun getState(): KeyboardState = currentState

    fun getPreviousState(): KeyboardState = previousState

    fun isKeyPressed(key: Keys): Boolean =
        currentState.isKeyDown(key) && previousState.isKeyUp(key)

    fun isKeyReleased(key: Keys): Boolean =
        currentState.isKeyUp(key) && previousState.isKeyDown(key)

    internal fun onKeyDown(keyCode: Int) {
        currentState.onKeyDown(keyCode)
    }

    internal fun onKeyUp(keyCode: Int) {
        currentState.onKeyUp(keyCode)
    }

    internal fun update() {
        previousState.keys.clear()
        previousState.keys.addAll(currentState.keys)
    }

    internal fun clear() {
        previousState.clear()
        currentState.clear()
    }
}
=== ./app/src/main/java/ru/kayron/dew/input/MouseState.kt ===
package ru.kayron.dew.input

import ru.kayron.dew.math.Point

data class MouseState(
    val x: Int = 0,
    val y: Int = 0,
    val leftButton: ButtonState = ButtonState.Released,
    val middleButton: ButtonState = ButtonState.Released,
    val rightButton: ButtonState = ButtonState.Released,
    val scrollWheelValue: Int = 0,
    val horizontalScrollWheelValue: Int = 0,
) {
    val position: Point get() = Point(x, y)

    enum class ButtonState {
        Pressed,
        Released,
    }
}
=== ./app/src/main/java/ru/kayron/dew/input/Mouse.kt ===
package ru.kayron.dew.input

object Mouse {
    private var currentState = MouseState()
    private var previousState = MouseState()

    fun getState(): MouseState = currentState

    fun setPosition(x: Int, y: Int) {
        currentState = currentState.copy(x = x, y = y)
    }

    fun isButtonPressed(button: MouseState.ButtonState): Boolean =
        button == MouseState.ButtonState.Pressed

    internal fun onTouch(x: Float, y: Float) {
        currentState = currentState.copy(
            x = x.toInt(),
            y = y.toInt(),
            leftButton = MouseState.ButtonState.Pressed
        )
    }

    internal fun onTouchUp() {
        currentState = currentState.copy(
            leftButton = MouseState.ButtonState.Released
        )
    }

    internal fun update() {
        previousState = currentState
    }
}
=== ./app/src/main/java/ru/kayron/dew/input/TouchLocation.kt ===
package ru.kayron.dew.input

import ru.kayron.dew.math.Vector2

data class TouchLocation(
    val id: Int = 0,
    val position: Vector2 = Vector2.Zero,
    val previousPosition: Vector2 = Vector2.Zero,
    val state: TouchLocationState = TouchLocationState.Invalid,
    val pressure: Float = 1f
) {
    enum class TouchLocationState {
        Invalid,
        Pressed,
        Moved,
        Released,
    }

    fun tryGetPreviousLocation(): TouchLocation {
        return if (previousPosition != position) {
            copy(position = previousPosition, state = TouchLocationState.Moved)
        } else this
    }
}
=== ./app/src/main/java/ru/kayron/dew/input/TouchCollection.kt ===
package ru.kayron.dew.input

class TouchCollection : ArrayList<TouchLocation>() {
    fun isAnyTouch(): Boolean = isNotEmpty()

    fun findById(id: Int): TouchLocation? = find { it.id == id }

    fun clearAll() {
        for (i in indices) {
            if (this[i].state != TouchLocation.TouchLocationState.Released) {
                this[i] = this[i].copy(state = TouchLocation.TouchLocationState.Released)
            }
        }
    }
}
=== ./app/src/main/java/ru/kayron/dew/input/Touch.kt ===
package ru.kayron.dew.input

import android.view.MotionEvent
import ru.kayron.dew.math.Vector2

object Touch {
    private val lock = Any()
    private var currentTouches = TouchCollection()
    private var previousTouches = TouchCollection()

    fun getState(): TouchCollection {
        synchronized(lock) { return TouchCollection().also { it.addAll(currentTouches) } }
    }

    fun getPreviousState(): TouchCollection {
        synchronized(lock) { return TouchCollection().also { it.addAll(previousTouches) } }
    }

    fun isAnyTouch(): Boolean = synchronized(lock) { currentTouches.isNotEmpty() }

    internal fun onTouchEvent(event: MotionEvent) {
        synchronized(lock) {
            val pointerIndex = event.actionIndex
            val pointerId = event.getPointerId(pointerIndex)
            val x = event.getX(pointerIndex)
            val y = event.getY(pointerIndex)

            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                    if (currentTouches.findById(pointerId) == null) {
                        currentTouches.add(
                            TouchLocation(
                                id = pointerId,
                                position = Vector2(x, y),
                                state = TouchLocation.TouchLocationState.Pressed,
                                pressure = event.getPressure(pointerIndex)
                            )
                        )
                    }
                }
                MotionEvent.ACTION_MOVE -> {
                    for (i in 0 until event.pointerCount) {
                        val id = event.getPointerId(i)
                        val touch = currentTouches.findById(id)
                        if (touch != null) {
                            val idx = currentTouches.indexOf(touch)
                            currentTouches[idx] = touch.copy(
                                position = Vector2(event.getX(i), event.getY(i)),
                                previousPosition = touch.position,
                                state = TouchLocation.TouchLocationState.Moved
                            )
                        }
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                    val touch = currentTouches.findById(pointerId)
                    if (touch != null) {
                        val idx = currentTouches.indexOf(touch)
                        currentTouches[idx] = touch.copy(state = TouchLocation.TouchLocationState.Released)
                    }
                }
                MotionEvent.ACTION_CANCEL -> {
                    currentTouches.clear()
                }
            }
        }
    }

    internal fun update() {
        synchronized(lock) {
            previousTouches.clear()
            previousTouches.addAll(currentTouches)
            currentTouches.removeAll { it.state == TouchLocation.TouchLocationState.Released }
        }
    }
}
=== ./app/src/main/java/ru/kayron/dew/input/Buttons.kt ===
package ru.kayron.dew.input

enum class Buttons(val value: Long) {
    DPadUp(1L),
    DPadDown(2L),
    DPadLeft(4L),
    DPadRight(8L),
    Start(16L),
    Back(32L),
    LeftStick(64L),
    RightStick(128L),
    LeftShoulder(256L),
    RightShoulder(512L),
    A(4096L),
    B(8192L),
    X(16384L),
    Y(32768L),
    LeftThumbstickLeft(2097152L),
    RightTrigger(4194304L),
    LeftTrigger(8388608L),
    RightThumbstickUp(16777216L),
    RightThumbstickDown(33554432L),
    RightThumbstickRight(67108864L),
    RightThumbstickLeft(134217728L),
    LeftThumbstickUp(268435456L),
    LeftThumbstickDown(536870912L),
    LeftThumbstickRight(1073741824L),
    BigButton(Int.MIN_VALUE.toLong()),
}
=== ./app/src/main/java/ru/kayron/dew/input/GamePadButtons.kt ===
package ru.kayron.dew.input

data class GamePadButtons(
    val a: Boolean = false,
    val b: Boolean = false,
    val x: Boolean = false,
    val y: Boolean = false,
    val start: Boolean = false,
    val back: Boolean = false,
    val leftShoulder: Boolean = false,
    val rightShoulder: Boolean = false,
    val leftStick: Boolean = false,
    val rightStick: Boolean = false,
    val bigButton: Boolean = false,
) {
    companion object {
        val None = GamePadButtons()
    }
}
=== ./app/src/main/java/ru/kayron/dew/input/GamePadDPad.kt ===
package ru.kayron.dew.input

data class GamePadDPad(
    val up: Boolean = false,
    val down: Boolean = false,
    val left: Boolean = false,
    val right: Boolean = false,
) {
    companion object {
        val None = GamePadDPad()
    }
}
=== ./app/src/main/java/ru/kayron/dew/input/GamePadThumbSticks.kt ===
package ru.kayron.dew.input

import ru.kayron.dew.math.Vector2

data class GamePadThumbSticks(
    val left: Vector2 = Vector2.Zero,
    val right: Vector2 = Vector2.Zero,
) {
    companion object {
        val None = GamePadThumbSticks()
    }
}
=== ./app/src/main/java/ru/kayron/dew/input/GamePadTriggers.kt ===
package ru.kayron.dew.input

data class GamePadTriggers(
    val left: Float = 0f,
    val right: Float = 0f,
) {
    companion object {
        val None = GamePadTriggers()
    }
}
=== ./app/src/main/java/ru/kayron/dew/input/GamePadState.kt ===
package ru.kayron.dew.input

data class GamePadState(
    val isConnected: Boolean = false,
    val packetNumber: Int = 0,
    val buttons: GamePadButtons = GamePadButtons.None,
    val dPad: GamePadDPad = GamePadDPad.None,
    val thumbSticks: GamePadThumbSticks = GamePadThumbSticks.None,
    val triggers: GamePadTriggers = GamePadTriggers.None,
)
=== ./app/src/main/java/ru/kayron/dew/input/GamePad.kt ===
package ru.kayron.dew.input

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.InputDevice
import android.view.MotionEvent
import android.view.KeyEvent

object GamePad {
    private val states = Array(4) { GamePadState() }
    private var vibrator: Vibrator? = null

    internal fun initialize(context: Context) {
        vibrator = context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    }

    fun getState(playerIndex: Int): GamePadState {
        return if (playerIndex in 0..3) states[playerIndex] else GamePadState()
    }

    fun setVibration(playerIndex: Int, leftMotor: Float, rightMotor: Float): Boolean {
        if (playerIndex !in 0..3) return false
        val vib = vibrator ?: return false
        if (!vib.hasVibrator()) return false
        val amplitude = (maxOf(leftMotor, rightMotor).coerceIn(0f, 1f) * 255f).toInt()
        if (amplitude <= 0) {
            vib.cancel()
            return true
        }
        vib.vibrate(VibrationEffect.createOneShot(100L, amplitude.coerceIn(1, 255)))
        return true
    }

    internal fun onKeyDown(keyCode: Int, playerIndex: Int = 0) {
        val state = states[playerIndex]
        val buttons = state.buttons
        val dPad = state.dPad
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> states[playerIndex] = state.copy(
                dPad = dPad.copy(up = true)
            )
            KeyEvent.KEYCODE_DPAD_DOWN -> states[playerIndex] = state.copy(
                dPad = dPad.copy(down = true)
            )
            KeyEvent.KEYCODE_DPAD_LEFT -> states[playerIndex] = state.copy(
                dPad = dPad.copy(left = true)
            )
            KeyEvent.KEYCODE_DPAD_RIGHT -> states[playerIndex] = state.copy(
                dPad = dPad.copy(right = true)
            )
            KeyEvent.KEYCODE_BUTTON_A -> states[playerIndex] = state.copy(
                buttons = buttons.copy(a = true)
            )
            KeyEvent.KEYCODE_BUTTON_B -> states[playerIndex] = state.copy(
                buttons = buttons.copy(b = true)
            )
            KeyEvent.KEYCODE_BUTTON_X -> states[playerIndex] = state.copy(
                buttons = buttons.copy(x = true)
            )
            KeyEvent.KEYCODE_BUTTON_Y -> states[playerIndex] = state.copy(
                buttons = buttons.copy(y = true)
            )
            KeyEvent.KEYCODE_BUTTON_L1 -> states[playerIndex] = state.copy(
                buttons = buttons.copy(leftShoulder = true)
            )
            KeyEvent.KEYCODE_BUTTON_R1 -> states[playerIndex] = state.copy(
                buttons = buttons.copy(rightShoulder = true)
            )
            KeyEvent.KEYCODE_BUTTON_L2 -> states[playerIndex] = state.copy(
                triggers = state.triggers.copy(left = 1f)
            )
            KeyEvent.KEYCODE_BUTTON_R2 -> states[playerIndex] = state.copy(
                triggers = state.triggers.copy(right = 1f)
            )
            KeyEvent.KEYCODE_BUTTON_THUMBL -> states[playerIndex] = state.copy(
                buttons = buttons.copy(leftStick = true)
            )
            KeyEvent.KEYCODE_BUTTON_THUMBR -> states[playerIndex] = state.copy(
                buttons = buttons.copy(rightStick = true)
            )
            KeyEvent.KEYCODE_BUTTON_START -> states[playerIndex] = state.copy(
                buttons = buttons.copy(start = true)
            )
            KeyEvent.KEYCODE_BUTTON_SELECT -> states[playerIndex] = state.copy(
                buttons = buttons.copy(back = true)
            )
        }
    }

    internal fun onKeyUp(keyCode: Int, playerIndex: Int = 0) {
        val state = states[playerIndex]
        val buttons = state.buttons
        val dPad = state.dPad
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> states[playerIndex] = state.copy(
                dPad = dPad.copy(up = false)
            )
            KeyEvent.KEYCODE_DPAD_DOWN -> states[playerIndex] = state.copy(
                dPad = dPad.copy(down = false)
            )
            KeyEvent.KEYCODE_DPAD_LEFT -> states[playerIndex] = state.copy(
                dPad = dPad.copy(left = false)
            )
            KeyEvent.KEYCODE_DPAD_RIGHT -> states[playerIndex] = state.copy(
                dPad = dPad.copy(right = false)
            )
            KeyEvent.KEYCODE_BUTTON_A -> states[playerIndex] = state.copy(
                buttons = buttons.copy(a = false)
            )
            KeyEvent.KEYCODE_BUTTON_B -> states[playerIndex] = state.copy(
                buttons = buttons.copy(b = false)
            )
            KeyEvent.KEYCODE_BUTTON_X -> states[playerIndex] = state.copy(
                buttons = buttons.copy(x = false)
            )
            KeyEvent.KEYCODE_BUTTON_Y -> states[playerIndex] = state.copy(
                buttons = buttons.copy(y = false)
            )
            KeyEvent.KEYCODE_BUTTON_L1 -> states[playerIndex] = state.copy(
                buttons = buttons.copy(leftShoulder = false)
            )
            KeyEvent.KEYCODE_BUTTON_R1 -> states[playerIndex] = state.copy(
                buttons = buttons.copy(rightShoulder = false)
            )
            KeyEvent.KEYCODE_BUTTON_L2 -> states[playerIndex] = state.copy(
                triggers = state.triggers.copy(left = 0f)
            )
            KeyEvent.KEYCODE_BUTTON_R2 -> states[playerIndex] = state.copy(
                triggers = state.triggers.copy(right = 0f)
            )
            KeyEvent.KEYCODE_BUTTON_THUMBL -> states[playerIndex] = state.copy(
                buttons = buttons.copy(leftStick = false)
            )
            KeyEvent.KEYCODE_BUTTON_THUMBR -> states[playerIndex] = state.copy(
                buttons = buttons.copy(rightStick = false)
            )
            KeyEvent.KEYCODE_BUTTON_START -> states[playerIndex] = state.copy(
                buttons = buttons.copy(start = false)
            )
            KeyEvent.KEYCODE_BUTTON_SELECT -> states[playerIndex] = state.copy(
                buttons = buttons.copy(back = false)
            )
        }
    }

    internal fun onJoystickMotion(event: MotionEvent) {
        val playerIndex = 0
        val state = states[playerIndex]
        val lx = event.getAxisValue(MotionEvent.AXIS_X)
        val ly = event.getAxisValue(MotionEvent.AXIS_Y)
        val rx = event.getAxisValue(MotionEvent.AXIS_Z)
        val ry = event.getAxisValue(MotionEvent.AXIS_RZ)
        val lt = event.getAxisValue(MotionEvent.AXIS_LTRIGGER)
        val rt = event.getAxisValue(MotionEvent.AXIS_RTRIGGER)
        states[playerIndex] = state.copy(
            thumbSticks = ru.kayron.dew.input.GamePadThumbSticks(
                left = ru.kayron.dew.math.Vector2(lx, ly),
                right = ru.kayron.dew.math.Vector2(rx, ry)
            ),
            triggers = ru.kayron.dew.input.GamePadTriggers(lt, rt),
            isConnected = true
        )
    }

    internal fun update() {
        for (i in states.indices) {
            states[i] = states[i].copy(packetNumber = states[i].packetNumber + 1)
        }
    }
}
=== ./app/src/main/java/ru/kayron/dew/audio/SoundEffect.kt ===
package ru.kayron.dew.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaCodec
import android.media.MediaDataSource
import android.media.MediaExtractor
import android.media.MediaFormat
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.ByteBuffer

private fun ByteArray.getShortAt(offset: Int): Short {
    return (((this[offset + 1].toInt() and 0xFF) shl 8) or (this[offset].toInt() and 0xFF)).toShort()
}

private fun ByteArray.getIntAt(offset: Int): Int {
    return ((this[offset + 3].toInt() and 0xFF) shl 24) or
           ((this[offset + 2].toInt() and 0xFF) shl 16) or
           ((this[offset + 1].toInt() and 0xFF) shl 8) or
           (this[offset].toInt() and 0xFF)
}

class SoundEffect(
    val buffer: ByteArray,
    val sampleRate: Int = 44100,
    val channels: Int = 2,
    val bitsPerSample: Int = 16,
    val duration: Long = 0L
) {
    private var instances = mutableListOf<SoundEffectInstance>()

    fun createInstance(): SoundEffectInstance {
        val instance = SoundEffectInstance(this)
        instances.add(instance)
        return instance
    }

    fun play(volume: Float = 1f, pitch: Float = 0f, pan: Float = 0f) {
        val instance = createInstance()
        instance.volume = volume
        instance.pitch = pitch
        instance.pan = pan
        instance.play()
    }

    fun dispose() {
        for (inst in instances.toList()) {
            inst.dispose()
        }
        instances.clear()
    }

    internal fun releaseInstance(instance: SoundEffectInstance) {
        instances.remove(instance)
    }

    companion object {
        fun fromWav(inputStream: InputStream): SoundEffect {
            val data = inputStream.use { it.readBytes() }
            if (data.size < 12) throw RuntimeException("Invalid WAV file")
            val riff = String(data, 0, 4)
            if (riff != "RIFF") throw RuntimeException("Not a WAV file")
            val wave = String(data, 8, 4)
            if (wave != "WAVE") throw RuntimeException("Not a WAVE file")
            var offset = 12
            var format: Short = 1
            var channels: Short = 1
            var sampleRate = 44100
            var bitsPerSample: Short = 16
            var audioData = ByteArray(0)

            while (offset < data.size - 8) {
                val chunkId = String(data, offset, 4)
                val chunkSize = data.getIntAt(offset + 4)
                offset += 8
                if (chunkSize < 0 || offset + chunkSize > data.size) {
                    throw RuntimeException("Invalid WAV chunk: $chunkId")
                }
                when (chunkId) {
                    "fmt " -> {
                        format = data.getShortAt(offset)
                        channels = data.getShortAt(offset + 2)
                        sampleRate = data.getIntAt(offset + 4)
                        bitsPerSample = data.getShortAt(offset + 14)
                    }
                    "data" -> {
                        audioData = data.copyOfRange(offset, offset + chunkSize)
                    }
                }
                offset += chunkSize + (chunkSize and 1)
            }
            if (format.toInt() != 1) {
                throw RuntimeException("Only PCM WAV is supported, format=$format")
            }
            if (audioData.isEmpty()) {
                throw RuntimeException("WAV data chunk is empty")
            }
            val durationMs = if (sampleRate > 0) audioData.size.toLong() * 1000 / (sampleRate * channels * bitsPerSample / 8) else 0L
            return SoundEffect(audioData, sampleRate, channels.toInt(), bitsPerSample.toInt(), durationMs)
        }

        fun fromOgg(inputStream: InputStream): SoundEffect {
            val data = inputStream.use { it.readBytes() }
            val source = ByteArrayMediaDataSource(data)
            val extractor = MediaExtractor()
            var codec: MediaCodec? = null
            try {
                extractor.setDataSource(source)
                var trackIndex = -1
                var inputFormat: MediaFormat? = null
                for (i in 0 until extractor.trackCount) {
                    val format = extractor.getTrackFormat(i)
                    val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                    if (mime.startsWith("audio/")) {
                        trackIndex = i
                        inputFormat = format
                        break
                    }
                }
                if (trackIndex < 0 || inputFormat == null) {
                    throw RuntimeException("No audio track found in OGG")
                }
                extractor.selectTrack(trackIndex)

                val mime = inputFormat.getString(MediaFormat.KEY_MIME)
                    ?: throw RuntimeException("Missing OGG mime type")
                codec = MediaCodec.createDecoderByType(mime)
                codec.configure(inputFormat, null, null, 0)
                codec.start()

                val output = ByteArrayOutputStream(data.size * 2)
                val info = MediaCodec.BufferInfo()
                var sawInputEnd = false
                var sawOutputEnd = false
                var sampleRate = if (inputFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                    inputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                } else 44100
                var channels = if (inputFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                    inputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                } else 2
                val timeoutUs = 10_000L

                while (!sawOutputEnd) {
                    if (!sawInputEnd) {
                        val inputIndex = codec.dequeueInputBuffer(timeoutUs)
                        if (inputIndex >= 0) {
                            val inputBuffer = codec.getInputBuffer(inputIndex)
                            val sampleSize = if (inputBuffer != null) {
                                extractor.readSampleData(inputBuffer, 0)
                            } else {
                                -1
                            }
                            if (sampleSize < 0) {
                                codec.queueInputBuffer(
                                    inputIndex,
                                    0,
                                    0,
                                    0L,
                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM
                                )
                                sawInputEnd = true
                            } else {
                                codec.queueInputBuffer(
                                    inputIndex,
                                    0,
                                    sampleSize,
                                    extractor.sampleTime.coerceAtLeast(0L),
                                    0
                                )
                                extractor.advance()
                            }
                        }
                    }

                    when (val outputIndex = codec.dequeueOutputBuffer(info, timeoutUs)) {
                        MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            val outputFormat = codec.outputFormat
                            if (outputFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                                sampleRate = outputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                            }
                            if (outputFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                                channels = outputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                            }
                        }
                        MediaCodec.INFO_TRY_AGAIN_LATER -> Unit
                        else -> if (outputIndex >= 0) {
                            val outputBuffer: ByteBuffer? = codec.getOutputBuffer(outputIndex)
                            if (outputBuffer != null && info.size > 0) {
                                val bytes = ByteArray(info.size)
                                outputBuffer.position(info.offset)
                                outputBuffer.limit(info.offset + info.size)
                                outputBuffer.get(bytes)
                                output.write(bytes)
                            }
                            sawOutputEnd = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                            codec.releaseOutputBuffer(outputIndex, false)
                        }
                    }
                }

                val pcm = output.toByteArray()
                val durationMs = if (sampleRate > 0 && channels > 0) {
                    pcm.size.toLong() * 1000L / (sampleRate * channels * 2L)
                } else 0L
                return SoundEffect(pcm, sampleRate, channels, 16, durationMs)
            } finally {
                try {
                    codec?.stop()
                } catch (_: Exception) {
                }
                codec?.release()
                extractor.release()
                source.close()
            }
        }
    }
}

private class ByteArrayMediaDataSource(private val data: ByteArray) : MediaDataSource() {
    override fun readAt(position: Long, buffer: ByteArray, offset: Int, size: Int): Int {
        if (position >= data.size) return -1
        val length = minOf(size, data.size - position.toInt())
        System.arraycopy(data, position.toInt(), buffer, offset, length)
        return length
    }

    override fun getSize(): Long = data.size.toLong()

    override fun close() {}
}
=== ./app/src/main/java/ru/kayron/dew/audio/SoundEffectInstance.kt ===
package ru.kayron.dew.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.PlaybackParams
import android.media.AudioTrack
import kotlin.math.pow

class SoundEffectInstance(private val soundEffect: SoundEffect) {
    var volume: Float = 1f
    var pitch: Float = 0f
    var pan: Float = 0f
    var isLooped: Boolean = false

    private var audioTrack: AudioTrack? = null
    private var state: SoundState = SoundState.Stopped

    enum class SoundState {
        Playing,
        Paused,
        Stopped,
    }

    fun play() {
        if (state == SoundState.Playing) return
        stop()
        val channelConfig = when (soundEffect.channels) {
            1 -> AudioFormat.CHANNEL_OUT_MONO
            2 -> AudioFormat.CHANNEL_OUT_STEREO
            else -> AudioFormat.CHANNEL_OUT_STEREO
        }
        val audioFormat = when (soundEffect.bitsPerSample) {
            8 -> AudioFormat.ENCODING_PCM_8BIT
            16 -> AudioFormat.ENCODING_PCM_16BIT
            else -> AudioFormat.ENCODING_PCM_16BIT
        }
        val bufferSize = soundEffect.buffer.size.coerceAtLeast(
            AudioTrack.getMinBufferSize(soundEffect.sampleRate, channelConfig, audioFormat)
        )
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val track = AudioTrack.Builder()
            .setAudioAttributes(attributes)
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(audioFormat)
                    .setSampleRate(soundEffect.sampleRate)
                    .setChannelMask(channelConfig)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        audioTrack = track
        track.write(soundEffect.buffer, 0, soundEffect.buffer.size)
        val bytesPerFrame = (soundEffect.channels * soundEffect.bitsPerSample / 8).coerceAtLeast(1)
        val frameCount = soundEffect.buffer.size / bytesPerFrame
        if (isLooped) {
            if (frameCount > 0) {
                track.setLoopPoints(0, frameCount, -1)
            }
        } else if (frameCount > 0) {
            track.notificationMarkerPosition = frameCount
            track.setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
                override fun onMarkerReached(track: AudioTrack) {
                    stop()
                }

                override fun onPeriodicNotification(track: AudioTrack) = Unit
            })
        }
        applyPlaybackParameters(track)
        track.play()
        state = SoundState.Playing
    }

    private fun applyPlaybackParameters(track: AudioTrack) {
        val clampedVolume = volume.coerceIn(0f, 1f)
        val clampedPan = pan.coerceIn(-1f, 1f)
        val leftVolume = clampedVolume * (1f - clampedPan).coerceIn(0f, 1f)
        val rightVolume = clampedVolume * (1f + clampedPan).coerceIn(0f, 1f)
        val pitchFactor = 2f.pow(pitch.coerceIn(-1f, 1f))

        @Suppress("DEPRECATION")
        track.setStereoVolume(leftVolume, rightVolume)
        track.playbackParams = PlaybackParams().setSpeed(1f).setPitch(pitchFactor)
    }

    fun pause() {
        audioTrack?.pause()
        state = SoundState.Paused
    }

    fun resume() {
        audioTrack?.play()
        state = SoundState.Playing
    }

    fun stop() {
        audioTrack?.let { track ->
            try {
                track.stop()
            } catch (_: IllegalStateException) {
            }
            track.release()
        }
        audioTrack = null
        state = SoundState.Stopped
        soundEffect.releaseInstance(this)
    }

    fun dispose() {
        stop()
    }
}
=== ./app/src/main/java/ru/kayron/dew/audio/AudioEngine.kt ===
package ru.kayron.dew.audio

class AudioEngine {
    var isDisposed: Boolean = false
        private set

    fun update() {}

    fun dispose() {
        isDisposed = true
    }
}
=== ./app/src/main/java/ru/kayron/dew/audio/AudioListener.kt ===
package ru.kayron.dew.audio

import ru.kayron.dew.math.Vector3
import ru.kayron.dew.math.Matrix

data class AudioListener(
    var position: Vector3 = Vector3.Zero,
    var forward: Vector3 = Vector3.Forward,
    var up: Vector3 = Vector3.Up,
    var velocity: Vector3 = Vector3.Zero,
) {
    companion object {
        val Default = AudioListener()
    }
}
=== ./app/src/main/java/ru/kayron/dew/audio/AudioEmitter.kt ===
package ru.kayron.dew.audio

import ru.kayron.dew.math.Vector3

data class AudioEmitter(
    var position: Vector3 = Vector3.Zero,
    var forward: Vector3 = Vector3.Forward,
    var up: Vector3 = Vector3.Up,
    var velocity: Vector3 = Vector3.Zero,
    var dopplerScale: Float = 1f,
)
=== ./app/src/main/java/ru/kayron/dew/content/ContentManager.kt ===
package ru.kayron.dew.content

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import ru.kayron.dew.Game
import ru.kayron.dew.graphics.Texture2D
import ru.kayron.dew.audio.SoundEffect
import java.io.InputStream
import java.util.Locale

class ContentManager(private val game: Game) {
    var rootDirectory: String = "Content"
        private set
    private val loadedAssets = mutableMapOf<String, Any>()
    private var assetManager: AssetManager? = null

    fun setAssetManager(am: AssetManager) {
        assetManager = am
        Texture2D.setAssetManager(am)
    }

    fun setRootDirectory(path: String) {
        rootDirectory = path.trim('/').ifEmpty { "Content" }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> load(path: String): T {
        val key = "$rootDirectory/$path"
        loadedAssets[key]?.let { return it as T }
        val asset = loadAsset(key)
        loadedAssets[key] = asset
        return asset as T
    }

    private fun loadAsset(path: String): Any {
        val lowerPath = path.lowercase(Locale.ROOT)
        if (lowerPath.endsWith(".png") || lowerPath.endsWith(".jpg") || lowerPath.endsWith(".jpeg") || lowerPath.endsWith(".bmp")) {
            val bitmap = openStream(path)?.use { BitmapFactory.decodeStream(it) }
                ?: throw RuntimeException("Texture not found: $path")
            return Texture2D.fromBitmap(bitmap)
        }
        if (lowerPath.endsWith(".wav")) {
            val stream = openStream(path) ?: throw RuntimeException("Sound not found: $path")
            return SoundEffect.fromWav(stream)
        }
        if (lowerPath.endsWith(".ogg")) {
            val stream = openStream(path) ?: throw RuntimeException("Sound not found: $path")
            return SoundEffect.fromOgg(stream)
        }
        if (lowerPath.endsWith(".txt") || lowerPath.endsWith(".json") || lowerPath.endsWith(".xml") || lowerPath.endsWith(".glsl")) {
            val stream = openStream(path) ?: throw RuntimeException("File not found: $path")
            return stream.bufferedReader().use { it.readText() }
        }
        throw RuntimeException("Unsupported asset type: $path")
    }

    private fun openStream(path: String): InputStream? {
        val am = assetManager ?: return null
        return try {
            am.open(path)
        } catch (e: Exception) {
            try {
                am.open("$rootDirectory/$path")
            } catch (e2: Exception) {
                null
            }
        }
    }

    fun <T> getLoaded(key: String): T? {
        @Suppress("UNCHECKED_CAST")
        return loadedAssets["$rootDirectory/$key"] as? T
    }

    fun unload() {
        for ((_, asset) in loadedAssets) {
            when (asset) {
                is Texture2D -> asset.dispose()
                is SoundEffect -> asset.dispose()
            }
        }
        loadedAssets.clear()
    }

    fun dispose() = unload()
}
=== ./app/src/main/java/ru/kayron/dew/GameTime.kt ===
package ru.kayron.dew

data class GameTime(
    var totalGameTime: Long = 0L,
    var elapsedGameTime: Long = 0L,
    var isRunningSlowly: Boolean = false
) {
    val totalGameTimeSeconds: Float get() = totalGameTime / 1_000_000_000f
    val elapsedGameTimeSeconds: Float get() = elapsedGameTime / 1_000_000_000f
}
=== ./app/src/main/java/ru/kayron/dew/Game.kt ===
package ru.kayron.dew

import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import ru.kayron.dew.graphics.GraphicsDevice
import ru.kayron.dew.input.*
import ru.kayron.dew.content.ContentManager
import ru.kayron.cargo.Cargo
import ru.kayron.cargo.CargoContainer

open class Game : View.OnKeyListener, View.OnTouchListener {
    val graphicsDevice = GraphicsDevice()
    val gameWindow: GameWindow = GameWindow()
    val cargo: CargoContainer = Cargo.root
    
    val graphicsDeviceManager: GraphicsDeviceManager = GraphicsDeviceManager(this)

    var isActive: Boolean = true
    var isMouseVisible: Boolean = true
    var exitRequested: Boolean = false
    private var isInitialized = false
    private var isRunning = false

    private var updateTime = GameTime()
    private var drawTime = GameTime()
    private var lastFrameTimeNanos: Long = 0L
    private var totalGameTimeNanos: Long = 0L

    val content: ContentManager = ContentManager(this)

    open fun initialize() {
        if (isInitialized) return
        graphicsDeviceManager.createDevice()
        graphicsDevice.initialize()
        
        loadContent()
        isInitialized = true
    }

    open fun loadContent() {}
    open fun unloadContent() {}

    open fun update(gameTime: GameTime) {
        
    }

    open fun draw(gameTime: GameTime) {
        
    }

    fun run() {
        isRunning = true
        lastFrameTimeNanos = System.nanoTime()
        initialize()
    }

    fun tick(): Boolean {
        if (exitRequested) return false
        val now = System.nanoTime()
        val elapsed = now - lastFrameTimeNanos
        lastFrameTimeNanos = now
        totalGameTimeNanos += elapsed

        val maxElapsed = 50_000_000L
        val clampedElapsed = minOf(elapsed, maxElapsed)

        Keyboard.update()
        Touch.update()
        GamePad.update()
        Mouse.update()

        updateTime = GameTime(totalGameTimeNanos, clampedElapsed)
        drawTime = GameTime(totalGameTimeNanos, clampedElapsed)

        update(updateTime)
        draw(drawTime)
        graphicsDevice.present()

        return true
    }

    fun exit() {
        exitRequested = true
    }

    fun resetElapsedTime() {
        lastFrameTimeNanos = System.nanoTime()
    }

    override fun onKey(v: View, keyCode: Int, event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            Keyboard.onKeyDown(keyCode)
            GamePad.onKeyDown(keyCode)
            return true
        } else if (event.action == KeyEvent.ACTION_UP) {
            Keyboard.onKeyUp(keyCode)
            GamePad.onKeyUp(keyCode)
            return true
        }
        return false
    }

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        Touch.onTouchEvent(event)
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            Mouse.onTouch(event.x, event.y)
        } else if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) {
            Mouse.onTouchUp()
            v.performClick()
        }
        return true
    }

    open fun dispose() {
        unloadContent()
        graphicsDevice.dispose()
    }
}
=== ./app/src/main/java/ru/kayron/dew/ecs/Entity.kt ===
package ru.kayron.dew.ecs

typealias Entity = Int=== ./app/src/main/java/ru/kayron/dew/ecs/Component.kt ===
package ru.kayron.dew.ecs

import ru.kayron.dew.utils.ArrayUtils

abstract class Component {
    protected var entityIds = IntArray(INITIAL_CAPACITY)
    protected var entityToIndex = IntArray(INITIAL_CAPACITY) { -1 }

    protected var size = 0

    val entitiesCount: Int
        get() = size

    fun hasEntity(entity: Entity): Boolean {
        return entity >= 0 &&
            entity < entityToIndex.size &&
            entityToIndex[entity] != -1
    }

    fun entities(): List<Entity> = entityIds
        .copyOfRange(0, size)
        .toList()

    protected fun indexOf(entity: Entity): Int {
        require(entity < entityToIndex.size) { "Entity out of bounds: $entity" }
        val idx = entityToIndex[entity]
        require(idx != -1) { "Entity $entity not found" }
        return idx
    }

    protected fun addEntity(entity: Entity): Int {
        entityIds = ArrayUtils.ensureCapacity(
            entityIds,
            size,
            ::grow
        )
        entityToIndex = ArrayUtils.ensureCapacity(
            entityToIndex,
            entity,
            ::grow
        )

        val index = size
        entityIds[index] = entity
        entityToIndex[entity] = index

        size++
        return index
    }

    fun remove(entity: Entity) {
        val index = indexOf(entity)
        val lastIndex = size - 1
        val lastEntity = entityIds[lastIndex]

        entityIds[index] = lastEntity
        entityToIndex[lastEntity] = index

        swap(index, lastIndex)

        entityToIndex[entity] = -1
        entityIds[lastIndex] = -1

        size--
    }

    fun clear() {
        for (i in 0 until size) {
            entityToIndex[entityIds[i]] = -1
            entityIds[i] = -1
        }
        size = 0
    }

    protected abstract fun grow(newSize: Int)
    protected abstract fun swap(a: Int, b: Int)
    
    companion object {
        val INITIAL_CAPACITY = 128
    }
}
=== ./app/src/main/java/ru/kayron/dew/ecs/DrawableGameSystem.kt ===
package ru.kayron.dew.ecs

import ru.kayron.dew.Game
import ru.kayron.dew.GameTime

open class DrawableGameSystem(game: Game) : GameSystem(game), IDrawable {
    override val drawOrder: Int = 0
    override var visible: Boolean = true
        set(value) {
            if (field != value) {
                field = value
            }
        }

    override fun draw(gameTime: GameTime) {}
}
=== ./app/src/main/java/ru/kayron/dew/ecs/GameSystem.kt ===
package ru.kayron.dew.ecs

import ru.kayron.dew.Game
import ru.kayron.dew.GameTime

open class GameSystem(protected val game: Game) : IGameSystem, IUpdateable {
    override var enabled: Boolean = true
    override val updateOrder: Int = 0

    override fun initialize() {}

    override fun update(gameTime: GameTime) {}
}
=== ./app/src/main/java/ru/kayron/dew/ecs/IDrawable.kt ===
package ru.kayron.dew.ecs

import ru.kayron.dew.GameTime

interface IDrawable {
    val drawOrder: Int
    var visible: Boolean
    fun draw(gameTime: GameTime)
}
=== ./app/src/main/java/ru/kayron/dew/ecs/IGameSystem.kt ===
package ru.kayron.dew.ecs

interface IGameSystem {
    fun initialize()
}
=== ./app/src/main/java/ru/kayron/dew/ecs/IUpdateable.kt ===
package ru.kayron.dew.ecs

import ru.kayron.dew.GameTime

interface IUpdateable {
    val updateOrder: Int
    var enabled: Boolean
    fun update(gameTime: GameTime)
}
=== ./app/src/main/java/ru/kayron/dew/ecs/World.kt ===
package ru.kayron.dew.ecs

import ru.kayron.dew.managers.*

class World(
    val entityManager: EntityManager,
    val componentManager: ComponentManager,
    val systemManager: SystemManager
) {
    
}=== ./app/src/main/java/ru/kayron/dew/components/PositionComponent.kt ===
package ru.kayron.dew.components

import ru.kayron.dew.ecs.Component
import ru.kayron.dew.ecs.Entity
import ru.kayron.dew.utils.swap
import ru.kayron.dew.math.Vector2

class TransformComponent : Component() {
    private var x = FloatArray(INITIAL_CAPACITY)
    private var y = FloatArray(INITIAL_CAPACITY)
    private var rotation = FloatArray(INITIAL_CAPACITY)
    private var scaleX = FloatArray(INITIAL_CAPACITY)
    private var scaleY = FloatArray(INITIAL_CAPACITY)
    
    fun add(
        entity: Entity, 
        xVal: Float = 0f, 
        yVal: Float = 0f,
        rotationVal: Float = 0f,
        scaleXVal: Float = 1f,
        scaleYVal: Float = 1f
    ) {
        val index = addEntity(entity)
        x[index] = xVal
        y[index] = yVal
        rotation[index] = rotationVal
        scaleX[index] = scaleXVal
        scaleY[index] = scaleYVal
    }
    
    fun update(
        entity: Entity, 
        xVal: Float? = null, 
        yVal: Float? = null,
        rotationVal: Float? = null,
        scaleXVal: Float? = null,
        scaleYVal: Float? = null
    ) {
        val index = indexOf(entity)
        xVal?.let { x[index] = it }
        yVal?.let { y[index] = it }
        rotationVal?.let { rotation[index] = it }
        scaleXVal?.let { scaleX[index] = it }
        scaleYVal?.let { scaleY[index] = it }
    }
    
    override fun grow(newSize: Int) {
        x = x.copyOf(newSize)
        y = y.copyOf(newSize)
        rotation = rotation.copyOf(newSize)
        scaleX = scaleX.copyOf(newSize)
        scaleY = scaleY.copyOf(newSize)
    }
    
    override fun swap(a: Int, b: Int) {
        x.swap(a, b)
        y.swap(a, b)
        rotation.swap(a, b)
        scaleX.swap(a, b)
        scaleY.swap(a, b)
    }
    
    fun x(entity: Entity) : Float = x[indexOf(entity)]
    fun y(entity: Entity) : Float = y[indexOf(entity)]
    fun pos(entity: Entity) : Vector2 = Vector2(x(entity), y(entity))
    fun rotation(entity: Entity) : Float = rotation[indexOf(entity)]
    fun scaleX(entity: Entity) : Float = scaleX[indexOf(entity)]
    fun scaleY(entity: Entity) : Float = scaleY[indexOf(entity)]
    fun scale(entity: Entity) : Vector2 = Vector2(scaleX(entity), scaleY(entity))
}=== ./app/src/main/java/ru/kayron/dew/components/VelocityComponent.kt ===
package ru.kayron.dew.components

import ru.kayron.dew.ecs.Component
import ru.kayron.dew.ecs.Entity
import ru.kayron.dew.utils.swap
import ru.kayron.dew.math.Vector2

class VelocityComponent : Component() {
    private var vx = FloatArray(INITIAL_CAPACITY)
    private var vy = FloatArray(INITIAL_CAPACITY)
    
    fun add(
        entity: Entity, 
        vxVal: Float = 0f, 
        vyVal: Float = 0f
    ) {
        val index = addEntity(entity)
        vx[index] = vxVal
        vy[index] = vyVal
    }
    
    fun update(
        entity: Entity, 
        vxVal: Float? = null, 
        vyVal: Float? = null
    ) {
        val index = indexOf(entity)
        vxVal?.let { vx[index] = it }
        vyVal?.let { vy[index] = it }
    }
    
    override fun grow(newSize: Int) {
        vx = vx.copyOf(newSize)
        vy = vy.copyOf(newSize)
    }
    
    override fun swap(a: Int, b: Int) {
        vx.swap(a, b)
        vy.swap(a, b)
    }
    
    fun vx(entity: Entity) : Float = vx[indexOf(entity)]
    fun vy(entity: Entity) : Float = vy[indexOf(entity)]
    fun vel(entity: Entity) : Vector2 = Vector2(vx(entity), vy(entity))
}=== ./app/src/main/java/ru/kayron/dew/components/CameraComponent.kt ===
package ru.kayron.dew.components

import ru.kayron.dew.ecs.Component
import ru.kayron.dew.ecs.Entity
import ru.kayron.dew.utils.swap
import ru.kayron.dew.math.Vector2

class CameraComponent : Component() {
    private var zoom = FloatArray(INITIAL_CAPACITY)
    private var viewportWidth = FloatArray(INITIAL_CAPACITY)
    private var viewportHeight = FloatArray(INITIAL_CAPACITY)
    private var active = BooleanArray(INITIAL_CAPACITY) { false }
    private var indexOfActive = -1
    
    fun add(
        entity: Entity, 
        zoomVal: Float = 1f,
        viewportWidthVal: Float = 0f, 
        viewportHeightVal: Float = 0f
    ) {
        val index = addEntity(entity)
        zoom[index] = zoomVal
        viewportWidth[index] = viewportWidthVal
        viewportHeight[index] = viewportHeightVal
    }
    
    fun update(
        entity: Entity, 
        zoomVal: Float? = null, 
        viewportWidthVal: Float? = null,
        viewportHeightVal: Float? = null
    ) {
        val index = indexOf(entity)
        zoomVal?.let { zoom[index] = it }
        viewportWidthVal?.let { viewportWidth[index] = it }
        viewportHeightVal?.let { viewportHeight[index] = it }
    }
    
    override fun grow(newSize: Int) {
        zoom = zoom.copyOf(newSize)
        viewportWidth = viewportWidth.copyOf(newSize)
        viewportHeight = viewportHeight.copyOf(newSize)
        
        val oldSize = active.size
        active = active.copyOf(newSize)
        for (i in oldSize..newSize) {
            active[i] = false
        }
    }
    
    override fun swap(a: Int, b: Int) {
        zoom.swap(a, b)
        viewportWidth.swap(a, b)
        viewportHeight.swap(a, b)
        active.swap(a, b)
    }
    
    fun zoom(entity: Entity) : Float = zoom[indexOf(entity)]
    fun viewportWidth(entity: Entity) : Float = viewportWidth[indexOf(entity)]
    fun viewportHeight(entity: Entity) : Float = viewportHeight[indexOf(entity)]
    fun viewport(entity: Entity) : Vector2 = Vector2(viewportWidth(entity), viewportHeight(entity))
    fun isActive(entity: Entity) : Boolean = active[indexOf(entity)]
    
    fun setActive(entity: Entity) {
        val index = indexOf(entity)
        active[index] = true
        
        if (indexOfActive != -1) active[indexOfActive] = false
        indexOfActive = index
    }
}=== ./app/src/main/java/ru/kayron/dew/components/SpriteComponent.kt ===
package ru.kayron.dew.components

import ru.kayron.dew.ecs.Component
import ru.kayron.dew.ecs.Entity
import ru.kayron.dew.utils.swap
import ru.kayron.dew.math.Vector2

class SpriteComponent : Component() {
    private var vx = FloatArray(INITIAL_CAPACITY)
    private var vy = FloatArray(INITIAL_CAPACITY)
    
    fun add(
        entity: Entity, 
        vxVal: Float = 0f, 
        vyVal: Float = 0f
    ) {
        val index = addEntity(entity)
        vx[index] = vxVal
        vy[index] = vyVal
    }
    
    fun update(
        entity: Entity, 
        vxVal: Float? = null, 
        vyVal: Float? = null
    ) {
        val index = indexOf(entity)
        vxVal?.let { vx[index] = it }
        vyVal?.let { vy[index] = it }
    }
    
    override fun grow(newSize: Int) {
        vx = vx.copyOf(newSize)
        vy = vy.copyOf(newSize)
    }
    
    override fun swap(a: Int, b: Int) {
        vx.swap(a, b)
        vy.swap(a, b)
    }
    
    fun vx(entity: Entity) : Float = vx[indexOf(entity)]
    fun vy(entity: Entity) : Float = vy[indexOf(entity)]
    fun vel(entity: Entity) : Vector2 = Vector2(vx(entity), vy(entity))
}=== ./app/src/main/java/ru/kayron/dew/utils/ArrayUtils.kt ===
package ru.kayron.dew.utils

class ArrayUtils {
    companion object {
        const val MAX_ARRAY_SIZE = Int.MAX_VALUE - 8
        
        fun growCapacity(old: Int, minRequired: Int): Int {
            if (old >= MAX_ARRAY_SIZE) return MAX_ARRAY_SIZE
            var newSize = old + (old shr 1) // x1.5
            if (newSize < minRequired) {
                newSize = minRequired
            }
            
            if (newSize < 0 || newSize > MAX_ARRAY_SIZE) {
                newSize = MAX_ARRAY_SIZE
            }
            return newSize
        }
        
        @JvmName("ensureCapacityInt")
        fun ensureCapacity(array: IntArray, target: Int, grow: ((Int) -> Unit)? = null) : IntArray {
            if (target >= array.size) {
                var newSize = growCapacity(array.size, target + 1)
                if (grow != null) grow(newSize)
                return array.copyOf(newSize)
            }
            return array
        }
        
        @JvmName("ensureCapacityBoolean")
        fun ensureCapacity(array: BooleanArray, target: Int, grow: ((Int) -> Unit)? = null) : BooleanArray {
            if (target >= array.size) {
                var newSize = growCapacity(array.size, target + 1)
                if (grow != null) grow(newSize)
                return array.copyOf(newSize)
            }
            return array
        }
        
        @JvmName("ensureCapacityByte")
        fun ensureCapacity(array: ByteArray, target: Int, grow: ((Int) -> Unit)? = null) : ByteArray {
            if (target >= array.size) {
                var newSize = growCapacity(array.size, target + 1)
                if (grow != null) grow(newSize)
                return array.copyOf(newSize)
            }
            return array
        }
        
        @JvmName("ensureCapacityChar")
        fun ensureCapacity(array: CharArray, target: Int, grow: ((Int) -> Unit)? = null) : CharArray {
            if (target >= array.size) {
                var newSize = growCapacity(array.size, target + 1)
                if (grow != null) grow(newSize)
                return array.copyOf(newSize)
            }
            return array
        }
        
        @JvmName("ensureCapacityShort")
        fun ensureCapacity(array: ShortArray, target: Int, grow: ((Int) -> Unit)? = null) : ShortArray {
            if (target >= array.size) {
                var newSize = growCapacity(array.size, target + 1)
                if (grow != null) grow(newSize)
                return array.copyOf(newSize)
            }
            return array
        }
        
        @JvmName("ensureCapacityLong")
        fun ensureCapacity(array: LongArray, target: Int, grow: ((Int) -> Unit)? = null) : LongArray {
            if (target >= array.size) {
                var newSize = growCapacity(array.size, target + 1)
                if (grow != null) grow(newSize)
                return array.copyOf(newSize)
            }
            return array
        }
        
        @JvmName("ensureCapacityFloat")
        fun ensureCapacity(array: FloatArray, target: Int, grow: ((Int) -> Unit)? = null) : FloatArray {
            if (target >= array.size) {
                var newSize = growCapacity(array.size, target + 1)
                if (grow != null) grow(newSize)
                return array.copyOf(newSize)
            }
            return array
        }
        
        @JvmName("ensureCapacityDouble")
        fun ensureCapacity(array: DoubleArray, target: Int, grow: ((Int) -> Unit)? = null) : DoubleArray {
            if (target >= array.size) {
                var newSize = growCapacity(array.size, target + 1)
                if (grow != null) grow(newSize)
                return array.copyOf(newSize)
            }
            return array
        }
        
        @JvmName("ensureCapacityNulls")
        fun <T> ensureCapacity(array: Array<T?>, target: Int, grow: ((Int) -> Unit)? = null) : Array<T?> {
            if (target >= array.size) {
                var newSize = growCapacity(array.size, target + 1)
                if (grow != null) grow(newSize)
                return array.copyOf(newSize)
            }
            return array
        }
    }
}

internal fun IntArray.swap(a: Int, b: Int) {
    val t = this[a]
    this[a] = this[b]
    this[b] = t
}

internal fun BooleanArray.swap(a: Int, b: Int) {
    val t = this[a]
    this[a] = this[b]
    this[b] = t
}

internal fun ByteArray.swap(a: Int, b: Int) {
    val t = this[a]
    this[a] = this[b]
    this[b] = t
}

internal fun CharArray.swap(a: Int, b: Int) {
    val t = this[a]
    this[a] = this[b]
    this[b] = t
}

internal fun ShortArray.swap(a: Int, b: Int) {
    val t = this[a]
    this[a] = this[b]
    this[b] = t
}

internal fun LongArray.swap(a: Int, b: Int) {
    val t = this[a]
    this[a] = this[b]
    this[b] = t
}

internal fun FloatArray.swap(a: Int, b: Int) {
    val t = this[a]
    this[a] = this[b]
    this[b] = t
}

internal fun DoubleArray.swap(a: Int, b: Int) {
    val t = this[a]
    this[a] = this[b]
    this[b] = t
}

internal fun <T> Array<T>.swap(a: Int, b: Int) {
    val t = this[a]
    this[a] = this[b]
    this[b] = t
}=== ./app/src/main/java/ru/kayron/dew/managers/EntityManager.kt ===
package ru.kayron.dew.managers

import ru.kayron.dew.ecs.Entity
import ru.kayron.dew.utils.ArrayUtils

class EntityManager {
    var nextId: Entity = 1
    var storage = IntArray(INITIAL_CAPACITY)
    var size = 0
    
    fun create() : Entity {
        if (size == 0) return nextId++
        return storage[--size]
    }
    
    fun remove(entity: Entity) {
        storage = ArrayUtils.ensureCapacity(
            storage,
            size,
            null
        )
        
        storage[size++] = entity
    }
    
    fun reset() {
        nextId = 1
        storage = IntArray(INITIAL_CAPACITY)
        size = 0
    }
    
    companion object {
        val INITIAL_CAPACITY = 128
    }
}=== ./app/src/main/java/ru/kayron/dew/managers/ComponentManager.kt ===
package ru.kayron.dew.managers

import ru.kayron.dew.ecs.Entity
import ru.kayron.dew.ecs.Component
import kotlin.reflect.KClass

class ComponentManager {
    private val storages = mutableMapOf<KClass<out Component>, Component>()
    
    fun <T : Component> get(type: KClass<T>): T = getOrThrow(type) as T
    
    inline fun <reified T : Component> get(): T = get(T::class)

    fun getEntitiesWith(
        type: KClass<out Component>,
        vararg otherTypes: KClass<out Component>
    ): List<Entity> {
        val queryStorages = (listOf(type) + otherTypes)
            .map { getOrThrow(it) }
            .sortedBy { it.entitiesCount }
        val smallestStorage = queryStorages.first()
        val otherStorages = queryStorages.drop(1)

        return smallestStorage.entities().filter { entity ->
            otherStorages.all { storage -> storage.hasEntity(entity) }
        }
    }

    @JvmName("getEntitiesWithOne")
    inline fun <reified T : Component> getEntitiesWith(): List<Entity> =
        getEntitiesWith(T::class)

    @JvmName("getEntitiesWithTwo")
    inline fun <
        reified T1 : Component,
        reified T2 : Component
    > getEntitiesWith(): List<Entity> =
        getEntitiesWith(T1::class, T2::class)

    @JvmName("getEntitiesWithThree")
    inline fun <
        reified T1 : Component,
        reified T2 : Component,
        reified T3 : Component
    > getEntitiesWith(): List<Entity> =
        getEntitiesWith(T1::class, T2::class, T3::class)

    @JvmName("getEntitiesWithFour")
    inline fun <
        reified T1 : Component,
        reified T2 : Component,
        reified T3 : Component,
        reified T4 : Component
    > getEntitiesWith(): List<Entity> =
        getEntitiesWith(T1::class, T2::class, T3::class, T4::class)

    @JvmName("getEntitiesWithFive")
    inline fun <
        reified T1 : Component,
        reified T2 : Component,
        reified T3 : Component,
        reified T4 : Component,
        reified T5 : Component
    > getEntitiesWith(): List<Entity> =
        getEntitiesWith(T1::class, T2::class, T3::class, T4::class, T5::class)

    @JvmName("getEntitiesWithSix")
    inline fun <
        reified T1 : Component,
        reified T2 : Component,
        reified T3 : Component,
        reified T4 : Component,
        reified T5 : Component,
        reified T6 : Component
    > getEntitiesWith(): List<Entity> =
        getEntitiesWith(T1::class, T2::class, T3::class, T4::class, T5::class, T6::class)

    @JvmName("getEntitiesWithSeven")
    inline fun <
        reified T1 : Component,
        reified T2 : Component,
        reified T3 : Component,
        reified T4 : Component,
        reified T5 : Component,
        reified T6 : Component,
        reified T7 : Component
    > getEntitiesWith(): List<Entity> =
        getEntitiesWith(T1::class, T2::class, T3::class, T4::class, T5::class, T6::class, T7::class)

    fun add(storage: Component) {
        storages[storage::class] = storage
    }
    
    fun removeEntity(entity: Entity) {
        storages.values.forEach {
            if (it.hasEntity(entity)) {
                it.remove(entity)
            }
        }
    }
    
    fun reset() {
        storages.clear()
    }

    private fun getOrThrow(type: KClass<out Component>): Component {
        return storages[type] ?: error("Storage not found: ${type.simpleName}")
    }
}
=== ./app/src/main/java/ru/kayron/dew/managers/SystemManager.kt ===
package ru.kayron.dew.managers

import ru.kayron.dew.utils.ArrayUtils
import ru.kayron.dew.ecs.IGameSystem
import ru.kayron.dew.ecs.IUpdateable
import ru.kayron.dew.ecs.IDrawable

class SystemManager {
    var updateSystems: Array<IUpdateable?> = arrayOfNulls<IUpdateable>(INITIAL_CAPACITY)
    var drawSystems: Array<IDrawable?> = arrayOfNulls<IDrawable>(INITIAL_CAPACITY)
    
    var updateSize = 0
    var drawSize = 0
    
    fun add(system: IGameSystem) {
        if (system is IUpdateable) {
            updateSystems = ArrayUtils.ensureCapacity(
                updateSystems,
                updateSize,
                null
            )
            updateSystems[updateSize++] = system
        }
        if (system is IDrawable) {
            drawSystems = ArrayUtils.ensureCapacity(
                drawSystems,
                drawSize,
                null
            )
            drawSystems[drawSize++] = system
        }
    }
    
    fun sort() {
        updateSystems.sortBy { it?.updateOrder }
        drawSystems.sortBy { it?.drawOrder }
    }
    
    companion object {
        val INITIAL_CAPACITY = 8
    }
}=== ./app/src/main/java/ru/kayron/dew/managers/SceneManager.kt ===
package ru.kayron.dew.managers

class SceneManager {
    
}=== ./app/src/main/java/ru/kayron/dew/IGraphicsDeviceService.kt ===
package ru.kayron.dew

import ru.kayron.dew.graphics.GraphicsDevice

interface IGraphicsDeviceService {
    val graphicsDevice: GraphicsDevice
}
=== ./app/src/main/java/ru/kayron/dew/IGraphicsDeviceManager.kt ===
package ru.kayron.dew

interface IGraphicsDeviceManager {
    fun createDevice()
    fun applyChanges()
}
=== ./app/src/main/java/ru/kayron/dew/GraphicsDeviceManager.kt ===
package ru.kayron.dew

import ru.kayron.dew.graphics.*
import ru.kayron.dew.graphics.PresentationParameters.DepthFormat
import ru.kayron.dew.graphics.PresentationParameters.DisplayOrientation
import ru.kayron.dew.graphics.PresentationParameters.PresentInterval

class GraphicsDeviceManager(private val game: Game) : IGraphicsDeviceManager, IGraphicsDeviceService {

    override val graphicsDevice: GraphicsDevice get() = game.graphicsDevice

    var preferredBackBufferWidth: Int = 0
    var preferredBackBufferHeight: Int = 0
    var preferredBackBufferFormat: SurfaceFormat = SurfaceFormat.Color
    var preferredDepthStencilFormat: DepthFormat = DepthFormat.Depth24Stencil8
    var preferredMultiSampleCount: Int = 0
    var synchronizedWithVerticalRetrace: Boolean = true
    var isFullScreen: Boolean = false

    private var initialized = false

    override fun createDevice() {
        if (preferredBackBufferWidth <= 0) preferredBackBufferWidth = 1920
        if (preferredBackBufferHeight <= 0) preferredBackBufferHeight = 1080

        val pp = graphicsDevice.presentationParameters
        pp.backBufferWidth = preferredBackBufferWidth
        pp.backBufferHeight = preferredBackBufferHeight
        pp.backBufferFormat = preferredBackBufferFormat
        pp.depthStencilFormat = preferredDepthStencilFormat
        pp.multiSampleCount = preferredMultiSampleCount
        pp.presentationInterval = if (synchronizedWithVerticalRetrace) PresentInterval.Default else PresentInterval.Immediate
        pp.isFullScreen = isFullScreen
        pp.displayOrientation = DisplayOrientation.LandscapeLeft

        graphicsDevice.setViewport(0, 0, preferredBackBufferWidth, preferredBackBufferHeight)

        game.cargo.addSingleton<IGraphicsDeviceService>(this)

        initialized = true
    }

    override fun applyChanges() {
        if (!initialized) createDevice()
        val pp = graphicsDevice.presentationParameters
        pp.backBufferWidth = preferredBackBufferWidth
        pp.backBufferHeight = preferredBackBufferHeight
        pp.isFullScreen = isFullScreen
        graphicsDevice.setViewport(0, 0, preferredBackBufferWidth, preferredBackBufferHeight)
    }

    fun toggleFullScreen() {
        isFullScreen = !isFullScreen
        applyChanges()
    }

    companion object {
        const val DefaultBackBufferWidth = 1920
        const val DefaultBackBufferHeight = 1080
    }
}
=== ./app/src/main/java/ru/kayron/dew/GameWindow.kt ===
package ru.kayron.dew

import android.view.View
import ru.kayron.dew.math.Rectangle
import ru.kayron.dew.graphics.PresentationParameters.DisplayOrientation

open class GameWindow {
    var clientBounds: Rectangle = Rectangle()
        protected set
    var currentOrientation: DisplayOrientation = DisplayOrientation.Default
    var view: View? = null
        internal set
    var allowUserResizing: Boolean = false

    fun setClientBounds(width: Int, height: Int) {
        clientBounds = Rectangle(0, 0, width, height)
    }

    companion object {
        fun create(view: View, width: Int, height: Int): GameWindow {
            val w = GameWindow()
            w.view = view
            w.setClientBounds(width, height)
            return w
        }
    }
}
=== ./app/src/main/java/ru/kayron/dew/DewGameView.kt ===
package ru.kayron.dew

import android.annotation.SuppressLint
import android.content.Context
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.view.InputDevice
import android.view.MotionEvent
import android.view.KeyEvent
import ru.kayron.dew.input.GamePad
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

@SuppressLint("ViewConstructor")
class DewGameView(
    context: Context,
    private val game: Game
) : GLSurfaceView(context), GLSurfaceView.Renderer {

    private var surfaceWidth = 0
    private var surfaceHeight = 0
    private var initialized = false

    init {
        setEGLContextClientVersion(3)
        setRenderer(this)
        renderMode = RENDERMODE_CONTINUOUSLY

        setOnKeyListener(game)
        setOnTouchListener(game)

        isFocusable = true
        isFocusableInTouchMode = true
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES30.glClearColor(0f, 0f, 0f, 1f)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        surfaceWidth = width
        surfaceHeight = height
        game.gameWindow.setClientBounds(width, height)
        game.graphicsDevice.presentationParameters.backBufferWidth = width
        game.graphicsDevice.presentationParameters.backBufferHeight = height
        game.graphicsDevice.setViewport(0, 0, width, height)

        if (!initialized) {
            game.run()
            initialized = true
        }
    }

    override fun onDrawFrame(gl: GL10?) {
        if (initialized) {
            val alive = game.tick()
            if (!alive) {
                (context as? DewActivity)?.finish()
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        game.onTouch(this, event)
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        val isJoystick = event.source and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK
        if (isJoystick && event.action == MotionEvent.ACTION_MOVE) {
            GamePad.onJoystickMotion(event)
            return true
        }
        return super.onGenericMotionEvent(event)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        game.onKey(this, keyCode, event)
        return true
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        game.onKey(this, keyCode, event)
        return true
    }

    fun onPauseGame() {
        game.isActive = false
        onPause()
    }

    fun onResumeGame() {
        game.isActive = true
        onResume()
    }
}
=== ./app/src/main/java/ru/kayron/dew/DewActivity.kt ===
package ru.kayron.dew

import android.os.Bundle
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import ru.kayron.dew.input.GamePad

abstract class DewActivity : AppCompatActivity() {
    protected abstract fun createGame(): Game

    private var gameView: DewGameView? = null
    private var game: Game? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        hideSystemUi()
        GamePad.initialize(this)
        val createdGame = createGame()
        game = createdGame
        createdGame.content.setAssetManager(assets)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                game?.exit()
            }
        })
        gameView = DewGameView(this, createdGame)
        setContentView(gameView)
        gameView?.requestFocus()
    }

    private fun hideSystemUi() {
        window.insetsController?.let { c ->
            c.hide(WindowInsets.Type.systemBars())
            c.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemUi()
    }

    override fun onPause() {
        gameView?.onPauseGame()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        gameView?.onResumeGame()
    }

    override fun onDestroy() {
        game?.dispose()
        super.onDestroy()
    }
}
=== ./app/src/main/java/ru/kayron/backside/BacksideGame.kt ===
package ru.kayron.backside

import android.graphics.Canvas
import android.graphics.Paint
import androidx.core.graphics.createBitmap
import ru.kayron.dew.Game
import ru.kayron.dew.GameTime
import ru.kayron.dew.graphics.DepthStencilState
import ru.kayron.dew.graphics.RasterizerState
import ru.kayron.dew.graphics.SpriteBatch
import ru.kayron.dew.graphics.SpriteFont
import ru.kayron.dew.graphics.Texture2D
import ru.kayron.dew.input.Touch
import ru.kayron.dew.input.TouchLocation
import ru.kayron.dew.input.Keyboard
import ru.kayron.dew.input.Keys
import ru.kayron.dew.math.*

open class BacksideGame : Game() {

    private lateinit var batch: SpriteBatch

    override fun loadContent() {
        batch = SpriteBatch(graphicsDevice)
    }
    
    override fun initialize() {
        
        super.initialize()
    }

    override fun update(gameTime: GameTime) {
        if (Keyboard.getState().isKeyDown(Keys.Escape)) {
            exit()
        }
        
        super.update(gameTime)
    }

    override fun draw(gameTime: GameTime) {
        graphicsDevice.clear(Color.CornflowerBlue)

        batch.begin(
            depthStencilState = DepthStencilState.None,
            rasterizerState = RasterizerState.CullNone
        )

        onDraw(batch)
        batch.end()
        
        super.draw(gameTime)
    }

    protected open fun onTouch(position: Vector2, state: TouchLocation.TouchLocationState) {}
    protected open fun onDraw(batch: SpriteBatch) {}

    override fun dispose() {
        batch.dispose()
        super.dispose()
    }
}
=== ./app/src/main/java/ru/kayron/backside/MainActivity.kt ===
package ru.kayron.backside

import ru.kayron.dew.DewActivity
import ru.kayron.dew.Game

class MainActivity : DewActivity() {
    override fun createGame(): Game = BacksideGame()
}
=== ./app/src/main/java/ru/kayron/cargo/Cargo.kt ===
package ru.kayron.cargo

object Cargo {
    val root = CargoContainer()
}=== ./app/src/main/java/ru/kayron/cargo/Lifetime.kt ===
package ru.kayron.cargo

enum class Lifetime {
    SINGLETON,
    SCOPED,
    TRANSIENT
}=== ./app/src/main/java/ru/kayron/cargo/Named.kt ===
package ru.kayron.cargo

@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class Named(
    val value: String
)=== ./app/src/main/java/ru/kayron/cargo/Provider.kt ===
package ru.kayron.cargo

fun interface Provider<T> {
    fun get(): T
}=== ./app/src/main/java/ru/kayron/cargo/Disposable.kt ===
package ru.kayron.cargo

interface Disposable {
    fun dispose()
}=== ./app/src/main/java/ru/kayron/cargo/Initializable.kt ===
package ru.kayron.cargo

interface Initializable {
    fun initialize()
}=== ./app/src/main/java/ru/kayron/cargo/Configuration.kt ===
package ru.kayron.cargo

interface Configuration {
    val name: String
}=== ./app/src/main/java/ru/kayron/cargo/TypeKey.kt ===
package ru.kayron.cargo

import kotlin.reflect.KClass
import kotlin.reflect.KType

sealed class TypeKey {

    abstract val raw: KClass<*>

    data class Simple(
        override val raw: KClass<*>
    ) : TypeKey()

    data class Generic(
        override val raw: KClass<*>,
        val arguments: List<TypeKey>
    ) : TypeKey()

    companion object {

        fun from(type: KType): TypeKey {

            val classifier =
                type.classifier as? KClass<*>
                    ?: error("Unsupported type: $type")

            val arguments =
                type.arguments
                    .mapNotNull { it.type }
                    .map { from(it) }

            return if (arguments.isEmpty()) {
                Simple(classifier)
            } else {
                Generic(classifier, arguments)
            }
        }
    }
}=== ./app/src/main/java/ru/kayron/cargo/FactoryDelegate.kt ===
package ru.kayron.cargo

fun interface FactoryDelegate {
    fun create(container: CargoContainer): Any
}=== ./app/src/main/java/ru/kayron/cargo/RegistrationKey.kt ===
package ru.kayron.cargo

internal data class RegistrationKey(
    val type: TypeKey,
    val qualifier: String?
)=== ./app/src/main/java/ru/kayron/cargo/ParameterInfo.kt ===
package ru.kayron.cargo

import kotlin.reflect.KParameter

internal data class ParameterInfo(
    val parameter: KParameter,
    val type: TypeKey,
    val qualifier: String?,
    val lazy: Boolean,
    val provider: Boolean,
    val nullable: Boolean
)=== ./app/src/main/java/ru/kayron/cargo/ConstructorInfo.kt ===
package ru.kayron.cargo

import kotlin.reflect.KFunction

internal data class ConstructorInfo(
    val constructor: KFunction<*>,
    val parameters: List<ParameterInfo>
)=== ./app/src/main/java/ru/kayron/cargo/Registration.kt ===
package ru.kayron.cargo

internal data class Registration(
    val abstraction: TypeKey,
    val implementation: TypeKey?,
    val factory: FactoryDelegate?,
    val lifetime: Lifetime,
    val qualifier: String?,
    val eager: Boolean
)=== ./app/src/main/java/ru/kayron/cargo/CargoModule.kt ===
package ru.kayron.cargo

import kotlin.reflect.KClass

class CargoModule {

    internal val registrations =
        mutableListOf<(CargoContainer) -> Unit>()

    fun <T : Any> singleton(
        type: KClass<T>,
        qualifier: String? = null,
        factory: CargoContainer.() -> T
    ) {

        registrations += {
            it.addSingleton(
                type,
                qualifier,
                factory
            )
        }
    }

    inline fun <reified T : Any> singleton(
        qualifier: String? = null,
        noinline factory: CargoContainer.() -> T
    ) {
        singleton(
            T::class,
            qualifier,
            factory
        )
    }

    fun <T : Any> scoped(
        type: KClass<T>,
        qualifier: String? = null,
        factory: CargoContainer.() -> T
    ) {

        registrations += {
            it.addScoped(
                type,
                qualifier,
                factory
            )
        }
    }

    inline fun <reified T : Any> scoped(
        qualifier: String? = null,
        noinline factory: CargoContainer.() -> T
    ) {
        scoped(
            T::class,
            qualifier,
            factory
        )
    }

    fun <T : Any> transient(
        type: KClass<T>,
        qualifier: String? = null,
        factory: CargoContainer.() -> T
    ) {

        registrations += {
            it.addTransient(
                type,
                qualifier,
                factory
            )
        }
    }

    inline fun <reified T : Any> transient(
        qualifier: String? = null,
        noinline factory: CargoContainer.() -> T
    ) {
        transient(
            T::class,
            qualifier,
            factory
        )
    }
}

fun module(
    block: CargoModule.() -> Unit
): CargoModule {

    return CargoModule().apply(block)
}=== ./app/src/main/java/ru/kayron/cargo/CargoContainer.kt ===
package ru.kayron.cargo

import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

class CargoContainer(
    private val parent: CargoContainer? = null
) : AutoCloseable {

    private val registrations: ConcurrentHashMap<RegistrationKey, Registration>
        get() = root()._registrations

    private val _registrations =
        ConcurrentHashMap<RegistrationKey, Registration>()

    private val singletonInstances: ConcurrentHashMap<RegistrationKey, Any>
        get() = root()._singletonInstances

    private val _singletonInstances =
        ConcurrentHashMap<RegistrationKey, Any>()

    private val scopedInstances =
        ConcurrentHashMap<RegistrationKey, Any>()

    private val constructorCache =
        ConcurrentHashMap<KClass<*>, ConstructorInfo>()

    private val resolvingStack =
        ThreadLocal.withInitial {
            ArrayDeque<TypeKey>()
        }

    private var frozen = false

    fun load(module: CargoModule) {

        ensureMutable()

        module.registrations.forEach {
            it(this)
        }
    }

    fun freeze() {
        frozen = true
    }

    private fun ensureMutable() {

        check(!frozen) {
            "Container is frozen"
        }
    }

    fun scope(): CargoContainer {
        return CargoContainer(this)
    }

    fun <T : Configuration> addConfig(
        type: KClass<T>,
        config: T
    ) {
        addSingleton(
            type = type,
            qualifier = config.name
        ) {
            config
        }
    }

    inline fun <reified T : Configuration> addConfig(
        config: T
    ) {
        addConfig(
            T::class,
            config
        )
    }

    fun <T : Any> addSingleton(
        type: KClass<T>,
        qualifier: String? = null,
        factory: CargoContainer.() -> T
    ) {

        register(
            abstraction = TypeKey.Simple(type),
            implementation = null,
            qualifier = qualifier,
            lifetime = Lifetime.SINGLETON,
            factory = FactoryDelegate {
                factory(it)
            }
        )
    }

    inline fun <reified T : Any> addSingleton(
        qualifier: String? = null,
        noinline factory: CargoContainer.() -> T
    ) {
        addSingleton(
            T::class,
            qualifier,
            factory
        )
    }

    fun <T : Any> addSingleton(
        type: KClass<T>,
        instance: T
    ) {
        register(
            abstraction = TypeKey.Simple(type),
            implementation = null,
            qualifier = null,
            lifetime = Lifetime.SINGLETON,
            factory = FactoryDelegate { instance }
        )
    }

    inline fun <reified T : Any> addSingleton(
        instance: T
    ) {
        addSingleton(T::class, instance)
    }

    fun <T : Any> addScoped(
        type: KClass<T>,
        qualifier: String? = null,
        factory: CargoContainer.() -> T
    ) {

        register(
            abstraction = TypeKey.Simple(type),
            implementation = null,
            qualifier = qualifier,
            lifetime = Lifetime.SCOPED,
            factory = FactoryDelegate {
                factory(it)
            }
        )
    }

    inline fun <reified T : Any> addScoped(
        qualifier: String? = null,
        noinline factory: CargoContainer.() -> T
    ) {
        addScoped(
            T::class,
            qualifier,
            factory
        )
    }

    fun <T : Any> addTransient(
        type: KClass<T>,
        qualifier: String? = null,
        factory: CargoContainer.() -> T
    ) {

        register(
            abstraction = TypeKey.Simple(type),
            implementation = null,
            qualifier = qualifier,
            lifetime = Lifetime.TRANSIENT,
            factory = FactoryDelegate {
                factory(it)
            }
        )
    }

    inline fun <reified T : Any> addTransient(
        qualifier: String? = null,
        noinline factory: CargoContainer.() -> T
    ) {
        addTransient(
            T::class,
            qualifier,
            factory
        )
    }

    fun <
            TAbstraction : Any,
            TImplementation : TAbstraction
            > bindSingleton(
        abstraction: KClass<TAbstraction>,
        implementation: KClass<TImplementation>,
        qualifier: String? = null
    ) {

        register(
            abstraction = TypeKey.Simple(abstraction),
            implementation = TypeKey.Simple(implementation),
            qualifier = qualifier,
            lifetime = Lifetime.SINGLETON,
            factory = null
        )
    }

    inline fun <
            reified TAbstraction : Any,
            reified TImplementation : TAbstraction
            > bindSingleton(
        qualifier: String? = null
    ) {
        bindSingleton(
            TAbstraction::class,
            TImplementation::class,
            qualifier
        )
    }

    fun <
            TAbstraction : Any,
            TImplementation : TAbstraction
            > bindScoped(
        abstraction: KClass<TAbstraction>,
        implementation: KClass<TImplementation>,
        qualifier: String? = null
    ) {

        register(
            abstraction = TypeKey.Simple(abstraction),
            implementation = TypeKey.Simple(implementation),
            qualifier = qualifier,
            lifetime = Lifetime.SCOPED,
            factory = null
        )
    }

    inline fun <
            reified TAbstraction : Any,
            reified TImplementation : TAbstraction
            > bindScoped(
        qualifier: String? = null
    ) {
        bindScoped(
            TAbstraction::class,
            TImplementation::class,
            qualifier
        )
    }

    fun <
            TAbstraction : Any,
            TImplementation : TAbstraction
            > bindTransient(
        abstraction: KClass<TAbstraction>,
        implementation: KClass<TImplementation>,
        qualifier: String? = null
    ) {

        register(
            abstraction = TypeKey.Simple(abstraction),
            implementation = TypeKey.Simple(implementation),
            qualifier = qualifier,
            lifetime = Lifetime.TRANSIENT,
            factory = null
        )
    }

    inline fun <
            reified TAbstraction : Any,
            reified TImplementation : TAbstraction
            > bindTransient(
        qualifier: String? = null
    ) {
        bindTransient(
            TAbstraction::class,
            TImplementation::class,
            qualifier
        )
    }

    private fun register(
        abstraction: TypeKey,
        implementation: TypeKey?,
        qualifier: String?,
        lifetime: Lifetime,
        factory: FactoryDelegate?
    ) {

        ensureMutable()

        val key = RegistrationKey(
            abstraction,
            qualifier
        )

        registrations[key] = Registration(
            abstraction = abstraction,
            implementation = implementation,
            factory = factory,
            lifetime = lifetime,
            qualifier = qualifier,
            eager = false
        )
    }

    fun build() {

        validateGraph()

        registrations.values
            .filter {
                it.lifetime == Lifetime.SINGLETON &&
                        it.eager
            }
            .forEach {
                resolveByKey(
                    RegistrationKey(
                        it.abstraction,
                        it.qualifier
                    )
                )
            }

        freeze()
    }

    fun <T : Any> get(
        type: KClass<T>,
        qualifier: String? = null
    ): T {

        val key = RegistrationKey(
            TypeKey.Simple(type),
            qualifier
        )

        return resolveByKey(key) as T
    }

    inline fun <reified T : Any> get(
        qualifier: String? = null
    ): T {
        return get(
            T::class,
            qualifier
        )
    }

    fun <T : Any> create(
        type: KClass<T>,
        vararg arguments: Any
    ): T {

        return instantiate(
            type,
            arguments.toList()
        )
    }

    inline fun <reified T : Any> create(
        vararg arguments: Any
    ): T {
        return create(
            T::class,
            *arguments
        )
    }

    private fun resolveByKey(
        key: RegistrationKey
    ): Any {

        singletonInstances[key]?.let {
            return it
        }

        scopedInstances[key]?.let {
            return it
        }

        val registration =
            registrations[key]
                ?: error(
                    "No registration for ${key.type}"
                )

        detectCircularDependency(key.type)

        try {

            val instance = when {

                registration.factory != null -> {
                    registration.factory.create(this)
                }

                registration.implementation != null -> {

                    val impl =
                        registration.implementation
                                as TypeKey.Simple

                    instantiate(
                        impl.raw,
                        emptyList()
                    )
                }

                else -> {
                    error("Invalid registration")
                }
            }

            when (registration.lifetime) {

                Lifetime.SINGLETON -> {

                    singletonInstances
                        .putIfAbsent(key, instance)

                    return singletonInstances[key]!!
                }

                Lifetime.SCOPED -> {

                    scopedInstances
                        .putIfAbsent(key, instance)

                    return scopedInstances[key]!!
                }

                Lifetime.TRANSIENT -> {
                    return instance
                }
            }

        } finally {
            resolvingStack.get().removeLast()
        }
    }

    private fun <T : Any> instantiate(
        type: KClass<T>,
        external: List<Any>
    ): T {

        val ctorInfo =
            constructorCache.getOrPut(type) {
                buildConstructorInfo(type)
            }

        val args =
            ctorInfo.parameters.associate {

                val value =
                    resolveParameter(
                        it,
                        external
                    )

                it.parameter to value
            }

        val instance =
            ctorInfo.constructor.callBy(args) as T

        if (instance is Initializable) {
            instance.initialize()
        }

        return instance
    }

    private fun buildConstructorInfo(
        type: KClass<*>
    ): ConstructorInfo {

        val ctor =
            type.primaryConstructor
                ?: type.constructors.singleOrNull()
                ?: error(
                    "No valid constructor for ${type.qualifiedName}"
                )

        val parameters =
            ctor.parameters.map {

                val qualifier =
                    it.annotations
                        .filterIsInstance<Named>()
                        .firstOrNull()
                        ?.value

                val typeKey =
                    TypeKey.from(it.type)

                ParameterInfo(
                    parameter = it,
                    type = typeKey,
                    qualifier = qualifier,
                    lazy =
                        (typeKey as? TypeKey.Simple)
                            ?.raw == Lazy::class,
                    provider =
                        (typeKey as? TypeKey.Simple)
                            ?.raw == Provider::class,
                    nullable = it.type.isMarkedNullable
                )
            }

        return ConstructorInfo(
            constructor = ctor,
            parameters = parameters
        )
    }

    private fun resolveParameter(
        parameter: ParameterInfo,
        external: List<Any>
    ): Any? {

        val raw =
            parameter.type.raw

        external.firstOrNull {
            raw.java.isAssignableFrom(it::class.java)
        }?.let {
            return it
        }

        if (parameter.lazy) {

            val generic =
                (parameter.type as TypeKey.Generic)
                    .arguments
                    .first()

            return lazy {
                resolveTypeKey(
                    generic,
                    parameter.qualifier
                )
            }
        }

        if (parameter.provider) {

            val generic =
                (parameter.type as TypeKey.Generic)
                    .arguments
                    .first()

            return Provider {
                resolveTypeKey(
                    generic,
                    parameter.qualifier
                )
            }
        }

        return try {

            resolveTypeKey(
                parameter.type,
                parameter.qualifier
            )

        } catch (_: Throwable) {

            if (parameter.nullable) {
                null
            } else {
                throw error(
                    "Failed to resolve ${parameter.type}"
                )
            }
        }
    }

    private fun resolveTypeKey(
        type: TypeKey,
        qualifier: String?
    ): Any {

        return resolveByKey(
            RegistrationKey(type, qualifier)
        )
    }

    private fun validateGraph() {

        registrations.values.forEach {

            if (
                it.lifetime == Lifetime.SINGLETON
            ) {

                validateSingletonDependencies(it)
            }
        }
    }

    private fun validateSingletonDependencies(
        registration: Registration
    ) {

        val implementation =
            registration.implementation
                ?: return

        val impl = implementation as TypeKey.Simple

        val ctorInfo =
            constructorCache.getOrPut(impl.raw) {
                buildConstructorInfo(impl.raw)
            }

        ctorInfo.parameters.forEach {

            val dependency =
                registrations[
                        RegistrationKey(
                            it.type,
                            it.qualifier
                        )
                ] ?: return@forEach

            if (
                dependency.lifetime == Lifetime.SCOPED
            ) {

                error(
                    "Singleton ${impl.raw.simpleName} depends on scoped ${it.type.raw.simpleName}"
                )
            }
        }
    }

    private fun detectCircularDependency(
        type: TypeKey
    ) {

        val stack = resolvingStack.get()

        if (type in stack) {

            val chain =
                (stack + type)
                    .joinToString(" -> ") {
                        it.raw.simpleName ?: "Unknown"
                    }

            error(
                "Circular dependency detected:\n$chain"
            )
        }

        stack.addLast(type)
    }

    private fun root(): CargoContainer {

        var current = this

        while (current.parent != null) {
            current = current.parent!!
        }

        return current
    }

    override fun close() {
        dispose()
    }

    fun dispose() {

        scopedInstances.values
            .reversed()
            .forEach {

                when (it) {

                    is AutoCloseable -> {
                        it.close()
                    }

                    is Disposable -> {
                        it.dispose()
                    }
                }
            }

        scopedInstances.clear()

        if (parent == null) {
            singletonInstances.values
                .reversed()
                .forEach {

                    when (it) {

                        is AutoCloseable -> {
                            it.close()
                        }

                        is Disposable -> {
                            it.dispose()
                        }
                    }
                }

            singletonInstances.clear()
        }
    }
}
