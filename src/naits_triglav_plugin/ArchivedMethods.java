package naits_triglav_plugin;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Pattern;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.prtech.svarog.SvCore;
import com.prtech.svarog.SvException;
import com.prtech.svarog.SvReader;
import com.prtech.svarog.svCONST;
import com.prtech.svarog_common.DbDataArray;
import com.prtech.svarog_common.DbDataObject;
import com.prtech.svarog_common.DbSearchCriterion;
import com.prtech.svarog_common.DbSearchCriterion.DbCompareOperand;
import com.prtech.svarog_common.DbSearchExpression;

public class ArchivedMethods {
	
	public Boolean checkIfWrongEnteredEarTagExists(DbDataObject animalObj, SvReader svr) throws SvException {
		Boolean result = false;
		DbDataArray replacedEarTags = svr.getObjectsByParentId(animalObj.getObject_id(),
				SvReader.getTypeIdByName(Tc.EAR_TAG_REPLC), null, 0, 0);
		if (!replacedEarTags.getItems().isEmpty()) {
			DbDataObject dboLastReplacedEarTag = replacedEarTags.get(replacedEarTags.size() - 1);
			if (dboLastReplacedEarTag.getVal(Tc.REASON) != null
					&& dboLastReplacedEarTag.getVal(Tc.REASON).toString().equals(Tc.WRONG_ENTRY)) {
				result = true;
			}
		}
		return result;
	}
	
	public Boolean checkIfSourceHoldingHasPositiveHealthStatus(DbDataObject movementObj, SvReader svr)
			throws SvException {
		Reader rdr = new Reader();
		Boolean result = true;
		String healthStatus = null;
		String sourceHolding = null;
		DbDataObject holding = null;
		if (movementObj.getVal(Tc.SOURCE_HOLDING_ID) != null) {
			sourceHolding = movementObj.getVal(Tc.SOURCE_HOLDING_ID).toString();
			holding = rdr.findAppropriateHoldingByPic(sourceHolding, svr);
			if (holding != null) {
				healthStatus = rdr.getHoldingHealthStatus(holding, svr);
				if (healthStatus.length() > 0) {
					if (!healthStatus.equals("en.positive")) {
						result = false;
					}
				}
			}
		}
		return result;
	}
	
	public Boolean checkIfDestinationHoldingHasPositiveHealthStatus(DbDataObject movementObj, SvReader svr)
			throws SvException {
		Reader rdr = new Reader();
		Boolean result = true;
		String healthStatus = null;
		String destinationHolding = null;
		DbDataObject holding = null;
		if (movementObj.getVal(Tc.DESTINATION_HOLDING_ID) != null) {
			destinationHolding = movementObj.getVal(Tc.DESTINATION_HOLDING_ID).toString();
			holding = rdr.findAppropriateHoldingByPic(destinationHolding, svr);
			if (holding != null) {
				healthStatus = rdr.getHoldingHealthStatus(holding, svr);
				if (healthStatus.length() > 0) {
					if (!healthStatus.equals("en.positive")) {
						result = false;
					}
				}
			}
		}
		return result;
	}
	
	/**
	 * 
	 * @param animalMovementObj
	 * @param daysNum
	 * @param svr
	 * @return
	 * @throws SvException
	 */
	public Boolean checkIfAnimalIsVaccinatedNDaysBeforeMovement(DbDataObject animalMovementObj, Integer daysNum,
			SvReader svr) throws SvException {
		Reader rdr = new Reader();
		DbDataObject animalObj = svr.getObjectById(animalMovementObj.getParent_id(),
				SvReader.getTypeIdByName(Tc.ANIMAL), null);
		DbDataArray vaccRecords = rdr.getLinkedVaccinationBooksPerAnimalOrFlock(animalObj, svr);
		DateTimeFormatter dtf = DateTimeFormat.forPattern(Tc.DATE_PATTERN);
		DbDataObject lastRecord = null;
		DateTime dt_departure = null;
		DateTime dt_vacc = null;
		Boolean result = false;
		if (animalMovementObj.getVal(Tc.DEPARTURE_DATE) != null) {
			dt_departure = dtf.parseDateTime(animalMovementObj.getVal(Tc.DEPARTURE_DATE).toString());
			if (vaccRecords != null && !vaccRecords.getItems().isEmpty()) {
				vaccRecords.getSortedItems(Tc.VACC_DATE);
				lastRecord = vaccRecords.get(vaccRecords.size() - 1);
				if (lastRecord != null && lastRecord.getVal(Tc.VACC_DATE) != null) {
					dt_vacc = dtf.parseDateTime(lastRecord.getVal(Tc.VACC_DATE).toString());
					Integer days = Days.daysBetween(dt_departure, dt_vacc).getDays();
					if (days >= daysNum) {
						result = true;
					}
				}
			}
		}
		return result;
	}
	
