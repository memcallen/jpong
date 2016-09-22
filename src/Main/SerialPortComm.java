package Main;

import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortList;

public class SerialPortComm {

	public static class PortReader implements SerialPortEventListener {
		public SerialPort serialPort = null;

		//public StringBuilder buffer = new StringBuilder();

		public boolean side = false;

		public int left = 50;
		public int right = 50;
		
		public PortReader(SerialPort port) {
			this.serialPort = port;
		}

		@Override
		public synchronized void serialEvent(SerialPortEvent event) {
			if (event.isRXCHAR()) {
				try {
					for (byte b : serialPort.readBytes(serialPort.getInputBufferBytesCount())) {
						switch (b) {
						case ':':
							side = false;
							break;
						case ';':
							side = true;
							break;
						default:
							if(side){
								right = Integer.valueOf(new String(new byte[]{b}));
							}else{
								left = Integer.valueOf(new String(new byte[]{b}));
							}
							break;
						}
					}

				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

	}

	public PortReader getReader() {
		return io;
	}

	private SerialPort serialPort;
	private PortReader io = null;

	public void initialize(String portName) throws Exception {
		serialPort = new SerialPort(portName);
		serialPort.openPort();
		serialPort.setParams(SerialPort.BAUDRATE_9600, SerialPort.DATABITS_8, SerialPort.STOPBITS_1,
				SerialPort.PARITY_NONE);
		serialPort.setFlowControlMode(SerialPort.FLOWCONTROL_RTSCTS_IN | SerialPort.FLOWCONTROL_RTSCTS_OUT);
		io = new PortReader(serialPort);
		serialPort.addEventListener(io, SerialPort.MASK_RXCHAR);
	}

	public void closeConnection() throws Exception {
		serialPort.closePort();
	}

	public void sendData(String data) {
		try {
			serialPort.writeString(data);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public String[] getAvailableSerialPorts() {
		return SerialPortList.getPortNames();
	}
}