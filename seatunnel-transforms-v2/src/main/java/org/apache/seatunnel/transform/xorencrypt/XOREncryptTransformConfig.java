package org.apache.seatunnel.transform.xorencrypt;


import lombok.Getter;
import lombok.Setter;
import org.apache.seatunnel.api.configuration.Option;
import org.apache.seatunnel.api.configuration.Options;
import org.apache.seatunnel.api.configuration.ReadonlyConfig;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Setter
public class XOREncryptTransformConfig implements Serializable {
    public static final Option<Map<String, String>> OPTIONS =
            Options.key("options")
                    .mapType()
                    .defaultValue(new LinkedHashMap<>())
                    .withDescription(
                            "指定需要加密的字段名称");
    private Map<String, String> options = new LinkedHashMap<>();

    public static XOREncryptTransformConfig of(ReadonlyConfig config) {
        XOREncryptTransformConfig transformConfig = new XOREncryptTransformConfig();
        transformConfig.setOptions(config.get(OPTIONS));
        return transformConfig;
    }
}
