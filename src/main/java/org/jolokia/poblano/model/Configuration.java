package org.jolokia.poblano.model;
/*
 * 
 * Copyright 2016 Roland Huss
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.*;

/**
 * Top-level configuration model object
 *
 * @author roland
 * @since 23/05/16
 */
public class Configuration {

    // All config elements found
    private Map<String, ConfigElement> elements = new HashMap<>();

    // All root elements
    private List<ConfigElement> rootElements = new ArrayList<>();

    /**
     * Get or create a config element
     *
     * @param parent the parent element or <code>null</code> if it is a root element
     * @param mojo mojo for which the config element should be updated
     * @param name name of the configuration
     * @param type type or class name
     * @param defaultVal default value if not set
     * @param documentation documentation to add
     */
    public void update(ConfigElement parent, String mojo, String name,
                       String type, String defaultVal, String documentation) {

        ConfigElement element = get(name, type);
        if (element == null) {
            element = create(parent, mojo, name, type, defaultVal, documentation);
        }
        element.addMojo(mojo);
    }

    private ConfigElement create(ConfigElement parent, String mojo, String name,
                                 String type, String defaultVal, String documentation) {
        ConfigElement element;
        element = new ConfigElement(parent,
                                mojo,
                                name,
                                type,
                                defaultVal,
                                documentation);
        elements.put(createId(name,type), element);
        if (parent == null) {
            rootElements.add(element);
        }
        return element;
    }

    public ConfigElement get(String name, String type) {
        return elements.get(createId(name, type));
    }

    private String createId(String name, String type) {
        return name + "|" + type;
    }

    public List<ConfigElement> getRootElements() {
        return rootElements;
    }
}
