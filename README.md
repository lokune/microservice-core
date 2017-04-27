# `Microservice`

In a 2014 article, Martin Fowler stated:

"The microservice architectural style is an approach to developing a single application as a suite of small services, each running in its own process and communicating with lightweight mechanisms, often an HTTP resource API. These services are built around business capabilities and independently deployable by fully automated deployment machinery."

Joshua Davis reinterpreted Martin Fowler's "micro":

"A MicroService is a misused term.  The best way to understand microservices is to understand that the concept of "micro" does not describe the size of the application but the domain by which the service encompasses.  It is well explained by many others, but at it's most simple it's the idea that if you can encapsulate a concept's data and functionality into a discrete set of commands you have a microservice."

# `core`

    noun

    the center or most important part of something

Organizations that have adopted the microservice architecture will realize that as we create more and more services, there arises the need to share code. For example, more than one service will need to connect and persist data to a database management system. It's a good idea to minimize repetition of logic from service to service. This is what inspired `microservice-core` which is an abstraction over some of the most popular `Scala` libraries so that each service only needs to do very little to interact with a DBMS, write & read files from HDFS etc.

## Getting Started

To get started with SBT, add the following to your `build.sbt` file:

`resolvers += "jitpack" at "https://jitpack.io"`

and then add:

`libraryDependencies += "com.github.lokune" %% "microservice-core" % "Tag"`

The following are the modules you can use in your service:

* `core-database`

  *  Get CRUD on postgres tables for FREE

  *  KISS postgres migrations

  *  Get CRUD on mongo collections for FREE
