= Hokan the Java IRC bot =

 == BOT MODULES

   Bot is divided in three modules

    - HokanIO
    - HokanEngine
    - HokanServices

    Each is independent Java SpringBoot application.
    Applications communicates using JMS/ActiveMQ.
    Applications store data using JPA.

       |------- ActiveMQ ----------|
       |            |              |
       |           JMS             |
       |            |              |
    HokanIO    HokanEngine    HokanServices
       \            |              /
        \           |             /
         \----------|------------/
                    |
                   JPA
                    |
        MariaDB: hokan_ng_springboot

    All components MUST use same database instance.

    All of the modules must be run same time Bot to operate fully. Starting HokanIO module will connect
    Bot to IRC but alone it does nothing else but joins channels and accepts Admin Token command.

 == JMS/ActiveMQ

   By default all components try to use ActiveMQ from "tcp://localhost:61616". This can be override either
   with command line parameter --JmsBrokerUrl=<anotherActiveMq:XXXX> or by changing application.properties
   and building jar again.

   Download latest Apache ActiveMQ http://activemq.apache.org/download.html and extract package.

   ActiveMQ can be started by running bin/activemq.bat (windows) or bin/activemq.sh (linux) with start parameter:

   bin/activemq start

   No further configuration is needed.

 == BUILDING HOKAN MODULES

    NOTE: Apache Maven to be installed so that mvn command is working from command line.
          Java JDK must be installed so that both java and javac is working from command line.
          Use latest version of Apache Maven and Java JDK.

    Assuming you have cloned with git all three modules to directories in following way:

        hokan_ng_springboot-engine/
    bot/hokan_ng_springboot-io/
        hokan_ng_springboot-services/

    Each module need to be build. Go to each module directory and build it:

    mvn install -Dmaven.test.skip=true

    this will generate JAR file to module target/ directory.

 == HOW TO INITIALIZE DATABASE

  DB initialize sql scripts are located in HokanIO module. That module is also used to create
  initial configuration how to connect to IRC network.

  1) Init MariaDB

   mysql < DatabaseInit/create_user.sql
   mysql < DatabaseInit/init_database.sql

   First one only need to run once.
   Later one can also be used to reset database again to empty.

  2) Create initial configuration to connect IRC

   NOTE: This step should only be done once after the DB has been initialized
         If needed, reset the DB and do this again.

   java -jar target\hokan_ng_springboot-io-0.0.1-final.jar --ConfigInit

   This will ask Network name, IrcServerConfig and Channels to use when connecting IRC network.

   NOTE: when run with --ConfigInit also AdminUserToken will be generated:

    ***************************************************
    *           !!!!!!  IMPORTANT !!!!!!              *
    *                                                 *
      ADMIN USER TOKEN IS: <XXXX>
      DO: /msg HokanBot @AdminUserToken <XXXX>
      TO GET ADMIN RIGHTS
    *                                                 *
    *           !!!!!!  IMPORTANT !!!!!!              *
    ***************************************************

    By sending bot message: @AdminUserToken <XXXX> the sender of message will be granted Admin rights.
    Token can only be used once.

 == DEFAULT PARAMETERS

   By default bot will try to use parameters defined in src/main/resources/application.properties
   to connect database.

   Either modify values in application.properties file and re-build jar to apply or when running bot
   override with command line parameters:

   java -jar target\hokan_ng_springboot-io-0.0.1-final.jar --spring.datasource.url=jdbc:mysql://DATABASE_HOST/DATABASE_NAME?autoReconnect=true

   All parameters in application.properties can be override same way ...

 == RUNNING HOKAN MODULES

    HokanIO: this is the one that connects to the IRC network and should be get working first. Once HokanIO
    starts ok and get online other modules is good to start.

    java -jar target\hokan_ng_springboot-io-0.0.1-final.jar

    Now bot should try to connect IRC server defined in step 3) and then join channels.

    HokanEngine: this handles all commands prefixed with !

    java -jar target\hokan_ng_springboot-engine-0.0.1-final.jar

    HokanServices: this collects and updates data used by commands, like weather and TV information.

    java -jar target\hokan_ng_springboot-services-0.0.1-final.jar


