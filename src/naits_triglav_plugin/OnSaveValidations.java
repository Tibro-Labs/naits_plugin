/*******************************************************************************
 * Copyright (c),  2017 TIBRO DOOEL Skopje
 *******************************************************************************/

package naits_triglav_plugin;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.lang.math.NumberUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.DateTimeComparator;

import com.prtech.svarog.I18n;
import com.prtech.svarog.SvConf;
import com.prtech.svarog.SvConversation;
import com.prtech.svarog.SvCore;
import com.prtech.svarog.SvException;
import com.prtech.svarog.SvLink;
import com.prtech.svarog.SvLock;
import com.prtech.svarog.SvReader;
import com.prtech.svarog.SvSequence;
import com.prtech.svarog.SvUtil;
import com.prtech.svarog.SvWriter;
import com.prtech.svarog.svCONST;
import com.prtech.svarog_common.DbDataArray;
import com.prtech.svarog_common.DbDataObject;
import com.prtech.svarog_common.ISvOnSave;

/**
 * Class for definition and control on preSave/afterSave validations checks
 * 
 * @author TIBRO_001
 *
 */

public class OnSaveValidations implements ISvOnSave {

	static final Logger log4j = LogManager.getLogger(OnSaveValidations.class.getName());

	static ArrayList<Long> handledSvTypes = null;

	boolean isTypeHandled(Long typeId) {
		if (handledSvTypes == null) {
			handledSvTypes = new ArrayList<Long>();
			handledSvTypes.add(SvReader.getTypeIdByName(Tc.HOLDING));
			handledSvTypes.add(SvReader.getTypeIdByName(Tc.HOLDING_RESPONSIBLE));
			handledSvTypes.add(SvReader.getTypeIdByName(Tc.ANIMAL));
			handledSvTypes.add(SvReader.getTypeIdByName(Tc.ANIMAL_MOVEMENT));
			handledSvTypes.add(SvReader.getTypeIdByName(Tc.FLOCK));
			handledSvTypes.add(SvReader.getTypeIdByName(Tc.FLOCK_MOVEMENT));
			handledSvTypes.add(SvReader.getTypeIdByName(Tc.OTHER_ANIMALS));
			handledSvTypes.add(SvReader.getTypeIdByName(Tc.RANGE));
			handledSvTypes.add(SvReader.getTypeIdByName(Tc.EXPORT_CERT));
			handledSvTypes.add(SvReader.getTypeIdByName(Tc.DISEASE));
			handledSvTypes.add(SvReader.getTypeIdByName(Tc.AREA_HEALTH));
			handledSvTypes.add(SvReader.getTypeIdByName(Tc.POST_SLAUGHT_FORM));
			handledSvTypes.add(SvReader.getTypeIdByName(Tc.PRE_SLAUGHT_FORM));
			handledSvTypes.add(SvReader.getTypeIdByName(Tc.SUPPLIER));
			handledSvTypes.add(SvReader.getTypeIdByName(Tc.TRANSFER));
			handledSvTypes.add(SvReader.getTypeIdByName(Tc.QUARANTINE));
			handledSvTypes.add(SvReader.getTypeIdByName(Tc.SPOT_CHECK));
			handledSvTypes.add(SvReader.getTypeIdByName(Tc.VACCINATION_BOOK));
			handledSvTypes.add(SvReader.getTypeIdByName(Tc.VACCINATION_RESULTS));
			handledSvTypes.add(SvReader.getTypeIdByName(Tc.VACCINATION_EVENT));
			handledSvTypes.add(SvReader.getTypeIdByName(Tc.EAR_TAG_REPLC));
			handledSvTypes.add(SvReader.getTypeIdByName(Tc.LAB_SAMPLE));
			handledSvTypes.add(SvReader.getTypeIdByName(Tc.LABORATORY));
			handledSvTypes.add(SvReader.getTypeIdByName(Tc.LAB_TEST_RESULT));
			handledSvTypes.add(SvReader.getTypeIdByName(Tc.MOVEMENT_DOC));
			handledSvTypes.add(SvReader.getTypeIdByName(Tc.ORDER));
			handledSvTypes.add(SvReader.getTypeIdByName(Tc.PET));
			handledSvTypes.add(SvReader.getTypeIdByName(Tc.PET_HEALTH_BOOK));
			handledSvTypes.add(SvReader.getTypeIdByName(Tc.PET_PASSPORT));
			handledSvTypes.add(SvReader.getTypeIdByName(Tc.INVENTORY_ITEM));
			handledSvTypes.add(SvReader.getTypeIdByName(Tc.STRAY_PET));
			handledSvTypes.add(SvReader.getTypeIdByName(Tc.HEALTH_PASSPORT));
			handledSvTypes.add(SvReader.getTypeIdByName(Tc.STRAY_PET_LOCATION));
			handledSvTypes.add(SvReader.getTypeIdByName(Tc.POPULATION));
			handledSvTypes.add(svCONST.OBJECT_TYPE_NOTIFICATION);
			handledSvTypes.add(svCONST.OBJECT_TYPE_MESSAGE);
			handledSvTypes.add(svCONST.OBJECT_TYPE_USER);
			handledSvTypes.add(svCONST.OBJECT_TYPE_GROUP);
			handledSvTypes.add(svCONST.OBJECT_TYPE_LINK);
			handledSvTypes.add(svCONST.OBJECT_TYPE_CODE);
		}
		return handledSvTypes.indexOf(typeId) >= 0;
	}

