package naits_triglav_plugin;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.prtech.svarog.SvException;
import com.prtech.svarog.SvReader;
import com.prtech.svarog_common.DbDataArray;
import com.prtech.svarog_common.DbDataObject;
import com.prtech.svarog_common.DbSearchCriterion;
import com.prtech.svarog_common.DbSearchCriterion.DbCompareOperand;
import com.prtech.svarog_common.DbSearchExpression;

public class PublicServiceDataTranslator {

	static final Logger log4j = LogManager.getLogger(PublicServiceDataTranslator.class.getName());

	public DbDataObject loadAnimalData(DbDataObject dboAnimal, String localeId, Reader rdr, SvReader svr)
			throws SvException {
		DbDataObject result = dboAnimal;
		if (dboAnimal.getVal(Tc.COUNTRY) != null) {
			String animalCountryTranslated = rdr.decodeCodeValue(SvReader.getTypeIdByName(Tc.ANIMAL), Tc.COUNTRY,
					dboAnimal.getVal(Tc.COUNTRY).toString(), localeId, svr);
			result.setVal(Tc.COUNTRY, animalCountryTranslated);
		}
		if (dboAnimal.getVal(Tc.ANIMAL_CLASS) != null) {
			String animalClass = dboAnimal.getVal(Tc.ANIMAL_CLASS).toString();
			String animalClassTranslated = rdr.decodeCodeValue(SvReader.getTypeIdByName(Tc.ANIMAL), Tc.ANIMAL_CLASS,
					animalClass, localeId, svr);
			result.setVal(Tc.ANIMAL_CLASS, animalClassTranslated);
			result.setVal(Tc.NOT_TRANSLATED_ANIMAL_CLASS, animalClass);
		}
		if (dboAnimal.getVal(Tc.ANIMAL_RACE) != null) {
			String animalRaceTranslated = rdr.decodeCodeValue(SvReader.getTypeIdByName(Tc.ANIMAL), Tc.ANIMAL_RACE,
					dboAnimal.getVal(Tc.ANIMAL_RACE).toString(), localeId, svr);
			result.setVal(Tc.ANIMAL_RACE, animalRaceTranslated);
		}
		if (dboAnimal.getVal(Tc.GENDER) != null) {
			String animalGenderTranslated = rdr.decodeCodeValue(SvReader.getTypeIdByName(Tc.ANIMAL), Tc.GENDER,
					dboAnimal.getVal(Tc.GENDER).toString(), localeId, svr);
			result.setVal(Tc.GENDER, animalGenderTranslated);
		}
		if (dboAnimal.getVal(Tc.COLOR) != null) {
			String animalColorTranslated = rdr.decodeCodeValue(SvReader.getTypeIdByName(Tc.ANIMAL), Tc.COLOR,
					dboAnimal.getVal(Tc.COLOR).toString(), localeId, svr);
			result.setVal(Tc.COLOR, animalColorTranslated);
		}
		return dboAnimal;
	}

