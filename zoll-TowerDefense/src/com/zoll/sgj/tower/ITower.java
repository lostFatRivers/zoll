package com.zoll.sgj.tower;

import com.zoll.sgj.dispatcher.ITick;
import com.zoll.sgj.monster.IMonster;

/**
 * 塔防中防御塔接口;
 * 
 * @author qianhang
 * 
 * @date 2015年8月29日 下午5:07:19
 * 
 * @project zoll-TowerDefense
 * 
 */
public interface ITower extends ITick{
	/**
	 * 攻击接口;
	 */
	public void doAttack();
	
	public boolean isInRange(IMonster monster);
}
