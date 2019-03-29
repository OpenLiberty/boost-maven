/*******************************************************************************
 * Copyright (c) 2018, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.boost.common.config;

import static io.openliberty.boost.common.config.LibertyConfigConstants.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import io.openliberty.boost.common.BoostLoggerI;
import io.openliberty.boost.common.boosters.liberty.AbstractBoosterLibertyConfig;
import io.openliberty.boost.common.utils.BoostUtil;

/**
 * Create a Liberty server.xml
 *
 */
public class LibertyServerConfigGenerator {

    private final String serverPath;
    private final String libertyInstallPath;

    private final BoostLoggerI logger;

    private Document doc;
    private Element featureManager;
    private Element serverRoot;

    private Set<String> featuresAdded;

    private Properties bootstrapProperties;

    public LibertyServerConfigGenerator(String serverPath, BoostLoggerI logger) throws ParserConfigurationException {

        this.serverPath = serverPath;
        this.libertyInstallPath = serverPath + "/../../.."; // Three directories
                                                            // back from
                                                            // 'wlp/usr/servers/BoostServer'
        this.logger = logger;

        generateDocument();

        featuresAdded = new HashSet<String>();
        bootstrapProperties = new Properties();
    }

    private void generateDocument() throws ParserConfigurationException {
        DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();

        // Create top level server config element
        doc = docBuilder.newDocument();
        serverRoot = doc.createElement("server");
        serverRoot.setAttribute("description", "Liberty server generated by Liberty Boost");
        doc.appendChild(serverRoot);

        // Create featureManager config element
        featureManager = doc.createElement(FEATURE_MANAGER);
        serverRoot.appendChild(featureManager);
    }

    public Document getServerDoc() {
        return doc;
    }

    /**
     * Add an HTTP endpoint configuration for this server config instance.
     *
     * @param host
     *            The host to use for this server.
     * @param httpPort
     *            The HTTP port to use for this server.
     * @param httpsPort
     *            The HTTPS port to use for this server.
     */
    public void addHttpEndpoint(String host, String httpPort, String httpsPort) {

        Element httpEndpoint = doc.createElement(HTTP_ENDPOINT);
        httpEndpoint.setAttribute("id", DEFAULT_HTTP_ENDPOINT);

        if (host != null) {
            httpEndpoint.setAttribute("host", host);
        }
        if (httpPort != null) {
            httpEndpoint.setAttribute("httpPort", httpPort);
        }
        if (httpsPort != null) {
            httpEndpoint.setAttribute("httpsPort", httpsPort);
        }

        serverRoot.appendChild(httpEndpoint);
    }

    /**
     * Add a keystore definition for this server
     * 
     * @param keystore
     *            The keystore file name.
     * @param keystorePassword
     *            The keystore password
     * @param keystoreType
     *            The keystore type
     */
    public void addKeystore(Map<String, String> keystoreProps, Map<String, String> keyProps) {
        Element keystore = doc.createElement(KEYSTORE);
        keystore.setAttribute("id", DEFAULT_KEYSTORE);

        for (String key : keystoreProps.keySet()) {
            keystore.setAttribute(key, keystoreProps.get(key));
        }

        if (!keyProps.isEmpty()) {
            Element keyEntry = doc.createElement(KEY_ENTRY);

            for (String key : keyProps.keySet()) {
                keyEntry.setAttribute(key, keyProps.get(key));
            }

            keystore.appendChild(keyEntry);
        }

        serverRoot.appendChild(keystore);

    }

    /**
     * Add a Liberty feature to the server configuration
     *
     * @param featureName
     *            The full name of the Liberty feature to add.
     */
    public void addFeature(String featureName) {
        if (!featuresAdded.contains(featureName)) {
            Element feature = doc.createElement(FEATURE);
            feature.appendChild(doc.createTextNode(featureName));
            featureManager.appendChild(feature);
            featuresAdded.add(featureName);
        }

    }

    /**
     * Add a feature to the feature config
     *
     * @param features
     *            A list of features to add.
     */
    public void addFeatures(List<String> features) {

        for (String featureName : features) {
            addFeature(featureName);
        }
    }

    /**
     * Write the server.xml and bootstrap.properties to the server config
     * directory
     *
     * @throws TransformerException
     * @throws IOException
     */
    public void writeToServer() throws TransformerException, IOException {
        // Replace auto-generated server.xml
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(new File(serverPath + "/server.xml"));
        transformer.transform(source, result);

        // Generate bootstrap.properties
        if (!bootstrapProperties.isEmpty()) {

            OutputStream output = null;
            try {
                output = new FileOutputStream(serverPath + "/bootstrap.properties");
                bootstrapProperties.store(output, null);
            } finally {
                if (output != null) {
                    output.close();
                }
            }
        }
    }

    public void addBootstrapProperties(Properties properties) throws IOException {

        if (properties != null) {

            //Using this to hold the properties we want to encrypt and the type of encryption we want to use
            Map<String, String> propertiesToEncrypt = BoostProperties.getPropertiesToEncrypt();

            for (String key : properties.stringPropertyNames()) {
                String value = properties.getProperty(key);

                if (propertiesToEncrypt.containsKey(key) && value != null && !value.equals("")) {
                    //Getting properties that might not have been passed with the other properties that will be written to boostrap.properties
                    //Don't want to add certain properties to the boostrap properties so we'll grab them here
                    Properties supportedProperties = BoostProperties.getConfiguredBoostProperties(logger);
                    value = BoostUtil.encrypt(libertyInstallPath,
                                              properties.getProperty(key),
                                              propertiesToEncrypt.get(key),
                                              supportedProperties.getProperty(BoostProperties.AES_ENCRYPTION_KEY), 
                                              logger);
                }

                bootstrapProperties.put(key, value);
            }
        }
    }

    public void addBoosterConfig(AbstractBoosterLibertyConfig configurator) throws IOException {
        configurator.addServerConfig(getServerDoc());

        Properties properties = configurator.getServerProperties();
        addBootstrapProperties(properties);
    }

    public void addApplication(String appName) {
        Element appCfg = doc.createElement(APPLICATION);
        appCfg.setAttribute(CONTEXT_ROOT, "/");
        appCfg.setAttribute(LOCATION, appName + "." + WAR_PKG_TYPE);
        appCfg.setAttribute(TYPE, WAR_PKG_TYPE);
        serverRoot.appendChild(appCfg);

    }
}
