#!/usr/bin/perl -w

# SUB FUNCTION DECLARATION

	sub parse;
	sub read_file;
	sub calculate_average;
	sub sort_score;
	sub print_table;
	sub write_for_matlab; 
	sub write_latex_table;
	sub fix_name;
	sub fix_name_tex;
	sub print_cls_cdf;
	sub print_bid_cdf;

# MEMBER VARIABLES

	@m_filename = ();
	@m_agent = ();
	$m_output = undef;
	$m_agentno = 8;
	$m_matlab = 0;
	$m_tex = 0;
	$m_sort = 0;
	
	@m_said = (); # sorted agent id
	
	@m_gameno = ();
	@m_totaltime = ();
	
	@m_runtime = ();
	@m_score = ();
	@m_utility = ();
	@m_cost = ();
	@m_penalty = ();
	@m_hotel_bonus = ();
	@m_null_package = ();
	@m_closing_price = ();
	@m_allocation = ();
	@m_collection = ();
	@m_transaction = ();

	@m_bid = ();
	@m_bid_counter = ();

	@m_total_sold = ();
	@m_flight_cost = ();
	@m_flight_unused = ();
	@m_flight_used = ();
	@m_hotel_cost = ();
	@m_hotel_unused = ();
	@m_hotel_used = ();
	@m_hotel_numbid = ();
	@m_hotel_avgbid = ();

# MAIN FUNCTION

	parse();
	
	foreach $filename (@m_filename){
		@ARGV = ($filename);
		read_file();
	}
	undef $filename;
	
	calculate_average();
	
	fix_name();
	
	if ($m_sort == 1) { sort_score(); }
	
	print_table();
	
	print_cls_cdf();
	#print_bid_cdf();
	
	if ($m_matlab == 1) { draw_confint(); }
	if ($m_tex == 1) { fix_name_tex(); write_latex_table(); }
	
	exit 0;

sub fix_name {
	for ($i=0; $i<$m_agentno; $i++) {
		if ($m_agent[$i] eq "SAAM") {
			$m_agent[$i] = "SAAT";
		}
		if ($m_agent[$i] eq "SAAm") {
			$m_agent[$i] = "SAAB";
		}
		if ($m_agent[$i] eq "00FO") {
			$m_agent[$i] = "TMU";
		}
		if ($m_agent[$i] eq "00TO") {
			$m_agent[$i] = "TMU*";
		}
		if ($m_agent[$i] eq "02FO") {
			$m_agent[$i] = "BE";
		}
		if ($m_agent[$i] eq "02TO") {
			$m_agent[$i] = "BE*";
		}
		if ($m_agent[$i] eq "AMUO") {
			$m_agent[$i] = "AMU";
		}
		if ($m_agent[$i] eq "SMUO") {
			$m_agent[$i] = "SMU";
		}
	}
}

sub fix_name_tex {
      for ($i=0; $i<$m_agentno; $i++) {
              if ($m_agent[$i] eq "SAAT") {
                      $m_agent[$i] = "\\SAAstar";
              }
              if ($m_agent[$i] eq "SAAB") {
                      $m_agent[$i] = "\\SAA";
              }
              if ($m_agent[$i] eq "TMU") {
                      $m_agent[$i] = "\\TMU";
              }
              if ($m_agent[$i] eq "TMU*") {
                      $m_agent[$i] = "\\TMUstar";
              }
              if ($m_agent[$i] eq "BE") {
                      $m_agent[$i] = "\\eval";
              }
              if ($m_agent[$i] eq "BE*") {
                      $m_agent[$i] = "\\evalstar";
              }
              if ($m_agent[$i] eq "AMU") {
                      $m_agent[$i] = "\\AMU";
              }
              if ($m_agent[$i] eq "SMU") {
                      $m_agent[$i] = "\\SMU";
              }
      }
}


