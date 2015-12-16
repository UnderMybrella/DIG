package org.abimon.DIG;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Scanner;

import javax.imageio.ImageIO;

import static org.abimon.DIG.EnumProcessing.*;

/** /Users/Undermybrella/DIG Programs/ */
public class DIG {

	public static boolean debug = false;
	public static String[] variableNames = new String[10];
	public static String tmpVarName = "";

	public static void main(String[] args){
		try{
			if(args.length >= 2)
				debug = Boolean.parseBoolean(args[1]);

			String programLoc = "";
			if(args.length == 0){
				System.out.print("Enter a DIG program to interpret: ");
				Scanner in = new Scanner(System.in);
				programLoc = in.nextLine();
				in.close();
			}
			else
				programLoc = args[0];

			if(!programLoc.endsWith(".png") && !programLoc.endsWith(".txt"))
				programLoc += ".png";
			File program = new File(programLoc);
			if(!program.exists()) 
				program = new File(System.getProperty("user.home") + File.separator + "DIG Programs" + File.separator + programLoc);

			if(program.exists())
				runProgram(program);
			else
				System.err.println("DIG program does not exist!");
		}
		catch(Throwable th){
			if(debug)
				th.printStackTrace();
		}
	}

	private static boolean acceptingAsciiInput(Color color) {
		return color != null && color.getRed() == 0 && color.getGreen() == 1;
	}


	static boolean isOpen = true;

	static File tmp = null;

