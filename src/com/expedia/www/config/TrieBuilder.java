package com.expedia.www.config;

import com.expedia.www.config.io.ConfigurationReader;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

public class TrieBuilder {
    public ConfigTrie build(ConfigurationReader reader) throws IOException, JAXBException {

        List<String> runtimeAttributePrecedence = reader.getRuntimeAttributePrecedence();
        ConfigTrie trie = new ConfigTrie();
        trie.setAttributePrecedence(runtimeAttributePrecedence);

        ConfigEntry entry;
        while ((entry = reader.getNextEntry()) != null) {
            ConfigNode root = trie.getOrCreateRoot(entry.key);
            ConfigNode node = root;

            if (runtimeAttributePrecedence != null) {
                Iterator<String> i = runtimeAttributePrecedence.iterator();

                while (i.hasNext()) {
                    String attributeFromPrecedence = i.next();
                    String runtimeValue = entry.runtimeAttributes.get(attributeFromPrecedence);
                    if (runtimeValue != null) {
                        RuntimeAttribute attributeForTrie = new RuntimeAttribute(attributeFromPrecedence, runtimeValue);
                        node = node.locateOrAddEntryFor(attributeForTrie);
                    }
                }
                node.setValue(entry.value);
            }
            else {
                //TODO treat entry.runtimeAttributes; as defining trie entries in order
                throw new RuntimeException("TrieBuilder requires that ConfigurationReader explicitly defines Runtime Attribute precedence.");
            }
        }
      //  ObjectMapper mapper = new ObjectMapper().writer().withDefaultPrettyPrinter();
    /*    ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
      //  String json = ow.writeValueAsString(trie);
        Writer strWriter = new StringWriter();
        ow.writeValue(strWriter, trie.getAllRootNodes().entrySet());
        String userDataJSON = strWriter.toString();
        System.out.println(userDataJSON);*/


        //JSONObject ex = new JSONObject(trie);

//System.out.println(ex.toString());

          ObjectMapper mapper = new ObjectMapper();
       System.out.println(mapper.writeValueAsString(trie));
        return trie;
    }
}
