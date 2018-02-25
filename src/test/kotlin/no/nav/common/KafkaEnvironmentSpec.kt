package no.nav.common

import kafka.utils.ZkUtils
import no.nav.common.zookeeper.ZKServer
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should contain all`
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it

object KafkaEnvironmentSpec : Spek({

    describe("active kafka env of one broker with none topics created") {

        val b = 1
        val t = emptyList<String>()

        beforeGroup {
            KafkaEnvironment.start()
        }

        it("should have $b broker(s)") {

            ZkUtils.apply(ZKServer.getUrl(), 500, 500, false).run {
                val nBroker = allBrokersInCluster.size()
                close()
                nBroker
            } `should be equal to` b

        }

        it("should not be any topics available") {

            ZkUtils.apply(ZKServer.getUrl(), 500, 500, false).run {
                val nTopics = allTopics.size()
                close()
                nTopics
            } `should be equal to` t.size
        }

        afterGroup {
            KafkaEnvironment.stop()
        }
    }

    describe("active kafka env of one broker with 2 topics created") {

        val b = 1
        val t = listOf("test1","test2")

        beforeGroup {
            KafkaEnvironment.start(topics = t)
        }

        it("should have $b broker(s)") {

            ZkUtils.apply(ZKServer.getUrl(), 500, 500, false).run {
                val nBroker = allBrokersInCluster.size()
                close()
                nBroker
            } `should be equal to` b

        }

        it("should be ${t.size} topics available") {

            ZkUtils.apply(ZKServer.getUrl(), 500, 500, false).run {
                val nTopics = allTopics.size()
                close()
                nTopics
            } `should be equal to` t.size
        }

        it("should have topics as requested available") {

            ZkUtils.apply(ZKServer.getUrl(), 500, 500, false).run {
                val topics = allTopics
                val lTopics = mutableListOf<String>()

                topics.foreach { lTopics.add(it) }
                close()
                lTopics
            } `should contain all` t
        }

        afterGroup {
            KafkaEnvironment.stop()
        }
    }

    describe("active kafka env of 2 brokers with 4 topics created") {

        val b = 2
        val t = listOf("test1","test2","test3","test4")

        beforeGroup {
            KafkaEnvironment.start(b, t)
        }

        it("should have $b broker(s)") {

            ZkUtils.apply(ZKServer.getUrl(), 500, 500, false).run {
                val nBroker = allBrokersInCluster.size()
                close()
                nBroker
            } `should be equal to` b

        }

        it("should be ${t.size} topics available") {

            ZkUtils.apply(ZKServer.getUrl(), 500, 500, false).run {
                val nTopics = allTopics.size()
                close()
                nTopics
            } `should be equal to` t.size
        }

        it("should have topics as requested available") {

            ZkUtils.apply(ZKServer.getUrl(), 500, 500, false).run {
                val topics = allTopics
                val lTopics = mutableListOf<String>()

                topics.foreach { lTopics.add(it) }
                close()
                lTopics
            } `should contain all` t
        }

        afterGroup {
            KafkaEnvironment.stop()
        }
    }

})