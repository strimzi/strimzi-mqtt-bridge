/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.bridge.mqtt;

import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.strimzi.kafka.bridge.mqtt.config.BridgeConfig;
import io.strimzi.kafka.bridge.mqtt.config.ConfigRetriever;
import io.strimzi.kafka.bridge.mqtt.kafka.BridgeKafkaProducerFactory;
import io.strimzi.kafka.bridge.mqtt.utils.MappingRulesLoader;
import io.strimzi.kafka.bridge.mqtt.core.MqttServer;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.io.IOException;
import java.util.Map;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static final String CONFIG_FILE_OPTION = "config-file";
    private static final String MAPPING_RULES_FILE_OPTION = "mapping-rules";

    public static void main(String[] args) {
        logger.info("Strimzi MQTT Bridge {} is starting", Main.class.getPackage().getImplementationVersion());
        try {
            //prepare the command line options
            CommandLine cmd = new DefaultParser().parse(generateCommandLineOptions(), args);

            //load the configuration file from the path specified in the command line
            String configFilePath = getAbsoluteFilePath(cmd.getOptionValue(Main.CONFIG_FILE_OPTION));
            String mappingRulesFile = getAbsoluteFilePath(cmd.getOptionValue(Main.MAPPING_RULES_FILE_OPTION));

            Map<String, ?> configRetriever = configFilePath != null ? ConfigRetriever.getConfig(configFilePath) : ConfigRetriever.getConfigFromEnv();
            BridgeConfig bridgeConfig = BridgeConfig.fromMap((Map<String, Object>) configRetriever);

            //set the mapping rules file path
            MappingRulesLoader.getInstance().init(mappingRulesFile);

            //initialize the Bridge Kafka Producer Factory
            BridgeKafkaProducerFactory.getInstance().init(bridgeConfig.getKafkaConfig());

            //start the MQTT server
            EventLoopGroup bossGroup = new NioEventLoopGroup();
            EventLoopGroup workerGroup = new NioEventLoopGroup();
            MqttServer mqttServer = new MqttServer(bridgeConfig.getMqttConfig(), bossGroup, workerGroup, ChannelOption.SO_KEEPALIVE);

            // start the MQTT server
            mqttServer.start();
        } catch (IOException | ParseException e) {
            logger.error("Error starting the MQTT server: ", e);
        }
    }

    /**
     * Generate the command line options.
     * The options are:
     *      --config-file: the path of the configuration file
     *      --mapping-rules: the path of the topic mapping rules file
     * E.g.:
     *      <application>  --config-file=/path/to/config/file --mapping-rules=/path/to/mapping/rules/file
     * @return the command line options
     */
    private static Options generateCommandLineOptions() {

        Options options = new Options();

        Option optionConfigFile = Option.builder()
                .longOpt(Main.CONFIG_FILE_OPTION)
                .hasArg(true)
                .desc("The path to the configuration file")
                .build();
        options.addOption(optionConfigFile);

        Option optionMappingRulesFile = Option.builder()
                .longOpt(Main.MAPPING_RULES_FILE_OPTION)
                .hasArg(true)
                .required()
                .desc("The path to the topic mapping rules file")
                .build();

        options.addOption(optionMappingRulesFile);
        return options;
    }

    /**
     * Get the absolute path of the file
     *
     * @param arg the path of the file
     * @return the absolute path of the file
     */
    private static String getAbsoluteFilePath(String arg) {
        if (arg == null) {
            return null;
        }
        return arg.startsWith(File.separator) ? arg : System.getProperty("user.dir") + File.separator + arg;
    }
}
