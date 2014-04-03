CPLEXBINDIR = /com/cplex/bin/x86_rhel4.0_3.4/
CPLEXLIB = /com/cplex/lib/cplex.jar

# JDK version
JDK = /pro/java/Linux/jdk1.5.0/bin/

# Class path
CLASSPATH = $(CPLEXLIB):.:..

# Virtual Machine command
VM = java

ROPTIONS = -enableassertions -Djava.library.path=$(CPLEXBINDIR) -Xms200m -Xmx200m

CODEBASEDIR = edu/brown/cs/
SICSBASEDIR = se/sics/tac/

all:
	javac -source 1.5 -classpath $(CLASSPATH) $(CODEBASEDIR)agent/*.java $(CODEBASEDIR)algorithm/*.java $(CODEBASEDIR)bidder/*.java $(CODEBASEDIR)completer/*.java $(CODEBASEDIR)modeler/*.java $(CODEBASEDIR)props/*.java $(CODEBASEDIR)simulator/*.java $(CODEBASEDIR)tac/*.java $(CODEBASEDIR)tacds/*.java $(SICSBASEDIR)aw/*.java $(SICSBASEDIR)util/*.java props/*.java

clean:
	rm -f $(CODEBASEDIR)agent/*.class 
	rm -f $(CODEBASEDIR)algorithm/*.class 
	rm -f $(CODEBASEDIR)bidder/*.class 
	rm -f $(CODEBASEDIR)completer/*.class 
	rm -f $(CODEBASEDIR)modeler/*.class 
	rm -f $(CODEBASEDIR)props/*.class 
	rm -f $(CODEBASEDIR)simulator/*.class 
	rm -f $(CODEBASEDIR)tac/*.class
	rm -f $(CODEBASEDIR)tacds/*.class
	rm -f $(SICSBASEDIR)aw/*.class
	rm -f $(SICSBASEDIR)util/*.class
	rm -f com/botbox/util/*.class
	rm -f props/*.class

runagent:
	$(JDK)$(VM) $(ROPTIONS) -cp $(CLASSPATH) se.sics.tac.aw.TACAgent
	
runsimulation:
	$(JDK)$(VM) $(ROPTIONS) -cp $(CLASSPATH) edu.brown.cs.simulator.SimulatorFactory
