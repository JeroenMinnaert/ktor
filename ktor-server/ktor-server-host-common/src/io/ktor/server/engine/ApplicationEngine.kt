package io.ktor.server.engine

import java.util.concurrent.*

/**
 * Engine which runs an application
 */
interface ApplicationEngine {

    open class Configuration {
        val parallelism = Runtime.getRuntime().availableProcessors()

        var connectionGroupSize = parallelism / 2 + 1
        var workerGroupSize = parallelism / 2 + 1
        var callGroupSize = parallelism
    }

    /**
     * Environment with which this engine is running
     */
    val environment: ApplicationEngineEnvironment

    /**
     * Starts this [ApplicationEngine]
     *
     * @param wait if true, this function does not exist until application engine stops and exits
     * @return returns this instance
     */
    fun start(wait: Boolean = false): ApplicationEngine

    /**
     * Stops this [ApplicationEngine]
     *
     * @param gracePeriod the maximum amount of time in milliseconds to allow for activity to cool down
     * @param timeout the maximum amount of time to wait until server stops gracefully
     * @param timeUnit the [TimeUnit] for [timeout]
     */
    fun stop(gracePeriod: Long, timeout: Long, timeUnit: TimeUnit)
}

