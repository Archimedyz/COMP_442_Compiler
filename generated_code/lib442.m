% MOON library for the COMP 442 Project
% The library contains the subroutines for the get and put functions involves for code generation.
%variables for the put and get subroutines
putv	dw	0	%variable for holding the value to display
getv	dw	0	%variable for holding the value to display
isn		dw	0	%variable for tracking whether the input was negative or not.
align % align incase this file is loaded second.
% the put() routine
put		sw		return(R0),R15		%
		lw		R15,putv(R0)        %
		clt		R15,R15,R0          %
		bnz		R15,putn            %
putgo	lw		R15,putv(R0)        %
		divi	R15,R15,1000        %
		modi	R15,R15,10          %
		addi	R15,R15,48          %
		putc 	R15                 %
		lw		R15,putv(R0)        %
		divi	R15,R15,100         %
		modi	R15,R15,10          %
		addi	R15,R15,48          %
		putc 	R15                 %
		lw		R15,putv(R0)        %
		divi	R15,R15,10          %
		modi	R15,R15,10          %
		addi	R15,R15,48          %
		putc 	R15                 %
		lw		R15,putv(R0)        %
		modi	R15,R15,10          %
		addi	R15,R15,48          %
		putc 	R15                 %
		addi 	R15,R0,10			%
		putc 	R15                 %
		addi 	R15,R0,13			%
		putc 	R15                 %
		lw		R15,return(R0)      %
		jr		R15                 %
putn	addi 	R15,R0,45 	        %
		putc 	R15                 %
		lw		R15,putv(R0)        %
		sub		R15,R0,R15          %
		sw		putv(R0),R15        %
		j		putgo               %
align % align incase this file is loaded second.
% the get() routine
get		sw		return(R0),R15		%
		addi	R15,R0,62			%
		putc	R15					%
		addi	R15,R0,32			%
		putc	R15					%
		sw		isn(R0),R0			% Reset isn, so it does not interfere with future get calls
		sub		R15,R15,R15			%
		getc	R15			        %
		sw		getv(R0),R15		%
		ceqi	R15,R15,45			%
		bnz		R15,getn			%
getgo	lw		R15,getv(R0)        %
		subi	R15,R15,48			%
		getc	R14					%
		subi	R14,R14,48			%
		muli	R15,R15,10			%
		add		R15,R15,R14			%
		getc	R14					%
		subi	R14,R14,48			%
		muli	R15,R15,10			%
		add		R15,R15,R14			%
		getc	R14					%
		subi	R14,R14,48			%
		muli	R15,R15,10			%
		add		R15,R15,R14			%
		sw		getv(R0),R15		%
		lw		R15,isn(R0)			%
		bnz		R15,getn2			%
getret	getc	R15					% one final getc, because pressing enter counts as a char.
		lw		R15,return(R0)      %
		jr		R15                 %
getn	sw 		isn(R0),R15	        %
		getc	R15					%
		sw		getv(R0),R15		%
		j		getgo               %
getn2	lw		R15,getv(R0)		%
		sub		R15,R0,R15			%
		sw		getv(R0),R15		%
		j		getret				%