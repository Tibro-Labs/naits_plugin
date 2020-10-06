/*******************************************************************************
 * Copyright (c),  2017 TIBRO DOOEL Skopje
 *******************************************************************************/

package naits_triglav_plugin;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.UUID;

import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.Duration;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.prtech.svarog.I18n;
import com.prtech.svarog.SvConf;
import com.prtech.svarog.SvException;
import com.prtech.svarog.SvReader;
import com.prtech.svarog.SvSecurity;
import com.prtech.svarog_common.DbDataArray;
import com.prtech.svarog_common.DbDataObject;

/**
 * Class for security checks while register/log in/log out/update pass/recover
 * user
 *
 * @author TIBRO_001
 *
 */

@Path("/SvSSO")
public class SvSSO {
	static final Logger log4j = LogManager.getLogger(SvSSO.class.getName());
	static HashMap<String, Object[]> passRecoveryTokens = new HashMap<String, Object[]>();

	/**
	 * Log in screen
	 * 
	 * @param l_user
	 * @param l_pass
	 * @return
	 * @author f.r*
	 */
	@Path("/logon/{l_user}/{l_pass}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response getToken(@PathParam("l_user") String l_user, @PathParam("l_pass") String l_pass) {
		String token = null;
		SvSecurity svsec = null;
		try {
			svsec = new SvSecurity();
			if (!(l_user == null || l_pass == null)) {
				token = svsec.logon(l_user.toUpperCase(), l_pass.toUpperCase());
			}
		} catch (SvException e) {
			return Response.status(401).entity(e.getFormattedMessage()).build();
		} finally {
			if (svsec != null) {
				svsec.release();
			}
		}
		return Response.status(200).entity(token).build();
	}

	@Path("/validateToken/{session_id}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response validateToken(@PathParam("session_id") String sessionId) {
		SvReader svReader = null;
		try {
			svReader = new SvReader(sessionId);
		} catch (Exception e) {
			return Response.status(401).entity(e.getMessage()).build();
		} finally {
			if (svReader != null) {
				svReader.release();
			}
		}
		return Response.status(200).entity("OK").build();
	}

	@Path("/logoff/{session_id}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response destroyToken(@PathParam("session_id") String sessionId) {
		SvSecurity svs = null;
		try {
			svs = new SvSecurity();
			svs.logoff(sessionId);
		} catch (Exception e) {
			return Response.status(401).entity(e.getMessage()).build();
		} finally {
			if (svs != null) {
				svs.release();
			}
		}
		return Response.status(200).entity("OK").build();
	}

	void cleanUpRecoveryData() {
		DateTime now = new DateTime();
		try {
			for (Iterator<Entry<String, Object[]>> it = passRecoveryTokens.entrySet().iterator(); it.hasNext();) {
				Entry<String, Object[]> ent = it.next();
				Object[] recoveryData = ent.getValue();
				DateTime ts = (DateTime) recoveryData[1];
				Duration duration = new Duration(ts, now);
				if (duration.toStandardMinutes().getMinutes() > 10) {
					it.remove();
				}
			}
		} catch (Exception ex) {
			log4j.error("Error cleaning up recovery data", ex);
		}
	}

	@Path("/updatePassword/{token}/{old_pass}/{new_pass}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response updatePassword(@PathParam("token") String token, @PathParam("old_pass") String oldPass,
			@PathParam("new_pass") String newPass, @Context HttpServletRequest httpRequest) {
		SvReader svr = null;
		SvSecurity svs = null;
		String returnMsgLbl = "updatePassword.error";
		try {
			svs = new SvSecurity();
			svr = new SvReader(token);
			DbDataObject dboUser = SvReader.getUserBySession(token);
			if (dboUser != null && dboUser.getObject_id() != 0) {
				svs.updatePassword(dboUser.getVal(Tc.USER_NAME).toString(), oldPass, newPass);
				returnMsgLbl = "updatePassword.success";
			}
		} catch (Exception e) {
			if (e instanceof SvException) {
				returnMsgLbl = ((SvException) e).getFormattedMessage();
				log4j.info("Error in SvSSO.updatePassword():" + ((SvException) e).getFormattedMessage(), e);
			} else {
				returnMsgLbl = e.toString();
				log4j.info("Error in SvSSO.updatePassword():" + e.getMessage(), e);
			}
		} finally {
			if (svr != null)
				svr.release();
			if (svs != null)
				svs.release();
		}
		return Response.status(200).entity(I18n.getText(returnMsgLbl)).build();
	}

