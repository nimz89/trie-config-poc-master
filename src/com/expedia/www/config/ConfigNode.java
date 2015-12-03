package com.expedia.www.config;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;

public class ConfigNode {
    private String value;
    private ConfigNode parent;
    private LinkedHashMap<RuntimeAttribute, ConfigNode> children = new LinkedHashMap<>(); //TODO replace with a Concurrent AND Linked Map-type data structure
    private Set<String> childNodeNames = new HashSet<>();

    public void setValue(String value) {
        if (this.value != null) {
            throw new RuntimeException("ConfigNode already has a value set");
        }
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public ConfigNode getParent() {
        return parent;
    }

    LinkedHashMap<RuntimeAttribute, ConfigNode> getChildren() {
        return children;
    }

    public boolean hasChildRuntimeAttribute(String name) {
        return childNodeNames.contains(name);
    }

    public ConfigNode findChild(RuntimeAttribute attribute) {
        return children.get(attribute);
    }

    public synchronized ConfigNode locateOrAddEntryFor(RuntimeAttribute attribute) {

        ConfigNode node = children.get(attribute);
        if (node == null) {
            node = new ConfigNode();
            node.parent = this;
            children.put(attribute, node);
            childNodeNames.add(attribute.name);
        }
        return node;
    }
}