sub print_cls_cdf {
	@cdf = ();
	@pdf = ();
	
	open WRITE, "> cls.out" or die;
	for($i=0;$i<@m_totaltime;$i++) {
		for($a=8; $a<15; $a++) {
			print WRITE $m_closing_price[$i][$a];
			print WRITE ",";
		}
		print WRITE $m_closing_price[$i][15];
		print WRITE "\n";
	}
	close WRITE;
	
	for($i=0;$i<1200;$i++) {
		for($a=8; $a<16; $a++) {
			$pdf[$i][$a] = 0;
		}
	}
	
	for($i=0;$i<@m_totaltime;$i++) {
		for($a=8; $a<16; $a++) {
			$pdf[int($m_closing_price[$i][$a])][$a]++;
		}
	}
	
	for($a=8; $a<16; $a++) {
		$cdf[0][$a] = $pdf[0][$a] / @m_totaltime;
	}

	for($i=1;$i<1200;$i++) {
		for($a=8; $a<16; $a++) {
			$cdf[$i][$a] = $cdf[$i-1][$a]+$pdf[$i][$a] / @m_totaltime;
		}
	}
	
	open WRITE, "> cls.pdf.out" or die;
	for ($i=0;$i<1200;$i++) {
		print WRITE "$pdf[$i][8],";
		print WRITE "$pdf[$i][9],";
		print WRITE "$pdf[$i][10],";
		print WRITE "$pdf[$i][11],";
		print WRITE "$pdf[$i][12],";
		print WRITE "$pdf[$i][13],";
		print WRITE "$pdf[$i][14],";
		print WRITE "$pdf[$i][15]\n";
	}
	close WRITE;

	open WRITE, "> cls.cdf.out" or die;
	for ($i=0;$i<1200;$i++) {
		print WRITE "$cdf[$i][8],";
		print WRITE "$cdf[$i][9],";
		print WRITE "$cdf[$i][10],";
		print WRITE "$cdf[$i][11],";
		print WRITE "$cdf[$i][12],";
		print WRITE "$cdf[$i][13],";
		print WRITE "$cdf[$i][14],";
		print WRITE "$cdf[$i][15]\n";
	}
	close WRITE;
}

# ONLY FOR SS HOTEL
sub print_bid_cdf {
	@cdf = ();
	@pdf = ();
	
	for($i=0;$i<1200;$i++) {
		for($a=0; $a<$m_agentno; $a++) {
			$pdf[$i][$a] = 0;
		}
	}
	
	for($a=0; $a<$m_agentno; $a++) {
		for($auc = 8; $auc < 12; $auc++) {
			for($i=0;$i<$m_bid_counter[$a][$auc];$i++) {
				$pdf[int($m_bid[$a][$auc][$i])][$a]++;
			}
		}
	}
	
	for($a=0; $a<$m_agentno; $a++) {
		$counter = 0;
		for($auc = 8; $auc < 12; $auc++) {
			$counter += $m_bid_counter[$a][$auc];
		}
		$cdf[0][$a] = $pdf[0][$a] / $counter;
	}

	for($i=1;$i<1200;$i++) {
		for($a=0; $a<$m_agentno; $a++) {
			$counter = 0;
			for($auc = 8; $auc < 12; $auc++) {
				$counter += $m_bid_counter[$a][$auc];
			}
			$cdf[$i][$a] = $cdf[$i-1][$a]+$pdf[$i][$a] / $counter;
		}
	}
	
	open WRITE, "> bid.ss.cdf.out" or die;
	for ($i=0;$i<1200;$i++) {
		for($a=0; $a<$m_agentno-1; $a++) {
			print WRITE "$cdf[$i][$a],";
		}
		$a = $m_agentno-1;
		print WRITE "$cdf[$i][$a]\n";
	}
	close WRITE;
}