	public void loadAnimalDataInJson(JsonObject jsoObj, DbDataObject dboAnimal, String localeId, Reader rdr,
			SvReader svr) throws SvException {
		jsoObj.addProperty(Tc.STATUS, dboAnimal.getStatus().toString());
		if (dboAnimal.getVal(Tc.ANIMAL_ID) != null) {
			jsoObj.addProperty(Tc.ANIMAL_ID, dboAnimal.getVal(Tc.ANIMAL_ID).toString());
		}
		if (dboAnimal.getVal(Tc.BIRTH_DATE) != null) {
			jsoObj.addProperty(Tc.BIRTH_DATE, dboAnimal.getVal(Tc.BIRTH_DATE).toString());
		}
		if (dboAnimal.getVal(Tc.SLAUGHTER_DATE) != null) {
			jsoObj.addProperty(Tc.SLAUGHTER_DATE, dboAnimal.getVal(Tc.SLAUGHTER_DATE).toString());
		}
		if (dboAnimal.getVal(Tc.COUNTRY) != null) {
			String animalCountryTranslated = rdr.decodeCodeValue(SvReader.getTypeIdByName(Tc.ANIMAL), Tc.COUNTRY,
					dboAnimal.getVal(Tc.COUNTRY).toString(), localeId, svr);
			jsoObj.addProperty(Tc.COUNTRY, animalCountryTranslated);
		}
		if (dboAnimal.getVal(Tc.ANIMAL_CLASS) != null) {
			String animalClass = dboAnimal.getVal(Tc.ANIMAL_CLASS).toString();
			String animalClassTranslated = rdr.decodeCodeValue(SvReader.getTypeIdByName(Tc.ANIMAL), Tc.ANIMAL_CLASS,
					animalClass, localeId, svr);
			jsoObj.addProperty(Tc.ANIMAL_CLASS, animalClassTranslated);
			jsoObj.addProperty(Tc.NOT_TRANSLATED_ANIMAL_CLASS, animalClass);
		}
		if (dboAnimal.getVal(Tc.ANIMAL_RACE) != null) {
			String animalRaceTranslated = rdr.decodeCodeValue(SvReader.getTypeIdByName(Tc.ANIMAL), Tc.ANIMAL_RACE,
					dboAnimal.getVal(Tc.ANIMAL_RACE).toString(), localeId, svr);
			jsoObj.addProperty(Tc.ANIMAL_RACE, animalRaceTranslated);
		}
		if (dboAnimal.getVal(Tc.GENDER) != null) {
			String animalGenderTranslated = rdr.decodeCodeValue(SvReader.getTypeIdByName(Tc.ANIMAL), Tc.GENDER,
					dboAnimal.getVal(Tc.GENDER).toString(), localeId, svr);
			jsoObj.addProperty(Tc.GENDER, animalGenderTranslated);
		}
		if (dboAnimal.getVal(Tc.COLOR) != null) {
			String animalColorTranslated = rdr.decodeCodeValue(SvReader.getTypeIdByName(Tc.ANIMAL), Tc.COLOR,
					dboAnimal.getVal(Tc.COLOR).toString(), localeId, svr);
			jsoObj.addProperty(Tc.COLOR, animalColorTranslated);
		}
	}

	public DbDataObject loadHoldingDataForAnimal(DbDataObject dboAnimal, String localeId, Reader rdr, SvReader svr)
			throws SvException {
		DbDataObject result = dboAnimal;
		DbDataObject dboHolding = svr.getObjectById(dboAnimal.getParent_id(), SvReader.getTypeIdByName(Tc.HOLDING),
				null);
		if (dboHolding != null) {
			if (dboHolding.getVal(Tc.PIC) != null) {
				result.setVal(Tc.HOLDING_PIC, dboHolding.getVal(Tc.PIC).toString());
			}
			if (dboHolding.getVal(Tc.VILLAGE_CODE) != null && dboAnimal.getStatus().toString().equals(Tc.POSTMORTEM)) {
				String translatedVillageCode = rdr.decodeCodeValue(SvReader.getTypeIdByName(Tc.VILLAGE),
						Tc.VILLAGE_CODE, dboHolding.getVal(Tc.VILLAGE_CODE).toString(), localeId, svr);
				String translatedMunicCode = rdr.decodeCodeValue(SvReader.getTypeIdByName(Tc.VILLAGE), Tc.MUNIC_CODE,
						dboHolding.getVal(Tc.MUNIC_CODE).toString(), localeId, svr);
				String translatedCommunCode = rdr.decodeCodeValue(SvReader.getTypeIdByName(Tc.VILLAGE), Tc.COMMUN_CODE,
						dboHolding.getVal(Tc.COMMUN_CODE).toString(), localeId, svr);
				result.setVal(Tc.SLAUGHTER_LOCATION_MUNIC, translatedMunicCode);
				result.setVal(Tc.SLAUGHTER_LOCATION_COMMUN, translatedCommunCode);
				result.setVal(Tc.SLAUGHTER_LOCATION_VILLAGE, translatedVillageCode);
			}
		}
		return result;
	}

	public void loadHoldingDataForAnimalinJson(JsonObject jsoObj, DbDataObject dboAnimal, String localeId, Reader rdr,
			SvReader svr) throws SvException {
		DbDataObject dboHolding = svr.getObjectById(dboAnimal.getParent_id(), SvReader.getTypeIdByName(Tc.HOLDING),
				null);
		if (dboHolding != null) {
			if (dboHolding.getVal(Tc.PIC) != null) {
				jsoObj.addProperty(Tc.HOLDING_PIC, dboHolding.getVal(Tc.PIC).toString());
			}
			loadHoldingAreaTranslations(jsoObj, dboHolding, localeId, rdr, svr);
		}
	}

