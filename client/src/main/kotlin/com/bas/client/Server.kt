package com.bas.client


import org.springframework.boot.Banner
import org.springframework.boot.SpringApplication
import org.springframework.boot.WebApplicationType.SERVLET
import org.springframework.boot.autoconfigure.SpringBootApplication


/**
 * Spring Boot application to interact with nodes via API
 */
@SpringBootApplication
open class Server{
    companion object {

        /**
         * Spring Boot server is started.
         */
        @JvmStatic
        fun main(args: Array<String>) {
            val app = SpringApplication(Server::class.java)
            app.setBannerMode(Banner.Mode.OFF)
            app.webApplicationType = SERVLET
            app.run(*args)
        }
    }
}




