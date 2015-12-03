package com.expedia.www.config.resolver;

import com.expedia.www.config.ConfigNode;
import com.expedia.www.config.ConfigTrie;
import com.expedia.www.config.RuntimeContext;

import java.util.List;

public class ExpwebSiteHierarchyAwareResolver extends DefaultResolver {

    SiteIdHierarchyMap siteIdHierarchyMap;

    public ExpwebSiteHierarchyAwareResolver(SiteIdHierarchyMap siteIdHierarchyMap) {
        this.siteIdHierarchyMap = siteIdHierarchyMap;
    }

    public String resolve(ConfigTrie configTrie, String configPropertyName, RuntimeContext context) {
        ConfigNode root = configTrie.getRoot(configPropertyName);
        if (root == null) {
            return null;
        }
        else {
            String value = resolveInternalRoot(root, context, configTrie.getAttributePrecedence());
            return value == null ? null : value.trim();
        }
    }


    protected String resolveInternalRoot(ConfigNode root, RuntimeContext context, List<String> attributes) {
        String value = resolveInternalFromChildNodes(root, context, attributes);

        if (value == null) {
            final String siteId = context.getValue("siteid");
            if (siteId != null) {
                String parentSiteId = siteIdHierarchyMap.getParentValue(siteId);
                if (parentSiteId != null) {
                    value = resolveInternalRoot(root,
                            runtimeAttributeName -> {
                                if ("siteid".equals(runtimeAttributeName)) {
                                    return parentSiteId;
                                }
                                else {
                                    return context.getValue(runtimeAttributeName);
                                }
                            },
                            attributes);
                }
            }
        }

        return value == null ? root.getValue() : value;
    }

}
