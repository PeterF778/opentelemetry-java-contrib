/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.sampler.consistent56;

import static io.opentelemetry.contrib.sampler.consistent56.ConsistentSamplingUtil.getInvalidThreshold;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.data.LinkData;
import java.util.List;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

/**
 * A consistent sampler that uses Span categorization and uses a different delegate sampler for each
 * category. Categorization of Spans is aided by Predicates, which can be combined with
 * ComposableSamplers into PredicatedSamplers.
 */
@Immutable
final class ConsistentRuleBasedSampler extends ConsistentSampler {

  @Nullable private final SpanKind spanKindToMatch;
  private final PredicatedSampler[] samplers;

  private final String description;

  /**
   * Constructs a new consistent rule based sampler using the given sequence of Predicates and
   * delegate Samplers.
   *
   * @param spanKindToMatch the SpanKind for which the Sampler applies, null value indicates all
   *     SpanKinds
   * @param samplers the PredicatedSamplers to evaluate and query
   */
  public static ConsistentRuleBasedSampler create(
      @Nullable SpanKind spanKindToMatch, PredicatedSampler... samplers) {
    return new ConsistentRuleBasedSampler(spanKindToMatch, samplers);
  }

  private ConsistentRuleBasedSampler(
      @Nullable SpanKind spanKindToMatch, PredicatedSampler... samplers) {
    this.spanKindToMatch = spanKindToMatch;
    this.samplers = samplers;

    StringBuilder builder = new StringBuilder("ConsistentRuleBasedSampler{");
    builder.append("span_kind=" + spanKindToMatch);
    for (PredicatedSampler delegate : samplers) {
      builder.append(",");
      builder.append(delegate.getSampler().getDescription());
    }
    builder.append("}");
    this.description = builder.toString();
  }

  @Override
  public SamplingIntent getSamplingIntent(
      Context parentContext,
      String name,
      SpanKind spanKind,
      Attributes attributes,
      List<LinkData> parentLinks) {

    if (spanKindToMatch == null || spanKindToMatch == spanKind) {
      for (PredicatedSampler delegate : samplers) {
        if (delegate
            .getPredicate()
            .spanMatches(parentContext, name, spanKind, attributes, parentLinks)) {
          return delegate
              .getSampler()
              .getSamplingIntent(parentContext, name, spanKind, attributes, parentLinks);
        }
      }
    }

    return () -> getInvalidThreshold();
  }

  @Override
  public String getDescription() {
    return description;
  }
}
