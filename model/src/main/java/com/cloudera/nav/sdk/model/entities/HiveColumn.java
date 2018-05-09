
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

package com.cloudera.nav.sdk.model.entities;

import com.cloudera.nav.sdk.model.HiveIdGenerator;
import com.cloudera.nav.sdk.model.SourceType;
import com.cloudera.nav.sdk.model.annotations.MClass;
import com.cloudera.nav.sdk.model.annotations.MProperty;
import com.google.common.collect.ImmutableMap;

import java.util.Map;

/**
 * Represents a Hive column; uniquely identified by the source id, database name,
 * table name, and column name. Note that the source type, entity type, and
 * namespace should not be modified.
 */
@MClass(model="hv_column")
public class HiveColumn extends Entity {

  private final String DATABASE_NAME = "databaseName";
  private final String TABLE_NAME = "tableName";
  private final String COLUMN_NAME = "columnName";

  @MProperty
  private String databaseName;
  @MProperty
  private String tableName;
  @MProperty
  private String columnName;

  public HiveColumn() {
    setSourceType(SourceType.HIVE);
    setEntityType(EntityType.FIELD);
  }

  public HiveColumn(String sourceId, String db, String table, String column) {
    this();
    setSourceId(sourceId);
    setDatabaseName(db);
    setTableName(table);
    setColumnName(column);
  }

  public HiveColumn(String id) {
    this();
    setIdentity(id);
  }

  public String getDatabaseName() {
    return databaseName;
  }

  public void setDatabaseName(String databaseName) {
    this.databaseName = databaseName;
  }

  public String getTableName() {
    return tableName;
  }

  public void setTableName(String tableName) {
    this.tableName = tableName;
  }

  /**
   * @return the column name. Aliases {@link HiveColumn#getName}
   */
  public String getColumnName() {
    return columnName;
  }

  /**
   * Changes the column name. Aliases {@link HiveColumn#setName}
   * @param columnName
   */
  public void setColumnName(String columnName) {
    this.columnName = columnName;
  }

  @Override
  public Map<String, String> getIdAttrsMap() {
    return ImmutableMap.of(
        DATABASE_NAME, this.getDatabaseName(),
        TABLE_NAME, this.getTableName(),
        COLUMN_NAME, this.getColumnName());
  }
}
