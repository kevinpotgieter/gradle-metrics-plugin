package uk.bluedawnsolutions.gradle

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification
import spock.lang.Unroll


class MetricsPluginSpec extends Specification {

    @Rule
    final TemporaryFolder testProjectDir = new TemporaryFolder()

    @Rule
    final GraphiteMockServer graphiteMockServer = new GraphiteMockServer()

    String projectName
    File buildFile

    def setup() {
        projectName = testProjectDir.root.getName()
        println "Project name is ${projectName}"

        buildFile = testProjectDir.newFile('build.gradle')
        //testing....

        println "buildFile = $buildFile"
    }

    def "Build should still pass with no configuration"() {
        given:
        buildFile.newWriter().withWriter { w ->
            w << """
            plugins {
                id 'uk.bluedawnsolutions.gradle.metrics-plugin'
            }

            task tempTask {
                doLast {
                    println "We've run the task!"
                }
            }
        """
        }

        when:
        def result = gradle("tempTask")

        then:
        result.task(":tempTask").outcome == TaskOutcome.SUCCESS
    }

    @Unroll
    def "Expect the following metric pattern: '#expectedMetricName' to be published to Graphite after the build completes"(def expectedMetricName) {
        given:
        buildFile.newWriter().withWriter { w ->
            w << """
            plugins {
                id 'uk.bluedawnsolutions.gradle.metrics-plugin'
            }

            buildMetrics{
                graphitePort = ${graphiteMockServer.port}
            }

            task tempTask {
                doLast {
                    println "We've run the task!"
                }
            }
        """
        }

        when:
        gradle("tempTask")

        then:
        graphiteMockServer.verifyMetricReceived(expectedMetricName)

        where:
        expectedMetricName << [
                /gradle\.build\..*\..*.build.success.count/,
                /gradle\.build\..*\..*.build.total.count/,
                /gradle\.build\..*\.tasks\.tempTask\.duration/

        ]
    }

    private BuildResult gradle(String... args) {
        logResult(GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments(*args)
                .forwardOutput()
                .withDebug(true)
                .withPluginClasspath()
                .build())
    }

    private static BuildResult logResult(BuildResult result) {
        if (result == null) return null;
        println result.getOutput()
        result
    }


}
