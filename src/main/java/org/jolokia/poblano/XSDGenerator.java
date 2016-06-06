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

import java.io.*;
import java.util.*;

import javax.xml.transform.OutputKeys;

import com.jamesmurty.utils.XMLBuilder2;
import org.jolokia.poblano.model.ConfigElement;
import org.jolokia.poblano.model.Configuration;
import org.jolokia.poblano.model.EnumValueElement;
import org.w3c.tidy.Tidy;

/**
 * @author roland
 * @since 30/05/16
 */
public class XSDGenerator {

    public static final String XSD_NS = "http://www.w3.org/2001/XMLSchema";
    private static final String XSD_HTML = "http://www.w3.org/1999/xhtml";

    // Mapping from Java class to simple types
    private static final Map<String,String> SIMPLE_TYPE_LOOKUP;

    private final Tidy tidy;

    public XSDGenerator() {
        tidy = createTidy();
    }

    public void generate(File targetFile, String targetNamespaceUri, Configuration config) throws IOException {
        XMLBuilder2 builder = createXsdBuilder(targetNamespaceUri);
        generateElements(builder, config, config.getRootElements());
        writeXsd(builder, targetFile);
    }

    private void generateElements(XMLBuilder2 builder, Configuration config, List<ConfigElement> elements) {
        for (ConfigElement element : elements) {
            XMLBuilder2 elBuilder = builder.element("xs:element", XSD_NS).a("name", element.getName());
            addDocumentation(elBuilder, element.getDocumentation());
            if (element.isMap()) {
                addMap(elBuilder);
            } else if (element.isEnum()) {
                addEnum(elBuilder, element);
            } else if (element.isComplexType()) {
                // Call recursively back to this methd ...
                addComplexType(elBuilder, config, element);
            } else {
                addSimpleType(elBuilder, element);
            }
        }
    }

    private void addSimpleType(XMLBuilder2 elBuilder, ConfigElement element) {
        String simpleType = convertSimpleType(element.getType());
        elBuilder.a("type", simpleType);
    }

    private void addComplexType(XMLBuilder2 elBuilder, Configuration config, ConfigElement element) {
        XMLBuilder2 typeBuilder =
            elBuilder
                .element("xs:complexType");
        if (element.isListLike()) {
            typeBuilder = typeBuilder.element("xs:sequence").a("minOccurs", "0");
        } else {
            typeBuilder = typeBuilder.element("xs:choice").a("maxOccurs", "unbounded");
        }

        generateElements(typeBuilder, config, element.getChildren());
    }

    private void addEnum(XMLBuilder2 elBuilder, ConfigElement element) {
        XMLBuilder2 innerBuilder = elBuilder
            .element("xs:simpleType")
            .element("xs:restriction")
              .a("base","xs:string");
        for (EnumValueElement enumValElement : element.getEnumValues()) {
            XMLBuilder2 valueBuilder = innerBuilder.element("xs:enumeration").a("value",enumValElement.getValue());
            String doc = enumValElement.getDocumentation();
            if (doc != null) {
                addDocumentation(valueBuilder,doc);
            }
        }
    }

    private void addMap(XMLBuilder2 elBuilder) {
        elBuilder
            .element("xs:complexType")
              .element("xs:sequence")
                 .element("xs:any").a("minOccurs", "0").a("maxOccurs", "unbounded");
    }

    private void addDocumentation(XMLBuilder2 builder, String doc) {
        if (doc != null && doc.trim().length() != 0) {
            XMLBuilder2 docBuilder = XMLBuilder2.parse("<div xmlns=\"" + XSD_HTML + "\">" + tidy(doc.trim()).trim() + "</div>");
            builder.element("xs:annotation").element("xs:documentation").importXMLBuilder(docBuilder);
        }
    }

    private String addNamespace(String namespace, String text) {
        return text.replaceAll("(</?)([^>/]+)(/?>)","$1" + namespace + ":$2$3");
    }

    private String tidy(String doc) {
        StringReader reader =  new StringReader(doc);
        StringWriter writer = new StringWriter();
        tidy.parse(reader, writer);
        return writer.toString();
    }

    private String convertSimpleType(String type) {
        String ret = SIMPLE_TYPE_LOOKUP.get(type);
        return ret != null ? ret : "xs:string";
    }

    private XMLBuilder2 createXsdBuilder(String targetNamespaceUri) {
        XMLBuilder2 builder =
            XMLBuilder2
                .create("xs:schema", XSD_NS)
                .a("targetNamespace",targetNamespaceUri)
                .a("xmlns",targetNamespaceUri)
                .a("xmlns:html",XSD_HTML)
                .a("elementFormDefault","qualified");
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

    private Tidy createTidy() {
        Tidy tidy = new Tidy();
        tidy.setXHTML(true);
        tidy.setQuiet(true);
        tidy.setShowWarnings(false);
        tidy.setPrintBodyOnly(true);
        tidy.setEncloseText(true);
        tidy.setTrimEmptyElements(true);
        return tidy;
    }
}
