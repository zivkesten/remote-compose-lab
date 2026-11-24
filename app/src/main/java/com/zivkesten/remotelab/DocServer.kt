package com.zivkesten.remotelab

import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

/** Tiny HTTP server inside the app serving the current document at http://127.0.0.1:8765/doc.rc */
class DocServer(private val port: Int = 8765) {
    @Volatile var bytes: ByteArray = ByteArray(0)
    @Volatile var version: Int = 0
    private var server: ServerSocket? = null

    fun start() {
        if (server != null) return
        val s = ServerSocket(port, 8, InetAddress.getByName("127.0.0.1"))
        server = s
        thread(isDaemon = true, name = "doc-server") {
            while (!s.isClosed) {
                try { handle(s.accept()) } catch (_: Exception) {}
            }
        }
    }

    private fun handle(sock: Socket) {
        sock.use {
            val reader = sock.getInputStream().bufferedReader()
            val request = reader.readLine() ?: return
            var ifNone: String? = null
            while (true) {
                val line = reader.readLine() ?: break
                if (line.isEmpty()) break
                if (line.startsWith("If-None-Match:", true)) ifNone = line.substringAfter(':').trim()
            }
            val out = sock.getOutputStream()
            val etag = "\"v$version\""
            if (!request.startsWith("GET /doc.rc") || bytes.isEmpty()) {
                out.write("HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\nConnection: close\r\n\r\n".toByteArray())
            } else if (ifNone == etag) {
                out.write("HTTP/1.1 304 Not Modified\r\nETag: $etag\r\nConnection: close\r\n\r\n".toByteArray())
            } else {
                val b = bytes
                out.write(("HTTP/1.1 200 OK\r\nContent-Type: application/octet-stream\r\nETag: $etag\r\n" +
                    "Content-Length: ${b.size}\r\nConnection: close\r\n\r\n").toByteArray())
                out.write(b)
            }
            out.flush()
        }
    }
}
