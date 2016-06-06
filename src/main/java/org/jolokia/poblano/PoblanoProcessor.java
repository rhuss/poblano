package org.jolokia.poblano;

import java.io.File;
import java.io.IOException;
import java.util.*;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.*;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

import com.google.auto.service.AutoService;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.jolokia.poblano.annotation.PoblanoElement;
import org.jolokia.poblano.annotation.PoblanoIgnore;
import org.jolokia.poblano.model.ConfigElement;
import org.jolokia.poblano.model.Configuration;
import org.jolokia.poblano.model.EnumValueElement;


@SupportedAnnotationTypes({
    "org.apache.maven.plugins.annotations.Parameter",
    "org.apache.maven.plugins.annotations.Mojo"
})
@SupportedSourceVersion(SourceVersion.RELEASE_7)
@AutoService(Processor.class)
public class PoblanoProcessor extends AbstractProcessor {

    // Object type considered to be plain configuration options
    private static final Set<String> PLAIN_TYPES = new HashSet<>(Arrays.asList(
        String.class.getName(),
        Integer.class.getName(),
        Long.class.getName(),
        Float.class.getName(),
        Double.class.getName(),
        Boolean.class.getName()));

    private Messager messager;
    private Configuration config;

    private WildcardType wildcardTypeNull;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        messager = processingEnv.getMessager();
        config = new Configuration();
        wildcardTypeNull = processingEnv.getTypeUtils().getWildcardType(null, null);
    }

    /** {@inheritDoc} */
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element el : roundEnv.getElementsAnnotatedWith(Mojo.class)) {
            Mojo mojo = el.getAnnotation(Mojo.class);
            info("%s: Processing ...", mojo.name());
            while (el != null) {
                // TODO: Remember the inheritance hierarchie so that asciidoc generation
                // can create separate documents for base and super class
                extractConfig(config, mojo.name(), new Stack<ConfigElement>(), el);
                el = getSuperClassElement((TypeElement) el);
            }
        }

        if (roundEnv.processingOver()) {
            try {
                XSDGenerator xsdGenerator = new XSDGenerator();
                xsdGenerator.generate(new File("/tmp/test.xsd"), "http://fabric8.io/docker-maven-plugin", config);
            } catch (IOException e) {
                error("Error while writing XSD: %s", e.getMessage());
            }
        }
        return false;
    }

    // Recursively extract configuration from basic and complext typs
    private void extractConfig(Configuration config, String mojoGoal, Stack<ConfigElement> parents, Element element) {
        List<VariableElement> fields = ElementFilter.fieldsIn(element.getEnclosedElements());
        for (VariableElement field : fields) {
            Parameter paramAnno = field.getAnnotation(Parameter.class);
            PoblanoIgnore ignore = field.getAnnotation(PoblanoIgnore.class);
            if (ignore == null && paramAnno != null) {
                String type = extractType(field);
                String name = extractName(field);
                String documentation = extractDocumentation(field);
                ConfigElement parent = parents.empty() ? null : parents.peek();

                if (isEnum(field)) {
                    List<EnumValueElement> enumValueElements = extracEnumValuesFromFieldElement(field);
                    config.updateEnum(parent, enumValueElements, mojoGoal, name, type, paramAnno.defaultValue(), documentation);
                    continue;
                }

                // Check for a complex type to decent into
                ConfigElement current = config.updateElement(parent, mojoGoal, name, type, paramAnno.defaultValue(), documentation);

                // If a list kind of element insert an extra config element for the list type
                if (isArray(field) || isCollection(field)) {
                    current = insertListItemElement(config, field, current, name, mojoGoal);
                }

                // Recursively examine included type
                Element complexElement = extractComplexElement(field);
                if (complexElement != null) {
                    parents.push(current);
                    extractConfig(config, mojoGoal, parents, complexElement);
                    parents.pop();
                }
            }
        }
    }

    private List<EnumValueElement> extracEnumValuesFromFieldElement(VariableElement element) {
        if (isEnum(element)) {
            Elements elementUtils = processingEnv.getElementUtils();
            List<EnumValueElement> ret = new ArrayList<>();
            // Cast is save because of the isEnum() above
            DeclaredType type = (DeclaredType) element.asType();
            Element enumElement = type.asElement();
            for (Element valueElement : enumElement.getEnclosedElements()) {
                if (valueElement.getKind() == ElementKind.ENUM_CONSTANT) {
                    ret.add(new EnumValueElement(valueElement.getSimpleName().toString(),
                                                 elementUtils.getDocComment(valueElement)));
                }
            }
            return ret;
        } else {
            return null;
        }

    }

    private ConfigElement insertListItemElement(Configuration config, VariableElement field, ConfigElement current, String name, String mojo) {
        DeclaredType listItemType = extractListElementType(field);
        if (listItemType == null) {
            listItemType = (DeclaredType) processingEnv.getElementUtils().getTypeElement("java.lang.String").asType();
        }
        String itemType = listItemType.toString();
        String itemName = extractItemName(field, name, listItemType);
        current = config.updateElement(current, mojo, itemName, itemType, null, null);
        return current;
    }

    private String extractItemName(VariableElement field, String name, DeclaredType listItemType) {
        PoblanoElement poblanoElement = field.getAnnotation(PoblanoElement.class);
        if (poblanoElement != null) {
            return poblanoElement.value();
        } else {
            return itemTypeToName(name, listItemType);
        }
    }

    private String itemTypeToName(String listName, DeclaredType listItemType) {
        if (isComplexType(listItemType)) {
            String itemTypeName = listItemType.toString();
            int idx = itemTypeName.lastIndexOf(".");
            if (idx > -1) {
                return itemTypeName.substring(idx+1).toLowerCase();
            } else {
                return itemTypeName.toLowerCase();
            }
        } else {
            // Try to determine singular form if list is named in plural
            if (listName.endsWith("s")) {
                return listName.substring(0,listName.length() - 1).toLowerCase();
            } else {
                return "item";
            }
        }
    }

    private Element extractComplexElement(VariableElement field) {
        if (isArray(field) || isCollection(field)) {
            DeclaredType listElementType = extractListElementType(field);
            if (listElementType != null) {
                if (isComplexType(listElementType)) {
                    return listElementType.asElement();
                }
            }
        } else if (isComplexType(field.asType())) {
            DeclaredType declareFieldType = (DeclaredType) field.asType();
            return declareFieldType.asElement();
        }
        return null;
    }

    private String extractType(VariableElement field) {
        if (isMap(field)) {
            return ConfigElement.MAP_TYPE;
        } else if (isArray(field)) {
            return ConfigElement.ARRAY_TYPE;
        } else if (isCollection(field)) {
            return ConfigElement.COLLECTION_TYPE;
        } else {
            return field.asType().toString();
        }
    }

    // The given field is either an array or a collection. Try to find out the complex type
    // for them
    private DeclaredType extractListElementType(VariableElement field) {
        if (isArray(field)) {
            ArrayType arrayType = (ArrayType) field.asType();
            return (DeclaredType) arrayType.getComponentType();
        } else if (isCollection(field) || isArray(field)){
            DeclaredType declaredType = (DeclaredType) field.asType();

            List<? extends TypeMirror> typeArgs = declaredType.getTypeArguments();
            if (typeArgs != null && typeArgs.size() == 1) {
                return (DeclaredType) typeArgs.get(0);
            }
        }

        return null;
    }

    private Element getSuperClassElement(TypeElement el) {
        TypeMirror supertype =  el.getSuperclass();
        if (supertype.getKind() != TypeKind.NONE) {
            DeclaredType declared = (DeclaredType) supertype;
            return declared.asElement();
        } else {
            return null;
        }
    }

    private boolean isComplexType(TypeMirror type) {
        return type.getKind() == TypeKind.DECLARED && !PLAIN_TYPES.contains(type.toString());
    }

    private boolean isEnum(VariableElement element) {
        TypeMirror type = element.asType();
        if (type.getKind() == TypeKind.DECLARED) {
            return ((DeclaredType) type).asElement().getKind() == ElementKind.ENUM;
        }
        return false;
    }

    private boolean isArray(VariableElement field) {
        TypeMirror type = field.asType();
        return type.getKind() == TypeKind.ARRAY;
    }

    private boolean isCollection(VariableElement field) {
        return isAssignable(field,"java.util.Collection", 1);
    }

    private boolean isMap(VariableElement field) {
        return isAssignable(field,"java.util.Map", 2);
    }

    private boolean isAssignable(VariableElement field, String assignableTo, int nrWildcads) {
        TypeMirror type = field.asType();


        if (type.getKind() == TypeKind.DECLARED) {
            Types typeUtils = processingEnv.getTypeUtils();
            Elements elementUtils = processingEnv.getElementUtils();

            TypeElement assignableElement = elementUtils.getTypeElement(assignableTo);
            TypeMirror[] wildCards = new TypeMirror[nrWildcads];
            for (int i = 0; i < nrWildcads; i++) {
                wildCards[i] = wildcardTypeNull;
            }
            DeclaredType declaredType = typeUtils.getDeclaredType(assignableElement, wildCards);
            boolean isAssignable = typeUtils.isAssignable(type, declaredType);

            return isAssignable;
        } else {
            return false;
        }
    }

    private String extractDocumentation(VariableElement el) {
        return processingEnv.getElementUtils().getDocComment(el);
    }

    private String extractName(Element el) {
        return el.getSimpleName().toString();
    }

    private String extractType(Element el) {
        return el.getSimpleName().toString();
    }

    private void info(String format, Object ... args) {
        messager.printMessage(Diagnostic.Kind.NOTE, String.format(format,args));
    }

    private void error(String format, Object ... args) {
        messager.printMessage(Diagnostic.Kind.ERROR, String.format(format,args));
    }
}
