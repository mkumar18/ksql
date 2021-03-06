/*
 * Copyright 2018 Confluent Inc.
 *
 * Licensed under the Confluent Community License; you may not use this file
 * except in compliance with the License.  You may obtain a copy of the License at
 *
 * http://www.confluent.io/confluent-community-license
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */

package io.confluent.ksql.rest.entity;

import static io.confluent.ksql.util.KsqlConfig.KSQL_TRANSIENT_QUERY_NAME_PREFIX_CONFIG;
import static io.confluent.ksql.util.KsqlConfig.SINK_NUMBER_OF_REPLICAS_PROPERTY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.google.common.collect.ImmutableMap;
import com.google.common.testing.EqualsTester;
import io.confluent.ksql.util.KsqlConfig;
import io.confluent.ksql.util.KsqlException;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

@SuppressWarnings("SameParameterValue")
public class KsqlRequestTest {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final String A_JSON_REQUEST = "{"
      + "\"ksql\":\"sql\","
      + "\"streamsProperties\":{"
      + "\"" + KsqlConfig.KSQL_SERVICE_ID_CONFIG + "\":\"some-service-id\""
      + "}}";
  private static final String A_JSON_REQUEST_WITH_COMMAND_NUMBER = "{"
      + "\"ksql\":\"sql\","
      + "\"streamsProperties\":{"
      + "\"" + KsqlConfig.KSQL_SERVICE_ID_CONFIG + "\":\"some-service-id\""
      + "},"
      + "\"commandSequenceNumber\":2}";
  private static final String A_JSON_REQUEST_WITH_NULL_COMMAND_NUMBER = "{"
      + "\"ksql\":\"sql\","
      + "\"streamsProperties\":{"
      + "\"" + KsqlConfig.KSQL_SERVICE_ID_CONFIG + "\":\"some-service-id\""
      + "},"
      + "\"commandSequenceNumber\":null}";

  private static final ImmutableMap<String, Object> SOME_PROPS = ImmutableMap.of(
      KsqlConfig.KSQL_SERVICE_ID_CONFIG, "some-service-id"
  );
  private static final long SOME_COMMAND_NUMBER = 2L;

  private static final KsqlRequest A_REQUEST = new KsqlRequest("sql", SOME_PROPS, null);
  private static final KsqlRequest A_REQUEST_WITH_COMMAND_NUMBER =
      new KsqlRequest("sql", SOME_PROPS, SOME_COMMAND_NUMBER);

  @BeforeClass
  public static void setUpClass() {
    OBJECT_MAPPER.registerModule(new Jdk8Module());
  }

  @Rule
  public final ExpectedException expectedException = ExpectedException.none();

  @Test
  public void shouldHandleNullStatement() {
    assertThat(new KsqlRequest(null, SOME_PROPS, SOME_COMMAND_NUMBER).getKsql(), is(""));
  }

  @Test
  public void shouldHandleNullProps() {
    assertThat(new KsqlRequest("sql", null, SOME_COMMAND_NUMBER).getStreamsProperties(),
        is(Collections.emptyMap()));
  }

  @Test
  public void shouldHandleNullCommandNumber() {
    assertThat(new KsqlRequest("sql", SOME_PROPS, null).getCommandSequenceNumber(), is(Optional.empty()));
  }

  @Test
  public void shouldDeserializeFromJson() {
    // When:
    final KsqlRequest request = deserialize(A_JSON_REQUEST);

    // Then:
    assertThat(request, is(A_REQUEST));
  }

  @Test
  public void shouldDeserializeFromJsonWithCommandNumber() {
    // When:
    final KsqlRequest request = deserialize(A_JSON_REQUEST_WITH_COMMAND_NUMBER);

    // Then:
    assertThat(request, is(A_REQUEST_WITH_COMMAND_NUMBER));
  }

  @Test
  public void shouldDeserializeFromJsonWithNullCommandNumber() {
    // When:
    final KsqlRequest request = deserialize(A_JSON_REQUEST_WITH_NULL_COMMAND_NUMBER);

    // Then:
    assertThat(request, is(A_REQUEST));
  }

  @Test
  public void shouldSerializeToJson() {
    // When:
    final String jsonRequest = serialize(A_REQUEST);

    // Then:
    assertThat(jsonRequest, is(A_JSON_REQUEST_WITH_NULL_COMMAND_NUMBER));
  }

  @Test
  public void shouldSerializeToJsonWithCommandNumber() {
    // When:
    final String jsonRequest = serialize(A_REQUEST_WITH_COMMAND_NUMBER);

    // Then:
    assertThat(jsonRequest, is(A_JSON_REQUEST_WITH_COMMAND_NUMBER));
  }

  @Test
  public void shouldImplementHashCodeAndEqualsCorrectly() {
    new EqualsTester()
        .addEqualityGroup(new KsqlRequest("sql", SOME_PROPS, SOME_COMMAND_NUMBER),
            new KsqlRequest("sql", SOME_PROPS, SOME_COMMAND_NUMBER))
        .addEqualityGroup(new KsqlRequest("different-sql", SOME_PROPS, SOME_COMMAND_NUMBER))
        .addEqualityGroup(new KsqlRequest("sql", ImmutableMap.of(), SOME_COMMAND_NUMBER))
        .addEqualityGroup(new KsqlRequest("sql", SOME_PROPS, null))
        .testEquals();
  }

  @Test
  public void shouldHandleShortProperties() {
    // Given:
    final String jsonRequest = "{"
        + "\"ksql\":\"sql\","
        + "\"streamsProperties\":{"
        + "\"" + KsqlConfig.SINK_NUMBER_OF_REPLICAS_PROPERTY + "\":2"
        + "}}";

    // When:
    final KsqlRequest request = deserialize(jsonRequest);

    // Then:
    assertThat(request.getStreamsProperties().get(SINK_NUMBER_OF_REPLICAS_PROPERTY), is((short)2));
  }

  @Test
  public void shouldThrowOnInvalidPropertyValue() {
    // Given:
    final KsqlRequest request = new KsqlRequest(
        "sql",
        ImmutableMap.of(
            SINK_NUMBER_OF_REPLICAS_PROPERTY, "not-parsable"
        ),
        null);

    expectedException.expect(KsqlException.class);
    expectedException.expectMessage(containsString(SINK_NUMBER_OF_REPLICAS_PROPERTY));
    expectedException.expectMessage(containsString("not-parsable"));

    // When:
    request.getStreamsProperties();
  }

  @Test
  public void shouldHandleNullPropertyValue() {
    // Given:
    final KsqlRequest request = new KsqlRequest(
        "sql",
        Collections.singletonMap(
            KSQL_TRANSIENT_QUERY_NAME_PREFIX_CONFIG, null
        ),
        null);

    // When:
    final Map<String, Object> props = request.getStreamsProperties();

    // Then:
    assertThat(props.keySet(), hasItem(KSQL_TRANSIENT_QUERY_NAME_PREFIX_CONFIG));
    assertThat(props.get(KSQL_TRANSIENT_QUERY_NAME_PREFIX_CONFIG), is(nullValue()));
  }

  private static String serialize(final KsqlRequest request) {
    try {
      return OBJECT_MAPPER.writeValueAsString(request);
    } catch (IOException e) {
      throw new RuntimeException("test invalid", e);
    }
  }

  private static KsqlRequest deserialize(final String json) {
    try {
      return OBJECT_MAPPER.readValue(json, KsqlRequest.class);
    } catch (IOException e) {
      if (e.getCause() instanceof RuntimeException) {
        throw (RuntimeException) e.getCause();
      }
      throw new RuntimeException(e);
    }
  }
}