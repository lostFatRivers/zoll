package com.zoll.business;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.zoll.anno.MessagePT;
import com.zoll.anno.RPCStub;
import com.zoll.protocol.ParamType;

/**
 * 数据上报接口;
 * 
 * @author qianhang
 * 
 * @date 2015年8月24日 下午5:34:44
 * 
 * @project zoll-RpcClient
 * 
 */
@RPCStub
public interface IReportService {

	@MessagePT(paramType = ParamType.REGISTER_DATA)
	public void report(RegisterData registerData);
	
	@MessagePT(paramType = ParamType.LOGIN_DATA)
	public void report(LoginData loginData);

	@MessagePT(paramType = ParamType.GOLD_DATA)
	public void report(GoldData goldData);

	@MessagePT(paramType = ParamType.SERVER_DATA)
	public void report(ServerData serverData);

	/**
	 * 上报注册数据
	 * 
	 * @author hawk
	 */
	public static class RegisterData {
		public String puid;
		public String device;
		public int playerId;
		public String time;

		public RegisterData() {
		}

		public RegisterData(String puid, String device, int playerId, String time) {
			this.puid = puid;
			this.device = device;
			this.playerId = playerId;
			this.time = time;
		}

		public String getPuid() {
			return puid;
		}

		public void setPuid(String puid) {
			this.puid = puid;
		}

		public String getDevice() {
			return device;
		}

		public void setDevice(String device) {
			this.device = device;
		}

		public int getPlayerId() {
			return playerId;
		}

		public void setPlayerId(int playerId) {
			this.playerId = playerId;
		}

		public String getTime() {
			return time;
		}

		public void setTime(String time) {
			this.time = time;
		}

		@Override
		public String toString() {
			return "RegisterData [puid=" + puid + ", device=" + device + ", playerId=" + playerId + ", time=" + time + "]";
		}
		
	}

	/**
	 * 上报登陆数据
	 * 
	 * @author hawk
	 */
	public static class LoginData {
		public String puid;
		public String device;
		public int playerId;
		public int period;
		public String time;

		public LoginData() {
		}

		public LoginData(String puid, String device, int playerId, int period, String time) {
			this.puid = puid;
			this.device = device;
			this.playerId = playerId;
			this.period = period;
			this.time = time;
		}
	}

	/**
	 * 上报充值数据
	 * 
	 * @author hawk
	 */
	public static class RechargeData {
		public String puid;
		public String device;
		public int playerId;
		public String playerName;
		public int playerLevel;
		public String orderId;
		public String productId;
		public int payMoney;
		public String currency;
		public String time;

		public RechargeData() {
			this.currency = "RMB";
			this.productId = "0";
			this.time = new Date().toString();
		}

		public RechargeData(String puid, String device, int playerId, String playerName, int playerLevel, String orderId, String productId, int payMoney, String currency, String time) {
			this.puid = puid;
			this.device = device;
			this.playerId = playerId;
			this.playerName = playerName;
			this.playerLevel = playerLevel;
			this.orderId = orderId;
			this.productId = productId;
			this.payMoney = payMoney;
			this.currency = currency;
			this.time = time;
		}

		public void setPuid(String puid) {
			this.puid = puid;
		}

		public void setDevice(String device) {
			this.device = device;
		}

		public void setPlayerId(int playerId) {
			this.playerId = playerId;
		}

		public void setPlayerName(String playerName) {
			this.playerName = playerName;
		}

		public void setPlayerLevel(int playerLevel) {
			this.playerLevel = playerLevel;
		}

		public void setOrderId(String orderId) {
			this.orderId = orderId;
		}

		public void setProductId(String productId) {
			this.productId = productId;
		}

		public void setPayMoney(int payMoney) {
			this.payMoney = payMoney;
		}

		public void setCurrency(String currency) {
			this.currency = currency;
		}

		public void setTime(String time) {
			this.time = time;
		}
	}

	/**
	 * 上报钻石数据
	 * 
	 * @author hawk
	 */
	public static class GoldData {
		public String puid;
		public String device;
		public int playerId;
		public int playerLevel;
		public int changeType;
		public String changeAction;
		public int goldType;
		public int gold;
		public String time;

		public GoldData() {
			this.time = new Date().toString();
		}

