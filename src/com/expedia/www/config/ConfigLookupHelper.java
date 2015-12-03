package com.expedia.www.config;

import com.expedia.www.config.resolver.DefaultResolver;

import javax.xml.bind.JAXBException;
import java.io.IOException;

public class ConfigLookupHelper {

    ConfigCollection collection;
    DefaultResolver resolver;

    public String lookup(String configPropertyName, String configNamespace, RuntimeContext context) throws IOException, JAXBException {
        ConfigTrie trie = collection.find(configNamespace);
        return resolver.resolve(trie, configPropertyName, context);
    }

}
