package com.heroku;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * @author Ryan Brainard
 */
class MappingConverter {

    static Map<String, String> convert(String mappings) {
        if (mappings == null) {
            return Collections.emptyMap();
        }

        final Properties props = new Properties();

        Reader input = null;
        try {
            input = new StringReader(mappings.replaceAll(";", "\n"));
            props.load(input);
        } catch (IOException e) {
            System.err.println("Failed to parse mappings from: " + mappings);
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }

        final Map<String, String> propsMap = new HashMap<String, String>();

        for (Map.Entry<Object, Object> prop : props.entrySet()) {
            propsMap.put(prop.getKey().toString(), prop.getValue().toString());
        }

        return propsMap;
    }
}
