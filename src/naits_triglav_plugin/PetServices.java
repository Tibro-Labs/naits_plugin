package naits_triglav_plugin;

import java.util.List;
import java.util.Map.Entry;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.prtech.svarog.SvCore;
import com.prtech.svarog.SvException;
import com.prtech.svarog.SvLink;
import com.prtech.svarog.SvReader;
import com.prtech.svarog.SvWriter;
import com.prtech.svarog.svCONST;
import com.prtech.svarog_common.DbDataArray;
import com.prtech.svarog_common.DbDataObject;

@Path("/naits_triglav_plugin/PetServices")
public class PetServices {

	static final Logger log4j = LogManager.getLogger(PetServices.class.getName());

	static final boolean isInitialized = initPlugin();

	static boolean initPlugin() {
		OnSaveValidations call = new OnSaveValidations();
		SvCore.registerOnSaveCallback(call, SvReader.getTypeIdByName(Tc.PET));
		SvCore.registerOnSaveCallback(call, SvReader.getTypeIdByName(Tc.PET_QUARANTINE));
		return true;
	}

	@Path("/managePetQuarantine/{sessionId}")
	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces("text/html;charset=utf-8")
	public Response managePetQuarantine(@PathParam("sessionId") String sessionId,
			MultivaluedMap<String, String> formVals) {
		String resultMsgLbl = "naits.error.massHerdAction";
		JsonObject jsonData = null;
		Gson gson = new Gson();
		try {
			for (Entry<String, List<String>> entry : formVals.entrySet()) {
				if (entry.getKey() != null && !entry.getKey().isEmpty()) {
					String key = entry.getKey();
					jsonData = gson.fromJson(key, JsonObject.class);
				}
			}
			if (jsonData != null && jsonData.has(Tc.OBJ_ARRAY)) {
				resultMsgLbl = petMassHandler(jsonData, sessionId);
			} else {
				resultMsgLbl = Tc.error_admConsoleBadJson;
			}
		} catch (SvException e) {
			resultMsgLbl = ((SvException) e).getLabelCode();
			log4j.error("Error in processing managePetQuarantine:" + ((SvException) e).getFormattedMessage(), e);
		} catch (Exception e) {
			log4j.error("General error in processing managePetQuarantine:", e);
		}
		return Response.status(200).entity(resultMsgLbl).build();
	}