	public DbDataObject loadPostMortemDataForAnimal(DbDataObject dboAnimal, String localeId, Reader rdr, SvReader svr)
			throws SvException, ParseException {
		DbDataObject result = dboAnimal;
		DbDataArray dbArrPostForm = svr.getObjectsByParentId(dboAnimal.getObject_id(),
				SvReader.getTypeIdByName(Tc.POST_SLAUGHT_FORM), null, 0, 0);
		if (dbArrPostForm != null && !dbArrPostForm.getItems().isEmpty()) {
			DbDataObject dboTempSlaught = dbArrPostForm.get(0);
			if (dboTempSlaught.getVal(Tc.SLAUGHTER_DATE) != null) {
				Date date = new SimpleDateFormat(Tc.DATE_PATTERN)
						.parse(dboTempSlaught.getVal(Tc.SLAUGHTER_DATE).toString());
				String formattedDate = new SimpleDateFormat(Tc.DATE_PATTERN).format(date);
				result.setVal(Tc.SLAUGHTER_DATE, formattedDate);
			}
		}
		return result;
	}

	public DbDataObject loadPostMortemDataForAnimalInJson(JsonObject jsoObj, DbDataObject dboAnimal, String localeId,
			Reader rdr, SvReader svr) throws SvException, ParseException {
		DbDataObject result = dboAnimal;
		DbDataArray dbArrPostForm = svr.getObjectsByParentId(dboAnimal.getObject_id(),
				SvReader.getTypeIdByName(Tc.POST_SLAUGHT_FORM), null, 0, 0);
		if (dbArrPostForm != null && !dbArrPostForm.getItems().isEmpty()) {
			DbDataObject dboTempSlaught = dbArrPostForm.get(0);
			if (dboTempSlaught.getVal(Tc.SLAUGHTER_DATE) != null) {
				Date date = new SimpleDateFormat(Tc.DATE_PATTERN)
						.parse(dboTempSlaught.getVal(Tc.SLAUGHTER_DATE).toString());
				String formattedDate = new SimpleDateFormat(Tc.DATE_PATTERN).format(date);
				jsoObj.addProperty(Tc.SLAUGHTER_DATE, formattedDate);
			}
		}
		return result;
	}

	public JsonObject loadHoldingHistoryForAnimal(DbDataObject dboAnimal, String localeId, Reader rdr, SvReader svr)
			throws SvException, ParseException {
		JsonArray jsonArrMovements = new JsonArray();
		JsonObject result = new JsonObject();
		DbDataArray dbArrAnimalMovements = svr.getObjectsByParentId(dboAnimal.getObject_id(),
				SvReader.getTypeIdByName(Tc.ANIMAL_MOVEMENT), null, 0, 0);
		DbDataArray filteredDbArrAnimalMovements = new DbDataArray();
		// filter only finished movements
		if (dbArrAnimalMovements != null && !dbArrAnimalMovements.getItems().isEmpty()) {
			for (DbDataObject dbo : dbArrAnimalMovements.getItems()) {
				if (dbo.getStatus().equals(Tc.FINISHED))
					filteredDbArrAnimalMovements.addDataItem(dbo);
			}
		}
		// load data for holding(s) from movements
		if (!filteredDbArrAnimalMovements.getItems().isEmpty()) {
			filteredDbArrAnimalMovements.getSortedItems(Tc.DEPARTURE_DATE);
			int i = 0;
			for (DbDataObject dbo : filteredDbArrAnimalMovements.getItems()) {
				JsonObject json = new JsonObject();
				DbDataObject dboHolding = rdr.findAppropriateHoldingByPic(dbo.getVal(Tc.SOURCE_HOLDING_ID).toString(),
						svr);
				if (dboHolding != null) {
					json.addProperty(Tc.PIC, dboHolding.getVal(Tc.PIC).toString());
					loadHoldingAreaTranslations(json, dboHolding, localeId, rdr, svr);
					jsonArrMovements.add(json);
					result.add(Tc.HOLDING, jsonArrMovements);
				}
				// get destination golding for last element in the movement
				// array
				if (i++ == filteredDbArrAnimalMovements.size() - 1) {
					dboHolding = rdr.findAppropriateHoldingByPic(dbo.getVal(Tc.DESTINATION_HOLDING_ID).toString(), svr);
					json.addProperty(Tc.PIC, dboHolding.getVal(Tc.PIC).toString());
					loadHoldingAreaTranslations(json, dboHolding, localeId, rdr, svr);
					jsonArrMovements.add(json);
					result.add(Tc.HOLDING, jsonArrMovements);
				}

			}
		} else {
			// first add the initial holding data
			DbDataObject dboCurrentHolding = svr.getObjectById(dboAnimal.getParent_id(),
					SvReader.getTypeIdByName(Tc.HOLDING), null);
			if (dboCurrentHolding != null) {
				JsonObject currHoldingJsonData = new JsonObject();
				currHoldingJsonData.addProperty(Tc.PIC, dboCurrentHolding.getVal(Tc.PIC).toString());
				loadHoldingAreaTranslations(currHoldingJsonData, dboCurrentHolding, localeId, rdr, svr);
				result.add(Tc.HOLDING, currHoldingJsonData);
			}
		}
		return result;
	}