	@Path("/updatePasswordAfterRecovery/{token}/{username}/{pin}/{new_pass}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response updatePasswordAfterRecovery(@PathParam("token") String token,
			@PathParam("username") String username, @PathParam("pin") String pin,
			@PathParam("new_pass") String new_pass, @Context HttpServletRequest httpRequest) {
		Object[] recoveryData = null;
		SvSecurity svs = null;
		String returnMsgLbl = "naits.success.updatePasswordAfterRecovery";
		try {
			svs = new SvSecurity();
			cleanUpRecoveryData();
			recoveryData = passRecoveryTokens.get(username);
			if (recoveryData == null || !token.equals(recoveryData[0])) {
				returnMsgLbl = "naits.error.recoveryDataListIsEmpty";
				log4j.error("Recovery data list is empty!");
				return Response.status(200).entity(returnMsgLbl).build();
			}
			if (!token.equals(recoveryData[0])) {
				returnMsgLbl = "naits.error.tokensAreNotEqual";
				log4j.error("Tokens compared are not equal. Input token:" + token + ", token found in the list: "
						+ recoveryData[0] + ". Check for possible wrong combination sent.");
				return Response.status(200).entity(returnMsgLbl).build();
			}
			svs.recoverPassword(username, pin, new_pass);
		} catch (Exception e) {
			returnMsgLbl = "naits.error.updatePasswordAfterRecovery";
			log4j.info("Error in SvSSO.updatePasswordAfterRecovery: ", e);
		} finally {
			if (svs != null)
				svs.release();
		}
		return Response.status(200).entity(returnMsgLbl).build();

	}

	@Path("/changePassword/{token}/{idbr}/{emb}/{new_pass}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response changePassword(@PathParam("idbr") String idbr, @PathParam("token") String token,
			@PathParam("new_pass") String newPassword, @PathParam("emb") String emb,
			@Context HttpServletRequest httpRequest) {
		cleanUpRecoveryData();
		Object[] recoveryData = passRecoveryTokens.get(idbr);
		String error = null;
		boolean farmerFound = false;
		SvSecurity svs = null;
		try {
			if (recoveryData != null) {
				if (token.equals(recoveryData[0])) {
					DbDataObject dboUser = null;
					svs = new SvSecurity();
					dboUser = svs.getUser(idbr);
					if (dboUser == null) {
						error = I18n.getText("user.user_not_found");
					} else {
						if (dboUser.getVal(Tc.USER_TYPE).equals("EXTERNAL")) {
							String farmExternalId = null;
							DbDataArray farmers = svs.getPOAObjects(dboUser.getObject_id(), "FARMER");
							if (farmers != null) {
								for (DbDataObject farm : farmers.getItems()) {
									farmExternalId = ((String) farm.getVal("FTYPE")).equals("G")
											? ((String) farm.getVal("TAX_NO")) : ((String) farm.getVal("ID_NO"));
									if (((String) farm.getVal("FIC")).equals(idbr) && farmExternalId.equals(emb)) {
										farmerFound = true;
										break;
									}
								}
							}
							if (farmerFound)
								dboUser.setVal(Tc.PASSWORD_HASH, (newPassword.toUpperCase()));
							else {
								error = I18n.getText("system.farmer_id_not_matched");
							}
						} else
							dboUser.setVal(Tc.PASSWORD_HASH, (newPassword.toUpperCase()));
						/*
						 * with overwrite flag, couse need to update
						 * password_hash f.r
						 */
						svs.createUser((String) dboUser.getVal(Tc.USER_NAME), (String) dboUser.getVal(Tc.PASSWORD_HASH),
								(String) dboUser.getVal(Tc.FIRST_NAME), (String) dboUser.getVal(Tc.LAST_NAME),
								(String) dboUser.getVal(Tc.E_MAIL), (String) dboUser.getVal(Tc.PIN),
								(String) dboUser.getVal("TAX_ID"), (String) dboUser.getVal(Tc.USER_TYPE), null, true);
						error = I18n.getText("system.password_changed_success");
						passRecoveryTokens.remove(idbr);
					}
				}
			} else
				error = I18n.getText("system.password_recovery_wrong_token");
		} catch (SvException e) {
			error = e.getFormattedMessage();
		} finally {
			if (svs != null)
				svs.release();
		}
		return Response.status(200).entity(error).build();

	}

