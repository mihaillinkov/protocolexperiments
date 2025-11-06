### Asynchronous http server written in Kotlin

Purpose of this project is to experiment with throughput, and achieve maximum performance using non-blocking api.

````
java \
-Dcom.sun.management.jmxremote  \
-Dcom.sun.management.jmxremote.port=9010  \
-Dcom.sun.management.jmxremote.rmi.port=9010 \
-Dcom.sun.management.jmxremote.ssl=false  \
-Dcom.sun.management.jmxremote.authenticate=false  \
-Djava.rmi.server.hostname=192.168.1.200 \
-cp "./*" http.MainKt
```