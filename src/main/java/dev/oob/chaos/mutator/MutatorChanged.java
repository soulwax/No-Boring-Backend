package dev.oob.chaos.mutator;

import dev.oob.chaos.i18n.Localized;

/** Domain event: a mutator started or wore off. */
public record MutatorChanged(Localized<MutatorState> state) {
}