	/**
	 * Method to return health status per specific holding and disease,
	 * according Area Health Management Tool
	 * 
	 * @param holdingPic
	 *            - pic of the holding
	 * @param disease
	 *            - code of disease (available codes: FMR, ANTRX, BRUC, TUBRC,
	 *            RAB)
	 * @param svr
	 * @return String (FREE/HIGH_RISK/LOW_RISK)
	 * @throws SvException
	 */
	public String checkHoldingHealthStatusByAreaByDisease(String holdingPic, String disease, SvReader svr)
			throws SvException {
		String holdingAreaStatus = Tc.FREE;
		Reader rdr = new Reader();
		DbDataObject holding = rdr.findAppropriateHoldingByPic(holdingPic, svr);
		if (holding != null && holding.getVal(Tc.VILLAGE_CODE) != null) {
			DbDataObject areaObj = rdr.findAppropriateAreaByCode(holding.getVal(Tc.VILLAGE_CODE).toString(), "3", svr);
			DbDataArray areaHealthStatuses = svr.getObjectsByParentId(areaObj.getObject_id(),
					SvReader.getTypeIdByName(Tc.AREA_HEALTH), null, 0, 0);
			if (!areaHealthStatuses.getItems().isEmpty()) {
				for (DbDataObject tempAreaHealthStatus : areaHealthStatuses.getItems()) {
					String tempDiseaseCode = tempAreaHealthStatus.getVal(Tc.DISEASE_ID).toString();
					if (tempDiseaseCode.equals(disease)) {
						String tempAreaStatus = tempAreaHealthStatus.getVal(Tc.AREA_STATUS).toString();
						if (tempAreaStatus.equals("0") || tempAreaStatus.equals("1")) {
							holdingAreaStatus = tempAreaStatus;
							break;
						}
					}
				}
			}
			if (!holdingAreaStatus.equals(Tc.FREE) && holdingAreaStatus.equals("0")) {
				holdingAreaStatus = Tc.HIGH_RISK;
			}
			if (!holdingAreaStatus.equals(Tc.FREE) && holdingAreaStatus.equals("1")) {
				holdingAreaStatus = Tc.LOW_RISK;
			}
		}
		return holdingAreaStatus;
	}
	
	/**
	 * Method to return message if movement of the animal between two holdings
	 * is allowed according the holding's area health status for any disease
	 * 
	 * @param dboAnimalMovement
	 *            - DbDataObject of the movement
	 * @param svr
	 * @return String (allowed/destHoldHighRisk/destHoldingLowRisk/
	 *         sourceHoldingHighRisk/sourceHoldingLowRisk)
	 * @throws SvException
	 */
	public String checkIfMovementValidAccordingHoldingsHealthStatus(DbDataObject dboAnimalMovement, SvReader svr)
			throws SvException {
		ValidationChecks vc = new ValidationChecks();
		String movementValidationAccordingHoldingHealthStatus = "naits.system.movement_allowed";
		// first check source holding
		if (dboAnimalMovement.getVal(Tc.SOURCE_HOLDING_ID) != null) {
			String sourceHoldingHealthStatus = vc.checkHoldingHealthStatusByArea(
					dboAnimalMovement.getVal(Tc.SOURCE_HOLDING_ID).toString(), svr);
			if (!sourceHoldingHealthStatus.equals(Tc.FREE) && sourceHoldingHealthStatus.equals(Tc.HIGH_RISK)) {
				movementValidationAccordingHoldingHealthStatus = "naits.system.movemBlocked.sourceHoldingHighRisk";
			}
			if (!sourceHoldingHealthStatus.equals(Tc.FREE)
					&& movementValidationAccordingHoldingHealthStatus.equals("allowed")
					&& sourceHoldingHealthStatus.equals(Tc.LOW_RISK)) {
				movementValidationAccordingHoldingHealthStatus = "naits.system.movemBlocked.sourceHoldingLowRisk";
			}
		}
		// then check destination holding, only if the source holding was with
		// FREE health status
		if (dboAnimalMovement.getVal(Tc.DESTINATION_HOLDING_ID) != null) {
			String destintionHoldingHealthStatus = vc.checkHoldingHealthStatusByArea(
					dboAnimalMovement.getVal(Tc.DESTINATION_HOLDING_ID).toString(), svr);
			if (!destintionHoldingHealthStatus.equals(Tc.FREE) && destintionHoldingHealthStatus.equals(Tc.HIGH_RISK)) {
				movementValidationAccordingHoldingHealthStatus = "naits.system.movemBlocked.destHoldingHighRisk";
			}
			if (!destintionHoldingHealthStatus.equals(Tc.FREE) && destintionHoldingHealthStatus.equals("allowed")
					&& destintionHoldingHealthStatus.equals(Tc.LOW_RISK)) {
				movementValidationAccordingHoldingHealthStatus = "naits.system.movemBlocked.destHoldingLowRisk";
			}
		}

		return movementValidationAccordingHoldingHealthStatus;
	}
	
