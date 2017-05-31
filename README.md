# demo-wsrm
Demo on using a persistent WS-RM Bridge

## Build

$ mvn clean install

## Deploy

feature:repo-list
feature:repo-add mvn:org.apifocal.demo.wsrm/features/1.0-SNAPSHOT/xml/features
feature:list | grep demo

