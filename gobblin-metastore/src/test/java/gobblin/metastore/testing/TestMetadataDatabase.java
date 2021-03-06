/*
 * Copyright (C) 2014-2016 LinkedIn Corp. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a copy of the
 * License at  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied.
 */

package gobblin.metastore.testing;

import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.Objects;
import java.util.UUID;


class TestMetadataDatabase implements ITestMetastoreDatabase {
  private final TestMetastoreDatabaseServer testMetastoreDatabaseServer;
  private final String database;

  TestMetadataDatabase(TestMetastoreDatabaseServer testMetastoreDatabaseServer, String version) throws Exception {
    this.testMetastoreDatabaseServer = testMetastoreDatabaseServer;
    this.database = String.format("gobblin_%s", UUID.randomUUID().toString().replace("-", StringUtils.EMPTY));
    this.resetDatabase(version);
  }

  @Override
  public void close() throws IOException {
    try {
      this.testMetastoreDatabaseServer.drop(database);
    } catch (URISyntaxException | SQLException ignored) {
    } finally {
      TestMetastoreDatabaseFactory.release(this);
    }
  }

  @Override
  public String getJdbcUrl() throws URISyntaxException {
    return this.testMetastoreDatabaseServer.getJdbcUrl(this.database).toString();
  }

  @Override
  public void resetDatabase(String version) throws Exception {
    this.testMetastoreDatabaseServer.prepareDatabase(database, version);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    TestMetadataDatabase that = (TestMetadataDatabase) o;
    return Objects.equals(database, that.database);
  }

  @Override
  public int hashCode() {
    return Objects.hash(database);
  }
}
