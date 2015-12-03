package com.expedia.www.config.resolver;

import com.expedia.www.config.ConfigNode;
import com.expedia.www.config.ConfigTrie;
import com.expedia.www.config.RuntimeAttribute;
import com.expedia.www.config.RuntimeContext;

import java.util.List;

public class DefaultResolver {

    public String resolve(ConfigTrie configTrie, String configPropertyName, RuntimeContext context) {
        ConfigNode root = configTrie.getRoot(configPropertyName);
        if (root == null) {
            return null;
        }
        else {
            String value = resolveInternal(root, context, configTrie.getAttributePrecedence());
            return value == null ? null : value.trim();
        }
    }

    protected String resolveInternalFromChildNodes(ConfigNode root, RuntimeContext context, List<String> attributes) {
        for (int $i=0; $i < attributes.size(); $i++) {
            String runtimeAttributeName = attributes.get($i);

            if (root.hasChildRuntimeAttribute(runtimeAttributeName)) {
                String runtimeAttributeValue = context.getValue(runtimeAttributeName);
                RuntimeAttribute runtimeAttributeWithValue = new RuntimeAttribute(runtimeAttributeName, runtimeAttributeValue);

                ConfigNode child = root.findChild(runtimeAttributeWithValue);
                if (child != null) {
                    String value = resolveInternal(child, context, attributes.subList($i+1, attributes.size()));
                    if (value != null) {
                        return value;
                    }
                }
            }
        }
        return null;
    }

    protected String resolveInternal(ConfigNode root, RuntimeContext context, List<String> attributes) {
        String value = resolveInternalFromChildNodes(root, context, attributes);
        return value == null ? root.getValue() : value;
    }

}