	@Path("/createUser/{user}/{pass}/{pinVat}/{email}/{firstName}/{lastName}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response createUser(@PathParam("user") String userName, @PathParam("pass") String password,
			@PathParam("pinVat") String pin, @PathParam("email") String eMail, @PathParam("firstName") String firstName,
			@PathParam("lastName") String lastName, @Context HttpServletRequest httpRequest) {
		String resultMsg = "naits.success.createUser";
		if (!(userName == null || password == null || eMail == null || pin == null)) {
			DbDataObject dboUser = null;
			SvSecurity svs = null;
			try {
				svs = new SvSecurity();
				boolean userExists = svs.checkIfUserExistsByUserName(userName);
				if (userExists) {
					resultMsg = "err;" + I18n.getText("createUser.user_exist");
					return Response.status(200).entity(resultMsg).build();
				}
				dboUser = svs.createUser(userName.toUpperCase(), password.toUpperCase(), firstName.toUpperCase(),
						lastName.toUpperCase(), eMail, pin.toUpperCase(), null, "INTERNAL", "PENDING");
				if (dboUser != null) {
					sendActivationEmail(dboUser, httpRequest);
				}
			} catch (SvException e) {
				resultMsg = "naits.error.generalErrorWhileCreatingUser";
				log4j.error("Error in create external user:", e);
			} finally {
				if (svs != null) {
					svs.release();
				}
			}
		}
		return Response.status(200).entity(resultMsg).build();
	}

	@Path("/createUserFromAdmConsole/{sessionId}")
	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces("text/html;charset=utf-8")
	public Response createUserFromAdmConsole(@PathParam("sessionId") String sessionId,
			MultivaluedMap<String, String> formVals, @Context HttpServletRequest httpRequest) {
		String resultMsgLbl = "naits.error.admConsole.badJsonFormat";
		JsonObject jsonData = null;
		Gson gson = new Gson();
		Writer wr = new Writer();
		try {
			for (Entry<String, List<String>> entry : formVals.entrySet()) {
				if (entry.getKey() != null && !entry.getKey().isEmpty()) {
					String key = entry.getKey();
					jsonData = gson.fromJson(key, JsonObject.class);
				}
			}
			if (jsonData != null) {
				JsonElement jso = jsonData.get("data");
				if (jsonData.has("data")) {
					resultMsgLbl = wr.createUser(jso.getAsJsonObject(), sessionId);
				}
			}
		} catch (Exception e) {
			if (e instanceof SvException) {
				resultMsgLbl = ((SvException) e).getLabelCode();
				log4j.error("Error in creating user: " + ((SvException) e).getFormattedMessage(), e);
			} else
				resultMsgLbl = "naits.error.general";
			log4j.error("General error in creating user", e);
		}
		return Response.status(200).entity(resultMsgLbl).build();
	}

