package com.logsniffer.app;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.fasterxml.jackson.databind.type.MapType;
import com.logsniffer.beanconfig.BeanConfigFactoryManager;
import com.logsniffer.beanconfig.ConfiguredBean;
import com.logsniffer.field.FieldJsonMapper;
import com.logsniffer.field.FieldsMap;
import com.logsniffer.field.FieldsMap.FieldsMapMixInLikeSerializer;

/**
 * Core app config.
 * 
 * @author mbok
 * 
 */
@Configuration
@Import({ StartupAppConfig.class, ConfigValueAppConfig.class })
public class CoreAppConfig {
	public static final String BEAN_LOGSNIFFER_PROPS = "logSnifferProps";
	public static final String LOGSNIFFER_PROPERTIES_FILE = "config.properties";
	private final Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	private ApplicationContext context;

	/**
	 * Registers the {@link ContextProvider}.
	 * 
	 * @return the context provider.
	 */
	@Bean
	public ContextProvider contextProvider() {
		ContextProvider.setContext(context);
		return new ContextProvider();
	}

	@Bean(name = { BEAN_LOGSNIFFER_PROPS })
	@Autowired
	public PropertiesFactoryBean logSnifferProperties(final ApplicationContext ctx) throws IOException {
		if (ctx.getEnvironment().acceptsProfiles("!" + ContextProvider.PROFILE_NONE_QA)) {
			final File qaFile = File.createTempFile("logsniffer", "qa");
			qaFile.delete();
			final String qaHomeDir = qaFile.getPath();
			logger.info("QA mode active, setting random home directory: {}", qaHomeDir);
			System.setProperty("logsniffer.home", qaHomeDir);
		}
		final PathMatchingResourcePatternResolver pathMatcher = new PathMatchingResourcePatternResolver();
		Resource[] classPathProperties = pathMatcher.getResources("classpath*:/config/**/logsniffer-*.properties");
		final Resource[] metainfProperties = pathMatcher
				.getResources("classpath*:/META-INF/**/logsniffer-*.properties");
		final PropertiesFactoryBean p = new PropertiesFactoryBean();
		for (final Resource r : metainfProperties) {
			classPathProperties = ArrayUtils.add(classPathProperties, r);
		}
		classPathProperties = ArrayUtils.add(classPathProperties,
				new FileSystemResource(System.getProperty("logsniffer.home") + "/" + LOGSNIFFER_PROPERTIES_FILE));
		p.setLocations(classPathProperties);
		p.setProperties(System.getProperties());
		p.setLocalOverride(true);
		p.setIgnoreResourceNotFound(true);
		return p;
	}

	/**
	 * Returns a general properties placeholder configurer based on
	 * {@link #logSnifferProperties()}.
	 * 
	 * @param props
	 *            autowired logSnifferProperties bean
	 * @return A general properties placeholder configurer.
	 * @throws IOException
	 */
	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
	@Autowired
	public static PropertyPlaceholderConfigurer propertyPlaceholderConfigurer(
			@Qualifier(BEAN_LOGSNIFFER_PROPS) final Properties props) throws IOException {
		final PropertyPlaceholderConfigurer c = new PropertyPlaceholderConfigurer();
		c.setIgnoreResourceNotFound(true);
		c.setIgnoreUnresolvablePlaceholders(true);
		c.setSystemPropertiesMode(PropertyPlaceholderConfigurer.SYSTEM_PROPERTIES_MODE_OVERRIDE);
		c.setProperties(props);
		return c;
	}

	@Bean
	public ObjectMapper jsonObjectMapper() {
		final ObjectMapper jsonMapper = new ObjectMapper();
		jsonMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
		jsonMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		jsonMapper.configure(Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
		jsonMapper.configure(Feature.ALLOW_SINGLE_QUOTES, true);
		jsonMapper.configure(MapperFeature.DEFAULT_VIEW_INCLUSION, false);

		final SimpleModule module = new SimpleModule("FieldsMapping", Version.unknownVersion());
		module.setSerializerModifier(new BeanSerializerModifier() {
			@Override
			public JsonSerializer<?> modifyMapSerializer(final SerializationConfig config, final MapType valueType,
					final BeanDescription beanDesc, final JsonSerializer<?> serializer) {
				if (FieldsMap.class.isAssignableFrom(valueType.getRawClass())) {
					return new FieldsMapMixInLikeSerializer();
				} else {
					return super.modifyMapSerializer(config, valueType, beanDesc, serializer);
				}
			}
		});
		jsonMapper.registerModule(module);
		return jsonMapper;
	}

	/**
	 * Used for proper serilization/deserilization of {@link FieldsMap}s.
	 * 
	 * @return
	 */
	@Bean
	public FieldJsonMapper fieldJsonMapper() {
		return new FieldJsonMapper();
	}

	/**
	 * Used for proper serilization/deserilization of {@link ConfiguredBean}s as
	 * key concept for persisting models in logsniffer.
	 * 
	 * @return a {@link BeanConfigFactoryManager} instance
	 */
	@Bean
	public BeanConfigFactoryManager beanConfigFactoryManager() {
		return new BeanConfigFactoryManager();
	}
}
