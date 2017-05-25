package io.vertx.test.spring.clustering;

import java.util.Map;
import java.util.Properties;
import org.jgroups.Channel;
import org.jgroups.JChannel;
import org.jgroups.conf.ConfiguratorFactory;
import org.jgroups.conf.ProtocolConfiguration;
import org.jgroups.conf.ProtocolStackConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Channel lookup implementation that creates a JChannel configuration from an XML configuration and then overrides
 * certain properties using the property map that is passed from the (Spring) config.
 * <p/>
 * The configuration file name can be specified in the property map, with the property key
 * <tt>lookupConfigurationFile</tt>. Properties to override should start with <tt>jgroups.</tt>
 * (<tt>PROPERTY_PREFIX</tt>), followed by the protocol name, followed by a dot, followed by the protocol property key.
 */
public class JGroupsChannelLookup {

	private static final Logger log = LoggerFactory.getLogger(JGroupsChannelLookup.class);

	public static Channel getJGroupsChannel(Properties p) {

		String configurationFile = p.getProperty("lookupConfigurationFile");

		try {
			ProtocolStackConfigurator configurator = ConfiguratorFactory.getStackConfigurator(configurationFile);

			overrideProperties(configurator, p);

			return new JChannel(configurator);
		}
		catch (Exception e) {
			throw new RuntimeException("Could not parse JGroups configuration.", e);
		}

	}

	private static void overrideProperties(ProtocolStackConfigurator configurator, Properties p) {

		for (Map.Entry<Object, Object> entry : p.entrySet()) {
			String entryKey = entry.getKey().toString();
			setProperty(configurator, entryKey, entry.getValue().toString());
		}

	}

	private static void setProperty(ProtocolStackConfigurator configurator, String key, String value) {
		int dotIndex = key.indexOf('.');
		if (dotIndex < 0) {
			log.warn("No valid protocol name found in property key: {}", key);
			return;
		}

		String protocolName = key.substring(0, dotIndex);
		String protocolPropertyKey = key.substring(dotIndex + 1);

		for (ProtocolConfiguration protocol : configurator.getProtocolStack()) {
			if (protocol.getProtocolName().equalsIgnoreCase(protocolName)) {
				protocol.getProperties().put(protocolPropertyKey, value);
				return;
			}
		}

		log.warn("Could not find protocol in protocol stack: {}", protocolName);
	}

}