	@Override
	public boolean beforeSave(SvCore parentCore, DbDataObject dbo) throws SvException {
		SvReader svr = null;
		SvWriter svw = null;
		SvLink svl = null;
		Reader rdr = null;
		Writer wr = null;
		Boolean result = true;
		ValidationChecks vc = null;
		ReentrantLock lock = null;
		DbDataObject dboAnimalOrFlock = null;
		if (!isTypeHandled(dbo.getObject_type())) {
			return true;
		}
		try {
			svr = new SvReader(parentCore);
			svw = new SvWriter(svr);
			svl = new SvLink(svw);
			rdr = new Reader();
			wr = new Writer();
			vc = new ValidationChecks();
			Long objectTypeId = dbo.getObject_type();
			if (objectTypeId == null || objectTypeId.equals(0L)) {
				result = false;
				throw (new SvException("system.objTypeNotFound", svCONST.systemUser, null, null));
			}
			DbDataObject objectTable = svr.getObjectById(objectTypeId, svCONST.OBJECT_TYPE_TABLE, null);
			String objTableName = objectTable.getVal(Tc.TABLE_NAME).toString();
			// depending from objType, do appropriate validations
			switch (objTableName) {
			case Tc.HOLDING:
				if (dbo.getObject_id().equals(0L)) {
					DbDataArray linkedHoldingsPerUser = rdr
							.getLinkedTablesWithGeoStatCodePerUser(objectTable.getObject_id(), svr);
					if (!linkedHoldingsPerUser.getItems().isEmpty()) {
						throw (new SvException("naits.error.userNotAllowedToAddHolding", svCONST.systemUser));
					}
				}
				if (dbo.getVal(Tc.PIC) == null) {
					generatePicPerHolding(dbo, parentCore);
				}
				autoSetSdiUnitsAccordingVillageCode(dbo);
				beforeSaveHoldingCheck(dbo, rdr, svr);
				break;
			case Tc.HOLDING_RESPONSIBLE:
				String publicRegistryPath = SvConf.getParam("public_registry.path");
				autoSetSdiUnitsAccordingVillageCode(dbo);
				if (!vc.checkIfFieldIsUnique(Tc.NAT_REG_NUMBER, dbo, svr)) {
					throw (new SvException("naits.error.natRegNumIsUnique", svCONST.systemUser, null, null));
				}
				if (!dbo.getObject_id().equals(0L)) {
					vc.checkIfHolderTypeCanBeUpdated(dbo, svr);
				}
				if (publicRegistryPath != null && dbo.getVal(Tc.HOLDER_TYPE).toString().equals("1")) {
					PublicRegistry pr = new PublicRegistry();
					if (!pr.publicRegistryCheck(dbo, publicRegistryPath, svr)) {
						dbo.setStatus(Tc.INVALID);
					}
				} else {
					beforeSaveHoldingResponsibleCheck(dbo, svr);
				}
				break;
			case Tc.ANIMAL:
				StringBuilder sb = new StringBuilder();
				String animalId = dbo.getVal(Tc.ANIMAL_ID).toString();
				String animalClass = dbo.getVal(Tc.ANIMAL_CLASS).toString();
				sb.append(animalId).append("_").append(animalClass);
				try {
					lock = SvLock.getLock(sb.toString(), false, 0);
					if (lock == null) {
						throw (new SvException("naits.error.objectUsedByOtherSession", svr.getInstanceUser()));
					}
					vc.inventoryItemCheck(dbo, rdr, svr);
					// check if animal id already existed once and was replaced
					if (rdr.findAnimalTagRepalcementViaOldEarTag(animalId, dbo.getParent_id(), svr) != null) {
						throw (new SvException("naits.error.animalIdAlreadyExistedButWasReplaced", svCONST.systemUser,
								null, null));
					}
					// prevent edition for slaughterhouse module
					if (vc.checkIfHoldingIsSlaughterhouse(dbo.getParent_id(), svr)) {
						if (dbo.getObject_id().equals(0L) && !vc.checkIfAnimalCanBeSavedToSlaughterHouseByOrigin(dbo))
							throw (new SvException(
									"naits.error.animalsRegisteredInSlaughterHouseMustHaveOriginInfoAndCantHaveOriginGe",
									svCONST.systemUser, null, null));
					}
					if (dbo.getVal(Tc.CHECK_COLUMN) == null || dbo.getVal(Tc.AUTO_GENERATED) == null) {
						result = beforeSaveAnimal(dbo, rdr, svr, svw);
						// TODO: Check if there is existing Lab Sample created
						// for current animal
					}
					vc.updateHoldingStatusAccordingHoldingTypeBeforeSaveAnimalOrFlock(dbo, svr);
					/*
					 * if (dbo.getVal(Tc.DEATH_DATE) != null && dbo.getStatus()
					 * !=
					 * 
					 * 
					 * null && dbo.getStatus().equals(Tc.VALID)) { throw (new
					 * SvException("naits.error.animalStatusIsStillValid",
					 * svCONST.systemUser, null, null)); }
					 */
				} finally {
					if (lock != null) {
						SvLock.releaseLock(sb.toString(), lock);
					}
				}
				break;
			case Tc.FLOCK:
				if (!vc.checkIfDbDataObjectHasMinimumNumOfFilledFields(dbo, 2)) {
					throw (new SvException("naits.error.cantSaveEmptyObject", svCONST.systemUser, null, null));
				}
				if (vc.checkIfDateIsInFuture(dbo, Tc.REGISTRATION_DATE)) {
					throw (new SvException("naits.error.registrationDateCannotBeInTheFuture", svCONST.systemUser, null,
							null));
				}
				wr.setFlockElementsToZero(dbo, Tc.ADULTS, true);
				// generate FLOCK ID
				if (dbo.getVal(Tc.FLOCK_ID) == null) {
					wr.generateFicPerFlock(dbo, svr);
				}
				if (dbo.getVal(Tc.ANIMAL_TYPE) == null) {
					throw (new SvException("naits.error.mustChooseAnimalType", svCONST.systemUser, null, null));
				}
				if (dbo.getVal(Tc.ANIMAL_TYPE) != null && !dbo.getVal(Tc.ANIMAL_TYPE).toString().equals("4")) {
					wr.calculateSumForFlock(dbo, svr);
				}
				if (dbo.getVal(Tc.ANIMAL_TYPE) != null && !dbo.getVal(Tc.ANIMAL_TYPE).toString().equals("4")
						&& !vc.validateFlockEwesNumber(dbo, svr)) {
					throw (new SvException("naits.error.NumberOfEwesCanNotBeBiggerThenNumberOfFemales",
							svCONST.systemUser, null, null));
				}
				if ((dbo.getVal(Tc.TOTAL) == null || dbo.getVal(Tc.TOTAL).toString().equals("0"))
						&& dbo.getVal(Tc.ANIMAL_TYPE) != null && dbo.getVal(Tc.ANIMAL_TYPE).toString().equals("4")) {
					throw (new SvException("naits.error.flockTotalCantBeNullAndMustBeLargerThanZero",
							svCONST.systemUser, null, null));
				}
				if (dbo.getVal(Tc.TOTAL) != null && dbo.getVal(Tc.TOTAL).toString().equals("0")
						&& dbo.getVal(Tc.ANIMAL_TYPE) != null && !dbo.getVal(Tc.ANIMAL_TYPE).toString().equals("4")) {
					throw (new SvException("naits.error.flockMalesOrFemalesCantBeNullOrZero", svCONST.systemUser, null,
							null));
				}
				if (dbo.getVal(Tc.ANIMAL_TYPE) != null && dbo.getVal(Tc.ANIMAL_TYPE).toString().equals("4")) {
					wr.setFlockElementsToZero(dbo, Tc.FEMALES, false);
					wr.setFlockElementsToZero(dbo, Tc.MALES, false);
					wr.setFlockElementsToZero(dbo, Tc.ADULTS, false);
				}
				vc.updateHoldingStatusAccordingHoldingTypeBeforeSaveAnimalOrFlock(dbo, svr);
				break;
			case Tc.ANIMAL_MOVEMENT:
				result = vc.checkIfAnimalMovementCanBeSaved(dbo, svr);
				break;
			case Tc.FLOCK_MOVEMENT:
				if (!vc.checkIfDepartureDateIsBeforeArrivalDate(dbo)) {
					throw (new SvException("naits.error.departureDateCannotBeAfterArrivalDate", svCONST.systemUser,
							null, null));
				}
				break;
			case Tc.OTHER_ANIMALS:
				// result = beforeSaveOtherAnimal(dbo, rdr, parentCore);
				if (!vc.checkIfDbDataObjectHasMinimumNumOfFilledFields(dbo, 2)) {
					throw (new SvException("naits.error.cantSaveEmptyObject", svCONST.systemUser, null, null));
				}
				break;
			case Tc.TRANSFER:
				wr.autoAsignSessionUserToObjectField(dbo, Tc.RETURNED_BY, false, svr);
				wr.setAutoDate(dbo, Tc.DATE_CREATED, false);
				DbDataObject dboOrgUnit = svr.getObjectById(dbo.getParent_id(), svCONST.OBJECT_TYPE_ORG_UNITS, null);
				if (dbo.getObject_id().equals(0L) && dbo.getVal(Tc.DIRECT_TRANSFER) == null) {
					dbo.setStatus(Tc.DRAFT);
				}
				if (!dbo.getObject_id().equals(0L)) {
					DbDataObject dboTransfer = svr.getObjectById(dbo.getObject_id(), dbo.getObject_type(),
							new DateTime());
					if (dboTransfer.getVal(Tc.SUBJECT_TO) != null && dbo.getVal(Tc.SUBJECT_TO) != null && !dboTransfer
							.getVal(Tc.SUBJECT_TO).toString().equals(dbo.getVal(Tc.SUBJECT_TO).toString())) {
						throw (new SvException("naits.error.arrivalPlaceCannotBeEdited", svCONST.systemUser, null,
								null));
					}
				}
				if (dbo.getVal(Tc.SUBJECT_TO) == null) {
					throw (new SvException("naits.error.transferMustHaveDepartureAndArrivalOrgUnit", svCONST.systemUser,
							null, null));
				}
				if (dbo.getObject_id().equals(0L) && dboOrgUnit != null) {
					if (dbo.getVal(Tc.TRANSFER_TYPE) != null && dbo.getVal(Tc.TRANSFER_TYPE).equals(Tc.DEFAULT)) {
						String externalId = "00";
						if (dboOrgUnit.getVal(Tc.EXTERNAL_ID) != null) {
							externalId = dboOrgUnit.getVal(Tc.EXTERNAL_ID).toString();
						}
						String transferId = wr.generateTransferId(externalId, svr);
						dbo.setVal(Tc.TRANSFER_ID, transferId);
					}
					if (dbo.getVal(Tc.SUBJECT_FROM) == null) {
						dbo.setVal(Tc.SUBJECT_FROM, dboOrgUnit.getVal(Tc.NAME).toString());
						dbo.setVal(Tc.ORIGIN_OBJ_ID, dboOrgUnit.getObject_id().toString());
					}
				}
				if (dbo.getVal(Tc.DESTINATION_OBJ_ID) != null) {
					DbDataObject dboDestinationOrgUnit = svr.getObjectById(
							Long.valueOf(dbo.getVal(Tc.DESTINATION_OBJ_ID).toString()), svCONST.OBJECT_TYPE_ORG_UNITS,
							null);
					if (dbo.getVal(Tc.DIRECT_TRANSFER) == null && dboDestinationOrgUnit != null
							&& dboDestinationOrgUnit.getObject_id().equals(dbo.getParent_id())) {
						throw (new SvException("naits.error.cantSendInventoryItemsToSameOrgUnit", svCONST.systemUser,
								null, null));
					}
				}
				result = vc.rangeValidationSet(dbo, rdr, Tc.TRANSFER, svr);
				break;
			case Tc.RANGE:
				result = vc.rangeValidationSet(dbo, rdr, Tc.RANGE, svr);
				break;
			case Tc.ORDER:
				if (dbo.getVal(Tc.ORDER_NUMBER) == null || dbo.getVal(Tc.ORDER_NUMBER).toString().equals("")) {
					throw (new SvException("naits.error.orderNumberIsMandatory", svCONST.systemUser, null, null));
				}
				if (!dbo.getObject_id().equals(0L)) {
					DbDataObject dboOrder = svr.getObjectById(dbo.getObject_id(), dbo.getObject_type(), new DateTime());
					if (dboOrder.getStatus().equals(Tc.RELEASED) && !dbo.getStatus().equals(Tc.RELEASED)) {
						throw (new SvException("naits.error.statusCantBeChangedOnReleasedOrder", svCONST.systemUser,
								null, null));
					}
				}
				if (dbo.getVal(Tc.DT_CREATION) == null) {
					dbo.setVal(Tc.DT_CREATION, new DateTime());
				}
				break;
			case Tc.SPOT_CHECK:
				wr.autoAsignSessionUserToObjectField(dbo, Tc.RESPONSIBLE_USER, true, svr);
				if (dbo.getObject_id().equals(0L) && dbo.getVal(Tc.DATE_OF_REG) == null) {
					wr.setAutoDate(dbo, Tc.DATE_OF_REG);
				}
				if (vc.checkIfDateIsInFuture(dbo, Tc.DATE_OF_REG)) {
					throw (new SvException("naits.error.registrationDateCannotBeInTheFuture", svr.getInstanceUser()));
				}
				if (vc.checkIfSameCheckSubjectIsShowingMoreThanOnceOnSameDateInHolding(dbo, svr))
					throw (new SvException("naits.error.CheckSubjectAlreadyExists", svCONST.systemUser, null, null));
				break;
			case Tc.SUPPLIER:
				if (rdr.getOrderStatusPerSupplier(dbo, svr).equals(Tc.RELEASED)) {
					throw (new SvException("naits.error.supplierCanNotBeModifiedAfterReleasedOrder", svCONST.systemUser,
							null, null));
				}
				break;
			case Tc.LABORATORY:
				if (dbo.getVal(Tc.LAB_NAME) == null || dbo.getVal(Tc.LAB_NAME).equals("")) {
					throw (new SvException("naits.error.labNameCantBeEmptyOrNull", svCONST.systemUser, null, null));
				}
				if (!vc.checkIfFieldIsUnique(Tc.LAB_NAME, dbo, svr)) {
					throw (new SvException("naits.error.labNameMustBeUnique", svCONST.systemUser, null, null));
				}
				break;
			case Tc.LAB_TEST_RESULT:
				DbDataObject dboLabSample = svr.getObjectById(dbo.getParent_id(),
						SvReader.getTypeIdByName(Tc.LAB_SAMPLE), null);
				if (!dbo.getObject_id().equals(0L)) {
					if (dbo.getVal(Tc.TEST_RESULT) == null || dbo.getVal(Tc.TEST_RESULT).equals("")) {
						throw (new SvException("naits.error.testResultCantBeEmptyOrNull", svCONST.systemUser, null,
								null));
					}
				}
				wr.setAutoDate(dbo, Tc.DATE_OF_TEST);
				DbDataObject dboTestType = rdr.getTestTypeDependOnLabSampleAndTestResult(dboLabSample, dbo, svr);
				if (dboTestType != null && dboTestType.getVal(Tc.TEST_NAME) != null) {
					dbo.setVal(Tc.TEST_NAME, dboTestType.getVal(Tc.TEST_NAME).toString());
				}
				break;
			case Tc.EAR_TAG_REPLC:
				DbDataObject parentAnimalObj = svr.getObjectById(dbo.getParent_id(),
						SvReader.getTypeIdByName(Tc.ANIMAL), null);
				if (dbo.getVal(Tc.NEW_EAR_TAG) != null && dbo.getVal(Tc.NEW_EAR_TAG).toString()
						.equals(parentAnimalObj.getVal(Tc.ANIMAL_ID).toString())) {
					throw (new SvException("naits.error.newEarTagCantBeSameAsOldEarTag", svCONST.systemUser, null,
							null));
				}
				DbDataObject oldInventoryItem = rdr.getInventoryItem(parentAnimalObj, Tc.APPLIED, true, svr);
				if (oldInventoryItem != null) {
					if (dbo.getVal(Tc.REASON) == null) {
						oldInventoryItem.setVal(Tc.TAG_STATUS, Tc.REPLACED);
					} else {
						if (dbo.getVal(Tc.REASON).toString().equals(Tc.WRONG_ENTRY)) {
							/*
							 * When ear_tag is wrongly entered, return the tag
							 * back to headquarter
							 */
							DbDataObject dboOrgUnitHeadquarter = rdr.searchForObject(svCONST.OBJECT_TYPE_ORG_UNITS,
									Tc.ORG_UNIT_TYPE, Tc.HEADQUARTER, svr);
							oldInventoryItem.setVal(Tc.TAG_STATUS, Tc.NON_APPLIED);
							oldInventoryItem.setParent_id(dboOrgUnitHeadquarter.getObject_id());
						} else {
							oldInventoryItem.setVal(Tc.TAG_STATUS, dbo.getVal(Tc.REASON).toString());
						}
					}
					oldInventoryItem.setVal(Tc.CHECK_COLUMN, true);
					svw.saveObject(oldInventoryItem, false);
				}
				break;
			case Tc.EXPORT_CERT:
				if (dbo.getVal(Tc.EXP_CERTIFICATE_ID) != null
						&& !NumberUtils.isNumber(dbo.getVal(Tc.EXP_CERTIFICATE_ID).toString())) {
					throw (new SvException("naits.error.exportCertificateIdInvalidFormat", svCONST.systemUser, null,
							null));
				}
				if (!vc.checkIfExportCertificateCanBeSaved(dbo, rdr, svr)) {
					throw (new SvException("naits.error.certificateCanNotBeGeneratedForBlacklistQuarantine",
							svCONST.systemUser, null, null));
				}
				// check forBIP HOLDING
				if (vc.checkIfBIPholdingValid(dbo, rdr, svr)) {
					throw (new SvException("naits.error.certificateCanNotBeGeneratedBIPholdingIDCisNotValid",
							svCONST.systemUser, null, null));
				}
				break;
			case Tc.AREA_HEALTH:
				DbDataObject parentObj = svr.getObjectById(dbo.getParent_id(), SvReader.getTypeIdByName(Tc.AREA), null);
				DbDataArray subAreas = rdr.getAreasByCoreArea(parentObj, svr);
				if ((dbo.getDt_delete() == null || !dbo.getDt_delete().isBeforeNow())
						&& vc.checkIfAreaHasDuplicateDisease(dbo, svr))
					throw (new SvException("naits.error.areaHealthCannotHaveDuplicateDisease", svCONST.systemUser, null,
							null));
				if (subAreas != null && !subAreas.getItems().isEmpty()) {
					wr.setAreaHealthToSubAreasDependOnCoreAreaAreaHealthObj(dbo, svr, svw);
				}
				if (dbo.getDt_delete() != null && dbo.getDt_delete().isBeforeNow() && subAreas != null
						&& !subAreas.getItems().isEmpty()) {
					wr.autoDeleteSubAreasHealthIfCoreAreaHealthIsDeleted(dbo, svr, svw);
				}
				break;
			case Tc.PRE_SLAUGHT_FORM:
				dboAnimalOrFlock = svr.getObjectById(dbo.getParent_id(), SvReader.getTypeIdByName(Tc.ANIMAL),
						new DateTime());
				if (dboAnimalOrFlock == null) {
					dboAnimalOrFlock = svr.getObjectById(dbo.getParent_id(), SvReader.getTypeIdByName(Tc.FLOCK), null);
					if (vc.isFlockSlaughterable(dboAnimalOrFlock)) {
						throw (new SvException("naits.error.cannotAddPreMortemOnFlockOfTypeBeehives",
								svCONST.systemUser, null, null));
					}
				}
				if (dbo.getObject_id().equals(0L)) {
					String diseases = rdr.getDiseaseasPerAnimalInMultiSelectDropDownFormat(dboAnimalOrFlock, rdr, svr);
					if (diseases != "") {
						wr.setDiseaseInPreOrPostSlaughterObj(diseases, dbo, svw);
					}
				}
				if (!(dboAnimalOrFlock.getStatus().equals(Tc.VALID)
						|| dboAnimalOrFlock.getStatus().equals(Tc.PREMORTEM))) {
					throw (new SvException("naits.error.animalOrFlockIsAlreadySlaughtred", svCONST.systemUser, null,
							null));
				}
				if (vc.checkIfAnimalOrFlockHasAlreadyValidPreMortem(dboAnimalOrFlock, svr)) {
					throw (new SvException("naits.error.animalOrFlockWithPermissableToKillPreMortemForm",
							svCONST.systemUser, null, null));
				}
				if (dboAnimalOrFlock.getStatus().equals(Tc.VALID)) {
					wr.changeStatus(dboAnimalOrFlock, Tc.PREMORTEM, svw, false);
				}
				if (vc.isDiseaseProhibited(dbo, Tc.DISEASE_SUSPISION_PM) || vc.isDiseaseProhibited(dbo, Tc.DISEASE)) {
					dbo.setVal(Tc.DECISION, "0"); // DISEASE SUSPISION
				}
				if (vc.checkIfAnimalHasBlockingDiseaseInPremortemForm(dboAnimalOrFlock, svr)) {
					throw (new SvException("naits.error.blockingDiseaseInPreMortem", svCONST.systemUser, null, null));
				}
				/*
				 * if (dboPreMortemForm != null &&
				 * (dboPreMortemForm.getVal(Tc.DISEASE) != null ||
				 * dboPreMortemForm.getVal(Tc.DISEASE).toString().equals("")) &&
				 * (dbo.getVal(Tc.DISEASE) != null ||
				 * !dbo.getVal(Tc.DISEASE).toString().equals(""))) { if
				 * (!dbo.getVal(Tc.DISEASE).equals(dboPreMortemForm.getVal(Tc.
				 * DISEASE))) { throw (new
				 * SvException("naits.error.diseaseFieldNotEditable",
				 * svCONST.systemUser, null, null)); } } else if
				 * (dbo.getObject_id().equals(0L) && dbo.getVal(Tc.DISEASE) !=
				 * null && dbo.getVal("IS_AUTO_GEN") == null) { throw (new
				 * SvException("naits.error.diseaseFieldNotEditable",
				 * svCONST.systemUser, null, null)); }
				 */

				if (!vc.checkIfAnimalHasAppropriateDiseaseInPreOrPostSlaughtForm(dbo, dboAnimalOrFlock, svr)) {
					throw (new SvException("naits.error.diseaseDoesntMatchWithAnimalBreedOrRace", svCONST.systemUser,
							null, null));
				}
				break;
			case Tc.POST_SLAUGHT_FORM:
				dboAnimalOrFlock = svr.getObjectById(dbo.getParent_id(), SvReader.getTypeIdByName(Tc.ANIMAL), null);
				DbDataObject dboPostSlaughter = svr.getObjectById(dbo.getObject_id(), dbo.getObject_type(),
						new DateTime());
				if (dboAnimalOrFlock == null) {
					dboAnimalOrFlock = svr.getObjectById(dbo.getParent_id(), SvReader.getTypeIdByName(Tc.FLOCK), null);
					if (vc.isFlockSlaughterable(dboAnimalOrFlock)) {
						throw (new SvException("naits.error.cannotAddPostMortemOnFlockOfTypeBeehives",
								svCONST.systemUser, null, null));
					}
				}
				if (dbo.getObject_id().equals(0L)) {
					String diseases = rdr.getDiseaseasPerAnimalInMultiSelectDropDownFormat(dboAnimalOrFlock, rdr, svr);
					if (diseases.equals("")) {
						wr.setDiseaseInPreOrPostSlaughterObj(diseases, dbo, svw);
					}
					if (!(dboAnimalOrFlock.getStatus().equals(Tc.SLAUGHTRD)
							|| dboAnimalOrFlock.getStatus().equals(Tc.POSTMORTEM))) {
						throw (new SvException("naits.error.statusOfTheAnimalIsNotSlaughtd", svCONST.systemUser, null,
								null));
					}
					if (dboAnimalOrFlock != null && dboAnimalOrFlock.getVal(Tc.DEATH_DATE) != null) {
						dbo.setVal(Tc.SLAUGHTER_DATE, new DateTime(dboAnimalOrFlock.getVal(Tc.DEATH_DATE).toString()));
					}
				}
				/*
				 * if (dboPostMortemForm != null &&
				 * (dboPostMortemForm.getVal(Tc.DISIESE_FINDING) != null ||
				 * dboPostMortemForm.getVal(Tc.DISIESE_FINDING).toString().
				 * equals("")) && (dbo.getVal(Tc.DISIESE_FINDING) != null ||
				 * !dbo.getVal(Tc.DISIESE_FINDING).toString().equals(""))) { if
				 * (!dbo.getVal(Tc.DISIESE_FINDING).equals(dboPostMortemForm.
				 * getVal(Tc.DISIESE_FINDING))) { throw (new
				 * SvException("naits.error.diseaseFieldNotEditable",
				 * svCONST.systemUser, null, null)); } } else if
				 * (dbo.getObject_id().equals(0L) && dbo.getVal(Tc.DISEASE) !=
				 * null && dbo.getVal("IS_AUTO_GEN") == null) { throw (new
				 * SvException("naits.error.diseaseFieldNotEditable",
				 * svCONST.systemUser, null, null)); }
				 */
				if (dbo.getVal(Tc.SLAUGHTER_DATE) != null) {
					DateTime dtSlaughter = new DateTime(dbo.getVal(Tc.SLAUGHTER_DATE).toString());
					if (dbo.getVal(Tc.MANUFACTURE_DATE) != null) {
						DateTime dtManufacture = new DateTime(dbo.getVal(Tc.MANUFACTURE_DATE).toString());
						if (dtManufacture.isBefore(dtSlaughter)) {
							throw (new SvException("naits.error.manufactureDateCantBeBeforeDateOfSlaughter",
									svCONST.systemUser, null, null));
						}
					}
					if (dboPostSlaughter != null && dboPostSlaughter.getVal(Tc.SLAUGHTER_DATE) != null) {
						DateTimeComparator dateTimeComparator = DateTimeComparator.getDateOnlyInstance();
						DateTime dbDtSlaughter = new DateTime(dboPostSlaughter.getVal(Tc.SLAUGHTER_DATE).toString());
						int dtDifference = dateTimeComparator.compare(dtSlaughter, dbDtSlaughter);
						if (dtDifference != 0) {
							throw (new SvException("naits.error.slaughterDateCantBeEdited", svCONST.systemUser, null,
									null));
						}
					}
				}
				if (!dbo.getObject_id().equals(0L)
						&& !vc.checkIfAnimalOrFlockHaveStatusPostMortem(dboAnimalOrFlock, svr)) {
					throw (new SvException("naits.error.statusOfTheAnimalIsNotSlaughtd", svCONST.systemUser, null,
							null));
				}
				if (!vc.checkIfAnimalHasAppropriateDiseaseInPreOrPostSlaughtForm(dbo, dboAnimalOrFlock, svr)) {
					throw (new SvException("naits.error.diseaseDoesntMatchWithAnimalBreedOrRace", svCONST.systemUser,
							null, null));
				}
				if (dboAnimalOrFlock != null && !dboAnimalOrFlock.getStatus().equals(Tc.POSTMORTEM)) {
					wr.changeStatus(dboAnimalOrFlock, Tc.POSTMORTEM, svw);
				}
				break;
			case Tc.QUARANTINE:
				vc.quarantineOnSaveValidationSet(dbo, rdr, svr);
				wr.trigerChangeOfExpCertStatus(svr, svw);
				break;
			case Tc.VACCINATION_BOOK:
				vc.vaccinationBookValdidationSet(dbo, wr, rdr, svw, svr);
				break;
			case Tc.VACCINATION_RESULTS:
				if (!vc.checkIfDbDataObjectHasMinimumNumOfFilledFields(dbo, 2)) {
					throw (new SvException("naits.error.cantSaveEmptyObject", svCONST.systemUser, null, null));
				}
				break;
			case Tc.VACCINATION_EVENT:
				vc.vaccinationEventValidationSet(dbo, svr);
				break;
			case Tc.LAB_SAMPLE:
				// code
				beforeSaveLabSample(dbo, rdr, wr, vc, svr, svw, svl);
				if (dbo.getVal(Tc.COLLECTIONER_NAME) == null) {
					DbDataObject dboUser = SvReader.getUserBySession(svr.getSessionId());
					dbo.setVal(Tc.COLLECTIONER_NAME, dboUser.getVal(Tc.USER_NAME).toString());
				}
				break;
			case Tc.MOVEMENT_DOC:
				if (dbo.getObject_id().equals(0L)) {
					wr.autoAsignSessionUserToObjectField(dbo, Tc.RESPONSIBLE_USER, true, svr);
				}
				if (!dbo.getObject_id().equals(0L) && dbo.getStatus().equals(Tc.RELEASED)) {
					wr.autoAsignSessionUserToObjectField(dbo, "recipient_user".toUpperCase(), true, svr);
				}
				break;
			case Tc.PET:
				vc.petValidationSet(dbo, wr, rdr, svw, svr);
				break;
			case Tc.STRAY_PET:
				vc.strayPetValidationSet(dbo, svr);
				break;
			case Tc.STRAY_PET_LOCATION:
				vc.strayPetLocationValidationSet(dbo, svr);
				break;
			case Tc.PET_HEALTH_BOOK:
				vc.petHealthBookValidationSet(dbo, wr, rdr, svr);
				break;
			case Tc.PET_PASSPORT:
				vc.petPassportValidationSet(dbo, rdr, svr);
				break;
			case Tc.HEALTH_PASSPORT:
				vc.healthPassportValidationSet(dbo, rdr, svw, svr);
				break;
			case Tc.PET_MOVEMENT:
				vc.petMovementValidationSet(dbo, svr);
				break;
			case Tc.SVAROG_USERS:
				if (!dbo.getObject_id().equals(0L) && dbo.getVal(Tc.USER_NAME) != null) {
					dbo.setVal(Tc.USER_NAME, dbo.getVal(Tc.USER_NAME).toString().toUpperCase());
				}
				break;
			case Tc.SVAROG_USER_GROUPS:
				if (dbo.getObject_id().equals(0L) && dbo.getVal("GROUP_UID") == null) {
					dbo.setVal("GROUP_UID", SvUtil.getUUID());
				}
				break;
			case Tc.SVAROG_NOTIFICATION:
				wr.autoAsignSessionUserToObjectField(dbo, Tc.SENDER, false, svr);
				vc.beforeSaveNotificationValidationSet(dbo, rdr, svr);
				break;
			case Tc.INVENTORY_ITEM:
				DbDataObject dboAnimalOrPet = svr.getObjectById(dbo.getParent_id(), SvReader.getTypeIdByName(Tc.ANIMAL),
						null);
				if (dboAnimalOrPet == null) {
					dboAnimalOrPet = svr.getObjectById(dbo.getParent_id(), SvReader.getTypeIdByName(Tc.PET), null);
				}
				if (dboAnimalOrPet != null && dbo.getVal(Tc.CHECK_COLUMN) == null) {
					DbDataObject dboInventoryItemObj = svr.getObjectById(dbo.getObject_id(), dbo.getObject_type(),
							new DateTime());
					if (dboInventoryItemObj != null && dboInventoryItemObj.getParent_id().equals(dbo.getParent_id())
							&& !dboInventoryItemObj.getValuesMap().toString().equals(dbo.getValuesMap().toString())) {
						throw (new SvException("naits.error.cannotEditAppliedEarTag", svCONST.systemUser, null, null));
					}
				}
				break;
			case Tc.POPULATION:
				vc.populationValidationSet(dbo, wr, svr);
				break;
			case Tc.SVAROG_MESSAGE:
				SvConversation svcon = new SvConversation();
				DbDataObject dboConversation = svr.getObjectById(dbo.getParent_id(), svCONST.OBJECT_TYPE_CONVERSATION,
						null);
				if ((boolean) dboConversation.getVal(Tc.IS_READ) && dbo.getVal(Tc.ASSIGNED_TO) != null) {
					if (dbo.getVal(Tc.CREATED_BY) != null
							&& !dbo.getVal(Tc.ASSIGNED_TO).toString().equals(dbo.getVal(Tc.CREATED_BY).toString())) {
						svcon.markAsUnread(svw, dboConversation);
					} else if (dboConversation.getVal(Tc.ASSIGNED_TO) != null && !dboConversation.getVal(Tc.ASSIGNED_TO)
							.toString().equals(dbo.getVal(Tc.ASSIGNED_TO).toString())) {
						svcon.markAsUnread(svw, dboConversation);
					}
				}
				break;
			case Tc.SVAROG_LINK:
				if (dbo.getVal(Tc.LINK_TYPE_ID) == null) {
					throw (new SvException("system.objTypeNotFound", svCONST.systemUser, null, null));
				}
				Long linkTypeId = Long.valueOf(dbo.getVal(Tc.LINK_TYPE_ID).toString());
				DbDataObject dboLinkType = svr.getObjectById(linkTypeId, svCONST.OBJECT_TYPE_LINK_TYPE, null);
				String linkName = dboLinkType.getVal(Tc.LINK_TYPE).toString();
				// by configuration in DbInit this can not be null
				Long objId1 = Long.valueOf(dbo.getVal(Tc.LINK_OBJ_ID_1).toString());
				Long objId2 = Long.valueOf(dbo.getVal(Tc.LINK_OBJ_ID_2).toString());

				// since we need to check every action correlated with person
				Long defObjectId = 0L;
				Long personType = SvReader.getTypeIdByName(Tc.HOLDING_RESPONSIBLE);
				if (Long.valueOf(dboLinkType.getVal("LINK_OBJ_TYPE_1").toString()).equals(personType)) {
					defObjectId = objId1;
				} else if (Long.valueOf(dboLinkType.getVal("LINK_OBJ_TYPE_2").toString()).equals(personType)) {
					defObjectId = objId2;
				}
				// DbDataObject dboPerson = svr.getObjectById(defObjectId,
				// personType, null);
				// if (dboPerson != null &&
				// dboPerson.getStatus().equals(Tc.INVALID)) {
				// throw (new
				// SvException("naits.error.personMustHaveStatusValidForAnyFurtherActions",
				// svCONST.systemUser, null, null));
				// }
				switch (linkName) {
				case Tc.HOLDING_KEEPER:
					DbDataObject dboHolding = svr.getObjectById(objId1, SvReader.getTypeIdByName(Tc.HOLDING), null);
					DbDataObject dboOwner = svr.getObjectById(objId2, SvReader.getTypeIdByName(Tc.HOLDING_RESPONSIBLE),
							null);
					if (rdr.checkIfSomeLinkExistsBetweenHoldingAndPerson(dboHolding, Tc.HOLDING_KEEPER, svr)) {
						DbDataObject currentOwner = rdr.getHoldingOwner(objId1, svr);
						if (currentOwner != null) {
							DbDataObject getKeeperLink = rdr.getLinkObject(objId1, currentOwner.getObject_id(),
									linkTypeId, svr);
							if (getKeeperLink != null) {
								svw.deleteObject(getKeeperLink, false);
							}
						}
					}
					dboHolding = svr.getObjectById(objId1, SvReader.getTypeIdByName(Tc.HOLDING), new DateTime());
					if (dboOwner != null) {
						// Check if holder type is not LEGAL_ENTITY
						if ((dboOwner.getVal(Tc.HOLDER_TYPE) == null
								|| !dboOwner.getVal(Tc.HOLDER_TYPE).toString().equals("2")) && objId1 != null
								&& objId2 != null) {
							wr.updateKeeperNameIfNeeded(dboHolding, dboOwner);
							wr.updateKeeperInfoInHolding(dboHolding, dboOwner);
						} else if (dboOwner.getVal(Tc.HOLDER_TYPE) != null
								&& dboOwner.getVal(Tc.HOLDER_TYPE).toString().equals("2")
								&& !vc.checkIfLegalEntityKeeperHasAtLeastOnePhysicalEntityAssociatedPerson(dboHolding,
										Tc.HOLDING_ASSOCIATED, rdr, svr)) {
							throw (new SvException(
									"naits.error.keeperThatIsLegalEntityMustHavePhysicalEntityAssociatedPerson",
									svCONST.systemUser, null, null));

						}
					}
					wr.updateHoldingStatus(dboHolding, Tc.NO_KEEPER, Tc.VALID, true, svr);
					break;
				case Tc.HOLDING_HERDER:
					DbDataObject dboHerder = svr.getObjectById(objId2, SvReader.getTypeIdByName(Tc.HOLDING_RESPONSIBLE),
							null);
					if (dboHerder != null && dboHerder.getVal(Tc.HOLDER_TYPE) != null
							&& !dboHerder.getVal(Tc.HOLDER_TYPE).toString().equals("1")) {
						throw (new SvException("naits.error.herderMustBePhysicalEntity", svCONST.systemUser, null,
								null));
					}
					break;
				case Tc.HOLDING_ASSOCIATED:
					DbDataObject dboAssociated = svr.getObjectById(objId2,
							SvReader.getTypeIdByName(Tc.HOLDING_RESPONSIBLE), null);
					if (dboAssociated != null && dboAssociated.getVal(Tc.HOLDER_TYPE) != null
							&& !dboAssociated.getVal(Tc.HOLDER_TYPE).toString().equals("1")) {
						throw (new SvException("naits.error.associatedMustBePhysicalEntity", svCONST.systemUser, null,
								null));
					}
					break;
				case Tc.DISEASE_QUARANTINE:
					if (!vc.checkIfQuarantineBlackList(objId2, svr))
						throw (new SvException("naits.error.quarantineCanNotHaveDiseaseIfNotFromBlacklistType",
								svCONST.systemUser, null, null));
					break;
				case Tc.ANIMAL_VACC_BOOK:
					DbDataObject dboEvent = null;
					DbDataObject dboVaccinationBook = svr.getObjectById(objId2,
							SvReader.getTypeIdByName(Tc.VACCINATION_BOOK), null);
					DbDataObject dboAnimal = svr.getObjectById(objId1, SvReader.getTypeIdByName(Tc.ANIMAL), null);
					if (dboVaccinationBook.getVal(Tc.CAMPAIGN_NAME) != null
							&& !dboVaccinationBook.getVal(Tc.CAMPAIGN_NAME).toString().equals("")) {
						DbDataArray arrVaccinationBooks = rdr.getLinkedVaccinationBooksPerAnimalOrFlock(dboAnimal, svr);
						if (vc.checkIfAnimalParticipatedInVaccinationEvent(arrVaccinationBooks,
								dboVaccinationBook.getVal(Tc.CAMPAIGN_NAME).toString())) {
							throw (new SvException("naits.error.animalAlreadyParticipatedInSelectedCampaign",
									svCONST.systemUser, null, null));
						}
						dboEvent = rdr.getVaccEventByName(dboVaccinationBook.getVal(Tc.CAMPAIGN_NAME).toString(), svr);
						List<String> animalTypes = rdr.getMultiSelectFieldValueAsList(dboEvent, Tc.ANIMAL_TYPE);
						if (!vc.checkIfAnimalTypeScopeIsApplicableOnSelectedAnimal(animalTypes, dboAnimal)) {
							throw (new SvException("naits.error.animalCampaignNotApplicableOnSelectedObjects",
									svr.getInstanceUser()));
						}
					}
					dboVaccinationBook.setVal(Tc.NO_ITEMS_TREATED, 1L);
					svw.saveObject(dboVaccinationBook, false);
					wr.createLabSampleBasedOnAnimalHealthBook(dboVaccinationBook, objId1, svr, svw);
					break;
				case Tc.FLOCK_VACC_BOOK:
					DbDataObject dboFlockTypeVaccinationBook = svr.getObjectById(objId2,
							SvReader.getTypeIdByName(Tc.VACCINATION_BOOK), null);
					DbDataObject dboFlock = svr.getObjectById(objId1, SvReader.getTypeIdByName(Tc.FLOCK), null);
					if (dboFlockTypeVaccinationBook.getVal(Tc.CAMPAIGN_NAME) != null
							&& !dboFlockTypeVaccinationBook.getVal(Tc.CAMPAIGN_NAME).toString().equals("")) {
						DbDataObject dboFlockEvent = rdr.getVaccEventByName(
								dboFlockTypeVaccinationBook.getVal(Tc.CAMPAIGN_NAME).toString(), svr);
						List<String> animalTypes = rdr.getMultiSelectFieldValueAsList(dboFlockEvent, Tc.ANIMAL_TYPE);
						if (!vc.checkIfAnimalTypeScopeIsApplicableOnSelectedAnimal(animalTypes, dboFlock)) {
							throw (new SvException("naits.error.animalCampaignNotApplicableOnSelectedObjects",
									svr.getInstanceUser()));
						}
					}
					if (dboFlockTypeVaccinationBook.getVal(Tc.NO_ITEMS_TREATED) != null
							&& dboFlock.getVal(Tc.TOTAL) != null
							&& Long.valueOf(dboFlockTypeVaccinationBook.getVal(Tc.NO_ITEMS_TREATED).toString()) > Long
									.valueOf(dboFlock.getVal(Tc.TOTAL).toString())) {
						throw (new SvException("naits.error.numberOfUnitsCantBeLargerThanTotalUnits",
								svr.getInstanceUser()));
					}
					break;
				case Tc.SUPPLY:
					DbDataObject dboOrder = svr.getObjectById(objId1, SvReader.getTypeIdByName(Tc.ORDER), null);
					if (dboOrder.getStatus().equals(Tc.RELEASED)) {
						throw (new SvException("naits.error.supplierCanNotBeAddedAfterReleasedOrder",
								svCONST.systemUser, null, null));
					}
					break;
				case Tc.HOLDING_QUARANTINE:
					DbDataObject holdingObj1 = svr.getObjectById(objId1, SvReader.getTypeIdByName(Tc.HOLDING), null);
					DbDataObject dboQuarantine = svr.getObjectById(objId2, SvReader.getTypeIdByName(Tc.QUARANTINE),
							null);
					if (!vc.checkIfQuarantineActive(dboQuarantine)) {
						throw (new SvException("naits.error.holdingCanNotBeAddedInNotActiveQuarantine",
								svCONST.systemUser, null, null));
					}
					if (objId2 != 0L && !vc.checkIfQuarantineBlackList(objId2, svr)
							&& rdr.checkIfSomeLinkExistsBetweenHoldingAndQuarantine(dboQuarantine, svr)) {
						throw (new SvException("naits.error.exportQuarantineCanHaveOnlyOneHolding", svCONST.systemUser,
								null, null));

					}
					if (objId2 != 0L && vc.checkIfHoldingBelongsInActiveQuarantine(holdingObj1, svr)) {
						throw (new SvException("naits.error.holdingCanBelongToOneActiveQuarantine", svCONST.systemUser,
								null, null));
					}
					if (holdingObj1.getVal(Tc.PIC) != null && dboQuarantine.getVal(Tc.RADIUS) == null) {
						Integer numIncomeMovements = rdr.getNoOfPendingMovements(holdingObj1.getVal(Tc.PIC).toString(),
								svr);
						Integer numOutgoingMovements = rdr.getNumOfOutgoingAnimalsOrFlock(objId1, Tc.ANIMAL, svr);
						Integer sumPendingMovements = numIncomeMovements + numOutgoingMovements;
						if (sumPendingMovements > 0) {
							throw (new SvException("naits.error.holdingWithPendingMovementCantHaveQuarantine",
									svCONST.systemUser, null, null));
						}
					}
					if (dboQuarantine.getVal(Tc.RADIUS) == null) {
						wr.trigerChangeOfExpCertStatus(svr, svw);
					}
					break;
				case Tc.SVAROG_CODES:
					if (!vc.setParentCodeValue(dbo, svr)) {
						throw (new SvException("naits.error.setCodeParentValue", svCONST.systemUser, null, null));
					}
					break;
				default:
					break;
				}
				break;
			default:
				break;
			}
		} finally {
			if (svr != null) {
				svr.release();
			}
			if (svw != null) {
				svw.release();
			}
			if (svl != null) {
				svl.release();
			}
		}
		return result;
	}

