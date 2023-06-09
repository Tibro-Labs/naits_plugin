package naits_triglav_plugin;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.math.NumberUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

import com.prtech.svarog.SvConf;
import com.prtech.svarog.SvException;
import com.prtech.svarog.SvGeometry;
import com.prtech.svarog.SvLink;
import com.prtech.svarog.SvReader;
import com.prtech.svarog.SvWriter;
import com.prtech.svarog.svCONST;
import com.prtech.svarog_common.DbDataArray;
import com.prtech.svarog_common.DbDataObject;
import com.prtech.svarog_common.DbSearchCriterion;
import com.prtech.svarog_common.DbSearchCriterion.DbCompareOperand;
import com.prtech.svarog_common.DbSearchExpression;
import com.prtech.svarog_common.SvCharId;

public class ValidationChecks {

	static final Logger log4j = LogManager.getLogger(ValidationChecks.class.getName());

	/**
	 * Method that checks if animal id already exist
	 * 
	 * @param animal
	 * @param svr
	 * @return
	 * @throws SvException
	 */
	public Boolean checkIfAnimalIdAlredyExist(DbDataObject animal, SvReader svr) throws SvException {
		Boolean result = true;
		Reader r = new Reader();
		// rdr instance of this service can be additionally completed,
		// depending on the client requirements
		if (!checkIfAnimalEarTagIsReplaced(animal, svr)) {
			if (!animal.getObject_id().equals(0L) && animal.getVal(Tc.ANIMAL_ID) != null) {
				DbDataObject dboAnimalOldVersion = r.getLastAnimalIdVersion(animal.getObject_id(), svr);
				if (!dboAnimalOldVersion.getVal(Tc.ANIMAL_ID).equals(animal.getVal(Tc.ANIMAL_ID))) {
					result = false;
				}
			}
		}
		return result;
	}

	public Boolean checkIfAnimalEarTagIsReplaced(DbDataObject animalObj, SvReader svr) throws SvException {
		Boolean result = false;
		DbDataArray replacedEarTags = svr.getObjectsByParentId(animalObj.getObject_id(),
				SvReader.getTypeIdByName(Tc.EAR_TAG_REPLC), null, 0, 0);
		replacedEarTags.getSortedItems(Tc.DT_INSERT, true);
		if (replacedEarTags != null && !replacedEarTags.getItems().isEmpty()) {
			DbDataObject tempEarTagReplc = replacedEarTags.get(0);
			if (tempEarTagReplc != null && tempEarTagReplc.getVal(Tc.OLD_EAR_TAG) != null && !tempEarTagReplc
					.getVal(Tc.OLD_EAR_TAG).toString().equals(animalObj.getVal(Tc.ANIMAL_ID).toString())) {
				result = true;
			}
		}
		return result;
	}

	/**
	 * Method that checks if period of newborn is appropriate
	 * 
	 * @param animal
	 * @param svr
	 * @return
	 * @throws SvException
	 */
	public Boolean checkIfPeriodBetweenNewbornsIsAppropriate(DbDataObject animal, SvReader svr) throws SvException {
		Boolean result = true;
		Reader rdr = null;
		DbDataObject lastBornChild = null;
		Integer differenceDays = 0;
		if (animal.getVal(Tc.MOTHER_TAG_ID) != null && !animal.getVal(Tc.MOTHER_TAG_ID).toString().trim().equals("")) {
			rdr = new Reader();
			DbDataArray childs = rdr.getAnimalSiblingsAccordingMotherTagId(animal, svr);
			if (childs != null && !childs.getItems().isEmpty()) {
				if (animal.getObject_id().equals(0L)) {
					lastBornChild = childs.get(childs.size() - 1);
				} else {
					int counter = 0;
					for (DbDataObject dboAnimal : childs.getItems()) {
						counter++;
						if (dboAnimal.getObject_id().equals(animal.getObject_id()) && childs.size() > 1) {
							if (!dboAnimal.equals(childs.get(0))) {
								lastBornChild = childs.get(counter - 2);
								break;
							} else {
								lastBornChild = childs.get(counter);
								break;
							}
						}
					}
				}
				if (lastBornChild != null && animal.getVal(Tc.BIRTH_DATE) != null
						&& lastBornChild.getVal(Tc.BIRTH_DATE) != null) {
					differenceDays = getDaysBetweenTwoDates(animal, lastBornChild);
					Integer absoluteValue = rdr.toAbsoluteValue(differenceDays);
					if (absoluteValue > 1 && absoluteValue < 200) {
						result = false;
					}
				}
			}
		}
		return result;
	}

	/**
	 * Method that checks if animals mother tag id exists
	 * 
	 * @param animal
	 * @param svr
	 * @return
	 * @throws SvException
	 */
	public Boolean checkIfAnimalMotherTagIdExist(DbDataObject animal, SvReader svr) throws SvException {
		Boolean result = true;
		Reader rdr = new Reader();
		DbDataObject animalMotherObj = null;
		if (animal.getVal(Tc.MOTHER_TAG_ID) != null && !animal.getVal(Tc.MOTHER_TAG_ID).toString().trim().equals("")
				&& animal.getVal(Tc.ANIMAL_CLASS) != null
				&& !animal.getVal(Tc.ANIMAL_CLASS).toString().trim().equals("")) {
			animalMotherObj = rdr.findAppropriateAnimalByAnimalIdAndAnimalClass(
					animal.getVal(Tc.MOTHER_TAG_ID).toString(), animal.getVal(Tc.ANIMAL_CLASS).toString(), true, svr);
			if (animalMotherObj == null) {
				result = false;
			}
		}
		return result;
	}

	/**
	 * Method that checks if animals father tag id exists
	 * 
	 * @param animal
	 * @param svr
	 * @return
	 * @throws SvException
	 */
	public Boolean checkIfAnimalFatherTagIdExist(DbDataObject animal, SvReader svr) throws SvException {
		Boolean result = true;
		Reader rdr = new Reader();
		DbDataObject animalFatherObj = null;
		if (animal.getVal(Tc.FATHER_TAG_ID) != null && !animal.getVal(Tc.FATHER_TAG_ID).toString().trim().equals("")
				&& animal.getVal(Tc.ANIMAL_CLASS) != null
				&& !animal.getVal(Tc.ANIMAL_CLASS).toString().trim().equals("")) {
			animalFatherObj = rdr.findAppropriateAnimalByAnimalIdAndAnimalClass(
					animal.getVal(Tc.FATHER_TAG_ID).toString(), animal.getVal(Tc.ANIMAL_CLASS).toString(), true, svr);
			if (animalFatherObj == null) {
				result = false;
			}
		}
		return result;
	}

	/**
	 * Method that checks if the animal is older than 14 months
	 * 
	 * @param animal
	 * @param svr
	 * @return
	 * @throws SvException
	 */
	public Boolean checkIfAnimalMotherAgeIsAppropriate(DbDataObject animal, SvReader svr) throws SvException {
		Boolean result = true;
		Reader rdr = new Reader();
		DbDataObject animalMotherObj = null;
		if (animal.getVal(Tc.MOTHER_TAG_ID) != null && !animal.getVal(Tc.MOTHER_TAG_ID).toString().trim().equals("")
				&& animal.getVal(Tc.ANIMAL_CLASS) != null
				&& !animal.getVal(Tc.ANIMAL_CLASS).toString().trim().equals("")
				&& animal.getVal(Tc.BIRTH_DATE) != null) {
			animalMotherObj = rdr.findAppropriateAnimalByAnimalIdAndAnimalClass(
					animal.getVal(Tc.MOTHER_TAG_ID).toString(), animal.getVal(Tc.ANIMAL_CLASS).toString(), true, svr);
			if (animalMotherObj != null && animalMotherObj.getVal(Tc.BIRTH_DATE) != null) {
				Integer getAgeInMonths = rdr.getPeriodOfMonthsBetweenTwoDates(
						animalMotherObj.getVal(Tc.BIRTH_DATE).toString(), animal.getVal(Tc.BIRTH_DATE).toString());
				if (getAgeInMonths < 14) {
					result = false;
				}
			}
		}
		return result;
	}

	public boolean checkIfAnimalMotherOrFatherAgeIsAppropriate(DbDataObject dboAnimal, String fatherOrMotherTagId,
			SvReader svr) throws SvException {
		Boolean result = true;
		Reader rdr = new Reader();
		DbDataObject dboAnimalFather = null;
		if (dboAnimal.getVal(Tc.ANIMAL_CLASS) != null && !dboAnimal.getVal(Tc.ANIMAL_CLASS).toString().trim().equals("")
				&& dboAnimal.getVal(Tc.BIRTH_DATE) != null) {
			dboAnimalFather = rdr.findAppropriateAnimalByAnimalIdAndAnimalClass(
					dboAnimal.getVal(fatherOrMotherTagId).toString(), dboAnimal.getVal(Tc.ANIMAL_CLASS).toString(),
					true, svr);
			if (dboAnimalFather != null && dboAnimalFather.getVal(Tc.BIRTH_DATE) != null
					&& new DateTime(dboAnimalFather.getVal(Tc.BIRTH_DATE).toString())
							.isAfter(new DateTime(dboAnimal.getVal(Tc.BIRTH_DATE).toString()))) {
				result = false;
			}
		}
		return result;
	}

	/**
	 * Method that checks if animal mother gender is valid
	 * 
	 * @param animal
	 * @param svr
	 * @return
	 * @throws SvException
	 */
	public Boolean checkIfAnimalMotherGenderIsValid(DbDataObject animal, SvReader svr) throws SvException {
		Boolean result = true;
		Reader rdr = new Reader();
		DbDataObject animalMotherObj = null;
		if (animal.getVal(Tc.MOTHER_TAG_ID) != null && !animal.getVal(Tc.MOTHER_TAG_ID).toString().trim().equals("")
				&& animal.getVal(Tc.ANIMAL_CLASS) != null
				&& !animal.getVal(Tc.ANIMAL_CLASS).toString().trim().equals("")) {
			animalMotherObj = rdr.findAppropriateAnimalByAnimalIdAndAnimalClass(
					animal.getVal(Tc.MOTHER_TAG_ID).toString(), animal.getVal(Tc.ANIMAL_CLASS).toString(), true, svr);
			if (animalMotherObj != null && animalMotherObj.getVal(Tc.GENDER) != null
					&& !animalMotherObj.getVal(Tc.GENDER).toString().equals("2")) {
				result = false;
			}
		}
		return result;
	}

	/**
	 * Method that checks if animal father gender is valid
	 * 
	 * @param animal
	 * @param svr
	 * @return
	 * @throws SvException
	 */
	public Boolean checkIfAnimalFatherGenderIsValid(DbDataObject animal, SvReader svr) throws SvException {
		Boolean result = true;
		Reader rdr = new Reader();
		DbDataObject animalFatherObj = null;
		if (animal.getVal(Tc.FATHER_TAG_ID) != null && !animal.getVal(Tc.FATHER_TAG_ID).toString().trim().equals("")
				&& animal.getVal(Tc.ANIMAL_CLASS) != null
				&& !animal.getVal(Tc.ANIMAL_CLASS).toString().trim().equals("")) {
			animalFatherObj = rdr.findAppropriateAnimalByAnimalIdAndAnimalClass(
					animal.getVal(Tc.FATHER_TAG_ID).toString(), animal.getVal(Tc.ANIMAL_CLASS).toString(), true, svr);
			if (animalFatherObj != null && animalFatherObj.getVal(Tc.GENDER) != null
					&& !animalFatherObj.getVal(Tc.GENDER).toString().equals("1")) {
				result = false;
			}
		}
		return result;
	}

	/**
	 * Method that checks if animal is not parent to its self
	 * 
	 * @param dboObjectToHandle
	 * @param svr
	 * @return
	 * @throws SvException
	 */
	public Boolean checkIfAnimalIdIsNotSelfParent(DbDataObject dboObjectToHandle, SvReader svr) throws SvException {
		Boolean result = true;
		if (dboObjectToHandle.getVal(Tc.FATHER_TAG_ID) != null
				&& !dboObjectToHandle.getVal(Tc.FATHER_TAG_ID).toString().trim().equals("") && dboObjectToHandle
						.getVal(Tc.FATHER_TAG_ID).toString().equals(dboObjectToHandle.getVal(Tc.ANIMAL_ID))) {
			result = false;
		}
		if (dboObjectToHandle.getVal(Tc.MOTHER_TAG_ID) != null
				&& !dboObjectToHandle.getVal(Tc.MOTHER_TAG_ID).toString().trim().equals("") && dboObjectToHandle
						.getVal(Tc.MOTHER_TAG_ID).toString().equals(dboObjectToHandle.getVal(Tc.ANIMAL_ID))) {
			result = false;
		}
		return result;
	}

	/**
	 * Method that checks if animals/pets mother/father has same ear_tag_id
	 * 
	 * @param animal
	 * @param svr
	 * @return
	 * @throws SvException
	 */
	public Boolean checkIfAnimalsMotherAndFatherHaveSameAnimalId(DbDataObject animal) throws SvException {
		Boolean result = false;
		if (animal.getVal(Tc.FATHER_TAG_ID) != null && animal.getVal(Tc.MOTHER_TAG_ID) != null
				&& !animal.getVal(Tc.FATHER_TAG_ID).toString().trim().equals("")
				&& animal.getVal(Tc.MOTHER_TAG_ID).toString().trim().equals("")
				&& animal.getVal(Tc.FATHER_TAG_ID).toString().equals(animal.getVal(Tc.MOTHER_TAG_ID).toString())) {
			result = true;
		}
		return result;
	}

	/**
	 * Method that checks if animals mother is from the same species
	 * 
	 * @param animal
	 * @param svr
	 * @return
	 * @throws SvException
	 */
	public Boolean checkIfAnimalsMotherIsFromSameSpecie(DbDataObject animal, SvReader svr) throws SvException {
		Boolean result = true;
		Reader rdr = new Reader();
		DbDataObject animalMotherObj = null;
		if (animal.getVal(Tc.MOTHER_TAG_ID) != null && !animal.getVal(Tc.MOTHER_TAG_ID).toString().trim().equals("")
				&& animal.getVal(Tc.ANIMAL_CLASS) != null
				&& !animal.getVal(Tc.ANIMAL_CLASS).toString().trim().equals("")) {
			animalMotherObj = rdr.findAppropriateAnimalByAnimalIdAndAnimalClass(
					animal.getVal(Tc.MOTHER_TAG_ID).toString(), animal.getVal(Tc.ANIMAL_CLASS).toString(), true, svr);
			if (animalMotherObj == null) {
				result = false;
			}
		}
		return result;
	}

	/**
	 * Method that checks if animals father is from the same species
	 * 
	 * @param animal
	 * @param svr
	 * @return
	 * @throws SvException
	 */
	public boolean checkIfAnimalsFatherIsFromSameSpecie(DbDataObject animal, SvReader svr) throws SvException {
		Boolean result = true;
		Reader rdr = new Reader();
		DbDataObject animalFatherObj = null;
		if (animal.getVal(Tc.FATHER_TAG_ID) != null && !animal.getVal(Tc.FATHER_TAG_ID).toString().trim().equals("")
				&& animal.getVal(Tc.ANIMAL_CLASS) != null
				&& !animal.getVal(Tc.ANIMAL_CLASS).toString().trim().equals("")) {
			animalFatherObj = rdr.findAppropriateAnimalByAnimalIdAndAnimalClass(
					animal.getVal(Tc.FATHER_TAG_ID).toString(), animal.getVal(Tc.ANIMAL_CLASS).toString(), true, svr);
			if (animalFatherObj == null) {
				result = false;
			}
		}
		return result;
	}

	/**
	 * Method that checks if animals registration date is after birth date
	 * 
	 * @param animal
	 * @param svr
	 * @return
	 * @throws SvException
	 */
	public Boolean checkIfAnimalRegDateIsNotAfterAnimalBirthDate(DbDataObject animal, SvReader svr) throws SvException {
		Boolean result = true;
		DateTime dt_registration = null;
		DateTime dt_birth = null;
		if (animal.getVal(Tc.REGISTRATION_DATE) != null && animal.getVal(Tc.BIRTH_DATE) != null) {
			dt_registration = new DateTime(animal.getVal(Tc.REGISTRATION_DATE).toString());
			dt_birth = new DateTime(animal.getVal(Tc.BIRTH_DATE).toString());
		}
		if (dt_registration != null && dt_birth != null) {
			if (dt_registration.isBefore(dt_birth)) {
				result = false;
			}
		}
		return result;
	}

	/**
	 * Method that checks if animals death date is not before birth date
	 * 
	 * @param animal
	 * @param svr
	 * @return
	 * @throws SvException
	 */
	public Boolean checkIfAnimalDeathDateIsNotBeforeBirthDate(DbDataObject animal, SvReader svr) throws SvException {
		Boolean result = true;
		DateTime dt_death = null;
		DateTime dt_birth = null;
		if (animal.getVal(Tc.DEATH_DATE) != null && animal.getVal(Tc.BIRTH_DATE) != null) {
			dt_death = new DateTime(animal.getVal(Tc.DEATH_DATE).toString());
			dt_birth = new DateTime(animal.getVal(Tc.BIRTH_DATE).toString());
		}
		if (dt_death != null && dt_birth != null) {
			if (dt_death.isBefore(dt_birth)) {
				result = false;
			}
		}
		return result;
	}

	/**
	 * Method that checks if departure date is not before arrival date of current
	 * animal movement
	 * 
	 * @param currentAnimalMovement
	 * @param svr
	 * @return
	 * @throws SvException
	 */
	public boolean checkIfDepartureDateIsNotBeforeLastArrivalDate(DbDataObject currentAnimalMovement, SvReader svr)
			throws SvException {
		Reader rdr = new Reader();
		DbDataObject lastAnimalMovement = rdr.getAllSorteFinisheddAnimalMovements(currentAnimalMovement, svr);
		String dt_departureCurrent = null;
		String dt_arrivalLast = null;
		Boolean result = true;
		String pattern = Tc.DATE_PATTERN;

		if (currentAnimalMovement != null && lastAnimalMovement != null
				&& currentAnimalMovement.getVal(Tc.DEPARTURE_DATE) != null
				&& lastAnimalMovement.getVal(Tc.ARRIVAL_DATE) != null) {
			dt_departureCurrent = currentAnimalMovement.getVal(Tc.DEPARTURE_DATE).toString().substring(0, 10);
			dt_arrivalLast = lastAnimalMovement.getVal(Tc.ARRIVAL_DATE).toString().substring(0, 10);
			DateTime convertedDepartureCurrent = DateTime.parse(dt_departureCurrent,
					DateTimeFormat.forPattern(pattern));
			DateTime convertedArrivalLast = DateTime.parse(dt_arrivalLast, DateTimeFormat.forPattern(pattern));

			if (convertedDepartureCurrent.isBefore(convertedArrivalLast)) {
				result = false;
			}
		}

		return result;
	}

	/**
	 * Method that checks animal movement if arrival date is not before departure
	 * date
	 * 
	 * @param dboAnimalOrFlockMovement
	 * @return
	 * @throws SvException
	 */
	public boolean checkIfDepartureDateIsBeforeArrivalDate(DbDataObject dboAnimalOrFlockMovement) throws SvException {
		boolean result = true;
		DateTime dtArrival = null;
		DateTime dtDeparture = null;
		if (dboAnimalOrFlockMovement.getVal(Tc.DEPARTURE_DATE) != null
				&& dboAnimalOrFlockMovement.getVal(Tc.ARRIVAL_DATE) != null) {
			dtArrival = new DateTime(dboAnimalOrFlockMovement.getVal(Tc.ARRIVAL_DATE).toString());
			dtDeparture = new DateTime(dboAnimalOrFlockMovement.getVal(Tc.DEPARTURE_DATE).toString());
			if (dtDeparture.isAfter(dtArrival)) {
				result = false;
			}
		}
		return result;
	}

	/**
	 * 
	 * 
	 * @param dboFlock
	 * @param svr
	 * @return
	 */
	public Boolean validateFlockEwesNumber(DbDataObject dboFlock, SvReader svr) {
		Boolean result = true;
		if (dboFlock.getVal(Tc.FEMALES) != null && dboFlock.getVal(Tc.ADULTS) != null) {
			int females = Integer.valueOf(dboFlock.getVal(Tc.FEMALES).toString());
			// # ewes
			int adult_females = Integer.valueOf(dboFlock.getVal(Tc.ADULTS).toString());
			if (adult_females > females) {
				result = false;
			}
		}
		return result;
	}

