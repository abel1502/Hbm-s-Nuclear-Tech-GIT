package com.hbm.dim.trait;

import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;

public class CBT_Invasion extends CelestialBodyTrait{

	public int wave;
	public int kills;
	public double waveTime;
	

	public CBT_Invasion() {}

	public CBT_Invasion(int wave, double waveTime) {
		this.wave = wave;
		this.waveTime = waveTime;
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		nbt.setInteger("wave", wave);
		nbt.setInteger("kills", kills);
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		wave = nbt.getInteger("wave");
		kills = nbt.getInteger("kills");

	}

	@Override
	public void writeToBytes(ByteBuf buf) {
		buf.writeInt(kills);
		buf.writeInt(wave);
		buf.writeDouble(waveTime);

	}

	@Override
	public void readFromBytes(ByteBuf buf) {
		kills = buf.readInt();
		wave = buf.readInt();
		waveTime = buf.readDouble();

	}

}