	private boolean sendActivationEmail(DbDataObject dboUser, HttpServletRequest httpRequest) throws SvException {
		String mailBody = I18n.getLongText("mail.body_activation");
		String feHost = "";
		String printParam = SvConf.getParam("print.jrxml_path");
		Properties rb = new Properties();
		rb.put("print.jrxml_path", printParam);
		if (rb.getProperty("frontend.gui_host") != null && rb.getProperty("frontend.gui_host").trim().length() > 4) {
			feHost = rb.getProperty("frontend.gui_host");
		} else {
			feHost = httpRequest.getScheme() + "s://" + httpRequest.getServerName();
		}
		String uri = feHost + "#/validate_user?action=ACTIVATE_USER&svToken=" + dboUser.getVal("USER_UID");
		HashMap<String, String> extParams = new HashMap<String, String>();
		extParams.put("{NAME}", dboUser.getVal(Tc.FIRST_NAME)
				+ (dboUser.getVal(Tc.LAST_NAME) != null ? " " + dboUser.getVal(Tc.LAST_NAME) : ""));
		extParams.put("{ACTIVATION_LINK}", uri);
		sendMail((String) dboUser.getVal(Tc.E_MAIL), I18n.getText("mail.body_activation"), mailBody, extParams);
		return true;
	}

	@Path("/activateUser/{user_uid}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response activateUser(@PathParam("user_uid") String userUid) {
		String retval = "";
		SvSecurity svs = null;
		try {
			svs = new SvSecurity();
			svs.activateExternalUser(userUid);
			retval = I18n.getText("user.activation_successful");
		} catch (Exception e) {
			retval = I18n.getText("user.activation_failed");
		} finally {
			if (svs != null)
				svs.release();
		}
		return Response.status(200).entity(retval).build();
	}

