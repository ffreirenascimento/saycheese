# saycheese
A system based on Instagram where Java's security API is applied.

COMPILE SERVER:
javac -d bin -cp /home/francisco/Documents/Coding/Java/Dependencies/gson-2.9.0.jar src/server/*.java
COMPILE CLIENT:
javac -d bin src/client/*.java
RUN SERVER: 
java -cp bin -Djava.security.manager -Djava.security.policy==server.policy src.server.SayCheeseServer 45678
RUN CLIENT:
java -cp bin -Djava.security.manager -Djava.security.policy==client.policy src.client.SayCheese localhost:45678 ffreire eusoumaiseu



TODO:
UNFOLLOW:
not unfollowing



LIKE:
is returning -1
photo file is not found
=======
Error while liking photo
=======
=======
Stopping the application
=======
=======
End of session
=======