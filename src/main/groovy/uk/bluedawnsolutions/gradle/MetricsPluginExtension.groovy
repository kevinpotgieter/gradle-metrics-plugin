package uk.bluedawnsolutions.gradle


class MetricsPluginExtension {

    def metricsPrefix = "gradle.build.${getHostname()}"
    def graphiteHost = "localhost"
    def graphitePort = 2003



    private static final getHostname(){
        try {
            return InetAddress.getLocalHost().getHostName().replaceAll("\\.", "-");
        } catch (Exception e) {
        }
        return "UnknownHost";
    }
}