	void sendMail(String recipientAddress, String mailSubject, String mailBody, HashMap<String, String> extParams) {
		for (Entry<String, String> ent : extParams.entrySet()) {
			mailBody = mailBody.replace(ent.getKey(), ent.getValue());
		}
		// Sender's email ID needs to be mentioned
		String from = SvConf.getParam("mail.from").trim();
		final String username = SvConf.getParam("mail.username").trim();
		final String password = SvConf.getParam("mail.password").trim();

		// Assuming you are sending email through relay.jangosmtp.net
		String host = SvConf.getParam("mail.host").trim();

		Properties props = new Properties();
		props.put("mail.smtp.auth", SvConf.getParam("mail.smtp.auth").trim());
		props.put("mail.smtp.starttls.enable", SvConf.getParam("mail.smtp.starttls.enable").trim());
		props.put("mail.smtp.host", host);
		props.put("mail.smtp.port", SvConf.getParam("mail.smtp.port").trim());
		props.put("mail.smtp.ssl.trust", host);

		// Get the Session object.
		Session session = Session.getInstance(props, new javax.mail.Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(username, password);
			}
		});
		try {
			// Create a default MimeMessage object.
			MimeMessage message = new MimeMessage(session);
			// Set From: header field of the header.
			message.setFrom(new InternetAddress(from));
			// Set To: header field of the header.
			message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientAddress));
			String mailFormat = SvConf.getParam("mail.format") != null ? SvConf.getParam("mail.format").trim()
					: "text/html; charset=UTF-8";
			message.setHeader("Content-Type", "text/html; charset=UTF-8");
			// Set Subject: header field
			message.setSubject(mailSubject, "UTF-8");
			// Now set the actual message
			message.setContent(mailBody, mailFormat);
			// Send message
			Transport.send(message);
			// System.out.println("Sent message successfully....");
		} catch (Exception e) {
			log4j.error("Sending mail failed", e);
		}
	}

	/* validation of user and recover pass. */
	@Path("/recover_pass/{userName}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response recoverPass(@PathParam("userName") String userName, @Context HttpServletRequest httpRequest)
			throws Exception {
		String result = "naits.success.recoverPassword";
		cleanUpRecoveryData();
		DbDataObject dboUser = null;
		SvSecurity svs = new SvSecurity();
		dboUser = svs.getUser(userName);
		if (dboUser != null) {
			Object[] recoveryData = new Object[2];
			recoveryData[0] = UUID.randomUUID().toString();
			recoveryData[1] = new DateTime();
			passRecoveryTokens.put((String) dboUser.getVal(Tc.USER_NAME), recoveryData);
			dboUser.setVal(Tc.USER_UID, recoveryData);
			sendPassRecoveryEmail(dboUser, httpRequest);
		} else {
			result = "naits.error.user_not_found";
			return Response.status(200).entity(result).build();
		}
		return Response.status(200).entity(result).build();
	}

	private boolean sendPassRecoveryEmail(DbDataObject dboUser, HttpServletRequest httpRequest) throws SvException {
		String mailBody = I18n.getLongText("mail.body_pass_recovery");
		String feHost = "";
		String frontendGuiHost = SvConf.getParam("frontend.gui_host");
		if (frontendGuiHost != null && frontendGuiHost.trim().length() > 4) {
			feHost = frontendGuiHost;
		} else {
			feHost = httpRequest.getScheme() + "s://" + httpRequest.getServerName();
		}
		Object[] recoveryData = (Object[]) dboUser.getVal(Tc.USER_UID);
		String uri = feHost + "#/validate_user?action=PASS_RECOVERY&USER_NAME=" + dboUser.getVal(Tc.USER_NAME) + "&UID="
				+ recoveryData[0];
		/*
		 * if user is internal send flag for internal.jsp in gui no embg needed
		 * f.r
		 */
		if (dboUser.getVal(Tc.USER_TYPE).equals("INTERNAL") || dboUser.getVal(Tc.USER_TYPE).equals("ADM")
				|| dboUser.getVal(Tc.USER_TYPE).equals("BATCH")) {
			uri = feHost + "#/validate_user?action=PASS_RECOVERY&USER_NAME=" + dboUser.getVal(Tc.USER_NAME) + "&UID="
					+ recoveryData[0];
		}
		HashMap<String, String> extParams = new HashMap<String, String>();
		extParams.put("{NAME}", dboUser.getVal(Tc.FIRST_NAME)
				+ (dboUser.getVal(Tc.LAST_NAME) != null ? " " + dboUser.getVal(Tc.LAST_NAME) : ""));
		extParams.put("{RECOVERY_TOKEN}", uri);
		sendMail((String) dboUser.getVal(Tc.E_MAIL), I18n.getText("mail.subject_revocerpass"), mailBody, extParams);
		return true;
	}

	@Path("/getBasicUserData/{sessionid}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response getBasicUserData(@PathParam("sessionid") String sessionid) {
		String result = "";
		try {
			DbDataObject dboUser = null;
			dboUser = SvReader.getUserBySession(sessionid);
			if (dboUser == null) {
				result = "err.cannotGetBasicUserData.pleaseLoggoff";
				return Response.status(200).entity(I18n.getText(result)).build();
			}
			String userType = "";
			String userName = "";
			String userFirstName = "";
			String userLastName = "";
			String userEmail = "";
			String pin = "";

			if (dboUser.getVal(Tc.USER_NAME) != null)
				userName = dboUser.getVal(Tc.USER_NAME).toString();
			if (dboUser.getVal(Tc.USER_TYPE) != null)
				userType = dboUser.getVal(Tc.USER_TYPE).toString();
			if (dboUser.getVal(Tc.FIRST_NAME) != null)
				userFirstName = dboUser.getVal(Tc.FIRST_NAME).toString();
			if (dboUser.getVal(Tc.LAST_NAME) != null)
				userLastName = dboUser.getVal(Tc.LAST_NAME).toString();
			if (dboUser.getVal(Tc.E_MAIL) != null)
				userEmail = dboUser.getVal(Tc.E_MAIL).toString();
			if (dboUser.getVal(Tc.PIN) != null)
				pin = dboUser.getVal(Tc.PIN).toString();

			result = "userObjId:" + dboUser.getObject_id().toString() + ";userName:" + userName + ";userType:"
					+ userType + ";userFirstName:" + userFirstName + ";userLastName:" + userLastName + ";userEmail:"
					+ userEmail + ";pinNumber:" + pin;
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage(), e);
			return Response.status(200).entity(e.getFormattedMessage()).build();
		}
		return Response.status(200).entity(result).build();
	}

}