	@Override
	public void afterSave(SvCore parentCore, DbDataObject dbo) throws SvException {
		SvReader svr = null;
		SvWriter svw = null;
		SvLink svl = null;
		Reader rdr = null;
		Writer wr = null;
		ValidationChecks vc = new ValidationChecks();
		if (!isTypeHandled(dbo.getObject_type())) {
		}

		try {
			svr = new SvReader(parentCore);
			svw = new SvWriter(svr);
			svl = new SvLink(svw);
			rdr = new Reader();
			wr = new Writer();
			Long objectTypeId = dbo.getObject_type();
			if (objectTypeId == null || objectTypeId.equals(0L)) {
				throw (new SvException("naits.error.objTypeNotFound", svCONST.systemUser, null, null));
			}
			DbDataObject objectTable = svr.getObjectById(objectTypeId, svCONST.OBJECT_TYPE_TABLE, null);
			String objTableName = objectTable.getVal(Tc.TABLE_NAME).toString();
			// depending from objType, do appropriate validations
			switch (objTableName) {
			case Tc.ANIMAL:
				if (vc.isInventoryItemCheckBlocked(dbo)) {
					String blockCheck = SvConf.getParam("app_block.disable_animal_check");
					DbDataObject dboInventoryItem = rdr.getInventoryItem(dbo, Tc.NON_APPLIED, false, svr);
					if (blockCheck == null && dboInventoryItem != null) {
						dboInventoryItem.setParent_id(dbo.getObject_id());
						dboInventoryItem.setVal(Tc.TAG_STATUS, Tc.APPLIED);
						svw.saveObject(dboInventoryItem, true);
					}
				}
				break;
			case Tc.PET:
				vc.petAfterSaveValidationSet(dbo, wr, rdr, svw, svr);
				break;
			// case Tc.HOLDING_RESPONSIBLE:
			// String publicRegistryPath =
			// SvConf.getParam("public_registry.path");
			// if (dbo.getStatus().equals(Tc.INVALID) && publicRegistryPath !=
			// null) {
			// throw (new SvException("naits.error.noDataFoundForCurrentPerson",
			// svCONST.systemUser, null, null));
			// }
			// break;
			case Tc.LAB_SAMPLE:
				DbDataObject animalObj = svr.getObjectById(dbo.getParent_id(), SvReader.getTypeIdByName(Tc.ANIMAL),
						null);
				// for generating new Laboratory test result
				if (!dbo.getObject_id().equals(0L) && dbo.getVal(Tc.DISEASE_TEST) != null
						&& !dbo.getVal(Tc.DISEASE_TEST).toString().equals("") && dbo.getStatus().equals(Tc.RECEIVED)) {
					DbDataArray labTestResults = svr.getObjectsByParentId(dbo.getObject_id(),
							SvReader.getTypeIdByName(Tc.LAB_TEST_RESULT), null, 0, 0);
					if (labTestResults == null || labTestResults.getItems().isEmpty()) {
						wr.createNLabTestResultDependOnMultiSelectedDiseasesInLabSample(dbo, svw);
					}
				}
				// auto-create Animal health book
				if (animalObj != null && !dbo.getObject_id().equals(0L) && dbo.getVal(Tc.SAMPLE_ID) != null
						&& dbo.getStatus().equals(Tc.COLLECTED) && (dbo.getVal(Tc.CHECK_COLUMN) == null
								|| !dbo.getVal(Tc.CHECK_COLUMN).toString().equals("1"))) {
					DbDataObject healthBookObj = rdr.searchForObject(SvReader.getTypeIdByName(Tc.VACCINATION_BOOK),
							Tc.SAMPLE_ID, dbo.getVal(Tc.SAMPLE_ID).toString(), svr);
					if (healthBookObj == null) {
						healthBookObj = wr.createVaccinationBook(dbo.getVal(Tc.DATE_OF_COLLECTION).toString(), "2",
								dbo.getVal(Tc.SAMPLE_ID).toString(), svw);
						svw.saveObject(healthBookObj);
						svl.linkObjects(animalObj, healthBookObj, Tc.ANIMAL_VACC_BOOK, "", true);
					}
				}
				break;
			case Tc.LAB_TEST_RESULT:
				DbDataObject dboLabSample = svr.getObjectById(dbo.getParent_id(),
						SvReader.getTypeIdByName(Tc.LAB_SAMPLE), null);
				wr.setHealthStatusToLabSample(dboLabSample.getObject_id(), svr, svw);
				break;
			case Tc.PET_HEALTH_BOOK:
				if (dbo.getVal(Tc.VET_OFFICER) == null) {
					DbDataObject dboUser = svr.getObjectById(dbo.getUser_id(), svCONST.OBJECT_TYPE_USER, null);
					if (dboUser != null) {
						dbo.setVal(Tc.VET_OFFICER, dboUser.getVal(Tc.USER_NAME).toString());
					}
				}
				break;
			case Tc.STRAY_PET:
				DbDataArray arrExistingLocations = svr.getObjectsByParentId(dbo.getObject_id(),
						SvReader.getTypeIdByName(Tc.STRAY_PET_LOCATION), new DateTime(), 0, 0);
				if (arrExistingLocations.getItems().isEmpty()) {
					DbDataArray arrStrayPetLocations = new DbDataArray();
					// Collection location
					DbDataObject dboCollectionLoc = wr.createStrayPetLocationWithoutGPSCoordinates("1", "", null, null,
							null, null, dbo.getObject_id());
					arrStrayPetLocations.addDataItem(dboCollectionLoc);
					// Release location
					DbDataObject dboReleaseLoc = wr.createStrayPetLocationWithoutGPSCoordinates("2", "", null, null,
							null, null, dbo.getObject_id());
					arrStrayPetLocations.addDataItem(dboReleaseLoc);
					// svg.saveGeometry(arrStrayPetLocations, true);
				}
				break;
			case Tc.POST_SLAUGHT_FORM:
				DbDataObject invalidatedPostMortem = rdr.getObjectByPkid(dbo.getPkid(), Tc.POST_SLAUGHT_FORM, svr);
				DbDataObject dboAnimalOrFlock = svr.getObjectById(invalidatedPostMortem.getParent_id(),
						SvReader.getTypeIdByName(Tc.ANIMAL), new DateTime());
				if (invalidatedPostMortem.getDt_delete().isBeforeNow()) {
					dbo = invalidatedPostMortem;
					if (dboAnimalOrFlock == null) {
						dboAnimalOrFlock = svr.getObjectById(dbo.getParent_id(), SvReader.getTypeIdByName(Tc.FLOCK),
								null);
					}
					DbDataArray arrPostMortems = svr.getObjectsByParentId(dbo.getParent_id(), dbo.getObject_type(),
							null, 0, 0);
					if (arrPostMortems.getItems().isEmpty()) {
						dboAnimalOrFlock.setStatus(Tc.SLAUGHTRD);
						svw.saveObject(dboAnimalOrFlock, false);
					}
				}
				break;
			case Tc.PRE_SLAUGHT_FORM:
				DbDataObject invalidatedPreMortem = rdr.getObjectByPkid(dbo.getPkid(), Tc.PRE_SLAUGHT_FORM, svr);
				DbDataObject dboAnimalOrFlock2 = svr.getObjectById(invalidatedPreMortem.getParent_id(),
						SvReader.getTypeIdByName(Tc.ANIMAL), null);
				if (invalidatedPreMortem.getDt_delete().isBeforeNow()) {
					dbo = invalidatedPreMortem;
					if (dboAnimalOrFlock2 == null) {
						dboAnimalOrFlock2 = svr.getObjectById(dbo.getParent_id(), SvReader.getTypeIdByName(Tc.FLOCK),
								null);
					}
					DbDataArray arrPreMortems = svr.getObjectsByParentId(dbo.getParent_id(), dbo.getObject_type(), null,
							0, 0);
					if (arrPreMortems != null && arrPreMortems.getItems().isEmpty()) {
						wr.changeStatus(dboAnimalOrFlock2, Tc.VALID, svw);
						svw.dbCommit();
					}
				}
				break;
			case Tc.SVAROG_LINK:
				if (dbo.getVal(Tc.LINK_TYPE_ID) == null) {
					throw (new SvException("system.objTypeNotFound", svCONST.systemUser, null, null));
				}
				Long linkTypeId = Long.valueOf(dbo.getVal(Tc.LINK_TYPE_ID).toString());
				DbDataObject dboLinkType = svr.getObjectById(linkTypeId, svCONST.OBJECT_TYPE_LINK_TYPE, null);
				String linkName = dboLinkType.getVal(Tc.LINK_TYPE).toString();
				// by configuration in DbInit this can not be null
				Long objId1 = Long.valueOf(dbo.getVal(Tc.LINK_OBJ_ID_1).toString());
				Long objId2 = Long.valueOf(dbo.getVal(Tc.LINK_OBJ_ID_2).toString());
				switch (linkName) {
				case Tc.HOLDING_KEEPER:
					DbDataObject dboHolding = svr.getObjectById(objId1, SvReader.getTypeIdByName(Tc.HOLDING), null);
					DbDataObject dboOwner = svr.getObjectById(objId2, SvReader.getTypeIdByName(Tc.HOLDING_RESPONSIBLE),
							null);
					DbDataObject dboLink = rdr.getLinkObject(dboHolding.getObject_id(), dboOwner.getObject_id(),
							Long.valueOf(dbo.getVal(Tc.LINK_TYPE_ID).toString()), false, svr);
					if (dboLink == null) {
						dboHolding = svr.getObjectById(objId1, SvReader.getTypeIdByName(Tc.HOLDING), new DateTime());
						wr.removeKeeperInfoInHolding(dboHolding);
						wr.updateHoldingStatus(dboHolding, Tc.NO_KEEPER, svr);
					}
					dboHolding = svr.getObjectById(objId1, SvReader.getTypeIdByName(Tc.HOLDING), new DateTime());
					break;
				case Tc.HOLDING_QUARANTINE:
					DbDataObject dboQuarantine = svr.getObjectById(objId2, SvReader.getTypeIdByName(Tc.QUARANTINE),
							null);
					if (dboQuarantine.getVal(Tc.QUARANTINE_TYPE) != null
							&& dboQuarantine.getVal(Tc.QUARANTINE_TYPE).toString().equals("0")) {
						DbDataArray arrExportCertificates = svr.getObjectsByParentId(objId2,
								SvReader.getTypeIdByName(Tc.EXPORT_CERT), null, 0, 0);
						if (!arrExportCertificates.getItems().isEmpty()) {
							DbDataObject dboLinkBetweenHoldingAndQuarantine = rdr.getLinkObject(objId1, objId2,
									linkTypeId, false, svr);
							if (dboLinkBetweenHoldingAndQuarantine == null) {
								throw (new SvException("naits.error.holdingCannotBeDeletedIfExportCertificateExists",
										svCONST.systemUser, null, null));
							}
						}
					}
					break;
				default:
					break;
				}
				break;
			default:
				break;
			}
		} finally {
			if (svr != null)
				svr.release();
			if (svw != null)
				svw.release();
			if (svl != null)
				svl.release();
		}

	}

