package com.sensorcontrol.modules.midicontroller;
import java.util.ArrayList;

import com.sensorcontrol.modules.IMessageListenerMidi;
import com.sensorcontrol.modules.IMessageSender;


public class StMcMessageSender implements IMessageSender 
{
	ArrayList<IMessageListenerMidi> midiListeners;
	
	public void setMidiListeners(ArrayList<IMessageListenerMidi> midiListeners)
	{
		this.midiListeners = midiListeners;
	}

	public void addMessageListener(Object listener) throws ClassCastException 
	{
		IMessageListenerMidi midiListener = (IMessageListenerMidi) listener;
		midiListeners.add(midiListener);
	}
	
	public String toString()
	{
		return "Out";
	}

	public void removeMessageListener(Object listener) 
	{
	}
	
}