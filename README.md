# saycheese
A system based on Instagram where Java's security API is applied.

COMPILE SERVER:
javac -d bin src/server/SayCheeseServer.java src/server/Com.java
COMPILE CLIENT:
javac -d bin src/client/SayCheese.java src/client/Com.java src/client/ClientStub.java
RUN SERVER: 
java -cp bin -Djava.security.manager -Djava.security.policy==server.policy src.server.SayCheeseServer 45678
RUN CLIENT:
java -cp bin -Djava.security.manager -Djava.security.policy==client.policy src.client.SayCheese localhost:45678 ffreire eusoumaiseu



TODO:
*There is no need to keep sending the current user as it is stored 
at the server.