sub print_table {
	
	print "\n";
	print "GAME.NO\t"; printf "%5d", (@m_totaltime-1); print"\n";
	print "GAME.T\t";  printf "%5d", $m_totaltime[@m_totaltime-1]; print"\n";
	print "H.PRICE\t"; for($a=8; $a<16; $a++) { printf "%5d ", $m_closing_price[@m_totaltime-1][$a]; } print "\n";
	print "H.DMAND\t"; for($a=8; $a<16; $a++) { printf "%5d ", $m_total_sold[$a]; } print "\n";
	print "\n";
	print "AGENT\t";   for($i=0; $i<$m_agentno; $i++) { printf "%5s ", $m_agent[$m_said[$i]]; } print "\n";
	print "SCORE\t";   for($i=0; $i<$m_agentno; $i++) { printf "%5d ", $m_score[$m_said[$i]][$m_gameno[$m_said[$i]]]; } print "\n";
	print "UTIL\t";    for($i=0; $i<$m_agentno; $i++) { printf "%5d ", $m_utility[$m_said[$i]][$m_gameno[$m_said[$i]]]; } print "\n";
	print "COST\t";    for($i=0; $i<$m_agentno; $i++) { printf "%5d ", $m_cost[$m_said[$i]][$m_gameno[$m_said[$i]]]; } print "\n";
	print "PENALTY\t"; for($i=0; $i<$m_agentno; $i++) { printf "%5d ", $m_penalty[$m_said[$i]][$m_gameno[$m_said[$i]]]; } print "\n";
	print "NULL\t";    for($i=0; $i<$m_agentno; $i++) { printf "%5.2f ", $m_null_package[$m_said[$i]][$m_gameno[$m_said[$i]]]; } print "\n";
	print "F.UNUSE\t"; for($i=0; $i<$m_agentno; $i++) { printf "%5.2f ", $m_flight_unused[$m_said[$i]]; } print"\n";
	print "F.USED\t";  for($i=0; $i<$m_agentno; $i++) { printf "%5.1f ", $m_flight_used[$m_said[$i]]; } print"\n";
	print "F.TCOST\t"; for($i=0; $i<$m_agentno; $i++) { printf "%5.0f ", $m_flight_cost[$m_said[$i]]; } print"\n";
	print "F.ACOST\t"; for($i=0; $i<$m_agentno; $i++) { printf "%5.0f ", $m_flight_cost[$m_said[$i]]/(0.0000001+$m_flight_unused[$m_said[$i]]+$m_flight_used[$m_said[$i]]); } print"\n";
	print "H.NUMBD\t"; for($i=0; $i<$m_agentno; $i++) { printf "%5.1f ", $m_hotel_numbid[$m_said[$i]]; } print "\n";
	print "H.AVGBD\t"; for($i=0; $i<$m_agentno; $i++) { printf "%5.0f ", $m_hotel_avgbid[$m_said[$i]]; } print "\n";
	print "H.BONUS\t"; for($i=0; $i<$m_agentno; $i++) { printf "%5.0f ", $m_hotel_bonus[$m_said[$i]][$m_gameno[$m_said[$i]]]; } print"\n";
	print "H.WON\t";   for($i=0; $i<$m_agentno; $i++) { printf "%5.1f ", $m_hotel_unused[$m_said[$i]]+$m_hotel_used[$m_said[$i]]; } print"\n";
	print "H.UNUSE\t"; for($i=0; $i<$m_agentno; $i++) { printf "%5.1f ", $m_hotel_unused[$m_said[$i]]; } print"\n";
	print "H.USED\t";  for($i=0; $i<$m_agentno; $i++) { printf "%5.1f ", $m_hotel_used[$m_said[$i]]; } print"\n";
	print "H.TCOST\t"; for($i=0; $i<$m_agentno; $i++) { printf "%5d ", $m_hotel_cost[$m_said[$i]]; } print "\n";
	print "H.ACOST\t"; for($i=0; $i<$m_agentno; $i++) { printf "%5.1f ", $m_hotel_cost[$m_said[$i]]/(0.0000001+$m_hotel_unused[$m_said[$i]]+$m_hotel_used[$m_said[$i]]); } print"\n";
	print "GAME.NO\t"; for($i=0; $i<$m_agentno; $i++) { printf "%5d ", $m_gameno[$m_said[$i]]; } print "\n";
	print "RUNTIME\t"; for($i=0; $i<$m_agentno; $i++) { printf "%5d ", $m_runtime[$m_said[$i]][$m_gameno[$m_said[$i]]]; } print "\n";
	print "\n";
	
}

