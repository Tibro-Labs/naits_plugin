/*******************************************************************************
 * Copyright (c),  2017 TIBRO DOOEL Skopje
 *******************************************************************************/

package naits_triglav_plugin;

import java.time.temporal.ChronoUnit;
import java.time.LocalDate;

import static org.junit.Assert.assertNotNull;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import org.apache.commons.codec.binary.Base64;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.Period;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.prtech.svarog.I18n;
import com.prtech.svarog.SvConf;
import com.prtech.svarog.SvCore;
import com.prtech.svarog.SvException;
import com.prtech.svarog.SvFileStore;
import com.prtech.svarog.SvLink;
import com.prtech.svarog.SvNote;
import com.prtech.svarog.SvParameter;
import com.prtech.svarog.SvReader;
import com.prtech.svarog.SvSequence;
import com.prtech.svarog.svCONST;
import com.prtech.svarog.CodeList;
import com.prtech.svarog_common.DbDataArray;
import com.prtech.svarog_common.DbDataObject;
import com.prtech.svarog_common.DbQueryExpression;
import com.prtech.svarog_common.DbQueryObject;
import com.prtech.svarog_common.DbSearch.DbLogicOperand;
import com.prtech.svarog_common.DbSearchCriterion;
import com.prtech.svarog_common.DbSearchCriterion.DbCompareOperand;

import com.prtech.svarog_common.DbSearchExpression;
import com.prtech.svarog_common.SvCharId;
import com.prtech.svarog_common.DbQueryObject.DbJoinType;
import com.prtech.svarog_common.DbQueryObject.LinkType;

import javax.ws.rs.core.MultivaluedMap;

/**
 * Helper class for access/search of data
 * 
 * @author TIBRO_001
 *
 */

public class Reader {

	static final int COMMIT_COUNT = 1000;
	static final Logger log4j = LogManager.getLogger(Reader.class.getName());

	// ANIMAL METHODS

	/**
	 * This method returns list of diseases of the current animal
	 * 
	 * @param animalObj DbDataObject of the animal
	 * 
	 * @param svr       SvReader instance
	 * 
	 * @return ArrayList<String>
	 */
	public ArrayList<String> getInformationAboutAnimalHealthStatus(DbDataObject animalObj, SvReader svr)
			throws SvException {
		DbDataArray vaccEvents = getAllVaccEventsForVaccBook(animalObj, svr);
		ArrayList<String> listOfDiseases = new ArrayList<>();
		String animalHealthStatus = null;
		String disease = null;
		String result = "This animal is not suspicious.";

		if (animalObj.getVal(Tc.ANIMAL_HEALTH_STATUS) != null) {
			animalHealthStatus = animalObj.getVal(Tc.ANIMAL_HEALTH_STATUS).toString();
			if (animalHealthStatus.equals("1") || animalHealthStatus.equals("2")) {
				result = "This animal is suspicious because has not healthy status: " + animalHealthStatus;
				listOfDiseases.add(result);
			}
		} else {
			if (vaccEvents != null && animalObj.getVal(Tc.ANIMAL_HEALTH_STATUS) != null
					&& animalObj.getVal(Tc.ANIMAL_HEALTH_STATUS).toString().equals("0")) {
				for (DbDataObject vaccEvent : vaccEvents.getItems()) {
					if (vaccEvent.getVal(Tc.DISEASE) != null && vaccEvent.getVal(Tc.DISEASE).toString().length() > 0) {
						disease = vaccEvent.getVal(Tc.DISEASE).toString();
						result = "This animal is suspicious because of the following disease: " + disease;
						if (!listOfDiseases.contains(result))
							listOfDiseases.add(result);
					}
				}
			}
		}
		if (listOfDiseases.isEmpty()) {
			listOfDiseases.add(result);
		}
		return listOfDiseases;
	}

	public void generateAnimalSummaryInfo(Long objectId, LinkedHashMap<String, String> jsonOrderedMap, SvNote svn,
			SvReader svr) throws SvException {
		DbDataObject animalObj = svr.getObjectById(objectId, SvReader.getTypeIdByName(Tc.ANIMAL), null);
		DbDataObject holdingObj = getHoldingObjectByObjectId(animalObj.getParent_id(), svr);
		jsonOrderedMap.put("naits.animal.animalAge", calcAnimalAge(objectId, svr));
		jsonOrderedMap.put("naits.keeper.full_name", getHoldingKeeperInfo(animalObj.getParent_id(), svr));
		jsonOrderedMap.put("naits.holding.pic",
				((holdingObj.getVal(Tc.PIC) != null) ? holdingObj.getVal(Tc.PIC).toString() : "N/A"));
		if (animalObj.getStatus().equals(Tc.DESTROYED)) {
			String destructionReason = svn.getNote(objectId, Tc.DESTRUCTION_NOTE);
			jsonOrderedMap.put("naits.animal.destroy_reason",
					destructionReason.trim() == "" ? "N/A" : destructionReason);
		}
	}

	/**
	 * Method that returns DbDataObject of type ANIMAL for the appropriate animal
	 * ear tag
	 * 
	 * @param animalUid animal ear tag number
	 * 
	 * @param svr       SvReader instance
	 * 
	 * @throws SvException
	 * @return DbDataObject
	 */
	public DbDataObject findAppropriateAnimalByAnimalId(String animalUid, SvReader svr) throws SvException {

		DbDataObject result = null;
		DbSearchCriterion cr1 = new DbSearchCriterion(Tc.ANIMAL_ID, DbCompareOperand.EQUAL, animalUid);
		DbSearchExpression dbs = new DbSearchExpression().addDbSearchItem(cr1);
		DbDataArray dbArray = svr.getObjects(dbs, SvReader.getTypeIdByName(Tc.ANIMAL), null, 0, 0);
		if (dbArray != null && !dbArray.getItems().isEmpty()) {
			result = dbArray.getItems().get(0);
		}
		return result;
	}

	public DbDataArray findAnimalByAnimalId(String animalId, SvReader svr) throws SvException {
		DbDataArray result = null;
		DbSearchCriterion cr = new DbSearchCriterion(Tc.ANIMAL_ID, DbCompareOperand.EQUAL, animalId);
		DbSearchExpression dbs = new DbSearchExpression().addDbSearchItem(cr);
		result = svr.getObjects(dbs, SvReader.getTypeIdByName(Tc.ANIMAL), null, 1000, 0);
		return result;
	}

	public DbDataObject findAppropriateAnimalByAnimalIdAndAnimalClass(String animalId, String animalClass,
			boolean useCache, SvReader svr) throws SvException {
		DbDataObject result = null;
		DbDataArray dbArray = null;
		DbSearchCriterion cr1 = new DbSearchCriterion(Tc.ANIMAL_ID, DbCompareOperand.EQUAL, animalId);
		DbSearchCriterion cr2 = new DbSearchCriterion(Tc.ANIMAL_CLASS, DbCompareOperand.EQUAL, animalClass);
		DbSearchExpression dbse = new DbSearchExpression().addDbSearchItem(cr1).addDbSearchItem(cr2);
		if (useCache) {
			dbArray = svr.getObjects(dbse, SvReader.getTypeIdByName(Tc.ANIMAL), null, 0, 0);
		} else {
			dbArray = svr.getObjects(dbse, SvReader.getTypeIdByName(Tc.ANIMAL), new DateTime(), 0, 0);
		}
		if (dbArray != null && !dbArray.getItems().isEmpty()) {
			result = dbArray.getItems().get(0);
		}
		return result;
	}

	public DbDataArray findLabSamplesPerAnimalIdAndAnimalClass(String animalId, String animalClass, boolean useCache,
			SvReader svr) throws SvException {
		DbDataArray labSamples = null;
		DbDataObject dboAnimal = findAppropriateAnimalByAnimalIdAndAnimalClass(animalId, animalClass, useCache, svr);
		if (dboAnimal != null) {
			labSamples = svr.getObjectsByParentId(dboAnimal.getObject_id(), SvReader.getTypeIdByName(Tc.LAB_SAMPLE),
					null, null, null);
		}
		return labSamples;
	}

	public DbDataArray findLabSampleResultsPerSampleId(String sampleId, SvReader svr) throws SvException {
		DbDataArray labSampleResults = null;
		DbDataArray dboLabSamples = findDataPerSingleFilter(Tc.SAMPLE_ID, sampleId, DbCompareOperand.EQUAL,
				SvReader.getTypeIdByName(Tc.LAB_SAMPLE), svr);
		if (dboLabSamples != null) {
			labSampleResults = svr.getObjectsByParentId(dboLabSamples.get(0).getObject_id(),
					SvReader.getTypeIdByName(Tc.LAB_TEST_RESULT), null, 0, 0);
		}
		return labSampleResults;
	}

	/**
	 * Method that returns DbDataObject of type EAR_TAG_REPLC animal ear tag
	 * 
	 * @param old_ear_tag old_ear_tag number
	 * 
	 * @param svr         SvReader instance
	 * 
	 * @throws SvException
	 * @return DbDataObject
	 */
	public DbDataObject findAnimalByOldEarTag(String oldEarTag, SvReader svr) throws SvException {
		DbDataObject dboAnimalFound = null;
		DbDataObject dboNextAnimalTagReplc = findAnimalTagRepalcementViaOldEarTag(oldEarTag, null, svr);
		if (dboNextAnimalTagReplc != null && dboNextAnimalTagReplc.getVal(Tc.NEW_EAR_TAG) != null) {
			String newEarTag = Tc.EMPTY_STRING;
			while (dboAnimalFound == null) {
				dboNextAnimalTagReplc = findAnimalTagRepalcementViaOldEarTag(oldEarTag, null, svr);
				if (dboNextAnimalTagReplc == null)
					break;
				newEarTag = dboNextAnimalTagReplc.getVal(Tc.NEW_EAR_TAG).toString();
				dboAnimalFound = findAppropriateAnimalByAnimalId(newEarTag, svr);
				if (dboAnimalFound == null) {
					oldEarTag = newEarTag;
				}
			}
		}
		return dboAnimalFound;
	}

	/**
	 * Method that returns DbDataArray of ORG_UNITS linked per user
	 * 
	 * @param svr SvReader instance
	 * 
	 * @throws SvException
	 * @return DbDataObject
	 */
	public DbDataArray getAllOrgUnitsPerUser(SvReader svr) throws SvException {
		DbDataArray dboOrgUnitsPerUser = null;
		DbDataObject dboUser = SvReader.getUserBySession(svr.getSessionId());
		if (dboUser != null) {
			DbDataObject linkPoaUserOrgUnit = SvReader.getLinkType(Tc.POA, svCONST.OBJECT_TYPE_USER,
					svCONST.OBJECT_TYPE_ORG_UNITS);
			dboOrgUnitsPerUser = svr.getObjectsByLinkedId(dboUser.getObject_id(), linkPoaUserOrgUnit, null, 0, 0);
		}
		return dboOrgUnitsPerUser;
	}

	public DbCompareOperand autoSetTheDbCompareOperandParam(String columnName, String columnValue, SvReader svr)
			throws SvException {
		DbCompareOperand compareOperand = DbCompareOperand.ILIKE;
		DbDataArray fieldsResult = findDataPerSingleFilter(Tc.FIELD_NAME, columnName, DbCompareOperand.EQUAL,
				svCONST.OBJECT_TYPE_FIELD, svr);
		if (fieldsResult != null) {
			DbDataObject dboField = fieldsResult.get(0);
			if (dboField != null && dboField.getVal(Tc.CODE_LIST_ID) != null) {
				compareOperand = DbCompareOperand.EQUAL;
			} else if (columnValue.matches("[0-9]+")) {
				compareOperand = DbCompareOperand.LIKE;
			}
		}
		return compareOperand;
	}

	/**
	 * Method that returns DbDataObject of type EAR_TAG_REPLC animal ear tag
	 * 
	 * @param old_ear_tag old_ear_tag number
	 * 
	 * @param svr         SvReader instance
	 * 
	 * @throws SvException
	 * @return DbDataObject
	 */
	public DbDataObject findAnimalTagRepalcementViaOldEarTag(String oldEarTag, Long parentId, SvReader svr)
			throws SvException {
		DbDataObject result = null;
		DbSearchCriterion cr1 = new DbSearchCriterion(Tc.OLD_EAR_TAG, DbCompareOperand.EQUAL, oldEarTag);
		DbSearchExpression dbse = new DbSearchExpression().addDbSearchItem(cr1);
		if (parentId != null) {
			DbSearchCriterion cr2 = new DbSearchCriterion(Tc.PARENT_ID, DbCompareOperand.EQUAL, parentId);
			dbse.addDbSearchItem(cr2);
		}
		DbDataArray dbArray = svr.getObjects(dbse, SvReader.getTypeIdByName(Tc.EAR_TAG_REPLC), null, 0, 0);
		if (dbArray != null && !dbArray.getItems().isEmpty()) {
			result = dbArray.getItems().get(0);
		}
		return result;
	}

	/**
	 * Method that returns DbDataObject of type ANIMAL for the appropriate animal
	 * ear tag and country id
	 * 
	 * @param countryId countryId code list value
	 * 
	 * @param animalUid animal ear tag number
	 * 
	 * @param svr       SvReader instance
	 * 
	 * @throws SvException
	 * @return DbDataObject
	 */
	public DbDataObject findAppropriateForeignAnimalByBarCode(String countryId, String animalUid, SvReader svr)
			throws SvException {

		DbDataObject result = null;
		DbSearchCriterion cr1 = new DbSearchCriterion(Tc.COUNTRY, DbCompareOperand.EQUAL, countryId);
		DbSearchCriterion cr2 = new DbSearchCriterion(Tc.ANIMAL_ID, DbCompareOperand.EQUAL, animalUid);

		DbDataArray dbArray = svr.getObjects(new DbSearchExpression().addDbSearchItem(cr1).addDbSearchItem(cr2),
				SvReader.getTypeIdByName(Tc.ANIMAL), null, 0, 0);
		if (dbArray != null && !dbArray.getItems().isEmpty()) {
			result = dbArray.getItems().get(0);
		}
		return result;
	}

	/**
	 * Method that returns DbDataObject of type SVAROG_USER for the appropriate user
	 * 
	 * @param userName userName of the user searched for
	 * 
	 * @param svr      SvReader instance
	 * 
	 * @throws SvException
	 * @return DbDataObject
	 */
	public DbDataObject findAppropriateUserByUserName(String userName, SvReader svr) throws SvException {

		DbDataObject result = null;
		DbSearchCriterion cr1 = new DbSearchCriterion(Tc.USER_NAME, DbCompareOperand.EQUAL, userName);
		DbSearchExpression dbs = new DbSearchExpression().addDbSearchItem(cr1);
		DbDataArray dbArray = svr.getObjects(dbs, svCONST.OBJECT_TYPE_USER, null, 0, 0);
		if (dbArray != null && !dbArray.getItems().isEmpty()) {
			result = dbArray.getItems().get(0);
		}
		return result;
	}

	/**
	 * Method that returns DbDataArray of type HOLDINGS for the appropriate
	 * areaCode/areaName
	 * 
	 * @param areaName code of the area / sorry for inappropriate naming
	 * 
	 * @param svr      SvReader instance
	 * 
	 * @throws SvException
	 * @return DbDataObject
	 */
	public DbDataArray findHoldingsPerArea(String areaName, SvReader svr) throws SvException {
		DbDataArray result = null;
		DbSearchCriterion cr1 = new DbSearchCriterion(Tc.PIC, DbCompareOperand.LIKE, areaName + Tc.PERCENT_OPERATOR);
		result = svr.getObjects(new DbSearchExpression().addDbSearchItem(cr1), SvReader.getTypeIdByName(Tc.HOLDING),
				null, 0, 0);
		return result;
	}

	public DbDataArray findDataPerSingleFilter(String columnName, String columnValue, DbCompareOperand compareOperand,
			Long objTypeId, SvReader svr) throws SvException {
		return findDataPerSingleFilter(columnName, columnValue, compareOperand, objTypeId, 0, svr);
	}

	public DbDataArray findDataPerSingleFilter(String columnName, String columnValue, DbCompareOperand compareOperand,
			Long objTypeId, Integer rowLimit, SvReader svr) throws SvException {
		return findDataPerSingleFilter(columnName, columnValue, compareOperand, objTypeId, rowLimit, null, svr);
	}

	public DbDataArray findDataPerSingleFilter(String columnName, String columnValue, DbCompareOperand compareOperand,
			Long objTypeId, Integer rowLimit, DateTime refDate, SvReader svr) throws SvException {
		DbDataArray result = null;
		DbSearchCriterion cr1 = null;
		if (compareOperand.equals(DbCompareOperand.LIKE)) {
			cr1 = new DbSearchCriterion(columnName, compareOperand, columnValue + Tc.PERCENT_OPERATOR);
		} else if (compareOperand.equals(DbCompareOperand.ILIKE)) {
			cr1 = new DbSearchCriterion(columnName, compareOperand,
					Tc.PERCENT_OPERATOR + columnValue + Tc.PERCENT_OPERATOR);
		} else {
			cr1 = new DbSearchCriterion(columnName, compareOperand, columnValue);
		}
		result = svr.getObjects(new DbSearchExpression().addDbSearchItem(cr1), objTypeId, refDate, rowLimit, 0);
		return result;
	}

	public DbDataArray findHoldingDataPerAreaGeostatCode(String columnName, String columnValue,
			DbCompareOperand compareOperand, String areaGeostatCode, SvReader svr) throws SvException {
		DbDataArray result = null;
		DbSearchCriterion cr1 = null;
		if (compareOperand.equals(DbCompareOperand.LIKE)) {
			cr1 = new DbSearchCriterion(columnName, compareOperand, columnValue + Tc.PERCENT_OPERATOR);
		} else {
			cr1 = new DbSearchCriterion(columnName, compareOperand, columnValue);
		}
		DbSearchCriterion cr2 = new DbSearchCriterion(columnName, DbCompareOperand.LIKE,
				areaGeostatCode + Tc.PERCENT_OPERATOR);
		result = svr.getObjects(new DbSearchExpression().addDbSearchItem(cr1).addDbSearchItem(cr2),
				SvReader.getTypeIdByName(Tc.HOLDING), null, 0, 0);
		return result;
	}

	public DbDataArray findAppropriateHoldingBySearch(String columnName, String columnValue, SvReader svr)
			throws SvException {
		DbDataArray holdings = new DbDataArray();
		DbCompareOperand compareOperand = autoSetTheDbCompareOperandParam(columnName, columnValue, svr);
		DbDataArray orgUnitPerUser = getAllOrgUnitsPerUser(svr);
		if (getAllOrgUnitsPerUser(svr) == null || getAllOrgUnitsPerUser(svr).getItems().isEmpty()) {
			holdings = findDataPerSingleFilter(columnName, columnValue, compareOperand,
					SvReader.getTypeIdByName(Tc.HOLDING), svr);
		} else {
			for (DbDataObject tempOrgUnit : orgUnitPerUser.getItems()) {
				if (tempOrgUnit.getVal(Tc.EXTERNAL_ID) != null) {
					String geostatAreaCode = tempOrgUnit.getVal(Tc.EXTERNAL_ID).toString();
					if (geostatAreaCode.startsWith(columnValue) || columnValue.startsWith(geostatAreaCode)) {
						DbDataArray tempHoldings = findDataPerSingleFilter(columnName, columnValue, compareOperand,
								SvReader.getTypeIdByName(Tc.HOLDING), svr);
						if (tempHoldings != null && !tempHoldings.getItems().isEmpty()) {
							for (DbDataObject tempHolding : tempHoldings.getItems()) {
								holdings.addDataItem(tempHolding);
							}
						}
					}

				}
			}
		}
		return holdings;
	}

	/**
	 * Method that returns DbDataArray of type HOLDINGS for the appropriate
	 * areaCode/areaName
	 * 
	 * @param areaName code of the area / sorry for inappropriate naming
	 * 
	 * @param svr      SvReader instance
	 * 
	 * @throws SvException
	 * @return DbDataObject
	 */
	public int findNumberOfHoldingsInActiveQuarantinePerArea(String areaName, SvReader svr) throws SvException {
		ValidationChecks vc = new ValidationChecks();
		int noOfHoldingsInActiveQuar = 0;
		DbDataArray result = null;
		DbSearchCriterion cr1 = new DbSearchCriterion(Tc.PIC, DbCompareOperand.LIKE, areaName + Tc.PERCENT_OPERATOR);
		result = svr.getObjects(new DbSearchExpression().addDbSearchItem(cr1), SvReader.getTypeIdByName(Tc.HOLDING),
				null, 0, 0);
		for (DbDataObject tempHolding : result.getItems()) {
			if (vc.checkIfHoldingBelongsInActiveQuarantine(tempHolding.getObject_id(), svr).booleanValue()) {
				noOfHoldingsInActiveQuar++;
			}
		}
		return noOfHoldingsInActiveQuar;
	}

	/**
	 * Method that returns DbDataArray of type AREA_HEALTH_STATUSes for the
	 * appropriate areaObj
	 * 
	 * @param areObj DbDataObject of AREA
	 * 
	 * @param svr    SvReader instance
	 * 
	 * @throws SvException
	 * @return DbDataObject
	 */
	public DbDataArray findHealthStatusesPerArea(DbDataObject areObj, SvReader svr) throws SvException {
		DbDataArray result = null;
		result = svr.getObjectsByParentId(areObj.getObject_id(), SvReader.getTypeIdByName(Tc.AREA_HEALTH), null, 0, 0);
		return result;
	}

	/**
	 * Method that returns DbDataObject of type FLOCK for the appropriate animal
	 * flock id
	 * 
	 * @param flockId flock identification number
	 * 
	 * @param svr     SvReader instance
	 * 
	 * @throws SvException
	 * @return DbDataObject
	 */
	public DbDataObject findAppropriateFlockByFlockId(String flockId, SvReader svr) throws SvException {

		DbDataObject result = null;
		DbSearchCriterion cr1 = new DbSearchCriterion(Tc.FLOCK_ID, DbCompareOperand.EQUAL, flockId);
		DbDataArray dbArray = svr.getObjects(new DbSearchExpression().addDbSearchItem(cr1),
				SvReader.getTypeIdByName(Tc.FLOCK), null, 0, 0);
		if (dbArray != null && !dbArray.getItems().isEmpty()) {
			result = dbArray.getItems().get(0);
		}
		return result;
	}

	/**
	 * Method that returns history list of the animal
	 * 
	 * @param animalId animal_id of the animal
	 * 
	 * @param svr      SvReader instance
	 * 
	 * @throws SvException
	 * @return DbDataArray
	 */
	public DbDataArray searchByAnimalIdWithHistory(String animalId, String animalClass, SvReader svr)
			throws SvException {
		DbDataArray result = null;
		DbSearchExpression findAnimal = new DbSearchExpression();
		DbSearchCriterion srchFilter1 = new DbSearchCriterion(Tc.ANIMAL_ID, DbCompareOperand.EQUAL, animalId);
		DbSearchCriterion srchFilter2 = new DbSearchCriterion(Tc.ANIMAL_CLASS, DbCompareOperand.EQUAL, animalClass);
		findAnimal.addDbSearchItem(srchFilter1).addDbSearchItem(srchFilter2);
		result = svr.getObjectsHistory(findAnimal, SvReader.getTypeIdByName(Tc.ANIMAL), 0, 0);
		result.getSortedItems(Tc.PKID, true);
		return result;
	}

	/**
	 * Method that returns history list of the flock ordered by PKID (ascending)
	 * 
	 * @param flockId
	 * @param svr
	 * @return
	 * @throws SvException
	 */
	public DbDataArray searchByFlockIdWithHistory(String flockId, SvReader svr) throws SvException {
		DbDataArray result = null;
		DbSearchExpression findAnimal = new DbSearchExpression();
		DbSearchCriterion srchFilter1 = new DbSearchCriterion(Tc.FLOCK_ID, DbCompareOperand.EQUAL, flockId);
		findAnimal.addDbSearchItem(srchFilter1);
		result = svr.getObjectsHistory(findAnimal, SvReader.getTypeIdByName(Tc.FLOCK), 0, 0);
		result.getSortedItems(Tc.PKID, true);
		return result;
	}

	/**
	 * Method that returns last version of the animal by object_id
	 * 
	 * @param animalObjId OBJECT_ID of the animal
	 * 
	 * @param svr         SvReader instance
	 * 
	 * @throws SvException
	 * @return DbDataObject
	 */
	public DbDataObject getLastAnimalIdVersion(Long animalObjId, SvReader svr) throws SvException {
		return svr.getObjectById(animalObjId, SvReader.getTypeIdByName(Tc.ANIMAL), null);
	}

	/**
	 * Method that calculates animal age
	 * 
	 * @param animalObjId object_id of the ANIMAL
	 * 
	 * @param svr         SvReader instance
	 * 
	 * @throws SvException
	 * @return String
	 */
	public String calcAnimalAge(Long animalObjId, String tableName, SvReader svr) throws SvException {
		String result = Tc.NOT_AVAILABLE_NA;
		if (animalObjId != null) {
			DbDataObject animalObj = svr.getObjectById(animalObjId, SvReader.getTypeIdByName(tableName), null);
			if (animalObj.getVal(Tc.BIRTH_DATE) != null) {
				Date birthDate = java.sql.Date.valueOf(animalObj.getVal(Tc.BIRTH_DATE).toString());
				Calendar calendar = Calendar.getInstance();
				java.sql.Date dtNow = new java.sql.Date(calendar.getTime().getTime());
				Long diffInYears = ChronoUnit.YEARS.between(LocalDate.parse(birthDate.toString()),
						LocalDate.parse(dtNow.toString()));
				Long diffInMonths = ChronoUnit.MONTHS.between(LocalDate.parse(birthDate.toString()),
						LocalDate.parse(dtNow.toString()));
				int months = Integer.valueOf(diffInMonths.toString()) % 12;
				result = diffInYears + " years, " + months + " months";
			}
		}
		return result;
	}

	public String calcAnimalAge(Long animalObjId, SvReader svr) throws SvException {
		return calcAnimalAge(animalObjId, Tc.ANIMAL, svr);
	}

	/**
	 * Method that returns list of animals per holding
	 * 
	 * @param tableName    name of the table (FLOCK or ANIMAL)
	 * 
	 * @param animalType   animal class when animal, animal type when flock
	 * 
	 * @param parentId     OBJECT_ID of the parent_id
	 * 
	 * @param animalStatus Status of the animal
	 * 
	 * @param svr          SvReader instance
	 * 
	 * @throws SvException
	 * @return DbDataArray
	 */
	public DbDataArray getAnimalsCount(String tableName, String animalType, Long parentId, String animalStatus,
			SvReader svr) {
		DbDataArray result = new DbDataArray();
		String tableField = Tc.EMPTY_STRING;
		Boolean isPet = false;
		if (tableName.equals(Tc.ANIMAL)) {
			tableField = Tc.ANIMAL_CLASS;
		} else if (tableName.equals(Tc.FLOCK)) {
			tableField = Tc.ANIMAL_TYPE;
		} else if (tableName.equals(Tc.PET)) {
			// this is for pet, will be extended in future
			isPet = true;
		}
		try {
			// table field should be ANIMALL_CLASS for ANIMAL, ANIMAL_TYPE for
			// FLOCK
			DbSearchCriterion cr1 = new DbSearchCriterion(tableField, DbCompareOperand.EQUAL, animalType);
			DbSearchCriterion cr2 = new DbSearchCriterion(Tc.PARENT_ID, DbCompareOperand.EQUAL, parentId);
			DbSearchCriterion cr3 = new DbSearchCriterion(Tc.STATUS, DbCompareOperand.EQUAL, animalStatus);
			DbSearchExpression dbse = new DbSearchExpression();
			if (!isPet) {
				dbse.addDbSearchItem(cr1);
			}
			dbse.addDbSearchItem(cr2).addDbSearchItem(cr3);
			result = svr.getObjects(dbse, SvReader.getTypeIdByName(tableName), null, 0, 0);
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage(), e);
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
		return result;
	}

	/**
	 * Method that returns list of animal siblings according mother tag id
	 * 
	 * @param animal DbDataObject of the animal
	 * @param svr    SvReader instance
	 * @return DbDataArray
	 * @throws SvException
	 */
	public DbDataArray getAnimalSiblingsAccordingMotherTagId(DbDataObject animal, SvReader svr) throws SvException {
		DbDataArray results = new DbDataArray();
		Reader rdr = new Reader();
		if (animal.getVal(Tc.MOTHER_TAG_ID) != null && animal.getVal(Tc.ANIMAL_CLASS) != null) {
			DbDataObject animalMotherObj = rdr.findAppropriateAnimalByAnimalIdAndAnimalClass(
					animal.getVal(Tc.MOTHER_TAG_ID).toString(), animal.getVal(Tc.ANIMAL_CLASS).toString(), true, svr);
			if (animalMotherObj != null) {
				DbSearchCriterion cr1 = new DbSearchCriterion(Tc.MOTHER_TAG_ID, DbCompareOperand.EQUAL,
						animalMotherObj.getVal(Tc.ANIMAL_ID).toString());
				DbSearchCriterion cr2 = new DbSearchCriterion(Tc.ANIMAL_CLASS, DbCompareOperand.EQUAL,
						animalMotherObj.getVal(Tc.ANIMAL_CLASS).toString());
				results = svr.getObjects(new DbSearchExpression().addDbSearchItem(cr1).addDbSearchItem(cr2),
						SvReader.getTypeIdByName(Tc.ANIMAL), null, 0, 0);
				results.getSortedItems(Tc.BIRTH_DATE);
			}

		}
		return results;
	}

	/**
	 * Method that returns list of fields that needs to be filled in ANIMAL
	 * 
	 * @param dboAnimal ANIMAL DbDataObject
	 * @param svr       SvReader instance
	 * @return ArrayList <String> of mandatory fields
	 * @throws SvException
	 */
	public ArrayList<String> getAnimalMandatoryFields(DbDataObject dboAnimal, SvReader svr) throws SvException {
		String localeId = svr.getUserLocaleId(svr.getInstanceUser());
		ArrayList<String> listOfMandatoryFields = new ArrayList<>();
		/*
		 * if (dboAnimal.getVal(Tc.BIRTH_DATE) == null) {
		 * listOfMandatoryFields.add(I18n.getText(localeId, "animal.birth_date")); }
		 */
		/*
		 * if (dboAnimal.getVal(Tc.REGISTRATION_DATE) == null) {
		 * listOfMandatoryFields.add(I18n.getText(localeId,
		 * "animal.registration_date")); }
		 */
		/*
		 * if (dboAnimal.getVal(Tc.COLOR) == null) {
		 * listOfMandatoryFields.add(I18n.getText(localeId, "animal.color")); }
		 */
		/*
		 * if (dboAnimal.getVal(Tc.GENDER) == null) {
		 * listOfMandatoryFields.add(I18n.getText(localeId, "animal.gender")); }
		 */
		if (dboAnimal.getVal(Tc.ANIMAL_CLASS) == null) {
			listOfMandatoryFields.add(I18n.getText(localeId, "animal.animal_class"));
		}
		/*
		 * if (dboAnimal.getVal(Tc.ANIMAL_RACE) == null) {
		 * listOfMandatoryFields.add(I18n.getText(localeId, "animal.animal_race")); }
		 */
		if (dboAnimal.getVal(Tc.ANIMAL_ID) == null) {
			listOfMandatoryFields.add(I18n.getText(localeId, "animal.animal_id"));
		}
		if (dboAnimal.getVal(Tc.COUNTRY) == null) {
			listOfMandatoryFields.add(I18n.getText(localeId, "animal.country"));
		}
		return listOfMandatoryFields;
	}

	/**
	 * Method that returns animal by animal bar code string
	 * 
	 * @param animalBarCode Animal bar code
	 * @param svr           SvReader instance
	 * @return
	 * @throws SvException
	 */
	public DbDataObject getAnimalByBarCode(String animalBarCode, SvReader svr) throws SvException {
		DbDataObject result = null;
		String countryId = Tc.EMPTY_STRING;
		String animalId = Tc.EMPTY_STRING;
		int strLength = animalBarCode.length();
		if (strLength < 3 || !Character.isLetter(animalBarCode.charAt(0))) {
			throw (new SvException("naits.error.animalBarCodePatternIsNotValidForSearch", svCONST.systemUser, null,
					null));
		}
		if (strLength > 2) {
			countryId = animalBarCode.substring(0, 2);
			animalId = animalBarCode.substring(2, strLength);
			if (countryId.equals(Tc.GE)) {
				try {
					result = findAppropriateAnimalByAnimalId(animalId, svr);
				} catch (SvException e) {
					log4j.error(e);
				}
			} else {
				try {
					result = findAppropriateForeignAnimalByBarCode(countryId, animalId, svr);
				} catch (SvException e) {
					log4j.error(e);
				}
			}
		}
		return result;
	}

	public String getAnimalAndSourceHoldingObjInfo(DbDataObject dboAnimal, String localeId, SvReader svr,
			String... nameOfField) throws SvException {
		DbDataObject dboSourceHolding = svr.getObjectById(dboAnimal.getParent_id(),
				SvReader.getTypeIdByName(Tc.HOLDING), null);
		// Source holding NAME and PIC info
		String result = I18n.getText(localeId, "naits.info.origin_holding_info")
				+ I18n.getText(localeId, "naits.main.search.by_holding_name") + ": "
				+ (dboSourceHolding.getVal(Tc.NAME) != null ? dboSourceHolding.getVal(Tc.NAME).toString()
						: Tc.NOT_AVAILABLE_NA)
				+ ", " + I18n.getText(localeId, "naits.holding.pic_info") + dboSourceHolding.getVal(Tc.PIC).toString()
				+ "; " + I18n.getText(localeId, "naits.info.animal_info");
		int counter = 0;
		// Animal info
		for (String tempNameOfField : nameOfField) {
			counter++;
			String labelInfo = I18n.getText(localeId, "animal." + tempNameOfField.toLowerCase());
			result += dboAnimal.getVal(tempNameOfField) != null
					? labelInfo + ": "
							+ decodeCodeValue(dboAnimal.getObject_type(), tempNameOfField,
									dboAnimal.getVal(tempNameOfField).toString(), localeId, svr)
					: labelInfo + ": N/A";
			if (counter < nameOfField.length)
				result += ", ";
		}
		return result;
	}

	public DbDataObject getInventoryItemAccordingAnimalParams(DbDataObject dbo, Boolean checkParentId, SvReader svr)
			throws SvException {
		return getInventoryItem(dbo, Tc.NON_APPLIED, checkParentId, svr);
	}

	/**
	 * Method to fetch inventory item per Animal/Pet object
	 * 
	 * @param dbo           Pet or Animal DbDataObject
	 * @param earTagStatus  Tag status we are looking for
	 *                      (APPLIED/NON_APPLIED/NON_APPLIED_LOST/APPLIED_LOST/FAULTY/DAMAGED/REPLACED)
	 * @param checkParentId Check if Inventory item is child of Pet or Animal object
	 * @param svr           SvReader instance
	 * @return
	 * @throws SvException
	 */
	public DbDataObject getInventoryItem(DbDataObject dbo, String defaultEarTag, String earTagStatus,
			String animalClassFieldName, Boolean checkParentId, SvReader svr) throws SvException {
		DbDataObject dboInventoryItem = null;
		String tagType = Tc.EMPTY_STRING;
		String fieldCrit1 = Tc.ANIMAL_ID;
		String fieldCrit2 = Tc.ANIMAL_CLASS;
		DbDataArray arrInventoryItems = new DbDataArray();
		if (animalClassFieldName != null) {
			fieldCrit2 = animalClassFieldName;
		}
		if (dbo.getObject_type().equals(SvReader.getTypeIdByName(Tc.PET))) {
			fieldCrit1 = Tc.PET_TAG_ID;
			fieldCrit2 = Tc.PET_TAG_TYPE;
		}
		tagType = getTagTypeAccordingAnimalOrPetType(dbo, fieldCrit2);
		// when editing animal/pet object case, is sufficient to take its childs
		// from INV_ITEM obj.type
		if (!dbo.getObject_id().equals(0L)) {
			DbDataArray finalArrayInvItems = new DbDataArray();
			arrInventoryItems = svr.getObjectsByParentId(dbo.getObject_id(),
					SvReader.getTypeIdByName(Tc.INVENTORY_ITEM), null, 0, 0);
			for (DbDataObject dboTempInvItem : arrInventoryItems.getItems()) {
				if (dboTempInvItem.getVal(Tc.EAR_TAG_NUMBER).equals(dbo.getVal(fieldCrit1))
						&& dboTempInvItem.getVal(Tc.TAG_TYPE).equals(tagType)) {
					finalArrayInvItems.addDataItem(dboTempInvItem);
				}
			}
			arrInventoryItems = finalArrayInvItems;
		} else {
			if (dbo.getVal(fieldCrit1) != null && dbo.getVal(fieldCrit2) != null) {
				DbSearchExpression dbse = new DbSearchExpression();
				String animalId = dbo.getVal(fieldCrit1).toString();
				if (defaultEarTag != null) {
					animalId = defaultEarTag;
				}
				DbSearchCriterion cr1 = new DbSearchCriterion(Tc.EAR_TAG_NUMBER, DbCompareOperand.EQUAL, animalId);
				DbSearchCriterion cr2 = null;
				if (earTagStatus != null) {
					cr2 = new DbSearchCriterion(Tc.TAG_STATUS, DbCompareOperand.EQUAL, earTagStatus);
				}

				DbSearchCriterion cr3 = new DbSearchCriterion(Tc.TAG_TYPE, DbCompareOperand.EQUAL, tagType);
				dbse.addDbSearchItem(cr1);
				if (cr2 != null) {
					dbse.addDbSearchItem(cr2);
				}
				dbse.addDbSearchItem(cr3);
				if (checkParentId && !dbo.getObject_id().equals(0L)) {
					DbSearchCriterion cr4 = new DbSearchCriterion(Tc.PARENT_ID, DbCompareOperand.EQUAL,
							dbo.getObject_id());
					dbse.addDbSearchItem(cr4);
				}
				arrInventoryItems = svr.getObjects(dbse, SvReader.getTypeIdByName(Tc.INVENTORY_ITEM), null, 0, 0);
			}
		}
		if (!arrInventoryItems.getItems().isEmpty()) {
			dboInventoryItem = arrInventoryItems.get(0);
		}
		return dboInventoryItem;
	}

	public DbDataObject getInventoryItem(DbDataObject dbo, String defaultEarTag, String earTagStatus,
			Boolean checkParentId, SvReader svr) throws SvException {
		return getInventoryItem(dbo, defaultEarTag, earTagStatus, null, checkParentId, svr);
	}

	public String getTagTypeAccordingAnimalOrPetType(DbDataObject dbo, String fieldName) {
		String tagType = Tc.EMPTY_STRING;
		if (dbo.getVal(fieldName) != null) {
			switch (dbo.getVal(fieldName).toString()) {
			case "1":
			case "2":
				// Cattle type (Buffalo also)
				tagType = "1";
				break;
			case "9":
			case "10":
				// Sheep type (Goat also)
				tagType = "3";
				break;
			case "11":
				// Pig type
				tagType = "4";
				break;
			case "12":
			case "400":
				// Ungulates type (Horse and Donkey)
				tagType = "5";
				break;
			// 2 cases for Pets
			case "MICROCHIP":
				// Pet RFID
				tagType = "7";
				break;
			case "EAR_TAG":
				// Pet visual tag
				tagType = "6";
				break;
			default:
				break;
			}
		}
		return tagType;
	}

	public DbDataObject getInventoryItem(DbDataObject dbo, String earTagStatus, Boolean checkParentId, SvReader svr)
			throws SvException {
		return getInventoryItem(dbo, null, earTagStatus, checkParentId, svr);
	}

	public DbDataArray getAnimalsLinkedToExportCertificate(DbDataObject dboExportCert, SvReader svr)
			throws SvException {
		DbDataObject linkBetweenAnimalAndExportCert = SvReader.getLinkType(Tc.ANIMAL_EXPORT_CERT,
				SvReader.getTypeIdByName(Tc.ANIMAL), SvReader.getTypeIdByName(Tc.EXPORT_CERT));
		return svr.getObjectsByLinkedId(dboExportCert.getObject_id(), dboExportCert.getObject_type(),
				linkBetweenAnimalAndExportCert, SvReader.getTypeIdByName(Tc.ANIMAL), true, null, 0, 0);
	}

	/**
	 * Method that returns appropriate animal races depend on animal class
	 * 
	 * @param codeItemValue
	 * @param svr
	 * @return
	 * @throws SvException
	 */
	public DbDataArray getAppropriateAnimalRacesPerClass(String codeItemValue, SvReader svr) throws SvException {
		DbDataArray result = new DbDataArray();
		DbDataArray allRaces = null;
		if (codeItemValue != null && codeItemValue.trim().length() > 0) {
			DbSearchCriterion cr1 = new DbSearchCriterion(Tc.ANIMAL_CLASS, DbCompareOperand.EQUAL, codeItemValue);
			DbSearchExpression dbse = new DbSearchExpression();
			dbse.addDbSearchItem(cr1);
			DbDataArray ar = svr.getObjects(dbse, SvReader.getTypeIdByName(Tc.ANIMAL_TYPE), null, 0, 0);
			if (ar != null && !ar.getItems().isEmpty()) {
				allRaces = searchForDependentAnimalRace(Tc.ANIMAL_RACE, svr);
				String userLocale = svr.getUserLocaleId(svr.getInstanceUser());
				if (allRaces != null && !allRaces.getItems().isEmpty()) {
					for (DbDataObject animalType : ar.getItems()) {
						if (animalType != null && animalType.getVal(Tc.ANIMAL_RACE) != null
								&& animalType.getVal(Tc.ANIMAL_RACE).toString().length() > 0) {
							for (DbDataObject race : allRaces.getItems()) {
								if (race != null && race.getVal(Tc.CODE_VALUE) != null && race.getVal(Tc.CODE_VALUE)
										.toString().equals(animalType.getVal(Tc.ANIMAL_RACE).toString())) {
									String translatedCodeItem = I18n.getText(userLocale,
											race.getVal(Tc.LABEL_CODE).toString());
									race.setVal(Tc.LBL_TRANSL, translatedCodeItem);
									result.addDataItem(race);
								}
							}
						}
					}
				}
			}
		}
		return result;
	}

	public DbDataArray searchForDependentAnimalRace(String codeListName, SvReader svr) throws SvException {
		DbDataArray itemsFound = null;
		DbSearchExpression srchExpr = new DbSearchExpression();
		DbSearchCriterion filterByParentCodeValue = new DbSearchCriterion(Tc.PARENT_CODE_VALUE, DbCompareOperand.EQUAL,
				codeListName);
		srchExpr.addDbSearchItem(filterByParentCodeValue);
		DbDataArray searchResult = svr.getObjects(srchExpr, svCONST.OBJECT_TYPE_CODE, null, 0, 0);
		if (!searchResult.getItems().isEmpty()) {
			itemsFound = searchResult;
		}
		return itemsFound;
	}

	// ANIMAL MOVEMENTS METHODS

	/**
	 * Method that returns if movement is found
	 * 
	 * @param movementParentObject
	 * 
	 * @param destinationHoldingObj
	 * 
	 * @param svc
	 * 
	 * @return true/false
	 * @throws SvException
	 */
	public boolean checkIfAnimalOrFlockMovementExist(DbDataObject movementParentObject,
			DbDataObject destinationHoldingObj, SvCore svc) throws SvException {
		return checkIfAnimalOrFlockMovementExist(movementParentObject, destinationHoldingObj, false, svc);
	}

	public Boolean checkIfObjectHasBeenEditedDependOnSpecificFields(DbDataObject dboNew, DbDataObject dboOld,
			String... fieldNames) {
		boolean result = false;
		LinkedHashMap<SvCharId, Object> lhm = dboNew.getValuesMap();
		for (Entry<SvCharId, Object> field : lhm.entrySet()) {
			for (String fieldName : fieldNames) {
				if (fieldName.equals(field.getKey().toString())) {
					String fieldValue1 = dboOld.getVal(fieldName) != null ? dboOld.getVal(fieldName).toString() : "";
					String fieldValue2 = field.getValue() != null ? field.getValue().toString() : "";
					if (!fieldValue1.equals(fieldValue2)) {
						result = true;
						break;
					}
				}
			}
			if (result) {
				break;
			}
		}
		return result;
	}

	public Boolean checkIfAnimalOrFlockMovementExist(DbDataObject animalOrFlockObj, DbDataObject destinationHoldingObj,
			Boolean includeRejected, SvCore svc) throws SvException {
		Boolean result = false;
		String destinationHoldingPic = null;
		if (destinationHoldingObj.getVal(Tc.PIC) != null) {
			destinationHoldingPic = destinationHoldingObj.getVal(Tc.PIC).toString();
		}
		DbDataArray existingAnimalOrFlockMovements = getExistingAnimalOrFlockMovements(animalOrFlockObj,
				destinationHoldingPic, Tc.VALID, svc);
		if (!existingAnimalOrFlockMovements.getItems().isEmpty()) {
			result = true;
		} else if (includeRejected && existingAnimalOrFlockMovements.getItems().isEmpty()) {
			existingAnimalOrFlockMovements = getExistingAnimalOrFlockMovements(animalOrFlockObj, destinationHoldingPic,
					Tc.REJECTED, svc);
			if (!existingAnimalOrFlockMovements.getItems().isEmpty()) {
				result = true;
			}
		}
		return result;
	}

	/**
	 * Method that returns existing movement of type ANIMAL_MOVEMENT
	 * 
	 * @param animalOrFlockObj      DbDataObject of type ANIMAL or FLOCK
	 * 
	 * @param destinationHoldingPic holding PIC destination of the movement
	 * 
	 * @param status                status of the movement
	 * 
	 * @param svc                   SvCore instance
	 * 
	 * @throws SvException
	 * @return DbDataObject
	 */
	public DbDataArray getExistingAnimalOrFlockMovements(DbDataObject animalOrFlockObj, String destinationHoldingPic,
			String status, SvCore svc) throws SvException {
		return getExistingAnimalOrFlockMovements(animalOrFlockObj, destinationHoldingPic, status, true, svc);
	}

	/**
	 * Method that returns existing movement of type ANIMAL_MOVEMENT
	 * 
	 * @param animalOrFlockObj      DbDataObject of type ANIMAL or FLOCK
	 * 
	 * @param destinationHoldingPic holding PIC destination of the movement
	 * 
	 * @param status                status of the movement
	 * 
	 * @param useCache
	 * 
	 * @param svc                   SvCore instance
	 * 
	 * @throws SvException
	 * @return DbDataObject
	 */
	public DbDataArray getExistingAnimalOrFlockMovements(DbDataObject animalOrFlockObj, String destinationHoldingPic,
			String status, Boolean isWithoutDestination, Boolean useCache, SvCore svc) throws SvException {
		DbDataArray result = null;
		SvReader svr = new SvReader(svc);
		DbSearchExpression findAnimalOrFlockMovement = new DbSearchExpression();
		String movementType = Tc.EMPTY_STRING;
		Object animalOrFlockId = null;
		String tableToSearchFrom = null;
		String movementIdColumn = null;
		if (animalOrFlockObj.getObject_type().equals(SvReader.getTypeIdByName(Tc.ANIMAL))) {
			movementType = Tc.INDIVIDUAL;
			animalOrFlockId = animalOrFlockObj.getVal(Tc.ANIMAL_ID);
			tableToSearchFrom = Tc.ANIMAL_MOVEMENT;
			movementIdColumn = Tc.ANIMAL_EAR_TAG;
		} else if (animalOrFlockObj.getObject_type().equals(SvReader.getTypeIdByName(Tc.FLOCK))) {
			movementType = Tc.GROUP;
			animalOrFlockId = animalOrFlockObj.getVal(Tc.FLOCK_ID);
			tableToSearchFrom = Tc.FLOCK_MOVEMENT;
			movementIdColumn = Tc.FLOCK_ID;
		}
		// current parent of animal/flock - parent of movementParentObj, always
		// a object from HOLDING type
		if (tableToSearchFrom != null) {
			DbDataObject dboSourceHolding = svr.getObjectById(animalOrFlockObj.getParent_id(),
					SvReader.getTypeIdByName(Tc.HOLDING), null);
			DbSearchCriterion srchFilter1 = new DbSearchCriterion(Tc.PARENT_ID, DbCompareOperand.EQUAL,
					animalOrFlockObj.getObject_id());
			DbSearchCriterion srchFilter2 = new DbSearchCriterion(Tc.SOURCE_HOLDING_ID, DbCompareOperand.EQUAL,
					dboSourceHolding.getVal(Tc.PIC));
			DbSearchCriterion srchFilter3 = null;
			if (isWithoutDestination) {
				srchFilter3 = new DbSearchCriterion(Tc.DESTINATION_HOLDING_ID, DbCompareOperand.ISNULL);
			} else {
				srchFilter3 = new DbSearchCriterion(Tc.DESTINATION_HOLDING_ID, DbCompareOperand.EQUAL,
						destinationHoldingPic);
			}
			DbSearchCriterion srchFilter4 = new DbSearchCriterion(Tc.MOVEMENT_TYPE, DbCompareOperand.EQUAL,
					movementType);
			DbSearchCriterion srchFilter5 = new DbSearchCriterion(Tc.STATUS, DbCompareOperand.EQUAL, status);
			DbSearchCriterion srchFilter6 = new DbSearchCriterion(movementIdColumn, DbCompareOperand.EQUAL,
					animalOrFlockId);
			// add status check in case when animalOrFlockObj is SOLD/LOST,
			// disable
			// the srchFilter3; for this case of movement destination holding is
			// missing
			if (animalOrFlockObj.getStatus().equals(Tc.SOLD) || animalOrFlockObj.getStatus().equals(Tc.LOST)) {
				findAnimalOrFlockMovement.addDbSearchItem(srchFilter1).addDbSearchItem(srchFilter2)
						.addDbSearchItem(srchFilter4).addDbSearchItem(srchFilter5).addDbSearchItem(srchFilter6);
			} else {
				findAnimalOrFlockMovement.addDbSearchItem(srchFilter1).addDbSearchItem(srchFilter2)
						.addDbSearchItem(srchFilter3).addDbSearchItem(srchFilter4).addDbSearchItem(srchFilter5)
						.addDbSearchItem(srchFilter6);
			}
			if (useCache.booleanValue()) {
				result = svr.getObjects(findAnimalOrFlockMovement, SvReader.getTypeIdByName(tableToSearchFrom), null, 0,
						0);
			} else {
				result = svr.getObjects(findAnimalOrFlockMovement, SvReader.getTypeIdByName(tableToSearchFrom),
						new DateTime(), 0, 0);
			}
		}
		svr.release();
		return result;
	}

	public DbDataArray getExistingAnimalOrFlockMovements(DbDataObject animalOrFlockObj, String destinationHoldingPic,
			String status, Boolean useCache, SvCore svc) throws SvException {
		return getExistingAnimalOrFlockMovements(animalOrFlockObj, destinationHoldingPic, status, false, useCache, svc);
	}

	/**
	 * Method that returns last finished animal movement
	 * 
	 * @param animalMovement
	 * @param svr
	 * @return
	 * @throws SvException
	 */
	public DbDataObject getAllSorteFinisheddAnimalMovements(DbDataObject animalMovement, SvReader svr)
			throws SvException {
		DbDataObject lastMovementOfAnimal = null;
		DbDataArray finishedMovements = new DbDataArray();
		if (animalMovement != null) {
			DbDataObject animal = svr.getObjectById(animalMovement.getParent_id(), SvReader.getTypeIdByName(Tc.ANIMAL),
					null);
			DbDataArray temp_results = new DbDataArray();
			if (animal != null) {
				temp_results = svr.getObjectsByParentId(animal.getObject_id(),
						SvReader.getTypeIdByName(Tc.ANIMAL_MOVEMENT), null, 0, 0);
			}
			if (!temp_results.getItems().isEmpty()) {
				for (DbDataObject tempAnimalMovement : temp_results.getItems()) {
					if (tempAnimalMovement != null && tempAnimalMovement.getVal(Tc.ARRIVAL_DATE) != null
							&& tempAnimalMovement.getStatus().equals(Tc.FINISHED)) {
						finishedMovements.addDataItem(tempAnimalMovement);
					}
				}
				if (!finishedMovements.getItems().isEmpty()) {
					finishedMovements.getSortedItems(Tc.ARRIVAL_DATE);
					lastMovementOfAnimal = finishedMovements.get(finishedMovements.size() - 1);
					log4j.trace("This is the last animal movement id happened: " + lastMovementOfAnimal.getObject_id());
				}
			}
		}

		return lastMovementOfAnimal;
	}

	/**
	 * Method that gets all ANIMAL MOVEMENTs with status TRANSFER by Ear Tag (Animal
	 * ID)
	 * 
	 * @param svr    SvReader instance
	 * @param earTag Ear tag of the ANIMAL
	 * @return DbDataArray
	 * @throws SvException
	 */
	public DbDataArray getTransferMovementsByEartag(SvReader svr, String earTag) throws SvException {
		DbDataArray result = new DbDataArray();
		if (earTag != null && svr != null) {
			try {
				DbSearchExpression dbse = new DbSearchExpression();
				DbSearchCriterion sc1 = new DbSearchCriterion(Tc.ANIMAL_EAR_TAG, DbCompareOperand.EQUAL, earTag);
				DbSearchCriterion sc2 = new DbSearchCriterion(Tc.MOVEMENT_REASON, DbCompareOperand.LIKE, "%TRANSFER%");

				dbse.addDbSearchItem(sc1).addDbSearchItem(sc2);

				result = svr.getObjects(dbse, SvCore.getTypeIdByName(Tc.ANIMAL_MOVEMENT), null, 0, 0);
			} catch (Exception e) {
				log4j.error("Failed fetching movements by eartag: " + e);
			}

		}
		return result;
	}

	/**
	 * Method that gets all ANIMAL MOVEMENTs with status TRANSFER by PIC
	 * 
	 * @param svr SvReader instance
	 * @param pic PIC (Holding ID)
	 * @return DbDataArray
	 * @throws SvException
	 */
	public DbDataArray getTransferMovementsByPIC(SvReader svr, String pic) throws SvException {

		DbDataArray result = new DbDataArray();
		if (pic != null && svr != null) {
			try {
				DbSearchExpression dbse = new DbSearchExpression();
				DbSearchExpression dbsePic = new DbSearchExpression();

				DbSearchCriterion sc1 = new DbSearchCriterion(Tc.DESTINATION_HOLDING_ID, DbCompareOperand.EQUAL, pic);
				sc1.setNextCritOperand(DbLogicOperand.OR.toString());
				DbSearchCriterion sc2 = new DbSearchCriterion(Tc.SOURCE_HOLDING_ID, DbCompareOperand.EQUAL, pic);
				DbSearchCriterion sc3 = new DbSearchCriterion(Tc.MOVEMENT_REASON, DbCompareOperand.LIKE, "%TRANSFER%");

				dbsePic.addDbSearchItem(sc1).addDbSearchItem(sc2);
				dbse.addDbSearchItem(dbsePic).addDbSearchItem(sc3);

				result = svr.getObjects(dbse, SvCore.getTypeIdByName(Tc.ANIMAL_MOVEMENT), null, 0, 0);
			} catch (Exception e) {
				log4j.error("Failed fetching movements by pic: " + e);
			}
		}

		return result;
	}

	/**
	 * Method that checks if certain animal/flock is allowed to move
	 * 
	 * @param sourceHoldingObj
	 * @param tempBlockReason
	 * @param blockReasons
	 * @param recommendBlck
	 * @param animalOrFlockObj
	 * @param movementObj
	 * @param svr
	 * @return
	 * @throws SvException
	 */
	public Boolean checkifMovementIsAllowedDependOnAreaHealthStatus(DbDataObject sourceHoldingObj,
			String tempBlockReason, ArrayList<String> blockReasons, ArrayList<String> recommendBlck,
			DbDataObject animalOrFlockObj, DbDataObject movementObj, SvReader svr) throws SvException {
		Boolean result = true;
		ArrayList<String> sourceHoldingAreaHealthDiseases = new ArrayList<>();
		ArrayList<String> destinationHoldingAreaHealthDiseases = new ArrayList<>();

		DbDataObject destinationHoldingObj = findAppropriateHoldingByPic(
				movementObj.getVal(Tc.DESTINATION_HOLDING_ID).toString(), svr);
		// check if source holding belongs to affected area
		if (sourceHoldingObj.getVal(Tc.VILLAGE_CODE) != null) {
			DbDataObject areaObj = findAppropriateAreaByCode(sourceHoldingObj.getVal(Tc.VILLAGE_CODE).toString(), "3",
					svr);
			tempBlockReason = findAppropriateHealthStatusForArea(areaObj, svr);
			if (!tempBlockReason.equals(Tc.FREE)) {
				DbDataArray arrSourceAreaHealth = svr.getObjectsByParentId(areaObj.getObject_id(),
						SvReader.getTypeIdByName(Tc.AREA_HEALTH), null, 0, 0);
				for (DbDataObject dboAreaHealth : arrSourceAreaHealth.getItems()) {
					sourceHoldingAreaHealthDiseases.add(dboAreaHealth.getVal(Tc.DISEASE_ID).toString());
				}
			}
		}
		// check if destination holding belongs to affected area
		if (destinationHoldingObj != null && destinationHoldingObj.getVal(Tc.VILLAGE_CODE) != null) {
			DbDataObject areaObj = findAppropriateAreaByCode(destinationHoldingObj.getVal(Tc.VILLAGE_CODE).toString(),
					"3", svr);
			tempBlockReason = findAppropriateHealthStatusForArea(areaObj, svr);
			if (!tempBlockReason.equals(Tc.FREE)) {
				DbDataArray arrDestinationAreaHealth = svr.getObjectsByParentId(areaObj.getObject_id(),
						SvReader.getTypeIdByName(Tc.AREA_HEALTH), null, 0, 0);
				for (DbDataObject dboAreaHealth : arrDestinationAreaHealth.getItems()) {
					destinationHoldingAreaHealthDiseases.add(dboAreaHealth.getVal(Tc.DISEASE_ID).toString());
				}
			}
		}
		// vaccination records should not be older than X month
		DateTime dateNow = new DateTime();
		DateTime datePeriodForAnthrax = dateNow.minusMonths(6);
		DateTime datePeriodForBrucellosis = dateNow.minusMonths(6);
		DateTime datePeriodForFmd = dateNow.minusMonths(5);

		Boolean missingVacc = false;

		DbDataArray vaccinationBooksPerAnimal = null;
		DbDataArray getVaccBooksForAnthrax = null;
		DbDataArray getVaccBooksForBrucellosis = null;
		DbDataArray getVaccBooksForFmd = null;

		if (!sourceHoldingAreaHealthDiseases.isEmpty()) {
			blockReasons.add(Tc.SOURCE_HOLDING_AFFECTED_AREA);
		}
		if (!destinationHoldingAreaHealthDiseases.isEmpty()) {
			blockReasons.add(Tc.DESTINATION_HOLDING_AFFECTED_AREA);
		}
		if (!sourceHoldingAreaHealthDiseases.isEmpty() || !destinationHoldingAreaHealthDiseases.isEmpty()) {
			// get all vaccination books
			vaccinationBooksPerAnimal = getLinkedVaccinationBooksPerAnimalOrFlock(animalOrFlockObj, svr);
			if (vaccinationBooksPerAnimal != null && !vaccinationBooksPerAnimal.getItems().isEmpty()) {
				// source/destination: Anthrax
				if (sourceHoldingAreaHealthDiseases.contains(Tc.ANTRX)
						|| destinationHoldingAreaHealthDiseases.contains(Tc.ANTRX)) {
					// get vaccination records with anthrax
					getVaccBooksForAnthrax = filterObjectsByDateTimeFrame(vaccinationBooksPerAnimal, Tc.VACC_DATE,
							datePeriodForAnthrax, dateNow, false);
					if (checkIfVaccineBookRecordPerGivenDiseaseIsMissing(getVaccBooksForAnthrax, "ANTHR", svr)) {
						missingVacc = true;
						recommendBlck.add(Tc.ANTRX_VACC_MISS);
					}
				}
				// source/destination: Brucellosis
				if (sourceHoldingAreaHealthDiseases.contains(Tc.BRUC)
						|| destinationHoldingAreaHealthDiseases.contains(Tc.BRUC)) {
					// get vaccination records with brucellosis
					getVaccBooksForBrucellosis = filterObjectsByDateTimeFrame(vaccinationBooksPerAnimal, Tc.VACC_DATE,
							datePeriodForBrucellosis, dateNow, false);
					if (checkIfVaccineBookRecordPerGivenDiseaseIsMissing(getVaccBooksForBrucellosis, Tc.BRUC, svr)) {
						missingVacc = true;
						recommendBlck.add(Tc.BRUC_VACC_MISS);
					}
				}
				// source/destination: Foot and mouth disease
				if (sourceHoldingAreaHealthDiseases.contains(Tc.FMR)
						|| destinationHoldingAreaHealthDiseases.contains(Tc.FMR)) {
					// get vaccination records with foot and mouth
					// disease
					getVaccBooksForFmd = filterObjectsByDateTimeFrame(vaccinationBooksPerAnimal, Tc.VACC_DATE,
							datePeriodForFmd, dateNow, false);
					if (checkIfVaccineBookRecordPerGivenDiseaseIsMissing(getVaccBooksForFmd, "FMD", svr)) {
						missingVacc = true;
						recommendBlck.add(Tc.FMD_VACC_MISS);
					}
				}

				if (missingVacc) {
					blockReasons.add(Tc.VACC_MISSING);
					result = false;
				}
			} else {
				// if there is no Vaccination record in Health Book
				blockReasons.add(Tc.VACC_MISSING);
				result = false;
				if (sourceHoldingAreaHealthDiseases.contains(Tc.ANTRX)
						|| destinationHoldingAreaHealthDiseases.contains(Tc.ANTRX)) {
					recommendBlck.add(Tc.ANTRX_VACC_MISS);
				}
				if (sourceHoldingAreaHealthDiseases.contains(Tc.BRUC)
						|| destinationHoldingAreaHealthDiseases.contains(Tc.BRUC)) {
					recommendBlck.add(Tc.BRUC_VACC_MISS);
				}
				if (sourceHoldingAreaHealthDiseases.contains(Tc.FMR)
						|| destinationHoldingAreaHealthDiseases.contains(Tc.FMR)) {
					recommendBlck.add(Tc.FMD_VACC_MISS);
				}
			}
		}
		return result;
	}

	/**
	 * Method to returns animal/flock pending outgoing movements
	 * 
	 * @param parentObjId Object_id of the Holding
	 * @param objectType  Object type (ANIMAL/FLOCK)
	 * @param svr
	 * @return
	 * @throws SvException
	 */
	public DbDataArray getAnimalOrFlockOutgoingPendingMovements(Long parentObjId, String objectType, SvReader svr)
			throws SvException {
		DbDataArray result = new DbDataArray();
		DbDataArray arrAnimalsOrFlockInMovement = svr.getObjectsByParentId(parentObjId,
				SvReader.getTypeIdByName(objectType), null, 0, 0);
		if (arrAnimalsOrFlockInMovement != null && !arrAnimalsOrFlockInMovement.getItems().isEmpty()) {
			for (DbDataObject dboAnimal : arrAnimalsOrFlockInMovement.getItems()) {
				if (dboAnimal.getStatus().equals(Tc.TRANSITION)) {
					result.addDataItem(dboAnimal);
				}
			}
		}
		return result;
	}

	public Integer getNumOfOutgoingAnimalsOrFlock(Long parentObjId, String objectType, SvReader svr)
			throws SvException {
		int cntOutgoingPendingMovements = 0;
		DbDataArray arrAnimalsOrFlockInMovement = getAnimalOrFlockOutgoingPendingMovements(parentObjId, objectType,
				svr);
		if (arrAnimalsOrFlockInMovement != null && !arrAnimalsOrFlockInMovement.getItems().isEmpty()) {
			cntOutgoingPendingMovements = arrAnimalsOrFlockInMovement.size();
		}
		return cntOutgoingPendingMovements;
	}

	// FLOCK METHODS

	/**
	 * Method that returns total number of flocks
	 * 
	 * @param flocks
	 * 
	 * @return Integer
	 */
	public Integer getTotalNoOfHeads(DbDataArray flocks) {
		int result = 0;
		for (DbDataObject tempFlock : flocks.getItems()) {
			if (tempFlock.getVal(Tc.TOTAL) != null) {
				result = result + Integer.valueOf(tempFlock.getVal(Tc.TOTAL).toString());
			}
		}
		return result;
	}

	// HOLDING METHODS

	/**
	 * Method that returns holding health status by animals
	 * 
	 * @param dboHolding
	 * @param svr
	 * @return
	 * @throws SvException
	 */
	public String getHoldingHealthStatus(DbDataObject dboHolding, SvReader svr) throws SvException {
		String holdingStatus = "en.positive";
		Integer precentNum = 0;
		if (dboHolding != null) {
			String healthPrecentage = calculateHoldingHealth(dboHolding.getObject_id(), svr);
			precentNum = Integer.valueOf(healthPrecentage.replace(Tc.PERCENT_OPERATOR, ""));
			if (precentNum < 100) {
				holdingStatus = "en.suspect";
				if (precentNum == 0) {
					holdingStatus = "en.negative";
				}
			}
		}
		return I18n.getText(holdingStatus) + ", " + precentNum.toString() + Tc.PERCENT_OPERATOR;
	}

	/**
	 * Method for fetching Flock linked to Vaccination book
	 * 
	 * @param dboVaccinationBook
	 * @param svr                SvReader instance
	 * @return
	 * @throws SvException
	 */
	public DbDataObject getFlockLinkedToVaccinationBook(DbDataObject dboVaccinationBook, SvReader svr)
			throws SvException {
		DbDataObject dboFlock = null;
		DbDataObject dboLinkBetweenFlockAndVaccinationBook = SvLink.getLinkType(Tc.FLOCK_VACC_BOOK,
				SvReader.getTypeIdByName(Tc.FLOCK), SvReader.getTypeIdByName(Tc.VACCINATION_BOOK));
		DbDataArray allItems = svr.getObjectsByLinkedId(dboVaccinationBook.getObject_id(),
				SvReader.getTypeIdByName(Tc.VACCINATION_BOOK), dboLinkBetweenFlockAndVaccinationBook,
				SvReader.getTypeIdByName(Tc.FLOCK), true, null, 0, 0);
		if (!allItems.getItems().isEmpty()) {
			dboFlock = allItems.get(0);
		}
		return dboFlock;
	}

	/**
	 * Method that returns next or previous holding object
	 * 
	 * @param direction        next holding or previous holding (FORWARD, BACKWARD)
	 * 
	 * @param currHoldingObjId object_id of the HOLDING
	 * 
	 * @param svr              SvReader instance
	 * 
	 * @throws SvException
	 * @return DbDataObject
	 */
	public DbDataObject getNextOrPreviousHolding(String direction, DbDataObject dboHolding, SvReader svr)
			throws SvException {
		DbSearchCriterion dbSearch = new DbSearchCriterion(Tc.VILLAGE_CODE, DbCompareOperand.EQUAL,
				dboHolding.getVal(Tc.VILLAGE_CODE));
		DbQueryObject query = new DbQueryObject(SvReader.getDbtByName(Tc.HOLDING), dbSearch, null, null);
		ArrayList<String> orderBy = new ArrayList<String>();
		orderBy.add(Tc.NAME);
		query.setOrderByFields(orderBy);

		if (log4j.isDebugEnabled())
			log4j.debug("Fetching holding book for village:" + dboHolding.getVal(Tc.VILLAGE_CODE));
		DbDataArray allHoldings = svr.getObjects(query, 0, 0);

		DbDataObject dboHoldingToReturn = null;
		try {
			if (!allHoldings.getItems().isEmpty()) {
				for (int i = 0; i < allHoldings.size(); i++) {
					DbDataObject tempHolding = allHoldings.get(i);
					if (tempHolding.getObject_id().equals(dboHolding.getObject_id())) {
						log4j.trace(tempHolding.getVal(Tc.PIC).toString());
						if (direction.equals(Tc.FORWARD)) {
							if (i == allHoldings.size() - 1) {
								dboHoldingToReturn = allHoldings.get(0);
							} else {
								dboHoldingToReturn = allHoldings.get(i + 1);
							}
							break;
						}
						if (direction.equals(Tc.BACKWARD)) {
							if (i == 0) {
								dboHoldingToReturn = allHoldings.get(allHoldings.size() - 1);
							} else {
								dboHoldingToReturn = allHoldings.get(i - 1);
							}
							break;
						}
					}
				}
			}
		} catch (Exception ex) {
			dboHoldingToReturn = dboHolding;
		}
		return dboHoldingToReturn;
	}

	/**
	 * Method that returns next or previous PIC of the holding
	 * 
	 * @param currHoldingObjId object_id of the HOLDING
	 * 
	 * @param direction        next holding or previous holding (FORWARD, BACKWARD)
	 * 
	 * @param svr              SvReader instance
	 * 
	 * @throws SvException
	 * @return String
	 */
	public String getNextOrPreviousHoldingPic(Long currHoldingObjId, String direction, SvReader svr)
			throws SvException {
		String nextHoldingPic = Tc.EMPTY_STRING;
		if (currHoldingObjId != null) {
			DbDataObject dboHolding = svr.getObjectById(currHoldingObjId, SvReader.getTypeIdByName(Tc.HOLDING), null);
			DbDataObject holdingRequired = getNextOrPreviousHolding(direction, dboHolding, svr);
			if (holdingRequired != null && holdingRequired.getVal(Tc.PIC) != null) {
				nextHoldingPic = holdingRequired.getVal(Tc.PIC).toString();
			}
		}
		return nextHoldingPic;
	}

	/**
	 * Method that returns number of animals per holding. This method categorize and
	 * count animals by their type
	 * 
	 * @param tableName    name of the table (FLOCK or ANIMAL)
	 * 
	 * @param animalType   animal class when animal, animal type when flock
	 * 
	 * @param holdingObjId OBJECT_ID of the holding
	 * 
	 * @param svr          SvReader instance
	 * 
	 * @throws SvException
	 * @return DbDataArray
	 */
	public String getAnimalNumberPerHolding(String tableName, String animalType, Long holdingObjId, SvReader svr) {
		String result = "0";

		switch (animalType) {
		case "1":
			// when FLOCK, 1 means OVINE
			DbDataArray cattle_cnt = getAnimalsCount(tableName, "1", holdingObjId, Tc.VALID, svr);
			DbDataArray buffolo_cnt = getAnimalsCount(tableName, "2", holdingObjId, Tc.VALID, svr);
			result = String.valueOf(cattle_cnt.size() + buffolo_cnt.size());
			if (tableName.equals(Tc.FLOCK)) {
				DbDataArray cnt_flock_ovine = getAnimalsCount(tableName, animalType, holdingObjId, Tc.VALID, svr);
				result = cnt_flock_ovine.size() + ",  #" + getTotalNoOfHeads(cnt_flock_ovine);
			}
			break;
		case "2":
			// when FLOCK, 2 means CAPRINE
			DbDataArray sheep_cnt = getAnimalsCount(tableName, "9", holdingObjId, Tc.VALID, svr);
			DbDataArray goat_cnt = getAnimalsCount(tableName, "10", holdingObjId, Tc.VALID, svr);
			result = String.valueOf(sheep_cnt.size() + goat_cnt.size());
			if (tableName.equals(Tc.FLOCK)) {
				DbDataArray cnt_flock_caprine = getAnimalsCount(tableName, animalType, holdingObjId, Tc.VALID, svr);
				result = cnt_flock_caprine.size() + ",  #" + getTotalNoOfHeads(cnt_flock_caprine);
			}
			break;
		case "3":
			// when FLOCK, 3 means PORCINE
			DbDataArray porcine_cnt = getAnimalsCount(tableName, "11", holdingObjId, Tc.VALID, svr);
			result = String.valueOf(porcine_cnt.getItems().size());
			if (tableName.equals(Tc.FLOCK)) {
				DbDataArray cnt_flock_porcine = getAnimalsCount(tableName, animalType, holdingObjId, Tc.VALID, svr);
				result = cnt_flock_porcine.size() + ",  #" + getTotalNoOfHeads(cnt_flock_porcine);
			}
			break;
		default:
			break;
		}
		return result;
	}

	/**
	 * Method that returns DbDataObject of type HOLDING per appropriate pic
	 * 
	 * @param srchPic  PIC (Premise identification code) of the holding
	 * 
	 * @param SvReader SvCore instance
	 * 
	 * @throws SvException
	 * @return DbDataObject
	 */
	public DbDataObject findAppropriateHoldingByPic(String srchPic, SvReader svr) throws SvException {
		DbDataObject result = null;
		DbSearchCriterion cr1 = new DbSearchCriterion(Tc.PIC, DbCompareOperand.EQUAL, srchPic);
		DbDataArray dbArray = svr.getObjects(new DbSearchExpression().addDbSearchItem(cr1),
				SvReader.getTypeIdByName(Tc.HOLDING), null, 0, 0);
		if (!dbArray.getItems().isEmpty()) {
			result = dbArray.getItems().get(0);
			// log4j.trace("Holding with srchPic: " + srchPic + " already
			// exists.");
		}
		return result;
	}

	/**
	 * Method that returns DbDataObject of type AREA per appropriate areaCode
	 * 
	 * @param areaCode Code of the area
	 *                 (village_code/commun_code/munic_code/region_code)
	 * 
	 * @param areaType Type of the area
	 * 
	 * @param SvReader SvCore instance
	 * 
	 * @throws SvException
	 * @return DbDataObject
	 */
	public DbDataObject findAppropriateAreaByCode(String areaCode, String areaType, SvReader svr) throws SvException {
		DbDataObject result = null;
		DbSearchCriterion cr1 = new DbSearchCriterion(Tc.AREA_NAME, DbCompareOperand.EQUAL, areaCode);
		DbSearchCriterion cr2 = new DbSearchCriterion(Tc.AREA_TYPE, DbCompareOperand.EQUAL, areaType);
		DbDataArray dbArray = svr.getObjects(new DbSearchExpression().addDbSearchItem(cr1).addDbSearchItem(cr2),
				SvReader.getTypeIdByName(Tc.AREA), null, 0, 0);
		if (!dbArray.getItems().isEmpty()) {
			result = dbArray.getItems().get(0);
		}
		return result;
	}

	public String findAppropriateHealthStatusForArea(DbDataObject areaObj, SvReader svr) throws SvException {
		String holdingAreaStatus = "FREE";
		if (areaObj != null) {
			DbDataArray areaHealthStatuses = svr.getObjectsByParentId(areaObj.getObject_id(),
					SvReader.getTypeIdByName(Tc.AREA_HEALTH), null, 0, 0);
			if (!areaHealthStatuses.getItems().isEmpty()) {
				for (DbDataObject tempAreaHealthStatus : areaHealthStatuses.getItems()) {
					String tempAreaStatus = tempAreaHealthStatus.getVal(Tc.AREA_STATUS).toString();
					if (tempAreaStatus.equals("0") || tempAreaStatus.equals("1")) {
						holdingAreaStatus = tempAreaStatus;
						break;
					}
				}
			}
			switch (holdingAreaStatus) {
			case "0":
				holdingAreaStatus = Tc.HIGH_RISK;
				break;
			case "1":
				holdingAreaStatus = Tc.LOW_RISK;
				break;
			default:
				break;
			}
		}
		return holdingAreaStatus;
	}

	/**
	 * Method for fetching all subAreas by coreArea (returns all subAreas that
	 * area_code starts like the coreArea area_code)
	 * 
	 * @param dboArea AREA object
	 * @param svr     SvReader instance
	 * @return DbDataArray
	 * @throws SvException
	 */
	public DbDataArray getAreasByCoreArea(DbDataObject dboArea, SvReader svr) throws SvException {
		String areaName = Tc.EMPTY_STRING;
		DbDataArray result = null;
		if (dboArea.getVal(Tc.AREA_NAME) != null && dboArea.getVal(Tc.AREA_TYPE) != null) {
			areaName = dboArea.getVal(Tc.AREA_NAME).toString();
			DbSearchCriterion cr1 = new DbSearchCriterion(Tc.AREA_NAME, DbCompareOperand.LIKE,
					areaName + Tc.PERCENT_OPERATOR);
			DbSearchCriterion cr2 = new DbSearchCriterion(Tc.AREA_TYPE, DbCompareOperand.NOTEQUAL,
					dboArea.getVal(Tc.AREA_TYPE).toString());
			result = svr.getObjects(new DbSearchExpression().addDbSearchItem(cr1).addDbSearchItem(cr2),
					dboArea.getObject_type(), null, 0, 0);
		}
		return result;
	}

	/**
	 * Method that returns auto-generated areas by parent area
	 * 
	 * @param supAreaHealthObj AREA_HEALTH object
	 * @param dboAreaParent    AREA object
	 * @param autoGenerated    Is AREA_HEALTH auto-generated
	 * @param svr
	 * @return
	 * @throws SvException
	 */
	public DbDataArray getAutoGeneratedAreaHealhObjects(DbDataObject supAreaHealthObj, DbDataObject dboAreaParent,
			Boolean autoGenerated, SvReader svr) throws SvException {
		DbDataArray result = null;
		DbSearchCriterion cr1 = new DbSearchCriterion(Tc.DISEASE_ID, DbCompareOperand.EQUAL,
				supAreaHealthObj.getVal(Tc.DISEASE_ID).toString());
		DbSearchCriterion cr2 = new DbSearchCriterion(Tc.AREA_STATUS, DbCompareOperand.EQUAL,
				supAreaHealthObj.getVal(Tc.AREA_STATUS).toString());
		DbSearchCriterion cr3 = new DbSearchCriterion(Tc.AUTO_GENERATED, DbCompareOperand.EQUAL, autoGenerated);
		DbSearchCriterion cr4 = new DbSearchCriterion(Tc.PARENT_ID, DbCompareOperand.EQUAL,
				dboAreaParent.getObject_id());
		result = svr.getObjects(new DbSearchExpression().addDbSearchItem(cr1).addDbSearchItem(cr2).addDbSearchItem(cr3)
				.addDbSearchItem(cr4), SvReader.getTypeIdByName(Tc.AREA_HEALTH), null, 0, 0);
		return result;
	}

	/**
	 * Method that returns list of pending movements per holding
	 * 
	 * @param destinationHoldingObjId PIC of the destination holding
	 * 
	 * @param svc                     SvCore instance
	 * 
	 * @throws SvException
	 * @return DbDataArray with all animal movements found
	 */
	public DbDataArray getNumOfIncomingAnimalsOrFlock(String destinationHoldingObjId, String objectType, SvReader svr)
			throws SvException {
		String linkType = Tc.FLOCK_MOVEMENT_HOLDING;
		String movementType = Tc.FLOCK_MOVEMENT;
		if (objectType.equals(Tc.ANIMAL)) {
			linkType = Tc.ANIMAL_MOVEMENT_HOLDING;
			movementType = Tc.ANIMAL_MOVEMENT;
		}
		DbDataArray validMovements = new DbDataArray();
		DbDataObject holdingObj = findAppropriateHoldingByPic(destinationHoldingObjId, svr);
		if (holdingObj != null) {
			DbDataObject linkAnimalMovementHolding = SvReader.getLinkType(linkType, holdingObj.getObject_type(),
					SvReader.getTypeIdByName(movementType));
			DbDataArray allMovements = svr.getObjectsByLinkedId(holdingObj.getObject_id(), holdingObj.getObject_type(),
					linkAnimalMovementHolding, SvReader.getTypeIdByName(movementType), false, null, 0, 0);
			if (allMovements != null && !allMovements.getItems().isEmpty()) {
				for (DbDataObject temp : allMovements.getItems()) {
					if (temp.getStatus().equals(Tc.VALID)) {
						validMovements.addDataItem(temp);
					}
				}
			}
		}
		return validMovements;
	}

	public DbDataArray getPendingAnimalMovementsPerHolding(String destinationHoldingObjId, SvReader svr)
			throws SvException {
		return getNumOfIncomingAnimalsOrFlock(destinationHoldingObjId, Tc.ANIMAL, svr);
	}

	public void setSummaryInformationPerStrayPet(LinkedHashMap<String, String> jsonOrderedMap, DbDataObject dboStrayPet,
			SvReader svr) throws SvException {
		jsonOrderedMap.put("naits.stray_pet.petId",
				((dboStrayPet.getVal(Tc.PET_TAG_ID) != null) ? dboStrayPet.getVal(Tc.PET_TAG_ID).toString() : "N/A"));
		jsonOrderedMap.put("naits.stray_pet.petCharacteristics",
				((dboStrayPet.getVal(Tc.PET_CHARACTERISTICS) != null)
						? dboStrayPet.getVal(Tc.PET_CHARACTERISTICS).toString()
						: "N/A"));
		DbDataObject dboAnimalShelter = svr.getObjectById(dboStrayPet.getParent_id(),
				SvReader.getTypeIdByName(Tc.HOLDING), null);
		DbDataArray arrCaretakers = getStrayPetCaretaker(dboStrayPet, svr);
		if (!arrCaretakers.getItems().isEmpty()) {
			DbDataObject dboCareTaker = arrCaretakers.get(0);
			jsonOrderedMap.put("naits.stray_pet.caretaker",
					((dboCareTaker.getVal(Tc.NAT_REG_NUMBER) != null)
							? dboCareTaker.getVal(Tc.NAT_REG_NUMBER).toString()
							: "N/A"));
		} else {
			jsonOrderedMap.put("naits.stray_pet.caretaker", "N/A");
		}
		if (dboAnimalShelter != null) {
			jsonOrderedMap.put("naits.holding.pic",
					((dboAnimalShelter.getVal(Tc.PIC) != null) ? dboAnimalShelter.getVal(Tc.PIC).toString() : "N/A"));
		}
	}

	public void generatePopulationSummaryInfo(LinkedHashMap<String, String> jsonOrderedMap, Long objectId, SvReader svr)
			throws SvException {
		DbDataObject dboPopulation = svr.getObjectById(objectId, SvReader.getTypeIdByName(Tc.POPULATION), null);
		jsonOrderedMap.put("naits.population.populationId",
				((dboPopulation.getVal(Tc.POPULATION_ID) != null) ? dboPopulation.getVal(Tc.POPULATION_ID).toString()
						: "N/A"));

		jsonOrderedMap.put("naits.population.populationName",
				((dboPopulation.getVal(Tc.POPULATION_NAME) != null)
						? dboPopulation.getVal(Tc.POPULATION_NAME).toString()
						: "N/A"));

		Integer numRegions = getDistinctCountOfGeolocationsAccordingGeostatCode(dboPopulation, Tc.REGION_CODE, svr);
		Integer numMunics = getDistinctCountOfGeolocationsAccordingGeostatCode(dboPopulation, Tc.MUNIC_CODE, svr);
		Integer numCommuns = getDistinctCountOfGeolocationsAccordingGeostatCode(dboPopulation, Tc.COMMUN_CODE, svr);
		Integer numVillages = getDistinctCountOfGeolocationsAccordingGeostatCode(dboPopulation, Tc.VILLAGE_CODE, svr);

		if (numRegions > 0) {
			jsonOrderedMap.put("naits.population.num_regions", numRegions.toString());
			jsonOrderedMap.put("naits.population.num_munics", numMunics.toString());
			jsonOrderedMap.put("naits.population.num_communs", numCommuns.toString());
			jsonOrderedMap.put("naits.population.num_villages", numVillages.toString());
		}
		String numHoldings = getPopulationHoldingNumParamValue(dboPopulation, svr);
		if (numHoldings != null) {
			jsonOrderedMap.put("naits.population.num_holdings", numHoldings);
		}
		String numAnimals = getPopulationAnimalNumParamValue(dboPopulation, svr);
		if (numAnimals != null) {
			jsonOrderedMap.put("naits.population.num_animals", numAnimals);
		}
	}

	public Integer getDistinctCountOfGeolocationsAccordingGeostatCode(DbDataObject dboPopulation, String geoType,
			SvReader svr) throws SvException {
		Integer result = 0;
		HashSet<String> hs = new HashSet<>();
		DbDataArray arrPopulationLocations = new DbDataArray();
		arrPopulationLocations = svr.getObjectsByParentId(dboPopulation.getObject_id(),
				SvReader.getTypeIdByName(Tc.POPULATION_LOCATION), null, 0, 0);
		if (arrPopulationLocations.getItems().isEmpty()) {
			arrPopulationLocations = svr.getObjectsByParentId(dboPopulation.getObject_id(),
					SvReader.getTypeIdByName(Tc.POPULATION_LOCATION), new DateTime(), 0, 0);
		}
		if (arrPopulationLocations.getItems().isEmpty()) {
			result = -1;
		} else {
			for (DbDataObject dboPopulationLocation : arrPopulationLocations.getItems()) {
				String geostatCode = dboPopulationLocation.getVal(Tc.GEOSTAT_CODE).toString();
				switch (geoType) {
				case Tc.REGION_CODE:
					hs.add(geostatCode.substring(0, 2));
					break;
				case Tc.MUNIC_CODE:
					hs.add(geostatCode.substring(0, 4));
					break;
				case Tc.COMMUN_CODE:
					hs.add(geostatCode.substring(0, 6));
					break;
				case Tc.VILLAGE_CODE:
					hs.add(geostatCode.substring(0, 8));
					break;
				default:
					break;
				}
			}
			result = hs.size();
		}
		return result;
	}

	/**
	 * Method that returns ArrayList of random Geostat codes depend on
	 * Stratification filter. For example, if you have added Stratification filter
	 * with 10 out of 15 regions it will return 10 random selected regions. But, if
	 * you have added 50 villages, that belong to that region, the randomizer will
	 * return 50 or less villages (depends on region/munic/commun)
	 * 
	 * @param dboStratFilter
	 * @param svr
	 * @return
	 * @throws SvException
	 */
	public ArrayList<String> getRandomGeoUnitsByStratificationFilter(DbDataObject dboStratFilter, SvReader svr)
			throws SvException {
		ArrayList<String> result = new ArrayList<>();
		HashSet<String> hsRegions = new HashSet<>();
		HashSet<String> hsMunics = new HashSet<>();
		HashSet<String> hsCommuns = new HashSet<>();
		DbDataArray arrPopulationLocations = new DbDataArray();
		arrPopulationLocations = svr.getObjectsByParentId(dboStratFilter.getParent_id(),
				SvReader.getTypeIdByName(Tc.POPULATION_LOCATION), null, 0, 0);
		if (arrPopulationLocations.getItems().isEmpty()) {
			arrPopulationLocations = svr.getObjectsByParentId(dboStratFilter.getObject_id(),
					SvReader.getTypeIdByName(Tc.POPULATION_LOCATION), new DateTime(), 0, 0);
		}
		if (!arrPopulationLocations.getItems().isEmpty()) {
			DbDataArray shuffledPopulationLocations = getNRandomElementsFromDbDataArray(arrPopulationLocations,
					arrPopulationLocations.size());
			Integer numRegion = Integer.valueOf(dboStratFilter.getVal(Tc.NUM_REGIONS).toString());
			Integer numMunic = Integer.valueOf(dboStratFilter.getVal(Tc.NUM_MUNICS).toString());
			Integer numCommun = Integer.valueOf(dboStratFilter.getVal(Tc.NUM_COMMUNS).toString());
			Integer numVillage = Integer.valueOf(dboStratFilter.getVal(Tc.NUM_VILLAGES).toString());
			int counter = 0;
			for (DbDataObject dboPopulationLocation : shuffledPopulationLocations.getItems()) {
				String geostatCode = dboPopulationLocation.getVal(Tc.GEOSTAT_CODE).toString();
				if (hsRegions.add(geostatCode.substring(0, 2))) {
					counter++;
					if (numRegion.equals(counter)) {
						break;
					}
				}
			}
			counter = 0;
			for (DbDataObject dboPopulationLocation : shuffledPopulationLocations.getItems()) {
				String geostatCode = dboPopulationLocation.getVal(Tc.GEOSTAT_CODE).toString();
				for (String region : hsRegions) {
					if (geostatCode.startsWith(region) && hsMunics.add(geostatCode.substring(0, 4))) {
						counter++;
						if (numMunic.equals(counter)) {
							break;
						}
					}
				}
				if (numMunic.equals(counter)) {
					break;
				}
			}
			counter = 0;
			for (DbDataObject dboPopulationLocation : shuffledPopulationLocations.getItems()) {
				String geostatCode = dboPopulationLocation.getVal(Tc.GEOSTAT_CODE).toString();
				for (String munic : hsMunics) {
					if (geostatCode.startsWith(munic) && hsCommuns.add(geostatCode.substring(0, 6))) {
						counter++;
						if (numCommun.equals(counter)) {
							break;
						}
					}
				}
				if (numCommun.equals(counter)) {
					break;
				}
			}
			counter = 0;
			for (DbDataObject dboPopulationLocation : shuffledPopulationLocations.getItems()) {
				String geostatCode = dboPopulationLocation.getVal(Tc.GEOSTAT_CODE).toString();
				for (String commun : hsCommuns) {
					if (geostatCode.startsWith(commun)) {
						result.add(geostatCode);
						counter++;
						if (numVillage.equals(counter)) {
							break;
						}
					}
				}
				if (numVillage.equals(counter)) {
					break;
				}
			}
		}
		return result;
	}

	public boolean hasPermission(String tableName, SvReader svr) throws SvException {
		UserManager um = new UserManager();
		boolean result = false;
		ArrayList<String> currentCustomPermissions = um.getCustomPermissionForUserOrGroup(svr.getInstanceUser(), svr);
		if (!currentCustomPermissions.isEmpty())
			if (currentCustomPermissions.contains(tableName + ".READ")
					|| currentCustomPermissions.contains(tableName + ".WRITE")
					|| currentCustomPermissions.contains(tableName + ".FULL")) {
				result = true;
			}
		return result;
	}

	public boolean hasPermission(String tableName, String permissionType, SvReader svr) throws SvException {
		UserManager um = new UserManager();
		boolean result = false;
		ArrayList<String> currentCustomPermissions = um.getCustomPermissionForUserOrGroup(svr.getInstanceUser(), svr);
		if (!currentCustomPermissions.isEmpty()) {
			for (String permission : currentCustomPermissions) {
				if (permission.equals(tableName + "." + permissionType)) {
					result = true;
					break;
				}
			}
		}
		return result;
	}

	/**
	 * Method that returns holding summary info
	 * 
	 * @param holdingObj DbDataObject of the holding
	 * @param svr        SvReader instance
	 * @return LinkedHashMap<String, String>
	 * @throws SvException
	 */
	public LinkedHashMap<String, String> generateHoldingSummaryInformation(DbDataObject holdingObj, SvReader svr)
			throws SvException {
		String holdingType = Tc.EMPTY_STRING;
		// current permissions for full holding summary
		boolean hasHoldResponsiblePerm = hasPermission(Tc.HOLDING_RESPONSIBLE, svr);
		boolean hasAnimalPerm = hasPermission(Tc.ANIMAL, svr);
		boolean hasAnimalMovementPerm = hasPermission(Tc.ANIMAL_MOVEMENT, svr);
		boolean hasFlockPerm = hasPermission(Tc.FLOCK, svr);
		boolean hasFlockMovementPerm = hasPermission(Tc.FLOCK_MOVEMENT, svr);
		boolean hasLabSamplePerm = hasPermission(Tc.LAB_SAMPLE, svr);
		boolean hasLabTestResultPerm = hasPermission(Tc.LAB_TEST_RESULT, svr);

		if (holdingObj.getVal(Tc.TYPE) != null) {
			holdingType = holdingObj.getVal(Tc.TYPE).toString();
		}
		LinkedHashMap<String, String> jsonOrderedMap = new LinkedHashMap<String, String>();
		if (hasHoldResponsiblePerm) {
			jsonOrderedMap.put("naits.keeper.full_name", getHoldingKeeperInfo(holdingObj.getObject_id(), svr));
		}
		switch (holdingType) {
		case Tc.ANIMAL_SHELTER_TYPE:
			PetServices pet_srvc = new PetServices();
			jsonOrderedMap.put("naits.pets_total_number",
					String.valueOf(getAnimalsCount(Tc.PET, "", holdingObj.getObject_id(), Tc.VALID, svr).size()));
			jsonOrderedMap.put("naits.active_quarantines",
					String.valueOf(pet_srvc.countQuarantinesPerHolding(holdingObj.getObject_id(), true, false, svr)));
			jsonOrderedMap.put("naits.expired_quarantines",
					String.valueOf(pet_srvc.countQuarantinesPerHolding(holdingObj.getObject_id(), false, true, svr)));
			break;
		case Tc.VET_CLINIC_TYPE:
			jsonOrderedMap.put("naits.pets_total_number",
					String.valueOf(getAnimalsCount(Tc.PET, "", holdingObj.getObject_id(), Tc.VALID, svr).size()));
		case Tc.VET_STATION_TYPE:
			break;
		case Tc.SLAUGHTERHOUSE_TYPE:
			jsonOrderedMap.put("naits.shelf_life_hours",
					holdingObj.getVal(Tc.SHELF_LIFE) != null ? holdingObj.getVal(Tc.SHELF_LIFE).toString() : "N/A");
			break;
		default:
			if (hasAnimalPerm && hasLabSamplePerm && hasLabTestResultPerm) {
				jsonOrderedMap.put("naits.holding.health_status", getHoldingHealthStatus(holdingObj, svr));
			}
			if (hasHoldResponsiblePerm) {
				jsonOrderedMap.put("naits.holding.herder_activity",
						getHearderActivityPeriod(holdingObj.getObject_id(), svr));
			}
			if (hasAnimalPerm) {
				jsonOrderedMap.put("naits.animals.cnt.bovine",
						getAnimalNumberPerHolding(Tc.ANIMAL, "1", holdingObj.getObject_id(), svr));
				jsonOrderedMap.put("naits.animals.cnt.ovine",
						getAnimalNumberPerHolding(Tc.ANIMAL, "2", holdingObj.getObject_id(), svr));
				jsonOrderedMap.put("naits.animals.cnt.caprine",
						getAnimalNumberPerHolding(Tc.ANIMAL, "3", holdingObj.getObject_id(), svr));
			}
			if (hasFlockPerm) {
				jsonOrderedMap.put("naits.flock.cnt.ovine",
						getAnimalNumberPerHolding(Tc.FLOCK, "1", holdingObj.getObject_id(), svr));
				jsonOrderedMap.put("naits.flock.cnt.caprine",
						getAnimalNumberPerHolding(Tc.FLOCK, "2", holdingObj.getObject_id(), svr));
				jsonOrderedMap.put("naits.flock.cnt.porcine",
						getAnimalNumberPerHolding(Tc.FLOCK, "3", holdingObj.getObject_id(), svr));
			}
			if (hasAnimalPerm && hasAnimalMovementPerm) {
				jsonOrderedMap.put("naits.holding.pendingMovements",
						String.valueOf(getNoOfPendingMovements(holdingObj.getVal(Tc.PIC).toString(), svr)));
				jsonOrderedMap.put("naits.holding.outgoingAnimalMovements",
						String.valueOf(getNumOfOutgoingAnimalsOrFlock(holdingObj.getObject_id(), Tc.ANIMAL, svr)));
			}
			if (hasFlockPerm && hasFlockMovementPerm) {
				jsonOrderedMap.put("naits.holding.pendingMovementsFlock",
						String.valueOf(getNoOfPendingMovements(holdingObj.getVal(Tc.PIC).toString(), Tc.FLOCK, svr)));
				jsonOrderedMap.put("naits.holding.outgoingFlockMovements",
						String.valueOf(getNumOfOutgoingAnimalsOrFlock(holdingObj.getObject_id(), Tc.FLOCK, svr)));
			}
			break;
		}
		return jsonOrderedMap;
	}

	public Integer getMaxNumberOfAnimalsOrHoldings(DbDataObject dboStratFilter, SvReader svr) throws SvException {
		int result = 0;
		DbDataObject dboPopulation = svr.getObjectById(dboStratFilter.getParent_id(),
				SvReader.getTypeIdByName(Tc.POPULATION), null);
		if (dboPopulation != null && dboPopulation.getVal(Tc.EXTRACTION_TYPE) != null) {
			if (dboPopulation.getVal(Tc.EXTRACTION_TYPE).toString().equals(Tc.ANIMAL)
					&& dboStratFilter.getVal(Tc.NUM_ANIMALS) != null) {
				result = Integer.valueOf(dboStratFilter.getVal(Tc.NUM_ANIMALS).toString());
			} else if (dboPopulation.getVal(Tc.EXTRACTION_TYPE).toString().equals(Tc.HOLDING)
					&& dboStratFilter.getVal(Tc.NUM_HOLDINGS) != null) {
				result = Integer.valueOf(dboStratFilter.getVal(Tc.NUM_HOLDINGS).toString());
			}
		}
		return result;
	}

	public String getPassportRequestsNumDependOnStatus(Long objectId, String status, SvReader svr) throws SvException {
		int counter = 0;
		String result = "0";
		DbSearchCriterion cr1 = new DbSearchCriterion(Tc.HOLDING_OBJ_ID + "::NUMERIC", DbCompareOperand.EQUAL,
				objectId);
		DbDataArray arrResult = svr.getObjects(new DbSearchExpression().addDbSearchItem(cr1),
				SvReader.getTypeIdByName(Tc.PASSPORT_REQUEST), null, 0, 0);
		if (!arrResult.getItems().isEmpty()) {
			for (DbDataObject dbo : arrResult.getItems()) {
				if (dbo.getStatus().equals(status))
					counter++;
			}
			result = String.valueOf(counter);
		}
		return result;
	}

	/**
	 * Method that returns area summary info
	 * 
	 * @param areaObj DbDataObject of the area
	 * @param svr     SvReader instance
	 * @return LinkedHashMap<String, String>
	 * @throws SvException
	 */
	public LinkedHashMap<String, String> generateAreaSummaryInformation(DbDataObject areaObj, SvReader svr)
			throws SvException {
		LinkedHashMap<String, String> jsonOrderedMap = new LinkedHashMap<String, String>();
		if (areaObj != null && areaObj.getVal(Tc.AREA_NAME) != null) {
			jsonOrderedMap.put("naits.area.number_of_holdings",
					String.valueOf(findHoldingsPerArea(areaObj.getVal(Tc.AREA_NAME).toString(), svr).size()));
			jsonOrderedMap.put("naits.area.number_of_holdings_in_active_quarantine", String.valueOf(
					findNumberOfHoldingsInActiveQuarantinePerArea(areaObj.getVal(Tc.AREA_NAME).toString(), svr)));
			jsonOrderedMap.put("naits.area.registered_no_of_health_statuses_per_disease",
					String.valueOf(findHealthStatusesPerArea(areaObj, svr).size()));
		}
		return jsonOrderedMap;
	}

	/**
	 * Method that returns number of pending movements per holding
	 * 
	 * @param holdingPic PIC of the holding
	 * @param svr        SvReader instance
	 * @return Integer
	 * @throws SvException
	 */
	public Integer getNoOfPendingMovements(String holdingPic, SvReader svr) throws SvException {
		return getNoOfPendingMovements(holdingPic, Tc.ANIMAL, svr);
	}

	public Integer getNoOfPendingMovements(String holdingPic, String objectType, SvReader svr) throws SvException {
		int result = 0;
		DbDataArray validMovements = null;
		if (objectType.equals(Tc.ANIMAL)) {
			validMovements = getPendingAnimalMovementsPerHolding(holdingPic, svr);
		} else {
			validMovements = getNumOfIncomingAnimalsOrFlock(holdingPic, Tc.FLOCK, svr);
		}
		if (validMovements != null)
			result = validMovements.size();
		return result;
	}

	/**
	 * Method that checks if svlink exists between Holding and Holding Responsible
	 * exists
	 * 
	 * @param dboHolding DbDataObject of the holding
	 * 
	 * @param linkName   name of the
	 *                   link(HOLDING_ASSOCIATED/HOLDING_KEEPER/HOLDING_HERDER)
	 * 
	 * @param svr        SvReader instance
	 * 
	 * @throws SvException
	 * @return Boolean
	 */
	public Boolean checkIfSomeLinkExistsBetweenHoldingAndPerson(DbDataObject dboHolding, String linkName, SvReader svr)
			throws SvException {
		Boolean result = false;
		DbDataObject linkHoldingPerson = SvLink.getLinkType(linkName, SvReader.getTypeIdByName(Tc.HOLDING),
				SvReader.getTypeIdByName(Tc.HOLDING_RESPONSIBLE));
		DbDataArray allItems = svr.getObjectsByLinkedId(dboHolding.getObject_id(), SvReader.getTypeIdByName(Tc.HOLDING),
				linkHoldingPerson, SvReader.getTypeIdByName(Tc.HOLDING_RESPONSIBLE), false, null, 0, 0);
		if (!allItems.getItems().isEmpty())
			result = true;

		return result;
	}

	/**
	 * Method that checks if svlink between Holding and Quarantine exists
	 * 
	 * @param dboHolding DbDataObject of the quarantine
	 * 
	 * @param linkName   name of the
	 *                   link(HOLDING_ASSOCIATED/HOLDING_KEEPER/HOLDING_HERDER)
	 * 
	 * @param svr        SvReader instance
	 * 
	 * @throws SvException
	 * @return Boolean
	 */
	public Boolean checkIfSomeLinkExistsBetweenHoldingAndQuarantine(DbDataObject dboQuarantine, SvReader svr)
			throws SvException {
		Boolean result = false;
		DbDataObject linkHoldingPerson = SvLink.getLinkType(Tc.HOLDING_QUARANTINE, SvReader.getTypeIdByName(Tc.HOLDING),
				SvReader.getTypeIdByName(Tc.QUARANTINE));
		DbDataArray allItems = svr.getObjectsByLinkedId(dboQuarantine.getObject_id(),
				SvReader.getTypeIdByName(Tc.QUARANTINE), linkHoldingPerson, SvReader.getTypeIdByName(Tc.HOLDING), true,
				null, 0, 0);
		if (!allItems.getItems().isEmpty())
			result = true;

		return result;
	}

	/**
	 * Method that checks if svlink between Holding and Quarantine exists
	 * 
	 * @param dboHolding DbDataObject of the quarantine
	 * 
	 * @param linkName   name of the
	 *                   link(HOLDING_ASSOCIATED/HOLDING_KEEPER/HOLDING_HERDER)
	 * 
	 * @param svr        SvReader instance
	 * 
	 * @throws SvException
	 * @return Boolean
	 */
	public DbDataObject getHoldingLinkedPerExportQuaranitne(DbDataObject dboQuarantine, SvReader svr)
			throws SvException {
		DbDataObject result = null;
		DbDataObject linkHoldingPerson = SvLink.getLinkType(Tc.HOLDING_QUARANTINE, SvReader.getTypeIdByName(Tc.HOLDING),
				SvReader.getTypeIdByName(Tc.QUARANTINE));
		DbDataArray allItems = svr.getObjectsByLinkedId(dboQuarantine.getObject_id(),
				SvReader.getTypeIdByName(Tc.QUARANTINE), linkHoldingPerson, SvReader.getTypeIdByName(Tc.HOLDING), true,
				null, 0, 0);
		if (!allItems.getItems().isEmpty())
			result = allItems.get(0);

		return result;
	}

	// MOVEMENT_DOC

	DbDataArray getAnimalMovementsByMovementDoc(DbDataObject movementDocObj, Boolean useCache, SvReader svr)
			throws SvException {
		DbDataArray resultArr = new DbDataArray();
		String movementType = Tc.EMPTY_STRING;
		if (movementDocObj.getVal(Tc.MOVEMENT_TYPE) != null) {
			movementType = Tc.ANIMAL_MOVEMENT;
			if (movementDocObj.getVal(Tc.MOVEMENT_TYPE).toString().equals(Tc.FLOCK)) {
				movementType = Tc.FLOCK_MOVEMENT;
			}
		}
		if (!movementType.equals("") && movementDocObj.getVal(Tc.MOVEMENT_DOC_ID) != null) {
			DbSearchCriterion cr1 = new DbSearchCriterion(Tc.MOVEMENT_DOC_ID, DbCompareOperand.EQUAL,
					movementDocObj.getVal(Tc.MOVEMENT_DOC_ID).toString());
			DbSearchExpression dbse = new DbSearchExpression();
			dbse.addDbSearchItem(cr1);
			if (useCache.booleanValue()) {
				resultArr = svr.getObjects(dbse, SvReader.getTypeIdByName(movementType), null, 0, 0);
			} else {
				resultArr = svr.getObjects(dbse, SvReader.getTypeIdByName(movementType), new DateTime(), 0, 0);
			}
		}
		return resultArr;
	}

	/**
	 * Method that returns animal movements by Movement Document ID
	 * 
	 * @param movementDocId
	 * @param svr
	 * @return
	 * @throws SvException
	 */
	DbDataArray getAnimalMovementsByMovementDocId(String movementDocId, SvReader svr) throws SvException {
		DbDataArray mvmDocsArr = null;
		DbDataArray resultArr = null;
		DbDataObject mvmDocObj = null;
		DbSearchCriterion cr1 = new DbSearchCriterion(Tc.MOVEMENT_DOC_ID, DbCompareOperand.EQUAL, movementDocId);
		DbSearchExpression dbse = new DbSearchExpression();
		dbse.addDbSearchItem(cr1);
		mvmDocsArr = svr.getObjects(dbse, SvReader.getTypeIdByName(Tc.MOVEMENT_DOC), null, 0, 0);
		if (!mvmDocsArr.getItems().isEmpty()) {
			mvmDocObj = mvmDocsArr.get(0);
			if (mvmDocObj != null) {
				resultArr = getAnimalMovementsByMovementDoc(mvmDocObj, false, svr);
			}
		}
		return resultArr;
	}

	/**
	 * Method that gets HOLDING objects by Unit ID
	 * 
	 * @param svr    SvReader instance
	 * @param unitId Unit ID
	 * @return DbDataArray
	 */
	public DbDataArray getHoldingsByUnitId(SvReader svr, String unitId) {
		DbDataArray result = new DbDataArray();
		if (unitId != null && svr != null) {
			try {
				DbSearchExpression dbse = new DbSearchExpression();
				DbSearchCriterion sc1 = new DbSearchCriterion(Tc.VILLAGE_CODE, DbCompareOperand.EQUAL, unitId);
				DbSearchCriterion sc2 = new DbSearchCriterion("GEOM", DbCompareOperand.ISNULL);

				dbse.addDbSearchItem(sc1).addDbSearchItem(sc2);

				result = svr.getObjects(dbse, SvCore.getTypeIdByName(Tc.HOLDING), null, 0, 0);
			} catch (Exception e) {
				log4j.error("Failed fetching holdings by village_code: " + e);
			}
		}
		return result;
	}

	/**
	 * Method that gets linked HOLDING for certain EXPORT QUARANTINE
	 * 
	 * @param dboQuarantine QUARANTINE object
	 * @param svr           SvReader instance
	 * @return
	 * @throws SvException
	 */
	public DbDataObject getLinkBetweenHoldingAndQuarantine(DbDataObject dboQuarantine, SvReader svr)
			throws SvException {
		DbDataObject dboLinkedHolding = null;
		DbDataObject linkHoldingAndQuarantine = SvLink.getLinkType(Tc.HOLDING_QUARANTINE,
				SvReader.getTypeIdByName(Tc.HOLDING), SvReader.getTypeIdByName(Tc.QUARANTINE));
		DbDataArray allItems = svr.getObjectsByLinkedId(dboQuarantine.getObject_id(),
				SvReader.getTypeIdByName(Tc.QUARANTINE), linkHoldingAndQuarantine, SvReader.getTypeIdByName(Tc.HOLDING),
				true, null, 0, 0);
		if (!allItems.getItems().isEmpty()) {
			dboLinkedHolding = allItems.get(0);
		}
		return dboLinkedHolding;
	}

	/**
	 * Method that return Holding object by Object_Id
	 * 
	 * @param objectId
	 * @param svr
	 * @return
	 * @throws SvException
	 */
	public DbDataObject getHoldingObjectByObjectId(Long objectId, SvReader svr) throws SvException {
		DbDataObject dboHolidng = svr.getObjectById(objectId, SvReader.getTypeIdByName(Tc.HOLDING), null);
		return dboHolidng;
	}

	/**
	 * Method that calculates holding health depend on Laboratory samples and
	 * Movement document blocks.
	 * 
	 * @param holding_Obj_Id Object_Id of Holding
	 * @param svr            SvReader instance
	 * @return (String) Holding health precentage
	 * @throws SvException
	 */
	public String calculateHoldingHealth(Long holdingObjId, SvReader svr) throws SvException {
		String result = "100%";
		int countUnhealthyAnimals = 0;
		Integer totalAnimals = 0;
		try {
			DbDataObject dboHolding = svr.getObjectById(holdingObjId, SvReader.getTypeIdByName(Tc.HOLDING), null);
			if (dboHolding != null) {
				DbDataArray arrAnimals = svr.getObjectsByParentId(holdingObjId, SvReader.getTypeIdByName(Tc.ANIMAL),
						null, 0, 0);
				if (arrAnimals != null && !arrAnimals.getItems().isEmpty()) {
					totalAnimals = arrAnimals.size();
					for (DbDataObject dboAnimal : arrAnimals.getItems()) {
						Boolean hasPositiveOrInconclusiveLabSample = false;
						DbDataArray arrLabSamples = getLabSamplesPerAnimal(dboAnimal, svr);
						if (arrLabSamples != null && !arrLabSamples.getItems().isEmpty()) {
							for (DbDataObject dboLabSample : arrLabSamples.getItems()) {
								if (dboLabSample.getStatus().equals(Tc.PROCESSED)) {
									DbDataArray arrLabTestResults = getLabTestResultsPerLabSample(dboLabSample, svr);
									if (arrLabTestResults != null && !arrLabTestResults.getItems().isEmpty()) {
										for (DbDataObject dboLabTestResult : arrLabTestResults.getItems()) {
											if (dboLabTestResult.getVal(Tc.TEST_RESULT) != null) {
												if (dboLabTestResult.getVal(Tc.TEST_RESULT).toString().equals("0")) {
													hasPositiveOrInconclusiveLabSample = true;
													countUnhealthyAnimals++;
												} else if (dboLabTestResult.getVal(Tc.TEST_RESULT).toString()
														.equals("2")) {
													if (!checkifInconclusiveSamplesAreTestedAgainPerAnimal(dboAnimal,
															svr)) {
														hasPositiveOrInconclusiveLabSample = true;
														countUnhealthyAnimals++;
													}
												}
											}
										}
									}
								}
							}
						}
						if (!hasPositiveOrInconclusiveLabSample) {
							DbDataObject dboMovementDocBlock = searchForObject(
									SvReader.getTypeIdByName(Tc.MOVEMENT_DOC_BLOCK), Tc.ANIMAL_FLOCK_ID,
									dboAnimal.getVal(Tc.ANIMAL_ID).toString(), svr);
							if (dboMovementDocBlock != null && !dboMovementDocBlock.getStatus().equals(Tc.RESOLVED)) {
								countUnhealthyAnimals++;
							}
						}
					}
					if (countUnhealthyAnimals != 0) {
						Integer precentageHealthy = 100 - calculatePrecentage(countUnhealthyAnimals, totalAnimals);
						result = precentageHealthy + Tc.PERCENT_OPERATOR;
					}
				}
			}
		} catch (SvException e) {
			result = "100%";
			log4j.error("Errror occurred while calculating Holding health status" + e);
		}
		return result;
	}

	/**
	 * Method that gets HOLDING for EXPORT_CERTIFICATE
	 * 
	 * @param dboQuarantine QUARANTINE object
	 * @param svr           SvReader instance
	 * @return
	 * @throws SvException
	 */
	public DbDataObject getHoldingPerExportCertificate(DbDataObject dboExportCertificate, SvReader svr)
			throws SvException {
		DbDataObject dboHolding = null;
		if (dboExportCertificate == null || dboExportCertificate.getParent_id().equals(0L)) {
			return dboHolding;
		}
		DbDataObject dboExportQuarantine = svr.getObjectById(dboExportCertificate.getParent_id(),
				SvReader.getTypeIdByName(Tc.QUARANTINE), null);
		if (dboExportQuarantine != null) {
			dboHolding = getLinkBetweenHoldingAndQuarantine(dboExportQuarantine, svr);
		}
		return dboHolding;
	}

	// HOLDING RESPONSIBLE METHODS

	/**
	 * Method that returns link object between HOLDING and HOLDING_RESPONSIBLE
	 * 
	 * @param dboHoldingResponsible DbDataObject of the holding responsible
	 * 
	 * @param linkName              name of the link
	 * 
	 * @param parentCore            SvCore instance
	 * 
	 * @throws SvException
	 * @return DbDataObject of type SVAROG_LINK_TYPE
	 */
	public DbDataArray getLinkedHoldingsByHoldingResponsibleAndLinkName(DbDataObject dboHoldingResponsible,
			String linkName, SvCore parentCore) throws SvException {
		DbDataArray arrHoldings = null;
		SvReader svr = null;
		try {
			svr = new SvReader(parentCore);
			DbDataObject dboLinkBetweenHoldingAndHoldingResponsible = SvLink.getLinkType(linkName,
					SvReader.getTypeIdByName(Tc.HOLDING), SvReader.getTypeIdByName(Tc.HOLDING_RESPONSIBLE));
			DbDataArray allItems = svr.getObjectsByLinkedId(dboHoldingResponsible.getObject_id(),
					SvReader.getTypeIdByName(Tc.HOLDING_RESPONSIBLE), dboLinkBetweenHoldingAndHoldingResponsible,
					SvReader.getTypeIdByName(Tc.HOLDING), true, null, 0, 0);
			if (allItems != null && !allItems.getItems().isEmpty()) {
				DbSearchExpression getLinkToInvalidate = new DbSearchExpression();
				DbSearchCriterion critKeeperObjectId = new DbSearchCriterion(Tc.LINK_OBJ_ID_2, DbCompareOperand.EQUAL,
						dboHoldingResponsible.getObject_id());
				DbSearchCriterion critLinkTypeObjectId = new DbSearchCriterion(Tc.LINK_TYPE_ID, DbCompareOperand.EQUAL,
						dboLinkBetweenHoldingAndHoldingResponsible.getObject_id());
				getLinkToInvalidate.addDbSearchItem(critKeeperObjectId);
				getLinkToInvalidate.addDbSearchItem(critLinkTypeObjectId);
				arrHoldings = svr.getObjects(getLinkToInvalidate, svCONST.OBJECT_TYPE_LINK, null, 0, 0);
			}
		} catch (SvException e) {
			log4j.error("Error occurred in findLinkBetweenKeeperAndHolding" + e);
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
		return arrHoldings;
	}

	/**
	 * Method that returns full name of the keeper
	 * 
	 * @param holdingId object_id of the holding
	 * 
	 * @param svr       SvReader instance
	 * 
	 * @throws SvException
	 * @return String
	 */
	public String getHoldingKeeperInfo(Long holdingId, Boolean useCache, SvReader svr) throws SvException {
		String result = Tc.NOT_AVAILABLE_NA;
		DbDataArray allItems = null;
		DbDataObject dboHolding = svr.getObjectById(holdingId, SvReader.getTypeIdByName(Tc.HOLDING), null);
		if (dboHolding != null) {
			DbDataObject linkHoldingPerson = SvLink.getLinkType(Tc.HOLDING_KEEPER, SvReader.getTypeIdByName(Tc.HOLDING),
					SvReader.getTypeIdByName(Tc.HOLDING_RESPONSIBLE));
			if (useCache) {
				allItems = svr.getObjectsByLinkedId(dboHolding.getObject_id(), SvReader.getTypeIdByName(Tc.HOLDING),
						linkHoldingPerson, SvReader.getTypeIdByName(Tc.HOLDING_RESPONSIBLE), false, null, 0, 0);
			} else {
				allItems = svr.getObjectsByLinkedId(dboHolding.getObject_id(), SvReader.getTypeIdByName(Tc.HOLDING),
						linkHoldingPerson, SvReader.getTypeIdByName(Tc.HOLDING_RESPONSIBLE), false, new DateTime(), 0,
						0);
			}
			if (!allItems.getItems().isEmpty()) {
				DbDataObject res = allItems.get(0);
				if (res.getVal(Tc.FULL_NAME) != null) {
					result = res.getVal(Tc.FULL_NAME).toString();
				}
			}
		}
		return result;
	}

	/**
	 * Method for searching Person (HOLDING_RESPONSIBLE) according private number
	 * 
	 * @param privateNumber Private Number / NAT_REG_NUMBER
	 * @param svr
	 * @return
	 */
	public DbDataObject getDboPersonByPrivateNumber(String privateNumber, SvReader svr) {
		return searchForObject(SvReader.getTypeIdByName(Tc.HOLDING_RESPONSIBLE), Tc.NAT_REG_NUMBER, privateNumber, svr);
	}

	public String getHoldingKeeperInfo(Long holdingId, SvReader svr) throws SvException {
		return getHoldingKeeperInfo(holdingId, false, svr);
	}

	/**
	 * Method that returns activity period per Herder into string
	 * 
	 * @param holdingId Object_Id of the holding
	 * @param svr       SvReader instance
	 * @return String of all Herders in given Holding with their activity period
	 * @throws SvException
	 */
	public String getHearderActivityPeriod(Long holdingId, SvReader svr) throws SvException {
		String result = Tc.NOT_AVAILABLE_NA;
		String herderActivityInfo;
		DbDataObject dboHolding = svr.getObjectById(holdingId, SvReader.getTypeIdByName(Tc.HOLDING), null);
		if (dboHolding != null) {
			DbDataObject linkHoldingPerson = SvLink.getLinkType(Tc.HOLDING_HERDER, SvReader.getTypeIdByName(Tc.HOLDING),
					SvReader.getTypeIdByName(Tc.HOLDING_RESPONSIBLE));
			DbDataArray allItems = svr.getObjectsByLinkedId(dboHolding.getObject_id(),
					SvReader.getTypeIdByName(Tc.HOLDING), linkHoldingPerson,
					SvReader.getTypeIdByName(Tc.HOLDING_RESPONSIBLE), false, null, 0, 0);
			if (!allItems.getItems().isEmpty()) {
				int counter = 0;
				result = Tc.EMPTY_STRING;
				for (DbDataObject herderObj : allItems.getItems()) {
					counter++;
					DbDataObject dboLink = getLinkObject(holdingId, herderObj.getObject_id(),
							linkHoldingPerson.getObject_id(), svr);
					String activityPeriod = getActivityPeriodPerHered(dboLink, svr);
					if (activityPeriod.equals(" ")) {
						activityPeriod = Tc.NOT_AVAILABLE_NA;
					}
					herderActivityInfo = herderObj.getVal(Tc.FULL_NAME) != null
							? herderObj.getVal(Tc.FULL_NAME).toString() + " - " + activityPeriod
							: "N/A";
					if (counter < allItems.size()) {
						herderActivityInfo += "; ";
					}
					result += herderActivityInfo;
				}
			}
		}
		return result;
	}

	/**
	 * Method that checks if holding person exists
	 * 
	 * @param dboHolding DbDataObject of the holding
	 * 
	 * @param linkName   name of the link
	 * 
	 * @param svr        SvReader instance
	 * 
	 * @throws SvException
	 * @return DbDataObject of type HOLDING_RESPOSIBLE
	 */
	public DbDataObject getHoldingPersonRelatedToHolding(DbDataObject dboHolding, String linkName, SvReader svr)
			throws SvException {
		DbDataObject result = null;
		DbDataObject linkHoldingPerson = SvLink.getLinkType(linkName, SvReader.getTypeIdByName(Tc.HOLDING),
				SvReader.getTypeIdByName(Tc.HOLDING_RESPONSIBLE));
		DbDataArray allItems = svr.getObjectsByLinkedId(dboHolding.getObject_id(), SvReader.getTypeIdByName(Tc.HOLDING),
				linkHoldingPerson, SvReader.getTypeIdByName(Tc.HOLDING_RESPONSIBLE), false, null, 0, 0);
		if (allItems != null && !allItems.getItems().isEmpty())
			result = allItems.get(0);
		return result;
	}

	/**
	 * Method that returns holding owner (HOLDING_OWNER) by link.
	 * 
	 * @param holdingObjId object_id of the HOLDING object
	 * @param svr          SvReader instance
	 * @return
	 * @throws SvException
	 */
	public DbDataObject getHoldingOwner(Long holdingObjId, SvReader svr) throws SvException {
		DbDataObject holding = svr.getObjectById(holdingObjId, SvReader.getTypeIdByName(Tc.HOLDING), null);
		DbDataObject keeper = null;
		if (holding != null) {
			DbDataObject holdingKeeper = SvReader.getLinkType(Tc.HOLDING_KEEPER, SvReader.getTypeIdByName(Tc.HOLDING),
					SvReader.getTypeIdByName(Tc.HOLDING_RESPONSIBLE));
			DbDataArray allItems = svr.getObjectsByLinkedId(holding.getObject_id(),
					SvReader.getTypeIdByName(Tc.HOLDING), holdingKeeper,
					SvReader.getTypeIdByName(Tc.HOLDING_RESPONSIBLE), false, null, 0, 0);
			if (allItems != null && !allItems.getItems().isEmpty()) {
				keeper = allItems.get(0);
			}
		}
		return keeper;
	}

	/**
	 * Method that return pet owner by link.
	 * 
	 * @param petObjId
	 * @param svr
	 * @return
	 * @throws SvException
	 */
	public DbDataObject getPetOwner(Long petObjId, SvReader svr) throws SvException {
		DbDataObject dboPet = svr.getObjectById(petObjId, SvReader.getTypeIdByName(Tc.PET), null);
		DbDataObject dboOwner = null;
		if (dboPet != null) {
			DbDataObject petOwner = SvReader.getLinkType(Tc.PET_OWNER, SvReader.getTypeIdByName(Tc.PET),
					SvReader.getTypeIdByName(Tc.HOLDING_RESPONSIBLE));
			DbDataArray allItems = svr.getObjectsByLinkedId(dboPet.getObject_id(), SvReader.getTypeIdByName(Tc.PET),
					petOwner, SvReader.getTypeIdByName(Tc.HOLDING_RESPONSIBLE), false, null, 0, 0);
			if (allItems != null && !allItems.getItems().isEmpty()) {
				dboOwner = allItems.get(0);
			}
		}
		return dboOwner;
	}

	public String getActivityPeriodPerHered(DbDataObject dboLink, SvReader svr) {
		String result = Tc.EMPTY_STRING;
		SvParameter svp = null;
		String dateFrom = null;
		String dateTo = null;
		try {
			svp = new SvParameter(svr);
			dateFrom = svp.getParamDateTime(dboLink, "param.activity_from") != null
					? svp.getParamDateTime(dboLink, "param.activity_from").toString().substring(0, 10)
					: "";
			dateTo = svp.getParamDateTime(dboLink, "param.activity_to") != null
					? svp.getParamDateTime(dboLink, "param.activity_to").toString().substring(0, 10)
					: "";
			result = "[" + dateFrom.replace("-", "/") + " - " + dateTo.replace("-", "/") + "]";
		} catch (SvException e) {
			log4j.error(e);
		} finally {
			if (svp != null)
				svp.release();
		}
		return result;
	}

	/**
	 * Method that returns holding responsible/s depend on link name. Currently, the
	 * options are HOLDING_ASSOCIATED, HOLDING_HERDER, HOLDING KEEPER
	 * 
	 * @param dboHolding Holding object
	 * @param linkName   Name of the link
	 * @param svr        SvReader instance
	 * @return
	 * @throws SvException
	 */
	public DbDataArray getResponsiblePersonRelatedToHoldingDependOnLinkName(DbDataObject dboHolding, String linkName,
			SvReader svr) throws SvException {
		DbDataArray arrResponsible = null;
		DbDataObject dboLinkHoldingAndResponsiblePerson = SvReader.getLinkType(linkName,
				SvReader.getTypeIdByName(Tc.HOLDING), SvReader.getTypeIdByName(Tc.HOLDING_RESPONSIBLE));
		if (dboLinkHoldingAndResponsiblePerson != null) {
			arrResponsible = svr.getObjectsByLinkedId(dboHolding.getObject_id(), dboHolding.getObject_type(),
					dboLinkHoldingAndResponsiblePerson, SvReader.getTypeIdByName(Tc.HOLDING_RESPONSIBLE), false, null,
					0, 0);
		}
		return arrResponsible;
	}

	// VACCINATION METHODS

	/**
	 * Method for fetching municipalities that were targeted by certain Vaccination
	 * Event / Camapign
	 */
	public ArrayList<String> getTargetedMunicipalitiesByVaccinationEvent(DbDataObject dboVaccinationEvent, SvReader svr)
			throws SvException {
		Connection conn = null;
		String schema = SvConf.getDefaultSchema();
		Long vaccinationObjectId = dboVaccinationEvent.getObject_id();
		String petOrAnimalScopeCampaignSubquery = "SELECT VP.PARENT_ID FROM " + schema + ".VPET VP " + "JOIN " + schema
				+ ".VPET_HEALTH_BOOK PHB ON PHB.PARENT_ID=VP.OBJECT_ID " + "WHERE VP.DT_DELETE > NOW()  "
				+ "AND PHB.DT_DELETE > NOW()  " + "AND PHB.CAMPAIGN_NAME IS NOT NULL  " + "AND PHB.CAMPAIGN_NAME = ";
		if (dboVaccinationEvent.getVal(Tc.CAMPAIGN_SCOPE) != null
				&& dboVaccinationEvent.getVal(Tc.CAMPAIGN_SCOPE).toString().equals(Tc.ANIMAL)) {
			petOrAnimalScopeCampaignSubquery = "SELECT VA." + Tc.PARENT_ID + " FROM " + schema + ".VANIMAL VA JOIN "
					+ schema + ".VSVAROG_LINK VSL ON (VSL.LINK_OBJ_ID_1=VA.OBJECT_ID AND"
					+ " VSL.LINK_TYPE_ID = (SELECT " + Tc.OBJECT_ID + " FROM " + schema
					+ ".VSVAROG_LINK_TYPE WHERE DT_DELETE > NOW() AND " + Tc.LINK_TYPE + "=" + "'" + Tc.ANIMAL_VACC_BOOK
					+ "'" + ")" + ")" + " JOIN " + schema + ".VVACCINATION_BOOK VVB ON VVB." + Tc.OBJECT_ID + "="
					+ "VSL.LINK_OBJ_ID_2 " + "WHERE VVB.DT_DELETE > NOW() "
					+ "AND VA.DT_DELETE > NOW() AND VSL.DT_DELETE > NOW() AND VVB.CAMPAIGN_NAME IS NOT NULL "
					+ "AND VVB.CAMPAIGN_NAME = ";
		}
		ArrayList<String> municipalities = new ArrayList<>();

		String selectQuery = "SELECT DISTINCT VH.MUNIC_CODE FROM " + schema
				+ ".VHOLDING VH WHERE VH.DT_DELETE > NOW() AND" + " VH." + Tc.OBJECT_ID + " IN ("
				+ petOrAnimalScopeCampaignSubquery + "(SELECT " + "VVE." + Tc.CAMPAIGN_NAME + " FROM " + schema
				+ ".VVACCINATION_EVENT VVE WHERE VVE.DT_DELETE > NOW() AND VVE." + Tc.OBJECT_ID + " = ?)" + ")";
		ResultSet rs = null;
		conn = svr.dbGetConn();
		try (PreparedStatement selectStatement = conn.prepareStatement(selectQuery.toUpperCase(),
				ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);) {
			selectStatement.setLong(1, vaccinationObjectId);
			rs = selectStatement.executeQuery();
			while (rs.next()) {
				municipalities.add(rs.getString(Tc.MUNIC_CODE));
			}
		} catch (Exception e) {
			throw (new SvException("naits.error.getingSelectedResult", svr.getInstanceUser(), e));
		} finally {
			try {
				if (rs != null)
					rs.close();
				if (conn != null)
					conn.close();
			} catch (SQLException e) {
				log4j.error(new SvException("naits.error.getingSelectedResult", svr.getInstanceUser(), e));
				// throw (new SvException("naits.error.getingSelectedResult",
				// svr.getInstanceUser(), e));
			}
		}
		return municipalities;
	}

	/**
	 * Method that counts overlapping transfer
	 * 
	 * @param tagType
	 * @param startTag
	 * @param endTag
	 * @param parentId
	 * @param svr
	 * @return
	 * @throws SvException
	 */
	public int getOverlappingTransferArrayListCustomSQL(String tagType, Long startTag, Long endTag, Long parentId,
			SvReader svr) throws SvException {
		int total = 0;
		Connection conn = svr.dbGetConn();
		String selectCountOverlappingTransfer = "SELECT COUNT(1) AS TOTAL FROM NAITS.VTRANSFER as tbl0 WHERE ( (tbl0.DT_DELETE > now()) AND ( (tbl0.TAG_TYPE = ?) "
				+ "AND (tbl0.PARENT_ID = ?) AND (tbl0.STATUS != 'RELEASED') AND "
				+ "( (tbl0.START_TAG_ID <= ?) OR (tbl0.START_TAG_ID <= ?)) AND "
				+ "( (tbl0.END_TAG_ID >= ?) OR (tbl0.END_TAG_ID >= ?)))) LIMIT 1 OFFSET 0";
		ResultSet rs = null;

		try (PreparedStatement selectStatement = conn.prepareStatement(selectCountOverlappingTransfer,
				ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);) {
			selectStatement.setString(1, tagType);
			selectStatement.setLong(2, parentId);
			selectStatement.setLong(3, startTag);
			selectStatement.setLong(4, endTag);
			selectStatement.setLong(5, startTag);
			selectStatement.setLong(6, endTag);

			rs = selectStatement.executeQuery();
			while (rs.next()) {
				total = rs.getInt("TOTAL");
			}
		} catch (Exception e) {
			throw (new SvException("naits.error.getingSelectedResult", svr.getInstanceUser(), e));
		} finally {
			try {
				if (rs != null)
					rs.close();
				if (conn != null)
					conn.close();
			} catch (SQLException e) {
				log4j.error(new SvException("naits.error.getingSelectedResult", svr.getInstanceUser(), e));
			}
		}
		return total;
	}

	public DbDataArray getCampaignTargetedMunicipalityUnits(DbDataObject dboVaccinationEvent, SvReader svr)
			throws SvException {
		DbDataArray result = new DbDataArray();
		ArrayList<String> municipalitiesTargeted = getTargetedMunicipalitiesByVaccinationEvent(dboVaccinationEvent,
				svr);
		for (String munics : municipalitiesTargeted) {
			DbDataArray arrOrganisationalUnits = searchForObjectWithSingleFilter(svCONST.OBJECT_TYPE_ORG_UNITS,
					Tc.EXTERNAL_ID, Long.valueOf(munics), svr);
			if (!arrOrganisationalUnits.getItems().isEmpty()) {
				result.addDataItem(arrOrganisationalUnits.get(0));
			}
		}
		return result;
	}

	/**
	 * Temporary method for getting vaccine code values according disease. This need
	 * to be replaced appropriately after we get the full list of diseases for
	 * vaccination book
	 * 
	 * @param diseaseCode
	 * @param svr
	 * @return
	 */
	public String getVaccinationCodeInVaccinationBookAccordingDiseaseCode(String diseaseCode) {
		String result = "'100'";
		switch (diseaseCode) {
		case "1":
			result = "'FMD-1', 'FMD-2', 'FMD-RE', 'FMD-FORCE'";
			break;
		case "2":
			result = "'ANTHR-GOV', 'ANTHR-RE', 'ANTHR-FORCE'";
			break;
		case "3":
			result = "'BRUC-GOV', 'BRUC-RE', 'BRUC-FORCE'";
			break;
		case "5":
			result = "'RABIES-GOV', 'RABIES-RE', 'RABIES-FORCE'";
			break;
		default:
			break;
		}
		return result;
	}

	/**
	 * Method that returns valid vaccination events between date of start and date
	 * of end
	 * 
	 * @param svr
	 * @return
	 */
	public DbDataArray getValidVaccEvents(SvReader svr, String holdingType) throws SvException {
		DbDataArray result = new DbDataArray();
		Reader rdr = null;
		ArrayList<String> animalTypes = null;
		try {
			rdr = new Reader();
			DbSearchExpression dbse0 = new DbSearchExpression();
			DbSearchCriterion cr1 = new DbSearchCriterion(Tc.EVENT_START, DbCompareOperand.LESS_EQUAL, new DateTime());
			DbSearchCriterion cr2 = new DbSearchCriterion(Tc.EVENT_END, DbCompareOperand.GREATER_EQUAL, new DateTime());
			dbse0.addDbSearchItem(cr1).addDbSearchItem(cr2);
			DbDataArray arrValidVaccinations = svr.getObjects(dbse0, SvReader.getTypeIdByName(Tc.VACCINATION_EVENT),
					null, 0, 0);
			if (!arrValidVaccinations.getItems().isEmpty()) {
				for (DbDataObject tempVaccEvent : arrValidVaccinations.getItems()) {
					animalTypes = new ArrayList<>();
					String temp = campaignActivityNameInCustomFormat(tempVaccEvent, svr);
					tempVaccEvent.setVal(Tc.NOTE, temp);
					String localeId = svr.getUserLocaleId(svr.getInstanceUser());
					StringBuilder sb = new StringBuilder();
					DbDataArray fields = svr.getObjectsByParentId(tempVaccEvent.getObject_type(),
							svCONST.OBJECT_TYPE_FIELD, null, 0, 0);
					for (DbDataObject dboField : fields.getItems()) {
						String fieldName = dboField.getVal(Tc.FIELD_NAME) != null
								? dboField.getVal(Tc.FIELD_NAME).toString()
								: "";
						if (fieldName.equals(Tc.ANIMAL_TYPE) && dboField.getVal(Tc.CODE_LIST_MNEMONIC) != null
								&& tempVaccEvent.getVal(fieldName) != null) {
							List<String> tempFieldValues = rdr.getMultiSelectFieldValueAsList(tempVaccEvent, fieldName);
							for (String codeValue : tempFieldValues) {
								animalTypes.add(codeValue);
								String decodedValue = rdr.decodeCodeValue(tempVaccEvent.getObject_type(), fieldName,
										codeValue, localeId, svr);
								sb.append(decodedValue);
								if (!codeValue.equals(tempFieldValues.get(tempFieldValues.size() - 1))) {
									sb.append(", ");
								}
							}
							tempVaccEvent.setVal(fieldName, sb.toString());
						}
					}
					if (!animalTypes.isEmpty()) {
						if (Tc.ANIMAL_SHELTER_TYPE.equals(holdingType) || Tc.VET_CLINIC_TYPE.equals(holdingType)) {
							for (String scopeType : animalTypes) {
								if (scopeType.equalsIgnoreCase("401") || scopeType.equalsIgnoreCase("402")
										|| scopeType.equalsIgnoreCase("403")) {
									result.addDataItem(tempVaccEvent);
									break;
								}
							}
						} else {
							for (String scopeType : animalTypes) {
								if (scopeType.equalsIgnoreCase("1") || scopeType.equalsIgnoreCase("2")
										|| scopeType.equalsIgnoreCase("9") || scopeType.equalsIgnoreCase("10")
										|| scopeType.equalsIgnoreCase("11") || scopeType.equalsIgnoreCase("12")
										|| scopeType.equalsIgnoreCase("400")) {
									result.addDataItem(tempVaccEvent);
									break;
								}
							}
						}
					}
				}
			}
		} catch (Exception e) {
			log4j.error("Error occurred in getValidVaccEvents" + e);
		}
		return result;
	}

	/**
	 * Method that returns expired Vaccination events
	 * 
	 * @param svr
	 * @return
	 */
	public DbDataArray getExpiredVaccinationEvents(SvReader svr) throws SvException {
		DbDataArray result = new DbDataArray();
		Reader rdr = null;
		ArrayList<String> animalTypes = null;
		try {
			rdr = new Reader();
			DbSearchExpression dbse0 = new DbSearchExpression();
			DbSearchCriterion cr1 = new DbSearchCriterion(Tc.EVENT_END, DbCompareOperand.LESS_EQUAL, new DateTime());
			DbSearchCriterion cr2 = new DbSearchCriterion(Tc.ANIMAL_TYPE, DbCompareOperand.NOTEQUAL, Tc.PET);
			dbse0.addDbSearchItem(cr1).addDbSearchItem(cr2);
			DbDataArray arrValidVaccinations = svr.getObjects(dbse0, SvReader.getTypeIdByName(Tc.VACCINATION_EVENT),
					null, 0, 0);
			if (!arrValidVaccinations.getItems().isEmpty()) {
				for (DbDataObject tempVaccEvent : arrValidVaccinations.getItems()) {
					animalTypes = new ArrayList<>();
					String temp = campaignActivityNameInCustomFormat(tempVaccEvent, svr);
					tempVaccEvent.setVal(Tc.NOTE, temp);
					String localeId = svr.getUserLocaleId(svr.getInstanceUser());
					StringBuilder sb = new StringBuilder();
					DbDataArray fields = svr.getObjectsByParentId(tempVaccEvent.getObject_type(),
							svCONST.OBJECT_TYPE_FIELD, null, 0, 0);
					for (DbDataObject dboField : fields.getItems()) {
						String fieldName = dboField.getVal(Tc.FIELD_NAME) != null
								? dboField.getVal(Tc.FIELD_NAME).toString()
								: "";
						if (fieldName.equals(Tc.ANIMAL_TYPE) && dboField.getVal(Tc.CODE_LIST_MNEMONIC) != null
								&& tempVaccEvent.getVal(fieldName) != null) {
							List<String> tempFieldValues = rdr.getMultiSelectFieldValueAsList(tempVaccEvent, fieldName);
							for (String codeValue : tempFieldValues) {
								animalTypes.add(codeValue);
								String decodedValue = rdr.decodeCodeValue(tempVaccEvent.getObject_type(), fieldName,
										codeValue, localeId, svr);
								sb.append(decodedValue);
								if (!codeValue.equals(tempFieldValues.get(tempFieldValues.size() - 1))) {
									sb.append(", ");
								}
							}
							tempVaccEvent.setVal(fieldName, sb.toString());
						}
					}
					for (String scopeType : animalTypes) {
						if (scopeType.equalsIgnoreCase("1") || scopeType.equalsIgnoreCase("2")
								|| scopeType.equalsIgnoreCase("9") || scopeType.equalsIgnoreCase("10")
								|| scopeType.equalsIgnoreCase("11") || scopeType.equalsIgnoreCase("12")
								|| scopeType.equalsIgnoreCase("400")) {
							result.addDataItem(tempVaccEvent);
							break;
						}
					}
				}
			}
		} catch (Exception e) {
			log4j.error("Error occurred in getExpiredVaccinationEvents" + e.getMessage());
		}
		return result;
	}

	public String campaignActivityNameInCustomFormat(DbDataObject dboVaccEvent, SvReader svr) throws SvException {
		StringBuilder activityName = new StringBuilder();
		String localeId = svr.getUserLocaleId(svr.getInstanceUser());
		activityName.append(dboVaccEvent.getVal(Tc.CAMPAIGN_NAME).toString()).append("/").append(decodeCodeValue(
				dboVaccEvent.getObject_type(), Tc.ACTIVITY_TYPE, dboVaccEvent.getVal(Tc.ACTIVITY_TYPE).toString(),
				localeId, svr)).append(
						(dboVaccEvent.getVal(Tc.CAMPAIGN_SCOPE) != null
								? "/" + decodeCodeValue(dboVaccEvent.getObject_type(), Tc.CAMPAIGN_SCOPE,
										dboVaccEvent.getVal(Tc.CAMPAIGN_SCOPE).toString(), localeId, svr)
								: ""))
				.append((dboVaccEvent.getVal(Tc.DISEASE) != null ? "/" + decodeCodeValue(dboVaccEvent.getObject_type(),
						Tc.DISEASE, dboVaccEvent.getVal(Tc.DISEASE).toString(), localeId, svr) : ""));
		if (dboVaccEvent.getVal(Tc.ANIMAL_TYPE) != null) {
			String[] charArr = dboVaccEvent.getVal(Tc.ANIMAL_TYPE).toString().split(",");
			activityName.append("/");
			for (int i = 0; i < charArr.length; i++) {
				activityName.append(
						decodeCodeValue(dboVaccEvent.getObject_type(), Tc.ANIMAL_TYPE, charArr[i], localeId, svr));
				if (i != charArr.length - 1)
					activityName.append(",");
			}
		}
		return activityName.toString();
	}

	/**
	 * Method that returns array of valid test types
	 * 
	 * @param objectId
	 * @param testType Test type
	 * @param svr      SvReader instance
	 * @return
	 * @throws SvException
	 */
	public DbDataArray getValidTestTypes(Long objectId, String testType, SvReader svr) throws SvException {
		return getValidTestTypes(objectId, testType, false, svr);
	}

	public DbDataArray getValidTestTypes(Long objectId, String testType, Boolean useCache, SvReader svr)
			throws SvException {
		DbDataArray result = null;
		DbDataObject testResultObj = null;
		testResultObj = svr.getObjectById(objectId, SvReader.getTypeIdByName(Tc.LAB_TEST_RESULT), null);
		if (!useCache) {
			testResultObj = svr.getObjectById(objectId, SvReader.getTypeIdByName(Tc.LAB_TEST_RESULT), new DateTime());
		}
		if (testResultObj != null && testType != null && !testType.equals("")
				&& testResultObj.getVal(Tc.SAMPLE_DISEASE) != null) {
			DbDataObject labSample = svr.getObjectById(testResultObj.getParent_id(),
					SvReader.getTypeIdByName(Tc.LAB_SAMPLE), null);
			if (labSample != null && labSample.getVal(Tc.SAMPLE_TYPE) != null) {
				LinkedHashMap<String, String> criterias = new LinkedHashMap<>();
				criterias.put(Tc.DISEASE, testResultObj.getVal(Tc.SAMPLE_DISEASE).toString());
				criterias.put(Tc.TEST_TYPE, testType);
				criterias.put(Tc.SAMPLE_TYPE, labSample.getVal(Tc.SAMPLE_TYPE).toString());
				result = getDbDataWithCriteria(criterias, SvReader.getTypeIdByName(Tc.LAB_TEST_TYPE), svr);
			}
		}
		return result;
	}

	/**
	 * Method that returns data in grid type Json format.
	 * 
	 * @param dbArray
	 * 
	 * @param tableName Name of the table
	 * @return
	 * @throws SvException
	 */
	public String convertDbDataArrayToGridJson(DbDataArray dbArray, String tableName, boolean skipRepoFields,
			SvReader svr) throws SvException {
		return convertDbDataArrayToGridJson(dbArray, tableName, skipRepoFields, null, null, svr);
	}

	/**
	 * Method that returns data in grid type Json format.
	 * 
	 * @param dbArray
	 * 
	 * @param tableName Name of the table
	 * @return
	 * @throws SvException
	 */
	public String convertDbDataArrayToGridJson(DbDataArray dbArray, String tableName, boolean skipRepoFields,
			String fieldNameToSortBy, String sortAscOrDesc, SvReader svr) throws SvException {
		String result = Tc.EMPTY_ARRAY_STRING;
		if (fieldNameToSortBy != null && !dbArray.getItems().isEmpty()) {
			dbArray.getSortedItems(fieldNameToSortBy, true);
		}
		org.json.simple.JSONArray resultJsonArray = null;
		if (sortAscOrDesc == null || sortAscOrDesc.equals("ASC")) {
			resultJsonArray = convertDbDataArrayToJSONArrayAscending(dbArray, tableName, skipRepoFields, svr);
		} else if (sortAscOrDesc != null && sortAscOrDesc.equals("DESC")) {
			resultJsonArray = convertDbDataArrayToJSONArrayDescending(dbArray, tableName, skipRepoFields, svr);
		}
		if (!resultJsonArray.isEmpty()) {
			result = resultJsonArray.toJSONString();
		}
		return result;
	}

	/**
	 * Method that returns data in grid type Json format.
	 * 
	 * @param dbArray
	 * 
	 * @param tableName Name of the table
	 * @return
	 * @throws SvException
	 */
	public String convertDbDataArrayToGridJsonWithoutSorting(DbDataArray dbArray, String tableName,
			boolean skipRepoFields, SvReader svr) throws SvException {
		String result = Tc.EMPTY_ARRAY_STRING;
		org.json.simple.JSONArray resultJsonArray = null;
		resultJsonArray = convertDbDataArrayToJSONArray(dbArray, tableName, false, svr);
		if (!resultJsonArray.isEmpty()) {
			result = resultJsonArray.toJSONString();
		}
		return result;
	}

	public LinkedHashMap<String, JsonElement> getDbDataObjectsAsLinkedHashMap(DbDataObject dbo, String tableName,
			boolean skipRepoFields, SvReader svr) throws SvException {
		DbDataObject dboField = null;
		String localeId = Tc.EMPTY_STRING;
		JsonObject convertedJObj = dbo.toJson().getAsJsonObject(dbo.getClass().getCanonicalName());
		LinkedHashMap<String, JsonElement> lhmObj = new LinkedHashMap<>();
		if (svr != null)
			localeId = svr.getUserLocaleId(svr.getInstanceUser());
		for (Entry<String, JsonElement> tempConverted : convertedJObj.entrySet()) {
			if (!tempConverted.getKey().equals("values")) {
				if (!skipRepoFields) {
					lhmObj.put(tableName + "." + tempConverted.getKey().toUpperCase(), tempConverted.getValue());
				}
			} else {
				JsonArray jsonArray = tempConverted.getValue().getAsJsonArray();
				for (JsonElement je : jsonArray) {
					for (Entry<String, JsonElement> value : je.getAsJsonObject().entrySet()) {
						// check if field has flag Tc.SV_ISLABEL:true
						if (svr != null) {
							dboField = SvReader.getFieldByName(tableName, value.getKey().toUpperCase());
							if (dboField != null && dboField.getVal(Tc.SV_ISLABEL) != null
									&& dboField.getVal(Tc.SV_ISLABEL).equals(true)) {
								lhmObj.put(tableName + "." + value.getKey().toUpperCase() + "_CODE", value.getValue());
								StringBuilder sb = new StringBuilder(
										"\"" + (I18n.getText(localeId, value.getValue().toString().replace("\"", "")))
												+ "\"");
								lhmObj.put(tableName + "." + value.getKey().toUpperCase(),
										new JsonParser().parse(sb.toString()));
							} else {
								lhmObj.put(tableName + "." + value.getKey().toUpperCase(), value.getValue());
							}
						} else {
							lhmObj.put(tableName + "." + value.getKey().toUpperCase(), value.getValue());
						}
					}
				}
			}
		}
		return lhmObj;
	}

	public DbDataArray revertDbDataArray(DbDataArray dbArray) throws SvException {
		DbDataArray revertedArr = new DbDataArray();
		for (int i = dbArray.size() - 1; i >= 0; i--) {
			revertedArr.addDataItem(dbArray.get(i));
		}
		return revertedArr;
	}

	public org.json.simple.JSONArray convertDbDataArrayToJSONArrayDescending(DbDataArray dbArray, String tableName,
			Boolean skipRepoFields, SvReader svr) throws SvException {
		org.json.simple.JSONArray resultJsonArray = new org.json.simple.JSONArray();
		for (int i = dbArray.size() - 1; i >= 0; i--) {
			DbDataObject dbo = dbArray.get(i);
			LinkedHashMap<String, JsonElement> lhmObj = getDbDataObjectsAsLinkedHashMap(dbo, tableName, skipRepoFields,
					svr);
			resultJsonArray.add(lhmObj);
		}
		return resultJsonArray;
	}

	public org.json.simple.JSONArray convertDbDataArrayToJSONArrayAscending(DbDataArray dbArray, String tableName,
			Boolean skipRepoFields, SvReader svr) throws SvException {
		org.json.simple.JSONArray resultJsonArray = new org.json.simple.JSONArray();
		for (DbDataObject dbo : dbArray.getItems()) {
			LinkedHashMap<String, JsonElement> lhmObj = getDbDataObjectsAsLinkedHashMap(dbo, tableName, skipRepoFields,
					svr);
			resultJsonArray.add(lhmObj);
		}
		return resultJsonArray;
	}

	public org.json.simple.JSONArray convertDbDataArrayToJSONArray(DbDataArray dbArray, String tableName,
			Boolean skipRepoFields, SvReader svr) throws SvException {
		return convertDbDataArrayToJSONArrayAscending(dbArray, tableName, skipRepoFields, svr);
	}

	public org.json.simple.JSONArray convertArrayListToJSONArray(ArrayList<String> inputArray) throws SvException {
		org.json.simple.JSONArray resultJsonArray = new org.json.simple.JSONArray();
		for (int i = 0; i < inputArray.size(); i++) {
			resultJsonArray.add(inputArray.get(i));
		}
		return resultJsonArray;
	}

	public String convertDbDataArrayToGridJsonDescending(DbDataArray dbArray, String tableName, boolean skipRepoFields,
			SvReader svr) throws SvException {
		String result = "[]";
		DbDataObject dboField = null;
		String localeId = Tc.EMPTY_STRING;
		if (svr != null)
			localeId = svr.getUserLocaleId(svr.getInstanceUser());
		org.json.simple.JSONArray resultJsonArray = new org.json.simple.JSONArray();
		for (int i = dbArray.size() - 1; i > 0; i--) {
			DbDataObject dbo = dbArray.get(i);
			JsonObject convertedJObj = dbo.toJson().getAsJsonObject(dbo.getClass().getCanonicalName());
			LinkedHashMap<String, JsonElement> lhmObj = new LinkedHashMap<>();
			for (Entry<String, JsonElement> tempConverted : convertedJObj.entrySet()) {
				if (!tempConverted.getKey().equals("values")) {
					if (!skipRepoFields) {
						lhmObj.put(tableName + "." + tempConverted.getKey().toUpperCase(), tempConverted.getValue());
					}
				} else {
					JsonArray jsonArray = tempConverted.getValue().getAsJsonArray();
					for (JsonElement je : jsonArray) {
						for (Entry<String, JsonElement> value : je.getAsJsonObject().entrySet()) {
							// check if field has flag Tc.SV_ISLABEL:true
							if (svr != null) {
								dboField = SvReader.getFieldByName(tableName, value.getKey().toUpperCase());
								if (dboField != null && dboField.getVal(Tc.SV_ISLABEL) != null
										&& dboField.getVal(Tc.SV_ISLABEL).equals(true)) {
									lhmObj.put(tableName + "." + value.getKey().toUpperCase() + "_CODE",
											value.getValue());
									StringBuilder sb = new StringBuilder("\""
											+ (I18n.getText(localeId, value.getValue().toString().replace("\"", "")))
											+ "\"");
									lhmObj.put(tableName + "." + value.getKey().toUpperCase(),
											new JsonParser().parse(sb.toString()));
								} else {
									lhmObj.put(tableName + "." + value.getKey().toUpperCase(), value.getValue());
								}
							} else {
								lhmObj.put(tableName + "." + value.getKey().toUpperCase(), value.getValue());
							}
						}
					}
				}
			}
			resultJsonArray.add(lhmObj);
		}
		if (!resultJsonArray.isEmpty()) {
			result = resultJsonArray.toJSONString();
		}
		return result;
	}

	public String convertDbDataArrayToGridJson(DbDataArray dbArray, String tableName) throws SvException {
		return convertDbDataArrayToGridJson(dbArray, tableName, null);
	}

	public String convertDbDataArrayToGridJson(DbDataArray dbArray, String tableName, SvReader svr) throws SvException {
		return convertDbDataArrayToGridJson(dbArray, tableName, false, svr);
	}

	/**
	 * This method returns list of vaccination books per animal or flock
	 * 
	 * @param dboAnimalOrFlock DbDataObject of the animal or flock
	 * 
	 * @param svr              SvReader instance
	 * 
	 * @return DbDataArray
	 */
	public DbDataArray getLinkedVaccinationBooksPerAnimalOrFlock(DbDataObject dboAnimalOrFlock, SvReader svr)
			throws SvException {
		DbDataArray result = new DbDataArray();
		String linkType = Tc.ANIMAL_VACC_BOOK;
		if (dboAnimalOrFlock.getObject_type().equals(SvReader.getTypeIdByName(Tc.FLOCK))) {
			linkType = Tc.FLOCK_VACC_BOOK;
		}
		DbDataObject linkAnimalVaccBook = SvLink.getLinkType(linkType, dboAnimalOrFlock.getObject_type(),
				SvReader.getTypeIdByName(Tc.VACCINATION_BOOK));
		DbDataArray allItems = svr.getObjectsByLinkedId(dboAnimalOrFlock.getObject_id(),
				dboAnimalOrFlock.getObject_type(), linkAnimalVaccBook, SvReader.getTypeIdByName(Tc.VACCINATION_BOOK),
				false, null, 0, 0);
		if (allItems != null && !allItems.getItems().isEmpty()) {
			result = allItems;
		}
		return result;
	}

	/**
	 * Method that returns animal linked to certain vacination book
	 * 
	 * @param vaccinationBookObj VACCINATION_BOOK instance
	 * @param svr                SvReader instance
	 * @return
	 * @throws SvException
	 */
	public DbDataObject getAnimalLinkedToVaccinationBook(DbDataObject vaccinationBookObj, SvReader svr)
			throws SvException {
		DbDataObject animalObj = null;
		DbDataObject linkAnimalVaccBook = SvLink.getLinkType(Tc.ANIMAL_VACC_BOOK, SvReader.getTypeIdByName(Tc.ANIMAL),
				SvReader.getTypeIdByName(Tc.VACCINATION_BOOK));
		DbDataArray allItems = svr.getObjectsByLinkedId(vaccinationBookObj.getObject_id(),
				SvReader.getTypeIdByName(Tc.VACCINATION_BOOK), linkAnimalVaccBook, SvReader.getTypeIdByName(Tc.ANIMAL),
				true, null, 0, 0);
		if (allItems != null && !allItems.getItems().isEmpty()) {
			animalObj = allItems.get(0);
		}
		return animalObj;
	}

	/**
	 * This method returns list of vacc events by vacc book
	 * 
	 * @param animalObj DbDataObject of the animal
	 * 
	 * @param svr       SvReader instance
	 * 
	 * @return DbDataArray
	 */
	public DbDataArray getAllVaccEventsForVaccBook(DbDataObject dboVaccBook, SvReader svr) throws SvException {
		DbDataArray dbArrEvents = null;
		if (dboVaccBook != null) {
			DbDataObject dboLinkType = SvLink.getLinkType(Tc.VACC_EVENT_BOOK,
					SvReader.getTypeIdByName(Tc.VACCINATION_EVENT), SvReader.getTypeIdByName(Tc.VACCINATION_BOOK));
			dbArrEvents = svr.getObjectsByLinkedId(dboVaccBook.getObject_id(),
					SvReader.getTypeIdByName(Tc.VACCINATION_BOOK), dboLinkType,
					SvReader.getTypeIdByName(Tc.VACCINATION_EVENT), true, null, 0, 0);
			if (dbArrEvents.getItems().isEmpty() && dboVaccBook.getVal(Tc.CAMPAIGN_NAME) != null) {
				dbArrEvents = searchForObjectWithSingleFilter(SvReader.getTypeIdByName(Tc.VACCINATION_EVENT),
						Tc.CAMPAIGN_NAME, dboVaccBook.getVal(Tc.CAMPAIGN_NAME).toString(), svr);
			}
		}
		return dbArrEvents;
	}

	public DbDataObject getVaccEventByName(String campaignName, SvReader svr) throws SvException {
		DbDataObject vaccEvent = null;
		DbSearchCriterion cr1 = new DbSearchCriterion(Tc.CAMPAIGN_NAME, DbCompareOperand.LIKE, campaignName);
		DbDataArray result = svr.getObjects(new DbSearchExpression().addDbSearchItem(cr1),
				SvReader.getTypeIdByName(Tc.VACCINATION_EVENT), null, 0, 0);
		if (result != null && !result.getItems().isEmpty()) {
			vaccEvent = result.get(0);
		}
		return vaccEvent;
	}

	/**
	 * Help method that fetch all dependent data for disease according campaign type
	 * Purpose: to load appropriate list of dependent dropdown
	 * 
	 * @param parentCode:   code_value of parent code list item
	 * @param codeListName: name of the code list, which should be accessed
	 * @throws SvException
	 */
	public DbDataArray searchForDependentDiseaseByCapmaignType(String campaignActivityCode, SvReader svr)
			throws SvException {
		DbDataArray itemsFound = new DbDataArray();

		String diseaseCodes = Tc.EMPTY_STRING;
		if (campaignActivityCode.equals("1")) {
			diseaseCodes = "1,2,5,3,11,13,14,17,37,18,19,10,9,35,58,60,70,71,61,72";
		} else if (campaignActivityCode.equals("2")) {
			diseaseCodes = "4,23,5,3,22,24,2,1,25,8,26,27,28,34,33,31,7,6,9,10,29,11,13,14,15,16,17,37,18,38,39,40,41,42,43,44,46,47,48,21,62,63,64,65,66,67,69,70,71,72";
		} else if (campaignActivityCode.equals("3")) {
			diseaseCodes = "4,51,22";
		} else if (campaignActivityCode.equals("4")) {
			diseaseCodes = "21,20,68,69";
		}

		DbSearchExpression srchExpr = null;
		DbSearchCriterion filterByParentCodeValue = new DbSearchCriterion(Tc.PARENT_CODE_VALUE, DbCompareOperand.EQUAL,
				"DISEASE_NAME");
		String[] diseaseAry = diseaseCodes.split(",");
		String userLocale = svr.getUserLocaleId(svr.getInstanceUser());
		for (int i = 0; i < diseaseAry.length; i++) {
			srchExpr = new DbSearchExpression();
			DbSearchCriterion filterByCodeValue = new DbSearchCriterion(Tc.CODE_VALUE, DbCompareOperand.EQUAL,
					diseaseAry[i]);
			srchExpr.addDbSearchItem(filterByParentCodeValue).addDbSearchItem(filterByCodeValue);
			DbDataArray searchResult = svr.getObjects(srchExpr, svCONST.OBJECT_TYPE_CODE, null, 0, 0);
			if (!searchResult.getItems().isEmpty()) {
				String translatedCodeItem = I18n.getText(userLocale,
						searchResult.get(0).getVal(Tc.LABEL_CODE).toString());
				searchResult.get(0).setVal(Tc.LBL_TRANSL, translatedCodeItem);
				itemsFound.addDataItem(searchResult.get(0));
			}
		}

		return itemsFound;
	}

	/**
	 * Method for calculating precentage
	 * 
	 * @param num      number of total number
	 * @param totalNum total number
	 * @return
	 */
	public Integer calculatePrecentage(Integer num, Integer totalNum) {
		double precentage = (double) num / totalNum;
		double resultPrecentage = precentage * 100;
		Integer totalResult = (int) resultPrecentage;
		return totalResult;
	}

	public Boolean checkIfVaccineBookRecordPerGivenDiseaseIsMissing(DbDataArray vaccBooksPerDisease, String disease,
			SvReader svr) {
		Boolean result = false;
		if (vaccBooksPerDisease != null && !vaccBooksPerDisease.getItems().isEmpty()) {
			if (!checkIfHealthBookHaveVaccRecordPerDisease(vaccBooksPerDisease, disease, svr)) {
				result = true;
			}
		} else {
			result = true;
		}
		return result;
	}

	/**
	 * Method that checks if vaccination book have Vacc_Code record per given
	 * disease.
	 * 
	 * @param arrHealthBooks     Array of vaccination books
	 * @param vaccCodeForDisease Vaccination code for disease
	 * @param svr                SvReader instance
	 * @return
	 */
	public Boolean checkIfHealthBookHaveVaccRecordPerDisease(DbDataArray arrHealthBooks, String vaccCodeForDisease,
			SvReader svr) {
		Boolean result = false;
		for (DbDataObject dboVaccBook : arrHealthBooks.getItems()) {
			if (dboVaccBook.getVal(Tc.VACC_CODE) != null
					&& dboVaccBook.getVal(Tc.VACC_CODE).toString().startsWith(vaccCodeForDisease)) {
				result = true;
				break;
			}
		}
		return result;
	}

	// USER METHODS

	/**
	 * Method that returns all the data for specific user
	 * 
	 * @param dboUser    DbDataObject of user
	 * 
	 * @param contactObj DbDataObject of CONTACT_DATA type
	 * 
	 * @throws SvException
	 * @return String
	 */
	public String getUserFullData(DbDataObject dboUser, DbDataObject contactObj) {

		String result = Tc.EMPTY_STRING;
		String userName = Tc.EMPTY_STRING;
		String userType = Tc.EMPTY_STRING;
		String userFirstName = Tc.EMPTY_STRING;
		String userLastName = Tc.EMPTY_STRING;
		String userEmail = Tc.EMPTY_STRING;
		String pin = Tc.EMPTY_STRING;
		String streetType = Tc.EMPTY_STRING;
		String streetName = Tc.EMPTY_STRING;
		String houseNumber = Tc.EMPTY_STRING;
		String postalCode = Tc.EMPTY_STRING;
		String city = Tc.EMPTY_STRING;
		String state = Tc.EMPTY_STRING;
		String phoneNumber = Tc.EMPTY_STRING;
		String mobilePhoneNumber = Tc.EMPTY_STRING;
		String fax = Tc.EMPTY_STRING;
		String email = Tc.EMPTY_STRING;

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
		if (contactObj != null && !contactObj.getObject_id().equals(0L)) {
			if (contactObj.getVal("STREET_TYPE") != null)
				streetType += contactObj.getVal("STREET_TYPE").toString();
			if (contactObj.getVal("STREET_NAME") != null)
				streetName += contactObj.getVal("STREET_NAME").toString();
			if (contactObj.getVal("HOUSE_NUMBER") != null) {
				houseNumber += contactObj.getVal("HOUSE_NUMBER").toString();
			}
			if (contactObj.getVal("POSTAL_CODE") != null)
				postalCode = contactObj.getVal("POSTAL_CODE").toString();
			if (contactObj.getVal("CITY") != null)
				city = contactObj.getVal("CITY").toString();
			if (contactObj.getVal("STATE") != null)
				state = contactObj.getVal("STATE").toString();
			if (contactObj.getVal("PHONE_NUMBER") != null)
				phoneNumber = contactObj.getVal("PHONE_NUMBER").toString();
			if (contactObj.getVal("MOBILE_NUMBER") != null)
				mobilePhoneNumber = contactObj.getVal("MOBILE_NUMBER").toString();
			if (contactObj.getVal("FAX") != null)
				fax = contactObj.getVal("FAX").toString();
			if (contactObj.getVal("EMAIL") != null)
				email = contactObj.getVal("EMAIL").toString();
		}

		result = "userObjId:" + dboUser.getObject_id().toString() + ";userName:" + userName + ";userType:" + userType
				+ ";userFirstName:" + userFirstName + ";userLastName:" + userLastName + ";userEmail:" + userEmail
				+ ";pinNumber:" + pin + ";streetType:" + streetType + ";streetName:" + streetName + ";houseNumber:"
				+ houseNumber + ";postalCode:" + postalCode + ";city:" + city + ";state:" + state + ";phoneNumber:"
				+ phoneNumber + ";mobilePhoneNumber:" + mobilePhoneNumber + ";fax:" + fax + ";email:" + email;

		return result;
	}

	/**
	 * 
	 * @param userObj DbDataObject of the user
	 * @param svn     SvNote instance
	 * @param svr     SvReader instance
	 * @return LinkedHashMap<String, String>
	 * @throws SvException
	 */
	public void generateUserOrGroupSummaryInformation(LinkedHashMap<String, String> jsonOrderedMap,
			DbDataObject userObj, SvNote svn, SvReader svr) throws SvException {
		if (userObj.getObject_type().equals(svCONST.OBJECT_TYPE_USER) && userObj.getStatus().equals(Tc.SUSPENDED)) {
			String suspensionNote = svn.getNote(userObj.getObject_id(), "SUSPENSION_NOTE");
			jsonOrderedMap.put("naits.userSuspensionNote:", suspensionNote);
		}
		UserManager userM = new UserManager();
		ArrayList<String> currentCustomPermissions = userM.getCustomPermissionForUserOrGroup(userObj, svr);
		int real_cnt = 0;
		if (!currentCustomPermissions.isEmpty()) {
			for (int i = 0; i < currentCustomPermissions.size(); i++) {
				if (!jsonOrderedMap.containsValue(currentCustomPermissions.get(i))) {
					real_cnt++;
					jsonOrderedMap.put(real_cnt + ".", currentCustomPermissions.get(i));
				}
			}
		}
	}

	public String getLaboratoriesAssignedToUserBySessionId(SvReader svr) throws SvException {
		String result = "[]";
		DbDataObject userObj = SvReader.getUserBySession(svr.getSessionId());
		if (userObj != null) {
			DbDataObject linkPOABetweenUserAndLaboratory = SvReader.getLinkType(Tc.POA, userObj.getObject_type(),
					SvReader.getTypeIdByName(Tc.LABORATORY));
			DbDataArray linkedLabs = svr.getObjectsByLinkedId(userObj.getObject_id(), linkPOABetweenUserAndLaboratory,
					null, 0, 0);
			if (linkedLabs != null) {
				result = convertDbDataArrayToGridJson(linkedLabs, "LABORATORY");
			}
		}
		return result;
	}

	// LABEL MANAGEMENT METHODS

	/**
	 * Method that returns appropriate label translation for field/column
	 * 
	 * @param codeListId object_id of the appropriate code list
	 * 
	 * @param value      code value of the code list item
	 * 
	 * @param localeId   locale_id
	 * @param svr        SvReader instance
	 * 
	 * @throws SvException
	 * @return String of the translated code value
	 */
	public String translateCodeValueForField(Long codeListId, String value, String localeId, SvReader svr)
			throws SvException {
		String translatedCodeValue = Tc.NOT_AVAILABLE_NA;
		DbDataObject codeListObj = svr.getObjectById(codeListId, svCONST.OBJECT_TYPE_CODE, null);
		if (codeListObj != null && value != null && !value.trim().equals("")) {
			DbDataArray codeListItems = svr.getObjectsByParentId(codeListObj.getObject_id(), svCONST.OBJECT_TYPE_CODE,
					null, 0, 0);
			if (codeListItems != null && !codeListItems.getItems().isEmpty()) {
				for (DbDataObject currCodeListItem : codeListItems.getItems()) {
					if (currCodeListItem.getVal(Tc.CODE_VALUE) != null
							&& currCodeListItem.getVal(Tc.CODE_VALUE).equals(value)
							&& currCodeListItem.getVal(Tc.LABEL_CODE) != null) {
						translatedCodeValue = I18n.getText(localeId, currCodeListItem.getVal(Tc.LABEL_CODE).toString());
						break;
					}
				}
			}
		}
		return translatedCodeValue;
	}

	/**
	 * Method that checks and returns the label if is already installed
	 * 
	 * @param labelCode the labelCode of the label we look for
	 * @param localeId  localeId of the label we look for (for the same labelCode)
	 * @param svReader  SvReader instance
	 * @throws SvException
	 * @return DbDataObject if exists, else null
	 * 
	 */
	public DbDataObject getLabel(String labelCode, String localeId, SvReader svReader) throws SvException {

		DbDataObject result = null;
		DbDataArray searchResult = new DbDataArray();
		DbSearchExpression findLabel = new DbSearchExpression();
		DbSearchCriterion byLabelCode = new DbSearchCriterion(Tc.LABEL_CODE, DbCompareOperand.EQUAL, labelCode);
		DbSearchCriterion byLocaleId = new DbSearchCriterion(Tc.LOCALE_ID, DbCompareOperand.EQUAL, localeId);
		findLabel.addDbSearchItem(byLabelCode);
		findLabel.addDbSearchItem(byLocaleId);
		searchResult = svReader.getObjects(findLabel, svCONST.OBJECT_TYPE_LABEL, null, 0, 0);
		if (searchResult.getItems().isEmpty()) {
			searchResult = svReader.getObjects(findLabel, svCONST.OBJECT_TYPE_LABEL, new DateTime(), 0, 0);
		}
		if (!searchResult.getItems().isEmpty()) {
			result = searchResult.getItems().get(0);
		}
		return result;
	}

	/**
	 * Method to fetch appropriate label translation for table id/value/locale id
	 * 
	 * @param tableId   object_type of the table
	 * 
	 * @param fieldName name of the column
	 * 
	 * @param value     code_list value
	 * 
	 * @param localeId  locale_id
	 * 
	 * @param svr       SvReader instance
	 * 
	 * @throws SvException
	 * @return String of the translated value
	 */
	public String decodeCodeValue(Long tableId, String fieldName, String value, String localeId, SvReader svr)
			throws SvException {
		String translatedValue = Tc.NOT_AVAILABLE_NA;
		DbDataObject dboTable = svr.getObjectById(tableId, svCONST.OBJECT_TYPE_TABLE, null);
		if (dboTable != null && value != null) {
			DbDataArray fields = svr.getObjectsByParentId(dboTable.getObject_id(), svCONST.OBJECT_TYPE_FIELD, null, 0,
					0);
			DbDataObject fieldObj = null;
			if (fields != null && !fields.getItems().isEmpty()) {
				for (DbDataObject currField : fields.getItems()) {
					if (currField.getVal(Tc.FIELD_NAME).equals(fieldName)) {
						fieldObj = currField;
						break;
					}
				}
				if (fieldObj != null && fieldObj.getVal(Tc.CODE_LIST_ID) != null) {
					Long codeListId = Long.valueOf(fieldObj.getVal(Tc.CODE_LIST_ID).toString());
					translatedValue = translateCodeValueForField(codeListId, value, localeId, svr);
				}
			}
		}
		return translatedValue;
	}

	// LABORATORY MODULE METHODS

	/**
	 * Method for searching Lab Sample by Sample ID
	 * 
	 * @param sampleId
	 * @param svr
	 * @return
	 * @throws SvException
	 */
	public DbDataObject getLabSampleBySampleId(String sampleId, SvReader svr) throws SvException {
		DbDataObject labSampleObj = null;
		DbSearchCriterion cr1 = new DbSearchCriterion(Tc.SAMPLE_ID, DbCompareOperand.EQUAL, sampleId);
		DbDataArray resultArr = svr.getObjects(new DbSearchExpression().addDbSearchItem(cr1),
				SvReader.getTypeIdByName(Tc.LAB_SAMPLE), null, 0, 0);
		if (resultArr != null && !resultArr.getItems().isEmpty()) {
			labSampleObj = resultArr.get(0);
		}
		return labSampleObj;
	}

	public DbDataObject getLinkObjectBetweenTwoLinkedObjects(DbDataObject dboObj1, DbDataObject dboObj2,
			String linkName, SvReader svr) throws SvException {
		DbDataObject getLink = null;
		if (dboObj1 != null && dboObj2 != null) {
			DbDataObject linkLaboratorySample = SvLink.getLinkType(linkName, dboObj1.getObject_type(),
					dboObj2.getObject_type());
			if (linkLaboratorySample != null) {
				getLink = getLinkObject(dboObj1.getObject_id(), dboObj2.getObject_id(),
						linkLaboratorySample.getObject_id(), svr);
			}
		}
		return getLink;
	}

	public DbDataArray getTestTypeApplicableCombination(String multiDisease, String sampleType, SvReader svr)
			throws SvException {
		DbSearchCriterion cr1 = null;
		DbSearchCriterion cr2 = null;
		DbSearchExpression dbse = null;
		DbDataArray arr = null;
		DbDataArray selectedDiseases = new DbDataArray();
		List<String> listOfDiseases = new ArrayList<String>(Arrays.asList(multiDisease.split(",")));
		for (String i : listOfDiseases) {
			cr1 = new DbSearchCriterion(Tc.DISEASE, DbCompareOperand.EQUAL, i);
			cr2 = new DbSearchCriterion(Tc.SAMPLE_TYPE, DbCompareOperand.EQUAL, sampleType);
			dbse = new DbSearchExpression();
			dbse.addDbSearchItem(cr1).addDbSearchItem(cr2);
			arr = svr.getObjects(dbse, SvReader.getTypeIdByName(Tc.LAB_TEST_TYPE), null, 0, 0);
			if (arr != null && !arr.getItems().isEmpty()) {
				for (DbDataObject temp : arr.getItems()) {
					selectedDiseases.addDataItem(temp);
				}
			} else {
				selectedDiseases = new DbDataArray();
				break;
			}
		}
		return selectedDiseases;
	}

	public String checkIfAllTestResultsHaveHealthStatus(DbDataObject dboLabSample, DbDataObject dbo, SvReader svr)
			throws SvException {
		Boolean resultTestCheck = true;
		String result = Tc.EMPTY_STRING;
		if (dboLabSample != null && (dboLabSample.getVal(Tc.LAB_TEST_STATUS) == null
				|| dboLabSample.getVal(Tc.LAB_TEST_STATUS).toString().equals(""))) {
			DbDataArray testResults = svr.getObjectsByParentId(dboLabSample.getObject_id(),
					SvReader.getTypeIdByName(Tc.LAB_TEST_RESULT), null, 0, 0);
			if (dbo != null && dbo.getVal(Tc.TEST_RESULT) != null
					&& !dbo.getVal(Tc.TEST_RESULT).toString().equals("0")) {
				if (testResults != null && !testResults.getItems().isEmpty()) {
					for (DbDataObject testResObj : testResults.getItems()) {
						if (testResObj != null && !testResObj.getObject_id().equals(dbo.getObject_id())
								&& (testResObj.getVal(Tc.TEST_RESULT) == null
										|| testResObj.getVal(Tc.TEST_RESULT).equals(""))) {
							resultTestCheck = false;
							break;
						}
					}
					if (!resultTestCheck) {
						for (DbDataObject testResObj : testResults.getItems()) {
							if (testResObj != null && !testResObj.getObject_id().equals(dbo.getObject_id())
									&& testResObj.getVal(Tc.TEST_RESULT) != null
									&& testResObj.getVal(Tc.TEST_RESULT).toString().equals("0")) {
								result = "en.positive";
								break;
							}
						}
						if (!result.equals("en.positive")) {
							result = "en.negative";
						}
					}
				}
			} else if (dbo != null && dbo.getVal(Tc.TEST_RESULT) != null
					&& dbo.getVal(Tc.TEST_RESULT).toString().equals("0")) {
				result = "en.positive";
			}
		}
		return result;
	}

	/**
	 * Method that returns label if Laboratory Sample that belongs to certain animal
	 * have status POSITIVE
	 * 
	 * @param animalOrHolding
	 * @param svr
	 * @return
	 * @throws SvException
	 */
	public String checkLabSampleHealthStatus(DbDataObject animalOrHoldingObj, SvReader svr) throws SvException {
		String result = Tc.EMPTY_STRING;
		DbDataArray labSamplesArr = svr.getObjectsByParentId(animalOrHoldingObj.getObject_id(),
				SvReader.getTypeIdByName(Tc.LAB_SAMPLE), null, 0, 0);
		if (labSamplesArr != null && !labSamplesArr.getItems().isEmpty()) {
			DbDataObject dboLabSample = labSamplesArr.get(labSamplesArr.size() - 1);
			if (dboLabSample.getVal(Tc.TEST_RESULT_STATUS) != null
					&& dboLabSample.getVal(Tc.TEST_RESULT_STATUS).toString().equals("0")) {
				result = "block_reason_AnimalHasPositiveLabSample";
			}
		}
		return result;
	}

	public Integer getPeriodOfMonthsBetweenParameterDateAndDateNow(DbDataObject dbDataObj, String dateColumn) {
		Integer result = 0;
		DateTime dateNow = new DateTime();
		if (dbDataObj.getVal(dateColumn) != null) {
			DateTime currentDate = new DateTime(dbDataObj.getVal(dateColumn).toString());
			Period diff = new Period(currentDate, dateNow);
			result = diff.getYears() * 12 + diff.getMonths();
		}
		return result;
	}

	public Integer getPeriodOfMonthsBetweenTwoDates(String dateFirst, String dateSecond) {
		Integer result = 0;
		if (dateFirst != null && dateSecond != null) {
			DateTime dateOfObj1 = new DateTime(dateFirst);
			DateTime dateOfObj2 = new DateTime(dateSecond);
			Period diff = new Period(dateOfObj1, dateOfObj2);
			result = diff.getYears() * 12 + diff.getMonths();
		}
		return result;
	}

	public String getHealthStatusPerAnimalAccordingLabSample(DbDataObject animalOrHoldingObj, SvReader svr)
			throws SvException {
		String result = Tc.EMPTY_STRING;
		String diseaseCause = Tc.EMPTY_STRING;
		DbDataArray labSamplesArr = svr.getObjectsByParentId(animalOrHoldingObj.getObject_id(),
				SvReader.getTypeIdByName(Tc.LAB_SAMPLE), null, 0, 0);
		if (labSamplesArr != null && !labSamplesArr.getItems().isEmpty()) {
			for (DbDataObject dataObject : labSamplesArr.getItems()) {
				diseaseCause = checkLabSampleDiseasesForMovementCheck(dataObject, svr);
				if (!diseaseCause.equals("")) {
					result = dataObject.getObject_id() + "," + diseaseCause;
					break;
				}
			}
		}
		return result;
	}

	/**
	 * Method that checks if period between Animal Health Book last record and Lab
	 * Sample collection date is same as parameter daysOrMonths
	 * 
	 * @param holdingObj
	 * @param dateOfCollection
	 * @param period
	 * @param daysOrMonths
	 * @param svr
	 * @return
	 * @throws SvException
	 */
	public Boolean checkIfAnimalsHasValidImmunizationPeriodInAnimalHealthBook(DbDataObject holdingObj,
			String dateOfCollection, Integer period, SvReader svr) throws SvException {
		Boolean result = true;
		Integer periodVacc = -1;
		DbDataArray animalsArr = svr.getObjectsByParentId(holdingObj.getObject_id(),
				SvReader.getTypeIdByName(Tc.ANIMAL), null, 0, 0);
		if (animalsArr != null && !animalsArr.getItems().isEmpty()) {
			for (DbDataObject tempAnim : animalsArr.getItems()) {
				if (tempAnim != null && tempAnim.getStatus().equals(Tc.VALID)) {
					DbDataArray vaccRecords = getLinkedVaccinationBooksPerAnimalOrFlock(tempAnim, svr);
					if (vaccRecords != null && !vaccRecords.getItems().isEmpty()) {
						DbDataObject lastVaccRecordInAnimalHealthBook = vaccRecords.get(0);
						if (lastVaccRecordInAnimalHealthBook.getVal(Tc.VACC_DATE) != null) {
							DateTime vaccRecDate = new DateTime(
									lastVaccRecordInAnimalHealthBook.getVal(Tc.VACC_DATE).toString());
							DateTime collectionDate = new DateTime(dateOfCollection);
							if (vaccRecDate.isAfter(collectionDate)) {
								periodVacc = (int) ValidationChecks.getDateDiff(collectionDate, vaccRecDate,
										TimeUnit.DAYS);
							}
						}
						if (periodVacc.equals(-1) || periodVacc >= period) {
							result = false;
							break;
						}
					} else {
						result = false;
						break;
					}
				}
			}
		}
		return result;
	}

	/**
	 * Method that returns prohibited diseases in Movement Document
	 * 
	 * @param testResultObject
	 * @return
	 */
	public String getDiseaseCodeAccordingTestResult(DbDataObject testResultObject) {
		String diseaseResult = Tc.EMPTY_STRING;
		String dieaseTestName = testResultObject.getVal(Tc.SAMPLE_DISEASE).toString();
		String positiveOrSuspectFlag = testResultObject.getVal(Tc.TEST_RESULT).toString();
		switch (dieaseTestName) {
		case "5":
			diseaseResult = Tc.RABIES_SUSPECT;
			if (positiveOrSuspectFlag.equals("0")) {
				diseaseResult = Tc.RABIES_POSITIVE;
			}
			break;
		case "2":
			diseaseResult = Tc.ANTHRAX_SUSPECT;
			if (positiveOrSuspectFlag.equals("0")) {
				diseaseResult = Tc.ANTHRAX_POSITIVE;
			}
			break;
		case "3":
			diseaseResult = Tc.BRUC_SUSPECT;
			if (positiveOrSuspectFlag.equals("0")) {
				diseaseResult = Tc.BRUC_POSITIVE;
			}
			break;
		case "1":
			diseaseResult = Tc.FMD_SUSPECT;
			if (positiveOrSuspectFlag.equals("0")) {
				diseaseResult = Tc.FMD_POSITIVE;
			}
			break;
		case "4":
			diseaseResult = Tc.TUBER_SUSPECT;
			if (positiveOrSuspectFlag.equals("0")) {
				diseaseResult = Tc.TUBER_POSITIVE;
			}
			break;
		default:
			break;
		}
		return diseaseResult;
	}

	/**
	 * Method that check if Lab Sample has status Positive by some of the blocking
	 * diseases. For example, if the Lab Sample is positive because of anthrax, it
	 * means it's blocking disease.
	 * 
	 * @param labSampleObject Lab Sample instance
	 * @param svr             SvReader instance
	 * @return blocking disease. Return Type: String
	 * @throws SvException
	 */
	public String checkLabSampleDiseasesForMovementCheck(DbDataObject labSampleObject, SvReader svr)
			throws SvException {
		String diseaseResult = Tc.EMPTY_STRING;
		if (labSampleObject.getStatus().equals(Tc.PROCESSED)) {
			DbDataArray testResultsArr = getLabTestResultsPerLabSample(labSampleObject, svr);
			if (testResultsArr != null && !testResultsArr.getItems().isEmpty()) {
				for (DbDataObject dboTestRes : testResultsArr.getItems()) {
					if (dboTestRes != null && dboTestRes.getVal(Tc.SAMPLE_DISEASE) != null
							&& dboTestRes.getVal(Tc.TEST_RESULT) != null
							&& (dboTestRes.getVal(Tc.TEST_RESULT).toString().equals("2")
									|| dboTestRes.getVal(Tc.TEST_RESULT).toString().equals("0"))) {
						diseaseResult = getDiseaseCodeAccordingTestResult(dboTestRes);
						if (!diseaseResult.equals("")) {
							break;
						}
					}
				}
			}
		}
		return diseaseResult;
	}

	/**
	 * Method that checks if inconclusive sample is tested again.
	 * 
	 * @param animalObj
	 * @param svr
	 * @return
	 * @throws SvException
	 */
	public boolean checkifInconclusiveSamplesAreTestedAgainPerAnimal(DbDataObject animalObj, SvReader svr)
			throws SvException {
		Boolean result = true;
		String diseaseCause = Tc.EMPTY_STRING;
		DbDataObject tempLabSample = null;
		String dateOfSampleCollection = Tc.EMPTY_STRING;
		DbDataArray labSamplesArr = getLabSamplesPerAnimal(animalObj, svr);
		if (labSamplesArr != null && !labSamplesArr.getItems().isEmpty()) {
			for (DbDataObject labSample : labSamplesArr.getItems()) {
				if (labSample.getStatus().equals(Tc.PROCESSED)) {
					result = false;
					diseaseCause = checkLabSampleDiseasesForMovementCheck(labSample, svr);
					if (!diseaseCause.equals("")) {
						dateOfSampleCollection = labSample.getVal(Tc.DATE_OF_COLLECTION).toString();
						tempLabSample = labSample;
						break;
					}
				}
			}
			if (!diseaseCause.equals("")) {
				for (DbDataObject labSample : labSamplesArr.getItems()) {
					if (labSample.getVal(Tc.DATE_OF_COLLECTION) != null) {
						DateTime dateOfRetakenLabSample = new DateTime(
								labSample.getVal(Tc.DATE_OF_COLLECTION).toString());
						DateTime dateOfInconclusiveSample = new DateTime(dateOfSampleCollection);
						if (dateOfRetakenLabSample.isAfter(dateOfInconclusiveSample)
								&& labSample.getVal(Tc.DISEASE_TEST) != null && tempLabSample != null
								&& tempLabSample.getVal(Tc.DISEASE_TEST) != null && labSample.getVal(Tc.DISEASE_TEST)
										.toString().equals(tempLabSample.getVal(Tc.DISEASE_TEST).toString())) {
							result = true;
							break;
						}
					}
				}
			}
		}
		return result;
	}

	/**
	 * Method that checks if certain Holding Object have animals that have Lab
	 * Sample that has TEST_RESULT_STATUS equal to POSITIVE. This method can be used
	 * as holding healthStatus check.
	 * 
	 * @param holdingObj
	 * @param svr
	 * @return false if it's okay
	 * @throws SvException
	 */
	public Boolean checkIfHoldingHasAnimalsThatHavePositiveLabSample(DbDataObject holdingObj, SvReader svr)
			throws SvException {
		Boolean result = false;
		String flagString = Tc.EMPTY_STRING;
		DbDataArray animalsArr = svr.getObjectsByParentId(holdingObj.getObject_id(),
				SvReader.getTypeIdByName(Tc.ANIMAL), null, 0, 0);
		if (animalsArr != null && !animalsArr.getItems().isEmpty()) {
			for (DbDataObject animalObj : animalsArr.getItems()) {
				flagString = getHealthStatusPerAnimalAccordingLabSample(animalObj, svr);
				if (!flagString.equals("")) {
					result = true;
					break;
				}
			}
		}
		return result;
	}

	/**
	 * Method that returns Movement Document Block
	 * 
	 * @param movementDocObjId           Movement Document Object_Id (Parent_Id of
	 *                                   the Movement Doc Block)
	 * @param animalOrFlockMovementObjId Animal/Flock movement Object_Id
	 * @param svr                        SvReader instance
	 * @return
	 * @throws SvException
	 */
	public DbDataObject getMovementDocBlockByMovementObjId(Long movementDocObjId, Long animalOrFlockMovementObjId,
			SvReader svr) throws SvException {
		DbDataObject dboMovementDocBlock = null;
		DbDataArray arrMvmDocBlocks = svr.getObjectsByParentId(movementDocObjId,
				SvReader.getTypeIdByName(Tc.MOVEMENT_DOC_BLOCK), null, 0, 0);
		if (arrMvmDocBlocks != null && !arrMvmDocBlocks.getItems().isEmpty()) {
			for (DbDataObject tempBlock : arrMvmDocBlocks.getItems()) {
				if (tempBlock.getVal(Tc.MOVE_OBJ_ID) != null && Long
						.valueOf(tempBlock.getVal(Tc.MOVE_OBJ_ID).toString()).equals(animalOrFlockMovementObjId)) {
					dboMovementDocBlock = tempBlock;
				}
			}
		}
		return dboMovementDocBlock;
	}

	public DbDataArray getLabSamplesPerAnimal(DbDataObject dboAnimal, SvReader svr) throws SvException {
		DbDataArray labSamplesArr = null;
		if (dboAnimal != null) {
			labSamplesArr = svr.getObjectsByParentId(dboAnimal.getObject_id(), SvReader.getTypeIdByName(Tc.LAB_SAMPLE),
					null, 0, 0);
		}
		return labSamplesArr;
	}

	public DbDataArray getLabTestResultsPerLabSample(DbDataObject dboLabSample, SvReader svr) throws SvException {
		DbDataArray labTestResultsArr = null;
		if (dboLabSample != null) {
			labTestResultsArr = svr.getObjectsByParentId(dboLabSample.getObject_id(),
					SvReader.getTypeIdByName(Tc.LAB_TEST_RESULT), null, 0, 0);
		}
		return labTestResultsArr;
	}

	// INVENTORY ITEM METHODS

	public DbDataObject getDboInventoryItemDependOnTransfer(Long parentId, String earTagNum, String earTagType,
			SvReader svr) throws SvException {
		return getDboInventoryItemDependOnTransfer(parentId, earTagNum, earTagType, Tc.INVENTORY_ITEM, svr);
	}

	public DbDataObject getDboInventoryItemDependOnTransfer(Long parentId, String earTagNum, String earTagType,
			String tableName, SvReader svr) throws SvException {
		DbDataObject result = null;
		// DbSearchCriterion cr1 = new DbSearchCriterion(Tc.PARENT_ID,
		// DbCompareOperand.EQUAL, parentId);
		DbSearchCriterion cr2 = new DbSearchCriterion(Tc.EAR_TAG_NUMBER, DbCompareOperand.EQUAL, earTagNum);
		DbSearchCriterion cr3 = new DbSearchCriterion(Tc.TAG_TYPE, DbCompareOperand.EQUAL, earTagType);
		DbSearchExpression dbse = new DbSearchExpression();
		// dbse.addDbSearchItem(cr1).addDbSearchItem(cr2).addDbSearchItem(cr3);
		dbse.addDbSearchItem(cr2).addDbSearchItem(cr3);
		DbDataArray arr = svr.getObjects(dbse, SvReader.getTypeIdByName(tableName), null, 0, 0);
		if (!arr.getItems().isEmpty()) {
			result = arr.get(0);
		}
		return result;
	}

	public DbDataObject getDboInventoryItem(String earTagNum, String earTagType, SvReader svr) throws SvException {
		DbDataObject result = null;
		DbSearchCriterion cr1 = new DbSearchCriterion(Tc.EAR_TAG_NUMBER, DbCompareOperand.EQUAL, earTagNum);
		DbSearchCriterion cr2 = new DbSearchCriterion(Tc.TAG_TYPE, DbCompareOperand.EQUAL, earTagType);
		DbSearchExpression dbse = new DbSearchExpression();
		dbse.addDbSearchItem(cr1).addDbSearchItem(cr2);
		DbDataArray arr = svr.getObjects(dbse, SvReader.getTypeIdByName(Tc.INVENTORY_ITEM), null, 0, 0);
		if (!arr.getItems().isEmpty()) {
			result = arr.get(0);
		}
		return result;
	}

	public DbDataObject getDboInventoryItemDependOnTransfer(String earTagNum, String earTagType, SvReader svr)
			throws SvException {
		DbDataObject result = null;
		DbSearchCriterion cr1 = new DbSearchCriterion(Tc.EAR_TAG_NUMBER, DbCompareOperand.EQUAL, earTagNum);
		DbSearchCriterion cr2 = new DbSearchCriterion(Tc.TAG_TYPE, DbCompareOperand.EQUAL, earTagType);
		DbSearchExpression dbse = new DbSearchExpression();
		dbse.addDbSearchItem(cr1).addDbSearchItem(cr2);
		DbDataArray arr = svr.getObjects(dbse, SvReader.getTypeIdByName(Tc.INVENTORY_ITEM), null, 0, 0);
		if (!arr.getItems().isEmpty()) {
			result = arr.get(0);
		}
		return result;
	}

	public DbDataArray getUnappliedInventoryItemByAnimalEarTagNumber(String earTagNum, SvReader svr)
			throws SvException {
		// removed tag_status search criterion #1560
		DbSearchCriterion cr2 = new DbSearchCriterion(Tc.EAR_TAG_NUMBER, DbCompareOperand.EQUAL, earTagNum);
		DbDataArray result = svr.getObjects(new DbSearchExpression().addDbSearchItem(cr2),
				SvReader.getTypeIdByName(Tc.INVENTORY_ITEM), null, 0, 0);
		return result;
	}

	public LinkedHashMap<String, String> generateInventoryItemSummaryInfo(DbDataObject dboInventoryItem, Reader rdr,
			SvReader svr) throws SvException {
		String localeId = svr.getUserLocaleId(svr.getInstanceUser());
		LinkedHashMap<String, String> jsonOrderedMap = new LinkedHashMap<String, String>();
		jsonOrderedMap.put("naits.inventory_item.ear_tag_number",
				dboInventoryItem.getVal(Tc.EAR_TAG_NUMBER) != null
						? dboInventoryItem.getVal(Tc.EAR_TAG_NUMBER).toString()
						: Tc.NOT_AVAILABLE_NA);
		jsonOrderedMap.put("naits.inventory_item.ear_tag_type",
				dboInventoryItem.getVal(Tc.TAG_TYPE) != null ? rdr.decodeCodeValue(dboInventoryItem.getObject_type(),
						Tc.TAG_TYPE, dboInventoryItem.getVal(Tc.TAG_TYPE).toString(), localeId, svr)
						: Tc.NOT_AVAILABLE_NA);
		jsonOrderedMap.put("naits.inventory_item.tag_status",
				dboInventoryItem.getVal(Tc.TAG_STATUS) != null ? dboInventoryItem.getVal(Tc.TAG_STATUS).toString()
						: Tc.NOT_AVAILABLE_NA);
		jsonOrderedMap.put("naits.inventory_item.order_number",
				dboInventoryItem.getVal(Tc.ORDER_NUMBER) != null ? dboInventoryItem.getVal(Tc.ORDER_NUMBER).toString()
						: Tc.NOT_AVAILABLE_NA);
		try {
			String belongsToTransfer = "en.yes";
			DbDataObject dboTransfer = rdr.getTransferAccordingInventoryItem(dboInventoryItem.getParent_id(),
					dboInventoryItem.getVal(Tc.TAG_TYPE).toString(),
					dboInventoryItem.getVal(Tc.EAR_TAG_NUMBER).toString(), null, Tc.DRAFT, svr);
			if (dboTransfer == null) {
				belongsToTransfer = "en.nope";
			}
			jsonOrderedMap.put("naits.inventory_item.belongsToTransfer", (I18n.getText(localeId, belongsToTransfer)));

		} catch (Exception e) {
			log4j.error(e.getMessage());
			jsonOrderedMap.put("naits.inventory_item.belongsToTransfer", I18n.getText(localeId, "en.missing_data"));
		}
		DbDataObject holding = svr.getObjectById(dboInventoryItem.getParent_id(), SvReader.getTypeIdByName(Tc.HOLDING),
				null);
		if (holding != null) {
			jsonOrderedMap.put("naits.inventory_item.holdingPic", holding.getVal(Tc.PIC).toString());
		}
		return jsonOrderedMap;
	}

	// POPULATION METHODS

	public String getPopulationHoldingNumParamValue(DbDataObject dboPopulation, SvReader svr) {
		String result = null;
		SvParameter svp = null;
		try {
			svp = new SvParameter(svr);
			try {
				result = svp.getParamString(dboPopulation, "param.population_sample.num_holdings");
			} catch (Exception e) {
				return null;
			}
		} catch (SvException e) {
			log4j.error(e);
		} finally {
			if (svp != null)
				svp.release();
		}
		return result;
	}

	public String getPopulationAnimalNumParamValue(DbDataObject dboPopulation, SvReader svr) {
		String result = null;
		SvParameter svp = null;
		try {
			svp = new SvParameter(svr);
			try {
				result = svp.getParamString(dboPopulation, "param.population_sample.num_animals");
			} catch (Exception e) {
				return null;
			}
		} catch (SvException e) {
			log4j.error(e);
			return null;
		} finally {
			if (svp != null)
				svp.release();
		}
		return result;
	}

	public DbDataObject getDbPopulationHoldingNumParam(DbDataObject dboPopulation, SvReader svr) {
		DbDataObject result = null;
		SvParameter svp = null;
		try {
			svp = new SvParameter(svr);
			result = svp.getParamObject(dboPopulation, "param.population_sample.num_holdings");
		} catch (SvException e) {
			log4j.error(e);
			return null;
		} finally {
			if (svp != null)
				svp.release();
		}
		return result;
	}

	public DbDataObject getDbPopulationAnimalNumParam(DbDataObject dboPopulation, SvReader svr) {
		DbDataObject result = null;
		SvParameter svp = null;
		try {
			svp = new SvParameter(svr);
			result = svp.getParamObject(dboPopulation, "param.population_sample.num_animals");
		} catch (SvException e) {
			log4j.error(e);
			return null;
		} finally {
			if (svp != null)
				svp.release();
		}
		return result;
	}

	/**
	 * Method that checks if certain Population or Sample object status can be
	 * updated/changed
	 * 
	 * @param dbo
	 * @param nextStatus
	 * @param svr
	 * @return
	 * @throws SvException
	 */
	public Boolean checkCanChangeStatus(DbDataObject dbo, String nextStatus, SvReader svr) throws SvException {
		Boolean result = true;

		if (dbo.getStatus().equals(nextStatus)) {
			result = false;
		}
		return result;
	}

	/**
	 * Method that gets objects from type ANIMAL or HOLDING depends on extracted
	 * type in the population
	 * 
	 * @param population POPULATION object
	 * @param svr        SvReader instance
	 * @return DbDataArray
	 * @throws SvException
	 */
	public DbDataArray getAnimalsOrHoldingsBySelectionResult(DbDataObject population, SvReader svr) throws SvException {
		DbDataObject animal = null;
		DbDataObject holding = null;
		String extractedType = null;
		DbDataArray result = new DbDataArray();
		DbDataArray selectionRes = svr.getObjectsByParentId(population.getObject_id(),
				SvReader.getTypeIdByName(Tc.SELECTION_RESULT), null, 0, 0);
		if (population.getVal(Tc.EXTRACTED_TYPE) != null) {
			extractedType = population.getVal(Tc.EXTRACTED_TYPE).toString();
			if (extractedType.equals(Tc.ANIMAL)) {
				for (DbDataObject selected : selectionRes.getItems()) {
					animal = svr.getObjectById(Long.parseLong(selected.getVal(Tc.SELECTED_OBJ_ID).toString()),
							SvReader.getTypeIdByName(Tc.ANIMAL), null);
					result.addDataItem(animal);
				}
			} else if (extractedType.equals(Tc.HOLDING)) {
				for (DbDataObject selected : selectionRes.getItems()) {
					holding = svr.getObjectById(Long.parseLong(selected.getVal(Tc.SELECTED_OBJ_ID).toString()),
							SvReader.getTypeIdByName(Tc.HOLDING), null);
					result.addDataItem(holding);
				}
			}
		}
		return result;
	}

	public DbDataArray getChildsByParentObject(DbDataObject populationObj, String childType, SvReader svr)
			throws SvException {
		DbDataArray ar = svr.getObjectsByParentId(populationObj.getObject_id(), SvReader.getTypeIdByName(childType),
				null, 0, 0);
		return ar;
	}

	public DbDataObject getPopulationJob(Long objectId, SvReader svr) throws SvException {
		DbSearchCriterion cr1 = new DbSearchCriterion(Tc.NOTE, DbCompareOperand.LIKE,
				Tc.PERCENT_OPERATOR + objectId.toString());
		DbSearchExpression dbse = new DbSearchExpression();
		dbse.addDbSearchItem(cr1);
		DbDataArray result = svr.getObjects(dbse, svCONST.OBJECT_TYPE_JOB, null, 0, 0);
		result.getSortedItems(Tc.DT_INSERT);
		DbDataObject getJobObj = null;
		if (result != null && !result.getItems().isEmpty()) {
			getJobObj = result.get(result.size() - 1);
		}
		return getJobObj;
	}

	public String getAreaSelectedList(DbDataObject populationObj, SvReader svr) throws SvException {
		DbDataArray areas = null;
		DbDataObject searchObj = null;
		ArrayList<String> arl = new ArrayList<>();
		String areasString = Tc.EMPTY_STRING;
		String areaLabelCode = Tc.EMPTY_STRING;
		String sessionLocaleId = Tc.EMPTY_STRING;
		sessionLocaleId = svr.getUserLocaleId(svr.getInstanceUser());
		areas = getLinkedAreaPerPopulation(populationObj, svr);
		searchObj = searchForObject(svCONST.OBJECT_TYPE_CODE, Tc.CODE_VALUE, Tc.AREA_CODE, svr);
		if (areas != null && searchObj != null) {
			for (DbDataObject area : areas.getItems()) {
				if (area.getVal(Tc.AREA_NAME) != null) {
					areaLabelCode = translateCodeValueForField(searchObj.getObject_id(),
							area.getVal(Tc.AREA_NAME).toString(), sessionLocaleId, svr);
					arl.add(areaLabelCode);
				}
			}
			if (!arl.isEmpty()) {
				areasString = String.join(", ", arl);
			}
		}
		return areasString;
	}

	// DISEASE METHODS

	/**
	 * Method that gets all valid combination of Animal class/race and disease.
	 * 
	 * @param dboAnimal ANIMAL object
	 * @param svr       SvReader instance
	 * @return DbDataArray
	 * @throws SvException
	 */
	public DbDataArray getAppropriateDiseasesByAnimal(DbDataObject dboAnimal, SvReader svr) throws SvException {
		DbDataArray getAnimalTypes = null;
		DbDataObject currentAniType = null;
		DbDataObject linkBetweenAnimalTypeAndDisease = null;
		DbDataArray getLinkedDiseases = null;
		if (dboAnimal != null && dboAnimal.getVal(Tc.ANIMAL_CLASS) != null
				&& dboAnimal.getVal(Tc.ANIMAL_RACE) != null) {
			DbSearchCriterion cr1 = new DbSearchCriterion(Tc.ANIMAL_CLASS, DbCompareOperand.EQUAL,
					dboAnimal.getVal(Tc.ANIMAL_CLASS));
			DbSearchCriterion cr2 = new DbSearchCriterion(Tc.ANIMAL_RACE, DbCompareOperand.EQUAL,
					dboAnimal.getVal(Tc.ANIMAL_RACE));
			DbSearchExpression dbse = new DbSearchExpression();
			dbse.addDbSearchItem(cr1).addDbSearchItem(cr2);
			getAnimalTypes = svr.getObjects(dbse, SvReader.getTypeIdByName(Tc.ANIMAL_TYPE), null, 0, 0);
			if (!getAnimalTypes.getItems().isEmpty()) {
				currentAniType = getAnimalTypes.get(0);
				linkBetweenAnimalTypeAndDisease = SvLink.getLinkType(Tc.ANI_TYPE_DISEASE,
						SvReader.getTypeIdByName(Tc.ANIMAL_TYPE), SvReader.getTypeIdByName(Tc.DISEASE));
				getLinkedDiseases = svr.getObjectsByLinkedId(currentAniType.getObject_id(),
						SvReader.getTypeIdByName(Tc.ANIMAL_TYPE), linkBetweenAnimalTypeAndDisease,
						SvReader.getTypeIdByName(Tc.DISEASE), false, null, 0, 0);
			}
		}
		return getLinkedDiseases;
	}

	/**
	 * Method that returns disease/s (separated by comma) per Animal.
	 * 
	 * @param dboAnimal
	 * @param rdr
	 * @param svr
	 * @return
	 * @throws SvException
	 */
	public String getDiseaseasPerAnimalInMultiSelectDropDownFormat(DbDataObject dboAnimal, Reader rdr, SvReader svr)
			throws SvException {
		String result = Tc.EMPTY_STRING;
		StringBuilder sb = new StringBuilder();
		HashSet<String> hsDiseases = null;
		DbDataArray arrLabSamples = null;
		if (dboAnimal != null) {
			arrLabSamples = getLabSamplesPerAnimal(dboAnimal, svr);
			if (arrLabSamples != null && !arrLabSamples.getItems().isEmpty()) {
				for (DbDataObject dboLabSample : arrLabSamples.getItems()) {
					DbDataArray arrLabTestResult = getLabTestResultsPerLabSample(dboLabSample, svr);
					if (arrLabTestResult != null && !arrLabTestResult.getItems().isEmpty()) {
						hsDiseases = new HashSet<>();
						for (DbDataObject dboLabTestResut : arrLabTestResult.getItems()) {
							if (dboLabTestResut != null && dboLabTestResut.getVal(Tc.SAMPLE_DISEASE) != null
									&& dboLabTestResut.getVal(Tc.TEST_RESULT) != null
									&& dboLabTestResut.getVal(Tc.TEST_RESULT).toString().equals("0")) {
								hsDiseases.add(dboLabTestResut.getVal(Tc.SAMPLE_DISEASE).toString());
							}
						}
					}
				}
			}
			if (hsDiseases != null && !hsDiseases.isEmpty()) {
				for (String disease : hsDiseases) {
					sb.append(disease).append(",");
				}
				result = sb.substring(0, sb.length() - 1);
			}
		}
		return result;
	}

	public DbDataArray getAppropriateDiseasePerCampaignActivityType(String codeItemValue, SvReader svr)
			throws SvException {
		DbDataArray result = null;
		if (codeItemValue != null && codeItemValue.trim().length() > 0) {
			result = searchForDependentDiseaseByCapmaignType(codeItemValue, svr);
		}
		return result;
	}

	public DbDataObject getDiseaseObjByDiseaseName(String diseaseName, SvReader svr) throws SvException {
		DbDataObject dboDisease = null;
		DbSearchCriterion cr1 = new DbSearchCriterion(Tc.DISEASE_NAME, DbCompareOperand.EQUAL, diseaseName);
		DbDataArray arr = svr.getObjects(new DbSearchExpression().addDbSearchItem(cr1),
				SvReader.getTypeIdByName(Tc.DISEASE), null, 0, 0);
		if (arr != null && !arr.getItems().isEmpty()) {
			dboDisease = arr.get(0);
		}
		return dboDisease;
	}

	// TRANSFER METHODS

	/**
	 * Method that returns last Transfer object of certain Inventory item that
	 * belonged to
	 * 
	 * @param parentId     Inventory item parent_id
	 * @param tagType      Tag type of the Inventory item
	 * @param earTagNumber Ear tag number of the Inventory item
	 * @param status       Status of the Transfer
	 * @param svr          SvReader instance
	 * @return
	 * @throws SvException
	 */
	public DbDataObject getTransferAccordingInventoryItem(Long parentId, String tagType, String earTagNumber,
			String status, SvReader svr) throws SvException {
		return getTransferAccordingInventoryItem(parentId, tagType, earTagNumber, Tc.DEFAULT, status, svr);
	}

	public DbDataObject getTransferAccordingInventoryItem(Long parentId, String tagType, String earTagNumber,
			String transferType, String status, SvReader svr) throws SvException {
		DbDataObject dboTransfer = null;
		DbDataArray arrTransfers = new DbDataArray();
		DbSearchCriterion cr1 = null;
		if (transferType != null) {
			cr1 = new DbSearchCriterion(Tc.DESTINATION_OBJ_ID, DbCompareOperand.EQUAL, String.valueOf(parentId));
		} else {
			cr1 = new DbSearchCriterion(Tc.PARENT_ID, DbCompareOperand.EQUAL, parentId);
		}

		DbSearchCriterion cr2 = new DbSearchCriterion(Tc.TAG_TYPE, DbCompareOperand.EQUAL, tagType);
		DbSearchExpression dbse = new DbSearchExpression();
		dbse.addDbSearchItem(cr1).addDbSearchItem(cr2);

		if (status != null) {
			DbSearchCriterion cr3 = new DbSearchCriterion(Tc.STATUS, DbCompareOperand.EQUAL, status);
			dbse.addDbSearchItem(cr3);
		}
		if (transferType != null) {
			DbSearchCriterion cr4 = new DbSearchCriterion(Tc.TRANSFER_TYPE, DbCompareOperand.EQUAL, transferType);
			dbse.addDbSearchItem(cr4);
		}

		DbSearchCriterion cr5 = new DbSearchCriterion(Tc.START_TAG_ID, DbCompareOperand.LESS_EQUAL,
				Long.valueOf(earTagNumber));
		cr5.setNextCritOperand(Tc.OR);
		DbSearchCriterion cr6 = new DbSearchCriterion(Tc.START_TAG_ID, DbCompareOperand.LESS_EQUAL,
				Long.valueOf(earTagNumber));

		DbSearchExpression dbse1 = new DbSearchExpression();
		dbse1.addDbSearchItem(cr5).addDbSearchItem(cr6);

		DbSearchCriterion cr7 = new DbSearchCriterion(Tc.END_TAG_ID, DbCompareOperand.GREATER_EQUAL,
				Long.valueOf(earTagNumber));
		cr7.setNextCritOperand(Tc.OR);
		DbSearchCriterion cr8 = new DbSearchCriterion(Tc.END_TAG_ID, DbCompareOperand.GREATER_EQUAL,
				Long.valueOf(earTagNumber));

		DbSearchExpression dbse2 = new DbSearchExpression();
		dbse2.addDbSearchItem(cr7).addDbSearchItem(cr8);

		DbSearchExpression dbse3 = new DbSearchExpression();
		dbse3.addDbSearchItem(dbse).addDbSearchItem(dbse1).addDbSearchItem(dbse2);

		arrTransfers = svr.getObjects(dbse3, SvReader.getTypeIdByName(Tc.TRANSFER), null, 0, 0);
		if (arrTransfers.getItems().isEmpty()) {
			arrTransfers = svr.getObjects(dbse3, SvReader.getTypeIdByName(Tc.TRANSFER), new DateTime(), 0, 0);
		}
		if (!arrTransfers.getItems().isEmpty()) {
			dboTransfer = arrTransfers.get(arrTransfers.size() - 1);
		}
		return dboTransfer;
	}

	public DbDataArray getOverlappingRanges(String tableName, Long parent_Id, String tag_type, Long start_tag_id,
			Long end_tag_id, SvReader svr) throws SvException {
		return getOverlappingRanges(tableName, parent_Id, tag_type, start_tag_id, end_tag_id, true, svr);
	}

	public DbDataArray getOverlappingRanges(String tableName, Long parent_Id, String tag_type, Long start_tag_id,
			Long end_tag_id, Boolean useCache, SvReader svr) throws SvException {
		DateTime dtNow = null;
		if (!useCache) {
			dtNow = new DateTime();
		}
		DbDataArray overlappingRanges;
		DbSearchCriterion cr1 = new DbSearchCriterion(Tc.TAG_TYPE, DbCompareOperand.EQUAL, tag_type);
		DbSearchCriterion cr21 = new DbSearchCriterion(Tc.START_TAG_ID, DbCompareOperand.LESS_EQUAL, start_tag_id);
		// cr21.setNextCritOperand(Tc.OR);
		DbSearchCriterion cr22 = new DbSearchCriterion(Tc.START_TAG_ID, DbCompareOperand.LESS_EQUAL, end_tag_id);
		DbSearchExpression dbse1 = new DbSearchExpression();
		dbse1.addDbSearchItem(cr21);
		dbse1.addDbSearchItem(cr21).addDbSearchItem(cr22);

		DbSearchCriterion cr31 = new DbSearchCriterion(Tc.END_TAG_ID, DbCompareOperand.GREATER_EQUAL, start_tag_id);
		cr31.setNextCritOperand(Tc.OR);
		DbSearchCriterion cr32 = new DbSearchCriterion(Tc.END_TAG_ID, DbCompareOperand.GREATER_EQUAL, end_tag_id);
		DbSearchExpression dbse2 = new DbSearchExpression();
		dbse2.addDbSearchItem(cr31).addDbSearchItem(cr32);

		DbSearchExpression dbSearchExp = new DbSearchExpression();
		if (tableName.equals(Tc.TRANSFER)) {
			DbSearchCriterion cr4 = new DbSearchCriterion(Tc.PARENT_ID, DbCompareOperand.EQUAL, parent_Id);
			DbSearchCriterion cr5 = new DbSearchCriterion(Tc.STATUS, DbCompareOperand.NOTEQUAL, Tc.RELEASED);
			DbSearchCriterion cr6 = new DbSearchCriterion(Tc.STATUS, DbCompareOperand.NOTEQUAL, Tc.CANCELED);
			dbSearchExp.addDbSearchItem(cr4).addDbSearchItem(cr5).addDbSearchItem(cr6);
		}
		dbSearchExp.addDbSearchItem(cr1).addDbSearchItem(dbse1).addDbSearchItem(dbse2);
		overlappingRanges = svr.getObjects(dbSearchExp, SvReader.getTypeIdByName(tableName), dtNow, 1000, 0);
		return overlappingRanges;
	}

	public DbDataArray getOverlappingRangeInTransfer(Long objectType, Long parent_Id, String tag_type,
			Long start_tag_id, Long end_tag_id, SvReader svr) throws SvException {
		DbSearchCriterion cr1 = new DbSearchCriterion(Tc.TAG_TYPE, DbCompareOperand.EQUAL, tag_type);
		DbSearchCriterion cr4 = new DbSearchCriterion(Tc.PARENT_ID, DbCompareOperand.EQUAL, parent_Id);
		DbSearchCriterion cr5 = new DbSearchCriterion(Tc.STATUS, DbCompareOperand.NOTEQUAL, Tc.RELEASED);
		DbSearchCriterion cr6 = new DbSearchCriterion(Tc.STATUS, DbCompareOperand.NOTEQUAL, Tc.CANCELED);
		DbSearchCriterion cr21 = new DbSearchCriterion(Tc.START_TAG_ID, DbCompareOperand.LESS_EQUAL, start_tag_id);
		cr21.setNextCritOperand(Tc.OR);
		DbSearchCriterion cr22 = new DbSearchCriterion(Tc.START_TAG_ID, DbCompareOperand.LESS_EQUAL, end_tag_id);
		DbSearchExpression dbse1 = new DbSearchExpression();
		dbse1.addDbSearchItem(cr21).addDbSearchItem(cr22);

		DbSearchCriterion cr31 = new DbSearchCriterion(Tc.END_TAG_ID, DbCompareOperand.GREATER_EQUAL, start_tag_id);
		cr31.setNextCritOperand(Tc.OR);
		DbSearchCriterion cr32 = new DbSearchCriterion(Tc.END_TAG_ID, DbCompareOperand.GREATER_EQUAL, end_tag_id);
		DbSearchExpression dbse2 = new DbSearchExpression();
		dbse2.addDbSearchItem(cr31).addDbSearchItem(cr32);

		DbSearchExpression dbSearchExp = new DbSearchExpression();
		dbSearchExp.addDbSearchItem(cr1).addDbSearchItem(cr4).addDbSearchItem(cr5).addDbSearchItem(cr6)
				.addDbSearchItem(dbse1).addDbSearchItem(dbse2);
		return svr.getObjects(dbSearchExp, objectType, null, 1, 0);
	}

	// ORG_UNIT METHODS

	/**
	 * Method that returns parent org units per org unit
	 * 
	 * @param dbArr
	 * @param dboOrgUnit
	 * @param svr
	 * @return
	 * @throws Exception
	 * @throws SvException
	 */
	public DbDataArray getAppropriateParentOrgUnits(DbDataArray dbArr, DbDataObject dboOrgUnit, SvReader svr)
			throws Exception, SvException {
		if (dbArr.getItems().isEmpty()) {
			dbArr.addDataItem(dboOrgUnit);
		}
		if (dboOrgUnit.getVal(Tc.PARENT_OU_ID) != null) {
			DbDataObject dboParentOrgUnit = svr.getObjectById(
					Long.valueOf(dboOrgUnit.getVal(Tc.PARENT_OU_ID).toString()), svCONST.OBJECT_TYPE_ORG_UNITS, null);
			if (dboParentOrgUnit != null && dboParentOrgUnit.getVal(Tc.NAME) != null
					&& !dboParentOrgUnit.getVal(Tc.NAME).toString().equalsIgnoreCase("HEADQUARTER")) {
				dbArr.addDataItem(dboParentOrgUnit);
				return getAppropriateParentOrgUnits(dbArr, dboParentOrgUnit, svr);
			}
		}
		return dbArr;
	}

	/**
	 * Method that returns sub Org Unit
	 * 
	 * @param dbArr
	 * @param dboOrgUnit
	 * @param svr
	 * @return
	 * @throws Exception
	 * @throws SvException
	 */
	public DbDataArray getOrgUntByParentOuId(Long parentOuId, SvReader svr) throws Exception, SvException {
		return searchForObjectWithSingleFilter(svCONST.OBJECT_TYPE_ORG_UNITS, Tc.PARENT_OU_ID, parentOuId, svr);
	}

	/**
	 * Method that returns initial linked org unit and all sub org units linked to
	 * certain user
	 * 
	 * @param dboUser    User DbDataObject
	 * @param dboOrgUnit Selected/Initial Org Unit DbDataObject
	 * @param svr        SvReader instance
	 * @return
	 * @throws SvException
	 */
	public DbDataArray getAppropriateSubOrgUnits(DbDataObject dboUser, DbDataObject dboOrgUnit, SvReader svr)
			throws SvException {
		DbDataArray arrSubOrgUnits = new DbDataArray();
		if (dboOrgUnit.getVal(Tc.EXTERNAL_ID) != null) {
			DbDataObject dbLinkBetweenUserAndOrgUnit = SvReader.getLinkType(Tc.POA, svCONST.OBJECT_TYPE_USER,
					svCONST.OBJECT_TYPE_ORG_UNITS);
			DbDataArray arrLinkedOrgUnitsPerUser = svr.getObjectsByLinkedId(dboUser.getObject_id(),
					dboUser.getObject_type(), dbLinkBetweenUserAndOrgUnit, svCONST.OBJECT_TYPE_ORG_UNITS, false,
					new DateTime(), 0, 0);
			if (!arrLinkedOrgUnitsPerUser.getItems().isEmpty()) {
				for (DbDataObject tempOrgUnit : arrLinkedOrgUnitsPerUser.getItems()) {
					if (tempOrgUnit.getVal(Tc.EXTERNAL_ID) != null && tempOrgUnit.getVal(Tc.EXTERNAL_ID).toString()
							.startsWith(dboOrgUnit.getVal(Tc.EXTERNAL_ID).toString())) {
						arrSubOrgUnits.addDataItem(tempOrgUnit);
					}
				}
			}
		} else if (dboOrgUnit.getVal(Tc.NAME) != null && dboOrgUnit.getVal(Tc.NAME).toString().equals(Tc.HEADQUARTER)) {
			arrSubOrgUnits.addDataItem(dboOrgUnit);
		}
		return arrSubOrgUnits;
	}

	public DbDataObject getOrgUnitByExternalId(Long externalId, SvReader svr) throws SvException {
		DbDataObject dboOrgUnit = null;
		DbSearchCriterion cr1 = new DbSearchCriterion(Tc.EXTERNAL_ID, DbCompareOperand.EQUAL, externalId);
		DbDataArray result = svr.getObjects(new DbSearchExpression().addDbSearchItem(cr1),
				svCONST.OBJECT_TYPE_ORG_UNITS, null, 0, 0);
		if (!result.getItems().isEmpty()) {
			dboOrgUnit = result.get(0);
		}
		return dboOrgUnit;
	}

	/**
	 * Method that returns SdiUnit object_id by UNIT_ID
	 * 
	 * @param externalUnitId UNIT_ID of the unit
	 * @param unitClass      class of the unit
	 * @param svc            SvCore instance
	 * @throws SvException
	 * @return Long- object_id of the unit
	 * 
	 */
	public Long findAppropriateSdiUnitByUnitId(String externalUnitId, String unitClass, SvCore svc) {
		SvReader svr = null;
		Long result = null;
		try {
			svr = new SvReader(svc);
			DbSearchCriterion cr1 = new DbSearchCriterion("UNIT_ID", DbCompareOperand.EQUAL, externalUnitId);
			DbSearchCriterion cr2 = null;
			switch (unitClass) {
			case "REGION":
				cr2 = new DbSearchCriterion(Tc.UNIT_CLASS, DbCompareOperand.EQUAL, "1");
				break;
			case "MUNICIPALITY":
				cr2 = new DbSearchCriterion(Tc.UNIT_CLASS, DbCompareOperand.EQUAL, "2");
				break;
			case "VILLAGE":
				cr2 = new DbSearchCriterion(Tc.UNIT_CLASS, DbCompareOperand.EQUAL, "3");
				break;
			default:
				break;
			}
			DbDataArray dbArray = svr.getObjects(new DbSearchExpression().addDbSearchItem(cr1).addDbSearchItem(cr2),
					svCONST.OBJECT_TYPE_SDI_UNITS, null, 0, 0);
			if (dbArray != null && !dbArray.getItems().isEmpty() && dbArray.getItems().get(0).getObject_id() != null) {
				result = dbArray.getItems().get(0).getObject_id();
				log4j.trace("SDI_UNIT with unit_id code: " + externalUnitId + " already exists");
			}
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage(), e);
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
		return result;
	}

	/**
	 * Method for fetching all municipalities
	 * 
	 * @param svr SvReader instance
	 * @return DbDataArray
	 */
	public DbDataArray getMunicipalities(SvReader svr) {
		DbDataArray result = new DbDataArray();
		try {
			DbSearchExpression dbse = new DbSearchExpression();
			DbSearchCriterion sc1 = new DbSearchCriterion(Tc.UNIT_CLASS, DbCompareOperand.EQUAL, "2");
			dbse.addDbSearchItem(sc1);

			result = svr.getObjects(dbse, svCONST.OBJECT_TYPE_SDI_UNITS, null, 0, 0);
		} catch (Exception e) {
			log4j.error("Failed fetching municipalitiers with: " + e.getMessage());
		}
		return result;
	}

	public DbDataArray getOrgUnitDependOnParentExternalId(Long betweenStart, Long betweenEnd, SvReader svr)
			throws SvException {
		DbSearchCriterion cr1 = new DbSearchCriterion(Tc.EXTERNAL_ID, DbCompareOperand.BETWEEN, betweenStart,
				betweenEnd);
		DbDataArray result = svr.getObjects(new DbSearchExpression().addDbSearchItem(cr1),
				svCONST.OBJECT_TYPE_ORG_UNITS, null, 0, 0);
		return result;
	}

	// PET METHODTS

	/**
	 * Method for getting pet Archive ID. Only euthanized pets have Archive ID
	 * 
	 * @param dboPet
	 * @param svr
	 * @return
	 */
	public String getParamPetArchiveId(DbDataObject dboPet, SvReader svr) {
		String result = null;
		SvParameter svp = null;
		try {
			svp = new SvParameter(svr);
			result = svp.getParamString(dboPet, "param.pet_archive_id");
		} catch (SvException e) {
			log4j.error(e);
		} finally {
			if (svp != null)
				svp.release();
		}
		return result;
	}

	public LinkedHashMap<String, String> generatePetSummaryInfo(Long objectId, SvReader svr)
			throws SvException, ParseException {
		LinkedHashMap<String, String> jsonOrderedMap = new LinkedHashMap<String, String>();
		Reader rdr = new Reader();
		DbDataObject dboPet = svr.getObjectById(objectId, SvReader.getTypeIdByName(Tc.PET), null);
		DbDataObject dboHolding = svr.getObjectById(dboPet.getParent_id(), SvReader.getTypeIdByName(Tc.HOLDING), null);
		String ownerFullName = getPetOwnerFullName(dboPet, svr);
		String archiveNumber = dboPet.getVal(Tc.ARCHIVE_NUMBER) != null ? dboPet.getVal(Tc.ARCHIVE_NUMBER).toString()
				: null;
		DbDataObject dboLink = null;
		DbDataObject dboOwner = rdr.getPetOwner(dboPet, svr);
		if (dboOwner != null) {
			dboLink = rdr.getLinkObjectBetweenTwoLinkedObjects(dboPet, dboOwner, Tc.PET_OWNER, svr);
		}
		jsonOrderedMap.put("naits.pet.petAge", calcAnimalAge(objectId, Tc.PET, svr));
		if (archiveNumber != null) {
			jsonOrderedMap.put("naits.pet.petArchiveId", archiveNumber);
		}
		if (dboPet.getVal(Tc.DT_EUTHANASIA) != null) {
			jsonOrderedMap.put("naits.pet.dateOfEuthanasia", dboPet.getVal(Tc.DT_EUTHANASIA).toString());
		}
		jsonOrderedMap.put("naits.summary_info.has_owner",
				ownerFullName.equals("") ? I18n.getText(svr.getUserLocaleId(svr.getInstanceUser()), "en.nope")
						: I18n.getText(svr.getUserLocaleId(svr.getInstanceUser()), "en.yes"));
		if (!ownerFullName.equals("")) {
			jsonOrderedMap.put("naits.summary_info.owner_fullname", ownerFullName);
		}
		if (dboLink != null) {
			Date date = new SimpleDateFormat(Tc.DATE_PATTERN).parse(dboLink.getDt_insert().toString());
			String formattedDate = new SimpleDateFormat(Tc.DATE_PATTERN_2).format(date);
			jsonOrderedMap.put("naits.summary_info.owner_valid_from", formattedDate);
		}
		if (dboHolding != null) {
			jsonOrderedMap.put("naits.holding.pic",
					((dboHolding.getVal(Tc.PIC) != null) ? dboHolding.getVal(Tc.PIC).toString() : "N/A"));
		} else {
			jsonOrderedMap.put("naits.holding.pic", Tc.NOT_AVAILABLE_NA);
		}
		return jsonOrderedMap;
	}

	public DbDataObject getPetByPetId(String petId, SvReader svr) throws SvException {
		return getPetByPetIdAndPetType(Tc.PET_TAG_ID, petId, null, true, svr);
	}

	/*
	 * public DbDataObject getPetByPetId(String petId, SvReader svr) throws
	 * SvException { return getPetByPetIdAndPetType(Tc.PET_TAG_ID, petId, null,
	 * true, svr); }
	 */

	public DbDataObject getPetByPetIdAndPetType(String columnName, String petId, String petType, Boolean useCache,
			SvReader svr) throws SvException {
		DbDataObject result = null;
		DateTime dtNow = null;
		if (!useCache) {
			dtNow = new DateTime();
		}
		DbSearchCriterion cr1 = new DbSearchCriterion(columnName, DbCompareOperand.EQUAL, petId);
		DbSearchExpression dbs = new DbSearchExpression().addDbSearchItem(cr1);
		if (petType != null) {
			DbSearchCriterion cr2 = new DbSearchCriterion(Tc.PET_TYPE, DbCompareOperand.EQUAL, petType);
			dbs.addDbSearchItem(cr2);
		}
		DbDataArray dbArray = svr.getObjects(dbs, SvReader.getTypeIdByName(Tc.PET), dtNow, 0, 0);
		if (dbArray != null && !dbArray.getItems().isEmpty()) {
			result = dbArray.getItems().get(0);
		}
		return result;
	}

	public DbDataObject getPetOwner(DbDataObject dboPet, SvReader svr) throws SvException {
		DbDataObject dboOwner = null;
		DbDataObject dboLinkBetweenPetAndOwner = SvReader.getLinkType(Tc.PET_OWNER, dboPet.getObject_type(),
				SvReader.getTypeIdByName(Tc.HOLDING_RESPONSIBLE));
		DbDataArray arrResult = svr.getObjectsByLinkedId(dboPet.getObject_id(), dboPet.getObject_type(),
				dboLinkBetweenPetAndOwner, SvReader.getTypeIdByName(Tc.HOLDING_RESPONSIBLE), false, null, 0, 0);
		if (!arrResult.getItems().isEmpty()) {
			dboOwner = arrResult.get(0);
		}
		return dboOwner;
	}

	public DbDataArray getPetsByDboOwner(DbDataObject dboOwner, SvReader svr) throws SvException {
		DbDataObject dboLinkBetweenPetAndOwner = SvReader.getLinkType(Tc.PET_OWNER, SvReader.getTypeIdByName(Tc.PET),
				SvReader.getTypeIdByName(Tc.HOLDING_RESPONSIBLE));
		return svr.getObjectsByLinkedId(dboOwner.getObject_id(), dboOwner.getObject_type(), dboLinkBetweenPetAndOwner,
				SvReader.getTypeIdByName(Tc.PET), true, null, 0, 0);
	}

	public DbDataArray getPetContacts(DbDataObject dboPet, SvReader svr) throws SvException {
		DbDataObject dboLinkBetweenPetAndOwner = SvReader.getLinkType(Tc.PET_CONTACT, dboPet.getObject_type(),
				SvReader.getTypeIdByName(Tc.HOLDING_RESPONSIBLE));
		DbDataArray arrContacts = svr.getObjectsByLinkedId(dboPet.getObject_id(), dboPet.getObject_type(),
				dboLinkBetweenPetAndOwner, SvReader.getTypeIdByName(Tc.HOLDING_RESPONSIBLE), false, null, 0, 0);
		return arrContacts;
	}

	public DbDataArray getStrayPetCaretaker(DbDataObject dboStrayPet, SvReader svr) throws SvException {
		DbDataObject dboLinkBetweenPetAndOwner = SvReader.getLinkType(Tc.STRAY_CARETAKER, dboStrayPet.getObject_type(),
				SvReader.getTypeIdByName(Tc.HOLDING_RESPONSIBLE));
		DbDataArray arrContacts = svr.getObjectsByLinkedId(dboStrayPet.getObject_id(), dboStrayPet.getObject_type(),
				dboLinkBetweenPetAndOwner, SvReader.getTypeIdByName(Tc.HOLDING_RESPONSIBLE), false, null, 0, 0);
		return arrContacts;
	}

	public String getPetOwnerFullName(DbDataObject dboPet, SvReader svr) throws SvException {
		String result = Tc.EMPTY_STRING;
		DbDataObject dboOwner = getPetOwner(dboPet, svr);
		if (dboOwner != null && dboOwner.getVal(Tc.FULL_NAME) != null) {
			result = dboOwner.getVal(Tc.FULL_NAME).toString();
		}
		return result;
	}

	/**
	 * Method for fetching PET_MOVEMENT objects.
	 * 
	 * @param dboPet
	 * @param arrInStatus ArrayList with statuses we want to check (expects always
	 *                    to have elements)
	 * @param svr
	 * @return
	 * @throws SvException
	 */
	public DbDataObject getPetMovement(DbDataObject dboPet, ArrayList<String> arrInStatus, SvReader svr)
			throws SvException {
		DbDataObject dboPetMovement = null;
		DbSearchExpression dbse0 = new DbSearchExpression();
		DbSearchExpression dbse1 = new DbSearchExpression();
		DbSearchCriterion cr1 = new DbSearchCriterion(Tc.PARENT_ID, DbCompareOperand.EQUAL, dboPet.getObject_id());
		dbse0.addDbSearchItem(cr1);
		for (String movementStatus : arrInStatus) {
			DbSearchCriterion cr2 = new DbSearchCriterion(Tc.STATUS, DbCompareOperand.EQUAL, movementStatus);
			cr2.setNextCritOperand(Tc.OR);
			dbse1.addDbSearchItem(cr2);
		}
		DbDataArray arrPetMovements = svr.getObjects(
				new DbSearchExpression().addDbSearchItem(dbse0).addDbSearchItem(dbse1),
				SvReader.getTypeIdByName(Tc.PET_MOVEMENT), null, 0, 0);
		if (!arrPetMovements.getItems().isEmpty()) {
			dboPetMovement = arrPetMovements.get(0);
		}
		return dboPetMovement;
	}

	// HEALTH_PASSPORT

	public DbDataObject getLastValidHealthPassport(DbDataObject dboPet, DateTime refDate, SvReader svr)
			throws SvException {
		DbDataObject dboLastValidHealthPassport = null;
		DbDataArray arrHealthPassports = svr.getObjectsByParentId(dboPet.getObject_id(),
				SvReader.getTypeIdByName(Tc.HEALTH_PASSPORT), null, 0, 0);
		if (!arrHealthPassports.getItems().isEmpty()) {
			for (DbDataObject dboHealthPassport : arrHealthPassports.getItems()) {
				if (dboHealthPassport.getStatus().equals(Tc.VALID)) {
					dboLastValidHealthPassport = dboHealthPassport;
					break;
				}
			}
		}
		return dboLastValidHealthPassport;
	}

	// OTHER METHODS

	/**
	 * Help method that fetch all dependent data for each region/munic/commun
	 * Purpose: to load appropriate list of dependent dropdown
	 * 
	 * @param parentCode:   code_value of parent code list item
	 * @param codeListName: name of the code list, which should be accessed
	 * @throws SvException
	 */
	public DbDataArray searchForDependentMunicCommunVillage(String parentCode, String codeListName, SvReader svr)
			throws SvException {
		DbDataArray itemsFound = null;

		DbSearchExpression srchExpr = new DbSearchExpression();
		DbSearchCriterion filterByParentCodeValue = new DbSearchCriterion(Tc.PARENT_CODE_VALUE, DbCompareOperand.EQUAL,
				codeListName);
		DbSearchCriterion filterByCodeValue = new DbSearchCriterion(Tc.CODE_VALUE, DbCompareOperand.LIKE,
				parentCode + Tc.PERCENT_OPERATOR);
		srchExpr.addDbSearchItem(filterByParentCodeValue).addDbSearchItem(filterByCodeValue);
		DbDataArray searchResult = svr.getObjects(srchExpr, svCONST.OBJECT_TYPE_CODE, null, 0, 0);
		if (!searchResult.getItems().isEmpty()) {
			itemsFound = searchResult;
			String userLocale = svr.getUserLocaleId(svr.getInstanceUser());
			for (DbDataObject tempCodeItemFound : itemsFound.getItems()) {
				String translatedCodeItem = I18n.getText(userLocale,
						tempCodeItemFound.getVal(Tc.LABEL_CODE).toString());
				tempCodeItemFound.setVal(Tc.LBL_TRANSL, translatedCodeItem);
			}
		}
		return itemsFound;
	}

	/**
	 * Method that checks and return if existing form_field_type by label code
	 * 
	 * @param labelCode the label code of the form_field_type object
	 * @param svReader  SvReader instance
	 * @throws SvException
	 * @returns DbDataObject if found some, if no returns null
	 */
	public DbDataObject getFormFieldType(String labelCode, SvReader svReader) throws SvException {

		DbDataObject result = null;

		DbSearchExpression getFormType = new DbSearchExpression();
		DbSearchCriterion filterByLabel = new DbSearchCriterion(Tc.LABEL_CODE, DbCompareOperand.EQUAL, labelCode);
		getFormType.addDbSearchItem(filterByLabel);
		DbDataArray searchResult = svReader.getObjects(getFormType, svCONST.OBJECT_TYPE_FORM_FIELD_TYPE, null, 0, 0);

		if (!searchResult.getItems().isEmpty()) {
			result = searchResult.getItems().get(0);
		}
		return result;
	}

	/**
	 * 
	 * @param svr
	 * @param qDbo
	 * @return
	 * @throws SvException
	 */
	public DbDataArray getHoldingQuarantineLinks(SvReader svr, DbDataObject qDbo) throws SvException {
		DbDataObject linkType = SvLink.getLinkType(Tc.HOLDING_QUARANTINE, SvCore.getTypeIdByName(Tc.HOLDING),
				qDbo.getObject_type());
		DbSearchExpression dbse = new DbSearchExpression();

		DbSearchCriterion sc1 = new DbSearchCriterion(Tc.LINK_TYPE_ID, DbCompareOperand.EQUAL, linkType.getObject_id());
		sc1.setNextCritOperand(DbLogicOperand.AND.toString());
		DbSearchCriterion sc2 = new DbSearchCriterion(Tc.LINK_OBJ_ID_2, DbCompareOperand.EQUAL, qDbo.getObject_id());

		dbse.addDbSearchItem(sc1);
		dbse.addDbSearchItem(sc2);

		return svr.getObjects(dbse, svCONST.OBJECT_TYPE_LINK, null, 0, 0);
	}

	/**
	 * Method that checks if SvLink exists between two DB objects
	 * 
	 * @param dbo1     The first DbDataObject
	 * 
	 * @param dbo2     The second DbDataObject
	 * 
	 * @param linkName name of the link type
	 * 
	 * @param refDate  The reference date on which we want to get the data set
	 * 
	 * @param svr      SvReader instance
	 * 
	 * @throws SvException
	 * @return Boolean
	 */
	public boolean checkIfLinkExists(DbDataObject dbo1, DbDataObject dbo2, String linkName, DateTime refDate,
			SvReader svr) throws SvException {
		Boolean result = false;
		DbDataObject dbLink = SvLink.getLinkType(linkName, dbo1.getObject_type(), dbo2.getObject_type());
		DbDataArray allItems = svr.getObjectsByLinkedId(dbo1.getObject_id(), dbLink, refDate, 0, 0);
		for (DbDataObject dbo : allItems.getItems()) {
			if (dbo.getObject_id().equals(dbo2.getObject_id())) {
				result = true;
				break;
			}
		}
		return result;
	}

	/**
	 * Method that returns DbDataArray for specific criteria and object type
	 * 
	 * @param hashMapColumnNameAndValue LinkedHashMap with key:ColumnName and
	 *                                  value:FieldValue
	 * @param objType                   object type
	 * @param svr                       SvReader instance
	 * @return
	 * @throws SvException
	 */
	public DbDataArray getDbDataWithCriteria(LinkedHashMap<String, String> criteriaHashMap, Long objType, SvReader svr)
			throws SvException {
		DbDataArray result = new DbDataArray();
		DbSearchCriterion cr1 = null;
		DbSearchExpression dbse = new DbSearchExpression();
		if (criteriaHashMap != null && !criteriaHashMap.isEmpty()) {
			for (Entry<String, String> a : criteriaHashMap.entrySet()) {
				cr1 = new DbSearchCriterion(a.getKey(), DbCompareOperand.EQUAL, a.getValue());
				dbse.addDbSearchItem(cr1);
			}
			result = svr.getObjects(dbse, objType, null, 0, 0);
		}
		return result;
	}

	/**
	 * Method that returns N size shuffled DbDataArray
	 * 
	 * @param arrayToRandomize array to be randomized
	 * @param resultSampleSize size of the output of shuffled DbDataArray (N)
	 * @param svr              SvReader instance
	 * @return shuffled N size DbDataArray
	 */
	public DbDataArray getNRandomElementsFromDbDataArray(DbDataArray arrayToRandomize, Integer resultSampleSize) {
		DbDataArray result = null;
		if (arrayToRandomize != null && !arrayToRandomize.getItems().isEmpty()) {
			result = new DbDataArray();

			Collections.shuffle(arrayToRandomize.getItems());
			for (int i = 0; i < resultSampleSize; i++) {
				result.addDataItem(arrayToRandomize.getItems().get(i));
			}
		} else {
			log4j.trace("The input array list is null/empty. Nothing to be shuffled.");
		}
		return result;
	}

	public String getOrderTransferStatusPerRange(DbDataObject dboRange, SvReader svr) throws SvException {
		String orderStatus = Tc.EMPTY_STRING;
		DbDataObject dbo = null;
		if (dboRange.getParent_id() != null) {
			dbo = svr.getObjectById(dboRange.getParent_id(), SvReader.getTypeIdByName(Tc.ORDER), null);
			orderStatus = "range_";
			if (dbo == null) {
				dbo = svr.getObjectById(dboRange.getObject_id(), SvReader.getTypeIdByName(Tc.TRANSFER), null);
				orderStatus = "transfer_";
			}
		}
		if (dbo != null)
			orderStatus += dbo.getStatus();
		return orderStatus;
	}

	public String getOrderStatusPerSupplier(DbDataObject dboSupplier, SvReader svr) throws SvException {
		String orderStatus = Tc.EMPTY_STRING;
		if (dboSupplier != null) {
			DbDataObject linkOrderSupplier = SvLink.getLinkType(Tc.SUPPLY, SvReader.getTypeIdByName(Tc.ORDER),
					SvReader.getTypeIdByName(Tc.SUPPLIER));
			DbDataArray allItems = svr.getObjectsByLinkedId(dboSupplier.getObject_id(),
					SvReader.getTypeIdByName(Tc.SUPPLIER), linkOrderSupplier, SvReader.getTypeIdByName(Tc.ORDER), true,
					null, 0, 0);
			if (allItems != null && !allItems.getItems().isEmpty()) {
				orderStatus = allItems.get(0).getStatus();
			}
		}
		return orderStatus;
	}

	public String getGeostatCodeFromOrgUnit(DbDataObject inventoryItemObj, SvReader svr) throws SvException {
		String result = Tc.EMPTY_STRING;
		DbDataObject orgUnitObj = svr.getObjectById(inventoryItemObj.getParent_id(), svCONST.OBJECT_TYPE_ORG_UNITS,
				null);
		if (orgUnitObj != null && orgUnitObj.getVal(Tc.EXTERNAL_ID) != null) {
			result = orgUnitObj.getVal(Tc.EXTERNAL_ID).toString();
		}
		return result;
	}

	/**
	 * Method that gets Link Object if Link exists
	 * 
	 * @param obj_1    Linked Object_Id 1
	 * @param obj_2    Linked Object_Id 2
	 * @param linkType Object type
	 * @param svr
	 * @return DbDataObject - SvLink Object
	 * @throws SvException
	 */
	public DbDataObject getLinkObject(Long obj_1, Long obj_2, Long linkType, SvReader svr) throws SvException {
		return getLinkObject(obj_1, obj_2, linkType, true, svr);
	}

	public DbDataObject getLinkObject(Long obj_1, Long obj_2, Long linkType, Boolean useCache, SvReader svr)
			throws SvException {
		DbDataObject getLink = null;
		DbDataArray resultArr;
		DbSearchCriterion cr1 = new DbSearchCriterion(Tc.LINK_OBJ_ID_1, DbCompareOperand.EQUAL, obj_1);
		DbSearchCriterion cr2 = new DbSearchCriterion(Tc.LINK_OBJ_ID_2, DbCompareOperand.EQUAL, obj_2);
		DbSearchCriterion cr3 = new DbSearchCriterion(Tc.LINK_TYPE_ID, DbCompareOperand.EQUAL, linkType);
		DbSearchExpression dbse = new DbSearchExpression();
		dbse.addDbSearchItem(cr1).addDbSearchItem(cr2).addDbSearchItem(cr3);
		if (useCache) {
			resultArr = svr.getObjects(dbse, svCONST.OBJECT_TYPE_LINK, null, 0, 0);
		} else {
			resultArr = svr.getObjects(dbse, svCONST.OBJECT_TYPE_LINK, new DateTime(), 0, 0);
		}
		if (!resultArr.getItems().isEmpty()) {
			getLink = resultArr.get(0);
		}
		return getLink;
	}

	/**
	 * Method that gets linked objects between ANIMAL and EXPORT_CERTIFICATE
	 * 
	 * @param dboAnimal ANIMAL object
	 * @param svr       SvReader instance
	 * @return
	 * @throws SvException
	 */
	public DbDataObject getLinkBetweenAnimalAndExportCert(DbDataObject dboAnimal, SvReader svr) throws SvException {
		DbDataObject getLinkedExpCert = null;
		DbDataObject getLink = null;
		DbDataObject linkAnimalExportCert = SvLink.getLinkType(Tc.ANIMAL_EXPORT_CERT,
				SvReader.getTypeIdByName(Tc.ANIMAL), SvReader.getTypeIdByName(Tc.EXPORT_CERT));
		DbDataArray allItems = svr.getObjectsByLinkedId(dboAnimal.getObject_id(), linkAnimalExportCert, null, 0, 0);
		if (allItems != null && !allItems.getItems().isEmpty()) {
			getLinkedExpCert = allItems.get(0);
			getLink = getLinkObject(dboAnimal.getObject_id(), getLinkedExpCert.getObject_id(),
					linkAnimalExportCert.getObject_id(), svr);// ANIMAL_EXPORT_CERT
		}
		return getLink;
	}

	/**
	 * Method that is used for dependency drop down i.e, returns villages depend on
	 * previous selected Munic/Commun/Region code
	 * 
	 * @param codeItemValue Region/Commun/Munic
	 * @param svr           SvReader instance
	 * @return
	 * @throws SvException
	 */
	public DbDataArray getDependentMunicComunVillage(String codeItemValue, SvReader svr) throws SvException {
		DbDataArray result = null;
		String codeListName = Tc.EMPTY_STRING;
		if (codeItemValue != null && codeItemValue.trim().length() > 0) {
			if (!(codeItemValue.length() == 2 || codeItemValue.length() == 4 || codeItemValue.length() == 6)
					&& codeItemValue.length() != 8)
			// added case && codeItemValue.length() != 8 since the dep.dropdown
			// is not aware when the last
			// note is being reached and on each village selection it was
			// filling the log with the next logerror
			{
				log4j.error("naits.error.getVillageDependentDropdown.invalidInputParam" + codeItemValue);
			}
			if (codeItemValue.length() == 2) {
				codeListName = Tc.MUNICIPALITIES;
			}
			if (codeItemValue.length() == 4) {
				codeListName = Tc.COMMUNITIES;
			}
			if (codeItemValue.length() == 6) {
				codeListName = Tc.VILLAGES;
			}
			result = searchForDependentMunicCommunVillage(codeItemValue, codeListName, svr);
		}
		return result;

	}

	public DbDataArray getLinkedAreaPerPopulation(DbDataObject populationObj, SvReader svr) throws SvException {
		DbDataObject linkBetweenAreaAndPopulation = SvReader.getLinkType(Tc.AREA_POPULATION,
				SvReader.getTypeIdByName(Tc.AREA), populationObj.getObject_type());
		DbDataArray arr = svr.getObjectsByLinkedId(populationObj.getObject_id(), populationObj.getObject_type(),
				linkBetweenAreaAndPopulation, SvReader.getTypeIdByName(Tc.AREA), true, null, 0, 0);
		return arr;
	}

	/**
	 * 
	 * @param objToSearch
	 * @param columnToSearch
	 * @param valueToSearch
	 * @param svReader
	 * @return
	 */
	public DbDataObject searchForObject(long objToSearch, String columnToSearch, String valueToSearch,
			SvReader svReader) {
		DbDataArray foundObjects;
		DbDataObject result = null;
		try {
			DbSearchExpression expr = new DbSearchExpression();
			expr.addDbSearchItem(new DbSearchCriterion(columnToSearch, DbCompareOperand.EQUAL, valueToSearch));
			foundObjects = svReader.getObjects(expr, objToSearch, null, 0, 0);
			if (!foundObjects.getItems().isEmpty()) {
				result = foundObjects.get(0);
			}
		} catch (SvException ex) {
			log4j.error("Error in searchForObject: " + ex.getFormattedMessage());
		}
		return result;
	}

	public DbDataArray searchForObjectWithSingleFilter(long objToSearch, String columnToSearch, Object valueToSearch,
			SvReader svr) {
		return searchForObjectWithSingleFilter(objToSearch, columnToSearch, valueToSearch, true, svr);
	}

	public DbDataArray searchForObjectWithSingleFilter(long objToSearch, String columnToSearch, Object valueToSearch,
			Boolean useCache, SvReader svr) {
		DbDataArray foundObjects = new DbDataArray();
		DateTime dtNow = null;
		if (!useCache) {
			dtNow = new DateTime();
		}
		try {
			DbSearchExpression expr = new DbSearchExpression();
			expr.addDbSearchItem(new DbSearchCriterion(columnToSearch, DbCompareOperand.EQUAL, valueToSearch));
			foundObjects = svr.getObjects(expr, objToSearch, dtNow, 0, 0);
		} catch (SvException ex) {
			log4j.error("Error in searchForObject: " + ex.getFormattedMessage());
		}
		return foundObjects;
	}

	/**
	 * Method for getting multi-select field value in List.
	 * 
	 * @param dboObject instance of the object we want to use
	 * @param fieldName name of multi-select field
	 * @return List <String>
	 */
	public List<String> getMultiSelectFieldValueAsList(DbDataObject dbo, String fieldName) {
		List<String> multiSelectResult = new ArrayList<>();
		if (dbo.getVal(fieldName) != null && !dbo.getVal(fieldName).toString().equals("")) {
			String multiValue = dbo.getVal(fieldName).toString();
			multiSelectResult = new ArrayList<String>(Arrays.asList(multiValue.split(",")));
		}
		return multiSelectResult;
	}

	/**
	 * Custom date formatter
	 * 
	 * @param date Date in string format (YYYY-MM-DD)
	 * @return Date in string format (DD.MM.YYYY)
	 */
	public String customDateFormatter(String date) {
		String[] dateForm = date.split("-");
		String result = date;
		if (date != null && !date.equals("")) {
			result = new String();
			String temp = dateForm[dateForm.length - 1];
			dateForm[dateForm.length - 1] = dateForm[0];
			dateForm[0] = temp;

			for (int i = 0; i < dateForm.length; i++) {
				result += dateForm[i];
				if (i != dateForm.length - 1) {
					result += ".";
				}
			}
		}
		return result;
	}

	public Integer getDayDiffBetweenDates(String date1, String date2) {
		Integer result = 0;
		String pattern = Tc.DATE_PATTERN;
		DateTime dateOfFirst = DateTime.parse(date1, DateTimeFormat.forPattern(pattern));
		DateTime dateOfSecond = DateTime.parse(date2, DateTimeFormat.forPattern(pattern));
		result = (int) ValidationChecks.getDateDiff(dateOfFirst, dateOfSecond, TimeUnit.DAYS);
		return result;
	}

	public DateTime getDateFromDBField(String dbFieldName, DbDataObject dboToExtractInfoFrom) {
		DateTime result = null;
		if (dboToExtractInfoFrom.getVal(dbFieldName) != null) {
			String pattern = Tc.DATE_PATTERN;
			String departureDate = dboToExtractInfoFrom.getVal(dbFieldName).toString().substring(0, 10);
			result = DateTime.parse(departureDate, DateTimeFormat.forPattern(pattern));
		}
		return result;
	}

	public DbDataArray getExpiredQuarantines(SvReader svr) throws SvException {
		DbDataArray tempResult = null;
		DbDataArray resultQuarantines = new DbDataArray();
		DateTime dtNow = new DateTime();
		DbSearchCriterion cr1 = new DbSearchCriterion(Tc.DATE_TO, DbCompareOperand.LESS_EQUAL, dtNow);
		DbSearchCriterion cr2 = new DbSearchCriterion(Tc.QUARANTINE_TYPE, DbCompareOperand.EQUAL, "0");
		DbSearchExpression dbse = new DbSearchExpression();
		dbse.addDbSearchItem(cr1).addDbSearchItem(cr2);
		tempResult = svr.getObjects(dbse, SvReader.getTypeIdByName(Tc.QUARANTINE), null, 0, 0);
		for (DbDataObject dboQuarantine : tempResult.getItems()) {
			String dateTo = dboQuarantine.getVal(Tc.DATE_TO).toString();
			DateTime quarantineDateTo = new DateTime(dateTo);
			Integer result = (int) ValidationChecks.getDateDiff(dtNow, quarantineDateTo, TimeUnit.HOURS);
			if (result > 72) {
				resultQuarantines.addDataItem(dboQuarantine);
			}
		}
		return resultQuarantines;
	}

	public DbDataArray getExportCertificatesOfExpiredQuarantines(SvReader svr) throws SvException {
		DbDataArray arrExportCertificates = new DbDataArray();
		DbDataArray arrQuarantines = getExpiredQuarantines(svr);
		if (arrQuarantines != null && !arrQuarantines.getItems().isEmpty()) {
			for (DbDataObject dboQuarantine : arrQuarantines.getItems()) {
				DbDataArray arrExportCertificatesPerQuarantine = svr.getObjectsByParentId(dboQuarantine.getObject_id(),
						SvReader.getTypeIdByName(Tc.EXPORT_CERT), null, 0, 0);
				if (arrExportCertificatesPerQuarantine != null
						&& !arrExportCertificatesPerQuarantine.getItems().isEmpty()) {
					for (DbDataObject dboExportCert : arrExportCertificatesPerQuarantine.getItems()) {
						if (dboExportCert.getStatus().equals(Tc.VALID)) {
							arrExportCertificates.addDataItem(dboExportCert);
						}
					}
				}
			}
		}
		return arrExportCertificates;
	}

	public Integer toAbsoluteValue(Integer num) {
		Integer convertedNum = num;
		String numberToString = String.valueOf(num);
		if (numberToString.contains("-")) {
			String tempAbsoulteValue = numberToString.replace("-", "");
			convertedNum = Integer.valueOf(tempAbsoulteValue.trim());
		}
		return convertedNum;
	}

	// example: DATE_OF_ACTION, NOW()-6MONTHS, NOW()
	/**
	 * 
	 * Method that is used as filter i.e, it gets all objects of given array in
	 * given time interval (between two dates)
	 * 
	 * @param array_Objects  DbDataArray - Array to be filtered
	 * @param date_Field_Obj String - Name of the date field of the objects in the
	 *                       given array
	 * @param date_1         Date 1 (From)
	 * @param date_2         Date 2 (To)
	 * @param use_As_Chek
	 * 
	 * @return filtered DbDataArray
	 */
	public DbDataArray filterObjectsByDateTimeFrame(DbDataArray arrayObjects, String dateFieldObj, DateTime date1,
			DateTime date2, boolean useAsChek) {
		DbDataArray resultObjects = new DbDataArray();
		DateTime convertedDateField = null;
		if (arrayObjects != null && !arrayObjects.getItems().isEmpty()) {
			arrayObjects.getSortedItems(dateFieldObj);
			for (DbDataObject dboObject : arrayObjects.getItems()) {
				if (dboObject.getVal(dateFieldObj) != null) {
					convertedDateField = new DateTime(dboObject.getVal(dateFieldObj).toString());
					if (convertedDateField.isAfter(date1) && convertedDateField.isBefore(date2)) {
						resultObjects.addDataItem(dboObject);
						if (useAsChek) {
							break;
						}
					}
				}
			}
		}
		return resultObjects;
	}

	/**
	 * 
	 * Method that is used as filter i.e, it gets all objects of given array in
	 * given time interval (between two dates). The default second date (Date to) is
	 * NOW()
	 * 
	 * @param array_Objects  DbDataArray - Array to be filtered
	 * @param date_Field_Obj String - Name of the date field of the objects in the
	 *                       given array
	 * @param date_1         Date 1 (From)
	 * @return filtered DbDataArray
	 */
	public DbDataArray filterObjectsByDateTimeFrame(DbDataArray array_Objects, String date_Field_Obj, DateTime date_1) {
		DateTime date_Now = new DateTime();
		return filterObjectsByDateTimeFrame(array_Objects, date_Field_Obj, date_1, date_Now, false);
	}

	/**
	 * Method that returns object by PKID.
	 * 
	 * @param pkid       PKID of the object
	 * @param objectType Object type
	 * @param svr        SvReader instance
	 * @return
	 * @throws SvException
	 */
	public DbDataObject getObjectByPkid(Long pkid, String objectType, SvReader svr) throws SvException {
		DbDataObject resultObj = new DbDataObject();
		DbSearchCriterion cr1 = new DbSearchCriterion(Tc.PKID, DbCompareOperand.EQUAL, pkid);
		DbSearchExpression dbse = new DbSearchExpression();
		dbse.addDbSearchItem(cr1);
		DbDataArray resultArray = svr.getObjectsHistory(dbse, SvReader.getTypeIdByName(objectType), 0, 0);
		if (resultArray != null && !resultArray.getItems().isEmpty()) {
			resultObj = resultArray.get(0);
		}
		return resultObj;
	}

	public DbDataObject getTestTypeDependOnLabSampleAndTestResult(DbDataObject dboLabSample, DbDataObject dboTestResult,
			SvReader svr) throws SvException {
		DbDataObject dboResult = null;
		DbSearchCriterion cr1 = new DbSearchCriterion(Tc.DISEASE, DbCompareOperand.EQUAL,
				dboTestResult.getVal(Tc.SAMPLE_DISEASE).toString());
		DbSearchCriterion cr2 = new DbSearchCriterion(Tc.TEST_TYPE, DbCompareOperand.EQUAL,
				dboLabSample.getVal(Tc.SAMPLE_TEST_TYPE).toString());
		DbSearchCriterion cr3 = new DbSearchCriterion(Tc.SAMPLE_TYPE, DbCompareOperand.EQUAL,
				dboLabSample.getVal(Tc.SAMPLE_TYPE).toString());
		DbSearchExpression dbse = new DbSearchExpression();
		dbse.addDbSearchItem(cr1).addDbSearchItem(cr2).addDbSearchItem(cr3);
		DbDataArray arr = svr.getObjects(dbse, SvReader.getTypeIdByName(Tc.LAB_TEST_TYPE), null, 0, 0);
		if (arr != null && !arr.getItems().isEmpty()) {
			dboResult = arr.get(0);
		}
		return dboResult;
	}

	/**
	 * Method that returns duplicate/similar notification
	 * 
	 * @param dboNotification
	 * @param svr
	 * @return
	 * @throws SvException
	 */
	public DbDataObject getDuplicateNotification(DbDataObject dboNotification, SvReader svr) throws SvException {
		DbDataObject result = null;
		DbSearchCriterion cr1 = new DbSearchCriterion(Tc.TITLE, DbCompareOperand.EQUAL,
				dboNotification.getVal(Tc.TITLE).toString());
		DbSearchCriterion cr2 = new DbSearchCriterion(Tc.MESSAGE, DbCompareOperand.EQUAL,
				dboNotification.getVal(Tc.MESSAGE).toString());
		DbSearchCriterion cr3 = new DbSearchCriterion(Tc.TYPE, DbCompareOperand.EQUAL,
				dboNotification.getVal(Tc.TYPE).toString());
		DbSearchExpression dbse = new DbSearchExpression();
		dbse.addDbSearchItem(cr1).addDbSearchItem(cr2).addDbSearchItem(cr3);
		if (!dboNotification.getObject_id().equals(0L)) {
			DbSearchCriterion cr4 = new DbSearchCriterion(Tc.OBJECT_ID, DbCompareOperand.NOTEQUAL,
					dboNotification.getObject_id());
			dbse.addDbSearchItem(cr4);
		}
		DbDataArray arr = svr.getObjects(dbse, dboNotification.getObject_type(), null, 0, 0);
		if (arr != null && !arr.getItems().isEmpty()) {
			result = arr.get(0);
		}
		return result;
	}

	/**
	 * 
	 * @param objectType  - type of the object we want to search for geo location
	 * @param geostatCode - geostat code which may have 2 digits for REGION, 4 for
	 *                    MUNIC, 6 for COMMUN, 8 for VILLAGE
	 * @param dbc         - DbCompareOperand param in order to define how precise
	 *                    the search will be (dependent on LIKE/EQUAL)
	 * @param svr         - SvReader instance
	 * @return
	 * @throws SvException
	 */
	public DbDataArray getObjectsByLocation(String objectType, String geostatCode, DbCompareOperand dbc, SvReader svr)
			throws SvException {
		DbDataArray objectsFound = new DbDataArray();
		if (geostatCode.length() == 2) {
			objectsFound = findDataPerSingleFilter(Tc.REGION_CODE, geostatCode, dbc,
					SvReader.getTypeIdByName(objectType), 10000, svr);
		} else if (geostatCode.length() == 4) {
			objectsFound = findDataPerSingleFilter(Tc.MUNIC_CODE, geostatCode, dbc,
					SvReader.getTypeIdByName(objectType), 10000, svr);
		} else if (geostatCode.length() == 6) {
			objectsFound = findDataPerSingleFilter(Tc.COMMUN_CODE, geostatCode, dbc,
					SvReader.getTypeIdByName(objectType), 10000, svr);
		} else if (geostatCode.length() == 8) {
			String columnName = Tc.VILLAGE_CODE;
			if (Tc.LAB_SAMPLE.equals(objectType)) {
				columnName = Tc.GEOSTAT_CODE;
			}
			objectsFound = findDataPerSingleFilter(columnName, geostatCode, dbc, SvReader.getTypeIdByName(objectType),
					10000, svr);
		}
		return objectsFound;
	}

	public DbDataArray getNotesAccordingParentIdAndNoteName(Long parentId, String noteName, SvReader svr)
			throws SvException {
		DbDataArray arrNotes;
		DbSearchCriterion cr1 = new DbSearchCriterion(Tc.PARENT_ID, DbCompareOperand.EQUAL, parentId);
		DbSearchCriterion cr2 = new DbSearchCriterion(Tc.NOTE_NAME, DbCompareOperand.EQUAL, noteName);
		arrNotes = svr.getObjects(new DbSearchExpression().addDbSearchItem(cr1).addDbSearchItem(cr2),
				svCONST.OBJECT_TYPE_NOTES, new DateTime(), 0, 0);
		return arrNotes;
	}

	/**
	 * Method for generating Quarantine id with sequence
	 * 
	 * @param dboQuarantine Quarantine DbDataObject
	 * @param svr           SvReader instance
	 * @return
	 * @throws SvException
	 */
	public String generateQuarantineId(DbDataObject dboQuarantine, SvReader svr) throws SvException {
		SvSequence svs = null;
		String generatedQuarantineId = null;
		String quarantineTypePrefix = "BQ";
		if (dboQuarantine == null)
			throw (new SvException("naits.error.dboQuarantineIsNull", svr.getInstanceUser()));
		if (dboQuarantine.getVal(Tc.QUARANTINE_TYPE) != null
				&& dboQuarantine.getVal(Tc.QUARANTINE_TYPE).toString().equals("0")) {
			quarantineTypePrefix = "EQ";
		}
		try {
			svs = new SvSequence(svr.getSessionId());
			Long seqId = svs.getSeqNextVal(dboQuarantine.getObject_type().toString(), false);
			Thread.sleep(2);
			String quarantineSeq = String.format("%05d", Integer.valueOf(seqId.toString()));
			generatedQuarantineId = quarantineTypePrefix + "-" + quarantineSeq;
			dboQuarantine.setVal(Tc.QUARANTINE_ID, generatedQuarantineId);
			svs.dbCommit();
		} catch (SvException | InterruptedException e) {
			log4j.error(e);
		} finally {
			if (svs != null) {
				svs.release();
			}
		}
		return generatedQuarantineId;
	}

	/**
	 * Method for finding locale id per user, If not set returns default
	 * 
	 * @param svr SvReader instance
	 */

	private static String getLocaleId(SvReader svr) {
		String locale = SvConf.getDefaultLocale();
		try {
			if (svr.getUserLocale(SvReader.getUserBySession(svr.getSessionId())) != null) {
				DbDataObject localeObj = svr.getUserLocale(SvReader.getUserBySession(svr.getSessionId()));
				if (localeObj.getVal(Tc.LOCALE_ID).toString() != null)
					locale = localeObj.getVal(Tc.LOCALE_ID).toString();
			}
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage(), e);
		}
		return locale;
	}

	private static JsonObject addBoleanParse(JsonObject jsonData, DbDataObject recordObject, String readField,
			String saveField) {
		Boolean tmpB = null;
		try {
			tmpB = (Boolean) recordObject.getVal(readField);
			if (tmpB) { // true
				if ("mk_MK".equalsIgnoreCase(SvConf.getDefaultLocale()))
					jsonData.addProperty(saveField, I18n.getText(SvConf.getDefaultLocale(), "mk.yes"));
				else
					jsonData.addProperty(saveField, I18n.getText(SvConf.getDefaultLocale(), "yes"));
			} else {
				if ("mk_MK".equalsIgnoreCase(SvConf.getDefaultLocale()))
					jsonData.addProperty(saveField, I18n.getText(SvConf.getDefaultLocale(), "mk.no"));
				else
					jsonData.addProperty(saveField, I18n.getText(SvConf.getDefaultLocale(), "no"));
			}
		} catch (Exception e) {
			String tmpStr = null;
			tmpStr = (String) recordObject.getVal(readField);
			if (tmpStr != null) {
				if (tmpStr.equalsIgnoreCase("true") || tmpStr.equalsIgnoreCase("t") || tmpStr.equalsIgnoreCase("y")
						|| tmpStr.equalsIgnoreCase("1") || tmpStr.equalsIgnoreCase("yes")
						|| tmpStr.equalsIgnoreCase(I18n.getText(SvConf.getDefaultLocale(), "mk.yes"))
						|| tmpStr.equalsIgnoreCase(I18n.getText(SvConf.getDefaultLocale(), "yes")))
					tmpB = true;
				if (tmpStr.equalsIgnoreCase("false") || tmpStr.equalsIgnoreCase("f") || tmpStr.equalsIgnoreCase("n")
						|| tmpStr.equalsIgnoreCase("0") || tmpStr.equalsIgnoreCase("no")
						|| tmpStr.equalsIgnoreCase(I18n.getText(SvConf.getDefaultLocale(), "mk.no"))
						|| tmpStr.equalsIgnoreCase(I18n.getText(SvConf.getDefaultLocale(), "no")))
					tmpB = false;

			}
			debugException(e);
		}
		return jsonData;
	}

	/**
	 * procedure to add value to JsonObject one field value from record from table,
	 * this will not process svarog repo fields, this is very similar to
	 * addValueToJsonObject1 but works on data that is part of a query or a simple
	 * table
	 * 
	 * @param jsonData     JsonObject object in which we want to save the data
	 * @param recordObject DbDataObject object from which we read the data
	 * @param tmpField     DbDataObject object from SVAROG_FIELDS that will describe
	 *                     the field that we try to save
	 * @param saveField    String under what name we want to insert the value, if we
	 *                     have query data is saved as TBL0, TBL1, TB2, so with this
	 *                     we translate those with correct table names
	 * @param doTranslate  Boolean if set to TRUE it will translate all label_codes,
	 *                     so it should be TRUE most of the time except in
	 *                     administrative console where we have to set the codes
	 * 
	 * @return JSON String with data from oData Object
	 */
	private static JsonObject addValueToJsonObject2(JsonObject jsonData, DbDataObject recordObject,
			DbDataObject tmpField, String readField, String saveField, Boolean doTranslate, SvReader svr) {
		JsonObject jsonDataForWork = jsonData;
		CodeList cl = null;
		String nameField = tmpField.getVal(Tc.FIELD_NAME).toString();
		if (processField(nameField))
			try {
				if (recordObject.getVal(readField) == null) {
					return jsonDataForWork;
				}
				switch (tmpField.getVal(Tc.FIELD_TYPE).toString()) {
				case Tc.NVARCHAR:
					String tmpS = null;
					if (recordObject.getVal(readField) != null)
						tmpS = recordObject.getVal(readField).toString();
					if (tmpField.getVal(Tc.SV_ISLABEL) != null && tmpField.getVal(Tc.SV_ISLABEL).equals(true)) {
						tmpS = I18n.getText(getLocaleId(svr), tmpS);
						jsonDataForWork.addProperty(saveField, tmpS);
						break;
					}
					if (tmpField.getVal(Tc.SV_MUTLISELECT) != null && tmpField.getVal(Tc.SV_MUTLISELECT).equals(true)) {
						String translatedResult = Tc.EMPTY_STRING;
						StringBuilder trResBuild = new StringBuilder();

						if (tmpField.getVal(Tc.CODE_LIST_ID) != null) {
							cl = new CodeList(svr);
							HashMap<String, String> listMap = cl.getCodeList(getLocaleId(svr),
									Long.valueOf(tmpField.getVal(Tc.CODE_LIST_ID).toString()), true);
							String[] cArray = tmpS.split(SvConf.getMultiSelectSeparator());
							// if SvConf.getMultiSelectSeparator() is empty, set
							// default to ,
							String multiSelectOperator = SvConf.getMultiSelectSeparator() == null ? ","
									: SvConf.getMultiSelectSeparator();
							for (String tempCodeListKey : cArray) {
								translatedResult = translatedResult
										+ I18n.getText(getLocaleId(svr), listMap.get(tempCodeListKey))
										+ multiSelectOperator;
								trResBuild.append(I18n.getText(getLocaleId(svr), listMap.get(tempCodeListKey))
										+ multiSelectOperator);
							}
						}
						trResBuild.substring(0, trResBuild.length() - 1);
						translatedResult = translatedResult.substring(0, translatedResult.length() - 1);
						jsonDataForWork.addProperty(saveField, trResBuild.toString());
						break;
					}
					if (tmpS != null) {
						if (Tc.LABEL_CODE.equalsIgnoreCase(nameField) && doTranslate)
							tmpS = I18n.getText(getLocaleId(svr), tmpS);

						jsonDataForWork.addProperty(saveField, tmpS);
					}
					break;
				case Tc.NUMERIC:
					Number tmpN = null;
					Long isFloat = (Long) tmpField.getVal(Tc.FIELD_SCALE);
					if (isFloat == null || isFloat == 0) {
						Long tmpL = null;
						tmpL = Long.valueOf(recordObject.getVal(readField).toString());
						tmpN = tmpL;
					} else {
						Double tmpDo = null;
						tmpDo = Double.valueOf(recordObject.getVal(nameField).toString());
						tmpN = tmpDo;
					}
					if (tmpN != null)
						jsonDataForWork.addProperty(saveField, tmpN);
					break;
				case Tc.BOOLEAN:
					jsonDataForWork = addBoleanParse(jsonData, recordObject, readField, saveField);
					break;
				case Tc.DATE: // for some reason date was saved as datetime
					DateTime tmpDsh = new DateTime(recordObject.getVal(readField));

					int monthInt = tmpDsh.monthOfYear().get();
					int dayInt = tmpDsh.dayOfMonth().get();
					String monthStr = ((monthInt < 10) ? "0" : "") + String.valueOf(monthInt);
					String dayStr = ((dayInt < 10) ? "0" : "") + String.valueOf(dayInt);
					jsonDataForWork.addProperty(saveField, tmpDsh.year().get() + "-" + monthStr + "-" + dayStr);

					break;
				case Tc.TIMESTAMP:
				case Tc.DATETIME:
					DateTime tmpDlg = null;
					tmpDlg = (DateTime) recordObject.getVal(readField);
					if (tmpDlg != null)
						jsonDataForWork.addProperty(saveField, tmpDlg.toString());
					break;
				default:
				}
			} catch (Exception e) {
				debugException(e);
			} finally {
				if (cl != null)
					cl.release();
			}
		return jsonDataForWork;
	}

	public static void debugException(Exception e) {
		if (log4j.isDebugEnabled())
			log4j.debug(e.getMessage(), e);
	}

	public static Boolean processField(String fieldName) {
		Boolean retVal = false;
		if (!Tc.PKID.equalsIgnoreCase(fieldName) && !"GUI_METADATA".equalsIgnoreCase(fieldName)
				&& !"CENTROID".equalsIgnoreCase(fieldName) && !"GEOM".equalsIgnoreCase(fieldName))
			retVal = true;
		return retVal;
	}

	public static JsonObject prapareSvarogData(DbDataObject dbo, String tableName, JsonObject jBo) {
		jBo.addProperty(tableName + "." + Tc.PKID, dbo.getPkid());
		jBo.addProperty(tableName + "." + Tc.OBJECT_ID, dbo.getObject_id());
		jBo.addProperty(tableName + "." + Tc.PARENT_ID, dbo.getParent_id());
		jBo.addProperty(tableName + "." + Tc.OBJECT_TYPE, dbo.getObject_type());
		jBo.addProperty(tableName + "." + Tc.STATUS, dbo.getStatus());
		return jBo;
	}

	public static JsonObject prapareSvarogDataFull(DbDataObject dbo, String tableName, JsonObject jBo) {
		JsonObject jBo1 = prapareSvarogData(dbo, tableName, jBo);
		jBo1.addProperty(tableName + "." + Tc.USER_ID, dbo.getUser_id());
		jBo1.addProperty(tableName + "." + Tc.DT_INSERT, dbo.getDt_insert().toString());
		jBo1.addProperty(tableName + "." + Tc.DT_DELETE, dbo.getDt_delete().toString());
		return jBo1;
	}

	public static JsonObject prapareSvarogDataFull(DbDataObject dbo, String tableName, int i, JsonObject jBo) {
		JsonObject jData = jBo;
		String tmpStr = Tc.TBL + i + "_";
		if (dbo.getVal(tmpStr + Tc.PKID) == null)
			jData = prapareSvarogDataFull(dbo, tableName, jBo);
		else {
			if (dbo.getVal(tmpStr + Tc.PKID) != null)
				jData.addProperty(tableName + "." + Tc.PKID, (Long) dbo.getVal(tmpStr + Tc.PKID));
			if (dbo.getVal(tmpStr + Tc.META_PKID) != null)
				jData.addProperty(tableName + "." + Tc.META_PKID, (Long) dbo.getVal(tmpStr + Tc.META_PKID));
			if (dbo.getVal(tmpStr + Tc.OBJECT_ID) != null)
				jData.addProperty(tableName + "." + Tc.OBJECT_ID, (Long) dbo.getVal(tmpStr + Tc.OBJECT_ID));
			if (dbo.getVal(tmpStr + Tc.PARENT_ID) != null)
				jData.addProperty(tableName + "." + Tc.PARENT_ID, (Long) dbo.getVal(tmpStr + Tc.PARENT_ID));
			if (dbo.getVal(tmpStr + Tc.OBJECT_TYPE) != null)
				jData.addProperty(tableName + "." + Tc.OBJECT_TYPE, (Long) dbo.getVal(tmpStr + Tc.OBJECT_TYPE));
			if (dbo.getVal(tmpStr + Tc.STATUS) != null)
				jData.addProperty(tableName + "." + Tc.STATUS, dbo.getVal(tmpStr + Tc.STATUS).toString());
			if (dbo.getVal(tmpStr + Tc.USER_ID) != null)
				jData.addProperty(tableName + "." + Tc.USER_ID, (Long) dbo.getVal(tmpStr + Tc.USER_ID));
			if (dbo.getVal(tmpStr + Tc.DT_DELETE) != null)
				jData.addProperty(tableName + "." + Tc.DT_DELETE, dbo.getVal(tmpStr + Tc.DT_DELETE).toString());
			if (dbo.getVal(tmpStr + Tc.DT_INSERT) != null)
				jData.addProperty(tableName + "." + Tc.DT_INSERT, dbo.getVal(tmpStr + Tc.DT_INSERT).toString());
		}
		return jData;
	}

	public static JsonObject prapareSvarogData(DbDataObject dbo, String tableName, int i, JsonObject jBo) {
		JsonObject jData = jBo;
		String tmpStr = Tc.TBL + i + "_";
		if (dbo.getVal(tmpStr + Tc.PKID) == null)
			jData = prapareSvarogData(dbo, tableName, jBo);
		else {
			if (dbo.getVal(tmpStr + Tc.PKID) != null)
				jData.addProperty(tableName + "." + Tc.PKID, (Long) dbo.getVal(tmpStr + Tc.PKID));
			if (dbo.getVal(tmpStr + Tc.OBJECT_ID) != null)
				jData.addProperty(tableName + "." + Tc.OBJECT_ID, (Long) dbo.getVal(tmpStr + Tc.OBJECT_ID));
			if (dbo.getVal(tmpStr + Tc.PARENT_ID) != null)
				jData.addProperty(tableName + "." + Tc.PARENT_ID, (Long) dbo.getVal(tmpStr + Tc.PARENT_ID));
			if (dbo.getVal(tmpStr + Tc.OBJECT_TYPE) != null)
				jData.addProperty(tableName + "." + Tc.OBJECT_TYPE, (Long) dbo.getVal(tmpStr + Tc.OBJECT_TYPE));
			if (dbo.getVal(tmpStr + Tc.STATUS) != null)
				jData.addProperty(tableName + "." + Tc.STATUS, dbo.getVal(tmpStr + Tc.STATUS).toString());
		}
		return jData;
	}

	/**
	 * procedure to generate part of the Json string for the data that is part of
	 * SVAROG core, overloaded version used when we make join query
	 * 
	 * @param vData            DbDataArray this is where all data from the query is
	 *                         stored
	 * @param tablesUsedArray  Array of String array of tables used in the query,
	 *                         that MUST be in same order as used in building the
	 *                         query
	 * @param tableShowArray   Array of Boolean , to save on some time and
	 *                         string/Json size, we can hide full tables that don't
	 *                         have anything for display
	 * @param tablesusedCount  int , when we make svarog join every table is renamed
	 *                         to TBL[i] , this will tell us how many tables are we
	 *                         joining so we don't go out of index
	 * @param doTranslate      Boolean if set to TRUE it will translate all
	 *                         label_codes, so it should be TRUE most of the time
	 *                         except in administrative console where we have to set
	 *                         the codes
	 * @param svr              connected SvReader
	 * @param isfullSvarogData Boolean if set to TRUE it will return full REPO field
	 *                         data, FALSE will return whatever is set in the REPO
	 *                         fields GUI_METADATA
	 * 
	 * @return JSON JsonArray with data from VData array
	 */
	public static JsonArray prapareTableQueryData(DbDataArray vData, String[] tablesUsedArray, Boolean[] tableShowArray,
			int tablesusedCount, Boolean doTranslate, SvReader svr, Boolean isfullSvarogData) {
		JsonArray jarr = new JsonArray();
		if (vData != null && !vData.getItems().isEmpty())
			for (int j = 0; j < vData.getItems().size(); j++) {
				jarr.add(prapareTableQueryJsonObject(vData.getItems().get(j), tablesUsedArray, tableShowArray,
						tablesusedCount, doTranslate, svr, isfullSvarogData));
			}
		return jarr;
	}

	/**
	 * procedure to generate part of the Json string for the data that is part of
	 * SVAROG core, overloaded version used when we make join query
	 * 
	 * @param vData           DbDataArray this is where all data from the query is
	 *                        stored
	 * @param tablesUsedArray Array of String array of tables used in the query,
	 *                        that MUST be in same order as used in building the
	 *                        query
	 * @param tableShowArray  Array of Boolean , to save on some time and
	 *                        string/Json size, we can hide full tables that don't
	 *                        have anything for display
	 * @param i               int , when we make svarog join every table is renamed
	 *                        to TBL[i] , this will tell us how many tables are we
	 *                        joining so we don't go out of index
	 * @param svr             connected SvReader
	 * 
	 * @return JSON String with data from VData array
	 */
	public static String prapareTableQueryData(DbDataArray vData, String[] tablesUsedArray, Boolean[] tableShowArray,
			int tablesusedCount, Boolean doTranslate, SvReader svr) {
		return prapareTableQueryData(vData, tablesUsedArray, tableShowArray, tablesusedCount, doTranslate, svr, false)
				.toString();
	}

	private static JsonObject prapareTableQueryJsonObject(DbDataObject obj1, String[] tablesUsedArray,
			Boolean[] tableShowArray, int tablesusedCount, Boolean doTranslate, SvReader svr,
			Boolean isfullSvarogData) {
		JsonObject jData = new JsonObject();
		for (int k = 0; k < tablesusedCount; k++)
			if (tableShowArray[k]) {
				DbDataObject tableObject = SvCore.getDbtByName(tablesUsedArray[k]);
				DbDataArray typetoGet = SvCore.getFields(tableObject.getObject_id());
				if (isfullSvarogData)
					jData = prapareSvarogDataFull(obj1, tablesUsedArray[k], k, jData);
				else
					jData = prapareSvarogData(obj1, tablesUsedArray[k], k, jData);
				for (int i = 0; i < typetoGet.getItems().size(); i++) {
					String tmpField = typetoGet.getItems().get(i).getVal(Tc.FIELD_NAME).toString();
					String fieldToRead = Tc.EMPTY_STRING;
					if (tablesusedCount != 1)
						fieldToRead = Tc.TBL + k + "_";
					fieldToRead = fieldToRead + tmpField;
					String readField = tablesUsedArray[k] + "." + tmpField;
					jData = addValueToJsonObject2(jData, obj1, typetoGet.getItems().get(i), fieldToRead, readField,
							doTranslate, svr);
				}
			}
		return jData;
	}

	public static JsonArray prapareTableQueryDataV1(List<DbDataObject> vData, String[] tablesUsedArray,
			Boolean[] tableShowArray, int tablesusedCount, Boolean doTranslate, SvReader svr,
			Boolean isfullSvarogData) {
		JsonArray jarr = new JsonArray();
		if (vData != null && !vData.isEmpty())
			for (int j = 0; j < vData.size(); j++) {
				jarr.add(prapareTableQueryJsonObject(vData.get(j), tablesUsedArray, tableShowArray, tablesusedCount,
						doTranslate, svr, isfullSvarogData));
			}
		return jarr;
	}

	public DbDataArray getAllowedObjectsAccordingPOALink(String searchTableName, DbDataObject dboGeoField,
			DbSearchCriterion dbCrit, Integer recordNumber, SvReader svr) throws SvException {
		DbDataArray returnData = new DbDataArray();
		boolean isHeadquarter = false;
		DbDataArray linkedOrgUnitsPerUser = getLinkedOrgUnitsPerUser(svr);
		linkedOrgUnitsPerUser.getSortedItems(Tc.EXTERNAL_ID);
		DbDataArray arrLinkedTablesWithGeostatCode = getLinkedTablesWithGeoStatCodePerUser(
				SvReader.getDbtByName(searchTableName).getObject_id(), svr);
		if ((arrLinkedTablesWithGeostatCode == null || arrLinkedTablesWithGeostatCode.getItems().isEmpty())
				&& !linkedOrgUnitsPerUser.getItems().isEmpty()) {
			// this will work only if we prohibit linking User to Object
			// that does not belongs to certain ORG_UNIT (It belongs when
			// geostat of Object matches ORG_UNIT -> EXTERNAL_ID)
			DbSearchExpression dbse = null;
			DbSearchExpression dbse2 = new DbSearchExpression();
			for (DbDataObject dboOrgUnit : linkedOrgUnitsPerUser.getItems()) {
				if (dboOrgUnit.getVal(Tc.EXTERNAL_ID) != null) {
					dbse = new DbSearchExpression();
					DbSearchCriterion cr1 = new DbSearchCriterion(dboGeoField.getVal(Tc.FIELD_NAME).toString(),
							DbCompareOperand.LIKE, dboOrgUnit.getVal(Tc.EXTERNAL_ID).toString() + Tc.PERCENT_OPERATOR);
					if (dbse.getSQLExpression().trim().equals("")) {
						cr1.setNextCritOperand(Tc.OR);
						dbse.addDbSearchItem(cr1);
					} else {
						cr1.setNextCritOperand(Tc.OR);
						dbse.addDbSearchItem(cr1);
					}
				} else {
					isHeadquarter = true;
				}
				dbse2.addDbSearchItem(dbCrit);
				if (dbse != null) {
					dbse2.addDbSearchItem(dbse);
				}
				returnData = svr.getObjects(dbse2, SvReader.getTypeIdByName(searchTableName), null, recordNumber, 0);
				if (isHeadquarter) {
					break;
				}
			}
		} else {
			if (arrLinkedTablesWithGeostatCode != null && !arrLinkedTablesWithGeostatCode.getItems().isEmpty()) {
				returnData = arrLinkedTablesWithGeostatCode;
			} else {
				returnData = svr.getObjects(new DbSearchExpression().addDbSearchItem(dbCrit),
						SvReader.getDbtByName(searchTableName).getObject_id(), null, recordNumber, 0);
			}
		}
		return returnData;
	}

	/**
	 * Objects linked with custom POA
	 * 
	 * @param tableId
	 * @param svr
	 * @return
	 * @throws SvException
	 */
	public DbDataArray getLinkedTablesWithGeoStatCodePerUser(Long tableId, SvReader svr) throws SvException {
		DbDataObject dbLink = SvReader.getLinkType(Tc.CUSTOM_POA, svCONST.OBJECT_TYPE_USER, tableId);
		DbDataObject dboUser = SvReader.getUserBySession(svr.getSessionId());
		return svr.getObjectsByLinkedId(dboUser.getObject_id(), dbLink, null, 0, 0);
	}

	/** getLinkedOrgUnitsPerUser */
	public DbDataArray getLinkedOrgUnitsPerUser(SvReader svr) throws SvException {
		// DbDataArray linkedOrgUnitsPerUser = new DbDataArray();
		DbDataObject dbLink = SvReader.getLinkType(Tc.POA, svCONST.OBJECT_TYPE_USER, svCONST.OBJECT_TYPE_ORG_UNITS);
		DbDataObject dboUser = SvReader.getUserBySession(svr.getSessionId());
		// linkedOrgUnitsPerUser =
		// svr.getObjectsByLinkedId(dboUser.getObject_id(), dbLink, null, 0, 0);
		return svr.getObjectsByLinkedId(dboUser.getObject_id(), dbLink, null, 0, 0);
	}

	public DbDataObject getGeoField(Long tableObjId, SvReader svr) throws SvException {
		DbDataObject dbField = null;
		DbDataArray dbArrFields = svr.getObjectsByParentId(tableObjId, svCONST.OBJECT_TYPE_FIELD, null, 0, 0);
		if (!dbArrFields.getItems().isEmpty()) {
			for (DbDataObject tempDboField : dbArrFields.getItems()) {
				if (tempDboField.getVal(Tc.EXTENDED_PARAMS) != null) {
					JSONParser parser = new JSONParser();
					try {
						JSONObject json = (JSONObject) parser.parse(tempDboField.getVal(Tc.EXTENDED_PARAMS).toString());
						if (json.containsKey(Tc.IS_GEOSTAT)) {
							dbField = tempDboField;
							break;
						}
					} catch (Exception e) {
						log4j.error("Bad json format in extended param: " + tempDboField.getVal(Tc.FIELD_NAME));
					}
				}
			}
		}
		return dbField;
	}

	public DbDataArray getValidAnimalsOrFlockByParentId(Long parentId, Long objectType, SvReader svr)
			throws SvException {
		DbDataArray validAnimalOrFlocks = new DbDataArray();
		DbSearchCriterion cr1 = new DbSearchCriterion(Tc.PARENT_ID, DbCompareOperand.EQUAL, parentId);
		DbSearchCriterion cr2 = new DbSearchCriterion(Tc.STATUS, DbCompareOperand.EQUAL, Tc.VALID);
		validAnimalOrFlocks = svr.getObjects(new DbSearchExpression().addDbSearchItem(cr1).addDbSearchItem(cr2),
				objectType, null, 0, 0);
		if (validAnimalOrFlocks.getItems().isEmpty()) {
			validAnimalOrFlocks = svr.getObjects(new DbSearchExpression().addDbSearchItem(cr1).addDbSearchItem(cr2),
					objectType, new DateTime(), 0, 0);
		}
		return validAnimalOrFlocks;
	}

	/**
	 * Method that gets all new/unread Conversations assigned to user
	 * 
	 * @param userObjectId Object_Id of the user
	 * @param svr          SvReader instance
	 * @return
	 * @throws SvException
	 */
	public DbDataArray getUnreadMessagesPerUser(Long userObjectId, SvReader svr) throws SvException {
		DbSearchCriterion cr1 = new DbSearchCriterion(Tc.ASSIGNED_TO, DbCompareOperand.EQUAL, userObjectId);
		DbSearchCriterion cr2 = new DbSearchCriterion(Tc.IS_READ, DbCompareOperand.EQUAL, false);
		return svr.getObjects(new DbSearchExpression().addDbSearchItem(cr1).addDbSearchItem(cr2),
				svCONST.OBJECT_TYPE_CONVERSATION, null, 0, 0);
	}

	/**
	 * A method that gets all unread replies in Conversation per user i.e if the
	 * user creates message assigned to another user, and if the assigned user
	 * replies on that message, we count it as unread reply, until the creator of
	 * the message doesn't open it
	 * 
	 * @param userObjectId Object_Id of the user
	 * @param svr          SvReader instance
	 * @return
	 * @throws SvException
	 */
	public DbDataArray getUnreadMessageRepliesPerUser(Long userObjectId, SvReader svr) throws SvException {
		DbDataArray arrUnreadMessageReplies;
		DbDataArray arrResult = new DbDataArray();
		DbSearchCriterion cr1 = new DbSearchCriterion(Tc.CREATED_BY, DbCompareOperand.EQUAL, userObjectId);
		DbSearchCriterion cr2 = new DbSearchCriterion(Tc.IS_READ, DbCompareOperand.EQUAL, false);
		arrUnreadMessageReplies = svr.getObjects(new DbSearchExpression().addDbSearchItem(cr1).addDbSearchItem(cr2),
				svCONST.OBJECT_TYPE_CONVERSATION, null, 0, 0);
		if (!arrUnreadMessageReplies.getItems().isEmpty()) {
			for (DbDataObject dboConversation : arrUnreadMessageReplies.getItems()) {
				DbDataArray arrMessages = svr.getObjectsByParentId(dboConversation.getObject_id(),
						svCONST.OBJECT_TYPE_MESSAGE, null, 0, 0);
				if (!arrMessages.getItems().isEmpty()) {
					DbDataObject dboMessage = arrMessages.get(arrMessages.size() - 1);
					if (dboMessage.getVal(Tc.CREATED_BY) != null
							&& !Long.valueOf(dboMessage.getVal(Tc.CREATED_BY).toString())
									.equals(Long.valueOf(dboConversation.getVal(Tc.CREATED_BY).toString()))) {
						arrResult.addDataItem(dboConversation);
					}
				}
			}
		}
		return arrResult;
	}

	/**
	 * Returns count of {@link #getUnreadMessageRepliesPerUser(Long, SvReader)} and
	 * {@link #getUnreadMessagesPerUser(Long, SvReader)}
	 * 
	 * @param svr SvReader instance
	 * @return
	 * @throws SvException
	 */
	public Integer countUnreadMessagesPerUser(SvReader svr) throws SvException {
		DbDataObject dboCurrentUser = svr.getInstanceUser();
		int cntUnreadConversations = getUnreadMessagesPerUser(dboCurrentUser.getObject_id(), svr).size();
		int cntUnreadReplies = getUnreadMessageRepliesPerUser(dboCurrentUser.getObject_id(), svr).size();
		return cntUnreadConversations + cntUnreadReplies;
	}

	/**
	 * Method that returns history list of Pet object
	 * 
	 * @param petId Pet ID of the pet
	 * 
	 * @param svr   SvReader instance
	 * 
	 * @throws SvException
	 * @return DbDataArray
	 */
	public DbDataArray getDboPetWithHistory(Long objectId, SvReader svr) throws SvException {
		DbDataArray result = null;
		DbSearchExpression dbse = new DbSearchExpression();
		DbSearchCriterion cr1 = new DbSearchCriterion(Tc.OBJECT_ID, DbCompareOperand.EQUAL, objectId);
		dbse.addDbSearchItem(cr1);
		result = svr.getObjectsHistory(dbse, SvReader.getTypeIdByName(Tc.PET), 0, 0);
		result.getSortedItems(Tc.PKID, true);
		return result;
	}

	/**
	 * Method that returns array of Movement document objects by animal ID or flock
	 * ID
	 * 
	 * @param animalOrFlockId
	 * @param movementType
	 * @param svr
	 * @return
	 * @throws SvException
	 */
	public DbDataArray getMovementDocumentByAnimalOrFlockId(String animalOrFlockId, String movementType, SvReader svr)
			throws SvException {
		DbDataArray arrMovementDocuments = new DbDataArray();
		DbDataArray arrAnimalOrFlockMovementObjects;
		String columnName = Tc.ANIMAL_EAR_TAG;
		Long objectType = SvReader.getTypeIdByName(Tc.ANIMAL_MOVEMENT);
		if (movementType.equalsIgnoreCase(Tc.FLOCK)) {
			columnName = Tc.FLOCK_ID;
			objectType = SvReader.getTypeIdByName(Tc.FLOCK_MOVEMENT);
		}
		arrAnimalOrFlockMovementObjects = findDataPerSingleFilter(columnName, animalOrFlockId, DbCompareOperand.EQUAL,
				objectType, svr);
		if (!arrAnimalOrFlockMovementObjects.getItems().isEmpty()) {
			for (DbDataObject dboMovement : arrAnimalOrFlockMovementObjects.getItems()) {
				if (dboMovement.getVal(Tc.MOVEMENT_DOC_ID) != null) {
					DbDataObject dboMovementDoc = searchForObject(SvReader.getTypeIdByName(Tc.MOVEMENT_DOC),
							Tc.MOVEMENT_DOC_ID, dboMovement.getVal(Tc.MOVEMENT_DOC_ID).toString(), svr);
					if (dboMovementDoc != null) {
						arrMovementDocuments.addDataItem(dboMovementDoc);
					}
				}
			}
		}
		return arrMovementDocuments;
	}

	/**
	 * Method that returns array of Movement document objects by transporter license
	 * 
	 * @param animalOrFlockId
	 * @param movementType
	 * @param svr
	 * @return
	 * @throws SvException
	 */
	public DbDataArray getMovementDocumentByTransporterLicense(String transporterLicense, SvReader svr)
			throws SvException {
		DbDataArray arrMovementDocuments = new DbDataArray();
		Long objectAnimalMovementType = SvReader.getTypeIdByName(Tc.ANIMAL_MOVEMENT);
		Long objectFlockMovementType = SvReader.getTypeIdByName(Tc.FLOCK_MOVEMENT);
		DbDataArray arrAnimalMovementObjects = getAnimalOrFlockMovementByTransporterLicense(transporterLicense,
				objectAnimalMovementType, 0, svr);
		if (arrAnimalMovementObjects != null && !arrAnimalMovementObjects.getItems().isEmpty()) {
			for (DbDataObject dboMovement : arrAnimalMovementObjects.getItems()) {
				if (dboMovement.getVal(Tc.MOVEMENT_DOC_ID) != null) {
					DbDataObject dboMovementDoc = searchForObject(SvReader.getTypeIdByName(Tc.MOVEMENT_DOC),
							Tc.MOVEMENT_DOC_ID, dboMovement.getVal(Tc.MOVEMENT_DOC_ID).toString(), svr);
					if (dboMovementDoc != null) {
						arrMovementDocuments.addDataItem(dboMovementDoc);
					}
				}
			}
		}
		DbDataArray arrFlockMovementObjects = getAnimalOrFlockMovementByTransporterLicense(transporterLicense,
				objectFlockMovementType, 0, svr);
		if (arrFlockMovementObjects != null && !arrFlockMovementObjects.getItems().isEmpty()) {
			for (DbDataObject dboMovement : arrFlockMovementObjects.getItems()) {
				if (dboMovement.getVal(Tc.MOVEMENT_DOC_ID) != null) {
					DbDataObject dboMovementDoc = searchForObject(SvReader.getTypeIdByName(Tc.MOVEMENT_DOC),
							Tc.MOVEMENT_DOC_ID, dboMovement.getVal(Tc.MOVEMENT_DOC_ID).toString(), svr);
					if (dboMovementDoc != null) {
						arrMovementDocuments.addDataItem(dboMovementDoc);
					}
				}
			}
		}
		return arrMovementDocuments;
	}

	public DbDataArray getInventoryItemsByRange(Long parentId, Long rangeFrom, Long rangeTo, String tagType,
			String order, SvReader svr) throws SvException {
		DbSearchCriterion cr1 = new DbSearchCriterion(Tc.EAR_TAG_NUMBER + "::NUMERIC", DbCompareOperand.BETWEEN,
				rangeFrom, rangeTo);
		DbSearchCriterion cr2 = new DbSearchCriterion(Tc.PARENT_ID, DbCompareOperand.EQUAL, parentId);
		DbSearchExpression dbse = new DbSearchExpression();
		dbse.addDbSearchItem(cr1).addDbSearchItem(cr2);
		if (!Tc.NULL.equals(tagType)) {
			cr1 = new DbSearchCriterion(Tc.TAG_TYPE, DbCompareOperand.EQUAL, tagType);
			dbse.addDbSearchItem(cr1);
		}
		if (!Tc.NULL.equals(order)) {
			cr1 = new DbSearchCriterion(Tc.ORDER_NUMBER, DbCompareOperand.EQUAL, order);
			dbse.addDbSearchItem(cr1);
		}
		return svr.getObjects(dbse, SvReader.getTypeIdByName(Tc.INVENTORY_ITEM), null, 2000, 0);
	}

	public DbDataArray getAnimalOrFlockMovementByTransporterLicense(String transporterLicense, Long objTypeId,
			Integer rowLimit, SvReader svr) throws SvException {
		DbDataArray result = null;
		DbSearchCriterion cr1 = new DbSearchCriterion(Tc.TRANSPORTER_LICENSE, DbCompareOperand.EQUAL,
				transporterLicense);
		DbSearchCriterion cr2 = new DbSearchCriterion(Tc.MOVEMENT_TRANSPORT_TYPE, DbCompareOperand.EQUAL, Tc.VEHICLE);
		result = svr.getObjects(new DbSearchExpression().addDbSearchItem(cr1).addDbSearchItem(cr2), objTypeId, null,
				rowLimit, 0);
		return result;
	}

	public String getActionNameDependOnSubactionType(String subactionType, SvReader svr) throws SvException {
		String result = Tc.EMPTY_STRING;
		switch (subactionType) {
		case "VACCINATE":
		case "PHYSICAL_CHECK":
			result = Tc.ACTIVITY;
			break;
		case "SLAUGHTER":
		case "DIED":
		case "ABSENT":
		case "DESTROY":
		case "LOST":
		case "SOLD":
			result = Tc.RETIRE;
			break;
		case "GENERATE_PRE_MORTEM":
		case "GENERATE_POST_MORTEM":
			result = "OTHER";
			break;
		default:
			throw new SvException("naits.error.invalid_action_type", svr.getInstanceUser());
		}
		return result;
	}

	public String getSubactionNameDependOnSubactionType(String subactionType, SvReader svr) throws SvException {
		String result = Tc.EMPTY_STRING;
		switch (subactionType) {
		case "VACCINATE":
		case "PHYSICAL_CHECK":
		case "DIED":
		case "ABSENT":
		case "SOLD":
		case "LOST":
			result = subactionType;
			break;
		case "SLAUGHTER":
			result = Tc.SLAUGHTRD;
			break;
		case "DESTROY":
			result = Tc.DESTROYED;
			break;
		case "GENERATE_PRE_MORTEM":
			result = Tc.GENERATE_PREMORTEM;
			break;
		case "GENERATE_POST_MORTEM":
			result = Tc.GENERATE_POSTMORTEM;
			break;
		default:
			throw new SvException("naits.error.invalid_subaction_type", svr.getInstanceUser());
		}
		return result;
	}

	public String getDiseaseCodeInVaccinationBookAccordingVaccinationCode(String vaccCode) {
		String result = "";
		switch (vaccCode) {
		case "FMD-1":
		case "FMD-2":
		case "FMD-RE":
		case "FMD-FORCE":
			result = "1";
			break;
		case "ANTHR-GOV":
		case "ANTHR-RE":
		case "ANTHR-FORCE":
			result = "2";
			break;
		case "BRUC-GOV":
		case "BRUC-RE":
		case "BRUC-FORCE":
			result = "3";
			break;
		case "RABIES-GOV":
		case "RABIES-RE":
		case "RABIES-FORCE":
			result = "5";
			break;
		default:
			break;
		}
		return result;
	}

	public DbDataArray getSvFilesPerDbo(DbDataObject dbo, SvReader svr) throws SvException {
		DbDataObject dboLinkBetweenDboAndFile = SvReader.getLinkType(Tc.LINK_FILE, dbo.getObject_type(),
				svCONST.OBJECT_TYPE_FILE);
		return svr.getObjectsByLinkedId(dbo.getObject_id(), dboLinkBetweenDboAndFile, null, 0, 0);
	}

	public List<String> getRFIDEarTagsViaFile(DbDataObject dboRfid) throws SvException {
		ArrayList<String> listRfidTags = null;
		String[] arrRfidTags;
		if (dboRfid.getVal(Tc.FILE_EAR_TAGS) != null) {
			listRfidTags = new ArrayList<>();
			byte[] data = Base64.decodeBase64(dboRfid.getVal(Tc.FILE_EAR_TAGS).toString());
			try (InputStream is = new ByteArrayInputStream(data);
					InputStreamReader isr = new InputStreamReader(is);
					BufferedReader br = new BufferedReader(isr);) {
				String line = null;
				while ((line = br.readLine()) != null) {
					arrRfidTags = line.split(",");
					for (String rfidTag : arrRfidTags) {
						listRfidTags.add(rfidTag);
					}
				}
			} catch (IOException e) {
				log4j.error("Error occurred while reading file in RFID_INPUT object: " + e);
			}
		}
		return listRfidTags;
	}

	public List<String> getRFIDTagsViaFileToList(DbDataObject dboFile, SvFileStore svfs) throws SvException {
		ArrayList<String> listRFIDTags = new ArrayList<>();
		try (InputStream is = svfs.getFileAsStream(dboFile);
				InputStreamReader isr = new InputStreamReader(is);
				BufferedReader br = new BufferedReader(isr)) {
			String[] listRfidEarTags;
			String line = null;
			while ((line = br.readLine()) != null) {
				listRfidEarTags = line.split(",");
				for (String rfidTag : listRfidEarTags) {
					listRFIDTags.add(rfidTag);
				}
			}
		} catch (IOException e) {
			log4j.error(e);
		}
		return listRFIDTags;
	}

	/**
	 * Method used for downloading attached SvFile i.e returns OutputStream
	 * 
	 * @param dbo
	 * @param fileName
	 * @param fileLabelCode
	 * @param out
	 * @param svr
	 * @return
	 * @throws IOException
	 */
	public boolean downloadSvFile(DbDataObject dbo, String fileName, String fileLabelCode, OutputStream out,
			SvReader svr) throws IOException {
		boolean result = true;
		try {
			byte[] fileData = getLinkedSvFileByteArray(dbo, fileName, fileLabelCode, svr);
			out.write(fileData);
		} catch (Exception e) {
			log4j.error("Error occured while downloading Sample file... " + e);
			result = false;
		}
		return result;
	}

	/**
	 * Method that returns attached Svarog file in byte array
	 * 
	 * @param dboObject
	 * @param linkTypeName
	 * @param svr
	 * @return last uploaded file
	 */
	public byte[] getLinkedSvFileByteArray(DbDataObject dboObject, String fileName, String fileLabelCode,
			SvReader svr) {
		SvFileStore svfs = null;
		DateTime refDate = null;
		DbDataObject dboFile = null;
		byte[] fileData = null;
		try {
			svfs = new SvFileStore(svr);
			dboFile = new DbDataObject();
			refDate = new DateTime();
			DbDataObject dboLinkBetweenPopulationAndFile = SvReader.getLinkType(Tc.LINK_FILE,
					dboObject.getObject_type(), svCONST.OBJECT_TYPE_FILE);
			DbDataArray arrFiles = svr.getObjectsByLinkedId(dboObject.getObject_id(), dboLinkBetweenPopulationAndFile,
					refDate, 0, 0);
			if (!arrFiles.getItems().isEmpty()) {
				for (DbDataObject dboTempFile : arrFiles.getItems()) {
					if (fileName == null) {
						if (dboTempFile.getVal(Tc.FILE_NOTES) != null
								&& dboTempFile.getVal(Tc.FILE_NOTES).toString().endsWith(fileLabelCode)) {
							dboFile = dboTempFile;
						}
					} else {
						if (dboTempFile.getVal(Tc.FILE_NAME) != null && dboTempFile.getVal(Tc.FILE_NOTES) != null
								&& dboTempFile.getVal(Tc.FILE_NOTES).toString().equals(fileLabelCode)
								&& dboTempFile.getVal(Tc.FILE_NAME).toString().equals(fileName)) {
							dboFile = dboTempFile;
						}
					}
					if (dboFile.getObject_id() != null && !dboFile.getObject_id().equals(0L)) {
						fileData = svfs.getFileAsByte(dboFile);
						break;
					}
				}
			}
		} catch (Exception e) {
			log4j.error(e);
		} finally {
			if (svfs != null) {
				svfs.release();
			}
		}
		return fileData;
	}

	/**
	 * Method that returns Holding by Holding Responsible's NAT_REG_NUMBER.
	 * 
	 * @param dboKeeperId
	 * @param svr
	 * @return
	 * @throws SvException
	 */
	public DbDataArray getLinkedHoldingsByKeeperNationalRegistrationNumber(String natRegNumber, SvReader svr)
			throws SvException {
		DbDataArray dboArrHoldings = null;
		DbDataObject dboKeeper = searchForObject(SvReader.getTypeIdByName(Tc.HOLDING_RESPONSIBLE), Tc.NAT_REG_NUMBER,
				natRegNumber, svr);
		if (dboKeeper != null) {
			DbDataObject linkHoldingAndKeeper = SvLink.getLinkType(Tc.HOLDING_KEEPER,
					SvReader.getTypeIdByName(Tc.HOLDING), SvReader.getTypeIdByName(Tc.HOLDING_RESPONSIBLE));
			dboArrHoldings = svr.getObjectsByLinkedId(dboKeeper.getObject_id(),
					SvReader.getTypeIdByName(Tc.HOLDING_RESPONSIBLE), linkHoldingAndKeeper,
					SvReader.getTypeIdByName(Tc.HOLDING), true, null, 0, 0);
		}
		return dboArrHoldings;
	}

	/**
	 * Method that returns valid Export certificates linked to certain Animal. One
	 * Animal can be linked to only one valid export certificate at a time
	 * 
	 * @param dboAnimal
	 * @param svr
	 * @return
	 * @throws SvException
	 */
	public DbDataObject getValidExportCertificateLinkedWithDboAnimal(DbDataObject dboAnimal, SvReader svr)
			throws SvException {
		DbDataObject dboExportCertificate = null;
		DbDataObject dboSvarogLink = SvReader.getDbt(svCONST.OBJECT_TYPE_LINK);
		DbDataObject dboAnimalDesc = SvReader.getDbt(dboAnimal.getObject_type());
		DbDataObject dboExportCertificateDesc = SvReader.getDbt(SvReader.getTypeIdByName(Tc.EXPORT_CERT));
		DbDataObject dboLinkBetweenAnimalAndExportCertificate = SvReader.getLinkType(Tc.ANIMAL_EXPORT_CERT,
				dboAnimalDesc, dboExportCertificateDesc);

		DbQueryObject dqoSvLink = new DbQueryObject(dboSvarogLink, null, DbJoinType.INNER, null,
				LinkType.CUSTOM_FREETEXT, null, null);
		dqoSvLink.setCustomFreeTextJoin(" on tbl0.link_obj_id_1 = tbl1.object_id and tbl0.link_type_id="
				+ dboLinkBetweenAnimalAndExportCertificate.getObject_id() + " and tbl1.object_id="
				+ dboAnimal.getObject_id());
		DbQueryObject dqoAnimal = new DbQueryObject(dboAnimalDesc, null, DbJoinType.INNER, null,
				LinkType.CUSTOM_FREETEXT, null, null);
		dqoAnimal.setCustomFreeTextJoin(" on tbl0.link_obj_id_2 = tbl2.object_id");
		DbQueryObject dqoExportCertificate = new DbQueryObject(dboExportCertificateDesc, null, DbJoinType.INNER, null,
				LinkType.CUSTOM_FREETEXT, null, null);
		dqoExportCertificate.setCustomFreeTextJoin(" on tbl0.link_type_id = tbl3.object_id and status =" + Tc.VALID);

		DbQueryExpression dqe = new DbQueryExpression();
		dqe.addItem(dqoSvLink);
		dqe.addItem(dqoAnimal);
		dqe.addItem(dqoExportCertificate);
		DbDataArray arrLinkedObjects = svr.getObjects(dqe, 0, 0);
		if (arrLinkedObjects != null && !arrLinkedObjects.getItems().isEmpty()) {
			DbDataObject dboLinkedObject = arrLinkedObjects.get(0);
			dboExportCertificate = svr.getObjectById(
					Long.valueOf(dboLinkedObject.getVal("TBL2_" + Tc.OBJECT_ID).toString()),
					dboExportCertificateDesc.getObject_id(), null);
		}
		return dboExportCertificate;
	}

	public DbDataArray getValidRegionsLinkedWithUser(DbDataObject dboUser, SvReader svr) {
		DbDataArray dbArrResult = null;
		DbDataArray db = null;
		try {
			dbArrResult = new DbDataArray();
			db = new DbDataArray();
			DbDataObject dboSvarogLink = SvReader.getDbt(svCONST.OBJECT_TYPE_LINK);
			DbDataObject dboOrgUnit = SvReader.getDbt(svCONST.OBJECT_TYPE_ORG_UNITS);
			DbDataObject dboUserDesc = SvReader.getDbt(dboUser.getObject_type());
			DbDataObject dboLinkTypeBetweenUserAndOrgUnit = SvReader.getLinkType(Tc.POA, dboUser.getObject_type(),
					dboOrgUnit.getObject_id());
			DbQueryObject dqoSvLink = new DbQueryObject(dboSvarogLink, null, DbJoinType.INNER, null,
					LinkType.CUSTOM_FREETEXT, null, null);
			dqoSvLink.setCustomFreeTextJoin(" on tbl0.link_obj_id_1 = tbl1.object_id and tbl0.link_type_id="
					+ dboLinkTypeBetweenUserAndOrgUnit.getObject_id() + " and tbl1.object_id="
					+ dboUser.getObject_id());
			DbQueryObject dqoUser = new DbQueryObject(dboUserDesc, null, DbJoinType.INNER, null,
					LinkType.CUSTOM_FREETEXT, null, null);
			dqoUser.setCustomFreeTextJoin(" on tbl0.link_obj_id_2 = tbl2.object_id and tbl2.org_unit_type =" + "'"
					+ Tc.REGIONAL_OFFICE + "'");
			DbQueryObject dqoOrgUnit = new DbQueryObject(dboOrgUnit, null, DbJoinType.INNER, null,
					LinkType.CUSTOM_FREETEXT, null, null);
			DbQueryExpression dqe = new DbQueryExpression();
			dqe.addItem(dqoSvLink);
			dqe.addItem(dqoUser);
			dqe.addItem(dqoOrgUnit);
			dbArrResult = svr.getObjects(dqe, 0, 0);
			if (dbArrResult != null && !dbArrResult.getItems().isEmpty()) {
				for (DbDataObject dbo : dbArrResult.getItems()) {
					DbDataObject dboRes = svr.getObjectById(Long.valueOf(dbo.getVal("TBL2_" + Tc.OBJECT_ID).toString()),
							dboOrgUnit.getObject_id(), null);
					db.addDataItem(dboRes);
				}
			}
		} catch (Exception e) {
			log4j.error(e.getMessage());
		}
		return db;
	}

	/**
	 * Method that search TRANSFER by multiple criteria. Skip status search only for
	 * HQ OUTGOING TRANSFER
	 * 
	 * @param parentId         Parent object ID
	 * @param rangeFrom        Start tag ID
	 * @param rangeTo          End tag ID
	 * @param transferType     Type of transfer (INCOME/OUTCOME)
	 * @param shouldSkipStatus Flag for status skip
	 * @param svr              SvReader instance
	 * @return
	 * @throws SvException
	 */
	public DbDataArray getDbArrayOfDboTransfersByRangeAndParentId(Long parentId, Long rangeFrom, Long rangeTo,
			String transferType, Boolean shouldSkipStatus, SvReader svr) throws SvException {
		DbSearchExpression dbse1 = new DbSearchExpression();
		DbSearchExpression dbse2 = new DbSearchExpression();
		DbSearchExpression dbse3 = new DbSearchExpression();
		DbSearchCriterion cr1 = new DbSearchCriterion(Tc.START_TAG_ID, DbCompareOperand.BETWEEN, rangeFrom, rangeTo);
		DbSearchCriterion cr2 = new DbSearchCriterion(Tc.END_TAG_ID, DbCompareOperand.BETWEEN, rangeFrom, rangeTo);
		DbSearchCriterion cr3 = null;
		if (transferType.equalsIgnoreCase(Tc.INCOME_TRANSFER)) {
			cr3 = new DbSearchCriterion(Tc.DESTINATION_OBJ_ID, DbCompareOperand.EQUAL, parentId.toString());
		} else {
			cr3 = new DbSearchCriterion(Tc.PARENT_ID, DbCompareOperand.EQUAL, parentId);
		}
		dbse1.addDbSearchItem(cr1).addDbSearchItem(cr2).addDbSearchItem(cr3);
		dbse3.addDbSearchItem(dbse1);
		if (!shouldSkipStatus) {
			DbSearchCriterion cr4 = new DbSearchCriterion(Tc.STATUS, DbCompareOperand.EQUAL, Tc.DRAFT);
			cr4.setNextCritOperand(Tc.OR);
			DbSearchCriterion cr5 = new DbSearchCriterion(Tc.STATUS, DbCompareOperand.EQUAL, Tc.DELIVERED);
			dbse2.addDbSearchItem(cr4).addDbSearchItem(cr5);
			dbse3.addDbSearchItem(dbse2);
		}
		return svr.getObjects(dbse3, SvReader.getTypeIdByName(Tc.TRANSFER), null, 0, 0);
	}

	public void doDestinationOrgUnitCheckForDirectInventoryTransfer(Long destinationOrgUnitId, DbDataObject dboTransfer,
			SvReader svr) throws SvException {
		if (dboTransfer.getVal(Tc.DESTINATION_OBJ_ID) != null
				&& !dboTransfer.getVal(Tc.DESTINATION_OBJ_ID).toString().equals(Tc.EMPTY_STRING)) {
			destinationOrgUnitId = Long.valueOf(dboTransfer.getVal(Tc.DESTINATION_OBJ_ID).toString());
			DbDataObject orgUnit = svr.getObjectById(destinationOrgUnitId, svCONST.OBJECT_TYPE_ORG_UNITS, null);
			if (orgUnit == null) {
				orgUnit = svr.getObjectById(destinationOrgUnitId, SvReader.getTypeIdByName(Tc.HOLDING), null);
				if (orgUnit == null) {
					destinationOrgUnitId = 0L;
				}
			}
		}
	}

	public Long getLongValuePerField(DbDataObject dbo, String fieldName, SvReader svr) throws SvException {
		if (dbo == null) {
			return null;
		}
		DbDataObject dboTable = SvReader.getDbt(dbo.getObject_type());
		Long value = null;
		DbDataObject dboField = null;
		ArrayList<DbDataObject> dboFieldList = svr
				.getObjectsByParentId(dboTable.getObject_id(), svCONST.OBJECT_TYPE_FIELD, null, 0, 0).getItems();
		for (DbDataObject tempDboField : dboFieldList) {
			if (tempDboField.getVal(Tc.FIELD_NAME).equals(fieldName)) {
				dboField = tempDboField;
				break;
			}
		}
		Object fieldValue = dbo.getVal(fieldName);
		if (fieldValue != null) {
			switch (dboField.getVal(Tc.FIELD_TYPE).toString()) {
			case Tc.DATE:
			case Tc.TIMESTAMP:
			case Tc.DATETIME:
				DateTime dtValue = new DateTime(fieldValue.toString());
				value = dtValue.getMillis();
				break;
			case Tc.NUMERIC:
				value = (Long) fieldValue;
				break;
			default:
				break;
			}
		}
		return value;
	}

	/**
	 * Method used for sorting by certain field of type numeric, nvarchar and
	 * date/timestamp
	 * 
	 * @param arr
	 * @param fieldName
	 * @param svr
	 * @return
	 * @throws SvException
	 */
	public ArrayList<DbDataObject> sortDescendingDbDataArray(ArrayList<DbDataObject> arr, String fieldName,
			SvReader svr) throws SvException {
		ArrayList<DbDataObject> arrDboSortedItems = arr;
		if (!arr.isEmpty()) {
			arrDboSortedItems.sort(Comparator.comparingLong((DbDataObject dbo) -> {
				try {
					return getLongValuePerField(dbo, fieldName, svr);
				} catch (SvException e) {
					log4j.error(e);
				}
				return 0;
			}));
		}
		return arrDboSortedItems;
	}

	public JsonObject getCustomTableFormDataPerStrayPetLocation(Long parentId, String locationReason,
			Long currentHoldingId, SvReader svr) throws SvException {
		DbDataObject dboPet = svr.getObjectById(parentId, SvReader.getTypeIdByName(Tc.PET), null);
		DbDataObject dboHolding = svr.getObjectById(dboPet.getParent_id(), SvReader.getTypeIdByName(Tc.HOLDING), null);
		DbDataObject dboCurrentHolding = svr.getObjectById(currentHoldingId, SvReader.getTypeIdByName(Tc.HOLDING),
				null);
		JsonObject jObj = new JsonObject();
		jObj.addProperty(Tc.PARENT_ID, parentId);
		jObj.addProperty(Tc.LOCATION_REASON, locationReason);
		if (dboCurrentHolding != null) {
			dboHolding = dboCurrentHolding;
		}
		JsonObject jObjPetLocation = new JsonObject();
		jObjPetLocation.addProperty(Tc.REGION_CODE, dboHolding.getVal(Tc.REGION_CODE).toString());
		jObjPetLocation.addProperty(Tc.MUNIC_CODE, dboHolding.getVal(Tc.MUNIC_CODE).toString());
		jObjPetLocation.addProperty(Tc.COMMUN_CODE, dboHolding.getVal(Tc.COMMUN_CODE).toString());
		jObjPetLocation.addProperty(Tc.VILLAGE_CODE, dboHolding.getVal(Tc.VILLAGE_CODE).toString());
		jObj.add("stray_pet_location.basic_info", jObjPetLocation);
		return jObj;
	}

	public void getInboxMessagesThroughLink(DbDataObject dboUser, DbDataArray finalMessageList, SvReader svr)
			throws SvException {
		// get ASSIGN_TO messages through link
		getInboxMessagePerUser(dboUser, Tc.MSG_TO, finalMessageList, svr);
		// get ASSIGN_CC messages through link
		getInboxMessagePerUser(dboUser, Tc.MSG_CC, finalMessageList, svr);
		// get ASSIGN_BCC messages through link
		getInboxMessagePerUser(dboUser, Tc.MSG_BCC, finalMessageList, svr);
	}

	public void getInboxMessagePerUser(DbDataObject dboUser, String linkCode, DbDataArray finalMessageList,
			SvReader svr) throws SvException {
		DbDataObject dbLinkMessageAndUser = SvReader.getLinkType(linkCode, SvReader.getTypeIdByName(Tc.MESSAGE),
				svCONST.OBJECT_TYPE_USER);
		DbDataArray linkedTo = svr.getObjectsByLinkedId(dboUser.getObject_id(), svCONST.OBJECT_TYPE_USER,
				dbLinkMessageAndUser, SvReader.getTypeIdByName(Tc.MESSAGE), true, null, 0, 0);
		if (linkedTo != null && !linkedTo.getItems().isEmpty()) {
			for (DbDataObject dbo : linkedTo.getItems()) {
				if (dbo.getStatus().equals(Tc.VALID)) {
					finalMessageList.addDataItem(dbo);
				}
			}
		}
	}

	/**
	 * Method that return array of subjects by criteria
	 * 
	 * @param subjectTitle      Title of the subject
	 * @param subjectCategory   Category of subject
	 * @param subjectSearchType Criteria (subject title or subject category)
	 * @param svr               SvReader instance
	 * @return
	 * @throws SvException
	 */
	public DbDataArray getSubjectsByCriterium(String subjectTitle, String subjectCategory, String subjectSearchType,
			SvReader svr) throws SvException {
		DbDataArray dbArrSubjects = null;
		switch (subjectSearchType) {
		case Tc.TITLE:
			dbArrSubjects = findDataPerSingleFilter(Tc.TITLE, subjectTitle, DbCompareOperand.EQUAL,
					SvReader.getTypeIdByName(Tc.SUBJECT), svr);
			break;
		case Tc.CATEGORY:
			dbArrSubjects = findDataPerSingleFilter(Tc.CATEGORY, subjectCategory, DbCompareOperand.EQUAL,
					SvReader.getTypeIdByName(Tc.SUBJECT), svr);
			break;
		default:
			break;
		}
		return dbArrSubjects;
	}

	// Method for msgTo,msgCC,msgBcc assign to users processing
	public List<Long> convertStringIntoLongList(String s) {
		List<Long> result = new ArrayList<Long>();
		if (s != null && !s.trim().equals("")) {
			s = s.replace("[", "");
			s = s.replace("]", "");
			s = s.trim();
			if (!s.equals("")) {
				String[] s_list = s.split(",");
				for (String tempStr : s_list) {
					result.add(Long.valueOf(tempStr.trim()));
				}
			}
		}
		return result;
	}

	/**
	 * Method that search messages by subject (title/category) and messages
	 * (priority) criteria. Additionally, the user can see the messages only if has
	 * one of this links with message/s (MSG_TO/MSG_CC/MSG_BCC)
	 * 
	 * @param dboUser         Current user
	 * @param subjectTitle    Title of the subject
	 * @param subjectCategory Category of the subject
	 * @param messagePriority Priority of the message
	 * @param svr             SvReader instance
	 * @return
	 * @throws SvException
	 */
	public DbDataArray getSubjectsBySubjectAndMessageCriteria(DbDataObject dboUser, String subjectTitle,
			String subjectCategory, String subjectPriority, String messageText, SvReader svr) throws SvException {
		DbDataObject dboSubjectDesc = SvReader.getDbt(SvReader.getTypeIdByName(Tc.SUBJECT));
		DbDataObject dboMessageDesc = SvReader.getDbt(SvReader.getTypeIdByName(Tc.MESSAGE));
		String cr1 = " and tbl0.title ilike " + "'%" + subjectTitle + "%'";
		String cr2 = " and tbl0.category = " + "'" + subjectCategory + "'";
		String cr3 = " and tbl0.priority = " + "'" + subjectPriority + "'";
		String cr4 = " and tbl1.text ilike " + "'%" + messageText + "%'";
		if (subjectTitle == null || subjectTitle.trim().equals("")) {
			cr1 = Tc.EMPTY_STRING;
		}
		if (subjectCategory == null || subjectCategory.trim().equals("")) {
			cr2 = Tc.EMPTY_STRING;
		}
		if (subjectPriority == null || subjectPriority.trim().equals("")) {
			cr3 = Tc.EMPTY_STRING;
		}
		if (messageText == null || messageText.trim().equals("")) {
			cr4 = Tc.EMPTY_STRING;
		}
		DbQueryObject dqoSubject = new DbQueryObject(dboSubjectDesc, null, DbJoinType.INNER, null,
				LinkType.CUSTOM_FREETEXT, null, null);
		dqoSubject.setCustomFreeTextJoin(" on tbl0.object_id = tbl1.parent_id" + cr1 + cr2 + cr3 + cr4);
		DbQueryObject dqoMessage = new DbQueryObject(dboMessageDesc, null, DbJoinType.INNER, null,
				LinkType.CUSTOM_FREETEXT, null, null);
		DbQueryExpression dbqe = new DbQueryExpression();
		dbqe.addItem(dqoSubject);
		dbqe.addItem(dqoMessage);
		DbDataArray dbArrQuery = svr.getObjects(dbqe, 0, 0);
		DbDataArray dbArrMessages = new DbDataArray();
		if (dbArrQuery != null && !dbArrQuery.getItems().isEmpty()) {
			for (DbDataObject dboLinkedObject : dbArrQuery.getItems()) {
				dboMessageDesc = svr.getObjectById(
						Long.valueOf(dboLinkedObject.getVal("TBL1_" + Tc.OBJECT_ID).toString()),
						SvReader.getTypeIdByName(Tc.MESSAGE), null);
				dboSubjectDesc = svr.getObjectById(
						Long.valueOf(dboLinkedObject.getVal("TBL0_" + Tc.OBJECT_ID).toString()),
						SvReader.getTypeIdByName(Tc.SUBJECT), null);
				if (checkIfLinkExists(dboMessageDesc, dboUser, Tc.MSG_TO, null, svr)
						|| checkIfLinkExists(dboMessageDesc, dboUser, Tc.MSG_CC, null, svr)
						|| checkIfLinkExists(dboMessageDesc, dboUser, Tc.MSG_BCC, null, svr)
						|| dboMessageDesc.getVal(Tc.CREATED_BY).equals(dboUser.getObject_id())) {
					dbArrMessages.addDataItem(dboSubjectDesc);
				}
			}
		}
		return dbArrMessages;
	}

	/**
	 * Method that return array of user/s linked to message (TO/CC/BCC)
	 * 
	 * @param dboMessage DbDataObject of message
	 * @param linkType   Type of link (TO/CC/BCC)
	 * @param svr        SvReader instance
	 * @return
	 * @throws SvException
	 */
	public DbDataArray getUsersLinkedToMessage(DbDataObject dboMessage, String linkType, SvReader svr)
			throws SvException {
		DbDataArray dbArrUsers = new DbDataArray();
		DbDataObject dbLink = SvLink.getLinkType(linkType, SvReader.getTypeIdByName(Tc.MESSAGE),
				svCONST.OBJECT_TYPE_USER);
		dbArrUsers = svr.getObjectsByLinkedId(dboMessage.getObject_id(), SvReader.getTypeIdByName(Tc.MESSAGE), dbLink,
				svCONST.OBJECT_TYPE_USER, false, null, 0, 0);

		return dbArrUsers;
	}

	/**
	 * Method that return array of user/s linked to message
	 * 
	 * 
	 * @param dboMessage Message object
	 * @param json       Json object
	 * @param svr        SvReader instance
	 * @return
	 * @throws SvException
	 */
	public DbDataArray getUsersLinkedToMessage(DbDataObject dboMessage, JsonObject json, SvReader svr)
			throws SvException {
		DbDataArray dbArrMsgTo = new DbDataArray();
		dbArrMsgTo = getUsersLinkedToMessage(dboMessage, Tc.MSG_TO, svr);
		return dbArrMsgTo;
	}

	/**
	 * Method that return users linked to org unit.
	 * 
	 * @param orgUnitObjId Org unit object id
	 * @param svr          SvReader instance
	 * @return
	 * @throws SvException
	 */
	public DbDataArray getUsersLinkedToOrgUnit(Long orgUnitObjId, SvReader svr) throws SvException {
		DbDataArray dbArrUsers = new DbDataArray();
		DbDataObject dbLink = SvLink.getLinkType(Tc.POA, svCONST.OBJECT_TYPE_USER, svCONST.OBJECT_TYPE_ORG_UNITS);
		dbArrUsers = svr.getObjectsByLinkedId(orgUnitObjId, svCONST.OBJECT_TYPE_ORG_UNITS, dbLink,
				svCONST.OBJECT_TYPE_USER, true, null, 0, 0);
		return dbArrUsers;
	}

	/**
	 * Method that return translated object type
	 * 
	 * @param objType
	 * @param svr
	 * @return
	 */
	public String getTableNameByType(Long objType, SvReader svr) {
		String result = Tc.EMPTY_STRING;
		try {
			DbDataObject dbo = svr.getObjectById(objType, svCONST.OBJECT_TYPE_TABLE, null);
			if (dbo != null) {
				result = dbo.getVal(Tc.TABLE_NAME).toString().toUpperCase();
			}
		} catch (SvException e) {
			log4j.trace("Failed to translate");
		}
		return result;
	}

	/**
	 * Method that return VALID subjects sent by user or ARCHIVED subjects
	 * 
	 * @param dboUser       DbDataObject of user
	 * @param subjectStatus Status of subject
	 * @param svr           SvReader instance
	 * @return
	 * @throws SvException
	 */
	public DbDataArray getSentOrArchiveSubjects(DbDataObject dboUser, String subjectStatus, SvReader svr)
			throws SvException {
		DbDataArray dbArrSubjects = new DbDataArray();
		DbDataObject dboSubjectDesc = SvReader.getDbt(SvReader.getTypeIdByName(Tc.SUBJECT));
		DbDataObject dboMessageDesc = SvReader.getDbt(SvReader.getTypeIdByName(Tc.MESSAGE));
		String cr1 = " and tbl1.created_by =" + dboUser.getObject_id();
		String cr2 = " and tbl0.status =" + "'" + subjectStatus + "'";
		DbQueryObject dqoSubject = new DbQueryObject(dboSubjectDesc, null, DbJoinType.INNER, null,
				LinkType.CUSTOM_FREETEXT, null, null);
		if (subjectStatus.equals(Tc.VALID)) {
			dqoSubject.setCustomFreeTextJoin(" on tbl0.object_id = tbl1.parent_id" + cr1 + cr2);
		} else {
			dqoSubject.setCustomFreeTextJoin(" on tbl0.object_id = tbl1.parent_id" + cr2);
		}
		DbQueryObject dqoMessage = new DbQueryObject(dboMessageDesc, null, DbJoinType.INNER, null,
				LinkType.CUSTOM_FREETEXT, null, null);
		DbQueryExpression dbqe = new DbQueryExpression();
		dbqe.addItem(dqoSubject);
		dbqe.addItem(dqoMessage);
		DbDataArray dbArrQuery = svr.getObjects(dbqe, 0, 0);
		if (dbArrQuery != null && !dbArrQuery.getItems().isEmpty()) {
			for (DbDataObject dboLinkedObject : dbArrQuery.getItems()) {
				dboSubjectDesc = svr.getObjectById(
						Long.valueOf(dboLinkedObject.getVal("TBL0_" + Tc.OBJECT_ID).toString()),
						SvReader.getTypeIdByName(Tc.SUBJECT), null);
				dbArrSubjects.addDataItem(dboSubjectDesc);
			}
		}
		return dbArrSubjects;
	}

	/**
	 * Method that get MESSAGE/s by criteria (TO/CC/BCC)
	 * 
	 * @param dboUser DbDataObject of user
	 * @param svr     SvReader instance
	 * @return
	 * @throws SvException
	 */
	public DbDataArray getMessagesByCriteria(DbDataObject dboUser, SvReader svr) throws SvException {
		DbDataArray finalMessageList = new DbDataArray();
		// get messages through link
		getInboxMessagesThroughLink(dboUser, finalMessageList, svr);
		return finalMessageList;
	}

	/**
	 * Method that remove duplicates from array of subjects
	 * 
	 * @param dbArrSubjects DbDataArray of subjects
	 * @return
	 */
	public DbDataArray removeDuplicatesFromDbDataArray(DbDataArray dbArrSubjects) {
		DbDataArray dbArrFinal = new DbDataArray();
		HashSet<DbDataObject> hs = new HashSet<>();
		if (dbArrSubjects != null && !dbArrSubjects.getItems().isEmpty()) {
			for (DbDataObject dboSubject : dbArrSubjects.getItems()) {
				hs.add(dboSubject);
			}
			if (hs != null && !hs.isEmpty()) {
				for (DbDataObject dbo : hs) {
					dbArrFinal.addDataItem(dbo);
				}
			}
		}
		return dbArrFinal;
	}

	/**
	 * Method that put SUBJECT/s in array with status VALID
	 * 
	 * @param dbArrMessages Array or messages
	 * @param svr           SvReader instance
	 * @return
	 * @throws SvException
	 */
	public DbDataArray getSubjectsInArray(DbDataArray dbArrMessages, SvReader svr) throws SvException {
		DbDataArray dbArrSubjects = new DbDataArray();
		if (dbArrMessages != null && !dbArrMessages.getItems().isEmpty()) {
			for (DbDataObject dboTempMessage : dbArrMessages.getItems()) {
				DbDataObject dboSubject = svr.getObjectById(dboTempMessage.getParent_id(),
						SvReader.getTypeIdByName(Tc.SUBJECT), null);
				if (dboSubject != null && dboSubject.getStatus().equals(Tc.VALID)) {
					dbArrSubjects.addDataItem(dboSubject);
				}
			}
		}
		return dbArrSubjects;
	}

	/**
	 * Method that get INBOX subjects in appropriate JSON format
	 * 
	 * @param dbArrFinal DbDataArray of messages
	 * @param dboUser    DbDataObject of user
	 * @param svr        SvReader instance
	 * @return
	 * @throws SvException
	 */
	public JsonArray getInboxSubjectsWithMessageRecipientInfo(DbDataArray dbArrFinal, DbDataObject dboUser,
			SvReader svr) throws SvException {
		DbDataArray dbArrMsgTo = new DbDataArray();
		JsonArray jsonArray = new JsonArray();
		DbDataObject dboMessage = new DbDataObject();
		if (dbArrFinal != null && !dbArrFinal.getItems().isEmpty()) {
			for (DbDataObject dbo : dbArrFinal.getItems()) {
				JsonObject json = new JsonObject();
				json.add(Tc.SUBJECT, dbo.toSimpleJson());
				DbDataArray dbMessages = svr.getObjectsByParentId(dbo.getObject_id(),
						SvReader.getTypeIdByName(Tc.MESSAGE), null, 0, 0);
				if (dbMessages != null && !dbMessages.getItems().isEmpty()) {
					if (dbMessages.size() == 1) {
						dboMessage = dbMessages.get(0);
					} else if (dbMessages.size() > 1) {
						dboMessage = dbMessages.get(dbMessages.size() - 1);
					}
					if (checkIfLinkExists(dboMessage, dboUser, Tc.MSG_TO, null, svr)
							|| checkIfLinkExists(dboMessage, dboUser, Tc.MSG_BCC, null, svr)
							|| dboMessage.getVal(Tc.CREATED_BY).equals(dboUser.getObject_id())) {
						dbArrMsgTo = getUsersLinkedToMessage(dboMessage, json, svr);
						json.add(Tc.MSG_TO, dbArrMsgTo.toSimpleJson());
						json.add(Tc.MSG_CC, getUsersLinkedToMessage(dboMessage, Tc.MSG_CC, svr).toSimpleJson());
						json.add(Tc.MSG_BCC, getUsersLinkedToMessage(dboMessage, Tc.MSG_BCC, svr).toSimpleJson());
					}
					if (checkIfLinkExists(dboMessage, dboUser, Tc.MSG_CC, null, svr)) {
						dbArrMsgTo = getUsersLinkedToMessage(dboMessage, json, svr);
						json.add(Tc.MSG_TO, dbArrMsgTo.toSimpleJson());
					}
					json.addProperty(Tc.CREATED_BY_USERNAME, dboMessage.getVal(Tc.CREATED_BY_USERNAME).toString());
					json.addProperty(Tc.DATE_OF_CREATION, dboMessage.getDt_insert().toString());
					json.addProperty(Tc.LINK_STATUS,
							getStatusOfLinkObjectBetweenMessageAndUser(dbMessages, dboUser.getObject_id(), svr));
				}
				jsonArray.add(json);
			}
		}
		return jsonArray;
	}

	/**
	 * Method that get SENT or ARCHIVED subjects in appropriate JSON format
	 *
	 * @param dbArrFinal DbDataArray of subjects
	 * @param svr        SvReader instance
	 * @return
	 * @throws SvException
	 */
	public JsonArray getSentOrArchivedSubjectsWithMessageRecipientInfo(DbDataArray dbArrFinal, DbDataObject dboUser,
			SvReader svr) throws SvException {
		DbDataObject dboMessage = new DbDataObject();
		DbDataArray dbArrMsgTo = new DbDataArray();
		JsonArray jsonArray = new JsonArray();
		if (dbArrFinal != null && !dbArrFinal.getItems().isEmpty()) {
			for (DbDataObject dbo : dbArrFinal.getItems()) {
				JsonObject json = new JsonObject();
				json.add(Tc.SUBJECT, dbo.toSimpleJson());
				DbDataArray dbArrMessages = svr.getObjectsByParentId(dbo.getObject_id(),
						SvReader.getTypeIdByName(Tc.MESSAGE), null, 0, 0);
				if (dbArrMessages != null && !dbArrMessages.getItems().isEmpty()) {
					if (dbArrMessages.size() == 1) {
						dboMessage = dbArrMessages.get(0);
					} else if (dbArrMessages.size() > 1) {
						dboMessage = dbArrMessages.get(dbArrMessages.size() - 1);
					}
					dbArrMsgTo = getUsersLinkedToMessage(dboMessage, json, svr);
					json.add(Tc.MSG_TO, dbArrMsgTo.toSimpleJson());
					json.add(Tc.MSG_CC, getUsersLinkedToMessage(dboMessage, Tc.MSG_CC, svr).toSimpleJson());
					json.add(Tc.MSG_BCC, getUsersLinkedToMessage(dboMessage, Tc.MSG_BCC, svr).toSimpleJson());
					json.addProperty(Tc.CREATED_BY_USERNAME, dboMessage.getVal(Tc.CREATED_BY_USERNAME).toString());
					json.addProperty(Tc.DATE_OF_CREATION, dboMessage.getDt_insert().toString());
					json.addProperty(Tc.LINK_STATUS,
							getStatusOfLinkObjectBetweenMessageAndUser(dbArrMessages, dboUser.getObject_id(), svr));
				}
				jsonArray.add(json);
			}
		}
		return jsonArray;
	}

	/**
	 * Method that return number of links between message and user with status
	 * UNSEEN
	 * 
	 * @param dboUser DbDataObject of user
	 * @param svr     SvReader instance
	 * @return
	 * @throws SvException
	 */
	public int getNumberOfUnreadMessagesPerUser(DbDataObject dboUser, SvReader svr) throws SvException {
		DbDataArray dbArr = new DbDataArray();
		DbSearchCriterion cr1 = new DbSearchCriterion(Tc.STATUS, DbCompareOperand.EQUAL, Tc.UNSEEN);
		DbSearchCriterion cr2 = new DbSearchCriterion(Tc.LINK_OBJ_ID_2, DbCompareOperand.EQUAL, dboUser.getObject_id());
		dbArr = svr.getObjects(new DbSearchExpression().addDbSearchItem(cr1).addDbSearchItem(cr2),
				svCONST.OBJECT_TYPE_LINK, null, 0, 0);
		return dbArr.size();
	}

	/**
	 * Method that return org units per org unit type
	 * 
	 * @param orgUnitType Org unit type
	 * @param svr         SvReader instance
	 * @return
	 * @throws SvException
	 */
	public DbDataArray searchForOrgUnit(String orgUnitType, SvReader svr) throws SvException {
		DbSearchCriterion cr1 = new DbSearchCriterion(Tc.ORG_UNIT_TYPE, DbCompareOperand.EQUAL, orgUnitType);
		DbSearchExpression dbs = new DbSearchExpression().addDbSearchItem(cr1);
		return svr.getObjects(dbs, svCONST.OBJECT_TYPE_ORG_UNITS, null, 0, 0);
	}

	/**
	 * Method that return status of link between message and user
	 * 
	 * @param dbArrMessages DbDataArray of messages in subject
	 * @param dboUserObjId  Object id of user
	 * @param svr           SvReader instance
	 * @return
	 * @throws SvException
	 */
	public String getStatusOfLinkObjectBetweenMessageAndUser(DbDataArray dbArrMessages, Long dboUserObjId, SvReader svr)
			throws SvException {
		String result = Tc.VALID;
		DbDataArray dbArr = null;
		for (DbDataObject dboMessage : dbArrMessages.getItems()) {
			DbSearchCriterion cr1 = new DbSearchCriterion(Tc.LINK_OBJ_ID_1, DbCompareOperand.EQUAL,
					dboMessage.getObject_id());
			DbSearchCriterion cr2 = new DbSearchCriterion(Tc.LINK_OBJ_ID_2, DbCompareOperand.EQUAL, dboUserObjId);
			DbSearchCriterion cr3 = new DbSearchCriterion(Tc.STATUS, DbCompareOperand.EQUAL, Tc.UNSEEN);
			dbArr = svr.getObjects(
					new DbSearchExpression().addDbSearchItem(cr1).addDbSearchItem(cr2).addDbSearchItem(cr3),
					svCONST.OBJECT_TYPE_LINK, new DateTime(), 0, 0);
			if (dbArr != null && !dbArr.getItems().isEmpty()) {
				result = Tc.UNSEEN;
			}
		}
		return result;
	}

	public DbDataObject getSvFormTypeByTitle(String title, SvReader svr) throws SvException {
		DbDataObject questionnaire = null;
		DbSearchCriterion cr1 = new DbSearchCriterion(Tc.LABEL_CODE, DbCompareOperand.EQUAL, title);
		DbDataArray result = svr.getObjects(new DbSearchExpression().addDbSearchItem(cr1),
				svCONST.OBJECT_TYPE_FORM_TYPE, null, 0, 0);
		if (result != null && !result.getItems().isEmpty()) {
			questionnaire = result.get(0);
		}
		return questionnaire;
	}

	public DbDataArray getDbArrayOfSvFormTypeByParentTypeId(Long parentTypeId, SvReader svr) throws SvException {
		DbSearchCriterion cr1 = new DbSearchCriterion(Tc.PARENT_ID, DbCompareOperand.EQUAL, parentTypeId);
		return svr.getObjects(new DbSearchExpression().addDbSearchItem(cr1), svCONST.OBJECT_TYPE_FORM_TYPE, null, 0, 0);
	}

	public DbDataArray getDbArrayOfSvFormFieldTypeLinkedToSvFormType(Long questionnaireObjId, SvReader svr)
			throws SvException {
		DbDataObject dbLinkFormTypeFormFieldType = SvReader.getLinkType(Tc.FORM_FIELD_LINK,
				svCONST.OBJECT_TYPE_FORM_TYPE, svCONST.OBJECT_TYPE_FORM_FIELD_TYPE);
		return svr.getObjectsByLinkedId(questionnaireObjId, svCONST.OBJECT_TYPE_FORM_TYPE, dbLinkFormTypeFormFieldType,
				svCONST.OBJECT_TYPE_FORM_FIELD_TYPE, false, null, 0, 0);
	}

	/**
	 * Method that prepare JSON schema for questionnaire
	 * 
	 * @param formType       DbDataObject of questionnaire
	 * @param formFieldTypes DbDataArray of questions
	 * @param svr            SvReader instance
	 * @return
	 * @throws SvException
	 */
	public JsonObject prepareJsonSchemaForQuestionnaire(DbDataObject formType, DbDataArray formFieldTypes, SvReader svr)
			throws SvException {
		JsonObject jData = new JsonObject();
		CodeList cl = null;
		if (svr != null)
			cl = new CodeList(svr);
		Long linkTypeId = SvCore.getTypeIdByName(Tc.SVAROG_CODES);
		DbSearchCriterion critM = new DbSearchCriterion(Tc.CODE_VALUE, DbCompareOperand.EQUAL,
				Tc.NUMERIC_YES_NO_WITHOUT_CHOOSE);
		DbDataArray vData = svr.getObjects(critM, linkTypeId, null, 0, 0);
		if (vData != null && !vData.getItems().isEmpty()) {
			cl = new CodeList(svr);
			HashMap<String, String> listMap;
			listMap = cl.getCodeList(SvConf.getDefaultLocale(), vData.get(0).getObject_id(), true);
			Iterator<Entry<String, String>> it = listMap.entrySet().iterator();
			StringBuilder strBtmp = new StringBuilder("[");
			StringBuilder strBtmp1 = new StringBuilder("[");
			String prefix = "";
			while (it.hasNext()) {
				Entry<String, String> pair = it.next();
				strBtmp.append(prefix);
				strBtmp1.append(prefix);
				strBtmp.append(pair.getKey());
				strBtmp1.append(pair.getValue());
				prefix = ",";
				it.remove();
			}
			strBtmp.append(']');
			strBtmp1.append(']');
			String translatedQuestionnaireTitle = getLabelCodeTextOrDescValue(formType.getVal(Tc.LABEL_CODE).toString(),
					svr);
			jData.addProperty("title",
					I18n.getText(svr.getUserLocaleId(svr.getInstanceUser()), translatedQuestionnaireTitle));
			jData.addProperty("type", "object");
			JsonObject propertiesObject = new JsonObject();
			JsonArray jsonArray = new JsonArray();
			for (DbDataObject tempObject : formFieldTypes.getItems()) {
				String fullLabelCodeWithPrefix = tempObject.getVal(Tc.LABEL_CODE).toString();
				String labelCode = fullLabelCodeWithPrefix.substring(18, fullLabelCodeWithPrefix.length());
				if (tempObject.getVal(Tc.LABEL_CODE) != null) {
					JsonObject itemObject = new JsonObject();
					if (formType.getVal(Tc.FORM_CATEGORY).toString().equals(Tc.COMPLEX_QUESTIONNAIRE)) {
						DbDataArray dbArrSvParam = svr.getObjectsByParentId(tempObject.getObject_id(),
								svCONST.OBJECT_TYPE_PARAM, null, 0, 0);
						if (dbArrSvParam != null && !dbArrSvParam.getItems().isEmpty()) {
							for (DbDataObject dboSvParam : dbArrSvParam.getItems()) {
								DbDataArray dbArrSvParamValues = svr.getObjectsByParentId(dboSvParam.getObject_id(),
										svCONST.OBJECT_TYPE_PARAM_VALUE, null, 0, 0);
								if (dbArrSvParamValues != null && !dbArrSvParamValues.getItems().isEmpty()) {
									DbDataObject dboSvParamValue = dbArrSvParamValues.get(0);
									if (dboSvParamValue.getVal(Tc.VALUE).toString().equals("2")) {
										itemObject.addProperty("type", "array");
									}
									if (dboSvParamValue.getVal(Tc.VALUE).toString().equals("1")) {
										itemObject.addProperty("type", "string");
									}
									if (dboSvParamValue.getVal(Tc.VALUE).toString().equalsIgnoreCase("short")
											|| dboSvParamValue.getVal(Tc.VALUE).toString().equalsIgnoreCase("long")) {
										itemObject.addProperty("type", "string");
										itemObject.addProperty("maxLength",
												Integer.valueOf(tempObject.getVal(Tc.FIELD_SIZE).toString()));
									}
									if (dboSvParamValue.getVal(Tc.VALUE).toString().equalsIgnoreCase("true")) {
										jsonArray.add(new JsonPrimitive(labelCode));
									}
								}
							}
						}
					}
					if (formType.getVal(Tc.FORM_CATEGORY).toString().equals(Tc.SIMPLE_QUESTIONNAIRE)) {
						itemObject.addProperty("type", "number");
						DbDataArray dbArrSvParam = svr.getObjectsByParentId(tempObject.getObject_id(),
								svCONST.OBJECT_TYPE_PARAM, null, 0, 0);
						if (dbArrSvParam != null && !dbArrSvParam.getItems().isEmpty()) {
							DbDataObject dboSvParam = dbArrSvParam.get(0);
							DbDataArray dbArrSvParamValues = svr.getObjectsByParentId(dboSvParam.getObject_id(),
									svCONST.OBJECT_TYPE_PARAM_VALUE, null, 0, 0);
							if (dbArrSvParamValues != null && !dbArrSvParamValues.getItems().isEmpty()) {
								DbDataObject dboSvParamValue = dbArrSvParamValues.get(0);
								if (dboSvParamValue.getVal(Tc.VALUE).toString().equalsIgnoreCase("true")) {
									jsonArray.add(new JsonPrimitive(labelCode));
								}
							}
						}
					}
					String translatedQuestion = getLabelCodeTextOrDescValue(tempObject.getVal(Tc.LABEL_CODE).toString(),
							svr);
					itemObject.addProperty("title", translatedQuestion);
					itemObject.addProperty("category", formType.getVal(Tc.FORM_CATEGORY).toString());
					propertiesObject.add(labelCode, itemObject);
				}
			}
			jData.add("properties", propertiesObject);
			jData.add("required", jsonArray);
		}
		if (cl != null) {
			cl.release();
		}
		return jData;
	}

	public Long getCodeList(String codeValue, SvReader svr) throws SvException {
		Long codeListId = null;
		DbSearchCriterion cr1 = new DbSearchCriterion(Tc.CODE_VALUE, DbCompareOperand.EQUAL, codeValue);
		DbDataArray dbArrCodes = svr.getObjects(new DbSearchExpression().addDbSearchItem(cr1),
				SvReader.getTypeIdByName(Tc.SVAROG_CODES), null, 0, 0);
		if (dbArrCodes != null && !dbArrCodes.getItems().isEmpty()) {
			codeListId = dbArrCodes.get(0).getObject_id();
		}
		return codeListId;
	}

	public JsonObject prepareJsonSchemaForEditedQuestionnaire(DbDataObject formType, DbDataArray formFieldTypes,
			SvReader svr) throws SvException {
		JsonObject jData = new JsonObject();
		CodeList cl = null;
		if (svr != null)
			cl = new CodeList(svr);
		Long linkTypeId = SvCore.getTypeIdByName(Tc.SVAROG_CODES);
		DbSearchCriterion critM = new DbSearchCriterion(Tc.CODE_VALUE, DbCompareOperand.EQUAL,
				Tc.NUMERIC_YES_NO_WITHOUT_CHOOSE);
		DbDataArray vData = svr.getObjects(critM, linkTypeId, null, 0, 0);
		if (vData != null && !vData.getItems().isEmpty()) {
			cl = new CodeList(svr);
			HashMap<String, String> listMap;
			listMap = cl.getCodeList(SvConf.getDefaultLocale(), vData.get(0).getObject_id(), true);
			Iterator<Entry<String, String>> it = listMap.entrySet().iterator();
			StringBuilder strBtmp = new StringBuilder("[");
			StringBuilder strBtmp1 = new StringBuilder("[");
			String prefix = "";
			while (it.hasNext()) {
				Entry<String, String> pair = it.next();
				strBtmp.append(prefix);
				strBtmp1.append(prefix);
				strBtmp.append(pair.getKey());
				strBtmp1.append(pair.getValue());
				prefix = ",";
				it.remove();
			}
			strBtmp.append(']');
			strBtmp1.append(']');
			String translatedQuestionnaireTitle = getLabelCodeTextOrDescValue(formType.getVal(Tc.LABEL_CODE).toString(),
					svr);
			jData.addProperty("title",
					I18n.getText(svr.getUserLocaleId(svr.getInstanceUser()), translatedQuestionnaireTitle));
			jData.addProperty("type", "object");
			JsonObject propertiesObject = new JsonObject();
			for (DbDataObject tempObject : formFieldTypes.getItems()) {
				String fullLabelCodeWithPrefix = tempObject.getVal(Tc.LABEL_CODE).toString();
				String labelCode = fullLabelCodeWithPrefix.substring(18, fullLabelCodeWithPrefix.length());
				JsonObject current1 = new JsonObject();
				current1.addProperty("type", "string");
				JsonObject current2 = new JsonObject();
				current2.addProperty("type", "boolean");

				if (tempObject.getVal(Tc.LABEL_CODE) != null) {
					JsonObject itemObject = new JsonObject();
					String translatedQuestion = getLabelCodeTextOrDescValue(tempObject.getVal(Tc.LABEL_CODE).toString(),
							svr);
					itemObject.addProperty("title", translatedQuestion);
					itemObject.addProperty("category", formType.getVal(Tc.FORM_CATEGORY).toString());
					itemObject.addProperty("type", "object");
					JsonObject current = new JsonObject();
					current.add("New question", current1);
					current.add(Tc.SHOULD_DELETE, current2);
					itemObject.add("properties", current);
					propertiesObject.add(labelCode, itemObject);
				}
			}
			jData.add("properties", propertiesObject);
		}
		return jData;
	}

	/**
	 * Method that return all questionnaires
	 * 
	 * @param svr SvReader instance
	 * @return
	 * @throws SvException
	 */
	public DbDataArray getSvFormTypes(SvReader svr) throws SvException {
		DbSearchExpression dbs = new DbSearchExpression().addDbSearchItem(
				new DbSearchCriterion(Tc.OBJECT_TYPE, DbCompareOperand.EQUAL, svCONST.OBJECT_TYPE_FORM_TYPE));
		return svr.getObjects(dbs, svCONST.OBJECT_TYPE_FORM_TYPE, null, 0, 0);
	}

	/**
	 * Method that return the number of questions in a questionnaire
	 * 
	 * @param dboQuestionnaire DbDataObject of questionnaire
	 * @param svr              SvReader instance
	 * @return
	 * @throws SvException
	 */
	public int getNumberOfQuestionsInAQuestionnaire(DbDataObject dboQuestionnaire, SvReader svr) throws SvException {
		int numberOfQuestionsInAQuestionnaire = 0;
		if (dboQuestionnaire != null) {
			DbDataObject dbLinkQuestionnaireAndQuestion = SvReader.getLinkType(Tc.FORM_FIELD_LINK,
					svCONST.OBJECT_TYPE_FORM_TYPE, svCONST.OBJECT_TYPE_FORM_FIELD_TYPE);
			DbDataArray dbArrAllItems = svr.getObjectsByLinkedId(dboQuestionnaire.getObject_id(),
					svCONST.OBJECT_TYPE_FORM_TYPE, dbLinkQuestionnaireAndQuestion, svCONST.OBJECT_TYPE_FORM_FIELD_TYPE,
					false, null, 0, 0);
			if (dbArrAllItems != null && !dbArrAllItems.getItems().isEmpty()) {
				numberOfQuestionsInAQuestionnaire = dbArrAllItems.size();
			}
		}
		return numberOfQuestionsInAQuestionnaire;
	}

	public DbDataArray getQuestionsInAQuestionnaire(DbDataObject dboQuestionnaire, SvReader svr) throws SvException {
		DbDataArray dbArrAllItems = null;
		if (dboQuestionnaire != null) {
			DbDataObject dbLinkQuestionnaireAndQuestion = SvReader.getLinkType(Tc.FORM_FIELD_LINK,
					svCONST.OBJECT_TYPE_FORM_TYPE, svCONST.OBJECT_TYPE_FORM_FIELD_TYPE);
			dbArrAllItems = svr.getObjectsByLinkedId(dboQuestionnaire.getObject_id(), svCONST.OBJECT_TYPE_FORM_TYPE,
					dbLinkQuestionnaireAndQuestion, svCONST.OBJECT_TYPE_FORM_FIELD_TYPE, false, null, 0, 0);
		}
		return dbArrAllItems;
	}

	/**
	 * Method that return questionnaire creators username
	 * 
	 * @param dboQuestionnaire DbDataObejct of questionnaire
	 * @param svr              SvReader instance
	 * @return
	 * @throws SvException
	 */
	public String getUsersUserbaneByUserId(DbDataObject dboQuestionnaire, SvReader svr) throws SvException {
		String userName = Tc.EMPTY_STRING;
		if (dboQuestionnaire != null) {
			DbDataObject dboUser = svr.getObjectById(dboQuestionnaire.getUser_id(), svCONST.OBJECT_TYPE_USER, null);
			if (dboUser != null) {
				userName = dboUser.getVal(Tc.USER_NAME).toString();
			}
		}
		return userName;
	}

	/**
	 * Method that return list of links between message and user with status UNSEEN
	 * 
	 * @param dboUser DbDataObject of user
	 * @param svr     SvReader instance
	 * @return
	 * @throws SvException
	 */
	public DbDataArray getListOfUnreadMessagesPerUser(DbDataObject dboUser, SvReader svr) throws SvException {
		DbSearchCriterion cr1 = new DbSearchCriterion(Tc.STATUS, DbCompareOperand.EQUAL, Tc.UNSEEN);
		DbSearchCriterion cr2 = new DbSearchCriterion(Tc.LINK_OBJ_ID_2, DbCompareOperand.EQUAL, dboUser.getObject_id());
		return svr.getObjects(new DbSearchExpression().addDbSearchItem(cr1).addDbSearchItem(cr2),
				svCONST.OBJECT_TYPE_LINK, null, 0, 0);
	}

	/**
	 * Method that return list answers to question
	 * 
	 * @param questionObjId Object id of DbDataObject of question
	 * @param svr           SvReader instance
	 * @return
	 * @throws SvException
	 */
	public DbDataArray getAnswerToQuestion(Long questionObjId, Long formObjId, SvReader svr) throws SvException {
		DbSearchCriterion cr1 = new DbSearchCriterion(Tc.FIELD_TYPE_ID, DbCompareOperand.EQUAL, questionObjId);
		DbSearchCriterion cr2 = new DbSearchCriterion(Tc.FORM_OBJECT_ID, DbCompareOperand.EQUAL, formObjId);
		return svr.getObjects(new DbSearchExpression().addDbSearchItem(cr1).addDbSearchItem(cr2),
				svCONST.OBJECT_TYPE_FORM_FIELD, null, 0, 0);
	}

	/**
	 * Method that return object id of DbDataObject of question by question label
	 * code
	 * 
	 * @param labelCode String question
	 * @param svr       SvReader instance
	 * @return
	 * @throws SvException
	 */
	public Long getQuestionObjIdByLabelCode(String labelCode, SvReader svr) throws SvException {
		DbDataArray dbArrQuestions = null;
		Long questionObjId = null;
		DbSearchCriterion cr = new DbSearchCriterion(Tc.LABEL_CODE, DbCompareOperand.EQUAL,
				"naits.form_labels." + labelCode);
		dbArrQuestions = svr.getObjects(new DbSearchExpression().addDbSearchItem(cr),
				svCONST.OBJECT_TYPE_FORM_FIELD_TYPE, null, 0, 0);
		if (dbArrQuestions != null && !dbArrQuestions.getItems().isEmpty()) {
			questionObjId = dbArrQuestions.get(0).getObject_id();
		}
		return questionObjId;
	}

	public Long getSvLocaleObjid(String localeId, SvReader svr) throws SvException {
		Long localeObjId = 0L;
		DbSearchCriterion cr1 = new DbSearchCriterion(Tc.LOCALE_ID, DbCompareOperand.EQUAL, localeId);
		DbSearchExpression dbs = new DbSearchExpression();
		DbDataArray dbArr = svr.getObjects(dbs.addDbSearchItem(cr1), svCONST.OBJECT_TYPE_LOCALE, null, 0, 0);
		if (dbArr != null && !dbArr.getItems().isEmpty()) {
			localeObjId = dbArr.get(0).getObject_id();
		}
		return localeObjId;
	}

	public String getLabelCodeTextOrDescValue(String labelCode, SvReader svr) throws SvException {
		String translatedLabelCode = Tc.EMPTY_STRING;
		DbSearchCriterion cr = new DbSearchCriterion(Tc.LABEL_CODE, DbCompareOperand.EQUAL, labelCode);
		DbDataArray dbArrCodes = svr.getObjects(new DbSearchExpression().addDbSearchItem(cr), svCONST.OBJECT_TYPE_LABEL,
				null, 0, 0);
		if (dbArrCodes != null && !dbArrCodes.getItems().isEmpty()) {
			translatedLabelCode = dbArrCodes.get(0).getVal(Tc.LABEL_TEXT).toString();
			if (translatedLabelCode.length() == 199) {
				translatedLabelCode = dbArrCodes.get(0).getVal(Tc.LABEL_DESCR).toString();
			}
		}
		return translatedLabelCode;
	}

	public String getLocaleIdByLabelCode(String labelCode, SvReader svr) throws SvException {
		String localeId = Tc.EMPTY_STRING;
		DbSearchCriterion cr = new DbSearchCriterion(Tc.LABEL_CODE, DbCompareOperand.EQUAL, labelCode);
		DbDataArray dbArrCodes = svr.getObjects(new DbSearchExpression().addDbSearchItem(cr), svCONST.OBJECT_TYPE_LABEL,
				null, 0, 0);
		if (dbArrCodes != null && !dbArrCodes.getItems().isEmpty()) {
			localeId = dbArrCodes.get(0).getVal(Tc.LOCALE_ID).toString();
		}
		return localeId;
	}

	public String getDboAnimalCountry(DbDataObject dbo) {
		return getDboAnimalCountry(dbo, false);
	}

	public String getDboAnimalCountry(DbDataObject dbo, Boolean isCountryOfOrigin) {
		String result = Tc.EMPTY_STRING;
		if (dbo != null) {
			if (isCountryOfOrigin) {
				if (dbo.getVal(Tc.COUNTRY_OLD_ID) != null) {
					result = dbo.getVal(Tc.COUNTRY_OLD_ID).toString();
				}
			} else {
				if (dbo.getVal(Tc.COUNTRY) != null) {
					result = dbo.getVal(Tc.COUNTRY).toString();
				}
			}
		}
		return result;
	}

	public DbDataArray getAllQuestionOptionsByQuestionTranslatedLabelCode(String questionTranslatedLabelCode,
			SvReader svr) throws SvException {
		DbSearchCriterion cr1 = new DbSearchCriterion(Tc.LABEL_CODE, DbCompareOperand.LIKE,
				questionTranslatedLabelCode + Tc.PERCENT_OPERATOR);
		DbSearchExpression dbs = new DbSearchExpression();
		DbDataArray dbArr = svr.getObjects(dbs.addDbSearchItem(cr1), svCONST.OBJECT_TYPE_CODE, null, 0, 0);
		return dbArr;
	}

	public StringBuilder exportLabelsForComplexQuestionnaire(DbDataObject dboQuestion, StringBuilder sb, SvReader svr)
			throws SvException {
		String newFileLine = Tc.EMPTY_STRING;
		String isMandatory = Tc.EMPTY_STRING;
		String typeOfQuestionnaire = Tc.EMPTY_STRING;
		DbDataArray dbArrSvParam = svr.getObjectsByParentId(dboQuestion.getObject_id(), svCONST.OBJECT_TYPE_PARAM, null,
				0, 0);
		if (dbArrSvParam != null && !dbArrSvParam.getItems().isEmpty()) {
			for (DbDataObject dboSvParam : dbArrSvParam.getItems()) {
				DbDataArray dbArrSvParamValues = svr.getObjectsByParentId(dboSvParam.getObject_id(),
						svCONST.OBJECT_TYPE_PARAM_VALUE, null, 0, 0);
				if (dbArrSvParamValues != null && !dbArrSvParamValues.getItems().isEmpty()) {
					DbDataObject dboSvParamValue = dbArrSvParamValues.get(0);
					if (dboSvParamValue.getVal(Tc.VALUE).toString().equalsIgnoreCase("true")
							|| dboSvParamValue.getVal(Tc.VALUE).toString().equalsIgnoreCase("false")) {
						isMandatory = dboSvParamValue.getVal(Tc.VALUE).toString();
					}
					if (dboSvParamValue.getVal(Tc.VALUE).toString().equalsIgnoreCase("short")
							|| dboSvParamValue.getVal(Tc.VALUE).toString().equalsIgnoreCase("long")) {
						typeOfQuestionnaire = dboSvParamValue.getVal(Tc.VALUE).toString();
					}
					if (dboSvParamValue.getVal(Tc.VALUE).toString().equalsIgnoreCase("1")) {
						typeOfQuestionnaire = dboSvParamValue.getVal(Tc.VALUE).toString();
					}
					if (dboSvParamValue.getVal(Tc.VALUE).toString().equalsIgnoreCase("2")) {
						typeOfQuestionnaire = dboSvParamValue.getVal(Tc.VALUE).toString();
					}
				}
			}
			DbDataObject fftScore = getFftScoreForAnswer(dboQuestion.getObject_id(), svr);
			DbDataArray fftScores = getFftScoresForAnswer(dboQuestion.getObject_id(), svr);
			ArrayList<DbDataObject> items = fftScores.getSortedItems(Tc.PKID, true);
			fftScores = new DbDataArray();
			for (int i = items.size(); --i >= 0;) {
				fftScores.addDataItem(items.get(i));
			}
			if (typeOfQuestionnaire.equalsIgnoreCase("2")) {
				newFileLine = new StringBuilder(Tc.qq).append(";").append(dboQuestion.getVal(Tc.LABEL_CODE).toString())
						.append(";")
						.append(getLabelCodeTextOrDescValue(dboQuestion.getVal(Tc.LABEL_CODE).toString(), svr))
						.append(";").append(isMandatory).append(";").append(typeOfQuestionnaire).append(Tc.NEW_LINE)
						.toString();
			} else {
				if (fftScore == null) {
					newFileLine = new StringBuilder(Tc.qq).append(";")
							.append(dboQuestion.getVal(Tc.LABEL_CODE).toString()).append(";")
							.append(getLabelCodeTextOrDescValue(dboQuestion.getVal(Tc.LABEL_CODE).toString(), svr))
							.append(";").append(isMandatory).append(";").append(typeOfQuestionnaire).append(Tc.NEW_LINE)
							.toString();
				} else if (fftScore.getVal(Tc.CLI_LABEL) != null) {
					newFileLine = new StringBuilder(Tc.qq).append(";")
							.append(dboQuestion.getVal(Tc.LABEL_CODE).toString()).append(";")
							.append(getLabelCodeTextOrDescValue(dboQuestion.getVal(Tc.LABEL_CODE).toString(), svr))
							.append(";").append(isMandatory).append(";").append(typeOfQuestionnaire).append(";")
							.append(fftScore.getVal(Tc.SCORE).toString()).append(";")
							.append(fftScore.getVal(Tc.CLI_LABEL).toString()).append(Tc.NEW_LINE).toString();
				} else {
					newFileLine = new StringBuilder(Tc.qq).append(";")
							.append(dboQuestion.getVal(Tc.LABEL_CODE).toString()).append(";")
							.append(getLabelCodeTextOrDescValue(dboQuestion.getVal(Tc.LABEL_CODE).toString(), svr))
							.append(";").append(isMandatory).append(";").append(typeOfQuestionnaire).append(";")
							.append(fftScore.getVal(Tc.SCORE).toString()).append(Tc.NEW_LINE).toString();
				}
			}
			if (!newFileLine.isEmpty()) {
				sb.append(newFileLine);
			}
			DbDataArray dbArrQuestionOptions = getAllQuestionOptionsByQuestionLabelCode(
					dboQuestion.getVal(Tc.LABEL_CODE).toString(), svr);
			for (DbDataObject dboTemp : dbArrQuestionOptions.getItems()) {
				if (typeOfQuestionnaire.equalsIgnoreCase("2") && fftScores != null && !fftScores.getItems().isEmpty()) {
					for (DbDataObject tempFftScore : fftScores.getItems()) {
						if (tempFftScore.getVal(Tc.CLI_LABEL).toString()
								.equalsIgnoreCase(dboTemp.getVal(Tc.CODE_VALUE).toString())) {
							sb.append(Tc.opt).append(";").append(dboTemp.getVal(Tc.LABEL_CODE).toString()).append(";")
									.append(dboTemp.getVal(Tc.CODE_VALUE).toString()).append(";")
									.append(fftScore.getVal(Tc.SCORE).toString()).append(Tc.NEW_LINE);
							// break;
						} else {
							sb.append(Tc.opt).append(";").append(dboTemp.getVal(Tc.LABEL_CODE).toString()).append(";")
									.append(dboTemp.getVal(Tc.CODE_VALUE).toString()).append(";").append("null")
									.append(Tc.NEW_LINE);
							// break;
						}
						break;
					}
				} else {
					sb.append(Tc.opt).append(";").append(dboTemp.getVal(Tc.LABEL_CODE).toString()).append(";")
							.append(dboTemp.getVal(Tc.CODE_VALUE).toString()).append(Tc.NEW_LINE);
				}
			}
		}
		return sb;
	}

	public StringBuilder exportLabelsForSimpleQuestionnaire(DbDataObject dboQuestion, StringBuilder sb, SvReader svr)
			throws SvException {
		String newFileLine = Tc.EMPTY_STRING;
		String isMandatory = Tc.EMPTY_STRING;
		DbDataArray dbArrSvParam = svr.getObjectsByParentId(dboQuestion.getObject_id(), svCONST.OBJECT_TYPE_PARAM, null,
				0, 0);
		if (dbArrSvParam != null && !dbArrSvParam.getItems().isEmpty()) {
			DbDataObject dboSvParam = dbArrSvParam.get(0);
			DbDataArray dbArrSvParamValues = svr.getObjectsByParentId(dboSvParam.getObject_id(),
					svCONST.OBJECT_TYPE_PARAM_VALUE, null, 0, 0);
			if (dbArrSvParamValues != null && !dbArrSvParamValues.getItems().isEmpty()) {
				DbDataObject dboSvParamValue = dbArrSvParamValues.get(0);
				isMandatory = dboSvParamValue.getVal(Tc.VALUE).toString();
			}
		}
		DbDataObject fftScore = getFftScoreForAnswer(dboQuestion.getObject_id(), svr);
		if (fftScore != null) {
			newFileLine = new StringBuilder(Tc.qq).append(";").append(dboQuestion.getVal(Tc.LABEL_CODE).toString())
					.append(";").append(getLabelCodeTextOrDescValue(dboQuestion.getVal(Tc.LABEL_CODE).toString(), svr))
					.append(";").append(isMandatory).append(";").append(fftScore.getVal(Tc.SCORE).toString())
					.append(";").append(fftScore.getVal(Tc.CLI_LABEL).toString()).append(Tc.NEW_LINE).toString();

		} else {
			newFileLine = new StringBuilder(Tc.qq).append(";").append(dboQuestion.getVal(Tc.LABEL_CODE).toString())
					.append(";").append(getLabelCodeTextOrDescValue(dboQuestion.getVal(Tc.LABEL_CODE).toString(), svr))
					.append(";").append(isMandatory).append(Tc.NEW_LINE).toString();
		}
		if (!newFileLine.isEmpty()) {
			sb.append(newFileLine);
		}
		return sb;
	}

	public String exportLabelsQuestionnairePart(DbDataObject dboQuestionnaire, SvReader svr) throws SvException {
		String translatedObjectType = getTableNameByType(dboQuestionnaire.getParent_id(), svr);
		String questionnairePart = new StringBuilder(Tc.qnr).append(";")
				.append(dboQuestionnaire.getVal(Tc.LABEL_CODE).toString()).append(";")
				.append(getLabelCodeTextOrDescValue(dboQuestionnaire.getVal(Tc.LABEL_CODE).toString(), svr)).append(";")
				.append(getLocaleIdByLabelCode(dboQuestionnaire.getVal(Tc.LABEL_CODE).toString(), svr)).append(";")
				.append(dboQuestionnaire.getVal(Tc.FORM_CATEGORY).toString()).append(";").append(translatedObjectType)
				.append(Tc.NEW_LINE).toString();
		return questionnairePart;
	}

	public void exportQuestionnaire(Long questionnaireObjId, java.io.OutputStream output, SvReader svr)
			throws SvException, IOException {
		StringBuilder sb = new StringBuilder();
		DbDataObject dboQuestionnaire = svr.getObjectById(questionnaireObjId, svCONST.OBJECT_TYPE_FORM_TYPE, null);
		if (dboQuestionnaire != null) {
			String questionnaire = exportLabelsQuestionnairePart(dboQuestionnaire, svr);
			DbDataArray dbArrQuestions = getQuestionsInAQuestionnaire(dboQuestionnaire, svr);
			if (dbArrQuestions != null && !dbArrQuestions.getItems().isEmpty()) {
				for (DbDataObject dboQuestion : dbArrQuestions.getItems()) {
					if (dboQuestionnaire.getVal(Tc.FORM_CATEGORY).toString().equals(Tc.SIMPLE_QUESTIONNAIRE)) {
						exportLabelsForSimpleQuestionnaire(dboQuestion, sb, svr);
					}
					if (dboQuestionnaire.getVal(Tc.FORM_CATEGORY).toString().equals(Tc.COMPLEX_QUESTIONNAIRE)) {
						exportLabelsForComplexQuestionnaire(dboQuestion, sb, svr);
					}
				}
				output.write(questionnaire.toString().getBytes());
				output.write(sb.toString().getBytes());
			}
		}
	}

	public DbDataObject getSvFormBySvFormType(Long formTypeId, Long parentId, SvReader svr) throws SvException {
		DbDataObject dboSvForm = null;
		DbSearchCriterion cr1 = new DbSearchCriterion(Tc.FORM_TYPE_ID, DbCompareOperand.EQUAL, formTypeId);
		DbSearchCriterion cr2 = new DbSearchCriterion(Tc.PARENT_ID, DbCompareOperand.EQUAL, parentId);
		DbDataArray dbArrSvFormType = svr.getObjects(new DbSearchExpression().addDbSearchItem(cr1).addDbSearchItem(cr2),
				svCONST.OBJECT_TYPE_FORM, null, 0, 0);
		if (dbArrSvFormType != null && !dbArrSvFormType.getItems().isEmpty()) {
			dboSvForm = dbArrSvFormType.get(0);
		}
		return dboSvForm;
	}

	public DbDataArray getAllHoldingKeepersByHoldingObjid(Long holdingObjId, SvReader svr) throws SvException {
		DbDataArray result = new DbDataArray();
		DbDataArray dbArrKeepers = new DbDataArray();
		DbDataArray dbArrLinksBetweenHoldingAndKeeper = new DbDataArray();
		DbDataObject linkType = SvLink.getLinkType(Tc.HOLDING_KEEPER, SvReader.getTypeIdByName(Tc.HOLDING),
				SvReader.getTypeIdByName(Tc.HOLDING_RESPONSIBLE));
		DbSearchCriterion cr1 = new DbSearchCriterion(Tc.LINK_OBJ_ID_1, DbCompareOperand.EQUAL, holdingObjId);
		DbSearchCriterion cr2 = new DbSearchCriterion(Tc.LINK_TYPE_ID, DbCompareOperand.EQUAL, linkType.getObject_id());
		dbArrLinksBetweenHoldingAndKeeper = svr.getObjects(
				new DbSearchExpression().addDbSearchItem(cr1).addDbSearchItem(cr2), svCONST.OBJECT_TYPE_LINK, null, 0,
				0);
		if (dbArrLinksBetweenHoldingAndKeeper != null && !dbArrLinksBetweenHoldingAndKeeper.getItems().isEmpty()) {
			dbArrLinksBetweenHoldingAndKeeper = svr.getObjectsHistory(
					new DbSearchExpression().addDbSearchItem(cr1).addDbSearchItem(cr2), svCONST.OBJECT_TYPE_LINK, 0, 0);
			dbArrLinksBetweenHoldingAndKeeper.getSortedItems(Tc.PKID, true);
			for (DbDataObject dboTempLink : dbArrLinksBetweenHoldingAndKeeper.getItems()) {
				DbDataObject dboKeeper = svr.getObjectById((Long) dboTempLink.getVal(Tc.LINK_OBJ_ID_2),
						SvReader.getTypeIdByName(Tc.HOLDING_RESPONSIBLE), null);
				dbArrKeepers.addDataItem(dboKeeper);
			}
			for (int i = dbArrKeepers.size() - 1; i >= 0; i--) {
				DbDataObject dbo = dbArrKeepers.get(i);
				result.addDataItem(dbo);
			}
		}
		return result;
	}

	public DbDataArray getAllAvailableAnimalsPerSelectedType(DbDataObject dboHerd, String animalType, SvReader svr)
			throws SvException {
		DbSearchCriterion cr1 = new DbSearchCriterion(Tc.STATUS, DbCompareOperand.EQUAL, Tc.VALID);
		DbSearchCriterion cr2 = new DbSearchCriterion(Tc.ANIMAL_CLASS, DbCompareOperand.EQUAL, animalType);
		DbSearchCriterion cr3 = new DbSearchCriterion(Tc.PARENT_ID, DbCompareOperand.EQUAL, dboHerd.getParent_id());
		// DbSearchCriterion cr4 = new DbSearchCriterion(Tc.IS_IN_HERD,
		// DbCompareOperand.ISNULL);
		return svr.getObjects(new DbSearchExpression().addDbSearchItem(cr1).addDbSearchItem(cr2).addDbSearchItem(cr3),
				SvReader.getTypeIdByName(Tc.ANIMAL), null, 1000, 0);
	}

	public DbDataObject getQuestionObjectByLabelCode(String labelCode, SvReader svr) throws SvException {
		DbDataArray dbArrQuestions = null;
		DbDataObject dboQuestion = null;
		DbSearchCriterion cr = new DbSearchCriterion(Tc.LABEL_CODE, DbCompareOperand.EQUAL, labelCode);
		dbArrQuestions = svr.getObjects(new DbSearchExpression().addDbSearchItem(cr),
				svCONST.OBJECT_TYPE_FORM_FIELD_TYPE, null, 0, 0);
		if (dbArrQuestions != null && !dbArrQuestions.getItems().isEmpty()) {
			dboQuestion = dbArrQuestions.get(0);
		}
		return dboQuestion;
	}

	public DbDataObject getCorrectAnswerOfQuestion(Long parentId, String cliLabel, Long fftId, SvReader svr)
			throws SvException {
		DbDataObject dbo = null;
		DbSearchCriterion cr1 = new DbSearchCriterion(Tc.CLI_LABEL, DbCompareOperand.EQUAL, cliLabel);
		DbSearchCriterion cr2 = new DbSearchCriterion(Tc.PARENT_ID, DbCompareOperand.EQUAL, parentId);
		DbSearchCriterion cr3 = new DbSearchCriterion(Tc.FFT_ID, DbCompareOperand.EQUAL, fftId);
		DbDataArray res = svr.getObjects(
				new DbSearchExpression().addDbSearchItem(cr1).addDbSearchItem(cr2).addDbSearchItem(cr3),
				SvReader.getTypeIdByName(Tc.FFT_SCORE), null, 0, 0);
		if (res != null && !res.getItems().isEmpty()) {
			dbo = res.get(0);
		}
		return dbo;
	}

	public DbDataObject getFftScorePerAnswer(Long fftId, Long parentId, SvReader svr) throws SvException {
		DbDataObject dbo = null;
		DbSearchCriterion cr1 = new DbSearchCriterion(Tc.FFT_ID, DbCompareOperand.EQUAL, fftId);
		DbSearchCriterion cr2 = new DbSearchCriterion(Tc.PARENT_ID, DbCompareOperand.EQUAL, parentId);
		DbDataArray res = svr.getObjects(new DbSearchExpression().addDbSearchItem(cr1).addDbSearchItem(cr2),
				SvReader.getTypeIdByName(Tc.FFT_SCORE), null, 0, 0);
		if (res != null && !res.getItems().isEmpty()) {
			dbo = res.get(0);
		}
		return dbo;
	}

	public DbDataObject getMovementDocumentPerAnimal(String movementDocId, SvReader svr) throws SvException {
		DbDataObject dbo = null;
		DbSearchCriterion cr1 = new DbSearchCriterion(Tc.MOVEMENT_DOC_ID, DbCompareOperand.EQUAL, movementDocId);
		DbSearchCriterion cr2 = new DbSearchCriterion(Tc.MOVEMENT_TYPE, DbCompareOperand.EQUAL, Tc.ANIMAL);
		DbDataArray res = svr.getObjects(new DbSearchExpression().addDbSearchItem(cr1).addDbSearchItem(cr2),
				SvReader.getTypeIdByName(Tc.MOVEMENT_DOC), null, 0, 0);
		if (res != null && !res.getItems().isEmpty()) {
			dbo = res.get(0);
		}
		return dbo;
	}

	public DbDataObject getAnswerPerQuestion(Long questionnaireObjId, Long parentId, Long questionObjId, SvReader svr)
			throws SvException {
		DbDataArray res = null;
		DbDataObject answer = null;
		DbDataObject dboSvForm = getSvFormBySvFormType(questionnaireObjId, parentId, svr);
		if (dboSvForm != null) {
			DbSearchCriterion cr1 = new DbSearchCriterion(Tc.FORM_OBJECT_ID, DbCompareOperand.EQUAL,
					dboSvForm.getObject_id());
			DbSearchCriterion cr2 = new DbSearchCriterion(Tc.FIELD_TYPE_ID, DbCompareOperand.EQUAL, questionObjId);
			res = svr.getObjects(new DbSearchExpression().addDbSearchItem(cr1).addDbSearchItem(cr2),
					SvReader.getTypeIdByName(Tc.SVAROG_FORM_FIELD), null, 0, 0);
		}
		if (res != null && !res.getItems().isEmpty()) {
			answer = res.get(res.size() - 1);
		}
		return answer;
	}

	public DbDataObject getFfScoreForAnswer(Long ffId, Long parentId, SvReader svr) throws SvException {
		DbDataObject dboFfScore = null;
		DbSearchCriterion cr1 = new DbSearchCriterion(Tc.FF_ID, DbCompareOperand.EQUAL, ffId);
		DbSearchCriterion cr2 = new DbSearchCriterion(Tc.PARENT_ID, DbCompareOperand.EQUAL, parentId);
		DbDataArray res = svr.getObjects(new DbSearchExpression().addDbSearchItem(cr1).addDbSearchItem(cr2),
				SvReader.getTypeIdByName(Tc.FF_SCORE), null, 0, 0);
		if (res != null && !res.getItems().isEmpty()) {
			dboFfScore = res.get(res.size() - 1);
		}
		return dboFfScore;
	}

	public DbDataArray getFftScoreForAnswer(Long fftId, Long parentId, String cliLabel, SvReader svr)
			throws SvException {
		DbSearchCriterion cr1 = new DbSearchCriterion(Tc.FFT_ID, DbCompareOperand.EQUAL, fftId);
		DbSearchCriterion cr2 = new DbSearchCriterion(Tc.PARENT_ID, DbCompareOperand.EQUAL, parentId);
		DbSearchCriterion cr3 = new DbSearchCriterion(Tc.CLI_LABEL, DbCompareOperand.EQUAL, cliLabel);
		DbDataArray res = svr.getObjects(
				new DbSearchExpression().addDbSearchItem(cr1).addDbSearchItem(cr2).addDbSearchItem(cr3),
				SvReader.getTypeIdByName(Tc.FFT_SCORE), null, 0, 0);
		return res;
	}

	public DbDataObject getFftScoreForAnswer(Long fftId, SvReader svr) throws SvException {
		DbDataObject dboFftScore = null;
		DbSearchCriterion cr = new DbSearchCriterion(Tc.FFT_ID, DbCompareOperand.EQUAL, fftId);
		DbDataArray res = svr.getObjects(new DbSearchExpression().addDbSearchItem(cr),
				SvReader.getTypeIdByName(Tc.FFT_SCORE), null, 0, 0);
		if (res != null && !res.getItems().isEmpty()) {
			dboFftScore = res.get(0);
		}
		return dboFftScore;
	}

	public DbDataArray getFftScoresForAnswer(Long fftId, SvReader svr) throws SvException {
		DbSearchCriterion cr = new DbSearchCriterion(Tc.FFT_ID, DbCompareOperand.EQUAL, fftId);
		return svr.getObjects(new DbSearchExpression().addDbSearchItem(cr), SvReader.getTypeIdByName(Tc.FFT_SCORE),
				null, 0, 0);

	}

	public DbDataArray getFfScoresForMultipleTypeOfQuestionnaire(Long ffId, Long parentId, SvReader svr)
			throws SvException {
		DbSearchCriterion cr1 = new DbSearchCriterion(Tc.FF_ID, DbCompareOperand.EQUAL, ffId);
		DbSearchCriterion cr2 = new DbSearchCriterion(Tc.PARENT_ID, DbCompareOperand.EQUAL, parentId);
		return svr.getObjects(new DbSearchExpression().addDbSearchItem(cr1).addDbSearchItem(cr2),
				SvReader.getTypeIdByName(Tc.FF_SCORE), null, 0, 0);
	}

	public Long calculateTotalAchievedScoreOfQuestionnaire(Long questionnaireObjId, Long parentId, SvReader svr)
			throws SvException {
		Long totalAchievedScore = 0L;
		if (questionnaireObjId != null && parentId != null) {
			DbDataObject dboSvForm = getSvFormBySvFormType(questionnaireObjId, parentId, svr);
			DbDataArray questionsInQuestionnaire = getDbArrayOfSvFormFieldTypeLinkedToSvFormType(questionnaireObjId,
					svr);
			if (questionsInQuestionnaire != null && !questionsInQuestionnaire.getItems().isEmpty()) {
				for (DbDataObject tempQuestion : questionsInQuestionnaire.getItems()) {
					DbDataObject answerForTempQuestion = getAnswerPerQuestion(questionnaireObjId, parentId,
							tempQuestion.getObject_id(), svr);
					if (answerForTempQuestion != null && dboSvForm != null) {
						DbDataObject ffScore = getFfScoreForAnswer(answerForTempQuestion.getObject_id(),
								dboSvForm.getObject_id(), svr);
						if (ffScore != null) {
							totalAchievedScore = totalAchievedScore + (Long) ffScore.getVal(Tc.SCORE);
						}
					}
				}
			}
		}
		return totalAchievedScore;
	}

	public Long calculateTotalScoreOfQuestionnaire(Long questionnaireObjId, SvReader svr) throws SvException {
		Long totalScore = 0L;
		if (questionnaireObjId != null) {
			DbDataArray questionsInQuestionnaire = getDbArrayOfSvFormFieldTypeLinkedToSvFormType(questionnaireObjId,
					svr);
			if (questionsInQuestionnaire != null && !questionsInQuestionnaire.getItems().isEmpty()) {
				for (DbDataObject tempQuestion : questionsInQuestionnaire.getItems()) {
					DbDataObject dboFftScore = getFftScoreForAnswer(tempQuestion.getObject_id(), svr);
					if (dboFftScore != null) {
						totalScore = totalScore + (Long) dboFftScore.getVal(Tc.MAX_SCORE);
					}
				}
			}
		}
		return totalScore;
	}

	public Long calculateTotalScoreForMultipleTypeOfQuestionnaire(Long questionnaireObjId, SvReader svr)
			throws SvException {
		Long totalScore = 0L;
		if (questionnaireObjId != null) {
			DbDataArray questionsInQuestionnaire = getDbArrayOfSvFormFieldTypeLinkedToSvFormType(questionnaireObjId,
					svr);
			if (questionsInQuestionnaire != null && !questionsInQuestionnaire.getItems().isEmpty()) {
				for (DbDataObject tempQuestion : questionsInQuestionnaire.getItems()) {
					DbDataArray dboFftScores = getFftScoresForAnswer(tempQuestion.getObject_id(), svr);
					if (dboFftScores != null && !dboFftScores.getItems().isEmpty()) {
						for (DbDataObject dboFftScore : dboFftScores.getItems()) {
							totalScore = totalScore + (Long) dboFftScore.getVal(Tc.SCORE);
						}

					}

				}
			}
		}
		return totalScore;
	}

	public Long calculateTotalAchievedScoreForMultipleTypeQuestionnaire(Long questionnaireObjId, Long parentId,
			SvReader svr) throws SvException {
		Long totalAchievedScore = 0L;
		if (questionnaireObjId != null && parentId != null) {
			DbDataObject dboSvForm = getSvFormBySvFormType(questionnaireObjId, parentId, svr);
			DbDataArray questionsInQuestionnaire = getDbArrayOfSvFormFieldTypeLinkedToSvFormType(questionnaireObjId,
					svr);
			if (questionsInQuestionnaire != null && !questionsInQuestionnaire.getItems().isEmpty()) {
				for (DbDataObject tempQuestion : questionsInQuestionnaire.getItems()) {
					DbDataObject answerForTempQuestion = getAnswerPerQuestion(questionnaireObjId, parentId,
							tempQuestion.getObject_id(), svr);
					if (answerForTempQuestion != null && dboSvForm != null) {
						DbDataArray ffScores = getFfScoresForMultipleTypeOfQuestionnaire(
								answerForTempQuestion.getObject_id(), dboSvForm.getObject_id(), svr);
						if (ffScores != null && !ffScores.getItems().isEmpty()) {
							for (DbDataObject ffScore : ffScores.getItems()) {
								totalAchievedScore = totalAchievedScore + (Long) ffScore.getVal(Tc.SCORE);
							}
						}
					}
				}
			}
		}
		return totalAchievedScore;
	}

	public DbDataObject getCodeListByLabelCode(String questionLabelCode, String labelCode, SvReader svr)
			throws SvException {
		DbDataObject dboCodeList = null;
		DbSearchCriterion cr1 = new DbSearchCriterion(Tc.LABEL_CODE, DbCompareOperand.EQUAL,
				questionLabelCode + "." + labelCode);
		DbDataArray dbArrCodes = svr.getObjects(new DbSearchExpression().addDbSearchItem(cr1),
				SvReader.getTypeIdByName(Tc.SVAROG_CODES), null, 0, 0);
		if (dbArrCodes != null && !dbArrCodes.getItems().isEmpty()) {
			dboCodeList = dbArrCodes.get(0);
		}
		return dboCodeList;
	}

	public DbDataObject getQuestionLabelByQuestion(String labelCode, SvReader svr) throws SvException {
		DbDataObject dboQuestion = null;
		DbSearchCriterion cr1 = new DbSearchCriterion(Tc.LABEL_CODE, DbCompareOperand.LIKE, labelCode);
		DbDataArray dbArr = svr.getObjects(new DbSearchExpression().addDbSearchItem(cr1),
				SvReader.getTypeIdByName(Tc.SVAROG_LABELS), null, 0, 0);
		if (dbArr != null && !dbArr.getItems().isEmpty()) {
			dboQuestion = dbArr.get(0);
		}
		return dboQuestion;
	}

	public DbDataArray getAllQuestionOptionsByQuestionLabelCode(String parentCodeValue, SvReader svr)
			throws SvException {
		DbSearchCriterion cr1 = new DbSearchCriterion(Tc.PARENT_CODE_VALUE, DbCompareOperand.EQUAL, parentCodeValue);
		DbSearchExpression dbs = new DbSearchExpression();
		DbDataArray dbArr = svr.getObjects(dbs.addDbSearchItem(cr1), svCONST.OBJECT_TYPE_CODE, null, 0, 0);
		return dbArr;
	}

	/**
	 * Method that prepares JsonArray for WS (getQuestionnairesByParentTypeId)
	 * 
	 * @param jsonArray        JsonArray
	 * @param dboQuestionnaire DbDataObject of questionnaire
	 * @param objectId         Object ID of parent (HOLDING / ANIMAL)
	 * @param svr              SvReader instance
	 * @throws SvException
	 */
	public void prepareJsonArrayForQuestionnairesByParentId(JsonArray jsonArray, DbDataObject dboQuestionnaire,
			Long objectId, SvReader svr) throws SvException {
		JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty(Tc.OBJECT_ID, dboQuestionnaire.getObject_id());
		jsonObject.addProperty(Tc.QUESTIONNAIRE_TYPE, dboQuestionnaire.getVal(Tc.FORM_CATEGORY).toString());
		jsonObject.addProperty(Tc.LABEL_CODE, dboQuestionnaire.getVal(Tc.LABEL_CODE).toString());
		jsonObject.addProperty(Tc.STATUS, dboQuestionnaire.getStatus());
		jsonObject.addProperty(Tc.QUESTIONNAIRE_TITLE,
				getLabelCodeTextOrDescValue(dboQuestionnaire.getVal(Tc.LABEL_CODE).toString(), svr));
		jsonObject.addProperty(Tc.SCOPE, getTableNameByType(dboQuestionnaire.getParent_id(), svr));
		jsonObject.addProperty(Tc.AUTOINSTANCE_SINGLE, dboQuestionnaire.getVal(Tc.AUTOINSTANCE_SINGLE).toString());
		jsonObject.addProperty(Tc.NUMBER_OF_QUESTIONS, getNumberOfQuestionsInAQuestionnaire(dboQuestionnaire, svr));
		if (objectId != null) {
			String achievedFromTotalScore = Tc.EMPTY_STRING;
			DbDataObject svForm = getSvFormBySvFormType(dboQuestionnaire.getObject_id(), objectId, svr);
			if (svForm != null) {
				jsonObject.addProperty(Tc.FORM_STATUS, svForm.getStatus().toString());
				DbDataArray dbArrSvParam = svr.getObjectsByParentId(svForm.getObject_id(), svCONST.OBJECT_TYPE_PARAM,
						null, 0, 0);
				if (dbArrSvParam != null && !dbArrSvParam.getItems().isEmpty()) {
					DbDataObject dboSvParam = dbArrSvParam.get(0);
					DbDataArray dbArrSvParamValues = svr.getObjectsByParentId(dboSvParam.getObject_id(),
							svCONST.OBJECT_TYPE_PARAM_VALUE, null, 0, 0);
					if (dbArrSvParamValues != null && !dbArrSvParamValues.getItems().isEmpty()) {
						DbDataObject dboSvParamValue = dbArrSvParamValues.get(0);
						achievedFromTotalScore = dboSvParamValue.getVal(Tc.VALUE).toString();
						jsonObject.addProperty(Tc.TOTAL_SCORE, achievedFromTotalScore);
					}
				}
			}
		}
		jsonArray.add(jsonObject);
	}

	/**
	 * Method that prepare JsonArray for WS (getQuestionnairesData)
	 * 
	 * @param jsonArray
	 * @param dboQuestionnaire DbDataObject of questionnaire
	 * @param svr              SvReader instance
	 * @throws SvException
	 */
	public void prepareJsonArrayForQuestionnaireData(JsonArray jsonArray, DbDataObject dboQuestionnaire, SvReader svr)
			throws SvException {
		JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty(Tc.OBJECT_ID, dboQuestionnaire.getObject_id());
		jsonObject.addProperty(Tc.LABEL_CODE, dboQuestionnaire.getVal(Tc.LABEL_CODE).toString());
		jsonObject.addProperty(Tc.DT_INSERT, dboQuestionnaire.getDt_insert().toString());
		jsonObject.addProperty(Tc.QUESTIONNAIRE_CREATOR, getUsersUserbaneByUserId(dboQuestionnaire, svr));
		jsonObject.addProperty(Tc.NUMBER_OF_QUESTIONS, getNumberOfQuestionsInAQuestionnaire(dboQuestionnaire, svr));
		jsonObject.addProperty(Tc.QUESTIONNAIRE_TITLE,
				getLabelCodeTextOrDescValue(dboQuestionnaire.getVal(Tc.LABEL_CODE).toString(), svr));
		jsonObject.addProperty(Tc.SCOPE, getTableNameByType(dboQuestionnaire.getParent_id(), svr));
		jsonArray.add(jsonObject);
	}

	public DbDataArray getQuestionAnswers(Long svFormFieldTypeObjId, SvReader svr) throws SvException {
		DbSearchCriterion cr2 = new DbSearchCriterion(Tc.FIELD_TYPE_ID, DbCompareOperand.EQUAL, svFormFieldTypeObjId);
		return svr.getObjects(new DbSearchExpression().addDbSearchItem(cr2),
				SvReader.getTypeIdByName(Tc.SVAROG_FORM_FIELD), null, 0, 0);
	}

	/**
	 * Method that prepare UI schema for complex questionnaire
	 * 
	 * @param jsonUISchema    JsonObject of ui schema
	 * @param svFormFieldType DbDataObject of question
	 * @param svr             SvReader instance
	 * @throws SvException
	 */
	public void prepareUiSchemaForComplexQuestionnaire(JsonObject jsonUISchema, DbDataObject svFormFieldType,
			SvReader svr) throws SvException {
		DbDataArray dbArrSvParam = svr.getObjectsByParentId(svFormFieldType.getObject_id(), svCONST.OBJECT_TYPE_PARAM,
				null, 0, 0);
		if (dbArrSvParam != null && !dbArrSvParam.getItems().isEmpty()) {
			for (DbDataObject dboSvParam : dbArrSvParam.getItems()) {
				DbDataArray dbArrSvParamValues = svr.getObjectsByParentId(dboSvParam.getObject_id(),
						svCONST.OBJECT_TYPE_PARAM_VALUE, null, 0, 0);
				if (dbArrSvParamValues != null && !dbArrSvParamValues.getItems().isEmpty()) {
					DbDataObject dboSvParamValue = dbArrSvParamValues.get(0);
					if (dboSvParamValue.getVal(Tc.VALUE).toString().equals("2")) {
						jsonUISchema.addProperty("ui:widget", "checkboxes");
					}
					if (dboSvParamValue.getVal(Tc.VALUE).toString().equals("1")) {
						jsonUISchema.addProperty("ui:widget", "radio");
					}
					if (dboSvParamValue.getVal(Tc.VALUE).toString().equalsIgnoreCase("long")
							&& svFormFieldType.getVal(Tc.FIELD_SIZE).equals(2000L)) {
						jsonUISchema.addProperty("ui:widget", "textarea");
					}
				}
			}
		}
	}

	public DbDataArray getSvFormField(Long svFormObjId, Long svFormFieldTypeObjId, SvReader svr) throws SvException {
		DbSearchCriterion cr1 = new DbSearchCriterion(Tc.FORM_OBJECT_ID, DbCompareOperand.EQUAL, svFormObjId);
		DbSearchCriterion cr2 = new DbSearchCriterion(Tc.FIELD_TYPE_ID, DbCompareOperand.EQUAL, svFormFieldTypeObjId);
		return svr.getObjects(new DbSearchExpression().addDbSearchItem(cr1).addDbSearchItem(cr2),
				SvReader.getTypeIdByName(Tc.SVAROG_FORM_FIELD), null, 0, 0);
	}

	/**
	 * Method for fetching municipalities that were targeted by certain Vaccination
	 * Event / Camapign
	 */
	public ArrayList<String> getCorrespondingDatesForStatisticalReportsComponent(String statisticalReportName,
			SvReader svr) throws SvException {
		Connection conn = null;
		ArrayList<String> dateResults = new ArrayList<>();
		// String schema = SvConf.getDefaultSchema();
		String sqlStmt = "";
		switch (statisticalReportName) {
		case "species":
			sqlStmt = "select calc_date from naits.stat_hold_anim group by calc_date";
			break;
		case "statuses":
			sqlStmt = "select calc_date from naits.stat_hold_anim_stat group by calc_date";
			break;
		case "ages":
			sqlStmt = "select calc_date from naits.stat_hold_anim_age group by calc_date";
			break;
		case "flocks":
			sqlStmt = "select calc_date from naits.stat_hold_anim_fl group by calc_date;";
			break;
		default:
			break;
		}
		if (!sqlStmt.equals("")) {
			ResultSet rs = null;
			conn = svr.dbGetConn();
			try (PreparedStatement selectStatement = conn.prepareStatement(sqlStmt.toUpperCase(),
					ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);) {
				rs = selectStatement.executeQuery();
				while (rs.next()) {
					dateResults.add(rs.getString("CALC_DATE"));
				}
			} catch (Exception e) {
				throw (new SvException("naits.error.getingSelectedResult", svr.getInstanceUser(), e));
			} finally {
				try {
					if (rs != null)
						rs.close();
					if (conn != null)
						conn.close();
				} catch (SQLException e) {
					log4j.error(new SvException("naits.error.getingSelectedResult", svr.getInstanceUser(), e));
					// throw (new
					// SvException("naits.error.getingSelectedResult",
					// svr.getInstanceUser(), e));
				}
			}
		}

		return dateResults;
	}

	public DbDataArray getTerminatedAnimals(Long holdingId, String dateFrom, String dateTo, int rowLimit, SvReader svr)
			throws SvException {
		DbDataArray terminatedAnimals = new DbDataArray();
		DbSearchExpression dbse1 = new DbSearchExpression();
		DbSearchExpression finalExpression = new DbSearchExpression();
		DbDataObject dboHolding = svr.getObjectById(holdingId, SvReader.getTypeIdByName(Tc.HOLDING), null);
		DbSearchCriterion dbcIfHoldingNotSlaghterhouse = null;
		DbSearchCriterion dbc1 = new DbSearchCriterion(Tc.STATUS, DbCompareOperand.EQUAL, Tc.POSTMORTEM);
		dbc1.setNextCritOperand(DbLogicOperand.OR.toString());
		DbSearchCriterion dbc2 = new DbSearchCriterion(Tc.STATUS, DbCompareOperand.EQUAL, Tc.DESTROYED);
		dbc2.setNextCritOperand(DbLogicOperand.OR.toString());
		DbSearchCriterion dbc3 = new DbSearchCriterion(Tc.STATUS, DbCompareOperand.EQUAL, Tc.EXPORTED);
		if (!dboHolding.getVal(Tc.TYPE).equals("7")) {
			dbc3.setNextCritOperand(DbLogicOperand.OR.toString());
			dbcIfHoldingNotSlaghterhouse = new DbSearchCriterion(Tc.STATUS, DbCompareOperand.EQUAL, Tc.SLAUGHTRD);
		}
		DbSearchCriterion dbc4 = new DbSearchCriterion(Tc.PARENT_ID, DbCompareOperand.EQUAL, holdingId);
		dbse1.addDbSearchItem(dbc1);
		dbse1.addDbSearchItem(dbc2);
		dbse1.addDbSearchItem(dbc3);
		if (dbcIfHoldingNotSlaghterhouse != null) {
			dbse1.addDbSearchItem(dbcIfHoldingNotSlaghterhouse);
		}
		dbse1.setNextCritOperand(DbLogicOperand.AND.toString());
		DbSearchCriterion dbc5 = null;
		finalExpression.addDbSearchItem(dbc4).addDbSearchItem(dbse1);
		if (dateFrom != null && !dateFrom.equals("null") && dateTo != null && !dateTo.equals("null")) {
			dbc5 = new DbSearchCriterion(Tc.DT_INSERT, DbCompareOperand.BETWEEN, new DateTime(dateFrom),
					new DateTime(dateTo));
			finalExpression.addDbSearchItem(dbc5);
		} else {
			rowLimit = 100;
		}
		DbQueryObject query = new DbQueryObject(SvReader.getDbtByName(Tc.ANIMAL), finalExpression, null, null);
		ArrayList<String> orderBy = new ArrayList<String>();
		orderBy.add(Tc.PKID + " " + Tc.DESC);
		query.setOrderByFields(orderBy);
		terminatedAnimals = svr.getObjects(query, rowLimit, 0);
		return terminatedAnimals;
	}

	public DbDataArray getTerminatedPets(Long holdingId, String dateFrom, String dateTo, int rowLimit, SvReader svr)
			throws SvException {
		DbDataArray terminatedPets = new DbDataArray();
		DbSearchExpression dbse = new DbSearchExpression();
		DbSearchExpression finalExpression = new DbSearchExpression();
		DbSearchCriterion dbc1 = new DbSearchCriterion(Tc.STATUS, DbCompareOperand.EQUAL, Tc.EUTHANIZED);
		dbc1.setNextCritOperand(DbLogicOperand.OR.toString());
		DbSearchCriterion dbc2 = new DbSearchCriterion(Tc.STATUS, DbCompareOperand.EQUAL, Tc.DIED);
		dbc2.setNextCritOperand(DbLogicOperand.OR.toString());
		DbSearchCriterion dbc3 = new DbSearchCriterion(Tc.STATUS, DbCompareOperand.EQUAL, Tc.DISPOSAL);
		dbc2.setNextCritOperand(DbLogicOperand.OR.toString());
		DbSearchCriterion dbc4 = new DbSearchCriterion(Tc.STATUS, DbCompareOperand.EQUAL, Tc.EXPORTED);
		dbse.addDbSearchItem(dbc1);
		dbse.addDbSearchItem(dbc2);
		dbse.addDbSearchItem(dbc3);
		dbse.addDbSearchItem(dbc4);
		dbse.setNextCritOperand(DbLogicOperand.AND.toString());
		DbSearchCriterion dbc5 = new DbSearchCriterion(Tc.PARENT_ID, DbCompareOperand.EQUAL, holdingId);
		finalExpression.addDbSearchItem(dbc5).addDbSearchItem(dbse);
		if (dateFrom != null && !dateFrom.equals("null") && dateTo != null && !dateTo.equals("null")) {
			DbSearchCriterion dbc6 = new DbSearchCriterion(Tc.DT_INSERT, DbCompareOperand.BETWEEN,
					new DateTime(dateFrom), new DateTime(dateTo));
			finalExpression.addDbSearchItem(dbc6);
		} else {
			rowLimit = 100;
		}
		DbQueryObject query = new DbQueryObject(SvReader.getDbtByName(Tc.PET), finalExpression, null, null);
		ArrayList<String> orderBy = new ArrayList<String>();
		orderBy.add(Tc.PKID + " " + Tc.DESC);
		query.setOrderByFields(orderBy);
		terminatedPets = svr.getObjects(query, rowLimit, 0);
		return terminatedPets;
	}

	public DbDataArray getReleasedPets(Long holdingId, String dateFrom, String dateTo, int rowLimit, SvReader svr)
			throws SvException {
		DbDataArray terminatedPets = new DbDataArray();
		DbSearchExpression dbse = new DbSearchExpression();
		DbSearchCriterion dbc1 = new DbSearchCriterion(Tc.STATUS, DbCompareOperand.EQUAL, Tc.RELEASED);
		DbSearchCriterion dbc2 = new DbSearchCriterion(Tc.PARENT_ID, DbCompareOperand.EQUAL, holdingId);
		dbse.addDbSearchItem(dbc1).addDbSearchItem(dbc2);
		if (dateFrom != null && !dateFrom.equals("null") && dateTo != null && !dateTo.equals("null")) {
			DbSearchCriterion dbc3 = new DbSearchCriterion(Tc.DT_INSERT, DbCompareOperand.BETWEEN,
					new DateTime(dateFrom), new DateTime(dateTo));
			dbse.addDbSearchItem(dbc3);
		} else {
			rowLimit = 100;
		}
		DbQueryObject query = new DbQueryObject(SvReader.getDbtByName(Tc.PET), dbse, null, null);
		ArrayList<String> orderBy = new ArrayList<String>();
		orderBy.add(Tc.PKID + " " + Tc.DESC);
		query.setOrderByFields(orderBy);
		terminatedPets = svr.getObjects(query, rowLimit, 0);
		return terminatedPets;
	}

	public DbDataArray getFinishedMovementDocuments(String destinationHoldingPic, String dateFrom, String dateTo,
			int rowLimit, SvReader svr) throws SvException {
		DbDataArray finishedMovementDocuments = new DbDataArray();
		DbSearchExpression finalExpression = new DbSearchExpression();
		DbSearchCriterion dbc1 = new DbSearchCriterion(Tc.STATUS, DbCompareOperand.NOTEQUAL, Tc.DRAFT);
		DbSearchCriterion dbc2 = new DbSearchCriterion(Tc.DESTINATION_HOLDING_PIC, DbCompareOperand.EQUAL,
				destinationHoldingPic);
		finalExpression.addDbSearchItem(dbc1).addDbSearchItem(dbc2);
		DbSearchCriterion dbc3 = null;
		if (dateFrom != null && !dateFrom.equals("null") && dateTo != null && !dateTo.equals("null")) {
			dbc3 = new DbSearchCriterion(Tc.DT_INSERT, DbCompareOperand.BETWEEN, new DateTime().minusDays(30),
					new DateTime());
			finalExpression.addDbSearchItem(dbc3);
		} else {
			rowLimit = 100;
		}
		DbQueryObject query = new DbQueryObject(SvReader.getDbtByName(Tc.MOVEMENT_DOC), finalExpression, null, null);
		ArrayList<String> orderBy = new ArrayList<String>();
		orderBy.add(Tc.PKID + " " + Tc.DESC);
		query.setOrderByFields(orderBy);
		finishedMovementDocuments = svr.getObjects(query, rowLimit, 0);

		return finishedMovementDocuments;
	}

	public DbDataArray getFinishedMovements(Long holdingObjId, String dateFrom, String dateTo, int rowLimit,
			SvReader svr) throws SvException {
		DbDataArray finishedMovements = new DbDataArray();
		DbDataObject dboHolding = svr.getObjectById(holdingObjId, SvReader.getTypeIdByName(Tc.HOLDING), null);
		if (dboHolding != null) {
			DbDataObject dboSvarogLink = SvReader.getDbt(svCONST.OBJECT_TYPE_LINK);
			DbDataObject dboHoldingDesc = SvReader.getDbt(SvReader.getTypeIdByName(Tc.HOLDING));
			DbDataObject dboAnimalMovementDesc = SvReader.getDbt(SvReader.getTypeIdByName(Tc.ANIMAL_MOVEMENT));
			DbDataObject dboLinkBetweenAnimalAndExportCertificate = SvReader.getLinkType(Tc.ANIMAL_MOVEMENT_HOLDING,
					dboHoldingDesc, dboAnimalMovementDesc);
			DbQueryObject dqoSvLink = new DbQueryObject(dboSvarogLink, null, DbJoinType.INNER, null,
					LinkType.CUSTOM_FREETEXT, null, null);
			dqoSvLink.setCustomFreeTextJoin(" on tbl0.link_obj_id_1 = tbl1.object_id and tbl0.link_type_id="
					+ dboLinkBetweenAnimalAndExportCertificate.getObject_id() + " and tbl1.object_id="
					+ dboHolding.getObject_id());
			DbQueryObject dqoHolding = new DbQueryObject(dboHoldingDesc, null, DbJoinType.INNER, null,
					LinkType.CUSTOM_FREETEXT, null, null);
			if (dateFrom != null && !dateFrom.equals("null") && dateTo != null && !dateTo.equals("null")) {
				dqoHolding.setCustomFreeTextJoin(
						" on tbl0.link_obj_id_2 = tbl2.object_id and tbl2.dt_insert between to_date('" + (dateFrom)
								+ "','YYYY-MM-DD') and to_date('" + dateTo + "', 'YYYY-MM-DD')  + interval '1' day");
			} else {
				dqoHolding.setCustomFreeTextJoin(" on tbl0.link_obj_id_2 = tbl2.object_id ");
				rowLimit = 100;
			}

			ArrayList<String> orderBy = new ArrayList<String>();
			orderBy.add("tbl2.PKID " + Tc.DESC);
			DbQueryObject dqoAnimalMovement = new DbQueryObject(dboAnimalMovementDesc, null, DbJoinType.INNER, null,
					LinkType.CUSTOM_FREETEXT, orderBy, null);
			dqoAnimalMovement.setCustomFreeTextJoin(" on tbl0.link_type_id = tbl3.object_id and status =" + Tc.VALID);
			DbQueryExpression dqe = new DbQueryExpression();
			dqe.addItem(dqoSvLink);
			dqe.addItem(dqoHolding);
			dqe.addItem(dqoAnimalMovement);
			dqe.setReturnType(dboAnimalMovementDesc);
			finishedMovements = svr.getObjects(dqe, 0, 0);
		}
		return finishedMovements;
	}

	public DbDataArray alignDbDataArrayToSpecificSize(DbDataArray dbArr, int rowLimit) {
		DbDataArray finalArr = new DbDataArray();
		ArrayList<DbDataObject> tempArr = dbArr.getItems();
		if (dbArr.size() > rowLimit) {
			tempArr.subList(rowLimit, tempArr.size()).clear();
			;
		}
		finalArr.setItems(tempArr);
		return finalArr;
	}

	public DbDataArray getFiltratedHoldings(String type, String name, String pic, String keeperId, String geostatCode,
			String address, int rowLimit, SvReader svr) throws SvException {
		DbDataArray result = new DbDataArray();
		DbSearchExpression dbse = new DbSearchExpression();
		if (type != null && !type.trim().equals("null")) {
			DbSearchCriterion dbc1 = new DbSearchCriterion(Tc.TYPE, DbCompareOperand.EQUAL, type);
			dbse.addDbSearchItem(dbc1);
		}
		if (name != null && !name.trim().equals("null")) {
			DbSearchCriterion dbc2 = new DbSearchCriterion(Tc.NAME, DbCompareOperand.ILIKE,
					Tc.PERCENT_OPERATOR + name + Tc.PERCENT_OPERATOR);
			dbse.addDbSearchItem(dbc2);
		}
		if (pic != null && !pic.trim().equals("null")) {
			DbSearchCriterion dbc3 = new DbSearchCriterion(Tc.PIC, DbCompareOperand.ILIKE,
					Tc.PERCENT_OPERATOR + pic + Tc.PERCENT_OPERATOR);
			dbse.addDbSearchItem(dbc3);
		}
		if (keeperId != null && !keeperId.trim().equals("null")) {
			DbSearchCriterion dbc4 = new DbSearchCriterion(Tc.KEEPER_ID, DbCompareOperand.LIKE, keeperId);
			dbse.addDbSearchItem(dbc4);
		}
		if (address != null && !address.trim().equals("null")) {
			DbSearchCriterion dbc5 = new DbSearchCriterion(Tc.PHYSICAL_ADDRESS, DbCompareOperand.ILIKE, address);
			dbse.addDbSearchItem(dbc5);
		}
		if (geostatCode != null && !geostatCode.trim().equals("null")) {
			String field_name = "";
			if (geostatCode.length() == 2) {
				field_name = Tc.REGION_CODE;
			} else if (geostatCode.length() == 4) {
				field_name = Tc.MUNIC_CODE;
			} else if (geostatCode.length() == 6) {
				field_name = Tc.COMMUN_CODE;
			} else if (geostatCode.length() == 8) {
				field_name = Tc.VILLAGE_CODE;
			}
			DbSearchCriterion dbc6 = new DbSearchCriterion(field_name, DbCompareOperand.EQUAL, geostatCode);
			dbse.addDbSearchItem(dbc6);
		}
		DbQueryObject query = new DbQueryObject(SvReader.getDbtByName(Tc.HOLDING), dbse, null, null);
		ArrayList<String> orderBy = new ArrayList<String>();
		orderBy.add(Tc.PKID + " " + Tc.DESC);
		query.setOrderByFields(orderBy);
		result = svr.getObjects(query, rowLimit, 0);
		return result;
	}

	public DbDataArray getFiltratedPersons(String idNo, String firstName, String lastName, String fullName,
			String geostatCode, String phoneNumber, int rowLimit, SvReader svr) throws SvException {
		DbDataArray result = new DbDataArray();
		DbSearchExpression dbse = new DbSearchExpression();
		if (idNo != null && !idNo.trim().equals("null")) {
			DbSearchCriterion dbc1 = new DbSearchCriterion(Tc.NAT_REG_NUMBER, DbCompareOperand.ILIKE,
					Tc.PERCENT_OPERATOR + idNo + Tc.PERCENT_OPERATOR);
			dbse.addDbSearchItem(dbc1);
		}
		if (firstName != null && !firstName.trim().equals("null")) {
			DbSearchCriterion dbc2 = new DbSearchCriterion(Tc.FIRST_NAME, DbCompareOperand.ILIKE,
					firstName + Tc.PERCENT_OPERATOR);
			dbse.addDbSearchItem(dbc2);
		}
		if (lastName != null && !lastName.trim().equals("null")) {
			DbSearchCriterion dbc3 = new DbSearchCriterion(Tc.LAST_NAME, DbCompareOperand.ILIKE,
					lastName + Tc.PERCENT_OPERATOR);
			dbse.addDbSearchItem(dbc3);
		}
		if (fullName != null && !fullName.trim().equals("null")) {
			DbSearchCriterion dbc4 = new DbSearchCriterion(Tc.FULL_NAME, DbCompareOperand.ILIKE,
					Tc.PERCENT_OPERATOR + fullName + Tc.PERCENT_OPERATOR);
			dbse.addDbSearchItem(dbc4);
		}
		if (geostatCode != null && !geostatCode.trim().equals("null")) {
			String field_name = "";
			if (geostatCode.length() == 2) {
				field_name = Tc.REGION_CODE;
			} else if (geostatCode.length() == 4) {
				field_name = Tc.MUNIC_CODE;
			} else if (geostatCode.length() == 6) {
				field_name = Tc.COMMUN_CODE;
			} else if (geostatCode.length() == 8) {
				field_name = Tc.VILLAGE_CODE;
			}
			DbSearchCriterion dbc6 = new DbSearchCriterion(field_name, DbCompareOperand.EQUAL, geostatCode);
			dbse.addDbSearchItem(dbc6);
		}
		if (phoneNumber != null && !phoneNumber.trim().equals("null")) {
			DbSearchExpression dbsePhones = new DbSearchExpression();
			DbSearchCriterion dbc5 = new DbSearchCriterion(Tc.PHONE_NUMBER, DbCompareOperand.LIKE,
					phoneNumber + Tc.PERCENT_OPERATOR);
			dbc5.setNextCritOperand(DbLogicOperand.OR.toString());
			DbSearchCriterion dbc6 = new DbSearchCriterion(Tc.MOBILE_NUMBER, DbCompareOperand.LIKE,
					phoneNumber + Tc.PERCENT_OPERATOR);
			dbsePhones.addDbSearchItem(dbc5).addDbSearchItem(dbc6);
			dbse.addDbSearchItem(dbsePhones);
		}
		DbQueryObject query = new DbQueryObject(SvReader.getDbtByName(Tc.HOLDING_RESPONSIBLE), dbse, null, null);
		ArrayList<String> orderBy = new ArrayList<String>();
		orderBy.add(Tc.PKID + " " + Tc.DESC);
		query.setOrderByFields(orderBy);
		result = svr.getObjects(query, rowLimit, 0);
		return result;
	}

	public DbDataArray getFiltratedAnimals(String animalId, String status, String animalClass, String breed,
			String color, String country, int rowLimit, SvReader svr) throws SvException {
		DbDataArray result = new DbDataArray();
		DbSearchExpression dbse = new DbSearchExpression();
		DbSearchCriterion dbc;
		if (animalId != null && !animalId.trim().equals("null")) {
			DbSearchExpression dbseAnimalIds = new DbSearchExpression();
			DbSearchCriterion dbc1 = new DbSearchCriterion(Tc.ANIMAL_ID, DbCompareOperand.ILIKE,
					Tc.PERCENT_OPERATOR + animalId + Tc.PERCENT_OPERATOR);
			dbc1.setNextCritOperand(DbLogicOperand.OR.toString());
			DbSearchCriterion dbc2 = new DbSearchCriterion(Tc.OLD_ANIMAL_ID, DbCompareOperand.ILIKE,
					Tc.PERCENT_OPERATOR + animalId + Tc.PERCENT_OPERATOR);
			dbseAnimalIds.addDbSearchItem(dbc1).addDbSearchItem(dbc2);
			dbse.addDbSearchItem(dbseAnimalIds);
		}
		if (status != null && !status.trim().equals("null")) {
			dbc = new DbSearchCriterion(Tc.STATUS, DbCompareOperand.EQUAL, status);
			dbse.addDbSearchItem(dbc);
		}
		if (animalClass != null && !animalClass.trim().equals("null")) {
			dbc = new DbSearchCriterion(Tc.ANIMAL_CLASS, DbCompareOperand.EQUAL, animalClass);
			dbse.addDbSearchItem(dbc);
		}
		if (breed != null && !breed.trim().equals("null")) {
			dbc = new DbSearchCriterion(Tc.ANIMAL_RACE, DbCompareOperand.EQUAL, breed);
			dbse.addDbSearchItem(dbc);
		}
		if (color != null && !color.trim().equals("null")) {
			dbc = new DbSearchCriterion(Tc.COLOR, DbCompareOperand.EQUAL, color);
			dbse.addDbSearchItem(dbc);
		}
		if (country != null && !country.trim().equals("null")) {
			DbSearchExpression dbseAnimalCountry = new DbSearchExpression();
			DbSearchCriterion dbc1 = new DbSearchCriterion(Tc.COUNTRY, DbCompareOperand.EQUAL, country);
			dbc1.setNextCritOperand(DbLogicOperand.OR.toString());
			DbSearchCriterion dbc2 = new DbSearchCriterion(Tc.COUNTRY_OLD_ID, DbCompareOperand.EQUAL, country);
			dbseAnimalCountry.addDbSearchItem(dbc1).addDbSearchItem(dbc2);
			dbse.addDbSearchItem(dbseAnimalCountry);
		}
		DbQueryObject query = new DbQueryObject(SvReader.getDbtByName(Tc.ANIMAL), dbse, null, null);
		ArrayList<String> orderBy = new ArrayList<String>();
		orderBy.add(Tc.PKID + " " + Tc.DESC);
		query.setOrderByFields(orderBy);
		result = svr.getObjects(query, rowLimit, 0);
		return result;
	}

	public DbDataArray getOutgoingTransfersPerOrgUnit(Long parentId, String tagType, String startTagId, String endTagId,
			String dateFrom, String dateTo, int rowLimit, SvReader svr) throws SvException {
		DbDataArray result = new DbDataArray();
		DbSearchExpression dbse = new DbSearchExpression();
		DbSearchCriterion dbc;
		if (parentId != null) {
			dbc = new DbSearchCriterion(Tc.PARENT_ID, DbCompareOperand.EQUAL, parentId);
			dbse.addDbSearchItem(dbc);
		}
		if (tagType != null && !tagType.trim().equals("null")) {
			dbc = new DbSearchCriterion(Tc.TAG_TYPE, DbCompareOperand.EQUAL, tagType);
			dbse.addDbSearchItem(dbc);
		}
		if (startTagId != null && !startTagId.trim().equals("null")) {
			dbc = new DbSearchCriterion(Tc.START_TAG_ID, DbCompareOperand.EQUAL, Integer.valueOf(startTagId));
			dbse.addDbSearchItem(dbc);
		}
		if (endTagId != null && !endTagId.trim().equals("null")) {
			dbc = new DbSearchCriterion(Tc.END_TAG_ID, DbCompareOperand.EQUAL, Integer.valueOf(endTagId));
			dbse.addDbSearchItem(dbc);
		}
		if (dateFrom != null && !dateFrom.equals("null") && dateTo != null && !dateTo.equals("null")) {
			dbc = new DbSearchCriterion(Tc.DT_INSERT, DbCompareOperand.BETWEEN, new DateTime(dateFrom),
					new DateTime(dateTo));
			dbse.addDbSearchItem(dbc);
		} else {
			rowLimit = 100;
		}
		DbQueryObject query = new DbQueryObject(SvReader.getDbtByName(Tc.TRANSFER), dbse, null, null);
		ArrayList<String> orderBy = new ArrayList<String>();
		orderBy.add(Tc.PKID + " " + Tc.DESC);
		query.setOrderByFields(orderBy);
		result = svr.getObjects(query, rowLimit, 0);
		return result;
	}

	public DbDataArray getIncomingTransfersPerOrgUnit(String destinationOrgUnitObjId, String tagType, String startTagId,
			String endTagId, String dateFrom, String dateTo, int rowLimit, SvReader svr) throws SvException {
		DbDataArray result = new DbDataArray();
		DbSearchExpression dbse = new DbSearchExpression();
		DbSearchCriterion dbc;
		if (destinationOrgUnitObjId != null) {
			dbc = new DbSearchCriterion(Tc.DESTINATION_OBJ_ID, DbCompareOperand.EQUAL, destinationOrgUnitObjId);
			dbse.addDbSearchItem(dbc);
		}
		if (tagType != null && !tagType.trim().equals("null")) {
			dbc = new DbSearchCriterion(Tc.TAG_TYPE, DbCompareOperand.EQUAL, tagType);
			dbse.addDbSearchItem(dbc);
		}
		if (startTagId != null && !startTagId.trim().equals("null")) {
			dbc = new DbSearchCriterion(Tc.START_TAG_ID, DbCompareOperand.EQUAL, Integer.valueOf(startTagId));
			dbse.addDbSearchItem(dbc);
		}
		if (endTagId != null && !endTagId.trim().equals("null")) {
			dbc = new DbSearchCriterion(Tc.END_TAG_ID, DbCompareOperand.EQUAL, Integer.valueOf(endTagId));
			dbse.addDbSearchItem(dbc);
		}
		if (dateFrom != null && !dateFrom.equals("null") && dateTo != null && !dateTo.equals("null")) {
			dbc = new DbSearchCriterion(Tc.DT_INSERT, DbCompareOperand.BETWEEN, new DateTime(dateFrom),
					new DateTime(dateTo));
			dbse.addDbSearchItem(dbc);
		} else {
			rowLimit = 100;
		}
		DbQueryObject query = new DbQueryObject(SvReader.getDbtByName(Tc.TRANSFER), dbse, null, null);
		ArrayList<String> orderBy = new ArrayList<String>();
		orderBy.add(Tc.PKID + " " + Tc.DESC);
		query.setOrderByFields(orderBy);
		result = svr.getObjects(query, rowLimit, 0);
		return result;
	}

	public DbDataObject findLastAnimalMovement(Long animalObjId, DbDataObject holdingObj, SvReader svr)
			throws SvException {
		DbDataObject lastAnimalOrFlockMovementObj = null;
		DbDataObject dboObjectToHandle = svr.getObjectById(animalObjId, SvReader.getTypeIdByName(Tc.ANIMAL), null);
		if (holdingObj == null) {
			holdingObj = svr.getObjectById(dboObjectToHandle.getParent_id(), SvReader.getTypeIdByName(Tc.HOLDING),
					null);
		}
		DbSearchExpression dbse = new DbSearchExpression();
		DbSearchCriterion dbc1 = new DbSearchCriterion(Tc.STATUS, DbCompareOperand.EQUAL, Tc.FINISHED);
		DbSearchCriterion dbc2 = new DbSearchCriterion(Tc.ANIMAL_EAR_TAG, DbCompareOperand.EQUAL,
				dboObjectToHandle.getVal(Tc.ANIMAL_ID));
		DbSearchCriterion dbc3 = new DbSearchCriterion(Tc.DESTINATION_HOLDING_ID, DbCompareOperand.EQUAL,
				holdingObj.getVal(Tc.PIC));
		dbse.addDbSearchItem(dbc1).addDbSearchItem(dbc2).addDbSearchItem(dbc3);
		DbQueryObject query = new DbQueryObject(SvReader.getDbtByName(Tc.ANIMAL_MOVEMENT), dbse, null, null);
		ArrayList<String> orderBy = new ArrayList<String>();
		orderBy.add(Tc.PKID + " " + Tc.DESC);
		query.setOrderByFields(orderBy);
		DbDataArray dboAnimalOrFlockMovementsArr = svr.getObjects(query, 0, 0);
		if (dboAnimalOrFlockMovementsArr.size() > 0) {
			lastAnimalOrFlockMovementObj = dboAnimalOrFlockMovementsArr.getItems().get(0);
		}
		return lastAnimalOrFlockMovementObj;
	}

	public DbDataArray getFiltratedFlocks(String flockId, String status, String animalClass, String color, int rowLimit,
			SvReader svr) throws SvException {
		DbDataArray result = new DbDataArray();
		DbSearchExpression dbse = new DbSearchExpression();
		DbSearchCriterion dbc;
		if (flockId != null && !flockId.trim().equals("null")) {
			dbc = new DbSearchCriterion(Tc.FLOCK_ID, DbCompareOperand.LIKE, flockId + Tc.PERCENT_OPERATOR);
			dbse.addDbSearchItem(dbc);
		}
		if (status != null && !status.trim().equals("null")) {
			dbc = new DbSearchCriterion(Tc.STATUS, DbCompareOperand.EQUAL, status);
			dbse.addDbSearchItem(dbc);
		}
		if (animalClass != null && !animalClass.trim().equals("null")) {
			dbc = new DbSearchCriterion(Tc.ANIMAL_TYPE, DbCompareOperand.EQUAL, animalClass);
			dbse.addDbSearchItem(dbc);
		}
		if (color != null && !color.trim().equals("null")) {
			dbc = new DbSearchCriterion(Tc.EAR_TAG_COLOR, DbCompareOperand.EQUAL, color);
			dbse.addDbSearchItem(dbc);
		}
		DbQueryObject query = new DbQueryObject(SvReader.getDbtByName(Tc.FLOCK), dbse, null, null);
		ArrayList<String> orderBy = new ArrayList<String>();
		orderBy.add(Tc.PKID + " " + Tc.DESC);
		query.setOrderByFields(orderBy);
		result = svr.getObjects(query, rowLimit, 0);
		return result;
	}

	/**
	 * Returns dbDataObject of holding responsible
	 * 
	 * @param natRegNumber - id number of holding responsible
	 * @param svr          - SvReader instance
	 * @return DbDataObject
	 * @throws SvException
	 */
	public DbDataObject findHoldingResponsibleById(String natRegNumber, SvReader svr) throws SvException {
		DbDataObject holdingResponsible = null;
		DbDataArray holdingResponsibles = findDataPerSingleFilter(Tc.NAT_REG_NUMBER, natRegNumber,
				DbCompareOperand.EQUAL, SvReader.getTypeIdByName(Tc.HOLDING_RESPONSIBLE), svr);
		if (holdingResponsibles != null && !holdingResponsibles.getItems().isEmpty()) {
			holdingResponsible = holdingResponsibles.getItems().get(0);
		}
		return holdingResponsible;
	}

	/**
	 * Returns DbDataArray of holdings
	 *
	 * @param holdingResponsibleObject - DbDataObject of holding responsible
	 * @param svr                      - SvReader instance
	 * @return DbDataArray
	 * @throws SvException
	 */
	public DbDataArray findHoldingsPerHoldingResponsibleObj(DbDataObject holdingResponsibleObject, SvReader svr)
			throws SvException {
		return findHoldingsPerHoldingResponsibleObjId(holdingResponsibleObject.getObject_id(), svr);
	}

	/**
	 * Returns DbDataArray of holdings
	 * 
	 * @param holdingResponsibleObjectId - object_id of holding responsible
	 * @param svr                        - SvReader instance
	 * @return DbDataArray
	 * @throws SvException
	 */
	public DbDataArray findHoldingsPerHoldingResponsibleObjId(Long holdingResponsibleObjectId, SvReader svr)
			throws SvException {
		DbDataArray holdingsByHoldingResponsible_final = new DbDataArray();
		DbDataObject holdingResponsible = svr.getObjectById(holdingResponsibleObjectId,
				SvReader.getTypeIdByName(Tc.HOLDING_RESPONSIBLE), null);
		if (holdingResponsible != null) {
			DbDataArray holdingsByHoldingMember = getLinkedHoldingsByHoldingResponsibleAndLinkName(holdingResponsible,
					Tc.HOLDING_MEMBER_OF, svr);
			DbDataArray holdingsByHoldingAssociated = getLinkedHoldingsByHoldingResponsibleAndLinkName(
					holdingResponsible, Tc.HOLDING_ASSOCIATED, svr);
			DbDataArray holdingsByKeeper = getLinkedHoldingsByHoldingResponsibleAndLinkName(holdingResponsible,
					Tc.HOLDING_KEEPER, svr);
			DbDataArray holdingsByHerder = getLinkedHoldingsByHoldingResponsibleAndLinkName(holdingResponsible,
					Tc.HOLDING_HERDER, svr);
			if (holdingsByHoldingMember != null && !holdingsByHoldingMember.getItems().isEmpty()) {
				for (DbDataObject holdingMember : holdingsByHoldingMember.getItems()) {
					DbDataObject holdingByMember = svr.getObjectById((Long) holdingMember.getVal(Tc.LINK_OBJ_ID_1),
							SvReader.getTypeIdByName(Tc.HOLDING), null);
					holdingsByHoldingResponsible_final.addDataItem(holdingByMember);
				}
			}
			if (holdingsByHoldingAssociated != null && !holdingsByHoldingAssociated.getItems().isEmpty()) {
				for (DbDataObject holdingAssociated : holdingsByHoldingAssociated.getItems()) {
					DbDataObject holdingByAssociated = svr.getObjectById(
							(Long) holdingAssociated.getVal(Tc.LINK_OBJ_ID_1), SvReader.getTypeIdByName(Tc.HOLDING),
							null);
					holdingsByHoldingResponsible_final.addDataItem(holdingByAssociated);
				}
			}
			if (holdingsByKeeper != null && !holdingsByKeeper.getItems().isEmpty()) {
				for (DbDataObject holdingKeeper : holdingsByKeeper.getItems()) {
					DbDataObject holdingByKeeper = svr.getObjectById((Long) holdingKeeper.getVal(Tc.LINK_OBJ_ID_1),
							SvReader.getTypeIdByName("HOLDING"), null);
					holdingsByHoldingResponsible_final.addDataItem(holdingByKeeper);
				}
			}
			if (holdingsByHerder != null && !holdingsByHerder.getItems().isEmpty()) {
				for (DbDataObject holdingHerder : holdingsByHerder.getItems()) {
					DbDataObject holdingByHerder = svr.getObjectById((Long) holdingHerder.getVal(Tc.LINK_OBJ_ID_1),
							SvReader.getTypeIdByName(Tc.HOLDING), null);
					holdingsByHoldingResponsible_final.addDataItem(holdingByHerder);
				}
			}
		}
		return holdingsByHoldingResponsible_final;
	}

	/**
	 * Returns DbDataArray of linked objects
	 * 
	 * @param linkName        - name of the link
	 * @param firstTableName  - name of the first table in link
	 * @param secondTableName - name of the second table in link
	 * @param object          - object to be searched for
	 * @param isReverse       - indicator if the link is reverse
	 * @param svr             - SvReader instance
	 * @return DbDataArray
	 * @throws SvException
	 */
	public DbDataArray getObjectArrayByLink(String linkName, String firstTableName, String secondTableName,
			DbDataObject object, Boolean isReverse, SvReader svr) throws SvException {
		DbDataArray objectsByLink = new DbDataArray();
		if (linkName != null && !linkName.equals("") && firstTableName != null && !firstTableName.equals("")
				&& secondTableName != null && !secondTableName.equals("")) {
			DbDataObject link = SvLink.getLinkType(linkName, SvReader.getTypeIdByName(firstTableName),
					SvReader.getTypeIdByName(secondTableName));
			if (!isReverse) {
				objectsByLink = svr.getObjectsByLinkedId(object.getObject_id(),
						SvReader.getTypeIdByName(secondTableName), link, SvReader.getTypeIdByName(firstTableName),
						isReverse, null, 0, 0);
			} else if (isReverse) {
				objectsByLink = svr.getObjectsByLinkedId(object.getObject_id(),
						SvReader.getTypeIdByName(firstTableName), link, SvReader.getTypeIdByName(secondTableName),
						isReverse, null, 0, 0);
			}
		}
		return objectsByLink;
	}

	/**
	 * Returns DbDataArray of animals in holdings
	 * 
	 * @param holdings - DbDataArray of holdings
	 * @param svr      - SvReader instance
	 * @return DbDataArray
	 * @throws SvException
	 */
	public DbDataArray getAnimalsForHoldings(DbDataArray holdings, SvReader svr) throws SvException {
		DbDataArray animals = new DbDataArray();
		if (holdings != null && !holdings.getItems().isEmpty()) {
			for (DbDataObject tempHoldingObj : holdings.getItems()) {
				DbDataArray animalsInHolding = svr.getObjectsByParentId(tempHoldingObj.getObject_id(),
						SvReader.getTypeIdByName(Tc.ANIMAL), null, 0, 0);
				if (animalsInHolding != null && !animalsInHolding.getItems().isEmpty()) {
					for (DbDataObject tempAnimalObj : animalsInHolding.getItems()) {
						animals.addDataItem(tempAnimalObj);
					}
				}
			}
		}
		return animals;
	}

	/**
	 * Returns DbDataArray of animals
	 * 
	 * @param holdings - MultivaluedMap map of holdings
	 * @param svr      - SvReader instance
	 * @return DbDataArray of animals
	 * @throws SvException
	 */
	public DbDataArray getAnimalsByHoldings(MultivaluedMap<String, String> holdings, SvReader svr) throws SvException {
		Gson gson = new Gson();
		JsonObject jsonData = null;
		Long holdingId = null;
		JsonArray jsonArray = null;
		DbDataArray animals = new DbDataArray();
		for (Entry<String, List<String>> holding : holdings.entrySet()) {
			if (holding.getKey() != null && !holding.getKey().isEmpty()) {
				String key = holding.getKey();
				jsonData = gson.fromJson(key, JsonObject.class);
				if (!jsonData.isJsonNull() && jsonData.has("HOLDING.OBJECT.IDS")) {
					jsonArray = jsonData.get("HOLDING.OBJECT.IDS").getAsJsonArray();
				}
				for (JsonElement jsonElement : jsonArray) {
					JsonObject obj = jsonElement.getAsJsonObject();
					if (obj.has("HOLDING.OBJECT_ID")) {
						holdingId = obj.get("HOLDING.OBJECT_ID").getAsLong();
						DbDataArray animalsByHolding = svr.getObjectsByParentId(holdingId,
								SvReader.getTypeIdByName(Tc.ANIMAL), null, 0, 0);
						if (animalsByHolding != null && !animalsByHolding.getItems().isEmpty()) {
							for (DbDataObject animal : animalsByHolding.getItems()) {
								animals.addDataItem(animal);
							}
						}
					}
				}
			}
		}
		return animals;
	}

	/**
	 * Returns DbDataArray of animals
	 * 
	 * @param holdings - String of ids of holdings
	 * @param svr      - SvReader instance
	 * @return DbDataArray of animals
	 * @throws SvException
	 */
	public DbDataArray getAnimalsByHoldings(String idsOfHoldings, SvReader svr) throws SvException {
		DbDataArray animals = new DbDataArray();
		String[] arrholdingsIds = null;
		DbDataArray animalsByHolding = null;
		Long holdingId = null;
		if (idsOfHoldings != null && !idsOfHoldings.isEmpty()) {
			arrholdingsIds = idsOfHoldings.split(",");
			for (String holding : arrholdingsIds) {
				holdingId = Long.parseLong(holding.trim());
				animalsByHolding = svr.getObjectsByParentId(holdingId, SvReader.getTypeIdByName(Tc.ANIMAL), null, 0, 0);
				if (animalsByHolding != null && !animalsByHolding.getItems().isEmpty()) {
					for (DbDataObject animal : animalsByHolding.getItems()) {
						animals.addDataItem(animal);
					}
				}
			}
		}
		return animals;
	}

	/**
	 * Returns DbDataArray of vaccination events
	 * 
	 * @param animalObj - object_id of animal
	 * @param svr       - SvReader instance
	 * @return DbDataArray of vaccination events
	 * @throws SvException
	 */
	public DbDataArray getVaccEventsByAnimal(DbDataObject animalObj, SvReader svr) throws SvException {
		DbDataArray vaccEventByAnimalVaccBook = new DbDataArray();
		DbDataArray vaccEventByAnimalVaccBook_final = new DbDataArray();
		DbDataObject linkAnimalVaccBook = SvLink.getLinkType(Tc.ANIMAL_VACC_BOOK, SvReader.getTypeIdByName(Tc.ANIMAL),
				SvReader.getTypeIdByName(Tc.VACCINATION_BOOK));
		DbDataArray animalsVaccBooks = svr.getObjectsByLinkedId(animalObj.getObject_id(),
				SvReader.getTypeIdByName(Tc.ANIMAL), linkAnimalVaccBook, SvReader.getTypeIdByName(Tc.VACCINATION_BOOK),
				false, null, null, null);
		DbDataObject linkVaccEventBook = SvLink.getLinkType(Tc.VACC_EVENT_BOOK,
				SvReader.getTypeIdByName(Tc.VACCINATION_EVENT), SvReader.getTypeIdByName(Tc.VACCINATION_BOOK));
		if (animalsVaccBooks != null && !animalsVaccBooks.getItems().isEmpty()) {
			for (DbDataObject vaccBook : animalsVaccBooks.getItems()) {
				vaccEventByAnimalVaccBook = svr.getObjectsByLinkedId(vaccBook.getObject_id(),
						SvReader.getTypeIdByName(Tc.VACCINATION_BOOK), linkVaccEventBook,
						SvReader.getTypeIdByName(Tc.VACCINATION_EVENT), true, null, 0, 0);
				if (vaccEventByAnimalVaccBook != null && !vaccEventByAnimalVaccBook.getItems().isEmpty()) {
					for (DbDataObject vaccEvent : vaccEventByAnimalVaccBook.getItems()) {
						vaccEventByAnimalVaccBook_final.addDataItem(vaccEvent);
					}
				}
			}
		}
		return vaccEventByAnimalVaccBook_final;
	}

	/**
	 * Returns true if campaign exists in vaccination events
	 * 
	 * @param animalObjId   - object_id of animal
	 * @param campaignObjId - object_id of vaccination_event
	 * @param svr           - SvReader instance
	 * @return true if campaign exists in vaccination events
	 * @throws SvException
	 */
	public Boolean checkIfAnimalParticipatedInCampaign(Long animalObjId, Long campaignObjId, SvReader svr)
			throws SvException {
		DbDataArray vaccEventByAnimal = new DbDataArray();
		Long vaccObjId = null;
		Boolean ifCampaignExist = false;
		DbDataObject animalObj = svr.getObjectById(animalObjId, SvReader.getTypeIdByName(Tc.ANIMAL), null);
		DbDataObject campaignObj = svr.getObjectById(campaignObjId, SvReader.getTypeIdByName(Tc.VACCINATION_EVENT),
				null);
		if (animalObj != null && campaignObj != null) {
			vaccEventByAnimal = getVaccEventsByAnimal(animalObj, svr);
			if (vaccEventByAnimal != null && !vaccEventByAnimal.getItems().isEmpty()) {
				for (DbDataObject vaccEvent : vaccEventByAnimal.getItems()) {
					vaccObjId = vaccEvent.getObject_id();
					if (campaignObjId.equals(vaccObjId)) {
						ifCampaignExist = true;
						break;
					}
				}
			}
		} else {
			throw (new SvException("naits.error.campaignExistsInAnimalVaccEvents", svCONST.systemUser, null, null));
		}
		return ifCampaignExist;
	}
}