	public boolean checkIfHoldingIsSlaughterhouse(Long holdingID, SvReader svr) throws SvException {
		Boolean result = false;
		if (holdingID != null) {
			DbDataObject dboHolding = svr.getObjectById(holdingID, SvReader.getTypeIdByName(Tc.HOLDING), null);
			result = checkIfHoldingIsSlaughterhouse(dboHolding);
		}
		return result;
	}

	/**
	 * 
	 * @param dboHolding
	 * @return
	 */
	public boolean checkIfHoldingIsSlaughterhouse(DbDataObject dboHolding) {
		Boolean result = false;
		if (dboHolding != null && dboHolding.getVal(Tc.TYPE) != null
				&& dboHolding.getVal(Tc.TYPE).toString().equals("7")) {
			result = true;
		}
		return result;
	}

	/**
	 * Method that checks if animal that belongs to slaughter house has origin
	 * country GE. This method is used in onSaveValidation to check if animal is
	 * registered in slaughter house
	 * 
	 * @param dboAnimal
	 * @param svr
	 * @return
	 * @throws SvException
	 */
	public Boolean checkIfAnimalCanBeSavedToSlaughterHouseByOrigin(DbDataObject dboAnimal) throws SvException {
		Boolean result = true;
		if ((dboAnimal.getVal(Tc.COUNTRY_OLD_ID) != null
				&& dboAnimal.getVal(Tc.COUNTRY_OLD_ID).toString().equals(Tc.GE))
				|| (dboAnimal.getVal(Tc.COUNTRY) != null && dboAnimal.getVal(Tc.COUNTRY).toString().equals(Tc.GE))) {
			result = false;
		}
		return result;
	}

	/**
	 * 
	 * @param dboHolding
	 * @return
	 */
	public Boolean checkIfHoldingIsBorderInspectionPoint(DbDataObject dboHolding) {
		Boolean result = false;
		if (dboHolding != null && dboHolding.getVal(Tc.TYPE) != null
				&& dboHolding.getVal(Tc.TYPE).toString().equals("14")) {
			result = true;
		}
		return result;
	}

	/**
	 * Method that checks if holding is linked to active quarantine
	 * 
	 * @param dboHolding
	 * @param svr
	 * @throws SvException
	 */
	public Boolean checkIfHoldingBelongsInActiveQuarantine(Long dboHoldingId, SvReader svr) throws SvException {
		Boolean result = false;
		DbDataObject dboHolding = svr.getObjectById(dboHoldingId, SvReader.getTypeIdByName(Tc.HOLDING), null);
		DbDataObject dbLink = SvLink.getLinkType(Tc.HOLDING_QUARANTINE, SvReader.getTypeIdByName(Tc.HOLDING),
				SvReader.getTypeIdByName(Tc.QUARANTINE));
		DbDataArray dbaQuarantines = null;
		if (dboHolding != null && dbLink != null) {
			dbaQuarantines = svr.getObjectsByLinkedId(dboHolding.getObject_id(), dbLink, null, 0, 0);
			if (dbaQuarantines != null && !dbaQuarantines.getItems().isEmpty()) {
				for (DbDataObject tempQuarantine : dbaQuarantines.getItems()) {
					if (checkIfQuarantineActive(tempQuarantine)) {
						result = true;
						break;
					}
				}
			}
		}
		return result;
	}

	/**
	 * Method that checks if holding is linked to active SPECIFIC quarantine type
	 * quarantine
	 * 
	 * @param dboHoldingId
	 * @param quarantineType
	 * @param svr
	 * @throws SvException
	 */
	public Boolean checkIfHoldingBelongsInActiveSpecificQuarantine(Long dboHoldingId, String quarantineType,
			SvReader svr) throws SvException {
		Boolean result = false;
		DbDataObject dboHolding = svr.getObjectById(dboHoldingId, SvReader.getTypeIdByName(Tc.HOLDING), null);
		DbDataObject dbLink = SvLink.getLinkType(Tc.HOLDING_QUARANTINE, SvReader.getTypeIdByName(Tc.HOLDING),
				SvReader.getTypeIdByName(Tc.QUARANTINE));
		DbDataArray dbaQuarantines = null;
		if (dboHolding != null && dbLink != null) {
			dbaQuarantines = svr.getObjectsByLinkedId(dboHolding.getObject_id(), dbLink, null, 0, 0);
			if (dbaQuarantines != null && !dbaQuarantines.getItems().isEmpty()) {
				for (DbDataObject tempQuarantine : dbaQuarantines.getItems()) {
					if (checkIfQuarantineActive(tempQuarantine)) {
						if (tempQuarantine.getVal(Tc.QUARANTINE_TYPE).equals(quarantineType)) {
							result = true;
							break;
						}
					}
				}
			}
		}
		return result;
	}

	public Boolean checkIfQuarantineActive(DbDataObject dboQuarantine) {
		Boolean result = false;
		String date_from = null;
		String date_to = null;
		String pattern = Tc.DATE_PATTERN;
		DateTime currentDate = DateTime.now().withTime(0, 0, 0, 0);
		if (dboQuarantine != null && dboQuarantine.getVal(Tc.DATE_FROM) != null
				&& dboQuarantine.getVal(Tc.DATE_TO) != null) {
			date_from = dboQuarantine.getVal(Tc.DATE_FROM).toString();
			date_to = dboQuarantine.getVal(Tc.DATE_TO).toString();
			DateTime convertedDateFrom = DateTime.parse(date_from, DateTimeFormat.forPattern(pattern)).withTime(0, 0, 0,
					0);
			DateTime convertedDateTo = DateTime.parse(date_to, DateTimeFormat.forPattern(pattern)).withTime(0, 0, 0, 0);
			if (convertedDateFrom.compareTo(currentDate) <= 0 && convertedDateTo.compareTo(currentDate) >= 0) {
				result = true;

			}
		}
		return result;
	}

	public DbDataObject checkIfAnimalIdExistsInInventoryItem(DbDataObject dbo, Reader rdr, SvReader svr)
			throws SvException {
		DbDataObject dboInventoryItem = rdr.getInventoryItemAccordingAnimalParams(dbo, false, svr);
		if (dboInventoryItem == null && !dbo.getObject_type().equals(SvReader.getTypeIdByName(Tc.PET))) {
			throw (new SvException("naits.error.noAvailableInventoryItemWithCurrentEarTagId", svCONST.systemUser, null,
					null));
		}
		return dboInventoryItem;
	}

	public void checkIfAnimalBirthDateIsNull(DbDataObject dboAnimal, Reader rdr, SvReader svr) throws SvException {
		if (dboAnimal.getVal(Tc.BIRTH_DATE) == null)
			throw (new SvException("naits.error.birthDateOfNewlyRegisteredAnimalCanNotBeNull", svCONST.systemUser, null,
					null));
		return;
	}

	public Boolean checkIfQuarantineBlackList(Long quarantineObjId, SvReader svr) throws SvException {
		Boolean result = false;
		DbDataObject quarantineObj = svr.getObjectById(quarantineObjId, SvReader.getTypeIdByName(Tc.QUARANTINE), null);
		if (quarantineObj != null && quarantineObj.getVal(Tc.QUARANTINE_TYPE) != null
				&& quarantineObj.getVal(Tc.QUARANTINE_TYPE).equals("1")) {
			result = true;
		}
		return result;
	}

	/**
	 * Method to return health status per specific holding, according Area Health
	 * Management Tool
	 * 
	 * @param holdingPic - pic of the holding
	 * @param svr
	 * @return String (FREE/HIGH_RISK/LOW_RISK)
	 * @throws SvException
	 */
	public String checkHoldingHealthStatusByArea(String holdingPic, SvReader svr) throws SvException {
		String holdingAreaStatus = Tc.FREE;
		Reader rdr = new Reader();
		DbDataObject holding = rdr.findAppropriateHoldingByPic(holdingPic, svr);
		if (holding != null && holding.getVal(Tc.VILLAGE_CODE) != null) {
			DbDataObject villageObj = rdr.findAppropriateAreaByCode(holding.getVal(Tc.VILLAGE_CODE).toString(), "3",
					svr);
			holdingAreaStatus = rdr.findAppropriateHealthStatusForArea(villageObj, svr);
			if (holdingAreaStatus.equals(Tc.FREE)) {
				DbDataObject communObj = rdr.findAppropriateAreaByCode(
						holding.getVal(Tc.VILLAGE_CODE).toString().substring(0, 6), "2", svr);
				holdingAreaStatus = rdr.findAppropriateHealthStatusForArea(communObj, svr);
			}
			if (holdingAreaStatus.equals(Tc.FREE)) {
				DbDataObject municObj = rdr.findAppropriateAreaByCode(
						holding.getVal(Tc.VILLAGE_CODE).toString().substring(0, 4), "1", svr);
				holdingAreaStatus = rdr.findAppropriateHealthStatusForArea(municObj, svr);
			}
			if (holdingAreaStatus.equals(Tc.FREE)) {
				DbDataObject regionObj = rdr.findAppropriateAreaByCode(
						holding.getVal(Tc.VILLAGE_CODE).toString().substring(0, 2), "0", svr);
				holdingAreaStatus = rdr.findAppropriateHealthStatusForArea(regionObj, svr);
			}
		}
		return holdingAreaStatus;
	}

	/**
	 * Method that checks if AREA_HEALTH has duplicate disease
	 * 
	 * @param areaHealthObj AREA_HEALTH object
	 * @param svr           SvReader instance
	 * @return
	 * @throws SvException
	 */
	public Boolean checkIfAreaHasDuplicateDisease(DbDataObject areaHealthObj, SvReader svr) throws SvException {
		Boolean result = false;
		DbDataArray areaHealthStatuses = null;
		if (areaHealthObj != null) {
			areaHealthStatuses = svr.getObjectsByParentId(areaHealthObj.getParent_id(),
					SvReader.getTypeIdByName(Tc.AREA_HEALTH), null, 0, 0);
		}
		if (areaHealthStatuses != null && !areaHealthStatuses.getItems().isEmpty()) {
			for (DbDataObject tempAreaHealth : areaHealthStatuses.getItems()) {
				if (areaHealthObj.getVal(Tc.DISEASE_ID) != null && tempAreaHealth.getVal(Tc.DISEASE_ID) != null
						&& areaHealthObj.getVal(Tc.DISEASE_ID).toString()
								.equals(tempAreaHealth.getVal(Tc.DISEASE_ID).toString())) {
					result = true;
					break;
				}
			}
		}
		return result;
	}

	/**
	 * Method that checks if EXPORT CERTIFICATE can be saved depend on quarantine
	 * type.
	 * 
	 * @param dbo EXPORT_CERTIFICATE object
	 * @param rdr Reader instance
	 * @param svr SvReader instance
	 * @return
	 * @throws SvException
	 */
	public Boolean checkIfExportCertificateCanBeSaved(DbDataObject dbo, Reader rdr, SvReader svr) throws SvException {
		Boolean result = false;
		if (dbo.getParent_id() != null) {
			DbDataObject quarantineObject = svr.getObjectById(dbo.getParent_id(),
					SvReader.getTypeIdByName(Tc.QUARANTINE), null);
			// log4j.trace(quarantineObject.getVal(Tc.TYPE));
			if (quarantineObject.getVal(Tc.QUARANTINE_TYPE) != null
					&& quarantineObject.getVal(Tc.QUARANTINE_TYPE).toString().equals("0")) {
				// quarantine_type 0 corresponds to en.export_quarantine, 1
				// corresponds to en.blacklist
				result = true;
			}
			if (!rdr.checkIfSomeLinkExistsBetweenHoldingAndQuarantine(quarantineObject, svr)) {
				throw (new SvException("naits.error.quarantineOfTypeExportCertMustHaveHolding", svCONST.systemUser,
						null, null));
			}
		}
		return result;
	}

	public boolean checkIfAnimalOrFlockDoesNotBelongToDraftMovementDoc(DbDataObject animalOrFlockObj, Reader rdr,
			SvReader svr) throws SvException {
		boolean result = true;
		DbDataArray animalOrFlockMovements = null;
		if (animalOrFlockObj.getObject_type().equals(SvReader.getTypeIdByName(Tc.ANIMAL))) {
			animalOrFlockMovements = svr.getObjectsByParentId(animalOrFlockObj.getObject_id(),
					SvReader.getTypeIdByName(Tc.ANIMAL_MOVEMENT), null, 0, 0);
		} else {
			animalOrFlockMovements = svr.getObjectsByParentId(animalOrFlockObj.getObject_id(),
					SvReader.getTypeIdByName(Tc.FLOCK_MOVEMENT), null, 0, 0);
		}
		if (animalOrFlockMovements != null && !animalOrFlockMovements.getItems().isEmpty()) {
			for (DbDataObject movementObj : animalOrFlockMovements.getItems()) {
				if (movementObj != null && movementObj.getVal(Tc.MOVEMENT_DOC_ID) != null) {
					DbDataObject moveDoc = rdr.searchForObject(SvReader.getTypeIdByName(Tc.MOVEMENT_DOC),
							Tc.MOVEMENT_DOC_ID, movementObj.getVal(Tc.MOVEMENT_DOC_ID).toString(), svr);
					if (moveDoc != null && moveDoc.getStatus().equals(Tc.DRAFT)
							&& movementObj.getStatus().equalsIgnoreCase(Tc.VALID)) {
						result = false;
						break;
					}
				}
			}
		}
		return result;
	}

	/**
	 * Method that checks (returns true) if specific field is unique. Otherwise
	 * returns false. Actually we use this method so we can set specific field to be
	 * unique.
	 * 
	 * @param fieldName Field we want to be unique
	 * @param dbo       Object that contains that field
	 * @param svr       SvReader instance
	 * @return
	 * @throws SvException
	 */
	public boolean checkIfFieldIsUnique(String fieldName, DbDataObject dbo, SvReader svr) throws SvException {
		Boolean result = true;
		if (dbo.getVal(fieldName) != null) {
			DbSearchCriterion cr1 = new DbSearchCriterion(fieldName, DbCompareOperand.EQUAL,
					dbo.getVal(fieldName).toString());
			DbSearchExpression dbse = new DbSearchExpression();
			dbse.addDbSearchItem(cr1);
			DbDataArray ar = svr.getObjects(dbse, dbo.getObject_type(), null, 0, 0);
			if ((dbo.getObject_id().equals(0L) && !ar.getItems().isEmpty())
					|| (!dbo.getObject_id().equals(0L) && ar.size() > 1)) {
				result = false;
			}
		}
		return result;
	}

	/**
	 * Method that checks if Border Inspection Point Holding is valid.
	 * 
	 * @param dbo
	 * @param rdr
	 * @param svr
	 * @return
	 * @throws SvException
	 */
	public Boolean checkIfBIPholdingValid(DbDataObject dbo, Reader rdr, SvReader svr) throws SvException {
		Boolean result = false;
		if (dbo.getVal("bip_holding_id") != null) {
			DbDataObject dboHolding = rdr.findAppropriateHoldingByPic(dbo.getVal("bip_holding_id").toString(), svr);
			if (dboHolding != null && checkIfHoldingIsBorderInspectionPoint(dboHolding)) {
				result = true;
			}
		}
		return result;
	}

	/**
	 * Method that checks if ANIMAL or FLOCK has status POSTMORTEM so we can edit
	 * post-mortem form.
	 * 
	 * @param dboPostSlaugh
	 * @param svr
	 * @return
	 * @throws SvException
	 */
	public Boolean checkIfAnimalOrFlockHaveStatusPostMortem(DbDataObject dboAnimalOrFlock, SvReader svr)
			throws SvException {
		Boolean result = false;
		if (dboAnimalOrFlock != null && dboAnimalOrFlock.getStatus() != null
				&& dboAnimalOrFlock.getStatus().equals(Tc.POSTMORTEM)) {
			result = true;
		}
		return result;
	}

	/**
	 * Void method that has set of QUARANTINE validations. Than, we use them in
	 * OnSaveValidation class
	 * 
	 * @param dbo DbDataObject of quarantine
	 * @param svr SvReader instance
	 * @throws SvException
	 */
	public void quarantineOnSaveValidationSet(DbDataObject dbo, Reader rdr, SvReader svr) throws SvException {
		DbDataObject dboQuarantineDatabaseVersion = svr.getObjectById(dbo.getObject_id(),
				SvReader.getTypeIdByName(Tc.QUARANTINE), new DateTime());
		if (dboQuarantineDatabaseVersion != null) {
			if (dbo.getVal(Tc.QUARANTINE_TYPE).toString().equals(Tc.BLACKLIST_QUARANTINE)) {
				if (rdr.checkIfObjectHasBeenEditedDependOnSpecificFields(dbo, dboQuarantineDatabaseVersion,
						Tc.DATE_FROM, Tc.DATE_TO, Tc.QUARANTINE_TYPE)) {
					throw (new SvException("naits.error.quarantineStartDateEndDateAndTypeAreNotUpdatable",
							svr.getInstanceUser()));
				}
			}
			// here only the quarantine date_to can be edited
			else if (dbo.getVal(Tc.QUARANTINE_TYPE).toString().equals(Tc.EXPORT_QUARANTINE)) {
				DateTime dateToCurrentVersion = rdr.getDateFromDBField(Tc.DATE_TO, dbo);
				DateTime dateToLastVersion = rdr.getDateFromDBField(Tc.DATE_TO, dboQuarantineDatabaseVersion);
				if (rdr.checkIfObjectHasBeenEditedDependOnSpecificFields(dbo, dboQuarantineDatabaseVersion,
						Tc.DATE_FROM, Tc.QUARANTINE_TYPE)) {
					throw (new SvException("naits.error.quarantineStartDateAndTypeAreNotUpdatable",
							svr.getInstanceUser()));
				}
				SimpleDateFormat formatter = new SimpleDateFormat(Tc.DATE_PATTERN);
				if (formatter.format(new DateTime().toDate())
						.compareTo(formatter.format(dateToLastVersion.toDate())) == 0) {
					if (dateToCurrentVersion.isBefore(dateToLastVersion)) {
						throw (new SvException("naits.error.quarantineEndDateCannotBeShorted", svr.getInstanceUser()));
					}
					if ((int) getDateDiff(dateToCurrentVersion, dateToLastVersion, TimeUnit.DAYS) > 15) {
						throw (new SvException("naits.error.quarantineEndDateCannotBeExtendedForMoreThan15Days",
								svr.getInstanceUser()));
					}
				} else {
					if (rdr.checkIfObjectHasBeenEditedDependOnSpecificFields(dbo, dboQuarantineDatabaseVersion,
							Tc.DATE_TO)) {
						throw (new SvException("naits.error.quarantineCanBeEditedOnlyOnLastDayOfItsExpiration",
								svr.getInstanceUser()));
					}
				}
			}
		}
		DateTime dtFrom = new DateTime(dbo.getVal(Tc.DATE_FROM).toString());
		DateTime dtNow = new DateTime();
		if (dtFrom.isBefore(dtNow.minusDays(1))) {
			throw (new SvException("naits.error.quarantineCanNotBeRegisteredInThePast", svCONST.systemUser, null,
					null));
		}
		if (isQuarantineStartDateAfterEndDate(dbo)) {
			throw (new SvException("naits.error.quarantineCanNotHaveEndDateBeforeStartDate", svr.getInstanceUser()));
		}
		if (dbo.getVal(Tc.QUARANTINE_ID) == null) {
			rdr.generateQuarantineId(dbo, svr);
		}
	}

