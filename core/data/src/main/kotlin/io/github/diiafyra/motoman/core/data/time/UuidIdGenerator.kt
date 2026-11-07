package io.github.diiafyra.motoman.core.data.time

import io.github.diiafyra.motoman.core.domain.util.IdGenerator
import java.util.UUID
import javax.inject.Inject

internal class UuidIdGenerator
    @Inject
    constructor() : IdGenerator {
        override fun newId(): String = UUID.randomUUID().toString()
    }
