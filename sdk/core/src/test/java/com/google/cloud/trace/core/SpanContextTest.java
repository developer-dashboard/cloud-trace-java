// Copyright 2016 Google Inc. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.cloud.trace.core;

import static com.google.common.truth.Truth.assertThat;

import java.math.BigInteger;
import org.junit.Test;

public class SpanContextTest {
  private static final SpanContext first =
      new SpanContext(new TraceId(BigInteger.valueOf(10)), new SpanId(30), new TraceOptions(0));
  private static final SpanContext second =
      new SpanContext(new TraceId(BigInteger.valueOf(20)), new SpanId(40), new TraceOptions(1));

  @Test
  public void testGetTraceId() {
    assertThat(first.getTraceId()).isEqualTo(new TraceId(BigInteger.valueOf(10)));
    assertThat(second.getTraceId()).isEqualTo(new TraceId(BigInteger.valueOf(20)));
  }

  @Test
  public void testGetSpanId() {
    assertThat(first.getSpanId()).isEqualTo(new SpanId(30));
    assertThat(second.getSpanId()).isEqualTo(new SpanId(40));
  }

  @Test
  public void testTraceOptions() {
    assertThat(first.getTraceOptions()).isEqualTo(new TraceOptions(0));
    assertThat(second.getTraceOptions()).isEqualTo(new TraceOptions(1));
  }

  @Test
  public void testOverrideOptions() {
    assertThat(first.overrideOptions(new TraceOptions(3))).isEqualTo(
        new SpanContext(new TraceId(BigInteger.valueOf(10)), new SpanId(30), new TraceOptions(3)));
  }

  @Test
  public void testEquals() {
    assertThat(first).isEqualTo(first);
    assertThat(first).isNotEqualTo(second);
  }

  @Test
  public void testHashCode() {
    assertThat(first.hashCode()).isEqualTo(first.hashCode());
    assertThat(first.hashCode()).isNotEqualTo(second.hashCode());
  }

  @Test
  public void testToString() {
    assertThat(first.toString()).isEqualTo(
        "SpanContext{traceId=TraceId{traceId=0000000000000000000000000000000a},"
        + " spanId=SpanId{spanId=30}, traceOptions=TraceOptions{optionsMask=0}}");
  }
}
