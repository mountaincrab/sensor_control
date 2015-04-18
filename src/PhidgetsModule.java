import java.awt.Label;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoSerializable;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.phidgets.*;
import com.phidgets.event.*;

import javax.imageio.metadata.IIOInvalidTreeException;
import javax.swing.*;

public class PhidgetsModule implements IModule, AttachListener, SensorChangeListener, KryoSerializable, Serializable {
	private PhidgetsMessageSender[] messageSenders;
	private Integer ikpSerial;
	private Integer[] triggerChangeValues;
	private transient InterfaceKitPhidget ikp;
	private transient JPanel controlPanel;
	private transient ExecutorService exe;
	private transient ExecutorService[] exes;
	private transient JComboBox<Integer>[] triggerControls;
		
	public PhidgetsModule() throws PhidgetException {
		messageSenders = new PhidgetsMessageSender[8];
		triggerChangeValues = new Integer[8];
		for (int x=0; x<8; x++) {
			messageSenders[x] = new PhidgetsMessageSender(x);
			triggerChangeValues[x] = 8;
		}
		ikpSerial = null;
		
		init();
	}	
	
	public void attached(AttachEvent ae) {
		try {
			System.out.println(ae.getSource().getSerialNumber());
		} catch (PhidgetException pe) {}
	}
	
	// The caller of this method will wait for it to return before sending
	// the next event. Therefore, it doesn't need to be synchronized.
	public void sensorChanged(final SensorChangeEvent sce) {
		exes[sce.getIndex()].execute(new Runnable() {
			public void run() {
				messageSenders[sce.getIndex()].send(new SensorChangeEvent(ikp, 0, sce.getValue()));	
			}
		});
	}

	public Object[] getMessageSenders() {
		return messageSenders;
	}
	
	public Object[] getMessageListeners() {
		return new Object[0];
	}

	public JComponent getControlPanel() {
		return controlPanel;
	}

	public void delete() {
		try {
			ikp.close();
		} catch (PhidgetException e) {
			e.printStackTrace();
		}
	}

	public String getListenerLabel(int index) {
		return null;
	}

	public String getSenderLabel(int index) {
		return Integer.toString(index);
	}

	public String getLabel() {
		return "Phidgets Board";
	}
	
	private void init() throws PhidgetException {
		// Create GUI.
		controlPanel = new JPanel();
		controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.Y_AXIS));
		// Insert title labels.
		controlPanel.add(new JLabel("Phidgets Module"));
		controlPanel.add(new JLabel("Sensor trigger settings: "));
		
		// Create array of controls for the sensor trigger settings.
		triggerControls = new JComboBox[8];
		Integer[] values = new Integer[]{1,2,4,8,16,24,32,40,48,56,64,72,80,88,96,104,112,118,126};
		for (int x=0; x<triggerControls.length; x++) {
			triggerControls[x] = new JComboBox<Integer>(new DefaultComboBoxModel<Integer>(values));
			// Add action listener which will set the change trigger to the selected value.
			triggerControls[x].setSelectedItem(triggerChangeValues[x]);
			triggerControls[x].addActionListener(new TriggerControlListener(x));
			JPanel row = new JPanel();
			row.add(new JLabel("Sensor " + Integer.toString(x) + ": "));
			row.add(triggerControls[x]);
			controlPanel.add(row);
		}
		
		// Create thread pools. These should not be serialised.
		exes = new ExecutorService[8];
		for (int x=0; x<exes.length; x++) {
			exes[x] = Executors.newSingleThreadExecutor();
		}
		
		ikp = new InterfaceKitPhidget();
		
		// If ikpSerial has already been saved (the session has been saved and loaded
		// from disk, then open that particular serial, else just open any.
		if (ikpSerial != null) {
			ikp.open(ikpSerial);
		} else {
			ikp.openAny();
		}
		ikpSerial = 872983;
		/*ikp.waitForAttachment();
		ikpSerial = ikp.getSerialNumber();
		ikp.addAttachListener(this);
		ikp.addSensorChangeListener(this);
		
		// Set default sensor change trigger values.
		for (int x=0; x<8; x++) {
			ikp.setSensorChangeTrigger(x, (Integer)triggerControls[x].getSelectedItem());
		}*/
	}

	public void write(Kryo kryo, Output output) {
		kryo.writeObject(output, ikpSerial);
		kryo.writeObject(output, messageSenders);
		kryo.writeObject(output, triggerChangeValues);
	}

	public void read(Kryo kryo, Input input) {
		ikpSerial = kryo.readObject(input, Integer.class);
		messageSenders = kryo.readObject(input, PhidgetsMessageSender[].class);
		triggerChangeValues = kryo.readObject(input, Integer[].class);
		/*try {
			init();
		} catch (PhidgetException e) {
			JOptionPane.showMessageDialog(null, "An unnknown error occurred during loading the Phidgets Module.");
		}*/
	}
	
	
	private class TriggerControlListener implements ActionListener {
		private int index;
		public TriggerControlListener(int index) {
			this.index = index;
		}
		public void actionPerformed(ActionEvent e) {
			try {
				triggerChangeValues[index] = (Integer) triggerControls[index].getSelectedItem();
				ikp.setSensorChangeTrigger(index, triggerChangeValues[index]);
			} catch (PhidgetException e1) {
				e1.printStackTrace();
			}
		}
	}
	
	// Note: these methods are not used in the application, they were used in testing to compare 
	// the speed of Java native serialization and Kryo serilaization.
	private void writeObject(ObjectOutputStream output) throws IOException {
		output.writeObject(ikpSerial);
		output.writeObject(messageSenders);
		output.writeObject(triggerChangeValues);
	}
	
	private void readObject(ObjectInputStream input) throws ClassNotFoundException, IOException {
		ikpSerial = (int) input.readObject();
		messageSenders = (PhidgetsMessageSender[]) input.readObject();
		triggerChangeValues = (Integer[]) input.readObject();
	}
}
