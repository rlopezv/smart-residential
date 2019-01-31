/**
 *
 */
package com.upm.miot.asp.smartresidential.control;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.upm.miot.asp.smartreidential.AppConstants;
import com.upm.miot.asp.smartresidential.config.ActuatorConfig;
import com.upm.miot.asp.smartresidential.config.SensorConfig;
import com.upm.miot.asp.smartresidential.config.SystemConfig;
import com.upm.miot.asp.smartresidential.data.Message;
import com.upm.miot.asp.smartresidential.element.SrActuator;
import com.upm.miot.asp.smartresidential.element.SrSensor;
import com.upm.miot.asp.smartresidential.element.SrSystem;

/**
 * Base class for controllers Provides common services Uses an object of the
 * type MqttClientConfig for its configuration It's handled as a Thread
 * Implements MqttCallback form Pahoo for handling incoming messages
 *
 * @author ramon
 *
 */
public abstract class AbstractSystemController implements MqttCallback, Runnable {

	private Logger LOGGER = LoggerFactory.getLogger(AbstractSystemController.class);

	private Client client = null;
	private Map<String, String> config = new HashMap<>();
	private boolean exit = false;
	private final BlockingQueue<Message> messageQueue = new LinkedBlockingQueue<>();
	private SrSystem system;
	private long measureInterval;

	public AbstractSystemController() {
	}

	public void config(SystemConfig systemConfig) {
		system = new SrSystem();
		system.setId(systemConfig.getSystemId());
		for (SensorConfig sensorConfig : systemConfig.getSensors()) {
			addSensor(sensorConfig);
		}

		for (ActuatorConfig actuatorConfig : systemConfig.getActuators()) {
			addActuator(actuatorConfig);
		}

		this.config = systemConfig.getAdditionalProperties();
		systemConfig.getMqttConfig().setTopic(AppConstants.APP_NAME + "/" + system.getId());
		this.client = new Client(systemConfig.getMqttConfig());
	}

	private void addActuator(ActuatorConfig actuatorConfig) {
		SrActuator actuator = new SrActuator();
		actuator.setType(actuatorConfig.getElementType());
		actuator.setStatus(actuatorConfig.getDefaultValue());
		system.addActuator(actuator);
	}

	private void addSensor(SensorConfig sensorConfig) {
		SrSensor sensor = new SrSensor();
		sensor.setType(sensorConfig.getElementType());
		sensor.setMaxValue(sensorConfig.getMaxValue());
		sensor.setMinValue(sensorConfig.getMinValue());
		system.addSensor(sensor);
	}

	public Client getClient() {
		return client;
	}

	public void setClient(Client client) {
		this.client = client;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.paho.client.mqttv3.MqttCallback#connectionLost(java.lang.
	 * Throwable)
	 */
	@Override
	public void connectionLost(Throwable th) {
		LOGGER.warn("Connection lost", th);
		while (!getClient().isConnected()) {
			LOGGER.warn("Reconnecting");
			try {
				getClient().connect();
				getClient().subscribe();
			} catch (MqttException e) {
				LOGGER.error("Error reconnecting", e);
				try {
					this.wait(1000);
				} catch (InterruptedException e1) {
					LOGGER.error("Interrupted Exeption", e);
				}
			}

		}

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.eclipse.paho.client.mqttv3.MqttCallback#deliveryComplete(org.eclipse.
	 * paho.client.mqttv3.IMqttDeliveryToken)
	 */
	@Override
	public void deliveryComplete(IMqttDeliveryToken token) {
		try {
			LOGGER.info("Delivered message:{}", token.getMessage().toString());
		} catch (MqttException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.paho.client.mqttv3.MqttCallback#messageArrived(java.lang.
	 * String, org.eclipse.paho.client.mqttv3.MqttMessage)
	 */
	@Override
	public void messageArrived(String topic, MqttMessage message) throws Exception {
		LOGGER.info("Message arrived ({}):{}", topic, message.toString());
		handleMessage(new Message().topic(topic).mqttMessage(message));
	}

	public void stop() throws MqttException {
		LOGGER.info("Stopping client");
		this.exit = true;
		this.getClient().disconnect();
	}

	public boolean isActive() {
		return !exit;
	}

	public Map<String, String> getConfig() {
		return config;
	}

	public void setConfig(Map<String, String> config) {
		this.config = config;
	}

	protected Message nextMessage() throws InterruptedException {
		return messageQueue.take();
	}

	@Override
	public void run() {
		// If necessary it configure external connections
		try {
			connect();
			getClient().setHandler(this);
			init();
			while (isActive()) {
				if (getMeasureInterval() > 0) {
					synchronized (this) {
						try {
							wait(getMeasureInterval());
							generateMessage();
						} catch (InterruptedException e) {
							LOGGER.error("Error taking measures");
						}
					}
				}
//				try {
//					Message message = nextMessage();
//					if (MessageTypeEnum.MQTTMESSAGE == message.getType()) {
//						handleMessage(message);
//					} else {
//						stop();
//					}
//				} catch (Exception e) {
//					LOGGER.error("Error handling message", e);
//				}
			}
		} catch (MqttException e1) {
			LOGGER.error("Cannot connect to broker, waiting");
			try {
				synchronized (this) {
					wait(100);
				}
			} catch (InterruptedException e) {
				LOGGER.error("Error syncing");
			}
		}

	}

	protected void connect() throws MqttException {
		getClient().start();
	}

	protected abstract void init();

	protected abstract void handleMessage(Message message);

	protected abstract void generateMessage();

	protected long getMeasureInterval() {
		return measureInterval;
	}

	protected void setMeasureInterval(long measureInterval) {
		this.measureInterval = measureInterval;
	}

	public SrSystem getSystem() {
		return system;
	}

	public void setSystem(SrSystem system) {
		this.system = system;
	}

}
