package naits_triglav_plugin;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.DateTimeComparator;
import org.joda.time.format.DateTimeFormat;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.prtech.svarog.SvCore;
import com.prtech.svarog.SvException;
import com.prtech.svarog.SvLink;
import com.prtech.svarog.SvLock;
import com.prtech.svarog.SvReader;
import com.prtech.svarog.SvSequence;
import com.prtech.svarog.SvWorkflow;
import com.prtech.svarog.SvWriter;
import com.prtech.svarog.svCONST;
import com.prtech.svarog_common.DbDataArray;
import com.prtech.svarog_common.DbDataObject;
import com.prtech.svarog_common.DbSearchCriterion;
import com.prtech.svarog_common.DbSearchCriterion.DbCompareOperand;
import com.prtech.svarog_common.DbSearchExpression;

public class HerdActions {

	static final Logger log4j = LogManager.getLogger(HerdActions.class.getName());

	static final boolean isInitialized = initPlugin();

	static boolean initPlugin() {
		OnSaveValidations call = new OnSaveValidations();
		SvCore.registerOnSaveCallback(call, SvReader.getTypeIdByName(Tc.HERD));
		SvCore.registerOnSaveCallback(call, SvReader.getTypeIdByName(Tc.HOLDING));
		return true;
	}

	public String herdMassHandler(JsonObject jsonData, String sessionId) throws SvException {
		String result = Tc.successMassAnimalsAction;
		SvReader svr = null;
		SvWriter svw = null;
		Writer wr = null;
		try {
			svr = new SvReader(sessionId);
			svw = new SvWriter(svr);
			wr = new Writer();
			String actionName = null;
			String subActionName = null;
			String animalClass = null;
			Long herdObjId = null;

			String herdName = null;
			Long herdContactPersonId = null;
			String herdNote = null;
			int count = 0;

			DbDataObject dboHerd = null;
			DbDataObject existingHerd = null;
			DbDataArray linksAnimalAndHerdToSave = new DbDataArray();
			JsonArray jsonArrayData = jsonData.get(Tc.OBJ_ARRAY).getAsJsonArray();
			JsonArray jsonParams = jsonData.get(Tc.OBJ_PARAMS).getAsJsonArray();

			for (JsonElement jsonElement : jsonParams) {
				JsonObject obj = jsonElement.getAsJsonObject();
				if (obj.has(Tc.MASS_PARAM_ACTION)) {
					actionName = obj.get(Tc.MASS_PARAM_ACTION).getAsString().toUpperCase();
				}
				if (obj.has(Tc.MASS_PARAM_SUBACTION)) {
					subActionName = obj.get(Tc.MASS_PARAM_SUBACTION).getAsString();
				}
				if (obj.has(Tc.MASS_PARAM_ANIMAL_CLASS)) {
					animalClass = obj.get(Tc.MASS_PARAM_ANIMAL_CLASS).getAsString();
				}
				if (obj.has(Tc.MASS_PARAM_HERD_OBJ_ID)) {
					herdObjId = obj.get(Tc.MASS_PARAM_HERD_OBJ_ID).getAsLong();
				}
				if (obj.has(Tc.MASS_PARAM_HERD_NAME)) {
					herdName = obj.get(Tc.MASS_PARAM_HERD_NAME).getAsString();
				}
				if (obj.has(Tc.MASS_PARAM_HERD_CONTACT_PERSON_ID)) {
					herdContactPersonId = obj.get(Tc.MASS_PARAM_HERD_CONTACT_PERSON_ID).getAsLong();
				}
				if (obj.has(Tc.MASS_PARAM_HERD_NOTE)) {
					herdNote = obj.get(Tc.MASS_PARAM_HERD_NOTE).getAsString();
				}
			}
			DbDataObject animalTypeDesc = SvReader.getDbtByName(Tc.ANIMAL);
			for (JsonElement jsonElement : jsonArrayData) {
				DbDataObject dboObjectToHandle = null;

				JsonObject obj = jsonElement.getAsJsonObject();
				if (obj.get(Tc.ANIMAL + "." + Tc.OBJECT_ID) != null) {
					dboObjectToHandle = svr.getObjectById(obj.get(Tc.ANIMAL + "." + Tc.OBJECT_ID).getAsLong(),
							animalTypeDesc, null);
				}
				DbDataObject dboObjToHandleParent = svr.getObjectById(dboObjectToHandle.getParent_id(),
						SvReader.getTypeIdByName(Tc.HOLDING), null);

				switch (actionName.toUpperCase()) {
				case Tc.HERD_ACTIONS:
					switch (subActionName.toUpperCase()) {
					case Tc.ADD_ANIMAL_TO_HERD:
						addAnimalToHerdValidationSet(dboObjectToHandle, svr);
						if (!dboObjectToHandle.getVal(Tc.ANIMAL_CLASS).toString().equals(animalClass)) {
							throw (new SvException("naits.error.allAnimalsMustHaveSameClassToBeAddToHerd",
									svr.getInstanceUser()));
						}
						if (herdObjId == 0L) {
							if (dboObjToHandleParent != null) {
								dboHerd = createHerd(dboObjToHandleParent.getObject_id(), animalClass, herdName,
										herdContactPersonId, herdNote, dboObjectToHandle, null, svr);
								svw.saveObject(dboHerd);
								herdObjId = dboHerd.getObject_id();
							}
							DbDataObject dboLinkAnimalHerd = createLinkBetweenAnimalAndHerd(dboObjectToHandle, dboHerd,
									wr, svr);
							linksAnimalAndHerdToSave.addDataItem(dboLinkAnimalHerd);
							count++;
							dboObjectToHandle.setVal(Tc.IS_IN_HERD, true);
							if (dboHerd.getVal(Tc.HERD_ID) != null) {
								dboObjectToHandle.setVal(Tc.HERD_ID, dboHerd.getVal(Tc.HERD_ID).toString());
							}
							svw.saveObject(dboObjectToHandle);
						} else {
							existingHerd = svr.getObjectById(herdObjId, SvReader.getTypeIdByName(Tc.HERD), null);
							if (existingHerd != null) {
								if (!existingHerd.getStatus().equalsIgnoreCase(Tc.VALID)) {
									throw (new SvException(
											"naits.error.animalCannotBeAddedToHerdWithStatusDifferentThanValid",
											svr.getInstanceUser()));
								}
								DbDataObject dboLinkAnimalHerd = createLinkBetweenAnimalAndHerd(dboObjectToHandle,
										existingHerd, wr, svr);
								linksAnimalAndHerdToSave.addDataItem(dboLinkAnimalHerd);
								count++;
								dboObjectToHandle.setVal(Tc.IS_IN_HERD, true);
								if (existingHerd.getVal(Tc.HERD_ID) != null) {
									dboObjectToHandle.setVal(Tc.HERD_ID, existingHerd.getVal(Tc.HERD_ID).toString());
								}
								svw.saveObject(dboObjectToHandle);
							}
						}
						break;
					case Tc.REMOVE_ANIMAL_FROM_HERD:
						if (!invalidateLinkBetweenAnimalAndHerd(dboObjectToHandle, herdObjId, svr)) {
							result = "naits.error.failedToInactivateLinkBetweenAnimalAndHerd";
						} else {
							dboObjectToHandle.setVal(Tc.IS_IN_HERD, null);
							dboObjectToHandle.setVal(Tc.HERD_ID, null);
							svw.saveObject(dboObjectToHandle);
						}
					default:
						break;
					}
				default:
					break;
				}
				if (count == 100) {
					count = 0;
					svw.saveObject(linksAnimalAndHerdToSave, true, true);
					linksAnimalAndHerdToSave = new DbDataArray();
				}
			}
			if (!linksAnimalAndHerdToSave.getItems().isEmpty()) {
				svw.saveObject(linksAnimalAndHerdToSave, true, true);
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
		}
		return result;
	}

	public String addHerdToHolding(JsonObject jsonData, String sessionId) throws SvException {
		String result = Tc.successMassAnimalsAction;
		SvReader svr = null;
		SvWriter svw = null;
		try {
			svr = new SvReader(sessionId);
			svw = new SvWriter(svr);

			String actionName = null;
			Long holdingObjId = null;
			String herdAnimalType = null;
			String herdName = null;
			Long herdContactPersonId = null;
			String herdNote = null;

			JsonArray jsonArrayData = jsonData.get(Tc.OBJ_ARRAY).getAsJsonArray();
			JsonArray jsonParams = jsonData.get(Tc.OBJ_PARAMS).getAsJsonArray();

			for (JsonElement jsonElement : jsonParams) {
				JsonObject obj = jsonElement.getAsJsonObject();
				if (obj.has(Tc.MASS_PARAM_ACTION)) {
					actionName = obj.get(Tc.MASS_PARAM_ACTION).getAsString().toUpperCase();
				}
				if (obj.has(Tc.MASS_PARAM_HOLDING_OBJ_ID)) {
					holdingObjId = obj.get(Tc.MASS_PARAM_HOLDING_OBJ_ID).getAsLong();
				}
				if (obj.has(Tc.MASS_PARAM_HERD_ANIMAL_TYPE)) {
					herdAnimalType = obj.get(Tc.MASS_PARAM_HERD_ANIMAL_TYPE).getAsString();
				}
				if (obj.has(Tc.MASS_PARAM_HERD_NAME)) {
					herdName = obj.get(Tc.MASS_PARAM_HERD_NAME).getAsString();
				}
				if (obj.has(Tc.MASS_PARAM_HERD_CONTACT_PERSON_ID)) {
					herdContactPersonId = obj.get(Tc.MASS_PARAM_HERD_CONTACT_PERSON_ID).getAsLong();
				}
				if (obj.has(Tc.MASS_PARAM_HERD_NOTE)) {
					herdNote = obj.get(Tc.MASS_PARAM_HERD_NOTE).getAsString();
				}
			}
			DbDataObject herdTypeDesc = SvReader.getDbtByName(Tc.HERD);
			DbDataObject dboObjToHandleParent = null;
			for (JsonElement jsonElement : jsonArrayData) {
				DbDataObject dboObjectToHandle = null;
				JsonObject obj = jsonElement.getAsJsonObject();
				if (obj.get(Tc.HERD + "." + Tc.OBJECT_ID) != null) {
					dboObjectToHandle = svr.getObjectById(obj.get(Tc.HERD + "." + Tc.OBJECT_ID).getAsLong(),
							herdTypeDesc, null);
				}

				if (holdingObjId != null) {
					dboObjToHandleParent = svr.getObjectById(holdingObjId, SvReader.getTypeIdByName(Tc.HOLDING), null);
				}
				switch (actionName.toUpperCase()) {
				case Tc.ADD_HERD_TO_HOLDING:
					dboObjectToHandle = createHerd(dboObjToHandleParent.getObject_id(), herdAnimalType, herdName,
							herdContactPersonId, herdNote, null, herdAnimalType, svr);
					svw.saveObject(dboObjectToHandle);
				default:
					break;
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
		}
		return result;
	}

	public DbDataObject createHerd(Long parentId, String animalType, String name, Long contactPersonId, String note,
			DbDataObject dboAnimal, String herdAnimalType, SvReader svr) {
		DbDataObject dbo = new DbDataObject();
		dbo.setObject_type(SvReader.getTypeIdByName(Tc.HERD));
		dbo.setParent_id(parentId);
		String generatedSeq = generateHerdIdSequence(dboAnimal, herdAnimalType, null, true, svr);
		if (generatedSeq != null) {
			dbo.setVal(Tc.HERD_ID, generatedSeq);
		}
		dbo.setVal(Tc.ANIMAL_TYPE, animalType);
		dbo.setVal(Tc.NAME, name);
		dbo.setVal(Tc.CONTACT_PERSON_ID, contactPersonId);
		dbo.setVal(Tc.NOTE, note);
		return dbo;
	}

	public DbDataObject createHerdWithoutHerdId(Long parentId, String animalType, String name, Long contactPersonId,
			SvReader svr) {
		DbDataObject dbo = new DbDataObject();
		dbo.setObject_type(SvReader.getTypeIdByName(Tc.HERD));
		if (parentId != null) {
			dbo.setParent_id(parentId);
		}
		dbo.setVal(Tc.ANIMAL_TYPE, animalType);
		dbo.setVal(Tc.CONTACT_PERSON_ID, contactPersonId);
		if (name != null) {
			dbo.setVal(Tc.NAME, name);
		}
		return dbo;
	}

	public DbDataObject createLinkBetweenAnimalAndHerd(DbDataObject dboAnimal, DbDataObject dboHerd, Writer wr,
			SvReader svr) {
		DbDataObject dboLink = null;
		if (dboAnimal != null && dboHerd != null) {
			DbDataObject dbLinkAnimalAndHerd = SvReader.getLinkType(Tc.ANIMAL_HERD, SvReader.getTypeIdByName(Tc.ANIMAL),
					SvReader.getTypeIdByName(Tc.HERD));
			dboLink = wr.createSvarogLink(dbLinkAnimalAndHerd.getObject_id(), dboAnimal, dboHerd);

		}
		return dboLink;
	}

	public Boolean checkIfAnimalAlreadyBelongsInHerd(Long animalObjid, SvReader svr) throws SvException {
		Boolean belongsInHerd = false;
		DbDataObject dboAnimal = svr.getObjectById(animalObjid, SvReader.getTypeIdByName(Tc.ANIMAL), null);
		DbDataObject dbLink = SvLink.getLinkType(Tc.ANIMAL_HERD, SvReader.getTypeIdByName(Tc.ANIMAL),
				SvReader.getTypeIdByName(Tc.HERD));
		DbDataArray dbArrHerds = null;
		if (dboAnimal != null && dbLink != null) {
			dbArrHerds = svr.getObjectsByLinkedId(dboAnimal.getObject_id(), dbLink, null, 0, 0);
			if (dbArrHerds != null && !dbArrHerds.getItems().isEmpty()) {
				belongsInHerd = true;
			}
		}
		return belongsInHerd;
	}

	public Boolean removeAnimalFromHerd(Long animalObjid, SvReader svr) throws SvException {
		Boolean removedFromHerd = false;
		DbDataObject dboAnimal = svr.getObjectById(animalObjid, SvReader.getTypeIdByName(Tc.ANIMAL), null);
		DbDataObject dbLink = SvLink.getLinkType(Tc.ANIMAL_HERD, SvReader.getTypeIdByName(Tc.ANIMAL),
				SvReader.getTypeIdByName(Tc.HERD));
		DbDataArray dbArrHerds = null;
		if (dboAnimal != null && dbLink != null) {
			dbArrHerds = svr.getObjectsByLinkedId(dboAnimal.getObject_id(), dbLink, null, 0, 0);
			if (dbArrHerds != null && !dbArrHerds.getItems().isEmpty()) {
				removedFromHerd = invalidateLinkBetweenAnimalAndHerd(dboAnimal,
						dbArrHerds.getItems().get(0).getObject_id(), svr);
			}
		}
		return removedFromHerd;
	}

	/**
	 * Method that return animals linked to herd
	 * 
	 * @param herdObjId
	 *            Herd object id
	 * @param svr
	 *            SvReader instance
	 * @return
	 * @throws SvException
	 */
	public DbDataArray getAnimalsInHerd(Long herdObjId, SvReader svr) throws SvException {
		DbDataArray dbArrAnimalsInHerd = null;
		if (herdObjId != null) {
			DbDataObject dbLink = SvLink.getLinkType(Tc.ANIMAL_HERD, SvReader.getTypeIdByName(Tc.ANIMAL),
					SvReader.getTypeIdByName(Tc.HERD));
			dbArrAnimalsInHerd = svr.getObjectsByLinkedId(herdObjId, SvReader.getTypeIdByName(Tc.HERD), dbLink,
					SvReader.getTypeIdByName(Tc.ANIMAL), true, null, 0, 0);
		}
		return dbArrAnimalsInHerd;
	}

	/**
	 * Method that return size of array (animals in herd)
	 * 
	 * @param herdObjId
	 *            Herd object id
	 * @param svr
	 *            SvReader instance
	 * @return
	 * @throws SvException
	 */
	public Long getSizeOfListAnimalsInHerd(Long herdObjId, SvReader svr) throws SvException {
		Long result = 0L;
		DbDataArray dbArr = getAnimalsInHerd(herdObjId, svr);
		if (dbArr != null && !dbArr.getItems().isEmpty()) {
			result = (long) dbArr.size();
		}
		return result;
	}

	public String generateHerdIdSequence(DbDataObject dboAnimal, String animalType, Long holdingObjId,
			Boolean customSave, SvReader svr) {
		SvSequence svs = null;
		String pic = null;
		String generatedSeq = null;
		DbDataObject dboHolding = null;
		try {
			svs = new SvSequence(svr.getSessionId());
			if (customSave) {
				if (dboAnimal != null && dboAnimal.getParent_id() != null) {
					dboHolding = svr.getObjectById(dboAnimal.getParent_id(), SvReader.getTypeIdByName(Tc.HOLDING),
							null);
					if (dboHolding.getVal(Tc.PIC) != null) {
						pic = dboHolding.getVal(Tc.PIC).toString();
					}
				}

			} else {
				if (holdingObjId != null) {
					dboHolding = svr.getObjectById(holdingObjId, SvReader.getTypeIdByName(Tc.HOLDING), null);
					if (dboHolding.getVal(Tc.PIC) != null) {
						pic = dboHolding.getVal(Tc.PIC).toString();
					}
				}
			}
			if (!pic.equals(Tc.EMPTY_STRING)) {
				Long seqId = svs.getSeqNextVal(pic, false);
				String formattedSeq = String.format("%03d", Integer.valueOf(seqId.toString()));
				if (animalType == null) {
					generatedSeq = pic + Tc.PATH_DELIMITER + dboAnimal.getVal(Tc.ANIMAL_CLASS).toString()
							+ Tc.PATH_DELIMITER + formattedSeq;
				} else {
					generatedSeq = pic + Tc.PATH_DELIMITER + animalType + Tc.PATH_DELIMITER + formattedSeq;
				}
			}
			svs.dbCommit();
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
		} finally {
			if (svs != null)
				svs.release();
		}
		return generatedSeq;
	}

	public void addAnimalToHerdValidationSet(DbDataObject dboAnimal, SvReader svr) throws SvException {
		if (dboAnimal != null) {
			if (!dboAnimal.getStatus().equals(Tc.VALID)) {
				throw (new SvException("naits.error.onlyValidAnimalCanBeHandledWithChoosenAction",
						svr.getInstanceUser()));
			}
			if (checkIfAnimalAlreadyBelongsInHerd(dboAnimal.getObject_id(), svr)) {
				throw (new SvException("naits.error.animalAlreadyBelongsToHerd", svr.getInstanceUser()));
			}
		}
	}

	public boolean invalidateLinkBetweenAnimalAndHerd(DbDataObject dboAnimal, Long herdObjId, SvReader svr)
			throws SvException {
		Boolean result = false;
		Reader rdr = new Reader();
		Writer wr = new Writer();
		if (dboAnimal != null && herdObjId != null) {
			DbDataObject dboHerd = svr.getObjectById(herdObjId, SvReader.getTypeIdByName(Tc.HERD), null);
			if (rdr.checkIfLinkExists(dboAnimal, dboHerd, Tc.ANIMAL_HERD, null, svr)) {
				DbDataObject dbLink = SvReader.getLinkType(Tc.ANIMAL_HERD, SvReader.getTypeIdByName(Tc.ANIMAL),
						SvReader.getTypeIdByName(Tc.HERD));
				DbDataObject dboLink = rdr.getLinkObject(dboAnimal.getObject_id(), dboHerd.getObject_id(),
						dbLink.getObject_id(), svr);
				wr.invalidateLink(dboLink, true, svr);
				result = true;
			}
		}
		return result;
	}

	public void generateHerdIdSequence(DbDataObject dbo, String animalType, Long holdingObjId, SvReader svr) {
		SvSequence svs = null;
		String pic = null;
		String generatedSeq = null;
		DbDataObject dboHolding = null;
		try {
			svs = new SvSequence(svr.getSessionId());
			if (holdingObjId != null) {
				dboHolding = svr.getObjectById(holdingObjId, SvReader.getTypeIdByName(Tc.HOLDING), null);
				if (dboHolding.getVal(Tc.PIC) != null) {
					pic = dboHolding.getVal(Tc.PIC).toString();
				}
			}
			if (!pic.equals(Tc.EMPTY_STRING) && animalType != null) {
				Long seqId = svs.getSeqNextVal(pic, false);
				String formattedSeq = String.format("%03d", Integer.valueOf(seqId.toString()));
				generatedSeq = pic + Tc.PATH_DELIMITER + animalType + Tc.PATH_DELIMITER + formattedSeq;
				dbo.setVal(Tc.HERD_ID, generatedSeq);
				svs.dbCommit();
			}
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
		} finally {
			if (svs != null)
				svs.release();
		}
	}

	public JsonArray prepareJsonObjectForHerdMassActions(Long herdObjId, SvReader svr) throws SvException {
		DbDataArray dbArrAnimalsInHerd = new DbDataArray();
		JsonArray jArrOfAnimals = new JsonArray();
		Gson gson = new Gson();
		Reader rdr = new Reader();
		if (herdObjId != null) {
			dbArrAnimalsInHerd = getAnimalsInHerd(herdObjId, svr);
		}
		if (dbArrAnimalsInHerd != null && !dbArrAnimalsInHerd.getItems().isEmpty()) {
			String dbDataArrayToJsonFormat = rdr.convertDbDataArrayToGridJson(dbArrAnimalsInHerd, Tc.ANIMAL);
			jArrOfAnimals = gson.fromJson(dbDataArrayToJsonFormat, JsonArray.class);
		}
		return jArrOfAnimals;
	}

	/**
	 * Get specific value from appropriate key in Json object
	 * 
	 * @param jsonData
	 *            Json object
	 * @param param
	 *            Specific key
	 * @return
	 */
	public String getValueFromJson(JsonObject jsonData, String param) {
		String subAction = Tc.EMPTY_STRING;
		JsonArray jsonParams = jsonData.get(Tc.OBJ_PARAMS).getAsJsonArray();
		for (JsonElement jsonElement : jsonParams) {
			JsonObject obj = jsonElement.getAsJsonObject();
			if (obj.has(param)) {
				subAction = obj.get(param).getAsString().toUpperCase();
			}
		}
		return subAction;
	}

	public DbDataObject createHerdMovementWithoutDestination(DbDataObject movementParentObj, String typeOfMovement,
			String dateOfMovement, SvReader svr, SvWriter svw, SvWorkflow sww) throws SvException {
		DbDataObject dboHerdMovement = createHerdMovementObject(movementParentObj, null, typeOfMovement, null, null,
				dateOfMovement, null, dateOfMovement, null, null, null, null, null, svw, svr);
		sww.moveObject(movementParentObj, typeOfMovement);
		return dboHerdMovement;
	}

	public DbDataObject createHerdMovementObject(DbDataObject dboHerd, String movementDocumentId, String movementAction,
			String movementReason, DbDataObject destinationHoldingObj, String estmDateDeparture, String estmDateArrival,
			String departureDate, String movementTransportType, String movementTransporterId,
			String movementTransporterName, String transporterLicense, String disinfectionDate, SvWriter svw,
			SvReader svr) throws SvException {
		Writer wr = new Writer();
		Reader rdr = new Reader();
		ValidationChecks vc = new ValidationChecks();
		String pattern = Tc.DATE_PATTERN;
		DbDataObject dboHerdMovement = new DbDataObject();
		dboHerdMovement.setObject_type(SvReader.getTypeIdByName(Tc.HERD_MOVEMENT));

		if (dboHerd != null) {
			dboHerdMovement.setParent_id(dboHerd.getObject_id());
		}
		dboHerdMovement.setStatus(Tc.VALID);
		if (movementAction != null && !movementAction.isEmpty()) {
			dboHerdMovement.setVal(Tc.MOVEMENT_ACTION, movementAction);
		}
		if (movementReason != null && !movementReason.isEmpty()) {
			dboHerdMovement.setVal(Tc.MOVEMENT_REASON, movementReason);
		}
		DbDataObject dboSourceHolding = svr.getObjectById(dboHerd.getParent_id(), SvReader.getTypeIdByName(Tc.HOLDING),
				null);
		if (dboSourceHolding != null) {
			dboHerdMovement.setVal(Tc.SOURCE_HOLDING_ID, dboSourceHolding.getVal(Tc.PIC).toString());
		}

		if (destinationHoldingObj != null) {
			dboHerdMovement.setVal(Tc.DESTINATION_HOLDING_ID, destinationHoldingObj.getVal(Tc.PIC).toString());
		}

		dboHerdMovement.setVal(Tc.USER_SENDER, svr.getInstanceUser().getVal(Tc.USER_NAME));
		// dboHerdMovement.setVal(Tc.USER_RECIPIENT, null);

		if (estmDateDeparture != null && !estmDateDeparture.equals("null") && !estmDateDeparture.equals("")) {
			DateTime convertedDate = DateTime.parse(estmDateDeparture, DateTimeFormat.forPattern(pattern));
			String substr_date = convertedDate.toString().substring(0, 10);
			dboHerdMovement.setVal(Tc.ESTM_DATE_DEPARTURE, substr_date);
		}
		if (estmDateArrival != null && !estmDateArrival.equals("null") && !estmDateArrival.equals("")) {
			DateTime convertedDate = DateTime.parse(estmDateArrival, DateTimeFormat.forPattern(pattern));
			String substr_date = convertedDate.toString().substring(0, 10);
			dboHerdMovement.setVal(Tc.ESTM_DATE_ARRIVAL, substr_date);
		}

		if (departureDate != null && !departureDate.equals("null") && !departureDate.equals("")) {
			DateTime convertedDate = DateTime.parse(departureDate, DateTimeFormat.forPattern(pattern));
			String substr_date = convertedDate.toString().substring(0, 10);
			dboHerdMovement.setVal(Tc.DEPARTURE_DATE, substr_date);
		}

		if (movementTransportType != null && !movementTransportType.isEmpty()) {
			dboHerdMovement.setVal(Tc.TRANSPORT_TYPE, movementTransportType);
		}
		if (movementTransporterId != null && !movementTransporterId.isEmpty()) {
			dboHerdMovement.setVal(Tc.TRANSPORTER_ID, Long.valueOf(movementTransporterId));
		}
		if (movementTransporterName != null && !movementTransporterName.isEmpty()) {
			dboHerdMovement.setVal(Tc.TRANSPORTER_NAME, movementTransporterName);
		}
		if (transporterLicense != null && !transporterLicense.isEmpty()) {
			dboHerdMovement.setVal(Tc.TRANSPORTER_LICENSE, transporterLicense);
		}

		if (disinfectionDate != null && !disinfectionDate.equals("null") && !disinfectionDate.equals("")) {
			DateTime convertedDate = DateTime.parse(disinfectionDate, DateTimeFormat.forPattern(pattern));
			if (convertedDate.isAfter(new DateTime())) {
				throw (new SvException("naits.error.disinfectionDateCannotBeInTheFuture", svCONST.systemUser, null,
						null));
			}
			String substr_date = convertedDate.toString().substring(0, 10);
			dboHerdMovement.setVal(Tc.DESINFECTION_DATE, substr_date);
		}

		if (movementDocumentId != null) {
			dboHerdMovement.setVal(Tc.MOVEMENT_DOC_ID, movementDocumentId);
		}

		if (!(Tc.LOST.equals(movementAction) || Tc.SOLD.equals(movementAction))) {
			if (vc.checkIfHoldingBelongsToAffectedArea(dboSourceHolding, svr)
					|| vc.checkIfHoldingBelongsToAffectedArea(destinationHoldingObj, svr)) {
				Boolean isAllowed = rdr.checkifMovementIsAllowedDependOnAreaHealthStatus(dboSourceHolding, "",
						new ArrayList<>(), new ArrayList<>(), dboHerd, dboHerdMovement, svr);
				if (!isAllowed.booleanValue()) {
					dboHerdMovement.setStatus(Tc.REJECTED);
				}
			}
		}
		svw.saveObject(dboHerdMovement);
		if (destinationHoldingObj != null && checkIfHerdMovementExists(dboHerd, destinationHoldingObj, false, svr)) {
			wr.linkObjects(dboHerdMovement, destinationHoldingObj, Tc.HERD_MOVEMENT_HOLDING, "linked via WS", svw);
		}
		return dboHerdMovement;
	}

	/**
	 * Method that create Herd heatlh book object
	 * 
	 * @param parentId
	 *            Herd object ID
	 * @param dateOfAction
	 *            Date of action
	 * @param actionType
	 *            VACC_ACTIVITY_TYPE codelist
	 * @param tretmType
	 *            TRETM_TYPE codelist
	 * @param note
	 *            Note
	 * @param headCount
	 *            Number of animals
	 * @param svr
	 * @return
	 */
	public DbDataObject createHerdHealthBook(Long parentId, String dateOfAction, String actionType, String tretmType,
			String note, Long headCount, SvReader svr) {
		DbDataObject dbo = new DbDataObject();
		dbo.setObject_type(SvReader.getTypeIdByName(Tc.HERD_HEALTH_BOOK));
		dbo.setParent_id(parentId);
		dbo.setVal(Tc.DATE_OF_ACTION, dateOfAction);
		dbo.setVal(Tc.ACTION_TYPE, actionType);
		dbo.setVal(Tc.TRETM_TYPE, tretmType);
		dbo.setVal(Tc.NOTE, note);
		dbo.setVal(Tc.HEADCOUNT, headCount);
		return dbo;
	}

	public DbDataObject startHerdMovement(DbDataObject dboHerd, String movementDocumentId, String movementAction,
			String movementReason, DbDataObject destinationHoldingObj, String estmDateDeparture, String estmDateArrival,
			String departureDate, String movementTransportType, String movementTransporterId,
			String movementTransporterName, String transporterLicense, String disinfectionDate, SvWorkflow sww,
			SvWriter svw, SvReader svr) throws SvException {
		DbDataObject dboHerdMovement = null;
		if (dboHerd != null) {
			dboHerdMovement = createHerdMovementObject(dboHerd, movementDocumentId, movementAction, movementReason,
					destinationHoldingObj, estmDateDeparture, estmDateArrival, departureDate, movementTransportType,
					movementTransporterId, movementTransporterName, transporterLicense, disinfectionDate, svw, svr);
			if (dboHerdMovement != null && !dboHerdMovement.getStatus().equals(Tc.REJECTED)) {
				sww.moveObject(dboHerd, Tc.TRANSITION);
			}
		}
		return dboHerdMovement;
	}

	public void finishHerdMovement(DbDataObject dboHerd, DbDataObject dboDestinationHolding, String dateOfMovement,
			String dateOfAdmission, String transporterId, SvWorkflow sww, SvReader svr) throws SvException {
		ValidationChecks vc = new ValidationChecks();
		DbDataObject dboHerdMovement = null;

		DbDataArray dbArrExistingMovements = getExistingMovementsForHerd(dboHerd, dboDestinationHolding, Tc.VALID, svr);
		if (dbArrExistingMovements != null && !dbArrExistingMovements.getItems().isEmpty()) {
			dboHerdMovement = dbArrExistingMovements.get(0);
			if (dboHerd != null && dboDestinationHolding != null) {
				dboHerd.setParent_id(dboDestinationHolding.getObject_id());
				dboHerdMovement.setVal(Tc.RECIPIENT_USER, svr.getInstanceUser().getVal(Tc.USER_NAME));

				if (dateOfMovement != null && !dateOfMovement.equals("null") && !dateOfMovement.equals("")) {
					String pattern = Tc.DATE_PATTERN;
					DateTime convertedDate = DateTime.parse(dateOfMovement, DateTimeFormat.forPattern(pattern));
					if (convertedDate.isAfter(new DateTime())) {
						throw (new SvException("naits.error.movementDateCannotBeInTheFuture", svCONST.systemUser, null,
								null));
					}
					dboHerdMovement.setVal(Tc.ARRIVAL_DATE, convertedDate);
				}
				if (dboHerdMovement.getVal(Tc.ARRIVAL_DATE) == null) {
					Calendar calendar = Calendar.getInstance();
					java.sql.Date dtNow = new java.sql.Date(calendar.getTime().getTime());
					dboHerdMovement.setVal(Tc.ARRIVAL_DATE, dtNow);
				}

				if (dboDestinationHolding != null) {
					dboHerdMovement.setVal(Tc.DESTINATION_HOLDING_ID, dboDestinationHolding.getVal(Tc.PIC).toString());
				}

				if (dboHerdMovement.getVal(Tc.DESTINATION_HOLDING_ID) == null && dboHerd.getStatus().equals(Tc.SOLD)
						|| dboHerd.getStatus().equals(Tc.LOST)) {
					dboHerdMovement.setVal(Tc.DESTINATION_HOLDING_ID, dboDestinationHolding.getVal(Tc.PIC).toString());
				}

				if (dboHerdMovement.getVal(Tc.MOVEMENT_ACTION) != null
						&& (dboHerdMovement.getVal(Tc.MOVEMENT_ACTION).toString().equalsIgnoreCase(Tc.SOLD)
								|| dboHerdMovement.getVal(Tc.MOVEMENT_ACTION).toString().equalsIgnoreCase(Tc.LOST))) {
					dboHerdMovement.setVal(Tc.MOVEMENT_ACTION, Tc.RETIRE_TRANSFER);
				}

				if (vc.checkIfHoldingIsSlaughterhouse(dboDestinationHolding.getObject_id(), svr)) {
					if (dateOfAdmission != null && !"null".equals(dateOfAdmission)) {
						String pattern = Tc.DATE_PATTERN;
						DateTime convertedDate = DateTime.parse(dateOfAdmission, DateTimeFormat.forPattern(pattern));
						if (convertedDate.isAfter(new DateTime())) {
							throw (new SvException("naits.error.admittanceDateCannotBeInTheFuture", svCONST.systemUser,
									null, null));
						}
						if (dboHerdMovement.getVal(Tc.ARRIVAL_DATE) != null) {
							DateTime dtArrival = new DateTime(dboHerdMovement.getVal(Tc.ARRIVAL_DATE));
							if (DateTimeComparator.getDateOnlyInstance().compare(dtArrival, convertedDate) >= 1) {
								throw (new SvException("naits.error.dateOfMovementCannotBeAfterDateOfAdmission",
										svCONST.systemUser, null, null));
							}
						}
						dboHerdMovement.setVal(Tc.ADMITTANCE_DATE, convertedDate);
					}
					if (transporterId != null && !"null".equals(transporterId)) {
						Long holdingResponsibleObjId = Long.valueOf(transporterId);
						DbDataObject dboHoldingResponsible = svr.getObjectById(holdingResponsibleObjId,
								SvReader.getTypeIdByName(Tc.HOLDING_RESPONSIBLE), null);
						dboHerdMovement.setVal(Tc.TRANSPORTER_ID, holdingResponsibleObjId);
						String holdingResponsibleFullName = Tc.EMPTY_STRING;
						if (dboHoldingResponsible.getVal(Tc.FULL_NAME) != null) {
							holdingResponsibleFullName = dboHoldingResponsible.getVal(Tc.FULL_NAME).toString();
						}
						if ((holdingResponsibleFullName == null || holdingResponsibleFullName.length() < 2)
								&& dboHoldingResponsible.getVal(Tc.FIRST_NAME) != null
								&& dboHoldingResponsible.getVal(Tc.LAST_NAME) != null) {
							holdingResponsibleFullName = dboHoldingResponsible.getVal(Tc.FIRST_NAME).toString() + " "
									+ dboHoldingResponsible.getVal(Tc.LAST_NAME).toString();
						}
						dboHerdMovement.setVal(Tc.TRANSPORTER_NAME, holdingResponsibleFullName);
					}
				}

				if (dboHerdMovement.getIs_dirty()) {
					sww.moveObject(dboHerdMovement, Tc.FINISHED, true);
				}
			}
		}
	}

	public DbDataArray getExistingMovementsForHerd(DbDataObject dboHerd, DbDataObject dboDestinationHolding,
			String status, SvReader svr) throws SvException {
		DbDataObject sourceHolding = svr.getObjectById(dboHerd.getParent_id(), SvReader.getTypeIdByName(Tc.HOLDING),
				null);
		DbSearchCriterion cr1 = new DbSearchCriterion(Tc.PARENT_ID, DbCompareOperand.EQUAL, dboHerd.getObject_id());
		DbSearchCriterion cr2 = new DbSearchCriterion(Tc.SOURCE_HOLDING_ID, DbCompareOperand.EQUAL,
				sourceHolding.getVal(Tc.PIC).toString());
		DbSearchCriterion cr3 = new DbSearchCriterion(Tc.DESTINATION_HOLDING_ID, DbCompareOperand.EQUAL,
				dboDestinationHolding.getVal(Tc.PIC).toString());
		DbSearchCriterion cr4 = new DbSearchCriterion(Tc.STATUS, DbCompareOperand.EQUAL, status);
		DbSearchExpression dbse = new DbSearchExpression();
		dbse.addDbSearchItem(cr1).addDbSearchItem(cr2).addDbSearchItem(cr3).addDbSearchItem(cr4);
		return svr.getObjects(dbse, SvCore.getTypeIdByName(Tc.HERD_MOVEMENT), null, 0, 0);
	}

	// public void cancelMovement(DbDataObject dboHerd, SvWriter svw, SvWorkflow
	// sww, SvReader svr) throws SvException {
	// DbDataArray dbArrExistingMovements =
	// svr.getObjectsByParentId(dboHerd.getObject_id(),
	// SvReader.getTypeIdByName(Tc.HERD_MOVEMENT), null, 0, 0);
	// if (dbArrExistingMovements != null &&
	// !dbArrExistingMovements.getItems().isEmpty()) {
	// for (DbDataObject dboTempMovement : dbArrExistingMovements.getItems()) {
	// if (dboTempMovement.getStatus().equals(Tc.VALID)) {
	// dboTempMovement.setStatus(Tc.CANCELED);
	// } else {
	// dboTempMovement.setStatus(Tc.VALID);
	// }
	// svw.saveObject(dboTempMovement);
	// }
	// }
	// if (!dboHerd.getStatus().equals(Tc.VALID)) {
	// sww.moveObject(dboHerd, Tc.VALID, true);
	// }
	// }

	public void cancelMovement(DbDataObject dboHerd, SvWorkflow sww, SvReader svr) throws SvException {
		DbDataObject dboHerdMovement = null;
		ReentrantLock lock = null;
		try {
			DbDataArray existingMovements = svr.getObjectsByParentId(dboHerd.getObject_id(),
					SvReader.getTypeIdByName(Tc.HERD_MOVEMENT), null, 0, 0);
			if (existingMovements != null && !existingMovements.getItems().isEmpty()) {
				for (DbDataObject tempObj : existingMovements.getItems()) {
					if (tempObj.getStatus().equals(Tc.VALID)) {
						dboHerdMovement = tempObj;
						lock = SvLock.getLock("HERD-MASS-HANDLER-CANCEL-MOV-" + dboHerdMovement.getObject_id(), false,
								0);
						if (lock == null) {
							throw (new SvException("naits.error.objectUsedByOtherSession", svr.getInstanceUser()));
						}
						break;
					}
				}
				if (dboHerdMovement != null && dboHerdMovement.getStatus().equals(Tc.VALID)) {
					if (!dboHerd.getStatus().equals(Tc.VALID)) {
						sww.moveObject(dboHerd, Tc.VALID);
					}
					sww.moveObject(dboHerdMovement, Tc.CANCELED);
				}
			}
		} finally {
			if (lock != null && dboHerdMovement != null) {
				SvLock.releaseLock("HERD-MASS-HANDLER-CANCEL-MOV-" + dboHerdMovement.getObject_id(), lock);
			}
		}
	}

	public DbDataArray getAllHoldingResponsiblesPerHerd(Long herdObjId, SvReader svr) throws SvException {
		DbDataArray dbArrResponsibles = null;
		DbSearchCriterion cr = new DbSearchCriterion(Tc.OBJECT_ID, DbCompareOperand.EQUAL, herdObjId);
		DbDataArray dbArrHerdHistory = svr.getObjectsHistory(new DbSearchExpression().addDbSearchItem(cr),
				SvReader.getTypeIdByName(Tc.HERD), 0, 0);
		if (dbArrHerdHistory != null && !dbArrHerdHistory.getItems().isEmpty()) {
			dbArrResponsibles = new DbDataArray();
			for (DbDataObject dboTempHerd : dbArrHerdHistory.getItems()) {
				if (dboTempHerd.getVal(Tc.CONTACT_PERSON_ID) != null) {
					DbDataObject dboHoldingResponsible = svr.getObjectById(
							(Long) dboTempHerd.getVal(Tc.CONTACT_PERSON_ID),
							SvReader.getTypeIdByName(Tc.HOLDING_RESPONSIBLE), null);
					dbArrResponsibles.addDataItem(dboHoldingResponsible);
				}
			}
			dbArrResponsibles.getSortedItems(Tc.PKID, true);
		}
		return dbArrResponsibles;
	}

	public Boolean checkIfHerdMovementExists(DbDataObject dboHerd, DbDataObject dboDestinationHolding,
			Boolean includeRejected, SvReader svr) throws SvException {
		Boolean result = false;
		DbDataArray existingHerdMovements = getExistingMovementsForHerd(dboHerd, dboDestinationHolding, Tc.VALID, svr);
		if (existingHerdMovements != null && !existingHerdMovements.getItems().isEmpty()) {
			result = true;
		} else if (includeRejected && existingHerdMovements.getItems().isEmpty()) {
			existingHerdMovements = getExistingMovementsForHerd(dboHerd, dboDestinationHolding, Tc.REJECTED, svr);
			if (!existingHerdMovements.getItems().isEmpty()) {
				result = true;
			}
		}
		return result;
	}

	public String updateStatusOfMovementDoc(JsonArray jsonData, String nextStatus, SvReader svr) throws SvException {
		String result = "naits.error.errorMovementDocStatusUpdate";
		SvWorkflow sww = null;
		SvWriter svw = null;
		Reader rdr = new Reader();
		Writer wr = new Writer();
		DbDataArray herdMovements = null;
		try {
			svw = new SvWriter(svr);
			sww = new SvWorkflow(svw);
			svr.setAutoCommit(false);

			String action = nextStatus.toUpperCase().trim();
			DbDataObject movementDocTypeDesc = SvReader.getDbtByName(Tc.MOVEMENT_DOC);

			for (JsonElement jsonElement : jsonData) {
				JsonObject obj = jsonElement.getAsJsonObject();
				DbDataObject dboObjectToHandle = null;
				if (obj.get("MOVEMENT_DOC.OBJECT_ID") != null) {
					dboObjectToHandle = svr.getObjectById(obj.get("MOVEMENT_DOC.OBJECT_ID").getAsLong(),
							movementDocTypeDesc, null);
				}
				if (dboObjectToHandle == null) {
					throw (new SvException("naits.error.no_movement_found", svr.getInstanceUser()));
				}

				herdMovements = getHerdMovementsByMovementDocument(dboObjectToHandle, svr);
				if (herdMovements == null || herdMovements.size() < 1) {
					throw (new SvException("naits.error.mvmDocIsEmpty", svr.getInstanceUser()));
				}

				switch (action) {
				case Tc.RELEASED:
					Boolean resolvedFlag = true;
					result = "naits.error.mvmDocStatusHasToBeDraft";
					if (dboObjectToHandle.getStatus().equalsIgnoreCase(Tc.DRAFT)) {
						result = "naits.success.releasedHerdMovementDoc";
						DbDataArray movementDocBlocksArr = svr.getObjectsByParentId(dboObjectToHandle.getObject_id(),
								SvReader.getTypeIdByName(Tc.MOVEMENT_DOC_BLOCK), null, 0, 0);

						if (movementDocBlocksArr != null && !movementDocBlocksArr.getItems().isEmpty()) {
							for (DbDataObject mvmDocBlockObj : movementDocBlocksArr.getItems()) {
								if (mvmDocBlockObj.getStatus().equals(Tc.UNRESOLVED)) {
									resolvedFlag = false;
									break;
								}
							}
							result = "naits.error.mvmDocHaveMvmDocBlocked";
							if (resolvedFlag.equals(true)) {
								sww.moveObject(dboObjectToHandle, Tc.RELEASED, false);
								result = "naits.success.releasedHerdMovementDoc";
							}
						} else {
							sww.moveObject(dboObjectToHandle, Tc.RELEASED, false);
						}
						if (dboObjectToHandle.getStatus().equals(Tc.RELEASED)) {
							DbDataObject dboHerd = null;
							for (DbDataObject herdMovement : herdMovements.getItems()) {
								dboHerd = svr.getObjectById(herdMovement.getParent_id(),
										SvReader.getTypeIdByName(Tc.HERD), new DateTime());
								DbDataObject dboDestinationHolding = rdr.findAppropriateHoldingByPic(
										herdMovement.getVal(Tc.DESTINATION_HOLDING_ID).toString(), svr);
								finishHerdMovement(dboHerd, dboDestinationHolding, null, null, null, sww, svr);
								sww.moveObject(dboHerd, Tc.VALID, true);
								if (dboHerd != null) {
									DbDataArray animalsInHerd = getAnimalsInHerd(dboHerd.getObject_id(), svr);
									if (animalsInHerd != null && !animalsInHerd.getItems().isEmpty()) {
										for (DbDataObject tempAnimal : animalsInHerd.getItems()) {
											DbDataArray animalMovements = svr.getObjectsByParentId(
													tempAnimal.getObject_id(),
													SvReader.getTypeIdByName(Tc.ANIMAL_MOVEMENT), null, 0, 0);
											if (animalMovements != null && !animalMovements.getItems().isEmpty()) {
												for (DbDataObject tempAnimalMovement : animalMovements.getItems()) {
													DbDataObject animalMovementDocument = rdr
															.getMovementDocumentPerAnimal(tempAnimalMovement
																	.getVal(Tc.MOVEMENT_DOC_ID).toString(), svr);
													if (animalMovementDocument != null) {
														sww.moveObject(animalMovementDocument, Tc.RELEASED);
														result = "naits.success.releasedMovementDoc";
													}
												}
											}
											sww.moveObject(tempAnimal, Tc.VALID);
										}
									}
								}
							}
							wr.autoAsignSessionUserToObjectField(dboObjectToHandle, Tc.RECIPIENT_USER, false, svr);
							svw.saveObject(dboObjectToHandle, false);
						}
					}
					break;
				case Tc.CANCELLED:
					DbDataObject dboHerd = null;
					if (dboObjectToHandle.getStatus().equalsIgnoreCase(Tc.DRAFT)) {
						for (DbDataObject dboMovement : herdMovements.getItems()) {
							dboHerd = svr.getObjectById(dboMovement.getParent_id(), SvReader.getTypeIdByName(Tc.HERD),
									null);
							if (dboHerd != null) {
								cancelMovement(dboHerd, sww, svr);
							}
							sww.moveObject(dboObjectToHandle, Tc.CANCELED);
							result = "naits.success.canceledHerdMovementDoc";
							if (dboHerd != null) {
								DbDataArray animalsInHerd = getAnimalsInHerd(dboHerd.getObject_id(), svr);
								if (animalsInHerd != null && !animalsInHerd.getItems().isEmpty()) {
									for (DbDataObject tempAnimal : animalsInHerd.getItems()) {
										DbDataArray animalMovements = svr.getObjectsByParentId(
												tempAnimal.getObject_id(), SvReader.getTypeIdByName(Tc.ANIMAL_MOVEMENT),
												null, 0, 0);
										if (animalMovements != null && !animalMovements.getItems().isEmpty()) {
											for (DbDataObject tempAnimalMovement : animalMovements.getItems()) {
												DbDataObject animalMovementDocument = rdr.getMovementDocumentPerAnimal(
														tempAnimalMovement.getVal(Tc.MOVEMENT_DOC_ID).toString(), svr);
												if (animalMovementDocument != null) {
													sww.moveObject(animalMovementDocument, Tc.CANCELED);
													result = "naits.success.canceledMovementDoc";
												}
											}
										}
										sww.moveObject(tempAnimal, Tc.VALID);
									}
								}
							}
						}
					} else {
						result = "naits.error.mvmDocStatusHasToBeDraft";
					}
					break;
				default:
					break;
				}
			}
			svr.dbCommit();
			svw.dbCommit();
			sww.dbCommit();
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
		} finally {
			if (svw != null) {
				svw.release();
			}
			if (sww != null) {
				sww.release();
			}
		}
		return result;
	}

	public DbDataArray getHerdMovementsByMovementDocument(DbDataObject movementDocument, SvReader svr)
			throws SvException {
		DbDataArray herdMovements = new DbDataArray();
		if (movementDocument.getVal(Tc.MOVEMENT_DOC_ID) != null) {
			DbSearchCriterion cr1 = new DbSearchCriterion(Tc.MOVEMENT_DOC_ID, DbCompareOperand.EQUAL,
					movementDocument.getVal(Tc.MOVEMENT_DOC_ID).toString());
			DbSearchExpression dbse = new DbSearchExpression();
			dbse.addDbSearchItem(cr1);
			herdMovements = svr.getObjects(dbse, SvReader.getTypeIdByName(Tc.HERD_MOVEMENT), null, 0, 0);
		}
		return herdMovements;
	}

	/**
	 * Method that creates link between LABORATORY and LAB_SAMPLE
	 * 
	 * @param dboLabSample
	 *            DbDataObject of lab sample
	 * @param laboratoryName
	 *            Laboratory name
	 * @param rdr
	 *            Reader instance
	 * @param svr
	 *            SvReader instance
	 * @param svw
	 *            SvWriter instance
	 * @return
	 * @throws SvException
	 */
	public String createLinkBetweenLaboratoryAndHerdLabSampleByLabName(Long herdLabSampleObjId, String laboratoryName,
			Reader rdr, SvWriter svw, SvReader svr) throws SvException {
		String result = Tc.EMPTY_STRING;
		Writer wr = new Writer();
		DbDataObject dboLabSample = svr.getObjectById(herdLabSampleObjId, SvReader.getTypeIdByName(Tc.LAB_SAMPLE),
				null);
		if (dboLabSample.getStatus().equals(Tc.COLLECTED)) {
			dboLabSample.setStatus(Tc.QUEUED);
			svw.saveObject(dboLabSample);
		}
		if (dboLabSample != null && !dboLabSample.getParent_id().equals(0L)) {
			DbDataObject dboLab = rdr.searchForObject(SvReader.getTypeIdByName(Tc.LABORATORY), Tc.LAB_NAME,
					laboratoryName, svr);
			if (dboLab != null) {
				DbDataObject dbLinkLabAndLabSample = SvReader.getLinkType(Tc.LABORATORY_SAMPLE,
						SvReader.getTypeIdByName(Tc.LABORATORY), SvReader.getTypeIdByName(Tc.LAB_SAMPLE));
				DbDataObject dboLink = wr.createSvarogLink(dbLinkLabAndLabSample.getObject_id(), dboLab, dboLabSample);
				svw.saveObject(dboLink);
				result = "naits.success.successfullyAssignedHerdLabSampleToLab";
			}
		}
		return result;
	}
}