	public Boolean beforeSaveHoldingCheck(DbDataObject dbo, Reader rdr, SvReader svr) throws SvException {
		Boolean result = false;
		if (dbo.getObject_id().equals(0L)) {
			dbo.setStatus(Tc.NO_KEEPER);
		}
		if (dbo.getVal(Tc.DT_CREATION) != null) {
			Date dt_creation = (Date) dbo.getVal(Tc.DT_CREATION);
			java.util.Date today = Calendar.getInstance().getTime();
			if (dt_creation.after(today)) {
				throw (new SvException("naits.error.beforeSaveCheck_inputDateIsAfterTheCurrentDate", svCONST.systemUser,
						null, null));
			}
		}
		if (dbo.getVal(Tc.VILLAGE_CODE) != null) {
			String translatedVillageCode = "";
			DbDataArray codeItem = rdr.searchForDependentMunicCommunVillage(dbo.getVal(Tc.VILLAGE_CODE).toString(),
					Tc.VILLAGES, svr);
			if (!codeItem.getItems().isEmpty()) {
				String villageLabelCode = codeItem.get(0).getVal(Tc.LABEL_CODE).toString();
				translatedVillageCode = I18n.getText(Tc.GEORGIAN_LOCALE, villageLabelCode);
			}
			dbo.setVal(Tc.VILLAGE_NAME, translatedVillageCode);
		}
		if (dbo.getVal(Tc.PIC) == null) {
			throw (new SvException("naits.error.null_pic", svCONST.systemUser, null, null));
		}
		return result;
	}

