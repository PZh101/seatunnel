package org.apache.seatunnel.transform.xorencrypt;

import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import org.apache.seatunnel.api.table.catalog.CatalogTable;
import org.apache.seatunnel.api.table.catalog.Column;
import org.apache.seatunnel.api.table.catalog.PhysicalColumn;
import org.apache.seatunnel.api.table.type.BasicType;
import org.apache.seatunnel.api.table.type.DecimalType;
import org.apache.seatunnel.api.table.type.SeaTunnelDataType;
import org.apache.seatunnel.api.table.type.SeaTunnelRowAccessor;
import org.apache.seatunnel.transform.common.ErrorHandleWay;
import org.apache.seatunnel.transform.common.MultipleFieldOutputTransform;

import java.util.*;
import java.util.stream.Collectors;

public class XOREncryptTransform extends MultipleFieldOutputTransform {
    private XOREncryptTransformConfig xorEncryptTransformConfig;

    public XOREncryptTransform(@NonNull CatalogTable inputCatalogTable) {
        super(inputCatalogTable);
    }

    public XOREncryptTransform(@NonNull CatalogTable inputCatalogTable, ErrorHandleWay errorHandleWay) {
        super(inputCatalogTable, errorHandleWay);
    }

    public void setXorEncryptTransformConfig(XOREncryptTransformConfig xorEncryptTransformConfig) {
        this.xorEncryptTransformConfig = xorEncryptTransformConfig;
    }

    @Override
    protected Object[] getOutputFieldValues(SeaTunnelRowAccessor inputRow) {
        LinkedHashMap<String, Column> columnMap = inputCatalogTable.getTableSchema().getColumns().stream().reduce(new LinkedHashMap<>(),
                (_map, _col) -> {
                    _map.put(_col.getName(), _col);
                    return _map;
                },
                (a, b) -> {
                    a.putAll(b);
                    return a;
                }
        );
        String[] fieldNames = inputCatalogTable.getTableSchema().getFieldNames();
        Object[] outputFieldValues = new Object[fieldNames.length];
        Map<String, String> optionMap = getOptionMap();
        String encryptFieldNameString = optionMap.get("encryptFieldNames");
        String dncryptFieldNameString = optionMap.get("decryptFieldNames");
        //encrypt和decrypt，如果不存在，则返回原数据
        String method = optionMap.get("method");
        if (StringUtils.isEmpty(method)) {
            method = "encrypt";
        }
        if ("encrypt".equals(method) && StringUtils.isEmpty(encryptFieldNameString)) {
            return inputRow.getFields();
        }
        if ("decrypt".equals(method) && StringUtils.isEmpty(dncryptFieldNameString)) {
            return inputRow.getFields();
        }
        Set<String> encryptFieldNameSet = getFieldNameSet(encryptFieldNameString);
        Set<String> decryptFieldNameSet = getFieldNameSet(dncryptFieldNameString);
        for (int i = 0; i < fieldNames.length; i++) {
            String fieldName = fieldNames[i];
//            Column column = columnMap.get(fieldName);
            Object fieldValue = inputRow.getField(i);
            if ("encrypt".equals(method)) {
                //处理加密
                if (encryptFieldNameSet.contains(fieldName)) {
                    if (fieldValue == null) {
                        outputFieldValues[i] = null;
                    } else {
                        outputFieldValues[i] = XOREncryptor.encryptString(String.valueOf(fieldValue));
                    }
                } else {
                    outputFieldValues[i] = fieldValue;
                }
            } else if ("decrypt".equals(method)) {
                //处理解密
                if (decryptFieldNameSet.contains(fieldName)) {
                    if (fieldValue == null) {
                        outputFieldValues[i] = null;
                    } else {
                        outputFieldValues[i] = XOREncryptor.decrypt(fieldValue, String.class);
                    }
                } else {
                    outputFieldValues[i] = fieldValue;
                }
            }
//            SeaTunnelDataType<?> dataType = column.getDataType();
//            if (dataType.equals(BasicType.STRING_TYPE)) {
//                // 字符串类型
//                Object encrypted = XOREncryptor.encrypt(fieldValue);
//                outputFieldValues[i] = encrypted;
//            } else if (isNumericType(dataType)) {
//                // 这里添加你的处理逻辑
//                outputFieldValues[i] = fieldValue;
//            } else {
//                outputFieldValues[i] = fieldValue;
//            }
        }
        return outputFieldValues;
    }