	/**
	 * Method that checks if ANIMAL or FLOCK has valid pre-mortem form. We use this
	 * validation so we can legal create post-mortem form
	 * 
	 * @param dboPreSlaught
	 * @param svr
	 * @return
	 * @throws SvException
	 */
	public Boolean checkIfAnimalOrFlockHasAlreadyValidPreMortem(DbDataObject dboAnimalOrFlock, SvReader svr)
			throws SvException {
		Boolean result = false;
		DbDataArray preMortemsForAnimalOrFlock = null;
		int counter = 0;
		if (dboAnimalOrFlock != null) {
			preMortemsForAnimalOrFlock = svr.getObjectsByParentId(dboAnimalOrFlock.getObject_id(),
					SvReader.getTypeIdByName(Tc.PRE_SLAUGHT_FORM), null, 0, 0);
			if (preMortemsForAnimalOrFlock != null && !preMortemsForAnimalOrFlock.getItems().isEmpty()) {
				for (DbDataObject dbo : preMortemsForAnimalOrFlock.getItems()) {
					if (dbo.getVal(Tc.DECISION) != null && dbo.getVal(Tc.DECISION).toString().equals("1")) {
						counter++;
					}
				}
				if (counter >= 1) {
					result = true;
				}
			}
		}
		return result;
	}

	/**
	 * This list of diseases are used to check if Pre_Slaught_Form contains any of
	 * the prohibited list of diseases.
	 * 
	 * @param parentCodeName
	 * @return
	 */
	public ArrayList<String> prohibitedDiseasesAndSuspissionDiseasesInPremortemForm(String parentCodeName) {
		ArrayList<String> unacceptableDiseases = new ArrayList<>();
		if (parentCodeName.equals(Tc.DISEASE_NAME)) {
			unacceptableDiseases.add("2");
			unacceptableDiseases.add("59");
			unacceptableDiseases.add("5");
			unacceptableDiseases.add("55");
			unacceptableDiseases.add("57");
			unacceptableDiseases.add("50");
			unacceptableDiseases.add("51");
			unacceptableDiseases.add("1");
			unacceptableDiseases.add("58");
			unacceptableDiseases.add("12");
			unacceptableDiseases.add("7");
		} else {
			// DISEASE_SUSPISION
			unacceptableDiseases.add("3");
			unacceptableDiseases.add("16");
			unacceptableDiseases.add("4");
			unacceptableDiseases.add("5");
			unacceptableDiseases.add("12");
			unacceptableDiseases.add("6");
			unacceptableDiseases.add("13");
			unacceptableDiseases.add("17");
			unacceptableDiseases.add("2");
			unacceptableDiseases.add("15");
			unacceptableDiseases.add("14");
		}
		return unacceptableDiseases;
	}

	/**
	 * Check method used only for Pre_Slaught_Form so we can prohibit new forms if
	 * unacceptable disease are spotted.
	 * 
	 * @param dboAnimalOrFlock
	 * @param svr
	 * @return
	 * @throws SvException
	 */
	public boolean checkIfAnimalHasBlockingDiseaseInPremortemForm(DbDataObject dboAnimalOrFlock, SvReader svr)
			throws SvException {
		boolean result = false;
		DbDataArray preMortemsForAnimalOrFlock = null;
		if (dboAnimalOrFlock != null) {
			preMortemsForAnimalOrFlock = svr.getObjectsByParentId(dboAnimalOrFlock.getObject_id(),
					SvReader.getTypeIdByName(Tc.PRE_SLAUGHT_FORM), null, 0, 0);
			if (preMortemsForAnimalOrFlock != null && !preMortemsForAnimalOrFlock.getItems().isEmpty()) {
				for (DbDataObject dbo : preMortemsForAnimalOrFlock.getItems()) {
					if (dbo.getVal(Tc.DISEASE_SUSPISION_PM) != null
							&& prohibitedDiseasesAndSuspissionDiseasesInPremortemForm(Tc.DISEASE_SUSPISION_PM)
									.contains(dbo.getVal(Tc.DISEASE_SUSPISION_PM).toString())) {
						result = true;
						break;
					}
				}
				if (!result) {
					for (DbDataObject dbo : preMortemsForAnimalOrFlock.getItems()) {
						if (dbo.getVal(Tc.DISEASE_NAME) != null
								&& prohibitedDiseasesAndSuspissionDiseasesInPremortemForm(Tc.DISEASE_NAME)
										.contains(dbo.getVal(Tc.DISEASE_NAME).toString())) {
							result = true;
							break;
						}
					}
				}
			}
		}
		return result;
	}

	public Boolean isDiseaseProhibited(DbDataObject dbo, String fieldName) {
		Reader rdr = new Reader();
		Boolean result = false;
		if (dbo.getVal(fieldName) != null) {
			ArrayList<String> arrProhibitedDiseases = prohibitedDiseasesAndSuspissionDiseasesInPremortemForm(fieldName);
			List<String> arrString = rdr.getMultiSelectFieldValueAsList(dbo, fieldName);
			if (!arrString.isEmpty()) {
				for (String foundDisease : arrString) {
					for (String prohibitDisease : arrProhibitedDiseases) {
						if (foundDisease.equals(prohibitDisease)) {
							result = true;
							break;
						}
					}
				}
			}
		}
		return result;
	}

	public Boolean checkIfRangeCanBeSaved(DbDataObject dbo, String tag_type, Long start_tag_id, Long end_tag_id,
			SvReader svr) throws SvException {
		Reader rdr = new Reader();
		String throwMessage = "naits.error.beforeSaveCheck_range_overlapping";
		DbDataObject dbOrderOrTransfer = svr.getObjectById(dbo.getParent_id(), SvReader.getTypeIdByName(Tc.ORDER),
				null);
		String objectType = Tc.RANGE;
		if (dbOrderOrTransfer == null) {
			dbOrderOrTransfer = dbo;
			objectType = Tc.TRANSFER;
		}
		DbDataArray arrRangeIntersect = new DbDataArray();
		if (dbOrderOrTransfer != null) {
			if (dbOrderOrTransfer.getVal(Tc.CACHE_PARENT_ID) != null) {
				arrRangeIntersect = rdr.getOverlappingRanges(objectType,
						Long.valueOf(dbOrderOrTransfer.getVal(Tc.CACHE_PARENT_ID).toString()), tag_type, start_tag_id,
						end_tag_id, svr);
			} else {
				arrRangeIntersect = rdr.getOverlappingRanges(objectType, dbOrderOrTransfer.getParent_id(), tag_type,
						start_tag_id, end_tag_id, svr);
			}
		}
		if (!arrRangeIntersect.getItems().isEmpty()) {
			if (objectType.equals(Tc.TRANSFER)) {
				throwMessage += "_" + objectType.toLowerCase();
			}
			throw (new SvException(throwMessage, svr.getInstanceUser()));
		} else {
			return true;
		}
	}

	public Boolean checkIfTransferIsOverlapping(Long objectType, Long parentId, String tag_type, Long start_tag_id,
			Long end_tag_id, Reader rdr, SvReader svr) {
		boolean result = false;
		try {
			int total = rdr.getOverlappingTransferArrayListCustomSQL(tag_type, start_tag_id, end_tag_id, parentId, svr);
			if (total > 0) {
				result = true;
			}
		} catch (SvException e) {
			log4j.error(e);
		}
		return result;
	}

	public Boolean checkIfAnimalMovementCanBeSaved(DbDataObject dbo, SvReader svr) throws SvException {
		Boolean result = false;
		Reader rdr = new Reader();
		DateTime convertedBirthDate = null;
		if (dbo.getParent_id() != null) {
			DbDataObject animalObj = svr.getObjectById(dbo.getParent_id(), SvReader.getTypeIdByName(Tc.ANIMAL), null);
			convertedBirthDate = rdr.getDateFromDBField(Tc.BIRTH_DATE, animalObj);
		}

		// escape check from future departure date + before birth date movement
		if (dbo.getVal(Tc.DEPARTURE_DATE) != null) {
			DateTime convertedDeparture = rdr.getDateFromDBField(Tc.DEPARTURE_DATE, dbo);
			// if (convertedDeparture.isAfter(new DateTime())) {
			// throw (new
			// SvException("naits.error.beforeSaveCheck_inputDateIsAfterTheCurrentDate",
			// svCONST.systemUser,
			// null, null));
			// }
			if (convertedBirthDate != null && convertedBirthDate.isAfter(convertedDeparture)) {
				throw (new SvException("naits.error.beforeSaveCheck_departureDateCanNotBeBeforeAnimalBirthDate",
						svCONST.systemUser, null, null));
			}
		}

		// escape check from future arrival date + before birth date movement
		if (dbo.getVal(Tc.ARRIVAL_DATE) != null) {
			DateTime convertedArrival = rdr.getDateFromDBField(Tc.ARRIVAL_DATE, dbo);
			if (convertedArrival.isAfter(new DateTime())) {
				throw (new SvException("naits.error.beforeSaveCheck_inputDateIsAfterTheCurrentDate", svCONST.systemUser,
						null, null));
			}
			if (convertedBirthDate != null && convertedBirthDate.isAfter(convertedArrival)) {
				throw (new SvException("naits.error.beforeSaveCheck_ArrivalDateCanNotBeBeforeAnimalBirthDate",
						svCONST.systemUser, null, null));
			}
		}

		// escape check from estimation date inconsistency + before birth date
		// movement
		if (dbo.getVal(Tc.ESTM_DATE_DEPARTURE) != null && dbo.getVal(Tc.ESTM_DATE_ARRIVAL) != null) {
			DateTime convertedEstmDeparture = rdr.getDateFromDBField(Tc.ESTM_DATE_DEPARTURE, dbo);
			DateTime convertedEstmArrival = rdr.getDateFromDBField(Tc.ESTM_DATE_ARRIVAL, dbo);
			// if (convertedEstmDeparture.isAfter(new DateTime())) {
			// throw (new
			// SvException("naits.error.beforeSaveCheck_inputDateIsAfterTheCurrentDate",
			// svCONST.systemUser,
			// null, null));
			// }
			// if (convertedEstmArrival.isAfter(new DateTime())) {
			// throw (new
			// SvException("naits.error.beforeSaveCheck_inputDateIsAfterTheCurrentDate",
			// svCONST.systemUser,
			// null, null));
			// }
			if (convertedBirthDate != null && convertedEstmDeparture != null
					&& convertedBirthDate.isAfter(convertedEstmDeparture)) {
				throw (new SvException(
						"naits.error.beforeSaveCheck_estimatedDepartOrArrivalDateCanNotBeAfterAnimalBirthDate",
						svCONST.systemUser, null, null));
			}
			if (convertedBirthDate != null && convertedEstmArrival != null
					&& convertedBirthDate.isAfter(convertedEstmArrival)) {
				throw (new SvException(
						"naits.error.beforeSaveCheck_estimatedDepartOrArrivalDateCanNotBeAfterAnimalBirthDate",
						svCONST.systemUser, null, null));
			}

			if (convertedEstmDeparture != null && convertedEstmArrival != null
					&& convertedEstmDeparture.isAfter(convertedEstmArrival)) {
				throw (new SvException(
						"naits.error.beforeSaveCheck_estimatedDepartDateCanNotBeAfterEstimatedArrivalDate",
						svCONST.systemUser, null, null));
			}

		}

		// set of animal_movement validation checks
		animalMovementOnSaveValidationSet(dbo, svr);
		result = true;
		return result;
	}

	/**
	 * Method that checks if animal has appropriate class/race combination. Later we
	 * use this method in OnSaveValidation
	 * 
	 * @param dboAnimal ANIMAL object
	 * @param svr       SvReader instance
	 * @return
	 * @throws SvException
	 */
	public Boolean checkIfAnimalHasNotAppropriateClassOrRace(DbDataObject dboAnimal, SvReader svr) throws SvException {
		Boolean result = true;
		if (dboAnimal.getVal(Tc.ANIMAL_CLASS) != null && dboAnimal.getVal(Tc.ANIMAL_RACE) != null) {
			DbSearchCriterion sc1 = new DbSearchCriterion(Tc.ANIMAL_CLASS, DbCompareOperand.EQUAL,
					dboAnimal.getVal(Tc.ANIMAL_CLASS).toString());
			DbSearchCriterion sc2 = new DbSearchCriterion(Tc.ANIMAL_RACE, DbCompareOperand.EQUAL,
					dboAnimal.getVal(Tc.ANIMAL_RACE).toString());
			DbSearchExpression dbse = new DbSearchExpression();
			dbse.addDbSearchItem(sc1).addDbSearchItem(sc2);

			DbDataArray ar = svr.getObjects(dbse, SvReader.getTypeIdByName(Tc.ANIMAL_TYPE), null, 0, 0);

			if (!ar.getItems().isEmpty()) {
				result = false;
			}
		}
		return result;
	}

	/**
	 * Method that checks if newborn has twin
	 * 
	 * @param animalObj ANIMAL object
	 * @param svr       SvReader instance
	 * @return true/false
	 * @throws SvException
	 */
	public Boolean checkIfAnimalIsTwin(DbDataObject animalObj, SvReader svr) throws SvException {
		DbDataArray getSibilings = null;
		DbDataObject currentAnimal = null;
		Reader rdr = null;
		Boolean result = false;
		Integer differenceDays = 0;
		if (animalObj.getVal(Tc.MOTHER_TAG_ID) != null
				&& !animalObj.getVal(Tc.MOTHER_TAG_ID).toString().trim().equals("")) {
			rdr = new Reader();
			getSibilings = rdr.getAnimalSiblingsAccordingMotherTagId(animalObj, svr);
			if (getSibilings != null && !getSibilings.getItems().isEmpty()) {
				currentAnimal = getSibilings.get(getSibilings.size() - 1);
				if (animalObj != null && currentAnimal != null && animalObj.getVal(Tc.ANIMAL_CLASS) != null
						&& currentAnimal.getVal(Tc.ANIMAL_CLASS) != null
						&& animalObj.getVal(Tc.ANIMAL_CLASS).toString()
								.equals(currentAnimal.getVal(Tc.ANIMAL_CLASS).toString())
						&& animalObj.getVal(Tc.ANIMAL_RACE).toString()
								.equals(currentAnimal.getVal(Tc.ANIMAL_RACE).toString())
						&& animalObj.getVal(Tc.BIRTH_DATE) != null && currentAnimal.getVal(Tc.BIRTH_DATE) != null) {
					differenceDays = getDaysBetweenTwoDates(currentAnimal, animalObj);
					if (differenceDays >= 0 && differenceDays < 2) {
						result = true;
					}
				}
			}
		}
		return result;
	}

	/**
	 * Void method that has set of ANIMAL validations. Than, we use them in
	 * OnSaveValidation class
	 * 
	 * @param dboAnimal ANIMAL object
	 * @param rdr       Reader instance
	 * @param svr       SvReader instance
	 * @throws SvException
	 */
	public void animalOnSaveValidationSet(DbDataObject dboAnimal, Reader rdr, SvReader svr, SvWriter svw)
			throws SvException {
		if (dboAnimal != null) {
			Writer wr = new Writer();
			DbDataArray animalSibilings = null;
			DbDataObject twinChild = null;
			// period between newborns is missing here
			if (!checkIfAnimalIdAlredyExist(dboAnimal, svr)) {
				throw (new SvException("naits.error.beforeSaveCheck_animal_animalIdCanNotBeChanged", svCONST.systemUser,
						null, null));
			}
			if (!checkIfAnimalIdIsNotSelfParent(dboAnimal, svr))
				throw (new SvException("naits.error.beforeSaveCheck_animal_canNotBeSelfParent", svCONST.systemUser,
						null, null));
			// animalMotherChecks
			if (dboAnimal.getVal(Tc.MOTHER_TAG_ID) != null
					&& !dboAnimal.getVal(Tc.MOTHER_TAG_ID).toString().trim().equals("")) {
				if (!checkIfAnimalMotherTagIdExist(dboAnimal, svr))
					throw (new SvException("naits.error.beforeSaveCheck_animal_motherTagIdNotRegistered",
							svCONST.systemUser, null, null));
				if (!checkIfAnimalsMotherIsFromSameSpecie(dboAnimal, svr))
					throw (new SvException(
							"naits.error.beforeSaveCheck_animal_animalMotherMustHaveSameSpecieAsTheChild",
							svCONST.systemUser, null, null));
				if (!checkIfAnimalMotherGenderIsValid(dboAnimal, svr))
					throw (new SvException("naits.error.beforeSaveCheck_animal_invalidMotherGender", svCONST.systemUser,
							null, null));
				if (!checkIfAnimalMotherOrFatherAgeIsAppropriate(dboAnimal, Tc.MOTHER_TAG_ID, svr)) {
					throw (new SvException("naits.error.beforeSaveCheck_animal_invalidMotherAge", svCONST.systemUser,
							null, null));
				}
				if (!checkIfAnimalMotherAgeIsAppropriate(dboAnimal, svr))
					throw (new SvException("naits.error.beforeSaveCheck_animal_motherAgeIsUnderAge", svCONST.systemUser,
							null, null));
				if (!checkIfPeriodBetweenNewbornsIsAppropriate(dboAnimal, svr))
					throw (new SvException("naits.error.periodBetweenNewBornAnimalAndLastBornAnimalAreNotAppropriate",
							svCONST.systemUser, null, null));
				if (dboAnimal.getObject_id().equals(0L) && checkIfAnimalIsTwin(dboAnimal, svr)) {
					animalSibilings = rdr.getAnimalSiblingsAccordingMotherTagId(dboAnimal, svr);
					twinChild = animalSibilings.get(0);
					wr.notifyUserGroupByGroupName("ADMINISTRATORS", "SYS_GEN", "Twins registration detected.",
							"Animal with ear tag ID: " + dboAnimal.getVal(Tc.ANIMAL_ID).toString()
									+ " is twin with animal with ear tag ID: "
									+ twinChild.getVal(Tc.ANIMAL_ID).toString() + " Breed: "
									+ rdr.decodeCodeValue(SvReader.getTypeIdByName(Tc.ANIMAL), Tc.ANIMAL_CLASS,
											twinChild.getVal(Tc.ANIMAL_CLASS).toString(),
											svr.getUserLocaleId(svr.getInstanceUser()), svr),
							"", null, svr, svw);
				}
			}
			// animalFatherChecks
			if (dboAnimal.getVal(Tc.FATHER_TAG_ID) != null
					&& !dboAnimal.getVal(Tc.FATHER_TAG_ID).toString().trim().equals("")) {
				if (!checkIfAnimalFatherTagIdExist(dboAnimal, svr))
					throw (new SvException("naits.error.beforeSaveCheck_animal_fatherTagIdNotRegistered",
							svCONST.systemUser, null, null));
				if (!checkIfAnimalsFatherIsFromSameSpecie(dboAnimal, svr))
					throw (new SvException(
							"naits.error.beforeSaveCheck_animal_animalFatherMustHaveSameSpecieAsTheChild",
							svCONST.systemUser, null, null));
				if (!checkIfAnimalFatherGenderIsValid(dboAnimal, svr))
					throw (new SvException("naits.error.beforeSaveCheck_animal_invalidFatherGender", svCONST.systemUser,
							null, null));
				if (!checkIfAnimalMotherOrFatherAgeIsAppropriate(dboAnimal, Tc.FATHER_TAG_ID, svr)) {
					throw (new SvException("naits.error.beforeSaveCheck_animal_invalidFatherAge", svCONST.systemUser,
							null, null));
				}
			}
			// animalMotherFatherChecks
			if (checkIfAnimalsMotherAndFatherHaveSameAnimalId(dboAnimal)) {
				throw (new SvException("naits.error.beforeSaveCheck_animal_parentCanNotBeTheSame", svCONST.systemUser,
						null, null));
			}
			if (!checkIfAnimalRegDateIsNotAfterAnimalBirthDate(dboAnimal, svr))
				throw (new SvException("naits.error.beforeSaveCheck_animal_regDateCanNotBeBeforeBirthDate",
						svCONST.systemUser, null, null));
			if (!checkIfAnimalDeathDateIsNotBeforeBirthDate(dboAnimal, svr))
				throw (new SvException("naits.error.beforeSaveCheck_animal_deathDateCanNotBeBeforeBirthDate",
						svCONST.systemUser, null, null));

			if (dboAnimal.getObject_id().equals(0L) && checkIfAnimalHasNotAppropriateClassOrRace(dboAnimal, svr))
				throw (new SvException("naits.error.animalDoesNotHaveAppropriateAnimalClassOrRace", svCONST.systemUser,
						null, null));
			if (!checkIfAnimalHasClassAndRace(dboAnimal, Tc.ANIMAL_CLASS)) {
				throw (new SvException("naits.error.anmalBreedCantBeNull", svCONST.systemUser, null, null));
			}
		}

	}