sub write_latex_table {	
	print "% Writing the table to \'$m_output.tex\' file.\n";
	open WRITE, "> $m_output.tex" or die "analyze.pl error : Can't open $m_output.tex.\n";

	print WRITE "\\begin{tabular}{|l"; for ($i=0; $i<9; $i++) { print WRITE "|c"; } print WRITE "|}\n";
	
	for ($j=0; $j<$m_agentno/8; $j++) {
		$bgn = $j * 8;
		$end = $j * 8 + 8; if ($m_agentno < $end) { $end = $m_agentno; }
		
		print WRITE "\\hline\n";
		print WRITE "Agent";     for($i=$bgn; $i<$end; $i++) { printf WRITE "&%4s", $m_agent[$m_said[$i]]; } print WRITE "\\\\\n";
		print WRITE "\\hline\n";
		print WRITE "Score";     for($i=$bgn; $i<$end; $i++) { printf WRITE "&%4d ", $m_score[$m_said[$i]][$m_gameno[$m_said[$i]]]; } print WRITE "\\\\\n";
		print WRITE "Utility";   for($i=$bgn; $i<$end; $i++) { printf WRITE "&%4d ", $m_utility[$m_said[$i]][$m_gameno[$m_said[$i]]]; } print WRITE "\\\\\n";
		print WRITE "Cost";      for($i=$bgn; $i<$end; $i++) { printf WRITE "&%4d ", $m_cost[$m_said[$i]][$m_gameno[$m_said[$i]]]; } print WRITE "\\\\\n";
		print WRITE "Penalty";   for($i=$bgn; $i<$end; $i++) { printf WRITE "&%4d ", $m_penalty[$m_said[$i]][$m_gameno[$m_said[$i]]]; } print WRITE "\\\\\n";
		print WRITE "Null";      for($i=$bgn; $i<$end; $i++) { printf WRITE "&%4.2f ", $m_null_package[$m_said[$i]][$m_gameno[$m_said[$i]]]; } print WRITE "\\\\\n";
		print WRITE "F.won";     for($i=$bgn; $i<$end; $i++) { printf WRITE "&%4.2f ", $m_flight_unused[$m_said[$i]]+$m_flight_used[$m_said[$i]]; } print WRITE "\\\\\n";		
		print WRITE "F.unused";  for($i=$bgn; $i<$end; $i++) { printf WRITE "&%4.2f ", $m_flight_unused[$m_said[$i]]; } print WRITE "\\\\\n";
		print WRITE "F.used";    for($i=$bgn; $i<$end; $i++) { printf WRITE "&%4.1f ", $m_flight_used[$m_said[$i]]; } print WRITE "\\\\\n";
		print WRITE "F.totCost"; for($i=$bgn; $i<$end; $i++) { printf WRITE "&%4.0f ", $m_flight_cost[$m_said[$i]]; } print WRITE "\\\\\n";
		print WRITE "F.avgCost"; for($i=$bgn; $i<$end; $i++) { printf WRITE "&%4.0f ", $m_flight_cost[$m_said[$i]]/($m_flight_unused[$m_said[$i]]+$m_flight_used[$m_said[$i]]); } print WRITE "\\\\\n";
		print WRITE "H.numBids"; for($i=$bgn; $i<$end; $i++) { printf WRITE "&%4.1f ", $m_hotel_numbid[$m_said[$i]]; } print WRITE "\\\\\n";
		print WRITE "H.avgBid";  for($i=$bgn; $i<$end; $i++) { printf WRITE "&%4.0f ", $m_hotel_avgbid[$m_said[$i]]; } print WRITE "\\\\\n";
		print WRITE "H.bonus";   for($i=$bgn; $i<$end; $i++) { printf WRITE "&%4.0f ", $m_hotel_bonus[$m_said[$i]][$m_gameno[$m_said[$i]]]; } print WRITE "\\\\\n";
		print WRITE "H.won";     for($i=$bgn; $i<$end; $i++) { printf WRITE "&%4.1f ", $m_hotel_unused[$m_said[$i]]+$m_hotel_used[$m_said[$i]]; } print WRITE "\\\\\n";
		print WRITE "H.unused";  for($i=$bgn; $i<$end; $i++) { printf WRITE "&%4.1f ", $m_hotel_unused[$m_said[$i]]; } print WRITE "\\\\\n";
		print WRITE "H.used";    for($i=$bgn; $i<$end; $i++) { printf WRITE "&%4.1f ", $m_hotel_used[$m_said[$i]]; } print WRITE "\\\\\n";
		print WRITE "H.totCost"; for($i=$bgn; $i<$end; $i++) { printf WRITE "&%4d ", $m_hotel_cost[$m_said[$i]]; } print WRITE "\\\\\n";
		print WRITE "H.avgCost"; for($i=$bgn; $i<$end; $i++) { printf WRITE "&%4.1f ", $m_hotel_cost[$m_said[$i]]/($m_hotel_unused[$m_said[$i]]+$m_hotel_used[$m_said[$i]]); } print WRITE "\\\\\n";
		#print WRITE "Runtime"; for($i=$bgn; $i<$end; $i++) { printf WRITE "&%4d ", $m_runtime[$m_said[$i]][$m_gameno[$m_said[$i]]]; } print WRITE "\\\\\n";
		#print WRITE "Game.No"; for($i=$bgn; $i<$end; $i++) { printf WRITE "&%4d ", $m_gameno[$m_said[$i]]; } print WRITE "\\\\\n";
	}

	$game_no = @m_totaltime-1;	
	$filename = $m_output;
	$filename =~ s/_/\\_/;
	
	print WRITE "\\hline\n";
	print WRITE "\\end{tabular}\n";
	
	undef $filename;
	undef $game_no;
	
	close(WRITE);
}

