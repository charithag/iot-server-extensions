/*
 * Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.device.mgt.iot.firealarm.api;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.device.mgt.common.DeviceManagementException;
import org.wso2.carbon.device.mgt.iot.common.devicecloud.DeviceController;
import org.wso2.carbon.device.mgt.iot.common.devicecloud.config.DeviceCloudConfigManager;
import org.wso2.carbon.device.mgt.iot.common.devicecloud.config.DeviceCloudManagementConfig;
import org.wso2.carbon.device.mgt.iot.common.devicecloud.config
		.DeviceCloudManagementControllerConfig;
import org.wso2.carbon.device.mgt.iot.common.devicecloud.config.controlqueue
		.DeviceControlQueueConfig;
import org.wso2.carbon.device.mgt.iot.common.devicecloud.exception.DeviceControllerException;
import org.wso2.carbon.device.mgt.iot.common.devicecloud.exception.UnauthorizedException;
import org.wso2.carbon.device.mgt.iot.firealarm.api.util.DeviceJSON;
import org.wso2.carbon.device.mgt.iot.firealarm.api.util.MQTTFirealarmSubscriber;
import org.wso2.carbon.device.mgt.iot.firealarm.constants.FireAlarmConstants;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.*;


public class FireAlarmControllerService {

	private static Log log = LogFactory.getLog(FireAlarmControllerService.class);

	public static final String CONTROL_QUEUE_ENDPOINT;
	private static Map<String, LinkedList<String>> replyMsgQueue =new HashMap<>();;
	private static Map<String, LinkedList<String>> internalControlsQueue=new HashMap<>();
	private static MQTTFirealarmSubscriber mqttFireAlarmSubscriber;

	static {

		DeviceCloudManagementConfig config = null;
		try {
			config = DeviceCloudConfigManager.getInstance().getDeviceCloudMgtConfig();
		} catch (DeviceControllerException ex) {
			log.error(ex.getMessage());
		}

		if (config != null) {
			// controller configurations
			DeviceCloudManagementControllerConfig controllerConfig =
					config.getDeviceCloudManagementControllerConfig();

			// reading control queue configurations
			String controlQueueKey = controllerConfig.getDeviceControlQueue();
			DeviceControlQueueConfig controlQueueConfig = config.getControlQueuesMap().get(
					controlQueueKey);
			if (controlQueueConfig == null) {
				log.error("Error occurred when trying to read control queue configurations");
			}

			String mqttUrl = "";
			String mqttPort = "";
			if (controlQueueConfig != null) {
				mqttUrl = controlQueueConfig.getEndPoint();
				mqttPort = controlQueueConfig.getPort();
			}

			CONTROL_QUEUE_ENDPOINT = mqttUrl + ":" + mqttPort;
			log.info("CONTROL_QUEUE_ENDPOINT Successfully initialized.");
		} else {
			CONTROL_QUEUE_ENDPOINT = null;
			log.error("CONTROL_QUEUE_ENDPOINT initialization failed.");
		}

	}


	public void setMqttFireAlarmSubscriber(MQTTFirealarmSubscriber mqttFireAlarmSubscriber) {
		FireAlarmControllerService.mqttFireAlarmSubscriber = mqttFireAlarmSubscriber;
		try {
			mqttFireAlarmSubscriber.subscribe();
		} catch (DeviceManagementException e) {
			log.error(e.getErrorMessage());
		}
	}

	public MQTTFirealarmSubscriber getMqttFireAlarmSubscriber() {
		return mqttFireAlarmSubscriber;
	}

	public static Map<String, LinkedList<String>> getReplyMsgQueue() {
		return Collections.unmodifiableMap(replyMsgQueue);
	}

	public static Map<String, LinkedList<String>> getInternalControlsQueue() {
		return Collections.unmodifiableMap(internalControlsQueue);
	}

	/*    Service to switch "ON" and "OFF" the FireAlarm bulb
			   Called by an external client intended to control the FireAlarm bulb */
	@Path("/switchbulb")
	@POST
	public void switchBulb(@HeaderParam("owner") String owner,
						   @HeaderParam("deviceId") String deviceId,
						   @Context HttpServletResponse response) {

		try {
			boolean result = DeviceController.setControl(owner, FireAlarmConstants.DEVICE_TYPE,
														 deviceId, "BULB", "IN");
			if (result) {
				response.setStatus(HttpStatus.SC_ACCEPTED);

			} else {
				response.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
			}

		} catch (UnauthorizedException e) {
			response.setStatus(HttpStatus.SC_UNAUTHORIZED);

		}
	}

	/*    Service to read the temperature from the FireAlarm temperature sensor
				   Called by an external client intended to get the current temperature */
	@Path("/readtemperature")
	@GET
	public String requestTemperature(@HeaderParam("owner") String owner,
									 @HeaderParam("deviceId") String deviceId,
									 @Context HttpServletResponse response) {
		String replyMsg;
		try {
			boolean result = DeviceController.setControl(owner, FireAlarmConstants.DEVICE_TYPE,
														 deviceId, "TEMPERATURE", "IN");
			if (result) {
				response.setStatus(HttpStatus.SC_ACCEPTED);
				replyMsg = "Request to read temperature sent to controls queue. Please wait...";
				return replyMsg;
			} else {
				response.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
				replyMsg =
						"An error occured whilst routing the request to queue. Re-try after some" +
								" " +
								"time...";
				return replyMsg;
			}

		} catch (UnauthorizedException e) {
			response.setStatus(HttpStatus.SC_UNAUTHORIZED);
			replyMsg =
					"An error occured whilst routing the request to queue. Re-try after some " +
							"time...";
			return replyMsg;
		}
	}

	/*    Service to toggle the FireAlarm fan between "ON" and "OFF"
			   Called by an external client intended to control the FireAlarm fan */
	@Path("/togglefan")
	@POST
	public void switchFan(@HeaderParam("owner") String owner,
						  @HeaderParam("deviceId") String deviceId,
						  @Context HttpServletResponse response) {

		try {
			boolean result = DeviceController.setControl(owner, FireAlarmConstants.DEVICE_TYPE,
														 deviceId, "FAN", "IN");
			if (result) {
				response.setStatus(HttpStatus.SC_ACCEPTED);

			} else {
				response.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);

			}

		} catch (UnauthorizedException e) {
			response.setStatus(HttpStatus.SC_UNAUTHORIZED);

		}

	}

	/*    Service to poll the control-queue for the controls sent to the FireAlarm
			   Called by the FireAlarm device  */
	@Path("/readcontrols/{owner}/{deviceId}")
	@GET
	public String readControls(@PathParam("owner") String owner,
							   @PathParam("deviceId") String deviceId,
							   @Context HttpServletResponse response) {
		String result;
		LinkedList<String> deviceControlList = internalControlsQueue.get(deviceId);

		if (deviceControlList == null) {
			result = "No controls have been set for device " + deviceId + " of owner " + owner;
			response.setStatus(HttpStatus.SC_NO_CONTENT);
		} else {
			try {
				result = deviceControlList.remove(); //returns the  head value
				response.setStatus(HttpStatus.SC_ACCEPTED);

			} catch (NoSuchElementException ex) {
				result = "There are no more controls for device " + deviceId + " of owner " +
						owner;
				response.setStatus(HttpStatus.SC_NO_CONTENT);
			}
		}
		if (log.isDebugEnabled()) {
			log.debug(result);
		}
		return result;
	}

	/*    Service to send back the replies for the controls sent to the FireAlarm
		   Called by the FireAlarm device  */
	@Path("/reply")
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	public void reply(final DeviceJSON replyMsg,
					  @Context HttpServletResponse response) {
		try {
			boolean result = DeviceController.setControl(replyMsg.owner,
														 FireAlarmConstants.DEVICE_TYPE,
														 replyMsg.deviceId,
														 replyMsg.reply, "OUT");
			if (result) {
				response.setStatus(HttpStatus.SC_ACCEPTED);

			} else {
				response.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);

			}

		} catch (UnauthorizedException e) {
			response.setStatus(HttpStatus.SC_UNAUTHORIZED);

		}

	}

	/*    Service to push all the sensor data collected by the FireAlarm
		   Called by the FireAlarm device  */
	@Path("/pushalarmdata")
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	public void pushAlarmData(
			final DeviceJSON dataMsg, @Context HttpServletResponse response) {
		boolean result;

		String sensorValues = dataMsg.value;
		log.info("Recieved Sensor Data Values: " + sensorValues);

		String sensors[] = sensorValues.split(":");
		try {
			if (sensors.length == 3) {
				String temperature = sensors[0];
				String bulb = sensors[1];
				String fan = sensors[2];

				sensorValues = "Temperature:" + temperature + "C\tBulb Status:" + bulb +
						"\t\tFan Status:" +
						fan;
				log.info(sensorValues);

				result = DeviceController.pushData(dataMsg.owner, FireAlarmConstants.DEVICE_TYPE,
												   dataMsg.deviceId,
												   System.currentTimeMillis(),
												   "DeviceData", temperature, "TEMP");

				if (!result) {
					response.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
					return;
				}

				result = DeviceController
						.pushData(dataMsg.owner, FireAlarmConstants.DEVICE_TYPE, dataMsg.deviceId,
								  System.currentTimeMillis(),
								  "DeviceData", bulb, "BULB");

				if (!result) {
					response.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
					return;
				}

				result = DeviceController.pushData(dataMsg.owner, FireAlarmConstants.DEVICE_TYPE,
												   dataMsg.deviceId, System.currentTimeMillis(),
												   "DeviceData", fan, "FAN");

				if (!result) {
					response.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
				}

			} else {
				result = DeviceController.pushData(dataMsg.owner, FireAlarmConstants.DEVICE_TYPE,
												   dataMsg.deviceId, System.currentTimeMillis(),
												   "DeviceData", dataMsg.value, dataMsg.reply);
				if (!result) {
					response.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
				}
			}

		} catch (UnauthorizedException e) {
			response.setStatus(HttpStatus.SC_UNAUTHORIZED);

		}

	}
}