	public static void runProgram(File program) throws Throwable{
		HashMap<String, Variable> variableList = new HashMap<String, Variable>();
		BufferedImage programData = ImageIO.read(transform(program));
		PrintStream out = System.out;
		int language = -1; //0 is Java, 1 is Python
		String line = "";
		EnumProcessing currentProcess = NONE;
		EnumProcessing previousProcess = NONE;

		Variable currentVariable = null;
		String tmpVariableData = "";
		int indentation = 0;
		for(int x = 0; x < programData.getWidth(); x++)
			for(int y = 0; y < programData.getHeight(); y++)
			{
				Color color = new Color(programData.getRGB(x, y));
				previousProcess = currentProcess;
				if(color.getRed() == 0 && color.getGreen() == 1); //Text Entry
				else if(((color.getRed() == 0 && color.getGreen() == 0 && color.getBlue() < 2) || (color.getRed() == 255 && color.getGreen() == 254)) && language == -1)
				{
					language = color.getBlue();
					switch(language){
					case 0:
						tmp = new File(program.getAbsolutePath().substring(0, program.getAbsolutePath().length() - 4) + ".java");
						if(tmp.exists())
							tmp.delete();
						tmp.createNewFile();
						out = new PrintStream(tmp);
						break;
					case 1:
						tmp = new File(program.getAbsolutePath().substring(0, program.getAbsolutePath().length() - 4) + ".py");
						if(tmp.exists())
							tmp.delete();
						tmp.createNewFile();
						out = new PrintStream(tmp);
					}
				}
				else if(color.getRed() == 0){
					if(color.getGreen() == 0){
						if(color.getBlue() == 2)
							currentProcess = NEWLINE;
						else if(color.getBlue() == 3)
							currentProcess = PRINT;
						else if(color.getBlue() == 4)
							currentProcess = SET_VARIABLE_NAME;
						else if(color.getBlue() == 5)
							currentProcess = SET_VARIABLE_TYPE;
						else if(color.getBlue() == 6)
							currentProcess = SET_VARIABLE_VALUE;
						else if(color.getBlue() == 7)
							currentProcess = GET_VARIABLE;
						else if(color.getBlue() == 8)
							currentProcess = INPUT;
						else if(color.getBlue() == 9)
							currentProcess = START_WHILE;
						else if(color.getBlue() == 10){
							indentation--;
							currentProcess = END_INDENT;
						}
						else if(color.getBlue() == 11)
							currentProcess = BREAK;
						else if(color.getBlue() == 12)
							currentProcess = IF;
						else if(color.getBlue() == 13)
							currentProcess = ELSE;
						else if(color.getBlue() == 14)
							currentProcess = ELSEIF;
						else if(color.getBlue() == 15)
							currentProcess = FOR;
					}
				}
				else if(color.getRed() == 2 && color.getGreen() == 0 && color.getBlue() == 0)
					currentProcess = STRING_LOWER;
				else if(color.getRed() == 2 && color.getGreen() == 0 && color.getBlue() == 2)
					currentProcess = STRING_TRIM;
				else if(color.getRed() == 2 && color.getGreen() == 0 && color.getBlue() == 3)
					currentProcess = STRING_SPLIT;
				else if(color.getRed() == 2 && color.getGreen() == 0 && color.getBlue() == 255)
					currentProcess = STRING_CAST;
				else if(color.getRed() == 2 && color.getGreen() == 1 && color.getBlue() == 1)
					currentProcess = INT_SUBTRACT;
				else if(color.getRed() == 2 && color.getGreen() == 1 && color.getBlue() == 3)
					currentProcess = INT_MULTIPLY;
				else if(color.getRed() == 2 && color.getGreen() == 1 && color.getBlue() == 255)
					currentProcess = INT_CAST;
				else if(color.getRed() == 2 && color.getGreen() == 2 && color.getBlue() == 0)
					currentProcess = ARRAY_ACCESS;
				else if(color.getRed() == 255 && color.getGreen() == 255 && color.getBlue() == 0)
					currentProcess = EMPTY;
				else if(color.getRed() == 255 && color.getGreen() == 255 && color.getBlue() == 1)
					currentProcess = END_CAST;
				else if(color.getRed() == 255 && color.getGreen() == 255 && (color.getBlue() >= 220 && color.getBlue() < 230) && (currentVariable != null || currentProcess == SET_VARIABLE_NAME)){
					tmpVarName = currentVariable.name;
					currentVariable.name = variableNames[color.getBlue()-220];
					if(debug)
						System.out.println(Arrays.toString(variableNames));
					currentProcess = GET_VARIABLE;
				}
				else if(color.getRed() == 255 && color.getGreen() == 255 && (color.getBlue() >= 230 && color.getBlue() < 240) && (currentVariable != null || currentProcess == SET_VARIABLE_NAME))
					currentProcess = SET_MEMORY_VAR;
				else if(color.getRed() == 255 && color.getGreen() == 255 && (color.getBlue() >= 240 && color.getBlue() < 250) && (currentVariable != null || currentProcess == SET_VARIABLE_NAME)) //Get Variable i
					currentProcess = GET_MEMORY_VAR;
				else if(color.getRed() == 255 && color.getGreen() == 255 && color.getBlue() == 254)
					currentProcess = PRINT_ACCESS;
				else if(color.getRed() == 255 && color.getGreen() == 255 && color.getBlue() == 255)
					currentProcess = FREEFORM;
				if(language == 0){
					if(currentProcess == FREEFORM && acceptingAsciiInput(color))
						out.print((char) color.getBlue());
				}
				if(language == 1)
				{
					if(previousProcess != currentProcess)
					{
						if(debug)
							System.out.println("Switched from " + previousProcess + " to " + currentProcess);
						switch(previousProcess){
						case PRINT:
							if(currentProcess == PRINT_ACCESS)
								break;
							if(currentProcess != GET_VARIABLE && currentProcess != SET_VARIABLE_NAME && currentProcess != GET_MEMORY_VAR)
								line += "\")";
							else{
								line += "\" + ";
								if(currentVariable != null)
									line += "str(";
							}
							break;
						case SET_VARIABLE_NAME:
							currentVariable = new Variable(tmpVariableData);
							if(!variableList.containsKey(currentVariable.name))
								variableList.put(currentVariable.name, currentVariable);
							tmpVariableData = "";
							break;
						case SET_VARIABLE_TYPE:
							if(currentVariable != null && EnumType.hasValue(tmpVariableData.toUpperCase())){
								currentVariable.setType(EnumType.valueOf(tmpVariableData.toUpperCase()));
								variableList.get(currentVariable.name).setType(currentVariable.type);
							}
							tmpVariableData = "";
							break;
						case SET_VARIABLE_VALUE:
							if(currentVariable != null && variableList.get(currentVariable.name).type == EnumType.STRING){
								line += "\"";
								if(currentProcess == INPUT || currentProcess == GET_VARIABLE || currentProcess == SET_VARIABLE_NAME || currentProcess == GET_MEMORY_VAR)
									line += " + ";
							}
							else if(currentVariable != null && variableList.get(currentVariable.name).type == EnumType.INT)
								if(currentProcess == INPUT || currentProcess == GET_VARIABLE || currentProcess == SET_VARIABLE_NAME || currentProcess == GET_MEMORY_VAR);
								else
									line += ")";
							break;
						case GET_VARIABLE:
							if(currentProcess == PRINT || currentProcess == INPUT){// || currentProcess == SET_VARIABLE_VALUE){
								//if(currentVariable != null && variableList.get(currentVariable.name).type == EnumType.INT)
								line += ")";
								line += " + \"";
							}
							//							else if(variableList.get(currentVariable.name).type == EnumType.INT)
							//								line += ")";
							if(!tmpVarName.equals("")){
								currentVariable.name = tmpVarName;
								tmpVarName = "";
							}

							//							else if(currentVariable != null && variableList.get(currentVariable.name).type == EnumType.INT)
							//								line += ")";
							break;
						case INPUT:
							if(currentProcess != GET_VARIABLE && currentProcess != GET_MEMORY_VAR)
								line += "\")";
							else
								line += "\" + ";

							if(currentVariable != null && variableList.get(currentVariable.name).type == EnumType.INT)
								line += ")";
							break;
						case START_WHILE:
							if(color.getRed() != 2 && currentProcess != GET_VARIABLE && currentProcess != GET_MEMORY_VAR){
								line += ":";
								indentation++;
							}
							break;
						case IF:
							if(color.getRed() != 2 && currentProcess != GET_VARIABLE && currentProcess != GET_MEMORY_VAR){
								line += ":";
								indentation++;
							}
							break;
						case ELSE:
							if(color.getRed() != 2 && currentProcess != GET_VARIABLE && currentProcess != GET_MEMORY_VAR){
								line += ":";
								indentation++;
							}
							break;
						case ELSEIF:
							if(color.getRed() != 2 && currentProcess != GET_VARIABLE && currentProcess != GET_MEMORY_VAR){
								line += ":";
								indentation++;
							}
							break;
						case FOR:
							if(currentProcess == NEWLINE){
								line += ":";
								indentation++;
							}
							else
								line += " in ";
							break;
						case STRING_SPLIT:
							line += ")";
							break;
						case ARRAY_ACCESS:
							line += "]";
							break;
						case PRINT_ACCESS:
							line += " + \"";
							break;

						default:
							break;
						}
					}

					String indent = "";
					for(int i = 0; i < indentation; i++)
						indent += "\t";

					switch(currentProcess){
					case NONE:
						break;
					case NEWLINE:
						out.println(line);
						line = indent;
						break;
					case PRINT:
						if(line.contains("print") && acceptingAsciiInput(color))
							line += (char) color.getBlue();
						else if(!line.contains("print"))
							line += "print(\"";
						break;
					case FREEFORM:
						if(acceptingAsciiInput(color))
							line += (char) color.getBlue();
						break;
					case SET_VARIABLE_NAME:
						if(acceptingAsciiInput(color))
							tmpVariableData += (char) color.getBlue();
						break;
					case SET_VARIABLE_TYPE:
						if(acceptingAsciiInput(color))
							tmpVariableData += (char) color.getBlue();
						else if(color.getRed() == 1 && color.getGreen() == 1 && color.getBlue() == 0)
							tmpVariableData += "STRING";
						else if(color.getRed() == 1 && color.getGreen() == 1 && color.getBlue() == 1)
							tmpVariableData += "INT";
						else if(color.getRed() == 1 && color.getGreen() == 1 && color.getBlue() == 2)
							tmpVariableData += "ARRAY";
						break;
					case SET_VARIABLE_VALUE:
						if(currentVariable != null)
							if(line.trim().matches(".* = .*") && acceptingAsciiInput(color))
								line += (char) color.getBlue();
							else if(!line.trim().matches(".* = .*"))
								line += currentVariable.name + " = " + (variableList.get(currentVariable.name).type == EnumType.STRING ? "\"" : variableList.get(currentVariable.name).type == EnumType.INT ? "int(" : "");
						break;
					case GET_VARIABLE:
						if(acceptingAsciiInput(color))
							line += (char) color.getBlue();
						else if(currentVariable != null){
							//							if(variableList.get(currentVariable.name).type == EnumType.INT)
							//								line += "int(";

							line += currentVariable.name;
						}
						break;
					case INPUT:
						if(line.contains("input") && acceptingAsciiInput(color))
							line += (char) color.getBlue();
						else if(!line.contains("input"))
							line += "input(\"";
						break;
					case START_WHILE:
						if(line.startsWith("while") && acceptingAsciiInput(color)){
							line += (char) color.getBlue();
						}
						else if(!line.contains("while"))
							line += "while ";
						else if(color.getRed() == 1 && color.getGreen() == 0)
							line += logicalConditions(color);
						break;
					case BREAK:
						line += "break";
						break;
					case IF:
						if(line.contains("if") && acceptingAsciiInput(color)){
							line += (char) color.getBlue();
						}
						else if(!line.contains("if"))
							line += "if ";
						else if(color.getRed() == 1 && color.getGreen() == 0)
							line += logicalConditions(color);
						break;
					case ELSEIF:
						if(line.contains("elif") && acceptingAsciiInput(color)){
							line += (char) color.getBlue();
						}
						else if(!line.contains("elif"))
							line += "elif ";
						else if(color.getRed() == 1 && color.getGreen() == 0)
							line += logicalConditions(color);
						break;
					case ELSE:
						if(!line.contains("else"))
							line += "else";
						break;
					case FOR:
						if(line.contains("for") && acceptingAsciiInput(color))
							line += (char) color.getBlue();
						else if(!line.contains("for"))
							line += "for ";
						break;
					case STRING_LOWER:
						line += ".lower()";
						break;
					case STRING_TRIM:
						line += ".strip()";
						break;
					case STRING_SPLIT:
						if(acceptingAsciiInput(color))
							line += (char) color.getBlue();
						else
							line += ".split(";
						break;
					case STRING_CAST:
						if(previousProcess != GET_VARIABLE)
							line += "str(";
						break;
					case INT_SUBTRACT:
						line += " - ";
						break;
					case INT_MULTIPLY:
						if(acceptingAsciiInput(color))
							line += (char) color.getBlue();
						else
							line += " * ";
						break;
					case END_CAST:
						line += ")";
						break;
					case ARRAY_ACCESS:
						if(acceptingAsciiInput(color))
							line += (char) color.getBlue();
						else
							line += "[";
						break;
					case SET_MEMORY_VAR:
						variableNames[color.getBlue()-230] = currentVariable.name;
						if(debug)
							System.out.println(Arrays.toString(variableNames));
						break;
					case GET_MEMORY_VAR:
						currentVariable.name = variableNames[color.getBlue()-240];
						if(debug)
							System.out.println(Arrays.toString(variableNames));
						break;
					case PRINT_ACCESS:
						if(acceptingAsciiInput(color))
							line += (char) color.getBlue();
						else
							line += "\" + ";
						break;
					default:
						break;
					}
				}
			}
		out.close();

		Runtime.getRuntime().addShutdownHook(new Thread(){
			public void run(){
				if(!debug)
					tmp.delete();
			}
		});

		if(tmp != null)
		{
			System.out.println("Switching to program output...");
			System.out.println("******************************");
			ProcessBuilder process = null;
			if(language == 0)
				process = new ProcessBuilder("javac", tmp.getAbsolutePath());
			if(language == 1)
				process = new ProcessBuilder("python", tmp.getAbsolutePath());
			process.redirectErrorStream(true);	
			Process proc = process.start();
			InputStream pin = proc.getInputStream();
			PrintStream pout = new PrintStream(proc.getOutputStream());
			isOpen = true;
			new Thread(){
				public void run(){
					try{
						byte[] pine = new byte[8192];
						while(true)
						{
							int read = pin.read(pine);
							if(read <= 0)
								break;
							System.out.println(new String(pine).trim());
							pine = new byte[8192];
						}
					}
					catch(Throwable th){}
					try{
						pin.close();
					}
					catch(Throwable th){};
					System.out.println("******************************");
					isOpen = false;
				}
			}.start();

			new Thread(){
				public void run(){
					while(isOpen){
						try{
							Thread.sleep(100);
						}catch(Throwable th){}
					}
					System.exit(0);
				}
			}.start();

			Scanner in = new Scanner(System.in);
			while(isOpen){
				pout.println(in.nextLine());
				pout.flush();
			}
			in.close();
		}
	}