sub draw_confint {

	print "% Writing to \'$m_output.score, .utility, .cost, .agent\' files.\n";
	open SCORE, "> $m_output.score" or die "analyze.pl error : Can't open $m_output.score.\n";;
	open UTILITY, "> $m_output.utility" or die "analyze.pl error : Can't open $m_output.utility.\n";
	open COST, "> $m_output.cost" or die "analyze.pl error : Can't open $m_output.cost.\n";
	open AGENT, "> $m_output.agent" or die "analyze.pl error : Can't open $m_output.agent.\n";
	
	$game_no = Inf;
	
	for($i=0; $i<$m_agentno; $i++) {
		if ($game_no > $m_gameno[$i]) {
			$game_no = $m_gameno[$i];
		}
	}
	
	for($i=0; $i<$m_agentno; $i++) {
		print AGENT "$m_agent[$m_said[$i]]\n";
	}

	for($g=0; $g<$game_no; $g++) {
		for($i=0; $i<$m_agentno; $i++) {
			if ($i < $m_agentno - 1) {
				$string = ",";
			} else {
				$string = "\n";
			}
			
			print SCORE "$m_score[$m_said[$i]][$g]$string";
			print UTILITY "$m_utility[$m_said[$i]][$g]$string";
			print COST "$m_cost[$m_said[$i]][$g]$string";
			
			undef $string;
		}
	}
	
	undef $game_no;
	
	close SCORE;
	close UTILITY;
	close COST;
	close AGENT;

	open MATLAB, "> matlab_temp.m" or die "analyze.pl error : Can't open matlab_temp.\n";
	print MATLAB "cd matlab;\n";
	print MATLAB "printallconfint(\'../$m_output\');\n";
	close MATLAB;
	
	system "matlab < matlab_temp.m";
	
	#system "rm $m_output.score -f;";
	system "rm $m_output.utility -f;";
	system "rm $m_output.cost -f;";
	system "rm $m_output.agent -f;";
	system "rm matlab_temp.m -f;";
}

sub sort_score {

	print "% Sorting by score.\n";
	
	@temp_score = ();
	
	for($i=0; $i<$m_agentno; $i++) {
		$temp_score[$i] = $m_score[$i][$m_gameno[$i]];
	}
	
	for($i=0; $i<$m_agentno; $i++) {
		$max_index = 0;
		$max_score = $temp_score[0];
		
		for($j=0; $j<$m_agentno; $j++) {
			if ($max_score < $temp_score[$j]) {
				$max_score = $temp_score[$j];
				$max_index = $j;
			}
		}
		
		$m_said[$i] = $max_index;
		$temp_score[$max_index] = -Inf;
		
		undef $max_index;
		undef $max_score;
	}
	
	print "% Sorted order : @m_said.\n";
	
	undef @temp_score;
}