	/**
	 * Method that checks if user is administrator i.e if it's linked to group of
	 * type Administrators with Default group link type
	 * 
	 * @param dboUser
	 * @param svr
	 * @return
	 * @throws SvException
	 */
	public boolean checkIfUserIsAdministrator(DbDataObject dboUser, SvReader svr) throws SvException {
		boolean result = false;
		DbDataArray allDefaultGroups = svr.getAllUserGroups(dboUser, true);
		if (!allDefaultGroups.getItems().isEmpty()) {
			DbDataObject dboDefaultGroup = allDefaultGroups.get(0);
			if (dboDefaultGroup.getVal(Tc.GROUP_TYPE) != null
					&& dboDefaultGroup.getVal(Tc.GROUP_TYPE).toString().equals(Tc.ADMINISTRATORS)) {
				result = true;
			}
		}
		return result;
	}

	/**
	 * Void method that has set of ANIMAL MOVEMENT validations. Than, we use them in
	 * OnSaveValidation class
	 * 
	 * @param dboAnimalMovement ANIMAL_MOVEMENT object
	 * @param svr               SvReader instance
	 * @throws SvException
	 */
	public void animalMovementOnSaveValidationSet(DbDataObject dboAnimalMovement, SvReader svr) throws SvException {
		if (dboAnimalMovement != null) {
			ValidationChecks validator = new ValidationChecks();
			if (!validator.checkIfDepartureDateIsBeforeArrivalDate(dboAnimalMovement))
				throw (new SvException("naits.error.arrivalDateCanNotBeforeDepartureDate", svCONST.systemUser, null,
						null));
			if (!validator.checkIfDepartureDateIsNotBeforeLastArrivalDate(dboAnimalMovement, svr))
				throw (new SvException("naits.error.departureDateCanNotBeBeforeLastArrivalDate", svCONST.systemUser,
						null, null));
			/*
			 * if (!validator.checkIfSourceHoldingHasPositiveHealthStatus(
			 * dboAnimalMovement, svr)) throw (new
			 * SvException("naits.error.sourceHoldingIsSuspiciousOrNegative",
			 * svCONST.systemUser, null, null)); if
			 * (!validator.checkIfDestinationHoldingHasPositiveHealthStatus(
			 * dboAnimalMovement, svr)) throw (new SvException(
			 * "naits.error.destinationHoldingIsSuspiciousOrNegative", svCONST.systemUser,
			 * null, null));
			 * 
			 * String movementValidationAccordingHoldingHealthStatus = validator
			 * .checkIfMovementValidAccordingHoldingsHealthStatus( dboAnimalMovement, svr);
			 * 
			 * if (!movementValidationAccordingHoldingHealthStatus.equals(
			 * "naits.system.movement_allowed")) { throw (new
			 * SvException(movementValidationAccordingHoldingHealthStatus,
			 * svCONST.systemUser, null, null)); }
			 */
		}
	}

	/**
	 * 
	 * 
	 * @param dboCode
	 * @param svr
	 * @return
	 * @throws SvException
	 */
	public Boolean setParentCodeValue(DbDataObject dboCode, SvReader svr) throws SvException {
		Boolean result = false;
		if (dboCode.getParent_id() > 0L) {
			DbDataObject parentCOde = svr.getObjectById(dboCode.getParent_id(), svCONST.OBJECT_TYPE_CODE, null);
			dboCode.setVal(Tc.PARENT_CODE_VALUE, parentCOde.getVal(Tc.CODE_VALUE));
			result = true;
		} else {
			result = true;
		}
		return result;
	}

	/**
	 * Method that checks if quarantine from type EXPORT CERTIFICATE has appropriate
	 * dates
	 * 
	 * @param obj QUARANTINE object
	 * @param svr SvReader instance
	 * @return
	 * @throws SvException
	 */
	public boolean checkIfDateOfInsertIsBeforeEndDate(DbDataObject quarantine, SvReader svr) throws SvException {
		Boolean result = false;
		String pattern = Tc.DATE_PATTERN;
		DateTime currentDate = new DateTime();
		DateTime convertedCurrentDate = DateTime.parse(currentDate.toString().substring(0, 10),
				DateTimeFormat.forPattern(pattern));
		DateTime endDate = new DateTime(quarantine.getVal(Tc.DATE_TO).toString());
		DateTime convertedEndDate = DateTime.parse(endDate.toString().substring(0, 10),
				DateTimeFormat.forPattern(pattern));
		if (convertedCurrentDate.isBefore(convertedEndDate)) {
			result = true;
		}
		return result;
	}

	/**
	 * @param firstDate
	 * @param secondDate
	 * @param timeUnit
	 * @return
	 */
	public static long getDateDiff(DateTime firstDate, DateTime secondDate, TimeUnit timeUnit) {
		long diff = firstDate.toDate().getTime() - secondDate.toDate().getTime();
		return timeUnit.convert(diff, TimeUnit.MILLISECONDS);
	}

	/**
	 * Boolean method that calculate days between two dates. Useful for days
	 * difference between new animal and last child animal
	 * 
	 * @param lastBronChild DbDataObject of lastBronChild
	 * @param currentChild  DbDataObject of currentChild
	 * @param num           Days difference between lastBronChildBirthDate and
	 *                      animal1 currentChildBirthDate
	 * @return true, false
	 * 
	 */
	public Integer getDaysBetweenTwoDates(DbDataObject lastBronChild, DbDataObject currentChild) {
		String pattern = Tc.DATE_PATTERN;
		String lastBronChildBirthDate = lastBronChild.getVal(Tc.BIRTH_DATE).toString().substring(0, 10);
		String currentChildBirthDate = currentChild.getVal(Tc.BIRTH_DATE).toString().substring(0, 10);

		DateTime lastBronChildBirthDateTime = DateTime.parse(lastBronChildBirthDate,
				DateTimeFormat.forPattern(pattern));
		DateTime currentChildBirthDateTime = DateTime.parse(currentChildBirthDate, DateTimeFormat.forPattern(pattern));

		Integer diffDays = (int) getDateDiff(lastBronChildBirthDateTime, currentChildBirthDateTime, TimeUnit.DAYS);
		return diffDays;
	}

	public Boolean checkIfMultiselectedDiseasesAreAppropriate(DbDataObject dbo, String fieldName,
			DbDataArray appropriateDiseases) {
		Boolean result = true;
		String diseases = Tc.EMPTY_STRING;
		int countApplicableDisease = 0;
		if (appropriateDiseases != null && dbo != null && dbo.getVal(fieldName) != null
				&& !dbo.getVal(fieldName).toString().equals("") && dbo.getVal(fieldName).toString().length() >= 1) {
			diseases = dbo.getVal(fieldName).toString();
			List<String> list = new ArrayList<String>(Arrays.asList(diseases.split(",")));
			if (list != null) {
				for (String tempDisease : list) {
					for (DbDataObject appDisease : appropriateDiseases.getItems()) {
						if (appDisease.getVal(Tc.DISEASE_NAME) != null
								&& appDisease.getVal(Tc.DISEASE_NAME).toString().equals(tempDisease)) {
							countApplicableDisease++;
						}
					}
				}
				if (countApplicableDisease < list.size()) {
					result = false;
				}
			}
		}
		return result;
	}

	/**
	 * Method that checks if certain type of animal has appropriate disease
	 * 
	 * @param preOrPostSlauightObj pre/post slaughter form
	 * @param svr                  SvReader instance
	 * @return
	 * @throws SvException
	 */
	public Boolean checkIfAnimalHasAppropriateDiseaseInPreOrPostSlaughtForm(DbDataObject preOrPostSlauightObj,
			DbDataObject animalOrFlockObj, SvReader svr) throws SvException {
		Boolean result = false;
		Reader rdr = new Reader();
		String fieldName = Tc.EMPTY_STRING;
		DbDataArray diseasesFound = rdr.getAppropriateDiseasesByAnimal(animalOrFlockObj, svr);
		if (preOrPostSlauightObj.getObject_type().equals(SvReader.getTypeIdByName(Tc.PRE_SLAUGHT_FORM))) {
			fieldName = Tc.DISEASE;
		} else if (preOrPostSlauightObj.getObject_type().equals(SvReader.getTypeIdByName(Tc.POST_SLAUGHT_FORM))) {
			fieldName = Tc.DISIESE_FINDING;
		}
		if (fieldName != "") {
			if (preOrPostSlauightObj.getVal(fieldName) == null
					|| preOrPostSlauightObj.getVal(fieldName).toString().equals("")) {
				result = true;
			} else {
				if (diseasesFound != null && !diseasesFound.getItems().isEmpty()) {
					if (preOrPostSlauightObj.getVal(fieldName).toString().length() == 1) {
						for (DbDataObject dboDisease : diseasesFound.getItems()) {
							if (dboDisease.getVal(Tc.DISEASE_NAME) != null && dboDisease.getVal(Tc.DISEASE_NAME)
									.toString().equals(preOrPostSlauightObj.getVal(fieldName).toString())) {
								result = true;
								break;
							}
						}
					} else if (preOrPostSlauightObj.getVal(fieldName).toString().length() > 1) {
						result = checkIfMultiselectedDiseasesAreAppropriate(preOrPostSlauightObj, fieldName,
								diseasesFound);
					}
				}
			}
		}
		return result;
	}

	/**
	 * Method that checks if same SPOT_CHECK object is showing more than once on
	 * same date in HOLDING
	 * 
	 * @param dboSpotCheck
	 * @param svr
	 * @return
	 * @throws SvException
	 */
	public Boolean checkIfSameCheckSubjectIsShowingMoreThanOnceOnSameDateInHolding(DbDataObject dboSpotCheck,
			SvReader svr) throws SvException {
		Boolean result = false;
		DbDataArray onSpotChecks = svr.getObjectsByParentId(dboSpotCheck.getParent_id(),
				SvReader.getTypeIdByName(Tc.SPOT_CHECK), null, 0, 0);
		if (onSpotChecks != null && !onSpotChecks.getItems().isEmpty()) {
			for (DbDataObject checkObj : onSpotChecks.getItems()) {
				if (checkObj.getVal(Tc.DATE_OF_REG) != null && dboSpotCheck.getVal(Tc.DATE_OF_REG) != null
						&& checkObj.getVal(Tc.CHECK_SUBJECT) != null && dboSpotCheck.getVal(Tc.CHECK_SUBJECT) != null
						&& checkObj.getVal(Tc.DATE_OF_REG).toString()
								.equals(dboSpotCheck.getVal(Tc.DATE_OF_REG).toString())) {
					if (checkObj.getVal(Tc.CHECK_SUBJECT).toString()
							.equals(dboSpotCheck.getVal(Tc.CHECK_SUBJECT).toString())) {
						result = true;
						break;
					}
				}
			}
		}
		return result;
	}

	/**
	 * Method that checks if animal has disease in PRE_SLAUGHTER_FORM
	 * 
	 * @param dboAnimal
	 * @param svr
	 * @return
	 * @throws SvException
	 */
	public Boolean checkIfAnimalHasAnyDiseaseInPreSlaughtForms(DbDataObject dboAnimal, SvReader svr)
			throws SvException {
		Boolean result = true;
		DbDataArray preMortemForms = svr.getObjectsByParentId(dboAnimal.getObject_id(),
				SvReader.getTypeIdByName(Tc.PRE_SLAUGHT_FORM), null, 0, 0);
		if (preMortemForms != null && !preMortemForms.getItems().isEmpty()) {
			for (DbDataObject dboPreMortem : preMortemForms.getItems()) {
				if (dboPreMortem.getVal(Tc.DISEASE) != null
						&& dboPreMortem.getVal(Tc.DISEASE).toString().length() > 0) {
					result = false;
					break;
				}
			}
		}
		return result;
	}

	/**
	 * Method that checks if ANIMAL/FLOCK have Pre-Mortem form that is with
	 * negative/suspicious decision for slaughtering
	 * 
	 * @param dbo
	 * @param svr
	 * @return
	 * @throws SvException
	 */
	public Boolean checkIfFlockHaveNegativeOrSuspiciousDecisionInPreSlaughtForm(DbDataObject dbo, SvReader svr)
			throws SvException {
		Boolean result = false;
		DbDataArray preMortemForms = svr.getObjectsByParentId(dbo.getObject_id(),
				SvReader.getTypeIdByName(Tc.PRE_SLAUGHT_FORM), null, 0, 0);
		if (preMortemForms != null && !preMortemForms.getItems().isEmpty()) {
			for (DbDataObject dboPreMortem : preMortemForms.getItems()) {
				if (dboPreMortem.getVal(Tc.DECISION) != null
						&& !dboPreMortem.getVal(Tc.DECISION).toString().equals("1")) {
					result = true;
					break;
				}
			}
		}
		return result;
	}

	/**
	 * Method that checks if animal is part of some valid VACC_EVENT and checks if
	 * there is any disease recorded
	 * 
	 * @param dboAnimal ANIMAL object
	 * @param svr       SvReader instance
	 * @return
	 * @throws SvException
	 */
	public boolean checkIfAnimalHasAnyActiveVaccEventForSpecificDisease(DbDataObject dboAnimal, String slaughterDate,
			Reader rdr, SvReader svr) throws SvException {
		Boolean result = false;
		DbDataObject dboVaccinationEvent = null;
		DateTime dtDeathDate = null;
		if (slaughterDate != null && isStringValidDate(slaughterDate)) {
			dtDeathDate = new DateTime(slaughterDate);
		} else {
			dtDeathDate = new DateTime();
		}
		DbDataArray arrVaccinationBooks = rdr.getLinkedVaccinationBooksPerAnimalOrFlock(dboAnimal, svr);
		if (!arrVaccinationBooks.getItems().isEmpty()) {
			for (DbDataObject dboVaccinationBook : arrVaccinationBooks.getItems()) {
				if (dboVaccinationBook.getVal(Tc.CAMPAIGN_NAME) != null
						&& dboVaccinationBook.getVal(Tc.VACC_DATE) != null) {
					dboVaccinationEvent = rdr.getVaccEventByName(dboVaccinationBook.getVal(Tc.CAMPAIGN_NAME).toString(),
							svr);
					if (dboVaccinationEvent != null && dboVaccinationEvent.getVal(Tc.PROHIBITION_PERIOD) != null) {
						Long prohibitionPeriod = (long) dboVaccinationEvent.getVal(Tc.PROHIBITION_PERIOD);
						if (prohibitionPeriod > 0L) {
							DateTime dtActionDate = new DateTime(dboVaccinationBook.getVal(Tc.VACC_DATE).toString());
							Long daysAfterLastVaccine = getDateDiff(dtDeathDate, dtActionDate, TimeUnit.DAYS);
							if (daysAfterLastVaccine < prohibitionPeriod) {
								result = true;
							}
						}
					}
				}
			}
		}
		return result;
	}

	public Boolean checkIfAnimalHasClassAndRace(DbDataObject dbo, String fieldName) {
		Boolean result = true;
		if (dbo.getVal(fieldName) == null || dbo.getVal(fieldName).toString().equals("")) {
			result = false;
		}
		return result;
	}

	/**
	 * Method that checks if specific object has minimal number of filled fields
	 * 
	 * @param dbo
	 * @param numOfMinimumFilledFields
	 * @return
	 */
	public Boolean checkIfDbDataObjectHasMinimumNumOfFilledFields(DbDataObject dbo, int numOfMinimumFilledFields) {
		Boolean result = false;
		int count = 0;
		for (Entry<SvCharId, Object> entry : dbo.getValuesMap().entrySet()) {
			if (entry.getValue() != null && !entry.getValue().toString().equals("")) {
				count++;
			}
		}
		if (count >= numOfMinimumFilledFields) {
			result = true;
		}
		return result;
	}

	public String calcMovDocumentStatusAccordingMovements(DbDataObject kMovementDocObj, Reader rdr, SvReader svr)
			throws SvException {
		String result = Tc.RELEASED;
		int cntCanceled = 0;
		int cntFinish = 0;
		int cntValid = 0;
		DbDataArray animalOrFlockMovementsArr = rdr.getAnimalMovementsByMovementDoc(kMovementDocObj, true, svr);
		for (DbDataObject tempMovement : animalOrFlockMovementsArr.getItems()) {
			String status = tempMovement.getStatus().toString();
			switch (status) {
			case Tc.VALID:
				cntValid++;
				break;
			case Tc.CANCELED:
				cntCanceled++;
				break;
			case Tc.FINISHED:
				cntFinish++;
				break;
			default:
				break;
			}
		}
		if (cntValid > 0) {
			result = Tc.DRAFT;
		}
		if (animalOrFlockMovementsArr.size() == cntCanceled) {
			result = Tc.CANCELED;
		}
		if ((animalOrFlockMovementsArr.size() == cntFinish) || (cntValid == 0 && cntFinish > 0 && cntCanceled > 0
				&& ((cntFinish + cntCanceled) == animalOrFlockMovementsArr.size()))) {
			result = Tc.RELEASED;
		}
		return result;
	}

	public boolean checkIfAnimalTypeScopeIsApplicableOnSelectedAnimal(List<String> animalTypes, DbDataObject dbo) {
		Boolean result = false;
		String columnName = Tc.ANIMAL_CLASS;
		String animalTypeScope = Tc.EMPTY_STRING;
		if (dbo != null) {
			if (dbo.getObject_type().equals(SvReader.getTypeIdByName(Tc.FLOCK))) {
				columnName = Tc.ANIMAL_TYPE;
				if (dbo.getVal(columnName) != null) {
					switch (dbo.getVal(columnName).toString()) {
					case "1":
						// ovine
						animalTypeScope = "9"; // 9 is Sheep
						break;
					case "2":
						// caprine
						animalTypeScope = "10"; // 10 is Goat
						break;
					case "3":
						// porcine
						animalTypeScope = "11"; // 11 is Pig
						break;
					default:
						break;
					}
				}
			} else if (dbo.getObject_type().equals(SvReader.getTypeIdByName(Tc.PET))) {
				columnName = Tc.PET_TYPE;
				if (dbo.getVal(columnName) != null) {
					switch (dbo.getVal(columnName).toString()) {
					case "1":
						animalTypeScope = "401"; // Dog
						break;
					case "2":
						animalTypeScope = "402"; // Cat
						break;
					case "3":
						animalTypeScope = "403"; // Ferret
						break;
					default:
						break;
					}
				}
			} else {
				if (dbo.getVal(columnName) != null) {
					animalTypeScope = dbo.getVal(columnName).toString();
				}
			}
			if (animalTypes != null && !animalTypes.isEmpty()) {
				for (String animalScope : animalTypes) {
					if (animalScope.equals(animalTypeScope)) {
						result = true;
						break;
					}
				}
			}
		}
		return result;
	}

	public Boolean checkIfLegalEntityKeeperHasAtLeastOnePhysicalEntityAssociatedPerson(DbDataObject dboHolding,
			String linkName, Reader rdr, SvReader svr) throws SvException {
		Boolean result = false;
		DbDataArray arrAssociatedPersons = rdr.getResponsiblePersonRelatedToHoldingDependOnLinkName(dboHolding,
				linkName, svr);
		if (arrAssociatedPersons != null && !arrAssociatedPersons.getItems().isEmpty()) {
			for (DbDataObject dboAssociatedPerson : arrAssociatedPersons.getItems()) {
				if (dboAssociatedPerson.getVal(Tc.HOLDER_TYPE) != null
						&& dboAssociatedPerson.getVal(Tc.HOLDER_TYPE).toString().equals("1")) {
					result = true;
					break;
				}
			}
		}
		return result;
	}

