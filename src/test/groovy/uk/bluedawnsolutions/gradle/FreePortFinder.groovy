package uk.bluedawnsolutions.gradle


class FreePortFinder {

    public static int getFreePort() {
        ServerSocket socket = null;
        try {
            socket = new ServerSocket(0);
            socket.setReuseAddress(true);
            int port = socket.getLocalPort();
            closeSocket(socket);
            return port;
        } catch (IOException ignored) {
        } finally {
            closeSocket(socket);
        }
        throw new IllegalStateException("Unable find a free TCP/IP port");

    }

    private static void closeSocket(ServerSocket socket) {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }
}
