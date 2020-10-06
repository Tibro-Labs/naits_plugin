/*******************************************************************************
 * Copyright (c),  2017 TIBRO DOOEL Skopje
 *******************************************************************************/

package naits_triglav_plugin;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.lang.math.NumberUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.openxml4j.util.Nullable;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.joda.time.DateTime;
import org.joda.time.DateTimeComparator;
import org.joda.time.format.DateTimeFormat;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.prtech.svarog.I18n;
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
import com.prtech.svarog.SvWorkflow;
import com.prtech.svarog.SvWriter;
import com.prtech.svarog.svCONST;
import com.prtech.svarog_common.DbDataArray;
import com.prtech.svarog_common.DbDataObject;
import com.prtech.svarog_common.DbSearchCriterion;
import com.prtech.svarog_common.DbSearchExpression;
import com.prtech.svarog_common.DbSearchCriterion.DbCompareOperand;

/**
 * Helper class for create/update data
 * 
 * @author TIBRO_001
 *
 */

public class Writer {
	static final int COMMIT_COUNT = 1000;
	static final Logger log4j = LogManager.getLogger(Writer.class.getName());

	/**
	 * function for create contact data
	 * 
	 * @throws SvException
	 */
	public void saveContactData2(Long parentId, JsonObject jsonData, SvWriter svw) throws SvException {
		DbDataObject testContact = new DbDataObject();
		testContact.setObject_type(svCONST.OBJECT_TYPE_CONTACT_DATA);
		testContact.setParent_id(parentId);
		if (jsonData.get("street_type") != null)
			testContact.setVal("street_type", jsonData.get("street_type").toString().replaceAll("\"", ""));
		if (jsonData.get("street_name") != null)
			testContact.setVal("street_name", jsonData.get("street_name").toString().replaceAll("\"", ""));
		if (jsonData.get("house_number") != null)
			testContact.setVal("house_number", jsonData.get("house_number").toString().replaceAll("\"", ""));
		if (jsonData.get("postal_code") != null)
			testContact.setVal("postal_code", jsonData.get("postal_code").toString().replaceAll("\"", ""));
		if (jsonData.get("city") != null)
			testContact.setVal("city", jsonData.get("city").toString().replaceAll("\"", ""));
		if (jsonData.get("state") != null)
			testContact.setVal("state", jsonData.get("state").toString().replaceAll("\"", ""));
		if (jsonData.get("phone_number") != null)
			testContact.setVal("phone_number", jsonData.get("phone_number").toString().replaceAll("\"", ""));
		if (jsonData.get("mobile_number") != null)
			testContact.setVal("mobile_number", jsonData.get("mobile_number").toString().replaceAll("\"", ""));
		if (jsonData.get("fax") != null)
			testContact.setVal("fax", jsonData.get("fax").toString().replaceAll("\"", ""));
		if (jsonData.get("email") != null)
			testContact.setVal("email", jsonData.get("email").toString().replaceAll("\"", ""));
		svw.saveObject(testContact);
	}

	/**
	 * method for update user mail
	 * 
	 * @throws SvException
	 */
	public void updateUserMail(String email, DbDataObject dboUser, String sessionId) throws SvException {
		SvWriter svw = new SvWriter(sessionId);
		SvReader svr = new SvReader(svw);
		try {
			if (!dboUser.getVal(Tc.E_MAIL).equals(email)) {
				ReentrantLock lock = null;
				try {
					lock = SvLock.getLock(String.valueOf(dboUser.getObject_id()), false, 0);
					dboUser.setVal(Tc.E_MAIL, email);
					svw.saveObject(dboUser);
					dboUser = svr.getObjectById(dboUser.getObject_id(), svCONST.OBJECT_TYPE_USER, null);
					svw.dbCommit();
				} finally {
					if (lock != null) {
						SvLock.releaseLock(String.valueOf(dboUser.getObject_id()), lock);
					}
				}
			}
		} finally {
			if (svw != null)
				svw.release();
			if (svr != null)
				svr.release();
		}
	}

	/**
	 * function for edit user data
	 * 
	 * @throws SvException
	 */
	public void editUserData(String firstName, String lastName, DbDataObject dboUser, String sessionId)
			throws SvException {
		SvWriter svw = new SvWriter(sessionId);
		SvReader svr = new SvReader(svw);
		try {
			ReentrantLock lock = null;
			try {
				lock = SvLock.getLock(String.valueOf(dboUser.getObject_id()), false, 0);
				if (!dboUser.getVal(Tc.FIRST_NAME).equals(firstName))
					dboUser.setVal(Tc.FIRST_NAME, firstName);
				if (!dboUser.getVal(Tc.LAST_NAME).equals(firstName))
					dboUser.setVal(Tc.LAST_NAME, lastName);
				svw.saveObject(dboUser);
				dboUser = svr.getObjectById(dboUser.getObject_id(), svCONST.OBJECT_TYPE_USER, null);
			} finally {
				if (lock != null && dboUser != null) {
					SvLock.releaseLock(String.valueOf(dboUser.getObject_id()), lock);
				}
			}
			svw.dbCommit();
		} finally {
			if (svw != null)
				svw.release();
			if (svr != null)
				svr.release();
		}
	}

	/**
	 * function for linking objects for certain link
	 * 
	 * @param obj1
	 *            objectId of the first object (left) which should be linked
	 * @param obj2
	 *            objectId of the second object (right) which should be linked
	 * @param linkType
	 *            linkType of the link which will be used to link the objects
	 *            (check SVAROG_LINK_TYPES)
	 * @param linkNote
	 *            note for the link which will be created (escape it if not
	 *            needed)
	 * @param baseSvarogInst
	 *            instance of svarog.SvarogCore
	 * @throws SvException
	 */
	public void linkObjects(DbDataObject obj1, DbDataObject obj2, String linkType, String linkNote,
			SvCore baseSvarogInst) throws SvException {
		linkObjects(obj1, obj2, linkType, linkNote, baseSvarogInst, false);
	}

	public void linkObjects(DbDataObject obj1, DbDataObject obj2, String linkType, String linkNote,
			SvCore baseSvarogInst, Boolean autoCommit) throws SvException {
		SvLink svLink = new SvLink(baseSvarogInst);
		try {
			svLink.linkObjects(obj1, obj2, linkType, linkNote, autoCommit);
		} finally {
			if (svLink != null) {
				svLink.release();
			}
		}
	}

	public Boolean invalidateLink(DbDataObject dboLink, SvCore parentCore) {
		return invalidateLink(dboLink, true, parentCore);
	}

