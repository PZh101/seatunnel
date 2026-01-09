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

package org.apache.seatunnel.transform.business;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.apache.seatunnel.api.table.catalog.*;
import org.apache.seatunnel.api.table.type.SeaTunnelDataType;
import org.apache.seatunnel.api.table.type.SeaTunnelRow;
import org.apache.seatunnel.api.table.type.SeaTunnelRowType;
import org.apache.seatunnel.common.utils.JsonUtils;
import org.apache.seatunnel.transform.common.AbstractCatalogSupportMapTransform;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class BusinessTransform extends AbstractCatalogSupportMapTransform {
    public static String PLUGIN_NAME = "BusinessProcessor";
    private final BusinessTransformConfig config;
    private MyHttpClient myHttpClient;
    public static final String DATE_FORMAT = "yyyy-MM-dd";
    public static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static final String TIME_FORMAT = "HH:mm:ss";
    // 用于跟踪新增字段的索引
    private final Map<String, Integer> newFieldIndexMap = new LinkedHashMap<>();
    private int originalFieldCount;

    // 用于存储动态确定的字段信息
    private final Map<String, SeaTunnelDataType<?>> fieldTypes = new LinkedHashMap<>();

    // 标记表结构是否已初始化
//    private final AtomicBoolean schemaInitialized = new AtomicBoolean(false);
//    private volatile CatalogTable realOutputCatalogTable;

    public BusinessTransform(
            @NonNull BusinessTransformConfig config, @NonNull CatalogTable catalogTable) {
        super(catalogTable);
        this.config = config;
    }

    @Override
    public void open() {
//        super.open();
        myHttpClient = new MyHttpClient(config.getHttpUrl(), null, null, null);
    }

    @Override
    public String getPluginName() {
        return PLUGIN_NAME;
    }

    @Override
    protected SeaTunnelRow transformRow(SeaTunnelRow inputRow) {
        try {
            Map<String, Object> requestData = buildRequestData(inputRow);
            CloseableHttpResponse response = sendHttpRequest(requestData);

            if (isSuccessfulResponse(response)) {
                Map<String, Object> responseData = parseResponse(response);
                if (responseData != null && responseData.containsKey("data")) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> fieldDataMap = (Map<String, Object>) responseData.get("data");

                    if (shouldFilterRow(fieldDataMap)) {
                        return null;
                    }

                    return buildOutputRow(inputRow, fieldDataMap);
                }
            }
        } catch (IOException e) {
            log.warn("自定义HTTP处理失败", e);
        }
        return inputRow;
    }

    private Map<String, Object> buildRequestData(SeaTunnelRow inputRow) {
        String[] fieldNames = inputCatalogTable.getTableSchema().getFieldNames();
        Object[] fields = inputRow.getFields();
        if (fields.length != fieldNames.length) {
            log.warn("Field length is not equal to field names length.");
        }

        Map<String, Object> requestData = new LinkedHashMap<>();
        Map<String, Object> rowData = new LinkedHashMap<>();
        TableSchema tableSchema = inputCatalogTable.getTableSchema();
        for (int i = 0; i < fieldNames.length; i++) {
            String fieldName = fieldNames[i];
            Column column = tableSchema.getColumn(fieldName);
            Object fieldValue = fields[i];
            if (column != null) {
                fieldValue = convertToJsonValue(column.getDataType(), fieldValue);
            }
            rowData.put(fieldName, fieldValue);
        }
        originalFieldCount = fieldNames.length;

        requestData.put("row", rowData);
        requestData.put("table", inputCatalogTable.getTableId().getTableName());
        requestData.put("databaseName", inputCatalogTable.getTableId().getDatabaseName());
        requestData.put("catalogName", inputCatalogTable.getTableId().getCatalogName());
        requestData.put("schemaName", inputCatalogTable.getTableId().getSchemaName());
        requestData.put("businessId", config.getBusinessId());
        requestData.put("options", config.getOptions());

        return requestData;
    }

    private CloseableHttpResponse sendHttpRequest(Map<String, Object> requestData) throws IOException {
        HttpPost httpPost = new HttpPost();
        httpPost.setURI(URI.create(config.getHttpUrl()));
        httpPost.setEntity(new StringEntity(JsonUtils.toJsonString(requestData), ContentType.APPLICATION_JSON));
        return myHttpClient.getClient().execute(httpPost);
    }

    private boolean isSuccessfulResponse(CloseableHttpResponse response) {
        return response.getStatusLine().getStatusCode() == 200 && response.getEntity() != null;
    }

    private Map<String, Object> parseResponse(CloseableHttpResponse response) throws IOException {
        String jsonObjectString = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
        return JsonUtils.toMap(jsonObjectString, String.class, Object.class);
    }

    private boolean shouldFilterRow(Map<String, Object> fieldDataMap) {
        return fieldDataMap.containsKey("__filter__") && Boolean.TRUE.equals(fieldDataMap.get("__filter__"));
    }

    private SeaTunnelRow buildOutputRow(SeaTunnelRow inputRow, Map<String, Object> fieldDataMap) {
        TableSchema tableSchema = getProducedCatalogTable().getTableSchema();
        initializeNewFieldIndexMap(tableSchema);

        List<Object> outputFieldValues = new ArrayList<>();

        Map<String, Integer> fieldIndexMap = buildFieldIndexMap();

        // 处理响应中的字段更新和新增
        for (Map.Entry<String, Object> entry : fieldDataMap.entrySet()) {
            String fieldName = entry.getKey();
            Object fieldValue = entry.getValue();

            // 跳过内部使用的特殊字段
            if ("__filter__".equals(fieldName)) {
                continue;
            }

            if (config.getOutputFields() != null && !config.getOutputFields().isEmpty()) {
                if (newFieldIndexMap.containsKey(fieldName)) {
                    int newFieldIndex = newFieldIndexMap.getOrDefault(fieldName, 0);
                    Column column = tableSchema.getColumn(fieldName);
                    if (column != null) {
                        fieldValue = convertToSeatunnelValue(column.getDataType(), fieldValue);
                    }
                    outputFieldValues.add(newFieldIndex, fieldValue);
                }
            } else {
                if (fieldIndexMap.containsKey(fieldName)) {
                    Integer i = fieldIndexMap.getOrDefault(fieldName, 0);
                    Column column = tableSchema.getColumn(fieldName);
                    if (column != null) {
                        fieldValue = convertToSeatunnelValue(column.getDataType(), fieldValue);
                    }
                    outputFieldValues.add(i, fieldValue);
                }
            }
        }

        // 构造新的SeaTunnelRow
        SeaTunnelRow newSeaTunnelRow = new SeaTunnelRow(outputFieldValues.toArray());
        newSeaTunnelRow.setTableId(inputRow.getTableId());
        newSeaTunnelRow.setRowKind(inputRow.getRowKind());
        return newSeaTunnelRow;
    }

    private void initializeNewFieldIndexMap(TableSchema tableSchema) {
        newFieldIndexMap.clear();
        int fieldIndex = 0;
        for (Column column : tableSchema.getColumns()) {
            newFieldIndexMap.put(column.getName(), fieldIndex++);
        }
    }

    private Map<String, Integer> buildFieldIndexMap() {
        String[] fieldNames = inputCatalogTable.getTableSchema().getFieldNames();
        Map<String, Integer> fieldIndexMap = new LinkedHashMap<>();
        for (int i = 0; i < fieldNames.length; i++) {
            fieldIndexMap.put(fieldNames[i], i);
        }
        return fieldIndexMap;
    }

    @Override
    protected TableSchema transformTableSchema() {
        List<Map<String, String>> outputFields = config.getOutputFields();
        if (outputFields == null) {
            outputFields = new ArrayList<>();
        }
        List<Column> inputColumns = inputCatalogTable.getTableSchema().getColumns();
        SeaTunnelRowType seaTunnelRowType =
                inputCatalogTable.getTableSchema().toPhysicalRowDataType();
        List<Column> outputColumns = new ArrayList<>(outputFields.size());
//        ArrayList<String> inputFieldNames = Lists.newArrayList(seaTunnelRowType.getFieldNames());
        ArrayList<String> outputFieldNames = new ArrayList<>();

        for (Column inputColumn : inputColumns) {
            PhysicalColumn outputColumn =
                    PhysicalColumn.of(
                            inputColumn.getName(),
                            inputColumn.getDataType(),
                            inputColumn.getColumnLength(),
                            inputColumn.isNullable(),
                            inputColumn.getDefaultValue(),
                            inputColumn.getComment());
            outputColumns.add(outputColumn);
            outputFieldNames.add(outputColumn.getName());
        }
        outputFields.forEach(
                (item) -> {
                    String fieldName = item.get(BusinessTransformConfig.FIELD_NAME.key());
                    String fieldType = item.get(BusinessTransformConfig.FIELD_TYPE.key());
                    String comment = item.get(BusinessTransformConfig.FIELD_COMMENT.key());
                    long columnLength = 0L;
                    try {
                        columnLength = Long.parseLong(item.get(BusinessTransformConfig.FIELD_LENGTH.key()));
                    } catch (NumberFormatException ignored) {
                    }
                    Object defaultValue = item.get(BusinessTransformConfig.FIELD_DEFAULT_VALUE.key());
                    boolean fieldNullable = Boolean.parseBoolean(item.get(BusinessTransformConfig.FIELD_NULLABLE.key()));
                    SeaTunnelDataType<?> seaTunnelDataType = SeaTunnelDataTypeConvertorUtil.deserializeSeaTunnelDataType(fieldName, fieldType);
                    PhysicalColumn outputColumn =
                            PhysicalColumn.of(
                                    fieldName,
                                    seaTunnelDataType,
                                    columnLength,
                                    fieldNullable,
                                    defaultValue,
                                    comment);
                    outputColumns.add(outputColumn);
                    outputFieldNames.add(outputColumn.getName());
                });

        return getTableSchema(outputColumns, outputFieldNames, inputCatalogTable.getTableSchema());
    }

    public static TableSchema getTableSchema(List<Column> outputColumns, ArrayList<String> outputFieldNames, TableSchema tableSchema) {
        List<ConstraintKey> outputConstraintKeys =
                tableSchema.getConstraintKeys().stream()
                        .filter(
                                key -> {
                                    List<String> constraintColumnNames =
                                            key.getColumnNames().stream()
                                                    .map(
                                                            ConstraintKey.ConstraintKeyColumn
                                                                    ::getColumnName)
                                                    .collect(Collectors.toList());
                                    return outputFieldNames.containsAll(constraintColumnNames);
                                })
                        .map(ConstraintKey::copy)
                        .collect(Collectors.toList());

        PrimaryKey copiedPrimaryKey = null;
        if (tableSchema.getPrimaryKey() != null
                && outputFieldNames.containsAll(
                tableSchema.getPrimaryKey().getColumnNames())) {
            copiedPrimaryKey = tableSchema.getPrimaryKey().copy();
        }

        return TableSchema.builder()
                .primaryKey(copiedPrimaryKey)
                .columns(outputColumns)
                .constraintKey(outputConstraintKeys)
                .build();
    }

    @Override
    protected TableIdentifier transformTableIdentifier() {
        return inputCatalogTable.getTableId().copy();
    }

    @Override
    public void close() {
        super.close();
    }

    public Object convertToSeatunnelValue(SeaTunnelDataType<?> seaTunnelDataType, Object rawFieldValue) {
        if (rawFieldValue instanceof String) {
            if (seaTunnelDataType.getTypeClass() == LocalTime.class) {
                return LocalTime.parse(rawFieldValue.toString(), DateTimeFormatter.ofPattern(TIME_FORMAT));
            } else if (seaTunnelDataType.getTypeClass() == LocalDateTime.class) {
                return LocalDateTime.parse(rawFieldValue.toString(), DateTimeFormatter.ofPattern(DATE_TIME_FORMAT));
            } else if (seaTunnelDataType.getTypeClass() == LocalDate.class) {
                return LocalDate.parse(rawFieldValue.toString(), DateTimeFormatter.ofPattern(DATE_FORMAT));
            }
        }
        return rawFieldValue;
    }

    public Object convertToJsonValue(SeaTunnelDataType<?> seaTunnelDataType, Object rawFieldValue) {
        if (rawFieldValue == null) {
            return null;
        }
        if (seaTunnelDataType.getTypeClass() == LocalTime.class && rawFieldValue instanceof LocalTime) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(TIME_FORMAT);
            return formatter.format((LocalTime) rawFieldValue);
        } else if (seaTunnelDataType.getTypeClass() == LocalDateTime.class) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT);
            return formatter.format((LocalDateTime) rawFieldValue);
        } else if (seaTunnelDataType.getTypeClass() == LocalDate.class) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_FORMAT);
            return formatter.format((LocalDate) rawFieldValue);
        }
        return rawFieldValue;
    }

}