	public Boolean rangeValidationSet(DbDataObject dbo, Reader rdr, String objectType, SvReader svr)
			throws SvException {
		ValidationChecks vc = new ValidationChecks();
		Boolean result = true;
		String status = rdr.getOrderTransferStatusPerRange(dbo, svr);
		if (status.endsWith(Tc.RELEASED) || status.endsWith(Tc.CANCELED)) {
			if (status.startsWith("range_")) {
				throw (new SvException("naits.error.orderStatusIsAlreadyReleaseAndNewRangesCanNotBeAdded",
						svCONST.systemUser, null, null));
			} else {
				throw (new SvException("naits.error.transferWithStatusReleasedCannotBeEdited", svCONST.systemUser, null,
						null));
			}
		}
		if (dbo.getVal(Tc.CHECK_COLUMN) == null) {
			vc.checkIfRangeCanBeSaved(dbo, dbo.getVal(Tc.TAG_TYPE).toString(), (Long) dbo.getVal(Tc.START_TAG_ID),
					(Long) dbo.getVal(Tc.END_TAG_ID), svr);
		}
		DbDataObject dboRangeOrTransfer = svr.getObjectById(dbo.getObject_id(), dbo.getObject_type(), new DateTime());
		if (dbo.getVal(Tc.TAG_TYPE) == null) {
			throw (new SvException("naits.error.transferMustHaveTagType", svCONST.systemUser, null, null));
		}
		String tagType = dbo.getVal(Tc.TAG_TYPE).toString();
		switch (tagType) {
		case "2":
			if (dbo.getVal(Tc.QUANTITY) == null) {
				throw (new SvException("naits.error.rangePerFlocksMustContainQuantityInfo", svCONST.systemUser, null,
						null));
			}
			if (dbo.getVal(Tc.START_TAG_ID) != null || dbo.getVal(Tc.END_TAG_ID) != null) {
				dbo.setVal(Tc.START_TAG_ID, 0L);
				dbo.setVal(Tc.END_TAG_ID, 0L);
			}
			break;
		default:
			if (dbo.getVal(Tc.START_TAG_ID) == null || dbo.getVal(Tc.END_TAG_ID) == null) {
				throw (new SvException("naits.error.rangePerIndividualAnimalsMustContainStartEndTagInfo",
						svCONST.systemUser, null, null));
			}
			if (dbo.getVal(Tc.QUANTITY) != null) {
				throw (new SvException("naits.error.quantityNeedsToBeFilledOnlyWhenTagTypeIsFlock", svCONST.systemUser,
						null, null));
			}
			if (dbo.getObject_id().equals(0L)) {
				Long startTagId = Long.valueOf(dbo.getVal(Tc.START_TAG_ID).toString());
				Long endTagId = Long.valueOf(dbo.getVal(Tc.END_TAG_ID).toString());
				dbo.setVal(Tc.UNIT_NUMBER, String.valueOf(Math.abs(endTagId - startTagId) + 1L));

				if (endTagId < startTagId) {
					throw (new SvException("naits.error.endingTagIdMusteLargerThanStartingTagId", svCONST.systemUser,
							null, null));
				}
				if (dbo.getObject_type().equals(SvReader.getTypeIdByName(Tc.TRANSFER))) {
					if (dbo.getVal(Tc.DIRECT_TRANSFER) == null
							&& !checkIfEarTagRangeInTransferExistsInInventoryItem(dbo, svr)) {
						throw (new SvException("naits.error.inventoryItemDoesntHaveAllEarTagsEnteredInTheRange",
								svCONST.systemUser, null, null));
					}
				}
			}
			break;
		}
		if (rdr.getOrderTransferStatusPerRange(dbo, svr).equals(Tc.RELEASED) && dbo.getDt_delete() != null
				&& dbo.getDt_delete().isBeforeNow()) {
			throw (new SvException("naits.error.rangeCanNotBeDeletedOrderStatusAlreadyReleased", svCONST.systemUser,
					null, null));
		}
		if (dboRangeOrTransfer != null) {
			if (!compareDbObjectFields(dboRangeOrTransfer.getVal(Tc.TAG_TYPE), dbo.getVal(Tc.TAG_TYPE))
					|| !compareDbObjectFields(dboRangeOrTransfer.getVal(Tc.START_TAG_ID), dbo.getVal(Tc.START_TAG_ID))
					|| !compareDbObjectFields(dboRangeOrTransfer.getVal(Tc.END_TAG_ID), dbo.getVal(Tc.END_TAG_ID))) {
				throw (new SvException("naits.error.followingFieldsInTransferCantBeEdited", svCONST.systemUser, null,
						null));
			}
		}

		return result;
	}

	public Boolean checkIfEarTagRangeInTransferExistsInInventoryItem(DbDataObject dboTransfer, SvReader svr)
			throws SvException {
		Boolean result = true;
		Reader rdr = new Reader();
		Long startTagId = Long.valueOf(dboTransfer.getVal(Tc.START_TAG_ID).toString());
		Long endTagId = Long.valueOf(dboTransfer.getVal(Tc.END_TAG_ID).toString());
		String transferTagType = dboTransfer.getVal(Tc.TAG_TYPE).toString();
		Long parentId = dboTransfer.getParent_id();
		String transferType = dboTransfer.getVal(Tc.TRANSFER_TYPE) != null
				? dboTransfer.getVal(Tc.TRANSFER_TYPE).toString()
				: Tc.DEFAULT;
		for (Long i = startTagId; i <= endTagId; i++) {
			String transferEarTag = String.valueOf(i);
			DbDataObject tempDboInventory = rdr.getDboInventoryItemDependOnTransfer(parentId, transferEarTag,
					transferTagType, svr);
			if ((tempDboInventory == null)
					|| (transferType.equals(Tc.REVERSE) && isInventoryItemApplied(tempDboInventory))) {
				result = false;
				break;
			}
		}
		return result;
	}

	public boolean isInventoryItemApplied(DbDataObject dboInventoryItem) {
		boolean result = false;
		if (dboInventoryItem != null && dboInventoryItem.getVal(Tc.TAG_STATUS) != null
				&& dboInventoryItem.getVal(Tc.TAG_STATUS).toString().equals(Tc.APPLIED)) {
			result = true;
		}
		return result;
	}

	public String checkIfAnimalBelongsToSlaughterhouse(Long object_Id, String session_Id) {
		SvReader svr = null;
		String result = Tc.EMPTY_STRING;
		try {
			svr = new SvReader(session_Id);
			DbDataObject dboAnimalOrFlock = svr.getObjectById(object_Id, SvReader.getTypeIdByName(Tc.ANIMAL), null);
			if (dboAnimalOrFlock != null) {
				result = String.valueOf(checkIfHoldingIsSlaughterhouse(dboAnimalOrFlock.getParent_id(), svr));
			} else {
				dboAnimalOrFlock = svr.getObjectById(object_Id, SvReader.getTypeIdByName(Tc.FLOCK), null);
				if (dboAnimalOrFlock != null) {
					result = String.valueOf(checkIfHoldingIsSlaughterhouse(dboAnimalOrFlock.getParent_id(), svr));
				}
			}
		} catch (SvException e) {
			result = e.getLabelCode();
		} finally {
			if (svr != null)
				svr.release();
		}
		return result;
	}

	public Boolean checkIfMovementIsAllowedPerHolding(Long holdingObjId, Reader rdr, SvReader svr) throws SvException {
		Boolean movementAllowed = true;
		DbDataObject dboHolding = svr.getObjectById(holdingObjId, SvReader.getTypeIdByName(Tc.HOLDING), null);
		DbDataArray arrAnimals = svr.getObjectsByParentId(dboHolding.getObject_id(),
				SvReader.getTypeIdByName(Tc.ANIMAL), null, 0, 0);
		if (arrAnimals != null && !arrAnimals.getItems().isEmpty()) {
			for (DbDataObject dboTempAnimal : arrAnimals.getItems()) {
				String movementProhibitions = rdr.getHealthStatusPerAnimalAccordingLabSample(dboTempAnimal, svr);
				if (!movementProhibitions.equals("")) {
					movementAllowed = false;
					break;
				}
			}
			if (movementAllowed) {
				if (dboHolding != null && checkIfHoldingBelongsInActiveSpecificQuarantine(holdingObjId,
						Tc.BLACKLIST_QUARANTINE, svr)) {
					movementAllowed = false;
				}
			}
		}
		return movementAllowed;
	}

	public void beforeSaveNotificationValidationSet(DbDataObject dboToHandle, Reader rdr, SvReader svr)
			throws SvException {
		if (dboToHandle != null) {
			if (dboToHandle.getVal(Tc.TYPE) == null) {
				throw (new SvException("naits.error.pleaseEnterAppropriateNotificationType", svCONST.systemUser, null,
						null));
			}
			if (dboToHandle.getVal(Tc.TITLE) == null) {
				throw (new SvException("naits.error.pleaseEnterAppropriateNotificationTitle", svCONST.systemUser, null,
						null));
			}
			if (dboToHandle.getVal(Tc.TITLE).toString().length() < 3) {
				throw (new SvException("naits.error.notificationShortTitle", svCONST.systemUser, null, null));
			}
			if (dboToHandle.getVal(Tc.MESSAGE) == null) {
				throw (new SvException("naits.error.pleaseEnterAppropriateNotificationMessage", svCONST.systemUser,
						null, null));
			}
			if ((dboToHandle.getVal(Tc.VALID_FROM) != null && dboToHandle.getVal(Tc.VALID_TO) == null)
					|| (dboToHandle.getVal(Tc.VALID_TO) != null && dboToHandle.getVal(Tc.VALID_FROM) == null)
					|| (dboToHandle.getVal(Tc.VALID_FROM) == null && dboToHandle.getVal(Tc.VALID_TO) == null)) {
				throw (new SvException("naits.error.pleaseEnterAppropriateValidNotificationDateFromAndDateTo",
						svCONST.systemUser, null, null));
			}
			DateTime dtValidFrom = new DateTime(dboToHandle.getVal(Tc.VALID_FROM).toString());
			DateTime dtValidTo = new DateTime(dboToHandle.getVal(Tc.VALID_TO).toString());
			if (dtValidFrom.isAfter(dtValidTo)) {
				throw (new SvException("naits.error.dtValidFromMustBeBeforeValidTo", svCONST.systemUser, null, null));
			}
			DbDataObject dboNotification = rdr.getDuplicateNotification(dboToHandle, svr);
			if (dboNotification != null) {
				throw (new SvException("naits.error.notificationExist", svCONST.systemUser, null, null));
			}
		}
	}

	/**
	 * Set of validations for animals/flock in slaughterhouse
	 * 
	 * @param holdingObjectId   Object id of the Holding
	 * @param dboObjectToHandle Animal/Flock object
	 * @param objectTypeLabel   Object type label (animal/flock)
	 * @param svr               SvReader instance
	 * @throws SvException
	 */
	public void slaughterhouseHoldingValidationSet(Long holdingObjectId, DbDataObject dboObjectToHandle,
			String objectTypeLabel, Boolean isSlaughterhouseOperator, SvReader svr) throws SvException {
		if (checkIfHoldingIsSlaughterhouse(holdingObjectId, svr)) {
			if (!dboObjectToHandle.getStatus().equals(Tc.PREMORTEM)) {
				throw (new SvException("naits.error.itemMustHavePremortemStatus", svr.getInstanceUser()));
			}
			if (!checkIfAnimalOrFlockHasAlreadyValidPreMortem(dboObjectToHandle, svr)) {
				throw (new SvException("naits.error.itemHasNotRegisteredPreSlaughtrForms", svr.getInstanceUser()));
			}
			if (!checkIfAnimalHasAnyDiseaseInPreSlaughtForms(dboObjectToHandle, svr)) {
				throw (new SvException("naits.error." + objectTypeLabel + "HasDiseaseSoCanNotBeSlaughtered",
						svr.getInstanceUser()));
			}
			if (dboObjectToHandle.getObject_type().equals(SvReader.getTypeIdByName(Tc.FLOCK))
					&& checkIfFlockHaveNegativeOrSuspiciousDecisionInPreSlaughtForm(dboObjectToHandle, svr)) {
				throw (new SvException("naits.error.flockHasSuspiciousDecisionInPreMortem", svr.getInstanceUser()));
			}
			if (isSlaughterhouseOperator) {
				throw (new SvException("naits.error.slaughterhouseOperatorNotAllowedToSlaughter",
						svr.getInstanceUser()));
			}
		}
	}

	/**
	 * Simple method used for comparing two object values. It converts them in
	 * String
	 * 
	 * @param fieldVal1
	 * @param fieldVal2
	 * @return
	 */
	public Boolean compareDbObjectFields(Object fieldVal1, Object fieldVal2) {
		Boolean result = true;
		if (fieldVal1 != null && fieldVal2 != null && !fieldVal1.toString().equals(fieldVal2.toString())) {
			result = false;
		}
		return result;
	}

	/**
	 * Method that checks if HOLDING belongs to affected area (HIGH_RISK/LOW_RISK)
	 * 
	 * @param dboHolding
	 * @param svr
	 * @return
	 * @throws SvException
	 */
	public Boolean checkIfHoldingBelongsToAffectedArea(DbDataObject dboHolding, SvReader svr) throws SvException {
		Boolean result = false;
		Reader rdr = new Reader();
		DbDataObject dboArea = rdr.findAppropriateAreaByCode(dboHolding.getVal(Tc.VILLAGE_CODE).toString(), "3", svr);
		String areaHealthStatus = rdr.findAppropriateHealthStatusForArea(dboArea, svr);
		if (!areaHealthStatus.equals(Tc.FREE)) {
			result = true;
		}
		return result;
	}

	/**
	 * Method that checks if there is any duplicate record of same Campaign
	 * 
	 * @param listOfCurrentVaccBooks
	 * @param nameOfCampaign
	 * @return
	 */
	public boolean checkIfAnimalParticipatedInVaccinationEvent(DbDataArray listOfCurrentVaccBooks,
			String nameOfCampaign) {
		boolean result = false;
		if (listOfCurrentVaccBooks != null && !listOfCurrentVaccBooks.getItems().isEmpty()) {
			for (DbDataObject dboVaccBook : listOfCurrentVaccBooks.getItems()) {
				if (dboVaccBook.getVal(Tc.CAMPAIGN_NAME) != null
						&& nameOfCampaign.equals(dboVaccBook.getVal(Tc.CAMPAIGN_NAME).toString())) {
					result = true;
					break;
				}
			}
		}
		return result;
	}

	public void updateHoldingStatusAccordingHoldingTypeBeforeSaveAnimalOrFlock(DbDataObject dboAnimalOrFlock,
			SvReader svr) throws SvException {
		Writer wr = new Writer();
		DbDataObject dboCurrentHolding = svr.getObjectById(dboAnimalOrFlock.getParent_id(),
				SvReader.getTypeIdByName(Tc.HOLDING), new DateTime());
		DbDataObject dboOldAnimalVersion = svr.getObjectById(dboAnimalOrFlock.getObject_id(),
				dboAnimalOrFlock.getObject_type(), new DateTime());
		if (dboOldAnimalVersion == null && dboCurrentHolding != null
				&& dboCurrentHolding.getStatus().equals(Tc.SUSPENDED)) {
			dboCurrentHolding.setVal(Tc.CHECK_COLUMN, true);
			if (dboCurrentHolding.getStatus().equals(Tc.SUSPENDED)) {
				wr.updateHoldingStatus(dboCurrentHolding, Tc.SUSPENDED, Tc.VALID, svr);
			}
		}
	}

	public boolean checkIfHoldingIsCommercialOrSubsistenceFarmType(DbDataObject dboHolding) {
		boolean result = false;
		if (dboHolding.getVal(Tc.TYPE) != null) {
			String holdingType = dboHolding.getVal(Tc.TYPE).toString();
			if (holdingType.equals("5") || holdingType.equals("6")) {
				result = true;
			}
		}

		return result;
	}

	public boolean isFlockSlaughterable(DbDataObject dboFlock) {
		boolean result = false;
		if (dboFlock.getVal(Tc.ANIMAL_TYPE) != null) {
			String flockAniType = dboFlock.getVal(Tc.ANIMAL_TYPE).toString();
			if (flockAniType.equals("4") || !flockAniType.equals("6") || !flockAniType.equals("7")) {
				result = true;
			}
		}
		return result;
	}

	public void petValidationSet(DbDataObject dboPet, Writer wr, Reader rdr, SvWriter svw, SvReader svr)
			throws SvException {
		DbDataObject dboPetLastVersion = svr.getObjectById(dboPet.getObject_id(), dboPet.getObject_type(),
				new DateTime());
		DbDataObject dboParentHolding = svr.getObjectById(dboPet.getParent_id(), SvReader.getTypeIdByName(Tc.HOLDING),
				null);
		if (dboPet.getVal(Tc.PET_TYPE) == null) {
			throw (new SvException("naits.error.beforeSaveCheck_pet_PleaseEnterPetType", svCONST.systemUser, null,
					null));
		}
		if (!checkIfFieldIsUnique(Tc.PET_TAG_ID, dboPet, svr)) {
			throw (new SvException("naits.error.beforeSaveCheck_pet_petIdMustBeUnique", svCONST.systemUser, null,
					null));
		}
		if (dboPet.getVal(Tc.PET_TAG_TYPE).toString().equals(Tc.NONE) && dboPet.getVal(Tc.PET_TAG_ID) != null) {
			throw (new SvException("naits.error.tagTypeMustBeDifferentThanNoneIfTagIdIsEntered", svCONST.systemUser,
					null, null));
		}
		if (!dboPet.getVal(Tc.PET_TAG_TYPE).toString().equals(Tc.NONE) && dboPet.getVal(Tc.PET_TAG_ID) == null) {
			throw (new SvException("naits.error.tagTypeMustBeNoneIfTagIdIsNotEntered", svCONST.systemUser, null, null));
		}
		if (dboPetLastVersion != null) {
			if (dboPetLastVersion.getVal(Tc.PET_TAG_ID) != null && dboPet.getVal(Tc.PET_TAG_ID) == null) {
				throw (new SvException("naits.error.beforeSaveCheck_pet_petIdCannotBeEdited", svCONST.systemUser, null,
						null));
			}
			if (dboPetLastVersion.getVal(Tc.PET_TAG_ID) != null
					&& !dboPetLastVersion.getVal(Tc.PET_TAG_ID).toString()
							.equals(dboPet.getVal(Tc.PET_TAG_ID).toString())
					&& dboPet.getVal(Tc.CHECK_COLUMN) == null) {
				throw (new SvException("naits.error.beforeSaveCheck_pet_petIdCannotBeEdited", svCONST.systemUser, null,
						null));
			}
			if (dboPet.getVal(Tc.ADDITIONAL_STATUS) != null) {
				String additionalStatus = dboPet.getVal(Tc.ADDITIONAL_STATUS).toString();
				if ((additionalStatus.equals(Tc.ABANDONED) || additionalStatus.equals(Tc.RETURNED))
						&& !additionalStatus.equals(dboPetLastVersion.getVal(Tc.ADDITIONAL_STATUS))) {
					ArrayList<String> arrInStatuses = new ArrayList<>();
					arrInStatuses.add(Tc.RELEASED);
					arrInStatuses.add(Tc.ADOPTED);
					DbDataObject dboPetMovement = rdr.getPetMovement(dboPet, arrInStatuses, svr);
					wr.updatePetOwnerLinkAccordingAdditionalStatus(dboPet, dboPetLastVersion, additionalStatus, rdr,
							svr);
					if (dboParentHolding != null) {
						wr.finishPetMovementAndCreatePetLocation(dboPetLastVersion, dboPetMovement, dboParentHolding,
								arrInStatuses, svw, svr);
					}
					dboPet.setStatus(Tc.VALID);
				}
			}
		}
		if (dboPet.getVal(Tc.PET_TAG_ID) != null) {
			String petId = dboPet.getVal(Tc.PET_TAG_ID).toString();
			if (!NumberUtils.isDigits(petId)) {
				throw (new SvException("naits.error.beforeSaveCheck_pet_petIdMustBeDigitOnly", svCONST.systemUser, null,
						null));
			}
		}
		if (!checkIfAnimalIdIsNotSelfParent(dboPet, svr))
			throw (new SvException("naits.error.beforeSaveCheck_pet_canNotBeSelfParent", svCONST.systemUser, null,
					null));
		// Date checks
		DateTime dtNow = new DateTime();
		if (dboPet.getVal(Tc.BIRTH_DATE) != null) {
			DateTime dtBirth = rdr.getDateFromDBField(Tc.BIRTH_DATE, dboPet);
			if (dtBirth.isAfter(dtNow))
				throw (new SvException("naits.error.beforeSaveCheck_birthDateCanNotBeAfterCurrentDate",
						svCONST.systemUser, null, null));
		}
		if (dboPet.getVal(Tc.REGISTRATION_DATE) != null) {
			DateTime dtRegistration = rdr.getDateFromDBField(Tc.REGISTRATION_DATE, dboPet);
			if (dtRegistration.isAfter(dtNow))
				throw (new SvException("naits.error.beforeSaveCheck_registrationDateIsAfterTheCurrentDate",
						svCONST.systemUser, null, null));
		}
		if (!checkIfAnimalDeathDateIsNotBeforeBirthDate(dboPet, svr))
			throw (new SvException("naits.error.beforeSaveCheck_pet_deathDateCanNotBeBeforeBirthDate",
					svCONST.systemUser, null, null));
		if (!checkIfAnimalRegDateIsNotAfterAnimalBirthDate(dboPet, svr))
			throw (new SvException("naits.error.beforeSaveCheck_pet_regDateCanNotBeBeforeBirthDate", svCONST.systemUser,
					null, null));

		// Parent checks
		if (checkIfAnimalsMotherAndFatherHaveSameAnimalId(dboPet))
			throw (new SvException("naits.error.beforeSaveCheck_pet_parentCanNotBeTheSame", svCONST.systemUser, null,
					null));
		if (dboPet.getVal(Tc.MOTHER_TAG_ID) != null && !dboPet.getVal(Tc.MOTHER_TAG_ID).toString().trim().equals("")) {
			if (!checkIfAnimalMotherTagIdExist(dboPet, svr))
				throw (new SvException("naits.error.beforeSaveCheck_pet_motherTagIdNotRegistered", svCONST.systemUser,
						null, null));
			if (!checkIfAnimalMotherGenderIsValid(dboPet, svr))
				throw (new SvException("naits.error.beforeSaveCheck_pet_invalidMotherGender", svCONST.systemUser, null,
						null));
		}
		if (dboPet.getVal(Tc.FATHER_TAG_ID) != null && !dboPet.getVal(Tc.FATHER_TAG_ID).toString().trim().equals("")) {
			if (!checkIfAnimalFatherTagIdExist(dboPet, svr))
				throw (new SvException("naits.error.beforeSaveCheck_pet_fatherTagIdNotRegistered", svCONST.systemUser,
						null, null));
			if (!checkIfAnimalFatherGenderIsValid(dboPet, svr))
				throw (new SvException("naits.error.beforeSaveCheck_pet_invalidFatherGender", svCONST.systemUser, null,
						null));
		}

		// Stray pet validations
		if (dboPet.getVal(Tc.IS_STRAY_PET) != null && dboPet.getVal(Tc.IS_STRAY_PET).equals("1")) {
			// Check for pet of type Dog
			if (dboPet.getVal(Tc.PET_TYPE) != null && !dboPet.getVal(Tc.PET_TYPE).toString().equals("1")) {
				if (dboPet.getVal(Tc.DT_ADOPTION) != null) {
					throw (new SvException("naits.error.beforeSaveCheck_pet_dtAdoptionIsAvailableOnlyForPetsOfTypeDog",
							svCONST.systemUser, null, null));
				}
				if (dboPet.getVal(Tc.DT_EUTHANASIA) != null) {
					throw (new SvException(
							"naits.error.beforeSaveCheck_pet_dtEuthanasiaIsAvailableOnlyForPetsOfTypeDog",
							svCONST.systemUser, null, null));
				}
			}

			// Date checks
			if (dboPet.getVal(Tc.DT_EUTHANASIA) != null) {
				DateTime dtDeath = rdr.getDateFromDBField(Tc.DT_EUTHANASIA, dboPet);
				dboPet.setVal(Tc.DEATH_DATE, dtDeath);
			}

			if (dboPet.getVal(Tc.DT_ADOPTION) != null && dboPet.getVal(Tc.DT_DEATH) != null) {
				DateTime dtAdoption = rdr.getDateFromDBField(Tc.DT_ADOPTION, dboPet);
				DateTime dtDeath = rdr.getDateFromDBField(Tc.DEATH_DATE, dboPet);
				if (dtAdoption.isAfter(dtDeath))
					throw (new SvException("naits.error.beforeSaveCheck_adoptionDateCannotBeAfterDeathDate",
							svCONST.systemUser, null, null));
			}
		}

		if (checkIfDateIsInFuture(dboPet, Tc.DT_ADOPTION)) {
			throw (new SvException("naits.error.adoptionDateCannotBeInTheFuture", svr.getInstanceUser()));
		}
	}

