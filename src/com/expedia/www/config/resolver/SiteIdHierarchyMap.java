package com.expedia.www.config.resolver;

import com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.*;
import java.io.*;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Loads the siteid hierarchy and keeps it in memory. Loads it only once.
 * Provides programmatic access (as a map of child->parent) in a flat map format.
 * TODO must be made a part of the hotloading story
 *
 * @author <a href="mailto:agodika@expedia.com>anuraag - agodika@expedia.com</a>
 */
public class SiteIdHierarchyMap
{

    public Document m_tree;

    private String contentRoot;
    private String brandsDirPath;
    public Map<String, String> m_siteIdAncestorMap = new HashMap<String, String>();

    public SiteIdHierarchyMap(String brandsDir){
        this.brandsDirPath = brandsDir;

        if (this.brandsDirPath.startsWith("file:")){
            this.brandsDirPath = this.brandsDirPath.substring(5);
        }

        String[] brandFiles = new File(brandsDirPath).list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                if(name.toLowerCase(Locale.US).endsWith(".xml"))
                    return true;
                else
                    return false;
            }
        });

        for(String brandFile : brandFiles){
            String brand = brandFile.split("\\.xml")[0];
            String brandFilePath = brandsDirPath + File.separator + brandFile;
            load(brand, brandFilePath);
        }
    }

    public SiteIdHierarchyMap(String brand, File file)
    {
        load(brand, file);
    }

    public SiteIdHierarchyMap(String brand, InputStream is)
    {
        load(brand, is);
    }

    /*
     * Marking final to solve PMD ConstructorCallsOverridableMethod error
     */
    public final void load(String brand, File siteIdXmlFile)
    {
        try
        {
            load(brand, new FileInputStream(siteIdXmlFile));
        }
        catch (FileNotFoundException e)
        {
            throw new RuntimeException("Unable to open siteid heirarchy file for parsing", e);
        }
    }

    /*
     * Marking final to solve PMD ConstructorCallsOverridableMethod error
     */
    public final void load(String brand, String brandXmlFilePath)
    {
        InputStream is = getClass().getClassLoader().getResourceAsStream(brandXmlFilePath);
        if (is != null)
        {
            load(brand, is);
        }
        else
        {
            File file = new File(brandXmlFilePath);
            load(brand, file);
        }
    }


    /**
     * Loads the siteid heirarchy in memory from the given fileName.
     */
    /*
     * Marking final to solve PMD ConstructorCallsOverridableMethod error
     */
    public final void load(String brand, InputStream is)
    {
        Document doc = openDocument(is);

        m_tree = doc;

        XPathFactory factory = XPathFactory.newInstance();
        XPath xpath = factory.newXPath();
        XPathExpression expr;
        try
        {
            expr = xpath.compile("//site-hierarchy//site");

            Object result = expr.evaluate(doc, XPathConstants.NODESET);
            NodeList nodes = (NodeList) result;

//            NodeList nodes = doc.getElementsByTagName("treenode");

            int size = nodes.getLength();

            for (int i = 0 ; i < size ; i++)
            {
                Node node = nodes.item(i);
                NamedNodeMap attributes = node.getAttributes();
                Node parent = node.getParentNode();

                String name;
                String parentName;

                name = attributes.getNamedItem("id").getNodeValue();
                Node parentNode = parent.getAttributes().getNamedItem("id");

                if(parentNode != null){
                    parentName = parentNode.getNodeValue();
                }else{
                    //parentName = "1407";
                    parentName = brand;
                    //parentName = null;
                }
                m_siteIdAncestorMap.put(name, parentName);

            }

        }
        catch (XPathExpressionException e)
        {
            throw new RuntimeException("Unable to parse siteid hierarchy", e);
        }

    }

    public static Document openDocument(InputStream is)
    {

        DocumentBuilderFactory    documentBuilderFactory;
        DocumentBuilder            documentBuilder;
        Document                document;

        // Step 1: Create a DocumentBuilderFactory and configure it

        try
        {
            documentBuilderFactory = new DocumentBuilderFactoryImpl();
            documentBuilderFactory.setNamespaceAware(false);
            documentBuilderFactory.setValidating(false);
            documentBuilderFactory.setXIncludeAware(false);

            // Optional configuration: set various configuration options
            documentBuilderFactory.setIgnoringComments(true);
            documentBuilderFactory.setIgnoringElementContentWhitespace(true);
            documentBuilder = documentBuilderFactory.newDocumentBuilder();

            // Set an error handler to process parsing errors.
            documentBuilder.setErrorHandler(new DefaultHandler());

            document = documentBuilder.parse(is);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Error while parsing XML file to DOM", e);
        }

        return document;
    }

    public String getParentValue(String key)
    {
        return m_siteIdAncestorMap.get(key);
    }

    //    @Override
    public String put(String key, String value)
    {
        return m_siteIdAncestorMap.put(key, value);
    }

    public String getContentRoot()
    {
        return contentRoot;
    }

    public void setContentRoot(String contentRoot)
    {
        this.contentRoot = contentRoot;
    }
}
