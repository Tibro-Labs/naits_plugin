/*******************************************************************************
 * Copyright (c),  2017 TIBRO DOOEL Skopje
 *******************************************************************************/
package naits_triglav_plugin;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

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
import javax.ws.rs.core.StreamingOutput;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.joda.time.DateTime;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import com.prtech.svarog.I18n;
import com.prtech.svarog.SvConf;
import com.prtech.svarog.SvCore;
import com.prtech.svarog.SvException;
import com.prtech.svarog.SvGeometry;
import com.prtech.svarog.SvLink;
import com.prtech.svarog.SvLock;
import com.prtech.svarog.SvNote;
import com.prtech.svarog.SvNotification;
import com.prtech.svarog.SvReader;
import com.prtech.svarog.SvSecurity;
import com.prtech.svarog.SvWorkflow;
import com.prtech.svarog.SvWriter;
import com.prtech.svarog.svCONST;
import com.prtech.svarog_common.DbDataArray;
import com.prtech.svarog_common.DbDataObject;
import com.prtech.svarog_common.DbSearchCriterion;
import com.prtech.svarog_common.DbSearchCriterion.DbCompareOperand;
import com.prtech.svarog_common.DbSearchExpression;

import com.vividsolutions.jts.geom.Geometry;

/**
 * Class holding a services used for the NAITS system
 *
 * @author TIBRO_001
 *
 */

@Path("/naits_triglav_plugin/ApplicationServices")
public class ApplicationServices {
	/**
	 * Log4j instance used for logging
	 */
	static final Logger log4j = LogManager.getLogger(ApplicationServices.class.getName());

	static final boolean isInitialized = initPlugin();

	static boolean initPlugin() {
		OnSaveValidations call = new OnSaveValidations();
		SvCore.registerOnSaveCallback(call, SvReader.getTypeIdByName(Tc.ANIMAL));
		SvCore.registerOnSaveCallback(call, SvReader.getTypeIdByName(Tc.ANIMAL_MOVEMENT));
		SvCore.registerOnSaveCallback(call, SvReader.getTypeIdByName(Tc.HOLDING));
		SvCore.registerOnSaveCallback(call, SvReader.getTypeIdByName(Tc.HOLDING_RESPONSIBLE));
		SvCore.registerOnSaveCallback(call, SvReader.getTypeIdByName(Tc.FLOCK));
		SvCore.registerOnSaveCallback(call, SvReader.getTypeIdByName(Tc.FLOCK_MOVEMENT));
		SvCore.registerOnSaveCallback(call, SvReader.getTypeIdByName(Tc.OTHER_ANIMALS));
		SvCore.registerOnSaveCallback(call, SvReader.getTypeIdByName(Tc.RANGE));
		SvCore.registerOnSaveCallback(call, SvReader.getTypeIdByName(Tc.EXPORT_CERT));
		SvCore.registerOnSaveCallback(call, SvReader.getTypeIdByName(Tc.DISEASE));
		SvCore.registerOnSaveCallback(call, SvReader.getTypeIdByName(Tc.AREA_HEALTH));
		SvCore.registerOnSaveCallback(call, SvReader.getTypeIdByName(Tc.POST_SLAUGHT_FORM));
		SvCore.registerOnSaveCallback(call, SvReader.getTypeIdByName(Tc.PRE_SLAUGHT_FORM));
		SvCore.registerOnSaveCallback(call, SvReader.getTypeIdByName(Tc.SUPPLIER));
		SvCore.registerOnSaveCallback(call, SvReader.getTypeIdByName(Tc.TRANSFER));
		SvCore.registerOnSaveCallback(call, SvReader.getTypeIdByName(Tc.QUARANTINE));
		SvCore.registerOnSaveCallback(call, SvReader.getTypeIdByName(Tc.SPOT_CHECK));
		SvCore.registerOnSaveCallback(call, SvReader.getTypeIdByName(Tc.VACCINATION_BOOK));
		SvCore.registerOnSaveCallback(call, SvReader.getTypeIdByName(Tc.VACCINATION_RESULTS));
		SvCore.registerOnSaveCallback(call, SvReader.getTypeIdByName(Tc.VACCINATION_EVENT));
		SvCore.registerOnSaveCallback(call, SvReader.getTypeIdByName(Tc.EAR_TAG_REPLC));
		SvCore.registerOnSaveCallback(call, SvReader.getTypeIdByName(Tc.LAB_SAMPLE));
		SvCore.registerOnSaveCallback(call, SvReader.getTypeIdByName(Tc.LABORATORY));
		SvCore.registerOnSaveCallback(call, SvReader.getTypeIdByName(Tc.LAB_TEST_TYPE));
		SvCore.registerOnSaveCallback(call, SvReader.getTypeIdByName(Tc.LAB_TEST_RESULT));
		SvCore.registerOnSaveCallback(call, SvReader.getTypeIdByName(Tc.MOVEMENT_DOC));
		SvCore.registerOnSaveCallback(call, SvReader.getTypeIdByName(Tc.ORDER));
		SvCore.registerOnSaveCallback(call, SvReader.getTypeIdByName(Tc.PET));
		SvCore.registerOnSaveCallback(call, SvReader.getTypeIdByName(Tc.PET_HEALTH_BOOK));
		SvCore.registerOnSaveCallback(call, SvReader.getTypeIdByName(Tc.PET_PASSPORT));
		SvCore.registerOnSaveCallback(call, SvReader.getTypeIdByName(Tc.INVENTORY_ITEM));
		SvCore.registerOnSaveCallback(call, SvReader.getTypeIdByName(Tc.STRAY_PET));
		SvCore.registerOnSaveCallback(call, SvReader.getTypeIdByName(Tc.STRAY_PET_LOCATION));
		SvCore.registerOnSaveCallback(call, SvReader.getTypeIdByName(Tc.HEALTH_PASSPORT));
		SvCore.registerOnSaveCallback(call, SvReader.getTypeIdByName(Tc.POPULATION));
		SvCore.registerOnSaveCallback(call, SvReader.getTypeIdByName(Tc.RFID_INPUT));
		SvCore.registerOnSaveCallback(call, svCONST.OBJECT_TYPE_LINK);
		SvCore.registerOnSaveCallback(call, svCONST.OBJECT_TYPE_MESSAGE);
		SvCore.registerOnSaveCallback(call, svCONST.OBJECT_TYPE_NOTIFICATION);
		SvCore.registerOnSaveCallback(call, svCONST.OBJECT_TYPE_ORG_UNITS);
		SvCore.registerOnSaveCallback(call, svCONST.OBJECT_TYPE_USER);
		SvCore.registerOnSaveCallback(call, svCONST.OBJECT_TYPE_GROUP);
		return true;
	}

	@Path("/switchSessionLocale/{sessionId}/{localeId}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response switchSessionLocale(@PathParam("sessionId") String sessionId,
			@PathParam("localeId") String localeId, @Context HttpServletRequest httpRequest) {
		String resultMsgLbl = "switchUserLocale.sessionMissingError";
		SvReader svr = null;
		SvWriter svw = null;
		if (!sessionId.equals("undefined")) {
			try {
				svr = new SvReader(sessionId);
				svw = new SvWriter(svr);
				svw.dbSetAutoCommit(false);
				DbDataObject dboUser = SvReader.getUserBySession(sessionId);
				if (dboUser != null) {
					ReentrantLock lock = null;
					try {
						lock = SvLock.getLock(String.valueOf(dboUser.getObject_id()), false, 0);
						if (lock == null) {
							throw (new SvException(Tc.objectUsedByOtherSession, svr.getInstanceUser()));
						}
						DbDataObject userLocale = svr.getUserLocale(dboUser);
						if ((userLocale.getVal(Tc.LOCALE_ID) == null) || (userLocale.getVal(Tc.LOCALE_ID) != null
								&& !userLocale.getVal(Tc.LOCALE_ID).equals(localeId))) {
							svw.setUserLocale(dboUser.getVal(Tc.USER_NAME).toString(), localeId);
							svw.dbCommit();
							dboUser.setVal(Tc.LOCALE, localeId);
							dboUser.setIs_dirty(true);
							// force cache
							userLocale = svr.getUserLocale(dboUser);
							resultMsgLbl = "switchUserLocale.success";
						} else {
							resultMsgLbl = "switchUserLocale.localePerUserAlredySet";
						}
					} finally {
						if (lock != null && dboUser != null) {
							SvLock.releaseLock(String.valueOf(dboUser.getObject_id()), lock);
						}
					}
				}

			} catch (SvException e) {
				resultMsgLbl = e.getLabelCode();
				log4j.error("Error in switching user locale: " + e.getFormattedMessage(), e);
			} finally {
				if (svr != null) {
					svr.release();
				}
				if (svw != null) {
					svw.release();
				}
			}
		}
		return Response.status(200).entity(I18n.getText(resultMsgLbl)).build();
	}

	@Path("/checkIfShouldEnforcePassword/{sessionId}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response checkIfShouldEnforcePassword(@PathParam("sessionId") String sessionId,
			@Context HttpServletRequest httpRequest) {
		Boolean result = false;
		SvReader svr = null;
		if (!sessionId.equals("undefined")) {
			try {
				svr = new SvReader(sessionId);
				DbDataObject dboUser = SvReader.getUserBySession(sessionId);
				// if user has hash from default password, then enforce him to
				// change it
				// TODO:Put the default password and its hash into
				// svarog.properties file
				if (dboUser != null && dboUser.getVal(Tc.PASS_HSH) != null
						&& dboUser.getVal(Tc.PASS_HSH).equals("480FA232A3FDDFA31A5696DB829A90D7")) {
					result = true;
				}
			} catch (SvException e) {
				log4j.error("Error in checkIfShouldEnforcePassword: " + e.getFormattedMessage(), e);
			} finally {
				if (svr != null) {
					svr.release();
				}
			}
		}
		return Response.status(200).entity(result.toString()).build();
	}

	@Path("/massUserAction/{sessionId}/{actionName}/{subActionName}/{note}")
	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces("text/html;charset=utf-8")
	public Response massUserAction(@PathParam("sessionId") String sessionId, @PathParam("actionName") String actionName,
			@PathParam("subActionName") String subActionName, @PathParam("note") String actionNote,
			MultivaluedMap<String, String> formVals, @Context HttpServletRequest httpRequest) {
		String resultMsgLbl = "naits.error.massUserAction";
		JsonObject jsonData = null;
		Gson gson = new Gson();
		MassActions massAct = new MassActions();
		try {
			for (Entry<String, List<String>> entry : formVals.entrySet()) {
				if (entry.getKey() != null && !entry.getKey().isEmpty()) {
					String key = entry.getKey();
					jsonData = gson.fromJson(key, JsonObject.class);
				}
			}
			if (jsonData != null && jsonData.has(Tc.OBJ_ARRAY)) {
				massAct.userMassHandler(actionName, subActionName, actionNote,
						jsonData.get(Tc.OBJ_ARRAY).getAsJsonArray(), sessionId);
				resultMsgLbl = "naits.success.massUserAction" + actionName.toLowerCase();
			} else {
				resultMsgLbl = Tc.error_admConsoleBadJson;
			}
		} catch (Exception e) {
			resultMsgLbl = "naits.error.general";
			if (e instanceof SvException) {
				log4j.error("Error in processing mass action for: " + ((SvException) e).getFormattedMessage(), e);
			} else {
				log4j.error("General error in processing mass action", e);
			}

		}
		return Response.status(200).entity(resultMsgLbl).build();
	}

	@Path("/massTagChangeStatus/{sessionId}/{newStatus}")
	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces("text/html;charset=utf-8")
	public Response massTagChangeStatus(@PathParam("sessionId") String sessionId,
			@PathParam("newStatus") String newStatus, MultivaluedMap<String, String> formVals,
			@Context HttpServletRequest httpRequest) {
		String resultMsgLbl = "naits.error.massAnimalsAction";
		JsonObject jsonData = null;
		Gson gson = new Gson();
		MassActions massAct = new MassActions();
		try {
			for (Entry<String, List<String>> entry : formVals.entrySet()) {
				if (entry.getKey() != null && !entry.getKey().isEmpty()) {
					String key = entry.getKey();
					jsonData = gson.fromJson(key, JsonObject.class);
				}
			}
			if (jsonData != null && jsonData.has(Tc.OBJ_ARRAY)) {
				massAct.inventoryItemsMassStatusChange(newStatus, jsonData.get(Tc.OBJ_ARRAY).getAsJsonArray(),
						sessionId);
				resultMsgLbl = "naits.success.massTagChangeStatus";
			} else {
				resultMsgLbl = Tc.error_admConsoleBadJson;
			}
		} catch (Exception e) {
			resultMsgLbl = "naits.error.general";
			if (e instanceof SvException) {
				log4j.error("Error in processing mass action for: " + ((SvException) e).getFormattedMessage(), e);
			} else {
				log4j.error("General error in processing mass action", e);
			}

		}
		return Response.status(200).entity(resultMsgLbl).build();
	}

	@Path("/saveContactData2/{sessionId}/{parentIdForContact}/{parentIdObjectName}")
	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces("text/html;charset=utf-8")
	public Response saveContactData2(@PathParam("sessionId") String sessionId,
			@PathParam("parentIdForContact") Long parentIdForContact,
			@PathParam("parentIdObjectName") String parentIdObjectName, MultivaluedMap<String, String> formVals,
			@Context HttpServletRequest httpRequest) {
		String resultMsgLbl = "naits.success.saveContactData";
		SvReader svr = null;
		SvWriter svw = null;
		DbDataObject dboUser = null;
		JsonObject jsonData = null;
		Gson gson = new Gson();
		Writer wr = null;
		try {
			svr = new SvReader(sessionId);
			svw = new SvWriter(svr);
			wr = new Writer();
			dboUser = SvReader.getUserBySession(sessionId);

			for (Entry<String, List<String>> entry : formVals.entrySet()) {
				if (entry.getKey() != null && !entry.getKey().isEmpty()) {
					String key = entry.getKey();
					jsonData = gson.fromJson(key, JsonObject.class);
				}
			}
			DbDataObject parentObj = svr.getObjectById(parentIdForContact, SvReader.getTypeIdByName(parentIdObjectName),
					null);
			if (parentObj == null || parentObj.getObject_id() == 0L) {
				throw (new SvException("naits.error.parentIdObjectNameNotExist", svr.getInstanceUser()));
			}
			ReentrantLock lock = null;
			try {
				lock = SvLock.getLock(String.valueOf(parentObj.getObject_id()), false, 0);
				if (lock == null) {
					throw (new SvException(Tc.objectUsedByOtherSession, svr.getInstanceUser()));
				}
				// check if other contact data exist for the same parent
				DbDataArray existingContactData = svr.getObjectsByParentId(parentObj.getObject_id(),
						svCONST.OBJECT_TYPE_CONTACT_DATA, null, 0, 0);
				// if exists invalidate it
				if (!existingContactData.getItems().isEmpty()) {
					for (DbDataObject tempDbo : existingContactData.getItems())
						svw.deleteObject(tempDbo, false);
				}
				wr.saveContactData2(parentObj.getObject_id(), jsonData, svw);
				if (jsonData != null && jsonData.has("e_mail"))
					wr.updateUserMail(jsonData.get("e_mail").getAsString(), dboUser, svw);
				dboUser = svr.getObjectById(dboUser.getObject_id(), svCONST.OBJECT_TYPE_USER, new DateTime());
				svw.dbCommit();
			} finally {
				if (lock != null) {
					SvLock.releaseLock(String.valueOf(parentObj.getObject_id()), lock);
				}
			}
		} catch (SvException e) {
			resultMsgLbl = "naits.error.saveContactData";
			log4j.error("Error in saving contact data for object: ", e);
		} finally {
			if (svr != null) {
				svr.release();
			}
			if (svw != null) {
				svw.release();
			}
		}
		return Response.status(200).entity(resultMsgLbl).build();
	}

	@Path("/getFullUserData/{sessionid}/{objectId}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response getFullUserData(@PathParam("sessionid") String sessionid, @PathParam("objectId") Long objectId) {
		String result = Tc.EMPTY_STRING;
		SvReader svr = null;
		DbDataObject contactObj = null;
		DbDataObject dboUser = null;
		try {
			svr = new SvReader(sessionid);
			Reader r = new Reader();
			dboUser = SvReader.getUserBySession(sessionid);
			DbDataArray dboContactData = svr.getObjectsByParentId(objectId, svCONST.OBJECT_TYPE_CONTACT_DATA, null, 0,
					0);
			if (!dboContactData.getItems().isEmpty()) {
				contactObj = dboContactData.get(0);
			}
			result = r.getUserFullData(dboUser, contactObj);

		} catch (SvException e) {
			log4j.error(e.getFormattedMessage(), e);
			return Response.status(200).entity(e.getFormattedMessage()).build();
		} finally {
			if (svr != null)
				svr.release();
		}
		return Response.status(200).entity(result).build();
	}

	@Path("/editUserData/{sessionId}/{firstName}/{lastName}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response editUserData(@PathParam("sessionId") String sessionId, @PathParam("firstName") String firstName,
			@PathParam("lastName") String lastName) {
		String resultMsgLbl = "naits.error.editUserInfo";
		Writer wr = null;
		try {
			wr = new Writer();
			DbDataObject dboUser = SvReader.getUserBySession(sessionId);
			if (dboUser == null) {
				resultMsgLbl = "naits.error.cannotGetBasicUserData.pleaseLoggoff";
				return Response.status(200).entity(resultMsgLbl).build();
			}
			wr.editUserData(firstName, lastName, dboUser, sessionId);
			resultMsgLbl = "naits.success.editUserInfo";
		} catch (SvException e) {
			log4j.error(Tc.ERROR_DETECTED + e.getFormattedMessage(), e);
		}
		return Response.status(200).entity(resultMsgLbl).build();
	}

	@Path("/getAllowedCustomObjectsPerUser/{sessionId}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response getAllowedCustomObjectsPerUser(@PathParam("sessionId") String sessionId) {
		SvReader svr = null;
		DbDataArray allowedCustomObjects = new DbDataArray();
		String result = null;
		UserManager um = null;
		ArrayList<String> groupNames = new ArrayList<String>();
		groupNames.add(Tc.FVIRO);
		groupNames.add(Tc.CVIRO);
		groupNames.add(Tc.LABORANT);
		groupNames.add(Tc.PRIVATE_VETERINARIANS);
		groupNames.add(Tc.PET_VETERINARIANS);
		groupNames.add(Tc.HOLDING_ADMINISTRATORS);

		try {
			svr = new SvReader(sessionId);
			um = new UserManager();

			DbDataObject dboUser = SvReader.getUserBySession(sessionId);
			HashMap<SvCore.SvAclKey, HashMap<String, DbDataObject>> allPermissions = svr.getPermissions();
			Iterator<Map.Entry<SvCore.SvAclKey, HashMap<String, DbDataObject>>> it = allPermissions.entrySet()
					.iterator();
			boolean specialCaseFVIROuser = false;
			boolean specialCaseCVIROuser = false;
			boolean specialCaseLaborantUser = false;
			boolean specialCasePrivateVetUser = false;
			boolean specialCaseAnimalShelterUser = false;
			boolean specialCaseHoldingAdministratorUser = false;
			boolean isMovementDocumentSearchAllowed = um.checkIfUserHasCustomPermission(dboUser,
					Tc.CUSTOM_MOVEMENT_DOC_SEARCH_PERMISSION, svr);
			boolean isAnimalReadOnlySearchAllowed = um.checkIfUserHasCustomPermission(dboUser,
					Tc.CUSTOM_ANIMAL_SEARCH_PERMISSION, svr);
			boolean hasRFIDSearchCustomPermission = um.checkIfUserHasCustomPermission(dboUser,
					Tc.CUSTOM_RFID_SEARCH_PERMISSION, svr);
			boolean hasReadInventoryItemPermission = um.checkIfUserHasCustomPermission(dboUser, Tc.INVENTORY_ITEM,
					Tc.READ, svr);
			boolean hasFullInventoryItemPermission = um.checkIfUserHasCustomPermission(dboUser, Tc.INVENTORY_ITEM,
					Tc.FULL, svr);

			ArrayList<String> matchedUserGroups = um.checkIfUserLinkedToSomeDefaultGroups(dboUser, groupNames, svr);
			if (matchedUserGroups.contains(Tc.FVIRO)) {
				specialCaseFVIROuser = true;
			} else if (matchedUserGroups.contains(Tc.CVIRO)) {
				specialCaseCVIROuser = true;
			} else if (matchedUserGroups.contains(Tc.LABORANT)) {
				specialCaseLaborantUser = true;
			} else if (matchedUserGroups.contains(Tc.PRIVATE_VETERINARIANS)) {
				specialCasePrivateVetUser = true;
			} else if (matchedUserGroups.contains(Tc.PET_VETERINARIANS)) {
				specialCaseAnimalShelterUser = true;
			} else if (matchedUserGroups.contains(Tc.HOLDING_ADMINISTRATORS)) {
				specialCaseHoldingAdministratorUser = true;
			}
			while (it.hasNext()) {
				Map.Entry<SvCore.SvAclKey, HashMap<String, DbDataObject>> pair = it.next();
				SvCore.SvAclKey tempAllowedObject = pair.getKey();
				Long tempObjId = tempAllowedObject.getObjectId();
				String tableName = Tc.EMPTY_STRING;
				if (tempObjId != null) {
					DbDataObject dboTable = svr.getObjectById(tempObjId, svCONST.OBJECT_TYPE_TABLE, null);
					if (dboTable != null && dboTable.getVal(Tc.TABLE_NAME) != null) {
						tableName = dboTable.getVal(Tc.TABLE_NAME).toString().toUpperCase();
						if (!tableName.startsWith(Tc.SVAROG)) {
							if ((specialCaseFVIROuser || specialCaseCVIROuser) && (Arrays
									.asList(Tc.LAB_SAMPLE, Tc.VACCINATION_EVENT, Tc.HOLDING, Tc.ANIMAL, Tc.LABORATORY)
									.contains(tableName))) {
								allowedCustomObjects.addDataItem(dboTable);
							} else if (specialCaseLaborantUser && (Arrays.asList(Tc.LABORATORY).contains(tableName))) {
								allowedCustomObjects.addDataItem(dboTable);
							} else if (specialCasePrivateVetUser
									&& (Arrays.asList(Tc.HOLDING, Tc.ANIMAL).contains(tableName))) {
								allowedCustomObjects.addDataItem(dboTable);
							} else if (isAnimalReadOnlySearchAllowed && tableName.equals(Tc.ANIMAL)) {
								allowedCustomObjects.addDataItem(dboTable);
							} else if ((hasRFIDSearchCustomPermission || specialCaseFVIROuser)
									&& tableName.equals(Tc.RFID_INPUT)) {
								allowedCustomObjects.addDataItem(dboTable);
							} else if ((specialCaseHoldingAdministratorUser || isMovementDocumentSearchAllowed)
									&& tableName.equals(Tc.MOVEMENT_DOC)) {
								allowedCustomObjects.addDataItem(dboTable);
							} else if ((!specialCaseFVIROuser && !specialCaseCVIROuser && !specialCaseLaborantUser
									&& !specialCasePrivateVetUser && !isAnimalReadOnlySearchAllowed)
									&& (Arrays
											.asList(Tc.HOLDING, Tc.ANIMAL, Tc.HOLDING_RESPONSIBLE, Tc.VACCINATION_EVENT,
													Tc.QUARANTINE, Tc.AREA, Tc.POPULATION, Tc.INVENTORY_ITEM,
													Tc.EXPORT_CERT, Tc.LAB_SAMPLE)
											.contains(tableName)
											|| (specialCaseAnimalShelterUser && tableName.equals(Tc.PET)))) {
								allowedCustomObjects.addDataItem(dboTable);
							}
						} else if ((!specialCaseFVIROuser && !specialCaseCVIROuser && !specialCaseLaborantUser
								&& !specialCasePrivateVetUser && !isAnimalReadOnlySearchAllowed)
								&& tableName.contains(Tc.SVAROG_ORG_UNITS)
								&& (hasReadInventoryItemPermission || hasFullInventoryItemPermission)) {
							allowedCustomObjects.addDataItem(dboTable);
						}
					}
				}
			}
			result = allowedCustomObjects.toJson().toString();
		} catch (SvException e) {
			log4j.error(Tc.ERROR_DETECTED + e.getFormattedMessage(), e);
			return Response.status(200).entity(e.getFormattedMessage()).build();
		} finally {
			if (svr != null)
				svr.release();
		}
		return Response.status(200).entity(result).build();
	}

	@Path("/getNotificationsPerUser/{sessionId}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response getNotificationsPerUser(@PathParam("sessionId") String sessionId) {
		SvNotification svnf = null;
		DbDataArray notificationsPerUser = new DbDataArray();
		DbDataArray tempNotificationsPerUser = new DbDataArray();
		String result = null;
		try {
			svnf = new SvNotification(sessionId);
			DbDataObject userObj = SvReader.getUserBySession(sessionId);
			if (userObj != null && userObj.getObject_id() != 0L) {
				notificationsPerUser = svnf.getNotificationPerUser(userObj);
				for (DbDataObject dboNotification : notificationsPerUser.getItems()) {
					if (dboNotification.getVal(Tc.VALID_TO) != null) {
						DateTime tempDate = new DateTime(dboNotification.getVal(Tc.VALID_TO).toString());
						dboNotification.setVal(Tc.VALID_TO, tempDate.plusDays(1));
						tempNotificationsPerUser.addDataItem(dboNotification);
					}
				}
				notificationsPerUser = new DbDataArray();
				if (!tempNotificationsPerUser.getItems().isEmpty()) {
					notificationsPerUser = svnf.getValidNotifications(tempNotificationsPerUser);
				}
				result = notificationsPerUser.toJson().toString();
			}
		} catch (SvException e) {
			log4j.error(Tc.ERROR_DETECTED + e.getFormattedMessage(), e);
			return Response.status(200).entity(e.getFormattedMessage()).build();
		} finally {
			if (svnf != null)
				svnf.release();
		}
		return Response.status(200).entity(result).build();
	}

	@Path("/checkIfUserHasAdmGroup/{sessionId}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response checkIfUserHasAdmGroup(@PathParam("sessionId") String sessionId) {
		String result = "false";
		DbDataObject userObj;
		SvReader svr = null;
		UserManager userMng = null;
		try {
			userObj = SvReader.getUserBySession(sessionId);
			svr = new SvReader(sessionId);
			userMng = new UserManager();
			if (userObj != null && userObj.getObject_id() != 0L && userObj.getVal(Tc.USER_NAME) != null
					&& userMng.checkIfUserHasAdmGroup(userObj, svr)) {
				result = "true";
			}
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
		} finally {
			if (svr != null) {
				svr.release();
			}
		}

		return Response.status(200).entity(result).build();
	}

	@Path("/getUserGroups/{sessionId}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response getUserGroups(@PathParam("sessionId") String sessionId) {
		String result = Tc.EMPTY_STRING;
		DbDataObject dboUser;
		SvReader svr = null;
		try {
			dboUser = SvReader.getUserBySession(sessionId);
			svr = new SvReader(sessionId);
			DbDataArray allDefaultGroups = svr.getAllUserGroups(dboUser, false);
			for (DbDataObject tempUserGroup : allDefaultGroups.getItems()) {
				result = result + tempUserGroup.getVal(Tc.GROUP_NAME).toString() + ",";
			}
			result = result.replaceAll(",$", "");
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
		} finally {
			if (svr != null) {
				svr.release();
			}
		}

		return Response.status(200).entity(result).build();
	}

	@Path("/getPicPerHolding/{sessionId}/{holdingObjId}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response getPicPerHolding(@PathParam("sessionId") String sessionId,
			@PathParam("holdingObjId") Long holdingObjId) {
		SvReader svr = null;
		Reader rdr = new Reader();
		String result = Tc.EMPTY_STRING;
		try {
			svr = new SvReader(sessionId);
			DbDataObject dboHolding = rdr.getHoldingObjectByObjectId(holdingObjId, svr);
			if (dboHolding.getVal(Tc.PIC) != null)
				result = dboHolding.toSimpleJson().toString();
		} catch (SvException e) {
			log4j.error(Tc.ERROR_DETECTED + e.getFormattedMessage(), e);
			result = e.getFormattedMessage();
		} finally {
			if (svr != null)
				svr.release();
		}
		return Response.status(200).entity(result).build();
	}

