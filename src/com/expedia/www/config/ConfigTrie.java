package com.expedia.www.config;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ConfigTrie {

    Map<String, ConfigNode> rootNodes = new ConcurrentHashMap<>();
    private List<String> attributePrecedence;
    LinkedHashMap<RuntimeAttribute, ConfigNode> getChildren() {
        return  children;
    }
    private LinkedHashMap<RuntimeAttribute, ConfigNode> children = new LinkedHashMap<>(); //TODO replace with a Concurrent AND Linked Map-type data structure

    //package access is deliberate
    synchronized ConfigNode getOrCreateRoot(String configPropertyName) {
        configPropertyName = configPropertyName.toLowerCase();
        ConfigNode root = rootNodes.get(configPropertyName);
        if (root == null) {
            root = new ConfigNode();
            rootNodes.put(configPropertyName, root);
        }
        return root;
    }

    Map<String, ConfigNode> getAllRootNodes() {
        return rootNodes;
    }

    public ConfigNode getRoot(String configPropertyName) {
        return rootNodes.get(configPropertyName.toLowerCase());
    }

    public void setAttributePrecedence(List<String> attributePrecedence) {
        this.attributePrecedence = Collections.unmodifiableList(attributePrecedence);
    }


    public List<String> getAttributePrecedence() {
        return attributePrecedence;
    }


}
