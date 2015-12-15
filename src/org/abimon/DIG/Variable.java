package org.abimon.DIG;

public class Variable {
	String name;
	
	EnumType type = EnumType.STRING;
	
	public Variable(String name){
		this.name = name;
	}
	
	public void setType(EnumType type) {
		this.type = type;
	}
	
	public String toString(){
		return name + ": " + type;
	}
}
