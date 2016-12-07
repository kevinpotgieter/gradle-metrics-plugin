package uk.bluedawnsolutions.gradle

import com.codahale.metrics.MetricRegistry
import com.codahale.metrics.graphite.Graphite
import com.codahale.metrics.graphite.GraphiteReporter
import org.gradle.BuildResult
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskState

class MetricsPlugin implements Plugin<Project> {

    private MetricRegistry metricRegistry = new MetricRegistry()

    @Override
    void apply(Project project) {
        final GString BUILD_FAILURE_METRIC_NAME = "${project.name}.build.failure"
        final GString BUILD_SUCCESS_METRIC_NAME = "${project.name}.build.success"
        final GString BUILD_TOTAL_COUNT_METRIC_NAME = "${project.name}.build.total"

        project.configure(project) {
            extensions.create("buildMetrics",
                    MetricsPluginExtension)
        }

        project.gradle.taskGraph.beforeTask { Task task ->
            metricRegistry.timer(getMetricNameFromTask(task))
        }

        project.gradle.taskGraph.afterTask { Task task, TaskState state ->
            metricRegistry.timers.get(getMetricNameFromTask(task)).time().stop()
        }

        project.gradle.buildFinished { BuildResult buildResult ->
            metricRegistry.counter(BUILD_TOTAL_COUNT_METRIC_NAME).inc()
            //register the success and error counters
            metricRegistry.counter(BUILD_FAILURE_METRIC_NAME).inc(0)
            metricRegistry.counter(BUILD_SUCCESS_METRIC_NAME).inc(0)

            buildResult.failure ? metricRegistry.counter(BUILD_FAILURE_METRIC_NAME).inc()
                    : metricRegistry.counter(BUILD_SUCCESS_METRIC_NAME).inc()

            def metricsPrefix = project.buildMetrics.metricsPrefix
            def graphiteHost = project.buildMetrics.graphiteHost
            def graphitePort = project.buildMetrics.graphitePort

            project.logger.info("Attempting to publish build metrics using the following details: host => ${graphiteHost}, port => ${graphitePort}, metricsPrefix => ${metricsPrefix} ")

            GraphiteReporter metricReporter = GraphiteReporter.forRegistry(metricRegistry)
                    .prefixedWith(metricsPrefix)
                    .build(new Graphite(hostname: graphiteHost, port: graphitePort))

            metricReporter.report()
        }
    }

    static String getMetricNameFromTask(Task task) {
        "${task.project.name}.${task.name}.duration"
    }

}