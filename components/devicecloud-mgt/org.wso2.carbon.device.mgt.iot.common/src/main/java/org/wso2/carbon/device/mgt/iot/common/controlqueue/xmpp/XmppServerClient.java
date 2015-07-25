package org.wso2.carbon.device.mgt.iot.common.controlqueue.xmpp;

import org.apache.axiom.om.util.Base64;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpHeaders;
import org.wso2.carbon.device.mgt.iot.common.controlqueue.ControlQueueConnector;
import org.wso2.carbon.device.mgt.iot.common.exception.DeviceControllerException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

/**
 * Created by smean-MAC on 7/23/15.
 */
public class XmppServerClient implements ControlQueueConnector {

	private static final Log log = LogFactory.getLog(XmppServerClient.class);

	private static final String XMPP_SERVER_API_CONTEXT = "/plugins/restapi/v1";
	private static final String USERS_API = "/users";
	private static final String GROUPS_API = "/groups";
	private static final String APPLICATION_JSON_MT = "application/json; charset=UTF-8";

	private String xmppEndpoint;
	private String xmppUsername;
	private String xmppPassword;
	private boolean xmppEnabled = false;

	public XmppServerClient() {
	}

	@Override
	public void initControlQueue() throws DeviceControllerException {
		xmppEndpoint = XmppConfig.getInstance().getXmppEndpoint();
		xmppUsername = XmppConfig.getInstance().getXmppUsername();
		xmppPassword = XmppConfig.getInstance().getXmppPassword();
		xmppEnabled = XmppConfig.getInstance().isEnabled();
	}

	@Override
	public void enqueueControls(HashMap<String, String> deviceControls)
			throws DeviceControllerException {
		if (xmppEnabled) {

		} else {
			log.warn("XMPP <Enabled> set to false in 'devicecloud-config.xml'");
		}
	}

	public boolean createXMPPAccount(XmppAccount newUserAccount) throws DeviceControllerException {

		if (xmppEnabled) {
			String xmppUsersAPIEndpoint = xmppEndpoint + XMPP_SERVER_API_CONTEXT + USERS_API;
			if (log.isDebugEnabled()) {
				log.debug("The API Endpoint URL of the XMPP Server is set to: " +
								  xmppUsersAPIEndpoint);
			}

			String encodedString = xmppUsername + ":" + xmppPassword;
			encodedString = Base64.encode(encodedString.getBytes(StandardCharsets.UTF_8));

			String authorizationHeader = "Basic " + encodedString;

			NameValuePair[] xmppAccountPayLoad = new NameValuePair[4];
			xmppAccountPayLoad[0] = new NameValuePair("username", newUserAccount.getUsername());
			xmppAccountPayLoad[1] = new NameValuePair("password", newUserAccount.getPassword());
			xmppAccountPayLoad[2] = new NameValuePair("name", newUserAccount.getAccountName());
			xmppAccountPayLoad[3] = new NameValuePair("email", newUserAccount.getEmail());

			HttpClient httpClient = new HttpClient();
			PostMethod httpPost = new PostMethod(xmppUsersAPIEndpoint);

			httpPost.addRequestHeader(HttpHeaders.AUTHORIZATION, authorizationHeader);
			httpPost.addRequestHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_MT);
			httpPost.setRequestBody(xmppAccountPayLoad);

			int responseStatusFromUserCreation;
			String responseFromUserCreation = "";
			try {
				responseStatusFromUserCreation = httpClient.executeMethod(httpPost);
				responseFromUserCreation = httpPost.getResponseBodyAsString();
			} catch (IOException e) {
				String errorMsg =
						"Error occured whilst trying a 'POST' at : " + xmppUsersAPIEndpoint;
				log.error(errorMsg);
				throw new DeviceControllerException(errorMsg, e);
			}

			if (responseStatusFromUserCreation != HttpStatus.SC_CREATED) {
				String errorMsg =
						"XMPP Server returned status: '" + responseStatusFromUserCreation +
								" " + HttpStatus.getStatusText(responseStatusFromUserCreation) +
								"' for account creation with error:\n" + responseFromUserCreation;
				log.error(errorMsg);
				throw new DeviceControllerException(errorMsg);
			}
		} else {
			log.warn("XMPP <Enabled> set to false in 'devicecloud-config.xml'");
			return false;
		}
		return true;
	}
}
