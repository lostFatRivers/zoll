package junitTest;

import com.zoll.business.IReportService;
import com.zoll.business.IReportService.RegisterData;
import com.zoll.client.RpcClientBus;
import com.zoll.client.proxy.CallRemoteHandler;
import com.zoll.client.proxy.ProxyInterfaceManager;

public class ClientMain {
	public static void main(String[] args) throws InterruptedException {
		RpcClientBus.getInstance().initClient();
		
//		IReportService report = new PlayerReportService();
		
		ProxyInterfaceManager.getInstance().initProxy(new CallRemoteHandler());
		
		IReportService report2 = ProxyInterfaceManager.getInstance().getProxy(IReportService.class);
		RegisterData registerData = new RegisterData("sssf_vdsa", "33f3szf4g46sd", 11232, "234.55.213");
		report2.report(registerData);
		System.out.println(registerData);
	}
}