	public Boolean beforeSaveLabSample(DbDataObject dbo, Reader rdr, Writer wr, ValidationChecks vc, SvReader svr,
			SvWriter svw, SvLink svl) throws SvException {
		Boolean result = false;
		String checkColum = "";
		DbDataObject labSampleDb = svr.getObjectById(dbo.getObject_id(), dbo.getObject_type(), new DateTime());
		if (dbo.getObject_id().equals(0L)) {
			// initial status
			dbo.setStatus(Tc.COLLECTED);
		}
		if (vc.checkIfDateIsInFuture(dbo, Tc.DATE_OF_COLLECTION)) {
			throw (new SvException("naits.error.collectionDateCannotBeInTheFuture", svCONST.systemUser, null, null));
		}
		if (!dbo.getObject_id().equals(0L) && dbo.getVal(Tc.ANIMAL_EAR_TAG) != null
				&& labSampleDb.getVal(Tc.ANIMAL_EAR_TAG) != null) {
			if (!dbo.getVal(Tc.ANIMAL_EAR_TAG).toString().equals(labSampleDb.getVal(Tc.ANIMAL_EAR_TAG).toString())) {
				throw (new SvException("naits.error.animalEarTagCantBeChnaged", svCONST.systemUser, null, null));
			}
		}
		// disease can't be null
		if (dbo.getVal(Tc.CHECK_COLUMN) != null) {
			checkColum = dbo.getVal(Tc.CHECK_COLUMN).toString();
		}
		if (!checkColum.equals("1")) {
			if (dbo.getVal(Tc.DISEASE_TEST) == null || dbo.getVal(Tc.DISEASE_TEST).toString().equals("")) {
				throw (new SvException("naits.error.diseaseFieldCantBeEmpty", svCONST.systemUser, null, null));
			}

			if (dbo.getVal(Tc.SAMPLE_TYPE) == null || dbo.getVal(Tc.SAMPLE_TYPE).toString().equals("")) {
				throw (new SvException("naits.error.sampleFieldCantBeEmpty", svCONST.systemUser, null, null));
			}

			if (dbo.getVal(Tc.SAMPLE_TEST_TYPE) == null || dbo.getVal(Tc.SAMPLE_TEST_TYPE).toString().equals("")) {
				throw (new SvException("naits.error.testTypeFieldCantBeEmpty", svCONST.systemUser, null, null));
			}
			// check if selected disease/s match with selected sample type
			// TODO issue #1094
			/*
			 * if (!vc.checkIfLabSampleHasValidDiseaseSampleTestCombination(dbo,
			 * rdr, svr)) { throw (new SvException(
			 * "naits.error.selectedDiseaseDoesntMatchWithSelectedSampleType",
			 * svCONST.systemUser, null, null)); }
			 */
		}
		// disable edit
		if (!dbo.getObject_id().equals(0L) && labSampleDb != null && labSampleDb.getStatus().equals(Tc.PROCESSED)
				&& dbo.getVal(Tc.TEST_RESULT_STATUS) == null) {
			if (!labSampleDb.equals(dbo)) {
				throw (new SvException("naits.error.labSampleWithStatusReceivedCantBeEdited", svCONST.systemUser, null,
						null));
			}
		}

		if (!dbo.getParent_id().equals(0L)) {
			DbDataObject animalOrHoldingObj = svr.getObjectById(dbo.getParent_id(), SvReader.getTypeIdByName(Tc.ANIMAL),
					null);
			if (animalOrHoldingObj == null) {
				// parent is holding
				animalOrHoldingObj = svr.getObjectById(dbo.getParent_id(), SvReader.getTypeIdByName(Tc.HOLDING), null);
				DbDataObject objHoldingKeeper = rdr.getHoldingOwner(animalOrHoldingObj.getObject_id(), svr);
				if (dbo.getObject_id().equals(0L)) {
					dbo.setVal(Tc.HOLDING_PIC, animalOrHoldingObj.getVal(Tc.PIC).toString());
					String sampleId = generateSampleId(animalOrHoldingObj.getVal(Tc.PIC).toString(), "", svr);
					dbo.setVal(Tc.SAMPLE_ID, sampleId);
					dbo.setVal(Tc.GEOSTAT_CODE, animalOrHoldingObj.getVal(Tc.VILLAGE_CODE).toString());
					if (objHoldingKeeper != null && objHoldingKeeper.getVal(Tc.FULL_NAME) != null) {
						dbo.setVal(Tc.HOLDING_RESP, objHoldingKeeper.getVal(Tc.FULL_NAME).toString());
					} else {
						dbo.setVal(Tc.HOLDING_RESP, "N/A");
					}
				}
				if (dbo.getVal(Tc.SAMPLE_ORIGIN) != null && dbo.getVal(Tc.SAMPLE_ORIGIN).toString().equals("1")) {
					throw (new SvException("naits.error.wrongSampleOrigin", svCONST.systemUser, null, null));
				} else if (dbo.getVal(Tc.SAMPLE_ORIGIN) != null && dbo.getVal(Tc.SAMPLE_ORIGIN).toString().equals("2")
						&& dbo.getVal(Tc.ANIMAL_EAR_TAG) != null) {
					throw (new SvException("naits.error.cantAddAnimalEarTagInHoldingOriginatedSample",
							svCONST.systemUser, null, null));
				}
			} else {
				if (dbo.getVal(Tc.SAMPLE_ORIGIN) != null && dbo.getVal(Tc.SAMPLE_ORIGIN).toString().equals("2")) {
					throw (new SvException("naits.error.wrongSampleOrigin", svCONST.systemUser, null, null));
				}
				if (dbo.getVal(Tc.DISEASE_TEST) != null) {
					DbDataArray diseasesFound = rdr.getAppropriateDiseasesByAnimal(animalOrHoldingObj, svr);
					if (!vc.checkIfMultiselectedDiseasesAreAppropriate(dbo, Tc.DISEASE_TEST, diseasesFound)) {
						throw (new SvException("naits.error.diseaseDoesntMatchWithAnimalBreedOrRace",
								svCONST.systemUser, null, null));
					}
				}
				if (dbo.getObject_id().equals(0L)) {
					DbDataObject holdingObj = svr.getObjectById(animalOrHoldingObj.getParent_id(),
							SvReader.getTypeIdByName(Tc.HOLDING), null);
					DbDataObject objHoldingKeeper = rdr.getHoldingOwner(holdingObj.getObject_id(), svr);
					String sampleId = generateSampleId(animalOrHoldingObj.getVal(Tc.ANIMAL_ID).toString(), "", svr);
					dbo.setVal(Tc.SAMPLE_ID, sampleId);
					dbo.setVal(Tc.ANIMAL_EAR_TAG, animalOrHoldingObj.getVal(Tc.ANIMAL_ID).toString());
					dbo.setVal(Tc.GEOSTAT_CODE, holdingObj.getVal(Tc.VILLAGE_CODE).toString());
					dbo.setVal(Tc.HOLDING_PIC, holdingObj.getVal(Tc.PIC).toString());
					if (objHoldingKeeper != null && objHoldingKeeper.getVal(Tc.FULL_NAME) != null) {
						dbo.setVal(Tc.HOLDING_RESP, objHoldingKeeper.getVal(Tc.FULL_NAME).toString());
					} else {
						dbo.setVal(Tc.HOLDING_RESP, "N/A");
					}
				}
			}
		}

		// for objects without parent
		if (dbo.getParent_id().equals(0L)) {
			// check if animal_id exists
			if (dbo.getVal(Tc.SAMPLE_ORIGIN) != null && dbo.getVal(Tc.SAMPLE_ORIGIN).toString().equals("2")) {
				// when adding sample without parent, can't be holding
				throw (new SvException("naits.error.cantAddSampleWithOriginHoldingWithoutParent", svCONST.systemUser,
						null, null));
			}
			if (dbo.getVal(Tc.ANIMAL_EAR_TAG) == null) {
				throw (new SvException("naits.error.pleaseEnterAnimalEarTag", svCONST.systemUser, null, null));
			}
			if (dbo.getVal(Tc.SAMPLE_ID) == null) {
				String sampleId = generateSampleId(dbo.getVal(Tc.ANIMAL_EAR_TAG).toString(), "", svr);
				dbo.setVal(Tc.SAMPLE_ID, sampleId);
			}
			if (dbo.getVal(Tc.ANIMAL_EAR_TAG) != null) {
				DbDataObject existingAnimal = rdr.searchForObject(SvReader.getTypeIdByName(Tc.ANIMAL), Tc.ANIMAL_ID,
						dbo.getVal(Tc.ANIMAL_EAR_TAG).toString(), svr);
				if (existingAnimal != null) {
					dbo.setParent_id(existingAnimal.getObject_id());
				} else {
					DbDataObject earTagInventoryObj = rdr.searchForObject(SvReader.getTypeIdByName(Tc.INVENTORY_ITEM),
							Tc.EAR_TAG_NUMBER, dbo.getVal(Tc.ANIMAL_EAR_TAG).toString(), svr);
					if (earTagInventoryObj == null) {
						throw (new SvException(
								"naits.error.enteredEarTagDoesNotExistInRegisteredAnimalsNorInventoryItem",
								svCONST.systemUser, null, null));
					} else {
						String geostatCode = rdr.getGeostatCodeFromOrgUnit(earTagInventoryObj, svr);
						if (!geostatCode.equals("")) {
							dbo.setVal(Tc.GEOSTAT_CODE, geostatCode);
						}
					}
				}
			}
		}
		return result;
	}

