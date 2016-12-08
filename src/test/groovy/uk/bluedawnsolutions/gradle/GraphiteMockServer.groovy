package uk.bluedawnsolutions.gradle

import org.junit.rules.ExternalResource
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.nio.charset.Charset
import java.nio.charset.CharsetDecoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

import static org.awaitility.Awaitility.await

class GraphiteMockServer extends ExternalResource implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(GraphiteMockServer.class)

    private ServerSocketChannel server
    private int port
    private Selector selector
    private Thread thread

    private static final ConcurrentHashMap<String, Tuple> COLLECTED_METRICS = new ConcurrentHashMap<>()

    @Override
    public void before() throws Throwable {
        LOG.info("Graphite Mock before...")
        server = ServerSocketChannel.open()
        server.configureBlocking(false)
        port = FreePortFinder.freePort
        LOG.info("uk.bluedawnsolutions.gradle.GraphiteMockServer starting : port={}", port)
        println("uk.bluedawnsolutions.gradle.GraphiteMockServer starting : port=" + port)
        server.socket().bind(new InetSocketAddress(port))
        selector = Selector.open()
        server.register(selector, SelectionKey.OP_ACCEPT)

        thread = new Thread(this)
        thread.start()
        LOG.info("... done Graphite Mock before")
    }

    @Override
    public void after() {
        LOG.info("Graphite Mock after")
        try {
            thread.interrupt()
        } catch (Exception e) {
        }
        try {
            selector.close()
        } catch (IOException e) {
        }
        try {
            server.close()
        } catch (Exception e) {
        }
    }

    public int getPort() {
        return port
    }

    public def verifyMetricReceived(def regex) {
        await("Locating metric name with patterrn: ${regex}").atMost(500, TimeUnit.MILLISECONDS).until {
            def keys = COLLECTED_METRICS.keySet().findAll { key ->
                key ==~ regex
            }
            keys.size() > 0
        }
    }

    @Override
    public void run() {
        Charset charset = Charset.forName("UTF-8")
        CharsetDecoder decoder = charset.newDecoder()

        try {
            while (!thread.isInterrupted()) {
                selector.select()

                Iterator<SelectionKey> iterator = selector.isOpen() ? selector.selectedKeys().iterator() : new ArrayList<>().iterator()
                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next()
                    iterator.remove()

                    if (!key.isValid()) {
                        continue
                    }

                    if (key.isAcceptable()) {
                        SocketChannel client = server.accept()
                        client.configureBlocking(false)
                        client.register(selector, SelectionKey.OP_READ)
                        continue
                    }

                    if (key.isReadable()) {
                        SocketChannel client = (SocketChannel) key.channel()
                        int BUFFER_SIZE = 2048
                        ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE)
                        try {
                            int bytesRead = client.read(buffer)
                            if (bytesRead > 0) {
                                buffer.flip()
                                collectMetrics(decoder.decode(buffer))
                            }
                        } catch (Exception e) {
                            e.printStackTrace()
                        }

                        continue
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace()
        }
    }

    static def collectMetrics(CharBuffer charBuffer) {
        charBuffer.toString().splitEachLine('\n') {
            it.collect { item ->
                def singleMetricElements = item.split(" ")
                if (singleMetricElements.length == 3) {
                    println item
                    COLLECTED_METRICS.put(singleMetricElements[0], new Tuple(singleMetricElements[1], singleMetricElements[2]))
                }
            }
        }
    }
}