	@Path("/searchForObject/{sessionId}/{searchStr}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response globalSearchForObject(@PathParam("sessionId") String sessionId,
			@PathParam("searchStr") String searchStr) {
		String result = null;
		Reader r = null;
		SvReader svr = null;
		DbDataObject animalExist = null;
		DbDataObject premiseExist = null;
		if (searchStr == null || searchStr.trim().length() == 0) {
			log4j.error("The search string in globalSearchForObject is null or empty.");
		}
		r = new Reader();
		try {
			svr = new SvReader(sessionId);
			String animalRegEx = "^[0-9]{8}$";
			String premiseRegEx = "^[0-9]{11}$";
			if (searchStr != null && searchStr.matches(animalRegEx)) {
				animalExist = r.findAppropriateAnimalByAnimalId(searchStr, svr);
				if (animalExist != null) {
					result = "ANIMAL:" + animalExist.toJson().toString();
				}
			}
			if (searchStr != null && searchStr.matches(premiseRegEx)) {
				premiseExist = r.findAppropriateHoldingByPic(searchStr, svr);
				if (premiseExist != null) {
					result = "HOLDING:" + premiseExist.toJson().toString();
				}
			}
		} catch (SvException e) {
			log4j.error("Error in globalSearchForObject:" + e.getFormattedMessage(), e);
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
		return Response.status(200).entity(result).build();
	}

	@Path("/prepareReactJsonUISchema/{sessionId}/{reactComponent}/{parentId}/{parentTypeId}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response prepareReactJsonUISchema(@PathParam("sessionId") String sessionId,
			@PathParam("reactComponent") String reactComponent, @PathParam("parentId") Long parentId,
			@PathParam("parentTypeId") Long parentTypeId) {
		String result = null;
		SvReader svr = null;
		try {
			svr = new SvReader(sessionId);
			ReactJsonBuilder cjb = new ReactJsonBuilder();
			DbDataArray allYNDocuments = svr.getObjectsByParentId(parentTypeId, svCONST.OBJECT_TYPE_FORM_TYPE, null, 0,
					0);
			result = cjb.prepareReactJsonUISchema("DROPDOWN", allYNDocuments);
		} catch (SvException e) {
			log4j.error("Error in globalSearchForObject:" + e.getFormattedMessage(), e);
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
		return Response.status(200).entity(result).build();
	}

	@Path("/prepareReactJsonFormData/{sessionId}/{reactComponent}/{parentId}/{parentTypeId}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response prepareReactJsonFormData(@PathParam("sessionId") String sessionId,
			@PathParam("reactComponent") String reactComponent, @PathParam("parentId") Long parentId,
			@PathParam("parentTypeId") Long parentTypeId) {
		String result = null;
		SvReader svr = null;
		try {
			svr = new SvReader(sessionId);
			ReactJsonBuilder cjb = new ReactJsonBuilder();
			DbDataArray allYNDocuments = svr.getObjectsByParentId(parentTypeId, svCONST.OBJECT_TYPE_FORM_TYPE, null, 0,
					0);
			DbDataArray formVals = new DbDataArray();
			for (DbDataObject tempForm : allYNDocuments.getItems()) {
				DbDataArray tempFormVals = svr.getFormsByParentId(parentId, tempForm.getObject_id(), null, null);
				if (tempFormVals != null && !tempFormVals.getItems().isEmpty()) {
					formVals.addDataItem(tempFormVals.getItems().get(0));
				}
			}
			result = cjb.prepareReactJsonFormData("DROPDOWN", formVals);
		} catch (SvException e) {
			log4j.error("Error in globalSearchForObject:" + e.getFormattedMessage(), e);
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
		return Response.status(200).entity(result).build();
	}

	@Path("/translateCodeItem/{sessionId}/{tableId}/{fieldName}/{value}/{localeId}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response translateCodeItem(@PathParam("sessionId") String sessionId, @PathParam("tableId") Long tableId,
			@PathParam("fieldName") String fieldName, @PathParam("value") String value,
			@PathParam("localeId") String localeId) {
		String result = "N/A";
		SvReader svr = null;
		try {
			svr = new SvReader(sessionId);
			Reader r = new Reader();
			result = r.decodeCodeValue(tableId, fieldName, value, localeId, svr);
		} catch (SvException e) {
			log4j.error("Error occured in translateCodeItem:" + e.getFormattedMessage(), e);
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
		return Response.status(200).entity(result).build();
	}

	@Path("/getHoldingKeeperInfo/{sessionId}/{holdingId}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response getHoldingKeeperInfo(@PathParam("sessionId") String sessionId,
			@PathParam("holdingId") Long holdingId) {
		String result = null;
		SvReader svr = null;
		try {
			svr = new SvReader(sessionId);
			Reader r = new Reader();
			// rdr instance of this service can be additionally completed,
			// depending on the client requirements
			result = r.getHoldingKeeperInfo(holdingId, svr);
		} catch (SvException e) {
			log4j.error("Error occured in translateCodeItem:" + e.getFormattedMessage(), e);
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
		return Response.status(200).entity(result).build();
	}

	@Path("/checkIfAnimalIdExist/{sessionId}/{animalObjId}/{animalTagId}/{animalClass}/{holdingId}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response checkIfAnimalIdExist(@PathParam("sessionId") String sessionId,
			@PathParam("animalObjId") Long animalObjId, @PathParam("animalTagId") String animalTagId,
			@PathParam("animalClass") String animalClass, @PathParam("holdingId") Long holdingId) {
		String resultMsgLbl = Tc.EMPTY_STRING;
		String animalAndSourceHoldingInfo = Tc.EMPTY_STRING;
		SvReader svr = null;
		Reader rdr = null;
		ValidationChecks vc = null;
		try {
			svr = new SvReader(sessionId);
			rdr = new Reader();
			vc = new ValidationChecks();
			String localeId = svr.getUserLocaleId(svr.getInstanceUser());
			// rdr instance of this service can be additionally completed,
			// depending on the client requirements
			if (animalTagId != null && animalClass != null) {
				DbDataObject animalFound = rdr.findAppropriateAnimalByAnimalIdAndAnimalClass(animalTagId, animalClass,
						true, svr);
				if (animalFound != null) {
					DbDataObject dboHolding = svr.getObjectById(animalFound.getParent_id(),
							SvReader.getTypeIdByName(Tc.HOLDING), null);
					animalAndSourceHoldingInfo = rdr.getAnimalAndSourceHoldingObjInfo(animalFound, localeId, svr,
							Tc.GENDER, Tc.ANIMAL_RACE, Tc.COLOR);

					// check only for newly registered animals
					if (animalObjId.equals(0L)) {
						if (animalFound.getParent_id().equals(holdingId)) {
							throw (new SvException("naits.error.animalAlreadyExistsInYourHolding",
									svr.getInstanceUser()));
						} else if (animalFound.getStatus().equals(Tc.VALID)) {
							resultMsgLbl = I18n.getText(localeId, "naits.system.theAnimalAlreadyRegistered") + " "
									+ animalAndSourceHoldingInfo;
						} else if (animalFound.getStatus().equals(Tc.EXPORTED)
								|| animalFound.getStatus().equals(Tc.PENDING_EX)) {
							throw (new SvException("naits.error.animalsWithStatusExportedOrPendingExport",
									svr.getInstanceUser()));
						} else if (animalFound.getStatus().equals(Tc.TRANSITION)) {
							throw (new SvException("naits.error.animalHasPendingMovement", svr.getInstanceUser()));
						} else if (dboHolding != null && vc.checkIfHoldingIsSlaughterhouse(dboHolding)
								&& (animalFound.getStatus().equals(Tc.SLAUGHTRD)
										|| animalFound.getStatus().equals(Tc.PREMORTEM)
										|| animalFound.getStatus().equals(Tc.POSTMORTEM)
										|| animalFound.getStatus().equals(Tc.DESTROYED))) {
							throw (new SvException(
									"naits.error.animalsThatBelongToSlaughterHouseAndStatusIsInvalidForDirectTransfer",
									svr.getInstanceUser()));
						} else if (animalFound.getStatus().equals(Tc.LOST) || animalFound.getStatus().equals(Tc.DIED)
								|| animalFound.getStatus().equals(Tc.SLAUGHTRD)
								|| animalFound.getStatus().equals(Tc.ABSENT)
								|| animalFound.getStatus().equals(Tc.SOLD)) {
							resultMsgLbl = I18n.getText(localeId, "naits.error.animalIsInactive") + " "
									+ animalAndSourceHoldingInfo;
						}
					}
				}
			}
		} catch (SvException e) {
			resultMsgLbl = e.getLabelCode();
			log4j.error("Error occured in checkIfAnimalIdExist:" + e.getFormattedMessage(), e);
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
		return Response.status(200).entity(resultMsgLbl).build();
	}

	@Path("/calcAnimalAge/{sessionId}/{animalId}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response calcAnimalAge(@PathParam("sessionId") String sessionId, @PathParam("animalId") String animalObjId) {
		String result = Tc.EMPTY_STRING;
		SvReader svr = null;
		try {
			svr = new SvReader(sessionId);
			Reader r = new Reader();
			// rdr instance of this service can be additionally completed,
			// depending on the client requirements
			if (animalObjId != null) {
				result = r.calcAnimalAge(Long.valueOf(animalObjId), svr);
			}
		} catch (SvException e) {
			log4j.error("Error occured in checkIfAnimalIdExist:" + e.getFormattedMessage(), e);
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
		return Response.status(200).entity(result).build();
	}

	@Path("/transferAnimalOrFlockToHolding/{sessionId}")
	@POST
	@Produces("text/html;charset=utf-8")
	public Response transferAnimalOrFlockToHolding(@PathParam("sessionId") String sessionId,
			@PathParam("parentId") Long parentId, MultivaluedMap<String, String> formVals) {
		String result = "naits.error.errorOccurredWhileExecutingTransferAnimalOrFlockToHolding";
		Writer wr = null;
		Gson gson = null;
		JsonObject jObj = null;
		try {
			wr = new Writer();
			gson = new Gson();
			for (Entry<String, List<String>> entry : formVals.entrySet()) {
				if (entry.getKey() != null && !entry.getKey().isEmpty()) {
					String key = entry.getKey();
					jObj = gson.fromJson(key, JsonObject.class);
				}
			}
			result = wr.moveAnimalOrFlockViaDirectTransfer(jObj, sessionId);
		} catch (Exception e) {
			log4j.error(e.getMessage());
		}
		return Response.status(200).entity(result).build();
	}

	@Path("/transferAnimalOrFlockToHolding/{sessionId}/{animalOrFlockId}/{animalClass}/{destinationHoldingObjId}/{dateOfAdmission}/{transporterPersonId}/{totalUnits}/{maleUnits}/{femaleUnits}/{adultsUnits}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response transferAnimalOrFlockToHolding(@PathParam("sessionId") String sessionId,
			@PathParam("animalOrFlockId") String animalOrFlockId, @PathParam("animalClass") String animalClass,
			@PathParam("destinationHoldingObjId") String destinationHoldingObjId,
			@PathParam("dateOfAdmission") String dateOfAdmission,
			@PathParam("transporterPersonId") String transporterPersonId, @PathParam("totalUnits") Long totalUnits,
			@PathParam("maleUnits") Long maleUnits, @PathParam("femaleUnits") Long femaleUnits,
			@PathParam("adultsUnits") Long adultsUnits) throws InterruptedException, SvException {
		String resultMsgLabel = "naits.error.invalidInputInformationsForDirectTransfer";
		SvReader svr = null;
		SvWriter svw = null;
		SvWorkflow sww = null;
		ValidationChecks vc = null;
		Reader rdr = null;
		Writer wr = null;
		MassActions ma = null;
		try {
			svr = new SvReader(sessionId);
			svw = new SvWriter(svr);
			sww = new SvWorkflow(svw);
			wr = new Writer();
			vc = new ValidationChecks();
			rdr = new Reader();
			ma = new MassActions();
			String blockCheck = SvConf.getParam("app_block.disable_animal_check");
			// PublicRegistry pr = new PublicRegistry();
			String flockFlag = Tc.EMPTY_STRING;
			// rdr instance of this service can be additionally completed,
			// depending on the client requirements
			String movementType = Tc.ANIMAL_MOVEMENT_HOLDING;
			if (animalOrFlockId != null && destinationHoldingObjId != null && dateOfAdmission != null
					&& animalClass != null && transporterPersonId != null) {
				if (!dateOfAdmission.equalsIgnoreCase("null")
						&& new DateTime(dateOfAdmission).isAfter(new DateTime())) {
					throw (new SvException("naits.error.addmitanceDateCannotBeInTheFuture", svCONST.systemUser, null,
							null));
				}
				ArrayList<String> mandatoryFields = new ArrayList<>();
				DbDataObject dboToMove = rdr.findAppropriateAnimalByAnimalIdAndAnimalClass(animalOrFlockId, animalClass,
						true, svr);
				if (dboToMove != null) {
					if (vc.isInventoryItemCheckBlocked(dboToMove)) {
						DbDataObject dboInventoryItem = rdr.getInventoryItem(dboToMove, Tc.APPLIED, true, svr);
						if (blockCheck == null && dboInventoryItem == null) {
							throw (new SvException("naits.error.theAnimalYouHaveSelectedDoesntHaveValidInventoryItem",
									svr.getInstanceUser()));
						}
					}
				} else if (dboToMove == null) {
					dboToMove = rdr.findAppropriateFlockByFlockId(animalOrFlockId, svr);
					flockFlag = "Flock";
					movementType = Tc.FLOCK_MOVEMENT_HOLDING;
				}
				dboToMove.setVal(Tc.CHECK_COLUMN, true);
				dboToMove.setVal(Tc.AUTO_GENERATED, false);
				DbDataObject dboMovement = null;
				DbDataObject dboMovementDoc = null;
				resultMsgLabel = "naits.error.transfer.fail.anmalIdDoesNotExist";
				if (dboToMove != null) {
					ReentrantLock lock = null;
					try {
						lock = SvLock.getLock(String.valueOf(dboToMove.getObject_id()), false, 0);
						if (lock == null) {
							throw (new SvException(Tc.objectUsedByOtherSession, svr.getInstanceUser()));
						}
						/*
						 * if (transporterPersonId != null && NumberUtils.isNumber(transporterPersonId))
						 * { // Long holdingResponsibleObjId = // Long.valueOf(transporterPersonId);
						 * DbDataObject dboHoldingResponsible =
						 * svr.getObjectById(holdingResponsibleObjId,
						 * SvReader.getTypeIdByName(Tc.HOLDING_RESPONSIBLE), null); if
						 * (dboHoldingResponsible != null &&
						 * !pr.publicRegistryCheck(dboHoldingResponsible)) { throw (new SvException(
						 * "naits.error.invalidPersonInformation", svCONST.systemUser, null, null));
						 * 
						 * } svw.saveObject(dboHoldingResponsible, false); }
						 */
						if (dboToMove.getStatus().equals(Tc.TRANSITION)) {
							throw (new SvException("naits.error.selectedAnimalIsInTransitionSoCantBeDirectTransfered",
									svr.getInstanceUser()));
						}
						if (dboToMove != null && dboToMove.getStatus().equals(Tc.VALID)) {
							Boolean isCountryGeorgia = vc.checkIfMovementIsAllowedPerAnimalCountry(dboToMove, svr);
							if (!isCountryGeorgia) {
								throw (new SvException("naits.error.animalWithDifferentCountryThanGeorgiaCannotBeMoved",
										svr.getInstanceUser()));
							}
						}
						if (dboToMove.getStatus().equals(Tc.EXPORTED) || dboToMove.getStatus().equals(Tc.PENDING_EX)) {
							throw (new SvException("naits.error.animalsWithStatusExportedOrPendingExport",
									svr.getInstanceUser()));
						}
						if (vc.checkIfHoldingIsSlaughterhouse(dboToMove.getParent_id(), svr)
								&& (dboToMove.getStatus().equals(Tc.SLAUGHTRD)
										|| dboToMove.getStatus().equals(Tc.PREMORTEM)
										|| dboToMove.getStatus().equals(Tc.POSTMORTEM)
										|| dboToMove.getStatus().equals(Tc.DESTROYED))) {
							throw (new SvException(
									"naits.error.animalsThatBelongToSlaughterHouseAndStatusIsInvalidForDirectTransfer",
									svr.getInstanceUser()));
						}

						DbDataObject dboDestinationHolding = svr.getObjectById(Long.valueOf(destinationHoldingObjId),
								SvReader.getTypeIdByName(Tc.HOLDING), null);
						DbDataObject dboSourceHolding = svr.getObjectById(dboToMove.getParent_id(),
								SvReader.getTypeIdByName(Tc.HOLDING), null);
						if (dboToMove.getStatus().equals(Tc.LOST) || dboToMove.getStatus().equals(Tc.SOLD)) {
							DbDataArray arrMovements = rdr.getExistingAnimalOrFlockMovements(dboToMove, null, Tc.VALID,
									true, true, svr);
							if (arrMovements != null && !arrMovements.getItems().isEmpty()) {
								DbDataObject dboAnimalOrFlockMovement = arrMovements.get(0);
								dboToMove.setParent_id(dboDestinationHolding.getObject_id());
								dboToMove.setStatus(Tc.VALID);
								svw.saveObject(dboToMove, false);
								dboAnimalOrFlockMovement.setStatus(Tc.FINISHED);
								dboAnimalOrFlockMovement.setVal(Tc.DESTINATION_HOLDING_ID,
										dboDestinationHolding.getVal(Tc.PIC).toString());
								dboAnimalOrFlockMovement.setVal(Tc.ARRIVAL_DATE, new DateTime());
								wr.linkObjects(dboDestinationHolding, dboAnimalOrFlockMovement, movementType,
										"linked via direct_transfer WS", svw);
								svw.saveObject(dboAnimalOrFlockMovement, false);
								resultMsgLabel = "naits.success.transfer";
								svw.dbCommit();
							}
						} else {
							if (dboDestinationHolding != null) {
								dboMovementDoc = wr.createMovementDocument(dboToMove.getParent_id(), svr);
								if (dboMovementDoc != null) {
									if (flockFlag.equalsIgnoreCase(Tc.FLOCK)) {
										dboMovementDoc.setVal(Tc.MOVEMENT_TYPE, Tc.FLOCK);
									} else {
										dboMovementDoc.setVal(Tc.MOVEMENT_TYPE, Tc.ANIMAL);
									}
									dboMovementDoc.setVal(Tc.DESTINATION_HOLDING_PIC,
											dboDestinationHolding.getVal(Tc.PIC));
									svw.saveObject(dboMovementDoc, false);
								}
								resultMsgLabel = "naits.error.transfer.fail.anmalIdDoesNotExist";
							}
							if (flockFlag.equals("")) {
								if (mandatoryFields == null || mandatoryFields.isEmpty()) {
									resultMsgLabel = "naits.error.sourceHoldingBelongsToActiveQuarantine";
									if (dboSourceHolding != null && !vc.checkIfHoldingBelongsInActiveQuarantine(
											dboSourceHolding.getObject_id(), svr)) {
										resultMsgLabel = "naits.error.animalAlreadyExistsInYourHolding";
										if (dboDestinationHolding != null && !dboDestinationHolding.getObject_id()
												.equals(dboSourceHolding.getObject_id())) {
											resultMsgLabel = "naits.error.selectedAnimalIsInTransitionSoCantBeDirectTransfered"
													+ flockFlag;
											if (!dboToMove.getStatus().equals(Tc.TRANSITION)) {
												dboMovement = wr.startAnimalOrFlockMovement(dboToMove,
														dboDestinationHolding, Tc.DIRECT_TRANSFER, null,
														dboMovementDoc.getVal(Tc.MOVEMENT_DOC_ID).toString(), null,
														null, null, null, null, null, svr, svw, sww);
												if (dboMovement != null) {
													if (dboMovement.getStatus().equals(Tc.REJECTED)) {
														resultMsgLabel = "naits.error.animalDirectTransferRejected";
													}
													svw.dbCommit();
													sww.dbCommit();
													Thread.sleep(2);
													if (!dboMovement.getStatus().equals(Tc.REJECTED)) {
														if (vc.checkIfHoldingIsSlaughterhouse(dboDestinationHolding)) {
															wr.finishAnimalOrFlockMovement(dboToMove,
																	dboDestinationHolding, dateOfAdmission,
																	dateOfAdmission, transporterPersonId, svw, sww);
														} else {
															wr.finishAnimalOrFlockMovement(dboToMove,
																	dboDestinationHolding, null, null, null, svw, sww);
														}
														svw.dbCommit();
														sww.dbCommit();
														resultMsgLabel = "naits.success.transfer";
													}
												}
											}
										}
									}
								} else if (dboToMove != null && mandatoryFields != null && !mandatoryFields.isEmpty()) {
									resultMsgLabel = I18n.getText(svr.getUserLocaleId(svr.getInstanceUser()),
											"naits.info.mandatoryFieldsForDirectTransfer") + " "
											+ mandatoryFields.toString();
								}
							} else {
								try {
									DbDataObject dboFlockUnit = wr.createFlockMovementUnit(dboToMove, totalUnits,
											femaleUnits, maleUnits, adultsUnits, svw, svr);
									resultMsgLabel = "naits.error.sourceHoldingBelongsToActiveQuarantine";
									if (dboSourceHolding != null && !vc.checkIfHoldingBelongsInActiveQuarantine(
											dboSourceHolding.getObject_id(), svr)) {
										resultMsgLabel = "naits.error.flockExistsInYourHolding";
										if (dboDestinationHolding != null && !dboDestinationHolding.getObject_id()
												.equals(dboSourceHolding.getObject_id())) {
											resultMsgLabel = "naits.error.selectedFlockIsInTransitionSoCantBeDirectTransfered"
													+ flockFlag;
											if (!dboToMove.getStatus().equals(Tc.TRANSITION)) {
												dboMovement = wr.startAnimalOrFlockMovement(dboFlockUnit,
														dboDestinationHolding, Tc.DIRECT_TRANSFER, null,
														dboMovementDoc.getVal(Tc.MOVEMENT_DOC_ID).toString(), null,
														null, null, null, null, null, svr, svw, sww);
												if (dboMovement != null) {
													if (dboMovement.getStatus().equals(Tc.REJECTED)) {
														resultMsgLabel = "naits.error.flockDirectTransferRejected";
													}
													svw.dbCommit();
													sww.dbCommit();
													Thread.sleep(2);
													if (!dboMovement.getStatus().equals(Tc.REJECTED)) {
														if (vc.checkIfHoldingIsSlaughterhouse(dboDestinationHolding)
																&& dboToMove != null) {
															wr.finishAnimalOrFlockMovement(dboFlockUnit,
																	dboDestinationHolding, dateOfAdmission,
																	dateOfAdmission, transporterPersonId, svw, sww);
														} else {
															wr.finishAnimalOrFlockMovement(dboFlockUnit,
																	dboDestinationHolding, null, null, null, svw, sww);
														}
														svw.dbCommit();
														sww.dbCommit();
														resultMsgLabel = "naits.success.flockDirectTransfer";
													}
												}
											}
										}
									}
								} catch (SvException e) {
									resultMsgLabel = e.getLabelCode();
								}
							}
							if (dboMovement == null) {
								throw (new SvException(resultMsgLabel, svr.getInstanceUser()));
							}
							if (dboMovementDoc != null) {
								resultMsgLabel = ma.checkAnimalOrFlockMovementsInMovementDocument(dboMovementDoc,
										svr.getSessionId());
								// this means that movement document was
								// generated/exists
								if (!resultMsgLabel.equals("naits.success.checkMovementsInMvmDoc")) {
									dboMovementDoc.setStatus(Tc.INVALID);
								} else {
									dboMovementDoc.setStatus(Tc.RELEASED);
									resultMsgLabel = resultMsgLabel + "_" + dboMovementDoc.getObject_id().toString();
								}
								svw.saveObject(dboMovementDoc);
								svw.dbCommit();
							}
						}
					} finally {
						if (lock != null && dboToMove != null) {
							SvLock.releaseLock(String.valueOf(dboToMove.getObject_id()), lock);
						}
					}
					@SuppressWarnings(Tc.UNUSED)
					DbDataObject refreshAnimalOrFlock = svr.getObjectById(dboToMove.getObject_id(),
							dboToMove.getObject_type(), new DateTime());
				}
			}
		} catch (SvException e) {
			log4j.error("Error occured in checkIfAnimalIdExist:" + e.getFormattedMessage(), e);
			resultMsgLabel = e.getLabelCode();
		} finally {
			if (svr != null) {
				svr.release();
			}
			if (svw != null) {
				svw.release();
			}
			if (sww != null) {
				sww.release();
			}
		}
		return Response.status(200).entity(resultMsgLabel).build();
	}

	@Path("/getNextOrPreviousHolding/{sessionId}/{holdingObjId}/{direction}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response getNextOrPreviousHolding(@PathParam("sessionId") String sessionId,
			@PathParam("holdingObjId") Long holdingObjId, @PathParam("direction") String direction) {
		String resultPic = Tc.EMPTY_STRING;
		SvReader svr = null;
		try {
			svr = new SvReader(sessionId);
			Reader rdr = new Reader();
			// rdr instance of this service can be additionally completed,
			// depending on the client requirements
			if (holdingObjId != null && direction != null) {
				DbDataObject dboHolding = svr.getObjectById(holdingObjId, SvReader.getTypeIdByName(Tc.HOLDING), null);
				if (dboHolding != null
						&& (direction.equalsIgnoreCase((Tc.FORWARD)) || direction.equalsIgnoreCase(Tc.BACKWARD))) {
					resultPic = rdr.getNextOrPreviousHoldingPic(dboHolding.getObject_id(), direction.toUpperCase(),
							svr);
				}
			}
		} catch (SvException e) {
			log4j.error("Error occured in getNextOrPreviousHolding:" + e.getFormattedMessage(), e);
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
		return Response.status(200).entity(resultPic).build();
	}

	@SuppressWarnings("unchecked")
	@Path("/getObjectSummary/{sessionId}/{tableName}/{objectId}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response getObjectSummary(@PathParam("sessionId") String sessionId, @PathParam("tableName") String tableName,
			@PathParam("objectId") Long objectId) throws ParseException {
		String result = Tc.EMPTY_STRING;
		SvReader svr = null;
		SvNote svn = null;
		Reader rdr = null;
		ValidationChecks vs = null;
		try {
			svr = new SvReader(sessionId);
			svn = new SvNote(svr);
			rdr = new Reader();
			vs = new ValidationChecks();
			// rdr instance of this service can be additionally completed,
			// depending on the client requirements
			if (tableName != null && objectId != null) {
				DbDataObject dboObject = svr.getObjectById(objectId, SvReader.getTypeIdByName(tableName), null);
				JSONObject jsonObj = new JSONObject();
				LinkedHashMap<String, String> jsonOrderedMap = new LinkedHashMap<>();
				if (dboObject != null) {
					switch (tableName) {
					case Tc.HOLDING:
						jsonOrderedMap = rdr.generateHoldingSummaryInformation(dboObject, svr);
						jsonObj.put("orderedItems", jsonOrderedMap);
						result = jsonObj.toJSONString();
						break;
					case Tc.HOLDING_RESPONSIBLE:
						break;
					case Tc.ANIMAL:
						rdr.generateAnimalSummaryInfo(objectId, jsonOrderedMap, svn, svr);
						jsonObj.put("orderedItems", jsonOrderedMap);
						result = jsonObj.toJSONString();
						break;
					case Tc.SVAROG_USERS:
						DbDataObject dboUser = svr.getObjectById(objectId, svCONST.OBJECT_TYPE_USER, null);
						LinkedHashMap<String, String> userSummaryInfoMap = new LinkedHashMap<>();
						if (vs.checkIfUserIsAdministrator(dboUser, svr)) {
							userSummaryInfoMap.put("naits.summary_info.admin_user", Tc.EMPTY_STRING);
						}
						rdr.generateUserOrGroupSummaryInformation(userSummaryInfoMap, dboUser, svn, svr);
						jsonObj.put("orderedItems", userSummaryInfoMap);
						result = jsonObj.toJSONString();
						break;
					case Tc.SVAROG_USER_GROUPS:
						DbDataObject dboUserGroup = svr.getObjectById(objectId, svCONST.OBJECT_TYPE_GROUP, null);
						LinkedHashMap<String, String> groupSummaryInfoMap = new LinkedHashMap<>();
						rdr.generateUserOrGroupSummaryInformation(groupSummaryInfoMap, dboUserGroup, svn, svr);
						jsonObj.put("orderedItems", groupSummaryInfoMap);
						result = jsonObj.toJSONString();
						break;
					case Tc.QUARANTINE:
						DbDataObject dboQuarantine = svr.getObjectById(objectId,
								SvReader.getTypeIdByName(Tc.QUARANTINE), null);
						String quarStatus = "Inactive";
						if (vs.checkIfQuarantineActive(dboQuarantine)) {
							quarStatus = "Active";
						}
						jsonOrderedMap.put("naits.quarantine.status", quarStatus);
						jsonObj.put("orderedItems", jsonOrderedMap);
						result = jsonObj.toJSONString();
						break;
					case Tc.POPULATION:
						rdr.generatePopulationSummaryInfo(jsonOrderedMap, objectId, svr);
						jsonObj.put("orderedItems", jsonOrderedMap);
						result = jsonObj.toJSONString();
						break;
					case Tc.AREA:
						jsonOrderedMap = rdr.generateAreaSummaryInformation(dboObject, svr);
						jsonObj.put("orderedItems", jsonOrderedMap);
						result = jsonObj.toJSONString();
						break;
					case Tc.PET:
						jsonOrderedMap = rdr.generatePetSummaryInfo(objectId, svr);
						jsonObj.put("orderedItems", jsonOrderedMap);
						result = jsonObj.toJSONString();
						break;
					case Tc.STRAY_PET:
						DbDataObject dboStrayPet = svr.getObjectById(objectId, SvReader.getTypeIdByName(Tc.STRAY_PET),
								null);
						rdr.setSummaryInformationPerStrayPet(jsonOrderedMap, dboStrayPet, svr);
						jsonObj.put("orderedItems", jsonOrderedMap);
						result = jsonObj.toJSONString();
						break;
					case Tc.INVENTORY_ITEM:
						jsonOrderedMap = rdr.generateInventoryItemSummaryInfo(dboObject, rdr, svr);
						jsonObj.put("orderedItems", jsonOrderedMap);
						result = jsonObj.toJSONString();
						break;
					default:
						break;
					}
				}
			}
		} catch (SvException e) {
			log4j.error("Error occured in getObjectSummary: " + e.getFormattedMessage());
		} finally {
			if (svr != null) {
				svr.release();
			}
			if (svn != null) {
				svn.release();
			}
		}
		return Response.status(200).entity(result).build();
	}

	@Path("/assignOrUnasignPackagePermissionOnUserOrGroup/{sessionId}/{packageName}/{actionName}")
	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces("text/html;charset=utf-8")
	public Response assignOrUnasignPackagePermissionOnUserOrGroup(@PathParam("sessionId") String sessionId,
			@PathParam("packageName") String packageName, @PathParam("actionName") String actionName,
			MultivaluedMap<String, String> formVals, @Context HttpServletRequest httpRequest) throws Exception {
		String resultMsgLbl = "naits.error.admConsole.permissionPackageNotAttached";
		JsonObject jsonData = null;
		Gson gson = new Gson();
		UserManager um = new UserManager();
		try {
			for (Entry<String, List<String>> entry : formVals.entrySet()) {
				if (entry.getKey() != null && !entry.getKey().isEmpty()) {
					String key = entry.getKey();
					jsonData = gson.fromJson(key, JsonObject.class);
				}
			}
			if (jsonData != null && jsonData.has(Tc.OBJ_ARRAY)) {
				resultMsgLbl = um.assignOrUnasignPackagePermissions(packageName, actionName,
						jsonData.get(Tc.OBJ_ARRAY).getAsJsonArray(), sessionId);
			} else {
				resultMsgLbl = Tc.error_admConsoleBadJson;
			}
		} catch (SvException e) {
			resultMsgLbl = e.getLabelCode();
			log4j.error("Error in processing permission attach to user: " + ((SvException) e).getFormattedMessage(), e);
		}
		return Response.status(200).entity(resultMsgLbl).build();
	}

	@Path("/attachUserToGroup/{sessionId}/{groupName}")
	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces("text/html;charset=utf-8")
	public Response attachUserToGroup(@PathParam("sessionId") String sessionId,
			@PathParam("groupName") String groupName, MultivaluedMap<String, String> formVals,
			@Context HttpServletRequest httpRequest) {
		String resultMsgLbl = "naits.error.admConsole.userToGroupAttached";
		JsonObject jsonData = null;
		Gson gson = new Gson();
		UserManager um = new UserManager();
		try {
			for (Entry<String, List<String>> entry : formVals.entrySet()) {
				if (entry.getKey() != null && !entry.getKey().isEmpty()) {
					String key = entry.getKey();
					jsonData = gson.fromJson(key, JsonObject.class);
				}
			}
			if (jsonData != null && jsonData.has(Tc.OBJ_ARRAY)) {
				resultMsgLbl = um.attachUserToGroup(groupName, jsonData.get(Tc.OBJ_ARRAY).getAsJsonArray(), sessionId);
			} else {
				resultMsgLbl = Tc.error_admConsoleBadJson;
			}
		} catch (Exception e) {
			if (e instanceof SvException) {
				log4j.error("Error in processing attach user to group: " + ((SvException) e).getFormattedMessage(), e);
			} else
				log4j.error("General error in processing attach user to group", e);
		}
		return Response.status(200).entity(resultMsgLbl).build();
	}

	@Path("/detachUserFromGroup/{sessionId}/{groupName}")
	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces("text/html;charset=utf-8")
	public Response detachUserFromGroup(@PathParam("sessionId") String sessionId,
			@PathParam("groupName") String groupName, MultivaluedMap<String, String> formVals,
			@Context HttpServletRequest httpRequest) {
		String resultMsgLbl = "naits.error.admConsole.userToGroupDetached";
		JsonObject jsonData = null;
		Gson gson = new Gson();
		UserManager um = new UserManager();
		try {
			for (Entry<String, List<String>> entry : formVals.entrySet()) {
				if (entry.getKey() != null && !entry.getKey().isEmpty()) {
					String key = entry.getKey();
					jsonData = gson.fromJson(key, JsonObject.class);
				}
			}
			if (jsonData != null && jsonData.has(Tc.OBJ_ARRAY)) {
				resultMsgLbl = um.detachUserToGroup(groupName, jsonData.get(Tc.OBJ_ARRAY).getAsJsonArray(), sessionId);
			} else {
				resultMsgLbl = Tc.error_admConsoleBadJson;
			}
		} catch (SvException e) {
			log4j.error("Error in processing detach user to group: " + ((SvException) e).getFormattedMessage(), e);
		} catch (Exception e) {
			log4j.error("General error in processing detach user to group", e);
		}
		return Response.status(200).entity(resultMsgLbl).build();
	}

	@Path("/getDependentDropdown/{sessionId}/{tableName}/{codeItemValue}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response getDependentDropdown(@PathParam("sessionId") String sessionId,
			@PathParam("tableName") String tableName, @PathParam("codeItemValue") String codeItemValue) {
		String result = Tc.EMPTY_STRING;
		SvReader svr = null;
		Reader rdr = null;
		try {
			svr = new SvReader(sessionId);
			rdr = new Reader();
			DbDataArray getDependentCodeListItem = null;
			switch (tableName) {
			case Tc.HOLDING:
				getDependentCodeListItem = rdr.getDependentMunicComunVillage(codeItemValue, svr);
				break;
			case Tc.STRAY_PET_LOCATION:
				getDependentCodeListItem = rdr.getDependentMunicComunVillage(codeItemValue, svr);
				break;
			case Tc.HOLDING_RESPONSIBLE:
				getDependentCodeListItem = rdr.getDependentMunicComunVillage(codeItemValue, svr);
				break;
			case Tc.LABORATORY:
				getDependentCodeListItem = rdr.getDependentMunicComunVillage(codeItemValue, svr);
				break;
			case Tc.ANIMAL:
				getDependentCodeListItem = rdr.getAppropriateAnimalRacesPerClass(codeItemValue, svr);
				break;
			case Tc.VACCINATION_EVENT:
				getDependentCodeListItem = rdr.getAppropriateDiseasePerCampaignActivityType(codeItemValue, svr);
				break;
			default:
				break;
			}
			if (getDependentCodeListItem != null)
				result = getDependentCodeListItem.toSimpleJson().toString();
		} catch (SvException e) {
			log4j.error("Error in dependent dropdown: " + ((SvException) e).getFormattedMessage(), e);
		} catch (Exception e) {
			log4j.error("General error in processing detach user to group", e);
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
		return Response.status(200).entity(result).build();
	}

	@Path("/getValidVaccEvents/{sessionId}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response getValidVaccinationEvents(@PathParam("sessionId") String sessionId) {
		return getValidVaccinationEvents(sessionId, null);
	}

	@Path("/getValidVaccEvents/{sessionId}/{holdingType}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response getValidVaccinationEvents(@PathParam("sessionId") String sessionId,
			@PathParam("holdingType") String holdingType) {
		String result = Tc.EMPTY_STRING;
		SvReader svr = null;
		Reader rdr = null;
		try {
			svr = new SvReader(sessionId);
			rdr = new Reader();
			DbDataArray allValidEvents = rdr.getValidVaccEvents(svr, holdingType);
			result = allValidEvents.toSimpleJson().toString();
		} catch (SvException e) {
			log4j.error("Error in getValidVaccinationEvenets: " + ((SvException) e).getFormattedMessage(), e);
		} catch (Exception e) {
			log4j.error("General error in getValidVaccinationEvenets", e);
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
		return Response.status(200).entity(result).build();
	}

	@Path("/getValidTestTypes/{sessionId}/{objectId}/{testType}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response getValidTestTypes(@PathParam("sessionId") String sessionId, @PathParam("objectId") Long objectId,
			@PathParam("testType") String testType) {
		String result = Tc.EMPTY_STRING;
		SvReader svr = null;
		Reader rdr = null;
		try {
			svr = new SvReader(sessionId);
			rdr = new Reader();
			DbDataArray validTestTypes = rdr.getValidTestTypes(objectId, testType, svr);
			result = rdr.convertDbDataArrayToGridJson(validTestTypes, Tc.LAB_TEST_TYPE);
		} catch (SvException e) {
			log4j.error("Error in getValidTestTypes: " + ((SvException) e).getFormattedMessage(), e);
		} catch (Exception e) {
			log4j.error("General error in getValidTestTypes", e);
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
		return Response.status(200).entity(result).build();
	}

	@Path("/createActivityPeriodPerHerder/{sessionId}/{holdingObjId}/{dateFrom}/{dateTo}")
	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces("text/html;charset=utf-8")
	public Response createActivityPeriodPerHerder(@PathParam("sessionId") String sessionId,
			@PathParam("holdingObjId") Long holdingObjId, @PathParam("dateFrom") String dateFrom,
			@PathParam("dateTo") String dateTo, MultivaluedMap<String, String> formVals,
			@Context HttpServletRequest httpRequest) {
		String resultMsgLbl = "naits.error.setActivityPeriodPerHerder";
		JsonObject jsonData = null;
		Gson gson = new Gson();
		MassActions massAct = new MassActions();
		try {
			for (Entry<String, List<String>> entry : formVals.entrySet()) {
				if (entry.getKey() != null && !entry.getKey().isEmpty()) {
					String key = entry.getKey();
					jsonData = gson.fromJson(key, JsonObject.class);
				}
			}
			if (jsonData != null && jsonData.has(Tc.OBJ_ARRAY)) {
				resultMsgLbl = massAct.createActivityPeriod(holdingObjId, dateFrom, dateTo,
						jsonData.get(Tc.OBJ_ARRAY).getAsJsonArray(), sessionId);
			} else {
				resultMsgLbl = Tc.error_admConsoleBadJson;
			}
		} catch (Exception e) {
			log4j.error("General error in createActivityPeriodPerHerder", e);
		}
		return Response.status(200).entity(resultMsgLbl).build();
	}

	@Path("/generateInventoryItem/{sessionId}")
	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces("text/html;charset=utf-8")
	public Response generateInventoryItem(@PathParam("sessionId") String sessionId,
			MultivaluedMap<String, String> formVals, @Context HttpServletRequest httpRequest) {
		String resultMsgLbl = "naits.error.generalError";
		SvReader svr = null;
		SvWriter svw = null;

		JsonObject jsonData = null;
		Gson gson = new Gson();
		try {
			svr = new SvReader(sessionId);
			svr.setAutoCommit(false);
			svw = new SvWriter(svr);
			Writer wr = new Writer();
			for (Entry<String, List<String>> entry : formVals.entrySet()) {
				if (entry.getKey() != null && !entry.getKey().isEmpty()) {
					String key = entry.getKey();
					jsonData = gson.fromJson(key, JsonObject.class);
				}
			}
			if (jsonData != null && jsonData.has(Tc.OBJ_ARRAY)) {
				resultMsgLbl = wr.generateInventoryItem(jsonData.get(Tc.OBJ_ARRAY).getAsJsonArray(), svr, svw);
				if (resultMsgLbl.trim().equals("")) {
					resultMsgLbl = "naits.success.saveInventoryItem";
				}
			} else {
				resultMsgLbl = Tc.error_admConsoleBadJson;
			}
		} catch (SvException e) {
			resultMsgLbl = e.getLabelCode();
			log4j.error(Tc.ERROR_SAVING_ANIMAL + e.getFormattedMessage(), e);
		} finally {
			if (svr != null) {
				svr.release();
			}
			if (svw != null) {
				svw.release();
			}
		}
		return Response.status(200).entity(resultMsgLbl).build();
	}

	@Path("/moveAndLinkAnimals/{sessionId}/{actionName}/{actionParam}")
	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces("text/html;charset=utf-8")
	public Response moveAndLinkAnimals(@PathParam("sessionId") String sessionId,
			@PathParam("actionName") String actionName, @PathParam("actionParam") String actionParam,
			MultivaluedMap<String, String> formVals, @Context HttpServletRequest httpRequest) {
		String resultMsgLbl = "naits.error.massAnimalsAction";
		JsonObject jsonData = null;
		Gson gson = new Gson();
		MassActions massAct = new MassActions();
		try {
			for (Entry<String, List<String>> entry : formVals.entrySet()) {
				if (entry.getKey() != null && !entry.getKey().isEmpty()) {
					String key = entry.getKey();
					jsonData = gson.fromJson(key, JsonObject.class);
				}
			}
			if (jsonData != null && jsonData.has(Tc.OBJ_ARRAY)) {
				resultMsgLbl = massAct.moveAndLinkAnimals(actionName, actionParam,
						jsonData.get(Tc.OBJ_ARRAY).getAsJsonArray(), sessionId);
			} else {
				resultMsgLbl = Tc.error_admConsoleBadJson;
			}
		} catch (SvException e) {
			resultMsgLbl = e.getLabelCode();
			log4j.error("Error in processing mass action for: " + ((SvException) e).getFormattedMessage(), e);
		} catch (Exception e) {
			resultMsgLbl = "naits.error.general";
			log4j.error("General error in processing mass action", e);
		}
		return Response.status(200).entity(resultMsgLbl).build();
	}

	@Path("/labSampleMassAction/{sessionId}/{actionName}/{subActionName}/{actionParam}")
	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces("text/html;charset=utf-8")
	public Response labSampleMassAction(@PathParam("sessionId") String sessionId,
			@PathParam("actionName") String actionName, @PathParam("subActionName") String subActionName,
			@PathParam("actionParam") String actionParam, MultivaluedMap<String, String> formVals,
			@Context HttpServletRequest httpRequest) {
		String resultMsgLbl = "naits.error.labSampleMassAction";
		JsonObject jsonData = null;
		Gson gson = new Gson();
		MassActions massAct = new MassActions();
		try {
			for (Entry<String, List<String>> entry : formVals.entrySet()) {
				if (entry.getKey() != null && !entry.getKey().isEmpty()) {
					String key = entry.getKey();
					jsonData = gson.fromJson(key, JsonObject.class);
				}
			}
			if (jsonData != null && jsonData.has(Tc.OBJ_ARRAY)) {
				resultMsgLbl = massAct.labSampleHandler(actionName, subActionName, actionParam,
						jsonData.get(Tc.OBJ_ARRAY).getAsJsonArray(), sessionId);
			} else {
				resultMsgLbl = Tc.error_admConsoleBadJson;
			}
		} catch (SvException e) {
			resultMsgLbl = e.getLabelCode();
			log4j.error("Error in labSampleMassAction: " + ((SvException) e).getFormattedMessage(), e);
		} catch (Exception e) {
			resultMsgLbl = "naits.error.general";
			log4j.error("General error in labSampleMassAction:", e);
		}
		return Response.status(200).entity(resultMsgLbl).build();
	}

	@Path("/changeStatus/{sessionId}/{status}")
	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces("text/html;charset=utf-8")
	public Response changeStatus(@PathParam("sessionId") String sessionId, @PathParam("status") String status,
			MultivaluedMap<String, String> formVals, @Context HttpServletRequest httpRequest) {
		String resultMsgLbl = "naits.error.changeStatus";
		SvReader svr = null;
		Writer wr = new Writer();
		JsonObject jsonData = null;
		Gson gson = new Gson();
		try {
			svr = new SvReader(sessionId);
			for (Entry<String, List<String>> entry : formVals.entrySet()) {
				if (entry.getKey() != null && !entry.getKey().isEmpty()) {
					String key = entry.getKey();
					jsonData = gson.fromJson(key, JsonObject.class);
				}
			}
			if (jsonData != null
					&& (jsonData.has(Tc.OBJ_ARRAY) || (jsonData.has("objectId") && jsonData.has("tableName")))) {
				wr.changeStatus(jsonData, status, svr);
				resultMsgLbl = "naits.success.changeStatus";
			} else {
				resultMsgLbl = Tc.error_admConsoleBadJson;
			}
		} catch (SvException e) {
			resultMsgLbl = e.getLabelCode();
			log4j.error("Error in changeStatus: " + ((SvException) e).getFormattedMessage(), e);
		} catch (Exception e) {
			resultMsgLbl = "naits.error.general";
			log4j.error("General error in changeStatus", e);
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
		return Response.status(200).entity(resultMsgLbl).build();
	}

	@Path("/movementDocStatusUpdate/{sessionId}/{status}")
	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces("text/html;charset=utf-8")
	public Response movementDocStatusUpdate(@PathParam("sessionId") String sessionId,
			@PathParam("status") String status, MultivaluedMap<String, String> formVals,
			@Context HttpServletRequest httpRequest) {
		String resultMsgLbl = "naits.error.changeStatus";
		SvReader svr = null;
		MassActions massAct = new MassActions();
		JsonObject jsonData = null;
		Gson gson = new Gson();
		try {
			svr = new SvReader(sessionId);
			for (Entry<String, List<String>> entry : formVals.entrySet()) {
				if (entry.getKey() != null && !entry.getKey().isEmpty()) {
					String key = entry.getKey();
					jsonData = gson.fromJson(key, JsonObject.class);
				}
			}
			if (jsonData != null && jsonData.has(Tc.OBJ_ARRAY)) {
				resultMsgLbl = massAct.updateStatusOfMovementDoc(jsonData.get(Tc.OBJ_ARRAY).getAsJsonArray(), status,
						svr);
			} else {
				resultMsgLbl = Tc.error_admConsoleBadJson;
			}
		} catch (Exception e) {
			resultMsgLbl = "naits.error.general";
			if (e instanceof SvException) {
				if (((SvException) e).getLabelCode().equals("naits.info.cantChangeStatus")) {
					resultMsgLbl = "naits.info.cantChangeStatus";
				}
				log4j.error("Error in processing mass action for: " + ((SvException) e).getFormattedMessage(), e);
			} else {
				log4j.error("General error in processing mass action", e);
			}

		} finally {
			if (svr != null) {
				svr.release();
			}
		}
		return Response.status(200).entity(resultMsgLbl).build();
	}

	@Path("/getHoldingPerExportQuarantine/{sessionId}/{quarantineObjId}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response getHoldingPerExportQuarantine(@PathParam("sessionId") String sessionId,
			@PathParam("quarantineObjId") Long quarantineObjId) {
		SvReader svr = null;
		Reader rdr = null;
		String result = Tc.EMPTY_STRING;
		try {
			svr = new SvReader(sessionId);
			rdr = new Reader();
			DbDataObject dboQuarantine = svr.getObjectById(quarantineObjId, SvReader.getTypeIdByName(Tc.QUARANTINE),
					null);
			if (dboQuarantine != null && dboQuarantine.getVal(Tc.QUARANTINE_TYPE) != null
					&& dboQuarantine.getVal(Tc.QUARANTINE_TYPE).toString().equals("0")) {
				DbDataObject dboHolding = rdr.getHoldingLinkedPerExportQuaranitne(dboQuarantine, svr);
				if (dboHolding != null)
					result = dboHolding.toSimpleJson().toString();
			}
		} catch (SvException e) {
			log4j.error(Tc.ERROR_DETECTED + e.getFormattedMessage(), e);
			return Response.status(200).entity(e.getFormattedMessage()).build();
		} finally {
			if (svr != null)
				svr.release();
		}
		return Response.status(200).entity(result).build();
	}

	/**
	 * Saves quarantine object <br>
	 * Finds all holdings that are located inside the q-space and creates a link for
	 * each <br>
	 * tweak this to work with existing quarantines, edit mode
	 * 
	 * @param token
	 * @param center      - center of the quarantine
	 * @param radius      - radis of the quarantine
	 * @param formVals
	 * @param httpRequest
	 * 
	 * @return Quarantine dbDataObject as Json
	 */
	@Path("/saveQuarantine/{token}/{objectId}/{geom}/{radius}")
	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces("application/json")
	public Response saveQuarantineViaMap(@PathParam("token") String token, @PathParam("objectId") Long objectId,
			@PathParam("geom") String geom, @PathParam("radius") String radius, MultivaluedMap<String, String> formVals,
			@Context HttpServletRequest httpRequest) {
		String result = "naits.success.save_quarantine_via_map";
		int httpResponse = 200;
		SvReader svr = null;
		SvWriter svw = null;
		DbDataObject qDbo = null;
		WSSvarog utils = null;
		Writer writer = null;
		WsGeometry geometry = null;
		try {
			if (formVals == null)
				throw new SvException("naits.error.form_data_not_found", SvCore.getUserBySession(token));

			svr = new SvReader(token);
			svr.setIncludeGeometries(true);
			svw = new SvWriter(svr);
			utils = new WSSvarog();
			writer = new Writer();
			geometry = new WsGeometry();
			boolean isUpdate = objectId.compareTo(0L) > 0 ? true : false;

			if (radius != null && geom != null) {
				// create/edit quarantine object and geometry shape
				qDbo = utils.generateQuarantineDboFromMapData(svr, objectId, formVals, radius, isUpdate);
				Geometry qShape = geometry.createQuarantineShape(svr, geom, radius);
				// get relevant holdings, invalidate if update
				DbDataArray hArr = geometry.getHoldingsByQuarantine(svr, qShape);
				// Should we move link invalidation after a confiramtion that
				// the root object is properly updated?
				if (isUpdate)
					writer.invalidateLinksBetweenHoldingAndQuarantineViaMap(svw, svr, qDbo);
				// Save objects
				svw.saveObject(qDbo, false);
				geometry.saveQuarantineGeometry(svr, svw, qDbo, geom, radius, qShape, isUpdate);
				geometry.invalidateGeoCache(qShape, qDbo.getObject_type());
				svw.dbCommit();
				writer.generateLinksBetweenHoldingAndQuarantineViaMap(qDbo, hArr, svw);
			} else {
				svw.saveObject(qDbo);
			}
		} catch (SvException e) {
			result = "naits.error.save_quarantine_via_map";
			log4j.error("Error in saveQuarantine: " + ((SvException) e).getFormattedMessage(), e);
		} catch (Exception e) {
			result = "naits.error.save_quarantine_via_map";
			log4j.error("General error in saveQuarantine", e);
		} finally {
			if (svw != null)
				svw.release();
			if (svr != null)
				svr.release();
		}
		return Response.status(httpResponse).entity(result).build();
	}

	@Path("/attachUserToOrgUnit/{sessionId}/{orgUnitId}")
	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces("text/html;charset=utf-8")
	public Response attachUserToOrgUnit(@PathParam("sessionId") String sessionId,
			@PathParam("orgUnitId") Long orgUnitId, MultivaluedMap<String, String> formVals,
			@Context HttpServletRequest httpRequest) {
		String resultMsgLbl = "naits.error.admConsole.userToOrgUnirAttached";
		JsonObject jsonData = null;
		Gson gson = new Gson();
		UserManager um = new UserManager();
		try {
			for (Entry<String, List<String>> entry : formVals.entrySet()) {
				if (entry.getKey() != null && !entry.getKey().isEmpty()) {
					String key = entry.getKey();
					jsonData = gson.fromJson(key, JsonObject.class);
				}
			}
			if (jsonData != null && jsonData.has(Tc.OBJ_ARRAY)) {
				resultMsgLbl = um.attachUserToOrgUnit(orgUnitId, jsonData.get(Tc.OBJ_ARRAY).getAsJsonArray(),
						sessionId);
			}
		} catch (SvException e) {
			log4j.error("Error in attachUserToOrgUnit: " + ((SvException) e).getFormattedMessage(), e);
		} catch (Exception e) {
			log4j.error("General error in attachUserToOrgUnit", e);
		}
		return Response.status(200).entity(I18n.getText(resultMsgLbl)).build();
	}

	@Path("/detachUserToOrgUnit/{sessionId}/{groupName}")
	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces("text/html;charset=utf-8")
	public Response detachUserToOrgUnit(@PathParam("sessionId") String sessionId,
			@PathParam("groupName") Long orgUnitId, MultivaluedMap<String, String> formVals,
			@Context HttpServletRequest httpRequest) {
		String resultMsgLbl = "naits.error.admConsole.userToOrgUnitDetach";
		JsonObject jsonData = null;
		Gson gson = new Gson();
		UserManager um = new UserManager();
		try {
			for (Entry<String, List<String>> entry : formVals.entrySet()) {
				if (entry.getKey() != null && !entry.getKey().isEmpty()) {
					String key = entry.getKey();
					jsonData = gson.fromJson(key, JsonObject.class);
				}
			}
			if (jsonData != null && jsonData.has(Tc.OBJ_ARRAY)) {
				resultMsgLbl = um.detachUsersToOrgUnit(orgUnitId, jsonData.get(Tc.OBJ_ARRAY).getAsJsonArray(),
						sessionId);
			} else {
				resultMsgLbl = Tc.error_admConsoleBadJson;
			}
		} catch (SvException e) {
			log4j.error("Error in detachUserToOrgUnit: " + ((SvException) e).getFormattedMessage(), e);
		} catch (Exception e) {
			log4j.error("General error in detachUserToOrgUnit", e);
		}
		return Response.status(200).entity(I18n.getText(resultMsgLbl)).build();
	}

	@Path("/assignUserToLaboratory/{sessionId}/{laboratoryObjId}")
	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces("text/html;charset=utf-8")
	public Response assignUserToLaboratory(@PathParam("sessionId") String sessionId,
			@PathParam("laboratoryObjId") Long laboratoryObjId, MultivaluedMap<String, String> formVals,
			@Context HttpServletRequest httpRequest) {
		String resultMsgLbl = "naits.error.admConsole.userToLaboratoryAssigned";
		JsonObject jsonData = null;
		Gson gson = new Gson();
		UserManager um = new UserManager();
		try {
			for (Entry<String, List<String>> entry : formVals.entrySet()) {
				if (entry.getKey() != null && !entry.getKey().isEmpty()) {
					String key = entry.getKey();
					jsonData = gson.fromJson(key, JsonObject.class);
				}
			}
			if (jsonData != null && jsonData.has(Tc.OBJ_ARRAY)) {
				resultMsgLbl = um.assignUserToObjectWithPoa(laboratoryObjId,
						jsonData.get(Tc.OBJ_ARRAY).getAsJsonArray(), sessionId);
			} else {
				resultMsgLbl = Tc.error_admConsoleBadJson;
			}
		} catch (SvException e) {
			log4j.error("Error in assignUserToLaboratory: " + ((SvException) e).getFormattedMessage(), e);
		} catch (Exception e) {
			log4j.error("General error in assignUserToLaboratory", e);
		}
		return Response.status(200).entity(I18n.getText(resultMsgLbl)).build();
	}

	@Path("/unassignUserFromLaboratory/{sessionId}/{laboratoryObjId}")
	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces("text/html;charset=utf-8")
	public Response unassignUserFromLaboratory(@PathParam("sessionId") String sessionId,
			@PathParam("laboratoryObjId") Long laboratoryObjId, MultivaluedMap<String, String> formVals,
			@Context HttpServletRequest httpRequest) {
		String resultMsgLbl = "naits.error.admConsole.userToLaboratoryUnassigned";
		JsonObject jsonData = null;
		Gson gson = new Gson();
		UserManager um = new UserManager();
		try {
			for (Entry<String, List<String>> entry : formVals.entrySet()) {
				if (entry.getKey() != null && !entry.getKey().isEmpty()) {
					String key = entry.getKey();
					jsonData = gson.fromJson(key, JsonObject.class);
				}
			}
			if (jsonData != null && jsonData.has(Tc.OBJ_ARRAY)) {
				resultMsgLbl = um.unassignUserFromObjectWithPoa(laboratoryObjId,
						jsonData.get(Tc.OBJ_ARRAY).getAsJsonArray(), sessionId);
			} else {
				resultMsgLbl = Tc.error_admConsoleBadJson;
			}
		} catch (Exception e) {
			if (e instanceof SvException) {
				log4j.error("Error in processing attach user to group: " + ((SvException) e).getFormattedMessage(), e);
			} else
				log4j.error("General error in processing attach user to group", e);
		}
		return Response.status(200).entity(I18n.getText(resultMsgLbl)).build();
	}

	@Path("/detachUserToOrgUnits/{sessionId}/{userId}")
	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces("text/html;charset=utf-8")
	public Response detachUserToOrgUnits(@PathParam("sessionId") String sessionId, @PathParam("userId") Long userId,
			MultivaluedMap<String, String> formVals, @Context HttpServletRequest httpRequest) {
		String resultMsgLbl = "naits.error.admConsole.userToOrgUnirAttached";
		JsonObject jsonData = null;
		Gson gson = new Gson();
		UserManager um = new UserManager();
		try {
			for (Entry<String, List<String>> entry : formVals.entrySet()) {
				if (entry.getKey() != null && !entry.getKey().isEmpty()) {
					String key = entry.getKey();
					jsonData = gson.fromJson(key, JsonObject.class);
				}
			}
			if (jsonData != null && jsonData.has(Tc.OBJ_ARRAY)) {
				um.detachUserToOrgUnis(userId, jsonData.get(Tc.OBJ_ARRAY).getAsJsonArray(), sessionId);
				resultMsgLbl = Tc.success_userToGroupAttached;
			} else {
				resultMsgLbl = Tc.error_admConsoleBadJson;
			}
		} catch (SvException e) {
			log4j.error("Error in detachUserToOrgUnits: " + ((SvException) e).getFormattedMessage(), e);
		} catch (Exception e) {
			log4j.error("General error in detachUserToOrgUnits", e);
		}
		return Response.status(200).entity(I18n.getText(resultMsgLbl)).build();
	}

	@Path("/getLocaleId/{sessionId}/{locale}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response getLocaleId(@PathParam("sessionId") String sessionId, @PathParam("locale") String locale) {
		String result = "naits.error.notFound.locale";
		SvReader svr = null;
		try {
			svr = new SvReader(sessionId);
			DbSearchCriterion dbs = new DbSearchCriterion(Tc.LOCALE_ID, DbCompareOperand.EQUAL, locale);
			DbDataArray locales = svr.getObjects(dbs, svCONST.OBJECT_TYPE_LOCALE, null, 0, 0);

			if (locales.size() == 1) {
				result = locales.get(0).getObject_id().toString();
			}
		} catch (SvException e) {
			result = "naits.error.general";
			log4j.error(Tc.ERROR_SAVING_ANIMAL + e.getFormattedMessage(), e);
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
		return Response.status(200).entity(result).build();
	}

	@Path("/exportCertifiedAnimals/{sessionId}/{objectId}")
	@POST
	@Produces("text/html;charset=utf-8")
	public Response exportCertifiedAnimals(@PathParam("sessionId") String sessionId,
			@PathParam("objectId") Long objectId, MultivaluedMap<String, String> formVals,
			@Context HttpServletRequest httpRequest) {
		String resultMsgLbl = Tc.EMPTY_STRING;
		JsonObject jsonData = null;
		Gson gson = null;
		MassActions massAct = null;
		try {
			gson = new Gson();
			massAct = new MassActions();
			for (Entry<String, List<String>> entry : formVals.entrySet()) {
				if (entry.getKey() != null && !entry.getKey().isEmpty()) {
					String key = entry.getKey();
					jsonData = gson.fromJson(key, JsonObject.class);
				}
			}
			if (jsonData != null && jsonData.has(Tc.OBJ_ARRAY)) {
				resultMsgLbl = massAct.exportCertMassHandler(jsonData.get(Tc.OBJ_ARRAY).getAsJsonArray(), sessionId,
						objectId);
			} else {
				resultMsgLbl = Tc.error_admConsoleBadJson;
			}
		} catch (SvException e) {
			log4j.error("Error in exportCertifiedAnimals: " + ((SvException) e).getFormattedMessage(), e);
		} catch (Exception e) {
			log4j.error("General error in exportCertifiedAnimals", e);
		}
		return Response.status(200).entity(resultMsgLbl).build();
	}

	@Path("/cancelExportCertificate/{sessionId}/{objectId}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response cancelExportCertificate(@PathParam("sessionId") String sessionId,
			@PathParam("objectId") Long objectId, @Context HttpServletRequest httpRequest) {
		String resultMsgLbl = Tc.EMPTY_STRING;
		Writer wr = null;
		Reader rdr = null;
		try {
			wr = new Writer();
			rdr = new Reader();
			resultMsgLbl = wr.cancelExportCertificate(objectId, rdr, sessionId);
		} catch (SvException e) {
			resultMsgLbl = "naits.error.general";
			log4j.error("Error in cancelExportCertificate: " + ((SvException) e).getFormattedMessage(), e);
		} catch (Exception e) {
			resultMsgLbl = "naits.error.general";
			log4j.error("General error in cancelExportCertificate", e);
		}
		return Response.status(200).entity(resultMsgLbl).build();
	}

	@Path("/checkMovementDoc/{sessionId}/{objectId}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response checkMovementDoc(@PathParam("sessionId") String sessionId, @PathParam("objectId") Long objectId,
			@Context HttpServletRequest httpRequest) {
		String resultMsgLbl = Tc.EMPTY_STRING;
		MassActions massAct = new MassActions();
		SvReader svr = null;
		try {
			svr = new SvReader(sessionId);

			DbDataObject movementDoc = svr.getObjectById(objectId, SvReader.getTypeIdByName(Tc.MOVEMENT_DOC), null);
			if (movementDoc != null) {
				ReentrantLock lock = null;
				try {
					lock = SvLock.getLock(String.valueOf(movementDoc.getObject_id()), false, 0);
					if (lock == null) {
						throw (new SvException(Tc.objectUsedByOtherSession, svr.getInstanceUser()));
					}
					resultMsgLbl = massAct.checkAnimalOrFlockMovementsInMovementDocument(movementDoc, sessionId);
				} finally {
					if (svr != null) {
						svr.release();
					}
					if (lock != null && movementDoc != null) {
						SvLock.releaseLock(String.valueOf(movementDoc.getObject_id()), lock);
					}
				}
			}
		} catch (SvException e) {
			resultMsgLbl = "naits.error.general";
			log4j.error("Error in checkMovementDoc: " + ((SvException) e).getFormattedMessage(), e);
		} catch (Exception e) {
			resultMsgLbl = "naits.error.general";
			log4j.error("General error in checkMovementDoc", e);
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
		return Response.status(200).entity(resultMsgLbl).build();
	}

	@Path("/assignResultToLabSample/{sessionId}/{parentId}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response assignResultToLabSample(@PathParam("sessionId") String sessionId,
			@PathParam("parentId") Long parentId, @Context HttpServletRequest httpRequest) {
		String resultMsgLbl = Tc.EMPTY_STRING;
		SvReader svr = null;
		SvWriter svw = null;
		Writer wr = null;
		try {
			svr = new SvReader(sessionId);
			svw = new SvWriter(svr);
			svw.dbSetAutoCommit(false);
			wr = new Writer();
			resultMsgLbl = wr.setHealthStatusToLabSample(parentId, svr, svw);
		} catch (SvException e) {
			resultMsgLbl = "naits.error.general";
			log4j.error("Error in assignResultToLabSample: " + ((SvException) e).getFormattedMessage(), e);
		} catch (Exception e) {
			resultMsgLbl = "naits.error.general";
			log4j.error("General error in assignResultToLabSample", e);
		} finally {
			if (svr != null) {
				svr.release();
			}
			if (svw != null) {
				svw.release();
			}
		}
		return Response.status(200).entity(resultMsgLbl).build();
	}

	@Path("/getDynamicDropdown/{sessionId}/{dropdownCase}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response getDynamicDropdown(@PathParam("sessionId") String sessionId,
			@PathParam("dropdownCase") String dropdownCase) {
		String result = Tc.EMPTY_STRING;
		SvReader svr = null;
		UserManager um = null;
		try {
			svr = new SvReader(sessionId);
			um = new UserManager();

			if (dropdownCase != null) {
				switch (dropdownCase) {
				case "USER_GROUP_CUSTOM_PERMISSIONS":
					ArrayList<String> customPermissions = new ArrayList<String>();
					DbSearchCriterion cr11 = new DbSearchCriterion("SYSTEM_TABLE", DbCompareOperand.EQUAL, false);
					DbDataArray dbArray1 = svr.getObjects(new DbSearchExpression().addDbSearchItem(cr11),
							svCONST.OBJECT_TYPE_TABLE, null, 0, 0);
					for (DbDataObject tempDbo : dbArray1.getItems()) {
						customPermissions.add(tempDbo.getVal(Tc.TABLE_NAME) + ".READ");
						customPermissions.add(tempDbo.getVal(Tc.TABLE_NAME) + ".FULL");
					}
					result = customPermissions.toString();
					break;
				case "USER_PACKAGE_PERMISSIONS":
					ArrayList<String> allPermPackages = um.loadPermissionPackages();
					ArrayList<String> temp = new ArrayList<String>();
					for (String tempPermissionPackage : allPermPackages) {
						String tempData[] = tempPermissionPackage.split(":");
						temp.add(tempData[0]);
					}
					result = temp.toString();
					break;
				default:
					break;
				}
			}
		} catch (SvException e) {
			log4j.error("Error occured in getDynamicDropdown ws (ApplicationServices.java):" + e.getFormattedMessage(),
					e);
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
		return Response.status(200).entity(result).build();
	}

	@Path("/getHoldingForExportCert/{sessionId}/{exportCertObjId}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response getHoldingForExportCert(@PathParam("sessionId") String sessionId,
			@PathParam("exportCertObjId") Long exportCertObjId) {
		String result = Tc.EMPTY_STRING;
		SvReader svr = null;
		Reader rdr = null;
		try {
			svr = new SvReader(sessionId);
			rdr = new Reader();
			if (exportCertObjId != null && !exportCertObjId.equals(0L)) {
				DbDataObject dboExportCert = svr.getObjectById(exportCertObjId,
						SvReader.getTypeIdByName(Tc.EXPORT_CERT), null);
				if (dboExportCert != null) {
					DbDataObject dboHolding = rdr.getHoldingPerExportCertificate(dboExportCert, svr);
					if (dboHolding != null) {
						result = dboHolding.toSimpleJson().toString();
					}
				}
			}
		} catch (SvException e) {
			log4j.error(
					"Error occured in getHoldingForExportCert ws (ApplicationServices.java):" + e.getFormattedMessage(),
					e);
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
		return Response.status(200).entity(result).build();
	}

	@Path("/generateAnimals/{sessionId}/{objectId}/{startEarTagId}/{endEarTagId}/{animalClass}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response generateAnimals(@PathParam("sessionId") String sessionId, @PathParam("objectId") Long objectId,
			@PathParam("startEarTagId") String startEarTagId, @PathParam("endEarTagId") String endEarTagId,
			@PathParam("animalClass") String animalClass) {
		String result = Tc.EMPTY_STRING;
		SvReader svr = null;
		SvWriter svw = null;
		Writer wr = null;
		try {
			svr = new SvReader(sessionId);
			svw = new SvWriter(svr);
			svw.setAutoCommit(false);
			wr = new Writer();
			result = wr.generateAnimalObjects(objectId, startEarTagId, endEarTagId, animalClass, null, null, null, svr,
					svw);
		} catch (SvException e) {
			result = e.getLabelCode();
			log4j.error("Error occured in generateAnimals ws (ApplicationServices.java):" + e.getFormattedMessage(), e);
		} finally {
			if (svr != null) {
				svr.release();
			}
			if (svw != null) {
				svw.release();
			}
		}
		return Response.status(200).entity(result).build();
	}

	@Path("/generateAnimals/{sessionId}/{objectId}/{startEarTagId}/{endEarTagId}/{animalClass}/{animalBreed}/{animalGender}/{animalBirthDate}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response generateAnimals(@PathParam("sessionId") String sessionId, @PathParam("objectId") Long objectId,
			@PathParam("startEarTagId") String startEarTagId, @PathParam("endEarTagId") String endEarTagId,
			@PathParam("animalClass") String animalClass, @PathParam("animalBreed") String animalBreed,
			@PathParam("animalGender") String animalGender, @PathParam("animalBirthDate") String animalBirthDate) {
		String result = Tc.EMPTY_STRING;
		SvReader svr = null;
		SvWriter svw = null;
		Writer wr = null;
		try {
			svr = new SvReader(sessionId);
			svw = new SvWriter(svr);
			svw.setAutoCommit(false);
			wr = new Writer();
			DateTime animalBirthDateConverted = new DateTime(animalBirthDate);
			result = wr.generateAnimalObjects(objectId, startEarTagId, endEarTagId, animalClass, animalBreed,
					animalGender, animalBirthDateConverted, svr, svw);
		} catch (SvException e) {
			result = e.getLabelCode();
			log4j.error("Error occured in generateAnimals ws (ApplicationServices.java):" + e.getFormattedMessage(), e);
		} finally {
			if (svr != null) {
				svr.release();
			}
			if (svw != null) {
				svw.release();
			}
		}
		return Response.status(200).entity(result).build();
	}

	@Path("/createEarTagReplacementAndUpdateAnimalId/{sessionId}/{objectId}/{newEarTag}/{replacementDate}/{reason}/{note}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response createEarTagReplacementAndUpdateAnimalId(@PathParam("sessionId") String sessionId,
			@PathParam("objectId") Long objectId, @PathParam("newEarTag") String newEarTag,
			@PathParam("replacementDate") String replacementDate, @PathParam("reason") String reason,
			@PathParam("note") String note) {
		String result = "naits.info.animalIdNotUpdated";
		SvReader svr = null;
		SvWriter svw = null;
		Writer wr = null;
		DbDataObject earTagReplcObject = null;
		try {
			svr = new SvReader(sessionId);
			svw = new SvWriter(svr);
			// svw.setAutoCommit(false);

			wr = new Writer();
			DbDataObject dboAnimal = svr.getObjectById(objectId, SvReader.getTypeIdByName(Tc.ANIMAL), null);
			if (!dboAnimal.getStatus().equals(Tc.VALID)) {
				throw (new SvException("naits.error.earTagReplacementCanBeDoneOnlyForValidAnimals", svCONST.systemUser,
						null, null));
			}
			// ReentrantLock lock = null;
			try {
				/*
				 * lock = SvLock.getLock(String.valueOf(dboAnimal.getObject_id()), false, 0); if
				 * (lock == null) { throw (new SvException(Tc.objectUsedByOtherSession,
				 * svr.getInstanceUser())); }
				 */
				earTagReplcObject = wr.createEarTagReplcObject(newEarTag, replacementDate, reason, note, objectId, svr);
				svw.saveObject(earTagReplcObject, false);
				if (newEarTag != null && !newEarTag.equals("")) {
					result = wr.updateAnimalEarTagByEarTagReplacement(earTagReplcObject, svr, svw);
				}
			} finally {
				/*
				 * if (lock != null && dboAnimal != null) {
				 * SvLock.releaseLock(String.valueOf(dboAnimal.getObject_id()), lock); }
				 */
			}
			svw.dbCommit();
		} catch (SvException e) {
			result = e.getLabelCode();
			log4j.error("Error occured in createEarTagReplacementAndUpdateAnimalId ws (ApplicationServices.java):"
					+ e.getFormattedMessage(), e);
		} finally {
			if (svr != null) {
				svr.release();
			}
			if (svw != null) {
				svw.release();
			}
		}
		return Response.status(200).entity(result).build();
	}

	@Path("/assignOrgUnitToUser/{sessionId}/{objectId}/{userObjId}/{orgUnitObjId}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response assignOrgUnitToUser(@PathParam("sessionId") String sessionId,
			@PathParam("userObjId") Long userObjId, @PathParam("orgUnitObjId") Long orgUnitObjId) {
		String result = "naits.error.assignOrgUnitToUserNotCompleted";
		SvReader svr = null;
		UserManager um = null;
		try {
			svr = new SvReader(sessionId);
			um = new UserManager();
			DbDataObject dboUser = svr.getObjectById(userObjId, svCONST.OBJECT_TYPE_USER, null);
			DbDataObject dboOrgUnit = svr.getObjectById(orgUnitObjId, svCONST.OBJECT_TYPE_ORG_UNITS, null);
			if (dboUser != null && dboOrgUnit != null) {
				ReentrantLock lock = null;
				try {
					lock = SvLock.getLock(String.valueOf(dboUser.getObject_id()), false, 0);
					if (lock == null) {
						throw (new SvException(Tc.objectUsedByOtherSession, svr.getInstanceUser()));
					}
					result = um.createPOABetweenUserAndOrgUnit(dboUser, dboOrgUnit, svr);
				} finally {
					if (lock != null && dboUser != null) {
						SvLock.releaseLock(String.valueOf(dboUser.getObject_id()), lock);
					}
				}
			}
		} catch (SvException e) {
			result = e.getFormattedMessage();
			log4j.error("Error occured in assignOrgUnitToUser ws (ApplicationServices.java):" + e.getFormattedMessage(),
					e);
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
		return Response.status(200).entity(result).build();
	}

	@Path("/getAssignedLabsPerLaborant/{sessionId}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response getAssignedLabsPerLaborant(@PathParam("sessionId") String sessionId) {
		String result = Tc.EMPTY_ARRAY_STRING;
		SvReader svr = null;
		Reader rdr = new Reader();
		try {
			svr = new SvReader(sessionId);
			result = rdr.getLaboratoriesAssignedToUserBySessionId(svr);
		} catch (SvException e) {
			result = e.getFormattedMessage();
			log4j.error("Error occured in assignOrgUnitToUser ws (ApplicationServices.java):" + e.getFormattedMessage(),
					e);
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
		return Response.status(200).entity(result).build();
	}

	@Path("/createPopulationFilter/{sessionId}/{populationId}/{filterId}/{value}/{note}")
	@POST
	@Produces("text/html;charset=utf-8")
	public Response createPopulationFilter(@PathParam("sessionId") String sessionId,
			@PathParam("populationId") Long populationId, @PathParam("filterId") Long filterId,
			@PathParam("value") String filterValue, @PathParam("note") String filterNote) {
		String result = "naits.error.customCreatePopulationFilterError";
		SvWriter svw = null;
		Writer wr = null;
		try {
			svw = new SvWriter(sessionId);
			wr = new Writer();
			ReentrantLock lock = null;
			try {
				lock = SvLock.getLock(String.valueOf(populationId), false, 0);
				if (lock == null) {
					throw (new SvException(Tc.objectUsedByOtherSession, svw.getInstanceUser()));
				}
				DbDataObject dboPopulationFilter = wr.createPopulationFilter(populationId, filterId, filterValue,
						filterNote, svw);
				svw.saveObject(dboPopulationFilter, true);
				result = "naits.success.customCreatePopulationFilter";
			} finally {
				if (lock != null) {
					SvLock.releaseLock(String.valueOf(populationId), lock);
				}
			}
		} catch (SvException e) {
			result = e.getFormattedMessage();
			log4j.error(
					"Error occured in createPopulationFilter ws (ApplicationServices.java):" + e.getFormattedMessage(),
					e);
		} finally {
			if (svw != null) {
				svw.release();
			}
		}
		return Response.status(200).entity(result).build();
	}

	@Path("/changeHoldingStatus/{sessionId}/{holdingObjId}/{newStatus}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response changeHoldingStatus(@PathParam("sessionId") String sessionId,
			@PathParam("holdingObjId") Long holdingObjId, @PathParam("newStatus") String newStatus) {
		String result = "naits.error.holdingStatusWasNotChanged";
		SvReader svr = null;
		SvGeometry svg = null;
		try {
			svr = new SvReader(sessionId);
			svr.setIncludeGeometries(true);
			svg = new SvGeometry(svr);
			svg.setAllowNullGeometry(true);
			DbDataObject dboHolding = svr.getObjectById(holdingObjId, SvReader.getTypeIdByName(Tc.HOLDING), null);
			if (!dboHolding.getStatus().equals(newStatus)) {
				ReentrantLock lock = null;
				try {
					lock = SvLock.getLock(String.valueOf(dboHolding.getObject_id()), false, 0);
					if (lock == null) {
						throw (new SvException(Tc.objectUsedByOtherSession, svr.getInstanceUser()));
					}
					dboHolding.setStatus(newStatus);
					svg.saveGeometry(dboHolding);
					result = "naits.success.holdingStatusWasSuccessfullyChanged";
				} finally {
					if (lock != null && dboHolding != null) {
						SvLock.releaseLock(String.valueOf(dboHolding.getObject_id()), lock);
					}
				}
			} else {
				result = "naits.info.holdingAlreadyHasThisStatus";
			}

		} catch (SvException e) {
			result = e.getFormattedMessage();
			log4j.error("Error occured in changeHoldingStatus ws (ApplicationServices.java):" + e.getFormattedMessage(),
					e);
		} finally {
			if (svr != null) {
				svr.release();
			}
			if (svg != null) {
				svg.release();
			}
		}
		return Response.status(200).entity(result).build();
	}

	@Path("/getAnimalByBarCode/{sessionId}/{animaBarCodeId}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response getAnimalByBarCode(@PathParam("sessionId") String sessionId,
			@PathParam("animaBarCodeId") String animaBarCodeId) {
		String result = Tc.EMPTY_ARRAY_STRING;
		SvReader svr = null;
		Reader rdr = null;
		try {
			svr = new SvReader(sessionId);
			rdr = new Reader();
			// rdr instance of this service can be additionally completed,
			// depending on the client requirements
			if (animaBarCodeId != null && !animaBarCodeId.equals("null")) {
				DbDataObject animalFound = rdr.getAnimalByBarCode(animaBarCodeId, svr);
				if (animalFound != null) {
					DbDataArray array = new DbDataArray();
					array.addDataItem(animalFound);
					result = rdr.convertDbDataArrayToGridJson(array, Tc.ANIMAL);
				}
			}
		} catch (SvException e) {
			log4j.error("Error occured in checkIfAnimalIdExist:" + e.getFormattedMessage(), e);
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
		return Response.status(200).entity(result).build();
	}

	@Path("/getAnimalByReplacedTagId/{sessionId}/{replacedTagId}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response getAnimalByReplacedTagId(@PathParam("sessionId") String sessionId,
			@PathParam("replacedTagId") String replacedTagId) {
		String result = Tc.EMPTY_ARRAY_STRING;
		SvReader svr = null;
		Reader rdr = null;
		try {
			svr = new SvReader(sessionId);
			rdr = new Reader();
			// rdr instance of this service can be additionally completed,
			// depending on the client requirements
			if (replacedTagId != null && !replacedTagId.equals("null")) {
				DbDataObject animalFound = rdr.findAnimalByOldEarTag(replacedTagId, svr);
				if (animalFound != null) {
					DbDataArray array = new DbDataArray();
					array.addDataItem(animalFound);
					result = rdr.convertDbDataArrayToGridJson(array, Tc.ANIMAL);
				}
			}
		} catch (SvException e) {
			log4j.error("Error occured in checkIfAnimalIdExist:" + e.getFormattedMessage(), e);
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
		return Response.status(200).entity(result).build();
	}

	@Path("/searchAppropriateHoldings/{sessionId}/{columnName}/{columnValue}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response searchAppropriateHoldings(@PathParam("sessionId") String sessionId,
			@PathParam("columnName") String columnName, @PathParam("columnValue") String columnValue) {
		String result = Tc.EMPTY_ARRAY_STRING;
		SvReader svr = null;
		Reader rdr = null;
		try {
			svr = new SvReader(sessionId);
			rdr = new Reader();
			DbDataArray holdings = rdr.findAppropriateHoldingBySearch(columnName, columnValue, svr);
			result = rdr.convertDbDataArrayToGridJson(holdings, Tc.HOLDING);
		} catch (SvException e) {
			log4j.error("Error occured in checkIfAnimalIdExist:" + e.getFormattedMessage(), e);
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
		return Response.status(200).entity(result).build();
	}

	@Path("/updateStatus/{sessionId}/{tableName}/{nextStatus}")
	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces("text/html;charset=utf-8")
	public Response updateStatus(@PathParam("sessionId") String sessionId, @PathParam("tableName") String tableName,
			@PathParam("nextStatus") String nextStatus, MultivaluedMap<String, String> formVals,
			@Context HttpServletRequest httpRequest) {
		String resultMsgLbl = "naits.error.updatingStatus";
		JsonObject jsonData = null;
		Gson gson = null;
		MassActions massAction = null;
		try {
			gson = new Gson();
			massAction = new MassActions();
			for (Entry<String, List<String>> entry : formVals.entrySet()) {
				if (entry.getKey() != null && !entry.getKey().isEmpty()) {
					String key = entry.getKey();
					jsonData = gson.fromJson(key, JsonObject.class);
				}
			}
			if (jsonData != null && jsonData.has(Tc.OBJ_ARRAY)) {
				resultMsgLbl = massAction.updateStatusMassAction(tableName, nextStatus,
						jsonData.get(Tc.OBJ_ARRAY).getAsJsonArray(), sessionId);
			} else {
				resultMsgLbl = Tc.error_admConsoleBadJson;
			}
		} catch (Exception e) {
			resultMsgLbl = "naits.error.general";
			log4j.error("General error in processing mass action", e);
		}
		return Response.status(200).entity(resultMsgLbl).build();
	}

	@Path("/changeAppropriateColor/{sessionId}/{holdingObjId}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response changeAppropriateColor(@PathParam("sessionId") String sessionId,
			@PathParam("holdingObjId") Long holdingObjId) {
		String result = Tc.EMPTY_STRING;
		SvSecurity svs = null;
		SvReader svr = null;
		Reader rdr = null;
		ValidationChecks vc = null;
		try {
			svs = new SvSecurity();
			((SvCore) svs).switchUser(svCONST.serviceUser);
			svr = new SvReader(svs);
			rdr = new Reader();
			vc = new ValidationChecks();

			HashMap<String, Object> mapObj = new HashMap<>();
			mapObj.put(Tc.COLOR_HEALTH_STATUS, rdr.calculateHoldingHealth(holdingObjId, svr));
			mapObj.put(Tc.COLOR_QUARANTINE_STATUS, "0");
			mapObj.put(Tc.COLOR_MOVEMENT_STATUS, "1");
			if (vc.checkIfHoldingBelongsInActiveQuarantine(holdingObjId, svr)) {
				mapObj.replace(Tc.COLOR_QUARANTINE_STATUS, "1");
			}
			if (!vc.checkIfMovementIsAllowedPerHolding(holdingObjId, rdr, svr)) {
				mapObj.put(Tc.COLOR_MOVEMENT_STATUS, "0");
			}
			result = new JSONObject(mapObj).toJSONString();
		} catch (SvException e) {
			log4j.error("Error occured in changeAppropriateColor:", e);
		} finally {
			if (svs != null) {
				svs.release();
			}
			if (svr != null) {
				svr.release();
			}
		}
		return Response.status(200).entity(result).build();
	}

	@Path("/checkIfAnimalBelongsToSlaughterhouse/{sessionId}/{object_Id}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response checkIfAnimalBelongsToSlaughterhouse(@PathParam("sessionId") String sessionId,
			@PathParam("object_Id") Long object_Id) {
		ValidationChecks vc = null;
		String resultMsg = Tc.EMPTY_STRING;
		try {
			vc = new ValidationChecks();
			resultMsg = vc.checkIfAnimalBelongsToSlaughterhouse(object_Id, sessionId);
		} catch (Exception e) {
			log4j.error(e.getMessage());
		}
		return Response.status(200).entity(resultMsg).build();
	}

	@Path("/getValidCampaignEvents/{sessionId}/{holdingType}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response getValidCampaignEvents(@PathParam("sessionId") String sessionId,
			@PathParam("holdingType") String holdingType) {
		String result = Tc.EMPTY_STRING;
		SvReader svr = null;
		Reader rdr = null;
		try {
			svr = new SvReader(sessionId);
			rdr = new Reader();
			DbDataArray arrValidVaccinations = rdr.getValidVaccEvents(svr, holdingType);
			result = rdr.convertDbDataArrayToGridJson(arrValidVaccinations, Tc.VACCINATION_EVENT);
		} catch (SvException e) {
			result = e.getLabelCode();
			log4j.error("Error in getValidCampaignEvents: " + ((SvException) e).getFormattedMessage(), e);
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
		return Response.status(200).entity(result).build();
	}

	@Path("/assignNotificationToUserOrUserGroup/{sessionId}/{userOrGroupObjId}/{actionName}")
	@POST
	@Produces("text/html;charset=utf-8")
	public Response assignNotificationToUserOrUserGroup(@PathParam("sessionId") String sessionId,
			@PathParam("userOrGroupObjId") String userOrGroupName, @PathParam("actionName") String actionName,
			MultivaluedMap<String, String> formVals, @Context HttpServletRequest httpRequest) {
		String result = "naits.error.updatingStatus";
		JsonObject jsonData = null;
		Gson gson = null;
		MassActions massAction = null;
		String decodeUserOrUserName = Tc.EMPTY_STRING;
		try {
			gson = new Gson();
			massAction = new MassActions();
			for (Entry<String, List<String>> entry : formVals.entrySet()) {
				if (entry.getKey() != null && !entry.getKey().isEmpty()) {
					String key = entry.getKey();
					jsonData = gson.fromJson(key, JsonObject.class);
				}
			}
			if (jsonData != null && jsonData.has(Tc.OBJ_ARRAY)) {
				decodeUserOrUserName = URLDecoder.decode(userOrGroupName.trim(), "UTF-8");
				result = massAction.assignOrUnassignNotification(decodeUserOrUserName, actionName,
						jsonData.get(Tc.OBJ_ARRAY).getAsJsonArray(), sessionId);
			} else {
				result = Tc.error_admConsoleBadJson;
			}
		} catch (Exception e) {
			result = "naits.error.general";
			log4j.error("General error in processing mass action", e);
		}
		return Response.status(200).entity(result).build();
	}

	@Path("/inventoryDirectTransfer/{sessionId}/{transferObjId}")
	@POST
	@Produces("text/html;charset=utf-8")
	public Response moveInventoryItemToOrgUnit2(@PathParam("sessionId") String sessionId,
			@PathParam("transferObjId") Long transferObjId, @Context HttpServletRequest httpRequest) {
		String resultMsgLbl = "naits.error.inventoryDirectTransfer";
		MassActions massAction = new MassActions();
		try {
			resultMsgLbl = massAction.inventoryDirectTransfer(transferObjId, sessionId);
		} catch (Exception e) {
			resultMsgLbl = e.getMessage();
			log4j.error("General error in processing mass action: ", e);
		}
		return Response.status(200).entity(resultMsgLbl).build();
	}

	@Path("/checkIfRangesOverlap/{sessionId}/{tableName}/{parentId}/{tagType}/{startTagId}/{endTagId}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response checkIfRangesOverlap(@PathParam("sessionId") String sessionId,
			@PathParam("tableName") String tableName, @PathParam("parentId") Long parentId,
			@PathParam("tagType") String tagType, @PathParam("startTagId") Long startTagId,
			@PathParam("endTagId") Long endTagId) {
		String resultMsgLbl = "null";
		SvReader svr = null;
		Reader rdr = null;
		DbDataArray overlappedRanges = null;
		try {
			svr = new SvReader(sessionId);
			rdr = new Reader();
			if (tagType != null && !tagType.equals("2") && startTagId != null && endTagId != null
					&& startTagId.toString().length() != 0 && endTagId.toString().length() != 0
					&& endTagId.toString().length() >= startTagId.toString().length()) {
				overlappedRanges = rdr.getOverlappingRanges(tableName.toUpperCase(), parentId, tagType, startTagId,
						endTagId, false, svr);
				if (overlappedRanges != null && !overlappedRanges.getItems().isEmpty()) {
					resultMsgLbl = "naits.error.ovelappingRange";
				}
			}
		} catch (SvException e) {
			resultMsgLbl = e.getLabelCode();
			log4j.error("General error in processing mass action", e);
		}
		return Response.status(200).entity(resultMsgLbl).build();
	}

	@Path("/getValidOrgUnitsDependOnParentOrgUnit/{sessionId}/{externalId}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response getValidOrgUnitsDependOnParentOrgUnit(@PathParam("sessionId") String sessionId,
			@PathParam("externalId") String externalId) {
		String result = Tc.EMPTY_ARRAY_STRING;
		SvReader svr = null;
		Reader rdr = null;
		try {
			svr = new SvReader(sessionId);
			rdr = new Reader();
			DbDataArray orgUnits = new DbDataArray();
			Long betweenStart = 10L;
			Long betweenEnd = 99L;
			String tableName = Tc.SVAROG_ORG_UNITS;
			if (externalId == null || externalId.equals("null")) {
				orgUnits = rdr.getOrgUnitDependOnParentExternalId(betweenStart, betweenEnd, svr);
			} else {
				Integer geostatCodeLength = externalId.length();
				betweenStart = Long.valueOf(externalId) * 100L;
				betweenEnd = (Long.valueOf(externalId) * 100L) + 99L;
				if (geostatCodeLength.equals(2) || geostatCodeLength.equals(4) || geostatCodeLength.equals(6)) {
					orgUnits = rdr.getOrgUnitDependOnParentExternalId(betweenStart, betweenEnd, svr);
				} else if (geostatCodeLength.equals(8)) {
					tableName = Tc.HOLDING;
					orgUnits = rdr.findDataPerSingleFilter(Tc.VILLAGE_CODE, externalId, DbCompareOperand.EQUAL,
							SvReader.getTypeIdByName(tableName), svr);
				}
			}
			result = rdr.convertDbDataArrayToGridJson(orgUnits, tableName, svr);
		} catch (SvException e) {
			result = e.getLabelCode();
			log4j.error("Error while fetching ORG_UNITS in getValidOrgUnitsDependOnParentOrgUnit: ", e);
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
		return Response.status(200).entity(result).build();
	}

	@Path("/getHoldingsAccordingGeostatCode/{sessionId}/{geostatCode}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response getHoldingsAccordingGeostatCode(@PathParam("sessionId") String sessionId,
			@PathParam("geostatCode") String geostatCode) {
		return getObjectsByLocation(sessionId, Tc.HOLDING, geostatCode);
	}

	@Path("/getHoldingResponsiblesAccordingGeostatCode/{sessionId}/{geostatCode}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response getHoldingResponsiblesAccordingGeostatCode(@PathParam("sessionId") String sessionId,
			@PathParam("geostatCode") String geostatCode) {
		return getObjectsByLocation(sessionId, Tc.HOLDING_RESPONSIBLE, geostatCode);
	}

	@Path("/getLabSamplesAccordingGeostatCode/{sessionId}/{geostatCode}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response getLabSamplesAccordingGeostatCode(@PathParam("sessionId") String sessionId,
			@PathParam("geostatCode") String geostatCode) {
		return getObjectsByLocation(sessionId, Tc.LAB_SAMPLE, geostatCode);
	}

	@Path("/getObjectsByLocation/{sessionId}/{objectType}/{geostatCode}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response getObjectsByLocation(@PathParam("sessionId") String sessionId,
			@PathParam("objectType") String tableName, @PathParam("geostatCode") String geostatCode) {
		String result = Tc.EMPTY_ARRAY_STRING;
		SvReader svr = null;
		Reader rdr = null;
		try {
			svr = new SvReader(sessionId);
			rdr = new Reader();
			DbCompareOperand dbc = DbCompareOperand.EQUAL;
			DbDataArray objectsFound = new DbDataArray();
			if (tableName.equals(Tc.LAB_SAMPLE)) {
				objectsFound = rdr.findDataPerSingleFilter(Tc.GEOSTAT_CODE, geostatCode, DbCompareOperand.LIKE,
						SvReader.getTypeIdByName(tableName), 10000, svr);
			} else if (tableName.equals(Tc.SVAROG_ORG_UNITS)) {
				objectsFound = rdr.findDataPerSingleFilter(Tc.EXTERNAL_ID + "::VARCHAR", geostatCode,
						DbCompareOperand.EQUAL, SvReader.getTypeIdByName(tableName), 10000, svr);
			} else {
				objectsFound = rdr.getObjectsByLocation(tableName, geostatCode, dbc, svr);
			}
			if (!objectsFound.getItems().isEmpty()) {
				result = rdr.convertDbDataArrayToGridJson(objectsFound, tableName);
			}
		} catch (SvException e) {
			result = e.getLabelCode();
			log4j.error("Error while fetching LAB_SAMPLE in getLabSamplesAccordingGeostatCode: ", e);
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
		return Response.status(200).entity(result).build();
	}

	@Path("/applyAvailableInventoryItemsOnValidAvailableAnimals/{sessionId}/{holdingObjId}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response applyAvailableInventoryItemsOnValidAvailableAnimals(@PathParam("sessionId") String sessionId,
			@PathParam("holdingObjId") Long holdingObjId) {
		String result = Tc.EMPTY_STRING;
		SvReader svr = null;
		SvWriter svw = null;
		Writer wr = null;
		try {
			svr = new SvReader(sessionId);
			svr.setAutoCommit(false);
			svw = new SvWriter(svr);
			wr = new Writer();
			ReentrantLock lock = null;
			try {
				lock = SvLock.getLock(String.valueOf(holdingObjId), false, 0);
				if (lock == null) {
					throw (new SvException(Tc.objectUsedByOtherSession, svr.getInstanceUser()));
				}
				result = wr.applyAvailableUnappliedInventoryItemsOnValidAnimals(holdingObjId, svr, svw);
			} finally {
				if (lock != null) {
					SvLock.releaseLock(String.valueOf(holdingObjId), lock);
				}
			}
		} catch (SvException e) {
			result = e.getLabelCode();
			log4j.error("Error while applying inventory ear tags on animals: ", e);
		} finally {
			if (svr != null) {
				svr.release();
			}
			if (svw != null) {
				svw.release();
			}
		}
		return Response.status(200).entity(result).build();
	}

	@Path("/setNoteDescription/{sessionId}/{objectId}/{noteName}")
	@POST
	@Produces("text/html;charset=utf-8")
	public Response setNoteDescription(@PathParam("sessionId") String sessionId, @PathParam("objectId") Long objectId,
			@PathParam("noteName") String noteName, MultivaluedMap<String, String> formVals,
			@Context HttpServletRequest httpRequest) {
		String resultMsgLbl = "naits.error.setNoteDescription";
		SvReader svr = null;
		SvWriter svw = null;
		SvNote svn = null;
		DbDataObject dboNote = null;
		JsonObject jsonData = null;
		Gson gson = null;
		String noteText = Tc.EMPTY_STRING;
		try {
			svr = new SvReader(sessionId);
			svw = new SvWriter(svr);
			svn = new SvNote(svw);
			gson = new Gson();
			Reader rdr = new Reader();
			for (Entry<String, List<String>> entry : formVals.entrySet()) {
				if (entry.getKey() != null && !entry.getKey().isEmpty()) {
					String key = entry.getKey();
					jsonData = gson.fromJson(key, JsonObject.class);
				}
			}
			if (jsonData.has("noteText")) {
				noteText = jsonData.get("noteText").getAsString();
				resultMsgLbl = "naits.success.setNoteDescription";
			}
			if (!noteText.trim().equalsIgnoreCase("")) {
				String existingNoteText = svn.getNote(objectId, noteName);
				ReentrantLock lock = null;
				try {
					lock = SvLock.getLock(String.valueOf(objectId), false, 0);
					if (lock == null) {
						throw (new SvException(Tc.objectUsedByOtherSession, svr.getInstanceUser()));
					}
					boolean noteExist = !existingNoteText.trim().equals("") ? true : false;
					if (noteExist) {
						if (!existingNoteText.equals(noteText)) {
							dboNote = rdr.getNotesAccordingParentIdAndNoteName(objectId, noteName, svr).get(0);
							dboNote.setVal(Tc.NOTE_TEXT, noteText);
							svw.saveObject(dboNote, false);
						}
					} else {
						svn.setNote(objectId, noteName, noteText, true);
					}
					svw.dbCommit();
				} finally {
					if (lock != null) {
						SvLock.releaseLock(String.valueOf(objectId), lock);
					}
				}
			}
		} catch (Exception e) {
			resultMsgLbl = e.getMessage();
			log4j.error("General error in setting note / setNoteDescription", e);
		} finally {
			if (svr != null)
				svr.release();
			if (svw != null)
				svw.release();
			if (svn != null)
				svn.release();
		}
		return Response.status(200).entity(resultMsgLbl).build();
	}

	@Path("/getNoteDescription/{sessionId}/{objectId}/{noteName}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response getNoteDescription(@PathParam("sessionId") String sessionId, @PathParam("objectId") Long objectId,
			@PathParam("noteName") String noteName, @Context HttpServletRequest httpRequest) {
		String resultMsgLbl = "naits.error.getNoteDescription";
		SvReader svr = null;
		SvNote svn = null;
		try {
			svr = new SvReader(sessionId);
			svn = new SvNote(svr);
			resultMsgLbl = svn.getNote(objectId, noteName);
		} catch (Exception e) {
			resultMsgLbl = e.getMessage();
			log4j.error("General error in fetching note / getNoteDescription", e);
		} finally {
			if (svr != null)
				svr.release();
			if (svn != null)
				svn.release();
		}
		return Response.status(200).entity(resultMsgLbl).build();
	}

	@Path("/getTargetedMunicipalitiesWithCampaign/{sessionId}/{objectId}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response getTargetedMunicipalitiesWithCampaign(@PathParam("sessionId") String sessionId,
			@PathParam("objectId") Long objectId, @Context HttpServletRequest httpRequest) {
		String resultMsg = Tc.EMPTY_STRING;
		SvReader svr = null;
		Reader rdr = null;
		try {
			svr = new SvReader(sessionId);
			rdr = new Reader();
			DbDataObject dboVaccinationEvent = svr.getObjectById(objectId,
					SvReader.getTypeIdByName(Tc.VACCINATION_EVENT), null);
			DbDataArray resultMunicipalityUnits = rdr.getCampaignTargetedMunicipalityUnits(dboVaccinationEvent, svr);
			resultMunicipalityUnits.getSortedItems(Tc.NAME);
			resultMsg = rdr.convertDbDataArrayToGridJson(resultMunicipalityUnits, Tc.SVAROG_ORG_UNITS, svr);
		} catch (Exception e) {
			resultMsg = e.getMessage();
			log4j.error("General error in fetching targetet munics / getTargetedMunicipalitiesWithCampaign", e);
		} finally {
			if (svr != null)
				svr.release();
		}
		return Response.status(200).entity(resultMsg).build();
	}

	@Path("/customSearchForInventoryItemByEarTag/{sessionId}/{animalEarTag}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response customSearchForInventoryItemByEarTag(@PathParam("sessionId") String sessionId,
			@PathParam("animalEarTag") String animalEarTag, @Context HttpServletRequest httpRequest) {
		String result = Tc.EMPTY_ARRAY_STRING;
		SvReader svr = null;
		Reader rdr = null;
		try {
			svr = new SvReader(sessionId);
			rdr = new Reader();
			DbDataArray resultSet = rdr.getUnappliedInventoryItemByAnimalEarTagNumber(animalEarTag, svr);
			result = rdr.convertDbDataArrayToGridJson(resultSet, Tc.INVENTORY_ITEM);
		} catch (Exception e) {
			result = e.getMessage();
			log4j.error("General error in fetching targetet munics / getTargetedMunicipalitiesWithCampaign", e);
		} finally {
			if (svr != null)
				svr.release();
		}
		return Response.status(200).entity(result).build();
	}

	@Path("/massPetAction/{sessionId}")
	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces("text/html;charset=utf-8")
	public Response massPetAction(@PathParam("sessionId") String sessionId, MultivaluedMap<String, String> formVals,
			@Context HttpServletRequest httpRequest) {
		String resultMsgLbl = "naits.error.massAnimalsAction";
		JsonObject jsonData = null;
		Gson gson = new Gson();
		MassActions massAct = new MassActions();
		try {
			for (Entry<String, List<String>> entry : formVals.entrySet()) {
				if (entry.getKey() != null && !entry.getKey().isEmpty()) {
					String key = entry.getKey();
					jsonData = gson.fromJson(key, JsonObject.class);
				}
			}
			if (jsonData != null && jsonData.has(Tc.OBJ_ARRAY)) {
				resultMsgLbl = massAct.massObjectHandler(jsonData, sessionId);
			} else {
				resultMsgLbl = Tc.error_admConsoleBadJson;
			}
		} catch (SvException e) {
			log4j.error("Error in assignResultToLabSample: " + ((SvException) e).getFormattedMessage(), e);
		} catch (Exception e) {
			log4j.error("General error in assignResultToLabSample", e);
		}
		return Response.status(200).entity(resultMsgLbl).build();
	}

	@Path("/customSaveAnimalObject/{sessionId}")
	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces("text/html;charset=utf-8")
	public Response customSaveAnimalObject(@PathParam("sessionId") String sessionId,
			MultivaluedMap<String, String> formVals, @Context HttpServletRequest httpRequest) {
		String resultMsgLbl = "naits.error.createCustomAnimalObject";
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
			if (jsonData != null && jsonData.has(Tc.OBJ_ARRAY)) {
				DbDataObject dboAnimal = wr.createAnimalObject(jsonData.get(Tc.OBJ_ARRAY).getAsJsonArray(), sessionId);
				if (dboAnimal != null) {
					resultMsgLbl = "naits.main.forms.data_save_success";
				} else {
					resultMsgLbl = "naits.main.forms.data_save_error";
				}
			} else {
				resultMsgLbl = Tc.error_admConsoleBadJson;
			}
		} catch (Exception e) {
			resultMsgLbl = "naits.error.general";
			log4j.error("General error in processing mass action", e);
		}
		return Response.status(200).entity(resultMsgLbl).build();
	}

	@Path("/getOrgUnitToSimpleJsonByObjectId/{sessionId}/{objectId}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response getOrgUnitToSimpleJsonByObjectId(@PathParam("sessionId") String sessionId,
			@PathParam("objectId") Long objectId) {
		SvReader svr = null;
		String result = Tc.EMPTY_STRING;
		try {
			svr = new SvReader(sessionId);
			DbDataObject dboOrgUnit = svr.getObjectById(objectId, svCONST.OBJECT_TYPE_ORG_UNITS, null);
			if (dboOrgUnit != null && dboOrgUnit.getVal(Tc.NAME) != null)
				result = dboOrgUnit.toSimpleJson().toString();
		} catch (SvException e) {
			log4j.error(Tc.ERROR_DETECTED + e.getFormattedMessage(), e);
			result = e.getFormattedMessage();
		} finally {
			if (svr != null)
				svr.release();
		}
		return Response.status(200).entity(result).build();
	}

	@Path("/getPetByPetPassportOrOwnerId/{sessionId}/{searchObjType}/{passportOrOwnerId}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response getPetByPetPassportId(@PathParam("sessionId") String sessionId,
			@PathParam("searchObjType") String searchObjType,
			@PathParam("passportOrOwnerId") String passportOrOwnerId) {
		SvReader svr = null;
		Reader rdr = null;
		DbDataArray result = null;
		String resultMsg = Tc.EMPTY_ARRAY_STRING;
		try {
			svr = new SvReader(sessionId);
			rdr = new Reader();
			result = new DbDataArray();
			DbDataObject dboPetPassportOrOwner = null;
			if (searchObjType.equals(Tc.PET_PASSPORT)) {
				dboPetPassportOrOwner = rdr.searchForObject(SvReader.getTypeIdByName(Tc.HEALTH_PASSPORT),
						Tc.PASSPORT_ID, passportOrOwnerId, svr);
				if (dboPetPassportOrOwner != null) {
					DbDataObject dboPet = svr.getObjectById(dboPetPassportOrOwner.getParent_id(),
							SvReader.getTypeIdByName(Tc.PET), null);
					if (dboPet != null) {
						result.addDataItem(dboPet);
					}
				}
			} else {
				dboPetPassportOrOwner = rdr.getDboPersonByPrivateNumber(passportOrOwnerId, svr);
				if (dboPetPassportOrOwner != null) {
					result = rdr.getPetsByDboOwner(dboPetPassportOrOwner, svr);
				}
			}
			resultMsg = rdr.convertDbDataArrayToGridJson(result, Tc.PET);
		} catch (SvException e) {
			log4j.error(Tc.ERROR_DETECTED + e.getFormattedMessage(), e);
		} finally {
			if (svr != null)
				svr.release();
		}
		return Response.status(200).entity(resultMsg).build();
	}

	@Path("/checkIfHoldingBelongsToActiveQuarantineOrAffectedArea/{sessionId}/{objectId}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response checkIfHoldingBelongsToActiveQuarantineOrAffectedArea(@PathParam("sessionId") String sessionId,
			@PathParam("objectId") Long objectId) {
		SvReader svr = null;
		String result = Tc.EMPTY_STRING;
		ValidationChecks vc = null;
		try {
			svr = new SvReader(sessionId);
			vc = new ValidationChecks();

			JsonObject jObj = null;
			DbDataObject dboHolding = svr.getObjectById(objectId, SvReader.getTypeIdByName(Tc.HOLDING), null);

			if (dboHolding != null) {
				jObj = new JsonObject();
				String isInAffectedArea = vc.checkIfHoldingBelongsToAffectedArea(dboHolding, svr).toString();
				String isInActiveQuarantine = vc
						.checkIfHoldingBelongsInActiveSpecificQuarantine(objectId, Tc.BLACKLIST_QUARANTINE, svr)
						.toString();
				jObj.addProperty("isInAffectedArea", isInAffectedArea);
				jObj.addProperty("isInActiveQuarantine", isInActiveQuarantine);
				result = jObj.toString();
			}
		} catch (SvException e) {
			log4j.error(Tc.ERROR_DETECTED + e.getFormattedMessage(), e);
			result = e.getFormattedMessage();
		} finally {
			if (svr != null)
				svr.release();
		}
		return Response.status(200).entity(result).build();
	}

	@Path("/getLinkedHoldingsPerUser/{sessionId}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response getPicPerUser(@PathParam("sessionId") String sessionId) {
		SvReader svr = null;
		String result = Tc.EMPTY_ARRAY_STRING;
		Reader rdr = null;
		UserManager um = null;
		try {
			svr = new SvReader(sessionId);
			rdr = new Reader();
			um = new UserManager();
			Boolean hasPermission = false;
			DbDataObject dboUser = SvReader.getUserBySession(sessionId);
			ArrayList<String> currentCustomPermissions = um.getCustomPermissionForUserOrGroup(dboUser, svr);
			if (!currentCustomPermissions.isEmpty()) {
				for (int i = 0; i < currentCustomPermissions.size(); i++) {
					if (currentCustomPermissions.get(i).startsWith(Tc.HOLDING)) {
						hasPermission = true;
						break;
					}
				}
				if (hasPermission) {
					DbDataObject dboHoldingDesc = SvReader.getDbtByName(Tc.HOLDING);
					if (dboUser != null) {
						DbDataArray linkedHoldings = rdr
								.getLinkedTablesWithGeoStatCodePerUser(dboHoldingDesc.getObject_id(), svr);
						result = rdr.convertDbDataArrayToGridJson(linkedHoldings,
								dboHoldingDesc.getVal(Tc.TABLE_NAME).toString());
					}
				}
			}
		} catch (SvException e) {
			log4j.error(Tc.ERROR_DETECTED + e.getFormattedMessage(), e);
			result = e.getFormattedMessage();
		} finally {
			if (svr != null)
				svr.release();
		}
		return Response.status(200).entity(result).build();
	}

	/**
	 * Web service to return any table using one filter "ilike" for one field only ,
	 * it will also return svarog repo fields DEDICATE for: case insensitive search
	 * 
	 * @param sessionId  Session ID (SID) of the web communication between browser
	 *                   and web server
	 * @param table_name String table from which we want to get data
	 * @param fieldNAme  String name of the field that we try to filter
	 * @param fieldValue String value that we are trying to find, will be cast to
	 *                   Integer for numeric values
	 * @param no_rec     Integer how many records we want to pull from the table
	 * 
	 * @return Json with all objects found
	 */
	@Path("/getTableWithILike/{session_id}/{table_name}/{fieldNAme}/{fieldValue}/{no_rec}")
	@GET
	@Produces("application/json")
	public Response getTableWithILike(@PathParam("session_id") String sessionId,
			@PathParam("table_name") String tableName, @PathParam("fieldNAme") String fieldName,
			@PathParam("fieldValue") String fieldValue, @PathParam("no_rec") Integer recordNumber,
			@Context HttpServletRequest httpRequest) {
		SvReader svr = null;
		String retString = Tc.EMPTY_STRING;
		String fieldWithSpecialCharacter = fieldValue.trim();
		String[] tablesUsedArray = new String[1];
		Boolean[] tableShowArray = new Boolean[1];
		int tablesusedCount = 1;
		try {
			svr = new SvReader(sessionId);
			Reader rdr = new Reader();
			Long tableID = SvReader.getDbtByName(tableName.toUpperCase()).getObject_id();
			if (fieldValue != null && fieldValue.contains("%2F")) {
				fieldWithSpecialCharacter = java.net.URLDecoder.decode(fieldValue.trim(),
						StandardCharsets.UTF_8.name());
			}
			DbSearchExpression expr = new DbSearchExpression();
			DbSearchCriterion critU = new DbSearchCriterion(fieldName.toUpperCase(), DbCompareOperand.ILIKE,
					'%' + fieldWithSpecialCharacter + '%');
			if (fieldName.toUpperCase().contains("DT") || fieldName.toUpperCase().contains(Tc.DATE)) {
				critU = new DbSearchCriterion(fieldName.toUpperCase(), DbCompareOperand.EQUAL,
						new DateTime(fieldWithSpecialCharacter));
			}
			expr.addDbSearchItem(critU);
			DbDataArray vData = new DbDataArray();
			DbDataObject dboGeoField = rdr.getGeoField(tableID, svr);
			if (dboGeoField != null) {
				vData = rdr.getAllowedObjectsAccordingPOALink(tableName, dboGeoField, critU, recordNumber, svr);
			} else {
				vData = svr.getObjects(expr, tableID, null, recordNumber, 0);
			}
			tablesUsedArray[0] = SvReader.getDbt(tableID).getVal(Tc.TABLE_NAME).toString();
			tableShowArray[0] = true;
			retString = Reader.prapareTableQueryData(vData, tablesUsedArray, tableShowArray, tablesusedCount, true,
					svr);
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage(), e);
			return Response.status(401).entity(e.getFormattedMessage()).build();
		} catch (UnsupportedEncodingException e) {
			log4j.error(e.toString(), e);
			return Response.status(401).entity(e.toString()).build();
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
		return Response.status(200).entity(retString).build();
	}

	@Path("/assignOwnerToStrayPet/{sessionId}/{strayPetId}/{personObjId}/{adoptionDate}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response assignOwnerToPet(@PathParam("sessionId") String sessionId, @PathParam("strayPetId") Long strayPetId,
			@PathParam("personObjId") Long personObjId, @PathParam("adoptionDate") String adoptionDate) {
		SvReader svr = null;
		SvWriter svw = null;
		SvLink svl = null;
		Writer wr = null;
		String result = "naits.error.assignOwnerToStrayPet";
		try {
			svr = new SvReader(sessionId);
			svr.dbSetAutoCommit(false);
			svw = new SvWriter(svr);
			svl = new SvLink(svw);

			wr = new Writer();

			DbDataObject dboStrayPet = svr.getObjectById(strayPetId, SvReader.getTypeIdByName(Tc.STRAY_PET), null);
			DbDataObject dboPerson = svr.getObjectById(personObjId, SvReader.getTypeIdByName(Tc.HOLDING_RESPONSIBLE),
					null);
			if (dboStrayPet != null && dboPerson != null) {
				result = wr.assignOwnerToStrayPet(dboStrayPet, dboPerson, adoptionDate, svl, svw, svr);
				svr.dbCommit();
			}
		} catch (SvException e) {
			log4j.error(Tc.ERROR_DETECTED + e.getFormattedMessage(), e);
			result = e.getLabelCode();
		} finally {
			if (svr != null)
				svr.release();
			if (svw != null)
				svw.release();
			if (svl != null)
				svl.release();
		}
		return Response.status(200).entity(result).build();
	}

	@Path("/sendPassportRequestToVeterinaryStation/{sessionId}/{petObjectId}/{vetStationObjectId}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response sendPassportRequestToVeterinaryStation(@PathParam("sessionId") String sessionId,
			@PathParam("petObjectId") Long petObjectId, @PathParam("vetStationObjectId") Long vetStationObjectId) {
		SvReader svr = null;
		SvWriter svw = null;
		Writer wr = null;
		String result = "naits.error.sendPassportRequestToVeterinaryStation";
		try {
			svr = new SvReader(sessionId);
			svr.dbSetAutoCommit(false);
			svw = new SvWriter(svr);

			wr = new Writer();

			DbDataObject dboPet = svr.getObjectById(petObjectId, SvReader.getTypeIdByName(Tc.PET), null);
			DbDataObject dboVeterinaryStation = svr.getObjectById(vetStationObjectId,
					SvReader.getTypeIdByName(Tc.HOLDING), null);
			if (dboPet != null && dboVeterinaryStation != null) {
				result = wr.sendPassportRequest(dboPet, dboVeterinaryStation, svw, svr);
				svw.dbCommit();
			}
		} catch (SvException e) {
			log4j.error(Tc.ERROR_DETECTED + e.getFormattedMessage(), e);
			result = e.getLabelCode();
		} finally {
			if (svr != null)
				svr.release();
			if (svw != null)
				svw.release();
		}
		return Response.status(200).entity(result).build();
	}

	@Path("/massObjectHandler/{sessionId}")
	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces("text/html;charset=utf-8")
	public Response massObjectHandler(@PathParam("sessionId") String sessionId, MultivaluedMap<String, String> formVals,
			@Context HttpServletRequest httpRequest) {
		String resultMsgLbl = "naits.error.massAnimalsAction";
		JsonObject jsonData = null;
		Gson gson = new Gson();
		MassActions massAct = new MassActions();
		try {
			for (Entry<String, List<String>> entry : formVals.entrySet()) {
				if (entry.getKey() != null && !entry.getKey().isEmpty()) {
					String key = entry.getKey();
					jsonData = gson.fromJson(key, JsonObject.class);
				}
			}
			if (jsonData != null && jsonData.has(Tc.OBJ_ARRAY)) {
				resultMsgLbl = massAct.massObjectHandler(jsonData, sessionId);
			} else {
				resultMsgLbl = Tc.error_admConsoleBadJson;
			}
		} catch (SvException e) {
			resultMsgLbl = ((SvException) e).getLabelCode();
			log4j.error("Error in processing massObjectHandler: " + ((SvException) e).getFormattedMessage(), e);
		} catch (Exception e) {
			log4j.error("General error in processing massObjectHandler:", e);
		}
		return Response.status(200).entity(resultMsgLbl).build();
	}

	@Path("/massAnimalAndFlockAction/{sessionId}")
	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces("text/html;charset=utf-8")
	public Response massAnimalAndFlockAction(@PathParam("sessionId") String sessionId,
			MultivaluedMap<String, String> formVals, @Context HttpServletRequest httpRequest) {
		String resultMsgLbl = "naits.error.massAnimalsAction";
		JsonObject jsonData = null;
		Gson gson = new Gson();
		MassActions massAct = new MassActions();
		try {
			for (Entry<String, List<String>> entry : formVals.entrySet()) {
				if (entry.getKey() != null && !entry.getKey().isEmpty()) {
					String key = entry.getKey();
					jsonData = gson.fromJson(key, JsonObject.class);
				}
			}
			if (jsonData != null && jsonData.has(Tc.OBJ_ARRAY)) {
				resultMsgLbl = massAct.animalFlockMassHandler(jsonData, sessionId);
			} else {
				resultMsgLbl = Tc.error_admConsoleBadJson;
			}
		} catch (SvException e) {
			resultMsgLbl = ((SvException) e).getLabelCode();
			log4j.error("Error in processing massAnimalAndFlockAction:" + ((SvException) e).getFormattedMessage(), e);
		} catch (Exception e) {
			log4j.error("General error in processing massAnimalAndFlockAction:", e);
		}
		return Response.status(200).entity(resultMsgLbl).build();
	}

	@Path("/customSaveVaccinationEvent/{sessionId}")
	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces("text/html;charset=utf-8")
	public Response customSaveVaccinationEvent(@PathParam("sessionId") String sessionId,
			MultivaluedMap<String, String> formVals) {
		String resultMsgLbl = "naits.error.createCustomAnimalObject";
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
			if (jsonData != null && jsonData.has(Tc.OBJ_ARRAY)) {
				DbDataObject dboVaccinationEvent = wr
						.createDboVaccinationEvent(jsonData.get(Tc.OBJ_ARRAY).getAsJsonArray(), sessionId);
				if (dboVaccinationEvent != null) {
					resultMsgLbl = "naits.main.forms.data_save_success";
				} else {
					resultMsgLbl = "naits.main.forms.data_save_error";
				}
			} else {
				resultMsgLbl = Tc.error_admConsoleBadJson;
			}
		} catch (Exception e) {
			log4j.error("General error in processing customSaveVaccinationEvent:", e);
		}
		return Response.status(200).entity(resultMsgLbl).build();
	}

	@Path("/checkIfPetHasValidPassport/{sessionId}/{objectId}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response checkIfPetHasValidPassport(@PathParam("sessionId") String sessionId,
			@PathParam("objectId") Long objectId) {
		SvReader svr = null;
		Reader rdr = null;
		String result = "false";
		try {
			svr = new SvReader(sessionId);
			rdr = new Reader();

			DbDataObject dboPet = svr.getObjectById(objectId, SvReader.getTypeIdByName(Tc.PET), null);
			if (dboPet != null) {
				DbDataObject dboLastValidPassport = rdr.getLastValidHealthPassport(dboPet, null, svr);
				if (dboLastValidPassport != null) {
					result = "true";
				}
			}
		} catch (SvException e) {
			log4j.error(Tc.ERROR_DETECTED + e.getFormattedMessage(), e);
		} finally {
			if (svr != null)
				svr.release();
		}
		return Response.status(200).entity(result).build();
	}

	@Path("/checkIfPetHasInventoryItem/{sessionId}/{objectId}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response checkIfPetHasInventoryItem(@PathParam("sessionId") String sessionId,
			@PathParam("objectId") Long objectId) {
		SvReader svr = null;
		String result = "false";
		try {
			svr = new SvReader(sessionId);
			DbDataObject dboPet = svr.getObjectById(objectId, SvReader.getTypeIdByName(Tc.PET), null);
			if (dboPet != null) {
				DbDataArray arrInventoryItem = svr.getObjectsByParentId(dboPet.getObject_id(),
						SvReader.getTypeIdByName(Tc.INVENTORY_ITEM), null, 0, 0);
				if (!arrInventoryItem.getItems().isEmpty()) {
					result = "true";
				}
			}
		} catch (SvException e) {
			log4j.error(Tc.ERROR_DETECTED + e.getFormattedMessage(), e);
		} finally {
			if (svr != null)
				svr.release();
		}
		return Response.status(200).entity(result).build();
	}

	@Path("/updatePetId/{sessionId}/{objectId}/{updatedPetId}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response updatePetId(@PathParam("sessionId") String sessionId, @PathParam("objectId") Long objectId,
			@PathParam("updatedPetId") String updatedPetId) {
		SvReader svr = null;
		SvWriter svw = null;
		Reader rdr = null;
		Writer wr = null;
		String result = "naits.error.updatePetId";
		try {
			svr = new SvReader(sessionId);
			svw = new SvWriter(svr);

			svw.dbSetAutoCommit(false);

			rdr = new Reader();
			wr = new Writer();

			DbDataObject dboPet = svr.getObjectById(objectId, SvReader.getTypeIdByName(Tc.PET), null);
			if (dboPet != null) {
				if (wr.updatePetId(dboPet, updatedPetId, rdr, svw, svr)) {
					result = "naits.success.updatePetId";
				}
				@SuppressWarnings(Tc.UNUSED)
				DbDataObject refreshPetCache = svr.getObjectById(dboPet.getObject_id(), dboPet.getObject_type(),
						new DateTime());
			}
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
			result = e.getLabelCode();
		} finally {
			if (svr != null)
				svr.release();
			if (svw != null)
				svw.release();
		}
		return Response.status(200).entity(result).build();
	}

	@Path("/hasReportToolPermission/{sessionId}/{permissionType}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response hasReportToolPermission(@PathParam("sessionId") String sessionId,
			@PathParam("permissionType") String permissionType) {
		SvReader svr = null;
		String result = Boolean.FALSE.toString();
		UserManager um = null;
		try {
			svr = new SvReader(sessionId);
			um = new UserManager();
			DbDataObject dboUser = SvReader.getUserBySession(sessionId);
			if (dboUser != null) {
				result = um.checkIfUserHasCustomPermission(dboUser, permissionType.toLowerCase(), svr).toString();
			}
		} catch (SvException e) {
			log4j.error(Tc.ERROR_DETECTED + e.getFormattedMessage(), e);
		} finally {
			if (svr != null)
				svr.release();
		}
		return Response.status(200).entity(result).build();
	}

	@GET
	@Path("/generatePopulationSample/{sessionId}/{objectId}/{fileName}/{sheetName}/{fileSuffix}")
	@Produces("application/vnd.ms-excel")
	public Response generatePopulationSample(@PathParam("sessionId") final String sessionId,
			@PathParam("objectId") final Long objectId, @PathParam("fileName") final String fileName,
			@PathParam("sheetName") final String sheetName, @PathParam("fileSuffix") final String fileSuffix,
			@Context HttpServletRequest httpRequest) {
		StreamingOutput fileStream = new StreamingOutput() {
			@Override
			public void write(java.io.OutputStream output) throws IOException {
				SvReader svr = null;
				SvWriter svw = null;
				Writer wr = null;
				try {
					svr = new SvReader(sessionId);
					svw = new SvWriter(svr);
					svw.setAutoCommit(false);
					wr = new Writer();
					DbDataObject dboPopulation = svr.getObjectById(objectId, SvReader.getTypeIdByName(Tc.POPULATION),
							new DateTime());
					if (dboPopulation.getStatus().equals(Tc.VALID)) {
						dboPopulation.setStatus(Tc.DRAFT);
						svw.saveObject(dboPopulation, false);
						svw.dbCommit();
					} else if (dboPopulation.getStatus().equals(Tc.DRAFT)) {
						DbDataArray arrVillages = svr.getObjectsByParentId(dboPopulation.getObject_id(),
								SvReader.getTypeIdByName(Tc.POPULATION_LOCATION), null, 0, 0);
						if (!arrVillages.getItems().isEmpty()) {
							for (DbDataObject dboPopulationLocation : arrVillages.getItems()) {
								svw.deleteObject(dboPopulationLocation, true);
							}
						}
						svw.dbCommit();
					}
					ByteArrayOutputStream bstr = new ByteArrayOutputStream();
					byte[] xlsData = wr.createExcelFileForSampleState(dboPopulation, sheetName, bstr, svr);
					output.write(xlsData);
					wr.uploadSampleXlsFile(fileName, dboPopulation, "naits.actions.sample_attachment", xlsData, svw,
							svr);
				} catch (Exception e) {
					log4j.error("Error printing PDF/Excel! method: generatePopulationSample", e);
				} finally {
					if (svr != null)
						svr.release();
					if (svw != null)
						svw.release();
					output.close();
				}
			}
		};
		return Response.ok(fileStream, MediaType.APPLICATION_OCTET_STREAM).header("content-disposition",
				"attachment; filename = " + fileName + "_" + fileSuffix + Tc.XLS_EXTENSION).build();
	}

	@GET
	@Path("/downloadSampleFile/{sessionId}/{populationId}/{fileLabelCode}/{fileSuffix}")
	@Produces("application/vnd.ms-excel")
	public Response downloadSampleFile(@PathParam("sessionId") final String sessionId,
			@PathParam("populationId") final Long populationId, @PathParam("fileLabelCode") final String fileLabelCode,
			@PathParam("fileSuffix") final String fileSuffix, @Context HttpServletRequest httpRequest) {
		StreamingOutput fileStream = new StreamingOutput() {
			@Override
			public void write(java.io.OutputStream output) throws IOException {
				SvReader svr = null;
				SvWriter svw = null;
				Writer wr = null;
				try {
					// stratification_attachment
					// sample_attachment
					svr = new SvReader(sessionId);
					svw = new SvWriter(svr);
					svw.setAutoCommit(false);

					wr = new Writer();
					DbDataObject dboPopulation = svr.getObjectById(populationId,
							SvReader.getTypeIdByName(Tc.POPULATION), null);
					if (dboPopulation != null) {
						ByteArrayOutputStream bstr = new ByteArrayOutputStream();
						byte[] data = bstr.toByteArray();
						output.write(data);
						wr.downloadSampleXlsFile(dboPopulation, fileLabelCode, output, svr);
					}
				} catch (Exception e) {
					log4j.error("Error printing PDF/Excel! method: generatePopulationSample", e);
				} finally {
					if (svr != null)
						svr.release();
					if (svw != null)
						svw.release();
					output.close();
				}
			}
		};
		return Response.ok(fileStream, MediaType.APPLICATION_OCTET_STREAM).header("content-disposition",
				"attachment; filename = " + fileLabelCode + "_" + fileSuffix + Tc.XLS_EXTENSION).build();
	}

	@Path("/createLinkBetweenAreaAndPopulation/{sessionId}/{objectId}/{geostatCode}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response createLinkBetweenAreaAndPopulation(@PathParam("sessionId") String sessionId,
			@PathParam("objectId") Long objectId, @PathParam("geostatCode") String geostatCode) {
		SvReader svr = null;
		String result = "naits.error.geoLocationAlreadyLinkedToPopulation";
		Writer wr = null;
		Reader rdr = null;
		try {
			svr = new SvReader(sessionId);

			wr = new Writer();
			rdr = new Reader();

			DbDataObject dboPopulation = svr.getObjectById(objectId, SvReader.getTypeIdByName(Tc.POPULATION), null);
			DbDataObject dboArea = rdr.searchForObject(SvReader.getTypeIdByName(Tc.AREA), Tc.AREA_NAME, geostatCode,
					svr);
			if (dboPopulation != null && dboArea != null) {
				if (dboPopulation.getStatus().equals(Tc.FINAL)) {
					throw (new SvException("naits.error.cannotEditPopulationWithFinalStatus", svCONST.systemUser, null,
							null));
				}
				DbDataObject dboLinkTypeAreaPopulation = SvReader.getLinkType(Tc.AREA_POPULATION,
						SvReader.getTypeIdByName(Tc.AREA), SvReader.getTypeIdByName(Tc.POPULATION));
				DbDataObject dboLinkAreaPopulation = rdr.getLinkObject(dboArea.getObject_id(),
						dboPopulation.getObject_id(), dboLinkTypeAreaPopulation.getObject_id(), svr);
				if (dboLinkAreaPopulation == null) {
					wr.linkObjects(dboArea, dboPopulation, Tc.AREA_POPULATION, "", svr, true);
					result = "naits.success.linkAreaToPopulation";
				}
			}
		} catch (SvException e) {
			result = e.getLabelCode();
			log4j.error(Tc.ERROR_DETECTED + e.getFormattedMessage(), e);
		} finally {
			if (svr != null)
				svr.release();
		}
		return Response.status(200).entity(result).build();
	}

	@Path("/applyStratificationFilter/{sessionId}/{populationId}")
	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces("text/html;charset=utf-8")
	public Response applyStratificationFilter(@PathParam("sessionId") String sessionId,
			@PathParam("populationId") Long populationId, MultivaluedMap<String, String> formVals) {
		String resultMsgLbl = "naits.error.applyStratificationFilter";
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
			if (jsonData != null && jsonData.has(Tc.OBJ_PARAMS)) {
				DbDataObject dboStratFilter = wr.createStratificationFilter(
						jsonData.get(Tc.OBJ_PARAMS).getAsJsonArray(), populationId, sessionId);
				if (dboStratFilter != null) {
					resultMsgLbl = "naits.success.applyStratificationFilter";
				}
			} else {
				resultMsgLbl = Tc.error_admConsoleBadJson;
			}
		} catch (Exception e) {
			if (e instanceof SvException)
				resultMsgLbl = ((SvException) e).getLabelCode();
			log4j.error("General error in processing applyStratificationFilter", e);

		}
		return Response.status(200).entity(resultMsgLbl).build();
	}

	@Path("/getStratificationNumbers/{sessionId}/{populationId}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response getStratificationNumbers(@PathParam("sessionId") String sessionId,
			@PathParam("populationId") Long populationId) {
		String result = Tc.EMPTY_STRING;
		SvReader svr = null;
		Reader rdr = null;
		try {
			svr = new SvReader(sessionId);
			rdr = new Reader();

			DbDataObject dboPopulation = svr.getObjectById(populationId, SvReader.getTypeIdByName(Tc.POPULATION),
					new DateTime());
			Integer numRegions = rdr.getDistinctCountOfGeolocationsAccordingGeostatCode(dboPopulation, Tc.REGION_CODE,
					svr);
			Integer numMunics = rdr.getDistinctCountOfGeolocationsAccordingGeostatCode(dboPopulation, Tc.MUNIC_CODE,
					svr);
			Integer numCommuns = rdr.getDistinctCountOfGeolocationsAccordingGeostatCode(dboPopulation, Tc.COMMUN_CODE,
					svr);
			Integer numVillages = rdr.getDistinctCountOfGeolocationsAccordingGeostatCode(dboPopulation, Tc.VILLAGE_CODE,
					svr);
			HashMap<String, Object> mapObj = new HashMap<String, Object>();
			if (numRegions > 0) {
				mapObj.put("REGIONS", numRegions.toString());
				mapObj.put("MUNICS", numMunics.toString());
				mapObj.put("COMMUNS", numCommuns.toString());
				mapObj.put("VILLAGES", numVillages.toString());
			}
			result = new JSONObject(mapObj).toJSONString();
		} catch (SvException e) {
			log4j.error("Error occured in checkIfAnimalIdExist:" + e.getFormattedMessage(), e);
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
		return Response.status(200).entity(result).build();
	}

	@Path("/checkIfSvFileExists/{sessionId}/{fileName}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response checkIfSvFileExistsByName(@PathParam("sessionId") String sessionId,
			@PathParam("fileName") String fileName) {
		Boolean result = false;
		SvReader svr = null;
		DbDataArray arrResult = new DbDataArray();
		try {
			svr = new SvReader(sessionId);
			DbSearchCriterion cr1 = new DbSearchCriterion(Tc.FILE_NAME, DbCompareOperand.EQUAL, fileName);
			arrResult = svr.getObjects(new DbSearchExpression().addDbSearchItem(cr1), svCONST.OBJECT_TYPE_FILE, null, 0,
					0);
			if (arrResult.getItems().isEmpty()) {
				arrResult = svr.getObjects(new DbSearchExpression().addDbSearchItem(cr1), svCONST.OBJECT_TYPE_FILE,
						new DateTime(), 0, 0);
			}
			if (!arrResult.getItems().isEmpty()) {
				result = true;
			}
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
		return Response.status(200).entity(result.toString()).build();
	}

	@GET
	@Path("/stratifyPopulation/{sessionId}/{populationId}/{fileSuffix}/{objectId}")
	@Produces("application/vnd.ms-excel")
	public Response stratifyPopulation(@PathParam("sessionId") final String sessionId,
			@PathParam("populationId") final Long populationId, @PathParam("fileSuffix") String fileSuffix,
			@PathParam("objectId") Long objectId, @Context HttpServletRequest httpRequest) {
		StreamingOutput fileStream = new StreamingOutput() {
			@Override
			public void write(java.io.OutputStream output) throws IOException {
				SvReader svr = null;
				SvWriter svw = null;
				Writer wr = null;
				try {
					svr = new SvReader(sessionId);
					svw = new SvWriter(svr);
					svw.setAutoCommit(false);
					wr = new Writer();
					DbDataObject dboPopulation = svr.getObjectById(populationId,
							SvReader.getTypeIdByName(Tc.POPULATION), null);
					DbDataObject dboStratFilter = svr.getObjectById(objectId, SvReader.getTypeIdByName(Tc.STRAT_FILTER),
							null);
					ByteArrayOutputStream bstr = new ByteArrayOutputStream();
					byte[] data = wr.createExcelFileAccordingDbDataArray(dboPopulation, dboStratFilter, bstr, svr);
					output.write(data);
					wr.uploadSampleXlsFile("strat_sample", dboPopulation, "naits.actions.stratification_attachment",
							data, svw, svr);
				} catch (Exception e) {
					log4j.error("Error printing PDF/Excel! method: generatePopulationSample", e);
				} finally {
					if (svr != null)
						svr.release();
					if (svw != null)
						svw.release();
					output.close();
				}
			}
		};
		return Response.ok(fileStream, MediaType.APPLICATION_OCTET_STREAM).header("content-disposition",
				"attachment; filename = " + "stratified_sample" + "_" + fileSuffix + Tc.XLS_EXTENSION).build();
	}

	@Path("/getExpiredVaccinationEvents/{sessionId}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response getExpiredVaccinationEvents(@PathParam("sessionId") String sessionId) {
		String result = Tc.EMPTY_STRING;
		SvReader svr = null;
		Reader rdr = null;
		try {
			svr = new SvReader(sessionId);
			rdr = new Reader();
			DbDataArray arrExpiredVaccEvents = rdr.getExpiredVaccinationEvents(svr);
			result = rdr.convertDbDataArrayToGridJson(arrExpiredVaccEvents, Tc.VACCINATION_EVENT);
		} catch (Exception e) {
			if (e instanceof SvException) {
				log4j.error("Error in getExpiredVaccinationEvents: " + ((SvException) e).getFormattedMessage(), e);
			} else
				log4j.error("General error in getExpiredVaccinationEvents", e);
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
		return Response.status(200).entity(result).build();
	}

	@Path("/deleteAnimalObject/{sessionId}/{animalId}/{animalClass}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response deleteAnimalObject(@PathParam("sessionId") String sessionId, @PathParam("animalId") String animalId,
			@PathParam("animalClass") String animalClass) {
		String result = "naits.error.deleteAnimalObject";
		SvReader svr = null;
		SvWriter svw = null;
		Writer wr = null;
		try {
			svr = new SvReader(sessionId);
			svw = new SvWriter(svr);
			svw.dbSetAutoCommit(false);
			wr = new Writer();
			result = wr.deleteAnimalObject(animalId, animalClass, svw, svr);
		} catch (Exception e) {
			if (e instanceof SvException) {
				log4j.error("Error in deleteAnimalObject: " + ((SvException) e).getFormattedMessage(), e);
			} else
				log4j.error("General error in deleteAnimalObject", e);
		} finally {
			if (svr != null) {
				svr.release();
			}
			if (svw != null) {
				svw.release();
			}
		}
		return Response.status(200).entity(result).build();
	}

	@Path("/countUnreadMessages/{sessionId}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response countUnreadMessages(@PathParam("sessionId") String sessionId) {
		String result = "0";
		SvReader svr = null;
		Reader rdr = null;
		try {
			svr = new SvReader(sessionId);
			rdr = new Reader();
			result = rdr.countUnreadMessagesPerUser(svr).toString();
		} catch (Exception e) {
			if (e instanceof SvException) {
				log4j.error("Error in countUnreadMessages: " + ((SvException) e).getFormattedMessage(), e);
			} else
				log4j.error("General error in countUnreadMessages", e);
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
		return Response.status(200).entity(result).build();
	}

	@Path("/getMovementDocumentByAnimalOrFlockId/{sessionId}/{animalOrFlockId}/{movementType}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response getMovementDocumentByAnimalOrFlockId(@PathParam("sessionId") String sessionId,
			@PathParam("animalOrFlockId") String animalOrFlockId, @PathParam("movementType") String movementType) {
		String result = Tc.EMPTY_ARRAY_STRING;
		SvReader svr = null;
		DbDataArray arrMovementDocuments = null;
		Reader rdr = null;
		try {
			svr = new SvReader(sessionId);
			rdr = new Reader();
			arrMovementDocuments = rdr.getMovementDocumentByAnimalOrFlockId(animalOrFlockId, movementType, svr);
			result = rdr.convertDbDataArrayToGridJson(arrMovementDocuments, Tc.MOVEMENT_DOC);
		} catch (Exception e) {
			if (e instanceof SvException) {
				log4j.error("Error in getMovementDocumentByAnimalOrFlockId: " + ((SvException) e).getFormattedMessage(),
						e);
			} else
				log4j.error("General error in getMovementDocumentByAnimalOrFlockId", e);
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
		return Response.status(200).entity(result).build();
	}

	@Path("/getInventoryItemByRange/{sessionId}/{parentId}/{rangeFrom}/{rangeTo}/{tagType}/{order}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response getInventoryItemByRange(@PathParam("sessionId") String sessionId,
			@PathParam("parentId") Long parentId, @PathParam("rangeFrom") Long rangeFrom,
			@PathParam("rangeTo") Long rangeTo, @PathParam("tagType") String tagType,
			@PathParam("order") String order) {
		String result = Tc.EMPTY_ARRAY_STRING;
		SvReader svr = null;
		Reader rdr = null;
		try {
			svr = new SvReader(sessionId);
			rdr = new Reader();
			DbDataArray arrInventoryItems = rdr.getInventoryItemsByRange(parentId, rangeFrom, rangeTo, tagType, order,
					svr);
			result = rdr.convertDbDataArrayToGridJson(arrInventoryItems, Tc.INVENTORY_ITEM);
		} catch (Exception e) {
			if (e instanceof SvException) {
				log4j.error("Error in getInventoryItemByRange: " + ((SvException) e).getFormattedMessage(), e);
			} else
				log4j.error("General error in getInventoryItemByRange", e);
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
		return Response.status(200).entity(result).build();
	}

	@Path("/moveGroupOfIndividualInventoryItems/{sessionId}/{tableName}/{orgUnitObjectId}")
	@POST
	@Produces("text/html;charset=utf-8")
	public Response moveGroupOfIndividualInventoryItems(@PathParam("sessionId") String sessionId,
			@PathParam("tableName") String tableName, @PathParam("orgUnitObjectId") Long orgUnitObjectId,
			MultivaluedMap<String, String> formVals) {
		String resultMsgLbl = "naits.error.moveInventoryItemToOrgUnit";
		JsonObject jsonData = null;
		Gson gson = null;
		MassActions massAction = null;
		try {
			gson = new Gson();
			massAction = new MassActions();
			for (Entry<String, List<String>> entry : formVals.entrySet()) {
				if (entry.getKey() != null && !entry.getKey().isEmpty()) {
					String key = entry.getKey();
					jsonData = gson.fromJson(key, JsonObject.class);
				}
			}
			if (jsonData != null && jsonData.has(Tc.OBJ_ARRAY)) {
				resultMsgLbl = massAction.inventoryIndividualDirectTransfer(orgUnitObjectId,
						jsonData.get(Tc.OBJ_ARRAY).getAsJsonArray(), sessionId);
			} else {
				resultMsgLbl = Tc.error_admConsoleBadJson;
			}
		} catch (Exception e) {
			resultMsgLbl = e.getMessage();
			log4j.error("General error in processing mass action: ", e);
		}
		return Response.status(200).entity(resultMsgLbl).build();
	}

	@Path("/checkRangeValidity/{sessionId}/{orgUnitId}/{startTagId}/{endTagId}/{tagType}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response checkRangeValidity(@PathParam("sessionId") String sessionId, @PathParam("orgUnitId") Long orgUnitId,
			@PathParam("startTagId") Long startTagId, @PathParam("endTagId") Long endTagId,
			@PathParam("tagType") String tagType) {
		String result = "naits.success.checkRangeValidity";
		SvReader svr = null;
		SvWriter svw = null;
		ValidationChecks vc = null;
		try {
			svr = new SvReader(sessionId);
			svw = new SvWriter(svr);
			vc = new ValidationChecks();

			ArrayList<String> missingInvItemsInRange = vc.checkIfAllItemsInTransferRangeBelongToOrgUnit(orgUnitId,
					startTagId, endTagId, tagType, svr);
			if (!missingInvItemsInRange.isEmpty()) {
				result = "naits.error.senderDoesNotHaveInventoryItemsDefinedInTheRange";
				result += missingInvItemsInRange.toString();
			}

		} catch (Exception e) {
			if (e instanceof SvException) {
				result = ((SvException) e).getLabelCode();
			}
			log4j.error("Error occurred while creating transfer in moveInventoryItemByRange...", e);
		} finally {
			if (svr != null) {
				svr.release();
			}
			if (svw != null) {
				svw.release();
			}
		}
		return Response.status(200).entity(result).build();
	}

	@Path("/preCheckMoveInventoryItemByRange/{sessionId}")
	@POST
	@Produces("text/html;charset=utf-8")
	public Response preCheckMoveInventoryItemByRange(@PathParam("sessionId") String sessionId,
			MultivaluedMap<String, String> formVals) {
		String result = "naits.success.preCheckMoveInventoryItemByRange";
		SvReader svr = null;
		SvWriter svw = null;
		ValidationChecks vc = null;
		JsonObject jObj = null;
		JsonArray jsonParams = null;
		Gson gson = null;
		try {
			svr = new SvReader(sessionId);
			svw = new SvWriter(svr);
			gson = new Gson();
			vc = new ValidationChecks();
			for (Entry<String, List<String>> entry : formVals.entrySet()) {
				if (entry.getKey() != null && !entry.getKey().isEmpty()) {
					String key = entry.getKey();
					jObj = gson.fromJson(key, JsonObject.class);
				}
			}
			jsonParams = jObj.get(Tc.OBJ_PARAMS).getAsJsonArray();

			String tagType = Tc.EMPTY_STRING;
			Long startTagId = 0L;
			Long endTagId = 0L;
			Long senderOrgUnitId = 0L;

			for (int i = 0; i < jsonParams.size(); i++) {
				JsonObject obj = jsonParams.get(i).getAsJsonObject();
				if (obj.has(Tc.MASS_PARAM_TAG_TYPE)) {
					tagType = obj.get(Tc.MASS_PARAM_TAG_TYPE).getAsString();
				}
				if (obj.has(Tc.MASS_PARAM_RANGE_FROM)) {
					startTagId = obj.get(Tc.MASS_PARAM_RANGE_FROM).getAsLong();
				}
				if (obj.has(Tc.MASS_PARAM_RANGE_TO)) {
					endTagId = obj.get(Tc.MASS_PARAM_RANGE_TO).getAsLong();
				}
				if (obj.has(Tc.MASS_PARAM_SENDER_OBJ_ID)) {
					senderOrgUnitId = obj.get(Tc.MASS_PARAM_SENDER_OBJ_ID).getAsLong();
				}
			}

			if (endTagId < startTagId) {
				result = "naits.error.startTagIdCannotBeLargerThanEndTagId";
			}

			ArrayList<String> missingInvItemsInRange = vc.checkIfAllItemsInTransferRangeBelongToOrgUnit(senderOrgUnitId,
					startTagId, endTagId, tagType, svr);
			if (!missingInvItemsInRange.isEmpty()) {
				result = "naits.error.senderDoesNotHaveInventoryItemsDefinedInTheRange";
				result += missingInvItemsInRange.toString();
			}

		} catch (Exception e) {
			if (e instanceof SvException) {
				result = ((SvException) e).getLabelCode();
			}
			log4j.error("Error occurred while creating transfer in moveInventoryItemByRange...", e);
		} finally {
			if (svr != null) {
				svr.release();
			}
			if (svw != null) {
				svw.release();
			}
		}
		return Response.status(200).entity(result).build();
	}

	@Path("/moveInventoryItemByRange/{sessionId}/{preCheckDone}")
	@POST
	@Produces("text/html;charset=utf-8")
	public Response moveInventoryItemByRange(@PathParam("sessionId") String sessionId,
			@PathParam("preCheckDone") Boolean preCheckDone, MultivaluedMap<String, String> formVals) {
		String result = "naits.error.moveInventoryItemByRange";
		SvReader svr = null;
		SvWriter svw = null;
		Writer wr = null;
		ValidationChecks vc = null;
		JsonObject jObj = null;
		JsonArray jsonParams = null;
		Gson gson = null;
		try {
			svr = new SvReader(sessionId);
			svw = new SvWriter(svr);
			wr = new Writer();
			gson = new Gson();
			vc = new ValidationChecks();
			for (Entry<String, List<String>> entry : formVals.entrySet()) {
				if (entry.getKey() != null && !entry.getKey().isEmpty()) {
					String key = entry.getKey();
					jObj = gson.fromJson(key, JsonObject.class);
				}
			}
			jsonParams = jObj.get(Tc.OBJ_PARAMS).getAsJsonArray();

			String tagType = Tc.EMPTY_STRING;
			Long startTagId = 0L;
			Long endTagId = 0L;
			Long paramDestinationObjId = 0L;
			Long senderOrgUnitId = 0L;
			Long quantity = null;
			String reason = null;

			for (int i = 0; i < jsonParams.size(); i++) {
				JsonObject obj = jsonParams.get(i).getAsJsonObject();
				if (obj.has(Tc.MASS_PARAM_TAG_TYPE)) {
					tagType = obj.get(Tc.MASS_PARAM_TAG_TYPE).getAsString();
				}
				if (obj.has(Tc.MASS_PARAM_RANGE_FROM)) {
					startTagId = obj.get(Tc.MASS_PARAM_RANGE_FROM).getAsLong();
				}
				if (obj.has(Tc.MASS_PARAM_RANGE_TO)) {
					endTagId = obj.get(Tc.MASS_PARAM_RANGE_TO).getAsLong();
				}
				if (obj.has(Tc.MASS_PARAM_DESTINATION_OBJ_ID)) {
					paramDestinationObjId = obj.get(Tc.MASS_PARAM_DESTINATION_OBJ_ID).getAsLong();
				}
				if (obj.has(Tc.MASS_PARAM_QUANTITY)) {
					quantity = obj.get(Tc.MASS_PARAM_QUANTITY).getAsLong();
				}
				if (obj.has(Tc.MASS_PARAM_REASON)) {
					reason = obj.get(Tc.MASS_PARAM_REASON).getAsString();
				}
				if (obj.has(Tc.MASS_PARAM_SENDER_OBJ_ID)) {
					senderOrgUnitId = obj.get(Tc.MASS_PARAM_SENDER_OBJ_ID).getAsLong();
				}
			}

			if (!preCheckDone) {
				if (endTagId < startTagId) {
					throw new SvException("naits.error.startTagIdCannotBeLargerThanEndTagId", svr.getInstanceUser());
				}

				ArrayList<String> missingInvItemsInRange = vc.checkIfAllItemsInTransferRangeBelongToOrgUnit(
						senderOrgUnitId, startTagId, endTagId, tagType, svr);
				if (!missingInvItemsInRange.isEmpty()) {
					throw new SvException("naits.error.senderDoesNotHaveInventoryItemsDefinedInTheRange",
							svr.getInstanceUser());
				}
			}
			DbDataObject dboOrgUnitSender = svr.getObjectById(senderOrgUnitId, svCONST.OBJECT_TYPE_ORG_UNITS, null);
			if (dboOrgUnitSender == null) {
				dboOrgUnitSender = svr.getObjectById(senderOrgUnitId, SvReader.getTypeIdByName(Tc.HOLDING), null);
			}

			DbDataObject dboDestinationOrganisationalUnitOrHolding = svr.getObjectById(paramDestinationObjId,
					svCONST.OBJECT_TYPE_ORG_UNITS, null);
			if (dboDestinationOrganisationalUnitOrHolding == null) {
				dboDestinationOrganisationalUnitOrHolding = svr.getObjectById(paramDestinationObjId,
						SvReader.getTypeIdByName(Tc.HOLDING), null);
			}
			if (dboDestinationOrganisationalUnitOrHolding == null) {
				throw new SvException("naits.error.destinationLocationNotFound", svr.getInstanceUser());
			}
			DbDataObject dboTransfer = wr.createTransferObject(tagType, startTagId, endTagId, quantity, null,
					dboOrgUnitSender.getVal(Tc.NAME).toString(),
					dboDestinationOrganisationalUnitOrHolding.getVal(Tc.NAME).toString(), null, null, reason,
					String.valueOf(dboOrgUnitSender.getObject_id()),
					String.valueOf(dboDestinationOrganisationalUnitOrHolding.getObject_id()));
			dboTransfer.setParent_id(dboOrgUnitSender.getObject_id());
			dboTransfer.setVal(Tc.CACHE_PARENT_ID, dboOrgUnitSender.getObject_id());
			dboTransfer.setVal(Tc.TRANSFER_TYPE, Tc.DIRECT_TRANSFER);
			svw.saveObject(dboTransfer, true);
			wr.directInventoryTransfer(dboTransfer, false, svr, svw);
			result = "naits.success.moveInventoryItemByRange";

		} catch (Exception e) {
			if (e instanceof SvException) {
				result = ((SvException) e).getLabelCode();
			}
			log4j.error("Error occurred while creating transfer in moveInventoryItemByRange...", e);
		} finally {
			if (svr != null) {
				svr.release();
			}
			if (svw != null) {
				svw.release();
			}
		}
		return Response.status(200).entity(result).build();
	}

	@Path("/createRevereseTransfer/{sessionId}/{dboTransferObjId}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response createRevereseTransfer(@PathParam("sessionId") String sessionId,
			@PathParam("dboTransferObjId") Long dboTransferObjId) {
		String resultMsgLbl = "naits.error.createRevereseTransfer";
		SvReader svr = null;
		SvWriter svw = null;
		Writer wr = null;
		ValidationChecks vc = null;
		try {
			svr = new SvReader(sessionId);
			svw = new SvWriter(svr);
			wr = new Writer();
			vc = new ValidationChecks();
			DbDataObject dboInitTransfer = svr.getObjectById(dboTransferObjId, SvReader.getTypeIdByName(Tc.TRANSFER),
					null);

			if (!dboInitTransfer.getStatus().equals(Tc.RELEASED)) {
				throw new SvException("naits.error.onlyTransferWithReleasedStatusCanBeReversed", svr.getInstanceUser());
			}

			ArrayList<String> missingInvItemsInRange = vc.checkIfAllItemsInTransferRangeBelongToOrgUnit(
					Long.valueOf(dboInitTransfer.getVal(Tc.DESTINATION_OBJ_ID).toString()),
					Long.valueOf(dboInitTransfer.getVal(Tc.START_TAG_ID).toString()),
					Long.valueOf(dboInitTransfer.getVal(Tc.END_TAG_ID).toString()),
					dboInitTransfer.getVal(Tc.TAG_TYPE).toString(), svr);
			if (!missingInvItemsInRange.isEmpty()) {
				throw new SvException("naits.error.senderDoesNotHaveInventoryItemsDefinedInTheRange",
						svr.getInstanceUser());
			}

			if (dboInitTransfer != null) {
				DbDataObject dboReverseTransfer = wr.createReverseTransferObject(dboInitTransfer, wr, svr);
				svw.saveObject(dboReverseTransfer, false);
				wr.directInventoryTransfer(dboReverseTransfer, false, svr, svw);
				resultMsgLbl = "naits.success.createRevereseTransfer";
			}

		} catch (Exception e) {
			resultMsgLbl = e.getMessage();
			log4j.error("General error in processing mass action", e);
		} finally {
			if (svr != null) {
				svr.release();
			}
			if (svw != null) {
				svw.release();
			}
		}
		return Response.status(200).entity(resultMsgLbl).build();
	}

	@Path("/getMovementDocumentByTransporterLicense/{sessionId}/{transporterLicence}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response getMovementDocumentByTransporterLincence(@PathParam("sessionId") String sessionId,
			@PathParam("transporterLicence") String transporterLicence) {
		String result = Tc.EMPTY_ARRAY_STRING;
		SvReader svr = null;
		Reader rdr = null;
		try {
			svr = new SvReader(sessionId);
			rdr = new Reader();
			DbDataArray arrMovementDocuments = rdr.getMovementDocumentByTransporterLicense(transporterLicence, svr);
			result = rdr.convertDbDataArrayToGridJson(arrMovementDocuments, Tc.MOVEMENT_DOC);
		} catch (Exception e) {
			if (e instanceof SvException) {
				log4j.error(
						"Error in getMovementDocumentByTransporterLincence: " + ((SvException) e).getFormattedMessage(),
						e);
			} else
				log4j.error("General error in getMovementDocumentByTransporterLincence", e);
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
		return Response.status(200).entity(result).build();
	}

	@Path("/getAllLinkedUserGroupsPerUser/{sessionId}/{userObjectId}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response getAllLinkedUserGroupsPerUser(@PathParam("sessionId") String sessionId,
			@PathParam("userObjectId") Long userObjectId) {
		String result = Tc.EMPTY_ARRAY_STRING;
		SvReader svr = null;
		Reader rdr = null;
		try {
			svr = new SvReader(sessionId);
			rdr = new Reader();
			DbDataObject dboUser = svr.getObjectById(userObjectId, svCONST.OBJECT_TYPE_USER, null);
			DbDataArray arrUserGroups = svr.getAllUserGroups(dboUser, false);
			result = rdr.convertDbDataArrayToGridJson(arrUserGroups, Tc.SVAROG_USER_GROUPS);
		} catch (Exception e) {
			if (e instanceof SvException) {
				log4j.error(
						"Error in getMovementDocumentByTransporterLincence: " + ((SvException) e).getFormattedMessage(),
						e);
			} else
				log4j.error("General error in getMovementDocumentByTransporterLincence", e);
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
		return Response.status(200).entity(result).build();
	}

	@Path("/getApplicableLaboratoryTests/{sessionId}/{activityType}/{activitySubType}/{disease}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response getApplicableLaboratoryTests(@PathParam("sessionId") String sessionId,
			@PathParam("activityType") String activityType, @PathParam("activitySubType") String activitySubType,
			@PathParam("disease") String disease) {
		String result = Tc.EMPTY_ARRAY_STRING;
		SvReader svr = null;
		Reader rdr = null;
		LinkedHashMap<String, String> criteriasHashMap = null;
		DbDataArray arrAvailableTests = null;
		try {
			svr = new SvReader(sessionId);
			rdr = new Reader();
			if (activityType.equals("2")) {
				criteriasHashMap = new LinkedHashMap<>();
				criteriasHashMap.put(Tc.DISEASE, disease);
				criteriasHashMap.put(Tc.SAMPLE_TYPE, activitySubType);
				arrAvailableTests = rdr.getDbDataWithCriteria(criteriasHashMap,
						SvReader.getTypeIdByName(Tc.LAB_TEST_TYPE), svr);
				result = rdr.convertDbDataArrayToGridJson(arrAvailableTests, Tc.LAB_TEST_TYPE);
			}
		} catch (Exception e) {
			log4j.error("General error in getApplicableLaboratoryTestsAccording", e);
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
		return Response.status(200).entity(result).build();
	}

	@Path("/deleteObject/{sessionId}/{objectId}/{objectType}/{useCache}/{autoCommit}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response deleteObject(@PathParam("sessionId") String sessionId, @PathParam("objectId") Long objectId,
			@PathParam("objectType") Long objectType, @PathParam("useCache") boolean useCache,
			@PathParam("autoCommit") boolean autoCommit) {
		String result = "naits.error.deleteRecord";
		SvReader svr = null;
		SvWriter svw = null;
		Writer wr = null;
		try {
			svr = new SvReader(sessionId);
			svw = new SvWriter(svr);
			svw.setAutoCommit(false);

			wr = new Writer();
			if (wr.deleteObject(objectId, objectType, useCache, autoCommit, svw, svr)) {
				result = "naits.success.successfullyDeletedRecord";
			}
		} catch (Exception e) {
			log4j.error("General error in deleteObject", e);
		} finally {
			if (svr != null) {
				svr.release();
			}
			if (svw != null) {
				svw.release();
			}
		}
		return Response.status(200).entity(result).build();
	}

	@Path("/generateRFIDResultObjects/{sessionId}")
	@POST
	@Produces("text/html;charset=utf-8")
	public Response generateRFIDResultObjects(@PathParam("sessionId") String sessionId,
			MultivaluedMap<String, String> formVals) throws InterruptedException {
		String resultMessage = "naits.error.generateRFIDResultObjects";
		JsonObject responseJson = null;
		JsonObject postDataJson = null;
		SvReader svr = null;
		SvWriter svw = null;
		Writer wr = null;
		Gson gson = null;
		try {
			svr = new SvReader(sessionId);
			svw = new SvWriter(svr);
			svw.setAutoCommit(false);

			gson = new Gson();
			wr = new Writer();

			for (Entry<String, List<String>> entry : formVals.entrySet()) {
				if (entry.getKey() != null && !entry.getKey().isEmpty()) {
					String key = entry.getKey();
					postDataJson = gson.fromJson(key, JsonObject.class);
				}
			}
			if (postDataJson != null && postDataJson.has(Tc.OBJ_PARAMS) && postDataJson.has(Tc.OBJ_ARRAY)) {
				responseJson = wr.getGeneratedRfidResults(postDataJson, svr, svw);
				if (responseJson != null && responseJson.has("activityResponses")) {
					resultMessage = "naits.success.generateRFIDResultObjects";
				}
			}
		} catch (SvException e) {
			log4j.error("General error in generateRFIDResultObjects", e);
			resultMessage = e.getLabelCode();
		} finally {
			if (svr != null) {
				svr.release();
			}
			if (svw != null) {
				svw.release();
			}
		}
		return Response.status(200).entity(resultMessage).build();
	}

	@Path("/displayExportCertificateReportButton/{sessionId}/{objectId}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response displayExportCertificateReportButton(@PathParam("sessionId") String sessionId,
			@PathParam("objectId") Long objectId) {
		String resultMessage = Boolean.FALSE.toString();
		SvReader svr = null;
		try {
			svr = new SvReader(sessionId);
			DbDataObject dboExportCertificate = svr.getObjectById(objectId, SvReader.getTypeIdByName(Tc.EXPORT_CERT),
					null);
			if (dboExportCertificate != null) {
				DbDataObject dboQuarantine = svr.getObjectById(dboExportCertificate.getParent_id(),
						SvReader.getTypeIdByName(Tc.QUARANTINE), null);
				if (dboQuarantine.getVal(Tc.DATE_TO) != null) {
					DateTime dtDateTo = new DateTime(dboQuarantine.getVal(Tc.DATE_TO).toString());
					DateTime dtNow = new DateTime();
					Long numDaysAfterQuarantineExpired = ValidationChecks.getDateDiff(dtNow, dtDateTo, TimeUnit.HOURS);
					if (numDaysAfterQuarantineExpired >= 0) {
						resultMessage = Boolean.TRUE.toString();
					}
				}
			}
		} catch (Exception e) {
			log4j.error("General error in displayExportCertificateReportButton", e);
			resultMessage = e.getMessage();
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
		return Response.status(200).entity(resultMessage).build();
	}

	/**
	 * Web service for uploading file that contains RFID tags in comma separated
	 * file
	 * 
	 * @param sessionId
	 * @param objectId
	 * @param fileInput
	 * @param fileDetail
	 * @return
	 */
	@Path("/uploadRFIDTags/{sessionId}/{objectId}")
	@POST
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces("text/html;charset=utf-8")
	public Response uploadFile(@PathParam("sessionId") String sessionId, @PathParam("objectId") Long objectId,
			@FormDataParam("file") InputStream fileInput,
			@FormDataParam("file") FormDataContentDisposition fileDetail) {
		String resultMessage = "naits.success.uploadRFIDTags";
		SvReader svr = null;
		SvWriter svw = null;
		Writer wr = null;
		try {
			svr = new SvReader(sessionId);
			svw = new SvWriter(svr);
			wr = new Writer();
			DbDataObject dboRfid = svr.getObjectById(objectId, SvReader.getTypeIdByName(Tc.RFID_INPUT), null);
			if (dboRfid != null && dboRfid.getStatus().equals(Tc.PROCESSED)) {
				throw new SvException("naits.error.cannotUploadFileWhenStatusIsProcessed", svr.getInstanceUser());
			}
			byte[] data = IOUtils.toByteArray(fileInput);
			if (data.length < 1) {
				throw new SvException("naits.error.cannotUploadEmptyFile", svr.getInstanceUser());
			}
			if (!wr.uploadSvFile(fileDetail.getFileName(), dboRfid, "naits.actions.rfid_attachment", data, false, svw,
					svr)) {
				resultMessage = "naits.error.uploadRFIDTags";
			}
		} catch (IOException | SvException e) {
			if (e instanceof SvException)
				resultMessage = ((SvException) e).getLabelCode();
			log4j.error(e.getLocalizedMessage());
		} finally {
			if (svr != null) {
				svr.release();
			}
			if (svw != null) {
				svw.release();
			}
		}
		return Response.status(200).entity(resultMessage).build();
	}

	@Path("/getSvFilePerDbo/{sessionId}/{objectId}/{objectTypeId}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response getSvFilePerDbo(@PathParam("sessionId") String sessionId, @PathParam("objectId") Long objectId,
			@PathParam("objectTypeId") Long objectTypeId) {
		String resultObject = Tc.EMPTY_STRING;
		SvReader svr = null;
		Reader rdr = null;
		DbDataObject dboRfid = null;
		DbDataObject dboFile = null;
		try {
			svr = new SvReader(sessionId);
			rdr = new Reader();
			dboRfid = svr.getObjectById(objectId, objectTypeId, null);
			DbDataArray arrLinkedSvFiles = rdr.getSvFilesPerDbo(dboRfid, svr);
			if (arrLinkedSvFiles != null && !arrLinkedSvFiles.getItems().isEmpty()) {
				dboFile = arrLinkedSvFiles.get(0);
				resultObject = dboFile.toSimpleJson().toString();
			}
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
		return Response.status(200).entity(resultObject).build();
	}

	@GET
	@Path("/downloadSvFile/{sessionId}/{objectId}/{objectType}/{fileName}/{fileLabelCode}")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response downloadSvFile(@PathParam("sessionId") final String sessionId,
			@PathParam("objectId") final Long objectId, @PathParam("objectType") final Long objectType,
			@PathParam("fileName") final String fileName, @PathParam("fileLabelCode") final String fileLabelCode,
			@Context HttpServletRequest httpRequest) {
		StreamingOutput fileStream = new StreamingOutput() {
			@Override
			public void write(java.io.OutputStream output) throws IOException {
				SvReader svr = null;
				SvWriter svw = null;
				Reader rdr = null;
				try {
					svr = new SvReader(sessionId);
					svw = new SvWriter(svr);
					svw.setAutoCommit(false);

					rdr = new Reader();
					DbDataObject dbo = svr.getObjectById(objectId, objectType, null);
					if (dbo != null) {
						ByteArrayOutputStream bstr = new ByteArrayOutputStream();
						byte[] data = bstr.toByteArray();
						output.write(data);
						rdr.downloadSvFile(dbo, fileName, fileLabelCode, output, svr);
					}
				} catch (Exception e) {
					log4j.error("Error printing PDF/Excel! method: generatePopulationSample", e);
				} finally {
					if (svr != null)
						svr.release();
					if (svw != null)
						svw.release();
					output.close();
				}
			}
		};
		return Response.ok(fileStream, MediaType.APPLICATION_OCTET_STREAM)
				.header("content-disposition", "attachment; filename = " + fileName).build();
	}

	@GET
	@Path("/downloadAttachedFilePerDbObject/{sessionId}/{objectId}/{objectType}/{fieldName}/{fileName}")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response downloadAttachedFilePerDbObject(@PathParam("sessionId") final String sessionId,
			@PathParam("objectId") final Long objectId, @PathParam("objectType") final Long objectType,
			@PathParam("fileName") final String fileName, @PathParam("fieldName") final String fieldName,
			@Context HttpServletRequest httpRequest) {
		StreamingOutput fileStream = new StreamingOutput() {
			@Override
			public void write(java.io.OutputStream output) throws IOException {
				SvReader svr = null;
				try {
					svr = new SvReader(sessionId);
					DbDataObject dbo = svr.getObjectById(objectId, objectType, null);
					if (dbo != null) {
						if (dbo.getVal(fieldName) != null && dbo.getVal(fieldName).toString().length() > 1) {
							byte[] data = Base64.decodeBase64(dbo.getVal(fieldName).toString());
							output.write(data);
						}
					}
				} catch (Exception e) {
					log4j.error("Error printing PDF/Excel! method: generatePopulationSample", e);
				} finally {
					if (svr != null)
						svr.release();
					output.close();
				}
			}
		};
		return Response.ok(fileStream, MediaType.APPLICATION_OCTET_STREAM)
				.header("content-disposition", "attachment; filename = " + fileName).build();
	}

	@Path("/createCustomRfidInput/{sessionId}/{objectId}")
	@POST
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces("application/json")
	public Response createCustomRfidInput(@PathParam("sessionId") String sessionId,
			@PathParam("objectId") Long objectId, FormDataMultiPart multipart, @Context HttpServletRequest httpRequest)
			throws IOException {
		String resultMessage = Tc.EMPTY_ARRAY_STRING;
		SvReader svr = null;
		SvWriter svw = null;
		Reader rdr = null;
		JsonArray jsonData = null;
		DbDataArray resultArr = null;
		DbDataObject dboToSave = null;
		String jObjString = Tc.EMPTY_STRING;
		byte[] data = null;
		String fileName = null;
		try {
			svr = new SvReader(sessionId);
			svw = new SvWriter(svr);
			rdr = new Reader();

			dboToSave = svr.getObjectById(objectId, SvReader.getTypeIdByName(Tc.RFID_INPUT), new DateTime());
			if (dboToSave == null) {
				dboToSave = new DbDataObject();
				dboToSave.setObject_type(SvReader.getTypeIdByName(Tc.RFID_INPUT));
			}
			Map<String, List<FormDataBodyPart>> map = multipart.getFields();
			for (Map.Entry<String, List<FormDataBodyPart>> entry : map.entrySet()) {
				for (FormDataBodyPart part : entry.getValue()) {
					if (!part.getName().equals("form-data")) {
						try (InputStream in = part.getValueAs(InputStream.class);) {
							data = IOUtils.toByteArray(in);
							fileName = part.getName();
							dboToSave.setVal(Tc.FILE_EAR_TAGS, Base64.encodeBase64String(data));
							dboToSave.setVal(Tc.FILE_NAME, fileName);
						}
					} else {
						jObjString = part.getValue();
					}
				}
			}
			if (!jObjString.equals(Tc.EMPTY_STRING)) {
				Gson gson = new Gson();
				jsonData = gson.fromJson(jObjString, JsonArray.class);
				for (int i = 0; i < jsonData.size(); i++) {
					JsonObject jo = jsonData.get(i).getAsJsonObject();
					if (jo.has(Tc.IMPORT_TYPE)) {
						dboToSave.setVal(Tc.IMPORT_TYPE, jo.get(Tc.IMPORT_TYPE).getAsString());
					}
					if (jo.has(Tc.ANIMAL_TYPE)) {
						dboToSave.setVal(Tc.ANIMAL_TYPE, jo.get(Tc.ANIMAL_TYPE).getAsString());
					}
					if (jo.has(Tc.TEXT_EAR_TAGS)) {
						dboToSave.setVal(Tc.TEXT_EAR_TAGS, jo.get(Tc.TEXT_EAR_TAGS).getAsString());
					}
				}
			}
			svw.saveObject(dboToSave, true);
			resultArr = new DbDataArray();
			resultArr.addDataItem(dboToSave);
			resultMessage = rdr.convertDbDataArrayToGridJson(resultArr, Tc.RFID_INPUT);
		} catch (SvException e) {
			resultMessage = e.getLabelCode();
			log4j.error(e);
		} finally {
			if (svr != null) {
				svr.release();
			}
			if (svw != null) {
				svw.release();
			}
		}
		return Response.status(200).entity(resultMessage).build();
	}

	/**
	 * Web service that fetch animals depend on status.
	 * 
	 * @param sessionId
	 * @param animalStatus
	 * @return
	 */
	@Path("getAnimalsByStatus/{sessionId}/{animalStatus}")
	@GET
	@Produces("application/json")
	public Response getAnimalsByStatus(@PathParam("sessionId") String sessionId,
			@PathParam("animalStatus") String animalStatus) {
		String result = Tc.EMPTY_ARRAY_STRING;
		SvReader svr = null;
		Reader rdr = null;
		try {
			svr = new SvReader(sessionId);
			rdr = new Reader();
			if (animalStatus.equals(Tc.LOST) || animalStatus.equals(Tc.TRANSITION)) {
				DbDataArray dbArray = rdr.findDataPerSingleFilter(Tc.STATUS, animalStatus, DbCompareOperand.EQUAL,
						SvReader.getTypeIdByName(Tc.ANIMAL), 10000, null, svr);
				result = rdr.convertDbDataArrayToGridJson(dbArray, Tc.ANIMAL);
			}
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage(), e);
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
		return Response.status(200).entity(result).build();
	}

	@Path("getObjectsByParentId/{sessionId}/{parentId}/{tableName}/{orderByField}/{orderAscDesc}")
	@GET
	@Produces("application/json")
	public Response getObjectsByParentId(@PathParam("sessionId") String sessionId, @PathParam("parentId") Long parentId,
			@PathParam("tableName") String tableName, @PathParam("orderByField") String orderByField,
			@PathParam("orderAscDesc") String orderAscDesc) {
		String result = Tc.EMPTY_ARRAY_STRING;
		SvReader svr = null;
		Reader rdr = null;
		try {
			svr = new SvReader(sessionId);
			rdr = new Reader();
			DbDataObject dboTypeDesc = SvReader.getDbtByName(tableName.toUpperCase());
			DbDataArray arrResult = svr.getObjectsByParentId(parentId, dboTypeDesc.getObject_id(), null, 0, 0);
			result = rdr.convertDbDataArrayToGridJson(arrResult, dboTypeDesc.getVal(Tc.TABLE_NAME).toString(), false,
					orderByField, orderAscDesc, null);
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage(), e);
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
		return Response.status(200).entity(result).build();
	}

	@Path("setStatusOfConversation/{sessionId}/{objectId}/{additionalStatus}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response setStatusOfConversation(@PathParam("sessionId") String sessionId,
			@PathParam("objectId") Long objectId, @PathParam("additionalStatus") Boolean additionalStatus) {
		String result = Tc.EMPTY_ARRAY_STRING;
		SvReader svr = null;
		DbDataObject dboConversation = null;
		try {
			svr = new SvReader(sessionId);
			dboConversation = new DbDataObject();
			dboConversation = svr.getObjectById(objectId, svCONST.OBJECT_TYPE_CONVERSATION, null);
			if (dboConversation != null) {
				dboConversation.setVal(Tc.CONVERSATION_STATUS, Tc.SENT);
				result = "naits.success.conversationStatusUpdatedToSent";
				if (dboConversation.getVal(Tc.CATEGORY) != null
						&& dboConversation.getVal(Tc.CATEGORY).equals(Tc.INFO)) {
					if (dboConversation.getVal(Tc.IS_READ) != null && dboConversation.getVal(Tc.IS_READ).equals(true)) {
						dboConversation.setVal(Tc.CONVERSATION_STATUS, Tc.READ);
						result = "naits.success.conversationStatusUpdatedToRead";
					}
				}
				if (dboConversation.getVal(Tc.CATEGORY) != null
						&& dboConversation.getVal(Tc.CATEGORY).equals(Tc.TASK)) {
					if (dboConversation.getVal(Tc.IS_READ) != null && dboConversation.getVal(Tc.IS_READ).equals(true)) {
						dboConversation.setVal(Tc.CONVERSATION_STATUS, Tc.READ);
						result = "naits.success.conversationStatusUpdatedToRead";
						if (additionalStatus) {
							dboConversation.setVal(Tc.CONVERSATION_STATUS, Tc.CLOSED);
							result = "naits.success.conversationStatusUpdatedToClosed";
						}
					}
				}
			}
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage(), e);
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
		return Response.status(200).entity(result).build();
	}

	/**
	 * Web service that return all responsible users per region
	 * 
	 * @param sessionId
	 * @param objectId
	 * @return
	 */
	@Path("getResponsibleUsersPerRegion/{sessionId}/{objectId}")
	@GET
	@Produces("application/json")
	public Response getResponsibleUsersPerRegion(@PathParam("sessionId") String sessionId,
			@PathParam("objectId") Long objectId) {
		String result = Tc.EMPTY_ARRAY_STRING;
		SvReader svr = null;
		DbDataObject dboOrgUnit = null;
		DbDataArray dbArrOrgUnits = null;
		DbDataObject dboLinkType = null;
		Reader rdr = null;
		try {
			svr = new SvReader(sessionId);
			dboOrgUnit = new DbDataObject();
			dbArrOrgUnits = new DbDataArray();
			dboLinkType = new DbDataObject();
			rdr = new Reader();
			dboOrgUnit = svr.getObjectById(objectId, svCONST.OBJECT_TYPE_ORG_UNITS, null);
			if (dboOrgUnit != null) {
				dboLinkType = SvReader.getLinkType(Tc.POA, svCONST.OBJECT_TYPE_USER, svCONST.OBJECT_TYPE_ORG_UNITS);
				if (dboLinkType != null && dboOrgUnit.getVal(Tc.ORG_UNIT_TYPE).equals(Tc.REGIONAL_OFFICE)) {
					dbArrOrgUnits = svr.getObjectsByLinkedId(dboOrgUnit.getObject_id(), dboOrgUnit.getObject_type(),
							dboLinkType, svCONST.OBJECT_TYPE_USER, true, null, 0, 0);
				}
			}
			result = rdr.convertDbDataArrayToGridJson(dbArrOrgUnits, "SVAROG_USERS", true, svr);
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage(), e);
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
		return Response.status(200).entity(result).build();
	}

	/**
	 * Web service that inactivate existing link between pet and owner
	 * 
	 * @param sessionId
	 * @param ownerObjId
	 * @param petObjId
	 * @return
	 */
	@Path("inactivateLinkBetweenPetAndOwner/{sessionId}/{ownerObjId}/{petObjId}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response inactivateLinkBetweenPetAndOwner(@PathParam("sessionId") String sessionId,
			@PathParam("ownerObjId") Long ownerObjId, @PathParam("petObjId") Long petObjId) {
		String result = "naits.error.linkFailedToInactivate";
		boolean output = false;
		SvReader svr = null;
		Writer wr = null;
		DbDataObject dboPet = null;
		DbDataObject dboOwner = null;
		try {
			svr = new SvReader(sessionId);
			wr = new Writer();
			dboOwner = svr.getObjectById(ownerObjId, SvReader.getTypeIdByName(Tc.HOLDING_RESPONSIBLE), null);
			dboPet = svr.getObjectById(petObjId, SvReader.getTypeIdByName(Tc.PET), null);
			output = wr.inactivateLinkBetweenExistingPetAndOwner(dboOwner, dboPet, svr);
			if (output) {
				result = "naits.success.linkSuccessfullyInactivated";
			}
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage(), e);
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
		return Response.status(200).entity(result).build();
	}

	@Path("checkIfPetBelongsToAnimalShelter/{sessionId}/{petId}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response checkIfPetBelongsToAnimalShelter(@PathParam("sessionId") String sessionId,
			@PathParam("petId") String petId) {
		SvReader svr = null;
		Reader rdr = null;
		DbDataObject dboPet = null;
		JsonObject jObj = new JsonObject();
		String localeId = "";
		try {
			svr = new SvReader(sessionId);
			localeId = svr.getUserLocaleId(svr.getInstanceUser());
			rdr = new Reader();
			dboPet = rdr.getPetByPetIdAndPetType(Tc.PET_ID, petId, null, true, svr);
			if (dboPet != null) {
				DbDataObject dboHolding = svr.getObjectById(dboPet.getParent_id(), SvReader.getTypeIdByName(Tc.HOLDING),
						null);
				if (dboHolding != null) {
					if (dboPet.getParent_id().equals(dboHolding.getObject_id())) {
						String labelCode = I18n.getText(localeId, "naits.info.petAlreadyExistsInHolding");
						jObj.addProperty(Tc.LABEL_CODE, labelCode);
						jObj.addProperty(Tc.PIC, dboHolding.getVal(Tc.PIC).toString());
						if (dboHolding.getVal(Tc.NAME) != null) {
							jObj.addProperty(Tc.NAME, dboHolding.getVal(Tc.NAME).toString());
						}
					}
				}
			}
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage(), e);
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
		return Response.status(200).entity(jObj.toString()).build();
	}

	@Path("getTransferByRange/{sessionId}/{parentId}/{rangeFrom}/{rangeTo}/{transferType}/{shouldSkipStatus}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response getTransferByRange(@PathParam("sessionId") String sessionId, @PathParam("parentId") Long parentId,
			@PathParam("rangeFrom") Long rangeFrom, @PathParam("rangeTo") Long rangeTo,
			@PathParam("transferType") String transferType, @PathParam("shouldSkipStatus") Boolean shouldSkipStatus) {
		SvReader svr = null;
		Reader rdr = null;
		String result = "";
		try {
			svr = new SvReader(sessionId);
			rdr = new Reader();

			DbDataArray arrDboTransfers = rdr.getDbArrayOfDboTransfersByRangeAndParentId(parentId, rangeFrom, rangeTo,
					transferType, shouldSkipStatus, svr);
			result = rdr.convertDbDataArrayToGridJson(arrDboTransfers, Tc.TRANSFER, svr);
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage(), e);
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
		return Response.status(200).entity(result).build();
	}

	@Path("getCustomTableFormDataPerStrayPetLocation/{sessionId}/{parentId}/{locationReason}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response getCustomTableFormDataPerStrayPetLocation(@PathParam("sessionId") String sessionId,
			@PathParam("parentId") Long parentId, @PathParam("locationReason") String locationReason) {
		return getCustomTableFormDataPerStrayPetLocation(sessionId, parentId, locationReason, 0L);
	}

	@Path("getLastPetMovement/{sessionId}/{objectId}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response getLastPetMovement(@PathParam("sessionId") String sessionId, @PathParam("objectId") Long objectId) {
		String result = "0";
		SvReader svr = null;
		Reader rdr = null;
		try {
			svr = new SvReader(sessionId);
			rdr = new Reader();
			ArrayList<String> inStatus = new ArrayList<>();
			inStatus.add(Tc.ADOPTED);
			inStatus.add(Tc.RELEASED);
			inStatus.add(Tc.FINISHED);
			DbDataObject dboPet = svr.getObjectById(objectId, SvReader.getTypeIdByName(Tc.PET), null);
			DbDataObject dboPetMovement = rdr.getPetMovement(dboPet, inStatus, svr);
			if (dboPetMovement != null)
				result = dboPetMovement.getObject_id().toString();
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage(), e);
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
		return Response.status(200).entity(result).build();
	}

	@Path("getCustomTableFormDataPerStrayPetLocation/{sessionId}/{parentId}/{locationReason}/{currentHoldingId}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response getCustomTableFormDataPerStrayPetLocation(@PathParam("sessionId") String sessionId,
			@PathParam("parentId") Long parentId, @PathParam("locationReason") String locationReason,
			@PathParam("currentHoldingId") Long currentHoldingId) {
		SvReader svr = null;
		Reader rdr = null;
		String result = Tc.EMPTY_JSON_STRING;
		try {
			svr = new SvReader(sessionId);
			rdr = new Reader();

			result = rdr.getCustomTableFormDataPerStrayPetLocation(parentId, locationReason, currentHoldingId, svr)
					.toString();
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage(), e);
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
		return Response.status(200).entity(result).build();
	}

	/**
	 * Web service that return translated object type
	 * 
	 * @param sessionId
	 * @param objType
	 * @return
	 */
	@Path("/getTranslatedTableObjectType/{sessionId}/{objType}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response getTranslatedTableObjectType(@PathParam("sessionId") String sessionId,
			@PathParam("objType") Long objType) {
		String translatedValue = Tc.EMPTY_STRING;
		SvReader svr = null;
		Reader rdr = null;
		try {
			svr = new SvReader(sessionId);
			rdr = new Reader();
			translatedValue = rdr.getTableNameByType(objType, svr);
		} catch (Exception e) {
			log4j.error("General error in processing getTranslatedTableObjectType:", e);
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
		return Response.status(200).entity(translatedValue).build();
	}

	/**
	 * Method that return list of keepers by holding object id
	 * 
	 * @param sessionId
	 * @param holdingObjid
	 * @return
	 */
	@Path("/getAllKeepersByHoldingByObjId/{sessionId}/{holdingObjid}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response getAllKeepersByHoldingByObjId(@PathParam("sessionId") String sessionId,
			@PathParam("holdingObjid") Long holdingObjid) {
		String result = Tc.EMPTY_ARRAY_STRING;
		SvReader svr = null;
		Reader rdr = null;
		DbDataArray res = null;
		try {
			svr = new SvReader(sessionId);
			rdr = new Reader();
			res = new DbDataArray();
			if (holdingObjid != null) {
				res = rdr.getAllHoldingKeepersByHoldingObjid(holdingObjid, svr);
				if (res != null && !res.getItems().isEmpty()) {
					result = rdr.convertDbDataArrayToGridJson(res, Tc.HOLDING_RESPONSIBLE);
				}
			}
		} catch (Exception e) {
			log4j.error("General error in processing getAllKeepersByHoldingByObjId:", e);
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
		return Response.status(200).entity(result).build();
	}

	@Path("getDatesForStatisticalReports/{sessionId}/{reportType}/{sortOrder}")
	@GET
	@Produces("application/json")
	public Response getDatesForStatisticalReports(@PathParam("sessionId") String sessionId,
			@PathParam("reportType") String reportType, @PathParam("sortOrder") String sortOrder) {
		String result = Tc.EMPTY_ARRAY_STRING;
		SvReader svr = null;
		Reader rdr = null;
		try {
			svr = new SvReader(sessionId);
			rdr = new Reader();
			ArrayList<String> datesForReport = rdr.getCorrespondingDatesForStatisticalReportsComponent(reportType, svr);
			JSONArray jsonRsult = rdr.convertArrayListToJSONArray(datesForReport);
			result = String.valueOf(jsonRsult);
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage(), e);
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
		return Response.status(200).entity(result).build();
	}

	@Path("getTerminatedAnimals/{sessionId}/{holdingObjId}/{dateFrom}/{dateTo}/{rowlimit}")
	@GET
	@Produces("application/json")
	public Response getTerminatedAnimals(@PathParam("sessionId") String sessionId,
			@PathParam("holdingObjId") Long holdingObjId, @PathParam("dateFrom") String dateFrom,
			@PathParam("dateTo") String dateTo, @PathParam("rowLimit") int rowLimit) {
		String result = Tc.EMPTY_ARRAY_STRING;
		SvReader svr = null;
		Reader rdr = null;
		try {
			svr = new SvReader(sessionId);
			rdr = new Reader();
			DbDataArray terminatedAnimals = rdr.getTerminatedAnimals(holdingObjId, dateFrom, dateTo, rowLimit, svr);
			result = rdr.convertDbDataArrayToGridJsonWithoutSorting(terminatedAnimals, Tc.ANIMAL, false, svr);
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage(), e);
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
		return Response.status(200).entity(result).build();
	}

	@Path("getTerminatedPets/{sessionId}/{holdingObjId}/{dateFrom}/{dateTo}/{rowlimit}")
	@GET
	@Produces("application/json")
	public Response getTerminatedPets(@PathParam("sessionId") String sessionId,
			@PathParam("holdingObjId") Long holdingObjId, @PathParam("dateFrom") String dateFrom,
			@PathParam("dateTo") String dateTo, @PathParam("rowLimit") int rowLimit) {
		String result = Tc.EMPTY_ARRAY_STRING;
		SvReader svr = null;
		Reader rdr = null;
		try {
			svr = new SvReader(sessionId);
			rdr = new Reader();
			DbDataArray terminatedPets = rdr.getTerminatedPets(holdingObjId, dateFrom, dateTo, rowLimit, svr);
			result = rdr.convertDbDataArrayToGridJsonWithoutSorting(terminatedPets, Tc.PET, false, svr);
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage(), e);
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
		return Response.status(200).entity(result).build();
	}

	@Path("getReleasedPets/{sessionId}/{holdingObjId}/{dateFrom}/{dateTo}/{rowlimit}")
	@GET
	@Produces("application/json")
	public Response getReleasedPets(@PathParam("sessionId") String sessionId,
			@PathParam("holdingObjId") Long holdingObjId, @PathParam("dateFrom") String dateFrom,
			@PathParam("dateTo") String dateTo, @PathParam("rowLimit") int rowLimit) {
		String result = Tc.EMPTY_ARRAY_STRING;
		SvReader svr = null;
		Reader rdr = null;
		try {
			svr = new SvReader(sessionId);
			rdr = new Reader();
			DbDataArray releasedPets = rdr.getReleasedPets(holdingObjId, dateFrom, dateTo, rowLimit, svr);
			result = rdr.convertDbDataArrayToGridJsonWithoutSorting(releasedPets, Tc.PET, false, svr);
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage(), e);
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
		return Response.status(200).entity(result).build();
	}

	@Path("getFinishedMovementDocuments/{sessionId}/{destinationHoldingPic}/{dateFrom}/{dateTo}/{rowlimit}")
	@GET
	@Produces("application/json")
	public Response getFinishedMovementDocuments(@PathParam("sessionId") String sessionId,
			@PathParam("destinationHoldingPic") String destinationHoldingPic, @PathParam("dateFrom") String dateFrom,
			@PathParam("dateTo") String dateTo, @PathParam("rowLimit") int rowLimit) {
		String result = Tc.EMPTY_ARRAY_STRING;
		SvReader svr = null;
		Reader rdr = null;
		try {
			svr = new SvReader(sessionId);
			rdr = new Reader();
			DbDataArray finishedMovementDocs = rdr.getFinishedMovementDocuments(destinationHoldingPic, dateFrom, dateTo,
					rowLimit, svr);
			result = rdr.convertDbDataArrayToGridJsonWithoutSorting(finishedMovementDocs, Tc.MOVEMENT_DOC, false, svr);
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage(), e);
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
		return Response.status(200).entity(result).build();
	}

	@Path("getFinishedAnimalMovements/{sessionId}/{holdingObjId}/{dateFrom}/{dateTo}/{rowlimit}")
	@GET
	@Produces("application/json")
	public Response getFinishedAnimalMovements(@PathParam("sessionId") String sessionId,
			@PathParam("destinationHoldingPic") Long holdingObjId, @PathParam("dateFrom") String dateFrom,
			@PathParam("dateTo") String dateTo, @PathParam("rowLimit") int rowLimit) {
		String result = Tc.EMPTY_ARRAY_STRING;
		SvReader svr = null;
		Reader rdr = null;
		try {
			svr = new SvReader(sessionId);
			rdr = new Reader();
			DbDataArray finishedMovements = rdr.getFinishedMovements(holdingObjId, dateFrom, dateTo, rowLimit, svr);
			log4j.trace(finishedMovements.size());
			result = rdr.convertDbDataArrayToGridJsonWithoutSorting(finishedMovements, Tc.ANIMAL_MOVEMENT, false, svr);
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage(), e);
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
		return Response.status(200).entity(result).build();
	}

	@Path("getHoldingsByCriteria/{sessionId}/{type}/{name}/{pic}/{keeperId}/{geoCode}/{address}/{rowlimit}")
	@GET
	@Produces("application/json")
	public Response getHoldingsByCriteria(@PathParam("sessionId") String sessionId, @PathParam("type") String type,
			@PathParam("name") String name, @PathParam("pic") String pic, @PathParam("keeperId") String keeperId,
			@PathParam("geoCode") String geoCode, @PathParam("address") String address,
			@PathParam("rowLimit") int rowLimit) {
		String result = Tc.EMPTY_ARRAY_STRING;
		SvReader svr = null;
		Reader rdr = null;
		try {
			svr = new SvReader(sessionId);
			rdr = new Reader();
			DbDataArray filteredHoldings = rdr.getFiltratedHoldings(type, name, pic, keeperId, geoCode, address,
					rowLimit, svr);
			result = rdr.convertDbDataArrayToGridJsonWithoutSorting(filteredHoldings, Tc.HOLDING, false, svr);
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage(), e);
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
		return Response.status(200).entity(result).build();
	}

	@Path("getPersonsByCriteria/{sessionId}/{idNo}/{firstName}/{lastName}/{fullName}/{geoCode}/{phoneNumber}/{rowlimit}")
	@GET
	@Produces("application/json")
	public Response getPersonsByCriteria(@PathParam("sessionId") String sessionId, @PathParam("idNo") String idNo,
			@PathParam("firstName") String firstName, @PathParam("lastName") String lastName,
			@PathParam("fullName") String fullName, @PathParam("geoCode") String geoCode,
			@PathParam("phoneNumber") String phoneNumber, @PathParam("rowLimit") int rowLimit) {
		String result = Tc.EMPTY_ARRAY_STRING;
		SvReader svr = null;
		Reader rdr = null;
		try {
			svr = new SvReader(sessionId);
			rdr = new Reader();
			DbDataArray filteredPersons = rdr.getFiltratedPersons(idNo, firstName, lastName, fullName, geoCode,
					phoneNumber, rowLimit, svr);
			result = rdr.convertDbDataArrayToGridJsonWithoutSorting(filteredPersons, Tc.HOLDING_RESPONSIBLE, false,
					svr);
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage(), e);
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
		return Response.status(200).entity(result).build();
	}

	@Path("getAnimalsByCriteria/{sessionId}/{animalId}/{status}/{animalClass}/{breed}/{color}/{country}/{rowlimit}")
	@GET
	@Produces("application/json")
	public Response getAnimalsByCriteria(@PathParam("sessionId") String sessionId,
			@PathParam("animalId") String animalId, @PathParam("status") String status,
			@PathParam("animalClass") String animalClass, @PathParam("breed") String breed,
			@PathParam("color") String color, @PathParam("country") String country,
			@PathParam("rowLimit") int rowLimit) {
		String result = Tc.EMPTY_ARRAY_STRING;
		SvReader svr = null;
		Reader rdr = null;
		try {
			svr = new SvReader(sessionId);
			rdr = new Reader();
			DbDataArray filtratedAnimals = rdr.getFiltratedAnimals(animalId, status, animalClass, breed, color, country,
					rowLimit, svr);
			result = rdr.convertDbDataArrayToGridJsonWithoutSorting(filtratedAnimals, Tc.ANIMAL, false, svr);
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage(), e);
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
		return Response.status(200).entity(result).build();
	}

	@Path("getOutgoingTransfersPerOrgUnit/{sessionId}/{parentId}/{tagType}/{startTagId}/{endTagId}/{dateFrom}/{dateTo}/{rowlimit}")
	@GET
	@Produces("application/json")
	public Response getOutgoingTransfersPerOrgUnit(@PathParam("sessionId") String sessionId,
			@PathParam("parentId") Long parentId, @PathParam("tagType") String tagType,
			@PathParam("startTagId") String startTagId, @PathParam("endTagId") String endTagId,
			@PathParam("dateFrom") String dateFrom, @PathParam("dateTo") String dateTo,
			@PathParam("rowLimit") int rowLimit) {
		String result = Tc.EMPTY_ARRAY_STRING;
		SvReader svr = null;
		Reader rdr = null;
		try {
			svr = new SvReader(sessionId);
			rdr = new Reader();
			DbDataArray filtratedAnimals = rdr.getOutgoingTransfersPerOrgUnit(parentId, tagType, startTagId, endTagId,
					dateFrom, dateTo, rowLimit, svr);
			result = rdr.convertDbDataArrayToGridJsonWithoutSorting(filtratedAnimals, Tc.TRANSFER, false, svr);
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage(), e);
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
		return Response.status(200).entity(result).build();
	}

	@Path("getIncomingTransfersPerOrgUnit/{sessionId}/{destinationOrgUnitObjId}/{tagType}/{startTagId}/{endTagId}/{dateFrom}/{dateTo}/{rowlimit}")
	@GET
	@Produces("application/json")
	public Response getIncomingTransfersPerOrgUnit(@PathParam("sessionId") String sessionId,
			@PathParam("destinationOrgUnitObjId") String destinationOrgUnitObjId, @PathParam("tagType") String tagType,
			@PathParam("startTagId") String startTagId, @PathParam("endTagId") String endTagId,
			@PathParam("dateFrom") String dateFrom, @PathParam("dateTo") String dateTo,
			@PathParam("rowLimit") int rowLimit) {
		String result = Tc.EMPTY_ARRAY_STRING;
		SvReader svr = null;
		Reader rdr = null;
		try {
			svr = new SvReader(sessionId);
			rdr = new Reader();
			DbDataArray filtratedAnimals = rdr.getIncomingTransfersPerOrgUnit(destinationOrgUnitObjId, tagType,
					startTagId, endTagId, dateFrom, dateTo, rowLimit, svr);
			result = rdr.convertDbDataArrayToGridJsonWithoutSorting(filtratedAnimals, Tc.TRANSFER, false, svr);
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage(), e);
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
		return Response.status(200).entity(result).build();
	}

	@Path("getFlocksByCriteria/{sessionId}/{flockId}/{status}/{animalClass}/{color}/{rowlimit}")
	@GET
	@Produces("application/json")
	public Response getFlocksByCriteria(@PathParam("sessionId") String sessionId, @PathParam("flockId") String flockId,
			@PathParam("status") String status, @PathParam("animalClass") String animalClass,
			@PathParam("color") String color, @PathParam("rowLimit") int rowLimit) {
		String result = Tc.EMPTY_ARRAY_STRING;
		SvReader svr = null;
		Reader rdr = null;
		try {
			svr = new SvReader(sessionId);
			rdr = new Reader();
			DbDataArray filtratedAnimals = rdr.getFiltratedFlocks(flockId, status, animalClass, color, rowLimit, svr);
			result = rdr.convertDbDataArrayToGridJsonWithoutSorting(filtratedAnimals, Tc.FLOCK, false, svr);
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage(), e);
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
		return Response.status(200).entity(result).build();
	}

	@Path("checkIfAnimalParticipatedInCampaign/{sessionId}/{animalObjId}/{campaignObjId}")
	@GET
	@Produces("application/json")
	public Response checkIfAnimalParticipatedInCampaign(@PathParam("sessionId") String sessionId,
			@PathParam("animalObjId") Long animalObjId, @PathParam("campaignObjId") Long campaignObjId) {
		String resultMsgLbl = Tc.EMPTY_ARRAY_STRING;
		Boolean ifAnimalParticipatedInCampaign = false;
		SvReader svr = null;
		Reader rdr = null;
		try {
			svr = new SvReader(sessionId);
			rdr = new Reader();
			ifAnimalParticipatedInCampaign = rdr.checkIfAnimalParticipatedInCampaign(animalObjId, campaignObjId, svr);
			resultMsgLbl = ifAnimalParticipatedInCampaign.toString();
		} catch (SvException e) {
			resultMsgLbl = e.getLabelCode();
			log4j.error("Error in checkIfAnimalParticipatedInCampaign: " + e.getFormattedMessage(), e);
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
		return Response.status(200).entity(resultMsgLbl).build();
	}
}