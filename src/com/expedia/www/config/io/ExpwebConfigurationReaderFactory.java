package com.expedia.www.config.io;

import com.expedia.www.config.ConfigEntry;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExpwebConfigurationReaderFactory implements ConfigurationReaderFactory {

    private String bizconfigPath;

    public ExpwebConfigurationReaderFactory(String bizconfigDirectoryPath) {
        this.bizconfigPath = bizconfigDirectoryPath;
        if (bizconfigPath.endsWith("/") || bizconfigPath.endsWith(File.separator)) {
            bizconfigPath = bizconfigPath.substring(0, bizconfigPath.length() - 1);
        }
    }

    @Override
    public ConfigurationReader getInstance(String configNamespace) {
        return new BizconfigReader(configNamespace);
    }

    public class BizconfigReader implements ConfigurationReader {

        Properties entries;
        String precedenceOrder;
        Iterator keysIterator;
        Pattern brandPattern = Pattern.compile("(\\?|\\s)brand\\s*=");
        Pattern sitePattern = Pattern.compile("(\\?|\\s)siteid\\s*=");

        public BizconfigReader(String configNamespace) {
            String path = bizconfigPath + File.separator + configNamespace.replaceAll("\\.", File.separator) + ".properties.xml";
            File bizconfig = new File(path);
            if (!bizconfig.exists()) {
                throw new RuntimeException("Unable to locate BizConfig file for namespace=" + configNamespace + ". Path checked was " + path);
            }

            try (FileInputStream fis = new FileInputStream(bizconfig)) {
                entries = new Properties();
                entries.loadFromXML(fis);
                precedenceOrder = (String) entries.remove("order");
                if (precedenceOrder == null) {
                    precedenceOrder = "siteid, locale";
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public List<String> getRuntimeAttributePrecedence() {
            return Arrays.asList(precedenceOrder.replaceAll("\\s", "").split(","));
        }

        @Override
        public ConfigEntry getNextEntry() {

            if (keysIterator == null) {
                keysIterator = entries.keySet().iterator();
            }

            ConfigEntry entry = null;
            if (keysIterator.hasNext()) {
                String key = (String) keysIterator.next();

                if (key != null) {
                    entry = parse(key, entries.getProperty(key));
                }
            }

            return entry;
        }

        private ConfigEntry parse(String keyString, String value) {

            ConfigEntry entry = new ConfigEntry();
            entry.value = value;

            Matcher brandMatcher = brandPattern.matcher(keyString);
            Matcher siteMatcher = sitePattern.matcher(keyString);

            if(brandMatcher.find() && siteMatcher.find()) {
                throw new RuntimeException("siteid and brand attribute cannot coexist in a key: " + keyString);
            }
            else {
                String[] attributes = keyString.split(" and |[?]");
                int len$ = attributes.length;

                for (int i$ = 0; i$ < len$; ++i$) {
                    String[] pair = attributes[i$].split("=");
                    if (pair.length > 1) {
                        String k = pair[0].trim();
                        String v = pair[1].trim();
                        if (k.equalsIgnoreCase("brand")) {
                            k = "siteid";
                        }

                        entry.runtimeAttributes.put(k, v);
                    } else {
                        entry.key = pair[0].trim().toLowerCase();
                    }
                }
            }

            return entry;
        }
    }

}
