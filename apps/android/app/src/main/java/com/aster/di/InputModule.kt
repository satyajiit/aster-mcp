package com.aster.di

import com.aster.service.input.AccessibilityInputBackend
import com.aster.service.input.InputBackend
import com.aster.service.input.InputRouter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * The key-press backend chain.
 *
 * ONE binding style, deliberately, mirroring [ModeModule.provideCommandHandlers]:
 * a single `@Provides` assembles the ordered list AND the router that walks it.
 * The two alternatives are both traps —
 *  - an `@Inject` constructor on a backend *plus* a `@Provides` for that same
 *    backend is a duplicate-binding compile error; and
 *  - injecting a bare `List<InputBackend>` has no binding at all, and in Kotlin
 *    would additionally need `@JvmSuppressWildcards` to even match Dagger's
 *    `List<InputBackend>` (Kotlin compiles the parameter to `List<? extends
 *    InputBackend>`).
 * Assembling the list by hand sidesteps both and keeps the ORDER visible in one
 * readable place, which is the part that carries meaning.
 */
@Module
@InstallIn(SingletonComponent::class)
object InputModule {

    /**
     * Backend ORDER is the policy: least-privileged and cheapest first, so a key
     * that the accessibility API can genuinely express never escalates to
     * anything heavier.
     *
     * Today there is exactly one backend, so the router is a pass-through — but a
     * correct one, with the fall-through asymmetry and the reporting already in
     * place. A second backend is one more element in this list and no change
     * anywhere else.
     */
    @Provides
    @Singleton
    fun provideInputRouter(): InputRouter = InputRouter(
        listOf<InputBackend>(
            AccessibilityInputBackend(),
        )
    )
}