	public Boolean invalidateLink(DbDataObject dboLink, Boolean autoCommit, SvCore parentCore) {
		Boolean result = false;
		SvWriter svw = null;
		try {
			svw = new SvWriter(parentCore);
			if (dboLink != null) {
				svw.deleteObject(dboLink, autoCommit);
				svw.dbCommit();
			}
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage(), e);
		} finally {
			if (svw != null) {
				svw.release();
			}
		}
		return result;
	}

	public void createVaccTreatmentRecord(DbDataObject animalOrFlockObj, DbDataObject vaccEventObj, String dateOfAction,
			String untisTreated, SvWriter svw) throws SvException {
		DbDataObject dboRecord = new DbDataObject();
		String note = "";
		String vetPerson = "";
		String activityType = "";
		String campaignName = "";
		String vaccBookType = Tc.ANIMAL;
		String linkName = Tc.ANIMAL_VACC_BOOK;
		Long numUnitsTreated = 1L;
		if (animalOrFlockObj.getObject_type().equals(SvReader.getTypeIdByName(Tc.FLOCK))) {
			vaccBookType = Tc.FLOCK;
			linkName = Tc.FLOCK_VACC_BOOK;
			if (animalOrFlockObj.getVal(Tc.TOTAL) != null) {
				numUnitsTreated = Long.valueOf(untisTreated);
				if (Long.valueOf(numUnitsTreated) > Long.valueOf(animalOrFlockObj.getVal(Tc.TOTAL).toString())) {
					throw (new SvException("naits.error.numberOfUnitsCantBeLargerThanTotalUnits",
							svw.getInstanceUser()));
				}
				if (numUnitsTreated.equals(0L)) {
					numUnitsTreated = Long.valueOf(animalOrFlockObj.getVal(Tc.TOTAL).toString());
				}
			}
		}
		if (vaccEventObj.getVal(Tc.NOTE) != null) {
			note = vaccEventObj.getVal(Tc.NOTE).toString();
		}
		if (vaccEventObj.getVal(Tc.RESPONSIBLE) != null) {
			vetPerson = vaccEventObj.getVal(Tc.RESPONSIBLE).toString();
		}
		if (vaccEventObj.getVal(Tc.ACTIVITY_TYPE) != null) {
			activityType = vaccEventObj.getVal(Tc.ACTIVITY_TYPE).toString();
		}
		if (vaccEventObj.getVal(Tc.CAMPAIGN_NAME) != null) {
			campaignName = vaccEventObj.getVal(Tc.CAMPAIGN_NAME).toString();
		}
		dboRecord.setObject_type(SvReader.getTypeIdByName(Tc.VACCINATION_BOOK));
		// dboRecord.setParent_id(animalObj.getObject_id());
		String dtNow = new DateTime().toString().substring(0, 10);
		String paramDate = dtNow;
		if (dateOfAction != null && !dateOfAction.toString().equals("") && !dateOfAction.toString().equals("null")) {
			paramDate = dateOfAction;
		}
		dboRecord.setVal(Tc.VACC_DATE, paramDate);
		dboRecord.setVal(Tc.NOTE, note);
		dboRecord.setVal(Tc.VET_OFFICER, vetPerson);
		dboRecord.setVal(Tc.BOOK_TYPE, vaccBookType);
		dboRecord.setVal(Tc.ACTIVITY_TYPE, activityType);
		dboRecord.setVal(Tc.CAMPAIGN_NAME, campaignName);
		dboRecord.setVal(Tc.NO_ITEMS_TREATED, numUnitsTreated);
		svw.saveObject(dboRecord, false);
		linkObjects(vaccEventObj, dboRecord, Tc.VACC_EVENT_BOOK, "linked via WS", svw);
		linkObjects(animalOrFlockObj, dboRecord, linkName, "linked via WS", svw);
	}

	/**
	 * Method that creates OR updates Pet health book according Vaccination
	 * event
	 * 
	 * @param dboHealthBook
	 *            DbDataObject of type PET_HEALTH_BOOK. null when we want to
	 *            create new record
	 * @param dboVaccinationEvent
	 *            DbDataObject of type VACCINATION_EVENT
	 * @param actionDate
	 *            Action date
	 * @param responsibleUser
	 *            Responsible user
	 * @param parentId
	 *            Parent_Id
	 * @return
	 * @throws SvException
	 */
	public DbDataObject createOrUpdatePetHealthBookAccordingVaccinationEvent(DbDataObject dboHealthBook,
			DbDataObject dboVaccinationEvent, String actionDate, String responsibleUser, Long parentId)
			throws SvException {
		DbDataObject dboPetHealthBook = null;
		String activityType = null;
		String campaignName = null;
		String activitySubtype = null;
		Long immunizationPeriod = null;
		Long prohibitionPeriod = null;
		String vaccinationEventNote = null;
		if (dboVaccinationEvent.getVal(Tc.ACTIVITY_TYPE) != null) {
			activityType = dboVaccinationEvent.getVal(Tc.ACTIVITY_TYPE).toString();
		}
		if (dboVaccinationEvent.getVal(Tc.CAMPAIGN_NAME) != null) {
			campaignName = dboVaccinationEvent.getVal(Tc.CAMPAIGN_NAME).toString();
		}
		if (dboVaccinationEvent.getVal(Tc.ACTIVITY_SUBTYPE) != null) {
			activitySubtype = dboVaccinationEvent.getVal(Tc.ACTIVITY_SUBTYPE).toString();
		}
		if (dboVaccinationEvent.getVal(Tc.NOTE) != null) {
			vaccinationEventNote = dboVaccinationEvent.getVal(Tc.NOTE).toString();
		}
		if (dboVaccinationEvent.getVal(Tc.PROHIBITION_PERIOD) != null) {
			immunizationPeriod = Long.valueOf(dboVaccinationEvent.getVal(Tc.PROHIBITION_PERIOD).toString());
		}
		if (dboVaccinationEvent.getVal(Tc.IMMUNIZATION_PERIOD) != null) {
			prohibitionPeriod = Long.valueOf(dboVaccinationEvent.getVal(Tc.IMMUNIZATION_PERIOD).toString());
		}
		dboPetHealthBook = createPetHealthBook(dboHealthBook, actionDate, campaignName, null, responsibleUser,
				activityType, activitySubtype, immunizationPeriod, prohibitionPeriod, null, null, null,
				vaccinationEventNote, parentId);
		return dboPetHealthBook;
	}

	public DbDataObject createPetHealthBook(String actionDate, String activityType, Long parentId) throws SvException {
		return createPetHealthBook(null, actionDate, null, null, null, activityType, null, null, null, null, null, null,
				null, parentId);
	}

	public DbDataObject createPetHealthBook(DbDataObject dboHealthBook, String actionDate, String campaignName,
			String vaccinationCode, String vetOfficer, String activityType, String activitySubType,
			Long immunizationPeriod, Long prohibitionPeriod, String treatmentType, Long vaccEventObjId, String sampleId,
			String note, Long parentId) throws SvException {
		if (dboHealthBook == null) {
			dboHealthBook = new DbDataObject();
			dboHealthBook.setObject_type(SvReader.getTypeIdByName(Tc.PET_HEALTH_BOOK));
		}
		DateTime dtAction = new DateTime();
		if (parentId != null) {
			dboHealthBook.setParent_id(parentId);
		}
		if (dboHealthBook.getVal(Tc.ACTION_DATE) == null) {
			dtAction = new DateTime(actionDate);
			dboHealthBook.setVal(Tc.ACTION_DATE, dtAction);
		}
		if (campaignName != null)
			dboHealthBook.setVal(Tc.CAMPAIGN_NAME, campaignName);
		if (vaccinationCode != null)
			dboHealthBook.setVal(Tc.VACCINATION_CODE, vaccinationCode);
		if (vetOfficer != null)
			dboHealthBook.setVal(Tc.VET_OFFICER, vetOfficer);
		if (activityType != null)
			dboHealthBook.setVal(Tc.ACTIVITY_TYPE, activityType);
		if (activitySubType != null)
			dboHealthBook.setVal(Tc.ACTIVITY_SUBTYPE, activitySubType);
		if (immunizationPeriod != null)
			dboHealthBook.setVal(Tc.IMMUNIZATION_PERIOD, immunizationPeriod);
		if (prohibitionPeriod != null)
			dboHealthBook.setVal(Tc.PROHIBITION_PERIOD, prohibitionPeriod);
		if (treatmentType != null)
			dboHealthBook.setVal(Tc.TREATMENT_TYPE, treatmentType);
		if (vaccEventObjId != null)
			dboHealthBook.setVal(Tc.VACC_EVENT_OBJ_ID, vaccEventObjId);
		if (sampleId != null)
			dboHealthBook.setVal(Tc.SAMPLE_ID, sampleId);
		if (note != null)
			dboHealthBook.setVal(Tc.NOTE, note);
		return dboHealthBook;
	}

	public DbDataObject createDboPetAccordingStrayPet(DbDataObject dboStrayPet, String adoptionDate, SvWriter svw)
			throws SvException {
		DateTime dtAdoption = new DateTime();
		String petId = dboStrayPet.getVal(Tc.PET_ID) != null ? dboStrayPet.getVal(Tc.PET_ID).toString() : null;
		String petTagType = dboStrayPet.getVal(Tc.PET_TAG_TYPE) != null ? dboStrayPet.getVal(Tc.PET_TAG_TYPE).toString()
				: null;
		String petType = dboStrayPet.getVal(Tc.PET_TYPE) != null ? dboStrayPet.getVal(Tc.PET_TYPE).toString() : null;
		String petBreed = dboStrayPet.getVal(Tc.PET_BREED) != null ? dboStrayPet.getVal(Tc.PET_BREED).toString() : null;
		if (adoptionDate != null && !adoptionDate.equals("null")) {
			dtAdoption = new DateTime(adoptionDate);
		}
		DbDataObject dboPet = createDboPet(petId, petTagType, petType, petBreed, true, dtAdoption);
		svw.saveObject(dboPet, false);
		return dboPet;
	}

	public String sendPassportRequest(DbDataObject dboPet, DbDataObject dboVetStation, SvWriter svw, SvReader svr)
			throws SvException {
		Reader rdr = new Reader();
		ValidationChecks vc = new ValidationChecks();
		String result = "naits.success.sentPassportRequestPerPet";
		DbDataObject dboOwner = rdr.getPetOwner(dboPet, svr);
		if (dboOwner == null) {
			throw (new SvException("naits.error.cannotSendPassportRequestWithoutOwner", svCONST.systemUser, null,
					null));
		}
		if (vc.checkIfPetHasDraftPassportRequest(dboPet, svr)) {
			throw (new SvException("naits.error.foundDraftPassportRequest", svCONST.systemUser, null, null));
		}
		DbDataObject dboPassportRequest = createPassportRequest(dboPet, dboOwner, svr);
		if (dboPassportRequest != null) {
			dboPassportRequest.setVal(Tc.HOLDING_OBJ_ID, String.valueOf(dboVetStation.getObject_id()));
			svw.saveObject(dboPassportRequest, false);
		}
		return result;
	}

	public DbDataObject createPassportRequest(DbDataObject dboPet, DbDataObject dboOwner, SvReader svr) {
		String passportId = null;
		String requestId = null;
		DateTime requestDate = new DateTime();
		String petId = dboPet.getVal(Tc.PET_ID) != null ? dboPet.getVal(Tc.PET_ID).toString() : Tc.NOT_AVAILABLE_NA;
		String gender = dboPet.getVal(Tc.GENDER) != null ? dboPet.getVal(Tc.GENDER).toString() : Tc.NOT_AVAILABLE_NA;
		String petType = dboPet.getVal(Tc.PET_TYPE) != null ? dboPet.getVal(Tc.PET_TYPE).toString()
				: Tc.NOT_AVAILABLE_NA;
		String petBreed = dboPet.getVal(Tc.PET_BREED) != null ? dboPet.getVal(Tc.PET_BREED).toString()
				: Tc.NOT_AVAILABLE_NA;
		String ownerPn = dboOwner.getVal(Tc.NAT_REG_NUMBER) != null ? dboOwner.getVal(Tc.NAT_REG_NUMBER).toString()
				: Tc.NOT_AVAILABLE_NA;
		String ownerFirstName = dboOwner.getVal(Tc.FIRST_NAME) != null ? dboOwner.getVal(Tc.FIRST_NAME).toString()
				: Tc.NOT_AVAILABLE_NA;
		String ownerLastName = dboOwner.getVal(Tc.LAST_NAME) != null ? dboOwner.getVal(Tc.LAST_NAME).toString()
				: Tc.NOT_AVAILABLE_NA;
		if (!petId.equals(Tc.NOT_AVAILABLE_NA)) {
			requestId = generateRequestId(petId, svr);
		}
		return createPassportRequest(passportId, requestDate, petId, gender, petType, petBreed, ownerPn, ownerFirstName,
				ownerLastName, requestId, String.valueOf(dboOwner.getObject_id()), dboPet.getObject_id());
	}

	public DbDataObject createPassportRequest(String passportId, DateTime requestDate, String petId, String gender,
			String petType, String petBreed, String ownerPn, String ownerFirstName, String ownerLastName,
			String requestId, String ownerObjId, Long parentId) {
		DbDataObject dboRecord = new DbDataObject();
		dboRecord.setObject_type(SvReader.getTypeIdByName(Tc.PASSPORT_REQUEST));
		dboRecord.setParent_id(parentId);
		dboRecord.setStatus(Tc.DRAFT);
		dboRecord.setVal(Tc.REQUEST_ID, requestId);
		dboRecord.setVal(Tc.REQUEST_DATE, requestDate);
		dboRecord.setVal(Tc.PET_ID, petId);
		dboRecord.setVal(Tc.GENDER, gender);
		dboRecord.setVal(Tc.PET_TYPE, petType);
		dboRecord.setVal(Tc.PET_BREED, petBreed);
		dboRecord.setVal(Tc.OWNER_PRIVATE_NUMBER, ownerPn);
		dboRecord.setVal(Tc.OWNER_FIRST_NAME, ownerFirstName);
		dboRecord.setVal(Tc.OWNER_LAST_NAME, ownerLastName);
		dboRecord.setVal(Tc.PERSON_OBJ_ID, ownerObjId);
		return dboRecord;
	}

	/**
	 * 
	 * @param passportId
	 * @param deliveredTo
	 * @param dtDelivered
	 * @param dtIssiued
	 * @param dtExpiration
	 * @param requestObjectId
	 * @return
	 */
	public DbDataObject createPetPassport(String passportId, String deliveredTo, String holdingObjId,
			DateTime dtDelivered, DateTime dtIssiued, DateTime dtExpiration, String petObjId, Long parentId) {
		DbDataObject dboRecord = new DbDataObject();
		dboRecord.setObject_type(SvReader.getTypeIdByName(Tc.PET_PASSPORT));
		dboRecord.setParent_id(parentId);
		dboRecord.setVal(Tc.PASSPORT_ID, passportId);
		dboRecord.setVal(Tc.DELIVERED_TO, deliveredTo);
		dboRecord.setVal(Tc.HOLDING_OBJ_ID, holdingObjId);
		dboRecord.setVal(Tc.DT_DELIVERED, dtDelivered);
		dboRecord.setVal(Tc.DT_ISSIUED, dtIssiued);
		dboRecord.setVal(Tc.DT_EXPIRATION, dtExpiration);
		dboRecord.setVal(Tc.PET_OBJ_ID, petObjId);
		return dboRecord;
	}

	/**
	 * Method for assigning Owner (Holding Responsible) to Stray pet. When we
	 * assign Owner to Stray pet, it means that the pet has been adopted and new
	 * Pet object is created (According Stray Pet data)
	 * 
	 * @param dboStrayPet
	 *            Stray pet object
	 * @param dboPerson
	 *            Holding Responsible object
	 * @param svl
	 *            SvLink instance
	 * @param svw
	 *            SvWriter instance
	 * @param svr
	 *            SvReader instance
	 * @return String
	 * @throws SvException
	 */
	public String assignOwnerToStrayPet(DbDataObject dboStrayPet, DbDataObject dboPerson, String adoptionDate,
			SvLink svl, SvWriter svw, SvReader svr) throws SvException {
		Reader rdr = new Reader();
		String result = "naits.success.assignOwnerToStrayPer";
		String petId = dboStrayPet.getVal(Tc.PET_ID) != null ? dboStrayPet.getVal(Tc.PET_ID).toString() : "";
		String petType = dboStrayPet.getVal(Tc.PET_TYPE) != null ? dboStrayPet.getVal(Tc.PET_TYPE).toString() : "";
		DbDataObject dboPet = rdr.getPetByPetIdAndPetType(petId, petType, true, svr);
		if (dboPet != null && dboPet.getVal(Tc.IS_ADOPTED) != null && dboPet.getVal(Tc.IS_ADOPTED).equals(true)) {
			throw (new SvException("naits.error.strayPetIsAdopted", svCONST.systemUser, null, null));
		}
		dboPet = createDboPetAccordingStrayPet(dboStrayPet, adoptionDate, svw);
		if (dboPet != null) {
			svl.linkObjects(dboPet, dboPerson, Tc.PET_OWNER, null, false);
			dboStrayPet.setParent_id(dboPet.getObject_id());
			svw.saveObject(dboStrayPet, false);
			result += "_" + dboPet.getObject_id();
		}
		return result;
	}

	public DbDataObject createDboPet(String petId, String petTagType, String petType, String petBreed,
			Boolean isAdopted, DateTime registrationDate) {
		DbDataObject dboRecord = new DbDataObject();
		dboRecord.setObject_type(SvReader.getTypeIdByName(Tc.PET));
		dboRecord.setVal(Tc.PET_ID, petId);
		dboRecord.setVal(Tc.PET_TAG_TYPE, petTagType);
		dboRecord.setVal(Tc.PET_TYPE, petType);
		dboRecord.setVal(Tc.PET_BREED, petBreed);
		dboRecord.setVal(Tc.IS_ADOPTED, isAdopted);
		dboRecord.setVal(Tc.REGISTRATION_DATE, registrationDate);
		return dboRecord;
	}

	/**
	 * Method for creating vaccination treatment record for animal
	 * 
	 * @param animalObj
	 * @param vaccEventObj
	 * @param svw
	 * @throws SvException
	 */
	public void createVaccTreatmentRecord(DbDataObject animalObj, DbDataObject vaccEventObj, SvWriter svw)
			throws SvException {
		createVaccTreatmentRecord(animalObj, vaccEventObj, new DateTime().toString().substring(0, 10), null, svw);
	}

	/**
	 * 
	 * @param labSampleRegDate
	 * @param activityType
	 * @param svw
	 * @return
	 * @throws SvException
	 */
	public DbDataObject createVaccinationBook(String labSampleRegDate, String activityType, String labSampleId,
			SvWriter svw) throws SvException {
		DbDataObject dboRecord = new DbDataObject();
		dboRecord.setObject_type(SvReader.getTypeIdByName(Tc.VACCINATION_BOOK));
		dboRecord.setVal(Tc.VACC_DATE, labSampleRegDate);
		dboRecord.setVal(Tc.SAMPLE_ID, labSampleId);
		dboRecord.setVal(Tc.ACTIVITY_TYPE, activityType);// 2
		return dboRecord;
	}

	/**
	 * Method for starting animal or flock movement
	 * 
	 * @param animalOrFlockObj
	 * @param destinationHoldingObj
	 * @param moveReason
	 * @param dateOfMovement
	 * @param svr
	 * @param svw
	 * @param sww
	 * @throws SvException
	 */
	public DbDataObject startAnimalOrFlockMovement(DbDataObject animalOrFlockObj, DbDataObject destinationHoldingObj,
			String moveReason, String dateOfMovement, String movementDocId, String movementTransportType,
			String transporterLicense, String estmDateArrival, String estmDateDeparture, String disinfectionDate,
			String animalMvmReason, SvReader svr, SvWriter svw, SvWorkflow sww) throws SvException {
		DbDataObject dboAnimalMovement = null;
		ReentrantLock lock = null;
		if (animalOrFlockObj != null) {
			try {
				lock = SvLock.getLock("ANIM-MASS-HANDLER-START_MOV-" + animalOrFlockObj.getObject_id(), false, 0);
				if (lock == null) {
					throw (new SvException("naits.error.objectUsedByOtherSession", svr.getInstanceUser()));
				}
				dboAnimalMovement = createAnimalOrFlockMovementObject(animalOrFlockObj, destinationHoldingObj,
						moveReason, dateOfMovement, movementDocId, movementTransportType, transporterLicense,
						estmDateArrival, estmDateDeparture, disinfectionDate, animalMvmReason, svr, svw);
				if (dboAnimalMovement != null && !dboAnimalMovement.getStatus().equals(Tc.REJECTED)) {
					sww.moveObject(animalOrFlockObj, Tc.TRANSITION);
				}

			} finally {
				if (lock != null) {
					SvLock.releaseLock("ANIM-MASS-HANDLER-START_MOV-" + animalOrFlockObj.getObject_id(), lock);
				}
			}
		}
		return dboAnimalMovement;
	}

	/**
	 * Method for setting finished movements
	 * 
	 * @param animalOrFlockObj
	 * @param destinationHoldingObj
	 * @param dateOfMovement
	 * @param dateOfAdmission
	 * @param transporterPersonId
	 * @param svw
	 * @param sww
	 * @throws SvException
	 */
	public void finishAnimalOrFlockMovement(DbDataObject animalOrFlockObj, DbDataObject destinationHoldingObj,
			String dateOfMovement, String dateOfAdmission, String transporterPersonId, SvWriter svw, SvWorkflow sww)
			throws SvException {
		finishAnimalOrFlockMovement(animalOrFlockObj, destinationHoldingObj, dateOfMovement, dateOfAdmission,
				transporterPersonId, false, svw, sww);
	}

	/**
	 * Method for setting finished movements
	 * 
	 * @param animalOrFlockObj
	 * @param destinationHoldingObj
	 * @param dateOfMovement
	 * @param dateOfAdmission
	 * @param transporterPersonId
	 * @param svw
	 * @param sww
	 * @throws SvException
	 */
	public void finishAnimalOrFlockMovement(DbDataObject animalOrFlockObj, DbDataObject destinationHoldingObj,
			String dateOfMovement, String dateOfAdmission, String transporterPersonId, Boolean refreshCache,
			SvWriter svw, SvWorkflow sww) throws SvException {
		Reader rdr = new Reader();
		ValidationChecks vc = new ValidationChecks();
		String destinationHoldingPic = null;
		SvReader svr = null;
		ReentrantLock lock = null;
		DbDataObject animalOrFlockMovementObj = null;
		try {
			svr = new SvReader(svw);
			if (destinationHoldingObj.getVal(Tc.PIC) != null) {
				destinationHoldingPic = destinationHoldingObj.getVal(Tc.PIC).toString();
			}
			DbDataArray existingMovement = rdr.getExistingAnimalOrFlockMovements(animalOrFlockObj,
					destinationHoldingPic, Tc.VALID, false, svw);
			if (existingMovement.size() > 1) {
				throw (new SvException("naits.error.animalOrFlockHasMoreThanOneValidMovements", svCONST.systemUser,
						null, null));
			}
			if (existingMovement.size() == 1) {
				animalOrFlockMovementObj = existingMovement.get(0);
				lock = SvLock.getLock("ANIM-MASS-HANDLER-FINISH_MOV-" + animalOrFlockMovementObj.getObject_id(), false,
						0);
				if (lock == null) {
					throw (new SvException("naits.error.objectUsedByOtherSession", svr.getInstanceUser()));
				}
				animalOrFlockObj.setParent_id(Long.valueOf(destinationHoldingObj.getObject_id()));
				animalOrFlockMovementObj.setVal(Tc.USER_RECIPIENT, svr.getInstanceUser().getVal(Tc.USER_NAME));
				if (dateOfMovement != null && !dateOfMovement.equals("null")) {
					// String substr_date = dateOfMovement.substring(0, 10);
					String pattern = Tc.DATE_PATTERN;
					DateTime convertedDate = DateTime.parse(dateOfMovement, DateTimeFormat.forPattern(pattern));
					if (convertedDate.isAfter(new DateTime())) {
						throw (new SvException("naits.error.movementDateCannotBeInTheFuture", svCONST.systemUser, null,
								null));
					}
					animalOrFlockMovementObj.setVal(Tc.ARRIVAL_DATE, convertedDate);
					// svw.saveObject(animalOrFlockMovementObj, false);
				}
				if (animalOrFlockMovementObj.getVal(Tc.ARRIVAL_DATE) == null) {
					Calendar calendar = Calendar.getInstance();
					java.sql.Date dtNow = new java.sql.Date(calendar.getTime().getTime());
					animalOrFlockMovementObj.setVal(Tc.ARRIVAL_DATE, dtNow);
					// svw.saveObject(animalOrFlockMovementObj, false);
				}
				// case to invalidate/finish movement when UNDO-RETIRE animal
				if (animalOrFlockMovementObj.getVal(Tc.DESTINATION_HOLDING_ID) == null && animalOrFlockObj
						.getParent_id().equals(animalOrFlockMovementObj.getVal(Tc.DESTINATION_HOLDING_ID))) {
					animalOrFlockMovementObj.setVal(Tc.DESTINATION_HOLDING_ID, destinationHoldingPic);
					// svw.saveObject(animalOrFlockMovementObj, false);
				}
				if (animalOrFlockMovementObj.getVal(Tc.DESTINATION_HOLDING_ID) == null
						&& animalOrFlockObj.getStatus().equals(Tc.SOLD)
						|| animalOrFlockObj.getStatus().equals(Tc.LOST)) {
					animalOrFlockMovementObj.setVal(Tc.DESTINATION_HOLDING_ID, destinationHoldingPic);
				}
				if (animalOrFlockMovementObj.getVal(Tc.MOVEMENT_REASON) != null && (animalOrFlockMovementObj
						.getVal(Tc.MOVEMENT_REASON).toString().equalsIgnoreCase(Tc.SOLD)
						|| animalOrFlockMovementObj.getVal(Tc.MOVEMENT_REASON).toString().equalsIgnoreCase(Tc.LOST))) {
					animalOrFlockMovementObj.setVal(Tc.MOVEMENT_REASON, Tc.RETIRE_TRANSFER);
				}
				// used only for finishing movements or accepting animals that
				// arrived in slaughter house module
				if (vc.checkIfHoldingIsSlaughterhouse(destinationHoldingObj)) {
					if (dateOfAdmission != null && !"null".equals(dateOfAdmission)) {
						String pattern = Tc.DATE_PATTERN;
						DateTime convertedDate = DateTime.parse(dateOfAdmission, DateTimeFormat.forPattern(pattern));
						if (convertedDate.isAfter(new DateTime())) {
							throw (new SvException("naits.error.admittanceDateCannotBeInTheFuture", svCONST.systemUser,
									null, null));
						}
						if (animalOrFlockMovementObj.getVal(Tc.ARRIVAL_DATE) != null) {
							DateTime dtArrival = new DateTime(animalOrFlockMovementObj.getVal(Tc.ARRIVAL_DATE));
							if (DateTimeComparator.getDateOnlyInstance().compare(dtArrival, convertedDate) >= 1) {
								throw (new SvException("naits.error.dateOfMovementCannotBeAfterDateOfAdmission",
										svCONST.systemUser, null, null));
							}
						}
						animalOrFlockMovementObj.setVal(Tc.ADMITTANCE_DATE, convertedDate);
					}
					if (transporterPersonId != null && !"null".equals(transporterPersonId)) {
						Long holdingResponsibleObjId = Long.valueOf(transporterPersonId);
						DbDataObject dboHoldingResponsible = svr.getObjectById(holdingResponsibleObjId,
								SvReader.getTypeIdByName(Tc.HOLDING_RESPONSIBLE), null);
						animalOrFlockMovementObj.setVal(Tc.TRANSPORTER_ID, holdingResponsibleObjId);
						String holdingResponsibleFullName = "";
						if (dboHoldingResponsible.getVal(Tc.FULL_NAME) != null) {
							holdingResponsibleFullName = dboHoldingResponsible.getVal(Tc.FULL_NAME).toString();
						}
						if ((holdingResponsibleFullName == null || holdingResponsibleFullName.length() < 2)
								&& dboHoldingResponsible.getVal(Tc.FIRST_NAME) != null
								&& dboHoldingResponsible.getVal(Tc.LAST_NAME) != null) {
							holdingResponsibleFullName = dboHoldingResponsible.getVal(Tc.FIRST_NAME).toString() + " "
									+ dboHoldingResponsible.getVal(Tc.LAST_NAME).toString();
						}
						animalOrFlockMovementObj.setVal(Tc.TRANSPORTER_NAME, holdingResponsibleFullName);
					}
				}
				if (animalOrFlockMovementObj.getIs_dirty()) {
					animalOrFlockMovementObj.setStatus(Tc.FINISHED);
					svw.saveObject(animalOrFlockMovementObj, false);
				}
				sww.moveObject(animalOrFlockObj, Tc.VALID, false);
				vc.updateHoldingStatusAccordingHoldingTypeAfterSaveAnimal(animalOrFlockObj, svr);
				if (refreshCache) {
					@SuppressWarnings("unused")
					DbDataObject refreshAnimalCache = svr.getObjectById(animalOrFlockObj.getObject_id(),
							animalOrFlockObj.getObject_type(), new DateTime());
				}
			}
		} finally {
			if (svr != null)
				svr.release();
			if (lock != null && animalOrFlockMovementObj != null) {
				SvLock.releaseLock("ANIM-MASS-HANDLER-FINISH_MOV-" + animalOrFlockMovementObj.getObject_id(), lock);
			}
		}
	}

	/**
	 * Method that cancels created valid movement (changes status into Canceled)
	 * 
	 * @param animalOrFlockObj
	 *            ANIMAL or FLOCK DbDataObject
	 * @param destinationHoldingPic
	 *            Destination holding PIC
	 * @param sww
	 *            SvWorkflow instance
	 * @param svr
	 *            SvReader instance
	 * @throws SvException
	 */
	public void cancelMovement(DbDataObject animalOrFlockObj, SvWorkflow sww, SvReader svr) throws SvException {
		DbDataObject animalOrFlockMovementObj = null;
		ReentrantLock lock = null;
		try {
			String movementObjectType = Tc.ANIMAL_MOVEMENT;
			if (animalOrFlockObj.getObject_type().equals(SvReader.getTypeIdByName(Tc.FLOCK))) {
				movementObjectType = Tc.FLOCK_MOVEMENT;
			}
			DbDataArray existingMovement = svr.getObjectsByParentId(animalOrFlockObj.getObject_id(),
					SvReader.getTypeIdByName(movementObjectType), null, 0, 0, null);
			if (existingMovement != null && existingMovement.size() > 0) {
				for (DbDataObject tempObj : existingMovement.getItems()) {
					if (tempObj.getStatus().equals(Tc.VALID)) {
						animalOrFlockMovementObj = tempObj;
						lock = SvLock.getLock("ANIM-MASS-HANDLER-CANCEL-MOV-" + animalOrFlockMovementObj.getObject_id(),
								false, 0);
						if (lock == null) {
							throw (new SvException("naits.error.objectUsedByOtherSession", svr.getInstanceUser()));
						}
						break;
					}
				}
				if (animalOrFlockMovementObj != null && animalOrFlockMovementObj.getStatus().equals(Tc.VALID)) {
					if (!animalOrFlockObj.getStatus().equals(Tc.VALID)) {
						sww.moveObject(animalOrFlockObj, Tc.VALID);
					}
					sww.moveObject(animalOrFlockMovementObj, Tc.CANCELED);
				}
			}
		} finally {
			if (lock != null && animalOrFlockMovementObj != null) {
				SvLock.releaseLock("ANIM-MASS-HANDLER-CANCEL-MOV-" + animalOrFlockMovementObj.getObject_id(), lock);
			}
		}
	}

	public void changeStatus(DbDataObject dbo, String newStatus, SvWriter svw, Boolean dbCommit) throws SvException {
		SvWorkflow sww = new SvWorkflow(svw);
		changeStatus(dbo, newStatus, sww, dbCommit);
		sww.dbCommit();
		if (sww != null) {
			sww.release();
		}
	}

	public void changeStatus(DbDataObject dbo, String newStatus, SvWriter svw) throws SvException {
		changeStatus(dbo, newStatus, svw, true);
	}

	public void changeStatus(DbDataObject dbo, String newStatus, SvWorkflow sww) throws SvException {
		sww.moveObject(dbo, newStatus);
	}

	public void changeStatus(DbDataObject dbo, String newStatus, SvWorkflow sww, Boolean dbCommit) throws SvException {
		sww.moveObject(dbo, newStatus, dbCommit);
	}

	/**
	 * Method for creating animal or flock movement without holding destination
	 * 
	 * @param movementParentObj
	 * @param typeOfMovement
	 * @param svr
	 * @param svw
	 * @param sww
	 * @throws SvException
	 */
	public void createAnimalOrFlockMovementWithoutDestination(DbDataObject movementParentObj, String typeOfMovement,
			String dateOfMovement, SvReader svr, SvWriter svw, SvWorkflow sww) throws SvException {
		createAnimalOrFlockMovementObject(movementParentObj, null, typeOfMovement, dateOfMovement, null, null, null,
				null, null, null, null, svr, svw);
		sww.moveObject(movementParentObj, typeOfMovement);
	}

	/**
	 * Method for creating animal or flock movement
	 * 
	 * @param objToMove
	 * @param destinationHoldingObj
	 * @param moveReason
	 * @param dateOfMovement
	 * @param movementDocId
	 * @param movementTransportType
	 * @param transporterLicense
	 * @param estmDateArrival
	 * @param estmDateDeparture
	 * @param disinfectionDate
	 * @param animalMvmReason
	 * @param svr
	 * @param svw
	 * @return
	 * @throws SvException
	 */
	public DbDataObject createAnimalOrFlockMovementObject(DbDataObject objToMove, DbDataObject destinationHoldingObj,
			String moveReason, String dateOfMovement, String movementDocId, String movementTransportType,
			String transporterLicense, String estmDateArrival, String estmDateDeparture, String disinfectionDate,
			String animalMvmReason, SvReader svr, SvWriter svw) throws SvException {
		String pattern = Tc.DATE_PATTERN;
		Reader rdr = new Reader();
		ValidationChecks vc = new ValidationChecks();
		DbDataArray existingMovement = new DbDataArray();
		if (destinationHoldingObj != null) {
			existingMovement = rdr.getExistingAnimalOrFlockMovements(objToMove,
					destinationHoldingObj.getVal(Tc.PIC).toString(), Tc.VALID, false, svw);
		}
		if (!existingMovement.getItems().isEmpty()) {
			throw (new SvException("naits.error.animalOrFlockHasMoreThanOneValidMovements", svCONST.systemUser, null,
					null));
		}
		DbDataObject dboRecord = new DbDataObject();

		if (objToMove.getObject_type().equals(SvReader.getTypeIdByName(Tc.ANIMAL))) {
			dboRecord.setObject_type(SvReader.getTypeIdByName(Tc.ANIMAL_MOVEMENT));
		}
		if (objToMove.getObject_type().equals(SvReader.getTypeIdByName(Tc.FLOCK))) {
			dboRecord.setObject_type(SvReader.getTypeIdByName(Tc.FLOCK_MOVEMENT));
		}
		// parent_id of the movement should always be the id of animal/flock
		dboRecord.setParent_id(objToMove.getObject_id());
		// here should be appropriate valid movement reason
		dboRecord.setVal(Tc.MOVEMENT_REASON, moveReason);
		// source holding id is always known as parent id of objToMove
		DbDataObject sourceHolding = svr.getObjectById(objToMove.getParent_id(), SvReader.getTypeIdByName(Tc.HOLDING),
				null);
		dboRecord.setVal(Tc.SOURCE_HOLDING_ID, sourceHolding.getVal(Tc.PIC).toString());
		// if there is known destination holding id, save it
		if (destinationHoldingObj != null) {
			dboRecord.setVal(Tc.DESTINATION_HOLDING_ID, destinationHoldingObj.getVal(Tc.PIC).toString());
		}
		if (!(Tc.LOST.equals(moveReason) || Tc.SOLD.equals(moveReason))) {
			if (vc.checkIfHoldingBelongsToAffectedArea(sourceHolding, svr)
					|| vc.checkIfHoldingBelongsToAffectedArea(destinationHoldingObj, svr)) {
				Boolean isAllowed = rdr.checkifMovementIsAllowedDependOnAreaHealthStatus(sourceHolding, "",
						new ArrayList<>(), new ArrayList<>(), objToMove, dboRecord, svr);
				if (!isAllowed.booleanValue()) {
					dboRecord.setStatus(Tc.REJECTED);
				}
			}
		}

		if (objToMove.getObject_type().equals(SvReader.getTypeIdByName(Tc.ANIMAL))) {
			dboRecord.setVal(Tc.ANIMAL_EAR_TAG, objToMove.getVal(Tc.ANIMAL_ID));
			dboRecord.setVal(Tc.MOVEMENT_TYPE, Tc.INDIVIDUAL);
		}
		if (objToMove.getObject_type().equals(SvReader.getTypeIdByName(Tc.FLOCK))) {
			dboRecord.setVal(Tc.FLOCK_ID, objToMove.getVal(Tc.FLOCK_ID));
			dboRecord.setVal(Tc.MOVEMENT_TYPE, Tc.GROUP);
		}
		if (dateOfMovement != null && !dateOfMovement.equals("null") && !dateOfMovement.equals("")) {
			DateTime convertedDate = DateTime.parse(dateOfMovement, DateTimeFormat.forPattern(pattern));
			String substr_date = convertedDate.toString().substring(0, 10);
			dboRecord.setVal(Tc.DEPARTURE_DATE, substr_date);
		}
		if (dboRecord.getVal(Tc.DEPARTURE_DATE) == null || dateOfMovement.equals("null") || dateOfMovement.equals("")) {
			if (destinationHoldingObj != null && vc.checkIfHoldingIsSlaughterhouse(destinationHoldingObj)
					&& moveReason != null && !moveReason.equals("null") && !moveReason.equals("")) {
				if (!moveReason.equals(Tc.DIRECT_TRANSFER)) {
					dboRecord.setVal(Tc.DEPARTURE_DATE, new DateTime());
				}
			} else {
				dboRecord.setVal(Tc.DEPARTURE_DATE, new DateTime());
			}
		}
		if (movementDocId != null) {
			dboRecord.setVal(Tc.MOVEMENT_DOC_ID, movementDocId);
		}
		if (movementTransportType != null) {
			dboRecord.setVal(Tc.MOVEMENT_TRANSPORT_TYPE, movementTransportType);
		}
		if (transporterLicense != null) {
			dboRecord.setVal(Tc.TRANSPORTER_LICENSE, transporterLicense);
		}
		if (estmDateArrival != null && !estmDateArrival.equals("null") && !estmDateArrival.equals("")) {
			DateTime convertedDate = DateTime.parse(estmDateArrival, DateTimeFormat.forPattern(pattern));
			String substr_date = convertedDate.toString().substring(0, 10);
			dboRecord.setVal(Tc.ESTM_DATE_ARRIVAL, substr_date);
		}
		if (estmDateDeparture != null && !estmDateDeparture.equals("null") && !estmDateDeparture.equals("")) {
			DateTime convertedDate = DateTime.parse(estmDateDeparture, DateTimeFormat.forPattern(pattern));
			String substr_date = convertedDate.toString().substring(0, 10);
			dboRecord.setVal(Tc.ESTM_DATE_DEPARTURE, substr_date);
		}
		if (dboRecord.getVal(Tc.ESTM_DATE_DEPARTURE) == null || estmDateDeparture.equals("null")
				|| dateOfMovement.equals("")) {
			setAutoDate(dboRecord, Tc.ESTM_DATE_DEPARTURE);
		}
		if (disinfectionDate != null && !disinfectionDate.equals("null") && !disinfectionDate.equals("")) {
			DateTime convertedDate = DateTime.parse(disinfectionDate, DateTimeFormat.forPattern(pattern));
			if (convertedDate.isAfter(new DateTime())) {
				throw (new SvException("naits.error.disinfectionDateCannotBeInTheFuture", svCONST.systemUser, null,
						null));
			}
			String substr_date = convertedDate.toString().substring(0, 10);
			dboRecord.setVal(Tc.DISINFECTION_DATE, substr_date);
		}
		if (animalMvmReason != null) {
			if (objToMove.getObject_type().equals(SvReader.getTypeIdByName(Tc.ANIMAL))) {
				dboRecord.setVal(Tc.ANIMAL_MVM_REASON, animalMvmReason);
			} else if (objToMove.getObject_type().equals(SvReader.getTypeIdByName(Tc.FLOCK))) {
				dboRecord.setVal(Tc.FLOCK_MVM_REASON, animalMvmReason);
			}
		}
		dboRecord.setVal(Tc.USER_SENDER, svr.getInstanceUser().getVal(Tc.USER_NAME));
		svw.saveObject(dboRecord);
		if (objToMove.getObject_type().equals(SvReader.getTypeIdByName(Tc.ANIMAL))) {
			if (destinationHoldingObj != null
					&& rdr.checkIfAnimalOrFlockMovementExist(objToMove, destinationHoldingObj, svw)) {
				linkObjects(destinationHoldingObj, dboRecord, Tc.ANIMAL_MOVEMENT_HOLDING, "linked via WS", svw);
			} else if (destinationHoldingObj == null) {
				linkObjects(sourceHolding, dboRecord, Tc.ANIMAL_MOVEMENT_HOLDING, "linked via WS", svw);
			} else if (Tc.DIRECT_TRANSFER.equals(moveReason)
					&& rdr.checkIfAnimalOrFlockMovementExist(objToMove, destinationHoldingObj, true, svw)) {
				linkObjects(destinationHoldingObj, dboRecord, Tc.ANIMAL_MOVEMENT_HOLDING, "linked via WS", svw);
			}
		} else if (objToMove.getObject_type().equals(SvReader.getTypeIdByName(Tc.FLOCK))) {
			if (destinationHoldingObj != null
					&& rdr.checkIfAnimalOrFlockMovementExist(objToMove, destinationHoldingObj, svw)) {
				linkObjects(destinationHoldingObj, dboRecord, Tc.FLOCK_MOVEMENT_HOLDING, "linked via WS", svw);
			} else if (destinationHoldingObj == null) {
				linkObjects(sourceHolding, dboRecord, Tc.FLOCK_MOVEMENT_HOLDING, "linked via WS", svw);
			} else if (Tc.DIRECT_TRANSFER.equals(moveReason)
					&& rdr.checkIfAnimalOrFlockMovementExist(objToMove, destinationHoldingObj, true, svw)) {
				linkObjects(destinationHoldingObj, dboRecord, Tc.FLOCK_MOVEMENT_HOLDING, "linked via WS", svw);
			}
		}
		return dboRecord;
	}

	/**
	 * Method for updating keeper name if needed
	 * 
	 * @param holdingObjId
	 * @param holderObjId
	 * @param svr
	 * @throws SvException
	 */
	public void updateKeeperNameIfNeeded(DbDataObject dboHolding, DbDataObject dboHolder) throws SvException {
		if (dboHolding != null && dboHolder != null) {
			String holdingKeeperName = null;
			if (dboHolding.getVal(Tc.NAME) != null)
				holdingKeeperName = dboHolding.getVal(Tc.NAME).toString();
			String holderFullName = null;
			if (dboHolder.getVal(Tc.FULL_NAME) != null)
				holderFullName = dboHolder.getVal(Tc.FULL_NAME).toString();

			if ((holderFullName != null && holdingKeeperName != null && !holdingKeeperName.equals(holderFullName))
					|| (holderFullName != null && holdingKeeperName == null)) {
				dboHolding.setVal(Tc.NAME, holderFullName);
			}
		}
	}

	public void updateKeeperInfoInHolding(DbDataObject dboHolding, DbDataObject dboHolder) throws SvException {
		if (dboHolder.getVal(Tc.NAT_REG_NUMBER) != null) {
			dboHolding.setVal(Tc.KEEPER_ID, dboHolder.getVal(Tc.NAT_REG_NUMBER).toString());
		}
		if (dboHolder.getVal(Tc.PHONE_NUMBER) != null) {
			dboHolding.setVal(Tc.KEEPER_MOBILE_NUM, dboHolder.getVal(Tc.PHONE_NUMBER).toString());
		} else if (dboHolder.getVal(Tc.MOBILE_NUMBER) != null) {
			dboHolding.setVal(Tc.KEEPER_MOBILE_NUM, dboHolder.getVal(Tc.MOBILE_NUMBER).toString());
		}
	}

	public void removeKeeperInfoInHolding(DbDataObject dboHolding) throws SvException {
		dboHolding.setVal(Tc.NAME, null);
		dboHolding.setVal(Tc.KEEPER_ID, null);
		dboHolding.setVal(Tc.KEEPER_MOBILE_NUM, null);
	}

	/**
	 * Method for status update of Holding. Allows null geometry
	 * 
	 * @param dboHolding
	 * @param nextStatus
	 * @param svr
	 * @throws SvException
	 */
	public void updateHoldingStatus(DbDataObject dboHolding, String prevStatus, String nextStatus, boolean suspendCheck,
			SvReader svr) throws SvException {
		svr.setIncludeGeometries(true);
		SvGeometry svg = null;
		ValidationChecks vc = null;
		Reader rdr = null;
		try {
			svg = new SvGeometry(svr);
			svg.setAllowNullGeometry(true);
			vc = new ValidationChecks();
			rdr = new Reader();
			if (dboHolding != null) {
				if (prevStatus == null || dboHolding.getStatus().equals(prevStatus)) {
					// commercial and subsistence farm
					if (suspendCheck && !Tc.NO_KEEPER.equals(nextStatus)
							&& vc.checkIfHoldingIsCommercialOrSubsistenceFarmType(dboHolding)) {
						DbDataArray arrAnimals = rdr.getValidAnimalsOrFlockByParentId(dboHolding.getObject_id(),
								SvReader.getTypeIdByName(Tc.ANIMAL), svr);
						DbDataArray arrFlocks = rdr.getValidAnimalsOrFlockByParentId(dboHolding.getObject_id(),
								SvReader.getTypeIdByName(Tc.FLOCK), svr);
						if (arrAnimals.getItems().isEmpty() && arrFlocks.getItems().isEmpty()) {
							dboHolding.setStatus(Tc.SUSPENDED);
						} else {
							if (nextStatus.equals(Tc.VALID)) {
								dboHolding.setStatus(Tc.VALID);
							}
						}
					} else {
						dboHolding.setStatus(nextStatus);
					}
				}
				if (dboHolding.getIs_dirty()) {
					svg.saveGeometry(dboHolding);
				}
			}
		} finally {
			if (svg != null) {
				svg.release();
			}
		}
	}

	public void updateHoldingStatus(DbDataObject dboHolding, String nextStatus, SvReader svr) throws SvException {
		updateHoldingStatus(dboHolding, null, nextStatus, svr);
	}

	public void updateHoldingStatus(DbDataObject dboHolding, String prevStatus, String nextStatus, SvReader svr)
			throws SvException {
		updateHoldingStatus(dboHolding, prevStatus, nextStatus, false, svr);
	}

	/**
	 * Method for generating FIC per flock
	 * 
	 * @param dboFlock
	 * @param svr
	 */
	public void generateFicPerFlock(DbDataObject dboFlock, SvReader svr) {
		SvSequence svs = null;
		if (svr.getSessionId() != null) {
			try {
				svs = new SvSequence(svr.getSessionId());
				// svw = new SvWriter(svs);
				String pic = "";
				if (dboFlock.getParent_id() != null) {
					DbDataObject dboHolding = svr.getObjectById(dboFlock.getParent_id(),
							SvReader.getTypeIdByName(Tc.HOLDING), null);
					if (dboHolding.getVal(Tc.PIC) != null) {
						pic = dboHolding.getVal(Tc.PIC).toString();
					}
				}
				if (!pic.equals("")) {
					Long seqId = svs.getSeqNextVal(pic, false);
					String formattedSeq = String.format("%03d", Integer.valueOf(seqId.toString()));
					String generatedFic = pic + "-" + formattedSeq;
					dboFlock.setVal(Tc.FLOCK_ID, generatedFic);
					svs.dbCommit();
				}
			} catch (SvException e) {
				log4j.error(e.getFormattedMessage(), e);
			} finally {
				if (svs != null)
					svs.release();
			}
		}
	}

	/**
	 * Method that calculates sum of males and females animals per flock
	 * 
	 * @param dboFlock
	 * @param svr
	 */
	public void calculateSumForFlock(DbDataObject dboFlock, SvReader svr) {
		if (svr.getSessionId() != null && dboFlock != null) {
			Long males_cnt = 0L;
			if (dboFlock.getVal(Tc.MALES) != null) {
				males_cnt = (Long) dboFlock.getVal(Tc.MALES);
			} else {
				dboFlock.setVal(Tc.MALES, males_cnt);
			}
			Long females_cnt = 0L;
			if (dboFlock.getVal(Tc.FEMALES) != null) {
				females_cnt = (Long) dboFlock.getVal(Tc.FEMALES);
			} else {
				dboFlock.setVal(Tc.FEMALES, females_cnt);
			}
			Long total = males_cnt + females_cnt;
			dboFlock.setVal(Tc.TOTAL, total);
		}
	}

	public void setFlockElementsToZero(DbDataObject dboFlock, String fieldName, Boolean checkIsNull)
			throws SvException {
		if ((!checkIsNull && dboFlock != null)
				|| ((checkIsNull && dboFlock != null && dboFlock.getVal(fieldName) == null))) {
			dboFlock.setVal(fieldName, 0);
		}
	}

	/**
	 * Method for creating new user
	 * 
	 * @param jsonData
	 * @param sessionId
	 */
	public String createUser(JsonObject jsonData, String sessionId) {
		String result = "naits.success.createUser";
		SvReader svr = null;
		SvWriter svw = null;
		SvSecurity svs = null;
		String userFirstName = null;
		String userLastName = null;
		String userEmail = null;
		String userPin = null;
		String userPassword = null;
		String userName = null;
		String userGroupName = null;
		DbDataObject dboUser = null;
		try {
			UserManager um = new UserManager();
			svr = new SvReader(sessionId);
			svw = new SvWriter(svr);
			svs = new SvSecurity(svw);
			svs.dbSetAutoCommit(false);
			svw.dbSetAutoCommit(false);
			JsonObject obj = jsonData;
			if (obj.get("USER.FIRST_NAME") == null || obj.get("USER.LAST_NAME") == null || obj.get("USER.EMAIL") == null
					|| obj.get("USER.PIN") == null || obj.get("USER.PASSWORD") == null
					|| obj.get("USER.USER_GROUP") == null) {
				throw (new SvException("naits.error.createUser.hasNullData", svr.getInstanceUser()));
			}
			userFirstName = obj.get("USER.FIRST_NAME").toString().replace("\"", "").toUpperCase();
			userLastName = obj.get("USER.LAST_NAME").toString().replace("\"", "").toUpperCase();
			userEmail = obj.get("USER.EMAIL").toString().replace("\"", "");
			userPin = obj.get("USER.PIN").toString().replace("\"", "").toUpperCase();
			userPassword = obj.get("USER.PASSWORD").toString().replace("\"", "").toUpperCase();
			userName = userFirstName.trim().substring(0, 1).toUpperCase() + '.' + userLastName.trim().toUpperCase();
			userGroupName = obj.get("USER.USER_GROUP").toString().replace("\"", "").toUpperCase();
			// create user
			UserManager userManager = new UserManager();
			dboUser = userManager.createDefaultTestUser(userName, userFirstName, userLastName, userEmail, userPin,
					userPassword, svr);
			DbDataObject dboGroup = userManager.findUserOrUserGroup(svCONST.OBJECT_TYPE_GROUP, Tc.GROUP_NAME,
					userGroupName, svr);
			svw.saveObject(dboUser);
			if (dboGroup == null) {
				throw (new SvException("naits.error.userGroupDoesNotExist", svr.getInstanceUser()));
			}
			svw.dbCommit();
			svs.addUserToGroup(dboUser, dboGroup, true);
			ArrayList<String> mandatoryPermissions = new ArrayList<String>();
			mandatoryPermissions = um.getSvarogCoreTablePermission(svr);
			mandatoryPermissions.add("system.null_geometry");
			um.setSidPermission(dboUser.getVal(Tc.USER_NAME).toString(), svCONST.OBJECT_TYPE_USER, mandatoryPermissions,
					"GRANT", svr.getSessionId());
			svw.dbCommit();
		} catch (SvException sve) {
			result = sve.getLabelCode();
			log4j.error(sve.getFormattedMessage(), sve);
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
		return result;
	}

	public DbDataObject createAutoGeneratedAnimalObject(String animalId, String countryOrigin, String animalClass,
			java.sql.Date registrationDate, Boolean isAutoGenerated, Long parentId) {
		DbDataObject dboAnimal = new DbDataObject();
		dboAnimal.setObject_type(SvReader.getTypeIdByName(Tc.ANIMAL));
		dboAnimal.setVal(Tc.ANIMAL_ID, animalId);
		dboAnimal.setVal(Tc.ANIMAL_CLASS, animalClass);
		dboAnimal.setVal(Tc.ANIMAL_RACE, " ");
		dboAnimal.setVal(Tc.REGISTRATION_DATE, registrationDate);
		dboAnimal.setVal(Tc.COUNTRY, countryOrigin);
		dboAnimal.setVal(Tc.AUTO_GENERATED, isAutoGenerated);
		dboAnimal.setParent_id(parentId);
		return dboAnimal;
	}

	public String generateAnimalObjects(Long holdingObjId, String startTagId, String endTagId, String animalClass,
			SvReader svr, SvWriter svw) throws SvException {
		String result = "naits.success.successfullyAddedAnimalsWithAnimalMassGeneratorAction";
		Reader rdr = new Reader();
		if (holdingObjId != null) {
			// Reader instance
			ReentrantLock lock = null;
			try {
				lock = SvLock.getLock(String.valueOf(holdingObjId), false, 0);
				if (lock == null) {
					throw (new SvException("naits.error.objectUsedByOtherSession", svr.getInstanceUser()));
				}
				// Objects to use and array for proper committing
				DbDataObject dboAnimal = null;
				DbDataObject tempAnimalToSearch = null;
				DbDataArray objectsToSave = new DbDataArray();

				// Checks if fields are filled appropriate
				if ((startTagId == null || startTagId.equals("") || !NumberUtils.isNumber(startTagId))) {
					throw (new SvException("naits.error.startTagTagInvalidNumberFormat", svr.getInstanceUser()));
				}
				// Checks if fields are filled appropriate
				if ((endTagId == null || endTagId.equals("") || !NumberUtils.isNumber(endTagId))) {
					throw (new SvException("naits.error.endTagTagInvalidNumberFormat", svr.getInstanceUser()));
				}
				if (animalClass == null || animalClass.equals("")) {
					throw (new SvException("naits.error.anmalBreedCantBeNull", svr.getInstanceUser()));
				}
				// tag conversion
				long startTagInt = Long.valueOf(startTagId);
				long endTagInt = Long.valueOf(endTagId);

				if (startTagInt > endTagInt) {
					throw (new SvException("naits.error.startTagIdCannotBeLargerThanEndTagId", svr.getInstanceUser()));
				}

				// Get todays date
				Calendar calendar = Calendar.getInstance();
				java.sql.Date dtNow = new java.sql.Date(calendar.getTime().getTime());
				int counter = 0;
				for (Long i = startTagInt; i <= endTagInt; i++) {
					tempAnimalToSearch = rdr.findAppropriateAnimalByAnimalIdAndAnimalClass(i.toString(), animalClass,
							true, svr);
					if (tempAnimalToSearch != null) {
						throw (new SvException("naits.error.exsistingAnimalsWithAnimalIdEnteredInTheRange",
								svr.getInstanceUser()));
					}
					dboAnimal = createAutoGeneratedAnimalObject(i.toString(), "GE", animalClass, dtNow, true,
							holdingObjId);
					dboAnimal.setVal(Tc.CHECK_COLUMN, true);
					objectsToSave.addDataItem(dboAnimal);
					counter++;
					if (counter == COMMIT_COUNT) {
						counter = 0;
						svw.saveObject(objectsToSave, true, true);
						objectsToSave = new DbDataArray();
					}
				}
				if (objectsToSave.getItems().size() > 0) {
					svw.saveObject(objectsToSave, true, true);
				}
			} finally {
				if (lock != null) {
					SvLock.releaseLock(String.valueOf(holdingObjId), lock);
				}
			}
		}
		return result;
	}

	public String generateInventoryItem(JsonArray jsonData, SvReader svr, SvWriter svw) throws SvException {
		String retMessage = "";
		SvWorkflow sww = null;
		try {
			sww = new SvWorkflow(svw);
			DbDataObject orderTypeDesc = SvReader.getDbtByName(Tc.ORDER);
			DbDataObject rangeTypeDesc = SvReader.getDbtByName(Tc.RANGE);
			DbDataObject dboOrder = null;
			for (int i = 0; i < jsonData.size(); i++) {
				JsonObject obj = jsonData.get(i).getAsJsonObject();
				DbDataArray objectsToSave = new DbDataArray();
				DbDataObject dboObjectToHandle = null;
				String tag_type = "";
				int recCount = 0;
				if (obj.get("RANGE.OBJECT_ID") != null) {
					dboObjectToHandle = svr.getObjectById(obj.get("RANGE.OBJECT_ID").getAsLong(), rangeTypeDesc, null);
				}
				if (obj.get("RANGE.PARENT_ID") != null) {
					dboOrder = svr.getObjectById(obj.get("RANGE.PARENT_ID").getAsLong(), orderTypeDesc, null);
				}
				if (dboOrder == null) {
					throw (new SvException("naits.error.noObjectfound", svr.getInstanceUser()));
				}
				if (dboObjectToHandle.getStatus().equals(Tc.RELEASED)) {
					throw (new SvException("naits.error.rangeMustBeValid", svr.getInstanceUser()));
				}
				if (dboOrder.getStatus().equals(Tc.DELIVERED)) {
					ReentrantLock lock = null;
					try {
						lock = SvLock.getLock(String.valueOf(dboObjectToHandle.getObject_id()), true, 0);
						if (lock == null) {
							throw (new SvException("naits.error.objectUsedByOtherSession", svr.getInstanceUser()));
						}
						DbDataObject dbInventory = null;
						DbDataObject orgUnit = svr.getObjectById(dboOrder.getParent_id(), svCONST.OBJECT_TYPE_ORG_UNITS,
								null);
						String order_Num = dboOrder.getVal(Tc.ORDER_NUMBER) != null
								? dboOrder.getVal(Tc.ORDER_NUMBER).toString() : null;
						Long startRange = null;
						Long endRange = null;
						Long parent_id = orgUnit.getObject_id();
						startRange = obj.get("RANGE.START_TAG_ID").getAsLong();
						endRange = obj.get("RANGE.END_TAG_ID").getAsLong();
						tag_type = obj.get("RANGE.TAG_TYPE").getAsString();
						for (Long k = startRange; k <= endRange; k++) {
							dbInventory = new DbDataObject(SvReader.getTypeIdByName("INVENTORY_ITEM"));
							dbInventory.setVal(Tc.EAR_TAG_NUMBER, k.toString());
							dbInventory.setVal(Tc.TAG_TYPE, tag_type);
							if (order_Num != null) {
								dbInventory.setVal(Tc.ORDER_NUMBER, order_Num);
							}
							dbInventory.setParent_id(parent_id);
							dbInventory.setVal(Tc.TAG_STATUS, Tc.NON_APPLIED);
							objectsToSave.addDataItem(dbInventory);
							recCount++;
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
						sww.moveObject(dboObjectToHandle, Tc.PROCESSED, true);
					} finally {
						if (i == jsonData.size() - 1) {
							DbDataArray arrRanges = svr.getObjectsByParentId(dboOrder.getObject_id(),
									rangeTypeDesc.getObject_id(), null, 0, 0);
							if (!arrRanges.getItems().isEmpty() && dboOrder != null) {
								int count = 0;
								for (DbDataObject dboRange : arrRanges.getItems()) {
									if (dboRange.getStatus().equals(Tc.PROCESSED)) {
										count++;
									}
								}
								if (count == arrRanges.size()) {
									sww.moveObject(dboOrder, Tc.RELEASED, false);
								}
							}
							svr.dbCommit();
							svw.dbCommit();
							sww.dbCommit();
						}
						if (lock != null) {
							SvLock.releaseLock(String.valueOf(dboObjectToHandle.getObject_id()), lock);
						}
					}
				} else {
					retMessage = "naits.info.invalidObjStatus";
					break;
				}
			}
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
		} finally {
			if (sww != null) {
				sww.release();
			}
		}
		return retMessage;
	}

	public String moveTransferRangeAndGenerateToInventoryItem(JsonArray jsonData, String sessionId) throws Exception {
		String result = "naits.success.saveInventoryItem";
		SvReader svr = null;
		SvWriter svw = null;
		SvWorkflow sww = null;
		Reader rdr = null;
		try {

			svr = new SvReader(sessionId);
			svw = new SvWriter(svr);
			sww = new SvWorkflow(svw);

			rdr = new Reader();
			String destinationOrgUnit = "";

			DbDataObject transferTypeDesc = SvReader.getDbtByName(Tc.TRANSFER);
			for (int i = 0; i < jsonData.size(); i++) {
				JsonObject obj = jsonData.get(i).getAsJsonObject();
				DbDataObject dboObjectToHandle = null;

				if (obj.get("TRANSFER.OBJECT_ID") != null) {
					dboObjectToHandle = svr.getObjectById(obj.get("TRANSFER.OBJECT_ID").getAsLong(), transferTypeDesc,
							new DateTime());
				}

				if (dboObjectToHandle == null) {
					throw (new SvException("naits.error.no_transfer_found", svr.getInstanceUser()));
				}
				ReentrantLock lock = null;
				try {
					lock = SvLock.getLock(String.valueOf(dboObjectToHandle.getObject_id()), false, 0);
					if (lock == null) {
						throw (new SvException("naits.error.objectUsedByOtherSession", svr.getInstanceUser()));
					}
					if (!dboObjectToHandle.getStatus().equals(Tc.DELIVERED)) {
						throw (new SvException("naits.error.transferMustBeDelivered", svr.getInstanceUser()));
					}

					if (dboObjectToHandle.getVal(Tc.DESTINATION_OBJ_ID) != null
							&& !dboObjectToHandle.getVal(Tc.DESTINATION_OBJ_ID).toString().equals("")) {
						destinationOrgUnit = dboObjectToHandle.getVal(Tc.DESTINATION_OBJ_ID).toString();
					} else {
						throw (new SvException("naits.error.transferDoesntHaveDestinationOrgUnit",
								svr.getInstanceUser()));
					}

					Long startRange = null;
					Long endRange = null;
					String tagType = "";

					DbDataArray objectsToSave = new DbDataArray();
					int recCount = 0;

					DbDataObject orgUnit = svr.getObjectById(Long.valueOf(destinationOrgUnit),
							svCONST.OBJECT_TYPE_ORG_UNITS, null);

					if (orgUnit == null) {
						orgUnit = svr.getObjectById(Long.valueOf(destinationOrgUnit),
								SvReader.getTypeIdByName(Tc.HOLDING), null);
					}
					Long destinationParentId = orgUnit.getObject_id();
					Long currentParentId = obj.get("TRANSFER.PARENT_ID").getAsLong();
					startRange = obj.get("TRANSFER.START_TAG_ID").getAsLong();
					endRange = obj.get("TRANSFER.END_TAG_ID").getAsLong();
					tagType = obj.get("TRANSFER.TAG_TYPE").getAsString();
					for (Long j = startRange; j <= endRange; j++) {
						String currentEarTag = String.valueOf(j);
						DbDataObject tempDboInventory = rdr.getDboInventoryItemDependOnTransfer(currentParentId,
								currentEarTag, tagType, svr);
						if (tempDboInventory != null) {
							tempDboInventory.setParent_id(destinationParentId);
							objectsToSave.addDataItem(tempDboInventory);
							recCount++;
						}
						if (recCount == COMMIT_COUNT) {
							recCount = 0;
							svw.saveObject(objectsToSave, true, true);
							objectsToSave = new DbDataArray();
						}
					}
					if (!objectsToSave.getItems().isEmpty()) {
						recCount = 0;
						svw.saveObject(objectsToSave, true, true);
						objectsToSave = new DbDataArray();
					}
					sww.moveObject(dboObjectToHandle, Tc.RELEASED, true);
				} finally {
					if (lock != null && dboObjectToHandle != null) {
						SvLock.releaseLock(String.valueOf(dboObjectToHandle.getObject_id()), lock);
					}
				}
			}
		} catch (SvException sve) {
			log4j.error(sve.getFormattedMessage(), sve);
			result = sve.getLabelCode();
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
		return result;
	}

	public void generateLinksBetweenHoldingAndQuarantineViaMap(DbDataObject qDbo, DbDataArray hArr, SvWriter svw)
			throws SvException {
		// add links for all intersecting holdings
		DbDataObject linkDbo = null;
		DbDataObject linkType = SvLink.getLinkType(Tc.HOLDING_QUARANTINE, SvCore.getTypeIdByName(Tc.HOLDING),
				qDbo.getObject_type());
		int i = 0;

		for (DbDataObject hDbo : hArr.getItems()) {
			linkDbo = new DbDataObject(svCONST.OBJECT_TYPE_LINK);
			linkDbo.setVal(Tc.LINK_TYPE_ID, linkType.getObject_id());
			linkDbo.setVal(Tc.LINK_OBJ_ID_1, hDbo.getObject_id());
			linkDbo.setVal(Tc.LINK_OBJ_ID_2, qDbo.getObject_id());
			linkDbo.setVal(Tc.LINK_NOTES, "linked via map batch");
			try {
				svw.saveObject(linkDbo, false);
				i++;
			} catch (Exception e) {
				log4j.error("Save failed for " + linkDbo.getObject_id() + "due to exception: " + e.toString());
			}
			if (i == 1000) {
				svw.dbCommit();
				i = 0;
			}
		}

		svw.dbCommit();
	}

	/**
	 * Method for creating Quarantine object
	 * 
	 * @param dateFrom
	 * @param dateTo
	 * @param reason
	 * @param quarantineType
	 * @return DbDataObject
	 */
	public DbDataObject createQuarantineObject(String dateFrom, String dateTo, String reason, String quarantineType) {
		DbDataObject quarantineObj = new DbDataObject();
		quarantineObj.setObject_type(SvReader.getTypeIdByName(Tc.QUARANTINE));
		quarantineObj.setVal(Tc.DATE_FROM, dateFrom);
		quarantineObj.setVal(Tc.DATE_TO, dateTo);
		quarantineObj.setVal(Tc.REASON, reason);
		quarantineObj.setVal(Tc.QUARANTINE_TYPE, quarantineType);
		return quarantineObj;
	}

	public void createQuarantineFromMovementRules(DbDataObject dboHolding, DbDataObject dboDisease, String dateFrom,
			String dateTo, String reason, String quarantineType, SvReader svr, SvWriter svw) throws SvException {
		DbDataObject dboQuarantine = createQuarantineObject(dateFrom, dateTo, reason, quarantineType);
		svw.saveObject(dboQuarantine, false);
		linkObjects(dboHolding, dboQuarantine, Tc.HOLDING_QUARANTINE, "", svr);
		linkObjects(dboDisease, dboQuarantine, Tc.DISEASE_QUARANTINE, "", svr);
	}

	public void invalidateLinksBetweenHoldingAndQuarantineViaMap(SvWriter svw, SvReader svr, DbDataObject qDbo)
			throws SvException {
		// invalidate links for all intersecting holdings
		Reader rdr = new Reader();
		DbDataArray linkArr = rdr.getHoldingQuarantineLinks(svr, qDbo);

		int i = 0;
		for (DbDataObject link : linkArr.getItems()) {
			link.setIs_dirty(true);
			svw.deleteObject(link, false);
			i++;
			if (i == 1000) {
				svw.dbCommit();
				i = 0;
			}
		}
		svw.dbCommit();
	}

	public void changeStatus(JsonObject jData, String nextStatus, SvReader svr) throws SvException {
		SvWorkflow sww = null;
		Reader rdr = new Reader();
		try {
			sww = new SvWorkflow(svr);
			JsonObject obj = null;
			Long objectId = null;
			Long objectType = null;
			DbDataObject dbo = null;
			if (jData.has(Tc.OBJ_ARRAY)) {
				JsonArray jArray = jData.get(Tc.OBJ_ARRAY).getAsJsonArray();
				for (int i = 0; i < jArray.size(); i++) {
					obj = jArray.get(i).getAsJsonObject();
					objectId = obj.get("ORDER.OBJECT_ID").getAsLong();
					objectType = obj.get("ORDER.OBJECT_TYPE").getAsLong();

					dbo = svr.getObjectById(objectId, SvReader.getDbt(objectType), null);
					if (rdr.checkCanChangeStatus(dbo, nextStatus, svr)) {
						ReentrantLock lock = null;
						try {
							lock = SvLock.getLock(String.valueOf(dbo.getObject_id()), false, 0);
							if (lock == null) {
								throw (new SvException("naits.error.objectUsedByOtherSession", svr.getInstanceUser()));
							}
							sww.moveObject(dbo, nextStatus, false);
						} finally {
							if (lock != null && dbo != null) {
								SvLock.releaseLock(String.valueOf(dbo.getObject_id()), lock);
							}
						}
					} else {
						throw (new SvException("naits.info.cantChangeStatus", svr.getInstanceUser()));
					}
				}
				sww.dbCommit();
			} else if (jData.has("objectId") && jData.has("tableName")) {
				objectId = jData.get("objectId").getAsLong();
				String tableName = jData.get("tableName").getAsString();

				dbo = svr.getObjectById(objectId, SvReader.getTypeIdByName(tableName), null);
				if (rdr.checkCanChangeStatus(dbo, nextStatus, svr)) {
					ReentrantLock lock = null;
					try {
						lock = SvLock.getLock(String.valueOf(dbo.getObject_id()), false, 0);
						if (lock == null) {
							throw (new SvException("naits.error.objectUsedByOtherSession", svr.getInstanceUser()));
						}
						sww.moveObject(dbo, nextStatus, true);
					} finally {
						if (lock != null && dbo != null) {
							SvLock.releaseLock(String.valueOf(dbo.getObject_id()), lock);
						}
					}
				} else {
					throw (new SvException("naits.info.cantChangeStatus", svr.getInstanceUser()));
				}
			}
		} finally {
			if (sww != null) {
				sww.release();
			}
		}
	}

	/**
	 * 
	 * Method for notifying user groups
	 * 
	 * @param groupName
	 *            name of group
	 * @param type
	 *            type of notification
	 * @param title
	 *            title of the notification
	 * @param message
	 *            message to be shown in notification
	 * @param sender
	 *            information about the sender
	 * @param eventId
	 *            id of the event (if there is one)
	 * @param svr
	 *            SvReader instance
	 * @param svw
	 *            SvWriter instance
	 * @throws SvException
	 */
	public void notifyUserGroupByGroupName(String groupName, String type, String title, String message, String sender,
			Long eventId, SvReader svr, SvWriter svw) throws SvException {
		SvNotification svn = null;
		DateTime dateNow = new DateTime();
		UserManager um = new UserManager();
		svn = new SvNotification(svr);
		DbDataObject groupObj = um.findUserOrUserGroup(svCONST.OBJECT_TYPE_GROUP, Tc.GROUP_NAME, groupName, svr);
		if (groupObj != null) {
			DbDataObject dboNotification = svn.createNotificationObj(type, title, message, sender, 0L,
					dateNow.toString().substring(0, 10), dateNow.plusDays(5).toString().substring(0, 10));
			svw.saveObject(dboNotification, false);
			svn.createLinkBetweenNotificationAndUserOrUserGroup(dboNotification, groupObj, true);
			svn.dbCommit();
		}
	}

	/**
	 * Method for creating PRE_SLAUGHT_FORM
	 * 
	 * @param clinicalEx
	 * @param decision
	 * @param parentId
	 * @return
	 */
	public DbDataObject createPreMortemForm(String clinicalEx, String decision, Long parentId) {
		DbDataObject preMortem = new DbDataObject();
		preMortem.setObject_type(SvReader.getTypeIdByName(Tc.PRE_SLAUGHT_FORM));
		preMortem.setVal(Tc.CLINICAL_EXAM, clinicalEx);
		preMortem.setVal(Tc.DECISION, decision);
		preMortem.setParent_id(parentId);
		return preMortem;
	}

	/**
	 * Method for creating POST_SLAUGHT_FORM
	 * 
	 * @param clinicalEx
	 * @param decision
	 * @param parentId
	 * @return
	 * @throws SvException
	 */
	public DbDataObject createPostMortem(String slaughterDate, String additionalSampling, String meatDecision,
			String organsDecision, Long parentId) throws SvException {
		DbDataObject postMortem = new DbDataObject();
		postMortem.setObject_type(SvReader.getTypeIdByName(Tc.POST_SLAUGHT_FORM));
		if (slaughterDate == null) {
			setAutoDate(postMortem, Tc.SLAUGHTER_DATE);
		}
		postMortem.setVal(Tc.ADDITIONAL_INFORMATION, additionalSampling);// 2
		postMortem.setVal(Tc.DECISION, meatDecision);// 1
		postMortem.setVal(Tc.DECISION_ORGANS, organsDecision);// 1
		postMortem.setParent_id(parentId);
		return postMortem;
	}

	/**
	 * Method that is used for automatically setting todays date
	 * 
	 * @param dboObjToHandle
	 *            object we want to handle
	 * @param dateField
	 *            Name of field we want to set todays day
	 * @param svw
	 * @throws SvException
	 */
	public void setAutoDate(DbDataObject dboObjToHandle, String dateField, boolean overwriteField) throws SvException {
		Calendar calendar = Calendar.getInstance();
		java.sql.Date dtNow = new java.sql.Date(calendar.getTime().getTime());
		if (dboObjToHandle != null) {
			if (overwriteField)
				dboObjToHandle.setVal(dateField, dtNow);
			else {
				if (dboObjToHandle.getVal(dateField) == null)
					dboObjToHandle.setVal(dateField, dtNow);
			}
		}
	}

	public void setAutoDate(DbDataObject dboObjToHandle, String dateField) throws SvException {
		setAutoDate(dboObjToHandle, dateField, true);
	}

	/**
	 * Method that sets certain Date type field to null
	 * 
	 * @param dboObjToHandle
	 * @param dateField
	 * @param svw
	 * @throws SvException
	 */
	public void setDateFieldToNull(DbDataObject dboObjToHandle, String dateField, SvWriter svw) throws SvException {
		Nullable<DateTime> dt = new Nullable<>();
		if (dboObjToHandle.getVal(dateField) != null) {
			dboObjToHandle.setVal(dateField, dt.getValue());
			svw.saveObject(dboObjToHandle);
		}
	}

	public void setAutoDateWithSave(DbDataObject dboObjToHandle, String dateField, SvWriter svw)
			throws SvException, InterruptedException {
		setAutoDate(dboObjToHandle, dateField);
		svw.saveObject(dboObjToHandle);
		Thread.sleep(2);
	}

	/**
	 * Method that is used for creating AREA_HEALTH objects to all subAreas by
	 * coreArea's AREA_HEALTH
	 * 
	 * @param dboAreaHealth
	 *            AREA_HEALTH object
	 * @param svr
	 *            SvReader instance
	 * @param svw
	 *            SvWriter instance
	 * @throws SvException
	 */
	public void setAreaHealthToSubAreasDependOnCoreAreaAreaHealthObj(DbDataObject dboAreaHealth, SvReader svr,
			SvWriter svw) throws SvException {
		Reader rdr = new Reader();
		ValidationChecks vc = null;
		DbDataArray subAreas = null;
		DbDataObject subAreaHealth = null;
		DbDataObject dboArea = svr.getObjectById(dboAreaHealth.getParent_id(), SvReader.getTypeIdByName(Tc.AREA), null);
		if (dboArea != null && dboAreaHealth.getVal(Tc.DISEASE_ID) != null
				&& dboAreaHealth.getVal(Tc.AREA_STATUS) != null) {
			subAreas = rdr.getAreasByCoreArea(dboArea, svr);
			vc = new ValidationChecks();
			for (DbDataObject subArea : subAreas.getItems()) {
				subAreaHealth = new DbDataObject();
				subAreaHealth.setObject_type(dboAreaHealth.getObject_type());
				subAreaHealth.setVal(Tc.DISEASE_ID, dboAreaHealth.getVal(Tc.DISEASE_ID).toString());
				subAreaHealth.setVal(Tc.AREA_STATUS, dboAreaHealth.getVal(Tc.AREA_STATUS).toString());
				subAreaHealth.setVal(Tc.REASON,
						"Auto-Generated. Core Area: " + dboArea.getVal(Tc.AREA_NAME).toString());
				subAreaHealth.setVal(Tc.AUTO_GENERATED, true);
				subAreaHealth.setParent_id(subArea.getObject_id());
				if (!vc.checkIfAreaHasDuplicateDisease(subAreaHealth, svr)) {
					svw.saveObject(subAreaHealth, false);
				}
			}
		}
	}

	public String setHealthStatusToLabSample(Long parentId, SvReader svr, SvWriter svw) throws SvException {
		boolean resultTestCheck = true;
		String result = "naits.error.labSampleAlreadyHaveHealthStatus";
		DbDataObject dboLabSample = svr.getObjectById(parentId, SvReader.getTypeIdByName(Tc.LAB_SAMPLE), null);
		if (dboLabSample != null && (dboLabSample.getVal(Tc.TEST_RESULT_STATUS) == null
				|| dboLabSample.getVal(Tc.TEST_RESULT_STATUS).toString().equals(""))) {
			ReentrantLock lock = null;
			try {
				lock = SvLock.getLock(String.valueOf(dboLabSample.getObject_id()), false, 0);
				if (lock == null) {
					throw (new SvException("naits.error.objectUsedByOtherSession", svr.getInstanceUser()));
				}
				DbDataArray testResults = svr.getObjectsByParentId(dboLabSample.getObject_id(),
						SvReader.getTypeIdByName(Tc.LAB_TEST_RESULT), new DateTime(), 0, 0);
				if (!testResults.getItems().isEmpty()) {
					for (DbDataObject testResObj : testResults.getItems()) {
						if (testResObj != null && (testResObj.getVal(Tc.TEST_RESULT) == null
								|| testResObj.getVal(Tc.TEST_RESULT).equals(""))) {
							result = "naits.error.allTestResultsMustHaveHealthStatus";
							resultTestCheck = false;
							break;
						}
					}
					if (resultTestCheck) {
						for (DbDataObject testResObj : testResults.getItems()) {
							if (testResObj.getVal(Tc.TEST_RESULT) != null) {
								if (testResObj.getVal(Tc.TEST_RESULT).toString().equals("0")) {
									result = "naits.success.changedHealthStatusToPositive";
									dboLabSample.setVal(Tc.TEST_RESULT_STATUS, "0");
									break;
								} else if (testResObj.getVal(Tc.TEST_RESULT).toString().equals("2")) {
									result = "naits.success.changedHealthStatusToPositive";
									dboLabSample.setVal(Tc.TEST_RESULT_STATUS, "2");
									break;
								}
							}
						}
						if (!result.equals("naits.success.changedHealthStatusToPositive")) {
							result = "naits.success.changedHealthStatusToNegative";
							dboLabSample.setVal(Tc.TEST_RESULT_STATUS, "1");
						}
						dboLabSample.setStatus(Tc.PROCESSED);
						svw.saveObject(dboLabSample, false);
						svw.dbCommit();
					}
				}
			} finally {
				if (lock != null && dboLabSample != null) {
					SvLock.releaseLock(String.valueOf(dboLabSample.getObject_id()), lock);
				}
			}
		}
		return result;
	}

	public DbDataObject createEarTagReplcObject(String newEarTag, String replacementDate, String reason, String note,
			Long parentId, SvReader svr) throws SvException {
		ValidationChecks vc = new ValidationChecks();
		DbDataObject parentObj = svr.getObjectById(parentId, SvReader.getTypeIdByName(Tc.ANIMAL), null);
		DbDataObject earTagReplcObject = new DbDataObject();
		earTagReplcObject.setObject_type(SvReader.getTypeIdByName(Tc.EAR_TAG_REPLC));
		earTagReplcObject.setVal(Tc.NEW_EAR_TAG, newEarTag);
		if (parentObj != null && parentObj.getVal(Tc.ANIMAL_ID) != null) {
			earTagReplcObject.setVal(Tc.OLD_EAR_TAG, parentObj.getVal(Tc.ANIMAL_ID).toString());
		}
		earTagReplcObject.setVal(Tc.REPLACEMENT_DATE, replacementDate);
		if (vc.checkIfDateIsInFuture(earTagReplcObject, Tc.REPLACEMENT_DATE)) {
			throw (new SvException("naits.error.replacementDateCannotBeInTheFuture", svr.getInstanceUser()));
		}
		if (reason != null && !reason.trim().equals("") && !reason.trim().equals("null")) {
			if (reason.equals(Tc.WRONG_ENTRY) && vc.checkIfWrongEnteredEarTagExists(parentObj, svr)) {
				throw (new SvException("naits.error.cannotEnterWrongEntryAsReasonTwice", svr.getInstanceUser()));
			}
			earTagReplcObject.setVal(Tc.REASON, reason);
		}
		if (note != null && !note.equals("null")) {
			earTagReplcObject.setVal(Tc.NOTE, note);
		}
		earTagReplcObject.setParent_id(parentId);
		return earTagReplcObject;
	}

	public String updateAnimalEarTagByEarTagReplacement(DbDataObject earTagReplcObj, SvReader svr, SvWriter svw)
			throws SvException {
		String result = "";
		DbDataObject parentObj = null;// ANIMAL
		if (earTagReplcObj != null && earTagReplcObj.getVal(Tc.NEW_EAR_TAG) != null) {
			parentObj = svr.getObjectById(earTagReplcObj.getParent_id(), SvReader.getTypeIdByName(Tc.ANIMAL), null);
			if (parentObj != null) {
				if (parentObj.getVal(Tc.ANIMAL_ID) != null) {
					parentObj.setVal(Tc.ANIMAL_ID, earTagReplcObj.getVal(Tc.NEW_EAR_TAG).toString());
					svw.saveObject(parentObj);
					result = "naits.success.successfullyUpdatedAnimalEarTagId";
				}
			}
		}
		return result;
	}

	public void autoDeleteSubAreasHealthIfCoreAreaHealthIsDeleted(DbDataObject dboAreaHealth, SvReader svr,
			SvWriter svw) throws SvException {
		Reader rdr = new Reader();
		DbDataArray subAreas = null;
		DbDataObject subAreaHealth = null;
		DbDataArray subAreaHealthArr = null;
		DbDataObject dboArea = svr.getObjectById(dboAreaHealth.getParent_id(), SvReader.getTypeIdByName(Tc.AREA), null);
		if (dboArea != null && dboAreaHealth.getVal(Tc.DISEASE_ID) != null
				&& dboAreaHealth.getVal(Tc.AREA_STATUS) != null) {
			subAreas = rdr.getAreasByCoreArea(dboArea, svr);
			for (DbDataObject subArea : subAreas.getItems()) {
				subAreaHealthArr = rdr.getAutoGeneratedAreaHealhObjects(dboAreaHealth, subArea, true, svr);
				if (subAreaHealthArr != null && subAreaHealthArr.size() > 0) {
					subAreaHealth = subAreaHealthArr.get(0);
					svw.deleteObject(subAreaHealth, false);
				}
			}
		}
	}

	public void setVaccBookDependOnVaccEvent(DbDataObject vaccBook, SvReader svr, SvWriter svw) throws SvException {
		DbDataObject vaccEvent = null;
		Reader rdr = null;
		if (vaccBook.getVal(Tc.CAMPAIGN_NAME) != null) {
			rdr = new Reader();
			vaccEvent = rdr.getVaccEventByName(vaccBook.getVal(Tc.CAMPAIGN_NAME).toString(), svr);
			if (vaccEvent != null) {
				if (vaccEvent.getVal(Tc.ACTIVITY_TYPE) != null) {
					vaccBook.setVal(Tc.ACTIVITY_TYPE, vaccEvent.getVal(Tc.ACTIVITY_TYPE).toString());
				}
				if (vaccEvent.getVal(Tc.ACTIVITY_SUBTYPE) != null) {
					vaccBook.setVal(Tc.ACTIVITY_SUBTYPE, vaccEvent.getVal(Tc.ACTIVITY_SUBTYPE).toString());
				}
				if (vaccEvent.getVal(Tc.IMMUNIZATION_PERIOD) != null) {
					vaccBook.setVal(Tc.IMMUNIZATION_PERIOD,
							Integer.valueOf(vaccEvent.getVal(Tc.IMMUNIZATION_PERIOD).toString()));
				}
				if (vaccEvent.getVal(Tc.PROHIBITION_PERIOD) != null) {
					vaccBook.setVal(Tc.PROHIBITION_PERIOD,
							Integer.valueOf(vaccEvent.getVal(Tc.PROHIBITION_PERIOD).toString()));
				}
			}
		}
	}

	/**
	 * Method that generates N LAB_TEST RESULTS depend on N selected diseases in
	 * DISEASE_TEST field in LAB_SAMPLE.
	 * 
	 * @param dbo
	 * @param svw
	 * @throws SvException
	 */
	public void createNLabTestResultDependOnMultiSelectedDiseasesInLabSample(DbDataObject dbo, SvWriter svw)
			throws SvException {
		Reader rdr = new Reader();
		DbDataObject labTestResult = null;
		String testType = dbo.getVal(Tc.SAMPLE_TEST_TYPE) != null ? dbo.getVal(Tc.SAMPLE_TEST_TYPE).toString() : null;
		List<String> listOfDiseases = rdr.getMultiSelectFieldValueAsList(dbo, Tc.DISEASE_TEST);
		for (int i = 0; i < listOfDiseases.size(); i++) {
			labTestResult = createLabTestResult(dbo.getObject_id(), listOfDiseases.get(i).trim(), testType);
			svw.saveObject(labTestResult);
		}
	}

	/**
	 * 
	 * @param parentId
	 * @return
	 * @throws SvException
	 */
	public DbDataObject createLabTestResult(Long parentId, String disease, String sampleTestType) throws SvException {
		DbDataObject labTestResult = new DbDataObject();
		labTestResult.setObject_type(SvReader.getTypeIdByName(Tc.LAB_TEST_RESULT));
		labTestResult.setVal(Tc.SAMPLE_DISEASE, disease);
		labTestResult.setVal(Tc.TEST_TYPE, sampleTestType);
		labTestResult.setParent_id(parentId);
		return labTestResult;
	}

	/**
	 * Method for creation of movement document
	 * 
	 * @param holdingObjId
	 * @param svr
	 * @return DbDataObject
	 * @throws SvException
	 */
	public DbDataObject createMovementDocument(Long holdingObjId, SvReader svr) throws SvException {
		SvSequence svs = null;
		DbDataObject movementDoc = null;
		try {
			svs = new SvSequence(svr.getSessionId());
			movementDoc = new DbDataObject();
			movementDoc.setObject_type(SvReader.getTypeIdByName(Tc.MOVEMENT_DOC));
			movementDoc.setStatus(Tc.DRAFT);
			setAutoDate(movementDoc, Tc.DT_REGISTRATION);
			movementDoc.setParent_id(holdingObjId);
			String pic = "";
			if (holdingObjId != null) {
				DbDataObject dboHolding = svr.getObjectById(holdingObjId, SvReader.getTypeIdByName(Tc.HOLDING), null);
				if (dboHolding.getVal(Tc.PIC) != null) {
					pic = dboHolding.getVal(Tc.PIC).toString();
				}
			}
			if (!pic.equals("")) {
				Long seqId = svs.getSeqNextVal("MD-" + pic, false);
				String formattedSeq = String.format("%03d", Integer.valueOf(seqId.toString()));
				String generatedMovementId = "MD-" + pic + "-" + formattedSeq;
				movementDoc.setVal(Tc.MOVEMENT_DOC_ID, generatedMovementId);
				svs.dbCommit();
			}
			movementDoc.setVal(Tc.RESPONSIBLE_USER, svr.getInstanceUser().getVal(Tc.USER_NAME));
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
		} finally {
			if (svs != null)
				svs.release();
		}
		return movementDoc;
	}

	/**
	 * 
	 * @param petId
	 * @param svr
	 * @return
	 */
	public String generateRequestId(String petId, SvReader svr) {
		SvSequence svs = null;
		String passportRequestSeq = "";
		String generateRequestId = "";
		try {
			svs = new SvSequence(svr.getSessionId());
			Long seqId = svs.getSeqNextVal("RQ" + petId, false);
			Thread.sleep(2);
			passportRequestSeq = String.format("%03d", Integer.valueOf(seqId.toString()));
			generateRequestId = "RQ-" + petId + "-" + passportRequestSeq;
			svs.dbCommit();
		} catch (SvException | InterruptedException e) {
			log4j.error(e);
		} finally {
			if (svs != null) {
				svs.release();
			}
		}
		return generateRequestId;
	}

	/**
	 * 
	 * @param parentId
	 * @param animalOrFlockMovementObject
	 * @param blockReason
	 * @param recommendation
	 * @param svr
	 * @return
	 * @throws SvException
	 */
	public DbDataObject createMovementDocBlock(Long parentId, DbDataObject animalOrFlockMovementObject,
			String blockReason, String recommendation, SvReader svr) throws SvException {
		DbDataObject movementDocBlock = new DbDataObject();
		movementDocBlock.setObject_type(SvReader.getTypeIdByName(Tc.MOVEMENT_DOC_BLOCK));
		movementDocBlock.setParent_id(parentId);
		movementDocBlock.setStatus(Tc.UNRESOLVED);
		movementDocBlock.setVal(Tc.MOVE_OBJ_ID, animalOrFlockMovementObject.getObject_id());
		if (animalOrFlockMovementObject.getObject_type().equals(SvReader.getTypeIdByName(Tc.ANIMAL_MOVEMENT))) {
			movementDocBlock.setVal(Tc.ANIMAL_FLOCK_ID, animalOrFlockMovementObject.getVal(Tc.ANIMAL_EAR_TAG));
		} else {
			movementDocBlock.setVal(Tc.ANIMAL_FLOCK_ID, animalOrFlockMovementObject.getVal(Tc.FLOCK_ID));
		}
		movementDocBlock.setVal(Tc.BLOCK_REASON, blockReason);
		movementDocBlock.setVal(Tc.RECOMMENDATION, recommendation);
		return movementDocBlock;
	}

	/**
	 * 
	 * @param animalEarTag
	 * @param holdingPic
	 * @param holdingResp
	 * @param sampleOrigin
	 * @param actionDate
	 * @param parentId
	 * @return
	 */

	public DbDataObject createLabSampleObject(String animalEarTag, String holdingPic, String holdingResp,
			String sampleOrigin, String actionDate, Long parentId) {
		String pattern = Tc.DATE_PATTERN;
		DateTime convertedActionDate = DateTime.parse(actionDate, DateTimeFormat.forPattern(pattern));
		DbDataObject labSampleObj = new DbDataObject();
		labSampleObj.setObject_type(SvReader.getTypeIdByName(Tc.LAB_SAMPLE));
		labSampleObj.setStatus(Tc.COLLECTED);
		labSampleObj.setVal(Tc.ANIMAL_EAR_TAG, animalEarTag);
		labSampleObj.setVal(Tc.HOLDING_PIC, holdingPic);
		labSampleObj.setVal(Tc.HOLDING_RESP, holdingResp);
		labSampleObj.setVal(Tc.SAMPLE_ORIGIN, sampleOrigin);
		labSampleObj.setVal(Tc.DATE_OF_COLLECTION, convertedActionDate);
		labSampleObj.setParent_id(parentId);
		return labSampleObj;
	}

	public void createLabSampleBasedOnAnimalHealthBook(DbDataObject dboVaccinationBook, Long animalObjectId,
			SvReader svr, SvWriter svw) throws SvException {
		if (dboVaccinationBook != null && dboVaccinationBook.getVal(Tc.VACC_DATE) != null
				&& dboVaccinationBook.getVal(Tc.ACTIVITY_TYPE) != null
				&& dboVaccinationBook.getVal(Tc.ACTIVITY_TYPE).toString().equals("2")
				&& (dboVaccinationBook.getVal(Tc.SAMPLE_ID) == null
						|| dboVaccinationBook.getVal(Tc.SAMPLE_ID).equals(""))) {
			createLabSampleBasedOnAnimalHealthBook(animalObjectId, dboVaccinationBook.getVal(Tc.VACC_DATE).toString(),
					dboVaccinationBook.getVal(Tc.CAMPAIGN_NAME), svr, svw);

		}
	}

	/**
	 * Method for generating LAB_SAMPLE if new VACCINATION_BOOK is created.
	 * 
	 * @param animalObjId
	 * @param actionDate
	 * @param svr
	 * @param svw
	 * @throws SvException
	 */
	public void createLabSampleBasedOnAnimalHealthBook(Long animalObjId, String actionDate, Object campaignName,
			SvReader svr, SvWriter svw) throws SvException {
		Reader rdr = new Reader();
		String animalId = "";
		String holdingPic = "";
		String keeperFullName = "";
		String dboCampaignName = null;
		DbDataObject objLinkedAnimal = svr.getObjectById(animalObjId, SvReader.getTypeIdByName(Tc.ANIMAL), null);
		DbDataObject objAnimalParentHolding = null;
		DbDataObject objHoldingKeeper = null;
		DbDataObject labSampleObj = null;
		DbDataObject dboVaccinationEvent = null;
		if (objLinkedAnimal != null) {
			animalId = objLinkedAnimal.getVal(Tc.ANIMAL_ID).toString();
			objAnimalParentHolding = svr.getObjectById(objLinkedAnimal.getParent_id(),
					SvReader.getTypeIdByName(Tc.HOLDING), null);
			objHoldingKeeper = rdr.getHoldingOwner(objAnimalParentHolding.getObject_id(), svr);
			if (objAnimalParentHolding != null && objAnimalParentHolding.getVal(Tc.PIC) != null) {
				holdingPic = objAnimalParentHolding.getVal(Tc.PIC).toString();
			}
			if (objHoldingKeeper != null && objHoldingKeeper.getVal(Tc.FULL_NAME) != null) {
				keeperFullName = objHoldingKeeper.getVal(Tc.FULL_NAME).toString();
			}
			if (campaignName != null) {
				dboCampaignName = campaignName.toString();
			}
		}
		labSampleObj = createLabSampleObject(animalId, holdingPic, keeperFullName, "1", actionDate, animalObjId);
		if (dboCampaignName != null) {
			dboVaccinationEvent = rdr.getVaccEventByName(dboCampaignName, svr);
			if (dboVaccinationEvent.getVal(Tc.DISEASE) != null && !dboVaccinationEvent.getVal(Tc.DISEASE).equals("")) {
				labSampleObj.setVal(Tc.DISEASE_TEST, dboVaccinationEvent.getVal(Tc.DISEASE).toString());
			}
			if (dboVaccinationEvent.getVal(Tc.ACTIVITY_SUBTYPE) != null
					&& !dboVaccinationEvent.getVal(Tc.ACTIVITY_SUBTYPE).equals("")) {
				labSampleObj.setVal(Tc.SAMPLE_TYPE, dboVaccinationEvent.getVal(Tc.ACTIVITY_SUBTYPE).toString());
			}
			if (dboVaccinationEvent.getVal(Tc.TEST_TYPE_OBJ_ID) != null) {
				DbDataObject dboLabTestType = svr.getObjectById((Long) dboVaccinationEvent.getVal(Tc.TEST_TYPE_OBJ_ID),
						SvReader.getTypeIdByName(Tc.LAB_TEST_TYPE), null);
				if (dboLabTestType != null && dboLabTestType.getVal(Tc.TEST_TYPE) != null) {
					labSampleObj.setVal(Tc.SAMPLE_TEST_TYPE, dboLabTestType.getVal(Tc.TEST_TYPE).toString());
				}
			}
		}
		labSampleObj.setVal("CHECK_COLUMN", "1");
		svw.saveObject(labSampleObj);
	}

	/**
	 * Method for assigning laboratory to certain Laboratory Sample
	 * 
	 * @param labSampleObj
	 * @param laboratoryName
	 * @param rdr
	 * @param wr
	 * @param svr
	 * @param svw
	 * @throws SvException
	 */
	public String createLinkBetweenLaboratoryAndLabSampleByLabName(DbDataObject labSampleObj, String laboratoryName,
			Reader rdr, SvReader svr, SvWriter svw) throws SvException {
		// link assigned laboratory
		String result = "";
		String flagString = ""; // flag for label
		DbDataObject laboratoryObject = null;
		if (labSampleObj.getStatus().equals(Tc.COLLECTED)) {
			labSampleObj.setStatus(Tc.QUEUED);
		}
		if (!labSampleObj.getParent_id().equals(0L) && laboratoryName != null && !laboratoryName.equals("")) {
			DbDataObject linkLabLabSample = null;
			// assigned lab
			laboratoryObject = rdr.searchForObject(SvReader.getTypeIdByName(Tc.LABORATORY), Tc.LAB_NAME, laboratoryName,
					svr);
			if (laboratoryObject != null) {
				// if lab changed, invalidate old link.
				if (labSampleObj.getVal(Tc.LABORATORY_NAME) != null) {
					DbDataObject oldAssignedLaboratory = rdr.searchForObject(SvReader.getTypeIdByName(Tc.LABORATORY),
							Tc.LAB_NAME, labSampleObj.getVal(Tc.LABORATORY_NAME).toString(), svr);
					linkLabLabSample = rdr.getLinkObjectBetweenTwoLinkedObjects(oldAssignedLaboratory, labSampleObj,
							Tc.LABORATORY_SAMPLE, svr);
					if (linkLabLabSample != null
							&& !laboratoryName.equals(labSampleObj.getVal(Tc.LABORATORY_NAME).toString())) {
						svw.deleteObject(linkLabLabSample, false);
						flagString = "Change";
						// successfully changed laboratory
					}
				}
				if (labSampleObj.getVal(Tc.LABORATORY_NAME) == null
						|| !laboratoryName.equals(labSampleObj.getVal(Tc.LABORATORY_NAME).toString())) {
					// first save the changed object
					labSampleObj.setVal(Tc.LABORATORY_NAME, laboratoryName);
					svw.saveObject(labSampleObj, false);
					linkObjects(laboratoryObject, labSampleObj, Tc.LABORATORY_SAMPLE, "", svw);
					result = "naits.success.successfullyAssignedLaboratory" + flagString;
				}
				svw.dbCommit();
			}
		}
		return result;
	}

	public String setHealthStatusOfTestResults(Long labSampleObjId, String healthStatus, SvReader svr, SvWriter svw)
			throws SvException {
		String result = "naits.success.setHealthStatusOfTestResults";
		DbDataObject labSampleObj = svr.getObjectById(labSampleObjId, SvReader.getTypeIdByName(Tc.LAB_SAMPLE),
				new DateTime());
		DbDataArray testResults = svr.getObjectsByParentId(labSampleObjId, SvReader.getTypeIdByName(Tc.LAB_TEST_RESULT),
				null, 0, 0);
		if (labSampleObj.getStatus().equals(Tc.RECEIVED)) {
			if (!testResults.getItems().isEmpty()) {
				for (DbDataObject testResObj : testResults.getItems()) {
					testResObj.setVal(Tc.TEST_RESULT, healthStatus);
					svw.saveObject(testResObj, false);
				}
				labSampleObj.setVal(Tc.TEST_RESULT_STATUS, healthStatus);
				svw.saveObject(labSampleObj, false);
				changeStatus(labSampleObj, Tc.PROCESSED, svw);
				setHealthStatusToLabSample(labSampleObjId, svr, svw);
			} else {
				result = "naits.error.setHealthStatusOfTestResults";
			}
		} else {
			result = "naits.error.labSampleMustBeReceived";
		}
		return result;
	}

	public void autoAsignSessionUserToObjectField(DbDataObject dbo, String fieldName, Boolean replaceIfExist,
			SvReader svr) throws SvException {
		DbDataObject dboUser = SvReader.getUserBySession(svr.getSessionId());
		if ((replaceIfExist.equals(true)) || (replaceIfExist.equals(false) && dbo.getVal(fieldName) == null)) {
			dbo.setVal(fieldName, dboUser.getVal(Tc.USER_NAME).toString());
		}
	}

	public void autoSetTreatmentTypeInVaccinationBookDependOnUserGroup(DbDataObject dbo, SvReader svr)
			throws SvException {
		Boolean isState = false;
		if (dbo.getVal(Tc.TRETM_TYPE) == null) {
			DbDataObject dboUser = SvReader.getUserBySession(svr.getSessionId());
			DbDataObject dboLinkBetweenUserAndUserGroup = SvReader.getLinkType(Tc.USER_GROUP, dboUser.getObject_type(),
					svCONST.OBJECT_TYPE_GROUP);
			DbDataArray arrGroups = svr.getObjectsByLinkedId(dboUser.getObject_id(), dboUser.getObject_type(),
					dboLinkBetweenUserAndUserGroup, svCONST.OBJECT_TYPE_GROUP, false, null, 0, 0);
			if (arrGroups != null && arrGroups.size() > 0) {
				for (DbDataObject dboGroup : arrGroups.getItems()) {
					if (dboGroup.getVal(Tc.GROUP_NAME) != null
							&& dboGroup.getVal(Tc.GROUP_NAME).toString().equals(Tc.PRIVATE_VETERINARIANS)) {
						isState = true;
						dbo.setVal(Tc.TRETM_TYPE, Tc.NON_STATE);
						break;
					}
				}
			}
			if (!isState) {
				dboLinkBetweenUserAndUserGroup = SvReader.getLinkType(Tc.USER_DEFAULT_GROUP, dboUser.getObject_type(),
						svCONST.OBJECT_TYPE_GROUP);
				arrGroups = svr.getObjectsByLinkedId(dboUser.getObject_id(), dboUser.getObject_type(),
						dboLinkBetweenUserAndUserGroup, svCONST.OBJECT_TYPE_GROUP, false, null, 0, 0);
				if (arrGroups != null && arrGroups.size() > 0) {
					for (DbDataObject dboGroup : arrGroups.getItems()) {
						if (dboGroup.getVal(Tc.GROUP_NAME) != null
								&& dboGroup.getVal(Tc.GROUP_NAME).toString().equals(Tc.PRIVATE_VETERINARIANS)) {
							isState = true;
							dbo.setVal(Tc.TRETM_TYPE, Tc.NON_STATE);
							break;
						}
					}
				}
			}
			if (!isState) {
				dbo.setVal(Tc.TRETM_TYPE, Tc.STATE);
			}
		}
	}

	/**
	 * 
	 * @param populationId
	 * @param filterId
	 * @param filterValue
	 * @return
	 * @throws SvException
	 */
	public DbDataObject createPopulationFilter(Long populationId, Long filterId, String filterValue, String filterNote,
			SvWriter svw) throws SvException {
		SvReader svr = new SvReader(svw);
		DbDataObject dboCriteriaType = svr.getObjectById(filterId, SvReader.getTypeIdByName(Tc.CRITERIA_TYPE), null);
		String criteriaTypeLabelCode = dboCriteriaType.getVal(Tc.LABEL_CODE) != null
				? dboCriteriaType.getVal(Tc.LABEL_CODE).toString() : "";
		DbDataObject dboCriteria = new DbDataObject();
		dboCriteria.setObject_type(SvReader.getTypeIdByName(Tc.CRITERIA));
		dboCriteria.setParent_id(populationId);
		dboCriteria.setVal(Tc.CRITERIA_TYPE_ID, filterId);
		dboCriteria.setVal(Tc.VALUE, filterValue);
		dboCriteria.setVal(Tc.NOTE, filterNote);
		// auto fill the criteria name acording criteria type label code
		// this means what the field name must be marked as SVLABEL in extended
		// params configuration
		dboCriteria.setVal(Tc.NAME, criteriaTypeLabelCode);
		if (svr != null)
			svr.release();
		return dboCriteria;
	}

	public DbDataObject createRangeObject(String tagType, Long startTagId, Long endTagId, Long quantity, Long parentId)
			throws SvException {
		DbDataObject rangeObj = new DbDataObject();
		rangeObj.setObject_type(SvReader.getTypeIdByName(Tc.RANGE));
		rangeObj.setVal(Tc.TAG_TYPE, tagType);
		rangeObj.setVal(Tc.START_TAG_ID, startTagId);
		rangeObj.setVal(Tc.END_TAG_ID, endTagId);
		rangeObj.setVal(Tc.QUANTITY, quantity);
		rangeObj.setParent_id(parentId);
		return rangeObj;
	}

	public DbDataObject createTransferObject(String tagType, Long startTagId, Long endTagId, Long quantity,
			String transferType, String departurePlace, String arrivalPlace, String issuedByPerson,
			String receivedByPerson, String reason, String originObjId, String destinationObjectId) throws SvException {
		DbDataObject transferObj = new DbDataObject();
		transferObj.setObject_type(SvReader.getTypeIdByName(Tc.TRANSFER));
		transferObj.setStatus(Tc.DRAFT);
		transferObj.setVal(Tc.TAG_TYPE, tagType);
		transferObj.setVal(Tc.START_TAG_ID, startTagId);
		transferObj.setVal(Tc.END_TAG_ID, endTagId);
		transferObj.setVal(Tc.QUANTITY, quantity);
		transferObj.setVal(Tc.TRANSFER_TYPE, transferType);
		transferObj.setVal(Tc.SUBJECT_FROM, departurePlace);
		transferObj.setVal(Tc.SUBJECT_TO, arrivalPlace);
		transferObj.setVal(Tc.RETURNED_BY, issuedByPerson);
		transferObj.setVal(Tc.RECEIVED_BY, receivedByPerson);
		transferObj.setVal(Tc.REASON, reason);
		transferObj.setVal(Tc.ORIGIN_OBJ_ID, originObjId);
		transferObj.setVal(Tc.DESTINATION_OBJ_ID, destinationObjectId);
		return transferObj;
	}

	public DbDataObject createReverseTransferObject(DbDataObject dboInitTransfer, Long rangeFrom, Long rangeTo,
			Writer wr, SvReader svr) throws SvException {
		DbDataObject dboReverseTransfer = null;
		dboReverseTransfer = createTransferObject(dboInitTransfer.getVal(Tc.TAG_TYPE).toString(), null, null, null,
				null, dboInitTransfer.getVal(Tc.SUBJECT_FROM).toString(),
				dboInitTransfer.getVal(Tc.SUBJECT_TO).toString(), dboInitTransfer.getVal(Tc.RETURNED_BY).toString(),
				dboInitTransfer.getVal(Tc.RECEIVED_BY).toString(), null, null,
				dboInitTransfer.getVal(Tc.DESTINATION_OBJ_ID).toString());
		dboReverseTransfer.setObject_type(dboInitTransfer.getObject_type());
		dboReverseTransfer.setVal(Tc.TRANSFER_ID,
				wr.generateReverseTransferId(dboInitTransfer.getVal(Tc.TRANSFER_ID).toString(), svr));
		dboReverseTransfer.setVal(Tc.START_TAG_ID, rangeFrom);
		dboReverseTransfer.setVal(Tc.END_TAG_ID, rangeTo);
		dboReverseTransfer.setVal(Tc.ORIGIN_OBJ_ID, dboInitTransfer.getVal(Tc.DESTINATION_OBJ_ID).toString());
		dboReverseTransfer.setVal(Tc.DESTINATION_OBJ_ID, dboInitTransfer.getParent_id().toString());
		dboReverseTransfer.setParent_id(Long.valueOf(dboInitTransfer.getVal(Tc.DESTINATION_OBJ_ID).toString()));
		dboReverseTransfer.setVal(Tc.TRANSFER_TYPE, Tc.REVERSE);
		return dboReverseTransfer;
	}

	/**
	 * Method for canceling/expiring Export certificate
	 * 
	 * @param exportCertObjId
	 *            Object_Id of the Export certificate
	 * @param rdr
	 *            Reader instance
	 * @param sessionId
	 *            Session ID
	 * @return
	 * @throws SvException
	 */
	public String cancelExportCertificate(Long exportCertObjId, Reader rdr, String sessionId) throws SvException {
		SvReader svr = null;
		SvWorkflow sww = null;
		String result = "naits.error.cancelExportCert";
		DbDataObject dboExportCert = null;
		try {
			svr = new SvReader(sessionId);
			sww = new SvWorkflow(svr);

			sww.setAutoCommit(false);
			dboExportCert = svr.getObjectById(exportCertObjId, SvReader.getTypeIdByName(Tc.EXPORT_CERT), null);
			if (dboExportCert != null && dboExportCert.getStatus().equals(Tc.VALID)) {
				ReentrantLock lock = null;
				try {
					lock = SvLock.getLock(String.valueOf(dboExportCert.getObject_id()), false, 0);
					if (lock == null) {
						throw (new SvException("naits.error.objectUsedByOtherSession", svr.getInstanceUser()));
					}
					DbDataObject dboLinkBetweenAnimalAndExportCert = SvReader.getLinkType(Tc.ANIMAL_EXPORT_CERT,
							SvReader.getTypeIdByName(Tc.ANIMAL), SvReader.getTypeIdByName(Tc.EXPORT_CERT));
					DbDataArray linkedAnimalsArr = svr.getObjectsByLinkedId(exportCertObjId,
							dboExportCert.getObject_type(), dboLinkBetweenAnimalAndExportCert,
							SvReader.getTypeIdByName(Tc.ANIMAL), true, null, 0, 0);
					if (linkedAnimalsArr != null && linkedAnimalsArr.size() > 0) {
						for (DbDataObject dboAnimal : linkedAnimalsArr.getItems()) {
							DbDataObject dboLink = rdr.getLinkObject(dboAnimal.getObject_id(), exportCertObjId,
									dboLinkBetweenAnimalAndExportCert.getObject_id(), svr);
							if (dboLink != null) {
								invalidateLink(dboLink, false, svr);
							}
							sww.moveObject(dboAnimal, Tc.VALID, false);
						}
					}
					sww.moveObject(dboExportCert, Tc.CANCELED, false);
					sww.dbCommit();
					result = "naits.success.cancelExportCert";
				} finally {
					if (lock != null) {
						SvLock.releaseLock(String.valueOf(dboExportCert.getObject_id()), lock);
					}
				}
			} else {
				result = "naits.error.onlyValidExportCertCanBeCanceled";
			}
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage(), e);
		} finally {
			if (sww != null) {
				sww.release();
			}
		}
		return result;
	}

	public DbDataObject createFlockObject(String animalType, String earTagColor, Long males, Long females, Long total,
			Long adults, String registrationDate, String usedTagQuantity, Long parent_id) {
		DbDataObject flockObj = new DbDataObject();
		flockObj.setObject_type(SvReader.getTypeIdByName(Tc.FLOCK));
		flockObj.setVal(Tc.ANIMAL_TYPE, animalType);
		flockObj.setVal(Tc.EAR_TAG_COLOR, earTagColor);
		flockObj.setVal(Tc.MALES, males);
		flockObj.setVal(Tc.FEMALES, females);
		flockObj.setVal(Tc.TOTAL, total);
		flockObj.setVal(Tc.ADULTS, adults);
		flockObj.setVal(Tc.REGISTRATION_DATE, registrationDate);
		flockObj.setVal(Tc.USED_TAG_QUANTITY, usedTagQuantity);
		flockObj.setParent_id(parent_id);
		return flockObj;
	}

	/**
	 * Set disease to Pre Or Post slaughter object.
	 * 
	 * @param disease
	 * @param preOrPostSlaughterObj
	 * @param svr
	 * @throws SvException
	 */
	public void setDiseaseInPreOrPostSlaughterObj(String disease, DbDataObject preOrPostSlaughterObj, SvWriter svw)
			throws SvException {
		String fieldName = "";
		if (preOrPostSlaughterObj != null) {
			if (preOrPostSlaughterObj.getObject_type().equals(SvReader.getTypeIdByName(Tc.PRE_SLAUGHT_FORM))) {
				fieldName = Tc.DISEASE;
			} else if (preOrPostSlaughterObj.getObject_type().equals(SvReader.getTypeIdByName(Tc.POST_SLAUGHT_FORM))) {
				fieldName = Tc.DISIESE_FINDING;
			}
			if (!fieldName.equals("") && (preOrPostSlaughterObj.getVal(fieldName) == null
					|| preOrPostSlaughterObj.getVal(fieldName).toString().equals(""))) {
				preOrPostSlaughterObj.setVal(fieldName, disease);
			}
		}
	}

	/**
	 * This method creates params for certain Link Object, in this case for
	 * HOLDING_HERDER link type.
	 * 
	 * @param dtFrom
	 *            Active from
	 * @param dtTo
	 *            Active to
	 * @param dboLink
	 *            Link object
	 * @param svr
	 * @throws SvException
	 */
	public void createActivityPeriodPerHerder(String dateFrom, String dateTo, DbDataObject dboLink, SvParameter svp)
			throws SvException {
		DateTime dtFrom = new DateTime(dateFrom);
		DateTime dtTo = new DateTime(dateTo);
		svp.setParamDateTime(dboLink, "param.activity_from", dtFrom, false);
		svp.setParamDateTime(dboLink, "param.activity_to", dtTo, false);
	}

	public void trigerChangeOfExpCertStatus(SvReader svr, SvWriter svw) throws SvException {
		Reader rdr = new Reader();
		DbDataArray expCertForInvalidate = rdr.getExportCertificatesOfExpiredQuarantines(svr);
		if (expCertForInvalidate.size() > 0) {
			for (DbDataObject tempExpCert : expCertForInvalidate.getItems()) {
				DbDataArray arrLinkedAnimals = rdr.getAnimalsLinkedToExportCertificate(tempExpCert, svr);
				if (arrLinkedAnimals != null && arrLinkedAnimals.size() > 0) {
					for (DbDataObject dboAnimal : arrLinkedAnimals.getItems()) {
						if (dboAnimal.getStatus().equals(Tc.PENDING_EX)) {
							undoAnimalPendingExport(dboAnimal, rdr, svr, svw);
						}
					}
				}
				changeStatus(tempExpCert, Tc.EXPIRED, svw);
			}
		}
	}

	public void undoAnimalPendingExport(DbDataObject dboObjectToHandle, Reader rdr, SvReader svr, SvWriter svw)
			throws SvException {
		if (dboObjectToHandle.getStatus().equals(Tc.PENDING_EX)) {
			DbDataObject linkAnimalExportCert = rdr.getLinkBetweenAnimalAndExportCert(dboObjectToHandle, svr);
			if (linkAnimalExportCert != null) {
				invalidateLink(linkAnimalExportCert, svr);
			}
		}
		changeStatus(dboObjectToHandle, Tc.VALID, svw);
	}

	public DbDataObject createFlockMovementUnit(DbDataObject dboObjectToHandle, Long totalUnits, Long femaleUnits,
			Long maleUnits, Long adultsUnits, SvWriter svw, SvReader svr) throws SvException {
		Long newTotalMales = 0L;
		Long newTotalFemales = 0L;
		Long newTotalAdults = 0L;
		Long newTotalBeehivesUnits = 0L;
		Boolean createNewFlock = true;
		DbDataObject flockUnitsToMoveObj = null;
		flockUnitsToMoveObj = createFlockObject(dboObjectToHandle.getVal(Tc.ANIMAL_TYPE).toString(),
				dboObjectToHandle.getVal(Tc.EAR_TAG_COLOR) != null
						? dboObjectToHandle.getVal(Tc.EAR_TAG_COLOR).toString() : null,
				maleUnits, femaleUnits, totalUnits, adultsUnits,
				dboObjectToHandle.getVal(Tc.REGISTRATION_DATE) != null
						? dboObjectToHandle.getVal(Tc.REGISTRATION_DATE).toString() : null,
				dboObjectToHandle.getVal(Tc.USED_TAG_QUANTITY) != null
						? dboObjectToHandle.getVal(Tc.USED_TAG_QUANTITY).toString() : null,
				dboObjectToHandle.getParent_id());
		if (!dboObjectToHandle.getVal(Tc.ANIMAL_TYPE).toString().equals("4")) {
			if (totalUnits != null && totalUnits > 0L) {
				throw (new SvException("naits.error.totalUnitsApplicableOnlyOnBeehives", svr.getInstanceUser()));
			}
			if (!femaleUnits.equals(0L) && adultsUnits.equals(0L) && dboObjectToHandle.getVal(Tc.ADULTS) != null
					&& !dboObjectToHandle.getVal(Tc.ADULTS).toString().equals("0")) {
				throw (new SvException("naits.error.pleaseEnterNumberOfAdultEwesWhenFemalesIsInserted",
						svr.getInstanceUser()));
			}
			if (!adultsUnits.equals(0L) && !femaleUnits.equals(0L) && adultsUnits > femaleUnits) {
				throw (new SvException("naits.error.numberOfAdultUnitsCantBeBiggerThanTotalNumberOfFlock",
						svr.getInstanceUser()));
			}
			if (dboObjectToHandle.getVal(Tc.ADULTS) == null) {
				dboObjectToHandle.setVal(Tc.ADULTS, 0L);
				// svw.saveObject(dboObjectToHandle, false);
			}
			if (dboObjectToHandle.getVal(Tc.FEMALES) != null && dboObjectToHandle.getVal(Tc.MALES) != null) {
				if ((Long.valueOf(dboObjectToHandle.getVal(Tc.FEMALES).toString()).equals(femaleUnits)
						&& Long.valueOf(dboObjectToHandle.getVal(Tc.MALES).toString()).equals(maleUnits)
						&& Long.valueOf(dboObjectToHandle.getVal(Tc.ADULTS).toString()).equals(adultsUnits))
						|| femaleUnits.equals(0L) && maleUnits.equals(0L) && adultsUnits.equals(0L)) {
					createNewFlock = false;
				}
			}
			// MALES
			if (createNewFlock) {
				if (maleUnits != null && !maleUnits.equals(0L)) {
					if (dboObjectToHandle.getVal(Tc.MALES) == null
							|| dboObjectToHandle.getVal(Tc.MALES).toString().trim().equals("")) {
						throw (new SvException("naits.error.selectedFlockDoesntHaveMales", svr.getInstanceUser()));
					}
					if (maleUnits > Long.valueOf(dboObjectToHandle.getVal(Tc.MALES).toString())) {
						throw (new SvException("naits.error.numberOfMaleUnitsCantBeBiggerThanTotalNumberOfFlock",
								svr.getInstanceUser()));
					}
					newTotalMales = Long.valueOf(dboObjectToHandle.getVal(Tc.MALES).toString()) - maleUnits;
					dboObjectToHandle.setVal(Tc.MALES, newTotalMales);
				}
				// FEMALES
				if (femaleUnits != null && !femaleUnits.equals(0L)) {
					if (dboObjectToHandle.getVal(Tc.FEMALES) == null
							|| dboObjectToHandle.getVal(Tc.FEMALES).toString().trim().equals("")) {
						throw (new SvException("naits.error.selectedFlockDoesntHaveFemales", svr.getInstanceUser()));
					}
					if (femaleUnits > Long.valueOf(dboObjectToHandle.getVal(Tc.FEMALES).toString())) {
						throw (new SvException("naits.error.numberOfFemaleUnitsCantBeBiggerThanTotalNumberOfFlock",
								svr.getInstanceUser()));
					}
					newTotalFemales = Long.valueOf(dboObjectToHandle.getVal(Tc.FEMALES).toString()) - femaleUnits;
					dboObjectToHandle.setVal(Tc.FEMALES, newTotalFemales);
				}
				// ADULTS
				if (adultsUnits != null && !adultsUnits.equals(0L)) {
					if (dboObjectToHandle.getVal(Tc.ADULTS) == null
							|| dboObjectToHandle.getVal(Tc.ADULTS).toString().trim().equals("")) {
						throw (new SvException("naits.error.selectedFlockDoesntHaveAdults", svr.getInstanceUser()));
					}
					if (adultsUnits > Long.valueOf(dboObjectToHandle.getVal(Tc.ADULTS).toString())) {
						throw (new SvException("naits.error.numberOfAdultUnitsCantBeBiggerThanTotalNumberOfFlock",
								svr.getInstanceUser()));
					}
					if (femaleUnits.equals(0L)) {
						flockUnitsToMoveObj.setVal(Tc.FEMALES, adultsUnits);
						newTotalFemales = Long.valueOf(dboObjectToHandle.getVal(Tc.FEMALES).toString()) - adultsUnits;
						dboObjectToHandle.setVal(Tc.FEMALES, newTotalFemales);
					}
					newTotalAdults = Long.valueOf(dboObjectToHandle.getVal(Tc.ADULTS).toString()) - adultsUnits;
					dboObjectToHandle.setVal(Tc.ADULTS, newTotalAdults);
				}
				if ((Long.valueOf(dboObjectToHandle.getVal(Tc.FEMALES).toString()).equals(0L))
						&& Long.valueOf(dboObjectToHandle.getVal(Tc.MALES).toString()).equals(0L)
						&& Long.valueOf(dboObjectToHandle.getVal(Tc.ADULTS).toString()).equals(0L)) {
					createNewFlock = false;
				}
			}
		} else {
			if ((maleUnits != null && maleUnits > 0L) || (femaleUnits != null && femaleUnits > 0L)
					|| (adultsUnits != null && adultsUnits > 0L)) {
				throw (new SvException("naits.error.beehivesTypeInputOnlyTotal", svr.getInstanceUser()));
			}
			if (totalUnits == null || totalUnits > Long.valueOf(dboObjectToHandle.getVal(Tc.TOTAL).toString())) {
				throw (new SvException("naits.error.numberOfTotalUnitsCantBeBiggerThanTotalNumberOfFlock",
						svr.getInstanceUser()));
			}
			if (totalUnits != null && !totalUnits.equals(0L)) {
				newTotalBeehivesUnits = Long.valueOf(dboObjectToHandle.getVal(Tc.TOTAL).toString()) - totalUnits;
				dboObjectToHandle.setVal(Tc.TOTAL, newTotalBeehivesUnits);
			}
			if (newTotalBeehivesUnits.equals(0L)) {
				createNewFlock = false;
			}
		}
		if (createNewFlock) {
			svw.saveObject(dboObjectToHandle, false);
			svw.saveObject(flockUnitsToMoveObj, false);
		} else {
			flockUnitsToMoveObj = svr.getObjectById(dboObjectToHandle.getObject_id(),
					SvReader.getTypeIdByName(Tc.FLOCK), new DateTime());
		}
		return flockUnitsToMoveObj;
	}

	public String applyAvailableUnappliedInventoryItemsOnValidAnimals(Long holdingObjectId, SvReader svr, SvWriter svw)
			throws SvException {
		Reader rdr = new Reader();
		Writer wr = new Writer();
		Boolean hasValidItems = false;
		String result = "naits.error.validInventoryItemsNotFound";
		LinkedHashMap<String, String> jsonOrderedMap = new LinkedHashMap<String, String>();
		String localeId = svr.getUserLocaleId(svr.getInstanceUser());
		ArrayList<Long> appliedTagsOnAnimals = new ArrayList<>();
		Boolean classMatchesWithTagType = false;
		int counter = 0;
		DbDataArray arrInventoryItems = svr.getObjectsByParentId(holdingObjectId,
				SvReader.getTypeIdByName(Tc.INVENTORY_ITEM), null, 0, 0);
		if (arrInventoryItems != null && !arrInventoryItems.getItems().isEmpty()) {
			DbDataArray arrAnimals = svr.getObjectsByParentId(holdingObjectId, SvReader.getTypeIdByName(Tc.ANIMAL),
					null, 0, 0);
			for (DbDataObject dboInventoryItem : arrInventoryItems.getItems()) {
				if ((dboInventoryItem.getVal(Tc.TAG_STATUS) == null
						|| dboInventoryItem.getVal(Tc.TAG_STATUS).equals(Tc.NON_APPLIED))
						&& dboInventoryItem.getVal(Tc.TAG_TYPE) != null) {
					if (!hasValidItems) {
						hasValidItems = true;
					}
					if (arrAnimals != null && !arrAnimals.getItems().isEmpty()) {
						for (DbDataObject dboAnimal : arrAnimals.getItems()) {
							if (dboAnimal.getStatus().equals(Tc.VALID) && (appliedTagsOnAnimals.isEmpty()
									|| !appliedTagsOnAnimals.contains(dboAnimal.getObject_id()))) {
								switch (dboInventoryItem.getVal(Tc.TAG_TYPE).toString()) {
								case "1":// Cattle type
									if (dboAnimal.getVal(Tc.ANIMAL_CLASS) != null
											&& (dboAnimal.getVal(Tc.ANIMAL_CLASS).toString().equals("1")
													|| dboAnimal.getVal(Tc.ANIMAL_CLASS).toString().equals("2"))) {
										classMatchesWithTagType = true;
									}
									break;
								case "3":// Sheep type
									if (dboAnimal.getVal(Tc.ANIMAL_CLASS) != null
											&& dboAnimal.getVal(Tc.ANIMAL_CLASS).toString().equals("9")) {
										classMatchesWithTagType = true;
									}
									break;
								case "4":// Pig type
									if (dboAnimal.getVal(Tc.ANIMAL_CLASS) != null
											&& dboAnimal.getVal(Tc.ANIMAL_CLASS).toString().equals("11")) {
										classMatchesWithTagType = true;
									}
									break;
								default:
									break;
								}
								if (classMatchesWithTagType) {
									counter++;
									classMatchesWithTagType = false;
									appliedTagsOnAnimals.add(dboAnimal.getObject_id());
									dboInventoryItem.setVal(Tc.ANIMAL_OBJ_ID, dboAnimal.getObject_id());
									dboInventoryItem.setVal(Tc.TAG_STATUS, "APPLIED");
									dboInventoryItem.setParent_id(dboAnimal.getObject_id());
									svw.saveObject(dboInventoryItem, false);
									jsonOrderedMap.put(
											I18n.getText(localeId, "naits.main.inventory_item.general") + " "
													+ dboInventoryItem.getVal(Tc.EAR_TAG_NUMBER).toString() + " "
													+ I18n.getText(localeId, "en.of_type") + " "
													+ rdr.decodeCodeValue(SvReader.getTypeIdByName(Tc.INVENTORY_ITEM),
															Tc.TAG_TYPE, dboInventoryItem.getVal(Tc.TAG_TYPE)
																	.toString(),
															localeId, svr)
													+ " " + I18n.getText(localeId, "en.appliedOn"),
											dboAnimal.getVal(Tc.ANIMAL_ID).toString());

									DbDataObject dboEarTagReplc = wr.createEarTagReplcObject(
											dboInventoryItem.getVal(Tc.EAR_TAG_NUMBER).toString(),
											new DateTime().toString().substring(0, 10), "", "",
											dboAnimal.getObject_id(), svr);
									svw.saveObject(dboEarTagReplc, false);
									break;
								}
							}
						}
					} else {
						result = "naits.error.validAnimalsNotFound";
						break;
					}
				}
			}
		}
		if (counter == 0) {
			if (hasValidItems)
				result = "naits.error.validAnimalsNotFound";
		} else if (counter > 0) {
			result = I18n.getText(localeId, "naits.success.applyEarTagNumber");
			result += jsonOrderedMap.toString().trim().replace("{", "").replace("}", "").replace("=", ": ");
			svw.dbCommit();
		}
		return result;
	}

	public String generateTransferId(String externalId, SvReader svr) {
		String result = "";
		SvSequence svs = null;
		java.sql.Date dtNow = new java.sql.Date(new DateTime().getMillis());
		String dateSeq = String.valueOf(dtNow).replace("-", "");
		String extIdSeq = externalId.substring(externalId.length() - 2, externalId.length());
		try {
			svs = new SvSequence(svr.getSessionId());
			Long seqId = svs.getSeqNextVal(dateSeq + extIdSeq, false);
			String formattedSeq = String.format("%03d", Integer.valueOf(seqId.toString()));
			result = dateSeq + extIdSeq + "-" + formattedSeq;
			svs.dbCommit();
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
		} finally {
			if (svs != null)
				svs.release();
		}
		return result;
	}

	public DbDataObject createStrayPetLocationWithoutGPSCoordinates(String locType, String regionCode, String municCode,
			String communCode, String villageCode, String address, Long parentId) throws SvException {
		DbDataObject dbo = new DbDataObject();
		dbo.setObject_type(SvReader.getTypeIdByName(Tc.STRAY_PET_LOCATION));
		dbo.setParent_id(parentId);
		dbo.setStatus(Tc.VALID);
		if (locType != null)
			dbo.setVal(Tc.LOCATION_REASON, locType);
		if (regionCode != null)
			dbo.setVal(Tc.REGION_CODE, regionCode);
		if (municCode != null)
			dbo.setVal(Tc.MUNIC_CODE, municCode);
		if (communCode != null)
			dbo.setVal(Tc.COMMUN_CODE, communCode);
		if (villageCode != null)
			dbo.setVal(Tc.VILLAGE_CODE, villageCode);
		if (address != null)
			dbo.setVal(Tc.ADDRESS, address);
		return dbo;
	}

	public DbDataObject createPetLocationAccordingAnimalShelterHolding(DbDataObject dboPet,
			DbDataObject dboShelterHolding, String locationType, SvReader svr) throws SvException {
		DbDataObject dbo = new DbDataObject();
		dbo.setObject_type(SvReader.getTypeIdByName(Tc.STRAY_PET_LOCATION));
		dbo.setParent_id(dboPet.getObject_id());
		dbo.setStatus(Tc.VALID);
		if (dboPet.getVal(Tc.WEIGHT) != null) {
			dbo.setVal(Tc.WEIGHT, dboPet.getVal(Tc.WEIGHT));
		}
		if (dboShelterHolding != null) {
			if (locationType != null)
				dbo.setVal(Tc.LOCATION_REASON, locationType);
			if (dboShelterHolding.getVal(Tc.REGION_CODE) != null)
				dbo.setVal(Tc.REGION_CODE, dboShelterHolding.getVal(Tc.REGION_CODE));
			if (dboShelterHolding.getVal(Tc.MUNIC_CODE) != null)
				dbo.setVal(Tc.MUNIC_CODE, dboShelterHolding.getVal(Tc.MUNIC_CODE));
			if (dboShelterHolding.getVal(Tc.COMMUN_CODE) != null)
				dbo.setVal(Tc.COMMUN_CODE, dboShelterHolding.getVal(Tc.COMMUN_CODE));
			if (dboShelterHolding.getVal(Tc.VILLAGE_CODE) != null)
				dbo.setVal(Tc.VILLAGE_CODE, dboShelterHolding.getVal(Tc.VILLAGE_CODE));
		}
		return dbo;
	}

	public String generateReverseTransferId(String transferId, SvReader svr) {
		String result = "";
		SvSequence svs = null;
		try {
			svs = new SvSequence(svr.getSessionId());
			Long seqId = svs.getSeqNextVal(transferId + "-R-", false);
			String formattedSeq = String.format("%03d", Integer.valueOf(seqId.toString()));
			result = transferId + "-R-" + formattedSeq;
			svs.dbCommit();
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
		} finally {
			if (svs != null)
				svs.release();
		}
		return result;
	}

	/**
	 * Method for creating new animal object
	 * 
	 * @param jsonData
	 * @param sessionId
	 * @return
	 */
	public DbDataObject createAnimalObject(JsonArray jsonData, String sessionId) {
		SvWriter svw = null;
		DbDataObject animalObj = null;
		try {
			svw = new SvWriter(sessionId);
			svw.setAutoCommit(false);

			JsonObject jsonAnimalAttributes = jsonData.get(0).getAsJsonObject();
			if (jsonAnimalAttributes != null) {
				animalObj = new DbDataObject();
				animalObj.setObject_type(SvReader.getTypeIdByName(Tc.ANIMAL));
				animalObj.setStatus(Tc.VALID);

				if (jsonAnimalAttributes.has(Tc.PARENT_ID)) {
					animalObj.setParent_id(jsonAnimalAttributes.get(Tc.PARENT_ID).getAsLong());
				}
				if (jsonAnimalAttributes.has(Tc.ANIMAL_ID)) {
					animalObj.setVal(Tc.ANIMAL_ID, jsonAnimalAttributes.get(Tc.ANIMAL_ID).getAsString());
				}
				if (jsonAnimalAttributes.has(Tc.COUNTRY)) {
					animalObj.setVal(Tc.COUNTRY, jsonAnimalAttributes.get(Tc.COUNTRY).getAsString());
				}
				if (jsonAnimalAttributes.has(Tc.COUNTRY_OLD_ID)) {
					animalObj.setVal(Tc.COUNTRY_OLD_ID, jsonAnimalAttributes.get(Tc.COUNTRY_OLD_ID).getAsString());
				}
				if (jsonAnimalAttributes.has(Tc.OLD_ANIMAL_ID)) {
					animalObj.setVal(Tc.OLD_ANIMAL_ID, jsonAnimalAttributes.get(Tc.OLD_ANIMAL_ID).getAsString());
				}
				if (jsonAnimalAttributes.has(Tc.ANIMAL_CLASS)) {
					animalObj.setVal(Tc.ANIMAL_CLASS, jsonAnimalAttributes.get(Tc.ANIMAL_CLASS).getAsString());
				}
				if (jsonAnimalAttributes.has(Tc.ANIMAL_RACE)) {
					animalObj.setVal(Tc.ANIMAL_RACE, jsonAnimalAttributes.get(Tc.ANIMAL_RACE).getAsString());
				}
				if (jsonAnimalAttributes.has(Tc.GENDER)) {
					animalObj.setVal(Tc.GENDER, jsonAnimalAttributes.get(Tc.GENDER).getAsString());
				}
				if (jsonAnimalAttributes.has(Tc.COLOR)) {
					animalObj.setVal(Tc.COLOR, jsonAnimalAttributes.get(Tc.COLOR).getAsString());
				}
				if (jsonAnimalAttributes.has(Tc.BIRTH_DATE)) {
					animalObj.setVal(Tc.BIRTH_DATE, jsonAnimalAttributes.get(Tc.BIRTH_DATE).getAsString());
				}
				if (jsonAnimalAttributes.has(Tc.REGISTRATION_DATE)) {
					animalObj.setVal(Tc.REGISTRATION_DATE,
							jsonAnimalAttributes.get(Tc.REGISTRATION_DATE).getAsString());
				}
				if (jsonAnimalAttributes.has(Tc.MOTHER_TAG_ID)) {
					animalObj.setVal(Tc.MOTHER_TAG_ID, jsonAnimalAttributes.get(Tc.MOTHER_TAG_ID).getAsString());
				}
				if (jsonAnimalAttributes.has(Tc.FATHER_TAG_ID)) {
					animalObj.setVal(Tc.FATHER_TAG_ID, jsonAnimalAttributes.get(Tc.FATHER_TAG_ID).getAsString());
				}
				svw.saveObject(animalObj, false);
				svw.dbCommit();
			}
		} catch (SvException e) {
			log4j.error("Something went wrong while creating animal object");
		} finally {
			if (svw != null)
				svw.release();
		}
		return animalObj;
	}

	public Boolean updatePetId(DbDataObject dboPet, String updatedPetId, Reader rdr, SvWriter svw, SvReader svr)
			throws SvException {
		Boolean result = false;
		if (dboPet.getVal(Tc.PET_ID) == null) {
			throw (new SvException("naits.error.missingPetId", svr.getInstanceUser()));
		}
		String currentPetId = dboPet.getVal(Tc.PET_ID).toString();

		if (currentPetId.equals(updatedPetId)) {
			throw (new SvException("naits.error.cannotReplaceSamePetId", svr.getInstanceUser()));
		}
		DbDataObject dboLastValidPassport = rdr.getLastValidHealthPassport(dboPet, new DateTime(), svr);
		DbDataObject dboInventoryItemToApply = rdr.getInventoryItem(dboPet, updatedPetId, Tc.NON_APPLIED, false, svr);
		DbDataArray arrInventoryItems = svr.getObjectsByParentId(dboPet.getObject_id(),
				SvReader.getTypeIdByName(Tc.INVENTORY_ITEM), null, 0, 0);

		if (dboInventoryItemToApply == null) {
			throw (new SvException("naits.error.noAvailableInventoryItemWithCurrentEarTagId", svr.getInstanceUser()));
		}
		if (!arrInventoryItems.getItems().isEmpty()) {
			// Update the last inventory item
			DbDataObject dboLastInventoryItem = arrInventoryItems.get(0);
			if (dboLastInventoryItem.getVal(Tc.TAG_STATUS).toString().equals(Tc.APPLIED)) {
				dboLastInventoryItem.setVal(Tc.TAG_STATUS, Tc.REPLACED);
				dboLastInventoryItem.setVal(Tc.CHECK_COLUMN, false);
				svw.saveObject(dboLastInventoryItem, false);
			}
			// Update the replaced inventory item
			dboInventoryItemToApply.setParent_id(dboPet.getObject_id());
			dboInventoryItemToApply.setVal(Tc.TAG_STATUS, Tc.APPLIED);
			svw.saveObject(dboInventoryItemToApply, false);
			// Update pet id
			dboPet.setVal(Tc.PET_ID, updatedPetId);
			dboPet.setVal(Tc.CHECK_COLUMN, false);
			svw.saveObject(dboPet, false);

			if (dboLastValidPassport != null) {
				dboLastValidPassport.setVal(Tc.PET_ID, updatedPetId);
				svw.saveObject(dboLastValidPassport, false);
			}
			result = true;
			svw.dbCommit();
		}
		return result;
	}

	/**
	 * Method for creating DbDataObject Vaccination_Event from form data
	 * 
	 * @param jsonData
	 * @param sessionId
	 * @return
	 */
	public DbDataObject createDboVaccinationEvent(JsonArray jsonData, String sessionId) {
		SvWriter svw = null;
		DbDataObject dboVaccEvent = null;
		try {
			svw = new SvWriter(sessionId);
			svw.setAutoCommit(false);

			JsonObject jsonAnimalAttributes = jsonData.get(0).getAsJsonObject();
			if (jsonAnimalAttributes != null) {
				dboVaccEvent = new DbDataObject();
				dboVaccEvent.setObject_type(SvReader.getTypeIdByName(Tc.VACCINATION_EVENT));
				dboVaccEvent.setStatus(Tc.VALID);
				dboVaccEvent.setVal(Tc.CAMPAIGN_SCOPE, Tc.PET);

				if (jsonAnimalAttributes.has(Tc.CAMPAIGN_NAME)) {
					dboVaccEvent.setVal(Tc.CAMPAIGN_NAME, jsonAnimalAttributes.get(Tc.CAMPAIGN_NAME).getAsString());
				}
				if (jsonAnimalAttributes.has(Tc.EVENT_START)) {
					dboVaccEvent.setVal(Tc.EVENT_START, jsonAnimalAttributes.get(Tc.EVENT_START).getAsString());
				}
				if (jsonAnimalAttributes.has(Tc.EVENT_END)) {
					dboVaccEvent.setVal(Tc.EVENT_END, jsonAnimalAttributes.get(Tc.EVENT_END).getAsString());
				}
				if (jsonAnimalAttributes.has(Tc.DISEASE)) {
					dboVaccEvent.setVal(Tc.DISEASE, jsonAnimalAttributes.get(Tc.DISEASE).getAsString());
				}
				if (jsonAnimalAttributes.has(Tc.REGISTRATION_DATE)) {
					dboVaccEvent.setVal(Tc.REGISTRATION_DATE,
							jsonAnimalAttributes.get(Tc.REGISTRATION_DATE).getAsString());
				}
				if (jsonAnimalAttributes.has(Tc.ANIMAL_TYPE)) {
					dboVaccEvent.setVal(Tc.ANIMAL_TYPE, jsonAnimalAttributes.get(Tc.ANIMAL_TYPE).getAsString());
				}
				if (jsonAnimalAttributes.has(Tc.ACTIVITY_TYPE)) {
					dboVaccEvent.setVal(Tc.ACTIVITY_TYPE, jsonAnimalAttributes.get(Tc.ACTIVITY_TYPE).getAsString());
				}
				if (jsonAnimalAttributes.has(Tc.ACTIVITY_SUBTYPE)) {
					dboVaccEvent.setVal(Tc.ACTIVITY_SUBTYPE,
							jsonAnimalAttributes.get(Tc.ACTIVITY_SUBTYPE).getAsString());
				}
				if (jsonAnimalAttributes.has(Tc.IMMUNIZATION_PERIOD)) {
					dboVaccEvent.setVal(Tc.IMMUNIZATION_PERIOD,
							jsonAnimalAttributes.get(Tc.IMMUNIZATION_PERIOD).getAsLong());
				}
				if (jsonAnimalAttributes.has(Tc.PROHIBITION_PERIOD)) {
					dboVaccEvent.setVal(Tc.PROHIBITION_PERIOD,
							jsonAnimalAttributes.get(Tc.PROHIBITION_PERIOD).getAsLong());
				}
				if (jsonAnimalAttributes.has(Tc.NOTE)) {
					dboVaccEvent.setVal(Tc.NOTE, jsonAnimalAttributes.get(Tc.NOTE).getAsString());
				}
				svw.saveObject(dboVaccEvent, false);
				svw.dbCommit();
			}
		} catch (SvException e) {
			log4j.error("Something went wrong while creating vaccination object");
		} finally {
			if (svw != null)
				svw.release();
		}
		return dboVaccEvent;
	}

	/**
	 * Method for creating Pet movement object
	 * 
	 * @param dboPet
	 * @param destinationHoldingPic
	 * @param destinationHoldingObjectId
	 * @param dateOfAction
	 * @param setMovementStatus
	 * @param autoCommit
	 * @param svw
	 * @param svr
	 * @return
	 * @throws SvException
	 */
	public DbDataObject createPetMovement(DbDataObject dboPet, String destinationHoldingPic,
			Long destinationHoldingObjectId, DateTime dateOfAction, String movementStatus, String userSender,
			Boolean autoCommit, SvWriter svw, SvReader svr) throws SvException {
		DbDataObject dboSourceHolding = svr.getObjectById(dboPet.getParent_id(), SvReader.getTypeIdByName(Tc.HOLDING),
				null);
		DbDataObject dboPetMovement = new DbDataObject();
		dboPetMovement.setObject_type(SvReader.getTypeIdByName(Tc.PET_MOVEMENT));
		dboPetMovement.setParent_id(dboPet.getObject_id());
		dboPetMovement.setVal(Tc.PET_ID, dboPet.getVal(Tc.PET_ID).toString());
		if (dboSourceHolding != null && dboSourceHolding.getVal(Tc.PIC) != null) {
			dboPetMovement.setVal(Tc.SOURCE_HOLDING_PIC, dboSourceHolding.getVal(Tc.PIC).toString());
		}
		dboPetMovement.setVal(Tc.DESTINATION_HOLDING_PIC, destinationHoldingPic);
		dboPetMovement.setVal(Tc.HOLDING_OBJ_ID,
				destinationHoldingObjectId != null ? destinationHoldingObjectId.toString() : null);
		dboPetMovement.setVal(Tc.DT_DEPARTURE, dateOfAction);
		dboPetMovement.setVal(Tc.USER_SENDER, userSender);
		dboPetMovement.setStatus(movementStatus);

		svw.saveObject(dboPetMovement, autoCommit);
		return dboPetMovement;
	}

	public DbDataObject createSvarogLink(Long linkTypeObjId, DbDataObject dbo1, DbDataObject dbo2) {
		DbDataObject dbLink = new DbDataObject();
		dbLink.setObject_type(svCONST.OBJECT_TYPE_LINK);
		dbLink.setVal(Tc.LINK_TYPE_ID, linkTypeObjId);
		dbLink.setVal(Tc.LINK_OBJ_ID_1, dbo1.getObject_id());
		dbLink.setVal(Tc.LINK_OBJ_ID_2, dbo2.getObject_id());
		return dbLink;
	}

	public String createLinkBetweenUserAndOrgUnitHierarchically(DbDataObject dboObjectToHandle, DbDataObject dboOrgUnit,
			DbDataObject dbLinkType, Reader rdr, String localeId, SvWriter svw, SvReader svr) throws Exception {
		String result = I18n.getText(localeId, "naits.success.attachedUserToOrgUnit");
		DbDataArray arrLinks = new DbDataArray();
		try {
			DbDataObject dboLinkHeadquarter = rdr.getLinkObject(dboObjectToHandle.getObject_id(),
					dboOrgUnit.getObject_id(), dbLinkType.getObject_id(), svr);
			if (dboLinkHeadquarter != null) {
				throw (new SvException("naits.error.userAlreadyLinkedToAllOrgUnits", svr.getInstanceUser()));
			}
			dboLinkHeadquarter = createSvarogLink(dbLinkType.getObject_id(), dboObjectToHandle, dboOrgUnit);
			arrLinks.addDataItem(dboLinkHeadquarter);
			DbDataArray arrRegionOrgUnits = rdr.getOrgUntByParentOuId(dboOrgUnit.getObject_id(), svr);
			if (!arrRegionOrgUnits.getItems().isEmpty()) {
				for (DbDataObject dboRegion : arrRegionOrgUnits.getItems()) {
					DbDataObject dboLinkRegion = rdr.getLinkObject(dboObjectToHandle.getObject_id(),
							dboRegion.getObject_id(), dbLinkType.getObject_id(), svr);
					if (dboLinkRegion == null) {
						dboLinkRegion = createSvarogLink(dbLinkType.getObject_id(), dboObjectToHandle, dboRegion);
						arrLinks.addDataItem(dboLinkRegion);
					}
					DbDataArray arrMunicOrgUnits = rdr.getOrgUntByParentOuId(dboRegion.getObject_id(), svr);
					if (!arrMunicOrgUnits.getItems().isEmpty()) {
						for (DbDataObject dboMunic : arrMunicOrgUnits.getItems()) {
							DbDataObject dboLinkMunic = rdr.getLinkObject(dboObjectToHandle.getObject_id(),
									dboMunic.getObject_id(), dbLinkType.getObject_id(), svr);
							if (dboLinkMunic == null) {
								dboLinkMunic = createSvarogLink(dbLinkType.getObject_id(), dboObjectToHandle, dboMunic);
								arrLinks.addDataItem(dboLinkMunic);
							}
							DbDataArray arrCommunOrgUnits = rdr.getOrgUntByParentOuId(dboMunic.getObject_id(), svr);
							if (!arrCommunOrgUnits.getItems().isEmpty()) {
								for (DbDataObject dboCommun : arrCommunOrgUnits.getItems()) {
									DbDataObject dboLinkCommun = rdr.getLinkObject(dboObjectToHandle.getObject_id(),
											dboCommun.getObject_id(), dbLinkType.getObject_id(), svr);
									if (dboLinkCommun == null) {
										dboLinkCommun = createSvarogLink(dbLinkType.getObject_id(), dboObjectToHandle,
												dboCommun);
										arrLinks.addDataItem(dboLinkCommun);
									}
									DbDataArray arrVillageOrgUnits = rdr.getOrgUntByParentOuId(dboCommun.getObject_id(),
											svr);
									if (!arrVillageOrgUnits.getItems().isEmpty()) {
										for (DbDataObject dboVillage : arrVillageOrgUnits.getItems()) {
											DbDataObject dboLinkVillage = rdr.getLinkObject(
													dboObjectToHandle.getObject_id(), dboVillage.getObject_id(),
													dbLinkType.getObject_id(), svr);
											if (dboLinkVillage == null) {
												dboLinkVillage = createSvarogLink(dbLinkType.getObject_id(),
														dboObjectToHandle, dboVillage);
												arrLinks.addDataItem(dboLinkVillage);
											}
										}
									}
								}
							}
						}
					}
					if (!arrLinks.getItems().isEmpty()) {
						svw.saveObject(arrLinks, true, true);
						arrLinks = new DbDataArray();
					}
				}
			}
		} catch (SvException e) {
			result = I18n.getText(localeId, e.getLabelCode());
		}
		return result;
	}

	/**
	 * Method that links all sub-Org units according external ID
	 * 
	 * @param dboUserOrGroup
	 *            User or Group db instance
	 * @param externalId
	 *            Org unit external ID
	 * @param isGroup
	 *            Is DB instance group
	 * @param localeId
	 *            Locale ID
	 * @param dboLinkType
	 *            DB link type between User and Org unit
	 * @param rdr
	 *            Reader instance
	 * @param svl
	 *            SvLink instance
	 * @param svr
	 *            SvReader instance
	 * @return
	 * @throws SvException
	 */
	public ArrayList<String> linkSubOrgUnitsWithUserOrUserGroup(DbDataObject dboUserOrGroup, String externalId,
			boolean isGroup, String localeId, DbDataObject dboLinkType, Reader rdr, SvLink svl, SvReader svr)
			throws SvException {
		ArrayList<String> result = new ArrayList<>();
		DbDataArray arrSubOrgUnits = rdr.findDataPerSingleFilter(Tc.EXTERNAL_ID + "::VARCHAR", externalId,
				DbCompareOperand.LIKE, svCONST.OBJECT_TYPE_ORG_UNITS, svr);
		if (!arrSubOrgUnits.getItems().isEmpty()) {
			if (!isGroup) {
				for (DbDataObject dboOrgUnit : arrSubOrgUnits.getItems()) {
					DbDataObject dbLinkObj = rdr.getLinkObject(dboUserOrGroup.getObject_id(), dboOrgUnit.getObject_id(),
							dboLinkType.getObject_id(), svr);
					if (dbLinkObj == null) {
						svl.linkObjects(dboUserOrGroup, dboOrgUnit, Tc.POA, null, false, true);
					}
				}
			} else {
				result.add(dboUserOrGroup.getVal(Tc.GROUP_NAME) != null
						? I18n.getText(localeId, "naits.info.user_group_name")
								+ dboUserOrGroup.getVal(Tc.GROUP_NAME).toString()
						: "N/A");
				DbDataObject dboLinkBetweenUserAndUserGroup = SvReader.getLinkType(Tc.USER_GROUP,
						svCONST.OBJECT_TYPE_USER, dboUserOrGroup.getObject_type());
				DbDataArray arrayAttachedUsers = svr.getObjectsByLinkedId(dboUserOrGroup.getObject_id(),
						dboUserOrGroup.getObject_type(), dboLinkBetweenUserAndUserGroup, svCONST.OBJECT_TYPE_USER, true,
						null, 0, 0);
				result.add(I18n.getText(localeId, "naits.info.user_group_total_users")
						+ String.valueOf(arrayAttachedUsers.size()));
				if (!arrayAttachedUsers.getItems().isEmpty()) {
					for (DbDataObject dboOrgUnit : arrSubOrgUnits.getItems()) {
						for (DbDataObject dboUser : arrayAttachedUsers.getItems()) {
							DbDataObject dbLinkObj = rdr.getLinkObject(dboUser.getObject_id(),
									dboOrgUnit.getObject_id(), dboLinkType.getObject_id(), svr);
							if (dbLinkObj == null) {
								svl.linkObjects(dboUser, dboOrgUnit, Tc.POA, null, false, true);
							}
						}
					}
				}
			}
		}
		return result;
	}

	public String removeLinkBetweenUserAndOrgUnitHierarchically(DbDataObject dboObjectToHandle, DbDataObject dboOrgUnit,
			DbDataObject dbLinkType, Reader rdr, String localeId, SvWriter svw, SvReader svr) throws Exception {
		String result = I18n.getText(localeId, "naits.success.detachUserToOrgUnit");
		try {
			DbDataObject dboLinkHeadquarter = rdr.getLinkObject(dboObjectToHandle.getObject_id(),
					dboOrgUnit.getObject_id(), dbLinkType.getObject_id(), svr);
			if (dboLinkHeadquarter == null) {
				throw (new SvException("naits.error.userIsNotLinkedToHeadquarterOrgUnit", svr.getInstanceUser()));
			}
			svw.deleteObject(dboLinkHeadquarter, false);
			DbDataArray arrRegionOrgUnits = rdr.getOrgUntByParentOuId(dboOrgUnit.getObject_id(), svr);
			if (!arrRegionOrgUnits.getItems().isEmpty()) {
				for (DbDataObject dboRegion : arrRegionOrgUnits.getItems()) {
					DbDataObject dboLinkRegion = rdr.getLinkObject(dboObjectToHandle.getObject_id(),
							dboRegion.getObject_id(), dbLinkType.getObject_id(), svr);
					if (dboLinkRegion != null) {
						svw.deleteObject(dboLinkRegion, false);
					}
					DbDataArray arrMunicOrgUnits = rdr.getOrgUntByParentOuId(dboRegion.getObject_id(), svr);
					if (!arrMunicOrgUnits.getItems().isEmpty()) {
						for (DbDataObject dboMunic : arrMunicOrgUnits.getItems()) {
							DbDataObject dboLinkMunic = rdr.getLinkObject(dboObjectToHandle.getObject_id(),
									dboMunic.getObject_id(), dbLinkType.getObject_id(), svr);
							if (dboLinkMunic != null) {
								svw.deleteObject(dboLinkMunic, false);
							}
							DbDataArray arrCommunOrgUnits = rdr.getOrgUntByParentOuId(dboMunic.getObject_id(), svr);
							if (!arrCommunOrgUnits.getItems().isEmpty()) {
								for (DbDataObject dboCommun : arrCommunOrgUnits.getItems()) {
									DbDataObject dboLinkCommun = rdr.getLinkObject(dboObjectToHandle.getObject_id(),
											dboCommun.getObject_id(), dbLinkType.getObject_id(), svr);
									if (dboLinkCommun != null) {
										svw.deleteObject(dboLinkCommun, false);
									}
									DbDataArray arrVillageOrgUnits = rdr.getOrgUntByParentOuId(dboCommun.getObject_id(),
											svr);
									if (!arrVillageOrgUnits.getItems().isEmpty()) {
										for (DbDataObject dboVillage : arrVillageOrgUnits.getItems()) {
											DbDataObject dboLinkVillage = rdr.getLinkObject(
													dboObjectToHandle.getObject_id(), dboVillage.getObject_id(),
													dbLinkType.getObject_id(), svr);
											if (dboLinkVillage != null) {
												svw.deleteObject(dboLinkVillage, false);

											}
										}
									}
								}
							}
						}
					}
					svw.dbCommit();
				}
			}
		} catch (SvException e) {
			result = I18n.getText(localeId, e.getLabelCode());
		}
		return result;
	}

	/**
	 * Method for generating Archive ID for euthanized pets
	 * 
	 * @param dboPet
	 * @param svr
	 * @return
	 */
	public String generateArchiveNumber(DbDataObject dboPet, SvReader svr) {
		SvSequence svs = null;
		DateTime dtNow = new DateTime();
		String generatedQuarantineId = null;
		String monthAction = dtNow.getMonthOfYear() < 10 ? "0" + dtNow.getMonthOfYear()
				: String.valueOf(dtNow.getMonthOfYear());
		String yearAction = String.valueOf(dtNow.getYear());
		try {
			svs = new SvSequence(svr.getSessionId());
			Long seqId = svs.getSeqNextVal(dboPet.getObject_type().toString(), false);
			Thread.sleep(2);
			String petSequence = String.format("%06d", Integer.valueOf(seqId.toString()));
			generatedQuarantineId = yearAction + monthAction + "-" + petSequence;
			svs.dbCommit();
		} catch (SvException | InterruptedException e) {
			log4j.error("Error occurred while generating Archive ID: " + e);
			Thread.currentThread().interrupt();
		} finally {
			if (svs != null) {
				svs.release();
			}
		}
		return generatedQuarantineId;
	}

	public void createParamPetArchiveId(DbDataObject dboPet, SvReader svr) throws SvException {
		SvParameter svp = null;
		try {
			svp = new SvParameter(svr);
			String archiveId = generateArchiveNumber(dboPet, svr);
			svp.setParamString(dboPet, "param.pet_archive_id", archiveId, false);
		} catch (SvException e) {
			log4j.error("Error occurred while creating param.pet_archive_id: " + e.getFormattedMessage());
		} finally {
			if (svp != null) {
				svp.release();
			}
		}
	}

	public boolean invalidateLastValidMeasurementObject(Long parentId, String status, boolean autoCommit, SvWriter svw,
			SvReader svr) throws SvException {
		boolean result = true;
		try {
			DbSearchCriterion cr1 = new DbSearchCriterion(Tc.PARENT_ID, DbCompareOperand.EQUAL, parentId);
			DbSearchCriterion cr2 = new DbSearchCriterion(Tc.STATUS, DbCompareOperand.EQUAL, status);
			DbDataArray arrMeasurements = svr.getObjects(
					new DbSearchExpression().addDbSearchItem(cr1).addDbSearchItem(cr2),
					SvReader.getTypeIdByName(Tc.MEASUREMENT), null, 0, 0);
			if (!arrMeasurements.getItems().isEmpty()) {
				svw.deleteObject(arrMeasurements.get(0), autoCommit);
			}
		} catch (Exception e) {
			log4j.error("Error occurred while invalidating last valid Measurement...");
			result = false;
		}
		return result;
	}

	/**
	 * Method for creating measurement object
	 * 
	 * @param measurementValue
	 *            Value
	 * @param measurementType
	 *            Measurement type (WEIGHT/HEIGHT)
	 * @param measurementUnit
	 *            Measurement unit (G/KG/CM/M)
	 * @param measurementDate
	 *            Date of measurement
	 * @param parentId
	 *            Parent object on which measures has been taken
	 * @return
	 */
	public DbDataObject createMeasurementObject(Long measurementValue, String measurementType, String measurementUnit,
			DateTime measurementDate, Long parentId) {
		DbDataObject dboMeasurement = new DbDataObject();
		dboMeasurement.setObject_type(SvReader.getTypeIdByName(Tc.MEASUREMENT));
		dboMeasurement.setParent_id(parentId);
		dboMeasurement.setVal(Tc.MEASUREMENT_VALUE, measurementValue);
		dboMeasurement.setVal(Tc.MEASUREMENT_TYPE, measurementType);
		dboMeasurement.setVal(Tc.MEASUREMENT_UNIT, measurementUnit);
		dboMeasurement.setVal(Tc.MEASUREMENT_DATE, measurementDate);
		return dboMeasurement;
	}

	/**
	 * Method for generating EXCEL (xlsx) file according sample
	 * 
	 * @param dboPopulation
	 * @param sheetName
	 * @param localeId
	 *            EN/KA_GE
	 * @param out
	 * @param svr
	 * @throws SvException
	 */
	public byte[] createExcelFileForSampleState(DbDataObject dboPopulation, String sheetName, ByteArrayOutputStream out,
			SvReader svr) throws SvException {
		byte[] data = null;
		String enUsLocale = "en_US";
		String kaGeLocale = "ka_GE";
		StringBuilder query = PopulationQueryBuilder.getQueryAccordingPopulationFilters(dboPopulation);
		StringBuilder queryGeolocationFilter = PopulationQueryBuilder
				.buildQueryAccordingGeolocationFilters(dboPopulation, svr);
		if (!queryGeolocationFilter.toString().isEmpty()) {
			query.append(queryGeolocationFilter);
		}
		String queryWithCustomSelect = "";
		String queryHoldingValidation = "";
		if (dboPopulation.getVal(Tc.EXTRACTION_TYPE) != null) {
			if (dboPopulation.getVal(Tc.EXTRACTION_TYPE).toString().equals(Tc.ANIMAL)) {
				queryWithCustomSelect = PopulationQueryBuilder.buildSimpleQueryWithSubquery(
						"NAITS.VANIMAL VA JOIN NAITS.VHOLDING VH " + "ON VA.PARENT_ID=VH.OBJECT_ID ", "VA." + Tc.PKID,
						"IN", query,
						"VA.OBJECT_ID, NAITS.GET_LABEL_TEXT_PER_VALUE(VA.STATUS,'OBJ_STATUS','" + enUsLocale
								+ "') STATUS_en_US," + "NAITS.GET_LABEL_TEXT_PER_VALUE(VA.STATUS,'OBJ_STATUS','"
								+ kaGeLocale + "') STATUS_ka_GE",
						"ANIMAL_ID", "BIRTH_DATE", "REGISTRATION_DATE",
						"NAITS.GET_LABEL_TEXT_PER_VALUE(ANIMAL_CLASS,'ANIMAL_CLASS','" + enUsLocale
								+ "')ANIMAL_CLASS_en_US",
						"NAITS.GET_LABEL_TEXT_PER_VALUE(ANIMAL_CLASS,'ANIMAL_CLASS','" + kaGeLocale
								+ "')ANIMAL_CLASS_ka_GE",
						"NAITS.GET_LABEL_TEXT_PER_VALUE(ANIMAL_RACE,'ANIMAL_RACE','" + enUsLocale
								+ "')ANIMAL_RACE_ka_GE",
						"NAITS.GET_LABEL_TEXT_PER_VALUE(ANIMAL_RACE,'ANIMAL_RACE','" + kaGeLocale
								+ "')ANIMAL_RACE_ka_GE",
						"NAITS.GET_LABEL_TEXT_PER_VALUE(GENDER,'GENDER','" + enUsLocale + "')GENDER_en_US",
						"NAITS.GET_LABEL_TEXT_PER_VALUE(GENDER,'GENDER','" + kaGeLocale + "')GENDER_ka_GE",
						"NAITS.GET_LABEL_TEXT_PER_VALUE(COLOR,'COLOR','" + enUsLocale + "')COLOR_en_US",
						"NAITS.GET_LABEL_TEXT_PER_VALUE(COLOR,'COLOR','" + kaGeLocale + "')COLOR_ka_GE",
						"NAITS.GET_LABEL_TEXT_PER_VALUE(COUNTRY,'COUNTRY','" + enUsLocale + "')COUNTRY_en_US",
						"NAITS.GET_LABEL_TEXT_PER_VALUE(COUNTRY,'COUNTRY','" + kaGeLocale + "')COUNTRY_ka_GE",
						"NAITS.GET_LABEL_TEXT_PER_VALUE(COUNTRY_OLD_ID,'COUNTRY','" + enUsLocale
								+ "')COUNTRY_OLD_ID_en_US",
						"NAITS.GET_LABEL_TEXT_PER_VALUE(COUNTRY_OLD_ID,'COUNTRY','" + kaGeLocale
								+ "')COUNTRY_OLD_ID_ka_GE",
						"VA.EXTERNAL_ID", "MOTHER_TAG_ID", "FATHER_TAG_ID", "VH.PIC",
						"NAITS.GET_AGE_MONTHS(VA.BIRTH_DATE)", "VILLAGE_CODE");
			} else if (dboPopulation.getVal(Tc.EXTRACTION_TYPE).toString().equals(Tc.HOLDING)) {
				queryWithCustomSelect = PopulationQueryBuilder.buildSimpleQueryWithSubquery(
						"NAITS.VANIMAL VA JOIN NAITS.VHOLDING VH " + "ON VA.PARENT_ID=VH.OBJECT_ID ", "VA." + Tc.PKID,
						"IN", query,
						"DISTINCT VH.OBJECT_ID, NAITS.GET_LABEL_TEXT_PER_VALUE(VH.STATUS,'OBJ_STATUS','" + enUsLocale
								+ "') STATUS_en_US," + "NAITS.GET_LABEL_TEXT_PER_VALUE(VH.STATUS,'OBJ_STATUS','"
								+ kaGeLocale + "') STATUS_ka_GE",
						Tc.PIC, Tc.NAME + " AS \"HOLDING NAME\"", Tc.KEEPER_ID, Tc.KEEPER_MOBILE_NUM,
						"NAITS.GET_LABEL_TEXT_PER_VALUE(TYPE, 'HOLDING_MAIN_TYPE','" + enUsLocale
								+ "') AS \"HOLDING TYPE_en_US\"",
						"NAITS.GET_LABEL_TEXT_PER_VALUE(TYPE, 'HOLDING_MAIN_TYPE','" + kaGeLocale
								+ "') AS \"HOLDING TYPE_ka_GE\"",
						Tc.PHYSICAL_ADDRESS + " AS \"PHYSICAL ADDRESS\"",
						"NAITS.GET_LABEL_TEXT_PER_VALUE(REGION_CODE,'REGIONS','" + enUsLocale + "') AS REGION_en_US",
						"NAITS.GET_LABEL_TEXT_PER_VALUE(REGION_CODE,'REGIONS','" + kaGeLocale + "') AS REGION_ka_GE",
						"NAITS.GET_LABEL_TEXT_PER_VALUE(MUNIC_CODE,'MUNICIPALITIES','" + enUsLocale
								+ "') AS MUNICIPALITY_en_US",
						"NAITS.GET_LABEL_TEXT_PER_VALUE(MUNIC_CODE,'MUNICIPALITIES','" + kaGeLocale
								+ "') AS MUNICIPALITY_ka_GE",
						"NAITS.GET_LABEL_TEXT_PER_VALUE(COMMUN_CODE,'COMMUNITIES','" + enUsLocale
								+ "') AS COMMUNITY_en_US",
						"NAITS.GET_LABEL_TEXT_PER_VALUE(COMMUN_CODE,'COMMUNITIES','" + kaGeLocale
								+ "') AS COMMUNITY_ka_GE",
						"NAITS.GET_LABEL_TEXT_PER_VALUE(VILLAGE_CODE,'VILLAGES','" + enUsLocale + "') AS VILLAGE_en_US",
						"NAITS.GET_LABEL_TEXT_PER_VALUE(VILLAGE_CODE,'VILLAGES','" + kaGeLocale + "') AS VILLAGE_ka_GE",
						"DATE_OF_REG AS \"REGISTRATION DATE\"", "VH.EXTERNAL_ID AS \"EXTERNAL ID\"",
						"VH.APPROVAL_NUM AS \"APPROVAL NUMBER\"", "VILLAGE_CODE");
			}
			queryHoldingValidation = " AND VH.DT_DELETE > NOW()";
			if (dboPopulation.getVal(Tc.POPULATION_STATUS) != null) {
				if (dboPopulation.getVal(Tc.POPULATION_STATUS).equals(Tc.VALID)) {
					queryHoldingValidation += " AND VA.STATUS = 'VALID'";
				} else {
					queryHoldingValidation += " AND VA.STATUS <> 'VALID'";
				}
			}
			if (dboPopulation.getVal(Tc.HOLDING_STATUS) != null) {
				if (dboPopulation.getVal(Tc.HOLDING_STATUS).equals(Tc.VALID)) {
					queryHoldingValidation += " AND VH.STATUS = 'VALID'";
				} else {
					queryHoldingValidation += " AND VH.STATUS <> 'VALID'";
				}
			}
		}
		try {
			data = createExcelFileAccordingQueryResultSet(dboPopulation, queryWithCustomSelect + queryHoldingValidation,
					sheetName, out, svr);
		} catch (IOException e) {
			log4j.error(e);
		}
		return data;
	}

	/**
	 * Method for generating EXCEL (xlsx) file according result set from given
	 * query as parameter. The final result of this method, writes the data in
	 * ByteArrayOutputStream
	 * 
	 * @param query
	 *            Query string
	 * @param sheetName
	 *            Initial sheet name. If the number of rows from the ResultSet
	 *            is larger than 40 000, it will create new sheet in format
	 *            sheetName + n
	 * @param out
	 *            ByteArrayOutputStream instance
	 * @param svr
	 *            SvReader instance
	 * @throws IOException
	 */
	public byte[] createExcelFileAccordingQueryResultSet(DbDataObject dboPopulation, String query, String sheetName,
			ByteArrayOutputStream out, SvReader svr) throws IOException {
		XSSFWorkbook workbook = null;
		try {
			workbook = new XSSFWorkbook();
			setDataRowsToExcel(dboPopulation, workbook, sheetName, query, true, svr);
			workbook.write(out);
		} catch (Exception e) {
			log4j.error("Error occured while creating excel... ", e);
		}
		return out.toByteArray();
	}

	public byte[] createExcelFileAccordingDbDataArray(DbDataObject dboPopulation, DbDataObject dboStratFilter,
			ByteArrayOutputStream out, SvReader svr) throws IOException {
		XSSFWorkbook workbook = null;
		try {
			// workbook =
			// stratifyUploadedSamplePerPopulation(dboPopulation,dboStratFilter,
			// svr);
			workbook = stratifyUploadedSamplePerPopulationWithRandomizedXLSXRows(dboPopulation, dboStratFilter, svr);
			workbook.write(out);
		} catch (Exception e) {
			log4j.error("Error occured while creating excel... ", e);
		}
		return out.toByteArray();
	}

	/**
	 * Method for setting excel data according query result set
	 * 
	 * @param workbook
	 *            HSSFWorkbook instance
	 * @param sheetName
	 *            Initial sheet name. If the number of rows from the ResultSet
	 *            is larger than 60 000, it will create new sheet in format
	 *            sheetName + n
	 * @param query
	 *            Query string
	 * @param svr
	 *            SvReader instance
	 * @throws SvException
	 */
	public void setDataRowsToExcel(XSSFWorkbook workbook, String sheetName, String query, SvReader svr)
			throws SvException {
		setDataRowsToExcel(null, workbook, sheetName, query, false, svr);
	}

	public void setDataRowsToExcel(DbDataObject dboPopulation, XSSFWorkbook workbook, String sheetName, String query,
			boolean isPopulation, SvReader svr) throws SvException {
		SvWriter svw = null;
		try {
			svw = new SvWriter(svr);
			svw.dbSetAutoCommit(false);

			Connection conn = null;
			PreparedStatement selectStatement = null;
			XSSFSheet sheet = workbook.createSheet(sheetName);
			ResultSet rs = null;
			int counter = 0;
			int counterCommit = 0;
			int sheetNumber = 0;
			HashSet<String> hsGeostatCodes = null;
			HashSet<String> hsHoldings = null;
			DbDataArray arrPopulationLocations = new DbDataArray();
			try {
				conn = svr.dbGetConn();
				selectStatement = conn.prepareStatement(query, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
				rs = selectStatement.executeQuery();
				ArrayList<String> columnList = getListOfColumns(rs);
				setHeadRowDBColumns(sheet, columnList);
				if (isPopulation) {
					hsGeostatCodes = new HashSet<>();
					hsHoldings = new HashSet<>();
				}
				int countAnimals = 0;
				while (rs.next()) {
					if (counter == 60000) {
						sheet = workbook.createSheet(sheetName + String.valueOf(sheetNumber + 1));
						setHeadRowDBColumns(sheet, columnList);
						counter = 0;
					}
					counter++;
					countAnimals++;
					XSSFRow row = sheet.createRow(counter);
					ResultSetMetaData metaData = rs.getMetaData();
					// set population sample result location
					if (isPopulation) {
						String geostatCode = rs.getString(Tc.VILLAGE_CODE);
						String pic = rs.getString(Tc.PIC);
						hsHoldings.add(pic);
						if (hsGeostatCodes.add(geostatCode)) {
							DbDataObject dboPopulationLocation = createPopulationLocation(geostatCode,
									dboPopulation.getObject_id());
							arrPopulationLocations.addDataItem(dboPopulationLocation);
							counterCommit++;
							if (counterCommit == COMMIT_COUNT) {
								svw.saveObject(arrPopulationLocations, true);
								arrPopulationLocations = new DbDataArray();
								counterCommit = 0;
							}
						}
					}
					for (int i = 0; i < metaData.getColumnCount(); i++) {
						row.createCell(i)
								.setCellValue(rs.getObject(i + 1) != null ? rs.getObject(i + 1).toString() : "");
					}
				}
				if (!arrPopulationLocations.getItems().isEmpty()) {
					svw.saveObject(arrPopulationLocations, true);
				}
				if (hsHoldings != null) {
					createParamPopulationHoldingNum(dboPopulation, hsHoldings.size(), svr);
				}
				if (dboPopulation.getVal(Tc.EXTRACTION_TYPE) != null
						&& dboPopulation.getVal(Tc.EXTRACTION_TYPE).equals(Tc.ANIMAL)) {
					createParamPopulationAnimalsNum(dboPopulation, countAnimals, svr);
				}
			} catch (Exception e) {
				log4j.error("Error occurred while setting rows with data in excel file... " + e);
			} finally {
				try {
					if (rs != null)
						rs.close();
					if (conn != null)
						conn.close();
					if (selectStatement != null)
						selectStatement.close();
				} catch (SQLException e) {
					log4j.error(e.getLocalizedMessage());
				}
			}
		} catch (SvException e) {
			log4j.error(e);
		}
	}

	public DbDataObject createPopulationLocation(String geostatCode, Long parentId) {
		DbDataObject dboPopulationLocation = new DbDataObject();
		dboPopulationLocation.setObject_type(SvReader.getTypeIdByName(Tc.POPULATION_LOCATION));
		dboPopulationLocation.setParent_id(parentId);
		dboPopulationLocation.setVal(Tc.GEOSTAT_CODE, geostatCode);
		return dboPopulationLocation;
	}

	/**
	 * Method that returns list of Columns according ResultSet metadata
	 * 
	 * @param rs
	 * @return
	 * @throws SQLException
	 */
	public ArrayList<String> getListOfColumns(ResultSet rs) throws SQLException {
		ArrayList<String> columnList = new ArrayList<>();
		ResultSetMetaData metaData = rs.getMetaData();
		for (int i = 1; i < metaData.getColumnCount() + 1; i++) {
			columnList.add(metaData.getColumnName(i).toUpperCase());
		}
		return columnList;
	}

	/**
	 * Method that sets columns according list of column names at first row (0
	 * row)
	 * 
	 * @param sheet
	 * @param columnList
	 */
	public void setHeadRowDBColumns(XSSFSheet sheet, ArrayList<String> columnList) {
		try {
			XSSFRow rowhead = sheet.createRow(0);
			for (int i = 0; i < columnList.size(); i++) {
				String columnName = columnList.get(i);
				rowhead.createCell(i).setCellValue(columnName);
			}
			sheet.setAutoFilter(new CellRangeAddress(0, 0, 0, columnList.size()));
			sheet.createFreezePane(0, 1);
		} catch (Exception e) {
			log4j.error("Error occurred while setting head row of excel... " + e);
		}
	}

	/**
	 * Method for generating Archive ID for euthanized pets
	 * 
	 * @param dboPet
	 * @param svr
	 * @return
	 */
	public String generatePopulationId(DbDataObject dboPopulation, SvReader svr) {
		SvSequence svs = null;
		DateTime dtNow = new DateTime();
		String generatedPopulationId = null;
		String monthAction = dtNow.getMonthOfYear() < 10 ? "0" + dtNow.getMonthOfYear()
				: String.valueOf(dtNow.getMonthOfYear());
		String populationType = dboPopulation.getVal(Tc.POPULATION_TYPE) != null
				? dboPopulation.getVal(Tc.POPULATION_TYPE).toString().equals("1") ? "AI-" : "SDD-" : "";
		String yearAction = String.valueOf(dtNow.getYear());
		try {
			svs = new SvSequence(svr.getSessionId());
			Long seqId = svs.getSeqNextVal(dboPopulation.getObject_type().toString(), false);
			Thread.sleep(2);
			String seq = String.format("%05d", Integer.valueOf(seqId.toString()));
			generatedPopulationId = populationType + yearAction + monthAction + "-" + seq;
			svs.dbCommit();
		} catch (SvException | InterruptedException e) {
			log4j.error("Error occurred while population ID: " + e);
			Thread.currentThread().interrupt();
		} finally {
			if (svs != null) {
				svs.release();
			}
		}
		return generatedPopulationId;
	}

	public boolean uploadSampleXlsFile(String fileName, DbDataObject dboPopulation, String fileLabelCode, byte[] data,
			SvWriter svw, SvReader svr) {
		boolean result = true;
		SvFileStore svfs = null;
		DateTime refDate = null;
		DbDataObject dboFile = null;
		try {
			svfs = new SvFileStore(svr);
			boolean isUpdated = false;
			refDate = new DateTime();
			String svFileName = new StringBuilder().append(fileName).append("_").append(dboPopulation.getObject_id())
					.toString();

			DbDataObject dboLinkBetweenPopulationAndFile = SvReader.getLinkType(Tc.LINK_FILE,
					dboPopulation.getObject_type(), svCONST.OBJECT_TYPE_FILE);

			DbDataArray arrFiles = svr.getObjectsByLinkedId(dboPopulation.getObject_id(),
					dboLinkBetweenPopulationAndFile, refDate, 0, 0);
			if (!arrFiles.getItems().isEmpty()) {
				for (DbDataObject dboTempFile : arrFiles.getItems()) {
					if (dboTempFile.getVal(Tc.FILE_NOTES) != null
							&& dboTempFile.getVal(Tc.FILE_NOTES).toString().endsWith(fileLabelCode)) {
						dboFile = dboTempFile;
						dboFile.setVal(Tc.FILE_SIZE, data.length);
						svfs.saveFile(dboFile, null, data, false);
						isUpdated = true;
						break;
					}
				}
				if (!isUpdated && arrFiles.size() < 2) {
					dboFile = createSvFile(svFileName, refDate, fileLabelCode, -1L, data);
					svfs.saveFile(dboFile, dboPopulation, data, false);
				}
			} else {
				dboFile = createSvFile(svFileName, refDate, fileLabelCode, -1L, data);
				svfs.saveFile(dboFile, dboPopulation, data, false);
			}
			svw.dbCommit();
		} catch (SvException e) {
			log4j.debug("Error occured while uploading file... ", e);
			result = false;
		} finally {
			if (svfs != null)
				svfs.release();
		}
		return result;
	}

	public boolean downloadSampleXlsFile(DbDataObject dboPopulation, String fileLabelCode, OutputStream out,
			SvReader svr) throws IOException {
		boolean result = true;
		try {
			byte[] fileData = getLinkedSvFileByteArray(dboPopulation, fileLabelCode, svr);
			out.write(fileData);
		} catch (Exception e) {
			log4j.debug("Error occured while downloading Sample file... ", e);
			result = false;
		}
		return result;
	}

	/**
	 * Method that returns XSSFWorkbook (XLSX file) with stratified population
	 * sample
	 * 
	 * @param dboPopulation
	 *            {@link DbDataObject} POPULATION
	 * @param dboStratFilter
	 *            {@link DbDataObject} STRAT_FILTER
	 * @param out
	 *            {@link OutputStream}
	 * @param svr
	 *            {@link SvReader} instance
	 * @return {@link XSSFWorkbook} in XLSX format
	 * @throws IOException
	 */
	public XSSFWorkbook stratifyUploadedSamplePerPopulation(DbDataObject dboPopulation, DbDataObject dboStratFilter,
			SvReader svr) throws IOException {
		String sheetName = "stratification_result";
		XSSFWorkbook resultWorkbook = new XSSFWorkbook();
		XSSFSheet resultSheet = resultWorkbook.createSheet(sheetName);
		InputStream is = null;
		try {
			is = getLinkedSvFileStream(dboPopulation, svr);
			Reader rdr = new Reader();
			XSSFWorkbook workbook = new XSSFWorkbook(is);
			ArrayList<String> arrRandomVillages = rdr.getRandomGeoUnitsByStratificationFilter(dboStratFilter, svr);
			Integer maxNum = rdr.getMaxNumberOfAnimalsOrHoldings(dboStratFilter, svr);
			for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
				XSSFSheet sheet = workbook.getSheetAt(i);
				int rowCounter = 1;
				int tempCounter = 0;
				Iterator<Row> rowIterator = sheet.iterator();
				while (rowIterator.hasNext()) {
					Row row = rowIterator.next();
					if (row != null) {
						if (tempCounter < 1) {
							XSSFRow dataRow = resultSheet.createRow(0);
							int cellCounter = 0;
							Iterator<Cell> cellIterator = row.cellIterator();
							while (cellIterator.hasNext()) {
								Cell tempCell = cellIterator.next();
								dataRow.createCell(cellCounter).setCellValue(tempCell.getStringCellValue());
								cellCounter++;
							}
							resultSheet.setAutoFilter(new CellRangeAddress(0, 0, 0, cellCounter));
							resultSheet.createFreezePane(0, 1);
						} else {
							if (row != null) {
								XSSFRow dataRow = resultSheet.createRow(rowCounter);
								Cell lastCell = row.getCell(row.getLastCellNum() - 1);
								if (lastCell != null) {
									String villageCode = lastCell.getStringCellValue();
									if (arrRandomVillages.contains(villageCode)) {
										Iterator<Cell> cellIterator = row.cellIterator();
										int cellCounter = 0;
										while (cellIterator.hasNext()) {
											Cell tempCell = cellIterator.next();
											dataRow.createCell(cellCounter).setCellValue(tempCell.getStringCellValue());
											cellCounter++;
										}
										rowCounter++;
									}
								}
							}
						}
					}
					if (!maxNum.equals(0) && maxNum.equals(rowCounter - 1)) {
						break;
					}
					tempCounter++;
				}
			}
		} catch (Exception e) {
			log4j.error("Error occured while downloading Sample file... ", e);
		}
		return resultWorkbook;
	}

	/**
	 * Method that returns XSSFWorkbook (XLSX file) with stratified population
	 * sample. To be even more random, we randomize the rows in the Excel file
	 * 
	 * @param dboPopulation
	 *            {@link DbDataObject} POPULATION
	 * @param dboStratFilter
	 *            {@link DbDataObject} STRAT_FILTER
	 * @param out
	 *            {@link OutputStream}
	 * @param svr
	 *            {@link SvReader} instance
	 * @return {@link XSSFWorkbook} in XLSX format
	 * @throws IOException
	 */
	public XSSFWorkbook stratifyUploadedSamplePerPopulationWithRandomizedXLSXRows(DbDataObject dboPopulation,
			DbDataObject dboStratFilter, SvReader svr) throws IOException {
		String sheetName = "stratification_result";
		XSSFWorkbook resultWorkbook = new XSSFWorkbook();
		XSSFSheet resultSheet = resultWorkbook.createSheet(sheetName);
		InputStream is = null;
		try {
			is = getLinkedSvFileStream(dboPopulation, svr);
			Reader rdr = new Reader();
			XSSFWorkbook workbook = new XSSFWorkbook(is);
			ArrayList<String> arrRandomVillages = rdr.getRandomGeoUnitsByStratificationFilter(dboStratFilter, svr);
			Integer maxNum = rdr.getMaxNumberOfAnimalsOrHoldings(dboStratFilter, svr);
			for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
				XSSFSheet sheet = workbook.getSheetAt(i);
				XSSFRow tempRow = sheet.getRow(sheet.getFirstRowNum());
				int rowCounter = 1;
				int tempCounter = 0;
				XSSFRow tempDataRow = resultSheet.createRow(0);
				int tempCellCounter = 0;
				Iterator<Cell> tempCellIterator = tempRow.cellIterator();
				// set header
				while (tempCellIterator.hasNext()) {
					Cell tempCell = tempCellIterator.next();
					tempDataRow.createCell(tempCellCounter).setCellValue(tempCell.getStringCellValue());
					tempCellCounter++;
				}
				resultSheet.setAutoFilter(new CellRangeAddress(0, 0, 0, tempCellCounter));
				resultSheet.createFreezePane(0, 1);
				ArrayList<Integer> arrNumRows = new ArrayList<>();
				for (int j = 0; j < sheet.getLastRowNum(); j++) {
					arrNumRows.add(j);
				}
				Collections.shuffle(arrNumRows);
				for (int k = 0; k < arrNumRows.size(); k++) {
					XSSFRow row = sheet.getRow(arrNumRows.get(k));
					if (row != null && tempCounter > 0) {
						XSSFRow dataRow = resultSheet.createRow(rowCounter);
						Cell lastCell = row.getCell(row.getLastCellNum() - 1);
						if (lastCell != null) {
							String villageCode = lastCell.getStringCellValue();
							if (arrRandomVillages.contains(villageCode)) {
								Iterator<Cell> cellIterator = row.cellIterator();
								int cellCounter = 0;
								while (cellIterator.hasNext()) {
									Cell tempCell = cellIterator.next();
									dataRow.createCell(cellCounter).setCellValue(tempCell.getStringCellValue());
									cellCounter++;
								}
								rowCounter++;
							}
						}
					}
					if (!maxNum.equals(0) && maxNum.equals(rowCounter - 1)) {
						break;
					}
					tempCounter++;
				}
			}
		} catch (Exception e) {
			log4j.error("Error occured while downloading Sample file... ", e);
		}
		return resultWorkbook;
	}

	/**
	 * Method that returns attached Svarog file in byte array
	 * 
	 * @param dboObject
	 * @param linkTypeName
	 * @param svr
	 * @return last uploaded file
	 */
	public byte[] getLinkedSvFileByteArray(DbDataObject dboObject, String fileLabelCode, SvReader svr) {
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
					if (dboTempFile.getVal(Tc.FILE_NOTES) != null
							&& dboTempFile.getVal(Tc.FILE_NOTES).toString().endsWith(fileLabelCode)) {
						dboFile = dboTempFile;
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

	public InputStream getLinkedSvFileStream(DbDataObject dboObject, SvReader svr) {
		SvFileStore svfs = null;
		DateTime refDate = null;
		DbDataObject dboFile = null;
		InputStream is = null;
		try {
			svfs = new SvFileStore(svr);
			dboFile = new DbDataObject();
			refDate = new DateTime();
			DbDataObject dboLinkBetweenPopulationAndFile = SvReader.getLinkType(Tc.LINK_FILE,
					dboObject.getObject_type(), svCONST.OBJECT_TYPE_FILE);
			DbDataArray arrFiles = svr.getObjectsByLinkedId(dboObject.getObject_id(), dboLinkBetweenPopulationAndFile,
					refDate, 0, 0);
			if (!arrFiles.getItems().isEmpty()) {
				dboFile = arrFiles.get(0);
				is = svfs.getFileAsStream(dboFile);
			}
		} catch (Exception e) {
			log4j.error(e);
		} finally {
			if (svfs != null) {
				svfs.release();
			}
		}
		return is;
	}

	public DbDataObject createSvFile(String fileName, DateTime fileDate, String fileNote, Long fileStoreId,
			byte[] data) {
		DbDataObject object = new DbDataObject();
		object.setObject_type(svCONST.OBJECT_TYPE_FILE);
		object.setVal(Tc.FILE_TYPE, "ATTACHMENT");
		object.setVal(Tc.FILE_NAME, fileName);
		object.setVal(Tc.FILE_SIZE, data.length);
		object.setVal(Tc.FILE_DATE, fileDate);
		object.setVal(Tc.FILE_NOTES, fileNote);
		object.setVal(Tc.FILE_STORE_ID, fileStoreId);
		return object;
	}

	public DbDataObject createStratificationFilter(JsonArray jsonData, Long populationId, String sessionId) {
		SvWriter svw = null;
		DbDataObject dboStratificationObject = null;
		try {
			svw = new SvWriter(sessionId);
			svw.setAutoCommit(false);

			JsonObject jsonStratificationFilterObject = jsonData.get(0).getAsJsonObject();
			if (jsonStratificationFilterObject != null) {
				dboStratificationObject = new DbDataObject();
				dboStratificationObject.setObject_type(SvReader.getTypeIdByName(Tc.STRAT_FILTER));
				dboStratificationObject.setStatus(Tc.VALID);
				dboStratificationObject.setParent_id(populationId);

				if (jsonStratificationFilterObject.has(Tc.NUM_REGIONS)) {
					dboStratificationObject.setVal(Tc.NUM_REGIONS,
							jsonStratificationFilterObject.get(Tc.NUM_REGIONS).getAsLong());
				}
				if (jsonStratificationFilterObject.has(Tc.NUM_MUNICS)) {
					dboStratificationObject.setVal(Tc.NUM_MUNICS,
							jsonStratificationFilterObject.get(Tc.NUM_MUNICS).getAsLong());
				}
				if (jsonStratificationFilterObject.has(Tc.NUM_COMMUNS)) {
					dboStratificationObject.setVal(Tc.NUM_COMMUNS,
							jsonStratificationFilterObject.get(Tc.NUM_COMMUNS).getAsLong());
				}
				if (jsonStratificationFilterObject.has(Tc.NUM_VILLAGES)) {
					dboStratificationObject.setVal(Tc.NUM_VILLAGES,
							jsonStratificationFilterObject.get(Tc.NUM_VILLAGES).getAsLong());
				}
				if (jsonStratificationFilterObject.has(Tc.NUM_HOLDINGS)) {
					dboStratificationObject.setVal(Tc.NUM_HOLDINGS,
							jsonStratificationFilterObject.get(Tc.NUM_HOLDINGS).getAsLong());
				}
				if (jsonStratificationFilterObject.has(Tc.NUM_ANIMALS)) {
					dboStratificationObject.setVal(Tc.NUM_ANIMALS,
							jsonStratificationFilterObject.get(Tc.NUM_ANIMALS).getAsLong());
				}
				svw.saveObject(dboStratificationObject, false);
				svw.dbCommit();
			}
		} catch (SvException e) {
			log4j.error("Something went wrong while creating vaccination object");
		} finally {
			if (svw != null)
				svw.release();
		}
		return dboStratificationObject;
	}

	public void createParamPopulationHoldingNum(DbDataObject dboPopulation, Integer totalNum, SvReader svr)
			throws SvException {
		SvParameter svp = null;
		try {
			svp = new SvParameter(svr);
			svp.setParamString(dboPopulation, "param.population_sample.num_holdings", totalNum.toString(), false);
			svp.dbCommit();
		} catch (SvException e) {
			log4j.error("Error occurred while creating param.population_sample.num_holdings: ", e);
		} finally {
			if (svp != null) {
				svp.release();
			}
		}
	}

	public void createParamPopulationAnimalsNum(DbDataObject dboPopulation, Integer totalNum, SvReader svr)
			throws SvException {
		SvParameter svp = null;
		try {
			svp = new SvParameter(svr);
			svp.setParamString(dboPopulation, "param.population_sample.num_animals", totalNum.toString(), false);
			svp.dbCommit();
		} catch (SvException e) {
			log4j.error(
					"Error occurred while creating param.population_sample.num_animals: " + e.getFormattedMessage());
		} finally {
			if (svp != null) {
				svp.release();
			}
		}
	}

	/**
	 * Method that deletes animal and returns last Ear tag to headquarter
	 * 
	 * @return
	 * @throws SvException
	 */
	public String deleteAnimalObject(String animalId, String animalClass, SvWriter svw, SvReader svr)
			throws SvException {
		String result = "naits.success.deleteAnimalObject";
		Reader rdr = null;
		try {
			rdr = new Reader();
			DbDataObject dboAnimal = rdr.findAppropriateAnimalByAnimalIdAndAnimalClass(animalId, animalClass, true,
					svr);
			if (dboAnimal == null) {
				throw (new SvException("naits.error.naits.error.noAnimalFound", svCONST.systemUser, null, null));
			}
			DbDataObject dboHolding = svr.getObjectById(dboAnimal.getParent_id(), SvReader.getTypeIdByName(Tc.HOLDING),
					null);
			svw.deleteObject(dboAnimal);
			DbDataArray arrInventoryItems = svr.getObjectsByParentId(dboAnimal.getObject_id(),
					SvReader.getTypeIdByName(Tc.INVENTORY_ITEM), null, 0, 0);
			if (!arrInventoryItems.getItems().isEmpty()) {
				for (DbDataObject dboInventoryItem : arrInventoryItems.getItems()) {
					if (dboInventoryItem.getVal(Tc.EAR_TAG_NUMBER) != null
							&& dboInventoryItem.getVal(Tc.EAR_TAG_NUMBER).toString().equals(animalId)) {
						DbDataObject dboOrgUnitHeadquarter = rdr.searchForObject(svCONST.OBJECT_TYPE_ORG_UNITS,
								Tc.ORG_UNIT_TYPE, Tc.HEADQUARTER, svr);
						dboInventoryItem.setVal(Tc.TAG_STATUS, Tc.NON_APPLIED);
						dboInventoryItem.setParent_id(dboOrgUnitHeadquarter.getObject_id());
						dboInventoryItem.setVal(Tc.CHECK_COLUMN, true);
						svw.saveObject(dboInventoryItem);
						break;
					}
				}
			}
			if (dboHolding != null) {
				updateHoldingStatus(dboHolding, Tc.VALID, Tc.SUSPENDED, true, svr);
			}
			svw.dbCommit();
		} catch (SvException sve) {
			result = sve.getLabelCode();
		}
		return result;
	}

	public void setDestructionNotePerAnimal(DbDataObject dbo, String destructionNote, Reader rdr, boolean autoCommit,
			SvWriter svw, SvReader svr) {
		SvNote svn = null;
		try {
			svn = new SvNote(svr);
			DbDataObject dboNote = null;
			String existingNoteText = svn.getNote(dbo.getObject_id(), Tc.DESTRUCTION_NOTE);
			if (existingNoteText != null && !existingNoteText.trim().equals("")
					&& !existingNoteText.equals(destructionNote)) {
				dboNote = rdr.getNotesAccordingParentIdAndNoteName(dbo.getObject_id(), Tc.DESTRUCTION_NOTE, svr).get(0);
				dboNote.setVal(Tc.NOTE_TEXT, destructionNote);
				svw.saveObject(dboNote, false);
			} else {
				svn.setNote(dbo.getObject_id(), Tc.DESTRUCTION_NOTE, destructionNote, false);
			}
			if (autoCommit) {
				svn.dbCommit();
			}
		} catch (SvException sve) {
			log4j.error(sve);
		} finally {
			if (svn != null) {
				svn.release();
			}
		}
	}
}