	public Boolean beforeSaveHoldingResponsibleCheck(DbDataObject dbo, SvReader svr) throws SvException {
		Boolean result = false;
		String firstName = dbo.getVal(Tc.FIRST_NAME) != null ? dbo.getVal(Tc.FIRST_NAME).toString() : "";
		String lastName = dbo.getVal(Tc.LAST_NAME) != null ? dbo.getVal(Tc.LAST_NAME).toString() : "";
		// first process the full name - only for new object
		if (dbo.getObject_id().equals(0L)) {
			dbo.setVal(Tc.FULL_NAME, firstName.trim() + " " + lastName.trim());
		} else {
			// make comparison for first and last name, and according the result
			// change or not the full name
			DbDataObject dboHoldingResponsible = svr.getObjectById(dbo.getObject_id(), dbo.getObject_type(),
					new DateTime());
			String oldFirstName = dboHoldingResponsible.getVal(Tc.FIRST_NAME) != null
					? dboHoldingResponsible.getVal(Tc.FIRST_NAME).toString() : "";
			String oldLastName = dboHoldingResponsible.getVal(Tc.LAST_NAME) != null
					? dboHoldingResponsible.getVal(Tc.LAST_NAME).toString() : "";
			if (!oldFirstName.equals(firstName) || !oldLastName.equals(lastName)) {
				dbo.setVal(Tc.FULL_NAME, firstName.trim() + " " + lastName.trim());
			}
		}
		// now compare the results, relevant according the holder type
		if (dbo.getVal(Tc.HOLDER_TYPE) != null) {
			if (dbo.getVal(Tc.HOLDER_TYPE).toString().equals("1")
					&& (dbo.getVal(Tc.FIRST_NAME) == null || dbo.getVal(Tc.LAST_NAME) == null)) {
				throw (new SvException("naits.error.enterFirstNameAndLastNameProperly", svCONST.systemUser, null,
						null));
			} else if (dbo.getVal(Tc.HOLDER_TYPE).toString().equals("2")) {
				if (dbo.getVal(Tc.GENDER) != null) {
					throw (new SvException("naits.error.legalEntityHoldingResponsibleCantHaveGender",
							svCONST.systemUser, null, null));
				}
				if (dbo.getVal(Tc.FULL_NAME) == null || dbo.getVal(Tc.FULL_NAME).toString().trim().length() < 2) {
					throw (new SvException("naits.error.enterAppropriateFirstOrLastNameToFulfillFullName",
							svCONST.systemUser, null, null));
				}
			}

		}

		return result;
	}