sub calculate_average {
	
	$game_no = @m_totaltime;
	
	$m_totaltime[$game_no] = 0;
	for($a=8; $a<16; $a++) { 
		$m_closing_price[$game_no][$a] = 0;
		$m_total_sold[$a] = 0;
	}
	
	for($g=0; $g<$game_no; $g++) {
		$m_totaltime[$game_no] += $m_totaltime[$g]/$game_no;
		for($a=8; $a<16; $a++) { $m_closing_price[$game_no][$a] += $m_closing_price[$g][$a]/$game_no; }
	}
	
	undef $game_no;
	
	for($i=0; $i<$m_agentno; $i++) {
		$m_hotel_numbid[$i] = 0;
		$m_hotel_avgbid[$i] = 0;
	}
	
	for($i=0; $i<$m_agentno; $i++) {
		
		$game_no = $m_gameno[$i];
		
		$m_runtime[$i][$game_no] = 0;
		$m_score[$i][$game_no] = 0;
		$m_utility[$i][$game_no] = 0;
		$m_cost[$i][$game_no] = 0;
		$m_hotel_bonus[$i][$game_no] = 0;
		$m_penalty[$i][$game_no] = 0;
		$m_null_package[$i][$game_no] = 0;
		$m_hotel_numbid[$i] = 0;
		$m_hotel_avgbid[$i] = 0; 

		for($a=0; $a<16; $a++) {
			$m_collection[$i][$game_no][$a] = 0;
			$m_allocation[$i][$game_no][$a] = 0;
		}
		
		$m_flight_cost[$i] = 0;
		$m_flight_unused[$i] = 0;
		$m_flight_used[$i] = 0;
		
		$m_hotel_cost[$i] = 0;
		$m_hotel_unused[$i] = 0;
		$m_hotel_used[$i] = 0;
		
		for($g=0; $g<$game_no; $g++) {
			$m_runtime[$i][$game_no]+=$m_runtime[$i][$g]/$game_no;
			$m_score[$i][$game_no]+=$m_score[$i][$g]/$game_no;
			$m_utility[$i][$game_no]+=$m_utility[$i][$g]/$game_no;
			$m_cost[$i][$game_no]+=$m_cost[$i][$g]/$game_no;
			$m_hotel_bonus[$i][$game_no]+=$m_hotel_bonus[$i][$g]/$game_no;
			$m_penalty[$i][$game_no]+=$m_penalty[$i][$g]/$game_no;
			$m_null_package[$i][$game_no]+=$m_null_package[$i][$g]/$game_no;
		
			for($a=0; $a<16; $a++) {
				$m_collection[$i][$game_no][$a]+=$m_collection[$i][$g][$a]/$game_no;
				$m_allocation[$i][$game_no][$a]+=$m_allocation[$i][$g][$a]/$game_no;
				$m_total_sold[$a]+=$m_collection[$i][$g][$a]/$game_no;
			}
		
			for($a=8; $a<16; $a++) {
				$m_hotel_cost[$i]+=$m_transaction[$i][$g][$a]/$game_no;
			}
		
			for($a=0; $a<8; $a++) {
				$m_flight_cost[$i]+=$m_transaction[$i][$g][$a]/$game_no;
			}
		}

		for($a=0; $a<8; $a++) {
			$m_flight_unused[$i]+=($m_collection[$i][$game_no][$a]-$m_allocation[$i][$game_no][$a]);
			$m_flight_used[$i]+=$m_allocation[$i][$game_no][$a];
		}
		
		for($a=8; $a<16; $a++) {
			$m_hotel_unused[$i]+=($m_collection[$i][$game_no][$a]-$m_allocation[$i][$game_no][$a]);
			$m_hotel_used[$i]+=$m_allocation[$i][$game_no][$a];
			$m_hotel_numbid[$i] += $m_bid_counter[$i][$a] / $m_gameno[$i];
			$m_hotel_avgbid[$i] += $m_bid[$i][$a] / $m_gameno[$i] / $m_bid_counter[$i][$a];
		}
		
		undef $game_no;
	}
}