	public static String logicalConditions(Color color){
		if(color.getRed() == 1 && color.getGreen() == 0){
			if(color.getBlue() == 0)
				return "True";
			else if(color.getBlue() == 1)
				return "False";
			else if(color.getBlue() == 2)
				return " and ";
			else if(color.getBlue() == 3)
				return " or ";
			else if(color.getBlue() == 4)
				return " == ";
			else if(color.getBlue() == 5)
				return " < "; //Less Than
			else if(color.getBlue() == 6)
				return " <= "; //Less Than or Equal To
			else if(color.getBlue() == 7)
				return " > "; //Greater Than
			else if(color.getBlue() == 8)
				return " >= "; //Greater Than or Equal To
			else if(color.getBlue() == 9)
				return " !"; //Not
			else if(color.getBlue() == 10)
				return " != ";//Not Equal
			else if(color.getBlue() == 11)
				return  "(";
			else if(color.getBlue() == 12)
				return ") ";
			
		}
		return "";
	}

	public static File transform(File fileLoc){
		if(fileLoc.getName().endsWith(".png"))
			return fileLoc;
		try{
			File f = fileLoc;

			BufferedReader in = new BufferedReader(new FileReader(f));

			String line = in.readLine();

			BufferedImage img = new BufferedImage(Integer.parseInt(line.split(",")[0].trim()), Integer.parseInt(line.split(",")[1].trim()), BufferedImage.TYPE_INT_ARGB);

			LinkedList<String> pixels = new LinkedList<String>();

			while((line = in.readLine()) != null){
				if(line.split(",").length > 2)
					pixels.add(line);
			}

			for(int x = 0; x < img.getWidth(); x++)
				for(int y = 0; y < img.getHeight(); y++){
					String pixel = pixels.poll();
					if(debug)
						System.out.println(pixel);
					if(pixel != null)
						img.setRGB(x, y, new Color(Integer.parseInt(pixel.split(",")[0].trim()), Integer.parseInt(pixel.split(",")[1].trim()), Integer.parseInt(pixel.split(",")[2].trim())).getRGB());
					else
						img.setRGB(x, y, new Color(255, 255, 0).getRGB());
				}
			in.close();
			File imgLoc = new File(f.getAbsolutePath().replace(".txt", ".png"));
			if(debug)
				System.out.println("Writing to " + imgLoc);
			ImageIO.write(img, "PNG", imgLoc);
			return imgLoc;
		}
		catch(Throwable th){
			if(debug)
				th.printStackTrace();
		}
		return null;
	}
}
