package com.zoll.tools;

/**
 * the interface for convert between primitive type and byte[];
 * 
 * @author qianhang
 * 
 * @date 2015年8月21日 下午3:54:26
 * 
 * @project zoll-RpcClient
 * 
 */
public interface NumberCodec {

	public String convertCharset(String charset);

	public byte[] short2Bytes(short value, int byteLength);

	public byte[] int2Bytes(int value, int byteLength);

	public byte[] long2Bytes(long value, int byteLength);

	public byte[] float2Bytes(float value, int byteLength);

	public byte[] double2Bytes(double value, int byteLength);

	public short bytes2Short(byte[] bytes, int byteLength);

	public int bytes2Int(byte[] bytes, int byteLength);

	public long bytes2Long(byte[] bytes, int byteLength);

	public float bytes2Float(byte[] bytes, int byteLength);

	public double bytes2Double(byte[] bytes, int byteLength);
}