sub read_file {
	$game_no = 0;
	$counter = 0;

	@map_gaid_aid = (); # from game agent id to agent id
	@map_gaid_gid = (); # from game agent id to game id
	
	$line = 0;
	@fields = ();
	
	open(LOG, $ARGV[0]);
	while($line = <LOG>) {
		if ($line =~ /^%,end,/) {
                        if (@m_totaltime + $game_no < 1000) {
                                $game_no++;
                        }
		}
	}
	close(LOG);	
	
	
	open(LOG, $ARGV[0]);
	while($line = <LOG>) { if ($counter < $game_no) {
		chomp($line);
		
		#,bgn,game
		if($line =~ /^%,bgn,/) {
		}
		
		#,agt,agent_id,name
		if($line =~ /^%,agt,/) {
			@fields = split(/,/,$line);
			
			$agent_id = $fields[2];
			$agent_name = $fields[3];
			
			$map_gaid_aid[$agent_id] = -1;
			
			for ($i=0; $i<@m_agent; $i++) {
				if ($agent_name =~ /^SAABX/) {
					$agent_name = "SAAX";
				}
			
				if ($agent_name =~ /^$m_agent[$i]/) {
					$map_gaid_aid[$agent_id] = $i;
					$map_gaid_gid[$agent_id] = $m_gameno[$i];
					$m_gameno[$i]++;
					
					for ($a=0; $a<16; $a++) {
						$m_transaction[$map_gaid_aid[$agent_id]][$map_gaid_gid[$agent_id]][$a] = 0;
					}
				}
			}
			
			undef $agent_id;
			undef $agent_name;
		}
		
		#,run,time,agent,calculation_time
		if($line =~ /^%,run,/) {
			@fields = split(/,/,$line);
			
			if ($map_gaid_aid[$fields[3]] != -1) {
				$m_runtime[$map_gaid_aid[$fields[3]]][$map_gaid_gid[$fields[3]]] += $fields[4];
			}
		}
		
		#,trs,time,buyer,seller,auction,quantity,price
		if($line =~ /^%,trs,/) {
			@fields = split(/,/,$line);

			if ($map_gaid_aid[$fields[3]] != -1) {
				$m_transaction[$map_gaid_aid[$fields[3]]][$map_gaid_gid[$fields[3]]][$fields[5]] += $fields[6] * $fields[7];
			}
		}
		
		#,bid,time,agent,auction,quantity,price
		if($line =~ /^%,bid,/) {
			@fields = split(/,/,$line);
			
			$agent_id = $map_gaid_aid[$fields[3]];
			if ($agent_id != -1) {
				$auction_id = $fields[4];
				for ($i=0;$i<$fields[5];$i++) {
					$m_bid[ $agent_id ][ $auction_id ][ $m_bid_counter[$agent_id][$auction_id] ] = $fields[6];
					$m_bid_counter[ $agent_id ][ $auction_id ]++;
				}
			}
		}

		
		#, 1 ,  2, ,  3  ,   4   , 5  , 6     ,  7       , 8
		#,scr,agent,score,utility,cost,penalty,hotelbonus,nullpackage
		if($line =~ /^%,scr,/) {
			@fields = split(/,/,$line);
			
			$agent_id = $map_gaid_aid[$fields[2]];
			
			if ($agent_id != -1) {

				$game_id = $map_gaid_gid[$fields[2]];
				
				$m_score[$agent_id][$game_id]   = $fields[3];
				$m_utility[$agent_id][$game_id]    = $fields[4];
				$m_cost[$agent_id][$game_id]    = $fields[5];
				$m_penalty[$agent_id][$game_id] = $fields[6];
				$m_hotel_bonus[$agent_id][$game_id]  = $fields[7];
				$m_null_package[$agent_id][$game_id] = $fields[8];

				undef $game_id;

			}
						
			undef $agent_id;
		}
		
		#,cls,auction,price
		if($line =~ /^%,cls,/) {
			@fields = split(/,/,$line);
			$m_closing_price[@m_totaltime][$fields[2]] = $fields[3];
		}
		
		#,all,agent,...
		if($line =~ /^%,all,/) {
			@fields = split(/,/,$line);
			
			if ($map_gaid_aid[$fields[2]] != -1) {
				for($auction_id=0; $auction_id<16; $auction_id++){
					$m_allocation[$map_gaid_aid[$fields[2]]][$map_gaid_gid[$fields[2]]][$auction_id] = $fields[3+$auction_id];
				}
			}
		}
		
		#,cll,agent,...
		if($line =~ /^%,cll,/) {
			@fields = split(/,/,$line);
			if ($map_gaid_aid[$fields[2]] != -1) {
				for($auction_id=0; $auction_id<16; $auction_id++){
					$m_collection[$map_gaid_aid[$fields[2]]][$map_gaid_gid[$fields[2]]][$auction_id] = $fields[3+$auction_id];
				}
			}
		}
		
		#,end,game,length
		if($line =~ /^%,end,/) {
			@fields = split(/,/,$line);
			$m_totaltime[@m_totaltime] = $fields[3];
			$counter++;
		}
	} }
	
	close(LOG);
	
	undef $game_no;
	undef $counter;
	undef @map_gaid_aid;
	undef @map_gaid_gid;
	undef $line;
	undef @fields;
}

