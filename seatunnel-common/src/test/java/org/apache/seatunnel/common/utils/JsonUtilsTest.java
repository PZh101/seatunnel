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

package org.apache.seatunnel.common.utils;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class JsonUtilsTest {

    @Test
    public void testLocalDateTimeSerialization() {
        LocalDateTime dateTime = LocalDateTime.of(2023, 12, 25, 15, 30, 45);
        String json = JsonUtils.toJsonString(dateTime);
        assertEquals("\"2023-12-25 15:30:45\"", json);
    }

    @Test
    public void testLocalDateSerialization() {
        LocalDate date = LocalDate.of(2023, 12, 25);
        String json = JsonUtils.toJsonString(date);
        assertEquals("\"2023-12-25\"", json);
    }

    @Test
    public void testLocalTimeSerialization() {
        LocalTime time = LocalTime.of(15, 30, 45);
        String json = JsonUtils.toJsonString(time);
        assertEquals("\"15:30:45\"", json);
    }

    @Test
    public void testMapWithLocalDateTimeSerialization() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", "test");
        map.put("dateTime", LocalDateTime.of(2023, 12, 25, 15, 30, 45));
        map.put("date", LocalDate.of(2023, 12, 25));
        map.put("time", LocalTime.of(15, 30, 45));

        String json = JsonUtils.toJsonString(map);
        assertFalse(json.contains("["), "LocalDateTime should not be serialized as array");
        assertFalse(json.contains("2023,12,25"), "LocalDateTime should not be serialized as array");
    }
}