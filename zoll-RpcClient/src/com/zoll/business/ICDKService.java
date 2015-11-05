package com.zoll.business;

public interface ICDKService {
	public String getTypeNameFromCdk(String cdk);
	
	public boolean typeLimitMultiUse(String type);
	
	public int useCdk(String puid, int playerid, String playername, String cdk, StringBuilder rewardRef);
}
