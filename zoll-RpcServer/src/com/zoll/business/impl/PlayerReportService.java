package com.zoll.business.impl;

import com.zoll.business.IReportService;

public class PlayerReportService implements IReportService {

	@Override
	public void report(RegisterData registerData) {
		System.out.println(registerData.getPuid());
		System.out.println(registerData.getDevice());
		System.out.println(registerData.getPlayerId());
		System.out.println(registerData.getTime());
		registerData.setPuid("SUCCESS!");
		registerData.setDevice("SUCCESS!");
		registerData.setPlayerId(1000000);
		registerData.setTime("SUCCESS!");
	}

	@Override
	public void report(LoginData loginData) {
		// TODO Auto-generated method stub

	}

	@Override
	public void report(GoldData goldData) {
		// TODO Auto-generated method stub

	}

	@Override
	public void report(ServerData serverData) {
		// TODO Auto-generated method stub

	}

}
