/*
 * Copyright (c) 2015 Cloudera, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cloudera.nav.plugin.client.examples.extraction;

import com.cloudera.nav.plugin.client.NavApiCient;

import java.util.Map;

/**
 * Iterable used for entities and relations in MetadataResultSet object returned
 * by extractMetadata() calls.
 */
public class IncrementalExtractIterable implements Iterable<Map<String, Object>> {

  private final NavApiCient client;
  private final MetadataType type;
  private final String query;
  private final Integer limit;
  private final Iterable<String> extractorRunIds;

  public IncrementalExtractIterable(NavApiCient client, MetadataType type,
                                    String query, Integer limit,
                                    Iterable<String> extractorRunIds){
    this.query = query;
    this.type = type;
    this.client = client;
    this.limit =limit;
    this.extractorRunIds = extractorRunIds;
  }

  @Override
  public IncrementalExtractIterator iterator() {
    return new IncrementalExtractIterator(client, type, query, limit, extractorRunIds);
  }
}
