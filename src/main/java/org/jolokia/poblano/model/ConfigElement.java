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
 * @author roland
 * @since 23/05/16
 */
public class ConfigElement {

    // Unique id of this config element
    private String id;

    // Name of this config parameter
    private String name;

    // Default value
    private String defaultVal;

    // Documentation
    private String documentation;

    // Type
    private String type;

    // Set of mojos where this config element applies to
    private Set<String> mojos;

    // Parent element
    private ConfigElement parent;

    // Child elements
    private List<ConfigElement> children;

    ConfigElement(ConfigElement parent,
                          String mojo,
                          String name,
                          String type,
                          String defaultVal,
                          String documentation) {
        this.parent = parent;
        this.name = name;
        this.type = type;
        this.documentation = documentation;
        this.defaultVal = defaultVal;
        children = new ArrayList<>();
        mojos = new HashSet<>();
        mojos.add(mojo);
    }

    public void addChild(ConfigElement child) {
        children.add(child);
    }

    public boolean hasChildren() {
        return !children.isEmpty();
    }

    public void addMojo(String mojo) {
        mojos.add(mojo);
    }

    public String getName() {
        return name;
    }

    public String getDefaultVal() {
        return defaultVal;
    }

    public String getDocumentation() {
        return documentation;
    }

    public String getType() {
        return type;
    }

    public List<ConfigElement> getChildren() {
        return children;
    }

    public ConfigElement getParent() {
        return parent;
    }
}