	public Boolean beforeSaveAnimal(DbDataObject dbo, Reader rdr, SvReader svr, SvWriter svw) throws SvException {
		ValidationChecks vc = new ValidationChecks();
		Boolean result = false;
		if (dbo.getVal(Tc.ANIMAL_ID) != null && dbo.getVal(Tc.ANIMAL_CLASS) != null) {
			String currIdNo = dbo.getVal(Tc.ANIMAL_ID).toString();
			if (dbo.getObject_id().equals(0L) && !NumberUtils.isDigits(currIdNo)) {
				throw (new SvException("naits.error.beforeSaveCheck_AnimalIdMustBeDigitOnly", svCONST.systemUser, null,
						null));
			}
		}
		if (dbo.getVal(Tc.BIRTH_DATE) != null) {
			Date birth_date = null;
			birth_date = (Date) new DateTime(dbo.getVal(Tc.BIRTH_DATE)).toDate();
			java.util.Date today = Calendar.getInstance().getTime();
			Calendar birth_date_min = Calendar.getInstance();
			birth_date_min.setTime(today);
			birth_date_min.add(Calendar.YEAR, -30);
			Calendar birthDate = Calendar.getInstance();
			birthDate.setTime(birth_date);

			if (birth_date.after(today)) {
				throw (new SvException("naits.error.beforeSaveCheck_birthDateCanNotBeAfterCurrentDate",
						svCONST.systemUser, null, null));
			}
			// log4j.info((birthDate.compareTo(birth_date_min)));
			if ((birthDate.compareTo(birth_date_min)) < 0) {
				throw (new SvException("naits.error.beforeSaveCheck_birthDateCanNotBeMoreThan30YearsBefore",
						svCONST.systemUser, null, null));
			}
		}
		// if new record
		if (dbo.getVal(Tc.REGISTRATION_DATE) != null) {
			Date register_date = (Date) new DateTime(dbo.getVal(Tc.REGISTRATION_DATE)).toDate();
			java.util.Date today = Calendar.getInstance().getTime();
			if (register_date.after(today)) {
				throw (new SvException("naits.error.beforeSaveCheck_registrationDateIsAfterTheCurrentDate",
						svCONST.systemUser, null, null));
			}
			Calendar register_date_min = Calendar.getInstance();
			register_date_min.setTime(today);
			register_date_min.add(Calendar.YEAR, -30);
			Calendar register_dateC = Calendar.getInstance();
			register_dateC.setTime(register_date);
			if ((register_dateC.compareTo(register_date_min)) < 0) {
				throw (new SvException("naits.error.beforeSaveCheck_regDateCanNotBeMoreThan30YearsBefore",
						svCONST.systemUser, null, null));
			}
		}

		// set of animal validation checks
		vc.animalOnSaveValidationSet(dbo, rdr, svr, svw);

		return result;
	}

