package uk.bluedawnsolutions.gradle

import com.codahale.metrics.MetricRegistry
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
            if (buildResult.failure) {
                println "${buildResult.action} failed!"
            } else {
                println "${buildResult.action} succeeded!"
            }
        }
    }

    String getMetricNameFromTask(Task task) {
        "${task.name}.duration"
    }
}