package org.apache.seatunnel.transform.business;

import org.apache.seatunnel.api.table.type.LocalTimeType;
import org.apache.seatunnel.api.table.type.SeaTunnelDataType;
import org.apache.seatunnel.common.utils.JsonUtils;
import org.apache.seatunnel.shade.com.fasterxml.jackson.databind.JsonNode;
import org.apache.seatunnel.shade.com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Map;

/**
 * @描述 JsonUtilsTest
 * @版本 1.0.0
 * @作者 zhoup
 * @MJ 内部
 * @模块 seatunnel
 * @最后修改时间 2025/11/25 9:49
 */
class JsonUtilsTest {
    @Test
    void testData() {
        java.util.HashMap<String, Object> map = new java.util.HashMap<String, Object>();
        map.put("name", "12");
        map.put("start", LocalDateTime.now());
        String jsonString = JsonUtils.toJsonString(map);
        System.out.println(jsonString);
        ObjectNode jsonNodes = JsonUtils.parseObject(jsonString);
        JsonNode start = jsonNodes.get("start");
        System.out.println(start.getNodeType());
    }

//    @Test
//    void testData1() {
//        java.util.HashMap<String, Object> map = new java.util.HashMap<String, Object>();
//        map.put("name", "12");
//        map.put("start", LocalDateTime.now());
//        String jsonString = org.apache.seatunnel.transform.business.JsonUtils.toJsonString(map);
//        System.out.println(jsonString);
//        Map<String, Object> map1 = org.apache.seatunnel.transform.business.JsonUtils.toMap(jsonString, String.class, Object.class);
//        System.out.println(map1);
//        LocalDateTime start1 = org.apache.seatunnel.transform.business.JsonUtils.parseObject((String) map1.get("start"), LocalDateTime.class);
//        System.out.println(start1);
//        ObjectNode jsonNodes = org.apache.seatunnel.transform.business.JsonUtils.parseObject(jsonString);
//        JsonNode start = jsonNodes.get("start");
//        System.out.println(start.getNodeType());
//    }
//
//    @Test
//    void testData2() {
//        java.util.HashMap<String, Object> map = new java.util.HashMap<String, Object>();
//        map.put("name", "12");
//        map.put("start", LocalDateTime.now());
//
//        String jsonString = org.apache.seatunnel.transform.business.JsonUtils.toJsonString(map);
//        System.out.println(jsonString);
//        Map<String, Object> map1 = org.apache.seatunnel.transform.business.JsonUtils.toMap(jsonString, String.class, Object.class);
//
//        Object o = map1.get("start");
//
//        Object o1 = convertFieldValue(LocalTimeType.LOCAL_DATE_TIME_TYPE, o);
//        System.out.println(map1);
//    }

    public Object convertFieldValue(SeaTunnelDataType<?> seaTunnelDataType, Object rawFieldValue) {
        if (rawFieldValue instanceof String) {
            if (seaTunnelDataType.getTypeClass() == Date.class) {
                return java.sql.Date.valueOf(rawFieldValue.toString());
            } else if (seaTunnelDataType.getTypeClass() == LocalTime.class) {
                return LocalTime.parse(rawFieldValue.toString());
            } else if (seaTunnelDataType.getTypeClass() == LocalDateTime.class) {
                DateTimeFormatter simpleDateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                return LocalDateTime.parse(rawFieldValue.toString(), simpleDateFormat);
            } else if (seaTunnelDataType.getTypeClass() == LocalDate.class) {
                return LocalDate.parse(rawFieldValue.toString());
            }
        }
        return rawFieldValue;
    }
}