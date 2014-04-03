#!/usr/bin/perl -w

$CPLEXBINDIR = "/com/cplex/bin/x86_rhel4.0_3.4/";
$CPLEXLIB = "/com/cplex/lib/cplex.jar";
$JDK = "/pro/java/Linux/jdk1.5.0/bin/";
$CLASSPATH = "$CPLEXLIB:.:..";
$VM = "java";
$ROPTIONS = "-enableassertions -Djava.library.path=$CPLEXBINDIR -Xms200m -Xmx200m";

$file_name = $ARGV[0];
$command = "$JDK$VM $ROPTIONS -cp $CLASSPATH edu.brown.cs.simulator.SimulatorFactory $file_name";

system($command);
