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