	public void petAfterSaveValidationSet(DbDataObject dboPet, Writer wr, Reader rdr, SvWriter svw, SvReader svr)
			throws SvException {
		SvGeometry svg = null;
		DbDataObject dboPetLastVersion = null;
		try {
			svg = new SvGeometry(svw);
			svg.setAllowNullGeometry(true);
			DbDataArray arrDboPetHistory = rdr.getDboPetWithHistory(dboPet.getObject_id(), svr);
			if (arrDboPetHistory.size() > 1) {
				dboPetLastVersion = arrDboPetHistory.get(arrDboPetHistory.size() - 2);
			}
			String blockCheck = SvConf.getParam("app_block.disable_animal_check");
			DbDataObject dboPetInventoryItem = rdr.getInventoryItem(dboPet, Tc.NON_APPLIED, false, svr);
			if (blockCheck == null && dboPetInventoryItem != null && dboPet.getVal(Tc.CHECK_COLUMN) == null) {
				dboPetInventoryItem.setParent_id(dboPet.getObject_id());
				dboPetInventoryItem.setVal(Tc.TAG_STATUS, Tc.APPLIED);
				svw.saveObject(dboPetInventoryItem, false);
			}
			// Stray pet location details
			// DbDataObject dboPetShelterHolding =
			// svr.getObjectById(dboPet.getParent_id(),
			// SvReader.getTypeIdByName(Tc.HOLDING), null);
			DbDataArray arrExistingLocation = svr.getObjectsByParentId(dboPet.getObject_id(),
					SvReader.getTypeIdByName(Tc.STRAY_PET_LOCATION), new DateTime(), 0, 0);
			if (dboPet.getVal(Tc.IS_STRAY_PET) != null && dboPet.getVal(Tc.IS_STRAY_PET).toString().equals("1")
					&& arrExistingLocation.getItems().isEmpty()) {
				// Collection location
				// DbDataObject dboPetCollectionLoc =
				// wr.createPetLocationAccordingAnimalShelterHolding(dboPet,
				// dboPetShelterHolding, null, "1", svr);
				// svg.saveGeometry(dboPetCollectionLoc);
			}
			if (dboPet.getVal(Tc.WEIGHT) != null) {
				Long lastWeight = null;
				Long currentWeight = Long.valueOf(dboPet.getVal(Tc.WEIGHT).toString());
				DbDataObject dboNewMeasurement = wr.createMeasurementObject(
						Long.valueOf(dboPet.getVal(Tc.WEIGHT).toString()), Tc.WEIGHT, Tc.MEASURE_KG, new DateTime(),
						dboPet.getObject_id());
				if (dboPetLastVersion == null) {
					svw.saveObject(dboNewMeasurement, false);
				} else {
					if (dboPetLastVersion.getVal(Tc.WEIGHT) != null) {
						lastWeight = Long.valueOf(dboPetLastVersion.getVal(Tc.WEIGHT).toString());
					}
					if (!currentWeight.equals(lastWeight)) {
						// create new measurement object, invalidate old one
						boolean isInvalidated = wr.invalidateLastValidMeasurementObject(dboPet.getObject_id(), Tc.VALID,
								false, svw, svr);
						if (isInvalidated) {
							svw.saveObject(dboNewMeasurement, false);
						}
					}
				}
			}
			svw.dbCommit();
		} catch (SvException e) {
			log4j.error(e);
		} finally {
			if (svg != null) {
				svg.release();
			}
		}
	}

	public void strayPetValidationSet(DbDataObject dboStrayPet, SvReader svr) throws SvException {
		Reader rdr = new Reader();
		DbDataObject dboPet = rdr.getPetByPetId(dboStrayPet.getVal(Tc.PET_TAG_ID).toString(), svr);
		// ID check
		if (dboStrayPet.getVal(Tc.PET_TAG_ID) != null) {
			if (!NumberUtils.isDigits(dboStrayPet.getVal(Tc.PET_TAG_ID).toString())) {
				throw (new SvException("naits.error.beforeSaveCheck_pet_petIdMustBeDigitOnly", svCONST.systemUser, null,
						null));
			}
			if (dboStrayPet.getObject_id().equals(0L) && dboPet != null) {
				throw (new SvException("naits.error.beforeSaveCheck_pet_registredPetHasBeenFoundWithCurrentPetId",
						svCONST.systemUser, null, null));
			}
		}

		if (dboStrayPet.getVal(Tc.PET_TYPE) == null) {
			throw (new SvException("naits.error.beforeSaveCheck_pet_PleaseEnterPetType", svCONST.systemUser, null,
					null));
		}

		// Check for pet of type Dog
		if (dboStrayPet.getVal(Tc.PET_TYPE) != null && !dboStrayPet.getVal(Tc.PET_TYPE).toString().equals("1")
				&& (dboStrayPet.getVal(Tc.DT_EUTHANASIA) != null || dboStrayPet.getVal(Tc.DT_ADOPTION) != null
						|| dboStrayPet.getVal(Tc.DT_DEATH) != null)) {
			throw (new SvException("naits.error.beforeSaveCheck_pet_DateSectionIsAvailableOnlyForPetsOfTypeDog",
					svCONST.systemUser, null, null));
		}

		// Date checks
		if (dboStrayPet.getVal(Tc.DT_EUTHANASIA) != null) {
			DateTime dtDeath = rdr.getDateFromDBField(Tc.DT_EUTHANASIA, dboStrayPet);
			dboStrayPet.setVal(Tc.DT_DEATH, dtDeath);
		}

		if (dboStrayPet.getVal(Tc.DT_ADOPTION) != null && dboStrayPet.getVal(Tc.DT_DEATH) != null) {
			DateTime dtAdoption = rdr.getDateFromDBField(Tc.DT_ADOPTION, dboStrayPet);
			DateTime dtDeath = rdr.getDateFromDBField(Tc.DT_DEATH, dboStrayPet);
			if (dtAdoption.isAfter(dtDeath))
				throw (new SvException("naits.error.beforeSaveCheck_adoptionDateCannotBeAfterDeathDate",
						svCONST.systemUser, null, null));
		}
	}

	public void strayPetLocationValidationSet(DbDataObject dboLocation, SvReader svr) throws SvException {
		SvWriter svw = null;
		SvGeometry svg = null;
		try {
			svw = new SvWriter(svr);
			svg = new SvGeometry(svw);
			svg.setAllowNullGeometry(true);

			DbDataObject dboPet = svr.getObjectById(dboLocation.getParent_id(), SvReader.getTypeIdByName(Tc.PET), null);
			DbDataArray arrPetLocations = svr.getObjectsByParentId(dboLocation.getParent_id(),
					dboLocation.getObject_type(), new DateTime(), 0, 0);
			if (dboLocation.getObject_id().equals(0L) && !arrPetLocations.getItems().isEmpty()) {
				for (DbDataObject dboStrayPetLocation : arrPetLocations.getItems()) {
					if (dboStrayPetLocation.getStatus().equals(Tc.VALID)
							&& dboStrayPetLocation.getVal(Tc.LOCATION_REASON) != null
							&& dboLocation.getVal(Tc.LOCATION_REASON) != null
							&& dboStrayPetLocation.getVal(Tc.LOCATION_REASON).toString()
									.equals(dboLocation.getVal(Tc.LOCATION_REASON).toString())) {
						dboStrayPetLocation.setStatus(Tc.INVALID);
						svg.saveGeometry(dboStrayPetLocation);
					}
				}
			}
			if (Tc.VALID.equals(dboLocation.getStatus()) && dboLocation.getVal(Tc.WEIGHT) != null
					&& !dboLocation.getVal(Tc.WEIGHT).equals(dboPet.getVal(Tc.WEIGHT))) {
				dboPet.setVal(Tc.WEIGHT, dboLocation.getVal(Tc.WEIGHT));
				svw.saveObject(dboPet);
			}
		} catch (SvException e) {
			log4j.error(e);
		} finally {
			if (svw != null) {
				svw.release();
			}
			if (svg != null) {
				svg.release();
			}
		}
	}

	/**
	 * Validation checks for applying Inventory item on Animal
	 * 
	 * @param dbo
	 * @param rdr
	 * @param svr
	 * @return
	 * @throws SvException
	 */
	public Boolean inventoryItemCheck(DbDataObject dbo, Reader rdr, SvReader svr) throws SvException {
		Boolean result = false;
		String blockCheck = SvConf.getParam("app_block.disable_animal_check");
		DbDataObject dboInventoryItem = rdr.getInventoryItem(dbo, Tc.APPLIED, true, svr);
		if (isInventoryItemCheckBlocked(dbo) && blockCheck == null && dboInventoryItem == null) {
			checkIfAnimalIdExistsInInventoryItem(dbo, rdr, svr);
		}
		DbDataObject dboInventoryItemNonApplied = rdr.getInventoryItem(dbo, Tc.NON_APPLIED, true, svr);
		if (blockCheck == null && dboInventoryItemNonApplied != null
				&& dboInventoryItemNonApplied.getParent_id() != 0L) {
			DbDataObject dboOrgUnit = svr.getObjectById(dboInventoryItemNonApplied.getParent_id(),
					svCONST.OBJECT_TYPE_ORG_UNITS, null);
			if (dboOrgUnit != null && (dboOrgUnit.getVal(Tc.ORG_UNIT_TYPE).equals(Tc.HEADQUARTER)
					|| dboOrgUnit.getVal(Tc.ORG_UNIT_TYPE).equals(Tc.REGIONAL_OFFICE))) {
				throw (new SvException("naits.error.inventoryShouldNotBelongToHeadquarterAnyOtherTypeIsAvailable",
						svCONST.systemUser, null, null));
			}
		}
		return result;
	}

	public boolean isInventoryItemCheckBlocked(DbDataObject dboAnimal) {
		Reader rdr = new Reader();
		Boolean result = true;
		Boolean dtBirthCriterium = false;
		String dbAnimalCountry = rdr.getDboAnimalCountry(dboAnimal);
		DateTime dtBirthDate = null;
		DateTime dtRegistrationDate = null;
		if (dboAnimal.getVal(Tc.BIRTH_DATE) != null && dboAnimal.getVal(Tc.REGISTRATION_DATE) != null) {
			dtBirthDate = new DateTime(dboAnimal.getVal(Tc.BIRTH_DATE).toString());
			dtRegistrationDate = new DateTime(dboAnimal.getVal(Tc.REGISTRATION_DATE).toString());
			if (dtRegistrationDate.isAfter(dtBirthDate)) {
				dtBirthCriterium = true;
			}
		}
		String dbAnimalOriginCountry = rdr.getDboAnimalCountry(dboAnimal, true);
		if (!dbAnimalCountry.equals(Tc.GE) || (dbAnimalCountry.equals(Tc.GE) && !dbAnimalOriginCountry.equals(Tc.GE)
				&& !dbAnimalOriginCountry.equals("") && dtBirthCriterium)) {
			result = false;
		}
		return result;
	}

	/**
	 * Method that prevents duplicate campaign participation per Pet
	 * 
	 * @param objectId       Pet object_id
	 * @param vaccEventObjId Vaccination Book / Campaign object_id
	 * @param svr            SvReader instance
	 * @return
	 * @throws SvException
	 */
	public Boolean isPetAllowedToParticipateInCampaign(Long objectId, Long vaccEventObjId, SvReader svr)
			throws SvException {
		Boolean result = false;
		DbSearchCriterion cr1 = new DbSearchCriterion(Tc.PARENT_ID, DbCompareOperand.EQUAL, objectId);
		DbSearchCriterion cr2 = new DbSearchCriterion(Tc.VACC_EVENT_OBJ_ID, DbCompareOperand.EQUAL, vaccEventObjId);
		DbDataArray arr = svr.getObjects(new DbSearchExpression().addDbSearchItem(cr1).addDbSearchItem(cr2),
				SvReader.getTypeIdByName(Tc.PET_HEALTH_BOOK), new DateTime(), 0, 0);
		if (arr.getItems().isEmpty()) {
			result = true;
		}

		return result;
	}

	public void petPassportValidationSet(DbDataObject dbo, Reader rdr, SvReader svr) throws SvException {
		DateTime dtIssiued = null;
		DateTime dtDelivered = null;
		DateTime dtExpiration = null;
		DateTime dtNow = new DateTime();
		DbDataObject dboPassportRequest = svr.getObjectById(dbo.getParent_id(),
				SvReader.getTypeIdByName(Tc.PASSPORT_REQUEST), null);

		DbDataArray arrPetPassports = rdr.searchForObjectWithSingleFilter(SvReader.getTypeIdByName(Tc.PET_PASSPORT),
				"PET_OBJ_ID", String.valueOf(dboPassportRequest.getParent_id()), false, svr);
		if (!arrPetPassports.getItems().isEmpty()) {
			for (DbDataObject dboPetPassport : arrPetPassports.getItems()) {
				if (!dboPetPassport.getObject_id().equals(dbo.getObject_id())
						&& dboPetPassport.getVal(Tc.DT_EXPIRATION) != null
						&& dboPetPassport.getStatus().equals(Tc.VALID)) {
					dtExpiration = new DateTime(dboPetPassport.getVal(Tc.DT_EXPIRATION).toString());
					if (dtNow.isBefore(dtExpiration)) {
						throw (new SvException("naits.error.thereIsExistingValidPetPassport", svCONST.systemUser, null,
								null));
					}
				}
			}
		}

		if (dbo.getVal(Tc.DT_ISSIUED) != null && dbo.getVal(Tc.DT_DELIVERED) != null) {
			dtIssiued = new DateTime(dbo.getVal(Tc.DT_ISSIUED).toString());
			dtDelivered = new DateTime(dbo.getVal(Tc.DT_DELIVERED).toString());
			if (dtIssiued.isAfter(dtDelivered)) {
				throw (new SvException("naits.error.dateOfIssuingCannotBeAfterDateOfDelivery", svCONST.systemUser, null,
						null));
			}
		}
		if (dbo.getVal(Tc.DT_EXPIRATION) != null) {
			dtExpiration = new DateTime(dbo.getVal(Tc.DT_EXPIRATION).toString());
			if (dtDelivered != null && dtExpiration.isBefore(dtDelivered)) {
				throw (new SvException("naits.error.dateOfExpirationCannotBeBeforeDateOfDelivery", svCONST.systemUser,
						null, null));
			}
			if (dtIssiued != null && dtExpiration.isBefore(dtIssiued)) {
				throw (new SvException("naits.error.dateOfExpirationCannotBeBeforeDateOfIssiuing", svCONST.systemUser,
						null, null));
			}
		}
	}

	public void healthPassportValidationSet(DbDataObject dbo, Reader rdr, SvWriter svw, SvReader svr)
			throws SvException {
		if (dbo.getVal(Tc.HOLDING_NAME) == null) {
			throw (new SvException("naits.error.pleaseEnterAppropriateHoldingName", svCONST.systemUser, null, null));
		}
		if (dbo.getVal(Tc.PET_ID) == null) {
			throw (new SvException("naits.error.pleaseEnterAppropriatePetId", svCONST.systemUser, null, null));
		}
		if (dbo.getVal(Tc.DT_VALID_FROM) == null || dbo.getVal(Tc.DT_VALID_TO) == null) {
			throw (new SvException("naits.error.pleaseEnterAppropriateDates", svCONST.systemUser, null, null));
		}
		DbDataObject dboPet = rdr.getPetByPetIdAndPetType(Tc.PET_ID, dbo.getVal(Tc.PET_ID).toString(), null, false,
				svr);
		DbDataObject dboCurrentValidPassport = null;
		if (dboPet != null) {
			if (!dboPet.getObject_id().equals(dbo.getParent_id())) {
				dbo.setParent_id(dboPet.getObject_id());
			}
			dboCurrentValidPassport = rdr.getLastValidHealthPassport(dboPet, new DateTime(), svr);
			DbDataObject dboOwner = rdr.getPetOwner(dboPet, svr);
			if (dboOwner == null) {
				throw (new SvException("naits.error.withoutOwnerPassportCannotBeAdded", svCONST.systemUser, null,
						null));
			}
		}

		DateTime dtValidFrom = new DateTime(dbo.getVal(Tc.DT_VALID_FROM).toString());
		DateTime dtValidTo = new DateTime(dbo.getVal(Tc.DT_VALID_TO).toString());
		if (dbo.getObject_id().equals(0L)) {
			if (dboCurrentValidPassport != null) {
				// if it's still valid
				if (dboCurrentValidPassport.getStatus().equals(Tc.VALID)) {
					dboCurrentValidPassport.setStatus(Tc.INVALID);
					svw.saveObject(dboCurrentValidPassport, false);
				}
			}
		}

		if (dbo.getVal(Tc.DT_VALID_FROM) != null && dbo.getVal(Tc.DT_VALID_TO) != null
				&& dtValidFrom.isAfter(dtValidTo)) {
			throw (new SvException("naits.error.validFromCannotBeAfterValidTo", svCONST.systemUser, null, null));
		}
	}

