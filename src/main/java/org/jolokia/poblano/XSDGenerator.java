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
import java.util.List;
import java.util.Properties;

import javax.xml.transform.OutputKeys;

import com.jamesmurty.utils.XMLBuilder2;
import org.jolokia.poblano.model.ConfigElement;
import org.jolokia.poblano.model.Configuration;

/**
 * @author roland
 * @since 30/05/16
 */
public class XSDGenerator {

    public final static String XSD_NS = "http://www.w3.org/2001/XMLSchema";

    public void generate(File targetFile, String targetNamespaceUri, Configuration config) throws IOException {
        XMLBuilder2 builder = createXsdBuilder(targetNamespaceUri);
        generateElements(builder, config, config.getRootElements());
        writeXsd(builder, targetFile);
    }

    private void generateElements(XMLBuilder2 builder, Configuration config, List<ConfigElement> elements) {
        for (ConfigElement element : elements) {
            XMLBuilder2 elBuilder = builder.element("xs:element", XSD_NS)
                                           .a("name",element.getName())
                                           .a("type", element.getType());
            generateElements(elBuilder, config, element.getChildren());
        }
    }

    private XMLBuilder2 createXsdBuilder(String targetNamespaceUri) {
        XMLBuilder2 builder =
            XMLBuilder2
                .create("xs:schema", XSD_NS)
            .a("targetNamespace",targetNamespaceUri)
            .a("xmlns",targetNamespaceUri)
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
}