	public JsonObject loadHoldingHistoryForAnimal2(DbDataObject dboAnimal, String localeId, Reader rdr, SvReader svr)
			throws SvException, ParseException {
		JsonArray jsonArrMovements = new JsonArray();
		JsonObject json;
		DbDataObject dboHolding;
		JsonObject result = new JsonObject();
		ArrayList<Long> visitedPics = new ArrayList<Long>();
		// fetch all animal historical data
		DbSearchCriterion cr1 = new DbSearchCriterion(Tc.OBJECT_ID, DbCompareOperand.EQUAL, dboAnimal.getObject_id());
		DbDataArray animalHistoryObject = svr.getObjectsHistory(new DbSearchExpression().addDbSearchItem(cr1),
				SvReader.getTypeIdByName(Tc.ANIMAL), 0, 0);
		animalHistoryObject.getSortedItems(Tc.PKID);
		// distinct historical data by its parent - holding
		for (DbDataObject tempAnimalHistoryRecord : animalHistoryObject.getItems()) {
			Long dboHoldingObjId = tempAnimalHistoryRecord.getParent_id();
			if (!visitedPics.contains(dboHoldingObjId)) {
				visitedPics.add(dboHoldingObjId);
			}
		}
		// load already distincted holding list with proper data
		if (!visitedPics.isEmpty()) {
			for (Long holdingObjId : visitedPics) {
				json = new JsonObject();
				dboHolding = svr.getObjectById(holdingObjId, SvReader.getTypeIdByName(Tc.HOLDING), null);
				if (dboHolding != null && dboHolding.getVal(Tc.PIC) != null) {
					json.addProperty(Tc.PIC, dboHolding.getVal(Tc.PIC).toString());
					loadHoldingAreaTranslations(json, dboHolding, localeId, rdr, svr);
					jsonArrMovements.add(json);
					result.add(Tc.HOLDING, jsonArrMovements);
				}
			}
		}
		return result;
	}

