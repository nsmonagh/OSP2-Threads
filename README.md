## OSP2-Threads

A reconstruction of the CPU scheduler unit within OSP2.

## Installation and Setup Instructions

Clone this repository. You will need `java` and `javac` installed on your machine.  

Installation for Ubuntu:

`sudo apt-get install default-jdk`

To Run the Application:  

* Navigate to the cloned repository and run either to compile:

  * (windows) javac –g –classpath .;OSP.jar; -d . *.java
  
  * (unix) javac –g –classpath .:OSP.jar: -d . *.java

* Then to run the application type either:

  * (windows) java –classpath .;OSP.jar osp.OSP
  
  * (unix) java –classpath .:OSP.jar osp.OSP 