	public Boolean checkIfAnimalOrFlockBelongsToReleasedtMovementDoc(DbDataObject animalOrFlockObj, Reader rdr,
			SvReader svr) throws SvException {
		Boolean result = false;
		DbDataArray animalOrFlockMovementsObj = null;
		DbDataObject movementObj = null;
		if (animalOrFlockObj.getObject_type().equals(SvReader.getTypeIdByName(Tc.ANIMAL))) {
			animalOrFlockMovementsObj = svr.getObjectsByParentId(animalOrFlockObj.getObject_id(),
					SvReader.getTypeIdByName(Tc.ANIMAL_MOVEMENT), null, 0, 0);
		} else {
			animalOrFlockMovementsObj = svr.getObjectsByParentId(animalOrFlockObj.getObject_id(),
					SvReader.getTypeIdByName(Tc.FLOCK_MOVEMENT), null, 0, 0);
		}
		if (animalOrFlockMovementsObj != null && !animalOrFlockMovementsObj.getItems().isEmpty()) {
			if (animalOrFlockMovementsObj.get(0).getStatus().equals(Tc.VALID)) {
				movementObj = animalOrFlockMovementsObj.get(0);
				if (movementObj.getVal(Tc.MOVEMENT_DOC_ID) != null) {
					DbDataObject tempMovementDoc = rdr.searchForObject(SvReader.getTypeIdByName(Tc.MOVEMENT_DOC),
							Tc.MOVEMENT_DOC_ID, movementObj.getVal(Tc.MOVEMENT_DOC_ID).toString(), svr);
					if ((tempMovementDoc != null && tempMovementDoc.getStatus().equals(Tc.RELEASED))
							|| ((tempMovementDoc == null))) {
						result = true;
					}
				}
			}
		}
		return result;
	}
	
	/**
	 * Method that checks if ANIMAL or FLOCK has status SLAUGHTRD so we can
	 * create post-mortem form.
	 * 
	 * @param dboPostSlaugh
	 * @param svr
	 * @return
	 * @throws SvException
	 */
	public Boolean checkIfAnimalOrFlockHaveStatusSlaught(DbDataObject dboAnimalOrFlock, SvReader svr)
			throws SvException {
		Boolean result = false;
		if (dboAnimalOrFlock != null && dboAnimalOrFlock.getStatus() != null
				&& dboAnimalOrFlock.getStatus().equals(Tc.SLAUGHTRD)) {
			result = true;
		}
		return result;
	}
	
	/**
	 * 
	 * @param dbo
	 * @param rdr
	 * @param parentCore
	 * @return
	 * @throws SvException
	 */
	public Boolean checkIfOtherAnimalCanBeSaved(DbDataObject dbo, Reader rdr, SvCore parentCore) throws SvException {
		Boolean result = false;
		if (dbo.getVal(Tc.NOTE) != null) {
			String note = dbo.getVal(Tc.NOTE).toString();
			if (!Pattern.matches("[a-zA-Z0-9]*", note)) {
				throw (new SvException("naits.error.beforeSaveCheck_onlyAlphabeticCharsAllowed", svCONST.systemUser,
						null, null));
			}
		}
		result = true;
		return result;
	}
	
