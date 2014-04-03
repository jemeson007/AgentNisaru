#!/usr/bin/perl -w

	sub run;
	sub function;
	sub killall;
	sub analyze;
	
	$directory=`pwd`;
	chomp($directory);


	@hosts=qw(
	davinci
	eastman
	eich
	engelbart
	);
	
	@names=qw(
	config/simset/last.decision
	config/simset/last.game
	config/simset/first.decision
	config/simset/first.game
	);

	if (@ARGV == 0) {
		run();
	}

	if (@ARGV != 0 && $ARGV[0] eq "kill") {
		killall();
	}

	if (@ARGV != 0 && $ARGV[0] eq "analyze") {
		analyze();
	}

	exit 0;

sub analyze {
	foreach $name (@names) {
		$short_name = $name;
		$removal = "config/simset/";
		$short_name =~ s/$removal//;
		system("analyze.pl -f ../result/$short_name.out -a -s");
	}
}

sub run {
	for($i=0;$i<@names;$i++){
		$name = $names[$i];
		$host = $hosts[$i];
		function();
	}
}

sub killall {
	for($i=0;$i<@names;$i++){
		$host = $hosts[$i];
		$command = "ssh $host -x \"";
		$command = $command."killall -u sjlee;";
		$command = $command."\"";
		system($command);
	}
}

sub function {
	$command = "ssh $host -o ConnectTimeout=0 -x exit;";
	
	$res = system($command);
	if($res==0){
		$short_name = $name;
		$removal = "config/simset/";
		$short_name =~ s/$removal//;
		
		$command = "ssh $host -x \"";
		$command = $command."cd $directory;";
		$command = $command."cd ..; simulate.pl $name > result/$short_name.out&";
		$command = $command."renice 10 -u sjlee;";
		$command = $command."\"";
		
		$pid = fork();
		if ($pid == 0) {
			print $command."\n";
			$res = system($command);
			exit(0);
		}
	}
}
