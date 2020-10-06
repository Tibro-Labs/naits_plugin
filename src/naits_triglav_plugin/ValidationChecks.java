package naits_triglav_plugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.apache.commons.lang.math.NumberUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.prtech.svarog.SvConf;
import com.prtech.svarog.SvCore;
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
		replacedEarTags.getSortedItems("DT_INSERT", true);
		if (replacedEarTags != null && replacedEarTags.size() > 0) {
			DbDataObject tempEarTagReplc = replacedEarTags.get(0);
			if (tempEarTagReplc != null && tempEarTagReplc.getVal(Tc.OLD_EAR_TAG) != null && !tempEarTagReplc
					.getVal(Tc.OLD_EAR_TAG).toString().equals(animalObj.getVal(Tc.ANIMAL_ID).toString())) {
				result = true;
			}
		}
		return result;
	}

	public Boolean checkIfWrongEnteredEarTagExists(DbDataObject animalObj, SvReader svr) throws SvException {
		Boolean result = false;
		DbDataArray replacedEarTags = svr.getObjectsByParentId(animalObj.getObject_id(),
				SvReader.getTypeIdByName(Tc.EAR_TAG_REPLC), null, 0, 0);
		if (!replacedEarTags.getItems().isEmpty()) {
			for (DbDataObject dboEarTagReplc : replacedEarTags.getItems()) {
				if (dboEarTagReplc.getVal(Tc.REASON) != null
						&& dboEarTagReplc.getVal(Tc.REASON).toString().equals(Tc.WRONG_ENTRY)) {
					result = true;
					break;
				}
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
			if (childs != null && childs.size() > 0) {
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
	 * Method that checks if departure date is not before arrival date of
	 * current animal movement
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
	 * Method that checks animal movement if arrival date is not before
	 * departure date
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

	public Boolean checkIfHoldingIsSlaughterhouse(Long holdingID, SvReader svr) throws SvException {
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
	public Boolean checkIfHoldingIsSlaughterhouse(DbDataObject dboHolding) {
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
	 * Method that checks animal movement if specific holding belongs in active
	 * quarantine
	 * 
	 * @param currentAnimalMovement
	 * @param svr
	 * @throws SvException
	 */
	public Boolean checkIfHoldingBelongsInActiveQuarantine(Long dboHoldingId, SvReader svr) throws SvException {
		Boolean result = false;
		DbDataObject dboHolding = svr.getObjectById(dboHoldingId, SvReader.getTypeIdByName(Tc.HOLDING), null);
		if (dboHolding != null)
			result = checkIfHoldingBelongsInActiveQuarantine(dboHolding, svr);
		return result;
	}

	/**
	 * Method that checks if holding is linked to active quarantine
	 * 
	 * @param dboHolding
	 * @param svr
	 * @throws SvException
	 */
	public Boolean checkIfHoldingBelongsInActiveQuarantine(DbDataObject dboHolding, SvReader svr) throws SvException {
		Boolean result = false;
		DbDataObject dbLink = SvLink.getLinkType(Tc.HOLDING_QUARANTINE, SvReader.getTypeIdByName(Tc.HOLDING),
				SvReader.getTypeIdByName(Tc.QUARANTINE));
		DbDataArray dbaQuarantines = null;
		if (dboHolding != null && dbLink != null) {
			dbaQuarantines = svr.getObjectsByLinkedId(dboHolding.getObject_id(), dbLink, null, 0, 0);
			if (dbaQuarantines != null && dbaQuarantines.size() > 0) {
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
	 * Method that checks animal movement if specific holding belongs only in
	 * active export quarantine
	 * 
	 * @param currentAnimalMovement
	 * @param svr
	 * @throws SvException
	 */
	public Boolean checkIfHoldingBelongsInActiveDiseaseQuarantine(Long dboHoldingId, SvReader svr) throws SvException {
		Boolean result = false;
		DbDataObject dboHolding = svr.getObjectById(dboHoldingId, SvReader.getTypeIdByName(Tc.HOLDING), null);
		if (dboHolding != null)
			result = checkIfHoldingBelongsInActiveDiseaseQuarantine(dboHolding, svr);
		return result;
	}

	/**
	 * Method that checks if holding is linked only to active blacklist
	 * quarantine
	 * 
	 * @param dboHolding
	 * @param svr
	 * @throws SvException
	 */
	public Boolean checkIfHoldingBelongsInActiveDiseaseQuarantine(DbDataObject dboHolding, SvReader svr)
			throws SvException {
		Boolean result = false;
		DbDataObject dbLink = SvLink.getLinkType(Tc.HOLDING_QUARANTINE, SvReader.getTypeIdByName(Tc.HOLDING),
				SvReader.getTypeIdByName(Tc.QUARANTINE));
		DbDataArray dbaQuarantines = null;
		if (dboHolding != null && dbLink != null) {
			dbaQuarantines = svr.getObjectsByLinkedId(dboHolding.getObject_id(), dbLink, null, 0, 0);
			if (dbaQuarantines != null && dbaQuarantines.size() > 0) {
				for (DbDataObject tempQuarantine : dbaQuarantines.getItems()) {
					if (checkIfQuarantineActive(tempQuarantine)) {
						if (tempQuarantine.getVal(Tc.QUARANTINE_TYPE).equals("1")) {
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

	public DbDataObject checkIfAnimalIdExistsInInventoryItem(DbDataObject dbo, Reader rdr, SvReader svr)
			throws SvException {
		DbDataObject dboInventoryItem = rdr.getInventoryItemAccordingAnimalParams(dbo, false, svr);
		if (dboInventoryItem == null && !dbo.getObject_type().equals(SvReader.getTypeIdByName(Tc.PET))) {
			throw (new SvException("naits.error.noAvailableInventoryItemWithCurrentEarTagId", svCONST.systemUser, null,
					null));
		}
		return dboInventoryItem;
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
			if (vaccRecords != null && vaccRecords.size() > 0) {
				vaccRecords.getSortedItems(Tc.VACC_DATE);
				lastRecord = vaccRecords.get(vaccRecords.size() - 1);
				if (lastRecord != null && lastRecord.getVal(Tc.VACC_DATE) != null) {
					dt_vacc = dtf.parseDateTime(lastRecord.getVal(Tc.VACC_DATE).toString());
					Integer days = Days.daysBetween(dt_departure, dt_vacc).getDays();
					// log4j.debug(days);
					if (days >= daysNum) {
						result = true;
					}
				}
			}
		}
		return result;
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
	 * Method to return health status per specific holding, according Area
	 * Health Management Tool
	 * 
	 * @param holdingPic
	 *            - pic of the holding
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
			if (areaHealthStatuses.size() > 0) {
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
	 * Method that checks if AREA_HEALTH has duplicate disease
	 * 
	 * @param areaHealthObj
	 *            AREA_HEALTH object
	 * @param svr
	 *            SvReader instance
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
		if (areaHealthStatuses != null && areaHealthStatuses.size() > 0) {
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
		String movementValidationAccordingHoldingHealthStatus = "naits.system.movement_allowed";
		// first check source holding
		if (dboAnimalMovement.getVal(Tc.SOURCE_HOLDING_ID) != null) {
			String sourceHoldingHealthStatus = checkHoldingHealthStatusByArea(
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
			String destintionHoldingHealthStatus = checkHoldingHealthStatusByArea(
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

	/**
	 * Method that checks if EXPORT CERTIFICATE can be saved depend on
	 * quarantine type.
	 * 
	 * @param dbo
	 *            EXPORT_CERTIFICATE object
	 * @param rdr
	 *            Reader instance
	 * @param svr
	 *            SvReader instance
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

	public Boolean checkIfAnimalOrFlockDoesNotBelongToDraftMovementDoc(DbDataObject animalOrFlockObj, Reader rdr,
			SvReader svr) throws SvException {
		Boolean result = true;
		DbDataArray animalOrFlockMovements = null;
		if (animalOrFlockObj.getObject_type().equals(SvReader.getTypeIdByName(Tc.ANIMAL))) {
			animalOrFlockMovements = svr.getObjectsByParentId(animalOrFlockObj.getObject_id(),
					SvReader.getTypeIdByName(Tc.ANIMAL_MOVEMENT), null, 0, 0);
		} else {
			animalOrFlockMovements = svr.getObjectsByParentId(animalOrFlockObj.getObject_id(),
					SvReader.getTypeIdByName(Tc.FLOCK_MOVEMENT), null, 0, 0);
		}
		if (animalOrFlockMovements != null && animalOrFlockMovements.size() > 0) {
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
		if (animalOrFlockMovementsObj != null && animalOrFlockMovementsObj.size() > 0) {
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
	 * Method that checks (returns true) if specific field is unique. Otherwise
	 * returns false. Actually we use this method so we can set specific field
	 * to be unique.
	 * 
	 * @param fieldName
	 *            Field we want to be unique
	 * @param dbo
	 *            Object that contains that field
	 * @param svr
	 *            SvReader instance
	 * @return
	 * @throws SvException
	 */
	public Boolean checkIfFieldIsUnique(String fieldName, DbDataObject dbo, SvReader svr) throws SvException {
		Boolean result = true;
		if (dbo.getVal(fieldName) != null) {
			DbSearchCriterion cr1 = new DbSearchCriterion(fieldName, DbCompareOperand.EQUAL,
					dbo.getVal(fieldName).toString());
			DbSearchExpression dbse = new DbSearchExpression();
			dbse.addDbSearchItem(cr1);
			DbDataArray ar = svr.getObjects(dbse, dbo.getObject_type(), null, 0, 0);
			if ((dbo.getObject_id().equals(0L) && ar != null && ar.size() > 0)
					|| (!dbo.getObject_id().equals(0L) && ar != null && ar.size() > 1)) {
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
	 * Method that checks if ANIMAL or FLOCK has status POSTMORTEM so we can
	 * edit post-mortem form.
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
	 * @param dboQuarantine
	 * @param svr
	 * @throws SvException
	 */
	public void quarantineOnSaveValidationSet(DbDataObject dboQuarantine, Reader rdr, SvReader svr) throws SvException {
		String pattern = Tc.DATE_PATTERN;
		if (dboQuarantine.getObject_id().equals(0L) && dboQuarantine.getVal(Tc.DATE_FROM) != null) {
			String quarantineStartDate = dboQuarantine.getVal(Tc.DATE_FROM).toString().substring(0, 10);
			String quarantineEndDate = dboQuarantine.getVal(Tc.DATE_TO).toString().substring(0, 10);
			DateTime convertedQuarantineStartDate = DateTime.parse(quarantineStartDate,
					DateTimeFormat.forPattern(pattern));
			DateTime convertedQuarantineEndDate = DateTime.parse(quarantineEndDate, DateTimeFormat.forPattern(pattern));
			DateTime now = new DateTime();
			if (convertedQuarantineStartDate.isBefore(now.minusDays(1))) {
				throw (new SvException("naits.error.quarantineCanNotBeRegisteredInThePast", svCONST.systemUser, null,
						null));
			}
			if (convertedQuarantineEndDate.isBefore(convertedQuarantineStartDate)) {
				throw (new SvException("naits.error.quarantineCanNotHaveEndDateBeforeStartDate", svCONST.systemUser,
						null, null));
			}
		} else if (!dboQuarantine.getObject_id().equals(0L)) {
			// prevent update of quaranine star date
			DbDataObject quaranineFromDb = svr.getObjectById(dboQuarantine.getObject_id(),
					SvReader.getTypeIdByName(Tc.QUARANTINE), null);
			if (!dboQuarantine.getVal(Tc.DATE_FROM).equals(quaranineFromDb.getVal(Tc.DATE_FROM))) {
				throw (new SvException("naits.error.quarantineStartDateNotUpdatable", svCONST.systemUser, null, null));
			}
			if (!dboQuarantine.getVal(Tc.QUARANTINE_TYPE).equals(quaranineFromDb.getVal(Tc.QUARANTINE_TYPE))) {
				throw (new SvException("naits.error.quarantineTypeNotUpdatable", svCONST.systemUser, null, null));
			}
		}
		if (dboQuarantine.getVal(Tc.QUARANTINE_ID) == null) {
			rdr.generateQuarantineId(dboQuarantine, svr);
		}
	}

	/**
	 * Method that checks if ANIMAL or FLOCK has valid pre-mortem form. We use
	 * this validation so we can legal create post-mortem form
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
			if (preMortemsForAnimalOrFlock != null && preMortemsForAnimalOrFlock.size() > 0) {
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
	 * This list of diseases are used to check if Pre_Slaught_Form contains any
	 * of the prohibited list of diseases.
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
	 * Check method used only for Pre_Slaught_Form so we can prohibit new forms
	 * if unacceptable disease are spotted.
	 * 
	 * @param dboAnimalOrFlock
	 * @param svr
	 * @return
	 * @throws SvException
	 */
	public Boolean checkIfAnimalHasBlockingDiseaseInPremortemForm(DbDataObject dboAnimalOrFlock, SvReader svr)
			throws SvException {
		Boolean result = false;
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
		Boolean result = false;
		if (dbo.getVal(fieldName) != null && prohibitedDiseasesAndSuspissionDiseasesInPremortemForm(fieldName)
				.contains(dbo.getVal(fieldName).toString()))
			result = true;
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
		if (arrRangeIntersect.size() > 0) {
			if (objectType.equals(Tc.TRANSFER)) {
				throwMessage += "_" + objectType.toLowerCase();
			}
			throw (new SvException(throwMessage, svr.getInstanceUser()));
		} else {
			return true;
		}
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
				throw (new SvException("naits.error.beforeSaveCheck_departureDateCanNotBeAfterAnimalBirthDate",
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
	 * Method that checks if animal has appropriate class/race combination.
	 * Later we use this method in OnSaveValidation
	 * 
	 * @param dboAnimal
	 *            ANIMAL object
	 * @param svr
	 *            SvReader instance
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
	 * @param animalObj
	 *            ANIMAL object
	 * @param svr
	 *            SvReader instance
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
			if (getSibilings != null && getSibilings.size() > 0) {
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
	 * @param dboAnimal
	 *            ANIMAL object
	 * @param rdr
	 *            Reader instance
	 * @param svr
	 *            SvReader instance
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
									+ twinChild.getVal(Tc.ANIMAL_CLASS).toString(),
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
	 * Method that checks if user is administrator i.e if it's linked to group
	 * of type Administrators with Default group link type
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
	 * Void method that has set of ANIMAL MOVEMENT validations. Than, we use
	 * them in OnSaveValidation class
	 * 
	 * @param dboAnimalMovement
	 *            ANIMAL_MOVEMENT object
	 * @param svr
	 *            SvReader instance
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
			 * "naits.error.destinationHoldingIsSuspiciousOrNegative",
			 * svCONST.systemUser, null, null));
			 * 
			 * String movementValidationAccordingHoldingHealthStatus = validator
			 * .checkIfMovementValidAccordingHoldingsHealthStatus(
			 * dboAnimalMovement, svr);
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
	 * @param dboRangeOrTransfer
	 * @param dboOrgUnit
	 * @param svr
	 * @return
	 * @throws SvException
	 */
	public Boolean checkIfOrgUnitContainsEarTagByRange(DbDataObject dboRangeOrTransfer, DbDataObject dboOrgUnit,
			SvReader svr) throws SvException {
		Boolean result = true;
		Long startRange = null;
		Long endRange = null;
		String tag_type = "";
		Reader rdr = new Reader();
		if (dboRangeOrTransfer != null && dboOrgUnit != null) {
			startRange = Long.valueOf(dboRangeOrTransfer.getVal(Tc.START_TAG_ID).toString());
			endRange = Long.valueOf(dboRangeOrTransfer.getVal(Tc.END_TAG_ID).toString());
			tag_type = dboRangeOrTransfer.getVal(Tc.TAG_TYPE).toString();
			for (Long i = startRange; i <= endRange; i++) {
				DbDataObject inventoryItemFound = rdr.getInvetoryItemPerOrgUnit(dboOrgUnit, tag_type, i.toString(),
						svr);
				if (inventoryItemFound == null) {
					result = false;
					break;
				}
			}
		}
		return result;
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
	 * Method that checks if quarantine from type EXPORT CERTIFICATE has
	 * appropriate dates
	 * 
	 * @param obj
	 *            QUARANTINE object
	 * @param svr
	 *            SvReader instance
	 * @return
	 * @throws SvException
	 */
	public Boolean checkIfDateOfInsertIsBeforeEndDate(DbDataObject quarantine, SvReader svr) throws SvException {
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
	 * @param lastBronChild
	 *            DbDataObject of lastBronChild
	 * @param currentChild
	 *            DbDataObject of currentChild
	 * @param num
	 *            Days difference between lastBronChildBirthDate and animal1
	 *            currentChildBirthDate
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
		String diseases = "";
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
	 * @param preOrPostSlauightObj
	 *            pre/post slaughter form
	 * @param svr
	 *            SvReader instance
	 * @return
	 * @throws SvException
	 */
	public Boolean checkIfAnimalHasAppropriateDiseaseInPreOrPostSlaughtForm(DbDataObject preOrPostSlauightObj,
			DbDataObject animalOrFlockObj, SvReader svr) throws SvException {
		Boolean result = false;
		Reader rdr = new Reader();
		String fieldName = "";
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
				if (diseasesFound != null && diseasesFound.size() > 0) {
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
		DbDataObject twinAnimal = null;
		DbDataArray animalSibilings = rdr.getAnimalSiblingsAccordingMotherTagId(dboAnimal, svr);
		if (animalSibilings != null && animalSibilings.size() > 0) {
			if (checkIfAnimalIsTwin(dboAnimal, svr)) {
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
		if (onSpotChecks != null && onSpotChecks.size() > 0) {
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
		if (preMortemForms != null && preMortemForms.size() > 0) {
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
		if (preMortemForms != null && preMortemForms.size() > 0) {
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
	 * Method that checks if animal is part of some valid VACC_EVENT and checks
	 * if there is any disease recorded
	 * 
	 * @param dboAnimal
	 *            ANIMAL object
	 * @param svr
	 *            SvReader instance
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

	public Boolean checkIfLabSampleHasValidDiseaseSampleTestCombination(DbDataObject dboLabSample, Reader rdr,
			SvReader svr) throws SvException {
		Boolean result = true;
		DbDataArray testTypes = null;
		List<String> diseases = null;
		if (dboLabSample != null && dboLabSample.getVal(Tc.DISEASE_TEST) != null
				&& dboLabSample.getVal(Tc.SAMPLE_TYPE) != null && dboLabSample.getVal(Tc.SAMPLE_TEST_TYPE) != null) {
			diseases = rdr.getMultiSelectFieldValueAsList(dboLabSample, Tc.DISEASE_TEST);
			if (diseases != null && diseases.size() > 0) {
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
			if (animalOrFlockMovementsArr != null && animalOrFlockMovementsArr.size() > 0) {
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
		String animalTypeScope = "";
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
		if (arrAssociatedPersons != null && arrAssociatedPersons.size() > 0) {
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
		DbDataObject dboRangeOrTransfer = svr.getObjectById(dbo.getObject_id(), dbo.getObject_type(), new DateTime());
		if (dbo.getVal(Tc.TAG_TYPE) == null) {
			throw (new SvException("naits.error.transferMustHaveTagType", svCONST.systemUser, null, null));
		}
		if (dbo.getVal(Tc.TAG_TYPE).equals("2") && dbo.getVal(Tc.QUANTITY) == null) {
			throw (new SvException("naits.error.rangePerFlocksMustContainQuantityInfo", svCONST.systemUser, null,
					null));
		} else if (!dbo.getVal(Tc.TAG_TYPE).equals("2") && dbo.getVal(Tc.QUANTITY) != null) {
			throw (new SvException("naits.error.quantityNeedsToBeFilledOnlyWhenTagTypeIsFlock", svCONST.systemUser,
					null, null));
		}
		if (!dbo.getVal(Tc.TAG_TYPE).equals("2")
				&& (dbo.getVal(Tc.START_TAG_ID) == null || dbo.getVal(Tc.END_TAG_ID) == null)) {
			throw (new SvException("naits.error.rangePerIndividualAnimalsMustContainStartEndTagInfo",
					svCONST.systemUser, null, null));
		}
		if (dbo.getVal(Tc.TAG_TYPE).equals("2")
				&& (dbo.getVal(Tc.START_TAG_ID) != null || dbo.getVal(Tc.END_TAG_ID) != null)) {
			dbo.setVal(Tc.START_TAG_ID, 0L);
			dbo.setVal(Tc.END_TAG_ID, 0L);
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
		if (dbo.getObject_id().equals(0L) && !dbo.getVal(Tc.TAG_TYPE).equals("2")) {
			Long startTagId = Long.valueOf(dbo.getVal(Tc.START_TAG_ID).toString());
			Long endTagId = Long.valueOf(dbo.getVal(Tc.END_TAG_ID).toString());
			dbo.setVal(Tc.UNIT_NUMBER, String.valueOf(Math.abs(endTagId - startTagId)));

			if (endTagId < startTagId) {
				throw (new SvException("naits.error.endingTagIdMusteLargerThanStartingTagId", svCONST.systemUser, null,
						null));
			}
			if (dbo.getObject_type().equals(SvReader.getTypeIdByName(Tc.TRANSFER))) {
				if (dbo.getVal(Tc.DIRECT_TRANSFER) == null
						&& !checkIfEarTagRangeInTransferExistsInInventoryItem(dbo, svr)) {
					throw (new SvException("naits.error.inventoryItemDoesntHaveAllEarTagsEnteredInTheRange",
							svCONST.systemUser, null, null));
				}
			}
			result = checkIfRangeCanBeSaved(dbo, dbo.getVal(Tc.TAG_TYPE).toString(), startTagId, endTagId, svr);
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
				? dboTransfer.getVal(Tc.TRANSFER_TYPE).toString() : Tc.DEFAULT;
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
		if (dboInventoryItem != null && dboInventoryItem.getVal(Tc.ANIMAL_OBJ_ID) != null) {
			result = true;
		}
		return result;
	}

	public String checkIfAnimalBelongsToSlaughterhouse(Long object_Id, String session_Id) {
		SvReader svr = null;
		String result = "";
		try {
			svr = new SvReader(session_Id);
			DbDataObject dboAnimalOrFlock = svr.getObjectById(object_Id, SvReader.getTypeIdByName(Tc.ANIMAL), null);
			if (dboAnimalOrFlock != null) {
				result = checkIfHoldingIsSlaughterhouse(dboAnimalOrFlock.getParent_id(), svr).toString();
			} else {
				dboAnimalOrFlock = svr.getObjectById(object_Id, SvReader.getTypeIdByName(Tc.FLOCK), null);
				if (dboAnimalOrFlock != null) {
					result = checkIfHoldingIsSlaughterhouse(dboAnimalOrFlock.getParent_id(), svr).toString();
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
		if (arrAnimals != null && arrAnimals.size() > 0) {
			for (DbDataObject dboTempAnimal : arrAnimals.getItems()) {
				String movementProhibitions = rdr.getHealthStatusPerAnimalAccordingLabSample(dboTempAnimal, svr);
				if (!movementProhibitions.equals("")) {
					movementAllowed = false;
					break;
				}
			}
			if (movementAllowed) {
				if (checkIfHoldingBelongsInActiveDiseaseQuarantine(dboHolding, svr)) {
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
	 * @param holdingObjectId
	 *            Object id of the Holding
	 * @param dboObjectToHandle
	 *            Animal/Flock object
	 * @param objectTypeLabel
	 *            Object type label (animal/flock)
	 * @param svr
	 *            SvReader instance
	 * @throws SvException
	 */
	public void slaughterhouseHoldingValidationSet(Long holdingObjectId, DbDataObject dboObjectToHandle,
			String objectTypeLabel, SvReader svr) throws SvException {
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
	 * Method that checks if HOLDING belongs to affected area
	 * (HIGH_RISK/LOW_RISK)
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
		if (dboOldAnimalVersion == null && dboCurrentHolding.getStatus().equals(Tc.SUSPENDED)) {
			wr.updateHoldingStatus(dboCurrentHolding, Tc.SUSPENDED, Tc.VALID, svr);
		}
	}

	public void updateHoldingStatusAccordingHoldingTypeAfterSaveAnimal(DbDataObject dboAnimalOrFlock, SvReader svr)
			throws SvException {
		Writer wr = new Writer();
		Reader rdr = new Reader();
		DbDataObject dboOldAnimalVersion = null;
		DbDataObject dboCurrHolding = null;
		DbDataArray arrAnimalOrFlockHistory = null;
		if (dboAnimalOrFlock.getObject_type().equals(SvReader.getTypeIdByName(Tc.ANIMAL))) {
			arrAnimalOrFlockHistory = rdr.searchByAnimalIdWithHistory(dboAnimalOrFlock.getVal(Tc.ANIMAL_ID).toString(),
					dboAnimalOrFlock.getVal(Tc.ANIMAL_CLASS).toString(), svr);
		} else {
			arrAnimalOrFlockHistory = rdr.searchByFlockIdWithHistory(dboAnimalOrFlock.getVal(Tc.FLOCK_ID).toString(),
					svr);
		}
		// update source holding if need
		if (arrAnimalOrFlockHistory.size() > 1) {
			dboOldAnimalVersion = arrAnimalOrFlockHistory.get(arrAnimalOrFlockHistory.size() - 2);
			if (!dboOldAnimalVersion.getParent_id().equals(dboAnimalOrFlock.getParent_id())) {
				DbDataObject dboOldHolding = svr.getObjectById(dboOldAnimalVersion.getParent_id(),
						SvReader.getTypeIdByName(Tc.HOLDING), null);
				if (checkIfHoldingIsCommercialOrSubsistenceFarmType(dboOldHolding)) {
					wr.updateHoldingStatus(dboOldHolding, Tc.VALID, Tc.SUSPENDED, true, svr);
				}
			}
		}
		// update source if needed
		dboCurrHolding = svr.getObjectById(dboAnimalOrFlock.getParent_id(), SvReader.getTypeIdByName(Tc.HOLDING), null);
		if (dboCurrHolding != null) {
			wr.updateHoldingStatus(dboCurrHolding, Tc.SUSPENDED, Tc.VALID, svr);
		}
	}

	public boolean checkIfHoldingIsCommercialOrSubsistenceFarmType(DbDataObject dboHolding) {
		boolean result = false;
		if (dboHolding.getVal(Tc.TYPE) != null && (dboHolding.getVal(Tc.TYPE).toString().equals("5")
				|| dboHolding.getVal(Tc.TYPE).toString().equals("6"))) {
			result = true;
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

	public boolean isFlockSlaughterable(DbDataObject dboFlock) {
		boolean result = false;
		if (dboFlock != null && dboFlock.getVal(Tc.ANIMAL_TYPE) != null
				&& dboFlock.getVal(Tc.ANIMAL_TYPE).toString().equals("4")) {
			result = true;
		}
		return result;
	}

	public void petValidationSet(DbDataObject dboPet, Writer wr, Reader rdr, SvWriter svw, SvReader svr)
			throws SvException {
		DbDataObject dboPetLastVersion = svr.getObjectById(dboPet.getObject_id(), dboPet.getObject_type(),
				new DateTime());
		if (dboPet.getVal(Tc.PET_TYPE) == null) {
			throw (new SvException("naits.error.beforeSaveCheck_pet_PleaseEnterPetType", svCONST.systemUser, null,
					null));
		}

		if (!checkIfFieldIsUnique(Tc.PET_ID, dboPet, svr)) {
			throw (new SvException("naits.error.beforeSaveCheck_pet_petIdMustBeUnique", svCONST.systemUser, null,
					null));
		}

		if (dboPet.getObject_id().equals(0L) && dboPet.getVal(Tc.PET_ID) == null) {
			String archiveNumber = wr.generateArchiveNumber(dboPet, svr);
			dboPet.setVal(Tc.ARCHIVE_NUMBER, archiveNumber);
		}

		if (dboPetLastVersion != null) {
			if (dboPetLastVersion.getVal(Tc.PET_ID) != null && dboPet.getVal(Tc.PET_ID) == null) {
				throw (new SvException("naits.error.beforeSaveCheck_pet_petIdCannotBeEdited", svCONST.systemUser, null,
						null));
			}
			if (dboPetLastVersion.getVal(Tc.PET_ID) != null
					&& !dboPetLastVersion.getVal(Tc.PET_ID).toString().equals(dboPet.getVal(Tc.PET_ID).toString())
					&& dboPet.getVal(Tc.CHECK_COLUMN) == null) {
				throw (new SvException("naits.error.beforeSaveCheck_pet_petIdCannotBeEdited", svCONST.systemUser, null,
						null));
			}
		}

		if (dboPet.getVal(Tc.PET_ID) != null) {
			String petId = dboPet.getVal(Tc.PET_ID).toString();
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
			DbDataObject dboPetShelterHolding = svr.getObjectById(dboPet.getParent_id(),
					SvReader.getTypeIdByName(Tc.HOLDING), null);
			DbDataArray arrExistingLocation = svr.getObjectsByParentId(dboPet.getObject_id(),
					SvReader.getTypeIdByName(Tc.STRAY_PET_LOCATION), new DateTime(), 0, 0);
			if (dboPet.getVal(Tc.IS_STRAY_PET) != null && dboPet.getVal(Tc.IS_STRAY_PET).toString().equals("1")
					&& arrExistingLocation.getItems().isEmpty()) {
				// Collection location
				DbDataObject dboPetCollectionLoc = wr.createPetLocationAccordingAnimalShelterHolding(dboPet,
						dboPetShelterHolding, "1", svr);
				svg.saveGeometry(dboPetCollectionLoc);
			}

			if (dboPet.getVal(Tc.WEIGHT) != null) {
				Long lastWeight = null;
				Long currentWeight = Long.valueOf(dboPet.getVal(Tc.WEIGHT).toString());
				if (dboPetLastVersion == null) {
					DbDataObject dboNewMeasurement = wr.createMeasurementObject(
							Long.valueOf(dboPet.getVal(Tc.WEIGHT).toString()), Tc.WEIGHT, Tc.MEASURE_KG, new DateTime(),
							dboPet.getObject_id());
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
							DbDataObject dboNewMeasurement = wr.createMeasurementObject(
									Long.valueOf(dboPet.getVal(Tc.WEIGHT).toString()), Tc.WEIGHT, Tc.MEASURE_KG,
									new DateTime(), dboPet.getObject_id());
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
		DbDataObject dboPet = rdr.getPetByPetId(dboStrayPet.getVal(Tc.PET_ID).toString(), svr);
		// ID check
		if (dboStrayPet.getVal(Tc.PET_ID) != null) {
			if (!NumberUtils.isDigits(dboStrayPet.getVal(Tc.PET_ID).toString())) {
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
		SvGeometry svg = null;
		try {
			svg = new SvGeometry(svr);
			svg.setAllowNullGeometry(true);

			DbDataArray arrPetLocations = svr.getObjectsByParentId(dboLocation.getParent_id(),
					dboLocation.getObject_type(), new DateTime(), 0, 0);
			if (dboLocation.getObject_id().equals(0L) && !arrPetLocations.getItems().isEmpty()) {
				for (DbDataObject dboStrayPetLocation : arrPetLocations.getItems()) {
					if (dboStrayPetLocation.getStatus().equals(Tc.VALID)
							&& dboStrayPetLocation.getVal(Tc.LOCATION_REASON).toString()
									.equals(dboLocation.getVal(Tc.LOCATION_REASON).toString())) {
						dboStrayPetLocation.setStatus(Tc.INVALID);
						svg.saveGeometry(dboStrayPetLocation);
					}
				}
			}
		} catch (SvException e) {
			log4j.error(e);
		} finally {
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
		return result;
	}

	public Boolean isInventoryItemCheckBlocked(DbDataObject dboAnimal) {
		Boolean result = true;
		Boolean dtBirthCriterium = false;
		String dbAnimalCountry = getDboAnimalCountry(dboAnimal);
		DateTime dtBirthDate = null;
		DateTime dtRegistrationDate = null;
		if (dboAnimal.getVal(Tc.BIRTH_DATE) != null && dboAnimal.getVal(Tc.REGISTRATION_DATE) != null) {
			dtBirthDate = new DateTime(dboAnimal.getVal(Tc.BIRTH_DATE).toString());
			dtRegistrationDate = new DateTime(dboAnimal.getVal(Tc.REGISTRATION_DATE).toString());
			if (dtRegistrationDate.isAfter(dtBirthDate)) {
				dtBirthCriterium = true;
			}
		}
		String dbAnimalOriginCountry = getDboAnimalCountry(dboAnimal, true);
		if (!dbAnimalCountry.equals(Tc.GE) || (dbAnimalCountry.equals(Tc.GE) && !dbAnimalOriginCountry.equals(Tc.GE)
				&& !dbAnimalOriginCountry.equals("") && dtBirthCriterium)) {
			result = false;
		}
		return result;
	}

	public String getDboAnimalCountry(DbDataObject dbo) {
		return getDboAnimalCountry(dbo, false);
	}

	public String getDboAnimalCountry(DbDataObject dbo, Boolean isCountryOfOrigin) {
		String result = "";
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

	/**
	 * Method that prevents duplicate campaign participation per Pet
	 * 
	 * @param objectId
	 *            Pet object_id
	 * @param vaccEventObjId
	 *            Vaccination Book / Campaign object_id
	 * @param svr
	 *            SvReader instance
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
		DbDataObject dboPet = rdr.getPetByPetIdAndPetType(dbo.getVal(Tc.PET_ID).toString(), null, false, svr);
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

	public void vaccinationEventValidationSet(DbDataObject dbo, SvReader svr) throws SvException {
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
					|| dbo.getVal(Tc.DISEASE).toString().equals("35"))) {
				throw (new SvException("naits.error.inapplicableDiseaseForPetScope", svCONST.systemUser, null, null));
			}
		}

		if (dbo.getVal(Tc.CAMPAIGN_SCOPE) != null && dbo.getVal(Tc.CAMPAIGN_SCOPE).toString().equals(Tc.ANIMAL)
				&& dbo.getVal(Tc.ANIMAL_TYPE) != null) {
			List<String> animalTypeScope = rdr.getMultiSelectFieldValueAsList(dbo, Tc.ANIMAL_TYPE);
			for (String animalType : animalTypeScope) {
				if (animalType.equals("401") || animalType.equals("402") || animalType.equals("403")) {
					throw (new SvException("naits.error.animalScopeCanBeAppliedOnAnimalTypeOnly", svCONST.systemUser,
							null, null));
				}
			}
		}
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
	 * Set of validations for Pet movement object
	 * 
	 * @param dbo
	 * @param rdr
	 * @param svr
	 * @throws SvException
	 */
	public void petMovementValidationSet(DbDataObject dbo, SvReader svr) throws SvException {
		if (!dbo.getStatus().equals(Tc.RELEASED) && dbo.getVal(Tc.HOLDING_OBJ_ID) == null) {
			throw (new SvException("naits.error.cannotCreateMovementWithoutDestination", svCONST.systemUser, null,
					null));
		}
		DbDataObject dboPet = svr.getObjectById(dbo.getParent_id(), SvReader.getTypeIdByName(Tc.PET), new DateTime());
		DbDataObject dboSourceHolding = svr.getObjectById(dboPet.getParent_id(), SvReader.getTypeIdByName(Tc.HOLDING),
				null);
		DbDataObject dboDestinationHolding = svr.getObjectById(Long.valueOf(dbo.getVal(Tc.HOLDING_OBJ_ID).toString()),
				SvReader.getTypeIdByName(Tc.HOLDING), null);
		if (!dboPet.getStatus().equals(Tc.VALID)) {
			throw (new SvException("naits.error.petMustBeValid", svCONST.systemUser, null, null));
		}
		if (dboDestinationHolding.getObject_id().equals(dboSourceHolding.getObject_id())) {
			throw (new SvException("naits.error.sourceAndDestinationHoldingAreSame", svCONST.systemUser, null, null));
		}
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
		DateTime dt = null;
		try {
			dt = new DateTime(date);
		} catch (Exception e) {
			result = false;
		}
		return result;
	}
}