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

package org.wso2.carbon.device.mgt.iot.common;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.device.mgt.iot.common.datastore.impl.ThriftDataStoreConnector;
import org.wso2.carbon.device.mgt.iot.common.config.server.DeviceCloudConfigManager;
import org.wso2.carbon.device.mgt.iot.common.config.server.datasource.ControlQueue;
import org.wso2.carbon.device.mgt.iot.common.config.server.datasource.DataStore;
import org.wso2.carbon.device.mgt.iot.common.config.server.datasource.DeviceCloudConfig;
import org.wso2.carbon.device.mgt.iot.common.controlqueue.ControlQueueConnector;
import org.wso2.carbon.device.mgt.iot.common.datastore.DataStoreConnector;
import org.wso2.carbon.device.mgt.iot.common.exception.DeviceControllerException;
import org.wso2.carbon.device.mgt.iot.common.exception.UnauthorizedException;
import org.wso2.carbon.device.mgt.iot.common.util.ResourceFileLoader;
import org.wso2.carbon.utils.CarbonUtils;

import java.io.File;
import java.util.HashMap;
import java.util.List;

public class DeviceController {

	private static final Log log = LogFactory.getLog(DeviceController.class);

	private static HashMap<String,DataStoreConnector> dataStoresMap = new HashMap<String,DataStoreConnector>();
	private static ControlQueueConnector mqttControlQueue;



	public static void init(){
		DeviceCloudConfig config = DeviceCloudConfigManager.getInstance().getDeviceCloudMgtConfig();

		if (config != null) {
			initSecurity(config);
			loadDataStores(config);
			loadControlQueues(config);
		}

	}



	private static void loadDataStores(DeviceCloudConfig config){
		List<DataStore> dataStores=config.getDataStores().getDataStore();
		if (dataStores == null) {
			log.error("Error occurred when trying to read data stores configurations");
			return;
		}

		for(DataStore dataStore:dataStores) {
			try {
				String handlerClass = dataStore.getPublisherClass();


				Class<?> dataStoreClass = Class.forName(handlerClass);
				if (DataStoreConnector.class.isAssignableFrom(dataStoreClass)) {

					DataStoreConnector dataStoreConnector = (DataStoreConnector) dataStoreClass.newInstance();
					String configName=dataStore.getName();
					if(dataStore.isEnabled()) {
						dataStoresMap.put(configName, dataStoreConnector);
						dataStoreConnector.initDataStore(dataStore);
					}
				}
			} catch (ClassNotFoundException | IllegalAccessException | InstantiationException ex) {
				log.error("Error occurred when trying to initiate data store", ex);
			} catch (DeviceControllerException ex) {
				log.error(ex.getMessage());
			}
		}
	}

	private static void loadControlQueues(DeviceCloudConfig config){
		List<ControlQueue> controlQueues=config.getControlQueues().getControlQueue();
		if (controlQueues == null) {
			log.error("Error occurred when trying to read data stores configurations");
			return;
		}

		for(ControlQueue controlQueue:controlQueues) {
			try {
				String handlerClass = controlQueue.getControlClass();


				Class<?> controlQueueClass = Class.forName(handlerClass);
				if (ControlQueueConnector.class.isAssignableFrom(controlQueueClass)) {

					if(controlQueue.isEnabled()) {
						mqttControlQueue = (ControlQueueConnector) controlQueueClass.newInstance();
					}
				}
			} catch (ClassNotFoundException | IllegalAccessException | InstantiationException ex) {
				log.error("Error occurred when trying to initiate data store"+controlQueue.getName());
			}
		}

	}

	private static void initSecurity(DeviceCloudConfig config ){

		String trustStoreFile = null;
		String trustStorePassword = null;
		File certificateFile = null;

		trustStoreFile = config.getSecurity().getClientTrustStore();
		trustStorePassword = config.getSecurity().getPassword();
		String certificatePath =
				CarbonUtils.getCarbonHome() + File.separator + "repository" + File.separator +
						"resources" + File.separator + "security" + File.separator;

		certificateFile = new ResourceFileLoader(certificatePath + trustStoreFile).getFile();
		if (certificateFile.exists()) {
			trustStoreFile = certificateFile.getAbsolutePath();
			log.info("Trust Store Path : " + trustStoreFile);

			System.setProperty("javax.net.ssl.trustStore", trustStoreFile);
			System.setProperty("javax.net.ssl.trustStorePassword", trustStorePassword);
		} else {
			log.error("Trust Store not found in path : " + certificateFile.getAbsolutePath());
		}
	}

	public static boolean pushBamData(String owner, String deviceType, String deviceId, Long time,
									  String key,
									  String value, String description)  throws UnauthorizedException {

		HashMap<String, String> deviceDataMap = new HashMap<String, String>();

		deviceDataMap.put("owner", owner);
		deviceDataMap.put("deviceType", deviceType);
		deviceDataMap.put("deviceId", deviceId);
		deviceDataMap.put("time", "" + time);
		deviceDataMap.put("key", key);
		deviceDataMap.put("value", value);
		deviceDataMap.put("description", description);

		return pushData(deviceDataMap, ThriftDataStoreConnector.DataStoreConstants.BAM);

	}
	private static boolean pushData(HashMap<String, String> deviceDataMap,String publisherType)  throws UnauthorizedException{
		try {
			DataStoreConnector dataStoreConnector=dataStoresMap.get(publisherType);
			if(dataStoreConnector==null){

				log.info(publisherType+" is not enabled");
				return false;
			}

			dataStoreConnector.publishIoTData(deviceDataMap);
			return true;
		} catch (DeviceControllerException e) {
			log.error(e.getMessage());
			return false;

		}

	}

	public static boolean pushCEPData(String owner, String deviceType, String deviceId, Long time,
									  String key,
									  String value, String description)  throws UnauthorizedException {
		HashMap<String, String> deviceDataMap = new HashMap<String, String>();

		deviceDataMap.put("owner", owner);
		deviceDataMap.put("deviceType", deviceType);
		deviceDataMap.put("deviceId", deviceId);
		deviceDataMap.put("time", "" + time);
		deviceDataMap.put("key", key);
		deviceDataMap.put("value", value);
		deviceDataMap.put("description", description);

		return pushData(deviceDataMap, ThriftDataStoreConnector.DataStoreConstants.CEP);

	}

	public static boolean publishMqttControl(String owner, String deviceType, String deviceId, String key,
									 String value)
			throws UnauthorizedException {
		HashMap<String, String> deviceControlsMap = new HashMap<String, String>();

		deviceControlsMap.put("owner", owner);
		deviceControlsMap.put("deviceType", deviceType);
		deviceControlsMap.put("deviceId", deviceId);
		deviceControlsMap.put("key", key);
		deviceControlsMap.put("value", value);

//      DeviceValidator deviceChecker = new DeviceValidator();
//      DeviceIdentifier dId = new DeviceIdentifier(deviceId, deviceType);

		try {
//            boolean exists = deviceChecker.isExist(owner, dId);
			boolean exists = true;

			if (exists) {
				if(mqttControlQueue==null){
					log.info("mqtt is not enabled");
					return false;

				}
				mqttControlQueue.enqueueControls(deviceControlsMap);
				return true;
			} else {
				throw new UnauthorizedException(
						"There is no mapping between owner:" + owner + " and device id:" +
								deviceId);
			}
		} catch (DeviceControllerException e) {

			log.error(e.getMessage());
			return false;
		} /*catch (DeviceManagementException e) {
            log.error("Error whilst trying to authenticate the owner with device");
            return false;

        }*/

	}

}