	/**
	 * Method that checks if twin animal is having appropriate father
	 * 
	 * @param dboAnimal
	 *            ANIMAL object
	 * @param svr
	 *            SvReader instance
	 * @return
	 * @throws SvException
	 */
	public Boolean checkIfTwinAnimalHasAppropriateFather(DbDataObject dboAnimal, SvReader svr) throws SvException {
		Boolean result = true;
		Reader rdr = new Reader();
		ValidationChecks vc = new ValidationChecks();
		DbDataObject twinAnimal = null;
		DbDataArray animalSibilings = rdr.getAnimalSiblingsAccordingMotherTagId(dboAnimal, svr);
		if (animalSibilings != null && !animalSibilings.getItems().isEmpty()) {
			if (vc.checkIfAnimalIsTwin(dboAnimal, svr)) {
				twinAnimal = animalSibilings.get(0);
				if (dboAnimal.getVal(Tc.FATHER_TAG_ID) != null && twinAnimal.getVal(Tc.FATHER_TAG_ID) != null
						&& !dboAnimal.getVal(Tc.FATHER_TAG_ID).toString()
								.equals(twinAnimal.getVal(Tc.FATHER_TAG_ID).toString())) {
					result = false;
				}
			}
		}
		return result;
	}
	
	/**
	 * 
	 * @param dboSpotCheck
	 * @param svr
	 * @return
	 */
	public Boolean checkIfNumberOfMissingTagsIsNotLargerThanNumberOfTags(DbDataObject dboSpotCheck) {
		Boolean result = true;
		Integer missingTags = null;
		Integer numTags = null;
		if (dboSpotCheck.getVal(Tc.MISSING_TAGS) != null && dboSpotCheck.getVal(Tc.NUM_TAGS) != null) {
			missingTags = Integer.parseInt(dboSpotCheck.getVal(Tc.MISSING_TAGS).toString());
			numTags = Integer.parseInt(dboSpotCheck.getVal(Tc.NUM_TAGS).toString());
			if (missingTags > numTags) {
				result = false;
			}
		}
		return result;
	}
	
	/**
	 * Method that checks if SPOT_CHECK is edited and if it's date of
	 * registration is changed
	 * 
	 * @param dboSpotCheck
	 *            SPOT_CHECK object
	 * @param svr
	 *            SvReader instance
	 * @return
	 * @throws SvException
	 */
	public Boolean checkIfDateOfRegistrationIsEdited(DbDataObject dboSpotCheck, SvReader svr) throws SvException {
		// this method will be in use only if READ ONLY widget on DATE_OF_REG is
		// removed
		Boolean result = false;
		DbDataObject spotCheckFromDb = svr.getObjectById(dboSpotCheck.getObject_id(),
				SvReader.getTypeIdByName(Tc.SPOT_CHECK), null);
		if (!dboSpotCheck.getVal(Tc.DATE_OF_REG).equals(spotCheckFromDb.getVal(Tc.DATE_OF_REG))) {
			result = true;
		}
		return result;
	}
	
	public Boolean checkIfLabSampleHasValidDiseaseSampleTestCombination(DbDataObject dboLabSample, Reader rdr,
			SvReader svr) throws SvException {
		Boolean result = true;
		DbDataArray testTypes = null;
		List<String> diseases = null;
		if (dboLabSample != null && dboLabSample.getVal(Tc.DISEASE_TEST) != null
				&& dboLabSample.getVal(Tc.SAMPLE_TYPE) != null && dboLabSample.getVal(Tc.SAMPLE_TEST_TYPE) != null) {
			diseases = rdr.getMultiSelectFieldValueAsList(dboLabSample, Tc.DISEASE_TEST);
			if (diseases != null && !diseases.isEmpty()) {
				for (String disease : diseases) {
					LinkedHashMap<String, String> criterias = new LinkedHashMap<>();
					criterias.put(Tc.DISEASE, disease);
					criterias.put(Tc.TEST_TYPE, dboLabSample.getVal(Tc.SAMPLE_TEST_TYPE).toString());
					criterias.put(Tc.SAMPLE_TYPE, dboLabSample.getVal(Tc.SAMPLE_TYPE).toString());
					testTypes = rdr.getDbDataWithCriteria(criterias, SvReader.getTypeIdByName(Tc.LAB_TEST_TYPE), svr);
					if (testTypes == null || testTypes.size() < 1) {
						result = false;
						break;
					}
				}
			}
		}
		return result;
	}
	
