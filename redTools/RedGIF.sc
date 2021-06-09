//redFrik

//related: RedBMP

RedGIF {
	var	<>type,  //string

	<>width, <>height,  //integers
	<>resolution,  //integer
	<>sorted,  //integer
	<>background,  //color
	<>aspectRatio,  //integer
	<>depth,  //integer
	<>globalColorMap,  //array of colors

	<>controls,  //array of RedGIFControl
	<>comments,  //array of strings

	<>appId, <>appCode,  //strings
	<>appData,  //array

	<>images,  //array of images

	//used for lzw decompression
	dict,  //array of arrays
	val,  //array
	old;  //integer

	*read {|path, action|
		^super.new.read(path, action);
	}
	read {|path, action|
		path= path.standardizePath;
		if(File.exists(path), {
			this.prRead(path);
			{action.value(this)}.fork;
		}, {"%: file % not found".format(this.class.name, path).error});
	}

	makeWindow {|bounds, rate= 1.0, interpolation= \fast|
		var b= bounds ?? {Rect(300, 300, width, height)};
		var win= Window(this.class.name, b);
		var img= Image(width, height).interpolation_(interpolation);
		var index= 0, image, row;
		win.drawFunc= {
			var i, j, x, y, size;
			var left, right, top, bottom;
			var pass1, pass2, pass3, pass4;
			image= images.wrapAt(index);
			if(background.notNil and:{index%images.size==0}, {
				img.fill(background);
			});
			if(image.interlaced, {
				pass1= (height+7).div(8);
				pass2= (height+3).div(8);
				pass3= (height+1).div(4);
				pass4= height.div(2);
			});
			i= 0;
			j= 0;
			left= image.bounds.left;
			right= image.bounds.width+left;
			top= image.bounds.top;
			bottom= image.bounds.height+top;
			size= width*height;
			while({i<size}, {
				x= i.mod(width);
				y= i.div(width);
				if(image.interlaced, {
					case
					{y<pass1} {y= y*8}
					{y<(pass1+pass2)} {y= y-pass1*8+4}
					{y<(pass1+pass2+pass3)} {y= y-(pass1+pass2)*4+2}
					{y= y-(pass1+pass2+pass3)*2+1}
					;
				});
				if(x>=left and:{x<right and:{y>=top and:{y<bottom}}}, {
					if(image.control.transparentFlag.not or:{
						image.indices[j]!=image.control.transparent
					}, {
						img.setColor(image.data[j], x, y);
					});
					j= j+1;
				});
				i= i+1;
			});
			Pen.drawImage(Rect(0, 0, win.bounds.width, win.bounds.height), img);
			switch(image.control.disposalMethod,
				1, nil,  //leave image for overwrite
				2, {img.fill(background)},  //restore background
				3, {"TODO disposalMethod==3 restore previous".postln}
			);
		};
		if(images.size>1, {  //if animated gif
			Routine({
				while({win.isClosed.not}, {
					//TODO check userinputflag here
					(images.wrapAt(index).control.duration*0.01/rate).max(0.01).wait;
					index= index+1;
					win.refresh;
				});
			}).play(AppClock);
		});
		win.onClose= {img.free};
		win.front;
		^win;
	}

	//--private
	prRead {|path|
		var file= File(path, "rb");
		var separator;
		this.prReadHeader(file);
		if(type=="GIF87a" or:{type=="GIF89a"}, {
			this.prReadLogicalScreenDescriptor(file);
			images= [];
			"reading ".post;
			while({separator= file.getInt8.bitAnd(0xff); separator!=0x3b}, {
				switch(separator,
					0x21, {this.prReadExtensionBlock(file)},
					0x2c, {this.prReadImageDescriptor(file); ".".post},
					0x00, {},
					{"%: unknown separator %".format(this.class.name, separator).warn}
				);
			});
			file.close;
			"\ndecoding".post;
			images.do{|image, i|  //copy over control objects to image objects
				image.data= this.prDecode(image.data, image.codeSize);
				image.indices= Array(image.data.size);
				image.data= image.data.collect{|x|
					image.indices.add(x);
					image.colorMap[x];
				};
				".".post;
				if(i<controls.size, {  //set corresponding control object
					image.control= controls[i];
				}, {
					if(controls.size>0, {  //use last found control object
						image.control= controls.last;
					}, {  //use default control
						image.control= RedGIFControl.new;
						image.control.duration= 10;
						image.control.transparent= Color.clear;
						image.control.disposalMethod= 0;
						image.control.userInputFlag= false;
						image.control.transparentFlag= true;
					});
				});
			};
		}, {
			"%: type % not recognized".format(this.class.name, type).error;
			file.close;
		});
	}

	prReadHeader {|file|  //header with signature and version (6bytes)
		type= {file.getChar}.dup(6).join;
	}

	prReadLogicalScreenDescriptor {|file|  //logical screen descriptor (7bytes)
		var flags;
		width= file.getInt16LE;  //logical screen width
		height= file.getInt16LE;  //logical screen height
		flags= file.getInt8.bitAnd(0xff);
		background= file.getInt8.bitAnd(0xff);  //background color index
		aspectRatio= file.getInt8.bitAnd(0xff);  //pixel aspect ratio
		if(aspectRatio!=0, {
			aspectRatio= aspectRatio+15/64;
		});
		depth= 1.leftShift(flags.bitAnd(0x07)+1);  //size of global color table
		sorted= flags.rightShift(3).bitAnd(0x01);
		resolution= flags.rightShift(4).bitAnd(0x07)+1;  //number of bits per color
		if(flags.bitTest(7), {  //global color map (3bytes*numColors)
			globalColorMap= {
				Color(*file.read(Int8Array[0, 0, 0]).bitAnd(0xff)/255);
			}.dup(depth);
			background= globalColorMap[background];
		}, {
			globalColorMap= [];
			background= nil;
		});
	}

	prReadExtensionBlock {|file|  //extension block (?bytes)
		var flags, blockSize, ctrl;
		switch(file.getInt8.bitAnd(0xff),
			0x01, {
				"%: plain text not yet supported".format(this.class.name).warn;
				blockSize= file.getInt8.bitAnd(0xff);
				if(blockSize!=12, {"blockSize in plaintext not 12".warn});
				("\ttextgrid left pos:"+file.getInt16.bitAnd(0xff)).postln;
				("\ttextgrid top pos :"+file.getInt16.bitAnd(0xff)).postln;
				("\ttextgrid width   :"+file.getInt16.bitAnd(0xff)).postln;
				("\ttextgrid height  :"+file.getInt16.bitAnd(0xff)).postln;
				("\tchar cell width  :"+file.getInt8.bitAnd(0xff)).postln;
				("\tchar cell height :"+file.getInt8.bitAnd(0xff)).postln;
				("\tforecolor index  :"+file.getInt8.bitAnd(0xff)).postln;
				("\tbackcolor index  :"+file.getInt8.bitAnd(0xff)).postln;
				while({blockSize= file.getInt8.bitAnd(0xff); blockSize!=0}, {
					"plaintext block...".postln;
					{file.getChar}.dup(blockSize).join.postln;
				});
			},
			0xf9, {  //graphic control extension block
				ctrl= RedGIFControl.new;
				blockSize= file.getInt8.bitAnd(0xff);
				flags= file.getInt8.bitAnd(0xff);  //packed field
				ctrl.duration= file.getInt16LE;  //delay time (1/100ths of a second)
				ctrl.transparent= file.getInt8.bitAnd(0xff);
				ctrl.disposalMethod= flags.rightShift(2).bitAnd(0x07);
				ctrl.userInputFlag= flags.bitTest(1);  //wait for user input flag
				ctrl.transparentFlag= flags.bitTest(0);  //transparency flag
				controls= controls.add(ctrl);
			},
			0xfe, {  //comment extension block
				while({blockSize= file.getInt8.bitAnd(0xff); blockSize!=0}, {
					comments= comments.add({file.getChar}.dup(blockSize).join);
				});
			},
			0xff, {  //application extension block
				blockSize= file.getInt8.bitAnd(0xff);
				appId= {file.getChar}.dup(8).join;
				appCode= {file.getChar}.dup(3).join;
				while({blockSize= file.getInt8.bitAnd(0xff); blockSize!=0}, {
					appData= file.read(Int8Array.newClear(blockSize)).bitAnd(0xff);
				});
			},
			{"%: extensionblock - code not recognised".format(this.class.name).warn}
		);
	}

	prReadImageDescriptor {|file|  //local descriptor (10bytes)
		var flags, bounds, image;
		bounds= {file.getInt16LE}.dup(4).asRect;
		flags= file.getInt8.bitAnd(0xff);
		image= RedGIFImage(bounds, flags);

		if(image.hasColorMap, {
			image.colorMap= {
				Color(*file.read(Int8Array[0, 0, 0]).bitAnd(0xff)/255);
			}.dup(image.depth);
		}, {
			image.colorMap= globalColorMap;
		});

		//--read table based image data
		image.codeSize= file.getInt8.bitAnd(0xff);
		image.data= this.prReadData(file);
		images= images.add(image);
	}

	prReadData {|file|
		var blockSize, data= List.new;
		while({blockSize= file.getInt8.bitAnd(0xff); blockSize!=0}, {
			data.addAll(file.read(Int8Array.newClear(blockSize)).bitAnd(0xff));
		});
		^data;
	}

	//--lzw decompression
	prDecode {|arr, codeSize|
		var extractCode= {
			var i= 0;
			{|size|
				var sum= 0, j= 0;
				while({j<size}, {
					sum= sum+(arr[i.div(8)].rightShift(i.mod(8)).bitAnd(1)*1.leftShift(j));
					i= i+1;
					j= j+1;
				});
				[i, sum];
			};
		}.value;
		var clearCode= 1.leftShift(codeSize);
		var endCode= clearCode+1;
		var initArr= {|i| [i]}.dup(endCode+1);
		var code, tempCodeSize= codeSize+1;
		var c, out= List[];
		var more= true, index, maxIndex= arr.size-1*8;
		while({more}, {
			#index, code= extractCode.value(tempCodeSize);
			if(code==clearCode, {
				dict= Array(4096).overWrite(initArr);
				tempCodeSize= codeSize+1;
				#index, code= extractCode.value(tempCodeSize);
				out.add(code);
				val= [code];
			}, {
				if(code==endCode or:{index>maxIndex}, {
					more= false;
				}, {
					c= dict[code];
					if(c.notNil, {
						dict.add(val++c[0]);
					}, {
						c= dict[old]++val[0];
						dict.add(c);
					});
					val= c.asCollection;
					out.addAll(c);
					if(tempCodeSize<12 and:{dict.size==1.leftShift(tempCodeSize)}, {
						tempCodeSize= tempCodeSize+1;
					});
				});
			});
			old= code;
		});
		^out.array;
	}
}

RedGIFImage {
	var <bounds, <flags, <>colorMap, <>data, <>control, <>indices, <>codeSize;
	*new {|bounds, flags| ^super.newCopyArgs(bounds, flags)}
	hasColorMap {^flags.bitTest(7)}
	interlaced {^flags.bitTest(6)}
	sorted {^flags.bitTest(5)}
	depth {^1.leftShift(flags.bitAnd(0x07)+1)}  //size of local color table
}

RedGIFControl {
	var	<>duration,  //integer
	<>transparent,  //integer
	<>disposalMethod,  //integer
	<>userInputFlag,  //boolean
	<>transparentFlag;  //boolean
}