	public void petHealthBookValidationSet(DbDataObject dbo, Writer wr, Reader rdr, SvReader svr) throws SvException {
		DbDataObject dboEvent = null;
		DbDataObject dboPetHealthBook = svr.getObjectById(dbo.getObject_id(), dbo.getObject_type(), new DateTime());
		if (dbo.getVal(Tc.CAMPAIGN_NAME) != null) {
			if (dboPetHealthBook != null) {
				if (rdr.checkIfObjectHasBeenEditedDependOnSpecificFields(dbo, dboPetHealthBook, Tc.CAMPAIGN_NAME,
						Tc.ACTION_DATE, Tc.ACTIVITY_TYPE, Tc.ACTIVITY_SUBTYPE, Tc.NOTE)) {
					throw (new SvException("naits.error.cannotEditFieldsFulfilledByCampaignData", svCONST.systemUser,
							null, null));
				}
			}
			if (dbo.getObject_id().equals(0L)) {
				dboEvent = rdr.getVaccEventByName(dbo.getVal(Tc.CAMPAIGN_NAME).toString(), svr);
				wr.createOrUpdatePetHealthBookAccordingVaccinationEvent(dbo, dboEvent, null, null, null);
			}
		}
		if (dbo.getObject_id().equals(0L)) {
			dbo.setVal(Tc.VET_OFFICER, svr.getInstanceUser().getVal(Tc.USER_NAME).toString());
			if (dbo.getVal(Tc.CAMPAIGN_NAME) != null) {
				dbo.setVal(Tc.TREATMENT_TYPE, Tc.STATE);
			} else {
				dbo.setVal(Tc.TREATMENT_TYPE, Tc.NON_STATE);
			}
		}
		if (dboEvent != null) {
			DbDataObject dboPet = svr.getObjectById(dbo.getParent_id(), SvReader.getTypeIdByName(Tc.PET), null);
			if (dboPet != null) {
				dbo.setVal(Tc.VACC_EVENT_OBJ_ID, dboEvent.getObject_id());
				if (!isPetAllowedToParticipateInCampaign(dboPet.getObject_id(), Long.valueOf(dboEvent.getObject_id()),
						svr)) {
					throw (new SvException("naits.error.petAlreadyParticipatedInSelectedCampaign", svCONST.systemUser,
							null, null));
				}
				List<String> animalTypes = rdr.getMultiSelectFieldValueAsList(dboEvent, Tc.ANIMAL_TYPE);
				if (!checkIfAnimalTypeScopeIsApplicableOnSelectedAnimal(animalTypes, dboPet)) {
					throw (new SvException("naits.error.petCampaignNotApplicableOnSelectedObjects",
							svr.getInstanceUser()));
				}
			}
		}
		if (dbo.getVal(Tc.ACTION_DATE) != null && dbo.getVal(Tc.DT_EXPIRATION) != null) {
			DateTime dtAction = new DateTime(dbo.getVal(Tc.ACTION_DATE).toString());
			DateTime dtExpiration = new DateTime(dbo.getVal(Tc.DT_EXPIRATION).toString());
			if (dtAction.isAfter(dtExpiration)) {
				throw (new SvException("naits.error.dateOfActionCannotBeAfterDateOfExpiration", svr.getInstanceUser()));
			}
		}
		if (checkIfDateIsInFuture(dbo, Tc.ACTION_DATE)) {
			throw (new SvException("naits.error.actionDateCannotBeInTheFuture", svCONST.systemUser, null, null));
		}
	}

	public void vaccinationEventValidationSet(DbDataObject dbo) throws SvException {
		Reader rdr = new Reader();
		if (dbo.getVal(Tc.EVENT_START) != null && dbo.getVal(Tc.EVENT_END) != null) {
			DateTime dtEventStart = new DateTime(dbo.getVal(Tc.EVENT_START).toString());
			DateTime dtEventEnd = new DateTime(dbo.getVal(Tc.EVENT_END).toString());
			if (dtEventStart.isAfter(dtEventEnd)) {
				throw (new SvException("naits.error.startDateOfEventCannotBeAfterEndDateOfEvent", svCONST.systemUser,
						null, null));
			}
		}

		if (checkIfDateIsInFuture(dbo, Tc.REGISTRATION_DATE)) {
			throw (new SvException("naits.error.registrationDateCannotBeInTheFuture", svCONST.systemUser, null, null));
		}

		if (dbo.getVal(Tc.CAMPAIGN_SCOPE) != null && dbo.getVal(Tc.CAMPAIGN_SCOPE).toString().equals(Tc.PET)) {
			if (dbo.getVal(Tc.ANIMAL_TYPE) != null) {
				List<String> animalTypeScope = rdr.getMultiSelectFieldValueAsList(dbo, Tc.ANIMAL_TYPE);
				for (String animalType : animalTypeScope) {
					if (!(animalType.equals("401") || animalType.equals("402") || animalType.equals("403"))) {
						throw (new SvException("naits.error.petScopeCanBeAppliedOnPetTypeOnly", svCONST.systemUser,
								null, null));
					}
				}
			}
			if (dbo.getVal(Tc.ACTIVITY_TYPE) != null && !(dbo.getVal(Tc.ACTIVITY_TYPE).toString().equals("1")
					|| dbo.getVal(Tc.ACTIVITY_TYPE).toString().equals("2"))) {
				throw (new SvException("naits.error.inapplicableActivityType", svCONST.systemUser, null, null));
			}
			if (dbo.getVal(Tc.DISEASE) != null && !(dbo.getVal(Tc.DISEASE).toString().equals("61")
					|| dbo.getVal(Tc.DISEASE).toString().equals("60") || dbo.getVal(Tc.DISEASE).toString().equals("5")
					|| dbo.getVal(Tc.DISEASE).toString().equals("35") || dbo.getVal(Tc.DISEASE).toString().equals("70")
					|| dbo.getVal(Tc.DISEASE).toString().equals("71"))) {
				throw (new SvException("naits.error.inapplicableDiseaseForPetScope", svCONST.systemUser, null, null));
			}
		}

		// if (dbo.getVal(Tc.CAMPAIGN_SCOPE) != null &&
		// dbo.getVal(Tc.CAMPAIGN_SCOPE).toString().equals(Tc.ANIMAL)
		// && dbo.getVal(Tc.ANIMAL_TYPE) != null) {
		// List<String> animalTypeScope =
		// rdr.getMultiSelectFieldValueAsList(dbo, Tc.ANIMAL_TYPE);
		// for (String animalType : animalTypeScope) {
		// if (animalType.equals("401") || animalType.equals("402") ||
		// animalType.equals("403")) {
		// throw (new
		// SvException("naits.error.animalScopeCanBeAppliedOnAnimalTypeOnly",
		// svCONST.systemUser,
		// null, null));
		// }
		// }
		// }

		if (dbo.getVal(Tc.ACTIVITY_TYPE) != null && dbo.getVal(Tc.DISEASE) != null
				&& dbo.getVal(Tc.ANIMAL_TYPE) != null) {
			HashMap<String, ArrayList<String>> hmActivityAnimalTypeAndDisease = validateActivityTypeWithAnimalTypeAndDiseases(
					dbo.getVal(Tc.CAMPAIGN_SCOPE).toString(), dbo.getVal(Tc.ACTIVITY_TYPE).toString(),
					dbo.getVal(Tc.ANIMAL_TYPE).toString());
			List<String> animalTypeScope = rdr.getMultiSelectFieldValueAsList(dbo, Tc.ANIMAL_TYPE);
			if (hmActivityAnimalTypeAndDisease != null) {
				ArrayList<String> arrAvailableDiseases = new ArrayList<String>();
				if (dbo.getVal(Tc.CAMPAIGN_SCOPE).toString().equals(Tc.HOLDING)
						&& hmActivityAnimalTypeAndDisease.get(Tc.HOLDING) != null) {
					arrAvailableDiseases = hmActivityAnimalTypeAndDisease.get(Tc.HOLDING);
					if (!arrAvailableDiseases.contains(dbo.getVal(Tc.DISEASE).toString())) {
						throw (new SvException("naits.error.holdingScopeNotApplicableWithChosenDisease",
								svCONST.systemUser, null, null));
					}
				} else {
					for (String animalType : animalTypeScope) {
						hmActivityAnimalTypeAndDisease = validateActivityTypeWithAnimalTypeAndDiseases(
								dbo.getVal(Tc.CAMPAIGN_SCOPE).toString(), dbo.getVal(Tc.ACTIVITY_TYPE).toString(),
								animalType);
						if (hmActivityAnimalTypeAndDisease.containsKey(animalType)) {
							arrAvailableDiseases = hmActivityAnimalTypeAndDisease.get(animalType);
							if (!arrAvailableDiseases.contains(dbo.getVal(Tc.DISEASE).toString())) {
								throw (new SvException("naits.error.animalTypeNotApplicableWithChosenDisease",
										svCONST.systemUser, null, null));
							}
						}
					}
				}
			}
		}
	}

	public HashMap<String, ArrayList<String>> validateActivityTypeWithAnimalTypeAndDiseases(String campaignScope,
			String activityType, String animalType) {
		HashMap<String, ArrayList<String>> hmResult = new HashMap<>();
		ArrayList<String> arrCattleAndBuffaloApplicableDiseases = null;
		ArrayList<String> arrSheepAndGoatApplicableDiseases = null;
		ArrayList<String> arrPigApplicableDiseases = null;
		ArrayList<String> arrPetApplicableDiseases = null;
		ArrayList<String> arrHoldngApplicableDiseases = null;
		switch (activityType) {
		case "1":// VACCINE
			switch (animalType) {
			case "1":
			case "2":
				String vaccineCattleAndBuffalo = "3,5,2,1,10,11,14,18,19,20,21,58";
				arrCattleAndBuffaloApplicableDiseases = new ArrayList<>(
						Arrays.asList(vaccineCattleAndBuffalo.split(Tc.COMMA_DELIMITER)));
				hmResult.put(animalType, arrCattleAndBuffaloApplicableDiseases);
				break;
			case "9":
			case "10":
				String vaccineGoatAndSheep = "3,5,2,1,10,12,13,14,17,18,19,20,58,72";
				arrSheepAndGoatApplicableDiseases = new ArrayList<>(
						Arrays.asList(vaccineGoatAndSheep.split(Tc.COMMA_DELIMITER)));
				hmResult.put(animalType, arrSheepAndGoatApplicableDiseases);
				break;
			case "11":
				String vaccinePigs = "2,1,6,9,10,14,15,16,18,19,20";
				arrPigApplicableDiseases = new ArrayList<>(Arrays.asList(vaccinePigs.split(Tc.COMMA_DELIMITER)));
				hmResult.put(animalType, arrPigApplicableDiseases);
				break;
			case "401":
			case "402":
			case "403":
				String vaccinePets = "70,71,5,35,60,61";
				arrPetApplicableDiseases = new ArrayList<>(Arrays.asList(vaccinePets.split(Tc.COMMA_DELIMITER)));
				hmResult.put(animalType, arrPetApplicableDiseases);
				break;
			default:
				break;
			}
			break;
		case "3": // DIAGNOSTIC
			String diagnosticCattleAndBuffalo = "4";
			arrCattleAndBuffaloApplicableDiseases = new ArrayList<>(
					Arrays.asList(diagnosticCattleAndBuffalo.split(Tc.COMMA_DELIMITER)));
			hmResult.put(animalType, arrCattleAndBuffaloApplicableDiseases);
			break;
		case "4": // THERAPETUIC
			if (campaignScope.equals(Tc.HOLDING)) {
				String therapetuicHolding = "68";
				arrHoldngApplicableDiseases = new ArrayList<>(
						Arrays.asList(therapetuicHolding.split(Tc.COMMA_DELIMITER)));
				hmResult.put(Tc.HOLDING, arrHoldngApplicableDiseases);
			} else {
				switch (animalType) {
				case "1":
				case "2":
					String therapetuicCattleAndBuffalo = "20,21,69";
					arrCattleAndBuffaloApplicableDiseases = new ArrayList<>(
							Arrays.asList(therapetuicCattleAndBuffalo.split(Tc.COMMA_DELIMITER)));
					hmResult.put(animalType, arrCattleAndBuffaloApplicableDiseases);
					break;
				case "9":
				case "10":
					String therapetuicSheepAndGoat = "20,69";
					arrSheepAndGoatApplicableDiseases = new ArrayList<>(
							Arrays.asList(therapetuicSheepAndGoat.split(Tc.COMMA_DELIMITER)));
					hmResult.put(animalType, arrSheepAndGoatApplicableDiseases);
					break;
				default:
					break;
				}
			}
			break;
		case "2": // SAMPLING
			if (campaignScope.equals(Tc.HOLDING)) {
				String samplingHolding = "2,1,58,26,27,28,31,33,34,35,39,40,42,43,44,46,47,50,51,62,64,65,67,69";
				arrHoldngApplicableDiseases = new ArrayList<>(Arrays.asList(samplingHolding.split(Tc.COMMA_DELIMITER)));
				hmResult.put(Tc.HOLDING, arrHoldngApplicableDiseases);
			} else {
				switch (animalType) {
				case "1":
				case "2":
					String samplingCattleAndBuffalo = "3,5,2,1,10,11,14,18,19,21,23,58,4,22,24,28,31,33,34,35,16,38,39,40,41,50,62,63,69";
					arrCattleAndBuffaloApplicableDiseases = new ArrayList<>(
							Arrays.asList(samplingCattleAndBuffalo.split(Tc.COMMA_DELIMITER)));
					hmResult.put(animalType, arrCattleAndBuffaloApplicableDiseases);
					break;
				case "9":
				case "10":
					String samplingGoatAndSheep = "3,5,2,1,10,12,13,14,17,18,19,23,58,4,22,24,28,31,33,34,35,16,37,38,39,40,41,50,62,63,66,69,72";
					arrSheepAndGoatApplicableDiseases = new ArrayList<>(
							Arrays.asList(samplingGoatAndSheep.split(Tc.COMMA_DELIMITER)));
					hmResult.put(animalType, arrSheepAndGoatApplicableDiseases);
					break;
				case "11":
					String samplingPigs = "2,1,6,9,10,14,15,16,18,19,4,28,29,31,33,34,38,39,40,41,50,52,69,70,71";
					arrPigApplicableDiseases = new ArrayList<>(Arrays.asList(samplingPigs.split(Tc.COMMA_DELIMITER)));
					hmResult.put(animalType, arrPigApplicableDiseases);
					break;
				default:
					break;
				}
			}
			break;
		default:
			break;
		}
		return hmResult;
	}

	public void vaccinationBookValdidationSet(DbDataObject dbo, Writer wr, Reader rdr, SvWriter svw, SvReader svr)
			throws SvException {
		DbDataObject dboVaccBook = svr.getObjectById(dbo.getObject_id(), dbo.getObject_type(), new DateTime());
		wr.autoAsignSessionUserToObjectField(dbo, Tc.VET_OFFICER, true, svr);
		if (!checkIfDbDataObjectHasMinimumNumOfFilledFields(dbo, 2)) {
			throw (new SvException("naits.error.cantSaveEmptyObject", svCONST.systemUser, null, null));
		}
		if (dbo.getVal(Tc.CAMPAIGN_NAME) != null) {
			if (dboVaccBook != null && rdr.checkIfObjectHasBeenEditedDependOnSpecificFields(dbo, dboVaccBook,
					Tc.CAMPAIGN_NAME, Tc.ACTION_DATE, Tc.ACTIVITY_TYPE, Tc.ACTIVITY_SUBTYPE, Tc.IMMUNIZATION_PERIOD,
					Tc.PROHIBITION_PERIOD)) {
				throw (new SvException("naits.error.cannotEditFieldsFulfilledByCampaignData", svCONST.systemUser, null,
						null));
			}
			wr.setVaccBookDependOnVaccEvent(dbo, svr, svw);
		}
		if (dboVaccBook != null) {
			DbDataObject dboAnimalOrFlockObj = rdr.getAnimalLinkedToVaccinationBook(dboVaccBook, svr);
			if (dboAnimalOrFlockObj != null && dbo.getVal(Tc.NO_ITEMS_TREATED) != null
					&& dboVaccBook.getVal(Tc.NO_ITEMS_TREATED) != null && !dbo.getVal(Tc.NO_ITEMS_TREATED).toString()
							.equals(dboVaccBook.getVal(Tc.NO_ITEMS_TREATED).toString())) {
				throw (new SvException("naits.error.cannotEditNumberOfItemsTreatedInAnimalHealthBook",
						svCONST.systemUser, null, null));
			} else {
				dboAnimalOrFlockObj = rdr.getFlockLinkedToVaccinationBook(dboVaccBook, svr);
				if (dboAnimalOrFlockObj != null && dboAnimalOrFlockObj.getVal(Tc.TOTAL) != null
						&& dbo.getVal(Tc.NO_ITEMS_TREATED) != null
						&& Integer.valueOf(dbo.getVal(Tc.NO_ITEMS_TREATED).toString()) > Integer
								.valueOf(dboAnimalOrFlockObj.getVal(Tc.TOTAL).toString())) {
					throw (new SvException("naits.error.cannotEnterLargerNumberOfTreatedItemsThanTotal",
							svCONST.systemUser, null, null));
				}
			}
		}
		wr.autoSetTreatmentTypeInVaccinationBookDependOnUserGroup(dbo, svr);
		if (checkIfDateIsInFuture(dbo, Tc.VACC_DATE)) {
			throw (new SvException("naits.error.actionDateCannotBeInTheFuture", svCONST.systemUser, null, null));
		}
	}

	public Boolean checkIfPetHasDraftPassportRequest(DbDataObject dboPet, SvReader svr) throws SvException {
		Boolean result = false;
		DbDataArray arrPassRequests = svr.getObjectsByParentId(dboPet.getObject_id(),
				SvReader.getTypeIdByName(Tc.PASSPORT_REQUEST), null, 0, 0);
		if (!arrPassRequests.getItems().isEmpty()) {
			for (DbDataObject dboPassReq : arrPassRequests.getItems()) {
				if (dboPassReq.getStatus().equals(Tc.DRAFT)) {
					result = true;
					break;
				}
			}
		}
		return result;
	}

	/**
	 * Method that returns true if some of the fields of certain object has been
	 * edited
	 * 
	 * @param dbo
	 * @param dboLastVerison
	 * @return
	 */
	public boolean checkIfDbObjectIsEdited(DbDataObject dbo, DbDataObject dboLastVerison) {
		boolean result = false;
		for (Entry<SvCharId, Object> entry : dbo.getValuesMap().entrySet()) {
			Object obj = dboLastVerison.getValuesMap().get(entry.getKey());
			if ((entry.getValue() != null && !entry.getValue().equals(obj))
					|| (obj != null && !obj.equals(entry.getValue()))) {
				result = true;
				break;
			}
		}
		return result;
	}