    @Override
    protected Column[] getOutputColumns() {
        if (inputCatalogTable == null) {
            return new Column[0];
        }
        Map<String, String> optionMap = getOptionMap();
        String encryptFieldNameString = optionMap.get("encryptFieldNames");
        List<Column> catalogTableColumns = inputCatalogTable.getTableSchema().getColumns();
        List<Column> columns = new ArrayList<>();
        if (StringUtils.isEmpty(encryptFieldNameString)) {
            for (Column srcColumn : catalogTableColumns) {
                PhysicalColumn destColumn =
                        PhysicalColumn.of(
                                srcColumn.getName(),
                                srcColumn.getDataType(),
                                srcColumn.getColumnLength(),
                                srcColumn.isNullable(),
                                srcColumn.getDefaultValue(),
                                srcColumn.getComment());
                columns.add(destColumn);
            }
        } else {
            Set<String> fieldNameSet = getFieldNameSet(encryptFieldNameString);
            for (Column srcColumn : catalogTableColumns) {
                if (fieldNameSet.contains(srcColumn.getName())) {
                    PhysicalColumn destColumn =
                            PhysicalColumn.of(
                                    srcColumn.getName(),
                                    BasicType.STRING_TYPE,
                                    512,
                                    srcColumn.isNullable(),
                                    srcColumn.getDefaultValue(),
                                    srcColumn.getComment());
                    columns.add(destColumn);
                } else {
                    PhysicalColumn destColumn =
                            PhysicalColumn.of(
                                    srcColumn.getName(),
                                    srcColumn.getDataType(),
                                    srcColumn.getColumnLength(),
                                    srcColumn.isNullable(),
                                    srcColumn.getDefaultValue(),
                                    srcColumn.getComment());
                    columns.add(destColumn);
                }
            }
        }
        return columns.toArray(new Column[0]);
    }

    /**
     * 判断是否为字符串类型或数字类型
     *
     * @param dataType 数据类型
     * @return 是否为字符串或数字类型
     */
    private boolean isNumericType(SeaTunnelDataType<?> dataType) {
        // 整数类型
        if (dataType.equals(BasicType.INT_TYPE) ||
                dataType.equals(BasicType.LONG_TYPE) ||
                dataType.equals(BasicType.SHORT_TYPE) ||
                dataType.equals(BasicType.BYTE_TYPE)) {
            return true;
        }

        // 浮点数类型
        if (dataType.equals(BasicType.FLOAT_TYPE) ||
                dataType.equals(BasicType.DOUBLE_TYPE)) {
            return true;
        }

        // Decimal类型
        if (dataType instanceof DecimalType) {
            return true;
        }

        return false;
    }

    @Override
    public String getPluginName() {
        return "XOREncrypt";
    }

    public Map<String, String> getOptionMap() {
        if (xorEncryptTransformConfig == null || xorEncryptTransformConfig.getOptions() == null) {
            return new LinkedHashMap<>();
        }
        return xorEncryptTransformConfig.getOptions();
    }

    private Set<String> getFieldNameSet(String fieldNamesString) {
        if (StringUtils.isEmpty(fieldNamesString)) {
            return new HashSet<>();
        }
        String[] fieldNameArray = StringUtils.split(fieldNamesString, ",");
        return Arrays.stream(fieldNameArray).collect(Collectors.toSet());
    }

}
