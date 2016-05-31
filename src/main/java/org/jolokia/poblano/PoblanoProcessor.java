package org.jolokia.poblano;

import java.io.File;
import java.io.IOException;
import java.util.*;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

import com.google.auto.service.AutoService;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.jolokia.poblano.model.ConfigElement;
import org.jolokia.poblano.model.Configuration;


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

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        messager = processingEnv.getMessager();
        config = new Configuration();
    }

    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element el : roundEnv.getElementsAnnotatedWith(Mojo.class)) {
            Mojo mojo = el.getAnnotation(Mojo.class);
            info("%s: Processing ...", mojo.name());
            while (el != null) {
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

    private Element getSuperClassElement(TypeElement el) {
        TypeMirror supertype =  el.getSuperclass();
        if (supertype.getKind() != TypeKind.NONE) {
            DeclaredType declared = (DeclaredType) supertype;
            return declared.asElement();
        } else {
            return null;
        }
    }

    private void extractConfig(Configuration config, String mojo, Stack<ConfigElement> parents, Element element) {
        List<VariableElement> fields = ElementFilter.fieldsIn(element.getEnclosedElements());
        for (VariableElement field : fields) {
            Parameter paramAnno = field.getAnnotation(Parameter.class);
            if (paramAnno != null) {
                String type = field.asType().toString();
                String name = extractName(field);
                String documentation = extractDocumentation(field);
                ConfigElement parent = parents.empty() ? null : parents.peek();
                ConfigElement current = config.update(parent, mojo, name, type, paramAnno.defaultValue(), documentation);
                if (isComplexType(field)) {
                    DeclaredType declareFieldType = (DeclaredType) field.asType();
                    parents.push(current);
                    extractConfig(config, mojo, parents, declareFieldType.asElement());
                    parents.pop();
                }
            }
        }
    }

    private boolean isComplexType(VariableElement field) {
        TypeMirror type = field.asType();
        return type.getKind() == TypeKind.DECLARED && !PLAIN_TYPES.contains(type.toString());
    }

    private ConfigElement extractParent(Configuration config, Element parentEl) {
        if (parentEl != null) {
            return config.get(extractName(parentEl), extractType(parentEl));
        } else {
            return null;
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