	public static String calcCheckDigit(String digStr) {
		int len = digStr.length();
		int sum = 0, rem = 0;
		for (int k = 1; k <= len; k++) // compute weighted sum
			sum += (11 - k) * Character.getNumericValue(digStr.charAt(k - 1));
		if ((rem = sum % 11) == 0)
			return "0";
		else if (rem == 1)
			return "X";
		return (new Integer(11 - rem)).toString();
	}

	/**
	 * Method for generating Sample ID with sequence
	 * 
	 * @param animalOrHoldingId
	 *            animal ear tag or holding PIC
	 * @param dateOfCollection
	 *            date of collection
	 * @param svr
	 *            SvReader instance
	 * @return
	 */
	public String generateSampleId(String animalOrHoldingId, String dateOfCollection, SvReader svr) {
		SvSequence svs = null;
		String animalSampledSeq = "";
		String generateLabSampleId = "";
		try {
			svs = new SvSequence(svr.getSessionId());
			// String concDate = dateOfCollection.replaceAll("-",
			// "").substring(1);
			Long seqId = svs.getSeqNextVal(animalOrHoldingId, false);
			Thread.sleep(2);
			animalSampledSeq = String.format("%01d", Integer.valueOf(seqId.toString()));
			generateLabSampleId = animalOrHoldingId + "-" + animalSampledSeq;
			svs.dbCommit();
		} catch (SvException | InterruptedException e) {
			log4j.error(e);
		} finally {
			if (svs != null) {
				svs.release();
			}
		}
		return generateLabSampleId;
	}

	public void generatePicPerHolding(DbDataObject dbo, SvCore parentCore) {
		SvSequence svs = null;
		if (parentCore != null) {
			try {
				svs = new SvSequence(parentCore.getSessionId());
				String villageCode = "";
				String holdingSeq = "";
				if (dbo.getVal(Tc.VILLAGE_CODE) != null) {
					villageCode = dbo.getVal(Tc.VILLAGE_CODE).toString();
				}
				ReentrantLock lock = null;
				Long seqId = svs.getSeqNextVal(villageCode, false);
				try {
					lock = SvLock.getLock(String.valueOf(seqId), false, 0);
					if (lock == null) {
						throw (new SvException("naits.error.objectUsedByOtherSession", parentCore.getInstanceUser()));
					}
					holdingSeq = String.format("%04d", Integer.valueOf(seqId.toString()));
					String checkDigit = calcCheckDigit(villageCode + holdingSeq);
					String generatedHic = villageCode + holdingSeq + checkDigit;
					dbo.setVal(Tc.PIC, generatedHic);
					svs.dbCommit();
				} finally {
					if (lock != null) {
						SvLock.releaseLock(String.valueOf(seqId), lock);
					}
				}
			} catch (SvException e) {
				log4j.error(e);
			} finally {
				if (svs != null)
					svs.release();
			}
		}
	}

	public void autoSetSdiUnitsAccordingVillageCode(DbDataObject dbo) {
		String villageFieldName = Tc.VILLAGE_CODE;
		String municFieldName = Tc.MUNIC_CODE;
		String regionFieldName = Tc.REGION_CODE;
		String communFieldName = Tc.COMMUN_CODE;

		String villageCode = "";
		if (dbo.getVal(villageFieldName) != null) {
			villageCode = dbo.getVal(villageFieldName).toString();
		}
		if (!villageCode.equals("") && villageCode.length() == 8) {
			dbo.setVal(communFieldName, villageCode.substring(0, 6));
		}
		if (!villageCode.equals("") && villageCode.length() == 8) {
			dbo.setVal(municFieldName, villageCode.substring(0, 4));
		}
		if (!villageCode.equals("") && villageCode.length() == 8) {
			dbo.setVal(regionFieldName, villageCode.substring(0, 2));
		}
	}

	public void invalidateLinkBetweenExistingKeeperAndHolding(Long holdingKeeperObjId, SvCore parentCore) {
		SvReader svr = null;
		try {
			svr = new SvReader(parentCore);
			Reader rdr = new Reader();
			Writer wr = new Writer();
			DbDataObject existingLink = null;
			DbDataObject dboHoldingKeeper;

			dboHoldingKeeper = svr.getObjectById(holdingKeeperObjId, SvReader.getTypeIdByName(Tc.HOLDING_RESPONSIBLE),
					null);
			if (dboHoldingKeeper != null) {
				existingLink = rdr.findLinkBetweenKeeperAndHolding(dboHoldingKeeper, Tc.HOLDING_KEEPER, svr);
				if (existingLink != null) {
					wr.invalidateLink(existingLink, svr);
				}
			}
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage(), e);
		} finally {
			if (svr != null)
				svr.release();
		}

	}

}