	public String checkIfMovementDocumentShouldBeReleasedOrCanceled(DbDataObject dboAnimalOrFlockMovementObj,
			Reader rdr, String destinationHoldingPic, String status, SvReader svr) throws SvException {
		String result = Tc.RELEASED;
		int counterCanceled = 0;
		int counterFinish = 0;
		Integer sum = 0;
		DbDataArray animalOrFlockMovementsArr = null;
		if (dboAnimalOrFlockMovementObj != null && dboAnimalOrFlockMovementObj.getVal(Tc.MOVEMENT_DOC_ID) != null) {
			animalOrFlockMovementsArr = rdr.getAnimalMovementsByMovementDocId(
					dboAnimalOrFlockMovementObj.getVal(Tc.MOVEMENT_DOC_ID).toString(), svr);
			if (animalOrFlockMovementsArr != null && !animalOrFlockMovementsArr.getItems().isEmpty()) {
				for (DbDataObject animalOrFlockMovementObj : animalOrFlockMovementsArr.getItems()) {
					if (animalOrFlockMovementObj != null
							&& (animalOrFlockMovementObj.getStatus().equals(Tc.CANCELED))) {
						counterCanceled++;
					} else if (animalOrFlockMovementObj != null
							&& (animalOrFlockMovementObj.getStatus().equals(Tc.FINISHED))) {
						counterFinish++;
					}
				}
				sum = counterFinish + counterCanceled;
				if (sum.equals(animalOrFlockMovementsArr.size())) {

				}
			}
		}
		return result;
	}
	
	/**
	 * Method that contains set of validations for transfer
	 * 
	 * @param dboInitialTransfer
	 * @param rangeFrom
	 * @param rangeTo
	 * @param svr
	 * @throws SvException
	 */
	public void reverseTransferOnSaveValidationSet(DbDataObject dboInitialTransfer, Long rangeFrom, Long rangeTo,
			SvReader svr) throws SvException {
		if (dboInitialTransfer.getVal(Tc.START_TAG_ID) != null && dboInitialTransfer.getVal(Tc.END_TAG_ID) != null) {
			Long startTagId = Long.valueOf(dboInitialTransfer.getVal(Tc.START_TAG_ID).toString());
			Long endTagId = Long.valueOf(dboInitialTransfer.getVal(Tc.END_TAG_ID).toString());
			if (rangeFrom < startTagId) {
				throw (new SvException("naits.error.rangeFromCannotBeLessThanInitialStartTagId",
						svr.getInstanceUser()));
			} else if (rangeFrom > endTagId) {
				throw (new SvException("naits.error.rangeFromCannotBeLargerThanInitialEndTagId",
						svr.getInstanceUser()));
			} else if (rangeTo > endTagId) {
				throw (new SvException("naits.error.rangeToCannotBeLessThanInitialEndTagId", svr.getInstanceUser()));
			} else if (rangeTo < startTagId) {
				throw (new SvException("naits.error.rangeToCannotBeLargerThanInitialStartTagId",
						svr.getInstanceUser()));
			}
		}
	}
	
	public boolean checkIfRfidInputStateAllowedToBeProcessedInMassAction(DbDataObject dboRfidInputState,
			String actionType) {
		boolean result = true;
		if (!actionType.equalsIgnoreCase(Tc.REGISTRATION) && (dboRfidInputState.getStatus().equals(Tc.NONAPPLIED)
				|| dboRfidInputState.getStatus().equals(Tc.NOT_FOUND))) {
			result = false;
		}
		return result;
	}
	
	/**
	 * Method that checks if animal has PRE_SLAUGHT_FORM.
	 * 
	 * @param dboAnimal
	 *            ANIMAL object
	 * @param svr
	 *            SvReader instance
	 * @return true/false
	 * @throws SvException
	 */
	public Boolean checkIfPreMortemCanBeGenerated(DbDataObject dboAnimal, SvReader svr) throws SvException {
		Boolean result = false;
		DbDataArray preMortemsForAnimalOrFlock = null;
		if (dboAnimal != null) {
			preMortemsForAnimalOrFlock = svr.getObjectsByParentId(dboAnimal.getObject_id(),
					SvReader.getTypeIdByName(Tc.PRE_SLAUGHT_FORM), null, 0, 0);
			if (preMortemsForAnimalOrFlock == null || preMortemsForAnimalOrFlock.size() < 1) {
				result = true;
			}
		}
		return result;
	}
	
	public Long getSvFormPerSvFormType(Long formTypeId, SvReader svr) throws SvException {
		Long svFormObjId = null;
		DbSearchCriterion cr1 = new DbSearchCriterion(Tc.FORM_TYPE_ID, DbCompareOperand.EQUAL, formTypeId);
		DbDataArray res = svr.getObjects(new DbSearchExpression().addDbSearchItem(cr1),
				SvReader.getTypeIdByName(Tc.SVAROG_FORM), null, 0, 0);
		if (res != null && !res.getItems().isEmpty()) {
			svFormObjId = res.get(0).getObject_id();
		}
		return svFormObjId;
	}

}
