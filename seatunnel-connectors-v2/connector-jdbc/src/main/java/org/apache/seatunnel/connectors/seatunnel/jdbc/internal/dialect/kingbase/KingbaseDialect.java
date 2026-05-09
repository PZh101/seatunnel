/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.seatunnel.connectors.seatunnel.jdbc.internal.dialect.kingbase;

import com.kingbase8.jdbc.KbConnection;
import org.apache.seatunnel.api.table.catalog.TablePath;
import org.apache.seatunnel.connectors.seatunnel.jdbc.internal.converter.JdbcRowConverter;
import org.apache.seatunnel.connectors.seatunnel.jdbc.internal.dialect.DatabaseIdentifier;
import org.apache.seatunnel.connectors.seatunnel.jdbc.internal.dialect.JdbcDialect;
import org.apache.seatunnel.connectors.seatunnel.jdbc.internal.dialect.JdbcDialectTypeMapper;
import org.apache.seatunnel.connectors.seatunnel.jdbc.internal.dialect.mysql.MySqlTypeConverter;
import org.apache.seatunnel.connectors.seatunnel.jdbc.internal.dialect.mysql.MySqlTypeMapper;
import org.apache.seatunnel.connectors.seatunnel.jdbc.internal.dialect.mysql.MysqlDialect;
import org.apache.seatunnel.connectors.seatunnel.jdbc.internal.dialect.mysql.MysqlJdbcRowConverter;

import java.sql.Connection;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

public class KingbaseDialect implements JdbcDialect {
    private String compatibleLevel;

    public KingbaseDialect(String compatibleLevel) {
        this.compatibleLevel = compatibleLevel;
    }

    public void initCompatibleLevel(Connection connection) {
        if (connection instanceof KbConnection) {
            KbConnection kbConnection = (KbConnection) connection;
            this.compatibleLevel = kbConnection.getCompatibleLevel();
        }
    }

    @Override
    public String dialectName() {
        return DatabaseIdentifier.KINGBASE;
    }

    @Override
    public JdbcRowConverter getRowConverter() {
        if (isMySQL()) {
            return new MysqlJdbcRowConverter();
        }
        return new KingbaseJdbcRowConverter();
    }

    @Override
    public JdbcDialectTypeMapper getJdbcDialectTypeMapper() {
        if (isMySQL()) {
            return new MySqlTypeMapper();
        }
        return new KingbaseTypeMapper();
    }

    @Override
    public Optional<String> getUpsertStatement(
            String database, String tableName, String[] fieldNames, String[] uniqueKeyFields) {

        if (isMySQL()) {
            return new MysqlDialect().getUpsertStatement(database, tableName, fieldNames, uniqueKeyFields);
        }
        String uniqueColumns =
                Arrays.stream(uniqueKeyFields)
                        .map(this::quoteIdentifier)
                        .collect(Collectors.joining(", "));
        String updateClause =
                Arrays.stream(fieldNames)
                        .map(
                                fieldName ->
                                        quoteIdentifier(fieldName)
                                                + "=EXCLUDED."
                                                + quoteIdentifier(fieldName))
                        .collect(Collectors.joining(", "));
        String upsertSQL =
                String.format(
                        "%s ON CONFLICT (%s) DO UPDATE SET %s",
                        getInsertIntoStatement(database, tableName, fieldNames),
                        uniqueColumns,
                        updateClause);
        return Optional.of(upsertSQL);
    }

    @Override
    public TablePath parse(String tablePath) {
        return TablePath.of(tablePath, true);
    }

    private boolean isMySQL() {
        return "mysql".equalsIgnoreCase(this.compatibleLevel);
    }
}
