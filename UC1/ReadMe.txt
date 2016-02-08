Camel Inmarsat PoC
==================

1) Build and install the project.
   Ensure Fuse is not running, otherwise you will encounter of clash of ActiveMQ Brokers
   > mvn clean install

2) Include the following property in Fuse's 'system.properties'
   > ms-stub-schema-version=v1

3) ensure admin/admin is commented out from:
   > etc/users.properties

4) start Fuse

5) install netty-http
   > features:install camel-netty-http

6) install Use Case 1
   > install mvn:com.inmarsat/uc1/1.0


The stubs are started by default.

Check the ports have been opened:

	[developer@localhost system]$ netstat -an | grep 9999
	tcp6       0      0 127.0.0.1:29999         :::*                    LISTEN     
	tcp6       0      0 127.0.0.1:19999         :::*                    LISTEN

To stop MS-Dynamics run:
   > route-stop stub-ms-dynamics uc1

Check the port is no longer open:
    
	[developer@localhost system]$ netstat -an | grep 9999
	tcp6       0      0 127.0.0.1:29999         :::*                    LISTEN

To start MS-Dynamics run:
   > route-start stub-ms-dynamics uc1


Useful for DEMO:
================

- Use Hawtio to send JMS messages to the JMS listener
- the failed/retry folders are located under:
     $FUSE_HOME/src/data/manual/ms

