package com.tblf.parsing;

import com.tblf.Model.Model;
import com.tblf.Model.ModelFactory;
import com.tblf.Model.ModelPackage;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.gmt.modisco.java.*;
import org.eclipse.gmt.modisco.java.Package;
import org.eclipse.gmt.modisco.java.emf.JavaFactory;
import org.eclipse.gmt.modisco.java.emf.JavaPackage;
import org.eclipse.gmt.modisco.omg.kdm.kdm.KdmPackage;
import org.eclipse.gmt.modisco.omg.kdm.source.SourcePackage;
import org.eclipse.modisco.java.composition.javaapplication.*;
import org.eclipse.modisco.kdm.source.extension.ASTNodeSourceRegion;
import org.eclipse.modisco.kdm.source.extension.CodeUnit2File;
import org.eclipse.modisco.kdm.source.extension.ExtensionPackage;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Created by Thibault on 02/10/2017.
 */
public class TraceParserTest {
    private File trace;
    private ResourceSet resourceSet;

    @Before
    public void setUpRealCode() throws IOException {
        resourceSet = new ResourceSetImpl();
        Logger rootLogger = LogManager.getLogManager().getLogger("");
        rootLogger.setLevel(Level.FINE);
        for (Handler h : rootLogger.getHandlers()) {
            h.setLevel(Level.FINE);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("&:com.tblf.SimpleProject.AppTest:<init>\n");
        sb.append("&:com.tblf.SimpleProject.AppTest:testApp\n");
        sb.append("%:com.tblf.SimpleProject.App:method\n");
        sb.append("?:5\n");
        sb.append("?:6");

        trace = File.createTempFile("tmpTrace", ".extr");

        Files.write(trace.toPath(), sb.toString().getBytes());

        EPackage.Registry.INSTANCE.put(JavaPackage.eNS_URI, JavaPackage.eINSTANCE);
        EPackage.Registry.INSTANCE.put(JavaapplicationPackage.eNS_URI, JavaapplicationPackage.eINSTANCE);
        EPackage.Registry.INSTANCE.put(KdmPackage.eNS_URI, KdmPackage.eINSTANCE);
        EPackage.Registry.INSTANCE.put(SourcePackage.eNS_URI, SourcePackage.eINSTANCE);
        EPackage.Registry.INSTANCE.put(ModelPackage.eNS_URI, ModelPackage.eINSTANCE);

        Resource.Factory.Registry reg = Resource.Factory.Registry.INSTANCE;
        Map<String, Object> m = reg.getExtensionToFactoryMap();
        m.put("xmi", new XMIResourceFactoryImpl());

        File file = new File("src/test/resources/models/simpleProject");
        Files.walk(file.toPath()).filter(path -> path.toString().endsWith(".xmi")).forEach(path -> {

            System.out.println("now adding: "+path.getFileName().toString());
            try {
                Resource resource = resourceSet.getResource(URI.createURI(path.toUri().toURL().toString()), true);
                System.out.println(resource);
                resourceSet.getResources().add(resource);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }

        });

        Assert.assertTrue(resourceSet.getResources().stream().filter(resource -> resource.getContents().get(0) == null).collect(Collectors.toList()).isEmpty());
    }

    @Test
    public void checkParse() throws IOException {
        TraceParser traceParser = new TraceParser(trace, resourceSet);
        Model model = traceParser.parse();

        File file = new File("src/test/resources/myAnalysisModel.xmi");

        if (file.exists()) {
            file.delete();
        }

        file.createNewFile();
        Resource resource = resourceSet.createResource(URI.createURI(file.toURI().toURL().toString()));

        resource.getContents().add(model);

        model.getAnalyses().forEach(System.out::println);
        resource.save(Collections.EMPTY_MAP);
    }

}