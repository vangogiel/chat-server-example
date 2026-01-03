#chat-server-example

This is a simple application to represent a chat server with bidirectional stream. 

It uses gRPC for communication and PostgreSql for storage. The storage has been designed with future expansion in mind.
It uses `conversation_id` that can be reconstructed by application layer from sender and recipient UUID. 
The `message_id` itself is ULID, so that it can be used for sorting. 

This architecture allows for a simple and low cost start with PostgreSql, but allow to swiftly swap for something 
more scalable and performant like Cassandra in the future.

This application could be expanded with another table to store undelivered messages to allow separate concerns. 
This approach would also allow for easier slotting in with potential NoSQL database in the future.