sub parse() {
	$option = "-h";
	if (@ARGV == 0) {
		@ARGV = ("-h");
	}
	
	foreach $argument (@ARGV) {
		if ($argument =~ /^-/) {
			$option = $argument;
		}
		
		$is_executed = 0;
		
		if ($option =~ /^(-f|--file)/) {
			if ($argument =~ /^[^-]/) {
				$m_filename[@m_filename] = $argument;
				open WRITE, "< $argument" or die "analyze.pl error : Can't open $argument.\n";
				close WRITE;
			}
			$is_executed = 1;
		}
		
		if ($option =~ /-o/ || $option =~ /--output/) {
			if ($argument =~ /^[^-]/) {
				$m_output = $argument;
			}
			$is_executed = 1;
		}
	
		if ($option =~ /-n/ || $option =~ /--agentno/) {
			if ($argument =~ /^[^-]/) {
				if ($argument =~ /^-?\d+$/) {
					$m_agentno = $argument;
				} else {
					die "analyze.pl error : agentno argument is not an integer.\n";
				}
			}
			$is_executed = 1;
		}
		
		if ($option =~ /-a/ || $option =~ /--agent/) {
			if ($argument =~ /^[^-]/) {
				$m_agent[@m_agent] = $argument;
			}
			$is_executed = 1;
		}
	
		if ($option =~ /-m/ || $option =~ /--matlab/) {
			$m_matlab = 1;
			$is_executed = 1;
		}
	
		if ($option =~ /-t/ || $option =~ /--tex/) {
			$m_tex = 1;
			$is_executed = 1;
		}
	
		if ($option =~ /-s/ || $option =~ /--sort/) { 
			$m_sort = 1;
			$is_executed = 1;
		}
	
		if ($option =~ /-h/ || $option =~ /--help/) {
			print "ANALYZE.PL\n";
			print "\tcreated by Seong Jae Lee\n";
			print "\tlast update at 2007/04/19\n";
			print "\n";
			print "SYNOPSIS\n";
			print "\tanalyze.pl [-h --help] [-f --file FILE ...] [-o --output FILE]\n";
			print "\t[-n --agentno INTEGER] [-a -agent AGENT ...] [-m --matlab] [-t --tex] [-s --sort]\n";
			print "\n";
			print "EXAMPLE\n";
			print "\tanalyze.pl -f ../normal.out -n 16 -m -t\n";
			print "\tanalyze.pl -f ../normal.cslab1a.out ../normal.cslab2a.out -o ../normal -s\n";
			print "\tanalyze.pl -f ../random.out -a SAAX SAA0 00T 00F 02T 02F AMU SMU\n";
			print "\n";
			print "DESCRIPTION\n";
			print "\t-h, --help	Display the analayze.pl information.\n";
			print "\t-f, --file	Identify the input files. The extension of the file should be '.out'.\n";
			print "\t-o, --output	Identify the name of the output file. \n\t\t\tThe default is the first input filename without its '.out' extension.\n";
			print "\t-n, --agentno	Identify the number of agents in the game. The default is 8.\n";
			print "\t-a, --agent	Identify the name of agents in the game. \n";
			print "\t\t\tWhen agentno is 8, default is SAAT, SAAB, 00F, 00T, 02F, 02T, AMU, SMU.\n";
			print "\t\t\tWhen agentno is 10, default is SAAX, SAAM, SAAm, SAA0, 00F, 00T, 02F,\n\t\t\t02T, AMU, SMU.\n";
			print "\t\t\tWhen agentno is 16, default is SAAX, SAAM, SAAm, SAA0, 00FO, 00TO,\n\t\t\t02FO, 02TO, AMUO, SMUO, 00FN, 00TN, 02FN, 02TN, AMUN, SMUN.\n";
			print "\t-m, --matlab	Create .score, .utility, .cost, .agent files.\n";
			print "\t-t, --tex	Create .tex table file.\n";
			print "\t-s, --sort	All output files are displayed in the order sorted by score.\n";
			print "\t\n";
			exit 0;
		}
	
		if ($is_executed == 0) {
			die "analyze.pl error : Invalid command arguments. Try analyze.pl -h.";
		}
	}
	
	if (@m_filename == 0) {
		die "analyze.pl error : Input file is null.\n";
	}
	
	if (!$m_output) {
		$m_output = $m_filename[0];
		$m_output =~ s/.out$//;
	}
	
	if (@m_agent != 0) {
		if (@m_agent != $m_agentno) {
			die "analyze.pl error : number of agent names mismatches agentno\n";
		}
	}
	
	if (@m_agent == 0) {
		if ($m_agentno == 7) { @m_agent = qw(SAA0 00FO 00TO 02FO 02TO AMUO SMUO); }
		if ($m_agentno == 8) { @m_agent = qw(SAAT SAAB 00FO 00TO 02FO 02TO AMUO SMUO); }
		if ($m_agentno == 10) { @m_agent = qw(SAAX SAAM SAAm SAA0 00FO 00TO 02FO 02TO AMUO SMUO); }
		if ($m_agentno == 16) { @m_agent = qw(SAAX SAAM SAAm SAA0 00FO 00TO 02FO 02TO AMUO SMUO 00FN 00TN 02FN 02TN AMUN SMUN); }
		if ($m_agentno == 17) { @m_agent = qw(SAAX SAAM SAAm SAA0 SAAT 00FO 00TO 02FO 02TO AMUO SMUO 00FN 00TN 02FN 02TN AMUN SMUN); }
	}
	
	for ($i=0; $i<$m_agentno; $i++) {
		$m_gameno[$i] = 0;
		$m_said[$i] = $i;
	}
	
	for ($i=0; $i<$m_agentno; $i++) {
		for($a=0; $a<28; $a++) {
			$m_bid_counter[$i][$a] = 0;
		}
	}
	
	print "% Input files are @m_filename.\n";
	print "% Output file is $m_output.\n";	
	print "% Agent number is $m_agentno.\n";
	print "% Agent names are @m_agent.\n";
	
	undef $option;
	undef $argument;
	undef $is_executed;
	undef @ARGV;
}
