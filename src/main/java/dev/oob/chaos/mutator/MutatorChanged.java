package dev.oob.chaos.mutator;

/** Domain event: a mutator started or wore off. */
public record MutatorChanged(MutatorState state) {
}
