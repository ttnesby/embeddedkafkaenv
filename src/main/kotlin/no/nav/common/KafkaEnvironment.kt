package no.nav.common

import kafka.admin.AdminUtils
import kafka.admin.RackAwareMode
import kafka.admin.TopicCommand
import kafka.utils.ZkUtils
import no.nav.common.embeddedkafka.KBServer
import no.nav.common.embeddedkafkarest.KRServer
import no.nav.common.embeddedschemaregistry.SRServer
import no.nav.common.embeddedutils.*
import no.nav.common.embeddedzookeeper.ZKServer
import java.util.*

/**
 * A in-memory kafka environment consisting of
 * - 1 zookeeper
 * - configurable no of kafka brokers
 * - 1 schema registry
 * - 1 rest gateway
 *
 * Also adding configurable topics to the cluster
 */
object KafkaEnvironment {

    data class ServerPark(
            val zookeeper: ZKServer,
            val brokers: List<KBServer>,
            val schemaregistry: SRServer,
            val rest: KRServer
    )

    /**
     * Start the kafka environment
     * @param noOfBrokers no of brokers to spin up, default one
     * @param topics a list of topics to create - default empty
     * @param withSchemaRegistry include schema registry - default true
     * @param withRest include rest server - default false
     * @return a map of urls - key values: 'broker', 'schema', 'rest'
     *
     * Observe that the url for multiple brokers will be a string like '<url broker 1>, <url broker 2>, ...'
     */
    fun start(
            noOfBrokers: Int = 1,
            topics: List<String> = emptyList(),
            withSchemaRegistry: Boolean = true,
            withRest: Boolean = false) : Map<String,String> {

        //allocate enough available port
        val noOfPorts = 1 + noOfBrokers + listOf(withSchemaRegistry, withRest).filter { true }.size
        val portsIter = (1..noOfPorts).map { getAvailablePort() }.iterator()

        val serverPark = ServerPark(
                ZKServer(portsIter.next()),
                (1..noOfBrokers).map { KBServer(portsIter.next(),it-1,noOfBrokers) },
                if (withSchemaRegistry) SRServer(portsIter.next()) else SRServer(0),
                if (withRest) KRServer(portsIter.next()) else KRServer(0)
        )

        ZKServer.onReceive(ZKStart)
        KBServer.onReceive(KBStart(noOfBrokers))
        SRServer.onReceive(SRStart)
        KRServer.onReceive(KRStart)

        if (!topics.isEmpty()) createTopics(topics, noOfBrokers)

        return mapOf("broker" to KBServer.getUrl(),"schema" to SRServer.getUrl(), "rest" to KRServer.getUrl())
    }

    /**
     * Stop the kafka environment - all topics and events will be deleted
     */
    fun stop() {

        KRServer.onReceive(KRStop)
        SRServer.onReceive(SRStop)
        KBServer.onReceive(KBStop)
        ZKServer.onReceive(ZKStop)
    }

    // see the following links for creating topic
    // https://insight.io/github.com/apache/kafka/blob/1.0/core/src/main/scala/kafka/admin/TopicCommand.scala
    // https://insight.io/github.com/apache/kafka/blob/1.0/core/src/main/scala/kafka/utils/ZkUtils.scala
    // https://insight.io/github.com/apache/kafka/blob/1.0/core/src/main/scala/kafka/admin/AdminUtils.scala

    private fun createTopics(topics: List<String>, noPartitions: Int) {

        val sessTimeout = 1500
        val connTimeout = 500

        val zkUtils =  ZkUtils.apply(ZKServer.getUrl(), sessTimeout, connTimeout, false)

        topics.forEach {

            // core/admin/TopicCommand for details
            val opts = TopicCommand.TopicCommandOptions(
                    arrayOf(
                            "--create",
                            it,
                            "--if-not-exists",
                            "--partitions",noPartitions.toString(),
                            "--replication-factor",1.toString(),
                            "--zookeeper",ZKServer.getUrl()
                    )
            )
            val config = Properties() // no advanced config of topic...
            val partitions = opts.options().valueOf(opts.partitionsOpt()).toInt()
            val replicas = opts.options().valueOf(opts.replicationFactorOpt()).toInt()
            val rackAwareDisabled = RackAwareMode.`Disabled$`()

            AdminUtils.createTopic(zkUtils, it, partitions, replicas, config, rackAwareDisabled)
        }

        zkUtils.close()
    }

}