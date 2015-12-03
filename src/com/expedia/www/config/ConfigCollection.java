package com.expedia.www.config;

import com.expedia.www.config.io.ConfigurationReaderFactory;
import com.expedia.www.config.io.ExpwebConfigurationReaderFactory;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

public class ConfigCollection {

    private ConcurrentHashMap<String, ConfigTrie> configTrieMap = new ConcurrentHashMap<>();
    private boolean lazyLoadingEnabled = true; //TODO get in constructor
    private ConfigurationReaderFactory readerFactory = new ExpwebConfigurationReaderFactory("bizconfig"); //TODO get in constructor
    private TrieBuilder trieBuilder = new TrieBuilder(); //TODO get in constructor

    public ConfigTrie find(String configNamespace) throws IOException, JAXBException {

        ConfigTrie trie = configTrieMap.get(configNamespace);

        if (trie == null && lazyLoadingEnabled) {
            trie = trieBuilder.build(readerFactory.getInstance(configNamespace));
            configTrieMap.put(configNamespace, trie);
        }

        return trie;
    }
}
