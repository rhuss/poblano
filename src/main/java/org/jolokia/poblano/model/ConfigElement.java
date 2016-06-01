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

    public static final String MAP_TYPE = "map";
    public static final String COLLECTION_TYPE = "list";
    public static final String ARRAY_TYPE = "array";

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
        this.id = createId(parent, name, type);
        children = new ArrayList<>();
        mojos = new HashSet<>();
        mojos.add(mojo);
    }

    /**
     * Create unique id for this config element
     *
     * @param parent parent element when it is about complex elements
     * @param name name of the element
     * @param type its type
     * @return a strin representation of the type
     */
    public static String createId(ConfigElement parent, String name, String type) {
        String parentId = parent != null ? parent.getId() + "|" : "";
        return parentId + "|" + name + "|" + type;
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

    public boolean isMap() {
        return MAP_TYPE.equals(type);
    }

    public boolean isList() {
        return COLLECTION_TYPE.equals(type);
    }

    public boolean isArray() {
        return ARRAY_TYPE.equals(type);
    }


    public boolean isComplexType() {
        return hasChildren();
    }

    public String getId() {
        return id;
    }

    public boolean isListLike() {
        return isList() || isArray();
    }
}
