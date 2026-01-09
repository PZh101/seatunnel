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

import lombok.Getter;
import lombok.Setter;
import org.apache.seatunnel.api.configuration.Option;
import org.apache.seatunnel.api.configuration.Options;
import org.apache.seatunnel.api.configuration.ReadonlyConfig;
import org.apache.seatunnel.shade.com.fasterxml.jackson.core.type.TypeReference;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class BusinessTransformConfig implements Serializable {
    public static final Option<Map<String, String>> OPTIONS =
            Options.key("options")
                    .mapType()
                    .noDefaultValue()
                    .withDescription(
                            "Specify the field mapping relationship between input and output");
    public static final Option<String> BUSINESS_ID =
            Options.key("business_id")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("业务ID");

    public static final Option<String> HTTP_URL =
            Options.key("http_url")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("http连接");
    public static final Option<List<Map<String, String>>> OUTPUT_FIELDS =
            Options.key("output_fields")
                    .type(new TypeReference<List<Map<String, String>>>() {
                    })
                    .noDefaultValue()
                    .withDescription(
                            "Specify the field mapping relationship between input and output");
    public static final Option<String> FIELD_NAME =
            Options.key("field_name")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("字段名称");
    public static final Option<String> FIELD_TYPE =
            Options.key("field_type")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("字段类型");
    public static final Option<String> FIELD_LENGTH =
            Options.key("field_length")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("字段长度");
    public static final Option<String> FIELD_COMMENT =
            Options.key("field_comment")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("字段精度");
    public static final Option<String> FIELD_DEFAULT_VALUE =
            Options.key("field_default_value")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("字段小数位数");
    public static final Option<String> FIELD_NULLABLE =
            Options.key("field_nullable")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("字段是否为空");
    private Map<String, String> options = new LinkedHashMap<>();
    private List<Map<String, String>> outputFields = new ArrayList<>();

    private String businessId;
    private String httpUrl;

    public static BusinessTransformConfig of(ReadonlyConfig config) {
        BusinessTransformConfig businessTransformConfig = new BusinessTransformConfig();
        businessTransformConfig.setOptions(config.get(OPTIONS));
        businessTransformConfig.setBusinessId(config.get(BUSINESS_ID));
        businessTransformConfig.setHttpUrl(config.get(HTTP_URL));
        List<Map<String, String>> mapList = config.get(OUTPUT_FIELDS);
        businessTransformConfig.setOutputFields(mapList);
        return businessTransformConfig;
    }
}
