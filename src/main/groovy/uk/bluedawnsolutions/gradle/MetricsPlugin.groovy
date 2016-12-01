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
        project.configure(project) {
            extensions.create("buildMetrics",
                    MetricsPluginExtension)
        }

        project.gradle.taskGraph.beforeTask { Task task ->
            metricRegistry.timer(getMetricNameFromTask(task))
        }

        project.gradle.taskGraph.afterTask { Task task, TaskState state ->
            metricRegistry.timer(getMetricNameFromTask(task)).time().stop()
        }

        project.gradle.buildFinished { BuildResult buildResult ->

            metricRegistry.counter("${project.name}.build.total").inc()
            buildResult.failure ? metricRegistry.counter("${project.name}.build.failure").inc()
                    : metricRegistry.counter("${project.name}.build.success").inc()

            def metricsPrefix = project.buildMetrics.metricsPrefix
            def graphiteHost = project.buildMetrics.graphiteHost
            def graphitePort = project.buildMetrics.graphitePort

            project.logger.info("Attempting to publish build metrics using the following details: host => ${graphiteHost}, port => ${graphitePort}, metricsPrefix => ${metricsPrefix} ")

            GraphiteReporter metricReporter = GraphiteReporter.forRegistry(metricRegistry)
                    .prefixedWith(metricsPrefix)
                    .build(new Graphite(graphiteHost, graphitePort))

            metricReporter.report()
        }
    }

    String getMetricNameFromTask(Task task) {
        "${task.project.name}.${task.name}.duration"
    }

}