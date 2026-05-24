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
