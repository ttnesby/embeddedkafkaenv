package no.nav.common.embeddedkafkarest

import io.confluent.kafkarest.KafkaRestApplication
import io.confluent.kafkarest.KafkaRestConfig
import no.nav.common.embeddedutils.*
import java.lang.NullPointerException
import java.util.*

class KRServer(
        override val port: Int,
        private val zkURL: String,
        private val kbURL: String,
        private var srURL: String) : ServerBase() {

    // see link below for starting up embeddedkafkarest
    // https://github.com/confluentinc/kafka-rest/blob/4.0.x/src/main/java/io/confluent/kafkarest/KafkaRestMain.java

    override val url = "http://$host:$port"

    // not possible to restart rest at this level, use inner core class
    private class KRS(url: String, zkURL: String, kbURL: String, srURL: String) {

        val krServer = KafkaRestApplication(Properties().apply {
            set(KafkaRestConfig.LISTENERS_CONFIG, url)
            set(KafkaRestConfig.ZOOKEEPER_CONNECT_CONFIG, zkURL)
            set(KafkaRestConfig.BOOTSTRAP_SERVERS_CONFIG, kbURL)
            set(KafkaRestConfig.SCHEMA_REGISTRY_URL_CONFIG, srURL)
            set(KafkaRestConfig.PRODUCER_THREADS_CONFIG, 3)
        })
    }

    private val kr = mutableListOf<KRS>()

    override fun start() = when (status) {
        NotRunning -> {
            KRS(url, zkURL, kbURL, srURL).apply {
                kr.add(this)
                try{ krServer.start() } catch (e: NullPointerException) {/**/}
            }
            status = Running
        }
        else -> {}
    }

    override fun stop() = when (status) {
        Running -> {
            kr.first().apply {
                try {
                    krServer.stop()
                    krServer.join()
                } catch (e: NullPointerException) {/**/}
            }
            kr.removeAll { true }
            status = NotRunning
        }
        else -> {}
    }
}