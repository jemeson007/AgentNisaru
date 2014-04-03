#!/usr/bin/perl -w
	sub read_file;
	sub print_cdf_pdf;

	$file_name = $ARGV[0];
	@prediction = ();
	$prediction_length = 0;
	
	read_file();
	
	print "total $prediction_length predictions are collected.\n";
	
	print_cdf_pdf();
	
	undef $prediction_length;
	undef @prediction;
	undef $file_name;
	
	exit 0;

sub read_file {
	open LOG, $file_name or die;
	while ($line = <LOG>) {
		chomp($line);
		
		if($line =~ /^Priceline/) {
			$line =~ s/Priceline : //;
			@fields = split(/, /, $line);
			
			
			for($i=0; $i<8; $i++) {
				$prediction[$i][$prediction_length] = $fields[$i];
			}
			
			$prediction_length++;
	
			undef @fields;
		}
	}
	undef $line;
	close LOG;
	
	open WRITE, "> temp.out" or die;
	for($i=0; $i<$prediction_length; $i++){
		print WRITE "$prediction[0][$i],";
		print WRITE "$prediction[1][$i],";
		print WRITE "$prediction[2][$i],";
		print WRITE "$prediction[3][$i],";
		print WRITE "$prediction[4][$i],";
		print WRITE "$prediction[5][$i],";
		print WRITE "$prediction[6][$i],";
		print WRITE "$prediction[7][$i]\n";
	}
	close WRITE;
}

sub print_cdf_pdf {
	@cdf = ();
	@pdf = ();
	@another_pdf = ();
	
	for($i=0;$i<2000;$i++) {
		for($j=0;$j<8;$j++) {
			$pdf[$j][$i]=0;
		}
	}
	
	for($i=0;$i<$prediction_length;$i++) {
		for($j=0;$j<8;$j++) {
			$pdf[$j][$prediction[$j][$i]]++;
		}
	}
	
	for($j=0;$j<8;$j++) {
		$cdf[$j][0] = $pdf[$j][0]/$prediction_length;
	}
	
	for($i=1;$i<2000;$i++) {
		for($j=0;$j<8;$j++) {
			$cdf[$j][$i]=$cdf[$j][$i-1]+$pdf[$j][$i]/$prediction_length;
		}
	}

	
	for($j=0;$j<8;$j++) {
		$another_pdf[$j][0]=($cdf[$j][2]+2*$cdf[$j][0]);
		$another_pdf[$j][1]=($cdf[$j][3]+$cdf[$j][0]);
	}
	
	for($i=2;$i<1998;$i++) {
		for($j=0;$j<8;$j++) {
			$another_pdf[$j][$i]=($cdf[$j][$i+2]-$cdf[$j][$i-2]);
		}
	}
	
	open WRITE, "> cdf.out" or die;
	for ($i=0;$i<2000;$i++) {
		print WRITE "$cdf[0][$i],";
		print WRITE "$cdf[1][$i],";
		print WRITE "$cdf[2][$i],";
		print WRITE "$cdf[3][$i],";
		print WRITE "$cdf[4][$i],";
		print WRITE "$cdf[5][$i],";
		print WRITE "$cdf[6][$i],";
		print WRITE "$cdf[7][$i]\n";
	}
	close WRITE;
	
	open WRITE, "> pdf.out" or die;
	for ($i=0;$i<399;$i++) {
		print WRITE "$another_pdf[0][$i],";
		print WRITE "$another_pdf[1][$i],";
		print WRITE "$another_pdf[2][$i],";
		print WRITE "$another_pdf[3][$i],";
		print WRITE "$another_pdf[4][$i],";
		print WRITE "$another_pdf[5][$i],";
		print WRITE "$another_pdf[6][$i],";
		print WRITE "$another_pdf[7][$i]\n";
	}
	close WRITE;

	undef @another_pdf;
	undef @pdf;
	undef @cdf;
}
