package org.abimon.DIG;

public enum EnumType {
	STRING,
	INT,
	ARRAY,
	DICT,
	
	;

	public static boolean hasValue(String upperCase) {
		try{
			EnumType.valueOf(upperCase);
			return true;
		}catch(Throwable th){}
		
		return false;
	}
}
