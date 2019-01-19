package de.phyrone.namegen

import freemarker.cache.ClassTemplateLoader
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.AutoHeadResponse
import io.ktor.features.CachingHeaders
import io.ktor.features.StatusPages
import io.ktor.freemarker.FreeMarker
import io.ktor.freemarker.FreeMarkerContent
import io.ktor.http.CacheControl
import io.ktor.http.ContentType
import io.ktor.http.content.CachingOptions
import io.ktor.http.content.TextContent
import io.ktor.http.content.resources
import io.ktor.http.content.static
import io.ktor.response.respond
import io.ktor.response.respondRedirect
import io.ktor.routing.get
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import picocli.CommandLine
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.*

class Webservice : Runnable {
    @CommandLine.Option(names = ["-e", "--use-env"])
    internal var useEnv = false
    private var names: MutableList<String> = ArrayList()
    @CommandLine.Option(names = ["-h", "--host"])
    internal var host = "0.0.0.0"
    @CommandLine.Option(names = ["-p", "--port"])
    internal var port = 8080
    private var html = getHtml()
    private var random = Random()

    private fun getNames() {
        println("Loading Names...")
        var ret: MutableList<String> = ArrayList()
        if (useEnv) {
            ret = ArrayList(
                    Arrays.asList(*System.getenv("RANDOMNAMES")
                            .replace(',', ';')
                            .split(";".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()))
        } else {
            try {
                if (NAMEFILE.exists()) {
                    val scanner = Scanner(FileInputStream(NAMEFILE))
                    while (scanner.hasNextLine()) {
                        ret.addAll(Arrays.asList(*scanner.nextLine().split(",".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()))
                    }
                    scanner.close()
                } else {
                    if (NAMEFILE.createNewFile()) {
                        println("names.txt Created -> fill and restart it!")
                        System.exit(0)
                    } else {
                        System.err.println("names.txt failed to create -> made it manually")
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }

        }
        if (ret.isEmpty()) {
            System.err.println("Error: Name's is Empty -> Stopping Server")
            System.exit(1)
        }
        names.clear()
        ret.forEach { s ->
            if (!s.isEmpty()) {
                names.add(s.replace(" ", "").capitalize())
            }
        }
    }

    private fun getHtml(): String {
        println("Loading Html...")
        val scanner = Scanner(Webservice::class.java.getResourceAsStream("/files/web/template.ftl"))
        val ret = StringBuilder()
        while (scanner.hasNextLine()) {
            ret.append(scanner.nextLine()).append("\n")
        }
        scanner.close()
        return ret.toString()
    }

    override fun run() {
        getNames()
        println("Starting Webservice...")
        embeddedServer(
                Netty,
                port = port,
                host = host
        ) {
            install(FreeMarker) {
                templateLoader = ClassTemplateLoader(Webservice::class.java.classLoader, "/files/web")
            }
            install(AutoHeadResponse)
            install(StatusPages)
            install(CachingHeaders) {
                options { outgoingContent ->
                    when (outgoingContent.contentType?.withoutParameters()) {
                        ContentType.Text.CSS -> CachingOptions(CacheControl.MaxAge(maxAgeSeconds = 24 * 60 * 60))
                        ContentType.Text.JavaScript -> CachingOptions(CacheControl.MaxAge(maxAgeSeconds = 24 * 60 * 60))
                        ContentType.Image.Any -> CachingOptions(CacheControl.MaxAge(maxAgeSeconds = 24 * 60 * 60))
                        else -> null
                    }
                }
            }
            routing {
                fun callToNumber(call: ApplicationCall) = call.parameters["number"] ?: "2"
                static("/assets/") {
                    resources("/files/web/assets/")
                }
                get("/") {
                    call.respondRedirect("/2/", true)
                }
                route("/{number}/") {
                    get("/") {
                        call.respond(FreeMarkerContent("template.ftl", mapOf(
                                "randomname" to generateName(callToNumber(call))
                        )))
                    }
                    get("/json") {
                        // call.response.header("Content-Type", "application/json; charset=utf-8")
                        call.respond(TextContent("{\n  \"name\": \"" + generateName(callToNumber(call)) + "\"\n}", ContentType.Application.Json))
                    }
                    get("/raw") {
                        call.respond(generateName(callToNumber(call)))
                    }
                    static("/assets/") {
                        resources("/files/web/assets/")
                    }
                }

            }
        }.start(true)
        /*
        Spark.ipAddress(host)
        Spark.port(port)
        Spark.threadPool(10)
        Spark.staticFiles.location("/files/web/")
        Spark.get("/") { _, res ->
            res.redirect("/2/")
            null
        }

        Spark.get("/:number/") { request, response ->
            response.header("Content-Type", "text/html; charset=utf-8")
            html.replace("EXAMPLENAME", generateName(request.params(":number")))
        }
        Spark.get("/:number/raw") { request, response ->
            response.header("Content-Type", "text/plain; charset=utf-8")
            generateName(request.params(":number"))
        }
        Spark.get("/:number/json") { request, response ->
            response.header("Content-Type", "application/json; charset=utf-8")
            "{\n  \"name\": \"" + generateName(request.params(":number")) + "\"\n}"
        }
        Spark.init()
        */
    }

    private fun generateName(stringNumber: String): String {
        var number = 2
        try {
            number = Integer.parseInt(stringNumber)
        } catch (ignored: NumberFormatException) {
        }

        val ret = StringBuilder()
        var i = 0
        while (number.coerceAtLeast(1).coerceAtMost(25) > i) {
            ret.append(names[random.nextInt(names.size)])
            i++
        }
        return ret.toString()
    }

    companion object {


        private val NAMEFILE = File("names.txt")

        @JvmStatic
        fun main(args: Array<String>) {
            CommandLine.run(Webservice(), System.out, *args)
        }
    }
}
