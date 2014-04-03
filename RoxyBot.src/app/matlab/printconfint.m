function printconfint(metric,filename)

set(gca,'FontSize',16);

file = strcat(filename,'.',metric)
s = csvread(file);
s = s/1000;
m = mean(s)
[h, sig, ci] = ttest(s);
ci
ylim_max = max(max(ci)); % sjlee
ylim_min = min(min(ci)); % sjlee
l = m - ci(1,:); % low
u = ci(2,:) - m; % high
x = 1:length(m);
errorbar(x,m,l,u,'+k','LineWidth',1)

file = strcat(filename,'.agent');
fid = fopen(file);
ticks = textscan(fid, '%s');
ticks = fixnames(ticks);
set(gca,'XTick',1:length(m))
set(gca,'XTickLabel',ticks{1})
xlim([.5 (length(m)+.5)])

ylim([ylim_min-(ylim_max-ylim_min)/20, ylim_max+(ylim_max-ylim_min)/20])

set(gca,'TickLength',[.02 .02])
set(gca,'LineWidth',1)
%set(gca,'FontSize',18);

%title(strcat(filename,'.',metric));
xlabel('Agent');
ylabel(textread(strcat(metric, '.name'), '%s', 'whitespace', ''));

print('-depsc', strcat(filename, '.', metric, '.eps'))
%print('-dpng', strcat(filename, '.', metric, '.png'))
