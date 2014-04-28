# Overview #
Blip is a toolkit for enabling distributed components to communicate using
IP multicast. It was developed to enable highly distributed systems to 
communicate efficiently.

This project is a complete rewrite and refactoring of several versions of 
toolkits used on different projects in the telecommunications and electric 
utility industry. This project has been re-designed to offer modular multicast 
communications in a very small package. An emphasis has been placed on code 
readability and maintenance to encourage social coding in the evolution of the 
project.

# Example Applications #

Previous versions of this toolkit have been used to solve many different 
problems:

* **System Instrumentation** - The response time and resource metrics of application components were broadcast to data storage and analitical components allowing real time monitoring of systems.

* **System Management** - Instrumented system components (hardware and software) contained agents which allowed system management commands to be processed. 

* **Centralized Logging** - Log messages would be broadcast to a group of storage and analizers for handling. Similar to syslog, but the sender of the log woul never need to know where to send the log entry; no endpoint needed to be maintained. New log message processors could be inserted and removed without having to reconfigure any of the loggers.

* **Parallel Processing** - Work was broadcast to a group of workers who would acquire the task. The requester would determine which component could acquire the task for processing. If the task failed, the requester would allow another component to acquire the task. Sometimes two or more compoents would be allows to acquire the task, send their results which the requester would reconcile for accuracy.

* **Service Bus** - Completely decoupled services would listen for requests and respond with results. This was combined with Contract-Net protocols to enable 

* **Security Logging** - Security events were broadcast to all nodes in the network for analysis. This prevented the wiring of destination of specific messages to specialized processors. 

* **Session Sharing** - Applications whould generate a session and manage it locally. When a client with that session appeared in a different component with that session, the session was made available to the new component. 

* **Map-Reduce** - A request would be broadcast to an unknown number of consumers which would then return their portion of the response. The requester would then assemble the responses into a larger result set. 

* **Clustering** - Components would continually broadcast status and share work. This allowed for clusterd components operate in a highly-available and fault-tolerant manner. 

* **Shared Memory** - Using the command pattern, updates to a shared model would be broadcast to multiple components to affect a shared data model across many different platforms. Acting like a prevelant system, all the components could be taken down and brought up at will (or as a result of a failure) and achieve complete synchronization.

# Protocols #

This project only contains the basic reliable delivery of sequenced messages to 
multicast groups. The example applications above use this reliable protocol as 
a basic transport mechanism in supporting their application-specific protocols. 
The protocols required to implements the above are expected to be made available
in separate projects.

# Using The Toolkit #

The unit tsts have been written to provide basic usage scenarios. Consulting 
these tests will provide examplars for usage.

