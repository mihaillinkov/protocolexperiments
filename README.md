### Asynchronous http server written in Kotlin

Purpose of this project is to experiment with throughput, and achieve maximum performance using non-blocking api.

````
java \
-Dcom.sun.management.jmxremote  \
-Dcom.sun.management.jmxremote.port=$PORT  \
-Dcom.sun.management.jmxremote.rmi.port=$PORT \
-Dcom.sun.management.jmxremote.ssl=false  \
-Dcom.sun.management.jmxremote.authenticate=false  \
-Djava.rmi.server.hostname=$PUBLIC_IP \
-cp "./*" http.MainKt
```