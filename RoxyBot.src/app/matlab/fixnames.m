%2000f --> TargetMU
%2000t --> TargetMU*
%smu   --> StraightMU
%amu   --> AverageMU
%2002f --> BidEvaluator
%2002t --> BidEvaluator*
%saaf  --> SAA
%saat  --> SAA* 
%SAA, SAA*
%Eval, Eval*
%TMU, TMU*
%AMU, SMU

function [niceticks] = fixnames(ticks)
for i=1:length(ticks{1})
	if strcmp(ticks{1}{i}, '2000-f')
		ticks{1}{i} = 'TM';
	end
	if strcmp(ticks{1}{i},'2000-t')
		ticks{1}{i} = 'TM*';
	end
	if strcmp(ticks{1}{i} , 'smu')
		ticks{1}{i} = 'SM';
	end
	if strcmp(ticks{1}{i} , 'amu')
		ticks{1}{i} = 'AM';
	end
	if strcmp(ticks{1}{i} , '2002-f')
		ticks{1}{i} = 'BE';
	end
	if strcmp(ticks{1}{i} , '2002-t')
		ticks{1}{i} = 'BE*';
	end
	if strcmp(ticks{1}{i} , 'saa-f')
		ticks{1}{i} = 'SA';
	end
	if strcmp(ticks{1}{i} , 'saa-t')
		ticks{1}{i} = 'SA*';
	end
	ticks{1}{1};
	niceticks=ticks;
end