		public GoldData(String puid, String device, int playerId, int playerLevel, int changeType, String changeAction, int goldType, int gold, String time) {
			this.puid = puid;
			this.device = device;
			this.playerId = playerId;
			this.playerLevel = playerLevel;
			this.changeType = changeType;
			this.changeAction = changeAction;
			this.goldType = goldType;
			this.gold = gold;
			this.time = time;
		}

		public void setPuid(String puid) {
			this.puid = puid;
		}

		public void setDevice(String device) {
			this.device = device;
		}

		public void setPlayerId(int playerId) {
			this.playerId = playerId;
		}

		public void setPlayerLevel(int playerLevel) {
			this.playerLevel = playerLevel;
		}

		public void setChangeType(int changeType) {
			this.changeType = changeType;
		}

		public void setChangeAction(String changeAction) {
			this.changeAction = changeAction;
		}

		public void setGoldType(int goldType) {
			this.goldType = goldType;
		}

		public void setGold(int gold) {
			this.gold = gold;
		}

		public void setTime(String time) {
			this.time = time;
		}
	}

	/**
	 * 上报新手指引数据
	 * 
	 * @author hawk
	 */
	public static class TutorialData {
		public String puid;
		public String device;
		public int playerId;
		public int playerLevel;
		public int step;
		public String args;
		public String time;

		public TutorialData() {
			this.time = new Date().toString();
		}

		public TutorialData(String puid, String device, int playerId, int playerLevel, int step, String args, String time) {
			this.puid = puid;
			this.device = device;
			this.playerId = playerId;
			this.playerLevel = playerLevel;
			this.step = step;
			this.args = args;
			this.time = time;
		}

		public void setPuid(String puid) {
			this.puid = puid;
		}

		public void setDevice(String device) {
			this.device = device;
		}

		public void setPlayerId(int playerId) {
			this.playerId = playerId;
		}

		public void setPlayerLevel(int playerLevel) {
			this.playerLevel = playerLevel;
		}

		public void setStep(int step) {
			this.step = step;
		}

		public void setArgs(String args) {
			this.args = args;
		}

		public void setTime(String time) {
			this.time = time;
		}
	}

	/**
	 * 上报充值数据
	 * 
	 * @author hawk
	 */
	public static class ServerData {
		public String ip;
		public int listenPort;
		public int scriptPort;
		public String dbUrl;
		public String dbUser;
		public String dbPwd;

		public ServerData() {
			this.ip = "";
			this.listenPort = 9595;
			this.scriptPort = 9595;
		}

		public ServerData(String ip, int listenPort, int scriptPort, String dbUrl, String dbUser, String dbPwd) {
			this.ip = ip;
			this.listenPort = listenPort;
			this.scriptPort = scriptPort;
			this.dbUrl = dbUrl;
			this.dbUser = dbUser;
			this.dbPwd = dbPwd;
		}

		public String getIp() {
			return ip;
		}

		public void setIp(String ip) {
			this.ip = ip;
		}

		public int getListenPort() {
			return listenPort;
		}

		public void setListenPort(int listenPort) {
			this.listenPort = listenPort;
		}

		public int getScriptPort() {
			return scriptPort;
		}

		public void setScriptPort(int scriptPort) {
			this.scriptPort = scriptPort;
		}

		public String getDbUrl() {
			return dbUrl;
		}

		public void setDbUrl(String dbUrl) {
			this.dbUrl = dbUrl;
		}

		public String getDbUser() {
			return dbUser;
		}

		public void setDbUser(String dbUser) {
			this.dbUser = dbUser;
		}

		public String getDbPwd() {
			return dbPwd;
		}

		public void setDbPwd(String dbPwd) {
			this.dbPwd = dbPwd;
		}
	}

	/**
	 * 上报通用数据
	 * 
	 * @author hawk
	 */
	public static class CommonData {
		public String puid;
		public String device;
		public int playerId;
		public String time;
		public List<String> args;

		public CommonData() {
		}

		public CommonData(String puid, String device, int playerId, String time) {
			this.puid = puid;
			this.device = device;
			this.playerId = playerId;
			this.time = time;
		}

		public void setArgs(String... args) {
			if (args != null) {
				if (this.args == null) {
					this.args = new ArrayList<String>(args.length);
				}

				for (String arg : args) {
					if (arg.length() > 0) {
						this.args.add(arg);
					}
				}
			}
		}
	}
}
