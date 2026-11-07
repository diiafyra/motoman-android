package io.github.diiafyra.motoman.core.domain.util

/**
 * Creates record ids on the device. Client-generated ids let a record keep its id when it is later
 * synced, so an upload can be retried without creating a duplicate.
 */
public fun interface IdGenerator {
    public fun newId(): String
}
