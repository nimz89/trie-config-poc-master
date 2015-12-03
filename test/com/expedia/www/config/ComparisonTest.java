package com.expedia.www.config;

import com.expedia.e3.platform.foundation.configuration.BaseConfiguration;
import com.expedia.e3.platform.foundation.configuration.IStripingContext;
import com.expedia.e3.platform.foundation.configuration.StripingAttribute;
import com.expedia.www.config.io.ConfigurationReaderFactory;
import com.expedia.www.config.io.ExpwebConfigurationReaderFactory;
import com.expedia.www.config.resolver.DefaultResolver;
import com.expedia.www.config.resolver.ExpwebSiteHierarchyAwareResolver;
import com.expedia.www.config.resolver.SiteIdHierarchyMap;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.*;

public class ComparisonTest {

    interface Command<T> {
        void execute(T t) throws IOException, JAXBException;
    }

    static class StcsConfiguration extends BaseConfiguration {
        public void loadEntries(String propertyFile) {
            super.setConfigurationData(super.loadEntriesFrom(propertyFile));
        }
    }

    static class TestStripingContext implements IStripingContext {

        Map<String, String> currentEnvValues;

        @Override
        public Object getAttributeValue(StripingAttribute stripingAttribute) {
            return currentEnvValues.get(stripingAttribute.getAttributeName());
        }

        @Override
        public Object getParentValue(StripingAttribute stripingAttribute, Object o) {
            if (StripingAttribute.SITEID.equals(stripingAttribute.getAttributeName())) {
                return siteIdHierarchyMap.getParentValue((String)o);
            }
            return null;
        }
    }

    static class TestRuntimeContext implements RuntimeContext {

        Map<String, String> currentEnvValues;

        @Override
        public String getValue(String runtimeAttributeName) {
            return currentEnvValues.get(runtimeAttributeName);
        }
    }

    static class CompareTrieWithStcs implements Command<File> {

        ThreadMXBean threadmxbean = ManagementFactory.getThreadMXBean();
        long stcsLoadTimeNanos, trieLoadTimeNanos, stcsResolveTimeNanos, trieResolveTimeNanos, numResolves;
        long numCompareEqual;

        //TODO replace DefaultResolver with ExpwebResolver and simultaneously update TestStripingContext to use DefaultStripingContext type logic
        DefaultResolver defaultResolver = new DefaultResolver();
        ExpwebSiteHierarchyAwareResolver expwebResolver = new ExpwebSiteHierarchyAwareResolver(siteIdHierarchyMap);

        public CompareTrieWithStcs() {
            if (threadmxbean.isCurrentThreadCpuTimeSupported()) {
                if (!threadmxbean.isThreadCpuTimeEnabled()) {
                    threadmxbean.setThreadCpuTimeEnabled(true);
                }
            }
        }

        @Override
        public String toString() {

            String loadTimeImprovement = (((double) stcsLoadTimeNanos / trieLoadTimeNanos)) + " times";
            String resolveTimeImprovement = (((double) stcsResolveTimeNanos / trieResolveTimeNanos)) + " times";

            return "CompareTrieWithStcs { " +
                    "stcsLoadTimeNanos=" + stcsLoadTimeNanos +
                    ", trieLoadTimeNanos=" + trieLoadTimeNanos +
                    ", stcsResolveTimeNanos=" + stcsResolveTimeNanos +
                    ", trieResolveTimeNanos=" + trieResolveTimeNanos +
                    ", numResolves=" + numResolves +
                    ", numCompareEqual=" + numCompareEqual +
                    '}' +
                    "\n Faster To Load  = " + (stcsLoadTimeNanos > trieLoadTimeNanos ? "Trie " : "Stcs ") + loadTimeImprovement +
                    "\n Faster To Resolve  = " + (stcsResolveTimeNanos > trieResolveTimeNanos ? "Trie " : "Stcs ") + resolveTimeImprovement +
                    "\n Miss Percentage  = " + (((numResolves - numCompareEqual) / (double) numResolves) * 100) + "%"
                    ;
        }

        BaseConfiguration loadStcsConfig(File file) {
            long starttime = threadmxbean.getCurrentThreadCpuTime();
            StcsConfiguration stcsConfig = new StcsConfiguration();
            stcsConfig.loadEntries(file.getAbsolutePath());
            long endtime = threadmxbean.getCurrentThreadCpuTime();
            stcsLoadTimeNanos += (endtime - starttime);

            return stcsConfig;
        }

        ConfigTrie loadTrieConfig(String namespace) throws IOException, JAXBException {
            long starttime = threadmxbean.getCurrentThreadCpuTime();
            ConfigTrie configTrie = new TrieBuilder().build(readerFactory.getInstance(namespace));
            long endtime = threadmxbean.getCurrentThreadCpuTime();
            trieLoadTimeNanos += (endtime - starttime);

            return configTrie;
        }