	@Path("/deleteSinglePetQuarantine/{sessionId}/{petQuarantinjeObjId}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response deleteSinglePetQuarantine(@PathParam("sessionId") String sessionId,
			@PathParam("petQuarantinjeObjId") Long petQuarantinjeObjId) {
		String result = "naits.error.deleteSinglePetQuarantine";
		SvReader svr = null;
		SvWriter svw = null;
		try {
			svr = new SvReader(sessionId);
			svw = new SvWriter(svr);
			svw.dbSetAutoCommit(false);
			DbDataObject dboPetQuarantine = svr.getObjectById(petQuarantinjeObjId,
					SvReader.getTypeIdByName(Tc.PET_QUARANTINE), null);
			if (dboPetQuarantine != null) {
				Boolean b = deletePetQuarantine(dboPetQuarantine, svr, svw);
				if (b) {
					result = "naits.success.singlePetQuarantineDeletedWIthAllLinkedPets";
				}
			}
		} catch (Exception e) {
			if (e instanceof SvException) {
				log4j.error("Error in deleteSinglePetQuarantine: " + ((SvException) e).getFormattedMessage(), e);
			} else
				log4j.error("General error in deleteSinglePetQuarantine", e);
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

	@Path("/savePetQuarantine/{sessionId}/{petQuarantineObjId}/{dateFrom}/{dateTo}/{note}/{conclusion}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response savePetQuarantine(@PathParam("sessionId") String sessionId,
			@PathParam("petQuarantineObjId") Long petQuarantineObjId, @PathParam("dateFrom") String dateFrom,
			@PathParam("dateTo") String dateTo, @PathParam("note") String note,
			@PathParam("conclusion") String conclusion) {
		String result = "naits.error.saveSinglePetQuarantine";
		SvReader svr = null;
		SvWriter svw = null;
		try {
			svr = new SvReader(sessionId);
			svw = new SvWriter(svr);
			svw.dbSetAutoCommit(false);
			DbDataObject existingDboPetQuarantine = null;
			if (petQuarantineObjId != null && petQuarantineObjId != 0L) {
				existingDboPetQuarantine = svr.getObjectById(petQuarantineObjId,
						SvReader.getTypeIdByName(Tc.PET_QUARANTINE), null);
			}
			if (dateFrom != null && dateFrom != "null" && dateTo != null && dateTo != "null") {
				DbDataObject dboPetQuarantine = createPetQuarantine(existingDboPetQuarantine, dateFrom, dateTo, note,
						conclusion);
				svw.saveObject(dboPetQuarantine);
				result = "naits.success.saveSinglePetQuarantine";
			}
		} catch (Exception e) {
			if (e instanceof SvException) {
				log4j.error("Error in saveSinglePetQuarantine: " + ((SvException) e).getFormattedMessage(), e);
			} else
				log4j.error("General error in saveSinglePetQuarantine", e);
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

	public String petMassHandler(JsonObject jsonData, String sessionId) throws SvException {
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
			Long quarantineObjId = null;

			int count = 0;

			DbDataObject dboPetQuarantine = null;
			DbDataArray linksPetAndQuarantineToSave = new DbDataArray();
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
				if (obj.has(Tc.MASS_PARAM_PET_QUARANTINE_OBJ_ID)) {
					quarantineObjId = obj.get(Tc.MASS_PARAM_PET_QUARANTINE_OBJ_ID).getAsLong();
					dboPetQuarantine = svr.getObjectById(quarantineObjId, SvReader.getTypeIdByName(Tc.PET_QUARANTINE),
							null);
				}
			}
			DbDataObject petTypeDesc = SvReader.getDbtByName(Tc.PET);
			for (JsonElement jsonElement : jsonArrayData) {
				DbDataObject dboObjectToHandle = null;

				JsonObject obj = jsonElement.getAsJsonObject();
				if (obj.get(Tc.PET + "." + Tc.OBJECT_ID) != null) {
					dboObjectToHandle = svr.getObjectById(obj.get(Tc.PET + "." + Tc.OBJECT_ID).getAsLong(), petTypeDesc,
							null);
				}
				switch (actionName.toUpperCase()) {
				case Tc.PET_ACTIONS:
					switch (subActionName.toUpperCase()) {
					case Tc.ADD_PET_TO_QUARANTINE:
						addPetToQuarantineValidationSet(dboObjectToHandle, svr);
						DbDataObject dboLinkPetQuarantine = createLinkBetweenPetAndQuarantine(dboObjectToHandle,
								dboPetQuarantine, wr, svr);
						linksPetAndQuarantineToSave.addDataItem(dboLinkPetQuarantine);
						count++;
						break;
					case Tc.REMOVE_PET_FROM_QUARANTINE:
						if (!invalidateLinkBetweenPetAndQuarantine(dboObjectToHandle, quarantineObjId, svr)) {
							result = "naits.error.failedToInactivateLinkBetweenAnimalAndHerd";
						}
					default:
						break;
					}
				default:
					break;
				}
				if (count == 100) {
					count = 0;
					svw.saveObject(linksPetAndQuarantineToSave, true, true);
					linksPetAndQuarantineToSave = new DbDataArray();
				}
			}
			if (!linksPetAndQuarantineToSave.getItems().isEmpty()) {
				svw.saveObject(linksPetAndQuarantineToSave, true, true);
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

	public void addPetToQuarantineValidationSet(DbDataObject dboPet, SvReader svr) throws SvException {
		if (dboPet != null) {
			if (!dboPet.getStatus().equals(Tc.VALID)) {
				throw (new SvException("naits.error.onlyValidPetCanBeHandledWithChoosenAction", svr.getInstanceUser()));
			}
			if (checkIfPetAlreadyBelongsInQuarantine(dboPet.getObject_id(), svr)) {
				throw (new SvException("naits.error.petAlreadyBelongsInTheQuarantine", svr.getInstanceUser()));
			}
		}
	}

	public Boolean checkIfPetAlreadyBelongsInQuarantine(Long petObjid, SvReader svr) throws SvException {
		Boolean belongsInQuarantine = false;
		DbDataObject dboPet = svr.getObjectById(petObjid, SvReader.getTypeIdByName(Tc.PET), null);
		DbDataObject dbLink = SvLink.getLinkType(Tc.PET_QUARANTINE, SvReader.getTypeIdByName(Tc.PET),
				SvReader.getTypeIdByName(Tc.PET_QUARANTINE));
		DbDataArray dbArrPets = null;
		if (dboPet != null && dbLink != null) {
			dbArrPets = svr.getObjectsByLinkedId(dboPet.getObject_id(), dbLink, null, 0, 0);
			if (dbArrPets != null && !dbArrPets.getItems().isEmpty()) {
				belongsInQuarantine = true;
			}
		}
		return belongsInQuarantine;
	}

	public boolean invalidateLinkBetweenPetAndQuarantine(DbDataObject dboPet, Long petQuarantineObjId, SvReader svr)
			throws SvException {
		Boolean result = false;
		Reader rdr = new Reader();
		Writer wr = new Writer();
		if (dboPet != null && petQuarantineObjId != null) {
			DbDataObject dboPetQuarantine = svr.getObjectById(petQuarantineObjId,
					SvReader.getTypeIdByName(Tc.PET_QUARANTINE), null);
			if (rdr.checkIfLinkExists(dboPet, dboPetQuarantine, Tc.PET_QUARANTINE, null, svr)) {
				DbDataObject dbLink = SvReader.getLinkType(Tc.PET_QUARANTINE, SvReader.getTypeIdByName(Tc.PET),
						SvReader.getTypeIdByName(Tc.PET_QUARANTINE));
				DbDataObject dboLink = rdr.getLinkObject(dboPet.getObject_id(), dboPetQuarantine.getObject_id(),
						dbLink.getObject_id(), svr);
				wr.invalidateLink(dboLink, true, svr);
				result = true;
			}
		}
		return result;
	}

	public DbDataObject createLinkBetweenPetAndQuarantine(DbDataObject dboPet, DbDataObject dboPetQuarantine, Writer wr,
			SvReader svr) {
		DbDataObject dboLink = null;
		if (dboPet != null && dboPetQuarantine != null) {
			DbDataObject dbLinkAnimalAndHerd = SvReader.getLinkType(Tc.PET_QUARANTINE, SvReader.getTypeIdByName(Tc.PET),
					SvReader.getTypeIdByName(Tc.PET_QUARANTINE));
			dboLink = wr.createSvarogLink(dbLinkAnimalAndHerd.getObject_id(), dboPet, dboPetQuarantine);

		}
		return dboLink;
	}

	public Boolean deletePetQuarantine(DbDataObject dboPetQuarantine, SvReader svr, SvWriter svw) throws SvException {
		Boolean result = false;
		DbDataObject dbLinkBetweenPetAndQuarantine = SvReader.getLinkType(Tc.PET_QUARANTINE,
				SvReader.getTypeIdByName(Tc.PET), SvReader.getTypeIdByName(Tc.PET_QUARANTINE));
		DbDataArray linkedPets = svr.getObjectsByLinkedId(dboPetQuarantine.getObject_id(),
				dboPetQuarantine.getObject_type(), dbLinkBetweenPetAndQuarantine, SvReader.getTypeIdByName(Tc.PET),
				false, null, 0, 0);
		if (!linkedPets.getItems().isEmpty()) {
			for (DbDataObject tempLinkedPet : linkedPets.getItems()) {
				invalidateLinkBetweenPetAndQuarantine(tempLinkedPet, dboPetQuarantine.getObject_id(), svr);
			}
		}
		svw.deleteObject(dboPetQuarantine);
		result = true;
		return result;
	}

	public DbDataObject createPetQuarantine(DbDataObject existingDboQuarantine, String dateFrom, String dateTo,
			String note, String conclusion) throws SvException {
		DbDataObject dboPetQuarantine = new DbDataObject();
		String pattern = Tc.DATE_PATTERN;
		DateTime convertedDateFrom = null;
		DateTime convertedDateTo;
		if (existingDboQuarantine != null) {
			dboPetQuarantine = existingDboQuarantine;
		} else {
			dboPetQuarantine.setObject_type(SvReader.getTypeIdByName(Tc.PET_QUARANTINE));
		}
		if (dateFrom != null && !dateFrom.equals("null") && !dateFrom.equals("")) {
			convertedDateFrom = DateTime.parse(dateFrom, DateTimeFormat.forPattern(pattern));
			if (convertedDateFrom.isAfter(new DateTime())) {
				throw (new SvException("naits.error.validation_check_dateFromCanNotBeInTheFuture", svCONST.systemUser,
						null, null));
			}
			String substr_dateFrom = convertedDateFrom.toString().substring(0, 10);
			dboPetQuarantine.setVal(Tc.DATE_FROM, substr_dateFrom);
		}
		if (dateTo != null && !dateTo.equals("null") && !dateTo.equals("") && convertedDateFrom != null) {
			convertedDateTo = DateTime.parse(dateTo, DateTimeFormat.forPattern(pattern));
			if (convertedDateTo.isBefore(convertedDateFrom)) {
				throw (new SvException("naits.error.validation_check_dateToCanNotBeBeforDateFrom", svCONST.systemUser,
						null, null));
			}
			String substr_dateTo = convertedDateTo.toString().substring(0, 10);
			dboPetQuarantine.setVal(Tc.DATE_TO, substr_dateTo);
		}
		if (note != null) {
			dboPetQuarantine.setVal(Tc.NOTE, note);
		}
		if (conclusion != null) {
			dboPetQuarantine.setVal(Tc.CONCLUSION, note);
		}
		return dboPetQuarantine;
	}

	public int countQuarantinesPerHolding(Long holdingObjId, Boolean countActiveQuarantines,
			Boolean countExpiredQuarantines, SvReader svr) throws SvException {
		int result = 0;
		DateTime tempDateOfExpiry = null;
		DbDataArray quarantinesPerHodling = svr.getObjectsByParentId(holdingObjId,
				SvReader.getTypeIdByName(Tc.PET_QUARANTINE), null, null, null);
		for (DbDataObject tempPetQuarantine : quarantinesPerHodling.getItems()) {
			if (tempPetQuarantine.getVal(Tc.DATE_TO) != null) {
				tempDateOfExpiry = new DateTime(tempPetQuarantine.getVal(Tc.DATE_TO).toString());
				if (countActiveQuarantines && tempDateOfExpiry.isBeforeNow()) {
					result++;
				}
				if (countExpiredQuarantines && tempDateOfExpiry.isAfterNow()) {
					result++;
				}
			}
		}
		return result;
	}
}
