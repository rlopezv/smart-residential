package com.upm.miot.asp.smartresidential.control;

import java.util.Optional;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.upm.miot.asp.smartreidential.AppConstants;
import com.upm.miot.asp.smartresidential.data.Message;
import com.upm.miot.asp.smartresidential.data.MessageBuilder;
import com.upm.miot.asp.smartresidential.data.MessageException;
import com.upm.miot.asp.smartresidential.element.SrActuator;
import com.upm.miot.asp.smartresidential.model.SrIrrigationPlan;
import com.upm.miot.asp.smartresidential.model.SrSystemMeassure;

/**
 *
 * @author ramon
 *
 */
public class BaseSystemController extends AbstractSystemController {

	private Logger LOGGER = LoggerFactory.getLogger(BaseSystemController.class);

	@Override
	protected void init() {
		String sInterval = getConfig().getOrDefault(AppConstants.MEASURE_INTERVAL, "1");
		setMeasureInterval(Long.valueOf(sInterval) * 60000);
	}

	@Override
	protected void handleMessage(Message message) {
		LOGGER.info("Message recevied:{}", message);
		try {
			if (message.getTopic().endsWith(AppConstants.PLAN)) {
				SrIrrigationPlan plan = MessageBuilder.getBuilder().parseObject(message.getMqttMessage().getPayload(),
						SrIrrigationPlan.class);
				LOGGER.info("Irrigation Plan:{}", plan);
			} else {
				String actuatorType = message.getTopic().substring(message.getTopic().lastIndexOf("/") + 1,
						message.getTopic().length());
				Optional<SrActuator> actuator = getSystem().getActuators().stream()
						.filter(p -> actuatorType.equalsIgnoreCase(p.getType())).findFirst();
				if (actuator.isPresent() && "off".equalsIgnoreCase(message.getMqttMessage().toString())) {
					if (actuator.get().getStatus()) {
						actuator.get().setStatus(Boolean.FALSE);
						LOGGER.info("Change actuator status ({}):{}", actuatorType, Boolean.FALSE);
					}
				} else if (actuator.isPresent() && "on".equalsIgnoreCase(message.getMqttMessage().toString())) {
					actuator.get().setStatus(Boolean.TRUE);
					LOGGER.info("Change actuator status ({}):{}", actuatorType, Boolean.TRUE);
				} else {
					LOGGER.warn("Actuator or command not valid ({}):{}", message.getTopic(),
							message.getMqttMessage().toString());
				}
			}
		} catch (MessageException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	protected void generateMessage() {
		SrSystemMeassure message = getSystem().getMeasures();
		try {
			getClient().publish(MessageBuilder.getBuilder().buildMessage(message));
		} catch (MqttException | MessageException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