        @Override
        public void execute(File file) throws IOException, JAXBException {
            String namespace = file.getAbsolutePath().replace(bizConfigDirectory.getAbsolutePath(), "").replace(".properties.xml", "");

            /* Load Trie first, so that any delay due to file being cached on disk controller gets attributed to Trie instead of STCS*/
            ConfigTrie configTrie = loadTrieConfig(namespace);
            TestRuntimeContext testRuntimeContext = new TestRuntimeContext();

            BaseConfiguration stcsConfig = loadStcsConfig(file);
            TestStripingContext testStripingContext = new TestStripingContext();

            Map<String, ConfigNode> configNodeMap = configTrie.getAllRootNodes();
            Set<String> allProperties = configNodeMap.keySet();

            for (String key : allProperties) {
                ConfigNode root = configNodeMap.get(key);
                Set<Map<String, String>> allEnvironmentPropertiesPermutations = new LinkedHashSet<>();

                buildEnvPropPermutations(root, allEnvironmentPropertiesPermutations, new HashMap<String, String>());

                for (Map<String, String> environmentValues : allEnvironmentPropertiesPermutations) {
                    testStripingContext.currentEnvValues = environmentValues;
                    testRuntimeContext.currentEnvValues = environmentValues;

                    String stcsValue = resolveViaStcs(stcsConfig, key, testStripingContext);
                    String trieValue = resolveViaTrie(configTrie, key, testRuntimeContext);
                    numResolves++;

                    boolean comparedEqual = ((stcsValue == null && trieValue == null) || stcsValue.equals(trieValue));
                    numCompareEqual += comparedEqual ? 1 : 0;

                    if (!comparedEqual) {
                        System.out.println("MISS : " + namespace + " :: " + key + " :@: " + environmentValues);
                        System.out.println("StcsValue = " + stcsValue + "; TrieValue = " + trieValue);
                    }
                }
            }
        }

        private String resolveViaTrie(ConfigTrie configTrie, String key, TestRuntimeContext testRuntimeContext) {
            long starttime = threadmxbean.getCurrentThreadCpuTime();
            String value = expwebResolver.resolve(configTrie, key, testRuntimeContext);
            long endtime = threadmxbean.getCurrentThreadCpuTime();
            trieResolveTimeNanos += (endtime - starttime);
            return value;
        }

        private String resolveViaStcs(BaseConfiguration stcsConfig, String key, TestStripingContext testStripingContext) {
            long starttime = threadmxbean.getCurrentThreadCpuTime();
            String value = stcsConfig.getConfigurationValue(key, testStripingContext);
            long endtime = threadmxbean.getCurrentThreadCpuTime();
            stcsResolveTimeNanos += (endtime - starttime);
            return value;
        }

        private void buildEnvPropPermutations(ConfigNode root, Set<Map<String, String>> allEnvironmentPropertiesPermutations, HashMap<String, String> map) {
            if (map != null) {
                allEnvironmentPropertiesPermutations.add(map);
            }

            if (! root.getChildren().isEmpty()) {
                for (RuntimeAttribute attribute : root.getChildren().keySet()) {

                    if (allEnvironmentPropertiesPermutations.size() > 75000) {
//                        System.out.println(allEnvironmentPropertiesPermutations.toString().replaceAll("},", "},\n"));
//                        System.exit(1);

                        HashMap<String, String> newMapOfAttributes = new HashMap<>();
                        if (map != null) {
                            newMapOfAttributes.putAll(map);
                        }
                        newMapOfAttributes.put(attribute.name, attribute.value);
                        allEnvironmentPropertiesPermutations.add(newMapOfAttributes);
                        buildEnvPropPermutations(root.getChildren().get(attribute), allEnvironmentPropertiesPermutations, newMapOfAttributes);
                    }
                    else {
                        Set<Map<String, String>> newPermutationSet = new LinkedHashSet<>();
                        HashMap<String, String> newMapOfAttributes = null;

                        for (Map<String, String> existingMap: allEnvironmentPropertiesPermutations) {
                            newMapOfAttributes = new HashMap<>();
                            newMapOfAttributes.putAll(existingMap);
                            newMapOfAttributes.put(attribute.name, attribute.value);
                            newPermutationSet.add(newMapOfAttributes);
                        }

                        allEnvironmentPropertiesPermutations.addAll(newPermutationSet);

                        buildEnvPropPermutations(root.getChildren().get(attribute), allEnvironmentPropertiesPermutations, newMapOfAttributes);
                    }
                }
            }
        }
    }


    static String bizConfigFolder = "bizconfig";
    static File bizConfigDirectory = new File(bizConfigFolder);
    static ConfigurationReaderFactory readerFactory = new ExpwebConfigurationReaderFactory(bizConfigFolder);
    static SiteIdHierarchyMap siteIdHierarchyMap = new SiteIdHierarchyMap("global");


    public static void main(String args[]) throws IOException, JAXBException {

        CompareTrieWithStcs comparer = new CompareTrieWithStcs();

//        File file = new File("bizconfig/com/expedia/www/domain/config/FreeCancellationConfig.properties.xml");
//        comparer.execute(file);

        forEachBizConfigFile(bizConfigDirectory, comparer);

        System.out.println(comparer);
    }

    private static void forEachBizConfigFile(File rootDirectory, Command<File> command) throws IOException, JAXBException {
        LinkedList queue = new LinkedList();
        queue.add(rootDirectory);
        File file = null;

        int count = 0;

        while((file = (File)queue.poll()) != null) {
            if(file.isDirectory()) {
                File[] var11 = file.listFiles();
                int var12 = var11.length;

                for(int var13 = 0; var13 < var12; ++var13) {
                    File child = var11[var13];
                    queue.add(child);
                }
            }
            else if(file.isFile() && file.getName().endsWith(".properties.xml")) {

                command.execute(file);
//                if (count++ > 100) {
//                    return;
//                }
            }
        }
    }

}