	public void populationValidationSet(DbDataObject dbo, Writer wr, SvReader svr) throws SvException {
		DbDataObject dboPopulationLastVersion = null;
		DbDataObject dboVaccinationEvent = null;
		// Age
		if (dbo.getVal(Tc.AGE_FILTER) != null && dbo.getVal(Tc.FILTER_AGE_TO) == null
				&& dbo.getVal(Tc.FILTER_AGE_FROM) == null) {
			throw (new SvException("naits.error.enterAppropriateFromValueInAge", svCONST.systemUser, null, null));
		}
		if ((dbo.getVal(Tc.FILTER_AGE_FROM) != null || dbo.getVal(Tc.FILTER_AGE_TO) != null)
				&& dbo.getVal(Tc.AGE_FILTER) == null) {
			throw (new SvException("naits.error.enterAppropriateFilterTypeInAge", svCONST.systemUser, null, null));
		}
		if (dbo.getVal(Tc.FILTER_AGE_TO) != null && dbo.getVal(Tc.FILTER_AGE_FROM) != null
				&& Long.valueOf(dbo.getVal(Tc.FILTER_AGE_TO).toString()) < Long
						.valueOf(dbo.getVal(Tc.FILTER_AGE_FROM).toString())) {
			throw (new SvException("naits.error.ageFromCannotBeLargerThanAgeTo", svCONST.systemUser, null, null));
		}
		// Vaccination book
		if (dbo.getVal(Tc.VACC_BOOK_FILTER) != null) {
			if (dbo.getVal(Tc.VACC_BOOK_FROM) == null && dbo.getVal(Tc.VACC_BOOK_TO) == null) {
				throw (new SvException("naits.error.enterAppropriateFromValueInCampaign", svCONST.systemUser, null,
						null));
			}
		}
		if ((dbo.getVal(Tc.VACC_BOOK_FROM) != null || dbo.getVal(Tc.VACC_BOOK_TO) != null
				|| dbo.getVal(Tc.DISEASE_VACC_BOOK) != null) && dbo.getVal(Tc.VACC_BOOK_FILTER) == null) {
			throw (new SvException("naits.error.enterAppropriateFilterTypeInVaccBook", svCONST.systemUser, null, null));
		}
		// Campaign
		if (dbo.getVal(Tc.VACCINE_FILTER) == null && dbo.getVal(Tc.CAMPAIGN_OBJ_ID) != null) {
			throw (new SvException("naits.error.enterAppropriateDiseasePopulationType", svCONST.systemUser, null,
					null));
		}
		if (dbo.getVal(Tc.VACCINE_FILTER) != null && dbo.getVal(Tc.CAMPAIGN_OBJ_ID) == null) {
			throw (new SvException("naits.error.pleaseSelectAppropriateCampaign", svCONST.systemUser, null, null));
		}
		if (dbo.getVal(Tc.CAMPAIGN_OBJ_ID) != null) {
			if (dbo.getVal(Tc.VACCINE_FILTER) == null) {
				throw (new SvException("naits.error.enterAppropriateDiseasePopulationType", svCONST.systemUser, null,
						null));
			}
			dboVaccinationEvent = svr.getObjectById(Long.valueOf(dbo.getVal(Tc.CAMPAIGN_OBJ_ID).toString()),
					SvReader.getTypeIdByName(Tc.VACCINATION_EVENT), null);
			if (dboVaccinationEvent.getVal(Tc.DISEASE) != null) {
				dbo.setVal(Tc.DISEASE_VACCINATION, dboVaccinationEvent.getVal(Tc.DISEASE).toString());
			}
		}
		if (dbo.getVal(Tc.FILTER_VACCINATION_TO) != null && dbo.getVal(Tc.FILTER_VACCINATION_FROM) != null
				&& Long.valueOf(dbo.getVal(Tc.FILTER_VACCINATION_TO).toString()) < Long
						.valueOf(dbo.getVal(Tc.FILTER_VACCINATION_FROM).toString())) {
			throw (new SvException("naits.error.vaccineFromCannotBeLargerThanAgeTo", svCONST.systemUser, null, null));
		}
		// Sample
		if (dbo.getVal(Tc.SAMPLE_FILTER) != null && dbo.getVal(Tc.FILTER_HEALTH_CHECK_TO) == null
				&& dbo.getVal(Tc.FILTER_HEALTH_CHECK_FROM) == null) {
			throw (new SvException("naits.error.enterAppropriateFromValueInSample", svCONST.systemUser, null, null));
		}
		if (dbo.getVal(Tc.FILTER_HEALTH_CHECK_TO) != null && dbo.getVal(Tc.FILTER_HEALTH_CHECK_FROM) != null
				&& Long.valueOf(dbo.getVal(Tc.FILTER_HEALTH_CHECK_TO).toString()) < Long
						.valueOf(dbo.getVal(Tc.FILTER_HEALTH_CHECK_FROM).toString())) {
			throw (new SvException("naits.error.sampleFromCannotBeLargerThanAgeTo", svCONST.systemUser, null, null));
		}
		if (dbo.getVal(Tc.FILTER_HEALTH_CHECK_FROM) != null || dbo.getVal(Tc.FILTER_HEALTH_CHECK_TO) != null) {
			if (dbo.getVal(Tc.SAMPLE_FILTER) == null) {
				throw (new SvException("naits.error.enterAppropriateSamplePopulationType", svCONST.systemUser, null,
						null));
			}
			if (dbo.getVal(Tc.DISEASE_STATUS) == null) {
				throw (new SvException("naits.error.diseaseIsMissingForSampleFilter", svCONST.systemUser, null, null));
			}
		}
		// Ear tag/Inventory item
		if (dbo.getVal(Tc.EAR_TAG_FILTER) != null && dbo.getVal(Tc.FILTER_MISSING_TAG_FROM) == null
				&& dbo.getVal(Tc.FILTER_MISSING_TAG_TO) == null) {
			throw (new SvException("naits.error.enterAppropriateFiltersInInventory", svCONST.systemUser, null, null));
		}
		if ((dbo.getVal(Tc.FILTER_MISSING_TAG_FROM) != null || dbo.getVal(Tc.FILTER_MISSING_TAG_TO) != null)
				&& dbo.getVal(Tc.EAR_TAG_FILTER) == null) {
			throw (new SvException("naits.error.enterAppropriateFilterTypeInEarTag", svCONST.systemUser, null, null));
		}
		if (dbo.getObject_id().equals(0L)) {
			String populationId = wr.generatePopulationId(dbo, svr);
			dbo.setVal(Tc.POPULATION_ID, populationId);
			dbo.setVal(Tc.DT_CREATION, new DateTime());
		} else {
			dboPopulationLastVersion = svr.getObjectById(dbo.getObject_id(), dbo.getObject_type(), new DateTime());
			if (dboPopulationLastVersion.getStatus().equals(Tc.FINAL)
					&& checkIfDbObjectIsEdited(dbo, dboPopulationLastVersion)) {
				throw (new SvException("naits.error.cannotEditPopulationWithFinalStatus", svCONST.systemUser, null,
						null));
			}
		}
	}

	public boolean checkIfDateIsInFuture(DbDataObject dbo, String fieldName) {
		boolean result = false;
		if (dbo != null && dbo.getVal(fieldName) != null) {
			DateTime dtDate = new DateTime(dbo.getVal(fieldName).toString());
			if (dtDate.isAfter(new DateTime())) {
				result = true;
			}
		}
		return result;
	}

	public void checkIfHolderTypeCanBeUpdated(DbDataObject dbo, SvReader svr) throws SvException {
		DbDataObject dboHoldingResponsible = svr.getObjectById(dbo.getObject_id(), dbo.getObject_type(), null);
		if (dboHoldingResponsible.getVal(Tc.IS_PROCESSED) != null
				&& dboHoldingResponsible.getVal(Tc.IS_PROCESSED).equals(true)
				&& dboHoldingResponsible.getStatus().equals(Tc.VALID)
				&& dboHoldingResponsible.getVal(Tc.HOLDER_TYPE) != null
				&& dboHoldingResponsible.getVal(Tc.HOLDER_TYPE).toString().equals("1")
				&& dbo.getVal(Tc.HOLDER_TYPE) != null && dbo.getVal(Tc.HOLDER_TYPE).toString().equals("2")) {
			throw new SvException("naits.error.cannotEditTypeOnValidPerson", svCONST.systemUser);
		}
	}

	/**
	 * Method that checks if input string is in valid {@link DateTime} format by
	 * trying to create new DateTime instance with the input string as parameter
	 * 
	 * @param date
	 * @return
	 */
	public boolean isStringValidDate(String date) {
		boolean result = true;
		try {
			@SuppressWarnings("unused")
			DateTime dt = new DateTime(date);
		} catch (Exception e) {
			result = false;
		}
		return result;
	}

	/**
	 * Method that prohibits animal movement if animal country is not GE.
	 * 
	 * @param animalObjId
	 * @param svr
	 * @return
	 * @throws SvException
	 */
	public Boolean checkIfMovementIsAllowedPerAnimalCountry(DbDataObject dboAnimal, SvReader svr) throws SvException {
		Boolean movementAllowed = true;
		if (dboAnimal != null && dboAnimal.getVal(Tc.COUNTRY) != null
				&& !dboAnimal.getVal(Tc.COUNTRY).toString().equals(Tc.GE)) {
			movementAllowed = false;
		}
		return movementAllowed;
	}

	/**
	 * Method that checks if quarantine's date_from is after quarantine's date_to.
	 * 
	 * @param dbo
	 * @param svr
	 * @return
	 */
	public Boolean isQuarantineStartDateAfterEndDate(DbDataObject dbo) {
		Boolean isAfter = false;
		DateTime dtFrom = new DateTime(dbo.getVal(Tc.DATE_FROM).toString());
		DateTime dtTo = new DateTime(dbo.getVal(Tc.DATE_TO).toString());
		if (dtFrom.isAfter(dtTo)) {
			isAfter = true;
		}
		return isAfter;
	}

	public void rfidValidationSet(DbDataObject dbo, Writer wr, Reader rdr, SvReader svr) throws SvException {
		if (dbo.getVal(Tc.ANIMAL_TYPE) == null) {
			throw new SvException("naits.error.missing_animal_type", svr.getInstanceUser());
		}
		if (dbo.getVal(Tc.IMPORT_TYPE) != null) {
			if (dbo.getVal(Tc.IMPORT_TYPE).toString().equals(Tc.VIA_FILE)) {
				List<String> earTagList = rdr.getRFIDEarTagsViaFile(dbo);
				if (earTagList == null || earTagList.isEmpty()) {
					throw new SvException("naits.error.empty_file_cannot_be_processed", svr.getInstanceUser());
				}
			} else {
				if (dbo.getVal(Tc.TEXT_EAR_TAGS) == null
						|| dbo.getVal(Tc.TEXT_EAR_TAGS).toString().trim().length() < 1) {
					throw new SvException("naits.error.missing_ear_tags_inserted", svr.getInstanceUser());
				}
			}
		}
		if (dbo.getVal(Tc.RFID_NUMBER) == null) {
			String rfidInputSeqNumber = wr.generateRFIDSeqNumber(dbo.getObject_type(), svr);
			dbo.setVal(Tc.RFID_NUMBER, rfidInputSeqNumber);
		}
	}

	public boolean checkIfActionTypeIsAlreadyExecutedOnRfidInputState(DbDataObject dboRfidInputState,
			String currentAction, Reader rdr) {
		boolean result = false;
		if (dboRfidInputState.getVal(Tc.EXECUTED_ACTIONS) != null) {
			List<String> executedActions = rdr.getMultiSelectFieldValueAsList(dboRfidInputState, Tc.EXECUTED_ACTIONS);
			if (!executedActions.isEmpty()) {
				for (String executedAction : executedActions) {
					if (executedAction.trim().equals(currentAction)) {
						result = true;
						break;
					}
				}
			}
		}
		return result;
	}

	public boolean checkIfRfidInputStateAllowedToBeProcessedInMassAction(DbDataObject dboRfidInputState,
			String actionType, Reader rdr, SvReader svr) throws SvException {
		boolean result = true;
		if (dboRfidInputState.getStatus().equals(Tc.NOT_FOUND)) {
			DbDataObject rfidInputStateParent = svr.getObjectById(dboRfidInputState.getParent_id(),
					SvReader.getTypeIdByName(Tc.RFID_INPUT), null);
			if (rfidInputStateParent != null) {
				DbDataObject dboAnimal = null;
				String rfidString = dboRfidInputState.getVal(Tc.ANIMAL_EAR_TAG).toString();
				String animalClass = rfidInputStateParent.getVal(Tc.ANIMAL_TYPE).toString();
				if (rfidString.length() > 8) {
					String animalIdViaRfid = rfidString.substring(rfidString.length() - 8);
					if (!animalIdViaRfid.isEmpty()) {
						dboAnimal = rdr.findAppropriateAnimalByAnimalIdAndAnimalClass(animalIdViaRfid, animalClass,
								true, svr);
					}
				} else {
					dboAnimal = rdr.findAppropriateAnimalByAnimalIdAndAnimalClass(rfidString, animalClass, true, svr);
				}
				if (dboAnimal == null) {
					result = false;
					if (actionType.equals(Tc.REGISTRATION)) {
						result = true;
					}
				}
			}
		}
		if (!actionType.equalsIgnoreCase(Tc.REGISTRATION) && (dboRfidInputState.getStatus().equals(Tc.NONAPPLIED))) {
			result = false;
		}
		return result;
	}

	public DbDataObject validationSetForGeneratingRfidInputResult(String actionType, String animalBreed,
			Long destinationObjectId, Long destinationObjectType, DbDataObject dboRfidInput, DateTime actionDate,
			SvReader svr) throws SvException {
		DbDataObject dboDestinationObject = null;
		if (actionType == null) {
			throw new SvException("naits.error.missing_action_type", svr.getInstanceUser());
		}
		if (dboRfidInput == null) {
			throw new SvException("naits.error.missing_rfid_input", svr.getInstanceUser());
		}
		if (actionDate == null) {
			actionDate = new DateTime();
		}

		if (!actionType.equals(Tc.ACTION)) {
			if (actionType.equals(Tc.REGISTRATION)) {
				if (animalBreed == null) {
					throw new SvException("naits.error.missing_animal_breed", svr.getInstanceUser());
				}
				if (destinationObjectId == null) {
					throw new SvException("naits.error.missing_destination_number", svr.getInstanceUser());
				}
				dboDestinationObject = svr.getObjectById(destinationObjectId, destinationObjectType, null);
				if (dboDestinationObject == null) {
					throw new SvException("naits.error.no_destination_holding_found", svr.getInstanceUser());
				}
				if (dboDestinationObject.getVal(Tc.TYPE) != null
						&& (dboDestinationObject.getVal(Tc.TYPE).toString().equals(Tc.VET_STATION_TYPE)
								|| dboDestinationObject.getVal(Tc.TYPE).toString().equals(Tc.ANIMAL_SHELTER_TYPE))) {
					throw new SvException("naits.error.register_animal_in_shelter_or_vet_station",
							svr.getInstanceUser());
				}
			} else if (actionType.equals(Tc.TRANSFER)) {
				if (destinationObjectId == null) {
					throw new SvException("naits.error.missing_destination_number", svr.getInstanceUser());
				}
				dboDestinationObject = svr.getObjectById(destinationObjectId, destinationObjectType, null);
				if (dboDestinationObject == null) {
					throw new SvException("naits.error.no_destination_holding_found", svr.getInstanceUser());
				}
			} else if (actionType.equals(Tc.MOVE_TO_CERTIFICATE)) {
				if (destinationObjectId == null) {
					throw new SvException("naits.error.missing_destination_number", svr.getInstanceUser());
				}
				dboDestinationObject = svr.getObjectById(destinationObjectId, destinationObjectType, null);
				if (dboDestinationObject == null) {
					throw new SvException("naits.error.no_export_certificate_found", svr.getInstanceUser());
				}
			}
		}
		return dboDestinationObject;
	}

	public boolean checkIfAnimalCanBeMovedToExportCertificate(DbDataObject dboExportCertificate, DbDataObject dboAnimal,
			SvReader svr) throws SvException {
		Reader rdr = new Reader();
		boolean result = true;
		DbDataObject dboQuaranitne = svr.getObjectById(dboExportCertificate.getParent_id(),
				SvReader.getTypeIdByName(Tc.QUARANTINE), null);
		DbDataObject dboHolding = rdr.getHoldingLinkedPerExportQuaranitne(dboQuaranitne, svr);
		if (dboHolding != null && !dboAnimal.getParent_id().equals(dboHolding.getObject_id())) {
			result = false;
		}
		return result;
	}

	public ArrayList<String> checkIfAllItemsInTransferRangeBelongToOrgUnit(Long senderOrgUnitId,
			Long transferStartRange, Long transferEndRange, String transferTagType, SvReader svr) throws SvException {
		Reader rdr = new Reader();
		ArrayList<String> missingInvItems = new ArrayList<String>();

		// initiate ownership check
		for (Long i = transferStartRange; i <= transferEndRange; i++) {
			String earTagNo = i.toString();
			DbDataObject dboInvItem = rdr.getDboInventoryItem(earTagNo, transferTagType, svr);
			if (dboInvItem == null || !dboInvItem.getParent_id().equals(senderOrgUnitId)) {
				missingInvItems.add(earTagNo);
			}
		}

		return missingInvItems;
	}

	/**
	 * Method that prevent creating duplicate animals - (Goat/Sheep),
	 * (Horse/Donkey), (Cattle/Buffalo)
	 * 
	 * @param dbo DbDataObject of animal
	 * @param rdr Reader instance
	 * @param svr SvReader instance
	 * @throws SvException
	 */
	public void checksToPreventCreatingDuplicatesForEquidsAndRuminants(DbDataObject dbo, Reader rdr, SvReader svr)
			throws SvException {
		String currIdNo = dbo.getVal(Tc.ANIMAL_ID).toString();
		DbDataObject dboAnimalWithSameId = rdr.findAppropriateAnimalByAnimalId(currIdNo, svr);
		if (dboAnimalWithSameId != null) {
			if (dbo.getVal(Tc.ANIMAL_CLASS).toString().equals("12")
					|| dbo.getVal(Tc.ANIMAL_CLASS).toString().equals("400")
							&& (dboAnimalWithSameId.getVal(Tc.ANIMAL_CLASS).toString().equals("12")
									|| dboAnimalWithSameId.getVal(Tc.ANIMAL_CLASS).toString().equals("400"))) {
				throw (new SvException("naits.error.beforeSaveCheck_AnimalIdForEquidsMustBeUnique", svCONST.systemUser,
						null, null));
			}
			if (dbo.getVal(Tc.ANIMAL_CLASS).toString().equals("9")
					|| dbo.getVal(Tc.ANIMAL_CLASS).toString().equals("10")
							&& (dboAnimalWithSameId.getVal(Tc.ANIMAL_CLASS).toString().equals("9")
									|| dboAnimalWithSameId.getVal(Tc.ANIMAL_CLASS).toString().equals("10"))) {
				throw (new SvException("naits.error.beforeSaveCheck_AnimalIdForRuminantsMustBeUnique",
						svCONST.systemUser, null, null));
			}
			if (dbo.getVal(Tc.ANIMAL_CLASS).toString().equals("1") || dbo.getVal(Tc.ANIMAL_CLASS).toString().equals("2")
					&& (dboAnimalWithSameId.getVal(Tc.ANIMAL_CLASS).toString().equals("1")
							|| dboAnimalWithSameId.getVal(Tc.ANIMAL_CLASS).toString().equals("2"))) {
				throw (new SvException("naits.error.beforeSaveCheck_AnimalIdForCattleAndBuffaloMustBeUnique",
						svCONST.systemUser, null, null));
			}
		}
	}

	public void petQuarantineValidationSet(DbDataObject dbo, Reader rdr, SvWriter svw, SvReader svr)
			throws SvException {
		if (new DateTime(dbo.getVal(Tc.DATE_FROM).toString()).isAfter(new DateTime())) {
			throw (new SvException("naits.error.validation_check_dateFromCanNotBeInTheFuture", svCONST.systemUser, null,
					null));
		}
		if (new DateTime(dbo.getVal(Tc.DATE_TO).toString())
				.isBefore(new DateTime(dbo.getVal(Tc.DATE_FROM).toString()))) {
			throw (new SvException("naits.error.validation_check_dateToCanNotBeBeforDateFrom", svCONST.systemUser, null,
					null));
		}

	}
}