	public JsonObject loadVaccineHistoryPerDiseaseForAnimal(DbDataObject dboAnimal, String localeId, Reader rdr,
			SvReader svr) throws SvException, ParseException {
		JsonObject result = new JsonObject();
		JsonObject jtempObj = new JsonObject();
		HashSet<String> distinctDiseaseSet = new HashSet<>();
		ArrayList<DbDataObject> dboVaccBooks = rdr.getLinkedVaccinationBooksPerAnimalOrFlock(dboAnimal, svr).getItems();
		ArrayList<DbDataObject> filteredVaccBook = new ArrayList<>();
		// filter only ones related with campaign
		if (dboVaccBooks != null && !dboVaccBooks.isEmpty()) {
			for (DbDataObject dboVaccBook : dboVaccBooks) {
				if ((dboVaccBook.getVal(Tc.ACTIVITY_TYPE) != null
						&& (dboVaccBook.getVal(Tc.ACTIVITY_TYPE).toString().equals("1")
								|| dboVaccBook.getVal(Tc.ACTIVITY_TYPE).toString().equals("4"))
						|| dboVaccBook.getVal(Tc.CAMPAIGN_NAME) != null)) {
					filteredVaccBook.add(dboVaccBook);
				}
			}
		}
		DbDataArray vaccinationEvents = new DbDataArray();
		(new Reader()).sortDescendingDbDataArray(filteredVaccBook, Tc.VACC_DATE, svr);
		if (filteredVaccBook != null && !filteredVaccBook.isEmpty()) {
			for (int i = filteredVaccBook.size() - 1; i >= 0; i--) {
				DbDataObject dboVaccBook = filteredVaccBook.get(i);
				DbDataArray dbArrVaccEvents = rdr.getAllVaccEventsForVaccBook(dboVaccBook, svr);
				if (dbArrVaccEvents != null && !dbArrVaccEvents.getItems().isEmpty()) {
					DbDataObject dboVaccEvent = dbArrVaccEvents.get(0);
					if (dboVaccEvent.getVal(Tc.DISEASE) != null) {
						if (!vaccinationEvents.getItems().contains(dboVaccEvent)) {
							String campaignName = "";
							if (dboVaccEvent.getVal(Tc.CAMPAIGN_NAME) != null) {
								campaignName = dboVaccEvent.getVal(Tc.CAMPAIGN_NAME).toString();
							}
							JsonObject json = new JsonObject();
							String disease = dboVaccEvent.getVal(Tc.DISEASE).toString();
							String translatedDisease = rdr.decodeCodeValue(
									SvReader.getTypeIdByName(Tc.VACCINATION_EVENT), Tc.DISEASE, disease, localeId, svr);
							json.addProperty(Tc.DISEASE_NAME, translatedDisease);
							json.addProperty(Tc.CAMPAIGN_NAME, campaignName);
							json.addProperty(Tc.DATE, dboVaccBook.getVal(Tc.VACC_DATE).toString());
							if (disease.equals("1") || disease.equals("2") || disease.equals("3")
									|| disease.equals("5")) {
								if (dboVaccEvent.getVal(Tc.IMMUNIZATION_PERIOD) != null)
									json.addProperty(Tc.IMMUNIZATION_PERIOD,
											dboVaccEvent.getVal(Tc.IMMUNIZATION_PERIOD).toString());
								if (distinctDiseaseSet.add(disease)) {
									jtempObj.add(disease, json);
								}
							}

						}
					}
				} else {
					if (dboVaccBook.getVal(Tc.VACC_CODE) != null) {
						JsonObject json = new JsonObject();
						String disease = rdr.getDiseaseCodeInVaccinationBookAccordingVaccinationCode(
								dboVaccBook.getVal(Tc.VACC_CODE).toString());
						String translatedDisease = rdr.decodeCodeValue(SvReader.getTypeIdByName(Tc.VACCINATION_EVENT),
								Tc.DISEASE, disease, localeId, svr);
						json.addProperty(Tc.DISEASE_NAME, translatedDisease);
						json.addProperty(Tc.DATE, dboVaccBook.getVal(Tc.VACC_DATE).toString());
						if (!disease.equals(Tc.EMPTY_STRING)) {
							if (dboVaccBook.getVal(Tc.IMMUNIZATION_PERIOD) != null)
								json.addProperty(Tc.IMMUNIZATION_PERIOD,
										dboVaccBook.getVal(Tc.IMMUNIZATION_PERIOD).toString());
							if (distinctDiseaseSet.add(disease)) {
								jtempObj.add(disease, json);
							}
						}
					}
				}

			}
		}
		result.add(Tc.VACCINE_PER_DISEASE_HISTORY, jtempObj);
		return result;
	}

	void loadHoldingAreaTranslations(JsonObject currentJson, DbDataObject dboHolding, String localeId, Reader rdr,
			SvReader svr) throws SvException {
		if (dboHolding.getVal(Tc.VILLAGE_CODE) != null) {
			currentJson.addProperty(Tc.REGION_CODE,
					translateHoldingArea(Tc.REGION_CODE, dboHolding, localeId, rdr, svr));
			currentJson.addProperty(Tc.MUNIC_CODE, translateHoldingArea(Tc.MUNIC_CODE, dboHolding, localeId, rdr, svr));
			currentJson.addProperty(Tc.VILLAGE_CODE,
					translateHoldingArea(Tc.VILLAGE_CODE, dboHolding, localeId, rdr, svr));
		}
	}

	String translateHoldingArea(String areaType, DbDataObject dboHolding, String localeId, Reader rdr, SvReader svr)
			throws SvException {
		String result = "";
		if (areaType.equals(Tc.REGION_CODE) || areaType.equals(Tc.VILLAGE_CODE) || areaType.equals(Tc.MUNIC_CODE)) {
			result = rdr.decodeCodeValue(SvReader.getTypeIdByName(Tc.VILLAGE), areaType,
					dboHolding.getVal(areaType).toString(), localeId, svr);
		}
		return result;
	}

}
