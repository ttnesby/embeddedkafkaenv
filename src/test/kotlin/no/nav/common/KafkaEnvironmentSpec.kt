package no.nav.common

import com.github.kittinunf.fuel.httpGet
import kafka.utils.ZkUtils
import org.amshove.kluent.*
import org.apache.zookeeper.client.FourLetterWordMain
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.context
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it

object KafkaEnvironmentSpec : Spek({

    val sessTimeout = 1500
    val connTimeout = 500

    describe("kafka environment tests") {

        context("default kafka environment") {

            val keDefault = KafkaEnvironment()

            val nBroker = 1
            val nTopics = 0

            beforeGroup {
                keDefault.start()
            }

            it("should have 1 zookeeper") {

                FourLetterWordMain.send4LetterWord(
                        keDefault.serverPark.zookeeper.host,
                        keDefault.serverPark.zookeeper.port,
                        "ruok") `should be equal to` "imok\n"

            }

            it("should have $nBroker broker") {

                ZkUtils.apply(keDefault.serverPark.zookeeper.url, sessTimeout, connTimeout, false).run {
                    val nBroker = allBrokersInCluster.size()
                    close()
                    nBroker
                } `should be equal to` nBroker
            }

            it("should have $nTopics topics available") {

                ZkUtils.apply(keDefault.serverPark.zookeeper.url, sessTimeout, connTimeout, false).run {
                    val nBroker = allTopics.size()
                    close()
                    nBroker
                } `should be equal to` nTopics

            }

            afterGroup {
                keDefault.tearDown()
            }
        }

        context("basic kafka environment") {

            val basicTopics = listOf("basic01","basic02")
            val keBasic = KafkaEnvironment(topics = basicTopics)

            val zku = keBasic.serverPark.zookeeper.url
            val nBroker = 1

            beforeGroup {
                keBasic.start()
            }

            it("should have 1 zookeeper") {

                FourLetterWordMain.send4LetterWord(
                        keBasic.serverPark.zookeeper.host,
                        keBasic.serverPark.zookeeper.port,
                        "ruok") `should be equal to` "imok\n"

            }

            it("should have $nBroker broker") {

                ZkUtils.apply(zku, sessTimeout, connTimeout, false).run {
                    val nBroker = allBrokersInCluster.size()
                    close()
                    nBroker
                } `should be equal to` nBroker
            }

            it("should have ${basicTopics.size} topics available") {

                ZkUtils.apply(zku, sessTimeout, connTimeout, false).run {
                    val nBroker = allTopics.size()
                    close()
                    nBroker
                } `should be equal to` basicTopics.size

            }

            it("should have topics as requested available") {

                ZkUtils.apply(zku, sessTimeout, connTimeout, false).run {
                    val topics = allTopics
                    val lTopics = mutableListOf<String>()

                    topics.foreach { lTopics.add(it) }
                    close()
                    lTopics
                } `should contain all` basicTopics
            }

            afterGroup {
                keBasic.tearDown()
            }
        }

        context("strange_1 kafka environment") {

            val keStrange1 = KafkaEnvironment(-2)

            val zku = keStrange1.serverPark.zookeeper.url
            val nBroker = 0

            beforeGroup {
                keStrange1.start()
            }

            it("should have 1 zookeeper") {

                FourLetterWordMain.send4LetterWord(
                        keStrange1.serverPark.zookeeper.host,
                        keStrange1.serverPark.zookeeper.port,
                        "ruok") `should be equal to` "imok\n"

            }

            it("should have $nBroker broker") {

                ZkUtils.apply(zku, sessTimeout, connTimeout, false).run {
                    val nBroker = allBrokersInCluster.size()
                    close()
                    nBroker
                } `should be equal to` nBroker
            }

            afterGroup {
                keStrange1.tearDown()
            }
        }

        context("strange_2 kafka environment") {

            val strange2Topics = listOf("strange201","strange202","strange203")
            val keStrange2 = KafkaEnvironment(
                    0,
                    topics = strange2Topics,
                    withRest = true)

            val zku = keStrange2.serverPark.zookeeper.url
            val nBroker = 1

            beforeGroup {
                keStrange2.start()
            }

            it("should have 1 zookeeper") {

                FourLetterWordMain.send4LetterWord(
                        keStrange2.serverPark.zookeeper.host,
                        keStrange2.serverPark.zookeeper.port,
                        "ruok") `should be equal to` "imok\n"

            }

            it("should have $nBroker broker") {

                ZkUtils.apply(zku, sessTimeout, connTimeout, false).run {
                    val nBroker = allBrokersInCluster.size()
                    close()
                    nBroker
                } `should be equal to` nBroker
            }

            // +1 is due to schema registry topic for schemas
            it("should have ${strange2Topics.size + 1} topics available") {

                ZkUtils.apply(zku, sessTimeout, connTimeout, false).run {
                    val nBroker = allTopics.size()
                    close()
                    nBroker
                } `should be equal to` (strange2Topics.size + 1)

            }

            it("should have topics as requested available") {

                ZkUtils.apply(zku, sessTimeout, connTimeout, false).run {
                    val topics = allTopics
                    val lTopics = mutableListOf<String>()

                    topics.foreach { lTopics.add(it) }
                    close()
                    lTopics
                } `should contain all` strange2Topics
            }

            it("should have a schema registry") {

                (keStrange2.serverPark.schemaregistry.url + "/config")
                        .httpGet()
                        .responseString().third.component1() shouldEqual """{"compatibilityLevel":"BACKWARD"}"""
            }

            it("should have a rest server") {

                // quick and raw http&json
                (keStrange2.serverPark.rest.url + "/brokers")
                        .httpGet()
                        .responseString().third.component1() shouldEqual """{"brokers":[0]}"""
            }

            afterGroup {
                keStrange2.tearDown()
            }
        }
    }
})