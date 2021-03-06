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

package io.confluent.ksql.cli.console.table.builder;

import com.google.common.collect.ImmutableList;
import io.confluent.ksql.cli.console.table.Table;
import io.confluent.ksql.cli.console.table.Table.Builder;
import io.confluent.ksql.rest.entity.FunctionNameList;
import java.util.List;
import java.util.stream.Stream;

public class FunctionNameListTableBuilder implements TableBuilder<FunctionNameList> {

  private static final List<String> HEADERS =
      ImmutableList.of("Function Name", "Type");

  @Override
  public Table buildTable(final FunctionNameList functionNameList) {
    final Stream<List<String>> rows = functionNameList.getFunctions()
        .stream()
        .sorted()
        .map(func -> ImmutableList.of(func.getName(), func.getType().name().toUpperCase()));

    return new Builder()
        .withColumnHeaders(HEADERS)
        .withRows(rows)
        .build();
  }
}
