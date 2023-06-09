package naits_triglav_plugin;

import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.FileHandler;
import java.util.logging.SimpleFormatter;
import java.util.Random;
import java.util.Scanner;
import java.util.TimeZone;
import java.util.UUID;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.math.NumberUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.joda.time.DateTime;
import org.joda.time.DateTimeComparator;
import org.joda.time.Duration;
import org.joda.time.Period;
import org.joda.time.format.DateTimeFormat;
import org.json.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.prtech.svarog.I18n;
import com.prtech.svarog.SvConf;
import com.prtech.svarog.SvCore;
import com.prtech.svarog.SvException;
import com.prtech.svarog.SvFileStore;
import com.prtech.svarog.SvGeometry;
import com.prtech.svarog.SvLink;
import com.prtech.svarog.SvLock;
import com.prtech.svarog.SvNote;
import com.prtech.svarog.SvNotification;
import com.prtech.svarog.SvParameter;
import com.prtech.svarog.SvReader;
import com.prtech.svarog.SvSecurity;
import com.prtech.svarog.SvSequence;
import com.prtech.svarog.SvUtil;
import com.prtech.svarog.SvWorkflow;
import com.prtech.svarog.SvWriter;
import com.prtech.svarog.svCONST;
import com.prtech.svarog_common.DbDataArray;
import com.prtech.svarog_common.DbDataObject;
import com.prtech.svarog_common.DbQueryExpression;
import com.prtech.svarog_common.DbQueryObject;
import com.prtech.svarog_common.DbSearch;
import com.prtech.svarog_common.DbSearchCriterion;
import com.prtech.svarog_common.DbSearchCriterion.DbCompareOperand;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

import com.prtech.svarog_common.DbSearchExpression;
import com.prtech.svarog_common.SvCharId;

import naits_triglav_plugin.ReactJsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class NaitsPluginTests {

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

	static final Logger log4j = LogManager.getLogger(NaitsPluginTests.class.getName());

	static HashMap<String, Object[]> passRecoveryTokens = new HashMap<String, Object[]>();

	public static String login(String username, String password) {
		String tokenLocal = "";
		try {
			SvSecurity svSec = new SvSecurity();
			log4j.debug(SvUtil.getMD5(password));
			tokenLocal = svSec.logon(username, SvUtil.getMD5(password));
		} catch (Exception e) {
			return "";
		}
		return tokenLocal;
	}

	public String testFlockDirectTransfer(Long animalOrFlockId, String destinationHoldingObjId,
			String transporterPersonId, String dateOfAdmission, SvReader svr) throws SvException {
		String resultMsgLabel = "naits.error.invalidInputInformationsForDirectTransfer";
		SvWriter svw = null;
		SvWorkflow sww = null;
		ValidationChecks vc = null;
		Reader rdr = null;
		try {
			svw = new SvWriter(svr);
			sww = new SvWorkflow(svw);
			Writer wr = new Writer();
			vc = new ValidationChecks();
			rdr = new Reader();
			// rdr instance of this service can be additionally completed,
			// depending on the client requirements
			if (animalOrFlockId != null && destinationHoldingObjId != null && dateOfAdmission != null
					&& transporterPersonId != null) {
				resultMsgLabel = "naits.error.transfer.fail.anmalIdDoesNotExist";
				ArrayList<String> mandatoryFields = new ArrayList<>();
				DbDataObject dboAnimal = svr.getObjectById(animalOrFlockId, SvReader.getTypeIdByName(Tc.ANIMAL),
						new DateTime());
				DbDataObject dboFlock = svr.getObjectById(animalOrFlockId, SvReader.getTypeIdByName(Tc.FLOCK), null);
				DbDataObject dboToMove = null;
				if (dboAnimal != null) {
					dboToMove = dboAnimal;
					mandatoryFields = rdr.getAnimalMandatoryFields(dboAnimal, svr);
				} else {
					dboToMove = dboFlock;
				}
				if (dboToMove != null && (mandatoryFields == null || mandatoryFields.size() == 0)) {
					DbDataObject dboDestinationHolding = svr.getObjectById(Long.valueOf(destinationHoldingObjId),
							SvReader.getTypeIdByName(Tc.HOLDING), null);
					DbDataObject dboSourceHolding = svr.getObjectById(dboToMove.getParent_id(),
							SvReader.getTypeIdByName(Tc.HOLDING), null);
					if (dboSourceHolding != null) {
						if (dboDestinationHolding != null && !dboToMove.getStatus().equals(Tc.TRANSITION)) {
							wr.startAnimalOrFlockMovement(dboToMove, dboDestinationHolding, "DIRECT_TRANSFER", null,
									null, null, null, null, null, null, null, svr, svw, sww);
							svw.dbCommit();
							sww.dbCommit();
							if (vc.checkIfHoldingIsSlaughterhouse(dboDestinationHolding) && dboAnimal != null) {
								wr.finishAnimalOrFlockMovement(dboToMove, dboDestinationHolding, null, dateOfAdmission,
										transporterPersonId, svw, sww);
							} else {
								wr.finishAnimalOrFlockMovement(dboToMove, dboDestinationHolding, null, null, null, svw,
										sww);
							}
							svw.dbCommit();
							sww.dbCommit();
							resultMsgLabel = "naits.success.transfer";
						} else {
							resultMsgLabel = "naits.error.selectedAnimalIsInTransitionSoCantBeDirectTransfered";
						}
					} else {
						resultMsgLabel = "naits.error.sourceHoldingBelongsToActiveQuarantine";
					}
				} else if (dboToMove != null && mandatoryFields != null && mandatoryFields.size() > 0) {
					resultMsgLabel = I18n.getText(svr.getUserLocaleId(svr.getInstanceUser()).toString(),
							"naits.info.mandatoryFieldsForDirectTransfer") + " " + mandatoryFields.toString();
				}

			}
		} catch (SvException e) {
			log4j.error("Error occured in checkIfAnimalIdExist:" + e.getFormattedMessage(), e);
			resultMsgLabel = "naits.error.transfer.fail";
		} finally {
			if (svw != null) {
				svw.release();
			}
			if (sww != null) {
				sww.release();
			}
		}
		return resultMsgLabel;
	}

	@Test
	public void testPrepareReactJsonUiForm() {
		String token = login("A.ADMIN3", "naits1234");
		SvReader svr = null;
		Long parentTypeId = 21485L;
		Long parentId = 188217L;
		try {
			svr = new SvReader(token);
			ReactJsonBuilder cjb = new ReactJsonBuilder();
			DbDataArray allYNDocuments = svr.getObjectsByParentId(parentTypeId, svCONST.OBJECT_TYPE_FORM_TYPE, null, 0,
					0);
			System.out.println(allYNDocuments.toJson());
			String temp = cjb.prepareReactJsonUISchema("DROPDOWN", allYNDocuments);
			System.out.println(temp);
			// DbDataArray formVals = svr.getFormsByParentId(parentId,
			// allYNDocuments.get(0).getObject_id(), null, null);
			DbDataArray formVals = new DbDataArray();
			for (DbDataObject tempForm : allYNDocuments.getItems()) {
				DbDataArray tempFormVals = svr.getFormsByParentId(parentId, tempForm.getObject_id(), null, null);
				if (tempFormVals != null && tempFormVals.size() > 0) {
					formVals.addDataItem(tempFormVals.getItems().get(0));
				}
			}
			String temp2 = cjb.prepareReactJsonFormData("DROPDOWN", formVals);
			System.out.println(temp2);
			System.out.println(formVals.toJson());
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage(), e);
			e.printStackTrace();
			fail();
		} finally {
			if (svr != null)
				svr.release();
		}
	}

	@Test
	public void testGetGeneralYNDoc() {
		String token = login("ADMIN", "welcome");
		SvReader svr = null;
		Long parentTypeId = 21485L;
		Long parentId = 188217L;
		try {
			svr = new SvReader(token);
			DbDataArray allYNDocuments = svr.getObjectsByParentId(parentTypeId, svCONST.OBJECT_TYPE_FORM_TYPE, null, 0,
					0);
			System.out.println(allYNDocuments.toJson());
			DbDataArray formVals = svr.getFormsByParentId(parentId, allYNDocuments.get(0).getObject_id(), null, null);
			System.out.println(formVals.toJson());
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage(), e);
			e.printStackTrace();
			fail();
		} finally {
			if (svr != null)
				svr.release();
		}
	}

	@Test
	public void testCreateUserFromImportedPersons() {
		String token = login("ADMIN", "welcome");
		SvReader svr = null;
		SvSecurity svs = null;
		try {
			svr = new SvReader(token);
			svs = new SvSecurity();
			DbSearchCriterion cr1 = new DbSearchCriterion("OWNER_TYPE", DbCompareOperand.EQUAL, "2");
			DbDataArray results = svr.getObjects(new DbSearchExpression().addDbSearchItem(cr1),
					SvReader.getTypeIdByName("PREMISE_OWNER"), null, 0, 0);
			log4j.info("Total premise owners for import: " + results.size());
			int cnt_new = 0;
			int cnt_existing = 0;
			for (DbDataObject tempPerson : results.getItems()) {
				/* check if user already exist */
				DbSearchExpression getUser = new DbSearchExpression();
				DbSearchCriterion filterByUserName = new DbSearchCriterion("USER_NAME", DbCompareOperand.EQUAL,
						(String) tempPerson.getVal("NAT_REG_NUMBER"));
				getUser.addDbSearchItem(filterByUserName);
				Boolean userExists = svs.checkIfExists("SVAROG_USERS", getUser);
				if (userExists.equals(false)) {
					String firstName = (String) tempPerson.getVal("FIRST_NAME");
					String lastName = (String) tempPerson.getVal("LAST_NAME");
					if (tempPerson.getVal("FULL_NAME") != null) {
						String[] splitFullName = tempPerson.getVal("FULL_NAME").toString().split("\\s+");
						if (firstName == null) {
							firstName = splitFullName[0];
						}
						if (lastName == null) {
							lastName = splitFullName[1];
						}
					}

					DbDataObject tempUser = svs.createUser((String) tempPerson.getVal("NAT_REG_NUMBER"),
							(String) tempPerson.getVal("NAT_REG_NUMBER"), firstName, lastName, "info@email.com",
							(String) tempPerson.getVal("NAT_REG_NUMBER"), null, "EXTERNAL", "VALID", false);
					svs.empowerUser(tempUser, tempPerson);
					cnt_new++;
				} else {
					cnt_existing++;
				}
			}
			log4j.info(cnt_existing + " users already exist.");
			log4j.info(cnt_new + " new users created.");
			// svs.dbCommit();
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage(), e);
			e.printStackTrace();
			fail();
		} finally {
			if (svr != null)
				svr.release();
			if (svs != null)
				svs.release();
		}
	}

	@Test
	public void testGetLabelForCodeListItem() {
		String token = login("ADMIN", "welcome");
		SvReader svr = null;
		Long tableId = 21508L; // id for animal
		String fieldName = "COLOR";
		String value = "7";
		String languageId = "en_US";
		try {
			svr = new SvReader(token);
			Reader r = new Reader();
			String result = r.decodeCodeValue(tableId, fieldName, value, languageId, svr);
			log4j.info(result);
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage(), e);
			e.printStackTrace();
			fail();
		}

	}

	@Test
	public void testSaveAnimal() {
		String token = login("ADMIN", "welcome");
		SvReader svReader = null;
		SvWriter svWriter = null;
		try {
			svReader = new SvReader(token);
			svWriter = new SvWriter(svReader);
			Writer wr = new Writer();
			wr.saveSimplifiedAnimalData("M", "22.01.1880", "1", "1005", "123456789", "66677799994", 1234L, svReader,
					svWriter);

		} catch (SvException e) {
			log4j.error(e.getFormattedMessage(), e);
			e.printStackTrace();
			fail();
		}

	}

	@Test
	public void changePassAfterecovery() {
		String token = login("ABA_DABA", "welcome");
		cleanUpRecoveryData();
		DbDataObject dboUser = null;
		SvSecurity svs = null;
		String error = "";
		try {
			svs = new SvSecurity();
			dboUser = svs.getUser("ABA_DABA");
			if (dboUser != null) {
				Object[] recoveryData = new Object[2];
				recoveryData[0] = UUID.randomUUID().toString();
				recoveryData[1] = new DateTime();
				passRecoveryTokens.put((String) dboUser.getVal("USER_NAME"), recoveryData);
				dboUser.setVal("RECOVERY_TOKEN", recoveryData);
				// sendPassRecoveryEmail(dboUser, httpRequest);

			} else {
				error = I18n.getText("err.user_not_found");
				// return Response.status(200).entity(error).build();
			}

			cleanUpRecoveryData();
			/*
			 * recoveryData = passRecoveryTokens.get(username); if (recoveryData == null ||
			 * !token.equals(recoveryData[0])) { returnMsgLbl =
			 * "updatePassword.error.tokenNotExist"; return
			 * Response.status(200).entity(I18n.getText(returnMsgLbl)).build(); }
			 */
		} catch (SvException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Test
	public void createUserGroups22() {
		SvReader svReader = null;
		SvWriter svWriter = null;
		SvSecurity svc = null;
		String token = login("ADMIN", "welcome");
		try {
			svReader = new SvReader(token);
			svWriter = new SvWriter(svReader);

			svc = new SvSecurity(svWriter);
			DbDataObject dbDataEntryClerks = null;
			dbDataEntryClerks = findUserOrUserGroup(svCONST.OBJECT_TYPE_GROUP, "GROUP_TYPE", "DATA_ENTRY_CLERK",
					svReader);
			if (dbDataEntryClerks == null) {
				dbDataEntryClerks = createDataEntryClerkUserGroup();
				svWriter.saveObject(dbDataEntryClerks);
			}

			DbDataObject dbDataTestUser = svc.getUser("R.PEJOV");
			// svWriter.saveObject(dbDataTestUser);

			// svc.addUserToGroup(dbDataTestUser, dbDataEntryClerks, false);

			ArrayList<String> permissions = new ArrayList<String>(
					Arrays.asList("HOLDING.FULL", "HOLDING_RESPONSIBLE.FULL", "ANIMAL.FULL", "VACCINATION_BOOK.FULL",
							"VACCINATION_EVENT.FULL", "VACCINATION_RESULTS.FULL", "system.null_geometry"));

			ArrayList<String> corePermissions = new ArrayList<String>();
			DbSearchCriterion cr11 = new DbSearchCriterion("SYSTEM_TABLE", DbCompareOperand.EQUAL, true);
			DbDataArray dbArray1 = svReader.getObjects(new DbSearchExpression().addDbSearchItem(cr11),
					svCONST.OBJECT_TYPE_TABLE, null, 0, 0);
			for (DbDataObject tempDbo : dbArray1.getItems()) {
				corePermissions.add(tempDbo.getVal("TABLE_NAME") + ".FULL");
			}

			try {
				// grant permissions to group
				if (setSidPermission(dbDataEntryClerks.getVal("GROUP_NAME").toString(), svCONST.OBJECT_TYPE_GROUP,
						permissions, "GRANT", token))
					log4j.info("Permissions granted.");
				else
					log4j.info("Core permissions not granted.");

				if (setSidPermission(dbDataEntryClerks.getVal("GROUP_NAME").toString(), svCONST.OBJECT_TYPE_GROUP,
						corePermissions, "GRANT", token))
					log4j.info("Custom permissions granted.");
				else
					log4j.info("Custom permissions  not granted.");
			} catch (Exception e) {
				if (e instanceof SvException)
					System.out.println(((SvException) e).getFormattedMessage());
				e.printStackTrace();
				// fail("Test raised an exception");
			}
			svWriter.dbCommit();
		}

		catch (SvException e) {
			log4j.error("Error getting screen forms (support types) for app type: ", e.getFormattedMessage());
		}
	}

	public DbDataObject findUserOrUserGroup(Long objToSearchIn, String columnToCompare, String valueToCompare,
			SvReader svr) throws SvException {
		DbDataObject result = null;
		DbSearchCriterion cr11 = new DbSearchCriterion(columnToCompare, DbCompareOperand.EQUAL, valueToCompare);
		DbDataArray results = svr.getObjects(new DbSearchExpression().addDbSearchItem(cr11), objToSearchIn, null, 0, 0);
		if (results.size() > 0) {
			result = results.get(0);
		}
		return result;
	}

	// USER_GROUP
	public static DbDataObject createDataEntryClerkUserGroup() {
		DbDataObject dboGroup = new DbDataObject();
		dboGroup.setObject_type(svCONST.OBJECT_TYPE_GROUP);
		dboGroup.setVal("GROUP_TYPE", "DATA_ENTRY_CLERK");
		dboGroup.setVal("GROUP_UID", SvUtil.getUUID());
		dboGroup.setVal("GROUP_NAME", "DATA_ENTRY_CLERK");
		dboGroup.setVal("E_MAIL", "admin@admin.com");
		dboGroup.setVal("GROUP_SECURITY_TYPE", "POA");
		return dboGroup;
	}

	// USER_GROUP
	public static DbDataObject createPrivateVeterinariansGroup() {
		DbDataObject dboGroup = new DbDataObject();
		dboGroup.setObject_type(svCONST.OBJECT_TYPE_GROUP);
		dboGroup.setVal("GROUP_TYPE", "VET_OFFICERS");
		dboGroup.setVal("GROUP_UID", SvUtil.getUUID());
		dboGroup.setVal("GROUP_NAME", "PRIVATE_VETERINARIANS");
		dboGroup.setVal("E_MAIL", "admin@admin.com");
		dboGroup.setVal("GROUP_SECURITY_TYPE", "POA");
		return dboGroup;
	}

	public static DbDataObject createVeterinariansGroup() {
		DbDataObject dboGroup = new DbDataObject();
		dboGroup.setObject_type(svCONST.OBJECT_TYPE_GROUP);
		dboGroup.setVal("GROUP_TYPE", "VET_OFFICERS");
		dboGroup.setVal("GROUP_UID", SvUtil.getUUID());
		dboGroup.setVal("GROUP_NAME", "VET_OFFICERS");
		dboGroup.setVal("E_MAIL", "admin@admin.com");
		dboGroup.setVal("GROUP_SECURITY_TYPE", "POA");
		return dboGroup;
	}

	// USER
	public static DbDataObject createDefaultTestUser() {
		DbDataObject dboUser = new DbDataObject();
		dboUser.setObject_type(svCONST.OBJECT_TYPE_USER);
		dboUser.setVal("USER_TYPE", "INTERNAL");
		dboUser.setVal("USER_UID", UUID.randomUUID().toString());
		dboUser.setVal("USER_NAME", "ABA_DABA");
		dboUser.setVal("FIRST_NAME", "ABA");
		dboUser.setVal("LAST_NAME", "DABA");
		dboUser.setVal("PIN", "67831213");
		dboUser.setVal("E_MAIL", "aba_daba@gmail.com");
		dboUser.setVal("PASSWORD_HASH", SvUtil.getMD5(SvUtil.getMD5("welcome")));
		return dboUser;
	}

	// USER
	public static DbDataObject createVetTestUser() {
		DbDataObject dboUser = new DbDataObject();
		dboUser.setObject_type(svCONST.OBJECT_TYPE_USER);
		dboUser.setVal("USER_TYPE", "INTERNAL");
		dboUser.setVal("USER_UID", UUID.randomUUID().toString());
		dboUser.setVal("USER_NAME", "VET_001");
		dboUser.setVal("FIRST_NAME", "John");
		dboUser.setVal("LAST_NAME", "Michaelos");
		dboUser.setVal("PIN", "67831213");
		dboUser.setVal("E_MAIL", "john_michaelosa@gmail.com");
		dboUser.setVal("PASSWORD_HASH", SvUtil.getMD5(SvUtil.getMD5("welcome")));
		return dboUser;
	}

	public boolean setSidPermission(String sidName, Long sidType, ArrayList<String> permissionKeys, String operation,
			String token) {
		SvSecurity svs = null;
		SvReader svr = null;
		DbDataObject sid = null;
		try {
			svr = new SvReader(token);
			svr.switchUser("ADMIN");
			svs = new SvSecurity(svr);
			svs.setAutoCommit(false);
			sid = svs.getSid(sidName, sidType);
			for (String permissionKey : permissionKeys)
				if (operation.equals("GRANT"))
					svs.grantPermission(sid, permissionKey);
				else if (operation.equals("REVOKE"))
					svs.revokePermission(sid, permissionKey);
				else
					System.out.println("Operation must be either GRANT or REVOKE, Wrong operation:" + operation);
			svs.dbCommit();
		} catch (SvException e) {
			return false;
		} finally {
			if (svs != null)
				svs.release();
			if (svr != null)
				svr.release();
		}
		return true;

	}

	public void createExternalUser(String userName, String password, String pinVat, String eMail, String firstName,
			String lastName, String initials, String initial_user_password, String phone_number, String mobile_number,
			String address, String job_desig_code, String job_desig_descr, SvReader svr) {
		String error = null;
		String pin = "";
		String taxId = "";
		String resultMsg = "";
		if (!(userName == null || password == null || eMail == null || pinVat == null)) {
			DbDataObject dboUser = null;
			DbDataObject dboUserDetails = null;
			SvSecurity svs = null;
			SvWriter svw = null;
			try {
				svs = new SvSecurity(svr);
				svw = new SvWriter(svr);
				Boolean userExists = svs.checkIfUserExistsByUserName(userName);
				if (userExists) {
					resultMsg = "err;" + I18n.getText("user.user_exist");
					return;
				}

				dboUser = svs.createUser(userName.toUpperCase(), password.toUpperCase(), firstName.toUpperCase(),
						lastName.toUpperCase(), eMail, pin.toUpperCase(), taxId.toUpperCase(), "EXTERNAL", "PENDING");

				if (dboUser != null) {
					error = "ok;" + I18n.getText("user.user_created");
					Writer wr = new Writer();
					/*
					 * dboUserDetails = wr.createUserDetails(dboUser, initials,
					 * initial_user_password, phone_number, mobile_number, address, job_desig_code,
					 * job_desig_descr, svw); svw.saveObject(dboUserDetails);
					 */
					// sendActivationEmail(dboUser, httpRequest);
					svw.dbCommit();
				}

			} catch (SvException e) {
				error = "err;" + I18n.getText(e.getLabelCode());
				log4j.error("Error in create external user:", e.getFormattedMessage());
			} finally {
				if (svs != null) {
					svs.release();
				}
				if (svw != null) {
					svw.release();
				}
			}
		}

	}

	@Test
	public void createExternalUser() {
		SvReader svReader = null;
		String retvalString = "";
		String token = login("ADMIN", "welcome");
		ArrayList<DbDataObject> sortedListScreenForms = new ArrayList<DbDataObject>();
		try {
			svReader = new SvReader(token);

			createExternalUser("A_MAKEDONSKI", "12345678", "20041999605023", "a.makedonski@gmail.com", "ALEKSANDAR",
					"MAKEODNSKI", "A.M.", null, null, "070255255", "Plostad na R.Makedonija", "Voin", "Voin na konj",
					svReader);

		} catch (SvException e) {
			log4j.error("Error getting screen forms (support types) for app type: ", e.getFormattedMessage());
			// return
			// Response.status(401).entity(e.getFormattedMessage()).build();
		} finally {
			if (svReader != null) {
				svReader.release();
			}
			if (svReader != null) {
				svReader.release();
			}

		}
		System.out.println(retvalString);
	}

	@Test
	public void testBeforeSaveValidations() throws SvException {
		SvWriter svw = null;
		SvReader svr = null;
		Reader rdr = new Reader();
		try {
			String token = login("A.ADMIN3", "naits1234");
			OnSaveValidations call = new OnSaveValidations();
			SvCore.registerOnSaveCallback(call, SvReader.getTypeIdByName("LAB_SAMPLE"));
			svr = new SvReader(token);
			svw = new SvWriter(svr);
			svw.dbSetAutoCommit(false);
			DbDataObject dboAnimal = rdr.findAppropriateAnimalByAnimalId("10550790", svr);
			// dboLabSample = svr.getObjectById(2597864L,
			// SvReader.getTypeIdByName(Tc.LAB_SAMPLE), null);
			DbDataObject dboLabSample = new DbDataObject();
			dboLabSample.setObject_type(SvReader.getTypeIdByName(Tc.LAB_SAMPLE));
			dboLabSample.setVal(Tc.DATE_OF_COLLECTION, "2003-03-03");
			// dboLabSample.setVal(Tc.LABORATORY_NAME, "TEST_TEST_4");
			dboLabSample.setVal(Tc.DISEASE_TEST, "3");
			dboLabSample.setVal(Tc.SAMPLE_TYPE, "2");
			dboLabSample.setVal(Tc.SAMPLE_ORIGIN, "1");

			dboLabSample.setParent_id(dboAnimal.getObject_id());
			svw.saveObject(dboLabSample);
			svw.dbCommit();
			log4j.debug(dboLabSample.toSimpleJson());
		} catch (SvException e) {
			log4j.error("Error: ", e.getFormattedMessage());
			// fail();
		} finally {
			if (svw != null) {
				svw.dbRollback();
				svw.release();
			}
		}
	}

	@Test
	public void testGetNotificationPerUser() throws SvException {
		DbDataArray notificationsFound = new DbDataArray();
		SvReader svr = null;
		SvWriter svw = null;
		SvLink svl = null;
		String token = login("L.JANEV", "naits123");

		svr = new SvReader(token);
		svw = new SvWriter(svr);
		svl = new SvLink(svw);
		svl.setAutoCommit(false);
		svl.dbSetAutoCommit(false);

		// get user from the current session
		DbDataObject userObj = svr.getUserBySession(svr.getSessionId());

		// improvise existing notification per user
		DbDataObject notificationObj = new DbDataObject();
		notificationObj.setObject_type(svCONST.OBJECT_TYPE_NOTIFICATION);
		notificationObj.setStatus("VALID");
		notificationObj.setVal("TYPE", "SYSTEM");
		notificationObj.setVal("TITLE", "SYSTEM ALERT 7!");
		notificationObj.setVal("MESSAGE", "Notification 7. Please read carefully.");
		notificationObj.setVal("SENDER", "Administrator");
		notificationObj.setVal("EVENT_ID", new Long("22"));
		svw.saveObject(notificationObj, false);
		svw.dbCommit();
		DbDataObject linkNotificationUser = SvLink.getLinkType("LINK_NOTIFICATION_USER",
				svCONST.OBJECT_TYPE_NOTIFICATION, svCONST.OBJECT_TYPE_USER);
		svl.linkObjects(notificationObj.getObject_id(), userObj.getObject_id(), linkNotificationUser.getObject_id(), "",
				false, false);
		svw.dbCommit();
		// get all notifications per user
		notificationsFound = svr.getObjectsByLinkedId(userObj.getObject_id(), svCONST.OBJECT_TYPE_USER,
				linkNotificationUser, svCONST.OBJECT_TYPE_NOTIFICATION, true, null, 0, 0);

		// find the userGroup for the user
		DbDataArray userGroups = svr.getUserGroups();
		DbDataObject linkUserAndUserGroup = SvLink.getLinkType("USER_GROUP", svCONST.OBJECT_TYPE_USER,
				svCONST.OBJECT_TYPE_GROUP);
		DbDataObject linkUserAndUserDefaultGroup = SvLink.getLinkType("USER_DEFAULT_GROUP", svCONST.OBJECT_TYPE_USER,
				svCONST.OBJECT_TYPE_GROUP);

		DbDataArray userDefaultGroupArr = svr.getObjectsByLinkedId(userObj.getObject_id(), svCONST.OBJECT_TYPE_USER,
				linkUserAndUserDefaultGroup, svCONST.OBJECT_TYPE_GROUP, false, null, 0, 0);
		if (userDefaultGroupArr.size() > 0)
			for (DbDataObject tempGroup : userDefaultGroupArr.getItems())
				userGroups.addDataItem(tempGroup);

		DbDataArray userGroupArr = svr.getObjectsByLinkedId(userObj.getObject_id(), svCONST.OBJECT_TYPE_USER,
				linkUserAndUserGroup, svCONST.OBJECT_TYPE_GROUP, false, null, 0, 0);
		if (userGroupArr.size() > 0)
			for (DbDataObject tempGroup : userGroupArr.getItems())
				userGroups.addDataItem(tempGroup);

		DbDataArray notificationsFoundPerUserGroup = new DbDataArray();
		// get all notifications per userGroup on which the user belongs
		if (userGroups.size() > 0) {
			DbDataObject linkNotificationUserGroup = SvLink.getLinkType("LINK_NOTIFICATION_GROUP",
					svCONST.OBJECT_TYPE_NOTIFICATION, svCONST.OBJECT_TYPE_GROUP);
			DbDataArray tempNotificationsFound = new DbDataArray();
			for (DbDataObject tempUserGroup : userGroups.getItems()) {
				tempNotificationsFound = svr.getObjectsByLinkedId(tempUserGroup.getObject_id(),
						svCONST.OBJECT_TYPE_GROUP, linkNotificationUserGroup, svCONST.OBJECT_TYPE_NOTIFICATION, true,
						null, 0, 0);
				if (tempNotificationsFound.size() > 0) {
					for (DbDataObject tempnotification : tempNotificationsFound.getItems())
						notificationsFoundPerUserGroup.addDataItem(tempnotification);
				}
				tempNotificationsFound = new DbDataArray();
			}
			System.out.println(notificationsFoundPerUserGroup.getItems().size());
		}

		System.out.println(notificationsFound.getItems().size());

	}

	@Test
	public void testRecoverPass() throws SvException {
		SvReader svr = null;
		String token = login("ADMIN", "welcome");
		DbDataArray allowedCustomObjects = new DbDataArray();
		String result = null;
		SvSecurity svs = new SvSecurity();
		DbDataObject dboUser = svs.getUser("ABA_DABA");

		cleanUpRecoveryData();
		if (dboUser != null) {
			Object[] recoveryData = new Object[2];
			recoveryData[0] = UUID.randomUUID().toString();
			recoveryData[1] = new DateTime();
			passRecoveryTokens.put((String) dboUser.getVal("USER_NAME"), recoveryData);
			dboUser.setVal("RECOVERY_TOKEN", recoveryData);
			// sendPassRecoveryEmail(dboUser, httpRequest);

		} else {
			log4j.info("err.user_not_found");
		}
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

	@Test
	public void testGetPermissions() throws SvException {
		SvReader svr = null;
		String token = login("R.PEJOV", "naits123");
		DbDataArray allowedCustomObjects = new DbDataArray();
		String result = null;
		// SvSecurity svs = new SvSecurity();
		try {
			// DbDataObject dboUser1 = svs.getUser("aba_daba");
			svr = new SvReader(token);
			DbDataObject dboUser = SvReader.getUserBySession(token);
			svr = new SvReader(token);
			HashMap<SvCore.SvAclKey, HashMap<String, DbDataObject>> allPermissions = svr.getPermissions();
			Iterator it = allPermissions.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry pair = (Map.Entry) it.next();

				// System.out.println(pair.getKey() + " = " + pair.getValue());
				SvCore.SvAclKey tempAllowedObject = (SvCore.SvAclKey) pair.getKey();
				Long tempObjId = tempAllowedObject.getObjectId();
				System.out.println(tempObjId);

				// System.out.println(temp22);
				DbDataObject dboTable = svr.getObjectById(tempObjId, svCONST.OBJECT_TYPE_TABLE, null);
				// System.out.println(dboTable.getVal("TABLE_NAME"));
				if (dboTable != null && dboTable.getVal("TABLE_NAME") != null
						&& dboTable.getVal("TABLE_NAME").toString().toUpperCase().contains("SVAROG")) {
					System.out.println(dboTable.getVal("TABLE_NAME"));
					allowedCustomObjects.addDataItem(dboTable);
				}
				// it.remove(); // avoids a ConcurrentModificationException
			}

			result = allowedCustomObjects.toJson().toString();
			System.out.println(result);
			log4j.info(allPermissions.size());
		} catch (SvException e) {
			log4j.error("Error: ", e.getFormattedMessage());
			fail();
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
	}

	@Test
	public void createVetOfficers() {
		SvReader svReader = null;
		SvWriter svWriter = null;
		SvSecurity svc = null;
		String retvalString = "";
		String token = login("ADMIN", "welcome");
		ArrayList<DbDataObject> sortedListScreenForms = new ArrayList<DbDataObject>();
		try {
			svReader = new SvReader(token);
			svWriter = new SvWriter(svReader);
			svc = new SvSecurity(svWriter);

			DbDataObject vetOfficersGroup = null;
			vetOfficersGroup = findUserOrUserGroup(svCONST.OBJECT_TYPE_GROUP, "GROUP_TYPE", "VET_OFFICERS", svReader);
			if (vetOfficersGroup == null) {
				System.out.println("User group VET_OFFICERS was not found");
				fail();
			}

			Boolean vetExist = svc.checkIfUserExistsByUserName("VET_001");
			DbDataObject dbDataTestUser = null;
			if (vetExist.equals(false)) {
				System.out.println("The test user VET_001 does not exist.");
				svc.createUser("VET_001", "welcome", "John", "Mikaelos", "john.mikaelos@pin.com", "654321", "",
						"INTERNAL", "VALID", false);
				System.out.println("The test user VET_001 was created.");
			} else {
				dbDataTestUser = svc.getUser("VET_001");
			}
			svc.switchUser(dbDataTestUser);
			DbDataArray linkedGroups = svc.getUserGroups();
			if (linkedGroups.size() > 0
					&& !linkedGroups.getItems().get(0).getObject_id().equals(vetOfficersGroup.getObject_id())) {
				System.out.println(
						"The test user VET_001 does not belong to group VET_OFFICERS. Linking user with user group in progress.");
				svc.addUserToGroup(dbDataTestUser, vetOfficersGroup, false);
				System.out.println("Linking finished succesfully.");
			}
			System.out.println("The test user VET_001 belongs to group VET_OFFICERS.");

			ArrayList<String> permissions = new ArrayList<String>(Arrays.asList("VACCINATION_BOOK.FULL",
					"VACCINATION_RESULTS.FULL", "VACCINATION_EVENT.FULL", "ANIMALS.READ"));
			ArrayList<String> corePermissions = new ArrayList<String>();
			DbSearchCriterion cr11 = new DbSearchCriterion("SYSTEM_TABLE", DbCompareOperand.EQUAL, true);
			DbDataArray dbArray1 = svReader.getObjects(new DbSearchExpression().addDbSearchItem(cr11),
					svCONST.OBJECT_TYPE_TABLE, null, 0, 0);

			for (DbDataObject tempDbo : dbArray1.getItems()) {
				corePermissions.add(tempDbo.getVal("TABLE_NAME") + ".FULL");
			}
			// grant permissions to group
			if (setSidPermission(vetOfficersGroup.getVal("GROUP_NAME").toString(), svCONST.OBJECT_TYPE_GROUP,
					permissions, "GRANT", token))
				log4j.info("Permissionas granted.");
			else
				log4j.info("Core permissions granted.");
			// fail("Can't grant permission");

			// grant core permissions to group
			if (setSidPermission(vetOfficersGroup.getVal("GROUP_NAME").toString(), svCONST.OBJECT_TYPE_GROUP,
					corePermissions, "GRANT", token))
				log4j.info("Core permissions granted.");
			else
				log4j.info("Core permissions granted.");
			// fail("Can't grant permission");

		} catch (SvException e) {
			log4j.error("Error getting screen forms (support types) for app type: ", e.getFormattedMessage());
		} finally {
			if (svReader != null) {
				svReader.release();
			}
			if (svWriter != null) {
				svWriter.release();
			}
		}
	}

	@Test
	public void createUserGroups() {
		SvReader svReader = null;
		SvWriter svWriter = null;
		SvSecurity svc = null;
		String retvalString = "";
		String token = login("ADMIN", "welcome");
		ArrayList<DbDataObject> sortedListScreenForms = new ArrayList<DbDataObject>();
		try {
			svReader = new SvReader(token);
			svWriter = new SvWriter(svReader);

			svc = new SvSecurity(svWriter);
			DbDataObject dbDataEntryClerks = null;
			dbDataEntryClerks = findUserOrUserGroup(svCONST.OBJECT_TYPE_GROUP, "GROUP_TYPE", "DATA_ENTRY_CLERK",
					svReader);
			if (dbDataEntryClerks == null) {
				dbDataEntryClerks = createDataEntryClerkUserGroup();
				svWriter.saveObject(dbDataEntryClerks);
			}

			DbDataObject dbDataTestUser = svc.getUser("ABA_DABA");
			// svWriter.saveObject(dbDataTestUser);

			// svc.addUserToGroup(dbDataTestUser, dbDataEntryClerks, false);
			ArrayList<String> permissions = new ArrayList<String>(Arrays.asList("HOLDING.READ", "PREMISE.READ",
					"PREMISE_OWNER.READ", "LIVESTOCK.FULL", "VACCINATION_EVENT.FULL"));

			/*
			 * ArrayList<String> permissions = new ArrayList<String>(
			 * Arrays.asList("HOLDING.READ", "PREMISE.READ", "PREMISE_OWNER.READ",
			 * "LIVESTOCK.FULL", "VACCINATION_BOOK.FULL", "VACCINATION_EVENT.FULL",
			 * "VACCINATION_RESULTS.FULL"));
			 */

			ArrayList<String> corePermissions = new ArrayList<String>();
			DbSearchCriterion cr11 = new DbSearchCriterion("SYSTEM_TABLE", DbCompareOperand.EQUAL, true);
			DbDataArray dbArray1 = svReader.getObjects(new DbSearchExpression().addDbSearchItem(cr11),
					svCONST.OBJECT_TYPE_TABLE, null, 0, 0);
			for (DbDataObject tempDbo : dbArray1.getItems()) {
				corePermissions.add(tempDbo.getVal("TABLE_NAME") + ".FULL");
			}

			try {
				// grant permissions to group
				if (setSidPermission(dbDataEntryClerks.getVal("GROUP_NAME").toString(), svCONST.OBJECT_TYPE_GROUP,
						permissions, "GRANT", token))
					log4j.info("Permissionas granted.");
				else
					log4j.info("Core permissions granted.");
				// fail("Can't grant permission");

				// grant core permissions to group
				if (setSidPermission(dbDataEntryClerks.getVal("GROUP_NAME").toString(), svCONST.OBJECT_TYPE_GROUP,
						corePermissions, "GRANT", token))
					log4j.info("Core permissions granted.");
				else
					log4j.info("Core permissions granted.");
				// fail("Can't grant permission");

				/*
				 * DbSearchCriterion cr1 = new DbSearchCriterion("USER_NAME",
				 * DbCompareOperand.EQUAL, "ABA_DABA"); DbDataArray dbArray =
				 * svReader.getObjects(new DbSearchExpression().addDbSearchItem(cr1),
				 * svCONST.OBJECT_TYPE_USER, null, 0, 0);
				 * 
				 * setSidPermission(dbArray.getItems().get(0).getVal("USER_NAME" ).toString(),
				 * svCONST.OBJECT_TYPE_USER, permissions, "GRANT", token);
				 * 
				 * log4j.info("User permissionas granted.");
				 */

			} catch (Exception e) {
				if (e instanceof SvException)
					System.out.println(((SvException) e).getFormattedMessage());
				e.printStackTrace();
				// fail("Test raised an exception");
			}

		} catch (SvException e) {
			log4j.error("Error getting screen forms (support types) for app type: ", e.getFormattedMessage());
			// return
			// Response.status(401).entity(e.getFormattedMessage()).build();
		} finally {
			if (svReader != null) {
				svReader.release();
			}
			if (svWriter != null) {
				svWriter.release();
			}

		}
		System.out.println(retvalString);
	}

	@Test
	public void testChangePasswrod() {
		String error = null;
		String pin = "";
		String taxId = "";
		String resultMsg = "";
		String token = login("ABA_DABA", "welcome");

		DbDataObject dboUser = null;
		DbDataObject dboUserDetails = null;
		SvSecurity svs = null;
		SvWriter svw = null;
		SvReader svr = null;
		try {
			svs = new SvSecurity();
			svw = new SvWriter(token);
			svr = new SvReader(svw);
			DbDataObject dbUser = svr.getUserBySession(token);
			String s = dbUser.getVal("PASSWORD_HASH").toString();
			if ((dbUser.getVal("USER_NAME") == null || dbUser.getVal("PASSWORD_HASH") == null
					|| dbUser.getVal("E_MAIL") == null || dbUser.getVal("PIN") == null)) {
				fail();
			}
			Boolean userExists = svs.checkIfUserExistsByUserName(dbUser.getVal("USER_NAME").toString());
			if (userExists) {
				resultMsg = "err;" + I18n.getText("user.user_exist");
				// return Response.status(200).entity(resultMsg).build();
			}

			dboUser = svs.createUser(dbUser.getVal("USER_NAME").toString().toUpperCase(),
					dbUser.getVal("PASSWORD").toString().toUpperCase(),
					dbUser.getVal("FIRST_NAME").toString().toUpperCase(),
					dbUser.getVal("LAST_NAME").toString().toUpperCase(), dbUser.getVal("EMAIL").toString(),
					dbUser.getVal("PIN").toString().toUpperCase(), dbUser.getVal("TAX_ID").toString().toUpperCase(),
					"EXTERNAL", "PENDING");

			if (dboUser != null) {
				error = "ok;" + I18n.getText("user.user_created");
				/*
				 * Writer wr = new Writer();
				 * 
				 * dboUserDetails = wr.saveContactData(dboUser.getObject_id(), "", address, "",
				 * "", "", "", phoneNumber, mobileNumber, fax, email, svw);
				 * svw.saveObject(dboUserDetails);
				 */
				// sendActivationEmail(dboUser, httpRequest);
				svw.dbCommit();
			}

		} catch (SvException e) {
			error = "err;" + I18n.getText(e.getLabelCode());
			log4j.error("Error in create external user:", e.getFormattedMessage());
		} finally {
			if (svs != null) {
				svs.release();
			}
			if (svw != null) {
				svw.release();
			}
		}

	}

	@Test
	public void testChangePassData() {
		String token = login("ABA_DABA", "welcome");
		/*
		 * String result = changePasswordCore(token, "67831213",
		 * "40be4e59b9a2a2b5dffb918c0e86b3d7", "5f6a7a33997557362fe77ecfcc267b8f");
		 * String result2 = changePasswordCore(token, "67831213",
		 * "5f6a7a33997557362fe77ecfcc267b8f", "5f6a7a33997557362fe77ecfcc267b8g");
		 */
		String result = updatePassword(token, "ABA_DABA", "40be4e59b9a2a2b5dffb918c0e86b3d7",
				"5f6a7a33997557362fe77ecfcc267b8f");
		System.out.println(result);

	}

	public String updatePassword(String token, String userName, String oldPass, String newPass) {
		SvSecurity svs = null;
		SvReader svr = null;
		String returnMsgLbl = null;
		try {
			svs = new SvSecurity();
			svr = new SvReader(token);
			DbDataObject dboUser = SvReader.getUserBySession(token);
			if (dboUser != null && dboUser.getObject_id() != 0) {
				svs.updatePassword(userName, oldPass, newPass);
				returnMsgLbl = "updatePassword.success";
			}
		} catch (Exception e) {
			if (e instanceof SvException) {
				returnMsgLbl = ((SvException) e).getFormattedMessage();
				log4j.info("Error in SvSSO.changePassword():" + ((SvException) e).getFormattedMessage(), e);
			} else {
				returnMsgLbl = e.toString();
				log4j.info("Error in SvSSO.changePassword():" + e.getMessage(), e);
			}
		} finally {
			if (svr != null)
				svr.release();
			if (svs != null)
				svs.release();
		}

		return returnMsgLbl;
	}

	public String changePasswordCore(String token, String pin, String old_pass, String new_pass) {
		SvReader svr = null;
		SvWriter svw = null;
		String returnMsgLbl = "error.changePassword";
		try {
			svr = new SvReader(token);
			svw = new SvWriter(svr);
			DbDataObject dboUser = SvReader.getUserBySession(token);
			if (dboUser != null && dboUser.getObject_id() != 0) {
				if (!dboUser.getVal("PIN").toString().equals(pin)) {
					returnMsgLbl = "changePassword.error.incorrectPin";
					// return
					// Response.status(200).entity(I18n.getText(returnMsgLbl)).build();
				}
				if (!dboUser.getVal("PASSWORD_HASH").toString()
						.equals(SvUtil.getMD5(old_pass.toUpperCase()).toUpperCase())) {
					returnMsgLbl = "changePassword.error.incorrectOldPass";
					// return
					// Response.status(200).entity(I18n.getText(returnMsgLbl)).build();
				}
				dboUser.setVal("PASSWORD_HASH", SvUtil.getMD5(new_pass.toUpperCase()));
				svw.saveObject(dboUser, false);
				svw.dbCommit();
				DbDataObject refreshCacheTriggerObj = svr.getObjectById(dboUser.getObject_id(),
						svCONST.OBJECT_TYPE_USER, null);
				returnMsgLbl = "changePassword.success";
				// passRecoveryTokens.remove(username);
			}
		} catch (Exception e) {
			if (e instanceof SvException) {
				returnMsgLbl = ((SvException) e).getFormattedMessage();
				log4j.info("Error in SvSSO.changePassword():" + ((SvException) e).getFormattedMessage(), e);
			} else {
				returnMsgLbl = e.toString();
				log4j.info("Error in SvSSO.changePassword():" + e.getMessage(), e);
			}
		} finally {
			if (svr != null)
				svr.release();
			if (svw != null)
				svw.release();
		}
		return returnMsgLbl;

	}

	@Test
	public void testChangeUserData() {
		String token = login("ADMIN", "welcome");
		SvReader svr = null;
		SvWriter svw = null;
		Writer wr = new Writer();
		try {
			svr = new SvReader(token);
			svw = new SvWriter(svr);
			DbDataObject dboUser = SvReader.getUserBySession(token);
			wr.editUserData("ADMIN_TEST", "TEST", dboUser, token);
			/*
			 * dboUser.setVal("FIRST_NAME", "ADMIN_TEST"); svw.saveObject(dboUser, false);
			 * dboUser = svr.getObjectById(dboUser.getObject_id(), svCONST.OBJECT_TYPE_USER,
			 * null); System.out.println(dboUser.getVal("FIRST_NAME")); String result =
			 * getBasicUserData(token, svr); System.out.println(result);
			 */
			svw.dbRollback();
		} catch (SvException e) {
			log4j.error("error:" + e.getFormattedMessage(), e);
		} finally {
			if (svr != null) {
				svr.release();
			}
			if (svw != null) {
				svw.release();
			}
		}
	}

	public String getBasicUserData(@PathParam("sessionid") String sessionid, SvReader svr) {
		String result = "";
		try {
			DbDataObject dboUser = null;
			dboUser = svr.getUserBySession(sessionid);
			if (dboUser == null) {
				result = "err.cannotGetBasicUserData.pleaseLoggoff";
				return result;
			}
			String userType = "";
			String userFirstName = "";
			String userLastName = "";
			String userEmail = "";
			String pin = "";

			if (dboUser.getVal("USER_TYPE") != null)
				userType = dboUser.getVal("USER_TYPE").toString();
			if (dboUser.getVal("FIRST_NAME") != null)
				userFirstName = dboUser.getVal("FIRST_NAME").toString();
			if (dboUser.getVal("LAST_NAME") != null)
				userLastName = dboUser.getVal("LAST_NAME").toString();
			if (dboUser.getVal("E_MAIL") != null)
				userEmail = dboUser.getVal("E_MAIL").toString();
			if (dboUser.getVal("PIN") != null)
				pin = dboUser.getVal("PIN").toString();

			result = "userObjId:" + dboUser.getObject_id().toString() + ";userType:" + userType + ";userFirstName:"
					+ userFirstName + ";userLastName:" + userLastName + ";userEmail:" + userEmail + ";pinNumber:" + pin;
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage(), e);

		}
		return result;
	}

	@Test
	public void testAddChangeUser() {
		DbDataObject dbo = new DbDataObject();
		String token = login("ADMIN", "welcome");
		SvReader svr = null;
		try {
			svr = new SvReader(token);
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage(), e);
			log4j.error(e.getConfigData(), e);
		}
	}

	public static String calc(String digStr) {
		int len = digStr.length();
		int sum = 0, rem = 0;
		int[] digArr = new int[len];
		for (int k = 1; k <= len; k++) // compute weighted sum
			sum += (11 - k) * Character.getNumericValue(digStr.charAt(k - 1));
		if ((rem = sum % 11) == 0)
			return "0";
		else if (rem == 1)
			return "X";
		return (new Integer(11 - rem)).toString();
	}

	@Test
	public void generateHic() {
		String token = login("ADMIN", "welcome");

		SvReader svr = null;
		SvSequence svs = null;
		String hic = null;
		try {
			svr = new SvReader(token);
			if (svr.getSessionId() != null) {
				svs = new SvSequence(svr.getSessionId());
			}
			DbDataObject dbo = svr.getObjectById(12436L, SvReader.getTypeIdByName("HOLDING"), null);
			DateTime tmpDsh = new DateTime(dbo.getVal("DT_CREATION"));
			log4j.info("success");

			// FORMAT: RRMMCCVVHHHHX
			// 2d for the region - RR
			String regionCode = "00";
			// 2d for the municipality -MM
			String municCode = "00";
			// 2d for the community - CC
			String communityCode = "00";
			// 2d for the village - VV
			String villageCode = "00";
			// 4d for the holding (sequence) - HHHH
			String holdingSeqCode = "";
			Long seqId = svs.getSeqNextVal(villageCode, false);
			holdingSeqCode = String.format("%04d", Integer.valueOf(seqId.toString()));
			// 1d check digit (ex.mod12) - X
			String checkDigit = calc(regionCode + municCode + villageCode + holdingSeqCode);
			System.out.println(checkDigit);
			System.out.println(
					"The calculated HIC:" + regionCode + municCode + villageCode + holdingSeqCode + checkDigit);

		} catch (Exception e) {
			e.printStackTrace();
			log4j.error(e);
		} finally {
			if (svr != null) {
				svr.release();
			}
			if (svs != null) {
				svs.release();
			}
		}

	}

	@Test
	public void searchForObject() {
		String token = login("ADMIN", "welcome");
		String result = null;
		SvReader svr = null;
		Reader r = null;
		DbDataObject animalExist = null;
		DbDataObject premiseExist = null;
		String searchStr = "10922118";
		if (searchStr == null || searchStr.trim().length() == 0) {
			log4j.error("The search string in globalSearchForObject is null or empty.");
		}
		r = new Reader();
		try {
			svr = new SvReader(token);
			String animalRegEx = "^[0-9]{8}$";
			String premiseRegEx = "^[0-9]{11}$";
			// System.out.println(searchStr.matches(animalRegEx));
			if (searchStr.matches(animalRegEx)) {
				animalExist = r.findAppropriateAnimalByAnimalId(searchStr, svr);
				result = animalExist.toJson().toString();
			}
			if (searchStr.matches(premiseRegEx)) {
				premiseExist = r.findAppropriateHoldingByPic(searchStr, svr);
				result = premiseExist.toJson().toString();
			}
			System.out.println(result);
		} catch (Exception e) {
			e.printStackTrace();
			log4j.error(e);
		} finally {
			if (svr != null) {
				svr.release();
			}
		}

	}

	@Test
	public void generateDropDown() {
		String token = login("ADMIN", "welcome");
		SvReader svr = null;
		Reader r = new Reader();
		ReactJsonBuilder cjb = new ReactJsonBuilder();
		try {
			svr = new SvReader(token);
			// Long codeListObjId = r.searchForObject("VILLAGES", "code_value",
			// "SVAROG_CODES", svr);
			// DbDataArray allCodeListChild =
			// r.getCodeListItemsByParent(codeListObjId, "1535", "en_US", svr);
			// if (allCodeListChild.size() > 0)
			// System.out.println(allCodeListChild.toJson());
			// String str = cjb.prepareReactJsonFormData("DROPDOWN",
			// allCodeListChild);
		} catch (Exception e) {
			e.printStackTrace();
			log4j.error(e);
		} finally {
			if (svr != null) {
				svr.release();
			}
		}

	}

	@Test
	public void testGetPermissions2() throws SvException {
		SvReader svr = null;
		SvWriter svw = null;
		String token = login("TEST_GRANT", "welcome");
		DbDataArray allowedCustomObjects = new DbDataArray();
		String result = null;

		try {
			// DbDataObject dboUser1 = svs.getUser("aba_daba");
			svr = new SvReader(token);
			svw = new SvWriter(svr);
			DbDataObject dboUser = SvReader.getUserBySession(token);
			svr = new SvReader(token);
			HashMap<SvCore.SvAclKey, HashMap<String, DbDataObject>> allPermissions = svr.getPermissions();
			Iterator it = allPermissions.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry pair = (Map.Entry) it.next();
				System.out.println(pair.getKey() + " = " + pair.getValue());
				SvCore.SvAclKey tempAllowedObject = (SvCore.SvAclKey) pair.getKey();
				Long tempObjId = tempAllowedObject.getObjectId();
				// System.out.println(tempObjId);
				// System.out.println(temp22);
				DbDataObject dboTable = svr.getObjectById(tempObjId, svCONST.OBJECT_TYPE_TABLE, null);
				/*
				 * if (dboTable != null && dboTable.getVal("TABLE_NAME") != null)
				 * System.out.println(dboTable.getVal("TABLE_NAME")); else
				 * System.out.println(tempObjId);
				 */
			}
			SvSecurity svs = new SvSecurity();
			DbDataObject sid = svs.getSid("TEST_GRANT", svCONST.OBJECT_TYPE_GROUP);
			DbDataArray permissions = svw.getPermissions(sid, svr);
			permissions.rebuildIndex("LABEL_CODE", true);
			for (DbDataObject dboPerm : permissions.getItems()) {
				System.out.println(dboPerm.getVal("LABEL_CODE"));
			}
			log4j.info(allPermissions.size());
		} catch (SvException e) {
			log4j.error("Error: ", e.getFormattedMessage());
			fail();
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
	}

	@Test
	public void testCreateLinkForKeeper() throws SvException {
		SvReader svr = null;
		SvLink svl = null;
		String token = login("k.stojanovski", "naits1234");
		Reader rdr = new Reader();
		try {
			svr = new SvReader(token);
			svl = new SvLink(svr);
			DbDataObject dblink = SvCore.getLinkType("HOLDING_KEEPER", SvReader.getTypeIdByName("HOLDING"),
					SvReader.getTypeIdByName("HOLDING_RESPONSIBLE"));

			// DbDataArray allHoldings =
			// svr.getObjectsByTypeId(SvReader.getTypeIdByName("HOLDING"), null,
			// 0, 0);
			DbSearchExpression getHoldings = new DbSearchExpression();
			DbSearchCriterion filterByIsNotNull = new DbSearchCriterion("TYPE", DbCompareOperand.ISNULL);
			getHoldings.addDbSearchItem(filterByIsNotNull);
			DbDataArray allHoldings = svr.getObjects(getHoldings, SvReader.getTypeIdByName("HOLDING"), null, 0, 0);
			int cnt = 0;
			for (DbDataObject dboHolding : allHoldings.getItems()) {
				if (dboHolding.getVal("OWNER_OBJ_ID") != null) {
					Long id = Long.valueOf(dboHolding.getVal("OWNER_OBJ_ID").toString());
					DbDataObject dboHoldingKeeper = svr.getObjectById(id,
							SvReader.getTypeIdByName("HOLDING_RESPONSIBLE"), null);
					svl.linkObjects(dboHolding.getObject_id(), dboHoldingKeeper.getObject_id(), dblink.getObject_id(),
							"", false, false);
					cnt++;
					if (cnt == 5000) {
						svl.dbCommit();
						cnt = 0;
					}
				}
			}

			svl.dbCommit();
		} catch (SvException e) {
			log4j.error("Error: ", e.getFormattedMessage());
			fail();
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
	}

	@Test
	public void testCreateVaccBookRecord() throws SvException {
		SvReader svr = null;
		SvWriter svw = null;
		String token = login("L.JANEV", "naits123");
		Writer wr = new Writer();
		try {
			svr = new SvReader(token);
			svw = new SvWriter(svr);
			DbDataObject dboAnimal = svr.getObjectById(85096L, SvReader.getTypeIdByName("ANIMAL"), null);
			DbDataObject dboVaccEvent = svr.getObjectById(223933L, SvReader.getTypeIdByName("VACCINATION_EVENT"), null);
			// wr.createVaccTreatmentRecord(dboAnimal, dboVaccEvent, svw);
			svw.dbCommit();
		} catch (SvException e) {
			log4j.error("Error: ", e.getFormattedMessage());
			fail();
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
	}

	@Test
	public void testAnimalMovement() throws SvException {
		SvReader svr = null;
		SvWriter svw = null;
		SvWorkflow sww = null;
		String token = login("L.JANEV", "naits123");
		Writer wr = new Writer();
		Reader rdr = new Reader();
		try {
			svr = new SvReader(token);
			svw = new SvWriter(svr);
			sww = new SvWorkflow(svw);
			DbDataObject dboAnimal = svr.getObjectById(85096L, SvReader.getTypeIdByName("ANIMAL"), null);
			DbDataObject dboHolding = svr.getObjectById(58425L, SvReader.getTypeIdByName("HOLDING"), null);
			if (!rdr.checkIfAnimalOrFlockMovementExist(dboAnimal, dboHolding, svr)) {
				wr.startAnimalOrFlockMovement(dboAnimal, dboHolding, "TRANSFER", null, null, null, null, null, null,
						null, null, svr, svw, sww);
			} else {
				// wr.finishAnimalOrFlockMovement(dboAnimal, dboHolding, null,
				// svw, sww);
			}

			svw.dbCommit();
		} catch (SvException e) {
			log4j.error("Error: ", e.getFormattedMessage());
			fail();
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
	}

	@Test
	public void testGetHoldingKeeper() throws SvException {
		SvReader svr = null;
		SvWriter svw = null;
		SvWorkflow sww = null;
		String token = login("L.JANEV", "naits123");
		Writer wr = new Writer();
		Reader rdr = new Reader();
		try {
			svr = new SvReader(token);
			svw = new SvWriter(svr);
			sww = new SvWorkflow(svw);
			rdr.getHoldingKeeperInfo(58331L, svr);
		} catch (SvException e) {
			log4j.error("Error: ", e.getFormattedMessage());
			fail();
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
	}

	@Test
	public void testgetPicPerHolding() throws SvException {
		SvReader svr = null;
		SvWriter svw = null;
		SvWorkflow sww = null;
		String token = login("L.JANEV", "naits123");
		Writer wr = new Writer();
		Reader rdr = new Reader();
		try {
			svr = new SvReader(token);
			svw = new SvWriter(svr);
			sww = new SvWorkflow(svw);
			DbDataObject dboHolding = svr.getObjectById(58331L, SvReader.getTypeIdByName("HOLDING"), null);
			if (dboHolding != null && dboHolding.getVal("PIC") != null) {
				// result = dboHolding.getVal("PIC").toString();
				String result = dboHolding.toSimpleJson().toString();
				log4j.info(result);
			}
		} catch (SvException e) {
			log4j.error("Error: ", e.getFormattedMessage());
			fail();
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
	}

	@Test
	public void testUpdateKeeperNameIfNeeded() throws SvException {
		SvReader svr = null;
		String token = login("L.JANEV", "naits123");
		Writer wr = new Writer();
		try {
			svr = new SvReader(token);
			svr.setIncludeGeometries(true);
			Long holdingObjId = 58331L;
			Long holderObjId = 38589L;
			// wr.updateKeeperNameIfNeeded(holdingObjId, holderObjId, svr);
		} catch (SvException e) {
			log4j.error("Error: ", e.getFormattedMessage());
			fail();
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
	}

	@Test
	public void testFlocIDGeneration() throws SvException {
		SvReader svr = null;
		String token = login("L.JANEV", "naits123");
		Writer wr = new Writer();
		try {
			svr = new SvReader(token);
			svr.setIncludeGeometries(true);
			Long flockObjId = 223219L;
			DbDataObject dboFlock = svr.getObjectById(flockObjId, SvReader.getTypeIdByName("FLOCK"), null);
			wr.generateFicPerFlock(dboFlock, svr);
		} catch (SvException e) {
			log4j.error("Error: ", e.getFormattedMessage());
			fail();
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
	}

	@Test
	public void createVillageObject() throws SvException {
		SvReader svr = null;
		SvWriter svw = null;
		String token = login("L.JANEV", "naits123");
		try {
			svr = new SvReader(token);
			svw = new SvWriter(svr);
			DbDataArray newVillagesToSave = new DbDataArray();
			DbSearchExpression getAllVillages = new DbSearchExpression();
			DbSearchCriterion filterByLabel = new DbSearchCriterion("PARENT_CODE_VALUE", DbCompareOperand.EQUAL,
					"VILLAGES");
			getAllVillages.addDbSearchItem(filterByLabel);
			DbDataArray allVillages = svr.getObjects(getAllVillages, svCONST.OBJECT_TYPE_CODE, null, 0, 0);
			for (DbDataObject tempCodeValue : allVillages.getItems()) {
				DbDataObject dboVillage = new DbDataObject();
				dboVillage.setObject_type(SvReader.getTypeIdByName("VILLAGE"));
				if (tempCodeValue.getVal("CODE_VALUE").toString().length() < 8) {
					log4j.info(tempCodeValue.getVal("CODE_VALUE").toString());
				}
				/*
				 * dboVillage.setVal("REGION_CODE",
				 * tempCodeValue.getVal("CODE_VALUE").toString().substring(0, 2));
				 * dboVillage.setVal("MUNIC_CODE",
				 * tempCodeValue.getVal("CODE_VALUE").toString().substring(0, 4));
				 * dboVillage.setVal("COMMUN_CODE",
				 * tempCodeValue.getVal("CODE_VALUE").toString().substring(0, 6));
				 * dboVillage.setVal("VILLAGE_CODE",
				 * tempCodeValue.getVal("CODE_VALUE").toString().substring(0, 8));
				 * newVillagesToSave.addDataItem(dboVillage);
				 */
			}
			// svw.saveObject(newVillagesToSave);

		} catch (SvException e) {
			log4j.error("Error: ", e.getFormattedMessage());
			fail();
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
	}

	@Test
	public void testAnimalAgeCalculaton() {
		SvReader svr = null;
		String token = login("L.JANEV", "naits123");
		Reader r;
		try {
			svr = new SvReader(token);
			r = new Reader();
			// 84982L
			String age = r.calcAnimalAge(170664L, svr);
		} catch (SvException e) {
			log4j.error("Error: ", e.getFormattedMessage());
			fail();
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
	}

	@Test
	public void testTransferAnimalDirect() throws SvException {
		SvReader svr = null;
		SvWriter svw = null;
		SvWorkflow sww = null;
		String token = login("L.JANEV", "naits123");
		Writer wr = new Writer();
		Reader rdr = new Reader();
		try {
			svr = new SvReader(token);
			svw = new SvWriter(svr);
			sww = new SvWorkflow(svw);
			DbDataObject dboAnimal = svr.getObjectById(85096L, SvReader.getTypeIdByName("ANIMAL"), null);
			DbDataObject dboHolding = svr.getObjectById(58425L, SvReader.getTypeIdByName("HOLDING"), null);
			if (!rdr.checkIfAnimalOrFlockMovementExist(dboAnimal, dboHolding, svr)) {
				wr.startAnimalOrFlockMovement(dboAnimal, dboHolding, "TRANSFER", null, null, null, null, null, null,
						null, null, svr, svw, sww);
			} else {
				// wr.finishAnimalOrFlockMovement(dboAnimal, dboHolding, null,
				// svw, sww);
			}

			svw.dbCommit();
		} catch (SvException e) {
			log4j.error("Error: ", e.getFormattedMessage());
			fail();
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
	}

	@Test
	public void testUpdateDeathDate() throws SvException {
		SvReader svr = null;
		SvWriter svw = null;
		String token = login("L.JANEV", "naits123");
		Writer wr = new Writer();
		Reader rdr = new Reader();
		try {
			svr = new SvReader(token);
			svw = new SvWriter(svr);
			DbDataObject dboAnimal = svr.getObjectById(85096L, SvReader.getTypeIdByName("ANIMAL"), null);

			Calendar calendar = Calendar.getInstance();
			java.sql.Date dtNow = new java.sql.Date(calendar.getTime().getTime());

			dboAnimal.setVal("DEATH_DATE", dtNow);
			svw.saveObject(dboAnimal);
			svw.dbCommit();

		} catch (SvException e) {
			log4j.error("Error: ", e.getFormattedMessage());
			fail();
		} finally {
			if (svr != null) {
				svr.release();
			}
			if (svw != null) {
				svw.release();
			}
		}
	}

	@Test
	public void testGetNextHolding() throws SvException {
		SvReader svr = null;
		SvWriter svw = null;
		String token = login("L.JANEV", "naits123");
		Writer wr = new Writer();
		Reader rdr = new Reader();
		try {
			svr = new SvReader(token);
			svw = new SvWriter(svr);
			DbDataObject dboHolding = svr.getObjectById(229816L, SvReader.getTypeIdByName("HOLDING"), null);
			DbDataObject nextHolding = null;
			String name = dboHolding.getVal("NAME").toString();
			String pic = dboHolding.getVal("PIC").toString();
			String picFirstValue = "20006";
			int charValue = picFirstValue.charAt(0);
			String next = String.valueOf((char) (charValue + 1));
			System.out.println(next);
			DbDataArray allHoldings = svr.getObjectsByTypeId(SvReader.getTypeIdByName("HOLDING"), null, 0, 0);
			// allHoldings.getSortedItems("PIC");
			allHoldings.getSortedItems("NAME");
			Boolean getNext = false;
			for (DbDataObject tempHolding : allHoldings.getItems()) {
				if (getNext) {
					nextHolding = tempHolding;
					break;
				}
				if (tempHolding.getObject_id().equals(dboHolding.getObject_id())) {
					log4j.info(tempHolding.getVal("NAME").toString());
					getNext = true;
				}
			}
			if (nextHolding != null) {
				log4j.info("Next holding" + nextHolding.getVal("NAME").toString());
			}
			// test this
			String nextHoldingPic = rdr.getNextOrPreviousHoldingPic(229816L, "FORWARD", svr);
			String previousHoldingPic = rdr.getNextOrPreviousHoldingPic(229816L, "BACKWARD", svr);
			log4j.info("NEXT:" + nextHoldingPic);
			log4j.info("PREV:" + previousHoldingPic);
		} catch (SvException e) {
			log4j.error("Error: ", e.getFormattedMessage());
			fail();
		} finally {
			if (svr != null) {
				svr.release();
			}
			if (svw != null) {
				svw.release();
			}
		}
	}

	@Test
	public void testLinkProblem2() throws SvException {
		SvReader svr = null;
		String token = login("L.JANEV", "naits123");
		Writer wr = new Writer();
		Reader rdr = new Reader();
		try {
			svr = new SvReader(token);
			DbDataObject dbLink = svr.getLinkType("ANIMAL_MOVEMENT_HOLDING", SvReader.getTypeIdByName("HOLDING"),
					SvReader.getTypeIdByName("ANIMAL_MOVEMENT"));
			DbDataArray vData = svr.getObjectsByLinkedId(58351L, SvReader.getTypeIdByName("HOLDING"),
					"ANIMAL_MOVEMENT_HOLDING", SvReader.getTypeIdByName("ANIMAL_MOVEMENT"), false, null, 0, 0);
			DbDataArray vData2 = svr.getObjectsByLinkedId(58351L, SvReader.getTypeIdByName("HOLDING"),
					"ANIMAL_MOVEMENT_HOLDING", SvReader.getTypeIdByName("ANIMAL_MOVEMENT"), true, null, 0, 0, "VALID");
			log4j.info("vb");
			DbDataObject dboFlock = svr.getObjectById(223219L, SvReader.getTypeIdByName("FLOCK"), null);
			String str = dboFlock.toSimpleJson().toString();
			log4j.info(str);
		} catch (SvException e) {
			log4j.error("Error: ", e.getFormattedMessage());
			fail();
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
	}

	@Test
	public void testTypelessJsonObject() throws SvException {
		SvReader svr = null;
		String token = login("L.JANEV", "naits123");
		Writer wr = new Writer();
		Reader rdr = new Reader();
		try {
			svr = new SvReader(token);
			DbDataObject dboFlock = new DbDataObject();
			dboFlock.setVal("animals.cnt.label", "23");
			dboFlock.setVal("person.cnt.label", "30");
			String str = dboFlock.toSimpleJson().toString();
			log4j.info(str);
			JSONObject jsonObj = new JSONObject();
			jsonObj.put("animals.cnt.label", "23");
			jsonObj.put("person.cnt.label", "30");
			log4j.info(jsonObj.toJSONString());

			// 64367L
			// 58351L
			Long objId = 64367L;
			DbDataObject dboHolding = svr.getObjectById(64367L, SvReader.getTypeIdByName("HOLDING"), null);
			JSONObject jsonObj2 = new JSONObject();
			LinkedHashMap<String, String> jsonOrderedMap = new LinkedHashMap<String, String>();
			jsonOrderedMap.put("keeper.full_name", rdr.getHoldingKeeperInfo(58351L, svr));
			jsonOrderedMap.put("animals.cnt.bovine", rdr.getAnimalNumberPerHolding("ANIMAL", "1", objId, svr));
			jsonOrderedMap.put("animals.cnt.ovine", rdr.getAnimalNumberPerHolding("ANIMAL", "2", objId, svr));
			jsonOrderedMap.put("animals.cnt.caprine", rdr.getAnimalNumberPerHolding("ANIMAL", "3", objId, svr));
			jsonOrderedMap.put("naits.flock.cnt.ovine", rdr.getAnimalNumberPerHolding("FLOCK", "1", objId, svr));
			jsonOrderedMap.put("naits.flock.cnt.caprine", rdr.getAnimalNumberPerHolding("FLOCK", "2", objId, svr));
			jsonOrderedMap.put("naits.flock.cnt.porcine", rdr.getAnimalNumberPerHolding("FLOCK", "3", objId, svr));
			jsonOrderedMap.put("naits.holding.pendingMovements",
					String.valueOf(rdr.getNoOfPendingMovements(dboHolding.getVal("PIC").toString(), svr)));
			jsonObj2.put("orderedItems", jsonOrderedMap);
			String result = jsonObj2.toJSONString();
			log4j.info(result);

		} catch (

		SvException e) {
			log4j.error("Error: ", e.getFormattedMessage());
			fail();
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
	}

	@Test
	public void testRegExp() throws SvException {

		String name = "fish,2";
		if (Pattern.matches("[a-zA-Z0-9]*", name)) {
			log4j.info(true);
		} else {
			log4j.info(false);
		}

		// String date = "2018-09-09T22:00:00.000Z";
		String date = "2018-9-29";
		// String substr_date = date.substring(0, 10);
		String pattern = Tc.DATE_PATTERN;
		DateTime convertedDate = DateTime.parse(date, DateTimeFormat.forPattern(pattern));
		log4j.info(convertedDate);

	}

	@Test
	public void testAttachUserGoupPermission() throws SvException {
		String packageName = "READ";
		String token = login("A.ADMIN3", "naits123");
		SvReader svr = null;
		SvWriter svw = null;
		SvSecurity svs = null;
		try {
			svr = new SvReader(token);
			svw = new SvWriter(svr);
			svs = new SvSecurity(svw);
			svs.setAutoCommit(false);
			svw.setAutoCommit(false);
			UserManager userM = new UserManager();

			DbDataObject groupObj = svr.getObjectById(29072L, svCONST.OBJECT_TYPE_GROUP, null);
			ArrayList<String> coreTablePermissions = userM.getSvarogCoreTablePermission(svr);
			ArrayList<String> customTablePermissions = new ArrayList<String>();
			switch (packageName) {
			case "READ":
				customTablePermissions = userM.getSvarogCustomTablePermission("READ", svr);
				// userM.attachPermissionsToUserGroup(groupObj,
				// coreTablePermissions, customTablePermissions, token);
				break;
			case "FULL":
				customTablePermissions = userM.getSvarogCustomTablePermission("FULL", svr);
				// userM.attachPermissionsToUserGroup(groupObj,
				// coreTablePermissions, customTablePermissions, token);
				break;
			default:
				break;
			}

		} catch (SvException e) {
			log4j.error("Error occured in getNextOrPreviousHolding:" + e.getFormattedMessage(), e);
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
	}

	@Test
	public void testAttachUserGoupPermission2() throws SvException {
		String packageName = "READ";
		String token = login("A.ADMIN3", "naits123");
		SvReader svr = null;
		SvWriter svw = null;
		SvSecurity svs = null;
		try {
			svr = new SvReader(token);
			svw = new SvWriter(svr);
			svs = new SvSecurity(svw);
			svs.setAutoCommit(false);
			svw.setAutoCommit(false);
			UserManager userM = new UserManager();

			DbDataObject groupObj = svr.getObjectById(29071L, svCONST.OBJECT_TYPE_GROUP, null);
			ArrayList<String> coreTablePermissions = userM.getSvarogCoreTablePermission(svr);
			ArrayList<String> customTablePermissions = new ArrayList<String>();
			switch (packageName) {
			case "READ":
				customTablePermissions = userM.getSvarogCustomTablePermission("READ", svr);
				// userM.attachPermissionsToUserGroup(groupObj,
				// coreTablePermissions, customTablePermissions, token);
				break;
			case "FULL":
				customTablePermissions = userM.getSvarogCustomTablePermission("FULL", svr);
				// userM.attachPermissionsToUserGroup(groupObj,
				// coreTablePermissions, customTablePermissions, token);
				break;
			default:
				break;
			}
		} catch (SvException e) {
			log4j.error("Error occured in getNextOrPreviousHolding:" + e.getFormattedMessage(), e);
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
	}

	@Test
	public void updateGeomObjectTest() {
		Long holdingObjId = 54165L;
		String token = login("L.JANEV", "naits123");
		SvReader svr = null;
		SvGeometry svg = null;
		try {
			svr = new SvReader(token);
			svg = new SvGeometry(token);
			svg.setAllowNullGeometry(true);
			svg.setAutoCommit(false);
			svr.setIncludeGeometries(true);

			DbDataObject dboHolding = svr.getObjectById(holdingObjId, SvReader.getTypeIdByName("HOLDING"), null);
			dboHolding.setVal("PHYSICAL_ADDRESS", "123");
			svg.saveGeometry(dboHolding);

		} catch (SvException e) {
			log4j.error("Error occured in getNextOrPreviousHolding:" + e.getFormattedMessage(), e);
		} finally {
			if (svr != null) {
				svr.release();
			}
			if (svg != null) {
				svg.release();
			}
		}
	}

	@Test
	public void animalValidationSetChecksTest() {
		SvReader svr = null;
		SvWriter svw = null;
		String token = null;
		Reader rdr = new Reader();
		try {
			token = login("A.ADMIN3", "naits1234");
			svr = new SvReader(token);
			svw = new SvWriter(svr);
			svw.dbSetAutoCommit(false);

			DbDataObject dboAnimal = rdr.findAppropriateAnimalByAnimalId("55533301", svr);
			dboAnimal.setVal(Tc.FATHER_TAG_ID, "55533300");
			svw.saveObject(dboAnimal, false);

			if (dboAnimal != null) {
				ValidationChecks validator = new ValidationChecks();

				if (!validator.checkIfAnimalFatherGenderIsValid(dboAnimal, svr))
					throw (new SvException("system.beforeSaveCheck_animal_invalidFatherGender", svCONST.systemUser,
							null, null));

				if (!validator.checkIfPeriodBetweenNewbornsIsAppropriate(dboAnimal, svr))
					throw (new SvException("system.beforeSaveCheck_animal_canNotBeSelfParent", svCONST.systemUser, null,
							null));

				if (!validator.checkIfAnimalIdIsNotSelfParent(dboAnimal, svr))
					throw (new SvException("system.beforeSaveCheck_animal_canNotBeSelfParent", svCONST.systemUser, null,
							null));

				/*
				 * if (validator.checkIfAnimalsMotherAndFatherHaveSameAnimalId( dboAnimal, svr))
				 * throw (new SvException(
				 * "system.beforeSaveCheck_animal_parentCanNotBeTheSame", svCONST.systemUser,
				 * null, null));
				 */

				if (!validator.checkIfAnimalMotherGenderIsValid(dboAnimal, svr))
					throw (new SvException("system.beforeSaveCheck_animal_invalidMotherGender", svCONST.systemUser,
							null, null));

				if (!validator.checkIfAnimalRegDateIsNotAfterAnimalBirthDate(dboAnimal, svr))
					throw (new SvException("system.beforeSaveCheck_animal_regDateCanNotBeBeforeBirthDate",
							svCONST.systemUser, null, null));

				if (!validator.checkIfAnimalDeathDateIsNotBeforeBirthDate(dboAnimal, svr))
					throw (new SvException("system.beforeSaveCheck_animal_deathDateCanNotBeBeforeBirthDate",
							svCONST.systemUser, null, null));

				if (!validator.checkIfAnimalMotherAgeIsAppropriate(dboAnimal, svr))
					throw (new SvException("system.beforeSaveCheck_animal_motherAgeIsUnderAge", svCONST.systemUser,
							null, null));
			}

		} catch (SvException e) {
			log4j.error("Error occured in getNextOrPreviousHolding:" + e.getFormattedMessage(), e);
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
	}

	@Test
	public void testUserPermission() {
		Long holdingObjId = 54165L;
		String token = login("A.ADMIN3", "naits123");
		String token2 = login("L.JANEV", "naits123");
		SvReader svr = null;
		SvWriter svw = null;
		SvSecurity svs = null;
		try {
			svr = new SvReader(token);
			svw = new SvWriter(svr);
			svs = new SvSecurity(svw);
			DbDataObject dboUserJan = SvReader.getUserBySession(token2);
			UserManager userM = new UserManager();
			ArrayList<String> currentPermissions = userM
					.getCurrentUserPermissions(dboUserJan.getVal("USER_NAME").toString(), svr);
			log4j.info(currentPermissions);
			ArrayList<String> coreTablePermissions = userM.getSvarogCoreTablePermission(svr);
			ArrayList<String> customTablePermissions = userM.getSvarogCustomTablePermission("FULL", svr);
			userM.attachPermissionsToUser(dboUserJan.getVal("USER_NAME").toString(), customTablePermissions, svr, svw);
			svw.dbCommit();
			currentPermissions = userM.getCurrentUserPermissions(dboUserJan.getVal("USER_NAME").toString(), svr);
			log4j.info(currentPermissions);
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
		} finally {

		}
	}

	@Test
	public void testGetUserOrGroupPermissions() {
		Long holdingObjId = 54165L;
		String token = login("A.ADMIN3", "naits1234");
		String token2 = login("A.ASUS", "naits1234");
		SvReader svr = null;
		SvWriter svw = null;
		SvSecurity svs = null;
		try {
			svr = new SvReader(token);
			svw = new SvWriter(svr);
			svs = new SvSecurity(svw);
			DbDataObject dboUserAdmin = SvReader.getUserBySession(token);
			DbDataObject dboUserJan = SvReader.getUserBySession(token2);
			UserManager userM = new UserManager();
			Reader rdr = new Reader();
			ArrayList<String> currentPermissions = userM.getCustomPermissionForUserOrGroup(dboUserAdmin, svr);
			// log4j.info(currentPermissions);
			currentPermissions = userM.getCustomPermissionForUserOrGroup(dboUserJan, svr);
			log4j.info(currentPermissions);
			// LinkedHashMap<String, String> testMap =
			// rdr.generateUserOrGroupSummaryInformation(dboUserJan, svr);
			JSONObject jsonObj = new JSONObject();
			// jsonObj.put("orderedItems", testMap);
			String result = jsonObj.toJSONString();
			log4j.info(result);

			DbDataObject dboUserGroup = svr.getObjectById(29071L, svCONST.OBJECT_TYPE_GROUP, null);
			// LinkedHashMap<String, String> testMap2 =
			// rdr.generateUserOrGroupSummaryInformation(dboUserGroup, svr);
			JSONObject jsonObj2 = new JSONObject();
			// jsonObj.put("orderedItems", testMap2);
			String result2 = jsonObj.toJSONString();
			log4j.info(result2);
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
		} finally {

		}
	}

	@Test
	public void testGetAnimalObjectHistory() {
		SvReader svr = null;
		String token = login("L.JANEV", "naits123");
		String animalTagId = "0790864";
		Reader r = new Reader();
		try {
			svr = new SvReader(token);
			// DbDataArray animalsFound =
			// r.searchByAnimalIdWithHistory(animalTagId, svr);
			// log4j.info(animalsFound.toSimpleJson());
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
	}

	@Test
	public void testGetUserGroups() {
		SvReader svr = null;
		String token = login("ADMIN", "welcome");
		Long userObjId = 2301947L;
		Reader r = new Reader();
		try {
			svr = new SvReader(token);
			DbDataObject dboUser = svr.getObjectById(userObjId, svCONST.OBJECT_TYPE_USER, null);
			DbDataArray allDefaultGroups = svr.getAllUserGroups(dboUser, true);
			DbDataArray allAdditionalGroups = svr.getAllUserGroups(dboUser, false);
			log4j.info("ok");
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
	}

	@Test
	public void testDepdendencyDroprownVilage() {
		SvReader svr = null;
		String token = login("ADMIN", "welcome");
		Reader r = new Reader();
		try {
			svr = new SvReader(token);
			DbDataArray results1 = r.searchForDependentMunicCommunVillage("15", "MUNICIPALITIES", svr);
			log4j.info(results1.toJson());
			if (results1.size() != 6) {
				Assert.fail();
			}
			DbDataArray results2 = r.searchForDependentMunicCommunVillage("1523", "COMMUNITIES", svr);
			log4j.info(results2.toSimpleJson());
			if (results2.size() != 11) {
				Assert.fail();
			}
			DbDataArray results3 = r.searchForDependentMunicCommunVillage("152344", "VILLAGES", svr);
			log4j.info(results3.toSimpleJson());
			if (results3.size() != 4) {
				Assert.fail();
			}
			log4j.info("12".length());

		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
	}

	/*
	 * @Test public void testUserMassActions() { SvReader svr = null; SvWriter svw =
	 * null; SvWorkflow sww = null; SvNote svn = null; String token = null; Long
	 * userObjId = 29069L; MassActions massAct = null; Writer wr = null; try { token
	 * = login("ADMIN", "welcome"); svr = new SvReader(token); svw = new
	 * SvWriter(svr); sww = new SvWorkflow(svw); svn = new SvNote(sww); massAct =
	 * new MassActions(); wr = new Writer(); svw.setAutoCommit(false);
	 * sww.setAutoCommit(false); svn.setAutoCommit(false); DbDataObject dboUser =
	 * svr.getObjectById(userObjId, svCONST.OBJECT_TYPE_USER, null);
	 * massAct.userMassAction(dboUser, "RESET_PASS", null, null, svw, sww, svn, wr);
	 * massAct.userMassAction(dboUser, "CHANGE_STATUS", "SUSPENDED", "bad player",
	 * svw, sww, svn, wr); String suspensionNote =
	 * svn.getNote(dboUser.getObject_id(), "SUSPENSION_NOTE");
	 * massAct.userMassAction(dboUser, "CHANGE_STATUS", "INACTIVE", null, svw, sww,
	 * svn, wr); massAct.userMassAction(dboUser, "CHANGE_STATUS", "VALID", null,
	 * svw, sww, svn, wr); svn.dbRollback(); svw.dbRollback(); sww.dbRollback();
	 * log4j.info("ok"); } catch (SvException e) {
	 * log4j.error(e.getFormattedMessage()); } finally { if (svr != null) {
	 * svr.release(); } } }
	 */

	@Test
	public void testDateFormat() throws ParseException {
		String dt_from = "10.21.18";
		DateFormat formatter = new SimpleDateFormat("MM/dd/yy");
		Date dt_from_parsed = formatter.parse(dt_from.replace(".", "/"));
		log4j.info(dt_from_parsed);
	}

	@Test
	public void testNewAnimalMovement() throws SvException {
		SvReader svr = null;
		SvWriter svw = null;
		String token = null;
		Writer wrt = null;
		ValidationChecks vc = null;
		Reader rdr = null;
		try {
			token = login("ADMIN", "welcome");
			svr = new SvReader(token);
			svw = new SvWriter(svr);
			wrt = new Writer();
			rdr = new Reader();
			vc = new ValidationChecks();
			OnSaveValidations osv = new OnSaveValidations();
			svw.dbSetAutoCommit(false);

			// setiran parentId vo metodata
			// svw.saveObject(animalMovement, false);

			// vc.checkIfDepartureDateIsNotBeforeLastArrivalDate(animalMovement,
			// svr);

			DbDataObject animMovementTest = svr.getObjectById(1206153L, svr.getTypeIdByName("ANIMAL_MOVEMENT"), null);

			// rdr.getAllSortedAnimalMovements(animMovementTest,svr);
			// test for dateConverter()

			// osv.animalMovementOnSaveValidationSet(animMovementTest, svr);

		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
			fail();
		} finally {
			if (svr != null) {
				svr.release();
			}
			if (svw != null) {
				svw.release();
			}
		}
	}

	@Test
	public void testNumberOfAnimalsPerHolding() throws SvException {
		SvReader svr = null;
		String token = null;
		Reader rdr = null;
		try {
			token = login("ADMIN", "welcome");
			svr = new SvReader(token);
			rdr = new Reader();

			String rez = new String();
			// works fine
			// rez = rdr.getAnimalNumberPerHolding("FLOCK", "2", 50145L, svr);
			// log4j.debug(rez);

			// test for flocks
			// DbDataArray flocks =
			// svr.getObjectsByTypeId(svr.getTypeIdByName("FLOCK"), null, 0, 0);
			// Integer numberOfAnimalsInFlock = new
			// Integer(rdr.getTotalNoOfHeads(flocks));
			// log4j.debug(numberOfAnimalsInFlock);

			// rdr instance of this service can be additionally completed,
			// depending on the client requirements
			String result = "";
			String tableName = "HOLDING";
			DbDataObject dboObject = svr.getObjectById(564388L, SvReader.getTypeIdByName(tableName), null);
			JSONObject jsonObj = new JSONObject();
			LinkedHashMap<String, String> jsonOrderedMap = new LinkedHashMap<String, String>();
			if (dboObject != null) {
				jsonOrderedMap = rdr.generateHoldingSummaryInformation(dboObject, svr);
				jsonObj.put("orderedItems", jsonOrderedMap);
				result = jsonObj.toJSONString();// smeneto vo reader sve sto e
												// 2, smeneto vo 9. Raboti ok.
			}
			log4j.debug(result);

		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
			fail();
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
	}

	@Test
	public void testForAnimalDiseases() {
		SvReader svr = null;
		String token = null;
		Reader rdr = null;
		ValidationChecks vc = null;
		try {
			token = login("ADMIN", "welcome");
			svr = new SvReader(token);
			rdr = new Reader();
			vc = new ValidationChecks();
			DbDataObject animalObj = svr.getObjectById(1226956L, SvReader.getTypeIdByName("ANIMAL"), null);
			ArrayList pom = rdr.getInformationAboutAnimalHealthStatus(animalObj, svr);
			log4j.debug(pom);
			if (pom == null) {
				Assert.fail("AnimalDisease failed. Null pointer");
			}
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
			fail();
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
	}

	@Test
	public void randomArrayElementsTest() {
		ArrayList<Long> longElements = null;
		ArrayList<Long> randomLongElements = null;
		Random rnd = null;
		Long[] lel = { 123L, 13456L, 13416L, 13426L, 13436L, 13446L, 134556L, 134536L, 1343256L, 1345116L };
		try {

			longElements = new ArrayList<>();
			randomLongElements = new ArrayList<>();
			rnd = new Random();
			for (int i = 1; i <= lel.length; i++) {
				longElements.add(lel[i]);
			}
			for (int i = 0; i < longElements.size(); i++) {
				int index = rnd.nextInt(longElements.size() + 1);
				if (randomLongElements.contains(longElements.get(index))) {
					longElements.remove(index);
					randomLongElements.add(longElements.get(index));
				}
			}

			log4j.debug(randomLongElements + "    ");

			log4j.debug(longElements);
		} catch (Exception e) {
			log4j.error(e.getMessage());
			fail();
		}
	}

	@Test
	public void testHashMap() {
		SvReader svr = null;
		String token = null;
		ValidationChecks vc = null;
		Reader rdr = null;
		try {
			token = login("ADMIN", "welcome");
			svr = new SvReader(token);
			vc = new ValidationChecks();
			rdr = new Reader();
			/*
			 * DbDataObject anim = svr.getObjectById(584745L,
			 * SvReader.getTypeIdByName("ANIMAL"), null);
			 * 
			 * DbDataArray array = rdr.getAnimalSiblingsAccordingMotherTagId(anim, svr);
			 * DbDataArray result = rdr.generateRandom(array, 5, svr); for(DbDataObject
			 * animObj: result.getItems()) { log4j.debug(animObj.getObject_id()); //TEST FOR
			 * RANDOM ARRAY WITH N SIZE }
			 */

			LinkedHashMap<String, String> pom = new LinkedHashMap<>();
			pom.put("ANIMAL_ID", "1261433");
			pom.put("ANIMAL_CLASS", "1");
			DbDataArray arr = rdr.getDbDataWithCriteria(pom, 28569L, svr);
			log4j.debug(arr.size());

		} catch (Exception e) {
			log4j.error(e.getMessage());
			fail();
		}
	}

	/*
	 * @Test public void testForPopulation() { SvReader svr = null; SvWriter svw =
	 * null; String token = null; Reader rdr = null; Writer wrt = null; try { token
	 * = login("ADMIN", "welcome"); svr = new SvReader(token); svw = new
	 * SvWriter(svr); rdr = new Reader(); wrt = new Writer();
	 * 
	 * DbDataObject populationObjVaccHolding = wrt.createPopulation("1", "HOLDING");
	 * svw.saveObject(populationObjVaccHolding, false);
	 * 
	 * DbDataObject criterionTypeAnimalGenderObj =
	 * wrt.createCriteriaType("crit.holding.animal_gender", "GENDER", "EQUAL", "1",
	 * "NVARCHAR"); svw.saveObject(criterionTypeAnimalGenderObj,false);
	 * 
	 * DbDataObject criterionTypeAnimalAgeObj =
	 * wrt.createCriteriaType("crit.holding.animal_age", "BIRTH_DATE", "LESS_EQUAL",
	 * "0", "DATETIME"); svw.saveObject(criterionTypeAnimalAgeObj, false);
	 * 
	 * DbDataObject criterionObjAnimalGender =
	 * wrt.createCritera(criterionTypeAnimalGenderObj.getObject_id(), "1",
	 * populationObjVaccHolding.getObject_id());
	 * svw.saveObject(criterionObjAnimalGender, false );
	 * 
	 * DbDataObject criterionObjHoldingRegion =
	 * wrt.createCritera(criterionTypeAnimalAgeObj.getObject_id(), "20",
	 * populationObjVaccHolding.getObject_id());
	 * svw.saveObject(criterionObjHoldingRegion, false);
	 * 
	 * //svw.dbCommit();
	 * 
	 * 
	 * 
	 * } catch (Exception e) { log4j.error(e.getMessage()); fail(); } }
	 */
	@Test
	public void testForCreatingPopulation() {
		// COMMITED ROWS INTO NAITS FOR KIKAC
		SvReader svr = null;
		SvWriter svw = null;
		String token = null;
		Writer wrt = null;
		try {
			token = login("ADMIN", "welcome");
			svr = new SvReader(token);
			svw = new SvWriter(svr);
			wrt = new Writer();
			svw.setAutoCommit(false);

			// DbDataObject populationObjDefHolding =
			// wrt.createPopulation("2","HOLDING");
			// svw.saveObject(populationObjDefHolding, false);

			// DbDataObject criteriaObjForHold = wrt.createCriteria(1279044L,
			// "2");
			// svw.saveObject(criteriaObjForHold, false);

			// DbDataObject criteriaObjForHold2 = wrt.createCriteria(1279045L,
			// "30");
			// svw.saveObject(criteriaObjForHold2, false);

			// svw.dbCommit();
			// svw.saveObject(populationObjVaccHolding, false);

		} catch (Exception e) {
			log4j.error(e.getMessage());
			fail();
		} finally {
			if (svr != null) {
				svr.release();
			}
			if (svw != null) {
				svw.release();
			}
		}
	}

	@Test
	public void testLinkAreaPopulation() {
		SvReader svr = null;
		SvWriter svw = null;
		SvLink svl = null;
		String token = null;
		Reader rdr = null;
		Writer wrt = null;
		try {
			token = login("ADMIN", "welcome");
			svr = new SvReader(token);
			svw = new SvWriter(svr);
			svl = new SvLink(svw);
			wrt = new Writer();
			svw.setAutoCommit(false);

			DbDataObject popObj = svr.getObjectById(1921054L, SvReader.getTypeIdByName("POPULATION"), null);
			DbDataObject areaVillageObj = svr.getObjectById(1279153L, SvReader.getTypeIdByName("AREA"), null);

			svl.linkObjects(areaVillageObj, popObj, "AREA_POPULATION", "", true, false);
			svl.dbCommit();

			/*
			 * DbDataObject linkAreaPop = SvLink.getLinkType("AREA_POPULATION",
			 * areaVillageObj.getObject_type(), popObj.getObject_type()); DbDataArray
			 * allItems = svr.getObjectsByLinkedId(areaVillageObj.getObject_id(),
			 * areaVillageObj.getObject_type(), linkAreaPop, popObj.getObject_type(), false,
			 * null, 0, 0); DbDataArray rez = allItems;
			 */

		} catch (Exception e) {
			log4j.error(e.getMessage());
			fail();
		} finally {
			if (svr != null) {
				svr.release();
			}
			if (svw != null) {
				svw.release();
			}
		}
	}

	@Test
	public void testAnimalStatusBeforPostMortemSave() {
		SvReader svr = null;
		SvWriter svw = null;
		String token = null;
		ValidationChecks vc = null;
		OnSaveValidations ovc = null;
		Writer wrt = null;
		try {
			token = login("ADMIN", "welcome");
			svr = new SvReader(token);
			svw = new SvWriter(svr);
			vc = new ValidationChecks();
			wrt = new Writer();
			ovc = new OnSaveValidations();
			svw.setAutoCommit(false);

			DbDataObject animalObj = svr.getObjectById(77920L, SvReader.getTypeIdByName("ANIMAL"), null);

			DbDataObject postSlToFail = new DbDataObject();
			postSlToFail.setObject_type(SvReader.getTypeIdByName("POST_SLAUGHT_FORM"));
			postSlToFail.setParent_id(animalObj.getObject_id());

			// if (!ovc.checkIfAnimalStatusIsSlaught(postSlToFail, svr)) {
			// log4j.info("Animal has status other than slaught");
			// } else {
			// fail();
			// }
			svw.saveObject(postSlToFail, false);
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
	}

	/*
	 * @Test public void testHoldingHealthStatusByAniamls() { SvReader svr = null;
	 * String token = null; ValidationChecks vc = null; try { token = login("ADMIN",
	 * "welcome"); svr = new SvReader(token); vc = new ValidationChecks();
	 * DbDataObject animalMovementObj = svr.getObjectById(1201004L,
	 * SvReader.getTypeIdByName("ANIMAL_MOVEMENT"), null); if
	 * (vc.checkIfDestinationHoldingHasPositiveHealthStatus(animalMovementObj, svr))
	 * { log4j.debug("true"); } else {
	 * Assert.fail("Animal destination holding is infected"); }
	 * 
	 * if (vc.checkIfSourceHoldingHasPositiveHealthStatus(animalMovementObj, svr)) {
	 * log4j.debug("true"); } else {
	 * Assert.fail("Animal source holding is infected"); } } catch (SvException e) {
	 * log4j.error(e.getFormattedMessage()); fail(); } finally { if (svr != null) {
	 * svr.release(); } } }
	 */

	/*
	 * @Test public void testAnimalVaccRecAfterMovement() { SvReader svr = null;
	 * String token = null; ValidationChecks vc = null; try { token = login("ADMIN",
	 * "welcome"); svr = new SvReader(token); vc = new ValidationChecks();
	 * DbDataObject animalObj = svr.getObjectById(1200125L,
	 * SvReader.getTypeIdByName("ANIMAL_MOVEMENT"), null); if
	 * (vc.checkIfAnimalHaveVaccRecordAfterMovementRequest(animalObj, svr)) {
	 * log4j.debug("true"); } else {
	 * Assert.fail("Animal is not vaccinated after movement"); } } catch
	 * (SvException e) { log4j.error(e.getFormattedMessage()); fail(); } finally {
	 * if (svr != null) { svr.release(); } } }
	 */

	/*
	 * @Test public void testAreaHealthStatus() { SvReader svr = null; SvWriter svw
	 * = null; String token = null; Reader rdr = null; Writer wrt = null; // 540752
	 * try { token = login("ADMIN", "welcome"); svr = new SvReader(token); svw = new
	 * SvWriter(svr); rdr = new Reader(); wrt = new Writer(); DbDataObject holding =
	 * svr.getObjectById(1200087L, SvReader.getTypeIdByName("HOLDING"), null);
	 * DbDataObject animalObj = wrt.createAnimalObject("1", "1", null, "2018-12-06",
	 * null, null, "1", "17", "10000019", null, "2", "GE", holding.getObject_id());
	 * svw.saveObject(animalObj, false);
	 * 
	 * String pom = rdr.getAreaHealthStatus("29326031",12, svr); log4j.debug(pom);
	 * if (pom != "Healthy") { Assert.fail("Area health status is negative"); } else
	 * { log4j.debug("Area is healthy!"); } } catch (SvException e) {
	 * log4j.error(e.getFormattedMessage()); fail(); } finally { if (svr != null) {
	 * svr.release(); } } }
	 */

	@Test
	public void testAreaHealthStatus() {
		SvReader svr = null;
		SvWriter svw = null;
		String token = null;
		ValidationChecks vc = null;
		OnSaveValidations ovc = null;
		Writer wrt = null;
		try {
			token = login("ADMIN", "welcome");
			svr = new SvReader(token);
			svw = new SvWriter(svr);
			vc = new ValidationChecks();
			wrt = new Writer();
			ovc = new OnSaveValidations();
			svw.setAutoCommit(false);

			/*
			 * DbDataObject areaHealthObjToFail = wrt.createAreaHealth("BRUC", "2", "Perun",
			 * 1279293L); if (!vc.checkIfAreaHasDuplicateDisease(areaHealthObjToFail, svr))
			 * { log4j.info("Area does not have duplicate disease"); } else { fail(); }
			 * svw.saveObject(areaHealthObjToFail, false);
			 */
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
	}

	@Test
	public void testAnimalAgeBug() {
		SvReader svr = null;
		String token = null;
		Reader rdr = new Reader();
		// 540752
		try {
			token = login("ADMIN", "welcome");
			svr = new SvReader(token);
			String pattern = Tc.DATE_PATTERN;
			DbDataObject dboAnimal = svr.getObjectById(1204851L, 28569L, null);
			String result = "";
			result = rdr.calcAnimalAge(dboAnimal.getObject_id(), svr);
			log4j.debug(result);
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
			fail();
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
	}

	@Test
	public void testGetHoldingKeeperByLink() {
		SvReader svr = null;
		String token = null;
		Reader rdr = null;
		// 540752
		try {
			token = login("ADMIN", "welcome");
			svr = new SvReader(token);
			rdr = new Reader();

			DbDataObject holding = rdr.getHoldingOwner(501375L, svr);
			log4j.debug(holding.toJson());

		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
			fail();
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
	}

	@Test
	public void testBigDecimal() {
		SvReader svr = null;
		String token = null;
		// 540752
		try {
			token = login("ADMIN", "welcome");
			svr = new SvReader(token);
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
			fail();
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
	}

	@Test
	public void testGetAnimalsOrHoldingsBySelectionResult() {
		SvReader svr = null;
		String token = null;
		Reader rdr = new Reader();
		// 540752
		try {
			token = login("A.ADMIN3", "naits1234");
			svr = new SvReader(token);
			DbDataObject dboPop = svr.getObjectById(2177737L, SvReader.getTypeIdByName("POPULATION"), null);
			log4j.debug(dboPop);
			DbDataArray result = rdr.getAnimalsOrHoldingsBySelectionResult(dboPop, svr);
			log4j.debug(result.toJson());

		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
			fail();
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
	}

	@Test
	public void testEndDateBeforeStartDate() {
		SvReader svr = null;
		String token = null;
		Reader rdr = new Reader();
		ValidationChecks vc = new ValidationChecks();
		// 540752
		try {
			token = login("A.ADMIN3", "naits1234");
			svr = new SvReader(token);
			/*
			 * String pattern = Tc.DATE_PATTERN; DbDataObject dboQuarantine =
			 * svr.getObjectById(2303348L, SvReader.getTypeIdByName("QUARANTINE"),null);
			 * dboQuarantine.setObject_id(0L);
			 * vc.quarantineOnSaveValidationSet(dboQuarantine,svr);
			 */

			// 2302798
			// 1248320 animal

			DbDataObject animalDbo = svr.getObjectById(1248320L, SvReader.getTypeIdByName("ANIMAL"), null);

			DbSearchCriterion sc1 = new DbSearchCriterion("ANIMAL_CLASS", DbCompareOperand.EQUAL,
					animalDbo.getVal("ANIMAL_CLASS").toString());
			DbSearchCriterion sc2 = new DbSearchCriterion("ANIMAL_RACE", DbCompareOperand.EQUAL,
					animalDbo.getVal("ANIMAL_RACE").toString());
			DbSearchExpression dbse = new DbSearchExpression();
			dbse.addDbSearchItem(sc1).addDbSearchItem(sc2);

			DbDataArray ar = svr.getObjects(dbse, SvReader.getTypeIdByName("ANIMAL_TYPE"), null, 0, 0);

			if (vc.checkIfAnimalHasNotAppropriateClassOrRace(animalDbo, svr)) {
				log4j.debug("error");
			} else {
				log4j.debug("okej");
			}

			log4j.debug(ar.toJson());

		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
			fail();
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
	}

	@Test
	public void testSlaughterStatusPreMortem() {
		SvReader svr = null;
		String token = null;
		Reader rdr = new Reader();
		ValidationChecks vc = new ValidationChecks();
		// 540752
		try {
			token = login("A.ADMIN3", "naits1234");
			svr = new SvReader(token);
			DbDataObject dboPostSlaugh = svr.getObjectById(2302798L, SvReader.getTypeIdByName("PRE_SLAUGHT_FORM"),
					null);
			if (vc.checkIfAnimalOrFlockHaveStatusSlaught(dboPostSlaugh, svr)) {
				log4j.debug("ERROR");
			} else {
				log4j.debug("OK");
			}
			// 2302798

		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
			fail();
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
	}

	@Test
	public void testLinkAnimalExport() {
		SvReader svr = null;
		String token = null;
		Reader rdr = new Reader();
		ValidationChecks vc = new ValidationChecks();
		// 540752
		try {
			token = login("A.ADMIN3", "naits1234");
			svr = new SvReader(token);
			DbDataObject bipObj = svr.getObjectById(2448661L, SvReader.getTypeIdByName("HOLDING"), null);

			Boolean b = vc.checkIfHoldingIsBorderInspectionPoint(bipObj);

			log4j.info(b);
			// 2303348 quarantine
			// 1921232

		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
			fail();
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
	}

	@Test
	public void testQuarantineTypeEdit() {
		SvReader svr = null;
		String token = null;
		SvWriter svw = null;
		Reader rdr = new Reader();
		ValidationChecks vc = new ValidationChecks();
		// 540752
		try {
			token = login("A.ADMIN3", "naits1234");
			svr = new SvReader(token);
			svw = new SvWriter(svr);
			svw.setAutoCommit(false);
			DbDataObject exportCertObj = svr.getObjectById(2448533L, SvReader.getTypeIdByName("EXPORT_CERT"), null);
			DbDataObject animalObj = svr.getObjectById(2448639L, SvReader.getTypeIdByName("ANIMAL"), null);
			DbDataObject dbLink = SvLink.getLinkType("ANIMAL_EXPORT_CERT", animalObj.getObject_type(),
					exportCertObj.getObject_type());
			DbDataArray ar = svr.getObjectsByLinkedId(animalObj.getObject_id(), animalObj.getObject_type(), dbLink,
					exportCertObj.getObject_type(), false, null, 0, 0);

			if (ar.size() > 0) {
				throw (new SvException("naits.error.testErrorException", svCONST.systemUser, null, null));
			}

			log4j.debug(ar.toJson());

		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
			fail();
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
	}

	@Test
	public void testForTestingTests() throws Exception {
		// 78428 animal obj_id
		SvReader svr = null;
		SvWriter svw = null;
		String token = null;
		Reader rdr = new Reader();
		MassActions ma = new MassActions();
		ValidationChecks vc = new ValidationChecks();
		Writer wr = new Writer();
		// 540752
		try {
			token = login("A.ADMIN3", "naits1234");
			svr = new SvReader(token);
			svw = new SvWriter(svr);
			svw.setAutoCommit(false);
			DbDataObject dboAnimal = svr.getObjectById(171450L, SvReader.getTypeIdByName("ANIMAL"), null);
			DbDataObject linkAnimExport = rdr.getLinkBetweenAnimalAndExportCert(dboAnimal, svr);
			if (dboAnimal.getStatus().equals("PENDING_EX") && linkAnimExport != null) {
				wr.invalidateLink(linkAnimExport, svr);
			}
			log4j.debug(linkAnimExport.toJson());

		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
			fail();
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
	}

	@Test
	public void testForTestForTestingTests() throws Exception {
		// 78428 animal obj_id
		SvReader svr = null;
		SvWriter svw = null;
		String token = null;
		Reader rdr = new Reader();
		MassActions ma = new MassActions();
		ValidationChecks vc = new ValidationChecks();
		Writer wr = new Writer();
		// 540752
		try {
			token = login("A.ADMIN3", "naits1234");
			svr = new SvReader(token);
			svw = new SvWriter(svr);
			svw.setAutoCommit(false);
			DbDataObject getLink = svr.getLinkType("ANIMAL_EXPORT_CERT", SvReader.getTypeIdByName("ANIMAL"),
					SvReader.getTypeIdByName("EXPORT_CERT"));
			DbDataObject dboAnimal = svr.getObjectById(86155L, SvReader.getTypeIdByName("ANIMAL"), null);
			DbDataObject linkAnimExport = rdr.getLinkBetweenAnimalAndExportCert(dboAnimal, svr);
			log4j.debug(linkAnimExport);
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
			fail();
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
	}

	@Test
	public void testForChangingStatusToCerifiedAnimals() throws Exception {
		// 78428 animal obj_id
		SvReader svr = null;
		SvWriter svw = null;
		String token = null;
		Reader rdr = new Reader();
		MassActions ma = new MassActions();
		// 540752
		try {
			OnSaveValidations onSave = new OnSaveValidations();
			SvCore.registerOnSaveCallback(onSave, SvReader.getTypeIdByName(Tc.MOVEMENT_DOC));
			token = login("A.ADMIN3", "naits1234");
			svr = new SvReader(token);
			svw = new SvWriter(svr);
			svw.setAutoCommit(false);

			// DbDataObject dboTransfer = svr.getObjectById(21622977L,
			// SvReader.getTypeIdByName(Tc.ANIMAL_MOVEMENT), null);

			DbDataObject dboAnimal = svr.getObjectById(21627904L, SvReader.getTypeIdByName(Tc.ANIMAL_MOVEMENT), null);

			JsonArray jArr = new JsonArray();

			jArr.add(dboAnimal.toSimpleJson());
			// jArr.add(dboTransfer2.toSimpleJson());

			/*
			 * String pom = ma.animalFlockMassHandler("ANIMAL_MOVEMENT", "MOVE",
			 * Tc.FINISH_MOVEMENT, "73059", new DateTime().toString().substring(0, 9), null,
			 * null, null, null, null, null, null, null, jArr, svr.getSessionId());
			 */
			// String res = ma.exportCertifiedAnimals(1921235L, svr);//
			// EXPORT_CERT
			// log4j.debug(pom);

		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
			fail();
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
	}

	@Test
	public void testForMethodInSvConverter() {
		// 78428 animal obj_id
		SvReader svr = null;
		SvWriter svw = null;
		String token = null;
		Reader rdr = new Reader();
		MassActions ma = new MassActions();
		// 540752
		try {
			token = login("A.ADMIN3", "naits1234");
			svr = new SvReader(token);
			svw = new SvWriter(svr);
			DbDataArray db = new DbDataArray();
			DbDataArray diseases = svr.getObjectsByTypeId(SvReader.getTypeIdByName("DISEASE"), null, 0, 0);
			for (DbDataObject dboDis : diseases.getItems()) {
				// DbDataObject ar = rdr.getDiseases(dboDis, svr);
				// if(ar.getVal("CODE_VALUE").toString()getClass().equals())
			}

		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
			fail();
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
	}

	@Test
	public void testCalidationForMotherPregnancy() {
		SvReader svr = null;
		String token = null;
		SvWriter svw = null;
		ValidationChecks vc = new ValidationChecks();
		// 540752
		try {
			token = login("A.ADMIN3", "naits1234");
			svr = new SvReader(token);
			svw = new SvWriter(svr);
			svw.setAutoCommit(false);
			DbDataObject currAnimal = new DbDataObject();
			currAnimal.setObject_type(SvReader.getTypeIdByName("ANIMAL"));
			currAnimal.setVal("MOTHER_TAG_ID", "0192352");
			currAnimal.setVal("ANIMAL_ID", "22002200");
			currAnimal.setVal("BIRTH_DATE", "2015-05-05");
			currAnimal.setVal("REGISTRATION_DATE", "2015-06-05");
			currAnimal.setVal("COUNTRY", "GE");
			currAnimal.setVal("ANIMAL_CLASS", "1");
			currAnimal.setVal("ANIMAL_RACE", "1013");
			currAnimal.setVal("GENDER", "1");
			currAnimal.setVal("COLOR", "1");
			if (vc.checkIfPeriodBetweenNewbornsIsAppropriate(currAnimal, svr)) {
				svw.saveObject(currAnimal, false);
				log4j.debug("OK E");
			} else {
				log4j.debug("NE E");
			}

		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
			fail();
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
	}

	@Test
	public void testCalidationForAnimalTwin() {
		SvReader svr = null;
		String token = null;
		SvWriter svw = null;
		ValidationChecks vc = new ValidationChecks();
		// 540752
		try {
			token = login("A.ADMIN3", "naits1234");
			svr = new SvReader(token);
			svw = new SvWriter(svr);
			svw.setAutoCommit(false);
			DbDataObject currAnimal = new DbDataObject();
			currAnimal.setObject_type(SvReader.getTypeIdByName("ANIMAL"));
			currAnimal.setVal("MOTHER_TAG_ID", "5454546666");
			currAnimal.setVal("ANIMAL_ID", "99920205");
			currAnimal.setVal("BIRTH_DATE", "2019-03-05");
			currAnimal.setVal("REGISTRATION_DATE", "2019-03-05");
			currAnimal.setVal("COUNTRY", "GE");
			currAnimal.setVal("ANIMAL_CLASS", "2");
			currAnimal.setVal("ANIMAL_RACE", "2");
			currAnimal.setVal("GENDER", "1");
			currAnimal.setVal("COLOR", "1");
			if (vc.checkIfTwinAnimalHasAppropriateFather(currAnimal, svr)) {
				svw.saveObject(currAnimal, false);
				log4j.debug("OK E");
				svw.dbCommit();
			} else {
				log4j.debug("NE E");
			}
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
			fail();
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
	}

	@Test
	public void testPostMortemDiseaseValidation() {
		SvReader svr = null;
		String token = null;
		SvWriter svw = null;
		Reader rdr = new Reader();
		ValidationChecks vc = new ValidationChecks();
		// 540752
		try {
			token = login("A.ADMIN3", "naits1234");
			svr = new SvReader(token);
			svw = new SvWriter(svr);
			svw.setAutoCommit(false);

			ArrayList<String> tempList = new ArrayList<>();
			DbDataObject anim = rdr.findAppropriateAnimalByAnimalId("1281777181", svr);
			DbDataObject preToTest = new DbDataObject();
			preToTest.setObject_type(SvReader.getTypeIdByName(Tc.PRE_SLAUGHT_FORM));
			preToTest.setVal("CLINICAL_EXAM", "CLINIC_SUSPICIOUS");
			preToTest.setVal("DECISION", "1");
			preToTest.setVal("DISEASE", "21,22");
			preToTest.setVal("TEST_FIELD", true);
			if (preToTest.getVal("TEST_FIELD").equals(false)) {
				log4j.debug("ok");
			}

			preToTest.setParent_id(anim.getObject_id());
			svw.saveObject(preToTest, false);
			DbDataArray bla = rdr.getAppropriateDiseasesByAnimal(anim, svr);
			for (DbDataObject temp : bla.getItems()) {
				tempList.add(temp.getVal(Tc.DISEASE_NAME).toString());
			}
			log4j.debug(tempList.toString());
			if (vc.checkIfAnimalHasAppropriateDiseaseInPreOrPostSlaughtForm(preToTest, preToTest, svr)) {
				log4j.debug("Multiselected diseases are appropriate for this animal");
			} else {
				log4j.debug("Not appropriate selected disease");
			}

		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
			fail();
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
	}

	@Test
	public void testForNotificationToAllAdmins() {
		SvReader svr = null;
		String token = null;
		SvWriter svw = null;
		Reader rdr = new Reader();
		Writer wr = new Writer();
		UserManager um = new UserManager();
		ValidationChecks vc = new ValidationChecks();
		// 540752
		try {
			token = login("A.ADMIN3", "naits1234");
			svr = new SvReader(token);
			svw = new SvWriter(svr);
			svw.setAutoCommit(false);

			// METHOD

			DbDataObject dboAnimal = rdr.findAppropriateAnimalByAnimalId("32323234", svr);
			log4j.debug("LONG" + dboAnimal.getObject_id());
			log4j.debug("INT" + dboAnimal.getObject_id().intValue());

			/*
			 * DbDataArray animalSibilings = null; DbDataObject twinChild = null; if
			 * (vc.checkIfAnimalIsTwin(dboAnimal, svr)) { animalSibilings =
			 * rdr.getAnimalSiblingsAccordingMotherTagId(dboAnimal, svr); twinChild =
			 * animalSibilings.get(0); wr.notifyUserGroupByGroupName("ADMINISTRATORS",
			 * "SYS_GEN", "Twins registration detected.", "Animal with ear tag ID: " +
			 * dboAnimal.getVal("ANIMAL_ID").toString() +
			 * " is twin with animal with ear tag ID: " +
			 * twinChild.getVal("ANIMAL_ID").toString(), "", null, svr); }
			 */

		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
			fail();
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
	}

	@Test
	public void testSandraForAnimalMovement() {
		SvReader svr = null;
		String token = null;
		SvWriter svw = null;
		Reader rdr = new Reader();
		Writer wr = new Writer();
		ValidationChecks vc = new ValidationChecks();
		// 540752
		try {
			token = login("L.JANEV", "naits1234");
			svr = new SvReader(token);
			svw = new SvWriter(svr);
			svw.setAutoCommit(false);

			// METHOD
			DbDataObject dboAnimal = rdr.findAppropriateAnimalByAnimalId("10123652", svr);
			DbDataObject animalMvm = new DbDataObject();
			animalMvm.setObject_type(SvReader.getTypeIdByName("ANIMAL_MOVEMENT"));
			animalMvm.setVal("MOVEMENT_REASON", "DIRECT_TRANSFER");
			animalMvm.setVal("SOURCE_HOLDING_ID", "13121500102");
			animalMvm.setVal("DESTINATION_HOLDING_ID", "13172800016");
			animalMvm.setVal("ANIMAL_EAR_TAG", "8523697");
			animalMvm.setVal("MOVEMENT_TYPE", "INDIVIDUAL");
			animalMvm.setVal("DEPARTURE_DATE", "2019-03-12");
			animalMvm.setVal("ARRIVAL_DATE", "2019-04-12");
			animalMvm.setParent_id(dboAnimal.getObject_id());
			if (!vc.checkIfDestinationHoldingHasPositiveHealthStatus(animalMvm, svr)) {
				log4j.debug("OK");
			} else {
				log4j.debug("BAD");
			}
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
			fail();
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
	}

	@Test
	public void testForCreatingSpotCheckObject() {
		SvReader svr = null;
		SvWriter svw = null;
		String token = null;
		Writer wr = new Writer();
		Reader rdr = new Reader();
		ValidationChecks vc = new ValidationChecks();
		// 540752
		try {
			token = login("A.ADMIN3", "naits1234");
			svr = new SvReader(token);
			svw = new SvWriter(svr);
			svw.setAutoCommit(false);
			// DbDataObject dboSpotCheck = svr.getObjectById(2512555L,
			// SvReader.getTypeIdByName("SPOT_CHECK"), null);
			// DbDataObject dboSpotCheck = svr.getObjectById(2512556L,
			// SvReader.getTypeIdByName("SPOT_CHECK"), null);
			DbDataObject parentHolding = rdr.findAppropriateHoldingByPic("2932413703182", svr);
			DbDataObject dbo = new DbDataObject();
			dbo.setObject_type(SvReader.getTypeIdByName("SPOT_CHECK"));
			dbo.setVal("CHECK_SUBJECT", "2");
			dbo.setVal("NUM_TAGS", 7);
			dbo.setVal("MISSING_TAGS", 5);
			dbo.setVal("IS_AVAILABLE", "1");
			dbo.setVal("IS_COMPLETED", "2");
			dbo.setVal("NOTE", "Test should pass");
			dbo.setParent_id(parentHolding.getObject_id());
			// DbDataObject anim = svr.getObjectById(91925L,
			// SvReader.getTypeIdByName(Tc.ANIMAL), null);
			if (dbo != null && dbo.getVal("DEATH_DATE") == null) {
				wr.setAutoDate(dbo, Tc.DATE_OF_REG);
			}
			if (!vc.checkIfSameCheckSubjectIsShowingMoreThanOnceOnSameDateInHolding(dbo, svr)) {
				svw.saveObject(dbo, false);
			}
			log4j.debug("OK");
			svw.dbCommit();

		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
	}

	@Test
	public void testCheckSpotDateComaprasion() {
		SvReader svr = null;
		String token = null;
		Writer wr = new Writer();
		Reader rdr = new Reader();
		ValidationChecks vc = new ValidationChecks();
		// 540752
		try {
			String pattern = Tc.DATE_PATTERN;
			DateTime ld = new DateTime("2019-03-15");
			token = login("A.ADMIN3", "naits1234");
			svr = new SvReader(token);
			DbDataObject labSampleObj = svr.getObjectById(3129731L, SvReader.getTypeIdByName(Tc.LAB_SAMPLE), null);
			Integer pom = rdr.getPeriodOfMonthsBetweenParameterDateAndDateNow(labSampleObj, Tc.DATE_OF_COLLECTION);

			log4j.debug(pom);

			DateTime dateNow = new DateTime();
			DateTime testDate = new DateTime("2019-07-08");
			Period diff = new Period(testDate, dateNow);
			pom = diff.getYears() * 12 + diff.getMonths();

			log4j.debug(pom);

		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
			fail();
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
	}

	@Test
	public void testForSettingDateTimeInDb() {
		SvReader svr = null;
		String token = null;
		// 540752
		try {
			token = login("ADMIN", "welcome");
			svr = new SvReader(token);
			String pattern = Tc.DATE_PATTERN;
			DateTime ld = new DateTime("2017-10-21");
			DateTime now = new DateTime();
			DateTime convertedCurrentDate = DateTime.parse(now.toString().substring(0, 10),
					DateTimeFormat.forPattern(pattern));
			log4j.debug(convertedCurrentDate.toString().substring(0, 10));

			Calendar calendar = Calendar.getInstance();
			java.sql.Date dtNow = new java.sql.Date(calendar.getTime().getTime());
			DateTime dt_dt = new DateTime();
			log4j.debug("LONG OF COSTUM DATE" + dt_dt);
			log4j.debug("LONG OF TODAYS DATE" + dtNow.toString());

		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
			fail();
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
	}

	@Test
	public void testAnimalAutoDeath() {
		SvReader svr = null;
		String token = null;
		SvWriter svw = null;
		SvWorkflow sww = null;
		Writer wr = new Writer();
		ValidationChecks vc = new ValidationChecks();
		// 540752
		try {
			Reader rdr = new Reader();
			token = login("A.ADMIN3", "naits1234");
			svr = new SvReader(token);
			svw = new SvWriter(svr);
			sww = new SvWorkflow(svr);
			svw.setAutoCommit(false);
			sww.setAutoCommit(false);
			DbDataObject currAnimal = rdr.findAppropriateAnimalByAnimalId("22330568", svr);
			wr.changeStatus(currAnimal, Tc.SLAUGHTRD, sww);
			if (currAnimal.getVal("DEATH_DATE") == null) {
				// wr.setAutoDateWithSave(currAnimal, "DEATH_DATE", svw);
				// currAnimal = svr.getObjectById(currAnimal.getObject_id(),
				// SvReader.getTypeIdByName(Tc.ANIMAL), null);
				// svw.saveObject(currAnimal);2
			} else {
				log4j.debug("It has death_date");
			}
			log4j.debug("Success");
			// DbDataObject userObj =
			// SvReader.getUserBySession(svr.getSessionId());
			// log4j.debug(userObj.getVal("USER_NAME").toString());
			svw.dbCommit();
			sww.dbCommit();
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
			fail();
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
	}

	@Test
	public void testCreatingFlock() {
		SvReader svr = null;
		String token = null;
		SvWriter svw = null;
		SvWorkflow sww = null;
		Writer wr = new Writer();
		ValidationChecks vc = new ValidationChecks();
		// 540752
		try {
			Reader rdr = new Reader();
			token = login("A.ADMIN3", "naits1234");
			svr = new SvReader(token);
			svw = new SvWriter(svr);
			sww = new SvWorkflow(svr);
			svw.setAutoCommit(false);
			sww.setAutoCommit(false);
			DbDataObject dbo = new DbDataObject();
			dbo.setObject_type(SvReader.getTypeIdByName(Tc.FLOCK));
			dbo.setParent_id(532684L);
			if (dbo.getVal(Tc.FLOCK_ID) == null) {
				wr.generateFicPerFlock(dbo, svr);
			}
			wr.calculateSumForFlock(dbo, svr);
			if (!vc.validateFlockEwesNumber(dbo, svr)) {
				throw (new SvException("naits.error.NumberOfEwesCanNotBeBiggerThenNumberOfFemales", svCONST.systemUser,
						null, null));
			}
			wr.setFlockElementsToZero(dbo, "MALES", true);
			wr.setFlockElementsToZero(dbo, "FEMALES", true);
			svw.dbCommit();
			// sww.dbCommit();
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
			fail();
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
	}

	@Test
	public void testAnimalValidationInSlaughterHouse() {
		SvReader svr = null;
		String token = null;
		SvWriter svw = null;
		ValidationChecks vc = new ValidationChecks();
		// 540752
		try {
			Reader rdr = new Reader();
			token = login("A.ADMIN3", "naits1234");
			svr = new SvReader(token);
			svw = new SvWriter(svr);
			svw.setAutoCommit(false);
			DbDataObject currAnimal = new DbDataObject();
			currAnimal.setObject_type(SvReader.getTypeIdByName("ANIMAL"));
			DbDataObject dboHolding = rdr.findAppropriateHoldingByPic("2932413703182", svr);
			currAnimal.setVal("MOTHER_TAG_ID", "5454546666");
			currAnimal.setVal("ANIMAL_ID", "99920205");
			currAnimal.setVal("BIRTH_DATE", "2019-03-05");
			currAnimal.setVal("REGISTRATION_DATE", "2019-03-05");
			currAnimal.setVal("COUNTRY", "GE");
			// currAnimal.setVal("COUNTRY_OLD_ID", "KA");
			currAnimal.setVal("ANIMAL_CLASS", "2");
			currAnimal.setVal("ANIMAL_RACE", "2");
			currAnimal.setVal("GENDER", "1");
			currAnimal.setVal("COLOR", "1");
			currAnimal.setParent_id(dboHolding.getObject_id());// Slaughter
																// house
			if (vc.checkIfAnimalCanBeSavedToSlaughterHouseByOrigin(currAnimal)) {
				svw.saveObject(currAnimal, false);
				log4j.debug("OK E");
				svw.dbCommit();
			} else {
				log4j.debug("NE E");
			}
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
			fail();
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
	}

	@Test
	public void testAnimalPreMortemForm() {
		SvReader svr = null;
		String token = null;
		Reader rdr = new Reader();
		SvWriter svw = null;
		ValidationChecks vc = new ValidationChecks();
		// 540752
		try {
			token = login("A.ADMIN3", "naits1234");
			svr = new SvReader(token);
			svw = new SvWriter(svr);
			svw.setAutoCommit(false);
			DbDataObject animalObj = rdr.findAppropriateAnimalByAnimalId("10912316", svr);
			// DbDataObject prePost = svr.getObjectById(1936592L,
			// SvReader.getTypeIdByName("POST_SLAUGHT"), null);
			/*
			 * DbDataObject preToTest = new DbDataObject();
			 * preToTest.setObject_type(SvReader.getTypeIdByName(Tc. PRE_SLAUGHT_FORM));
			 * preToTest.setVal("CLINICAL_EXAM", "CLINIC_SUSPICIOUS");
			 * preToTest.setVal("DECISION", "1"); preToTest.setVal("DISEASE", "1234");
			 * preToTest.setParent_id(animalObj.getObject_id()); svw.saveObject(preToTest,
			 * false);
			 */

			DbDataObject preSl = svr.getObjectById(1936593L, SvReader.getTypeIdByName(Tc.PRE_SLAUGHT_FORM), null);
			Boolean result = vc.checkIfAnimalHasAppropriateDiseaseInPreOrPostSlaughtForm(preSl, preSl, svr);
			if (result.equals(true)) {
				log4j.debug("OK");
				svw.dbCommit();
			} else {
				log4j.debug("PROBLEM");
			}

		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
			fail();
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
	}

	@Test
	public void testAnimalSummaryInfo() {
		SvReader svr = null;
		String token = null;
		Reader rdr = new Reader();
		ValidationChecks vc = new ValidationChecks();
		// 540752
		try {
			token = login("A.ADMIN3", "naits1234");
			svr = new SvReader(token);
			JSONObject jsonObj = new JSONObject();
			LinkedHashMap<String, String> jsonOrderedMap = new LinkedHashMap<String, String>();
			// DbDataObject animalObj =
			// rdr.findAppropriateAnimalByAnimalId("10076123", svr);
			DbDataObject holdingObj = svr.getObjectById(1269556L, SvReader.getTypeIdByName("HOLDING"), null);
			// jsonObj.put("orderedItems", testMap2);

			ArrayList<DbDataObject> ar = new ArrayList<>();
			JsonObject jo = new JsonObject();
			for (Entry<SvCharId, Object> e : holdingObj.getValuesMap().entrySet()) {
				if (e.getKey() != null && e.getValue() != null)
					jo.addProperty(e.getKey().toString(), e.getValue().toString());
			}
			String pic = jo.get("PIC").getAsString();
			jsonObj.put("orderedItems", jo);
			log4j.debug(jsonObj);
			log4j.debug(holdingObj.toSimpleJson());
			log4j.debug(pic);
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
			fail();
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
	}

	@Test
	public void testPendingMovements() {
		SvReader svr = null;
		String token = null;
		Reader rdr = new Reader();
		ValidationChecks vc = new ValidationChecks();
		// 540752
		try {
			token = login("A.ADMIN3", "naits1234");
			svr = new SvReader(token);

			DbDataArray pom = rdr.getPendingAnimalMovementsPerHolding("29324133182", svr);
			log4j.debug(pom.size());

			System.err.println();
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
			fail();
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
	}

	@Test
	public void testDirectTr() {
		// 540752
		String resultMsgLabel = "naits.success.transfer";
		SvReader svr = null;
		SvWriter svw = null;
		SvWorkflow sww = null;
		String token = null;
		ValidationChecks vc = null;
		Reader rdr = new Reader();
		try {
			token = login("A.ADMIN3", "naits1234");
			svr = new SvReader(token);
			svw = new SvWriter(svr);
			sww = new SvWorkflow(svw);
			Reader r = new Reader();
			Writer wr = new Writer();
			vc = new ValidationChecks();
			String dateOfAdmission = "2019-03-19";
			DbDataObject dboAnimal = rdr.findAppropriateAnimalByAnimalId("10580123", svr);
			Long animalOrFlockId = dboAnimal.getObject_id();
			DbDataObject dboHolding = rdr.findAppropriateHoldingByPic("2932413703182", svr);
			// rdr instance of this service can be additionally completed,
			// depending on the client requirements
			if (animalOrFlockId != null) {
				resultMsgLabel = "naits.error.transfer.fail.anmalIdDoesNotExist";
				DbDataObject dboToMove = null;
				if (dboAnimal != null) {
					dboToMove = dboAnimal;
					if (dboToMove != null) {
						if (dboHolding != null) {
							wr.startAnimalOrFlockMovement(dboToMove, dboHolding, "DIRECT_TRANSFER", null, null, null,
									null, null, null, null, null, svr, svw, sww);
							svw.dbCommit();
							sww.dbCommit();
							if (vc.checkIfHoldingIsSlaughterhouse(dboHolding)) {
								wr.finishAnimalOrFlockMovement(dboToMove, dboHolding, null, dateOfAdmission,
										"62003012315", svw, sww);
							} else {
								wr.finishAnimalOrFlockMovement(dboToMove, dboHolding, null, null, null, svw, sww);
							}
							svw.dbCommit();
							sww.dbCommit();
							resultMsgLabel = "naits.success.transfer";
						}
					}
				}
			}
		} catch (SvException e) {
			log4j.error("Error occured in checkIfAnimalIdExist:" + e.getFormattedMessage(), e);
			resultMsgLabel = "naits.error.transfer.fail";
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
	}

	@Test
	public void testDependentClassRace() {
		SvReader svr = null;
		String token = null;
		Reader rdr = new Reader();
		ValidationChecks vc = new ValidationChecks();
		// 540752
		try {
			token = login("A.ADMIN3", "naits1234");
			svr = new SvReader(token);
			String animalClass = "1";

			/*
			 * DbSearchCriterion cr1 = new DbSearchCriterion("ANIMAL_CLASS",
			 * DbCompareOperand.EQUAL, animalClass); DbSearchExpression dbse = new
			 * DbSearchExpression(); dbse.addDbSearchItem(cr1); DbDataArray ar =
			 * svr.getObjects(dbse, SvReader.getTypeIdByName(Tc.ANIMAL_TYPE), null, 0, 0);
			 * 
			 * log4j.debug("THIS  " + ar.toSimpleJson().toString());
			 */
			DbDataArray ar = rdr.getAppropriateAnimalRacesPerClass("9", svr);
			ar.getSortedItems("PARENT_CODE_VALUE");
			log4j.debug("SIZE: " + ar.size() + "   THIS IS FROM CODES " + ar.toSimpleJson().toString());

			System.err.println();
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
			fail();
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
	}

	@Test
	public void testGeneratingPreMortemForms() {
		SvReader svr = null;
		SvWriter svw = null;
		String token = null;
		Reader rdr = new Reader();
		Writer wr = new Writer();
		ValidationChecks vc = new ValidationChecks();
		// 540752
		try {
			token = login("A.ADMIN3", "naits1234");
			svr = new SvReader(token);
			svw = new SvWriter(svr);
			svw.dbSetAutoCommit(false);
			String animalClass = "1";
			int count = 0;
			DbDataArray ar = svr.getObjectsByParentId(1269556L, SvReader.getTypeIdByName(Tc.ANIMAL), null, 0, 0);
			for (DbDataObject obj : ar.getItems()) {
				if (obj != null && obj.getStatus().equals("VALID") && vc.checkIfPreMortemCanBeGenerated(obj, svr)) {
					DbDataObject preMortem = wr.createPreMortemForm("CLINIC_EXAM", "1", obj.getObject_id());
					svw.saveObject(preMortem, false);
					count++;
				}
			}
			svw.dbCommit();
			log4j.debug(count);
			System.err.println();
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
			fail();
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
	}

	@Test
	public void testSubActionsAndHolding() {
		SvReader svr = null;
		SvWriter svw = null;
		String token = null;
		Reader rdr = new Reader();
		Writer wr = new Writer();
		ValidationChecks vc = new ValidationChecks();
		// 540752
		try {
			token = login("A.ADMIN3", "naits1234");
			svr = new SvReader(token);
			svw = new SvWriter(svr);
			String animalClass = "1";
			String subActionName = "";
			/*
			 * DbDataObject dboObjectToHandle = svr.getObjectById(1269556L,
			 * SvReader.getTypeIdByName(Tc.HOLDING), null); if
			 * ((!subActionName.toUpperCase().equals(Tc.SLAUGHTRD)) &&
			 * vc.checkIfHoldingIsSlaughterhouse(dboObjectToHandle.getObject_id( ), svr)) {
			 * if (!subActionName.toUpperCase().equals(Tc.DESTROYED) &&
			 * vc.checkIfHoldingIsSlaughterhouse(dboObjectToHandle.getObject_id( ), svr)) {
			 * throw (new SvException("naits.error.THIS_IS_ELSE_IF",
			 * svr.getInstanceUser())); } }
			 */

			ArrayList<String> arl = null;
			LinkedHashMap<String, String> jsonOrderedMap = new LinkedHashMap<String, String>();
			DbDataObject populationObj = svr.getObjectById(2439256L, SvReader.getTypeIdByName(Tc.POPULATION), null);

			DbDataObject jobOfPopulation = rdr.getPopulationJob(populationObj.getObject_id(), svr);

			DbDataArray areas = rdr.getLinkedAreaPerPopulation(populationObj, svr);

			DbDataArray filters = null; // rdr.population(populationObj,
										// Tc.CRITERIA, svr);
			DbDataArray samples = null; // rdr.getPopulationChilds(populationObj,
										// Tc.SAMPLE, svr);

			jsonOrderedMap.put("naits.show.area_selected", "Area selected:");
			String areasString = "";
			String criteriaString = "";
			if (areas != null) {
				arl = new ArrayList<>();
				for (Integer i = 0; i < areas.size(); i++) {

					if (areas.get(i).getVal(Tc.AREA_NAME) != null) {
						arl.add(areas.get(i).getVal(Tc.AREA_NAME).toString());
					}
				}
				if (arl != null && arl.size() > 0) {
					log4j.debug(areasString);
					jsonOrderedMap.put("naits.show.areas", areasString.trim());
				}
			}

			jsonOrderedMap.put("naits.show.filters", "Filters selected:");
			if (filters != null) {
				arl = new ArrayList<>();
				for (Integer i = 0; i < filters.size(); i++) {
					if (filters.get(i).getVal("CRITERIA_TYPE_ID") != null) {

						arl.add(filters.get(i).getVal("CRITERIA_TYPE_ID").toString());
					}
				}
				if (arl != null && arl.size() > 0) {
					criteriaString = arl.toString();
					log4j.debug(criteriaString);
					jsonOrderedMap.put("naits.show.criteria_id", criteriaString.trim());
				}
			}
			String applyFilter = null;
			DbDataArray selectionResPop = null;// rdr.getSelectionResultByParentId(populationObj.getObject_id(),
												// svr);
			if ((populationObj.getStatus().toString().equals("INPROGRESS")
					|| populationObj.getStatus().toString().equals("FINAL")) && applyFilter.equals("Yes")
					&& selectionResPop != null && selectionResPop.size() > 0) {
				jsonOrderedMap.put("naits.show.filter_applied", "Filter applied: " + applyFilter);
			} else {
				jsonOrderedMap.put("naits.show.filter_applied", "Filter applied: No");
			}
			if (samples != null && samples.size() > 0) {
				String status = samples.get(samples.size() - 1).getStatus().toString();
				DbDataArray selectionResSamp = null;// rdr
				// .getSelectionResultByParentId(samples.get(samples.size() -
				// 1).getParent_id(), svr);
				if ((status.equals("INPROGRESS") || status.equals("FINAL")) && selectionResSamp != null
						&& selectionResSamp.size() > 0) {
					jsonOrderedMap.put("naits.show.samples", "Selection (Randomized) Sample Applied: Yes");
				}
			} else {
				jsonOrderedMap.put("naits.show.samples", "Selection (Randomized) Sample Applied: No");
			}

			log4j.debug(jsonOrderedMap.toString());

			System.err.println();
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
			fail();
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
	}

	/**
	 * Convert String to Long.
	 *
	 * @param string String to be converted.
	 * @return Converted Long.
	 */
	public Long convertStringIntoLong(String string) {
		try {
			return Long.parseLong(string);
		} catch (NumberFormatException exception) {
			// Output expected NumberFormatException.
			log4j.debug(exception);
		} catch (Exception exception) {
			// Output unexpected Exceptions.
			log4j.debug(exception);
		}
		return null;
	}

	@Test
	public void testOr() {
		String obj_id_str = "154541";
		if (NumberUtils.isNumber(obj_id_str)) {
			System.out.println(convertStringIntoLong(obj_id_str));
		} else {
			System.out.println("NE E");
		}
	}

	@Test
	public void testGetAreaLabelCodeByCodeValue() {
		SvReader svr = null;
		SvWriter svw = null;
		String token = null;
		Reader rdr = new Reader();
		Writer wr = new Writer();
		ValidationChecks vc = new ValidationChecks();
		// 540752
		try {
			token = login("A.ADMIN3", "naits1234");
			svr = new SvReader(token);
			// svw = new SvWriter(svr);
			DbDataObject searchObj = rdr.searchForObject(svCONST.OBJECT_TYPE_CODE, "CODE_VALUE", "AREA_CODE", svr);

			String areaLabelCode = rdr.translateCodeValueForField(searchObj.getObject_id(), "1153",
					svr.getUserLocaleId(svr.getInstanceUser()).toString(), svr);
			String p = I18n.getText(svr.getUserLocaleId(svr.getInstanceUser()).toString(), "crit.animal.animal_age");
			throw (new SvException("crit.animal.animal_age" + " blaaaa", svCONST.systemUser));
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
	}

	@Test
	public void testAreasCoreAreas() {
		SvReader svr = null;
		SvWriter svw = null;
		String token = null;
		Reader rdr = new Reader();
		Writer wr = new Writer();
		// 540752
		try {
			token = login("A.ADMIN3", "naits1234");
			svr = new SvReader(token);
			svw = new SvWriter(svr);
			svw.setAutoCommit(false);
			DbDataObject dboAreaHealth = new DbDataObject();
			/*
			 * dboAreaHealth.setObject_type(SvReader.getTypeIdByName(Tc. AREA_HEALTH));
			 * dboAreaHealth.setVal("DISEASE_ID", "TUBRC");
			 * dboAreaHealth.setVal("AREA_STATUS", "0");
			 * dboAreaHealth.setParent_id(1280344L);
			 */
			// svw.saveObject(dboAreaHealth);
			DbDataObject rabiesHealth = svr.getObjectById(2581176L, SvReader.getTypeIdByName(Tc.AREA_HEALTH), null);
			wr.setAreaHealthToSubAreasDependOnCoreAreaAreaHealthObj(rabiesHealth, svr, svw);
			log4j.debug("OK");
			svw.dbCommit();
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
	}

	@Test
	public void testUndoRetirement() {
		SvReader svr = null;
		SvWriter svw = null;
		String token = null;
		Reader rdr = new Reader();
		ValidationChecks vc = new ValidationChecks();
		Writer wr = new Writer();
		// 540752
		try {
			token = login("A.ADMIN3", "naits1234");
			svr = new SvReader(token);
			svw = new SvWriter(svr);
			svw.setAutoCommit(false);
			String objectTypeLabel = "animal";
			// 0671238
			DbDataObject dboObjectToHandle = rdr.findAppropriateAreaByCode("15253233", "3", svr);
			DbDataObject areaHealthObj = new DbDataObject();
			areaHealthObj.setObject_type(SvReader.getTypeIdByName(Tc.AREA_HEALTH));
			areaHealthObj.setVal("DISEASE_ID", "BRUC");
			areaHealthObj.setVal("AREA_STATUS", "2");
			areaHealthObj.setParent_id(dboObjectToHandle.getObject_id());
			svw.saveObject(areaHealthObj, false);
			if (vc.checkIfAreaHasDuplicateDisease(areaHealthObj, svr)) {
				log4j.debug("DUPLIKAT");
			} else {
				log4j.debug("NE SE");
			}

			// Nullable<DateTime> dt = new Nullable<>();
			// dboObjectToHandle.setVal(Tc.DEATH_DATE, dt.getValue());
			// Date date = new Date()
			// dboObjectToHandle.setVal("DEATH_DATE", );
			// log4j.debug("test: "+ dboObjectToHandle);
			// svw.saveObject(dboObjectToHandle);

			log4j.debug("success");
			// svw.dbCommit();

		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
	}

	@Test
	public void testReverseLinkAnimalMovementHolding() {
		SvReader svr = null;
		SvWriter svw = null;
		String token = null;
		Reader rdr = new Reader();
		Writer wr = new Writer();
		ValidationChecks vc = new ValidationChecks();
		// 540752
		try {
			token = login("A.ADMIN3", "naits1234");
			svr = new SvReader(token);
			DbDataObject animalM = svr.getObjectById(2583016L, SvReader.getTypeIdByName(Tc.ANIMAL_MOVEMENT), null);
			DbDataObject linkAnimalMoveHolding = SvReader.getLinkType(Tc.ANIMAL_MOVEMENT_HOLDING,
					SvReader.getTypeIdByName(Tc.HOLDING), SvReader.getTypeIdByName(Tc.ANIMAL_MOVEMENT));
			DbDataArray arr = svr.getObjectsByLinkedId(animalM.getObject_id(), animalM.getObject_type(),
					linkAnimalMoveHolding, SvReader.getTypeIdByName(Tc.HOLDING), true, null, 0, 0);
			if (arr != null) {
				log4j.debug("HOLDING  " + arr.toSimpleJson());
			}
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
	}

	@Test
	public void testTotalFlocks() {
		SvReader svr = null;
		SvWriter svw = null;
		String token = null;
		Reader rdr = new Reader();
		Writer wr = new Writer();
		// 540752
		try {
			token = login("A.ADMIN3", "naits1234");
			svr = new SvReader(token);
			svw = new SvWriter(svr);
			svw.setAutoCommit(false);
			DbDataObject dboFlock = rdr.findAppropriateFlockByFlockId("2640593704313/001", svr);
			if (svr.getSessionId() != null && dboFlock != null) {
				Long males_cnt = 0L;
				if (dboFlock.getVal("MALES") != null) {
					males_cnt = (Long) dboFlock.getVal("MALES");
				}
				Long females_cnt = 0L;
				if (dboFlock.getVal("FEMALES") != null) {
					females_cnt = (Long) dboFlock.getVal("FEMALES");
				}
				Long total = males_cnt + females_cnt;

				log4j.debug(total);
			}
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
	}

	@Test
	public void testLabelCodesForImport() {
		SvReader svr = null;
		String token = null;
		Reader rdr = new Reader();
		Writer wr = new Writer();
		// 540752
		try {
			token = login("A.ADMIN3", "naits1234");
			svr = new SvReader(token);

			DbDataArray ar = null;// rdr.getVillageLabelCodeByLabelText("Tbilisi",
									// "en_US", svr);
			if (ar != null && ar.size() == 1) {
				log4j.debug(ar.get(0).getVal(Tc.CODE_VALUE).toString());

				log4j.debug(ar.get(0).getVal(Tc.CODE_VALUE).toString().substring(0, 2));

				log4j.debug(ar.get(0).getVal(Tc.CODE_VALUE).toString().substring(0, 4));

				log4j.debug(ar.get(0).getVal(Tc.CODE_VALUE).toString().substring(0, 6));

			}

			// log4j.debug();
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
	}

	@Test
	public void testUndoOnLostAnimals() {
		SvReader svr = null;
		SvWriter svw = null;
		SvWorkflow sww = null;
		String token = null;
		Reader rdr = new Reader();
		Writer wr = new Writer();
		// 540752
		try {
			token = login("A.ADMIN3", "naits1234");
			svr = new SvReader(token);
			svw = new SvWriter(svr);
			sww = new SvWorkflow(svw);
			svw.setAutoCommit(false);

			DbDataObject dboToHandle = rdr.findAppropriateAnimalByAnimalId("0425551", svr);
			DbDataObject parentObj = svr.getObjectById(dboToHandle.getParent_id(), SvReader.getTypeIdByName(Tc.HOLDING),
					null);
			DbDataArray movements = null;
			if (dboToHandle.getStatus().equalsIgnoreCase("LOST")) {
				movements = svr.getObjectsByParentId(dboToHandle.getObject_id(),
						SvReader.getTypeIdByName(Tc.ANIMAL_MOVEMENT), null, 0, 0);
				if (movements != null && movements.size() > 0) {
					for (DbDataObject animalMovement : movements.getItems()) {
						if (animalMovement != null && animalMovement.getStatus().equals(Tc.VALID)
								&& animalMovement.getVal(Tc.MOVEMENT_REASON).equals(dboToHandle.getStatus())
								&& animalMovement.getVal(Tc.SOURCE_HOLDING_ID) != null
								&& animalMovement.getVal(Tc.DESTINATION_HOLDING_ID) == null) {
							wr.finishAnimalOrFlockMovement(dboToHandle, parentObj, null, null, null, svw, sww);
							break;
						}
					}
				}
			}

			svw.dbCommit();
			sww.dbCommit();
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
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
	}

	@Test
	public void testLinkBetweenAnimMovementAndHoldingWithoutDestination() {
		SvReader svr = null;
		SvWriter svw = null;
		SvWorkflow sww = null;
		String token = null;
		Reader rdr = new Reader();
		Writer wr = new Writer();
		// 540752
		try {
			token = login("A.ADMIN3", "naits1234");
			svr = new SvReader(token);
			svw = new SvWriter(svr);
			sww = new SvWorkflow(svw);
			svw.setAutoCommit(false);

			DbDataObject dboToHandle = rdr.findAppropriateAnimalByAnimalId("11302668", svr);
			wr.createAnimalOrFlockMovementWithoutDestination(dboToHandle, "LOST", "", svr, svw, sww);

			svw.dbCommit();
			sww.dbCommit();
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
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
	}

	@Test
	public void testVaccCamaignName() {
		SvReader svr = null;
		// SvWriter svw = null;
		// SvWorkflow sww = null;
		String token = null;
		Reader rdr = new Reader();
		Writer wr = new Writer();
		// 540752
		try {
			token = login("A.ADMIN3", "naits1234");
			svr = new SvReader(token);
			// svw = new SvWriter(svr);
			// sww = new SvWorkflow(svw);
			// svw.setAutoCommit(false);
			DbDataObject obj = rdr.searchForObject(svCONST.OBJECT_TYPE_CODE, Tc.CODE_VALUE, "VACC_ACTIVITY_TYPE", svr);
			DbDataObject tempVaccEvent = svr.getObjectById(1199611L, SvReader.getTypeIdByName("VACCINATION_EVENT"),
					null);
			String hello = "";
			String localeId = "ka_GE";// svr.getUserLocaleId(svr.getInstanceUser()).toString();
			hello = (tempVaccEvent.getVal("CAMPAIGN_NAME").toString() + "/"
					+ rdr.decodeCodeValue(tempVaccEvent.getObject_type(), "ACTIVITY_TYPE",
							tempVaccEvent.getVal("ACTIVITY_TYPE").toString(), localeId, svr)
					+ (tempVaccEvent.getVal("CAMPAIGN_SCOPE") != null
							? "/" + rdr.decodeCodeValue(tempVaccEvent.getObject_type(), "CAMPAIGN_SCOPE",
									tempVaccEvent.getVal("CAMPAIGN_SCOPE").toString(), localeId, svr)
							: "")
					+ (tempVaccEvent.getVal("DISEASE") != null
							? "/" + rdr.decodeCodeValue(tempVaccEvent.getObject_type(), "DISEASE",
									tempVaccEvent.getVal("DISEASE").toString(), localeId, svr)
							: ""));

			log4j.debug("NAME:   " + hello);

		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
		} finally {
			if (svr != null) {
				svr.release();
			}
			/*
			 * if (svw != null) { svw.release(); } if (sww != null) { sww.release(); }
			 */
		}
	}

	@Test
	public void testMassAnimalGenerator() {
		SvReader svr = null;
		SvWriter svw = null;
		String token = null;
		Reader rdr = new Reader();
		Writer wr = new Writer();
		// 540752
		try {
			token = login("A.ADMIN3", "naits1234");
			svr = new SvReader(token);
			svw = new SvWriter(svr);
			String startEarTag = "22";
			String endEarTag = "344";
			int maxSizeOfAnimalId = 10;
			int startInt = 0;
			int endInt = 0;
			if (NumberUtils.isNumber(startEarTag) && NumberUtils.isNumber(endEarTag)) {
				startInt = startEarTag.length();
				endInt = endEarTag.length();
				if (startInt <= 3 && endInt <= 3) {
					maxSizeOfAnimalId -= (startInt + endInt);
				}
			}

			String pom = wr.generateAnimalObjects(62973L, "aa", "311", "1", svr, svw);
			log4j.debug(pom);
			// log4j.debug("Integer max_val: " + Long.MAX_VALUE);
		} catch (SvException e) {
			log4j.error(e.getLabelCode());
		} finally {
			if (svr != null) {
				svr.release();
			}
			if (svw != null) {
				svw.release();
			}
		}
	}

	@Test
	public void testDeleteOnSubAreas() {
		SvReader svr = null;
		SvWriter svw = null;
		String token = null;
		Reader rdr = new Reader();
		Writer wr = new Writer();
		// 540752
		try {
			token = login("A.ADMIN3", "naits1234");
			svr = new SvReader(token);
			svw = new SvWriter(svr);
			svw.setAutoCommit(false);
			DbDataObject dboAreaHealth = svr.getObjectById(2581176L, SvReader.getTypeIdByName(Tc.AREA_HEALTH), null);
			dboAreaHealth.setDt_delete(new DateTime());
			svw.saveObject(dboAreaHealth);
			DbDataObject dboAreaHlt = svr.getObjectById(2581008L, SvReader.getTypeIdByName(Tc.AREA_HEALTH), null);
			log4j.info(dboAreaHealth.toSimpleJson());
			if (dboAreaHealth != null && dboAreaHealth.getDt_delete().isBeforeNow()) {
				wr.autoDeleteSubAreasHealthIfCoreAreaHealthIsDeleted(dboAreaHealth, svr, svw);
			}
			log4j.debug("OK");
			svw.dbCommit();
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
	}

	@Test
	public void testProhibitTwoActiveQuarantines() {
		SvReader svr = null;
		// SvWriter svw = null;
		String token = null;
		Reader rdr = new Reader();
		Writer wr = new Writer();
		ValidationChecks vc = new ValidationChecks();
		// 540752
		try {
			token = login("A.ADMIN3", "naits1234");
			svr = new SvReader(token);
			DbDataObject holdingObj = svr.getObjectById(63427L, SvReader.getTypeIdByName(Tc.HOLDING), null);
			Boolean res = vc.checkIfHoldingBelongsInActiveQuarantine(holdingObj, svr);
			if (res == true) {
				log4j.debug("Pripagja");
			} else {
				log4j.debug("Not");
			}

		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
	}

	@Test
	public void testVaccBookValuesSetDependOnVaccEvent() {
		SvReader svr = null;
		// SvWriter svw = null;
		String token = null;
		Reader rdr = new Reader();
		Writer wr = new Writer();
		SvWriter svw = null;
		ValidationChecks vc = new ValidationChecks();
		// 540752
		try {
			token = login("A.ADMIN3", "naits1234");
			svr = new SvReader(token);
			svw = new SvWriter(svr);
			svw.setAutoCommit(false);
			Calendar calendar = Calendar.getInstance();
			java.sql.Date dtNow = new java.sql.Date(calendar.getTime().getTime());

			DbDataObject vaccBook = new DbDataObject();
			vaccBook.setObject_type(SvReader.getTypeIdByName(Tc.VACCINATION_BOOK));
			vaccBook.setVal("VACC_CODE", "ნოდურალური დერმატიტი - შემოდგომა 2018");
			vaccBook.setVal("VACC_DATE", dtNow);
			wr.setVaccBookDependOnVaccEvent(vaccBook, svr, svw);

			svw.saveObject(vaccBook);
			log4j.debug("success: " + vaccBook.toSimpleJson());
			svw.dbCommit();
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
	}

	@Test
	public void testOrderOfNewAddedObjects() {
		SvReader svr = null;
		// SvWriter svw = null;
		String token = null;
		Reader rdr = new Reader();
		Writer wr = new Writer();
		SvWriter svw = null;
		ValidationChecks vc = new ValidationChecks();
		// 540752
		try {
			token = login("A.ADMIN3", "naits1234");
			svr = new SvReader(token);

			DbDataObject animalObj = rdr.findAppropriateAnimalByAnimalId("10417750", svr);
			/*
			 * DbDataObject linkAnimVaccBook = svr.getLinkType("ANIMAL_VACC_BOOK",
			 * animalObj.getObject_type(), SvReader.getTypeIdByName(Tc.VACCINATION_BOOK));
			 * DbDataArray arr = svr.getObjectsByLinkedId(animalObj.getObject_id(),
			 * linkAnimVaccBook, null, 0, 0); arr.getSortedItems("DT_INSERT", true);
			 * DbDataObject test = arr.get(arr.size() - 1);
			 */
			DbDataObject dbo = svr.getObjectById(1278406L, SvReader.getTypeIdByName(Tc.OTHER_ANIMALS), null);
			int count = 0;
			for (Entry<SvCharId, Object> entry : dbo.getValuesMap().entrySet()) {
				if (entry.getValue() != null && !entry.getValue().toString().equals("")) {
					log4j.debug(entry.getKey().toString());
					count++;
				}
			}
			log4j.debug(count);
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
	}

	@Test
	public void testEarReplc() {
		SvReader svr = null;
		// SvWriter svw = null;
		String token = null;
		Reader rdr = new Reader();
		Writer wr = new Writer();
		SvWriter svw = null;
		ValidationChecks vc = new ValidationChecks();
		// 540752
		try {
			token = login("A.ADMIN3", "naits1234");
			svr = new SvReader(token);
			svw = new SvWriter(svr);
			svw.setAutoCommit(false);
			Calendar calendar = Calendar.getInstance();
			java.sql.Date dtNow = new java.sql.Date(calendar.getTime().getTime());
			DbDataObject animalObj = rdr.findAppropriateAnimalByAnimalId("9668564", svr);
			DbDataObject earTagReplc = null;// wr.createEarTagReplcObject("9768564",
											// "2019-03-03", "UNIT
											// TEST",animalObj.getObject_id(),
											// svr);
			svw.saveObject(earTagReplc, false);
			String result = wr.updateAnimalEarTagByEarTagReplacement(earTagReplc, svr, svw);
			log4j.debug("result message: " + result);
			svw.dbCommit();
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
		} finally {
			if (svr != null) {
				svr.release();
			}
			if (svw != null) {
				svw.release();
			}
		}
	}

	@Test
	public void testValidationsOnFlock() {
		SvReader svr = null;
		// SvWriter svw = null;
		String token = null;
		Reader rdr = new Reader();
		Writer wr = new Writer();
		ValidationChecks vc = new ValidationChecks();
		// 540752
		try {
			token = login("A.ADMIN3", "naits1234");
			svr = new SvReader(token);
			Calendar calendar = Calendar.getInstance();
			java.sql.Date dtNow = new java.sql.Date(calendar.getTime().getTime());
			DbDataObject flockObj = rdr.findAppropriateFlockByFlockId("1313123000008/001", svr);
			String dtInst = flockObj.getDt_insert().toString();
			log4j.debug(dtInst);
			if (dtInst.startsWith("2019-05-17")) {
				log4j.debug("OK");
			} else {
				log4j.debug("NOT");
			}
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
	}

	@Test
	public void testMandatoryFields() {
		SvReader svr = null;
		// SvWriter svw = null;
		String token = null;
		Reader rdr = new Reader();
		Writer wr = new Writer();
		ValidationChecks vc = new ValidationChecks();
		// 540752
		try {
			token = login("A.ADMIN3", "naits1234");
			svr = new SvReader(token);
			rdr = new Reader();
			Calendar calendar = Calendar.getInstance();
			java.sql.Date dtNow = new java.sql.Date(calendar.getTime().getTime());
			DbDataObject animalObj = rdr.findAppropriateAnimalByAnimalId("5614484223", svr);
			ArrayList<String> ar = rdr.getAnimalMandatoryFields(animalObj, svr);
			if (ar != null && ar.size() > 0) {
				throw (new SvException("mu fali mandatory field", svr.getInstanceUser()));
			} else {
				log4j.error("OK");
			}

		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
	}

	@Test
	public void testCurrentDate() {
		SvReader svr = null;
		String token = null;
		// 540752
		try {

			token = login("ADMIN", "welcome");
			svr = new SvReader(token);
			String pattern = Tc.DATE_PATTERN;
			DateTime ld = new DateTime("2018-09-04");
			DateTime now = new DateTime();
			DateTime convertedCurrentDate = DateTime.parse(now.toString().substring(0, 10),
					DateTimeFormat.forPattern(pattern));
			Period dif = new Period(ld, now);
			log4j.debug(dif.getYears() * 12 + dif.getMonths());

			// test period

			ValidationChecks vc = new ValidationChecks();

			Integer diffDays = (int) vc.getDateDiff(ld, convertedCurrentDate, TimeUnit.DAYS);

			log4j.debug(diffDays);
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
			fail();
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
	}

	@Test
	public void checkIfFieldIsUnique() {
		SvReader svr = null;
		String token = null;
		Reader rdr = null;
		SvWriter svw = null;
		SvWorkflow sww = null;
		ValidationChecks vc = null;
		// 540752
		try {

			token = login("ADMIN", "welcome");
			svr = new SvReader(token);
			svw = new SvWriter(svr);
			sww = new SvWorkflow(svw);
			rdr = new Reader();
			vc = new ValidationChecks();
			Writer wr = new Writer();
			DbDataObject dbo = svr.getObjectById(245966L, SvReader.getTypeIdByName(Tc.HOLDING_RESPONSIBLE), null);
			DbDataObject dbo1 = svr.getObjectById(37924L, SvReader.getTypeIdByName(Tc.HOLDING_RESPONSIBLE), null);
			dbo1.setVal("PHONE_NUMBER", "12312300");
			if (vc.checkIfFieldIsUnique(Tc.NAT_REG_NUMBER, dbo, svr)) {
				log4j.debug("OK");
			} else {
				log4j.debug("NOT OK");
			}

			if (dbo.equals(dbo1)) {
				log4j.debug("EQUALS TEST on OBJ: SUCC");
			} else {
				log4j.debug("EQUALS TEST on OBJ: FAIL");
			}

			String subActionName = "";
			DbDataObject dboAnimal = rdr.findAppropriateAnimalByAnimalId("0688224", svr);
			wr.cancelMovement(dboAnimal, sww, svr);
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
			fail();
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
	}

	@Test
	public void testCancelAnimalMovement() {
		SvReader svr = null;
		String token = null;
		Reader rdr = null;
		ValidationChecks vc = null;
		SvWorkflow sww = null;
		SvWriter svw = null;
		// 540752
		try {

			token = login("A.ADMIN3", "naits1234");
			svr = new SvReader(token);
			svw = new SvWriter(svw);
			sww = new SvWorkflow(svr);
			rdr = new Reader();
			vc = new ValidationChecks();
			String subActionName = "CANCEL_MOVEMENT";
			DbDataObject dboAnimal = rdr.findAppropriateAnimalByAnimalId("1291072", svr);
			DbDataArray existingMovement = rdr.getExistingAnimalOrFlockMovements(dboAnimal, "2932413703182", Tc.VALID,
					svr);
			if (existingMovement.size() > 0) {
				DbDataObject animalOrFlockMovementObj = existingMovement.get(0);
				if (subActionName.equalsIgnoreCase("CANCEL_MOVEMENT")) {
					if (dboAnimal.getStatus().equals(Tc.TRANSITION)
							&& animalOrFlockMovementObj.getStatus().equals(Tc.VALID)) {
						sww.moveObject(dboAnimal, Tc.VALID);
						sww.moveObject(animalOrFlockMovementObj, "CANCELED");
					}
				} else {
					throw (new SvException("naits.error.nofound", svr.getInstanceUser()));
				}
			}
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
			fail();
		} finally {
			if (svr != null) {
				svr.release();
			}
			if (svw != null) {
				svr.release();
			}
			if (sww != null) {
				svr.release();
			}
		}
	}

	@Test
	public void testRunningQuery() throws SQLException {
		SvReader svr = null;
		String token = null;
		Reader rdr = null;
		ValidationChecks vc = null;
		String s = "";
		PreparedStatement cst = null;
		ResultSet rs = null;
		try {
			token = login("A.ADMIN3", "naits1234");
			svr = new SvReader(token);
			rdr = new Reader();
			vc = new ValidationChecks();
			GeometryFactory gf = new GeometryFactory();
			Coordinate coord = new Coordinate();
			ArrayList<String> dms = new ArrayList<>();
			ArrayList<String> result = new ArrayList<>();

			String temp = "";
			cst = svr.dbGetConn().prepareStatement(
					"select region_code, munic_code,commun_code,village_code,naits.translateCodeValue(region_code,'REGIONS','en_US')reg,naits.translateCodeValue(munic_code,'MUNICIPALITIES','en_US')mun,naits.translateCodeValue(commun_code,'COMMUNITIES','en_US')comm,naits.translateCodeValue(village_code,'VILLAGES','en_US')vill,village_code FROM naits.vvillage");

			rs = cst.executeQuery();
			while (rs.next()) {
				dms.add(rs.getString(1));
				dms.add(rs.getString(2));
				dms.add(rs.getString(3));
				dms.add(rs.getString(4));
				dms.add(rs.getString(5));
				dms.add(rs.getString(6));
				dms.add(rs.getString(7));
				dms.add(rs.getString(8));
				dms.add(":");
			}
			ArrayList<String> ar = new ArrayList<>();
			String[] test = new String[10000];
			String[] splitByComma = new String[100];
			test = dms.toString().split(":");
			for (int i = 0; i < test.length; i++) {
				if (test[i].contains("Gurjaani") && test[i].contains("Gurjaani")) {
					ar.add(test[i]);
				}

			}
			if (ar != null && ar.size() > 0) {
				splitByComma = ar.get(0).toString().split(",");
				result.add(splitByComma[1]);
				result.add(splitByComma[2]);
				result.add(splitByComma[3]);
				result.add(splitByComma[4]);
			}
			log4j.debug(result.toString());
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
			fail();
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
	}

	@Test
	public void testVillageCode() throws SQLException {
		SvReader svr = null;
		String token = null;
		Reader rdr = null;
		ValidationChecks vc = null;
		String s = "";
		PreparedStatement cst = null;
		ResultSet rs = null;
		// 540752
		try {

			token = login("ADMIN", "welcome");
			svr = new SvReader(token);
			rdr = new Reader();
			vc = new ValidationChecks();
			String input = "0.1234515";
			DbDataObject dbo = svr.getObjectById(245966L, SvReader.getTypeIdByName(Tc.HOLDING_RESPONSIBLE), null);
			// DbDataObject test =
			// rdr.getVillageInfoByCommunityAndVillage("Zugdidi", "Zugdidi",
			// "en_US", svr);

			GeometryFactory gf = new GeometryFactory();
			Coordinate coord = new Coordinate();
			String dms = "";
			cst = svr.dbGetConn().prepareStatement("SELECT (ST_AsLatLonText(ST_MakePoint(?, ?)));");
			// 'POINT (41.413182 43.48346)'
			cst.setDouble(1, 41.413182);
			cst.setDouble(2, 43.48346);

			rs = cst.executeQuery();
			while (rs.next()) {
				dms = rs.getString(1);
			}

			String[] sArr = input.split("[.]");
			String s0 = sArr[0];
			if (s0.length() < 2) {
				s = "0" + s0;
			} else {
				s = s0;
			}

			log4j.debug(s);

			String[] dmsArray = dms.split("[NE]+");

			String dmsLat = dmsArray[0];
			String dmsLon = dmsArray[1];

			dbo.setVal("GPS_NORTH", dmsLat);
			dbo.setVal("GPS_EAST", dmsLon);

			cst = svr.dbGetConn().prepareStatement(
					"SELECT 	ST_X (ST_TRANSFORM( ST_Transform(ST_SetSRID(ST_MakePoint(?, ?),?),	?) , ?) ),ST_Y (ST_TRANSFORM( ST_Transform(ST_SetSRID(ST_MakePoint(?, ?),?), ?), ?) );");
			// x params
			cst.setDouble(1, Double.valueOf(41.413182));
			cst.setDouble(2, Double.valueOf(43.48346));
			cst.setInt(3, 4326);
			cst.setInt(4, 32638);
			cst.setInt(5, 32638);
			// y params
			cst.setDouble(6, Double.valueOf(41.413182));
			cst.setDouble(7, Double.valueOf(43.48346));
			cst.setInt(8, 4326);
			cst.setInt(9, 32638);
			cst.setInt(10, 32638);

			rs = cst.executeQuery();
			while (rs.next()) {
				coord.x = rs.getDouble(1);
				coord.y = rs.getDouble(2);
			}

			Point point = gf.createPoint(coord);
			SvGeometry.setGeometry(dbo, point);

			log4j.debug(dmsArray);

		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
			fail();
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
	}

	@Test
	public void testLabSampleMassHandler() throws Exception {
		SvReader svr = null;
		SvWriter svw = null;
		String token = null;
		Reader rdr = null;
		MassActions ma = null;
		// 540752
		try {

			token = login("A.ADMIN3", "naits1234");
			svr = new SvReader(token);
			svw = new SvWriter(svr);
			rdr = new Reader();
			ma = new MassActions();
			OnSaveValidations call = new OnSaveValidations();
			SvCore.registerOnSaveCallback(call, SvReader.getTypeIdByName("LAB_SAMPLE"));
			DbDataObject sampleObj = svr.getObjectById(3126272L, SvReader.getTypeIdByName(Tc.LAB_SAMPLE), null);
			sampleObj.setVal("CHECK_COLUMN", "1");
			svw.saveObject(sampleObj);

		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
			fail();
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
	}

	@Test
	public void testDirectTransferOnFlock() {
		SvReader svr = null;
		String token = null;
		// 540752
		try {

			token = login("A.ADMIN3", "naits1234");
			svr = new SvReader(token);
			String pattern = Tc.DATE_PATTERN;
			DateTime ld = new DateTime("2017-10-21");
			DateTime now = new DateTime();
			DateTime convertedCurrentDate = DateTime.parse(now.toString().substring(0, 10),
					DateTimeFormat.forPattern(pattern));
			Period dif = new Period(ld, convertedCurrentDate);
			log4j.debug(dif.getYears() + " " + dif.getMonths() + " " + dif.getDays());

			ValidationChecks vc = new ValidationChecks();
			Reader rdr = new Reader();

			String pom = testFlockDirectTransfer(78036L, "1269556", "2017-10-21", "52001021324", svr);

			log4j.debug(pom);
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
			fail();
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
	}

	@Test
	public void generateHic2() {
		String token = login("ADMIN", "welcome");

		SvReader svr = null;
		SvSequence svs = null;
		String hic = null;
		try {
			svr = new SvReader(token);
			if (svr.getSessionId() != null) {
				svs = new SvSequence(svr.getSessionId());
			}
			DbDataObject dbo = svr.getObjectById(12436L, SvReader.getTypeIdByName("HOLDING"), null);
			DateTime tmpDsh = new DateTime(dbo.getVal("DT_CREATION"));
			log4j.info("success");

			// FORMAT: RRMMCCVVHHHHX
			// 2d for the region - RR
			String regionCode = "00";
			// 2d for the municipality -MM
			String municCode = "00";
			// 2d for the community - CC
			String communityCode = "00";
			// 2d for the village - VV
			String villageCode = "00";
			// 4d for the holding (sequence) - HHHH
			String holdingSeqCode = "";
			Long seqId = svs.getSeqNextVal(villageCode, false);
			holdingSeqCode = String.format("%04d", Integer.valueOf(seqId.toString()));
			// 1d check digit (ex.mod12) - X
			String checkDigit = calc(regionCode + municCode + villageCode + holdingSeqCode);
			System.out.println(checkDigit);
			System.out.println(
					"The calculated HIC:" + regionCode + municCode + villageCode + holdingSeqCode + checkDigit);

		} catch (Exception e) {
			e.printStackTrace();
			log4j.error(e);
		} finally {
			if (svr != null) {
				svr.release();
			}
			if (svs != null) {
				svs.release();
			}
		}

	}

	@Test
	public void testLabSampleId() {
		SvReader svr = null;
		String token = null;
		SvSequence svs = null;
		OnSaveValidations osv = new OnSaveValidations();
		// 540752
		try {

			token = login("A.ADMIN3", "naits1234");
			svr = new SvReader(token);
			svs = new SvSequence(svr.getSessionId());
			// 50110
			Reader rdr = new Reader();
			String date = "2019-05-05";
			String concDate = date.replaceAll("-", "").substring(1);
			log4j.debug(concDate.substring(1));

			DbDataObject dbo = svr.getObjectById(78360L, SvReader.getTypeIdByName(Tc.ANIMAL), null);
			String animalId = "10550790";
			String animalSampledSeq = "";
			Long seqId = svs.getSeqNextVal(animalId, false);
			animalSampledSeq = String.format("%04d", Integer.valueOf(seqId.toString()));
			String checkDigit = osv.calcCheckDigit(animalId + animalSampledSeq);
			String generateLabSampleId = animalId + animalSampledSeq;

			svs.dbCommit();
			System.out.println(generateLabSampleId);
			System.out.println("The calculated HIC:" + animalId + animalSampledSeq + checkDigit);

			DbDataObject healthBookObj = svr.getObjectById(1200418L, SvReader.getTypeIdByName(Tc.VACCINATION_BOOK),
					null);
			DbDataObject test = rdr.getAnimalLinkedToVaccinationBook(healthBookObj, svr);
			if (test != null) {
				log4j.debug("OK");
			} else {
				log4j.debug("NOT");
			}

			log4j.debug(generateLabSampleId);
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
			fail();
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
	}

	@Test
	public void testLinkBetweenLaboratoryAndSample() {
		SvReader svr = null;
		String token = null;
		Reader rdr = null;
		SvWriter svw = null;
		SvWorkflow sww = null;
		ValidationChecks vc = null;
		// 540752
		try {

			token = login("A.ADMIN3", "naits1234");
			svr = new SvReader(token);
			svw = new SvWriter(svr);
			sww = new SvWorkflow(svw);
			rdr = new Reader();
			vc = new ValidationChecks();
			Writer wr = new Writer();
			String subActionName = "CANCEL_MOVEMENT";
			DbDataObject dboLab = svr.getObjectById(2594062L, SvReader.getTypeIdByName(Tc.LABORATORY), null);
			DbDataObject dboLabSample = svr.getObjectById(2594274L, SvReader.getTypeIdByName(Tc.LAB_SAMPLE), null);

			OnSaveValidations osv = new OnSaveValidations();

			log4j.debug(dboLab.toJson());
			log4j.debug(dboLabSample.toJson());

		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
			fail();
		} finally {
			if (svr != null) {
				svr.release();
			}
			if (svw != null) {
				svr.release();
			}
			if (sww != null) {
				svr.release();
			}
		}
	}

	@Test
	public void createLabSampleBasedOnHealthBook() {
		SvReader svr = null;
		String token = null;
		Reader rdr = null;
		SvWriter svw = null;
		SvWorkflow sww = null;
		ValidationChecks vc = null;
		// 540752
		try {

			token = login("A.ADMIN3", "naits1234");
			svr = new SvReader(token);
			svw = new SvWriter(svr);
			sww = new SvWorkflow(svw);
			rdr = new Reader();
			vc = new ValidationChecks();
			Writer wr = new Writer();

			String pattern = Tc.DATE_PATTERN;
			DbDataObject dbo = svr.getObjectById(2594634L, SvReader.getTypeIdByName(Tc.LAB_SAMPLE), null);
			dbo.setVal(Tc.LABORATORY_NAME, "TEST_TEST_4");

			DbDataObject labSampleDb = svr.getObjectById(dbo.getObject_id(), dbo.getObject_type(), new DateTime());

			DateTime convertedActionDate = DateTime.parse("2019-02-08", DateTimeFormat.forPattern(pattern));

			// wr.createLabSampleBasedOnAnimalHealthBook(78778L, "2019-02-08",
			// svr, svw);
			// wr.createNLabTestResultDependOnMultiSelectedDiseasesInLabSample(dbo,
			// svw);
			log4j.debug("FINISH");
			// log4j.debug(dboLab.toJson());
			// log4j.debug(dboLabSample.toJson());

		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
			fail();
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
	}

	@Test
	public void testLabTestResult() throws SvException {
		SvReader svr = null;
		SvWriter svw = null;
		Reader rdr = new Reader();
		try {
			String token = login("A.ADMIN3", "naits1234");
			svr = new SvReader(token);
			svw = new SvWriter(svr);
			svw.setAutoCommit(false);
			OnSaveValidations call = new OnSaveValidations();
			DbSearchCriterion crit = new DbSearchCriterion(Tc.NAME, DbCompareOperand.EQUAL, "munic.munic_code_4728");
			DbDataArray orgUnuts = svr.getObjects(crit, svCONST.OBJECT_TYPE_ORG_UNITS, null, 0, 0);
			DbDataObject orgUnit = orgUnuts.getItems().get(0);
			Long parent_id = orgUnit.getObject_id();
			DbDataObject testType = svr.getObjectById(2597061L, SvReader.getTypeIdByName(Tc.LAB_TEST_TYPE), null);
			DbDataArray result = new DbDataArray();
			DbDataArray ar = svr.getObjectsByTypeId(SvReader.getTypeIdByName(Tc.LAB_TEST_TYPE), null, 0, 0);
			int counter = 0;
			if (ar != null && ar.size() > 0) {
				log4j.debug(ar.size());
				for (DbDataObject dbo : ar.getItems()) {
					if (dbo.getVal("SAMPLE_TYPE").toString().length() == 1
							&& !NumberUtils.isNumber(dbo.getVal("SAMPLE_TYPE").toString())) {
						dbo.setDt_delete(new DateTime());
						svw.saveObject(dbo);
						result.addDataItem(dbo);
						counter++;
					}
				}
			}
			svw.saveObject(result);
			log4j.debug(counter);
			svw.dbCommit();
		} catch (SvException e) {
			log4j.error("Error: ", e.getFormattedMessage());
			// fail();
		} finally {
			if (svr != null) {
				svr.release();
			}
			if (svw != null) {
				svw.release();
			}
		}
	}

	@Test
	public void testPendingExport() throws SvException {
		SvReader svr = null;
		SvWriter svw = null;
		Reader rdr = new Reader();
		try {
			String token = login("A.ADMIN3", "naits1234");
			svr = new SvReader(token);
			svw = new SvWriter(svr);
			svw.setAutoCommit(false);
			// brucellosis
			// q_fever
			OnSaveValidations call = new OnSaveValidations();
			SvCore.registerOnSaveCallback(call, SvReader.getTypeIdByName("LAB_SAMPLE"));
			MassActions ma = new MassActions();
			DbDataObject exportCertObj = svr.getObjectById(1278559L, SvReader.getTypeIdByName(Tc.EXPORT_CERT), null);

			// S//tring res =
			// ma.exportCertifiedAnimals(exportCertObj.getObject_id(), svr);

			log4j.debug(exportCertObj.toSimpleJson());
			// log4j.debug(res);
		} catch (SvException e) {
			log4j.error("Error: ", e.getFormattedMessage());
			// fail();
		} finally {
			if (svr != null) {
				svr.release();
			}
			if (svw != null) {
				svw.release();
			}
		}
	}

	@Test
	public void testValidTestType() throws SvException {
		SvReader svr = null;
		SvWriter svw = null;
		Reader rdr = new Reader();
		try {
			String token = login("A.ADMIN3", "naits1234");
			svr = new SvReader(token);
			svw = new SvWriter(svr);
			svw.setAutoCommit(false);
			// brucellosis
			// q_fever
			DbDataObject testResult = svr.getObjectById(2596086L, SvReader.getTypeIdByName(Tc.LAB_TEST_RESULT), null);
			String res = "";
			DbDataArray arr = rdr.getValidTestTypes(testResult.getObject_id(), "1", svr);

			String pom = rdr.convertDbDataArrayToGridJson(arr, "BLA");

			log4j.debug(pom);
		} catch (SvException e) {
			log4j.error("Error: ", e.getFormattedMessage());
			// fail();
		} finally {
			if (svr != null) {
				svr.release();
			}
			if (svw != null) {
				svw.release();
			}
		}
	}

	@Test
	public void testLabTEstType() throws SvException {
		SvReader svr = null;
		SvWriter svw = null;
		Reader rdr = new Reader();
		try {
			String token = login("A.ADMIN3", "naits1234");
			svr = new SvReader(token);
			svw = new SvWriter(svr);
			svw.setAutoCommit(false);
			// brucellosis
			// q_fever
			DbDataArray ar = rdr.getTestTypeApplicableCombination("3,23", "3", svr);// blood

			log4j.debug(ar.toSimpleJson());
		} finally {
			if (svr != null) {
				svr.release();
			}
			if (svw != null) {
				svw.release();
			}
		}
	}

	@Test
	public void testObjectsOnSaveScenarios() throws SvException {
		SvWriter svw = null;
		SvReader svr = null;
		Reader rdr = new Reader();
		DbDataObject dboLabSample = null;
		try {
			String token = login("A.ADMIN3", "naits1234");
			OnSaveValidations call = new OnSaveValidations();
			SvCore.registerOnSaveCallback(call, SvReader.getTypeIdByName("LAB_SAMPLE"));
			svr = new SvReader(token);
			svw = new SvWriter(svr);
			svw.dbSetAutoCommit(false);
			DbDataObject dboAnimal = rdr.findAppropriateAnimalByAnimalId("10027493", svr);
			dboLabSample = new DbDataObject();
			// dboLabSample = svr.getObjectById(3126699L,
			// SvReader.getTypeIdByName(Tc.LAB_SAMPLE), null);
			dboLabSample.setObject_type(SvReader.getTypeIdByName(Tc.LAB_SAMPLE));
			dboLabSample.setVal(Tc.DATE_OF_COLLECTION, "2005-05-05");
			dboLabSample.setVal(Tc.LABORATORY_NAME, "LAB_TEST_2");
			dboLabSample.setVal(Tc.DISEASE_TEST, "3");
			dboLabSample.setVal(Tc.SAMPLE_TYPE, "3");
			dboLabSample.setVal(Tc.SAMPLE_ORIGIN, "1");
			dboLabSample.setParent_id(dboAnimal.getObject_id());

			// dboLabSample.setParent_id(dboAnimal.getObject_id());
			svw.saveObject(dboLabSample);
			svw.dbCommit();
			log4j.debug(dboLabSample.toSimpleJson());
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
			// fail();
		} finally {
			if (svr != null) {
				svr.release();
			}
			if (svw != null) {
				svw.release();
			}
		}
	}
	// 2598292

	@Test
	public void testAssigningLaboratoryCustomWs() {
		SvReader svr = null;
		String token = null;
		Reader rdr = null;
		SvWriter svw = null;
		SvWorkflow sww = null;
		ValidationChecks vc = null;
		// 540752
		try {

			token = login("A.ADMIN3", "naits1234");
			svr = new SvReader(token);
			svw = new SvWriter(svr);
			sww = new SvWorkflow(svw);
			rdr = new Reader();
			vc = new ValidationChecks();
			Writer wr = new Writer();

			String pattern = Tc.DATE_PATTERN;

			DbDataObject labSampleObj = svr.getObjectById(2598122L, SvReader.getTypeIdByName(Tc.LAB_SAMPLE),
					new DateTime());
			labSampleObj.setVal("DISEASE_TEST", "54");
			String laboratoryName = "TEST_TEST_4";
			String result = wr.createLinkBetweenLaboratoryAndLabSampleByLabName(labSampleObj, laboratoryName, rdr, svr,
					svw);

			log4j.debug(result);
			// log4j.debug(dboLab.toJson());
			// log4j.debug(dboLabSample.toJson());

		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
			fail();
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
	}

	@Test
	public void testChangingStatusOfLabSample() {
		SvWriter svw = null;
		SvReader svr = null;
		Reader rdr = new Reader();
		DbDataObject dboLabSample = null;
		try {
			String token = login("I.BIRAJ", "naits1234");
			OnSaveValidations call = new OnSaveValidations();
			svr = new SvReader(token);
			svw = new SvWriter(svr);
			svw.dbSetAutoCommit(false);
			// DbDataObject dboAnimal =
			// rdr.findAppropriateAnimalByAnimalId("3126636", svr);
			dboLabSample = svr.getObjectById(3126519L, SvReader.getTypeIdByName(Tc.LAB_SAMPLE), null);
			dboLabSample.setStatus(Tc.QUEUED);
			// dboLabSample.setParent_id(dboAnimal.getObject_id());
			svw.saveObject(dboLabSample);
			svw.dbCommit();
			log4j.debug(dboLabSample.toSimpleJson());
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
			// fail();
		} finally {
			if (svw != null) {
				svw.release();
			}
		}
	}

	@Test
	public void testOrgUnit() throws Exception {
		SvWriter svw = null;
		SvReader svr = null;
		Reader rdr = new Reader();
		UserManager um = new UserManager();
		MassActions ma = new MassActions();
		try {
			String token = login("A.ADMIN3", "naits1234");
			svr = new SvReader(token);
			svw = new SvWriter(svr);
			svw.dbSetAutoCommit(false);

			// USER_OBJS
			DbDataObject user1 = svr.getObjectById(2595682L, svCONST.OBJECT_TYPE_USER, null);
			// DbDataObject user2 = svr.getObjectById(1233969L,
			// svCONST.OBJECT_TYPE_USER, null);
			// DbDataObject user3 = svr.getObjectById(220483L,
			// svCONST.OBJECT_TYPE_USER, null);

			// LAB_SAMPLES
			// DbDataObject labSample1 = svr.getObjectById(3128869L,
			// SvReader.getTypeIdByName(Tc.LAB_SAMPLE), null);

			JsonArray jArr = new JsonArray();

			jArr.add(user1.toSimpleJson());
			// jArr.add(user2.toSimpleJson());
			// jArr.add(user3.toSimpleJson());

			String pom = um.assignUserToObjectWithPoa(2518084L, jArr, svr.getSessionId());
			log4j.debug(pom);

			// String pom2 = ma.labSampleHandler("SET_HEALTH_STATUS_TO_RESULTS",
			// "NEGATIVE", "", jArr, svr.getSessionId());
			// log4j.debug(pom2);

			// svw.dbCommit();
		} catch (SvException e) {
			log4j.error("Error: ", e.getFormattedMessage());
			// fail();
		} finally {
			if (svw != null) {
				svw.dbRollback();
				svw.release();
			}
		}
	}

	@Test
	public void testCheckInINventoryItem() throws Exception {
		SvWriter svw = null;
		SvReader svr = null;
		Reader rdr = new Reader();
		ValidationChecks vc = new ValidationChecks();
		try {
			String token = login("A.ADMIN3", "naits1234");
			svr = new SvReader(token);
			svw = new SvWriter(svr);
			svw.dbSetAutoCommit(false);
			OnSaveValidations call = new OnSaveValidations();
			SvCore.registerOnSaveCallback(call, SvReader.getTypeIdByName("RANGE"));

			Boolean result = true;
			DbDataObject rangeObj = new DbDataObject();
			rangeObj.setObject_type(SvReader.getTypeIdByName(Tc.RANGE));
			rangeObj.setVal("TAG_TYPE", "1");
			rangeObj.setVal(Tc.START_TAG_ID, "1212111011");
			rangeObj.setVal(Tc.END_TAG_ID, "1212111012");
			rangeObj.setParent_id(3127356L);
			svw.saveObject(rangeObj);

			log4j.debug("SAVE DONE");

			DbDataObject dboRange = svr.getObjectById(1276430L, SvReader.getTypeIdByName(Tc.RANGE), null);
			DbDataObject dboOrder = svr.getObjectById(dboRange.getParent_id(), SvReader.getTypeIdByName(Tc.ORDER),
					null);
			DbDataObject dboOrgUnit = svr.getObjectById(dboOrder.getParent_id(), svCONST.OBJECT_TYPE_ORG_UNITS, null);

			// result = vc.checkIfOrgUnitContainsEarTagByRange(dboRange,
			// dboOrgUnit, svr);
			if (result.equals(true)) {
				log4j.debug("ITS TRUE");
			} else {
				log4j.debug("ITS FALSE");
			}

		} catch (SvException e) {
			log4j.error("Error: ", e.getFormattedMessage());
			// fail();
		} finally {
			if (svr != null) {
				svr.release();
			}
			if (svw != null) {
				svw.release();
			}
		}
	}

	@Test
	public void testValidTestTypes() throws Exception {
		SvWriter svw = null;
		SvReader svr = null;
		Reader rdr = new Reader();
		JsonArray jsonArr = new JsonArray();
		JSONArray bigArr = new JSONArray();
		Gson gson = new Gson();
		try {
			String token = login("A.ADMIN3", "naits1234");
			svr = new SvReader(token);
			DbDataArray arr1 = null;// rdr.getValidVaccEvents(svr);
			// DbDataArray arr2 = rdr.getValidTestTypes(3126620L, "0", svr);

			DbDataObject pom = arr1.get(0);
			DbDataArray vsvarogfieldsArray = SvCore.getRepoDbtFields();

			String pomTest = rdr.convertDbDataArrayToGridJson(arr1, Tc.LAB_TEST_RESULT);
			log4j.debug(pomTest);

			String temp = "{";
			int countFields = 0;
			for (Entry<String, JsonElement> entry : pom.toSimpleJson().entrySet()) {
				countFields++;
				temp += "\"" + "LAB_TEST_RESLT." + entry.getKey().toString().toUpperCase() + "\"" + ":"
						+ entry.getValue();
				if (countFields != pom.getValuesMap().size()) {
					temp += ",";
				} else {
					temp += "}";
				}
				log4j.debug(temp);
			}

			log4j.debug(arr1.toSimpleJson());
			log4j.debug(temp);
			log4j.debug(rdr.convertDbDataArrayToGridJson(arr1, "TEST_RESULT"));

			// jsonArr.add(arr1.getItems().toString());
			JsonObject jsonObj = new JsonObject();
			jsonObj = arr1.toJson();

			log4j.debug(jsonArr.getAsJsonArray());

			log4j.debug(jsonObj.toString());
			jsonObj.get("items:").getAsJsonArray();
			// log4j.debug(arr1.toSimpleJson().toString());
			// log4j.debug(arr2.toSimpleJson().toString());
			log4j.debug("OK");
		} catch (SvException e) {
			log4j.error("Error: ", e.getFormattedMessage());
			// fail();
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
	}
	// 3127475

	@Test
	public void testLabSampleHealthStatus() {
		SvReader svr = null;
		SvWriter svw = null;
		String token = null;
		Reader rdr = new Reader();
		Writer wr = new Writer();
		MassActions ma = new MassActions();
		// 540752
		try {
			String result = "";
			token = login("A.ADMIN3", "naits1234");
			svr = new SvReader(token);
			svw = new SvWriter(svr);
			svw.dbSetAutoCommit(false);
			OnSaveValidations call = new OnSaveValidations();
			SvCore.registerOnSaveCallback(call, SvReader.getTypeIdByName(Tc.LAB_SAMPLE));
			DbDataObject dboLabSample = svr.getObjectById(3126981L, SvReader.getTypeIdByName(Tc.LAB_SAMPLE), null);
			DbDataArray temp = new DbDataArray();
			temp.addDataItem(dboLabSample);
			log4j.debug(rdr.convertDbDataArrayToGridJson(temp, Tc.LAB_SAMPLE));
			String res = wr.setHealthStatusToLabSample(dboLabSample.getObject_id(), svr, svw);
			log4j.debug(res);
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
			fail();
		} finally {
			if (svr != null) {
				svr.release();
			}
			if (svw != null) {
				svw.release();
			}
		}
	}

	@Test
	public void testExportingAnimals() throws Exception {
		SvReader svr = null;
		String token = null;
		Reader rdr = new Reader();
		Writer wr = new Writer();
		SvWriter svw = null;
		MassActions ma = new MassActions();
		// 540752
		try {
			String result = "";
			token = login("A.ADMIN3", "naits1234");
			svr = new SvReader(token);
			svw = new SvWriter(svr);
			svw.dbSetAutoCommit(false);
			String pattern = Tc.DATE_PATTERN;
			OnSaveValidations call = new OnSaveValidations();
			SvCore.registerOnSaveCallback(call, SvReader.getTypeIdByName("ANIMAL"));
			SvCore.registerOnSaveCallback(call, SvReader.getTypeIdByName(Tc.EXPORT_CERT));
			// DbDataObject animalObj =
			// rdr.findAppropriateAnimalByAnimalId("10553148", svr);

			// DbDataObject animalObj1 =
			// rdr.findAppropriateAnimalByAnimalId("456646", svr);
			JsonArray jArr = new JsonArray();
			DbDataObject exportCertObj = svr.getObjectById(3128984L, SvReader.getTypeIdByName(Tc.EXPORT_CERT), null);

			DbDataObject linkAnimalExportCert = SvReader.getLinkType(Tc.ANIMAL_EXPORT_CERT,
					SvReader.getTypeIdByName(Tc.ANIMAL), SvReader.getTypeIdByName(Tc.EXPORT_CERT));
			DbDataArray exportedAnimals = svr.getObjectsByLinkedId(exportCertObj.getObject_id(),
					SvReader.getTypeIdByName(Tc.EXPORT_CERT), linkAnimalExportCert, SvReader.getTypeIdByName(Tc.ANIMAL),
					true, null, 0, 0);
			// check if all animals are exported to change
			// the status of the export_cert
			for (DbDataObject animalObjEc : exportedAnimals.getItems()) {
				jArr.add(animalObjEc.toSimpleJson());
			}

			// jArr.add(animalObj1.toSimpleJson());
			String res = ma.exportCertMassHandler(jArr, svr.getSessionId(), 3127067L);
			log4j.debug(res);
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
			fail();
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
	}

	@Test
	public void testGsonJsonResponse() throws Exception {
		SvReader svr = null;
		String token = null;
		Reader rdr = new Reader();
		Writer wr = new Writer();
		MassActions ma = new MassActions();
		// 540752
		try {
			String result = "";
			token = login("A.ADMIN3", "naits1234");
			svr = new SvReader(token);
			String pattern = Tc.DATE_PATTERN;
			DbDataObject animalObj = rdr.findAppropriateAnimalByAnimalId("5698745", svr);
			DbDataObject dboObjectToHandle = svr.getObjectById(112671L, SvReader.getTypeIdByName(Tc.ANIMAL), null);
			JsonObject jsonAnimObj = new JsonObject();
			jsonAnimObj = animalObj.toJson();
			log4j.debug(jsonAnimObj);
			// String res =
			// ma.exportCertMassHandler(jsonAnimObj.getAsJsonArray(),
			// svr.getSessionId());
			// log4j.debug(res);
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
			fail();
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
	}

	// TODO: MOVEMENT MODULE EXTENSION

	@Test
	public void testStartAnimalMovement() throws Exception {
		SvReader svr = null;
		String token = null;
		Reader rdr = new Reader();
		Writer wr = new Writer();
		MassActions ma = new MassActions();
		// 540752
		try {
			String result = "";
			token = login("A.ADMIN3", "naits1234");
			svr = new SvReader(token);
			OnSaveValidations call = new OnSaveValidations();
			SvCore.registerOnSaveCallback(call, SvReader.getTypeIdByName(Tc.VACCINATION_BOOK));
			SvCore.registerOnSaveCallback(call, SvReader.getTypeIdByName(Tc.ANIMAL));
			SvCore.registerOnSaveCallback(call, SvReader.getTypeIdByName(Tc.LAB_SAMPLE));
			// 12331233
			DbDataObject animalObj = rdr.findAppropriateAnimalByAnimalId("4943242452", svr);

			// 1313123000041
			JsonArray jsonArr = new JsonArray();
			jsonArr.add(animalObj.toSimpleJson());

			/*
			 * String res = ma.animalFlockMassHandler("ANIMAL", "ANIMAL_CHECKS",
			 * "PHYSICAL_CHECK", "2", "", "", "", "", "", "", "", "", "", jsonArr,
			 * svr.getSessionId()); log4j.debug(res);
			 */
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
			fail();
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
	}

	@Test
	public void testMvmDocBlock() throws Exception {
		SvReader svr = null;
		SvWriter svw = null;
		String token = null;
		Reader rdr = new Reader();
		Writer wr = new Writer();
		MassActions ma = new MassActions();
		// 540752
		try {
			String result = "";
			token = login("A.ADMIN3", "naits1234");
			svr = new SvReader(token);
			svw = new SvWriter(svr);
			svw.setAutoCommit(false);

			DbDataObject dboMvmDoc = svr.getObjectById(3226242L, SvReader.getTypeIdByName(Tc.MOVEMENT_DOC), null);

			String pom = ma.checkAnimalOrFlockMovementsInMovementDocument(dboMvmDoc, svr.getSessionId());

			DbDataObject holdingObj = rdr.findAppropriateHoldingByPic("1317126000068", svr);

			// log4j.debug(pom);

		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
			fail();
		} finally {
			if (svr != null) {
				svr.release();
			}
			if (svw != null) {
				svw.release();
			}
		}
	}

	@Test
	public void testCancelMovementAction() throws Exception {
		SvReader svr = null;
		String token = null;
		Reader rdr = new Reader();
		Writer wr = new Writer();
		MassActions ma = new MassActions();
		ApplicationServices aps = new ApplicationServices();
		ValidationChecks vc = new ValidationChecks();
		// 540752
		try {
			OnSaveValidations callback = new OnSaveValidations();
			SvCore.registerOnSaveCallback(callback, SvReader.getTypeIdByName(Tc.MOVEMENT_DOC));
			String result = "";
			token = login("A.ADMIN3", "naits1234");
			svr = new SvReader(token);
			// 12331233
			// DbDataObject animalObj =
			// rdr.findAppropriateAnimalByAnimalId("737373343", svr);
			DbDataObject movemDocObj = svr.getObjectById(21628210L, SvReader.getTypeIdByName(Tc.MOVEMENT_DOC), null);
			// 1313123000041
			JsonArray jsonArr = new JsonArray();
			jsonArr.add(movemDocObj.toSimpleJson());
			String res = ma.updateStatusOfMovementDoc(jsonArr, Tc.RELEASED, svr);
			svr.dbCommit();
			// Boolean pom =
			// vc.checkIfAnimalOrFlockBelongsToReleasedtMovementDoc(animalObj,
			// rdr, svr);
			log4j.debug(res);
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
			fail();
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
	}

	@Test
	public void testAfterSave() throws SvException, UnsupportedEncodingException {

		// log4j.debug(mvmDocObj.toSimpleJson());
		String url = "MD-1317123000025%2F008";
		String result = "";
		if (url.contains("%2F")) {
			result = java.net.URLDecoder.decode(url, StandardCharsets.UTF_8.name());
		}
		log4j.debug(result);
		ArrayList<String> pom = new ArrayList<>();
		pom.add("1");
		pom.add("2");
		pom.add("4");
		String help = pom.toString().replace("[", "").replace("]", "").replace(" ", "");
		String str2 = "";
		for (String str : pom) {
			switch (str) {
			case "2":
				log4j.debug("VLEZE");
				str2 = str;
				break;
			default:
				break;
			}
			if (!str2.equals(""))
				break;
		}

		log4j.debug(help);
	}

	@Test
	public void testGetTableWithLike() throws UnsupportedEncodingException {
		SvReader svr = null;
		SvSecurity svsec = null;
		try {
			svsec = new SvSecurity();
			String token = svsec.logon("L.JANEV", SvUtil.getMD5("naits1234"));

			// Long userId = 220493L; //l.janev
			svr = new SvReader(token);
			// WsReactElements rea = new WsReactElements();
			Object aaa = null;
			String baba = "";
			String fieldWithSpecialCharacter = "";
			String fieldValue = "MD-1317123000025%2F007";
			if (!fieldValue.equals("") && fieldValue.contains("%2F")) {
				fieldWithSpecialCharacter = java.net.URLDecoder.decode(fieldValue, StandardCharsets.UTF_8.name());
				if (!fieldWithSpecialCharacter.equals("")) {

				}
			}
			// Response responseHtml = rea.getTableWithLike(token,
			// "ANIMAL_MOVEMENT", "MOVEMENT_DOC_ID", fieldValue, 100,
			// null);
			// aaa = responseHtml.getEntity();
			// baba = aaa.toString();
			log4j.info(baba);
		} catch (SvException ex) {
			log4j.error(ex.getFormattedMessage());
			fail(ex.getFormattedMessage());
		} finally {
			// release
		}
	}

	@Test
	public void testAnimalExportCertLink() throws SvException, UnsupportedEncodingException {
		SvWriter svw = null;
		SvReader svr = null;
		Reader rdr = new Reader();
		try {
			String token = login("A.ADMIN3", "naits1234");
			OnSaveValidations call = new OnSaveValidations();
			SvCore.registerOnSaveCallback(call, SvReader.getTypeIdByName(Tc.MOVEMENT_DOC));
			svr = new SvReader(token);
			svw = new SvWriter(svr);
			svw.dbSetAutoCommit(false);

			// DbDataObject dboObjectToHandle = svr.getObjectById(object_id,
			// typeDescriptor, refDate);

			DbDataObject linkAnimalExportCert = SvLink.getLinkType("ANIMAL_EXPORT_CERT",
					SvReader.getTypeIdByName(Tc.ANIMAL), SvReader.getTypeIdByName(Tc.EXPORT_CERT));
			DbDataArray allItems = svr.getObjectsByLinkedId(213L, SvReader.getTypeIdByName(Tc.ANIMAL),
					linkAnimalExportCert, SvReader.getTypeIdByName(Tc.EXPORT_CERT), false, null, 0, 0);
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
			// fail();
		} finally {
			if (svr != null) {
				svr.release();
			}
			if (svw != null) {
				svw.release();
			}
		}
	}

	@Test
	public void testGetRepoData() throws Exception {
		SvReader svr = null;
		SvWriter svw = null;
		String token = null;
		Writer wr = new Writer();

		// 540752
		try {
			String result = "";
			token = login("A.ADMIN3", "naits1234");
			svr = new SvReader(token);
			svw = new SvWriter(svr);
			String pattern = Tc.DATE_PATTERN;
			svw.setAutoCommit(false);
			OnSaveValidations call = new OnSaveValidations();
			SvCore.registerOnSaveCallback(call, SvReader.getTypeIdByName(Tc.MOVEMENT_DOC));

			DbDataObject animalTypeDesc = SvReader.getDbtByName(Tc.LAB_SAMPLE);

			DbDataObject labSampleObj = svr.getObjectById(3128869L, SvReader.getTypeIdByName(Tc.LAB_SAMPLE), null);

			labSampleObj.setVal("TEST_RESULT_STATUS", "2");
			svw.saveObject(labSampleObj, false);

		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
			fail();
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
	}

	@Test
	public void testGeneratingLabSampleDependOnAnimalHealthBook() throws Exception {
		SvReader svr = null;
		SvWriter svw = null;
		SvLink svl = null;
		String token = null;
		Reader rdr = new Reader();
		Writer wr = new Writer();

		// 540752
		try {
			String result = "";
			token = login("A.ADMIN3", "naits1234");
			svr = new SvReader(token);
			svw = new SvWriter(svr);
			svl = new SvLink(svr);
			String pattern = Tc.DATE_PATTERN;
			svw.setAutoCommit(false);

			OnSaveValidations call = new OnSaveValidations();
			SvCore.registerOnSaveCallback(call, SvReader.getTypeIdByName(Tc.ANIMAL));
			SvCore.registerOnSaveCallback(call, SvReader.getTypeIdByName(Tc.ANIMAL_MOVEMENT));
			SvCore.registerOnSaveCallback(call, SvReader.getTypeIdByName(Tc.HOLDING));
			SvCore.registerOnSaveCallback(call, SvReader.getTypeIdByName(Tc.HOLDING_RESPONSIBLE));
			SvCore.registerOnSaveCallback(call, SvReader.getTypeIdByName(Tc.FLOCK));
			SvCore.registerOnSaveCallback(call, SvReader.getTypeIdByName(Tc.OTHER_ANIMALS));
			SvCore.registerOnSaveCallback(call, svCONST.OBJECT_TYPE_LINK);
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
			SvCore.registerOnSaveCallback(call, SvReader.getTypeIdByName(Tc.EAR_TAG_REPLC));
			SvCore.registerOnSaveCallback(call, SvReader.getTypeIdByName(Tc.LAB_SAMPLE));
			SvCore.registerOnSaveCallback(call, SvReader.getTypeIdByName(Tc.LABORATORY));
			SvCore.registerOnSaveCallback(call, SvReader.getTypeIdByName(Tc.LAB_TEST_TYPE));

			// create test VACC BOOK
			DbDataObject animalHealthBook = wr.createVaccinationBook("2000-01-01", "2", "", svw);
			svw.saveObject(animalHealthBook, false);

			// link it on some animal
			DbDataObject animalObj = rdr.findAppropriateAnimalByAnimalId("10421230", svr);

			// create link
			svl.linkObjects(animalObj, animalHealthBook, Tc.ANIMAL_VACC_BOOK, "unitTest", false);

			log4j.debug("ALMOST DONE");
			svw.dbCommit();
			log4j.debug("FINALLY DONE");
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
			fail();
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
	}

	@Test
	public void testDeletePrePostMortemForm() throws Exception {
		SvReader svr = null;
		SvWriter svw = null;
		SvLink svl = null;
		String token = null;
		Reader rdr = new Reader();
		Writer wr = new Writer();
		WSSvarog wsvg = new WSSvarog();
		// 540752
		try {
			token = login("A.ADMIN3", "naits1234");
			svr = new SvReader(token);
			svw = new SvWriter(svr);
			svl = new SvLink(svr);
			String pattern = Tc.DATE_PATTERN;
			svw.setAutoCommit(false);

			OnSaveValidations call = new OnSaveValidations();
			SvCore.registerOnSaveCallback(call, SvReader.getTypeIdByName(Tc.POST_SLAUGHT_FORM));
			SvCore.registerOnSaveCallback(call, SvReader.getTypeIdByName(Tc.PRE_SLAUGHT_FORM));

			Object coreObj = new Object();
			String result = "";

			// create test PRE_MORTEM_FORM

			/*
			 * DbDataObject preMortObj =
			 * svr.getObjectById(3129417L,SvReader.getTypeIdByName(Tc. PRE_SLAUGHT_FORM),
			 * null); preMortObj.setDt_delete(new DateTime()); svw.saveObject(preMortObj);
			 */

			DbDataObject preMortemForm = wr.createPreMortemForm("CLINIC_HEALTHY", "1", 3126080L);
			svw.saveObject(preMortemForm);
			svw.dbCommit();

			log4j.debug("PreMortem successfully saved. Object id: " + preMortemForm.getObject_id() + " PKID: "
					+ preMortemForm.getPkid());

			Response responseHtml = wsvg.deleteObjectWithSaveCheck(svr.getSessionId(), preMortemForm.getObject_id(),
					preMortemForm.getObject_type(), preMortemForm.getPkid(), null);

			coreObj = responseHtml.getEntity();
			result = coreObj.toString();
			log4j.debug("RESLULT: " + result);
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
			fail();
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
	}

	@Test
	public void testAnimalWithBlockingDisease() throws Exception {
		SvReader svr = null;
		SvWriter svw = null;
		SvLink svl = null;
		String token = null;
		Reader rdr = new Reader();
		Writer wr = new Writer();
		WSSvarog wsvg = new WSSvarog();
		// 540752
		try {
			token = login("A.ADMIN3", "naits1234");
			svr = new SvReader(token);
			svw = new SvWriter(svr);
			svl = new SvLink(svr);
			String pattern = Tc.DATE_PATTERN;
			svw.setAutoCommit(false);

			DbDataObject labSampleObject = rdr.getLabSampleBySampleId("101171230001", svr);
			String result = rdr.checkLabSampleDiseasesForMovementCheck(labSampleObject, svr);
			log4j.debug("RESLULT: " + result);
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
			fail();
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
	}

	@Test
	public void testAssignedLaboratories() throws Exception {
		SvReader svr = null;
		String token = null;
		Reader rdr = new Reader();
		// 540752
		try {
			token = login("O.LABORANT2", "naits1234");
			svr = new SvReader(token);
			String result = rdr.getLaboratoriesAssignedToUserBySessionId(svr);
			log4j.debug(result);
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
			fail();
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
	}

	@Test
	public void testCheckIfInconclusivLabSamplesHaveNewResult() throws Exception {
		SvReader svr = null;
		SvWriter svw = null;
		SvLink svl = null;
		String token = null;
		Reader rdr = new Reader();
		Writer wr = new Writer();
		WSSvarog wsvg = new WSSvarog();
		// 540752
		try {
			token = login("A.ADMIN3", "naits1234");
			svr = new SvReader(token);
			svw = new SvWriter(svr);
			svl = new SvLink(svr);
			String pattern = Tc.DATE_PATTERN;
			svw.setAutoCommit(false);
			DbDataObject animalObj = svr.getObjectById(2583001L, SvReader.getTypeIdByName(Tc.ANIMAL), null);

			Boolean result = rdr.checkifInconclusiveSamplesAreTestedAgainPerAnimal(animalObj, svr);

			log4j.debug(result);
			log4j.debug("ok");

		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
			fail();
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
	}

	@Test
	public void updateLaboratories() throws Exception {
		SvReader svr = null;
		SvWriter svw = null;
		SvLink svl = null;
		String token = null;
		Reader rdr = new Reader();
		Writer wr = new Writer();
		WSSvarog wsvg = new WSSvarog();
		// 540752
		try {
			token = login("A.ADMIN3", "naits1234");
			svr = new SvReader(token);
			svw = new SvWriter(svr);
			svl = new SvLink(svr);
			String pattern = Tc.DATE_PATTERN;
			svw.setAutoCommit(false);
			DbDataObject animalObj = svr.getObjectById(2583001L, SvReader.getTypeIdByName(Tc.ANIMAL), null);

			Boolean result = rdr.checkifInconclusiveSamplesAreTestedAgainPerAnimal(animalObj, svr);

			log4j.debug(result);
			log4j.debug("ok");

		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
			fail();
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
	}

	@Test
	public void updateDisease() throws Exception {
		SvReader svr = null;
		SvWriter svw = null;
		SvLink svl = null;
		String token = null;
		Reader rdr = new Reader();
		Writer wr = new Writer();
		// 540752
		try {
			token = login("A.ADMIN3", "naits1234");
			svr = new SvReader(token);
			svw = new SvWriter(svr);
			svl = new SvLink(svr);
			String pattern = Tc.DATE_PATTERN;
			svw.setAutoCommit(false);
			DbDataArray diseases = svr.getObjectsByTypeId(SvReader.getTypeIdByName(Tc.DISEASE), null, 0, 0);

			for (DbDataObject disease : diseases.getItems()) {
				if (disease.getVal("MOVEMENT_PERMISION") != null
						&& disease.getVal("MOVEMENT_PERMISION").toString().contains("permited")) {
					disease.setVal("MOVEMENT_PERMISION", "NO_PERM");
					svw.saveObject(disease);
				}
			}

			log4j.debug("ok");
			svw.dbCommit();
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
			fail();
		} finally {
			if (svr != null) {
				svr.release();
			}
			if (svw != null) {
				svw.release();
			}
		}
	}

	@Test
	public void testCampaignName() throws Exception {
		SvReader svr = null;
		SvWriter svw = null;
		String token = null;
		Writer wr = new Writer();

		// 540752
		try {
			String result = "";
			token = login("A.ADMIN3", "naits1234");
			svr = new SvReader(token);
			svw = new SvWriter(svr);
			String pattern = Tc.DATE_PATTERN;
			svw.setAutoCommit(false);
			/*
			 * DbDataObject vaccBook = svr.getObjectById(2584990L,
			 * SvReader.getTypeIdByName(Tc.VACCINATION_BOOK), null);
			 * wr.setVaccBookDependOnVaccEvent(vaccBook, svr, svw);
			 * svw.saveObject(vaccBook); svw.dbCommit();
			 * log4j.debug(vaccBook.toSimpleJson());
			 */
			DbDataObject dboObjectToHandle = svr.getObjectById(2594515L, SvReader.getTypeIdByName(Tc.ANIMAL), null);
			wr.changeStatus(dboObjectToHandle, Tc.PENDING_EX, svw);
			DbDataObject exportCertObj = null;
			DbDataArray allItems = null;
			DbDataArray exportedAnimals = null;
			int counter = 0;
			if (dboObjectToHandle != null && dboObjectToHandle.getStatus().equals(Tc.PENDING_EX)) {

				DbDataObject linkAnimalExportCert = SvLink.getLinkType("ANIMAL_EXPORT_CERT",
						SvReader.getTypeIdByName(Tc.ANIMAL), SvReader.getTypeIdByName(Tc.EXPORT_CERT));
				allItems = svr.getObjectsByLinkedId(dboObjectToHandle.getObject_id(),
						SvReader.getTypeIdByName(Tc.ANIMAL), linkAnimalExportCert,
						SvReader.getTypeIdByName(Tc.EXPORT_CERT), false, null, 0, 0);

				// get export_cert object
				if (allItems != null && allItems.size() == 1) {
					exportCertObj = allItems.get(0);
					exportedAnimals = svr.getObjectsByLinkedId(exportCertObj.getObject_id(),
							SvReader.getTypeIdByName(Tc.EXPORT_CERT), linkAnimalExportCert,
							SvReader.getTypeIdByName(Tc.ANIMAL), true, null, 0, 0);
					// check if all animals are exported to change the status of
					// the export cert
					for (DbDataObject animalObj : exportedAnimals.getItems()) {
						if (animalObj != null && animalObj.getStatus().equals(Tc.EXPORTED)) {
							counter++;
						}
					}
					if (counter == exportedAnimals.size()) {
						wr.changeStatus(exportCertObj, "PROCESSED", svw);
					}
					result = "naits.success.exportAnimal";
				}
			}
			svw.dbCommit();
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
			fail();
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
	}

	@Test
	public void testLabSampleApplicableCombination() {
		SvWriter svw = null;
		SvReader svr = null;
		Reader rdr = new Reader();
		ValidationChecks vc = new ValidationChecks();
		try {
			String token = login("A.ADMIN3", "naits1234");
			OnSaveValidations call = new OnSaveValidations();
			SvCore.registerOnSaveCallback(call, SvReader.getTypeIdByName(Tc.LAB_SAMPLE));
			svr = new SvReader(token);
			svw = new SvWriter(svr);
			svw.dbSetAutoCommit(false);

			DbDataObject dboLabSample = svr.getObjectById(3224359L, SvReader.getTypeIdByName(Tc.LAB_SAMPLE), null);

			svw.saveObject(dboLabSample);

		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
			// fail();
		} finally {
			if (svr != null) {
				svr.release();
			}
			if (svw != null) {
				svw.release();
			}
		}
	}

	@Test
	public void testIncrementDate() {

		SvWriter svw = null;
		SvReader svr = null;
		Reader rdr = new Reader();
		ValidationChecks vc = new ValidationChecks();
		Writer wr = new Writer();
		try {
			String token = login("A.ADMIN3", "naits1234");
			OnSaveValidations call = new OnSaveValidations();
			SvCore.registerOnSaveCallback(call, SvReader.getTypeIdByName(Tc.LAB_SAMPLE));
			svr = new SvReader(token);
			svw = new SvWriter(svr);
			svw.dbSetAutoCommit(false);

			DateTime time1 = new DateTime("2019-06-06");
			DateTime timePlusMonth = new DateTime(time1.plusDays(2));

			DbDataObject dboHolding = rdr.findAppropriateHoldingByPic("13172800022", svr);

			DbDataArray arr = rdr.getHoldingsByUnitId(svr, dboHolding.getVal(Tc.VILLAGE_CODE).toString());

			for (DbDataObject filterHoldingObj : arr.getItems()) {

			}

			log4j.debug(arr.size());
			log4j.debug(timePlusMonth.toString().substring(0, 10));
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
			// fail();
		} finally {
			if (svr != null) {
				svr.release();
			}
			if (svw != null) {
				svw.release();
			}
		}
	}

	@Test
	public void testSvParam() throws Exception {
		SvWriter svw = null;
		SvReader svr = null;
		Reader rdr = new Reader();
		MassActions massAct = new MassActions();
		try {
			String token = login("A.ADMIN3", "naits1234");
			OnSaveValidations call = new OnSaveValidations();
			SvCore.registerOnSaveCallback(call, SvReader.getTypeIdByName(Tc.RANGE));
			SvCore.registerOnSaveCallback(call, SvReader.getTypeIdByName(Tc.TRANSFER));
			svr = new SvReader(token);
			svw = new SvWriter(svr);
			svw.dbSetAutoCommit(false);

			DbDataObject dboTransfer = svr.getObjectById(2527794L, SvReader.getTypeIdByName(Tc.TRANSFER), null);

			DbDataObject dboTransfer2 = svr.getObjectById(2275812L, SvReader.getTypeIdByName(Tc.TRANSFER), null);

			JsonArray jArr = new JsonArray();

			jArr.add(dboTransfer.toSimpleJson());
			jArr.add(dboTransfer2.toSimpleJson());

			String pom = "";// massAct.createRangeForTransfer(jArr, token, "1",
							// 56L, 57L, null);

			log4j.debug(pom);

		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
			// fail();
		} finally {
			if (svr != null) {
				svr.release();
			}
			if (svw != null) {
				svw.release();
			}
		}
	}

	@Test
	public void testNewMvmDocBlock() throws Exception {
		SvWriter svw = null;
		SvReader svr = null;
		Reader rdr = new Reader();
		MassActions massAct = new MassActions();
		try {
			String token = login("A.ADMIN3", "naits1234");
			OnSaveValidations call = new OnSaveValidations();
			SvCore.registerOnSaveCallback(call, SvReader.getTypeIdByName(Tc.RANGE));
			SvCore.registerOnSaveCallback(call, SvReader.getTypeIdByName(Tc.TRANSFER));
			svr = new SvReader(token);
			svw = new SvWriter(svr);
			svw.dbSetAutoCommit(false);

			DbDataObject dboMvmDoc = svr.getObjectById(3226099L, SvReader.getTypeIdByName(Tc.MOVEMENT_DOC), null);

			String pom = massAct.checkAnimalOrFlockMovementsInMovementDocument(dboMvmDoc, svr.getSessionId());

			log4j.debug(pom);
			svw.dbCommit();
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
			// fail();
		} finally {
			if (svr != null) {
				svr.release();
			}
			if (svw != null) {
				svw.release();
			}
		}
	}

	@Test
	public void testPopulationModule() throws SvException {
		SvWriter svw = null;
		SvReader svr = null;
		Reader rdr = new Reader();
		try {
			String token = login("A.ADMIN3", "naits1234");
			OnSaveValidations call = new OnSaveValidations();
			SvCore.registerOnSaveCallback(call, SvReader.getTypeIdByName(Tc.MOVEMENT_DOC));
			svr = new SvReader(token);
			svw = new SvWriter(svr);
			svw.dbSetAutoCommit(false);

			DbDataObject dboPopulation = svr.getObjectById(2439281L, SvReader.getTypeIdByName(Tc.POPULATION), null);

			JsonArray jArr = new JsonArray();

			jArr.add(dboPopulation.toSimpleJson());

			Writer wr = new Writer();

			wr.changeStatus(dboPopulation.toSimpleJson(), Tc.DRAFT, svr);

		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
			// fail();
		} finally {
			if (svr != null) {
				svr.release();
			}
			if (svw != null) {
				svw.release();
			}
		}
	}

	@Test
	public void testGeneratePdf() throws SvException, IOException, ParseException {
		SvWriter svw = null;
		SvReader svr = null;
		Reader rdr = new Reader();
		try {
			String token = login("A.ADMIN3", "naits1234");
			svr = new SvReader(token);
			svw = new SvWriter(svr);
			svw.dbSetAutoCommit(false);

			DbDataObject mvmObjExist = rdr.getMovementDocBlockByMovementObjId(3225229L, 3225230L, svr);
			log4j.debug(mvmObjExist);

		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
			// fail();
		} finally {
			if (svr != null) {
				svr.release();
			}
			if (svw != null) {
				svw.release();
			}
		}
	}

	@Test
	public void testIncomingOutgoingCustomWs() throws SvException {
		SvWriter svw = null;
		SvReader svr = null;
		Reader rdr = new Reader();
		try {
			String token = login("A.ADMIN3", "naits1234");
			svr = new SvReader(token);
			svw = new SvWriter(svr);
			svw.dbSetAutoCommit(false);

			DbDataArray result = new DbDataArray();

			DbDataObject OrgUnitObj = svr.getObjectById(1275811L, svCONST.OBJECT_TYPE_ORG_UNITS, null);

			// income
			DbDataObject linkIncomeOrg = SvLink.getLinkType(Tc.INCOME_TRANSFER, SvReader.getTypeIdByName(Tc.TRANSFER),
					svCONST.OBJECT_TYPE_ORG_UNITS);
			// outcome
			DbDataObject linkOutGoing = SvLink.getLinkType(Tc.OUTCOME_TRANSFER, SvReader.getTypeIdByName(Tc.TRANSFER),
					svCONST.OBJECT_TYPE_ORG_UNITS);
			// linked income
			DbDataArray allItemsIncome = svr.getObjectsByLinkedId(OrgUnitObj.getObject_id(),
					svCONST.OBJECT_TYPE_ORG_UNITS, linkIncomeOrg, SvReader.getTypeIdByName(Tc.TRANSFER), true, null, 0,
					0);
			// linked outgoing
			DbDataArray allItemsOutcome = svr.getObjectsByLinkedId(OrgUnitObj.getObject_id(),
					svCONST.OBJECT_TYPE_ORG_UNITS, linkOutGoing, SvReader.getTypeIdByName(Tc.TRANSFER), true, null, 0,
					0);

			if (allItemsIncome != null && allItemsIncome.size() > 0) {
				for (DbDataObject incomeObj : allItemsIncome.getItems()) {
					incomeObj.setVal("TRANSFER_TYPE", "INCOME");
					result.addDataItem(incomeObj);
				}
			}
			if (allItemsOutcome != null && allItemsOutcome.size() > 0) {
				for (DbDataObject incomeObj : allItemsOutcome.getItems()) {
					incomeObj.setVal("TRANSFER_TYPE", "OUTCOME");
					result.addDataItem(incomeObj);
				}
			}

			String pom = rdr.convertDbDataArrayToGridJson(result, Tc.TRANSFER);

			log4j.debug(pom);

		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
			// fail();
		} finally {
			if (svr != null) {
				svr.release();
			}
			if (svw != null) {
				svw.release();
			}
		}
	}

	@Test
	public void testMoveToInventoryItem() throws Exception {
		// 78428 animal obj_id
		SvReader svr = null;
		SvWriter svw = null;
		String token = null;
		Reader rdr = new Reader();
		Writer wr = new Writer();
		MassActions ma = new MassActions();
		// 540752
		try {
			token = login("A.ADMIN3", "1234Naits1234");
			svr = new SvReader(token);
			svw = new SvWriter(svr);
			svw.setAutoCommit(false);

			DbDataObject dboTransfer;
			JsonArray jArr = new JsonArray();

			// 100.144 examples
			/*
			 * dboTransfer = svr.getObjectById(13310864L,
			 * SvReader.getTypeIdByName(Tc.TRANSFER), null);
			 * jArr.add(dboTransfer.toSimpleJson()); dboTransfer =
			 * svr.getObjectById(13310865L, SvReader.getTypeIdByName(Tc.TRANSFER), null);
			 * jArr.add(dboTransfer.toSimpleJson()); dboTransfer =
			 * svr.getObjectById(13310866L, SvReader.getTypeIdByName(Tc.TRANSFER), null);
			 * jArr.add(dboTransfer.toSimpleJson());
			 */

			// mepa test param
			// 12122593
			// 12158641
			// 12175440
			// 12189776
			// 12197266

			// dboTransfer = svr.getObjectById(12122593L,
			// SvReader.getTypeIdByName(Tc.TRANSFER), null);
			// jArr.add(dboTransfer.toSimpleJson());
			DbSearchCriterion dbc1 = new DbSearchCriterion(Tc.STATUS, DbCompareOperand.EQUAL, Tc.DRAFT);
			DbSearchCriterion dbc2 = new DbSearchCriterion(Tc.OBJECT_ID, DbCompareOperand.LESS, 13400000);
			DbSearchExpression dbSearchExp = new DbSearchExpression();
			DbDataArray allDraftTransfers = svr.getObjects(dbSearchExp.addDbSearchItem(dbc1),
					SvReader.getTypeIdByName(Tc.TRANSFER), null, 0, 0);

			int count = 0;
			// for (DbDataObject tempTransfer : allDraftTransfers.getItems()) {
			// jArr.add(tempTransfer.toSimpleJson());
			// count++;
			// if (count == 10) {
			// break;
			// }
			// }
			// dboTransfer = svr.getObjectById(12546281L,
			// SvReader.getTypeIdByName(Tc.TRANSFER), null);
			// jArr.add(dboTransfer.toSimpleJson());

			// String pom = wr.moveInventoryItem(jArr, svr, svw);

			String pom = wr.moveTransferRangeAndGenerateToInventoryItem(allDraftTransfers, svr.getSessionId());

			log4j.debug(pom);

			svw.dbCommit();

		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
			fail();
		} finally {
			if (svr != null) {
				svr.release();
			}
			if (svw != null) {
				svw.release();
			}
		}
	}

	@Test
	public void testBlockAnimalWhenFinishMovement() throws Exception {
		// 78428 animal obj_id
		SvReader svr = null;
		SvWriter svw = null;
		String token = null;
		Reader rdr = new Reader();
		MassActions ma = new MassActions();
		// 540752
		try {
			token = login("A.ADMIN3", "naits1234");
			svr = new SvReader(token);
			svw = new SvWriter(svr);
			svw.setAutoCommit(false);

			DbDataObject vHolding = svr.getObjectById(21623383L, SvReader.getTypeIdByName(Tc.HOLDING), null);

			DbDataObject linkHoldingPerson = SvLink.getLinkType(Tc.HOLDING_QUARANTINE,
					SvReader.getTypeIdByName(Tc.HOLDING), SvReader.getTypeIdByName(Tc.QUARANTINE));
			DbDataArray allItems = svr.getObjectsByLinkedId(70517L, SvReader.getTypeIdByName(Tc.HOLDING),
					linkHoldingPerson, SvReader.getTypeIdByName(Tc.QUARANTINE), false, null, 0, 0);

			DbDataObject expQuar = allItems.get(0);
			expQuar.setVal(Tc.DATE_FROM, "2019-04-04");
			expQuar.setVal(Tc.DATE_TO, "2019-06-06");
			svw.saveObject(expQuar);

			svw.dbCommit();

		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
			fail();
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
	}

	@Test
	public void testCreateIncomeOutcomeTransfer() throws Exception {
		// 78428 animal obj_id
		SvReader svr = null;
		SvWriter svw = null;
		String token = null;
		Reader rdr = new Reader();
		Writer wr = new Writer();
		MassActions ma = new MassActions();
		// 540752
		try {
			OnSaveValidations call = new OnSaveValidations();
			token = login("A.ADMIN3", "naits1234");
			svr = new SvReader(token);
			svw = new SvWriter(svr);
			SvCore.registerOnSaveCallback(call, SvReader.getTypeIdByName(Tc.TRANSFER));
			svw.setAutoCommit(false);

			Object aaa = null;
			String baba = "";
			ApplicationServices aps = new ApplicationServices();
			// Response responseHtml = aps.createIncomeOutcomeTransfer(token,
			// 1275813L, "1", "123", "124",
			// "INCOME", "HEADQUARTER", null, null, null, null, null);
			// aaa = responseHtml.getEntity();
			// baba = aaa.toString();
			log4j.info(baba);

		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
			fail();
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
	}

	@Test
	public void testApplicableScopeOnAnimalTypes() throws Exception {
		// 78428 animal obj_id
		SvReader svr = null;
		SvWriter svw = null;
		String token = null;
		Reader rdr = new Reader();
		Writer wr = new Writer();
		MassActions ma = new MassActions();
		ValidationChecks vc = new ValidationChecks();
		// 540752
		try {
			OnSaveValidations call = new OnSaveValidations();
			token = login("A.ADMIN3", "naits1234");
			svr = new SvReader(token);
			svw = new SvWriter(svr);

			DbDataObject vaccEvent = rdr.getVaccEventByName("Multiselect animal types", svr);

			DbDataObject animalObj = rdr.findAppropriateAnimalByAnimalId("3453885", svr);

			JsonObject jData = new JsonObject();

			log4j.debug(jData);

			List<String> test = rdr.getMultiSelectFieldValueAsList(vaccEvent, Tc.ANIMAL_TYPE);

			log4j.debug(test.get(0));

			if (vc.checkIfAnimalTypeScopeIsApplicableOnSelectedAnimal(test, animalObj)) {
				log4j.debug("OK");
			}

		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
			fail();
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
	}

	@Test
	public void testCreateTransferAsJsonData() throws Exception {
		// 78428 animal obj_id
		SvReader svr = null;
		SvWriter svw = null;
		String token = null;
		Reader rdr = new Reader();
		Writer wr = new Writer();
		MassActions ma = new MassActions();
		ValidationChecks vc = new ValidationChecks();
		// 540752
		try {
			OnSaveValidations call = new OnSaveValidations();
			token = login("A.ADMIN3", "naits1234");
			svr = new SvReader(token);
			svw = new SvWriter(svr);

			JsonObject jData = new JsonObject();
			jData.addProperty("objectId", 1275833L);
			jData.addProperty("startTagId", "123");
			jData.addProperty("endTagId", "124");
			jData.addProperty("tagType", "1");
			jData.addProperty("transferType", "INCOME");
			jData.addProperty("departurePlace", "");
			jData.addProperty("arrivalPlace", "munic.munic_code_2635");
			jData.addProperty("issuedByPerson", "");
			jData.addProperty("receivedByPerson", "");
			jData.addProperty("reason", "");
			jData.addProperty("quantity", "");

			String pom = ""; // wr.createIncomeOutcomeTransferObject(jData, "",
								// svw, svr);
			log4j.debug(pom);

		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
			fail();
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
	}

	@Test
	public void testLabTestResultFullFill() throws Exception {
		// 78428 animal obj_id
		SvReader svr = null;
		SvWriter svw = null;
		String token = null;
		Reader rdr = new Reader();
		Writer wr = new Writer();
		MassActions ma = new MassActions();
		ValidationChecks vc = new ValidationChecks();
		// 540752
		try {
			OnSaveValidations call = new OnSaveValidations();
			token = login("A.ADMIN3", "naits1234");
			svr = new SvReader(token);
			svw = new SvWriter(svr);
			svw.dbSetAutoCommit(false);

			ArrayList<Integer> test = new ArrayList<>();
			Integer[] testInt = new Integer[5];
			test.add(2);
			test.add(224);
			test.add(24);
			test.add(44);
			test.add(524);

			Integer[] reversedArr = new Integer[5];

			for (int i = 0; i < test.size(); i++) {
				testInt[i] = test.get(i);
				reversedArr[i] = test.get(i);
			}
			int k = 0;
			while (k != reversedArr.length - 1) {
				if (reversedArr.length - 1 % 2 == 0) {

				}
				if (reversedArr.length - 1 % 2 != 0) {

				}
			}

			for (int i = 0; i < reversedArr.length; i++) {
				log4j.debug(reversedArr[i]);
			}

			int i = 0;
			while (i != testInt.length - 1) {
				if (testInt[i] > testInt[i + 1]) {
					int temp = testInt[i + 1];
					testInt[i + 1] = testInt[i];
					testInt[i] = temp;
				}
				i++;
			}

		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
			fail();
		} finally {
			if (svr != null) {
				svr.release();
			}
			if (svw != null) {
				svw.release();
			}
		}
	}

	@Test
	public void testKeeperReplacement() throws Exception {
		// 78428 animal obj_id
		SvReader svr = null;
		SvWriter svw = null;
		String token = null;
		Reader rdr = new Reader();
		Writer wr = new Writer();
		MassActions ma = new MassActions();
		ValidationChecks vc = new ValidationChecks();
		// 540752
		try {
			OnSaveValidations call = new OnSaveValidations();
			SvCore.registerOnSaveCallback(call, SvReader.getTypeIdByName(Tc.HOLDING_RESP));
			SvCore.registerOnSaveCallback(call, (svCONST.OBJECT_TYPE_LINK));
			token = login("A.ADMIN3", "naits1234");
			svr = new SvReader(token);
			svw = new SvWriter(svr);
			svw.setAutoCommit(false);

			DbDataObject dboHoldResp = svr.getObjectById(40066L, SvReader.getTypeIdByName(Tc.HOLDING_RESPONSIBLE),
					null);

			DbDataObject dboHolding = rdr.findAppropriateHoldingByPic("1313123000065", svr);

			wr.linkObjects(dboHolding, dboHoldResp, Tc.HOLDING_KEEPER, "test-daut", svr);

			svw.dbCommit();

		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
			fail();
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
	}

	@Test
	public void testCerifingAnimals() throws Exception {
		// 78428 animal obj_id
		SvReader svr = null;
		SvWriter svw = null;
		String token = null;
		Reader rdr = new Reader();
		Writer wr = new Writer();
		MassActions ma = new MassActions();
		ValidationChecks vc = new ValidationChecks();
		// 540752
		try {
			token = login("A.ADMIN3", "naits1234");
			svr = new SvReader(token);
			svw = new SvWriter(svr);
			svw.setAutoCommit(false);

			DbDataObject dboAnim = rdr.findAppropriateAnimalByAnimalId("10126536", svr);
			DbDataObject dboAnim2 = rdr.findAppropriateAnimalByAnimalId("10126537", svr);

			JsonArray jArr = new JsonArray();

			jArr.add(dboAnim.toSimpleJson());
			jArr.add(dboAnim2.toSimpleJson());

			String pom = ma.moveAndLinkAnimals("PENDINGEXPORT", "21625465", jArr, svr.getSessionId());

			log4j.debug(pom);

		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
			fail();
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
	}

	@Test
	public void testPreventionOfCreatingQuarantineWhenPendingMvmExist() throws Exception {
		// 78428 animal obj_id
		SvReader svr = null;
		SvWriter svw = null;
		String token = null;
		Reader rdr = new Reader();
		Writer wr = new Writer();
		MassActions ma = new MassActions();
		ValidationChecks vc = new ValidationChecks();
		// 540752
		try {
			// OnSaveValidations call = new OnSaveValidations();
			// SvCore.registerOnSaveCallback(call,
			// SvReader.getTypeIdByName(Tc.QUARANTINE));
			// SvCore.registerOnSaveCallback(call, (svCONST.OBJECT_TYPE_LINK));
			token = login("A.ADMIN3", "naits1234");
			svr = new SvReader(token);
			svw = new SvWriter(svr);
			svw.setAutoCommit(false);

			DbDataObject dboAnim = rdr.findAppropriateHoldingByPic("1313123000072", svr);
			DbDataObject quarantineObj = new DbDataObject();
			quarantineObj.setObject_type(SvReader.getTypeIdByName(Tc.QUARANTINE));
			quarantineObj.setVal(Tc.DATE_FROM, new DateTime());
			quarantineObj.setVal(Tc.DATE_TO, new DateTime());
			quarantineObj.setVal(Tc.QUARANTINE_TYPE, "1");
			svw.saveObject(quarantineObj, false);

			wr.linkObjects(dboAnim, quarantineObj, Tc.HOLDING_QUARANTINE, "test", svr);

			svw.dbCommit();
			// JsonArray jArr = new JsonArray();

			// jArr.add(dboAnim.toSimpleJson());
			// jArr.add(dboAnim2.toSimpleJson());

			log4j.debug("DONE");

		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
			fail();
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
	}

	@Test
	public void test_mest() throws Exception {
		// 78428 animal obj_id
		SvReader svr = null;
		SvWriter svw = null;
		String token = null;
		Reader rdr = new Reader();
		Writer wr = new Writer();
		MassActions ma = new MassActions();
		ValidationChecks vc = new ValidationChecks();
		// 540752
		try {
			OnSaveValidations call = new OnSaveValidations();
			SvCore.registerOnSaveCallback(call, SvReader.getTypeIdByName(Tc.QUARANTINE));
			SvCore.registerOnSaveCallback(call, (svCONST.OBJECT_TYPE_LINK));
			token = login("A.ADMIN3", "naits1234");
			svr = new SvReader(token);
			svw = new SvWriter(svr);
			svw.setAutoCommit(false);

			DbDataObject dboAnim = rdr.findAppropriateHoldingByPic("13131400123", svr);
			DbDataObject quarantineObj = new DbDataObject();
			quarantineObj.setObject_type(SvReader.getTypeIdByName(Tc.QUARANTINE));
			quarantineObj.setVal(Tc.DATE_FROM, new DateTime());
			quarantineObj.setVal(Tc.DATE_TO, new DateTime());
			quarantineObj.setVal(Tc.QUARANTINE_TYPE, "1");
			svw.saveObject(quarantineObj, false);

			wr.linkObjects(dboAnim, quarantineObj, Tc.HOLDING_QUARANTINE, "test", svr);

			// JsonArray jArr = new JsonArray();

			// jArr.add(dboAnim.toSimpleJson());
			// jArr.add(dboAnim2.toSimpleJson());

			log4j.debug("DONE");

		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
			fail();
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
	}

	@Test
	public void testPreOrPostSlaughter() throws Exception {
		// 78428 animal obj_id
		SvReader svr = null;
		SvWriter svw = null;
		String token = null;
		Reader rdr = new Reader();
		Writer wr = new Writer();
		MassActions ma = new MassActions();
		// 540752
		try {
			token = login("A.ADMIN3", "naits1234");
			svr = new SvReader(token);
			svw = new SvWriter(svr);
			svw.setAutoCommit(false);
			OnSaveValidations call = new OnSaveValidations();
			SvCore.registerOnSaveCallback(call, SvReader.getTypeIdByName(Tc.POST_SLAUGHT_FORM));
			// DbDataObject dboTransfer = svr.getObjectById(21622977L,
			// SvReader.getTypeIdByName(Tc.ANIMAL_MOVEMENT), null);
			DbDataObject dboAnimal = rdr.findAppropriateAnimalByAnimalId("73838", svr);
			dboAnimal.setStatus(Tc.SLAUGHTRD);
			svw.saveObject(dboAnimal);

			DbDataObject preSlaught = wr.createPostMortem(null, "2", "1", "1", dboAnimal.getObject_id());
			svw.saveObject(preSlaught, false);
			svw.dbCommit();
			log4j.debug("OK");

		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
			fail();
		} finally {
			if (svr != null) {
				svr.release();
			}
			if (svw != null) {
				svw.release();
			}
		}
	}

	@Test
	public void testDetachUserGroup() throws Exception {
		// 78428 animal obj_id
		SvReader svr = null;
		SvWriter svw = null;
		String token = null;
		Reader rdr = new Reader();
		Writer wr = new Writer();
		MassActions ma = new MassActions();
		UserManager um = new UserManager();
		// 540752
		try {
			token = login("A.ADMIN3", "naits1234");
			svr = new SvReader(token);
			svw = new SvWriter(svr);
			svw.setAutoCommit(false);

			DbDataObject dboUser = svr.getObjectById(21627169L, svCONST.OBJECT_TYPE_USER, null);

			JsonElement dboUserJson = dboUser.toSimpleJson();

			JsonArray jArr = new JsonArray();

			jArr.add(dboUserJson);

			um.attachUserToGroup("CVIRO", jArr, svr.getSessionId());

			log4j.debug("");

		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
			fail();
		} finally {
			if (svr != null) {
				svr.release();
			}
			if (svw != null) {
				svw.release();
			}
		}
	}

	@Test
	public void testDautakisa() throws SvException {
		SvReader svr = null;
		SvWriter svw = null;
		SvSecurity svs = null;
		String token = null;
		Reader rdr = new Reader();
		Writer wr = new Writer();
		MassActions ma = new MassActions();
		UserManager um = new UserManager();
		// 540752
		try {
			token = login("A.ADMIN3", "naits1234");
			svr = new SvReader(token);
			svw = new SvWriter(svr);
			svw.setAutoCommit(false);
			svs = new SvSecurity(svw);

			UserManager userManager = new UserManager();
			DbDataObject dboUser = userManager.createDefaultTestUser("K.RIKI", "KIKI", "RIKI", "k.riki@com.mk", "0000",
					"naits1234", svr);
			DbDataObject dboGroup = userManager.findUserOrUserGroup(svCONST.OBJECT_TYPE_GROUP, "GROUP_NAME",
					"HOLDING_REGISTRATORS", svr);
			svw.saveObject(dboUser);
			if (dboGroup == null) {
				throw (new SvException("naits.error.userGroupDoesNotExist", svr.getInstanceUser()));
			}
			svw.dbCommit();
			svs.addUserToGroup(dboUser, dboGroup, true);
			ArrayList<String> mandatoryPermissions = new ArrayList<String>();
			mandatoryPermissions.add("system.null_geometry");
			um.setSidPermission(dboUser.getVal("USER_NAME").toString(), svCONST.OBJECT_TYPE_USER, mandatoryPermissions,
					"GRANT", svr.getSessionId());
			svw.dbCommit();

		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
			fail();
		} finally {
			if (svr != null) {
				svr.release();
			}
			if (svw != null) {
				svw.release();
			}
		}
	}

	@Test
	public void testStringReplacement() throws SvException {
		String user_name = "L-JANEV";
		if (user_name != null && !user_name.equals("") && user_name.contains("-")) {
			String userName = user_name.replace("-", ".");
			log4j.debug(user_name.replace("-", ".").toString());
		}
	}

	@Test
	public void testPackagePermissions() throws Exception {
		SvReader svr = null;
		String token = null;
		Reader rdr = new Reader();
		Writer wr = new Writer();
		MassActions ma = new MassActions();
		UserManager um = new UserManager();
		// 540752
		try {
			token = login("R.PEJOV", "naits1234");
			svr = new SvReader(token);
			// DbDataObject user = svr.getObjectById(21627206L,
			// svCONST.OBJECT_TYPE_USER, null);

			// JsonArray jArr = new JsonArray();
			// jArr.add(user.toSimpleJson());
			// um.assignOrUnasignPackagePermissions("HOLDING_REGISTRATOR_PCK",
			// "ASSIGN", jArr, token);

			// packages to update:
			// "HOLDING_ADMINISTRATORS" FULL_HOLDING_ADMINISTRATOR_PCK
			// "QUARANTINE_ADMINISTRATORS" QUARANTINE_ADMINISTRATOR_PCK
			// "RYSK_ANALYZE_ADMINISTRATORS" RYSK_ANALYZE_ADMINISTRATOR_PCK
			// "FVIRO" FVIRO_PCK
			// "CVIRO" CVIRO_PCK
			DbSearchCriterion cr1 = new DbSearchCriterion(Tc.GROUP_NAME, DbCompareOperand.EQUAL,
					"HOLDING_ADMINISTRATORS");
			DbDataObject slaughtAdminGroup = svr
					.getObjects(new DbSearchExpression().addDbSearchItem(cr1), svCONST.OBJECT_TYPE_GROUP, null, 0, 0)
					.get(0);
			DbDataObject dbLink = SvReader.getLinkType(Tc.USER_GROUP, svCONST.OBJECT_TYPE_USER,
					svCONST.OBJECT_TYPE_GROUP);
			DbDataArray users = svr.getObjectsByLinkedId(slaughtAdminGroup.getObject_id(),
					slaughtAdminGroup.getObject_type(), dbLink, svCONST.OBJECT_TYPE_USER, true, null, 0, 0);
			log4j.debug(users.size());
			Thread.sleep(5000);
			String temp = "";
			for (DbDataObject dboUser : users.getItems()) {
				temp = assignOrUnasignPackagePermissions("FULL_HOLDING_ADMINISTRATOR_PCK", "ASSIGN",
						dboUser.getObject_id(), svr.getSessionId());
				log4j.debug(temp);
			}
			// log4j.debug("");
			// log4j.debug(um.getCurrentUserPermissions("L.LENOVOVSKI",
			// svr).toString());
			// log4j.debug(um.getCustomPermissionForUserOrGroup(user,
			// svr).toString());
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
			fail();
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
	}

	@Test
	public void testPackagePermissionsDetails() throws Exception {
		SvReader svr = null;
		String token = null;
		Reader rdr = new Reader();
		Writer wr = new Writer();
		MassActions ma = new MassActions();
		UserManager um = new UserManager();
		// 540752
		try {
			token = login("A.ADMIN3", "naits1234");
			svr = new SvReader(token);
			String languageId = svr.getSessionLocale(token).getVal("LOCALE_ID").toString();
			log4j.debug(languageId);
			Object aaa = null;
			String baba = "";
			ApplicationServices aps = new ApplicationServices();
			Response resp = aps.getDynamicDropdown(token, "USER_PACKAGE_PERMISSIONS");
			aaa = resp.getEntity();
			baba = aaa.toString();
			log4j.debug(baba);
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
			fail();
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
	}

	@Test
	public void testLocaleTransaltion() throws SvException {
		SvReader svr = null;
		String token = null;
		Reader rdr = new Reader();
		Writer wr = new Writer();
		MassActions ma = new MassActions();
		UserManager um = new UserManager();
		// 540752
		try {
			token = login("A.ADMIN3", "naits1234");
			svr = new SvReader(token);
			String languageId = svr.getSessionLocale(token).getVal("LOCALE_ID").toString();
			DbDataObject animalFound = rdr.findAppropriateAnimalByAnimalId("46345635", svr);
			String animalGender = animalFound.getVal(Tc.GENDER) != null ? rdr.decodeCodeValue(
					animalFound.getObject_type(), Tc.GENDER, animalFound.getVal(Tc.GENDER).toString(), languageId, svr)
					: "";
			String animalBreed = animalFound.getVal(Tc.ANIMAL_RACE) != null
					? rdr.decodeCodeValue(animalFound.getObject_type(), Tc.ANIMAL_RACE,
							animalFound.getVal(Tc.ANIMAL_RACE).toString(), languageId, svr)
					: "";
			String animalColor = animalFound.getVal(Tc.COLOR) != null ? rdr.decodeCodeValue(
					animalFound.getObject_type(), Tc.COLOR, animalFound.getVal(Tc.COLOR).toString(), languageId, svr)
					: "";
			DbDataObject dboSourceHolding = svr.getObjectById(animalFound.getParent_id(),
					SvReader.getTypeIdByName(Tc.HOLDING), null);
			String sourceHoldingInfo = I18n.getText(languageId, "naits.main.search.by_holding_name") + ": "
					+ dboSourceHolding.getVal(Tc.NAME).toString() + ", " + I18n.getText(languageId, "naits.holding.pic")
					+ " " + dboSourceHolding.getVal(Tc.PIC).toString();

			String animalObjInfo = I18n.getText(languageId, "animal.gender") + ": " + animalGender + ", "
					+ I18n.getText(languageId, "animal.animal_race") + ": " + animalBreed + ", "
					+ I18n.getText(languageId, "animal.color") + ": " + animalColor;

			String test = rdr.getAnimalAndSourceHoldingObjInfo(animalFound, languageId, svr, Tc.GENDER, Tc.ANIMAL_RACE,
					Tc.COLOR);

			log4j.debug(sourceHoldingInfo);
			log4j.debug(animalObjInfo);
			log4j.debug("TESTOT:" + test);
			// log4j.debug();

		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
			fail();
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
	}

	public DbDataObject createInvoice(String actName, Long pricePerHold, Double pricePerAct, Long pricePerDoc) {
		DbDataObject invoiceObj = new DbDataObject();
		invoiceObj.setObject_type(SvReader.getTypeIdByName("INVOICE_PRICE"));
		invoiceObj.setVal("ACT_NAME", actName);
		invoiceObj.setVal("PRICE_PER_HOLDING", pricePerHold);
		invoiceObj.setVal("PRICE_PER_ACT", pricePerAct);
		invoiceObj.setVal("PRICE_PER_DOC", pricePerDoc);
		return invoiceObj;
	}

	@Test
	public void testCreateInvoice() throws SvException {
		SvReader svr = null;
		SvWriter svw = null;
		String token = null;
		Reader rdr = new Reader();
		Writer wr = new Writer();
		MassActions ma = new MassActions();
		UserManager um = new UserManager();
		// 540752
		try {
			token = login("A.ADMIN3", "naits1234");
			svr = new SvReader(token);
			svw = new SvWriter(svr);
			svw.setAutoCommit(false);
			ArrayList<String> pom = new ArrayList<>();
			pom.add("1,0,0.5,0");
			pom.add("2,0,0.5,0");
			pom.add("9,0,5,0");
			pom.add("10,0,2,0");
			pom.add("3,0,0.5,0");
			pom.add("4,0,0.2,0");
			pom.add("11,1,0.05,0");
			pom.add("5,0,0,0");
			pom.add("6,0,0,0");
			pom.add("7,1,0,0");
			pom.add("8,1,0,0");

			for (String s : pom) {
				ArrayList<String> multiSelectResult = new ArrayList<String>(Arrays.asList(s.split(",")));
				String actName = multiSelectResult.get(0);
				Long priceHold = Long.valueOf(multiSelectResult.get(1));
				Double priceAct = Double.valueOf(multiSelectResult.get(2));
				Long priceDoc = Long.valueOf(multiSelectResult.get(3));
				DbDataObject inObj = createInvoice(actName, priceHold, priceAct, priceDoc);
				svw.saveObject(inObj, false);
			}

			// log4j.debug();
			svw.dbCommit();
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
			fail();
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
	}

	@Test
	public void testSortInArrayList() throws Exception {
		ArrayList<String> test = new ArrayList<>();
		test.add("Daut");
		String[] pom = test.get(0).split(",");
		log4j.debug(pom[0]);
	}

	@Test
	public void testAnimalMovementOnEdit() throws SvException {
		SvReader svr = null;
		SvWriter svw = null;
		String token = null;
		Reader rdr = new Reader();
		Writer wr = new Writer();
		MassActions ma = new MassActions();
		UserManager um = new UserManager();
		// 540752
		try {
			OnSaveValidations call = new OnSaveValidations();
			SvCore.registerOnSaveCallback(call, SvReader.getTypeIdByName(Tc.ANIMAL_MOVEMENT));
			token = login("A.ADMIN3", "naits1234");
			svr = new SvReader(token);
			svw = new SvWriter(svr);
			svw.setAutoCommit(false);

			DbDataObject movementObj = svr.getObjectById(21627234L, SvReader.getTypeIdByName(Tc.ANIMAL_MOVEMENT), null);
			movementObj.setVal(Tc.VET_OFFICER, "VET_OFFICER_3");
			svw.saveObject(movementObj, false);
			log4j.debug("SAVE");
			svw.dbCommit();
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
			fail();
		} finally {
			if (svr != null) {
				svr.release();
			}
			if (svw != null) {
				svw.release();
			}
		}
	}

	@Test
	public void testHerderActivity() throws SvException {
		SvReader svr = null;
		SvWriter svw = null;
		String token = null;
		Reader rdr = new Reader();
		Writer wr = new Writer();
		MassActions ma = new MassActions();
		UserManager um = new UserManager();
		// 540752
		try {
			OnSaveValidations call = new OnSaveValidations();
			SvCore.registerOnSaveCallback(call, SvReader.getTypeIdByName(Tc.ANIMAL_MOVEMENT));
			token = login("A.ADMIN3", "naits1234");
			svr = new SvReader(token);
			svw = new SvWriter(svr);
			svw.setAutoCommit(false);

			DbDataObject dboHoldingResp = svr.getObjectById(488425L, SvReader.getTypeIdByName(Tc.HOLDING_RESPONSIBLE),
					null);

			JsonArray jArr = new JsonArray();
			jArr.add(dboHoldingResp.toSimpleJson());

			// String result = ma.createActivityPeriod(72149L, "2019-01-01",
			// "2019-11-08", jArr, token);

		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
			fail();
		} finally {
			if (svr != null) {
				svr.release();
			}
			if (svw != null) {
				svw.release();
			}
		}
	}

	public DbDataObject createParamType(String labelCode, Boolean autoCommit, SvWriter svw) throws SvException {
		DbDataObject paramTypeObj = null;
		try {
			paramTypeObj = new DbDataObject();
			paramTypeObj.setObject_type(svCONST.OBJECT_TYPE_PARAM_TYPE);
			paramTypeObj.setStatus("VALID");
			paramTypeObj.setParent_id(0L);
			paramTypeObj.setDt_insert(new DateTime());
			paramTypeObj.setDt_delete(svCONST.MAX_DATE);
			paramTypeObj.setVal("LABEL_CODE", labelCode);
			paramTypeObj.setVal("DATA_TYPE", "NVARCHAR");
			paramTypeObj.setVal("INPUT_TYPE", "TEXT_AREA");
			svw.saveObject(paramTypeObj, autoCommit);
		} finally {
			if (svw != null)
				svw.release();
		}

		return paramTypeObj;
	}

	@Test
	public void testParamTypeParamParamVal() throws SvException {
		SvReader svr = null;
		SvWriter svw = null;
		String token = null;
		SvParameter svp = null;
		Reader rdr = new Reader();
		Writer wr = new Writer();
		MassActions ma = new MassActions();
		UserManager um = new UserManager();
		// 540752
		try {
			token = login("A.ADMIN3", "naits1234");
			svr = new SvReader(token);
			svw = new SvWriter(svr);
			svp = new SvParameter(svr);
			svw.setAutoCommit(false);
			DbDataObject dboHoldingResp = svr.getObjectById(488425L, SvReader.getTypeIdByName(Tc.HOLDING_RESPONSIBLE),
					null);
			DbDataObject dboHolding = svr.getObjectById(72149L, SvReader.getTypeIdByName(Tc.HOLDING), null);
			DbDataObject dboLink = rdr.getLinkObjectBetweenTwoLinkedObjects(dboHolding, dboHoldingResp,
					Tc.HOLDING_HERDER, svr);

			/*
			 * DbDataObject svParamType = svr.getObjectById(21627951L,
			 * svCONST.OBJECT_TYPE_PARAM_TYPE, null); svParamType.setVal(Tc.LABEL_CODE,
			 * "param.activity_from"); svw.saveObject(svParamType, false);
			 */
			createParamType("param.pet_archive_id", false, svw);

			/*
			 * DateTime dateTo = new DateTime("2019-07-11"); svp.setParamDateTime(dboLink,
			 * "param.activity_to", dateTo);
			 */
			svw.dbCommit();
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
			fail();
		} finally {
			if (svr != null) {
				svr.release();
			}
			if (svw != null) {
				svw.release();
			}
		}
	}

	@Test
	public void testHoldingSummaryInfo() throws SvException {
		SvReader svr = null;
		String token = null;
		Reader rdr = new Reader();
		Writer wr = new Writer();
		MassActions ma = new MassActions();
		UserManager um = new UserManager();
		// 540752
		try {
			token = login("A.ADMIN3", "naits1234");
			svr = new SvReader(token);
			String result = "";
			DbDataObject dboHolding = rdr.findAppropriateHoldingByPic("1317123000037", svr);
			// DbDataObject dboHolding = svr.getObjectById(72149L,
			// SvReader.getTypeIdByName(Tc.HOLDING), null);
			LinkedHashMap<String, String> jsonOrderedMap = new LinkedHashMap<String, String>();
			JSONObject jsonObj = new JSONObject();
			jsonOrderedMap = rdr.generateHoldingSummaryInformation(dboHolding, svr);
			jsonObj.put("orderedItems", jsonOrderedMap);
			result = jsonObj.toJSONString();
			log4j.debug(result);
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
			fail();
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
	}

	@Test
	public void testCreateFlock() throws SvException {
		SvReader svr = null;
		SvWriter svw = null;
		String token = null;
		Reader rdr = new Reader();
		Writer wr = new Writer();
		MassActions ma = new MassActions();
		UserManager um = new UserManager();
		// 540752
		try {
			OnSaveValidations onSave = new OnSaveValidations();
			SvCore.registerOnSaveCallback(onSave, SvReader.getTypeIdByName(Tc.FLOCK));
			token = login("A.ADMIN3", "naits1234");
			svr = new SvReader(token);
			svw = new SvWriter(svr);
			svw.setAutoCommit(false);

			DbDataObject dboFlock = wr.createFlockObject("3", "1", 30L, null, null, null,
					new DateTime().toString().substring(0, 9), null, 73377L);
			svw.saveObject(dboFlock, false);
			svw.dbCommit();

		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
			fail();
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
	}

	@Test
	public void testRecipient() throws SvException {
		SvReader svr = null;
		SvWriter svw = null;
		String token = null;
		Reader rdr = new Reader();
		Writer wr = new Writer();
		MassActions ma = new MassActions();
		UserManager um = new UserManager();
		// 540752
		try {
			OnSaveValidations onSave = new OnSaveValidations();
			SvCore.registerOnSaveCallback(onSave, SvReader.getTypeIdByName(Tc.MOVEMENT_DOC));
			token = login("A.ADMIN3", "naits1234");
			svr = new SvReader(token);
			svw = new SvWriter(svr);
			svw.setAutoCommit(false);

			DbDataObject mvmDoc = svr.getObjectById(21628471L, SvReader.getTypeIdByName(Tc.MOVEMENT_DOC), null);
			mvmDoc.setVal(Tc.RECIPIENT_USER, svr.getInstanceUser().getVal(Tc.USER_NAME));
			svw.saveObject(mvmDoc, false);
			log4j.debug(mvmDoc.toSimpleJson());
			svw.dbCommit();

		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
			fail();
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
	}

	@Test
	public void testReleaseMvmDoc() throws SvException {
		SvReader svr = null;
		SvWriter svw = null;
		String token = null;
		Reader rdr = new Reader();
		Writer wr = new Writer();
		MassActions ma = new MassActions();
		UserManager um = new UserManager();
		// 540752
		try {
			OnSaveValidations onSave = new OnSaveValidations();
			SvCore.registerOnSaveCallback(onSave, SvReader.getTypeIdByName(Tc.MOVEMENT_DOC));
			token = login("A.ADMIN3", "naits1234");
			svr = new SvReader(token);
			svw = new SvWriter(svr);
			svw.setAutoCommit(false);

			DbDataObject mvmDoc = svr.getObjectById(21628429L, SvReader.getTypeIdByName(Tc.MOVEMENT_DOC), null);

			JsonArray jArr = new JsonArray();
			jArr.add(mvmDoc.toSimpleJson());

			String pom = ma.updateStatusOfMovementDoc(jArr, Tc.RELEASED, svr);

			log4j.debug(pom);
			svw.dbCommit();

		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
			fail();
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
	}

	@Test
	public void testLinkBetweenUserAndHolding() throws SvException {
		SvReader svr = null;
		SvWriter svw = null;
		String token = null;
		Reader rdr = new Reader();
		Writer wr = new Writer();
		SvLink svLink = null;
		MassActions ma = new MassActions();
		UserManager um = new UserManager();
		// 540752
		try {
			OnSaveValidations onSave = new OnSaveValidations();
			SvCore.registerOnSaveCallback(onSave, SvReader.getTypeIdByName(Tc.MOVEMENT_DOC));
			token = login("A.ADMIN3", "naits1234");
			svr = new SvReader(token);
			svw = new SvWriter(svr);
			svLink = new SvLink(svw);
			svw.setAutoCommit(false);
			svLink.setAutoCommit(false);
			DbDataObject holdingObj = svr.getObjectById(75365L, SvReader.getTypeIdByName(Tc.HOLDING), null);

			DbDataObject userObj = svr.getObjectById(21627206L, svCONST.OBJECT_TYPE_USER, null);

			svLink.linkObjects(userObj, holdingObj, "POA", "test link between USER and HOLDING", true);

			svLink.dbCommit();

		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
			fail();
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
	}

	@Test
	public void testSearchAnimalByBarCode() {
		// configure the core to clean up every 5 seconds
		SvReader svr = null;
		String sessionId = login("A.ADMIN3", "naits1234");
		Reader rdr = new Reader();
		try {
			svr = new SvReader(sessionId);
			DbDataObject dbo = rdr.getAnimalByBarCode("GE11938918", svr);
			log4j.info(dbo.toSimpleJson());
			dbo = rdr.getAnimalByBarCode("EE0018830569", svr);
			log4j.info(dbo.toSimpleJson());
			DbDataArray arr = new DbDataArray();
			arr.addDataItem(dbo);
			log4j.info(rdr.convertDbDataArrayToGridJson(arr, Tc.ANIMAL));
			log4j.info("INVALID FORMAT");
			String pom = "{";
			pom += dbo.toSimpleJson().toString() + "}";
			log4j.debug(pom);

			String g = "04-10-2019";

		} catch (SvException e) {
			fail("Test failed");
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (svr != null)
				svr.release();
		}
	}

	@Test
	public void testQuery() {
		// configure the core to clean up every 5 seconds
		SvReader svr = null;
		String sessionId = login("A.ADMIN3", "naits1234");
		Reader rdr = new Reader();
		try {
			svr = new SvReader(sessionId);

			log4j.debug(svr.getCoreLastActivity());

			DateTime dt = new DateTime(svr.getCoreLastActivity());

			log4j.debug(dt);

			log4j.debug(dt.getChronology());

			log4j.debug(dt.getHourOfDay());

			log4j.debug(dt.toString().substring(11, 19));

		} catch (SvException e) {
			fail("Test failed");
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (svr != null)
				svr.release();
		}
	}

	public String testNest(JsonArray jarr) {
		return "ok";
	}

	@Test
	public void isJsonValidTest() throws org.json.simple.parser.ParseException {
		String jsonInString = "";
		Gson gson = null;
		SvReader svr = null;
		Reader rdr = new Reader();
		try {
			String sessionId = login("A.ADMIN3", "naits1234");
			svr = new SvReader(sessionId);

			DbDataObject dboAnim = rdr.findAppropriateAnimalByAnimalId("47343543", svr);

			DbDataArray arr = new DbDataArray();
			arr.addDataItem(dboAnim);
			jsonInString = rdr.convertDbDataArrayToGridJson(arr, Tc.ANIMAL);

			JSONArray jsonArr = new JSONArray(jsonInString);

			log4j.debug("JSON parser");

			log4j.debug(jsonArr.toString());
			try {
				gson = new Gson();

				gson.fromJson(jsonInString, Object.class);

				log4j.debug("IT IS VALID");
			} catch (com.google.gson.JsonSyntaxException ex) {
				log4j.debug("IT IS NOT VALID");
			}
		} catch (SvException e) {
			e.printStackTrace();
		} finally {
			if (svr != null)
				svr.release();
		}

	}

	@Test
	public void testRemoveComma() throws SvException, UnsupportedEncodingException {
		String diseases = "0,1,2,";
		String pom = diseases.substring(0, diseases.length() - 1);
		log4j.debug(pom);
		String before = "D.CEKOVIC!@#%^&*";
		String encoded = URLEncoder.encode(before, "UTF-8");
		String decoded = URLDecoder.decode(encoded, "UTF-8");
		System.out.println(encoded);
		System.out.println(decoded);
	}

	public void testTest(SvReader svr, SvWriter svw) throws SvException {
		Reader rdr = new Reader();
		Writer wr = new Writer();
		int counter = 0;
		DbDataArray expCertForInvalidate = fixExportedCertificates(svr);
		if (expCertForInvalidate.size() > 0) {
			for (DbDataObject tempExpCert : expCertForInvalidate.getItems()) {
				DbDataArray arrLinkedAnimals = rdr.getAnimalsLinkedToExportCertificate(tempExpCert, svr);
				if (arrLinkedAnimals != null && arrLinkedAnimals.size() > 0) {
					for (DbDataObject dboAnimal : arrLinkedAnimals.getItems()) {
						if (dboAnimal.getStatus().equals(Tc.PENDING_EX)) {
							wr.undoAnimalPendingExport(dboAnimal, rdr, svr, svw);
							counter++;
						}
					}
				}
				wr.changeStatus(tempExpCert, Tc.EXPIRED, svw);
			}
		}
		log4j.debug(counter);
	}

	public DbDataArray fixExportedCertificates(SvReader svr) throws SvException {
		Reader rdr = new Reader();
		DbDataArray arrExportCertificates = new DbDataArray();
		DbDataArray arrQuarantines = rdr.getExpiredQuarantines(svr);
		if (arrQuarantines != null && arrQuarantines.size() > 0) {
			for (DbDataObject dboQuarantine : arrQuarantines.getItems()) {
				DbDataArray arrExportCertificatesPerQuarantine = svr.getObjectsByParentId(dboQuarantine.getObject_id(),
						SvReader.getTypeIdByName(Tc.EXPORT_CERT), null, 0, 0);
				if (arrExportCertificatesPerQuarantine != null && arrExportCertificatesPerQuarantine.size() > 0) {
					for (DbDataObject dboExportCert : arrExportCertificatesPerQuarantine.getItems()) {
						if (dboExportCert.getStatus().equals("EXPIRED")) {
							if (!dboExportCert.getStatus().equals(Tc.CANCELED)) {
								arrExportCertificates.addDataItem(dboExportCert);
							}
						}
					}
				}
			}
		}
		return arrExportCertificates;
	}

	@Test
	public void updateExportCert() {
		// configure the core to clean up every 5 seconds
		SvReader svr = null;
		SvWriter svw = null;
		String sessionId = login("A.ADMIN3", "naits1234");
		Reader rdr = new Reader();
		Writer wr = new Writer();
		ValidationChecks vc = new ValidationChecks();
		try {
			svr = new SvReader(sessionId);
			svw = new SvWriter(svr);
			svw.dbSetAutoCommit(false);
			DbDataObject exportObj = svr.getObjectById(1760568L, SvReader.getTypeIdByName(Tc.EXPORT_CERT), null);
			DbDataObject exportObj1 = svr.getObjectById(2408315L, SvReader.getTypeIdByName(Tc.EXPORT_CERT), null);
			DbDataObject exportObj2 = svr.getObjectById(2517011L, SvReader.getTypeIdByName(Tc.EXPORT_CERT), null);
			DbDataObject exportObj3 = svr.getObjectById(2517544L, SvReader.getTypeIdByName(Tc.EXPORT_CERT), null);
			DbDataObject exportObj4 = svr.getObjectById(3151774L, SvReader.getTypeIdByName(Tc.EXPORT_CERT), null);
			DbDataObject exportObj5 = svr.getObjectById(3152882L, SvReader.getTypeIdByName(Tc.EXPORT_CERT), null);
			DbDataObject exportObj6 = svr.getObjectById(3152974L, SvReader.getTypeIdByName(Tc.EXPORT_CERT), null);
			DbDataObject exportObj7 = svr.getObjectById(3152997L, SvReader.getTypeIdByName(Tc.EXPORT_CERT), null);
			DbDataObject exportObj8 = svr.getObjectById(3153010L, SvReader.getTypeIdByName(Tc.EXPORT_CERT), null);
			DbDataObject exportObj9 = svr.getObjectById(3786410L, SvReader.getTypeIdByName(Tc.EXPORT_CERT), null);
			DbDataObject exportObj10 = svr.getObjectById(3786418L, SvReader.getTypeIdByName(Tc.EXPORT_CERT), null);
			DbDataObject exportObj11 = svr.getObjectById(4782627L, SvReader.getTypeIdByName(Tc.EXPORT_CERT), null);
			DbDataObject exportObj12 = svr.getObjectById(6375235L, SvReader.getTypeIdByName(Tc.EXPORT_CERT), null);

			DbDataArray arr = new DbDataArray();

			arr.addDataItem(exportObj);
			arr.addDataItem(exportObj1);
			arr.addDataItem(exportObj2);
			arr.addDataItem(exportObj3);
			arr.addDataItem(exportObj4);
			arr.addDataItem(exportObj5);
			arr.addDataItem(exportObj6);
			arr.addDataItem(exportObj7);
			arr.addDataItem(exportObj8);
			arr.addDataItem(exportObj9);
			arr.addDataItem(exportObj10);
			arr.addDataItem(exportObj11);
			arr.addDataItem(exportObj12);

			for (DbDataObject dbo : arr.getItems()) {
				dbo.setStatus(Tc.PROCESSED);
				svw.saveObject(dbo, false);
			}

			svw.dbCommit();

		} catch (SvException e) {
			fail("Test failed");
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (svr != null)
				svr.release();
		}
	}

	@Test
	public void testObjSummaryInfo() {
		// configure the core to clean up every 5 seconds
		SvReader svr = null;
		SvWriter svw = null;
		String sessionId = login("A.ADMIN3", "naits1234");
		Reader rdr = new Reader();
		Writer wr = new Writer();
		ValidationChecks vc = new ValidationChecks();
		try {
			svr = new SvReader(sessionId);
			svw = new SvWriter(svr);
			svw.dbSetAutoCommit(false);

			/*
			 * DbDataObject dboAnimal = rdr.findAppropriateAnimalByAnimalId("000012345",
			 * svr); Boolean pom = vc.checkIfAnimalMotherAgeIsAppropriate(dboAnimal, svr);
			 */

			testTest(svr, svw);

		} catch (SvException e) {
			fail("Test failed");
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (svr != null)
				svr.release();
		}
	}

	@Test
	public void testPeriodBetweenAnimals() {
		// configure the core to clean up every 5 seconds
		SvReader svr = null;
		SvWriter svw = null;
		String sessionId = login("A.ADMIN3", "naits1234");
		Reader rdr = new Reader();
		Writer wr = new Writer();
		ValidationChecks vc = new ValidationChecks();
		try {
			svr = new SvReader(sessionId);
			svw = new SvWriter(svr);
			svw.dbSetAutoCommit(false);

			// DbDataObject dboAnimal =
			// rdr.findAppropriateAnimalByAnimalId("020202122", svr);
			/*
			 * if (!vc.checkIfPeriodBetweenNewbornsIsAppropriate(dboAnimal, svr)) { throw
			 * (new SvException(
			 * "naits.error.periodBetweenNewBornAnimalAndLastBornAnimalAreNotAppropriate",
			 * svCONST.systemUser, null, null)); }
			 */
			// DbDataObject dboAnimal2 =
			// rdr.findAppropriateAnimalByAnimalId("020202122", svr);
			// DbDataArray duplicateArr = new DbDataArray();
			// duplicateArr.addDataItem(dboAnimal);
			// duplicateArr.addDataItem(dboAnimal2);
			LinkedHashSet<Long> hashSet = new LinkedHashSet<>();
			Long pom1 = 1L;
			hashSet.add(pom1);
			Long pom2 = 1L;
			hashSet.add(pom2);
			Long pom3 = 2L;
			hashSet.add(pom3);
			ArrayList<Long> sve = new ArrayList<>(hashSet);
			log4j.debug(sve);

		} catch (SvException e) {
			fail("Test failed");
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (svr != null)
				svr.release();
			if (svw != null)
				svw.release();
		}
	}

	@Test
	public void testDirectTransfer() throws Exception {
		// configure the core to clean up every 5 seconds
		SvReader svr = null;
		SvWriter svw = null;
		String sessionId = login("A.ADMIN3", "naits1234");
		Reader rdr = new Reader();
		Writer wr = new Writer();
		ValidationChecks vc = new ValidationChecks();
		ApplicationServices rea = new ApplicationServices();
		try {
			svr = new SvReader(sessionId);
			svw = new SvWriter(svr);
			svw.dbSetAutoCommit(false);

			Object temp = null;
			String result = "";
			Response responseHtml = rea.transferAnimalOrFlockToHolding(sessionId, "1317123000017-006", "1", "1275929",
					"2019-10-16", "360262", 0L, 0L, 0L, 0L);
			temp = responseHtml.getEntity();
			result = temp.toString();
			log4j.debug(result);
		} catch (SvException e) {
			fail("Test failed");
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (svr != null)
				svr.release();
			if (svw != null)
				svw.release();
		}
	}

	@Test
	public void testAbsoluteValue() {
		// configure the core to clean up every 5 seconds
		Integer a = 5;
		Integer b = 8;
		Integer result = b - a;
		log4j.debug(result);
		String toAbsolute = result.toString();
		if (toAbsolute.contains("-")) {
			String pom = toAbsolute.replace("-", "");
			result = Integer.valueOf(pom);
		}
		log4j.debug(result);
		DbDataArray db = new DbDataArray();
		log4j.debug(db.size());

	}

	@Test
	public void testSvarogGetVal() {
		// configure the core to clean up every 5 seconds
		SvReader svr = null;
		SvWriter svw = null;
		String sessionId = login("A.ADMIN3", "naits1234");
		Reader rdr = new Reader();
		Writer wr = new Writer();
		ValidationChecks vc = new ValidationChecks();
		try {
			svr = new SvReader(sessionId);
			svw = new SvWriter(svr);
			svw.dbSetAutoCommit(false);

			DbDataObject dboAnimal = rdr.findAppropriateAnimalByAnimalId("2951981", svr);
			if (dboAnimal.getVal("TESTTEST") == null) {
				log4j.debug("OK");
			}

		} catch (SvException e) {
			fail("Test failed");
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (svr != null)
				svr.release();
		}
	}

	static final int COMMIT_COUNT = 1000;

	public void importEarTagsProcess(Long parentId, Long startRange, Long endRange, String tag_type, SvReader svr,
			SvWriter svw) throws SvException {
		DbDataObject dbInventory = null;
		DbDataArray objectsToSave = new DbDataArray();
		Long typeId = SvReader.getTypeIdByName("INVENTORY_ITEM");
		int recCount = 0;
		for (Long i = startRange; i <= endRange; i++) {
			dbInventory = new DbDataObject(typeId);
			dbInventory.setVal(Tc.EAR_TAG_NUMBER, i.toString());
			dbInventory.setVal(Tc.TAG_TYPE, tag_type);
			dbInventory.setParent_id(parentId);
			DbSearchCriterion cr1 = new DbSearchCriterion(Tc.TAG_TYPE, DbCompareOperand.EQUAL, tag_type);
			DbSearchCriterion cr2 = new DbSearchCriterion(Tc.EAR_TAG_NUMBER, DbCompareOperand.EQUAL, i.toString());
			DbSearchExpression dbse = new DbSearchExpression();
			dbse.addDbSearchItem(cr1).addDbSearchItem(cr2);
			DbDataArray result = svr.getObjects(dbse, typeId, null, 0, 0);
			if (result == null || result.size() < 1) {
				objectsToSave.addDataItem(dbInventory);
				recCount++;
			}
			if (recCount == COMMIT_COUNT) {
				recCount = 0;
				svw.saveObject(objectsToSave, true, true);
				objectsToSave = new DbDataArray();
			}
		}
		if (objectsToSave.getItems().size() > 0) {
			recCount = 0;
			svw.saveObject(objectsToSave, true, true);
			objectsToSave = new DbDataArray();
		}
		log4j.debug("SUCCESS");
	}

	@Test
	public void importEarTags() {
		SvReader svr = null;
		SvWriter svw = null;
		String sessionId = login("A.ADMIN3", "naits1234");
		Reader rdr = new Reader();
		Writer wr = new Writer();
		ValidationChecks vc = new ValidationChecks();
		try {
			svr = new SvReader(sessionId);
			svw = new SvWriter(svr);
			svw.dbSetAutoCommit(false);

			importEarTagsProcess(49L, 10200000L, 12822000L, "1", svr, svw);
			svw.dbCommit();
		} catch (SvException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (svr != null)
				svr.release();
		}
	}

	@Test
	public void testSvObjectHistory() {
		// configure the core to clean up every 5 seconds
		SvReader svr = null;
		SvWriter svw = null;
		String sessionId = login("A.ADMIN3", "naits1234");
		Reader rdr = new Reader();
		Writer wr = new Writer();
		ValidationChecks vc = new ValidationChecks();
		try {
			svr = new SvReader(sessionId);
			svw = new SvWriter(svr);
			svw.dbSetAutoCommit(false);

			DbDataObject dboAnimal = rdr.findAppropriateAnimalByAnimalId("8126332902", svr);
			DbSearchCriterion cr1 = new DbSearchCriterion("PKID", DbCompareOperand.EQUAL, 21869276L);
			DbSearchExpression dbse = new DbSearchExpression();
			dbse.addDbSearchItem(cr1);
			DbDataArray dboInvalidate = svr.getObjectsHistory(dbse, SvReader.getTypeIdByName(Tc.ANIMAL), 0, 0);
			log4j.debug(dboInvalidate.toSimpleJson());
		} catch (SvException e) {
			fail("Test failed");
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (svr != null)
				svr.release();
		}
	}

	@Test
	public void testJsonObjectWithArray() throws Exception {
		// configure the core to clean up every 5 seconds
		SvReader svr = null;
		SvWriter svw = null;
		String sessionId = login("A.ADMIN3", "naits1234");
		Reader rdr = new Reader();
		Writer wr = new Writer();
		ValidationChecks vc = new ValidationChecks();
		try {
			svr = new SvReader(sessionId);
			svw = new SvWriter(svr);
			svw.dbSetAutoCommit(false);

			String pom = "{\"objectArray\":[{\"TRANSFER.PKID\":24705383,\"TRANSFER.OBJECT_ID\":24455838,\"TRANSFER.PARENT_ID\":1275788,\"TRANSFER.OBJECT_TYPE\":1275638,\"TRANSFER.STATUS\":\"DRAFT\",\"TRANSFER.DESTINATION_OBJ_ID\":\"1275788\",\"TRANSFER.TAG_TYPE\":\"1\",\"TRANSFER.START_TAG_ID\":566,\"TRANSFER.END_TAG_ID\":567,\"TRANSFER.TRANSFER_TYPE\":\"INCOME\",\"TRANSFER.SUBJECT_FROM\":\"Tbilisi\",\"TRANSFER.SUBJECT_TO\":\"region.region_code_23\",\"TRANSFER.RETURNED_BY\":\"A.ADMIN3\"},{\"TRANSFER.PKID\":24705387,\"TRANSFER.OBJECT_ID\":24455842,\"TRANSFER.PARENT_ID\":1275788,\"TRANSFER.OBJECT_TYPE\":1275638,\"TRANSFER.STATUS\":\"DRAFT\",\"TRANSFER.DESTINATION_OBJ_ID\":\"1275788\",\"TRANSFER.TAG_TYPE\":\"1\",\"TRANSFER.START_TAG_ID\":955,\"TRANSFER.END_TAG_ID\":956,\"TRANSFER.TRANSFER_TYPE\":\"INCOME\",\"TRANSFER.SUBJECT_FROM\":\"Ozurgeti\",\"TRANSFER.SUBJECT_TO\":\"region.region_code_23\",\"TRANSFER.RETURNED_BY\":\"A.ADMIN3\"}]}";
			JsonParser jp = new JsonParser();
			JsonObject jsonBig = (JsonObject) jp.parse(pom);
			log4j.debug(jsonBig);
			log4j.debug(jsonBig.has(Tc.OBJ_ARRAY));
			log4j.debug("SAMO SO GET: " + jsonBig.get(Tc.OBJ_ARRAY));
			log4j.debug("SAMO SO GET AS JSON ARRAY: " + jsonBig.getAsJsonArray(Tc.OBJ_ARRAY));
			MassActions ma = new MassActions();

			ma.updateStatusMassAction(Tc.TRANSFER, Tc.DELIVERED, jsonBig.getAsJsonArray(Tc.OBJ_ARRAY),
					svr.getSessionId());

		} catch (SvException e) {
			fail("Test failed");
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (svr != null)
				svr.release();
		}
	}

	@Test
	public void createUserGroupsTest() throws Exception {
		SvReader svr = null;
		SvWriter svw = null;
		String sessionId = login("A.ADMIN3", "naits1234");
		Reader rdr = new Reader();
		Writer wr = new Writer();
		ValidationChecks vc = new ValidationChecks();
		try {
			svr = new SvReader(sessionId);
			svw = new SvWriter(svr);
			svw.dbSetAutoCommit(false);
			DbDataObject dboPrivateVets = createPrivateVeterinariansGroup();
			svw.saveObject(dboPrivateVets, false);
			/*
			 * UserManager um = new UserManager(); DbDataObject dboUser2 =
			 * svr.getObjectById(24456341L, svCONST.OBJECT_TYPE_USER, null); DbDataObject
			 * dboUser = svr.getObjectById(24456254L, svCONST.OBJECT_TYPE_USER, null);
			 * 
			 * ArrayList<String> currUserPermissions = um
			 * .getCurrentUserPermissions(dboUser2.getVal("USER_NAME").toString( ), svr);
			 * 
			 * ArrayList<String> currGroupPermissions =
			 * um.getCurrentGroupPermissions("USERS", svr);
			 */

			svw.dbCommit();
			log4j.debug("succcess");
			// log4j.debug(currGroupPermissions);
		} catch (SvException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (svr != null)
				svr.release();
			if (svw != null)
				svw.release();
		}
	}

	@Test
	public void createRegularVets() throws Exception {
		SvReader svr = null;
		SvWriter svw = null;
		String sessionId = login("A.ADMIN3", "naits1234");
		Reader rdr = new Reader();
		Writer wr = new Writer();
		ValidationChecks vc = new ValidationChecks();
		try {
			svr = new SvReader(sessionId);
			svw = new SvWriter(svr);
			svw.dbSetAutoCommit(false);
			DbDataObject dboStateVets = createVeterinariansGroup();
			svw.saveObject(dboStateVets, false);
			svw.dbCommit();
			log4j.debug("succcess");
			// log4j.debug(currGroupPermissions);
		} catch (SvException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (svr != null)
				svr.release();
			if (svw != null)
				svw.release();
		}
	}

	@Test
	public void testSearchForObjects() {
		SvReader svr = null;
		SvWriter svw = null;
		String token = null;
		Reader rdr = new Reader();
		Writer wr = new Writer();
		ValidationChecks vc = new ValidationChecks();
		// 540752
		try {
			token = login("A.ADMIN3", "naits1234");
			svr = new SvReader(token);
			// svw = new SvWriter(svr);
			DbDataArray result = null;
			String startId = String.valueOf(38L) + "00";
			String endId = String.valueOf(38L) + "99";
			Long startLong = 38L * 100L;
			Long endLong = (38L * 100L) + 99L;
			DbSearchCriterion cr1 = new DbSearchCriterion(Tc.EXTERNAL_ID, DbCompareOperand.BETWEEN, startLong, endLong);
			result = svr.getObjects(new DbSearchExpression().addDbSearchItem(cr1), svCONST.OBJECT_TYPE_ORG_UNITS, null,
					0, 0);
			log4j.debug(result.toSimpleJson());
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
	}

	@Test
	public void setMetCrack() throws Exception {
		SvReader svr = null;
		SvWriter svw = null;
		String sessionId = login("A.ADMIN3", "naits1234");
		Reader rdr = new Reader();
		Writer wr = new Writer();
		ValidationChecks vc = new ValidationChecks();
		try {
			svr = new SvReader(sessionId);
			svw = new SvWriter(svr);
			svw.dbSetAutoCommit(false);

		} catch (SvException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (svr != null)
				svr.release();
			if (svw != null)
				svw.release();
		}
	}

	@Test
	public void testPrecentageMethod() throws Exception {
		SvReader svr = null;
		SvWriter svw = null;
		String sessionId = login("A.ADMIN3", "naits1234");
		Reader rdr = new Reader();
		Writer wr = new Writer();
		ValidationChecks vc = new ValidationChecks();
		try {
			svr = new SvReader(sessionId);
			svw = new SvWriter(svr);
			svw.dbSetAutoCommit(false);

			log4j.debug("HEALTH HEALTH" + rdr
					.calculateHoldingHealth(rdr.findAppropriateHoldingByPic("1317123000040", svr).getObject_id(), svr));

			log4j.debug("PRECENTAGE: " + rdr.calculatePrecentage(3, 48));

			ArrayList boom = null;

			String a = "This is proper MessAgE     a    ";
			log4j.debug(a.trim());

			// log4j.debug(vc.checkIfAnimalBelongsToSlaughterhouse(3225965L,
			// svr.getSessionId()));

		} catch (SvException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (svr != null)
				svr.release();
			if (svw != null)
				svw.release();
		}
	}

	@Test
	public void testInvenotryTransferOnSave() throws Exception {
		SvReader svr = null;
		SvWriter svw = null;
		String sessionId = login("A.ADMIN3", "naits1234");
		Reader rdr = new Reader();
		Writer wr = new Writer();
		ValidationChecks vc = new ValidationChecks();
		try {
			OnSaveValidations onSave = new OnSaveValidations();
			SvCore.registerOnSaveCallback(onSave, SvReader.getTypeIdByName(Tc.TRANSFER));
			svr = new SvReader(sessionId);
			svw = new SvWriter(svr);
			svw.dbSetAutoCommit(false);

			DbDataObject dboTransfer = new DbDataObject();
			dboTransfer.setObject_type(SvReader.getTypeIdByName(Tc.TRANSFER));
			dboTransfer.setVal(Tc.TAG_TYPE, "1");
			dboTransfer.setVal(Tc.START_TAG_ID, 569L);
			dboTransfer.setVal(Tc.END_TAG_ID, 570L);
			dboTransfer.setVal(Tc.SUBJECT_TO, "munic.munic_code_1153");
			svw.saveObject(dboTransfer, false);

			log4j.debug("OKD");
			svw.dbCommit();
		} catch (SvException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (svr != null)
				svr.release();
			if (svw != null)
				svw.release();
		}
	}

	@Test
	public void linkUsersWithLaboratory() throws Exception {
		SvReader svr = null;
		SvWriter svw = null;
		String sessionId = login("A.ADMIN3", "naits1234");
		Reader rdr = new Reader();
		Writer wr = new Writer();
		SvLink svl = null;
		ValidationChecks vc = new ValidationChecks();
		try {
			svr = new SvReader(sessionId);
			svw = new SvWriter(svr);
			svl = new SvLink(svw);
			svw.dbSetAutoCommit(false);

			DbDataObject dboUser = rdr.searchForObject(svCONST.OBJECT_TYPE_USER, Tc.USER_NAME, "I.BIRAJ", svr);
			DbDataArray arrLabs = svr.getObjectsByTypeId(SvReader.getTypeIdByName(Tc.LABORATORY), null, 0, 0);
			for (DbDataObject holdingObject : arrLabs.getItems()) {
				DbDataObject dbLinkType = SvLink.getLinkType("POA", dboUser.getObject_type(),
						holdingObject.getObject_type());
				DbDataObject dbLinkObj = rdr.getLinkObject(dboUser.getObject_id(), holdingObject.getObject_id(),
						dbLinkType.getObject_id(), svr);
				if (dbLinkObj == null) {
					svl.linkObjects(dboUser, holdingObject, "POA", "", false, true);
				}
			}

		} catch (SvException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (svr != null)
				svr.release();
			if (svw != null)
				svw.release();
		}
	}

	@Test
	public void testLocalDbPermissions() throws Exception {
		SvReader svr = null;
		SvWriter svw = null;
		String sessionId = login("A.ADMIN3", "naits1234");
		Reader rdr = new Reader();
		Writer wr = new Writer();
		SvLink svl = null;
		ValidationChecks vc = new ValidationChecks();
		UserManager um = new UserManager();
		SvSecurity svs = null;
		try {
			svr = new SvReader(sessionId);
			svw = new SvWriter(svr);
			svl = new SvLink(svw);
			svs = new SvSecurity(svl);
			svw.dbSetAutoCommit(false);
			String pom = um.getCurrentUserPermissions("A.ADMIN3", svr).toString();
			log4j.debug("*******PERMISSIONS********" + pom);
			ArrayList<String> mandatoryPermissions = new ArrayList<String>();
			mandatoryPermissions.add("system.null_geometry");
			um.processPermissionsToUserOrGroup("ASSIGN", SvReader.getUserBySession(sessionId),
					um.getSvarogCoreTablePermission(svr), um.getSvarogCustomTablePermission("FULL", svr), true, svr,
					svw, svs);
			log4j.debug("*******SUCCESS********");
		} catch (SvException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (svr != null)
				svr.release();
			if (svw != null)
				svw.release();
			if (svl != null)
				svl.release();
			if (svs != null)
				svs.release();
		}
	}

	public static int brojDoProsek(ArrayList<Integer> niza) {
		int result = 0;
		int num2 = 0;
		Double blaBla = 2.5D;
		log4j.debug(Math.round(blaBla));

		double averageArray = 0D;
		int sum = 0;
		for (Integer el : niza) {
			sum += el;
		}
		averageArray = (double) (sum / niza.size());
		String[] afterDotSplitter = String.valueOf(averageArray).toString().split("\\.");
		int tempResult = (int) averageArray;
		int tempIndex = 0;
		if (Integer.valueOf(afterDotSplitter[1]) > 5) {
			tempResult += 1;
		}
		int closestNum = 0;
		int minimalSum = Math.abs(tempResult - niza.get(0));
		ArrayList<Integer> tempArr = niza;
		for (int i = 0; i < niza.size(); i++) {
			if (tempResult == niza.get(i)) {
				result = niza.get(i);
				break;
			} else {
				int tempMinimalSum = Math.abs(tempResult - niza.get(i));
				if (tempMinimalSum <= minimalSum) {
					minimalSum = tempMinimalSum;
					result = niza.get(i);
				}
			}
		}
		return result;
	}

	public String assignOrUnasignPackagePermissions(String packageName, String actionName, Long objectId,
			String sessionId) throws Exception {
		String resultMsg = "naits.error.admConsole.permissionPackageNotAttached";
		SvReader svr = null;
		SvWriter svw = null;
		SvSecurity svs = null;
		UserManager um = new UserManager();
		try {
			svr = new SvReader(sessionId);
			svw = new SvWriter(svr);
			svs = new SvSecurity(svw);
			ArrayList<String> customPackagePermissions = new ArrayList<>();
			boolean containsSomePerm = false;
			DbDataObject dboObjectToHandle = svr.getObjectById(objectId, svCONST.OBJECT_TYPE_USER, null);
			// get all custom package permissions
			customPackagePermissions = um.getCustomPermissionsPerPackage(packageName, svr);

			// exclude existing user permissions from
			// customPackagePermissions
			// TODO
			// get all custom package permissions
			// if we receive multiple package names, we can split them
			// by
			// comma (',')
			customPackagePermissions = um.getCustomPermissionsPerPackage(packageName, svr);
			ArrayList<String> beforeDetachPerms = um.getCustomPermissionForUserOrGroup(dboObjectToHandle, svr);
			// exclude existing user permissions from
			// customPackagePermissions
			// TODO
			ArrayList<String> filteredPackagePermissions = new ArrayList<>();
			if (actionName.equals(Tc.ASSIGN)) {
				ArrayList<String> currentPermissions = um.getCurrentPermissionsPerUserOrGroup(dboObjectToHandle, svr);
				String currPermissions = currentPermissions.toString();
				resultMsg = "naits.success.admConsole.permissionPackageSuccessfullyAttached";
				if (currentPermissions != null && currentPermissions.size() > 0) {
					for (String packagePerm : customPackagePermissions) {
						if (!currPermissions.contains(packagePerm)) {
							filteredPackagePermissions.add(packagePerm);
							containsSomePerm = true;
						}
					}
				} else {
					containsSomePerm = true;
					filteredPackagePermissions = customPackagePermissions;
				}
			} else {
				containsSomePerm = true;
				resultMsg = "naits.success.admConsole.permissionPackageSuccessfullyDetached";
				filteredPackagePermissions = customPackagePermissions;
			}
			if (!containsSomePerm) {
				throw (new SvException("naits.error.userAlreadyHasAllThePermissionsFromTheChosenPackage",
						svr.getInstanceUser()));
			}
			if (filteredPackagePermissions == null || filteredPackagePermissions.size() == 0) {
				throw (new SvException("naits.error.packagePermissionIsEmptyNothingAssigned", svr.getInstanceUser()));
			}
			// assign all custom package permissions
			// use always only REVOKE false, so no
			// corePermissions needs to be added
			um.processPermissionsToUserOrGroup(actionName, dboObjectToHandle, null, filteredPackagePermissions, false,
					svr, svw, svs);
			if (actionName.equals(Tc.UNASSIGN)) {
				ArrayList<String> afterDetachPerms = um.getCustomPermissionForUserOrGroup(dboObjectToHandle, svr);
				for (String cpp : customPackagePermissions) {
					if (afterDetachPerms.contains(cpp)) {
						throw (new SvException("naits.error.permissionPackageCannotBeUnassignedGroupPermission",
								svr.getInstanceUser()));
					}
				}
				if (beforeDetachPerms.equals(afterDetachPerms)) {
					throw (new SvException("naits.error.noPackagePermissionWasUnassigned", svr.getInstanceUser()));
				}
			}
			svw.dbCommit();
		} finally {
			if (svr != null) {
				svr.release();
			}
			if (svw != null) {
				svw.release();
			}
			if (svs != null) {
				svs.release();
			}
		}
		return resultMsg;
	}

	@Test
	public void testNotifications() throws Exception {
		SvReader svr = null;
		SvWriter svw = null;
		SvNotification svn = null;
		String sessionId = login("A.ADMIN3", "naits1234");
		Reader rdr = new Reader();
		Writer wr = new Writer();
		ValidationChecks vc = new ValidationChecks();
		try {
			svr = new SvReader(sessionId);
			svw = new SvWriter(svr);
			svn = new SvNotification(svw);
			svw.dbSetAutoCommit(false);

			JSONObject resultJson = new JSONObject();

			HashMap<String, Object> mapObj = new HashMap<String, Object>();
			mapObj.put("health_status", rdr.calculateHoldingHealth(50431L, svr));
			mapObj.put("quarantine_status", "0");
			mapObj.put("movement_status", "1");
			if (vc.checkIfHoldingBelongsInActiveQuarantine(50431L, svr)) {
				mapObj.replace("quarantine_status", "1");
			}
			if (!vc.checkIfMovementIsAllowedPerHolding(50431L, rdr, svr)) {
				mapObj.put("movement_status", "0");
			}
			String result = new JSONObject(mapObj).toJSONString();
			System.out.println(result);

			log4j.debug(resultJson.toString());
			DbDataArray notificationsPerUser = new DbDataArray();
			DbDataObject userObj = svr.getUserBySession(svr.getSessionId());
			/*
			 * if (userObj != null && userObj.getObject_id() != 0L) { notificationsPerUser =
			 * svn.getNotificationPerUser(userObj); result =
			 * notificationsPerUser.toJson().toString(); } System.out.println(result);
			 */
			DbDataObject svLinkObj = svr.getObjectById(24457919L, svCONST.OBJECT_TYPE_LINK, null);
			wr.invalidateLink(svLinkObj, true, svr);
			svw.dbCommit();
			log4j.debug("SUCCESS");
		} catch (SvException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (svr != null)
				svr.release();
			if (svw != null)
				svw.release();
		}
	}

	@Test
	public void testSetDateInTimeStamp() throws Exception {
		SvReader svr = null;
		SvWriter svw = null;
		String sessionId = login("R.PEJOV", "naits1234");
		Reader rdr = new Reader();
		Writer wr = new Writer();
		ValidationChecks vc = new ValidationChecks();
		try {
			svr = new SvReader(sessionId);
			svw = new SvWriter(svr);
			svw.dbSetAutoCommit(false);

			DbDataArray pom = rdr.getOverlappingRanges("TRANSFER", 49L, "4", 70003L, 70007L, svr);
			log4j.debug(pom.toSimpleJson());

			Connection conn = null;

		} catch (SvException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (svr != null)
				svr.release();
			if (svw != null)
				svw.release();
		}
	}

	@Test
	public void testApplicationServices() throws Exception {
		SvReader svr = null;
		SvWriter svw = null;
		String sessionId = login("A.ADMIN3", "naits1234");
		Reader rdr = new Reader();
		Writer wr = new Writer();
		ValidationChecks vc = new ValidationChecks();
		ApplicationServices apps = new ApplicationServices();
		try {
			svr = new SvReader(sessionId);
			svw = new SvWriter(svr);
			svw.dbSetAutoCommit(false);

			Response r = apps.getValidOrgUnitsDependOnParentOrgUnit(svr.getSessionId(), "13171230");
			Object pom = new Object();
			/*
			 * pom = r.getEntity(); log4j.debug(pom.toString()); r =
			 * apps.getLabSamplesAccordingGeostatCode(svr.getSessionId(), "29"); pom =
			 * r.getEntity(); log4j.debug(pom.toString());
			 */
			r = apps.getHoldingsAccordingGeostatCode(svr.getSessionId(), "47");
			pom = r.getEntity();
			log4j.debug(pom.toString());
		} catch (SvException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (svr != null)
				svr.release();
			if (svw != null)
				svw.release();
		}
	}

	@Test
	public void testIsEmpty() throws Exception {
		SvReader svr = null;
		SvWriter svw = null;
		String sessionId = login("A.ADMIN3", "naits1234");
		Reader rdr = new Reader();
		Writer wr = new Writer();
		ValidationChecks vc = new ValidationChecks();
		ApplicationServices apps = new ApplicationServices();
		try {
			svr = new SvReader(sessionId);
			svw = new SvWriter(svr);
			svw.dbSetAutoCommit(false);

			DbDataArray ar = new DbDataArray();

			if (ar.size() == 0) {
				log4j.debug("OK ==0");
			}

			if (ar.getItems().isEmpty()) {
				log4j.debug("OK isEmpty");
			}

		} catch (SvException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (svr != null)
				svr.release();
			if (svw != null)
				svw.release();
		}
	}

	@Test
	public void testRelationBetweenInventoryItemAndAnimal() throws Exception {
		SvReader svr = null;
		SvWriter svw = null;
		String sessionId = login("A.ADMIN3", "naits1234");
		Reader rdr = new Reader();
		Writer wr = new Writer();
		ValidationChecks vc = new ValidationChecks();
		ApplicationServices apps = new ApplicationServices();
		try {
			svr = new SvReader(sessionId);
			svw = new SvWriter(svr);
			svw.dbSetAutoCommit(false);

			DbDataObject dboItem = rdr.searchForObject(SvReader.getTypeIdByName(Tc.INVENTORY_ITEM), Tc.EAR_TAG_NUMBER,
					"10000006", svr);

			DbDataObject dboAnimal = rdr.searchForObject(SvReader.getTypeIdByName(Tc.ANIMAL), Tc.ANIMAL_ID,
					"1234425555", svr);

			dboItem.setVal("ANIMAL_OBJ_ID", dboAnimal.getObject_id());

			svw.saveObject(dboItem, false);

			log4j.debug("ok");

			svw.dbCommit();

		} catch (SvException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (svr != null)
				svr.release();
			if (svw != null)
				svw.release();
		}
	}

	@Test
	public void testRelationBetweenInventoryItemAndHolding() throws Exception {
		SvReader svr = null;
		SvWriter svw = null;
		String sessionId = login("A.ADMIN3", "naits1234");
		Reader rdr = new Reader();
		Writer wr = new Writer();
		ValidationChecks vc = new ValidationChecks();
		ApplicationServices apps = new ApplicationServices();
		try {
			svr = new SvReader(sessionId);
			svw = new SvWriter(svr);
			svw.dbSetAutoCommit(false);
			DbDataArray arrItems = svr.getObjectsByParentId(24464773L, SvReader.getTypeIdByName(Tc.INVENTORY_ITEM),
					null, 0, 0);
			DbDataObject dboHolding = rdr.searchForObject(SvReader.getTypeIdByName(Tc.HOLDING), Tc.PIC,
					/* "292439310214X" */ "1317123000040", svr);
			DbDataArray orgUnits = rdr.findDataPerSingleFilter(Tc.VILLAGE_CODE, "29304731", DbCompareOperand.EQUAL,
					SvReader.getTypeIdByName(Tc.HOLDING), svr);
			log4j.debug(orgUnits.toSimpleJson());

			for (DbDataObject dboInventory : arrItems.getItems()) {
				dboInventory.setParent_id(dboHolding.getObject_id());
				svw.saveObject(dboInventory, false);
			}

			String pom = wr.applyAvailableUnappliedInventoryItemsOnValidAnimals(dboHolding.getObject_id(), svr, svw);

			log4j.debug("ok");

			svw.dbCommit();

		} catch (SvException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (svr != null)
				svr.release();
			if (svw != null)
				svw.release();
		}
	}

	@Test
	public void testI18n() throws Exception {
		SvReader svr = null;
		SvWriter svw = null;
		String sessionId = login("A.ADMIN3", "naits1234");
		try {
			svr = new SvReader(sessionId);
			svw = new SvWriter(svr);
			svw.dbSetAutoCommit(false);
			String codeItemValue = "2222222";
			if (!(codeItemValue.length() == 2 || codeItemValue.length() == 4 || codeItemValue.length() == 6)) {
				log4j.error("naits.error.getVillageDependentDropdown.invalidInputParam");
			}

			/*
			 * String localeId = svr.getUserLocaleId(svr.getInstanceUser());
			 * log4j.debug(I18n.getText(localeId, "naits.main.inventory_item.general") +
			 * "brojce 1" + " " + "en.appliedOn" + " " + "123213321213");
			 */

			DateTime dt1 = new DateTime("2019-12-20T02:02:02");
			DateTime dt2 = new DateTime("2019-12-20T03:03:03");

			DateTimeComparator dateTimeComparator = DateTimeComparator.getDateOnlyInstance();
			int dtDifference = dateTimeComparator.compare(dt1, dt2);
			log4j.debug(dtDifference);
			if (dtDifference != 0) {
				log4j.debug("OK");
			}

		} catch (SvException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (svr != null)
				svr.release();
			if (svw != null)
				svw.release();
		}
	}

	@Test
	public void testSetPermissionToUserOrUserGroup() throws Exception {
		SvReader svr = null;
		SvWriter svw = null;
		SvSecurity svs = null;
		String sessionId = login("A.ADMIN3", "naits1234");
		UserManager um = new UserManager();
		try {
			svr = new SvReader(sessionId);
			svr.dbSetAutoCommit(false);
			svw = new SvWriter(svr);
			svs = new SvSecurity(svw);
			Reader rdr = new Reader();
			DbDataObject dboAdmins = svr.getObjectById(-6L, svCONST.OBJECT_TYPE_GROUP, null);
			um.processPermissionsToUserOrGroup("ASSIGN", dboAdmins, um.getSvarogCoreTablePermission(svr),
					um.getSvarogCustomTablePermission("FULL", svr), true, svr, svw, svs);

		} catch (SvException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (svr != null)
				svr.release();
			if (svw != null)
				svw.release();
		}
	}

	@Test
	public void testNullValueInSearchCriteria() throws Exception {
		SvReader svr = null;
		SvWriter svw = null;
		String sessionId = login("A.ADMIN3", "naits1234");
		MassActions ma = new MassActions();
		try {
			OnSaveValidations call = new OnSaveValidations();
			SvCore.registerOnSaveCallback(call, SvReader.getTypeIdByName("TRANSFER"));
			svr = new SvReader(sessionId);
			svr.dbSetAutoCommit(false);
			svw = new SvWriter(svr);

			/*
			 * DbSearchCriterion cr1 = new DbSearchCriterion(Tc.ORIGIN_OBJ_ID,
			 * DbCompareOperand.ISNULL); DbDataArray arr = svr.getObjects(new
			 * DbSearchExpression().addDbSearchItem(cr1),
			 * SvReader.getTypeIdByName(Tc.TRANSFER), null, 0, 0);
			 * log4j.debug(arr.toSimpleJson());
			 */

			java.sql.Date dtNow = new java.sql.Date(new DateTime().getMillis());
			log4j.debug("#####################################" + new DateTime().toString());
			log4j.debug("#####################################" + dtNow);
			svw.dbCommit();
		} catch (SvException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (svr != null)
				svr.release();
			if (svw != null)
				svw.release();
		}
	}

	@Test
	public void testReverseTransferWs() throws Exception {
		SvReader svr = null;
		SvWriter svw = null;
		String sessionId = login("A.ADMIN3", "naits1234");
		MassActions ma = new MassActions();
		Writer wr = new Writer();
		try {
			svr = new SvReader(sessionId);
			svr.dbSetAutoCommit(false);
			svw = new SvWriter(svr);

			DbDataArray arr = new DbDataArray();
			DbDataArray dboTransfers = svr.getObjectsByTypeId(SvReader.getTypeIdByName(Tc.TRANSFER), null, 0, 0);
			for (DbDataObject dboTransfer : dboTransfers.getItems()) {
				DbDataObject dboOrgUnit = svr.getObjectById(dboTransfer.getParent_id(), svCONST.OBJECT_TYPE_ORG_UNITS,
						null);
				if (dboOrgUnit != null && dboOrgUnit.getVal(Tc.EXTERNAL_ID) != null) {
					String sequence = wr.generateTransferId(dboOrgUnit.getVal(Tc.EXTERNAL_ID).toString(), svr);
					dboTransfer.setVal(Tc.TRANSFER_ID, sequence);
					svw.saveObject(dboTransfer, false);
					arr.addDataItem(dboTransfer);
				}
			}
			if (arr != null) {
				svw.saveObject(arr, true, true);
			}
			svw.dbCommit();
		} catch (SvException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (svr != null)
				svr.release();
			if (svw != null)
				svw.release();
		}
	}

	@Test
	public void testTranslateCodeValue() throws Exception {
		SvReader svr = null;
		SvWriter svw = null;
		String sessionId = login("A.ADMIN3", "naits1234");
		MassActions ma = new MassActions();
		Writer wr = new Writer();
		Reader rdr = new Reader();
		try {
			svr = new SvReader(sessionId);
			svr.dbSetAutoCommit(false);
			svw = new SvWriter(svr);
			// DbDataArray arrResult =
			// rdr.findDataPerSingleFilter(Tc.EXTERNAL_ID, "29323535",
			// DbCompareOperand.EQUAL,
			// SvReader.getTypeIdByName(Tc.SVAROG_ORG_UNITS), svr);
			DbDataArray someArr = rdr.getOrgUnitDependOnParentExternalId(29323535L, 29323535L, svr);
			String pom = rdr.convertDbDataArrayToGridJson(someArr, Tc.SVAROG_ORG_UNITS);
			log4j.debug("TO ARRAYLIST+++++++++++++" + pom);
		} catch (SvException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (svr != null)
				svr.release();
			if (svw != null)
				svw.release();
		}
	}

	@Test
	public void measureExecutionOfToJsonMethod() throws Exception {
		SvReader svr = null;
		String sessionId = login("A.ADMIN3", "naits1234");
		MassActions ma = new MassActions();
		Writer wr = new Writer();
		Reader rdr = new Reader();
		try {
			long startTime = System.nanoTime();
			svr = new SvReader(sessionId);
			svr.dbSetAutoCommit(false);
			DbDataObject dbo = new DbDataObject();
			dbo.getClass();
			log4j.debug(dbo.getClass().getCanonicalName());
			DbDataObject dboAnimal = rdr.findAppropriateAnimalByAnimalId("38736783", svr);
			// log4j.debug(dboAnimal.toJson());
			DbDataArray dbArray = rdr.findDataPerSingleFilter(Tc.GEOSTAT_CODE, "29", DbCompareOperand.LIKE,
					SvReader.getTypeIdByName(Tc.LAB_SAMPLE), svr);
			// String fixedConverter= rdr.convertDbDataArrayToGridJson2(dbArray,
			// Tc.LAB_SAMPLE);
			String fixedConverter4 = rdr.convertDbDataArrayToGridJson(dbArray, Tc.LAB_SAMPLE);
			System.out.println("Execution time in nanoseconds  : " + (System.nanoTime() - startTime));
		} catch (SvException e) {
			e.printStackTrace();
		} finally {
			if (svr != null)
				svr.release();
		}
	}

	@Test
	public void measureExecutionOfHoldingBook() throws Exception {
		SvReader svr = null;
		String sessionId = login("A.ADMIN3", "naits1234");
		MassActions ma = new MassActions();
		Writer wr = new Writer();
		Reader rdr = new Reader();
		try {
			svr = new SvReader(sessionId);
			svr.dbSetAutoCommit(false);
			// DbDataObject dboHolding =
			// rdr.findAppropriateHoldingByPic("1313149000148", svr);

			// JsonObject pom = dboHolding.toSimpleJson();
			// log4j.debug(pom);
			DbDataObject dboHolding = rdr.findAppropriateAnimalByAnimalId("1351016", svr);
			DbDataArray arr = rdr.getExistingAnimalOrFlockMovements(dboHolding, null, "VALID", true, true, svr);
			log4j.debug(arr.get(0).toSimpleJson());
			// rdr.getNextOrPreviousHolding(Tc.BACKWARD, dboHolding, svr);
			// rdr.getNextOrPreviousHolding2(Tc.BACKWARD, dboHolding, svr);
		} catch (SvException e) {
			e.printStackTrace();
		} finally {
			if (svr != null)
				svr.release();
		}
	}

	@Test
	public void testTablesWithFieldsWithExtendedParams() throws Exception {
		SvReader svr = null;
		String sessionId = login("A.ADMIN3", "naits1234");
		MassActions ma = new MassActions();
		Writer wr = new Writer();
		Reader rdr = new Reader();
		try {
			svr = new SvReader(sessionId);
			svr.dbSetAutoCommit(false);
			/*
			 * DbDataObject dbo = svr.getObjectById(1361503L, svCONST.OBJECT_TYPE_ORG_UNITS,
			 * null); DbDataArray arr = new DbDataArray(); arr.addDataItem(dbo);
			 * log4j.debug("**********************************************" +
			 * "/n***********************" + "/n****"+rdr.convertDbDataArrayToGridJson(arr,
			 * "SVAROG_ORG_UNITS", svr));
			 */

			DbSearchCriterion cr1 = new DbSearchCriterion("PARENT_OU_ID", DbCompareOperand.EQUAL, 49L);
			DbDataArray ar = svr.getObjects(new DbSearchExpression().addDbSearchItem(cr1),
					svCONST.OBJECT_TYPE_ORG_UNITS, null, 0, 0);
			log4j.debug("**********************************************" + "/n***********************" + "/n****"
					+ rdr.convertDbDataArrayToGridJson(ar, "SVAROG_ORG_UNITS", svr));

		} catch (SvException e) {
			e.printStackTrace();
		} finally {
			if (svr != null)
				svr.release();
		}
		log4j.debug("{\"SV_ISLABEL\":true}");
	}

	@Test
	public void testSUmmaryInfoForAnimalShelter() throws Exception {
		SvReader svr = null;
		String sessionId = login("A.ADMIN3", "naits1234");
		MassActions ma = new MassActions();
		Writer wr = new Writer();
		Reader rdr = new Reader();
		try {
			svr = new SvReader(sessionId);
			svr.dbSetAutoCommit(false);
			DbDataObject dboHolding = svr.getObjectById(9262327L, SvReader.getTypeIdByName(Tc.HOLDING), null);
			LinkedHashMap<String, String> pom = rdr.generateHoldingSummaryInformation(dboHolding, svr);
			log4j.debug(pom.toString());
		} catch (SvException e) {
			e.printStackTrace();
		} finally {
			if (svr != null)
				svr.release();
		}
		log4j.debug("{\"SV_ISLABEL\":true}");
	}

	@Test
	public void testSuspensionNoteAndDescriptionNote() throws Exception {
		SvReader svr = null;
		SvWriter svw = null;
		SvNote svn = null;
		String sessionId = login("A.ADMIN3", "naits1234");
		MassActions ma = new MassActions();
		Writer wr = new Writer();
		Reader rdr = new Reader();
		ApplicationServices app = new ApplicationServices();
		try {
			svr = new SvReader(sessionId);
			svw = new SvWriter(svr);
			svn = new SvNote(svw);
			svr.dbSetAutoCommit(false);
			Object aaa = null;
			String baba = "";
			JsonObject job = new JsonObject();
			job.addProperty("noteText", "This is test! TOMI LOOK AT THIS! TOMI EDITED ME!!!!");
			MultivaluedMap<String, String> Map = new MultivaluedHashMap<String, String>();
			Map.add(job.toString(), null);
			// svn.setNote(9257017L, "SUSPENSION_NOTE", "User didn't vote for
			// our party", true);
			Response s = app.setNoteDescription(sessionId, 4767434L, "GROUP_DESCRIPTION", Map, null);
			aaa = s.getEntity();
			baba = aaa.toString();
			log4j.debug(baba);
			s = app.getNoteDescription(sessionId, 4767434L, "GROUP_DESCRIPTION", null);
			aaa = s.getEntity();
			baba = aaa.toString();
			log4j.debug(baba);
		} catch (SvException e) {
			e.printStackTrace();
		} finally {
			if (svr != null)
				svr.release();
		}
		log4j.debug("{\"SV_ISLABEL\":true}");
	}

	@Test
	public void testAttachOrgUnitToUserGroup() throws Exception {
		SvReader svr = null;
		SvWriter svw = null;
		SvNote svn = null;
		String sessionId = login("A.ADMIN3", "naits1234");
		MassActions ma = new MassActions();
		Writer wr = new Writer();
		Reader rdr = new Reader();
		UserManager um = new UserManager();
		String resultService = "";
		try {
			svr = new SvReader(sessionId);
			DbDataObject dboGroup1 = svr.getObjectById(29071L, svCONST.OBJECT_TYPE_GROUP, null);
			DbDataObject dboGroup2 = svr.getObjectById(-4L, svCONST.OBJECT_TYPE_GROUP, null);
			JsonArray jarr = new JsonArray();
			jarr.add(dboGroup1.toSimpleJson());
			jarr.add(dboGroup2.toSimpleJson());

			resultService = um.attachUserToOrgUnit(1361514L, jarr, sessionId);
			log4j.debug(resultService);
		} catch (SvException e) {
			e.printStackTrace();
		} finally {
			if (svr != null)
				svr.release();
		}
		log4j.debug(resultService);
		log4j.info(resultService);
	}

	@Test
	public void testingMestingJsonObjects() throws Exception {
		JsonObject jobj = new JsonObject();
		jobj.addProperty("1", "************************************* 2");
		log4j.debug(jobj.get("1").getAsString());
		log4j.info("");

	}

	@Test
	public void testSearchDataBySingleFilter() throws Exception {
		SvReader svr = null;
		SvWriter svw = null;
		SvNote svn = null;
		String sessionId = login("A.ADMIN3", "naits1234");
		MassActions ma = new MassActions();
		Writer wr = new Writer();
		Reader rdr = new Reader();
		UserManager um = new UserManager();
		String resultService = "";
		try {
			svr = new SvReader(sessionId);
			DbDataObject dboVaccinationEvent = svr.getObjectById(9802087L,
					SvReader.getTypeIdByName(Tc.VACCINATION_EVENT), null);

			// log4j.debug(rdr.convertDbDataArrayToGridJson(
			// rdr.getCampaignTargetedMunicipalityUnits(dboVaccinationEvent,
			// svr), Tc.SVAROG_ORG_UNITS));
			log4j.debug(rdr.getTargetedMunicipalitiesByVaccinationEvent(dboVaccinationEvent, svr));
		} catch (SvException e) {
			e.printStackTrace();
		} finally {
			if (svr != null)
				svr.release();
		}
		log4j.debug(resultService);
		log4j.info(resultService);
	}

	@Test
	public void testMultiMap() throws Exception {
		ListMultimap<String, String> ap = ArrayListMultimap.create();
		ap.put("key_1", "value_1");
		ap.put("key_1", "value_2");
		ap.put("key_1", "value_3");
		ap.put("key_2", "value_3");
		ap.put("key_2", "value_3");
		ap.put("key_3", "value_3");
		JsonObject jobj = new JsonObject();

		MultivaluedMap<String, String> Map = new MultivaluedHashMap<String, String>();

		log4j.info(ap.toString());
	}

	@Test
	public void testMassAnimalActionWithJsonObjectAsParameter() throws Exception {
		SvReader svr = null;
		SvWriter svw = null;
		SvNote svn = null;
		String sessionId = login("A.ADMIN3", "naits1234");
		MassActions ma = new MassActions();
		Writer wr = new Writer();
		Reader rdr = new Reader();
		ApplicationServices app = new ApplicationServices();
		try {
			svr = new SvReader(sessionId);
			svw = new SvWriter(svr);
			svn = new SvNote(svw);
			svr.dbSetAutoCommit(false);
			Gson gson = new Gson();
			JsonObject jsonData = null;
			Object aaa = null;
			String baba = "";
			JsonObject job = new JsonObject();
			job.addProperty("sessionId", "value");
			job.addProperty("tableName", "value");
			job.addProperty("actionName", "value");
			job.addProperty("subActionName", "value");
			job.addProperty("actionParam", "value");
			job.addProperty("dateOfMovement", "value");
			job.addProperty("dateOfAdmission", "value");
			job.addProperty("transporterPersonId", "value");
			job.addProperty("movementTransportType", "value");
			job.addProperty("transporterLicense", "value");
			job.addProperty("estmDateArrival", "value");
			job.addProperty("estmDateDeparture", "value");
			job.addProperty("disinfectionDate", "value");
			job.addProperty("animalMvmReason", "value");
			JsonArray jarr = new JsonArray();
			jarr.add(job);

			JsonObject job1 = new JsonObject();
			job1.addProperty("daut", "tuad");
			job1.addProperty("tuad", "puad");
			job1.addProperty("muad", "suad");
			JsonArray jarr1 = new JsonArray();
			jarr1.add(job1);

			JsonObject jobmain = new JsonObject();
			jobmain.add("objectArray", jarr);
			jobmain.add("params", jarr1);

			// JsonArray arr = jobmain.getAsJsonArray();
			// for (int i = 0; i < arr.size(); i++) {
			// JsonObject obj = arr.get(i).getAsJsonObject();
			// log4j.debug("IN FOR LOOP JSON OBJ: " + obj);
			// }

			log4j.debug("JSON_OBJECT: " + job);
			log4j.debug("JSON_ARRAY: " + jarr);
			log4j.debug("FINAL_JSON_OBJECT: " + jobmain);

			job = new JsonObject();
			job.addProperty("PARENT_ID", 532690L);
			job.addProperty("OBJECT_TYPE", 28569L);
			job.addProperty("STATUS", "VALID");
			job.addProperty("AUTO_GENERATED", "yes");
			job.addProperty("COUNTRY", "GE");
			job.addProperty("ANIMAL_ID", "23213213001");
			job.addProperty("ANIMAL_CLASS", "2");
			job.addProperty("ANIMAL_RACE", "222");
			job.addProperty("GENDER", "1");
			job.addProperty("BIRTH_DATE", "2020-01-29");
			job.addProperty("REGISTRATION_DATE", "2020-01-29");
			jarr = new JsonArray();
			jarr.add(job);
			wr.createAnimalObject(jarr, sessionId);

			// Response s = app.setNoteDescription(sessionId, 4767434L,
			// "GROUP_DESCRIPTION", Map, null);
			// aaa = s.getEntity();
			// baba = aaa.toString();
			log4j.debug(baba);
			log4j.debug(baba);
		} catch (SvException e) {
			e.printStackTrace();
		} finally {
			if (svr != null)
				svr.release();
		}
	}

	@Test
	public void testGetCustomPermissionsPerPackage() throws Exception {
		SvReader svr = null;
		SvWriter svw = null;
		SvNote svn = null;
		String sessionId = login("A.ADMIN3", "naits1234");
		MassActions ma = new MassActions();
		Writer wr = new Writer();
		Reader rdr = new Reader();
		UserManager um = new UserManager();
		String resultService = "";
		ApplicationServices ap = new ApplicationServices();
		try {
			svr = new SvReader(sessionId);
			Object aaa = null;
			String baba = "";

			// Response responseHtml = ap.getObjectSummary(svr.getSessionId(),
			// Tc.INVENTORY_ITEM, 2419968L);
			Response responseHtml = null;// ap.downloadSampleFile(svr.getSessionId(),
											// 10995433L, "", null);
			aaa = responseHtml.getEntity();
			baba = aaa.toString();

			log4j.debug(baba);
			// log4j.debug(rdr.getTransferAccordingInventoryItem(50715L, "4",
			// "90001326", Tc.DRAFT, null, svr).toSimpleJson());

		} catch (SvException e) {
			e.printStackTrace();
		} finally {
			if (svr != null)
				svr.release();
		}
		log4j.debug(resultService);
		log4j.info(resultService);
	}

	@Test
	public void testPostParams() throws Exception {
		SvReader svr = null;
		SvWriter svw = null;
		SvNote svn = null;
		String sessionId = login("A.ADMIN3", "naits1234");
		DateTime dt = new DateTime("1995-01-01");
		log4j.debug(String.valueOf(dt.getYear()).trim());
		MassActions ma = new MassActions();
		Writer wr = new Writer();
		Reader rdr = new Reader();
		ApplicationServices app = new ApplicationServices();
		try {
			svr = new SvReader(sessionId);
			svw = new SvWriter(svr);
			svn = new SvNote(svw);
			svr.dbSetAutoCommit(false);
			Object aaa = null;
			String baba = "";
			JsonObject job = new JsonObject();
			JsonArray jarr = new JsonArray();
			MultivaluedMap<String, String> Map = new MultivaluedHashMap<String, String>();
			Map.add(job.toString(), null);
			// svn.setNote(9257017L, "SUSPENSION_NOTE", "User didn't vote for
			// our party", true);
			Response s = app.massPetAction(sessionId, Map, null);
			aaa = s.getEntity();
			baba = aaa.toString();
			log4j.debug(baba);
		} catch (SvException e) {
			e.printStackTrace();
		} finally {
			if (svr != null)
				svr.release();
		}
		log4j.debug("{\"SV_ISLABEL\":true}");
	}

	@Test
	public void testQuarantineId() throws Exception {
		String date = "1942-03-11 00:00:00:".substring(0, 10).trim();
		File f = new File("./XCRMS.ClientSamples.exe");

		DateTime dt = new DateTime(date);
		SvReader svr = null;
		SvWriter svw = null;
		SvNote svn = null;
		String sessionId = login("A.ADMIN3", "naits1234");
		MassActions ma = new MassActions();
		Writer wr = new Writer();
		Reader rdr = new Reader();
		OnSaveValidations osv = new OnSaveValidations();
		ApplicationServices app = new ApplicationServices();
		try {
			svr = new SvReader(sessionId);
			svw = new SvWriter(svr);
			svn = new SvNote(svw);
			svr.dbSetAutoCommit(false);
			// log4j.debug(pr.getClass().getCanonicalName());
			DbDataObject dboQuarantine = svr.getObjectById(9266349L, SvReader.getTypeIdByName(Tc.QUARANTINE), null);
			dboQuarantine.toSimpleJson();
			log4j.debug(dboQuarantine.toSimpleJson());
			log4j.debug(dboQuarantine.toJson());
			log4j.debug(dboQuarantine.toJson());
			// String pom = rdr.generateQuarantineId(dboQuarantine, svr);
			// svw.saveObject(dboQuarantine);
		} catch (SvException e) {
			e.printStackTrace();
		} finally {
			if (svr != null)
				svr.release();
		}
		log4j.debug("{\"SV_ISLABEL\":true}");
	}

	@Test
	public void testCheckIfHoldingBelongsToActiveQuarantineOrAffectedArea() throws Exception {
		SvReader svr = null;
		String sessionId = login("A.ADMIN3", "naits1234");
		ApplicationServices app = new ApplicationServices();
		Reader rdr = new Reader();
		try {
			svr = new SvReader(sessionId);
			Object aaa = null;
			String baba = "";
			JsonObject job = new JsonObject();
			// DbDataObject dboHolding =
			// rdr.findAppropriateHoldingByPic("2117123000092", svr);
			Response s = app.getPetByPetPassportId(sessionId, "", "12341432");
			aaa = s.getEntity();
			baba = aaa.toString();
			log4j.debug(baba);
		} catch (SvException e) {
			e.printStackTrace();
		} finally {
			if (svr != null)
				svr.release();
		}
	}

	@Test
	public void recursionTest() throws Exception {
		SvReader svr = null;
		String sessionId = login("A.ADMIN3", "naits1234");
		ApplicationServices app = new ApplicationServices();
		UserManager um = new UserManager();
		Reader rdr = new Reader();
		try {
			svr = new SvReader(sessionId);
			DbDataObject dboOrgUnit = svr.getObjectById(9019296L, svCONST.OBJECT_TYPE_ORG_UNITS, null);
			DbDataArray dbArrOrgUnits = new DbDataArray();
			dbArrOrgUnits = rdr.getAppropriateParentOrgUnits(dbArrOrgUnits, dboOrgUnit, svr);
			dbArrOrgUnits.addDataItem(dboOrgUnit);
			log4j.debug(dbArrOrgUnits.toSimpleJson());
		} catch (SvException e) {
			e.printStackTrace();
		} finally {
			if (svr != null)
				svr.release();
		}
	}

	@Test
	public void testCustomPOALinking() throws Exception {
		SvReader svr = null;
		SvWriter svw = null;
		SvLink svl = null;
		String sessionId = login("A.ADMIN3", "naits1234");
		ApplicationServices app = new ApplicationServices();
		Reader rdr = new Reader();
		try {
			svr = new SvReader(sessionId);
			svr.setAutoCommit(false);
			svw = new SvWriter(svr);
			svl = new SvLink(svw);

			DbDataArray dbo = svr.getObjectsByTypeId(SvReader.getTypeIdByName(Tc.ORDER), null, 0, 0);
			for (int i = 0; i < dbo.size(); i++) {
				dbo.get(i).setVal(Tc.ORDER_NUMBER, String.valueOf(i));
				svw.saveObject(dbo.get(i), true);
			}
			log4j.debug("OK");
			DbDataObject dboUser = svr.getObjectById(9267804L, svCONST.OBJECT_TYPE_USER, null);// D.DASO
			DbDataObject dboLink = svr.getLinkType("CUSTOM_POA", svCONST.OBJECT_TYPE_USER,
					SvReader.getTypeIdByName(Tc.HOLDING));
			DbDataArray dboArr = svr.getObjectsByLinkedId(dboUser.getObject_id(), dboUser.getObject_type(), dboLink,
					SvReader.getTypeIdByName(Tc.HOLDING), false, null, 0, 0);
			// DbDataObject dboHolding = svr.getObjectById(4827027L,
			// SvReader.getTypeIdByName(Tc.HOLDING), null);

			svw.deleteObject(dboArr.get(0));
			svw.dbCommit();
			String num = "123142214";
			if (!NumberUtils.isDigits(num)) {
				log4j.debug("ISNOT");
			} else {
				log4j.debug("ITIS");
				try {
					Integer broj = Integer.valueOf(num);
				} catch (Exception e) {
					log4j.debug("I LIED");
				}
			}
			// svl.linkObjects(dboUser, dboHolding, "CUSTOM_POA", "", true);
			// svr.dbCommit();

		} catch (SvException e) {
			e.printStackTrace();
		} finally {
			if (svr != null)
				svr.release();
		}
	}

	@Test
	public void testGetObjectsWithILike() throws Exception {
		SvReader svr = null;
		String sessionId = login("D.DASO", "naits1234");
		ApplicationServices app = new ApplicationServices();
		Reader rdr = new Reader();
		try {
			svr = new SvReader(sessionId);
			Object temp = null;
			String responseResult = "";
			Response rep = app.getTableWithILike(svr.getSessionId(), "HOLDING", "PIC", "151", 10000, null);
			temp = rep.getEntity();
			responseResult = temp.toString();
			if (responseResult.equals("") || responseResult.equals("[]")) {
				fail();
			} else {
				log4j.debug(responseResult);
			}
		} catch (SvException e) {
			e.printStackTrace();
		} finally {
			if (svr != null)
				svr.release();
		}
	}

	@Test
	public void testLocks() throws Exception {
		SvReader svr = null;
		String sessionId = login("A.ADMIN3", "naits1234");
		ApplicationServices app = new ApplicationServices();
		Reader rdr = new Reader();
		try {
			svr = new SvReader(sessionId);
			DbSearchCriterion cr1 = new DbSearchCriterion(Tc.EVENT_END, DbCompareOperand.GREATER_EQUAL, new DateTime());
			DbDataArray arr = rdr.getValidVaccEvents(svr, null);
			log4j.debug(arr.toSimpleJson());
			java.util.logging.Logger logger = java.util.logging.Logger.getLogger("MyLog");
			FileHandler fh;
			try {
				// This block configure the logger with handler and formatter
				fh = new FileHandler("/home/daut/git/naits_triglav_plugin/custom_log.log");
				logger.addHandler(fh);
				SimpleFormatter formatter = new SimpleFormatter();
				fh.setFormatter(formatter);
				// the following statement is used to log any messages
				logger.info("testtest");
			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} catch (SvException e) {
			e.printStackTrace();
		} finally {
			if (svr != null)
				svr.release();
		}
	}

	@Test
	public void testStrayPet() throws Exception {
		SvReader svr = null;
		SvWriter svw = null;
		String sessionId = login("A.ADMIN3", "naits1234");
		ApplicationServices app = new ApplicationServices();
		Reader rdr = new Reader();
		ValidationChecks vc = new ValidationChecks();
		try {
			svr = new SvReader(sessionId);
			svw = new SvWriter(svr);
			DbDataObject dboStrayPet = new DbDataObject();
			dboStrayPet.setObject_type(SvReader.getTypeIdByName(Tc.STRAY_PET));
			dboStrayPet.setVal(Tc.PET_ID, "8291897213");
			dboStrayPet.setVal(Tc.PET_TYPE, "1");

			dboStrayPet.setVal(Tc.DT_EUTHANASIA, new DateTime());
			dboStrayPet.setVal(Tc.DT_ADOPTION, new DateTime().plusDays(1));
			svw.saveObject(dboStrayPet);

		} catch (SvException e) {
			e.printStackTrace();
		} finally {
			if (svr != null)
				svr.release();
		}
	}

	public static DbDataObject getHoldingRespByPn(String pn, SvReader svr) throws SvException {
		DbDataObject dbo = null;
		DbSearchCriterion cr1 = new DbSearchCriterion("NAT_REG_NUMBER", DbCompareOperand.EQUAL, pn);
		DbDataArray arr = svr.getObjects(new DbSearchExpression().addDbSearchItem(cr1),
				SvReader.getTypeIdByName(Tc.HOLDING_RESPONSIBLE), null, 0, 0);
		if (!arr.getItems().isEmpty())
			dbo = arr.get(0);
		return dbo;
	}

	@Test
	public void testHoldingResponsibleFromProduction() throws Exception {
		SvReader svr = null;
		SvWriter svw = null;
		String sessionId = login("R.PEJOV", "naits1234");
		ApplicationServices app = new ApplicationServices();
		Reader rdr = new Reader();
		ValidationChecks vc = new ValidationChecks();
		try {
			svr = new SvReader(sessionId);
			svr.setAutoCommit(false);
			svw = new SvWriter(svr);
			DbDataObject dboHoldingResp = getHoldingRespByPn("32001001426", svr);
			DbDataObject dboHoldingResp1 = getHoldingRespByPn("28001031703", svr);
			DbDataObject dboHoldingResp2 = getHoldingRespByPn("3001005189", svr);
			DbDataObject dboHoldingResp3 = getHoldingRespByPn("00010402865", svr);
			DbDataObject dboHoldingResp4 = getHoldingRespByPn("10001061083", svr);
			DbDataArray arr = new DbDataArray();
			arr.addDataItem(dboHoldingResp);
			arr.addDataItem(dboHoldingResp1);
			arr.addDataItem(dboHoldingResp2);
			arr.addDataItem(dboHoldingResp3);
			arr.addDataItem(dboHoldingResp4);
			int counter = 0;
			DbDataArray temp = new DbDataArray();
			for (DbDataObject dbo : arr.getItems()) {
				dbo.setVal("IS_PROCESSED", false);
				temp.addDataItem(dbo);
				counter++;
				if (counter == 5) {
					svw.saveObject(temp, true, true);
				}
			}

		} catch (SvException e) {
			e.printStackTrace();
			log4j.error(e.getMessage());
		} finally {
			if (svr != null)
				svr.release();
		}
	}

	@Test
	public void testLogger1() throws Exception {
		SvReader svr = null;
		String sessionId = login("A.ADMIN3", "naits1234");
		ApplicationServices app = new ApplicationServices();
		Reader rdr = new Reader();
		try {
			svr = new SvReader(sessionId);
			String codeVal = "29305931";
			String grVillageName = rdr.decodeCodeValue(SvReader.getTypeIdByName(Tc.HOLDING), Tc.VILLAGE_CODE, codeVal,
					"ka_GE", svr);
			DbDataArray temp2 = rdr.searchForDependentMunicCommunVillage(codeVal, Tc.VILLAGES, svr);
			DbSearchCriterion cr2 = new DbSearchCriterion("ORG_UNIT_TYPE", DbCompareOperand.EQUAL,
					"MUNICIPALITY_OFFICE");
			DbDataArray arr2 = svr.getObjects(new DbSearchExpression().addDbSearchItem(cr2),
					svCONST.OBJECT_TYPE_ORG_UNITS, null, 0, 0);
			log4j.debug(arr2.toSimpleJson());
			arr2.getSortedItems("EXTERNAL_ID");
			for (DbDataObject dbo : arr2.getItems()) {
				log4j.debug(dbo.getVal("EXTERNAL_ID"));
			}
			log4j.debug("next");
			int i = arr2.size() - 1;
			while (i > 0) {
				DbDataObject dbo = arr2.get(i);
				log4j.debug(dbo.getVal("EXTERNAL_ID"));
				i--;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		svr.release();
	}

	@Test
	public void testPassportRequest() throws Exception {
		SvReader svr = null;
		String sessionId = login("A.ADMIN3", "naits1234");
		ApplicationServices app = new ApplicationServices();
		Reader rdr = new Reader();
		try {
			svr = new SvReader(sessionId);
			DbDataObject dboPet = svr.getObjectById(9263199L, SvReader.getTypeIdByName(Tc.PET), null);
			DbDataObject dboVeterinaryStation = svr.getObjectById(9267753L, SvReader.getTypeIdByName(Tc.HOLDING), null);
			Object temp = null;
			Response appResponse = app.sendPassportRequestToVeterinaryStation(svr.getSessionId(), dboPet.getObject_id(),
					dboVeterinaryStation.getObject_id());
			temp = appResponse.getEntity();
			String res = temp.toString();
			if (!res.equals("naits.success.sentPassportRequestPerPet")) {
				Assert.fail();
			}
			log4j.debug(res);
		} catch (Exception e) {
			e.printStackTrace();
		}
		svr.release();
	}

	@Test
	public void testStrayPetSummaryInfo() throws Exception {
		SvReader svr = null;
		String sessionId = login("A.ADMIN3", "naits1234");
		ApplicationServices app = new ApplicationServices();
		Reader rdr = new Reader();
		Writer wr = new Writer();
		try {
			svr = new SvReader(sessionId);
			DbDataObject dboPet = svr.getObjectById(10553832L, SvReader.getTypeIdByName(Tc.STRAY_PET), null);
			log4j.debug(wr.generateRequestId("12332312", svr));
			DbDataObject dboVeterinaryStation = svr.getObjectById(9267753L, SvReader.getTypeIdByName(Tc.HOLDING), null);
			Object temp = null;
			Response appResponse = app.getObjectSummary(svr.getSessionId(), Tc.STRAY_PET, dboPet.getObject_id());
			temp = appResponse.getEntity();
			String res = temp.toString();
			log4j.debug(res);
		} catch (Exception e) {
			e.printStackTrace();
		}
		svr.release();
	}

	@Test
	public void testHoldingResponsible() throws Exception {
		SvReader svr = null;
		SvWriter svw = null;
		String sessionId = login("R.PEJOV", "naits1234");
		ApplicationServices app = new ApplicationServices();
		Reader rdr = new Reader();
		Writer wr = new Writer();
		try {
			svr = new SvReader(sessionId);
			svw = new SvWriter(svr);

			// DbDataObject dboNote = new DbDataObject();
			// dboNote.setObject_type(svCONST.OBJECT_TYPE_NOTES);
			// dboNote.setParent_id(0L);
			// dboNote.setVal(Tc.NOTE_NAME, "KEY_STORE");
			// dboNote.setVal(Tc.NOTE_TEXT,
			// "KeyId:2533015,KeyByte:086EA94B7FD7B5893020E0014A72F0F1B957E5AFAD7A2AD68A7E33F5C38B396F,Timestamp:637243640996169993");
			// svw.saveObject(dboNote, true);
			PublicRegistry pr = new PublicRegistry();
			DbDataObject dboNote = rdr.getNotesAccordingParentIdAndNoteName(0L, "KEY_STORE", svr).get(0);
			Document dbo = pr.getNormalizedXMLFile("/home/daut/KeyStorage.xml");
			// boolean bbb = pr.test(dbo, svr);

			// DbDataObject dbo =
			// rdr.searchForObject(SvReader.getTypeIdByName(Tc.HOLDING_RESPONSIBLE),
			// "NAT_REG_NUMBER",
			// "11001023908", svr);
			// svw.saveObject(dbo, false);
			log4j.debug("OK");
		} catch (Exception e) {
			e.printStackTrace();
		}
		svr.release();
	}

	@Test
	public void testTicksToTimestamp() throws Exception {
		Long TICKS_AT_EPOCH = 621355968000000000L;
		Long TICKS_PER_MILLISECOND = 10000L;
		DateTime dtNow = new DateTime();
		System.out.println("Current time" + dtNow);
		System.out.println(dtNow.getMillis());
		DateTime dt = new DateTime(dtNow.getMillis());
		System.out.println(dt);
		long ticks = 637243620931897232L;
		// valid
		Date date = new Date((ticks - TICKS_AT_EPOCH) / TICKS_PER_MILLISECOND);

		System.out.println(date);

		DateTime dtR = new DateTime(date);
		System.out.println("MILLIS OF DAY: " + dt.getMillis() + "  TICKS: " + ticks);
		System.out.println(dt);
		System.out.println(dt.minusDays(2).toDate());

		Calendar calendar = Calendar.getInstance();
		calendar.setTime(dt.minusHours(16).minusMinutes(1).toDate());
		long temp = (calendar.getTimeInMillis() * TICKS_PER_MILLISECOND) + TICKS_AT_EPOCH;
		Date date1 = new Date((temp - TICKS_AT_EPOCH) / TICKS_PER_MILLISECOND);
		System.out.println(date1);
		System.out.println(temp);
	}

	@Test
	public void testGeneraitingPreMortemWithSlaughterhousePermissions() throws Exception {
		SvReader svr = null;
		SvWriter svw = null;
		String sessionId = login("A.ADMIN3", "naits1234");
		ApplicationServices app = new ApplicationServices();
		Reader rdr = new Reader();
		ValidationChecks vc = new ValidationChecks();
		MassActions ma = new MassActions();
		try {
			svr = new SvReader(sessionId);
			svr.setAutoCommit(false);
			svw = new SvWriter(svr);
			// OnSaveValidations call = new OnSaveValidations();
			// SvCore.registerOnSaveCallback(call,
			// SvReader.getTypeIdByName(Tc.HOLDING_RESPONSIBLE));
			DbDataObject dboHoldingResp = rdr.getDboPersonByPrivateNumber("41001018250", svr);

			DbDataObject dboHoldingResp1 = rdr.getDboPersonByPrivateNumber("41001018250", svr);
			dboHoldingResp1.setVal(Tc.REGION_CODE, "15");
			// dboHoldingResp.setVal(Tc.ADDRESS, "edited by me");
			// svw.saveObject(dboHoldingResp, true);

			// if(rdr.checkIfObjectHasBeenEditedDependOnSpecificFields(dboHoldingResp,
			// dboHoldingResp1, "NAT_REG_NUMBER", Tc.REGION_CODE)) {
			// fail();
			// }

			// DbDataObject dboAnimal =
			// rdr.findAppropriateAnimalByAnimalId("10931238", svr);
			//
			// JsonArray jArr = new JsonArray();
			//
			// jArr.add(dboAnimal.toSimpleJson());
			// // jArr.add(dboTransfer2.toSimpleJson());
			//
			// //
			// ANIMAL/other/GENERATE_PREMORTEM/null/null/null/null/null/null/null/null/null/null
			//
			// String pom = ma.animalFlockMassHandler("ANIMAL", "other",
			// "GENERATE_PREMORTEM", null, null, null, null,
			// null, null, null, null, null, null, jArr, svr.getSessionId());
			// log4j.debug(pom);
			svw.dbCommit();
		} catch (SvException e) {
			e.printStackTrace();
			log4j.error(e.getMessage());
		} finally {
			if (svr != null)
				svr.release();
		}
	}

	@Test
	public void testAssignPermissionOnNonexistingTable() throws Exception {
		SvReader svr = null;
		SvWriter svw = null;
		SvSecurity svs = null;
		String sessionId = login("R.PEJOV", "naits1234");
		ApplicationServices app = new ApplicationServices();
		Reader rdr = new Reader();
		ValidationChecks vc = new ValidationChecks();
		MassActions ma = new MassActions();
		UserManager um = new UserManager();
		try {
			svr = new SvReader(sessionId);
			svr.setAutoCommit(false);
			svw = new SvWriter(svr);
			SvLink svl = new SvLink(svw);
			svs = new SvSecurity(svl);
			// SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
			// Date date = formatter.parse("2015-05-05");
			// DbDataObject dboUser = svr.getObjectById(11023485L,
			// svCONST.OBJECT_TYPE_USER, null);

			DbSearchCriterion cr1 = new DbSearchCriterion(Tc.USER_NAME, DbCompareOperand.EQUAL, "D.CHEKOVIKJ");
			DbDataObject dboUser = svr
					.getObjects(new DbSearchExpression().addDbSearchItem(cr1), svCONST.OBJECT_TYPE_USER, null, 0, 0)
					.get(0);

			DbDataObject dboOrgUnit = svr.getObjectById(49L, svCONST.OBJECT_TYPE_ORG_UNITS, null);
			DbDataObject dbLinkType = SvLink.getLinkType(Tc.POA, dboUser.getObject_type(), dboOrgUnit.getObject_type());
			DbDataObject dbLinkObj = rdr.getLinkObject(dboUser.getObject_id(), dboOrgUnit.getObject_id(),
					dbLinkType.getObject_id(), svr);
			if (dbLinkObj == null) {
				svl.linkObjects(dboUser, dboOrgUnit, Tc.POA, null, true, true);
			}

			JsonElement dboUserJson = dboUser.toSimpleJson();

			JsonArray jArr = new JsonArray();

			jArr.add(dboUserJson);

			String pom = um.attachUserToGroup("ADMINISTRATORS", jArr, svr.getSessionId());
			log4j.debug(pom);
			// log4j.debug(date);
			ArrayList<String> filteredPackagePermissions = new ArrayList<>();
			// filteredPackagePermissions.add("custom.statistical_report");
			filteredPackagePermissions.add("HOLDING.FULL");

			String responseRslt = null;

			// Response response =
			// app.checkIfUserCanUseStatisticalReportTool(sessionId);

			// responseRslt = response.getEntity().toString();

			um.processPermissionsToUserOrGroup("ASSIGN", svr.getInstanceUser(), null, filteredPackagePermissions, false,
					svr, svw, svs);
			svw.dbCommit();
		} catch (SvException e) {
			log4j.error(e.getMessage());
			fail();
		} finally {
			if (svr != null)
				svr.release();
			if (svw != null)
				svw.release();
			if (svs != null)
				svs.release();
		}
	}

	@Test
	public void testUpdatingPetId() throws Exception {
		SvReader svr = null;
		SvWriter svw = null;
		String sessionId = login("A.ADMIN3", "naits1234");
		ApplicationServices app = new ApplicationServices();
		Reader rdr = new Reader();
		ValidationChecks vc = new ValidationChecks();
		MassActions ma = new MassActions();
		try {
			svr = new SvReader(sessionId);
			svr.setAutoCommit(false);
			svw = new SvWriter(svr);
			OnSaveValidations call = new OnSaveValidations();
			SvCore.registerOnSaveCallback(call, SvReader.getTypeIdByName(Tc.PET));
			SvCore.registerOnSaveCallback(call, SvReader.getTypeIdByName(Tc.INVENTORY_ITEM));

			Object aaa = null;
			String baba = "";
			Response responseHtml = app.updatePetId(svr.getSessionId(), 10625644L, "2332221");
			aaa = responseHtml.getEntity();
			baba = aaa.toString();
			if (!baba.equals("naits.success.updatePetId")) {
				fail();
			}
			svw.dbCommit();
		} catch (SvException e) {
			e.printStackTrace();
			log4j.error(e.getMessage());
			fail();
		} finally {
			if (svr != null)
				svr.release();
			if (svw != null)
				svw.release();
		}
	}

	@Test
	public void testCreateCustomPermissionAcl() throws Exception {
		SvReader svr = null;
		SvWriter svw = null;
		String sessionId = login("R.PEJOV", "naits1234");
		ApplicationServices app = new ApplicationServices();
		Reader rdr = new Reader();
		ValidationChecks vc = new ValidationChecks();
		MassActions ma = new MassActions();
		try {
			svr = new SvReader(sessionId);
			svr.setAutoCommit(false);
			svw = new SvWriter(svr);
			DbDataObject dboAcl = new DbDataObject();
			dboAcl.setObject_type(svCONST.OBJECT_TYPE_ACL);
			dboAcl.setVal("ACCESS_TYPE", "EXECUTE");
			dboAcl.setVal("ACL_OBJECT_ID", 0L);
			dboAcl.setVal("ACL_OBJECT_TYPE", 50L);
			dboAcl.setVal("ACL_CONFIG_UNQ", "custom.statistical_report");
			dboAcl.setVal("LABEL_CODE", "custom.statistical_report");
			svw.saveObject(dboAcl, false);
			svw.dbCommit();
		} catch (SvException e) {
			e.printStackTrace();
			log4j.error(e.getMessage());
		} finally {
			if (svr != null)
				svr.release();
		}
	}

	@Test
	public void testPdfAndExcelGenerating() throws Exception {
		SvReader svr = null;
		SvWriter svw = null;
		String sessionId = login("A.ADMIN3", "naits1234");
		Reader rdr = new Reader();
		ValidationChecks vc = new ValidationChecks();
		Writer wr = new Writer();
		MassActions ma = new MassActions();
		Report rp = new Report();
		try {
			svr = new SvReader(sessionId);
			svw = new SvWriter(svr);
			// rp.generatePdfOrExcel(svr.getSessionId(), "15311111", Tc.PDF,
			// "2020-04-04", null, "STAT_ASBV", null);

			//

			DbDataObject dboCollectionLoc = wr.createStrayPetLocationWithoutGPSCoordinates("1", "", null, null, null,
					null, 10628997L);
			svw.saveObject(dboCollectionLoc, false);
			// Release location
			DbDataObject dboReleaseLoc = wr.createStrayPetLocationWithoutGPSCoordinates("2", "", null, null, null, null,
					10628997L);
			svw.saveObject(dboReleaseLoc, false);
			svw.dbCommit();

		} catch (SvException e) {
			e.printStackTrace();
			log4j.error(e.getMessage());
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
	}

	@Test
	public void testPetArchiveId() throws Exception {
		SvReader svr = null;
		SvWriter svw = null;
		String sessionId = login("R.PEJOV", "naits1234");
		Reader rdr = new Reader();
		ValidationChecks vc = new ValidationChecks();
		Writer wr = new Writer();
		MassActions ma = new MassActions();
		Report rp = new Report();
		try {
			svr = new SvReader(sessionId);
			svw = new SvWriter(svr);

			// DbDataObject dboPet = svr.getObjectById(10622185L,
			// SvReader.getTypeIdByName(Tc.PET), null);
			// wr.generateArchiveId(dboPet, svr);
			createParamType("param.pet_archive_id", false, svw);
			svw.dbCommit();
		} catch (SvException e) {
			e.printStackTrace();
			log4j.error(e.getMessage());
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
	}

	@Test
	public void testDateTimeGetProperties() throws Exception {
		// SvReader svr = null;
		// SvWriter svw = null;
		// String sessionId = login("R.PEJOV", "naits1234");
		// ApplicationServices app = new ApplicationServices();
		// Reader rdr = new Reader();
		// ValidationChecks vc = new ValidationChecks();
		// Writer wr = new Writer();
		// MassActions ma = new MassActions();
		// Report rp = new Report();
		try {
			// svr = new SvReader(sessionId);
			// svw = new SvWriter(svr);
			// svw.setAutoCommit(false);

			log4j.debug("DAY" + new DateTime().getDayOfMonth());
			log4j.debug("MONTH" + new DateTime().getMonthOfYear());
			log4j.debug("YEAR" + new DateTime().getYear());

			String schema = SvConf.getDefaultSchema();
			Long field1 = null;
			Long field1b = null;
			field1b = 1l;
			Long field2 = null;
			field2 = 1l;
			Long field3 = null;
			Long field3b = null;
			Long field3bb = null;
			field3bb = 1l;
			Long field4 = null;
			Long field4b = null;
			Long field4bb = null;
			field4bb = 1l;
			Long field5 = null;
			field5 = 1l;
			String matView1 = schema + ".holding_population_age_filter hpaf";
			String matView2 = schema + ".holding_population_gender_filter hpgf";
			String matView3 = schema + ".animal_population_disease_filter apdf";
			String matView4 = schema + ".animal_population_sample_filter apsf";
			String matView5 = schema + ".animal_population_inventory_filter apif";
			ArrayList<String> arrJoins = new ArrayList<>();
			LinkedHashMap<String, String> lhm = new LinkedHashMap<>();
			if (field1b != null) {
				arrJoins.add(matView1);
				lhm.put(matView1, "hpaf");
			}
			if (field2 != null) {
				arrJoins.add(matView2);
				lhm.put(matView2, "hpgf");
			}
			if (field3bb != null) {
				arrJoins.add(matView3);
				lhm.put(matView3, "apdf");
			}
			if (field4bb != null) {
				arrJoins.add(matView4);
				lhm.put(matView4, "apsf");
			}
			if (field5 != null) {
				arrJoins.add(matView5);
				lhm.put(matView5, "apif");
			}
			StringBuilder query = new StringBuilder();
			query.append("SELECT * FROM ");
			StringBuilder whereClause = new StringBuilder();
			whereClause.append("WHERE ");
			int i = 0;
			for (Map.Entry<String, String> mView : lhm.entrySet()) {
				i++;
				query.append(mView.getKey());
			}

			if (lhm.size() == 1) {

			}

			for (Map.Entry<String, String> mView : lhm.entrySet()) {
				i++;
				if (lhm.entrySet().size() > i) {
					query.append(" JOIN ");
				}
			}

			log4j.debug(query.toString());
		} catch (Exception e) {
			e.printStackTrace();
			log4j.error(e.getMessage());
		} finally {
			// if (svr != null) {
			// svr.release();
			// }
		}
	}

	@Test
	public void testUsersWithDefaultUserGroup() throws Exception {
		SvReader svr = null;
		SvWriter svw = null;
		String sessionId = login("R.PEJOV", "naits1234");
		Reader rdr = new Reader();
		ValidationChecks vc = new ValidationChecks();
		Writer wr = new Writer();
		MassActions ma = new MassActions();
		Report rp = new Report();
		try {
			svr = new SvReader(sessionId);
			svw = new SvWriter(svr);
			// DbDataObject dboOrgUnit =
			// rdr.getOrgUnitDependOnParentExternalId(15l, 16l, svr).get(0);
			DbDataObject dboUserDaut = svr.getObjectById(10628535L, svCONST.OBJECT_TYPE_USER, null);

			// wr.linkObjects(dboUserDaut, dboOrgUnit, "CUSTOM_POA", "test 1",
			// svr);
			// svw.dbCommit();

			// LINK CONFIG
			DbDataObject dboLinkBetweenUserAndUserGroup = svr.getLinkType("USER_DEFAULT_GROUP",
					svCONST.OBJECT_TYPE_USER, svCONST.OBJECT_TYPE_GROUP);

			// DATA ENTRY GROUP //CVIRO //FVIRO
			DbSearchCriterion cr2 = new DbSearchCriterion(Tc.GROUP_TYPE, DbCompareOperand.EQUAL, "DATA_ENTRY_CLERK");
			DbDataObject dboUserGroup = svr
					.getObjects(new DbSearchExpression().addDbSearchItem(cr2), svCONST.OBJECT_TYPE_GROUP, null, 0, 0)
					.get(0);
			DbDataArray arr = svr.getObjectsByLinkedId(dboUserGroup.getObject_id(), dboUserGroup.getObject_type(),
					dboLinkBetweenUserAndUserGroup, svCONST.OBJECT_TYPE_USER, true, null, 0, 0);

			// USERS GROUP
			DbSearchCriterion cr1e = new DbSearchCriterion(Tc.GROUP_NAME, DbCompareOperand.EQUAL, "USERS");
			DbSearchCriterion cr2e = new DbSearchCriterion(Tc.GROUP_TYPE, DbCompareOperand.EQUAL, "USERS");
			DbDataObject dboUserGroupUsers = svr
					.getObjects(new DbSearchExpression().addDbSearchItem(cr1e).addDbSearchItem(cr2e),
							svCONST.OBJECT_TYPE_GROUP, null, 0, 0)
					.get(0);
			log4j.debug(arr.size());
			for (DbDataObject dboUser : arr.getItems()) {
				DbDataObject dboLink = rdr.getLinkObject(dboUser.getObject_id(), dboUserGroup.getObject_id(),
						dboLinkBetweenUserAndUserGroup.getObject_id(), svr);
				if (dboLink != null) {
					wr.invalidateLink(dboLink, false, svr);
				}
				wr.linkObjects(dboUser, dboUserGroup, "USER_GROUP", null, svr);
				wr.linkObjects(dboUser, dboUserGroupUsers, "USER_DEFAULT_GROUP", null, svr);
			}
			svw.dbCommit();
		} catch (SvException e) {
			e.printStackTrace();
			log4j.error(e.getMessage());
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
	}

	@Test
	public void testBuildingQUeriesAccordingPopulationFilters() throws Exception {
		SvReader svr = null;
		SvWriter svw = null;
		String sessionId = login("R.PEJOV", "naits1234");
		Reader rdr = new Reader();
		ValidationChecks vc = new ValidationChecks();
		Writer wr = new Writer();
		MassActions ma = new MassActions();
		Report rp = new Report();
		try {
			svr = new SvReader(sessionId);
			svw = new SvWriter(svr);
			svw.dbSetAutoCommit(false);

			DbDataObject dboPopulation = svr.getObjectById(10995190L, SvReader.getTypeIdByName(Tc.POPULATION), null);
			StringBuilder query = PopulationQueryBuilder.getQueryAccordingPopulationFilters(dboPopulation);
			String queryWithCustomSelect = PopulationQueryBuilder.buildSimpleQueryWithSubquery("NAITS.VANIMAL", Tc.PKID,
					"IN", query, "NAITS.GET_LABEL_TEXT_PER_VALUE(STATUS,'OBJ_STATUS','EN_US') STATUS", "ANIMAL_ID",
					"BIRTH_DATE", "REGISTRATION_DATE",
					"NAITS.GET_LABEL_TEXT_PER_VALUE(ANIMAL_CLASS,'ANIMAL_CLASS','EN_US')ANIMAL_CLASS",
					"NAITS.GET_LABEL_TEXT_PER_VALUE(ANIMAL_RACE,'ANIMAL_RACE','EN_US')ANIMAL_RACE",
					"NAITS.GET_LABEL_TEXT_PER_VALUE(GENDER,'GENDER','EN_US')GENDER",
					"NAITS.GET_LABEL_TEXT_PER_VALUE(COLOR,'COLOR','EN_US')COLOR",
					"NAITS.GET_LABEL_TEXT_PER_VALUE(COUNTRY,'COUNTRY','EN_US')COUNTRY",
					"NAITS.GET_LABEL_TEXT_PER_VALUE(COUNTRY_OLD_ID,'COUNTRY','EN_US')COUNTRY_OLD_ID", "EXTERNAL_ID",
					"MOTHER_TAG_ID", "FATHER_TAG_ID");
			log4j.debug(queryWithCustomSelect);
			Map<String, Object> mp = new LinkedHashMap<String, Object>();
			mp.put("ANIMAL_ID", "223305");
			mp.put("OBJECT_ID", 213231123L);
			// String pom1 = pqb.buildSimpleQuery("NAITS.VANIMAL", "VA", mp,
			// "*");
			// log4j.debug(pom1);

		} catch (SvException e) {
			e.printStackTrace();
			log4j.error(e.getMessage());
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
	}

	@Test
	public void testLinkAreaWithPopulation() throws Exception {
		SvReader svr = null;
		SvWriter svw = null;
		String sessionId = login("M.ZHIVKOVIKJ", "naits1234");
		Reader rdr = new Reader();
		ValidationChecks vc = new ValidationChecks();
		Writer wr = new Writer();
		MassActions ma = new MassActions();
		Report rp = new Report();
		try {
			svr = new SvReader(sessionId);
			svw = new SvWriter(svr);
			svw.dbSetAutoCommit(false);

			// Response r1 =
			// app.createLinkBetweenAreaAndPopulation(svr.getSessionId(),
			// 10995190L, "2932");
			// Response r =
			// app.createLinkBetweenAreaAndPopulation(svr.getSessionId(),
			// 10995190L, "29");
			// Object o = new Object();
			// o = r.getEntity();
			// String s = o.toString();
			// log4j.debug(s);

			DbDataObject dboPopulation = svr.getObjectById(11031296L, SvReader.getTypeIdByName(Tc.POPULATION), null);
			DbDataObject dboPopulationHolding = svr.getObjectById(11002994L, SvReader.getTypeIdByName(Tc.POPULATION),
					null);
			DbDataObject dboPopulation2 = svr.getObjectById(13382863L, SvReader.getTypeIdByName(Tc.POPULATION), null);

			wr.createExcelFileForSampleState(dboPopulation2, "my_sheet_bro", null, svr);
			// String pom =
			// PopulationQueryBuilder.buildQueryAccordingGeolocationFilters(dboPopulation,
			// svr).toString();
			// log4j.debug(pom);
		} catch (SvException e) {
			e.printStackTrace();
			log4j.error(e.getMessage());
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
	}

	public String buildSimpleQuery(String tableName, String tableInstanceName,
			Map<String, Object> filterColumnNameValue, String... selectColumns) {
		String resultQuery = "";
		StringBuilder select = new StringBuilder("SELECT ");
		StringBuilder from = new StringBuilder(" FROM ");
		StringBuilder where = new StringBuilder(" WHERE ");
		for (int i = 0; i < selectColumns.length; i++) {
			select.append(selectColumns[i]);
			if (i != selectColumns.length - 1) {
				select.append(", ");
			}
		}
		from.append(tableName).append(" ").append(tableInstanceName);
		for (Entry<String, Object> field : filterColumnNameValue.entrySet()) {
			String value = "";
			if (field.getValue() instanceof String) {
				value = "'" + field.getValue() + "'";
			}
			where.append(field.getKey()).append("=").append(value);
		}
		return resultQuery;
	}

	/**
	 * Set of validations for Pet movement object
	 * 
	 * @param dbo
	 * @param rdr
	 * @param svr
	 * @throws SvException
	 */
	public void populationValidationSet(DbDataObject dbo, SvReader svr) throws SvException {
		String schema = SvConf.getDefaultSchema();
		Long field1 = null;
		Long field1b = null;
		field1b = 1l;
		Long field2 = null;
		field2 = 1l;
		Long field3 = null;
		Long field3b = null;
		Long field3bb = null;
		field3bb = 1l;
		Long field4 = null;
		Long field4b = null;
		Long field4bb = null;
		field4bb = 1l;
		Long field5 = null;
		field5 = 1l;
		String matView1 = schema + "holding_population_age_filter hpaf";
		String matView2 = schema + "holding_population_gender_filter hpgf";
		String matView3 = schema + "animal_population_disease_filter apdf";
		String matView4 = schema + "animal_population_sample_filter apsf";
		String matView5 = schema + "animal_population_inventory_filter apif";
		ArrayList<String> arrJoins = new ArrayList<>();
		LinkedHashMap<String, String> lhm = new LinkedHashMap<>();
		if (field1b != null) {
			arrJoins.add(matView1);
			lhm.put(matView1, "hpaf");
		}
		if (field2 != null) {
			arrJoins.add(matView2);
			lhm.put(matView2, "hpgf");
		}
		if (field3bb != null) {
			arrJoins.add(matView3);
			lhm.put(matView3, "apdf");
		}
		if (field4bb != null) {
			arrJoins.add(matView4);
			lhm.put(matView4, "apsf");
		}
		if (field5 != null) {
			arrJoins.add(matView5);
			lhm.put(matView5, "apif");
		}
		StringBuilder query = new StringBuilder();
		query.append("SELECT * FROM ");
		StringBuilder whereClause = new StringBuilder();
		whereClause.append("WHERE ");
		int i = 0;
		for (Map.Entry<String, String> mView : lhm.entrySet()) {
			i++;
			query.append(mView.getKey());
			whereClause.append(mView.getValue()).append(".OBJECT_ID");
			if (lhm.entrySet().size() > i) {
				query.append(" JOIN ");
			}
		}
	}

	@Test
	public void testForFikjo() {
		SvReader svr = null;
		SvWriter svw = null;
		SvFileStore svfs = null;
		Reader rdr = new Reader();
		String sessionId = login("A.ADMIN3", "naits1234");
		try {
			svr = new SvReader(sessionId);
			svw = new SvWriter(svr);
			svfs = new SvFileStore(svr);
			DbDataObject dboStratificationObject = new DbDataObject();
			dboStratificationObject = new DbDataObject();
			dboStratificationObject.setObject_type(SvReader.getTypeIdByName(Tc.STRAT_FILTER));
			dboStratificationObject.setStatus(Tc.VALID);
			dboStratificationObject.setParent_id(11003004L);
			dboStratificationObject.setVal(Tc.NUM_REGIONS, 5L);
			dboStratificationObject.setVal(Tc.NUM_MUNICS, 15L);
			dboStratificationObject.setVal(Tc.NUM_COMMUNS, 20L);
			dboStratificationObject.setVal(Tc.NUM_VILLAGES, 40L);
			dboStratificationObject.setVal(Tc.NUM_HOLDINGS, 30L);
			svw.saveObject(dboStratificationObject, true);

			DbDataObject dboPopulation = svr.getObjectById(11003004L, SvReader.getTypeIdByName(Tc.POPULATION), null);
			DbDataObject dboStratFilter = svr.getObjectsByParentId(dboPopulation.getObject_id(),
					SvReader.getTypeIdByName(Tc.STRAT_FILTER), null, 0, 0).get(0);
			ArrayList<String> pom = rdr.getRandomGeoUnitsByStratificationFilter(dboStratFilter, svr);
			log4j.debug(pom);
			log4j.debug(pom.size());

			pom.toString();

			log4j.debug(rdr.getDistinctCountOfGeolocationsAccordingGeostatCode(dboPopulation, Tc.VILLAGE_CODE, svr));

			DbDataObject dboLinkBetweenPopulationAndFile = SvReader.getLinkType(Tc.LINK_FILE,
					dboPopulation.getObject_type(), svCONST.OBJECT_TYPE_FILE);

			DbDataArray arrFiles = svr.getObjectsByLinkedId(dboPopulation.getObject_id(),
					dboLinkBetweenPopulationAndFile, null, 0, 0);

			InputStream is = svfs.getFileAsStream(arrFiles.get(0));

			XSSFWorkbook workbook = new XSSFWorkbook(is);
			for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
				XSSFSheet sheet = workbook.getSheetAt(i);
				// Iterate through each rows one by one
				Iterator<Row> rowIterator = sheet.iterator();
				while (rowIterator.hasNext()) {
					Row row = rowIterator.next();
					// For each row, iterate through all the columns
					Iterator<Cell> cellIterator = row.cellIterator();

					while (cellIterator.hasNext()) {
						Cell cell = cellIterator.next();
						switch (cell.getCellType()) {
						case Cell.CELL_TYPE_NUMERIC:
							System.out.println(cell.getNumericCellValue() + ": N ");
							break;
						case Cell.CELL_TYPE_STRING:
							System.out.println(cell.getStringCellValue() + ": S ");
							break;
						}
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (svr != null)
				svr.release();
			if (svw != null)
				svw.release();
			if (svfs != null)
				svfs.release();
		}

	}

	@Test
	public void testGetSetParams() throws Exception {
		SvReader svr = null;
		SvWriter svw = null;
		String sessionId = login("R.PEJOV", "naits1234");
		Reader rdr = new Reader();
		ValidationChecks vc = new ValidationChecks();
		Writer wr = new Writer();
		MassActions ma = new MassActions();
		Report rp = new Report();
		try {
			svr = new SvReader(sessionId);
			svw = new SvWriter(svr);
			svw.dbSetAutoCommit(false);

			DbDataObject dboPopulation = svr.getObjectById(11023642L, SvReader.getTypeIdByName(Tc.POPULATION), null);
			DbDataObject pom = rdr.getDbPopulationHoldingNumParam(dboPopulation, svr);
			if (pom != null) {
				log4j.debug(pom.toSimpleJson());
			}

			String pomString = rdr.getPopulationHoldingNumParamValue(dboPopulation, svr);
			if (pomString != null) {
				log4j.debug(pomString);
			}

		} catch (SvException e) {
			fail();
			e.printStackTrace();
			log4j.error(e.getMessage());
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
	}

	/**
	 * NEEDS TO BE INSTALLED ON NAITS.TIBROLABS AND ALL MEPA ENVIRONMENTS
	 * 
	 * 
	 * @throws Exception
	 */
	@Test
	public void testHoldingKeeperProblem() throws Exception {
		SvReader svr = null;
		SvWriter svw = null;
		String sessionId = login("K.STOJANOVSKI", "naits1234");
		Reader rdr = new Reader();
		ValidationChecks vc = new ValidationChecks();
		Writer wr = new Writer();
		MassActions ma = new MassActions();
		Report rp = new Report();
		try {
			svr = new SvReader(sessionId);
			svw = new SvWriter(svr);
			DbDataObject dboHolding = svr.getObjectById(13260168L, SvReader.getTypeIdByName("HOLDING"), null);
			DbDataObject dboOwner = svr.getObjectById(316332L, SvReader.getTypeIdByName("HOLDING_RESPONSIBLE"), null);
			// wr.linkObjects(dboHolding, dboOwner, Tc.HOLDING_KEEPER, "test",
			// svr);
			DbDataObject dboOwner1 = svr.getObjectById(316360L, SvReader.getTypeIdByName("HOLDING_RESPONSIBLE"), null);
			// wr.linkObjects(dboHolding, dboOwner1, Tc.HOLDING_KEEPER, "test",
			// svr);
			DbDataObject dboOwner2 = svr.getObjectById(316365L, SvReader.getTypeIdByName("HOLDING_RESPONSIBLE"), null);
			// wr.linkObjects(dboHolding, dboOwner2, Tc.HOLDING_KEEPER, "test",
			// svr);
			DbDataObject dboOwner3 = svr.getObjectById(316366L, SvReader.getTypeIdByName("HOLDING_RESPONSIBLE"), null);
			// wr.linkObjects(dboHolding, dboOwner3, Tc.HOLDING_KEEPER, "test",
			// svr);
			DbDataObject dboOwner4 = svr.getObjectById(316334L, SvReader.getTypeIdByName("HOLDING_RESPONSIBLE"), null);
			// wr.linkObjects(dboHolding, dboOwner4, Tc.HOLDING_KEEPER, "test",
			// svr);
			DbDataObject dboOwner5 = svr.getObjectById(316362L, SvReader.getTypeIdByName("HOLDING_RESPONSIBLE"), null);
			// wr.linkObjects(dboHolding, dboOwner5, Tc.HOLDING_KEEPER, "test",
			// svr);
			DbDataObject dboOwner6 = svr.getObjectById(316367L, SvReader.getTypeIdByName("HOLDING_RESPONSIBLE"), null);
			// wr.linkObjects(dboHolding, dboOwner6, Tc.HOLDING_KEEPER, "test",
			// svr);
			DbDataObject dboOwner7 = svr.getObjectById(316341L, SvReader.getTypeIdByName("HOLDING_RESPONSIBLE"), null);
			wr.linkObjects(dboHolding, dboOwner7, Tc.HOLDING_KEEPER, "test", svr);
			svr.dbCommit();
		} catch (SvException e) {
			e.printStackTrace();
			log4j.error(e.getMessage());
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
	}

	@Test
	public void testPublicApiAnimal() throws Exception {
		String responseResult = "";
		PublicServices aps = null;
		aps = new PublicServices();
		// Response response = aps.searchAnimalById("756756747");
		// klanica 10730600
		// so > 5 dvizenja 12552533
		// so > 3 vakcini 11795062
		// so > 10 vakcini 11958071
		Response response = aps.searchAnimalById("12197533", "en_US");
		// test - so 2 bolesi, 13300849L
		// test - nema bolesti 13300860L
		Object responseObject = new Object();
		responseObject = response.getEntity();
		responseResult = response.getEntity().toString();
		log4j.info(responseResult);
		responseResult = responseObject.toString();
		log4j.debug(responseResult);
	}

	@Test
	public void testCreatePet() throws Exception {
		SvReader svr = null;
		SvWriter svw = null;
		String sessionId = login("T.USR_PET", "naits1234");
		try {
			svw = new SvWriter(svr);
			DbDataObject dboPet = new DbDataObject();
			dboPet.setObject_type(svr.getTypeIdByName(Tc.PET));
			dboPet.setVal("PET_ID", "123");
			dboPet.setVal("PET_TYPE", "1");
			dboPet.setVal("PET_TAG_TYPE", "EAR_TAG");
			svw.saveObject(dboPet);
		} catch (SvException e) {
			e.printStackTrace();
			log4j.error(e.getMessage());
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
	}

	@Test
	public void testDeleteLink() throws Exception {
		initPlugin();
		SvReader svr = null;
		SvWriter svw = null;
		String sessionId = login("K.STOJANOVSKI", "naits1234");
		try {
			svr = new SvReader(sessionId);
			svw = new SvWriter(svr);
			DbDataObject dboLink = svr.getObjectById(12648938L, svCONST.OBJECT_TYPE_LINK, null);
			Writer wr = new Writer();
			wr.invalidateLink(dboLink, false, svr);
		} catch (SvException e) {
			e.printStackTrace();
			log4j.error(e.getMessage());
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
	}

	@Test
	public void testDirectInvenotryTransfer() throws Exception {
		SvReader svr = null;
		SvWriter svw = null;
		String sessionId = login("K.STOJANOVSKI", "naits1234");
		Reader rdr = new Reader();
		Writer wr = new Writer();
		ValidationChecks vc = new ValidationChecks();
		try {
			OnSaveValidations onSave = new OnSaveValidations();
			SvCore.registerOnSaveCallback(onSave, SvReader.getTypeIdByName(Tc.TRANSFER));
			svr = new SvReader(sessionId);
			svw = new SvWriter(svr);
			svw.dbSetAutoCommit(false);

			DbDataObject dboTransfer = new DbDataObject();
			dboTransfer.setObject_type(SvReader.getTypeIdByName(Tc.TRANSFER));
			String tagType = "1";
			Long startTagId = 1500L;
			Long endTagId = 1502L;
			String subjectTo = "შიდა ქართლი";
			Long destinationObjId = 6460342L;

			dboTransfer = wr.createTransferObject(tagType, startTagId, endTagId, null, null, null, subjectTo, null,
					null, null, null, String.valueOf(destinationObjId));
			dboTransfer.setVal(Tc.DIRECT_TRANSFER, true);
			// dboTransfer.setVal(Tc.CACHE_PARENT_ID,
			// dboParentObjectToHandle.getObject_id());
			dboTransfer.setVal(Tc.TRANSFER_TYPE, Tc.DIRECT_TRANSFER);
			svw.saveObject(dboTransfer, true);

			wr.directInventoryTransfer(dboTransfer, true, svr, svw);
			log4j.debug("OKD");
			svw.dbCommit();
		} catch (SvException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (svr != null)
				svr.release();
			if (svw != null)
				svw.release();
		}
	}

	@Test
	public void test() {
		SvReader svr = null;
		SvWriter svw = null;
		String sessionId = login("K.STOJANOVSKI", "naits1234");
		try {
			svr = new SvReader(sessionId);
			ValidationChecks vc = new ValidationChecks();
			ArrayList<String> test = vc.checkIfAllItemsInTransferRangeBelongToOrgUnit2(49L, 1258L, 1258L, "4", svr);
			log4j.info(test.size());

			ArrayList<String> missingInvItemsInRange = vc.checkIfAllItemsInTransferRangeBelongToOrgUnit(49L, 500L,
					1420L, "4", svr);
			if (missingInvItemsInRange.size() > 0) {
				String result = "naits.error.senderDoesNotHaveInventoryItemsDefinedInTheRange";
				result += missingInvItemsInRange.toString();
				log4j.info(result);
			}

		} catch (SvException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (svr != null)
				svr.release();
			if (svw != null)
				svw.release();
		}
	}

	@Test
	public void fixAnimalInvItemsBug() throws FileNotFoundException {
		SvReader svr = null;
		SvWriter svw = null;
		String sessionId = login("A.ADMIN3", "1234Naits1234");
		try {
			svr = new SvReader(sessionId);
			svw = new SvWriter(svr);
			Scanner sc = new Scanner(new File("C:\\Users\\zpetr\\Downloads\\data-1615887359687-2.csv"));
			sc.useDelimiter("\n"); // sets the delimiter pattern
			int count = 0;
			int total = 0;
			while (sc.hasNext()) // returns a boolean value
			{
				String s = sc.next().replace("\"", "").toString();
				s = s.substring(0, s.length() - 1);
				Long tempInvTagObjId = Long.valueOf(s);
				DbDataObject dboNow = svr.getObjectById(tempInvTagObjId, SvReader.getTypeIdByName(Tc.INVENTORY_ITEM),
						null);
				DbDataObject dboFIfthOfMarch = svr.getObjectById(tempInvTagObjId,
						SvReader.getTypeIdByName(Tc.INVENTORY_ITEM), new DateTime("2021-03-05"));
				if (!dboFIfthOfMarch.getParent_id().equals(dboNow.getParent_id())) {
					dboNow.setParent_id(dboFIfthOfMarch.getParent_id());
					svw.saveObject(dboNow, false);
					count++;
				}
				if (count == 100) {
					total += count;
					svw.dbCommit();
					log4j.info("100 items commited");
					log4j.info("Total number items commited:" + total);
					count = 0;
				}
			}
			total += count;
			svw.dbCommit();
			log4j.info(count + " items commited");
			log4j.info("Total number items commited:" + total);
			count = 0;
			log4j.info(count);
		} catch (SvException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (svr != null)
				svr.release();
			if (svw != null)
				svw.release();
		}

	}

	@Test
	public void test33() throws Exception {

		SvReader svr = null;
		SvWriter svw = null;
		String sessionId = login("K.STOJANOVSKI", "naits1234");
		try {
			svr = new SvReader(sessionId);
			ValidationChecks vc = new ValidationChecks();
			Writer wr = new Writer();
			DbDataObject dboInitTransfer = svr.getObjectById(13314113L, SvReader.getTypeIdByName(Tc.TRANSFER), null);

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
				// resultMsgLbl = "naits.success.createRevereseTransfer";
			}

		} catch (SvException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (svr != null)
				svr.release();
			if (svw != null)
				svw.release();
		}

	}

	@Test
	public void customCreateNewMessageWs() throws ParseException {
		SvReader svr = null;
		SvWriter svw = null;
		String token = login("M.ZHIVKOVIKJ", "naits1234");
		MsgServices mm = new MsgServices();
		try {
			svr = new SvReader(token);
			svw = new SvWriter(svr);

			Object responseObject = new Object();
			String responseResult = "";
			MultivaluedHashMap<String, String> form = new MultivaluedHashMap<String, String>();

			// 1st,no
			// form.add("{\"objectArray\": [{ \"subject.basic_info\":
			// {\"MODULE_NAME\":\"ANIMALS\", \"TITLE\":\"TEST\",
			// \"ID_SUBJECT\":5},"
			// + "\"subject.additional_info\": {\"PRIORITY\":\"LOW\",
			// \"CATEGORY\":\"INFO\"},"
			// + "\"message.basic_info\": {\"TEXT\":\"Hello\"},"
			// + "\"message.assignment_info\": {\"TO\":[123]},"
			// + "\"message.additional_info\": {\"PRIORITY\":\"LOW\"} }]}", "");

			// 2nd

			/*
			 * form.add("{\"objectArray\": " +
			 * "[{ \"subject.basic_info\": {\"MODULE_NAME\":\"ANIMALS\", \"TITLE\":\"Subject\"},"
			 * +
			 * "\"subject.additional_info\": {\"PRIORITY\":\"LOW\", \"CATEGORY\":\"REMARK\"},"
			 * + "\"message.basic_info\": {\"TEXT\":\"Test test test test\"}," +
			 * "\"message.assignment_info\": {\"TO\":[11023485, 13291183], \"CC\": [11023485]},"
			 * + "\"subject.objId\": {\"OBJ_ID\":\"0\"}," +
			 * "\"message.additional_info\": {\"ATCH_OBJ_TYPE\":\"HOLDING_RESPONSIBLE\"}," +
			 * "\"message.attachment_info\": {\"PRIORITY\":\"\"}," + " }]}", "");
			 */

			/*
			 * form.add(
			 * "{\"objectArray\":[{\"subject.basic_info\":{\"MODULE_NAME\":\"ANIMALS\",\"TITLE\":\"Test title\"},\"subject.additional_info\":{\"PRIORITY\":\"LOW\",\"CATEGORY\":\"REMARK\"},\"message.basic_info\":{\"TEXT\":\"This is reply\"},\"message.assignment_info\":{\"TO\":[11023485,13291183],\"CC\":[11023485]},\"subject.objId\":{\"OBJ_ID\":\"0\"},\"message.additional_info\":{\"PRIORITY\":\"LOW\"},\"message.attachment_info\":{\"ATCH_OBJ_TYPE\":28568,\"ATCH_OBJ_ID\":421500,\"NAME\":\"00599397569\"}}]}"
			 * , "");
			 * 
			 * form.add(
			 * "{\"SUBJECT_OBJ_ID\":\"0\",\"SUBJECT_MODULE_NAME\":\"ANIMALS\",\"SUBJECT_TITLE\":\"Test title\",\"SUBJECT_PRIORITY\":\"LOW\",\"SUBJECT_CATEGORY\":\"REMARK\",\"MSG_TEXT\":\"This is reply\",\"MSG_PRIORITY\":\"LOW\",\"MSG_TO\":[11023485,13291183],\"MSG_CC\":[11023485],\"message.attachment_info\":[{\"NAME\":\"00599397569\",\"ATCH_OBJ_ID\":421500,\"ATCH_OBJ_TYPE\":28568},{\"NAME\":\"00599397570\",\"ATCH_OBJ_ID\":421501,\"ATCH_OBJ_TYPE\":28568}]}"
			 * , "");
			 */

			form.add("SUBJECT_OBJ_ID", "0");
			form.add("SUBJECT_MODULE_NAME", "ANIMALS");
			form.add("SUBJECT_TITLE", "0");
			form.add("SUBJECT_PRIORITY", "LOW");
			form.add("SUBJECT_CATEGORY", "REMARK");
			form.add("MSG_TEXT", "TEST - THIS IS A REPLY");
			form.add("MSG_PRIORITY", "LOW");
			form.add("MSG_TO", "[11023485, 13291183]");
			form.add("MSG_CC", "[11023485]");
			form.add("MSG_BCC", "[]");
			form.add("MSG_ATTACHMENT",
					"[{\"NAME\":\"00599397569\",\"ATCH_OBJ_ID\":421500,\"ATCH_OBJ_TYPE\":28568},{\"NAME\":\"00599397570\",\"ATCH_OBJ_ID\":421501,\"ATCH_OBJ_TYPE\":28568}]");

			Response response = mm.createNewMessage(login("M.ZHIVKOVIKJ", "naits1234"), form);
			responseObject = response.getEntity();
			responseResult = response.getEntity().toString();
			log4j.debug(responseResult);
			responseResult = responseObject.toString();
			log4j.debug(responseResult);
			//
		} catch (

		SvException e) {
			log4j.error(e);
		} finally {
			if (svr != null) {
				svr.release();
			}
			if (svw != null) {
				svw.release();
			}
		}
	}

	@Test
	public void testGetInbox() {
		MsgServices msg = new MsgServices();
		Response response = msg.getInboxMessages(login("T.ADMIN0", "naits1234"));
	}

	@Test
	public void testGetTerminatedAnimals() throws Exception {
		SvReader svr = null;
		String token = login("M.ZHIVKOVIKJ", "naits1234");
		Reader rdr = new Reader();
		try {
			svr = new SvReader(token);
			DbDataArray terminatedAnimals = rdr.getTerminatedAnimals(6767434L, "2015-12-12", "2022-12-12", 5000, svr);
			// DbDataArray terminatedAnimals =
			// rdr.getTerminatedAnimals(6767434L, "null",
			// "null", svr);
			String result = rdr.convertDbDataArrayToGridJsonWithoutSorting(terminatedAnimals, Tc.ANIMAL, false, svr);
			log4j.info(terminatedAnimals.size());
			// System.out.println(result);
		} catch (SvException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (svr != null)
				svr.release();
		}
	}

	@Test
	public void testFilteredHoldings() throws Exception {
		SvReader svr = null;
		String token = login("M.ZHIVKOVIKJ", "naits1234");
		Reader rdr = new Reader();
		try {
			svr = new SvReader(token);
			// DbDataArray terminatedAnimals =
			// rdr.getTerminatedAnimals(6767434L,
			// "2015-12-12", "2022-12-12", 10000, svr);
			DbDataArray filteredHoldings = rdr.getFiltratedHoldings("7", null, null, null, "32", null, 1000, svr);
			String result = rdr.convertDbDataArrayToGridJson(filteredHoldings, Tc.HOLDING, false, Tc.PKID, Tc.DESC,
					svr);
			log4j.info(filteredHoldings.size());
			// System.out.println(result);
		} catch (SvException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (svr != null)
				svr.release();
		}
	}

	@Test
	public void testFinishedMovementDocs() throws Exception {
		SvReader svr = null;
		String token = login("M.ZHIVKOVIKJ", "naits1234");
		Reader rdr = new Reader();
		String result;
		try {
			svr = new SvReader(token);
			int rowLimit = 1000;
			// DbDataArray terminatedAnimals =
			// rdr.getTerminatedAnimals(6767434L,
			// "2015-12-12", "2022-12-12", 10000, svr);
			DbDataArray finishedMovementDocs = rdr.getFinishedMovementDocuments("2930633100018", "2019-01-01",
					"2023-01-01", rowLimit, svr);
			finishedMovementDocs = rdr.revertDbDataArray(finishedMovementDocs);
			log4j.info(finishedMovementDocs.size());
			log4j.info(finishedMovementDocs.get(0).getPkid());
			log4j.info(finishedMovementDocs.get(finishedMovementDocs.size() - 1).getPkid());
			finishedMovementDocs = rdr.alignDbDataArrayToSpecificSize(finishedMovementDocs, 1000);
			log4j.info(finishedMovementDocs.size());
			log4j.info(finishedMovementDocs.get(0).getPkid());
			log4j.info(finishedMovementDocs.get(rowLimit - 1).getPkid());
			result = rdr.convertDbDataArrayToGridJson(finishedMovementDocs, Tc.MOVEMENT_DOC, false, Tc.PKID, Tc.DESC,
					svr);
			// System.out.println(result);
		} catch (SvException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (svr != null)
				svr.release();
		}
	}

	@Test
	public void testFinishedMovementDocsWS() throws Exception {
		// configure the core to clean up every 5 seconds
		SvReader svr = null;
		String sessionId = login("M.ZHIVKOVIKJ", "naits1234");
		Reader rdr = new Reader();
		ApplicationServices rea = new ApplicationServices();
		try {
			svr = new SvReader(sessionId);
			Object temp = null;
			String result = "";
			Response responseHtml = rea.getFinishedMovementDocuments(sessionId, "2930633100018", "null", "null", 1000);
			temp = responseHtml.getEntity();
			result = temp.toString();
			log4j.debug(result);
		} catch (SvException e) {
			fail("Test failed");
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (svr != null)
				svr.release();
		}
	}

	@Test
	public void testTerminatedAnimalsWS() throws Exception {
		// configure the core to clean up every 5 seconds
		SvReader svr = null;
		String sessionId = login("M.ZHIVKOVIKJ", "naits1234");
		Reader rdr = new Reader();
		ApplicationServices rea = new ApplicationServices();
		try {
			svr = new SvReader(sessionId);
			Object temp = null;
			String result = "";
			Response responseHtml = rea.getTerminatedAnimals(sessionId, 5015557L, null, null, 5000);
			temp = responseHtml.getEntity();
			result = temp.toString();
			log4j.debug(result);
		} catch (SvException e) {
			fail("Test failed");
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (svr != null)
				svr.release();
		}
	}

	@Test
	public void testFinishedAnimalMovementsWS() throws Exception {
		// configure the core to clean up every 5 seconds
		SvReader svr = null;
		String sessionId = login("M.ZHIVKOVIKJ", "naits1234");
		Reader rdr = new Reader();
		ApplicationServices rea = new ApplicationServices();
		try {
			svr = new SvReader(sessionId);
			Object temp = null;
			String result = "";
			Response responseHtml = rea.getFinishedAnimalMovements(sessionId, 6767434L, "2022-12-10", "2022-12-13",
					5000);
			temp = responseHtml.getEntity();
			result = temp.toString();
			log4j.debug(result);
		} catch (SvException e) {
			fail("Test failed");
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (svr != null)
				svr.release();
		}
	}

	@Test
	public void testPersonsWS() throws Exception {
		// configure the core to clean up every 5 seconds
		SvReader svr = null;
		String sessionId = login("M.ZHIVKOVIKJ", "naits1234");
		Reader rdr = new Reader();
		ApplicationServices rea = new ApplicationServices();
		try {
			svr = new SvReader(sessionId);
			Object temp = null;
			String result = "";
			Response responseHtml = rea.getPersonsByCriteria(sessionId, "240", "null", "null", "null", "null", "null",
					5000);
			temp = responseHtml.getEntity();
			result = temp.toString();
			log4j.debug(result);
		} catch (SvException e) {
			fail("Test failed");
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (svr != null)
				svr.release();
		}
	}

	@Test
	public void testAnimalsWS() throws Exception {
		// configure the core to clean up every 5 seconds
		SvReader svr = null;
		String sessionId = login("M.ZHIVKOVIKJ", "naits1234");
		Reader rdr = new Reader();
		ApplicationServices rea = new ApplicationServices();
		try {
			svr = new SvReader(sessionId);
			Object temp = null;
			String result = "";
			Response responseHtml = rea.getAnimalsByCriteria(sessionId, "1234", "VALID", "1", "null", "null", "GE",
					5000);
			temp = responseHtml.getEntity();
			result = temp.toString();
			log4j.debug(result);
		} catch (SvException e) {
			fail("Test failed");
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (svr != null)
				svr.release();
		}
	}

	@Test
	public void testOutgoingTransferWS() throws Exception {
		// configure the core to clean up every 5 seconds
		SvReader svr = null;
		String sessionId = login("A.ADMIN3", "1234Naits1234");
		Reader rdr = new Reader();
		ApplicationServices rea = new ApplicationServices();
		try {
			svr = new SvReader(sessionId);
			Object temp = null;
			String result = "";
			/*
			 * Response responseHtml = rea.getOutgoingTransfersPerOrgUnit(sessionId, 49L,
			 * "null", "null", "null", "null", "null", 5000);
			 */
			Response responseHtml = rea.getOutgoingTransfersPerOrgUnit(sessionId, 49L, "1", "72273000", "72273999",
					"null", "null", 5000);
			temp = responseHtml.getEntity();
			result = temp.toString();
			log4j.debug(result);
		} catch (SvException e) {
			fail("Test failed");
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (svr != null)
				svr.release();
		}
	}

	@Test
	public void testIncomingTransferWS() throws Exception {
		// configure the core to clean up every 5 seconds
		SvReader svr = null;
		String sessionId = login("M.ZHIVKOVIKJ", "naits1234");
		Reader rdr = new Reader();
		ApplicationServices rea = new ApplicationServices();
		try {
			svr = new SvReader(sessionId);
			Object temp = null;
			String result = "";
			Response responseHtml = rea.getIncomingTransfersPerOrgUnit(sessionId, "49", "null", "null", "null", "null",
					"null", 5000);
			temp = responseHtml.getEntity();
			result = temp.toString();
			log4j.debug(result);
		} catch (SvException e) {
			fail("Test failed");
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (svr != null)
				svr.release();
		}
	}

	@Test
	public void testUNDO_LAST_TRANSFER() throws Exception {
		SvReader svr = null;
		String sessionId = login("M.ZHIVKOVIKJ", "naits1234");
		Reader rdr = new Reader();
		Writer wr = new Writer();
		try {
			svr = new SvReader(sessionId);
			DbDataObject dboObjectToHandle = svr.getObjectById(6109109L, SvReader.getTypeIdByName(Tc.ANIMAL), null);
			DbDataObject dboObjToHandleParent = svr.getObjectById(dboObjectToHandle.getParent_id(),
					SvReader.getTypeIdByName(Tc.HOLDING), null);
			DbSearchExpression dbse = new DbSearchExpression();
			DbSearchCriterion dbc1 = new DbSearchCriterion(Tc.STATUS, DbCompareOperand.EQUAL, Tc.FINISHED);
			DbSearchCriterion dbc2 = new DbSearchCriterion(Tc.ANIMAL_EAR_TAG, DbCompareOperand.EQUAL,
					dboObjectToHandle.getVal(Tc.ANIMAL_ID));
			DbSearchCriterion dbc3 = new DbSearchCriterion(Tc.DESTINATION_HOLDING_ID, DbCompareOperand.EQUAL,
					dboObjToHandleParent.getVal(Tc.PIC));
			dbse.addDbSearchItem(dbc1).addDbSearchItem(dbc2).addDbSearchItem(dbc3);
			DbQueryObject query = new DbQueryObject(SvReader.getDbtByName(Tc.ANIMAL_MOVEMENT), dbse, null, null);
			ArrayList<String> orderBy = new ArrayList<String>();
			orderBy.add(Tc.PKID + " " + Tc.DESC);
			query.setOrderByFields(orderBy);
			DbDataArray dboAnimalOrFlockMovementsArr = svr.getObjects(query, 0, 0);
			DbDataObject lastAnimalOrFlockMovementObj = new DbDataObject();
			if (dboAnimalOrFlockMovementsArr.size() > 0) {
				lastAnimalOrFlockMovementObj = dboAnimalOrFlockMovementsArr.getItems().get(0);
			}
			String lastMovementOriginPic = lastAnimalOrFlockMovementObj.getVal(Tc.SOURCE_HOLDING_ID).toString();
			DbDataObject lastMovementOriginHoldingObj = rdr.findAppropriateHoldingByPic(lastMovementOriginPic, svr);
			JsonObject jObj = new JsonObject();
			JsonArray subJsonParams = new JsonArray();
			JsonObject jObj1 = new JsonObject();
			jObj1.addProperty(Tc.MASS_PARAM_ANIMAL_FLOCK_ID, dboObjectToHandle.getVal(Tc.ANIMAL_ID).toString());
			subJsonParams.add(jObj1);
			JsonObject jObj2 = new JsonObject();
			jObj2.addProperty(Tc.MASS_PARAM_HOLDING_OBJ_ID, lastMovementOriginHoldingObj.getObject_id());
			subJsonParams.add(jObj2);
			JsonObject jObj3 = new JsonObject();
			jObj3.addProperty(Tc.MASS_PARAM_ANIMAL_CLASS, dboObjectToHandle.getVal(Tc.ANIMAL_CLASS).toString());
			subJsonParams.add(jObj3);
			jObj.add(Tc.OBJ_PARAMS, subJsonParams);
			wr.moveAnimalOrFlockViaDirectTransfer(jObj, sessionId);
		} catch (SvException e) {
			fail("Test failed");
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (svr != null)
				svr.release();
		}
	}

	@Test
	public void testPubliWsGetLabSamples() throws Exception {
		String sessionId = login("M.ZHIVKOVIKJ", "naits1234");
		SvReader svr = null;
		try {
			// configure the core to clean up every 5 seconds
			PublicServices pblserv = new PublicServices();
			Object temp = null;
			Response responseHtml = pblserv.getLabSampleDetails(sessionId, "12080761", "1");
			temp = responseHtml.getEntity();
			String result = temp.toString();
			log4j.debug(result);
		} finally {
			if (svr != null)
				svr.release();
		}
	}

	@Test
	public void testPubliWsGetLabSampleResults() throws Exception {
		String sessionId = login("M.ZHIVKOVIKJ", "naits1234");
		SvReader svr = null;
		try {
			// configure the core to clean up every 5 seconds
			PublicServices pblserv = new PublicServices();
			Object temp = null;
			Response responseHtml = pblserv.getLabSampleResults(sessionId, "11958071-63");
			temp = responseHtml.getEntity();
			String result = temp.toString();
			log4j.debug(result);
		} finally {
			if (svr != null)
				svr.release();
		}
	}

	@Test
	public void testPubliWsCreateLabSampleResults() throws Exception {
		String sessionId = login("M.ZHIVKOVIKJ", "naits1234");
		SvReader svr = null;
		try {
			// configure the core to clean up every 5 seconds
			PublicServices pblserv = new PublicServices();
			Object temp = null;
			Response responseHtml = pblserv.getLabSampleResults(sessionId, "11958071-63");
			responseHtml = pblserv.createLabSampleResult(sessionId, "11958071-63", "1", "Test: Brucellosis-Blood",
					"2021-06-30", "1", "2");
			temp = responseHtml.getEntity();
			String result = temp.toString();
			log4j.debug(result);
		} finally {
			if (svr != null)
				svr.release();
		}
	}

	@Test
	public void testPubliWsCreateLabSamples() throws Exception {
		String sessionId = login("M.ZHIVKOVIKJ", "naits1234");
		SvReader svr = null;
		try {
			// configure the core to clean up every 5 seconds
			PublicServices pblserv = new PublicServices();
			Object temp = null;
			Response responseHtml = pblserv.getLabSampleDetails(sessionId, "12080761", "1");
			responseHtml = pblserv.createLabSample(sessionId, "1", "2020-05-14", "12080761", "1",
					svr.getUserBySession(sessionId).getVal(Tc.USER_NAME).toString(), "hodlingPic", "ბუხუტი კაპანაძე",
					null, "3", "0", "1", null);
			temp = responseHtml.getEntity();
			String result = temp.toString();
			log4j.debug(result);
		} finally {
			if (svr != null)
				svr.release();
		}
	}

	@Test
	public void testWS_moveGroupOfIndividualInventoryItems() throws Exception {
		SvReader svr = null;
		// String sessionId = login("M.ZHIVKOVIKJ", "naits1234");
		String sessionId = login("A.ADMIN3", "1234Naits1234");
		Reader rdr = new Reader();
		MassActions massAction = new MassActions();
		ApplicationServices appSrvc = new ApplicationServices();
		try {
			svr = new SvReader(sessionId);
			Object temp = null;
			String result = "";
			JsonArray jArr = new JsonArray();

			String json = "{\r\n" + "	\"INVENTORY_ITEM.PKID\": 51432075,\r\n"
					+ "	\"INVENTORY_ITEM.OBJECT_ID\": 22708433,\r\n" + "	\"INVENTORY_ITEM.PARENT_ID\": 6460336,\r\n"
					+ "	\"INVENTORY_ITEM.OBJECT_TYPE\": 1354034,\r\n" + "	\"INVENTORY_ITEM.STATUS\": \"VALID\",\r\n"
					+ "	\"INVENTORY_ITEM.TAG_TYPE\": \"1\",\r\n"
					+ "	\"INVENTORY_ITEM.EAR_TAG_NUMBER\": \"14143501\",\r\n"
					+ "	\"INVENTORY_ITEM.TAG_STATUS\": \"NON_APPLIED\",\r\n"
					+ "	\"INVENTORY_ITEM.ORDER_NUMBER\": \"011 2022 Cattle\"\r\n" + "}";

			JsonObject jsonObject = new Gson().fromJson(json, JsonObject.class);

			jArr.add(jsonObject);

			String resultMsgLbl = massAction.inventoryIndividualDirectTransfer(6460380L, jArr, sessionId);
			result = resultMsgLbl;
			log4j.debug(result);
		} catch (SvException e) {
			fail("Test failed");
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (svr != null)
				svr.release();
		}
	}

	@Test
	public void testSlaughterAnimalAsSlaughterhouseOperator() throws Exception {
		SvReader svr = null;
		String token = null;
		Reader rdr = new Reader();
		Writer wr = new Writer();
		MassActions massAct = new MassActions();
		// 540752
		try {
			String result = "";
			token = login("S.TEST2", "Naits123");
			svr = new SvReader(token);
			String resultMsgLbl = "naits.error.massAnimalsAction";
			ValidationChecks vc = new ValidationChecks();
			DbDataObject dboAnimal = rdr.findAppropriateAnimalByAnimalId("13386983", svr);
			/*
			 * boolean isSlaughterhouseOperator = false; DbDataArray arrAdditionalGroup =
			 * svr.getAllUserGroups(svr.getUserBySession(token), false); if
			 * (!arrAdditionalGroup.getItems().isEmpty()) { for (DbDataObject dboUserGroup :
			 * arrAdditionalGroup.getItems()) { if (dboUserGroup.getVal(Tc.GROUP_NAME) !=
			 * null && dboUserGroup.getVal(Tc.GROUP_NAME).toString().equals(Tc.
			 * SLAUGHTERHOUSE_OPERATOR)) { isSlaughterhouseOperator = true; break; } } }
			 * vc.slaughterhouseHoldingValidationSet(dboAnimal.getParent_id(), dboAnimal,
			 * "animal", isSlaughterhouseOperator, svr);
			 */

			JsonObject jsonData = null;
			JSONParser parser = new JSONParser();
			Object obj = parser.parse(new FileReader(
					"C:\\Users\\zpetr\\git\\svarog2\\naits_triglav_plugin\\conf\\json_cases\\MassAnimalFlockHandler_animalCaseJson"));
			JSONObject jsonObject = (JSONObject) obj;

			JsonParser parser2 = new JsonParser();
			JsonElement jsonElement = parser2.parse(new FileReader(
					"C:\\Users\\zpetr\\git\\svarog2\\naits_triglav_plugin\\conf\\json_cases\\MassAnimalFlockHandler_animalCaseJson"));
			jsonData = jsonElement.getAsJsonObject();

			resultMsgLbl = massAct.animalFlockMassHandler(jsonData, token);

		} catch (Exception e) {
			log4j.error(e);
			fail();
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
	}

	@Test
	public void testPetServicesNabageQuarantine() throws Exception {
		SvReader svr = null;
		String token = null;
		Reader rdr = new Reader();
		Writer wr = new Writer();
		PetServices petServices = new PetServices();
		// 540752
		try {
			String result = "";
			token = login("M.ZHIVKOVIKJ", "naits1234");
			svr = new SvReader(token);
			String resultMsgLbl = "naits.error.massAnimalsAction";
			ValidationChecks vc = new ValidationChecks();
			DbDataObject dboAnimal = rdr.findAppropriateAnimalByAnimalId("13386983", svr);

			JsonObject jsonData = null;
			JSONParser parser = new JSONParser();
			Object obj = parser.parse(new FileReader(
					"C:\\Users\\zpetr\\git\\svarog2\\naits_triglav_plugin\\conf\\json_cases\\PetServices"));
			JSONObject jsonObject = (JSONObject) obj;

			JsonParser parser2 = new JsonParser();
			JsonElement jsonElement = parser2.parse(new FileReader(
					"C:\\Users\\zpetr\\git\\svarog2\\naits_triglav_plugin\\conf\\json_cases\\PetServices"));
			jsonData = jsonElement.getAsJsonObject();

			resultMsgLbl = petServices.petMassHandler(jsonData, token);

		} catch (Exception e) {
			log4j.error(e);
			fail();
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
	}

	@Test
	public void testInvItemGeneration() throws Exception {
		SvReader svr = null;
		String token = null;
		Reader rdr = new Reader();
		Writer wr = new Writer();
		PetServices petServices = new PetServices();
		String resultMsgLbl = "naits.error.generalError";
		// 540752
		try {
			String result = "";
			token = login("A.ADMIN3", "1234Naits1234");
			svr = new SvReader(token);
			svr.setAutoCommit(false);
			SvWriter svw = new SvWriter(svr);
			JsonObject jsonData = null;
			JsonParser parser2 = new JsonParser();
			JsonElement jsonElement = parser2.parse(new FileReader(
					"C:\\Users\\zpetr\\git\\svarog2\\naits_triglav_plugin\\conf\\json_cases\\InventoryItemGeneration2"));
			jsonData = jsonElement.getAsJsonObject();

			if (jsonData != null && jsonData.has(Tc.OBJ_ARRAY)) {
				resultMsgLbl = wr.generateInventoryItem(jsonData.get(Tc.OBJ_ARRAY).getAsJsonArray(), svr, svw);
				if (resultMsgLbl.trim().equals("")) {
					resultMsgLbl = "naits.success.saveInventoryItem";
				}
			} else {
				resultMsgLbl = Tc.error_admConsoleBadJson;
			}


		} catch (Exception e) {
			log4j.error(e);
			fail();
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
	}

	@Test
	public void testNaitsDbConnection() throws Exception {
		SvReader svr = null;
		String token = null;
		try {
			token = login("A.ADMIN3", "WelcomeNaits123");
			svr = new SvReader(token);
		} catch (Exception e) {
			log4j.error(e);
			fail();
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
	}
	
}