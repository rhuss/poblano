package org.jolokia.poblano;
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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import javax.jnlp.PersistenceService;
import javax.xml.transform.OutputKeys;

import com.jamesmurty.utils.XMLBuilder2;
import org.apache.maven.plugins.annotations.Parameter;
import org.jolokia.poblano.model.ConfigElement;
import org.jolokia.poblano.model.Configuration;

/**
 * @author roland
 * @since 30/05/16
 */
public class XSDGenerator {

    public final static String XSD_NS = "http://www.w3.org/2001/XMLSchema";

    // Mapping from Java class to simple types
    private static final Map<String,String> SIMPLE_TYPE_LOOKUP;

    public void generate(File targetFile, String targetNamespaceUri, Configuration config) throws IOException {
        XMLBuilder2 builder = createXsdBuilder(targetNamespaceUri);
        generateElements(builder, config, config.getRootElements());
        writeXsd(builder, targetFile);
    }

    private void generateElements(XMLBuilder2 builder, Configuration config, List<ConfigElement> elements) {
        for (ConfigElement element : elements) {
            XMLBuilder2 elBuilder = builder.element("xs:element", XSD_NS)
                                           .a("name",element.getName());
            if (isComplexType(element)) {
                XMLBuilder2 typeBuilder = elBuilder.element("xs:complexType").element("xs:all").a("minOccurs","0");
                addDocumentation(typeBuilder,element);
                generateElements(typeBuilder, config, element.getChildren());
            } else {
                String simpleType = convertSimpleType(element.getType());
                elBuilder.a("type",simpleType);
                addDocumentation(elBuilder, element);
            }
        }
    }

    private void addDocumentation(XMLBuilder2 builder, ConfigElement element) {
        String doc = element.getDocumentation();
        if (doc != null && doc.trim().length() != 0) {
            builder.element("xs:annotation").element("xs:documentation").cdata(doc.trim());
        }
    }

    private String convertSimpleType(String type) {
        String ret = SIMPLE_TYPE_LOOKUP.get(type);
        return ret != null ? ret : "xs:string";
    }

    private boolean isComplexType(ConfigElement element) {
        return element.hasChildren();
    }

    private XMLBuilder2 createXsdBuilder(String targetNamespaceUri) {
        XMLBuilder2 builder =
            XMLBuilder2
                .create("xs:schema", XSD_NS)
                .a("targetNamespace",targetNamespaceUri)
                .a("xmlns",targetNamespaceUri)
                .a("elementFormDefault","qualified")
                  .element("xs:element")
                  .a("name","configuration")
                    .element("xs:complexType")
                      .element("xs:all")
                      .a("minOccurs","0");
        return builder;
    }

    private void writeXsd(XMLBuilder2 builder, File targetFile) throws IOException {
        Properties outputProps = new Properties();
        outputProps.put(OutputKeys.METHOD, "xml");
        outputProps.put(OutputKeys.INDENT, "yes");
        outputProps.put(OutputKeys.ENCODING,"utf8");
        outputProps.put("{http://xml.apache.org/xslt}indent-amount", "2");
        try (FileWriter writer = new FileWriter(targetFile)) {
            builder.toWriter(writer, outputProps);
        }
    }

    static {
        String[] types = {
            String.class.getName(), "xs:string",
            Integer.class.getName(), "xs:integer",
            "int", "xs:integer",
            Long.class.getName(), "xs:long",
            "long", "xs:long",
            Float.class.getName(), "xs:float",
            "float", "xs:float",
            Double.class.getName(), "xs:double",
            "double", "xs:double",
            Boolean.class.getName(), "xs:boolean",
            "boolean", "xs:boolean",
            Date.class.getName(), "xs:date"
        };

        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < types.length; i+=2) {
            map.put(types[i], types[i + 1]);
        }
        SIMPLE_TYPE_LOOKUP = Collections.unmodifiableMap(map);
    }
}
