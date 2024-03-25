package com.arcadia.tahoe.dsl;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.function.Predicate;

public enum Maybe {
  CONTAINS,
  SHOULD_BE,
  SHOULD_NOT_BE,
  IS_NOT,
  IS,
  HAS_NOT,
  HAS,
  HAVE,
  HAVE_NOT,
  ARE,
  ARE_NOT;

  public @Nonnull Predicate<Boolean> predicate() {
    return Predicate.isEqual(
        Predicate.isEqual(IS)
            .or(Predicate.isEqual(ARE))
            .or(Predicate.isEqual(SHOULD_BE))
            .or(Predicate.isEqual(HAS))
            .or(Predicate.isEqual(HAVE))
            .or(Predicate.isEqual(CONTAINS))
            .test(this));
  }

  public @Nonnull Boolean plural() {
    return this.equals(ARE) || this.equals(HAVE);
  }

  public @Nonnull Boolean yes() {
    return this.predicate().test(true);
  }

  public @Nonnull Boolean no() {
    return this.predicate().test(false);
  }

  public @Nonnull Optional<Boolean> optional() {
    return Optional.of(this.yes());
  }
}
