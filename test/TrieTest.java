import com.expedia.www.config.ConfigCollection;
import com.expedia.www.config.ConfigTrie;
import com.expedia.www.config.RuntimeContext;
import com.expedia.www.config.resolver.DefaultResolver;

import javax.xml.bind.JAXBException;
import java.io.IOException;

public class TrieTest {

    public static void main(String[] args) throws IOException, JAXBException {
        ConfigCollection configCollection = new ConfigCollection();
        ConfigTrie configTrie = configCollection.find("com.expedia.www.social.ui.triptrails.config.TripTrailConfig");

        RuntimeContext context = new RuntimeContext() {
            @Override
            public String getValue(String runtimeAttributeName) {
                if ("siteid".equals(runtimeAttributeName)) {
                    return "1";
                }
                return null;
            }
        };

        RuntimeContext siteIdFooContext = new RuntimeContext() {
            @Override
            public String getValue(String runtimeAttributeName) {
                if ("siteid".equals(runtimeAttributeName)) {
                    return "foo";
                }
                return null;
            }
        };

        DefaultResolver resolver = new DefaultResolver();

        String value = resolver.resolve(configTrie, "tripTrailsEnabled", context);
        value = resolver.resolve(configTrie, "tripTrailsEnabled", siteIdFooContext);
        value = resolver.resolve(configTrie, "foo", context);
        value = resolver.resolve(configTrie, "testEmail", context);
    }

}
